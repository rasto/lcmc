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


package lcmc.gui.dialog.pacemaker;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lcmc.data.Cluster;
import lcmc.gui.dialog.ClusterLogs;

/**
 * An implementation of an dialog with log files from many hosts.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class ServiceLogs extends ClusterLogs {
    /** Service type. e.g. Filesystem. */
    private final String serviceType;
    /** Service heartbeat id. */
    private final String serviceHbId;

    /** Prepares a new {@code ServiceLogs} object. */
    public ServiceLogs(final Cluster cluster,
                       final String serviceType,
                       final String serviceHbId) {
        super(cluster);
        this.serviceType = serviceType;
        this.serviceHbId = serviceHbId;
    }

    /** Returns a map from pattern name to its pattern. */
    @Override
    protected Map<String, String> getPatternMap() {
        final Map<String, String> pm = new LinkedHashMap<String, String>();
        pm.put("lrmd", wordBoundary("lrmd"));
        pm.put(serviceType, wordBoundary(serviceType));
        pm.put(serviceHbId, wordBoundary(serviceHbId));
        pm.put("ERROR", wordBoundary("ERROR"));
        return pm;
    }

    /** Returns which pattern names are selected by default. */
    @Override
    protected Set<String> getSelectedSet() {
        final Set<String> selected = new HashSet<String>();
        selected.add(serviceType); // TODO: till pacemaker 1.0.8
        selected.add("ERROR");
        return selected;
    }
}
