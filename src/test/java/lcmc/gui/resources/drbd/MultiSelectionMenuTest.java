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

package lcmc.gui.resources.drbd;

import java.util.Arrays;
import java.util.List;

import lcmc.data.Host;
import lcmc.data.resources.BlockDevice;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.resources.Info;
import lcmc.testutils.annotation.type.GuiTest;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MultiSelectionMenuTest {
    static {
        Tools.init();
    }

    @Mock
    private MultiSelectionInfo multiSelectionInfoStub;
    @Mock
    private Host hostStub;
    @Mock
    private ClusterBrowser browserStub;
    @Mock
    private BlockDevice blockDeviceStub;

    // can't use @Mock annotation, for these two, because the instanceof
    // wouldn't work in the SUT
    private final BlockDevInfo blockDevInfoStub = mock(BlockDevInfo.class);
    private final HostDrbdInfo hostDrbdInfoStub = mock(HostDrbdInfo.class);
    
    private final List<Info> selectedInfos = Arrays.asList(blockDevInfoStub,
                                                     hostDrbdInfoStub);
    private MultiSelectionMenu sut;

    @Before
    public void setUp() {
        when(multiSelectionInfoStub.getBrowser()).thenReturn(browserStub);
        when(hostDrbdInfoStub.getHost()).thenReturn(hostStub);

        when(blockDevInfoStub.getBlockDevice()).thenReturn(blockDeviceStub);

        sut = new MultiSelectionMenu(multiSelectionInfoStub, selectedInfos);
    }

    @Test
    @Category(GuiTest.class)
    public void menuShouldHaveItems() {
        final List<UpdatableItem> items = sut.getPulldownMenu();

        assertEquals(26, items.size());
    }
}
