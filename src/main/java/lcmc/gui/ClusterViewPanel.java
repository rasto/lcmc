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
import lcmc.data.Cluster;
import lcmc.data.ConfigData;
import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;
import lcmc.utilities.AllHostsUpdatable;
import lcmc.EditClusterDialog;

import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Dimension;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import javax.swing.border.TitledBorder;

/**
 * An implementation of a custer view with tree of services.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class ClusterViewPanel extends ViewPanel
                                    implements AllHostsUpdatable {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Cluster data object. */
    private final Cluster cluster;
    /** Background color of the status panel. */
    private static final Color STATUS_BACKGROUND =
                          Tools.getDefaultColor("ViewPanel.Status.Background");

    /** Prepares a new <code>ClusterViewPanel</code> object. */
    ClusterViewPanel(final Cluster cluster) {
        super();
        this.cluster = cluster;
        cluster.createClusterBrowser();
        getTree(cluster.getBrowser());
        cluster.getBrowser().initClusterBrowser();
        cluster.getBrowser().setClusterViewPanel(this);
        /* wizard buttons */
        final JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(STATUS_BACKGROUND);

        /* upgrade field */
        buttonPanel.add(
            Tools.getGUIData().getClustersPanel().registerUpgradeTextField());

        /* button area */
        final JPanel buttonArea = new JPanel(new BorderLayout());
        buttonArea.setBackground(STATUS_BACKGROUND);
        buttonArea.add(buttonPanel, BorderLayout.WEST);
        add(buttonArea, BorderLayout.NORTH);

        allHostsUpdate();
        Tools.getGUIData().registerAllHostsUpdate(this);
    }

    /** This is called when there was added a new host. */
    @Override
    public void allHostsUpdate() {
        cluster.getBrowser().updateClusterResources(
                                                cluster.getHostsArray(),
                                                cluster.getCommonFileSystems(),
                                                cluster.getCommonMountPoints());
    }

    /** Refreshes the cluster data in the view. */
    void refresh() {
        cluster.getBrowser().getTreeModel().reload();
    }

    /** Gets cluster object. */
    Cluster getCluster() {
        return cluster;
    }
}
