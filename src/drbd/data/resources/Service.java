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


package drbd.data.resources;

import drbd.utilities.Tools;

/**
 * This class holds data of a service.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class Service extends Resource {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Id is heartbeatId whithout name of the service. */
    private String id = null;
    /** Heartbeat id of this service. */
    private String heartbeatId = null;
    /** Whether the service is newly allocated. */
    private boolean newService = false;
    /** Whether the service is removed. */
    private boolean removed = false;
    /** Whether the service is being removed. */
    private boolean removing = false;
    /** Whether the service is modified. */
    private boolean modified = false;
    /** Whether the service is being modified. */
    private boolean modifying = false;
    /** Heartbeat class:  heartbeat, ocf, lsb. */
    private String resourceClass = null;
    /** Whether this service master when it is clone. */
    private boolean master = false;
    /** Heartbeat id prefix for resource. */
    public static final String RES_ID_PREFIX = "res_";
    /** Heartbeat id prefix for group. */
    public static final String GRP_ID_PREFIX = "grp_";
    /** Pacemaker id prefix for clone. */
    public static final String CL_ID_PREFIX = "cl_";
    /** Pacemaker id prefix for master/slave. */
    public static final String MS_ID_PREFIX = "ms_";
    /** Name of the clone set pacemaker object. */
    private static final String CLONE_SET_NAME =
                                Tools.getConfigData().PM_CLONE_SET_NAME;
    /** Name of the master / slave set pacemaker object. */
    private static final String MASTER_SLAVE_SET_NAME =
                                Tools.getConfigData().PM_MASTER_SLAVE_SET_NAME;
    /** Name of the group pacemaker object. */
    private static final String GROUP_NAME =
                                           Tools.getConfigData().PM_GROUP_NAME;

    /**
     * Prepares a new <code>Service</code> object.
     *
     * @param name
     *          name of this service.
     */
    public Service(final String name) {
        super(name);
    }

    /**
     * Returns id that is used in the heartbeat for this service.
     */
    public final String getHeartbeatId() {
        return heartbeatId;
    }

    /**
     * Returns id of the service. This is usually heartbeat id without service
     * name and underscore part.
     */
    public final String getId() {
        return id;
    }

    /**
     * Sets heartbeat id and gui id without the service name part.
     */
    public final void setHeartbeatId(final String heartbeatId) {
        this.heartbeatId = heartbeatId;
        if (GROUP_NAME.equals(getName())) {
            if (heartbeatId.equals(GRP_ID_PREFIX)) {
                id = "";
            } else if (heartbeatId.startsWith(GRP_ID_PREFIX)) {
                id = heartbeatId.substring(GRP_ID_PREFIX.length());
            } else {
                id = heartbeatId;
            }
        } else if (CLONE_SET_NAME.equals(getName())) {
            if (heartbeatId.equals(CL_ID_PREFIX)) {
                id = "";
            } else if (heartbeatId.startsWith(CL_ID_PREFIX)) {
                id = heartbeatId.substring(CL_ID_PREFIX.length());
            } else {
                id = heartbeatId;
            }
        } else if (MASTER_SLAVE_SET_NAME.equals(getName())) {
            if (heartbeatId.equals(MS_ID_PREFIX)) {
                id = "";
            } else if (heartbeatId.startsWith(MS_ID_PREFIX)) {
                id = heartbeatId.substring(MS_ID_PREFIX.length());
            } else {
                id = heartbeatId;
            }
        } else {
            if (heartbeatId.startsWith(RES_ID_PREFIX + getName() + "_")) {
                id = heartbeatId.substring((RES_ID_PREFIX + getName()).length()
                                           + 1);
            } else {
                id = heartbeatId;
            }
        }
        setValue("id", id);
    }

    /**
     * Sets the id and heartbeat id.
     */
    public final void setId(final String id) {
        this.id = id;
        if (GROUP_NAME.equals(getName())) {
            if (id.startsWith(GRP_ID_PREFIX)) {
                heartbeatId = id;
            } else {
                heartbeatId = GRP_ID_PREFIX + id;
            }
        } else if (CLONE_SET_NAME.equals(getName())) {
            if (id.startsWith(CL_ID_PREFIX)) {
                heartbeatId = id;
            } else {
                heartbeatId = CL_ID_PREFIX + id;
            }
        } else if (MASTER_SLAVE_SET_NAME.equals(getName())) {
            if (id.startsWith(MS_ID_PREFIX)) {
                heartbeatId = id;
            } else {
                heartbeatId = MS_ID_PREFIX + id;
            }
        } else {
            if (id.startsWith(RES_ID_PREFIX + getName() + "_")) {
                heartbeatId = id;
            } else {
                heartbeatId = RES_ID_PREFIX + getName() + "_" + id;
            }
        }
        setValue("id", id);
    }

    /**
     * Sets whether the service is newly allocated.
     */
    public final void setNew(final boolean newService) {
        this.newService = newService;
    }

    /**
     * Returns whether the service is newly allocated.
     */
    public final boolean isNew() {
        return newService;
    }

    /**
     * Sets whether the service was removed.
     */
    public final void setRemoved(final boolean removed) {
        this.removed = removed;
        if (removed) {
            removing = true;
        }
    }

    /**
     * Returns whether the service was removed.
     */
    public final boolean isRemoved() {
        return removed || removing;
    }

    /**
     * Sets that the service was done being removed.
     */
    public final void doneRemoving() {
        this.removing = removing;
    }

    /**
     * Sets whether the service was modified.
     */
    public final void setModified(final boolean modified) {
        this.modified = modified;
        if (modified) {
            modifying = true;
        }
    }

    /**
     * Sets that the service is done being modified.
     */
    public final void doneModifying() {
        modifying = false;
    }

    /**
     * Makes the service available, after the status as seen in the gui was
     * confirmed from the heartbeat.
     */
    public final void setAvailable() {
        newService      = false;
        modified = false;
        removed  = false;
    }

    /**
     * Returns whether the service is available. It is not available if it was
     * just created, it was just removed or modified.
     */
    public final boolean isAvailable() {
        return !newService && !modified && !removed && !modifying && !removing;
    }

    /**
     * Sets heartbeat resource class heartbeat (old style), ocf, lsb (from
     * init.d).
     */
    public final void setResourceClass(final String resourceClass) {
        this.resourceClass = resourceClass;
    }

    /**
     * Returns the heartbeat class of this service.
     */
    public final String getResourceClass() {
        return resourceClass;
    }

    /**
     * Sets this service if it is master.
     */
    public final void setMaster(final boolean master) {
        this.master = master;
    }

    /**
     * Returns whether this clone is master, if it is clone.
     */
    public final boolean isMaster() {
        return master;
    }
}
