/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2016, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.host.domain.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lcmc.common.domain.ConvertCmdCallback;
import lcmc.common.domain.util.Tools;
import lcmc.configs.DistResource;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lombok.Getter;
import lombok.val;

public class DistributionDetector {

    private final Host host;
    /** Whether dist info was already logged. */
    private boolean distInfoAlreadyLogged = false;
    private String detectedKernelName = "";
    private String detectedDistVersion = "";
    @Getter
    private String detectedKernelVersion = "";
    private String detectedDist = "";
    private String detectedArch = "";
    @Getter
    private String distributionName = "";
    @Getter
    private String distributionVersion = "";
    @Getter
    private String distributionVersionString = "";
    @Getter
    private String kernelName = "";
    @Getter
    private String kernelVersion = "";
    @Getter
    private String arch = "";

    private static final Logger LOG = LoggerFactory.getLogger(Host.class);

    public DistributionDetector(final Host host) {
        this.host = host;
    }

    /**
     * Sets distribution info for this host from array of strings.
     * Array consists of: kernel name, kernel version, arch, os, version
     * and distribution.
     */
    public void detect(final List<String> lines) {
        if (!distInfoAlreadyLogged) {
            for (final String di : lines) {
                LOG.debug1("setDistInfo: dist info: " + di);
            }
        }

        /* no breaks in the switch statement are intentional */
        String lsbVersion = null;
        String lsbDist = null;
        switch (lines.size()) {
            case 9:
                lsbVersion = lines.get(8); // TODO: not used
            case 8:
                lsbDist = lines.get(7);
            case 7:
                lsbVersion = lines.get(6); // TODO: not used
            case 6:
                lsbDist = lines.get(5);
            case 5:
                if (lsbDist == null || "linux".equals(lsbDist)) {
                    detectedDist = lines.get(4);
                } else {
                    detectedDist = lsbDist;
                }
            case 4:
                if (lsbVersion == null) {
                    detectedDistVersion = lines.get(3);
                } else {
                    detectedDistVersion = lines.get(3) + '/' + lsbVersion;
                }
            case 3:
                detectedKernelVersion = lines.get(2);
            case 2:
                detectedArch = lines.get(1);
            case 1:
                detectedKernelName = lines.get(0);
            case 0:
                break;
            default:
                LOG.appError("setDistInfo: list: ", List.of(lines).toString());
                break;
        }
        distributionName = getDistFromDistVersion(detectedDist);
        detectedDist = distributionName;
        distributionVersion = detectedDistVersion;
        initDistInfo();
        if (!distInfoAlreadyLogged) {
            LOG.debug1("setDistInfo: kernel name: " + detectedKernelName);
            LOG.debug1("setDistInfo: kernel version: " + detectedKernelVersion);
            LOG.debug1("setDistInfo: arch: " + detectedArch);
            LOG.debug1("setDistInfo: dist version: " + detectedDistVersion);
            LOG.debug1("setDistInfo: dist: " + detectedDist);
        }
        distInfoAlreadyLogged = true;
    }

    /** Initializes dist info. Must be called after setDistInfo. */
    void initDistInfo() {
        if (!"Linux".equals(detectedKernelName)) {
            LOG.appWarning("initDistInfo: detected kernel not linux: " + detectedKernelName);
        }
        kernelName = "Linux";

        if (!distributionName.equals(detectedDist)) {
            LOG.appError("initDistInfo: dist: " + distributionName + " does not match " + detectedDist);
        }
        distributionVersionString = getDistVersionString(distributionVersion);
        distributionVersion = getDistString("distributiondir");
        kernelVersion = getKernelDownloadDir(detectedKernelVersion);
        String arch0 = getDistString("arch:" + detectedArch);
        if (arch0 == null) {
            arch0 = detectedArch;
        }
        arch = arch0;
    }

    /** Returns the detected info to show. */
    public String getDetectedInfo() {
        return detectedDist + ' ' + detectedDistVersion;
    }

    /**
     * Gets distribution name from distribution version. E.g. suse from sles9.
     * This is used only when the distribution is selected in the pulldown menu,
     * not when it is detected.
     * The conversion rules for distributions are defined in DistResource.java,
     * with 'dist:' prefix.
     */
    public String getDistFromDistVersion(final String dV) {
        /* remove numbers */
        if ("No Match".equals(dV)) {
            return null;
        }
        LOG.debug1("getDistFromDistVersion:" + dV.replaceFirst("\\d.*", ""));
        val distString = getDistString("dist:" + dV.replaceFirst("\\d.*", ""));
        return distString == null ? dV : distString;
    }

