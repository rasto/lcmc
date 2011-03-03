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

package drbd.utilities;

import drbd.data.Host;
import java.util.Map;
import java.util.HashMap;

/**
 * This class provides LVM commands.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class LVM {
    /** Size placeholder. */
    private static final String SIZE_PH     = "@SIZE@";
    /** Device placeholder. */
    private static final String DEVICE_PH     = "@DEVICE@";

    /** Private constructor, cannot be instantiated. */
    private LVM() {
        /* Cannot be instantiated. */
    }

    /**
     * Executes the specified LVM command on the specified host and calls the
     * supplied callback function.
     */
    private static SSH.SSHOutput execCommand(final Host host,
                                             final String command,
                                             final boolean outputVisible,
                                             final boolean testOnly) {
        return Tools.execCommandProgressIndicator(
                                 host,
                                 command,
                                 null, /* exec callback */
                                 outputVisible,
                                 Tools.getString("LVM.ExecutingCommand")
                                 + " " + command + "...",
                                 SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /** Resize LVM device. */
    public static boolean resize(final Host host,
                                 final String blockDevice,
                                 final String size,
                                 final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(SIZE_PH, size);
        replaceHash.put(DEVICE_PH, blockDevice);
        final String command = host.getDistCommand("LVM.resize", replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }
}
