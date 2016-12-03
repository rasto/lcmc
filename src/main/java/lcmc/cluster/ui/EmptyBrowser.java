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

import lcmc.cluster.domain.Cluster;
import lcmc.common.ui.Browser;
import lcmc.common.ui.treemenu.EmptyTreeMenu;
import lcmc.host.domain.Host;
import lcmc.host.domain.Hosts;
import lcmc.host.ui.AllHostsInfo;
import lcmc.host.ui.HostBrowser;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.TreeSet;

/**
 * This class holds cluster resource data in a tree. It shows panels that allow
 * to edit data of services etc.
 * Every resource has its Info object, that accessible through the tree view.
 */
@Named
@Singleton
public final class EmptyBrowser extends Browser {
    /** Menu's all hosts node. */
    private DefaultMutableTreeNode allHostsNode;
    private DefaultMutableTreeNode treeTop;
    @Inject
    private AllHostsInfo allHostsInfo;
    @Inject
    private Hosts allHosts;
    @Inject
    private EmptyTreeMenu emptyTreeMenu;

    void init() {
        allHostsInfo.init(this);
        treeTop = emptyTreeMenu.createMenuTreeTop();
    }

    /** Adds small box with cluster possibility to load it and remove it. */
    public void addClusterBox(final Cluster cluster) {
        allHostsInfo.addClusterBox(cluster);
        allHostsInfo.setConnected(cluster);
        allHostsInfo.addCheckboxListener(cluster);
    }

    public void setDisconnected(final Cluster cluster) {
        allHostsInfo.setDisconnected(cluster);
    }

    /** Initializes hosts tree for the empty view. */
    void initHosts() {
        allHostsNode = emptyTreeMenu.createMenuItem(treeTop, allHostsInfo);
    }

    /** Updates resources of a cluster in the tree. */
    void updateHosts() {
        final Iterable<Host> allHostsSorted = new TreeSet<Host>(allHosts.getHostSet());
        emptyTreeMenu.removeChildren(allHostsNode);
        for (final Host host : allHostsSorted) {
            final HostBrowser hostBrowser = host.getBrowser();
            final MutableTreeNode resource = emptyTreeMenu.createMenuItem(allHostsNode, hostBrowser.getHostInfo());
        }
        emptyTreeMenu.reloadNodeDontSelect(allHostsNode);
        emptyTreeMenu.expandAndSelect(new Object[]{treeTop, allHostsNode});
    }

    @Override
    public void fireEventInViewPanel(final DefaultMutableTreeNode node) {
        if (node != null) {
            emptyTreeMenu.reloadNode(node);
            emptyTreeMenu.nodeChanged(node);
        }
    }
}

