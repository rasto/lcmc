/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2016, Rastislav Levrinc.
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
import lcmc.common.ui.Info;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class ClusterTreeMenu {
    private final TreeMenuController treeMenuController;

    public void addChild(final DefaultMutableTreeNode parent, final MutableTreeNode child) {
        treeMenuController.addChild(parent, child);
    }

    public final void setDisableListeners(final boolean disableListeners) {
        treeMenuController.setDisableListeners(disableListeners);
    }

    public final void reloadNodeDontSelect(final TreeNode node) {
        treeMenuController.reloadNodeDontSelect(node);
    }

    public final void reloadNode(final TreeNode node) {
        treeMenuController.reloadNode(node);
    }

    public DefaultMutableTreeNode createMenuItem(final DefaultMutableTreeNode parent, final InfoPresenter infoPresenter) {
        return treeMenuController.createMenuItem(parent, infoPresenter);
    }

    public List<Info> nodesToInfos(final Enumeration<TreeNode> e) {
        return treeMenuController.nodesToInfos(e);
    }

    public void expandAndSelect(final Object[] path) {
        treeMenuController.expandAndSelect(path);
    }

    public void nodeChanged(final DefaultMutableTreeNode node) {
        treeMenuController.nodeChanged(node);
    }

    public final JTree getMenuTree() {
        return treeMenuController.getMenuTree();
    }

    public void addListeners(final BiConsumer<InfoPresenter, Boolean> onSelect) {
        treeMenuController.addListeners(onSelect);
    }

    public void removeChildren(final DefaultMutableTreeNode parent) {
        treeMenuController.removeChildren(parent);
    }

    public DefaultMutableTreeNode createMenuItem(
            final DefaultMutableTreeNode parent,
            final InfoPresenter infoPresenter,
            final int position) {
        return treeMenuController.createMenuItem(parent, infoPresenter, position);
    }

    public boolean isDisableListeners() {
        return treeMenuController.isDisableListeners();
    }

    public final void repaintMenuTree() {
        treeMenuController.repaintMenuTree();
    }

    public final DefaultMutableTreeNode createMenuTreeTop(final InfoPresenter infoPresenter) {
        return treeMenuController.createMenuTreeTop(infoPresenter);
    }

    public void moveNodeUpToPosition(final DefaultMutableTreeNode node, final int position) {
        treeMenuController.moveNodeUpToPosition(node, position);
    }

    public void removeFromParent(final Collection<DefaultMutableTreeNode> nodes) {
        treeMenuController.removeFromParent(nodes);
    }

    public final void removeNode(final DefaultMutableTreeNode node) {
        treeMenuController.removeNode(node);
    }

    public void sortChildrenLeavingNewUp(final DefaultMutableTreeNode parent) {
        treeMenuController.sortChildrenLeavingNewUp(parent);
    }

    public int getIndex(final DefaultMutableTreeNode parent, final DefaultMutableTreeNode child) {
        return treeMenuController.getIndex(parent, child);
    }

    public int getChildCount(final DefaultMutableTreeNode parent) {
        return treeMenuController.getChildCount(parent);
    }
}
