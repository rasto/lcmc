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

package drbd.utilities;

import drbd.data.Host;
import drbd.configs.DistResource;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * This class provides LVM commands.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class LVM {
    /** Size placeholder. */
    private static final String SIZE_PH    = "@SIZE@";
    /** Device placeholder. */
    private static final String DEVICE_PH  = "@DEVICE@";
    /** LV name placeholder. */
    private static final String LV_NAME_PH = "@LVNAME@";
    /** Volume group placeholder. */
    private static final String VG_NAME_PH = "@VGNAME@";
    /** Physical volume names placeholder, delimited with space. */
    private static final String PV_NAMES_PH = "@PVNAMES@";

    /** Private constructor, cannot be instantiated. */
    private LVM() {
        /* Cannot be instantiated. */
    }

    /**
     * Executes the specified LVM command on the specified host and calls the
     * supplied callback function.
     */
    private static SSH.SSHOutput execCommand(final Host host,
                                             final String command,
                                             final boolean outputVisible,
                                             final boolean testOnly) {
        return Tools.execCommandProgressIndicator(
                                 host,
                                 command,
                                 null, /* exec callback */
                                 outputVisible,
                                 Tools.getString("LVM.ExecutingCommand")
                                 + " "
                                 + command.replaceAll(DistResource.SUDO, " ")
                                 + "...",
                                 SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /** Resize LVM device. */
    public static boolean resize(final Host host,
                                 final String blockDevice,
                                 final String size,
                                 final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(SIZE_PH, size);
        replaceHash.put(DEVICE_PH, blockDevice);
        final String command = host.getDistCommand("LVM.resize", replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Initialize a physical volume. */
    public static boolean pvCreate(final Host host,
                                   final String blockDevice,
                                   final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(DEVICE_PH, blockDevice);
        final String command = host.getDistCommand("LVM.pvcreate", replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Remove a physical volume. */
    public static boolean pvRemove(final Host host,
                                   final String blockDevice,
                                   final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(DEVICE_PH, blockDevice);
        final String command = host.getDistCommand("LVM.pvremove", replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Remove LVM device. */
    public static boolean lvRemove(final Host host,
                                   final String blockDevice,
                                   final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(DEVICE_PH, blockDevice);
        final String command = host.getDistCommand("LVM.lvremove", replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Create LVM device. */
    public static boolean lvCreate(final Host host,
                                   final String lvName,
                                   final String vgName,
                                   final String size,
                                   final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(SIZE_PH, size);
        replaceHash.put(LV_NAME_PH, lvName);
        replaceHash.put(VG_NAME_PH, vgName);
        final String command = host.getDistCommand("LVM.lvcreate", replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Create a volume group. */
    public static boolean vgCreate(final Host host,
                                   final String vgName,
                                   final List<String> pvNames,
                                   final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(PV_NAMES_PH, Tools.join(" ", pvNames));
        replaceHash.put(VG_NAME_PH, vgName);
        final String command = host.getDistCommand("LVM.vgcreate", replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Create an LVM snapshot. */
    public static boolean lvSnapshot(final Host host,
                                     final String snapshotName,
                                     final String device,
                                     final String size,
                                     final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(SIZE_PH, size);
        replaceHash.put(DEVICE_PH, device);
        replaceHash.put(LV_NAME_PH, snapshotName);
        final String command = host.getDistCommand("LVM.lvsnapshot",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, true, testOnly);
        return ret.getExitCode() == 0;
    }
}
