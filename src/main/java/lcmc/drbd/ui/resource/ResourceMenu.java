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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JMenuItem;

import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.UpdatableItem;

@Named
public class ResourceMenu {
    private ResourceInfo resourceInfo;
    @Inject
    private MenuFactory menuFactory;
    @Inject
    private Application application;

    public List<UpdatableItem> getPulldownMenu(final ResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        for (final VolumeInfo dvi : resourceInfo.getDrbdVolumes()) {
            final MyMenu volumesMenu = menuFactory.createMenu(
                    dvi.toString(),
                    new AccessMode(Application.AccessType.RO, false),
                    new AccessMode(Application.AccessType.RO, false));
            volumesMenu.onUpdate(new Runnable() {
                @Override
                public void run() {
                    application.isSwingThread();
                    volumesMenu.removeAll();
                    final Collection<UpdatableItem> volumeMenus = new ArrayList<UpdatableItem>();
                    for (final UpdatableItem u : dvi.createPopup()) {
                        volumeMenus.add(u);
                    }
                    for (final UpdatableItem u : volumeMenus) {
                        volumesMenu.add((JMenuItem) u);
                    }
                    volumesMenu.updateMenuComponents();
                    volumesMenu.processAccessMode();
                }
            });
            items.add(volumesMenu);
        }
        return items;
    }
}
