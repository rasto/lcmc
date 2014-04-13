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

import lcmc.data.*;
import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.resources.vms.VirtualDomainInfo;
import lcmc.gui.widget.Widget;
import lcmc.gui.CRMGraph;
import lcmc.gui.dialog.ClusterLogs;
import lcmc.data.resources.Resource;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.CRM;
import lcmc.utilities.Tools;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.MyList;
import lcmc.utilities.MyListModel;
import lcmc.Exceptions;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JDialog;
import javax.swing.tree.MutableTreeNode;

import lcmc.EditClusterDialog;
import lcmc.gui.widget.Check;
import lcmc.utilities.ComponentWithTest;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class holds info data for services view and global heartbeat
 * config.
 */
public final class ServicesInfo extends EditableInfo {
    /** Logger. */
    private static final Logger LOG =
                                 LoggerFactory.getLogger(ServicesInfo.class);
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Icon of the cluster. */
    static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(
                                Tools.getDefault("ClustersPanel.ClusterIcon"));

    /** Prepares a new {@code ServicesInfo} object. */
    public ServicesInfo(final String name, final Browser browser) {
        super(name, browser);
        setResource(new Resource(name));
    }

    /** Returns browser object of this info. */
    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** Sets info panel. */
    void setInfoPanel(final JComponent infoPanel) {
        this.infoPanel = infoPanel;
    }

    /** Returns names of all global parameters. */
    @Override
    public String[] getParametersFromXML() {
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
    @Override
    protected String getParamLongDesc(final String param) {
        return getBrowser().getCRMXML().getGlobalParamLongDesc(param);
    }

    /**
     * Returns short description of the global parameter, that is used as
     * label.
     */
    @Override
    protected String getParamShortDesc(final String param) {
        return getBrowser().getCRMXML().getGlobalParamShortDesc(param);
    }

    /** Returns default for this global parameter. */
    @Override
    protected Value getParamDefault(final String param) {
        return getBrowser().getCRMXML().getGlobalParamDefault(param);
    }

    /** Returns preferred value for this global parameter. */
    @Override
    protected Value getParamPreferred(final String param) {
        return getBrowser().getCRMXML().getGlobalParamPreferred(param);
    }

    /** Returns possible choices for pulldown menus if applicable. */
    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        return getBrowser().getCRMXML().getGlobalParamPossibleChoices(param);
    }

