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
import drbd.data.HostLocation;
import drbd.data.CRMXML;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import EDU.oswego.cs.dl.util.concurrent.Mutex;
/**
 * This class provides cib commands. There are commands that use cibadmin and
 * crm_resource commands to manipulate the cib, crm, etc.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class CRM {
    /** Output of the ptest. */
    private static volatile String ptestOutput = null;
    /** Ptest lock. */
    private static final Mutex mPtestLock = new Mutex();
    /** Delimiter that delimits the ptest and test cib part. */
    public static final String PTEST_END_DELIM = "--- PTEST END ---";
    /**
     * No instantiation.
     */
    private CRM() {
        /* empty */
    }

    /**
     * Returns cibadmin command.
     */
    public static String getCibCommand(final String command,
                                       final String objType,
                                       final String xml) {
        final StringBuffer cmd = new StringBuffer(300);
        cmd.append("/usr/sbin/cibadmin --obj_type ");
        cmd.append(objType);
        cmd.append(' ');
        cmd.append(command);
        cmd.append(" -X ");
        cmd.append(xml);
        return cmd.toString();
    }

    /**
     * Executes specified command on the host.
     */
    private static SSH.SSHOutput execCommand(final Host host,
                                             final String command,
                                             final boolean outputVisible,
                                             final boolean testOnly) {
        try {
            mPtestLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ptestOutput = null;
        mPtestLock.release();
        if (testOnly) {
            final String testCmd =
                 "export file=/var/lib/heartbeat/drbd-mc-test.xml;"
                 + "if [ ! -e $file ]; then /usr/sbin/cibadmin -Ql > $file;fi;"
                 + "export CIB_file=$file; ";
            final SSH.SSHOutput out = Tools.execCommand(
                                                 host,
                                                 testCmd + command,
                                                 null,
                                                 false,
                                                 SSH.DEFAULT_COMMAND_TIMEOUT);
            return out;
        } else {
            Tools.debug(null, "CRM.java: crm command: " + command, 1);
            return Tools.execCommandProgressIndicator(
                                    host,
                                    command,
                                    null,
                                    outputVisible,
                                    Tools.getString("CIB.ExecutingCommand"),
                                    SSH.DEFAULT_COMMAND_TIMEOUT);
        }
    }

    /**
     * Executes the ptest command and returns results.
     */
    public static String getPtest(final Host host) {
        try {
            mPtestLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (ptestOutput != null) {
            final String po = ptestOutput;
            mPtestLock.release();
            return po;
        }
        mPtestLock.release();
        final String command =
                            "export file=/var/lib/heartbeat/drbd-mc-test.xml;"
                            + "/usr/sbin/ptest -VVV -S -x $file 2>&1;echo '"
                            + PTEST_END_DELIM
                            + "';cat $file 2>/dev/null;"
                            + "mv -f $file{,.last} 2>/dev/null";
        final SSH.SSHOutput output = Tools.execCommand(
                                                host,
                                                command,
                                                null,
                                                false,
                                                SSH.DEFAULT_COMMAND_TIMEOUT);
        try {
            mPtestLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final String po = output.getOutput();
        if (ptestOutput == null) {
            ptestOutput = po;
        }
        mPtestLock.release();
        return po;
    }

    /**
     * Adds or updates resource in the CIB.
     */
    public static boolean setParameters(
                           final Host host,
                           final String command, /* -R or -C */
                           final String heartbeatId,
                           final String cloneId,
                           final boolean master,
                           final Map<String, String> cloneMetaArgs,
                           final Map<String, String> groupMetaArgs,
                           final String groupId,
                           final Map<String, String> pacemakerResAttrs,
                           final Map<String, String> pacemakerResArgs,
                           final Map<String, String> pacemakerMetaArgs,
                           String instanceAttrId,
                           final Map<String, String> nvpairIdsHash,
                           final Map<String, Map<String, String>> pacemakerOps,
                           String operationsId,
                           final String metaAttrsRefId,
                           final String cloneMetaAttrsRefId,
                           final String groupMetaAttrsRefId,
                           final String operationsRefId,
                           final boolean stonith,
                           final boolean testOnly) {

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
            //if (!cloneMetaArgs.isEmpty()) {
            xml.append(getMetaAttributes(host,
                                         cloneId,
                                         cloneMetaArgs,
                                         cloneMetaAttrsRefId));
            //}
        }
        if (groupId != null) {
            if (heartbeatId != null) {
                xml.append("<group id=\"");
                xml.append(groupId);
                xml.append("\">");
            }
            xml.append(getMetaAttributes(host,
                                         groupId,
                                         groupMetaArgs,
                                         groupMetaAttrsRefId));
        }
        if (heartbeatId != null) {
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
                    String newParamName;
                    if (stonith
                        && CRMXML.STONITH_PRIORITY_INSTANCE_ATTR.equals(
                                                                  paramName)) {
                        newParamName = "priority";
                    } else {
                        newParamName = paramName;
                    }
                    if (nvpairId == null) {
                        nvpairId = "nvpair-"
                                   + heartbeatId
                                   + "-"
                                   + newParamName;
                    }
                    xml.append("<nvpair id=\"");
                    xml.append(nvpairId);
                    xml.append("\" name=\"");
                    xml.append(newParamName);
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
        }
        /* operations */
        if (operationsRefId != null) {
            xml.append("<operations id-ref=\"");
            xml.append(operationsRefId);
            xml.append("\"/>");
        } else if (pacemakerOps != null && !pacemakerOps.isEmpty()) {
            //TODO: not "else if" but update the referred service.
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
        if (heartbeatId != null) {
            xml.append(getMetaAttributes(host,
                                         heartbeatId,
                                         pacemakerMetaArgs,
                                         metaAttrsRefId));
            xml.append("</primitive>");
        }
        if (groupId != null && heartbeatId != null) {
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

        final SSH.SSHOutput ret = execCommand(host,
                                              getCibCommand(command,
                                                            "resources",
                                                            xml.toString()),
                                              true,
                                              testOnly);
        return ret.getExitCode() == 0;
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
     * and parents. If colAttrsList or ordAttrsList are null, there is only
     * colocation or order but not both defined.
     */
    public static void setOrderAndColocation(
                                final Host host,
                                final String heartbeatId,
                                final String[] parents,
                                final List<Map<String, String>> colAttrsList,
                                final List<Map<String, String>> ordAttrsList,
                                final boolean testOnly) {
        for (int i = 0; i < parents.length; i++) {
            if (colAttrsList.get(i) != null) {
                addColocation(host,
                              null,
                              heartbeatId,
                              parents[i],
                              colAttrsList.get(i),
                              testOnly);
            }
            if (ordAttrsList.get(i) != null) {
                addOrder(host,
                         null,
                         parents[i],
                         heartbeatId,
                         ordAttrsList.get(i),
                         testOnly);
            }
        }
    }

    /** Returns one resource set xml. */
    private static String getOneRscSet(final String rscSetId,
                                       final CRMXML.RscSet rscSet,
                                       Map<String, String> attrs) {
        final StringBuffer xml = new StringBuffer(120);
        xml.append("<resource_set id=\"");
        xml.append(rscSetId);
        if (attrs == null) {
            attrs = new LinkedHashMap<String, String>();
            final String colocationRole = rscSet.getColocationRole();
            if (colocationRole != null) {
                attrs.put("role", colocationRole);
            }
            final String orderAction = rscSet.getOrderAction();
            if (orderAction != null) {
                attrs.put("action", orderAction);
            }
            final String sequential = rscSet.getSequential();
            if (sequential != null) {
                attrs.put("sequential", sequential);
            }
        }
        for (final String attr : attrs.keySet()) {
            final String value = attrs.get(attr);
            if ("".equals(value)) {
                continue;
            }
            xml.append("\" " + attr + "=\"");
            xml.append(value);
        }
        xml.append("\">");
        for (final String rscId : rscSet.getRscIds()) {
            xml.append("<resource_ref id=\"");
            xml.append(rscId);
            xml.append("\"/>");
        }
        xml.append("</resource_set>");
        return xml.toString();
    }

    /** Sets resource set. */
    public static boolean setRscSet(final Host host,
                                    final String colId,
                                    final boolean createCol,
                                    final String ordId,
                                    final boolean createOrd,
                                    final Map<
                                           CRMXML.RscSet,
                                           Map<String, String>> rscSetsColAttrs,
                                    final Map<
                                           CRMXML.RscSet,
                                           Map<String, String>> rscSetsOrdAttrs,
                                    final Map<String, String> attrs,
                                    final boolean testOnly) {
        if (colId != null) {
            if (rscSetsColAttrs.isEmpty()) {
                return removeColocation(host, colId, testOnly);
            }
            String cibadminOpt;
            if (createCol) {
                cibadminOpt = "-C";
            } else {
                cibadminOpt = "-R";
            }
            final boolean ret = setRscSetConstraint(host,
                                                    "rsc_colocation",
                                                    colId,
                                                    rscSetsColAttrs,
                                                    attrs,
                                                    cibadminOpt,
                                                    testOnly);
            if (!ret) {
                return false;
            }
        }
        if (ordId != null) {
            if (rscSetsOrdAttrs.isEmpty()) {
                return removeOrder(host, ordId, testOnly);
            }
            String cibadminOpt;
            if (createOrd) {
                cibadminOpt = "-C";
            } else {
                cibadminOpt = "-R";
            }
            return setRscSetConstraint(host,
                                       "rsc_order",
                                       ordId,
                                       rscSetsOrdAttrs,
                                       attrs,
                                       cibadminOpt,
                                       testOnly);
        }
        return true;
    }

    /** Sets resource set that is either colocation or order. */
    private static boolean setRscSetConstraint(
                    final Host host,
                    final String tag,
                    final String constraintId,
                    final Map<CRMXML.RscSet, Map<String, String>> rscSetsAttrs,
                    final Map<String, String> attrs,
                    final String cibadminOpt,
                    final boolean testOnly) {
        final StringBuffer xml = new StringBuffer(360);
        xml.append("'<");
        xml.append(tag);
        xml.append(" id=\"");
        xml.append(constraintId);
        if (attrs != null) {
            for (final String attr : attrs.keySet()) {
                final String value = attrs.get(attr);
                if (value == null || "".equals(value)) {
                    continue;
                }
                xml.append("\" " + attr + "=\"");
                xml.append(value);
            }
        }
        xml.append("\">");
        int rsId = 0;
        for (final CRMXML.RscSet rscSet : rscSetsAttrs.keySet()) {
            if (rscSet != null) {
                xml.append(getOneRscSet(constraintId + "-" + rsId,
                                        rscSet,
                                        rscSetsAttrs.get(rscSet)));
                rsId++;
            }
        }
        xml.append("</");
        xml.append(tag);
        xml.append(">'");

        final String command = getCibCommand(cibadminOpt,
                                             "constraints",
                                             xml.toString());
        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Returns xml for location xml.
     */
    private static String getLocationXML(final String heartbeatId,
                                         final String onHost,
                                         final String score,
                                         final String op,
                                         final String locationId) {
        final StringBuffer xml = new StringBuffer(360);
        xml.append("'<rsc_location id=\"");
        xml.append(locationId);
        xml.append("\" rsc=\"");
        xml.append(heartbeatId);
        if (op == null || "eq".equals(op)) {
            /* eq */
            if (onHost != null) {
                xml.append("\" node=\"");
                xml.append(onHost);
            }
            if (score != null) {
                xml.append("\" score=\"");
                xml.append(score);
            }
            xml.append("\"/>'");
        } else {
            /* ne, etc. */
            xml.append("\"><rule id=\"loc_");
            xml.append(heartbeatId);
            xml.append("-rule\"");
            if (score != null) {
                xml.append(" score=\"");
                xml.append(score);
                xml.append('"');
                if (onHost != null) {
                    xml.append("><expression attribute=\"#uname\" id=\"loc_");
                    xml.append(heartbeatId);
                    xml.append("-expression\" operation=\"");
                    xml.append(op);
                    xml.append("\" value=\"");
                    xml.append(onHost);
                    xml.append("\"/");
                }
            }
            xml.append("></rule></rsc_location>'");
        }
        return xml.toString();
    }

    /**
     * Sets location constraint.
     */
    public static boolean setLocation(final Host host,
                                      final String heartbeatId,
                                      final String onHost,
                                      final HostLocation hostLocation,
                                      String locationId,
                                      final boolean testOnly) {
        String command = "-U";
        if (locationId == null) {
            locationId = "loc_" + heartbeatId + "_" + onHost;
            command = "-C";
        }
        String score = null;
        String op = null;
        if (hostLocation != null) {
            score = hostLocation.getScore();
            op = hostLocation.getOperation();
        }
        final String xml = getLocationXML(heartbeatId,
                                          onHost,
                                          score,
                                          op,
                                          locationId);
        final SSH.SSHOutput ret = execCommand(
                                    host,
                                    getCibCommand(command, "constraints", xml),
                                    true,
                                    testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Removes location constraint.
     */
    public static boolean removeLocation(final Host host,
                                         final String locationId,
                                         final String heartbeatId,
                                         final HostLocation hostLocation,
                                         final boolean testOnly) {
        String score = null;
        String op = null;
        if (hostLocation != null) {
            score = hostLocation.getScore();
            op = hostLocation.getOperation();
        }
        final String xml = getLocationXML(heartbeatId,
                                          null,
                                          null,
                                          null,
                                          locationId);
        final String command = getCibCommand("-D", "constraints", xml);
        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Removes a resource from crm. If heartbeat id is null and group id is
     * not, the whole group will be removed.
     */
    public static boolean removeResource(final Host host,
                                         final String heartbeatId,
                                         final String groupId,
                                         final String cloneId,
                                         final boolean master,
                                         final boolean testOnly) {
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
        final String command = getCibCommand("-D",
                                             "resources",
                                             xml.toString());
        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Cleans up resource in crm.
     */
    public static boolean cleanupResource(final Host host,
                                          final String heartbeatId,
                                          final Host[] clusterHosts,
                                          final boolean testOnly) {
        /* make cleanup on all cluster hosts. */
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", heartbeatId);
        int exitCode = 0;
        for (Host clusterHost : clusterHosts) {
            replaceHash.put("@HOST@", clusterHost.getName());
            final String command =
                      host.getDistCommand("CRM.cleanupResource",
                                          replaceHash);
            final SSH.SSHOutput ret =
                                    execCommand(host, command, true, testOnly);
            exitCode = ret.getExitCode();
        }
        return exitCode == 0;
    }

    /**
     * Starts resource.
     */
    public static boolean startResource(final Host host,
                                        final String heartbeatId,
                                        final boolean testOnly) {
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
        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Returns meta attributes xml.
     */
    private static String getMetaAttributes(final Host host,
                                           final String heartbeatId,
                                           final Map<String, String> attrs,
                                           final String metaAttrsRefId) {
        final StringBuffer xml = new StringBuffer(360);
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        String idPostfix = "-meta_attributes";
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.1.4") <= 0) {
            idPostfix = "-meta-options";
        }
        if (metaAttrsRefId != null) {
            xml.append("<meta_attributes id-ref=\"");
            xml.append(metaAttrsRefId);
            xml.append("\"/>");
        } else {
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
                xml.append('-');
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
        }
        return xml.toString();
    }

    /**
     * Sets whether the service should be managed or not.
     */
    public static boolean setManaged(final Host host,
                                     final String heartbeatId,
                                     final boolean isManaged,
                                     final boolean testOnly) {
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
        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Stops resource.
     */
    public static boolean stopResource(final Host host,
                                       final String heartbeatId,
                                       final boolean testOnly) {
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
        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Migrates resource to the specified host.
     */
    public static boolean migrateResource(final Host host,
                                          final String heartbeatId,
                                          final String onHost,
                                          final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", heartbeatId);
        replaceHash.put("@HOST@", onHost);
        final String command = host.getDistCommand("CRM.migrateResource",
                                                   replaceHash);

        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Migrates resource from where it is running.
     */
    public static boolean migrateFromResource(final Host host,
                                              final String heartbeatId,
                                              final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", heartbeatId);
        final String command = host.getDistCommand("CRM.migrateFromResource",
                                                   replaceHash);

        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Migrates resource to the specified host.
     */
    public static boolean forceMigrateResource(final Host host,
                                               final String heartbeatId,
                                               final String onHost,
                                               final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", heartbeatId);
        replaceHash.put("@HOST@", onHost);
        final String command = host.getDistCommand("CRM.forceMigrateResource",
                                                   replaceHash);

        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Unmigrates resource that was previously migrated.
     */
    public static boolean unmigrateResource(final Host host,
                                            final String heartbeatId,
                                            final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", heartbeatId);
        final String command = host.getDistCommand(
                                             "CRM.unmigrateResource",
                                             replaceHash);
        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Sets global heartbeat parameters.
     */
    public static boolean setGlobalParameters(
                                        final Host host,
                                        final Map<String, String> args,
                                        final Map<String, String> rdiMetaArgs,
                                        String rscDefaultsId,
                                        final boolean testOnly) {
        final StringBuffer xml = new StringBuffer(360);
        xml.append(
            "'<crm_config><cluster_property_set id=\"cib-bootstrap-options\">");
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
            /* 2.1.4 */
            xml.append("<attributes>");
        }
        for (final String arg : args.keySet()) {
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
        final StringBuffer command = new StringBuffer(getCibCommand(
                                                               "-R",
                                                               "crm_config",
                                                               xml.toString()));
        if (rdiMetaArgs != null
            && (pmV != null
                || hbV == null
                || Tools.compareVersions(hbV, "2.99.0") >= 0)) {
            String updateOrReplace = "-R";
            if (rscDefaultsId == null) {
                rscDefaultsId = "rsc-options";
                updateOrReplace = "-U";
            }
            final StringBuffer rscdXML = new StringBuffer(360);
            rscdXML.append("'<rsc_defaults><meta_attributes id=\"");
            rscdXML.append(rscDefaultsId);
            rscdXML.append("\">");
            for (final String arg : rdiMetaArgs.keySet()) {
                final String id = "rsc-options-" + arg;
                rscdXML.append("<nvpair id=\"");
                rscdXML.append(id);
                rscdXML.append("\" name=\"");
                rscdXML.append(arg);
                rscdXML.append("\" value=\"");
                rscdXML.append(rdiMetaArgs.get(arg));
                rscdXML.append("\"/>");
            }
            rscdXML.append("</meta_attributes></rsc_defaults>'");

            command.append(';');
            command.append(getCibCommand(updateOrReplace,
                                         "rsc_defaults",
                                         rscdXML.toString()));
        }
        final SSH.SSHOutput ret =
                        execCommand(host, command.toString(), true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Removes colocation with specified colocation id.
     */
    public static boolean removeColocation(final Host host,
                                           final String colocationId,
                                           final boolean testOnly) {
        final StringBuffer xml = new StringBuffer(360);
        xml.append("'<rsc_colocation id=\"");
        xml.append(colocationId);
        xml.append("\"/>'");
        final String command = getCibCommand("-D",
                                             "constraints",
                                             xml.toString());
        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Adds colocation between service with heartbeatId and parentHbId.
     */
    public static boolean addColocation(final Host host,
                                        final String colId,
                                        final String heartbeatId,
                                        final String parentHbId,
                                        Map<String, String> attrs,
                                        final boolean testOnly) {
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
        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Removes order constraint with specified order id.
     */
    public static boolean removeOrder(final Host host,
                                      final String orderId,
                                      final boolean testOnly) {
        final StringBuffer xml = new StringBuffer(360);
        xml.append("'<rsc_order id=\"");
        xml.append(orderId);
        xml.append("\"/>'");
        final String command = getCibCommand("-D",
                                             "constraints",
                                             xml.toString());
        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Adds order constraint.
     */
    public static boolean addOrder(final Host host,
                                   final String ordId,
                                   final String parentHbId,
                                   final String heartbeatId,
                                   Map<String, String> attrs,
                                   final boolean testOnly) {
        String orderId;
        String cibadminOpt;
        if (ordId == null) {
            cibadminOpt = "-C"; /* create */
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
            final String type = "before";
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
        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
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
    public static boolean standByOn(final Host host, final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@HOST@", host.getName());
        final String command = host.getDistCommand("CRM.standByOn",
                                                   replaceHash);
        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Undoes heartbeat stand by.
     */
    public static boolean standByOff(final Host host, final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@HOST@", host.getName());
        final String command = host.getDistCommand("CRM.standByOff",
                                                   replaceHash);
        final SSH.SSHOutput ret = execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }
}
