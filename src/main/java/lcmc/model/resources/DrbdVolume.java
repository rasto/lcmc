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


package lcmc.model.resources;

import lcmc.model.StringValue;

/**
 * This class holds data of one drbd volumes.
 */
public final class DrbdVolume extends Resource implements ClusterBlockDeviceInterface {
    private String drbdDevice;
    private boolean commited = false;

    public DrbdVolume(final String name) {
        super(name);
    }

    @Override
    public String getDeviceName() {
        return drbdDevice;
    }

    public void setDrbdDevice(final String drbdDevice) {
        this.drbdDevice = drbdDevice;
        setValue("device", new StringValue(drbdDevice));
    }

    public void setCommited(final boolean commited) {
        this.commited = commited;
    }

    public boolean isCommited() {
        return commited;
    }
}
