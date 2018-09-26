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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import lcmc.Exceptions;
import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.SshOutput;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.network.InfoPresenter;
import lcmc.common.domain.*;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.crm.ui.resource.ServiceInfo;
import lcmc.crm.ui.resource.ServicesInfo;
import lcmc.crm.ui.resource.update.ResourceUpdater;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.robotest.StartTests;
import lcmc.robotest.Test;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections15.map.MultiKeyMap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class parses ocf crm xml, stores information like
 * short and long description, data types etc. for defined types
 * of services in the hashes and provides methods to get this
 * information.
 */
@RequiredArgsConstructor
public class CrmXml {
    private final ProgressIndicator progressIndicator;
    private final Application application;
    private final StartTests startTests;
    private final Supplier<ResourceUpdater> resourceUpdaterProvider;

    private static final Logger LOG = LoggerFactory.getLogger(CrmXml.class);
    private static final Table<String, String, String> RA_NON_ADVANCED_PARAM = HashBasedTable.create();
    static final Value PCMK_TRUE_VALUE = new StringValue("true");
    static final Value PCMK_FALSE_VALUE = new StringValue("false");

    private static final Value PCMK_TRUE_2 = new StringValue("True");
    private static final Value PCMK_FALSE_2 = new StringValue("False");

    private static final Value PCMK_YES = new StringValue("yes");
    private static final Value PCMK_NO = new StringValue("no");

    public static final Value DISABLED_IN_COMBOBOX = new StringValue("disabled");
    private static final String PARAM_TYPE_BOOLEAN = "boolean";
    private static final String PARAM_TYPE_INTEGER = "integer";
    private static final String PARAM_TYPE_LABEL = "label";
    private static final String PARAM_TYPE_STRING = "string";
    private static final String PARAM_TYPE_TIME = "time";
    private static final String FAIL_COUNT_PREFIX = "fail-count-";
    private static final Value[] ATTRIBUTE_ROLES = {new StringValue(),
                                                    new StringValue("Stopped"),
                                                    new StringValue("Started")};

    private static final Value[] ATTRIBUTE_ROLES_MASTER_SLAVE = {new StringValue(),
                                                                 new StringValue("Stopped"),
                                                                 new StringValue("Started"),
                                                                 new StringValue("Master"),
                                                                 new StringValue("Slave")};

    private static final Value[] ATTRIBUTE_ACTIONS = {new StringValue(),
                                                      new StringValue( "start"),
                                                      new StringValue( "stop")};

    private static final Value[] ATTRIBUTE_ACTIONS_MASTER_SLAVE = {new StringValue(),
                                                                   new StringValue("start"),
                                                                   new StringValue("promote"),
                                                                   new StringValue("demote"),
                                                                   new StringValue("stop")};
    public static final String TARGET_ROLE_STOPPED = "stopped";
    private static final String TARGET_ROLE_STARTED = "started";
    private static final String TARGET_ROLE_MASTER = "master";
    public static final String TARGET_ROLE_SLAVE = "slave";
    public static final Value INFINITY_VALUE = new StringValue("INFINITY");
    public static final Value PLUS_INFINITY_VALUE = new StringValue("+INFINITY");
    public static final Value MINUS_INFINITY_VALUE = new StringValue("-INFINITY");
    private static final Value[] INTEGER_VALUES = {new StringValue(),
                                                   new StringValue("0"),
                                                   new StringValue("2"),
                                                   new StringValue("100"),
                                                   INFINITY_VALUE,
                                                   MINUS_INFINITY_VALUE,
                                                   PLUS_INFINITY_VALUE};
    private static final String STONITH_TIMEOUT_INSTANCE_ATTR = "stonith-timeout";
    /** Name of the stonith priority instance attribute.
        It is actually only "priority" but it clashes with priority meta
        attribute. It is converted, wenn it is parsed and when it is stored to
        cib. */
    public static final String STONITH_PRIORITY_INSTANCE_ATTR = "stonith-priority";
    public static final String SCORE_CONSTRAINT_PARAM = "score";
    private static final String PRIORITY_META_ATTR = "priority";
    private static final String RESOURCE_STICKINESS_META_ATTR = "resource-stickiness";
    private static final String MIGRATION_THRESHOLD_META_ATTR = "migration-threshold";
    private static final String FAILURE_TIMEOUT_META_ATTR = "failure-timeout";
    private static final String MULTIPLE_ACTIVE_META_ATTR = "multiple-active";
    private static final String TARGET_ROLE_META_ATTR = "target-role";
    private static final String IS_MANAGED_META_ATTR = "is-managed";
    private static final String ALLOW_MIGRATE_META_ATTR = "allow-migrate";
    private static final String MASTER_MAX_META_ATTR = "master-max";
    private static final String MASTER_NODE_MAX_META_ATTR = "master-node-max";
    private static final String CLONE_MAX_META_ATTR = "clone-max";
    private static final String CLONE_NODE_MAX_META_ATTR = "clone-node-max";
    private static final String NOTIFY_META_ATTR = "notify";
    private static final String GLOBALLY_UNIQUE_META_ATTR = "globally-unique";
    private static final String ORDERED_META_ATTR = "ordered";
    private static final String INTERLEAVE_META_ATTR = "interleave";
    /** It has different default than clone "ordered". */
    public static final String GROUP_ORDERED_META_ATTR = "group-ordered";
    private static final String GROUP_COLLOCATED_META_ATTR = "collocated";
    public static final Value REQUIRE_ALL_TRUE_VALUE = PCMK_TRUE_VALUE;
    public static final Value REQUIRE_ALL_FALSE_VALUE = PCMK_FALSE_VALUE;
    public static final String REQUIRE_ALL_ATTR = "require-all";

    private static final Map<String, String> RSC_DEFAULTS_META_ATTR_SECTION = new HashMap<String, String>();
    private static final Collection<String> META_ATTR_NOT_ADVANCED = new ArrayList<String>();
    private static final Collection<String> GROUP_META_ATTR_NOT_ADVANCED = new ArrayList<String>();
    private static final Map<String, AccessMode.Type> META_ATTR_ACCESS_TYPE =
                                                                  new HashMap<String, AccessMode.Type>();
    private static final Map<String, AccessMode.Type> RSC_DEFAULTS_META_ATTR_ACCESS_TYPE =
                                                                        new HashMap<String, AccessMode.Type>();
    private static final Map<String, Value[]> META_ATTR_COMBO_BOX_CHOICES = new HashMap<String, Value[]>();
    private static final Map<String, Value[]> META_ATTR_POSSIBLE_CHOICES_MASTER_SLAVE = new HashMap<String, Value[]>();
    private static final Map<String, String> META_ATTR_SHORT_DESC = new HashMap<String, String>();
    private static final Map<String, String> META_ATTR_LONG_DESC = new HashMap<String, String>();
    private static final Map<String, Value> META_ATTR_DEFAULT = new HashMap<String, Value>();
    private static final Map<String, String> META_ATTR_TYPE = new HashMap<String, String>();
    private static final Map<String, Value> META_ATTR_PREFERRED = new HashMap<String, Value>();
    private static final Value[] PCMK_BOOLEAN_VALUES = {PCMK_TRUE_VALUE, PCMK_FALSE_VALUE};
    private static final Collection<String> IGNORE_RA_DEFAULTS_FOR = new ArrayList<String>();

    private static final String STONITH_PARAM_PCMK_HOST_CHECK = "pcmk_host_check";
    private static final String STONITH_PARAM_PCMK_HOST_LIST = "pcmk_host_list";
    private static final String STONITH_PARAM_PCMK_HOST_MAP = "pcmk_host_map";

    private static final String FENCING_ACTION_PARAM = "action";

    /** TODO: If this is set PCMK_HOST_LIST must be set. */
    private static final String PCMK_HOST_CHECK_STATIC = "static-list";
    /** TODO: If this is set PCMK_HOST_LIST must not be set. */
    private static final String PCMK_HOST_CHECK_DYNAMIC = "dynamic-list";
    public static final String PARAM_OCF_CHECK_LEVEL = "OCF_CHECK_LEVEL";

    private static final Pattern UNIT_PATTERN = Pattern.compile("^(\\d+)(\\D*)$");

    static {
        /* target-role */
        META_ATTR_COMBO_BOX_CHOICES.put(TARGET_ROLE_META_ATTR, new Value[]{new StringValue(),
                new StringValue(TARGET_ROLE_STARTED),
                new StringValue(TARGET_ROLE_STOPPED)});
        META_ATTR_POSSIBLE_CHOICES_MASTER_SLAVE.put(TARGET_ROLE_META_ATTR,
                                                    new Value[]{new StringValue(),
                                                                new StringValue(TARGET_ROLE_MASTER),
                                                                new StringValue(TARGET_ROLE_STARTED),
                                                                new StringValue(TARGET_ROLE_SLAVE),
                                                                new StringValue(TARGET_ROLE_STOPPED)});
        META_ATTR_SHORT_DESC.put(TARGET_ROLE_META_ATTR, Tools.getString("CRMXML.TargetRole.ShortDesc"));
        META_ATTR_LONG_DESC.put(TARGET_ROLE_META_ATTR, Tools.getString("CRMXML.TargetRole.LongDesc"));
        META_ATTR_DEFAULT.put(TARGET_ROLE_META_ATTR, null);
        META_ATTR_NOT_ADVANCED.add(TARGET_ROLE_META_ATTR);
        GROUP_META_ATTR_NOT_ADVANCED.add(TARGET_ROLE_META_ATTR);

        /* is-managed */
        META_ATTR_COMBO_BOX_CHOICES.put(IS_MANAGED_META_ATTR, PCMK_BOOLEAN_VALUES);
        META_ATTR_SHORT_DESC.put(IS_MANAGED_META_ATTR, Tools.getString("CRMXML.IsManaged.ShortDesc"));
        META_ATTR_LONG_DESC.put(IS_MANAGED_META_ATTR, Tools.getString("CRMXML.IsManaged.LongDesc"));
        META_ATTR_DEFAULT.put(IS_MANAGED_META_ATTR, PCMK_TRUE_VALUE);
        META_ATTR_TYPE.put(IS_MANAGED_META_ATTR, PARAM_TYPE_BOOLEAN);
        META_ATTR_NOT_ADVANCED.add(IS_MANAGED_META_ATTR);

        /* allow-migrate */
        META_ATTR_COMBO_BOX_CHOICES.put(ALLOW_MIGRATE_META_ATTR, PCMK_BOOLEAN_VALUES);
        META_ATTR_SHORT_DESC.put(ALLOW_MIGRATE_META_ATTR, Tools.getString("CRMXML.AllowMigrate.ShortDesc"));
        META_ATTR_LONG_DESC.put(ALLOW_MIGRATE_META_ATTR, Tools.getString("CRMXML.AllowMigrate.LongDesc"));
        META_ATTR_DEFAULT.put(ALLOW_MIGRATE_META_ATTR, PCMK_FALSE_VALUE);
        META_ATTR_TYPE.put(ALLOW_MIGRATE_META_ATTR, PARAM_TYPE_BOOLEAN);

        /* priority */
        META_ATTR_COMBO_BOX_CHOICES.put(PRIORITY_META_ATTR, new Value[]{new StringValue("0"),
                new StringValue("5"),
                new StringValue("10")});
        META_ATTR_SHORT_DESC.put(PRIORITY_META_ATTR, Tools.getString("CRMXML.Priority.ShortDesc"));
        META_ATTR_LONG_DESC.put(PRIORITY_META_ATTR, Tools.getString("CRMXML.Priority.LongDesc"));
        META_ATTR_DEFAULT.put(PRIORITY_META_ATTR, new StringValue("0"));
        META_ATTR_TYPE.put(PRIORITY_META_ATTR, PARAM_TYPE_INTEGER);

        /* resource-stickiness since 2.1.4 */
        META_ATTR_COMBO_BOX_CHOICES.put(RESOURCE_STICKINESS_META_ATTR, INTEGER_VALUES);
        META_ATTR_SHORT_DESC.put(RESOURCE_STICKINESS_META_ATTR, Tools.getString("CRMXML.ResourceStickiness.ShortDesc"));
        META_ATTR_LONG_DESC.put(RESOURCE_STICKINESS_META_ATTR, Tools.getString("CRMXML.ResourceStickiness.LongDesc"));
        META_ATTR_DEFAULT.put(RESOURCE_STICKINESS_META_ATTR, new StringValue("0"));
        META_ATTR_TYPE.put(RESOURCE_STICKINESS_META_ATTR, PARAM_TYPE_INTEGER);
        META_ATTR_NOT_ADVANCED.add(RESOURCE_STICKINESS_META_ATTR);

        /* migration-threshold */
        META_ATTR_COMBO_BOX_CHOICES.put(MIGRATION_THRESHOLD_META_ATTR,
                new Value[]{DISABLED_IN_COMBOBOX,
                        new StringValue("0"),
                        new StringValue("5"),
                        new StringValue("10")});
        META_ATTR_SHORT_DESC.put(MIGRATION_THRESHOLD_META_ATTR, Tools.getString("CRMXML.MigrationThreshold.ShortDesc"));
        META_ATTR_LONG_DESC.put(MIGRATION_THRESHOLD_META_ATTR, Tools.getString("CRMXML.MigrationThreshold.LongDesc"));
        META_ATTR_DEFAULT.put(MIGRATION_THRESHOLD_META_ATTR, DISABLED_IN_COMBOBOX);
        META_ATTR_TYPE.put(MIGRATION_THRESHOLD_META_ATTR, PARAM_TYPE_INTEGER);

        /* failure-timeout since 2.1.4 */
        META_ATTR_SHORT_DESC.put(FAILURE_TIMEOUT_META_ATTR, Tools.getString("CRMXML.FailureTimeout.ShortDesc"));
        META_ATTR_LONG_DESC.put(FAILURE_TIMEOUT_META_ATTR, Tools.getString("CRMXML.FailureTimeout.LongDesc"));
        META_ATTR_TYPE.put(FAILURE_TIMEOUT_META_ATTR, PARAM_TYPE_TIME);

        /* multiple-active */
        META_ATTR_COMBO_BOX_CHOICES.put(MULTIPLE_ACTIVE_META_ATTR, new Value[]{new StringValue("stop_start"),
                new StringValue("stop_only"),
                new StringValue("block")});
        META_ATTR_SHORT_DESC.put(MULTIPLE_ACTIVE_META_ATTR, Tools.getString("CRMXML.MultipleActive.ShortDesc"));
        META_ATTR_LONG_DESC.put(MULTIPLE_ACTIVE_META_ATTR, Tools.getString("CRMXML.MultipleActive.LongDesc"));
        META_ATTR_DEFAULT.put(MULTIPLE_ACTIVE_META_ATTR, new StringValue("stop_start"));

        /* master-max */
        META_ATTR_SHORT_DESC.put(MASTER_MAX_META_ATTR, "M/S Master-Max");
        META_ATTR_DEFAULT.put(MASTER_MAX_META_ATTR, new StringValue("1"));
        META_ATTR_TYPE.put(MASTER_MAX_META_ATTR, PARAM_TYPE_INTEGER);
        META_ATTR_COMBO_BOX_CHOICES.put(MASTER_MAX_META_ATTR, INTEGER_VALUES);
        RSC_DEFAULTS_META_ATTR_SECTION.put(MASTER_MAX_META_ATTR, "Master / Slave Resource Defaults");
        RSC_DEFAULTS_META_ATTR_ACCESS_TYPE.put(MASTER_MAX_META_ATTR, AccessMode.GOD);
        /* master-node-max */
        META_ATTR_SHORT_DESC.put(MASTER_NODE_MAX_META_ATTR, "M/S Master-Node-Max");
        META_ATTR_DEFAULT.put(MASTER_NODE_MAX_META_ATTR, new StringValue("1"));
        META_ATTR_TYPE.put(MASTER_NODE_MAX_META_ATTR, PARAM_TYPE_INTEGER);
        META_ATTR_COMBO_BOX_CHOICES.put(MASTER_NODE_MAX_META_ATTR, INTEGER_VALUES);
        RSC_DEFAULTS_META_ATTR_SECTION.put(MASTER_NODE_MAX_META_ATTR, "Master / Slave Resource Defaults");
        RSC_DEFAULTS_META_ATTR_ACCESS_TYPE.put(MASTER_NODE_MAX_META_ATTR, AccessMode.GOD);
        /* clone-max */
        META_ATTR_SHORT_DESC.put(CLONE_MAX_META_ATTR, "Clone Max");
        META_ATTR_DEFAULT.put(CLONE_MAX_META_ATTR, new StringValue(""));
        META_ATTR_PREFERRED.put(CLONE_MAX_META_ATTR, new StringValue("2"));
        META_ATTR_TYPE.put(CLONE_MAX_META_ATTR, PARAM_TYPE_INTEGER);
        META_ATTR_COMBO_BOX_CHOICES.put(CLONE_MAX_META_ATTR, INTEGER_VALUES);
        RSC_DEFAULTS_META_ATTR_SECTION.put(CLONE_MAX_META_ATTR, "Clone Resource Defaults");
        RSC_DEFAULTS_META_ATTR_ACCESS_TYPE.put(CLONE_MAX_META_ATTR, AccessMode.GOD);
        /* clone-node-max */
        META_ATTR_SHORT_DESC.put(CLONE_NODE_MAX_META_ATTR, "Clone Node Max");
        META_ATTR_DEFAULT.put(CLONE_NODE_MAX_META_ATTR, new StringValue("1"));
        META_ATTR_TYPE.put(CLONE_NODE_MAX_META_ATTR, PARAM_TYPE_INTEGER);
        META_ATTR_COMBO_BOX_CHOICES.put(CLONE_NODE_MAX_META_ATTR, INTEGER_VALUES);
        RSC_DEFAULTS_META_ATTR_SECTION.put(CLONE_NODE_MAX_META_ATTR, "Clone Resource Defaults");
        RSC_DEFAULTS_META_ATTR_ACCESS_TYPE.put(CLONE_NODE_MAX_META_ATTR, AccessMode.GOD);
        /* notify */
        META_ATTR_SHORT_DESC.put(NOTIFY_META_ATTR, "Notify");
        META_ATTR_DEFAULT.put(NOTIFY_META_ATTR, PCMK_FALSE_VALUE);
        META_ATTR_PREFERRED.put(NOTIFY_META_ATTR, PCMK_TRUE_VALUE);
        META_ATTR_COMBO_BOX_CHOICES.put(NOTIFY_META_ATTR, PCMK_BOOLEAN_VALUES);
        RSC_DEFAULTS_META_ATTR_SECTION.put(NOTIFY_META_ATTR, "Clone Resource Defaults");
        RSC_DEFAULTS_META_ATTR_ACCESS_TYPE.put(NOTIFY_META_ATTR, AccessMode.GOD);
        /* globally-unique */
        META_ATTR_SHORT_DESC.put(GLOBALLY_UNIQUE_META_ATTR, "Globally-Unique");
        META_ATTR_DEFAULT.put(GLOBALLY_UNIQUE_META_ATTR, PCMK_FALSE_VALUE);
        META_ATTR_COMBO_BOX_CHOICES.put(GLOBALLY_UNIQUE_META_ATTR, PCMK_BOOLEAN_VALUES);
        RSC_DEFAULTS_META_ATTR_SECTION.put(GLOBALLY_UNIQUE_META_ATTR, "Clone Resource Defaults");
        RSC_DEFAULTS_META_ATTR_ACCESS_TYPE.put(GLOBALLY_UNIQUE_META_ATTR, AccessMode.GOD);
        /* ordered */
        META_ATTR_SHORT_DESC.put(ORDERED_META_ATTR, "Ordered");
        META_ATTR_DEFAULT.put(ORDERED_META_ATTR, PCMK_FALSE_VALUE);
        META_ATTR_COMBO_BOX_CHOICES.put(ORDERED_META_ATTR, PCMK_BOOLEAN_VALUES);
        RSC_DEFAULTS_META_ATTR_SECTION.put(ORDERED_META_ATTR, "Clone Resource Defaults");
        RSC_DEFAULTS_META_ATTR_ACCESS_TYPE.put(ORDERED_META_ATTR, AccessMode.GOD);
        /* interleave */
        META_ATTR_SHORT_DESC.put(INTERLEAVE_META_ATTR, "Interleave");
        META_ATTR_DEFAULT.put(INTERLEAVE_META_ATTR, PCMK_FALSE_VALUE);
        META_ATTR_COMBO_BOX_CHOICES.put(INTERLEAVE_META_ATTR, PCMK_BOOLEAN_VALUES);
        RSC_DEFAULTS_META_ATTR_SECTION.put(INTERLEAVE_META_ATTR, "Clone Resource Defaults");
        RSC_DEFAULTS_META_ATTR_ACCESS_TYPE.put(INTERLEAVE_META_ATTR, AccessMode.GOD);
        META_ATTR_PREFERRED.put(INTERLEAVE_META_ATTR, PCMK_TRUE_VALUE);
        /* Group collocated */
        META_ATTR_SHORT_DESC.put(GROUP_COLLOCATED_META_ATTR, "Collocated");
        META_ATTR_DEFAULT.put(GROUP_COLLOCATED_META_ATTR, PCMK_TRUE_VALUE);
        META_ATTR_COMBO_BOX_CHOICES.put(GROUP_COLLOCATED_META_ATTR, PCMK_BOOLEAN_VALUES);
        RSC_DEFAULTS_META_ATTR_ACCESS_TYPE.put(GROUP_COLLOCATED_META_ATTR, AccessMode.ADMIN);
        /* group ordered */
        META_ATTR_SHORT_DESC.put(GROUP_ORDERED_META_ATTR, "Ordered");
        META_ATTR_DEFAULT.put(GROUP_ORDERED_META_ATTR, PCMK_TRUE_VALUE);
        META_ATTR_COMBO_BOX_CHOICES.put(GROUP_ORDERED_META_ATTR, PCMK_BOOLEAN_VALUES);
        RSC_DEFAULTS_META_ATTR_ACCESS_TYPE.put(GROUP_ORDERED_META_ATTR, AccessMode.ADMIN);

        /* ignore defaults for this RAs. It means that default values will be
        * saved in the cib. */
        IGNORE_RA_DEFAULTS_FOR.add("iSCSITarget");

        RA_NON_ADVANCED_PARAM.put("IPaddr2", "cidr_netmask", Tools.getString("CRMXML.OtherOptions"));
        RA_NON_ADVANCED_PARAM.put("VirtualDomain", "hypervisor", Tools.getString("CRMXML.OtherOptions"));
    }

