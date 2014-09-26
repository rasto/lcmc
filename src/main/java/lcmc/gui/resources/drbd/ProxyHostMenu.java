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

import lcmc.ProxyHostWizard;
import lcmc.gui.GUIData;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.HostBrowser;
import lcmc.gui.dialog.drbd.DrbdsLog;
import lcmc.utilities.DRBD;
import lcmc.utilities.EnablePredicate;
import lcmc.utilities.MenuAction;
import lcmc.utilities.MenuFactory;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Predicate;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.VisiblePredicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

@Named
public class ProxyHostMenu {
    /**
     * Not connectable.
     */
    private static final String NOT_CONNECTABLE_STRING = Tools.getString("ProxyHostInfo.NotConnectable");

    private ProxyHostInfo proxyHostInfo;
    @Inject
    private GUIData guiData;
    @Inject
    private ProxyHostWizard proxyHostWizard;
    @Inject
    private MenuFactory menuFactory;
    @Inject
    private Application application;
    @Inject
    private Provider<DrbdsLog> drbdsLogProvider;

    public List<UpdatableItem> getPulldownMenu(final ProxyHostInfo proxyHostInfo) {
        this.proxyHostInfo = proxyHostInfo;
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

        /* connect */
        final UpdatableItem connectItem = menuFactory.createMenuItem(
                        Tools.getString("HostDrbdInfo.Connect"),
                        null,
                        Tools.getString("HostDrbdInfo.Connect"),
                        Tools.getString("HostDrbdInfo.Disconnect"),
                        null,
                        Tools.getString("HostDrbdInfo.Disconnect"),
                        new AccessMode(Application.AccessType.RO, !AccessMode.ADVANCED),
                        new AccessMode(Application.AccessType.RO, !AccessMode.ADVANCED))
                        .predicate(new Predicate() {
                            @Override
                            public boolean check() {
                                return !getHost().isConnected();
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (getHost().getUsername() == null) {
                                    return NOT_CONNECTABLE_STRING;
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                if (getHost().isConnected()) {
                                    getHost().disconnect();
                                } else {
                                    getHost().connect(null, null);
                                }
                                getClusterBrowser().updateProxyHWInfo(getHost());
                            }
                        });
        items.add(connectItem);

        /* host wizard */
        final MyMenuItem hostWizardItem = menuFactory.createMenuItem(
                        Tools.getString("HostBrowser.ProxyHostWizard"),
                        HostBrowser.HOST_ICON_LARGE,
                        Tools.getString("HostBrowser.ProxyHostWizard"),
                        new AccessMode(Application.AccessType.RO, false),
                        new AccessMode(Application.AccessType.RO, false))
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                proxyHostWizard.init(getHost(), null);
                                proxyHostWizard.showDialogs();
                            }
                        });
        items.add(hostWizardItem);
        guiData.registerAddHostButton(hostWizardItem);
        final Application.RunMode runMode = Application.RunMode.LIVE;

        /* proxy start/stop */
        final UpdatableItem proxyItem = menuFactory.createMenuItem(
                        Tools.getString("HostDrbdInfo.Drbd.StopProxy"),
                        null,
                        getMenuToolTip("DRBD.stopProxy", ""),
                        Tools.getString("HostDrbdInfo.Drbd.StartProxy"),
                        null,
                        getMenuToolTip("DRBD.startProxy", ""),
                        new AccessMode(Application.AccessType.ADMIN, !AccessMode.ADVANCED),
                        new AccessMode(Application.AccessType.OP, !AccessMode.ADVANCED))
                        .predicate(new Predicate() {
                            @Override
                            public boolean check() {
                                return getHost().isDrbdProxyRunning();
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!getHost().isConnected()) {
                                    return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                if (getHost().isDrbdProxyRunning()) {
                                    DRBD.stopProxy(getHost(), runMode);
                                } else {
                                    DRBD.startProxy(getHost(), runMode);
                                }
                                getClusterBrowser().updateProxyHWInfo(getHost());
                            }
                        });
        items.add(proxyItem);

        /* all proxy connections up */
        final UpdatableItem allProxyUpItem = menuFactory.createMenuItem(
                        Tools.getString("HostDrbdInfo.Drbd.AllProxyUp"),
                        null,
                        getMenuToolTip("DRBD.proxyUp", DRBD.ALL_DRBD_RESOURCES),
                        new AccessMode(Application.AccessType.ADMIN, !AccessMode.ADVANCED),
                        new AccessMode(Application.AccessType.OP, !AccessMode.ADVANCED))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return getHost().isConnected() && getHost().isDrbdProxyRunning();
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                DRBD.proxyUp(getHost(), DRBD.ALL_DRBD_RESOURCES, null, runMode);
                                getClusterBrowser().updateProxyHWInfo(getHost());
                            }
                        });
        items.add(allProxyUpItem);

        /* all proxy connections down */
        final UpdatableItem allProxyDownItem = menuFactory.createMenuItem(
                        Tools.getString("HostDrbdInfo.Drbd.AllProxyDown"),
                        null,
                        getMenuToolTip("DRBD.proxyDown", DRBD.ALL_DRBD_RESOURCES),
                        new AccessMode(Application.AccessType.ADMIN, AccessMode.ADVANCED),
                        new AccessMode(Application.AccessType.OP, !AccessMode.ADVANCED))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return getHost().isConnected() && getHost().isDrbdProxyRunning();
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                DRBD.proxyDown(getHost(), DRBD.ALL_DRBD_RESOURCES, null, runMode);
                                getClusterBrowser().updateProxyHWInfo(getHost());
                            }
                        });
        items.add(allProxyDownItem);

        /* view logs */
        final UpdatableItem viewLogsItem = menuFactory.createMenuItem(
                        Tools.getString("HostBrowser.Drbd.ViewLogs"),
                        ProxyHostInfo.LOGFILE_ICON,
                        Tools.getString("HostBrowser.Drbd.ViewLogs"),
                        new AccessMode(Application.AccessType.RO, false),
                        new AccessMode(Application.AccessType.RO, false))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (!getHost().isConnected()) {
                                    return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                final DrbdsLog drbdsLog = drbdsLogProvider.get();
                                drbdsLog.init(getHost());
                                drbdsLog.showDialog();
                            }
                        });
        items.add(viewLogsItem);

        /* remove host from gui */
        final UpdatableItem removeHostItem = menuFactory.createMenuItem(
                        Tools.getString("HostBrowser.RemoveHost"),
                        HostBrowser.HOST_REMOVE_ICON,
                        Tools.getString("HostBrowser.RemoveHost"),
                        new AccessMode(Application.AccessType.RO, false),
                        new AccessMode(Application.AccessType.RO, false))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (getHost().isInCluster()) {
                                    return "it is a member of a cluster";
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                getHost().disconnect();
                                application.removeHostFromHosts(getHost());
                                guiData.allHostsUpdate();
                            }
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
