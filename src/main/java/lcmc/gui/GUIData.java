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
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSplitPane;
import javax.swing.RootPaneContainer;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.Cluster;
import lcmc.gui.resources.Info;
import lcmc.gui.resources.crm.ServicesInfo;
import lcmc.utilities.AllHostsUpdatable;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;
import lcmc.view.ClusterTab;
import lcmc.view.ClustersPanel;

/**
 * Holds global GUI data, so that they can be retrieved easily throughout
 * the application and some functions that use this data.
 */
public final class GUIData  {
    private static final Logger LOG = LoggerFactory.getLogger(GUIData.class);
    private Container mainFrame;
    private JPanel mainPanel;
    private JSplitPane terminalSplitPane;
    private ClustersPanel clustersPanel;
    /** Invisible panel with progress indicator. */
    private ProgressIndicatorPanel mainGlassPane;
    private MainMenu mainMenu;
    /** Browser that appears if there are no clusters. */
    private EmptyBrowser emptyBrowser;
    private final ReadWriteLock mAddClusterButtonListLock = new ReentrantReadWriteLock();
    private final Lock mAddClusterButtonListReadLock = mAddClusterButtonListLock.readLock();
    private final Lock mAddClusterButtonListWriteLock = mAddClusterButtonListLock.writeLock();
    private final Collection<JComponent> addClusterButtonList = new ArrayList<JComponent>();
    private final ReadWriteLock mAddHostButtonListLock = new ReentrantReadWriteLock();
    private final Lock mAddHostButtonListReadLock = mAddHostButtonListLock.readLock();
    private final Lock mAddHostButtonListWriteLock = mAddHostButtonListLock.writeLock();
    private final Collection<JComponent> addHostButtonList = new ArrayList<JComponent>();
    private final Map<JComponent, AccessMode> visibleInAccessType = new HashMap<JComponent, AccessMode>();
    /** Global elements like menus, that are enabled, disabled according to
     * their access type. */
    private final Map<JComponent, AccessMode> enabledInAccessType = new HashMap<JComponent, AccessMode>();
    /**
     * List of components that have allHostsUpdate method that must be called
     * when a host is added.
     */
    private final Collection<AllHostsUpdatable> allHostsUpdateList = new ArrayList<AllHostsUpdatable>();
    /** Selected components for copy/paste. */
    private List<Info> selectedComponents = null;

    public void setMainFrame(final Container mainFrame) {
        this.mainFrame = mainFrame;
    }

    public void setMainPanel(final JPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    public Container getMainFrame() {
        return mainFrame;
    }

    public Container getMainFrameContentPane() {
        return ((RootPaneContainer) mainFrame).getContentPane();
    }

    public JRootPane getMainFrameRootPane() {
        if (mainFrame instanceof JFrame) {
            return ((RootPaneContainer) mainFrame).getRootPane();
        } else if (mainFrame instanceof JApplet) {
            return ((RootPaneContainer) mainFrame).getRootPane();
        }
        return null;
    }

    public void setMainMenu(final MainMenu mainMenu) {
        this.mainMenu = mainMenu;
    }

    public MainMenu getMainMenu() {
        return mainMenu;
    }

    void setEmptyBrowser(final EmptyBrowser emptyBrowser) {
        this.emptyBrowser = emptyBrowser;
    }

    public EmptyBrowser getEmptyBrowser() {
        return emptyBrowser;
    }

    /** Returns main glass pane (with progress indicator). */
    public ProgressIndicatorPanel getMainGlassPane() {
        return mainGlassPane;
    }

    public void setMainGlassPane(final ProgressIndicatorPanel mainGlassPane) {
        this.mainGlassPane = mainGlassPane;
    }

    /** Sets split pane that contains terminal as bottom component. */
    public void setTerminalSplitPane(final JSplitPane terminalSplitPane) {
        this.terminalSplitPane = terminalSplitPane;
    }

    public void setTerminalPanel(final Component terminalPanel) {
        if (terminalPanel == null) {
            return;
        }
        final Component oldTerminalPanel = terminalSplitPane.getBottomComponent();
        if (!terminalPanel.equals(oldTerminalPanel)) {
            Tools.invokeAndWaitIfNeeded(new Runnable() {
                @Override
                public void run() {
                    final int loc = terminalSplitPane.getDividerLocation();
                    terminalSplitPane.setBottomComponent(terminalPanel);
                    if (loc > Tools.getDefaultInt("DrbdMC.height") - 100) {
                        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
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
            return mainPanel.getY() + terminalSplitPane.getBottomComponent().getY();
        }
    }

    public boolean isTerminalPanelExpanded() {
        return terminalSplitPane.getBottomComponent().getSize().getHeight() != 0;
    }

    public void expandTerminalSplitPane(final int buttonNo) {
        if (terminalSplitPane == null) {
            return;
        }
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                final int height = (int)
                    terminalSplitPane.getBottomComponent().getSize().getHeight();
                if ((buttonNo == 0 && height == 0) || (buttonNo == 1 && height > 0)) {
                    LOG.debug2("expandTerminalSplitPane:");
                    final BasicSplitPaneUI ui = (BasicSplitPaneUI) terminalSplitPane.getUI();
                    final BasicSplitPaneDivider divider = ui.getDivider();
                    final JButton button = (JButton) divider.getComponent(buttonNo);
                    button.doClick();
                }
            }
        });
    }

    public ClustersPanel getClustersPanel() {
        return clustersPanel;
    }

    public void setClustersPanel(final ClustersPanel clustersPanel) {
        this.clustersPanel = clustersPanel;
    }

    public void addClusterTab(final Cluster cluster) {
        final ClusterTab clusterTab = new ClusterTab();
        clusterTab.initWithCluster(cluster);
        clustersPanel.addClusterTab(clusterTab);
    }

    public void renameSelectedClusterTab(final String newName) {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                clustersPanel.renameSelectedTab(newName);
            }
        });
    }

    /**
     * This is used, if cluster was added, but than it was canceled.
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
                clustersPanel.refreshView();
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
                addClusterButton.setEnabled(Tools.getApplication().danglingHostsCount() >= 1);
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
        final boolean enabled = Tools.getApplication().danglingHostsCount() >= 1;
        mAddClusterButtonListReadLock.lock();
        try {
            for (final JComponent addClusterButton : addClusterButtonList) {
                addClusterButton.setEnabled(enabled);
            }
        } finally {
            mAddClusterButtonListReadLock.unlock();
        }
    }

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
    void addToVisibleInAccessType(final JComponent c, final AccessMode accessMode) {
        c.setVisible(Tools.getApplication().isAccessible(accessMode));
        visibleInAccessType.put(c, accessMode);
    }

    /**
     * Add to the list of components that are visible only in specific access
     * mode.
     */
    void addToEnabledInAccessType(final JComponent c, final AccessMode accessMode) {
        c.setEnabled(Tools.getApplication().isAccessible(accessMode));
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
        for (final Map.Entry<JComponent, AccessMode> accessEntry : visibleInAccessType.entrySet()) {
            accessEntry.getKey().setVisible(Tools.getApplication().isAccessible(accessEntry.getValue()));
        }
        for (final Map.Entry<JComponent, AccessMode> enabledEntry : enabledInAccessType.entrySet()) {
            enabledEntry.getKey().setEnabled(Tools.getApplication().isAccessible(enabledEntry.getValue()));
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

    public void setAccessible(final JComponent c, final Application.AccessType required) {
        c.setEnabled(Tools.getApplication().getAccessType().compareTo(required) >= 0);
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
        return c.getBrowser().getCrmGraph();
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
