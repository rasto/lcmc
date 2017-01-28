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
import lcmc.common.ui.CategoryInfo;
import lcmc.common.ui.EditableInfo;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.vm.ui.resource.ParallelInfo;
import lcmc.vm.ui.resource.VideoInfo;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TreeMenuControllerTest {
    private SwingUtils swingUtils;
    @Mock
    private CategoryInfo categoryInfo;
    @Mock
    private InfoPresenter infoPresenter;
    @Mock
    private InfoPresenter infoPresenter2;
    @Mock
    private EditableInfo editableInfo;

    private ClusterTreeMenu clusterTreeMenu;
    private DefaultMutableTreeNode menuTreeTop;
    private TreePath treePath = null;

    @Before
    public void setUp() {
        swingUtils = new SwingUtils() {
            @Override
            public void invokeInEdt(final Runnable runnable) {
                runnable.run();
            }

            @Override
            public void invokeAndWait(final Runnable runnable) {
                runnable.run();
            }
        };
        clusterTreeMenu = new ClusterTreeMenu(swingUtils) {
            @Override
            protected TreePath getPathForLocation(MouseEvent e) {
                return treePath;
            }
        };
        menuTreeTop = clusterTreeMenu.createMenuTreeTop(categoryInfo);
    }

    @Test
    public void categoryInfoShouldBeOnTopOfTheClusterTreeMenu() {

        assertThat(menuTreeTop.getUserObject(), is(categoryInfo));
    }

    @Test
    public void shouldAddAChild() {
        clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter);

        final JTree menuTree = clusterTreeMenu.getMenuTree();
        clusterTreeMenu.reloadNode(menuTreeTop);

        assertThat(menuTree.getRowCount(), is(1));
        assertThat(clusterTreeMenu.getChildCount(menuTreeTop), is(1));
    }

    @Test
    public void shouldMakePopupOnMousePress() {
        val menuItem = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter);

        final JTree menuTree = clusterTreeMenu.getMenuTree();
        clusterTreeMenu.reloadNode(menuTreeTop);
        final int x = 100;
        final int y = 100;
        val mousePress = new MouseEvent(menuTree, 0, 0, 0, x, y, 1, true, 2);
        treePath = new TreePath(new Object[]{menuTreeTop, menuItem});

        Arrays.stream(menuTree.getMouseListeners()).forEach(listener -> {
            listener.mousePressed(mousePress);
        });

        verify(infoPresenter).showPopup(menuTree, x, y);
    }

    @Test
    public void shouldAddChildOnSpecificPosition() {
        clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter);
        val child2 = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter2, 0);

        clusterTreeMenu.reloadNode(menuTreeTop);

        assertThat(clusterTreeMenu.getMenuTree().getRowCount(), is(2));
        assertThat(childAt(0), is(child2));
    }

    @Test
    public void reloadNodeShouldSelect() {
        val child1 = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter);
        val child2 = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter2);

        final JTree menuTree = clusterTreeMenu.getMenuTree();

        menuTree.getModel().addTreeModelListener(
                new TreeModelListener() {
                    @Override public void treeNodesChanged(final TreeModelEvent e) { }
                    @Override public void treeNodesInserted(final TreeModelEvent e) { }
                    @Override public void treeNodesRemoved(final TreeModelEvent e) { }

                    @Override
                    public void treeStructureChanged(final TreeModelEvent e) {
                        menuTree.setSelectionPath(new TreePath(e.getPath()));
                    }
                }
        );
        clusterTreeMenu.reloadNode(child1);
        clusterTreeMenu.reloadNode(child2);

        val selected = (InfoPresenter) ((DefaultMutableTreeNode) menuTree.getLastSelectedPathComponent()).getUserObject();
        assertThat(selected, is(infoPresenter2));
    }

    @Test
    public void reloadNodeShouldSelectPreviouslySelectedNode() {
        val child1 = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter);
        val child2 = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter2);

        final JTree menuTree = clusterTreeMenu.getMenuTree();

        menuTree.getModel().addTreeModelListener(
                new TreeModelListener() {
                    @Override public void treeNodesChanged(final TreeModelEvent e) { }
                    @Override public void treeNodesInserted(final TreeModelEvent e) { }
                    @Override public void treeNodesRemoved(final TreeModelEvent e) { }

                    @Override
                    public void treeStructureChanged(final TreeModelEvent e) {
                        menuTree.setSelectionPath(new TreePath(e.getPath()));
                    }
                }
        );
        clusterTreeMenu.reloadNode(child1);
        clusterTreeMenu.reloadNodeDontSelect(child2);

        val selected = (InfoPresenter) ((DefaultMutableTreeNode) menuTree.getLastSelectedPathComponent()).getUserObject();
        assertThat(selected, is(infoPresenter));
    }

    @Test
    public void nodeChangedShouldTriggerTreeNodesChangesEvent() {
        val child = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter);
        clusterTreeMenu.setDisableListeners(false);

        final Boolean[] eventTriggered = new Boolean[]{false};

        clusterTreeMenu.addListeners((infoPresenter, disableListeners) -> {
            eventTriggered[0] = true;
        });

        clusterTreeMenu.nodeChanged(child);

        assertThat(eventTriggered[0], is(true));
    }

    @Test
    public void shouldMoveNodeToSpecificPosition() {
        val child1 = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter);
        val child2 = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter2);

        clusterTreeMenu.moveNodeUpToPosition(child2, 0);

        assertThat(childAt(0), is(child2));
    }

    @Test
    public void removeNodeAndSelectParent() {
        val child1 = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter);
        val child2 = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter2);

        clusterTreeMenu.removeNode(child1);

        assertThat(clusterTreeMenu.getMenuTree().getRowCount(), is(1));
        assertThat(childAt(0), is(child2));
    }

    @Test
    public void shouldFindIndexOfAChild() {
        val child1 = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter);
        val child2 = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter2);

        assertThat(clusterTreeMenu.getIndex(menuTreeTop, child2), is(1));
    }

    @Test
    public void shouldSortNodesByName() {
        val editableInfo1 = mock(EditableInfo.class);
        val editableInfo2 = mock(EditableInfo.class);
        given(editableInfo1.isNew()).willReturn(false);
        given(editableInfo2.isNew()).willReturn(false);
        given(editableInfo1.getName()).willReturn("b");
        given(editableInfo2.getName()).willReturn("a");

        val child1 = clusterTreeMenu.createMenuItem(menuTreeTop, editableInfo1);
        val child2 = clusterTreeMenu.createMenuItem(menuTreeTop, editableInfo2);

        clusterTreeMenu.sortChildrenLeavingNewUp(menuTreeTop);

        assertThat(childAt(0), is(child2));
        assertThat(childAt(1), is(child1));
    }

    @Test
    public void shouldSortNodesLeavingNewUp() {
        val editableInfo1 = mock(EditableInfo.class);
        val editableInfo2 = mock(EditableInfo.class);

        given(editableInfo1.isNew()).willReturn(true);
        given(editableInfo1.getName()).willReturn("b");

        given(editableInfo2.isNew()).willReturn(false);
        given(editableInfo2.getName()).willReturn("a");

        val child1 = clusterTreeMenu.createMenuItem(menuTreeTop, editableInfo1);
        val child2 = clusterTreeMenu.createMenuItem(menuTreeTop, editableInfo2);

        clusterTreeMenu.sortChildrenLeavingNewUp(menuTreeTop);

        assertThat(childAt(0), is(child1));
        assertThat(childAt(1), is(child2));
    }

    @Test
    public void shouldSortDifferentClassesSeparately() {
        val parallelInfo1 = mock(ParallelInfo.class);
        val parallelInfo2 = mock(ParallelInfo.class);
        val videoInfo1 = mock(VideoInfo.class);
        val videoInfo2 = mock(VideoInfo.class);

        given(parallelInfo1.isNew()).willReturn(false);
        given(parallelInfo2.isNew()).willReturn(false);
        given(videoInfo1.isNew()).willReturn(false);
        given(videoInfo2.isNew()).willReturn(false);

        given(parallelInfo1.getName()).willReturn("d");
        given(parallelInfo2.getName()).willReturn("a");
        given(videoInfo1.getName()).willReturn("c");
        given(videoInfo2.getName()).willReturn("b");

        val child1 = clusterTreeMenu.createMenuItem(menuTreeTop, videoInfo1);
        val child2 = clusterTreeMenu.createMenuItem(menuTreeTop, videoInfo2);
        val child3 = clusterTreeMenu.createMenuItem(menuTreeTop, parallelInfo1);
        val child4 = clusterTreeMenu.createMenuItem(menuTreeTop, parallelInfo2);

        clusterTreeMenu.sortChildrenLeavingNewUp(menuTreeTop);

        assertThat(childAt(0), is(child2));
        assertThat(childAt(1), is(child1));
        assertThat(childAt(2), is(child4));
        assertThat(childAt(3), is(child3));
    }

    @Test
    public void shouldCallOnSelectWhenSelectionChanges() {
        val child1 = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter);
        clusterTreeMenu.setDisableListeners(false);

        final InfoPresenter[] selected = {null};
        clusterTreeMenu.addListeners((info, disableListeners) -> {
            selected[0] = info;
        });

        clusterTreeMenu.expandAndSelect(new Object[]{child1});

        assertThat(selected[0], is(infoPresenter));
    }

    @Test
    public void shouldCallOnSelectWhenNodeIsReloaded() {
        val child1 = clusterTreeMenu.createMenuItem(menuTreeTop, editableInfo);
        clusterTreeMenu.setDisableListeners(false);

        final InfoPresenter[] selected = {null};
        clusterTreeMenu.addListeners((info, disableListeners) -> {
            selected[0] = info;
        });

        clusterTreeMenu.reloadNode(child1);

        assertThat(selected[0], is(editableInfo));
    }

    private DefaultMutableTreeNode childAt(int index) {
        final JTree menuTree = clusterTreeMenu.getMenuTree();
        return (DefaultMutableTreeNode) menuTree.getModel().getChild(menuTreeTop, index);
    }
}