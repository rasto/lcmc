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

package drbd.gui.dialog;

import drbd.data.Host;

/**
 * An implementation of an dialog with log files.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class DrbdLog extends Logs {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;
    /** Drbd device name. */
    private final String deviceName;

    /**
     * Prepares a new <code>DrbdLog</code> object.
     */
    public DrbdLog(final Host host, final String device) {
        super(host);
        deviceName = device.substring(device.lastIndexOf('/') + 1) + ":";
    }

    /**
     * Returns a command name from the DistResource that gets the drbd log file.
     * "DrbdLog.log"
     */
    protected String logFileCommand() {
        return "DrbdLog.log";
    }

    /**
     * Returns a pattern that should be searched in the config file. (device
     * name)
     */
    protected String grepPattern() {
        return deviceName;
    }
}
