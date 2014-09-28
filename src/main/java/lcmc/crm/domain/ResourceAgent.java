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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import lcmc.common.domain.Application;
import lcmc.common.domain.Value;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.resources.crm.ServiceInfo;
import lcmc.gui.widget.Widget;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import org.apache.commons.collections15.map.MultiKeyMap;

/**
 * This class describes a resource agent with its name and class.
 * This is important in otder to distinguish services that have the same name
 * int the heartbeat, ocf, service or lsb classes.
 */
public final class ResourceAgent {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceAgent.class);
    public static final String SERVICE_CLASS_NAME = "service";
    public static final String UPSTART_CLASS_NAME = "upstart";
    public static final String SYSTEMD_CLASS_NAME = "systemd";
    /** Name of lsb style resource (/etc/init.d/*). */
    public static final String LSB_CLASS_NAME = "lsb";
    public static final Collection<String> SERVICE_CLASSES = new ArrayList<String>();
    /** Name of heartbeat style resource (heartbeat 1). */
    public static final String HEARTBEAT_CLASS_NAME = "heartbeat";
    /** Name of ocf style resource (heartbeat 2). */
    public static final String OCF_CLASS_NAME = "ocf";
    public static final String STONITH_CLASS_NAME = "stonith";
    public static final String HEARTBEAT_PROVIDER = "heartbeat";
    static {
        SERVICE_CLASSES.add(SERVICE_CLASS_NAME); /* contains upstart and systemd */
        SERVICE_CLASSES.add(UPSTART_CLASS_NAME);
        SERVICE_CLASSES.add(SYSTEMD_CLASS_NAME);
        SERVICE_CLASSES.add(LSB_CLASS_NAME); /* deprecated */
    }
    private final String serviceName;
    /** Name of the provider like "linbit". */
    private final String provider;
    /** Class of the service, like ocf. */
    private final String resourceClass;
    private String serviceVersion;
    /** Long description of the hb service. */
    private String serviceLongDesc;
    private String serviceShortDesc;
    /** Hash code. */
    private final int hash;
    private final List<String> masterSlaveParameters = new ArrayList<String>();
    private final List<String> parameters = new ArrayList<String>();
    private final Collection<String> requiredParams = new HashSet<String>();
    private final Collection<String> metaAttrParams = new HashSet<String>();
    private final Map<String, String> paramLongDescriptions = new HashMap<String, String>();
    private final Map<String, String> paramShortDescriptions = new HashMap<String, String>();
    private final Map<String, String> paramTypes = new HashMap<String, String>();
    private final Map<String, String> paramDefaults = new HashMap<String, String>();
    private final Map<String, String> paramPreferredValues = new HashMap<String, String>();
    private final Map<String, Value[]> paramPossibleChoices = new HashMap<String, Value[]>();
    private final Map<String, Value[]> paramPossibleChoicesMS = new HashMap<String, Value[]>();
    private final String pullDownMenuName;
    private final MultiKeyMap<String, Value> nameParameterToDefaultOperations = new MultiKeyMap<String, Value>();
    private final Collection<String> operationNames = new LinkedHashSet<String>();
    private boolean probablyMasterSlave = false;
    private boolean probablyClone = false;
    /** Sections for some parameters. */
    private final Map<String, String> paramSections = new HashMap<String, String>();
    private final Map<String, Widget.Type> fieldTypes = new HashMap<String, Widget.Type>();
    private final boolean pingService;
    /** Whether to ignore defaults, show them, but don't assume they are defaults. */
    private boolean ignoreDefaults = false;
    private boolean metaDataLoaded = false;

    public ResourceAgent(final String serviceName, final String provider, final String resourceClass) {
        this.serviceName = serviceName;
        this.provider = provider;
        this.resourceClass = resourceClass;
        operationNames.addAll(Arrays.asList(ClusterBrowser.CRM_OPERATIONS));
        hash = (serviceName == null ? 0 : serviceName.hashCode() * 31)
               + (resourceClass == null ? 0 : resourceClass.hashCode());
        if (HEARTBEAT_PROVIDER.equals(provider)) {
            pullDownMenuName = serviceName;
        } else {
            pullDownMenuName = provider + ':' + serviceName;
        }
        /* info fields */
        String section = "Resource";
        if (isClone()) {
            section = "Set";
        } else if (isGroup()) {
            section = "Group";
        }
        pingService = "ping".equals(serviceName) || "pingd".equals(serviceName);
        addParameter(ServiceInfo.GUI_ID);
        paramSections.put(ServiceInfo.GUI_ID, section);
        requiredParams.add(ServiceInfo.GUI_ID);
        paramShortDescriptions.put(ServiceInfo.GUI_ID, "Name");
        paramLongDescriptions.put(ServiceInfo.GUI_ID, "Name");

        addInfoParameter(section, ServiceInfo.PCMK_ID, "new...", "Id", "Id");
        if (!isClone() && !isGroup()) {
            addInfoParameter("Resource", ServiceInfo.RA_PARAM, getRAString(), "Resource Agent", "Resource Agent");
        }

    }

    private void addInfoParameter(final String section,
                                  final String name,
                                  final String defaultValue,
                                  final String shortName,
                                  final String longName) {
        addParameter(name);
        if (defaultValue != null) {
            paramDefaults.put(name, defaultValue);
        }
        paramSections.put(name, section);
        requiredParams.add(name);
        paramShortDescriptions.put(name, shortName);
        paramLongDescriptions.put(name, longName);
        fieldTypes.put(name, Widget.Type.LABELFIELD);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getProvider() {
        return provider;
    }

    /**
     * Returns the hb name as it should appear in the pull down menus. This
     * actually only because of "Filesytem / DRBD".
     */
    public String getPullDownMenuName() {
        return pullDownMenuName;
    }

    public String getResourceClass() {
        return resourceClass;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    /**
     * Returns whether two service equal. They have the same name and are from
     * the same hb class.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !getClass().isInstance(obj)) {
            return false;
        }
        final ResourceAgent other = getClass().cast(obj);
        return (serviceName == null ? other.serviceName == null : serviceName.equals(other.serviceName))
               && (resourceClass == null ? other.resourceClass == null
                                : resourceClass.equals(other.resourceClass));
    }

    void setServiceVersion(final String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    String getServiceVersion() {
        return serviceVersion;
    }

    void setServiceLongDesc(final String serviceLongDesc) {
        this.serviceLongDesc = serviceLongDesc;
    }

    String getServiceLongDesc() {
        return serviceLongDesc;
    }

    void setServiceShortDesc(final String serviceShortDesc) {
        this.serviceShortDesc = serviceShortDesc;
    }

    String getServiceShortDesc() {
        return serviceShortDesc;
    }

    void addMasterParameter(final String param) {
        masterSlaveParameters.add(param);
    }

    void addParameter(final String param) {
        masterSlaveParameters.add(param);
        parameters.add(param);
    }

    List<String> getParameters(final boolean master) {
        if (master) {
            return masterSlaveParameters;
        } else {
            return parameters;
        }
    }

    /**
     * Prints an error if some info was required for a parameter that does not
     * exist. This should never happen.
     */
    private void wrongParameterError(final String param) {
        LOG.appError("wrongParameterError: param: " + param);
    }

    void setParamRequired(final String param, final boolean required) {
        if (!masterSlaveParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        if (required) {
            requiredParams.add(param);
        } else {
            requiredParams.remove(param);
        }
    }

    /** Returns whether the parameter is required. */
    boolean isRequired(final String param) {
        return requiredParams.contains(param);
    }

    void setParamLongDesc(final String param, final String longDesc) {
        if (!masterSlaveParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramLongDescriptions.put(param, longDesc);
    }

    void setParamShortDesc(final String param, final String shortDesc) {
        if (!masterSlaveParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramShortDescriptions.put(param, shortDesc);
    }

    void setParamType(final String param, final String type) {
        if (!masterSlaveParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramTypes.put(param, type);
    }

    /** Gets the type of the parameter. */
    String getParamType(final String param) {
        return paramTypes.get(param);
    }

    /** Sets the default value of the parameter. */
    void setParamDefault(final String param, final String defaultValue) {
        if (!masterSlaveParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramDefaults.put(param, defaultValue);
    }

    String getDefaultValue(final String param) {
        return paramDefaults.get(param);
    }

    void setParamPreferred(final String param, final String preferredValue) {
        if (!masterSlaveParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramPreferredValues.put(param, preferredValue);
    }

    String getPreferredValue(final String param) {
        return paramPreferredValues.get(param);
    }

    void setParamPossibleChoices(final String param, final Value[] choices) {
        if (!masterSlaveParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramPossibleChoices.put(param, choices);
    }

    void setParamPossibleChoicesMS(final String param, final Value[] choices) {
        if (!masterSlaveParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramPossibleChoicesMS.put(param, choices);
    }

    Value[] getComboBoxChoices(final String param) {
        return paramPossibleChoices.get(param);
    }

    Value[] getComboBoxChoicesMasterSlave(final String param) {
        final Value[] ret = paramPossibleChoicesMS.get(param);
        if (ret == null) {
            return getComboBoxChoices(param);
        }
        return ret;
    }


    String getShortDesc(final String param) {
        return paramShortDescriptions.get(param);
    }

    String getLongDesc(final String param) {
        return paramLongDescriptions.get(param);
    }

    public boolean isFilesystem() {
        return "Filesystem".equals(serviceName) && isOCFClass();
    }

    public boolean isDrbddisk() {
        return "drbddisk".equals(serviceName) && isHeartbeatClass();
    }

    public boolean isLinbitDrbd() {
        return "drbd".equals(serviceName) && "linbit".equals(provider);
    }

    public boolean isHbDrbd() {
        return "drbd".equals(serviceName) && HEARTBEAT_PROVIDER.equals(provider);
    }

    void setParamIsMetaAttr(final String param, final boolean isMetaAttr) {
        if (!masterSlaveParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        if (isMetaAttr) {
            metaAttrParams.add(param);
        } else {
            metaAttrParams.remove(param);
        }
    }

    boolean isParamMetaAttr(final String param) {
        return metaAttrParams.contains(param);
    }

    public boolean isIPaddr() {
        return isOCFClass() && ("IPaddr".equals(serviceName) || "IPaddr2".equals(serviceName));
    }

    public boolean isVirtualDomain() {
        return isOCFClass() && "VirtualDomain".equals(serviceName);
    }

    public boolean isGroup() {
        return Application.PACEMAKER_GROUP_NAME.equals(serviceName) && "group".equals(resourceClass);
    }

    public boolean isClone() {
        return Application.PM_CLONE_SET_NAME.equals(serviceName) && "clone".equals(resourceClass);
    }

    public boolean isStonith() {
        return STONITH_CLASS_NAME.equals(resourceClass);
    }

    public boolean isHeartbeatClass() {
        return HEARTBEAT_CLASS_NAME.equals(resourceClass);
    }

    public boolean isOCFClass() {
        return OCF_CLASS_NAME.equals(resourceClass);
    }

    /**
     * Adds default value for operation like 'start' and param like 'timeout'
     * to the hash.
     */
    void addOperationDefault(final String name, final String param, final Value defaultValue) {
        nameParameterToDefaultOperations.put(name, param, defaultValue);
        operationNames.add(name);
    }

    public Value getOperationDefault(final String name, final String param) {
        return nameParameterToDefaultOperations.get(name, param);
    }

    public Iterable<String> getOperationNames() {
        return operationNames;
    }

    void setProbablyMasterSlave(final boolean probablyMasterSlave) {
        this.probablyMasterSlave = probablyMasterSlave;
    }

    public boolean isProbablyMasterSlave() {
        return probablyMasterSlave;
    }

    void setProbablyClone(final boolean probablyClone) {
        this.probablyClone = probablyClone;
    }

    public boolean isProbablyClone() {
        return probablyClone;
    }

    String getSection(final String param) {
        return paramSections.get(param);
    }

    void setSection(final String param, final String section) {
        paramSections.put(param, section);
    }

    public Widget.Type getFieldType(final String param) {
        return fieldTypes.get(param);
    }

    public String getRAString() {
        if (HEARTBEAT_PROVIDER.equals(provider)) {
            return resourceClass + ':' + serviceName;
        }
        return resourceClass + ':' + provider + ':' + serviceName;
    }

    public boolean isPingService() {
        return pingService;
    }

    public void setIgnoreDefaults(final boolean ignoreDefaults) {
        this.ignoreDefaults = ignoreDefaults;
    }

    public boolean isIgnoreDefaults() {
        return ignoreDefaults;
    }

    public boolean isMetaDataLoaded() {
        return metaDataLoaded;
    }

    public void setMetaDataLoaded(final boolean metaDataLoaded) {
        this.metaDataLoaded = metaDataLoaded;
    }

    public boolean hasParameter(final String param) {
        return parameters.contains(param);
    }
}
