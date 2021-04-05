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

package lcmc.crm.ui.resource;

import java.util.List;

import lcmc.common.domain.AccessMode;
import lcmc.host.domain.Host;
import lcmc.common.domain.ResourceValue;
import lcmc.crm.domain.Service;
import lcmc.cluster.ui.ClusterBrowser;

import static org.junit.Assert.assertEquals;

import lcmc.common.domain.EnablePredicate;
import lcmc.common.ui.utils.MenuAction;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.domain.Predicate;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.common.domain.VisiblePredicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.ImageIcon;

@RunWith(MockitoJUnitRunner.class)
public class CloneMenuTest {
    @Mock
    private CloneInfo cloneInfoStub;
    @Mock
    private ClusterBrowser clusterBrowserStub;
    @Mock
    private ResourceValue resourceStub;
    @Mock
    private Service serviceStub;
    @Mock
    private Host hostStub;
    @Mock
    private MyMenuItem menuItemStub;
    @Mock
    private MyMenu menuStub;
    @Mock
    private MenuFactory menuFactoryStub;
    @Mock
    private ServiceMenu serviceMenu;
    @InjectMocks
    private CloneMenu cloneMenu;

    @Before
    public void setUp() {
        when(cloneInfoStub.getBrowser()).thenReturn(clusterBrowserStub);
        when(cloneInfoStub.getService()).thenReturn(serviceStub);
        when(clusterBrowserStub.getClusterHosts()).thenReturn(new Host[]{hostStub});
        when(menuFactoryStub.createMenuItem(
                any(),
                (ImageIcon) anyObject(),
                any(),
                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuItemStub);
        when(menuFactoryStub.createMenuItem(
                any(),
                (ImageIcon) anyObject(),
                any(),

                any(),
                (ImageIcon) anyObject(),
                any(),

                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuItemStub);
        when(menuFactoryStub.createMenu(
                any(),
                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuStub);
        when(menuItemStub.enablePredicate((EnablePredicate) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.visiblePredicate((VisiblePredicate) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.predicate((Predicate) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.addAction((MenuAction) anyObject())).thenReturn(menuItemStub);
        when(menuStub.enablePredicate((EnablePredicate) anyObject())).thenReturn(menuStub);
    }

    @Test
    public void menuShouldHaveItems() {
        final List<UpdatableItem> items = cloneMenu.getPulldownMenu(cloneInfoStub);

        verify(menuItemStub, times(3)).predicate((Predicate) anyObject());
        verify(menuItemStub, times(4)).visiblePredicate((VisiblePredicate) anyObject());
        verify(menuItemStub, times(9)).enablePredicate((EnablePredicate) anyObject());
        verify(menuItemStub, times(11)).addAction((MenuAction) anyObject());
        verify(menuStub, times(4)).enablePredicate((EnablePredicate) anyObject());
        verify(menuStub, times(3)).onUpdate((Runnable) anyObject());
        assertEquals(15, items.size());
    }
}
