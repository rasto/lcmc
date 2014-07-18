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
import lcmc.data.Host;
import lcmc.data.drbd.DrbdInstallation;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.HostBrowser;
import lcmc.testutils.annotation.type.GuiTest;
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

@Category(GuiTest.class)
@RunWith(MockitoJUnitRunner.class)
public class ProxyHostMenuITest {
    static {
        Tools.init();
    }

    private ProxyHostMenu proxyHostMenu;

    @Mock
    private ProxyHostInfo proxyHostInfoStub;
    @Mock
    private HostBrowser hostBrowserStub;
    @Mock
    private Host hostStub;
    @Mock
    private DrbdInstallation drbdInstallationStub;
    @Mock
    private ClusterBrowser clusterBrowserStub;

    @Before
    public void setUp() {
        when(proxyHostInfoStub.getBrowser()).thenReturn(hostBrowserStub);
        when(proxyHostInfoStub.getHost()).thenReturn(hostStub);
        when(hostBrowserStub.getClusterBrowser()).thenReturn(clusterBrowserStub);
        proxyHostMenu = new ProxyHostMenu(proxyHostInfoStub);
    }

    @Test
    public void menuShouldHaveItems() {
        final List<UpdatableItem> items = proxyHostMenu.getPulldownMenu();

        assertEquals(7, items.size());
    }
}
