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
import java.util.ListResourceBundle;

/**
 * Here are commands for opensuse 13.1.
 */
public final class DistResource_suse_OPENSUSE13_1
                                    extends ListResourceBundle {

    /** Contents. */
    private static final Object[][] contents = {

        {"distributiondir", "sles11"},
        {"Support", "suse-OPENSUSE13_1"},

        {"Corosync.startCorosync",
         DistResource.SUDO + "systemctl start corosync.service"},

        {"Corosync.startPcmk",
         DistResource.SUDO + "systemctl start pacemaker.service"},

        {"Corosync.stopCorosync",
         DistResource.SUDO + "systemctl stop corosync.service"},

        {"Corosync.stopCorosyncWithPcmk",
         DistResource.SUDO + "systemctl stop pacemaker.service && "
         + DistResource.SUDO + "systemctl stop corosync.service"},

        {"Corosync.startCorosyncWithPcmk",
         DistResource.SUDO + "systemctl start corosync.service;;;"
         + DistResource.SUDO + "systemctl start pacemaker.service"},

        {"Corosync.reloadCorosync",
         "if ! " + DistResource.SUDO + "systemctl status corosync.service >/dev/null 2>&1; then "
         + DistResource.SUDO + "systemctl start corosync.service; fi"},

        {"Corosync.addToRc",
         DistResource.SUDO + "systemctl enable corosync.service"},

    };

    /** Get contents. */
    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
