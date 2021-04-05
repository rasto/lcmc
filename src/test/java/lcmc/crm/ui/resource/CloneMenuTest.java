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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.crm.domain.Service;
import lcmc.host.domain.Host;

@ExtendWith(MockitoExtension.class)
class CloneMenuTest {
    @Mock
    private CloneInfo cloneInfoStub;
    @Mock
    private ClusterBrowser clusterBrowserStub;
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
    @InjectMocks
    private CloneMenu cloneMenu;

    @BeforeEach
    void setUp() {
        when(cloneInfoStub.getBrowser()).thenReturn(clusterBrowserStub);
        when(cloneInfoStub.getService()).thenReturn(serviceStub);
        when(clusterBrowserStub.getClusterHosts()).thenReturn(new Host[]{hostStub});
        when(menuFactoryStub.createMenuItem(any(), any(), any(), any(), any())).thenReturn(menuItemStub);
        when(menuFactoryStub.createMenuItem(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(menuItemStub);
        when(menuFactoryStub.createMenu(any(), any(), any())).thenReturn(menuStub);
        when(menuItemStub.enablePredicate(any())).thenReturn(menuItemStub);
        when(menuItemStub.visiblePredicate(any())).thenReturn(menuItemStub);
        when(menuItemStub.predicate(any())).thenReturn(menuItemStub);
        when(menuItemStub.addAction(any())).thenReturn(menuItemStub);
        when(menuStub.enablePredicate(any())).thenReturn(menuStub);
    }

    @Test
    void menuShouldHaveItems() {
        final List<UpdatableItem> items = cloneMenu.getPulldownMenu(cloneInfoStub);

        verify(menuItemStub, times(3)).predicate(any());
        verify(menuItemStub, times(4)).visiblePredicate(any());
        verify(menuItemStub, times(9)).enablePredicate(any());
        verify(menuItemStub, times(11)).addAction(any());
        verify(menuStub, times(4)).enablePredicate(any());
        verify(menuStub, times(3)).onUpdate(any());
        assertThat(items).hasSize(15);
    }
}
