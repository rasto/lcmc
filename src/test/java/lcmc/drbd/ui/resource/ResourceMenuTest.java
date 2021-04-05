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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.UpdatableItem;

@ExtendWith(MockitoExtension.class)
class ResourceMenuTest {
   private Set<VolumeInfo> volumes;
   @Mock
   private ResourceInfo resourceInfoStub;
   @Mock
   private VolumeInfo drbdVolumeOneStub;
   @Mock
   private VolumeInfo drbdVolumeTwoStub;
   @Mock
   private MyMenu menuStub;
   @Mock
   private MenuFactory menuFactoryStub;
   @InjectMocks
   private ResourceMenu resourceMenu;

   @BeforeEach
   void setUp() {
      when(menuFactoryStub.createMenu(anyString(), any(), any())).thenReturn(menuStub);
      volumes = new HashSet<>(Arrays.asList(drbdVolumeOneStub, drbdVolumeTwoStub));
      when(resourceInfoStub.getDrbdVolumes()).thenReturn(volumes);
   }

   @Test
   void menuShouldHaveItems() {
      final List<UpdatableItem> items = resourceMenu.getPulldownMenu(resourceInfoStub);

      verify(menuStub, never()).enablePredicate(any());
      verify(menuStub, times(2)).onUpdate(any());
      assertThat(items).hasSize(2);
   }
}
