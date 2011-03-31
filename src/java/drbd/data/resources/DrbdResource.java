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

/**
 * This class holds data of one drbd resource.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class DrbdResource extends Resource
implements ClusterBlockDeviceInterface {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Drbd device. */
    private String device;
    /** Whether the config for this resource was already written at least once.
     */
    private boolean commited = false;

    /**
     * Prepares a new <code>DrbdResource</code> object.
     *
     * @param name
     *          drbd resource name
     * @param device
     *          drbd device
     */
    public DrbdResource(final String name,
                        final String device) {
        super(name);
        this.device = device;
        setValue("device", device);
    }

    /** Returns drbd device. */
    @Override public String getDevice() {
        return device;
    }

    /** Sets drbd device. */
    public void setDevice(final String device) {
        this.device = device;
        setValue("device", device);
    }

    /**
     * Sets commited flag. Resource is commited after the config
     * was generated and niether resource name nor drbd device can
     * be changed.
     */
    public void setCommited(final boolean commited) {
        this.commited = commited;
    }

    /** Returns whether this resoure was commited. */
    public boolean isCommited() {
        return commited;
    }
}
