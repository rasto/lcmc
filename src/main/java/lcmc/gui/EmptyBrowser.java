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

import lcmc.utilities.Tools;
import lcmc.data.Host;
import lcmc.data.Cluster;
import lcmc.gui.resources.Info;
import lcmc.gui.resources.AllHostsInfo;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class holds cluster resource data in a tree. It shows panels that allow
 * to edit data of services etc.
 * Every resource has its Info object, that accessible through the tree view.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class EmptyBrowser extends Browser {
    /** Menu's all hosts node. */
    private DefaultMutableTreeNode allHostsNode;
    /** Panel that holds this browser. */
    private EmptyViewPanel emptyViewPanel = null;
    /** All hosts info object of the host of this browser. */
    private final AllHostsInfo allHostsInfo = new AllHostsInfo(this);

    /** Prepares a new <code>CusterBrowser</code> object. */
    EmptyBrowser() {
        super();
        /* Load the default file */
        final String saveFile = Tools.getConfigData().getSaveFile();
        String xml = Tools.loadFile(saveFile, false);
        if (xml == null) {
            final String saveFileOld = Tools.getConfigData().getSaveFileOld();
            xml = Tools.loadFile(saveFileOld, false);
        }
        if (xml != null) {
            Tools.loadXML(xml);
        }
        setTreeTop();
    }

    /** Adds small box with cluster possibility to load it and remove it. */
    public void addClusterBox(final Cluster cluster) {
        allHostsInfo.addClusterBox(cluster);
        allHostsInfo.setConnected(cluster);
        allHostsInfo.addCheckboxListener(cluster);
    }

    /** Set cluster as disconnected. */
    public void setDisconnected(final Cluster cluster) {
        allHostsInfo.setDisconnected(cluster);
    }

    /** Sets the empty view panel. */
    void setEmptyViewPanel(final EmptyViewPanel emptyViewPanel) {
        this.emptyViewPanel = emptyViewPanel;
    }

    /** Returns empty view panel. */
    EmptyViewPanel getEmptyViewPanel() {
        return emptyViewPanel;
    }

    /** Sets the info panel component in the cluster view panel. */
    void setRightComponentInView(final Info i) {
        emptyViewPanel.setRightComponentInView(this, i);
    }

    /** Initializes hosts tree for the empty view. */
    void initHosts() {
        /* all hosts */
        allHostsNode = new DefaultMutableTreeNode(allHostsInfo);
        setNode(allHostsNode);
        topAdd(allHostsNode);
    }

    /** Updates resources of a cluster in the tree. */
    void updateHosts() {
        /* all hosts */
        final Host[] allHosts =
                              Tools.getConfigData().getHosts().getHostsArray();
        Tools.invokeLater(new Runnable() {
            @Override
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

