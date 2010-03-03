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
package drbd.gui.resources;

import drbd.gui.Browser;
import drbd.gui.ClusterBrowser;
import drbd.gui.GuiComboBox;
import drbd.data.ResourceAgent;
import drbd.data.Host;
import drbd.data.CRMXML;
import drbd.data.ClusterStatus;
import drbd.data.Subtext;
import drbd.utilities.UpdatableItem;
import drbd.utilities.CRM;
import drbd.utilities.MyMenu;
import drbd.utilities.Tools;
import drbd.utilities.MyList;
import drbd.utilities.MyMenuItem;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.SwingUtilities;
import javax.swing.DefaultListModel;
import javax.swing.JMenuItem;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JScrollPane;
import java.awt.geom.Point2D;

/**
 * GroupInfo class holds data for heartbeat group, that is in some ways
 * like normal service, but it can contain other services.
 */
public class GroupInfo extends ServiceInfo {
    /**
     * Creates new GroupInfo object.
     */
    public GroupInfo(final ResourceAgent ra, final Browser browser) {
        super(ClusterBrowser.PM_GROUP_NAME, ra, browser);
    }

    /**
     * Applies the changes to the group parameters.
     */
    public final void apply(final Host dcHost, final boolean testOnly) {
        final String[] params = getParametersFromXML();
        if (!testOnly) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    applyButton.setEnabled(false);
                    applyButton.setToolTipText(null);
                    final GuiComboBox idField = paramComboBoxGet(GUI_ID, null);
                    idField.setEnabled(false);
                }
            });

            /* add myself to the hash with service name and id as
             * keys */
            getBrowser().removeFromServiceInfoHash(this);
        }
        final String oldHeartbeatId = getHeartbeatId(testOnly);
        if (!testOnly) {
            if (oldHeartbeatId != null) {
                getBrowser().getHeartbeatIdToServiceInfo().remove(
                                                               oldHeartbeatId);
                getBrowser().getHeartbeatIdList().remove(oldHeartbeatId);
            }
            if (getService().isNew()) {
                final String id = getComboBoxValue(GUI_ID);
                getService().setIdAndCrmId(id);
                if (getTypeRadioGroup() != null) {
                    getTypeRadioGroup().setEnabled(false);
                }
            }
            getBrowser().addToHeartbeatIdList(this);
            getBrowser().addNameToServiceInfoHash(this);
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
            final String[] parents =
                             getBrowser().getHeartbeatGraph().getParents(this);
            final List<Map<String, String>> colAttrsList =
                                       new ArrayList<Map<String, String>>();
            final List<Map<String, String>> ordAttrsList =
                                       new ArrayList<Map<String, String>>();
            for (final String parentId : parents) {
                final ServiceInfo parentInfo =
                                getBrowser().getServiceInfoFromCRMId(parentId);
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
                                      parents,
                                      colAttrsList,
                                      ordAttrsList,
                                      testOnly);
        }
        setLocations(heartbeatId, dcHost, testOnly);
        if (!testOnly) {
            storeComboBoxValues(params);
            getBrowser().reload(getNode());
            checkResourceFields(null, params);
        }
        getBrowser().getHeartbeatGraph().repaint();
    }

    /**
     * Returns the list of services that can be added to the group.
     */
    public final List<ResourceAgent> getAddGroupServiceList(final String cl) {
        return getBrowser().getCRMXML().getServices(cl);
    }

    /**
     * Adds service to this group. Adds it in the submenu in the menu tree
     * and initializes it.
     *
     * @param newServiceInfo
     *      service info object of the new service
     */
    public final void addGroupServicePanel(final ServiceInfo newServiceInfo,
                                           final boolean reloadNode) {
        newServiceInfo.getService().setResourceClass(
                    newServiceInfo.getResourceAgent().getResourceClass());
        newServiceInfo.setGroupInfo(this);
        getBrowser().addToHeartbeatIdList(newServiceInfo);
        final DefaultMutableTreeNode newServiceNode =
                                new DefaultMutableTreeNode(newServiceInfo);
        newServiceInfo.setNode(newServiceNode);
        getNode().add(newServiceNode);
        if (reloadNode) {
            getBrowser().reload(getNode());
            getBrowser().reload(newServiceNode);
        }
    }

    /**
     * Adds service to this group and creates new service info object.
     */
    public final void addGroupServicePanel(final ResourceAgent newRA,
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
            Tools.appError("No groups in group allowed");
            return;
        } else {
            newServiceInfo = new ServiceInfo(name, newRA, getBrowser());
        }
        addGroupServicePanel(newServiceInfo, reloadNode);
    }

    /**
     * Returns on which node this group is running, meaning on which node
     * all the services are running. Null if they running on different
     * nodes or not at all.
     */
    public final List<String> getRunningOnNodes(final boolean testOnly) {
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

    /**
     * Returns node name of the host where this service is running.
     */
    public List<String> getMasterOnNodes(final boolean testOnly) {
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

    /**
     * Starts all resources in the group.
     */
    public final void startResource(final Host dcHost,
                                    final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                     getHeartbeatId(testOnly),
                                                     testOnly);
        if (resources != null) {
            for (final String hbId : resources) {
                CRM.startResource(dcHost, hbId, testOnly);
            }
        }
    }

    /**
     * Stops all resources in the group.
     */
    public final void stopResource(final Host dcHost,
                                   final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                       getHeartbeatId(testOnly),
                                                       testOnly);
        if (resources != null) {
            for (final String hbId : resources) {
                CRM.stopResource(dcHost, hbId, testOnly);
            }
        }
    }

    /**
     * Cleans up all resources in the group.
     */
    public final void cleanupResource(final Host dcHost,
                                      final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                      getHeartbeatId(testOnly),
                                                      testOnly);
        if (resources != null) {
            for (final String hbId : resources) {
                getBrowser().getServiceInfoFromCRMId(hbId).cleanupResource(
                                                                   dcHost,
                                                                   testOnly);
            }
        }
    }

    /**
     * Sets whether the group services are managed.
     */
    public final void setManaged(final boolean isManaged,
                                 final Host dcHost,
                                 final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
        }
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                      getHeartbeatId(testOnly),
                                                      testOnly);
        if (resources != null) {
            for (final String hbId : resources) {
                CRM.setManaged(dcHost, hbId, isManaged, testOnly);
            }
        }
    }

    /**
     * Returns items for the group popup.
     */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem>items = super.createPopup();
        final boolean testOnly = false;
        /* add group service */
        final MyMenu addGroupServiceMenuItem = new MyMenu(
                        Tools.getString("ClusterBrowser.Hb.AddGroupService")) {
            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                return !getBrowser().clStatusFailed();
            }

            public void update() {
                super.update();

                removeAll();
                for (final String cl : ClusterBrowser.HB_CLASSES) {
                    final MyMenu classItem =
                            new MyMenu(ClusterBrowser.HB_CLASS_MENU.get(cl));
                    DefaultListModel dlm = new DefaultListModel();
                    for (final ResourceAgent ra : getAddGroupServiceList(cl)) {
                        final MyMenuItem mmi =
                                    new MyMenuItem(ra.getMenuName()) {
                            private static final long serialVersionUID = 1L;
                            public void action() {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        final CloneInfo ci = getCloneInfo();
                                        if (ci != null) {
                                            ci.getPopup().setVisible(false);
                                        }
                                        getPopup().setVisible(false);
                                    }
                                });
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
                    final JScrollPane jsp = Tools.getScrollingMenu(
                                              classItem,
                                              dlm,
                                              new MyList(dlm, getBackground()),
                                              null);
                    if (jsp == null) {
                        classItem.setEnabled(false);
                    } else {
                        classItem.add(jsp);
                    }
                    add(classItem);
                }
            }
        };
        items.add(0, (UpdatableItem) addGroupServiceMenuItem);
        registerMenuItem((UpdatableItem) addGroupServiceMenuItem);

        /* group services */
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                      getHeartbeatId(testOnly),
                                                      testOnly);
        if (resources != null) {
            for (final String hbId : resources) {
                final ServiceInfo gsi =
                                    getBrowser().getServiceInfoFromCRMId(hbId);
                final MyMenu groupServicesMenu = new MyMenu(gsi.toString()) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return true;
                    }

                    public void update() {
                        super.update();
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                removeAll();
                            }
                        });
                        for (final UpdatableItem u : gsi.createPopup()) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    add((JMenuItem) u);
                                    u.update();
                                }
                            });
                        }
                    }
                };
                items.add((UpdatableItem) groupServicesMenu);
                registerMenuItem((UpdatableItem) groupServicesMenu);
            }
        }
        return items;
    }

    /**
     * Removes this group from the cib.
     */
    public final void removeMyself(final boolean testOnly) {
        if (getService().isNew()) {
            removeMyselfNoConfirm(getBrowser().getDCHost(), testOnly);
            getService().setNew(false);
            getService().doneRemoving();
            return;
        }
        String desc = Tools.getString(
                              "ClusterBrowser.confirmRemoveGroup.Description");

        final StringBuffer services = new StringBuffer();

        final Enumeration e = getNode().children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode n =
                                  (DefaultMutableTreeNode) e.nextElement();
            final ServiceInfo child = (ServiceInfo) n.getUserObject();
            services.append(child.toString());
            if (e.hasMoreElements()) {
                services.append(", ");
            }

        }

        desc  = desc.replaceAll("@GROUP@", "'" + toString() + "'");
        desc  = desc.replaceAll("@SERVICES@", services.toString());
        if (Tools.confirmDialog(
                Tools.getString("ClusterBrowser.confirmRemoveGroup.Title"),
                desc,
                Tools.getString("ClusterBrowser.confirmRemoveGroup.Yes"),
                Tools.getString("ClusterBrowser.confirmRemoveGroup.No"))) {
            if (!testOnly) {
                getService().setRemoved(true);
            }
            removeMyselfNoConfirm(getBrowser().getDCHost(), testOnly);
            getService().setNew(false);
        }
        getService().doneRemoving();
    }

    /**
     * Remove all the services in the group and the group.
     */
    public final void removeMyselfNoConfirm(final Host dcHost,
                                            final boolean testOnly) {
        if (getService().isNew()) {
            if (!testOnly) {
                getService().setNew(false);
                getBrowser().getHeartbeatGraph().killRemovedVertices();
            }
        } else {
            super.removeMyselfNoConfirm(dcHost, testOnly);
            String cloneId = null;
            boolean master = false;
            if (getCloneInfo() != null) {
                cloneId = getCloneInfo().getHeartbeatId(testOnly);
                master = getCloneInfo().getService().isMaster();
            }
            CRM.removeResource(dcHost,
                               null,
                               getHeartbeatId(testOnly),
                               cloneId, /* clone id */
                               master,
                               testOnly);
        }
    }

    /**
     * Removes the group, but not the services.
     */
    public final void removeMyselfNoConfirmFromChild(final Host dcHost,
                                                     final boolean testOnly) {
        super.removeMyselfNoConfirm(dcHost, testOnly);
    }

    /**
     * Returns tool tip for the group vertex.
     */
    public final String getToolTipText(final boolean testOnly) {
        final List<String> hostNames = getRunningOnNodes(testOnly);
        final StringBuffer sb = new StringBuffer(220);
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

        final Enumeration e = getNode().children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode n =
                                (DefaultMutableTreeNode) e.nextElement();
            final ServiceInfo child = (ServiceInfo) n.getUserObject();
            sb.append("\n&nbsp;&nbsp;&nbsp;");
            sb.append(child.getToolTipText(testOnly));
        }

        return sb.toString();
    }

    /**
     * Returns whether one of the services on one of the hosts failed.
     */
    public final boolean isOneFailed(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                     getHeartbeatId(testOnly),
                                                     testOnly);
        if (resources != null) {
            for (final String hbId : resources) {
                if (getBrowser().getServiceInfoFromCRMId(hbId).isOneFailed(
                                                                  testOnly)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns whether one of the services on one of the hosts failed.
     */
    public final boolean isOneFailedCount(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                     getHeartbeatId(testOnly),
                                                     testOnly);
        if (resources != null) {
            for (final String hbId : resources) {
                if (getBrowser().getServiceInfoFromCRMId(
                                           hbId).isOneFailedCount(testOnly)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns whether one of the services failed to start.
     */
    public final boolean isFailed(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                   getHeartbeatId(testOnly),
                                                   testOnly);
        if (resources != null) {
            for (final String hbId : resources) {
                final ServiceInfo si =
                                   getBrowser().getServiceInfoFromCRMId(hbId);
                if (si == null) {
                    continue;
                }
                if (si.isFailed(testOnly)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns subtexts that appears in the service vertex.
     */
    public final Subtext[] getSubtextsForGraph(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                   getHeartbeatId(testOnly),
                                                   testOnly);
        final List<Subtext> texts = new ArrayList<Subtext>();
        Subtext prevSubtext = null;
        final Host dcHost = getBrowser().getDCHost();
        if (resources != null) {
            for (final String hbId : resources) {
                final ServiceInfo si =
                                   getBrowser().getServiceInfoFromCRMId(hbId);
                if (si != null) {
                    final Subtext[] subtexts =
                                         si.getSubtextsForGraph(testOnly);
                    Subtext sSubtext = null;
                    if (subtexts == null || subtexts.length == 0) {
                        continue;
                    }
                    sSubtext = subtexts[0];
                    if (prevSubtext == null
                        || !sSubtext.getSubtext().equals(
                                              prevSubtext.getSubtext())) {
                        texts.add(new Subtext(sSubtext.getSubtext()
                                              + ":",
                                              sSubtext.getColor()));
                        prevSubtext = sSubtext;
                    }
                    if (si != null) {
                        String unmanaged = "";
                        if (!si.isManaged(testOnly)) {
                            unmanaged = " / unmanaged";
                        }
                        String migrated = "";
                        if (si.getMigratedTo(testOnly) != null) {
                            migrated = " / migrated";
                        }
                        texts.add(new Subtext("   " + si.toString()
                                              + unmanaged
                                              + migrated,
                                              sSubtext.getColor()));
                        boolean skip = true;
                        for (final Subtext st : subtexts) {
                            if (skip) {
                                skip = false;
                                continue;
                            }
                            texts.add(new Subtext("   " + st.getSubtext(),
                                                  st.getColor()));
                        }
                    }
                }
            }
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /**
     * Returns whether all some services or the whole group is migrated.
     */
    public final List<Host> getMigratedTo(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                      getHeartbeatId(testOnly),
                                                      testOnly);
        List<Host> hosts = super.getMigratedTo(testOnly);
        if (resources == null) {
            return null;
        } else {
            if (resources.isEmpty()) {
                return null;
            }
            for (final String hbId : resources) {
                final ServiceInfo si =
                                    getBrowser().getServiceInfoFromCRMId(hbId);
                if (si != null) {
                    final List<Host> siHosts = si.getMigratedTo(testOnly);
                    if (siHosts != null) {
                        if (hosts == null) {
                            hosts = new ArrayList<Host>();
                        }
                        hosts.addAll(siHosts);
                    }
                }
            }
        }
        return hosts;
    }


    /**
     * Returns whether at least one service is unmaneged.
     */
    public final boolean isManaged(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                      getHeartbeatId(testOnly),
                                                      testOnly);
        if (resources == null) {
            return true;
        } else {
            if (resources.isEmpty()) {
                return true;
            }
            for (final String hbId : resources) {
                final ServiceInfo si =
                                    getBrowser().getServiceInfoFromCRMId(hbId);
                if (si != null && !si.isManaged(testOnly)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns whether all of the services are started.
     */
    public final boolean isStarted(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                      getHeartbeatId(testOnly),
                                                      testOnly);
        if (resources != null) {
            for (final String hbId : resources) {
                final ServiceInfo si =
                                    getBrowser().getServiceInfoFromCRMId(hbId);
                if (si != null && !si.isStarted(testOnly)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns whether one of the services is stopped.
     */
    public final boolean isStopped(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                    getHeartbeatId(testOnly),
                                                    testOnly);
        if (resources != null) {
            for (final String hbId : resources) {
                final ServiceInfo si =
                                    getBrowser().getServiceInfoFromCRMId(hbId);
                if (si != null && si.isStopped(testOnly)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if all services in the group are running.
     */
    public final boolean isRunning(final boolean testOnly) {
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                 getHeartbeatId(testOnly),
                                                 testOnly);
        if (resources != null) {
            for (final String hbId : resources) {
                final ServiceInfo si =
                                    getBrowser().getServiceInfoFromCRMId(hbId);
                if (si != null && !si.isRunning(testOnly)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If group does not have any services, its changes
     * cannot by applied.
     */
    public final boolean checkResourceFieldsChanged(final String param,
                                                    final String[] params) {
        final DefaultMutableTreeNode n = getNode();
        if (n == null) {
            return false;
        }
        final Enumeration e = n.children();
        if (!e.hasMoreElements()) {
            return false;
        }
        return super.checkResourceFieldsChanged(param, params);
    }

    /**
     * Update menus with positions and calles their update methods.
     */
    public final void updateMenus(final Point2D pos) {
        super.updateMenus(pos);
        final ClusterStatus cs = getBrowser().getClusterStatus();
        final List<String> resources = cs.getGroupResources(
                                                 getHeartbeatId(false),
                                                 false);
        if (resources != null) {
            for (final String hbId : resources) {
                final ServiceInfo si =
                                    getBrowser().getServiceInfoFromCRMId(hbId);
                si.updateMenus(pos);
            }
        }
    }

}
