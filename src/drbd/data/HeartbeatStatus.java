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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections.map.MultiKeyMap;

/**
 * This class parses heartbeat status, stores information
 * in the hashes and provides methods to get this information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class HeartbeatStatus {
    /** Map from group to the list of resources in this group. */
    private final Map<String, List<String>> groupsToResources =
                                        new HashMap<String, List<String>>();
    /** Map from resource to the group to which it belongs. */
    private final Map<String, String> resourceToGroup =
                                                new HashMap<String, String>();
    /** Map from resource to the node on which the resource is running. */
    private final Map<String, String> runningOnNodeHash =
                                                new HashMap<String, String>();
    /** Map from resource to the list of resources to which this resource is
     * colocated. This is not symetrical. */
    private Map<String, List<String>> colocationMap =
                                            new HashMap<String, List<String>>();
    /** Map from resource to the list of resources to which this resource is
     * in ordered relation. */
    private Map<String, List<String>> orderMap =
                                            new HashMap<String, List<String>>();
    /** Map from resource and host to the location score. */
    private Map<String, Map<String, String>> locationMap =
                                    new HashMap<String, Map<String, String>>();
    /** Map from location id to the location score. */
    private Map<String,String> locationScoreMap = new HashMap<String, String>();
    /** Map from resource and host to location id. */
    private MultiKeyMap resHostToLocIdMap = new MultiKeyMap();
    /** Map from global config parameter to the value. */
    private Map<String, String> globalConfigMap =
                                                new HashMap<String, String>();
    /** Map from service to its heartbeat service object. */
    private Map<String, HeartbeatService> resourceTypeMap =
                                        new HashMap<String, HeartbeatService>();
    /** Map from service to its type. group / native. */
    private final Map<String, String> resourceItemTypeMap =
                                                new HashMap<String, String>();
    /** Map from service to its status: running, not running, unmanaged. */
    private final Map<String, String> resourceStatusMap =
                                                new HashMap<String, String>();
    /** Map from service and its parameters to the values. */
    private Map<String, Map<String, String>> parametersMap =
                                    new HashMap<String, Map<String, String>>();
    /** Whether the crmd or pengine was already parsed. */
    private final List<String> alreadyParsed = new ArrayList<String>();

    /** Map from resource id to the list of locations ids. */
    private Map<String, List<String>> locationsIdMap =
                                           new HashMap<String, List<String>>();
    /** Map from resource 1 and resource 2 to the colocation id. */
    private MultiKeyMap colocationIdMap = new MultiKeyMap();
    /** Map from resource 1 and resource 2 to the score. */
    private MultiKeyMap colocationScoreMap = new MultiKeyMap();
    /** Map from resource 1 and resource 2 to the order id. */
    private MultiKeyMap orderIdMap = new MultiKeyMap();
    /** Map from resource 1 and resource 2 to the order score. */
    private MultiKeyMap orderScoreMap = new MultiKeyMap();
    /** Map from resource 1 and resource 2 to the string if the order is
     * symmetrical. */
    private MultiKeyMap orderSymmetricalMap = new MultiKeyMap();
    /** Map from resource 1 and resource 2 to the direction of the order.
     * (before, after) */
    private MultiKeyMap orderDirectionMap = new MultiKeyMap();
    /** Operations map. */
    private MultiKeyMap operationsMap = new MultiKeyMap();
    /** All nodes. */
    private final Set<String> allNodes = new HashSet<String>();
    /** All active nodes. */
    private final Set<String> activeNodes = new HashSet<String>();
    /** All crm nodes. */
    private final Set<String> crmNodes = new HashSet<String>();
    /** DC Node. */
    private String dc = null;

    /**
     * Returns value of global config parameter.
     */
    public final String getGlobalParam(final String param) {
        return globalConfigMap.get(param);
    }

    /**
     * Returns global parameters.
     */
    public final String[] getGlobalParameters() {
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
        final Map<String, String> params = parametersMap.get(hbId);
        if (params != null) {
            return params.get(param);
        }
        return null;
    }

    /**
     * Returns the dc host.
     */
    public final String getDC() {
        return dc;
    }

    /**
     * Sets the dc host.
     */
    public final void setDC(final String dc) {
        this.dc = dc;
    }

    /**
     * Returns a map from parameter to its values.
     */
    public final Map<String, String> getParamValuePairs(final String hbId) {
        return parametersMap.get(hbId);
    }

    /**
     * Returns all groups as an string array.
     */
    public final String[] getAllGroups() {
        return groupsToResources.keySet().toArray(
                                        new String[groupsToResources.size()]);
    }

    /**
     * Returns list of resources belonging to the specified group.
     */
    public final List<String> getGroupResources(final String group) {
        return groupsToResources.get(group);
    }

    /**
     * Returns list of all resources.
     */
    public final String[] getAllResources() {
        return parametersMap.keySet().toArray(
                                            new String[parametersMap.size()]);
    }

    /**
     * Returns type of the service, e.g. IPAddr
     */
    public final HeartbeatService getResourceType(final String hbId) {
        return resourceTypeMap.get(hbId);
    }

    /**
     * Returns whether the resource is group.
     */
    public final boolean isGroup(final String hbId) {
        return resourceItemTypeMap.get(hbId).equals("group");
    }

    /**
     * Returns the status of the resource.
     */
    public final String getStatus(final String hbId) {
        return resourceStatusMap.get(hbId);
    }

    /**
     * Returns colocation map.
     */
    public final Map<String, List<String>> getColocationMap() {
        return colocationMap;
    }

    /**
     * Returns order map.
     */
    public final Map<String, List<String>> getOrderMap() {
        return orderMap;
    }

    /**
     * Returns colocation id of two resources.
     */
    public final String getColocationId(final String rsc1,
                                        final String rsc2) {
        final String ret = (String) colocationIdMap.get(rsc1, rsc2);
        if (ret == null) {
            return (String) colocationIdMap.get(rsc2, rsc1);
        }
        return ret;
    }

    /**
     * Returns colocation score.
     */
    public final String getColocationScore(final String rsc1,
                                           final String rsc2) {
        final String ret = (String) colocationScoreMap.get(rsc1, rsc2);
        if (ret == null) {
            return (String) colocationScoreMap.get(rsc2, rsc1);
        }
        return ret;
    }


    /**
     * Returns order id of two resources.
     */
    public final String getOrderId(final String rsc1, final String rsc2) {
        final String ret = (String) orderIdMap.get(rsc1, rsc2);
        if (ret == null) {
            return (String) orderIdMap.get(rsc2, rsc1);
        }
        return ret;
    }

    /**
     * Returns order score of two resources.
     */
    public final String getOrderScore(final String rsc1, final String rsc2) {
        final String ret = (String) orderScoreMap.get(rsc1, rsc2);
        if (ret == null) {
            return (String) orderScoreMap.get(rsc2, rsc1);
        }
        return ret;
    }

    /**
     * Returns a string that takes "true" or "false" value, whether order is
     * symmetrical.
     */
    public final String getOrderSymmetrical(final String rsc1,
                                            final String rsc2) {
        final String ret = (String) orderSymmetricalMap.get(rsc1, rsc2);
        if (ret == null) {
            return (String) orderSymmetricalMap.get(rsc2, rsc1);
        }
        return ret;
    }

    /**
     * Returns heartbeat ids of all resource locations.
     */
    public final List<String> getLocationIds(final String rsc) {
        final List<String> locs = locationsIdMap.get(rsc);
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
        final String ret = (String) orderDirectionMap.get(rsc1, rsc2);
        if (ret == null) {
            return (String) orderDirectionMap.get(rsc2, rsc1);
        }
        return ret;
    }

    /**
     * Returns score for resource and host.
     */
    public final String getScore(final String hbId, final String onHost) {
        final Map<String, String> hostToScoreMap = locationMap.get(hbId);
        if (hostToScoreMap != null) {
            return hostToScoreMap.get(onHost);
        }
        return null;
    }

    /**
     * Returns score from location id.
     */
    public final String getLocationScore(final String locationId) {
        return locationScoreMap.get(locationId);
    }

    /**
     * Returns location id for specified resource and host.
     */
    public final String getLocationId(final String rsc, final String node) {
        return (String) resHostToLocIdMap.get(rsc, node);
    }

    /**
     * Returns value of an operation.
     */
    public final String getOperation(final String hbId,
                                     final String op,
                                     final String param) {
        return (String) operationsMap.get(hbId, op, param);
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
        return activeNodes.contains(node);
    }

    /**
     * Parses the command with data.
     */
    private void parseCommand(final String command,
                                    final List<String> data,
                                    final HeartbeatOCF hbOCF) {
        final String[] commands = command.split("<<<>>>");
        final String cmd = commands[0];

        if (commands.length == 1) {
            /*
             * all_nodes, active_nodes, crm_nodes, hb_config, crm_config,
             * dc, all_rsc
             */
            if ("all_nodes".equals(cmd)) {
                // TODO: check if all nodes are in cluster
                for (int i = 0; i < data.size(); i++) {
                    allNodes.add(data.get(i));
                }
            } else if ("active_nodes".equals(cmd)) {
                for (int i = 0; i < data.size(); i++) {
                    activeNodes.add(data.get(i));
                }
            } else if ("crm_nodes".equals(cmd)) {
                for (int i = 0; i < data.size(); i++) {
                    crmNodes.add(data.get(i));
                }
            } else if ("crm_config".equals(cmd) && data.size() == 7) {
                /* heartbeat < 2.1.3 */
                try {
                    final String transTimeout   = data.get(0);
                    final String symCluster     = data.get(1);
                    final String stonithEnabled = data.get(2);
                    final String noQPol         = data.get(3);
                    final String resStickiness  = data.get(4);
                    // TODO: what is that?
                    final String unknown        = data.get(5);
                    final String resFStickiness = data.get(6);

                    globalConfigMap.put("default-action-timeout", transTimeout);
                    globalConfigMap.put("symmetric-cluster", symCluster);
                    globalConfigMap.put("stonith-enabled", stonithEnabled);
                    globalConfigMap.put("no-quorum-policy", noQPol);
                    globalConfigMap.put("default-resource-stickiness",
                                        resStickiness);

                    globalConfigMap.put("default-resource-failure-stickiness",
                                        resFStickiness);
                } catch (IndexOutOfBoundsException e) {
                    final StringBuffer d = new StringBuffer(40);
                    d.append(command);
                    d.append(": ");
                    for (final String line : data) {
                        d.append(line);
                        d.append(' ');
                    }

                    Tools.appError("could not get data from crm_config",
                                   d.toString(),
                                   e);
                    return;
                }
            } else if ("dc".equals(cmd)) {
                dc = data.get(0);
            }
        } else if (commands.length == 2) {
            if ("cib_query".equals(cmd)) {
                /* pacemaker, hb >=2.99, second command is cib and it means
                 * everything */
                parseCibQuery(hbOCF,
                              Tools.join("\n", data.toArray(
                                                   new String[data.size()])));

            } else if ("crm_config".equals(cmd) && data != null && !data.isEmpty()) {
                /* heartbeat >= 2.1.3 */
                globalConfigMap.put(commands[1], data.get(0));

            /*
             * host: node_config, running_rsc
             * rsc:  rsc_type, rsc_status, rsc_running_on, rsc_attrs,
             *       rsc_params
             */
            } else if ("crm_metadata".equals(cmd)) {
                parseCrmMetadata(commands[1], data, hbOCF);
            } else if ("rsc_params".equals(cmd)
                       || "rsc_metaattrs".equals(cmd)) {
                final String hbId = commands[1];
                if (!isGroup(hbId)) {
                    Map<String, String>params = parametersMap.get(hbId);
                    if (params == null) {
                        params = new HashMap<String, String>();
                    }
                    parametersMap.put(hbId, params);
                    final Iterator<String> i = data.iterator();
                    while (i.hasNext()) {
                        final String id = i.next();
                        final String name = i.next();
                        final String value = i.next();
                        params.put(name, value);
                    }
                }
            } else if ("rsc_full_ops".equals(cmd)) {
                final String hbId = commands[1];
                try {
                    final int length = Integer.parseInt(data.get(0));
                    final int size = data.size() - 1;
                    if (length > 0 && size > 0) {
                        final int numberOfOPs = size / length;
                        for (int i = 0; i < numberOfOPs; i++) {
                            final String id         = data.get(i * length + 1);
                            final String name       = data.get(i * length + 2);
                            final String desc       = data.get(i * length + 3);
                            final String interv     = data.get(i * length + 4);
                            final String timeout    = data.get(i * length + 5);
                            final String startDelay = data.get(i * length + 6);
                            final String disabled   = data.get(i * length + 7);
                            final String role       = data.get(i * length + 8);
                            final String prereq     = data.get(i * length + 9);
                            final String onFail     = data.get(i * length + 10);

                            operationsMap.put(hbId, name, "interval", interv);
                            operationsMap.put(hbId, name, "timeout", timeout);
                            operationsMap.put(hbId,
                                              name,
                                              "start-delay",
                                              startDelay);
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    Tools.appError("could not get " + cmd, "", e);
                    return;
                }
            } else if ("rsc_type".equals(cmd)) { // item time: group, native...
                /* find out if this is a group */
                final String hbId = commands[1];
                try {
                    final String itemType = data.get(0);
                    resourceItemTypeMap.put(hbId, itemType);
                } catch (IndexOutOfBoundsException e) {
                    Tools.appError("could not get " + cmd, "", e);
                    return;
                }
            } else if ("rsc_status".equals(cmd)) { //
                /* Find out if this service running, not running or
                 * unmanaged. */
                final String hbId = commands[1];
                try {
                    final String rscStatus = data.get(0);
                    resourceStatusMap.put(hbId, rscStatus);
                } catch (IndexOutOfBoundsException e) {
                    Tools.appError("could not get " + cmd, "", e);
                    return;
                }
            } else if ("rsc_attrs".equals(cmd)) {
                final String hbId = commands[1];
                if (!isGroup(hbId)) {
                    try {
                        final String hbClass = data.get(2);
                        final String type = data.get(4);
                        resourceTypeMap.put(hbId, hbOCF.getHbService(type,
                                                                     hbClass));
                    } catch (IndexOutOfBoundsException e) {
                        Tools.appError("could not get " + cmd, "", e);
                        return;
                    }
                }
            } else if ("sub_rsc".equals(cmd)) {
                final String groupId = commands[1];
                if (!data.isEmpty()) { /* got group */
                    final List<String> rscList = new ArrayList<String>();
                    groupsToResources.put(groupId, rscList);
                    for (final String rsc : data) {
                        rscList.add(rsc);
                        resourceToGroup.put(rsc, groupId);
                    }
                }
            } else if ("rsc_running_on".equals(cmd)) {
                final String hbId = commands[1];
                if (!isGroup(hbId)) {
                    try {
                        if (!data.isEmpty()) {
                            final String onHost = data.get(0);
                            runningOnNodeHash.put(hbId, onHost);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        Tools.appError("could not get " + cmd, "", e);
                        return;
                    }
                }
            }
        } else if (commands.length == 3) {
            /*
             * rsc_location, rsc_order, rsc_colocation
             */
            if ("get_co".equals(commands[0])) {
                final String subCmd = commands[1];
                /* final String hbId = commands[2]; */
                if ("rsc_order".equals(subCmd)) {
                    try {
                        final String ordId = data.get(0);
                        final String from  = data.get(1);
                        final String how   = data.get(2);
                        final String to    = data.get(3);
                        List<String> tos = orderMap.get(from);
                        if (tos == null) {
                            tos = new ArrayList<String>();
                        }
                        if ("before".equals(how)) {
                            tos.add(to);
                        } else if ("after".equals(how)) {
                            tos.add(to); //TODO:
                        }
                        orderMap.put(from, tos);
                        orderDirectionMap.put(from, to, how);
                        orderIdMap.put(from, to, ordId);
                    } catch (IndexOutOfBoundsException e) {
                        Tools.appError("could not get " + cmd, "", e);
                        return;
                    }
                } else if ("rsc_colocation".equals(subCmd)) {
                    try {
                        final String colId = data.get(0);
                        final String rsc1  = data.get(1);
                        final String rsc2  = data.get(2);
                        // TODO: only INFINITY supported
                        final String score = data.get(3);
                        List<String> tos = colocationMap.get(rsc1);
                        if (tos == null) {
                            tos = new ArrayList<String>();
                        }
                        tos.add(rsc2);
                        colocationMap.put(rsc1, tos);
                        colocationScoreMap.put(rsc1, rsc2, score);
                        colocationIdMap.put(rsc1, rsc2, colId);
                    } catch (IndexOutOfBoundsException e) {
                        Tools.appError("could not get " + cmd, "", e);
                        return;
                    }
                } else if ("rsc_location".equals(subCmd)) {
                    try {
                        String locId;
                        String rscId;
                        String score;
                        String expr1;
                        String expr2;
                        String onHost;
                        /* String booleanOp; TODO: ? */
                        if (data.size() == 7) {
                            /* heartbeat < 2.1.3 */
                            locId = data.get(0);
                            rscId = data.get(1);
                            score = data.get(2);
                            // data.get(3); // expr id
                            expr1 = data.get(4);
                            expr2 = data.get(5);
                            onHost = data.get(6);
                        } else if (data.size() == 4) {
                            /* heartbeat 2.1.4, TODO: expresions are ignored.*/
                            locId = data.get(0);
                            rscId = data.get(1);
                            score = data.get(2);
                            expr1 = "#uname"; // TODO
                            expr2 = "eq"; // TODO
                            onHost = data.get(3);
                        } else {
                            locId = data.get(0);
                            rscId = data.get(1);
                            score = data.get(2);
                            //TODO: boolean op not implemented
                            /* booleanOp = data.get(3);  */
                            // data.get(3); // expr id
                            expr1 = data.get(5);
                            expr2 = data.get(6);
                            onHost = data.get(7);
                        }
                        List<String> locs = locationsIdMap.get(rscId);
                        if (locs == null) {
                            locs = new ArrayList<String>();
                            locationsIdMap.put(rscId, locs);
                        }
                        locs.add(locId);
                        if ("#uname".equals(expr1) && "eq".equals(expr2)) {
                            Map<String, String> hostScoreMap =
                                                        locationMap.get(rscId);
                            if (hostScoreMap == null) {
                                hostScoreMap = new HashMap<String, String>();
                                locationMap.put(rscId, hostScoreMap);
                            }
                            hostScoreMap.put(onHost, score);
                            resHostToLocIdMap.put(rscId,
                                                  onHost,
                                                  locId);
                        }
                        locationScoreMap.put(locId, score);
                    } catch (IndexOutOfBoundsException e) {
                        Tools.appError("could not get " + cmd, "", e);
                        return;
                    }
                }
            }
        } else {
            Tools.appError("unknown command: " + command);
        }
    }

    /**
     * Parses status.
     */
    public final void parseStatus(final String status,
                                  final HeartbeatOCF hbOCF) {
        final String[] lines = status.split("\n");
        String command    = null;
        List<String> data = null;
        /* TODO: some of this does not have to be cleared in pacemaker. */
        allNodes.clear();
        activeNodes.clear();
        crmNodes.clear();
        groupsToResources.clear();
        resourceToGroup.clear();
        runningOnNodeHash.clear();
        colocationMap.clear();
        colocationIdMap.clear();
        locationsIdMap.clear();
        colocationScoreMap.clear();
        orderMap.clear();
        orderIdMap.clear();
        orderDirectionMap.clear();
        locationMap.clear();
        resHostToLocIdMap.clear();
        locationScoreMap.clear();
        globalConfigMap.clear();
        resourceTypeMap.clear();
        resourceItemTypeMap.clear();
        resourceStatusMap.clear();
        parametersMap.clear();
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
                    parseCommand(command, data, hbOCF);
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
        final List<String> rscs = new ArrayList<String>();
        groupsToResources.put("none", rscs);
        for (final String rsc : parametersMap.keySet()) {
            if (!resourceToGroup.containsKey(rsc)) {
                rscs.add(rsc);
            }
        }
    }

    /**
     * Parses crm metadata. Only once.
     */
    public final void parseCrmMetadata(final String which,
                                       final List<String> data,
                                       final HeartbeatOCF hbOCF) {
        /* doing this only once per pengine and crmd */
        if (alreadyParsed.contains(which)) {
            return;
        }
        alreadyParsed.add(which);
        data.remove(0); /* <?xml ... */
        /* need only long descs and defaults for advanced options */
        hbOCF.parseCrmMetaData(which,
                               Tools.join("\n",
                                          data.toArray(
                                                    new String[data.size()])));
    }

    /**
     * Parses output from cib_query\ncib command.
     */
    private final void parseCibQuery(final HeartbeatOCF hbOCF,
                                     final String query) {
        CibQuery cibQueryMap = hbOCF.parseCibQuery(query);
        globalConfigMap    = cibQueryMap.getCrmConfig();
        parametersMap      = cibQueryMap.getParameters();
        resourceTypeMap    = cibQueryMap.getResourceType();

        colocationMap      = cibQueryMap.getColocation();
        colocationScoreMap = cibQueryMap.getColocationScore();
        colocationIdMap    = cibQueryMap.getColocationId();

        orderMap          = cibQueryMap.getOrder();
        orderScoreMap     = cibQueryMap.getOrderScore();
        orderSymmetricalMap = cibQueryMap.getOrderSymmetrical();
        orderIdMap        = cibQueryMap.getOrderId();
        orderDirectionMap = cibQueryMap.getOrderDirection(); /* not in pacemaker */

        locationMap       = cibQueryMap.getLocation();
        locationsIdMap    = cibQueryMap.getLocationsId();
        locationScoreMap  = cibQueryMap.getLocationScore();
        resHostToLocIdMap = cibQueryMap.getResHostToLocId();
        operationsMap     = cibQueryMap.getOperations();
    }
}
