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

import java.awt.FlowLayout;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import lcmc.LCMC;
import lcmc.common.ui.Access;
import lcmc.common.ui.CallbackAction;
import lcmc.common.ui.GUIData;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.crm.domain.CrmXml;
import lcmc.crm.domain.ClusterStatus;
import lcmc.host.domain.Host;
import lcmc.crm.domain.ResourceAgent;
import lcmc.common.domain.Value;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.ui.EditConfig;
import lcmc.crm.ui.ServiceLogs;
import lcmc.cluster.ui.widget.Widget;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.ui.utils.ComponentWithTest;
import lcmc.common.domain.EnablePredicate;
import lcmc.common.ui.utils.MenuAction;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyList;
import lcmc.common.ui.utils.MyListModel;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.domain.Predicate;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.common.domain.VisiblePredicate;

@Named
public class ServiceMenu {
    @Inject
    private GUIData drbdGui;
    @Inject
    private EditConfig editDialog;
    @Inject
    private MenuFactory menuFactory;
    @Inject
    private Application application;
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private Provider<ServiceLogs> serviceLogsProvider;
    @Inject
    private TreeMenuController treeMenuController;
    @Inject
    private Access access;

    public List<UpdatableItem> getPulldownMenu(final ServiceInfo serviceInfo) {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final Application.RunMode runMode = Application.RunMode.LIVE;
        final CloneInfo ci = serviceInfo.getCloneInfo();
        if (ci == null) {
            addDependencyMenuItems(serviceInfo, items, false, runMode);
        }
        /* start resource */
        final ComponentWithTest startMenuItem = menuFactory.createMenuItem(
                        Tools.getString("ClusterBrowser.Hb.StartResource"),
                        ServiceInfo.START_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (serviceInfo.getBrowser().crmStatusFailed()) {
                                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                                } else if (serviceInfo.isStarted(runMode)) {
                                    return Tools.getString("ServiceInfo.AlreadyStarted");
                                } else {
                                    return serviceInfo.getService().isAvailableWithText();
                                }
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                serviceInfo.hidePopup();
                                serviceInfo.startResource(serviceInfo.getBrowser().getDCHost(), runMode);
                            }
                        });
        final ButtonCallback startItemCallback = serviceInfo.getBrowser().new ClMenuItemCallback(null)
                        .addAction(new CallbackAction() {
                            @Override
                            public void run(final Host dcHost) {
                                serviceInfo.startResource(dcHost, Application.RunMode.TEST);
                            }
                        });
        serviceInfo.addMouseOverListener(startMenuItem, startItemCallback);
        items.add((UpdatableItem) startMenuItem);

        /* stop resource */
        final ComponentWithTest stopMenuItem =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.StopResource"),
                        ServiceInfo.STOP_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (serviceInfo.getBrowser().crmStatusFailed()) {
                                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                                } else if (serviceInfo.isStopped(runMode)) {
                                    return Tools.getString("ServiceInfo.AlreadyStopped");
                                } else {
                                    return serviceInfo.getService().isAvailableWithText();
                                }
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                serviceInfo.hidePopup();
                                serviceInfo.stopResource(serviceInfo.getBrowser().getDCHost(), runMode);
                            }
                        });
        final ButtonCallback stopItemCallback = serviceInfo.getBrowser().new ClMenuItemCallback(null)
                        .addAction(new CallbackAction() {
                            @Override
                            public void run(final Host dcHost) {
                                serviceInfo.stopResource(dcHost, Application.RunMode.TEST);
                            }
                        });
        serviceInfo.addMouseOverListener(stopMenuItem, stopItemCallback);
        items.add((UpdatableItem) stopMenuItem);

        /* up group resource */
        final ComponentWithTest upMenuItem =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.UpResource"),
                        ServiceInfo.GROUP_UP_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return serviceInfo.getGroupInfo() != null;
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (serviceInfo.getResource().isNew()) {
                                    return ServiceInfo.IS_NEW_STRING;
                                }
                                final GroupInfo gi = serviceInfo.getGroupInfo();
                                if (gi == null) {
                                    return "no";
                                }
                                if (serviceInfo.getBrowser().crmStatusFailed()) {
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
                                final int index = treeMenuController.getIndex(giNode, node);
                                if (index == 0) {
                                    return "already up";
                                }
                                return null;
                            }
                        })

                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                serviceInfo.hidePopup();
                                serviceInfo.upResource(serviceInfo.getBrowser().getDCHost(), runMode);
                            }
                        });
        final ButtonCallback upItemCallback =
                serviceInfo.getBrowser().new ClMenuItemCallback(null)
                        .addAction(new CallbackAction() {
                            @Override
                            public void run(final Host dcHost) {
                                serviceInfo.upResource(dcHost, Application.RunMode.TEST);
                            }
                        });
        serviceInfo.addMouseOverListener(upMenuItem, upItemCallback);
        items.add((UpdatableItem) upMenuItem);

        /* down group resource */
        final ComponentWithTest downMenuItem =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.DownResource"),
                        ServiceInfo.GROUP_DOWN_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            @Override
                            public boolean check() {
                                return serviceInfo.getGroupInfo() != null;
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (serviceInfo.getResource().isNew()) {
                                    return ServiceInfo.IS_NEW_STRING;
                                }
                                final GroupInfo gi = serviceInfo.getGroupInfo();
                                if (gi == null) {
                                    return "no";
                                }
                                if (serviceInfo.getBrowser().crmStatusFailed()) {
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
                                final int index = treeMenuController.getIndex(giNode, node);
                                final int groupChildCount = treeMenuController.getChildCount(giNode);
                                if (index >= groupChildCount - 1) {
                                    return "already down";
                                }
                                return null;
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                serviceInfo.hidePopup();
                                serviceInfo.downResource(serviceInfo.getBrowser().getDCHost(), runMode);
                            }
                        });
        final ButtonCallback downItemCallback = serviceInfo.getBrowser().new ClMenuItemCallback(null)
                        .addAction(new CallbackAction() {
                            @Override
                            public void run(final Host dcHost) {
                                serviceInfo.downResource(dcHost, Application.RunMode.TEST);
                            }
                        });
        serviceInfo.addMouseOverListener(downMenuItem, downItemCallback);
        items.add((UpdatableItem) downMenuItem);

        /* clean up resource */
        final UpdatableItem cleanupMenuItem = menuFactory.createMenuItem(
                        Tools.getString("ClusterBrowser.Hb.CleanUpFailedResource"),
                        ServiceInfo.SERVICE_RUNNING_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        Tools.getString("ClusterBrowser.Hb.CleanUpResource"),
                        ServiceInfo.SERVICE_RUNNING_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))

                        .predicate(new Predicate() {
                            @Override
                            public boolean check() {
                                return serviceInfo.getService().isAvailable() && serviceInfo.isOneFailed(runMode);
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (serviceInfo.getBrowser().crmStatusFailed()) {
                                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                                } else if (!serviceInfo.isOneFailedCount(runMode)) {
                                    return "no fail count";
                                } else {
                                    return serviceInfo.getService().isAvailableWithText();
                                }
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                serviceInfo.hidePopup();
                                serviceInfo.cleanupResource(serviceInfo.getBrowser().getDCHost(), runMode);
                            }
                        });
        /* cleanup ignores CIB_file */
        items.add(cleanupMenuItem);

        /* manage resource */
        final ComponentWithTest manageMenuItem =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.ManageResource"),
                        ServiceInfo.MANAGE_BY_CRM_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        Tools.getString("ClusterBrowser.Hb.UnmanageResource"),
                        ServiceInfo.UNMANAGE_BY_CRM_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,

                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .predicate(new Predicate() {
                            @Override
                            public boolean check() {
                                return !serviceInfo.isManaged(runMode);
                            }
                        })
                        .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                                if (serviceInfo.getBrowser().crmStatusFailed()) {
                                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                                } else {
                                    return serviceInfo.getService().isAvailableWithText();
                                }
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                serviceInfo.hidePopup();
                                if (text.equals(Tools.getString("ClusterBrowser.Hb.ManageResource"))) {
                                    serviceInfo.setManaged(true, serviceInfo.getBrowser().getDCHost(), runMode);
                                } else {
                                    serviceInfo.setManaged(false, serviceInfo.getBrowser().getDCHost(), runMode);
                                }
                            }
                        });
        final ButtonCallback manageItemCallback =
                serviceInfo.getBrowser().new ClMenuItemCallback(null)
                        .addAction(new CallbackAction() {
                            @Override
                            public void run(final Host dcHost) {
                                serviceInfo.setManaged(!serviceInfo.isManaged(Application.RunMode.TEST),
                                                       dcHost,
                                                       Application.RunMode.TEST);
                            }
                        });
        serviceInfo.addMouseOverListener(manageMenuItem, manageItemCallback);
        items.add((UpdatableItem) manageMenuItem);
        addMigrateMenuItems(serviceInfo, items);
        if (ci == null) {
            /* remove service */
            final ComponentWithTest removeMenuItem = menuFactory.createMenuItem(
                    Tools.getString("ClusterBrowser.Hb.RemoveService"),
                    ClusterBrowser.REMOVE_ICON,
                    ClusterBrowser.STARTING_PTEST_TOOLTIP,
                    new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                    new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                    .enablePredicate(new EnablePredicate() {
                        @Override
                        public String check() {
                            if (serviceInfo.getService().isNew()) {
                                return null;
                            }
                            if (serviceInfo.getBrowser().crmStatusFailed()) {
                                return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                            } else if (serviceInfo.getService().isRemoved()) {
                                return ServiceInfo.IS_BEING_REMOVED_STRING;
                            } else if (serviceInfo.isRunning(runMode) && !access.isAdvancedMode()) {
                                return "cannot remove running resource<br>(advanced mode only)";
                            }
                            if (serviceInfo.getGroupInfo() == null) {
                                return null;
                            }
                            final ClusterStatus cs = serviceInfo.getBrowser().getClusterStatus();
                            final List<String> gr = cs.getGroupResources(
                                                          serviceInfo.getGroupInfo().getHeartbeatId(runMode),
                                                          runMode);


                            if (gr != null && gr.size() > 1) {
                                return null;
                            } else {
                                return "you can remove the group";
                            }
                        }
                    })
                    .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                            serviceInfo.hidePopup();
                            if (serviceInfo.getService().isOrphaned()) {
                                serviceInfo.cleanupResource(serviceInfo.getBrowser().getDCHost(), runMode);
                            } else {
                                serviceInfo.removeMyself(Application.RunMode.LIVE);
                            }
                            serviceInfo.getBrowser().getCrmGraph().repaint();
                        }
                    });
            final ButtonCallback removeItemCallback =
                    serviceInfo.getBrowser().new ClMenuItemCallback(null) {
                        @Override
                        public boolean isEnabled() {
                            return super.isEnabled() && !serviceInfo.getService().isNew();
                        }
                    }
                    .addAction(new CallbackAction() {
                        @Override
                        public void run(final Host dcHost) {
                            serviceInfo.removeMyselfNoConfirm(dcHost, Application.RunMode.TEST);
                        }
                    });
            serviceInfo.addMouseOverListener(removeMenuItem, removeItemCallback);
            items.add((UpdatableItem) removeMenuItem);
        }

        /* view log */
        final UpdatableItem viewLogMenu = menuFactory.createMenuItem(
                Tools.getString("ClusterBrowser.Hb.ViewServiceLog"),
                ServiceInfo.LOGFILE_ICON,
                null,
                new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                new AccessMode(AccessMode.RO, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (serviceInfo.getService().isNew()) {
                            return ServiceInfo.IS_NEW_STRING;
                        } else {
                            return null;
                        }
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        serviceInfo.hidePopup();
                        final ServiceLogs serviceLogs = serviceLogsProvider.get();
                        serviceLogs.init(serviceInfo.getBrowser().getCluster(),
                                serviceInfo.getNameForLog(),
                                serviceInfo.getService().getCrmId());
                        serviceLogs.showDialog();
                    }
                });
        items.add(viewLogMenu);
        /* more migrate options */
        final MyMenu migrateSubmenu = menuFactory.createMenu(
                Tools.getString("ClusterBrowser.MigrateSubmenu"),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    public String check() {
                        return null; //TODO: enable only if it has items
                    }
                });
        items.add(migrateSubmenu);

        addMoreMigrateMenuItems(serviceInfo, migrateSubmenu);

        /* config files */
        final MyMenu filesSubmenu = menuFactory.createMenu(
                Tools.getString("ClusterBrowser.FilesSubmenu"),
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        return null; //TODO: enable only if it has items
                    }
                });
        filesSubmenu.onUpdate(new Runnable() {
            @Override
            public void run() {
                swingUtils.isSwingThread();
                swingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        filesSubmenu.removeAll();
                        addFilesMenuItems(serviceInfo, filesSubmenu);
                        filesSubmenu.updateMenuComponents();
                        filesSubmenu.processAccessMode();
                    }
                });
            }
        });
        items.add(filesSubmenu);
        return items;
    }

    /**
     * Adds new Service and dependence.
     */
    private MyMenu getAddServiceMenuItem(final ServiceInfo serviceInfo,
                                         final Application.RunMode runMode,
                                         final String name) {
        final MyMenu serviceMenu = menuFactory.createMenu(name,
                                                          new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                                          new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (serviceInfo.getBrowser().crmStatusFailed()) {
                            return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                        } else if (serviceInfo.getService().isRemoved()) {
                            return ServiceInfo.IS_BEING_REMOVED_STRING;
                        } else if (serviceInfo.getService().isOrphaned()) {
                            return ServiceInfo.IS_ORPHANED_STRING;
                        } else if (serviceInfo.getService().isNew()) {
                            return ServiceInfo.IS_NEW_STRING;
                        }
                        return null;
                    }
                });
        serviceMenu.onUpdate(new Runnable() {
            @Override
            public void run() {
                swingUtils.isSwingThread();
                serviceMenu.removeAll();
                final Point2D pos = serviceMenu.getPos();
                final CrmXml crmXML = serviceInfo.getBrowser().getCrmXml();
                final ResourceAgent fsService = crmXML.getResourceAgent("Filesystem",
                                                                        ResourceAgent.HEARTBEAT_PROVIDER,
                                                                        ResourceAgent.OCF_CLASS_NAME);
                if (crmXML.isLinbitDrbdResourceAgentPresent()) { /* just skip it, if it is not */
                    /* Linbit:DRBD */
                    addDrbdLinbitMenu(serviceInfo, serviceMenu, crmXML, pos, fsService, runMode);
                }
                if (crmXML.isDrbddiskResourceAgentPresent()) { /* just skip it,
                                                     if it is not */
                    /* drbddisk */
                    addDrbddiskMenu(serviceInfo, serviceMenu, crmXML, pos, fsService, runMode);
                }
                final ResourceAgent ipService = crmXML.getResourceAgent("IPaddr2",
                                                                        ResourceAgent.HEARTBEAT_PROVIDER,
                                                                        ResourceAgent.OCF_CLASS_NAME);
                if (ipService != null) { /* just skip it, if it is not*/
                    /* ipaddr */
                    addIpMenu(serviceInfo, serviceMenu, pos, ipService, runMode);
                }
                if (fsService != null) { /* just skip it, if it is not*/
                    /* Filesystem */
                    addFilesystemMenu(serviceInfo, serviceMenu, pos, fsService, runMode);
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
                    colocationWi.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                    colocationWi.setPreferredSize(colocationWi.getMinimumSize());
                    orderWi.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                    orderWi.setPreferredSize(orderWi.getMinimumSize());
                    final JPanel colOrdPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
                    colOrdPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                    colOrdPanel.add(colocationWi);
                    colOrdPanel.add(orderWi);
                    AccessMode.Mode mode = AccessMode.NORMAL;
                    if (ResourceAgent.UPSTART_CLASS_NAME.equals(cl) || ResourceAgent.SYSTEMD_CLASS_NAME.equals(cl)) {
                        mode = AccessMode.ADVANCED;
                    }
                    if (ResourceAgent.LSB_CLASS_NAME.equals(cl) && !serviceInfo.getAddServiceList(
                                                                        ResourceAgent.SERVICE_CLASS_NAME).isEmpty()) {
                        mode = AccessMode.ADVANCED;
                    }
                    final MyMenu classItem = menuFactory.createMenu(
                            ClusterBrowser.getClassMenuName(cl),
                            new AccessMode(AccessMode.ADMIN, mode),
                            new AccessMode(AccessMode.OP, mode));
                    final MyListModel<MyMenuItem> dlm = new MyListModel<MyMenuItem>();
                    for (final ResourceAgent ra : services) {
                        addResourceAgentMenu(serviceInfo, ra, dlm, pos, popups, colocationWi, orderWi, runMode);
                    }
                    final boolean ret = drbdGui.getScrollingMenu(
                                                      ClusterBrowser.getClassMenuName(cl),
                                                      colOrdPanel,
                                                      classItem,
                                                      dlm,
                                                      new MyList<MyMenuItem>(dlm, serviceMenu.getBackground()),
                                                      serviceInfo,
                                                      popups,
                                                      null);
                    if (!ret) {
                        classItem.setEnabled(false);
                    }
                    serviceMenu.add(classItem);
                }
                serviceMenu.updateMenuComponents();
                serviceMenu.processAccessMode();
            }
        });
        return serviceMenu;
    }

    /**
     * Adds migrate and unmigrate menu items.
     */
    protected void addMigrateMenuItems(final ServiceInfo serviceInfo, final List<UpdatableItem> items) {
        /* migrate resource */
        final Application.RunMode runMode = Application.RunMode.LIVE;
        for (final Host host : serviceInfo.getBrowser().getClusterHosts()) {
            final String hostName = host.getName();
            final MyMenuItem migrateFromMenuItem =
                    menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.MigrateFromResource")
                                               + ' ' + hostName,
                            ServiceInfo.MIGRATE_ICON,
                            ClusterBrowser.STARTING_PTEST_TOOLTIP,

                            Tools.getString("ClusterBrowser.Hb.MigrateFromResource") + ' ' + hostName + " (offline)",
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
                                public boolean check() {
                                    if (!host.isCrmStatusOk()) {
                                        return false;
                                    }
                                    final List<String> runningOnNodes = serviceInfo.getRunningOnNodes(runMode);
                                    if (runningOnNodes == null || runningOnNodes.size() < 1) {
                                        return false;
                                    }
                                    boolean runningOnNode = false;
                                    for (final String ron : runningOnNodes) {
                                        if (hostName.toLowerCase(Locale.US).equals(ron.toLowerCase(Locale.US))) {
                                            runningOnNode = true;
                                            break;
                                        }
                                    }
                                    if (!serviceInfo.getBrowser().crmStatusFailed()
                                        && serviceInfo.getService().isAvailable()
                                        && runningOnNode
                                        && host.isCrmStatusOk()) {
                                        return true;
                                    } else {
                                        return false;
                                    }
                                }
                            })
                            .addAction(new MenuAction() {
                                @Override
                                public void run(final String text) {
                                    serviceInfo.hidePopup();
                                    serviceInfo.migrateFromResource(serviceInfo.getBrowser().getDCHost(),
                                                                    hostName,
                                                                    runMode);
                                }
                            });
            final ButtonCallback migrateItemCallback =
                    serviceInfo.getBrowser().new ClMenuItemCallback(null)
                            .addAction(new CallbackAction() {
                                @Override
                                public void run(final Host dcHost) {
                                    serviceInfo.migrateFromResource(dcHost, hostName, Application.RunMode.TEST);
                                }
                            });
            serviceInfo.addMouseOverListener(migrateFromMenuItem, migrateItemCallback);
            items.add(migrateFromMenuItem);
        }

        /* unmigrate resource */
        final ComponentWithTest unmigrateMenuItem =
                menuFactory.createMenuItem(
                        Tools.getString("ClusterBrowser.Hb.UnmigrateResource"),
                        ServiceInfo.UNMIGRATE_ICON,
                        ClusterBrowser.STARTING_PTEST_TOOLTIP,
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(new VisiblePredicate() {
                            public boolean check() {
                                if (!serviceInfo.getBrowser().crmStatusFailed()
                                    && serviceInfo.getService().isAvailable()
                                    && (serviceInfo.getMigratedTo(runMode) != null
                                    || serviceInfo.getMigratedFrom(runMode) != null)) {
                                    return true;
                                } else {
                                    return false;
                                }
                            }
                        })
                        .addAction(new MenuAction() {
                            @Override
                            public void run(final String text) {
                                serviceInfo.hidePopup();
                                serviceInfo.unmigrateResource(serviceInfo.getBrowser().getDCHost(), runMode);
                            }
                        });
        final ButtonCallback unmigrateItemCallback =
                serviceInfo.getBrowser().new ClMenuItemCallback(null)
                        .addAction(new CallbackAction() {
                                       @Override
                                       public void run(final Host dcHost) {
                                           serviceInfo.unmigrateResource(dcHost, Application.RunMode.TEST);
                                       }
                                   }

                        );
        serviceInfo.addMouseOverListener(unmigrateMenuItem, unmigrateItemCallback);
        items.add((UpdatableItem) unmigrateMenuItem);
    }

    /**
     * Adds "migrate from" and "force migrate" menuitems to the submenu.
     */
    protected void addMoreMigrateMenuItems(final ServiceInfo serviceInfo, final MyMenu submenu) {
        final Application.RunMode runMode = Application.RunMode.LIVE;
        for (final Host host : serviceInfo.getBrowser().getClusterHosts()) {
            final String hostName = host.getName();
            final MyMenuItem migrateMenuItem =
                    menuFactory.createMenuItem(
                            Tools.getString("ClusterBrowser.Hb.MigrateResource") + ' ' + hostName,
                            ServiceInfo.MIGRATE_ICON,
                            ClusterBrowser.STARTING_PTEST_TOOLTIP,

                            Tools.getString("ClusterBrowser.Hb.MigrateResource") + ' ' + hostName + " (offline)",
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
                                    final List<String> runningOnNodes = serviceInfo.getRunningOnNodes(runMode);
                                    if (runningOnNodes == null || runningOnNodes.isEmpty()) {
                                        return false;
                                    }
                                    final String runningOnNode = runningOnNodes.get(0).toLowerCase(Locale.US);
                                    if (serviceInfo.getBrowser().crmStatusFailed() || !host.isCrmStatusOk()) {
                                        return false;
                                    } else {
                                        final String tp = serviceInfo.getService().isAvailableWithText();
                                        if (tp != null) {
                                            return false;
                                        }
                                    }
                                    if (hostName.toLowerCase(Locale.US).equals(runningOnNode)) {
                                        return false;
                                    } else {
                                        return true;
                                    }
                                }
                            })
                            .addAction(new MenuAction() {
                                @Override
                                public void run(final String text) {
                                    serviceInfo.hidePopup();
                                    serviceInfo.migrateResource(hostName,
                                                                serviceInfo.getBrowser().getDCHost(),
                                                                runMode);
                                }
                            });
            final ButtonCallback migrateItemCallback =
                    serviceInfo.getBrowser().new ClMenuItemCallback(null)
                            .addAction(new CallbackAction() {
                                @Override
                                public void run(final Host dcHost) {
                                    serviceInfo.migrateResource(hostName, dcHost, Application.RunMode.TEST);
                                }
                            });
            serviceInfo.addMouseOverListener(migrateMenuItem, migrateItemCallback);
            submenu.add(migrateMenuItem);
        }
        for (final Host host : serviceInfo.getBrowser().getClusterHosts()) {
            final String hostName = host.getName();

            final MyMenuItem forceMigrateMenuItem =
                    menuFactory.createMenuItem(
                            Tools.getString("ClusterBrowser.Hb.ForceMigrateResource") + ' ' + hostName,
                            ServiceInfo.MIGRATE_ICON,
                            ClusterBrowser.STARTING_PTEST_TOOLTIP,

                            Tools.getString("ClusterBrowser.Hb.ForceMigrateResource") + ' ' + hostName + " (offline)",
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
                                    final List<String> runningOnNodes = serviceInfo.getRunningOnNodes(runMode);
                                    if (runningOnNodes == null || runningOnNodes.isEmpty()) {
                                        return false;
                                    }
                                    final String runningOnNode = runningOnNodes.get(0).toLowerCase(Locale.US);
                                    if (!serviceInfo.getBrowser().crmStatusFailed()
                                        && serviceInfo.getService().isAvailable()
                                        && !hostName.toLowerCase(Locale.US).equals(runningOnNode)
                                        && host.isCrmStatusOk()) {
                                        return true;
                                    } else {
                                        return false;
                                    }
                                }
                            })
                            .addAction(new MenuAction() {
                                @Override
                                public void run(final String text) {
                                    serviceInfo.hidePopup();
                                    serviceInfo.forceMigrateResource(hostName,
                                                                     serviceInfo.getBrowser().getDCHost(),
                                                                     runMode);
                                }
                            });
            final ButtonCallback forceMigrateItemCallback =
                    serviceInfo.getBrowser().new ClMenuItemCallback(null)
                            .addAction(new CallbackAction() {
                                @Override
                                public void run(final Host dcHost) {
                                    serviceInfo.forceMigrateResource(hostName,
                                                                     dcHost,
                                                                     Application.RunMode.TEST);
                                }
                            });
            serviceInfo.addMouseOverListener(forceMigrateMenuItem, forceMigrateItemCallback);
            submenu.add(forceMigrateMenuItem);
        }
    }

    /**
     * Return config files defined in DistResource config files.
     */
    private List<String> getConfigFiles(final ServiceInfo serviceInfo) {
        final String raName;
        final ServiceInfo cs = serviceInfo.getContainedService();
        if (cs == null) {
            raName = serviceInfo.getResourceAgent().getRAString();
        } else {
            raName = cs.getResourceAgent().getRAString();
        }
        final Host[] hosts = serviceInfo.getBrowser().getCluster().getHostsArray();
        final List<String> cfs = new ArrayList<String>(hosts[0].getDistStrings(raName + ".files"));
        final Collection<String> params = new ArrayList<String>(hosts[0].getDistStrings(raName + ".params"));
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

    /**
     * Adds config files menuitems to the submenu.
     */
    protected void addFilesMenuItems(final ServiceInfo serviceInfo, final MyMenu submenu) {
        final List<String> configFiles = getConfigFiles(serviceInfo);
        for (final String configFile : configFiles) {
            final MyMenuItem fileItem =
                    menuFactory.createMenuItem(
                            configFile,
                            null,
                            null,
                            new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                            new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL))
                            .addAction(new MenuAction() {
                                @Override
                                public void run(final String text) {
                                    editDialog.init(configFile, serviceInfo.getBrowser().getCluster().getHosts());
                                    editDialog.showDialog();

                                }
                            });
            submenu.add(fileItem);
        }
    }

    /**
     * Adds existing service menu item for every member of a group.
     */
    protected void addExistingGroupServiceMenuItems(final ServiceInfo serviceInfo,
                                                    final ServiceInfo existingService,
                                                    final MyListModel<MyMenuItem> dlm,
                                                    final Map<MyMenuItem, ButtonCallback> callbackHash,
                                                    final MyList<MyMenuItem> list,
                                                    final JCheckBox colocationWi,
                                                    final JCheckBox orderWi,
                                                    final List<JDialog> popups,
                                                    final Application.RunMode runMode) {
        /* empty */
    }

    protected void addExistingServiceMenuItem(final ServiceInfo serviceInfo,
                                              final String name,
                                              final ServiceInfo otherService,
                                              final MyListModel<MyMenuItem> dlm,
                                              final Map<MyMenuItem, ButtonCallback> callbackHash,
                                              final MyList<MyMenuItem> list,
                                              final JCheckBox colocationWi,
                                              final JCheckBox orderWi,
                                              final Iterable<JDialog> popups,
                                              final Application.RunMode runMode) {
        final MyMenuItem existingServiceMenu = menuFactory.createMenuItem(
                                                    name,
                                                    null,
                                                    null,
                                                    new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                                    new AccessMode(AccessMode.OP, AccessMode.NORMAL));
        existingServiceMenu.addAction(new MenuAction() {
            @Override
            public void run(final String text) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        serviceInfo.hidePopup();
                        swingUtils.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                for (final JDialog otherP : popups) {
                                    otherP.dispose();
                                }
                            }
                        });
                        serviceInfo.addServicePanel(otherService,
                                                    null,
                                                    colocationWi.isSelected(),
                                                    orderWi.isSelected(),
                                                    true,
                                                    serviceInfo.getBrowser().getDCHost(),
                                                    runMode);
                        swingUtils.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                existingServiceMenu.repaint();
                            }
                        });
                    }
                });
                thread.start();
            }
        });
        dlm.addElement(existingServiceMenu);
        final ButtonCallback mmiCallback =
                serviceInfo.getBrowser().new ClMenuItemCallback(null)
                        .addAction(new CallbackAction() {
                            @Override
                            public void run(final Host dcHost) {
                                serviceInfo.addServicePanel(otherService,
                                                            null,
                                                            colocationWi.isSelected(),
                                                            orderWi.isSelected(),
                                                            true,
                                                            dcHost,
                                                            Application.RunMode.TEST);
                            }
                        });
        callbackHash.put(existingServiceMenu, mmiCallback);
    }

    /**
     * Returns existing service manu item.
     */
    private MyMenu getExistingServiceMenuItem(final ServiceInfo serviceInfo,
                                              final String name,
                                              final boolean enableForNew,
                                              final Application.RunMode runMode) {
        final MyMenu serviceMenu = menuFactory.createMenu(name,
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (serviceInfo.getBrowser().crmStatusFailed()) {
                            return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                        } else if (serviceInfo.getService().isRemoved()) {
                            return ServiceInfo.IS_BEING_REMOVED_STRING;
                        } else if (serviceInfo.getService().isOrphaned()) {
                            return ServiceInfo.IS_ORPHANED_STRING;
                        } else if (!enableForNew && serviceInfo.getService().isNew()) {
                            return ServiceInfo.IS_NEW_STRING;
                        }
                        if (serviceInfo.getBrowser().getExistingServiceList(serviceInfo).isEmpty()) {
                            return "&lt;&lt;empty;&gt;&gt;";
                        }
                        return null;
                    }
                });
        serviceMenu.onUpdate(new Runnable() {
            @Override
            public void run() {
                swingUtils.isSwingThread();
                final JCheckBox colocationWi = new JCheckBox("Colo", true);
                final JCheckBox orderWi = new JCheckBox("Order", true);
                colocationWi.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                colocationWi.setPreferredSize(colocationWi.getMinimumSize());
                orderWi.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                orderWi.setPreferredSize(orderWi.getMinimumSize());
                serviceMenu.setEnabled(false);
                serviceMenu.removeAll();

                final MyListModel<MyMenuItem> dlm = new MyListModel<MyMenuItem>();
                final Map<MyMenuItem, ButtonCallback> callbackHash = new HashMap<MyMenuItem, ButtonCallback>();
                final MyList<MyMenuItem> list = new MyList<MyMenuItem>(dlm, serviceMenu.getBackground());

                final List<JDialog> popups = new ArrayList<JDialog>();
                for (final ServiceInfo otherService : serviceInfo.getBrowser().getExistingServiceList(serviceInfo)) {
                    if (otherService.isConstraintPlaceholder() && serviceInfo.isConstraintPlaceholder()) {
                        continue;
                    }
                    if (otherService.getCloneInfo() != null || otherService.getGroupInfo() != null) {
                        /* skip services that are clones or in groups. */
                        continue;
                    }
                    addExistingServiceMenuItem(serviceInfo,
                                               otherService.toString(),
                                               otherService,
                                               dlm,
                                               callbackHash,
                                               list,
                                               colocationWi,
                                               orderWi,
                                               popups,
                                               runMode);
                    if (otherService.getResourceAgent() != null && otherService.getResourceAgent().isGroup()) {
                        final GroupMenu groupMenu = LCMC.getInstance(GroupMenu.class);
                        groupMenu.addExistingGroupServiceMenuItems(serviceInfo,
                                                                   otherService,
                                                                   dlm,
                                                                   callbackHash,
                                                                   list,
                                                                   colocationWi,
                                                                   orderWi,
                                                                   popups,
                                                                   runMode);
                    }
                }
                final JPanel colOrdPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
                colOrdPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
                colOrdPanel.add(colocationWi);
                colOrdPanel.add(orderWi);
                final boolean ret = drbdGui.getScrollingMenu(name,
                                                             colOrdPanel,
                                                             serviceMenu,
                                                             dlm,
                                                             list,
                                                             serviceInfo,
                                                             popups,
                                                             callbackHash);
                if (!ret) {
                    serviceMenu.setEnabled(false);
                }
                serviceMenu.updateMenuComponents();
                serviceMenu.processAccessMode();
            }
        });
        return serviceMenu;
    }

    /**
     * Adds Linbit DRBD RA menu item. It is called in swing thread.
     */
    private void addDrbdLinbitMenu(final ServiceInfo serviceInfo,
                                   final MyMenu menu,
                                   final CrmXml crmXML,
                                   final Point2D pos,
                                   final ResourceAgent fsService,
                                   final Application.RunMode runMode) {
        final MyMenuItem ldMenuItem = menuFactory.createMenuItem(
                                            Tools.getString("ClusterBrowser.linbitDrbdMenuName"),
                                            null,
                                            null,
                                            new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                            new AccessMode(AccessMode.OP, AccessMode.NORMAL));
        ldMenuItem.addAction(new MenuAction() {
            @Override
            public void run(final String text) {
                serviceInfo.hidePopup();
                if (!serviceInfo.getBrowser().linbitDrbdConfirmDialog()) {
                    return;
                }

                final FilesystemRaInfo fsi = (FilesystemRaInfo) serviceInfo.addServicePanel(
                                                                                  fsService,
                                                                                  ldMenuItem.getPos(),
                                                                                  true, /* colocation */
                                                                                  true, /* order */
                                                                                  true,
                                                                                  false,
                                                                                  runMode);
                fsi.setDrbddiskIsPreferred(false);
                serviceInfo.getBrowser().getCrmGraph().repaint();
            }
        });
        if (serviceInfo.getBrowser().atLeastOneDrbddiskConfigured() || !crmXML.isLinbitDrbdResourceAgentPresent()) {
            ldMenuItem.setEnabled(false);
        }
        ldMenuItem.setPos(pos);
        menu.add(ldMenuItem);
    }

    /**
     * Adds drbddisk RA menu item. It is called in swing thread.
     */
    private void addDrbddiskMenu(final ServiceInfo serviceInfo,
                                 final MyMenu menu,
                                 final CrmXml crmXML,
                                 final Point2D pos,
                                 final ResourceAgent fsService,
                                 final Application.RunMode runMode) {
        final MyMenuItem ddMenuItem = menuFactory.createMenuItem(
                                                     Tools.getString("ClusterBrowser.DrbddiskMenuName"),
                                                     null,
                                                     null,
                                                     new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                                     new AccessMode(AccessMode.OP, AccessMode.NORMAL));
        ddMenuItem.addAction(new MenuAction() {
            @Override
            public void run(final String text) {
                serviceInfo.hidePopup();
                final FilesystemRaInfo fsi = (FilesystemRaInfo) serviceInfo.addServicePanel(
                                                                                fsService,
                                                                                ddMenuItem.getPos(),
                                                                                true, /* colocation */
                                                                                true, /* order */
                                                                                true,
                                                                                false,
                                                                                runMode);
                fsi.setDrbddiskIsPreferred(true);
                serviceInfo.getBrowser().getCrmGraph().repaint();
            }
        });
        if (serviceInfo.getBrowser().isOneLinbitDrbdRaConfigured() || !crmXML.isDrbddiskResourceAgentPresent()) {
            ddMenuItem.setEnabled(false);
        }
        ddMenuItem.setPos(pos);
        menu.add(ddMenuItem);
    }

    /**
     * Adds Ipaddr RA menu item. It is called in swing thread.
     */
    private void addIpMenu(final ServiceInfo serviceInfo,
                           final MyMenu menu,
                           final Point2D pos,
                           final ResourceAgent ipService,
                           final Application.RunMode runMode) {
        final MyMenuItem ipMenuItem = menuFactory.createMenuItem(ipService.getPullDownMenuName(),
                                            null,
                                            null,
                                            new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                            new AccessMode(AccessMode.OP, AccessMode.NORMAL));
        ipMenuItem.addAction(new MenuAction() {
            @Override
            public void run(final String text) {
                serviceInfo.hidePopup();
                serviceInfo.addServicePanel(ipService,
                                            ipMenuItem.getPos(),
                                            true, /* colocation */
                                            true, /* order */
                                            true,
                                            false,
                                            runMode);
                serviceInfo.getBrowser().getCrmGraph().repaint();
            }
        });
        ipMenuItem.setPos(pos);
        menu.add(ipMenuItem);
    }

    /**
     * Adds Filesystem RA menu item. It is called in swing thread.
     */
    private void addFilesystemMenu(final ServiceInfo serviceInfo,
                                   final MyMenu menu,
                                   final Point2D pos,
                                   final ResourceAgent fsService,
                                   final Application.RunMode runMode) {
        final MyMenuItem fsMenuItem =
                menuFactory.createMenuItem(fsService.getPullDownMenuName(),
                                           null,
                                           null,
                                           new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                           new AccessMode(AccessMode.OP, AccessMode.NORMAL));
        fsMenuItem.addAction(new MenuAction() {
            @Override
            public void run(final String text) {
                serviceInfo.hidePopup();
                serviceInfo.addServicePanel(fsService,
                                            fsMenuItem.getPos(),
                                            true, /* colocation */
                                            true, /* order */
                                            true,
                                            false,
                                            runMode);
                serviceInfo.getBrowser().getCrmGraph().repaint();
            }
        });
        fsMenuItem.setPos(pos);
        menu.add(fsMenuItem);
    }

    /**
     * Adds resource agent RA menu item. It is called in swing thread.
     */
    private void addResourceAgentMenu(final ServiceInfo serviceInfo,
                                      final ResourceAgent ra,
                                      final MyListModel<MyMenuItem> dlm,
                                      final Point2D pos,
                                      final Iterable<JDialog> popups,
                                      final JCheckBox colocationWi,
                                      final JCheckBox orderWi,
                                      final Application.RunMode runMode) {
        final MyMenuItem resourceAgentMenu = menuFactory.createMenuItem(
                                                  ra.getPullDownMenuName(),
                                                  null,
                                                  null,
                                                  new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                                  new AccessMode(AccessMode.OP, AccessMode.NORMAL));
        resourceAgentMenu.addAction(new MenuAction() {
            @Override
            public void run(final String text) {
                serviceInfo.hidePopup();
                swingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        for (final JDialog otherP : popups) {
                            otherP.dispose();
                        }
                    }
                });
                if (ra.isLinbitDrbd() && !serviceInfo.getBrowser().linbitDrbdConfirmDialog()) {
                    return;
                } else if (ra.isHbDrbd() && !serviceInfo.getBrowser().hbDrbdConfirmDialog()) {
                    return;
                }
                serviceInfo.addServicePanel(ra,
                                            resourceAgentMenu.getPos(),
                                            colocationWi.isSelected(),
                                            orderWi.isSelected(),
                                            true,
                                            false,
                                            runMode);
                serviceInfo.getBrowser().getCrmGraph().repaint();
            }
        });
        resourceAgentMenu.setPos(pos);
        dlm.addElement(resourceAgentMenu);
    }


    /**
     * Adds menu items with dependend services and groups.
     */
    protected void addDependencyMenuItems(final ServiceInfo serviceInfo,
                                          final Collection<UpdatableItem> items,
                                          final boolean enableForNew,
                                          final Application.RunMode runMode) {
        /* add new group and dependency*/
        final MyMenuItem addGroupMenuItem =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.AddDependentGroup"),
                                           null,
                                           null,
                                           new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                           new AccessMode(AccessMode.OP, AccessMode.NORMAL));
        addGroupMenuItem.enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (serviceInfo.getBrowser().crmStatusFailed()) {
                            return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                        } else if (serviceInfo.getService().isRemoved()) {
                            return ServiceInfo.IS_BEING_REMOVED_STRING;
                        } else if (serviceInfo.getService().isOrphaned()) {
                            return ServiceInfo.IS_ORPHANED_STRING;
                        } else if (serviceInfo.getService().isNew()) {
                            return ServiceInfo.IS_NEW_STRING;
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        serviceInfo.hidePopup();
                        final CrmXml crmXML = serviceInfo.getBrowser().getCrmXml();
                        serviceInfo.addServicePanel(crmXML.getGroupResourceAgent(),
                                                    addGroupMenuItem.getPos(),
                                                    false, /* colocation only */
                                                    false, /* order only */
                                                    true,
                                                    false,
                                                    runMode);
                        serviceInfo.getBrowser().getCrmGraph().repaint();
                    }
                });
        items.add(addGroupMenuItem);

        /* add new service and dependency*/
        final MyMenu addServiceMenuItem = getAddServiceMenuItem(serviceInfo,
                                                                runMode,
                                                                Tools.getString("ClusterBrowser.Hb.AddDependency"));
        items.add(addServiceMenuItem);

        /* add existing service dependency*/
        final MyMenu existingServiceMenuItem = getExistingServiceMenuItem(
                                                     serviceInfo,
                                                     Tools.getString("ClusterBrowser.Hb.AddStartBefore"),
                                                     enableForNew,
                                                     runMode);
        items.add(existingServiceMenuItem);
    }
}
