/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
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
package lcmc.crm.ui.resource;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;
import lcmc.common.domain.Application;
import lcmc.common.domain.ColorText;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.crm.domain.CrmXml;
import lcmc.crm.domain.ClusterStatus;
import lcmc.host.domain.Host;
import lcmc.crm.domain.ResourceAgent;
import lcmc.common.domain.Value;
import lcmc.common.ui.Browser;
import lcmc.cluster.ui.widget.Check;
import lcmc.cluster.ui.widget.Widget;
import lcmc.crm.service.CRM;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.UpdatableItem;

/**
 * GroupInfo class holds data for heartbeat group, that is in some ways
 * like normal service, but it can contain other services.
 */
@Named
public class GroupInfo extends ServiceInfo {
    private static final Logger LOG = LoggerFactory.getLogger(GroupInfo.class);
    private final List<ServiceInfo> groupServices = new ArrayList<ServiceInfo>();

    private final ReadWriteLock mGroupServiceLock = new ReentrantReadWriteLock();
    private final Lock mGroupServiceReadLock = mGroupServiceLock.readLock();
    private final Lock mGroupServiceWriteLock = mGroupServiceLock.writeLock();
    @Inject
    private GroupMenu groupMenu;
    @Inject @Named("serviceInfo")
    private Provider<ServiceInfo> serviceInfoProvider;
    @Inject
    private Provider<FilesystemRaInfo> filesystemInfoProvider;
    @Inject
    private Provider<LinbitDrbdInfo> linbitDrbdInfoProvider;
    @Inject
    private Provider<DrbddiskInfo> drbddiskInfoProvider;
    @Inject
    private Provider<IPaddrInfo> ipaddrInfoProvider;
    @Inject
    private Provider<VirtualDomainInfo> virtualDomainInfoProvider;
    @Inject
    private Application application;
    @Inject
    private TreeMenuController treeMenuController;

    void init(final ResourceAgent ra, final Browser browser) {
        super.init(Application.PACEMAKER_GROUP_NAME, ra, browser);
    }

