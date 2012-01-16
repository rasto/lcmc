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
 * Here are commands for ubuntu karmic.
 */
public final class DistResource_ubuntu_KARMIC
                                        extends java.util.ListResourceBundle {

    /** Get contents. */
    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support",            "ubuntu-KARMIC"},
        {"distributiondir",    "ubuntu-karmic-server"},

        /* pacemaker heartbeat install method 1 */
        {"HbPmInst.install.text.1",
         "apt-get install: 1.0.x / 2.99.x"},

        {"HbPmInst.install.1",
         "apt-get update && /usr/bin/apt-get -y install -o"
         + " 'DPkg::Options::force=--force-confnew' pacemaker-heartbeat"},

        /* pacemaker corosync install method 1 */
        {"PmInst.install.text.1",
         "apt-get install: 1.0.x / 1.0.x"},

        {"PmInst.install.1",
         "apt-get update && /usr/bin/apt-get -y install -o"
         + " 'DPkg::Options::force=--force-confnew' pacemaker-openais "
         + " && (grep 'START=no' /etc/default/corosync && echo 'START=yes'>>/etc/default/corosync)"
         + " && if [ -e /etc/corosync/corosync.conf ];then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig; fi"},
    };
}
