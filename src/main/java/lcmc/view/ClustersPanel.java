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

package lcmc.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import lcmc.gui.GUIData;
import lcmc.model.Application;
import lcmc.model.Cluster;
import lcmc.model.UserConfig;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An implementation of a panel that holds cluster tabs. Clicking on the tab,
 * changes also host that is shown in the terminal panel, to the host, that
 * is active in the cluster.
 */
@Component
public final class ClustersPanel extends JPanel {
    private static final Logger LOG = LoggerFactory.getLogger(ClustersPanel.class);
    private static final ImageIcon ALL_CLUSTERS_ICON = Tools.createImageIcon(
                                                            Tools.getDefault("ClustersPanel.ClustersIcon"));
    private static final String ALL_CLUSTERS_LABEL = Tools.getString("ClustersPanel.ClustersTab");
    private static final int TAB_BORDER_WIDTH = 3;
    private JTabbedPane tabbedPane;
    @Autowired
    private ClusterTabFactory clusterTabFactory;
    private ClusterTab previouslySelectedTab = null;
    @Autowired
    private UserConfig userConfig;
    @Autowired
    private GUIData guiData;
    @Autowired
    private Application application;

    /** Shows the tabbed pane. */
    public void init() {
        guiData.setClustersPanel(this);
        tabbedPane = new JTabbedPane();

        setTabLook();

        loadDefaultConfigFile();

        addClustersTab(ALL_CLUSTERS_LABEL);

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent changeEvent) {
                final JTabbedPane prevSource = (JTabbedPane) changeEvent.getSource();
                final ClusterTab source = previouslySelectedTab;
                previouslySelectedTab = (ClusterTab) prevSource.getSelectedComponent();

                /* show dialogs only if got here from other tab. */
                if ((source == null || source.getName() == null)) {
                    return;
                }

                final ClusterTab clusterTab = getClusterTab();
                if (clusterTab == null) {
                    return;
                }

                final Cluster cluster = clusterTab.getCluster();
                if (cluster != null) {
                    refreshView();
                }
            }
        });
        add(tabbedPane);
    }

    public void addClusterTab(final ClusterTab clusterTab) {
        final Cluster cluster = clusterTab.getCluster();
        LOG.debug2("addTab: cluster: " + cluster.getName());
        if (tabbedPane.getTabCount() == 1) {
           removeAllTabs();
        }
        final String title = Tools.join(" ", cluster.getHostNames());
        tabbedPane.addTab(cluster.getName(), ClusterTab.CLUSTER_ICON, clusterTab, title);

        addTabComponentWithCloseButton(clusterTab);
        tabbedPane.setSelectedComponent(clusterTab);
        refreshView();
    }

    /** Adds an epmty tab, that opens new cluster dialogs. */
    void addClustersTab(final String label) {
        tabbedPane.addTab(label,
                          ALL_CLUSTERS_ICON,
                          clusterTabFactory.createClusterTab(null),
                          Tools.getString("ClustersPanel.ClustersTabTip"));
    }

    public void removeTab() {
        removeSelectedTab(getClusterTab());
    }

    public void removeTabWithCluster(final Cluster cluster) {
        removeSelectedTab(cluster.getClusterTab());
    }

    public void removeAllTabs() {
        tabbedPane.removeAll();
        addClustersTab("");
    }

    public void renameSelectedTab(final String newName) {
        final JLabel label = getClusterTab().getLabelTitle();
        if (label != null) {
            label.setText(newName);
        }
        refreshView();
    }

    public void refreshView() {
        tabbedPane.invalidate();
        tabbedPane.validate();
        tabbedPane.repaint();
    }

    public ClusterTab getClusterTab() {
        final java.awt.Component sp = tabbedPane.getSelectedComponent();
        if (sp == null) {
            return null;
        } else  {
            return (ClusterTab) sp;
        }
    }

    private void setTabLook() {
        setLayout(new GridLayout(1, 1));
        setBackground(Tools.getDefaultColor("ClustersPanel.Background"));

        UIManager.put("TabbedPane.selected", Tools.getDefaultColor("ViewPanel.Status.Background"));
        UIManager.put("TabbedPane.foreground", Color.WHITE);
        UIManager.put("TabbedPane.background", Tools.getDefaultColor("ViewPanel.Background"));
        setBorder(BorderFactory.createLineBorder(Tools.getDefaultColor("ClustersPanel.Background"), TAB_BORDER_WIDTH));

        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        final BorderlessTabbedPaneUI borderlessTabbedPaneUI = new BorderlessTabbedPaneUI();
        tabbedPane.setUI(borderlessTabbedPaneUI);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    private void addTabComponentWithCloseButton(final ClusterTab clusterTab) {
        final JPanel tabPanel = clusterTab.createTabComponentWithCloseButton();

        final int index = tabbedPane.indexOfComponent(clusterTab);
        tabbedPane.setTabComponentAt(index, tabPanel);
    }

    /**
     * Removes selected tab, after clicking on the cancel button in the config
     * dialogs.
     */
    private void removeSelectedTab(final ClusterTab selectedTab) {
        if (selectedTab != null) {
            selectedTab.getCluster().setClusterTab(null);
            tabbedPane.remove(selectedTab);
        }
        if (tabbedPane.getTabCount() == 1) {
            tabbedPane.removeAll();
            addClustersTab(ALL_CLUSTERS_LABEL);
        }
    }

    /** This class is used to override the tab look. */
    private static class BorderlessTabbedPaneUI extends BasicTabbedPaneUI {
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

    private void loadDefaultConfigFile() {
        final String saveFile = application.getDefaultSaveFile();
        String xml = Tools.loadFile(guiData, saveFile, false);
        if (xml == null) {
            final String saveFileOld = application.getSaveFileOld();
            xml = Tools.loadFile(guiData, saveFileOld, false);
        }
        if (xml != null) {
            userConfig.loadXML(xml);
        }
    }

}
