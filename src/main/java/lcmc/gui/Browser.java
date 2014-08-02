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
import java.awt.Component;
import java.awt.Dimension;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import lcmc.model.Application;
import lcmc.gui.resources.CategoryInfo;
import lcmc.gui.resources.Info;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

/**
 * This class holds host and cluster resource data in a tree. It shows
 * panels that allow to edit the data of resources, services etc., hosts and
 * clusters.
 * Every resource has its Info object, that accessible through the tree view.
 */
public class Browser {
    private static final Logger LOG = LoggerFactory.getLogger(Browser.class);
    public static final ImageIcon CATEGORY_ICON = Tools.createImageIcon(Tools.getDefault("Browser.CategoryIcon"));
    public static final ImageIcon APPLY_ICON = Tools.createImageIcon(Tools.getDefault("Browser.ApplyIcon"));
    public static final ImageIcon REVERT_ICON = Tools.createImageIcon(Tools.getDefault("Browser.RevertIcon"));
    public static final ImageIcon ACTIONS_MENU_ICON = Tools.createImageIcon(Tools.getDefault("Browser.MenuIcon"));
    public static final Color PANEL_BACKGROUND = Tools.getDefaultColor("ViewPanel.Background");
    public static final Color BUTTON_PANEL_BACKGROUND = Tools.getDefaultColor("ViewPanel.ButtonPanel.Background");
    public static final Color STATUS_BACKGROUND = Tools.getDefaultColor("ViewPanel.Status.Background");
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode treeTop;
    private JTree tree;

    private JSplitPane infoPanelSplitPane;
    private final Lock mDRBDtestLock = new ReentrantLock();

    protected final void setMenuTreeTop() {
        final CategoryInfo categoryInfo = new CategoryInfo();
        categoryInfo.init(Tools.getString("Browser.Resources"), this);
        treeTop = new DefaultMutableTreeNode(categoryInfo);
        treeModel = new DefaultTreeModel(treeTop);
    }

    protected final void setMenuTreeTop(final Info info) {
        treeTop = new DefaultMutableTreeNode(info);
        treeModel = new DefaultTreeModel(treeTop);
    }

    protected final void setMenuTree(final JTree tree) {
        this.tree = tree;
    }

    public final JTree getMenuTree() {
        return tree;
    }

    public final void repaintMenuTree() {
        final JTree t = tree;
        if (t != null) {
            t.repaint();
        }
    }

    final DefaultMutableTreeNode getTreeTop() {
        return treeTop;
    }

    public final void reloadAndWait(final TreeNode node, final boolean select) {
        final JTree t = tree;
        DefaultMutableTreeNode oldN = null;
        if (t != null) {
            oldN = (DefaultMutableTreeNode) t.getLastSelectedPathComponent();
        }
        final DefaultMutableTreeNode oldNode = oldN;
        if (node != null) {
            treeModel.reload(node);
        }
        if (!select && t != null && oldNode != null) {
            /* if don't want to select, we reselect the old path. */
            //t.setSelectionPath(path);
            treeModel.reload(oldNode);
        }
    }

