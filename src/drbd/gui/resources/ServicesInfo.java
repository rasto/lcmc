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
import drbd.gui.HeartbeatGraph;
import drbd.gui.dialog.ClusterLogs;
import drbd.data.Host;
import drbd.data.ResourceAgent;
import drbd.data.ClusterStatus;
import drbd.data.resources.Resource;
import drbd.data.PtestData;
import drbd.data.CRMXML;
import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.utilities.Unit;
import drbd.utilities.UpdatableItem;
import drbd.utilities.CRM;
import drbd.utilities.Tools;
import drbd.utilities.ButtonCallback;
import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;
import drbd.utilities.MyList;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.DefaultListModel;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * This class holds info data for services view and global heartbeat
 * config.
 */
public class ServicesInfo extends EditableInfo {
    /** Cache for the info panel. */
    private JComponent infoPanel = null;

    /**
     * Prepares a new <code>ServicesInfo</code> object.
     */
    public ServicesInfo(final String name, final Browser browser) {
        super(name, browser);
        setResource(new Resource(name));
    }

    /**
     * Returns browser object of this info.
     */
    protected final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /**
     * Sets info panel.
     */
    public final void setInfoPanel(final JComponent infoPanel) {
        this.infoPanel = infoPanel;
    }

    /**
     * Returns icon for services menu item.
     */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        return null;
    }

    /**
     * Returns names of all global parameters.
     */
    public final String[] getParametersFromXML() {
        final CRMXML crmxml = getBrowser().getCRMXML();
        if (crmxml == null) {
            return null;
        }
        return crmxml.getGlobalParameters();
    }

    /**
     * Returns long description of the global parameter, that is used for
     * tool tips.
     */
    protected final String getParamLongDesc(final String param) {
        return getBrowser().getCRMXML().getGlobalParamLongDesc(param);
    }

    /**
     * Returns short description of the global parameter, that is used as
     * label.
     */
    protected final String getParamShortDesc(final String param) {
        return getBrowser().getCRMXML().getGlobalParamShortDesc(param);
    }

    /**
     * Returns default for this global parameter.
     */
    protected final String getParamDefault(final String param) {
        return getBrowser().getCRMXML().getGlobalParamDefault(param);
    }

    /**
     * Returns preferred value for this global parameter.
     */
    protected final String getParamPreferred(final String param) {
        return getBrowser().getCRMXML().getGlobalParamPreferred(param);
    }

    /**
     * Returns possible choices for pulldown menus if applicable.
     */
    protected final Object[] getParamPossibleChoices(final String param) {
        return getBrowser().getCRMXML().getGlobalParamPossibleChoices(param);
    }

    /**
     * Checks if the new value is correct for the parameter type and
     * constraints.
     */
    protected final boolean checkParam(final String param,
                                       final String newValue) {
        return getBrowser().getCRMXML().checkGlobalParam(param, newValue);
    }

    /** Returns whether the global parameter is of the integer type. */
    protected final boolean isInteger(final String param) {
        return getBrowser().getCRMXML().isGlobalInteger(param);
    }
    /** Returns whether the global parameter is of the label type. */
    protected final boolean isLabel(final String param) {
        return getBrowser().getCRMXML().isGlobalLabel(param);
    }

    /**
     * Returns whether the global parameter is of the time type.
     */
    protected final boolean isTimeType(final String param) {
        return getBrowser().getCRMXML().isGlobalTimeType(param);
    }

    /** Returns whether this parameter is advanced. */
    protected final boolean isAdvanced(final String param) {
        if (!Tools.areEqual(getParamDefault(param),
                            getParamSaved(param))) {
            /* it changed, show it */
            return false;
        }
        return getBrowser().getCRMXML().isGlobalAdvanced(param);
    }

    /** Returns access type of this parameter. */
    protected final ConfigData.AccessType getAccessType(final String param) {
        return getBrowser().getCRMXML().getGlobalAccessType(param);
    }

    /** Whether the parameter should be enabled. */
    protected final boolean isEnabled(final String param) {
        return true;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    protected final boolean isEnabledOnlyInAdvancedMode(final String param) {
        return false;
    }

    /**
     * Returns whether the global parameter is required.
     */
    protected final boolean isRequired(final String param) {
        return getBrowser().getCRMXML().isGlobalRequired(param);
    }

    /**
     * Returns whether the global parameter is of boolean type and
     * requires a checkbox.
     */
    protected final boolean isCheckBox(final String param) {
        return getBrowser().getCRMXML().isGlobalBoolean(param);
    }

    /**
     * Returns type of the global parameter.
     */
    protected final String getParamType(final String param) {
        return getBrowser().getCRMXML().getGlobalParamType(param);
    }

    /**
     * Returns section to which the global parameter belongs.
     */
    protected final String getSection(final String param) {
        return getBrowser().getCRMXML().getGlobalSection(param);
    }

    /**
     * Applies changes that user has entered.
     */
    public final void apply(final Host dcHost, final boolean testOnly) {
        final String[] params = getParametersFromXML();
        if (!testOnly) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    getApplyButton().setEnabled(false);
                    getApplyButton().setToolTipText(null);
                }
            });
        }

        /* update pacemaker */
        final Map<String, String> args = new HashMap<String, String>();
        for (final String param : params) {
            final String value = getComboBoxValue(param);
            if (value.equals(getParamDefault(param))) {
                continue;
            }

            if ("".equals(value)) {
                continue;
            }
            args.put(param, value);
        }
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        final String[] rdiParams = rdi.getParametersFromXML();
        final Map<String, String> rdiMetaArgs =
                                           new LinkedHashMap<String, String>();
        for (final String param : rdiParams) {
            final String value = rdi.getComboBoxValue(param);
            if (value.equals(rdi.getParamDefault(param))) {
                    continue;
            }
            if (!"".equals(value)) {
                rdiMetaArgs.put(param, value);
            }
        }
        final String rscDefaultsId =
                    getBrowser().getClusterStatus().getRscDefaultsId(testOnly);
        CRM.setGlobalParameters(dcHost,
                                args,
                                rdiMetaArgs,
                                rscDefaultsId,
                                testOnly);
        if (!testOnly) {
            storeComboBoxValues(params);
            rdi.storeComboBoxValues(rdiParams);
            checkResourceFields(null, params);
        }
    }

    /**
     * Sets heartbeat global parameters after they were obtained.
     */
    public final void setGlobalConfig() {
        final String[] params = getParametersFromXML();
        for (String param : params) {
            final String value =
                         getBrowser().getClusterStatus().getGlobalParam(param);
            final String oldValue = getParamSaved(param);
            if (value != null && !value.equals(oldValue)) {
                getResource().setValue(param, value);
                final GuiComboBox cb = paramComboBoxGet(param, null);
                if (cb != null) {
                    cb.setValue(value);
                }
            }
        }
        if (infoPanel == null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    getInfoPanel();
                }
            });
        }
    }

    /**
     * Check if this connection is filesystem with drbd ra and if so, set
     * it.
     */
    private void setFilesystemWithDrbd(final ServiceInfo siP,
                                       final ServiceInfo si) {
        if (siP.getResourceAgent().isLinbitDrbd()) {
            /* linbit::drbd -> Filesystem */
            ((FilesystemInfo) si).setLinbitDrbdInfo((LinbitDrbdInfo) siP);
        } else {
            /* drbddisk -> Filesystem */
            ((FilesystemInfo) si).setDrbddiskInfo((DrbddiskInfo) siP);
        }
    }

    /**
     * Sets clone info object.
     */
    private CloneInfo setCreateCloneInfo(final String cloneId,
                                         final boolean testOnly) {
        CloneInfo newCi = null;
        newCi = (CloneInfo) getBrowser().getServiceInfoFromCRMId(cloneId);
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        if (newCi == null) {
            final Point2D p = null;
            newCi =
               (CloneInfo) addServicePanel(
                                        getBrowser().getCRMXML().getHbClone(),
                                        p,
                                        false,
                                        cloneId,
                                        null,
                                        testOnly);
            newCi.getService().setNew(false);
            getBrowser().addToHeartbeatIdList(newCi);
            final Map<String, String> resourceNode =
                                  clStatus.getParamValuePairs(
                                          newCi.getHeartbeatId(testOnly));
            newCi.setParameters(resourceNode);
        } else {
            final Map<String, String> resourceNode =
                                  clStatus.getParamValuePairs(
                                          newCi.getHeartbeatId(testOnly));
            newCi.setParameters(resourceNode);
            if (!testOnly) {
                newCi.setUpdated(false);
                hg.repaint();
            }
        }
        hg.setVertexIsPresent(newCi);
        return newCi;
    }
    /**
     * Sets group info object.
     */
    private GroupInfo setCreateGroupInfo(final String group,
                                         final CloneInfo newCi,
                                         final boolean testOnly) {
        GroupInfo newGi = null;
        newGi = (GroupInfo) getBrowser().getServiceInfoFromCRMId(group);
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        if (newGi == null) {
            final Point2D p = null;
            newGi =
              (GroupInfo) addServicePanel(
                                     getBrowser().getCRMXML().getHbGroup(),
                                     p,
                                     false,
                                     group,
                                     newCi,
                                     testOnly);
            newGi.getService().setNew(false);
            final Map<String, String> resourceNode =
                                  clStatus.getParamValuePairs(
                                      newGi.getHeartbeatId(testOnly));
            newGi.setParameters(resourceNode);
            if (newCi != null) {
                newCi.addCloneServicePanel(newGi);
            }
        } else {
            final Map<String, String> resourceNode =
                                    clStatus.getParamValuePairs(
                                      newGi.getHeartbeatId(testOnly));
            newGi.setParameters(resourceNode);
            if (!testOnly) {
                newGi.setUpdated(false);
                hg.repaint();
            }
        }
        return newGi;
    }

    /**
     * Sets or create all resources.
     */
    private void setGroupResources(
                           final Set<String> allGroupsAndClones,
                           final String grpOrCloneId,
                           final GroupInfo newGi,
                           final CloneInfo newCi,
                           final List<ServiceInfo> groupServiceIsPresent,
                           final boolean testOnly) {
        final Map<ServiceInfo, Map<String, String>> setParametersHash =
                           new HashMap<ServiceInfo, Map<String, String>>();
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        if (newCi != null) {
            setParametersHash.put(
                            newCi,
                            clStatus.getParamValuePairs(grpOrCloneId));
        } else if (newGi != null) {
            setParametersHash.put(
                            newGi,
                            clStatus.getParamValuePairs(grpOrCloneId));
        }
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        boolean newService = false;
        for (String hbId : clStatus.getGroupResources(grpOrCloneId,
                                                           testOnly)) {
            if (allGroupsAndClones.contains(hbId)) {
                if (newGi != null) {
                    Tools.appWarning("group in group not implemented");
                    continue;
                }
                /* clone group */
                final GroupInfo gi = setCreateGroupInfo(hbId,
                                                        newCi,
                                                        testOnly);
                setGroupResources(allGroupsAndClones,
                                  hbId,
                                  gi,
                                  null,
                                  groupServiceIsPresent,
                                  testOnly);
                continue;
            }
            final ResourceAgent newRA = clStatus.getResourceType(hbId);
            if (newRA == null) {
                /* This is bad. There is a service but we do not have
                 * the heartbeat script of this service or the we look
                 * in the wrong places.
                 */
                Tools.appWarning(hbId + ": could not find resource agent");
            }
            /* continue of creating/updating of the
             * service in the gui.
             */
            ServiceInfo newSi = getBrowser().getServiceInfoFromCRMId(hbId);
            final Map<String, String> resourceNode =
                                       clStatus.getParamValuePairs(hbId);
            if (newSi == null) {
                newService = true;
                // TODO: get rid of the service name? (everywhere)
                String serviceName;
                if (newRA == null) {
                    serviceName = hbId;
                } else {
                    serviceName = newRA.getName();
                }
                if (newRA != null && newRA.isFilesystem()) {
                    newSi = new FilesystemInfo(serviceName,
                                               newRA,
                                               hbId,
                                               resourceNode,
                                               getBrowser());
                } else if (newRA != null && newRA.isLinbitDrbd()) {
                    newSi = new LinbitDrbdInfo(serviceName,
                                               newRA,
                                               hbId,
                                               resourceNode,
                                               getBrowser());
                } else if (newRA != null && newRA.isDrbddisk()) {
                    newSi = new DrbddiskInfo(serviceName,
                                             newRA,
                                             hbId,
                                             resourceNode,
                                             getBrowser());
                } else if (newRA != null && newRA.isIPaddr()) {
                    newSi = new IPaddrInfo(serviceName,
                                           newRA,
                                           hbId,
                                           resourceNode,
                                           getBrowser());
                } else if (newRA != null && newRA.isVirtualDomain()) {
                    newSi = new VirtualDomainInfo(serviceName,
                                                  newRA,
                                                  hbId,
                                                  resourceNode,
                                                  getBrowser());
                } else {
                    newSi = new ServiceInfo(serviceName,
                                            newRA,
                                            hbId,
                                            resourceNode,
                                            getBrowser());
                }
                newSi.getService().setHeartbeatId(hbId);
                getBrowser().addToHeartbeatIdList(newSi);
                final Point2D p = null;
                if (newGi != null) {
                    newGi.addGroupServicePanel(newSi, false);
                } else if (newCi != null) {
                    newCi.addCloneServicePanel(newSi);
                } else {
                    addServicePanel(newSi, p, false, false, testOnly);
                }
            } else {
                getBrowser().addNameToServiceInfoHash(newSi);
                setParametersHash.put(newSi, resourceNode);
            }
            newSi.getService().setNew(false);
            hg.setVertexIsPresent(newSi);
            if (newGi != null || newCi != null) {
                groupServiceIsPresent.add(newSi);
            }
        }

        for (final ServiceInfo newSi : setParametersHash.keySet()) {
            newSi.setParameters(setParametersHash.get(newSi));
            if (!testOnly) {
                newSi.setUpdated(false);
            }
        }
        if (newService) {
            getBrowser().reload(getBrowser().getServicesNode());
        }
        hg.repaint();
    }

    /**
     * This functions goes through all services, constrains etc. in
     * clusterStatus and updates the internal structures and graph.
     */
    public final void setAllResources(final boolean testOnly) {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        final Set<String> allGroupsAndClones = clStatus.getAllGroups();
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        hg.clearVertexIsPresentList();
        final List<ServiceInfo> groupServiceIsPresent =
                                                new ArrayList<ServiceInfo>();
        groupServiceIsPresent.clear();
        for (final String groupOrClone : allGroupsAndClones) {
            CloneInfo newCi = null;
            GroupInfo newGi = null;
            if (clStatus.isClone(groupOrClone)) {
                /* clone */
                newCi = setCreateCloneInfo(groupOrClone, testOnly);
            } else if (!"none".equals(groupOrClone)) {
                /* group */
                final GroupInfo gi =
                         (GroupInfo) getBrowser().getServiceInfoFromCRMId(
                                                                 groupOrClone);
                if (gi != null && gi.getCloneInfo() != null) {
                    /* cloned group is already done */
                    groupServiceIsPresent.add(gi);
                    continue;
                }
                newGi = setCreateGroupInfo(groupOrClone, newCi, testOnly);
                hg.setVertexIsPresent(newGi);
            }
            setGroupResources(allGroupsAndClones,
                              groupOrClone,
                              newGi,
                              newCi,
                              groupServiceIsPresent,
                              testOnly);
        }

        hg.clearColocationList();
        hg.clearOrderList();
        /* resource sets */
        final List<CRMXML.RscSetConnectionData> rscSetConnections =
                                               clStatus.getRscSetConnections();
        if (rscSetConnections != null) {
            final Map<CRMXML.RscSetConnectionData, ConstraintPHInfo>
             rdataToCphi =
             new LinkedHashMap<CRMXML.RscSetConnectionData, ConstraintPHInfo>();
            getBrowser().lockNameToServiceInfo();
            final Map<String, ServiceInfo> idToInfoHash =
                 getBrowser().getNameToServiceInfoHash(ConstraintPHInfo.NAME);
            final List<ConstraintPHInfo> preNewCphis =
                                            new ArrayList<ConstraintPHInfo>();
            if (idToInfoHash != null) {
                for (final String id : idToInfoHash.keySet()) {
                    final ConstraintPHInfo cphi =
                                       (ConstraintPHInfo) idToInfoHash.get(id);
                    final CRMXML.RscSetConnectionData rdataOrd =
                                            cphi.getRscSetConnectionDataOrd();
                    final CRMXML.RscSetConnectionData rdataCol =
                                            cphi.getRscSetConnectionDataCol();
                    if (cphi.getService().isNew()) {
                        preNewCphis.add(cphi);
                    }
                    if (rdataOrd != null && !rdataOrd.isEmpty()) {
                        rdataToCphi.put(rdataOrd, cphi);
                    }
                    if (rdataCol != null && !rdataCol.isEmpty()) {
                        rdataToCphi.put(rdataCol, cphi);
                    }
                }
            }
            getBrowser().unlockNameToServiceInfo();
            final List<ConstraintPHInfo> newCphis =
                                            new ArrayList<ConstraintPHInfo>();
            for (final CRMXML.RscSetConnectionData rdata : rscSetConnections) {
                ConstraintPHInfo cphi = null;
                PcmkRscSetsInfo prsi = null;

                for (final CRMXML.RscSetConnectionData ordata
                                                  : rdataToCphi.keySet()) {
                    if (ordata == rdata) {
                        continue;
                    }
                    if (rdata.equals(ordata)
                        || rdata.equalsReversed(ordata)) {
                        cphi = rdataToCphi.get(ordata);
                        cphi.setRscSetConnectionData(rdata);
                        break;
                    }
                }
                if (cphi == null) {
                    for (final CRMXML.RscSetConnectionData ordata
                                                  : rdataToCphi.keySet()) {
                        if (ordata == rdata) {
                            cphi = rdataToCphi.get(ordata);
                            break;
                        }
                        if (rdataToCphi.get(ordata).sameConstraintId(rdata)) {
                            /* use the same rsc set info object */
                            prsi = rdataToCphi.get(ordata).getPcmkRscSetsInfo();
                        }
                        if (rdataToCphi.get(ordata).getService().isNew()
                            || (rdata.samePlaceholder(ordata)
                                && rdataToCphi.get(ordata).sameConstraintId(
                                                                 rdata))) {
                            cphi = rdataToCphi.get(ordata);
                            cphi.setRscSetConnectionData(rdata);
                            prsi = cphi.getPcmkRscSetsInfo();
                            if (prsi != null) {
                                if (rdata.isColocation()) {
                                    prsi.addColocation(
                                                   rdata.getConstraintId(),
                                                   cphi);
                                } else {
                                    prsi.addOrder(rdata.getConstraintId(),
                                                  cphi);
                                }
                            }
                            break;
                        }
                    }
                }
                if (cphi == null && !preNewCphis.isEmpty()) {
                    /* placeholder */
                    cphi = preNewCphis.remove(0);
                    rdataToCphi.put(rdata, cphi);
                    cphi.setRscSetConnectionData(rdata);
                }
                if (cphi == null) {
                    cphi = new ConstraintPHInfo(getBrowser(), rdata);
                    if (prsi == null) {
                        prsi = new PcmkRscSetsInfo(getBrowser());
                    }
                    if (rdata.isColocation()) {
                        prsi.addColocation(rdata.getConstraintId(), cphi);
                    } else {
                        prsi.addOrder(rdata.getConstraintId(), cphi);
                    }
                    cphi.setPcmkRscSetsInfo(prsi);
                    getBrowser().addNameToServiceInfoHash(cphi);
                    newCphis.add(cphi); /* have to add it later,
                                           so that ids are correct. */
                    rdataToCphi.put(rdata, cphi);
                }
                hg.setVertexIsPresent(cphi);

                final CRMXML.RscSet rscSet1 = rdata.getRscSet1();
                final CRMXML.RscSet rscSet2 = rdata.getRscSet2();
                if (rdata.isColocation()) {
                    /* colocation */
                    if (rscSet1 != null) {
                        for (final String rscId : rscSet1.getRscIds()) {
                            final ServiceInfo si =
                               getBrowser().getServiceInfoFromCRMId(rscId);
                            hg.addColocation(rdata.getConstraintId(),
                                             cphi,
                                             si);
                        }
                    }
                    if (rscSet2 != null) {
                        for (final String rscId : rscSet2.getRscIds()) {
                            final ServiceInfo si =
                               getBrowser().getServiceInfoFromCRMId(rscId);
                            hg.addColocation(rdata.getConstraintId(),
                                             si,
                                             cphi);
                        }
                    }
                } else {
                    /* order */
                    if (rscSet1 != null) {
                        for (final String rscId : rscSet1.getRscIds()) {
                            final ServiceInfo si =
                               getBrowser().getServiceInfoFromCRMId(rscId);
                            hg.addOrder(rdata.getConstraintId(), si, cphi);
                        }
                    }
                    if (rscSet2 != null) {
                        for (final String rscId : rscSet2.getRscIds()) {
                            final ServiceInfo si =
                               getBrowser().getServiceInfoFromCRMId(rscId);
                            hg.addOrder(rdata.getConstraintId(), cphi, si);
                        }
                    }
                }
                if (!testOnly && cphi != null) {
                    cphi.setUpdated(false);
                    cphi.getService().setNew(false);
                }
            }

            for (final ConstraintPHInfo cphi : newCphis) {
                hg.addConstraintPlaceholder(cphi,
                                            null, /* pos */
                                            false);
            }
        }

        /* colocations */
        final Map<String, List<CRMXML.ColocationData>> colocationMap =
                                                 clStatus.getColocationRscMap();
        for (final String rscId : colocationMap.keySet()) {
            final List<CRMXML.ColocationData> withs =
                                                colocationMap.get(rscId);
            for (final CRMXML.ColocationData data : withs) {
                final String withRscId = data.getWithRsc();
                final ServiceInfo withSi = getBrowser().getServiceInfoFromCRMId(
                                                                   withRscId);
                final ServiceInfo siP = getBrowser().getServiceInfoFromCRMId(
                                                                        rscId);
                hg.addColocation(data.getId(), siP, withSi);
            }
        }

        /* orders */
        final Map<String, List<CRMXML.OrderData>> orderMap =
                                                    clStatus.getOrderRscMap();
        for (final String rscFirstId : orderMap.keySet()) {
            for (final CRMXML.OrderData data : orderMap.get(rscFirstId)) {
                final String rscThenId = data.getRscThen();
                final ServiceInfo si =
                        getBrowser().getServiceInfoFromCRMId(rscThenId);
                if (si != null) { /* not yet complete */
                    final ServiceInfo siP =
                            getBrowser().getServiceInfoFromCRMId(rscFirstId);
                    if (siP != null && siP.getResourceAgent() != null) {
                        /* dangling orders and colocations */
                        if ((siP.getResourceAgent().isDrbddisk()
                             || siP.getResourceAgent().isLinbitDrbd())
                            && si.getName().equals("Filesystem")) {
                            final List<CRMXML.ColocationData> cds =
                                     clStatus.getColocationDatas(rscFirstId);
                            if (cds != null) {
                                for (final CRMXML.ColocationData cd : cds) {
                                    if (cd.getWithRsc().equals(rscThenId)) {
                                        setFilesystemWithDrbd(siP, si);
                                    }
                                }
                            }
                        }
                        hg.addOrder(data.getId(), siP, si);
                    }
                }
            }
        }

        final Enumeration e = getNode().children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode n =
                      (DefaultMutableTreeNode) e.nextElement();
            final ServiceInfo g = (ServiceInfo) n.getUserObject();
            if (g.getResourceAgent().isGroup()
                || g.getResourceAgent().isClone()) {
                final Enumeration ge = g.getNode().children();
                while (ge.hasMoreElements()) {
                    final DefaultMutableTreeNode gn =
                              (DefaultMutableTreeNode) ge.nextElement();
                    final ServiceInfo s = (ServiceInfo) gn.getUserObject();
                    if (!groupServiceIsPresent.contains(s)
                        && !s.getService().isNew()) {
                        /* remove the group service from the menu that does
                         * not exist anymore. */
                        s.removeInfo();
                    }
                }
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                hg.killRemovedEdges();
                /** Set placeholders to "new", if they have no connections. */
                final Map<String, ServiceInfo> idToInfoHash =
                   getBrowser().getNameToServiceInfoHash(ConstraintPHInfo.NAME);
                if (idToInfoHash != null) {
                    for (final String id : idToInfoHash.keySet()) {
                        final ConstraintPHInfo cphi =
                                       (ConstraintPHInfo) idToInfoHash.get(id);
                        if (!cphi.getService().isNew() && cphi.isEmpty()) {
                            cphi.getService().setNew(true);
                        }
                    }
                }
                hg.killRemovedVertices();
                hg.scale();
            }
        });
    }

    /**
     * Clears the info panel cache, forcing it to reload.
     */
    public final boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /**
     * Returns type of the info text. text/plain or text/html.
     */
    protected final String getInfoType() {
        return Tools.MIME_TYPE_TEXT_HTML;
    }

    /**
     * Returns info for info panel, that hb status failed or null, in which
     * case the getInfoPanel() function will show.
     */
    public final String getInfo() {
        if (getBrowser().clStatusFailed()) {
            return Tools.getString("ClusterBrowser.ClStatusFailed");
        }
        return null;
    }

    /** Creates rsc_defaults panel. */
    private void addRscDefaultsPanel(final JPanel optionsPanel,
                                     final int leftWidth,
                                     final int rightWidth) {
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        rdi.paramComboBoxClear();
        final String[] params = rdi.getParametersFromXML();
        rdi.addParams(optionsPanel,
                      params,
                      leftWidth,
                      rightWidth,
                      null);
    }

    /** Returns editable info panel for global crm config. */
    public final JComponent getInfoPanel() {
        /* if don't have hb status we don't have all the info we need here.
         * TODO: OR we need to get hb status only once
         */
        if (getBrowser().clStatusFailed()) {
            return super.getInfoPanel();
        }
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        if (infoPanel != null) {
            hg.pickBackground();
            return infoPanel;
        }
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        if (getBrowser().getCRMXML() == null) {
            return newPanel;
        }
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;

            /**
             * Whether the whole thing should be enabled.
             */
            public final boolean isEnabled() {
                final Host dcHost = getBrowser().getDCHost();
                if (dcHost == null) {
                    return false;
                }
                final String pmV = dcHost.getPacemakerVersion();
                final String hbV = dcHost.getHeartbeatVersion();
                if (pmV == null
                    && hbV != null
                    && Tools.compareVersions(hbV, "2.1.4") <= 0) {
                    return false;
                }
                return true;
            }

            public final void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                hg.stopTestAnimation(getApplyButton());
                getApplyButton().setToolTipText(null);
            }

            public final void mouseOver() {
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
                hg.startTestAnimation(getApplyButton(), startTestLatch);
                final Host dcHost = getBrowser().getDCHost();
                getBrowser().ptestLockAcquire();
                final ClusterStatus clStatus = getBrowser().getClusterStatus();
                clStatus.setPtestData(null);
                apply(dcHost, true);
                final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
                getApplyButton().setToolTipText(ptestData.getToolTip());
                clStatus.setPtestData(ptestData);
                getBrowser().ptestLockRelease();
                startTestLatch.countDown();
            }
        };
        initApplyButton(buttonCallback);
        getBrowser().getRscDefaultsInfo().setApplyButton(getApplyButton());
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        final JMenu serviceCombo = getActionsMenu();
        mb.add(serviceCombo);
        buttonPanel.add(mb, BorderLayout.EAST);

        newPanel.add(buttonPanel);

        final String[] params = getParametersFromXML();
        addParams(optionsPanel,
                  params,
                  Tools.getDefaultInt("ClusterBrowser.DrbdResLabelWidth"),
                  Tools.getDefaultInt("ClusterBrowser.DrbdResFieldWidth"),
                  null);

        addRscDefaultsPanel(
                      optionsPanel,
                      Tools.getDefaultInt("ClusterBrowser.DrbdResLabelWidth"),
                      Tools.getDefaultInt("ClusterBrowser.DrbdResFieldWidth"));
        getApplyButton().addActionListener(
            new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(
                        new Runnable() {
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(getBrowser().getDCHost(), false);
                                getBrowser().clStatusUnlock();
                            }
                        }
                    );
                    thread.start();
                }
            }
        );

        /* apply button */
        addApplyButton(buttonPanel);
        getApplyButton().setEnabled(checkResourceFields(null, params));

        mainPanel.add(optionsPanel);

        newPanel.add(getMoreOptionsPanel(
                                  ClusterBrowser.SERVICE_LABEL_WIDTH
                                  + ClusterBrowser.SERVICE_FIELD_WIDTH + 4));
        newPanel.add(new JScrollPane(mainPanel));

        hg.pickBackground();
        infoPanel = newPanel;
        return infoPanel;
    }

    /** Returns heartbeat graph. */
    public final JPanel getGraphicalView() {
        return getBrowser().getHeartbeatGraph().getGraphPanel();
    }

    /**
     * Adds service to the list of services.
     * TODO: are they both used?
     */
    public final ServiceInfo addServicePanel(final ResourceAgent newRA,
                                             final Point2D pos,
                                             final boolean reloadNode,
                                             final String heartbeatId,
                                             final CloneInfo newCi,
                                             final boolean testOnly) {
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
            newServiceInfo = new GroupInfo(newRA, getBrowser());
        } else if (newRA.isClone()) {
            final boolean master =
                         getBrowser().getClusterStatus().isMaster(heartbeatId);
            String cloneName;
            if (master) {
                cloneName = ConfigData.PM_MASTER_SLAVE_SET_NAME;
            } else {
                cloneName = ConfigData.PM_CLONE_SET_NAME;
            }
            newServiceInfo = new CloneInfo(newRA,
                                           cloneName,
                                           master,
                                           getBrowser());
        } else {
            newServiceInfo = new ServiceInfo(name, newRA, getBrowser());
        }
        if (heartbeatId != null) {
            newServiceInfo.getService().setHeartbeatId(heartbeatId);
            getBrowser().addToHeartbeatIdList(newServiceInfo);
        }
        if (newCi == null) {
            addServicePanel(newServiceInfo,
                            pos,
                            reloadNode,
                            true,
                            testOnly);
        }
        return newServiceInfo;
    }

    /**
     * Adds new service to the specified position. If position is null, it
     * will be computed later. reloadNode specifies if the node in
     * the menu should be reloaded and get uptodate.
     */
    public final void addServicePanel(final ServiceInfo newServiceInfo,
                                      final Point2D pos,
                                      final boolean reloadNode,
                                      final boolean interactive,
                                      final boolean testOnly) {
        newServiceInfo.getService().setResourceClass(
                    newServiceInfo.getResourceAgent().getResourceClass());
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        if (!hg.addResource(newServiceInfo,
                            null,
                            pos,
                            false, /* colocation only */
                            false, /* order only */
                            testOnly)) {
            getBrowser().addNameToServiceInfoHash(newServiceInfo);
            final DefaultMutableTreeNode newServiceNode =
                                new DefaultMutableTreeNode(newServiceInfo);
            newServiceInfo.setNode(newServiceNode);
            getBrowser().getServicesNode().add(newServiceNode);
            if (interactive
                && newServiceInfo.getResourceAgent().isProbablyMasterSlave()) {
                /* only if it was added manually. */
                newServiceInfo.changeType(ServiceInfo.MASTER_SLAVE_TYPE_STRING);
            }
            if (reloadNode) {
                /* show it */
                getBrowser().reload(getBrowser().getServicesNode());
                getBrowser().reload(newServiceNode);
            }
            getBrowser().reloadAllComboBoxes(newServiceInfo);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    hg.scale();
                }
            });
        }
        hg.reloadServiceMenus();
    }

    /** Returns 'add service' list for graph popup menu. */
    public final List<ResourceAgent> getAddServiceList(final String cl) {
        return getBrowser().globalGetAddServiceList(cl);
    }

    /**
     * Returns background popup. Click on background represents cluster as
     * whole.
     */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final boolean testOnly = false;
        /* add group */
        final MyMenuItem addGroupMenuItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.AddGroup"),
                           null,
                           null,
                           new AccessMode(ConfigData.AccessType.ADMIN,
                                          false),
                           new AccessMode(ConfigData.AccessType.OP,
                                          false)) {
                private static final long serialVersionUID = 1L;

                public final String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    return null;
                }

                public final void action() {
                    hidePopup();
                    addServicePanel(getBrowser().getCRMXML().getHbGroup(),
                                    getPos(),
                                    true,
                                    null,
                                    null,
                                    testOnly);
                    getBrowser().getHeartbeatGraph().repaint();
                }
            };
        items.add((UpdatableItem) addGroupMenuItem);

        /* add service */
        final MyMenu addServiceMenuItem = new MyMenu(
                        Tools.getString("ClusterBrowser.Hb.AddService"),
                        new AccessMode(ConfigData.AccessType.OP,
                                       false),
                        new AccessMode(ConfigData.AccessType.OP,
                                       false)) {
            private static final long serialVersionUID = 1L;
            private final Mutex mUpdateLock = new Mutex();

            public final String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                return null;
            }

            public final void update() {
                final Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            if (mUpdateLock.attempt(0)) {
                                updateThread();
                                mUpdateLock.release();
                            }
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
                t.start();
            }
            private final void updateThread() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setEnabled(false);
                    }
                });
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        removeAll();
                    }
                });
                Point2D pos = getPos();
                final CRMXML crmXML = getBrowser().getCRMXML();
                final ResourceAgent fsService = crmXML.getResourceAgent(
                                        "Filesystem",
                                        ServiceInfo.HB_HEARTBEAT_PROVIDER,
                                        "ocf");
                if (crmXML.isLinbitDrbdPresent()) { /* just skip it,
                                                       if it is not */
                    final ResourceAgent linbitDrbdService =
                                                   crmXML.getHbLinbitDrbd();
                    final MyMenuItem ldMenuItem = new MyMenuItem(
                     Tools.getString("ClusterBrowser.linbitDrbdMenuName"),
                     null,
                     null,
                     new AccessMode(ConfigData.AccessType.ADMIN,
                                    false),
                     new AccessMode(ConfigData.AccessType.OP,
                                    false)) {
                        private static final long serialVersionUID = 1L;
                        public void action() {
                            hidePopup();
                            if (!getBrowser().linbitDrbdConfirmDialog()) {
                                return;
                            }

                            final FilesystemInfo fsi = (FilesystemInfo)
                                                           addServicePanel(
                                                                fsService,
                                                                getPos(),
                                                                true,
                                                                null,
                                                                null,
                                                                testOnly);
                            fsi.setDrbddiskIsPreferred(false);
                            getBrowser().getHeartbeatGraph().repaint();
                        }
                    };
                    if (getBrowser().atLeastOneDrbddisk()
                        || !crmXML.isLinbitDrbdPresent()) {
                        ldMenuItem.setEnabled(false);
                    }
                    ldMenuItem.setPos(pos);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            add(ldMenuItem);
                        }
                    });
                }
                final ResourceAgent ipService = crmXML.getResourceAgent(
                                         "IPaddr2",
                                         ServiceInfo.HB_HEARTBEAT_PROVIDER,
                                         "ocf");
                if (ipService != null) { /* just skip it, if it is not*/
                    final MyMenuItem ipMenuItem =
                         new MyMenuItem(ipService.getMenuName(),
                                        null,
                                        null,
                                        new AccessMode(
                                                  ConfigData.AccessType.ADMIN,
                                                  false),
                                        new AccessMode(ConfigData.AccessType.OP,
                                                       false)) {
                        private static final long serialVersionUID = 1L;
                        public void action() {
                            hidePopup();
                            addServicePanel(ipService,
                                            getPos(),
                                            true,
                                            null,
                                            null,
                                            testOnly);
                            getBrowser().getHeartbeatGraph().repaint();
                        }
                    };
                    ipMenuItem.setPos(pos);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            add(ipMenuItem);
                        }
                    });
                }
                if (crmXML.isDrbddiskPresent()
                    && (getBrowser().isDrbddiskPreferred()
                        || getBrowser().atLeastOneDrbddisk()
                        || !crmXML.isLinbitDrbdPresent())) {
                    final ResourceAgent drbddiskService =
                                                crmXML.getHbDrbddisk();
                    final MyMenuItem ddMenuItem = new MyMenuItem(
                     Tools.getString("ClusterBrowser.DrbddiskMenuName"),
                     null,
                     null,
                     new AccessMode(ConfigData.AccessType.ADMIN,
                                    false),
                     new AccessMode(ConfigData.AccessType.OP,
                                    false)) {
                        private static final long serialVersionUID = 1L;
                        public void action() {
                            hidePopup();
                            final FilesystemInfo fsi = (FilesystemInfo)
                                                           addServicePanel(
                                                                fsService,
                                                                getPos(),
                                                                true,
                                                                null,
                                                                null,
                                                                testOnly);
                            fsi.setDrbddiskIsPreferred(true);
                            getBrowser().getHeartbeatGraph().repaint();
                        }
                    };
                    if (getBrowser().isOneLinbitDrbd()
                        || !crmXML.isDrbddiskPresent()) {
                        ddMenuItem.setEnabled(false);
                    }
                    ddMenuItem.setPos(pos);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            add(ddMenuItem);
                        }
                    });
                }
                for (final String cl : ClusterBrowser.HB_CLASSES) {
                    final MyMenu classItem =
                            new MyMenu(ClusterBrowser.HB_CLASS_MENU.get(cl),
                                       new AccessMode(
                                                   ConfigData.AccessType.ADMIN,
                                                   false),
                                       new AccessMode(ConfigData.AccessType.OP,
                                                      false));
                    DefaultListModel dlm = new DefaultListModel();
                    for (final ResourceAgent ra : getAddServiceList(cl)) {
                        final MyMenuItem mmi =
                                new MyMenuItem(ra.getMenuName(),
                                               null,
                                               null,
                                               new AccessMode(
                                                    ConfigData.AccessType.ADMIN,
                                                    false),
                                               new AccessMode(
                                                    ConfigData.AccessType.OP,
                                                    false)) {
                            private static final long serialVersionUID = 1L;
                            public void action() {
                                hidePopup();
                                if (ra.isLinbitDrbd()
                                    &&
                                     !getBrowser().linbitDrbdConfirmDialog()) {
                                    return;
                                } else if (ra.isHbDrbd()
                                    &&
                                     !getBrowser().hbDrbdConfirmDialog()) {
                                    return;
                                }
                                addServicePanel(ra,
                                                getPos(),
                                                true,
                                                null,
                                                null,
                                                testOnly);
                                getBrowser().getHeartbeatGraph().repaint();
                            }
                        };
                        mmi.setPos(pos);
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
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            add(classItem);
                        }
                    });
                }
                super.update();
            }
        };
        items.add((UpdatableItem) addServiceMenuItem);
        /* add constraint placeholder */
        final MyMenuItem addConstraintPlaceholder =
            new MyMenuItem(Tools.getString(
                                "ServicesInfo.AddConstraintPlaceholder"),
                           null,
                           Tools.getString(
                             "ServicesInfo.AddConstraintPlaceholder.ToolTip"),
                           new AccessMode(ConfigData.AccessType.ADMIN,
                                          false),
                           new AccessMode(ConfigData.AccessType.OP,
                                          false)) {
                private static final long serialVersionUID = 1L;

                public final String enablePredicate() {
                    if (getBrowser().clStatusFailed()) {
                        return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                    }
                    return null;
                }

                public final void action() {
                    hidePopup();
                    final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
                    final ConstraintPHInfo cphi =
                                      new ConstraintPHInfo(getBrowser(), null);
                    cphi.getService().setNew(true);
                    getBrowser().addNameToServiceInfoHash(cphi);
                    hg.addConstraintPlaceholder(cphi,
                                                getPos(),
                                                testOnly);
                    final PcmkRscSetsInfo prsi =
                                      new PcmkRscSetsInfo(getBrowser(), cphi);
                    cphi.setPcmkRscSetsInfo(prsi);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            hg.scale();
                        }
                    });
                }
            };
        items.add((UpdatableItem) addConstraintPlaceholder);

        /* stop all services. */
        final MyMenuItem stopAllMenuItem = new MyMenuItem(
                Tools.getString("ClusterBrowser.Hb.StopAllServices"),
                ServiceInfo.STOP_ICON,
                new AccessMode(ConfigData.AccessType.ADMIN, true),
                new AccessMode(ConfigData.AccessType.ADMIN, false)) {
            private static final long serialVersionUID = 1L;

            public final String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                if (getBrowser().getExistingServiceList(null).isEmpty()) {
                    return "there are no services";
                }
                for (ServiceInfo si
                        : getBrowser().getExistingServiceList(null)) {
                    if (!si.isStopped(false) && !si.getService().isOrphaned()) {
                        return null;
                    }
                }
                return "all services are stopped";
            }

            public final void action() {
                hidePopup();
                final Host dcHost = getBrowser().getDCHost();
                for (final ServiceInfo si
                        : getBrowser().getExistingServiceList(null)) {
                    if (si.getGroupInfo() == null) {
                        if (!si.isStopped(false)
                            && !si.getService().isOrphaned()) {
                            si.stopResource(dcHost, false);
                        }
                    }
                }
                getBrowser().getHeartbeatGraph().repaint();
            }
        };
        final ClusterBrowser.ClMenuItemCallback stopAllItemCallback =
                   getBrowser().new ClMenuItemCallback(stopAllMenuItem, null) {
            public void action(final Host dcHost) {
                final Host thisDCHost = getBrowser().getDCHost();
                for (final ServiceInfo si
                        : getBrowser().getExistingServiceList(null)) {
                    if (si.getGroupInfo() == null
                        && !si.isConstraintPH()) {
                        if (!si.isStopped(true)
                            && !si.getService().isOrphaned()) {
                            si.stopResource(thisDCHost, true); /* test only */
                        }
                    }
                }
            }
        };
        addMouseOverListener(stopAllMenuItem, stopAllItemCallback);
        items.add((UpdatableItem) stopAllMenuItem);

        /* unmigrate all services. */
        final MyMenuItem unmigrateAllMenuItem = new MyMenuItem(
                Tools.getString("ClusterBrowser.Hb.UnmigrateAllServices"),
                ServiceInfo.UNMIGRATE_ICON,
                new AccessMode(ConfigData.AccessType.OP, false),
                new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            public boolean visiblePredicate() {
                return enablePredicate() == null;
            }

            public final String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                if (getBrowser().getExistingServiceList(null).isEmpty()) {
                    return "there are no services";
                }
                for (ServiceInfo si
                                 : getBrowser().getExistingServiceList(null)) {
                    if (si.getMigratedTo(testOnly) != null
                        || si.getMigratedFrom(testOnly) != null) {
                        return null;
                    }
                }
                return "nothing to unmigrate";
            }

            public final void action() {
                hidePopup();
                final Host dcHost = getBrowser().getDCHost();
                for (final ServiceInfo si
                                : getBrowser().getExistingServiceList(null)) {
                    if (si.getMigratedTo(testOnly) != null
                        || si.getMigratedFrom(testOnly) != null) {
                        si.unmigrateResource(dcHost, false);
                    }
                }
                getBrowser().getHeartbeatGraph().repaint();
            }
        };
        final ClusterBrowser.ClMenuItemCallback unmigrateAllItemCallback =
               getBrowser().new ClMenuItemCallback(unmigrateAllMenuItem, null) {
            public void action(final Host dcHost) {
                final Host thisDCHost = getBrowser().getDCHost();
                for (final ServiceInfo si
                                : getBrowser().getExistingServiceList(null)) {
                    if (si.getMigratedTo(testOnly) != null
                        || si.getMigratedFrom(testOnly) != null) {
                        si.unmigrateResource(dcHost, true); /* test only */
                    }
                }
            }
        };
        addMouseOverListener(unmigrateAllMenuItem, unmigrateAllItemCallback);
        items.add((UpdatableItem) unmigrateAllMenuItem);

        /* remove all services. */
        final MyMenuItem removeMenuItem = new MyMenuItem(
                Tools.getString("ClusterBrowser.Hb.RemoveAllServices"),
                ClusterBrowser.REMOVE_ICON,
                new AccessMode(ConfigData.AccessType.ADMIN, true),
                new AccessMode(ConfigData.AccessType.ADMIN, true)) {
            private static final long serialVersionUID = 1L;

            public final String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                if (getBrowser().getExistingServiceList(null).isEmpty()) {
                    return "there are no services";
                }
                return null;
            }

            public final void action() {
                hidePopup();
                if (Tools.confirmDialog(
                     Tools.getString(
                         "ClusterBrowser.confirmRemoveAllServices.Title"),
                     Tools.getString(
                     "ClusterBrowser.confirmRemoveAllServices.Description"),
                     Tools.getString(
                         "ClusterBrowser.confirmRemoveAllServices.Yes"),
                     Tools.getString(
                         "ClusterBrowser.confirmRemoveAllServices.No"))) {
                    final Host dcHost = getBrowser().getDCHost();
                    for (ServiceInfo si
                            : getBrowser().getExistingServiceList(null)) {
                        if (si.getGroupInfo() == null) {
                            if (si.getService().isOrphaned()) {
                                si.cleanupResource(dcHost, false);
                            } else if (!si.isRunning(false)) {
                                si.removeMyselfNoConfirm(dcHost, false);
                            }
                        }
                    }
                    getBrowser().getHeartbeatGraph().repaint();
                }
            }
        };
        items.add((UpdatableItem) removeMenuItem);


        /* view logs */
        final MyMenuItem viewLogsItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.ViewLogs"),
                           LOGFILE_ICON,
                           null,
                           new AccessMode(ConfigData.AccessType.RO,
                                          false),
                           new AccessMode(ConfigData.AccessType.RO,
                                          false)) {
                private static final long serialVersionUID = 1L;

                public final String enablePredicate() {
                    return null;
                }

                public final void action() {
                    ClusterLogs l = new ClusterLogs(getBrowser().getCluster());
                    l.showDialog();
                }
            };
        items.add((UpdatableItem) viewLogsItem);
        return items;
    }

    /**
     * Returns units.
     */
    protected final Unit[] getUnits() {
        return new Unit[]{
            new Unit("", "s", "Second", "Seconds"), /* default unit */
            new Unit("ms",  "ms", "Millisecond", "Milliseconds"),
            new Unit("us",  "us", "Microsecond", "Microseconds"),
            new Unit("s",   "s",  "Second",      "Seconds"),
            new Unit("min", "m",  "Minute",      "Minutes"),
            new Unit("h",   "h",  "Hour",        "Hours")
        };
    }

    /**
     * Removes this services info.
     * TODO: is not called yet
     */
    public final void removeMyself(final boolean testOnly) {
        super.removeMyself(testOnly);
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    public final boolean checkResourceFieldsCorrect(final String param,
                                                    final String[] params) {
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        boolean ret = true;
        if (!rdi.checkResourceFieldsCorrect(param,
                                            rdi.getParametersFromXML())) {
            ret = false;
        }
        if (!super.checkResourceFieldsCorrect(param, params)) {
            ret = false;
        }
        return ret;
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    public final boolean checkResourceFieldsChanged(final String param,
                                                    final String[] params) {
        boolean changed = false;
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        if (super.checkResourceFieldsChanged(param, params)) {
            changed = true;
        }
        if (rdi.checkResourceFieldsChanged(param,
                                           rdi.getParametersFromXML())) {
            changed = true;
        }
        return changed;
    }
}
