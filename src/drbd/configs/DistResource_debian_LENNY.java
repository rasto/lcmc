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
 * Here are commands for debian verson lenny.
 */
public class DistResource_debian_LENNY extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        /* Kernel versions and their counterpart in @KERNELVERSION@ variable in
         * the donwload url. Must begin with "kernel:" keyword. deprecated */

        /* distribution name that is used in the download url */
        {"distributiondir", "debian-lenny"},

        /* support */
        // TODO: use flags for this
        {"Support", "debian-LENNY"},

        /* corosync/pacemaker madkiss */
        {"PmInst.install.text.1",
         "Backports repo: 1.0.x/1.1.x"},

        {"PmInst.install.1",
         "echo 'deb http://backports.debian.org/debian-backports lenny-backports main'"
         + " > /etc/apt/sources.list.d/ha-clustering.list "
         + " && apt-get update"
         + " && apt-get -y -q  --allow-unauthenticated install"
         + " -o 'DPkg::Options::force=--force-confnew' psmisc pacemaker corosync"
         + " && sed -i 's/\\(START=\\)no/\\1yes/' /etc/default/corosync"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},

        {"HbPmInst.install.text.1", "the debian way: HB 2.1.x (obsolete)"},
        {"HbPmInst.install.1",
         "apt-get update && /usr/bin/apt-get -y -q install -o 'DPkg::Options::force=--force-confnew' heartbeat-2"},

        /* heartbeat apt-get install */
        {"HbPmInst.install.text.2",
         "the debian way: HB 2.1.3 (obsolete)"},

        {"HbPmInst.install.2",
         "apt-get update && "
         + "/usr/bin/apt-get -y -q install -o"
         + " 'DPkg::Options::force=--force-confnew' heartbeat-2"},

        /* Drbd install method 3 */
        {"DrbdInst.install.text.3",
         "the debian way"},

        {"DrbdInst.install.3",
         "apt-get update && /usr/bin/apt-get -y install -o "
         + "'DPkg::Options::force=--force-confnew' drbd8-modules-`uname -r` drbd8-utils"},

        /* heartbeat/pacemaker madkiss */
        {"HbPmInst.install.text.1",
         "Backports repo: 1.0.x/3.0.x"},

        {"HbPmInst.install.1",
         //"echo 'deb http://people.debian.org/~madkiss/ha lenny main' > /etc/apt/sources.list.d/ha-clustering.list "
         "echo 'deb http://backports.debian.org/debian-backports lenny-backports main' > /etc/apt/sources.list.d/ha-clustering.list "
         + " && apt-get update"
         + " && apt-get -y -q  --allow-unauthenticated -t lenny-backports install"
         + " -o 'DPkg::Options::force=--force-confnew' pacemaker heartbeat"},
    };
}
