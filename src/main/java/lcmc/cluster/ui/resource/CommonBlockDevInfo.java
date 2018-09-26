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

import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.crm.ui.resource.HbCategoryInfo;
import lcmc.crm.ui.resource.ServiceInfo;
import lcmc.drbd.domain.BlockDevice;
import lcmc.drbd.domain.CommonBlockDevice;
import lcmc.drbd.ui.resource.BlockDevInfo;

import javax.inject.Named;
import javax.swing.*;

/**
 * This class holds info data for a block device that is common
 * in all hosts in the cluster and can be chosen in the scrolling list in
 * the filesystem service.
 */
public class CommonBlockDevInfo extends HbCategoryInfo implements CommonDeviceInterface {
    /** block devices of this common block device on all nodes. */
    private BlockDevice[] blockDevices;
    private CommonBlockDevice commonBlockDevice;

    public void init(final String name, final BlockDevice[] blockDevices, final Browser browser) {
        super.init(name, browser);
        commonBlockDevice = new CommonBlockDevice(name);
        this.blockDevices = blockDevices;
    }

    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        return BlockDevInfo.HARDDISK_ICON;
    }

    @Override
    public String getDevice() {
        return getCommonBlockDevice().getDeviceName();
    }

    @Override
    public String getInfo() {
        return "Device    : " + getCommonBlockDevice().getName() + '\n';
    }

    /**
     * Returns string representation of the block devices, used in the pull
     * down menu.
     */
    @Override
    public String toString() {
        String name = getName();
        if (name == null || name.isEmpty()) {
            name = Tools.getString("ClusterBrowser.CommonBlockDevUnconfigured");
        }
        return name;
    }

    @Override
    public void setUsedByCRM(final ServiceInfo isUsedByCRM) {
        for (final BlockDevice bd : blockDevices) {
            bd.setUsedByCRM(isUsedByCRM != null);
        }
    }

    /**
     * TODO: or any is used by hb?
     */
    @Override
    public boolean isUsedByCRM() {
        boolean is = true;
        for (final BlockDevice blockDevice : blockDevices) {
            is = is && blockDevice.isUsedByCRM();
        }
        return is;
    }

    CommonBlockDevice getCommonBlockDevice() {
        return commonBlockDevice;
    }

    @Override
    public String getLastCreatedFs() {
        return null;
    }

    /** Returns how much of the filesystem is used. */
    @Override
    public int getUsed() {
        int used = -1;
        for (final BlockDevice bd : blockDevices) {
            if (bd.getUsed() > used) {
                used = bd.getUsed();
            }
        }
        return used;
    }
}
