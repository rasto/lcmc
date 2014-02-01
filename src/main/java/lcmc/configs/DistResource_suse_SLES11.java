/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.configs;

import java.util.Arrays;

/**
 * Here are commands for suse sles 11.
 */
public final class DistResource_suse_SLES11
                                        extends java.util.ListResourceBundle {

    /** Get contents. */
    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static final Object[][] contents = {
        /* Kernel versions and their counterpart in @KERNELVERSION@ variable in
         * the donwload url. Must begin with "kernel:" keyword. deprecated */

        /* distribution name that is used in the download url */
        {"distributiondir", "sles11"},
        {"Support", "suse-SLES11"},
        {"DRBD.load",
         DistResource.SUDO + "sed -i 's/\\(allow_unsupported_modules \\)0/\\11/'"
         + " /etc/modprobe.d/unsupported-modules;"
         + DistResource.SUDO + "/sbin/modprobe drbd"},

        /* Corosync/Openais/Pacemaker clusterlabs */
        {"PmInst.install.text.1",
         "opensuse SLE 11 SP1 repo: 1.1.x/1.2.x" },
        {"PmInst.install.1",
         "wget -N -nd -P /etc/zypp/repos.d/"
         + " http://download.opensuse.org/repositories/network:/ha-clustering/SLE_11/network:ha-clustering.repo "
         + " && zypper -n --no-gpg-check install pacemaker corosync"
         + " && if [ -e /etc/corosync/corosync.conf ];then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},

        /* Openais/Corosync/Pacemaker ha extension */
        {"PmInst.install.text.2", "HAE: 1.0.x/0.80.x" },
        {"PmInst.install.2",
         "zypper -n install pacemaker"
         + " && if [ -e /etc/ais/openais.conf ];then"
         + " mv /etc/ais/openais.conf /etc/ais/openais.conf.orig; fi"},

        /* Corosync/Openais/Pacemaker clusterlabs */
        {"PmInst.install.text.3",
         "clusterlabs repo: 1.0.x/1.2.x" },
        {"PmInst.install.3",
         "wget -N -nd -P /etc/zypp/repos.d/"
         + " http://www.clusterlabs.org/rpm/opensuse-11.1/clusterlabs.repo && "
         + "zypper -n --no-gpg-check install pacemaker corosync"
         + " && if [ -e /etc/corosync/corosync.conf ];then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},

        /* Heartbeat/Pacemaker Clusterlabs */
        {"HbPmInst.install.text.1",
         "clusterlabs repo: 1.0.x/3.0.x" },
        {"HbPmInst.install.1",
         "wget -N -nd -P /etc/zypp/repos.d/"
         + " http://www.clusterlabs.org/rpm/opensuse-11.1/clusterlabs.repo && "
         + "zypper -n --no-gpg-check install heartbeat pacemaker"},

        /* Drbd install method 3 */
        {"DrbdInst.install.text.3",
         "zypper install: 8.2.x"},

        {"DrbdInst.install.3",
         "zypper -n in drbd drbd-kmp-`uname -r|sed s/.*-//`"},

        {"DrbdInst.install.method.3",
         ""},

        {"Openais.startOpenais",
         "PATH=/sbin:$PATH " + DistResource.SUDO + "/etc/init.d/openais start"},

        {"Openais.stopOpenais",
         "PATH=/sbin:$PATH " + DistResource.SUDO + "/etc/init.d/openais stop"},

        {"Openais.reloadOpenais",
         "if ! PATH=/sbin:$PATH " + DistResource.SUDO + "/etc/init.d/openais status >/dev/null 2>&1; then "
         + "PATH=/sbin:$PATH " + DistResource.SUDO + "/etc/init.d/openais start; fi"},
    };
}
