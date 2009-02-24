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

/**
 * This class holds data that were parsed from cib xml. This is not used in old
 * heartbeats before pacemaker.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class CibQuery {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** crm_config / global config data */
    private Map<String,String> crmConfig;
    /** map with resources and its parameters. */
    private Map<String,Map<String,String>> parameters;

    /**
     * Sets crm config map.
     */
    public final void setCrmConfig(Map<String,String> crmConfig) {
        this.crmConfig = crmConfig;
    }

    /**
     * Returns crm config.
     */
    public final Map<String,String> getCrmConfig() {
        return crmConfig;
    }

    /**
     * Sets parameters map, with the first key being the resource id and the
     * second key being the parameter name.
     */
    public final void setParameters(Map<String,Map<String,String>> parameters) {
        this.parameters = parameters;
    }

    /**
     * Returns the parameters map.
     */
    public final Map<String,Map<String,String>> getParameters() {
        return parameters;
    }
}