    /**
     * Returns command from DistResource resource bundle for specific
     * distribution and version.
     */
    public String getDistCommand(final String text,
                                 final ConvertCmdCallback convertCmdCallback,
                                 final boolean inBash,
                                 final boolean inSudo) {
        if (text == null) {
            return null;
        }
        final String[] texts = text.split(";;;");
        final List<String> results = new ArrayList<>();
        int i = 0;
        for (final String t : texts) {
            String distString = getDistString(t);
            if (distString == null) {
                LOG.appWarning("getDistCommand: unknown command: " + t);
                distString = t;
            }
            if (inBash && i == 0) {
                String sudoS = "";
                if (inSudo) {
                    sudoS = DistResource.SUDO;
                }
                results.add(sudoS + "bash -c \"" + Tools.escapeQuotes(distString, 1) + '"');
            } else {
                results.add(distString);
            }
            i++;
        }
        String ret;
        if (results.isEmpty()) {
            ret = text;
        } else {
            ret = Tools.join(";;;", results.toArray(new String[0]));
        }
        if (convertCmdCallback != null) {
            ret = convertCmdCallback.convert(ret);
        }
        return ret;
    }

    /** Returns string that is specific to a distribution and version. */
    public String getDistString(final String text) {
        if (distributionName == null) {
            distributionName = "";
        }
        if (distributionVersionString == null) {
            distributionVersionString = "";
        }
        final Locale locale = new Locale(distributionName, distributionVersionString);
        LOG.debug2("getDistString: text: " + text + " dist: " + distributionName + " version: " + distributionVersionString);
        final ResourceBundle resourceString = ResourceBundle.getBundle("lcmc.configs.DistResource", locale);
        String ret;
        try {
            ret = resourceString.getString(text + '.' + arch);
        } catch (final Exception e) {
            ret = null;
        }
        if (ret == null) {
            try {
                ret = resourceString.getString(text);
                LOG.debug2("getDistString: ret: " + ret);
                return ret;
            } catch (final RuntimeException e) {
                return null;
            }
        }
        return ret;
    }

    /**
     * Gets compact representation of distribution and version. Distribution
     * and version are joined with "_" and all spaces and '.' are replaced by
     * "_" as well.
     */
    public String getDistVersionString(final String distributionVersion) {
        if (distributionName == null) {
            distributionName = "";
        }
        LOG.debug2("getDistVersionString: dist: " + distributionName + ", version: " + distributionVersion);
        final Locale locale = new Locale(distributionName, "");
        final ResourceBundle resourceCommand = ResourceBundle.getBundle("lcmc.configs.DistResource", locale);
        String distVersion = null;
        try {
            distVersion = resourceCommand.getString("version:" + distributionVersion);
        } catch (final Exception e) {
            /* with wildcard */
            final StringBuilder buf = new StringBuilder(distributionVersion);
            for (int i = distributionVersion.length() - 1; i >= 0; i--) {
                try {
                    distVersion = resourceCommand.getString("version:" + buf + '*');
                } catch (final Exception e2) {
                    distVersion = null;
                }
                if (distVersion != null) {
                    break;
                }
                buf.setLength(i);
            }
            if (distVersion == null) {
                distVersion = distributionVersion;
            }
        }
        LOG.debug2("getDistVersionString: dist version: " + distVersion);
        return distVersion;
    }

    /**
     * Converts kernelVersion as parsed from uname to a version that is used
     * in the download area on the website.
     */
    public String getKernelDownloadDir(final CharSequence kernelVersion) {
        final String regexp = getDistString("kerneldir");
        if (regexp != null && kernelVersion != null) {
            final Pattern p = Pattern.compile(regexp);
            final Matcher m = p.matcher(kernelVersion);
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }

    /** Returns string that is specific to a distribution and version. */
    public List<String> getDistStrings(final String text) {
        if (distributionName == null) {
            distributionName = "";
        }
        if (distributionVersionString == null) {
            distributionVersionString = "";
        }
        final Locale locale = new Locale(distributionName, distributionVersionString);
        LOG.debug2("getDistStrings: text: " + text + " dist: " + distributionName + " version: " + distributionVersionString);
        final ResourceBundle resourceString = ResourceBundle.getBundle("lcmc.configs.DistResource", locale);
        List<String> ret;
        try {
            ret = (List<String>) resourceString.getObject(text);
        } catch (final Exception e) {
            ret = new ArrayList<>();
        }
        return ret;
    }
}
