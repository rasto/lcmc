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


package lcmc.drbd.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lcmc.common.domain.ResourceValue;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;

/**
 * This class holds data of one block device.
 */
public class BlockDevice extends ResourceValue {
    private static final Logger LOG = LoggerFactory.getLogger(BlockDevice.class);
    private final Host host;
    private final String deviceName;
    private static final Set<String> CONNECTED_STATES =
        Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("Connected",
                                                                      "SyncTarget",
                                                                      "SyncSource",
                                                                      "StartingSyncS",
                                                                      "StartingSyncT",
                                                                      "WFBitMapS",
                                                                      "WFBitMapT",
                                                                      "WFSyncUUID",
                                                                      "PausedSyncS",
                                                                      "PausedSyncT",
                                                                      "VerifyS",
                                                                      "VerifyT")));

    private static final Set<String> SYNCING_STATES =
        Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("SyncTarget",
                                                                      "SyncSource",
                                                                      "PausedSyncS",
                                                                      "PausedSyncT")));

    private String blockSize;
    private String diskUuid;
    private Collection<String> diskIds = new HashSet<String>();
    private String mountedOn;
    private String fsType;
    private boolean drbd = false;
    private boolean isUsedByCRM;
    private BlockDevice metaDisk = null;
    /** Block device of which this block device is a meta disk. */
    private List<BlockDevice> metaDiskOfBlockDevices = new ArrayList<BlockDevice>();
    private boolean splitBrain = false;
    private String connectionState = null;
    private String nodeState = null;
    private String diskState = null;
    private String nodeStateOther = null;
    private String diskStateOther = null;
    private String syncedProgress = null;
    private String drbdFlags = null;
    /** How much of the file system is used in percents. */
    private int used = -1;
    private String volumeGroup = null;
    private String vgOnPhysicalVolume = null;
    private String logicalVolume = null;
    private BlockDevice drbdBlockDevice = null;
    private String drbdBackingDisk = null;

    public BlockDevice(final Host host, final String device) {
        super(device);
        this.host = host;
        this.deviceName = device;
    }

    public void updateFrom(final BlockDevice blockDevice) {
        setDiskUuid(blockDevice.getDiskUuid());
        setBlockSize(blockDevice.getBlockSize());
        setMountedOn(blockDevice.getMountedOn());
        setFsType(blockDevice.getFsType());
        setVolumeGroup(blockDevice.getVolumeGroup());
        setLogicalVolume(blockDevice.getLogicalVolume());
        setVgOnPhysicalVolume(blockDevice.getVgOnPhysicalVolume());
        setDiskIds(blockDevice.getDiskIds());
    }

    public String getBlockSize() {
        return blockSize;
    }

    public String getMountedOn() {
        return mountedOn;
    }

    public String getFsType() {
        return fsType;
    }

    public boolean isMounted() {
        return mountedOn != null;
    }

    public boolean isDrbd() {
        return drbd;
    }

    /**
     * Returns how much of the file system is used in percents. -1 denotes that
     * there is no usage information.
     */
    public int getUsed() {
        return used;
    }

    /** Resets all drbd info. E.g. after status failure. */
    public void resetDrbd() {
        splitBrain = false;
        connectionState = null;
        nodeState       = null;
        diskState       = null;
        syncedProgress  = null;
        drbdFlags       = null;
    }

    public boolean isUsedByCRM() {
        return isUsedByCRM;
    }

    public boolean isDrbdMetaDisk() {
        return !metaDiskOfBlockDevices.isEmpty();
    }

    public List<BlockDevice> getMetaDiskOfBlockDevices() {
        return metaDiskOfBlockDevices;
    }

    /**
     * Returns whether this block device is available for
     * drbd. That is if this device is not mounted and is
     * not used by CRM.
     */
    public boolean isAvailable() {
        return !isMounted() && !isUsedByCRM && !isDrbdMetaDisk() && !isVolumeGroupOnPhysicalVolume();
    }


    /** Sets or unsets drbd flag. */
    public void setDrbd(final boolean drbd) {
        this.drbd = drbd;
        if (!drbd) {
            connectionState = null;
            nodeState = null;
            diskState = null;
            syncedProgress = null;
            drbdFlags = null;
            if (metaDisk != null) {
                metaDisk.removeMetadiskOfBlockDevice(this);
                metaDisk = null;
            }
            metaDiskOfBlockDevices = new ArrayList<BlockDevice>();
        }
    }

    /**
     * Returns whether the specified data are different than the stored data.
     */
    public boolean isDifferent(final String connectionState,
                               final String nodeState,
                               final String diskState,
                               final String drbdFlags) {
        return !Tools.areEqual(this.connectionState, connectionState)
               || !Tools.areEqual(this.nodeState, nodeState)
               || !Tools.areEqual(this.diskState, diskState)
               || !Tools.areEqual(this.drbdFlags, drbdFlags);
    }

    public void setUsedByCRM(final boolean isUsedByCRM) {
        this.isUsedByCRM = isUsedByCRM;
    }

    /**
     * Adds metaDiskOfBlockDevice which is a block device of which block device
     * is this meta disk.
     */
    void addMetaDiskOfBlockDevice(final BlockDevice metaDiskOfBlockDevice) {
        if (!metaDiskOfBlockDevices.contains(metaDiskOfBlockDevice)) {
            metaDiskOfBlockDevices.add(metaDiskOfBlockDevice);
        }
    }

    public void setMetaDisk(final BlockDevice metaDisk) {
        this.metaDisk = metaDisk;
        if (metaDisk != null) {
            metaDisk.addMetaDiskOfBlockDevice(this);
        }
    }

    public void removeMetadiskOfBlockDevice(final BlockDevice metaDiskOfBlockDevice) {
        metaDiskOfBlockDevices.remove(metaDiskOfBlockDevice);
    }

    public BlockDevice getMetaDisk() {
        return metaDisk;
    }

    public void setConnectionState(final String connectionState) {
        this.connectionState = connectionState;
    }

    public void setDrbdFlags(final String drbdFlags) {
        this.drbdFlags = drbdFlags;
    }

    public void setNodeState(final String nodeState) {
        this.nodeState = nodeState;
    }

    public void setNodeStateOther(final String nodeStateOther) {
        this.nodeStateOther = nodeStateOther;
    }

    public void setDiskState(final String diskState) {
        this.diskState = diskState;
    }

    /** Sets disk node state of the other block device as it is in /proc/drbd. */
    public void setDiskStateOther(final String diskStateOther) {
        this.diskStateOther = diskStateOther;
    }

    public void setDiskUuid(final String diskUuid) {
        this.diskUuid = diskUuid;
    }

    public void setBlockSize(final String blockSize) {
        this.blockSize = blockSize;
    }
    public void setMountedOn(final String mountedOn) {
        this.mountedOn = mountedOn;
    }
    public void setFsType(final String fsType) {
        this.fsType = fsType;
    }
    public void setVolumeGroup(final String volumeGroup) {
        this.volumeGroup = volumeGroup;
    }
    public void setLogicalVolume(final String logicalVolume) {
        this.logicalVolume = logicalVolume;

    }
    public void setVgOnPhysicalVolume(final String vgOnPhysicalVolume) {
        this.vgOnPhysicalVolume = vgOnPhysicalVolume;
    }

    public void setDiskIds(final Collection<String> diskIds) {
        this.diskIds = diskIds;
    }

    public String getConnectionState() {
        return connectionState;
    }

    public String getNodeState() {
        return nodeState;
    }

    public String getNodeStateOther() {
        return nodeStateOther;
    }

    public String getDiskState() {
        return diskState;
    }

    public String getDiskStateOther() {
        return diskStateOther;
    }

    public void setSyncedProgressInPercents(final String syncedProgress) {
        this.syncedProgress = syncedProgress;
    }

    public String getSyncedProgress() {
        return syncedProgress;
    }

    public boolean isAttached() {
        if (!drbd) {
            return true;
        }
        if (diskState == null) {
            return false;
        }
        return !"Diskless".equals(diskState);
    }

    public boolean isDiskless() {
        if (!drbd) {
            return false;
        }
        if (diskState == null) {
            return true;
        }
        return "Diskless".equals(diskState);
    }

    /**
     * Returns whether this block device is connected and resets the split
     * brain flag if it is.
     */
    public boolean isConnected() {
        if (connectionState == null) {
            return false;
        }
        if (CONNECTED_STATES.contains(connectionState)) {
            setSplitBrain(false);
            return true;
        }
        return false;
    }

    /** Returns whether the device is connected or is waiting for connection. */
    public boolean isConnectedOrWF() {
        return isWFConnection() || isConnected();
    }

    public boolean isWFConnection() {
        if (connectionState == null) {
            return false;
        }
        return "WFConnection".equals(connectionState);
    }

    public boolean isPrimary() {
        if (nodeState == null) {
            return false;
        }
        return "Primary".equals(nodeState);
    }

    public boolean isSecondary() {
        if (nodeState == null) {
            return false;
        }
        return "Secondary".equals(nodeState);
    }

    private boolean checkDrbdFlag(final int flag) {
        return drbdFlags.indexOf(flag) >= 0;
    }

    public boolean isPausedSync() {
        if (drbdFlags == null) {
            return false;
        }
        return checkDrbdFlag('u');
    }

    public boolean isSyncing() {
        if (nodeState == null) {
            syncedProgress = null;
            return false;
        }
        if (SYNCING_STATES.contains(connectionState)) {
            return true;
        }
        syncedProgress = null;
        return false;
    }

    public boolean isVerifying() {
        if (nodeState == null) {
            return false;
        }
        return "VerifyS".equals(connectionState) || "VerifyT".equals(connectionState);
    }


    /**
     * Returns true if this node is source of the data, false if this node gets
     * data from other node.
     */
    public boolean isSyncSource() {
        if (connectionState == null) {
            return false;
        }
        return "SyncSource".equals(connectionState);
    }

    /** Returns true if this node is target for the data, otherwise false. */
     public boolean isSyncTarget() {
        if (connectionState == null) {
            return false;
        }
        return "SyncTarget".equals(connectionState);
    }

    public void setSplitBrain(final boolean splitBrain) {
        this.splitBrain = splitBrain;
    }

    public boolean isSplitBrain() {
        return splitBrain;
    }

    /** Returns string with meta disk and index in the parenthesis. */
    public String getMetaDiskString(final String md, final String mdi) {
        if (md == null || mdi == null) {
            return null;
        }
        final StringBuilder metaDiskString = new StringBuilder();
        if ("Flexible".equals(mdi)) {
            metaDiskString.append("flexible-meta-disk\t");
            metaDiskString.append(md);
        } else {
            metaDiskString.append("meta-disk\t");
            metaDiskString.append(md);
            if (!"internal".equals(md)) {
                metaDiskString.append('[');
                metaDiskString.append(mdi);
                metaDiskString.append(']');
            }
        }
        return metaDiskString.toString();
    }

    /** Returns string with stored meta disk and index in the parenthesis. */
    String getMetaDiskString() {
        return getMetaDiskString(getValue("DrbdMetaDisk").getValueForConfig(),
                                 getValue("DrbdMetaDiskIndex").getValueForConfig());
    }

    public String getSection(final String parameter) {
        return Tools.getString("BlockDevice.MetaDiskSection");
    }

    public boolean isSwap() {
        return fsType != null && "swap".equals(fsType);
    }

    public String getDiskUuid() {
        return diskUuid;
    }

    public Collection<String> getDiskIds() {
        return diskIds;
    }

    public String getVolumeGroup() {
        return volumeGroup;
    }

    public boolean isPhysicalVolume() {
        return vgOnPhysicalVolume != null;
    }

    public String getVgOnPhysicalVolume() {
        return vgOnPhysicalVolume;
    }

    public boolean isVolumeGroupOnPhysicalVolume() {
        return vgOnPhysicalVolume != null && !"".equals(vgOnPhysicalVolume);
    }

    /** Set volume group that is on this physical volume.
     * "", for no VG, but still it's physical volume. */
    public void setVolumeGroupOnPhysicalVolume(final String vgOnPhysicalVolume) {
        this.vgOnPhysicalVolume = vgOnPhysicalVolume;
    }

    public String getLogicalVolume() {
        return logicalVolume;
    }

    public BlockDevice getDrbdBlockDevice() {
        return drbdBlockDevice;
    }

    public boolean isDrbdPhysicalVolume() {
        return drbdBlockDevice != null && drbdBlockDevice.isPhysicalVolume();
    }

    public boolean isDrbdVolumeGroupOnPhysicalVolume() {
        return drbdBlockDevice != null && drbdBlockDevice.isVolumeGroupOnPhysicalVolume();
    }

    public void setDrbdBlockDevice(final BlockDevice drbdBlockDevice) {
        this.drbdBlockDevice = drbdBlockDevice;
    }

    public String getDrbdBackingDisk() {
        return drbdBackingDisk;
    }

    public void setDrbdBackingDisk(final String drbdBackingDisk) {
        this.drbdBackingDisk = drbdBackingDisk;
    }

    public void setUsed(final String usedStr) {
        if (usedStr != null) {
            this.used = Integer.parseInt(usedStr);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlockDevice that = (BlockDevice) o;

        if (!deviceName.equals(that.deviceName)) return false;
        if (!host.equals(that.host)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = deviceName.hashCode();
        result = 31 * result + host.hashCode();
        return result;
    }
}
