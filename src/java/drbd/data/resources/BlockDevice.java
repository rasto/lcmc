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
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This class holds data of one block device.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class BlockDevice extends Resource {
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
    /** Whether this block device is used by crm in Filesystem service. */
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
    private String nodeState = null;
    /** The disk state. */
    private String diskState = null;
    /** The sync progress in percent as String. */
    private String syncedProgress = null;
    /** Drbd flags. */
    private String drbdFlags = null;
    /** How much of the file system is used in percents. */
    private int used = -1;
    /** LVM group. */
    private String volumeGroup = null;
    /** Logical volume. */
    private String logicalVolume = null;

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

    /** Updates the block device. */
    public void update(final String line) {
        final Pattern p = Pattern.compile("([^:]+):(.*)");
        final String[] cols = line.split(" ");
        if (cols.length < 2) {
            Tools.appWarning("cannot parse line: " + line);
        } else {
            final String device = cols[0];
            setName(device);
            final Map<String, String> tokens = new HashMap<String, String>();
            for (int i = 1; i < cols.length; i++) {
                final Matcher m = p.matcher(cols[i]);
                if (m.matches()) {
                    tokens.put(m.group(1), m.group(2));
                } else {
                    Tools.appWarning("could not parse: " + line);
                }
            }
            this.readlink  = tokens.get("rl");
            this.blockSize = tokens.get("size");
            this.mountedOn = tokens.get("mp");
            this.fsType    = tokens.get("fs");
            this.volumeGroup = tokens.get("vg");
            this.logicalVolume = tokens.get("lv");
            final String usedStr = tokens.get("used");
            if (usedStr != null) {
                this.used      = Integer.parseInt(usedStr);
            }
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
        return !isMounted() && !isUsedByCRM && !isDrbdMetaDisk();
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
            netInterface          = null;
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

    /** Sets net interface. */
    void setNetInterface(final NetInterface netInterface) {
        this.netInterface = netInterface;
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

    /** Returns net interface object. */
    NetInterface getNetInterface() {
        return netInterface;
    }

    /**
     * Returns stored net interface and port concanated with ':'
     * It can return null if net interface or port are not defined.
     */
    public String getDrbdNetInterfaceWithPort(final String ni,
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
    public String getDrbdNetInterfaceWithPort() {
        return getDrbdNetInterfaceWithPort(getValue("DrbdNetInterface"),
                                           getValue("DrbdNetInterfacePort"));
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

    /** Sets disk node state as it is in /proc/drbd. */
    public void setDiskState(final String diskState) {
        this.diskState = diskState;
    }

    /** Returns connection state. */
    public String getConnectionState() {
        return connectionState;
    }

    /** Returns node state. */
    public String getNodeState() {
        return nodeState;
    }

    /** Returns disk state. */
    public String getDiskState() {
        return diskState;
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
        if ("Diskless".equals(diskState)) {
            return false;
        }
        return true;
    }

    /** Returns whether this block device is diskless. */
    public boolean isDiskless() {
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
    public boolean isConnected() {
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

    /** Returns whether the device is connected or is waiting for connection. */
    public boolean isConnectedOrWF() {
        if (isWFConnection() || isConnected()) {
            return true;
        }
        return false;
    }

    /** Returns whether this device is waiting for connection. */
    public boolean isWFConnection() {
        if (connectionState == null) {
            return false;
        }
        if ("WFConnection".equals(connectionState)) {
            return true;
        }
        return false;
    }

    /** Returns whether this device is primary. */
    public boolean isPrimary() {
        if (nodeState == null) {
            return false;
        }
        if ("Primary".equals(nodeState)) {
            return true;
        }
        return false;
    }

    /** Returns whether this device is secondary. */
    public boolean isSecondary() {
        if (nodeState == null) {
            return false;
        }
        if ("Secondary".equals(nodeState)) {
            return true;
        }
        return false;
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
        if ("SyncTarget".equals(connectionState)
            || "SyncSource".equals(connectionState)
            || "PausedSyncS".equals(connectionState)
            || "PausedSyncT".equals(connectionState)) {
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
    public boolean isSyncSource() {
        if (connectionState == null) {
            return false;
        }
        if ("SyncSource".equals(connectionState)) {
            return true;
        }
        return false;
    }

    /** Returns true if this node is target for the data, otherwise false. */
     public boolean isSyncTarget() {
        if (connectionState == null) {
            return false;
        }
        if ("SyncTarget".equals(connectionState)) {
            return true;
        }
        return false;
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
        return getMetaDiskString(getValue("DrbdMetaDisk"),
                                 getValue("DrbdMetaDiskIndex"));
    }

    /** Returns section by which the parameter is grouped in the views. */
    public String getSection(final String parameter) {
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

    /** Returns whether the block device is swap. */
    public boolean isSwap() {
        if (fsType != null && "swap".equals(fsType)) {
            return true;
        }
        return false;
    }

    /** Returns absolute path obtained with readlink. */
    public String getReadlink() {
        return readlink;
    }

    /** Returns lvm group or null. */
    public String getVolumeGroup() {
        return volumeGroup;
    }

    /** Returns logical volume. */
    public String getLogicalVolume() {
        return logicalVolume;
    }
}