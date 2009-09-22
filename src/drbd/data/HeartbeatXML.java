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
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Collections;
import org.apache.commons.collections.map.MultiKeyMap;

/**
 * This class parses ocf heartbeat xml, stores information like
 * short and long description, data types etc. for defined types
 * of services in the hashes and provides methods to get this
 * information.
 * TODO: must be renamed
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HeartbeatXML extends XML {
    /** Host. */
    private final Host host;
    /** List of global parameters. */
    private final List<String> globalParams = new ArrayList<String>();
    /** List of required global parameters. */
    private final List<String> globalRequiredParams = new ArrayList<String>();
    /** Map from class to the list of all heartbeat services. */
    private final Map<String, List<HeartbeatService>> classToServicesMap =
                                new HashMap<String, List<HeartbeatService>>();
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
    /** Predefined group as heartbeat service. */
    private final HeartbeatService hbGroup =
                      new HeartbeatService(Tools.getConfigData().PM_GROUP_NAME,
                                           "",
                                           "group");
    /** Predefined clone as heartbeat service. */
    private final HeartbeatService hbClone;
    /** Predefined drvbddisk as heartbeat service. */
    private final HeartbeatService hbDrbddisk =
                    new HeartbeatService("drbddisk", "heartbeat", "heartbeat");
    /** Mapfrom heartbeat service defined by name and class to the hearbeat
     * service object.
     */
    private final MultiKeyMap serviceToHbServiceMap = new MultiKeyMap();
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
    private static final String[] ATTRIBUTE_ROLES = {"Stopped",
                                                     "Started",
                                                     "Master",
                                                     "Slave"};
    /** Attribute actions. */
    private static final String[] ATTRIBUTE_ACTIONS = {"start",
                                                       "promote",
                                                       "demote",
                                                       "stop"};
    /**
     * Prepares a new <code>HeartbeatXML</code> object.
     */
    public HeartbeatXML(final Host host) {
        super();
        this.host = host;
        String command = null;
        final String hbV = host.getHeartbeatVersion();
        String[] booleanValues = getGlobalCheckBoxChoices();
        String[] integerValues = getIntegerValues();
        final String hb_boolean_true = booleanValues[0];
        final String hb_boolean_false = booleanValues[1];
        hbClone = new HeartbeatService(
                                Tools.getConfigData().PM_CLONE_SET_NAME,
                                "",
                                "clone");
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
        hbClone.setParamDefault("notify", hb_boolean_false);
        hbClone.setParamPreferred("notify", hb_boolean_true);
        hbClone.setParamPossibleChoices("notify", booleanValues);
        /* globally-unique */
        hbClone.addParameter("globally-unique");
        hbClone.setParamIsMetaAttr("globally-unique", true);
        hbClone.setParamShortDesc("globally-unique", "M/S Globally-Unique");
        hbClone.setParamDefault("globally-unique", hb_boolean_false);
        hbClone.setParamPossibleChoices("globally-unique", booleanValues);
        /* ordered */
        hbClone.addParameter("ordered");
        hbClone.setParamIsMetaAttr("ordered", true);
        hbClone.setParamShortDesc("ordered", "M/S Ordered");
        hbClone.setParamDefault("ordered", hb_boolean_false);
        hbClone.setParamPossibleChoices("ordered", booleanValues);
        /* interleave */
        hbClone.addParameter("interleave");
        hbClone.setParamIsMetaAttr("interleave", true);
        hbClone.setParamShortDesc("interleave", "M/S Interleave");
        hbClone.setParamDefault("interleave", hb_boolean_false);
        hbClone.setParamPossibleChoices("interleave", booleanValues);

        if (Tools.compareVersions(hbV, "2.1.3") <= 0) {
            command = host.getDistCommand("Heartbeat.2.1.3.getOCFParameters",
                                          (ConvertCmdCallback) null);
        }

        if (command == null && Tools.compareVersions(hbV, "2.1.4") <= 0) {
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
                            Tools.getString("HeartbeatXML.GetOCFParameters"));
        if (output == null) {
            //Tools.appError("heartbeat ocf output is null");
            return;
        }
        final String[] lines = output.split("\\r?\\n");
        final Pattern pp = Pattern.compile("^provider:\\s*(.*?)\\s*$");
        final Pattern bp = Pattern.compile("^<resource-agent name=\"(.*?)\".*");
        final Pattern ep = Pattern.compile("^</resource-agent>$");
        final StringBuffer xml = new StringBuffer("");
        String provider = null;
        String serviceName = null;
        boolean drbddiskPresent = false;
        for (int i = 0; i < lines.length; i++) {
            //<resource-agent name="AudibleAlarm">
            // ...
            //</resource-agent>
            final Matcher pm = pp.matcher(lines[i]);
            if (pm.matches()) {
                provider = pm.group(1);
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
                    }
                    parseMetaData(serviceName, provider, xml.toString());
                    serviceName = null;
                    xml.delete(0, xml.length() - 1);
                }
            }
        }
        if (!drbddiskPresent) {
            Tools.appError("drbddisk heartbeat script is not present");
        }

        /* Hardcoding global params */
        /* symmetric cluster */
        globalParams.add("symmetric-cluster");
        paramGlobalShortDescMap.put("symmetric-cluster", "Symmetric Cluster");
        paramGlobalLongDescMap.put("symmetric-cluster", "Symmetric Cluster");
        paramGlobalTypeMap.put("symmetric-cluster", PARAM_TYPE_BOOLEAN);
        paramGlobalDefaultMap.put("symmetric-cluster", hb_boolean_false);
        paramGlobalPossibleChoices.put("symmetric-cluster", booleanValues);
        globalRequiredParams.add("symmetric-cluster");

        /* stonith enabled */
        globalParams.add("stonith-enabled");
        paramGlobalShortDescMap.put("stonith-enabled", "Stonith Enabled");
        paramGlobalLongDescMap.put("stonith-enabled", "Stonith Enabled");
        paramGlobalTypeMap.put("stonith-enabled", PARAM_TYPE_BOOLEAN);
        paramGlobalDefaultMap.put("stonith-enabled", hb_boolean_true);
        paramGlobalPreferredMap.put("stonith-enabled", hb_boolean_false);
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
        paramGlobalPreferredMap.put("default-resource-stickiness", "100");
        globalRequiredParams.add("default-resource-stickiness");

        /* no quorum policy */
        globalParams.add("no-quorum-policy");
        paramGlobalShortDescMap.put("no-quorum-policy", "No Quorum Policy");
        paramGlobalLongDescMap.put("no-quorum-policy", "No Quorum Policy");
        // TODO: ignore, stop, freeze
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


        if (Tools.compareVersions(hbV, "2.1.3") >= 0) {
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
            paramGlobalDefaultMap.put("is-managed-default", hb_boolean_false);
            paramGlobalPossibleChoices.put("is-managed-default", booleanValues);

            paramGlobalTypeMap.put("stop-orphan-resources", PARAM_TYPE_BOOLEAN);
            paramGlobalDefaultMap.put("stop-orphan-resources",
                                                            hb_boolean_false);
            paramGlobalPossibleChoices.put("stop-orphan-resources",
                                           booleanValues);

            paramGlobalTypeMap.put("stop-orphan-actions", PARAM_TYPE_BOOLEAN);
            paramGlobalDefaultMap.put("stop-orphan-actions", hb_boolean_false);
            paramGlobalPossibleChoices.put("stop-orphan-actions",
                                           booleanValues);

            paramGlobalTypeMap.put("remove-after-stop", PARAM_TYPE_BOOLEAN);
            paramGlobalDefaultMap.put("remove-after-stop", hb_boolean_false);
            paramGlobalPossibleChoices.put("remove-after-stop", booleanValues);

            paramGlobalTypeMap.put("startup-fencing", PARAM_TYPE_BOOLEAN);
            paramGlobalDefaultMap.put("startup-fencing", hb_boolean_false);
            paramGlobalPossibleChoices.put("startup-fencing", booleanValues);

            paramGlobalTypeMap.put("start-failure-is-fatal",
                                   PARAM_TYPE_BOOLEAN);
            paramGlobalDefaultMap.put("start-failure-is-fatal",
                                      hb_boolean_false);
            paramGlobalPossibleChoices.put("start-failure-is-fatal",
                                           booleanValues);
        }

        /* Hardcoding colocation params */
        colParams.add("rsc-role");
        paramColShortDescMap.put("rsc-role", "rsc1 col role");
        paramColLongDescMap.put("rsc-role", "@RSC@ colocation role");
        paramColTypeMap.put("rsc-role", PARAM_TYPE_STRING);
        paramColDefaultMap.put("rsc-role", "Started");
        paramColPossibleChoices.put("rsc-role", ATTRIBUTE_ROLES);

        colParams.add("with-rsc-role");
        paramColShortDescMap.put("with-rsc-role", "rsc2 col role");
        paramColLongDescMap.put("with-rsc-role", "@WITH-RSC@ colocation role");
        paramColTypeMap.put("with-rsc-role", PARAM_TYPE_STRING);
        paramColDefaultMap.put("with-rsc-role", "Started");
        paramColPossibleChoices.put("with-rsc-role", ATTRIBUTE_ROLES);

        colParams.add("score");
        paramColShortDescMap.put("score", "Score");
        paramColLongDescMap.put("score", "Score");
        paramColTypeMap.put("score", PARAM_TYPE_INTEGER);
        paramColDefaultMap.put("score", "INFINITY");
        paramColPossibleChoices.put("score", integerValues);
        /* Hardcoding order params */
        ordParams.add("first-action");
        paramOrdShortDescMap.put("first-action", "rsc1 order action");
        paramOrdLongDescMap.put("first-action", "@FIRST-RSC@ order action");
        paramOrdTypeMap.put("first-action", PARAM_TYPE_STRING);
        paramOrdDefaultMap.put("first-action", "start");
        paramOrdPossibleChoices.put("first-action", ATTRIBUTE_ACTIONS);

        ordParams.add("then-action");
        paramOrdShortDescMap.put("then-action", "rsc2 order action");
        paramOrdLongDescMap.put("then-action", "@THEN-RSC@ order action");
        paramOrdTypeMap.put("then-action", PARAM_TYPE_STRING);
        paramOrdDefaultMap.put("then-action", "start");
        paramOrdPossibleChoices.put("then-action", ATTRIBUTE_ACTIONS);

        ordParams.add("symmetrical");
        paramOrdShortDescMap.put("symmetrical", "Symmetrical");
        paramOrdLongDescMap.put("symmetrical", "Symmetrical");
        paramOrdTypeMap.put("symmetrical", PARAM_TYPE_BOOLEAN);
        paramOrdDefaultMap.put("symmetrical", hb_boolean_false);
        paramOrdPossibleChoices.put("symmetrical", booleanValues);

        ordParams.add("score");
        paramOrdShortDescMap.put("score", "Score");
        paramOrdLongDescMap.put("score", "Score");
        paramOrdTypeMap.put("score", PARAM_TYPE_INTEGER);
        paramOrdDefaultMap.put("score", "INFINITY");
        paramOrdPossibleChoices.put("score", integerValues);
    }

    /**
     * Returns choices for check boxes in the global config. (True, False).
     */
    public final String[] getGlobalCheckBoxChoices() {
        final String hbV = host.getHeartbeatVersion();
        if (Tools.compareVersions(hbV, "2.1.3") >= 0) {
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
        return new String[]{"0", "INFINITY", "-INFINITY"};
    }


    /**
     * Returns choices for check box. (True, False).
     * The problem is, that heartbeat kept changing the lower and upper case in
     * the true and false values.
     */
    public final String[] getCheckBoxChoices(final HeartbeatService hbService,
                                             final String param) {
        final String paramDefault = getParamDefault(hbService, param);
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
        if (Tools.compareVersions(hbV, "2.1.3") >= 0) {
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
    public final List<HeartbeatService> getServices(final String cl) {
        final List<HeartbeatService> services = classToServicesMap.get(cl);
        if (services == null) {
            return new ArrayList<HeartbeatService>();
        }
        Collections.sort(services,
                         new Comparator<HeartbeatService>() {
                              public int compare(final HeartbeatService s1,
                                                 final HeartbeatService s2) {
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
    public final String[] getParameters(final HeartbeatService hbService) {
        /* return cached values */
        return hbService.getParameters();
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
    public final String getVersion(final HeartbeatService hbService) {
        return hbService.getVersion();
    }

    /**
     * Return short description of the service.
     */
    public final String getShortDesc(final HeartbeatService hbService) {
        return hbService.getShortDesc();
    }

    /**
     * Return long description of the service.
     */
    public final String getLongDesc(final HeartbeatService hbService) {
        return hbService.getLongDesc();
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
    public final String getParamShortDesc(final HeartbeatService hbService,
                                          final String param) {
        return hbService.getParamShortDesc(param);
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
    public final String getParamLongDesc(final HeartbeatService hbService,
                                         final String param) {
        final String shortDesc = getParamShortDesc(hbService, param);
        String longDesc = hbService.getParamLongDesc(param);
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
    public final String getParamType(final HeartbeatService hbService,
                               final String param) {
        return hbService.getParamType(param);
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
    public final String getParamPreferred(final HeartbeatService hbService,
                                          final String param) {
        return hbService.getParamPreferred(param);
    }

    /**
     * Returns default value for this parameter.
     */
    public final String getParamDefault(final HeartbeatService hbService,
                                        final String param) {
        return hbService.getParamDefault(param);
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
    public final String[] getParamPossibleChoices(
                                            final HeartbeatService hbService,
                                            final String param) {
        return hbService.getParamPossibleChoices(param); // TODO: dead code
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
    public final boolean isRequired(final HeartbeatService hbService,
                                    final String param) {
        return hbService.isRequired(param);
    }

    /**
     * Returns whether the parameter is meta attribute or not.
     */
    public final boolean isMetaAttr(final HeartbeatService hbService,
                                    final String param) {
        return hbService.isParamMetaAttr(param);
    }

    /**
     * Returns whether the parameter expects an integer value.
     */
    public final boolean isInteger(final HeartbeatService hbService,
                                   final String param) {
        final String type = getParamType(hbService, param);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    /**
     * Returns whether the parameter expects a boolean value.
     */
    public final boolean isBoolean(final HeartbeatService hbService,
                                   final String param) {
        final String type = getParamType(hbService, param);
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
    public final boolean isTimeType(final HeartbeatService hbService,
                                    final String param) {
        final String type = getParamType(hbService, param);
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
    public final String getSection(final HeartbeatService hbService,
                                   final String param) {
        if (isMetaAttr(hbService, param)) {
            return Tools.getString("HeartbeatXML.MetaAttrOptions");
        } else if (isRequired(hbService, param)) {
            return Tools.getString("HeartbeatXML.RequiredOptions");
        } else {
            return Tools.getString("HeartbeatXML.OptionalOptions");
        }
    }

    /**
     * Returns name of the section global parameter that will be
     * displayed.
     */
    public final String getGlobalSection(final String param) {
        if (isGlobalRequired(param)) {
            return Tools.getString("HeartbeatXML.RequiredOptions");
        } else {
            return Tools.getString("HeartbeatXML.OptionalOptions");
        }
    }

    /**
     * Checks parameter according to its type. Returns false if value does
     * not fit the type.
     */
    public final boolean checkParam(final HeartbeatService hbService,
                                    final String param,
                                    final String value) {
        final String type = getParamType(hbService, param);
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
            final Pattern p = Pattern.compile("^-?(\\d*|INFINITY)$");
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
                   && isRequired(hbService, param)) {
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
            final Pattern p = Pattern.compile("^-?(\\d*|INFINITY)$");
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
     * Parses the parameters.
     */
    private void parseParameters(final HeartbeatService hbService,
                                 final Node parametersNode) {
        /* target-role */
        final String hbV = host.getHeartbeatVersion();
        String targetRoleParam = "target-role";
        String isManagedParam = "is-managed";
        if (hbV != null && Tools.compareVersions(hbV, "2.99.0") < 0) {
            targetRoleParam = "target_role";
            isManagedParam = "is_managed";
        }
        hbService.addParameter(targetRoleParam);
        hbService.setParamPossibleChoices(targetRoleParam,
                                          new String[]{"started", "stopped"});
        hbService.setParamIsMetaAttr(targetRoleParam, true);
        hbService.setParamRequired(targetRoleParam, true);
        hbService.setParamShortDesc(
                         targetRoleParam,
                         Tools.getString("HeartbeatXML.TargetRole.ShortDesc"));
        hbService.setParamLongDesc(
                         targetRoleParam,
                         Tools.getString("HeartbeatXML.TargetRole.LongDesc"));
        hbService.setParamDefault(targetRoleParam, "stopped");

        hbService.addParameter(isManagedParam);
        hbService.setParamPossibleChoices(isManagedParam,
                                          new String[]{"true", "false"});
        hbService.setParamIsMetaAttr(isManagedParam, true);
        hbService.setParamRequired(isManagedParam, true);
        hbService.setParamShortDesc(
                         isManagedParam,
                         Tools.getString("HeartbeatXML.IsManaged.ShortDesc"));
        hbService.setParamLongDesc(
                         isManagedParam,
                         Tools.getString("HeartbeatXML.IsManaged.LongDesc"));
        hbService.setParamDefault(isManagedParam, "true");

        final NodeList parameters = parametersNode.getChildNodes();
        for (int i = 0; i < parameters.getLength(); i++) {
            final Node parameterNode = parameters.item(i);
            if (parameterNode.getNodeName().equals("parameter")) {
                final String param = getAttribute(parameterNode, "name");
                final String required = getAttribute(parameterNode, "required");
                hbService.addParameter(param);

                if (required != null && required.equals("1")) {
                    hbService.setParamRequired(param, true);
                }

                /* <longdesc lang="en"> */
                final Node longdescParamNode = getChildNode(parameterNode,
                                                            "longdesc");
                if (longdescParamNode != null) {
                    final String longDesc = getText(longdescParamNode);
                    hbService.setParamLongDesc(param, longDesc);
                }

                /* <shortdesc lang="en"> */
                final Node shortdescParamNode = getChildNode(parameterNode,
                                                             "shortdesc");
                if (shortdescParamNode != null) {
                    final String shortDesc = getText(shortdescParamNode);
                    hbService.setParamShortDesc(param, shortDesc);
                }

                /* <content> */
                final Node contentParamNode = getChildNode(parameterNode,
                                                           "content");
                if (contentParamNode != null) {
                    final String type = getAttribute(contentParamNode, "type");
                    final String defaultValue = getAttribute(contentParamNode,
                                                                    "default");

                    hbService.setParamType(param, type);
                    hbService.setParamDefault(param, defaultValue);
                }
            }
        }
    }

    /**
     * Parses the actions node.
     */
    private void parseActions(final HeartbeatService hbService,
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
                hbService.addOperationDefault(name, "depth", depth);
                hbService.addOperationDefault(name, "timeout", timeout);
                hbService.addOperationDefault(name, "interval", interval);
                hbService.addOperationDefault(name, "start-delay", startDelay);
                hbService.addOperationDefault(name, "role", role);
            }
        }
    }

    /**
     * Parses meta-data xml for parameters for service and fills up the hashes
     * "CRM Daemon"s are global config options.
     */
    public final void parseMetaData(final String serviceName,
                                    final String provider,
                                    final String xml) {
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
        String heartbeatClass = getAttribute(raNode, "class");
        if (heartbeatClass == null) {
            heartbeatClass = "ocf";
        }
        List<HeartbeatService> hbServiceList =
                                        classToServicesMap.get(heartbeatClass);
        if (hbServiceList == null) {
            hbServiceList = new ArrayList<HeartbeatService>();
            classToServicesMap.put(heartbeatClass, hbServiceList);
        }
        HeartbeatService hbService;
        if (serviceName.equals("drbddisk")
            && heartbeatClass.equals("heartbeat")) {
            hbService = hbDrbddisk;
        } else {
            hbService = new HeartbeatService(serviceName,
                                             provider,
                                             heartbeatClass);
        }
        serviceToHbServiceMap.put(serviceName,
                                  provider,
                                  heartbeatClass,
                                  hbService);
        hbServiceList.add(hbService);

        /* <version> */
        final Node versionNode = getChildNode(raNode, "version");
        if (versionNode != null) {
            hbService.setVersion(getText(versionNode));
        }

        /* <longdesc lang="en"> */
        final Node longdescNode = getChildNode(raNode, "longdesc");
        if (longdescNode != null) {
            hbService.setLongDesc(getText(longdescNode));
        }

        /* <shortdesc lang="en"> */
        final Node shortdescNode = getChildNode(raNode, "shortdesc");
        if (shortdescNode != null) {
            hbService.setShortDesc(getText(shortdescNode));
        }

        /* <parameters> */
        final Node parametersNode = getChildNode(raNode, "parameters");
        if (parametersNode != null) {
            parseParameters(hbService, parametersNode);
        }
        /* <actions> */
        final Node actionsNode = getChildNode(raNode, "actions");
        if (actionsNode != null) {
            parseActions(hbService, actionsNode);
        }
    }

    /**
     * Parses crm meta data, only to get long descriptions and default values
     * for advanced options.
     * Strange stuff
     *
     * which can be pengine or crmd
     *
     * TODO: check if it works with hb != 2.1.[34]
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
                        paramGlobalDefaultMap.put(param, defaultValue);
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
    public final HeartbeatService getHbService(final String serviceName,
                                               final String provider,
                                               final String hbClass) {
        return (HeartbeatService) serviceToHbServiceMap.get(serviceName,
                                                            provider,
                                                            hbClass);
    }

    /**
     * Returns the heartbeat service object of the drbddisk service.
     */
    public final HeartbeatService getHbDrbddisk() {
        return hbDrbddisk;
    }

    /**
     * Returns the heartbeat service object of the hearbeat group.
     */
    public final HeartbeatService getHbGroup() {
        return hbGroup;
    }

    /**
     * Returns the heartbeat service object of the hearbeat clone set.
     */
    public final HeartbeatService getHbClone() {
        return hbClone;
    }

    /**

    /**
     * Parses attributes, operations etc. from primitives and clones.
     */
    private void parseAttributes(
              final Node resourceNode,
              final String hbId,
              final Map<String, Map<String, String>> parametersMap,
              final Map<String, Map<String, String>> parametersNvpairsIdsMap,
              final Map<String, String> resourceInstanceAttrIdMap,
              final MultiKeyMap operationsMap,
              final Map<String, String> operationsIdMap,
              final Map<String, Map<String, String>> resOpIdsMap) {
        final Map<String, String> params =
                                        new HashMap<String, String>();
        parametersMap.put(hbId, params);
        final Map<String, String> nvpairIds =
                                        new HashMap<String, String>();
        parametersNvpairsIdsMap.put(hbId, nvpairIds);
        final String hbV = host.getHeartbeatVersion();
        /* <instance_attributes> */
        final Node instanceAttrNode =
                                   getChildNode(resourceNode,
                                                "instance_attributes");
        /* <nvpair...> */
        if (instanceAttrNode != null) {
            final String iAId = getAttribute(instanceAttrNode, "id");
            resourceInstanceAttrIdMap.put(hbId, iAId);
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
                    final String nvpairId = getAttribute(optionNode, "id");
                    final String name = getAttribute(optionNode, "name");
                    final String value = getAttribute(optionNode, "value");
                    params.put(name, value);
                    nvpairIds.put(name, nvpairId);
                }
            }
        }

        /* <operations> */
        final Node operationsNode = getChildNode(resourceNode,
                                                 "operations");
        if (operationsNode != null) {
            final String operationsId = getAttribute(operationsNode,
                                                     "id");
            operationsIdMap.put(hbId, operationsId);
            Map<String, String> opIds = new HashMap<String, String>();
            resOpIdsMap.put(hbId, opIds);
            /* <op> */
            final NodeList ops = operationsNode.getChildNodes();
            for (int k = 0; k < ops.getLength(); k++) {
                final Node opNode = ops.item(k);
                if (opNode.getNodeName().equals("op")) {
                    final String opId = getAttribute(opNode,
                                                            "id");
                    final String name = getAttribute(opNode, "name");
                    final String timeout = getAttribute(opNode,
                                                        "timeout");
                    final String interval = getAttribute(opNode,
                                                         "interval");
                    final String startDelay = getAttribute(opNode,
                                                           "start-delay");
                    operationsMap.put(hbId,
                                      name,
                                      "interval",
                                      interval);
                    operationsMap.put(hbId,
                                      name,
                                      "timeout",
                                      timeout);
                    operationsMap.put(hbId,
                                      name,
                                      "start-delay",
                                      startDelay);
                    opIds.put(name, opId);
                }
            }
        }

        /* <meta_attributtes> */
        final Node metaAttrsNode = getChildNode(resourceNode,
                                                "meta_attributes");
        if (metaAttrsNode != null) {
            final String metaAttrsId = getAttribute(metaAttrsNode, "id");
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

    /**
     * Parses the "primitive" node.
     */
    private void parsePrimitive(
                final Node primitiveNode,
                final List<String> groupResList,
                final Map<String, HeartbeatService> resourceTypeMap,
                final Map<String, Map<String, String>> parametersMap,
                final Map<String, Map<String, String>> parametersNvpairsIdsMap,
                final Map<String, String> resourceInstanceAttrIdMap,
                final MultiKeyMap operationsMap,
                final Map<String, String> operationsIdMap,
                final Map<String, Map<String, String>> resOpIdsMap) {
        final String hbClass = getAttribute(primitiveNode, "class");
        final String hbId = getAttribute(primitiveNode, "id");
        final String provider = getAttribute(primitiveNode, "provider");
        final String type = getAttribute(primitiveNode, "type");
        resourceTypeMap.put(hbId, getHbService(type, provider, hbClass));
        groupResList.add(hbId);
        parseAttributes(primitiveNode,
                        hbId,
                        parametersMap,
                        parametersNvpairsIdsMap,
                        resourceInstanceAttrIdMap,
                        operationsMap,
                        operationsIdMap,
                        resOpIdsMap);
    }

    /**
     * Returns a hash with resource information. (running_on)
     */
    public final Map<String, String> parseResStatus(final String resStatus) {
        final Map<String, String> runningOnNode = new HashMap<String, String>();
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
                final String runningOn =
                                      getAttribute(resourceNode, "running_on");
                runningOnNode.put(id, runningOn);
            }
        }
        return runningOnNode;
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
            final String iAId = getAttribute(instanceAttrNode, "id");
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
                        failedMap.put(uname, resId, value);
                    }
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
            return cibQueryData;
        }

        /* get root <cib> */
        final Node cibNode = getChildNode(document, "cib");
        if (cibNode == null) {
            return cibQueryData;
        }
        /* Designated Co-ordinator */
        final String dcUuid = getAttribute(cibNode, "dc-uuid");
        //TODO: more attributes are here

        /* <configuration> */
        final Node confNode = getChildNode(cibNode, "configuration");
        if (confNode == null) {
            return cibQueryData;
        }

        /* <crm_config> */
        final Node crmConfNode = getChildNode(confNode, "crm_config");
        if (crmConfNode == null) {
            return cibQueryData;
        }
        /*      <cluster_property_set> */
        final Node cpsNode = getChildNode(crmConfNode, "cluster_property_set");
        if (cpsNode == null) {
            return cibQueryData;
        }
        NodeList nvpairs;
        final String hbV = host.getHeartbeatVersion();
        if (hbV != null && Tools.compareVersions(hbV, "2.99.0") < 0) {
            /* <attributtes> only til 2.1.4 */
            final Node attrNode = getChildNode(cpsNode,
                                               "attributes");
            nvpairs = attrNode.getChildNodes();
        } else {
            nvpairs = cpsNode.getChildNodes();
        }
        Map<String, String> crmConfMap = new HashMap<String, String>();
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
                }
            }
        }

        /* <resources> */
        final Node resourcesNode = getChildNode(confNode, "resources");
        if (resourcesNode == null) {
            return cibQueryData;
        }
        /*      <primitive> */
        //Map<String,String> resourceItemTypeMap = new HashMap<String,String>();
        final Map<String, Map<String, String>> parametersMap =
                                    new HashMap<String, Map<String, String>>();
        final Map<String, Map<String, String>> parametersNvpairsIdsMap =
                                    new HashMap<String, Map<String, String>>();
        final Map<String, HeartbeatService> resourceTypeMap =
                                      new HashMap<String, HeartbeatService>();
        final Map<String, String> resourceInstanceAttrIdMap =
                                      new HashMap<String, String>();
        final MultiKeyMap operationsMap = new MultiKeyMap();
        final Map<String, String> operationsIdMap =
                                                new HashMap<String, String>();
        final Map<String, Map<String, String>> resOpIdsMap =
                                    new HashMap<String, Map<String, String>>();
        final Map<String, List<String>> groupsToResourcesMap =
                                           new HashMap<String, List<String>>();
        final Map<String, String> cloneToResourceMap =
                                                 new HashMap<String, String>();
        final List<String> masterList = new ArrayList<String>();
        final MultiKeyMap failedMap = new MultiKeyMap();
        groupsToResourcesMap.put("none", new ArrayList<String>());

        final NodeList primitivesGroups = resourcesNode.getChildNodes();
        for (int i = 0; i < primitivesGroups.getLength(); i++) {
            final Node primitiveGroupNode = primitivesGroups.item(i);
            if (primitiveGroupNode.getNodeName().equals("primitive")) {
                List<String> resList = groupsToResourcesMap.get("none");
                parsePrimitive(primitiveGroupNode,
                               resList,
                               resourceTypeMap,
                               parametersMap,
                               parametersNvpairsIdsMap,
                               resourceInstanceAttrIdMap,
                               operationsMap,
                               operationsIdMap,
                               resOpIdsMap);
            } else if (primitiveGroupNode.getNodeName().equals("group")) {
                final NodeList primitives = primitiveGroupNode.getChildNodes();
                final String groupId = getAttribute(primitiveGroupNode, "id");
                parametersMap.put(groupId, new HashMap<String, String>());
                List<String> resList = groupsToResourcesMap.get(groupId);
                if (resList == null) {
                    resList = new ArrayList<String>();
                    groupsToResourcesMap.put(groupId, resList);
                }

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
                                       resOpIdsMap);
                    }
                }
            } else if (primitiveGroupNode.getNodeName().equals("master")
                       || primitiveGroupNode.getNodeName().equals("clone")) {
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
                                resOpIdsMap);
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
                                       resOpIdsMap);
                    }
                }
                if (resList.size() > 0) {
                    cloneToResourceMap.put(cloneId, resList.get(0));
                    if (primitiveGroupNode.getNodeName().equals("master")) {
                        masterList.add(cloneId);
                    }
                }
            }
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
                rscRoleString     = "from-role"; //TODO: just guessing
                withRscString     = "to";
                withRscRoleString = "to-role"; //TODO: just guessing
                firstString       = "from";
                thenString        = "to";
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
                    final String score = getAttribute(constraintNode, "score");
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
                    final String score = getAttribute(constraintNode, "score");
                    // TODO: symmetrical order stuff
                    final String symmetrical = getAttribute(constraintNode,
                                                               "symmetrical");
                    final String firstAction = getAttribute(constraintNode,
                                                               "first-action");
                    final String thenAction = getAttribute(constraintNode,
                                                               "then-action");
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
                } else if (constraintNode.getNodeName().equals(
                                                            "rsc_location")) {
                    final String locId = getAttribute(constraintNode, "id");
                    final String node  = getAttribute(constraintNode, "node");
                    final String rsc   = getAttribute(constraintNode, "rsc");
                    final String score = getAttribute(constraintNode, "score");

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
                    hostScoreMap.put(node, score);
                    resHostToLocIdMap.put(rsc, node, locId);
                    locs.add(locId);
                }
            }
        }

        /* <status> */
        Set<String> activeNodes = new HashSet<String>();
        final Node statusNode = getChildNode(cibNode, "status");
        if (statusNode != null) {
            /* <node_state ...> */
            final NodeList nodes = statusNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node nodeStateNode = nodes.item(i);
                if ("node_state".equals(nodeStateNode.getNodeName())) {
                    final String id = getAttribute(nodeStateNode, "id");
                    final String uname = getAttribute(nodeStateNode, "uname");
                    final String crmd = getAttribute(nodeStateNode, "crmd");
                    final String shutdown =
                                        getAttribute(nodeStateNode, "shutdown");
                    final String inCcm = getAttribute(nodeStateNode, "in_ccm");
                    /* active / dead */
                    final String ha = getAttribute(nodeStateNode, "ha");
                    final String join = getAttribute(nodeStateNode, "join");
                    final String expected =
                                    getAttribute(nodeStateNode, "expected");
                    /* TODO: check and use other stuff too. */
                    if ("active".equals(ha)) {
                        activeNodes.add(uname);
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
        cibQueryData.setResOpIds(resOpIdsMap);
        cibQueryData.setActiveNodes(activeNodes);
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
    public final String[] getOrderParamPossibleChoices(final String param) {
        return paramOrdPossibleChoices.get(param);
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
        return Tools.getString("HeartbeatXML.OrderSectionParams");
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
            final Pattern p = Pattern.compile("^-?(\\d*|INFINITY)$");
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
    public final String[] getColocationParamPossibleChoices(final String param) {
        return paramColPossibleChoices.get(param);
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
        return Tools.getString("HeartbeatXML.ColocationSectionParams");
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
            final Pattern p = Pattern.compile("^-?(\\d*|INFINITY)$");
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
}
