/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.cluster.ui;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import lcmc.cluster.domain.Cluster;

@Named
@Singleton
public class ClusterTabFactory {
    private final ClustersPanel clustersPanel;
    private final Provider<ClusterTab> clusterTabProvider;

    public ClusterTabFactory(ClustersPanel clustersPanel, Provider<ClusterTab> clusterTabProvider) {
        this.clustersPanel = clustersPanel;
        this.clusterTabProvider = clusterTabProvider;
    }

    public ClusterTab createClusterTab(final Cluster cluster) {
        final ClusterTab clusterTab = clusterTabProvider.get();
        clusterTab.initWithCluster(cluster);
        if (cluster != null) {
            clustersPanel.addClusterTab(clusterTab);
        }
        return clusterTab;
    }
}
