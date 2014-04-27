package lcmc.gui.resources.crm;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JColorChooser;
import lcmc.EditHostDialog;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.HostBrowser;
import lcmc.gui.dialog.HostLogs;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.CRM;
import lcmc.utilities.Corosync;
import lcmc.utilities.Heartbeat;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Openais;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class HostMenu {
    private static final String NOT_IN_CLUSTER = "not in cluster";

    private final HostInfo hostInfo;

    public HostMenu(final HostInfo hostInfo) {
        this.hostInfo = hostInfo;
    }

    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        /* host wizard */
        final MyMenuItem hostWizardItem =
            new MyMenuItem(Tools.getString("HostBrowser.HostWizard"),
                           HostBrowser.HOST_ICON_LARGE,
                           "",
                           new AccessMode(Application.AccessType.RO, false),
                           new AccessMode(Application.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void action() {
                    final EditHostDialog dialog = new EditHostDialog(getHost());
                    dialog.showDialogs();
                }
            };
        items.add(hostWizardItem);
        Tools.getGUIData().registerAddHostButton(hostWizardItem);
        /* cluster manager standby on/off */
        final Application.RunMode runMode = Application.RunMode.LIVE;
        final MyMenuItem standbyItem =
            new MyMenuItem(Tools.getString("HostBrowser.CRM.StandByOn"),
                           HostInfo.HOST_STANDBY_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           Tools.getString("HostBrowser.CRM.StandByOff"),
                           HostInfo.HOST_STANDBY_OFF_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return !hostInfo.isStandby(runMode);
                }

                @Override
                public String enablePredicate() {
                    if (!getHost().isClStatus()) {
                        return HostInfo.NO_PCMK_STATUS_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    final Host dcHost =
                                  getBrowser().getClusterBrowser().getDCHost();
                    if (hostInfo.isStandby(runMode)) {
                        CRM.standByOff(dcHost, getHost(), runMode);
                    } else {
                        CRM.standByOn(dcHost, getHost(), runMode);
                    }
                }
            };
        final ClusterBrowser cb = getBrowser().getClusterBrowser();
        if (cb != null) {
            final ButtonCallback standbyItemCallback =
                                              cb.new ClMenuItemCallback(getHost()) {
                @Override
                public void action(final Host dcHost) {
                    if (hostInfo.isStandby(Application.RunMode.LIVE)) {
                        CRM.standByOff(dcHost, getHost(), Application.RunMode.TEST);
                    } else {
                        CRM.standByOn(dcHost, getHost(), Application.RunMode.TEST);
                    }
                }
            };
            hostInfo.addMouseOverListener(standbyItem, standbyItemCallback);
        }
        items.add(standbyItem);

        /* Migrate all services from this host. */
        final MyMenuItem allMigrateFromItem =
            new MyMenuItem(Tools.getString("HostInfo.CRM.AllMigrateFrom"),
                           HostInfo.HOST_STANDBY_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (!getHost().isClStatus()) {
                        return HostInfo.NO_PCMK_STATUS_STRING;
                    }
                    if (getBrowser().getClusterBrowser()
                                    .getExistingServiceList(null).isEmpty()) {
                        return "there are no services to migrate";
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final ServiceInfo si
                            : cb.getExistingServiceList(null)) {
                        if (!si.isConstraintPH()
                            && si.getGroupInfo() == null
                            && si.getCloneInfo() == null) {
                            final List<String> runningOnNodes =
                                  si.getRunningOnNodes(Application.RunMode.LIVE);
                            if (runningOnNodes != null
                                && runningOnNodes.contains(
                                                        getHost().getName())) {
                                final Host dcHost = getHost();
                                si.migrateFromResource(dcHost,
                                                       getHost().getName(),
                                                       Application.RunMode.LIVE);
                            }
                        }
                    }
                }
            };
        if (cb != null) {
            final ButtonCallback allMigrateFromItemCallback =
                                              cb.new ClMenuItemCallback(getHost()) {
                @Override
                public void action(final Host dcHost) {
                    for (final ServiceInfo si
                            : cb.getExistingServiceList(null)) {
                        if (!si.isConstraintPH() && si.getGroupInfo() == null) {
                            final List<String> runningOnNodes =
                                                   si.getRunningOnNodes(Application.RunMode.LIVE);
                            if (runningOnNodes != null
                                && runningOnNodes.contains(
                                                        getHost().getName())) {
                                si.migrateFromResource(dcHost,
                                                       getHost().getName(),
                                                       Application.RunMode.TEST);
                            }
                        }
                    }
                }
            };
            hostInfo.addMouseOverListener(allMigrateFromItem, allMigrateFromItemCallback);
        }
        items.add(allMigrateFromItem);
        /* Stop corosync/openais. */
        final MyMenuItem stopCorosyncItem =
            new MyMenuItem(Tools.getString("HostInfo.StopCorosync"),
                           HostInfo.HOST_STOP_COMM_LAYER_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           Tools.getString("HostInfo.StopOpenais"),
                           HostInfo.HOST_STOP_COMM_LAYER_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(Application.AccessType.ADMIN, true),
                           new AccessMode(Application.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    final Host h = getHost();
                    if (!h.isInCluster()) {
                        return NOT_IN_CLUSTER;
                    }
                    return null;
                }

                @Override
                public boolean predicate() {
                    /* when both are running it's openais. */
                    return getHost().isCsRunning() && !getHost().isAisRunning();
                }

                @Override
                public boolean visiblePredicate() {
                    return getHost().isCsRunning()
                           || getHost().isAisRunning();
                }

                @Override
                public void action() {
                    if (Tools.confirmDialog(
                         Tools.getString("HostInfo.confirmCorosyncStop.Title"),
                         Tools.getString("HostInfo.confirmCorosyncStop.Desc"),
                         Tools.getString("HostInfo.confirmCorosyncStop.Yes"),
                         Tools.getString("HostInfo.confirmCorosyncStop.No"))) {
                        final Host thisHost = getHost();
                        thisHost.setCommLayerStopping(true);
                        if (!thisHost.isPcmkStartedByCorosync()
                            && thisHost.isPcmkInit()
                            && thisHost.isPcmkRunning()) {
                            if (getHost().isCsRunning()
                                && !getHost().isAisRunning()) {
                                Corosync.stopCorosyncWithPcmk(thisHost);
                            } else {
                                Openais.stopOpenaisWithPcmk(thisHost);
                            }
                        } else {
                            if (getHost().isCsRunning()
                                && !getHost().isAisRunning()) {
                                Corosync.stopCorosync(thisHost);
                            } else {
                                Openais.stopOpenais(thisHost);
                            }
                        }
                        updateClusterView(thisHost);
                    }
                }
            };
        if (cb != null) {
            final ButtonCallback stopCorosyncItemCallback =
                                              cb.new ClMenuItemCallback(getHost()) {
                @Override
                public void action(final Host dcHost) {
                    if (!hostInfo.isStandby(Application.RunMode.LIVE)) {
                        CRM.standByOn(dcHost, getHost(), Application.RunMode.TEST);
                    }
                }
            };
            hostInfo.addMouseOverListener(stopCorosyncItem,
                                          stopCorosyncItemCallback);
        }
        items.add(stopCorosyncItem);
        /* Stop heartbeat. */
        final MyMenuItem stopHeartbeatItem =
            new MyMenuItem(Tools.getString("HostInfo.StopHeartbeat"),
                           HostInfo.HOST_STOP_COMM_LAYER_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(Application.AccessType.ADMIN, true),
                           new AccessMode(Application.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getHost().isHeartbeatRunning();
                }

                @Override
                public String enablePredicate() {
                    final Host h = getHost();
                    if (!h.isInCluster()) {
                        return NOT_IN_CLUSTER;
                    }
                    return null;
                }

                @Override
                public void action() {
                    if (Tools.confirmDialog(
                         Tools.getString("HostInfo.confirmHeartbeatStop.Title"),
                         Tools.getString("HostInfo.confirmHeartbeatStop.Desc"),
                         Tools.getString("HostInfo.confirmHeartbeatStop.Yes"),
                         Tools.getString("HostInfo.confirmHeartbeatStop.No"))) {
                        getHost().setCommLayerStopping(true);
                        Heartbeat.stopHeartbeat(getHost());
                        updateClusterView(getHost());
                    }
                }
            };
        if (cb != null) {
            final ButtonCallback stopHeartbeatItemCallback =
                                              cb.new ClMenuItemCallback(getHost()) {
                @Override
                public void action(final Host dcHost) {
                    if (!hostInfo.isStandby(Application.RunMode.LIVE)) {
                        CRM.standByOn(dcHost, getHost(), Application.RunMode.TEST);
                    }
                }
            };
            hostInfo.addMouseOverListener(stopHeartbeatItem,
                                          stopHeartbeatItemCallback);
        }
        items.add(stopHeartbeatItem);
        /* Start corosync. */
        final MyMenuItem startCorosyncItem =
            new MyMenuItem(Tools.getString("HostInfo.StartCorosync"),
                           HostInfo.HOST_START_COMM_LAYER_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(Application.AccessType.ADMIN, false),
                           new AccessMode(Application.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    final Host h = getHost();
                    return h.isCorosync()
                           && h.isCsInit()
                           && h.isCsAisConf()
                           && !h.isCsRunning()
                           && !h.isAisRunning()
                           && !h.isHeartbeatRunning()
                           && !h.isHeartbeatRc();
                }

                @Override
                public String enablePredicate() {
                    final Host h = getHost();
                    if (!h.isInCluster()) {
                        return NOT_IN_CLUSTER;
                    }
                    if (h.isAisRc() && !h.isCsRc()) {
                        return "Openais is in rc.d";
                    }
                    return null;
                }

                @Override
                public void action() {
                    getHost().setCommLayerStarting(true);
                    if (getHost().isPcmkRc()) {
                        Corosync.startCorosyncWithPcmk(getHost());
                    } else {
                        Corosync.startCorosync(getHost());
                    }
                    updateClusterView(getHost());
                }
            };
        if (cb != null) {
            final ButtonCallback startCorosyncItemCallback =
                                              cb.new ClMenuItemCallback(getHost()) {
                @Override
                public void action(final Host dcHost) {
                    //TODO
                }
            };
            hostInfo.addMouseOverListener(startCorosyncItem,
                                          startCorosyncItemCallback);
        }
        items.add(startCorosyncItem);
        /* Start openais. */
        final MyMenuItem startOpenaisItem =
            new MyMenuItem(Tools.getString("HostInfo.StartOpenais"),
                           HostInfo.HOST_START_COMM_LAYER_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(Application.AccessType.ADMIN, false),
                           new AccessMode(Application.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    final Host h = getHost();
                    return h.isAisInit()
                           && h.isCsAisConf()
                           && !h.isCsRunning()
                           && !h.isAisRunning()
                           && !h.isHeartbeatRunning()
                           && !h.isHeartbeatRc();
                }

                @Override
                public String enablePredicate() {
                    final Host h = getHost();
                    if (!h.isInCluster()) {
                        return NOT_IN_CLUSTER;
                    }
                    if (h.isCsRc() && !h.isAisRc()) {
                        return "Corosync is in rc.d";
                    }
                    return null;
                }

                @Override
                public void action() {
                    getHost().setCommLayerStarting(true);
                    Openais.startOpenais(getHost());
                    updateClusterView(getHost());
                }
            };
        if (cb != null) {
            final ButtonCallback startOpenaisItemCallback =
                                              cb.new ClMenuItemCallback(getHost()) {
                @Override
                public void action(final Host dcHost) {
                    //TODO
                }
            };
            hostInfo.addMouseOverListener(startOpenaisItem,
                                          startOpenaisItemCallback);
        }
        items.add(startOpenaisItem);
        /* Start heartbeat. */
        final MyMenuItem startHeartbeatItem =
            new MyMenuItem(Tools.getString("HostInfo.StartHeartbeat"),
                           HostInfo.HOST_START_COMM_LAYER_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(Application.AccessType.ADMIN, false),
                           new AccessMode(Application.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    final Host h = getHost();
                    return h.isHeartbeatInit()
                           && h.isHeartbeatConf()
                           && !h.isCsRunning()
                           && !h.isAisRunning()
                           && !h.isHeartbeatRunning();
                           //&& !h.isAisRc()
                           //&& !h.isCsRc(); TODO should check /etc/defaults/
                }

                @Override
                public String enablePredicate() {
                    final Host h = getHost();
                    if (!h.isInCluster()) {
                        return NOT_IN_CLUSTER;
                    }
                    return null;
                }

                @Override
                public void action() {
                    getHost().setCommLayerStarting(true);
                    Heartbeat.startHeartbeat(getHost());
                    updateClusterView(getHost());
                }
            };
        if (cb != null) {
            final ButtonCallback startHeartbeatItemCallback =
                                               cb.new ClMenuItemCallback(getHost()) {
                @Override
                public void action(final Host dcHost) {
                    //TODO
                }
            };
            hostInfo.addMouseOverListener(startHeartbeatItem,
                                          startHeartbeatItemCallback);
        }
        items.add(startHeartbeatItem);

        /* Start pacemaker. */
        final MyMenuItem startPcmkItem =
            new MyMenuItem(Tools.getString("HostInfo.StartPacemaker"),
                           HostInfo.HOST_START_COMM_LAYER_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(Application.AccessType.ADMIN, false),
                           new AccessMode(Application.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    final Host h = getHost();
                    return !h.isPcmkStartedByCorosync()
                           && !h.isPcmkRunning()
                           && (h.isCsRunning()
                               || h.isAisRunning())
                           && !h.isHeartbeatRunning();
                }

                @Override
                public String enablePredicate() {
                    final Host h = getHost();
                    if (!h.isInCluster()) {
                        return NOT_IN_CLUSTER;
                    }
                    return null;
                }

                @Override
                public void action() {
                    getHost().setPcmkStarting(true);
                    Corosync.startPacemaker(getHost());
                    updateClusterView(getHost());
                }
            };
        if (cb != null) {
            final ButtonCallback startPcmkItemCallback =
                                               cb.new ClMenuItemCallback(getHost()) {
                @Override
                public void action(final Host dcHost) {
                    //TODO
                }
            };
            hostInfo.addMouseOverListener(startPcmkItem,
                                          startPcmkItemCallback);
        }
        items.add(startPcmkItem);
        /* change host color */
        final UpdatableItem changeHostColorItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ChangeHostColor"),
                           null,
                           "",
                           new AccessMode(Application.AccessType.RO, false),
                           new AccessMode(Application.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void action() {
                    final Color newColor = JColorChooser.showDialog(
                                            Tools.getGUIData().getMainFrame(),
                                            "Choose " + getHost().getName()
                                            + " color",
                                            getHost().getPmColors()[0]);
                    if (newColor != null) {
                        getHost().setSavedColor(newColor);
                    }
                }
            };
        items.add(changeHostColorItem);

        /* view logs */
        final UpdatableItem viewLogsItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ViewLogs"),
                           HostInfo.LOGFILE_ICON,
                           "",
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
                    final HostLogs l = new HostLogs(getHost());
                    l.showDialog();
                }
            };
        items.add(viewLogsItem);
        /* advacend options */
        final UpdatableItem hostAdvancedSubmenu = new MyMenu(
                        Tools.getString("HostBrowser.AdvancedSubmenu"),
                        new AccessMode(Application.AccessType.OP, false),
                        new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (!getHost().isConnected()) {
                    return Host.NOT_CONNECTED_STRING;
                }
                return null;
            }

            @Override
            public void updateAndWait() {
                super.updateAndWait();
                getBrowser().addAdvancedMenu(this);
            }
        };
        items.add(hostAdvancedSubmenu);

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
                    final ClusterBrowser b = getBrowser().getClusterBrowser();
                    if (b != null) {
                        Tools.getGUIData().unregisterAllHostsUpdate(
                                                      b.getClusterViewPanel());
                    }
                    Tools.getApplication().removeHostFromHosts(getHost());
                    Tools.getGUIData().allHostsUpdate();
                }
            };
        items.add(removeHostItem);
        return items;
    }
    
    private Host getHost() {
        return hostInfo.getHost();
    }

    private HostBrowser getBrowser() {
        return hostInfo.getBrowser();
    }

    /* Update cluster view if available. */
    private void updateClusterView(final Host host) {
        final ClusterBrowser cb = getBrowser().getClusterBrowser();
        if (cb == null) {
            host.setIsLoading();
            host.getHWInfo(!Host.UPDATE_LVM);
        } else {
            cb.updateHWInfo(host, !Host.UPDATE_LVM);
        }
    }
}
