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
package lcmc.gui.resources;

import lcmc.gui.Browser;
import lcmc.data.resources.BlockDevice;
import lcmc.data.resources.CommonBlockDevice;
import lcmc.utilities.Tools;
import javax.swing.ImageIcon;

/**
 * This class holds info data for a block device that is common
 * in all hosts in the cluster and can be chosen in the scrolling list in
 * the filesystem service.
 */
public final class CommonBlockDevInfo extends HbCategoryInfo
                                      implements CommonDeviceInterface {
    /** block devices of this common block device on all nodes. */
    private final BlockDevice[] blockDevices;

    /** Prepares a new <code>CommonBlockDevInfo</code> object. */
    public CommonBlockDevInfo(final String name,
                              final BlockDevice[] blockDevices,
                              final Browser browser) {
        super(name, browser);
        setResource(new CommonBlockDevice(name));
        this.blockDevices = blockDevices;
    }

    /** Returns icon for common block devices menu category. */
    @Override public ImageIcon getMenuIcon(final boolean testOnly) {
        return BlockDevInfo.HARDDISK_ICON;
    }

    /** Returns device name of this block device. */
    @Override public String getDevice() {
        return getCommonBlockDevice().getDevice();
    }

    /** Returns info for this block device. */
    @Override String getInfo() {
        return "Device    : " + getCommonBlockDevice().getName() + "\n";
    }

    /**
     * Returns string representation of the block devices, used in the pull
     * down menu.
     */
    @Override public String toString() {
        String name = getName();
        if (name == null || "".equals(name)) {
            name = Tools.getString("ClusterBrowser.CommonBlockDevUnconfigured");
        }
        return name;
    }

    /** Sets this block device on all nodes ass used by crm. */
    @Override public void setUsedByCRM(final ServiceInfo isUsedByCRM) {
        for (BlockDevice bd : blockDevices) {
            bd.setUsedByCRM(isUsedByCRM != null);
        }
    }

    /**
     * Returns if all of the block devices are used by crm.
     * TODO: or any is used by hb?
     */
    @Override public boolean isUsedByCRM() {
        boolean is = true;
        for (int i = 0; i < blockDevices.length; i++) {
            is = is && blockDevices[i].isUsedByCRM();
        }
        return is;
    }

    /** Returns resource object of this block device. */
    CommonBlockDevice getCommonBlockDevice() {
        return (CommonBlockDevice) getResource();
    }

    /** Returns the last created filesystem. */
    @Override public String getCreatedFs() {
        return null;
    }

    /** Returns how much of the filesystem is used. */
    @Override public int getUsed() {
        int used = -1;
        for (BlockDevice bd : blockDevices) {
            if (bd.getUsed() > used) {
                used = bd.getUsed();
            }
        }
        return used;
    }
}
