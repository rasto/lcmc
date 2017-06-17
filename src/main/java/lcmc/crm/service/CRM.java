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

package lcmc.crm.service;

import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.SshOutput;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.configs.DistResource;
import lcmc.crm.domain.CrmXml;
import lcmc.crm.domain.HostLocation;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;

/**
 * This class provides cib commands. There are commands that use cibadmin and
 * crm_resource commands to manipulate the cib, crm, etc.
 */
public final class CRM {
    private static final Logger LOG = LoggerFactory.getLogger(CRM.class);
    public static final String CIB_OP_MODIFY = "-M";
    public static final String CIB_OP_CREATE = "-C";
    public static final String CIB_OP_REPLACE = "-R";
    /** Output of the ptest. */
    private static volatile String ptestOutput = null;
    private static final ReadWriteLock M_PTEST_LOCK = new ReentrantReadWriteLock();
    private static final Lock M_PTEST_READLOCK = M_PTEST_LOCK.readLock();
    private static final Lock M_PTEST_WRITELOCK = M_PTEST_LOCK.writeLock();
    /** Delimiter that delimits the ptest and test cib part. */
    public static final String PTEST_END_DELIM = "--- PTEST END ---";
    /** Location of lcmc-test.xml file. */
    public static final String LCMC_TEST_FILE = "/tmp/lcmc-test-" + UUID.randomUUID() + ".xml";

    public static String getCibCommand(final String command, final String objType, final String xml) {
        final StringBuilder cmd = new StringBuilder(300);
        cmd.append(DistResource.SUDO + "/usr/sbin/cibadmin -o ");
        cmd.append(objType);
        cmd.append(' ');
        cmd.append(command);
        cmd.append(" -X ");
        cmd.append(xml);
        return cmd.toString();
    }

    /** Executes specified command on the host. */
    private static SshOutput execCommand(final Host host, final String command, final Application.RunMode runMode) {
        M_PTEST_WRITELOCK.lock();
        try {
            ptestOutput = null;
        } finally {
            M_PTEST_WRITELOCK.unlock();
        }
        if (Application.isTest(runMode)) {
            final String testCmd =
                "if [ ! -e " + LCMC_TEST_FILE + " ]; "
             + "then " + DistResource.SUDO + "/usr/sbin/cibadmin -Ql > "
             + LCMC_TEST_FILE + ".new"
             + ";mv " + LCMC_TEST_FILE + ".new " + LCMC_TEST_FILE
             + ";fi;"
             + "export CIB_file=" + LCMC_TEST_FILE + ';';
            return host.captureCommand(new ExecCommandConfig().command(testCmd + command)
                                                              .silentCommand()
                                                              .silentOutput());
        } else {
            LOG.debug1("execCommand: crm command: " + command);
            return host.captureCommandProgressIndicator(Tools.getString("CIB.ExecutingCommand"),
                                                        new ExecCommandConfig().command(command));
        }
    }

    /** Executes the ptest command and returns results. */
    public static String getPtest(final Host host) {
        M_PTEST_READLOCK.lock();
        try {
            if (ptestOutput != null) {
                return ptestOutput;
            }
        } finally {
            M_PTEST_READLOCK.unlock();
        }
        final String command = "export PROG=/usr/sbin/crm_simulate;"
                               + "if [ -e /usr/sbin/ptest ];"
                               + " then export PROG=/usr/sbin/ptest; "
                               + "fi;"
                               + DistResource.SUDO + "$PROG -VVVVV -S -x "
                               + LCMC_TEST_FILE
                               + " 2>&1;echo '"
                               + PTEST_END_DELIM
                               + "';cat " + LCMC_TEST_FILE + " 2>/dev/null;"
                               + "mv -f " + LCMC_TEST_FILE + "{,.last} 2>/dev/null";
        final SshOutput output = host.captureCommand(new ExecCommandConfig().command(command)
                                                                            .silentCommand()
                                                                            .silentOutput());
        M_PTEST_WRITELOCK.lock();
        try {
            final String po = output.getOutput();
            if (ptestOutput == null) {
                ptestOutput = po;
            }
            return po;
        } finally {
            M_PTEST_WRITELOCK.unlock();
        }
    }

