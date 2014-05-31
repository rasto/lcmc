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


package lcmc.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import lcmc.data.CRMXML.ResStatus;
import lcmc.utilities.ConvertCmdCallback;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;
import lcmc.utilities.ssh.SSH;
import lcmc.utilities.ssh.SshOutput;

import org.apache.commons.collections15.map.MultiKeyMap;

/**
 * This class parses pacemaker/heartbeat status, stores information
 * in the hashes and provides methods to get this information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class ClusterStatus {
    /** Logger. */
    private static final Logger LOG =
                                 LoggerFactory.getLogger(ClusterStatus.class);
    /** Data from cib query. */
    private volatile CibQuery cibQueryMap = new CibQuery();
    /** Data from shadow cib. */
    private volatile CibQuery shadowCibQueryMap = new CibQuery();
    /** CRMXML object. */
    private final CRMXML crmXML;
    /** On which node the resource is running or is a slave. */
    private volatile Map<String, ResStatus> resStateMap = null;
    /** Results from ptest. */
    private volatile PtestData ptestData = null;
    /** Old status in string. */
    private String oldStatus = null;
    /** Old cib in string. */
    private String oldCib = null;
    /** Old advanced mode. */
    private boolean oldAdvancedMode = false;
    /** Host. */
    private final Host host;

    /**
     * Prepares a new {@code ClusterStatus} object.
     * Gets and parses metadata from pengine and crmd.
     */
    public ClusterStatus(final Host host, final CRMXML crmXML) {
        this.host = host;
        this.crmXML = crmXML;
        final String command =
                   host.getDistCommand("Heartbeat.getClusterMetadata",
                                       (ConvertCmdCallback) null);
        final SshOutput ret =
                    Tools.execCommandProgressIndicator(
                            host,
                            command,
                            null,  /* ExecCallback */
                            false, /* outputVisible */
                            Tools.getString("Heartbeat.getClusterMetadata"),
                            SSH.DEFAULT_COMMAND_TIMEOUT);
        final String output = ret.getOutput();
        if (ret.getExitCode() != 0) {
            return;
        }
        if (output != null) {
            crmXML.parseClusterMetaData(output);
        }
    }

    /** Returns value of global config parameter. */
    public String getGlobalParam(final String param) {
        return cibQueryMap.getCrmConfig().get(param);
    }

    /** Returns value of meta attribute  parameter. */
    public String getRscDefaultsParameter(final String param,
                                          final Application.RunMode runMode) {
        if (ptestData != null && Application.isTest(runMode)) {
            return shadowCibQueryMap.getRscDefaultsParams().get(param);
        } else {
            return cibQueryMap.getRscDefaultsParams().get(param);
        }
    }

    /** Returns value of the rsc defaults id. */
    public String getRscDefaultsId(final Application.RunMode runMode) {
        if (ptestData != null && Application.isTest(runMode)) {
            return shadowCibQueryMap.getRscDefaultsId();
        } else {
            return cibQueryMap.getRscDefaultsId();
        }
    }

    /** Returns value of parameter. */
    public String getParameter(final String hbId,
                               final String param,
                               final Application.RunMode runMode) {
        final Map<String, String> params;
        if (ptestData != null && Application.isTest(runMode)) {
            params = shadowCibQueryMap.getParameters().get(hbId);
        } else {
            params = cibQueryMap.getParameters().get(hbId);
        }
        if (params != null) {
            return params.get(param);
        }
        return null;
    }

    /**
     * Returns hash with parameter name and nvpair id of the specified service.
     */
    public Map<String, String> getParametersNvpairsIds(final String hbId) {
        return cibQueryMap.getParametersNvpairsIds().get(hbId);
    }

    /** Returns the dc host. */
    public String getDC() {
        return cibQueryMap.getDC();
    }

    /** Sets the dc host. */
    public void setDC(final String dc) {
        cibQueryMap.setDC(dc);
    }

    /** Returns a map from parameter to its values for rsc defaults. */
    public Map<String, String> getRscDefaultsValuePairs() {
        return cibQueryMap.getRscDefaultsParams();
    }

    /** Returns op defaults value pairs. */
    public Map<String, Value> getOpDefaultsValuePairs() {
        return cibQueryMap.getOpDefaultsParams();
    }


    /** Returns a map from parameter to its values. */
    public Map<String, String> getParamValuePairs(final String hbId) {
        return cibQueryMap.getParameters().get(hbId);
    }

    /** Returns all groups as an string array. */
    public Set<String> getAllGroups() {
        final Map<String, List<String>> groupsToResources =
                                            cibQueryMap.getGroupsToResources();
        return groupsToResources.keySet();
    }

    /** Returns list of resources belonging to the specified group. */
    public List<String> getGroupResources(final String group,
                                          final Application.RunMode runMode) {
        if (ptestData != null && Application.isTest(runMode)) {
            return shadowCibQueryMap.getGroupsToResources().get(group);
        } else {
            return cibQueryMap.getGroupsToResources().get(group);
        }
    }

    /** Returns whether clone is a master / slave resource. */
    public boolean isMaster(final String pmId) {
        return cibQueryMap.getMasterList().contains(pmId);
    }

    /** Returns whether the id is of the clone set. */
    public boolean isClone(final String cloneId) {
        return cibQueryMap.getCloneToResource().containsKey(cloneId);
    }

    /** Returns type of the service, e.g. IPAddr */
    public ResourceAgent getResourceType(final String hbId) {
        return cibQueryMap.getResourceType().get(hbId);
    }

    /** Returns whether the res is in LRM on host. */
    public boolean isInLRMOnHost(final String hostName,
                                 final String rscId,
                                 final Application.RunMode runMode) {
        final Set<String> inLRMList =
               cibQueryMap.getInLRM().get(hostName.toLowerCase(Locale.US));
        return inLRMList != null && inLRMList.contains(rscId);
    }

    /** Returns whether the res is orphaned. */
    public boolean isOrphaned(final String rscId) {
        return cibQueryMap.getOrphaned().contains(rscId);
    }

    /** Returns instance_attributes id the service. */
    public String getResourceInstanceAttrId(final String hbId) {
        return cibQueryMap.getResourceInstanceAttrId().get(hbId);
    }

    /** Returns colocation data from id. */
    public CRMXML.ColocationData getColocationData(final String colId) {
        return cibQueryMap.getColocationId().get(colId);
    }

    /** Returns list of colocation data from specified resource. */
    public List<CRMXML.ColocationData> getColocationDatas(final String rsc) {
        return cibQueryMap.getColocationRsc().get(rsc);
    }

    /** Returns colocation rsc map. */
    public Map<String, List<CRMXML.ColocationData>> getColocationRscMap() {
        return cibQueryMap.getColocationRsc();
    }

    /** Returns order data from id. */
    public CRMXML.OrderData getOrderData(final String ordId) {
        return cibQueryMap.getOrderId().get(ordId);
    }

    /** Returns list of order data from specified resource. */
    public List<CRMXML.OrderData> getOrderDatas(final String rsc) {
        return cibQueryMap.getOrderRsc().get(rsc);
    }

    /** Returns order rsc map. */
    public Map<String, List<CRMXML.OrderData>> getOrderRscMap() {
        return cibQueryMap.getOrderRsc();
    }

    /** Returns connections between resource sets. */
    public List<CRMXML.RscSetConnectionData> getRscSetConnections() {
        return cibQueryMap.getRscSetConnections();
    }

    /** Returns resource sets associated with the order id. */
    public List<CRMXML.RscSet> getRscSetsOrd(final String ordId) {
       return cibQueryMap.getOrderIdRscSets().get(ordId);
    }

    /** Returns resource sets associated with the colocation id. */
    public List<CRMXML.RscSet> getRscSetsCol(final String colId) {
       return cibQueryMap.getColocationIdRscSets().get(colId);
    }

    /** Returns heartbeat ids of all resource locations. */
    public Iterable<String> getLocationIds(final String rsc,
                                       final Application.RunMode runMode) {
        final List<String> locs;
        if (ptestData != null && Application.isTest(runMode)) {
            locs = shadowCibQueryMap.getLocationsId().get(rsc);
        } else {
            locs = cibQueryMap.getLocationsId().get(rsc);
        }
        if (locs == null) {
            return new ArrayList<String>();
        } else {
            return locs;
        }
    }

    /** Returns location object for resource and host. */
    public HostLocation getScore(final String hbId,
                                 final String onHost,
                                 final Application.RunMode runMode) {
        final Map<String, HostLocation> hostToScoreMap;
        if (ptestData != null && Application.isTest(runMode)) {
            hostToScoreMap = shadowCibQueryMap.getLocation().get(hbId);
        } else {
            hostToScoreMap = cibQueryMap.getLocation().get(hbId);
        }
        if (hostToScoreMap != null) {
            return hostToScoreMap.get(onHost.toLowerCase(Locale.US));
        }
        return null;
    }

    /** Returns ping location object for resource. */
    public HostLocation getPingScore(final String hbId,
                                     final Application.RunMode runMode) {
        final HostLocation hostLocation;
        if (ptestData != null && Application.isTest(runMode)) {
            hostLocation = shadowCibQueryMap.getPingLocation().get(hbId);
        } else {
            hostLocation = cibQueryMap.getPingLocation().get(hbId);
        }
        return hostLocation;
    }

    /** Returns location id for specified resource and host. */
    public String getLocationId(final String rsc,
                                final String node,
                                final Application.RunMode runMode) {
        /* node should not have to be in lower case. */
        if (ptestData != null && Application.isTest(runMode)) {
            return shadowCibQueryMap.getResHostToLocId().get(
                                                rsc,
                                                node.toLowerCase(Locale.US));
        } else {
            return cibQueryMap.getResHostToLocId().get(
                                                rsc,
                                                node.toLowerCase(Locale.US));
        }
    }

    /** Returns location id for ping for specified resource. */
    public String getPingLocationId(final String rsc,
                                    final Application.RunMode runMode) {
        /* node should not have to be in lower case. */
        if (ptestData != null && Application.isTest(runMode)) {
            return shadowCibQueryMap.getResPingToLocId().get(rsc);
        } else {
            return cibQueryMap.getResPingToLocId().get(rsc);
        }
    }

    /** Returns id from meta_attributes tag. */
    public String getMetaAttrsId(final String hbId) {
        return cibQueryMap.getMetaAttrsId().get(hbId);
    }

    /** Returns value of an operation. */
    public Value getOperation(final String hbId,
                              final String op,
                              final String param) {
        return cibQueryMap.getOperations().get(hbId, op, param);
    }

    /** Returns id from operations tag. */
    public String getOperationsId(final String hbId) {
        return cibQueryMap.getOperationsId().get(hbId);
    }

    /** Returns id from heartbeat id and operation name. */
    public String getOpId(final String hbId, final String op) {
        final Map<String, String> opIds =
                                cibQueryMap.getResOpIds().get(hbId);
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
        return cibQueryMap.getMetaAttrsRefs().get(hbId);
    }

    /**
     * Returns crm id from serivce that this service has operations from,
     * or null.
     */
    public String getOperationsRef(final String hbId) {
        return cibQueryMap.getOperationsRefs().get(hbId);
    }

    /** Returns on which nodes the resource is managed. */
    public boolean isManaged(final String hbId, final Application.RunMode runMode) {
        final PtestData pd = ptestData;
        if (pd != null && Application.isTest(runMode)) {
            return pd.isManaged(hbId);
        }
        if (resStateMap == null) {
            return true;
        }
        final ResStatus resStatus = resStateMap.get(hbId);
        return resStatus == null || resStatus.isManaged();
    }


    /** Returns on which nodes the resource is running. */
    public List<String> getRunningOnNodes(final String hbId,
                                          final Application.RunMode runMode) {
        final PtestData pd = ptestData;
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
        final ResStatus resStatus = resStateMap.get(hbId);
        if (resStatus == null) {
            return null;
        }
        /* this one is already sorted. */
        return resStatus.getRunningOnNodes();
    }

    /** Returns on which nodes the resource is slave. */
    public List<String> getSlaveOnNodes(final String hbId,
                                        final Application.RunMode runMode) {
        final PtestData pd = ptestData;
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
        final ResStatus resStatus = resStateMap.get(hbId);
        if (resStatus == null) {
            return null;
        }
        return resStatus.getSlaveOnNodes();
    }

    /** Returns on which nodes the resource is master. */
    public List<String> getMasterOnNodes(final String hbId,
                                         final Application.RunMode runMode) {
        final PtestData pd = ptestData;
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
        final ResStatus resStatus = resStateMap.get(hbId);
        if (resStatus == null) {
            return null;
        }
        return resStatus.getMasterOnNodes();
    }

    public Map<String, String> getAllocationScores(final String crmId,
                                                   final Application.RunMode runMode) {
        if (resStateMap == null) {
            return Collections.emptyMap();
        }
        final ResStatus resStatus = resStateMap.get(crmId);
        if (resStatus == null) {
            return Collections.emptyMap();
        }
        return resStatus.getAllocationScores();
    }


    /**
     * Returns String whether the node is online.
     * "yes", "no" or null if it is unknown.
     */
    public String isOnlineNode(final String node) {
        return cibQueryMap.getNodeOnline().get(node.toLowerCase(Locale.US));
    }

    /**
     * Sets whether the node is online.
     * "yes", "no" or null if it is unknown.
     */
    public void setOnlineNode(final String node, final String online) {
        cibQueryMap.getNodeOnline().put(node.toLowerCase(Locale.US), online);
    }

    /** Returns true if if node is pending. */
    public boolean isPendingNode(final String node) {
        return cibQueryMap.getNodePending().contains(
                                                 node.toLowerCase(Locale.US));
    }

    /** Returns true if if node was fenced. */
    public boolean isFencedNode(final String node) {
        return cibQueryMap.getFencedNodes().contains(
                                                 node.toLowerCase(Locale.US));
    }

    /** Returns fail count of the service on the specified node. */
    public String getFailCount(final String node,
                               final String res,
                               final Application.RunMode runMode) {
        return cibQueryMap.getFailCount(node.toLowerCase(Locale.US), res);
    }

    /** Returns ping count of the specified node. */
    public String getPingCount(final String node, final Application.RunMode runMode) {
        if (ptestData != null && Application.isTest(runMode)) {
            return shadowCibQueryMap.getPingCount(node.toLowerCase(Locale.US));
        }
        return cibQueryMap.getPingCount(node.toLowerCase(Locale.US));
    }

    /** Returns failed clones for the specified resource. */
    public Set<String> getFailedClones(final String node,
                                       final String res,
                                       final Application.RunMode runMode) {
        return cibQueryMap.getFailedClones().get(node.toLowerCase(Locale.US),
                                                 res);
    }

    /** Returns value for specified node and parameter. */
    public String getNodeParameter(final String node,
                                   final String param,
                                   final Application.RunMode runMode) {
        final MultiKeyMap<String, String> nodeParams;
        if (ptestData != null && Application.isTest(runMode)) {
            nodeParams = shadowCibQueryMap.getNodeParameters();
        } else {
            nodeParams = cibQueryMap.getNodeParameters();
        }
        if (nodeParams == null) {
            return null;
        } else {
            return nodeParams.get(node.toLowerCase(Locale.US), param);
        }
    }

    /** Parses the command with data. */
    private boolean parseCommand(final String command,
                                 final List<String> data) {
        final String[] commands = command.split("<<<>>>");
        final String cmd = commands[0];

        if (commands.length == 1) {
            if ("fenced_nodes".equals(cmd)) {

            } else if ("res_status".equals(cmd)) {
                final String status = Tools.join("\n", data.toArray(
                                                     new String[data.size()]));
                if (!status.equals(oldStatus)) {
                    LOG.debug1("parseCommand: status update: "
                               + host.getName());
                    oldStatus = status;
                    parseResStatus(status);
                    return true;
                }
            } else if ("cibadmin".equals(cmd)) {
                final String cib =
                       Tools.join("\n", data.toArray(new String[data.size()]));
                final boolean advancedMode =
                                        Tools.getApplication().isAdvancedMode();
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

    /** Parses status. */
    public boolean parseStatus(final String status) {
        final String[] lines = status.split("\n");
        String command    = null;
        List<String> data = null;

        boolean failed = false;

        /* remove all hashes */
        boolean updated = false;
        for (final String linenl : lines) {
            final String line = linenl.trim();
            if ("---start---".equals(line)
                || "init".equals(line)
                || "evt:cib_changed".equals(line)) {
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

    /** Parses output from crm_mon. */
    private void parseResStatus(final String resStatus) {
        resStateMap = crmXML.parseResStatus(resStatus);
    }

    /** Parses output from cibadmin command. */
    private void parseCibQuery(final String query) {
        cibQueryMap = crmXML.parseCibQuery(query);
    }

    /** Sets data from ptest. */
    public void setPtestData(final PtestData ptestData) {
        this.ptestData = ptestData;
        if (ptestData == null) {
            shadowCibQueryMap = new CibQuery();
        } else {
            shadowCibQueryMap = crmXML.parseCibQuery(
                "<pcmk>" + ptestData.getShadowCib() + "</pcmk>");
        }
    }

    /** Return last known raw cib. */
    public String getCibXml() {
        return oldCib;
    }
}
