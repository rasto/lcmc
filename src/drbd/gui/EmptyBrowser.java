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
import java.awt.Color;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;

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

    /** Color of the most of backgrounds. */
    private static final Color PANEL_BACKGROUND =
                    Tools.getDefaultColor("ViewPanel.Background");

    /**
     * Prepares a new <code>CusterBrowser</code> object.
     */
    public EmptyBrowser() {
        super();
        //heartbeatGraph = new HeartbeatGraph(this);
        setTreeTop();
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
        allHostsNode = new DefaultMutableTreeNode(new AllHostsInfo());
        setNode(allHostsNode);
        topAdd(allHostsNode);
    }

    /**
     * Updates resources of a cluster in the tree.
     */
    public final void updateHosts(final JTree treeMenu) {
        this.treeMenu = treeMenu;
        DefaultMutableTreeNode resource;

        /* all hosts */
        final Host[] allHosts =
                               Tools.getConfigData().getHosts().getHostsArray();
        allHostsNode.removeAllChildren();
        for (Host host : allHosts) {
            final HostBrowser hostBrowser = host.getBrowser();
            resource = new DefaultMutableTreeNode(hostBrowser.getHostInfo());
            setNode(resource);
            allHostsNode.add(resource);
        }
        reload(allHostsNode);
    }

    /**
     * This class holds all hosts that are added to the GUI as opposite to all
     * hosts in a cluster.
     */
    public class AllHostsInfo extends Info {
        /** getInfoPanel() cache. */
        private final JComponent infoPanel = null;
        /** Possibly selected host or null. */
        private final Host host;

        /**
         * Creates a new AllHostsInfo instance.
         */
        public AllHostsInfo() {
            super(Tools.getString("ClusterBrowser.AllHosts"));
            host = null;
        }

        /**
         * Creates a new AllHostsInfo instance, with selected host.
         *
         */
        public AllHostsInfo(final Host host) {
            super(host.getName());
            this.host = host;
        }

        /**
         * Returns info panel of all hosts menu item. If a host is selected,
         * its tab is selected.
         */
        public final JComponent getInfoPanel() {
            if (infoPanel != null) {
                return infoPanel;
            }

            infoPanel = new JPanel();
            infoPanel.setBackground(PANEL_BACKGROUND);
            return infoPanel;
        }
    }

}
