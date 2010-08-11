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
import drbd.gui.GuiComboBox;
import drbd.gui.resources.ServiceInfo;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import org.apache.commons.collections.map.MultiKeyMap;

/**
 * This class describes a resource agent with its name and class.
 * This is important in otder to distinguish services that have the same name
 * int the heartbeat, ocf or lsb classes.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class ResourceAgent {
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
    private final MultiKeyMap opToDefault = new MultiKeyMap();
    /** Whether the service is probably master/slave resource. */
    private boolean probablyMasterSlave = false;
    /** Sections for some parameters. */
    private final Map<String, String> sectionMap =
                                                new HashMap<String, String>();
    /** Map to field types for some parameters. */
    private final Map<String, GuiComboBox.Type> fieldType =
                                       new HashMap<String, GuiComboBox.Type>();
    /**
     * Prepares a new <code>ResourceAgent</code> object.
     */
    public ResourceAgent(final String name,
                         final String provider,
                         final String resourceClass) {
        this.name = name;
        this.provider = provider;
        this.resourceClass = resourceClass;
        hash = (name == null ? 0 : name.hashCode() * 31)
               + (resourceClass == null ? 0 : resourceClass.hashCode());
        if (!"heartbeat".equals(provider)) {
            menuName = provider + ":" + name;
        } else {
            menuName = name;
        }
        /* info fields */
        String section = "Resource";
        if (isClone()) {
            section = "Set";
        } else if (isGroup()) {
            section = "Group";
        }
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

    /**
     * Adds info paramter.
     */
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

    /**
     * Returns the hb service name.
     */
    public final String getName() {
        return name;
    }

    /**
     * returns the provider.
     */
    public final String getProvider() {
        return provider;
    }

    /**
     * Returns the hb name as it should appear in the pull down menus. This
     * actually only because of "Filesytem / DRBD".
     */
    public final String getMenuName() {
        return menuName;
    }

    /**
     * Returns the class of the service, like ocf.
     */
    public final String getResourceClass() {
        return resourceClass;
    }

    /**
     * Returns the hash code.
     */
    public final int hashCode() {
        return hash;
    }

    /**
     * Returns whethet two service equal. They have the same name and are from
     * the same hb class.
     */
    public final boolean equals(final Object oth) {
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

    /**
     * Sets ocf script version for service.
     */
    public final void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Returns the version of the hb ocf script.
     */
    public final String getVersion() {
        return version;
    }

    /**
     * Sets the long description of the service.
     */
    public final void setLongDesc(final String longDesc) {
        this.longDesc = longDesc;
    }

    /**
     * Gets the long description of the service.
     */
    public final String getLongDesc() {
        return longDesc;
    }

    /**
     * Sets the short description of the service.
     */
    public final void setShortDesc(final String shortDesc) {
        this.shortDesc = shortDesc;
    }

    /**
     * Gets the short description of the service.
     */
    public final String getShortDesc() {
        return shortDesc;
    }

    /**
     * Adds parameter of this service.
     */
    public final void addMasterParameter(final String param) {
        masterParameters.add(param);
    }

    /**
     * Adds parameter of this service.
     */
    public final void addParameter(final String param) {
        masterParameters.add(param);
        parameters.add(param);
    }

    /**
     * Returns an array of all service parameters.
     */
    public final String[] getParameters(final boolean master) {
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

    /**
     * Sets whether the supplied parameter is required.
     */
    public final void setParamRequired(final String param,
                                       final boolean required) {
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

    /**
     * Returns whether the parameter is required.
     */
    public final boolean isRequired(final String param) {
        return paramRequired.contains(param);
    }

    /**
     * Sets the parameter long description.
     */
    public final void setParamLongDesc(final String param,
                                       final String longDesc) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramLongDesc.put(param, longDesc);
    }

    /**
     * Sets the parameter short description.
     */
    public final void setParamShortDesc(final String param,
                                        final String shortDesc) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramShortDesc.put(param, shortDesc);
    }

    /**
     * Sets the parameter type.
     */
    public final void setParamType(final String param, final String type) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramType.put(param, type);
    }

    /**
     * Gets the type of the parameter.
     */
    public final String getParamType(final String param) {
        return paramType.get(param);
    }

    /**
     * Sets the default value of the parameter.
     */
    public final void setParamDefault(final String param,
                                      final String defaultValue) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramDefault.put(param, defaultValue);
    }

    /**
     * Gets the default value of the parameter.
     */
    public final String getParamDefault(final String param) {
        return paramDefault.get(param);
    }

    /**
     * Sets the preferred value of the parameter that is preferred over default
     * value.
     */
    public final void setParamPreferred(final String param,
                                        final String preferredValue) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramPreferred.put(param, preferredValue);
    }

    /**
     * Gets the preferred value of the parameter.
     */
    public final String getParamPreferred(final String param) {
        return paramPreferred.get(param);
    }

    /**
     * Sets the possible choices for the parameter.
     */
    public final void setParamPossibleChoices(final String param,
                                              final String[] choices) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramPossibleChoices.put(param, choices);
    }

    /**
     * Sets the possible choices for the parameter.
     */
    public final void setParamPossibleChoicesMS(final String param,
                                                final String[] choices) {
        if (!masterParameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramPossibleChoicesMS.put(param, choices);
    }

    /**
     * Gets the array of the possible choices of the parameter.
     */
    public final String[] getParamPossibleChoices(final String param) {
        return paramPossibleChoices.get(param);
    }

    /**
     * Gets the array of the possible choices of the m/s parameter.
     */
    public final String[] getParamPossibleChoicesMS(final String param) {
        final String[] ret = paramPossibleChoicesMS.get(param);
        if (ret == null) {
            return getParamPossibleChoices(param);
        }
        return ret;
    }


    /**
     * Returns the short description of the parameter.
     */
    public final String getParamShortDesc(final String param) {
        return paramShortDesc.get(param);
    }

    /**
     * Returns the long description of the parameter.
     */
    public final String getParamLongDesc(final String param) {
        return paramLongDesc.get(param);
    }

    /**
     * Returns whether this service is filesystem.
     */
    public final boolean isFilesystem() {
        return "Filesystem".equals(name) && isOCFClass();
    }

    /**
     * Returns whether this service is drbddisk.
     */
    public final boolean isDrbddisk() {
        return "drbddisk".equals(name) && isHeartbeatClass();
    }

    /**
     * Returns whether this service is linbit drbd ra.
     */
    public final boolean isLinbitDrbd() {
        return "drbd".equals(name) && "linbit".equals(provider);
    }

    /**
     * Returns whether this service is heartbeat drbd ra.
     */
    public final boolean isHbDrbd() {
        return "drbd".equals(name) && "heartbeat".equals(provider);
    }


    /**
     * Sets whether the supplied parameter is meta attribute.
     */
    public final void setParamIsMetaAttr(final String param,
                                         final boolean isMetaAttr) {
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

    /**
     * Returns whether the parameter is meta attribute.
     */
    public final boolean isParamMetaAttr(final String param) {
        return paramIsMetaAttr.contains(param);
    }

    /**
     * Returns whether this service is IPaddr or IPaddr2.
     */
    public final boolean isIPaddr() {
        return isOCFClass()
               && ("IPaddr".equals(name) || "IPaddr2".equals(name));
    }

    /**
     * Returns whether this service is VirtualDomain.
     */
    public final boolean isVirtualDomain() {
        return isOCFClass() && "VirtualDomain".equals(name);
    }

    /**
     * Returns whether this service/object is group.
     */
    public final boolean isGroup() {
        return ConfigData.PM_GROUP_NAME.equals(name)
               && "group".equals(resourceClass);
    }

    /**
     * Returns whether this service/object is clone set.
     */
    public final boolean isClone() {
        return ConfigData.PM_CLONE_SET_NAME.equals(name)
               && "clone".equals(resourceClass);
    }

    /**
     * Returns whether this service/object is stonith device.
     */
    public final boolean isStonith() {
        return "stonith".equals(resourceClass);
    }

    /**
     * Returns whether this service is in the heartbeat class.
     */
    public final boolean isHeartbeatClass() {
        return "heartbeat".equals(resourceClass);
    }

    /**
     * Returns whether this service is in the ocf class.
     */
    public final boolean isOCFClass() {
        return "ocf".equals(resourceClass);
    }

    /**
     * Adds default value for operation like 'start' and param like 'timeout'
     * to the hash.
     */
    public final void addOperationDefault(final String name,
                                          final String param,
                                          final String defaultValue) {
        opToDefault.put(name, param, defaultValue);
    }

    /**
     * Returns the default value of operation parameter.
     */
    public final String getOperationDefault(final String name,
                                            final String param) {
        return (String) opToDefault.get(name, param);
    }

    /**
     * Sets if this service is master/slave service (with certain probability).
     */
    public final void setProbablyMasterSlave(
                                           final boolean probablyMasterSlave) {
        this.probablyMasterSlave = probablyMasterSlave;
    }

    /**
     * Returns whether the service is probably master/slave resource.
     */
    public final boolean isProbablyMasterSlave() {
        return probablyMasterSlave;
    }

    /**
     * Returns section of some of the parameters.
     */
    public final String getSection(final String param) {
        return sectionMap.get(param);
    }

    /**
     * Returns field type of the param.
     */
    public final GuiComboBox.Type getFieldType(final String param) {
        return fieldType.get(param);
    }

    /**
     * Returns resource agent string like ocf:linbit:drbd.
     */
    public final String getRAString() {
        return resourceClass + "::" + provider + ":" + name;
    }
}
