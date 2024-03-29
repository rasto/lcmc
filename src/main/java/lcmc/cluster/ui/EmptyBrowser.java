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

import java.util.TreeSet;
import java.util.function.BiConsumer;

import javax.annotation.Resource;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import lcmc.cluster.domain.Cluster;
import lcmc.cluster.ui.network.InfoPresenter;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.common.ui.CategoryInfo;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.host.domain.Host;
import lcmc.host.domain.Hosts;
import lcmc.host.ui.AllHostsInfo;
import lcmc.host.ui.HostBrowser;

/**
 * This class holds cluster resource data in a tree. It shows panels that allow
 * to edit data of services etc.
 * Every resource has its Info object, that accessible through the tree view.
 */
@Named
@Singleton
public final class EmptyBrowser extends Browser {
    /**
     * Menu's all hosts node.
     */
    private DefaultMutableTreeNode allHostsNode;
    private DefaultMutableTreeNode treeTop;
    private final AllHostsInfo allHostsInfo;
    private final Hosts allHosts;
    private final TreeMenuController treeMenuController;
    @Resource(name = "categoryInfo")
    private CategoryInfo resourcesCategory;

    public EmptyBrowser(Application application, AllHostsInfo allHostsInfo, Hosts allHosts, TreeMenuController treeMenuController) {
        super(application);
        this.allHostsInfo = allHostsInfo;
        this.allHosts = allHosts;
        this.treeMenuController = treeMenuController;
    }

    void init() {
        allHostsInfo.init(this);
        resourcesCategory.init(Tools.getString("Browser.Resources"), null);
        treeTop = treeMenuController.createMenuTreeTop(resourcesCategory);
    }

    /**
     * Adds small box with cluster possibility to load it and remove it.
     */
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
        allHostsNode = treeMenuController.createMenuItem(treeTop, allHostsInfo);
    }

    /** Updates resources of a cluster in the tree. */
    void updateHosts() {
        final Iterable<Host> allHostsSorted = new TreeSet<>(allHosts.getHostSet());
        treeMenuController.removeChildren(allHostsNode);
        for (final Host host : allHostsSorted) {
            final HostBrowser hostBrowser = host.getBrowser();
            final MutableTreeNode resource = treeMenuController.createMenuItem(allHostsNode, hostBrowser.getHostInfo());
        }
        treeMenuController.reloadNodeDontSelect(allHostsNode);
        treeMenuController.expandAndSelect(new Object[]{treeTop, allHostsNode});
    }

    @Override
    public void fireEventInViewPanel(final DefaultMutableTreeNode node) {
        if (node != null) {
            treeMenuController.reloadNode(node);
            treeMenuController.nodeChanged(node);
        }
    }

    public void setDisabledDuringLoad(boolean disable) {
        treeMenuController.setDisableListeners(disable);
    }

    public void reloadTreeMenu(final DefaultMutableTreeNode node) {
        treeMenuController.reloadNodeDontSelect(node);
        treeMenuController.repaintMenuTree();
    }

    JTree createTreeMenu(final BiConsumer<InfoPresenter, Boolean> listener) {
        final JTree tree = treeMenuController.getMenuTree();
        treeMenuController.addListeners(listener);
        return tree;
    }
}

