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
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.HostBrowser;
import lcmc.gui.dialog.drbd.DrbdsLog;
import lcmc.utilities.DRBD;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class ProxyHostMenu {
    /** Not connectable. */
    private static final String NOT_CONNECTABLE_STRING =
                               Tools.getString("ProxyHostInfo.NotConnectable");
    
    private final ProxyHostInfo proxyHostInfo;
    
    public ProxyHostMenu(final ProxyHostInfo proxyHostInfo) {
        this.proxyHostInfo = proxyHostInfo;
    }

    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

        /* connect */
        final UpdatableItem connectItem =
            new MyMenuItem(Tools.getString("HostDrbdInfo.Connect"),
                           null,
                           Tools.getString("HostDrbdInfo.Connect"),
                           Tools.getString("HostDrbdInfo.Disconnect"),
                           null,
                           Tools.getString("HostDrbdInfo.Disconnect"),
                           new AccessMode(Application.AccessType.RO,
                                          !AccessMode.ADVANCED),
                           new AccessMode(Application.AccessType.RO,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return !getHost().isConnected();
                }

                @Override
                public String enablePredicate() {
                    if (getHost().getUsername() == null) {
                        return NOT_CONNECTABLE_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    if (getHost().isConnected()) {
                        getHost().disconnect();
                    } else {
                        getHost().connect(null, null);
                    }
                    getClusterBrowser().updateProxyHWInfo(getHost());
                }
            };
        items.add(connectItem);

        /* host wizard */
        final MyMenuItem hostWizardItem =
            new MyMenuItem(Tools.getString("HostBrowser.ProxyHostWizard"),
                           HostBrowser.HOST_ICON_LARGE,
                           Tools.getString("HostBrowser.ProxyHostWizard"),
                           new AccessMode(Application.AccessType.RO,
                                          false),
                           new AccessMode(Application.AccessType.RO,
                                          false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void action() {
                    final ProxyHostWizard dialog =
                            new ProxyHostWizard(getHost(), null);
                    dialog.showDialogs();
                }
            };
        items.add(hostWizardItem);
        Tools.getGUIData().registerAddHostButton(hostWizardItem);
        final Application.RunMode runMode = Application.RunMode.LIVE;

        /* proxy start/stop */
        final UpdatableItem proxyItem =
            new MyMenuItem(Tools.getString("HostDrbdInfo.Drbd.StopProxy"),
                           null,
                           getMenuToolTip("DRBD.stopProxy", ""),
                           Tools.getString("HostDrbdInfo.Drbd.StartProxy"),
                           null,
                           getMenuToolTip("DRBD.startProxy", ""),
                           new AccessMode(Application.AccessType.ADMIN,
                                          !AccessMode.ADVANCED),
                           new AccessMode(Application.AccessType.OP,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return getHost().isDrbdProxyRunning();
                }

                @Override
                public String enablePredicate() {
                    if (!getHost().isConnected()) {
                        return Host.NOT_CONNECTED_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    if (getHost().isDrbdProxyRunning()) {
                        DRBD.stopProxy(getHost(), runMode);
                    } else {
                        DRBD.startProxy(getHost(), runMode);
                    }
                    getClusterBrowser().updateProxyHWInfo(getHost());
                }
            };
        items.add(proxyItem);

        /* all proxy connections up */
        final UpdatableItem allProxyUpItem =
            new MyMenuItem(Tools.getString("HostDrbdInfo.Drbd.AllProxyUp"),
                           null,
                           getMenuToolTip("DRBD.proxyUp", DRBD.ALL),
                           new AccessMode(Application.AccessType.ADMIN,
                                          !AccessMode.ADVANCED),
                           new AccessMode(Application.AccessType.OP,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getHost().isConnected()
                           && getHost().isDrbdProxyRunning();
                }

                @Override
                public void action() {
                    DRBD.proxyUp(getHost(), DRBD.ALL, null, runMode);
                    getClusterBrowser().updateProxyHWInfo(getHost());
                }
            };
        items.add(allProxyUpItem);

        /* all proxy connections down */
        final UpdatableItem allProxyDownItem =
            new MyMenuItem(Tools.getString("HostDrbdInfo.Drbd.AllProxyDown"),
                           null,
                           getMenuToolTip("DRBD.proxyDown", DRBD.ALL),
                           new AccessMode(Application.AccessType.ADMIN,
                                          AccessMode.ADVANCED),
                           new AccessMode(Application.AccessType.OP,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getHost().isConnected()
                           && getHost().isDrbdProxyRunning();
                }

                @Override
                public void action() {
                    DRBD.proxyDown(getHost(), DRBD.ALL, null, runMode);
                    getClusterBrowser().updateProxyHWInfo(getHost());
                }
            };
        items.add(allProxyDownItem);

        /* view logs */
        final UpdatableItem viewLogsItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ViewLogs"),
                           ProxyHostInfo.LOGFILE_ICON,
                           Tools.getString("HostBrowser.Drbd.ViewLogs"),
                           new AccessMode(Application.AccessType.RO, false),
                           new AccessMode(Application.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (!getHost().isConnected()) {
                        return Host.NOT_CONNECTED_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    final DrbdsLog l = new DrbdsLog(getHost());
                    l.showDialog();
                }
            };
        items.add(viewLogsItem);

        /* remove host from gui */
        final UpdatableItem removeHostItem =
            new MyMenuItem(Tools.getString("HostBrowser.RemoveHost"),
                           HostBrowser.HOST_REMOVE_ICON,
                           Tools.getString("HostBrowser.RemoveHost"),
                           new AccessMode(Application.AccessType.RO, false),
                           new AccessMode(Application.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getHost().isInCluster()) {
                        return "it is a member of a cluster";
                    }
                    return null;
                }

                @Override
                public void action() {
                    getHost().disconnect();
                    Tools.getApplication().removeHostFromHosts(getHost());
                    Tools.getGUIData().allHostsUpdate();
                }
            };
        items.add(removeHostItem);

        return items;
    }

    /** Tool tip for menu items. */
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
