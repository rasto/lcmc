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
import drbd.data.AccessMode;
import drbd.utilities.Tools;
import drbd.utilities.AllHostsUpdatable;

import javax.swing.JFrame;
import javax.swing.JApplet;
import javax.swing.JRootPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import java.awt.Component;
import java.awt.Container;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import EDU.oswego.cs.dl.util.concurrent.Mutex;

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
    private Container mainFrame;
    /** Main panel of the whole application. */
    private JPanel mainPanel;
    /** Split pane where is the terminal panel. */
    private JSplitPane terminalSplitPane;
    /** Clusters panel. */
    private ClustersPanel clustersPanel;
    /** Invisible panel with progress indicator. */
    private ProgressIndicatorPanel mainGlassPane;
    /** Main menu. */
    private MainMenu mainMenu;
    /** Browser that appears if there are no clusters. */
    private EmptyBrowser emptyBrowser;
    /** 'Add Cluster" buttons list lock. */
    private final Mutex mAddClusterButtonListLock = new Mutex();
    /** 'Add Cluster' buttons. */
    private final List<JComponent> addClusterButtonList =
                                                   new ArrayList<JComponent>();
    /** 'Add Host" buttons list lock. */
    private final Mutex mAddHostButtonListLock = new Mutex();
    /** 'Add Host' buttons. */
    private final List<JComponent> addHostButtonList =
                                                   new ArrayList<JComponent>();
    /** Components that can be made visible in the god mode. */
    private final Map<JComponent, AccessMode> visibleInAccessType =
                                      new HashMap<JComponent, AccessMode>();
    /** Global elements like menus, that are enabled, disabled according to
     * their access type. */
    private final Map<JComponent, AccessMode> enabledInAccessType =
                                        new HashMap<JComponent, AccessMode>();
    /**
     * List of components that have allHostsUpdate method that must be called
     * when a host is added.
     */
    private final List<AllHostsUpdatable> allHostsUpdateList =
                                            new ArrayList<AllHostsUpdatable>();

    /** Sets main frame of this application. */
    public final void setMainFrame(final Container mainFrame) {
        this.mainFrame = mainFrame;
    }

    /** Sets main panel of this application. */
    public final void setMainPanel(final JPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    /** Gets main frame of this application. */
    public final Container getMainFrame() {
        return mainFrame;
    }

    /** Gets root pane of the main frame of this application. */
    public final JRootPane getMainFrameRootPane() {
        if (mainFrame instanceof JFrame) {
            return ((JFrame) mainFrame).getRootPane();
        } else if (mainFrame instanceof JApplet) {
            return ((JApplet) mainFrame).getRootPane();
        }
        return null;
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
     * Sets empty browser of this application.
     */
    public final void setEmptyBrowser(final EmptyBrowser emptyBrowser) {
        this.emptyBrowser = emptyBrowser;
    }

    /**
     * Gets empty browser of this application.
     */
    public final EmptyBrowser getEmptyBrowser() {
        return emptyBrowser;
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
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    final int loc = terminalSplitPane.getDividerLocation();
                    terminalSplitPane.setBottomComponent(terminalPanel);
                    if (loc > Tools.getDefaultInt("DrbdMC.height") - 100) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                expandTerminalSplitPane(1);
                            }
                        });
                    }
                }
            });
        }
    }

    /**
     * Returns the position of the terminal panel.
     */
    public final int getTerminalPanelPos() {
        if (terminalSplitPane.getBottomComponent() == null) {
            return 0;
        } else {
            return mainPanel.getY()
                   + terminalSplitPane.getBottomComponent().getY();
        }
    }

    /** Expands the terminal split pane. */
    public final void expandTerminalSplitPane(final int buttonNo) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final int height = (int)
                    terminalSplitPane.getBottomComponent().getSize()
                                                          .getHeight();
                if ((buttonNo == 0 && height == 0)
                    || (buttonNo == 1 && height > 0)) {
                    Tools.debug(this, "expand terminal split pane", 1);
                    final BasicSplitPaneUI ui =
                                   (BasicSplitPaneUI) terminalSplitPane.getUI();
                    final BasicSplitPaneDivider divider = ui.getDivider();
                    final JButton button = (JButton) divider.getComponent(
                                                                      buttonNo);
                    button.doClick();
                }
            }
        });
    }

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
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                clustersPanel.repaintTabs();
            }
        });
    }

    /**
     * Adds tab with new cluster to the clusters panel.
     */
    public final void addClusterTab(final Cluster cluster) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                clustersPanel.addTab(cluster);
            }
        });
    }

    /**
     * changes name of the selected cluster tab.
     */
    public final void renameSelectedClusterTab(final String newName) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                clustersPanel.renameSelectedTab(newName);
            }
        });
    }

    /**
     * Removes selected tab. This is used, if cluster was added, but than
     * it was canceled.
     */
    public final void removeSelectedClusterTab() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                clustersPanel.removeTab();
            }
        });
    }

    /**
     * Revalidates and repaints clusters panel.
     */
    public final void refreshClustersPanel() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                clustersPanel.refresh();
            }
        });
    }

    /**
     * Adds the 'Add Cluster' button to the list, so that it can be enabled or
     * disabled.
     */
    public final void registerAddClusterButton(
                                           final JComponent addClusterButton) {
        try {
            mAddClusterButtonListLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        if (!addClusterButtonList.contains(addClusterButton)) {
            addClusterButtonList.add(addClusterButton);
            addClusterButton.setEnabled(
                             Tools.getConfigData().danglingHostsCount() >= 1);
        }
        mAddClusterButtonListLock.release();
    }

    /**
     * Adds the 'Add Host' button to the list, so that it can be enabled or
     * disabled.
     */
    public final void registerAddHostButton(
                                           final JComponent addHostButton) {
        try {
            mAddHostButtonListLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        if (!addHostButtonList.contains(addHostButton)) {
            addHostButtonList.add(addHostButton);
        }
        mAddHostButtonListLock.release();
    }

    /**
     * Removes the 'Add Cluster' button from the list.
     */
    public final void unregisterAddClusterButton(
                                           final JComponent addClusterButton) {
        try {
            mAddClusterButtonListLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        addClusterButtonList.remove(addClusterButton);
        mAddClusterButtonListLock.release();
    }

    /**
     * Checks 'Add Cluster' buttons and menu items and enables them, if there
     * are enough hosts to make cluster.
     */
    public final void checkAddClusterButtons() {
        final boolean enabled =
                            Tools.getConfigData().danglingHostsCount() >= 1;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    mAddClusterButtonListLock.acquire();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                for (final JComponent addClusterButton : addClusterButtonList) {
                    addClusterButton.setEnabled(enabled);
                }
                mAddClusterButtonListLock.release();
            }
        });
    }

    /**
     * Enable/Disable all 'Add Cluster' buttons.
     */
    public final void enableAddClusterButtons(final boolean enable) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    mAddClusterButtonListLock.acquire();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                for (JComponent addClusterButton : addClusterButtonList) {
                    addClusterButton.setEnabled(enable);
                }
                mAddClusterButtonListLock.release();
            }
        });
    }

    /**
     * Enable/Disable all 'Add Host' buttons.
     */
    public final void enableAddHostButtons(final boolean enable) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    mAddHostButtonListLock.acquire();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                for (JComponent addHostButton : addHostButtonList) {
                    addHostButton.setEnabled(enable);
                }
                mAddHostButtonListLock.release();
            }
        });
    }


    /**
     * Add to the list of components that are visible only in specific access
     * mode.
     */
    public final void addToVisibleInAccessType(final JComponent c,
                                               final AccessMode accessMode) {
        c.setVisible(Tools.getConfigData().isAccessible(accessMode));
        visibleInAccessType.put(c, accessMode);
    }

    /**
     * Add to the list of components that are visible only in specific access
     * mode.
     */
    public final void addToEnabledInAccessType(final JComponent c,
                                               final AccessMode accessMode) {
        c.setEnabled(Tools.getConfigData().isAccessible(accessMode));
        enabledInAccessType.put(c, accessMode);
    }

    /**
     * Do gui actions when we are in the god mode.
     * - enable/disable look and feel menu etc
     */
    public final void godModeChanged(final boolean godMode) {
        Tools.startProgressIndicator("OH MY GOD!!! Hi Rasto!");
        Tools.stopProgressIndicator("OH MY GOD!!! Hi Rasto!");
        for (final Cluster cluster
                        : Tools.getConfigData().getClusters().getClusterSet()) {
            final ClusterBrowser cb = cluster.getBrowser();
            if (cb != null) {
                cb.getClusterViewPanel().resetOperatingModes(godMode);
            }
        }
        updateGlobalItems();
    }

    /** Sets operating mode in every cluster view. */
    public final void setOperatingModeGlobally(final Cluster fromCluster,
                                               final String opMode) {
        for (final Cluster cluster
                        : Tools.getConfigData().getClusters().getClusterSet()) {
            if (cluster == fromCluster) {
                continue;
            }
            final ClusterBrowser cb = cluster.getBrowser();
            if (cb != null) {
                cb.getClusterViewPanel().setOperatingMode(opMode);
            }
        }
    }

    /** Sets advanced mode in every cluster view. */
    public final void setAdvancedModeGlobally(final Cluster fromCluster,
                                              final boolean advancedMode) {
        for (final Cluster cluster
                        : Tools.getConfigData().getClusters().getClusterSet()) {
            if (cluster == fromCluster) {
                continue;
            }
            final ClusterBrowser cb = cluster.getBrowser();
            if (cb != null) {
                cb.getClusterViewPanel().setAdvancedMode(advancedMode);
            }
        }
    }

    /** Updates access of the item according of their access type. */
    public final void updateGlobalItems() {
        for (final JComponent c : visibleInAccessType.keySet()) {
            c.setVisible(Tools.getConfigData().isAccessible(
                                                visibleInAccessType.get(c)));
        }
        for (final JComponent c : enabledInAccessType.keySet()) {
            c.setEnabled(Tools.getConfigData().isAccessible(
                                                enabledInAccessType.get(c)));
        }
    }

    /**
     * Adds a component to the list of components that have allHostsUpdate
     * method that must be called when a host is added.
     */
    public final void registerAllHostsUpdate(
                                            final AllHostsUpdatable component) {
        if (!allHostsUpdateList.contains(component)) {
            allHostsUpdateList.add(component);
        }
    }

    /**
     * Adds a component from the list of components that have allHostsUpdate
     * method that must be called when a host is added.
     */
    public final void unregisterAllHostsUpdate(
                                            final AllHostsUpdatable component) {
        allHostsUpdateList.remove(component);
    }

    /** Calls allHostsUpdate method on all registered components. */
    public final void allHostsUpdate() {
        for (final AllHostsUpdatable component : allHostsUpdateList) {
            component.allHostsUpdate();
        }
        checkAddClusterButtons();
    }

    /** Enabled the component if it is accessible. */
    public final void setAccessible(final JComponent c,
                                    final ConfigData.AccessType required) {
        c.setEnabled(Tools.getConfigData().getAccessType().compareTo(
                                                                required) >= 0);
    }

}
