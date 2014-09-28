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

package lcmc.crm.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lcmc.common.domain.Value;
import org.apache.commons.collections15.map.MultiKeyMap;

/**
 * This class holds data that were parsed from cib xml. This is not used in old
 * heartbeats before pacemaker.
 */
final class CibQuery {
    private Map<String, String> crmConfig = new HashMap<String, String>();
    private Map<String, Map<String, String>> resourceParameters = new HashMap<String, Map<String, String>>();
    private Map<String, Map<String, String>> resourceParametersNvpairsIds;
    private Map<String, ResourceAgent> resourceType;
    private Set<String> orphanedResourcesList;
    private Map<String, Set<String>> resourcesInLRMList;
    private Map<String, String> resourceInstanceAttrId;
    private Map<String, List<CrmXml.ColocationData>> colocationRsc =
                                                          new LinkedHashMap<String, List<CrmXml.ColocationData>>();
    private Map<String, CrmXml.ColocationData> colocationId = new LinkedHashMap<String, CrmXml.ColocationData>();
    private Map<String, List<CrmXml.OrderData>> orderRsc = new LinkedHashMap<String, List<CrmXml.OrderData>>();
    private Map<String, CrmXml.OrderData> orderId = new LinkedHashMap<String, CrmXml.OrderData>();
    private Map<String, List<CrmXml.RscSet>> orderIdRscSets = new LinkedHashMap<String, List<CrmXml.RscSet>>();
    private Map<String, List<CrmXml.RscSet>> colocationIdRscSets = new LinkedHashMap<String, List<CrmXml.RscSet>>();
    private List<CrmXml.RscSetConnectionData> rscSetConnections = new ArrayList<CrmXml.RscSetConnectionData>();
    private MultiKeyMap<String, String> nodeParameters;
    private Map<String, Map<String, HostLocation>> locations = new HashMap<String, Map<String, HostLocation>>();
    private Map<String, HostLocation> pingLocations = new HashMap<String, HostLocation>();
    private Map<String, List<String>> locationsId = new HashMap<String, List<String>>();
    private Map<String, HostLocation> idToLocation = new HashMap<String, HostLocation>();
    private MultiKeyMap<String, String> resHostToLocId = new MultiKeyMap<String, String>();
    private Map<String, String> resPingToLocId = new HashMap<String, String>();
    private MultiKeyMap<String, Value> operations = new MultiKeyMap<String, Value>();
    private Map<String, String> operationsRefs = new HashMap<String, String>();
    private Map<String, String> metaAttrsId = new HashMap<String, String>();
    private Map<String, String> metaAttrsRefs = new HashMap<String, String>();
    private Map<String, String> operationsId = new HashMap<String, String>();
    private Map<String, Map<String, String>> resOpIds = new HashMap<String, Map<String, String>>();
    private Map<String, String> nodeOnline = new HashMap<String, String>();
    private Set<String> nodePending = new HashSet<String>();
    private Set<String> fencedNodes = new HashSet<String>();
    private Map<String, List<String>> groupsToResources = new HashMap<String, List<String>>();
    private Map<String, String> cloneToResource = new HashMap<String, String>();
    private List<String> masterList = new ArrayList<String>();
    private String designatedCoOrdinator = null;
    private MultiKeyMap<String, String> nodeFailedCount = new MultiKeyMap<String, String>();
    /** Map from rsc id to list of clone ids for failed clones. */
    private MultiKeyMap<String, Set<String>> resourceFailedCloneIds = new MultiKeyMap<String, Set<String>>();
    private Map<String, String> nodePingCount = new HashMap<String, String>();
    private String rscDefaultsId = null;
    private Map<String, String> rscDefaultsParams = new HashMap<String, String>();
    private Map<String, String> rscDefaultsParamsNvpairIds = new HashMap<String, String>();
    private Map<String, Value> opDefaultsParams = new HashMap<String, Value>();

    void setCrmConfig(final Map<String, String> crmConfig) {
        this.crmConfig = crmConfig;
    }

    Map<String, String> getCrmConfig() {
        return crmConfig;
    }

    /**
     * Sets parameters map, with the first key being the resource id and the
     * second key being the parameter name.
     */
    void setResourceParameters(final Map<String, Map<String, String>> resourceParameters) {
        this.resourceParameters = resourceParameters;
    }

    Map<String, Map<String, String>> getResourceParameters() {
        return resourceParameters;
    }

    /**
     * Sets the parameters nvpairs id map, with the first key being the
     * resource id and the second key being the parameter name.
     */
    void setResourceParametersNvpairsIds(final Map<String, Map<String, String>> resourceParametersNvpairsIds) {
        this.resourceParametersNvpairsIds = resourceParametersNvpairsIds;
    }

