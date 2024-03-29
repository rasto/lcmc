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

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.JMenuItem;

import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Access;
import lcmc.common.ui.EditConfig;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.treemenu.ClusterTreeMenu;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.crm.ui.ServiceLogs;
import lcmc.host.domain.Host;

@Named
public class CloneMenu extends ServiceMenu {
    private CloneInfo cloneInfo;
    private final MenuFactory menuFactory;
    private final SwingUtils swingUtils;

    public CloneMenu(MainData drbdGui, EditConfig editDialog, MenuFactory menuFactory, SwingUtils swingUtils,
            Provider<ServiceLogs> serviceLogsProvider, ClusterTreeMenu clusterTreeMenu, Access access) {
        super(drbdGui, editDialog, menuFactory, swingUtils, serviceLogsProvider, clusterTreeMenu, access);
        this.menuFactory = menuFactory;
        this.swingUtils = swingUtils;
    }

    @Override
    public List<UpdatableItem> getPulldownMenu(final ServiceInfo serviceInfo) {
        cloneInfo = (CloneInfo) serviceInfo;
        final List<UpdatableItem> items = super.getPulldownMenu(serviceInfo);
        final ServiceInfo cs = cloneInfo.getContainedService();
        if (cs == null) {
            return items;
        }
        final MyMenu csMenu = menuFactory.createMenu(cs.toString(), new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                new AccessMode(AccessMode.RO, AccessMode.NORMAL));
        csMenu.onUpdate(() -> {
            swingUtils.isSwingThread();
            csMenu.removeAll();
            final ServiceInfo cs0 = cloneInfo.getContainedService();
            if (cs0 != null) {
                for (final UpdatableItem u : cs0.createPopup()) {
                    csMenu.add((JMenuItem) u);
                    u.updateAndWait();
                }
            }
            csMenu.updateMenuComponents();
            csMenu.processAccessMode();
        });
        items.add(csMenu);
        return items;
    }

    @Override
    protected void addMigrateMenuItems(final ServiceInfo serviceInfo, final List<UpdatableItem> items) {
        super.addMigrateMenuItems(cloneInfo, items);
        if (!cloneInfo.getService().isMaster()) {
            return;
        }
        final Application.RunMode runMode = Application.RunMode.LIVE;
        for (final Host host : cloneInfo.getBrowser().getClusterHosts()) {
            final String hostName = host.getName();
            final MyMenuItem migrateFromMenuItem = menuFactory.createMenuItem(
                            Tools.getString("ClusterBrowser.Hb.MigrateFromResource") + ' ' + hostName + " (stop)", ServiceInfo.MIGRATE_ICON,
                            ClusterBrowser.STARTING_PTEST_TOOLTIP,

                            Tools.getString("ClusterBrowser.Hb.MigrateFromResource") + ' ' + hostName + " (stop) (offline)",
                            ServiceInfo.MIGRATE_ICON, ClusterBrowser.STARTING_PTEST_TOOLTIP,
                            new AccessMode(AccessMode.OP, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                    .predicate(host::isCrmStatusOk)
                    .visiblePredicate(() -> {
                        if (!host.isCrmStatusOk()) {
                            return false;
                        }
                        final List<String> runningOnNodes = cloneInfo.getRunningOnNodes(runMode);
                        if (runningOnNodes == null || runningOnNodes.isEmpty()) {
                            return false;
                        }
                        boolean runningOnNode = false;
                        for (final String ron : runningOnNodes) {
                            if (hostName.equalsIgnoreCase(ron)) {
                                runningOnNode = true;
                                break;
                            }
                        }
                        return !cloneInfo.getBrowser().crmStatusFailed() && cloneInfo.getService().isAvailable() && runningOnNode
                               && host.isCrmStatusOk();
                    })
                    .addAction(text -> {
                        cloneInfo.hidePopup();
                        if (cloneInfo.getService().isMaster()) {
                            /* without role=master */
                            cloneInfo.superMigrateFromResource(cloneInfo.getBrowser().getDCHost(), hostName, runMode);
                        } else {
                            cloneInfo.migrateFromResource(cloneInfo.getBrowser().getDCHost(), hostName, runMode);
                        }
                    });
            final ButtonCallback migrateItemCallback = cloneInfo.getBrowser().new ClMenuItemCallback(null).addAction(host1 -> {
                if (cloneInfo.getService().isMaster()) {
                    /* without role=master */
                    cloneInfo.superMigrateFromResource(host1, hostName, Application.RunMode.TEST);
                } else {
                    cloneInfo.migrateFromResource(host1, hostName, Application.RunMode.TEST);
                }
            });
            cloneInfo.addMouseOverListener(migrateFromMenuItem, migrateItemCallback);
            items.add(migrateFromMenuItem);
        }
    }

    /**
     * Adds "migrate from" and "force migrate" menuitems to the submenu.
     */
    @Override
    protected void addMoreMigrateMenuItems(final ServiceInfo serviceInfo, final MyMenu submenu) {
        /* no migrate / unmigrate menu advanced items for clones. */
    }
}