    private static final AccessMode.Type DEFAULT_ACCESS_TYPE = AccessMode.ADMIN;

    public static Unit getUnitMilliSec() {
        return new Unit("ms", "ms", "Millisecond", "Milliseconds");
    }

    public static Unit getUnitMicroSec() {
        return new Unit("us", "us", "Microsecond", "Microseconds");
    }

    public static Unit getUnitSecond() {
        return new Unit("", "s", "Second", "Seconds");
    }

    public static Unit getUnitMinute() {
        return new Unit("min", "m", "Minute", "Minutes");
    }

    public static Unit getUnitHour() {
        return new Unit("h", "h", "Hour", "Hours");
    }

    public static Unit[] getUnits() {
        return new Unit[]{getUnitMilliSec(), getUnitMicroSec(), getUnitSecond(), getUnitMinute(), getUnitHour()};
    }

    private Host host;
    private final List<String> globalParams = new ArrayList<String>();
    private final Collection<String> globalNotAdvancedParams = new ArrayList<String>();
    private final Map<String, AccessMode.Type> paramGlobalAccessTypes =
                                                              new HashMap<String, AccessMode.Type>();
    private final Collection<String> globalRequiredParams = new ArrayList<String>();
    private final Map<String, List<ResourceAgent>> classToServicesMap = new ConcurrentHashMap<String, List<ResourceAgent>>();
    private final Map<String, String> globalShortDescMap = new HashMap<String, String>();
    private final Map<String, String> globalLongDescMap = new HashMap<String, String>();
    private final Map<String, Value> globalDefaultMap = new HashMap<String, Value>();
    private final Map<String, Value> globalPreferredValuesMap = new HashMap<String, Value>();
    private final Map<String, String> globalTypeMap = new HashMap<String, String>();
    private final Map<String, Value[]> globalComboBoxChoices = new HashMap<String, Value[]>();
    private final List<String> colocationParams = new ArrayList<String>();
    /**  (rsc_colocation tag) */
    private final List<String> resourceSetColocationParams = new ArrayList<String>();
    /** (resource_set tag) */
    private final List<String> resourceSetColocationConnectionParams = new ArrayList<String>();
    private final Collection<String> cololcationRequiredParams = new ArrayList<String>();
    private final Map<String, String> paramColocationShortDescMap = new HashMap<String, String>();
    private final Map<String, String> paramColocationLongDescMap = new HashMap<String, String>();
    private final Map<String, Value> paramColocationDefaultMap = new HashMap<String, Value>();
    private final Map<String, Value> paramColocationPreferredMap = new HashMap<String, Value>();
    private final Map<String, String> paramColocationTypeMap = new HashMap<String, String>();
    private final Map<String, Value[]> paramColocationPossibleChoices = new HashMap<String, Value[]>();
    private final Map<String, Value[]> paramColocationPossibleChoicesMasterSlave = new HashMap<String, Value[]>();

    private final List<String> orderParams = new ArrayList<String>();
    /**  (rsc_order tag) */
    private final List<String> resourceSetOrderParams = new ArrayList<String>();
    /** (resource_set tag) */
    private final List<String> resourceSetOrderConnectionParams = new ArrayList<String>();
    private final Collection<String> orderRequiredParams = new ArrayList<String>();
    private final Map<String, String> paramOrderShortDescMap = new HashMap<String, String>();
    private final Map<String, String> paramOrderLongDescMap = new HashMap<String, String>();
    private final Map<String, Value> paramOrderDefaultMap = new HashMap<String, Value>();
    private final Map<String, Value> paramOrderPreferredMap = new HashMap<String, Value>();
    private final Map<String, String> paramOrderTypeMap = new HashMap<String, String>();
    private final Map<String, Value[]> paramOrderPossibleChoices = new HashMap<String, Value[]>();
    private final Map<String, Value[]> paramOrderPossibleChoicesMasterSlave = new HashMap<String, Value[]>();
    private final ResourceAgent groupResourceAgent = new ResourceAgent(Application.PACEMAKER_GROUP_NAME, "", "group");
    private ResourceAgent cloneResourceAgent;
    private final ResourceAgent drbddiskResourceAgent = new ResourceAgent("drbddisk",
                                                                          ResourceAgent.HEARTBEAT_PROVIDER,
                                                                          ResourceAgent.HEARTBEAT_CLASS_NAME);
    private final ResourceAgent linbitDrbdResourceAgent = new ResourceAgent("drbd",
                                                                            "linbit",
                                                                            ResourceAgent.OCF_CLASS_NAME);
    private final MultiKeyMap<String, ResourceAgent> serviceToResourceAgentMap =
                                                                    new MultiKeyMap<String, ResourceAgent>();
    private boolean drbddiskResourceAgentPresent;
    private boolean linbitDrbdResourceAgentPresent;
    private final List<Value> stonithHostlistChoices = new ArrayList<Value>();
    private Map<String, String> metaAttrParams = null;
    private Map<String, String> resourceDefaultsMetaAttrs = null;

