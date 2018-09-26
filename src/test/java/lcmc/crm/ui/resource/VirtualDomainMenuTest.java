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
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
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
import org.mockito.Mock;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.ImageIcon;

@RunWith(MockitoJUnitRunner.class)
public class VirtualDomainMenuTest {
    @Mock
    private VirtualDomainInfo virtualDomainInfoStub;
    @Mock
    private ClusterBrowser clusterBrowserStub;
    @Mock
    private Application applicationStub;
    @Mock
    private Host hostStub;
    @Mock
    private MyMenu menuStub;
    @Mock
    private MyMenuItem menuItemStub;
    @Mock
    private MenuFactory menuFactoryStub;
    private VirtualDomainMenu virtualDomainMenu;

    @Before
    public void setUp() {
        virtualDomainMenu = new VirtualDomainMenu(
                null,
                null,
                menuFactoryStub,
                null,
                null,
                null,
                null,
                null);
        when(virtualDomainInfoStub.getBrowser()).thenReturn(clusterBrowserStub);
        when(applicationStub.isUseTightvnc()).thenReturn(true);
        when(applicationStub.isUseRealvnc()).thenReturn(true);
        when(applicationStub.isUseUltravnc()).thenReturn(true);
        final Host[] clusterHosts = {hostStub};
        when(clusterBrowserStub.getClusterHosts()).thenReturn(clusterHosts);

        when(menuFactoryStub.createMenuItem(
                anyString(),
                (ImageIcon) anyObject(),
                anyString(),
                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuItemStub);
        when(menuFactoryStub.createMenu(
                anyString(),
                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuStub);
        when(menuFactoryStub.createMenuItem(anyString(),
                (ImageIcon) anyObject(),
                anyString(),

                anyString(),
                (ImageIcon) anyObject(),
                anyString(),

                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.enablePredicate((EnablePredicate) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.predicate((Predicate) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.visiblePredicate((VisiblePredicate) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.addAction((MenuAction) anyObject())).thenReturn(menuItemStub);
        when(menuStub.enablePredicate((EnablePredicate) anyObject())).thenReturn(menuStub);
    }

    @Test
    public void menuShouldHaveItems() {
        final List<UpdatableItem> items = virtualDomainMenu.getPulldownMenu(virtualDomainInfoStub);

        verify(menuItemStub, times(5)).predicate((Predicate) anyObject());
        verify(menuItemStub, times(6)).visiblePredicate((VisiblePredicate) anyObject());
        verify(menuItemStub, times(12)).enablePredicate((EnablePredicate) anyObject());
        verify(menuItemStub, times(16)).addAction((MenuAction) anyObject());
        verify(menuStub, times(4)).enablePredicate((EnablePredicate) anyObject());
        verify(menuStub, times(3)).onUpdate((Runnable) anyObject());
        assertEquals(18, items.size());
    }
}
