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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.apache.commons.collections.map.MultiKeyMap;

/**
 * This class holds data that were parsed from cib xml. This is not used in old
 * heartbeats before pacemaker.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class CibQuery {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** crm_config / global config data. */
    private Map<String, String> crmConfig = new HashMap<String, String>();
    /** Map with resources and its parameters. */
    private Map<String, Map<String, String>> parameters =
                                    new HashMap<String, Map<String, String>>();
    /** Map with resources and parameters nvpair ids. */
    private Map<String, Map<String, String>> parametersNvpairsIds;
    /** Map with resource type. */
    private Map<String, HeartbeatService> resourceType;
    /** Map with resource instance_attributes id. */
    private Map<String, String> resourceInstanceAttrId;
    /** Colocation map. */
    private Map<String, List<String>> colocation =
                                           new HashMap<String, List<String>>();
    /** Colocation score map. */
    private MultiKeyMap colocationScore;
    /** Colocation id map. */
    private MultiKeyMap colocationId;
    /** Order map. */
    private Map<String, List<String>> order =
                                           new HashMap<String, List<String>>();
    /** Order score map. */
    private MultiKeyMap orderScore;
    /** if order is symmetrical map. */
    private MultiKeyMap orderSymmetrical;
    /** Order id map. */
    private MultiKeyMap orderId;
    /** Order direction map. */
    private MultiKeyMap orderDirection;
    /** Location map. */
    private Map<String, Map<String, String>> location =
                                    new HashMap<String, Map<String, String>>();
    /** Locations id map. */
    private Map<String, List<String>> locationsId =
                                           new HashMap<String, List<String>>();
    /** Location id to score map. */
    private Map<String, String> locationScore =
                                           new HashMap<String, String>();
    /** Map from resource and host to location id. */
    private MultiKeyMap resHostToLocId = new MultiKeyMap();
    /** Operations map. */
    private MultiKeyMap operations = new MultiKeyMap();
    /** Operations id map. */
    private Map<String, String> operationsId = new HashMap<String, String>();
    /** <op> tag id map. */
    private Map<String, Map<String, String>> resOpIds =
                                    new HashMap<String, Map<String, String>>();
    /** List of active nodes. */
    private Set<String> activeNodes= new HashSet<String>();
    /** Group to resources map. */
    private Map<String, List<String>> groupsToResources =
                                           new HashMap<String, List<String>>();
    /** Designated co-ordinator. */
    private String dc = null;

    /**
     * Sets crm config map.
     */
    public final void setCrmConfig(final Map<String, String> crmConfig) {
        this.crmConfig = crmConfig;
    }

    /**
     * Returns crm config.
     */
    public final Map<String,String> getCrmConfig() {
        return crmConfig;
    }

    /**
     * Sets parameters map, with the first key being the resource id and the
     * second key being the parameter name.
     */
    public final void setParameters(
                           final Map<String, Map<String, String>> parameters) {
        this.parameters = parameters;
    }

    /**
     * Returns the parameters map.
     */
    public final Map<String ,Map<String, String>> getParameters() {
        return parameters;
    }

    /**
     * Sets the parameters nvpairs id map, with the first key being the
     * resource id and the second key being the parameter name.
     */
    public final void setParametersNvpairsIds(
                       final Map<String, Map<String, String>> parametersNvpairsIds) {
        this.parametersNvpairsIds = parametersNvpairsIds;
    }

    /**
     * Returns the parameters nvpairs id map.
     */
    public final Map<String ,Map<String, String>> getParametersNvpairsIds() {
        return parametersNvpairsIds;
    }

    /**
     * Sets the resource type map.
     */
    public final void setResourceType(
                            final Map<String, HeartbeatService> resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * Returns the resource type map.
     */
    public final Map<String, HeartbeatService> getResourceType() {
        return resourceType;
    }


    /**
     * Sets the resource instance_attributes id map.
     */
    public final void setResourceInstanceAttrId(
                            final Map<String, String> resourceInstanceAttrId) {
        this.resourceInstanceAttrId = resourceInstanceAttrId;
    }

    /**
     * Returns the resource instance_attributes map.
     */
    public final Map<String, String> getResourceInstanceAttrId() {
        return resourceInstanceAttrId;
    }


    /**
     * Sets the colocation map with one resource as a key and list of resources
     * with which it is colocated.
     */
    public final void setColocation(
                                final Map<String, List<String>> colocation) {
        this.colocation = colocation;
    }

    /**
     * Returns colocation map.
     */
    public final Map<String, List<String>> getColocation() {
        return colocation;
    }

    /**
     * Sets colocation score map.
     */
    public final void setColocationScore(final MultiKeyMap colocationScore) {
        this.colocationScore = colocationScore;
    }

    /**
     * Returns colocation score map.
     */
    public final MultiKeyMap getColocationScore() {
        return colocationScore;
    }

    /**
     * Sets colocation id map.
     */
    public final void setColocationId(final MultiKeyMap colocationId) {
        this.colocationId = colocationId;
    }

    /**
     * Returns colocation id map.
     */
    public final MultiKeyMap getColocationId() {
        return colocationId;
    }

    /**
     * Sets order map.
     */
    public final void setOrder(final Map<String, List<String>> order) {
        this.order = order;
    }

    /**
     * Returns order map.
     */
    public final Map<String, List<String>> getOrder() {
        return order;
    }

    /**
     * Sets order id map.
     */
    public final void setOrderId(final MultiKeyMap orderId) {
        this.orderId = orderId;
    }

    /**
     * Returns order id map.
     */
    public final MultiKeyMap getOrderId() {
        return orderId;
    }

    /**
     * Sets order score map.
     */
    public final void setOrderScore(final MultiKeyMap orderScore) {
        this.orderScore = orderScore;
    }

    /**
     * Returns order score map.
     */
    public final MultiKeyMap getOrderScore() {
        return orderScore;
    }

    /**
     * Sets if order is symmetrical map.
     */
    public final void setOrderSymmetrical(final MultiKeyMap orderSymmetrical) {
        this.orderSymmetrical = orderSymmetrical;
    }

    /**
     * Returns whether order is symmetrical.
     */
    public final MultiKeyMap getOrderSymmetrical() {
        return orderSymmetrical;
    }

    /**
     * Sets order direction map.
     */
    public final void setOrderDirection(final MultiKeyMap orderDirection) {
        this.orderDirection = orderDirection;
    }

    /**
     * Returns order direction map.
     */
    public final MultiKeyMap getOrderDirection() {
        return orderDirection;
    }

    /**
     * Sets location map.
     */
    public final void setLocation(final Map<String, Map<String, String>> location) {
        this.location = location;
    }

    /**
     * Returns location map.
     */
    public final Map<String, Map<String, String>> getLocation() {
        return location;
    }

    /**
     * Sets locations id map.
     */
    public final void setLocationsId(final Map<String, List<String>> locationsId) {
        this.locationsId = locationsId;
    }

    /**
     * Returns locations id map.
     */
    public final Map<String, List<String>> getLocationsId() {
        return locationsId;
    }

    /**
     * Sets map from location id to the score.
     */
    public final void setLocationScore(final Map<String, String> locationScore) {
        this.locationScore = locationScore;
    }

    /**
     * Returns map from location id to the score.
     */
    public final Map<String, String> getLocationScore() {
        return locationScore;
    }

    /**
     * Sets map from resource and host to the location id.
     */
    public final void setResHostToLocId(final MultiKeyMap resHostToLocId) {
        this.resHostToLocId = resHostToLocId;
    }

    /**
     * Returns map from resource and host to the location id.
     */
    public final MultiKeyMap getResHostToLocId() {
        return resHostToLocId;
    }

    /**
     * Sets operations map.
     */
    public final void setOperations(final MultiKeyMap operations) {
        this.operations = operations;
    }

    /**
     * Returns operations map.
     */
    public final MultiKeyMap getOperations() {
        return operations;
    }

    /**
     * Sets operations id map.
     */
    public final void setOperationsId(final Map<String, String> operationsId) {
        this.operationsId = operationsId;
    }

    /**
     * Returns operations id map.
     */
    public final Map<String, String> getOperationsId() {
        return operationsId;
    }

    /**
     * Sets <op> tag id map.
     */
    public final void setResOpIds(final Map<String, Map<String, String>> resOpIds) {
        this.resOpIds = resOpIds;
    }

    /**
     * Returns <op> tag id map.
     */
    public final Map<String, Map<String, String>> getResOpIds() {
        return resOpIds;
    }

    /**
     * Sets active nodes.
     */
    public final void setActiveNodes(final Set<String> activeNodes) {
        this.activeNodes = activeNodes;
    }

    /**
     * Gets active nodes.
     */
    public final Set<String> getActiveNodes() {
        return activeNodes;
    }

    /**
     * Sets the groups to resources map.
     */
    public final void setGroupsToResources(
                           final Map<String, List<String>> groupsToResources) {
        this.groupsToResources = groupsToResources;
    }

    /**
     * Gets the groups to resources map.
     */
    public final Map<String, List<String>> getGroupsToResources() {
        return groupsToResources;
    }

    /**
     * Sets the designated co-ordinator.
     */
    public final void setDC(final String dc) {
        this.dc = dc;
    }

    /**
     * Gets the designated co-ordinator.
     */
    public final String getDC() {
        return dc;
    }
}
