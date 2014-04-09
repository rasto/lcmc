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

import lcmc.utilities.Tools;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class holds data of one block device.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class BlockDevice extends Resource {
    /** Logger. */
    private static final Logger LOG =
                                  LoggerFactory.getLogger(BlockDevice.class);
    /** Block size in blocks. */
    private String blockSize;
    /** Disk UUID. */
    private String diskUuid;
    /** Disk ID. */
    private final Collection<String> diskIds = new HashSet<String>();
    /** Where is this block device mounted if it is at all. */
    private String mountedOn;
    /** Filesytem type. */
    private String fsType;
    /** Whether this device is drbd device. */
    private boolean drbd = false;
    /** Whether this block device is used by crm in Filesystem service. */
    private boolean isUsedByCRM;
    /** Drbd meta disk of this block device. */
    private BlockDevice metaDisk = null;
    /** Block device of which this block device is a meta disk. */
    private List<BlockDevice> metaDiskOfBlockDevices =
                                                new ArrayList<BlockDevice>();
    /**
     * Whether the block device is in drbd split-brain situation. */
    private boolean splitBrain = false;
    /** The connection state. */
    private String connectionState = null;
    /** The node state. */
    private String nodeState = null;
    /** The disk state. */
    private String diskState = null;
    /** The node state of the other bd. */
    private String nodeStateOther = null;
    /** The disk state of the other bd. */
    private String diskStateOther = null;
    /** The sync progress in percent as String. */
    private String syncedProgress = null;
    /** Drbd flags. */
    private String drbdFlags = null;
    /** How much of the file system is used in percents. */
    private int used = -1;
    /** LVM group. */
    private String volumeGroup = null;
    /** VG on physical volume. */
    private String vgOnPhysicalVolume = null;
    /** Logical volume. */
    private String logicalVolume = null;
    /** DRBD block device. */
    private BlockDevice drbdBlockDevice = null;
    /* Backing disk that is used in drbd config. */
    private String drbdBackingDisk = null;
    /** States that means that we are connected. */
    private static final Set<String> CONNECTED_STATES =
        Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                                                           "Connected",
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

    /** Syncing states. */
    private static final Set<String> SYNCING_STATES =
        Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                                                            "SyncTarget",
                                                            "SyncSource",
                                                            "PausedSyncS",
                                                            "PausedSyncT")));

    private static final String TOKEN_UUID    = "uuid";
    private static final String TOKEN_SIZE    = "size";
    private static final String TOKEN_MP      = "mp";
    private static final String TOKEN_FS      = "fs";
    private static final String TOKEN_VG      = "vg";
    private static final String TOKEN_LV      = "lv";
    private static final String TOKEN_PV      = "pv";
    private static final String TOKEN_DISK_ID = "disk-id";

    /**
     * Creates a new {@code BlockDevice} object.
     *
     * @param line
     *          line that contains device, blocksize, mount
     *          point and fs type delimited with space
     */
    public BlockDevice(final String line) {
        super();
        update(line);
    }

    /** Updates the block device. */
    public void update(final String line) {
        final Pattern p = Pattern.compile("([^:]+):(.*)");
        final String[] cols = line.split(" ");
        if (cols.length < 2) {
            LOG.appWarning("update: cannot parse line: " + line);
        } else {
            final String device = cols[0];
            setName(device);
            final Map<String, String> tokens = new HashMap<String, String>();
            for (int i = 1; i < cols.length; i++) {
                final Matcher m = p.matcher(cols[i]);
                if (m.matches()) {
                    if (TOKEN_DISK_ID.equals(m.group(1))) {
                        diskIds.add(m.group(2));
                    } else {
                        tokens.put(m.group(1), m.group(2));
                    }
                } else {
                    LOG.appWarning("update: could not parse: " + line);
                }
            }
            this.diskUuid           = tokens.get(TOKEN_UUID);
            this.blockSize          = tokens.get(TOKEN_SIZE);
            this.mountedOn          = tokens.get(TOKEN_MP);
            this.fsType             = tokens.get(TOKEN_FS);
            this.volumeGroup        = tokens.get(TOKEN_VG);
            this.logicalVolume      = tokens.get(TOKEN_LV);
            this.vgOnPhysicalVolume = tokens.get(TOKEN_PV);
        }
    }

    /** Returns block size. */
    public String getBlockSize() {
        return blockSize;
    }

    /** Returns mount point. */
    public String getMountedOn() {
        return mountedOn;
    }

    /** Returns file system type. */
    public String getFsType() {
        return fsType;
    }

    /** Returns whether this block device is mounted. */
    public boolean isMounted() {
        return mountedOn != null;
    }

    /** Returns true if this device is drbd device. */
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

    /** Returns true if this device is used by CRM. */
    public boolean isUsedByCRM() {
        return isUsedByCRM;
    }

    /** Returns true if this device is used as drbd meta-disk. */
    public boolean isDrbdMetaDisk() {
        return !metaDiskOfBlockDevices.isEmpty();
    }

    /** Returns the block devices of which this block device is a meta disk. */
    public List<BlockDevice> getMetaDiskOfBlockDevices() {
        return metaDiskOfBlockDevices;
    }

    /**
     * Returns whether this block device is available for
     * drbd. That is if this device is not mounted and is
     * not used by CRM.
     */
    public boolean isAvailable() {
        return !isMounted()
               && !isUsedByCRM
               && !isDrbdMetaDisk()
               && !isVolumeGroupOnPhysicalVolume();
    }


    /** Sets or unsets drbd flag. */
    public void setDrbd(final boolean drbd) {
        this.drbd = drbd;
        if (!drbd) {
            connectionState       = null;
            nodeState             = null;
            diskState             = null;
            syncedProgress        = null;
            drbdFlags             = null;
            if (metaDisk != null) {
                metaDisk.removeMetadiskOfBlockDevice(this);
                metaDisk              = null;
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


    /** Sets this device used by CRM flag. */
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

    /** Sets meta disk. */
    public void setMetaDisk(final BlockDevice metaDisk) {
        this.metaDisk = metaDisk;
        if (metaDisk != null) {
            metaDisk.addMetaDiskOfBlockDevice(this);
        }
    }

    /** Removes the meta disk info. */
    public void removeMetadiskOfBlockDevice(
                                    final BlockDevice metaDiskOfBlockDevice) {
        metaDiskOfBlockDevices.remove(metaDiskOfBlockDevice);
    }

    /** Returns meta-disk. */
    public BlockDevice getMetaDisk() {
        return metaDisk;
    }

    /** Sets drbd connection state as it is in /proc/drbd. */
    public void setConnectionState(final String connectionState) {
        this.connectionState = connectionState;
    }

    /** Sets all drbd flags at once. */
    public void setDrbdFlags(final String drbdFlags) {
        this.drbdFlags = drbdFlags;
    }

    /** Sets drbd node state as it is in /proc/drbd. */
    public void setNodeState(final String nodeState) {
        this.nodeState = nodeState;
    }

    /** Sets drbd node state of the other bd as it is in /proc/drbd. */
    public void setNodeStateOther(final String nodeStateOther) {
        this.nodeStateOther = nodeStateOther;
    }

    /** Sets disk node state as it is in /proc/drbd. */
    public void setDiskState(final String diskState) {
        this.diskState = diskState;
    }

    /** Sets disk node state of the other block device as it is in /proc/drbd.
     */
    public void setDiskStateOther(final String diskStateOther) {
        this.diskStateOther = diskStateOther;
    }

    /** Returns connection state. */
    public String getConnectionState() {
        return connectionState;
    }

    /** Returns node state. */
    public String getNodeState() {
        return nodeState;
    }

    /** Returns node state of the other bd. */
    public String getNodeStateOther() {
        return nodeStateOther;
    }

    /** Returns disk state. */
    public String getDiskState() {
        return diskState;
    }


    /** Returns disk state of the other bd. */
    public String getDiskStateOther() {
        return diskStateOther;
    }

    /** Sets the synced progress in percents. */
    public void setSyncedProgress(final String syncedProgress) {
        this.syncedProgress = syncedProgress;
    }

    /** Returns synced progress in percent. */
    public String getSyncedProgress() {
        return syncedProgress;
    }

    /** Returns whether this block device is attached. */
    public boolean isAttached() {
        if (!drbd) {
            return true;
        }
        if (diskState == null) {
            return false;
        }
        return !"Diskless".equals(diskState);
    }

    /** Returns whether this block device is diskless. */
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

    /** Returns whether this device is waiting for connection. */
    public boolean isWFConnection() {
        if (connectionState == null) {
            return false;
        }
        return "WFConnection".equals(connectionState);
    }

    /** Returns whether this device is primary. */
    public boolean isPrimary() {
        if (nodeState == null) {
            return false;
        }
        return "Primary".equals(nodeState);
    }

    /** Returns whether this device is secondary. */
    public boolean isSecondary() {
        if (nodeState == null) {
            return false;
        }
        return "Secondary".equals(nodeState);
    }

    /** Returns the boolean value of the specified flag. */
    private boolean checkDrbdFlag(final int flag) {
        return drbdFlags.indexOf(flag) >= 0;
    }

    /** Returns whether the sync was paused on this block device. */
    public boolean isPausedSync() {
        if (drbdFlags == null) {
            return false;
        }
        return checkDrbdFlag('u');
    }

    /** Returns whether the node is syncing with other node. */
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

    /** Returns whether the node is verifying with other node. */
    public boolean isVerifying() {
        if (nodeState == null) {
            return false;
        }
        return "VerifyS".equals(connectionState)
                || "VerifyT".equals(connectionState);
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

    /** Sets this block device to be in a split-brain situation. */
    public void setSplitBrain(final boolean splitBrain) {
        this.splitBrain = splitBrain;
    }

    /** Returns whether this block device is in split-brain. */
    public boolean isSplitBrain() {
        return splitBrain;
    }

    /** Returns string with meta disk and index in the parenthesis. */
    public String getMetaDiskString(final String md,
                                    final String mdi) {
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

    /** Returns section by which the parameter is grouped in the views. */
    public String getSection(final String parameter) {
        return Tools.getString("BlockDevice.MetaDiskSection");
    }

    /** Returns whether the block device is swap. */
    public boolean isSwap() {
        return fsType != null && "swap".equals(fsType);
    }

    /** Returns disk uuid. */
    public String getDiskUuid() {
        return diskUuid;
    }

    /** Returns whether the disk device is disk id. */
    public Collection<String> getDiskIds() {
        return diskIds;
    }

    /** Returns lvm group or null. */
    public String getVolumeGroup() {
        return volumeGroup;
    }

    /** Returns whether it is a physical volume. */
    public boolean isPhysicalVolume() {
        return vgOnPhysicalVolume != null;
    }

    /** Returns volume group that is on this physical volume. */
    public String getVolumeGroupOnPhysicalVolume() {
        return vgOnPhysicalVolume;
    }

    /** Returns whether there's a volume group on the physical volume. */
    public boolean isVolumeGroupOnPhysicalVolume() {
        return vgOnPhysicalVolume != null && !"".equals(vgOnPhysicalVolume);
    }

    /** Set volume group that is on this physical volume.
     * "", for no VG, but still it's physical volume. */
    public void setVolumeGroupOnPhysicalVolume(
                                            final String vgOnPhysicalVolume) {
        this.vgOnPhysicalVolume = vgOnPhysicalVolume;
    }

    /** Returns logical volume. */
    public String getLogicalVolume() {
        return logicalVolume;
    }

    /** Returns DRBD block device. */
    public BlockDevice getDrbdBlockDevice() {
        return drbdBlockDevice;
    }

    /** Return whether the drbd block device is a physical volume. */
    public boolean isDrbdPhysicalVolume() {
        return drbdBlockDevice != null && drbdBlockDevice.isPhysicalVolume();
    }

    /** Returns whether there's a volume group on the drbd physical volume. */
    public boolean isDrbdVolumeGroupOnPhysicalVolume() {
        return drbdBlockDevice != null
               && drbdBlockDevice.isVolumeGroupOnPhysicalVolume();
    }

    /** Sets DRBD block device. */
    public void setDrbdBlockDevice(final BlockDevice drbdBlockDevice) {
        this.drbdBlockDevice = drbdBlockDevice;
    }

    /** Return DRBD config disk. */
    public String getDrbdBackingDisk() {
        return drbdBackingDisk;
    }

    /** Set DRBD backing disk. */
    public void setDrbdBackingDisk(final String drbdBackingDisk) {
        this.drbdBackingDisk = drbdBackingDisk;
    }

    /** Set used. */
    public void setUsed(final String usedStr) {
        if (usedStr != null) {
            this.used = Integer.parseInt(usedStr);
        }
    }
}
