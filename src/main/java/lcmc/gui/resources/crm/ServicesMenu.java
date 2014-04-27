package lcmc.gui.resources.crm;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JDialog;
import lcmc.EditClusterDialog;
import lcmc.Exceptions;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.CRMXML;
import lcmc.data.Host;
import lcmc.data.ResourceAgent;
import lcmc.gui.CRMGraph;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.dialog.ClusterLogs;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.CRM;
import lcmc.utilities.ComponentWithTest;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.MyList;
import lcmc.utilities.MyListModel;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

public class ServicesMenu {

    private static final Logger LOG =
                                  LoggerFactory.getLogger(ServicesInfo.class);

    private final ServicesInfo servicesInfo;
    
    public ServicesMenu(ServicesInfo servicesInfo) {
        super();
        this.servicesInfo = servicesInfo;
    }

    public List<UpdatableItem> getPulldownMenu() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final Application.RunMode runMode = Application.RunMode.LIVE;

        /* add group */
        final UpdatableItem addGroupMenuItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.AddGroup"),
                           null,
                           null,
                           new AccessMode(Application.AccessType.ADMIN,
                                          false),
                           new AccessMode(Application.AccessType.OP,
                                          false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    servicesInfo.hidePopup();
                    servicesInfo.addServicePanel(
                                    getBrowser().getCRMXML().getHbGroup(),
                                    getPos(),
                                    true,
                                    null,
                                    null,
                                    runMode);
                    getBrowser().getCRMGraph().repaint();
                }
            };
        items.add(addGroupMenuItem);

        /* add service */
        final UpdatableItem addServiceMenuItem = new MyMenu(
                        Tools.getString("ClusterBrowser.Hb.AddService"),
                        new AccessMode(Application.AccessType.OP,
                                       false),
                        new AccessMode(Application.AccessType.OP,
                                       false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getBrowser().getCRMXML() == null) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                return null;
            }

            @Override
            public void updateAndWait() {
                Tools.isSwingThread();
                removeAll();
                final Point2D pos = getPos();
                final CRMXML crmXML = getBrowser().getCRMXML();
                if (crmXML == null) {
                    return;
                }
                final ResourceAgent fsService = crmXML.getResourceAgent(
                                        "Filesystem",
                                        ResourceAgent.HEARTBEAT_PROVIDER,
                                        ResourceAgent.OCF_CLASS);
                if (crmXML.isLinbitDrbdPresent()) { /* just skip it,
                                                       if it is not */
                    final MyMenuItem ldMenuItem = new MyMenuItem(
                     Tools.getString("ClusterBrowser.linbitDrbdMenuName"),
                     null,
                     null,
                     new AccessMode(Application.AccessType.ADMIN,
                                    false),
                     new AccessMode(Application.AccessType.OP,
                                    false)) {
                        private static final long serialVersionUID = 1L;
                        @Override
                        public void action() {
                            servicesInfo.hidePopup();
                            if (!getBrowser().linbitDrbdConfirmDialog()) {
                                return;
                            }

                            final FilesystemInfo fsi = (FilesystemInfo)
                                                           servicesInfo.addServicePanel(
                                                                fsService,
                                                                getPos(),
                                                                true,
                                                                null,
                                                                null,
                                                                runMode);
                            fsi.setDrbddiskIsPreferred(false);
                            getBrowser().getCRMGraph().repaint();
                        }
                    };
                    if (getBrowser().atLeastOneDrbddisk()
                        || !crmXML.isLinbitDrbdPresent()) {
                        ldMenuItem.setEnabled(false);
                    }
                    ldMenuItem.setPos(pos);
                    add(ldMenuItem);
                }
                final ResourceAgent ipService = crmXML.getResourceAgent(
                                         "IPaddr2",
                                         ResourceAgent.HEARTBEAT_PROVIDER,
                                         ResourceAgent.OCF_CLASS);
                if (ipService != null) { /* just skip it, if it is not*/
                    final MyMenuItem ipMenuItem =
                         new MyMenuItem(ipService.getMenuName(),
                                        null,
                                        null,
                                        new AccessMode(
                                                  Application.AccessType.ADMIN,
                                                  false),
                                        new AccessMode(Application.AccessType.OP,
                                                       false)) {
                        private static final long serialVersionUID = 1L;
                        @Override
                        public void action() {
                            servicesInfo.hidePopup();
                            servicesInfo.addServicePanel(ipService,
                                                         getPos(),
                                                         true,
                                                         null,
                                                         null,
                                                         runMode);
                            getBrowser().getCRMGraph().repaint();
                        }
                    };
                    ipMenuItem.setPos(pos);
                    add(ipMenuItem);
                }
                if (crmXML.isDrbddiskPresent()
                    && (getBrowser().isDrbddiskPreferred()
                        || getBrowser().atLeastOneDrbddisk()
                        || !crmXML.isLinbitDrbdPresent())) {
                    final MyMenuItem ddMenuItem = new MyMenuItem(
                     Tools.getString("ClusterBrowser.DrbddiskMenuName"),
                     null,
                     null,
                     new AccessMode(Application.AccessType.ADMIN,
                                    false),
                     new AccessMode(Application.AccessType.OP,
                                    false)) {
                        private static final long serialVersionUID = 1L;
                        @Override
                        public void action() {
                            servicesInfo.hidePopup();
                            final FilesystemInfo fsi = (FilesystemInfo)
                                                           servicesInfo.addServicePanel(
                                                                fsService,
                                                                getPos(),
                                                                true,
                                                                null,
                                                                null,
                                                                runMode);
                            fsi.setDrbddiskIsPreferred(true);
                            getBrowser().getCRMGraph().repaint();
                        }
                    };
                    if (getBrowser().isOneLinbitDrbd()
                        || !crmXML.isDrbddiskPresent()) {
                        ddMenuItem.setEnabled(false);
                    }
                    ddMenuItem.setPos(pos);
                    add(ddMenuItem);
                }
                final Collection<JDialog> popups = new ArrayList<JDialog>();
                for (final String cl : ClusterBrowser.HB_CLASSES) {
                    final List<ResourceAgent> services = servicesInfo.getAddServiceList(cl);
                    if (services.isEmpty()) {
                        /* no services, don't show */
                        continue;
                    }
                    boolean mode = !AccessMode.ADVANCED;
                    if (ResourceAgent.UPSTART_CLASS.equals(cl)
                        || ResourceAgent.SYSTEMD_CLASS.equals(cl)) {
                        mode = AccessMode.ADVANCED;
                    }
                    if (ResourceAgent.LSB_CLASS.equals(cl)
                        && !servicesInfo.getAddServiceList(
                                    ResourceAgent.SERVICE_CLASS).isEmpty()) {
                        mode = AccessMode.ADVANCED;
                    }
                    final MyMenu classItem =
                            new MyMenu(ClusterBrowser.getClassMenu(cl),
                                       new AccessMode(
                                                   Application.AccessType.ADMIN,
                                                   mode),
                                       new AccessMode(Application.AccessType.OP,
                                                      mode));
                    final MyListModel<MyMenuItem> dlm = new MyListModel<MyMenuItem>();
                    for (final ResourceAgent ra : services) {
                        final MyMenuItem mmi =
                                new MyMenuItem(ra.getMenuName(),
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
                                servicesInfo.hidePopup();
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
                                    &&
                                     !getBrowser().hbDrbdConfirmDialog()) {
                                    return;
                                }
                                servicesInfo.addServicePanel(ra,
                                                             getPos(),
                                                             true,
                                                             null,
                                                             null,
                                                             runMode);
                                getBrowser().getCRMGraph().repaint();
                            }
                        };
                        mmi.setPos(pos);
                        dlm.addElement(mmi);
                    }
                    final boolean ret = Tools.getScrollingMenu(
                                        ClusterBrowser.getClassMenu(cl),
                                        null, /* options */
                                        classItem,
                                        dlm,
                                        new MyList<MyMenuItem>(dlm,
                                                               getBackground()),
                                        servicesInfo,
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
        items.add(addServiceMenuItem);

        /* add constraint placeholder (and) */
        final UpdatableItem addConstraintPlaceholderAnd =
            new MyMenuItem(Tools.getString(
                                 "ServicesInfo.AddConstraintPlaceholderAnd"),
                           null,
                           Tools.getString(
                            "ServicesInfo.AddConstraintPlaceholderAnd.ToolTip"),
                           new AccessMode(Application.AccessType.ADMIN,
                                          false),
                           new AccessMode(Application.AccessType.OP,
                                          false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    servicesInfo.hidePopup();
                    final CRMGraph hg = getBrowser().getCRMGraph();
                    final ConstraintPHInfo cphi =
                         new ConstraintPHInfo(getBrowser(),
                                              null,
                                              ConstraintPHInfo.Preference.AND);
                    cphi.getService().setNew(true);
                    getBrowser().addNameToServiceInfoHash(cphi);
                    hg.addConstraintPlaceholder(cphi, getPos(), runMode);
                    final PcmkRscSetsInfo prsi =
                                      new PcmkRscSetsInfo(getBrowser(), cphi);
                    cphi.setPcmkRscSetsInfo(prsi);
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            hg.scale();
                        }
                    });
                }
            };
        items.add(addConstraintPlaceholderAnd);

        /* add constraint placeholder (or) */
        final UpdatableItem addConstraintPlaceholderOr =
            new MyMenuItem(Tools.getString(
                                 "ServicesInfo.AddConstraintPlaceholderOr"),
                           null,
                           Tools.getString(
                            "ServicesInfo.AddConstraintPlaceholderOr.ToolTip"),
                           new AccessMode(Application.AccessType.ADMIN,
                                          false),
                           new AccessMode(Application.AccessType.OP,
                                          false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    final String pmV =
                                getBrowser().getDCHost().getPacemakerVersion();
                    try {
                        //TODO: get this from constraints-.rng files
                        if (pmV == null
                            || Tools.compareVersions(pmV, "1.1.7") <= 0) {
                            return HbOrderInfo.NOT_AVAIL_FOR_PCMK_VERSION;
                        }
                    } catch (final Exceptions.IllegalVersionException e) {
                        LOG.appWarning("enablePredicate: unkonwn version: "
                                       + pmV);
                        /* enable it, if version check doesn't work */
                    }
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    servicesInfo.hidePopup();
                    final CRMGraph hg = getBrowser().getCRMGraph();
                    final ConstraintPHInfo cphi =
                         new ConstraintPHInfo(getBrowser(),
                                              null,
                                              ConstraintPHInfo.Preference.OR);
                    cphi.getService().setNew(true);
                    getBrowser().addNameToServiceInfoHash(cphi);
                    hg.addConstraintPlaceholder(cphi, getPos(), runMode);
                    final PcmkRscSetsInfo prsi =
                                      new PcmkRscSetsInfo(getBrowser(), cphi);
                    cphi.setPcmkRscSetsInfo(prsi);
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            hg.scale();
                        }
                    });
                }
            };
        items.add(addConstraintPlaceholderOr);

        /* stop all services. */
        final ComponentWithTest stopAllMenuItem = new MyMenuItem(
                Tools.getString("ClusterBrowser.Hb.StopAllServices"),
                ServiceInfo.STOP_ICON,
                new AccessMode(Application.AccessType.ADMIN, true),
                new AccessMode(Application.AccessType.ADMIN, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                if (getBrowser().getExistingServiceList(null).isEmpty()) {
                    return "there are no services";
                }
                for (final ServiceInfo si
                        : getBrowser().getExistingServiceList(null)) {
                    if (!si.isStopped(Application.RunMode.LIVE) && !si.getService().isOrphaned()) {
                        return null;
                    }
                }
                return "all services are stopped";
            }

            @Override
            public void action() {
                servicesInfo.hidePopup();
                final Host dcHost = getBrowser().getDCHost();
                for (final ServiceInfo si
                        : getBrowser().getExistingServiceList(null)) {
                    if (si.getGroupInfo() == null
                        && !si.isStopped(Application.RunMode.LIVE)
                        && !si.getService().isOrphaned()
                        && !si.getService().isNew()) {
                        si.stopResource(dcHost, Application.RunMode.LIVE);
                    }
                }
                getBrowser().getCRMGraph().repaint();
            }
        };
        final ButtonCallback stopAllItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                final Host thisDCHost = getBrowser().getDCHost();
                for (final ServiceInfo si
                        : getBrowser().getExistingServiceList(null)) {
                    if (si.getGroupInfo() == null
                        && !si.isConstraintPH()
                        && !si.isStopped(Application.RunMode.TEST)
                        && !si.getService().isOrphaned()
                        && !si.getService().isNew()) {
                        si.stopResource(thisDCHost, Application.RunMode.TEST);
                    }
                }
            }
        };
        servicesInfo.addMouseOverListener(stopAllMenuItem, stopAllItemCallback);
        items.add((UpdatableItem) stopAllMenuItem);

        /* unmigrate all services. */
        final ComponentWithTest unmigrateAllMenuItem = new MyMenuItem(
                Tools.getString("ClusterBrowser.Hb.UnmigrateAllServices"),
                ServiceInfo.UNMIGRATE_ICON,
                new AccessMode(Application.AccessType.OP, false),
                new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                return enablePredicate() == null;
            }

            @Override
            public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                if (getBrowser().getExistingServiceList(null).isEmpty()) {
                    return "there are no services";
                }
                for (final ServiceInfo si
                                 : getBrowser().getExistingServiceList(null)) {
                    if (si.getMigratedTo(runMode) != null
                        || si.getMigratedFrom(runMode) != null) {
                        return null;
                    }
                }
                return "nothing to unmigrate";
            }

            @Override
            public void action() {
                servicesInfo.hidePopup();
                final Host dcHost = getBrowser().getDCHost();
                for (final ServiceInfo si
                                : getBrowser().getExistingServiceList(null)) {
                    if (si.getMigratedTo(runMode) != null
                        || si.getMigratedFrom(runMode) != null) {
                        si.unmigrateResource(dcHost, Application.RunMode.LIVE);
                    }
                }
                getBrowser().getCRMGraph().repaint();
            }
        };
        final ButtonCallback unmigrateAllItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                for (final ServiceInfo si
                                : getBrowser().getExistingServiceList(null)) {
                    if (si.getMigratedTo(runMode) != null
                        || si.getMigratedFrom(runMode) != null) {
                        si.unmigrateResource(dcHost, Application.RunMode.TEST);
                    }
                }
            }
        };
        servicesInfo.addMouseOverListener(unmigrateAllMenuItem, unmigrateAllItemCallback);
        items.add((UpdatableItem) unmigrateAllMenuItem);

        /* remove all services. */
        final ComponentWithTest removeMenuItem = new MyMenuItem(
                Tools.getString("ClusterBrowser.Hb.RemoveAllServices"),
                ClusterBrowser.REMOVE_ICON,
                new AccessMode(Application.AccessType.ADMIN, true),
                new AccessMode(Application.AccessType.ADMIN, true)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                if (getBrowser().getExistingServiceList(null).isEmpty()) {
                    return "there are no services";
                }
                for (final ServiceInfo si
                        : getBrowser().getExistingServiceList(null)) {
                    if (si.getGroupInfo() == null) {
                        if (si.isRunning(Application.RunMode.LIVE)) {
                            return "there are running services";
                        }
                    }
                }
                return null;
            }

            @Override
            public void action() {
                servicesInfo.hidePopup();
                if (Tools.confirmDialog(
                     Tools.getString(
                         "ClusterBrowser.confirmRemoveAllServices.Title"),
                     Tools.getString(
                     "ClusterBrowser.confirmRemoveAllServices.Description"),
                     Tools.getString(
                         "ClusterBrowser.confirmRemoveAllServices.Yes"),
                     Tools.getString(
                         "ClusterBrowser.confirmRemoveAllServices.No"))) {
                    final Thread t = new Thread() {
                        @Override
                        public void run() {
                            final Host dcHost = getBrowser().getDCHost();
                            final List<ServiceInfo> services =
                                    getBrowser().getExistingServiceList(null);
                            for (final ServiceInfo si : services) {
                                if (si.getGroupInfo() == null) {
                                    final ResourceAgent ra =
                                                        si.getResourceAgent();
                                    if (ra != null && !ra.isClone()) {
                                        si.getService().setRemoved(true);
                                    }
                                }
                            }
                            CRM.erase(dcHost, runMode);
                            for (final ServiceInfo si : services) {
                                if (si.getGroupInfo() == null) {
                                    final ResourceAgent ra =
                                                        si.getResourceAgent();
                                    if (si.getService().isNew()) {
                                        si.removeMyself(runMode);
                                    } else if (ra != null && !ra.isClone()) {
                                        si.cleanupResource(dcHost, Application.RunMode.LIVE);
                                    }
                                }
                            }
                            getBrowser().getCRMGraph().repaint();
                        }
                    };
                    t.start();
                }
            }
        };
        final ButtonCallback removeItemCallback =
                                    getBrowser().new ClMenuItemCallback(null) {
            @Override
            public void action(final Host dcHost) {
                CRM.erase(dcHost, Application.RunMode.TEST);
            }
        };
        servicesInfo.addMouseOverListener(removeMenuItem, removeItemCallback);
        items.add((UpdatableItem) removeMenuItem);

        /* cluster wizard */
        final UpdatableItem clusterWizardItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.ClusterWizard"),
                           ServicesInfo.CLUSTER_ICON,
                           null,
                           new AccessMode(Application.AccessType.ADMIN,
                                          AccessMode.ADVANCED),
                           new AccessMode(Application.AccessType.ADMIN,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void action() {
                    final EditClusterDialog dialog =
                              new EditClusterDialog(getBrowser().getCluster());
                    dialog.showDialogs();
                }
            };
        items.add(clusterWizardItem);

        /* view logs */
        final UpdatableItem viewLogsItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.ViewLogs"),
                           ServicesInfo.LOGFILE_ICON,
                           null,
                           new AccessMode(Application.AccessType.RO,
                                          false),
                           new AccessMode(Application.AccessType.RO,
                                          false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void action() {
                    final ClusterLogs l = new ClusterLogs(getBrowser().getCluster());
                    l.showDialog();
                }
            };
        items.add(viewLogsItem);
        return items;
    }
    
    private ClusterBrowser getBrowser() {
        return servicesInfo.getBrowser();
    }

}
