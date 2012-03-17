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

package lcmc.data;

import lcmc.utilities.Tools;
import lcmc.gui.GuiComboBox;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.resources.ServiceInfo;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Arrays;
import org.apache.commons.collections15.map.MultiKeyMap;

/**
 * This class describes a resource agent with its name and class.
 * This is important in otder to distinguish services that have the same name
 * int the heartbeat, ocf or lsb classes.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class ResourceAgent {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Name of the service. */
    private final String name;
    /** Name of the provider like "linbit". */
    private final String provider;
    /** Class of the service, like ocf. */
    private final String resourceClass;
    /** Version of the hb service. */
    private String version;
    /** Long description of the hb service. */
    private String longDesc;
    /** Short description of the hb service. */
    private String shortDesc;
    /** Hash code. */
    private final int hash;
    /** List of master/slave parameters. */
    private final List<String> masterParameters = new ArrayList<String>();
    /** List of clone parameters. */
    private final List<String> parameters = new ArrayList<String>();
    /** List of required parameters. */
    private final Set<String> paramRequired = new HashSet<String>();
    /** List of parameters that are meta attributes. */
    private final Set<String> paramIsMetaAttr = new HashSet<String>();
    /** Map from parameter to its long description. */
    private final Map<String, String> paramLongDesc =
                                                 new HashMap<String, String>();
    /** Map from parameter to its short description. */
    private final Map<String, String> paramShortDesc =
                                                 new HashMap<String, String>();
    /** Map from parameter to its type. */
    private final Map<String, String> paramType = new HashMap<String, String>();
    /** Map from parameter to its default value. */
    private final Map<String, String> paramDefault =
                                                 new HashMap<String, String>();
    /** Map from parameter to its preferred value. */
    private final Map<String, String> paramPreferred =
                                                 new HashMap<String, String>();
    /** Map from parameter to an array of its possible choices. */
    private final Map<String, String[]> paramPossibleChoices =
                                               new HashMap<String, String[]>();
    /** Map from m/s parameter to an array of its possible choices. */
    private final Map<String, String[]> paramPossibleChoicesMS =
                                               new HashMap<String, String[]>();
    /** Name of the hb service in the pull down menus. */
    private final String menuName;
    /** Map that holds default values for operations. The keys are the name and
     * parameter. */
    private final MultiKeyMap<String, String> opToDefault =
                                            new MultiKeyMap<String, String>();
    /** Names of the operations, with some of them predefined. */
    private final Set<String> operations = new LinkedHashSet<String>();
    /** Whether the service is probably master/slave resource. */
    private boolean probablyMasterSlave = false;
    /** Whether the service is probably clone resource. */
    private boolean probablyClone = false;
    /** Sections for some parameters. */
    private final Map<String, String> sectionMap =
                                                new HashMap<String, String>();
    /** Map to field types for some parameters. */
    private final Map<String, GuiComboBox.Type> fieldType =
                                       new HashMap<String, GuiComboBox.Type>();
    /** Whether this resource agent is ping or pingd. */
    private final boolean pingService;
    /** Whether to ignore defaults, show them, but don't assume they are
     * defaults. */
    private boolean ignoreDefaults = false;
    /** Whether meta-data has been loaded. */
    private boolean metaDataLoaded = false;
    /** Name of lsb style resource (/etc/init.d/*). */
    public static final String LSB_CLASS = "lsb";
    /** Name of heartbeat style resource (heartbeat 1). */
    public static final String HEARTBEAT_CLASS = "heartbeat";
    /** Name of ocf style resource (heartbeat 2). */
    public static final String OCF_CLASS = "ocf";
    /** Name of stonith device class. */
    public static final String STONITH_CLASS = "stonith";
    /** Name of the heartbeat provider. */
    public static final String HEARTBEAT_PROVIDER = "heartbeat";
    /**
     * Prepares a new <code>ResourceAgent</code> object.
     */
    public ResourceAgent(final String name,
                         final String provider,
                         final String resourceClass) {
        this.name = name;
        this.provider = provider;
        this.resourceClass = resourceClass;
        operations.addAll(Arrays.asList(ClusterBrowser.HB_OPERATIONS));
        hash = (name == null ? 0 : name.hashCode() * 31)
               + (resourceClass == null ? 0 : resourceClass.hashCode());
        if (HEARTBEAT_PROVIDER.equals(provider)) {
            menuName = name;
        } else {
            menuName = provider + ":" + name;
        }
        /* info fields */
        String section = "Resource";
        if (isClone()) {
            section = "Set";
        } else if (isGroup()) {
            section = "Group";
        }
        pingService = "ping".equals(name) || "pingd".equals(name);
        addParameter(ServiceInfo.GUI_ID);
        sectionMap.put(ServiceInfo.GUI_ID, section);
        paramRequired.add(ServiceInfo.GUI_ID);
        paramShortDesc.put(ServiceInfo.GUI_ID, "Name");
        paramLongDesc.put(ServiceInfo.GUI_ID, "Name");

        addInfoParameter(section, ServiceInfo.PCMK_ID, "new...", "Id", "Id");
        if (!isClone() && !isGroup()) {
            addInfoParameter("Resource",
                             "ra",
                             getRAString(),
                             "Resource Agent",
                             "Resource Agent");
        }

    }

    /** Adds info paramter. */
    private void addInfoParameter(final String section,
                                  final String name,
                                  final String defaultValue,
                                  final String shortName,
                                  final String longName) {
        addParameter(name);
        if (defaultValue != null) {
            paramDefault.put(name, defaultValue);
        }
        sectionMap.put(name, section);
        paramRequired.add(name);
        paramShortDesc.put(name, shortName);
        paramLongDesc.put(name, longName);
        fieldType.put(name, GuiComboBox.Type.LABELFIELD);
    }

    /** Returns the hb service name. */
    public String getName() {
        return name;
    }

    /** Returns the provider. */
    public String getProvider() {
        return provider;
    }

    /**
     * Returns the hb name as it should appear in the pull down menus. This
     * actually only because of "Filesytem / DRBD".
     */
    public String getMenuName() {
        return menuName;
    }

    /** Returns the class of the service, like ocf. */
    public String getResourceClass() {
        return resourceClass;
    }

    /** Returns the hash code. */
    @Override
    public int hashCode() {
        return hash;
    }

    /**
     * Returns whethet two service equal. They have the same name and are from
     * the same hb class.
     */
    @Override
    public boolean equals(final Object oth) {
        if (this == oth) {
            return true;
        }
        if (oth == null || !getClass().isInstance(oth)) {
            return false;
        }
        final ResourceAgent other = getClass().cast(oth);
        return (name == null ? other.name == null : name.equals(other.name))
               && (resourceClass == null ? other.resourceClass == null
                                : resourceClass.equals(other.resourceClass));
    }

    /** Sets ocf script version for service. */
    void setVersion(final String version) {
        this.version = version;
    }

    /** Returns the version of the hb ocf script. */
    String getVersion() {
        return version;
    }

    /** Sets the long description of the service. */
    void setLongDesc(final String longDesc) {
        this.longDesc = longDesc;
    }

    /** Gets the long description of the service. */
    String getLongDesc() {
        return longDesc;
    }

    /** Sets the short description of the service. */
    void setShortDesc(final String shortDesc) {
        this.shortDesc = shortDesc;
    }

    /** Gets the short description of the service. */
    String getShortDesc() {
        return shortDesc;
    }

    /** Adds parameter of this service. */
    void addMasterParameter(final String param) {
        masterParameters.add(param);
    }

    /** Adds parameter of this service. */
    void addParameter(final String param) {
        masterParameters.add(param);
        parameters.add(param);
    }

    /** Returns an array of all service parameters. */
    String[] getParameters(final boolean master) {
        if (master) {
            return masterParameters.toArray(new String[parameters.size()]);
        } else {
            return parameters.toArray(new String[parameters.size()]);
        }
    }

    /**
     * Prints an error if some info was required for a parameter that does not
     * exist. This should never happen.
     */
    private void wrongParameterError(final String param) {
        Tools.appError("Wrong parameter: " + param);
    }

    /** Sets whether the supplied parameter is required. */
    void setParamRequired(final String param, final boolean required) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        if (required) {
            paramRequired.add(param);
        } else {
            paramRequired.remove(param);
        }
    }

    /** Returns whether the parameter is required. */
    boolean isRequired(final String param) {
        return paramRequired.contains(param);
    }

    /** Sets the parameter long description. */
    void setParamLongDesc(final String param,
                          final String longDesc) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramLongDesc.put(param, longDesc);
    }

    /** Sets the parameter short description. */
    void setParamShortDesc(final String param,
                                        final String shortDesc) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramShortDesc.put(param, shortDesc);
    }

    /** Sets the parameter type. */
    void setParamType(final String param, final String type) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramType.put(param, type);
    }

    /** Gets the type of the parameter. */
    String getParamType(final String param) {
        return paramType.get(param);
    }

    /** Sets the default value of the parameter. */
    void setParamDefault(final String param, final String defaultValue) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramDefault.put(param, defaultValue);
    }

    /** Gets the default value of the parameter. */
    String getParamDefault(final String param) {
        return paramDefault.get(param);
    }

    /**
     * Sets the preferred value of the parameter that is preferred over default
     * value.
     */
    void setParamPreferred(final String param, final String preferredValue) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramPreferred.put(param, preferredValue);
    }

    /** Gets the preferred value of the parameter. */
    String getParamPreferred(final String param) {
        return paramPreferred.get(param);
    }

    /** Sets the possible choices for the parameter. */
    void setParamPossibleChoices(final String param, final String[] choices) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramPossibleChoices.put(param, choices);
    }

    /** Sets the possible choices for the parameter. */
    void setParamPossibleChoicesMS(final String param,
                                   final String[] choices) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramPossibleChoicesMS.put(param, choices);
    }

    /** Gets the array of the possible choices of the parameter. */
    String[] getParamPossibleChoices(final String param) {
        return paramPossibleChoices.get(param);
    }

    /** Gets the array of the possible choices of the m/s parameter. */
    String[] getParamPossibleChoicesMS(final String param) {
        final String[] ret = paramPossibleChoicesMS.get(param);
        if (ret == null) {
            return getParamPossibleChoices(param);
        }
        return ret;
    }


    /** Returns the short description of the parameter. */
    String getParamShortDesc(final String param) {
        return paramShortDesc.get(param);
    }

    /** Returns the long description of the parameter. */
    String getParamLongDesc(final String param) {
        return paramLongDesc.get(param);
    }

    /** Returns whether this service is filesystem. */
    public boolean isFilesystem() {
        return "Filesystem".equals(name) && isOCFClass();
    }

    /** Returns whether this service is drbddisk. */
    public boolean isDrbddisk() {
        return "drbddisk".equals(name) && isHeartbeatClass();
    }

    /** Returns whether this service is linbit drbd ra. */
    public boolean isLinbitDrbd() {
        return "drbd".equals(name) && "linbit".equals(provider);
    }

    /** Returns whether this service is heartbeat drbd ra. */
    public boolean isHbDrbd() {
        return "drbd".equals(name) && HEARTBEAT_PROVIDER.equals(provider);
    }


    /** Sets whether the supplied parameter is meta attribute. */
    void setParamIsMetaAttr(final String param, final boolean isMetaAttr) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        if (isMetaAttr) {
            paramIsMetaAttr.add(param);
        } else {
            paramIsMetaAttr.remove(param);
        }
    }

    /** Returns whether the parameter is meta attribute. */
    boolean isParamMetaAttr(final String param) {
        return paramIsMetaAttr.contains(param);
    }

    /** Returns whether this service is IPaddr or IPaddr2. */
    public boolean isIPaddr() {
        return isOCFClass()
               && ("IPaddr".equals(name) || "IPaddr2".equals(name));
    }

    /** Returns whether this service is VirtualDomain. */
    public boolean isVirtualDomain() {
        return isOCFClass() && "VirtualDomain".equals(name);
    }

    /** Returns whether this service/object is group. */
    public boolean isGroup() {
        return ConfigData.PM_GROUP_NAME.equals(name)
               && "group".equals(resourceClass);
    }

    /** Returns whether this service/object is clone set. */
    public boolean isClone() {
        return ConfigData.PM_CLONE_SET_NAME.equals(name)
               && "clone".equals(resourceClass);
    }

    /** Returns whether this service/object is stonith device. */
    public boolean isStonith() {
        return STONITH_CLASS.equals(resourceClass);
    }

    /** Returns whether this service is in the heartbeat class. */
    public boolean isHeartbeatClass() {
        return HEARTBEAT_CLASS.equals(resourceClass);
    }

    /** Returns whether this service is in the ocf class. */
    public boolean isOCFClass() {
        return OCF_CLASS.equals(resourceClass);
    }

    /**
     * Adds default value for operation like 'start' and param like 'timeout'
     * to the hash.
     */
    void addOperationDefault(final String name,
                             final String param,
                             final String defaultValue) {
        opToDefault.put(name, param, defaultValue);
        operations.add(name);
    }

    /** Returns the default value of operation parameter. */
    public String getOperationDefault(final String name, final String param) {
        return opToDefault.get(name, param);
    }

    /** Returns name of all operations. */
    public Set<String> getOperationNames() {
        return operations;
    }

    /**
     * Sets if this service is master/slave service (with certain probability).
     */
    void setProbablyMasterSlave(final boolean probablyMasterSlave) {
        this.probablyMasterSlave = probablyMasterSlave;
    }

    /** Returns whether the service is probably master/slave resource. */
    public boolean isProbablyMasterSlave() {
        return probablyMasterSlave;
    }

    /** Sets if this service is clone service (with certain probability). */
    void setProbablyClone(final boolean probablyClone) {
        this.probablyClone = probablyClone;
    }

    /** Returns whether the service is probably master/slave resource. */
    public boolean isProbablyClone() {
        return probablyClone;
    }

    /** Returns section of some of the parameters. */
    String getSection(final String param) {
        return sectionMap.get(param);
    }

    /** Set section of some of the parameters. */
    void setSection(final String param, final String section) {
        sectionMap.put(param, section);
    }

    /** Returns field type of the param. */
    public GuiComboBox.Type getFieldType(final String param) {
        return fieldType.get(param);
    }

    /** Returns resource agent string like ocf:linbit:drbd. */
    public String getRAString() {
        return resourceClass + ":" + provider + ":" + name;
    }

    /** Returns whether this resource agent is ping or pingd. */
    public boolean isPingService() {
        return pingService;
    }

    /** Set whether the default should be used. */
    public void setIgnoreDefaults(final boolean ignoreDefaults) {
        this.ignoreDefaults = ignoreDefaults;
    }

    /** Whether the default should be used. */
    public boolean isIgnoreDefaults() {
        return ignoreDefaults;
    }

    /** Whether the meta data are loaded. */
    public final boolean isMetaDataLoaded() {
        return metaDataLoaded;
    }

    /** Set whether the meta data are loaded. */
    public final void setMetaDataLoaded(final boolean metaDataLoaded) {
        this.metaDataLoaded = metaDataLoaded;
    }
}
