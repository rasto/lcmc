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

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Collections;
import org.apache.commons.collections.map.MultiKeyMap;

/**
 * This class parses ocf crm xml, stores information like
 * short and long description, data types etc. for defined types
 * of services in the hashes and provides methods to get this
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class CRMXML extends XML {
    /** Host. */
    private final Host host;
    /** List of global parameters. */
    private final List<String> globalParams = new ArrayList<String>();
    /** List of required global parameters. */
    private final List<String> globalRequiredParams = new ArrayList<String>();
    /** Map from class to the list of all crm services. */
    private final Map<String, List<ResourceAgent>> classToServicesMap =
                                new HashMap<String, List<ResourceAgent>>();
    /** Map from global parameter to its short description. */
    private final Map<String, String> paramGlobalShortDescMap =
                                                new HashMap<String, String>();
    /** Map from global parameter to its long description. */
    private final Map<String, String> paramGlobalLongDescMap =
                                                new HashMap<String, String>();
    /** Map from global parameter to its default value. */
    private final Map<String, String> paramGlobalDefaultMap =
                                                new HashMap<String, String>();
    /** Map from global parameter to its preferred value. */
    private final Map<String, String> paramGlobalPreferredMap =
                                                new HashMap<String, String>();
    /** Map from global parameter to its type. */
    private final Map<String, String> paramGlobalTypeMap =
                                                new HashMap<String, String>();
    /** Map from global parameter to the array of possible choices. */
    private final Map<String, String[]> paramGlobalPossibleChoices =
                                              new HashMap<String, String[]>();
    /** List of parameters for colocations. */
    private final List<String> colParams = new ArrayList<String>();
    /** List of required parameters for colocations. */
    private final List<String> colRequiredParams = new ArrayList<String>();
    /** Map from colocation parameter to its short description. */
    private final Map<String, String> paramColShortDescMap =
                                                new HashMap<String, String>();
    /** Map from colocation parameter to its long description. */
    private final Map<String, String> paramColLongDescMap =
                                                new HashMap<String, String>();
    /** Map from colocation parameter to its default value. */
    private final Map<String, String> paramColDefaultMap =
                                                new HashMap<String, String>();
    /** Map from colocation parameter to its preferred value. */
    private final Map<String, String> paramColPreferredMap =
                                                new HashMap<String, String>();
    /** Map from colocation parameter to its type. */
    private final Map<String, String> paramColTypeMap =
                                                new HashMap<String, String>();
    /** Map from colocation parameter to the array of possible choices. */
    private final Map<String, String[]> paramColPossibleChoices =
                                              new HashMap<String, String[]>();

    /** Map from colocation parameter to the array of possible choices for
     * master/slave resource. */
    private final Map<String, String[]> paramColPossibleChoicesMS =
                                              new HashMap<String, String[]>();

    /** List of parameters for order. */
    private final List<String> ordParams = new ArrayList<String>();
    /** List of required parameters for orders. */
    private final List<String> ordRequiredParams = new ArrayList<String>();
    /** Map from order parameter to its short description. */
    private final Map<String, String> paramOrdShortDescMap =
                                             new HashMap<String, String>();
    /** Map from order parameter to its long description. */
    private final Map<String, String> paramOrdLongDescMap =
                                             new HashMap<String, String>();
    /** Map from order parameter to its default value. */
    private final Map<String, String> paramOrdDefaultMap =
                                                new HashMap<String, String>();
    /** Map from order parameter to its preferred value. */
    private final Map<String, String> paramOrdPreferredMap =
                                                new HashMap<String, String>();
    /** Map from order parameter to its type. */
    private final Map<String, String> paramOrdTypeMap =
                                                new HashMap<String, String>();
    /** Map from order parameter to the array of possible choices. */
    private final Map<String, String[]> paramOrdPossibleChoices =
                                              new HashMap<String, String[]>();
    /** Map from order parameter to the array of possible choices for
     * master/slave resource. */
    private final Map<String, String[]> paramOrdPossibleChoicesMS =
                                              new HashMap<String, String[]>();
    /** Predefined group as heartbeat service. */
    private final ResourceAgent hbGroup =
                      new ResourceAgent(Tools.getConfigData().PM_GROUP_NAME,
                                        "",
                                        "group");
    /** Predefined clone as heartbeat service. */
    private final ResourceAgent hbClone;
    /** Predefined drbddisk as heartbeat service. */
    private final ResourceAgent hbDrbddisk =
                        new ResourceAgent("drbddisk", "heartbeat", "heartbeat");
    /** Predefined linbit::drbd as pacemaker service. */
    private final ResourceAgent hbLinbitDrbd =
                                 new ResourceAgent("drbd", "linbit", "ocf");
    /** Mapfrom heartbeat service defined by name and class to the hearbeat
     * service object.
     */
    private final MultiKeyMap serviceToResourceAgentMap = new MultiKeyMap();
    /** Whether drbddisk ra is present. */
    private boolean drbddiskPresent = false;
    /** Whether linbit::drbd ra is present. */
    private boolean linbitDrbdPresent = false;

    /** Boolean parameter type. */
    private static final String PARAM_TYPE_BOOLEAN = "boolean";
    /** Integer parameter type. */
    private static final String PARAM_TYPE_INTEGER = "integer";
    /** String parameter type. */
    private static final String PARAM_TYPE_STRING = "string";
    /** Time parameter type. */
    private static final String PARAM_TYPE_TIME = "time";
    /** Fail count prefix. */
    private static final String FAIL_COUNT_PREFIX = "fail-count-";
    /** Attribute roles. */
    private static final String[] ATTRIBUTE_ROLES = {null,
                                                     "Stopped",
                                                     "Started"};
    /** Atribute roles for master/slave resource. */
    private static final String[] ATTRIBUTE_ROLES_MS = {null,
                                                        "Stopped",
                                                        "Started",
                                                        "Master",
                                                        "Slave"};
    /** Attribute actions. */
    private static final String[] ATTRIBUTE_ACTIONS = {null,
                                                       "start",
                                                       "stop"};
    /** Attribute actions for master/slave. */
    private static final String[] ATTRIBUTE_ACTIONS_MS = {null,
                                                          "start",
                                                          "promote",
                                                          "demote",
                                                          "stop"};
    /** Target role stopped. */
    public static final String TARGET_ROLE_STOPPED = "stopped";
    /** Target role started. */
    private static final String TARGET_ROLE_STARTED = "started";
    /** Target role master. */
    private static final String TARGET_ROLE_MASTER = "master";
    /** Target role slave. */
    private static final String TARGET_ROLE_SLAVE = "slave";
    /** INFINITY keyword. */
    public static final String INFINITY_STRING = "INFINITY";
    /** -INFINITY keyword. */
    public static final String MINUS_INFINITY_STRING = "-INFINITY";
    /** Constraint score keyword. */
    public static final String SCORE_STRING = "score";
    /**
     * Prepares a new <code>CRMXML</code> object.
     */
    public CRMXML(final Host host) {
        super();
        this.host = host;
        String command = null;
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        final String[] booleanValues = getGlobalCheckBoxChoices();
        final String[] integerValues = getIntegerValues();
        final String hbBooleanTrue = booleanValues[0];
        final String hbBooleanFalse = booleanValues[1];
        hbClone = new ResourceAgent(Tools.getConfigData().PM_CLONE_SET_NAME,
                                    "",
                                    "clone");
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
            setMetaAttributes(hbClone, "target_role", "is_managed");
        }
        /* clone-max */
        hbClone.addParameter("clone-max");
        hbClone.setParamIsMetaAttr("clone-max", true);
        hbClone.setParamShortDesc("clone-max", "M/S Clone Max");
        hbClone.setParamDefault("clone-max", "");
        hbClone.setParamPreferred("clone-max", "2");
        hbClone.setParamType("clone-max", PARAM_TYPE_INTEGER);
        hbClone.setParamPossibleChoices("clone-max", integerValues);

        /* clone-node-max */
        hbClone.addParameter("clone-node-max");
        hbClone.setParamIsMetaAttr("clone-node-max", true);
        hbClone.setParamShortDesc("clone-node-max", "M/S Clone Node Max");
        hbClone.setParamDefault("clone-node-max", "1");
        hbClone.setParamType("clone-node-max", PARAM_TYPE_INTEGER);
        hbClone.setParamPossibleChoices("clone-node-max", integerValues);

        /* notify */
        hbClone.addParameter("notify");
        hbClone.setParamIsMetaAttr("notify", true);
        hbClone.setParamShortDesc("notify", "M/S Notify");
        hbClone.setParamDefault("notify", hbBooleanFalse);
        hbClone.setParamPreferred("notify", hbBooleanTrue);
        hbClone.setParamPossibleChoices("notify", booleanValues);
        /* globally-unique */
        hbClone.addParameter("globally-unique");
        hbClone.setParamIsMetaAttr("globally-unique", true);
        hbClone.setParamShortDesc("globally-unique", "M/S Globally-Unique");
        hbClone.setParamDefault("globally-unique", hbBooleanFalse);
        hbClone.setParamPossibleChoices("globally-unique", booleanValues);
        /* ordered */
        hbClone.addParameter("ordered");
        hbClone.setParamIsMetaAttr("ordered", true);
        hbClone.setParamShortDesc("ordered", "M/S Ordered");
        hbClone.setParamDefault("ordered", hbBooleanFalse);
        hbClone.setParamPossibleChoices("ordered", booleanValues);
        /* interleave */
        hbClone.addParameter("interleave");
        hbClone.setParamIsMetaAttr("interleave", true);
        hbClone.setParamShortDesc("interleave", "M/S Interleave");
        hbClone.setParamDefault("interleave", hbBooleanFalse);
        hbClone.setParamPossibleChoices("interleave", booleanValues);

        if (pmV == null && Tools.compareVersions(hbV, "2.1.3") <= 0) {
            command = host.getDistCommand("Heartbeat.2.1.3.getOCFParameters",
                                          (ConvertCmdCallback) null);
        }

        if (command == null
            && pmV == null
            && Tools.compareVersions(hbV, "2.1.4") <= 0) {
            command = host.getDistCommand("Heartbeat.2.1.4.getOCFParameters",
                                          (ConvertCmdCallback) null);
        }

        if (command == null) {
            command = host.getDistCommand("Heartbeat.getOCFParameters",
                                          (ConvertCmdCallback) null);
        }
        final String output =
                    Tools.execCommandProgressIndicator(
                            host,
                            command,
                            null,  /* ExecCallback */
                            false, /* outputVisible */
                            Tools.getString("CRMXML.GetOCFParameters"));
        if (output == null) {
            //Tools.appError("heartbeat ocf output is null");
            return;
        }
        final String[] lines = output.split("\\r?\\n");
        final Pattern pp = Pattern.compile("^provider:\\s*(.*?)\\s*$");
        final Pattern mp = Pattern.compile("^master:\\s*(.*?)\\s*$");
        final Pattern bp = Pattern.compile("^<resource-agent name=\"(.*?)\".*");
        final Pattern ep = Pattern.compile("^</resource-agent>$");
        final StringBuffer xml = new StringBuffer("");
        String provider = null;
        String serviceName = null;
        boolean masterSlave = false; /* is probably m/s ...*/
        for (int i = 0; i < lines.length; i++) {
            //<resource-agent name="AudibleAlarm">
            // ...
            //</resource-agent>
            final Matcher pm = pp.matcher(lines[i]);
            if (pm.matches()) {
                provider = pm.group(1);
                continue;
            }
            final Matcher mm = mp.matcher(lines[i]);
            if (mm.matches()) {
                if ("".equals(mm.group(1))) {
                    masterSlave = false;
                } else {
                    masterSlave = true;
                }
                continue;
            }
            final Matcher m = bp.matcher(lines[i]);
            if (m.matches()) {
                serviceName = m.group(1);
            }
            if (serviceName != null) {
                xml.append(lines[i]);
                xml.append('\n');
                final Matcher m2 = ep.matcher(lines[i]);
                if (m2.matches()) {
                    if ("drbddisk".equals(serviceName)) {
                        drbddiskPresent = true;
                    } else if ("drbd".equals(serviceName)
                               && "linbit".equals(provider)) {
                        linbitDrbdPresent = true;
                    }
                    parseMetaData(serviceName,
                                  provider,
                                  xml.toString(),
                                  masterSlave);
                    serviceName = null;
                    xml.delete(0, xml.length() - 1);
                }
            }
        }
        if (!drbddiskPresent) {
            Tools.appWarning("drbddisk heartbeat script is not present");
        }
        if (!linbitDrbdPresent) {
            Tools.appWarning("linbit::drbd ocf ra is not present");
        }

        /* Hardcoding global params */
        /* symmetric cluster */
        globalParams.add("symmetric-cluster");
        paramGlobalShortDescMap.put("symmetric-cluster", "Symmetric Cluster");
        paramGlobalLongDescMap.put("symmetric-cluster", "Symmetric Cluster");
        paramGlobalTypeMap.put("symmetric-cluster", PARAM_TYPE_BOOLEAN);
        paramGlobalDefaultMap.put("symmetric-cluster", hbBooleanFalse);
        paramGlobalPossibleChoices.put("symmetric-cluster", booleanValues);
        globalRequiredParams.add("symmetric-cluster");

        /* stonith enabled */
        globalParams.add("stonith-enabled");
        paramGlobalShortDescMap.put("stonith-enabled", "Stonith Enabled");
        paramGlobalLongDescMap.put("stonith-enabled", "Stonith Enabled");
        paramGlobalTypeMap.put("stonith-enabled", PARAM_TYPE_BOOLEAN);
        paramGlobalDefaultMap.put("stonith-enabled", hbBooleanTrue);
        //paramGlobalPreferredMap.put("stonith-enabled", hbBooleanFalse);
        paramGlobalPossibleChoices.put("stonith-enabled", booleanValues);
        globalRequiredParams.add("stonith-enabled");

        /* transition timeout */
        globalParams.add("default-action-timeout");
        paramGlobalShortDescMap.put("default-action-timeout",
                                    "Transition Timeout");
        paramGlobalLongDescMap.put("default-action-timeout",
                                "Transition Timeout");
        paramGlobalTypeMap.put("default-action-timeout", PARAM_TYPE_INTEGER);
        paramGlobalDefaultMap.put("default-action-timeout", "20s");
        paramGlobalPossibleChoices.put("default-action-timeout", integerValues);
        globalRequiredParams.add("default-action-timeout");

        /* resource stickiness */
        globalParams.add("default-resource-stickiness");
        paramGlobalShortDescMap.put("default-resource-stickiness",
                                    "Resource Stickiness");
        paramGlobalLongDescMap.put("default-resource-stickiness",
                                   "Resource Stickiness");
        paramGlobalTypeMap.put("default-resource-stickiness",
                               PARAM_TYPE_INTEGER);
        paramGlobalPossibleChoices.put("default-resource-stickiness",
                                       integerValues);
        paramGlobalDefaultMap.put("default-resource-stickiness", "0");
        //paramGlobalPreferredMap.put("default-resource-stickiness", "100");
        globalRequiredParams.add("default-resource-stickiness");

        /* no quorum policy */
        globalParams.add("no-quorum-policy");
        paramGlobalShortDescMap.put("no-quorum-policy", "No Quorum Policy");
        paramGlobalLongDescMap.put("no-quorum-policy", "No Quorum Policy");
        // TODO: ignore, stop, freeze, there is more
        paramGlobalTypeMap.put("no-quorum-policy", PARAM_TYPE_STRING);
        paramGlobalDefaultMap.put("no-quorum-policy", "stop");
        paramGlobalPossibleChoices.put("no-quorum-policy",
                                       new String[]{"ignore",
                                                    "stop",
                                                    "freeze"});
        globalRequiredParams.add("no-quorum-policy");

        /* resource failure stickiness */
        globalParams.add("default-resource-failure-stickiness");
        paramGlobalShortDescMap.put("default-resource-failure-stickiness",
                                    "Resource Failure Stickiness");
        paramGlobalLongDescMap.put("default-resource-failure-stickiness",
                                   "Resource Failure Stickiness");
        paramGlobalTypeMap.put("default-resource-failure-stickiness",
                               PARAM_TYPE_INTEGER);
        paramGlobalPossibleChoices.put("default-resource-failure-stickiness",
                                       integerValues);
        paramGlobalDefaultMap.put("default-resource-failure-stickiness", "0");
        globalRequiredParams.add("default-resource-failure-stickiness");


        if (pmV != null || Tools.compareVersions(hbV, "2.1.3") >= 0) {
            final String[] params = {
                "stonith-action",
                "is-managed-default",
                "cluster-delay",
                "batch-limit",
                "stop-orphan-resources",
                "stop-orphan-actions",
                "remove-after-stop",
                "pe-error-series-max",
                "pe-warn-series-max",
                "pe-input-series-max",
                "startup-fencing",
                "start-failure-is-fatal",
                "dc_deadtime",
                "cluster_recheck_interval",
                "election_timeout",
                "shutdown_escalation",
                "crmd-integration-timeout",
                "crmd-finalization-timeout"
            };

            for (String param : params) {
                globalParams.add(param);
                String[] parts = param.split("[-_]");
                for (int i = 0; i < parts.length; i++) {
                    if ("dc".equals(parts[i])) {
                        parts[i] = "DC";
                    }
                    if ("crmd".equals(parts[i])) {
                        parts[i] = "CRMD";
                    } else {
                        parts[i] = Tools.ucfirst(parts[i]);
                    }
                }
                final String name = Tools.join(" ", parts);
                paramGlobalShortDescMap.put(param, name);
                paramGlobalLongDescMap.put(param, name);
                paramGlobalTypeMap.put(param, PARAM_TYPE_STRING);
                paramGlobalDefaultMap.put(param, "");
            }
            paramGlobalDefaultMap.put("stonith-action", "reboot");
            paramGlobalPossibleChoices.put("stonith-action",
                                           new String[]{"reboot", "poweroff"});

            paramGlobalTypeMap.put("is-managed-default", PARAM_TYPE_BOOLEAN);
            paramGlobalDefaultMap.put("is-managed-default", hbBooleanFalse);
            paramGlobalPossibleChoices.put("is-managed-default", booleanValues);

            paramGlobalTypeMap.put("stop-orphan-resources", PARAM_TYPE_BOOLEAN);
            paramGlobalDefaultMap.put("stop-orphan-resources",
                                                            hbBooleanFalse);
            paramGlobalPossibleChoices.put("stop-orphan-resources",
                                           booleanValues);

            paramGlobalTypeMap.put("stop-orphan-actions", PARAM_TYPE_BOOLEAN);
            paramGlobalDefaultMap.put("stop-orphan-actions", hbBooleanFalse);
            paramGlobalPossibleChoices.put("stop-orphan-actions",
                                           booleanValues);

            paramGlobalTypeMap.put("remove-after-stop", PARAM_TYPE_BOOLEAN);
            paramGlobalDefaultMap.put("remove-after-stop", hbBooleanFalse);
            paramGlobalPossibleChoices.put("remove-after-stop", booleanValues);

            paramGlobalTypeMap.put("startup-fencing", PARAM_TYPE_BOOLEAN);
            paramGlobalDefaultMap.put("startup-fencing", hbBooleanFalse);
            paramGlobalPossibleChoices.put("startup-fencing", booleanValues);

            paramGlobalTypeMap.put("start-failure-is-fatal",
                                   PARAM_TYPE_BOOLEAN);
            paramGlobalDefaultMap.put("start-failure-is-fatal",
                                      hbBooleanFalse);
            paramGlobalPossibleChoices.put("start-failure-is-fatal",
                                           booleanValues);
        }

        /* Hardcoding colocation params */
        colParams.add("with-rsc-role");
        paramColShortDescMap.put("with-rsc-role", "rsc1 col role");
        paramColLongDescMap.put("with-rsc-role", "@WITH-RSC@ colocation role");
        paramColTypeMap.put("with-rsc-role", PARAM_TYPE_STRING);
        paramColPossibleChoices.put("with-rsc-role", ATTRIBUTE_ROLES);
        paramColPossibleChoicesMS.put("with-rsc-role", ATTRIBUTE_ROLES_MS);

        colParams.add("rsc-role");
        paramColShortDescMap.put("rsc-role", "rsc2 col role");
        paramColLongDescMap.put("rsc-role", "@RSC@ colocation role");
        paramColTypeMap.put("rsc-role", PARAM_TYPE_STRING);
        paramColPossibleChoices.put("rsc-role", ATTRIBUTE_ROLES);
        paramColPossibleChoicesMS.put("rsc-role", ATTRIBUTE_ROLES_MS);

        colParams.add(SCORE_STRING);
        paramColShortDescMap.put(SCORE_STRING, "Score");
        paramColLongDescMap.put(SCORE_STRING, "Score");
        paramColTypeMap.put(SCORE_STRING, PARAM_TYPE_INTEGER);
        paramColDefaultMap.put(SCORE_STRING, null);
        //paramColPreferredMap.put(SCORE_STRING, INFINITY_STRING);
        paramColPossibleChoices.put(SCORE_STRING, integerValues);
        /* Hardcoding order params */
        ordParams.add("first-action");
        paramOrdShortDescMap.put("first-action", "rsc1 order action");
        paramOrdLongDescMap.put("first-action", "@FIRST-RSC@ order action");
        paramOrdTypeMap.put("first-action", PARAM_TYPE_STRING);
        paramOrdPossibleChoices.put("first-action", ATTRIBUTE_ACTIONS);
        paramOrdPossibleChoicesMS.put("first-action", ATTRIBUTE_ACTIONS_MS);
        paramOrdDefaultMap.put("first-action", null);

        ordParams.add("then-action");
        paramOrdShortDescMap.put("then-action", "rsc2 order action");
        paramOrdLongDescMap.put("then-action", "@THEN-RSC@ order action");
        paramOrdTypeMap.put("then-action", PARAM_TYPE_STRING);
        paramOrdPossibleChoices.put("then-action", ATTRIBUTE_ACTIONS);
        paramOrdPossibleChoicesMS.put("then-action", ATTRIBUTE_ACTIONS_MS);
        paramOrdDefaultMap.put("then-action", null);

        ordParams.add("symmetrical");
        paramOrdShortDescMap.put("symmetrical", "Symmetrical");
        paramOrdLongDescMap.put("symmetrical", "Symmetrical");
        paramOrdTypeMap.put("symmetrical", PARAM_TYPE_BOOLEAN);
        paramOrdDefaultMap.put("symmetrical", hbBooleanTrue);
        paramOrdPossibleChoices.put("symmetrical", booleanValues);

        ordParams.add(SCORE_STRING);
        paramOrdShortDescMap.put(SCORE_STRING, "Score");
        paramOrdLongDescMap.put(SCORE_STRING, "Score");
        paramOrdTypeMap.put(SCORE_STRING, PARAM_TYPE_INTEGER);
        //paramOrdPreferredMap.put(SCORE_STRING, INFINITY_STRING);
        paramOrdPossibleChoices.put(SCORE_STRING, integerValues);
        paramOrdDefaultMap.put(SCORE_STRING, null);
    }

    /**
     * Returns choices for check boxes in the global config. (True, False).
     */
    public final String[] getGlobalCheckBoxChoices() {
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        if (pmV != null || Tools.compareVersions(hbV, "2.1.3") >= 0) {
            return new String[]{
                Tools.getString("Heartbeat.2.1.3.Boolean.True"),
                Tools.getString("Heartbeat.2.1.3.Boolean.False")};
        } else {
            return new String[]{
                Tools.getString("Heartbeat.Boolean.True"),
                Tools.getString("Heartbeat.Boolean.False")};
        }
    }

    /**
     * Returns choices for integer fields.
     */
    public final String[] getIntegerValues() {
        return new String[]{null,
                            "0",
                            "2",
                            "100",
                            INFINITY_STRING,
                            MINUS_INFINITY_STRING};
    }


    /**
     * Returns choices for check box. (True, False).
     * The problem is, that heartbeat kept changing the lower and upper case in
     * the true and false values.
     */
    public final String[] getCheckBoxChoices(final ResourceAgent ra,
                                             final String param) {
        final String paramDefault = getParamDefault(ra, param);
        if (paramDefault != null) {
            if ("yes".equals(paramDefault) || "no".equals(paramDefault)) {
                return new String[]{"yes", "no"};
            } else if ("Yes".equals(paramDefault)
                       || "No".equals(paramDefault)) {
                return new String[]{"Yes", "No"};
            } else if ("true".equals(paramDefault)
                       || "false".equals(paramDefault)) {
                return new String[]{"true", "false"};
            } else if ("True".equals(paramDefault)
                       || "False".equals(paramDefault)) {
                return new String[]{"True", "False"};
            }
        }
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        if (pmV != null || Tools.compareVersions(hbV, "2.1.3") >= 0) {
            return new String[]{
                Tools.getString("Heartbeat.2.1.3.Boolean.True"),
                Tools.getString("Heartbeat.2.1.3.Boolean.False")};
        } else {
            return new String[]{
                Tools.getString("Heartbeat.Boolean.True"),
                Tools.getString("Heartbeat.Boolean.False")};
        }
    }

    /**
     * Returns all services as array of strings, sorted, with filesystem and
     * ipaddr in the begining.
     */
    public final List<ResourceAgent> getServices(final String cl) {
        final List<ResourceAgent> services = classToServicesMap.get(cl);
        if (services == null) {
            return new ArrayList<ResourceAgent>();
        }
        Collections.sort(services,
                         new Comparator<ResourceAgent>() {
                              public int compare(final ResourceAgent s1,
                                                 final ResourceAgent s2) {
                                  return s1.getName().compareToIgnoreCase(
                                                                s2.getName());
                              }
                         });
        return services;
    }

    /**
     * Returns parameters for service. Parameters are obtained from
     * ocf meta-data.
     */
    public final String[] getParameters(final ResourceAgent ra) {
        /* return cached values */
        return ra.getParameters();
    }

    /**
     * Returns global parameters.
     */
    public final String[] getGlobalParameters() {
        if (globalParams != null) {
            return globalParams.toArray(new String[globalParams.size()]);
        }
        return null;
    }

    /**
     * Return version of the service ocf script.
     */
    public final String getVersion(final ResourceAgent ra) {
        return ra.getVersion();
    }

    /**
     * Return short description of the service.
     */
    public final String getShortDesc(final ResourceAgent ra) {
        return ra.getShortDesc();
    }

    /**
     * Return long description of the service.
     */
    public final String getLongDesc(final ResourceAgent ra) {
        return ra.getLongDesc();
    }

    /**
     * Returns short description of the global parameter.
     */
    public final String getGlobalParamShortDesc(final String param) {
        String shortDesc = paramGlobalShortDescMap.get(param);
        if (shortDesc == null) {
            shortDesc = param;
        }
        return shortDesc;
    }

    /**
     * Returns short description of the service parameter.
     */
    public final String getParamShortDesc(final ResourceAgent ra,
                                          final String param) {
        return ra.getParamShortDesc(param);
    }

    /**
     * Returns long description of the global parameter.
     */
    public final String getGlobalParamLongDesc(final String param) {
        final String shortDesc = getGlobalParamShortDesc(param);
        String longDesc = paramGlobalLongDescMap.get(param);
        if (longDesc == null) {
            longDesc = "";
        }
        return Tools.html("<b>" + shortDesc + "</b>\n" + longDesc);
    }

    /**
     * Returns long description of the parameter and service.
     */
    public final String getParamLongDesc(final ResourceAgent ra,
                                         final String param) {
        final String shortDesc = getParamShortDesc(ra, param);
        String longDesc = ra.getParamLongDesc(param);
        if (longDesc == null) {
            longDesc = "";
        }
        return Tools.html("<b>" + shortDesc + "</b>\n" + longDesc);
    }

    /**
     * Returns type of a global parameter. It can be string, integer, boolean...
     */
    public final String getGlobalParamType(final String param) {
        return paramGlobalTypeMap.get(param);
    }

    /**
     * Returns type of the parameter. It can be string, integer, boolean...
     */
    public final String getParamType(final ResourceAgent ra,
                                     final String param) {
        return ra.getParamType(param);
    }

    /**
     * Returns default value for the global parameter.
     */
    public final String getGlobalParamDefault(final String param) {
        return paramGlobalDefaultMap.get(param);
    }

    /**
     * Returns the preferred value for the global parameter.
     */
    public final String getGlobalParamPreferred(final String param) {
        return paramGlobalPreferredMap.get(param);
    }

    /**
     * Returns the preferred value for this parameter.
     */
    public final String getParamPreferred(final ResourceAgent ra,
                                          final String param) {
        return ra.getParamPreferred(param);
    }

    /**
     * Returns default value for this parameter.
     */
    public final String getParamDefault(final ResourceAgent ra,
                                        final String param) {
        return ra.getParamDefault(param);
    }

    /**
     * Returns possible choices for a global parameter, that will be displayed
     * in the combo box.
     */
    public final String[] getGlobalParamPossibleChoices(final String param) {
        return paramGlobalPossibleChoices.get(param);
    }

    /**
     * Returns possible choices for a parameter, that will be displayed in
     * the combo box.
     */
    public final String[] getParamPossibleChoices(final ResourceAgent ra,
                                                  final String param) {
        return ra.getParamPossibleChoices(param);
    }

    /**
     * Checks if parameter is required or not.
     */
    public final boolean isGlobalRequired(final String param) {
        return globalRequiredParams.contains(param);
    }

    /**
     * Checks if parameter is required or not.
     */
    public final boolean isRequired(final ResourceAgent ra,
                                    final String param) {
        return ra.isRequired(param);
    }

    /**
     * Returns whether the parameter is meta attribute or not.
     */
    public final boolean isMetaAttr(final ResourceAgent ra,
                                    final String param) {
        return ra.isParamMetaAttr(param);
    }

    /**
     * Returns whether the parameter expects an integer value.
     */
    public final boolean isInteger(final ResourceAgent ra,
                                   final String param) {
        final String type = getParamType(ra, param);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    /**
     * Returns whether the parameter expects a boolean value.
     */
    public final boolean isBoolean(final ResourceAgent ra,
                                   final String param) {
        final String type = getParamType(ra, param);
        return PARAM_TYPE_BOOLEAN.equals(type);
    }

    /**
     * Returns whether the global parameter expects an integer value.
     */
    public final boolean isGlobalInteger(final String param) {
        final String type = getGlobalParamType(param);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    /**
     * Returns whether the global parameter expects a boolean value.
     */
    public final boolean isGlobalBoolean(final String param) {
        final String type = getGlobalParamType(param);
        return PARAM_TYPE_BOOLEAN.equals(type);
    }

    /**
     * Whether the service parameter is of the time type.
     */
    public final boolean isTimeType(final ResourceAgent ra,
                                    final String param) {
        final String type = getParamType(ra, param);
        return PARAM_TYPE_TIME.equals(type);
    }

    /**
     * Whether the global parameter is of the time type.
     */
    public final boolean isGlobalTimeType(final String param) {
        final String type = getGlobalParamType(param);
        return PARAM_TYPE_TIME.equals(type);
    }


    /**
     * Returns name of the section for service and parameter that will be
     * displayed.
     */
    public final String getSection(final ResourceAgent ra, final String param) {
        if (isMetaAttr(ra, param)) {
            return Tools.getString("CRMXML.MetaAttrOptions");
        } else if (isRequired(ra, param)) {
            return Tools.getString("CRMXML.RequiredOptions");
        } else {
            return Tools.getString("CRMXML.OptionalOptions");
        }
    }

    /**
     * Returns name of the section global parameter that will be
     * displayed.
     */
    public final String getGlobalSection(final String param) {
        if (isGlobalRequired(param)) {
            return Tools.getString("CRMXML.RequiredOptions");
        } else {
            return Tools.getString("CRMXML.OptionalOptions");
        }
    }

    /**
     * Checks parameter according to its type. Returns false if value does
     * not fit the type.
     */
    public final boolean checkParam(final ResourceAgent ra,
                                    final String param,
                                    final String value) {
        final String type = getParamType(ra, param);
        boolean correctValue = true;
        if (PARAM_TYPE_BOOLEAN.equals(type)) {
            if (!"yes".equals(value) && !"no".equals(value)
                && !Tools.getString("Heartbeat.Boolean.True").equals(value)
                && !Tools.getString("Heartbeat.Boolean.False").equals(value)
                && !Tools.getString(
                                "Heartbeat.2.1.3.Boolean.True").equals(value)
                && !Tools.getString(
                            "Heartbeat.2.1.3.Boolean.False").equals(value)) {
                correctValue = false;
            }
        } else if (PARAM_TYPE_INTEGER.equals(type)) {
            final Pattern p =
                        Pattern.compile("^-?(\\d*|" + INFINITY_STRING + ")$");
            final Matcher m = p.matcher(value);
            if (!m.matches()) {
                correctValue = false;
            }
        } else if (PARAM_TYPE_TIME.equals(type)) {
            final Pattern p =
                Pattern.compile("^-?\\d*(ms|msec|us|usec|s|sec|m|min|h|hr)?$");
            final Matcher m = p.matcher(value);
            if (!m.matches()) {
                correctValue = false;
            }
        } else if ((value == null || "".equals(value))
                   && isRequired(ra, param)) {
            correctValue = false;
        }
        return correctValue;
    }

    /**
     * Checks global parameter according to its type. Returns false if value
     * does not fit the type.
     */
    public final boolean checkGlobalParam(final String param,
                                          final String value) {
        final String type = getGlobalParamType(param);
        boolean correctValue = true;
        if (PARAM_TYPE_BOOLEAN.equals(type)) {
            if (!"yes".equals(value) && !"no".equals(value)
                && !Tools.getString("Heartbeat.Boolean.True").equals(value)
                && !Tools.getString("Heartbeat.Boolean.False").equals(value)
                && !Tools.getString(
                                "Heartbeat.2.1.3.Boolean.True").equals(value)
                && !Tools.getString(
                              "Heartbeat.2.1.3.Boolean.False").equals(value)) {

                correctValue = false;
            }
        } else if (PARAM_TYPE_INTEGER.equals(type)) {
            final Pattern p =
                        Pattern.compile("^-?(\\d*|" + INFINITY_STRING + ")$");
            final Matcher m = p.matcher(value);
            if (!m.matches()) {
                correctValue = false;
            }
        } else if (PARAM_TYPE_TIME.equals(type)) {
            final Pattern p =
                Pattern.compile("^-?\\d*(ms|msec|us|usec|s|sec|m|min|h|hr)?$");
            final Matcher m = p.matcher(value);
            if (!m.matches()) {
                correctValue = false;
            }
        } else if ((value == null || "".equals(value))
                   && isGlobalRequired(param)) {
            correctValue = false;
        }
        return correctValue;
    }

    /**
     * Sets meta attributes for resource agent.
     */
    private void setMetaAttributes(final ResourceAgent ra,
                                   final String targetRoleParam,
                                   final String isManagedParam) {
        ra.addParameter(targetRoleParam);
        // TODO: Master, Slave
        ra.setParamPossibleChoices(targetRoleParam,
                      new String[]{TARGET_ROLE_STARTED, TARGET_ROLE_STOPPED});
        ra.setParamIsMetaAttr(targetRoleParam, true);
        ra.setParamRequired(targetRoleParam, false);
        ra.setParamShortDesc(targetRoleParam,
                             Tools.getString("CRMXML.TargetRole.ShortDesc"));
        ra.setParamLongDesc(targetRoleParam,
                            Tools.getString("CRMXML.TargetRole.LongDesc"));
        // TODO: default is different in some prev hb */
        ra.setParamDefault(targetRoleParam, TARGET_ROLE_STARTED);

        ra.addParameter(isManagedParam);
        ra.setParamPossibleChoices(isManagedParam,
                                   new String[]{"true", "false"});
        ra.setParamIsMetaAttr(isManagedParam, true);
        ra.setParamRequired(isManagedParam, true);
        ra.setParamShortDesc(isManagedParam,
                             Tools.getString("CRMXML.IsManaged.ShortDesc"));
        ra.setParamLongDesc(
                                 isManagedParam,
                                 Tools.getString("CRMXML.IsManaged.LongDesc"));
        ra.setParamDefault(isManagedParam, "true");

    }

    /**
     * Parses the parameters.
     */
    private void parseParameters(final ResourceAgent ra,
                                 final Node parametersNode) {
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
            setMetaAttributes(ra, "target_role", "is_managed");
        } else {
            setMetaAttributes(ra, "target-role", "is-managed");
        }
        final NodeList parameters = parametersNode.getChildNodes();
        for (int i = 0; i < parameters.getLength(); i++) {
            final Node parameterNode = parameters.item(i);
            if (parameterNode.getNodeName().equals("parameter")) {
                final String param = getAttribute(parameterNode, "name");
                final String required = getAttribute(parameterNode, "required");
                ra.addParameter(param);

                if (required != null && required.equals("1")) {
                    ra.setParamRequired(param, true);
                }

                /* <longdesc lang="en"> */
                final Node longdescParamNode = getChildNode(parameterNode,
                                                            "longdesc");
                if (longdescParamNode != null) {
                    final String longDesc = getText(longdescParamNode);
                    ra.setParamLongDesc(param, longDesc);
                }

                /* <shortdesc lang="en"> */
                final Node shortdescParamNode = getChildNode(parameterNode,
                                                             "shortdesc");
                if (shortdescParamNode != null) {
                    final String shortDesc = getText(shortdescParamNode);
                    ra.setParamShortDesc(param, shortDesc);
                }

                /* <content> */
                final Node contentParamNode = getChildNode(parameterNode,
                                                           "content");
                if (contentParamNode != null) {
                    final String type = getAttribute(contentParamNode, "type");
                    final String defaultValue = getAttribute(contentParamNode,
                                                                    "default");

                    ra.setParamType(param, type);
                    ra.setParamDefault(param, defaultValue);
                }
            }
        }
    }

    /**
     * Parses the actions node.
     */
    private void parseActions(final ResourceAgent ra,
                              final Node actionsNode) {
        final NodeList actions = actionsNode.getChildNodes();
        for (int i = 0; i < actions.getLength(); i++) {
            final Node actionNode = actions.item(i);
            if (actionNode.getNodeName().equals("action")) {
                final String name = getAttribute(actionNode, "name");
                final String depth = getAttribute(actionNode, "depth");
                final String timeout = getAttribute(actionNode, "timeout");
                final String interval = getAttribute(actionNode, "interval");
                final String startDelay = getAttribute(actionNode,
                                                               "start-delay");
                final String role = getAttribute(actionNode, "role");
                ra.addOperationDefault(name, "depth", depth);
                ra.addOperationDefault(name, "timeout", timeout);
                ra.addOperationDefault(name, "interval", interval);
                ra.addOperationDefault(name, "start-delay", startDelay);
                ra.addOperationDefault(name, "role", role);
            }
        }
    }

    /**
     * Parses meta-data xml for parameters for service and fills up the hashes
     * "CRM Daemon"s are global config options.
     */
    public final void parseMetaData(final String serviceName,
                                    final String provider,
                                    final String xml,
                                    final boolean masterSlave) {
        final Document document = getXMLDocument(xml);
        if (document == null) {
            return;
        }

        /* get root <resource-agent> */
        final Node raNode = getChildNode(document, "resource-agent");
        if (raNode == null) {
            return;
        }

        /* class */
        String resourceClass = getAttribute(raNode, "class");
        if (resourceClass == null) {
            resourceClass = "ocf";
        }
        List<ResourceAgent> raList = classToServicesMap.get(resourceClass);
        if (raList == null) {
            raList = new ArrayList<ResourceAgent>();
            classToServicesMap.put(resourceClass, raList);
        }
        ResourceAgent ra;
        if ("drbddisk".equals(serviceName)
            && "heartbeat".equals(resourceClass)) {
            ra = hbDrbddisk;
        } else if ("drbd".equals(serviceName)
                   && "ocf".equals(resourceClass)
                   && "linbit".equals(provider)) {
            ra = hbLinbitDrbd;
        } else {
            ra = new ResourceAgent(serviceName, provider, resourceClass);
        }
        serviceToResourceAgentMap.put(serviceName,
                                      provider,
                                      resourceClass,
                                      ra);
        raList.add(ra);

        /* <version> */
        final Node versionNode = getChildNode(raNode, "version");
        if (versionNode != null) {
            ra.setVersion(getText(versionNode));
        }

        /* <longdesc lang="en"> */
        final Node longdescNode = getChildNode(raNode, "longdesc");
        if (longdescNode != null) {
            ra.setLongDesc(getText(longdescNode));
        }

        /* <shortdesc lang="en"> */
        final Node shortdescNode = getChildNode(raNode, "shortdesc");
        if (shortdescNode != null) {
            ra.setShortDesc(getText(shortdescNode));
        }

        /* <parameters> */
        final Node parametersNode = getChildNode(raNode, "parameters");
        if (parametersNode != null) {
            parseParameters(ra, parametersNode);
        }
        /* <actions> */
        final Node actionsNode = getChildNode(raNode, "actions");
        if (actionsNode != null) {
            parseActions(ra, actionsNode);
        }
        ra.setMasterSlave(masterSlave);
    }

    /**
     * Parses crm meta data, only to get long descriptions and default values
     * for advanced options.
     * Strange stuff
     *
     * which can be pengine or crmd
     */
    public final void parseClusterMetaData(final String xml) {
        final Document document = getXMLDocument(xml);
        if (document == null) {
            return;
        }

        /* get root <metadata> */
        final Node metadataNode = getChildNode(document, "metadata");
        if (metadataNode == null) {
            return;
        }

        /* get <resource-agent> */
        final NodeList resAgents = metadataNode.getChildNodes();
        final String[] booleanValues = getGlobalCheckBoxChoices();
        final String[] integerValues = getIntegerValues();
        for (int i = 0; i < resAgents.getLength(); i++) {
            final Node resAgentNode = resAgents.item(i);
            if (!resAgentNode.getNodeName().equals("resource-agent")) {
                continue;
            }

            /* <parameters> */
            final Node parametersNode = getChildNode(resAgentNode,
                                                     "parameters");
            if (parametersNode == null) {
                return;
            }

            final NodeList parameters = parametersNode.getChildNodes();
            for (int j = 0; j < parameters.getLength(); j++) {
                final Node parameterNode = parameters.item(j);
                if (parameterNode.getNodeName().equals("parameter")) {
                    final String param = getAttribute(parameterNode, "name");
                    final String required =
                                        getAttribute(parameterNode, "required");
                    if (!globalParams.contains(param)) {
                        globalParams.add(param);
                    }
                    if (required != null && required.equals("1")
                        && !globalRequiredParams.contains(param)) {
                        globalRequiredParams.add(param);
                    }

                    /* <longdesc lang="en"> */
                    final Node longdescParamNode = getChildNode(parameterNode,
                                                                "longdesc");
                    if (longdescParamNode != null) {
                        final String longDesc = getText(longdescParamNode);
                        paramGlobalLongDescMap.put(param, longDesc);
                    }

                    /* <content> */
                    final Node contentParamNode = getChildNode(parameterNode,
                                                               "content");
                    if (contentParamNode != null) {
                        final String type = getAttribute(contentParamNode,
                                                         "type");
                        final String defaultValue =
                                                getAttribute(contentParamNode,
                                                             "default");

                        paramGlobalTypeMap.put(param, type);
                        if (!"expected-quorum-votes".equals(param)) {
                            // TODO: workaround
                            paramGlobalDefaultMap.put(param, defaultValue);
                        }
                        if (PARAM_TYPE_BOOLEAN.equals(type)) {
                            paramGlobalPossibleChoices.put(param,
                                                           booleanValues);
                        }
                        if (PARAM_TYPE_INTEGER.equals(type)) {
                            paramGlobalPossibleChoices.put(param,
                                                           integerValues);
                        }
                    }
                }
            }
        }
        /* stonith timeout, workaround, because of param type comming wrong
         * from pacemaker */
        paramGlobalTypeMap.put("stonith-timeout", PARAM_TYPE_TIME);

    }

    /**
     * Returns the heartbeat service object for the specified service name and
     * heartbeat class.
     */
    public final ResourceAgent getResourceAgent(final String serviceName,
                                                final String provider,
                                                final String raClass) {
        return (ResourceAgent) serviceToResourceAgentMap.get(serviceName,
                                                             provider,
                                                             raClass);
    }

    /**
     * Returns the heartbeat service object of the drbddisk service.
     */
    public final ResourceAgent getHbDrbddisk() {
        return hbDrbddisk;
    }

    /**
     * Returns the heartbeat service object of the linbit::drbd service.
     */
    public final ResourceAgent getHbLinbitDrbd() {
        return hbLinbitDrbd;
    }

    /**
     * Returns the heartbeat service object of the hearbeat group.
     */
    public final ResourceAgent getHbGroup() {
        return hbGroup;
    }

    /**
     * Returns the heartbeat service object of the hearbeat clone set.
     */
    public final ResourceAgent getHbClone() {
        return hbClone;
    }

    /**

    /**
     * Parses attributes, operations etc. from primitives and clones.
     */
    private void parseAttributes(
              final Node resourceNode,
              final String crmId,
              final Map<String, Map<String, String>> parametersMap,
              final Map<String, Map<String, String>> parametersNvpairsIdsMap,
              final Map<String, String> resourceInstanceAttrIdMap,
              final MultiKeyMap operationsMap,
              final Map<String, String> operationsIdMap,
              final Map<String, Map<String, String>> resOpIdsMap,
              final Map<String, String> operationsIdRefs,
              final Map<String, String> operationsIdtoCRMId,
              final Map<String, String> metaAttrsIdRefs,
              final Map<String, String> metaAttrsIdToCRMId) {
        final Map<String, String> params =
                                        new HashMap<String, String>();
        parametersMap.put(crmId, params);
        final Map<String, String> nvpairIds =
                                        new HashMap<String, String>();
        parametersNvpairsIdsMap.put(crmId, nvpairIds);
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        /* <instance_attributes> */
        final Node instanceAttrNode =
                                   getChildNode(resourceNode,
                                                "instance_attributes");
        /* <nvpair...> */
        if (instanceAttrNode != null) {
            final String iAId = getAttribute(instanceAttrNode, "id");
            resourceInstanceAttrIdMap.put(crmId, iAId);
            NodeList nvpairsRes;
            if (pmV == null
                && hbV != null
                && Tools.compareVersions(hbV, "2.99.0") < 0) {
                /* <attributtes> only til 2.1.4 */
                final Node attrNode = getChildNode(instanceAttrNode,
                                                   "attributes");
                nvpairsRes = attrNode.getChildNodes();
            } else {
                nvpairsRes = instanceAttrNode.getChildNodes();
            }
            for (int j = 0; j < nvpairsRes.getLength(); j++) {
                final Node optionNode = nvpairsRes.item(j);
                if (optionNode.getNodeName().equals("nvpair")) {
                    final String nvpairId = getAttribute(optionNode, "id");
                    final String name = getAttribute(optionNode, "name");
                    final String value = getAttribute(optionNode, "value");
                    params.put(name, value);
                    nvpairIds.put(name, nvpairId);
                }
            }
        }

        /* <operations> */
        final Node operationsNode = getChildNode(resourceNode, "operations");
        if (operationsNode != null) {
            final String operationsIdRef = getAttribute(operationsNode,
                                                        "id-ref");
            if (operationsIdRef != null) {
                operationsIdRefs.put(crmId, operationsIdRef);
            } else {
                final String operationsId = getAttribute(operationsNode, "id");
                operationsIdMap.put(crmId, operationsId);
                operationsIdtoCRMId.put(operationsId, crmId);
                final Map<String, String> opIds = new HashMap<String, String>();
                resOpIdsMap.put(crmId, opIds);
                /* <op> */
                final NodeList ops = operationsNode.getChildNodes();
                for (int k = 0; k < ops.getLength(); k++) {
                    final Node opNode = ops.item(k);
                    if (opNode.getNodeName().equals("op")) {
                        final String opId = getAttribute(opNode, "id");
                        final String name = getAttribute(opNode, "name");
                        final String timeout = getAttribute(opNode, "timeout");
                        final String interval = getAttribute(opNode,
                                                             "interval");
                        final String startDelay = getAttribute(opNode,
                                                               "start-delay");
                        operationsMap.put(crmId, name, "interval", interval);
                        operationsMap.put(crmId, name, "timeout", timeout);
                        operationsMap.put(crmId,
                                          name,
                                          "start-delay",
                                          startDelay);
                        opIds.put(name, opId);
                    }
                }
            }
        }

        /* <meta_attributtes> */
        final Node metaAttrsNode = getChildNode(resourceNode,
                                                "meta_attributes");
        if (metaAttrsNode != null) {
            final String metaAttrsIdRef = getAttribute(metaAttrsNode, "id-ref");
            if (metaAttrsIdRef != null) {
                metaAttrsIdRefs.put(crmId, metaAttrsIdRef);
            } else {
                final String opId = getAttribute(metaAttrsNode, "id");
                /* <attributtes> only til 2.1.4 */
                NodeList nvpairsMA;
                if (hbV != null && Tools.compareVersions(hbV, "2.99.0") < 0) {
                    final Node attrsNode =
                                      getChildNode(metaAttrsNode, "attributes");
                    nvpairsMA = attrsNode.getChildNodes();
                } else {
                    nvpairsMA = metaAttrsNode.getChildNodes();
                }
                /* <nvpair...> */
                /* target-role and is-managed */
                for (int l = 0; l < nvpairsMA.getLength(); l++) {
                    final Node maNode = nvpairsMA.item(l);
                    if (maNode.getNodeName().equals("nvpair")) {
                        final String nvpairId = getAttribute(maNode, "id");
                        final String name = getAttribute(maNode, "name");
                        final String value = getAttribute(maNode, "value");
                        params.put(name, value);
                        nvpairIds.put(name, nvpairId);
                    }
                }
            }
        }
    }

    /**
     * Parses the "group" node.
     */
    private void parseGroup(
                final Node groupNode,
                final List<String> resList,
                final Map<String, List<String>> groupsToResourcesMap,
                final Map<String, Map<String, String>> parametersMap,
                final Map<String, ResourceAgent> resourceTypeMap,
                final Map<String, Map<String, String>> parametersNvpairsIdsMap,
                final Map<String, String> resourceInstanceAttrIdMap,
                final MultiKeyMap operationsMap,
                final Map<String, String> operationsIdMap,
                final Map<String, Map<String, String>> resOpIdsMap,
                final Map<String, String> operationsIdRefs,
                final Map<String, String> operationsIdtoCRMId,
                final Map<String, String> metaAttrsIdRefs,
                final Map<String, String> metaAttrsIdToCRMId) {
        final NodeList primitives = groupNode.getChildNodes();
        final String groupId = getAttribute(groupNode, "id");
        if (resList != null) {
            resList.add(groupId);
        }
        parametersMap.put(groupId, new HashMap<String, String>());
        List<String> groupResList = groupsToResourcesMap.get(groupId);
        if (groupResList == null) {
            groupResList = new ArrayList<String>();
            groupsToResourcesMap.put(groupId, groupResList);
        }

        for (int j = 0; j < primitives.getLength(); j++) {
            final Node primitiveNode = primitives.item(j);
            if (primitiveNode.getNodeName().equals("primitive")) {
                parsePrimitive(primitiveNode,
                               groupResList,
                               resourceTypeMap,
                               parametersMap,
                               parametersNvpairsIdsMap,
                               resourceInstanceAttrIdMap,
                               operationsMap,
                               operationsIdMap,
                               resOpIdsMap,
                               operationsIdRefs,
                               operationsIdtoCRMId,
                               metaAttrsIdRefs,
                               metaAttrsIdToCRMId);
            }
        }
    }

    /**
     * Parses the "primitive" node.
     */
    private void parsePrimitive(
                final Node primitiveNode,
                final List<String> groupResList,
                final Map<String, ResourceAgent> resourceTypeMap,
                final Map<String, Map<String, String>> parametersMap,
                final Map<String, Map<String, String>> parametersNvpairsIdsMap,
                final Map<String, String> resourceInstanceAttrIdMap,
                final MultiKeyMap operationsMap,
                final Map<String, String> operationsIdMap,
                final Map<String, Map<String, String>> resOpIdsMap,
                final Map<String, String> operationsIdRefs,
                final Map<String, String> operationsIdtoCRMId,
                final Map<String, String> metaAttrsIdRefs,
                final Map<String, String> metaAttrsIdToCRMId) {
        final String raClass = getAttribute(primitiveNode, "class");
        final String crmId = getAttribute(primitiveNode, "id");
        String provider = getAttribute(primitiveNode, "provider");
        if (provider == null) {
            provider = "heartbeat";
        }
        final String type = getAttribute(primitiveNode, "type");
        resourceTypeMap.put(crmId, getResourceAgent(type, provider, raClass));
        groupResList.add(crmId);
        parseAttributes(primitiveNode,
                        crmId,
                        parametersMap,
                        parametersNvpairsIdsMap,
                        resourceInstanceAttrIdMap,
                        operationsMap,
                        operationsIdMap,
                        resOpIdsMap,
                        operationsIdRefs,
                        operationsIdtoCRMId,
                        metaAttrsIdRefs,
                        metaAttrsIdToCRMId);
    }

    /**
     * This class holds parsed status of resource, m/s set, or clone set.
     */
    class ResStatus {
        /** On which nodes the resource runs, or is master. */
        private final List<String> runningOnNodes;
        /** On which nodes the resource is master if it is m/s resource. */
        private final List<String> masterOnNodes;
        /** On which nodes the resource is slave if it is m/s resource. */
        private final List<String> slaveOnNodes;
        /** Is managed by CRM. */
        private final boolean managed;

        /**
         * Creates a new ResStatus object.
         */
        public ResStatus(final List<String> runningOnNodes,
                         final List<String> masterOnNodes,
                         final List<String> slaveOnNodes,
                         final boolean managed) {
            this.runningOnNodes = runningOnNodes;
            this.masterOnNodes = masterOnNodes;
            this.slaveOnNodes = slaveOnNodes;
            this.managed = managed;
        }

        /**
         * Gets on which nodes the resource runs, or is master.
         */
        public final List<String> getRunningOnNodes() {
            return runningOnNodes;
        }

        /**
         * Gets on which nodes the resource is master if it is m/s resource.
         */
        public final List<String> getMasterOnNodes() {
            return masterOnNodes;
        }

        /**
         * Gets on which nodes the resource is slave if it is m/s resource.
         */
        public final List<String> getSlaveOnNodes() {
            return slaveOnNodes;
        }

        /**
         * Returns whether the resoruce is managed.
         */
        public final boolean isManaged() {
            return managed;
        }
    }

    /**
     * Returns a hash with resource information. (running_on)
     */
    public final Map<String, ResStatus> parseResStatus(final String resStatus) {
        final Map<String, ResStatus> resStatusMap =
                                           new HashMap<String, ResStatus>();
        final Document document = getXMLDocument(resStatus);
        if (document == null) {
            return null;
        }

        /* get root <resource_status> */
        final Node statusNode = getChildNode(document, "resource_status");
        if (statusNode == null) {
            return null;
        }
        /*      <resource...> */
        final NodeList resources = statusNode.getChildNodes();
        for (int i = 0; i < resources.getLength(); i++) {
            final Node resourceNode = resources.item(i);
            if (resourceNode.getNodeName().equals("resource")) {
                final String id = getAttribute(resourceNode, "id");
                final String isManaged = getAttribute(resourceNode, "managed");
                //if (runningOn != null && !"".equals(runningOn)) {
                //    final List<String> rList = new ArrayList<String>();
                //    rList.add(runningOn);
                //    resStatusMap.put(id, new ResStatus(rList, null));
                //}
                final NodeList statusList = resourceNode.getChildNodes();
                List<String> runningOnList = null;
                List<String> masterOnList = null;
                List<String> slaveOnList = null;
                boolean managed = false;
                if ("managed".equals(isManaged)) {
                    managed = true;
                }
                for (int j = 0; j < statusList.getLength(); j++) {
                    final Node setNode = statusList.item(j);
                    if (TARGET_ROLE_STARTED.equals(setNode.getNodeName())) {
                        final String node = getText(setNode);
                        if (runningOnList == null) {
                            runningOnList = new ArrayList<String>();
                        }
                        runningOnList.add(node);
                    } else if (TARGET_ROLE_MASTER.equals(
                                                      setNode.getNodeName())) {
                        final String node = getText(setNode);
                        if (masterOnList == null) {
                            masterOnList = new ArrayList<String>();
                        }
                        masterOnList.add(node);
                    } else if (TARGET_ROLE_SLAVE.equals(
                                                      setNode.getNodeName())) {
                        final String node = getText(setNode);
                        if (slaveOnList == null) {
                            slaveOnList = new ArrayList<String>();
                        }
                        slaveOnList.add(node);
                    }
                }
                resStatusMap.put(id, new ResStatus(runningOnList,
                                                   masterOnList,
                                                   slaveOnList,
                                                   managed));
            }
        }
        return resStatusMap;
    }

    /**
     * Parses the transient attributes.
     */
    private void parseTransientAttributes(final String uname,
                                          final Node transientAttrNode,
                                          final MultiKeyMap failedMap,
                                          final String hbV) {
        /* <instance_attributes> */
        final Node instanceAttrNode = getChildNode(transientAttrNode,
                                                   "instance_attributes");
        /* <nvpair...> */
        if (instanceAttrNode != null) {
            NodeList nvpairsRes;
            if (hbV != null && Tools.compareVersions(hbV, "2.99.0") < 0) {
                /* <attributtes> only til 2.1.4 */
                final Node attrNode = getChildNode(instanceAttrNode,
                                                   "attributes");
                nvpairsRes = attrNode.getChildNodes();
            } else {
                nvpairsRes = instanceAttrNode.getChildNodes();
            }
            for (int j = 0; j < nvpairsRes.getLength(); j++) {
                final Node optionNode = nvpairsRes.item(j);
                if (optionNode.getNodeName().equals("nvpair")) {
                    final String name = getAttribute(optionNode, "name");
                    final String value = getAttribute(optionNode, "value");
                    /* TODO: last-failure-" */
                    if (name.indexOf(FAIL_COUNT_PREFIX) == 0) {
                        final String resId =
                                    name.substring(FAIL_COUNT_PREFIX.length());
                        final Pattern p = Pattern.compile("(.*):(\\d+)$");
                        final Matcher m = p.matcher(resId);
                        if (m.matches()) {
                            failedMap.put(uname, m.group(1), value);
                        } else {
                            failedMap.put(uname, resId, value);
                        }
                    }
                }
            }
        }
    }

    /**
     * Parses node, to get info like if it is in stand by.
     */
    public final void parseNode(final String node,
                                final Node nodeNode,
                                final MultiKeyMap nodeParametersMap) {
        /* <instance_attributes> */
        final Node instanceAttrNode = getChildNode(nodeNode,
                                                   "instance_attributes");
        /* <nvpair...> */
        if (instanceAttrNode != null) {
            NodeList nvpairsRes;
            final String hbV = host.getHeartbeatVersion();
            final String pmV = host.getPacemakerVersion();
            if (pmV == null
                && hbV != null
                && Tools.compareVersions(hbV, "2.99.0") < 0) {
                /* <attributtes> only til 2.1.4 */
                final Node attrNode = getChildNode(instanceAttrNode,
                                                   "attributes");
                nvpairsRes = attrNode.getChildNodes();
            } else {
                nvpairsRes = instanceAttrNode.getChildNodes();
            }
            for (int j = 0; j < nvpairsRes.getLength(); j++) {
                final Node optionNode = nvpairsRes.item(j);
                if (optionNode.getNodeName().equals("nvpair")) {
                    final String name = getAttribute(optionNode, "name");
                    final String value = getAttribute(optionNode, "value");
                    nodeParametersMap.put(node, name, value);
                }
            }
        }
    }

    /**
     * Returns CibQuery object with information from the cib node.
     */
    public final CibQuery parseCibQuery(final String query) {
        final Document document = getXMLDocument(query);
        final CibQuery cibQueryData = new CibQuery();
        if (document == null) {
            Tools.appWarning("cib error");
            return cibQueryData;
        }

        /* get root <cib> */
        final Node cibNode = getChildNode(document, "cib");
        if (cibNode == null) {
            Tools.appWarning("there is no cib node");
            return cibQueryData;
        }
        /* Designated Co-ordinator */
        final String dcUuid = getAttribute(cibNode, "dc-uuid");
        //TODO: more attributes are here

        /* <configuration> */
        final Node confNode = getChildNode(cibNode, "configuration");
        if (confNode == null) {
            Tools.appWarning("there is no configuration node");
            return cibQueryData;
        }

        /* <crm_config> */
        final Node crmConfNode = getChildNode(confNode, "crm_config");
        if (crmConfNode == null) {
            Tools.appWarning("there is no crm_config node");
            return cibQueryData;
        }
        /*      <cluster_property_set> */
        final Node cpsNode = getChildNode(crmConfNode, "cluster_property_set");
        if (cpsNode == null) {
            Tools.appWarning("there is no cluster_property_set node");
            return cibQueryData;
        }
        NodeList nvpairs;
        final String hbV = host.getHeartbeatVersion();
        final String pmV = host.getPacemakerVersion();
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
            /* <attributtes> only til 2.1.4 */
            final Node attrNode = getChildNode(cpsNode,
                                               "attributes");
            nvpairs = attrNode.getChildNodes();
        } else {
            nvpairs = cpsNode.getChildNodes();
        }
        final Map<String, String> crmConfMap =
                                            new HashMap<String, String>();
        /*              <nvpair...> */
        for (int i = 0; i < nvpairs.getLength(); i++) {
            final Node optionNode = nvpairs.item(i);
            if (optionNode.getNodeName().equals("nvpair")) {
                final String name = getAttribute(optionNode, "name");
                final String value = getAttribute(optionNode, "value");
                crmConfMap.put(name, value);
            }
        }
        cibQueryData.setCrmConfig(crmConfMap);

        /* <nodes> */
        /* xml node with cluster node make stupid variable names, but let's
         * keep the convention. */
        String dc = null;
        final MultiKeyMap nodeParametersMap = new MultiKeyMap();
        final Node nodesNode = getChildNode(confNode, "nodes");
        if (nodesNode != null) {
            final NodeList nodes = nodesNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node nodeNode = nodes.item(i);
                if (nodeNode.getNodeName().equals("node")) {
                    /* TODO: doing nothing with the info, just getting the dc,
                     * for now.
                     */
                    final String uuid = getAttribute(nodeNode, "id");
                    final String uname = getAttribute(nodeNode, "uname");
                    if (dcUuid != null && dcUuid.equals(uuid)) {
                        dc = uname;
                    }
                    parseNode(uname, nodeNode, nodeParametersMap);
                }
            }
        }

        /* <resources> */
        final Node resourcesNode = getChildNode(confNode, "resources");
        if (resourcesNode == null) {
            Tools.appWarning("there is no resources node");
            return cibQueryData;
        }
        /*      <primitive> */
        //Map<String,String> resourceItemTypeMap = new HashMap<String,String>();
        final Map<String, Map<String, String>> parametersMap =
                                    new HashMap<String, Map<String, String>>();
        final Map<String, Map<String, String>> parametersNvpairsIdsMap =
                                    new HashMap<String, Map<String, String>>();
        final Map<String, ResourceAgent> resourceTypeMap =
                                      new HashMap<String, ResourceAgent>();
        final Map<String, String> resourceInstanceAttrIdMap =
                                      new HashMap<String, String>();
        final MultiKeyMap operationsMap = new MultiKeyMap();
        final Map<String, String> operationsIdMap =
                                                new HashMap<String, String>();
        final Map<String, Map<String, String>> resOpIdsMap =
                                    new HashMap<String, Map<String, String>>();
        /* must be linked, so that clone from group is before the group itself.
         */
        final Map<String, List<String>> groupsToResourcesMap =
                                     new LinkedHashMap<String, List<String>>();
        final Map<String, String> cloneToResourceMap =
                                                 new HashMap<String, String>();
        final List<String> masterList = new ArrayList<String>();
        final MultiKeyMap failedMap = new MultiKeyMap();
        groupsToResourcesMap.put("none", new ArrayList<String>());

        final NodeList primitivesGroups = resourcesNode.getChildNodes();
        final Map<String, String> operationsIdRefs =
                                                new HashMap<String, String>();
        final Map<String, String> operationsIdtoCRMId =
                                                new HashMap<String, String>();
        final Map<String, String> metaAttrsIdRefs =
                                                new HashMap<String, String>();
        final Map<String, String> metaAttrsIdToCRMId =
                                                new HashMap<String, String>();
        for (int i = 0; i < primitivesGroups.getLength(); i++) {
            final Node primitiveGroupNode = primitivesGroups.item(i);
            final String nodeName = primitiveGroupNode.getNodeName();
            if ("primitive".equals(nodeName)) {
                final List<String> resList =
                                        groupsToResourcesMap.get("none");
                parsePrimitive(primitiveGroupNode,
                               resList,
                               resourceTypeMap,
                               parametersMap,
                               parametersNvpairsIdsMap,
                               resourceInstanceAttrIdMap,
                               operationsMap,
                               operationsIdMap,
                               resOpIdsMap,
                               operationsIdRefs,
                               operationsIdtoCRMId,
                               metaAttrsIdRefs,
                               metaAttrsIdToCRMId);
            } else if ("group".equals(nodeName)) {
                parseGroup(primitiveGroupNode,
                           null,
                           groupsToResourcesMap,
                           parametersMap,
                           resourceTypeMap,
                           parametersNvpairsIdsMap,
                           resourceInstanceAttrIdMap,
                           operationsMap,
                           operationsIdMap,
                           resOpIdsMap,
                           operationsIdRefs,
                           operationsIdtoCRMId,
                           metaAttrsIdRefs,
                           metaAttrsIdToCRMId);
            } else if ("master".equals(nodeName)
                       || "master_slave".equals(nodeName)
                       || "clone".equals(nodeName)) {
                final NodeList primitives = primitiveGroupNode.getChildNodes();
                final String cloneId = getAttribute(primitiveGroupNode, "id");
                parametersMap.put(cloneId, new HashMap<String, String>());
                List<String> resList = groupsToResourcesMap.get(cloneId);
                if (resList == null) {
                    resList = new ArrayList<String>();
                    groupsToResourcesMap.put(cloneId, resList);
                }
                parseAttributes(primitiveGroupNode,
                                cloneId,
                                parametersMap,
                                parametersNvpairsIdsMap,
                                resourceInstanceAttrIdMap,
                                operationsMap,
                                operationsIdMap,
                                resOpIdsMap,
                                operationsIdRefs,
                                operationsIdtoCRMId,
                                metaAttrsIdRefs,
                                metaAttrsIdToCRMId);
                for (int j = 0; j < primitives.getLength(); j++) {
                    final Node primitiveNode = primitives.item(j);
                    if (primitiveNode.getNodeName().equals("primitive")) {
                        parsePrimitive(primitiveNode,
                                       resList,
                                       resourceTypeMap,
                                       parametersMap,
                                       parametersNvpairsIdsMap,
                                       resourceInstanceAttrIdMap,
                                       operationsMap,
                                       operationsIdMap,
                                       resOpIdsMap,
                                       operationsIdRefs,
                                       operationsIdtoCRMId,
                                       metaAttrsIdRefs,
                                       metaAttrsIdToCRMId);
                    } else if (primitiveNode.getNodeName().equals("group")) {
                        parseGroup(primitiveNode,
                                   resList,
                                   groupsToResourcesMap,
                                   parametersMap,
                                   resourceTypeMap,
                                   parametersNvpairsIdsMap,
                                   resourceInstanceAttrIdMap,
                                   operationsMap,
                                   operationsIdMap,
                                   resOpIdsMap,
                                   operationsIdRefs,
                                   operationsIdtoCRMId,
                                   metaAttrsIdRefs,
                                   metaAttrsIdToCRMId);
                    }
                }
                if (!resList.isEmpty()) {
                    cloneToResourceMap.put(cloneId, resList.get(0));
                    if ("master".equals(nodeName)
                        || "master_slave".equals(nodeName)) {
                        masterList.add(cloneId);
                    }
                }
            }
        }

        /* operationsRefs crm id -> crm id */
        final Map<String, String> operationsRefs =
                                                 new HashMap<String, String>();
        for (final String crmId : operationsIdRefs.keySet()) {
            final String idRef = operationsIdRefs.get(crmId);
            operationsRefs.put(crmId, operationsIdtoCRMId.get(idRef));
        }

        /* mettaAttrsRefs crm id -> crm id */
        final Map<String, String> metaAttrsRefs = new HashMap<String, String>();
        for (final String crmId : metaAttrsIdRefs.keySet()) {
            final String idRef = metaAttrsIdRefs.get(crmId);
            metaAttrsRefs.put(crmId, metaAttrsIdToCRMId.get(idRef));
        }

        /* <constraints> */
        final Map<String, List<String>> colocationMap =
                                          new HashMap<String, List<String>>();
        final MultiKeyMap colocationIdMap = new MultiKeyMap();
        final MultiKeyMap colocationScoreMap = new MultiKeyMap();
        final MultiKeyMap colocationRscRoleMap = new MultiKeyMap();
        final MultiKeyMap colocationWithRscRoleMap = new MultiKeyMap();

        final Map<String, List<String>> orderMap =
                                           new HashMap<String, List<String>>();
        final MultiKeyMap orderIdMap = new MultiKeyMap();
        final MultiKeyMap orderFirstActionMap = new MultiKeyMap();
        final MultiKeyMap orderThenActionMap = new MultiKeyMap();
        final MultiKeyMap orderDirectionMap = new MultiKeyMap();
        final MultiKeyMap orderScoreMap = new MultiKeyMap();
        final MultiKeyMap orderSymmetricalMap = new MultiKeyMap();

        final Map<String, Map<String, String>> locationMap =
                                    new HashMap<String, Map<String, String>>();
        final Map<String, List<String>> locationsIdMap =
                                           new HashMap<String, List<String>>();
        final MultiKeyMap resHostToLocIdMap = new MultiKeyMap();
        final Node constraintsNode = getChildNode(confNode, "constraints");
        if (constraintsNode != null) {
            final NodeList constraints = constraintsNode.getChildNodes();
            String rscString         = "rsc";
            String rscRoleString     = "rsc-role";
            String withRscString     = "with-rsc";
            String withRscRoleString = "with-rsc-role";
            String firstString       = "first";
            String thenString        = "then";
            String firstActionString = "first-action";
            String thenActionString  = "then-action";
            if (hbV != null && Tools.compareVersions(hbV, "2.99.0") < 0) {
                rscString         = "from";
                rscRoleString     = "from_role"; //TODO: just guessing
                withRscString     = "to";
                withRscRoleString = "to_role"; //TODO: just guessing
                firstString       = "from";
                thenString        = "to";
                firstActionString = "action";
                thenActionString  = "to_action";
            }
            for (int i = 0; i < constraints.getLength(); i++) {
                final Node constraintNode = constraints.item(i);
                if (constraintNode.getNodeName().equals("rsc_colocation")) {
                    final String colId = getAttribute(constraintNode, "id");
                    final String rsc = getAttribute(constraintNode, rscString);
                    final String rscRole = getAttribute(constraintNode,
                                                            rscRoleString);
                    final String withRsc = getAttribute(constraintNode,
                                                        withRscString);
                    final String withRscRole = getAttribute(constraintNode,
                                                            withRscRoleString);
                    final String score = getAttribute(constraintNode,
                                                      SCORE_STRING);
                    List<String> tos = colocationMap.get(rsc);
                    if (tos == null) {
                        tos = new ArrayList<String>();
                    }
                    tos.add(withRsc);
                    colocationMap.put(rsc, tos);
                    colocationScoreMap.put(rsc, withRsc, score);
                    colocationRscRoleMap.put(rsc, withRsc, rscRole);
                    colocationWithRscRoleMap.put(rsc, withRsc, withRscRole);
                    colocationIdMap.put(rsc, withRsc, colId);
                    // TODO: node-attribute
                } else if (constraintNode.getNodeName().equals("rsc_order")) {
                    final String ordId = getAttribute(constraintNode, "id");
                    final String rscFrom = getAttribute(constraintNode,
                                                        firstString);
                    final String rscTo = getAttribute(constraintNode,
                                                      thenString);
                    final String score = getAttribute(constraintNode,
                                                      SCORE_STRING);
                    final String symmetrical = getAttribute(constraintNode,
                                                               "symmetrical");
                    final String firstAction = getAttribute(constraintNode,
                                                            firstActionString);
                    final String thenAction = getAttribute(constraintNode,
                                                           thenActionString);
                    List<String> tos = orderMap.get(rscFrom);
                    if (tos == null) {
                        tos = new ArrayList<String>();
                    }
                    tos.add(rscTo);
                    orderMap.put(rscFrom, tos);
                    //TODO: before is not needed in pacemaker anymore
                    orderDirectionMap.put(rscFrom, rscTo, "before");
                    orderScoreMap.put(rscFrom, rscTo, score);
                    orderSymmetricalMap.put(rscFrom, rscTo, symmetrical);
                    orderIdMap.put(rscFrom, rscTo, ordId);
                    orderFirstActionMap.put(rscFrom, rscTo, firstAction);
                    orderThenActionMap.put(rscFrom, rscTo, thenAction);
                } else if ("rsc_location".equals(
                                              constraintNode.getNodeName())) {
                    final String locId = getAttribute(constraintNode, "id");
                    final String node  = getAttribute(constraintNode, "node");
                    final String rsc   = getAttribute(constraintNode, "rsc");
                    final String score = getAttribute(constraintNode,
                                                      SCORE_STRING);

                    List<String> locs = locationsIdMap.get(rsc);
                    if (locs == null) {
                        locs = new ArrayList<String>();
                        locationsIdMap.put(rsc, locs);
                    }
                    Map<String, String> hostScoreMap =
                                                locationMap.get(rsc);
                    if (hostScoreMap == null) {
                        hostScoreMap = new HashMap<String, String>();
                        locationMap.put(rsc, hostScoreMap);
                    }
                    if (node != null) {
                        resHostToLocIdMap.put(rsc, node, locId);
                    }
                    if (score != null) {
                        hostScoreMap.put(node, score);
                    }
                    locs.add(locId);
                    final Node ruleNode = getChildNode(constraintNode,
                                                       "rule");
                    if (ruleNode != null) {
                        final String score2 = getAttribute(ruleNode,
                                                           SCORE_STRING);
                        final String booleanOp = getAttribute(ruleNode,
                                                              "boolean-op");
                        // TODO: I know only "and", ignoring everything we
                        // don't know.
                        final Node expNode = getChildNode(ruleNode,
                                                          "expression");
                        if (expNode != null
                            && "expression".equals(expNode.getNodeName())) {
                            final String attr =
                                     getAttribute(expNode, "attribute");
                            final String op =
                                     getAttribute(expNode, "operation");
                            final String type =
                                     getAttribute(expNode, "type");
                            final String node2 =
                                     getAttribute(expNode, "value");
                            if ((booleanOp == null
                                 || "and".equals(booleanOp))
                                && "#uname".equals(attr)
                                && "string".equals(type)
                                && "eq".equals(op)) {
                                hostScoreMap.put(node2, score2);
                            }
                        }
                    }
                }
            }
        }

        /* <status> */
        final Map<String, String> nodeOnline = new HashMap<String, String>();
        final Node statusNode = getChildNode(cibNode, "status");
        if (statusNode != null) {
            /* <node_state ...> */
            final NodeList nodes = statusNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node nodeStateNode = nodes.item(i);
                if ("node_state".equals(nodeStateNode.getNodeName())) {
                    final String uname = getAttribute(nodeStateNode, "uname");
                    final String ha = getAttribute(nodeStateNode, "ha");
                    final String join = getAttribute(nodeStateNode, "join");
                    final String inCCM = getAttribute(nodeStateNode, "in_ccm");
                    //final String id = getAttribute(nodeStateNode, "id");
                    //final String crmd = getAttribute(nodeStateNode, "crmd");
                    //final String shutdown =
                    //                 getAttribute(nodeStateNode, "shutdown");
                    //final String inCcm =
                    //                   getAttribute(nodeStateNode, "in_ccm");
                    //final String expected =
                    //                getAttribute(nodeStateNode, "expected");
                    /* TODO: check and use other stuff too. */
                    if ("active".equals(ha)
                        && "member".equals(join)
                        && "true".equals(inCCM)) {
                        nodeOnline.put(uname, "yes");
                    } else {
                        nodeOnline.put(uname, "no");
                    }
                    final NodeList nodeStates = nodeStateNode.getChildNodes();
                    for (int j = 0; j < nodeStates.getLength(); j++) {
                        final Node nodeStateChild = nodeStates.item(j);
                        if ("transient_attributes".equals(
                                               nodeStateChild.getNodeName())) {
                            parseTransientAttributes(uname,
                                                     nodeStateChild,
                                                     failedMap,
                                                     hbV);
                        }
                    }
                }
            }
        }
        cibQueryData.setDC(dc);
        cibQueryData.setNodeParameters(nodeParametersMap);
        cibQueryData.setParameters(parametersMap);
        cibQueryData.setParametersNvpairsIds(parametersNvpairsIdsMap);
        cibQueryData.setResourceType(resourceTypeMap);
        cibQueryData.setResourceInstanceAttrId(resourceInstanceAttrIdMap);

        cibQueryData.setColocation(colocationMap);
        cibQueryData.setColocationScore(colocationScoreMap);
        cibQueryData.setColocationRscRole(colocationRscRoleMap);
        cibQueryData.setColocationWithRscRole(colocationWithRscRoleMap);
        cibQueryData.setColocationId(colocationIdMap);

        cibQueryData.setOrder(orderMap);
        cibQueryData.setOrderId(orderIdMap);
        cibQueryData.setOrderFirstAction(orderFirstActionMap);
        cibQueryData.setOrderThenAction(orderThenActionMap);
        cibQueryData.setOrderScore(orderScoreMap);
        cibQueryData.setOrderSymmetrical(orderSymmetricalMap);
        cibQueryData.setOrderDirection(orderDirectionMap);

        cibQueryData.setLocation(locationMap);
        cibQueryData.setLocationsId(locationsIdMap);
        cibQueryData.setResHostToLocId(resHostToLocIdMap);
        cibQueryData.setOperations(operationsMap);
        cibQueryData.setOperationsId(operationsIdMap);
        cibQueryData.setOperationsRefs(operationsRefs);
        cibQueryData.setMetaAttrsRefs(metaAttrsRefs);
        cibQueryData.setResOpIds(resOpIdsMap);
        cibQueryData.setNodeOnline(nodeOnline);
        cibQueryData.setGroupsToResources(groupsToResourcesMap);
        cibQueryData.setCloneToResource(cloneToResourceMap);
        cibQueryData.setMasterList(masterList);
        cibQueryData.setFailed(failedMap);
        return cibQueryData;
    }

    /**
     * Returns order parameters.
     */
    public final String[] getOrderParameters() {
        if (ordParams != null) {
            return ordParams.toArray(new String[ordParams.size()]);
        }
        return null;
    }

    /**
     * Checks if parameter is required or not.
     */
    public final boolean isOrderRequired(final String param) {
        return ordRequiredParams.contains(param);
    }

    /**
     * Returns short description of the order parameter.
     */
    public final String getOrderParamShortDesc(final String param) {
        String shortDesc = paramOrdShortDescMap.get(param);
        if (shortDesc == null) {
            shortDesc = param;
        }
        return shortDesc;
    }

    /**
     * Returns long description of the order parameter.
     */
    public final String getOrderParamLongDesc(final String param) {
        final String shortDesc = getOrderParamShortDesc(param);
        String longDesc = paramOrdLongDescMap.get(param);
        if (longDesc == null) {
            longDesc = "";
        }
        return Tools.html("<b>" + shortDesc + "</b>\n" + longDesc);
    }

    /**
     * Returns type of a order parameter. It can be string, integer, boolean...
     */
    public final String getOrderParamType(final String param) {
        return paramOrdTypeMap.get(param);
    }

    /**
     * Returns default value for the order parameter.
     */
    public final String getOrderParamDefault(final String param) {
        return paramOrdDefaultMap.get(param);
    }

    /**
     * Returns the preferred value for the order parameter.
     */
    public final String getOrderParamPreferred(final String param) {
        return paramOrdPreferredMap.get(param);
    }

    /**
     * Returns possible choices for a order parameter, that will be displayed
     * in the combo box.
     */
    public final String[] getOrderParamPossibleChoices(final String param,
                                                       final boolean ms) {
        if (ms) {
            return paramOrdPossibleChoicesMS.get(param);
        } else {
            return paramOrdPossibleChoices.get(param);
        }
    }

    /**
     * Returns whether the order parameter expects an integer value.
     */
    public final boolean isOrderInteger(final String param) {
        final String type = getOrderParamType(param);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    /**
     * Returns whether the order parameter expects a boolean value.
     */
    public final boolean isOrderBoolean(final String param) {
        final String type = getOrderParamType(param);
        return PARAM_TYPE_BOOLEAN.equals(type);
    }

    /**
     * Whether the order parameter is of the time type.
     */
    public final boolean isOrderTimeType(final String param) {
        final String type = getOrderParamType(param);
        return PARAM_TYPE_TIME.equals(type);
    }

    /**
     * Returns name of the section order parameter that will be
     * displayed.
     */
    public final String getOrderSection(final String param) {
        return Tools.getString("CRMXML.OrderSectionParams");
    }

    /**
     * Checks order parameter according to its type. Returns false if value
     * does not fit the type.
     */
    public final boolean checkOrderParam(final String param,
                                          final String value) {
        final String type = getOrderParamType(param);
        boolean correctValue = true;
        if (PARAM_TYPE_BOOLEAN.equals(type)) {
            if (!"yes".equals(value) && !"no".equals(value)
                && !Tools.getString("Heartbeat.Boolean.True").equals(value)
                && !Tools.getString("Heartbeat.Boolean.False").equals(value)
                && !Tools.getString(
                                "Heartbeat.2.1.3.Boolean.True").equals(value)
                && !Tools.getString(
                              "Heartbeat.2.1.3.Boolean.False").equals(value)) {

                correctValue = false;
            }
        } else if (PARAM_TYPE_INTEGER.equals(type)) {
            final Pattern p =
                        Pattern.compile("^-?(\\d*|" + INFINITY_STRING + ")$");
            final Matcher m = p.matcher(value);
            if (!m.matches()) {
                correctValue = false;
            }
        } else if (PARAM_TYPE_TIME.equals(type)) {
            final Pattern p =
                Pattern.compile("^-?\\d*(ms|msec|us|usec|s|sec|m|min|h|hr)?$");
            final Matcher m = p.matcher(value);
            if (!m.matches()) {
                correctValue = false;
            }
        } else if ((value == null || "".equals(value))
                   && isOrderRequired(param)) {
            correctValue = false;
        }
        return correctValue;
    }

    /**
     * Returns colocation parameters.
     */
    public final String[] getColocationParameters() {
        if (colParams != null) {
            return colParams.toArray(new String[colParams.size()]);
        }
        return null;
    }

    /**
     * Checks if parameter is required or not.
     */
    public final boolean isColocationRequired(final String param) {
        return colRequiredParams.contains(param);
    }

    /**
     * Returns short description of the colocation parameter.
     */
    public final String getColocationParamShortDesc(final String param) {
        String shortDesc = paramColShortDescMap.get(param);
        if (shortDesc == null) {
            shortDesc = param;
        }
        return shortDesc;
    }

    /**
     * Returns long description of the colocation parameter.
     */
    public final String getColocationParamLongDesc(final String param) {
        final String shortDesc = getColocationParamShortDesc(param);
        String longDesc = paramColLongDescMap.get(param);
        if (longDesc == null) {
            longDesc = "";
        }
        return Tools.html("<b>" + shortDesc + "</b>\n" + longDesc);
    }

    /**
     * Returns type of a colocation parameter. It can be string, integer...
     */
    public final String getColocationParamType(final String param) {
        return paramColTypeMap.get(param);
    }

    /**
     * Returns default value for the colocation parameter.
     */
    public final String getColocationParamDefault(final String param) {
        return paramColDefaultMap.get(param);
    }

    /**
     * Returns the preferred value for the colocation parameter.
     */
    public final String getColocationParamPreferred(final String param) {
        return paramColPreferredMap.get(param);
    }

    /**
     * Returns possible choices for a colocation parameter, that will be
     * displayed in the combo box.
     */
    public final String[] getColocationParamPossibleChoices(final String param,
                                                            final boolean ms) {
        if (ms) {
            return paramColPossibleChoicesMS.get(param);
        } else {
            return paramColPossibleChoices.get(param);
        }
    }

    /**
     * Returns whether the colocation parameter expects an integer value.
     */
    public final boolean isColocationInteger(final String param) {
        final String type = getColocationParamType(param);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    /**
     * Returns whether the colocation parameter expects a boolean value.
     */
    public final boolean isColocationBoolean(final String param) {
        final String type = getOrderParamType(param);
        return PARAM_TYPE_BOOLEAN.equals(type);
    }

    /**
     * Whether the colocation parameter is of the time type.
     */
    public final boolean isColocationTimeType(final String param) {
        final String type = getColocationParamType(param);
        return PARAM_TYPE_TIME.equals(type);
    }

    /**
     * Returns name of the section colocation parameter that will be
     * displayed.
     */
    public final String getColocationSection(final String param) {
        return Tools.getString("CRMXML.ColocationSectionParams");
    }

    /**
     * Checks colocation parameter according to its type. Returns false if value
     * does not fit the type.
     */
    public final boolean checkColocationParam(final String param,
                                          final String value) {
        final String type = getColocationParamType(param);
        boolean correctValue = true;
        if (PARAM_TYPE_BOOLEAN.equals(type)) {
            if (!"yes".equals(value) && !"no".equals(value)
                && !Tools.getString("Heartbeat.Boolean.True").equals(value)
                && !Tools.getString("Heartbeat.Boolean.False").equals(value)
                && !Tools.getString(
                                "Heartbeat.2.1.3.Boolean.True").equals(value)
                && !Tools.getString(
                              "Heartbeat.2.1.3.Boolean.False").equals(value)) {

                correctValue = false;
            }
        } else if (PARAM_TYPE_INTEGER.equals(type)) {
            final Pattern p =
                         Pattern.compile("^-?(\\d*|" + INFINITY_STRING + ")$");
            final Matcher m = p.matcher(value);
            if (!m.matches()) {
                correctValue = false;
            }
        } else if (PARAM_TYPE_TIME.equals(type)) {
            final Pattern p =
                Pattern.compile("^-?\\d*(ms|msec|us|usec|s|sec|m|min|h|hr)?$");
            final Matcher m = p.matcher(value);
            if (!m.matches()) {
                correctValue = false;
            }
        } else if ((value == null || "".equals(value))
                   && isColocationRequired(param)) {
            correctValue = false;
        }
        return correctValue;
    }

    /** Returns whether drbddisk ra is present. */
    public final boolean isDrbddiskPresent() {
        return drbddiskPresent;
    }
    /** Returns whether linbit::drbd ra is present. */
    public final boolean isLinbitDrbdPresent() {
        return linbitDrbdPresent;
    }
}
