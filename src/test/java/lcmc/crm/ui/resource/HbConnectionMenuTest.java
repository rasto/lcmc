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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.ui.utils.UpdatableItem;

@ExtendWith(MockitoExtension.class)
class HbConnectionMenuTest {
    @Mock
    private HbConnectionInfo hbConnectionInfoStub;
    @Mock
    private ClusterBrowser clusterBrowserStub;
    @Mock
    private MyMenuItem menuItemStub;
    @Mock
    private MenuFactory menuFactoryStub;
    @InjectMocks
    private HbConnectionMenu hbConnectionMenu;

    @BeforeEach
    void setUp() {
        when(hbConnectionInfoStub.getBrowser()).thenReturn(clusterBrowserStub);
        when(menuFactoryStub.createMenuItem(anyString(), any(), anyString(), any(), any())).thenReturn(menuItemStub);
        when(menuFactoryStub.createMenuItem(anyString(), any(), anyString(), anyString(), any(), anyString(), any(),
                any())).thenReturn(menuItemStub);
        when(menuItemStub.enablePredicate(any())).thenReturn(menuItemStub);
        when(menuItemStub.addAction(any())).thenReturn(menuItemStub);
        when(menuItemStub.predicate(any())).thenReturn(menuItemStub);
    }

    @Test
    void menuShouldHaveItems() {
        final List<UpdatableItem> items = hbConnectionMenu.getPulldownMenu(hbConnectionInfoStub);
        verify(menuItemStub, times(2)).predicate(any());
        verify(menuItemStub, never()).visiblePredicate(any());
        verify(menuItemStub, times(3)).enablePredicate(any());
        verify(menuItemStub, times(3)).addAction(any());

        assertThat(items).hasSize(3);
    }
}
