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


package drbd.gui.dialog.drbd;

import drbd.data.Host;
import drbd.gui.dialog.HostLogs;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;

/**
 * An implementation of an dialog with log files.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class DrbdsLog extends HostLogs {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Prepares a new <code>DrbdsLog</code> object.
     */
    public DrbdsLog(final Host host) {
        super(host);
    }

    /**
     * Returns a command name from the DistResource that gets the drbd log file.
     */
    protected final String logFileCommand() {
        return "DrbdLog.log";
    }

    /** Returns which pattern names are selected by default. */
    protected final Set<String> getSelectedSet() {
        final Set<String> selected = new HashSet<String>();
        selected.add("drbd");
        return selected;
    }

    /** Returns a map from pattern name to its pattern. */
    protected final Map<String, String> getPatternMap() {
        final Map<String, String> pm = new LinkedHashMap<String, String>();
        pm.put("drbd", wordBoundary("drbd"));
        return pm;
    }
}
