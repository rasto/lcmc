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

package lcmc.gui.resources.crm;

import java.util.Arrays;
import java.util.List;

import lcmc.common.domain.AccessMode;
import lcmc.host.domain.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.resources.Info;

import static org.junit.Assert.assertEquals;

import lcmc.utilities.EnablePredicate;
import lcmc.utilities.MenuAction;
import lcmc.utilities.MenuFactory;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Predicate;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.VisiblePredicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;

import javax.swing.ImageIcon;

@RunWith(MockitoJUnitRunner.class)
public class PcmkMultiSelectionMenuTest {
    @Mock
    private PcmkMultiSelectionInfo pcmkMultiSelectionInfoStub;
    @Mock
    private ClusterBrowser clusterBrowserStub;
    @Mock
    private ServiceInfo serviceInfoStub;
    @Mock
    private HostInfo hostInfoStub;
    @Mock
    private Host hostStub;
    @Mock
    private MyMenuItem menuItemStub;
    @Mock
    private MenuFactory menuFactoryStub;
    @InjectMocks
    private PcmkMultiSelectionMenu pcmkMultiSelectionMenu;

    @Before
    public void setUp() {
        final List<Info> selectedInfos = Arrays.asList(serviceInfoStub, hostInfoStub);
        when(pcmkMultiSelectionInfoStub.getBrowser()).thenReturn(clusterBrowserStub);
        when(pcmkMultiSelectionInfoStub.getSelectedInfos()).thenReturn(selectedInfos);
        when(clusterBrowserStub.getClusterHosts()).thenReturn(new Host[]{hostStub});
        when(menuFactoryStub.createMenuItem(anyString(),
                (ImageIcon) anyObject(),
                anyString(),
                (AccessMode) anyObject(),
                (AccessMode) anyObject())).thenReturn(menuItemStub);
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
        when(menuItemStub.addAction((MenuAction) anyObject())).thenReturn(menuItemStub);
        when(menuItemStub.visiblePredicate((VisiblePredicate) anyObject())).thenReturn(menuItemStub);
    }

    @Test
    public void menuShouldHaveItems() {
        final List<UpdatableItem> items = pcmkMultiSelectionMenu.getPulldownMenu(pcmkMultiSelectionInfoStub);

        verify(menuItemStub, times(3)).predicate((Predicate) anyObject());
        verify(menuItemStub, times(12)).visiblePredicate((VisiblePredicate) anyObject());
        verify(menuItemStub, times(10)).enablePredicate((EnablePredicate) anyObject());
        verify(menuItemStub, times(17)).addAction((MenuAction) anyObject());
        assertEquals(17, items.size());
    }
}
