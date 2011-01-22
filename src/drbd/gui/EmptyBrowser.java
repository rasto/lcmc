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

package drbd.gui;

import drbd.utilities.Tools;
import drbd.data.Host;
import drbd.data.Cluster;
import drbd.gui.resources.Info;
import drbd.gui.resources.AllHostsInfo;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JTree;
import javax.swing.SwingUtilities;

/**
 * This class holds cluster resource data in a tree. It shows panels that allow
 * to edit data of services etc.
 * Every resource has its Info object, that accessible through the tree view.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class EmptyBrowser extends Browser {
    /** Menu's all hosts node. */
    private DefaultMutableTreeNode allHostsNode;
    /** Panel that holds this browser. */
    private EmptyViewPanel emptyViewPanel = null;
    /** Tree menu root. */
    private JTree treeMenu;
    /** All hosts info object of the host of this browser. */
    private final AllHostsInfo allHostsInfo = new AllHostsInfo(this);

    /**
     * Prepares a new <code>CusterBrowser</code> object.
     */
    public EmptyBrowser() {
        super();
        /* Load the default file */
        final String saveFile = Tools.getConfigData().getSaveFile();
        final String xml = Tools.loadFile(saveFile, false);
        if (xml != null) {
            Tools.loadXML(xml);
        }
        setTreeTop();
    }

    /**
     * Adds small box with cluster possibility to load it and remove it.
     */
    public final void addClusterBox(final Cluster cluster) {
        allHostsInfo.addClusterBox(cluster);
        allHostsInfo.setAsStarted(cluster);
        allHostsInfo.addCheckboxListener(cluster);
    }

    /**
     * Sets the empty view panel.
     */
    public final void setEmptyViewPanel(final EmptyViewPanel emptyViewPanel) {
        this.emptyViewPanel = emptyViewPanel;
    }

    /**
     * Returns empty view panel.
     */
    public final EmptyViewPanel getEmptyViewPanel() {
        return emptyViewPanel;
    }

    /**
     * Sets the info panel component in the cluster view panel.
     */
    public final void setRightComponentInView(final Info i) {
        emptyViewPanel.setRightComponentInView(this, i);
    }

    /**
     * Initializes hosts tree for the empty view.
     */
    public final void initHosts() {
        /* all hosts */
        allHostsNode = new DefaultMutableTreeNode(allHostsInfo);
        setNode(allHostsNode);
        topAdd(allHostsNode);
    }

    /**
     * Updates resources of a cluster in the tree.
     */
    public final void updateHosts(final JTree treeMenu) {
        this.treeMenu = treeMenu;

        /* all hosts */
        final Host[] allHosts =
                              Tools.getConfigData().getHosts().getHostsArray();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                DefaultMutableTreeNode resource;
                allHostsNode.removeAllChildren();
                for (Host host : allHosts) {
                    final HostBrowser hostBrowser = host.getBrowser();
                    resource = new DefaultMutableTreeNode(
                                                    hostBrowser.getHostInfo());
                    //setNode(resource);
                    allHostsNode.add(resource);
                }
            }
        });
        reload(allHostsNode, false);
        selectPath(new Object[]{getTreeTop(), allHostsNode});
    }
}

