/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2016, Rastislav Levrinc.
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

package lcmc.crm.ui.resource.update;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.Application;
import lcmc.crm.domain.ClusterStatus;
import lcmc.crm.domain.CrmXml;
import lcmc.crm.domain.ResourceAgent;
import lcmc.crm.domain.RscSetConnectionData;
import lcmc.crm.ui.CrmGraph;
import lcmc.crm.ui.resource.CloneInfo;
import lcmc.crm.ui.resource.ConstraintPHInfo;
import lcmc.crm.ui.resource.CrmServiceFactory;
import lcmc.crm.ui.resource.DrbddiskInfo;
import lcmc.crm.ui.resource.FilesystemRaInfo;
import lcmc.crm.ui.resource.GroupInfo;
import lcmc.crm.ui.resource.LinbitDrbdInfo;
import lcmc.crm.ui.resource.PcmkRscSetsInfo;
import lcmc.crm.ui.resource.ServiceInfo;
import lcmc.crm.ui.resource.ServicesInfo;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Named
public class ResourceUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceUpdater.class);

    private ClusterBrowser browser;
    private ClusterStatus clusterStatus;
    private Application.RunMode runMode;
    private ServicesInfo servicesInfo;
    @Inject
    private Provider<ConstraintPHInfo> constraintPHInfoProvider;
    @Inject
    private Provider<PcmkRscSetsInfo> pcmkRscSetsInfoProvider;
    @Inject
    private Application application;
    @Inject
    private CrmServiceFactory crmServiceFactory;
    private CrmGraph crmGraph;
    private Set<String> allGroupsAndClones;
    private List<ServiceInfo> groupServiceIsPresent = Lists.newArrayList();
    private List<ServiceInfo> serviceIsPresent = Lists.newArrayList();

    /**
     * This functions goes through all services, constrains etc. in
     * clusterStatus and updates the internal structures and graph.
     */
    public void updateAllResources(final ServicesInfo servicesInfo,
                                   final ClusterBrowser browser,
                                   final ClusterStatus clusterStatus,
                                   final Application.RunMode runMode) {
        this.servicesInfo = servicesInfo;
        this.browser = browser;
        this.clusterStatus = clusterStatus;
        this.runMode = runMode;

        allGroupsAndClones = clusterStatus.getAllGroupsAndClones();
        crmGraph = browser.getCrmGraph();

        for (final String groupOrClone : allGroupsAndClones) {
            updateGroupOrClone(groupOrClone);
        }

        crmGraph.clearKeepColocationList();
        crmGraph.clearKeepOrderList();
        /* resource sets */
        final List<RscSetConnectionData> newRscSetConnections = clusterStatus.getRscSetConnections();
        if (newRscSetConnections != null) {
            final RscSetUpdater rscSetUpdater = new RscSetUpdater(runMode, browser, constraintPHInfoProvider, pcmkRscSetsInfoProvider);
            rscSetUpdater.update(newRscSetConnections);
            serviceIsPresent.addAll(rscSetUpdater.getServiceIsPresent());
        }

        updateColocations();

        /* orders */
        updateOrders();

        servicesInfo.cleanupServiceMenu(groupServiceIsPresent);
        crmGraph.updateRemovedElements(serviceIsPresent);
    }

    private void updateOrders() {
        final Map<String, List<CrmXml.OrderData>> orderMap = clusterStatus.getOrderRscMap();
        for (final Map.Entry<String, List<CrmXml.OrderData>> orderEntry : orderMap.entrySet()) {
            for (final CrmXml.OrderData data : orderEntry.getValue()) {
                final String rscThenId = data.getRscThen();
                final ServiceInfo si = this.browser.getServiceInfoFromCRMId(rscThenId);
                if (si != null) { /* not yet complete */
                    final ServiceInfo siP = this.browser.getServiceInfoFromCRMId(orderEntry.getKey());
                    if (siP != null && siP.getResourceAgent() != null) {
                        /* dangling orders and colocations */
                        if ((siP.getResourceAgent().isDrbddisk() || siP.getResourceAgent().isLinbitDrbd())
                                && "Filesystem".equals(si.getName())) {
                            final List<CrmXml.ColocationData> cds = clusterStatus.getColocationDatas(orderEntry.getKey());
                            if (cds != null) {
                                for (final CrmXml.ColocationData cd : cds) {
                                    if (cd.getWithRsc().equals(rscThenId)) {
                                        setFilesystemWithDrbd(siP, si);
                                    }
                                }
                            }
                        }
                        crmGraph.addOrder(data.getId(), siP, si);
                    }
                }
            }
        }
    }

    private void updateColocations() {
        final Map<String, List<CrmXml.ColocationData>> colocationMap = clusterStatus.getColocationRscMap();
        for (final Map.Entry<String, List<CrmXml.ColocationData>> colocationEntry : colocationMap.entrySet()) {
            final List<CrmXml.ColocationData> withs = colocationEntry.getValue();
            for (final CrmXml.ColocationData colocationData : withs) {
                final String withRscId = colocationData.getWithRsc();
                final ServiceInfo withSi = this.browser.getServiceInfoFromCRMId(withRscId);
                final ServiceInfo siP = this.browser.getServiceInfoFromCRMId(colocationEntry.getKey());
                crmGraph.addColocation(colocationData.getId(), siP, withSi);
            }
        }
    }

    private void updateGroupOrClone(String groupOrClone) {
        GroupInfo newGi = null;
        CloneInfo newCi = null;
        if (clusterStatus.isClone(groupOrClone)) {
            /* clone */
            newCi = setCreateCloneInfo(groupOrClone);
            serviceIsPresent.add(newCi);
        } else if (!"none".equals(groupOrClone)) {
            /* group */
            final GroupInfo gi = (GroupInfo) this.browser.getServiceInfoFromCRMId(groupOrClone);
            if (gi != null && gi.getCloneInfo() != null) {
                /* cloned group is already done */
                groupServiceIsPresent.add(gi);
                return;
            }
            newGi = setCreateGroupInfo(groupOrClone, null);
            serviceIsPresent.add(newGi);
        }
        setGroupResources(groupOrClone, newGi, newCi);
    }

    /**
     * Sets clone info object.
     */
    private CloneInfo setCreateCloneInfo(final String cloneId) {
        CloneInfo newCi = (CloneInfo) browser.getServiceInfoFromCRMId(cloneId);
        if (newCi == null) {
            final Point2D p = null;
            newCi = (CloneInfo) servicesInfo.addServicePanel(browser.getCloneResourceAgent(),
                            p,
                            false,
                            cloneId,
                            null,
                            runMode);
            browser.addToHeartbeatIdList(newCi);
            final Map<String, String> resourceNode = clusterStatus.getParamValuePairs(newCi.getHeartbeatId(runMode));
            newCi.setParameters(resourceNode);
        } else {
            final Map<String, String> resourceNode = clusterStatus.getParamValuePairs(newCi.getHeartbeatId(runMode));
            newCi.setParameters(resourceNode);
            if (Application.isLive(runMode)) {
                newCi.setUpdated(false);
                browser.repaint();
            }
        }
        newCi.setNew(false);
        return newCi;
    }

    private GroupInfo setCreateGroupInfo(final String group,
                                         final CloneInfo newCi) {
        GroupInfo newGi = (GroupInfo) browser.getServiceInfoFromCRMId(group);
        if (newGi == null) {
            final Point2D p = null;
            newGi = (GroupInfo) servicesInfo.addServicePanel(browser.getGroupResourceAgent(),
                            p,
                            false,
                            group,
                            newCi,
                            runMode);
            final Map<String, String> resourceNode = clusterStatus.getParamValuePairs(newGi.getHeartbeatId(runMode));
            newGi.setParameters(resourceNode);
            if (newCi != null) {
                newCi.addCloneServicePanel(newGi);
            }
        } else {
            final Map<String, String> resourceNode = clusterStatus.getParamValuePairs(newGi.getHeartbeatId(runMode));
            newGi.setParameters(resourceNode);
            if (Application.isLive(runMode)) {
                newGi.setUpdated(false);
                browser.repaint();
            }
        }
        newGi.setNew(false);
        return newGi;
    }

    private void setGroupResources(final String grpOrCloneId,
                                   final GroupInfo newGi,
                                   final CloneInfo newCi) {
        final Map<ServiceInfo, Map<String, String>> setParametersHash = new HashMap<ServiceInfo, Map<String, String>>();
        if (newCi != null) {
            setParametersHash.put(newCi, clusterStatus.getParamValuePairs(grpOrCloneId));
        } else if (newGi != null) {
            setParametersHash.put(newGi, clusterStatus.getParamValuePairs(grpOrCloneId));
        }
        final Optional<List<String>> groupResources = clusterStatus.getGroupResources(grpOrCloneId, runMode);
        if (!groupResources.isPresent()) {
            return;
        }
        boolean newService = false;
        int pos = 0;
        for (final String hbId : groupResources.get()) {
            if (clusterStatus.isOrphaned(hbId) && application.isHideLRM()) {
                continue;
            }
            ServiceInfo newServiceInfo;
            if (allGroupsAndClones.contains(hbId)) {
                /* clone group */
                final GroupInfo gi = setCreateGroupInfo(hbId, newCi);
                setGroupResources(
                        hbId,
                        gi,
                        null
                );
                newServiceInfo = gi;
            } else {
                final ResourceAgent newResourceAgent = clusterStatus.getResourceType(hbId);
                if (newResourceAgent == null) {
                    /* This is bad. There is a service but we do not have
                     * the heartbeat script of this service or the we look
                     * in the wrong places.
                     */
                    LOG.appWarning("setGroupResources: " + hbId + ": could not find resource agent");
                }
                /* continue of creating/updating of the
                 * service in the gui.
                 */
                newServiceInfo = browser.getServiceInfoFromCRMId(hbId);
                final Map<String, String> resourceNode = clusterStatus.getParamValuePairs(hbId);
                if (newServiceInfo == null) {
                    newService = true;
                    newServiceInfo = crmServiceFactory.createServiceWithParameters(
                            hbId,
                            newResourceAgent,
                            resourceNode,
                            browser);
                    newServiceInfo.setCrmId(hbId);
                    browser.addToHeartbeatIdList(newServiceInfo);
                    if (newGi != null) {
                        newGi.addGroupServicePanel(newServiceInfo, false);
                    } else if (newCi != null) {
                        newCi.addCloneServicePanel(newServiceInfo);
                    } else {
                        final Point2D p = null;
                        servicesInfo.addServicePanel(newServiceInfo, p, false, false, runMode);
                    }
                } else {
                    browser.addNameToServiceInfoHash(newServiceInfo);
                    setParametersHash.put(newServiceInfo, resourceNode);
                }
                newServiceInfo.setNew(false);
                serviceIsPresent.add(newServiceInfo);
                if (newGi != null || newCi != null) {
                    groupServiceIsPresent.add(newServiceInfo);
                }
            }
            final DefaultMutableTreeNode node = newServiceInfo.getNode();
            if (node != null) {
                servicesInfo.moveNodeToPosition(pos, node, this);
                pos++;
            }
        }

        for (final Map.Entry<ServiceInfo, Map<String, String>> setEntry : setParametersHash.entrySet()) {
            setEntry.getKey().setParameters(setEntry.getValue());
            if (Application.isLive(runMode)) {
                setEntry.getKey().setUpdated(false);
            }
        }
        if (newService) {
            servicesInfo.reloadNode(this);
        }
        browser.repaint();
    }

    /**
     * Check if this connection is filesystem with drbd ra and if so, set it.
     */
    private void setFilesystemWithDrbd(final ServiceInfo siP, final ServiceInfo si) {
        if (siP.getResourceAgent().isLinbitDrbd()) {
            /* linbit::drbd -> Filesystem */
            ((FilesystemRaInfo) si).setLinbitDrbdInfo((LinbitDrbdInfo) siP);
        } else {
            /* drbddisk -> Filesystem */
            ((FilesystemRaInfo) si).setDrbddiskInfo((DrbddiskInfo) siP);
        }
    }

}
