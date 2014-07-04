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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import lcmc.data.Cluster;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;

import org.springframework.stereotype.Component;

/**
 * An implementation of a panel that holds cluster tabs. Clicking on the tab,
 * changes also host that is shown in the terminal panel, to the host, that
 * is active in the cluster.
 */
@Component
public final class ClustersPanel extends JPanel {
    private static final Logger LOG = LoggerFactory.getLogger(ClustersPanel.class);
    /** Icon of the cluster. */
    private static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(Tools.getDefault("ClustersPanel.ClusterIcon"));
    /** Icon of all clusters. */
    private static final ImageIcon CLUSTERS_ICON = Tools.createImageIcon(
                                                       Tools.getDefault("ClustersPanel.ClustersIcon"));
    /** Name of all clusters tab. */
    private static final String CLUSTERS_LABEL = Tools.getString("ClustersPanel.ClustersTab");
    private static final int TAB_BORDER_WIDTH = 3;
    private JTabbedPane tabbedPane;
    /** New empty cluster tab. */
    private ClusterTab newClusterTab;
    /** Previously selected tab. */
    private ClusterTab prevSelected = null;

    private final Map<ClusterTab, JLabel> clusterTabLabels = new HashMap<ClusterTab, JLabel>();

    /** Shows the tabbed pane. */
    public void init() {
        setLayout(new GridLayout(1, 1));
        Tools.getGUIData().setClustersPanel(this);
        newClusterTab = new ClusterTab(null);
        setBackground(Tools.getDefaultColor("ClustersPanel.Background"));

        UIManager.put("TabbedPane.selected", Tools.getDefaultColor("ViewPanel.Status.Background"));
        UIManager.put("TabbedPane.foreground", Color.WHITE);
        UIManager.put("TabbedPane.background", Tools.getDefaultColor("ViewPanel.Background"));

        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);

        final MyTabbedPaneUI mtpui = new MyTabbedPaneUI();
        tabbedPane.setUI(mtpui);

        addClustersTab(CLUSTERS_LABEL);
        add(tabbedPane);
        setBorder(BorderFactory.createLineBorder(Tools.getDefaultColor("ClustersPanel.Background"), TAB_BORDER_WIDTH));
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        /* Register a change listener.
           This causes terminal panel to show correct host, after clicking on
           the cluster tab. TODO: is this comment right? */
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                final ClusterTab source = prevSelected;
                final JTabbedPane prevSource = (JTabbedPane) e.getSource();
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
                    if (cluster != null) {
                        refresh();
                    }
                }
            }
        });
    }

    /** Adds a new cluster tab. */
    void addTab(final Cluster cluster) {
        LOG.debug2("addTab: cluster: " + cluster.getName());
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
                                         if (cluster.isTabClosable()) {
                                             Tools.stopCluster(cluster);
                                             Tools.getGUIData().getEmptyBrowser().setDisconnected(cluster);
                                         }
                                     }
                                 });
                                 t.start();
                             }
                         };

        addTabComponent(tabbedPane, cluster.getName(), CLUSTER_ICON, ct, disconnectAction);
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
        clusterTabLabels.put(ct, lblTitle);

        final MyButton clusterButton = new MyButton("X");
        clusterButton.setBackgroundColor(Browser.STATUS_BACKGROUND);
        clusterButton.setMargin(new Insets(0, 0, 0, 0));
        clusterButton.setIconTextGap(0);

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        tabPanel.add(iconLabel, gbc);

        gbc.gridx++;
        gbc.weightx = 1.0;
        tabPanel.add(lblTitle, gbc);

        gbc.gridx++;
        gbc.weightx = 0.0;
        tabPanel.add(clusterButton, gbc);

        tabPane.setTabComponentAt(index, tabPanel);
        clusterButton.addActionListener(actionListener);
    }

    /** Adds an epmty tab, that opens new cluster dialogs. */
    void addClustersTab(final String label) {
        tabbedPane.addTab(label, CLUSTERS_ICON, newClusterTab, Tools.getString("ClustersPanel.ClustersTabTip"));
    }

    /**
     * Removes selected tab, after clicking on the cancel button in the config
     * dialogs.
     */
    private void removeTab(final ClusterTab selected) {
        if (selected != null) {
            selected.getCluster().setClusterTab(null);
            clusterTabLabels.remove(selected);
            tabbedPane.remove(selected);
        }
        if (tabbedPane.getTabCount() == 1) {
            clusterTabLabels.clear();
            tabbedPane.removeAll();
            addClustersTab(CLUSTERS_LABEL);
        }
    }

    void removeTab() {
        removeTab(getClusterTab());
    }

    /** Removes specified tab. */
    public void removeTab(final Cluster cluster) {
        removeTab(cluster.getClusterTab());
    }

    public void removeAllTabs() {
        clusterTabLabels.clear();
        tabbedPane.removeAll();
        addClustersTab("");
    }

    void renameSelectedTab(final String newName) {
        final JLabel label = clusterTabLabels.get(getClusterTab());
        if (label != null) {
            label.setText(newName);
        }
        refresh();
    }

    /** Refreshes the view. */
    void refresh() {
        tabbedPane.invalidate();
        tabbedPane.validate();
        tabbedPane.repaint();
    }

    /** Return cluster tab, that is in the JScrollPane. */
    ClusterTab getClusterTab() {
        final java.awt.Component sp = tabbedPane.getSelectedComponent();
        if (sp == null) {
            return null;
        } else  {
            return (ClusterTab) sp;
        }
    }

    /** This class is used to override the tab look. */
    static class MyTabbedPaneUI extends BasicTabbedPaneUI {
        @Override
        protected final Insets getContentBorderInsets(final int tabPlacement) {
            return new Insets(0, 0, 0, 0);
        }

        /** Overrides the content border painting with nothing. */
        @Override
        protected void paintContentBorder(final Graphics g, final int tabPlacement, final int selectedIndex) {
            /* No border */
        }
    }
}