    Map<String, Map<String, String>> getResourceParametersNvpairsIds() {
        return resourceParametersNvpairsIds;
    }

    void setResourceType(final Map<String, ResourceAgent> resourceType) {
        this.resourceType = resourceType;
    }

    void setOrphaned(final Set<String> orphanedList) {
        this.orphanedResourcesList = orphanedList;
    }

    void setInLRM(final Map<String, Set<String>> inLRMList) {
        this.resourcesInLRMList = inLRMList;
    }

    Map<String, ResourceAgent> getResourceType() {
        return resourceType;
    }

    Collection<String> getOrphaned() {
        return orphanedResourcesList;
    }

    Map<String, Set<String>> getInLRM() {
        return resourcesInLRMList;
    }

    void setResourceInstanceAttrId(final Map<String, String> resourceInstanceAttrId) {
        this.resourceInstanceAttrId = resourceInstanceAttrId;
    }

    Map<String, String> getResourceInstanceAttrId() {
        return resourceInstanceAttrId;
    }

    /**
     * Sets the colocation map with one resource as a key and list of
     * colocation constraints.
     */
    void setColocationRsc(final Map<String, List<CrmXml.ColocationData>> colocationRsc) {
        this.colocationRsc = colocationRsc;
    }

    /**
     * Sets the colocation map with resource id as a key with colocation data
     * object.
     */
    void setColocationId(final Map<String, CrmXml.ColocationData> colocationId) {
        this.colocationId = colocationId;
    }

    Map<String, CrmXml.ColocationData> getColocationId() {
        return colocationId;
    }

    Map<String, List<CrmXml.ColocationData>> getColocationRsc() {
        return colocationRsc;
    }

    /**
     * Sets the colocation map with one resource as a key and list of
     * colocation constraints.
     */
    void setOrderRsc(final Map<String, List<CrmXml.OrderData>> orderRsc) {
        this.orderRsc = orderRsc;
    }

    /**
     * Sets the order map with resource id as a key with order data
     * object.
     */
    void setOrderId(final Map<String, CrmXml.OrderData> orderId) {
        this.orderId = orderId;
    }

    void setOrderIdRscSets(final Map<String, List<CrmXml.RscSet>> orderIdRscSets) {
        this.orderIdRscSets = orderIdRscSets;
    }

    void setColocationIdRscSets(final Map<String, List<CrmXml.RscSet>> colocationIdRscSets) {
        this.colocationIdRscSets = colocationIdRscSets;
    }

    List<CrmXml.RscSetConnectionData> getRscSetConnections() {
        return rscSetConnections;
    }

    void setRscSetConnections(final List<CrmXml.RscSetConnectionData> rscSetConnections) {
        this.rscSetConnections = rscSetConnections;
    }

    Map<String, CrmXml.OrderData> getOrderId() {
        return orderId;
    }

    Map<String, List<CrmXml.OrderData>> getOrderRsc() {
        return orderRsc;
    }

    Map<String, List<CrmXml.RscSet>> getColocationIdRscSets() {
        return colocationIdRscSets;
    }

    Map<String, List<CrmXml.RscSet>> getOrderIdRscSets() {
        return orderIdRscSets;
    }

    void setNodeParameters(final MultiKeyMap<String, String> nodeParameters) {
        this.nodeParameters = nodeParameters;
    }

    MultiKeyMap<String, String> getNodeParameters() {
        return nodeParameters;
    }

    void setLocations(final Map<String, Map<String, HostLocation>> locations) {
        this.locations = locations;
    }

    Map<String, Map<String, HostLocation>> getLocations() {
        return locations;
    }

    void setPingLocations(final Map<String, HostLocation> pingLocations) {
        this.pingLocations = pingLocations;
    }

    Map<String, HostLocation> getPingLocations() {
        return pingLocations;
    }

    void setLocationsId(final Map<String, List<String>> locationsId) {
        this.locationsId = locationsId;
    }

    Map<String, List<String>> getLocationsId() {
        return locationsId;
    }

    void setLocationMap(final Map<String, HostLocation> idToLocation) {
        this.idToLocation = idToLocation;
    }

    Map<String, HostLocation> getLocationMap() {
        return idToLocation;
    }

    void setResHostToLocId(final MultiKeyMap<String, String> resHostToLocId) {
        this.resHostToLocId = resHostToLocId;
    }

    MultiKeyMap<String, String> getResHostToLocId() {
        return resHostToLocId;
    }

    void setResPingToLocId(final Map<String, String> resPingToLocId) {
        this.resPingToLocId = resPingToLocId;
    }

