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
package lcmc.common.ui.main;

import java.awt.Container;
import java.awt.Frame;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import lcmc.cluster.domain.Cluster;
import lcmc.cluster.ui.ClusterTab;
import lcmc.cluster.ui.ClustersPanel;
import lcmc.common.domain.AllHostsUpdatable;
import lcmc.common.ui.Info;
import lcmc.common.ui.ResourceGraph;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.ui.utils.MyList;
import lcmc.common.ui.utils.MyListModel;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.crm.ui.resource.ServicesInfo;
import lombok.Getter;
import lombok.Setter;
/**
 * Holds global GUI data, so that they can be retrieved easily throughout
 * the application and some functions that use this data.
 */
@Named
@Singleton
public class MainData {
    private ClustersPanel clustersPanel;
    /** Invisible panel with progress indicator. */
    private final ReadWriteLock mAddClusterButtonListLock = new ReentrantReadWriteLock();
    private final Lock mAddClusterButtonListReadLock = mAddClusterButtonListLock.readLock();
    private final Lock mAddClusterButtonListWriteLock = mAddClusterButtonListLock.writeLock();
    private final Collection<JComponent> addClusterButtonList = new ArrayList<>();
    private final ReadWriteLock mAddHostButtonListLock = new ReentrantReadWriteLock();
    private final Lock mAddHostButtonListReadLock = mAddHostButtonListLock.readLock();
    private final Lock mAddHostButtonListWriteLock = mAddHostButtonListLock.writeLock();
    private final Collection<JComponent> addHostButtonList = new ArrayList<>();
    /**
     * List of components that have allHostsUpdate method that must be called when a host is added.
     */
    private final Collection<AllHostsUpdatable> allHostsUpdateList = new ArrayList<>();
    /**
     * Selected components for copy/paste.
     */
    private List<Info> selectedComponents = null;

    private static volatile int prevScrollingMenuIndex = -1;

    private Container mainFrame;

    private final SwingUtils swingUtils;

    public static final String MIME_TYPE_TEXT_HTML = "text/html";
    public static final String MIME_TYPE_TEXT_PLAIN = "text/plain";
    public static final float DEFAULT_ANIM_FPS = 20.0f;

    @Getter
    @Setter
    private float animFPS;

