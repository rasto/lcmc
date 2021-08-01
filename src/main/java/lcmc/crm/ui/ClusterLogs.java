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


package lcmc.crm.ui;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import lcmc.cluster.domain.Cluster;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.ui.Logs;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;

/**
 * An implementation of an dialog with log files from many hosts.
 */
@Named
public class ClusterLogs extends Logs {
    private Cluster cluster;

    public ClusterLogs(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData) {
        super(application, swingUtils, widgetFactory, mainData);
    }

    public void init(final Cluster cluster) {
        this.cluster = cluster;
    }

    protected final Cluster getCluster() {
        return cluster;
    }

    @Override
    protected final Host[] getHosts() {
        return cluster.getHostsArray();
    }

    /** Returns a map from pattern name to its pattern. */
    @Override
    protected Map<String, String> getPatternMap() {
        final Map<String, String> patternMap = new LinkedHashMap<>();
        patternMap.put("lrmd", wordBoundary("lrmd"));
        patternMap.put("crmd", wordBoundary("crmd"));
        patternMap.put("pengine", wordBoundary("pengine"));
        patternMap.put("ERROR", wordBoundary("ERROR"));
        return patternMap;
    }

    @Override
    protected Set<String> getSelectedSet() {
        final Set<String> selected = new HashSet<>();
        selected.add("ERROR");
        return selected;
    }
}