    Map<String, String> getResPingToLocId() {
        return resPingToLocId;
    }

    void setOperations(final MultiKeyMap<String, Value> operations) {
        this.operations = operations;
    }

    void setOperationsRefs(final Map<String, String> operationsRefs) {
        this.operationsRefs = operationsRefs;
    }

    void setMetaAttrsRefs(final Map<String, String> metaAttrsRefs) {
        this.metaAttrsRefs = metaAttrsRefs;
    }

    void setMetaAttrsId(final Map<String, String> metaAttrsId) {
        this.metaAttrsId = metaAttrsId;
    }

    Map<String, String> getMetaAttrsId() {
        return metaAttrsId;
    }

    Map<String, String> getMetaAttrsRefs() {
        return metaAttrsRefs;
    }

    MultiKeyMap<String, Value> getOperations() {
        return operations;
    }

    Map<String, String> getOperationsRefs() {
        return operationsRefs;
    }

    void setOperationsId(final Map<String, String> operationsId) {
        this.operationsId = operationsId;
    }

    Map<String, String> getOperationsId() {
        return operationsId;
    }

    void setResOpIds(final Map<String, Map<String, String>> resOpIds) {
        this.resOpIds = resOpIds;
    }

    Map<String, Map<String, String>> getResOpIds() {
        return resOpIds;
    }

    void setNodeOnline(final Map<String, String> nodeOnline) {
        this.nodeOnline = nodeOnline;
    }

    Map<String, String> getNodeOnline() {
        return nodeOnline;
    }

    void setNodePending(final Set<String> nodePending) {
        this.nodePending = nodePending;
    }

    Collection<String> getNodePending() {
        return nodePending;
    }

    void setFencedNodes(final Set<String> fencedNodes) {
        this.fencedNodes = fencedNodes;
    }

    Collection<String> getFencedNodes() {
        return fencedNodes;
    }

    void setGroupsToResources(final Map<String, List<String>> groupsToResources) {
        this.groupsToResources = groupsToResources;
    }

    Map<String, List<String>> getGroupsToResources() {
        return groupsToResources;
    }

    void setCloneToResource(final Map<String, String> cloneToResource) {
        this.cloneToResource = cloneToResource;
    }

    Map<String, String> getCloneToResource() {
        return cloneToResource;
    }

    void setMasterList(final List<String> masterList) {
        this.masterList = masterList;
    }

    Collection<String> getMasterList() {
        return masterList;
    }

    void setDC(final String dc) {
        designatedCoOrdinator = dc;
    }

    String getDC() {
        return designatedCoOrdinator;
    }

    void setNodeFailedCount(final MultiKeyMap<String, String> nodeFailedCount) {
        this.nodeFailedCount = nodeFailedCount;
    }

    MultiKeyMap<String, String> getNodeFailedCount() {
        return nodeFailedCount;
    }

    void setNodePingCount(final Map<String, String> nodePingCount) {
        this.nodePingCount = nodePingCount;
    }

    Map<String, String> getNodePingCount() {
        return nodePingCount;
    }

    void setResourceFailedCloneIds(final MultiKeyMap<String, Set<String>> resourceFailedCloneIds) {
        this.resourceFailedCloneIds = resourceFailedCloneIds;
    }

    MultiKeyMap<String, Set<String>> getResourceFailedCloneIds() {
        return resourceFailedCloneIds;
    }

    String getFailCount(final String node, final String res) {
        return nodeFailedCount.get(node, res);
    }

    String getPingCount(final String node) {
        return nodePingCount.get(node);
    }

    void setRscDefaultsId(final String rscDefaultsId) {
        this.rscDefaultsId = rscDefaultsId;
    }

    String getRscDefaultsId() {
        return rscDefaultsId;
    }

    void setRscDefaultsParams(final Map<String, String> rscDefaultsParams) {
        this.rscDefaultsParams = rscDefaultsParams;
    }

    Map<String, String> getRscDefaultsParams() {
        return rscDefaultsParams;
    }

    void setRscDefaultsParamsNvpairIds(final Map<String, String> rscDefaultsParamsNvpairIds) {
        this.rscDefaultsParamsNvpairIds = rscDefaultsParamsNvpairIds;
    }

    Map<String, String> getRscDefaultsParamsNvpairIds() {
        return rscDefaultsParamsNvpairIds;
    }

    void setOpDefaultsParams(final Map<String, Value> opDefaultsParams) {
        this.opDefaultsParams = opDefaultsParams;
    }

    Map<String, Value> getOpDefaultsParams() {
        return opDefaultsParams;
    }
}
