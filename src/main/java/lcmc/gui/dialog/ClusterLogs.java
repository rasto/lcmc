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
import lcmc.data.Cluster;
import lcmc.data.Host;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * An implementation of an dialog with log files from many hosts.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class ClusterLogs extends Logs {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;
    /** Cluster object. */
    private final Cluster cluster;

    /** Prepares a new <code>ClusterLogs</code> object. */
    public ClusterLogs(final Cluster cluster) {
        super();
        this.cluster = cluster;
    }

    /** Returns the cluster object. */
    protected final Cluster getCluster() {
        return cluster;
    }

    /** Returns all hosts in the cluster. */
    @Override
    protected final Host[] getHosts() {
        return cluster.getHostsArray();
    }

    /**
     * Command that gets the log. The command must be specified in the
     * DistResource or some such.
     */
    @Override
    protected String logFileCommand() {
        return "Logs.hbLog";
    }

    /** Gets the title of the dialog as string. */
    @Override
    protected final String getDialogTitle() {
        return Tools.getString("Dialog.ClusterLogs.Title");
    }

    /** Returns a map from pattern name to its pattern. */
    @Override
    protected Map<String, String> getPatternMap() {
        final Map<String, String> pm = new LinkedHashMap<String, String>();
        pm.put("lrmd", wordBoundary("lrmd"));
        pm.put("crmd", wordBoundary("crmd"));
        pm.put("pengine", wordBoundary("pengine"));
        pm.put("ERROR", wordBoundary("ERROR"));
        return pm;
    }

    /** Returns which pattern names are selected by default. */
    @Override
    protected Set<String> getSelectedSet() {
        final Set<String> selected = new HashSet<String>();
        selected.add("ERROR");
        return selected;
    }
}
