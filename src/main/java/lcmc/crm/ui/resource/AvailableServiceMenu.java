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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.UpdatableItem;

@Named
public class AvailableServiceMenu {
    private final MenuFactory menuFactory;

    public AvailableServiceMenu(MenuFactory menuFactory) {
        this.menuFactory = menuFactory;
    }

    public List<UpdatableItem> getPulldownMenu(final AvailableServiceInfo availableServiceInfo) {
        final List<UpdatableItem> items = new ArrayList<>();
        final UpdatableItem addServiceMenu =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.AddServiceToCluster"), null, null,
                                   new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                           .enablePredicate(() -> {
                               if (availableServiceInfo.getBrowser()
                                                       .crmStatusFailed()) {
                                   return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                               }
                            return null;
                        })
                        .addAction(text -> {
                            availableServiceInfo.hidePopup();
                            final ServicesInfo si = availableServiceInfo.getBrowser().getServicesInfo();
                            si.addServicePanel(availableServiceInfo.getResourceAgent(), null, /* pos */
                                    true, null, null, Application.RunMode.LIVE);
                        });
        items.add(addServiceMenu);
        return items;
    }
}
