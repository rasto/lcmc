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

import java.util.*;

import org.apache.commons.collections15.map.MultiKeyMap;

/**
 * This class holds data that were parsed from cib xml. This is not used in old
 * heartbeats before pacemaker.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class CibQuery {
    /** crm_config / global config data. */
    private Map<String, String> crmConfig = new HashMap<String, String>();
    /** Map with resources and its parameters. */
    private Map<String, Map<String, String>> parameters =
                                    new HashMap<String, Map<String, String>>();
    /** Map with resources and parameters nvpair ids. */
    private Map<String, Map<String, String>> parametersNvpairsIds;
    /** Map with resource type. */
    private Map<String, ResourceAgent> resourceType;
    /** List with orphaned resources. */
    private Set<String> orphanedList;
    /** List with resources in LRM. */
    private Map<String, Set<String>> inLRMList;
    /** Map with resource instance_attributes id. */
    private Map<String, String> resourceInstanceAttrId;
    /** Colocation rsc map. */
    private Map<String, List<CRMXML.ColocationData>> colocationRsc =
                      new LinkedHashMap<String, List<CRMXML.ColocationData>>();
    /** Colocation id map. */
    private Map<String, CRMXML.ColocationData> colocationId =
                      new LinkedHashMap<String, CRMXML.ColocationData>();
    /** Order rsc map. */
    private Map<String, List<CRMXML.OrderData>> orderRsc =
                      new LinkedHashMap<String, List<CRMXML.OrderData>>();
    /** Order id map. */
    private Map<String, CRMXML.OrderData> orderId =
                      new LinkedHashMap<String, CRMXML.OrderData>();
    /** Order id to resource sets map. */
    private Map<String, List<CRMXML.RscSet>> orderIdRscSets =
                           new LinkedHashMap<String, List<CRMXML.RscSet>>();
    /** Colocation id to resource sets map. */
    private Map<String, List<CRMXML.RscSet>> colocationIdRscSets =
                           new LinkedHashMap<String, List<CRMXML.RscSet>>();
    /** All connections between resource sets. */
    private List<CRMXML.RscSetConnectionData> rscSetConnections =
                                  new ArrayList<CRMXML.RscSetConnectionData>();
    /** Node parameters map. */
    private MultiKeyMap<String, String> nodeParameters;
    /** Location map. */
    private Map<String, Map<String, HostLocation>> location =
                              new HashMap<String, Map<String, HostLocation>>();
    /** Ping location map. */
    private Map<String, HostLocation> pingLocation =
                                           new HashMap<String, HostLocation>();
    /** Locations id map. */
    private Map<String, List<String>> locationsId =
                                           new HashMap<String, List<String>>();
    /** Location id to host location map. */
    private Map<String, HostLocation> idToLocation =
                                           new HashMap<String, HostLocation>();
    /** Map from resource and host to location id. */
    private MultiKeyMap<String, String> resHostToLocId =
                                             new MultiKeyMap<String, String>();
    /** Map from resource to location id for ping. */
    private Map<String, String> resPingToLocId = new HashMap<String, String>();
    /** Operations map. */
    private MultiKeyMap<String, Value> operations =
                                            new MultiKeyMap<String, Value>();
    /** Operations refs map. */
    private Map<String, String> operationsRefs = new HashMap<String, String>();
    /** Meta attrs id map. */
    private Map<String, String> metaAttrsId = new HashMap<String, String>();
    /** Metaattrs refs map. */
    private Map<String, String> metaAttrsRefs = new HashMap<String, String>();
    /** Operations id map. */
    private Map<String, String> operationsId = new HashMap<String, String>();
    /** "op" tag id map. */
    private Map<String, Map<String, String>> resOpIds =
                                    new HashMap<String, Map<String, String>>();
    /** If node is online. */
    private Map<String, String> nodeOnline = new HashMap<String, String>();
    /** If node is pending. */
    private Set<String> nodePending = new HashSet<String>();
    /** If node is fenced. */
    private Set<String> fencedNodes = new HashSet<String>();
    /** Group to resources map. */
    private Map<String, List<String>> groupsToResources =
                                           new HashMap<String, List<String>>();
    /** Clone to its resource map. */
    private Map<String, String> cloneToResource =
                                               new HashMap<String, String>();
    /** List of mater resources. */
    private List<String> masterList = new ArrayList<String>();
    /** Designated co-ordinator. */
    private String designatedCoOrdinator = null;
    /** Map from nodename and resource to the fail-count. */
    private MultiKeyMap<String, String> failed =
                                             new MultiKeyMap<String, String>();
    /** Map from rsc id to list of clone ids for failed clones. */
    private MultiKeyMap<String, Set<String>> failedClones =
                                      new MultiKeyMap<String, Set<String>>();
    /** Ping count per node. */
    private Map<String, String> pingCount = new HashMap<String, String>();
    /** rsc_defaults meta attributes id. */
    private String rscDefaultsId = null;
    /** rsc_defaults parameters with values. */
    private Map<String, String> rscDefaultsParams =
                                                 new HashMap<String, String>();
    /** rsc_defaults parameters with ids. */
    private Map<String, String> rscDefaultsParamsNvpairIds =
                                                 new HashMap<String, String>();
    /** op_defaults parameters with values. */
    private Map<String, Value> opDefaultsParams = new HashMap<String, Value>();

    /** Sets crm config map. */
    void setCrmConfig(final Map<String, String> crmConfig) {
        this.crmConfig = crmConfig;
    }

    /** Returns crm config. */
    Map<String, String> getCrmConfig() {
        return crmConfig;
    }

    /**
     * Sets parameters map, with the first key being the resource id and the
     * second key being the parameter name.
     */
    void setParameters(final Map<String, Map<String, String>> parameters) {
        this.parameters = parameters;
    }

    /** Returns the parameters map. */
    Map<String, Map<String, String>> getParameters() {
        return parameters;
    }

    /**
     * Sets the parameters nvpairs id map, with the first key being the
     * resource id and the second key being the parameter name.
     */
    void setParametersNvpairsIds(
                  final Map<String, Map<String, String>> parametersNvpairsIds) {
        this.parametersNvpairsIds = parametersNvpairsIds;
    }

    /** Returns the parameters nvpairs id map. */
    Map<String, Map<String, String>> getParametersNvpairsIds() {
        return parametersNvpairsIds;
    }

    /** Sets the resource type map. */
    void setResourceType(final Map<String, ResourceAgent> resourceType) {
        this.resourceType = resourceType;
    }

    /** Sets the list with orphaned resources. */
    void setOrphaned(final Set<String> orphanedList) {
        this.orphanedList = orphanedList;
    }

    /** Sets the list with resources in LRM. */
    void setInLRM(final Map<String, Set<String>> inLRMList) {
        this.inLRMList = inLRMList;
    }

    /** Returns the resource type map. */
    Map<String, ResourceAgent> getResourceType() {
        return resourceType;
    }

    /** Returns list with orphaned resources. */
    Collection<String> getOrphaned() {
        return orphanedList;
    }

    /** Returns list with resources in LRM. */
    Map<String, Set<String>> getInLRM() {
        return inLRMList;
    }

    /** Sets the resource instance_attributes id map. */
    void setResourceInstanceAttrId(
                            final Map<String, String> resourceInstanceAttrId) {
        this.resourceInstanceAttrId = resourceInstanceAttrId;
    }

    /** Returns the resource instance_attributes map. */
    Map<String, String> getResourceInstanceAttrId() {
        return resourceInstanceAttrId;
    }


    /**
     * Sets the colocation map with one resource as a key and list of
     * colocation constraints.
     */
    void setColocationRsc(
              final Map<String, List<CRMXML.ColocationData>> colocationRsc) {
        this.colocationRsc = colocationRsc;
    }

    /**
     * Sets the colocation map with resource id as a key with colocation data
     * object.
     */
    void setColocationId(
                  final Map<String, CRMXML.ColocationData> colocationId) {
        this.colocationId = colocationId;
    }

    /** Returns colocation id map. */
    Map<String, CRMXML.ColocationData> getColocationId() {
        return colocationId;
    }

    /** Returns colocation rsc map. */
    Map<String, List<CRMXML.ColocationData>> getColocationRsc() {
        return colocationRsc;
    }

    /**
     * Sets the colocation map with one resource as a key and list of
     * colocation constraints.
     */
    void setOrderRsc(
              final Map<String, List<CRMXML.OrderData>> orderRsc) {
        this.orderRsc = orderRsc;
    }

    /**
     * Sets the order map with resource id as a key with order data
     * object.
     */
    void setOrderId(final Map<String, CRMXML.OrderData> orderId) {
        this.orderId = orderId;
    }

    /** Sets the order map with resource id as a key with resource sets. */
    void setOrderIdRscSets(
                       final Map<String, List<CRMXML.RscSet>> orderIdRscSets) {
        this.orderIdRscSets = orderIdRscSets;
    }

    /** Sets the order map with resource id as a key with resource sets. */
    void setColocationIdRscSets(
                  final Map<String, List<CRMXML.RscSet>> colocationIdRscSets) {
        this.colocationIdRscSets = colocationIdRscSets;
    }

    /** Returns rscSetConnections. */
    List<CRMXML.RscSetConnectionData> getRscSetConnections() {
        return rscSetConnections;
    }

    /** Sets rscSetConnections. */
    void setRscSetConnections(
                   final List<CRMXML.RscSetConnectionData> rscSetConnections) {
        this.rscSetConnections = rscSetConnections;
    }

    /** Returns id rsc map. */
    Map<String, CRMXML.OrderData> getOrderId() {
        return orderId;
    }

    /** Returns order rsc map. */
    Map<String, List<CRMXML.OrderData>> getOrderRsc() {
        return orderRsc;
    }

    /** Returns colocation id rsc to resource set map. */
    Map<String, List<CRMXML.RscSet>> getColocationIdRscSets() {
        return colocationIdRscSets;
    }

    /** Returns order id rsc to resource set map. */
    Map<String, List<CRMXML.RscSet>> getOrderIdRscSets() {
        return orderIdRscSets;
    }

    /** Sets node parameters map. */
    void setNodeParameters(final MultiKeyMap<String, String> nodeParameters) {
        this.nodeParameters = nodeParameters;
    }

    /** Gets node parameters map. */
    MultiKeyMap<String, String> getNodeParameters() {
        return nodeParameters;
    }

    /** Sets location map. */
    void setLocation(final Map<String, Map<String, HostLocation>> location) {
        this.location = location;
    }

    /** Returns location map. */
    Map<String, Map<String, HostLocation>> getLocation() {
        return location;
    }

    /** Sets ping location map. */
    void setPingLocation(final Map<String, HostLocation> pingLocation) {
        this.pingLocation = pingLocation;
    }

    /** Returns ping location map. */
    Map<String, HostLocation> getPingLocation() {
        return pingLocation;
    }

    /** Sets locations id map. */
    void setLocationsId(final Map<String, List<String>> locationsId) {
        this.locationsId = locationsId;
    }

    /** Returns locations id map. */
    Map<String, List<String>> getLocationsId() {
        return locationsId;
    }

    /** Sets map from location id to the score. */
    void setLocationMap(final Map<String, HostLocation> idToLocation) {
        this.idToLocation = idToLocation;
    }

    /** Returns map from location id to the score. */
    Map<String, HostLocation> getLocationMap() {
        return idToLocation;
    }

    /** Sets map from resource and host to the location id. */
    void setResHostToLocId(final MultiKeyMap<String, String> resHostToLocId) {
        this.resHostToLocId = resHostToLocId;
    }

    /** Returns map from resource and host to the location id. */
    MultiKeyMap<String, String> getResHostToLocId() {
        return resHostToLocId;
    }

    /** Sets map from resource to the location id for ping. */
    void setResPingToLocId(final Map<String, String> resPingToLocId) {
        this.resPingToLocId = resPingToLocId;
    }

    /** Returns map from resource to the location id for ping. */
    Map<String, String> getResPingToLocId() {
        return resPingToLocId;
    }


    /** Sets operations map. */
    void setOperations(final MultiKeyMap<String, Value> operations) {
        this.operations = operations;
    }

    /** Sets operations refs map. */
    void setOperationsRefs(final Map<String, String> operationsRefs) {
        this.operationsRefs = operationsRefs;
    }

    /** Sets meta attrs refs map. */
    void setMetaAttrsRefs(final Map<String, String> metaAttrsRefs) {
        this.metaAttrsRefs = metaAttrsRefs;
    }

    /** Sets meta attrs id map. */
    void setMetaAttrsId(final Map<String, String> metaAttrsId) {
        this.metaAttrsId = metaAttrsId;
    }

    /** Returns meta attrs id map. */
    Map<String, String> getMetaAttrsId() {
        return metaAttrsId;
    }

    /** Returns meta attrs refs map. */
    Map<String, String> getMetaAttrsRefs() {
        return metaAttrsRefs;
    }


    /** Returns operations map. */
    MultiKeyMap<String, Value> getOperations() {
        return operations;
    }

    /** Returns operations refs map. */
    Map<String, String> getOperationsRefs() {
        return operationsRefs;
    }

    /** Sets operations id map. */
    void setOperationsId(final Map<String, String> operationsId) {
        this.operationsId = operationsId;
    }

    /** Returns operations id map. */
    Map<String, String> getOperationsId() {
        return operationsId;
    }

    /** Sets "op" tag id map. */
    void setResOpIds(final Map<String, Map<String, String>> resOpIds) {
        this.resOpIds = resOpIds;
    }

    /** Returns "op" tag id map. */
    Map<String, Map<String, String>> getResOpIds() {
        return resOpIds;
    }

    /** Sets map with nodes and if they ore online. */
    void setNodeOnline(final Map<String, String> nodeOnline) {
        this.nodeOnline = nodeOnline;
    }

    /** Gets map whith nodes and if they are online. */
    Map<String, String> getNodeOnline() {
        return nodeOnline;
    }

    /** Sets map with nodes that are pending. */
    void setNodePending(final Set<String> nodePending) {
        this.nodePending = nodePending;
    }

    /** Gets map whith nodes that are pending. */
    Collection<String> getNodePending() {
        return nodePending;
    }

    /** Sets map with nodes that are fenced. */
    void setFencedNodes(final Set<String> fencedNodes) {
        this.fencedNodes = fencedNodes;
    }

    /** Gets map whith nodes that are fenced. */
    Collection<String> getFencedNodes() {
        return fencedNodes;
    }

    /** Sets the groups to resources map. */
    void setGroupsToResources(
                           final Map<String, List<String>> groupsToResources) {
        this.groupsToResources = groupsToResources;
    }

    /** Gets the groups to resources map. */
    Map<String, List<String>> getGroupsToResources() {
        return groupsToResources;
    }

    /** Sets the clone to resource map. */
    void setCloneToResource(final Map<String, String> cloneToResource) {
        this.cloneToResource = cloneToResource;
    }

    /** Gets the clone to resource map. */
    Map<String, String> getCloneToResource() {
        return cloneToResource;
    }

    /** Sets the master list. */
    void setMasterList(final List<String> masterList) {
        this.masterList = masterList;
    }

    /** Gets the master list. */
    Collection<String> getMasterList() {
        return masterList;
    }


    /** Sets the designated co-ordinator. */
    void setDC(final String dc) {
        designatedCoOrdinator = dc;
    }

    /** Gets the designated co-ordinator. */
    String getDC() {
        return designatedCoOrdinator;
    }

    /** Sets failed map. */
    void setFailed(final MultiKeyMap<String, String> failed) {
        this.failed = failed;
    }

    /** Returns failed map. */
    MultiKeyMap<String, String> getFailed() {
        return failed;
    }

    /** Sets node ping map. */
    void setPingCount(final Map<String, String> pingCount) {
        this.pingCount = pingCount;
    }

    /** Returns ping count map. */
    Map<String, String> getPingCount() {
        return pingCount;
    }

    /** Sets failed clone map. */
    void setFailedClones(final MultiKeyMap<String, Set<String>> failedClones) {
        this.failedClones = failedClones;
    }

    /** Returns failed clone map. */
    MultiKeyMap<String, Set<String>> getFailedClones() {
        return failedClones;
    }

    /** Returns fail-count. It can be "INFINITY" */
    String getFailCount(final String node, final String res) {
        return failed.get(node, res);
    }

    /** Returns ping count. */
    String getPingCount(final String node) {
        return pingCount.get(node);
    }

    /** Sets rsc_defaults meta attributes id. */
    void setRscDefaultsId(final String rscDefaultsId) {
        this.rscDefaultsId = rscDefaultsId;
    }

    /** Gets rsc_defaults meta attributes id. */
    String getRscDefaultsId() {
        return rscDefaultsId;
    }

    /** Sets rsc_defaults parameters with values. */
    void setRscDefaultsParams(final Map<String, String> rscDefaultsParams) {
        this.rscDefaultsParams = rscDefaultsParams;
    }

    /** Gets rsc_defaults parameters with values. */
    Map<String, String> getRscDefaultsParams() {
        return rscDefaultsParams;
    }

    /** Sets rsc_defaults parameters with ids. */
    void setRscDefaultsParamsNvpairIds(
                        final Map<String, String> rscDefaultsParamsNvpairIds) {
        this.rscDefaultsParamsNvpairIds = rscDefaultsParamsNvpairIds;
    }

    /** Gets rsc_defaults parameters with ids. */
    Map<String, String> getRscDefaultsParamsNvpairIds() {
        return rscDefaultsParamsNvpairIds;
    }

    /** Sets op_defaults parameters with values. */
    void setOpDefaultsParams(final Map<String, Value> opDefaultsParams) {
        this.opDefaultsParams = opDefaultsParams;
    }

    /** Gets op_defaults parameters with values. */
    Map<String, Value> getOpDefaultsParams() {
        return opDefaultsParams;
    }
}
