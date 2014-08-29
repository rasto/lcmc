/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
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

package lcmc.model.drbd;

import lcmc.Exceptions;
import lcmc.gui.dialog.host.DrbdVersions;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DrbdInstallation {
    private static final Logger LOG = LoggerFactory.getLogger(DrbdInstallation.class);

    /** Drbd version of the source tarball, that will be installed . */
    private String drbdBuildToInstall = null;
    private String drbdPackagesToInstall = null;
    private boolean drbdWillBeUpgraded = false;
    private boolean drbdWasNewlyInstalled = false;
    private String drbdInstallMethodIndex;
    private String proxyInstallMethodIndex;
    private DrbdVersions drbdVersions;

    private List<String> availableDrbdVersions = null;

    public void setDrbdToInstall(final DrbdVersions drbdVersions) {
        this.drbdVersions = drbdVersions;
    }

    public DrbdVersions getDrbdToInstall() {
        return drbdVersions;
    }

    /**
     * Sets drbdBuildToInstall. This build is the one that is to be installed.
     */
    public void setDrbdBuildToInstall(final String drbdBuildToInstall) {
        this.drbdBuildToInstall = drbdBuildToInstall;
    }

    /** Returns the drbd build to be installed. */
    public String getDrbdBuildToInstall() {
        return drbdBuildToInstall;
    }

    public void setDrbdPackagesToInstall(final String drbdPackagesToInstall) {
        this.drbdPackagesToInstall = drbdPackagesToInstall;
    }

    public void setDrbdWillBeUpgraded(final boolean drbdWillBeUpgraded) {
        this.drbdWillBeUpgraded = drbdWillBeUpgraded;
    }

    public void setDrbdWasNewlyInstalled(final boolean drbdWasNewlyInstalled) {
        this.drbdWasNewlyInstalled = drbdWasNewlyInstalled;
    }

    /**
     * Returns true if drbd will be upgraded and drbd was installed.
     * TODO: ???
     */
    public boolean isDrbdUpgraded() {
        return drbdWillBeUpgraded && drbdWasNewlyInstalled;
    }

    /** Sets drbd installation method index. */
    public void setDrbdInstallMethodIndex(final String drbdInstallMethodIndex) {
        this.drbdInstallMethodIndex = drbdInstallMethodIndex;
    }

    public String getDrbdInstallMethodIndex() {
        return drbdInstallMethodIndex;
    }

    public void setProxyInstallMethodIndex(final String proxyInstallMethodIndex) {
        this.proxyInstallMethodIndex = proxyInstallMethodIndex;
    }

    public String getProxyInstallMethodIndex() {
        return proxyInstallMethodIndex;
    }

    public String replaceVarsInCommand(String command) {
        if (drbdVersions != null) {
            final String drbdVersionToInstall = drbdVersions.getModuleVersion();
            if (drbdVersionToInstall != null && command.indexOf("@DRBDVERSION@") > -1) {
                command = command.replaceAll("@DRBDVERSION@", drbdVersionToInstall);
            }
            final String utilVersionToInstall = drbdVersions.getUtilVersion();
            if (utilVersionToInstall != null && command.indexOf("@DRBD-UTIL-VERSION@") > -1) {
                command = command.replaceAll("@DRBD-UTIL-VERSION@", utilVersionToInstall);
            }
        }
        if (drbdBuildToInstall != null && command.indexOf("@BUILD@") > -1) {
            command = command.replaceAll("@BUILD@", drbdBuildToInstall);
        }
        if (drbdPackagesToInstall != null && command.indexOf("@DRBDPACKAGES@") > -1) {
            command = command.replaceAll("@DRBDPACKAGES@", drbdPackagesToInstall);
        }
        return command;
    }

    public void setAvailableDrbdVersions(final String[] versions) {
        availableDrbdVersions = new ArrayList<String>(Arrays.asList(versions));
    }

    public String[] getAvailableDrbdVersions() {
        if (availableDrbdVersions == null) {
            return null;
        }
        return availableDrbdVersions.toArray(new String[availableDrbdVersions.size()]);
    }

    public boolean isDrbdUpgradeAvailable(final String versionString) {
        if (availableDrbdVersions == null || versionString == null) {
            return false;
        }
        final String version = versionString.split(" ")[0];
        for (final String v : availableDrbdVersions) {
            try {
                if (Tools.compareVersions(v, version) > 0) {
                    return true;
                }
            } catch (final Exceptions.IllegalVersionException e) {
                LOG.appWarning("isDrbdUpgradeAvailable: "+ e.getMessage(), e);
            }
        }
        return false;
    }
}
