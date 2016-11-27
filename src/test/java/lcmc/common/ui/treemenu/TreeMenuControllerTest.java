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
import lcmc.common.ui.utils.SwingUtils;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TreeMenuControllerTest {
    private SwingUtils swingUtils;
    @Mock
    private CategoryInfo categoryInfo;
    @Mock
    private InfoPresenter infoPresenter;
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
        clusterTreeMenu.reloadNode(menuTreeTop, true);

        assertThat(menuTree.getRowCount(), is(1));
        assertThat(clusterTreeMenu.getChildCount(menuTreeTop), is(1));
    }

    @Test
    public void shouldMakePopupOnMousePress() {
        val menuItem = clusterTreeMenu.createMenuItem(menuTreeTop, infoPresenter);

        final JTree menuTree = clusterTreeMenu.getMenuTree();
        clusterTreeMenu.reloadNode(menuTreeTop, true);
        final int x = 100;
        final int y = 100;
        val mousePress = new MouseEvent(menuTree, 0, 0, 0, x, y, 1, true, 2);
        treePath = new TreePath(new Object[]{menuTreeTop, menuItem});

        Arrays.stream(menuTree.getMouseListeners()).forEach(listener -> {
            listener.mousePressed(mousePress);
        });

        verify(infoPresenter).showPopup(menuTree, x, y);
    }
}