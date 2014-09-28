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

import lcmc.common.domain.Application;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Resource;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class holds data of a service.
 */
public class Service extends Resource {
    private static final Logger LOG = LoggerFactory.getLogger(Service.class);
    public static final String RES_ID_PREFIX = "res_";
    public static final String STONITH_ID_PREFIX = "stonith_";
    public static final String GRP_ID_PREFIX = "grp_";
    public static final String CL_ID_PREFIX = "cl_";
    public static final String MS_ID_PREFIX = "ms_";
    private static final String CLONE_SET_NAME = Application.PM_CLONE_SET_NAME;
    private static final String MASTER_SLAVE_SET_NAME = Application.PM_MASTER_SLAVE_SET_NAME;
    private static final String GROUP_NAME = Application.PACEMAKER_GROUP_NAME;
    private String id = null;
    private String crmId = null;
    private boolean removed = false;
    private boolean removing = false;
    private boolean modified = false;
    private boolean modifying = false;
    private boolean orphaned = false;
    /** Heartbeat class:  heartbeat, ocf, service (upstart, systemd). */
    private String resourceClass = null;
    private boolean master = false;
    private boolean stonith = false;

    public Service(final String name) {
        super(name);
    }

    public final String getCrmId() {
        return crmId;
    }

    /**
     * Returns id of the service. This is usually heartbeat id without service
     * name and underscore part.
     */
    public final String getId() {
        return id;
    }

    /** Sets heartbeat id and gui id without the service name part. */
    public final void setCrmId(final String crmId) {
        this.crmId = crmId;
        LOG.debug1("setHeartbeatId: set crm id: " + crmId);
        if (GROUP_NAME.equals(getName())) {
            if (crmId.equals(GRP_ID_PREFIX)) {
                id = "";
            } else if (crmId.startsWith(GRP_ID_PREFIX)) {
                id = crmId.substring(GRP_ID_PREFIX.length());
            } else {
                id = crmId;
            }
        } else if (CLONE_SET_NAME.equals(getName())) {
            if (crmId.equals(CL_ID_PREFIX)) {
                id = "";
            } else if (crmId.startsWith(CL_ID_PREFIX)) {
                id = crmId.substring(CL_ID_PREFIX.length());
            } else {
                id = crmId;
            }
        } else if (MASTER_SLAVE_SET_NAME.equals(getName())) {
            if (crmId.equals(MS_ID_PREFIX)) {
                id = "";
            } else if (crmId.startsWith(MS_ID_PREFIX)) {
                id = crmId.substring(MS_ID_PREFIX.length());
            } else {
                id = crmId;
            }
        } else {
            if (crmId.startsWith(RES_ID_PREFIX + getName() + '_')) {
                id = crmId.substring((RES_ID_PREFIX + getName()).length() + 1);
            } else if (crmId.startsWith(STONITH_ID_PREFIX + getName() + '_')) {
                id = crmId.substring((STONITH_ID_PREFIX + getName()).length() + 1);
            } else {
                id = crmId;
            }
        }
        LOG.debug1("setHeartbeatId: set crm id: " + crmId + ", id: " + id);
        setValue("id", new StringValue(id));
    }

    public final void setId(final String id) {
        this.id = id;
        LOG.debug1("setId: id: " + id);
    }

    /** Returns crm id from entered id. */
    public final String getCrmIdFromId(final String id) {
        if (GROUP_NAME.equals(getName())) {
            if (id.startsWith(GRP_ID_PREFIX)) {
                return id;
            } else {
                return GRP_ID_PREFIX + id;
            }
        } else if (CLONE_SET_NAME.equals(getName())) {
            if (id.startsWith(CL_ID_PREFIX)) {
                return id;
            } else {
                return CL_ID_PREFIX + id;
            }
        } else if (MASTER_SLAVE_SET_NAME.equals(getName())) {
            if (id.startsWith(MS_ID_PREFIX)) {
                return id;
            } else {
                return MS_ID_PREFIX + id;
            }
        } else {
            if (id.startsWith(RES_ID_PREFIX + getName() + '_')) {
                return id;
            } else if (id.startsWith(STONITH_ID_PREFIX + getName() + '_')) {
                return id;
            } else if (stonith) {
                return STONITH_ID_PREFIX + getName() + '_' + id;
            } else {
                return RES_ID_PREFIX + getName() + '_' + id;
            }
        }
    }

    public final void setIdAndCrmId(final String id) {
        this.id = id;
        crmId = getCrmIdFromId(id);
        setValue("id", new StringValue(id));
    }

    public final void setRemoved(final boolean removed) {
        this.removed = removed;
        if (removed) {
            removing = true;
        }
    }

    public final boolean isRemoved() {
        return removed || removing;
    }

    public final void doneRemoving() {
        this.removing = false;
    }

    public final void setModified(final boolean modified) {
        this.modified = modified;
        if (modified) {
            modifying = true;
        }
    }

    public final void doneModifying() {
        modifying = false;
    }

    /**
     * Makes the service available, after the status as seen in the gui was
     * confirmed from the heartbeat.
     */
    public final void setAvailable() {
        setNew(false);
        modified = false;
        removed  = false;
    }

    /**
     * Returns whether the service is available. It is not available if it was
     * just created, it was just removed or modified.
     */
    public final boolean isAvailable() {
        return !isNew() && !modified && !removed && !modifying && !removing && !orphaned;
    }

    /**
     * Returns whether the service is available with text why it isn't, null if
     * it is.
     */
    public final String isAvailableWithText() {
        if (isNew()) {
            return "it is not applied yet";
        } else if (modified) {
            return "it is being modified";
        } else if (removed) {
            return "it is being removed";
        } else if (modifying) {
            return "it is being modified";
        } else if (removing) {
            return "it is being removed";
        } else if (orphaned) {
            return "cannot do that to an orphan";
        }
        return null;
    }

    /**
     * Sets heartbeat resource class heartbeat (old style), ocf, lsb (from
     * init.d), service (upstart, systemd).
     */
    public final void setResourceClass(final String resourceClass) {
        this.resourceClass = resourceClass;
    }

    /** Returns the heartbeat class of this service. */
    public final String getResourceClass() {
        return resourceClass;
    }

    public void setMaster(final boolean master) {
        this.master = master;
    }

    public boolean isMaster() {
        return master;
    }

    public final void setStonith(final boolean stonith) {
        this.stonith = stonith;
    }

    public final boolean isOrphaned() {
        return orphaned;
    }

    public final void setOrphaned(final boolean orphaned) {
        this.orphaned = orphaned;
    }
}
