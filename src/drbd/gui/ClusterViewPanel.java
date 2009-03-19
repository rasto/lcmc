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
    /** Hb status play/stop button. */
    private final MyButton hbPlayStopButton;
    /** Hb status play/stop button foreground color. */
    private final Color hbPlayButtonColor;
    /** Hb status play/stop button background color. */
    private final Color hbPlayButtonBgColor;
    /** DRBD status play/stop button. */
    private final MyButton drbdPlayStopButton;
    /** DRBD status play/stop button foreground color. */
    private final Color drbdPlayButtonColor;
    /** DRBD status play/stop button background color. */
    private final Color drbdPlayButtonBgColor;
    /** Background color of the status panel. */
    private static final Color STATUS_BACKGROUND =
                        Tools.getDefaultColor("ViewPanel.Status.Background");
    /**
     * Prepares a new <code>ClusterViewPanel</code> object.
     */
    public ClusterViewPanel(final Cluster cluster) {
        super();
        this.cluster = cluster;
        cluster.createClusterBrowser();
        cluster.getBrowser().setClusterViewPanel(this);

        /* hb status buttons */
        hbPlayStopButton = new MyButton(
                        Tools.getString("ClusterViewPanel.StatusHbStopButton"));
        hbPlayStopButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final String command = e.getActionCommand();
                        if (command.equals(Tools.getString(
                                    "ClusterViewPanel.StatusHbPlayButton"))) {
                            hbStatusPlay();
                        } else if (command.equals(Tools.getString(
                                    "ClusterViewPanel.StatusHbStopButton"))) {
                            hbStatusStop();
                        }
                    }
                });

        hbPlayButtonColor   = hbPlayStopButton.getForeground();
        hbPlayButtonBgColor = hbPlayStopButton.getBackground();
        hbPlayStopButton.setForeground(Color.RED);

        final JPanel buttonPanel = new JPanel(new FlowLayout());
        //buttonPanel.setMinimumSize(new Dimension(0, 50));
        //buttonPanel.setPreferredSize(new Dimension(0, 50));
        //buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        buttonPanel.setBackground(STATUS_BACKGROUND);

        final JPanel hbStatusPanel = new JPanel();
        hbStatusPanel.setBackground(STATUS_BACKGROUND);
        TitledBorder titledBorder = Tools.getBorder(Tools.getString(
                                    "ClusterViewPanel.StatusHbButtonsTitle"));
        hbStatusPanel.setBorder(titledBorder);
        hbPlayStopButton.setPreferredSize(new Dimension(130, 20));

        hbStatusPanel.add(hbPlayStopButton);
        buttonPanel.add(hbStatusPanel);

        /* drbd status buttons */
        drbdPlayStopButton = new MyButton(
                    Tools.getString("ClusterViewPanel.StatusDrbdStopButton"));
        drbdPlayStopButton.setPreferredSize(new Dimension(130, 20));
        drbdPlayStopButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final String command = e.getActionCommand();
                        if (command.equals(Tools.getString(
                                    "ClusterViewPanel.StatusDrbdPlayButton"))) {
                            drbdStatusPlay();
                        } else if (command.equals(Tools.getString(
                                    "ClusterViewPanel.StatusDrbdStopButton"))) {
                            drbdStatusStop();
                        }
                    }
                });

        drbdPlayButtonColor   = drbdPlayStopButton.getForeground();
        drbdPlayButtonBgColor = drbdPlayStopButton.getBackground();
        drbdPlayStopButton.setForeground(Color.RED);

        final JPanel drbdStatusPanel = new JPanel();
        drbdStatusPanel.setBackground(STATUS_BACKGROUND);
        titledBorder = Tools.getBorder(
                    Tools.getString("ClusterViewPanel.StatusDrbdButtonsTitle"));
        drbdStatusPanel.setBorder(titledBorder);

        drbdStatusPanel.add(drbdPlayStopButton);
        buttonPanel.add(drbdStatusPanel);

        /* cluster buttons */
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
        titledBorder = Tools.getBorder(
                        Tools.getString("ClusterViewPanel.ClusterButtons"));
        clusterButtonsPanel.setBorder(titledBorder);

        clusterButtonsPanel.add(clusterWizardButton);
        buttonPanel.add(clusterButtonsPanel);

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

        createClusterView();
        Tools.getGUIData().registerAllHostsUpdate(this);
    }

    /**
     * creates cluster view and updates the tree.
     */
    private void createClusterView() {

        final JTree tree = getTree(cluster.getBrowser());
        cluster.getBrowser().updateClusterResources(
                                                tree,
                                                cluster.getHostsArray(),
                                                cluster.getCommonFileSystems(),
                                                cluster.getCommonMountPoints(),
                                                this);
    }

    /**
     * This is called when there was added a new host.
     */
    public final void allHostsUpdate() {
        createClusterView(); // TODO: should reload just all hosts
    }

    /**
     * Start automatic hb status updates.
     */
    public final void hbStatusPlay() {
        final ClusterViewPanel thisClass = this;
        final Thread thread = new Thread(
            new Runnable() {
                public void run() {
                    hbPlayStopButton.setEnabled(false);
                    hbPlayStopButton.setText(Tools.getString(
                                    "ClusterViewPanel.StatusHbStopButton"));
                    hbPlayStopButton.setForeground(Color.RED);
                    hbPlayStopButton.setBackground(hbPlayButtonBgColor);
                    cluster.getBrowser().startHbStatus(thisClass);
                }
            }
        );
        thread.start();
    }

    /**
     * Enables the hb status button.
     */
    public final void hbStatusButtonEnable() {
        hbPlayStopButton.setEnabled(true);
    }

    /**
     * Stop automatic hb status updates.
     */
    public final void hbStatusStop() {
        hbPlayStopButton.setEnabled(false);
        hbPlayStopButton.setText(Tools.getString(
                                    "ClusterViewPanel.StatusHbPlayButton"));
        hbPlayStopButton.setForeground(hbPlayButtonColor);
        final Runnable runnable = new Runnable() {
            public void run() {
                cluster.getBrowser().stopHbStatus();
                hbPlayStopButton.setEnabled(true);
            }
        };
        final Thread thread = new Thread(runnable);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /**
     * Should be called if hb status failed.
     */
    public final void hbStatusStopFailed() {
        hbPlayStopButton.setBackground(Color.RED);
        hbStatusStop();
    }

    /**
     * Start automatic drbd status updates.
     */
    public final void drbdStatusPlay() {
        final ClusterViewPanel thisClass = this;
        final Thread thread = new Thread(
            new Runnable() {
                public void run() {
                    drbdPlayStopButton.setEnabled(false);
                    drbdPlayStopButton.setText(Tools.getString(
                                    "ClusterViewPanel.StatusDrbdStopButton"));
                    drbdPlayStopButton.setForeground(Color.RED);
                    drbdPlayStopButton.setBackground(drbdPlayButtonBgColor);
                    cluster.getBrowser().startDrbdStatus(thisClass);
                }
            }
        );
        thread.start();
    }

    /**
     * Enables the drbd status button.
     */
    public final void drbdStatusButtonEnable() {
        drbdPlayStopButton.setEnabled(true);
    }

    /**
     * Stop automatic drbd status updates.
     */
    public final void drbdStatusStop() {
        drbdPlayStopButton.setEnabled(false);

        drbdPlayStopButton.setText(
                    Tools.getString("ClusterViewPanel.StatusDrbdPlayButton"));
        drbdPlayStopButton.setForeground(drbdPlayButtonColor);
        final Runnable runnable = new Runnable() {
            public void run() {
                cluster.getBrowser().stopDrbdStatus();
                drbdPlayStopButton.setEnabled(true);
            }
        };
        final Thread thread = new Thread(runnable);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
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
