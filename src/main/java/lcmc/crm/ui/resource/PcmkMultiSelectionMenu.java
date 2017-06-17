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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JColorChooser;

import lcmc.common.ui.Access;
import lcmc.common.ui.CallbackAction;
import lcmc.common.ui.main.MainData;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.crm.domain.ClusterStatus;
import lcmc.host.domain.Host;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.ui.Info;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.crm.service.CRM;
import lcmc.common.ui.utils.ComponentWithTest;
import lcmc.crm.service.Corosync;
import lcmc.common.domain.EnablePredicate;
import lcmc.crm.service.Heartbeat;
import lcmc.common.ui.utils.MenuAction;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.crm.service.Openais;
import lcmc.common.domain.Predicate;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.common.domain.VisiblePredicate;
import lcmc.host.domain.parser.HostParser;
import lombok.val;

@Named
public class PcmkMultiSelectionMenu {
    private PcmkMultiSelectionInfo pcmkMultiSelectionInfo;
    @Inject
    private MenuFactory menuFactory;
    @Inject
    private Application application;
    @Inject
    private MainData mainData;
    @Inject
    private Access access;

    public List<UpdatableItem> getPulldownMenu(final PcmkMultiSelectionInfo pcmkMultiSelectionInfo) {
        this.pcmkMultiSelectionInfo = pcmkMultiSelectionInfo;
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final Collection<ServiceInfo> selectedServiceInfos = new ArrayList<ServiceInfo>();
        final List<HostInfo> selectedHostInfos = new ArrayList<HostInfo>();
        for (final Info i : pcmkMultiSelectionInfo.getSelectedInfos()) {
            if (i instanceof ServiceInfo) {
                selectedServiceInfos.add((ServiceInfo) i);
            } else if (i instanceof HostInfo) {
                selectedHostInfos.add((HostInfo) i);
            }
        }
        if (!selectedHostInfos.isEmpty()) {
            createSelectedHostsPopup(selectedHostInfos, items);
        }
        if (!selectedServiceInfos.isEmpty()) {
            createSelectedServicesPopup(selectedServiceInfos, items);
        }
        return items;
    }

