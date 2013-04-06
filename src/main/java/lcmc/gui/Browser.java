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

import lcmc.utilities.Tools;
import lcmc.gui.resources.Info;
import lcmc.gui.resources.CategoryInfo;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

import javax.swing.SwingUtilities;
import javax.swing.JSplitPane;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class holds host and cluster resource data in a tree. It shows
 * panels that allow to edit the data of resources, services etc., hosts and
 * clusters.
 * Every resource has its Info object, that accessible through the tree view.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class Browser {
    /** Tree model of the menu tree. */
    private DefaultTreeModel treeModel;
    /** Top of the menu tree. */
    private DefaultMutableTreeNode treeTop;
    /** Tree. */
    private JTree tree;

    /** Split pane next to the menu. */
    private JSplitPane infoPanelSplitPane;
    /** Icon fot the categories. */
    public static final ImageIcon CATEGORY_ICON =
            Tools.createImageIcon(Tools.getDefault("Browser.CategoryIcon"));
    /** Apply icon. */
    public static final ImageIcon APPLY_ICON =
            Tools.createImageIcon(Tools.getDefault("Browser.ApplyIcon"));
    /** Revert icon. */
    public static final ImageIcon REVERT_ICON =
            Tools.createImageIcon(Tools.getDefault("Browser.RevertIcon"));
    /** Actions memu icon. */
    public static final ImageIcon MENU_ICON =
            Tools.createImageIcon(Tools.getDefault("Browser.MenuIcon"));
    /** Color of the most of backgrounds. */
    public static final Color PANEL_BACKGROUND =
                    Tools.getDefaultColor("ViewPanel.Background");
    /** Color of the button panel backgrounds. */
    public static final Color BUTTON_PANEL_BACKGROUND =
                     Tools.getDefaultColor("ViewPanel.ButtonPanel.Background");
    /** Color of the status backgrounds. */
    public static final Color STATUS_BACKGROUND =
                          Tools.getDefaultColor("ViewPanel.Status.Background");
    /** Color of the extra panel with advanced options. */
    static final Color EXTRA_PANEL_BACKGROUND =
                    Tools.getDefaultColor("ViewPanel.Status.Background");
    /** DRBD test lock. */
    private final Lock mDRBDtestLock = new ReentrantLock();

    /** Sets the top of the menu tree. */
    protected final void setTreeTop() {
        treeTop = new DefaultMutableTreeNode(new CategoryInfo(
                                        Tools.getString("Browser.Resources"),
                                        this));
        treeModel = new DefaultTreeModel(treeTop);
    }

    /** Sets the top of the menu tree. */
    protected final void setTreeTop(final Info info) {
        treeTop = new DefaultMutableTreeNode(info);
        treeModel = new DefaultTreeModel(treeTop);
    }

    /** Sets the tree instance variable. */
    protected final void setTree(final JTree tree) {
        this.tree = tree;
    }

    /** Returns the tree object. */
    public final JTree getTree() {
        return tree;
    }

    /** Repaints the menu tree. */
    public final void repaintTree() {
        final JTree t = tree;
        if (t != null) {
            t.repaint();
        }
    }

    /** Gets node that is on the top of the tree. */
    final DefaultMutableTreeNode getTreeTop() {
        return treeTop;
    }

    /** Reloads the node. */
    public final void reloadAndWait(final DefaultMutableTreeNode node,
                                    final boolean select) {
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

    /** Reloads the node. */
    public final void reload(final DefaultMutableTreeNode node,
                             final boolean select) {
        final JTree t = tree;
        DefaultMutableTreeNode oldN = null;
        if (t != null) {
            oldN = (DefaultMutableTreeNode) t.getLastSelectedPathComponent();
        }
        final DefaultMutableTreeNode oldNode = oldN;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (node != null) {
                    treeModel.reload(node);
                }
            }
        });
        if (!select && t != null && oldNode != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    /* if don't want to select, we reselect the old path. */
                    //t.setSelectionPath(path);
                    treeModel.reload(oldNode);
                }
            });
        }
    }

    /** Sets the node change for the node. */
    public final void nodeChanged(final DefaultMutableTreeNode node) {
        final String stacktrace = Tools.getStackTrace();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    treeModel.nodeChanged(node);
                } catch (Exception e) {
                    Tools.appError(node.getUserObject()
                                   + " node changed error: ", e);
                    Tools.appWarning("stacktrace: " + stacktrace);
                }
            }
        });
    }

    /** Adds the node to the top level. */
    protected final void topAdd(final DefaultMutableTreeNode node) {
        treeTop.add(node);
    }

    /** Repaints the split pane. */
    protected final void repaintSplitPane() {
        if (infoPanelSplitPane != null) {
            infoPanelSplitPane.repaint();
        }
    }

    /** Sets node variable in the info object that this tree node points to. */
    public final void setNode(final DefaultMutableTreeNode node) {
        ((Info) node.getUserObject()).setNode(node);
    }

    /** Gets tree model object. */
    final DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * Returns panel with info of some resource from Info object. The info is
     * specified in getInfoPanel method in the Info object. If a resource has a
     * graphical view, it returns a split pane with this view and the info
     * underneath.
     */
    final JComponent getInfoPanel(final Object nodeInfo,
                                  final boolean disabledDuringLoad) {
        if (nodeInfo == null) {
            return null;
        }
        final JPanel gView = ((Info) nodeInfo).getGraphicalView();
        final JComponent iPanel = ((Info) nodeInfo).getInfoPanel();
        if (gView == null) {
            return iPanel;
        } else {
            final int maxWidth = ClusterBrowser.SERVICE_LABEL_WIDTH
                                 + ClusterBrowser.SERVICE_FIELD_WIDTH
                                 + 36;
            final Dimension d = iPanel.getPreferredSize();
            iPanel.setMinimumSize(new Dimension(maxWidth, 0));
            iPanel.setMaximumSize(new Dimension(maxWidth,
                                                (int) Short.MAX_VALUE));
            if (infoPanelSplitPane != null) {
                if (!disabledDuringLoad) {
                    final int loc = infoPanelSplitPane.getDividerLocation();
                    infoPanelSplitPane.setLeftComponent(gView);
                    infoPanelSplitPane.setRightComponent(iPanel);
                    infoPanelSplitPane.setDividerLocation(loc);
                }
                return infoPanelSplitPane;
            }
            final JSplitPane newSplitPane =
                                    new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                   gView,
                                                   iPanel);
            newSplitPane.setResizeWeight(1);
            newSplitPane.setOneTouchExpandable(true);
            infoPanelSplitPane = newSplitPane;
            infoPanelSplitPane.repaint();
            return infoPanelSplitPane;
        }
    }

    /** Returns cell rendererer for tree. */
    final CellRenderer getCellRenderer() {
        return new CellRenderer();
    }

    /** Renders the cells for the menu. */
    static class CellRenderer extends DefaultTreeCellRenderer {
        /** Serial version UUID. */
        private static final long serialVersionUID = 1L;

        /** Creates new CellRenderer object. */
        CellRenderer() {
            super();
            setBackgroundNonSelectionColor(PANEL_BACKGROUND);
            setBackgroundSelectionColor(
                        Tools.getDefaultColor("ViewPanel.Status.Background"));
            setTextNonSelectionColor(
                        Tools.getDefaultColor("ViewPanel.Foreground"));
            setTextSelectionColor(
                        Tools.getDefaultColor("ViewPanel.Status.Foreground"));
        }

        /**
         * Returns the CellRenderer component, setting up the icons and
         * tooltips.
         */
        public Component getTreeCellRendererComponent(final JTree tree,
                                                      final Object value,
                                                      final boolean sel,
                                                      final boolean expanded,
                                                      final boolean leaf,
                                                      final int row,
                                                      final boolean hasFocus) {

            super.getTreeCellRendererComponent(
                            tree, value, sel,
                            expanded, leaf, row,
                            hasFocus);
            final Info i =
                    (Info) ((DefaultMutableTreeNode) value).getUserObject();
            if (i == null) {
                return this;
            }
            if (leaf) {
                final ImageIcon icon = i.getMenuIcon(false);
                if (icon != null) {
                    setIcon(icon);
                }
                setToolTipText("");
            } else {
                setToolTipText("");
                ImageIcon icon = i.getCategoryIcon(false);
                if (icon == null) {
                    icon = CATEGORY_ICON;
                }
                setIcon(icon);
            }

            return this;
        }
    }

    /** Acquire drbd test lock. */
    public final void drbdtestLockAcquire() {
        mDRBDtestLock.lock();
    }

    /** Release drbd test lock. */
    public final void drbdtestLockRelease() {
        mDRBDtestLock.unlock();
    }

    /** Selects specified path. */
    protected void selectPath(final Object[] path) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final TreePath tp = new TreePath(path);
                getTree().expandPath(tp);
                getTree().setSelectionPath(tp);
            }
        });
    }

    /** Add node. */
    public final void addNode(final DefaultMutableTreeNode node,
                              final DefaultMutableTreeNode child) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              node.add(child);
            }
        });
    }
}
