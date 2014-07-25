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

import java.util.ArrayList;
import java.util.List;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.gui.ClusterBrowser;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class AvailableServiceMenu {
    private final AvailableServiceInfo availableServiceInfo;
    
    public AvailableServiceMenu(final AvailableServiceInfo availableServiceInfo) {
        this.availableServiceInfo = availableServiceInfo;
    }

    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final UpdatableItem addServiceMenu = new MyMenuItem(
                        Tools.getString("ClusterBrowser.AddServiceToCluster"),
                        null,
                        null,
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (availableServiceInfo.getBrowser().crmStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                availableServiceInfo.hidePopup();
                final ServicesInfo si = availableServiceInfo.getBrowser().getServicesInfo();
                si.addServicePanel(availableServiceInfo.getResourceAgent(),
                                   null, /* pos */
                                   true,
                                   null,
                                   null,
                                   Application.RunMode.LIVE);
            }
        };
        items.add(addServiceMenu);
        return items;
    }
}
