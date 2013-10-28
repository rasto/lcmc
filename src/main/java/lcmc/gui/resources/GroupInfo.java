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
package lcmc.gui.resources;

import java.util.Collections;
import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.widget.Widget;
import lcmc.data.ResourceAgent;
import lcmc.data.Host;
import lcmc.data.CRMXML;
import lcmc.data.ClusterStatus;
import lcmc.data.Subtext;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.CRM;
import lcmc.utilities.MyMenu;
import lcmc.utilities.Tools;
import lcmc.utilities.MyList;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.MyListModel;
import lcmc.utilities.ButtonCallback;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.regex.Matcher;
import javax.swing.JDialog;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JCheckBox;
import java.awt.geom.Point2D;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lcmc.utilities.MyButton;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * GroupInfo class holds data for heartbeat group, that is in some ways
 * like normal service, but it can contain other services.
 */
public final class GroupInfo extends ServiceInfo {
    /** Logger. */
    private static final Logger LOG =
                                    LoggerFactory.getLogger(GroupInfo.class);
    private final List<ServiceInfo> groupServices =
                                                  new ArrayList<ServiceInfo>();

    private final ReadWriteLock mGroupServiceLock =
                                                  new ReentrantReadWriteLock();
    private final Lock mGroupServiceReadLock = mGroupServiceLock.readLock();
    private final Lock mGroupServiceWriteLock = mGroupServiceLock.writeLock();
    /** Creates new GroupInfo object. */
    GroupInfo(final ResourceAgent ra, final Browser browser) {
        super(ConfigData.PM_GROUP_NAME, ra, browser);
    }

