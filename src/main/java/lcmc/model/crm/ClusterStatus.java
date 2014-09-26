/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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


package lcmc.model.crm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import lcmc.model.Application;
import lcmc.model.Host;
import lcmc.model.Value;
import lcmc.model.crm.CrmXml.ResourceStatus;
import lcmc.utilities.ConvertCmdCallback;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;
import lcmc.utilities.ssh.ExecCommandConfig;
import lcmc.utilities.ssh.SshOutput;

import org.apache.commons.collections15.map.MultiKeyMap;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This class parses pacemaker/heartbeat status, stores information
 * in the hashes and provides methods to get this information.
 */
@Named
public final class ClusterStatus {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterStatus.class);
    private volatile CibQuery cibQuery = new CibQuery();
    private volatile CibQuery shadowCibQuery = new CibQuery();
    private CrmXml crmXML;
    /** On which node the resource is running or is a slave. */
    private volatile Map<String, ResourceStatus> resStateMap = null;
    private volatile PtestData ptestResult = null;
    private String oldStatus = null;
    private String oldCib = null;
    private boolean oldAdvancedMode = false;
    private Host host;
    @Inject
    private Application application;

    /**
     * Gets and parses metadata from pengine and crmd.
     */
    public void init(final Host host, final CrmXml crmXML) {
        this.host = host;
        this.crmXML = crmXML;
        final String command = host.getDistCommand("Heartbeat.getClusterMetadata",
                                                   (ConvertCmdCallback) null);
        final SshOutput ret = host.captureCommandProgressIndicator(Tools.getString("Heartbeat.getClusterMetadata"),
                                                                   new ExecCommandConfig().command(command)
                                                                                          .silentCommand()
                                                                                          .silentOutput());
        final String output = ret.getOutput();
        if (ret.getExitCode() != 0) {
            return;
        }
        if (output != null) {
            crmXML.parseClusterMetaData(output);
        }
    }

    public String getGlobalParam(final String param) {
        return cibQuery.getCrmConfig().get(param);
    }

    public String getRscDefaultsParameter(final String param, final Application.RunMode runMode) {
        if (ptestResult != null && Application.isTest(runMode)) {
            return shadowCibQuery.getRscDefaultsParams().get(param);
        } else {
            return cibQuery.getRscDefaultsParams().get(param);
        }
    }

    public String getRscDefaultsId(final Application.RunMode runMode) {
        if (ptestResult != null && Application.isTest(runMode)) {
            return shadowCibQuery.getRscDefaultsId();
        } else {
            return cibQuery.getRscDefaultsId();
        }
    }

    public String getParameter(final String hbId, final String param, final Application.RunMode runMode) {
        final Map<String, String> params;
        if (ptestResult != null && Application.isTest(runMode)) {
            params = shadowCibQuery.getResourceParameters().get(hbId);
        } else {
            params = cibQuery.getResourceParameters().get(hbId);
        }
        if (params != null) {
            return params.get(param);
        }
        return null;
    }

    public Map<String, String> getParametersNvpairsIds(final String hbId) {
        return cibQuery.getResourceParametersNvpairsIds().get(hbId);
    }

    public String getDC() {
        return cibQuery.getDC();
    }

    public void setDC(final String dc) {
        cibQuery.setDC(dc);
    }

    public Map<String, String> getRscDefaultsValuePairs() {
        return cibQuery.getRscDefaultsParams();
    }

    public Map<String, Value> getOpDefaultsValuePairs() {
        return cibQuery.getOpDefaultsParams();
    }

    public Map<String, String> getParamValuePairs(final String hbId) {
        return cibQuery.getResourceParameters().get(hbId);
    }

    public Set<String> getAllGroups() {
        final Map<String, List<String>> groupsToResources = cibQuery.getGroupsToResources();
        return groupsToResources.keySet();
    }

    public List<String> getGroupResources(final String group, final Application.RunMode runMode) {
        if (ptestResult != null && Application.isTest(runMode)) {
            return shadowCibQuery.getGroupsToResources().get(group);
        } else {
            return cibQuery.getGroupsToResources().get(group);
        }
    }

    public boolean isMaster(final String pmId) {
        return cibQuery.getMasterList().contains(pmId);
    }

    public boolean isClone(final String cloneId) {
        return cibQuery.getCloneToResource().containsKey(cloneId);
    }

    public ResourceAgent getResourceType(final String hbId) {
        return cibQuery.getResourceType().get(hbId);
    }

    public boolean isInLRMOnHost(final String hostName, final String rscId, final Application.RunMode runMode) {
        final Set<String> inLRMList = cibQuery.getInLRM().get(hostName.toLowerCase(Locale.US));
        return inLRMList != null && inLRMList.contains(rscId);
    }

    public boolean isOrphaned(final String rscId) {
        return cibQuery.getOrphaned().contains(rscId);
    }

    public String getResourceInstanceAttrId(final String hbId) {
        return cibQuery.getResourceInstanceAttrId().get(hbId);
    }

    public CrmXml.ColocationData getColocationData(final String colId) {
        return cibQuery.getColocationId().get(colId);
    }

    public List<CrmXml.ColocationData> getColocationDatas(final String rsc) {
        return cibQuery.getColocationRsc().get(rsc);
    }

    public Map<String, List<CrmXml.ColocationData>> getColocationRscMap() {
        return cibQuery.getColocationRsc();
    }

    public CrmXml.OrderData getOrderData(final String ordId) {
        return cibQuery.getOrderId().get(ordId);
    }

    public List<CrmXml.OrderData> getOrderDatas(final String rsc) {
        return cibQuery.getOrderRsc().get(rsc);
    }

    public Map<String, List<CrmXml.OrderData>> getOrderRscMap() {
        return cibQuery.getOrderRsc();
    }

    public List<CrmXml.RscSetConnectionData> getRscSetConnections() {
        return cibQuery.getRscSetConnections();
    }

    public List<CrmXml.RscSet> getRscSetsOrd(final String ordId) {
       return cibQuery.getOrderIdRscSets().get(ordId);
    }

    public List<CrmXml.RscSet> getRscSetsCol(final String colId) {
       return cibQuery.getColocationIdRscSets().get(colId);
    }

    public Iterable<String> getLocationIds(final String rsc, final Application.RunMode runMode) {
        final List<String> locs;
        if (ptestResult != null && Application.isTest(runMode)) {
            locs = shadowCibQuery.getLocationsId().get(rsc);
        } else {
            locs = cibQuery.getLocationsId().get(rsc);
        }
        if (locs == null) {
            return new ArrayList<String>();
        } else {
            return locs;
        }
    }

    public HostLocation getScore(final String hbId, final String onHost, final Application.RunMode runMode) {
        final Map<String, HostLocation> hostToScoreMap;
        if (ptestResult != null && Application.isTest(runMode)) {
            hostToScoreMap = shadowCibQuery.getLocations().get(hbId);
        } else {
            hostToScoreMap = cibQuery.getLocations().get(hbId);
        }
        if (hostToScoreMap != null) {
            return hostToScoreMap.get(onHost.toLowerCase(Locale.US));
        }
        return null;
    }

    public HostLocation getPingScore(final String hbId, final Application.RunMode runMode) {
        final HostLocation hostLocation;
        if (ptestResult != null && Application.isTest(runMode)) {
            hostLocation = shadowCibQuery.getPingLocations().get(hbId);
        } else {
            hostLocation = cibQuery.getPingLocations().get(hbId);
        }
        return hostLocation;
    }

    public String getLocationId(final String rsc, final String node, final Application.RunMode runMode) {
        /* node should not have to be in lower case. */
        if (ptestResult != null && Application.isTest(runMode)) {
            return shadowCibQuery.getResHostToLocId().get(rsc, node.toLowerCase(Locale.US));
        } else {
            return cibQuery.getResHostToLocId().get(rsc, node.toLowerCase(Locale.US));
        }
    }

    public String getPingLocationId(final String rsc, final Application.RunMode runMode) {
        /* node should not have to be in lower case. */
        if (ptestResult != null && Application.isTest(runMode)) {
            return shadowCibQuery.getResPingToLocId().get(rsc);
        } else {
            return cibQuery.getResPingToLocId().get(rsc);
        }
    }

    public String getMetaAttrsId(final String hbId) {
        return cibQuery.getMetaAttrsId().get(hbId);
    }

    public Value getOperation(final String hbId, final String op, final String param) {
        return cibQuery.getOperations().get(hbId, op, param);
    }

    public String getOperationsId(final String hbId) {
        return cibQuery.getOperationsId().get(hbId);
    }

    public String getOpId(final String hbId, final String op) {
        final Map<String, String> opIds = cibQuery.getResOpIds().get(hbId);
        if (opIds == null) {
            return null;
        }
        return opIds.get(op);
    }

    /**
     * Returns crm id from serivce that this service has meta attributes from,
     * or null.
     */
    public String getMetaAttrsRef(final String hbId) {
        return cibQuery.getMetaAttrsRefs().get(hbId);
    }

    /**
     * Returns crm id from serivce that this service has operations from,
     * or null.
     */
    public String getOperationsRef(final String hbId) {
        return cibQuery.getOperationsRefs().get(hbId);
    }

    public boolean isManaged(final String hbId, final Application.RunMode runMode) {
        final PtestData pd = ptestResult;
        if (pd != null && Application.isTest(runMode)) {
            return pd.isManaged(hbId);
        }
        if (resStateMap == null) {
            return true;
        }
        final ResourceStatus resourceStatus = resStateMap.get(hbId);
        return resourceStatus == null || resourceStatus.isManagedByCrm();
    }

    public List<String> getRunningOnNodes(final String hbId, final Application.RunMode runMode) {
        final PtestData pd = ptestResult;
        if (pd != null && Application.isTest(runMode)) {
            final List<String> ron = pd.getRunningOnNodes(hbId);
            if (ron != null) {
                Collections.sort(ron);
                return ron;
            }
        }
        if (resStateMap == null) {
            return null;
        }
        final ResourceStatus resourceStatus = resStateMap.get(hbId);
        if (resourceStatus == null) {
            return null;
        }
        /* this one is already sorted. */
        return resourceStatus.getRunningOnNodes();
    }

    public List<String> getSlaveOnNodes(final String hbId, final Application.RunMode runMode) {
        final PtestData pd = ptestResult;
        if (pd != null && Application.isTest(runMode)) {
            final List<String> son = pd.getSlaveOnNodes(hbId);
            if (son != null) {
                Collections.sort(son);
                return son;
            }
        }
        if (resStateMap == null) {
            return null;
        }
        final ResourceStatus resourceStatus = resStateMap.get(hbId);
        if (resourceStatus == null) {
            return null;
        }
        return resourceStatus.getSlaveOnNodes();
    }

    public List<String> getMasterOnNodes(final String hbId, final Application.RunMode runMode) {
        final PtestData pd = ptestResult;
        if (pd != null && Application.isTest(runMode)) {
            final List<String> mon = pd.getMasterOnNodes(hbId);
            if (mon != null) {
                Collections.sort(mon);
                return mon;
            }
        }
        if (resStateMap == null) {
            return null;
        }
        final ResourceStatus resourceStatus = resStateMap.get(hbId);
        if (resourceStatus == null) {
            return null;
        }
        return resourceStatus.getMasterOnNodes();
    }

    public Map<String, String> getAllocationScores(final String crmId, final Application.RunMode runMode) {
        if (resStateMap == null) {
            return Collections.emptyMap();
        }
        final ResourceStatus resourceStatus = resStateMap.get(crmId);
        if (resourceStatus == null) {
            return Collections.emptyMap();
        }
        return resourceStatus.getAllocationScores();
    }

    /**
     * Returns String whether the node is online.
     * "yes", "no" or null if it is unknown.
     */
    public String isOnlineNode(final String node) {
        return cibQuery.getNodeOnline().get(node.toLowerCase(Locale.US));
    }

    /**
     * Sets whether the node is online.
     * "yes", "no" or null if it is unknown.
     */
    public void setOnlineNode(final String node, final String online) {
        cibQuery.getNodeOnline().put(node.toLowerCase(Locale.US), online);
    }

    public boolean isPendingNode(final String node) {
        return cibQuery.getNodePending().contains(node.toLowerCase(Locale.US));
    }

    /** Returns true if if node was fenced. */
    public boolean isFencedNode(final String node) {
        return cibQuery.getFencedNodes().contains(node.toLowerCase(Locale.US));
    }

    public String getFailCount(final String node, final String res, final Application.RunMode runMode) {
        return cibQuery.getFailCount(node.toLowerCase(Locale.US), res);
    }

    public String getPingCount(final String node, final Application.RunMode runMode) {
        if (ptestResult != null && Application.isTest(runMode)) {
            return shadowCibQuery.getPingCount(node.toLowerCase(Locale.US));
        }
        return cibQuery.getPingCount(node.toLowerCase(Locale.US));
    }

    public Set<String> getFailedClones(final String node, final String res, final Application.RunMode runMode) {
        return cibQuery.getResourceFailedCloneIds().get(node.toLowerCase(Locale.US), res);
    }

    public String getNodeParameter(final String node, final String param, final Application.RunMode runMode) {
        final MultiKeyMap<String, String> nodeParams;
        if (ptestResult != null && Application.isTest(runMode)) {
            nodeParams = shadowCibQuery.getNodeParameters();
        } else {
            nodeParams = cibQuery.getNodeParameters();
        }
        if (nodeParams == null) {
            return null;
        } else {
            return nodeParams.get(node.toLowerCase(Locale.US), param);
        }
    }

    private boolean parseCommand(final String command, final List<String> data) {
        final String[] commands = command.split("<<<>>>");
        final String cmd = commands[0];

        if (commands.length == 1) {
            if ("fenced_nodes".equals(cmd)) {

            } else if ("res_status".equals(cmd)) {
                final String status = Tools.join("\n", data.toArray(new String[data.size()]));
                if (!status.equals(oldStatus)) {
                    LOG.debug1("parseCommand: status update: " + host.getName());
                    oldStatus = status;
                    parseResStatus(status);
                    return true;
                }
            } else if ("cibadmin".equals(cmd)) {
                final String cib = Tools.join("\n", data.toArray(new String[data.size()]));
                final boolean advancedMode = application.isAdvancedMode();
                if (!cib.equals(oldCib) || oldAdvancedMode != advancedMode) {
                    LOG.debug1("parseCommand: cib update: " + host.getName());
                    oldCib = cib;
                    oldAdvancedMode = advancedMode;
                    parseCibQuery(cib);
                    return true;
                }
            }
        } else {
            LOG.appError("parseCommand: unknown command: " + command);
        }
        return false;
    }

    public boolean parseStatus(final String status) {
        final String[] lines = status.split("\n");
        String command    = null;
        List<String> data = null;

        boolean failed = false;

        /* remove all hashes */
        boolean updated = false;
        for (final String linenl : lines) {
            final String line = linenl.trim();
            if ("---start---".equals(line) || "init".equals(line) || "evt:cib_changed".equals(line)) {
                continue;
            }
            if ("---done---".equals(line)) {
                break;
            }
            if (command == null) { /* start of command */
                command = line;
                data = null;
                continue;
            }
            if (line.equals(">>>" + command)) { /* end of command */
                if (!failed  && parseCommand(command, data)) {
                    updated = true;
                }
                command = null;
                continue;
            }

            /* first comes 'ok' */
            if ("ok".equals(line)) {
                data = new ArrayList<String>();
                failed = false;
            } else if ("fail".equals(line) || "None".equals(line)) {
                failed = true;
                data = new ArrayList<String>();
            } else if (data != null) {
                if (!failed) {
                    data.add(line);
                }
            } else {
                LOG.appWarning("parseStatus: error parsing heartbeat status, line not ok: " + line + '\n' + status);
            }
        }
        return updated;
    }

    private void parseResStatus(final String resStatus) {
        resStateMap = crmXML.parseResStatus(resStatus);
    }

    private void parseCibQuery(final String query) {
        cibQuery = crmXML.parseCibQuery(query);
    }

    public void setPtestResult(final PtestData ptestResult) {
        this.ptestResult = ptestResult;
        if (ptestResult == null) {
            shadowCibQuery = new CibQuery();
        } else {
            shadowCibQuery = crmXML.parseCibQuery(
                "<pcmk>" + ptestResult.getShadowCib() + "</pcmk>");
        }
    }

    /** Return last known raw cib. */
    public String getCibXml() {
        return oldCib;
    }
}
