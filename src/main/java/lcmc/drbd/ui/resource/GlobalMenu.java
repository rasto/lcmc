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
import java.util.List;
import java.util.function.Supplier;

import lcmc.cluster.ui.wizard.EditClusterDialog;
import lcmc.drbd.ui.ProxyHostWizard;
import lcmc.common.domain.AccessMode;
import lcmc.host.domain.Host;
import lcmc.cluster.ui.wizard.DrbdLogs;
import lcmc.host.domain.HostFactory;
import lcmc.common.ui.utils.MenuAction;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.UpdatableItem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GlobalMenu {
    private final Supplier<EditClusterDialog> editClusterDialogProvider;
    private final HostFactory hostFactory;
    private final Supplier<ProxyHostWizard> proxyHostWizardProvider;
    private final MenuFactory menuFactory;
    private final Supplier<DrbdLogs> drbdLogsProvider;

    public List<UpdatableItem> getPulldownMenu(final GlobalInfo globalInfo) {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

        /** Add proxy host */
        final UpdatableItem addProxyHostMenu = menuFactory.createMenuItem(
                Tools.getString("GlobalInfo.AddProxyHost"),
                null,
                Tools.getString("GlobalInfo.AddProxyHost"),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        addProxyHostWizard(globalInfo);
                    }
                });
        items.add(addProxyHostMenu);

        /* cluster wizard */
        final UpdatableItem clusterWizardItem = menuFactory.createMenuItem(
                Tools.getString("ClusterBrowser.Hb.ClusterWizard"),
                GlobalInfo.CLUSTER_ICON,
                null,
                new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED),
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL))
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        final EditClusterDialog editClusterDialog = editClusterDialogProvider.get();
                        editClusterDialog.showDialogs(globalInfo.getBrowser().getCluster());
                    }
                });
        items.add(clusterWizardItem);

        /* Rescan LVM */
        final UpdatableItem rescanLvmItem = menuFactory.createMenuItem(
                Tools.getString("GlobalInfo.RescanLvm"),
                null, /* icon */
                null,
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.ADVANCED))
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        globalInfo.getBrowser().updateHWInfo(Host.UPDATE_LVM);
                    }
                });
        items.add(rescanLvmItem);

        /* export image  */
        final UpdatableItem savePNGItem =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.ExportGraph"),
                        null,
                        null,
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL)).addAction(new MenuAction() {
                    @Override
                    public void run(String text) {
                        globalInfo.exportGraphAsPng();
                    }
                });
        items.add(savePNGItem);

        /* view log */
        final UpdatableItem viewLogMenu = menuFactory.createMenuItem(
                Tools.getString("ClusterBrowser.Drbd.ViewLogs"),
                GlobalInfo.LOGFILE_ICON,
                null,
                new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                new AccessMode(AccessMode.RO, AccessMode.NORMAL))
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        globalInfo.hidePopup();
                        final DrbdLogs drbdLogs = drbdLogsProvider.get();
                        drbdLogs.init(globalInfo.getCluster(), GlobalInfo.ALL_LOGS_PATTERN);
                        drbdLogs.showDialog();
                    }
                });
        items.add(viewLogMenu);
        return items;
    }

    private void addProxyHostWizard(final GlobalInfo globalInfo) {
        final Host proxyHost = hostFactory.createInstance();
        proxyHost.setCluster(globalInfo.getCluster());
        final ProxyHostWizard proxyHostWizard = proxyHostWizardProvider.get();
        proxyHostWizard.init(proxyHost, null);
        proxyHostWizard.showDialogs();
    }
}
