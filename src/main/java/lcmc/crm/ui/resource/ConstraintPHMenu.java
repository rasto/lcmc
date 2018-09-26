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
import java.util.function.Supplier;

import lcmc.common.ui.Access;
import lcmc.common.ui.CallbackAction;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.ui.EditConfig;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.treemenu.ClusterTreeMenu;
import lcmc.common.ui.utils.*;
import lcmc.crm.ui.ServiceLogs;
import lcmc.host.domain.Host;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.EnablePredicate;
import lcmc.common.domain.util.Tools;

public class ConstraintPHMenu extends ServiceMenu {
    private final MenuFactory menuFactory;

    private ConstraintPHInfo constraintPHInfo;

    public ConstraintPHMenu(MainData drbdGui, EditConfig editDialog, MenuFactory menuFactory, Application application, SwingUtils swingUtils, Supplier<ServiceLogs> serviceLogsProvider, ClusterTreeMenu clusterTreeMenu, Access access, GroupMenu groupMenu) {
        super(drbdGui, editDialog, menuFactory, application, swingUtils, serviceLogsProvider, clusterTreeMenu, access, groupMenu);
        this.menuFactory = menuFactory;
    }

    @Override
    public List<UpdatableItem> getPulldownMenu(final ServiceInfo serviceInfo) {
        this.constraintPHInfo = (ConstraintPHInfo) serviceInfo;
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final Application.RunMode runMode = Application.RunMode.LIVE;
        addDependencyMenuItems(constraintPHInfo, items, true, runMode);
        /* remove the placeholder and all constraints associated with it. */
        final MyMenuItem removeMenuItem = menuFactory.createMenuItem(
                Tools.getString("ConstraintPHInfo.Remove"),
                ClusterBrowser.REMOVE_ICON,
                ClusterBrowser.STARTING_PTEST_TOOLTIP,
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (constraintPHInfo.getBrowser().crmStatusFailed()) {
                            return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                        } else if (constraintPHInfo.getService().isRemoved()) {
                            return ConstraintPHInfo.IS_BEING_REMOVED_STRING;
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        constraintPHInfo.hidePopup();
                        constraintPHInfo.removeMyself(Application.RunMode.LIVE);
                        constraintPHInfo.getBrowser().getCrmGraph().repaint();
                    }
                });
        final ButtonCallback removeItemCallback = constraintPHInfo.getBrowser().new ClMenuItemCallback(null) {
            @Override
            public boolean isEnabled() {
                return super.isEnabled() && !constraintPHInfo.getService().isNew();
            }
        }
                .addAction(new CallbackAction() {
                    @Override
                    public void run(final Host dcHost) {
                        constraintPHInfo.removeMyselfNoConfirm(dcHost, Application.RunMode.TEST);
                    }
                });
        constraintPHInfo.addMouseOverListener(removeMenuItem, removeItemCallback);
        items.add(removeMenuItem);
        return items;
    }
}
