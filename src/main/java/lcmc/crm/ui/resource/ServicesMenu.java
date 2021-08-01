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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.JDialog;

import lcmc.Exceptions;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.wizard.EditClusterDialog;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.ui.utils.ComponentWithTest;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyList;
import lcmc.common.ui.utils.MyListModel;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.crm.domain.CrmXml;
import lcmc.crm.domain.ResourceAgent;
import lcmc.crm.service.CRM;
import lcmc.crm.ui.ClusterLogs;
import lcmc.crm.ui.CrmGraph;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

@Named
public class ServicesMenu {

    private static final Logger LOG = LoggerFactory.getLogger(ServicesInfo.class);

    private final EditClusterDialog editClusterDialog;
    private final MainData mainData;
    private final Provider<ConstraintPHInfo> constraintPHInfoProvider;
    private final Provider<PcmkRscSetsInfo> rscSetsInfoProvider;
    private final MenuFactory menuFactory;
    private final Application application;
    private final SwingUtils swingUtils;
    private final Provider<ClusterLogs> clusterLogsProvider;

    public ServicesMenu(EditClusterDialog editClusterDialog, MainData mainData, Provider<ConstraintPHInfo> constraintPHInfoProvider,
            Provider<PcmkRscSetsInfo> rscSetsInfoProvider, MenuFactory menuFactory, Application application, SwingUtils swingUtils,
            @Named("clusterLogs") Provider<ClusterLogs> clusterLogsProvider) {
        this.editClusterDialog = editClusterDialog;
        this.mainData = mainData;
        this.constraintPHInfoProvider = constraintPHInfoProvider;
        this.rscSetsInfoProvider = rscSetsInfoProvider;
        this.menuFactory = menuFactory;
        this.application = application;
        this.swingUtils = swingUtils;
        this.clusterLogsProvider = clusterLogsProvider;
    }

