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

import drbd.utilities.Tools;
import java.util.List;
import java.util.ArrayList;

/**
 * This class holds data of one block device.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class BlockDevice extends Resource {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Block size in blocks. */
    private String blockSize;
    /** absolute path from readlink. */
    private String readlink;
    /** Where is this block device mounted if it is at all. */
    private String mountedOn;
    /** Filesytem type. */
    private String fsType;
    /** Whether this device is drbd device. */
    private boolean drbd = false;
    /** Whether this block device is used by crm in Filesystem service.
     */
    private boolean isUsedByCRM;
    /** Drbd net interface of this block device. */
    private NetInterface netInterface;
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
    private String nodeState       = null;
    /** The disk state. */
    private String diskState       = null;
    /** The sync progress in percent as String. */
    private String syncedProgress  = null;
    /** Drbd flags. */
    private String drbdFlags       = null;
    /** How much of the file system is used in percents. */
    private int used            = -1;

    ///**
    // * Creates a new <code>BlockDevice</code> object.
    // */
    //public BlockDevice(final String name,
    //                   final String blockSize,
    //                   final String mountedOn,
    //                   final String fsType) {
    //    super(name);
    //    this.blockSize = blockSize;
    //    this.mountedOn = mountedOn;
    //    this.fsType    = fsType;
    //}

    /**
     * Creates a new <code>BlockDevice</code> object.
     *
     * @param line
     *          line that contains device, blocksize, mount
     *          point and fs type delimited with space
     */
    public BlockDevice(final String line) {
        super();
        update(line);
    }

    /**
     * Updates the block device.
     */
    public final void update(final String line) {
        final String[] cols = line.split(" ");
        if (cols.length < 2) {
            Tools.appError("cannot parse line: " + line);
        } else {
            final String device = cols[0];
            this.readlink = cols[1];
            setName(device);
            this.blockSize = cols[2];
            this.mountedOn = null;
            this.fsType    = null;
            if (cols.length > 4) {
                this.mountedOn = cols[3];
                this.fsType    = cols[4];
            }
            if (cols.length > 5) {
                this.used = Integer.parseInt(cols[5]);
            }
        }
    }

    /**
     * Returns block size.
     */
    public final String getBlockSize() {
        return blockSize;
    }

    /**
     * Returns mount point.
     */
    public final String getMountedOn() {
        return mountedOn;
    }

    /**
     * Returns file system type.
     */
    public final String getFsType() {
        return fsType;
    }

    /**
     * Returns whether this block device is mounted.
     */
    public final boolean isMounted() {
        return mountedOn != null;
    }

    /**
     * Returns true if this device is drbd device.
     */
    public final boolean isDrbd() {
        return drbd;
    }

    /**
     * Returns how much of the file system is used in percents. -1 denotes that
     * there is no usage information.
     */
    public final int getUsed() {
        return used;
    }

    /**
     * Resets all drbd info. E.g. after status failure.
     */
    public final void resetDrbd() {
        splitBrain = false;
        connectionState = null;
        nodeState       = null;
        diskState       = null;
        syncedProgress  = null;
        drbdFlags       = null;
    }

    /**
     * Returns true if this device is used by CRM.
     */
    public final boolean isUsedByCRM() {
        return isUsedByCRM;
    }

    /**
     * Returns true if this device is used as drbd meta-disk.
     */
    public final boolean isDrbdMetaDisk() {
        return !metaDiskOfBlockDevices.isEmpty();
    }

    /**
     * Returns the block devices of which this block device is a meta disk.
     */
    public final List<BlockDevice> getMetaDiskOfBlockDevices() {
        return metaDiskOfBlockDevices;
    }

    /**
     * Returns whether this block device is available for
     * drbd. That is if this device is not mounted and is
     * not used by CRM.
     */
    public final boolean isAvailable() {
        return !isMounted() && !isUsedByCRM && !isDrbdMetaDisk();
    }


    /**
     * Sets or unsets drbd flag.
     */
    public final void setDrbd(final boolean drbd) {
        this.drbd = drbd;
        if (!drbd) {
            connectionState       = null;
            nodeState             = null;
            diskState             = null;
            syncedProgress        = null;
            drbdFlags             = null;
            netInterface          = null;
            if (metaDisk != null) {
                metaDisk.removeMetadiskOfBlockDevice(this);
                metaDisk              = null;
            }
            metaDiskOfBlockDevices = new ArrayList<BlockDevice>();
        }
    }

    /**
     * Sets this device used by CRM flag.
     */
    public final void setUsedByCRM(final boolean isUsedByCRM) {
        this.isUsedByCRM = isUsedByCRM;
    }

    /**
     * Adds metaDiskOfBlockDevice which is a block device of which block device
     * is this meta disk.
     */
    public final void addMetaDiskOfBlockDevice(
                                    final BlockDevice metaDiskOfBlockDevice) {
        if (!metaDiskOfBlockDevices.contains(metaDiskOfBlockDevice)) {
            metaDiskOfBlockDevices.add(metaDiskOfBlockDevice);
        }
    }

    /**
     * Sets net interface.
     */
    public final void setNetInterface(final NetInterface netInterface) {
        this.netInterface = netInterface;
    }

    /**
     * Sets meta disk.
     */
    public final void setMetaDisk(final BlockDevice metaDisk) {
        this.metaDisk = metaDisk;
        if (metaDisk != null) {
            metaDisk.addMetaDiskOfBlockDevice(this);
        }
    }

    /**
     * Removes the meta disk info.
     */
    public final void removeMetadiskOfBlockDevice(
                                    final BlockDevice metaDiskOfBlockDevice) {
        metaDiskOfBlockDevices.remove(metaDiskOfBlockDevice);
    }

    /**
     * Returns meta-disk.
     */
    public final BlockDevice getMetaDisk() {
        return metaDisk;
    }

    /**
     * Returns net interface object.
     */
    public final NetInterface getNetInterface() {
        return netInterface;
    }

    /**
     * Returns stored net interface and port concanated with ':'
     * It can return null if net interface or port are not defined.
     */
    public final String getDrbdNetInterfaceWithPort(final String ni,
                                                    final String nip) {
        if (ni == null || nip == null) {
            return null;
        }
        return ni + ":" + nip;
    }

    /**
     * Returns stored net interface and port concanated with ':'
     * It can return null if net interface or port are not defined.
     */
    public final String getDrbdNetInterfaceWithPort() {
        return getDrbdNetInterfaceWithPort(getValue("DrbdNetInterface"),
                                           getValue("DrbdNetInterfacePort"));
    }

    /**
     * Sets drbd connection state as it is in /proc/drbd.
     */
    public final void setConnectionState(final String connectionState) {
        this.connectionState = connectionState;
    }

    /**
     * Sets all drbd flags at once.
     */
    public final void setDrbdFlags(final String drbdFlags) {
        this.drbdFlags = drbdFlags;
    }

    /**
     * Sets drbd node state as it is in /proc/drbd.
     */
    public final void setNodeState(final String nodeState) {
        this.nodeState = nodeState;
    }

    /**
     * Sets disk node state as it is in /proc/drbd.
     */
    public final void setDiskState(final String diskState) {
        this.diskState = diskState;
    }

    /**
     * Returns connection state.
     */
    public final String getConnectionState() {
        return connectionState;
    }

    /**
     * Returns node state.
     */
    public final String getNodeState() {
        return nodeState;
    }

    /**
     * Returns disk state.
     */
    public final String getDiskState() {
        return diskState;
    }

    /**
     * Sets the synced progress in percents.
     */
    public final void setSyncedProgress(final String syncedProgress) {
        this.syncedProgress = syncedProgress;
    }

    /**
     * Returns synced progress in percent.
     */
    public final String getSyncedProgress() {
        return syncedProgress;
    }

    /**
     * Returns whether this block device is attached.
     */
    public final boolean isAttached() {
        if (!drbd) {
            return true;
        }
        if (diskState == null) {
            return false;
        }
        if ("Diskless".equals(diskState)) {
            return false;
        }
        return true;
    }

    /**
     * Returns whether this block device is diskless.
     */
    public final boolean isDiskless() {
        if (!drbd) {
            return false;
        }
        if (diskState == null) {
            return true;
        }
        if ("Diskless".equals(diskState)) {
            return true;
        }
        return false;
    }

    /**
     * Returns whether this block device is connected and resets the split
     * brain flag if it is.
     */
    public final boolean isConnected() {
        if (connectionState == null) {
            return false;
        }
        if ("Connected".equals(connectionState)
            || "SyncTarget".equals(connectionState)
            || "SyncSource".equals(connectionState)
            || "StartingSyncS".equals(connectionState)
            || "StartingSyncT".equals(connectionState)
            || "WFBitMapS".equals(connectionState)
            || "WFBitMapT".equals(connectionState)
            || "WFSyncUUID".equals(connectionState)
            || "PausedSyncS".equals(connectionState)
            || "PausedSyncT".equals(connectionState)
            || "VerifyS".equals(connectionState)
            || "VerifyT".equals(connectionState)) {
            setSplitBrain(false);
            return true;
        }
        return false;
    }

    /**
     * Returns whether the device is connected or is waiting for connection.
     */
    public final boolean isConnectedOrWF() {
        if (isWFConnection() || isConnected()) {
            return true;
        }
        return false;
    }

    /**
     * Returns whether this device is waiting for connection.
     */
    public final boolean isWFConnection() {
        if (connectionState == null) {
            return false;
        }
        if ("WFConnection".equals(connectionState)) {
            return true;
        }
        return false;
    }

    /**
     * Returns whether this device is primary.
     */
    public final boolean isPrimary() {
        if (nodeState == null) {
            return false;
        }
        if ("Primary".equals(nodeState)) {
            return true;
        }
        return false;
    }

    /**
     * Returns whether this device is secondary.
     */
    public final boolean isSecondary() {
        if (nodeState == null) {
            return false;
        }
        if ("Secondary".equals(nodeState)) {
            return true;
        }
        return false;
    }

    /**
     * Returns the boolean value of the specified flag.
     */
    private boolean checkDrbdFlag(final int flag) {
        return drbdFlags.indexOf(flag) >= 0;
    }

    /**
     * Returns whether the sync was paused on this block device.
     */
    public final boolean isPausedSync() {
        if (drbdFlags == null) {
            return false;
        }
        return checkDrbdFlag('u');
    }

    /**
     * Returns whether the node is syncing with other node.
     */
    public final boolean isSyncing() {
        if (nodeState == null) {
            syncedProgress = null;
            return false;
        }
        if ("SyncTarget".equals(connectionState)
            || "SyncSource".equals(connectionState)
            || "PausedSyncS".equals(connectionState)
            || "PausedSyncT".equals(connectionState)) {
            return true;
        }
        syncedProgress = null;
        return false;
    }

    /**
     * Returns whether the node is verifying with other node.
     */
    public final boolean isVerifying() {
        if (nodeState == null) {
            return false;
        }
        if ("VerifyS".equals(connectionState)
            || "VerifyT".equals(connectionState)) {
            return true;
        }
        return false;
    }


    /**
     * Returns true if this node is source of the data, false if this node gets
     * data from other node.
     */
    public final boolean isSyncSource() {
        if (connectionState == null) {
            return false;
        }
        if ("SyncSource".equals(connectionState)) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if this node is target for the data, otherwise false.
     */
    public final boolean isSyncTarget() {
        if (connectionState == null) {
            return false;
        }
        if ("SyncTarget".equals(connectionState)) {
            return true;
        }
        return false;
    }

    /**
     * Sets this block device to be in a split-brain situation.
     */
    public final void setSplitBrain(final boolean splitBrain) {
        this.splitBrain = splitBrain;
    }

    /**
     * Returns whether this block device is in split-brain.
     */
    public final boolean isSplitBrain() {
        return splitBrain;
    }

    /**
     * Returns string with meta disk and index in the parenthesis.
     */
    public final String getMetaDiskString(final String md,
                                          final String mdi) {
        if (md == null || mdi == null) {
            return null;
        }
        final StringBuffer metaDiskString = new StringBuffer();
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

    /**
     * Returns string with stored meta disk and index in the parenthesis.
     */
    public final String getMetaDiskString() {
        return getMetaDiskString(getValue("DrbdMetaDisk"),
                                 getValue("DrbdMetaDiskIndex"));
    }

    /**
     * Returns section by which the parameter is grouped in
     * the views.
     */
    public final String getSection(final String parameter) {
        if ("DrbdNetInterface".equals(parameter)) {
            return "Drbd";
        } else if ("DrbdNetInterfacePort".equals(parameter)) {
            return "More";
        } else if ("DrbdMetaDisk".equals(parameter)) {
            return "Drbd";
        } else if ("DrbdMetaDiskIndex".equals(parameter)) {
            return "More";
        } else {
            return "";
        }
    }

    /**
     * Returns whether the block device is swap.
     */
    public final boolean isSwap() {
        if (fsType != null && "swap".equals(fsType)) {
            return true;
        }
        return false;
    }

    /**
     * Returns absolute path obtained with readlink.
     */
    public final String getReadlink() {
        return readlink;
    }
}
