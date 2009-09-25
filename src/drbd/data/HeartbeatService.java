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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import org.apache.commons.collections.map.MultiKeyMap;

/**
 * This class describes a heartbeat service with its name and class.
 * This is important in otder to distinguish services that have the same name
 * int the heartbeat, ocf or lsb classes.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HeartbeatService {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Name of the service. */
    private final String name;
    /** Name of the provider like "linbit". */
    private final String provider;
    /** Class of the service, like ocf. */
    private final String heartbeatClass;
    /** Version of the hb service. */
    private String version;
    /** Long description of the hb service. */
    private String longDesc;
    /** Short description of the hb service. */
    private String shortDesc;
    /** Hash code. */
    private final int hash;
    /** List of parameters. */
    private final List<String> parameters = new ArrayList<String>();
    /** List of required parameters. */
    private final List<String> paramRequired = new ArrayList<String>();
    /** List of parameters that are meta attributes. */
    private final List<String> paramIsMetaAttr = new ArrayList<String>();
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
    /** Name of the hb service in the pull down menus. */
    private final String menuName;
    /** Map that holds default values for operations. The keys are the name and
     * parameter. */
    private final MultiKeyMap opToDefault = new MultiKeyMap();
    /** Whether the parameter is a meta attribute. */

    /**
     * Prepares a new <code>HeartbeatService</code> object.
     */
    public HeartbeatService(final String name,
                            final String provider,
                            final String heartbeatClass) {
        this.name = name;
        this.provider = provider;
        this.heartbeatClass = heartbeatClass;
        hash = (name == null ? 0 : name.hashCode() * 31)
               + (heartbeatClass == null ? 0 : heartbeatClass.hashCode());
        if (isFilesystem()) {
            menuName = "Filesystem / DRBD";
        } else {
            if (!"heartbeat".equals(provider)) {
                menuName = name + ":" + provider;
            } else {
                menuName = name;
            }
        }
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
     * Returns the heartbeat class of the service, like ocf.
     */
    public final String getHeartbeatClass() {
        return heartbeatClass;
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
        final HeartbeatService other = getClass().cast(oth);
        return (name == null ? other.name == null : name.equals(other.name))
               && (heartbeatClass == null ? other.heartbeatClass == null
                                : heartbeatClass.equals(other.heartbeatClass));
    }

    /**
     * Sets heartbeat ocf script version for service.
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
    public final void addParameter(final String param) {
        parameters.add(param);
    }

    /**
     * Returns an array of all service parameters.
     */
    public final String[] getParameters() {
        return parameters.toArray(new String[parameters.size()]);
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
        if (!parameters.contains(param)) {
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
        if (!parameters.contains(param)) {
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
        if (!parameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramShortDesc.put(param, shortDesc);
    }

    /**
     * Sets the parameter type.
     */
    public final void setParamType(final String param, final String type) {
        if (!parameters.contains(param)) {
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
        if (!parameters.contains(param)) {
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
        if (!parameters.contains(param)) {
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
        if (!parameters.contains(param)) {
            wrongParameterError(param);
            return;
        }
        paramPossibleChoices.put(param, choices);
    }

    /**
     * Gets the array of the possible choices of the parameter.
     */
    public final String[] getParamPossibleChoices(final String param) {
        return paramPossibleChoices.get(param);
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
     * Sets whether the supplied parameter is meta attribute.
     */
    public final void setParamIsMetaAttr(final String param,
                                         final boolean isMetaAttr) {
        if (!parameters.contains(param)) {
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
     * Returns whether this service/object is group.
     */
    public final boolean isGroup() {
        return Tools.getConfigData().PM_GROUP_NAME.equals(name)
               && "group".equals(heartbeatClass);
    }

    /**
     * Returns whether this service/object is clone set.
     */
    public final boolean isClone() {
        return Tools.getConfigData().PM_CLONE_SET_NAME.equals(name)
               && "clone".equals(heartbeatClass);
    }


    /**
     * Returns whether this service is in the heartbeat class.
     */
    public final boolean isHeartbeatClass() {
        return "heartbeat".equals(heartbeatClass);
    }

    /**
     * Returns whether this service is in the ocf class.
     */
    public final boolean isOCFClass() {
        return "ocf".equals(heartbeatClass);
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
}
