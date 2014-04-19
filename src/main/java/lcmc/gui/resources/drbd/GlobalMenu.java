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
import java.util.List;
import lcmc.EditClusterDialog;
import lcmc.ProxyHostWizard;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.Host;
import lcmc.gui.dialog.cluster.DrbdLogs;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class GlobalMenu {
    
    private final GlobalInfo globalInfo;

    public GlobalMenu(final GlobalInfo globalInfo) {
        this.globalInfo = globalInfo;
    }

    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

        /** Add proxy host */
        final UpdatableItem addProxyHostMenu = new MyMenuItem(
                Tools.getString("GlobalInfo.AddProxyHost"), null,
                Tools.getString("GlobalInfo.AddProxyHost"), new AccessMode(
                        Application.AccessType.OP, false), new AccessMode(
                        Application.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override
            public void action() {
                addProxyHostWizard();
            }
        };
        items.add(addProxyHostMenu);

        /* cluster wizard */
        final UpdatableItem clusterWizardItem = new MyMenuItem(
                Tools.getString("ClusterBrowser.Hb.ClusterWizard"),
                GlobalInfo.CLUSTER_ICON, null, new AccessMode(
                        Application.AccessType.ADMIN, AccessMode.ADVANCED),
                new AccessMode(Application.AccessType.ADMIN,
                        !AccessMode.ADVANCED)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void action() {
                final EditClusterDialog dialog = new EditClusterDialog(
                        globalInfo.getBrowser().getCluster());
                dialog.showDialogs();
            }
        };
        items.add(clusterWizardItem);

        /* Rescan LVM */
        final UpdatableItem rescanLvmItem = new MyMenuItem(
                Tools.getString("GlobalInfo.RescanLvm"), null, /* icon */
                null, new AccessMode(Application.AccessType.OP,
                        !AccessMode.ADVANCED), new AccessMode(
                        Application.AccessType.OP, AccessMode.ADVANCED)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void action() {
                globalInfo.getBrowser().updateHWInfo(
                        Host.UPDATE_LVM);
            }
        };
        items.add(rescanLvmItem);

        /* view log */
        final UpdatableItem viewLogMenu = new MyMenuItem(
                Tools.getString("ClusterBrowser.Drbd.ViewLogs"),
                GlobalInfo.LOGFILE_ICON, null, new AccessMode(
                        Application.AccessType.RO, false), new AccessMode(
                        Application.AccessType.RO, false)) {

            private static final long serialVersionUID = 1L;

            @Override
            public void action() {
                globalInfo.hidePopup();
                final DrbdLogs l = new DrbdLogs(
                        globalInfo.getCluster(),
                        GlobalInfo.ALL_LOGS_PATTERN);
                l.showDialog();
            }
        };
        items.add(viewLogMenu);
        return items;
    }

    private void addProxyHostWizard() {
        final Host proxyHost = new Host();
        proxyHost.setCluster(globalInfo.getCluster());
        final ProxyHostWizard w = new ProxyHostWizard(proxyHost, null);
        w.showDialogs();
    }
}