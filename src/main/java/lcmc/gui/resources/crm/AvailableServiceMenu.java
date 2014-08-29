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
import lcmc.utilities.EnablePredicate;
import lcmc.utilities.MenuAction;
import lcmc.utilities.MenuFactory;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AvailableServiceMenu {
    @Autowired
    private MenuFactory menuFactory;

    public List<UpdatableItem> getPulldownMenu(final AvailableServiceInfo availableServiceInfo) {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final UpdatableItem addServiceMenu = menuFactory.createMenuItem(
                        Tools.getString("ClusterBrowser.AddServiceToCluster"),
                        null,
                        null,
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.OP, false))
            .enablePredicate(new EnablePredicate() {
                        @Override
                        public String check() {
                            if (availableServiceInfo.getBrowser().crmStatusFailed()) {
                                return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                            }
                            return null;
                        }})
            .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        availableServiceInfo.hidePopup();
                        final ServicesInfo si = availableServiceInfo.getBrowser().getServicesInfo();
                        si.addServicePanel(availableServiceInfo.getResourceAgent(),
                                           null, /* pos */
                                           true,
                                           null,
                                           null,
                                           Application.RunMode.LIVE);
                    }});
        items.add(addServiceMenu);
        return items;
    }
}
