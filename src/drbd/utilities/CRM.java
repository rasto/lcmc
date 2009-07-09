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

package drbd.utilities;

import drbd.data.Host;
import java.util.Map;
import java.util.HashMap;
/**
 * This class provides cib commands. There are commands that use cibadmin and
 * crm_resource commands to manipulate the cib, crm, etc.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class CRM {
    /**
     * No instantiation.
     */
    private CRM() { }

    /**
     * Returns cibadmin command.
     */
    public static String getCibCommand(final String command,
                                       final String objType,
                                       final String xml) {
        return "/usr/sbin/cibadmin --obj_type " + objType + " "
                                   + command
                                   + " -X " + xml;
    }

    /**
     * Executes specified command on the host.
     */
    private static void execCommand(final Host host,
                                    final String command,
                                    final boolean outputVisible) {
        Tools.execCommandProgressIndicator(
                                host,
                                command,
                                null,
                                outputVisible,
                                Tools.getString("CIB.ExecutingCommand"));
    }

    /**
     * Adds or updates resource in the CIB.
     */
    public static void setParameters(
                           final Host host,
                           final String command, /* -U or -C */
                           final String heartbeatId,
                           final String groupId,
                           final String args,
                           final Map<String, String> pacemakerResAttrs,
                           final Map<String, String> pacemakerResArgs,
                           final Map<String, String> pacemakerMetaArgs,
                           String instanceAttrId,
                           final Map<String, String> nvpairIdsHash,
                           final Map<String, Map<String, String>> pacemakerOps,
                           String operationsId) {
        if (args == null) {
            return;
        }

        if (instanceAttrId == null) {
            instanceAttrId = heartbeatId + "-instance_attributes";
        }
        final StringBuffer attrsString = new StringBuffer(100);
        for (final String attrName : pacemakerResAttrs.keySet()) {
            final String value = pacemakerResAttrs.get(attrName);
            attrsString.append(attrName);
            attrsString.append("=\"");
            attrsString.append(value);
            attrsString.append("\" ");
        }
        
        final StringBuffer xml = new StringBuffer(360);
        xml.append('\'');
        if (groupId != null) {
            xml.append("<group id=\"");
            xml.append(groupId);
            xml.append("\">");
        }
        xml.append("<primitive ");
        xml.append(attrsString);
        xml.append(">");
        /* instance_attributes */
        if (!pacemakerResArgs.isEmpty()) {
            xml.append("<instance_attributes id=\"");
            xml.append(instanceAttrId);
            xml.append("\">");
            final String hbVersion = host.getHeartbeatVersion();
            if (Tools.compareVersions(hbVersion, "2.99.0") < 0) {
                /* 2.1.4 */
                xml.append("<attributes>");
            }

            for (final String paramName : pacemakerResArgs.keySet()) {
                final String value = pacemakerResArgs.get(paramName);
                String nvpairId = null;
                if (nvpairIdsHash != null) {
                    nvpairId = nvpairIdsHash.get(paramName);
                }
                if (nvpairId == null) {
                    nvpairId = "nvpair-"
                               + heartbeatId
                               + "-"
                               + paramName;
                }
                xml.append("<nvpair id=\"");
                xml.append(nvpairId);
                xml.append("\" name=\"");
                xml.append(paramName);
                xml.append("\" value=\"");
                xml.append(value);
                xml.append("\"/>");
            }
            if (Tools.compareVersions(hbVersion, "2.99.0") < 0) {
                /* 2.1.4 */
                xml.append("</attributes>");
            }
            xml.append("</instance_attributes>");
        }
        /* operations */
        if (!pacemakerOps.isEmpty()) {

            final String hbVersion = host.getHeartbeatVersion();
            if (Tools.compareVersions(hbVersion, "2.99.0") >= 0) {
                // 2.1.4 does not have the id.
                if (operationsId == null) {
                    operationsId = heartbeatId + "-operations";
                }
                xml.append("<operations id=\"");
                xml.append(operationsId);
                xml.append("\">");
            } else {
                xml.append("<operations>");
            }
            for (final String op : pacemakerOps.keySet()) {
                Map<String, String> opHash = pacemakerOps.get(op);
                xml.append("<op");
                for (final String name : opHash.keySet()) {
                    final String value = opHash.get(name);
                    xml.append(" ");
                    xml.append(name);
                    xml.append("=\"");
                    xml.append(value);
                    xml.append("\"");
                }
                xml.append("/>");
            }
            xml.append("</operations>");
        }
        /* meta_attributes */
        if (!pacemakerMetaArgs.isEmpty()) {
            xml.append(getMetaAttributes(host,
                                         heartbeatId,
                                         pacemakerMetaArgs));
        }
        xml.append("</primitive>");
        if (groupId != null) {
            xml.append("</group>");
        }
        xml.append('\'');

        execCommand(host,
                    getCibCommand(command,
                                  "resources",
                                  xml.toString()),
                    true);
    }

    /**
     * Adds group to the cib.
     */
    public static void addGroup(final Host host,
                                final String args) {
        if (args == null) {
            /* does nothing, group is added with the first resource. */
            return;
        }
    }

    /**
     * Sets colocation and order constraint between service with heartbeatId
     * and parents.
     */
    public static void setOrderAndColocation(final Host host,
                                             final String heartbeatId,
                                             final String[] parents) {
        for (int i = 0; i < parents.length; i++) {
            addColocation(host, heartbeatId, parents[i]);
            addOrder(host, parents[i], heartbeatId);
        }
    }

    /**
     * Sets location constraint.
     */
    public static void setLocation(final Host host,
                                   final String heartbeatId,
                                   final String onHost,
                                   final String score,
                                   String locationId) {
        String command = "-U";
        if (locationId == null) {
            locationId = "loc_" + heartbeatId + "_" + onHost;
            command = "-C";
        }
 
        final StringBuffer xml = new StringBuffer(360);
        xml.append("'<rsc_location id=\"");
        xml.append(locationId);
        xml.append("\" rsc=\"");
        xml.append(heartbeatId);
        xml.append("\" node=\"");
        xml.append(onHost);
        if (score != null) {
            xml.append("\" score=\"");
            xml.append(score);
        }
        xml.append("\"/>'");
        execCommand(host,
                    getCibCommand(command,
                                  "constraints",
                                  xml.toString()),
                    true);
    }

    /**
     * Removes location constraint.
     */
    public static void removeLocation(final Host host,
                                      final String locationId,
                                      final String heartbeatId,
                                      final String score) {
        final StringBuffer xml = new StringBuffer(360);
        xml.append("'<rsc_location id=\"");
        xml.append(locationId);
        xml.append("\" rsc=\"");
        xml.append(heartbeatId);
        if (score != null) {
            xml.append("\" score=\"");
            xml.append(score);
        }
        xml.append("\"/>'");
        final String command = getCibCommand("-D",
                                             "constraints",
                                             xml.toString()); 
        execCommand(host, command, true);
    }


    /**
     * Removes a resource from crm. If heartbeat id is null and group id is
     * not, the whole group will be removed.
     */
    public static void removeResource(final Host host,
                                      final String heartbeatId,
                                      final String groupId) {
        final StringBuffer xml = new StringBuffer(360);
        xml.append('\'');
        if (groupId != null) {
            /* when removing the last resource in a group, remove the
             * whole group. */
            xml.append("<group id=\"");
            xml.append(groupId);
            xml.append("\">");
        }
        if (heartbeatId != null) {
            xml.append("<primitive id=\"");
            xml.append(heartbeatId);
            xml.append("\">");
            xml.append("</primitive>");
        }
        if (groupId != null) {
            xml.append("</group>");
        }
        xml.append('\'');
        final String command = getCibCommand(
                                      "-D",
                                      "resources",
                                      xml.toString()); 
        execCommand(host, command, true);
    }

    /**
     * Cleans up resource in crm.
     */
    public static void cleanupResource(final Host host,
                                       final String heartbeatId,
                                       final Host[] clusterHosts) {
        /* make cleanup on all cluster hosts. */
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", heartbeatId);
        for (Host clusterHost : clusterHosts) {
            replaceHash.put("@HOST@", clusterHost.getName());
            final String command =
                      host.getDistCommand("CRM.cleanupResource",
                                          replaceHash);
            execCommand(host, command, true);
        }
    }

    /**
     * Starts resource.
     */
    public static void startResource(final Host host,
                                     final String heartbeatId) {
        final String hbVersion = host.getHeartbeatVersion();
        String cmd = "CRM.startResource";
        if (Tools.compareVersions(hbVersion, "2.99.0") < 0) {
            cmd = "CRM.2.1.4.startResource";
        }
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", heartbeatId);
        final String command = host.getDistCommand(cmd, replaceHash); 
        execCommand(host, command, true);
    }

    private static String getMetaAttributes(final Host host,
                                           final String heartbeatId,
                                           final Map<String, String> attrs) {
        StringBuffer xml = new StringBuffer(360);
        xml.append("<meta_attributes id=\"");
        xml.append(heartbeatId);
        xml.append("_meta_attrs\">");

        final String hbVersion = host.getHeartbeatVersion();
        if (Tools.compareVersions(hbVersion, "2.99.0") < 0) {
            /* 2.1.4 */
            xml.append("<attributes>");
        }
        for (final String attr : attrs.keySet()) {
            xml.append("<nvpair id=\"");
            xml.append(heartbeatId);
            xml.append("-meta-options-");
            xml.append(attr);
            xml.append("\" name=\"");
            xml.append(attr);
            xml.append("\" value=\"");
            xml.append(attrs.get(attr));
            xml.append("\"/>");
        }

        if (Tools.compareVersions(hbVersion, "2.99.0") < 0) {
            /* 2.1.4 */
            xml.append("</attributes>");
        }
        xml.append("</meta_attributes>");
        return xml.toString();
    }

    /**
     * Sets whether the service should be managed or not.
     */
    public static void setManaged(final Host host,
                                  final String heartbeatId,
                                  final boolean isManaged) {
        String string;
        if (isManaged) {
            string = ".isManagedOn";
        } else {
            string = ".isManagedOff";
        }
        final String hbVersion = host.getHeartbeatVersion();
        String cmd = "CRM" + string;
        if (Tools.compareVersions(hbVersion, "2.99.0") < 0) {
            cmd = "CRM.2.1.4" + string;
        }
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", heartbeatId);

        final String command = host.getDistCommand(cmd, replaceHash);
        execCommand(host, command, true);
    }

    /**
     * Stops resource.
     */
    public static void stopResource(final Host host,
                                    final String heartbeatId) {
        final String hbVersion = host.getHeartbeatVersion();
        String cmd = "CRM.stopResource";
        if (Tools.compareVersions(hbVersion, "2.99.0") < 0) {
            cmd = "CRM.2.1.4.stopResource";
        }
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", heartbeatId);

        final String command = host.getDistCommand(cmd, replaceHash);
        execCommand(host, command, true);
    }

    /**
     * Migrates resource to the specified host.
     */
    public static void migrateResource(final Host host,
                                       final String heartbeatId,
                                       final String onHost) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", heartbeatId);
        replaceHash.put("@HOST@", onHost);
        final String command = host.getDistCommand("CRM.migrateResource",
                                                   replaceHash);

        execCommand(host, command, true);
    }

    /**
     * Unmigrates resource that was previously migrated.
     */
    public static void unmigrateResource(final Host host,
                                         final String heartbeatId) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", heartbeatId);
        final String command = host.getDistCommand(
                                             "CRM.unmigrateResource",
                                             replaceHash);
        execCommand(host, command, true);
    }

    /**
     * Sets global heartbeat parameters.
     */
    public static void setGlobalParameters(final Host host,
                                           final Map<String,String> args) {
        final StringBuffer xml = new StringBuffer(360);
        xml.append("'<crm_config><cluster_property_set id=\"cib-bootstrap-options\">");
        final String hbVersion = host.getHeartbeatVersion();
        if (Tools.compareVersions(hbVersion, "2.99.0") < 0) {
            /* 2.1.4 */
            xml.append("<attributes>");
        }
        for (String arg : args.keySet()) {
            String id = "cib-bootstrap-options-" + arg;
            xml.append("<nvpair id=\"");
            xml.append(id);
            xml.append("\" name=\"");
            xml.append(arg);
            xml.append("\" value=\"");
            xml.append(args.get(arg));
            xml.append("\"/>");
        }
        if (Tools.compareVersions(hbVersion, "2.99.0") < 0) {
            /* 2.1.4 */
            xml.append("</attributes>");
        }
        xml.append("</cluster_property_set></crm_config>'");
        final String command = getCibCommand("-U",
                                             "crm_config",
                                             xml.toString()); 
        execCommand(host, command, true);
    }

    /**
     * Removes colocation with specified colocation id.
     */
    public static void removeColocation(final Host host,
                                        final String colocationId,
                                        final String rsc1,
                                        final String rsc2,
                                        final String score) {
        final String hbVersion = host.getHeartbeatVersion();
        String rscString = "rsc";
        String withRscString = "with-rsc";
        if (Tools.compareVersions(hbVersion, "2.99.0") < 0) {
            /* <= 2.1.4 */
            rscString = "from";
            withRscString = "to";
        }
        final StringBuffer xml = new StringBuffer(360);
        xml.append("'<rsc_colocation id=\"");
        xml.append(colocationId);
        xml.append("\" " + rscString + "=\"");
        xml.append(rsc1);
        if (score != null) {
            xml.append("\" score=\"");
            xml.append(score);
        }
        xml.append("\" " + withRscString + "=\"");
        xml.append(rsc2);
        xml.append("\"/>'");
        final String command = getCibCommand("-D",
                                             "constraints",
                                             xml.toString()); 
        execCommand(host, command, true);
    }

    /**
     * Adds colocation between service with heartbeatId and parentHbId.
     */
    public static void addColocation(final Host host,
                                     final String heartbeatId,
                                     final String parentHbId) {
        String colocationId;
        if (parentHbId.compareTo(heartbeatId) < 0) {
            colocationId = "col_" + heartbeatId + "_" + parentHbId;
        } else {
            colocationId = "col_" + parentHbId + "_" + heartbeatId;
        }
        final String score = "INFINITY";
        String rscString = "rsc";
        String withRscString = "with-rsc";
        final String hbVersion = host.getHeartbeatVersion();
        if (Tools.compareVersions(hbVersion, "2.99.0") < 0) {
            /* <= 2.1.4 */
            rscString = "from";
            withRscString = "to";
        }
        final StringBuffer xml = new StringBuffer(360);
        xml.append("'<rsc_colocation id=\"");
        xml.append(colocationId);
        xml.append("\" " + rscString + "=\"");
        xml.append(heartbeatId);
        if (score != null) {
            xml.append("\" score=\"");
            xml.append(score);
        }
        xml.append("\" " + withRscString + "=\"");
        xml.append(parentHbId);
        xml.append("\"/>'");
        final String command = getCibCommand("-C", 
                                             "constraints",
                                             xml.toString()); 
        execCommand(host, command, true);
    }

    /**
     * Removes order constraint with specified order id.
     */
    public static void removeOrder(final Host host,
                                   final String orderId,
                                   final String rscFrom,
                                   final String rscTo,
                                   final String score,
                                   final String symmetrical) {
        final String hbVersion = host.getHeartbeatVersion();
        String firstString = "first";
        String thenString = "then";
        if (Tools.compareVersions(hbVersion, "2.99.0") < 0) {
            /* <= 2.1.4 */
            firstString = "from";
            thenString = "to";
        }
        final StringBuffer xml = new StringBuffer(360);
        xml.append("'<rsc_order id=\"");
        xml.append(orderId);
        xml.append("\" " + firstString + "=\"");
        xml.append(rscFrom);
        if (score != null) {
            xml.append("\" score=\"");
            xml.append(score);
        }
        if (symmetrical != null) {
            xml.append("\" symmetrical=\"");
            xml.append(symmetrical);
        }
        xml.append("\" " + thenString + "=\"");
        xml.append(rscTo);
        xml.append("\"/>'");
        final String command = getCibCommand("-D",
                                             "constraints",
                                             xml.toString()); 
        execCommand(host, command, true);
    }

    /**
     * Adds order constraint.
     */
    public static void addOrder(final Host host,
                                final String parentHbId,
                                final String heartbeatId) {
        final String orderId = "ord_" + parentHbId + "_" + heartbeatId;
        final String score = "INFINITY";
        final String symmetrical = null; // TODO:
        final String hbVersion = host.getHeartbeatVersion();
        String firstString = "first";
        String thenString = "then";
        if (Tools.compareVersions(hbVersion, "2.99.0") < 0) {
            /* <= 2.1.4 */
            firstString = "from";
            thenString = "to";
        }
        final StringBuffer xml = new StringBuffer(360);
        xml.append("'<rsc_order id=\"");
        xml.append(orderId);
        xml.append("\" " + firstString + "=\"");
        xml.append(parentHbId);
        if (score != null) {
            xml.append("\" score=\"");
            xml.append(score);
        }
        if (symmetrical != null) {
            xml.append("\" symmetrical=\"");
            xml.append(symmetrical);
        }
        xml.append("\" " + thenString + "=\"");
        xml.append(heartbeatId);
        if (Tools.compareVersions(hbVersion, "2.99.0") < 0) {
            /* <= 2.1.4 */
            final String type = "before"; //TODO: can be after
            xml.append("\" type=\"");
            xml.append(type);
        }
        xml.append("\"/>'");
        final String command = getCibCommand("-C",
                                             "constraints",
                                             xml.toString()); 
        execCommand(host, command, true);
    }

    /**
     * Moves resource up in a group.
     */
    public static void moveGroupResUp(final Host host,
                                      final String heartbeatId) {
        // TODO: not implemented
        Tools.appError("not implemented");
    }

    /**
     * Moves resource down in a group.
     */
    public static void moveGroupResDown(final Host host,
                                        final String heartbeatId) {
        // TODO: not implemented
        Tools.appError("not implemented");
    }

    /**
     * Makes heartbeat stand by.
     */
    public static void standByOn(final Host host) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@HOST@", host.getName());
        final String command = host.getDistCommand("CRM.standByOn",
                                                   replaceHash);
        execCommand(host, command, true);
    }

    /**
     * Undoes heartbeat stand by.
     */
    public static void standByOff(final Host host) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@HOST@", host.getName());
        final String command = host.getDistCommand("CRM.standByOff",
                                                   replaceHash);
        execCommand(host, command, true);
    }
}
