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
package lcmc.cluster.ui;
import java.awt.BorderLayout;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.Box;
import lcmc.cluster.domain.Cluster;
import lcmc.common.ui.GUIData;
import lcmc.common.ui.ViewPanel;
import lcmc.common.domain.AllHostsUpdatable;

/**
 * An implementation of a custer view with tree of services.
 */
@Named
public class ClusterViewPanel extends ViewPanel implements AllHostsUpdatable {
    private Cluster cluster;

    @Inject
    private ClusterBrowser clusterBrowser;
    @Inject
    private GUIData guiData;

    public void init(final Cluster cluster) {
        this.cluster = cluster;

        clusterBrowser.init(cluster);
        cluster.setBrowser(clusterBrowser);
        getTree(clusterBrowser);
        cluster.getBrowser().initClusterBrowser();
        cluster.getBrowser().setClusterViewPanel(this);
        add(Box.createVerticalStrut(4), BorderLayout.PAGE_START);

        allHostsUpdate();
        guiData.registerAllHostsUpdate(this);
    }

    /** This is called when there was added a new host. */
    @Override
    public void allHostsUpdate() {
        cluster.getBrowser().updateClusterResources(cluster.getHostsArray(),
                                                    cluster.getCommonFileSystems(),
                                                    cluster.getCommonMountPoints());
    }

    Cluster getCluster() {
        return cluster;
    }
}
