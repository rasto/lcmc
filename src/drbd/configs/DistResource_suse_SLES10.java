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
 * Here are commands for suse sles 10.
 */
public class DistResource_suse_SLES10 extends
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
        {"distributiondir", "sles10"},
        {"Support", "suse-SLES10"},

        {"HbInst.install.1.text", "download.opensuse.org" },
        {"HbInst.install.1", "rm -rf /tmp/drbd-mc-hbinst/; "
                           + "zypper -n install libnet && "
                           + "mkdir /tmp/drbd-mc-hbinst/ && "
                           + "wget -nd -r -np --progress=dot -P /tmp/drbd-mc-hbinst/ http://download.opensuse.org/repositories/server:/ha-clustering/SLES_10/@ARCH@/ && "
                           + "rm /tmp/drbd-mc-hbinst/pacemaker-mgmt-*.rpm && "
                           + "rm /tmp/drbd-mc-hbinst/heartbeat-ldirectord-*.rpm && "
                           + "rpm -Uvh /tmp/drbd-mc-hbinst/*.rpm && "
                           + "rm -rf /tmp/drbd-mc-hbinst/"},

        {"HbInst.install.text.2", "CD" },
        {"HbInst.install.2", "zypper -n install heartbeat"},

    };
}
