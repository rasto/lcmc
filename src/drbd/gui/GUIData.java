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

import drbd.data.Host;
import drbd.data.Cluster;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JComponent;
import java.awt.Component;
import java.util.List;
import java.util.ArrayList;


/**
 * GUIData
 *
 * Holds global GUI data, so that they can be retrieved easily throughout
 * the application and some functions that use this data.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class GUIData  {
    /** Main frame of the whole application. */
    private JFrame mainFrame;
    /** Split pane where is the terminal panel. */
    private JSplitPane terminalSplitPane;
    ///** Hosts panel. */
    //private HostsPanel hostsPanel;
    /** Clusters panel. */
    private ClustersPanel clustersPanel;
    /** Invisible panel with progress indicator. */
    private ProgressIndicatorPanel mainGlassPane;
    /** Main menu. */
    private MainMenu mainMenu;
    /** 'Add Cluster' button. */
    private MyButton addClusterButton = null;
    /** 'Add Cluster' menu item. */
    private JMenuItem addClusterMenuItem = null;
    /** Components that can be enabled and disabled in and out of the god mode.
     */
    private List<JComponent> visibleInGodModeList =
                                            new ArrayList<JComponent>();

    /**
     * Sets main frame of this application.
     */
    public final void setMainFrame(final JFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    /**
     * Gets main frame of this application.
     */
    public final JFrame getMainFrame() {
        return mainFrame;
    }

    /**
     * Sets main menu of this application.
     */
    public final void setMainMenu(final MainMenu mainMenu) {
        this.mainMenu = mainMenu;
    }

    /**
     * Gets main menu of this application.
     */
    public final MainMenu getMainMenu() {
        return mainMenu;
    }

    /**
     * Returns main glass pane (with progress indicator).
     */
    public final ProgressIndicatorPanel getMainGlassPane() {
        return mainGlassPane;
    }

    /**
     * Sets main glass pane.
     */
    public final void setMainGlassPane(
                                final ProgressIndicatorPanel mainGlassPane) {
        this.mainGlassPane = mainGlassPane;
    }

    /**
     * Sets split pane that contains terminal as bottom component.
     */
    public final void setTerminalSplitPane(
                                        final JSplitPane terminalSplitPane) {
        this.terminalSplitPane = terminalSplitPane;
    }

    /**
     * Sets terminal in bottom part of the split pane.
     */
    public final void setTerminalPanel(final Component terminalPanel) {
        final Component oldTerminalPanel =
                                terminalSplitPane.getBottomComponent();
        if (!terminalPanel.equals(oldTerminalPanel)) {
            terminalSplitPane.setOneTouchExpandable(false);
            final int dl = terminalSplitPane.getDividerLocation();
            /* this changes divider location */
            if (oldTerminalPanel != null) {
                terminalPanel.setSize(oldTerminalPanel.getSize());
            }
            terminalSplitPane.setBottomComponent(terminalPanel);
            terminalSplitPane.setDividerLocation(dl);
            terminalSplitPane.setOneTouchExpandable(true);
        }
    }

    /**
     * Returns the position of the terminal panel.
     */
    public final int getTerminalPanelPos() {
        if (terminalSplitPane.getBottomComponent() == null) {
            return 0;
        } else {
            return mainFrame.getContentPane().getY()
                   + terminalSplitPane.getBottomComponent().getY();
        }
    }

    ///**
    // * Sets hosts panel object, panel where are all the hosts.
    // */
    //public final void setHostsPanel(final HostsPanel hostsPanel) {
    //    this.hostsPanel = hostsPanel;
    //}

    ///**
    // * Gets hosts panel object.
    // */
    //public final HostsPanel getHostsPanel() {
    //    return hostsPanel;
    //}

    /**
     * Returns the panel with clusters.
     */
    public final ClustersPanel getClustersPanel() {
        return clustersPanel;
    }

    /**
     * Sets clusters panel object, panel where are all the clusters.
     */
    public final void setClustersPanel(final ClustersPanel clustersPanel) {
        this.clustersPanel = clustersPanel;
    }

    /**
     * Repaints hosts and clusters panels.
     */
    public final void repaintWithNewData() {
        //hostsPanel.repaintTabs();
        clustersPanel.repaintTabs();
    }

    ///**
    // * Adds tab with new host to the hosts panel.
    // */
    //public final void addHostTab(final Host host) {
    //    hostsPanel.addTab(host);
    //}

    ///**
    // * changes name of the selected host tab.
    // */
    //public final void renameSelectedHostTab(final String newName) {
    //    hostsPanel.renameSelectedTab(newName);
    //}

    ///**
    // * Removes selected tab. This is used, if host was added, but than
    // * it was canceled.
    // */
    //public final void removeSelectedHostTab() {
    //    hostsPanel.removeTab();
    //}

    /**
     * Adds tab with new cluster to the clusters panel.
     */
    public final void addClusterTab(final Cluster cluster) {
        clustersPanel.addTab(cluster);
    }

    /**
     * changes name of the selected cluster tab.
     */
    public final void renameSelectedClusterTab(final String newName) {
        clustersPanel.renameSelectedTab(newName);
    }

    /**
     * Removes selected tab. This is used, if cluster was added, but than
     * it was canceled.
     */
    public final void removeSelectedClusterTab() {
        clustersPanel.removeTab();
    }

    /**
     * Revalidates and repaints clusters panel.
     */
    public final void refreshClustersPanel() {
        clustersPanel.refresh();
    }

    /**
     * Sets the 'Add Cluster' button, so that it can be enabled or disabled.
     */
    public final void setAddClusterButton(final MyButton addClusterButton) {
        this.addClusterButton = addClusterButton;
    }

    /**
     * Sets the 'Add Cluster' menu item, so that it can be enabled or disabled.
     */
    public final void setAddClusterMenuItem(
                                        final JMenuItem addClusterMenuItem) {
        this.addClusterMenuItem = addClusterMenuItem;
    }

    /**
     * Checks 'Add Cluster' buttons and menu items and enables them, if there
     * are enough hosts to make cluster.
     */
    public final void checkAddClusterButtons() {
        final boolean enabled =
                            Tools.getConfigData().danglingHostsCount() >= 2;

        if (addClusterButton != null) {
            addClusterButton.setEnabled(enabled);
        }
        if (addClusterMenuItem != null) {
            addClusterMenuItem.setEnabled(enabled);
        }
    }

    /**
     * Add to the list of components that are visible only in god mode.
     */
    public final void addToVisibleInGodMode(JComponent c) {
        c.setVisible(false);
        visibleInGodModeList.add(c);
    }

    /**
     * Do gui actions when we are in the god mode.
     * - enable/disable look and feel menu
     */
    public final void godModeChanged(boolean godMode) {
        for (JComponent c : visibleInGodModeList) {
            c.setVisible(godMode);
        }
        Tools.startProgressIndicator("OH MY GOD!!!");
        Tools.stopProgressIndicator("OH MY GOD!!!");
    }
}
