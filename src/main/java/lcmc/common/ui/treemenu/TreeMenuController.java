/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.common.ui.treemenu;

import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.CategoryInfo;
import lcmc.common.ui.Info;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.vm.ui.resource.DiskInfo;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

@Named
@Singleton
public class TreeMenuController {
    private static final Logger LOG = LoggerFactory.getLogger(TreeMenuController.class);
    private DefaultTreeModel treeModel;
    private JTree tree;
    @Inject
    private Application application;
    @Resource(name="categoryInfo")
    private CategoryInfo resourcesCategory;

    public final DefaultMutableTreeNode createMenuTreeTop() {
        resourcesCategory.init(Tools.getString("Browser.Resources"), null);
        final DefaultMutableTreeNode treeTop = new DefaultMutableTreeNode(resourcesCategory);
        treeModel = new DefaultTreeModel(treeTop);
        return treeTop;
    }

    public final DefaultMutableTreeNode createMenuTreeTop(final Info info) {
        final DefaultMutableTreeNode treeTop = new DefaultMutableTreeNode(info);
        treeModel = new DefaultTreeModel(treeTop);
        return treeTop;
    }

    public DefaultMutableTreeNode createMenuItem(final DefaultMutableTreeNode parent, Info info) {
        final DefaultMutableTreeNode child = createMenuItem(info);
        addChild(parent, child);
        return child;
    }

    public DefaultMutableTreeNode createMenuItem(DefaultMutableTreeNode parent, Info info, int position) {
        final DefaultMutableTreeNode child = createMenuItem(info);
        insertNode(parent, child, position);
        return child;
    }


    public DefaultMutableTreeNode createMenuItem(Info info) {
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(info);
        info.setNode(node);
        return node;
    }

    public final JTree getMenuTree() {
        return tree;
    }

    public final void repaintMenuTree() {
        application.invokeInEdt(new Runnable() {
            @Override
            public void run() {
                final JTree t = tree;
                if (t != null) {
                    t.repaint();
                }
            }
        });
    }

    public final void reloadNode(final TreeNode node, final boolean select) {
        application.invokeInEdt(new Runnable() {
            @Override
            public void run() {
                final DefaultMutableTreeNode oldNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if (node != null) {
                    treeModel.reload(node);
                }
                if (!select && oldNode != null) {
                    /* if don't want to select, we reselect the old path. */
                    treeModel.reload(oldNode);
                }
            }
        });
    }

    public final void nodeChanged(final DefaultMutableTreeNode node) {
        final String stacktrace = Tools.getStackTrace();
        application.invokeInEdt(new Runnable() {
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

    /** Sets node variable in the info object that this tree node points to. */
    @Deprecated
    public final void setNode(final DefaultMutableTreeNode node) {
        ((Info) node.getUserObject()).setNode(node);
    }

    public final DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    public void selectPath(final Object[] path) {
        application.invokeInEdt(new Runnable() {
            @Override
            public void run() {
                final TreePath tp = new TreePath(path);
                tree.expandPath(tp);
                tree.setSelectionPath(tp);
            }
        });
    }

    public final TreeCellRenderer createCellRenderer() {
        return new CellRenderer();
    }

    public void moveNodeToPosition(final DefaultMutableTreeNode node, final int position) {
        application.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                final MutableTreeNode parent = (MutableTreeNode) node.getParent();
                if (parent != null) {
                    final int i = parent.getIndex(node);
                    if (i > position) {
                        parent.remove(node);
                        parent.insert(node, position);
                        reloadNode(parent, false);
                    }
                }
            }
        });
    }

    public void init() {
        tree = new JTree(getTreeModel());
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
        tree.setCellRenderer(createCellRenderer());
    }

    public final void removeNode(final DefaultMutableTreeNode node) {
        application.invokeInEdt(new Runnable() {
            @Override
            public void run() {
                removeNodeAndSelectParent(node);
            }
        });
    }

//    @Deprecated TODO
    public List<Info> nodesToInfos(final Enumeration<DefaultMutableTreeNode> e) {
        final List<Info> list = new ArrayList<Info>();
        application.invokeAndWait(new Runnable() {
            public void run() {
                while (e.hasMoreElements()) {
                    final DefaultMutableTreeNode n = e.nextElement();
                    list.add((Info) n.getUserObject());
                }
            }
        });
        return list;
    }

    public void addChild(final DefaultMutableTreeNode parent, final MutableTreeNode child) {
        final DefaultMutableTreeNode parent0 = parent;
        if (parent0 == null) {
            LOG.appError("addChild: parent cannot be null");
            return;
        }
        application.invokeInEdt(new Runnable() {
            @Override
            public void run() {
                parent0.add(child);
            }
        });
    }

    public int getIndex(final DefaultMutableTreeNode parent, final DefaultMutableTreeNode child) {
        final IntResult intResult = new IntResult();
        application.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                intResult.set(parent.getIndex(child));
            }
        });
        return intResult.get();
    }

    public int getChildCount(final DefaultMutableTreeNode parent) {
        final IntResult intResult = new IntResult();
        application.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                intResult.set(parent.getChildCount());
            }
        });
        return intResult.get();
    }

    public void removeFromParent(final Collection<DefaultMutableTreeNode> nodes) {
        application.invokeInEdt(new Runnable() {
            @Override
            public void run() {
                for (final DefaultMutableTreeNode node : nodes) {
                    removeNodeAndSelectParent(node);
                }
            }
        });
    }

    public void sortChildrenWithNewUp(final DefaultMutableTreeNode parent) {
        application.invokeInEdt(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                for (int j = 0; j < parent.getChildCount(); j++) {
                    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getChildAt(j);
                    final Info info = (Info) node.getUserObject();
                    final String name = info.getName();
                    if (i > 0) {
                        final DefaultMutableTreeNode prev = (DefaultMutableTreeNode) parent.getChildAt(j - 1);
                        final Info prevI = (Info) prev.getUserObject();
                        if (prevI.getClass().getName().equals(info.getClass().getName())) {
                            final String prevN = prevI.getName();
                            if (!prevI.getResource().isNew()
                                    && !info.getResource().isNew()
                                    && (prevN != null && prevN.compareTo(name) > 0)) {
                                parent.remove(j);
                                parent.insert(node, j - 1);
                            }
                        } else {
                            i = 0;
                        }
                    }
                    i++;
                }
            }
        });
    }

    public void removeChildren(final DefaultMutableTreeNode parent) {
        application.invokeInEdt(new Runnable() {
            @Override
            public void run() {
                parent.removeAllChildren();
            }
        });
    }


    private void removeNodeAndSelectParent(DefaultMutableTreeNode node) {
        final DefaultMutableTreeNode nodeToRemove = node;
        if (nodeToRemove == null) {
            return;
        }
        final MutableTreeNode parent = (MutableTreeNode) nodeToRemove.getParent();
        nodeToRemove.removeFromParent();
        final Info info = (Info) nodeToRemove.getUserObject();
        if (info != null) {
            info.setNode(null);
        }
        if (parent != null) {
            reloadNode(parent, true);
        }
    }

    private void insertNode(final DefaultMutableTreeNode parent, final DefaultMutableTreeNode child, final int i) {
        application.invokeInEdt(new Runnable() {
            @Override
            public void run() {
                parent.insert(child, i);
            }
        });
    }

    private class IntResult {
        volatile int result = 0;

        void set(final int result) {
            this.result = result;
        }

        int get() {
            return result;
        }
    }
}
