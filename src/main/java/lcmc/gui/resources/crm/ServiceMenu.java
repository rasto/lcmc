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

import java.awt.FlowLayout;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.crm.CrmXml;
import lcmc.data.crm.ClusterStatus;
import lcmc.data.Host;
import lcmc.data.crm.ResourceAgent;
import lcmc.data.Value;
import lcmc.data.resources.Service;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.dialog.EditConfig;
import lcmc.gui.dialog.pacemaker.ServiceLogs;
import lcmc.gui.widget.Widget;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.ComponentWithTest;
import lcmc.utilities.MyList;
import lcmc.utilities.MyListModel;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class ServiceMenu {
    
    private final ServiceInfo serviceInfo;
    
    public ServiceMenu(ServiceInfo serviceInfo) {
        super();
        this.serviceInfo = serviceInfo;
    }

    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final Application.RunMode runMode = Application.RunMode.LIVE;
        final CloneInfo ci = serviceInfo.getCloneInfo();
        if (ci == null) {
            addDependencyMenuItems(items, false, runMode);
        }
        /* start resource */
        final ComponentWithTest startMenuItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.StartResource"),
                           ServiceInfo.START_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    } else if (serviceInfo.isStarted(runMode)) {
                        return Tools.getString("ServiceInfo.AlreadyStarted");
                    } else {
                        return getService().isAvailableWithText();
                    }
                }

                @Override
                public void action() {
                    serviceInfo.hidePopup();
                    serviceInfo.startResource(getBrowser().getDCHost(), runMode);
                }
            };
        final ButtonCallback startItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                serviceInfo.startResource(dcHost, Application.RunMode.TEST);
            }
        };
        serviceInfo.addMouseOverListener(startMenuItem, startItemCallback);
        items.add((UpdatableItem) startMenuItem);

        /* stop resource */
        final ComponentWithTest stopMenuItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.StopResource"),
                           ServiceInfo.STOP_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    } else if (serviceInfo.isStopped(runMode)) {
                        return Tools.getString("ServiceInfo.AlreadyStopped");
                    } else {
                        return getService().isAvailableWithText();
                    }
                }

                @Override
                public void action() {
                    serviceInfo.hidePopup();
                    serviceInfo.stopResource(getBrowser().getDCHost(), runMode);
                }
            };
        final ButtonCallback stopItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                serviceInfo.stopResource(dcHost, Application.RunMode.TEST);
            }
        };
        serviceInfo.addMouseOverListener(stopMenuItem, stopItemCallback);
        items.add((UpdatableItem) stopMenuItem);

        /* up group resource */
        final ComponentWithTest upMenuItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.UpResource"),
                           ServiceInfo.GROUP_UP_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return serviceInfo.getGroupInfo() != null;
                }

                @Override
                public String enablePredicate() {
                    if (serviceInfo.getResource().isNew()) {
                        return ServiceInfo.IS_NEW_STRING;
                    }
                    final GroupInfo gi = serviceInfo.getGroupInfo();
                    if (gi == null) {
                        return "no";
                    }
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    final DefaultMutableTreeNode giNode = gi.getNode();
                    if (giNode == null) {
                        return "no";
                    }
                    final DefaultMutableTreeNode node = serviceInfo.getNode();
                    if (node == null) {
                        return "no";
                    }
                    final int index = giNode.getIndex(node);
                    if (index == 0) {
                        return "already up";
                    }
                    return null;
                }

                @Override
                public void action() {
                    serviceInfo.hidePopup();
                    serviceInfo.upResource(getBrowser().getDCHost(), runMode);
                }
            };
        final ButtonCallback upItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                serviceInfo.upResource(dcHost, Application.RunMode.TEST);
            }
        };
        serviceInfo.addMouseOverListener(upMenuItem, upItemCallback);
        items.add((UpdatableItem) upMenuItem);

        /* down group resource */
        final ComponentWithTest downMenuItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.DownResource"),
                           ServiceInfo.GROUP_DOWN_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(Application.AccessType.OP, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return serviceInfo.getGroupInfo() != null;
                }

                @Override
                public String enablePredicate() {
                    if (serviceInfo.getResource().isNew()) {
                        return ServiceInfo.IS_NEW_STRING;
                    }
                    final GroupInfo gi = serviceInfo.getGroupInfo();
                    if (gi == null) {
                        return "no";
                    }
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    final DefaultMutableTreeNode giNode = gi.getNode();
                    if (giNode == null) {
                        return "no";
                    }
                    final DefaultMutableTreeNode node = serviceInfo.getNode();
                    if (node == null) {
                        return "no";
                    }
                    final int index = giNode.getIndex(node);
                    if (index >= giNode.getChildCount() - 1) {
                        return "already down";
                    }
                    return null;
                }

                @Override
                public void action() {
                    serviceInfo.hidePopup();
                    serviceInfo.downResource(getBrowser().getDCHost(), runMode);
                }
            };
        final ButtonCallback downItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                serviceInfo.downResource(dcHost, Application.RunMode.TEST);
            }
        };
        serviceInfo.addMouseOverListener(downMenuItem, downItemCallback);
        items.add((UpdatableItem) downMenuItem);

        /* clean up resource */
        final UpdatableItem cleanupMenuItem =
            new MyMenuItem(
               Tools.getString("ClusterBrowser.Hb.CleanUpFailedResource"),
               ServiceInfo.SERVICE_RUNNING_ICON,
               ClusterBrowser.STARTING_PTEST_TOOLTIP,

               Tools.getString("ClusterBrowser.Hb.CleanUpResource"),
               ServiceInfo.SERVICE_RUNNING_ICON,
               ClusterBrowser.STARTING_PTEST_TOOLTIP,
               new AccessMode(Application.AccessType.OP, false),
               new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return getService().isAvailable()
                           && serviceInfo.isOneFailed(runMode);
                }

                @Override
                public String enablePredicate() {
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    } else if (!serviceInfo.isOneFailedCount(runMode)) {
                        return "no fail count";
                    } else {
                        return getService().isAvailableWithText();
                    }
                }

                @Override
                public void action() {
                    serviceInfo.hidePopup();
                    serviceInfo.cleanupResource(getBrowser().getDCHost(), runMode);
                }
            };
        /* cleanup ignores CIB_file */
        items.add(cleanupMenuItem);


        /* manage resource */
        final ComponentWithTest manageMenuItem =
            new MyMenuItem(
                  Tools.getString("ClusterBrowser.Hb.ManageResource"),
                  ServiceInfo.MANAGE_BY_CRM_ICON,
                  ClusterBrowser.STARTING_PTEST_TOOLTIP,

                  Tools.getString("ClusterBrowser.Hb.UnmanageResource"),
                  ServiceInfo.UNMANAGE_BY_CRM_ICON,
                  ClusterBrowser.STARTING_PTEST_TOOLTIP,

                  new AccessMode(Application.AccessType.OP, false),
                  new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return !serviceInfo.isManaged(runMode);
                }
                @Override
                public String enablePredicate() {
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    } else {
                        return getService().isAvailableWithText();
                    }
                }

                @Override
                public void action() {
                    serviceInfo.hidePopup();
                    if (getText().equals(Tools.getString(
                                    "ClusterBrowser.Hb.ManageResource"))) {
                        serviceInfo.setManaged(true, getBrowser().getDCHost(), runMode);
                    } else {
                        serviceInfo.setManaged(false, getBrowser().getDCHost(), runMode);
                    }
                }
            };
        final ButtonCallback manageItemCallback =
                                     getBrowser().new ClMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                serviceInfo.setManaged(!serviceInfo.isManaged(Application.RunMode.TEST),
                                       dcHost,
                                       Application.RunMode.TEST);
            }
        };
        serviceInfo.addMouseOverListener(manageMenuItem, manageItemCallback);
        items.add((UpdatableItem) manageMenuItem);
        addMigrateMenuItems(items);
        if (ci == null) {
            /* remove service */
            final ComponentWithTest removeMenuItem = new MyMenuItem(
                        Tools.getString("ClusterBrowser.Hb.RemoveService"),
                        ClusterBrowser.REMOVE_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getService().isNew()) {
                        return null;
                    }
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    } else if (getService().isRemoved()) {
                        return ServiceInfo.IS_BEING_REMOVED_STRING;
                    } else if (serviceInfo.isRunning(runMode)
                               && !Tools.getApplication().isAdvancedMode()) {
                        return "cannot remove running resource<br>"
                               + "(advanced mode only)";
                    }
                    if (serviceInfo.getGroupInfo() == null) {
                        return null;
                    }
                    final ClusterStatus cs = getBrowser().getClusterStatus();
                    final List<String> gr = cs.getGroupResources(
                                          serviceInfo.getGroupInfo().getHeartbeatId(runMode),
                                          runMode);


                    if (gr != null && gr.size() > 1) {
                        return null;
                    } else {
                        return "you can remove the group";
                    }
                }

                @Override
                public void action() {
                    serviceInfo.hidePopup();
                    if (getService().isOrphaned()) {
                        serviceInfo.cleanupResource(getBrowser().getDCHost(), runMode);
                    } else {
                        serviceInfo.removeMyself(Application.RunMode.LIVE);
                    }
                    getBrowser().getCrmGraph().repaint();
                }
            };
            final ButtonCallback removeItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
                @Override
                public boolean isEnabled() {
                    return super.isEnabled() && !getService().isNew();
                }
                @Override
                public void action(final Host dcHost) {
                    serviceInfo.removeMyselfNoConfirm(dcHost, Application.RunMode.TEST);
                }
            };
            serviceInfo.addMouseOverListener(removeMenuItem, removeItemCallback);
            items.add((UpdatableItem) removeMenuItem);
        }
        /* view log */
        final UpdatableItem viewLogMenu = new MyMenuItem(
                        Tools.getString("ClusterBrowser.Hb.ViewServiceLog"),
                        ServiceInfo.LOGFILE_ICON,
                        null,
                        new AccessMode(Application.AccessType.RO, false),
                        new AccessMode(Application.AccessType.RO, false)) {

            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getService().isNew()) {
                    return ServiceInfo.IS_NEW_STRING;
                } else {
                    return null;
                }
            }

            @Override
            public void action() {
                serviceInfo.hidePopup();
                final ServiceLogs l = new ServiceLogs(getBrowser().getCluster(),
                                                serviceInfo.getNameForLog(),
                                                getService().getHeartbeatId());
                l.showDialog();
            }
        };
        items.add(viewLogMenu);
        /* more migrate options */
        final MyMenu migrateSubmenu = new MyMenu(
                        Tools.getString("ClusterBrowser.MigrateSubmenu"),
                        new AccessMode(Application.AccessType.OP, false),
                        new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public String enablePredicate() {
                return null; //TODO: enable only if it has items
            }
        };
        items.add(migrateSubmenu);
        addMoreMigrateMenuItems(migrateSubmenu);

        /* config files */
        final UpdatableItem filesSubmenu = new MyMenu(
                        Tools.getString("ClusterBrowser.FilesSubmenu"),
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.ADMIN, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public String enablePredicate() {
                return null; //TODO: enable only if it has items
            }
            @Override
            public void updateAndWait() {
                super.updateAndWait();
                Tools.isSwingThread();
                final MyMenu self = this;
                Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                    @Override
                    public void run() {
                        removeAll();
                        addFilesMenuItems(self);
                    }
                });
            }
        };
        items.add(filesSubmenu);
        return items;
    }

    /** Adds new Service and dependence. */
    private MyMenu getAddServiceMenuItem(final Application.RunMode runMode,
                                         final String name) {
        return new MyMenu(name,
                          new AccessMode(Application.AccessType.ADMIN, false),
                          new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getBrowser().crmStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                } else if (getService().isRemoved()) {
                    return ServiceInfo.IS_BEING_REMOVED_STRING;
                } else if (getService().isOrphaned()) {
                    return ServiceInfo.IS_ORPHANED_STRING;
                } else if (getService().isNew()) {
                    return ServiceInfo.IS_NEW_STRING;
                }
                return null;
            }

            @Override
            public void updateAndWait() {
                Tools.isSwingThread();
                removeAll();
                final Point2D pos = getPos();
                final CrmXml crmXML = getBrowser().getCrmXml();
                final ResourceAgent fsService =
                     crmXML.getResourceAgent("Filesystem",
                                             ResourceAgent.HEARTBEAT_PROVIDER,
                                             ResourceAgent.OCF_CLASS_NAME);
                if (crmXML.isLinbitDrbdResourceAgentPresent()) { /* just skip it, if it
                                                       is not */
                    /* Linbit:DRBD */
                    addDrbdLinbitMenu(this, crmXML, pos, fsService, runMode);
                }
                if (crmXML.isDrbddiskResourceAgentPresent()) { /* just skip it,
                                                     if it is not */
                    /* drbddisk */
                    addDrbddiskMenu(this, crmXML, pos, fsService, runMode);
                }
                final ResourceAgent ipService = crmXML.getResourceAgent(
                                         "IPaddr2",
                                         ResourceAgent.HEARTBEAT_PROVIDER,
                                         ResourceAgent.OCF_CLASS_NAME);
                if (ipService != null) { /* just skip it, if it is not*/
                    /* ipaddr */
                    addIpMenu(this, pos, ipService, runMode);
                }
                if (fsService != null) { /* just skip it, if it is not*/
                    /* Filesystem */
                    addFilesystemMenu(this, pos, fsService, runMode);
                }
                final Collection<JDialog> popups = new ArrayList<JDialog>();
                for (final String cl : ClusterBrowser.CRM_CLASSES) {
                    final List<ResourceAgent> services = serviceInfo.getAddServiceList(cl);
                    if (services.isEmpty()) {
                        /* no services, don't show */
                        continue;
                    }
                    final JCheckBox colocationWi = new JCheckBox("Colo", true);
                    final JCheckBox orderWi = new JCheckBox("Order", true);
                    colocationWi.setBackground(
                                            ClusterBrowser.STATUS_BACKGROUND);
                    colocationWi.setPreferredSize(
                                            colocationWi.getMinimumSize());
                    orderWi.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                    orderWi.setPreferredSize(orderWi.getMinimumSize());
                    final JPanel colOrdPanel =
                            new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
                    colOrdPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                    colOrdPanel.add(colocationWi);
                    colOrdPanel.add(orderWi);
                    boolean mode = !AccessMode.ADVANCED;
                    if (ResourceAgent.UPSTART_CLASS_NAME.equals(cl)
                        || ResourceAgent.SYSTEMD_CLASS_NAME.equals(cl)) {
                        mode = AccessMode.ADVANCED;
                    }
                    if (ResourceAgent.LSB_CLASS_NAME.equals(cl)
                        && !serviceInfo.getAddServiceList(
                                    ResourceAgent.SERVICE_CLASS_NAME).isEmpty()) {
                        mode = AccessMode.ADVANCED;
                    }
                    final MyMenu classItem = new MyMenu(
                            ClusterBrowser.getClassMenuName(cl),
                            new AccessMode(Application.AccessType.ADMIN, mode),
                            new AccessMode(Application.AccessType.OP, mode));
                    final MyListModel<MyMenuItem> dlm =
                                                 new MyListModel<MyMenuItem>();
                    for (final ResourceAgent ra : services) {
                        addResourceAgentMenu(ra,
                                             dlm,
                                             pos,
                                             popups,
                                             colocationWi,
                                             orderWi,
                                             runMode);
                    }
                    final boolean ret = Tools.getScrollingMenu(
                            ClusterBrowser.getClassMenuName(cl),
                            colOrdPanel,
                            classItem,
                            dlm,
                            new MyList<MyMenuItem>(dlm,
                                                   getBackground()),
                            serviceInfo,
                            popups,
                            null);
                    if (!ret) {
                        classItem.setEnabled(false);
                    }
                    add(classItem);
                }
                super.updateAndWait();
            }
        };
    }

    /** Adds migrate and unmigrate menu items. */
    protected void addMigrateMenuItems(final List<UpdatableItem> items) {
        /* migrate resource */
        final Application.RunMode runMode = Application.RunMode.LIVE;
        for (final Host host : getBrowser().getClusterHosts()) {
            final String hostName = host.getName();
            final MyMenuItem migrateFromMenuItem =
               new MyMenuItem(Tools.getString(
                                   "ClusterBrowser.Hb.MigrateFromResource")
                                   + ' ' + hostName,
                              ServiceInfo.MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,

                              Tools.getString(
                                   "ClusterBrowser.Hb.MigrateFromResource")
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
                        final List<String> runningOnNodes =
                                     serviceInfo.getRunningOnNodes(runMode);
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
                        if (!getBrowser().crmStatusFailed()
                               && getService().isAvailable()
                               && runningOnNode
                               && host.isCrmStatusOk()) {
                            return null;
                        } else {
                            return ""; /* is not visible anyway */
                        }
                    }

                    @Override
                    public void action() {
                        serviceInfo.hidePopup();
                        serviceInfo.migrateFromResource(
                                            getBrowser().getDCHost(),
                                            hostName,
                                            runMode);
                    }
                };
            final ButtonCallback migrateItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
                @Override
                public void action(final Host dcHost) {
                    serviceInfo.migrateFromResource(dcHost, hostName, Application.RunMode.TEST);
                }
            };
            serviceInfo.addMouseOverListener(migrateFromMenuItem, migrateItemCallback);
            items.add(migrateFromMenuItem);
        }

        /* unmigrate resource */
        final ComponentWithTest unmigrateMenuItem =
            new MyMenuItem(
                    Tools.getString("ClusterBrowser.Hb.UnmigrateResource"),
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
                    if (!getBrowser().crmStatusFailed()
                           && getService().isAvailable()
                           && (serviceInfo.getMigratedTo(runMode) != null
                               || serviceInfo.getMigratedFrom(runMode) != null)) {
                        return null;
                    } else {
                        return ""; /* it's not visible anyway */
                    }
                }

                @Override
                public void action() {
                    serviceInfo.hidePopup();
                    serviceInfo.unmigrateResource(getBrowser().getDCHost(), runMode);
                }
            };
        final ButtonCallback unmigrateItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                serviceInfo.unmigrateResource(dcHost, Application.RunMode.TEST);
            }
        };
        serviceInfo.addMouseOverListener(unmigrateMenuItem, unmigrateItemCallback);
        items.add((UpdatableItem) unmigrateMenuItem);
    }

    /** Adds "migrate from" and "force migrate" menuitems to the submenu. */
    protected void addMoreMigrateMenuItems(final MyMenu submenu) {
        final Application.RunMode runMode = Application.RunMode.LIVE;
        for (final Host host : getBrowser().getClusterHosts()) {
            final String hostName = host.getName();
            final MyMenuItem migrateMenuItem =
               new MyMenuItem(Tools.getString(
                                   "ClusterBrowser.Hb.MigrateResource")
                                   + ' ' + hostName,
                              ServiceInfo.MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,

                              Tools.getString(
                                   "ClusterBrowser.Hb.MigrateResource")
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
                        final List<String> runningOnNodes =
                                     serviceInfo.getRunningOnNodes(runMode);
                        if (runningOnNodes == null
                            || runningOnNodes.isEmpty()) {
                            return Tools.getString(
                                            "ServiceInfo.NotRunningAnywhere");
                        }
                        final String runningOnNode =
                                runningOnNodes.get(0).toLowerCase(Locale.US);
                        if (getBrowser().crmStatusFailed()
                            || !host.isCrmStatusOk()) {
                            return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                        } else {
                            final String tp =
                                            getService().isAvailableWithText();
                            if (tp != null) {
                                return tp;
                            }
                        }
                        if (hostName.toLowerCase(Locale.US).equals(
                                                             runningOnNode)) {
                            return Tools.getString(
                                           "ServiceInfo.AlreadyRunningOnNode");
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public void action() {
                        serviceInfo.hidePopup();
                        serviceInfo.migrateResource(
                                                 hostName,
                                                 getBrowser().getDCHost(),
                                                 runMode);
                    }
                };
            final ButtonCallback migrateItemCallback =
                                     getBrowser().new ClMenuItemCallback(null) {
                @Override
                public void action(final Host dcHost) {
                    serviceInfo.migrateResource(hostName, dcHost, Application.RunMode.TEST);
                }
            };
            serviceInfo.addMouseOverListener(migrateMenuItem, migrateItemCallback);
            submenu.add(migrateMenuItem);
        }
        for (final Host host : getBrowser().getClusterHosts()) {
            final String hostName = host.getName();

            final MyMenuItem forceMigrateMenuItem =
               new MyMenuItem(Tools.getString(
                                   "ClusterBrowser.Hb.ForceMigrateResource")
                                   + ' ' + hostName,
                              ServiceInfo.MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,

                              Tools.getString(
                                   "ClusterBrowser.Hb.ForceMigrateResource")
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
                        final List<String> runningOnNodes =
                                     serviceInfo.getRunningOnNodes(runMode);
                        if (runningOnNodes == null
                            || runningOnNodes.isEmpty()) {
                            return Tools.getString(
                                            "ServiceInfo.NotRunningAnywhere");
                        }
                        final String runningOnNode =
                                runningOnNodes.get(0).toLowerCase(Locale.US);
                        if (!getBrowser().crmStatusFailed()
                               && getService().isAvailable()
                               && !hostName.toLowerCase(Locale.US).equals(
                                                                 runningOnNode)
                               && host.isCrmStatusOk()) {
                            return null;
                        } else {
                            return "";
                        }
                    }

                    @Override
                    public void action() {
                        serviceInfo.hidePopup();
                        serviceInfo.forceMigrateResource(
                                             hostName,
                                             getBrowser().getDCHost(),
                                             runMode);
                    }
                };
            final ButtonCallback forceMigrateItemCallback =
                                     getBrowser().new ClMenuItemCallback(null) {
                @Override
                public void action(final Host dcHost) {
                    serviceInfo.forceMigrateResource(
                                         hostName,
                                         dcHost,
                                         Application.RunMode.TEST);
                }
            };
            serviceInfo.addMouseOverListener(forceMigrateMenuItem,
                                 forceMigrateItemCallback);
            submenu.add(forceMigrateMenuItem);
        }
    }

    /** Return config files defined in DistResource config files. */
    private List<String> getConfigFiles() {
        final String raName;
        final ServiceInfo cs = serviceInfo.getContainedService();
        if (cs == null) {
            raName = serviceInfo.getResourceAgent().getRAString();
        } else {
            raName = cs.getResourceAgent().getRAString();
        }
        final Host[] hosts = getBrowser().getCluster().getHostsArray();
        final List<String> cfs =
             new ArrayList<String>(hosts[0].getDistStrings(raName + ".files"));
        final Collection<String> params =
            new ArrayList<String>(hosts[0].getDistStrings(raName + ".params"));
        params.add("configfile");
        params.add("config");
        params.add("conffile");
        for (final String param : params) {
            final Value value;
            if (cs == null) {
                final Widget wi = serviceInfo.getWidget(param, null);
                if (wi == null) {
                    value = serviceInfo.getParamSaved(param);
                } else {
                    value = wi.getValue();
                }
            } else {
                final Widget wi = cs.getWidget(param, null);
                if (wi == null) {
                    value = cs.getParamSaved(param);
                } else {
                    value = wi.getValue();
                }
            }
            if (value != null && !value.isNothingSelected()) {
                cfs.add(value.getValueForConfig());
            }
        }
        return cfs;
    }


    /** Adds config files menuitems to the submenu. */
    protected void addFilesMenuItems(final MyMenu submenu) {
        final List<String> configFiles = getConfigFiles();
        for (final String configFile : configFiles) {
            final MyMenuItem fileItem =
               new MyMenuItem(
                          configFile,
                          null,
                          null,
                          new AccessMode(Application.AccessType.ADMIN, false),
                          new AccessMode(Application.AccessType.ADMIN, false)) {
                    private static final long serialVersionUID = 1L;

                   @Override
                    public void action() {
                        final EditConfig ed =
                          new EditConfig(configFile,
                                         getBrowser().getCluster().getHosts());
                        ed.showDialog();

                    }
                };
            submenu.add(fileItem);
        }
    }

    /** Adds existing service menu item for every member of a group. */
    protected void addExistingGroupServiceMenuItems(
                        final ServiceInfo asi,
                        final MyListModel<MyMenuItem> dlm,
                        final Map<MyMenuItem, ButtonCallback> callbackHash,
                        final MyList<MyMenuItem> list,
                        final JCheckBox colocationWi,
                        final JCheckBox orderWi,
                        final List<JDialog> popups,
                        final Application.RunMode runMode) {
        /* empty */
    }

    /** Adds existing service menu item. */
    protected void addExistingServiceMenuItem(
                        final String name,
                        final ServiceInfo asi,
                        final MyListModel<MyMenuItem> dlm,
                        final Map<MyMenuItem, ButtonCallback> callbackHash,
                        final MyList<MyMenuItem> list,
                        final JCheckBox colocationWi,
                        final JCheckBox orderWi,
                        final Iterable<JDialog> popups,
                        final Application.RunMode runMode) {
        final MyMenuItem mmi = new MyMenuItem(name,
                                              null,
                                              null,
                                              new AccessMode(
                                                   Application.AccessType.ADMIN,
                                                   false),
                                              new AccessMode(
                                                   Application.AccessType.OP,
                                                   false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public void action() {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        serviceInfo.hidePopup();
                        Tools.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                for (final JDialog otherP : popups) {
                                    otherP.dispose();
                                }
                            }
                        });
                        serviceInfo.addServicePanel(asi,
                                                    null,
                                                    colocationWi.isSelected(),
                                                    orderWi.isSelected(),
                                                    true,
                                                    getBrowser().getDCHost(),
                                                    runMode);
                        Tools.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                repaint();
                            }
                        });
                    }
                });
                thread.start();
            }
        };
        dlm.addElement(mmi);
        final ButtonCallback mmiCallback =
                       getBrowser().new ClMenuItemCallback(null) {
                           @Override
                           public void action(final Host dcHost) {
                               serviceInfo.addServicePanel(
                                               asi,
                                               null,
                                               colocationWi.isSelected(),
                                               orderWi.isSelected(),
                                               true,
                                               dcHost,
                                               Application.RunMode.TEST);
                           }
                       };
        callbackHash.put(mmi, mmiCallback);
    }

    /** Returns existing service manu item. */
    private MyMenu getExistingServiceMenuItem(final String name,
                                              final boolean enableForNew,
                                              final Application.RunMode runMode) {
        return new MyMenu(name,
                          new AccessMode(Application.AccessType.ADMIN, false),
                          new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getBrowser().crmStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                } else if (getService().isRemoved()) {
                    return ServiceInfo.IS_BEING_REMOVED_STRING;
                } else if (getService().isOrphaned()) {
                    return ServiceInfo.IS_ORPHANED_STRING;
                } else if (!enableForNew && getService().isNew()) {
                    return ServiceInfo.IS_NEW_STRING;
                }
                if (getBrowser().getExistingServiceList(serviceInfo).isEmpty()) {
                    return "&lt;&lt;empty;&gt;&gt;";
                }
                return null;
            }

            @Override
            public void updateAndWait() {
                Tools.isSwingThread();
                final JCheckBox colocationWi = new JCheckBox("Colo", true);
                final JCheckBox orderWi = new JCheckBox("Order", true);
                colocationWi.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                colocationWi.setPreferredSize(colocationWi.getMinimumSize());
                orderWi.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                orderWi.setPreferredSize(orderWi.getMinimumSize());
                setEnabled(false);
                removeAll();

                final MyListModel<MyMenuItem> dlm =
                                                new MyListModel<MyMenuItem>();
                final Map<MyMenuItem, ButtonCallback> callbackHash =
                                 new HashMap<MyMenuItem, ButtonCallback>();
                final MyList<MyMenuItem> list =
                                   new MyList<MyMenuItem>(dlm, getBackground());

                final List<JDialog> popups = new ArrayList<JDialog>();
                for (final ServiceInfo asi
                            : getBrowser().getExistingServiceList(serviceInfo)) {
                    if (asi.isConstraintPH() && serviceInfo.isConstraintPH()) {
                        continue;
                    }
                    if (asi.getCloneInfo() != null
                        || asi.getGroupInfo() != null) {
                        /* skip services that are clones or in groups. */
                        continue;
                    }
                    addExistingServiceMenuItem(asi.toString(),
                                               asi,
                                               dlm,
                                               callbackHash,
                                               list,
                                               colocationWi,
                                               orderWi,
                                               popups,
                                               runMode);
                    if (asi.getResourceAgent() != null
                        && asi.getResourceAgent().isGroup()) {
                        final GroupMenu groupMenu =
                                          new GroupMenu((GroupInfo) asi);
                        groupMenu.addExistingGroupServiceMenuItems(
                                                             serviceInfo,
                                                             dlm,
                                                             callbackHash,
                                                             list,
                                                             colocationWi,
                                                             orderWi,
                                                             popups,
                                                             runMode);
                    }
                }
                final JPanel colOrdPanel =
                            new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
                colOrdPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                colOrdPanel.add(colocationWi);
                colOrdPanel.add(orderWi);
                final boolean ret =
                            Tools.getScrollingMenu(name,
                                                   colOrdPanel,
                                                   this,
                                                   dlm,
                                                   list,
                                                   serviceInfo,
                                                   popups,
                                                   callbackHash);
                if (!ret) {
                    setEnabled(false);
                }
                super.updateAndWait();
            }
        };
    }

    /** Adds Linbit DRBD RA menu item. It is called in swing thread. */
    private void addDrbdLinbitMenu(final MyMenu menu,
                                   final CrmXml crmXML,
                                   final Point2D pos,
                                   final ResourceAgent fsService,
                                   final Application.RunMode runMode) {
        final MyMenuItem ldMenuItem = new MyMenuItem(
                           Tools.getString("ClusterBrowser.linbitDrbdMenuName"),
                           null,
                           null,
                           new AccessMode(Application.AccessType.ADMIN, false),
                           new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public void action() {
                serviceInfo.hidePopup();
                if (!getBrowser().linbitDrbdConfirmDialog()) {
                    return;
                }

                final FilesystemInfo fsi = (FilesystemInfo)
                                               serviceInfo.addServicePanel(
                                                    fsService,
                                                    getPos(),
                                                    true, /* colocation */
                                                    true, /* order */
                                                    true,
                                                    false,
                                                    runMode);
                fsi.setDrbddiskIsPreferred(false);
                getBrowser().getCrmGraph().repaint();
            }
        };
        if (getBrowser().atLeastOneDrbddiskConfigured()
            || !crmXML.isLinbitDrbdResourceAgentPresent()) {
            ldMenuItem.setEnabled(false);
        }
        ldMenuItem.setPos(pos);
        menu.add(ldMenuItem);
    }

    /** Adds drbddisk RA menu item. It is called in swing thread. */
    private void addDrbddiskMenu(final MyMenu menu,
                                 final CrmXml crmXML,
                                 final Point2D pos,
                                 final ResourceAgent fsService,
                                 final Application.RunMode runMode) {
        final MyMenuItem ddMenuItem = new MyMenuItem(
                         Tools.getString("ClusterBrowser.DrbddiskMenuName"),
                         null,
                         null,
                         new AccessMode(Application.AccessType.ADMIN, false),
                         new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public void action() {
                serviceInfo.hidePopup();
                final FilesystemInfo fsi = (FilesystemInfo) serviceInfo.addServicePanel(
                                                    fsService,
                                                    getPos(),
                                                    true, /* colocation */
                                                    true, /* order */
                                                    true,
                                                    false,
                                                    runMode);
                fsi.setDrbddiskIsPreferred(true);
                getBrowser().getCrmGraph().repaint();
            }
        };
        if (getBrowser().isOneLinbitDrbdRaConfigured()
            || !crmXML.isDrbddiskResourceAgentPresent()) {
            ddMenuItem.setEnabled(false);
        }
        ddMenuItem.setPos(pos);
        menu.add(ddMenuItem);
    }

    /** Adds Ipaddr RA menu item. It is called in swing thread. */
    private void addIpMenu(final MyMenu menu,
                           final Point2D pos,
                           final ResourceAgent ipService,
                           final Application.RunMode runMode) {
        final MyMenuItem ipMenuItem =
          new MyMenuItem(ipService.getPullDownMenuName(),
                         null,
                         null,
                         new AccessMode(Application.AccessType.ADMIN, false),
                         new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public void action() {
                serviceInfo.hidePopup();
                serviceInfo.addServicePanel(ipService,
                                            getPos(),
                                            true, /* colocation */
                                            true, /* order */
                                            true,
                                            false,
                                            runMode);
                getBrowser().getCrmGraph().repaint();
            }
        };
        ipMenuItem.setPos(pos);
        menu.add(ipMenuItem);
    }

    /** Adds Filesystem RA menu item. It is called in swing thread. */
    private void addFilesystemMenu(final MyMenu menu,
                                   final Point2D pos,
                                   final ResourceAgent fsService,
                                   final Application.RunMode runMode) {
        final MyMenuItem fsMenuItem =
              new MyMenuItem(fsService.getPullDownMenuName(),
                             null,
                             null,
                             new AccessMode(Application.AccessType.ADMIN, false),
                             new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;
                @Override
                public void action() {
                    serviceInfo.hidePopup();
                    serviceInfo.addServicePanel(fsService,
                                                getPos(),
                                                true, /* colocation */
                                                true, /* order */
                                                true,
                                                false,
                                                runMode);
                    getBrowser().getCrmGraph().repaint();
                }
        };
        fsMenuItem.setPos(pos);
        menu.add(fsMenuItem);
    }

    /** Adds resource agent RA menu item. It is called in swing thread. */
    private void addResourceAgentMenu(final ResourceAgent ra,
                                      final MyListModel<MyMenuItem> dlm,
                                      final Point2D pos,
                                      final Iterable<JDialog> popups,
                                      final JCheckBox colocationWi,
                                      final JCheckBox orderWi,
                                      final Application.RunMode runMode) {
        final MyMenuItem mmi =
               new MyMenuItem(
                     ra.getPullDownMenuName(),
                     null,
                     null,
                     new AccessMode(Application.AccessType.ADMIN,
                                    false),
                     new AccessMode(Application.AccessType.OP,
                                    false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public void action() {
                serviceInfo.hidePopup();
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        for (final JDialog otherP : popups) {
                            otherP.dispose();
                        }
                    }
                });
                if (ra.isLinbitDrbd()
                    &&
                     !getBrowser().linbitDrbdConfirmDialog()) {
                    return;
                } else if (ra.isHbDrbd()
                     && !getBrowser().hbDrbdConfirmDialog()) {
                    return;
                }
                serviceInfo.addServicePanel(ra,
                                            getPos(),
                                            colocationWi.isSelected(),
                                            orderWi.isSelected(),
                                            true,
                                            false,
                                            runMode);
                getBrowser().getCrmGraph().repaint();
            }
        };
        mmi.setPos(pos);
        dlm.addElement(mmi);
    }


    /** Adds menu items with dependend services and groups. */
    protected void addDependencyMenuItems(final Collection<UpdatableItem> items,
                                          final boolean enableForNew,
                                          final Application.RunMode runMode) {
        /* add new group and dependency*/
        final UpdatableItem addGroupMenuItem =
            new MyMenuItem(Tools.getString(
                                "ClusterBrowser.Hb.AddDependentGroup"),
                           null,
                           null,
                           new AccessMode(Application.AccessType.ADMIN, false),
                           new AccessMode(Application.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    } else if (getService().isRemoved()) {
                        return ServiceInfo.IS_BEING_REMOVED_STRING;
                    } else if (getService().isOrphaned()) {
                        return ServiceInfo.IS_ORPHANED_STRING;
                    } else if (getService().isNew()) {
                        return ServiceInfo.IS_NEW_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    serviceInfo.hidePopup();
                    final CrmXml crmXML = getBrowser().getCrmXml();
                    serviceInfo.addServicePanel(
                                    crmXML.getGroupResourceAgent(),
                                    getPos(),
                                    false, /* colocation only */
                                    false, /* order only */
                                    true,
                                    false,
                                    runMode);
                    getBrowser().getCrmGraph().repaint();
                }
            };
        items.add(addGroupMenuItem);

        /* add new service and dependency*/
        final MyMenu addServiceMenuItem = getAddServiceMenuItem(
                        runMode,
                        Tools.getString("ClusterBrowser.Hb.AddDependency"));
        items.add(addServiceMenuItem);

        /* add existing service dependency*/
        final MyMenu existingServiceMenuItem = getExistingServiceMenuItem(
                    Tools.getString("ClusterBrowser.Hb.AddStartBefore"),
                    enableForNew,
                    runMode);
        items.add(existingServiceMenuItem);
    }

    protected ClusterBrowser getBrowser() {
        return serviceInfo.getBrowser();
    }
    
    protected Service getService() {
        return serviceInfo.getService();
    }

    protected ServiceInfo getServiceInfo() {
        return serviceInfo;
    }
}
