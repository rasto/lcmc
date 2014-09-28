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
package lcmc.gui;

import java.util.TreeSet;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import lcmc.common.domain.Application;
import lcmc.cluster.domain.Cluster;
import lcmc.host.domain.Host;
import lcmc.gui.resources.AllHostsInfo;
import lcmc.host.domain.Hosts;

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
    @Inject
    private AllHostsInfo allHostsInfo;
    @Inject
    private Application application;
    @Inject
    private Hosts allHosts;

    void init() {
        allHostsInfo.init(this);
        setMenuTreeTop();
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
        allHostsNode = new DefaultMutableTreeNode(allHostsInfo);
        setNode(allHostsNode);
        topLevelAdd(allHostsNode);
    }

    /** Updates resources of a cluster in the tree. */
    void updateHosts() {
        final Iterable<Host> allHostsSorted = new TreeSet<Host>(allHosts.getHostSet());
        application.invokeLater(new Runnable() {
            @Override
            public String toString() {
                return super.toString();
            }

            @Override
            public void run() {
                allHostsNode.removeAllChildren();
                for (final Host host : allHostsSorted) {
                    final HostBrowser hostBrowser = host.getBrowser();
                    final MutableTreeNode resource = new DefaultMutableTreeNode(hostBrowser.getHostInfo());
                    //setNode(resource);
                    allHostsNode.add(resource);
                }
            }
        });
        reloadNode(allHostsNode, false);
        selectPath(new Object[]{getTreeTop(), allHostsNode});
    }
}

