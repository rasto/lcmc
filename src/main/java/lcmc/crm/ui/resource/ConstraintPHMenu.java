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

import javax.inject.Inject;
import javax.inject.Named;

import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.ui.utils.UpdatableItem;

@Named
public class ConstraintPHMenu extends ServiceMenu {

    private ConstraintPHInfo constraintPHInfo;
    @Inject
    private MenuFactory menuFactory;

    @Override
    public List<UpdatableItem> getPulldownMenu(final ServiceInfo serviceInfo) {
        constraintPHInfo = (ConstraintPHInfo) serviceInfo;
        final List<UpdatableItem> items = new ArrayList<>();
        final Application.RunMode runMode = Application.RunMode.LIVE;
        addDependencyMenuItems(constraintPHInfo, items, true, runMode);
        /* remove the placeholder and all constraints associated with it. */
        final MyMenuItem removeMenuItem =
                menuFactory.createMenuItem(Tools.getString("ConstraintPHInfo.Remove"), ClusterBrowser.REMOVE_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP, new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL)).enablePredicate(() -> {
                    if (constraintPHInfo.getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    } else if (constraintPHInfo.getService().isRemoved()) {
                        return ConstraintPHInfo.IS_BEING_REMOVED_STRING;
                    }
                    return null;
                }).addAction(text -> {
                    constraintPHInfo.hidePopup();
                    constraintPHInfo.removeMyself(Application.RunMode.LIVE);
                    constraintPHInfo.getBrowser().getCrmGraph().repaint();
                });
        final ButtonCallback removeItemCallback = constraintPHInfo.getBrowser().new ClMenuItemCallback(null) {
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && !constraintPHInfo.getService().isNew();
            }
        }.addAction(dcHost -> constraintPHInfo.removeMyselfNoConfirm(dcHost, Application.RunMode.TEST));
        constraintPHInfo.addMouseOverListener(removeMenuItem, removeItemCallback);
        items.add(removeMenuItem);
        return items;
    }
}