    public MainData(SwingUtils swingUtils) {
        this.swingUtils = swingUtils;
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

    public ClustersPanel getClustersPanel() {
        return clustersPanel;
    }

    public void setClustersPanel(final ClustersPanel clustersPanel) {
        this.clustersPanel = clustersPanel;
    }


    /**
     * Adds a component to the list of components that have allHostsUpdate
     * method that must be called when a host is added.
     */
    public void registerAllHostsUpdate(final AllHostsUpdatable component) {
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

    /**
     * Return whether it is run as an applet.
     */
    public boolean isApplet() {
        return mainFrame instanceof JApplet;
    }

    public Container getMainFrame() {
        return mainFrame;
    }

    public void setMainFrame(final Container mainFrame) {
        this.mainFrame = mainFrame;
    }

    /**
     * Adds the 'Add Cluster' button to the list, so that it can be enabled or
     * disabled.
     */
    public void registerAddClusterButton(final JComponent addClusterButton) {
        mAddClusterButtonListWriteLock.lock();
        try {
            if (!addClusterButtonList.contains(addClusterButton)) {
                addClusterButtonList.add(addClusterButton);
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

    public Collection<JComponent> getAddClusterButtonList() {
        mAddClusterButtonListReadLock.lock();
        try {
            return new ArrayList<>(addClusterButtonList);
        } finally {
            mAddClusterButtonListReadLock.unlock();
        }
    }

    public Collection<JComponent> getAddHostButtonList() {
        mAddHostButtonListReadLock.lock();
        try {
            return new ArrayList<>(addHostButtonList);
        } finally {
            mAddHostButtonListReadLock.unlock();
        }
    }

    public Collection<AllHostsUpdatable> getAllHostsUpdateList() {
        return allHostsUpdateList;
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
        final Thread t = new Thread(() -> ssi.pasteServices(scs));
        t.start();
    }

    /** Returns a popup in a scrolling pane. */
    public boolean getScrollingMenu(final String name,
                                    final JPanel optionsPanel,
                                    final MyMenu menu,
                                    final MyListModel<MyMenuItem> dlm,
                                    final MyList<MyMenuItem> list,
                                    final Info infoObject,
                                    final Collection<JDialog> popups,
                                    final Map<MyMenuItem, ButtonCallback> callbackHash) {
        final int maxSize = dlm.getSize();
        if (maxSize <= 0) {
            return false;
        }
        prevScrollingMenuIndex = -1;
        list.setFixedCellHeight(25);
        list.setVisibleRowCount(Math.min(maxSize, 10));
        final JScrollPane sp = new JScrollPane(list);
        sp.setViewportBorder(null);
        sp.setBorder(null);
        final JTextField typeToSearchField = dlm.getFilterField();
        final JDialog popup;
        if (mainFrame instanceof JApplet) {
            popup = new JDialog(new JFrame(), name, false);
        } else {
            popup = new JDialog((Frame) mainFrame, name, false);
        }
        popup.setUndecorated(true);
        popup.setAlwaysOnTop(true);
        final JPanel popupPanel = new JPanel();
        popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.PAGE_AXIS));
        if (maxSize > 10) {
            popupPanel.add(typeToSearchField);
        }
        popupPanel.add(sp);
        if (optionsPanel != null) {
            popupPanel.add(optionsPanel);
        }
        popup.setContentPane(popupPanel);
        popups.add(popup);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(final MouseEvent e) {
                prevScrollingMenuIndex = -1;
                if (callbackHash != null) {
                    for (final MyMenuItem item : callbackHash.keySet()) {
                        callbackHash.get(item).mouseOut(item);
                        list.clearSelection();
                    }
                }
            }
            @Override
            public void mouseEntered(final MouseEvent e) {
                /* request focus here causes the applet making all
                textfields to be not editable. */
                list.requestFocus();
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                prevScrollingMenuIndex = -1;
                if (callbackHash != null) {
                    for (final MyMenuItem item : callbackHash.keySet()) {
                        callbackHash.get(item).mouseOut(item);
                    }
                }
                final int index = list.locationToIndex(e.getPoint());
                swingUtils.invokeLater(() -> {
                    list.setSelectedIndex(index);
                    //TODO: some submenus stay visible, during
                    //ptest, but this breaks group popup menu
                    //setMenuVisible(menu, false);
                    menu.setSelected(false);
                });
                final MyMenuItem item = dlm.getElementAt(index);
                item.actionThread();
            }
        });

        list.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                final Thread thread = new Thread(() -> {
                    int pIndex = list.locationToIndex(e.getPoint());
                    final Rectangle r = list.getCellBounds(pIndex, pIndex);
                    if (r == null) {
                        return;
                    }
                    if (!r.contains(e.getPoint())) {
                        pIndex = -1;
                    }
                    final int index = pIndex;
                    final int lastIndex = prevScrollingMenuIndex;
                    if (index == lastIndex) {
                        return;
                    }
                    prevScrollingMenuIndex = index;
                    swingUtils.invokeLater(() -> list.setSelectedIndex(index));
                    if (callbackHash != null) {
                        if (lastIndex >= 0) {
                            final MyMenuItem lastItem = dlm.getElementAt(lastIndex);
                            final ButtonCallback bc = callbackHash.get(lastItem);
                            if (bc != null) {
                                bc.mouseOut(lastItem);
                            }
                        }
                        if (index >= 0) {
                            final MyMenuItem item = dlm.getElementAt(index);
                            final ButtonCallback bc = callbackHash.get(item);
                            if (bc != null) {
                                bc.mouseOver(item);
                            }
                        }
                    }
                });
                thread.start();
            }
        });
        list.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(final KeyEvent e) {
                final int ch = e.getKeyCode();
                if (ch == KeyEvent.VK_UP && list.getSelectedIndex() == 0) {
                    swingUtils.invokeLater(typeToSearchField::requestFocus);
                } else if (ch == KeyEvent.VK_ESCAPE) {
                    swingUtils.invokeLater(() -> {
                        for (final JDialog otherP : popups) {
                            otherP.dispose();
                        }
                    });
                    infoObject.hidePopup();
                } else if (ch == KeyEvent.VK_SPACE || ch == KeyEvent.VK_ENTER) {
                    final MyMenuItem item = list.getSelectedValue();
                    if (item != null) {
                        item.actionThread();
                    }
                }
            }
            @Override
            public void keyReleased(final KeyEvent e) {
            }
            @Override
            public void keyTyped(final KeyEvent e) {
            }
        });
        popup.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(final WindowEvent e) {
            }
            @Override
            public void windowLostFocus(final WindowEvent e) {
                popup.dispose();
            }
        });

        typeToSearchField.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(final KeyEvent e) {
                final int ch = e.getKeyCode();
                final Thread thread = new Thread(() -> {
                    if (ch == KeyEvent.VK_DOWN) {
                        swingUtils.invokeLater(() -> {
                            list.requestFocus();
                            /* don't need to press down arrow twice */
                            list.setSelectedIndex(0);
                        });
                    } else if (ch == KeyEvent.VK_ESCAPE) {
                        swingUtils.invokeLater(() -> {
                            for (final JDialog otherP : popups) {
                                otherP.dispose();
                            }
                        });
                        infoObject.hidePopup();
                    } else if (ch == KeyEvent.VK_SPACE || ch == KeyEvent.VK_ENTER) {
                        final MyMenuItem item = list.getModel().getElementAt(0);
                        if (item != null) {
                            item.actionThread();
                        }
                    }
                });
                thread.start();
            }
            @Override
            public void keyReleased(final KeyEvent e) {
            }
            @Override
            public void keyTyped(final KeyEvent e) {
            }
        });

        /* menu is not new. */
        for (final MenuListener ml : menu.getMenuListeners()) {
            menu.removeMenuListener(ml);
        }
        menu.addMenuListener(new MenuListener() {
            @Override
            public void menuCanceled(final MenuEvent e) {
            }

            @Override
            public void menuDeselected(final MenuEvent e) {
                final Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(mouseLocation, sp);
                final boolean inside = sp.getBounds().contains(mouseLocation);

                for (final JDialog otherP : popups) {
                    if (popup != otherP || !inside) {
                        /* don't dispose the popup if it was clicked.  */
                        otherP.dispose();
                    }
                }
            }

            @Override
            public void menuSelected(final MenuEvent e) {
                final Point l = menu.getLocationOnScreen();
                for (final JDialog otherP : popups) {
                    otherP.dispose();
                }
                popup.setLocation((int) (l.getX() + menu.getBounds().getWidth()), (int) l.getY() - 1);
                popup.pack();
                popup.setVisible(true);
                typeToSearchField.requestFocus();
                typeToSearchField.selectAll();
                /* Setting location again. Moving it one pixel fixes
                the "gray window" problem. */
                popup.setLocation((int) (l.getX() + menu.getBounds().getWidth()), (int) l.getY());
            }
        });
        return true;
    }

    public boolean isSlow() {
        return animFPS < DEFAULT_ANIM_FPS;
    }

    public boolean isFast() {
        return animFPS > DEFAULT_ANIM_FPS;
    }
}