    /**
     * Checks if the new value is correct for the parameter type and
     * constraints.
     */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        return getBrowser().getCRMXML().checkGlobalParam(param, newValue);
    }

    /** Returns whether the global parameter is of the integer type. */
    @Override
    protected boolean isInteger(final String param) {
        return getBrowser().getCRMXML().isGlobalInteger(param);
    }
    /** Returns whether the global parameter is of the label type. */
    @Override
    protected boolean isLabel(final String param) {
        return getBrowser().getCRMXML().isGlobalLabel(param);
    }

    /** Returns whether the global parameter is of the time type. */
    @Override
    protected boolean isTimeType(final String param) {
        return getBrowser().getCRMXML().isGlobalTimeType(param);
    }

    /** Returns whether this parameter is advanced. */
    @Override
    protected boolean isAdvanced(final String param) {
        return Tools.areEqual(getParamDefault(param), getParamSaved(param))
               && getBrowser().getCRMXML().isGlobalAdvanced(param);
    }

    /** Returns access type of this parameter. */
    @Override
    protected Application.AccessType getAccessType(final String param) {
        return getBrowser().getCRMXML().getGlobalAccessType(param);
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override
    protected boolean isEnabledOnlyInAdvancedMode(final String param) {
        return false;
    }

    /** Returns whether the global parameter is required. */
    @Override
    protected boolean isRequired(final String param) {
        return getBrowser().getCRMXML().isGlobalRequired(param);
    }

    /**
     * Returns whether the global parameter is of boolean type and
     * requires a checkbox.
     */
    @Override
    protected boolean isCheckBox(final String param) {
        return getBrowser().getCRMXML().isGlobalBoolean(param);
    }

    /** Returns type of the global parameter. */
    @Override
    protected String getParamType(final String param) {
        return getBrowser().getCRMXML().getGlobalParamType(param);
    }

    /** Returns section to which the global parameter belongs. */
    @Override
    protected String getSection(final String param) {
        return getBrowser().getCRMXML().getGlobalSection(param);
    }

    /** Applies changes that user has entered. */
    void apply(final Host dcHost, final Application.RunMode runMode) {
        LOG.debug1("apply: start: test: " + runMode);
        final String[] params = getParametersFromXML();
        if (Application.isLive(runMode)) {
            Tools.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                    getApplyButton().setToolTipText("");
                }
            });
        }
        getInfoPanel();
        waitForInfoPanel();

        /* update pacemaker */
        final Map<String, String> args = new HashMap<String, String>();
        for (final String param : params) {
            final Value value = getComboBoxValue(param);
            if (Tools.areEqual(value, getParamDefault(param))) {
                continue;
            }

            if (value == null || value.isNothingSelected()) {
                continue;
            }
            args.put(param, value.getValueForConfig());
        }
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        final String[] rdiParams = rdi.getParametersFromXML();
        final Map<String, String> rdiMetaArgs =
                                           new LinkedHashMap<String, String>();
        for (final String param : rdiParams) {
            final Value value = rdi.getComboBoxValue(param);
            if (Tools.areEqual(value, rdi.getParamDefault(param))) {
                continue;
            }
            if (value != null && !value.isNothingSelected()) {
                rdiMetaArgs.put(param, value.getValueForConfig());
            }
        }
        final String rscDefaultsId =
                    getBrowser().getClusterStatus().getRscDefaultsId(runMode);
        CRM.setGlobalParameters(dcHost,
                                args,
                                rdiMetaArgs,
                                rscDefaultsId,
                                runMode);
        if (Application.isLive(runMode)) {
            storeComboBoxValues(params);
            rdi.storeComboBoxValues(rdiParams);
        }
        for (final ServiceInfo si : getBrowser().getExistingServiceList(null)) {
            final Check check = si.checkResourceFields(
                                                      null,
                                                      si.getParametersFromXML(),
                                                      true,
                                                      false,
                                                      false);
            if (check.isCorrect() && check.isChanged()) {
                si.apply(dcHost, runMode);
            }
        }
        if (Application.isLive(runMode)) {
            setApplyButtons(null, params);
        }
        LOG.debug1("apply: end: test: " + runMode);
    }

    /** Sets heartbeat global parameters after they were obtained. */
    public void setGlobalConfig(final ClusterStatus clStatus) {
        final String[] params = getParametersFromXML();
        for (final String param : params) {
            final String valueS = clStatus.getGlobalParam(param);
            if (valueS == null) {
                continue;
            }
            final Value value = new StringValue(valueS);
            final Value oldValue = getParamSaved(param);
            if (!Tools.areEqual(value, oldValue)) {
                getResource().setValue(param, value);
                final Widget wi = getWidget(param, null);
                if (wi != null) {
                    wi.setValue(value);
                }
            }
        }
        if (infoPanel == null) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getInfoPanel();
                }
            });
        }
    }

    /**
     * Check if this connection is filesystem with drbd ra and if so, set it.
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

    /** Sets clone info object. */
    private CloneInfo setCreateCloneInfo(final String cloneId,
                                         final ClusterStatus clStatus,
                                         final Application.RunMode runMode) {
        CloneInfo newCi = (CloneInfo) getBrowser().getServiceInfoFromCRMId(cloneId);
        final CRMGraph hg = getBrowser().getCRMGraph();
        if (newCi == null) {
            final Point2D p = null;
            newCi =
               (CloneInfo) addServicePanel(
                                        getBrowser().getCRMXML().getHbClone(),
                                        p,
                                        false,
                                        cloneId,
                                        null,
                                        runMode);
            getBrowser().addToHeartbeatIdList(newCi);
            final Map<String, String> resourceNode =
                                  clStatus.getParamValuePairs(
                                          newCi.getHeartbeatId(runMode));
            newCi.setParameters(resourceNode);
        } else {
            final Map<String, String> resourceNode =
                                  clStatus.getParamValuePairs(
                                          newCi.getHeartbeatId(runMode));
            newCi.setParameters(resourceNode);
            if (Application.isLive(runMode)) {
                newCi.setUpdated(false);
                hg.repaint();
            }
        }
        newCi.getService().setNew(false);
        return newCi;
    }

    /** Sets group info object. */
    private GroupInfo setCreateGroupInfo(final String group,
                                         final CloneInfo newCi,
                                         final ClusterStatus clStatus,
                                         final Application.RunMode runMode) {
        GroupInfo newGi = (GroupInfo) getBrowser().getServiceInfoFromCRMId(group);
        final CRMGraph hg = getBrowser().getCRMGraph();
        if (newGi == null) {
            final Point2D p = null;
            newGi =
              (GroupInfo) addServicePanel(
                                     getBrowser().getCRMXML().getHbGroup(),
                                     p,
                                     false,
                                     group,
                                     newCi,
                                     runMode);
            final Map<String, String> resourceNode =
                                  clStatus.getParamValuePairs(
                                      newGi.getHeartbeatId(runMode));
            newGi.setParameters(resourceNode);
            if (newCi != null) {
                newCi.addCloneServicePanel(newGi);
            }
        } else {
            final Map<String, String> resourceNode =
                                    clStatus.getParamValuePairs(
                                      newGi.getHeartbeatId(runMode));
            newGi.setParameters(resourceNode);
            if (Application.isLive(runMode)) {
                newGi.setUpdated(false);
                hg.repaint();
            }
        }
        newGi.getService().setNew(false);
        return newGi;
    }

    /** Sets or create all resources. */
    private void setGroupResources(
                               final Set<String> allGroupsAndClones,
                               final String grpOrCloneId,
                               final GroupInfo newGi,
                               final CloneInfo newCi,
                               final List<ServiceInfo> serviceIsPresent,
                               final List<ServiceInfo> groupServiceIsPresent,
                               final ClusterStatus clStatus,
                               final Application.RunMode runMode) {
        final Map<ServiceInfo, Map<String, String>> setParametersHash =
                           new HashMap<ServiceInfo, Map<String, String>>();
        if (newCi != null) {
            setParametersHash.put(
                            newCi,
                            clStatus.getParamValuePairs(grpOrCloneId));
        } else if (newGi != null) {
            setParametersHash.put(
                            newGi,
                            clStatus.getParamValuePairs(grpOrCloneId));
        }
        final CRMGraph hg = getBrowser().getCRMGraph();
        final List<String> gs = clStatus.getGroupResources(grpOrCloneId,
                                                           runMode);
        if (gs == null) {
            return;
        }
        boolean newService = false;
        int pos = 0;
        for (final String hbId : gs) {
            if (clStatus.isOrphaned(hbId) && Tools.getApplication().isNoLRM()) {
                continue;
            }
            ServiceInfo newSi;
            if (allGroupsAndClones.contains(hbId)) {
                if (newGi != null) {
                    LOG.appWarning("setGroupResources: group in group not implemented");
                    continue;
                }
                /* clone group */
                final GroupInfo gi = setCreateGroupInfo(hbId,
                                                        newCi,
                                                        clStatus,
                                                        runMode);
                setGroupResources(allGroupsAndClones,
                                  hbId,
                                  gi,
                                  null,
                                  serviceIsPresent,
                                  groupServiceIsPresent,
                                  clStatus,
                                  runMode);
                newSi = gi;
            } else {
                final ResourceAgent newRA = clStatus.getResourceType(hbId);
                if (newRA == null) {
                    /* This is bad. There is a service but we do not have
                     * the heartbeat script of this service or the we look
                     * in the wrong places.
                     */
                    LOG.appWarning("setGroupResources: " + hbId
                                   + ": could not find resource agent");
                }
                /* continue of creating/updating of the
                 * service in the gui.
                 */
                newSi = getBrowser().getServiceInfoFromCRMId(hbId);
                final Map<String, String> resourceNode =
                                             clStatus.getParamValuePairs(hbId);
                if (newSi == null) {
                    newService = true;
                    // TODO: get rid of the service name? (everywhere)
                    final String serviceName;
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
                    if (newGi != null) {
                        newGi.addGroupServicePanel(newSi, false);
                    } else if (newCi != null) {
                        newCi.addCloneServicePanel(newSi);
                    } else {
                        final Point2D p = null;
                        addServicePanel(newSi, p, false, false, runMode);
                    }
                } else {
                    getBrowser().addNameToServiceInfoHash(newSi);
                    setParametersHash.put(newSi, resourceNode);
                }
                newSi.getService().setNew(false);
                serviceIsPresent.add(newSi);
                if (newGi != null || newCi != null) {
                    groupServiceIsPresent.add(newSi);
                }
            }
            final DefaultMutableTreeNode n = newSi.getNode();
            if (n != null) {
                final int p = pos;
                Tools.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        final MutableTreeNode parent =
                                (MutableTreeNode) n.getParent();
                        if (parent != null) {
                            final int i = parent.getIndex(n);
                            if (i > p) {
                                parent.remove(n);
                                parent.insert(n, p);
                                getBrowser().reloadAndWait(parent, false);
                            }
                        }
                    }
                });
                pos++;
            }
        }

        for (final Map.Entry<ServiceInfo, Map<String, String>> setEntry
                                                 : setParametersHash.entrySet()) {
            setEntry.getKey().setParameters(setEntry.getValue());
            if (Application.isLive(runMode)) {
                setEntry.getKey().setUpdated(false);
            }
        }
        if (newService) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getBrowser().reloadAndWait(
                                        getBrowser().getServicesNode(), false);
                }
            });
        }
        hg.repaint();
    }

    /**
     * This functions goes through all services, constrains etc. in
     * clusterStatus and updates the internal structures and graph.
     */
    public void setAllResources(final ClusterStatus clStatus,
                                final Application.RunMode runMode) {
        if (clStatus == null) {
            return;
        }
        final Set<String> allGroupsAndClones = clStatus.getAllGroups();
        final CRMGraph hg = getBrowser().getCRMGraph();
        final List<ServiceInfo> groupServiceIsPresent =
                                                  new ArrayList<ServiceInfo>();
        final List<ServiceInfo> serviceIsPresent = new ArrayList<ServiceInfo>();
        for (final String groupOrClone : allGroupsAndClones) {
            CloneInfo newCi = null;
            GroupInfo newGi = null;
            if (clStatus.isClone(groupOrClone)) {
                /* clone */
                newCi = setCreateCloneInfo(groupOrClone, clStatus, runMode);
                serviceIsPresent.add(newCi);
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
                newGi = setCreateGroupInfo(groupOrClone,
                                           newCi,
                                           clStatus,
                                           runMode);
                serviceIsPresent.add(newGi);
            }
            setGroupResources(allGroupsAndClones,
                              groupOrClone,
                              newGi,
                              newCi,
                              serviceIsPresent,
                              groupServiceIsPresent,
                              clStatus,
                              runMode);
        }

        hg.clearKeepColocationList();
        hg.clearKeepOrderList();
        /* resource sets */
        final List<CRMXML.RscSetConnectionData> rscSetConnections =
                                       clStatus.getRscSetConnections();
        if (rscSetConnections != null) {
            final Map<CRMXML.RscSetConnectionData, ConstraintPHInfo>
             rdataToCphi =
                         new LinkedHashMap<CRMXML.RscSetConnectionData,
                                           ConstraintPHInfo>();
            getBrowser().lockNameToServiceInfo();
            final Map<String, ServiceInfo> idToInfoHash =
                 getBrowser().getNameToServiceInfoHash(
                                                ConstraintPHInfo.NAME);
            final List<ConstraintPHInfo> preNewCphis =
                                    new ArrayList<ConstraintPHInfo>();
            if (idToInfoHash != null) {
                for (final Map.Entry<String, ServiceInfo> infoEntry : idToInfoHash.entrySet()) {
                    final ConstraintPHInfo cphi =
                               (ConstraintPHInfo) infoEntry.getValue();
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
            final Collection<ConstraintPHInfo> newCphis =
                                    new ArrayList<ConstraintPHInfo>();
            for (final CRMXML.RscSetConnectionData rdata
                                                : rscSetConnections) {
                ConstraintPHInfo cphi = null;

                for (final Map.Entry<CRMXML.RscSetConnectionData, ConstraintPHInfo> phEntry
                                              : rdataToCphi.entrySet()) {
                    if (phEntry.getKey() == rdata) {
                        continue;
                    }
                    if (rdata.equals(phEntry.getKey())
                        || rdata.equalsReversed(phEntry.getKey())) {
                        cphi = phEntry.getValue();
                        cphi.setRscSetConnectionData(rdata);
                        break;
                    }
                }
                PcmkRscSetsInfo prsi = null;
                if (cphi == null) {
                    for (final Map.Entry<CRMXML.RscSetConnectionData, ConstraintPHInfo> phEntry
                                              : rdataToCphi.entrySet()) {
                        if (phEntry.getKey() == rdata) {
                            cphi = phEntry.getValue();
                            break;
                        }
                        if (phEntry.getValue().sameConstraintId(
                                rdata)) {
                            /* use the same rsc set info object */
                            prsi = phEntry.getValue().getPcmkRscSetsInfo();
                        }
                        if (phEntry.getValue().getService().isNew()
                            || (rdata.samePlaceholder(phEntry.getKey())
                                && phEntry.getValue().sameConstraintId(
                                rdata))) {
                            cphi = phEntry.getValue();
                            cphi.setRscSetConnectionData(rdata);
                            prsi = cphi.getPcmkRscSetsInfo();
                            if (prsi != null) {
                                if (rdata.isColocation()) {
                                    prsi.addColocation(
                                               rdata.getConstraintId(),
                                               cphi);
                                } else {
                                    prsi.addOrder(
                                               rdata.getConstraintId(),
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
                    cphi = new ConstraintPHInfo(
                                            getBrowser(),
                                            rdata,
                                            ConstraintPHInfo.Preference.AND);
                    if (prsi == null) {
                        prsi = new PcmkRscSetsInfo(getBrowser());
                    }
                    if (rdata.isColocation()) {
                        prsi.addColocation(rdata.getConstraintId(),
                                           cphi);
                    } else {
                        prsi.addOrder(rdata.getConstraintId(), cphi);
                    }
                    cphi.setPcmkRscSetsInfo(prsi);
                    getBrowser().addNameToServiceInfoHash(cphi);
                    newCphis.add(cphi); /* have to add it later,
                                           so that ids are correct. */
                    rdataToCphi.put(rdata, cphi);
                }
                serviceIsPresent.add(cphi);

                final CRMXML.RscSet rscSet1 = rdata.getRscSet1();
                final CRMXML.RscSet rscSet2 = rdata.getRscSet2();
                if (rdata.isColocation()) {
                    /* colocation */
                    if (rscSet1 != null) {
                        for (final String rscId : rscSet1.getRscIds()) {
                            final ServiceInfo si =
                               getBrowser().getServiceInfoFromCRMId(
                                                                rscId);
                            hg.addColocation(rdata.getConstraintId(),
                                             cphi,
                                             si);
                        }
                    }
                    if (rscSet2 != null) {
                        for (final String rscId : rscSet2.getRscIds()) {
                            final ServiceInfo si =
                               getBrowser().getServiceInfoFromCRMId(
                                                                rscId);
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
                               getBrowser().getServiceInfoFromCRMId(
                                                                rscId);
                            hg.addOrder(rdata.getConstraintId(),
                                        si,
                                        cphi);
                        }
                    }
                    if (rscSet2 != null) {
                        for (final String rscId : rscSet2.getRscIds()) {
                            final ServiceInfo si =
                               getBrowser().getServiceInfoFromCRMId(
                                                                rscId);
                            hg.addOrder(rdata.getConstraintId(),
                                        cphi,
                                        si);
                        }
                    }
                }
                if (Application.isLive(runMode)) {
                    cphi.setUpdated(false);
                    cphi.getService().setNew(false);
                }
            }

            for (final ConstraintPHInfo cphi : newCphis) {
                hg.addConstraintPlaceholder(cphi,
                                            null, /* pos */
                                            Application.RunMode.LIVE);
            }
        }

        /* colocations */
        final Map<String, List<CRMXML.ColocationData>> colocationMap =
                                        clStatus.getColocationRscMap();
        for (final Map.Entry<String, List<CRMXML.ColocationData>> colocationEntry
                                                            : colocationMap.entrySet()) {
            final List<CRMXML.ColocationData> withs =
                    colocationEntry.getValue();
            for (final CRMXML.ColocationData data : withs) {
                final String withRscId = data.getWithRsc();
                final ServiceInfo withSi =
                      getBrowser().getServiceInfoFromCRMId(withRscId);
                final ServiceInfo siP =
                           getBrowser().getServiceInfoFromCRMId(colocationEntry.getKey());
                hg.addColocation(data.getId(), siP, withSi);
            }
        }

        /* orders */
        final Map<String, List<CRMXML.OrderData>> orderMap =
                                              clStatus.getOrderRscMap();
        for (final Map.Entry<String, List<CRMXML.OrderData>> orderEntry : orderMap.entrySet()) {
            for (final CRMXML.OrderData data
                                         : orderEntry.getValue()) {
                final String rscThenId = data.getRscThen();
                final ServiceInfo si =
                        getBrowser().getServiceInfoFromCRMId(rscThenId);
                if (si != null) { /* not yet complete */
                    final ServiceInfo siP =
                      getBrowser().getServiceInfoFromCRMId(orderEntry.getKey());
                    if (siP != null && siP.getResourceAgent() != null) {
                        /* dangling orders and colocations */
                        if ((siP.getResourceAgent().isDrbddisk()
                             || siP.getResourceAgent().isLinbitDrbd())
                            && "Filesystem".equals(si.getName())) {
                            final List<CRMXML.ColocationData> cds =
                               clStatus.getColocationDatas(orderEntry.getKey());
                            if (cds != null) {
                                for (final CRMXML.ColocationData cd
                                                               : cds) {
                                    if (cd.getWithRsc().equals(
                                                         rscThenId)) {
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

        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> e = getNode().children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode n = e.nextElement();
            final ServiceInfo g = (ServiceInfo) n.getUserObject();
            if (g.getResourceAgent().isGroup()
                || g.getResourceAgent().isClone()) {
                @SuppressWarnings("unchecked")
                final Enumeration<DefaultMutableTreeNode> ge =
                                                        g.getNode().children();
                while (ge.hasMoreElements()) {
                    final DefaultMutableTreeNode gn = ge.nextElement();
                    final ServiceInfo s = (ServiceInfo) gn.getUserObject();
                    if (!groupServiceIsPresent.contains(s)
                        && !s.getService().isNew()) {
                        /* remove the group service from the menu
                           that does not exist anymore. */
                        s.removeInfo();
                    }
                }
            }
        }
        hg.setServiceIsPresentList(serviceIsPresent);
        /** Set placeholders to "new", if they have no connections. */
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                hg.killRemovedEdges();
                final Map<String, ServiceInfo> idToInfoHash =
                   getBrowser().getNameToServiceInfoHash(ConstraintPHInfo.NAME);
                if (idToInfoHash != null) {
                    for (final Map.Entry<String, ServiceInfo> serviceEntry : idToInfoHash.entrySet()) {
                        final ConstraintPHInfo cphi =
                                       (ConstraintPHInfo) serviceEntry.getValue();
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

    /** Clears the info panel cache, forcing it to reload. */
    @Override
    boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /** Returns type of the info text. text/plain or text/html. */
    @Override
    protected String getInfoType() {
        return Tools.MIME_TYPE_TEXT_HTML;
    }

    /**
     * Returns info for info panel, that hb status failed or null, in which
     * case the getInfoPanel() function will show.
     */
    @Override
    String getInfo() {
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
        rdi.widgetClear();
        final String[] params = rdi.getParametersFromXML();
        rdi.addParams(optionsPanel,
                      params,
                      leftWidth,
                      rightWidth,
                      null);
    }

    /** Returns editable info panel for global crm config. */
    @Override
    public JComponent getInfoPanel() {
        /* if don't have hb status we don't have all the info we need here.
         * TODO: OR we need to get hb status only once
         */
        if (getBrowser().clStatusFailed()) {
            return super.getInfoPanel();
        }
        final CRMGraph hg = getBrowser().getCRMGraph();
        if (infoPanel != null) {
            hg.pickBackground();
            return infoPanel;
        }
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.PAGE_AXIS));
        if (getBrowser().getCRMXML() == null
            || getBrowser().getClusterStatus() == null) {
            return newPanel;
        }
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;

            /**
             * Whether the whole thing should be enabled.
             */
            @Override
            public boolean isEnabled() {
                final Host dcHost = getBrowser().getDCHost();
                return dcHost != null && !Tools.versionBeforePacemaker(dcHost);
            }

            @Override
            public void mouseOut(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                hg.stopTestAnimation((JComponent) component);
                component.setToolTipText("");
            }

            @Override
            public void mouseOver(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                component.setToolTipText(
                                        ClusterBrowser.STARTING_PTEST_TOOLTIP);
                component.setToolTipBackground(Tools.getDefaultColor(
                                    "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                hg.startTestAnimation((JComponent) component, startTestLatch);
                final Host dcHost = getBrowser().getDCHost();
                getBrowser().ptestLockAcquire();
                try {
                    final ClusterStatus clStatus =
                                              getBrowser().getClusterStatus();
                    clStatus.setPtestData(null);
                    apply(dcHost, Application.RunMode.TEST);
                    final PtestData ptestData =
                                          new PtestData(CRM.getPtest(dcHost));
                    component.setToolTipText(ptestData.getToolTip());
                    clStatus.setPtestData(ptestData);
                } finally {
                    getBrowser().ptestLockRelease();
                }
                startTestLatch.countDown();
            }
        };
        initCommitButton(buttonCallback);
        getBrowser().getRscDefaultsInfo().setApplyButton(getApplyButton());
        getBrowser().getRscDefaultsInfo().setRevertButton(getRevertButton());
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        buttonPanel.add(getActionsButton(), BorderLayout.LINE_END);

        newPanel.add(buttonPanel);

        final String[] params = getParametersFromXML();
        addParams(optionsPanel,
                  params,
                  Tools.getDefaultSize("ClusterBrowser.DrbdResLabelWidth"),
                  Tools.getDefaultSize("ClusterBrowser.DrbdResFieldWidth"),
                  null);

        addRscDefaultsPanel(
                      optionsPanel,
                      Tools.getDefaultSize("ClusterBrowser.DrbdResLabelWidth"),
                      Tools.getDefaultSize("ClusterBrowser.DrbdResFieldWidth"));
        getApplyButton().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    LOG.debug1("actionPerformed: BUTTON: apply");
                    final Thread thread = new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(getBrowser().getDCHost(), Application.RunMode.LIVE);
                                getBrowser().clStatusUnlock();
                            }
                        }
                    );
                    thread.start();
                }
            }
        );
        getRevertButton().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    LOG.debug1("actionPerformed: BUTTON: revert");
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getBrowser().clStatusLock();
                            revert();
                            getBrowser().clStatusUnlock();
                        }
                    });
                    thread.start();
                }
            }
        );

        /* apply button */
        addApplyButton(buttonPanel);
        addRevertButton(buttonPanel);
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                setApplyButtons(null, params);
            }
        });

        mainPanel.add(optionsPanel);

        newPanel.add(getMoreOptionsPanel(
                                  ClusterBrowser.SERVICE_LABEL_WIDTH
                                  + ClusterBrowser.SERVICE_FIELD_WIDTH + 4));
        newPanel.add(new JScrollPane(mainPanel));

        hg.pickBackground();
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    /** Returns heartbeat graph. */
    @Override
    public JPanel getGraphicalView() {
        return getBrowser().getCRMGraph().getGraphPanel();
    }

    /**
     * Adds service to the list of services.
     * TODO: are they both used?
     */
    ServiceInfo addServicePanel(final ResourceAgent newRA,
                                final Point2D pos,
                                final boolean reloadNode,
                                final String heartbeatId,
                                final CloneInfo newCi,
                                final Application.RunMode runMode) {
        final ServiceInfo newServiceInfo;
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
            final String cloneName;
            if (master) {
                cloneName = Application.PM_MASTER_SLAVE_SET_NAME;
            } else {
                cloneName = Application.PM_CLONE_SET_NAME;
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
                            runMode);
        }
        return newServiceInfo;
    }

    /**
     * Adds new service to the specified position. If position is null, it
     * will be computed later. reloadNode specifies if the node in
     * the menu should be reloaded and get uptodate.
     */
    void addServicePanel(final ServiceInfo newServiceInfo,
                         final Point2D pos,
                         final boolean reloadNode,
                         final boolean interactive,
                         final Application.RunMode runMode) {
        newServiceInfo.getService().setResourceClass(
                    newServiceInfo.getResourceAgent().getResourceClass());
        final CRMGraph hg = getBrowser().getCRMGraph();
        getBrowser().addNameToServiceInfoHash(newServiceInfo);
        if (!hg.addResource(newServiceInfo,
                            null,
                            pos,
                            false, /* colocation only */
                            false, /* order only */
                            runMode)) {
            final DefaultMutableTreeNode newServiceNode =
                                new DefaultMutableTreeNode(newServiceInfo);
            newServiceInfo.setNode(newServiceNode);
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getBrowser().getServicesNode().add(newServiceNode);
                }
            });
            if (interactive) {
                if (newServiceInfo.getResourceAgent().isProbablyMasterSlave()) {
                    /* only if it was added manually. */
                    newServiceInfo.changeType(
                                        ServiceInfo.MASTER_SLAVE_TYPE_STRING);
                } else if (
                        newServiceInfo.getResourceAgent().isProbablyClone()) {
                    newServiceInfo.changeType(
                                        ServiceInfo.CLONE_TYPE_STRING);
                }
            }
            if (reloadNode) {
                /* show it */
                getBrowser().reload(getBrowser().getServicesNode(), false);
                getBrowser().reload(newServiceNode, true);
            }
            getBrowser().reloadAllComboBoxes(newServiceInfo);
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    hg.scale();
                }
            });
        }
        hg.reloadServiceMenus();
    }

    /** Returns 'add service' list for graph popup menu. */
    List<ResourceAgent> getAddServiceList(final String cl) {
        return getBrowser().globalGetAddServiceList(cl);
    }

    /**
     * Returns background popup. Click on background represents cluster as
     * whole.
     */
    @Override
    public List<UpdatableItem> createPopup() {
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
                    hidePopup();
                    addServicePanel(getBrowser().getCRMXML().getHbGroup(),
                                    getPos(),
                                    true,
                                    null,
                                    null,
                                    runMode);
                    getBrowser().getCRMGraph().repaint();
                }
            };
        items.add(addGroupMenuItem);
        final ServicesInfo thisClass = this;

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
                            hidePopup();
                            addServicePanel(ipService,
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
                            hidePopup();
                            final FilesystemInfo fsi = (FilesystemInfo)
                                                           addServicePanel(
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
                    final List<ResourceAgent> services = getAddServiceList(cl);
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
                        && !getAddServiceList(
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
                                hidePopup();
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
                                addServicePanel(ra,
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
                                        thisClass,
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
                    hidePopup();
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
                    hidePopup();
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
                hidePopup();
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
        addMouseOverListener(stopAllMenuItem, stopAllItemCallback);
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
                hidePopup();
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
        addMouseOverListener(unmigrateAllMenuItem, unmigrateAllItemCallback);
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
        addMouseOverListener(removeMenuItem, removeItemCallback);
        items.add((UpdatableItem) removeMenuItem);

        /* cluster wizard */
        final UpdatableItem clusterWizardItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.ClusterWizard"),
                           CLUSTER_ICON,
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
                           LOGFILE_ICON,
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

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all parameters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    @Override
    public Check checkResourceFields(final String param,
                                     final String[] params) {
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        final Check check = new Check(new ArrayList<String>(),
                                      new ArrayList<String>());
        check.addCheck(rdi.checkResourceFields(param,
                                               rdi.getParametersFromXML(),
                                               true));
        check.addCheck(super.checkResourceFields(param, params));
        for (final ServiceInfo si : getBrowser().getExistingServiceList(null)) {
            check.addCheck(si.checkResourceFields(null,
                                                  si.getParametersFromXML(),
                                                  true,
                                                  false,
                                                  false));
        }
        return check;
    }

    /** Revert all values. */
    @Override
    public void revert() {
        super.revert();
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        rdi.revert();
        for (final ServiceInfo si : getBrowser().getExistingServiceList(null)) {
            if (si.checkResourceFields(null,
                                       si.getParametersFromXML(),
                                       true,
                                       false,
                                       false).isChanged()) {
                si.revert();
            }
        }
        //TODO: should remove new resources and constraints
    }

    /**
     * Copy/paste field from one field to another.
     */
    private void copyPasteField(final Widget oldWi, final Widget newWi) {
        if (newWi == null || oldWi == null) {
            return;
        }
        final Value oldValue = oldWi.getValue();
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                if (oldValue == null || oldValue.isNothingSelected()) {
                    newWi.setValueNoListeners(null);
                } else {
                    newWi.setValueNoListeners(oldValue);
                }
            }
        });
    }

    private void copyPasteFields(final ServiceInfo oldSi,
                                 final ServiceInfo newSi) {
        /* parameters */
        for (final String param : oldSi.getParametersFromXML()) {
            if (ServiceInfo.GUI_ID.equals(param)
                || ServiceInfo.PCMK_ID.equals(param)) {
                if (getBrowser().isCRMId(oldSi.getService().getHeartbeatId())) {
                    continue;
                }
            }
            copyPasteField(oldSi.getWidget(param, null),
                           newSi.getWidget(param, null));
        }

        /* operations */
        copyPasteField(oldSi.getSameAsOperationsWi(),
                       newSi.getSameAsOperationsWi());

        for (final String op : oldSi.getResourceAgent().getOperationNames()) {
            for (final String param
                          : getBrowser().getCRMOperationParams(op)) {
                copyPasteField(oldSi.getOperationsComboBox(op, param),
                               newSi.getOperationsComboBox(op, param));
            }
        }

        /* locations */
        for (final Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            copyPasteField(oldSi.getScoreComboBoxHash().get(hi),
                           newSi.getScoreComboBoxHash().get(hi));
        }
        /* ping */
        copyPasteField(oldSi.getPingComboBox(),
                       newSi.getPingComboBox());
    }

    public void pasteServices(final List<Info> oldInfos) {
        if (oldInfos.isEmpty()) {
            return;
        }
        final String cn = getBrowser().getCluster().getName();
        Tools.startProgressIndicator(cn, "paste");
        final ClusterBrowser otherBrowser =
                                (ClusterBrowser) oldInfos.get(0).getBrowser();
        getBrowser().getClusterViewPanel().setDisabledDuringLoad(true);
        otherBrowser.getClusterViewPanel().setDisabledDuringLoad(true);
        for (Info oldI : oldInfos) {
            CloneInfo oci = null;
            if (oldI instanceof CloneInfo) {
                oci = (CloneInfo) oldI;
                oldI = oci.getContainedService();
            }
            final CloneInfo oldCi = oci;
            if (oldI instanceof ServiceInfo) {
                final ServiceInfo oldSi = (ServiceInfo) oldI;
                final ServiceInfo newSi =
                    addServicePanel(oldSi.getResourceAgent(),
                                    null, /* pos */
                                    true,
                                    null, /* clone id */
                                    null,
                                    Application.RunMode.LIVE);
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!(newSi instanceof CloneInfo)) {
                            oldSi.getInfoPanel();
                            newSi.getInfoPanel();
                            oldSi.waitForInfoPanel();
                            newSi.waitForInfoPanel();
                        }
                        if (oldCi != null) {
                            final Value v = newSi.getTypeRadioGroup().getValue();
                            if (oldCi.getService().isMaster()) {
                                if (!ServiceInfo.MASTER_SLAVE_TYPE_STRING.equals(v)) {
                                    newSi.getTypeRadioGroup().setValue(
                                         ServiceInfo.MASTER_SLAVE_TYPE_STRING);
                                }
                            } else {
                                if (!ServiceInfo.CLONE_TYPE_STRING.equals(v)) {
                                    newSi.getTypeRadioGroup().setValue(
                                                ServiceInfo.CLONE_TYPE_STRING);
                                }
                            }
                        }
                        copyPasteFields(oldSi, newSi);
                    }
                });

                /* clone parameters */
                final CloneInfo newCi = newSi.getCloneInfo();
                if (newCi != null) {
                    for (final String param : oldCi.getParametersFromXML()) {
                        if (ServiceInfo.GUI_ID.equals(param)
                            || ServiceInfo.PCMK_ID.equals(param)) {
                            if (getBrowser().isCRMId(
                                    oldCi.getService().getHeartbeatId())) {
                                continue;
                            }
                        }
                        Tools.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                copyPasteField(oldCi.getWidget(param, null),
                                               newCi.getWidget(param, null));
                            }
                        });
                    }
                }
                if (oldI instanceof GroupInfo) {
                    final GroupInfo oldGi = (GroupInfo) oldI;
                    final GroupInfo newGi = (GroupInfo) newSi;

                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            @SuppressWarnings("unchecked")
                            final Enumeration<DefaultMutableTreeNode> e =
                                                    oldGi.getNode().children();
                            while (e.hasMoreElements()) {
                                final DefaultMutableTreeNode n =
                                                               e.nextElement();
                                final ServiceInfo oldChild =
                                               (ServiceInfo) n.getUserObject();
                                oldChild.getInfoPanel();
                                final ServiceInfo newChild =
                                                newGi.addGroupServicePanel(
                                                    oldChild.getResourceAgent(),
                                                    false);
                                newChild.getInfoPanel();
                                        copyPasteFields(oldChild, newChild);
                            }
                            getBrowser().reload(newGi.getNode(), false);
                        }
                    });
                }
            }
        }
        Tools.stopProgressIndicator(cn, "paste");
        otherBrowser.getClusterViewPanel().setDisabledDuringLoad(false);
        getBrowser().getClusterViewPanel().setDisabledDuringLoad(false);
    }
}
