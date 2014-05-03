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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.utilities.MyMenu;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResourceMenuTest {
    private static final String ANY_NAME = "ANY_NAME";

    static {
        Tools.init();
    }
    private Set<VolumeInfo> volumes;
    @Mock
    private ResourceInfo resourceInfoStub;
    @Mock
    private VolumeInfo drbdVolumeOneStub;
    @Mock
    private VolumeInfo drbdVolumeTwoStub;
    @Spy
    private final MyMenu volumeMenuItemSpy =
                 new MyMenu(ANY_NAME,
                            new AccessMode(Application.AccessType.ADMIN, false),
                            new AccessMode(Application.AccessType.OP, false));

    private ResourceMenu sut;

    @Before
    public void setUp() {
        volumes = new HashSet<VolumeInfo>(Arrays.asList(drbdVolumeOneStub,
                                                        drbdVolumeTwoStub
                                                        ));
        when(resourceInfoStub.getDrbdVolumes()).thenReturn(volumes);

        sut = new ResourceMenu(resourceInfoStub);
    }

    @Test
    public void menuShouldHaveItems() {
        final List<UpdatableItem> volumeMenuItems =
                                                new ArrayList<UpdatableItem>();
        volumeMenuItems.add(volumeMenuItemSpy);
        when(drbdVolumeOneStub.createPopup()).thenReturn(volumeMenuItems);
        when(resourceInfoStub.getDrbdVolumes()).thenReturn(volumes);

        final List<UpdatableItem> items = sut.getPulldownMenu();
        for (final UpdatableItem item : items) {
            item.updateAndWait();
        }
        verify(volumeMenuItemSpy).updateAndWait();
        assertEquals(2, items.size());
    }
}
