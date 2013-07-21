/*
 * This file is part of Linux Cluster Management Console
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2011-2012, Rasto Levrinc
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package lcmc.gui.resources;

import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.data.Host;
import lcmc.data.ClusterStatus;
import lcmc.data.PtestData;
import lcmc.utilities.Tools;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.CRM;
import lcmc.utilities.Corosync;
import lcmc.utilities.Openais;
import lcmc.utilities.Heartbeat;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;
import javax.swing.JMenuBar;
import javax.swing.AbstractButton;
import javax.swing.JColorChooser;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * This class provides menus for service and host multi selection.
 */
public final class PcmkMultiSelectionInfo extends EditableInfo {
    /** All selected objects. */
    private final List<Info> selectedInfos;

    /** Prepares a new <code>PcmkMultiSelectionInfo</code> object. */
    public PcmkMultiSelectionInfo(final List<Info> selectedInfos,
                                  final Browser browser) {
        super("selection", browser);
        this.selectedInfos = selectedInfos;
    }

    /** @see EditableInfo#getMenuIcon() */
    @Override
    public ImageIcon getMenuIcon(final boolean testOnly) {
        return null;
    }

    /** @see EditableInfo#getInfoType() */
    @Override
    protected String getInfoType() {
        return Tools.MIME_TYPE_TEXT_HTML;
    }

    /** @see EditableInfo#getInfo() */
    @Override
    public String getInfo() {
        final StringBuilder s = new StringBuilder(80);
        s.append(Tools.getString("PcmkMultiSelectionInfo.Selection"));
        for (final Info si : selectedInfos) {
            if (si != null) {
                s.append(si.toString());
            }
            s.append("<br />");
        }
        return s.toString();
    }

