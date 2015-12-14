/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2015, Rastislav Levrinc.
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

import com.google.common.base.Optional;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.Application;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.crm.domain.ClusterStatus;
import lcmc.crm.domain.CrmXml;
import lcmc.crm.domain.ResourceAgent;
import lcmc.crm.ui.CrmGraph;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Named
public class ResourceUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceUpdater.class);

    private ClusterBrowser browser;
//    @Inject
    private ServicesInfo servicesInfo;
    @Inject
    private Provider<ConstraintPHInfo> constraintPHInfoProvider;
    @Inject
    private Provider<PcmkRscSetsInfo> pcmkRscSetsInfoProvider;
    @Inject
    private Application application;
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private TreeMenuController treeMenuController;
    @Inject
    private CrmServiceFactory crmServiceFactory;

    /**
     * This functions goes through all services, constrains etc. in
     * clusterStatus and updates the internal structures and graph.
     */
    public void setAllResources(final ServicesInfo servicesInfo,
                                final ClusterBrowser browser,
                                final ClusterStatus clusterStatus,
                                final Application.RunMode runMode) {
        this.servicesInfo = servicesInfo;
        this.browser = browser;

        final Set<String> allGroupsAndClones = clusterStatus.getAllGroupsAndClones();
        final CrmGraph crmGraph = browser.getCrmGraph();
        final List<ServiceInfo> groupServiceIsPresent = new ArrayList<ServiceInfo>();
        final List<ServiceInfo> serviceIsPresent = new ArrayList<ServiceInfo>();
        for (final String groupOrClone : allGroupsAndClones) {
            CloneInfo newCi = null;
            GroupInfo newGi = null;
            if (clusterStatus.isClone(groupOrClone)) {
                /* clone */
                newCi = setCreateCloneInfo(groupOrClone, clusterStatus, runMode);
                serviceIsPresent.add(newCi);
            } else if (!"none".equals(groupOrClone)) {
                /* group */
                final GroupInfo gi = (GroupInfo) this.browser.getServiceInfoFromCRMId(groupOrClone);
                if (gi != null && gi.getCloneInfo() != null) {
                    /* cloned group is already done */
                    groupServiceIsPresent.add(gi);
                    continue;
                }
                newGi = setCreateGroupInfo(groupOrClone, newCi, clusterStatus, runMode);
                serviceIsPresent.add(newGi);
            }
            setGroupResources(allGroupsAndClones,
                    groupOrClone,
                    newGi,
                    newCi,
                    serviceIsPresent,
                    groupServiceIsPresent,
                    clusterStatus,
                    runMode);
        }

        crmGraph.clearKeepColocationList();
        crmGraph.clearKeepOrderList();
        /* resource sets */
        final List<CrmXml.RscSetConnectionData> newRscSetConnections = clusterStatus.getRscSetConnections();
        if (newRscSetConnections != null) {
            final Map<CrmXml.RscSetConnectionData, ConstraintPHInfo> oldResourceSetToPlaceholder =
                    new LinkedHashMap<CrmXml.RscSetConnectionData, ConstraintPHInfo>();
            this.browser.lockNameToServiceInfo();
            final Map<String, ServiceInfo> oldIdToInfoHash = this.browser.getNameToServiceInfoHash(ConstraintPHInfo.NAME);
            final List<ConstraintPHInfo> preNewCphis = new ArrayList<ConstraintPHInfo>();
            if (oldIdToInfoHash != null) {
                for (final Map.Entry<String, ServiceInfo> infoEntry : oldIdToInfoHash.entrySet()) {
                    final ConstraintPHInfo cphi = (ConstraintPHInfo) infoEntry.getValue();
                    final CrmXml.RscSetConnectionData rdataOrd = cphi.getRscSetConnectionDataOrder();
                    final CrmXml.RscSetConnectionData rdataCol = cphi.getRscSetConnectionDataColocation();
                    if (cphi.isNew()) {
                        preNewCphis.add(cphi);
                    }
                    if (rdataOrd != null && !rdataOrd.isEmpty()) {
                        oldResourceSetToPlaceholder.put(rdataOrd, cphi);
                    }
                    if (rdataCol != null && !rdataCol.isEmpty()) {
                        oldResourceSetToPlaceholder.put(rdataCol, cphi);
                    }
                }
            }
            this.browser.unlockNameToServiceInfo();
            final Collection<ConstraintPHInfo> newCphis = new ArrayList<ConstraintPHInfo>();
            for (final CrmXml.RscSetConnectionData newRscSetConnectionData : newRscSetConnections) {
                ConstraintPHInfo constraintPHInfo = null;

                for (final Map.Entry<CrmXml.RscSetConnectionData, ConstraintPHInfo> phEntry : oldResourceSetToPlaceholder.entrySet()) {
                    final CrmXml.RscSetConnectionData oldRscSetConnectionData = phEntry.getKey();
                    final ConstraintPHInfo placeholder = phEntry.getValue();
                    if (oldRscSetConnectionData == newRscSetConnectionData) {
                        continue;
                    }
                    if (newRscSetConnectionData.equals(oldRscSetConnectionData) || newRscSetConnectionData.equalsAlthoughReversed(oldRscSetConnectionData)) {
                        constraintPHInfo = placeholder;
                        constraintPHInfo.setRscSetConnectionData(newRscSetConnectionData);
                        break;
                    }
                }
                PcmkRscSetsInfo rscSetsInfo = null;
                if (constraintPHInfo == null) {
                    for (final Map.Entry<CrmXml.RscSetConnectionData, ConstraintPHInfo> phEntry : oldResourceSetToPlaceholder.entrySet()) {
                        final CrmXml.RscSetConnectionData oldRrcSetConnectionData = phEntry.getKey();
                        final ConstraintPHInfo placeholder = phEntry.getValue();
                        if (oldRrcSetConnectionData == newRscSetConnectionData) {
                            constraintPHInfo = placeholder;
                            break;
                        }
                        if (placeholder.sameConstraintId(newRscSetConnectionData)) {
                            /* use the same rsc set info object */
                            rscSetsInfo = placeholder.getPcmkRscSetsInfo();
                        }
                        if (placeholder.isNew()
                                || (newRscSetConnectionData.canUseSamePlaceholder(oldRrcSetConnectionData)
                                && placeholder.sameConstraintId(newRscSetConnectionData))) {
                            constraintPHInfo = placeholder;
                            constraintPHInfo.setRscSetConnectionData(newRscSetConnectionData);
                            rscSetsInfo = constraintPHInfo.getPcmkRscSetsInfo();
                            if (rscSetsInfo != null) {
                                if (newRscSetConnectionData.isColocation()) {
                                    rscSetsInfo.addColocation(newRscSetConnectionData.getConstraintId(), constraintPHInfo);
                                } else {
                                    rscSetsInfo.addOrder(newRscSetConnectionData.getConstraintId(), constraintPHInfo);
                                }
                            }
                            break;
                        }
                    }
                }
                if (constraintPHInfo == null && !preNewCphis.isEmpty()) {
                    /* placeholder */
                    constraintPHInfo = preNewCphis.remove(0);
                    oldResourceSetToPlaceholder.put(newRscSetConnectionData, constraintPHInfo);
                    constraintPHInfo.setRscSetConnectionData(newRscSetConnectionData);
                }
                if (constraintPHInfo == null) {
                    constraintPHInfo = constraintPHInfoProvider.get();
                    constraintPHInfo.init(this.browser, newRscSetConnectionData, ConstraintPHInfo.Preference.AND);
                    if (rscSetsInfo == null) {
                        rscSetsInfo = pcmkRscSetsInfoProvider.get();
                        rscSetsInfo.init(this.browser);
                    }
                    if (newRscSetConnectionData.isColocation()) {
                        rscSetsInfo.addColocation(newRscSetConnectionData.getConstraintId(), constraintPHInfo);
                    } else {
                        rscSetsInfo.addOrder(newRscSetConnectionData.getConstraintId(), constraintPHInfo);
                    }
                    constraintPHInfo.setPcmkRscSetsInfo(rscSetsInfo);
                    this.browser.addNameToServiceInfoHash(constraintPHInfo);
                    newCphis.add(constraintPHInfo); /* have to add it later,
                                       so that ids are correct. */
                    oldResourceSetToPlaceholder.put(newRscSetConnectionData, constraintPHInfo);
                }
                serviceIsPresent.add(constraintPHInfo);

                final CrmXml.RscSet rscSet1 = newRscSetConnectionData.getRscSet1();
                final CrmXml.RscSet rscSet2 = newRscSetConnectionData.getRscSet2();
                if (newRscSetConnectionData.isColocation()) {
                    /* colocation */
                    if (rscSet1 != null) {
                        for (final String rscId : rscSet1.getRscIds()) {
                            final ServiceInfo si =
                                    this.browser.getServiceInfoFromCRMId(rscId);
                            crmGraph.addColocation(newRscSetConnectionData.getConstraintId(), constraintPHInfo, si);
                        }
                    }
                    if (rscSet2 != null) {
                        for (final String rscId : rscSet2.getRscIds()) {
                            final ServiceInfo si = this.browser.getServiceInfoFromCRMId(rscId);
                            crmGraph.addColocation(newRscSetConnectionData.getConstraintId(), si, constraintPHInfo);
                        }
                    }
                } else {
                    /* order */
                    if (rscSet1 != null) {
                        for (final String rscId : rscSet1.getRscIds()) {
                            final ServiceInfo si = this.browser.getServiceInfoFromCRMId(rscId);
                            crmGraph.addOrder(newRscSetConnectionData.getConstraintId(), si, constraintPHInfo);
                        }
                    }
                    if (rscSet2 != null) {
                        for (final String rscId : rscSet2.getRscIds()) {
                            final ServiceInfo si = this.browser.getServiceInfoFromCRMId(rscId);
                            crmGraph.addOrder(newRscSetConnectionData.getConstraintId(), constraintPHInfo, si);
                        }
                    }
                }
                if (Application.isLive(runMode)) {
                    constraintPHInfo.setUpdated(false);
                    constraintPHInfo.setNew(false);
                }
            }

            for (final ConstraintPHInfo cphi : newCphis) {
                crmGraph.addConstraintPlaceholder(cphi,
                        null, /* pos */
                        Application.RunMode.LIVE);
            }
        }

        /* colocations */
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

        /* orders */
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

        servicesInfo.cleanupServiceMenu(groupServiceIsPresent);
        crmGraph.updateRemovedElements(serviceIsPresent);
    }

    /**
     * Sets clone info object.
     */
    private CloneInfo setCreateCloneInfo(final String cloneId,
                                         final ClusterStatus clStatus,
                                         final Application.RunMode runMode) {
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
            final Map<String, String> resourceNode = clStatus.getParamValuePairs(newCi.getHeartbeatId(runMode));
            newCi.setParameters(resourceNode);
        } else {
            final Map<String, String> resourceNode = clStatus.getParamValuePairs(newCi.getHeartbeatId(runMode));
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
                                         final CloneInfo newCi,
                                         final ClusterStatus clStatus,
                                         final Application.RunMode runMode) {
        GroupInfo newGi = (GroupInfo) browser.getServiceInfoFromCRMId(group);
        if (newGi == null) {
            final Point2D p = null;
            newGi =
                    (GroupInfo) servicesInfo.addServicePanel(browser.getGroupResourceAgent(),
                            p,
                            false,
                            group,
                            newCi,
                            runMode);
            final Map<String, String> resourceNode = clStatus.getParamValuePairs(newGi.getHeartbeatId(runMode));
            newGi.setParameters(resourceNode);
            if (newCi != null) {
                newCi.addCloneServicePanel(newGi);
            }
        } else {
            final Map<String, String> resourceNode = clStatus.getParamValuePairs(newGi.getHeartbeatId(runMode));
            newGi.setParameters(resourceNode);
            if (Application.isLive(runMode)) {
                newGi.setUpdated(false);
                browser.repaint();
            }
        }
        newGi.setNew(false);
        return newGi;
    }

    private void setGroupResources(final Set<String> allGroupsAndClones,
                                   final String grpOrCloneId,
                                   final GroupInfo newGi,
                                   final CloneInfo newCi,
                                   final List<ServiceInfo> serviceIsPresent,
                                   final List<ServiceInfo> groupServiceIsPresent,
                                   final ClusterStatus clusterStatus,
                                   final Application.RunMode runMode) {
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
                final GroupInfo gi = setCreateGroupInfo(hbId, newCi, clusterStatus, runMode);
                setGroupResources(allGroupsAndClones,
                        hbId,
                        gi,
                        null,
                        serviceIsPresent,
                        groupServiceIsPresent,
                        clusterStatus,
                        runMode);
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
