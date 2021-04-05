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

package lcmc.drbd.ui.resource;

import java.util.List;

import lcmc.common.domain.AccessMode;
import lcmc.cluster.ui.ClusterBrowser;

import static org.junit.Assert.assertEquals;

import lcmc.common.domain.EnablePredicate;
import lcmc.common.ui.utils.MenuAction;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.domain.Predicate;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.common.domain.VisiblePredicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.ImageIcon;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class VolumeMenuITest {
    @Mock
    private VolumeInfo volumeInfoStub;
    @Mock
    private ResourceInfo resourceInfoStub;
    @Mock
    private ClusterBrowser clusterBrowserStub;
    @Mock
    private MyMenuItem menuItemStub;
    @Mock
    private MenuFactory menuFactoryStub;
    @InjectMocks
    private VolumeMenu volumeMenu;

    @Before
    public void setUp() {
        when(volumeInfoStub.getDrbdResourceInfo()).thenReturn(resourceInfoStub);
        when(volumeInfoStub.getBrowser()).thenReturn(clusterBrowserStub);
        when(menuFactoryStub.createMenuItem(any(),
                (ImageIcon) anyObject(),
                any(),

                any(),
                (ImageIcon) anyObject(),
                any(),

                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuItemStub);
        when(menuFactoryStub.createMenuItem(
                any(),
                (ImageIcon) anyObject(),
                any(),
                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.enablePredicate((EnablePredicate) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.predicate((Predicate) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.addAction((MenuAction) anyObject())).thenReturn(menuItemStub);
    }

    @Test
    public void menuShouldHaveItems() {
        final List<UpdatableItem> items = volumeMenu.getPulldownMenu(volumeInfoStub);

        verify(menuItemStub, times(2)).predicate((Predicate) anyObject());
        verify(menuItemStub, never()).visiblePredicate((VisiblePredicate) anyObject());
        verify(menuItemStub, times(5)).enablePredicate((EnablePredicate) anyObject());
        verify(menuItemStub, times(6)).addAction((MenuAction) anyObject());
        assertEquals(6, items.size());
    }
}
