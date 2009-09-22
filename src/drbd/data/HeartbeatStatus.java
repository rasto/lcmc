/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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


package drbd.data;

import drbd.utilities.Tools;
import drbd.utilities.ConvertCmdCallback;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * This class parses heartbeat status, stores information
 * in the hashes and provides methods to get this information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class HeartbeatStatus {
    /** Host. */
    private final Host host;
    /** Data from cib query. */
    private volatile CibQuery cibQueryMap = new CibQuery();
    /** DC Node. */
    private String dc = null;
    /** HeartbeatXML object. */
    private final HeartbeatXML heartbeatXML;
    /** On which node the resource is running. */
    private volatile Map<String, String> runningOnNodeHash = null;

    /**
     * Prepares a new <code>HeartbeatStatus</code> object.
     * Gets and parses metadata from pengine and crmd.
     */
    public HeartbeatStatus(final Host host,
                           final HeartbeatXML heartbeatXML) {
        this.host = host;
        this.heartbeatXML = heartbeatXML;
        final String command =
                   host.getDistCommand("Heartbeat.getClusterMetadata",
                                       (ConvertCmdCallback) null);
        final String output =
                    Tools.execCommandProgressIndicator(
                            host,
                            command,
                            null,  /* ExecCallback */
                            false, /* outputVisible */
                            Tools.getString("Heartbeat.getClusterMetadata"));
        if (output != null) {
            heartbeatXML.parseClusterMetaData(output);
        }
    }

    /**
     * Returns value of global config parameter.
     */
    public final String getGlobalParam(final String param) {
        return cibQueryMap.getCrmConfig().get(param);
    }

    /**
     * Returns global parameters.
     */
    public final String[] getGlobalParameters() {
        final Map<String, String> globalConfigMap =
                                                new HashMap<String, String>();
        if (globalConfigMap != null) {
            return globalConfigMap.keySet().toArray(
                                        new String[globalConfigMap.size()]);
        }
        return null;
    }

    /**
     * Returns value of parameter.
     */
    public final String getParameter(final String hbId, final String param) {
        final Map<String, String> params =
                                        cibQueryMap.getParameters().get(hbId);
        if (params != null) {
            return params.get(param);
        }
        return null;
    }

    /**
     * Returns hash with parameter name and nvpair id of the specified service.
     */
    public final Map<String, String> getParametersNvpairsIds(
                                                          final String hbId) {
        return cibQueryMap.getParametersNvpairsIds().get(hbId);
    }

    /**
     * Returns the dc host.
     */
    public final String getDC() {
        return cibQueryMap.getDC();
    }

    /**
     * Sets the dc host.
     */
    public final void setDC(final String dc) {
        cibQueryMap.setDC(dc);
    }

    /**
     * Returns a map from parameter to its values.
     */
    public final Map<String, String> getParamValuePairs(final String hbId) {
        return cibQueryMap.getParameters().get(hbId);
    }

    /**
     * Returns all groups as an string array.
     */
    public final String[] getAllGroups() {
        final Map<String, List<String>> groupsToResources =
                                            cibQueryMap.getGroupsToResources();
        return groupsToResources.keySet().toArray(
                                        new String[groupsToResources.size()]);
    }

    /**
     * Returns list of resources belonging to the specified group.
     */
    public final List<String> getGroupResources(final String group) {
        return cibQueryMap.getGroupsToResources().get(group);
    }

    /**
     * Returns all clone resources.
     */
    public final String[] getAllClones() {
        final Map<String, String> cloneToResource =
                                             cibQueryMap.getCloneToResource();
        return cloneToResource.keySet().toArray(
                                          new String[cloneToResource.size()]);
    }

    /**
     * Returns whether clone is a master / slave resource.
     */
    public final boolean isMaster(final String pmId) {
        return cibQueryMap.getMasterList().contains(pmId);
    }

    /**
     * Returns resource belonging to the specified clone.
     */
    public final String getCloneResource(final String clone) {
        return cibQueryMap.getCloneToResource().get(clone);
    }

    /**
     * Returns list of all resources.
     */
    public final String[] getAllResources() {
        final Map<String, Map<String, String>> parametersMap =
                                                   cibQueryMap.getParameters();
        return parametersMap.keySet().toArray(
                                            new String[parametersMap.size()]);
    }

    /**
     * Returns clone id.
     */
    public final String getResourceFromClone(final String cloneId) {
        return cibQueryMap.getCloneToResource().get(cloneId);
    }

    /**
     * Returns whether the id is of the clone set.
     */
    public final boolean isClone(final String cloneId) {
        return cibQueryMap.getCloneToResource().containsKey(cloneId);
    }

    /**
     * Returns type of the service, e.g. IPAddr
     */
    public final HeartbeatService getResourceType(final String hbId) {
        return cibQueryMap.getResourceType().get(hbId);
    }

    /**
     * Returns instance_attributes id the service.
     */
    public final String getResourceInstanceAttrId(final String hbId) {
        return cibQueryMap.getResourceInstanceAttrId().get(hbId);
    }

    /**
     * Returns colocation map.
     */
    public final Map<String, List<String>> getColocationMap() {
        return cibQueryMap.getColocation();
    }

    /**
     * Returns order map.
     */
    public final Map<String, List<String>> getOrderMap() {
        return cibQueryMap.getOrder();
    }

    /**
     * Returns colocation id of two resources.
     */
    public final String getColocationId(final String rsc1,
                                        final String rsc2) {
        final String ret = (String) cibQueryMap.getColocationId().get(rsc1,
                                                                      rsc2);
        if (ret == null) {
            return (String) cibQueryMap.getColocationId().get(rsc2, rsc1);
        }
        return ret;
    }

    /**
     * Returns colocation score.
     */
    public final String getColocationScore(final String rsc1,
                                           final String rsc2) {
        final String ret = (String) cibQueryMap.getColocationScore().get(rsc1,
                                                                         rsc2);
        if (ret == null) {
            return (String) cibQueryMap.getColocationScore().get(rsc2, rsc1);
        }
        return ret;
    }

    /**
     * Returns colocation rsc1 role.
     */
    public final String getColocationRscRole(final String rsc1,
                                             final String rsc2) {
        final String ret = (String) cibQueryMap.getColocationRscRole().get(
                                                                         rsc1,
                                                                         rsc2);
        if (ret == null) {
            return (String) cibQueryMap.getColocationRscRole().get(rsc2, rsc1);
        }
        return ret;
    }

    /**
     * Returns colocation rsc2 role.
     */
    public final String getColocationWithRscRole(final String rsc1,
                                                 final String rsc2) {
        final String ret = (String) cibQueryMap.getColocationWithRscRole().get(
                                                                         rsc1,
                                                                         rsc2);
        if (ret == null) {
            return (String) cibQueryMap.getColocationWithRscRole().get(rsc2,
                                                                       rsc1);
        }
        return ret;
    }

    /**
     * Returns order id of two resources.
     */
    public final String getOrderId(final String rsc1, final String rsc2) {
        final String ret = (String) cibQueryMap.getOrderId().get(rsc1, rsc2);
        if (ret == null) {
            return (String) cibQueryMap.getOrderId().get(rsc2, rsc1);
        }
        return ret;
    }

    /**
     * Returns order score of two resources.
     */
    public final String getOrderScore(final String rsc1, final String rsc2) {
        final String ret = (String) cibQueryMap.getOrderScore().get(rsc1,
                                                                    rsc2);
        if (ret == null) {
            return (String) cibQueryMap.getOrderScore().get(rsc2, rsc1);
        }
        return ret;
    }

    /**
     * Returns order first-action of two resources.
     */
    public final String getOrderFirstAction(final String rsc1,
                                            final String rsc2) {
        final String ret = (String) cibQueryMap.getOrderFirstAction().get(rsc1,
                                                                          rsc2);
        if (ret == null) {
            return (String) cibQueryMap.getOrderFirstAction().get(rsc2, rsc1);
        }
        return ret;
    }

    /**
     * Returns order then-action of two resources.
     */
    public final String getOrderThenAction(final String rsc1,
                                           final String rsc2) {
        final String ret = (String) cibQueryMap.getOrderThenAction().get(rsc1,
                                                                         rsc2);
        if (ret == null) {
            return (String) cibQueryMap.getOrderThenAction().get(rsc2, rsc1);
        }
        return ret;
    }

    /**
     * Returns a string that takes "true" or "false" value, whether order is
     * symmetrical.
     */
    public final String getOrderSymmetrical(final String rsc1,
                                            final String rsc2) {
        final String ret = (String) cibQueryMap.getOrderSymmetrical().get(rsc1,
                                                                          rsc2);
        if (ret == null) {
            return (String) cibQueryMap.getOrderSymmetrical().get(rsc2, rsc1);
        }
        return ret;
    }

    /**
     * Returns heartbeat ids of all resource locations.
     */
    public final List<String> getLocationIds(final String rsc) {
        final List<String> locs = cibQueryMap.getLocationsId().get(rsc);
        if (locs == null) {
            return new ArrayList<String>();
        } else {
            return locs;
        }
    }

    /**
     * Returns direction of the order constraint. 'before' or 'after'
     */
    public final String getOrderDirection(final String rsc1,
                                          final String rsc2) {
        final String ret = (String) cibQueryMap.getOrderDirection().get(rsc1,
                                                                        rsc2);
        cibQueryMap.getOrderDirection();
        if (ret == null) {
            return (String) cibQueryMap.getOrderDirection().get(rsc2, rsc1);
        }
        return ret;
    }

    /**
     * Returns score for resource and host.
     */
    public final String getScore(final String hbId, final String onHost) {
        final Map<String, String> hostToScoreMap =
                                           cibQueryMap.getLocation().get(hbId);
        if (hostToScoreMap != null) {
            return hostToScoreMap.get(onHost);
        }
        return null;
    }

    /**
     * Returns score from location id.
     */
    public final String getLocationScore(final String locationId) {
        return cibQueryMap.getLocationScore().get(locationId);
    }

    /**
     * Returns location id for specified resource and host.
     */
    public final String getLocationId(final String rsc, final String node) {
        return (String) cibQueryMap.getResHostToLocId().get(rsc, node);
    }

    /**
     * Returns value of an operation.
     */
    public final String getOperation(final String hbId,
                                     final String op,
                                     final String param) {
        return (String) cibQueryMap.getOperations().get(hbId, op, param);
    }

    /**
     * Returns id from operations tag.
     */
    public final String getOperationsId(final String hbId) {
        return cibQueryMap.getOperationsId().get(hbId);
    }

    /**
     * Returns id from heartbeat id and operation name.
     */
    public final String getOpId(final String hbId, final String op) {
        Map<String, String> opIds = cibQueryMap.getResOpIds().get(hbId);
        if (opIds == null) {
            return null;
        }
        return opIds.get(op);
    }

    /**
     * Returns on which node the resource is running.
     */
    public final String getRunningOnNode(final String hbId) {
        if (runningOnNodeHash == null) {
            return null;
        }
        return runningOnNodeHash.get(hbId);
    }

    /**
     * Returns whether the node is active.
     */
    public final boolean isActiveNode(final String node) {
        return cibQueryMap.getActiveNodes().contains(node.toLowerCase());
    }

    /**
     * Returns fail count of the service on the specified node.
     */
     public final String getFailCount(final String node, final String res) {
         return cibQueryMap.getFailCount(node, res);
     }

    /**
     * Parses the command with data.
     */
    private void parseCommand(final String command,
                                    final List<String> data) {
        final String[] commands = command.split("<<<>>>");
        final String cmd = commands[0];

        if (commands.length == 1) {
            if ("res_status".equals(cmd)) {
                parseResStatus(Tools.join("\n", data.toArray(
                                                   new String[data.size()])));

            } else if ("cibadmin".equals(cmd)) {
                parseCibQuery(Tools.join("\n", data.toArray(
                                                   new String[data.size()])));
            }
        } else {
            Tools.appError("unknown command: " + command);
        }
    }

    /**
     * Parses status.
     */
    public final void parseStatus(final String status) {
        final String[] lines = status.split("\n");
        String command    = null;
        List<String> data = null;

        dc = null;
        boolean failed = false;

        /* remove all hashes */
        for (String line : lines) {
            line = line.trim();
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
                if (!failed) {
                    parseCommand(command, data);
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
                Tools.appWarning("Error parsing heartbeat status, line not ok: "
                                 + line
                                 + "\n"
                                 + status);
            }
        }
    }

    /**
     * Parses output from crm_mon.
     */
    private void parseResStatus(final String resStatus) {
        runningOnNodeHash = heartbeatXML.parseResStatus(resStatus);
    }

    /**
     * Parses output from cibadmin command.
     */
    private void parseCibQuery(final String query) {
        cibQueryMap = heartbeatXML.parseCibQuery(query);
    }
}
