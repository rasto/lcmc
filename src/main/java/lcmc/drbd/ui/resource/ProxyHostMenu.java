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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.MainPresenter;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.drbd.service.DRBD;
import lcmc.drbd.ui.DrbdsLog;
import lcmc.drbd.ui.ProxyHostWizard;
import lcmc.host.domain.Host;
import lcmc.host.ui.HostBrowser;

@Named
public class ProxyHostMenu {
    /**
     * Not connectable.
     */
    private static final String NOT_CONNECTABLE_STRING = Tools.getString("ProxyHostInfo.NotConnectable");

    private ProxyHostInfo proxyHostInfo;
    @Inject
    private MainData mainData;
    @Inject
    private MainPresenter mainPresenter;
    @Inject
    private ProxyHostWizard proxyHostWizard;
    @Inject
    private MenuFactory menuFactory;
    @Inject
    private Application application;
    @Inject
    private Provider<DrbdsLog> drbdsLogProvider;

    public ProxyHostMenu() {
    }

    public List<UpdatableItem> getPulldownMenu(final ProxyHostInfo proxyHostInfo) {
        this.proxyHostInfo = proxyHostInfo;
        final List<UpdatableItem> items = new ArrayList<>();

        /* connect */
        final UpdatableItem connectItem =
                menuFactory.createMenuItem(Tools.getString("HostDrbdInfo.Connect"), null, Tools.getString("HostDrbdInfo.Connect"),
                                Tools.getString("HostDrbdInfo.Disconnect"), null, Tools.getString("HostDrbdInfo.Disconnect"),
                                new AccessMode(AccessMode.RO, AccessMode.NORMAL), new AccessMode(AccessMode.RO, AccessMode.NORMAL))
                        .predicate(() -> !getHost().isConnected())
                        .enablePredicate(() -> {
                            if (getHost().getUsername() == null) {
                                return NOT_CONNECTABLE_STRING;
                            }
                            return null;
                        })
                        .addAction(text -> {
                            if (getHost().isConnected()) {
                                getHost().disconnect();
                            } else {
                                getHost().connect(null, null);
                            }
                            getClusterBrowser().updateProxyHWInfo(getHost());
                        });
        items.add(connectItem);

        /* host wizard */
        final MyMenuItem hostWizardItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.ProxyHostWizard"), HostBrowser.HOST_ICON_LARGE,
                        Tools.getString("HostBrowser.ProxyHostWizard"), new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL)).addAction(text -> {
                    proxyHostWizard.init(getHost(), null);
                    proxyHostWizard.showDialogs();
                });
        items.add(hostWizardItem);
        mainData.registerAddHostButton(hostWizardItem);
        final Application.RunMode runMode = Application.RunMode.LIVE;

        /* proxy start/stop */
        final UpdatableItem proxyItem = menuFactory.createMenuItem(Tools.getString("HostDrbdInfo.Drbd.StopProxy"), null,
                        getMenuToolTip("DRBD.stopProxy", ""), Tools.getString("HostDrbdInfo.Drbd.StartProxy"), null,
                        getMenuToolTip("DRBD.startProxy", ""), new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .predicate(() -> getHost().isDrbdProxyRunning())
                .enablePredicate(() -> {
                    if (!getHost().isConnected()) {
                        return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                    }
                    return null;
                })
                .addAction(text -> {
                    if (getHost().isDrbdProxyRunning()) {
                        DRBD.stopProxy(getHost(), runMode);
                    } else {
                        DRBD.startProxy(getHost(), runMode);
                    }
                    getClusterBrowser().updateProxyHWInfo(getHost());
                });
        items.add(proxyItem);

        /* all proxy connections up */
        final UpdatableItem allProxyUpItem = menuFactory.createMenuItem(Tools.getString("HostDrbdInfo.Drbd.AllProxyUp"), null,
                        getMenuToolTip("DRBD.proxyUp", DRBD.ALL_DRBD_RESOURCES), new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(() -> getHost().isConnected() && getHost().isDrbdProxyRunning())
                .addAction(text -> {
                    DRBD.proxyUp(getHost(), DRBD.ALL_DRBD_RESOURCES, null, runMode);
                    getClusterBrowser().updateProxyHWInfo(getHost());
                });
        items.add(allProxyUpItem);

        /* all proxy connections down */
        final UpdatableItem allProxyDownItem = menuFactory.createMenuItem(Tools.getString("HostDrbdInfo.Drbd.AllProxyDown"), null,
                        getMenuToolTip("DRBD.proxyDown", DRBD.ALL_DRBD_RESOURCES), new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .visiblePredicate(() -> getHost().isConnected() && getHost().isDrbdProxyRunning())
                .addAction(text -> {
                    DRBD.proxyDown(getHost(), DRBD.ALL_DRBD_RESOURCES, null, runMode);
                    getClusterBrowser().updateProxyHWInfo(getHost());
                });
        items.add(allProxyDownItem);

        /* view logs */
        final UpdatableItem viewLogsItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.Drbd.ViewLogs"), ProxyHostInfo.LOGFILE_ICON,
                        Tools.getString("HostBrowser.Drbd.ViewLogs"), new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL)).enablePredicate(() -> {
                    if (!getHost().isConnected()) {
                        return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                    }
                    return null;
                }).addAction(text -> {
                    final DrbdsLog drbdsLog = drbdsLogProvider.get();
                    drbdsLog.init(getHost());
                    drbdsLog.showDialog();
                });
        items.add(viewLogsItem);

        /* remove host from gui */
        final UpdatableItem removeHostItem =
                menuFactory.createMenuItem(Tools.getString("HostBrowser.RemoveHost"), HostBrowser.HOST_REMOVE_ICON,
                        Tools.getString("HostBrowser.RemoveHost"), new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL)).enablePredicate(() -> {
                    if (getHost().isInCluster()) {
                        return "it is a member of a cluster";
                    }
                    return null;
                }).addAction(text -> {
                    getHost().disconnect();
                    application.removeHostFromHosts(getHost());
                    mainPresenter.allHostsUpdate();
                });
        items.add(removeHostItem);

        return items;
    }

    /**
     * Tool tip for menu items.
     */
    private String getMenuToolTip(final String cmd, final String res) {
        final String distString = getHost().getDistString(cmd);
        if (distString == null) {
            return "";
        }
        return distString.replaceAll("@RES-VOL@", res).replaceAll("@.*?@", "");
    }

    private Host getHost() {
        return proxyHostInfo.getHost();
    }

    private ClusterBrowser getClusterBrowser() {
        return proxyHostInfo.getBrowser().getClusterBrowser();
    }
}