    public List<UpdatableItem> getPulldownMenu(final ServicesInfo servicesInfo) {
        final List<UpdatableItem> items = new ArrayList<>();
        final Application.RunMode runMode = Application.RunMode.LIVE;

        /* add group */
        final MyMenuItem addGroupMenuItem = menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.AddGroup"), null, null,
                                                               new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                                                       .enablePredicate(() -> {
                                                           if (servicesInfo.getBrowser()
                                                                           .crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    return null;
                });
        addGroupMenuItem.addAction(text -> {
            servicesInfo.hidePopup();
            servicesInfo.addServicePanel(servicesInfo.getBrowser().getCrmXml().getGroupResourceAgent(), addGroupMenuItem.getPos(),
                    true, null, null, runMode);
            servicesInfo.getBrowser().getCrmGraph().repaint();

        });
        items.add(addGroupMenuItem);

        /* add service */
        final MyMenu addServiceMenuItem = menuFactory.createMenu(Tools.getString("ClusterBrowser.Hb.AddService"),
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                .enablePredicate(() -> {
                    if (servicesInfo.getBrowser().getCrmXml() == null) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    if (servicesInfo.getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    return null;
                });
        addServiceMenuItem.onUpdate(() -> {
            swingUtils.isSwingThread();
            addServiceMenuItem.removeAll();
            final Point2D pos = addServiceMenuItem.getPos();
            final CrmXml crmXML = servicesInfo.getBrowser().getCrmXml();
            if (crmXML == null) {
                return;
            }
            final ResourceAgent fsService =
                    crmXML.getResourceAgent("Filesystem", ResourceAgent.HEARTBEAT_PROVIDER, ResourceAgent.OCF_CLASS_NAME);
            if (crmXML.isLinbitDrbdResourceAgentPresent()) { /* just skip it, if it is not */
                final MyMenuItem ldMenuItem =
                        menuFactory.createMenuItem(Tools.getString("ClusterBrowser.linbitDrbdMenuName"), null, null,
                                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                new AccessMode(AccessMode.OP, AccessMode.NORMAL)).addAction(text -> {
                            servicesInfo.hidePopup();
                            if (!servicesInfo.getBrowser().linbitDrbdConfirmDialog()) {
                                return;
                            }

                            final FilesystemRaInfo fsi =
                                    (FilesystemRaInfo) servicesInfo.addServicePanel(fsService, addServiceMenuItem.getPos(), true,
                                            null, null, runMode);
                            fsi.setDrbddiskIsPreferred(false);
                            servicesInfo.getBrowser().getCrmGraph().repaint();
                        });
                if (servicesInfo.getBrowser().atLeastOneDrbddiskConfigured() || !crmXML.isLinbitDrbdResourceAgentPresent()) {
                    ldMenuItem.setEnabled(false);
                }
                ldMenuItem.setPos(pos);
                addServiceMenuItem.add(ldMenuItem);
            }
            final ResourceAgent ipService =
                    crmXML.getResourceAgent("IPaddr2", ResourceAgent.HEARTBEAT_PROVIDER, ResourceAgent.OCF_CLASS_NAME);
            if (ipService != null) { /* just skip it, if it is not*/
                final MyMenuItem ipMenuItem = menuFactory.createMenuItem(ipService.getPullDownMenuName(), null, null,
                                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .addAction(text -> {
                            servicesInfo.hidePopup();
                            servicesInfo.addServicePanel(ipService, addServiceMenuItem.getPos(), true, null, null, runMode);
                            servicesInfo.getBrowser().getCrmGraph().repaint();
                        });
                ipMenuItem.setPos(pos);
                addServiceMenuItem.add(ipMenuItem);
            }
            if (crmXML.isDrbddiskResourceAgentPresent() && (servicesInfo.getBrowser().isDrbddiskRAPreferred()
                                                            || servicesInfo.getBrowser().atLeastOneDrbddiskConfigured()
                                                            || !crmXML.isLinbitDrbdResourceAgentPresent())) {
                final MyMenuItem ddMenuItem =
                        menuFactory.createMenuItem(Tools.getString("ClusterBrowser.DrbddiskMenuName"), null, null,
                                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                new AccessMode(AccessMode.OP, AccessMode.NORMAL)).addAction(text -> {
                            servicesInfo.hidePopup();
                            final FilesystemRaInfo fsi =
                                    (FilesystemRaInfo) servicesInfo.addServicePanel(fsService, addServiceMenuItem.getPos(), true,
                                            null, null, runMode);
                            fsi.setDrbddiskIsPreferred(true);
                            servicesInfo.getBrowser().getCrmGraph().repaint();
                        });
                if (servicesInfo.getBrowser().isOneLinbitDrbdRaConfigured() || !crmXML.isDrbddiskResourceAgentPresent()) {
                    ddMenuItem.setEnabled(false);
                }
                ddMenuItem.setPos(pos);
                addServiceMenuItem.add(ddMenuItem);
            }
            final Collection<JDialog> popups = new ArrayList<>();
            for (final String cl : ClusterBrowser.CRM_CLASSES) {
                final List<ResourceAgent> services = servicesInfo.getAddServiceList(cl);
                if (services.isEmpty()) {
                    /* no services, don't show */
                    continue;
                }
                AccessMode.Mode mode = AccessMode.NORMAL;
                if (ResourceAgent.UPSTART_CLASS_NAME.equals(cl) || ResourceAgent.SYSTEMD_CLASS_NAME.equals(cl)) {
                    mode = AccessMode.ADVANCED;
                }
                if (ResourceAgent.LSB_CLASS_NAME.equals(cl) && !servicesInfo.getAddServiceList(ResourceAgent.SERVICE_CLASS_NAME)
                        .isEmpty()) {
                    mode = AccessMode.ADVANCED;
                }
                final MyMenu classItem =
                        menuFactory.createMenu(ClusterBrowser.getClassMenuName(cl), new AccessMode(AccessMode.ADMIN, mode),
                                new AccessMode(AccessMode.OP, mode));
                final MyListModel<MyMenuItem> dlm = new MyListModel<>();
                for (final ResourceAgent ra : services) {
                    final MyMenuItem mmi = menuFactory.createMenuItem(ra.getPullDownMenuName(), null, null,
                                    new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                            .addAction(text -> {
                                servicesInfo.hidePopup();
                                swingUtils.invokeLater(() -> {
                                    for (final JDialog otherP : popups) {
                                        otherP.dispose();
                                    }
                                });
                                if (ra.isLinbitDrbd() && !servicesInfo.getBrowser().linbitDrbdConfirmDialog()) {
                                    return;
                                } else if (ra.isHbDrbd() && !servicesInfo.getBrowser().hbDrbdConfirmDialog()) {
                                    return;
                                }
                                servicesInfo.addServicePanel(ra, addServiceMenuItem.getPos(), true, null, null, runMode);
                                servicesInfo.getBrowser().getCrmGraph().repaint();
                            });
                    mmi.setPos(pos);
                    dlm.addElement(mmi);
                }
                final boolean ret = mainData.getScrollingMenu(ClusterBrowser.getClassMenuName(cl), null, /* options */
                        classItem, dlm, new MyList<>(dlm, addServiceMenuItem.getBackground()), servicesInfo, popups, null);
                if (!ret) {
                    classItem.setEnabled(false);
                }
                addServiceMenuItem.add(classItem);
            }
            addServiceMenuItem.updateMenuComponents();
            addServiceMenuItem.processAccessMode();
        });
        items.add(addServiceMenuItem);

        /* add constraint placeholder (and) */
        final MyMenuItem addConstraintPlaceholderAnd =
                menuFactory.createMenuItem(Tools.getString("ServicesInfo.AddConstraintPlaceholderAnd"), null,
                                Tools.getString("ServicesInfo.AddConstraintPlaceholderAnd.ToolTip"),
                                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .enablePredicate(() -> {
                            if (servicesInfo.getBrowser().crmStatusFailed()) {
                                return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                            }
                            return null;
                        });
        addConstraintPlaceholderAnd.addAction(text -> {
            servicesInfo.hidePopup();
            final CrmGraph hg = servicesInfo.getBrowser().getCrmGraph();
            final ConstraintPHInfo constraintPHInfo = constraintPHInfoProvider.get();
            constraintPHInfo.init(servicesInfo.getBrowser(), null, ConstraintPHInfo.Preference.AND);
            constraintPHInfo.getService().setNew(true);
            servicesInfo.getBrowser().addNameToServiceInfoHash(constraintPHInfo);
            hg.addConstraintPlaceholder(constraintPHInfo, addConstraintPlaceholderAnd.getPos(), runMode);
            final PcmkRscSetsInfo rscSetsInfo = rscSetsInfoProvider.get();
            rscSetsInfo.init(servicesInfo.getBrowser(), constraintPHInfo);
            constraintPHInfo.setPcmkRscSetsInfo(rscSetsInfo);
            swingUtils.invokeLater(hg::scale);
        });
        items.add(addConstraintPlaceholderAnd);

        /* add constraint placeholder (or) */
        final MyMenuItem addConstraintPlaceholderOr =
                menuFactory.createMenuItem(Tools.getString("ServicesInfo.AddConstraintPlaceholderOr"), null,
                                Tools.getString("ServicesInfo.AddConstraintPlaceholderOr.ToolTip"),
                                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .enablePredicate(() -> {
                            final String pmV = servicesInfo.getBrowser().getDCHost().getHostParser().getPacemakerVersion();
                            try {
                                //TODO: get this from constraints-.rng files
                                if (pmV == null || Tools.compareVersions(pmV, "1.1.7") <= 0) {
                                    return HbOrderInfo.NOT_AVAIL_FOR_PCMK_VERSION;
                                }
                            } catch (final Exceptions.IllegalVersionException e) {
                                LOG.appWarning("enablePredicate: unkonwn version: " + pmV);
                                /* enable it, if version check doesn't work */
                            }
                            if (servicesInfo.getBrowser().crmStatusFailed()) {
                                return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                            }
                            return null;
                        });
        addConstraintPlaceholderOr.addAction(text -> {
            servicesInfo.hidePopup();
            final CrmGraph hg = servicesInfo.getBrowser().getCrmGraph();
            final ConstraintPHInfo constraintPHInfo = constraintPHInfoProvider.get();
            constraintPHInfo.init(servicesInfo.getBrowser(), null, ConstraintPHInfo.Preference.OR);
            constraintPHInfo.getService().setNew(true);
            servicesInfo.getBrowser().addNameToServiceInfoHash(constraintPHInfo);
            hg.addConstraintPlaceholder(constraintPHInfo, addConstraintPlaceholderOr.getPos(), runMode);
            final PcmkRscSetsInfo rscSetsInfo = rscSetsInfoProvider.get();
            rscSetsInfo.init(servicesInfo.getBrowser(), constraintPHInfo);
            constraintPHInfo.setPcmkRscSetsInfo(rscSetsInfo);
            swingUtils.invokeLater(hg::scale);
        });
        items.add(addConstraintPlaceholderOr);

        /* stop all services. */
        final ComponentWithTest stopAllMenuItem =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.StopAllServices"), ServiceInfo.STOP_ICON,
                                new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED), new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL))
                        .enablePredicate(() -> {
                            if (servicesInfo.getBrowser().crmStatusFailed()) {
                                return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                            }
                            if (servicesInfo.getBrowser().getExistingServiceList(null).isEmpty()) {
                                return "there are no services";
                            }
                            for (final ServiceInfo si : servicesInfo.getBrowser().getExistingServiceList(null)) {
                                if (!si.isStopped(Application.RunMode.LIVE) && !si.getService().isOrphaned()) {
                                    return null;
                                }
                            }
                            return "all services are stopped";
                        })
                        .addAction(text -> {
                            servicesInfo.hidePopup();
                            final Host dcHost = servicesInfo.getBrowser().getDCHost();
                            for (final ServiceInfo si : servicesInfo.getBrowser().getExistingServiceList(null)) {
                                if (si.getGroupInfo() == null && !si.isStopped(Application.RunMode.LIVE) && !si.getService()
                                        .isOrphaned() && !si.getService().isNew()) {
                                    si.stopResource(dcHost, Application.RunMode.LIVE);
                                }
                            }
                            servicesInfo.getBrowser().getCrmGraph().repaint();
                        });
        final ButtonCallback stopAllItemCallback = servicesInfo.getBrowser().new ClMenuItemCallback(null).addAction(host -> {
            final Host thisDCHost = servicesInfo.getBrowser().getDCHost();
            for (final ServiceInfo si : servicesInfo.getBrowser().getExistingServiceList(null)) {
                if (si.getGroupInfo() == null && !si.isConstraintPlaceholder() && !si.isStopped(Application.RunMode.TEST)
                    && !si.getService().isOrphaned() && !si.getService().isNew()) {
                    si.stopResource(thisDCHost, Application.RunMode.TEST);
                }
            }
        });
        servicesInfo.addMouseOverListener(stopAllMenuItem, stopAllItemCallback);
        items.add((UpdatableItem) stopAllMenuItem);

        /* unmigrate all services. */
        final ComponentWithTest unmigrateAllMenuItem =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.UnmigrateAllServices"), ServiceInfo.UNMIGRATE_ICON,
                                new AccessMode(AccessMode.OP, AccessMode.NORMAL), new AccessMode(AccessMode.OP, AccessMode.NORMAL))
                        .visiblePredicate(() -> {
                            if (servicesInfo.getBrowser().crmStatusFailed()) {
                                return false;
                            }
                            if (servicesInfo.getBrowser().getExistingServiceList(null).isEmpty()) {
                                return false;
                            }
                            for (final ServiceInfo si : servicesInfo.getBrowser().getExistingServiceList(null)) {
                                if (si.getMigratedTo(runMode) != null || si.getMigratedFrom(runMode) != null) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .addAction(text -> {
                            servicesInfo.hidePopup();
                            final Host dcHost = servicesInfo.getBrowser().getDCHost();
                            for (final ServiceInfo si : servicesInfo.getBrowser().getExistingServiceList(null)) {
                                if (si.getMigratedTo(runMode) != null || si.getMigratedFrom(runMode) != null) {
                                    si.unmigrateResource(dcHost, Application.RunMode.LIVE);
                                }
                            }
                            servicesInfo.getBrowser().getCrmGraph().repaint();
                        });
        final ButtonCallback unmigrateAllItemCallback = servicesInfo.getBrowser().new ClMenuItemCallback(null).addAction(dcHost -> {
            for (final ServiceInfo si : servicesInfo.getBrowser().getExistingServiceList(null)) {
                if (si.getMigratedTo(runMode) != null || si.getMigratedFrom(runMode) != null) {
                    si.unmigrateResource(dcHost, Application.RunMode.TEST);
                }
            }
        });
        servicesInfo.addMouseOverListener(unmigrateAllMenuItem, unmigrateAllItemCallback);
        items.add((UpdatableItem) unmigrateAllMenuItem);

        /* remove all services. */
        final ComponentWithTest removeMenuItem =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.RemoveAllServices"), ClusterBrowser.REMOVE_ICON,
                        new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED),
                        new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED)).enablePredicate(() -> {
                    if (servicesInfo.getBrowser().crmStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    if (servicesInfo.getBrowser().getExistingServiceList(null).isEmpty()) {
                        return "there are no services";
                    }
                    for (final ServiceInfo si : servicesInfo.getBrowser().getExistingServiceList(null)) {
                        if (si.getGroupInfo() == null) {
                            if (si.isRunning(Application.RunMode.LIVE)) {
                                return "there are running services";
                            }
                        }
                    }
                    return null;
                }).addAction(text -> {
                    servicesInfo.hidePopup();
                    if (application.confirmDialog(Tools.getString("ClusterBrowser.confirmRemoveAllServices.Title"),
                            Tools.getString("ClusterBrowser.confirmRemoveAllServices.Description"),
                            Tools.getString("ClusterBrowser.confirmRemoveAllServices.Yes"),
                            Tools.getString("ClusterBrowser.confirmRemoveAllServices.No"))) {
                        final Thread t = new Thread(() -> {
                            final Host dcHost = servicesInfo.getBrowser().getDCHost();
                            final List<ServiceInfo> services = servicesInfo.getBrowser().getExistingServiceList(null);
                            for (final ServiceInfo si : services) {
                                if (si.getGroupInfo() == null) {
                                    final ResourceAgent ra = si.getResourceAgent();
                                    if (ra != null && !ra.isClone()) {
                                        si.getService().setRemoved(true);
                                    }
                                }
                            }
                            CRM.erase(dcHost, runMode);
                            for (final ServiceInfo si : services) {
                                if (si.getGroupInfo() == null) {
                                    final ResourceAgent ra = si.getResourceAgent();
                                    if (si.getService().isNew()) {
                                        si.removeMyself(runMode);
                                    } else if (ra != null && !ra.isClone()) {
                                        si.cleanupResource(dcHost, Application.RunMode.LIVE);
                                    }
                                }
                            }
                            servicesInfo.getBrowser().getCrmGraph().repaint();
                        });
                        t.start();
                    }
                });
        final ButtonCallback removeItemCallback = servicesInfo.getBrowser().new ClMenuItemCallback(null).addAction(
                dcHost -> CRM.erase(dcHost, Application.RunMode.TEST));
        servicesInfo.addMouseOverListener(removeMenuItem, removeItemCallback);
        items.add((UpdatableItem) removeMenuItem);

        /* cluster wizard */
        final UpdatableItem clusterWizardItem =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.ClusterWizard"), ServicesInfo.CLUSTER_ICON, null,
                                new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED), new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL))
                        .addAction(text -> editClusterDialog.showDialogs(servicesInfo.getBrowser().getCluster()));
        items.add(clusterWizardItem);

        /* view logs */

        final UpdatableItem viewLogsItem =
                menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.ViewLogs"), ServicesInfo.LOGFILE_ICON, null,
                                new AccessMode(AccessMode.RO, AccessMode.NORMAL), new AccessMode(AccessMode.RO, AccessMode.NORMAL))
                        .addAction(text -> {
                            final ClusterLogs clusterLogs = clusterLogsProvider.get();
                            clusterLogs.init(servicesInfo.getBrowser().getCluster());
                            clusterLogs.showDialog();
                        });
        items.add(viewLogsItem);

        /* export image  */
        final UpdatableItem savePNGItem = menuFactory.createMenuItem(Tools.getString("ClusterBrowser.Hb.ExportGraph"), null,
                        new AccessMode(AccessMode.RO, AccessMode.NORMAL), new AccessMode(AccessMode.RO, AccessMode.NORMAL))
                .addAction(text -> servicesInfo.exportGraphAsPng());
        items.add(savePNGItem);
        return items;
    }

}
