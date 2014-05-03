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

import java.util.List;
import lcmc.data.Application;
import lcmc.data.DrbdXML;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.DrbdGraph;
import lcmc.testutils.annotation.type.GuiTest;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VolumeMenuTest {
    static {
        Tools.init();
    }

    @Mock
    private VolumeInfo volumeInfoStub;
    @Mock
    private ResourceInfo resourceInfoStub;
    @Mock
    private ClusterBrowser clusterBrowserStub;
    @Mock
    private DrbdXML drbdXmlStub;
    @Mock
    private DrbdGraph drbdGraphStub;
    @Mock
    private BlockDevInfo sourceStub;
    @Mock
    private BlockDevInfo destStub;

    private VolumeMenu sut;

    @Before
    public void setUp() {
        when(volumeInfoStub.getDrbdResourceInfo()).thenReturn(resourceInfoStub);
        when(volumeInfoStub.getBrowser()).thenReturn(clusterBrowserStub);
        when(volumeInfoStub.isConnected(Application.RunMode.LIVE)).thenReturn(true);
        when(resourceInfoStub.getBrowser()).thenReturn(clusterBrowserStub);
        when(clusterBrowserStub.getDrbdXML()).thenReturn(drbdXmlStub);
        when(clusterBrowserStub.getDrbdGraph()).thenReturn(drbdGraphStub);
        when(drbdGraphStub.getSource(volumeInfoStub)).thenReturn(sourceStub);
        when(drbdGraphStub.getDest(volumeInfoStub)).thenReturn(destStub);
        sut = new VolumeMenu(volumeInfoStub);
    }

    @Test
    @Category(GuiTest.class)
    public void menuShouldHaveItems() {
        final List<UpdatableItem> items = sut.getPulldownMenu();
        
        for (final UpdatableItem item : items) {
            ((MyMenuItem) item).action();
        }

        assertEquals(6, items.size());
    }

}
