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

package lcmc.lvm.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import lcmc.configs.DistResource;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.common.domain.util.Tools;
import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.SshOutput;

/**
 * This class provides LVM commands.
 */
public final class LVM {
    private static final String SIZE_PLACE_HOLDER = "@SIZE@";
    private static final String DEVICE_PLACE_HOLDER = "@DEVICE@";
    private static final String LV_NAME_PLACE_HOLDER = "@LVNAME@";
    private static final String VG_NAME_PLACE_HOLDER = "@VGNAME@";
    /** Physical volume names placeholder, delimited with space. */
    private static final String PV_NAMES_PLACE_HOLDER = "@PVNAMES@";

    /**
     * Executes the specified LVM command on the specified host and calls the
     * supplied callback function.
     */
    private static SshOutput execCommand(final Host host, final String command) {
        return host.captureCommandProgressIndicator(Tools.getString("LVM.ExecutingCommand")
                                                    + ' '
                                                    + command.replaceAll(DistResource.SUDO, " ")
                                                    + "...",
                                                    new ExecCommandConfig().command(command));
    }

    /** Resize LVM device. */
    public static boolean resize(final Host host,
                                 final String blockDevice,
                                 final String size,
                                 final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(SIZE_PLACE_HOLDER, size);
        replaceHash.put(DEVICE_PLACE_HOLDER, blockDevice);
        final String command = host.getHostParser().getDistCommand("LVM.resize", replaceHash);
        final SshOutput ret = execCommand(host, command);
        return ret.getExitCode() == 0;
    }

    /** Initialize a physical volume. */
    public static boolean pvCreate(final Host host,
                                   final String blockDevice,
                                   final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(DEVICE_PLACE_HOLDER, blockDevice);
        final String command = host.getHostParser().getDistCommand("LVM.pvcreate", replaceHash);
        final SshOutput ret = execCommand(host, command);
        return ret.getExitCode() == 0;
    }

    /** Remove a physical volume. */
    public static boolean pvRemove(final Host host,
                                   final String blockDevice,
                                   final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(DEVICE_PLACE_HOLDER, blockDevice);
        final String command = host.getHostParser().getDistCommand("LVM.pvremove", replaceHash);
        final SshOutput ret = execCommand(host, command);
        return ret.getExitCode() == 0;
    }

    /** Remove a volume group. */
    public static boolean vgRemove(final Host host,
                                   final String vgName,
                                   final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(VG_NAME_PLACE_HOLDER, vgName);
        final String command = host.getHostParser().getDistCommand("LVM.vgremove", replaceHash);
        final SshOutput ret = execCommand(host, command);
        return ret.getExitCode() == 0;
    }

    /** Remove LVM device. */
    public static boolean lvRemove(final Host host,
                                   final String blockDevice,
                                   final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(DEVICE_PLACE_HOLDER, blockDevice);
        final String command = host.getHostParser().getDistCommand("LVM.lvremove", replaceHash);
        final SshOutput ret = execCommand(host, command);
        return ret.getExitCode() == 0;
    }

    /** Create LVM device. */
    public static boolean lvCreate(final Host host,
                                   final String lvName,
                                   final String vgName,
                                   final String size,
                                   final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(SIZE_PLACE_HOLDER, size);
        replaceHash.put(LV_NAME_PLACE_HOLDER, lvName);
        replaceHash.put(VG_NAME_PLACE_HOLDER, vgName);
        final String command = host.getHostParser().getDistCommand("LVM.lvcreate", replaceHash);
        final SshOutput ret = execCommand(host, command);
        return ret.getExitCode() == 0;
    }

    public static boolean vgCreate(final Host host,
                                   final String vgName,
                                   final Collection<String> pvNames,
                                   final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(PV_NAMES_PLACE_HOLDER, Tools.join(" ", pvNames));
        replaceHash.put(VG_NAME_PLACE_HOLDER, vgName);
        final String command = host.getHostParser().getDistCommand("LVM.vgcreate", replaceHash);
        final SshOutput ret = execCommand(host, command);
        return ret.getExitCode() == 0;
    }

    public static boolean createLVSnapshot(final Host host,
                                           final String snapshotName,
                                           final String device,
                                           final String size,
                                           final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(SIZE_PLACE_HOLDER, size);
        replaceHash.put(DEVICE_PLACE_HOLDER, device);
        replaceHash.put(LV_NAME_PLACE_HOLDER, snapshotName);
        final String command = host.getHostParser().getDistCommand("LVM.lvsnapshot", replaceHash);
        final SshOutput ret = execCommand(host, command);
        return ret.getExitCode() == 0;
    }

    private LVM() {
        /* Cannot be instantiated. */
    }
}
