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
import drbd.utilities.SSH;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Collections;
import java.util.Locale;
import EDU.oswego.cs.dl.util.concurrent.Mutex;
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
    /** List of not advanced global parameters. */
    private final List<String> globalNotAdvancedParams =
                                                     new ArrayList<String>();
    /** Map from global parameter to its access type. */
    private final Map<String, ConfigData.AccessType> paramGlobalAccessTypes =
                                  new HashMap<String, ConfigData.AccessType>();
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
    /** List of parameters for colocation in resource sets.
     (rsc_colocation tag) */
    private final List<String> rscSetColParams = new ArrayList<String>();
    /** List of parameters for colocation in resource sets.
     (resource_set tag) */
    private final List<String> rscSetColConnectionParams =
                                                    new ArrayList<String>();
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
    /** List of parameters for order in resource sets. (rsc_order tag) */
    private final List<String> rscSetOrdParams = new ArrayList<String>();
    /** List of parameters for order in resource sets. (resource_set tag) */
    private final List<String> rscSetOrdConnectionParams =
                                                    new ArrayList<String>();
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
                                  new ResourceAgent(ConfigData.PM_GROUP_NAME,
                                                    "",
                                                    "group");
    /** Predefined clone as pacemaker service. */
    private final ResourceAgent pcmkClone;
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
    /** Choices for combo box in stonith hostlists. */
    private final List<String> hostlistChoices = new ArrayList<String>();

    /** Pacemaker "true" string. */
    public static final String PCMK_TRUE = "true";
    /** Pacemaker "false" string. */
    public static final String PCMK_FALSE = "false";
    /** Disabled string. */
    public static final String DISABLED_STRING = "disabled";
    /** Boolean parameter type. */
    private static final String PARAM_TYPE_BOOLEAN = "boolean";
    /** Integer parameter type. */
    private static final String PARAM_TYPE_INTEGER = "integer";
    /** Label parameter type. */
    private static final String PARAM_TYPE_LABEL = "label";
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
    /** Alternative INFINITY keyword. */
    public static final String PLUS_INFINITY_STRING = "+INFINITY";
    /** -INFINITY keyword. */
    public static final String MINUS_INFINITY_STRING = "-INFINITY";
    /** Choices for integer fields. */
    private static final String[] INTEGER_VALUES = {null,
                                                    "0",
                                                    "2",
                                                    "100",
                                                    INFINITY_STRING,
                                                    MINUS_INFINITY_STRING,
                                                    PLUS_INFINITY_STRING};
    /** Name of the stonith timeout instance attribute. */
    private static final String STONITH_TIMEOUT_INSTANCE_ATTR =
                                                            "stonith-timeout";
    /** Name of the stonith priority instance attribute.
        It is actually only "priority" but it clashes with priority meta
        attribute. It is converted, wenn it is parsed and when it is stored to
        cib. */
    public static final String STONITH_PRIORITY_INSTANCE_ATTR =
                                                            "stonith-priority";
    /** Constraint score keyword. */
    public static final String SCORE_STRING = "score";
    /** Meta attributes for primitives. Cannot be static because it changes
     * with versions. */
    private Map<String, String> metaAttrParams = null;
    /** Meta attributes for rsc defaults meta attributes. */
    private Map<String, String> rscDefaultsMetaAttrs = null;
    /** Name of the priority meta attribute. */
    private static final String PRIORITY_META_ATTR = "priority";
    /** Name of the resource-stickiness meta attribute. */
    private static final String RESOURCE_STICKINESS_META_ATTR =
                                                         "resource-stickiness";
    /** Name of the migration-threshold meta attribute. */
    private static final String MIGRATION_THRESHOLD_META_ATTR =
                                                         "migration-threshold";
    /** Name of the failure-timeout meta attribute. */
    private static final String FAILURE_TIMEOUT_META_ATTR = "failure-timeout";
    /** Name of the multiple-timeout meta attribute. */
    private static final String MULTIPLE_ACTIVE_META_ATTR = "multiple-active";
    /** Name of the target-role meta attribute. */
    private static final String TARGET_ROLE_META_ATTR = "target-role";
    /** Name of the is-managed meta attribute. */
    private static final String IS_MANAGED_META_ATTR = "is-managed";
    /** Name of the master-max clone meta attribute. */
    private static final String MASTER_MAX_META_ATTR = "master-max";
    /** Name of the master-node-max clone meta attribute. */
    private static final String MASTER_NODE_MAX_META_ATTR = "master-node-max";
    /** Name of the clone-max clone meta attribute. */
    private static final String CLONE_MAX_META_ATTR = "clone-max";
    /** Name of the clone-node-max clone meta attribute. */
    private static final String CLONE_NODE_MAX_META_ATTR = "clone-node-max";
    /** Name of the notify clone meta attribute. */
    private static final String NOTIFY_META_ATTR = "notify";
    /** Name of the globally-unique clone meta attribute. */
    private static final String GLOBALLY_UNIQUE_META_ATTR = "globally-unique";
    /** Name of the ordered clone meta attribute. */
    private static final String ORDERED_META_ATTR = "ordered";
    /** Name of the interleave clone meta attribute. */
    private static final String INTERLEAVE_META_ATTR = "interleave";
    /** Name of the ordered group meta attribute. It has different default than
     * clone "ordered". */
    public static final String GROUP_ORDERED_META_ATTR = "group-ordered";
    /** Name of the collocated group meta attribute. */
    private static final String GROUP_COLLOCATED_META_ATTR = "collocated";

    /** Section for meta attributes in rsc_defaults. */
    private static final Map<String, String> M_A_SECTION =
                                                 new HashMap<String, String>();
    /** List of meta attributes that are not advanced. */
    private static final List<String> M_A_NOT_ADVANCED =
                                                       new ArrayList<String>();
    /** Access type of meta attributes. */
    private static final Map<String, ConfigData.AccessType> M_A_ACCESS_TYPE =
                                  new HashMap<String, ConfigData.AccessType>();
    /** Access type of meta attributes in rsc defaults. */
    private static final Map<String, ConfigData.AccessType>
                                M_A_RSC_DEFAULTS_ACCESS_TYPE =
                                  new HashMap<String, ConfigData.AccessType>();
    /** Possible choices for meta attributes. */
    private static final Map<String, String[]> M_A_POSSIBLE_CHOICES =
                                                new HashMap<String, String[]>();
    /** Possible choices for m/s meta attributes. */
    private static final Map<String, String[]> M_A_POSSIBLE_CHOICES_MS =
                                                new HashMap<String, String[]>();
    /** Short descriptions for meta attributes. */
    private static final Map<String, String> M_A_SHORT_DESC =
                                                new HashMap<String, String>();
    /** Long descriptions for meta attributes. */
    private static final Map<String, String> M_A_LONG_DESC =
                                                new HashMap<String, String>();
    /** Defaults for meta attributes. */
    private static final Map<String, String> M_A_DEFAULT =
                                                new HashMap<String, String>();
    /** Types for meta attributes. */
    private static final Map<String, String> M_A_TYPE =
                                                new HashMap<String, String>();
    /** Preferred values for meta attributes. */
    private static final Map<String, String> M_A_PREFERRED =
                                                new HashMap<String, String>();
    /** Array of boolean values names in the cluster manager. */
    private static final String[] PCMK_BOOLEAN_VALUES = {PCMK_TRUE, PCMK_FALSE};
    static {
        /* target-role */
        M_A_POSSIBLE_CHOICES.put(
               TARGET_ROLE_META_ATTR,
               new String[]{null, TARGET_ROLE_STARTED, TARGET_ROLE_STOPPED});
        M_A_POSSIBLE_CHOICES_MS.put(
                   TARGET_ROLE_META_ATTR,
                   new String[]{null,
                                TARGET_ROLE_MASTER,
                                TARGET_ROLE_STARTED,
                                TARGET_ROLE_SLAVE,
                                TARGET_ROLE_STOPPED});
        M_A_SHORT_DESC.put(TARGET_ROLE_META_ATTR,
                           Tools.getString("CRMXML.TargetRole.ShortDesc"));
        M_A_LONG_DESC.put(TARGET_ROLE_META_ATTR,
                          Tools.getString("CRMXML.TargetRole.LongDesc"));
        M_A_DEFAULT.put(TARGET_ROLE_META_ATTR, null);
        M_A_NOT_ADVANCED.add(TARGET_ROLE_META_ATTR);

        /* is-managed */
        M_A_POSSIBLE_CHOICES.put(IS_MANAGED_META_ATTR, PCMK_BOOLEAN_VALUES);
        M_A_SHORT_DESC.put(IS_MANAGED_META_ATTR,
                           Tools.getString("CRMXML.IsManaged.ShortDesc"));
        M_A_LONG_DESC.put(IS_MANAGED_META_ATTR,
                          Tools.getString("CRMXML.IsManaged.LongDesc"));
        M_A_DEFAULT.put(IS_MANAGED_META_ATTR, PCMK_TRUE);
        M_A_TYPE.put(IS_MANAGED_META_ATTR, PARAM_TYPE_BOOLEAN);
        M_A_NOT_ADVANCED.add(IS_MANAGED_META_ATTR);

        /* priority */
        M_A_POSSIBLE_CHOICES.put(PRIORITY_META_ATTR,
                                 new String[]{"0", "5", "10"});
        M_A_SHORT_DESC.put(PRIORITY_META_ATTR,
                           Tools.getString("CRMXML.Priority.ShortDesc"));
        M_A_LONG_DESC.put(PRIORITY_META_ATTR,
                          Tools.getString("CRMXML.Priority.LongDesc"));
        M_A_DEFAULT.put(PRIORITY_META_ATTR, "0");
        M_A_TYPE.put(PRIORITY_META_ATTR, PARAM_TYPE_INTEGER);

        /* resource-stickiness since 2.1.4 */
        M_A_POSSIBLE_CHOICES.put(RESOURCE_STICKINESS_META_ATTR,
                                 INTEGER_VALUES);
        M_A_SHORT_DESC.put(
                      RESOURCE_STICKINESS_META_ATTR,
                      Tools.getString("CRMXML.ResourceStickiness.ShortDesc"));
        M_A_LONG_DESC.put(
                        RESOURCE_STICKINESS_META_ATTR,
                        Tools.getString("CRMXML.ResourceStickiness.LongDesc"));
        M_A_DEFAULT.put(RESOURCE_STICKINESS_META_ATTR, "0");
        M_A_TYPE.put(RESOURCE_STICKINESS_META_ATTR, PARAM_TYPE_INTEGER);
        M_A_NOT_ADVANCED.add(RESOURCE_STICKINESS_META_ATTR);

        /* migration-threshold */
        M_A_POSSIBLE_CHOICES.put(MIGRATION_THRESHOLD_META_ATTR,
                                 new String[]{DISABLED_STRING, "0", "5", "10"});
        M_A_SHORT_DESC.put(
                      MIGRATION_THRESHOLD_META_ATTR,
                      Tools.getString("CRMXML.MigrationThreshold.ShortDesc"));
        M_A_LONG_DESC.put(
                      MIGRATION_THRESHOLD_META_ATTR,
                      Tools.getString("CRMXML.MigrationThreshold.LongDesc"));
        M_A_DEFAULT.put(MIGRATION_THRESHOLD_META_ATTR, DISABLED_STRING);
        M_A_TYPE.put(MIGRATION_THRESHOLD_META_ATTR, PARAM_TYPE_INTEGER);

        /* failure-timeout since 2.1.4 */
        M_A_SHORT_DESC.put(FAILURE_TIMEOUT_META_ATTR,
                           Tools.getString("CRMXML.FailureTimeout.ShortDesc"));
        M_A_LONG_DESC.put(FAILURE_TIMEOUT_META_ATTR,
                          Tools.getString("CRMXML.FailureTimeout.LongDesc"));
        M_A_TYPE.put(FAILURE_TIMEOUT_META_ATTR, PARAM_TYPE_TIME);

        /* multiple-active */
        M_A_POSSIBLE_CHOICES.put(MULTIPLE_ACTIVE_META_ATTR,
                                 new String[]{"stop_start",
                                              "stop_only",
                                              "block"});
        M_A_SHORT_DESC.put(MULTIPLE_ACTIVE_META_ATTR,
                           Tools.getString("CRMXML.MultipleActive.ShortDesc"));
        M_A_LONG_DESC.put(MULTIPLE_ACTIVE_META_ATTR,
                          Tools.getString("CRMXML.MultipleActive.LongDesc"));
        M_A_DEFAULT.put(MULTIPLE_ACTIVE_META_ATTR, "stop_start");

        /* master-max */
        M_A_SHORT_DESC.put(MASTER_MAX_META_ATTR, "M/S Master-Max");
        M_A_DEFAULT.put(MASTER_MAX_META_ATTR, "1");
        M_A_TYPE.put(MASTER_MAX_META_ATTR, PARAM_TYPE_INTEGER);
        M_A_POSSIBLE_CHOICES.put(MASTER_MAX_META_ATTR, INTEGER_VALUES);
        M_A_SECTION.put(MASTER_MAX_META_ATTR,
                        "Master / Slave Resource Defaults");
        M_A_RSC_DEFAULTS_ACCESS_TYPE.put(MASTER_MAX_META_ATTR,
                                         ConfigData.AccessType.GOD);
        /* master-node-max */
        M_A_SHORT_DESC.put(MASTER_NODE_MAX_META_ATTR, "M/S Master-Node-Max");
        M_A_DEFAULT.put(MASTER_NODE_MAX_META_ATTR, "1");
        M_A_TYPE.put(MASTER_NODE_MAX_META_ATTR, PARAM_TYPE_INTEGER);
        M_A_POSSIBLE_CHOICES.put(MASTER_NODE_MAX_META_ATTR, INTEGER_VALUES);
        M_A_SECTION.put(MASTER_NODE_MAX_META_ATTR,
                        "Master / Slave Resource Defaults");
        M_A_RSC_DEFAULTS_ACCESS_TYPE.put(MASTER_NODE_MAX_META_ATTR,
                                         ConfigData.AccessType.GOD);
        /* clone-max */
        M_A_SHORT_DESC.put(CLONE_MAX_META_ATTR, "Clone Max");
        M_A_DEFAULT.put(CLONE_MAX_META_ATTR, "");
        M_A_PREFERRED.put(CLONE_MAX_META_ATTR, "2");
        M_A_TYPE.put(CLONE_MAX_META_ATTR, PARAM_TYPE_INTEGER);
        M_A_POSSIBLE_CHOICES.put(CLONE_MAX_META_ATTR, INTEGER_VALUES);
        M_A_SECTION.put(CLONE_MAX_META_ATTR, "Clone Resource Defaults");
        M_A_RSC_DEFAULTS_ACCESS_TYPE.put(CLONE_MAX_META_ATTR,
                                         ConfigData.AccessType.GOD);
        /* clone-node-max */
        M_A_SHORT_DESC.put(CLONE_NODE_MAX_META_ATTR, "Clone Node Max");
        M_A_DEFAULT.put(CLONE_NODE_MAX_META_ATTR, "1");
        M_A_TYPE.put(CLONE_NODE_MAX_META_ATTR, PARAM_TYPE_INTEGER);
        M_A_POSSIBLE_CHOICES.put(CLONE_NODE_MAX_META_ATTR, INTEGER_VALUES);
        M_A_SECTION.put(CLONE_NODE_MAX_META_ATTR, "Clone Resource Defaults");
        M_A_RSC_DEFAULTS_ACCESS_TYPE.put(CLONE_NODE_MAX_META_ATTR,
                                         ConfigData.AccessType.GOD);
        /* notify */
        M_A_SHORT_DESC.put(NOTIFY_META_ATTR, "Notify");
        M_A_DEFAULT.put(NOTIFY_META_ATTR, PCMK_FALSE);
        M_A_PREFERRED.put(NOTIFY_META_ATTR, PCMK_TRUE);
        M_A_POSSIBLE_CHOICES.put(NOTIFY_META_ATTR, PCMK_BOOLEAN_VALUES);
        M_A_SECTION.put(NOTIFY_META_ATTR, "Clone Resource Defaults");
        M_A_RSC_DEFAULTS_ACCESS_TYPE.put(NOTIFY_META_ATTR,
                                         ConfigData.AccessType.GOD);
        /* globally-unique */
        M_A_SHORT_DESC.put(GLOBALLY_UNIQUE_META_ATTR, "Globally-Unique");
        M_A_DEFAULT.put(GLOBALLY_UNIQUE_META_ATTR, PCMK_FALSE);
        M_A_POSSIBLE_CHOICES.put(GLOBALLY_UNIQUE_META_ATTR,
                                 PCMK_BOOLEAN_VALUES);
        M_A_SECTION.put(GLOBALLY_UNIQUE_META_ATTR, "Clone Resource Defaults");
        M_A_RSC_DEFAULTS_ACCESS_TYPE.put(GLOBALLY_UNIQUE_META_ATTR,
                                         ConfigData.AccessType.GOD);
        /* ordered */
        M_A_SHORT_DESC.put(ORDERED_META_ATTR, "Ordered");
        M_A_DEFAULT.put(ORDERED_META_ATTR, PCMK_FALSE);
        M_A_POSSIBLE_CHOICES.put(ORDERED_META_ATTR, PCMK_BOOLEAN_VALUES);
        M_A_SECTION.put(ORDERED_META_ATTR, "Clone Resource Defaults");
        M_A_RSC_DEFAULTS_ACCESS_TYPE.put(ORDERED_META_ATTR,
                                         ConfigData.AccessType.GOD);
        /* interleave */
        M_A_SHORT_DESC.put(INTERLEAVE_META_ATTR, "Interleave");
        M_A_DEFAULT.put(INTERLEAVE_META_ATTR, PCMK_FALSE);
        M_A_POSSIBLE_CHOICES.put(INTERLEAVE_META_ATTR, PCMK_BOOLEAN_VALUES);
        M_A_SECTION.put(INTERLEAVE_META_ATTR, "Clone Resource Defaults");
        M_A_RSC_DEFAULTS_ACCESS_TYPE.put(INTERLEAVE_META_ATTR,
                                         ConfigData.AccessType.GOD);
        /* Group collocated */
        M_A_SHORT_DESC.put(GROUP_COLLOCATED_META_ATTR, "Collocated");
        M_A_DEFAULT.put(GROUP_COLLOCATED_META_ATTR, PCMK_TRUE);
        M_A_POSSIBLE_CHOICES.put(GROUP_COLLOCATED_META_ATTR,
                                 PCMK_BOOLEAN_VALUES);
        M_A_RSC_DEFAULTS_ACCESS_TYPE.put(GROUP_COLLOCATED_META_ATTR,
                                         ConfigData.AccessType.ADMIN);
        /* group ordered */
        M_A_SHORT_DESC.put(GROUP_ORDERED_META_ATTR, "Ordered");
        M_A_DEFAULT.put(GROUP_ORDERED_META_ATTR, PCMK_TRUE);
        M_A_POSSIBLE_CHOICES.put(GROUP_ORDERED_META_ATTR, PCMK_BOOLEAN_VALUES);
        M_A_RSC_DEFAULTS_ACCESS_TYPE.put(GROUP_ORDERED_META_ATTR,
                                         ConfigData.AccessType.ADMIN);
    }
    /**
     * Prepares a new <code>CRMXML</code> object.
     */
    public CRMXML(final Host host) {
        super();
        this.host = host;
        String command = null;
        final String hbV = host.getHeartbeatVersion();
        final String pcmkV = host.getPacemakerVersion();
        final String[] booleanValues = PCMK_BOOLEAN_VALUES;
        final String hbBooleanTrue = booleanValues[0];
        final String hbBooleanFalse = booleanValues[1];
        /* hostlist choices for stonith */
        hostlistChoices.add("");
        final String[] hosts = host.getCluster().getHostNames();
        if (hosts != null && hosts.length < 8) {
            hostlistChoices.add(Tools.join(" ", hosts));
            for (final String h : hosts) {
                hostlistChoices.add(h);
            }
        }
        /* clones */
        pcmkClone = new ResourceAgent(ConfigData.PM_CLONE_SET_NAME,
                                      "",
                                      "clone");
        addMetaAttribute(pcmkClone, MASTER_MAX_META_ATTR,      null, true);
        addMetaAttribute(pcmkClone, MASTER_NODE_MAX_META_ATTR, null, true);
        addMetaAttribute(pcmkClone, CLONE_MAX_META_ATTR,       null, false);
        addMetaAttribute(pcmkClone, CLONE_NODE_MAX_META_ATTR,  null, false);
        addMetaAttribute(pcmkClone, NOTIFY_META_ATTR,          null, false);
        addMetaAttribute(pcmkClone, GLOBALLY_UNIQUE_META_ATTR, null, false);
        addMetaAttribute(pcmkClone, ORDERED_META_ATTR,         null, false);
        addMetaAttribute(pcmkClone, INTERLEAVE_META_ATTR,      null, false);

        addMetaAttribute(hbGroup, GROUP_ORDERED_META_ATTR, null, false);
        addMetaAttribute(hbGroup, GROUP_COLLOCATED_META_ATTR, null, false);
        /* groups */
        final Map<String, String> metaAttrParams = getMetaAttrParameters();
        for (final String metaAttr : metaAttrParams.keySet()) {
            addMetaAttribute(pcmkClone,
                             metaAttr,
                             metaAttrParams.get(metaAttr),
                             false);
            addMetaAttribute(hbGroup,
                             metaAttr,
                             metaAttrParams.get(metaAttr),
                             false);
        }

        if (pcmkV == null && Tools.compareVersions(hbV, "2.1.3") <= 0) {
            command = host.getDistCommand("Heartbeat.2.1.3.getOCFParameters",
                                          (ConvertCmdCallback) null);
        }

        if ((command == null || "".equals(command))
            && pcmkV == null
            && Tools.compareVersions(hbV, "2.1.4") <= 0) {
            command = host.getDistCommand("Heartbeat.2.1.4.getOCFParameters",
                                          (ConvertCmdCallback) null);
        }

        if (command == null || "".equals(command)) {
            command = host.getDistCommand("Heartbeat.getOCFParameters",
                                          (ConvertCmdCallback) null);
        }
        final SSH.SSHOutput ret =
                    Tools.execCommandProgressIndicator(
                            host,
                            command,
                            null,  /* ExecCallback */
                            false, /* outputVisible */
                            Tools.getString("CRMXML.GetOCFParameters"),
                            300000);
        if (ret.getExitCode() != 0) {
            return;
        }
        final String output = ret.getOutput();
        if (output == null) {
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
            /*
            <resource-agent name="AudibleAlarm">
             ...
            </resource-agent>
            */
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
                    xml.delete(0, xml.length());
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
        globalNotAdvancedParams.add("symmetric-cluster");

        /* stonith enabled */
        globalParams.add("stonith-enabled");
        paramGlobalShortDescMap.put("stonith-enabled", "Stonith Enabled");
        paramGlobalLongDescMap.put("stonith-enabled", "Stonith Enabled");
        paramGlobalTypeMap.put("stonith-enabled", PARAM_TYPE_BOOLEAN);
        paramGlobalDefaultMap.put("stonith-enabled", hbBooleanTrue);
        paramGlobalPossibleChoices.put("stonith-enabled", booleanValues);
        globalRequiredParams.add("stonith-enabled");
        globalNotAdvancedParams.add("stonith-enabled");

        /* transition timeout */
        globalParams.add("default-action-timeout");
        paramGlobalShortDescMap.put("default-action-timeout",
                                    "Transition Timeout");
        paramGlobalLongDescMap.put("default-action-timeout",
                                "Transition Timeout");
        paramGlobalTypeMap.put("default-action-timeout", PARAM_TYPE_INTEGER);
        paramGlobalDefaultMap.put("default-action-timeout", "20");
        paramGlobalPossibleChoices.put("default-action-timeout",
                                       INTEGER_VALUES);
        globalRequiredParams.add("default-action-timeout");

        /* resource stickiness */
        /* special case: is advanced parameter if not set. */
        globalParams.add("default-resource-stickiness");
        paramGlobalShortDescMap.put("default-resource-stickiness",
                                    "Resource Stickiness");
        paramGlobalLongDescMap.put("default-resource-stickiness",
                                   "Resource Stickiness");
        paramGlobalTypeMap.put("default-resource-stickiness",
                               PARAM_TYPE_INTEGER);
        paramGlobalPossibleChoices.put("default-resource-stickiness",
                                       INTEGER_VALUES);
        paramGlobalDefaultMap.put("default-resource-stickiness", "0");
        globalRequiredParams.add("default-resource-stickiness");

        /* no quorum policy */
        globalParams.add("no-quorum-policy");
        paramGlobalShortDescMap.put("no-quorum-policy", "No Quorum Policy");
        paramGlobalLongDescMap.put("no-quorum-policy", "No Quorum Policy");
        paramGlobalTypeMap.put("no-quorum-policy", PARAM_TYPE_STRING);
        paramGlobalDefaultMap.put("no-quorum-policy", "stop");
        paramGlobalPossibleChoices.put("no-quorum-policy",
                                       new String[]{"ignore",
                                                    "stop",
                                                    "freeze",
                                                    "suicide"});
        globalRequiredParams.add("no-quorum-policy");
        globalNotAdvancedParams.add("no-quorum-policy");

        /* resource failure stickiness */
        globalParams.add("default-resource-failure-stickiness");
        paramGlobalShortDescMap.put("default-resource-failure-stickiness",
                                    "Resource Failure Stickiness");
        paramGlobalLongDescMap.put("default-resource-failure-stickiness",
                                   "Resource Failure Stickiness");
        paramGlobalTypeMap.put("default-resource-failure-stickiness",
                               PARAM_TYPE_INTEGER);
        paramGlobalPossibleChoices.put("default-resource-failure-stickiness",
                                       INTEGER_VALUES);
        paramGlobalDefaultMap.put("default-resource-failure-stickiness", "0");
        globalRequiredParams.add("default-resource-failure-stickiness");


        if (pcmkV != null || Tools.compareVersions(hbV, "2.1.3") >= 0) {
            String clusterRecheckInterval = "cluster-recheck-interval";
            String dcDeadtime = "dc-deadtime";
            String electionTimeout = "election-timeout";
            String shutdownEscalation = "shutdown-escalation";
            if (pcmkV == null && Tools.compareVersions(hbV, "2.1.4") <= 0) {
                clusterRecheckInterval = "cluster_recheck_interval";
                dcDeadtime = "dc_deadtime";
                electionTimeout = "election_timeout";
                shutdownEscalation = "shutdown_escalation";
            }
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
                dcDeadtime,
                clusterRecheckInterval,
                electionTimeout,
                shutdownEscalation,
                "crmd-integration-timeout",
                "crmd-finalization-timeout"
            };
            globalParams.add("dc-version");
            paramGlobalShortDescMap.put("dc-version", "DC Version");
            paramGlobalTypeMap.put("dc-version", PARAM_TYPE_LABEL);
            paramGlobalAccessTypes.put("dc-version",
                                       ConfigData.AccessType.NEVER);
            globalParams.add("cluster-infrastructure");
            paramGlobalShortDescMap.put("cluster-infrastructure",
                                        "Cluster Infrastructure");
            paramGlobalTypeMap.put("cluster-infrastructure", PARAM_TYPE_LABEL);
            paramGlobalAccessTypes.put("cluster-infrastructure",
                                       ConfigData.AccessType.NEVER);

            globalNotAdvancedParams.add("no-quorum-policy");
            globalNotAdvancedParams.add("maintenance-mode");
            paramGlobalAccessTypes.put("maintenance-mode",
                                       ConfigData.AccessType.OP);
            globalNotAdvancedParams.add(clusterRecheckInterval);

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
        colParams.add("rsc-role");
        paramColShortDescMap.put("rsc-role", "rsc col role");
        paramColLongDescMap.put("rsc-role", "@RSC@ colocation role");
        paramColTypeMap.put("rsc-role", PARAM_TYPE_STRING);
        paramColPossibleChoices.put("rsc-role", ATTRIBUTE_ROLES);
        paramColPossibleChoicesMS.put("rsc-role", ATTRIBUTE_ROLES_MS);

        colParams.add("with-rsc-role");
        paramColShortDescMap.put("with-rsc-role", "with-rsc col role");
        paramColLongDescMap.put("with-rsc-role", "@WITH-RSC@ colocation role");
        paramColTypeMap.put("with-rsc-role", PARAM_TYPE_STRING);
        paramColPossibleChoices.put("with-rsc-role", ATTRIBUTE_ROLES);
        paramColPossibleChoicesMS.put("with-rsc-role", ATTRIBUTE_ROLES_MS);

        colParams.add(SCORE_STRING);
        paramColShortDescMap.put(SCORE_STRING, "Score");
        paramColLongDescMap.put(SCORE_STRING, "Score");
        paramColTypeMap.put(SCORE_STRING, PARAM_TYPE_INTEGER);
        paramColDefaultMap.put(SCORE_STRING, null);
        paramColPreferredMap.put(SCORE_STRING, INFINITY_STRING);
        paramColPossibleChoices.put(SCORE_STRING, INTEGER_VALUES);
        /* Hardcoding order params */
        ordParams.add("first-action");
        paramOrdShortDescMap.put("first-action", "first order action");
        paramOrdLongDescMap.put("first-action", "@FIRST-RSC@ order action");
        paramOrdTypeMap.put("first-action", PARAM_TYPE_STRING);
        paramOrdPossibleChoices.put("first-action", ATTRIBUTE_ACTIONS);
        paramOrdPossibleChoicesMS.put("first-action", ATTRIBUTE_ACTIONS_MS);
        paramOrdPreferredMap.put(SCORE_STRING, INFINITY_STRING);
        paramOrdDefaultMap.put("first-action", null);

        ordParams.add("then-action");
        paramOrdShortDescMap.put("then-action", "then order action");
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
        paramOrdPossibleChoices.put(SCORE_STRING, INTEGER_VALUES);
        paramOrdDefaultMap.put(SCORE_STRING, null);
        /* resource sets */
        rscSetOrdParams.add(SCORE_STRING);
        rscSetColParams.add(SCORE_STRING);

        rscSetOrdConnectionParams.add("action");
        paramOrdShortDescMap.put("action", "order action");
        paramOrdLongDescMap.put("action", "order action");
        paramOrdTypeMap.put("action", PARAM_TYPE_STRING);
        paramOrdPossibleChoices.put("action", ATTRIBUTE_ACTIONS);
        paramOrdPossibleChoicesMS.put("action", ATTRIBUTE_ACTIONS_MS);
        paramOrdDefaultMap.put("action", null);

        rscSetOrdConnectionParams.add("sequential");
        paramOrdShortDescMap.put("sequential", "sequential");
        paramOrdLongDescMap.put("sequential", "sequential");
        paramOrdTypeMap.put("sequential", PARAM_TYPE_BOOLEAN);
        paramOrdDefaultMap.put("sequential", hbBooleanTrue);
        paramOrdPossibleChoices.put("sequential", booleanValues);
        paramOrdPreferredMap.put("sequential", hbBooleanFalse);

        rscSetColConnectionParams.add("role");
        paramColShortDescMap.put("role", "col role");
        paramColLongDescMap.put("role", "colocation role");
        paramColTypeMap.put("role", PARAM_TYPE_STRING);
        paramColPossibleChoices.put("role", ATTRIBUTE_ROLES);
        paramColPossibleChoicesMS.put("role", ATTRIBUTE_ROLES_MS);

        rscSetColConnectionParams.add("sequential");
        paramColShortDescMap.put("sequential", "sequential");
        paramColLongDescMap.put("sequential", "sequential");
        paramColTypeMap.put("sequential", PARAM_TYPE_BOOLEAN);
        paramColDefaultMap.put("sequential", hbBooleanTrue);
        paramColPossibleChoices.put("sequential", booleanValues);
        paramColPreferredMap.put("sequential", hbBooleanFalse);
    }

    /** Returns choices for check box. (True, False). */
    public final String[] getCheckBoxChoices(final ResourceAgent ra,
                                             final String param) {
        final String paramDefault = getParamDefault(ra, param);
        return getCheckBoxChoices(paramDefault);
    }

    /**
     * Returns choices for check box. (True, False).
     * The problem is, that heartbeat kept changing the lower and upper case in
     * the true and false values.
     */
    private String[] getCheckBoxChoices(final String paramDefault) {
        if (paramDefault != null) {
            if ("yes".equals(paramDefault) || "no".equals(paramDefault)) {
                return new String[]{"yes", "no"};
            } else if ("Yes".equals(paramDefault)
                       || "No".equals(paramDefault)) {
                return new String[]{"Yes", "No"};
            } else if (PCMK_TRUE.equals(paramDefault)
                       || PCMK_FALSE.equals(paramDefault)) {
                return PCMK_BOOLEAN_VALUES;
            } else if ("True".equals(paramDefault)
                       || "False".equals(paramDefault)) {
                return new String[]{"True", "False"};
            }
        }
        final String hbV = host.getHeartbeatVersion();
        final String pcmkV = host.getPacemakerVersion();
        return PCMK_BOOLEAN_VALUES;
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
    public final String[] getParameters(final ResourceAgent ra,
                                        final boolean master) {
        /* return cached values */
        return ra.getParameters(master);
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
                                                  final String param,
                                                  final boolean ms) {
        if (ms) {
            return ra.getParamPossibleChoicesMS(param);
        } else {
            return ra.getParamPossibleChoices(param);
        }
    }

    /**
     * Checks if the global parameter is advanced.
     */
    public final boolean isGlobalAdvanced(final String param) {
        return !globalNotAdvancedParams.contains(param);
    }

    /**
     * Returns the global parameter's access type.
     */
    public final ConfigData.AccessType getGlobalAccessType(final String param) {
        final ConfigData.AccessType at = paramGlobalAccessTypes.get(param);
        if (at == null) {
            return ConfigData.AccessType.ADMIN; /* default access type */
        }
        return at;
    }

    /**
     * Checks if parameter is required or not.
     */
    public final boolean isGlobalRequired(final String param) {
        return globalRequiredParams.contains(param);
    }

    /**
     * Checks if parameter is advanced or not.
     */
    public final boolean isAdvanced(final ResourceAgent ra,
                                    final String param) {
        if (isMetaAttr(ra, param)) {
            if (ra == hbGroup || ra == pcmkClone) {
                return true;
            }
            return !M_A_NOT_ADVANCED.contains(param);
        }
        return !isRequired(ra, param);
    }

    /**
     * Returns access type of the parameter.
     */
    public final ConfigData.AccessType getAccessType(final ResourceAgent ra,
                                                     final String param) {
        if (isMetaAttr(ra, param)) {
            final ConfigData.AccessType accessType =
                                                M_A_ACCESS_TYPE.get(param);
            if (accessType != null) {
                return accessType;
            }
        }
        return ConfigData.AccessType.ADMIN;
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

    /** Returns whether the parameter expects an integer value. */
    public final boolean isInteger(final ResourceAgent ra,
                                   final String param) {
        final String type = getParamType(ra, param);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    /** Returns whether the parameter is read only label value. */
    public final boolean isLabel(final ResourceAgent ra,
                                 final String param) {
        final String type = getParamType(ra, param);
        return PARAM_TYPE_LABEL.equals(type);
    }

    /**
     * Returns whether the parameter expects a boolean value.
     */
    public final boolean isBoolean(final ResourceAgent ra,
                                   final String param) {
        final String type = getParamType(ra, param);
        return PARAM_TYPE_BOOLEAN.equals(type);
    }

    /** Returns whether the global parameter expects an integer value. */
    public final boolean isGlobalInteger(final String param) {
        final String type = getGlobalParamType(param);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    /** Returns whether the global parameter expects a label value. */
    public final boolean isGlobalLabel(final String param) {
        final String type = getGlobalParamType(param);
        return PARAM_TYPE_LABEL.equals(type);
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
        final String section = ra.getSection(param);
        if (section != null) {
            return section;
        }
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
            return Tools.getString("CRMXML.GlobalRequiredOptions");
        } else {
            return Tools.getString("CRMXML.GlobalOptionalOptions");
        }
    }

    /**
     * Converts rsc default parameter to the internal representation, that can
     * be different on older cluster software.
     */
    private String convertRscDefaultsParam(final String param) {
        final String newParam = rscDefaultsMetaAttrs.get(param);
        if (newParam == null) {
            return param;
        }
        return newParam;
    }
    /**
     * Checks meta attribute param.
     */
    public final boolean checkMetaAttrParam(final String param,
                                            final String value) {
        final String newParam = convertRscDefaultsParam(param);
        final String type = M_A_TYPE.get(newParam);
        final boolean required = isRscDefaultsRequired(newParam);
        final boolean metaAttr = true;
        return checkParam(type, required, metaAttr, newParam, value);
    }

    /**
     * Returns section of the rsc defaults meta attribute.
     */
    public final String getRscDefaultsSection(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        final String section = M_A_SECTION.get(newParam);
        if (section == null) {
            return Tools.getString("CRMXML.RscDefaultsSection");
        }
        return section;
    }

    /**
     * Returns default of the meta attribute.
     */
    public final String getRscDefaultsDefault(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        return M_A_DEFAULT.get(newParam);
    }

    /**
     * Returns preferred of the meta attribute.
     */
    public final String getRscDefaultsPreferred(final String param) {
        return null;
    }

    /**
     * Returns preferred of the meta attribute.
     */
    public final String[] getRscDefaultsPossibleChoices(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        return M_A_POSSIBLE_CHOICES.get(newParam);
    }

    /**
     * Returns choices for check box. (True, False).
     */
    public final String[] getRscDefaultsCheckBoxChoices(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        final String paramDefault = getRscDefaultsDefault(newParam);
        return getCheckBoxChoices(paramDefault);
    }

    /**
     * Returns short description of the default meta attr parameter.
     */
    public final String getRscDefaultsShortDesc(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        return M_A_SHORT_DESC.get(newParam);
    }

    /**
     * Return long description of the default meta attr parameter.
     */
    public final String getRscDefaultsLongDesc(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        return M_A_LONG_DESC.get(newParam);
    }

    /**
     * Returns type of the meta attribute.
     * It can be string, integer, boolean...
     */
    public final String getRscDefaultsType(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        return M_A_TYPE.get(newParam);
    }

    /** Checks if parameter is advanced. */
    public final boolean isRscDefaultsAdvanced(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        return !M_A_NOT_ADVANCED.contains(newParam);
    }

    /** Returns access type of the meta attribute. */
    public final ConfigData.AccessType getRscDefaultsAccessType(
                                                         final String param) {
        final String newParam = convertRscDefaultsParam(param);
        final ConfigData.AccessType at =
                                M_A_RSC_DEFAULTS_ACCESS_TYPE.get(newParam);
        if (at == null) {
            return ConfigData.AccessType.ADMIN;
        }
        return at;
    }

    /**
     * Checks if parameter is required or not.
     */
    public final boolean isRscDefaultsRequired(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        return false;
    }

    /** Checks if the meta attr parameter is integer. */
    public final boolean isRscDefaultsInteger(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        final String type = getRscDefaultsType(newParam);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    /** Returns whether meta attr parameter is label. */
    public final boolean isRscDefaultsLabel(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        final String type = getRscDefaultsType(newParam);
        return PARAM_TYPE_LABEL.equals(type);
    }

    /**
     * Checks if the meta attr parameter is boolean.
     */
    public final boolean isRscDefaultsBoolean(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        final String type = getRscDefaultsType(newParam);
        return PARAM_TYPE_BOOLEAN.equals(type);
    }

    /**
     * Whether the rsc default parameter is of the time type.
     */
    public final boolean isRscDefaultsTimeType(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        final String type = getRscDefaultsType(newParam);
        return PARAM_TYPE_TIME.equals(type);
    }


    /**
     * Checks parameter of the specified ra according to its type.
     * Returns false if value does not fit the type.
     */
    public final boolean checkParam(final ResourceAgent ra,
                                    final String param,
                                    final String value) {
        final String type = getParamType(ra, param);
        final boolean required = isRequired(ra, param);
        final boolean metaAttr = isMetaAttr(ra, param);
        return checkParam(type, required, metaAttr, param, value);
    }

    /**
     * Checks parameter according to its type. Returns false if value does
     * not fit the type.
     */
    private boolean checkParam(final String type,
                               final boolean required,
                               final boolean metaAttr,
                               final String param,
                               String value) {
        if (metaAttr
            && isRscDefaultsInteger(param)
            && DISABLED_STRING.equals(value)) {
            value = "";
        }
        boolean correctValue = true;
        if (PARAM_TYPE_BOOLEAN.equals(type)) {
            if (!"yes".equals(value) && !"no".equals(value)
                && !PCMK_TRUE.equals(value)
                && !PCMK_FALSE.equals(value)
                && !"True".equals(value)
                && !"False".equals(value)) {
                correctValue = false;
            }
        } else if (PARAM_TYPE_INTEGER.equals(type)) {
            final Pattern p =
                 Pattern.compile("^(-?\\d*|(-|\\+)?" + INFINITY_STRING + ")$");
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
        } else if ((value == null || "".equals(value)) && required) {
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
                && !PCMK_TRUE.equals(value)
                && !PCMK_FALSE.equals(value)
                && !"True".equals(value)
                && !"False".equals(value)) {

                correctValue = false;
            }
        } else if (PARAM_TYPE_INTEGER.equals(type)) {
            final Pattern p =
                Pattern.compile("^(-?\\d*|(-|\\+)?" + INFINITY_STRING + ")$");
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
     * Adds meta attribute to the resource agent.
     */
    private void addMetaAttribute(final ResourceAgent ra,
                                  final String name,
                                  String newName,
                                  final boolean masterSlave) {
        if (newName == null) {
            newName = name;
        }
        if (masterSlave) {
            ra.addMasterParameter(name);
        } else {
            ra.addParameter(name);
        }
        ra.setParamIsMetaAttr(name, true);
        ra.setParamRequired(name, false);
        ra.setParamPossibleChoices(name, M_A_POSSIBLE_CHOICES.get(newName));
        ra.setParamPossibleChoicesMS(name,
                                     M_A_POSSIBLE_CHOICES_MS.get(newName));
        ra.setParamShortDesc(name, M_A_SHORT_DESC.get(newName));
        ra.setParamLongDesc(name, M_A_LONG_DESC.get(newName));
        ra.setParamDefault(name, M_A_DEFAULT.get(newName));
        ra.setParamType(name, M_A_TYPE.get(newName));
        ra.setParamPreferred(name, M_A_PREFERRED.get(newName));
    }

    /**
     * Returns meta attribute parameters. The key is always, how the parameter
     * is called in the cluster manager and value how it is stored in the GUI.
     */
    private Map<String, String> getMetaAttrParameters() {
        if (metaAttrParams != null) {
            return metaAttrParams;
        }
        metaAttrParams = new LinkedHashMap<String, String>();
        final String hbV = host.getHeartbeatVersion();
        final String pcmkV = host.getPacemakerVersion();
        if (pcmkV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
            metaAttrParams.put("target_role", TARGET_ROLE_META_ATTR);
            metaAttrParams.put("is_managed", IS_MANAGED_META_ATTR);
        } else {
            metaAttrParams.put(TARGET_ROLE_META_ATTR, null);
            metaAttrParams.put(IS_MANAGED_META_ATTR, null);
        }
        metaAttrParams.put(MIGRATION_THRESHOLD_META_ATTR, null);
        metaAttrParams.put(PRIORITY_META_ATTR, null);
        metaAttrParams.put(MULTIPLE_ACTIVE_META_ATTR, null);
        if (pcmkV != null || Tools.compareVersions(hbV, "2.1.4") >= 0) {
            metaAttrParams.put(RESOURCE_STICKINESS_META_ATTR, null);
            metaAttrParams.put(FAILURE_TIMEOUT_META_ATTR, null);
        }
        return metaAttrParams;
    }

    /**
     * Returns meta attribute parameters. The key is always, how the parameter
     * is called in the cluster manager and value how it is stored in the GUI.
     */
    public final Map<String, String> getRscDefaultsParameters() {
        if (rscDefaultsMetaAttrs != null) {
            return rscDefaultsMetaAttrs;
        }
        rscDefaultsMetaAttrs = new LinkedHashMap<String, String>();
        final String hbV = host.getHeartbeatVersion();
        final String pcmkV = host.getPacemakerVersion();
        if (pcmkV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.99.0") < 0) {
            /* no rsc defaults in older versions. */
            return rscDefaultsMetaAttrs;
        }

        for (final String param : getMetaAttrParameters().keySet()) {
            rscDefaultsMetaAttrs.put(param, getMetaAttrParameters().get(param));
        }
        /* Master / Slave */
        rscDefaultsMetaAttrs.put(MASTER_MAX_META_ATTR,      null);
        rscDefaultsMetaAttrs.put(MASTER_NODE_MAX_META_ATTR, null);
        /* Clone */
        rscDefaultsMetaAttrs.put(CLONE_MAX_META_ATTR,       null);
        rscDefaultsMetaAttrs.put(CLONE_NODE_MAX_META_ATTR,  null);
        rscDefaultsMetaAttrs.put(NOTIFY_META_ATTR,          null);
        rscDefaultsMetaAttrs.put(GLOBALLY_UNIQUE_META_ATTR, null);
        rscDefaultsMetaAttrs.put(ORDERED_META_ATTR,         null);
        rscDefaultsMetaAttrs.put(INTERLEAVE_META_ATTR,      null);
        return rscDefaultsMetaAttrs;
    }

    /**
     * Parses the parameters.
     */
    private void parseParameters(final ResourceAgent ra,
                                 final Node parametersNode) {
        final String hbV = host.getHeartbeatVersion();
        final String pcmkV = host.getPacemakerVersion();

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
                    ra.setParamLongDesc(param, Tools.trimText(longDesc));
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
                    String defaultValue = getAttribute(contentParamNode,
                                                                    "default");
                    if (ra.isIPaddr() && "nic".equals(param)) {
                        // workaround for default value in IPaddr and IPaddr2
                        defaultValue = "";
                    }
                    if ("force_stop".equals(param)
                        && "0".equals(defaultValue)) {
                        // Workaround, default is "0" and should be false
                        defaultValue = "false";
                    }
                    if (ra.isPingService()) {
                        /* workaround: all types are integer in this ras. */
                        ra.setProbablyClone(true);
                        if ("host_list".equals(param)) {
                            ra.setParamRequired(param, true);
                        }
                    } else {
                        ra.setParamType(param, type);
                    }
                    ra.setParamDefault(param, defaultValue);
                }
                if (ra.isStonith() && "hostlist".equals(param)) {
                    ra.setParamPossibleChoices(
                             param,
                             hostlistChoices.toArray(
                                          new String[hostlistChoices.size()]));
                }
            }
        }
        if (ra.isStonith()) {
            /* stonith-timeout */
            ra.addParameter(STONITH_TIMEOUT_INSTANCE_ATTR);
            ra.setParamShortDesc(STONITH_TIMEOUT_INSTANCE_ATTR,
                                 "Stonith Timeout");
            ra.setParamType(STONITH_TIMEOUT_INSTANCE_ATTR, PARAM_TYPE_TIME);
            ra.setParamDefault(STONITH_TIMEOUT_INSTANCE_ATTR, "");
            /* priority */
            ra.addParameter(STONITH_PRIORITY_INSTANCE_ATTR);

            ra.setParamShortDesc(STONITH_PRIORITY_INSTANCE_ATTR,
                                 "Stonith Priority");
            ra.setParamPossibleChoices(STONITH_PRIORITY_INSTANCE_ATTR,
                                       new String[]{"", "0", "5", "10"});
            ra.setParamType(STONITH_PRIORITY_INSTANCE_ATTR, PARAM_TYPE_INTEGER);
            ra.setParamDefault(STONITH_PRIORITY_INSTANCE_ATTR, "");

        }
        final Map<String, String> metaAttrParams = getMetaAttrParameters();
        for (final String metaAttr : metaAttrParams.keySet()) {
            addMetaAttribute(ra, metaAttr, metaAttrParams.get(metaAttr), false);
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
            ra.setLongDesc(Tools.trimText(getText(longdescNode)));
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
        ra.setProbablyMasterSlave(masterSlave);
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
        final String[] booleanValues = PCMK_BOOLEAN_VALUES;

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
                        paramGlobalLongDescMap.put(param,
                                                   Tools.trimText(longDesc));
                    }

                    /* <content> */
                    final Node contentParamNode = getChildNode(parameterNode,
                                                               "content");
                    if (contentParamNode != null) {
                        final String type = getAttribute(contentParamNode,
                                                         "type");
                        String defaultValue = getAttribute(contentParamNode,
                                                           "default");
                        paramGlobalTypeMap.put(param, type);
                        if (PARAM_TYPE_TIME.equals(type)) {
                            final Pattern p = Pattern.compile("^(\\d+)s$");
                            final Matcher m = p.matcher(defaultValue);
                            if (m.matches()) {
                                defaultValue = m.group(1);
                            }
                        }
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
                                                           INTEGER_VALUES);
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
        final ResourceAgent ra =
               (ResourceAgent) serviceToResourceAgentMap.get(serviceName,
                                                             provider,
                                                             raClass);
        if (ra == null) {
            Tools.appWarning(raClass + ":" + provider + ":" + serviceName
                             + " RA does not exist");
            final ResourceAgent notInstalledRA =
                            new ResourceAgent(serviceName, provider, raClass);
            return notInstalledRA;

        }
        return ra;
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
        return pcmkClone;
    }

    /**
     * Parse resource defaults.
     */
    public final String parseRscDefaults(
                    final Node rscDefaultsNode,
                    final Map<String, String> rscDefaultsParams,
                    final Map<String, String> rscDefaultsParamsNvpairIds) {

        final Map<String, String> nvpairIds =
                                        new HashMap<String, String>();
        /* <meta_attributtes> */
        final Node metaAttrsNode = getChildNode(rscDefaultsNode,
                                                "meta_attributes");
        String rscDefaultsId = null;
        if (metaAttrsNode != null) {
            rscDefaultsId = getAttribute(metaAttrsNode, "id");
            /* <attributtes> only til 2.1.4 */
            NodeList nvpairsMA;
            final String hbV = host.getHeartbeatVersion();
            final String pcmkV = host.getPacemakerVersion();
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
                    String value = getAttribute(maNode, "value");
                    if (TARGET_ROLE_META_ATTR.equals(name)) {
                        value = value.toLowerCase(Locale.US);
                    }
                    rscDefaultsParams.put(name, value);
                    rscDefaultsParamsNvpairIds.put(name, nvpairId);
                }
            }
        }
        return rscDefaultsId;
    }

    /**
     * Parse op defaults.
     */
    public final void parseOpDefaults(
                                final Node opDefaultsNode,
                                final Map<String, String> opDefaultsParams) {

        final Map<String, String> nvpairIds =
                                        new HashMap<String, String>();
        /* <meta_attributtes> */
        final Node metaAttrsNode = getChildNode(opDefaultsNode,
                                                "meta_attributes");
        if (metaAttrsNode != null) {
            /* <attributtes> only til 2.1.4 */
            NodeList nvpairsMA;
            final String hbV = host.getHeartbeatVersion();
            final String pcmkV = host.getPacemakerVersion();
            if (hbV != null && Tools.compareVersions(hbV, "2.99.0") < 0) {
                final Node attrsNode =
                                  getChildNode(metaAttrsNode, "attributes");
                nvpairsMA = attrsNode.getChildNodes();
            } else {
                nvpairsMA = metaAttrsNode.getChildNodes();
            }
            /* <nvpair...> */
            for (int l = 0; l < nvpairsMA.getLength(); l++) {
                final Node maNode = nvpairsMA.item(l);
                if (maNode.getNodeName().equals("nvpair")) {
                    final String name = getAttribute(maNode, "name");
                    final String value = getAttribute(maNode, "value");
                    opDefaultsParams.put(name, value);
                }
            }
        }
    }

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
              final Map<String, String> metaAttrsIdMap,
              final Map<String, String> operationsIdMap,
              final Map<String, Map<String, String>> resOpIdsMap,
              final Map<String, String> operationsIdRefs,
              final Map<String, String> operationsIdtoCRMId,
              final Map<String, String> metaAttrsIdRefs,
              final Map<String, String> metaAttrsIdToCRMId,
              final boolean stonith) {
        final Map<String, String> params = new HashMap<String, String>();
        parametersMap.put(crmId, params);
        final Map<String, String> nvpairIds = new HashMap<String, String>();
        parametersNvpairsIdsMap.put(crmId, nvpairIds);
        final String hbV = host.getHeartbeatVersion();
        final String pcmkV = host.getPacemakerVersion();
        /* <instance_attributes> */
        final Node instanceAttrNode = getChildNode(resourceNode,
                                                   "instance_attributes");
        /* <nvpair...> */
        if (instanceAttrNode != null) {
            final String iAId = getAttribute(instanceAttrNode, "id");
            resourceInstanceAttrIdMap.put(crmId, iAId);
            NodeList nvpairsRes;
            if (pcmkV == null
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
                    String name = getAttribute(optionNode, "name");
                    final String value = getAttribute(optionNode, "value");
                    if (stonith && "priority".equals(name)) {
                        /* so it does not clash with meta attr priority */
                        name = STONITH_PRIORITY_INSTANCE_ATTR;
                    }
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
            if (operationsIdRef == null) {
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
            } else {
                operationsIdRefs.put(crmId, operationsIdRef);
            }
        }

        /* <meta_attributtes> */
        final Node metaAttrsNode = getChildNode(resourceNode,
                                                "meta_attributes");
        if (metaAttrsNode != null) {
            final String metaAttrsIdRef = getAttribute(metaAttrsNode, "id-ref");
            if (metaAttrsIdRef == null) {
                final String metaAttrsId = getAttribute(metaAttrsNode, "id");
                metaAttrsIdMap.put(crmId, metaAttrsId);
                metaAttrsIdToCRMId.put(metaAttrsId, crmId);
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
                        String value = getAttribute(maNode, "value");
                        if (TARGET_ROLE_META_ATTR.equals(name)) {
                            value = value.toLowerCase(Locale.US);
                        }
                        params.put(name, value);
                        nvpairIds.put(name, nvpairId);
                    }
                }
            } else {
                metaAttrsIdRefs.put(crmId, metaAttrsIdRef);
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
                final Map<String, String> metaAttrsIdMap,
                final Map<String, String> operationsIdMap,
                final Map<String, Map<String, String>> resOpIdsMap,
                final Map<String, String> operationsIdRefs,
                final Map<String, String> operationsIdtoCRMId,
                final Map<String, String> metaAttrsIdRefs,
                final Map<String, String> metaAttrsIdToCRMId) {
        final NodeList primitives = groupNode.getChildNodes();
        final String groupId = getAttribute(groupNode, "id");
        final String hbV = host.getHeartbeatVersion();
        final String pcmkV = host.getPacemakerVersion();
        final Map<String, String> params =
                                        new HashMap<String, String>();
        parametersMap.put(groupId, params);
        final Map<String, String> nvpairIds =
                                        new HashMap<String, String>();
        parametersNvpairsIdsMap.put(groupId, nvpairIds);
        if (resList != null) {
            resList.add(groupId);
        }
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
                               metaAttrsIdMap,
                               operationsIdMap,
                               resOpIdsMap,
                               operationsIdRefs,
                               operationsIdtoCRMId,
                               metaAttrsIdRefs,
                               metaAttrsIdToCRMId);
            }
        }

        /* <meta_attributtes> */
        final Node metaAttrsNode = getChildNode(groupNode,
                                                "meta_attributes");
        if (metaAttrsNode != null) {
            final String metaAttrsIdRef = getAttribute(metaAttrsNode, "id-ref");
            if (metaAttrsIdRef == null) {
                final String metaAttrsId = getAttribute(metaAttrsNode, "id");
                metaAttrsIdMap.put(groupId, metaAttrsId);
                metaAttrsIdToCRMId.put(metaAttrsId, groupId);
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
                        String name = getAttribute(maNode, "name");
                        String value = getAttribute(maNode, "value");
                        if (TARGET_ROLE_META_ATTR.equals(name)) {
                            value = value.toLowerCase(Locale.US);
                        }
                        if ("ordered".equals(name)) {
                            name = GROUP_ORDERED_META_ATTR;
                        }
                        params.put(name, value);
                        nvpairIds.put(name, nvpairId);
                    }
                }
            } else {
                metaAttrsIdRefs.put(groupId, metaAttrsIdRef);
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
                final Map<String, String> metaAttrsIdMap,
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
                        metaAttrsIdMap,
                        operationsIdMap,
                        resOpIdsMap,
                        operationsIdRefs,
                        operationsIdtoCRMId,
                        metaAttrsIdRefs,
                        metaAttrsIdToCRMId,
                        "stonith".equals(raClass));
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

    /** Returns a hash with resource information. (running_on) */
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
                    if (TARGET_ROLE_STARTED.equalsIgnoreCase(
                                                    setNode.getNodeName())) {
                        final String node = getText(setNode);
                        if (runningOnList == null) {
                            runningOnList = new ArrayList<String>();
                        }
                        runningOnList.add(node);
                    } else if (TARGET_ROLE_MASTER.equalsIgnoreCase(
                                                      setNode.getNodeName())) {
                        final String node = getText(setNode);
                        if (masterOnList == null) {
                            masterOnList = new ArrayList<String>();
                        }
                        masterOnList.add(node);
                    } else if (TARGET_ROLE_SLAVE.equalsIgnoreCase(
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
    private void parseTransientAttributes(
                              final String uname,
                              final Node transientAttrNode,
                              final MultiKeyMap failedMap,
                              final Map<String, Set<String>> failedClonesMap,
                              final Map<String, String> pingCountMap,
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
                    if ("pingd".equals(name)) {
                        pingCountMap.put(uname, value);
                    } else if (name.indexOf(FAIL_COUNT_PREFIX) == 0) {
                        final String resId =
                                    name.substring(FAIL_COUNT_PREFIX.length());
                        failedMap.put(uname.toLowerCase(Locale.US),
                                      resId,
                                      value);
                        final Pattern p = Pattern.compile("(.*):(\\d+)$");
                        final Matcher m = p.matcher(resId);
                        if (m.matches()) {
                            final String crmId = m.group(1);
                            Set<String> clones = failedClonesMap.get(crmId);
                            if (clones == null) {
                                clones = new LinkedHashSet<String>();
                                failedClonesMap.put(crmId, clones);
                            }
                            clones.add(m.group(2));
                            failedMap.put(uname.toLowerCase(Locale.US),
                                          crmId,
                                          value);
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
            final String pcmkV = host.getPacemakerVersion();
            if (pcmkV == null
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
                    nodeParametersMap.put(node.toLowerCase(Locale.US),
                                          name,
                                          value);
                }
            }
        }
    }

    /** Parses resource sets. */
    private void parseRscSets(
                        final Node node,
                        final String colId,
                        final String ordId,
                        final List<RscSet> rscSets,
                        final List<RscSetConnectionData> rscSetConnections) {
        final NodeList nodes = node.getChildNodes();
        RscSet prevRscSet = null;
        int rscSetCount = 0;
        int ordPos = 0;
        int colPos = 0;
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node rscSetNode = nodes.item(i);
            if (rscSetNode.getNodeName().equals("resource_set")) {
                final String id = getAttribute(rscSetNode, "id");
                final String sequential = getAttribute(rscSetNode,
                                                       "sequential");
                final String orderAction = getAttribute(rscSetNode, "action");
                final String colocationRole = getAttribute(rscSetNode, "role");
                final NodeList rscNodes = rscSetNode.getChildNodes();
                final List<String> rscIds = new ArrayList<String>();
                for (int j = 0; j < rscNodes.getLength(); j++) {
                    final Node rscRefNode = rscNodes.item(j);
                    if (rscRefNode.getNodeName().equals("resource_ref")) {
                        final String rscId = getAttribute(rscRefNode, "id");
                        rscIds.add(rscId);
                    }
                }
                final RscSet rscSet = new RscSet(id,
                                                 rscIds,
                                                 sequential,
                                                 orderAction,
                                                 colocationRole);
                rscSets.add(rscSet);
                if (prevRscSet != null) {
                    RscSetConnectionData rscSetConnectionData;
                    if (colId == null) {
                        /* order */
                        rscSetConnectionData =
                                    new RscSetConnectionData(prevRscSet,
                                                             rscSet,
                                                             ordId,
                                                             ordPos,
                                                             false);
                        ordPos++;
                    } else {
                        /* colocation */
                        rscSetConnectionData =
                                    new RscSetConnectionData(rscSet,
                                                             prevRscSet,
                                                             colId,
                                                             colPos,
                                                             true);
                        colPos++;
                    }
                    rscSetConnections.add(rscSetConnectionData);
                }
                prevRscSet = rscSet;
                rscSetCount++;
            }
        }
        if (rscSetCount == 1) {
            /* just one, dangling */
            RscSetConnectionData rscSetConnectionData;
            if (colId == null) {
                /* order */
                rscSetConnectionData = new RscSetConnectionData(prevRscSet,
                                                                null,
                                                                ordId,
                                                                ordPos,
                                                                false);
            } else {
                /* colocation */
                rscSetConnectionData = new RscSetConnectionData(prevRscSet,
                                                                null,
                                                                colId,
                                                                colPos,
                                                                true);
            }
            rscSetConnections.add(rscSetConnectionData);
        }
    }

    /** Returns CibQuery object with information from the cib node. */
    public final CibQuery parseCibQuery(final String query) {
        final Document document = getXMLDocument(query);
        final CibQuery cibQueryData = new CibQuery();
        if (document == null) {
            Tools.appWarning("cib error: " + query);
            return cibQueryData;
        }
        /* get root <pacemaker> */
        final Node pcmkNode = getChildNode(document, "pcmk");
        if (pcmkNode == null) {
            Tools.appWarning("there is no pcmk node");
            return cibQueryData;
        }

        /* get fenced nodes */
        final Set<String> fencedNodes = new HashSet<String>();
        final Node fencedNode = getChildNode(pcmkNode, "fenced");
        if (fencedNode != null) {
            final NodeList nodes = fencedNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node hostNode = nodes.item(i);
                if (hostNode.getNodeName().equals("node")) {
                    final String host = getText(hostNode);
                    if (host != null) {
                        fencedNodes.add(host.toLowerCase(Locale.US));
                    }
                }
            }
        }

        /* get <cib> */
        final Node cibNode = getChildNode(pcmkNode, "cib");
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

        /* <rsc_defaults> */
        final Node rscDefaultsNode = getChildNode(confNode, "rsc_defaults");
        String rscDefaultsId = null;
        final Map<String, String> rscDefaultsParams =
                                                new HashMap<String, String>();
        final Map<String, String> rscDefaultsParamsNvpairIds =
                                                new HashMap<String, String>();
        if (rscDefaultsNode != null) {
            rscDefaultsId = parseRscDefaults(rscDefaultsNode,
                                             rscDefaultsParams,
                                             rscDefaultsParamsNvpairIds);
        }

        /* <op_defaults> */
        final Node opDefaultsNode = getChildNode(confNode, "op_defaults");
        final Map<String, String> opDefaultsParams =
                                                new HashMap<String, String>();
        if (opDefaultsNode != null) {
            parseOpDefaults(opDefaultsNode, opDefaultsParams);
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
        final String pcmkV = host.getPacemakerVersion();
        if (pcmkV == null
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
        final Map<String, String> nodeOnline = new HashMap<String, String>();
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
                    if (!nodeOnline.containsKey(uname.toLowerCase(Locale.US))) {
                        nodeOnline.put(uname.toLowerCase(Locale.US), "no");
                    }
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
        final Map<String, Map<String, String>> parametersMap =
                                    new HashMap<String, Map<String, String>>();
        final Map<String, Map<String, String>> parametersNvpairsIdsMap =
                                    new HashMap<String, Map<String, String>>();
        final Map<String, ResourceAgent> resourceTypeMap =
                                      new HashMap<String, ResourceAgent>();
        final Set<String> orphanedList = new LinkedHashSet<String>();
        final Map<String, String> resourceInstanceAttrIdMap =
                                      new HashMap<String, String>();
        final MultiKeyMap operationsMap = new MultiKeyMap();
        final Map<String, String> metaAttrsIdMap =
                                                new HashMap<String, String>();
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
        final Map<String, Set<String>> failedClonesMap =
                                     new LinkedHashMap<String, Set<String>>();
        final Map<String, String> pingCountMap = new HashMap<String, String>();
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
                               metaAttrsIdMap,
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
                           metaAttrsIdMap,
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
                                metaAttrsIdMap,
                                operationsIdMap,
                                resOpIdsMap,
                                operationsIdRefs,
                                operationsIdtoCRMId,
                                metaAttrsIdRefs,
                                metaAttrsIdToCRMId,
                                false);
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
                                       metaAttrsIdMap,
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
                                   metaAttrsIdMap,
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
        final Map<String, ColocationData> colocationIdMap =
                                   new LinkedHashMap<String, ColocationData>();
        final Map<String, List<ColocationData>> colocationRscMap =
                                   new HashMap<String, List<ColocationData>>();
        final Map<String, OrderData> orderIdMap =
                                        new LinkedHashMap<String, OrderData>();
        final Map<String, List<RscSet>> orderIdRscSetsMap =
                                           new HashMap<String, List<RscSet>>();
        final Map<String, List<RscSet>> colocationIdRscSetsMap =
                                           new HashMap<String, List<RscSet>>();
        final List<RscSetConnectionData> rscSetConnections =
                                         new ArrayList<RscSetConnectionData>();
        final Map<String, List<OrderData>> orderRscMap =
                                        new HashMap<String, List<OrderData>>();
        final Map<String, Map<String, HostLocation>> locationMap =
                              new HashMap<String, Map<String, HostLocation>>();
        final Map<String, HostLocation> pingLocationMap =
                                           new HashMap<String, HostLocation>();
        final Map<String, List<String>> locationsIdMap =
                                           new HashMap<String, List<String>>();
        final MultiKeyMap resHostToLocIdMap = new MultiKeyMap();

        final Map<String, String> resPingToLocIdMap =
                                               new HashMap<String, String>();
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
                rscRoleString     = "from_role";
                withRscString     = "to";
                withRscRoleString = "to_role";
                firstString       = "to";
                thenString        = "from";
                firstActionString = "to_action";
                thenActionString  = "action";
            }
            for (int i = 0; i < constraints.getLength(); i++) {
                final Node constraintNode = constraints.item(i);
                if (constraintNode.getNodeName().equals("rsc_colocation")) {
                    final String colId = getAttribute(constraintNode, "id");
                    final String rsc = getAttribute(constraintNode, rscString);
                    final String withRsc = getAttribute(constraintNode,
                                                        withRscString);
                    if (rsc == null || withRsc == null) {
                        final List<RscSet> rscSets = new ArrayList<RscSet>();
                        parseRscSets(constraintNode,
                                     colId,
                                     null,
                                     rscSets,
                                     rscSetConnections);
                        colocationIdRscSetsMap.put(colId, rscSets);
                    }
                    final String rscRole = getAttribute(constraintNode,
                                                            rscRoleString);
                    final String withRscRole = getAttribute(constraintNode,
                                                            withRscRoleString);
                    final String score = getAttribute(constraintNode,
                                                      SCORE_STRING);
                    final ColocationData colocationData =
                                               new ColocationData(colId,
                                                                  rsc,
                                                                  withRsc,
                                                                  rscRole,
                                                                  withRscRole,
                                                                  score);
                    colocationIdMap.put(colId, colocationData);
                    List<ColocationData> withs = colocationRscMap.get(rsc);
                    if (withs == null) {
                        withs = new ArrayList<ColocationData>();
                    }
                    withs.add(colocationData);
                    colocationRscMap.put(rsc, withs);
                } else if (constraintNode.getNodeName().equals("rsc_order")) {
                    String rscFirst = getAttribute(constraintNode,
                                                   firstString);
                    String rscThen = getAttribute(constraintNode,
                                                  thenString);
                    final String ordId = getAttribute(constraintNode, "id");
                    if (rscFirst == null || rscThen == null) {
                        final List<RscSet> rscSets = new ArrayList<RscSet>();
                        parseRscSets(constraintNode,
                                     null,
                                     ordId,
                                     rscSets,
                                     rscSetConnections);
                        orderIdRscSetsMap.put(ordId, rscSets);
                    }
                    final String score = getAttribute(constraintNode,
                                                      SCORE_STRING);
                    final String symmetrical = getAttribute(constraintNode,
                                                            "symmetrical");
                    String firstAction = getAttribute(constraintNode,
                                                      firstActionString);
                    String thenAction = getAttribute(constraintNode,
                                                     thenActionString);
                    final String type = getAttribute(constraintNode,
                                                     "type");
                    if (type != null && "before".equals(type)) {
                        /* exchange resoruces */
                        final String rsc = rscFirst;
                        rscFirst = rscThen;
                        rscThen = rsc;
                        final String act = firstAction;
                        firstAction = thenAction;
                        thenAction = act;
                    }
                    final OrderData orderData = new OrderData(ordId,
                                                              rscFirst,
                                                              rscThen,
                                                              score,
                                                              symmetrical,
                                                              firstAction,
                                                              thenAction);
                    orderIdMap.put(ordId, orderData);
                    List<OrderData> thens = orderRscMap.get(rscFirst);
                    if (thens == null) {
                        thens = new ArrayList<OrderData>();
                    }
                    thens.add(orderData);
                    orderRscMap.put(rscFirst, thens);
                } else if ("rsc_location".equals(
                                              constraintNode.getNodeName())) {
                    final String locId = getAttribute(constraintNode, "id");
                    final String node  = getAttribute(constraintNode, "node");
                    final String rsc   = getAttribute(constraintNode, "rsc");
                    final String score = getAttribute(constraintNode,
                                                      SCORE_STRING);
                    final String role = null; // TODO

                    List<String> locs = locationsIdMap.get(rsc);
                    if (locs == null) {
                        locs = new ArrayList<String>();
                        locationsIdMap.put(rsc, locs);
                    }
                    Map<String, HostLocation> hostScoreMap =
                                                          locationMap.get(rsc);
                    if (hostScoreMap == null) {
                        hostScoreMap = new HashMap<String, HostLocation>();
                        locationMap.put(rsc, hostScoreMap);
                    }
                    if (node != null) {
                        resHostToLocIdMap.put(rsc,
                                              node.toLowerCase(Locale.US),
                                              locId);
                    }
                    if (score != null) {
                        hostScoreMap.put(node.toLowerCase(Locale.US),
                                         new HostLocation(score,
                                                          "eq",
                                                          null,
                                                          role));
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
                            final String value =
                                     getAttribute(expNode, "value");
                            if ((booleanOp == null
                                 || "and".equals(booleanOp))
                                && "#uname".equals(attr)) {
                                hostScoreMap.put(value.toLowerCase(Locale.US),
                                                 new HostLocation(score2,
                                                                  op,
                                                                  null,
                                                                  role));
                                resHostToLocIdMap.put(
                                                  rsc,
                                                  value.toLowerCase(Locale.US),
                                                  locId);
                            } else if ((booleanOp == null
                                        || "and".equals(booleanOp))
                                       && "pingd".equals(attr)) {
                                pingLocationMap.put(rsc,
                                                    new HostLocation(score2,
                                                                     op,
                                                                     value,
                                                                     null));
                                resPingToLocIdMap.put(rsc, locId);
                            } else {
                                Tools.appWarning(
                                    "could not parse rsc_location: " + locId);
                            }
                        }
                    }
                }
            }
        }

        /* <status> */
        final Node statusNode = getChildNode(cibNode, "status");
        final Set<String> nodePending = new HashSet<String>();
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
                    final String crmd = getAttribute(nodeStateNode, "crmd");
                    if ("active".equals(ha)
                        && "member".equals(join)
                        && "true".equals(inCCM)
                        && !"offline".equals(crmd)) {
                        nodeOnline.put(uname.toLowerCase(Locale.US), "yes");
                    } else {
                        nodeOnline.put(uname.toLowerCase(Locale.US), "no");
                    }
                    if ("pending".equals(join)) {
                        nodePending.add(uname.toLowerCase(Locale.US));
                    }
                    final NodeList nodeStates = nodeStateNode.getChildNodes();
                    /* transient attributes. */
                    for (int j = 0; j < nodeStates.getLength(); j++) {
                        final Node nodeStateChild = nodeStates.item(j);
                        if ("transient_attributes".equals(
                                               nodeStateChild.getNodeName())) {
                            parseTransientAttributes(uname,
                                                     nodeStateChild,
                                                     failedMap,
                                                     failedClonesMap,
                                                     pingCountMap,
                                                     hbV);
                        }
                    }
                    final List<String> resList =
                                  groupsToResourcesMap.get("none");
                    for (int j = 0; j < nodeStates.getLength(); j++) {
                        final Node nodeStateChild = nodeStates.item(j);
                        if ("lrm".equals(nodeStateChild.getNodeName())) {
                            parseLRM(nodeStateChild,
                                     resList,
                                     resourceTypeMap,
                                     parametersMap,
                                     orphanedList,
                                     failedClonesMap);
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
        cibQueryData.setOrphaned(orphanedList);
        cibQueryData.setResourceInstanceAttrId(resourceInstanceAttrIdMap);

        cibQueryData.setColocationRsc(colocationRscMap);
        cibQueryData.setColocationId(colocationIdMap);

        cibQueryData.setOrderId(orderIdMap);
        cibQueryData.setOrderIdRscSets(orderIdRscSetsMap);
        cibQueryData.setColocationIdRscSets(colocationIdRscSetsMap);
        cibQueryData.setRscSetConnections(rscSetConnections);
        cibQueryData.setOrderRsc(orderRscMap);

        cibQueryData.setLocation(locationMap);
        cibQueryData.setPingLocation(pingLocationMap);
        cibQueryData.setLocationsId(locationsIdMap);
        cibQueryData.setResHostToLocId(resHostToLocIdMap);
        cibQueryData.setResPingToLocId(resPingToLocIdMap);
        cibQueryData.setOperations(operationsMap);
        cibQueryData.setOperationsId(operationsIdMap);
        cibQueryData.setOperationsRefs(operationsRefs);
        cibQueryData.setMetaAttrsId(metaAttrsIdMap);
        cibQueryData.setMetaAttrsRefs(metaAttrsRefs);
        cibQueryData.setResOpIds(resOpIdsMap);
        cibQueryData.setNodeOnline(nodeOnline);
        cibQueryData.setNodePending(nodePending);
        cibQueryData.setGroupsToResources(groupsToResourcesMap);
        cibQueryData.setCloneToResource(cloneToResourceMap);
        cibQueryData.setMasterList(masterList);
        cibQueryData.setFailed(failedMap);
        cibQueryData.setFailedClones(failedClonesMap);
        cibQueryData.setPingCount(pingCountMap);
        cibQueryData.setRscDefaultsId(rscDefaultsId);
        cibQueryData.setRscDefaultsParams(rscDefaultsParams);
        cibQueryData.setRscDefaultsParamsNvpairIds(rscDefaultsParamsNvpairIds);
        cibQueryData.setOpDefaultsParams(opDefaultsParams);
        cibQueryData.setFencedNodes(fencedNodes);
        return cibQueryData;
    }

    /** Returns order parameters. */
    public final String[] getOrderParameters() {
        if (ordParams != null) {
            return ordParams.toArray(new String[ordParams.size()]);
        }
        return null;
    }

    /** Returns order parameters for resource sets. */
    public final String[] getRscSetOrderParameters() {
        if (rscSetOrdParams != null) {
            return rscSetOrdParams.toArray(new String[rscSetOrdParams.size()]);
        }
        return null;
    }

    /** Returns order parameters for resource sets. (Shown when an edge
     * is clicked, resource_set tag). */
    public final String[] getRscSetOrdConnectionParameters() {
        if (rscSetOrdConnectionParams != null) {
            return rscSetOrdConnectionParams.toArray(
                                new String[rscSetOrdConnectionParams.size()]);
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

    /** Returns whether the order parameter expects an integer value. */
    public final boolean isOrderInteger(final String param) {
        final String type = getOrderParamType(param);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    /** Returns whether the order parameter expects a label value. */
    public final boolean isOrderLabel(final String param) {
        final String type = getOrderParamType(param);
        return PARAM_TYPE_LABEL.equals(type);
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
                && !PCMK_TRUE.equals(value)
                && !PCMK_FALSE.equals(value)
                && !"True".equals(value)
                && !"False".equals(value)) {

                correctValue = false;
            }
        } else if (PARAM_TYPE_INTEGER.equals(type)) {
            final Pattern p =
                 Pattern.compile("^(-?\\d*|(-|\\+)?" + INFINITY_STRING + ")$");
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

    /** Returns colocation parameters for resource sets. (Shown when a
     * placeholder is clicked, rsc_colocation tag). */
    public final String[] getRscSetColocationParameters() {
        if (rscSetColParams != null) {
            return rscSetColParams.toArray(new String[rscSetColParams.size()]);
        }
        return null;
    }

    /** Returns colocation parameters for resource sets. (Shown when an edge
     * is clicked, resource_set tag). */
    public final String[] getRscSetColConnectionParameters() {
        if (rscSetColConnectionParams != null) {
            return rscSetColConnectionParams.toArray(
                                new String[rscSetColConnectionParams.size()]);
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

    /** Returns whether the colocation parameter expects an integer value. */
    public final boolean isColocationInteger(final String param) {
        final String type = getColocationParamType(param);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    /** Returns whether the colocation parameter expects a label value. */
    public final boolean isColocationLabel(final String param) {
        final String type = getColocationParamType(param);
        return PARAM_TYPE_LABEL.equals(type);
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
                && !PCMK_TRUE.equals(value)
                && !PCMK_FALSE.equals(value)
                && !"True".equals(value)
                && !"False".equals(value)) {

                correctValue = false;
            }
        } else if (PARAM_TYPE_INTEGER.equals(type)) {
            final Pattern p =
                 Pattern.compile("^(-?\\d*|(-|\\+)?" + INFINITY_STRING + ")$");
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

    /* Get resources that were removed but are in LRM. */
    public final void parseLRM(
                       final Node lrmNode,
                       final List<String> resList,
                       final Map<String, ResourceAgent> resourceTypeMap,
                       final Map<String, Map<String, String>> parametersMap,
                       final Set<String> orphanedList,
                       final Map<String, Set<String>> failedClonesMap) {
        final Node lrmResourcesNode = getChildNode(lrmNode, "lrm_resources");
        final NodeList lrmResources = lrmResourcesNode.getChildNodes();
        for (int j = 0; j < lrmResources.getLength(); j++) {
            final Node rscNode = lrmResources.item(j);
            if ("lrm_resource".equals(rscNode.getNodeName())) {
                final String resId = getAttribute(rscNode, "id");
                final Pattern p = Pattern.compile("(.*):(\\d+)$");
                final Matcher m = p.matcher(resId);
                String crmId;
                if (m.matches()) {
                    crmId = m.group(1);
                    Set<String> clones = failedClonesMap.get(crmId);
                    if (clones == null) {
                        clones = new LinkedHashSet<String>();
                        failedClonesMap.put(crmId, clones);
                    }
                    clones.add(m.group(2));
                } else {
                    crmId = resId;
                }
                if (Tools.getConfigData().isAdvancedMode()
                    && !resourceTypeMap.containsKey(crmId)) {
                    /* it is orphaned */
                    final String raClass = getAttribute(rscNode, "class");
                    String provider = getAttribute(rscNode, "provider");
                    if (provider == null) {
                        provider = "heartbeat";
                    }
                    final String type = getAttribute(rscNode, "type");
                    orphanedList.add(crmId);
                    resourceTypeMap.put(crmId, getResourceAgent(type,
                                                                provider,
                                                                raClass));
                    resList.add(crmId);
                    parametersMap.put(crmId, new HashMap<String, String>());
                }
            }
        }
    }

    /** Class that holds colocation data. */
    public class ColocationData {
        /** Colocation id. */
        private final String id;
        /** Colocation resource 1. */
        private final String rsc;
        /** Colocation resource 2. */
        private final String withRsc;
        /** Resource 1 role. */
        private final String rscRole;
        /** Resource 2 role. */
        private final String withRscRole;
        /** Colocation score. */
        private final String score;

        /** Creates new ColocationData object. */
        public ColocationData(final String id,
                              final String rsc,
                              final String withRsc,
                              final String rscRole,
                              final String withRscRole,
                              final String score) {
            this.id = id;
            this.rsc = rsc;
            this.withRsc = withRsc;
            this.rscRole = rscRole;
            this.withRscRole = withRscRole;
            this.score = score;
        }

        /** Returns colocation id. */
        public final String getId() {
            return id;
        }

        /** Returns colocation rsc. */
        public final String getRsc() {
            return rsc;
        }

        /** Returns colocation with-rsc. */
        public final String getWithRsc() {
            return withRsc;
        }

        /** Returns colocation rsc role. */
        public final String getRscRole() {
            return rscRole;
        }

        /** Returns colocation with-rsc role. */
        public final String getWithRscRole() {
            return withRscRole;
        }

        /** Returns colocation score. */
        public final String getScore() {
            return score;
        }
    }

    /** Class that holds order data. */
    public class OrderData {
        /** Order id. */
        private final String id;
        /** Order resource 1. */
        private final String rscFirst;
        /** Order resource 2. */
        private final String rscThen;
        /** Order score. */
        private final String score;
        /** Symmetical. */
        private final String symmetrical;
        /** Action of the first resource. */
        private final String firstAction;
        /** Action of the second resource. */
        private final String thenAction;

        /** Creates new OrderData object. */
        public OrderData(final String id,
                         final String rscFirst,
                         final String rscThen,
                         final String score,
                         final String symmetrical,
                         final String firstAction,
                         final String thenAction) {
            this.id = id;
            this.rscFirst = rscFirst;
            this.rscThen = rscThen;
            this.score = score;
            this.symmetrical = symmetrical;
            this.firstAction = firstAction;
            this.thenAction = thenAction;
        }

        /** Returns order id. */
        public final String getId() {
            return id;
        }

        /** Returns order first rsc. */
        public final String getRscFirst() {
            return rscFirst;
        }

        /** Returns order then rsc. */
        public final String getRscThen() {
            return rscThen;
        }

        /** Returns order score. */
        public final String getScore() {
            return score;
        }

        /** Returns order symmetrical attribute. */
        public final String getSymmetrical() {
            return symmetrical;
        }

        /** Returns order action for "first" resource. */
        public final String getFirstAction() {
            return firstAction;
        }

        /** Returns order action for "then" resource. */
        public final String getThenAction() {
            return thenAction;
        }
    }

    /** Class that holds resource set data. */
    public final class RscSet {
        /** Resource set id. */
        private final String id;
        /** Resources in this set. */
        private final List<String> rscIds;
        /** Resource ids lock. */
        private final Mutex mRscIdsLock = new Mutex();
        /** String whether the resource set is sequential or not. */
        private final String sequential;
        /** order action. */
        private final String orderAction;
        /** colocation role. */
        private final String colocationRole;

        /** Creates new RscSet object. */
        public RscSet(final String id,
                      final List<String> rscIds,
                      final String sequential,
                      final String orderAction,
                      final String colocationRole) {
            this.id = id;
            this.rscIds = rscIds;
            this.sequential = sequential;
            this.orderAction = orderAction;
            this.colocationRole = colocationRole;
        }

        /** Returns resource set id. */
        public String getId() {
            return id;
        }
        /** Returns resources in this set. */
        public List<String> getRscIds() {
            final List<String> copy = new ArrayList<String>();
            try {
                mRscIdsLock.acquire();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            for (final String id : rscIds) {
                copy.add(id);
            }
            mRscIdsLock.release();
            return copy;
        }

        /** Returns whether the resource set is sequential or not. */
        public String getSequential() {
            return sequential;
        }

        /** Returns whether this resource set is subset of the supplied
         * resource set. */
        public boolean isSubsetOf(final RscSet oRscSet) {
            if (oRscSet == null) {
                return false;
            }
            final List<String> oRscIds = oRscSet.getRscIds();
            try {
                mRscIdsLock.acquire();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            if (rscIds.isEmpty()) {
                mRscIdsLock.release();
                return false;
            }
            for (final String rscId : rscIds) {
                if (!oRscIds.contains(rscId)) {
                    mRscIdsLock.release();
                    return false;
                }
            }
            mRscIdsLock.release();
            return true;
        }

        /** Returns whether this resource set is equal to the supplied
         * resource set. The order of ids doesn't matter. */
        public boolean equals(final RscSet oRscSet) {
            if (oRscSet == null) {
                return false;
            }
            final List<String> oRscIds = oRscSet.getRscIds();
            try {
                mRscIdsLock.acquire();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            if (oRscIds.size() != rscIds.size()) {
                mRscIdsLock.release();
                return false;
            }
            for (final String rscId : rscIds) {
                if (!oRscIds.contains(rscId)) {
                    mRscIdsLock.release();
                    return false;
                }
            }
            mRscIdsLock.release();
            return true;
        }

        /** Removes one id from rsc ids. */
        public void removeRscId(final String id) {
            try {
                mRscIdsLock.acquire();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            rscIds.remove(id);
            mRscIdsLock.release();
        }

        /** Adds one id to rsc ids. */
        public void addRscId(final String id) {
            try {
                mRscIdsLock.acquire();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            rscIds.add(id);
            mRscIdsLock.release();
        }

        /** Return whether rsc ids are empty. */
        public boolean isRscIdsEmpty() {
            try {
                mRscIdsLock.acquire();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            final boolean empty = rscIds.isEmpty();
            mRscIdsLock.release();
            return empty;
        }

        /** String represantation of the resources set. */
        public String toString() {
            final StringBuffer s = new StringBuffer(20);
            s.append("rscset id: ");
            s.append(id);
            s.append(" ids: ");
            s.append(rscIds);
            return s.toString();
        }

        /** Returns order action. */
        public String getOrderAction() {
            return orderAction;
        }

        /** Returns colocation role. */
        public String getColocationRole() {
            return colocationRole;
        }

        /** Returns whether the resouce set is sequential. */
        public boolean isSequential() {
            return sequential == null || "true".equals(sequential);
        }
    }

    /** Class that holds data between two resource sests. */
    public final class RscSetConnectionData {
        /** Resource set 1. */
        private RscSet rscSet1;
        /** Resource set 2. */
        private RscSet rscSet2;
        /** Colocation id. */
        private String constraintId;
        /** Position in the resoruce set. */
        private final int connectionPos;
        /** Whether it is colocation. */
        private final boolean colocation;

        /** Creates new RscSetConnectionData object. */
        public RscSetConnectionData(final RscSet rscSet1,
                                    final RscSet rscSet2,
                                    final String constraintId,
                                    final int connectionPos,
                                    final boolean colocation) {
            this.rscSet1 = rscSet1;
            this.rscSet2 = rscSet2;
            this.constraintId = constraintId;
            this.connectionPos = connectionPos;
            this.colocation = colocation;
        }

        /** Returns resource set 1. */
        public RscSet getRscSet1() {
            return rscSet1;
        }

        /** Returns resource set 2. */
        public RscSet getRscSet2() {
            return rscSet2;
        }

        /** Returns order or constraint id. */
        public String getConstraintId() {
            return constraintId;
        }

        /** Returns order or constraint id. */
        public void setConstraintId(final String constraintId) {
            this.constraintId = constraintId;
        }

        /** Returns whether it is colocation. */
        public boolean isColocation() {
            return colocation;
        }

        /** Returns whether two resource sets are equal. */
        private boolean rscSetsAreEqual(final RscSet set1,
                                              final RscSet set2) {
            if (set1 == set2) {
                return true;
            }
            if (set1 == null || set2 == null) {
                return false;
            }
            return set1.equals(set2);
        }

        /** Whether the two resource sets are equal. */
        public boolean equals(final RscSetConnectionData oRdata) {
            final RscSet oRscSet1 = oRdata.getRscSet1();
            final RscSet oRscSet2 = oRdata.getRscSet2();
            return oRdata.isColocation() == colocation
                   && rscSetsAreEqual(rscSet1, oRscSet1)
                   && rscSetsAreEqual(rscSet2, oRscSet2);
        }

        /** Whether the two resource sets are equal,
            even if they are reversed. */
        public boolean equalsReversed(final RscSetConnectionData oRdata) {
            final RscSet oRscSet1 = oRdata.getRscSet1();
            final RscSet oRscSet2 = oRdata.getRscSet2();
            return oRdata.isColocation() == colocation
                   && ((rscSet1 == null /* when it's reversed. */
                           && oRscSet2 == null
                           && rscSetsAreEqual(rscSet2, oRscSet1))
                        || (rscSet2 == null
                            && oRscSet1 == null
                            && rscSetsAreEqual(rscSet1, oRscSet2)));
        }

        /** Returns whether the same palceholder should be used. */
        public boolean samePlaceholder(final RscSetConnectionData oRdata) {
            if (oRdata.isColocation() == colocation) {
                /* exactly the same */
                return equals(oRdata);
            }
            final RscSet oRscSet1 = oRdata.getRscSet1();
            final RscSet oRscSet2 = oRdata.getRscSet2();
            /* is subset only if both are zero */
            if ((rscSet1 == oRscSet1
                 || rscSet1 == null
                 || oRscSet1 == null
                 || rscSet1.isSubsetOf(oRscSet1)
                 || oRscSet1.isSubsetOf(rscSet1))
                && (rscSet2 == oRscSet2
                    || rscSet2 == null
                    || oRscSet2 == null
                    || rscSet2.isSubsetOf(oRscSet2)
                    || oRscSet2.isSubsetOf(rscSet2))) {
                 /* at least one subset without rscset being null. */
                if ((rscSet1 != null && rscSet1.isSubsetOf(oRscSet1))
                    || (oRscSet1 != null && oRscSet1.isSubsetOf(rscSet1))
                    || (rscSet2 != null && rscSet2.isSubsetOf(oRscSet2))
                    || (oRscSet2 != null && oRscSet2.isSubsetOf(rscSet2))) {
                    return true;
                }
            }
            if ((rscSet1 == oRscSet2
                 || rscSet1 == null
                 || oRscSet2 == null
                 || rscSet1.isSubsetOf(oRscSet2)
                 || oRscSet2.isSubsetOf(rscSet1))
                && (rscSet2 == oRscSet1
                    || rscSet2 == null
                    || oRscSet1 == null
                    || rscSet2.isSubsetOf(oRscSet1)
                    || oRscSet1.isSubsetOf(rscSet2))) {

                if ((rscSet1 != null && rscSet1.isSubsetOf(oRscSet2))
                    || (oRscSet2 != null && oRscSet2.isSubsetOf(rscSet1))
                    || (rscSet2 != null && rscSet2.isSubsetOf(oRscSet1))
                    || (oRscSet1 != null && oRscSet1.isSubsetOf(rscSet2))) {
                    return true;
                }
            }
            return false;
        }

        /** Reverse resource sets. */
        public void reverse() {
            final RscSet old1 = rscSet1;
            rscSet1 = rscSet2;
            rscSet2 = old1;
        }

        /** Returns whether it is an empty connection. */
        public boolean isEmpty() {
            if ((rscSet1 == null || rscSet1.isRscIdsEmpty())
                && (rscSet2 == null || rscSet2.isRscIdsEmpty())) {
                return true;
            }
            return false;
        }

        /** Returns connection position. */
        public int getConnectionPos() {
            return connectionPos;
        }

        /** String represantation of the resource set data. */
        public String toString() {
            final StringBuffer s = new StringBuffer(100);
            s.append("rsc set conn id: ");
            s.append(constraintId);
            if (colocation) {
                s.append(" (colocation)");
            } else {
                s.append(" (order)");
            }
            s.append("\n   (rscset1: ");
            if (rscSet1 == null) {
                s.append("null");
            } else {
                s.append(rscSet1.toString());
            }
            s.append(") \n   (rscset2: ");
            if (rscSet2 == null) {
                s.append("null");
            } else {
                s.append(rscSet2.toString());
            }
            s.append(") ");
            return s.toString();
        }
    }


}
