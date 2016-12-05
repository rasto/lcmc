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

import lcmc.cluster.ui.network.InfoPresenter;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.SwingUtils;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    private SwingUtils swingUtils;

    public ViewPanel() {
        super(new BorderLayout());
        setBackground(Tools.getDefaultColor("ViewPanel.Status.Background"));
    }

    public final JTree createPanels(final JTree tree) {

        final JScrollPane resourcesTreePane = new JScrollPane(tree);

        final JPanel resourceInfo = new JPanel();
        viewSP = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, resourcesTreePane, resourceInfo);

        resourcesTreePane.setMinimumSize(MENU_TREE_MIN_SIZE);
        resourceInfo.setMinimumSize(INFO_PANEL_MIN_SIZE);
        viewSP.setDividerLocation(DIVIDER_LOCATION);
        viewSP.setPreferredSize(MENU_TREE_SIZE);
        add(viewSP);

        return tree;
    }

    public void addListeners(final Browser browser, final JTree tree) {
        // Listen for when the selection changes.
        tree.addTreeSelectionListener(e -> {
            getUserObject(tree.getLastSelectedPathComponent()).ifPresent(nodeInfo -> {
                setRightComponentInView(browser, (Info) nodeInfo);
            });
        });

        tree.getModel().addTreeModelListener(
                new TreeModelListener() {
                    @Override
                    public void treeNodesChanged(final TreeModelEvent e) {
                        if (!disabledDuringLoad) {
                            val selected = e.getChildren();
                            if (selected != null && selected.length > 0) {
                                getUserObject(selected[0]).ifPresent(info -> {
                                    setRightComponentInView(browser, (Info) info);
                                });
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

    /** Returns whether expanding of paths is disabled during the initial load.
     */
    public final boolean isDisabledDuringLoad() {
        return disabledDuringLoad;
    }
    /** Sets if expanding of paths should be disabled during the initial load.*/
    public final void setDisabledDuringLoad(final boolean disabledDuringLoad) {
        this.disabledDuringLoad = disabledDuringLoad;
    }

    public final void setRightComponentInView(final Browser browser, final Info nodeInfo) {
        if (viewSP != null) {
            swingUtils.invokeInEdt(() -> {
                if (!mSetPanelLock.tryLock()) {
                    return;
                }
                final JComponent p = browser.getInfoPanel(nodeInfo, disabledDuringLoad);
                lastSelectedInfo = nodeInfo;
                if (!disabledDuringLoad && p != null) {
                    final int loc = viewSP.getDividerLocation();
                    if (viewSP.getRightComponent() != p) {
                        viewSP.setRightComponent(p);
                    }
                    viewSP.setDividerLocation(loc);
                }
                mSetPanelLock.unlock();
            });
        }
    }

    public final void reloadRightComponent() {
        final Info lsi = lastSelectedInfo;
        if (lsi != null) {
            setRightComponentInView(lsi.getBrowser(), lsi);
        }
    }

    public final Info getLastSelectedInfo() {
        return lastSelectedInfo;
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
}
