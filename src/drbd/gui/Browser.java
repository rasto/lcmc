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

import drbd.utilities.Tools;
import drbd.gui.resources.Info;
import drbd.gui.resources.CategoryInfo;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

import javax.swing.JSplitPane;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

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
    /** Actions memu icon. */
    public static final ImageIcon ACTIONS_ICON =
            Tools.createImageIcon(Tools.getDefault("Browser.ActionsIcon"));
    /** Color of the most of backgrounds. */
    public static final Color PANEL_BACKGROUND =
                    Tools.getDefaultColor("ViewPanel.Background");
    /** Color of the extra panel with advanced options. */
    public static final Color EXTRA_PANEL_BACKGROUND =
                    Tools.getDefaultColor("ViewPanel.Status.Background");
    /** Color of the status backgrounds. */
    public static final Color STATUS_BACKGROUND =
                          Tools.getDefaultColor("ViewPanel.Status.Background");
    /** DRBD test lock. */
    private final Mutex mDRBDtestLock = new Mutex();

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

    /**
     * Sets the tree instance variable.
     */
    protected final void setTree(final JTree tree) {
        this.tree = tree;
    }

    /**
     * Returns the tree object.
     */
    public final JTree getTree() {
        return tree;
    }

    /**
     * Repaints the menu tree.
     */
    public final void repaintTree() {
        tree.repaint();
    }

    /**
     * Gets node that is on the top of the tree.
     */
    public final DefaultMutableTreeNode getTreeTop() {
        return treeTop;
    }
    //public DefaultMutableTreeNode getTreeTop(Info info) {
    //    treeTop = new DefaultMutableTreeNode(info);
    //    treeModel = new DefaultTreeModel(treeTop);
    //    return treeTop;
    //}

    /** Reloads the node. */
    public final void reload(final DefaultMutableTreeNode node) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                treeModel.reload(node);
            }
        });
    }

    /** Sets the node change for the node. */
    public final void nodeChanged(final DefaultMutableTreeNode node) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                treeModel.nodeChanged(node);
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

    /**
     * Sets node variable in the info object that this tree node points to.
     */
    protected final void setNode(final DefaultMutableTreeNode node) {
        ((Info) node.getUserObject()).setNode(node);
    }

    /**
     * Gets tree model object.
     */
    public final DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * Returns panel with info of some resource from Info object. The info is
     * specified in getInfoPanel method in the Info object. If a resource has a
     * graphical view, it returns a split pane with this view and the info
     * underneath.
     */
    public final JComponent getInfoPanel(final Object nodeInfo) {
        if (nodeInfo == null) {
            return null;
        }
        final JPanel gView = ((Info) nodeInfo).getGraphicalView();
        final JComponent iPanel = ((Info) nodeInfo).getInfoPanel();
        if (gView == null) {
            return iPanel;
        } else {
            final int width = ClusterBrowser.SERVICE_LABEL_WIDTH
                              + ClusterBrowser.SERVICE_FIELD_WIDTH
                              + 36;
            final Dimension d = iPanel.getPreferredSize();
            iPanel.setMinimumSize(new Dimension(width, (int) d.getHeight()));
            iPanel.setMaximumSize(new Dimension(width, (int) Short.MAX_VALUE));
            final JSplitPane newSplitPane =
                                    new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                   gView,
                                                   iPanel);
            newSplitPane.setDividerSize(0);
            newSplitPane.setResizeWeight(1);
            infoPanelSplitPane = newSplitPane;
            infoPanelSplitPane.repaint();
            return infoPanelSplitPane;
        }
    }

    /**
     * Returns cell rendererer for tree.
     */
    public final CellRenderer getCellRenderer() {
        return new CellRenderer();
    }

    /**
     * Renders the cells for the menu.
     */
    class CellRenderer extends DefaultTreeCellRenderer {
        /** Serial version UUID. */
        private static final long serialVersionUID = 1L;

        /**
         * Creates new CellRenderer object.
         */
        public CellRenderer() {
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
        public Component getTreeCellRendererComponent(
                            final JTree tree,
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
            if (leaf) {
                final ImageIcon icon = i.getMenuIcon(false);
                if (icon != null) {
                    setIcon(icon);
                }
                setToolTipText(null);
            } else {
                setToolTipText(null);
                ImageIcon icon = i.getCategoryIcon();
                if (icon == null) {
                    icon = CATEGORY_ICON;
                }
                setIcon(icon);
            }

            return this;
        }
    }

    /**
     * Acquire drbd test lock.
     */
    public final void drbdtestLockAcquire() {
        try {
            mDRBDtestLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Release drbd test lock.
     */
    public final void drbdtestLockRelease() {
        mDRBDtestLock.release();
    }
}
