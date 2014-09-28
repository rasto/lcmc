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
package lcmc.common.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;

/**
 * An implementation of a host view with tree of resources. This view is used
 * in the host tab as well in the cluster tab.
 */
@Named
public class ViewPanel extends JPanel {
    private static final Dimension MENU_TREE_MIN_SIZE = new Dimension(200, 200);
    private static final Dimension INFO_PANEL_MIN_SIZE = new Dimension(200, 200);
    /** Preferred size of the menu tree. */
    private static final Dimension MENU_TREE_SIZE = new Dimension(400, 200);
    /** Location of the divider in the split pane. */
    private static final int DIVIDER_LOCATION   = 200;
    /** This view split pane. */
    private JSplitPane viewSP = null;
    /** Disabled during load. It disables the menu expanding.*/
    private volatile boolean disabledDuringLoad = true;
    private final Lock mSetPanelLock = new ReentrantLock();
    /** Last selected info object in the right pane. */
    private Info lastSelectedInfo = null;
    @Inject
    private Application application;

    public ViewPanel() {
        super(new BorderLayout());
        setBackground(Tools.getDefaultColor("ViewPanel.Status.Background"));
    }

    /** Returns the menu tree. */
    public final JTree getTree(final Browser browser) {
        final JTree tree = new JTree(browser.getTreeModel());
        browser.setMenuTree(tree);
        tree.setOpaque(true);
        tree.setBackground(Tools.getDefaultColor("ViewPanel.Background"));
        tree.setToggleClickCount(2);
        tree.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                /* do nothing */
            }

            @Override
            public void mouseEntered(final MouseEvent e) {
                /* do nothing */
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                /* do nothing */
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                final int selRow = tree.getRowForLocation(e.getX(), e.getY());
                final TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                if (selRow != -1 && e.getButton() > 1) {
                    final Info nodeInfo =
                            (Info) ((DefaultMutableTreeNode) selPath.getLastPathComponent()).getUserObject();
                    if (nodeInfo != null) {
                        nodeInfo.showPopup(tree, e.getX(), e.getY());
                        tree.setSelectionPath(selPath);
                    }
                }
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                /* do nothing */
            }
        });
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(browser.getCellRenderer());

        final JScrollPane resourcesTreePane = new JScrollPane(tree);

        final JPanel resourceInfo = new JPanel();
        viewSP = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, resourcesTreePane, resourceInfo);

        resourcesTreePane.setMinimumSize(MENU_TREE_MIN_SIZE);
        resourceInfo.setMinimumSize(INFO_PANEL_MIN_SIZE);
        viewSP.setDividerLocation(DIVIDER_LOCATION);
        viewSP.setPreferredSize(MENU_TREE_SIZE);
        add(viewSP);

        // Listen for when the selection changes.
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(final TreeSelectionEvent e) {
                setRightComponentInView(tree, viewSP, browser);
            }
        });

        tree.getModel().addTreeModelListener(
            new TreeModelListener() {
                @Override
                public void treeNodesChanged(final TreeModelEvent e) {
                    if (!disabledDuringLoad) {
                        final Object[] selected = e.getChildren();
                        if (selected != null && selected.length > 0) {
                            final Object o = ((DefaultMutableTreeNode) selected[0]).getUserObject();
                            setRightComponentInView(browser, (Info) o);
                        }
                    }
                }

                @Override
                public void treeNodesInserted(final TreeModelEvent e) {
                    /* do nothing */
                }

                @Override
                public void treeNodesRemoved(final TreeModelEvent e) {
                    /* do nothing */
                }

                @Override
                public void treeStructureChanged(final TreeModelEvent e) {
                    final Object[] path = e.getPath();
                    if (!disabledDuringLoad) {
                        final TreePath tp = new TreePath(path);
                        application.invokeLater(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        tree.expandPath(tp);
                                                        tree.setSelectionPath(tp);
                                                    }
                                                });
                    }
                }
            }
        );
        return tree;
    }

    /** Returns whether expanding of paths is disabled during the initial load.
     */
    public final boolean isDisabledDuringLoad() {
        return disabledDuringLoad;
    }
    /** Sets if expanding of paths should be disabled during the initial load.*/
    public final void setDisabledDuringLoad(final boolean disabledDuringLoad) {
        this.disabledDuringLoad = disabledDuringLoad;
    }

    /** Sets the right component in the view. */
    private void setRightComponentInView(final JTree tree, final JSplitPane viewSP, final Browser browser) {
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }
        if (node.getParent() == null) {
            /* it's not shown. */
            return;
        }

        final Object nodeInfo = node.getUserObject();
        if (nodeInfo instanceof Info) {
            lastSelectedInfo = (Info) nodeInfo;
        }
        if (nodeInfo != null) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (!mSetPanelLock.tryLock()) {
                        return;
                    }
                    final JComponent p = browser.getInfoPanel(nodeInfo, disabledDuringLoad);
                    if (!disabledDuringLoad) {
                        final int loc = viewSP.getDividerLocation();
                        viewSP.setRightComponent(p);
                        viewSP.setDividerLocation(loc);
                    }
                    mSetPanelLock.unlock();
                }
            });
        }
    }

    /** Sets the right component in the view. */
    public final void setRightComponentInView(final Browser browser, final Info nodeInfo) {
        if (viewSP != null) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (!mSetPanelLock.tryLock()) {
                        return;
                    }
                    final JComponent p = browser.getInfoPanel(nodeInfo, disabledDuringLoad);
                    lastSelectedInfo = nodeInfo;
                    if (!disabledDuringLoad && p != null) {
                        final int loc = viewSP.getDividerLocation();
                        viewSP.setRightComponent(p);
                        viewSP.setDividerLocation(loc);
                    }
                    mSetPanelLock.unlock();
                }
            });
        }
    }

    public final void reloadRightComponent() {
        final Info lsi = lastSelectedInfo;
        if (lsi != null) {
            lsi.selectMyself();
        }
    }

    public final Info getLastSelectedInfo() {
        return lastSelectedInfo;
    }
}