    public void init(final Host host, final ServicesInfo allServicesInfo) {
        this.host = host;
        final Value[] booleanValues = PCMK_BOOLEAN_VALUES;
        final Value hbBooleanTrue = booleanValues[0];
        final Value hbBooleanFalse = booleanValues[1];
        /* hostlist choices for stonith */
        stonithHostlistChoices.add(new StringValue());
        final String[] hosts = host.getCluster().getHostNames();
        if (hosts != null && hosts.length < 8) {
            stonithHostlistChoices.add(new StringValue(Tools.join(" ", hosts)));
            for (final String h : hosts) {
                stonithHostlistChoices.add(new StringValue(h));
            }
        }
        /* clones */
        cloneResourceAgent = new ResourceAgent(Application.PM_CLONE_SET_NAME, "", "clone");
        cloneResourceAgent.setMetaDataLoaded(true);
        addMetaAttributeToResourceAgent(cloneResourceAgent, MASTER_MAX_META_ATTR, null, true);
        addMetaAttributeToResourceAgent(cloneResourceAgent, MASTER_NODE_MAX_META_ATTR, null, true);
        addMetaAttributeToResourceAgent(cloneResourceAgent, CLONE_MAX_META_ATTR, null, false);
        addMetaAttributeToResourceAgent(cloneResourceAgent, CLONE_NODE_MAX_META_ATTR, null, false);
        addMetaAttributeToResourceAgent(cloneResourceAgent, NOTIFY_META_ATTR, null, false);
        addMetaAttributeToResourceAgent(cloneResourceAgent, GLOBALLY_UNIQUE_META_ATTR, null, false);
        addMetaAttributeToResourceAgent(cloneResourceAgent, ORDERED_META_ATTR, null, false);
        addMetaAttributeToResourceAgent(cloneResourceAgent, INTERLEAVE_META_ATTR, null, false);

        addMetaAttributeToResourceAgent(groupResourceAgent, GROUP_ORDERED_META_ATTR, null, false);
        addMetaAttributeToResourceAgent(groupResourceAgent, GROUP_COLLOCATED_META_ATTR, null, false);
        /* groups */
        final Map<String, String> maParams = getMetaAttrParameters();
        for (final String metaAttr : maParams.keySet()) {
            addMetaAttributeToResourceAgent(cloneResourceAgent, metaAttr, maParams.get(metaAttr), false);
            addMetaAttributeToResourceAgent(groupResourceAgent, metaAttr, maParams.get(metaAttr), false);
        }

        /* Hardcoding global params */
        /* symmetric cluster */
        globalParams.add("symmetric-cluster");
        globalShortDescMap.put("symmetric-cluster", "Symmetric Cluster");
        globalLongDescMap.put("symmetric-cluster", "Symmetric Cluster");
        globalTypeMap.put("symmetric-cluster", PARAM_TYPE_BOOLEAN);
        globalDefaultMap.put("symmetric-cluster", hbBooleanFalse);
        globalComboBoxChoices.put("symmetric-cluster", booleanValues);
        globalRequiredParams.add("symmetric-cluster");
        globalNotAdvancedParams.add("symmetric-cluster");

        /* stonith enabled */
        globalParams.add("stonith-enabled");
        globalShortDescMap.put("stonith-enabled", "Stonith Enabled");
        globalLongDescMap.put("stonith-enabled", "Stonith Enabled");
        globalTypeMap.put("stonith-enabled", PARAM_TYPE_BOOLEAN);
        globalDefaultMap.put("stonith-enabled", hbBooleanTrue);
        globalComboBoxChoices.put("stonith-enabled", booleanValues);
        globalRequiredParams.add("stonith-enabled");
        globalNotAdvancedParams.add("stonith-enabled");

        /* transition timeout */
        globalParams.add("default-action-timeout");
        globalShortDescMap.put("default-action-timeout", "Transition Timeout");
        globalLongDescMap.put("default-action-timeout", "Transition Timeout");
        globalTypeMap.put("default-action-timeout", PARAM_TYPE_INTEGER);
        globalDefaultMap.put("default-action-timeout", new StringValue("20"));
        globalComboBoxChoices.put("default-action-timeout", INTEGER_VALUES);
        globalRequiredParams.add("default-action-timeout");

        /* resource stickiness */
        /* special case: is advanced parameter if not set. */
        globalParams.add("default-resource-stickiness");
        globalShortDescMap.put("default-resource-stickiness", "Resource Stickiness");
        globalLongDescMap.put("default-resource-stickiness", "Resource Stickiness");
        globalTypeMap.put("default-resource-stickiness", PARAM_TYPE_INTEGER);
        globalComboBoxChoices.put("default-resource-stickiness", INTEGER_VALUES);
        globalDefaultMap.put("default-resource-stickiness", new StringValue("0"));
        globalRequiredParams.add("default-resource-stickiness");

        /* no quorum policy */
        globalParams.add("no-quorum-policy");
        globalShortDescMap.put("no-quorum-policy", "No Quorum Policy");
        globalLongDescMap.put("no-quorum-policy", "No Quorum Policy");
        globalTypeMap.put("no-quorum-policy", PARAM_TYPE_STRING);
        globalDefaultMap.put("no-quorum-policy", new StringValue("stop"));
        globalComboBoxChoices.put("no-quorum-policy", new Value[]{new StringValue("ignore"),
                new StringValue("stop"),
                new StringValue("freeze"),
                new StringValue("suicide")});
        globalRequiredParams.add("no-quorum-policy");
        globalNotAdvancedParams.add("no-quorum-policy");

        /* resource failure stickiness */
        globalParams.add("default-resource-failure-stickiness");
        globalShortDescMap.put("default-resource-failure-stickiness", "Resource Failure Stickiness");
        globalLongDescMap.put("default-resource-failure-stickiness", "Resource Failure Stickiness");
        globalTypeMap.put("default-resource-failure-stickiness", PARAM_TYPE_INTEGER);
        globalComboBoxChoices.put("default-resource-failure-stickiness", INTEGER_VALUES);
        globalDefaultMap.put("default-resource-failure-stickiness", new StringValue("0"));
        globalRequiredParams.add("default-resource-failure-stickiness");

        globalComboBoxChoices.put("placement-strategy", new Value[]{new StringValue("default"),
                new StringValue("utilization"),
                new StringValue("minimal"),
                new StringValue("balanced")});

        final String hbV = host.getHostParser().getHeartbeatVersion();
        final String pcmkV = host.getHostParser().getPacemakerVersion();
        try {
            if (pcmkV != null || Tools.compareVersions(hbV, "2.1.3") >= 0) {
                String clusterRecheckInterval = "cluster-recheck-interval";
                String dcDeadtime = "dc-deadtime";
                String electionTimeout = "election-timeout";
                String shutdownEscalation = "shutdown-escalation";
                if (Tools.versionBeforePacemaker(host)) {
                    clusterRecheckInterval = "cluster_recheck_interval";
                    dcDeadtime = "dc_deadtime";
                    electionTimeout = "election_timeout";
                    shutdownEscalation = "shutdown_escalation";
                }
                final String[] params = {"stonith-action",
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
                                         "crmd-finalization-timeout",
                                         "expected-quorum-votes",
                                         "maintenance-mode",
                };
                globalParams.add("dc-version");
                globalShortDescMap.put("dc-version", "DC Version");
                globalTypeMap.put("dc-version", PARAM_TYPE_LABEL);
                paramGlobalAccessTypes.put("dc-version", AccessMode.NEVER);
                globalParams.add("cluster-infrastructure");
                globalShortDescMap.put("cluster-infrastructure", "Cluster Infrastructure");
                globalTypeMap.put("cluster-infrastructure", PARAM_TYPE_LABEL);
                paramGlobalAccessTypes.put("cluster-infrastructure", AccessMode.NEVER);

                globalNotAdvancedParams.add("no-quorum-policy");
                globalNotAdvancedParams.add("maintenance-mode");
                paramGlobalAccessTypes.put("maintenance-mode", AccessMode.OP);
                globalNotAdvancedParams.add(clusterRecheckInterval);

                for (final String param : params) {
                    globalParams.add(param);
                    final String[] parts = param.split("[-_]");
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
                    globalShortDescMap.put(param, name);
                    globalLongDescMap.put(param, name);
                    globalTypeMap.put(param, PARAM_TYPE_STRING);
                    globalDefaultMap.put(param, new StringValue());
                }
                globalDefaultMap.put("stonith-action", new StringValue("reboot"));
                globalComboBoxChoices.put("stonith-action", new Value[]{new StringValue("reboot"),
                        new StringValue("poweroff")});

                globalTypeMap.put("is-managed-default", PARAM_TYPE_BOOLEAN);
                globalDefaultMap.put("is-managed-default", hbBooleanFalse);
                globalComboBoxChoices.put("is-managed-default", booleanValues);

                globalTypeMap.put("stop-orphan-resources", PARAM_TYPE_BOOLEAN);
                globalDefaultMap.put("stop-orphan-resources", hbBooleanFalse);
                globalComboBoxChoices.put("stop-orphan-resources", booleanValues);

                globalTypeMap.put("stop-orphan-actions", PARAM_TYPE_BOOLEAN);
                globalDefaultMap.put("stop-orphan-actions", hbBooleanFalse);
                globalComboBoxChoices.put("stop-orphan-actions", booleanValues);

                globalTypeMap.put("remove-after-stop", PARAM_TYPE_BOOLEAN);
                globalDefaultMap.put("remove-after-stop", hbBooleanFalse);
                globalComboBoxChoices.put("remove-after-stop", booleanValues);

                globalTypeMap.put("startup-fencing", PARAM_TYPE_BOOLEAN);
                globalDefaultMap.put("startup-fencing", hbBooleanFalse);
                globalComboBoxChoices.put("startup-fencing", booleanValues);

                globalTypeMap.put("start-failure-is-fatal", PARAM_TYPE_BOOLEAN);
                globalDefaultMap.put("start-failure-is-fatal", hbBooleanFalse);
                globalComboBoxChoices.put("start-failure-is-fatal", booleanValues);
            }
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("CRMXML: " + e.getMessage(), e);
        }

        /* Hardcoding colocation params */
        colocationParams.add("rsc-role");
        paramColocationShortDescMap.put("rsc-role", "rsc col role");
        paramColocationLongDescMap.put("rsc-role", "@RSC@ colocation role");
        paramColocationTypeMap.put("rsc-role", PARAM_TYPE_STRING);
        paramColocationPossibleChoices.put("rsc-role", ATTRIBUTE_ROLES);
        paramColocationPossibleChoicesMasterSlave.put("rsc-role", ATTRIBUTE_ROLES_MASTER_SLAVE);

        colocationParams.add("with-rsc-role");
        paramColocationShortDescMap.put("with-rsc-role", "with-rsc col role");
        paramColocationLongDescMap.put("with-rsc-role", "@WITH-RSC@ colocation role");
        paramColocationTypeMap.put("with-rsc-role", PARAM_TYPE_STRING);
        paramColocationPossibleChoices.put("with-rsc-role", ATTRIBUTE_ROLES);
        paramColocationPossibleChoicesMasterSlave.put("with-rsc-role", ATTRIBUTE_ROLES_MASTER_SLAVE);

        colocationParams.add(SCORE_CONSTRAINT_PARAM);
        paramColocationShortDescMap.put(SCORE_CONSTRAINT_PARAM, "Score");
        paramColocationLongDescMap.put(SCORE_CONSTRAINT_PARAM, "Score");
        paramColocationTypeMap.put(SCORE_CONSTRAINT_PARAM, PARAM_TYPE_INTEGER);
        paramColocationDefaultMap.put(SCORE_CONSTRAINT_PARAM, null);
        paramColocationPreferredMap.put(SCORE_CONSTRAINT_PARAM, INFINITY_VALUE);
        paramColocationPossibleChoices.put(SCORE_CONSTRAINT_PARAM, INTEGER_VALUES);
        /* Hardcoding order params */
        orderParams.add("first-action");
        paramOrderShortDescMap.put("first-action", "first order action");
        paramOrderLongDescMap.put("first-action", "@FIRST-RSC@ order action");
        paramOrderTypeMap.put("first-action", PARAM_TYPE_STRING);
        paramOrderPossibleChoices.put("first-action", ATTRIBUTE_ACTIONS);
        paramOrderPossibleChoicesMasterSlave.put("first-action", ATTRIBUTE_ACTIONS_MASTER_SLAVE);
        paramOrderPreferredMap.put(SCORE_CONSTRAINT_PARAM, INFINITY_VALUE);
        paramOrderDefaultMap.put("first-action", null);

        orderParams.add("then-action");
        paramOrderShortDescMap.put("then-action", "then order action");
        paramOrderLongDescMap.put("then-action", "@THEN-RSC@ order action");
        paramOrderTypeMap.put("then-action", PARAM_TYPE_STRING);
        paramOrderPossibleChoices.put("then-action", ATTRIBUTE_ACTIONS);
        paramOrderPossibleChoicesMasterSlave.put("then-action", ATTRIBUTE_ACTIONS_MASTER_SLAVE);
        paramOrderDefaultMap.put("then-action", null);

        orderParams.add("symmetrical");
        paramOrderShortDescMap.put("symmetrical", "Symmetrical");
        paramOrderLongDescMap.put("symmetrical", "Symmetrical");
        paramOrderTypeMap.put("symmetrical", PARAM_TYPE_BOOLEAN);
        paramOrderDefaultMap.put("symmetrical", hbBooleanTrue);
        paramOrderPossibleChoices.put("symmetrical", booleanValues);

        orderParams.add(SCORE_CONSTRAINT_PARAM);
        paramOrderShortDescMap.put(SCORE_CONSTRAINT_PARAM, "Score");
        paramOrderLongDescMap.put(SCORE_CONSTRAINT_PARAM, "Score");
        paramOrderTypeMap.put(SCORE_CONSTRAINT_PARAM, PARAM_TYPE_INTEGER);
        paramOrderPossibleChoices.put(SCORE_CONSTRAINT_PARAM, INTEGER_VALUES);
        paramOrderDefaultMap.put(SCORE_CONSTRAINT_PARAM, null);
        /* resource sets */
        resourceSetOrderParams.add(SCORE_CONSTRAINT_PARAM);
        resourceSetColocationParams.add(SCORE_CONSTRAINT_PARAM);

        resourceSetOrderConnectionParams.add("action");
        paramOrderShortDescMap.put("action", "order action");
        paramOrderLongDescMap.put("action", "order action");
        paramOrderTypeMap.put("action", PARAM_TYPE_STRING);
        paramOrderPossibleChoices.put("action", ATTRIBUTE_ACTIONS);
        paramOrderPossibleChoicesMasterSlave.put("action", ATTRIBUTE_ACTIONS_MASTER_SLAVE);
        paramOrderDefaultMap.put("action", null);

        resourceSetOrderConnectionParams.add("sequential");
        paramOrderShortDescMap.put("sequential", "sequential");
        paramOrderLongDescMap.put("sequential", "sequential");
        paramOrderTypeMap.put("sequential", PARAM_TYPE_BOOLEAN);
        paramOrderDefaultMap.put("sequential", hbBooleanTrue);
        paramOrderPossibleChoices.put("sequential", booleanValues);
        paramOrderPreferredMap.put("sequential", hbBooleanFalse);

        resourceSetOrderConnectionParams.add(REQUIRE_ALL_ATTR);
        paramOrderShortDescMap.put(REQUIRE_ALL_ATTR, "require all");
        paramOrderLongDescMap.put(REQUIRE_ALL_ATTR, "require all");
        paramOrderTypeMap.put(REQUIRE_ALL_ATTR, PARAM_TYPE_BOOLEAN);
        paramOrderDefaultMap.put(REQUIRE_ALL_ATTR, REQUIRE_ALL_TRUE_VALUE);
        paramOrderPossibleChoices.put(REQUIRE_ALL_ATTR, booleanValues);

        resourceSetColocationConnectionParams.add("role");
        paramColocationShortDescMap.put("role", "col role");
        paramColocationLongDescMap.put("role", "colocation role");
        paramColocationTypeMap.put("role", PARAM_TYPE_STRING);
        paramColocationPossibleChoices.put("role", ATTRIBUTE_ROLES);
        paramColocationPossibleChoicesMasterSlave.put("role", ATTRIBUTE_ROLES_MASTER_SLAVE);

        resourceSetColocationConnectionParams.add("sequential");
        paramColocationShortDescMap.put("sequential", "sequential");
        paramColocationLongDescMap.put("sequential", "sequential");
        paramColocationTypeMap.put("sequential", PARAM_TYPE_BOOLEAN);
        paramColocationDefaultMap.put("sequential", hbBooleanTrue);
        paramColocationPossibleChoices.put("sequential", booleanValues);
        paramColocationPreferredMap.put("sequential", hbBooleanFalse);
        groupResourceAgent.setMetaDataLoaded(true);

        initResourceAgentsWithoutMetaData();
        initOCFMetaDataConfigured();
        LOG.debug("CRMXML: cluster loaded");
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                initOCFMetaDataAll();
                final String hn = host.getName();
                final String text = Tools.getString("CRMXML.GetRAMetaData.Done");
                progressIndicator.startProgressIndicator(hn, text);
                final ClusterBrowser browser = allServicesInfo.getBrowser();
                final ClusterStatus clusterStatus = browser.getClusterStatus();
                if (clusterStatus != null) {
                    resourceUpdaterProvider.get().updateAllResources(allServicesInfo, browser, clusterStatus, Application.RunMode.LIVE);
                }
                final InfoPresenter lastSelectedInfo = browser.getClusterViewPanel().getLastSelectedInfo();
                if (lastSelectedInfo instanceof ServiceInfo || lastSelectedInfo instanceof ServicesInfo) {
                    browser.getClusterViewPanel().reloadRightComponent();
                }
                progressIndicator.stopProgressIndicator(hn, text);
                LOG.debug("CRMXML: RAs loaded");
                final Test autoTest = application.getAutoTest();
                if (autoTest != null) {
                    startTests.startTest(autoTest, browser.getCluster());
                }
            }
        });
        t.start();
    }

    private void initResourceAgentsWithoutMetaData() {
        final String command = host.getHostParser().getDistCommand("Heartbeat.getOCFParametersQuick", (ConvertCmdCallback) null);
        final SshOutput ret = host.captureCommandProgressIndicator(Tools.getString("CRMXML.GetRAMetaData"),
                                                                   new ExecCommandConfig().command(command)
                                                                                          .silentCommand()
                                                                                          .silentOutput()
                                                                                          .sshCommandTimeout(60000));
        boolean linbitDrbdPresent0 = false;
        boolean drbddiskPresent0 = false;
        if (ret.getExitCode() != 0) {
            drbddiskResourceAgentPresent = drbddiskPresent0;
            linbitDrbdResourceAgentPresent = linbitDrbdPresent0;
            return;
        }
        final String output = ret.getOutput();
        if (output == null) {
            drbddiskResourceAgentPresent = drbddiskPresent0;
            linbitDrbdResourceAgentPresent = linbitDrbdPresent0;
            return;
        }
        final String[] lines = output.split("\\r?\\n");
        final Pattern cp = Pattern.compile("^class:\\s*(.*?)\\s*$");
        final Pattern pp = Pattern.compile("^provider:\\s*(.*?)\\s*$");
        final Pattern sp = Pattern.compile("^ra:\\s*(.*?)\\s*$");
        final StringBuilder xml = new StringBuilder("");
        String resourceClass = null;
        String provider = null;
        String serviceName = null;
        for (final String line : lines) {
            final Matcher cm = cp.matcher(line);
            if (cm.matches()) {
                resourceClass = cm.group(1);
                continue;
            }
            final Matcher pm = pp.matcher(line);
            if (pm.matches()) {
                provider = pm.group(1);
                continue;
            }
            final Matcher sm = sp.matcher(line);
            if (sm.matches()) {
                serviceName = sm.group(1);
            }
            if (serviceName != null) {
                xml.append(line);
                xml.append('\n');
                if ("drbddisk".equals(serviceName)) {
                    drbddiskPresent0 = true;
                } else if ("drbd".equals(serviceName) && "linbit".equals(provider)) {
                    linbitDrbdPresent0 = true;
                }
                final ResourceAgent ra;
                if ("drbddisk".equals(serviceName) && ResourceAgent.HEARTBEAT_CLASS_NAME.equals(resourceClass)) {
                    ra = drbddiskResourceAgent;
                    ra.setMetaDataLoaded(true);
                    setLsbResourceAgent(serviceName, resourceClass, ra);
                } else if ("drbd".equals(serviceName)
                           && ResourceAgent.OCF_CLASS_NAME.equals(resourceClass)
                           && "linbit".equals(provider)) {
                    ra = linbitDrbdResourceAgent;
                } else {
                    ra = new ResourceAgent(serviceName, provider, resourceClass);
                    if (IGNORE_RA_DEFAULTS_FOR.contains(serviceName)) {
                        ra.setIgnoreDefaults(true);
                    }
                    if (ResourceAgent.SERVICE_CLASSES.contains(resourceClass)
                        || ResourceAgent.HEARTBEAT_CLASS_NAME.equals(resourceClass)) {
                        ra.setMetaDataLoaded(true);
                        setLsbResourceAgent(serviceName, resourceClass, ra);
                    }
                }
                serviceToResourceAgentMap.put(serviceName, provider, resourceClass, ra);
                List<ResourceAgent> raList = classToServicesMap.get(resourceClass);
                if (raList == null) {
                    raList = new ArrayList<ResourceAgent>();
                    classToServicesMap.put(resourceClass, raList);
                }
                raList.add(ra);
                serviceName = null;
                xml.delete(0, xml.length());
            }
        }
        drbddiskResourceAgentPresent = drbddiskPresent0;
        linbitDrbdResourceAgentPresent = linbitDrbdPresent0;
    }

    /**
     * Initialize resource agents with their meta data, the configured ones.
     * For faster start up.
     */
    private void initOCFMetaDataConfigured() {
        initOCFResourceAgentsWithMetaData(host.getHostParser().getDistCommand("Heartbeat.getOCFParametersConfigured",
                                          (ConvertCmdCallback) null));
    }

    /** Initialize resource agents with their meta data. */
    private void initOCFMetaDataAll() {
        initOCFResourceAgentsWithMetaData(host.getHostParser().getDistCommand("Heartbeat.getOCFParameters",
                                          (ConvertCmdCallback) null));
    }

    private void initOCFResourceAgentsWithMetaData(final String command) {
        final SshOutput ret = host.captureCommand(new ExecCommandConfig().command(command)
                                                                         .silentCommand()
                                                                         .silentOutput()
                                                                         .sshCommandTimeout(300000));
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
        final Pattern bp = Pattern.compile("<resource-agent.*\\s+name=\"(.*?)\".*");
        final Pattern sp = Pattern.compile("^ra-name:\\s*(.*?)\\s*$");
        final Pattern ep = Pattern.compile("</resource-agent>");
        final StringBuilder xml = new StringBuilder("");
        String provider = null;
        String serviceName = null;
        boolean nextRA = false;
        boolean masterSlave = false; /* is probably m/s ...*/
        for (final String line : lines) {
            /*
            <resource-agent name="AudibleAlarm">
            ...
            </resource-agent>
            */
            final Matcher pm = pp.matcher(line);
            if (pm.matches()) {
                provider = pm.group(1);
                continue;
            }
            final Matcher mm = mp.matcher(line);
            if (mm.matches()) {
                masterSlave = !"".equals(mm.group(1));
                continue;
            }
            final Matcher sm = sp.matcher(line);
            if (sm.matches()) {
                serviceName = sm.group(1);
                continue;
            }
            final Matcher m = bp.matcher(line);
            if (m.matches()) {
                nextRA = true;
            }
            if (nextRA) {
                xml.append(line);
                xml.append('\n');
                final Matcher m2 = ep.matcher(line);
                if (m2.matches()) {
                    parseMetaDataAndFillHashes(serviceName, provider, xml.toString(), masterSlave);
                    serviceName = null;
                    nextRA = false;
                    xml.delete(0, xml.length());
                }
            }
        }
        if (!drbddiskResourceAgentPresent) {
            LOG.appWarning("initOCFMetaData: drbddisk heartbeat script is not present");
        }
    }

    /** Returns choices for check box. (True, False). */
    public Value[] getCheckBoxChoices(final ResourceAgent resourceAgent, final String param) {
        final String paramDefault = getDefaultValue(resourceAgent, param);
        return getCheckBoxChoices(paramDefault);
    }

    /**
     * Returns choices for check box. (True, False).
     * The problem is, that heartbeat/pacemaker kept changing the lower and upper case in
     * the true and false values.
     */
    private Value[] getCheckBoxChoices(final String paramDefault) {
        if (paramDefault != null) {
            if ("yes".equals(paramDefault) || "no".equals(paramDefault)) {
                return new Value[]{new StringValue("yes"), new StringValue("no")};
            } else if ("Yes".equals(paramDefault) || "No".equals(paramDefault)) {
                return new Value[]{new StringValue("Yes"), new StringValue("No")};
            } else if (PCMK_TRUE_VALUE.getValueForConfig().equals(paramDefault)
                       || PCMK_FALSE_VALUE.getValueForConfig().equals(paramDefault)) {
                return PCMK_BOOLEAN_VALUES.clone();
            } else if ("True".equals(paramDefault) || "False".equals(paramDefault)) {
                return new Value[]{new StringValue("True"), new StringValue("False")};
            }
        }
        return PCMK_BOOLEAN_VALUES.clone();
    }

    /**
     * Returns all services as array of strings, sorted, with filesystem and
     * ipaddr in the beginning.
     */
    public List<ResourceAgent> getServices(final String raClass) {
        final List<ResourceAgent> services = classToServicesMap.get(raClass);
        if (services == null) {
            return new ArrayList<ResourceAgent>();
        }
        Collections.sort(Lists.newArrayList(services), new Comparator<ResourceAgent>() {
                                       @Override
                                       public int compare(final ResourceAgent ra1, final ResourceAgent ra2) {
                                           return ra1.getServiceName().compareToIgnoreCase(ra2.getServiceName());
                                       }
                                   });
        return services;
    }

    public List<String> getOcfMetaDataParameters(final ResourceAgent resourceAgent, final boolean master) {
        /* return cached values */
        return resourceAgent.getParameters(master);
    }

    public String[] getGlobalParameters() {
        if (globalParams != null) {
            return globalParams.toArray(new String[globalParams.size()]);
        }
        return null;
    }

    /** Return version of the service ocf script. */
    public String getOcfScriptVersion(final ResourceAgent resourceAgent) {
        return resourceAgent.getServiceVersion();
    }

    public String getShortDesc(final ResourceAgent resourceAgent) {
        return resourceAgent.getServiceShortDesc();
    }

    public String getLongDesc(final ResourceAgent resourceAgent) {
        return resourceAgent.getServiceLongDesc();
    }

    public String getGlobalShortDesc(final String param) {
        String shortDesc = globalShortDescMap.get(param);
        if (shortDesc == null) {
            shortDesc = param;
        }
        return shortDesc;
    }

    public String getShortDesc(final ResourceAgent resourceAgent, final String param) {
        return resourceAgent.getShortDesc(param);
    }

    public String getGlobalLongDesc(final String param) {
        final String shortDesc = getGlobalShortDesc(param);
        String longDesc = globalLongDescMap.get(param);
        if (longDesc == null) {
            longDesc = "";
        }
        return Tools.html("<b>" + shortDesc + "</b>\n" + longDesc);
    }

    public String getLongDesc(final ResourceAgent resourceAgent, final String param) {
        final String shortDesc = getShortDesc(resourceAgent, param);
        String longDesc = resourceAgent.getLongDesc(param);
        if (longDesc == null) {
            longDesc = "";
        }
        return Tools.html("<b>" + shortDesc + "</b>\n" + longDesc);
    }

    /**
     * Returns type of a global parameter. It can be string, integer, boolean...
     */
    public String getGlobalType(final String param) {
        return globalTypeMap.get(param);
    }

    /** Returns type of the parameter. It can be string, integer, boolean... */
    public String getParamType(final ResourceAgent resourceAgent, final String param) {
        return resourceAgent.getParamType(param);
    }

    public Value getGlobalParamDefault(final String param) {
        return globalDefaultMap.get(param);
    }

    public Value getGlobalPreferredValue(final String param) {
        return globalPreferredValuesMap.get(param);
    }

    public String getPreferredValue(final ResourceAgent resourceAgent, final String param) {
        return resourceAgent.getPreferredValue(param);
    }

    public String getDefaultValue(final ResourceAgent resourceAgent, final String param) {
        return resourceAgent.getDefaultValue(param);
    }

    public Value[] getGlobalComboBoxChoices(final String param) {
        return globalComboBoxChoices.get(param);
    }

    public Value[] getComboBoxChoices(final ResourceAgent resourceAgent,
                                      final String param,
                                      final boolean masterSlave) {
        if (masterSlave) {
            return resourceAgent.getComboBoxChoicesMasterSlave(param);
        } else {
            return resourceAgent.getComboBoxChoices(param);
        }
    }

    public boolean isGlobalParamAdvanced(final String param) {
        return !globalNotAdvancedParams.contains(param);
    }

    public AccessMode.Type getGlobalParamAccessType(final String param) {
        final AccessMode.Type accessType = paramGlobalAccessTypes.get(param);
        if (accessType == null) {
            return DEFAULT_ACCESS_TYPE;
        }
        return accessType;
    }

    public boolean isGlobalRequired(final String param) {
        return globalRequiredParams.contains(param);
    }

    public boolean isAdvanced(final ResourceAgent resourceAgent, final String param) {
        if (isMetaAttr(resourceAgent, param)) {
            if (resourceAgent == groupResourceAgent) {
                return !GROUP_META_ATTR_NOT_ADVANCED.contains(param);
            } else if (resourceAgent == cloneResourceAgent) {
                return true;
            }
            return !META_ATTR_NOT_ADVANCED.contains(param);
        }
        if (RA_NON_ADVANCED_PARAM.contains(resourceAgent.getServiceName(), param)) {
            return false;
        }
        return !isRequired(resourceAgent, param);
    }

    public AccessMode.Type getAccessType(final ResourceAgent resourceAgent, final String param) {
        if (isMetaAttr(resourceAgent, param)) {
            final AccessMode.Type accessType = META_ATTR_ACCESS_TYPE.get(param);
            if (accessType != null) {
                return accessType;
            }
        }
        return AccessMode.ADMIN;
    }

    public boolean isRequired(final ResourceAgent resourceAgent, final String param) {
        return resourceAgent.isRequired(param);
    }

    public boolean isMetaAttr(final ResourceAgent resourceAgent, final String param) {
        return resourceAgent.isParamMetaAttr(param);
    }

    public boolean isIntegerValue(final ResourceAgent resourceAgent, final String param) {
        final String type = getParamType(resourceAgent, param);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    public boolean isLabel(final ResourceAgent resourceAgent, final String param) {
        final String type = getParamType(resourceAgent, param);
        return PARAM_TYPE_LABEL.equals(type);
    }

    public boolean isBoolean(final ResourceAgent resourceAgent, final String param) {
        final String type = getParamType(resourceAgent, param);
        return PARAM_TYPE_BOOLEAN.equals(type);
    }

    public boolean isGlobalInteger(final String param) {
        final String type = getGlobalType(param);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    public boolean isGlobalLabel(final String param) {
        final String type = getGlobalType(param);
        return PARAM_TYPE_LABEL.equals(type);
    }

    public boolean isGlobalBoolean(final String param) {
        final String type = getGlobalType(param);
        return PARAM_TYPE_BOOLEAN.equals(type);
    }

    public boolean isTimeType(final ResourceAgent resourceAgent, final String param) {
        final String type = getParamType(resourceAgent, param);
        return PARAM_TYPE_TIME.equals(type);
    }

    public boolean isGlobalTimeType(final String param) {
        final String type = getGlobalType(param);
        return PARAM_TYPE_TIME.equals(type);
    }

    public String getSectionForDisplay(final ResourceAgent resourceAgent, final String param) {
        final String section = resourceAgent.getSection(param);
        if (section != null) {
            return section;
        }
        if (isMetaAttr(resourceAgent, param)) {
            return Tools.getString("CRMXML.MetaAttrOptions");
        } else if (isRequired(resourceAgent, param)) {
            return Tools.getString("CRMXML.RequiredOptions");
        } else {
            return Tools.getString("CRMXML.OptionalOptions");
        }
    }

    public String getGlobalSectionForDisplay(final String param) {
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
        final String newParam = resourceDefaultsMetaAttrs.get(param);
        if (newParam == null) {
            return param;
        }
        return newParam;
    }

    public boolean checkMetaAttrParam(final String param, final Value value) {
        final String newParam = convertRscDefaultsParam(param);
        final String type = META_ATTR_TYPE.get(newParam);
        final boolean required = isRscDefaultsRequired(newParam);
        final boolean metaAttr = true;
        return checkParam(type, required, metaAttr, newParam, value);
    }

    public String getRscDefaultsMetaAttrSection(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        final String section = RSC_DEFAULTS_META_ATTR_SECTION.get(newParam);
        if (section == null) {
            return Tools.getString("CRMXML.RscDefaultsSection");
        }
        return section;
    }

    public Value getRscDefaultsMetaAttrDefault(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        return META_ATTR_DEFAULT.get(newParam);
    }

    /** Returns preferred of the meta attribute. */
    public Value getRscDefaultsPreferred(final String param) {
        return null;
    }

    public Value[] getRscDefaultsComboBoxChoices(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        return META_ATTR_COMBO_BOX_CHOICES.get(newParam);
    }

    /** Returns choices for check box. (True, False). */
    public Value[] getRscDefaultsCheckBoxChoices(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        final Value paramDefault = getRscDefaultsMetaAttrDefault(newParam);
        return getCheckBoxChoices(paramDefault.getValueForConfig());
    }

    public String getRscDefaultsMetaAttrShortDesc(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        return META_ATTR_SHORT_DESC.get(newParam);
    }

    public String getRscDefaultsMetaAttrLongDesc(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        return META_ATTR_LONG_DESC.get(newParam);
    }

    /**
     * It can be string, integer, boolean...
     */
    public String getRscDefaultsMetaAttrType(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        return META_ATTR_TYPE.get(newParam);
    }

    public boolean isRscDefaultsAdvanced(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        return !META_ATTR_NOT_ADVANCED.contains(newParam);
    }


    public AccessMode.Type getRscDefaultsMetaAttrAccessType(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        final AccessMode.Type accessType = RSC_DEFAULTS_META_ATTR_ACCESS_TYPE.get(newParam);
        if (accessType == null) {
            return AccessMode.Type.ADMIN;
        }
        return accessType;
    }

    public boolean isRscDefaultsRequired(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        return false;
    }

    public boolean isRscDefaultsInteger(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        final String type = getRscDefaultsMetaAttrType(newParam);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    public boolean isRscDefaultsLabel(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        final String type = getRscDefaultsMetaAttrType(newParam);
        return PARAM_TYPE_LABEL.equals(type);
    }

    public boolean isRscDefaultsMetaAttrBoolean(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        final String type = getRscDefaultsMetaAttrType(newParam);
        return PARAM_TYPE_BOOLEAN.equals(type);
    }

    public boolean isRscDefaultsTimeType(final String param) {
        final String newParam = convertRscDefaultsParam(param);
        final String type = getRscDefaultsMetaAttrType(newParam);
        return PARAM_TYPE_TIME.equals(type);
    }

    /**
     * Checks parameter of the specified ra according to its type.
     * Returns false if value does not fit the type.
     */
    public boolean checkParam(final ResourceAgent resourceAgent, final String param, final Value value) {
        final String type = getParamType(resourceAgent, param);
        final boolean required = isRequired(resourceAgent, param);
        final boolean metaAttr = isMetaAttr(resourceAgent, param);
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
                               Value value) {
        if (metaAttr && isRscDefaultsInteger(param) && DISABLED_IN_COMBOBOX.equals(value)) {
            value = new StringValue();
        }
        boolean correctValue = true;
        if (PARAM_TYPE_BOOLEAN.equals(type)) {
            if (!PCMK_YES.equals(value) && !PCMK_NO.equals(value)
                && !PCMK_TRUE_VALUE.equals(value)
                && !PCMK_FALSE_VALUE.equals(value)
                && !PCMK_TRUE_2.equals(value)
                && !PCMK_FALSE_2.equals(value)) {
                correctValue = false;
            }
        } else if (PARAM_TYPE_INTEGER.equals(type)) {
            final Pattern p = Pattern.compile("^(-?\\d*|(-|\\+)?" + INFINITY_VALUE + ")$");
            if (value != null && !value.isNothingSelected()) {
                final Matcher m = p.matcher(value.getValueForConfig());
                if (!m.matches()) {
                    correctValue = false;
                }
            }
        } else if (PARAM_TYPE_TIME.equals(type)) {
            final Pattern p = Pattern.compile("^-?\\d*(ms|msec|us|usec|s|sec|m|min|h|hr)?$");
            if (value != null && !value.isNothingSelected()) {
                final Matcher m = p.matcher(value.getValueForConfig());
                if (!m.matches()) {
                    correctValue = false;
                }
            }
        } else if ((value == null || value.isNothingSelected()) && required) {
            correctValue = false;
        }
        return correctValue;
    }

    /**
     * Checks global parameter according to its type. Returns false if value
     * does not fit the type.
     */
    public boolean checkGlobalParam(final String param, final Value value) {
        final String type = getGlobalType(param);
        boolean correctValue = true;
        if (PARAM_TYPE_BOOLEAN.equals(type)) {
            if (!PCMK_YES.equals(value) && !PCMK_NO.equals(value)
                && !PCMK_TRUE_VALUE.equals(value)
                && !PCMK_FALSE_VALUE.equals(value)
                && !PCMK_TRUE_2.equals(value)
                && !PCMK_FALSE_2.equals(value)) {

                correctValue = false;
            }
        } else if (PARAM_TYPE_INTEGER.equals(type)) {
            final Pattern p = Pattern.compile("^(-?\\d*|(-|\\+)?" + INFINITY_VALUE + ")$");
            if (value != null && !value.isNothingSelected()) {
                final Matcher m = p.matcher(value.getValueForConfig());
                if (!m.matches()) {
                    correctValue = false;
                }
            }
        } else if (PARAM_TYPE_TIME.equals(type)) {
            final Pattern p = Pattern.compile("^-?\\d*(ms|msec|us|usec|s|sec|m|min|h|hr)?$");
            if (value != null && !value.isNothingSelected()) {
                final Matcher m = p.matcher(value.getValueForConfig());
                if (!m.matches()) {
                    correctValue = false;
                }
            }
        } else if ((value == null || value.isNothingSelected()) && isGlobalRequired(param)) {
            correctValue = false;
        }
        return correctValue;
    }

    private void addMetaAttributeToResourceAgent(final ResourceAgent resourceAgent,
                                                 final String name,
                                                 String newName,
                                                 final boolean masterSlave) {
        if (newName == null) {
            newName = name;
        }
        if (masterSlave) {
            resourceAgent.addMasterParameter(name);
        } else {
            resourceAgent.addParameter(name);
        }
        resourceAgent.setParamIsMetaAttr(name, true);
        resourceAgent.setParamRequired(name, false);
        resourceAgent.setParamPossibleChoices(name, META_ATTR_COMBO_BOX_CHOICES.get(newName));
        resourceAgent.setParamPossibleChoicesMS(name, META_ATTR_POSSIBLE_CHOICES_MASTER_SLAVE.get(newName));
        resourceAgent.setParamShortDesc(name, META_ATTR_SHORT_DESC.get(newName));
        resourceAgent.setParamLongDesc(name, META_ATTR_LONG_DESC.get(newName));
        resourceAgent.setParamDefault(name, StringValue.getValueForConfig(META_ATTR_DEFAULT.get(newName)));
        resourceAgent.setParamType(name, META_ATTR_TYPE.get(newName));
        resourceAgent.setParamPreferred(name, StringValue.getValueForConfig(META_ATTR_PREFERRED.get(newName)));
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
        if (Tools.versionBeforePacemaker(host)) {
            metaAttrParams.put("target_role", TARGET_ROLE_META_ATTR);
            metaAttrParams.put("is_managed", IS_MANAGED_META_ATTR);
        } else {
            metaAttrParams.put(TARGET_ROLE_META_ATTR, null);
            metaAttrParams.put(IS_MANAGED_META_ATTR, null);
        }
        metaAttrParams.put(MIGRATION_THRESHOLD_META_ATTR, null);
        metaAttrParams.put(PRIORITY_META_ATTR, null);
        metaAttrParams.put(MULTIPLE_ACTIVE_META_ATTR, null);
        metaAttrParams.put(ALLOW_MIGRATE_META_ATTR, null);
        final String hbV = host.getHostParser().getHeartbeatVersion();
        final String pcmkV = host.getHostParser().getPacemakerVersion();
        try {
            if (pcmkV != null || Tools.compareVersions(hbV, "2.1.4") >= 0) {
                metaAttrParams.put(RESOURCE_STICKINESS_META_ATTR, null);
                metaAttrParams.put(FAILURE_TIMEOUT_META_ATTR, null);
            }
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("getMetaAttrParameters: " + e.getMessage(), e);
        }
        return metaAttrParams;
    }

    /**
     * Returns meta attribute parameters. The key is always, how the parameter
     * is called in the cluster manager and value how it is stored in the GUI.
     */
    public Map<String, String> getRscDefaultsParameters() {
        if (resourceDefaultsMetaAttrs != null) {
            return resourceDefaultsMetaAttrs;
        }
        resourceDefaultsMetaAttrs = new LinkedHashMap<String, String>();
        if (host == null || Tools.versionBeforePacemaker(host)) {
            /* no rsc defaults in older versions. */
            return resourceDefaultsMetaAttrs;
        }

        for (final String param : getMetaAttrParameters().keySet()) {
            resourceDefaultsMetaAttrs.put(param, getMetaAttrParameters().get(param));
        }
        /* Master / Slave */
        resourceDefaultsMetaAttrs.put(MASTER_MAX_META_ATTR,      null);
        resourceDefaultsMetaAttrs.put(MASTER_NODE_MAX_META_ATTR, null);
        /* Clone */
        resourceDefaultsMetaAttrs.put(CLONE_MAX_META_ATTR,       null);
        resourceDefaultsMetaAttrs.put(CLONE_NODE_MAX_META_ATTR, null);
        resourceDefaultsMetaAttrs.put(NOTIFY_META_ATTR, null);
        resourceDefaultsMetaAttrs.put(GLOBALLY_UNIQUE_META_ATTR, null);
        resourceDefaultsMetaAttrs.put(ORDERED_META_ATTR, null);
        resourceDefaultsMetaAttrs.put(INTERLEAVE_META_ATTR, null);
        return resourceDefaultsMetaAttrs;
    }

    private void parseParameters(final ResourceAgent resourceAgent, final Node parametersNode) {
        final NodeList parameters = parametersNode.getChildNodes();

        for (int i = 0; i < parameters.getLength(); i++) {
            final Node parameterNode = parameters.item(i);
            if (parameterNode.getNodeName().equals("parameter")) {
                final String param = XMLTools.getAttribute(parameterNode, "name");
                final String required = XMLTools.getAttribute(parameterNode, "required");
                resourceAgent.addParameter(param);

                if (required != null && required.equals("1")) {
                    resourceAgent.setParamRequired(param, true);
                }

                /* <longdesc lang="en"> */
                final Node longdescParamNode = XMLTools.getChildNode(parameterNode, "longdesc");
                if (longdescParamNode != null) {
                    final String longDesc = XMLTools.getText(longdescParamNode);
                    resourceAgent.setParamLongDesc(param, Tools.trimText(longDesc));
                }

                /* <shortdesc lang="en"> */
                final Node shortdescParamNode = XMLTools.getChildNode(parameterNode, "shortdesc");
                if (shortdescParamNode != null) {
                    String shortDesc = XMLTools.getText(shortdescParamNode);
                    if (shortDesc.trim().equals("")) {
                        shortDesc = param;
                    }
                    resourceAgent.setParamShortDesc(param, shortDesc);
                }

                /* <content> */
                final Node contentParamNode = XMLTools.getChildNode(parameterNode, "content");
                if (contentParamNode != null) {
                    final String type = XMLTools.getAttribute(contentParamNode, "type");
                    String defaultValue = XMLTools.getAttribute(contentParamNode, "default");
                    if (defaultValue == null && resourceAgent.isStonith()
                        && PARAM_TYPE_BOOLEAN.equals(type)) {
                        defaultValue = PCMK_FALSE_VALUE.getValueForConfig();
                    }
                    if (resourceAgent.isIPaddr() && "nic".equals(param)) {
                        // workaround for default value in IPaddr and IPaddr2
                        defaultValue = "";
                    }
                    if ("force_stop".equals(param) && "0".equals(defaultValue)) {
                        // Workaround, default is "0" and should be false
                        defaultValue = "false";
                    }
                    if ("".equals(defaultValue) && "force_clones".equals(param)) {
                        defaultValue = "false";
                    }
                    if (resourceAgent.isPingService()) {
                        /* workaround: all types are integer in this ras. */
                        resourceAgent.setProbablyClone(true);
                        if ("host_list".equals(param)) {
                            resourceAgent.setParamRequired(param, true);
                        }
                    } else {
                        resourceAgent.setParamType(param, type);
                    }
                    if (resourceAgent.isStonith() && PARAM_TYPE_BOOLEAN.equals(type)
                        && defaultValue == null) {
                    }
                    resourceAgent.setParamDefault(param, defaultValue);
                }
                if (resourceAgent.isStonith() && ("hostlist".equals(param))) {
                    resourceAgent.setParamPossibleChoices(
                            param,
                            stonithHostlistChoices.toArray(new Value[stonithHostlistChoices.size()]));
                }
                final String section = RA_NON_ADVANCED_PARAM.get(resourceAgent.getServiceName(), param);
                if (section != null) {
                    resourceAgent.setSection(param, section);
                }
            }
        }
        if (resourceAgent.isStonith()) {
            /* stonith-timeout */
            resourceAgent.addParameter(STONITH_TIMEOUT_INSTANCE_ATTR);
            resourceAgent.setParamShortDesc(STONITH_TIMEOUT_INSTANCE_ATTR,
                                 Tools.getString("CRMXML.stonith-timeout.ShortDesc"));
            resourceAgent.setParamLongDesc(STONITH_TIMEOUT_INSTANCE_ATTR,
                                Tools.getString("CRMXML.stonith-timeout.LongDesc"));
            resourceAgent.setParamType(STONITH_TIMEOUT_INSTANCE_ATTR, PARAM_TYPE_TIME);
            resourceAgent.setParamDefault(STONITH_TIMEOUT_INSTANCE_ATTR, "");
            /* priority */
            // TODO: priority or stonith-priority?
            resourceAgent.addParameter(STONITH_PRIORITY_INSTANCE_ATTR);

            resourceAgent.setParamShortDesc(STONITH_PRIORITY_INSTANCE_ATTR,
                                 Tools.getString("CRMXML.stonith-priority.ShortDesc"));
            resourceAgent.setParamLongDesc(STONITH_PRIORITY_INSTANCE_ATTR,
                                Tools.getString("CRMXML.stonith-priority.LongDesc"));
            resourceAgent.setParamPossibleChoices(STONITH_PRIORITY_INSTANCE_ATTR,
                                                  new Value[]{new StringValue("0"),
                                                              new StringValue("5"),
                                                              new StringValue("10")});
            resourceAgent.setParamType(STONITH_PRIORITY_INSTANCE_ATTR, PARAM_TYPE_INTEGER);
            resourceAgent.setParamDefault(STONITH_PRIORITY_INSTANCE_ATTR, "0");

            /* pcmk_host_check for stonithd */
            resourceAgent.addParameter(STONITH_PARAM_PCMK_HOST_CHECK);
            resourceAgent.setParamPossibleChoices(STONITH_PARAM_PCMK_HOST_CHECK,
                                                  new Value[]{new StringValue(),
                                                              new StringValue(PCMK_HOST_CHECK_DYNAMIC),
                                                              new StringValue(PCMK_HOST_CHECK_STATIC)});
            resourceAgent.setParamShortDesc(STONITH_PARAM_PCMK_HOST_CHECK,
                                            Tools.getString("CRMXML.pcmk_host_check.ShortDesc"));
            resourceAgent.setParamLongDesc(STONITH_PARAM_PCMK_HOST_CHECK,
                                           Tools.getString("CRMXML.pcmk_host_check.LongDesc"));
            resourceAgent.setParamDefault(STONITH_PARAM_PCMK_HOST_CHECK, PCMK_HOST_CHECK_DYNAMIC);
            resourceAgent.setParamType(STONITH_PARAM_PCMK_HOST_CHECK, PARAM_TYPE_STRING);

            /* pcmk_host_list for stonithd */
            resourceAgent.addParameter(STONITH_PARAM_PCMK_HOST_LIST);
            resourceAgent.setParamShortDesc(STONITH_PARAM_PCMK_HOST_LIST,
                                            Tools.getString("CRMXML.pcmk_host_list.ShortDesc"));
            resourceAgent.setParamLongDesc(STONITH_PARAM_PCMK_HOST_LIST,
                                           Tools.getString("CRMXML.pcmk_host_list.LongDesc"));
            resourceAgent.setParamType(STONITH_PARAM_PCMK_HOST_LIST, PARAM_TYPE_STRING);
            resourceAgent.setParamPossibleChoices(
                                            STONITH_PARAM_PCMK_HOST_LIST,
                                            stonithHostlistChoices.toArray(new Value[stonithHostlistChoices.size()]));

            /* pcmk_host_map for stonithd */
            resourceAgent.addParameter(STONITH_PARAM_PCMK_HOST_MAP);
            resourceAgent.setParamShortDesc(STONITH_PARAM_PCMK_HOST_MAP,
                                            Tools.getString("CRMXML.pcmk_host_map.ShortDesc"));
            resourceAgent.setParamLongDesc(STONITH_PARAM_PCMK_HOST_MAP,
                                           Tools.getString("CRMXML.pcmk_host_map.LongDesc"));
            resourceAgent.setParamType(STONITH_PARAM_PCMK_HOST_MAP, PARAM_TYPE_STRING);
        }
        final Map<String, String> maParams = getMetaAttrParameters();
        for (final String metaAttr : maParams.keySet()) {
            addMetaAttributeToResourceAgent(resourceAgent, metaAttr, maParams.get(metaAttr), false);
        }
    }

    private void parseActionsNode(final ResourceAgent resourceAgent, final Node actionsNode) {
        final NodeList actions = actionsNode.getChildNodes();
        for (int i = 0; i < actions.getLength(); i++) {
            final Node actionNode = actions.item(i);
            if (actionNode.getNodeName().equals("action")) {
                final String name = XMLTools.getAttribute(actionNode, "name");
                if ("status ".equals(name)) {
                    /* workaround for iSCSITarget RA */
                    continue;
                }
                final String depth = XMLTools.getAttribute(actionNode, "depth");

                final Value timeout = parseValue(resourceAgent + ": " + name + " timeout",
                                                 XMLTools.getAttribute(actionNode, "timeout"));

                final Value interval = parseValue(resourceAgent + ": " + name + " interval",
                                                  XMLTools.getAttribute(actionNode, "interval"));

                final Value startDelay = parseValue(resourceAgent + ": " + name + " start-delay",
                        XMLTools.getAttribute(actionNode, "start-delay"));

                final String role = XMLTools.getAttribute(actionNode, "role");
                resourceAgent.addOperationDefault(name, "depth", new StringValue(depth));
                resourceAgent.addOperationDefault(name, "timeout", timeout);
                resourceAgent.addOperationDefault(name, "interval", interval);
                resourceAgent.addOperationDefault(name, "start-delay", startDelay);
                resourceAgent.addOperationDefault(name, "role", new StringValue(role));
            }
        }
        resourceAgent.addOperationDefault("monitor", PARAM_OCF_CHECK_LEVEL, new StringValue(""));
    }

    /** Parses the actions node that is list of values for action param. */
    private void parseStonithActions(final ResourceAgent ra, final Node actionsNode) {
        final NodeList actionNodes = actionsNode.getChildNodes();
        final List<Value> actions = new ArrayList<Value>();
        for (int i = 0; i < actionNodes.getLength(); i++) {
            final Node actionNode = actionNodes.item(i);
            if (actionNode.getNodeName().equals("action")) {
                final String name = XMLTools.getAttribute(actionNode, "name");
                actions.add(new StringValue(name));
            }
        }
        ra.setParamPossibleChoices(FENCING_ACTION_PARAM, actions.toArray(new Value[actions.size()]));
    }

    /**
     * Parses meta-data xml for parameters for service and fills up the hashes
     * "CRM Daemon"s are global config options.
     */
    void parseMetaDataAndFillHashes(final String serviceName,
                                    final String provider,
                                    final String xml,
                                    final boolean masterSlave) {
        final Document document = XMLTools.getXMLDocument(xml);
        if (document == null) {
            return;
        }

        /* get root <resource-agent> */
        final Node raNode = XMLTools.getChildNode(document, "resource-agent");
        if (raNode == null) {
            return;
        }

        /* class */
        String resourceClass = XMLTools.getAttribute(raNode, "class");
        if (resourceClass == null) {
            resourceClass = ResourceAgent.OCF_CLASS_NAME;
        }
        final ResourceAgent resourceAgent = serviceToResourceAgentMap.get(serviceName, provider, resourceClass);
        if (resourceAgent == null) {
            LOG.appWarning("parseMetaData: cannot save meta-data for: "
                           + resourceClass
                           + ':' + provider
                           + ':' + serviceName);
            return;
        }
        if (resourceAgent.isMetaDataLoaded()) {
            return;
        }
        if (ResourceAgent.SERVICE_CLASSES.contains(resourceClass)
            || ResourceAgent.HEARTBEAT_CLASS_NAME.equals(resourceClass)) {
            setLsbResourceAgent(serviceName, resourceClass, resourceAgent);
        } else {
            /* <version> */
            final Node versionNode = XMLTools.getChildNode(raNode, "version");
            if (versionNode != null) {
                resourceAgent.setServiceVersion(XMLTools.getText(versionNode));
            }

            /* <longdesc lang="en"> */
            final Node longdescNode = XMLTools.getChildNode(raNode, "longdesc");
            if (longdescNode != null) {
                resourceAgent.setServiceLongDesc(Tools.trimText(XMLTools.getText(longdescNode)));
            }

            /* <shortdesc lang="en"> */
            final Node shortdescNode = XMLTools.getChildNode(raNode, "shortdesc");
            if (shortdescNode != null) {
                resourceAgent.setServiceShortDesc(XMLTools.getText(shortdescNode));
            }

            /* <parameters> */
            final Node parametersNode = XMLTools.getChildNode(raNode, "parameters");
            if (parametersNode != null) {
                parseParameters(resourceAgent, parametersNode);
            }
            /* <actions> */
            final Node actionsNode = XMLTools.getChildNode(raNode, "actions");
            if (actionsNode != null) {
                if (resourceAgent.isStonith()
                    && resourceAgent.hasParameter(FENCING_ACTION_PARAM)) {
                    parseStonithActions(resourceAgent, actionsNode);
                } else {
                    parseActionsNode(resourceAgent, actionsNode);
                }
            }
            resourceAgent.setProbablyMasterSlave(masterSlave);
        }
        resourceAgent.setMetaDataLoaded(true);
    }

    /** Set resource agent to be used as LSB script. */
    private void setLsbResourceAgent(final String serviceName,
                                     final String raClass,
                                     final ResourceAgent resourceAgent) {
        resourceAgent.setServiceVersion("0.0");
        if (ResourceAgent.LSB_CLASS_NAME.equals(raClass)) {
            resourceAgent.setServiceLongDesc("LSB resource.");
            resourceAgent.setServiceShortDesc("/etc/init.d/" + serviceName);
        } else if (ResourceAgent.SERVICE_CLASSES.contains(raClass)) {
            resourceAgent.setServiceLongDesc(raClass);
            resourceAgent.setServiceShortDesc(serviceName);
        } else if (ResourceAgent.HEARTBEAT_CLASS_NAME.equals(raClass)) {
            resourceAgent.setServiceLongDesc("Heartbeat 1 RA.");
            resourceAgent.setServiceShortDesc("/etc/ha.d/resource.d/" + serviceName);
        }
        for (int i = 1; i < 11; i++) {
            final String param = Integer.toString(i);
            resourceAgent.addParameter(param);
            resourceAgent.setParamLongDesc(param, param);
            resourceAgent.setParamShortDesc(param, param);
            resourceAgent.setParamType(param, "string");
            resourceAgent.setParamDefault(param, "");
        }
        /* <actions> */
        for (final String name : new String[]{"start", "stop", "status", "meta-data"}) {
            resourceAgent.addOperationDefault(name, "timeout", new StringValue("15", CrmXml.getUnitSecond()));
        }
        final String monitorName = "monitor";
        resourceAgent.addOperationDefault(monitorName, "timeout", new StringValue("15", CrmXml.getUnitSecond()));
        resourceAgent.addOperationDefault(monitorName, "interval", new StringValue("15", CrmXml.getUnitSecond()));
        resourceAgent.addOperationDefault(monitorName, "start-delay", new StringValue("15", CrmXml.getUnitSecond()));
        resourceAgent.setProbablyMasterSlave(false);
    }

    /**
     * Parses crm meta data, only to get long descriptions and default values
     * for advanced options.
     * Strange stuff
     *
     * which can be pengine or crmd
     */
    void parseClusterMetaData(
        final String xml) {
        final Document document = XMLTools.getXMLDocument(xml);
        if (document == null) {
            return;
        }

        /* get root <metadata> */
        final Node metadataNode = XMLTools.getChildNode(document, "metadata");
        if (metadataNode == null) {
            return;
        }

        /* get <resource-agent> */
        final NodeList resAgents = metadataNode.getChildNodes();
        final Value[] booleanValues = PCMK_BOOLEAN_VALUES;

        for (int i = 0; i < resAgents.getLength(); i++) {
            final Node resAgentNode = resAgents.item(i);
            if (!resAgentNode.getNodeName().equals("resource-agent")) {
                continue;
            }

            /* <parameters> */
            final Node parametersNode = XMLTools.getChildNode(resAgentNode, "parameters");
            if (parametersNode == null) {
                return;
            }

            final NodeList parameters = parametersNode.getChildNodes();
            for (int j = 0; j < parameters.getLength(); j++) {
                final Node parameterNode = parameters.item(j);
                if (parameterNode.getNodeName().equals("parameter")) {
                    final String param = XMLTools.getAttribute(parameterNode, "name");
                    final String required = XMLTools.getAttribute(parameterNode, "required");
                    if (!globalParams.contains(param)) {
                        globalParams.add(param);
                    }
                    if (required != null && required.equals("1") && !globalRequiredParams.contains(param)) {
                        globalRequiredParams.add(param);
                    }

                    /* <longdesc lang="en"> */
                    final Node longdescParamNode = XMLTools.getChildNode(parameterNode, "longdesc");
                    if (longdescParamNode != null) {
                        final String longDesc = XMLTools.getText(longdescParamNode);
                        globalLongDescMap.put(param, Tools.trimText(longDesc));
                    }

                    /* <content> */
                    final Node contentParamNode = XMLTools.getChildNode(parameterNode, "content");
                    if (contentParamNode != null) {
                        final String type = XMLTools.getAttribute(contentParamNode, "type");
                        String dv = XMLTools.getAttribute(contentParamNode, "default");
                        if ("(null)".equals(dv)) {
                            dv = null;
                        }
                        globalTypeMap.put(param, type);
                        if (!"expected-quorum-votes".equals(param)) {
                            final Value defaultValue;
                            if (PARAM_TYPE_TIME.equals(type)) {
                                final Value v = parseValue(param, dv);
                                if (v == null) {
                                    defaultValue = new StringValue(dv);
                                } else {
                                    defaultValue = v;
                                }
                            } else {
                                defaultValue = new StringValue(dv);
                            }
                            globalDefaultMap.put(param, defaultValue);
                        }
                        if (PARAM_TYPE_BOOLEAN.equals(type)) {
                            globalComboBoxChoices.put(param, booleanValues);
                        }
                        if (PARAM_TYPE_INTEGER.equals(type)) {
                            globalComboBoxChoices.put(param, INTEGER_VALUES);
                        }
                    }
                }
            }
        }
        /* stonith timeout, workaround, because of param type comming wrong
        * from pacemaker */
        globalTypeMap.put("stonith-timeout", PARAM_TYPE_TIME);
    }

    public ResourceAgent getResourceAgent(final String serviceName, final String provider, final String raClass) {
        final ResourceAgent resourceAgent = serviceToResourceAgentMap.get(serviceName, provider, raClass);
        if (resourceAgent == null) {
            final ResourceAgent notInstalledRA = new ResourceAgent(serviceName, provider, raClass);
            if (ResourceAgent.SERVICE_CLASSES.contains(raClass)
                || ResourceAgent.HEARTBEAT_CLASS_NAME.equals(raClass)) {
                setLsbResourceAgent(serviceName, raClass, notInstalledRA);
            } else {
                LOG.appWarning("getResourceAgent: " + raClass
                               + ':' + provider + ':' + serviceName
                               + " RA does not exist");
            }
            serviceToResourceAgentMap.put(serviceName, provider, raClass, notInstalledRA);
            return notInstalledRA;

        }
        return resourceAgent;
    }

    public ResourceAgent getDrbddiskResourceAgent() {
        return drbddiskResourceAgent;
    }

    public ResourceAgent getLinbitDrbdResourceAgent() {
        return linbitDrbdResourceAgent;
    }

    public ResourceAgent getGroupResourceAgent() {
        return groupResourceAgent;
    }

    /** Returns the heartbeat service object of the heartbeat clone set. */
    public ResourceAgent getCloneResourceAgent() {
        return cloneResourceAgent;
    }

    String parseResourceDefaults(final Node rscDefaultsNode,
                                 final Map<String, String> rscDefaultsParams,
                                 final Map<String, String> rscDefaultsParamsNvpairIds) {
        
        /* <meta_attributtes> */
        final Node metaAttrsNode = XMLTools.getChildNode(rscDefaultsNode, "meta_attributes");
        String rscDefaultsId = null;
        if (metaAttrsNode != null) {
            rscDefaultsId = XMLTools.getAttribute(metaAttrsNode, "id");
            final NodeList nvpairsMA;
            if (Tools.versionBeforePacemaker(host)) {
                /* <attributtes> only til 2.1.4 */
                final Node attrsNode = XMLTools.getChildNode(metaAttrsNode, "attributes");
                nvpairsMA = attrsNode.getChildNodes();
            } else {
                nvpairsMA = metaAttrsNode.getChildNodes();
            }
            /* <nvpair...> */
            /* target-role and is-managed */
            for (int l = 0; l < nvpairsMA.getLength(); l++) {
                final Node maNode = nvpairsMA.item(l);
                if (maNode.getNodeName().equals("nvpair")) {
                    final String nvpairId = XMLTools.getAttribute(maNode, "id");
                    final String name = XMLTools.getAttribute(maNode, "name");
                    String value = XMLTools.getAttribute(maNode, "value");
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

    void parseOpDefaults(final Node opDefaultsNode, final Map<String, Value> opDefaultsParams) {
        
        /* <meta_attributtes> */
        final Node metaAttrsNode = XMLTools.getChildNode(opDefaultsNode, "meta_attributes");
        if (metaAttrsNode != null) {
            /* <attributtes> only til 2.1.4 */
            final NodeList nvpairsMA;
            if (Tools.versionBeforePacemaker(host)) {
                final Node attrsNode = XMLTools.getChildNode(metaAttrsNode, "attributes");
                nvpairsMA = attrsNode.getChildNodes();
            } else {
                nvpairsMA = metaAttrsNode.getChildNodes();
            }
            /* <nvpair...> */
            for (int l = 0; l < nvpairsMA.getLength(); l++) {
                final Node maNode = nvpairsMA.item(l);
                if (maNode.getNodeName().equals("nvpair")) {
                    final String name = XMLTools.getAttribute(maNode, "name");
                    final String value = XMLTools.getAttribute(maNode, "value");
                    opDefaultsParams.put(name, parseValue(name, value));
                }
            }
        }
    }

    /** Parses attributes, operations etc. from primitives and clones. */
    private void parseAttributes(final Node resourceNode,
                                 final String crmId,
                                 final Map<String, Map<String, String>> parametersMap,
                                 final Map<String, Map<String, String>> parametersNvpairsIdsMap,
                                 final Map<String, String> resourceInstanceAttrIdMap,
                                 final MultiKeyMap<String, Value> operationsMap,
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
        /* <instance_attributes> */
        final Node instanceAttrNode = XMLTools.getChildNode(resourceNode, "instance_attributes");
        /* <nvpair...> */
        if (instanceAttrNode != null) {
            final String iAId = XMLTools.getAttribute(instanceAttrNode, "id");
            resourceInstanceAttrIdMap.put(crmId, iAId);
            final NodeList nvpairsRes;
            if (Tools.versionBeforePacemaker(host)) {
                /* <attributtes> only til 2.1.4 */
                final Node attrNode = XMLTools.getChildNode(instanceAttrNode, "attributes");
                nvpairsRes = attrNode.getChildNodes();
            } else {
                nvpairsRes = instanceAttrNode.getChildNodes();
            }
            for (int j = 0; j < nvpairsRes.getLength(); j++) {
                final Node optionNode = nvpairsRes.item(j);
                if (optionNode.getNodeName().equals("nvpair")) {
                    final String nvpairId = XMLTools.getAttribute(optionNode, "id");
                    String name = XMLTools.getAttribute(optionNode, "name");
                    final String value = XMLTools.getAttribute(optionNode, "value");
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
        final Node operationsNode = XMLTools.getChildNode(resourceNode, "operations");
        if (operationsNode != null) {
            final String operationsIdRef = XMLTools.getAttribute(operationsNode, "id-ref");
            if (operationsIdRef == null) {
                final String operationsId = XMLTools.getAttribute(operationsNode, "id");
                operationsIdMap.put(crmId, operationsId);
                operationsIdtoCRMId.put(operationsId, crmId);
                final Map<String, String> opIds = new HashMap<String, String>();
                resOpIdsMap.put(crmId, opIds);
                /* <op> */
                final NodeList ops = operationsNode.getChildNodes();
                for (int k = 0; k < ops.getLength(); k++) {
                    final Node opNode = ops.item(k);
                    if (opNode.getNodeName().equals("op")) {
                        final String opId = XMLTools.getAttribute(opNode, "id");
                        final String name = XMLTools.getAttribute(opNode, "name");
                        final String timeout = XMLTools.getAttribute(opNode, "timeout");
                        final String interval = XMLTools.getAttribute(opNode, "interval");
                        final String startDelay = XMLTools.getAttribute(opNode, "start-delay");

                        operationsMap.put(crmId, name, "interval", parseValue("interval", interval));
                        operationsMap.put(crmId, name, "timeout", parseValue("timeout", timeout));
                        operationsMap.put(crmId, name, "start-delay", parseValue("startDelay", startDelay));

                        opIds.put(name, opId);
                        if ("monitor".equals(name)) {
                            final String checkLevel = parseCheckLevelMonitorAttribute(opNode);
                            operationsMap.put(crmId, name, PARAM_OCF_CHECK_LEVEL, new StringValue(checkLevel));
                        }
                    }
                }
            } else {
                operationsIdRefs.put(crmId, operationsIdRef);
            }
        }

        /* <meta_attributtes> */
        final Node metaAttrsNode = XMLTools.getChildNode(resourceNode, "meta_attributes");
        if (metaAttrsNode != null) {
            final String metaAttrsIdRef = XMLTools.getAttribute(metaAttrsNode, "id-ref");
            if (metaAttrsIdRef == null) {
                final String metaAttrsId = XMLTools.getAttribute(metaAttrsNode, "id");
                metaAttrsIdMap.put(crmId, metaAttrsId);
                metaAttrsIdToCRMId.put(metaAttrsId, crmId);
                /* <attributtes> only til 2.1.4 */
                final NodeList nvpairsMA;
                if (Tools.versionBeforePacemaker(host)) {
                    final Node attrsNode = XMLTools.getChildNode(metaAttrsNode, "attributes");
                    nvpairsMA = attrsNode.getChildNodes();
                } else {
                    nvpairsMA = metaAttrsNode.getChildNodes();
                }
                /* <nvpair...> */
                /* target-role and is-managed */
                for (int l = 0; l < nvpairsMA.getLength(); l++) {
                    final Node maNode = nvpairsMA.item(l);
                    if (maNode.getNodeName().equals("nvpair")) {
                        final String nvpairId = XMLTools.getAttribute(maNode, "id");
                        final String name = XMLTools.getAttribute(maNode, "name");
                        String value = XMLTools.getAttribute(maNode, "value");
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

    /** OCF_CHECK_LEVEL */
    private String parseCheckLevelMonitorAttribute(final Node opNode) {
        final Node iaNode = XMLTools.getChildNode(opNode, "instance_attributes");
        if (iaNode == null) {
            return "";
        }
        final Node nvpairNode = XMLTools.getChildNode(iaNode, "nvpair");
        if (nvpairNode == null) {
            return "";
        }
        final String name = XMLTools.getAttribute(nvpairNode, "name");
        final String value = XMLTools.getAttribute(nvpairNode, "value");
        if (PARAM_OCF_CHECK_LEVEL.equals(name)) {
            return value;
        } else {
            LOG.appWarning("parseCheckLevel: unexpected instance attribute: " + name + ' ' + value);
            return "";
        }
    }

    private void parseGroupNode(final Node groupNode,
                                final Collection<String> resList,
                                final Map<String, List<String>> groupsToResourcesMap,
                                final Map<String, Map<String, String>> parametersMap,
                                final Map<String, ResourceAgent> resourceTypeMap,
                                final Map<String, Map<String, String>> parametersNvpairsIdsMap,
                                final Map<String, String> resourceInstanceAttrIdMap,
                                final MultiKeyMap<String, Value> operationsMap,
                                final Map<String, String> metaAttrsIdMap,
                                final Map<String, String> operationsIdMap,
                                final Map<String, Map<String, String>> resOpIdsMap,
                                final Map<String, String> operationsIdRefs,
                                final Map<String, String> operationsIdtoCRMId,
                                final Map<String, String> metaAttrsIdRefs,
                                final Map<String, String> metaAttrsIdToCRMId) {
        final NodeList primitives = groupNode.getChildNodes();
        final String groupId = XMLTools.getAttribute(groupNode, "id");
        final Map<String, String> params = new HashMap<String, String>();
        parametersMap.put(groupId, params);
        final Map<String, String> nvpairIds = new HashMap<String, String>();
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
                parsePrimitiveNode(primitiveNode,
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
        final Node metaAttrsNode = XMLTools.getChildNode(groupNode, "meta_attributes");
        if (metaAttrsNode != null) {
            final String metaAttrsIdRef = XMLTools.getAttribute(metaAttrsNode, "id-ref");
            if (metaAttrsIdRef == null) {
                final String metaAttrsId = XMLTools.getAttribute(metaAttrsNode, "id");
                metaAttrsIdMap.put(groupId, metaAttrsId);
                metaAttrsIdToCRMId.put(metaAttrsId, groupId);
                /* <attributtes> only til 2.1.4 */
                final NodeList nvpairsMA;
                if (Tools.versionBeforePacemaker(host)) {
                    final Node attrsNode = XMLTools.getChildNode(metaAttrsNode, "attributes");
                    nvpairsMA = attrsNode.getChildNodes();
                } else {
                    nvpairsMA = metaAttrsNode.getChildNodes();
                }
                /* <nvpair...> */
                /* target-role and is-managed */
                for (int l = 0; l < nvpairsMA.getLength(); l++) {
                    final Node maNode = nvpairsMA.item(l);
                    if (maNode.getNodeName().equals("nvpair")) {
                        final String nvpairId = XMLTools.getAttribute(maNode, "id");
                        String name = XMLTools.getAttribute(maNode, "name");
                        String value = XMLTools.getAttribute(maNode, "value");
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

    private void parsePrimitiveNode(final Node primitiveNode,
                                    final Collection<String> groupResList,
                                    final Map<String, ResourceAgent> resourceTypeMap,
                                    final Map<String, Map<String, String>> parametersMap,
                                    final Map<String, Map<String, String>> parametersNvpairsIdsMap,
                                    final Map<String, String> resourceInstanceAttrIdMap,
                                    final MultiKeyMap<String, Value> operationsMap,
                                    final Map<String, String> metaAttrsIdMap,
                                    final Map<String, String> operationsIdMap,
                                    final Map<String, Map<String, String>> resOpIdsMap,
                                    final Map<String, String> operationsIdRefs,
                                    final Map<String, String> operationsIdtoCRMId,
                                    final Map<String, String> metaAttrsIdRefs,
                                    final Map<String, String> metaAttrsIdToCRMId) {
        final String templateId = XMLTools.getAttribute(primitiveNode, "template");
        final String crmId = XMLTools.getAttribute(primitiveNode, "id");
        if (templateId != null) {
            LOG.info("parsePrimitive: templates not implemented, ignoring: " + crmId + '/' + templateId);
            return;
        }
        final String raClass = XMLTools.getAttribute(primitiveNode, "class");
        String provider = XMLTools.getAttribute(primitiveNode, "provider");
        if (provider == null) {
            provider = ResourceAgent.HEARTBEAT_PROVIDER;
        }
        final String type = XMLTools.getAttribute(primitiveNode, "type");
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
                        ResourceAgent.STONITH_CLASS_NAME.equals(raClass));
    }

    private Map<String, String> parseAllocationScores(final NodeList scores) {
        final Map<String, String> allocationScores = new LinkedHashMap<String, String>();
        for (int i = 0; i < scores.getLength(); i++) {
            final Node scoreNode = scores.item(i);
            if (scoreNode.getNodeName().equals("score")) {
                final String h = XMLTools.getAttribute(scoreNode, "host");
                final String score = XMLTools.getAttribute(scoreNode, "score");
                allocationScores.put(h, score);
            }
        }
        return allocationScores;
    }

    /** Returns a hash with resource information. (running_on) */
    Map<String, ResourceStatus> parseResStatus(final String resStatus) {
        final Map<String, ResourceStatus> resStatusMap = new HashMap<String, ResourceStatus>();
        final Document document = XMLTools.getXMLDocument(resStatus);
        if (document == null) {
            return null;
        }

        /* get root <resource_status> */
        final Node statusNode = XMLTools.getChildNode(document, "resource_status");
        if (statusNode == null) {
            return null;
        }
        /*      <resource...> */
        final NodeList resources = statusNode.getChildNodes();
        for (int i = 0; i < resources.getLength(); i++) {
            final Node resourceNode = resources.item(i);
            if (resourceNode.getNodeName().equals("resource")) {
                final String id = XMLTools.getAttribute(resourceNode, "id");
                final String isManaged = XMLTools.getAttribute(resourceNode, "managed");
                final NodeList statusList = resourceNode.getChildNodes();
                boolean managed = false;
                if ("managed".equals(isManaged)) {
                    managed = true;
                }
                Map<String, String> allocationScores = new HashMap<String, String>();
                List<String> runningOnList = null;
                List<String> masterOnList = null;
                List<String> slaveOnList = null;
                for (int j = 0; j < statusList.getLength(); j++) {
                    final Node setNode = statusList.item(j);
                    if (TARGET_ROLE_STARTED.equalsIgnoreCase(
                        setNode.getNodeName())) {
                        final String node = XMLTools.getText(setNode);
                        if (runningOnList == null) {
                            runningOnList = new ArrayList<String>();
                        }
                        runningOnList.add(node);
                    } else if (TARGET_ROLE_MASTER.equalsIgnoreCase(
                        setNode.getNodeName())) {
                        final String node = XMLTools.getText(setNode);
                        if (masterOnList == null) {
                            masterOnList = new ArrayList<String>();
                        }
                        masterOnList.add(node);
                    } else if (TARGET_ROLE_SLAVE.equalsIgnoreCase(
                        setNode.getNodeName())) {
                        final String node = XMLTools.getText(setNode);
                        if (slaveOnList == null) {
                            slaveOnList = new ArrayList<String>();
                        }
                        slaveOnList.add(node);
                    } else if ("scores".equals(setNode.getNodeName())) {
                        allocationScores = parseAllocationScores(setNode.getChildNodes());
                    }
                }
                resStatusMap.put(id, new ResourceStatus(runningOnList,
                                                   masterOnList,
                                                   slaveOnList,
                                                   allocationScores,
                                                   managed));
            }
        }
        return resStatusMap;
    }

    private void parseTransientAttributes(final String uname,
                                          final Node transientAttrNode,
                                          final Table<String, String, String> failedMap,
                                          final Table<String, String, Set<String>> failedClonesMap,
                                          final Map<String, String> pingCountMap) {
        /* <instance_attributes> */
        final Node instanceAttrNode = XMLTools.getChildNode(transientAttrNode, "instance_attributes");
        /* <nvpair...> */
        if (instanceAttrNode != null) {
            final NodeList nvpairsRes;
            if (Tools.versionBeforePacemaker(host)) {
                /* <attributtes> only til 2.1.4 */
                final Node attrNode = XMLTools.getChildNode(instanceAttrNode, "attributes");
                nvpairsRes = attrNode.getChildNodes();
            } else {
                nvpairsRes = instanceAttrNode.getChildNodes();
            }
            for (int j = 0; j < nvpairsRes.getLength(); j++) {
                final Node optionNode = nvpairsRes.item(j);
                if (optionNode.getNodeName().equals("nvpair")) {
                    final String name = XMLTools.getAttribute(optionNode, "name");
                    final String value = XMLTools.getAttribute(optionNode, "value");
                    /* TODO: last-failure-" */
                    if ("pingd".equals(name)) {
                        pingCountMap.put(uname, value);
                    } else if (name.indexOf(FAIL_COUNT_PREFIX) == 0) {
                        final String resId = name.substring(FAIL_COUNT_PREFIX.length());
                        final String unameLowerCase = uname.toLowerCase(Locale.US);
                        failedMap.put(unameLowerCase, resId, value);
                        final Pattern p = Pattern.compile("(.*):(\\d+)$");
                        final Matcher m = p.matcher(resId);
                        if (m.matches()) {
                            final String crmId = m.group(1);
                            Set<String> clones = failedClonesMap.get(unameLowerCase, crmId);
                            if (clones == null) {
                                clones = new LinkedHashSet<String>();
                                failedClonesMap.put(unameLowerCase, crmId, clones);
                            }
                            clones.add(m.group(2));
                            failedMap.put(uname.toLowerCase(Locale.US), crmId, value);
                        }
                    }
                }
            }
        }
    }

    /** Parses node, to get info like if it is in stand by. */
    void parseNode(final String node, final Node nodeNode, final Table<String ,String, String> nodeParametersMap) {
        /* <instance_attributes> */
        final Node instanceAttrNode = XMLTools.getChildNode(nodeNode, "instance_attributes");
        /* <nvpair...> */
        if (instanceAttrNode != null) {
            final NodeList nvpairsRes;
            if (Tools.versionBeforePacemaker(host)) {
                /* <attributtes> only til 2.1.4 */
                final Node attrNode = XMLTools.getChildNode(instanceAttrNode, "attributes");
                nvpairsRes = attrNode.getChildNodes();
            } else {
                nvpairsRes = instanceAttrNode.getChildNodes();
            }
            for (int j = 0; j < nvpairsRes.getLength(); j++) {
                final Node optionNode = nvpairsRes.item(j);
                if (optionNode.getNodeName().equals("nvpair")) {
                    final String name = XMLTools.getAttribute(optionNode, "name");
                    final String value = XMLTools.getAttribute(optionNode, "value");
                    nodeParametersMap.put(node.toLowerCase(Locale.US), name, value);
                }
            }
        }
    }

    private void parseResourceSets(final Node node,
                                   final String colId,
                                   final String ordId,
                                   final Collection<RscSet> rscSets,
                                   final List<RscSetConnectionData> rscSetConnections) {
        final NodeList nodes = node.getChildNodes();
        RscSet prevRscSet = null;
        int rscSetCount = 0;
        int ordPos = 0;
        int colPos = 0;
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node rscSetNode = nodes.item(i);
            if (rscSetNode.getNodeName().equals("resource_set")) {
                final String id = XMLTools.getAttribute(rscSetNode, "id");
                final String sequential = XMLTools.getAttribute(rscSetNode, "sequential");
                final String requireAll = XMLTools.getAttribute(rscSetNode, REQUIRE_ALL_ATTR);
                final String orderAction = XMLTools.getAttribute(rscSetNode, "action");
                final String colocationRole = XMLTools.getAttribute(rscSetNode, "role");
                final NodeList rscNodes = rscSetNode.getChildNodes();
                final List<String> rscIds = new ArrayList<String>();
                for (int j = 0; j < rscNodes.getLength(); j++) {
                    final Node rscRefNode = rscNodes.item(j);
                    if (rscRefNode.getNodeName().equals("resource_ref")) {
                        final String rscId = XMLTools.getAttribute(rscRefNode, "id");
                        rscIds.add(rscId);
                    }
                }
                final RscSet rscSet = new RscSet(id, rscIds, sequential, requireAll, orderAction, colocationRole);
                rscSets.add(rscSet);
                if (prevRscSet != null) {
                    final RscSetConnectionData rscSetConnectionData;
                    if (colId == null) {
                        /* order */
                        rscSetConnectionData = new RscSetConnectionData(prevRscSet, rscSet, ordId, ordPos, false);
                        ordPos++;
                        rscSetConnections.add(0, rscSetConnectionData);
                    } else {
                        /* colocation */
                        rscSetConnectionData = new RscSetConnectionData(rscSet, prevRscSet, colId, colPos, true);
                        colPos++;
                        rscSetConnections.add(rscSetConnectionData);
                    }
                }
                prevRscSet = rscSet;
                rscSetCount++;
            }
        }
        if (rscSetCount == 1) {
            /* just one, dangling */
            final RscSetConnectionData rscSetConnectionData;
            if (colId == null) {
                /* order */
                rscSetConnectionData = new RscSetConnectionData(prevRscSet, null, ordId, ordPos, false);
            } else {
                /* colocation */
                rscSetConnectionData = new RscSetConnectionData(prevRscSet, null, colId, colPos, true);
            }
            rscSetConnections.add(rscSetConnectionData);
        }
    }

    /** Returns CibQuery object with information from the cib node. */
    CibQuery parseCibQuery(final String query) {
        final Document document = XMLTools.getXMLDocument(query);
        final CibQuery cibQueryData = new CibQuery();
        if (document == null) {
            LOG.appWarning("parseCibQuery: cib error: " + query);
            return cibQueryData;
        }
        /* get root <pacemaker> */
        final Node pcmkNode = XMLTools.getChildNode(document, "pcmk");
        if (pcmkNode == null) {
            LOG.appWarning("parseCibQuery: there is no pcmk node");
            return cibQueryData;
        }

        /* get fenced nodes */
        final Set<String> fencedNodes = new HashSet<String>();
        final Node fencedNode = XMLTools.getChildNode(pcmkNode, "fenced");
        if (fencedNode != null) {
            final NodeList nodes = fencedNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node hostNode = nodes.item(i);
                if (hostNode.getNodeName().equals("node")) {
                    final String h = XMLTools.getText(hostNode);
                    if (h != null) {
                        fencedNodes.add(h.toLowerCase(Locale.US));
                    }
                }
            }
        }

        /* get <cib> */
        final Node cibNode = XMLTools.getChildNode(pcmkNode, "cib");
        if (cibNode == null) {
            LOG.appWarning("parseCibQuery: there is no cib node");
            return cibQueryData;
        }
        /* Designated Co-ordinator */
        final String dcUuid = XMLTools.getAttribute(cibNode, "dc-uuid");
        //TODO: more attributes are here

        /* <configuration> */
        final Node confNode = XMLTools.getChildNode(cibNode, "configuration");
        if (confNode == null) {
            LOG.appWarning("parseCibQuery: there is no configuration node");
            return cibQueryData;
        }

        /* <rsc_defaults> */
        final Node rscDefaultsNode = XMLTools.getChildNode(confNode, "rsc_defaults");
        String rscDefaultsId = null;
        final Map<String, String> rscDefaultsParams = new HashMap<String, String>();
        final Map<String, String> rscDefaultsParamsNvpairIds = new HashMap<String, String>();
        if (rscDefaultsNode != null) {
            rscDefaultsId = parseResourceDefaults(rscDefaultsNode, rscDefaultsParams, rscDefaultsParamsNvpairIds);
        }

        /* <op_defaults> */
        final Node opDefaultsNode = XMLTools.getChildNode(confNode, "op_defaults");
        final Map<String, Value> opDefaultsParams = new HashMap<String, Value>();
        if (opDefaultsNode != null) {
            parseOpDefaults(opDefaultsNode, opDefaultsParams);
        }
        
        
        /* <crm_config> */
        final Node crmConfNode = XMLTools.getChildNode(confNode, "crm_config");
        if (crmConfNode == null) {
            LOG.appWarning("parseCibQuery: there is no crm_config node");
            return cibQueryData;
        }

        /*      <cluster_property_set> */
        final Node cpsNode = XMLTools.getChildNode(crmConfNode, "cluster_property_set");
        if (cpsNode == null) {
            LOG.appWarning("parseCibQuery: there is no cluster_property_set node");
        } else {
            final NodeList nvpairs;
            if (Tools.versionBeforePacemaker(host)) {
                /* <attributtes> only til 2.1.4 */
                final Node attrNode = XMLTools.getChildNode(cpsNode, "attributes");
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
                    final String name = XMLTools.getAttribute(optionNode, "name");
                    final String value = XMLTools.getAttribute(optionNode, "value");
                    crmConfMap.put(name, value);
                }
            }
            cibQueryData.setCrmConfig(crmConfMap);
        }

        /* <nodes> */
        /* xml node with cluster node make stupid variable names, but let's
        * keep the convention. */
        String dc = null;
        final Table<String, String, String> nodeParametersMap = HashBasedTable.create();
        final Node nodesNode = XMLTools.getChildNode(confNode, "nodes");
        final Map<String, String> nodeOnline = new HashMap<String, String>();
        final Map<String, String> nodeID = new HashMap<String, String>();
        if (nodesNode != null) {
            final NodeList nodes = nodesNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node nodeNode = nodes.item(i);
                if (nodeNode.getNodeName().equals("node")) {
                    /* TODO: doing nothing with the info, just getting the dc,
                    * for now.
                    */
                    final String id = XMLTools.getAttribute(nodeNode, "id");
                    final String uname = XMLTools.getAttribute(nodeNode, "uname");
                    if (!nodeID.containsKey(uname)) {
                        nodeID.put(uname, id);
                    }
                    if (dcUuid != null && dcUuid.equals(id)) {
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
        final Node resourcesNode = XMLTools.getChildNode(confNode, "resources");
        if (resourcesNode == null) {
            LOG.appWarning("parseCibQuery: there is no resources node");
            return cibQueryData;
        }
        /*      <primitive> */
        final Map<String, Map<String, String>> parametersMap = new HashMap<String, Map<String, String>>();
        final Map<String, Map<String, String>> parametersNvpairsIdsMap = new HashMap<String, Map<String, String>>();
        final Map<String, ResourceAgent> resourceTypeMap = new HashMap<String, ResourceAgent>();
        final Set<String> orphanedList = new HashSet<String>();
        /* host -> inLRMList list */
        final Map<String, Set<String>> inLRMList = new HashMap<String, Set<String>>();
        final Map<String, String> resourceInstanceAttrIdMap = new HashMap<String, String>();
        final MultiKeyMap<String, Value> operationsMap = new MultiKeyMap<String, Value>();
        final Map<String, String> metaAttrsIdMap = new HashMap<String, String>();
        final Map<String, String> operationsIdMap = new HashMap<String, String>();
        final Map<String, Map<String, String>> resOpIdsMap = new HashMap<String, Map<String, String>>();
        /* must be linked, so that clone from group is before the group itself.
        */
        final Map<String, List<String>> groupsToResourcesMap = new LinkedHashMap<String, List<String>>();
        final Map<String, String> cloneToResourceMap = new HashMap<String, String>();
        final List<String> masterList = new ArrayList<String>();
        final Table<String, String, String> failedMap = HashBasedTable.create();
        final Table<String, String, Set<String>> failedClonesMap = HashBasedTable.create();
        final Map<String, String> pingCountMap = new HashMap<String, String>();
        groupsToResourcesMap.put("none", new ArrayList<String>());

        final NodeList primitivesGroups = resourcesNode.getChildNodes();
        final Map<String, String> operationsIdRefs = new HashMap<String, String>();
        final Map<String, String> operationsIdtoCRMId = new HashMap<String, String>();
        final Map<String, String> metaAttrsIdRefs = new HashMap<String, String>();
        final Map<String, String> metaAttrsIdToCRMId = new HashMap<String, String>();
        for (int i = 0; i < primitivesGroups.getLength(); i++) {
            final Node primitiveGroupNode = primitivesGroups.item(i);
            final String nodeName = primitiveGroupNode.getNodeName();
            if ("primitive".equals(nodeName)) {
                final List<String> resList = groupsToResourcesMap.get("none");
                parsePrimitiveNode(primitiveGroupNode,
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
                parseGroupNode(primitiveGroupNode,
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
            } else if ("master".equals(nodeName) || "master_slave".equals(nodeName) || "clone".equals(nodeName)) {
                final NodeList primitives = primitiveGroupNode.getChildNodes();
                final String cloneId = XMLTools.getAttribute(primitiveGroupNode, "id");
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
                        parsePrimitiveNode(primitiveNode,
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
                        parseGroupNode(primitiveNode,
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
                    if ("master".equals(nodeName) || "master_slave".equals(nodeName)) {
                        masterList.add(cloneId);
                    }
                }
            }
        }

        /* operationsRefs crm id -> crm id */
        final Map<String, String> operationsRefs = new HashMap<String, String>();
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
        final Map<String, ColocationData> colocationIdMap = new LinkedHashMap<String, ColocationData>();
        final Map<String, List<ColocationData>> colocationRscMap = new HashMap<String, List<ColocationData>>();
        final Map<String, OrderData> orderIdMap = new LinkedHashMap<String, OrderData>();
        final Map<String, List<RscSet>> orderIdRscSetsMap = new HashMap<String, List<RscSet>>();
        final Map<String, List<RscSet>> colocationIdRscSetsMap = new HashMap<String, List<RscSet>>();
        final List<RscSetConnectionData> rscSetConnections = new ArrayList<RscSetConnectionData>();
        final Map<String, List<OrderData>> orderRscMap = new HashMap<String, List<OrderData>>();
        final Map<String, Map<String, HostLocation>> locationMap = new HashMap<String, Map<String, HostLocation>>();
        final Map<String, HostLocation> pingLocationMap = new HashMap<String, HostLocation>();
        final Map<String, List<String>> locationsIdMap = new HashMap<String, List<String>>();
        final Table<String, String, String> resHostToLocIdMap = HashBasedTable.create();
        
        final Map<String, String> resPingToLocIdMap = new HashMap<String, String>();
        final Node constraintsNode = XMLTools.getChildNode(confNode, "constraints");
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
            if (Tools.versionBeforePacemaker(host)) {
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
                    final String colId = XMLTools.getAttribute(constraintNode, "id");
                    final String rsc = XMLTools.getAttribute(constraintNode, rscString);
                    final String withRsc = XMLTools.getAttribute(constraintNode, withRscString);
                    if (rsc == null || withRsc == null) {
                        final List<RscSet> rscSets = new ArrayList<RscSet>();
                        parseResourceSets(constraintNode, colId, null, rscSets, rscSetConnections);
                        colocationIdRscSetsMap.put(colId, rscSets);
                    }
                    final String rscRole = XMLTools.getAttribute(constraintNode, rscRoleString);
                    final String withRscRole = XMLTools.getAttribute(constraintNode, withRscRoleString);
                    final String score = XMLTools.getAttribute(constraintNode, SCORE_CONSTRAINT_PARAM);
                    final ColocationData colocationData =
                                                 new ColocationData(colId, rsc, withRsc, rscRole, withRscRole, score);
                    colocationIdMap.put(colId, colocationData);
                    List<ColocationData> withs = colocationRscMap.get(rsc);
                    if (withs == null) {
                        withs = new ArrayList<ColocationData>();
                    }
                    withs.add(colocationData);
                    colocationRscMap.put(rsc, withs);
                } else if (constraintNode.getNodeName().equals("rsc_order")) {
                    String rscFirst = XMLTools.getAttribute(constraintNode, firstString);
                    String rscThen = XMLTools.getAttribute(constraintNode, thenString);
                    final String ordId = XMLTools.getAttribute(constraintNode, "id");
                    if (rscFirst == null || rscThen == null) {
                        final List<RscSet> rscSets = new ArrayList<RscSet>();
                        parseResourceSets(constraintNode, null, ordId, rscSets, rscSetConnections);
                        orderIdRscSetsMap.put(ordId, rscSets);
                    }
                    final String score = XMLTools.getAttribute(constraintNode, SCORE_CONSTRAINT_PARAM);
                    final String symmetrical = XMLTools.getAttribute(constraintNode, "symmetrical");
                    String firstAction = XMLTools.getAttribute(constraintNode, firstActionString);
                    String thenAction = XMLTools.getAttribute(constraintNode, thenActionString);
                    final String type = XMLTools.getAttribute(constraintNode, "type");
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
                } else if ("rsc_location".equals(constraintNode.getNodeName())) {
                    final String locId = XMLTools.getAttribute(constraintNode, "id");
                    final String node  = XMLTools.getAttribute(constraintNode, "node");
                    final String rsc   = XMLTools.getAttribute(constraintNode, "rsc");
                    final String score = XMLTools.getAttribute(constraintNode, SCORE_CONSTRAINT_PARAM);

                    List<String> locs = locationsIdMap.get(rsc);
                    if (locs == null) {
                        locs = new ArrayList<String>();
                        locationsIdMap.put(rsc, locs);
                    }
                    Map<String, HostLocation> hostScoreMap = locationMap.get(rsc);
                    if (hostScoreMap == null) {
                        hostScoreMap = new HashMap<String, HostLocation>();
                        locationMap.put(rsc, hostScoreMap);
                    }
                    final String role = null; // TODO
                    if (node != null) {
                        resHostToLocIdMap.put(rsc, node.toLowerCase(Locale.US), locId);
                        if (score != null) {
                            hostScoreMap.put(node.toLowerCase(Locale.US), new HostLocation(score, "eq", null, role));
                        }
                    }
                    locs.add(locId);
                    final Node ruleNode = XMLTools.getChildNode(constraintNode, "rule");
                    if (ruleNode != null) {
                        final String score2 = XMLTools.getAttribute(ruleNode, SCORE_CONSTRAINT_PARAM);
                        final String booleanOp = XMLTools.getAttribute(ruleNode, "boolean-op");
                        // TODO: I know only "and", ignoring everything we
                        // don't know.
                        final Node expNode = XMLTools.getChildNode(ruleNode, "expression");
                        if (expNode != null
                            && "expression".equals(expNode.getNodeName())) {
                            final String attr = XMLTools.getAttribute(expNode, "attribute");
                            final String op = XMLTools.getAttribute(expNode, "operation");
                            final String value = XMLTools.getAttribute(expNode, "value");
                            if ((booleanOp == null || "and".equals(booleanOp))
                                && "#uname".equals(attr)
                                && value != null) {
                                hostScoreMap.put(value.toLowerCase(Locale.US),
                                                 new HostLocation(score2, op, null, role));
                                resHostToLocIdMap.put(rsc, value.toLowerCase(Locale.US), locId);
                            } else if ((booleanOp == null || "and".equals(booleanOp)) && "pingd".equals(attr)) {
                                pingLocationMap.put(rsc, new HostLocation(score2, op, value, null));
                                resPingToLocIdMap.put(rsc, locId);
                            } else {
                                LOG.appWarning("parseCibQuery: could not parse rsc_location: " + locId);
                            }
                        }
                    }
                }
            }
        }

        /* <status> */
        final Node statusNode = XMLTools.getChildNode(cibNode, "status");
        final Set<String> nodePending = new HashSet<String>();
        if (statusNode != null) {
            /* <node_state ...> */
            final NodeList nodes = statusNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node nodeStateNode = nodes.item(i);
                if ("node_state".equals(nodeStateNode.getNodeName())) {
                    final String uname = XMLTools.getAttribute(nodeStateNode, "uname");
                    final String id = XMLTools.getAttribute(nodeStateNode, "id");
                    if (uname == null || !id.equals(nodeID.get(uname))) {
                        LOG.appWarning("parseCibQuery: skipping " + uname + ' ' + id);
                        continue;
                    }
                    final String join = XMLTools.getAttribute(nodeStateNode, "join");
                    final String inCCM = XMLTools.getAttribute(nodeStateNode, "in_ccm");
                    final String crmd = XMLTools.getAttribute(nodeStateNode, "crmd");
                    if ("member".equals(join) && "true".equals(inCCM) && !"offline".equals(crmd)) {
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
                            parseTransientAttributes(uname, nodeStateChild, failedMap, failedClonesMap, pingCountMap);
                        }
                    }
                    final List<String> resList =
                        groupsToResourcesMap.get("none");
                    for (int j = 0; j < nodeStates.getLength(); j++) {
                        final Node nodeStateChild = nodeStates.item(j);
                        if ("lrm".equals(nodeStateChild.getNodeName())) {
                            parseLrmResources(uname.toLowerCase(Locale.US),
                                    nodeStateChild,
                                    resList,
                                    resourceTypeMap,
                                    parametersMap,
                                    inLRMList,
                                    orphanedList,
                                    failedClonesMap);
                        }
                    }
                }
            }
        }
        cibQueryData.setDC(dc);
        cibQueryData.setNodeParameters(nodeParametersMap);
        cibQueryData.setResourceParameters(parametersMap);
        cibQueryData.setResourceParametersNvpairsIds(parametersNvpairsIdsMap);
        cibQueryData.setResourceType(resourceTypeMap);
        cibQueryData.setInLRM(inLRMList);
        cibQueryData.setOrphaned(orphanedList);
        cibQueryData.setResourceInstanceAttrId(resourceInstanceAttrIdMap);

        cibQueryData.setColocationRsc(colocationRscMap);
        cibQueryData.setColocationId(colocationIdMap);

        cibQueryData.setOrderId(orderIdMap);
        cibQueryData.setOrderIdRscSets(orderIdRscSetsMap);
        cibQueryData.setColocationIdRscSets(colocationIdRscSetsMap);
        cibQueryData.setRscSetConnections(rscSetConnections);
        cibQueryData.setOrderRsc(orderRscMap);

        cibQueryData.setLocations(locationMap);
        cibQueryData.setPingLocations(pingLocationMap);
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
        cibQueryData.setNodeFailedCount(failedMap);
        cibQueryData.setResourceFailedCloneIds(failedClonesMap);
        cibQueryData.setNodePingCount(pingCountMap);
        cibQueryData.setRscDefaultsId(rscDefaultsId);
        cibQueryData.setRscDefaultsParams(rscDefaultsParams);
        cibQueryData.setRscDefaultsParamsNvpairIds(rscDefaultsParamsNvpairIds);
        cibQueryData.setOpDefaultsParams(opDefaultsParams);
        cibQueryData.setFencedNodes(fencedNodes);
        return cibQueryData;
    }

    public String[] getOrderParameters() {
        if (orderParams != null) {
            return orderParams.toArray(new String[orderParams.size()]);
        }
        return null;
    }

    public String[] getResourceSetOrderParameters() {
        if (resourceSetOrderParams != null) {
            return resourceSetOrderParams.toArray(new String[resourceSetOrderParams.size()]);
        }
        return null;
    }

    /** Returns order parameters for resource sets. (Shown when an edge
     * is clicked, resource_set tag). */
    public String[] getRscSetOrdConnectionParameters() {
        if (resourceSetOrderConnectionParams != null) {
            return resourceSetOrderConnectionParams.toArray(new String[resourceSetOrderConnectionParams.size()]);
        }
        return null;
    }

    public boolean isOrderRequired(final String param) {
        return orderRequiredParams.contains(param);
    }

    public String getOrderParamShortDesc(final String param) {
        String shortDesc = paramOrderShortDescMap.get(param);
        if (shortDesc == null) {
            shortDesc = param;
        }
        return shortDesc;
    }

    public String getOrderParamLongDesc(final String param) {
        final String shortDesc = getOrderParamShortDesc(param);
        String longDesc = paramOrderLongDescMap.get(param);
        if (longDesc == null) {
            longDesc = "";
        }
        return Tools.html("<b>" + shortDesc + "</b>\n" + longDesc);
    }

    /**
     * Returns type of a order parameter. It can be string, integer, boolean...
     */
    public String getOrderParamType(final String param) {
        return paramOrderTypeMap.get(param);
    }

    public Value getOrderParamDefault(final String param) {
        return paramOrderDefaultMap.get(param);
    }

    public Value getOrderParamPreferred(final String param) {
        return paramOrderPreferredMap.get(param);
    }

    /**
     * Returns possible choices for a order parameter, that will be displayed
     * in the combo box.
     */
    public Value[] getOrderParamPossibleChoices(final String param, final boolean masterSlave) {
        if (masterSlave) {
            return paramOrderPossibleChoicesMasterSlave.get(param);
        } else {
            return paramOrderPossibleChoices.get(param);
        }
    }

    public boolean isOrderInteger(final String param) {
        final String type = getOrderParamType(param);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    public boolean isOrderLabel(final String param) {
        final String type = getOrderParamType(param);
        return PARAM_TYPE_LABEL.equals(type);
    }

    public boolean isOrderBoolean(final String param) {
        final String type = getOrderParamType(param);
        return PARAM_TYPE_BOOLEAN.equals(type);
    }

    public boolean isOrderTimeType(final String param) {
        final String type = getOrderParamType(param);
        return PARAM_TYPE_TIME.equals(type);
    }

    public String getOrderSectionToDisplay(final String param) {
        return Tools.getString("CRMXML.OrderSectionParams");
    }

    /**
     * Checks order parameter according to its type. Returns false if value
     * does not fit the type.
     */
    public boolean checkOrderParam(final String param, final Value value) {
        final String type = getOrderParamType(param);
        boolean correctValue = true;
        if (PARAM_TYPE_BOOLEAN.equals(type)) {
            if (!PCMK_YES.equals(value)
                && !PCMK_NO.equals(value)
                && !PCMK_TRUE_VALUE.equals(value)
                && !PCMK_FALSE_VALUE.equals(value)
                && !PCMK_TRUE_2.equals(value)
                && !PCMK_FALSE_2.equals(value)) {

                correctValue = false;
            }
        } else if (PARAM_TYPE_INTEGER.equals(type)) {
            final Pattern p = Pattern.compile("^(-?\\d*|(-|\\+)?" + INFINITY_VALUE + ")$");
            if (value != null && !value.isNothingSelected()) {
                final Matcher m = p.matcher(value.getValueForConfig());
                if (!m.matches()) {
                    correctValue = false;
                }
            }
        } else if (PARAM_TYPE_TIME.equals(type)) {
            final Pattern p = Pattern.compile("^-?\\d*(ms|msec|us|usec|s|sec|m|min|h|hr)?$");
            if (value != null && !value.isNothingSelected()) {
                final Matcher m = p.matcher(value.getValueForConfig());
                if (!m.matches()) {
                    correctValue = false;
                }
            }
        } else if ((value == null || value.isNothingSelected()) && isOrderRequired(param)) {
            correctValue = false;
        }
        return correctValue;
    }

    public String[] getColocationParameters() {
        if (colocationParams != null) {
            return colocationParams.toArray(new String[colocationParams.size()]);
        }
        return null;
    }

    /** Returns colocation parameters for resource sets. (Shown when a
     * placeholder is clicked, rsc_colocation tag). */
    public String[] getResourceSetColocationParameters() {
        if (resourceSetColocationParams != null) {
            return resourceSetColocationParams.toArray(new String[resourceSetColocationParams.size()]);
        }
        return null;
    }

    /** Returns colocation parameters for resource sets. (Shown when an edge
     * is clicked, resource_set tag). */
    public String[] getResourceSetColConnectionParameters() {
        if (resourceSetColocationConnectionParams != null) {
            return resourceSetColocationConnectionParams.toArray(
                new String[resourceSetColocationConnectionParams.size()]);
        }
        return null;
    }

    public boolean isColocationRequired(final String param) {
        return cololcationRequiredParams.contains(param);
    }

    public String getColocationParamShortDesc(final String param) {
        String shortDesc = paramColocationShortDescMap.get(param);
        if (shortDesc == null) {
            shortDesc = param;
        }
        return shortDesc;
    }

    public String getColocationParamLongDesc(final String param) {
        final String shortDesc = getColocationParamShortDesc(param);
        String longDesc = paramColocationLongDescMap.get(param);
        if (longDesc == null) {
            longDesc = "";
        }
        return Tools.html("<b>" + shortDesc + "</b>\n" + longDesc);
    }

    public String getColocationParamType(final String param) {
        return paramColocationTypeMap.get(param);
    }

    public Value getColocationParamDefault(final String param) {
        return paramColocationDefaultMap.get(param);
    }

    public Value getColocationParamPreferred(final String param) {
        return paramColocationPreferredMap.get(param);
    }

    public Value[] getColocationParamComboBoxChoices(final String param, final boolean masterSlave) {
        if (masterSlave) {
            return paramColocationPossibleChoicesMasterSlave.get(param);
        } else {
            return paramColocationPossibleChoices.get(param);
        }
    }

    public boolean isColocationInteger(final String param) {
        final String type = getColocationParamType(param);
        return PARAM_TYPE_INTEGER.equals(type);
    }

    public boolean isColocationLabel(final String param) {
        final String type = getColocationParamType(param);
        return PARAM_TYPE_LABEL.equals(type);
    }

    public boolean isColocationBoolean(final String param) {
        final String type = getOrderParamType(param);
        return PARAM_TYPE_BOOLEAN.equals(type);
    }

    public boolean isColocationTimeType(final String param) {
        final String type = getColocationParamType(param);
        return PARAM_TYPE_TIME.equals(type);
    }

    public String getColocationSectionForDisplay(final String param) {
        return Tools.getString("CRMXML.ColocationSectionParams");
    }

    public boolean checkColocationParam(final String param, final Value value) {
        final String type = getColocationParamType(param);
        boolean correctValue = true;
        if (PARAM_TYPE_BOOLEAN.equals(type)) {
            if (!PCMK_YES.equals(value) && !PCMK_NO.equals(value)
                && !PCMK_TRUE_VALUE.equals(value)
                && !PCMK_FALSE_VALUE.equals(value)
                && !PCMK_TRUE_2.equals(value)
                && !PCMK_FALSE_2.equals(value)) {

                correctValue = false;
            }
        } else if (PARAM_TYPE_INTEGER.equals(type)) {
            final Pattern p = Pattern.compile("^(-?\\d*|(-|\\+)?" + INFINITY_VALUE + ")$");
            if (value != null && value.getValueForConfig() != null) {
                final Matcher m = p.matcher(value.getValueForConfig());
                if (!m.matches()) {
                    correctValue = false;
                }
            }
        } else if (PARAM_TYPE_TIME.equals(type)) {
            final Pattern p = Pattern.compile("^-?\\d*(ms|msec|us|usec|s|sec|m|min|h|hr)?$");
            if (value != null && value.getValueForConfig() != null) {
                final Matcher m = p.matcher(value.getValueForConfig());
                if (!m.matches()) {
                    correctValue = false;
                }
            }
        } else if ((value == null || value.isNothingSelected()) && isColocationRequired(param)) {
            correctValue = false;
        }
        return correctValue;
    }

    /** Returns whether drbddisk ra is present. */
    public boolean isDrbddiskResourceAgentPresent() {
        return drbddiskResourceAgentPresent;
    }

    /** Returns whether linbit::drbd ra is present. */
    public boolean isLinbitDrbdResourceAgentPresent() {
        return linbitDrbdResourceAgentPresent;
    }

    void parseLrmResources(final String unameLowerCase,
                           final Node lrmNode,
                           final Collection<String> resList,
                           final Map<String, ResourceAgent> resourceTypeMap,
                           final Map<String, Map<String, String>> parametersMap,
                           final Map<String, Set<String>> inLRMList,
                           final Collection<String> orphanedList,
                           final Table<String, String, Set<String>> failedClonesMap) {
        final Node lrmResourcesNode = XMLTools.getChildNode(lrmNode, "lrm_resources");
        final NodeList lrmResources = lrmResourcesNode.getChildNodes();
        for (int j = 0; j < lrmResources.getLength(); j++) {
            final Node rscNode = lrmResources.item(j);
            if ("lrm_resource".equals(rscNode.getNodeName())) {
                final String resId = XMLTools.getAttribute(rscNode, "id");
                final Pattern p = Pattern.compile("(.*):(\\d+)$");
                final Matcher m = p.matcher(resId);
                final String crmId;
                if (m.matches()) {
                    crmId = m.group(1);
                    Set<String> clones = failedClonesMap.get(unameLowerCase, crmId);
                    if (clones == null) {
                        clones = new LinkedHashSet<String>();
                        failedClonesMap.put(unameLowerCase, crmId, clones);
                    }
                    clones.add(m.group(2));
                } else {
                    crmId = resId;
                }
                if (!resourceTypeMap.containsKey(crmId)) {
                    final String raClass = XMLTools.getAttribute(rscNode, "class");
                    String provider = XMLTools.getAttribute(rscNode, "provider");
                    if (provider == null) {
                        provider = ResourceAgent.HEARTBEAT_PROVIDER;
                    }
                    final String type = XMLTools.getAttribute(rscNode, "type");
                    resourceTypeMap.put(crmId, getResourceAgent(type, provider, raClass));
                    resList.add(crmId);
                    parametersMap.put(crmId, new HashMap<String, String>());
                    orphanedList.add(crmId);
                }
                /* it is in LRM */
                Set<String> inLRMOnHost = inLRMList.get(unameLowerCase);
                if (inLRMOnHost == null) {
                    inLRMOnHost = new HashSet<String>();
                    inLRMList.put(unameLowerCase, inLRMOnHost);
                }
                inLRMOnHost.add(crmId);
            }
        }
    }

    private Unit parseUnit(final String param, final String u) {
        if ("s".equals(u) || "".equals(u)) {
            return getUnitSecond();
        } else if ("ms".equals(u)) {
            return getUnitMilliSec();
        } else if ("us".equals(u)) {
            return getUnitMicroSec();
        } else if ("m".equals(u) || "min".equals(u)) {
            return getUnitMinute();
        } else if ("h".equals(u)) {
            return getUnitHour();
        } else {
            LOG.appError("can't parse unit: " + u + " param: " + param);
        }
        return null;
    }


    private Value parseValue(final String param, final CharSequence v) {
        if (v == null) {
            return null;
        }
        final Matcher m = UNIT_PATTERN.matcher(v);
        if (m.matches()) {
            final String value = m.group(1);
            final String u = m.group(2);
            final Unit unit = parseUnit(param, u);
            return new StringValue(value, unit);
        }
        return null;
    }

    /**
     * This class holds parsed status of resource, m/s set, or clone set.
     */
    static class ResourceStatus {
        private final List<String> runningOnNodes;
        private final List<String> masterOnNodes;
        private final List<String> slaveOnNodes;
        private final Map<String, String> allocationScores;
        /** Is managed by CRM. */
        private final boolean managedByCrm;

        ResourceStatus(final List<String> runningOnNodes,
                       final List<String> masterOnNodes,
                       final List<String> slaveOnNodes,
                       final Map<String, String> allocationScores,
                       final boolean managedByCrm) {
            this.runningOnNodes = runningOnNodes;
            this.masterOnNodes = masterOnNodes;
            this.slaveOnNodes = slaveOnNodes;
            this.allocationScores = allocationScores;
            this.managedByCrm = managedByCrm;
        }

        List<String> getRunningOnNodes() {
            return runningOnNodes;
        }

        List<String> getMasterOnNodes() {
            return masterOnNodes;
        }

        List<String> getSlaveOnNodes() {
            return slaveOnNodes;
        }

        boolean isManagedByCrm() {
            return managedByCrm;
        }

        Map<String, String> getAllocationScores() {
            return allocationScores;
        }
    }

    /** Class that holds colocation data. */
    public static final class ColocationData {
        private final String id;
        private final String rsc;
        private final String withRsc;
        private final String rscRole;
        private final String withRscRole;
        private final String score;

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

        public String getId() {
            return id;
        }

        public String getRsc() {
            return rsc;
        }

        public String getWithRsc() {
            return withRsc;
        }

        public String getRscRole() {
            return rscRole;
        }

        public String getWithRscRole() {
            return withRscRole;
        }

        public String getScore() {
            return score;
        }
    }

    public static final class OrderData {
        private final String id;
        private final String rscFirst;
        private final String rscThen;
        private final String score;
        private final String symmetrical;
        private final String firstAction;
        private final String thenAction;

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

        public String getId() {
            return id;
        }

        String getRscFirst() {
            return rscFirst;
        }

        public String getRscThen() {
            return rscThen;
        }

        public String getScore() {
            return score;
        }

        public String getSymmetrical() {
            return symmetrical;
        }

        public String getFirstAction() {
            return firstAction;
        }

        public String getThenAction() {
            return thenAction;
        }
    }

    /** Class that holds resource set data. */
    public static final class RscSet {
        private final String id;
        private final List<String> rscIds;
        private final ReadWriteLock mRscIdsLock = new ReentrantReadWriteLock();
        private final Lock mRscIdsReadLock = mRscIdsLock.readLock();
        private final Lock mRscIdsWriteLock = mRscIdsLock.writeLock();
        private final String sequential;
        private final String requireAll;
        private final String orderAction;
        private final String colocationRole;

        public RscSet(final String id,
                      final List<String> rscIds,
                      final String sequential,
                      final String requireAll,
                      final String orderAction,
                      final String colocationRole) {
            this.id = id;
            this.rscIds = rscIds;
            this.sequential = sequential;
            this.requireAll = requireAll;
            this.orderAction = orderAction;
            this.colocationRole = colocationRole;
        }

        public String getId() {
            return id;
        }
        public List<String> getRscIds() {
            final List<String> copy = new ArrayList<String>();
            mRscIdsReadLock.lock();
            try {
                for (final String rscId : rscIds) {
                    copy.add(rscId);
                }
            } finally {
                mRscIdsReadLock.unlock();
            }
            return copy;
        }

        public String getSequential() {
            return sequential;
        }

        public String getRequireAll() {
            return requireAll;
        }

        public boolean isSubsetOf(final RscSet oRscSet) {
            if (oRscSet == null) {
                return false;
            }
            final List<String> oRscIds = oRscSet.getRscIds();
            mRscIdsReadLock.lock();
            try {
                if (rscIds.isEmpty()) {
                    return false;
                }
                for (final String rscId : rscIds) {
                    if (!oRscIds.contains(rscId)) {
                        return false;
                    }
                }
            } finally {
                mRscIdsReadLock.unlock();
            }
            return true;
        }

        /** Returns whether this resource set is equal to the supplied
         * resource set. The order of ids doesn't matter. */
        boolean equals(final RscSet oRscSet) {
            if (oRscSet == null) {
                return false;
            }
            final List<String> oRscIds = oRscSet.getRscIds();
            mRscIdsReadLock.lock();
            try {
                if (oRscIds.size() != rscIds.size()) {
                    return false;
                }
                for (final String rscId : rscIds) {
                    if (!oRscIds.contains(rscId)) {
                        return false;
                    }
                }
            } finally {
                mRscIdsReadLock.unlock();
            }
            return true;
        }

        /** Adds one id to rsc ids. */
        public void addRscId(final String id) {
            mRscIdsWriteLock.lock();
            try {
                rscIds.add(id);
            } finally {
                mRscIdsWriteLock.unlock();
            }
        }

        boolean isRscIdsEmpty() {
            mRscIdsReadLock.lock();
            try {
                return rscIds.isEmpty();
            } finally {
                mRscIdsReadLock.unlock();
            }
        }

        @Override
        public String toString() {
            final StringBuilder s = new StringBuilder(20);
            s.append("rscset id: ");
            s.append(id);
            s.append(" ids: ");
            s.append(rscIds);
            return s.toString();
        }

        public String getOrderAction() {
            return orderAction;
        }

        public String getColocationRole() {
            return colocationRole;
        }

        public boolean isSequential() {
            return sequential == null || "true".equals(sequential);
        }

        /** Returns whether the resouce set requires all resources to be
         *  started . */
        public boolean isRequireAll() {
            return requireAll == null || REQUIRE_ALL_TRUE_VALUE.getValueForConfig().equals(requireAll);
        }
    }

}
