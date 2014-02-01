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
 * Here are commands for debian verson squeeze.
 */
public final class DistResource_debian_SQUEEZE
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
        {"distributiondir", "debian-squeeze"},

        /* support */
        {"Support", "debian-SQUEEZE"},

        /* corosync/pacemaker backports */
        {"HbPmInst.install.text.2",
         "Backports repo: 1.1.x/3.0.x"},

        {"HbPmInst.install.2",
         "echo 'deb http://backports.debian.org/debian-backports"
         + " squeeze-backports main'"
         + " > /etc/apt/sources.list.d/ha-clustering.list "
         + " && apt-get update"
         + " && apt-get -t squeeze-backports -y -q  --allow-unauthenticated install"
         + " -o 'DPkg::Options::force=--force-confnew' pacemaker heartbeat"},

        /* corosync/pacemaker madkiss */
        {"PmInst.install.text.2",
         "Backports repo: 1.1.x/1.4.x"},

        {"PmInst.install.2",
         "echo 'deb http://backports.debian.org/debian-backports"
         + " squeeze-backports main'"
         + " > /etc/apt/sources.list.d/ha-clustering.list "
         + " && apt-get update"
         + " && apt-get -t squeeze-backports -y -q  --allow-unauthenticated install"
         + " -o 'DPkg::Options::force=--force-confnew' pacemaker corosync"
         + " && sed -i 's/\\(START=\\)no/\\1yes/' /etc/default/corosync"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},

    };
}
