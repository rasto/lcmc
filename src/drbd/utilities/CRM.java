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
import java.util.LinkedHashMap;
import java.util.List;
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
                           final String command, /* -R or -C */
                           final String heartbeatId,
                           final String cloneId,
                           final boolean master,
                           final Map<String, String> cloneMetaArgs,
                           final String groupId,
                           final Map<String, String> pacemakerResAttrs,
                           final Map<String, String> pacemakerResArgs,
                           final Map<String, String> pacemakerMetaArgs,
                           String instanceAttrId,
                           final Map<String, String> nvpairIdsHash,
                           final Map<String, Map<String, String>> pacemakerOps,
                           String operationsId) {
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
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        if (cloneId != null) {
            if (master) {
                if (pmV == null
                    && hbV != null
                    && Tools.compareVersions(hbV, "2.99.0") < 0) {
                    xml.append("<master_slave id=\"");
                } else {
                    xml.append("<master id=\"");
                }
            } else {
                xml.append("<clone id=\"");
            }
            xml.append(cloneId);
            xml.append("\">");
            /* mater/slave meta_attributes */
            if (!cloneMetaArgs.isEmpty()) {
                xml.append(getMetaAttributes(host,
                                             cloneId,
                                             cloneMetaArgs));
            }
        }
        if (groupId != null) {
            xml.append("<group id=\"");
            xml.append(groupId);
            xml.append("\">");
        }
        xml.append("<primitive ");
        xml.append(attrsString);
        xml.append('>');
        /* instance_attributes */
        if (!pacemakerResArgs.isEmpty()) {
            xml.append("<instance_attributes id=\"");
            xml.append(instanceAttrId);
            xml.append("\">");
            if (pmV == null
                && hbV != null
                && Tools.compareVersions(hbV, "2.99.0") < 0) {
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
            if (pmV == null
                && hbV != null
                && Tools.compareVersions(hbV, "2.99.0") < 0) {
                /* 2.1.4 */
                xml.append("</attributes>");
            }
            xml.append("</instance_attributes>");
        }
        /* operations */
        if (!pacemakerOps.isEmpty()) {
            if (pmV != null
                || hbV == null
                || Tools.compareVersions(hbV, "2.99.0") >= 0) {
                /* 2.1.4 does not have the id. */
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
                final Map<String, String> opHash = pacemakerOps.get(op);
                xml.append("<op");
                for (final String name : opHash.keySet()) {
                    final String value = opHash.get(name);
                    xml.append(' ');
                    xml.append(name);
                    xml.append("=\"");
                    xml.append(value);
                    xml.append('"');
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
        if (cloneId != null) {
            if (master) {
                if (pmV == null
                    && hbV != null
                    && Tools.compareVersions(hbV, "2.99.0") < 0) {
                    xml.append("</master_slave>");
                } else {
                    xml.append("</master>");
                }
            } else {
                xml.append("</clone>");
            }
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
    public static void setOrderAndColocation(
                                final Host host,
                                final String heartbeatId,
                                final String[] parents,
                                final List<Map<String, String>> colAttrsList,
                                final List<Map<String, String>> ordAttrsList) {
        for (int i = 0; i < parents.length; i++) {
            addColocation(host,
                          null,
                          heartbeatId,
                          parents[i],
                          colAttrsList.get(i));
            addOrder(host, null, parents[i], heartbeatId, ordAttrsList.get(i));
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
                                      final String groupId,
                                      final String cloneId,
                                      final boolean master) {
        final StringBuffer xml = new StringBuffer(360);
        xml.append('\'');
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        if (cloneId != null) {
            if (master) {
                if (pmV == null
                    && hbV != null
                    && Tools.compareVersions(hbV, "2.99.0") < 0) {
                    xml.append("<master_slave id=\"");
                } else {
                    xml.append("<master id=\"");
                }
            } else {
                xml.append("<clone id=\"");
            }
            xml.append(cloneId);
            xml.append("\">");
        }
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
            xml.append("\"></primitive>");
        }
        if (groupId != null) {
            xml.append("</group>");
        }
        if (cloneId != null) {
            if (master) {
                if (pmV == null
                    && hbV != null
                    && Tools.compareVersions(hbV, "2.99.0") < 0) {
                    xml.append("</master_slave>");
                } else {
                    xml.append("</master>");
                }
            } else {
                xml.append("</clone>");
            }
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
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        String cmd = "CRM.startResource";
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
            cmd = "CRM.2.1.4.startResource";
        }
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", heartbeatId);
        final String command = host.getDistCommand(cmd, replaceHash);
        execCommand(host, command, true);
    }

    /**
     * Returns meta attributes xml.
     */
    private static String getMetaAttributes(final Host host,
                                           final String heartbeatId,
                                           final Map<String, String> attrs) {
        final StringBuffer xml = new StringBuffer(360);
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        String idPostfix = "-meta_attributes";
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.1.4") <= 0) {
            idPostfix = "-meta-options";
        }
        xml.append("<meta_attributes id=\"");
        xml.append(heartbeatId);
        xml.append(idPostfix);
        xml.append("\">");

        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
            /* 2.1.4 */
            xml.append("<attributes>");
        }
        for (final String attr : attrs.keySet()) {
            xml.append("<nvpair id=\"");
            xml.append(heartbeatId);
            xml.append(idPostfix);
            xml.append("-");
            xml.append(attr);
            xml.append("\" name=\"");
            xml.append(attr);
            xml.append("\" value=\"");
            xml.append(attrs.get(attr));
            xml.append("\"/>");
        }

        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
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
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        String cmd = "CRM" + string;
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
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
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        String cmd = "CRM.stopResource";
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
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
                                           final Map<String, String> args) {
        final StringBuffer xml = new StringBuffer(360);
        xml.append("'<crm_config>");
        xml.append("<cluster_property_set id=\"cib-bootstrap-options\">");
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
            /* 2.1.4 */
            xml.append("<attributes>");
        }
        for (String arg : args.keySet()) {
            final String id = "cib-bootstrap-options-" + arg;
            xml.append("<nvpair id=\"");
            xml.append(id);
            xml.append("\" name=\"");
            xml.append(arg);
            xml.append("\" value=\"");
            xml.append(args.get(arg));
            xml.append("\"/>");
        }
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
            /* 2.1.4 */
            xml.append("</attributes>");
        }
        xml.append("</cluster_property_set></crm_config>'");
        final String command = getCibCommand("-R",
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
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        String rscString = "rsc";
        String withRscString = "with-rsc";
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
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
                                     final String colId,
                                     final String heartbeatId,
                                     final String parentHbId,
                                     Map<String, String> attrs) {
        String colocationId;
        String cibadminOpt;
        if (colId == null) {
            cibadminOpt = "-C"; /* create */
            if (parentHbId.compareTo(heartbeatId) < 0) {
                colocationId = "col_" + heartbeatId + "_" + parentHbId;
            } else {
                colocationId = "col_" + parentHbId + "_" + heartbeatId;
            }
        } else {
            cibadminOpt = "-R"; /* replace */
            colocationId = colId;
        }
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        if (attrs == null) {
            attrs = new LinkedHashMap<String, String>();
        }
        attrs.put("rsc", heartbeatId);
        attrs.put("with-rsc", parentHbId);
        final StringBuffer xml = new StringBuffer(360);
        xml.append("'<rsc_colocation id=\"");
        xml.append(colocationId);
        final Map<String, String> convertHash = new HashMap<String, String>();
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
            /* <= 2.1.4 */
            convertHash.put("rsc", "from");
            convertHash.put("with-rsc", "to");
            convertHash.put("rsc-role", "from_role");
            convertHash.put("with-rsc-role", "to_role");
        }
        for (String attr : attrs.keySet()) {
            final String value = attrs.get(attr);
            if ("".equals(value)) {
                continue;
            }
            if (convertHash.containsKey(attr)) {
                attr = convertHash.get(attr);
            }
            xml.append("\" " + attr + "=\"");
            xml.append(value);
        }
        xml.append("\"/>'");
        final String command = getCibCommand(cibadminOpt,
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
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        String firstString = "first";
        String thenString = "then";
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
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
                                final String ordId,
                                final String parentHbId,
                                final String heartbeatId,
                                Map<String, String> attrs) {
        String orderId;
        String cibadminOpt;
        if (ordId == null) {
            cibadminOpt = "-C"; /* replace */
            orderId = "ord_" + parentHbId + "_" + heartbeatId;
        } else {
            cibadminOpt = "-R"; /* replace */
            orderId = ordId;
        }
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        if (attrs == null) {
            attrs = new LinkedHashMap<String, String>();
        }
        String firstString = "first";
        String thenString = "then";
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
            /* <= 2.1.4 */
            firstString = "from";
            thenString = "to";
        }
        final StringBuffer xml = new StringBuffer(360);
        xml.append("'<rsc_order id=\"");
        xml.append(orderId);
        attrs.put("first", parentHbId);
        attrs.put("then", heartbeatId);
        final Map<String, String> convertHash = new HashMap<String, String>();
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
            /* <= 2.1.4 */
            final String type = "before"; //TODO: can be after
            attrs.put("type", type);
            convertHash.put("first", "from");
            convertHash.put("then", "to");
            convertHash.put("first-action", "action");
            convertHash.put("then-action", "to_action");
        }
        for (String attr : attrs.keySet()) {
            final String value = attrs.get(attr);
            if ("".equals(value)) {
                continue;
            }
            if (convertHash.containsKey(attr)) {
                attr = convertHash.get(attr);
            }
            xml.append("\" " + attr + "=\"");
            xml.append(value);
        }
        xml.append("\"/>'");
        final String command = getCibCommand(cibadminOpt,
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
