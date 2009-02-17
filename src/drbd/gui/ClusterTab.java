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

import drbd.AddClusterDialog;
import drbd.AddHostDialog;
import drbd.data.Cluster;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;

import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;

/**
 * An implementation of a cluster tab, that contains host views of the hosts,
 * that are in the cluster.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class ClusterTab extends JPanel {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Cluster data object. */
    private final Cluster cluster;
    /** Add cluster button. */
    private MyButton addClusterButton = null;
    /** Add host button. */
    private MyButton addHostButton = null;
    /** Add cluster icon. */
    private static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(
                                    Tools.getDefault("ClusterTab.ClusterIcon"));
    /** Add host icon. */
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(
                                        Tools.getDefault("HostTab.HostIcon"));
    /** Dimension of the big buttons. */
    private static final Dimension BIG_BUTTON_DIMENSION =
                                                    new Dimension(300, 100);
    /**
     * Prepares a new <code>ClusterTab</code> object.
     */
    public ClusterTab(final Cluster cluster) {
        super(new BorderLayout());
        setBackground(Tools.getDefaultColor("ViewPanel.Status.Background"));
        this.cluster = cluster;
        if (cluster == null) {
            /* add new cluster button */
            setLayout(new FlowLayout());
            addClusterButton =
                    new MyButton(Tools.getString("ClusterTab.AddNewCluster"),
                                 CLUSTER_ICON);
            addClusterButton.setBackground(
                            Tools.getDefaultColor("DefaultButton.Background"));
            addClusterButton.setPreferredSize(BIG_BUTTON_DIMENSION);
            addClusterButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(
                        new Runnable() {
                            public void run() {
                                AddClusterDialog acd = new AddClusterDialog();
                            }
                        });
                    thread.start();
                }
            });
            Tools.getGUIData().setAddClusterButton(addClusterButton);
            Tools.getGUIData().checkAddClusterButtons();
            add(addClusterButton);

            /* add new host button */
            addHostButton = new MyButton(
                                    Tools.getString("ClusterTab.AddNewHost"),
                                    HOST_ICON);
            addHostButton.setBackground(
                            Tools.getDefaultColor("DefaultButton.Background"));
            addHostButton.setPreferredSize(BIG_BUTTON_DIMENSION);
            addHostButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(
                        new Runnable() {
                            public void run() {
                                enableButtons(false);
                                final AddHostDialog ahd = new AddHostDialog();
                                ahd.showDialogs();
                                enableButtons(true);
                            }
                        });
                    thread.start();
                }
            });
            add(addHostButton);
            final JLabel logo = new JLabel(
                                        Tools.createImageIcon(
                                                        "startpage_head.jpg"));
            add(logo);
        }
    }

    /**
     * Enables or disables add cluster and add host buttons.
     */
    public void enableButtons(final boolean enable) {
        if (addClusterButton != null) {
            addClusterButton.setEnabled(enable);
        }
        if (addHostButton != null) {
            addHostButton.setEnabled(enable);
        }
    }

    /**
     * adds host views to the desktop.
     */
    public void addClusterView() {
        if (cluster.hostsCount() > 1) {
            add(new ClusterViewPanel(cluster));
        }
        repaint();
    }

    /**
     * Returns cluster object.
     */
    public Cluster getCluster() {
        return cluster;
    }

    /**
     * Returns name of the cluster.
     */
    public String getName() {
        if (cluster == null) {
            return null;
        }
        return cluster.getName();
    }
}
