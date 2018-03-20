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
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.function.BiConsumer;

@Named
@Singleton
public class TreeMenuController {

    private static final Logger LOG = LoggerFactory.getLogger(TreeMenuController.class);
    private DefaultTreeModel treeModel;
    private JTree tree;
    private final SwingUtils swingUtils;

    private volatile boolean disableListeners = true;
    private BiConsumer<InfoPresenter, Boolean> onSelect;

    @Inject
    public TreeMenuController(final SwingUtils swingUtils) {
        this.swingUtils = swingUtils;
    }

    public final DefaultMutableTreeNode createMenuTreeTop(final InfoPresenter infoPresenter) {
        final DefaultMutableTreeNode treeTop = new DefaultMutableTreeNode(infoPresenter);
        treeModel = new DefaultTreeModel(treeTop);
        tree = createTree(treeModel);
        return treeTop;
    }

    public DefaultMutableTreeNode createMenuItem(final DefaultMutableTreeNode parent,
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
        infoPresenter.setNode(node);
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

    public final void reloadNodeDontSelect(final TreeNode node) {
        reloadNode(node, false);
    }

    public final void reloadNode(final TreeNode node) {
        reloadNode(node, true);
    }

    private void reloadNode(final TreeNode node, final boolean select) {
        swingUtils.invokeInEdt(() -> {
            final DefaultMutableTreeNode oldNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            treeModel.reload(node);
            if (!select) {
                /* if don't want to select, we reselect the old path. */
                treeModel.reload(oldNode);
            }
        });
    }

    public final void nodeChanged(final DefaultMutableTreeNode node) {
        if (onSelect != null) {
            getUserObject(node).ifPresent(infoPresenter -> onSelect.accept(infoPresenter, disableListeners));
        }
    }

    public void expandAndSelect(final Object[] path) {
        if (disableListeners) {
            return;
        }
        swingUtils.invokeInEdt(() -> {
            final TreePath tp = new TreePath(path);
            tree.expandPath(tp);
            tree.setSelectionPath(tp);
        });
    }

    public void moveNodeUpToPosition(final DefaultMutableTreeNode node, final int position) {
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

    public void sortChildrenLeavingNewUp(final DefaultMutableTreeNode parent) {
        swingUtils.invokeInEdt(() -> {
            int someVar = 0;
            for (int j = 0; j < parent.getChildCount(); j++) {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getChildAt(j);
                final EditableInfo info = (EditableInfo) node.getUserObject();
                final String name = info.getName();
                if (someVar > 0) {
                    final DefaultMutableTreeNode prev = (DefaultMutableTreeNode) parent.getChildAt(j - 1);
                    final EditableInfo prevI = (EditableInfo) prev.getUserObject();
                    if (prevI.getClass().getName().equals(info.getClass().getName())) {
                        final String prevN = prevI.getName();
                        if (!prevI.isNew() && !info.isNew() && (prevN != null && prevN.compareTo(name) > 0)) {
                            parent.remove(j);
                            parent.insert(node, j - 1);
                        }
                    } else {
                        someVar = 0;
                    }
                }
                someVar++;
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
        final InfoPresenter info = (InfoPresenter) nodeToRemove.getUserObject();
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

    private JTree createTree(final DefaultTreeModel treeModel) {
        val tree = new JTree(treeModel);
        tree.setOpaque(true);
        tree.setBackground(Tools.getDefaultColor("ViewPanel.Background"));
        tree.setToggleClickCount(2);
        tree.addMouseListener(new PopupListener());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(createCellRenderer());
        return tree;
    }

    private class PopupListener implements MouseListener {
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
//            final int selRow = tree.getRowForLocation(e.getX(), e.getY());
            final TreePath selPath = getPathForLocation(e);
            if (selPath != null && e.getButton() > 1) {
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
    }

    protected TreePath getPathForLocation(MouseEvent e) {
        return tree.getPathForLocation(e.getX(), e.getY());
    }

    public void addListeners(final BiConsumer<InfoPresenter, Boolean> onSelect) {
        this.onSelect = onSelect;
        // Listen for when the selection changes.
        tree.addTreeSelectionListener(e -> {
            getUserObject(tree.getLastSelectedPathComponent()).ifPresent(nodeInfo -> {
                if (!disableListeners) {
                    onSelect.accept(nodeInfo, disableListeners);
                }
            });
        });

        tree.getModel().addTreeModelListener(
                new TreeModelListener() {
                    @Override
                    public void treeNodesChanged(final TreeModelEvent e) {
                        val selected = e.getChildren();
                        if (selected != null && selected.length > 0) {
                            getUserObject(selected[0]).ifPresent(info -> {
                                if (!disableListeners) {
                                    onSelect.accept(info, disableListeners);
                                }
                            });
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
                        if (!disableListeners) {
                            val tp = new TreePath(path);
                            getUserObject(tp.getLastPathComponent()).ifPresent(infoPresenter -> {
                                if (infoPresenter instanceof EditableInfo) {
                                    swingUtils.invokeInEdt(() -> tree.setSelectionPath(tp));
                                }
                            });
                        }
                    }
                }
        );
    }

    private Optional<InfoPresenter> getUserObject(final Object nodeObject) {
        if (nodeObject == null) {
            return Optional.empty();
        }
        val node = (DefaultMutableTreeNode) nodeObject;
        if (nodeNotShowing(node)) {
            return Optional.empty();
        }
        return Optional.ofNullable((InfoPresenter) node.getUserObject());
    }

    private boolean nodeNotShowing(DefaultMutableTreeNode node) {
        return node.getParent() == null;
    }

    public final boolean isDisableListeners() {
        return disableListeners;
    }

    public final void setDisableListeners(final boolean disableListeners) {
        this.disableListeners = disableListeners;
    }
}
