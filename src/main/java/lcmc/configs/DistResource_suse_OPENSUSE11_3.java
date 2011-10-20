/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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
 * Here are commands for opensuse 11.3.
 */
public final class DistResource_suse_OPENSUSE11_3
                                    extends java.util.ListResourceBundle {

    /** Get contents. */
    @Override protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        /* Kernel versions and their counterpart in @KERNELVERSION@ variable in
         * the donwload url. Must begin with "kernel:" keyword. deprecated */

        /* distribution name that is used in the drbd download url */
        {"distributiondir", "sles11"},
        {"Support", "suse-OPENSUSE11_3"},
        {"DRBD.load", DistResource.SUDO + "/sbin/modprobe drbd"},

        /* Corosync/Openais/Pacemaker clusterlabs */
        {"PmInst.install.text.1",
         "clusterlabs repo: 1.0.x/1.2.x" },
        {"PmInst.install.1",
         "wget -N -nd -P /etc/zypp/repos.d/"
         + " http://www.clusterlabs.org/rpm/opensuse-11.3/clusterlabs.repo && "
         + "zypper -n --no-gpg-check install 'pacemaker<=1.1' 'corosync>=1.2.7'"
         + " 'libpacemaker3<=1.1' 'heartbeat-3.0.3'"
         + " && /sbin/chkconfig --add corosync"
         + " && if [ -e /etc/corosync/corosync.conf ];then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},

        /* Openais/Pacemaker native */
        {"PmInst.install.text.2", "zypper install: 1.0.x/0.80.x" },
        {"PmInst.install.2",
         "zypper -n install pacemaker"
         + " && if [ -e /etc/ais/openais.conf ];then"
         + " mv /etc/ais/openais.conf /etc/ais/openais.conf.orig; fi"
         + " && /sbin/chkconfig --add openais"},

        /* Heartbeat/Pacemaker Clusterlabs */
        {"HbPmInst.install.text.1",
         "clusterlabs repo: 1.0.x/3.0.x" },
        {"HbPmInst.install.1",
         "wget -N -nd -P /etc/zypp/repos.d/"
         + " http://www.clusterlabs.org/rpm/opensuse-11.3/clusterlabs.repo && "
         + "zypper -n --no-gpg-check install 'heartbeat>=3' 'pacemaker<=1.1'"
         + " 'libpacemaker3<=1.1' 'heartbeat-3.0.3'"
         + " && /sbin/chkconfig --add heartbeat"},

        /* Heartbeat/Pacemaker native */
        {"HbPmInst.install.text.2", "zypper install: 1.0.x/2.99.x" },
        {"HbPmInst.install.2",
         "zypper -n install heartbeat pacemaker"
         + " && /sbin/chkconfig --add heartbeat"},

        {"Corosync.startCorosync",
         DistResource.SUDO + "/etc/init.d/corosync start"},
        {"Corosync.stopCorosync",
         DistResource.SUDO + "/etc/init.d/corosync start"},
        {"Corosync.reloadCorosync",
         DistResource.SUDO + "/etc/init.d/corosync force-reload"},

        {"Openais.startOpenais",
         "PATH=/sbin:$PATH " + DistResource.SUDO + "/etc/init.d/openais start"},

        {"Openais.stopOpenais",
         "PATH=/sbin:$PATH " + DistResource.SUDO + "/etc/init.d/openais stop"},

        {"Openais.reloadOpenais",
         "if ! PATH=/sbin:$PATH " + DistResource.SUDO + "/etc/init.d/openais status >/dev/null 2>&1; then "
         + "PATH=/sbin:$PATH " + DistResource.SUDO + "/etc/init.d/openais start; fi"},
    };
}
