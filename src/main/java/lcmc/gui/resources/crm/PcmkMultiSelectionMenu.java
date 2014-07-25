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

package lcmc.gui.resources.crm;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.swing.JColorChooser;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.crm.ClusterStatus;
import lcmc.model.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.resources.Info;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.CRM;
import lcmc.utilities.ComponentWithTest;
import lcmc.utilities.Corosync;
import lcmc.utilities.Heartbeat;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Openais;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class PcmkMultiSelectionMenu {
    private final PcmkMultiSelectionInfo pcmkMultiSelectionInfo;

    public PcmkMultiSelectionMenu(
                        final PcmkMultiSelectionInfo pcmkMultiSelectionInfo) {
        this.pcmkMultiSelectionInfo = pcmkMultiSelectionInfo;
    }

    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final Collection<ServiceInfo> selectedServiceInfos =
                                                 new ArrayList<ServiceInfo>();
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

    /** Create menu items for selected hosts. */
    private void createSelectedHostsPopup(
                                        final List<HostInfo> selectedHostInfos,
                                        final Collection<UpdatableItem> items) {
        /* cluster manager standby on */
        final MyMenuItem standbyItem =
            new MyMenuItem(Tools.getString("PcmkMultiSelectionInfo.StandByOn"),
                           HostInfo.HOST_STANDBY_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final HostInfo hi : selectedHostInfos) {
                        if (!hi.isStandby(Application.RunMode.LIVE)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    final Host dcHost = getBrowser().getDCHost();
                    if (!dcHost.isCrmStatusOk()) {
                        return HostInfo.NO_PCMK_STATUS_STRING + " ("
                               + dcHost.getName() + ')';
                    }
                    return null;
                }

                @Override
                public void action() {
                    final Host dcHost = getBrowser().getDCHost();
                    for (final HostInfo hi : selectedHostInfos) {
                        if (!hi.isStandby(Application.RunMode.LIVE)) {
                            CRM.standByOn(dcHost,
                                          hi.getHost(),
                                          Application.RunMode.LIVE);
                        }
                    }
                }
            };
        final ButtonCallback standbyItemCallback =
                                   getBrowser().new ClMenuItemCallback(
                                                    getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final HostInfo hi : selectedHostInfos) {
                    if (!hi.isStandby(Application.RunMode.LIVE)) {
                        CRM.standByOn(dcHost, hi.getHost(), Application.RunMode.TEST);
                    }
                }
            }
        };
        pcmkMultiSelectionInfo.addMouseOverListener(standbyItem, standbyItemCallback);
        items.add(standbyItem);

        /* cluster manager standby off */
        final MyMenuItem onlineItem =
            new MyMenuItem(Tools.getString("PcmkMultiSelectionInfo.StandByOff"),
                           HostInfo.HOST_STANDBY_OFF_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final HostInfo hi : selectedHostInfos) {
                        if (hi.isStandby(Application.RunMode.LIVE)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    final Host dcHost = getBrowser().getDCHost();
                    if (!dcHost.isCrmStatusOk()) {
                        return HostInfo.NO_PCMK_STATUS_STRING + " ("
                               + dcHost.getName() + ')';
                    }
                    return null;
                }

                @Override
                public void action() {
                    final Host dcHost = getBrowser().getDCHost();
                    for (final HostInfo hi : selectedHostInfos) {
                        if (hi.isStandby(Application.RunMode.LIVE)) {
                            CRM.standByOff(dcHost,
                                           hi.getHost(),
                                           Application.RunMode.LIVE);
                        }
                    }
                }
            };
        final ButtonCallback onlineItemCallback =
                                   getBrowser().new ClMenuItemCallback(
                                                    getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final HostInfo hi : selectedHostInfos) {
                    if (hi.isStandby(Application.RunMode.LIVE)) {
                        CRM.standByOff(dcHost, hi.getHost(), Application.RunMode.TEST);
                    }
                }
            }
        };
        pcmkMultiSelectionInfo.addMouseOverListener(onlineItem, onlineItemCallback);
        items.add(onlineItem);

        /* Stop corosync/openais. */
        final MyMenuItem stopCorosyncItem =
          new MyMenuItem(Tools.getString("PcmkMultiSelectionInfo.StopCorosync"),
                         HostInfo.HOST_STOP_COMM_LAYER_ICON,
                         ClusterBrowser.STARTING_PTEST_TOOLTIP,

                         Tools.getString("PcmkMultiSelectionInfo.StopOpenais"),
                         HostInfo.HOST_STOP_COMM_LAYER_ICON,
                         ClusterBrowser.STARTING_PTEST_TOOLTIP,

                         new AccessMode(Application.AccessType.ADMIN, true),
                         new AccessMode(Application.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    /* when both are running it's openais. */
                    final HostInfo hi = selectedHostInfos.get(0);
                    return hi.getHost().isCorosyncRunning()
                           && !hi.getHost().isOpenaisRunning();
                }

                @Override
                public boolean visiblePredicate() {
                    for (final HostInfo hi : selectedHostInfos) {
                        if (hi.getHost().isCorosyncRunning()
                            || hi.getHost().isOpenaisRunning()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public void action() {
                    if (Tools.confirmDialog(
                         Tools.getString("HostInfo.confirmCorosyncStop.Title"),
                         Tools.getString("HostInfo.confirmCorosyncStop.Desc"),
                         Tools.getString("HostInfo.confirmCorosyncStop.Yes"),
                         Tools.getString("HostInfo.confirmCorosyncStop.No"))) {
                        for (final HostInfo hi : selectedHostInfos) {
                            hi.getHost().setCommLayerStopping(true);
                        }
                        for (final HostInfo hi : selectedHostInfos) {
                            final Host host = hi.getHost();
                            if (!host.isPcmkStartedByCorosync()
                                && host.hasPacemakerInitScript()
                                && host.isPacemakerRunning()) {
                                if (host.isCorosyncRunning()
                                    && !host.isOpenaisRunning()) {
                                    Corosync.stopCorosyncWithPcmk(host);
                                } else {
                                    Openais.stopOpenaisWithPcmk(host);
                                }
                            } else {
                                if (host.isCorosyncRunning()
                                    && !host.isOpenaisRunning()) {
                                    Corosync.stopCorosync(host);
                                } else {
                                    Openais.stopOpenais(host);
                                }
                            }
                        }

                        for (final HostInfo hi : selectedHostInfos) {
                            getBrowser().updateHWInfo(hi.getHost(),
                                                      !Host.UPDATE_LVM);
                        }
                    }
                }
            };
        final ButtonCallback stopCorosyncItemCallback =
                    getBrowser().new ClMenuItemCallback(
                                                    getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final HostInfo hi : selectedHostInfos) {
                    if (!hi.isStandby(Application.RunMode.LIVE)) {
                        CRM.standByOn(dcHost, hi.getHost(), Application.RunMode.TEST);
                    }
                }
            }
        };
        pcmkMultiSelectionInfo.addMouseOverListener(stopCorosyncItem,
                                                    stopCorosyncItemCallback);
        items.add(stopCorosyncItem);
        /* Stop heartbeat. */
        final MyMenuItem stopHeartbeatItem =
         new MyMenuItem(Tools.getString("PcmkMultiSelectionInfo.StopHeartbeat"),
                        HostInfo.HOST_STOP_COMM_LAYER_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        new AccessMode(Application.AccessType.ADMIN, true),
                        new AccessMode(Application.AccessType.ADMIN, false)) {
             private static final long serialVersionUID = 1L;

             @Override
             public boolean visiblePredicate() {
                 for (final HostInfo hi : selectedHostInfos) {
                     if (hi.getHost().isHeartbeatRunning()) {
                         return true;
                     }
                 }
                 return false;
             }

             @Override
             public void action() {
                 if (Tools.confirmDialog(
                      Tools.getString("HostInfo.confirmHeartbeatStop.Title"),
                      Tools.getString("HostInfo.confirmHeartbeatStop.Desc"),
                      Tools.getString("HostInfo.confirmHeartbeatStop.Yes"),
                      Tools.getString("HostInfo.confirmHeartbeatStop.No"))) {
                     for (final HostInfo hi : selectedHostInfos) {
                         hi.getHost().setCommLayerStopping(true);
                     }
                     for (final HostInfo hi : selectedHostInfos) {
                         final Host host = hi.getHost();
                         Heartbeat.stopHeartbeat(host);
                     }
                     for (final HostInfo hi : selectedHostInfos) {
                         getBrowser().updateHWInfo(hi.getHost(),
                                                   !Host.UPDATE_LVM);
                     }
                 }
             }
         };
        final ButtonCallback stopHeartbeatItemCallback =
                getBrowser().new ClMenuItemCallback(getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final HostInfo hi : selectedHostInfos) {
                    if (!hi.isStandby(Application.RunMode.LIVE)) {
                        CRM.standByOn(dcHost, hi.getHost(), Application.RunMode.TEST);
                    }
                }
            }
        };
        pcmkMultiSelectionInfo.addMouseOverListener(stopHeartbeatItem,
                                                    stopHeartbeatItemCallback);
        items.add(stopHeartbeatItem);
        /* Start corosync. */
        final MyMenuItem startCorosyncItem =
            new MyMenuItem(
                        Tools.getString("PcmkMultiSelectionInfo.StartCorosync"),
                        HostInfo.HOST_START_COMM_LAYER_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final HostInfo hi : selectedHostInfos) {
                        final Host h = hi.getHost();
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

                @Override
                public String enablePredicate() {
                    for (final HostInfo hi : selectedHostInfos) {
                        final Host h = hi.getHost();
                        if (h.isOpenaisInRc() && !h.isCorosyncInRc()) {
                            return "Openais is in rc.d";
                        }
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final HostInfo hi : selectedHostInfos) {
                        hi.getHost().setCommLayerStarting(true);
                    }
                    for (final HostInfo hi : selectedHostInfos) {
                        final Host h = hi.getHost();
                        if (h.isPacemakerInRc()) {
                            Corosync.startCorosyncWithPcmk(h);
                        } else {
                            Corosync.startCorosync(h);
                        }
                    }
                    for (final HostInfo hi : selectedHostInfos) {
                        getBrowser().updateHWInfo(hi.getHost(),
                                                  !Host.UPDATE_LVM);
                    }
                }
            };
        final ButtonCallback startCorosyncItemCallback =
                getBrowser().new ClMenuItemCallback(getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                //TODO
            }
        };
        pcmkMultiSelectionInfo.addMouseOverListener(startCorosyncItem,
                                                    startCorosyncItemCallback);
        items.add(startCorosyncItem);
        /* Start openais. */
        final MyMenuItem startOpenaisItem =
          new MyMenuItem(Tools.getString("PcmkMultiSelectionInfo.StartOpenais"),
                         HostInfo.HOST_START_COMM_LAYER_ICON,
                         ClusterBrowser.STARTING_PTEST_TOOLTIP,

                         new AccessMode(Application.AccessType.ADMIN, false),
                         new AccessMode(Application.AccessType.ADMIN, false)) {
              private static final long serialVersionUID = 1L;

              @Override
              public boolean visiblePredicate() {
                  for (final HostInfo hi : selectedHostInfos) {
                      final Host h = hi.getHost();
                      if (h.hasOpenaisInitScript()
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

              @Override
              public String enablePredicate() {
                  for (final HostInfo hi : selectedHostInfos) {
                      final Host h = hi.getHost();
                      if (h.isCorosyncInRc() && !h.isOpenaisInRc()) {
                          return "Corosync is in rc.d";
                      }
                  }
                  return null;
              }

              @Override
              public void action() {
                  for (final HostInfo hi : selectedHostInfos) {
                      hi.getHost().setCommLayerStarting(true);
                      Openais.startOpenais(hi.getHost());
                  }
                  for (final HostInfo hi : selectedHostInfos) {
                      getBrowser().updateHWInfo(hi.getHost(),
                                                !Host.UPDATE_LVM);
                  }
              }
          };
        final ButtonCallback startOpenaisItemCallback =
                getBrowser().new ClMenuItemCallback(getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                //TODO
            }
        };
        pcmkMultiSelectionInfo.addMouseOverListener(startOpenaisItem,
                                                    startOpenaisItemCallback);
        items.add(startOpenaisItem);
        /* Start heartbeat. */
        final MyMenuItem startHeartbeatItem =
            new MyMenuItem(
                      Tools.getString("PcmkMultiSelectionInfo.StartHeartbeat"),
                      HostInfo.HOST_START_COMM_LAYER_ICON,
                      ClusterBrowser.STARTING_PTEST_TOOLTIP,

                      new AccessMode(Application.AccessType.ADMIN, false),
                      new AccessMode(Application.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final HostInfo hi : selectedHostInfos) {
                        final Host h = hi.getHost();
                        if (h.hasHeartbeatInitScript()
                            && h.heartbeatConfigExists()
                            && !h.isCorosyncRunning()
                            && !h.isOpenaisRunning()
                            && !h.isHeartbeatRunning()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public void action() {
                    for (final HostInfo hi : selectedHostInfos) {
                        hi.getHost().setCommLayerStarting(true);
                    }
                    for (final HostInfo hi : selectedHostInfos) {
                        Heartbeat.startHeartbeat(hi.getHost());
                    }
                    for (final HostInfo hi : selectedHostInfos) {
                        getBrowser().updateHWInfo(hi.getHost(),
                                                  !Host.UPDATE_LVM);
                    }
                }
            };
        final ButtonCallback startHeartbeatItemCallback =
                getBrowser().new ClMenuItemCallback(getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                //TODO
            }
        };
        pcmkMultiSelectionInfo.addMouseOverListener(startHeartbeatItem,
                                                    startHeartbeatItemCallback);
        items.add(startHeartbeatItem);

        /* Start pacemaker. */
        final MyMenuItem startPcmkItem =
            new MyMenuItem(
                       Tools.getString("PcmkMultiSelectionInfo.StartPacemaker"),
                       HostInfo.HOST_START_COMM_LAYER_ICON,
                       ClusterBrowser.STARTING_PTEST_TOOLTIP,

                       new AccessMode(Application.AccessType.ADMIN, false),
                       new AccessMode(Application.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final HostInfo hi : selectedHostInfos) {
                        final Host h = hi.getHost();
                        if (!h.isPcmkStartedByCorosync()
                            && !h.isPacemakerRunning()
                            && (h.isCorosyncRunning()
                                || h.isOpenaisRunning())
                            && !h.isHeartbeatRunning()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public void action() {
                    for (final HostInfo hi : selectedHostInfos) {
                        Corosync.startPacemaker(hi.getHost());
                    }
                    for (final HostInfo hi : selectedHostInfos) {
                        getBrowser().updateHWInfo(hi.getHost(),
                                                  !Host.UPDATE_LVM);
                    }
                }
            };
        final ButtonCallback startPcmkItemCallback =
                getBrowser().new ClMenuItemCallback(getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                //TODO
            }
        };
        pcmkMultiSelectionInfo.addMouseOverListener(startPcmkItem,
                                                    startPcmkItemCallback);
        items.add(startPcmkItem);
        /* change host color */
        final UpdatableItem changeHostColorItem =
            new MyMenuItem(
                    Tools.getString("PcmkMultiSelectionInfo.ChangeHostColor"),
                    null,
                    "",
                    new AccessMode(Application.AccessType.RO, false),
                    new AccessMode(Application.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void action() {
                    final Host firstHost = selectedHostInfos.get(0).getHost();
                    final Color newColor = JColorChooser.showDialog(
                                            Tools.getGUIData().getMainFrame(),
                                            "Choose " + selectedHostInfos
                                            + " color",
                                            firstHost.getPmColors()[0]);
                    for (final HostInfo hi : selectedHostInfos) {
                        if (newColor != null) {
                            hi.getHost().setSavedHostColorInGraphs(newColor);
                        }
                    }
                }
            };
        items.add(changeHostColorItem);
    }

    /** Create menu items for selected services. */
    private void createSelectedServicesPopup(
                                 final Iterable<ServiceInfo> selectedServiceInfos,
                                 final Collection<UpdatableItem> items) {
        /* start resources */
        final ComponentWithTest startMenuItem =
            new MyMenuItem(Tools.getString(
                              "PcmkMultiSelectionInfo.StartSelectedResources"),
                           ServiceInfo.START_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    boolean allStarted = true;
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        if (!si.isStarted(Application.RunMode.LIVE)) {
                            allStarted = false;
                        }
                        final String avail =
                                        si.getService().isAvailableWithText();
                        if (avail != null) {
                            return avail;
                        }
                    }
                    if (allStarted) {
                        return Tools.getString("ServiceInfo.AlreadyStarted");
                    }
                    return null;
                }

                @Override
                public void action() {
                    pcmkMultiSelectionInfo.hidePopup();
                    final Host dcHost = getBrowser().getDCHost();
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        si.startResource(dcHost, Application.RunMode.LIVE);
                    }
                }
            };
        final ButtonCallback startItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                for (final ServiceInfo si : selectedServiceInfos) {
                    if (si.isConstraintPH()
                        || si.getService().isNew()
                        || si.getService().isOrphaned()) {
                        continue;
                    }
                    si.startResource(dcHost, Application.RunMode.TEST);
                }
            }
        };
        pcmkMultiSelectionInfo.addMouseOverListener(startMenuItem, startItemCallback);
        items.add((UpdatableItem) startMenuItem);
        /* stop resources */
        final ComponentWithTest stopMenuItem =
            new MyMenuItem(Tools.getString(
                                "PcmkMultiSelectionInfo.StopSelectedResources"),
                           ServiceInfo.STOP_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    boolean allStopped = true;
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        if (!si.isStopped(Application.RunMode.LIVE)) {
                            allStopped = false;
                        }
                        final String avail =
                                        si.getService().isAvailableWithText();
                        if (avail != null) {
                            return avail;
                        }
                    }
                    if (allStopped) {
                        return Tools.getString("ServiceInfo.AlreadyStopped");
                    }
                    return null;
                }

                @Override
                public void action() {
                    pcmkMultiSelectionInfo.hidePopup();
                    final Host dcHost = getBrowser().getDCHost();
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        si.stopResource(dcHost, Application.RunMode.LIVE);
                    }
                }
            };
        final ButtonCallback stopItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                for (final ServiceInfo si : selectedServiceInfos) {
                    if (si.isConstraintPH()
                        || si.getService().isNew()
                        || si.getService().isOrphaned()) {
                        continue;
                    }
                    si.stopResource(dcHost, Application.RunMode.TEST);
                }
            }
        };
        pcmkMultiSelectionInfo.addMouseOverListener(stopMenuItem, stopItemCallback);
        items.add((UpdatableItem) stopMenuItem);

        /* clean up resource */
        final UpdatableItem cleanupMenuItem =
            new MyMenuItem(
               Tools.getString("PcmkMultiSelectionInfo.CleanUpFailedResource"),
               ServiceInfo.SERVICE_RUNNING_ICON,
               ClusterBrowser.STARTING_PTEST_TOOLTIP,

               Tools.getString("PcmkMultiSelectionInfo.CleanUpResource"),
               ServiceInfo.SERVICE_RUNNING_ICON,
               ClusterBrowser.STARTING_PTEST_TOOLTIP,
               new AccessMode(Application.AccessType.OP, false),
               new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.getService().isAvailable()
                            && si.isOneFailed(Application.RunMode.LIVE)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    boolean failCount = false;
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
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
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        final String avail =
                                        si.getService().isAvailableWithText();
                        if (avail != null) {
                            return avail;
                        }
                    }
                    return null;
                }

                @Override
                public void action() {
                    pcmkMultiSelectionInfo.hidePopup();
                    final Host dcHost = getBrowser().getDCHost();
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        si.cleanupResource(dcHost, Application.RunMode.LIVE);
                    }
                }
            };
        /* cleanup ignores CIB_file */
        items.add(cleanupMenuItem);


        /* manage resource */
        final ComponentWithTest manageMenuItem =
            new MyMenuItem(
                  Tools.getString("PcmkMultiSelectionInfo.ManageResource"),
                  ServiceInfo.MANAGE_BY_CRM_ICON,
                  ClusterBrowser.STARTING_PTEST_TOOLTIP,
                  new AccessMode(Application.AccessType.OP, false),
                  new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        if (!si.isManaged(Application.RunMode.LIVE)) {
                            return true;
                        }
                    }
                    return false;
                }
                @Override
                public String enablePredicate() {
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        final String avail =
                                        si.getService().isAvailableWithText();
                        if (avail != null) {
                            return avail;
                        }
                    }
                    return null;
                }

                @Override
                public void action() {
                    pcmkMultiSelectionInfo.hidePopup();
                    final Host dcHost = getBrowser().getDCHost();
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        si.setManaged(true, dcHost, Application.RunMode.LIVE);
                    }
                }
            };
        final ButtonCallback manageItemCallback =
                                     getBrowser().new ClMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                for (final ServiceInfo si : selectedServiceInfos) {
                    if (si.isConstraintPH()
                        || si.getService().isNew()
                        || si.getService().isOrphaned()) {
                        continue;
                    }
                    si.setManaged(true, dcHost, Application.RunMode.TEST);
                }
            }
        };
        pcmkMultiSelectionInfo.addMouseOverListener(manageMenuItem, manageItemCallback);
        items.add((UpdatableItem) manageMenuItem);
        /* unmanage resource */
        final ComponentWithTest unmanageMenuItem =
            new MyMenuItem(
                  Tools.getString("PcmkMultiSelectionInfo.UnmanageResource"),
                  ServiceInfo.UNMANAGE_BY_CRM_ICON,
                  ClusterBrowser.STARTING_PTEST_TOOLTIP,

                  new AccessMode(Application.AccessType.OP, false),
                  new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        if (si.isManaged(Application.RunMode.LIVE)) {
                            return true;
                        }
                    }
                    return false;
                }
                @Override
                public String enablePredicate() {
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        final String avail =
                                        si.getService().isAvailableWithText();
                        if (avail != null) {
                            return avail;
                        }
                    }
                    return null;
                }

                @Override
                public void action() {
                    pcmkMultiSelectionInfo.hidePopup();
                    final Host dcHost = getBrowser().getDCHost();
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        si.setManaged(false, dcHost, Application.RunMode.LIVE);
                    }
                }
            };
        final ButtonCallback unmanageItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                for (final ServiceInfo si : selectedServiceInfos) {
                    if (si.isConstraintPH()
                        || si.getService().isNew()
                        || si.getService().isOrphaned()) {
                        continue;
                    }
                    si.setManaged(false, dcHost, Application.RunMode.TEST);
                }
            }
        };
        pcmkMultiSelectionInfo.addMouseOverListener(unmanageMenuItem, unmanageItemCallback);
        items.add((UpdatableItem) unmanageMenuItem);
        /* migrate resource */
        for (final Host host : getBrowser().getClusterHosts()) {
            final String hostName = host.getName();
            final MyMenuItem migrateFromMenuItem =
               new MyMenuItem(Tools.getString(
                                  "PcmkMultiSelectionInfo.MigrateFromResource")
                                  + ' ' + hostName,
                              ServiceInfo.MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,

                              Tools.getString(
                                  "PcmkMultiSelectionInfo.MigrateFromResource")
                                  + ' ' + hostName + " (offline)",
                              ServiceInfo.MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,
                              new AccessMode(Application.AccessType.OP, false),
                              new AccessMode(Application.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean predicate() {
                        return host.isCrmStatusOk();
                    }

                    @Override
                    public boolean visiblePredicate() {
                        return !host.isCrmStatusOk()
                               || enablePredicate() == null;
                    }

                    @Override
                    public String enablePredicate() {
                        for (final ServiceInfo si : selectedServiceInfos) {
                            if (si.isConstraintPH()
                                || si.getService().isNew()
                                || si.getService().isOrphaned()) {
                                continue;
                            }
                            if (getBrowser().crmStatusFailed()
                                || !host.isCrmStatusOk()) {
                                return "not available on this host";
                            }
                            final List<String> runningOnNodes =
                                                si.getRunningOnNodes(Application.RunMode.LIVE);
                            if (runningOnNodes == null
                                || runningOnNodes.size() < 1) {
                                return "must run";
                            }
                            boolean runningOnNode = false;
                            for (final String ron : runningOnNodes) {
                                if (hostName.toLowerCase(Locale.US).equals(
                                                 ron.toLowerCase(Locale.US))) {
                                    runningOnNode = true;
                                    break;
                                }
                            }
                            if (si.getService().isAvailable()
                                && runningOnNode) {
                            } else {
                                return "not available";
                            }
                        }
                        return null;
                    }

                    @Override
                    public void action() {
                        pcmkMultiSelectionInfo.hidePopup();
                        final Host dcHost = getBrowser().getDCHost();
                        for (final ServiceInfo si : selectedServiceInfos) {
                            if (si.isConstraintPH()
                                || si.getService().isNew()
                                || si.getService().isOrphaned()) {
                                continue;
                            }
                            si.migrateFromResource(dcHost, hostName, Application.RunMode.LIVE);
                        }
                    }
                };
            final ButtonCallback migrateItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
                @Override
                public void action(final Host dcHost) {
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        si.migrateFromResource(dcHost, hostName, Application.RunMode.TEST);
                    }
                }
            };
            pcmkMultiSelectionInfo.addMouseOverListener(migrateFromMenuItem, migrateItemCallback);
            items.add(migrateFromMenuItem);
        }

        /* unmigrate resource */
        final ComponentWithTest unmigrateMenuItem =
            new MyMenuItem(
                    Tools.getString("PcmkMultiSelectionInfo.UnmigrateResource"),
                    ServiceInfo.UNMIGRATE_ICON,
                    ClusterBrowser.STARTING_PTEST_TOOLTIP,
                    new AccessMode(Application.AccessType.OP, false),
                    new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return enablePredicate() == null;
                }

                @Override
                public String enablePredicate() {
                    // TODO: if it was migrated
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        if (!getBrowser().crmStatusFailed()
                             && si.getService().isAvailable()
                             && (si.getMigratedTo(Application.RunMode.LIVE) != null
                                 || si.getMigratedFrom(Application.RunMode.LIVE) != null)) {
                        } else {
                            return "not available";
                        }
                    }
                    return null;
                }

                @Override
                public void action() {
                    pcmkMultiSelectionInfo.hidePopup();
                    final Host dcHost = getBrowser().getDCHost();
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        si.unmigrateResource(dcHost, Application.RunMode.LIVE);
                    }
                }
            };
        final ButtonCallback unmigrateItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                for (final ServiceInfo si : selectedServiceInfos) {
                    if (si.isConstraintPH()
                        || si.getService().isNew()
                        || si.getService().isOrphaned()) {
                        continue;
                    }
                    si.unmigrateResource(dcHost, Application.RunMode.TEST);
                }
            }
        };
        pcmkMultiSelectionInfo.addMouseOverListener(unmigrateMenuItem, unmigrateItemCallback);
        items.add((UpdatableItem) unmigrateMenuItem);
        /* remove service */
        final ComponentWithTest removeMenuItem = new MyMenuItem(
                    Tools.getString("PcmkMultiSelectionInfo.RemoveService"),
                    ClusterBrowser.REMOVE_ICON,
                    ClusterBrowser.STARTING_PTEST_TOOLTIP,
                    new AccessMode(Application.AccessType.ADMIN, false),
                    new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getBrowser().crmStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                final ClusterStatus cs = getBrowser().getClusterStatus();
                for (final ServiceInfo si : selectedServiceInfos) {
                    if (si.getService().isNew()) {
                        continue;
                    } else if (si.getService().isRemoved()) {
                        return ServiceInfo.IS_BEING_REMOVED_STRING;
                    } else if (si.isRunning(Application.RunMode.LIVE)
                               && !Tools.getApplication().isAdvancedMode()) {
                        return "cannot remove running resource<br>"
                               + "(advanced mode only)";
                    }
                    final GroupInfo gi = si.getGroupInfo();
                    if (gi == null) {
                        continue;
                    }
                    final List<String> gr = cs.getGroupResources(
                                              gi.getHeartbeatId(Application.RunMode.LIVE),
                                              Application.RunMode.LIVE);


                    if (gr == null || gr.size() <= 1) {
                        return "you can remove the group";
                    }
                }
                return null;
            }

            @Override
            public void action() {
                pcmkMultiSelectionInfo.hidePopup();
                if (!Tools.confirmDialog(
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
        };
        final ButtonCallback removeItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
            @Override

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

            @Override
            public void action(final Host dcHost) {
                for (final ServiceInfo si : selectedServiceInfos) {
                    si.removeMyselfNoConfirm(dcHost, Application.RunMode.TEST);
                }
            }
        };
        pcmkMultiSelectionInfo.addMouseOverListener(removeMenuItem, removeItemCallback);
        items.add((UpdatableItem) removeMenuItem);
    }

    private ClusterBrowser getBrowser() {
        return pcmkMultiSelectionInfo.getBrowser();
    }
}