    /** Applies the the whole group if for example an order has changed. */
    void applyWhole(final Host dcHost,
                    final boolean createGroup,
                    final Iterable<String> newOrder,
                    final Application.RunMode runMode) {
        final String[] params = getParametersFromXML();
        if (Application.isLive(runMode)) {
            application.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    final MyButton ab = getApplyButton();
                    if (ab != null) {
                        ab.setEnabled(false);
                    }
                }
            });
        }
        getInfoPanel();
        waitForInfoPanel();

        final Map<String, String> groupMetaArgs = new LinkedHashMap<String, String>();
        for (final String param : params) {
            if (GUI_ID.equals(param) || PCMK_ID.equals(param)) {
                continue;
            }
            final Value value = getComboBoxValue(param);
            if (Tools.areEqual(value, getParamDefault(param))) {
                continue;
            }
            if (!value.isNothingSelected()) {
                if (CrmXml.GROUP_ORDERED_META_ATTR.equals(param)) {
                    groupMetaArgs.put("ordered", value.getValueForConfig());
                } else {
                    groupMetaArgs.put(param, value.getValueForConfig());
                }
            }
        }
        final Map<String, Map<String, String>> pacemakerResAttrs = new HashMap<String, Map<String, String>>();
        final Map<String, Map<String, String>> pacemakerResArgs = new HashMap<String, Map<String, String>>();
        final Map<String, Map<String, String>> pacemakerMetaArgs = new HashMap<String, Map<String, String>>();
        final Map<String, String> instanceAttrId = new HashMap<String, String>();
        final Map<String, Map<String, String>> nvpairIdsHash = new HashMap<String, Map<String, String>>();
        final Map<String, Map<String, Map<String, String>>> pacemakerOps =
                                                             new HashMap<String, Map<String, Map<String, String>>>();
        final Map<String, String> operationsId = new HashMap<String, String>();
        final Map<String, String> metaAttrsRefId = new HashMap<String, String>();
        final Map<String, String> operationsRefId = new HashMap<String, String>();
        final Map<String, Boolean> stonith = new HashMap<String, Boolean>();

        final ClusterStatus cs = getBrowser().getClusterStatus();
        for (final String resId : newOrder) {
            final ServiceInfo gsi = getBrowser().getServiceInfoFromCRMId(resId);
            if (gsi == null) {
                continue;
            }
            application.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    gsi.getInfoPanel();
                }
            });
        }
        application.waitForSwing();
        for (final String resId : newOrder) {
            final ServiceInfo gsi = getBrowser().getServiceInfoFromCRMId(resId);
            if (gsi == null) {
                continue;
            }
            pacemakerResAttrs.put(resId, gsi.getPacemakerResAttrs(runMode));
            pacemakerResArgs.put(resId, gsi.getPacemakerResArgs());
            pacemakerMetaArgs.put(resId, gsi.getPacemakerMetaArgs());
            instanceAttrId.put(resId, cs.getResourceInstanceAttrId(resId));
            nvpairIdsHash.put(resId, cs.getParametersNvpairsIds(resId));
            pacemakerOps.put(resId, gsi.getOperations(resId));
            operationsId.put(resId, cs.getOperationsId(resId));
            metaAttrsRefId.put(resId, gsi.getMetaAttrsRefId());
            operationsRefId.put(resId, gsi.getOperationsRefId());
            stonith.put(resId, gsi.getResourceAgent().isStonith());
        }
        final CloneInfo ci = getCloneInfo();
        String cloneId = null;
        boolean master = false;
        final Map<String, String> cloneMetaArgs = new LinkedHashMap<String, String>();
        String cloneMetaAttrsRefIds = null;
        if (createGroup && ci != null) {
            cloneId = ci.getHeartbeatId(runMode);
            final String[] cloneParams = ci.getParametersFromXML();
            master = ci.getService().isMaster();
            cloneMetaAttrsRefIds = ci.getMetaAttrsRefId();
            for (final String param : cloneParams) {
                if (GUI_ID.equals(param) || PCMK_ID.equals(param)) {
                    continue;
                }
                final Value value = ci.getComboBoxValue(param);
                if (Tools.areEqual(value, ci.getParamDefault(param))) {
                    continue;
                }
                if (!GUI_ID.equals(param) && !value.isNothingSelected()) {
                    cloneMetaArgs.put(param, value.getValueForConfig());
                }
            }
        }
        CRM.replaceGroup(createGroup,
                         dcHost,
                         cloneId,
                         master,
                         cloneMetaArgs,
                         cloneMetaAttrsRefIds,
                         newOrder,
                         groupMetaArgs,
                         getHeartbeatId(runMode),
                         pacemakerResAttrs,
                         pacemakerResArgs,
                         pacemakerMetaArgs,
                         instanceAttrId,
                         nvpairIdsHash,
                         pacemakerOps,
                         operationsId,
                         metaAttrsRefId,
                         getMetaAttrsRefId(),
                         operationsRefId,
                         stonith,
                         runMode);
        if (Application.isLive(runMode)) {
            storeComboBoxValues(params);
            treeMenuController.reloadNode(getNode(), false);
        }
        getBrowser().getCrmGraph().repaint();
    }

    /** Applies the changes to the group parameters. */
    @Override
    public void apply(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            application.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                }
            });
        }
        getInfoPanel();
        waitForInfoPanel();
        final String[] params = getParametersFromXML();
        if (Application.isLive(runMode)) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getApplyButton().setToolTipText("");
                    final Widget idField = getWidget(GUI_ID, null);
                    idField.setEnabled(false);
                }
            });

            /* add myself to the hash with service name and id as
             * keys */
            getBrowser().removeFromServiceInfoHash(this);
            final String oldHeartbeatId = getHeartbeatId(runMode);
            if (oldHeartbeatId != null) {
                getBrowser().mHeartbeatIdToServiceLock();
                getBrowser().getHeartbeatIdToServiceInfo().remove(oldHeartbeatId);
                getBrowser().mHeartbeatIdToServiceUnlock();
            }
            if (getService().isNew()) {
                final String id = getComboBoxValue(GUI_ID).getValueForConfig();
                getService().setIdAndCrmId(id);
                if (getTypeRadioGroup() != null) {
                    getTypeRadioGroup().setEnabled(false);
                }
            }
            getBrowser().addNameToServiceInfoHash(this);
            getBrowser().addToHeartbeatIdList(this);
        }

        /*
            MSG_ADD_GRP group
                    param_id1 param_name1 param_value1
                    param_id2 param_name2 param_value2
                    ...
                    param_idn param_namen param_valuen
        */
        final String heartbeatId = getHeartbeatId(runMode);
        if (getService().isNew()) {
            final Set<ServiceInfo> parents = getBrowser().getCrmGraph().getParents(this);
            final List<Map<String, String>> colAttrsList = new ArrayList<Map<String, String>>();
            final List<Map<String, String>> ordAttrsList = new ArrayList<Map<String, String>>();
            final List<String> parentIds = new ArrayList<String>();
            for (final ServiceInfo parentInfo : parents) {
                final String parentId = parentInfo.getService().getCrmId();
                parentIds.add(parentId);
                final Map<String, String> colAttrs = new LinkedHashMap<String, String>();
                final Map<String, String> ordAttrs = new LinkedHashMap<String, String>();
                colAttrs.put(CrmXml.SCORE_CONSTRAINT_PARAM, CrmXml.INFINITY_VALUE.getValueForConfig());
                ordAttrs.put(CrmXml.SCORE_CONSTRAINT_PARAM, CrmXml.INFINITY_VALUE.getValueForConfig());
                if (getService().isMaster()) {
                    colAttrs.put("with-rsc-role", "Master");
                    ordAttrs.put("first-action", "promote");
                    ordAttrs.put("then-action", "start");
                }
                colAttrsList.add(colAttrs);
                ordAttrsList.add(ordAttrs);
            }
            CRM.setOrderAndColocation(dcHost,
                                      heartbeatId,
                                      parentIds.toArray(new String [parentIds.size()]),
                                      colAttrsList,
                                      ordAttrsList,
                                      runMode);
            final Collection<String> newOrder = new ArrayList<String>();
            for (final ServiceInfo child : getGroupServices()) {
                newOrder.add(child.getHeartbeatId(runMode));
            }
            applyWhole(dcHost, true, newOrder, runMode);
            if (Application.isLive(runMode)) {
                setApplyButtons(null, params);
            }
            getBrowser().getCrmGraph().repaint();
            return;
        } else {
            final Map<String, String> groupMetaArgs = new LinkedHashMap<String, String>();
            for (final String param : params) {
                if (GUI_ID.equals(param) || PCMK_ID.equals(param)) {
                    continue;
                }
                final Value value = getComboBoxValue(param);
                if (Tools.areEqual(value, getParamDefault(param))) {
                    continue;
                }
                if (value != null && !value.isNothingSelected()) {
                    if (CrmXml.GROUP_ORDERED_META_ATTR.equals(param)) {
                        groupMetaArgs.put("ordered", value.getValueForConfig());
                    } else {
                        groupMetaArgs.put(param, value.getValueForConfig());
                    }
                }
            }
            CRM.setParameters(dcHost,
                              "-R",
                              null,  /* crm id */
                              null,  /* TODO: clone id */
                              false, /* master */
                              null,  /* cloneMetaArgs, */
                              groupMetaArgs,
                              heartbeatId, /* group id */
                              null,  /* pacemakerResAttrs, */
                              null,  /* pacemakerResArgs, */
                              null,  /* pacemakerMetaArgs, */
                              null,  /* cs.getResourceInstanceAttrId(heartbeatId), */
                              null,  /* cs.getParametersNvpairsIds(heartbeatId), */
                              null,  /* getOperations(heartbeatId), */
                              null,  /* cs.getOperationsId(heartbeatId), */
                              null,  /* getMetaAttrsRefId(), */
                              null,  /* cloneMetaAttrsRefIds, */
                              getMetaAttrsRefId(),
                              null,  /* getOperationsRefId(), */
                              false, /* stonith */
                              runMode);
        }
        final CloneInfo ci = getCloneInfo();
        if (ci == null) {
            setLocations(heartbeatId, dcHost, runMode);
        } else {
            ci.setLocations(heartbeatId, dcHost, runMode);

        }
        if (Application.isLive(runMode)) {
            storeComboBoxValues(params);
            treeMenuController.reloadNode(getNode(), false);
        }
        for (final ServiceInfo child : getGroupServices()) {
            final Check childCheck = child.checkResourceFields(null,
                                                               child.getParametersFromXML(),
                                                               false,
                                                               false,
                                                               true);
            if (childCheck.isCorrect() && childCheck.isChanged()) {
                child.apply(dcHost, runMode);
            }
        }
        if (Application.isLive(runMode)) {
            setApplyButtons(null, params);
        }
        getBrowser().getCrmGraph().repaint();
    }

    Iterable<ResourceAgent> getAddGroupServiceList(final String cl) {
        return getBrowser().getCrmXml().getServices(cl);
    }

    /**
     * Adds service to this group. Adds it in the submenu in the menu tree
     * and initializes it.
     *
     * @param newServiceInfo
     *      service info object of the new service
     */
    void addGroupServicePanel(final ServiceInfo newServiceInfo, final boolean reloadNode) {
        final DefaultMutableTreeNode gn = getNode();
        if (gn == null) {
            return;
        }
        newServiceInfo.getService().setResourceClass(newServiceInfo.getResourceAgent().getResourceClass());
        newServiceInfo.setGroupInfo(this);
        getBrowser().addNameToServiceInfoHash(newServiceInfo);
        getBrowser().addToHeartbeatIdList(newServiceInfo);
        final DefaultMutableTreeNode newServiceNode = treeMenuController.createMenuItem(newServiceInfo);
        mGroupServiceWriteLock.lock();
        try {
            groupServices.add(newServiceInfo);
        } finally {
            mGroupServiceWriteLock.unlock();
        }
        treeMenuController.addChild(gn, newServiceNode);
        if (reloadNode) {
            treeMenuController.reloadNode(gn, false);
            treeMenuController.reloadNode(newServiceNode, true);
        }
    }

    ServiceInfo addGroupServicePanel(final ResourceAgent newRA, final boolean reloadNode) {
        final ServiceInfo newServiceInfo;

        final String name = newRA.getServiceName();
        if (newRA.isFilesystem()) {
            newServiceInfo = filesystemInfoProvider.get();
        } else if (newRA.isLinbitDrbd()) {
            newServiceInfo = linbitDrbdInfoProvider.get();
        } else if (newRA.isDrbddisk()) {
            newServiceInfo = drbddiskInfoProvider.get();
        } else if (newRA.isIPaddr()) {
            newServiceInfo = ipaddrInfoProvider.get();
        } else if (newRA.isVirtualDomain()) {
            newServiceInfo = virtualDomainInfoProvider.get();
        } else if (newRA.isGroup()) {
            LOG.appError("addGroupServicePanel: no groups in group allowed");
            return null;
        } else {
            newServiceInfo = serviceInfoProvider.get();
        }
        newServiceInfo.init(name, newRA, getBrowser());
        addGroupServicePanel(newServiceInfo, reloadNode);
        return newServiceInfo;
    }

    /**
     * Returns on which node this group is running, meaning on which node
     * all the services are running. Null if they running on different
     * nodes or not at all.
     */
    @Override
    protected List<String> getRunningOnNodes(final Application.RunMode runMode) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(getHeartbeatId(runMode), runMode);
        final List<String> allNodes = new ArrayList<String>();
        if (resources != null) {
            for (final String hbId : resources) {
                final List<String> ns = cs.getRunningOnNodes(hbId, runMode);
                if (ns != null) {
                    for (final String n : ns) {
                        if (!allNodes.contains(n)) {
                            allNodes.add(n);
                        }
                    }
                }
            }
        }
        return allNodes;
    }

    /** Returns node name of the host where this service is running. */
    @Override
    List<String> getMasterOnNodes(final Application.RunMode runMode) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(getHeartbeatId(runMode), runMode);
        final List<String> allNodes = new ArrayList<String>();
        if (resources != null) {
            for (final String hbId : resources) {
                final List<String> ns = cs.getMasterOnNodes(hbId, runMode);
                if (ns != null) {
                    for (final String n : ns) {
                        if (!allNodes.contains(n)) {
                            allNodes.add(n);
                        }
                    }
                }
            }
        }
        return allNodes;
    }

    /** Starts all resources in the group. */
    @Override
    void startResource(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        for (final ServiceInfo child : getGroupServices()) {
            CRM.startResource(dcHost, child.getHeartbeatId(runMode), runMode);
        }
    }

    /** Stops all resources in the group. */
    @Override
    void stopResource(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        for (final ServiceInfo child : getGroupServices()) {
            CRM.stopResource(dcHost, child.getHeartbeatId(runMode), runMode);
        }
    }

    /** Cleans up all resources in the group. */
    @Override
    void cleanupResource(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        for (final ServiceInfo child : getGroupServices()) {
            child.cleanupResource(dcHost, runMode);
        }
    }

    /** Sets whether the group services are managed. */
    @Override
    void setManaged(final boolean isManaged, final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        for (final ServiceInfo child : getGroupServices()) {
            CRM.setManaged(dcHost, child.getHeartbeatId(runMode), isManaged, runMode);
        }
    }

    @Override
    public List<UpdatableItem> createPopup() {
        return groupMenu.getPulldownMenu(this);
    }

    /** Removes this group from the cib. */
    @Override
    public void removeMyself(final Application.RunMode runMode) {
        if (getService().isNew()) {
            removeMyselfNoConfirm(getBrowser().getDCHost(), runMode);
            getService().setNew(false);
            super.removeInfo();
            getService().doneRemoving();
            return;
        }
        String desc = Tools.getString("ClusterBrowser.confirmRemoveGroup.Description");

        final StringBuilder services = new StringBuilder();

        boolean first = true;
        for (final ServiceInfo child : getGroupServices()) {
            if (!first) {
                services.append(", ");
            }
            services.append(child);
            first = false;
        }

        desc  = desc.replaceAll("@GROUP@", '\'' + Matcher.quoteReplacement(toString()) + '\'');
        desc  = desc.replaceAll("@SERVICES@", Matcher.quoteReplacement(services.toString()));
        if (application.confirmDialog(Tools.getString("ClusterBrowser.confirmRemoveGroup.Title"),
                                desc,
                                Tools.getString("ClusterBrowser.confirmRemoveGroup.Yes"),
                                Tools.getString("ClusterBrowser.confirmRemoveGroup.No"))) {
            if (Application.isLive(runMode)) {
                getService().setRemoved(true);
            }
            removeMyselfNoConfirm(getBrowser().getDCHost(), runMode);
            super.removeInfo();
            getService().setNew(false);
        }
        getService().doneRemoving();
    }

    @Override
    public void removeInfo() {
        for (final ServiceInfo child : getGroupServices()) {
            child.removeInfo();
        }
        super.removeInfo();
    }

    /** Remove all the services in the group and the group. */
    @Override
    public void removeMyselfNoConfirm(final Host dcHost, final Application.RunMode runMode) {
        final Collection<ServiceInfo> children = new ArrayList<ServiceInfo>();
        if (Application.isLive(runMode)) {
            for (final ServiceInfo child : getGroupServices()) {
                child.getService().setRemoved(true);
                children.add(child);
            }
        }
        if (getService().isNew()) {
            if (Application.isLive(runMode)) {
                getService().setNew(false);
                getBrowser().getCrmGraph().killRemovedVertices();
                getService().doneRemoving();
            }
        } else {
            String cloneId = null;
            boolean master = false;
            final CloneInfo ci = getCloneInfo();
            if (ci != null) {
                cloneId = ci.getHeartbeatId(runMode);
                master = ci.getService().isMaster();
            }
            super.removeMyselfNoConfirm(dcHost, runMode);
            for (final ServiceInfo child : children) {
                child.removeConstraints(dcHost, runMode);
            }
            CRM.removeResource(dcHost, null, getHeartbeatId(runMode), cloneId, master, runMode);
            for (final ServiceInfo child : children) {
                child.cleanupResource(dcHost, runMode);
            }
        }
        if (Application.isLive(runMode)) {
            for (final ServiceInfo child : children) {
                getBrowser().mHeartbeatIdToServiceLock();
                getBrowser().getHeartbeatIdToServiceInfo().remove(child.getService().getCrmId());
                getBrowser().mHeartbeatIdToServiceUnlock();
                getBrowser().removeFromServiceInfoHash(child);
                child.cleanup();
                child.getService().doneRemoving();
            }
        }
    }

    /** Removes the group, but not the services. */
    void removeMyselfNoConfirmFromChild(final Host dcHost, final Application.RunMode runMode) {
        super.removeMyselfNoConfirm(dcHost, runMode);
    }

    /** Returns tool tip for the group vertex. */
    @Override
    public String getToolTipText(final Application.RunMode runMode) {
        final List<String> hostNames = getRunningOnNodes(runMode);
        final StringBuilder sb = new StringBuilder(220);
        sb.append("<b>");
        sb.append(this);
        if (hostNames == null || hostNames.isEmpty()) {
            sb.append(" not running");
        } else if (hostNames.size() == 1) {
            sb.append(" running on node: ");
        } else {
            sb.append(" running on nodes: ");
        }
        if (hostNames != null && !hostNames.isEmpty()) {
            sb.append(Tools.join(", ", hostNames.toArray(new String[hostNames.size()])));
        }
        sb.append("</b>");

        for (final ServiceInfo child : getGroupServices()) {
            sb.append("\n&nbsp;&nbsp;&nbsp;");
            sb.append(child.getToolTipText(runMode));
        }

        return sb.toString();
    }

    @Override
    boolean isOneFailed(final Application.RunMode runMode) {
        for (final ServiceInfo child : getGroupServices()) {
            if (child.isOneFailed(runMode)) {
                return true;
            }
        }
        return false;
    }

    @Override
    boolean isOneFailedCount(final Application.RunMode runMode) {
        for (final ServiceInfo child : getGroupServices()) {
            if (child.isOneFailedCount(runMode)) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether one of the services failed to start. */
    @Override
    public boolean isFailed(final Application.RunMode runMode) {
        for (final ServiceInfo child : getGroupServices()) {
            if (child.isFailed(runMode)) {
                return true;
            }
        }
        return false;
    }

    /** Returns subtexts that appears in the service vertex. */
    @Override
    public ColorText[] getSubtextsForGraph(final Application.RunMode runMode) {
        final List<ColorText> texts = new ArrayList<ColorText>();
        ColorText prevColorText = null;

        for (final ServiceInfo child : getGroupServices()) {
            final ColorText[] colorTexts = child.getSubtextsForGraph(runMode);

            if (colorTexts == null || colorTexts.length == 0) {
                continue;
            }
            final ColorText sColorText = colorTexts[0];
            if (prevColorText == null || !sColorText.getSubtext().equals(prevColorText.getSubtext())) {
                texts.add(new ColorText(sColorText.getSubtext() + ':', sColorText.getColor(), Color.BLACK));
                prevColorText = sColorText;
            }
            String unmanaged = "";
            if (!child.isManaged(runMode)) {
                unmanaged = " / unmanaged";
            }
            String migrated = "";
            if (child.getMigratedTo(runMode) != null || child.getMigratedFrom(runMode) != null) {
                migrated = " / migrated";
            }
            final HbConnectionInfo[] hbcis = getBrowser().getCrmGraph().getHbConnections(child);
            String constraintLeft = "";
            String constraint = "";
            if (hbcis != null) {
                boolean scoreFirst = false;
                boolean scoreThen = false;
                boolean someConnection = false;
                for (final HbConnectionInfo hbci : hbcis) {
                    if (hbci == null) {
                        continue;
                    }
                    if (!someConnection && hbci.hasColocationOrOrder(child)) {
                        someConnection = true;
                    }
                    if (!scoreFirst && !hbci.isOrdScoreNull(child, null)) {
                        scoreFirst = true;
                    }
                    if (!scoreThen && !hbci.isOrdScoreNull(null, child)) {
                        scoreThen = true;
                    }
                }
                if (someConnection) {
                    if (scoreFirst || scoreThen) {
                        if (scoreFirst) {
                            constraint = " \u2192"; /* -> */
                        }
                        if (scoreThen) {
                            constraintLeft = "\u2192 "; /* -> */
                        }
                    } else {
                        /* just colocation */
                        constraint = " --"; /* -- */
                    }
                }
            }
            texts.add(new ColorText("   " + constraintLeft + child + unmanaged + migrated + constraint,
                                  sColorText.getColor(),
                                  Color.BLACK));
            boolean skip = true;
            for (final ColorText st : colorTexts) {
                if (skip) {
                    skip = false;
                    continue;
                }
                texts.add(new ColorText("   " + st.getSubtext(), st.getColor(), Color.BLACK));
            }
        }
        return texts.toArray(new ColorText[texts.size()]);
    }

    /**
     * Returns from which hosts the services or the whole group was migrated.
     */
    @Override
    public List<Host> getMigratedFrom(final Application.RunMode runMode) {
        List<Host> hosts = super.getMigratedFrom(runMode);
        final List<ServiceInfo> gs = getGroupServices();
        if (gs.isEmpty()) {
            return null;
        }
        for (final ServiceInfo child : gs) {
            final List<Host> siHosts = child.getMigratedFrom(runMode);
            if (siHosts != null) {
                if (hosts == null) {
                    hosts = new ArrayList<Host>();
                }
                hosts.addAll(siHosts);
            }
        }
        return hosts;
    }

    /** Returns whether at least one service is unmanaged. */
    @Override
    public boolean isManaged(final Application.RunMode runMode) {
        final List<ServiceInfo> gs = getGroupServices();
        if (gs.isEmpty()) {
            return true;
        }
        for (final ServiceInfo child : gs) {
            if (!child.isManaged(runMode)) {
                return false;
            }
        }
        return true;
    }

    /** Returns whether all of the services are started. */
    @Override
    boolean isStarted(final Application.RunMode runMode) {
        for (final ServiceInfo child : getGroupServices()) {
            if (!child.isStarted(runMode)) {
                return false;
            }
        }
        return true;
    }

    /** Returns whether one of the services is stopped. */
    @Override
    public boolean isStopped(final Application.RunMode runMode) {
        for (final ServiceInfo child : getGroupServices()) {
            if (child.isStopped(runMode)) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether the group is stopped. */
    @Override
    boolean isGroupStopped(final Application.RunMode runMode) {
        return super.isStopped(runMode);
    }

    /** Returns true if at least one service in the group are running. */
    boolean isOneRunning(final Application.RunMode runMode) {
        for (final ServiceInfo child : getGroupServices()) {
            if (child.isRunning(runMode)) {
                return true;
            }
        }
        return false;
    }

    /** Returns true if all services in the group are running. */
    @Override
    public boolean isRunning(final Application.RunMode runMode) {
        for (final ServiceInfo child : getGroupServices()) {
            if (!child.isRunning(runMode)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If group does not have any services, its changes
     * cannot by applied.
     */
    @Override
    public Check checkResourceFields(final String param, final String[] params) {
        return checkResourceFields(param, params, false, false);
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    Check checkResourceFields(final String param,
                              final String[] params,
                              final boolean fromServicesInfo,
                              final boolean fromCloneInfo) {
        final List<String> incorrect = new ArrayList<String>();
        final List<String> changed = new ArrayList<String>();
        final Check check = new Check(incorrect, changed);
        check.addCheck(super.checkResourceFields(param, params, fromServicesInfo, fromCloneInfo, true));
        boolean hasSevices = false;
        for (final ServiceInfo child : getGroupServices()) {
            check.addCheck(child.checkResourceFields(null,
                                                     child.getParametersFromXML(),
                                                     fromServicesInfo,
                                                     fromCloneInfo,
                                                     true));
            hasSevices = true;
        }
        if (!hasSevices) {
            incorrect.add("no services");
        }
        return check;
    }

    /** Update menus with positions and calles their update methods. */
    @Override
    public void updateMenus(final Point2D pos) {
        super.updateMenus(pos);
        if (!application.isSlow()) {
            for (final ServiceInfo child : getGroupServices()) {
                child.updateMenus(pos);
            }
        }
    }

    @Override
    public ImageIcon getCategoryIcon(final Application.RunMode runMode) {
        if (getBrowser().allHostsWithoutClusterStatus() || !isOneRunning(runMode)) {
            return ServiceInfo.SERVICE_STOPPED_ICON_SMALL;
        }
        return ServiceInfo.SERVICE_RUNNING_ICON_SMALL;
    }

    /** Revert all values. */
    @Override
    public void revert() {
        super.revert();
        for (final ServiceInfo child : getGroupServices()) {
            if (child.checkResourceFields(null, child.getParametersFromXML(), true, false, false).isChanged()) {
                child.revert();
            }
        }
    }

    /** Return copy of the group services. */
    public List<ServiceInfo> getGroupServices() {
        mGroupServiceReadLock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<ServiceInfo>(groupServices));
        } finally {
            mGroupServiceReadLock.unlock();
        }
    }
}
