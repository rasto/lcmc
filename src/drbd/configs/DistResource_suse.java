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
 * Here are commands for all suses.
 */
public class DistResource_suse extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support", "suse"},
        {"version:SUSE LINUX Enterprise Server 9 (i586)", "SLES9"}, // detected
        //{"version:SUSE LINUX Enterprise Server 10 (i586)", "SLES10"}, // detected
        {"version:9*",     "SLES9"}, // from lsb_release
        {"version:10*",    "SLES10"}, // from lsb_release
        {"version:11*",    "SLES11"}, // from lsb_release
        {"version:sles9",  "SLES9"}, // chosen
        {"version:sles10", "SLES10"}, // chosen
        {"version:sles11", "SLES11"}, // chosen

        {"kerneldir", "(\\d+\\.\\d+\\.\\d+\\.\\d+-[.0-9]+).*"},
        /* drbd donwload and installation */
        // { "DrbdCheck.version", "/bin/rpm -qa|grep drbd- | sed s/drbd-//" },
        {"DrbdInst.install", "/bin/rpm -Uvh /tmp/drbdinst/@DRBDPACKAGE@ /tmp/drbdinst/@DRBDMODULEPACKAGE@"},
        /* heartbeat donwload and installation */
        //{ "HbCheck.version", "/bin/rpm -qa|grep heartbeat | sed s/.*heartbeat-//" },
        {"HbInst.install.i386", "i586" },
        {"HbInst.install.i486", "i586" },
        {"HbInst.install.i586", "i586" },

        {"HbInst.install.text", "CD" },
        {"HbInst.install", "zypper -n install heartbeat"},
    };
}
