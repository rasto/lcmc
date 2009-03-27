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
import drbd.data.Clusters;
import drbd.data.Cluster;
import drbd.utilities.Tools;

import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import java.awt.GridLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.UIManager;

/**
 * An implementation of a panel that holds cluster tabs. Clicking on the tab,
 * changes also host that is shown in the terminal panel, to the host, that
 * is active in the cluster.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class ClustersPanel extends JPanel {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** The tabbed pane where the tabs are painted. */
    private JTabbedPane tabbedPane;
    /** Icon of the cluster. */
    private static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(
                                Tools.getDefault("ClustersPanel.ClusterIcon"));
    /** New empty cluster tab. */
    private static final ClusterTab NEW_CLUSTER_TAB = new ClusterTab(null);
    /** Previously selected tab. */
    private ClusterTab prevSelected = null;
    /** Width of the tab border. */
    private static final int TAB_BORDER_WIDTH = 3;

    /**
     * Prepares a new <code>ClustersPanel</code> object.
     */
    public ClustersPanel() {
        super(new GridLayout(1, 1));
        setBackground(Tools.getDefaultColor("ClustersPanel.Background"));
        showGUI();
    }

    /**
     * Shows the tabbed pane.
     */
    private void showGUI() {
        Tools.getGUIData().setClustersPanel(this);
        UIManager.put("TabbedPane.selected",
                      Tools.getDefaultColor("ViewPanel.Status.Background"));
        UIManager.put("TabbedPane.foreground", Color.WHITE);
        UIManager.put("TabbedPane.background",
                      Tools.getDefaultColor("ViewPanel.Background"));



        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);

        final MyTabbedPaneUI mtpui = new MyTabbedPaneUI();
        tabbedPane.setUI(mtpui);

        addEmptyTab();
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

    /**
     * Adds a new cluster tab.
     */
    public final void addTab(final Cluster cluster) {
        Tools.debug(this, "cluster add tab " + cluster.getName());
        removeEmptyTab();
        final ClusterTab ct = new ClusterTab(cluster);
        cluster.setClusterTab(ct);
        /* insert tab before empty tab */
        tabbedPane.addTab(cluster.getName(),
                          CLUSTER_ICON,
                          ct,
                          Tools.join(" ", cluster.getHostNames()));
        tabbedPane.setSelectedComponent(ct);
        addEmptyTab();
        refresh();
    }

    /**
     * Adds an epmty tab, that opens new cluster dialogs.
     */
    public final void addEmptyTab() {
        tabbedPane.addTab("",
                          null,
                          NEW_CLUSTER_TAB,
                          Tools.getString("ClustersPanel.NewTabTip"));
    }

    /**
     * Removes the empty tab.
     */
    public final void removeEmptyTab() {
        tabbedPane.remove(tabbedPane.getTabCount() - 1);
    }

    /**
     * Removes selected tab, after clicking on the cancel button in the config
     * dialogs.
     */
    public final void removeTab() {
        final ClusterTab selected = getClusterTab();
        int index = tabbedPane.getSelectedIndex() - 1;
        if (index < 0) {
            index = 0;
        }
        if (selected != null) {
            /* deselecting so that dialogs don't appear */
            tabbedPane.setSelectedIndex(-1);
            tabbedPane.remove(selected);
            tabbedPane.setSelectedIndex(index);
        }
    }

    /**
     * Removes specified tab.
     */
    public final void removeTab(final Cluster cluster) {
        tabbedPane.remove(cluster.getClusterTab());
        cluster.setClusterTab(null);
    }

    /**
     * Removes all tabs.
     */
    public final void removeAllTabs() {
        tabbedPane.removeAll();
        addEmptyTab();
    }

    /**
     * renames selected added tab.
     */
    public final void renameSelectedTab(final String newName) {
        tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), newName);
        refresh();
    }

    /**
     * adds all cluster tabs, e.g. after loading of configuration.
     */
    private void addAllTabs() {
        final Clusters clusters = Tools.getConfigData().getClusters();
        if (clusters != null) {
            for (final Cluster cluster : clusters.getClusterSet()) {
                addTab(cluster);
            }
        }
        addEmptyTab();
    }

    /**
     * Refreshes the view.
     */
    public final void refresh() {
        tabbedPane.invalidate();
        tabbedPane.validate();
        tabbedPane.repaint();
    }

    /**
     * removes all tabs and adds them back, also a way to repaint them.
     */
    public final void repaintTabs() {
        tabbedPane.removeAll();
        addAllTabs();
    }

    /**
     * Return cluster tab, that is in the JScrollPane.
     */
    public final ClusterTab getClusterTab() {
        final Component sp = tabbedPane.getSelectedComponent();
        if (sp == null) {
            return null;
        } else  {
            return (ClusterTab) sp;
        }
    }

    /**
     * This class is used to override the tab look.
     */
    public class MyTabbedPaneUI
                            extends javax.swing.plaf.basic.BasicTabbedPaneUI {
        /**
         * Sets insets.
         */
        protected Insets getContentBorderInsets(final int tabPlacement) {
            return new Insets(0, 0, 0, 0);
        }

        /**
         * Overrides the content border painting with nothing.
         */
        protected void paintContentBorder(final Graphics g,
                                          final int tabPlacement,
                                          final int selectedIndex) {
            /* No border */
        }
    }
}
