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


package lcmc.data.resources;

import lcmc.data.StringValue;

/**
 * This class holds data of one drbd volumes.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class DrbdVolume extends Resource
implements ClusterBlockDeviceInterface {
    /** DRBD device. */
    private String device;
    /** Whether the config for this volume was already written at least once.
     */
    private boolean commited = false;

    /**
     * Prepares a new {@code DrbdVolume} object.
     */
    public DrbdVolume(final String name) {
        super(name);
    }

    /** Returns drbd device. */
    @Override
    public String getDevice() {
        return device;
    }

    /** Sets drbd device. */
    public void setDevice(final String device) {
        this.device = device;
        setValue("device", new StringValue(device));
    }

    /**
     * Sets commited flag. Resource is commited after the config
     * was generated and device cannot be changed.
     */
    public void setCommited(final boolean commited) {
        this.commited = commited;
    }

    /** Returns whether this resoure was commited. */
    public boolean isCommited() {
        return commited;
    }
}