    /** Applies the the whole group if for example an order has changed. */
    void applyWhole(final Host dcHost,
                    final boolean createGroup,
                    final List<String> newOrder,
                    final boolean testOnly) {
        final String[] params = getParametersFromXML();
        if (!testOnly) {
            Tools.invokeAndWait(new Runnable() {
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

        final Map<String, String> groupMetaArgs =
                                        new LinkedHashMap<String, String>();
        for (String param : params) {
            if (GUI_ID.equals(param)
                || PCMK_ID.equals(param)) {
                continue;
            }
            String value = getComboBoxValue(param);
            if (value == null) {
                value = "";
            }
            if (value.equals(getParamDefault(param))) {
                continue;
            }
            if (!"".equals(value)) {
                if (CRMXML.GROUP_ORDERED_META_ATTR.equals(param)) {
                    groupMetaArgs.put("ordered", value);
                } else {
                    groupMetaArgs.put(param, value);
                }
            }
        }
        final Map<String, Map<String, String>> pacemakerResAttrs =
                                 new HashMap<String, Map<String, String>>();
        final Map<String, Map<String, String>> pacemakerResArgs =
                                 new HashMap<String, Map<String, String>>();
        final Map<String, Map<String, String>> pacemakerMetaArgs =
                                 new HashMap<String, Map<String, String>>();
        final Map<String, String> instanceAttrId =
                                              new HashMap<String, String>();
        final Map<String, Map<String, String>> nvpairIdsHash =
                                 new HashMap<String, Map<String, String>>();
        final Map<String, Map<String, Map<String, String>>> pacemakerOps =
                    new HashMap<String, Map<String, Map<String, String>>>();
        final Map<String, String> operationsId =
                                              new HashMap<String, String>();
        final Map<String, String> metaAttrsRefId =
                                              new HashMap<String, String>();
        final Map<String, String> operationsRefId =
                                              new HashMap<String, String>();
        final Map<String, Boolean> stonith = new HashMap<String, Boolean>();

        final ClusterStatus cs = getBrowser().getClusterStatus();
        for (final String resId : newOrder) {
            final ServiceInfo gsi =
                               getBrowser().getServiceInfoFromCRMId(resId);
            if (gsi == null) {
                continue;
            }
            Tools.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    gsi.getInfoPanel();
                }
            });
        }
        Tools.waitForSwing();
        for (final String resId : newOrder) {
            final ServiceInfo gsi =
                               getBrowser().getServiceInfoFromCRMId(resId);
            if (gsi == null) {
                continue;
            }
            pacemakerResAttrs.put(resId,
                                  gsi.getPacemakerResAttrs(testOnly));
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
        final Map<String, String> cloneMetaArgs =
                                            new LinkedHashMap<String, String>();
        String cloneMetaAttrsRefIds = null;
        if (createGroup && ci != null) {
            cloneId = ci.getHeartbeatId(testOnly);
            final String[] cloneParams = ci.getParametersFromXML();
            master = ci.getService().isMaster();
            cloneMetaAttrsRefIds = ci.getMetaAttrsRefId();
            for (String param : cloneParams) {
                if (GUI_ID.equals(param)
                    || PCMK_ID.equals(param)) {
                    continue;
                }
                final String value = ci.getComboBoxValue(param);
                if (value.equals(ci.getParamDefault(param))) {
                    continue;
                }
                if (!GUI_ID.equals(param) && !"".equals(value)) {
                    cloneMetaArgs.put(param, value);
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
                         getHeartbeatId(testOnly),
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
                         testOnly);
        if (!testOnly) {
            storeComboBoxValues(params);
            getBrowser().reload(getNode(), false);
        }
        getBrowser().getCRMGraph().repaint();
    }

    /** Applies the changes to the group parameters. */
    @Override
    void apply(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            Tools.invokeAndWait(new Runnable() {
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
        if (!testOnly) {
            Tools.invokeLater(new Runnable() {
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
            final String oldHeartbeatId = getHeartbeatId(testOnly);
            if (oldHeartbeatId != null) {
                getBrowser().mHeartbeatIdToServiceLock();
                getBrowser().getHeartbeatIdToServiceInfo().remove(
                                                               oldHeartbeatId);
                getBrowser().mHeartbeatIdToServiceUnlock();
            }
            if (getService().isNew()) {
                final String id = getComboBoxValue(GUI_ID);
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
        final String heartbeatId = getHeartbeatId(testOnly);
        if (getService().isNew()) {
            final Set<ServiceInfo> parents =
                                    getBrowser().getCRMGraph().getParents(this);
            final List<Map<String, String>> colAttrsList =
                                       new ArrayList<Map<String, String>>();
            final List<Map<String, String>> ordAttrsList =
                                       new ArrayList<Map<String, String>>();
            final List<String> parentIds = new ArrayList<String>();
            for (final ServiceInfo parentInfo : parents) {
                final String parentId =
                                    parentInfo.getService().getHeartbeatId();
                parentIds.add(parentId);
                final Map<String, String> colAttrs =
                                       new LinkedHashMap<String, String>();
                final Map<String, String> ordAttrs =
                                       new LinkedHashMap<String, String>();
                colAttrs.put(CRMXML.SCORE_STRING, CRMXML.INFINITY_STRING);
                ordAttrs.put(CRMXML.SCORE_STRING, CRMXML.INFINITY_STRING);
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
                                      parentIds.toArray(
                                                new String [parentIds.size()]),
                                      colAttrsList,
                                      ordAttrsList,
                                      testOnly);
            final List<String> newOrder = new ArrayList<String>();
            for (final ServiceInfo child : getGroupServices()) {
                newOrder.add(child.getHeartbeatId(testOnly));
            }
            applyWhole(dcHost, true, newOrder, testOnly);
            if (!testOnly) {
                setApplyButtons(null, params);
            }
            getBrowser().getCRMGraph().repaint();
            return;
        } else {
            final Map<String, String> groupMetaArgs =
                                            new LinkedHashMap<String, String>();
            for (String param : params) {
                if (GUI_ID.equals(param)
                    || PCMK_ID.equals(param)) {
                    continue;
                }
                final String value = getComboBoxValue(param);
                if (value.equals(getParamDefault(param))) {
                    continue;
                }
                if (!"".equals(value)) {
                    if (CRMXML.GROUP_ORDERED_META_ATTR.equals(param)) {
                        groupMetaArgs.put("ordered", value);
                    } else {
                        groupMetaArgs.put(param, value);
                    }
                }
            }
            CRM.setParameters(
                        dcHost,
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
                        testOnly);
        }
        final CloneInfo ci = getCloneInfo();
        if (ci == null) {
            setLocations(heartbeatId, dcHost, testOnly);
        } else {
            ci.setLocations(heartbeatId, dcHost, testOnly);

        }
        if (!testOnly) {
            storeComboBoxValues(params);
            getBrowser().reload(getNode(), false);
        }
        for (final ServiceInfo child : getGroupServices()) {
            if (child.checkResourceFieldsCorrect(null,
                                                 child.getParametersFromXML(),
                                                 false,
                                                 false,
                                                 true)
                && child.checkResourceFieldsChanged(
                                                null,
                                                child.getParametersFromXML(),
                                                false,
                                                false,
                                                true)) {
                child.apply(dcHost, testOnly);
            }
        }
        if (!testOnly) {
            setApplyButtons(null, params);
        }
        getBrowser().getCRMGraph().repaint();
    }

    /** Returns the list of services that can be added to the group. */
    List<ResourceAgent> getAddGroupServiceList(final String cl) {
        return getBrowser().getCRMXML().getServices(cl);
    }

    /**
     * Adds service to this group. Adds it in the submenu in the menu tree
     * and initializes it.
     *
     * @param newServiceInfo
     *      service info object of the new service
     */
    void addGroupServicePanel(final ServiceInfo newServiceInfo,
                              final boolean reloadNode) {
        final DefaultMutableTreeNode gn = getNode();
        if (gn == null) {
            return;
        }
        newServiceInfo.getService().setResourceClass(
                        newServiceInfo.getResourceAgent().getResourceClass());
        newServiceInfo.setGroupInfo(this);
        getBrowser().addNameToServiceInfoHash(newServiceInfo);
        getBrowser().addToHeartbeatIdList(newServiceInfo);
        final DefaultMutableTreeNode newServiceNode =
                                   new DefaultMutableTreeNode(newServiceInfo);
        newServiceInfo.setNode(newServiceNode);
        mGroupServiceWriteLock.lock();
        try {
            groupServices.add(newServiceInfo);
        } finally {
            mGroupServiceWriteLock.unlock();
        }
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                gn.add(newServiceNode);
                if (reloadNode) {
                    getBrowser().reloadAndWait(gn, false);
                    getBrowser().reloadAndWait(newServiceNode, true);
                }
            }
        });
    }

    /** Adds service to this group and creates new service info object. */
    ServiceInfo addGroupServicePanel(final ResourceAgent newRA,
                              final boolean reloadNode) {
        ServiceInfo newServiceInfo;

        final String name = newRA.getName();
        if (newRA.isFilesystem()) {
            newServiceInfo = new FilesystemInfo(name, newRA, getBrowser());
        } else if (newRA.isLinbitDrbd()) {
            newServiceInfo = new LinbitDrbdInfo(name, newRA, getBrowser());
        } else if (newRA.isDrbddisk()) {
            newServiceInfo = new DrbddiskInfo(name, newRA, getBrowser());
        } else if (newRA.isIPaddr()) {
            newServiceInfo = new IPaddrInfo(name, newRA, getBrowser());
        } else if (newRA.isVirtualDomain()) {
            newServiceInfo = new VirtualDomainInfo(name, newRA, getBrowser());
        } else if (newRA.isGroup()) {
            LOG.appError("addGroupServicePanel: no groups in group allowed");
            return null;
        } else {
            newServiceInfo = new ServiceInfo(name, newRA, getBrowser());
        }
        addGroupServicePanel(newServiceInfo, reloadNode);
        return newServiceInfo;
    }

    /**
     * Returns on which node this group is running, meaning on which node
     * all the services are running. Null if they running on different
     * nodes or not at all.
     */
    @Override
    List<String> getRunningOnNodes(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                      getHeartbeatId(testOnly),
                                                      testOnly);
        final List<String> allNodes = new ArrayList<String>();
        if (resources != null) {
            for (final String hbId : resources) {
                final List<String> ns = cs.getRunningOnNodes(hbId,
                                                             testOnly);
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
    List<String> getMasterOnNodes(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                      getHeartbeatId(testOnly),
                                                      testOnly);
        final List<String> allNodes = new ArrayList<String>();
        if (resources != null) {
            for (final String hbId : resources) {
                final List<String> ns = cs.getMasterOnNodes(hbId,
                                                            testOnly);
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
    void startResource(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        for (final ServiceInfo child : getGroupServices()) {
            CRM.startResource(dcHost, child.getHeartbeatId(testOnly), testOnly);
        }
    }

    /** Stops all resources in the group. */
    @Override
    void stopResource(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        for (final ServiceInfo child : getGroupServices()) {
            CRM.stopResource(dcHost, child.getHeartbeatId(testOnly), testOnly);
        }
    }

    /** Cleans up all resources in the group. */
    @Override
    void cleanupResource(final Host dcHost, final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        for (final ServiceInfo child : getGroupServices()) {
            child.cleanupResource(dcHost, testOnly);
        }
    }

    /** Sets whether the group services are managed. */
    @Override
    void setManaged(final boolean isManaged,
                    final Host dcHost,
                    final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        for (final ServiceInfo child : getGroupServices()) {
            CRM.setManaged(dcHost,
                           child.getHeartbeatId(testOnly),
                           isManaged,
                           testOnly);
        }
    }

    /** Returns items for the group popup. */
    @Override
    public List<UpdatableItem> createPopup() {
        final boolean testOnly = false;
        final GroupInfo thisGroupInfo = this;
        /* add group service */
        final MyMenu addGroupServiceMenuItem = new MyMenu(
                        Tools.getString("ClusterBrowser.Hb.AddGroupService"),
                        new AccessMode(ConfigData.AccessType.ADMIN, false),
                        new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            private final Lock mUpdateLock = new ReentrantLock();

            @Override
            public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                } else {
                    return null;
                }
            }

            @Override
            public void update() {
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (mUpdateLock.tryLock()) {
                            try {
                                updateThread();
                            } finally {
                                mUpdateLock.unlock();
                            }
                        }
                    }
                });
                t.start();
            }

            private void updateThread() {
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        setEnabled(false);
                    }
                });
                Tools.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        removeAll();
                    }
                });
                final List<JDialog> popups = new ArrayList<JDialog>();
                for (final String cl : ClusterBrowser.HB_CLASSES) {
                    final MyMenu classItem =
                            new MyMenu(ClusterBrowser.HB_CLASS_MENU.get(cl),
                                   new AccessMode(ConfigData.AccessType.ADMIN,
                                                  false),
                                   new AccessMode(ConfigData.AccessType.OP,
                                                  false));
                    MyListModel<MyMenuItem> dlm = new MyListModel<MyMenuItem>();
                    for (final ResourceAgent ra : getAddGroupServiceList(cl)) {
                        final MyMenuItem mmi =
                            new MyMenuItem(
                                   ra.getMenuName(),
                                   null,
                                   null,
                                   new AccessMode(ConfigData.AccessType.ADMIN,
                                                  false),
                                   new AccessMode(ConfigData.AccessType.OP,
                                                  false)) {
                            private static final long serialVersionUID = 1L;
                            @Override
                            public void action() {
                                final CloneInfo ci = getCloneInfo();
                                if (ci != null) {
                                    ci.hidePopup();
                                }
                                hidePopup();
                                for (final JDialog otherP : popups) {
                                    otherP.dispose();
                                }
                                if (ra.isLinbitDrbd()
                                    && !getBrowser()
                                                .linbitDrbdConfirmDialog()) {
                                    return;
                                }
                                addGroupServicePanel(ra, true);
                                repaint();
                            }
                        };
                        dlm.addElement(mmi);
                    }
                    final boolean ret = Tools.getScrollingMenu(
                                ClusterBrowser.HB_CLASS_MENU.get(cl),
                                null, /* options */
                                classItem,
                                dlm,
                                new MyList<MyMenuItem>(dlm, getBackground()),
                                thisGroupInfo,
                                popups,
                                null);
                    if (!ret) {
                        classItem.setEnabled(false);
                    }
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            add(classItem);
                        }
                    });
                }
                Tools.waitForSwing();
                super.update();
            }
        };
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        items.add((UpdatableItem) addGroupServiceMenuItem);
        for (final UpdatableItem item : super.createPopup()) {
            items.add(item);
        }

        /* group services */
        if (!Tools.getConfigData().isSlow()) {
            for (final ServiceInfo child : getGroupServices()) {
                final MyMenu groupServicesMenu = new MyMenu(
                        child.toString(),
                        new AccessMode(ConfigData.AccessType.RO, false),
                        new AccessMode(ConfigData.AccessType.RO, false)) {
                    private static final long serialVersionUID = 1L;
                    private final Lock mUpdateLock = new ReentrantLock();

                    @Override
                    public String enablePredicate() {
                        return null;
                    }

                    @Override
                    public void update() {
                        final Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (mUpdateLock.tryLock()) {
                                    try {
                                        updateThread();
                                    } finally {
                                        mUpdateLock.unlock();
                                    }
                                }
                            }
                        });
                        t.start();
                    }

                    public void updateThread() {
                        Tools.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                setEnabled(false);
                            }
                        });
                        Tools.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                removeAll();
                            }
                        });
                        final List<UpdatableItem> serviceMenus =
                                        new ArrayList<UpdatableItem>();
                        for (final UpdatableItem u : child.createPopup()) {
                            serviceMenus.add(u);
                            u.update();
                        }
                        Tools.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                for (final UpdatableItem u
                                                     : serviceMenus) {
                                    add((JMenuItem) u);
                                }
                            }
                        });
                        super.update();
                    }
                };
                items.add((UpdatableItem) groupServicesMenu);
            }
        }
        return items;
    }

    /** Removes this group from the cib. */
    @Override
    public void removeMyself(final boolean testOnly) {
        if (getService().isNew()) {
            removeMyselfNoConfirm(getBrowser().getDCHost(), testOnly);
            getService().setNew(false);
            super.removeInfo();
            getService().doneRemoving();
            return;
        }
        String desc = Tools.getString(
                              "ClusterBrowser.confirmRemoveGroup.Description");

        final StringBuilder services = new StringBuilder();

        boolean first = true;
        for (final ServiceInfo child : getGroupServices()) {
            if (!first) {
                services.append(", ");
            }
            services.append(child.toString());
            first = false;
        }

        desc  = desc.replaceAll(
                            "@GROUP@",
                            "'" + Matcher.quoteReplacement(toString()) + "'");
        desc  = desc.replaceAll("@SERVICES@",
                                Matcher.quoteReplacement(services.toString()));
        if (Tools.confirmDialog(
                Tools.getString("ClusterBrowser.confirmRemoveGroup.Title"),
                desc,
                Tools.getString("ClusterBrowser.confirmRemoveGroup.Yes"),
                Tools.getString("ClusterBrowser.confirmRemoveGroup.No"))) {
            if (!testOnly) {
                getService().setRemoved(true);
            }
            removeMyselfNoConfirm(getBrowser().getDCHost(), testOnly);
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
    public void removeMyselfNoConfirm(final Host dcHost,
                                      final boolean testOnly) {
        final List<ServiceInfo> children = new ArrayList<ServiceInfo>();
        if (!testOnly) {
            for (final ServiceInfo child : getGroupServices()) {
                child.getService().setRemoved(true);
                children.add(child);
            }
        }
        if (getService().isNew()) {
            if (!testOnly) {
                getService().setNew(false);
                getBrowser().getCRMGraph().killRemovedVertices();
                getService().doneRemoving();
            }
        } else {
            String cloneId = null;
            boolean master = false;
            final CloneInfo ci = getCloneInfo();
            if (ci != null) {
                cloneId = ci.getHeartbeatId(testOnly);
                master = ci.getService().isMaster();
            }
            super.removeMyselfNoConfirm(dcHost, testOnly);
            for (final ServiceInfo child : children) {
                child.removeConstraints(dcHost, testOnly);
            }
            CRM.removeResource(dcHost,
                               null,
                               getHeartbeatId(testOnly),
                               cloneId, /* clone id */
                               master,
                               testOnly);
            for (final ServiceInfo child : children) {
                child.cleanupResource(dcHost, testOnly);
            }
        }
        if (!testOnly) {
            for (final ServiceInfo child : children) {
                getBrowser().mHeartbeatIdToServiceLock();
                getBrowser().getHeartbeatIdToServiceInfo().remove(
                                         child.getService().getHeartbeatId());
                getBrowser().mHeartbeatIdToServiceUnlock();
                getBrowser().removeFromServiceInfoHash(child);
                child.cleanup();
                child.getService().doneRemoving();
            }
        }
    }

    /** Removes the group, but not the services. */
    void removeMyselfNoConfirmFromChild(final Host dcHost,
                                        final boolean testOnly) {
        super.removeMyselfNoConfirm(dcHost, testOnly);
    }

    /** Returns tool tip for the group vertex. */
    @Override
    public String getToolTipText(final boolean testOnly) {
        final List<String> hostNames = getRunningOnNodes(testOnly);
        final StringBuilder sb = new StringBuilder(220);
        sb.append("<b>");
        sb.append(toString());
        if (hostNames == null || hostNames.isEmpty()) {
            sb.append(" not running");
        } else if (hostNames.size() == 1) {
            sb.append(" running on node: ");
        } else {
            sb.append(" running on nodes: ");
        }
        if (hostNames != null && !hostNames.isEmpty()) {
            sb.append(Tools.join(", ", hostNames.toArray(
                                               new String[hostNames.size()])));
        }
        sb.append("</b>");

        for (final ServiceInfo child : getGroupServices()) {
            sb.append("\n&nbsp;&nbsp;&nbsp;");
            sb.append(child.getToolTipText(testOnly));
        }

        return sb.toString();
    }

    /** Returns whether one of the services on one of the hosts failed. */
    @Override
    boolean isOneFailed(final boolean testOnly) {
        for (final ServiceInfo child : getGroupServices()) {
            if (child.isOneFailed(testOnly)) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether one of the services on one of the hosts failed. */
    @Override
    boolean isOneFailedCount(final boolean testOnly) {
        for (final ServiceInfo child : getGroupServices()) {
            if (child.isOneFailedCount(testOnly)) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether one of the services failed to start. */
    @Override
    public boolean isFailed(final boolean testOnly) {
        for (final ServiceInfo child : getGroupServices()) {
            if (child.isFailed(testOnly)) {
                return true;
            }
        }
        return false;
    }

    /** Returns subtexts that appears in the service vertex. */
    @Override
    public Subtext[] getSubtextsForGraph(final boolean testOnly) {
        final List<Subtext> texts = new ArrayList<Subtext>();
        Subtext prevSubtext = null;
        final Host dcHost = getBrowser().getDCHost();

        for (final ServiceInfo child : getGroupServices()) {
            final Subtext[] subtexts = child.getSubtextsForGraph(testOnly);

            if (subtexts == null || subtexts.length == 0) {
                continue;
            }
            final Subtext sSubtext = subtexts[0];
            if (prevSubtext == null
                || !sSubtext.getSubtext().equals(
                                      prevSubtext.getSubtext())) {
                texts.add(new Subtext(sSubtext.getSubtext()
                                      + ":",
                                      sSubtext.getColor(),
                                      Color.BLACK));
                prevSubtext = sSubtext;
            }
            String unmanaged = "";
            if (!child.isManaged(testOnly)) {
                unmanaged = " / unmanaged";
            }
            String migrated = "";
            if (child.getMigratedTo(testOnly) != null
                || child.getMigratedFrom(testOnly) != null) {
                migrated = " / migrated";
            }
            final HbConnectionInfo[] hbcis =
                          getBrowser().getCRMGraph().getHbConnections(child);
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
                    if (!someConnection
                        && hbci.hasColocationOrOrder(child)) {
                        someConnection = true;
                    }
                    if (!scoreFirst
                        && !hbci.isOrdScoreNull(child, null)) {
                        scoreFirst = true;
                    }
                    if (!scoreThen
                        && !hbci.isOrdScoreNull(null, child)) {
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
            texts.add(new Subtext("   "
                                  + constraintLeft
                                  + child.toString()
                                  + unmanaged
                                  + migrated
                                  + constraint,
                                  sSubtext.getColor(),
                                  Color.BLACK));
            boolean skip = true;
            for (final Subtext st : subtexts) {
                if (skip) {
                    skip = false;
                    continue;
                }
                texts.add(new Subtext("   " + st.getSubtext(),
                                      st.getColor(),
                                      Color.BLACK));
            }
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /**
     * Returns from which hosts the services or the whole group was migrated.
     */
    @Override
    public List<Host> getMigratedFrom(final boolean testOnly) {
        List<Host> hosts = super.getMigratedFrom(testOnly);
        final List<ServiceInfo> gs = getGroupServices();
        if (gs.isEmpty()) {
            return null;
        }
        for (final ServiceInfo child : gs) {
            final List<Host> siHosts = child.getMigratedFrom(testOnly);
            if (siHosts != null) {
                if (hosts == null) {
                    hosts = new ArrayList<Host>();
                }
                hosts.addAll(siHosts);
            }
        }
        return hosts;
    }

    /** Returns whether at least one service is unmaneged. */
    @Override
    public boolean isManaged(final boolean testOnly) {
        final List<ServiceInfo> gs = getGroupServices();
        if (gs.isEmpty()) {
            return true;
        }
        for (final ServiceInfo child : gs) {
            if (!child.isManaged(testOnly)) {
                return false;
            }
        }
        return true;
    }

    /** Returns whether all of the services are started. */
    @Override
    boolean isStarted(final boolean testOnly) {
        for (final ServiceInfo child : getGroupServices()) {
            if (!child.isStarted(testOnly)) {
                return false;
            }
        }
        return true;
    }

    /** Returns whether one of the services is stopped. */
    @Override
    public boolean isStopped(final boolean testOnly) {
        for (final ServiceInfo child : getGroupServices()) {
            if (child.isStopped(testOnly)) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether the group is stopped. */
    @Override
    boolean isGroupStopped(final boolean testOnly) {
        return super.isStopped(testOnly);
    }

    /** Returns true if at least one service in the group are running. */
    boolean isOneRunning(final boolean testOnly) {
        for (final ServiceInfo child : getGroupServices()) {
            if (child.isRunning(testOnly)) {
                return true;
            }
        }
        return false;
    }

    /** Returns true if all services in the group are running. */
    @Override
    public boolean isRunning(final boolean testOnly) {
        for (final ServiceInfo child : getGroupServices()) {
            if (!child.isRunning(testOnly)) {
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
    public boolean checkResourceFieldsChanged(final String param,
                                              final String[] params) {
        return checkResourceFieldsChanged(param, params, false, false);
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If group does not have any services, its changes
     * cannot by applied.
     */
    boolean checkResourceFieldsChanged(final String param,
                                       final String[] params,
                                       final boolean fromServicesInfo,
                                       final boolean fromCloneInfo) {
        boolean changed = super.checkResourceFieldsChanged(param,
                                                           params,
                                                           fromServicesInfo,
                                                           fromCloneInfo,
                                                           true);
        for (final ServiceInfo child : getGroupServices()) {
            if (child.checkResourceFieldsChanged(null,
                                                 child.getParametersFromXML(),
                                                 fromServicesInfo,
                                                 fromCloneInfo,
                                                 true)) {
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    @Override
    public boolean checkResourceFieldsCorrect(final String param,
                                              final String[] params) {
        return checkResourceFieldsCorrect(param, params, false, false);
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    boolean checkResourceFieldsCorrect(final String param,
                                       final String[] params,
                                       final boolean fromServicesInfo,
                                       final boolean fromCloneInfo) {
        boolean cor = super.checkResourceFieldsCorrect(param,
                                                       params,
                                                       fromServicesInfo,
                                                       fromCloneInfo,
                                                       true);
        for (final ServiceInfo child : getGroupServices()) {
            if (!child.checkResourceFieldsCorrect(null,
                                                  child.getParametersFromXML(),
                                                  fromServicesInfo,
                                                  fromCloneInfo,
                                                  true)) {
                cor = false;
            }
        }
        return cor;
    }

    /** Update menus with positions and calles their update methods. */
    @Override
    void updateMenus(final Point2D pos) {
        super.updateMenus(pos);
        if (!Tools.getConfigData().isSlow()) {
            for (final ServiceInfo child : getGroupServices()) {
                child.updateMenus(pos);
            }
        }
    }

    /** Adds existing service menu item for every member of a group. */
    @Override
    protected void addExistingGroupServiceMenuItems(
                        final ServiceInfo asi,
                        final MyListModel<MyMenuItem> dlm,
                        final Map<MyMenuItem, ButtonCallback> callbackHash,
                        final MyList<MyMenuItem> list,
                        final JCheckBox colocationCB,
                        final JCheckBox orderCB,
                        final List<JDialog> popups,
                        final boolean testOnly) {
        for (final ServiceInfo child : getGroupServices()) {
            asi.addExistingServiceMenuItem("         " + child.toString(),
                                           child,
                                           dlm,
                                           callbackHash,
                                           list,
                                           colocationCB,
                                           orderCB,
                                           popups,
                                           testOnly);
        }
    }

    /** Returns the icon for the category. */
    @Override
    public ImageIcon getCategoryIcon(final boolean testOnly) {
        if (getBrowser().allHostsDown() || !isOneRunning(testOnly)) {
            return ServiceInfo.SERVICE_STOPPED_ICON_SMALL;
        }
        return ServiceInfo.SERVICE_RUNNING_ICON_SMALL;
    }

    /** Revert all values. */
    @Override
    public void revert() {
        super.revert();
        for (final ServiceInfo child : getGroupServices()) {
            if (child.checkResourceFieldsChanged(null,
                                                 child.getParametersFromXML(),
                                                 true,
                                                 false,
                                                 false)) {
                child.revert();
            }
        }
    }

    /** Return copy of the group services. */
    private List<ServiceInfo> getGroupServices() {
        mGroupServiceReadLock.lock();
        try {
            return Collections.unmodifiableList(
                                    new ArrayList<ServiceInfo>(groupServices));
        } finally {
            mGroupServiceReadLock.unlock();
        }
    }
}
