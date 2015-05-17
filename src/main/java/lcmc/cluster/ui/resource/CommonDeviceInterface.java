/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
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
package lcmc.cluster.ui.resource;

import lcmc.common.domain.Value;
import lcmc.crm.ui.resource.ServiceInfo;

/**
 * This interface provides getDevice function for DRBD block devices or block
 * devices that don't have DRBD over them but are used by CRM.
 */
public interface CommonDeviceInterface extends Value {
    String getName();
    String getDevice();
    void setUsedByCRM(ServiceInfo isUsedByCRM);
    boolean isUsedByCRM();
    String getLastCreatedFs();
    /** Returns how much of the device is used. */
    int getUsed();
}