    /** Create menu items for selected hosts. */
    private void createSelectedHostsPopup(
                                        final List<HostInfo> selectedHostInfos,
                                        final List<UpdatableItem> items) {
        /* cluster manager standby on */
        final int size = selectedHostInfos.size();
        final MyMenuItem standbyItem =
            new MyMenuItem(Tools.getString("PcmkMultiSelectionInfo.StandByOn"),
                           HostInfo.HOST_STANDBY_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final HostInfo hi : selectedHostInfos) {
                        if (!hi.isStandby(CRM.LIVE)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    final Host dcHost = getBrowser().getDCHost();
                    if (!dcHost.isClStatus()) {
                        return HostInfo.NO_PCMK_STATUS_STRING + " ("
                               + dcHost.getName() + ")";
                    }
                    return null;
                }

                @Override
                public void action() {
                    final Host dcHost = getBrowser().getDCHost();
                    for (final HostInfo hi : selectedHostInfos) {
                        if (!hi.isStandby(CRM.LIVE)) {
                            CRM.standByOn(dcHost,
                                          hi.getHost(),
                                          CRM.LIVE);
                        }
                    }
                }
            };
        final ClusterBrowser.ClMenuItemCallback standbyItemCallback =
                                   getBrowser().new ClMenuItemCallback(
                                       standbyItem, getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final HostInfo hi : selectedHostInfos) {
                    if (!hi.isStandby(CRM.LIVE)) {
                        CRM.standByOn(dcHost, hi.getHost(), CRM.TESTONLY);
                    }
                }
            }
        };
        addMouseOverListener(standbyItem, standbyItemCallback);
        items.add(standbyItem);

        /* cluster manager standby off */
        final MyMenuItem onlineItem =
            new MyMenuItem(Tools.getString("PcmkMultiSelectionInfo.StandByOff"),
                           HostInfo.HOST_STANDBY_OFF_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final HostInfo hi : selectedHostInfos) {
                        if (hi.isStandby(CRM.LIVE)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    final Host dcHost = getBrowser().getDCHost();
                    if (!dcHost.isClStatus()) {
                        return HostInfo.NO_PCMK_STATUS_STRING + " ("
                               + dcHost.getName() + ")";
                    }
                    return null;
                }

                @Override
                public void action() {
                    final Host dcHost = getBrowser().getDCHost();
                    for (final HostInfo hi : selectedHostInfos) {
                        if (hi.isStandby(CRM.LIVE)) {
                            CRM.standByOff(dcHost,
                                           hi.getHost(),
                                           CRM.LIVE);
                        }
                    }
                }
            };
        final ClusterBrowser.ClMenuItemCallback onlineItemCallback =
                                   getBrowser().new ClMenuItemCallback(
                                       onlineItem, getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final HostInfo hi : selectedHostInfos) {
                    if (hi.isStandby(CRM.LIVE)) {
                        CRM.standByOff(dcHost, hi.getHost(), CRM.TESTONLY);
                    }
                }
            }
        };
        addMouseOverListener(onlineItem, onlineItemCallback);
        items.add(onlineItem);

        /* Stop corosync/openais. */
        final MyMenuItem stopCorosyncItem =
          new MyMenuItem(Tools.getString("PcmkMultiSelectionInfo.StopCorosync"),
                         HostInfo.HOST_STOP_COMM_LAYER_ICON,
                         ClusterBrowser.STARTING_PTEST_TOOLTIP,

                         Tools.getString("PcmkMultiSelectionInfo.StopOpenais"),
                         HostInfo.HOST_STOP_COMM_LAYER_ICON,
                         ClusterBrowser.STARTING_PTEST_TOOLTIP,

                         new AccessMode(ConfigData.AccessType.ADMIN, true),
                         new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    /* when both are running it's openais. */
                    final HostInfo hi = selectedHostInfos.get(0);
                    return hi.getHost().isCsRunning()
                           && !hi.getHost().isAisRunning();
                }

                @Override
                public boolean visiblePredicate() {
                    for (final HostInfo hi : selectedHostInfos) {
                        if (hi.getHost().isCsRunning()
                            || hi.getHost().isAisRunning()) {
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
                                && host.isPcmkInit()
                                && host.isPcmkRunning()) {
                                Corosync.stopCorosyncWithPcmk(host);
                            } else {
                                Corosync.stopCorosync(host);
                            }
                        }

                        for (final HostInfo hi : selectedHostInfos) {
                            getBrowser().updateHWInfo(hi.getHost());
                        }
                    }
                }
            };
        final ClusterBrowser.ClMenuItemCallback stopCorosyncItemCallback =
                    getBrowser().new ClMenuItemCallback(
                                                    stopCorosyncItem,
                                                    getBrowser().getDCHost()) {
            @Override
            public void action(final Host dchost) {
                for (final HostInfo hi : selectedHostInfos) {
                    if (!hi.isStandby(CRM.LIVE)) {
                        CRM.standByOn(dchost, hi.getHost(), CRM.TESTONLY);
                    }
                }
            }
        };
        addMouseOverListener(stopCorosyncItem,
                             stopCorosyncItemCallback);
        items.add(stopCorosyncItem);
        /* Stop heartbeat. */
        final MyMenuItem stopHeartbeatItem =
         new MyMenuItem(Tools.getString("PcmkMultiSelectionInfo.StopHeartbeat"),
                        HostInfo.HOST_STOP_COMM_LAYER_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        new AccessMode(ConfigData.AccessType.ADMIN, true),
                        new AccessMode(ConfigData.AccessType.ADMIN, false)) {
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
                         getBrowser().updateHWInfo(hi.getHost());
                     }
                 }
             }
         };
        final ClusterBrowser.ClMenuItemCallback stopHeartbeatItemCallback =
                getBrowser().new ClMenuItemCallback(stopHeartbeatItem,
                                                    getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                for (final HostInfo hi : selectedHostInfos) {
                    if (!hi.isStandby(CRM.LIVE)) {
                        CRM.standByOn(dcHost, hi.getHost(), CRM.TESTONLY);
                    }
                }
            }
        };
        addMouseOverListener(stopHeartbeatItem,
                             stopHeartbeatItemCallback);
        items.add(stopHeartbeatItem);
        /* Start corosync. */
        final MyMenuItem startCorosyncItem =
            new MyMenuItem(
                        Tools.getString("PcmkMultiSelectionInfo.StartCorosync"),
                        HostInfo.HOST_START_COMM_LAYER_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        new AccessMode(ConfigData.AccessType.ADMIN, false),
                        new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final HostInfo hi : selectedHostInfos) {
                        final Host h = hi.getHost();
                        if (h.isCorosync()
                            && h.isCsInit()
                            && h.isCsAisConf()
                            && !h.isCsRunning()
                            && !h.isAisRunning()
                            && !h.isHeartbeatRunning()
                            && !h.isHeartbeatRc()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    for (final HostInfo hi : selectedHostInfos) {
                        final Host h = hi.getHost();
                        if (h.isAisRc() && !h.isCsRc()) {
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
                        if (h.isPcmkRc()) {
                            Corosync.startCorosyncWithPcmk(h);
                        } else {
                            Corosync.startCorosync(h);
                        }
                    }
                    for (final HostInfo hi : selectedHostInfos) {
                        getBrowser().updateHWInfo(hi.getHost());
                    }
                }
            };
        final ClusterBrowser.ClMenuItemCallback startCorosyncItemCallback =
                getBrowser().new ClMenuItemCallback(startCorosyncItem,
                                                    getBrowser().getDCHost()) {
            @Override
            public void action(final Host dcHost) {
                //TODO
            }
        };
        addMouseOverListener(startCorosyncItem,
                             startCorosyncItemCallback);
        items.add(startCorosyncItem);
        /* Start openais. */
        final MyMenuItem startOpenaisItem =
          new MyMenuItem(Tools.getString("PcmkMultiSelectionInfo.StartOpenais"),
                         HostInfo.HOST_START_COMM_LAYER_ICON,
                         ClusterBrowser.STARTING_PTEST_TOOLTIP,

                         new AccessMode(ConfigData.AccessType.ADMIN, false),
                         new AccessMode(ConfigData.AccessType.ADMIN, false)) {
              private static final long serialVersionUID = 1L;

              @Override
              public boolean visiblePredicate() {
                  for (final HostInfo hi : selectedHostInfos) {
                      final Host h = hi.getHost();
                      if (h.isAisInit()
                          && h.isCsAisConf()
                          && !h.isCsRunning()
                          && !h.isAisRunning()
                          && !h.isHeartbeatRunning()
                          && !h.isHeartbeatRc()) {
                          return true;
                      }
                  }
                  return false;
              }

              @Override
              public String enablePredicate() {
                  for (final HostInfo hi : selectedHostInfos) {
                      final Host h = hi.getHost();
                      if (h.isCsRc() && !h.isAisRc()) {
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
                      getBrowser().updateHWInfo(hi.getHost());
                  }
              }
          };
        final ClusterBrowser.ClMenuItemCallback startOpenaisItemCallback =
                getBrowser().new ClMenuItemCallback(startOpenaisItem,
                                                    getBrowser().getDCHost()) {
            @Override
            public void action(final Host host) {
                //TODO
            }
        };
        addMouseOverListener(startOpenaisItem,
                             startOpenaisItemCallback);
        items.add(startOpenaisItem);
        /* Start heartbeat. */
        final MyMenuItem startHeartbeatItem =
            new MyMenuItem(
                      Tools.getString("PcmkMultiSelectionInfo.StartHeartbeat"),
                      HostInfo.HOST_START_COMM_LAYER_ICON,
                      ClusterBrowser.STARTING_PTEST_TOOLTIP,

                      new AccessMode(ConfigData.AccessType.ADMIN, false),
                      new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final HostInfo hi : selectedHostInfos) {
                        final Host h = hi.getHost();
                        if (h.isHeartbeatInit()
                            && h.isHeartbeatConf()
                            && !h.isCsRunning()
                            && !h.isAisRunning()
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
                        getBrowser().updateHWInfo(hi.getHost());
                    }
                }
            };
        final ClusterBrowser.ClMenuItemCallback startHeartbeatItemCallback =
                getBrowser().new ClMenuItemCallback(startHeartbeatItem,
                                                    getBrowser().getDCHost()) {
            @Override
            public void action(final Host host) {
                //TODO
            }
        };
        addMouseOverListener(startHeartbeatItem,
                             startHeartbeatItemCallback);
        items.add(startHeartbeatItem);

        /* Start pacemaker. */
        final MyMenuItem startPcmkItem =
            new MyMenuItem(
                       Tools.getString("PcmkMultiSelectionInfo.StartPacemaker"),
                       HostInfo.HOST_START_COMM_LAYER_ICON,
                       ClusterBrowser.STARTING_PTEST_TOOLTIP,

                       new AccessMode(ConfigData.AccessType.ADMIN, false),
                       new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final HostInfo hi : selectedHostInfos) {
                        final Host h = hi.getHost();
                        if (!h.isPcmkStartedByCorosync()
                            && !h.isPcmkRunning()
                            && (h.isCsRunning()
                                || h.isAisRunning())
                            && !h.isHeartbeatRunning()) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void action() {
                    for (final HostInfo hi : selectedHostInfos) {
                        Corosync.startPacemaker(hi.getHost());
                    }
                    for (final HostInfo hi : selectedHostInfos) {
                        getBrowser().updateHWInfo(hi.getHost());
                    }
                }
            };
        final ClusterBrowser.ClMenuItemCallback startPcmkItemCallback =
                getBrowser().new ClMenuItemCallback(startPcmkItem,
                                                    getBrowser().getDCHost()) {
            @Override
            public void action(final Host host) {
                //TODO
            }
        };
        addMouseOverListener(startPcmkItem,
                             startPcmkItemCallback);
        items.add(startPcmkItem);
        /* change host color */
        final MyMenuItem changeHostColorItem =
            new MyMenuItem(
                    Tools.getString("PcmkMultiSelectionInfo.ChangeHostColor"),
                    null,
                    "",
                    new AccessMode(ConfigData.AccessType.RO, false),
                    new AccessMode(ConfigData.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    return null;
                }

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
                            hi.getHost().setSavedColor(newColor);
                        }
                    }
                }
            };
        items.add(changeHostColorItem);
    }

    /** Create menu items for selected services. */
    private void createSelectedServicesPopup(
                                 final List<ServiceInfo> selectedServiceInfos,
                                 final List<UpdatableItem> items) {
        /* start resources */
        final MyMenuItem startMenuItem =
            new MyMenuItem(Tools.getString(
                              "PcmkMultiSelectionInfo.StartSelectedResources"),
                           ServiceInfo.START_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return true;
                }

                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    boolean allStarted = true;
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        if (!si.isStarted(CRM.LIVE)) {
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
                    hidePopup();
                    final Host dcHost = getBrowser().getDCHost();
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        si.startResource(dcHost, CRM.LIVE);
                    }
                }
            };
        final ClusterBrowser.ClMenuItemCallback startItemCallback =
                    getBrowser().new ClMenuItemCallback(startMenuItem, null) {
            @Override
            public void action(final Host dcHost) {
                for (final ServiceInfo si : selectedServiceInfos) {
                    if (si.isConstraintPH()
                        || si.getService().isNew()
                        || si.getService().isOrphaned()) {
                        continue;
                    }
                    si.startResource(dcHost, CRM.TESTONLY);
                }
            }
        };
        addMouseOverListener(startMenuItem, startItemCallback);
        items.add((UpdatableItem) startMenuItem);
        /* stop resources */
        final MyMenuItem stopMenuItem =
            new MyMenuItem(Tools.getString(
                                "PcmkMultiSelectionInfo.StopSelectedResources"),
                           ServiceInfo.STOP_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return true;
                }

                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    boolean allStopped = true;
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        if (!si.isStopped(CRM.LIVE)) {
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
                    hidePopup();
                    final Host dcHost = getBrowser().getDCHost();
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        si.stopResource(dcHost, CRM.LIVE);
                    }
                }
            };
        final ClusterBrowser.ClMenuItemCallback stopItemCallback =
                    getBrowser().new ClMenuItemCallback(stopMenuItem, null) {
            @Override
            public void action(final Host dcHost) {
                for (final ServiceInfo si : selectedServiceInfos) {
                    if (si.isConstraintPH()
                        || si.getService().isNew()
                        || si.getService().isOrphaned()) {
                        continue;
                    }
                    si.stopResource(dcHost, CRM.TESTONLY);
                }
            }
        };
        addMouseOverListener(stopMenuItem, stopItemCallback);
        items.add((UpdatableItem) stopMenuItem);

        /* clean up resource */
        final MyMenuItem cleanupMenuItem =
            new MyMenuItem(
               Tools.getString("PcmkMultiSelectionInfo.CleanUpFailedResource"),
               ServiceInfo.SERVICE_RUNNING_ICON,
               ClusterBrowser.STARTING_PTEST_TOOLTIP,

               Tools.getString("PcmkMultiSelectionInfo.CleanUpResource"),
               ServiceInfo.SERVICE_RUNNING_ICON,
               ClusterBrowser.STARTING_PTEST_TOOLTIP,
               new AccessMode(ConfigData.AccessType.OP, false),
               new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.getService().isAvailable()
                            && si.isOneFailed(CRM.LIVE)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    boolean failCount = false;
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        if (si.isOneFailedCount(CRM.LIVE)) {
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
                    hidePopup();
                    final Host dcHost = getBrowser().getDCHost();
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        si.cleanupResource(dcHost, CRM.LIVE);
                    }
                }
            };
        /* cleanup ignores CIB_file */
        items.add((UpdatableItem) cleanupMenuItem);


        /* manage resource */
        final MyMenuItem manageMenuItem =
            new MyMenuItem(
                  Tools.getString("PcmkMultiSelectionInfo.ManageResource"),
                  ServiceInfo.MANAGE_BY_CRM_ICON,
                  ClusterBrowser.STARTING_PTEST_TOOLTIP,
                  new AccessMode(ConfigData.AccessType.OP, false),
                  new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        if (!si.isManaged(CRM.LIVE)) {
                            return true;
                        }
                    }
                    return false;
                }
                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
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
                    hidePopup();
                    final Host dcHost = getBrowser().getDCHost();
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        si.setManaged(true, dcHost, CRM.LIVE);
                    }
                }
            };
        final ClusterBrowser.ClMenuItemCallback manageItemCallback =
                  getBrowser().new ClMenuItemCallback(manageMenuItem, null) {
            @Override
            public void action(final Host dcHost) {
                for (final ServiceInfo si : selectedServiceInfos) {
                    if (si.isConstraintPH()
                        || si.getService().isNew()
                        || si.getService().isOrphaned()) {
                        continue;
                    }
                    si.setManaged(true, dcHost, CRM.TESTONLY);
                }
            }
        };
        addMouseOverListener(manageMenuItem, manageItemCallback);
        items.add((UpdatableItem) manageMenuItem);
        /* unmanage resource */
        final MyMenuItem unmanageMenuItem =
            new MyMenuItem(
                  Tools.getString("PcmkMultiSelectionInfo.UnmanageResource"),
                  ServiceInfo.UNMANAGE_BY_CRM_ICON,
                  ClusterBrowser.STARTING_PTEST_TOOLTIP,

                  new AccessMode(ConfigData.AccessType.OP, false),
                  new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        if (si.isManaged(CRM.LIVE)) {
                            return true;
                        }
                    }
                    return false;
                }
                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
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
                    hidePopup();
                    final Host dcHost = getBrowser().getDCHost();
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        si.setManaged(false, dcHost, CRM.LIVE);
                    }
                }
            };
        final ClusterBrowser.ClMenuItemCallback unmanageItemCallback =
                  getBrowser().new ClMenuItemCallback(unmanageMenuItem, null) {
            @Override
            public void action(final Host dcHost) {
                for (final ServiceInfo si : selectedServiceInfos) {
                    if (si.isConstraintPH()
                        || si.getService().isNew()
                        || si.getService().isOrphaned()) {
                        continue;
                    }
                    si.setManaged(false, dcHost, CRM.TESTONLY);
                }
            }
        };
        addMouseOverListener(unmanageMenuItem, unmanageItemCallback);
        items.add((UpdatableItem) unmanageMenuItem);
        /* migrate resource */
        for (final Host host : getBrowser().getClusterHosts()) {
            final String hostName = host.getName();
            final MyMenuItem migrateFromMenuItem =
               new MyMenuItem(Tools.getString(
                                  "PcmkMultiSelectionInfo.MigrateFromResource")
                                  + " " + hostName,
                              ServiceInfo.MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,

                              Tools.getString(
                                  "PcmkMultiSelectionInfo.MigrateFromResource")
                                  + " " + hostName + " (offline)",
                              ServiceInfo.MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,
                              new AccessMode(ConfigData.AccessType.OP, false),
                              new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean predicate() {
                        return host.isClStatus();
                    }

                    @Override
                    public boolean visiblePredicate() {
                        return !host.isClStatus()
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
                            if (getBrowser().clStatusFailed()
                                || !host.isClStatus()) {
                                return "not available on this host";
                            }
                            final List<String> runningOnNodes =
                                                si.getRunningOnNodes(CRM.LIVE);
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
                                continue;
                            } else {
                                return "not available";
                            }
                        }
                        return null;
                    }

                    @Override
                    public void action() {
                        hidePopup();
                        final Host dcHost = getBrowser().getDCHost();
                        for (final ServiceInfo si : selectedServiceInfos) {
                            if (si.isConstraintPH()
                                || si.getService().isNew()
                                || si.getService().isOrphaned()) {
                                continue;
                            }
                            si.migrateFromResource(dcHost, hostName, CRM.LIVE);
                        }
                    }
                };
            final ClusterBrowser.ClMenuItemCallback migrateItemCallback =
               getBrowser().new ClMenuItemCallback(migrateFromMenuItem, null) {
                @Override
                public void action(final Host dcHost) {
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        si.migrateFromResource(dcHost, hostName, CRM.TESTONLY);
                    }
                }
            };
            addMouseOverListener(migrateFromMenuItem, migrateItemCallback);
            items.add(migrateFromMenuItem);
        }

        /* unmigrate resource */
        final MyMenuItem unmigrateMenuItem =
            new MyMenuItem(
                    Tools.getString("PcmkMultiSelectionInfo.UnmigrateResource"),
                    ServiceInfo.UNMIGRATE_ICON,
                    ClusterBrowser.STARTING_PTEST_TOOLTIP,
                    new AccessMode(ConfigData.AccessType.OP, false),
                    new AccessMode(ConfigData.AccessType.OP, false)) {
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
                        if (!getBrowser().clStatusFailed()
                             && si.getService().isAvailable()
                             && (si.getMigratedTo(CRM.LIVE) != null
                                 || si.getMigratedFrom(CRM.LIVE) != null)) {
                            continue;
                        } else {
                            return "not available";
                        }
                    }
                    return null;
                }

                @Override
                public void action() {
                    hidePopup();
                    final Host dcHost = getBrowser().getDCHost();
                    for (final ServiceInfo si : selectedServiceInfos) {
                        if (si.isConstraintPH()
                            || si.getService().isNew()
                            || si.getService().isOrphaned()) {
                            continue;
                        }
                        si.unmigrateResource(dcHost, CRM.LIVE);
                    }
                }
            };
        final ClusterBrowser.ClMenuItemCallback unmigrateItemCallback =
               getBrowser().new ClMenuItemCallback(unmigrateMenuItem, null) {
            @Override
            public void action(final Host dcHost) {
                for (final ServiceInfo si : selectedServiceInfos) {
                    if (si.isConstraintPH()
                        || si.getService().isNew()
                        || si.getService().isOrphaned()) {
                        continue;
                    }
                    si.unmigrateResource(dcHost, CRM.TESTONLY);
                }
            }
        };
        addMouseOverListener(unmigrateMenuItem, unmigrateItemCallback);
        items.add((UpdatableItem) unmigrateMenuItem);
        /* remove service */
        final MyMenuItem removeMenuItem = new MyMenuItem(
                    Tools.getString("PcmkMultiSelectionInfo.RemoveService"),
                    ClusterBrowser.REMOVE_ICON,
                    ClusterBrowser.STARTING_PTEST_TOOLTIP,
                    new AccessMode(ConfigData.AccessType.ADMIN, false),
                    new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                final ClusterStatus cs = getBrowser().getClusterStatus();
                for (final ServiceInfo si : selectedServiceInfos) {
                    if (si.getService().isNew()) {
                        continue;
                    } else if (si.getService().isRemoved()) {
                        return ServiceInfo.IS_BEING_REMOVED_STRING;
                    } else if (si.isRunning(CRM.LIVE)
                               && !Tools.getConfigData().isAdvancedMode()) {
                        return "cannot remove running resource<br>"
                               + "(advanced mode only)";
                    }
                    final GroupInfo gi = si.getGroupInfo();
                    if (gi == null) {
                        continue;
                    }
                    final List<String> gr = cs.getGroupResources(
                                              gi.getHeartbeatId(CRM.LIVE),
                                              CRM.LIVE);


                    if (gr != null && gr.size() > 1) {
                        continue;
                    } else {
                        return "you can remove the group";
                    }
                }
                return null;
            }

            @Override
            public void action() {
                hidePopup();
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
                        si.cleanupResource(dcHost, CRM.LIVE);
                    } else {
                        si.removeMyselfNoConfirm(dcHost, CRM.LIVE);
                    }
                }
                getBrowser().getCRMGraph().repaint();
            }
        };
        final ClusterBrowser.ClMenuItemCallback removeItemCallback =
                getBrowser().new ClMenuItemCallback(removeMenuItem, null) {
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
                    si.removeMyselfNoConfirm(dcHost, CRM.TESTONLY);
                }
            }
        };
        addMouseOverListener(removeMenuItem, removeItemCallback);
        items.add((UpdatableItem) removeMenuItem);
    }

    /** @see EditableInfo#createPopup() */
    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final List<ServiceInfo> selectedServiceInfos =
                                                 new ArrayList<ServiceInfo>();
        final List<HostInfo> selectedHostInfos = new ArrayList<HostInfo>();
        for (final Info i : selectedInfos) {
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

    /** @see EditableInfo#getBrowser() */
    @Override
    protected ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** @see EditableInfo#getGraphicalView() */
    @Override
    public JPanel getGraphicalView() {
        return getBrowser().getCRMGraph().getGraphPanel();
    }

    /** @see EditableInfo#isEnabledOnlyInAdvancedMode() */
    @Override
    protected boolean isEnabledOnlyInAdvancedMode(final String param) {
        return false;
    }

    /** @see EditableInfo#getAccessType() */
    @Override
    protected ConfigData.AccessType getAccessType(final String param) {
        return null;
    }

    /**
     * @see EditableInfo#getSection()
     */
    @Override
    protected String getSection(final String param) {
        return null;
    }

    /** @see EditableInfo#isRequired() */
    @Override
    protected boolean isRequired(final String param) {
        return false;
    }

    /** @see EditableInfo#isAdvanced() */
    @Override
    protected boolean isAdvanced(final String param) {
        return false;
    }

    /** @see EditableInfo#isEnabled() */
    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    /** @see EditableInfo#isInteger() */
    @Override
    protected boolean isInteger(final String param) {
        return false;
    }

    /** @see EditableInfo#isLabel() */
    @Override
    protected boolean isLabel(final String param) {
        return false;
    }

    /** @see EditableInfo#isTimeType() */
    @Override
    protected boolean isTimeType(final String param) {
        return false;
    }

    /** @see EditableInfo#isCheckBox() */
    @Override
    protected boolean isCheckBox(final String param) {
        return false;
    }

    /** @see EditableInfo#getParamType() */
    @Override
    protected String getParamType(final String param) {
        return null;
    }


    /** @see EditableInfo#getParametersFromXML() */
    @Override
    public String[] getParametersFromXML() {
        return null;
    }

    /**
     * @see EditableInfo#getParamPossibleChoices()
     */
    @Override
    protected Object[] getParamPossibleChoices(final String param) {
        return null;
    }

    /** @see EditableInfo#checkParam() */
    @Override
    protected boolean checkParam(final String param, final String newValue) {
        return true;
    }

    /** @see EditableInfo#getParamDefault() */
    @Override
    public String getParamDefault(final String param) {
        return null;
    }

    /**
     * @see EditableInfo#getParamPreferred()
     */
    @Override
    protected String getParamPreferred(final String param) {
        return null;
    }

    /**
     * @see EditableInfo#getParamShortDesc()
     */
    @Override
    protected String getParamShortDesc(final String param) {
        return null;
    }

    /**
     * @see EditableInfo#getParamLongDesc()
     */
    @Override
    protected String getParamLongDesc(final String param) {
        return null;
    }

    /** @see EditableInfo#getInfoPanel() */
    @Override
    public JComponent getInfoPanel() {
        Tools.isSwingThread();
        final boolean abExisted = getApplyButton() != null;
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;
            /**
             * Whether the whole thing should be enabled.
             */
            @Override
            public boolean isEnabled() {
                final Host dcHost = getBrowser().getDCHost();
                if (dcHost == null) {
                    return false;
                }
                if (Tools.versionBeforePacemaker(dcHost)) {
                    return false;
                }
                return true;
            }
            @Override
            public void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                getBrowser().getCRMGraph().stopTestAnimation(getApplyButton());
                getApplyButton().setToolTipText("");
            }

            @Override
            public void mouseOver() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                getApplyButton().setToolTipText(
                                        ClusterBrowser.STARTING_PTEST_TOOLTIP);
                getApplyButton().setToolTipBackground(Tools.getDefaultColor(
                                   "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                getBrowser().getCRMGraph().startTestAnimation(getApplyButton(),
                                                              startTestLatch);
                final Host dcHost = getBrowser().getDCHost();
                getBrowser().ptestLockAcquire();
                final ClusterStatus cs = getBrowser().getClusterStatus();
                cs.setPtestData(null);
                apply(dcHost, CRM.TESTONLY);
                final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
                getApplyButton().setToolTipText(ptestData.getToolTip());
                cs.setPtestData(ptestData);
                getBrowser().ptestLockRelease();
                startTestLatch.countDown();
            }
        };
        initApplyButton(buttonCallback);
        /* add item listeners to the apply button. */
        if (!abExisted) {
            getApplyButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        Tools.debug(this, "BUTTON: apply", 1);
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(getBrowser().getDCHost(), CRM.LIVE);
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );

            getRevertButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        Tools.debug(this, "BUTTON: revert", 1);
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                revert();
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );
        }
        /* main, button and options panels */
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        final AbstractButton serviceMenu = getActionsButton();
        buttonPanel.add(serviceMenu, BorderLayout.EAST);

        /* apply button */
        addApplyButton(buttonPanel);
        addRevertButton(buttonPanel);
        final String[] params = getParametersFromXML();
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                /* invoke later on purpose  */
                setApplyButtons(null, params);
            }
        });
        mainPanel.add(optionsPanel);
        mainPanel.add(super.getInfoPanel());
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(
                                  ClusterBrowser.SERVICE_LABEL_WIDTH
                                  + ClusterBrowser.SERVICE_FIELD_WIDTH + 4));
        newPanel.add(new JScrollPane(mainPanel));
        /* if id textfield was changed and this id is not used,
         * enable apply button */
        infoPanelDone();
        return newPanel;
    }

    /**
     * Apply the changes to the service parameters.
     * not implemented
     */
    void apply(final Host dcHost, final boolean testOnly) {
    }
}
