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
import drbd.data.Cluster;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import drbd.utilities.AllHostsUpdatable;
import drbd.EditClusterDialog;


import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.JLabel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Dimension;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.border.TitledBorder;



/**
 * An implementation of a custer view with tree of services.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class ClusterViewPanel extends ViewPanel implements AllHostsUpdatable {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Cluster data object. */
    private final Cluster cluster;
    /** Background color of the status panel. */
    private static final Color STATUS_BACKGROUND =
                        Tools.getDefaultColor("ViewPanel.Status.Background");
    /** Menu tree object. */
    private final JTree tree;

    /**
     * Prepares a new <code>ClusterViewPanel</code> object.
     */
    public ClusterViewPanel(final Cluster cluster) {
        super();
        this.cluster = cluster;
        cluster.createClusterBrowser();
        tree = getTree(cluster.getBrowser());
        cluster.getBrowser().initClusterBrowser();
        cluster.getBrowser().setClusterViewPanel(this);

        /* wizard buttons */
        final JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(STATUS_BACKGROUND);
        /* cluster wizard */
        final MyButton clusterWizardButton = new MyButton(
                            Tools.getString("ClusterViewPanel.ClusterWizard"));
        clusterWizardButton.setPreferredSize(new Dimension(130, 20));
        clusterWizardButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    public void run() {
                        final EditClusterDialog dialog =
                                        new EditClusterDialog(cluster);
                        dialog.showDialogs();
                    }
                });
                t.start();
            }
        });

        final JPanel clusterButtonsPanel = new JPanel();
        clusterButtonsPanel.setBackground(STATUS_BACKGROUND);
        final TitledBorder titledBorder = Tools.getBorder(
                        Tools.getString("ClusterViewPanel.ClusterButtons"));
        clusterButtonsPanel.setBorder(titledBorder);

        clusterButtonsPanel.add(clusterWizardButton);
        clusterButtonsPanel.add(Tools.expertModeButton());
        buttonPanel.add(clusterButtonsPanel);
        /* upgrade field */
        buttonPanel.add(
            Tools.getGUIData().getClustersPanel().registerUpgradeTextField());

        /* button area */
        final JPanel buttonArea = new JPanel(new BorderLayout());
        buttonArea.setBackground(STATUS_BACKGROUND);
        buttonArea.add(buttonPanel, BorderLayout.WEST);
        final JLabel logo = new JLabel(Tools.createImageIcon(
                                  Tools.getDefault("ClusterViewPanel.Logo")));
        final JPanel l = new JPanel(new BorderLayout());
        l.setBackground(Tools.getDefaultColor("ViewPanel.Status.Background"));
        l.add(logo, BorderLayout.NORTH);
        buttonArea.add(l, BorderLayout.EAST);
        add(buttonArea, BorderLayout.NORTH);

        allHostsUpdate();
        Tools.getGUIData().registerAllHostsUpdate(this);
    }

    /**
     * This is called when there was added a new host.
     */
    public final void allHostsUpdate() {
        cluster.getBrowser().updateClusterResources(
                                                cluster.getHostsArray(),
                                                cluster.getCommonFileSystems(),
                                                cluster.getCommonMountPoints());
    }

    /**
     * Refreshes the cluster data in the view.
     */
    public final void refresh() {
        cluster.getBrowser().getTreeModel().reload();
    }

    /**
     * Gets cluster object.
     */
    public final Cluster getCluster() {
        return cluster;
    }
}
