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

import lcmc.data.Clusters;
import lcmc.data.Cluster;
import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;

import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.UIManager;


import java.awt.GridLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;


/**
 * An implementation of a panel that holds cluster tabs. Clicking on the tab,
 * changes also host that is shown in the terminal panel, to the host, that
 * is active in the cluster.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class ClustersPanel extends JPanel {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** The tabbed pane where the tabs are painted. */
    private JTabbedPane tabbedPane;
    /** Icon of the cluster. */
    private static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(
                                Tools.getDefault("ClustersPanel.ClusterIcon"));
    /** Icon of all clusters. */
    private static final ImageIcon CLUSTERS_ICON = Tools.createImageIcon(
                               Tools.getDefault("ClustersPanel.ClustersIcon"));
    /** Name of all clusters tab. */
    private static final String CLUSTERS_LABEL =
                                Tools.getString("ClustersPanel.ClustersTab");
    /** New empty cluster tab. */
    private final ClusterTab newClusterTab;
    /** Previously selected tab. */
    private ClusterTab prevSelected = null;
    /** Width of the tab border. */
    private static final int TAB_BORDER_WIDTH = 3;

    /** Prepares a new <code>ClustersPanel</code> object. */
    ClustersPanel() {
        super(new GridLayout(1, 1));
        Tools.getGUIData().setClustersPanel(this);
        newClusterTab = new ClusterTab(null);
        setBackground(Tools.getDefaultColor("ClustersPanel.Background"));
        showGUI();
    }

    /** Shows the tabbed pane. */
    private void showGUI() {
        UIManager.put("TabbedPane.selected",
                      Tools.getDefaultColor("ViewPanel.Status.Background"));
        UIManager.put("TabbedPane.foreground", Color.WHITE);
        UIManager.put("TabbedPane.background",
                      Tools.getDefaultColor("ViewPanel.Background"));



        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);

        final MyTabbedPaneUI mtpui = new MyTabbedPaneUI();
        tabbedPane.setUI(mtpui);

        addClustersTab(CLUSTERS_LABEL);
        add(tabbedPane);
        this.setBorder(javax.swing.BorderFactory.createLineBorder(
                        Tools.getDefaultColor("ClustersPanel.Background"),
                        TAB_BORDER_WIDTH));
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        /* Register a change listener.
           This causes terminal panel to show correct host, after clicking on
           the cluster tab. TODO: is this comment right? */
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent evt) {
                final ClusterTab source = prevSelected;
                final JTabbedPane prevSource = (JTabbedPane) evt.getSource();
                prevSelected = (ClusterTab) prevSource.getSelectedComponent();
                String sourceName = null;
                if (source != null) {
                    sourceName = source.getName();
                }
                /* show dialogs only if got here from other tab. */
                if (sourceName == null) {
                    return;
                }

                final ClusterTab clusterTab = getClusterTab();
                if (clusterTab != null) {
                    final Cluster cluster = clusterTab.getCluster();
                    final int danglingHostsCount =
                                    Tools.getConfigData().danglingHostsCount();
                    if (cluster != null) {
                        refresh();
                    }
                }
            }
        });
    }

    /** Adds a new cluster tab. */
    void addTab(final Cluster cluster) {
        Tools.debug(this, "cluster add tab " + cluster.getName(), 2);
        final ClusterTab ct = new ClusterTab(cluster);
        cluster.setClusterTab(ct);
        if (tabbedPane.getTabCount() == 1) {
            removeAllTabs();
        }
        final String title = Tools.join(" ", cluster.getHostNames());
        tabbedPane.addTab(cluster.getName(), CLUSTER_ICON, ct, title);

        final ActionListener disconnectAction =
                         new ActionListener() {
                             @Override
                             public void actionPerformed(final ActionEvent e) {
                                 final Thread t = new Thread(new Runnable() {
                                     @Override
                                     public void run() {
                                         Tools.stopCluster(cluster);
                                         Tools.getGUIData().getEmptyBrowser()
                                                .setDisconnected(cluster);
                                     }
                                 });
                                 t.start();
                             }
                         };

        addTabComponent(tabbedPane,
                        cluster.getName(),
                        CLUSTER_ICON,
                        ct,
                        disconnectAction);
        tabbedPane.setSelectedComponent(ct);
        refresh();
    }

    /** Add tab component with close button. */
    private void addTabComponent(final JTabbedPane tabPane,
                                 final String title,
                                 final ImageIcon icon,
                                 final ClusterTab ct,
                                 final ActionListener actionListener) {
        final int index = tabPane.indexOfComponent(ct);
        final JPanel tabPanel = new JPanel(new GridBagLayout());
        tabPanel.setOpaque(false);
        final JLabel iconLabel = new JLabel(icon);
        final JLabel lblTitle = new JLabel(title);

        final MyButton clusterButton = new MyButton("X");
        clusterButton.setBackgroundColor(Browser.STATUS_BACKGROUND);
        clusterButton.setMargin(new Insets(0, 0, 0, 0));
        clusterButton.setIconTextGap(0);

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        tabPanel.add(iconLabel, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        tabPanel.add(lblTitle, gbc);

        gbc.gridx++;
        gbc.weightx = 0;
        tabPanel.add(clusterButton, gbc);

        tabPane.setTabComponentAt(index, tabPanel);
        clusterButton.addActionListener(actionListener);
    }

    /** Adds an epmty tab, that opens new cluster dialogs. */
    void addClustersTab(final String label) {
        tabbedPane.addTab(label,
                          CLUSTERS_ICON,
                          newClusterTab,
                          Tools.getString("ClustersPanel.ClustersTabTip"));
    }

    /**
     * Removes selected tab, after clicking on the cancel button in the config
     * dialogs.
     */
    void removeTab() {
        final ClusterTab selected = getClusterTab();
        selected.getCluster().setClusterTab(null);
        int index = tabbedPane.getSelectedIndex() - 1;
        if (index < 1) {
            index = 1;
        }
        if (selected != null) {
            tabbedPane.remove(selected);
        }
        if (tabbedPane.getTabCount() == 1) {
            tabbedPane.removeAll();
            addClustersTab(CLUSTERS_LABEL);
        }
    }

    /** Removes specified tab. */
    public void removeTab(final Cluster cluster) {
        tabbedPane.remove(cluster.getClusterTab());
        cluster.setClusterTab(null);
        if (tabbedPane.getTabCount() == 1) {
            tabbedPane.removeAll();
            addClustersTab(CLUSTERS_LABEL);
        }
    }

    /** Removes all tabs. */
    public void removeAllTabs() {
        tabbedPane.removeAll();
        addClustersTab("");
    }

    /** Renames selected added tab. */
    void renameSelectedTab(final String newName) {
        tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), newName);
        refresh();
    }

    /** Adds all cluster tabs, e.g. after loading of configuration. */
    private void addAllTabs() {
        final Clusters clusters = Tools.getConfigData().getClusters();
        addClustersTab(CLUSTERS_LABEL);
        if (clusters != null) {
            for (final Cluster cluster : clusters.getClusterSet()) {
                addTab(cluster);
            }
        }
    }

    /** Refreshes the view. */
    void refresh() {
        tabbedPane.invalidate();
        tabbedPane.validate();
        tabbedPane.repaint();
    }

    /** Removes all tabs and adds them back, also a way to repaint them. */
    void repaintTabs() {
        tabbedPane.removeAll();
        addAllTabs();
    }

    /** Return cluster tab, that is in the JScrollPane. */
    ClusterTab getClusterTab() {
        final Component sp = tabbedPane.getSelectedComponent();
        if (sp == null) {
            return null;
        } else  {
            return (ClusterTab) sp;
        }
    }

    /** This class is used to override the tab look. */
    static class MyTabbedPaneUI
                        extends javax.swing.plaf.basic.BasicTabbedPaneUI {
        /** Sets insets. */
        @Override
        protected final Insets getContentBorderInsets(final int tabPlacement) {
            return new Insets(0, 0, 0, 0);
        }

        /** Overrides the content border painting with nothing. */
        @Override
        protected void paintContentBorder(final Graphics g,
                                          final int tabPlacement,
                                          final int selectedIndex) {
            /* No border */
        }
    }

}
