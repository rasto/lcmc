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

import drbd.data.Clusters;
import drbd.data.Cluster;
import drbd.utilities.Tools;

import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JEditorPane;
import javax.swing.ImageIcon;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.border.LineBorder;

import java.util.List;
import java.util.ArrayList;

import java.awt.GridLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

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
    /** New empty cluster tab. */
    private static ClusterTab newClusterTab;
    /** Previously selected tab. */
    private ClusterTab prevSelected = null;
    /** Width of the tab border. */
    private static final int TAB_BORDER_WIDTH = 3;
    /** Upgrade check text. */
    private String upgradeCheck;
    /** Upgrade check lock. */
    private final Mutex mUpgradeLock = new Mutex();
    /** Upgrade check text fields. */
    private final List<JEditorPane> upgradeTextFields =
                                               new ArrayList<JEditorPane>();

    /** Prepares a new <code>ClustersPanel</code> object. */
    ClustersPanel() {
        super(new GridLayout(1, 1));
        Tools.getGUIData().setClustersPanel(this);
        if (Tools.getConfigData().isUpgradeCheckEnabled()) {
            upgradeCheck = Tools.getString("MainPanel.UpgradeCheck");
        } else {
            upgradeCheck = Tools.getString("MainPanel.UpgradeCheckDisabled");
        }
        newClusterTab = new ClusterTab(null);
        if (Tools.getConfigData().isUpgradeCheckEnabled()) {
            startUpgradeCheck();
        }
        if (Tools.getConfigData().arePluginsEnabled()) {
            loadPlugins();
        }
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

    /** Adds a new cluster tab. */
    void addTab(final Cluster cluster) {
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

    /** Adds an epmty tab, that opens new cluster dialogs. */
    void addEmptyTab() {
        tabbedPane.addTab("",
                          null,
                          newClusterTab,
                          Tools.getString("ClustersPanel.NewTabTip"));
    }

    /** Removes the empty tab. */
    void removeEmptyTab() {
        tabbedPane.remove(tabbedPane.getTabCount() - 1);
    }

    /**
     * Removes selected tab, after clicking on the cancel button in the config
     * dialogs.
     */
    void removeTab() {
        final ClusterTab selected = getClusterTab();
        selected.getCluster().setClusterTab(null);
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

    /** Removes specified tab. */
    public void removeTab(final Cluster cluster) {
        tabbedPane.remove(cluster.getClusterTab());
        cluster.setClusterTab(null);
    }

    /** Removes all tabs. */
    public void removeAllTabs() {
        tabbedPane.removeAll();
        addEmptyTab();
    }

    /** Renames selected added tab. */
    void renameSelectedTab(final String newName) {
        tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), newName);
        refresh();
    }

    /** Adds all cluster tabs, e.g. after loading of configuration. */
    private void addAllTabs() {
        final Clusters clusters = Tools.getConfigData().getClusters();
        if (clusters != null) {
            for (final Cluster cluster : clusters.getClusterSet()) {
                addTab(cluster);
            }
        }
        addEmptyTab();
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
    class MyTabbedPaneUI extends javax.swing.plaf.basic.BasicTabbedPaneUI {
        /** Sets insets. */
        @Override protected final Insets getContentBorderInsets(
                                                    final int tabPlacement) {
            return new Insets(0, 0, 0, 0);
        }

        /** Overrides the content border painting with nothing. */
        @Override protected void paintContentBorder(final Graphics g,
                                                    final int tabPlacement,
                                                    final int selectedIndex) {
            /* No border */
        }
    }

    /** Starts upgrade check. */
    private void startUpgradeCheck() {
        final Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                final String latestVersion = Tools.getLatestVersion();
                try {
                    mUpgradeLock.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (latestVersion == null) {
                    upgradeCheck =
                             Tools.getString("MainPanel.UpgradeCheckFailed");
                } else {
                    final String release = Tools.getRelease();
                    if (Tools.compareVersions(release, latestVersion) < 0) {
                        upgradeCheck =
                            Tools.getString("MainPanel.UpgradeAvailable")
                                     .replaceAll("@LATEST@", latestVersion);
                    } else {
                        upgradeCheck =
                                Tools.getString("MainPanel.NoUpgradeAvailable");
                    }
                }
                final String text = upgradeCheck;
                mUpgradeLock.release();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        for (final JEditorPane field : upgradeTextFields) {
                            field.setText(text);
                            field.setVisible(!"".equals(text));
                        }
                    }
                });
            }
        });
        thread.start();
    }

    /** Load plugins. */
    private void loadPlugins() {
        final Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                Tools.loadPlugins();
            }
        });
        thread.start();
    }

    /**
     * Register upgrade text field, that will be updated, when upgrade check is
     * done.
     */
    JEditorPane registerUpgradeTextField() {
        final JEditorPane upgradeField =
                                new JEditorPane(Tools.MIME_TYPE_TEXT_HTML, "");
        final LineBorder border = new LineBorder(Color.RED);
        upgradeField.setBorder(border);
        Tools.setEditorFont(upgradeField);
        upgradeField.setEditable(false);
        upgradeField.addHyperlinkListener(new HyperlinkListener() {
            @Override public void hyperlinkUpdate(final HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    Tools.openBrowser(e.getURL().toString());
                }
            }
        });
        upgradeField.setBackground(Color.WHITE);
        try {
            mUpgradeLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        upgradeTextFields.add(upgradeField);
        final String text = upgradeCheck;
        mUpgradeLock.release();
        upgradeField.setText(text);
        return upgradeField;
    }

    /** Unregister upgrade text field. */
    void unregisterUpgradeTextField(final JEditorPane field) {
        try {
            mUpgradeLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        upgradeTextFields.remove(field);
        mUpgradeLock.release();
    }
}
