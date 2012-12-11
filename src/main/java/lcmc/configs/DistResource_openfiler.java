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
 * Here are commands for all openfilers.
 */
public final class DistResource_openfiler extends java.util.ListResourceBundle {

    /** Get contents. */
    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support",                       "openfiler"},
        {"version:Openfiler NSA 2.3",     "2"},

        {"Heartbeat.deleteFromRc",
         DistResource.SUDO + "/sbin/chkconfig --del heartbeat"},

        {"Heartbeat.addToRc",
         DistResource.SUDO + "/sbin/chkconfig --level 2345 heartbeat on"},

        {"Corosync.addToRc",
         DistResource.SUDO + "/sbin/chkconfig --level 2345 corosync on"},

        {"Corosync.deleteFromRc",
         DistResource.SUDO + "/sbin/chkconfig --del corosync"},

        {"Openais.addToRc",
         DistResource.SUDO + "/sbin/chkconfig --level 2345 openais on"},

        {"Openais.deleteFromRc",
         DistResource.SUDO + "/sbin/chkconfig --del openais"},
    };
}