    public final void reloadNode(final TreeNode node, final boolean select) {
        final JTree t = tree;
        DefaultMutableTreeNode oldN = null;
        if (t != null) {
            oldN = (DefaultMutableTreeNode) t.getLastSelectedPathComponent();
        }
        final DefaultMutableTreeNode oldNode = oldN;
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                if (node != null) {
                    treeModel.reload(node);
                }
            }
        });
        if (!select && t != null && oldNode != null) {
            Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                @Override
                public void run() {
                    /* if don't want to select, we reselect the old path. */
                    //t.setSelectionPath(path);
                    treeModel.reload(oldNode);
                }
            });
        }
    }
    public final void nodeChangedAndWait(final TreeNode node) {
        treeModel.nodeChanged(node);
    }

    public final void nodeChanged(final DefaultMutableTreeNode node) {
        final String stacktrace = Tools.getStackTrace();
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                try {
                    treeModel.nodeChanged(node);
                } catch (final RuntimeException e) {
                    LOG.appError("nodeChangedAndWait: " + node.getUserObject()
                                 + " node changed error:\n"
                                 + stacktrace + "\n\n", e);
                }
            }
        });
    }

    protected final void topLevelAdd(final MutableTreeNode node) {
        Tools.isSwingThread();
        treeTop.add(node);
    }

    protected final void repaintSplitPane() {
        if (infoPanelSplitPane != null) {
            infoPanelSplitPane.repaint();
        }
    }

    /** Sets node variable in the info object that this tree node points to. */
    public final void setNode(final DefaultMutableTreeNode node) {
        ((Info) node.getUserObject()).setNode(node);
    }

    final DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * Returns panel with info of some resource from Info object. The info is
     * specified in getInfoPanel method in the Info object. If a resource has a
     * graphical view, it returns a split pane with this view and the info
     * underneath.
     */
    final JComponent getInfoPanel(final Object nodeInfo, final boolean disabledDuringLoad) {
        if (nodeInfo == null) {
            return null;
        }
        final JPanel gView = ((Info) nodeInfo).getGraphicalView();
        final JComponent iPanel = ((Info) nodeInfo).getInfoPanel();
        if (gView == null) {
            return iPanel;
        } else {
            final int maxWidth = ClusterBrowser.SERVICE_LABEL_WIDTH + ClusterBrowser.SERVICE_FIELD_WIDTH + 36;
            iPanel.setMinimumSize(new Dimension(maxWidth, 0));
            iPanel.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));
            if (infoPanelSplitPane != null) {
                if (!disabledDuringLoad) {
                    final int loc = infoPanelSplitPane.getDividerLocation();
                    infoPanelSplitPane.setLeftComponent(gView);
                    infoPanelSplitPane.setRightComponent(iPanel);
                    infoPanelSplitPane.setDividerLocation(loc);
                }
                return infoPanelSplitPane;
            }
            final JSplitPane newSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gView, iPanel);
            newSplitPane.setResizeWeight(1.0);
            newSplitPane.setOneTouchExpandable(true);
            infoPanelSplitPane = newSplitPane;
            infoPanelSplitPane.repaint();
            return infoPanelSplitPane;
        }
    }

    final TreeCellRenderer getCellRenderer() {
        return new CellRenderer();
    }

    public final void drbdtestLockAcquire() {
        mDRBDtestLock.lock();
    }

    public final void drbdtestLockRelease() {
        mDRBDtestLock.unlock();
    }

    protected void selectPath(final Object[] path) {
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                final TreePath tp = new TreePath(path);
                getMenuTree().expandPath(tp);
                getMenuTree().setSelectionPath(tp);
            }
        });
    }

    public final void addNode(final DefaultMutableTreeNode node, final MutableTreeNode child) {
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
              node.add(child);
            }
        });
    }

    /** Renders the cells for the menu. */
    private static class CellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 1L;

        CellRenderer() {
            super();
            setBackgroundNonSelectionColor(PANEL_BACKGROUND);
            setBackgroundSelectionColor(Tools.getDefaultColor("ViewPanel.Status.Background"));
            setTextNonSelectionColor(Tools.getDefaultColor("ViewPanel.Foreground"));
            setTextSelectionColor(Tools.getDefaultColor("ViewPanel.Status.Foreground"));
        }

        /**
         * Returns the CellRenderer component, setting up the icons and
         * tooltips.
         */
        @Override
        public Component getTreeCellRendererComponent(final JTree tree,
                                                      final Object value,
                                                      final boolean sel,
                                                      final boolean expanded,
                                                      final boolean leaf,
                                                      final int row,
                                                      final boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            final Info info = (Info) ((DefaultMutableTreeNode) value).getUserObject();
            if (info == null) {
                return this;
            }
            if (leaf) {
                final ImageIcon icon = info.getMenuIcon(Application.RunMode.LIVE);
                if (icon != null) {
                    setIcon(icon);
                }
                setToolTipText("");
            } else {
                setToolTipText("");
                ImageIcon icon = info.getCategoryIcon(Application.RunMode.LIVE);
                if (icon == null) {
                    icon = CATEGORY_ICON;
                }
                setIcon(icon);
            }
            return this;
        }
    }
}
