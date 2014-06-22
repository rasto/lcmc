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
import java.util.Collection;
import java.util.List;
import javax.swing.JMenuItem;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.utilities.MyMenu;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class ResourceMenu {
    
    private final ResourceInfo resourceInfo;

    public ResourceMenu(final ResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        for (final VolumeInfo dvi : resourceInfo.getDrbdVolumes()) {
            final UpdatableItem volumesMenu = new MyMenu(
                            dvi.toString(),
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void updateAndWait() {
                    Tools.isSwingThread();
                    removeAll();
                    final Collection<UpdatableItem> volumeMenus =
                                    new ArrayList<UpdatableItem>();
                    for (final UpdatableItem u : dvi.createPopup()) {
                        volumeMenus.add(u);
                    }
                    for (final UpdatableItem u : volumeMenus) {
                        add((JMenuItem) u);
                    }
                    super.updateAndWait();
                }
            };
            items.add(volumesMenu);
        }
        return items;
    }
}
