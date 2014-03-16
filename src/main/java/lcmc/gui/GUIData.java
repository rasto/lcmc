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

import lcmc.gui.resources.Info;
import lcmc.gui.resources.ServicesInfo;
import lcmc.data.Cluster;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.utilities.Tools;
import lcmc.utilities.AllHostsUpdatable;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSplitPane;
import javax.swing.RootPaneContainer;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import java.awt.Component;
import java.awt.Container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * GUIData
 *
 * Holds global GUI data, so that they can be retrieved easily throughout
 * the application and some functions that use this data.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class GUIData  {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(GUIData.class);
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
    private final ReadWriteLock mAddClusterButtonListLock =
                                                new ReentrantReadWriteLock();
    /** 'Add Cluster" buttons list read lock. */
    private final Lock mAddClusterButtonListReadLock =
                                        mAddClusterButtonListLock.readLock();
    /** 'Add Cluster" buttons list write lock. */
    private final Lock mAddClusterButtonListWriteLock =
                                        mAddClusterButtonListLock.writeLock();
    /** 'Add Cluster' buttons. */
    private final Collection<JComponent> addClusterButtonList =
                                                   new ArrayList<JComponent>();
    /** 'Add Host" buttons list lock. */
    private final ReadWriteLock mAddHostButtonListLock =
                                                new ReentrantReadWriteLock();
    /** 'Add Host" buttons list read lock. */
    private final Lock mAddHostButtonListReadLock =
                                        mAddHostButtonListLock.readLock();
    /** 'Add Host" buttons list write lock. */
    private final Lock mAddHostButtonListWriteLock =
                                        mAddHostButtonListLock.writeLock();
    /** 'Add Host' buttons. */
    private final Collection<JComponent> addHostButtonList =
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
    private final Collection<AllHostsUpdatable> allHostsUpdateList =
                                            new ArrayList<AllHostsUpdatable>();
    /** Selected components for copy/paste. */
    private List<Info> selectedComponents = null;

    /** Sets main frame of this application. */
    public void setMainFrame(final Container mainFrame) {
        this.mainFrame = mainFrame;
    }

    /** Sets main panel of this application. */
    public void setMainPanel(final JPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    /** Gets main frame of this application. */
    public Container getMainFrame() {
        return mainFrame;
    }

    /** Returns content pane of the main frame. */
    public Container getMainFrameContentPane() {
        return ((RootPaneContainer) mainFrame).getContentPane();
    }

    /** Gets root pane of the main frame of this application. */
    public JRootPane getMainFrameRootPane() {
        if (mainFrame instanceof JFrame) {
            return ((RootPaneContainer) mainFrame).getRootPane();
        } else if (mainFrame instanceof JApplet) {
            return ((RootPaneContainer) mainFrame).getRootPane();
        }
        return null;
    }

    /** Sets main menu of this application. */
    public void setMainMenu(final MainMenu mainMenu) {
        this.mainMenu = mainMenu;
    }

    /** Gets main menu of this application. */
    public MainMenu getMainMenu() {
        return mainMenu;
    }

    /** Sets empty browser of this application. */
    void setEmptyBrowser(final EmptyBrowser emptyBrowser) {
        this.emptyBrowser = emptyBrowser;
    }

    /** Gets empty browser of this application. */
    public EmptyBrowser getEmptyBrowser() {
        return emptyBrowser;
    }

    /** Returns main glass pane (with progress indicator). */
    public ProgressIndicatorPanel getMainGlassPane() {
        return mainGlassPane;
    }

    /** Sets main glass pane. */
    public void setMainGlassPane(final ProgressIndicatorPanel mainGlassPane) {
        this.mainGlassPane = mainGlassPane;
    }

    /** Sets split pane that contains terminal as bottom component. */
    void setTerminalSplitPane(final JSplitPane terminalSplitPane) {
        this.terminalSplitPane = terminalSplitPane;
    }

    /** Sets terminal in bottom part of the split pane. */
    public void setTerminalPanel(final Component terminalPanel) {
        if (terminalPanel == null) {
            return;
        }
        final Component oldTerminalPanel =
                                terminalSplitPane.getBottomComponent();
        if (!terminalPanel.equals(oldTerminalPanel)) {
            Tools.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    final int loc = terminalSplitPane.getDividerLocation();
                    terminalSplitPane.setBottomComponent(terminalPanel);
                    if (loc > Tools.getDefaultInt("DrbdMC.height") - 100) {
                        Tools.invokeLater(!Tools.CHECK_SWING_THREAD,
                                          new Runnable() {
                            @Override
                            public void run() {
                                expandTerminalSplitPane(1);
                            }
                        });
                    }
                }
            });
        }
    }

    /** Returns the position of the terminal panel. */
    int getTerminalPanelPos() {
        if (terminalSplitPane.getBottomComponent() == null) {
            return 0;
        } else {
            return mainPanel.getY()
                   + terminalSplitPane.getBottomComponent().getY();
        }
    }

    /** Returns whether the terminal panel is expanded. */
    public boolean isTerminalPanelExpanded() {
        return terminalSplitPane.getBottomComponent().getSize().getHeight()
               != 0;
    }

    /** Expands the terminal split pane. */
    public void expandTerminalSplitPane(final int buttonNo) {
        if (terminalSplitPane == null) {
            return;
        }
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                final int height = (int)
                    terminalSplitPane.getBottomComponent().getSize()
                                                          .getHeight();
                if ((buttonNo == 0 && height == 0)
                    || (buttonNo == 1 && height > 0)) {
                    LOG.debug2("expandTerminalSplitPane:");
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

    /** Returns the panel with clusters. */
    public ClustersPanel getClustersPanel() {
        return clustersPanel;
    }

    /** Sets clusters panel object, panel where are all the clusters. */
    void setClustersPanel(final ClustersPanel clustersPanel) {
        this.clustersPanel = clustersPanel;
    }

    /** Adds tab with new cluster to the clusters panel. */
    public void addClusterTab(final Cluster cluster) {
        clustersPanel.addTab(cluster);
    }

    /** changes name of the selected cluster tab. */
    public void renameSelectedClusterTab(final String newName) {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                clustersPanel.renameSelectedTab(newName);
            }
        });
    }

    /**
     * Removes selected tab. This is used, if cluster was added, but than
     * it was canceled.
     */
    public void removeSelectedClusterTab() {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                clustersPanel.removeTab();
            }
        });
    }

    /** Revalidates and repaints clusters panel. */
    public void refreshClustersPanel() {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                clustersPanel.refresh();
            }
        });
    }

    /**
     * Adds the 'Add Cluster' button to the list, so that it can be enabled or
     * disabled.
     */
    void registerAddClusterButton(final JComponent addClusterButton) {
        mAddClusterButtonListWriteLock.lock();
        try {
            if (!addClusterButtonList.contains(addClusterButton)) {
                addClusterButtonList.add(addClusterButton);
                addClusterButton.setEnabled(
                              Tools.getConfigData().danglingHostsCount() >= 1);
            }
        } finally {
            mAddClusterButtonListWriteLock.unlock();
        }
    }

    /**
     * Adds the 'Add Host' button to the list, so that it can be enabled or
     * disabled.
     */
    public void registerAddHostButton(final JComponent addHostButton) {
        mAddHostButtonListWriteLock.lock();
        try {
            if (!addHostButtonList.contains(addHostButton)) {
                addHostButtonList.add(addHostButton);
            }
        } finally {
            mAddHostButtonListWriteLock.unlock();
        }
    }

    /**
     * Checks 'Add Cluster' buttons and menu items and enables them, if there
     * are enough hosts to make cluster.
     */
    public void checkAddClusterButtons() {
        Tools.isSwingThread();
        final boolean enabled = Tools.getConfigData().danglingHostsCount() >= 1;
        mAddClusterButtonListReadLock.lock();
        try {
            for (final JComponent addClusterButton
                                            : addClusterButtonList) {
                addClusterButton.setEnabled(enabled);
            }
        } finally {
            mAddClusterButtonListReadLock.unlock();
        }
    }

    /** Enable/Disable all 'Add Cluster' buttons. */
    public void enableAddClusterButtons(final boolean enable) {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                mAddClusterButtonListReadLock.lock();
                try {
                    for (final JComponent addClusterButton : addClusterButtonList) {
                        addClusterButton.setEnabled(enable);
                    }
                } finally {
                    mAddClusterButtonListReadLock.unlock();
                }
            }
        });
    }

    /** Enable/Disable all 'Add Host' buttons. */
    public void enableAddHostButtons(final boolean enable) {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                mAddHostButtonListReadLock.lock();
                try {
                    for (final JComponent addHostButton : addHostButtonList) {
                        addHostButton.setEnabled(enable);
                    }
                } finally {
                    mAddHostButtonListReadLock.unlock();
                }
            }
        });
    }


    /**
     * Add to the list of components that are visible only in specific access
     * mode.
     */
    void addToVisibleInAccessType(final JComponent c,
                                  final AccessMode accessMode) {
        c.setVisible(Tools.getConfigData().isAccessible(accessMode));
        visibleInAccessType.put(c, accessMode);
    }

    /**
     * Add to the list of components that are visible only in specific access
     * mode.
     */
    void addToEnabledInAccessType(final JComponent c,
                                  final AccessMode accessMode) {
        c.setEnabled(Tools.getConfigData().isAccessible(accessMode));
        enabledInAccessType.put(c, accessMode);
    }

    /**
     * Do gui actions when we are in the god mode.
     * - enable/disable look and feel menu etc
     */
    void godModeChanged(final boolean godMode) {
        Tools.startProgressIndicator("OH MY GOD!!! Hi Rasto!");
        Tools.stopProgressIndicator("OH MY GOD!!! Hi Rasto!");
        getMainMenu().resetOperatingModes(godMode);
        updateGlobalItems();
    }

    /** Updates access of the item according of their access type. */
    void updateGlobalItems() {
        for (final Map.Entry<JComponent, AccessMode> accessEntry
                                                      : visibleInAccessType.entrySet()) {
            accessEntry.getKey().setVisible(Tools.getConfigData().isAccessible(
                    accessEntry.getValue()));
        }
        for (final Map.Entry<JComponent, AccessMode> enabledEntry
                                                      : enabledInAccessType.entrySet()) {
            enabledEntry.getKey().setEnabled(Tools.getConfigData().isAccessible(
                    enabledEntry.getValue()));
        }
    }

    /**
     * Adds a component to the list of components that have allHostsUpdate
     * method that must be called when a host is added.
     */
    void registerAllHostsUpdate(final AllHostsUpdatable component) {
        if (!allHostsUpdateList.contains(component)) {
            allHostsUpdateList.add(component);
        }
    }

    /**
     * Adds a component from the list of components that have allHostsUpdate
     * method that must be called when a host is added.
     */
    public void unregisterAllHostsUpdate(final AllHostsUpdatable component) {
        allHostsUpdateList.remove(component);
    }

    /** Calls allHostsUpdate method on all registered components. */
    public void allHostsUpdate() {
        for (final AllHostsUpdatable component : allHostsUpdateList) {
            component.allHostsUpdate();
        }
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                checkAddClusterButtons();
            }
        });
    }

    /** Enabled the component if it is accessible. */
    public void setAccessible(final JComponent c,
                                        final ConfigData.AccessType required) {
        c.setEnabled(Tools.getConfigData().getAccessType().compareTo(
                                                                required) >= 0);
    }

    private ServicesInfo getSelectedServicesInfo() {
        final ClustersPanel csp = clustersPanel;
        if (csp == null) {
            return null;
        }
        final ClusterTab selected = csp.getClusterTab();
        if (selected == null) {
            return null;
        }
        //TODO: or drbd
        final Cluster c = selected.getCluster();
        if (c == null) {
            return null;
        }
        return c.getBrowser().getServicesInfo();
    }

    private ResourceGraph getSelectedGraph() {
        final ClustersPanel csp = clustersPanel;
        if (csp == null) {
            return null;
        }
        final ClusterTab selected = csp.getClusterTab();
        if (selected == null) {
            return null;
        }
        //TODO: or drbd
        final Cluster c = selected.getCluster();
        if (c == null) {
            return null;
        }
        return c.getBrowser().getCRMGraph();
    }

    /** Copy / paste function. */
    public void copy() {
        final ResourceGraph g = getSelectedGraph();
        if (g == null) {
            return;
        }
        selectedComponents = g.getSelectedComponents();
    }

    /** Copy / paste function. */
    public void paste() {
        final List<Info> scs = selectedComponents;
        if (scs == null) {
            return;
        }
        final ServicesInfo ssi = getSelectedServicesInfo();
        if (ssi == null) {
            return;
        }
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                ssi.pasteServices(scs);
            }
        });
        t.start();
    }

    /**
     * Return whether it is run as an applet.
     */
    public boolean isApplet() {
        return mainFrame instanceof JApplet;
    }

}
