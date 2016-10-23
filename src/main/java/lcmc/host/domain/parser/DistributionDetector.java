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

import static lcmc.common.domain.util.Tools.getDistString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lcmc.common.domain.ConvertCmdCallback;
import lcmc.common.domain.util.Tools;
import lcmc.configs.DistResource;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lombok.Getter;

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
	 * @param lines
	 */
	@SuppressWarnings("fallthrough")
	public void detect(final List<String> lines) {
		if (lines == null) {
			LOG.debug("setDistInfo: " + host.getName() + " dist info is null");
			return;
		}
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
				LOG.appError("setDistInfo: list: ", Arrays.asList(lines).toString());
				break;
		}
		distributionName = detectedDist;
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
		this.kernelName = "Linux";

		if (!distributionName.equals(detectedDist)) {
			LOG.appError("initDistInfo: dist: " + distributionName + " does not match " + detectedDist);
		}
		distributionVersionString = Tools.getDistVersionString(distributionName, distributionVersion);
		distributionVersion = getDistString("distributiondir", detectedDist, distributionVersionString, null);
		this.kernelVersion = Tools.getKernelDownloadDir(detectedKernelVersion, getDistributionName(), distributionVersionString, null);
		String arch0 = getDistString("arch:" + detectedArch, getDistributionName(), distributionVersionString, null);
		if (arch0 == null) {
			arch0 = detectedArch;
		}
		this.arch = arch0;
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
	 * TODO: remove it?
	 */
	public String getDistFromDistVersion(final String dV) {
        /* remove numbers */
		if ("No Match".equals(dV)) {
			return null;
		}
		LOG.debug1("getDistFromDistVersion:" + dV.replaceFirst("\\d.*", ""));
		return getDistString("dist:" + dV.replaceFirst("\\d.*", ""), "", "", null);
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
		final List<String> results =  new ArrayList<String>();
		int i = 0;
		for (final String t : texts) {
			String distString = getDistString(t, distributionName, distributionVersionString, arch);
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
			ret = Tools.join(";;;", results.toArray(new String[results.size()]));
		}
		if (convertCmdCallback != null && ret != null) {
			ret = convertCmdCallback.convert(ret);
		}
		return ret;
	}
}