    private void createSelectedHostsPopup(final List<HostInfo> selectedHostInfos,
                                          final Collection<UpdatableItem> items) {
        /* cluster manager standby on */
        final MyMenuItem standbyItem =
                menuFactory.createMenuItem(Tools.getString("PcmkMultiSelectionInfo.StandByOn"),
                        HostInfo.HOST_STANDBY_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                for (final HostInfo hi : selectedHostInfos) {
                                    if (!hi.isStandby(Application.RunMode.LIVE)) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (getBrowser().crmStatusFailed()) {
                                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                                }
                                final Host dcHost = getBrowser().getDCHost();
                                if (!dcHost.isCrmStatusOk()) {
                                    return HostInfo.NO_PCMK_STATUS_STRING + " (" + dcHost.getName() + ')';
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                final Host dcHost = getBrowser().getDCHost();
                                for (final HostInfo hi : selectedHostInfos) {
                                    if (!hi.isStandby(Application.RunMode.LIVE)) {
                                        CRM.standByOn(dcHost, hi.getHost(), Application.RunMode.LIVE);
                                    }
                                }
                            }
                        });
        final ButtonCallback standbyItemCallback = getBrowser().new ClMenuItemCallback(getBrowser().getDCHost())
                .addAction(new CallbackAction() {
                    @Override
                    public void run(final Host dcHost) {
                        for (final HostInfo hi : selectedHostInfos) {
                            if (!hi.isStandby(Application.RunMode.LIVE)) {
                                CRM.standByOn(dcHost, hi.getHost(), Application.RunMode.TEST);
                            }
                        }
                    }
                });
        pcmkMultiSelectionInfo.addMouseOverListener(standbyItem, standbyItemCallback);
        items.add(standbyItem);

        /* cluster manager standby off */
        final MyMenuItem onlineItem =
                menuFactory.createMenuItem(Tools.getString("PcmkMultiSelectionInfo.StandByOff"),
                        HostInfo.HOST_STANDBY_OFF_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                for (final HostInfo hi : selectedHostInfos) {
                                    if (hi.isStandby(Application.RunMode.LIVE)) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (getBrowser().crmStatusFailed()) {
                                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                                }
                                final Host dcHost = getBrowser().getDCHost();
                                if (!dcHost.isCrmStatusOk()) {
                                    return HostInfo.NO_PCMK_STATUS_STRING + " (" + dcHost.getName() + ')';
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                final Host dcHost = getBrowser().getDCHost();
                                for (final HostInfo hi : selectedHostInfos) {
                                    if (hi.isStandby(Application.RunMode.LIVE)) {
                                        CRM.standByOff(dcHost, hi.getHost(), Application.RunMode.LIVE);
                                    }
                                }
                            }
                        });
        final ButtonCallback onlineItemCallback = getBrowser().new ClMenuItemCallback(getBrowser().getDCHost())
                .addAction(new CallbackAction() {
                    @Override
                    public void run(final Host dcHost) {
                        for (final HostInfo hi : selectedHostInfos) {
                            if (hi.isStandby(Application.RunMode.LIVE)) {
                                CRM.standByOff(dcHost, hi.getHost(), Application.RunMode.TEST);
                            }
                        }
                    }
                });
        pcmkMultiSelectionInfo.addMouseOverListener(onlineItem, onlineItemCallback);
        items.add(onlineItem);

        /* Stop corosync/openais. */
        final MyMenuItem stopCorosyncItem =
                menuFactory.createMenuItem(Tools.getString("PcmkMultiSelectionInfo.StopCorosync"),
                        HostInfo.HOST_STOP_COMM_LAYER_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        Tools.getString("PcmkMultiSelectionInfo.StopOpenais"),
                        HostInfo.HOST_STOP_COMM_LAYER_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED),
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL))
                        .predicate(new Predicate() {
                            @Override
                            public boolean check() {
                    /* when both are running it's openais. */
                                final HostInfo hi = selectedHostInfos.get(0);
                                return hi.getHost().getHostParser().isCorosyncRunning() && !hi.getHost().getHostParser().isOpenaisRunning();
                            }
                        })
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                for (final HostInfo hi : selectedHostInfos) {
                                    if (hi.getHost().getHostParser().isCorosyncRunning() || hi.getHost().getHostParser().isOpenaisRunning()) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                if (application.confirmDialog(Tools.getString("HostInfo.confirmCorosyncStop.Title"),
                                        Tools.getString("HostInfo.confirmCorosyncStop.Desc"),
                                        Tools.getString("HostInfo.confirmCorosyncStop.Yes"),
                                        Tools.getString("HostInfo.confirmCorosyncStop.No"))) {
                                    for (final HostInfo hi : selectedHostInfos) {
                                        hi.getHost().getHostParser().setCommLayerStopping(true);
                                    }
                                    for (final HostInfo hi : selectedHostInfos) {
                                        val host = hi.getHost();
                                        val hostParser = host.getHostParser();
                                        if (!hostParser.isPcmkStartedByCorosync()
                                                && hostParser.hasPacemakerInitScript()
                                                && hostParser.isPacemakerRunning()) {
                                            if (hostParser.isCorosyncRunning() && !hostParser.isOpenaisRunning()) {
                                                Corosync.stopCorosyncWithPcmk(host);
                                            } else {
                                                Openais.stopOpenaisWithPcmk(host);
                                            }
                                        } else {
                                            if (hostParser.isCorosyncRunning() && !hostParser.isOpenaisRunning()) {
                                                Corosync.stopCorosync(host);
                                            } else {
                                                Openais.stopOpenais(host);
                                            }
                                        }
                                    }

                                    for (final HostInfo hi : selectedHostInfos) {
                                        getBrowser().updateHWInfo(hi.getHost(), !Host.UPDATE_LVM);
                                    }
                                }
                            }
                        });
        final ButtonCallback stopCorosyncItemCallback =
                getBrowser().new ClMenuItemCallback(getBrowser().getDCHost())
                        .addAction(new CallbackAction() {
                            @Override
                            public void run(final Host dcHost) {
                                for (final HostInfo hi : selectedHostInfos) {
                                    if (!hi.isStandby(Application.RunMode.LIVE)) {
                                        CRM.standByOn(dcHost, hi.getHost(), Application.RunMode.TEST);
                                    }
                                }
                            }
                        });
        pcmkMultiSelectionInfo.addMouseOverListener(stopCorosyncItem, stopCorosyncItemCallback);
        items.add(stopCorosyncItem);
        /* Stop heartbeat. */
        final MyMenuItem stopHeartbeatItem =
                menuFactory.createMenuItem(Tools.getString("PcmkMultiSelectionInfo.StopHeartbeat"),
                        HostInfo.HOST_STOP_COMM_LAYER_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED),
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                for (final HostInfo hi : selectedHostInfos) {
                                    if (hi.getHost().getHostParser().isHeartbeatRunning()) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                if (application.confirmDialog(
                                        Tools.getString("HostInfo.confirmHeartbeatStop.Title"),
                                        Tools.getString("HostInfo.confirmHeartbeatStop.Desc"),
                                        Tools.getString("HostInfo.confirmHeartbeatStop.Yes"),
                                        Tools.getString("HostInfo.confirmHeartbeatStop.No"))) {
                                    for (final HostInfo hi : selectedHostInfos) {
                                        hi.getHost().getHostParser().setCommLayerStopping(true);
                                    }
                                    for (final HostInfo hi : selectedHostInfos) {
                                        final Host host = hi.getHost();
                                        Heartbeat.stopHeartbeat(host);
                                    }
                                    for (final HostInfo hi : selectedHostInfos) {
                                        getBrowser().updateHWInfo(hi.getHost(), !Host.UPDATE_LVM);
                                    }
                                }
                            }
                        });
        final ButtonCallback stopHeartbeatItemCallback =
                getBrowser().new ClMenuItemCallback(getBrowser().getDCHost())
                        .addAction(new CallbackAction() {
                            @Override
                            public void run(final Host dcHost) {
                                for (final HostInfo hi : selectedHostInfos) {
                                    if (!hi.isStandby(Application.RunMode.LIVE)) {
                                        CRM.standByOn(dcHost, hi.getHost(), Application.RunMode.TEST);
                                    }
                                }
                            }
                        });
        pcmkMultiSelectionInfo.addMouseOverListener(stopHeartbeatItem, stopHeartbeatItemCallback);
        items.add(stopHeartbeatItem);
        /* Start corosync. */
        final MyMenuItem startCorosyncItem =
                menuFactory.createMenuItem(
                        Tools.getString("PcmkMultiSelectionInfo.StartCorosync"),
                        HostInfo.HOST_START_COMM_LAYER_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                for (final HostInfo hi : selectedHostInfos) {
                                    final HostParser h = hi.getHost().getHostParser();
                                    if (h.isCorosyncInstalled()
                                            && h.hasCorosyncInitScript()
                                            && h.corosyncOrOpenaisConfigExists()
                                            && !h.isCorosyncRunning()
                                            && !h.isOpenaisRunning()
                                            && !h.isHeartbeatRunning()
                                            && !h.isHeartbeatInRc()) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                for (final HostInfo hi : selectedHostInfos) {
                                    final HostParser hostParser = hi.getHost().getHostParser();
                                    if (hostParser.isOpenaisInRc() && !hostParser.isCorosyncInRc()) {
                                        return "Openais is in rc.d";
                                    }
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                for (final HostInfo hi : selectedHostInfos) {
                                    hi.getHost().getHostParser().setCommLayerStarting(true);
                                }
                                for (final HostInfo hi : selectedHostInfos) {
                                    final Host h = hi.getHost();
                                    if (h.getHostParser().isPacemakerInRc()) {
                                        Corosync.startCorosyncWithPcmk(h);
                                    } else {
                                        Corosync.startCorosync(h);
                                    }
                                }
                                for (final HostInfo hi : selectedHostInfos) {
                                    getBrowser().updateHWInfo(hi.getHost(), !Host.UPDATE_LVM);
                                }
                            }
                        });
        final ButtonCallback startCorosyncItemCallback =
                getBrowser().new ClMenuItemCallback(getBrowser().getDCHost())
                        .addAction(new CallbackAction() {
                            @Override
                            public void run(final Host dcHost) {
                                //TODO
                            }
                        });
        pcmkMultiSelectionInfo.addMouseOverListener(startCorosyncItem, startCorosyncItemCallback);
        items.add(startCorosyncItem);
        /* Start openais. */
        final MyMenuItem startOpenaisItem =
                menuFactory.createMenuItem(Tools.getString("PcmkMultiSelectionInfo.StartOpenais"),
                        HostInfo.HOST_START_COMM_LAYER_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                for (final HostInfo hi : selectedHostInfos) {
                                    val hostParser = hi.getHost().getHostParser();
                                    if (hostParser.hasOpenaisInitScript()
                                            && hostParser.corosyncOrOpenaisConfigExists()
                                            && !hostParser.isCorosyncRunning()
                                            && !hostParser.isOpenaisRunning()
                                            && !hostParser.isHeartbeatRunning()
                                            && !hostParser.isHeartbeatInRc()) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                for (final HostInfo hi : selectedHostInfos) {
                                    final Host h = hi.getHost();
                                    if (h.getHostParser().isCorosyncInRc() && !h.getHostParser().isOpenaisInRc()) {
                                        return "Corosync is in rc.d";
                                    }
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                for (final HostInfo hi : selectedHostInfos) {
                                    hi.getHost().getHostParser().setCommLayerStarting(true);
                                    Openais.startOpenais(hi.getHost());
                                }
                                for (final HostInfo hi : selectedHostInfos) {
                                    getBrowser().updateHWInfo(hi.getHost(), !Host.UPDATE_LVM);
                                }
                            }
                        });
        final ButtonCallback startOpenaisItemCallback =
                getBrowser().new ClMenuItemCallback(getBrowser().getDCHost())
                        .addAction(new CallbackAction() {
                            @Override
                            public void run(final Host dcHost) {
                                //TODO
                            }
                        });
        pcmkMultiSelectionInfo.addMouseOverListener(startOpenaisItem, startOpenaisItemCallback);
        items.add(startOpenaisItem);
        /* Start heartbeat. */
        final MyMenuItem startHeartbeatItem =
                menuFactory.createMenuItem(
                        Tools.getString("PcmkMultiSelectionInfo.StartHeartbeat"),
                        HostInfo.HOST_START_COMM_LAYER_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                for (final HostInfo hi : selectedHostInfos) {
                                    val hostParser = hi.getHost().getHostParser();
                                    if (hostParser.hasHeartbeatInitScript()
                                            && hostParser.heartbeatConfigExists()
                                            && !hostParser.isCorosyncRunning()
                                            && !hostParser.isOpenaisRunning()
                                            && !hostParser.isHeartbeatRunning()) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                for (final HostInfo hi : selectedHostInfos) {
                                    hi.getHost().getHostParser().setCommLayerStarting(true);
                                }
                                for (final HostInfo hi : selectedHostInfos) {
                                    Heartbeat.startHeartbeat(hi.getHost());
                                }
                                for (final HostInfo hi : selectedHostInfos) {
                                    getBrowser().updateHWInfo(hi.getHost(), !Host.UPDATE_LVM);
                                }
                            }
                        });
        final ButtonCallback startHeartbeatItemCallback =
                getBrowser().new ClMenuItemCallback(getBrowser().getDCHost())
                        .addAction(new CallbackAction() {
                            @Override
                            public void run(final Host dcHost) {
                                //TODO
                            }
                        });
        pcmkMultiSelectionInfo.addMouseOverListener(startHeartbeatItem, startHeartbeatItemCallback);
        items.add(startHeartbeatItem);

        /* Start pacemaker. */
        final MyMenuItem startPcmkItem =
                menuFactory.createMenuItem(
                        Tools.getString("PcmkMultiSelectionInfo.StartPacemaker"),
                        HostInfo.HOST_START_COMM_LAYER_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                for (final HostInfo hi : selectedHostInfos) {
                                    val hostParser = hi.getHost().getHostParser();
                                    if (!hostParser.isPcmkStartedByCorosync()
                                            && !hostParser.isPacemakerRunning()
                                            && (hostParser.isCorosyncRunning() || hostParser.isOpenaisRunning())
                                            && !hostParser.isHeartbeatRunning()) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                for (final HostInfo hi : selectedHostInfos) {
                                    Corosync.startPacemaker(hi.getHost());
                                }
                                for (final HostInfo hi : selectedHostInfos) {
                                    getBrowser().updateHWInfo(hi.getHost(), !Host.UPDATE_LVM);
                                }
                            }
                        });
        final ButtonCallback startPcmkItemCallback =
                getBrowser().new ClMenuItemCallback(getBrowser().getDCHost())
                        .addAction(new CallbackAction() {
                            @Override
                            public void run(final Host dcHost) {
                                //TODO
                            }
                        });
        pcmkMultiSelectionInfo.addMouseOverListener(startPcmkItem, startPcmkItemCallback);
        items.add(startPcmkItem);
        /* change host color */
        final UpdatableItem changeHostColorItem =
                menuFactory.createMenuItem(
                        Tools.getString("PcmkMultiSelectionInfo.ChangeHostColor"),
                        null,
                        "",
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL))
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                final Host firstHost = selectedHostInfos.get(0).getHost();
                                final Color newColor = JColorChooser.showDialog(
                                        mainData.getMainFrame(),
                                        "Choose " + selectedHostInfos + " color",
                                        firstHost.getPmColors()[0]);
                                for (final HostInfo hi : selectedHostInfos) {
                                    if (newColor != null) {
                                        hi.getHost().setSavedHostColorInGraphs(newColor);
                                    }
                                }
                            }
                        });
        items.add(changeHostColorItem);
    }

    /**
     * Create menu items for selected services.
     */
    private void createSelectedServicesPopup(final Iterable<ServiceInfo> selectedServiceInfos,
                                             final Collection<UpdatableItem> items) {
        /* start resources */
        final ComponentWithTest startMenuItem =
                menuFactory.createMenuItem(Tools.getString("PcmkMultiSelectionInfo.StartSelectedResources"),
                        ServiceInfo.START_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (getBrowser().crmStatusFailed()) {
                                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                                }
                                boolean allStarted = true;
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    if (!si.isStarted(Application.RunMode.LIVE)) {
                                        allStarted = false;
                                    }
                                    final String avail = si.getService().isAvailableWithText();
                                    if (avail != null) {
                                        return avail;
                                    }
                                }
                                if (allStarted) {
                                    return Tools.getString("ServiceInfo.AlreadyStarted");
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                pcmkMultiSelectionInfo.hidePopup();
                                final Host dcHost = getBrowser().getDCHost();
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    si.startResource(dcHost, Application.RunMode.LIVE);
                                }
                            }
                        });
        final ButtonCallback startItemCallback = getBrowser().new ClMenuItemCallback(null)
                .addAction(new CallbackAction() {
                    @Override
                    public void run(final Host dcHost) {
                        for (final ServiceInfo si : selectedServiceInfos) {
                            if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                continue;
                            }
                            si.startResource(dcHost, Application.RunMode.TEST);
                        }
                    }
                });
        pcmkMultiSelectionInfo.addMouseOverListener(startMenuItem, startItemCallback);
        items.add((UpdatableItem) startMenuItem);
        /* stop resources */
        final ComponentWithTest stopMenuItem =
                menuFactory.createMenuItem(Tools.getString("PcmkMultiSelectionInfo.StopSelectedResources"),
                        ServiceInfo.STOP_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (getBrowser().crmStatusFailed()) {
                                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                                }
                                boolean allStopped = true;
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    if (!si.isStopped(Application.RunMode.LIVE)) {
                                        allStopped = false;
                                    }
                                    final String avail = si.getService().isAvailableWithText();
                                    if (avail != null) {
                                        return avail;
                                    }
                                }
                                if (allStopped) {
                                    return Tools.getString("ServiceInfo.AlreadyStopped");
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                pcmkMultiSelectionInfo.hidePopup();
                                final Host dcHost = getBrowser().getDCHost();
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    si.stopResource(dcHost, Application.RunMode.LIVE);
                                }
                            }
                        });
        final ButtonCallback stopItemCallback = getBrowser().new ClMenuItemCallback(null)
                .addAction(new CallbackAction() {
                    @Override
                    public void run(final Host dcHost) {
                        for (final ServiceInfo si : selectedServiceInfos) {
                            if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                continue;
                            }
                            si.stopResource(dcHost, Application.RunMode.TEST);
                        }
                    }
                });
        pcmkMultiSelectionInfo.addMouseOverListener(stopMenuItem, stopItemCallback);
        items.add((UpdatableItem) stopMenuItem);

        /* clean up resource */
        final UpdatableItem cleanupMenuItem =
                menuFactory.createMenuItem(
                        Tools.getString("PcmkMultiSelectionInfo.CleanUpFailedResource"),
                        ServiceInfo.SERVICE_RUNNING_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        Tools.getString("PcmkMultiSelectionInfo.CleanUpResource"),
                        ServiceInfo.SERVICE_RUNNING_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .predicate(new Predicate() {
                            @Override
                            public boolean check() {
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.getService().isAvailable() && si.isOneFailed(Application.RunMode.LIVE)) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (getBrowser().crmStatusFailed()) {
                                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                                }
                                boolean failCount = false;
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    if (si.isOneFailedCount(Application.RunMode.LIVE)) {
                                        failCount = true;
                                    }
                                }
                                if (!failCount) {
                                    return "no fail count";
                                }
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    final String avail = si.getService().isAvailableWithText();
                                    if (avail != null) {
                                        return avail;
                                    }
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                pcmkMultiSelectionInfo.hidePopup();
                                final Host dcHost = getBrowser().getDCHost();
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    si.cleanupResource(dcHost, Application.RunMode.LIVE);
                                }
                            }
                        });
        /* cleanup ignores CIB_file */
        items.add(cleanupMenuItem);

        /* manage resource */
        final ComponentWithTest manageMenuItem =
                menuFactory.createMenuItem(
                        Tools.getString("PcmkMultiSelectionInfo.ManageResource"),
                        ServiceInfo.MANAGE_BY_CRM_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    if (!si.isManaged(Application.RunMode.LIVE)) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (getBrowser().crmStatusFailed()) {
                                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                                }
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    final String avail = si.getService().isAvailableWithText();
                                    if (avail != null) {
                                        return avail;
                                    }
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                pcmkMultiSelectionInfo.hidePopup();
                                final Host dcHost = getBrowser().getDCHost();
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    si.setManaged(true, dcHost, Application.RunMode.LIVE);
                                }
                            }
                        });
        final ButtonCallback manageItemCallback = getBrowser().new ClMenuItemCallback(null)
                .addAction(new CallbackAction() {
                    @Override
                    public void run(final Host dcHost) {
                        for (final ServiceInfo si : selectedServiceInfos) {
                            if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                continue;
                            }
                            si.setManaged(true, dcHost, Application.RunMode.TEST);
                        }
                    }
                });
        pcmkMultiSelectionInfo.addMouseOverListener(manageMenuItem, manageItemCallback);
        items.add((UpdatableItem) manageMenuItem);
        /* unmanage resource */
        final ComponentWithTest unmanageMenuItem =
                menuFactory.createMenuItem(
                        Tools.getString("PcmkMultiSelectionInfo.UnmanageResource"),
                        ServiceInfo.UNMANAGE_BY_CRM_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    if (si.isManaged(Application.RunMode.LIVE)) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (getBrowser().crmStatusFailed()) {
                                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                                }
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    final String avail = si.getService().isAvailableWithText();
                                    if (avail != null) {
                                        return avail;
                                    }
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                pcmkMultiSelectionInfo.hidePopup();
                                final Host dcHost = getBrowser().getDCHost();
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    si.setManaged(false, dcHost, Application.RunMode.LIVE);
                                }
                            }
                        });
        final ButtonCallback unmanageItemCallback = getBrowser().new ClMenuItemCallback(null)
                .addAction(new CallbackAction() {
                    @Override
                    public void run(final Host dcHost) {
                        for (final ServiceInfo si : selectedServiceInfos) {
                            if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                continue;
                            }
                            si.setManaged(false, dcHost, Application.RunMode.TEST);
                        }
                    }
                });
        pcmkMultiSelectionInfo.addMouseOverListener(unmanageMenuItem, unmanageItemCallback);
        items.add((UpdatableItem) unmanageMenuItem);
        /* migrate resource */
        for (final Host host : getBrowser().getClusterHosts()) {
            final String hostName = host.getName();
            final MyMenuItem migrateFromMenuItem =
                    menuFactory.createMenuItem(Tools.getString("PcmkMultiSelectionInfo.MigrateFromResource")
                                    + ' ' + hostName,
                            ServiceInfo.MIGRATE_ICON,
                            ClusterBrowser.STARTING_PTEST_TOOLTIP,

                            Tools.getString("PcmkMultiSelectionInfo.MigrateFromResource")
                                    + ' ' + hostName + " (offline)",
                            ServiceInfo.MIGRATE_ICON,
                            ClusterBrowser.STARTING_PTEST_TOOLTIP,
                            new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                            new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                            .predicate(new Predicate() {
                                @Override
                                public boolean check() {
                                    return host.isCrmStatusOk();
                                }
                            })
                            .visiblePredicate(new VisiblePredicate() {
                                @Override
                                public boolean check() {
                                    if (!host.isCrmStatusOk()) {
                                        return false;
                                    }
                                    for (final ServiceInfo si : selectedServiceInfos) {
                                        if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                            continue;
                                        }
                                        if (getBrowser().crmStatusFailed() || !host.isCrmStatusOk()) {
                                            return false;
                                        }
                                        final List<String> runningOnNodes = si.getRunningOnNodes(Application.RunMode.LIVE);
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
                                        if (si.getService().isAvailable() && runningOnNode) {
                                        } else {
                                            return false;
                                        }
                                    }
                                    return true;
                                }
                            })
                            .addAction(new MenuAction() {
                                @Override
                                public void run(final String text) {
                                    pcmkMultiSelectionInfo.hidePopup();
                                    final Host dcHost = getBrowser().getDCHost();
                                    for (final ServiceInfo si : selectedServiceInfos) {
                                        if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                            continue;
                                        }
                                        si.migrateFromResource(dcHost, hostName, Application.RunMode.LIVE);
                                    }
                                }
                            });
            final ButtonCallback migrateItemCallback = getBrowser().new ClMenuItemCallback(null)
                    .addAction(new CallbackAction() {
                        @Override
                        public void run(final Host dcHost) {
                            for (final ServiceInfo si : selectedServiceInfos) {
                                if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                    continue;
                                }
                                si.migrateFromResource(dcHost, hostName, Application.RunMode.TEST);
                            }
                        }
                    });
            pcmkMultiSelectionInfo.addMouseOverListener(migrateFromMenuItem, migrateItemCallback);
            items.add(migrateFromMenuItem);
        }

        /* unmigrate resource */
        final ComponentWithTest unmigrateMenuItem =
                menuFactory.createMenuItem(
                        Tools.getString("PcmkMultiSelectionInfo.UnmigrateResource"),
                        ServiceInfo.UNMIGRATE_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    if (!getBrowser().crmStatusFailed()
                                            && si.getService().isAvailable()
                                            && (si.getMigratedTo(Application.RunMode.LIVE) != null
                                            || si.getMigratedFrom(Application.RunMode.LIVE) != null)) {
                                    } else {
                                        return false;
                                    }
                                }
                                return true;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                pcmkMultiSelectionInfo.hidePopup();
                                final Host dcHost = getBrowser().getDCHost();
                                for (final ServiceInfo si : selectedServiceInfos) {
                                    if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                        continue;
                                    }
                                    si.unmigrateResource(dcHost, Application.RunMode.LIVE);
                                }
                            }
                        });
        final ButtonCallback unmigrateItemCallback = getBrowser().new ClMenuItemCallback(null)
                .addAction(new CallbackAction() {
                    @Override
                    public void run(final Host dcHost) {
                        for (final ServiceInfo si : selectedServiceInfos) {
                            if (si.isConstraintPlaceholder() || si.getService().isNew() || si.getService().isOrphaned()) {
                                continue;
                            }
                            si.unmigrateResource(dcHost, Application.RunMode.TEST);
                        }
                    }
                });
        pcmkMultiSelectionInfo.addMouseOverListener(unmigrateMenuItem, unmigrateItemCallback);
        items.add((UpdatableItem) unmigrateMenuItem);
        /* remove service */
        final ComponentWithTest removeMenuItem = menuFactory.createMenuItem(
                Tools.getString("PcmkMultiSelectionInfo.RemoveService"),
                ClusterBrowser.REMOVE_ICON,
                ClusterBrowser.STARTING_PTEST_TOOLTIP,
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (getBrowser().crmStatusFailed()) {
                            return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                        }
                        final ClusterStatus cs = getBrowser().getClusterStatus();
                        for (final ServiceInfo si : selectedServiceInfos) {
                            if (si.getService().isNew()) {
                                continue;
                            } else if (si.getService().isRemoved()) {
                                return ServiceInfo.IS_BEING_REMOVED_STRING;
                            } else if (si.isRunning(Application.RunMode.LIVE) && !access.isAdvancedMode()) {
                                return "cannot remove running resource<br>(advanced mode only)";
                            }
                            final GroupInfo gi = si.getGroupInfo();
                            if (gi == null) {
                                continue;
                            }
                            val groupResources = cs.getGroupResources(
                                    gi.getHeartbeatId(Application.RunMode.LIVE),
                                    Application.RunMode.LIVE);


                            if (!groupResources.isPresent() || groupResources.get().size() <= 1) {
                                return "you can remove the group";
                            }
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        pcmkMultiSelectionInfo.hidePopup();
                        if (!application.confirmDialog(
                                Tools.getString("PcmkMultiSelectionInfo.confirmRemove.Title"),
                                Tools.getString("PcmkMultiSelectionInfo.confirmRemove.Desc"),
                                Tools.getString("PcmkMultiSelectionInfo.confirmRemove.Yes"),
                                Tools.getString("PcmkMultiSelectionInfo.confirmRemove.No"))) {
                            return;
                        }
                        final Host dcHost = getBrowser().getDCHost();
                        for (ServiceInfo si : selectedServiceInfos) {
                            final ServiceInfo cs = si.getContainedService();
                            if (cs != null) {
                                si = cs;
                            }
                            if (si.getService().isOrphaned()) {
                                si.cleanupResource(dcHost, Application.RunMode.LIVE);
                            } else {
                                si.removeMyselfNoConfirm(dcHost, Application.RunMode.LIVE);
                            }
                        }
                        getBrowser().getCrmGraph().repaint();
                    }
                });
        final ClusterBrowser.ClMenuItemCallback removeItemCallback = getBrowser().new ClMenuItemCallback(null) {

            public boolean isEnabled() {
                if (!super.isEnabled()) {
                    return false;
                }
                for (final ServiceInfo si : selectedServiceInfos) {
                    if (si.getService().isNew()) {
                        return false;
                    }
                }
                return true;
            }
        };
        removeItemCallback.addAction(new CallbackAction() {
            @Override
            public void run(final Host dcHost) {
                for (final ServiceInfo si : selectedServiceInfos) {
                    si.removeMyselfNoConfirm(dcHost, Application.RunMode.TEST);
                }
            }
        });
        pcmkMultiSelectionInfo.addMouseOverListener(removeMenuItem, removeItemCallback);
        items.add((UpdatableItem) removeMenuItem);
    }

    private ClusterBrowser getBrowser() {
        return pcmkMultiSelectionInfo.getBrowser();
    }
}
