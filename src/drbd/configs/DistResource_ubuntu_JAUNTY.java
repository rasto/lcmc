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
 * Here are commands for ubuntu jaunty.
 */
public class DistResource_ubuntu_JAUNTY extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support",            "ubuntu-JAUNTY"},
        {"distributiondir",    "ubuntu-jaunty-server"},

        /* pacemaker/hb install method 1 */
        {"HbPmInst.install.text.1",
         "the ubuntu way: HB 2.1.4 (not recommended)"},

        {"HbPmInst.install.1",
         "apt-get update && /usr/bin/apt-get -y install -o"
         + " 'DPkg::Options::force=--force-confnew' heartbeat-2"},

        /* pacemaker/ais disabled */
        {"HbPmInst.install.text.1",
         ""},
    };
}
