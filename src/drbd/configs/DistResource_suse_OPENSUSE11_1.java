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

package drbd.configs;

import java.util.Arrays;

/**
 * Here are commands for opensuse 11.
 */
public class DistResource_suse_OPENSUSE11_1 extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        /* Kernel versions and their counterpart in @KERNELVERSION@ variable in
         * the donwload url. Must begin with "kernel:" keyword. deprecated */

        /* distribution name that is used in the drbd download url */
        {"distributiondir", "sles11"},
        {"Support", "suse-OPENSUSE11_1"},
        {"DRBD.load",
         "sed -i 's/\\(allow_unsupported_modules \\)0/\\11/'"
         + " /etc/modprobe.d/unsupported-modules;"
         + "modprobe drbd"},

        /* Corosync/Openais/Pacemaker Opensuse */
        {"PmInst.install.text.1", "http://download.opensuse.org: zypper" },
        {"PmInst.install.1",
         "wget -N -nd -P /etc/zypp/repos.d/"
         + " http://download.opensuse.org/repositories/server:/ha-clustering/openSUSE_11.1/server:ha-clustering.repo && "
         + "zypper -n --no-gpg-check install pacemaker"
         + " && (/sbin/chkconfig --add corosync"
         + " || /sbin/chkconfig --add openais)"
         + " && if [ -e /etc/ais/openais.conf ];then"
         + " mv /etc/ais/openais.conf /etc/ais/openais.conf.orig; fi"
         + " && if [ -e /etc/corosync/corosync.conf ];then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig; fi"},

        /* Heartbeat/Pacemaker Opensuse */
        {"HbPmInst.install.text.1", "http://download.opensuse.org: zypper" },
        {"HbPmInst.install.1",
         "wget -N -nd -P /etc/zypp/repos.d/"
         + " http://download.opensuse.org/repositories/server:/ha-clustering/openSUSE_11.1/server:ha-clustering.repo && "
         + "zypper -n --no-gpg-check install heartbeat pacemaker"
         + " && chkconfig --add heartbeat"},

    };
}
