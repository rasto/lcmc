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

package lcmc.cluster.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import lcmc.cluster.domain.Cluster;
import lcmc.common.domain.Application;
import lcmc.common.domain.UserConfig;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.MainPresenter;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * An implementation of a panel that holds cluster tabs. Clicking on the tab,
 * changes also host that is shown in the terminal panel, to the host, that
 * is active in the cluster.
 */
@Named
@Singleton
public final class ClustersPanel extends JPanel {
    private static final Logger LOG = LoggerFactory.getLogger(ClustersPanel.class);
    private static final ImageIcon ALL_CLUSTERS_ICON = Tools.createImageIcon(Tools.getDefault("ClustersPanel.ClustersIcon"));
    private static final String ALL_CLUSTERS_LABEL = Tools.getString("ClustersPanel.ClustersTab");
    private static final int TAB_BORDER_WIDTH = 3;
    private JTabbedPane tabbedPane;
    private final Provider<ClusterTabFactory> clusterTabFactory;
    private ClusterTab previouslySelectedTab = null;
    private final UserConfig userConfig;
    private final MainData mainData;
    private final MainPresenter mainPresenter;
    private final Application application;

    public ClustersPanel(Provider<ClusterTabFactory> clusterTabFactory, UserConfig userConfig, MainData mainData,
            MainPresenter mainPresenter, Application application) {
        this.clusterTabFactory = clusterTabFactory;
        this.userConfig = userConfig;
        this.mainData = mainData;
        this.mainPresenter = mainPresenter;
        this.application = application;
    }

    /**
     * Shows the tabbed pane.
     */
    public void init() {
        mainData.setClustersPanel(this);
        tabbedPane = new JTabbedPane();

        setTabLook();

        loadDefaultConfigFile();

        addClustersTab(ALL_CLUSTERS_LABEL);

        tabbedPane.addChangeListener(changeEvent -> {
            final JTabbedPane prevSource = (JTabbedPane) changeEvent.getSource();
            final ClusterTab source = previouslySelectedTab;
            previouslySelectedTab = (ClusterTab) prevSource.getSelectedComponent();

            /* show dialogs only if got here from other tab. */
            if (source == null || source.getName() == null) {
                return;
            }

            getClusterTab().flatMap(ClusterTab::getCluster)
                           .ifPresent(it -> refreshView());
        });
        add(tabbedPane);
    }

    public void addClusterTab(final ClusterTab clusterTab) {
        clusterTab.getCluster()
                  .ifPresent(cluster -> {
                      LOG.debug2("addTab: cluster: " + cluster.getName());
                      if (tabbedPane.getTabCount() == 1) {
                          removeAllTabs();
                      }
                      final String title = Tools.join(" ", cluster.getHostNames());
                      tabbedPane.addTab(cluster.getName(), ClusterTab.CLUSTER_ICON, clusterTab, title);

                      addTabComponentWithCloseButton(clusterTab);
                      tabbedPane.setSelectedComponent(clusterTab);
                      refreshView();
                  });
    }

    /** Adds an epmty tab, that opens new cluster dialogs. */
    void addClustersTab(final String label) {
        tabbedPane.addTab(label, ALL_CLUSTERS_ICON, clusterTabFactory.get()
                                                                     .createClusterTab(null),
                Tools.getString("ClustersPanel.ClustersTabTip"));
    }

    public void removeTab() {
        getClusterTab().ifPresent(this::removeTab);
    }

    public void removeTabWithCluster(final Cluster cluster) {
        cluster.getClusterTab()
               .ifPresent(this::removeTab);
    }

    public void removeAllTabs() {
        tabbedPane.removeAll();
        addClustersTab("");
    }

    public void renameSelectedTab(final String newName) {
        getClusterTab().map(ClusterTab::getLabelTitle)
                       .ifPresent(label -> label.setText(newName));
        refreshView();
    }

    public void refreshView() {
        tabbedPane.invalidate();
        tabbedPane.validate();
        tabbedPane.repaint();
    }

    public Optional<ClusterTab> getClusterTab() {
        return Optional.ofNullable(tabbedPane.getSelectedComponent())
                       .map(sp -> (ClusterTab) sp);
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

    private void removeTab(ClusterTab selectedTab) {
        selectedTab.getCluster()
                   .ifPresent(it -> it.setClusterTab(null));
        tabbedPane.remove(selectedTab);
        handleOneTab();
    }

    private void handleOneTab() {
        if (tabbedPane.getTabCount() == 1) {
            tabbedPane.removeAll();
            addClustersTab(ALL_CLUSTERS_LABEL);
        }
    }

    /**
     * This class is used to override the tab look.
     */
    private static class BorderlessTabbedPaneUI extends BasicTabbedPaneUI {
        @Override
        protected final Insets getContentBorderInsets(final int tabPlacement) {
            return new Insets(0, 0, 0, 0);
        }

        /**
         * Overrides the content border painting with nothing.
         */
        @Override
        protected void paintContentBorder(final Graphics g, final int tabPlacement, final int selectedIndex) {
            /* No border */
        }
    }

    private void loadDefaultConfigFile() {
        final String saveFile = application.getDefaultSaveFile();
        String xml = Tools.loadFile(mainPresenter, saveFile, false);
        if (xml == null) {
            final String saveFileOld = application.getSaveFileOld();
            xml = Tools.loadFile(mainPresenter, saveFileOld, false);
        }
        if (xml != null) {
            userConfig.loadXML(xml);
        }
    }

}
