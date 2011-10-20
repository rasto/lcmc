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

package lcmc.gui.dialog;

import lcmc.utilities.Tools;
import lcmc.data.Host;

/**
 * An implementation of an dialog, with log files.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class HostLogs extends Logs {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Host from which we get the log/logs. */
    private final Host host;

    /** Prepares a new <code>HostLogs</code> object. */
    public HostLogs(final Host host) {
        super();
        this.host = host;
    }

    /** Returns the host. */
    protected final Host getHost() {
        return host;
    }

    /** Returns this host. */
    @Override protected final Host[] getHosts() {
        return new Host[]{host};
    }

    /**
     * Returns a command name from the DistResource that gets the drbd log file.
     * "HostLogs.hbLog"
     */
    @Override protected String logFileCommand() {
        return "Logs.hbLog";
    }

    /**
     * Gets the title of the dialog, defined as Dialog.Logs.Title in
     * TextResources.
     */
    @Override protected final String getDialogTitle() {
        return Tools.getString("Dialog.HostLogs.Title");
    }
}
