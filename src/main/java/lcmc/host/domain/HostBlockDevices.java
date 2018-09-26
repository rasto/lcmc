/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.host.domain;

import com.google.common.base.Optional;
import lcmc.drbd.domain.BlockDevice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HostBlockDevices {
    private Set<BlockDevice> blockDevices = new LinkedHashSet<BlockDevice>();
    private ConcurrentMap<String, BlockDevice> byName = new ConcurrentHashMap<String, BlockDevice>();

    public Set<BlockDevice> getBlockDevices() {
        return new LinkedHashSet<BlockDevice>(blockDevices);
    }

    public void setBlockDevices(final Collection<BlockDevice> newBlockDevices) {
        for (final BlockDevice newBlockDevice : newBlockDevices) {
            blockDevices.add(newBlockDevice);
            byName.put(newBlockDevice.getName(), newBlockDevice);
        }
        for (final BlockDevice oldBlockDevice : new HashSet<BlockDevice>(blockDevices)) {
            if (!newBlockDevices.contains(oldBlockDevice)) {
                blockDevices.remove(oldBlockDevice);
                byName.remove(oldBlockDevice);
            }
        }
    }

    /**
     * Returns blockDevices as array list of device names. Removes the
     * ones that are in the drbd and are already used in CRM.
     */
    public List<String> getBlockDevicesNames() {
        final List<String> blockDevicesNames = new ArrayList<String>();
        for (final String bdName : byName.keySet()) {
            final BlockDevice bd = byName.get(bdName);
            if (!bd.isDrbd() && !bd.isUsedByCRM()) {
                blockDevicesNames.add(bdName);
            }
        }
        return blockDevicesNames;
    }

    /**
     * Returns blockDevices as array list of device names.
     *
     * @param otherBlockDevices
     *          list of block devices with which the intersection with
     *          block devices of this host is made.
     *
     */
    public Optional<List<String>> getBlockDevicesNamesIntersection(final Optional<List<String>> otherBlockDevices) {
        if (!otherBlockDevices.isPresent()) {
            return Optional.of(getBlockDevicesNames());
        }
        final List<String> blockDevicesIntersection = new ArrayList<String>();
        for (final String otherBlockDevice : otherBlockDevices.get()) {
            final BlockDevice blockDevice = byName.get(otherBlockDevice);
            if (blockDevice != null && !blockDevice.isDrbd()) {
                blockDevicesIntersection.add(otherBlockDevice);
            }
        }
        return Optional.of(blockDevicesIntersection);
    }

    public void resetDrbdOnBlockDevices(final boolean drbdStatus) {
        if (!drbdStatus) {
            for (final BlockDevice blockDevice : getBlockDevices()) {
                blockDevice.resetDrbd();
            }
        }
    }

    public Optional<BlockDevice> getBlockDeviceByName(final String device) {
        return Optional.fromNullable(byName.get(device));
    }

    public void setDiskSpace(final Map<String, String> diskSpaces) {
        for (final Map.Entry<String, String> entry : diskSpaces.entrySet()) {
            final BlockDevice blockDevice = byName.get(entry.getKey());
            if (blockDevice != null) {
                blockDevice.setUsed(entry.getValue());
            }
        }
    }
}
