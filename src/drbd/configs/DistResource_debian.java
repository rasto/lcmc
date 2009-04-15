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
 * Here are commands for all debians.
 */
public class DistResource_debian extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support",             "debian"},
        //{"distribution",        "debian"},
        {"version:3.1",         "3_1"},
        {"version:3.2",         "3_2"},
        {"version:lenny/sid",   "LENNY"},
        {"version:testing",     "LENNY"},
        {"version:5.0",         "LENNY"},
        {"version:debian-etch", "4"},
        {"version:4.0",         "4"},
        {"version:4.0r1",       "4"},

        {"arch:x86_64",       "amd64"}, // convert arch to arch in the drbd download file

        /* directory capturing regexp on the website from the kernel version */
        {"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+).*"},

        {"DrbdInst.install", "echo | dpkg -i --force-confold /tmp/drbdinst/@DRBDPACKAGE@ /tmp/drbdinst/@DRBDMODULEPACKAGE@"},

        //{"HbCheck.version", "/usr/bin/dpkg-query --showformat='${Status} ${Version}\n' -W heartbeat-2 2>&1|grep '^install ok installed'|cut -d ' ' -f 4"},
        {"HbInst.install", "/usr/bin/apt-get -y -q install -o 'DPkg::Options::force=--force-confnew' heartbeat-2"},

        //{"UdevCheck.version", "/usr/bin/dpkg-query --showformat='${Status} ${Version}\n' -W udev 2>&1|grep '^install ok installed'|cut -d ' ' -f 4"},
        {"Udev.install",      "/usr/bin/apt-get -y -q install -o 'DPkg::Options::force=--force-confnew' udev"},
    };
}
