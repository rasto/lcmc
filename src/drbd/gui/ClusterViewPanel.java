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
import drbd.data.ConfigData;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import drbd.utilities.AllHostsUpdatable;
import drbd.EditClusterDialog;


import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.JLabel;
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
import javax.swing.SwingUtilities;



/**
 * An implementation of a custer view with tree of services.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class ClusterViewPanel extends ViewPanel implements AllHostsUpdatable {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Cluster data object. */
    private final Cluster cluster;
    /** Background color of the status panel. */
    private static final Color STATUS_BACKGROUND =
                          Tools.getDefaultColor("ViewPanel.Status.Background");
    /** Menu tree object. */
    private final JTree tree;
    /** Combo box with operating modes. */
    private final JComboBox operatingModesCB;
    /** Advanced mode button. */
    private final JCheckBox advancedModeCB;

    /** Prepares a new <code>ClusterViewPanel</code> object. */
    ClusterViewPanel(final Cluster cluster) {
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
        clusterWizardButton.setPreferredSize(new Dimension(150, 20));
        clusterWizardButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override public void run() {
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
        buttonPanel.add(clusterButtonsPanel);

        /* advanced mode button */
        advancedModeCB = createAdvancedModeButton();
        /* Operating mode */
        final JPanel opModePanel = new JPanel();
        opModePanel.setBackground(STATUS_BACKGROUND);
        final TitledBorder vmBorder = Tools.getBorder(
                          Tools.getString("ClusterViewPanel.OperatingMode"));
        opModePanel.setBorder(vmBorder);
        final String[] modes = Tools.getConfigData().getOperatingModes();
        final JComboBox opModeCB = new JComboBox(modes);

        final ConfigData.AccessType accessType =
                                        Tools.getConfigData().getAccessType();
        opModeCB.setSelectedItem(ConfigData.OP_MODES_MAP.get(accessType));
        opModeCB.addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                final String opMode = (String) e.getItem();
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override public void run() {
                            ConfigData.AccessType type =
                                        ConfigData.ACCESS_TYPE_MAP.get(opMode);
                            if (type == null) {
                                Tools.appError("unknown mode: " + opMode);
                                type = ConfigData.AccessType.RO;
                            }
                            Tools.getConfigData().setAccessType(type);
                            Tools.getGUIData().setOperatingModeGlobally(cluster,
                                                                        opMode);
                            cluster.getBrowser().checkAccessOfEverything();
                        }
                    });
                    thread.start();
                }
            }
        });
        opModePanel.add(opModeCB);
        opModePanel.add(advancedModeCB);
        buttonPanel.add(opModePanel);
        operatingModesCB = opModeCB;

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

    /** Returns advanced mode check box. That hides advanced options. */
    JCheckBox createAdvancedModeButton() {
        final JCheckBox emCB = new JCheckBox(Tools.getString(
                                                      "Browser.AdvancedMode"));
        emCB.setBackground(Tools.getDefaultColor(
                                            "ViewPanel.Status.Background"));
        emCB.setSelected(Tools.getConfigData().isAdvancedMode());
        emCB.addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                final boolean selected =
                                    e.getStateChange() == ItemEvent.SELECTED;
                if (selected != Tools.getConfigData().isAdvancedMode()) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override public void run() {
                            Tools.getConfigData().setAdvancedMode(selected);
                            Tools.getGUIData().setAdvancedModeGlobally(
                                                                     cluster,
                                                                     selected);
                            cluster.getBrowser().checkAccessOfEverything();
                        }
                    });
                    thread.start();
                }
            }
        });
        return emCB;
    }

    /** This is called when there was added a new host. */
    @Override public void allHostsUpdate() {
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

    /** Modify the operating modes combo box according to the godmode. */
    void resetOperatingModes(final boolean godMode) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (godMode) {
                    operatingModesCB.addItem(ConfigData.OP_MODE_GOD);
                    operatingModesCB.setSelectedItem(ConfigData.OP_MODE_GOD);
                } else {
                    operatingModesCB.removeItem(ConfigData.OP_MODE_GOD);
                }
            }
        });
    }

    /** Sets operating mode. */
    void setOperatingMode(final String opMode) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                operatingModesCB.setSelectedItem(opMode);
            }
        });
    }

    /** Sets advanced mode. */
    void setAdvancedMode(final boolean advancedMode) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                advancedModeCB.setSelected(advancedMode);
            }
        });
    }
}
