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

import lcmc.cluster.ui.network.InfoPresenter;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.EditableInfo;
import lcmc.common.ui.Info;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.tree.*;
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
    private final SwingUtils swingUtils;

    @Inject
    public TreeMenuController(final SwingUtils swingUtils) {
        this.swingUtils = swingUtils;
    }

    public final DefaultMutableTreeNode createMenuTreeTop(final InfoPresenter infoPresenter) {
        final DefaultMutableTreeNode treeTop = new DefaultMutableTreeNode(infoPresenter);
        treeModel = new DefaultTreeModel(treeTop);
        createTree();
        return treeTop;
    }

    private void createTree() {
        tree = new JTree(treeModel);
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
                    final InfoPresenter infoPresenter =
                            (InfoPresenter) ((DefaultMutableTreeNode) selPath.getLastPathComponent()).getUserObject();
                    if (infoPresenter != null) {
                        infoPresenter.showPopup(tree, e.getX(), e.getY());
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

    public DefaultMutableTreeNode createMenuItem(
            final DefaultMutableTreeNode parent,
            final InfoPresenter infoPresenter) {
        final DefaultMutableTreeNode child = createMenuItem(infoPresenter);
        addChild(parent, child);
        return child;
    }

    public DefaultMutableTreeNode createMenuItem(
            final DefaultMutableTreeNode parent,
            final InfoPresenter infoPresenter,
            final int position) {
        final DefaultMutableTreeNode child = createMenuItem(infoPresenter);
        insertNode(parent, child, position);
        return child;
    }


    public DefaultMutableTreeNode createMenuItem(final InfoPresenter infoPresenter) {
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(infoPresenter);
        if (infoPresenter instanceof Info) {
            ((Info) infoPresenter).setNode(node);
        }
        return node;
    }

    public final JTree getMenuTree() {
        return tree;
    }

    public final void repaintMenuTree() {
        swingUtils.invokeInEdt(() -> {
            final JTree t = tree;
            if (t != null) {
                t.repaint();
            }
        });
    }

    public final void reloadNode(final TreeNode node, final boolean select) {
        swingUtils.invokeInEdt(() -> {
            final DefaultMutableTreeNode oldNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node != null) {
                treeModel.reload(node);
            }
            if (!select && oldNode != null) {
                /* if don't want to select, we reselect the old path. */
                treeModel.reload(oldNode);
            }
        });
    }

    public final void nodeChanged(final DefaultMutableTreeNode node) {
        final String stacktrace = Tools.getStackTrace();
        swingUtils.invokeInEdt(() -> {
            try {
                treeModel.nodeChanged(node);
            } catch (final RuntimeException e) {
                LOG.appError("nodeChangedAndWait: " + node.getUserObject()
                        + " node changed error:\n"
                        + stacktrace + "\n\n", e);
            }
        });
    }

    /** Sets node variable in the info object that this tree node points to. */
    @Deprecated
    public final void setNode(final DefaultMutableTreeNode node) {
        ((Info) node.getUserObject()).setNode(node);
    }

    public void selectPath(final Object[] path) {
        swingUtils.invokeInEdt(() -> {
            final TreePath tp = new TreePath(path);
            tree.expandPath(tp);
            tree.setSelectionPath(tp);
        });
    }

    public void moveNodeToPosition(final DefaultMutableTreeNode node, final int position) {
        swingUtils.invokeAndWait(() -> {
            final MutableTreeNode parent = (MutableTreeNode) node.getParent();
            if (parent != null) {
                final int i = parent.getIndex(node);
                if (i > position) {
                    parent.remove(node);
                    parent.insert(node, position);
                    reloadNode(parent, false);
                }
            }
        });
    }

    public final void removeNode(final DefaultMutableTreeNode node) {
        swingUtils.invokeInEdt(() -> removeNodeAndSelectParent(node));
    }

    @Deprecated //TODO
    public List<Info> nodesToInfos(final Enumeration<DefaultMutableTreeNode> e) {
        final List<Info> list = new ArrayList<>();
        swingUtils.invokeAndWait(() -> {
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode n = e.nextElement();
                list.add((Info) n.getUserObject());
            }
        });
        return list;
    }

    public void addChild(final DefaultMutableTreeNode parent, final MutableTreeNode child) {
        if (parent == null) {
            LOG.appError("addChild: parent cannot be null");
            return;
        }
        swingUtils.invokeInEdt(() -> parent.add(child));
    }

    public int getIndex(final DefaultMutableTreeNode parent, final DefaultMutableTreeNode child) {
        final IntResult intResult = new IntResult();
        swingUtils.invokeAndWait(() -> intResult.set(parent.getIndex(child)));
        return intResult.get();
    }

    public int getChildCount(final DefaultMutableTreeNode parent) {
        final IntResult intResult = new IntResult();
        swingUtils.invokeAndWait(() -> intResult.set(parent.getChildCount()));
        return intResult.get();
    }

    public void removeFromParent(final Collection<DefaultMutableTreeNode> nodes) {
        swingUtils.invokeInEdt(() -> {
            for (final DefaultMutableTreeNode node : nodes) {
                removeNodeAndSelectParent(node);
            }
        });
    }

    public void sortChildrenWithNewUp(final DefaultMutableTreeNode parent) {
        swingUtils.invokeInEdt(() -> {
            int i = 0;
            for (int j = 0; j < parent.getChildCount(); j++) {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getChildAt(j);
                final EditableInfo info = (EditableInfo) node.getUserObject();
                final String name = info.getName();
                if (i > 0) {
                    final DefaultMutableTreeNode prev = (DefaultMutableTreeNode) parent.getChildAt(j - 1);
                    final EditableInfo prevI = (EditableInfo) prev.getUserObject();
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
        });
    }

    public void removeChildren(final DefaultMutableTreeNode parent) {
        swingUtils.invokeInEdt(parent::removeAllChildren);
    }


    private void removeNodeAndSelectParent(final DefaultMutableTreeNode nodeToRemove) {
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
        swingUtils.invokeInEdt(() -> parent.insert(child, i));
    }

    private static class IntResult {
        volatile int result = 0;

        void set(final int result) {
            this.result = result;
        }

        int get() {
            return result;
        }
    }

    private TreeCellRenderer createCellRenderer() {
        return new CellRenderer();
    }
}