    private static String getPrimitiveXML(final Host host,
                                          final String resId,
                                          final Map<String, String> pacemakerResAttrs,
                                          final Map<String, String> pacemakerResArgs,
                                          final Map<String, String> pacemakerMetaArgs,
                                          String instanceAttrId,
                                          final Map<String, String> nvpairIdsHash,
                                          final Map<String, Map<String, String>> pacemakerOps,
                                          String operationsId,
                                          final String metaAttrsRefId,
                                          final String operationsRefId,
                                          final Boolean stonith,
                                          final Application.RunMode runMode) {
        final StringBuilder xml = new StringBuilder(360);
        if (resId != null) {
            if (instanceAttrId == null) {
                instanceAttrId = resId + "-instance_attributes";
            }
            final StringBuilder attrsString = new StringBuilder(100);
            for (final Map.Entry<String, String> pacemakerResEntry : pacemakerResAttrs.entrySet()) {
                final String value = pacemakerResEntry.getValue();
                attrsString.append(pacemakerResEntry.getKey());
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
                if (Tools.versionBeforePacemaker(host)) {
                    /* 2.1.4 */
                    xml.append("<attributes>");
                }

                for (final String paramName : pacemakerResArgs.keySet()) {
                    final String value = pacemakerResArgs.get(paramName);
                    String nvpairId = null;
                    if (nvpairIdsHash != null) {
                        nvpairId = nvpairIdsHash.get(paramName);
                    }
                    final String newParamName;
                    if ((stonith != null && stonith) && CrmXml.STONITH_PRIORITY_INSTANCE_ATTR.equals(paramName)) {
                        newParamName = "priority";
                    } else {
                        newParamName = paramName;
                    }
                    if (nvpairId == null) {
                        nvpairId = "nvpair-" + resId + '-' + newParamName;
                    }
                    xml.append("<nvpair id=\"");
                    xml.append(nvpairId);
                    xml.append("\" name=\"");
                    xml.append(newParamName);
                    xml.append("\" value=\"");
                    xml.append(value);
                    xml.append("\"/>");
                }
                if (Tools.versionBeforePacemaker(host)) {
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
            if (Tools.versionBeforePacemaker(host)) {
                xml.append("<operations>");
            } else {
                /* 2.1.4 does not have the id. */
                if (operationsId == null) {
                    operationsId = resId + "-operations";
                }
                xml.append("<operations id=\"");
                xml.append(operationsId);
                xml.append("\">");
            }
            for (final Map.Entry<String, Map<String, String>> pacemakerOpsEntry : pacemakerOps.entrySet()) {
                final Map<String, String> opHash = pacemakerOpsEntry.getValue();
                xml.append("<op");
                boolean checkLevel = false;
                for (final Map.Entry<String, String> opEntry : opHash.entrySet()) {
                    if (CrmXml.PARAM_OCF_CHECK_LEVEL.equals(opEntry.getKey())) {
                        checkLevel = true;
                        continue;
                    }
                    final String value = opEntry.getValue();
                    xml.append(' ');
                    xml.append(opEntry.getKey());
                    xml.append("=\"");
                    xml.append(value);
                    xml.append('"');
                }
                if (checkLevel) {
                    xml.append('>');
                    final String iaId = resId + "-monitor-instance_attributes";
                    xml.append("<instance_attributes id=\"");
                    xml.append(iaId);
                    xml.append("\"><nvpair id=\"");
                    xml.append(iaId);
                    xml.append('-');
                    xml.append(CrmXml.PARAM_OCF_CHECK_LEVEL);
                    xml.append("\" name=\"");
                    xml.append(CrmXml.PARAM_OCF_CHECK_LEVEL);
                    xml.append("\" value=\"");
                    xml.append(opHash.get(CrmXml.PARAM_OCF_CHECK_LEVEL));
                    xml.append("\"/></instance_attributes></op>");
                } else {
                    xml.append("/>");
                }
            }
            xml.append("</operations>");
        }
        /* meta_attributes */
        if (resId != null) {
            xml.append(getMetaAttributes(host, resId, pacemakerMetaArgs, metaAttrsRefId));
            xml.append("</primitive>");
        }
        return xml.toString();
    }

    /** Adds or updates resource in the CIB. */
    public static boolean setParameters(final Host host,
                                        final String command, /* -R or -C */
                                        final String resId,
                                        final String cloneId,
                                        final boolean master,
                                        final Map<String, String> cloneMetaArgs,
                                        final Map<String, String> groupMetaArgs,
                                        final String groupId,
                                        final Map<String, String> pacemakerResAttrs,
                                        final Map<String, String> pacemakerResArgs,
                                        final Map<String, String> pacemakerMetaArgs,
                                        final String instanceAttrId,
                                        final Map<String, String> nvpairIdsHash,
                                        final Map<String, Map<String, String>> pacemakerOps,
                                        final String operationsId,
                                        final String metaAttrsRefId,
                                        final String cloneMetaAttrsRefId,
                                        final String groupMetaAttrsRefId,
                                        final String operationsRefId,
                                        final Boolean stonith,
                                        final Application.RunMode runMode) {
        final StringBuilder xml = new StringBuilder(360);
        xml.append('\'');
        if (cloneId != null) {
            if (master) {
                if (Tools.versionBeforePacemaker(host)) {
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
            xml.append(getMetaAttributes(host, cloneId, cloneMetaArgs, cloneMetaAttrsRefId));
            //}
        }
        if (groupId != null) {
            if (resId != null) {
                xml.append("<group id=\"");
                xml.append(groupId);
                xml.append("\">");
            }
            xml.append(getMetaAttributes(host, groupId, groupMetaArgs, groupMetaAttrsRefId));
        }
        xml.append(getPrimitiveXML(host,
                                   resId,
                                   pacemakerResAttrs,
                                   pacemakerResArgs,
                                   pacemakerMetaArgs,
                                   instanceAttrId,
                                   nvpairIdsHash,
                                   pacemakerOps,
                                   operationsId,
                                   metaAttrsRefId,
                                   operationsRefId,
                                   stonith,
                                   runMode));
        if (groupId != null && resId != null) {
            xml.append("</group>");
        }
        if (cloneId != null) {
            if (master) {
                if (Tools.versionBeforePacemaker(host)) {
                    xml.append("</master_slave>");
                } else {
                    xml.append("</master>");
                }
            } else {
                xml.append("</clone>");
            }
        }
        xml.append('\'');

        final SshOutput ret = execCommand(host, getCibCommand(command, "resources", xml.toString()), runMode);
        return ret.getExitCode() == 0;
    }

    /** Replaces the whole group. */
    public static boolean replaceGroup(final boolean createGroup,
                                       final Host host,
                                       final String cloneId,
                                       final boolean master,
                                       final Map<String, String> cloneMetaArgs,
                                       final String cloneMetaAttrsRefId,
                                       final Iterable<String> resourceIds,
                                       final Map<String, String> groupMetaArgs,
                                       final String groupId,
                                       final Map<String, Map<String, String>> pacemakerResAttrs,
                                       final Map<String, Map<String, String>> pacemakerResArgs,
                                       final Map<String, Map<String, String>> pacemakerMetaArgs,
                                       final Map<String, String> instanceAttrId,
                                       final Map<String, Map<String, String>> nvpairIdsHash,
                                       final Map<String, Map<String, Map<String, String>>> pacemakerOps,
                                       final Map<String, String> operationsId,
                                       final Map<String, String> metaAttrsRefId,
                                       final String groupMetaAttrsRefId,
                                       final Map<String, String> operationsRefId,
                                       final Map<String, Boolean> stonith,
                                       final Application.RunMode runMode) {
        final StringBuilder xml = new StringBuilder(720);
        xml.append('\'');
        if (cloneId != null) {
            if (master) {
                if (Tools.versionBeforePacemaker(host)) {
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
            xml.append(getMetaAttributes(host, cloneId, cloneMetaArgs, cloneMetaAttrsRefId));
        }
        xml.append("<group id=\"");
        xml.append(groupId);
        xml.append("\">");
        xml.append(getMetaAttributes(host, groupId, groupMetaArgs, groupMetaAttrsRefId));
        for (final String resId : resourceIds) {
            xml.append(getPrimitiveXML(host,
                                       resId,
                                       pacemakerResAttrs.get(resId),
                                       pacemakerResArgs.get(resId),
                                       pacemakerMetaArgs.get(resId),
                                       instanceAttrId.get(resId),
                                       nvpairIdsHash.get(resId),
                                       pacemakerOps.get(resId),
                                       operationsId.get(resId),
                                       metaAttrsRefId.get(resId),
                                       operationsRefId.get(resId),
                                       stonith.get(resId),
                                       runMode));
        }
        xml.append("</group>");
        if (cloneId != null) {
            if (master) {
                if (Tools.versionBeforePacemaker(host)) {
                    xml.append("</master_slave>");
                } else {
                    xml.append("</master>");
                }
            } else {
                xml.append("</clone>");
            }
        }
        xml.append('\'');

        final String cibadminOpt;
        if (createGroup) {
            cibadminOpt = CIB_OP_CREATE;
        } else {
            cibadminOpt = CIB_OP_REPLACE;
        }

        final SshOutput ret = execCommand(host, getCibCommand(cibadminOpt, "resources", xml.toString()), runMode);
        return ret.getExitCode() == 0;
    }

    /**
     * Sets colocation and order constraint between service with resId
     * and parents. If colAttrsList or ordAttrsList are null, there is only
     * colocation or order but not both defined.
     */
    public static void setOrderAndColocation(final Host host,
                                             final String resId,
                                             final String[] parents,
                                             final List<Map<String, String>> colAttrsList,
                                             final List<Map<String, String>> ordAttrsList,
                                             final Application.RunMode runMode) {
        for (int i = 0; i < parents.length; i++) {
            if (colAttrsList.get(i) != null) {
                addColocation(host, null, resId, parents[i], colAttrsList.get(i), runMode);
            }
            if (ordAttrsList.get(i) != null) {
                addOrder(host, null, parents[i], resId, ordAttrsList.get(i), runMode);
            }
        }
    }

    private static String getOneRscSet(
        final String rscSetId, final CrmXml.RscSet rscSet, Map<String, String> attrs) {
        final StringBuilder xml = new StringBuilder(120);
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
            final String requireAll = rscSet.getRequireAll();
            if (requireAll != null
                && !requireAll.equals(CrmXml.REQUIRE_ALL_TRUE_VALUE.getValueForConfig())) {
                attrs.put(CrmXml.REQUIRE_ALL_ATTR, requireAll);
            }
        }
        for (final Map.Entry<String, String> attrsEntry : attrs.entrySet()) {
            final String value = attrsEntry.getValue();
            if (value != null && value.isEmpty()) {
                continue;
            }
            xml.append("\" ").append(attrsEntry.getKey()).append("=\"");
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

    public static boolean setRscSet(final Host host,
                                    final String colId,
                                    final boolean createCol,
                                    final String ordId,
                                    final boolean createOrd,
                                    final Map< CrmXml.RscSet, Map<String, String>> rscSetsColAttrs,
                                    final Map<CrmXml.RscSet, Map<String, String>> rscSetsOrdAttrs,
                                    final Map<String, String> attrs,
                                    final Application.RunMode runMode) {
        if (colId != null) {
            if (rscSetsColAttrs.isEmpty()) {
                return removeColocation(host, colId, runMode);
            }
            final String cibadminOpt;
            if (createCol) {
                cibadminOpt = CIB_OP_CREATE;
            } else {
                cibadminOpt = CIB_OP_REPLACE;
            }
            final boolean ret = setRscSetConstraint(host, "rsc_colocation",
                                                    colId,
                                                    rscSetsColAttrs,
                                                    attrs,
                                                    cibadminOpt,
                                                    runMode);
            if (!ret) {
                return false;
            }
        }
        if (ordId != null) {
            if (rscSetsOrdAttrs.isEmpty()) {
                return removeOrder(host, ordId, runMode);
            }
            final String cibadminOpt;
            if (createOrd) {
                cibadminOpt = CIB_OP_CREATE;
            } else {
                cibadminOpt = CIB_OP_REPLACE;
            }
            return setRscSetConstraint(host,
                                       "rsc_order",
                                       ordId,
                                       rscSetsOrdAttrs,
                                       attrs,
                                       cibadminOpt,
                                       runMode);
        }
        return true;
    }

    private static boolean setRscSetConstraint(final Host host,
                                               final String tag,
                                               final String constraintId,
                                               final Map<CrmXml.RscSet, Map<String, String>> rscSetsAttrs,
                                               final Map<String, String> attrs,
                                               final String cibadminOpt,
                                               final Application.RunMode runMode) {
        final StringBuilder xml = new StringBuilder(360);
        xml.append("'<");
        xml.append(tag);
        xml.append(" id=\"");
        xml.append(constraintId);
        if (attrs != null) {
            for (final Map.Entry<String, String> attrsEntry : attrs.entrySet()) {
                final String value = attrsEntry.getValue();
                if (value == null || value.isEmpty()) {
                    continue;
                }
                xml.append("\" ").append(attrsEntry.getKey()).append("=\"");
                xml.append(value);
            }
        }
        xml.append("\">");
        int rsId = 0;
        for (final Map.Entry<CrmXml.RscSet, Map<String, String>> rscSetsEntry : rscSetsAttrs.entrySet()) {
            if (rscSetsEntry.getKey() != null) {
                xml.append(getOneRscSet(constraintId + '-' + rsId, rscSetsEntry.getKey(), rscSetsEntry.getValue()));
                rsId++;
            }
        }
        xml.append("</");
        xml.append(tag);
        xml.append(">'");

        final String command = getCibCommand(cibadminOpt, "constraints", xml.toString());
        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    private static String getLocationXML(final String resId,
                                         final String onHost,
                                         final String attribute,
                                         final String score,
                                         final String scoreAttribute,
                                         final String op,
                                         final String role,
                                         final String locationId) {
        final StringBuilder xml = new StringBuilder(360);
        xml.append("'<rsc_location id=\"");
        xml.append(locationId);
        xml.append("\" rsc=\"");
        xml.append(resId);
        if (op == null || ("eq".equals(op) && !"pingd".equals(attribute) && role == null)) {
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
            xml.append("\"><rule id=\"");
            xml.append(locationId);
            xml.append("-rule\"");
            if (score != null) {
                xml.append(" score=\"");
                xml.append(score);
                xml.append('"');
            }
            if (role != null) {
                xml.append(" role=\"");
                xml.append(role);
                xml.append('"');
            }
            if (scoreAttribute != null) {
                xml.append(" score-attribute=\"");
                xml.append(scoreAttribute);
                xml.append('"');
            }
            if (attribute != null) {
                xml.append("><expression attribute=\"");
                xml.append(attribute);
                xml.append("\" id=\"");
                xml.append(locationId);
                xml.append("-expression\" operation=\"");
                xml.append(op);
                if (onHost != null) {
                    xml.append("\" value=\"");
                    xml.append(onHost);
                }
                xml.append("\"/");
            }
            xml.append("></rule></rsc_location>'");
        }
        return xml.toString();
    }

    public static boolean setLocation(final Host host,
                                      final String resId,
                                      final String onHost,
                                      final HostLocation hostLocation,
                                      String locationId,
                                      final Application.RunMode runMode) {
        String command = CIB_OP_MODIFY;
        if (locationId == null) {
            locationId = "loc_" + resId + '_' + onHost;
            command = CIB_OP_CREATE;
        } else if ("migration".equals(locationId)) {
            locationId = "cli-standby-" + resId;
            command = CIB_OP_CREATE;
        } else if ("remigration".equals(locationId)) {
            locationId = "cli-standby-" + resId;
            command = CIB_OP_MODIFY;
        }
        String score = null;
        String op = null;
        String role = null;
        if (hostLocation != null) {
            score = hostLocation.getScore();
            op = hostLocation.getOperation();
            role = hostLocation.getRole();
        }
        final String xml = getLocationXML(resId, onHost, "#uname", score, null, op, role, locationId);
        final SshOutput ret = execCommand(host, getCibCommand(command, "constraints", xml), runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean setPingLocation(final Host host,
                                          final String resId,
                                          final String ruleType,
                                          String locationId,
                                          final Application.RunMode runMode) {
        String op = null;
        String value = null;
        String score = null;
        String scoreAttribute = null;
        String idPart = "";
        if ("defined".equals(ruleType)) {
            scoreAttribute = "pingd";
            op = "defined";
            idPart = "prefer";
        } else if ("eq0".equals(ruleType)) {
            score = "-INFINITY";
            op = "eq";
            value = "0";
            idPart = "exclude";
        }
        String command = CIB_OP_MODIFY;
        if (locationId == null) {
            locationId = "loc_" + resId + "-ping-" + idPart;
            command = CIB_OP_CREATE;
        }
        final String attribute = "pingd";
        final String xml = getLocationXML(resId, value, attribute, score, scoreAttribute, op, null, locationId);
        final SshOutput ret = execCommand(host, getCibCommand(command, "constraints", xml), runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean removeLocation(final Host host,
                                         final String locationId,
                                         final String resId,
                                         final Application.RunMode runMode) {
        final String xml = getLocationXML(resId, null, null, null, null, null, null, locationId);
        final String command = getCibCommand("-D", "constraints", xml);
        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    /**
     * Removes a resource from crm. If heartbeat id is null and group id is
     * not, the whole group will be removed.
     */
    public static boolean removeResource(final Host host,
                                         final String resId,
                                         final String groupId,
                                         final String cloneId,
                                         final boolean master,
                                         final Application.RunMode runMode) {
        final StringBuilder xml = new StringBuilder(360);
        xml.append('\'');
        if (cloneId != null) {
            if (master) {
                if (Tools.versionBeforePacemaker(host)) {
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
        if (resId != null) {
            xml.append("<primitive id=\"");
            xml.append(resId);
            xml.append("\"></primitive>");
        }
        if (groupId != null) {
            xml.append("</group>");
        }
        if (cloneId != null) {
            if (master) {
                if (Tools.versionBeforePacemaker(host)) {
                    xml.append("</master_slave>");
                } else {
                    xml.append("</master>");
                }
            } else {
                xml.append("</clone>");
            }
        }
        xml.append('\'');
        final String command = getCibCommand("-D", "resources", xml.toString());
        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean cleanupResource(final Host host,
                                          final String resId,
                                          final Host[] clusterHosts,
                                          final Application.RunMode runMode) {
        if (Application.isTest(runMode)) {
            return true;
        }
        /* make cleanup on all cluster hosts. */
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", resId);
        for (final Host clusterHost : clusterHosts) {
            replaceHash.put("@HOST@", clusterHost.getName());
            final String command = host.getDistCommand("CRM.cleanupResource", replaceHash);
            execCommand(host, command, runMode);
        }
        return true; /* always return true */
    }

    public static boolean startResource(final Host host, final String resId, final Application.RunMode runMode) {
        String cmd = "CRM.startResource";
        if (Tools.versionBeforePacemaker(host)) {
            cmd = "CRM.2.1.4.startResource";
        }
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", resId);
        final String command = host.getDistCommand(cmd, replaceHash);
        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    private static String getMetaAttributes(final Host host,
                                            final String resId,
                                            final Map<String, String> attrs,
                                            final String metaAttrsRefId) {
        final StringBuilder xml = new StringBuilder(360);
        String idPostfix = "-meta_attributes";
        if (Tools.versionBeforePacemaker(host)) {
            idPostfix = "-meta-options";
        }
        if (metaAttrsRefId == null) {
            xml.append("<meta_attributes id=\"");
            xml.append(resId);
            xml.append(idPostfix);
            xml.append("\">");

            if (Tools.versionBeforePacemaker(host)) {
                /* 2.1.4 */
                xml.append("<attributes>");
            }
            for (final Map.Entry<String, String> attrsEntry : attrs.entrySet()) {
                xml.append("<nvpair id=\"");
                xml.append(resId);
                xml.append(idPostfix);
                xml.append('-');
                xml.append(attrsEntry.getKey());
                xml.append("\" name=\"");
                xml.append(attrsEntry.getKey());
                xml.append("\" value=\"");
                xml.append(attrsEntry.getValue());
                xml.append("\"/>");
            }

            if (Tools.versionBeforePacemaker(host)) {
                /* 2.1.4 */
                xml.append("</attributes>");
            }
            xml.append("</meta_attributes>");
        } else {
            xml.append("<meta_attributes id-ref=\"");
            xml.append(metaAttrsRefId);
            xml.append("\"/>");
        }
        return xml.toString();
    }

    public static boolean setManaged(final Host host,
                                     final String resId,
                                     final boolean isManaged,
                                     final Application.RunMode runMode) {
        final String label;
        if (isManaged) {
            label = ".isManagedOn";
        } else {
            label = ".isManagedOff";
        }
        String cmd = "CRM" + label;
        if (Tools.versionBeforePacemaker(host)) {
            cmd = "CRM.2.1.4" + label;
        }
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", resId);

        final String command = host.getDistCommand(cmd, replaceHash);
        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean stopResource(final Host host, final String resId, final Application.RunMode runMode) {
        if (resId == null) {
            return false;
        }
        String cmd = "CRM.stopResource";
        if (Tools.versionBeforePacemaker(host)) {
            cmd = "CRM.2.1.4.stopResource";
        }
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", resId);

        final String command = host.getDistCommand(cmd, replaceHash);
        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean migrateResource(final Host host,
                                          final String resId,
                                          final String onHost,
                                          final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", resId);
        replaceHash.put("@HOST@", onHost);
        final String command = host.getDistCommand("CRM.migrateResource", replaceHash);

        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean migrateFromResource(final Host host, final String resId, final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", resId);
        final String command = host.getDistCommand("CRM.migrateFromResource", replaceHash);

        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean forceMigrateResource(final Host host,
                                               final String resId,
                                               final String onHost,
                                               final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", resId);
        replaceHash.put("@HOST@", onHost);
        final String command = host.getDistCommand("CRM.forceMigrateResource", replaceHash);

        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean unmigrateResource(final Host host, final String resId, final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@ID@", resId);
        final String command = host.getDistCommand("CRM.unmigrateResource", replaceHash);
        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean setGlobalParameters(final Host host,
                                              final Map<String, String> args,
                                              final Map<String, String> rdiMetaArgs,
                                              String rscDefaultsId,
                                              final Application.RunMode runMode) {
        final StringBuilder xml = new StringBuilder(360);
        xml.append("'<crm_config><cluster_property_set id=\"cib-bootstrap-options\">");
        if (Tools.versionBeforePacemaker(host)) {
            /* 2.1.4 */
            xml.append("<attributes>");
        }
        for (final Map.Entry<String, String> argsEntry : args.entrySet()) {
            final String id = "cib-bootstrap-options-" + argsEntry.getKey();
            xml.append("<nvpair id=\"");
            xml.append(id);
            xml.append("\" name=\"");
            xml.append(argsEntry.getKey());
            xml.append("\" value=\"");
            xml.append(argsEntry.getValue());
            xml.append("\"/>");
        }
        if (Tools.versionBeforePacemaker(host)) {
            /* 2.1.4 */
            xml.append("</attributes>");
        }
        xml.append("</cluster_property_set></crm_config>'");
        final StringBuilder command = new StringBuilder(getCibCommand(CIB_OP_REPLACE, "crm_config", xml.toString()));
        if (rdiMetaArgs != null && !Tools.versionBeforePacemaker(host)) {
            String updateOrReplace = CIB_OP_REPLACE;
            if (rscDefaultsId == null) {
                rscDefaultsId = "rsc-options";
                updateOrReplace = CIB_OP_MODIFY;
            }
            final StringBuilder rscdXML = new StringBuilder(360);
            rscdXML.append("'<rsc_defaults><meta_attributes id=\"");
            rscdXML.append(rscDefaultsId);
            rscdXML.append("\">");
            for (final Map.Entry<String, String> rdiMetaEntry : rdiMetaArgs.entrySet()) {
                final String id = "rsc-options-" + rdiMetaEntry.getKey();
                rscdXML.append("<nvpair id=\"");
                rscdXML.append(id);
                rscdXML.append("\" name=\"");
                rscdXML.append(rdiMetaEntry.getKey());
                rscdXML.append("\" value=\"");
                rscdXML.append(rdiMetaEntry.getValue());
                rscdXML.append("\"/>");
            }
            rscdXML.append("</meta_attributes></rsc_defaults>'");

            command.append(';');
            command.append(getCibCommand(updateOrReplace, "rsc_defaults", rscdXML.toString()));
        }
        final SshOutput ret = execCommand(host, command.toString(), runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean removeColocation(
        final Host host, final String colocationId, final Application.RunMode runMode) {
        final StringBuilder xml = new StringBuilder(360);
        xml.append("'<rsc_colocation id=\"");
        xml.append(colocationId);
        xml.append("\"/>'");
        final String command = getCibCommand("-D", "constraints", xml.toString());
        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean addColocation(final Host host,
                                        final String colId,
                                        final String resId,
                                        final String parentHbId,
                                        Map<String, String> attrs,
                                        final Application.RunMode runMode) {
        if (parentHbId == null) {
            return false;
        }
        final String colocationId;
        final String cibadminOpt;
        if (colId == null) {
            cibadminOpt = CIB_OP_CREATE;
            if (parentHbId.compareTo(resId) < 0) {
                colocationId = "col_" + resId + '_' + parentHbId;
            } else {
                colocationId = "col_" + parentHbId + '_' + resId;
            }
        } else {
            cibadminOpt = CIB_OP_REPLACE;
            colocationId = colId;
        }
        if (attrs == null) {
            attrs = new LinkedHashMap<String, String>();
        }
        attrs.put("rsc", resId);
        attrs.put("with-rsc", parentHbId);
        final StringBuilder xml = new StringBuilder(360);
        xml.append("'<rsc_colocation id=\"");
        xml.append(colocationId);
        final Map<String, String> convertHash = new HashMap<String, String>();
        if (Tools.versionBeforePacemaker(host)) {
            /* <= 2.1.4 */
            convertHash.put("rsc", "from");
            convertHash.put("with-rsc", "to");
            convertHash.put("rsc-role", "from_role");
            convertHash.put("with-rsc-role", "to_role");
        }
        for (String attr : attrs.keySet()) {
            final String value = attrs.get(attr);
            if (value != null && value.isEmpty()) {
                continue;
            }
            if (convertHash.containsKey(attr)) {
                attr = convertHash.get(attr);
            }
            xml.append("\" ").append(attr).append("=\"");
            xml.append(value);
        }
        xml.append("\"/>'");
        final String command = getCibCommand(cibadminOpt, "constraints", xml.toString());
        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean removeOrder(final Host host, final String orderId, final Application.RunMode runMode) {
        final StringBuilder xml = new StringBuilder(360);
        xml.append("'<rsc_order id=\"");
        xml.append(orderId);
        xml.append("\"/>'");
        final String command = getCibCommand("-D", "constraints", xml.toString());
        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean addOrder(final Host host,
                                   final String ordId,
                                   final String parentHbId,
                                   final String resId,
                                   Map<String, String> attrs,
                                   final Application.RunMode runMode) {
        final String orderId;
        final String cibadminOpt;
        if (ordId == null) {
            cibadminOpt = CIB_OP_CREATE;
            orderId = "ord_" + parentHbId + '_' + resId;
        } else {
            cibadminOpt = CIB_OP_REPLACE;
            orderId = ordId;
        }
        if (attrs == null) {
            attrs = new LinkedHashMap<String, String>();
        }
        final StringBuilder xml = new StringBuilder(360);
        xml.append("'<rsc_order id=\"");
        xml.append(orderId);
        attrs.put("first", parentHbId);
        attrs.put("then", resId);
        final Map<String, String> convertHash = new HashMap<String, String>();
        if (Tools.versionBeforePacemaker(host)) {
            /* <= 2.1.4 */
            convertHash.put("first", "to");
            convertHash.put("then", "from");
            convertHash.put("first-action", "action");
            convertHash.put("then-action", "to_action");
        }
        for (String attr : attrs.keySet()) {
            final String value = attrs.get(attr);
            if (value != null && value.isEmpty()) {
                continue;
            }
            if (convertHash.containsKey(attr)) {
                attr = convertHash.get(attr);
            }
            xml.append("\" ").append(attr).append("=\"");
            xml.append(value);
        }
        xml.append("\"/>'");
        final String command = getCibCommand(cibadminOpt, "constraints", xml.toString());
        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean standByOn(final Host host, final Host standByHost, final Application.RunMode runMode) {
        String cmd = "CRM.standByOn";
        if (Tools.versionBeforePacemaker(host)) {
            cmd = "CRM.2.1.4.standByOn";
        }
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@HOST@", standByHost.getName());
        final String command = host.getDistCommand(cmd, replaceHash);
        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean standByOff(final Host host, final Host standByHost, final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        String cmd = "CRM.standByOff";
        if (Tools.versionBeforePacemaker(host)) {
            cmd = "CRM.2.1.4.standByOff";
        }
        replaceHash.put("@HOST@", standByHost.getName());
        final String command = host.getDistCommand(cmd, replaceHash);
        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    public static boolean erase(final Host host, final Application.RunMode runMode) {
        final Map<String, String> replaceHash = Collections.emptyMap();
        final String command = host.getDistCommand("CRM.erase", replaceHash);
        final SshOutput ret = execCommand(host, command, runMode);
        return ret.getExitCode() == 0;
    }

    public static String crmConfigureCommit(final Host host, final String config, final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@CONFIG@", Tools.escapeQuotes(Matcher.quoteReplacement(config), 1));
        final String command = host.getDistCommand("CRM.configureCommit", replaceHash);
        final SshOutput ret = execCommand(host, command, runMode);
        if (ret.getExitCode() == 0) {
            return ret.getOutput();
        }
        return "error";
    }

    private CRM() {
        /* empty */
    }
}
