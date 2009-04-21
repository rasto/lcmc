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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This class provides drbd commands.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class DRBD {
    /** Device placeholder. */
    private static final String DEVICE_PH     = "@DEVICE@";
    /** Drbd resource placeholder. */
    private static final String RESOURCE_PH   = "@RESOURCE@";
    /** Drbd device placeholder. */
    private static final String DRBDDEV_PH    = "@DRBDDEV@";
    /** Filesystem placeholder. */
    private static final String FILESYSTEM_PH = "@FILESYSTEM@";

    /**
     * Private constructor, cannot be instantiated.
     */
    private DRBD() {
        /* Cannot be instantiated. */
    }

    /**
     * Executes the specified drbd command on the specified host and calls the
     * supplied callback function.
     *
     * @param outputVisible
     *          The flag whether the output should appear in
     *          the terminal panel.
     */
    private static void execCommand(final Host host,
                                    final String command,
                                    final ExecCallback execCallback,
                                    final boolean outputVisible) {
        Tools.execCommandProgressIndicator(
                                     host,
                                     command,
                                     execCallback,
                                     outputVisible,
                                     Tools.getString("DRBD.ExecutingCommand")
                                     + " " + command + "...");
    }

    /**
     * Converts the command replacing the @RESOURCE@ placeholder.
     */
    private static String convert(String command,
                                  final String resource) {
        if (command.indexOf(RESOURCE_PH) > -1) {
            command = command.replaceAll(RESOURCE_PH, resource);
        }
        return command;
    }

    /**
     * Executes the drbdadm attach on the specified host and resource.
     */
    public static void attach(final Host host, final String resource) {
        attach(host, resource, null);
    }

    /**
     * Executes the drbdadm attach on the specified host and resource
     * and calls the callback function.
     */
    public static void attach(final Host host,
                              final String resource,
                              final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.attach"),
                                 resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Executes the drbdadm detach on the specified host and resource.
     */
    public static void detach(final Host host, final String resource) {
        detach(host, resource, null);
    }

    /**
     * Executes the drbdadm detach on the specified host and resource
     * and calls the callback function.
     */
    public static void detach(final Host host,
                              final String resource,
                              final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.detach"),
                                 resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Executes the drbdadm connect on the specified host and resource.
     */
    public static void connect(final Host host, final String resource) {
        connect(host, resource, null);
    }

    /**
     * Executes the drbdadm connect on the specified host and resource
     * and calls the callback function.
     */
    public static void connect(final Host host,
                               final String resource,
                               final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.connect"),
                                 resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Executes the drbdadm disconnect on the specified host and resource.
     */
    public static void disconnect(final Host host, final String resource) {
        disconnect(host, resource, null);
    }

    /**
     * Executes the drbdadm disconnect on the specified host and resource
     * and calls the callback function.
     */
    public static void disconnect(final Host host,
                                  final String resource,
                                  final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.disconnect"),
                                 resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Executes the drbdadm pause-sync on the specified host and resource.
     */
    public static void pauseSync(final Host host, final String resource) {
        pauseSync(host, resource, null);
    }

    /**
     * Executes the drbdadm pause-sync on the specified host and resource
     * and calls the callback function.
     */
    public static void pauseSync(final Host host,
                                 final String resource,
                                 final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.pauseSync"),
                                       resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Executes the drbdadm resume-sync on the specified host and resource.
     */
    public static void resumeSync(final Host host, final String resource) {
        resumeSync(host, resource, null);
    }

    /**
     * Executes the drbdadm resume-sync on the specified host and resource
     * and calls the callback function.
     */
    public static void resumeSync(final Host host,
                                  final String resource,
                                  final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.resumeSync"),
                                       resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Executes the drbdadm primary on the specified host and resource.
     */
    public static void setPrimary(final Host host, final String resource) {
        setPrimary(host, resource, null);
    }

    /**
     * Executes the drbdadm primary on the specified host and resource
     * and calls the callback function.
     */
    public static void setPrimary(final Host host,
                                  final String resource,
                                  final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.setPrimary"),
                                       resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Executes the drbdadm secondary on the specified host and resource.
     */
    public static void setSecondary(final Host host, final String resource) {
        setSecondary(host, resource, null);
    }

    /**
     * Executes the drbdadm secondary on the specified host and resource
     * and calls the callback function.
     */
    public static void setSecondary(final Host host,
                                    final String resource,
                                    final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.setSecondary"),
                                       resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Loads the drbd, executes the drbdadm create-md and up on the specified
     * host and resource.
     */
    public static void initDrbd(final Host host, final String resource) {
        initDrbd(host, resource, null);
    }

    /**
     * Loads the drbd, executes the drbdadm create-md and up on the specified
     * host and resource and calls the callback function.
     */
    public static void initDrbd(final Host host,
                                final String resource,
                                final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.initDrbd"),
                                 resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Creates a drbd meta-data on the specified host, resource and block
     * device.
     */
    public static void createMD(final Host host,
                                final String resource,
                                final String device) {
        createMD(host, resource, device, null);
    }

    /**
     * Creates a drbd meta-data on the specified host, resource and block
     * device and calls the callback function.
     */
    public static void createMD(final Host host,
                                final String resource,
                                final String device,
                                final ExecCallback execCallback) {
        String command = convert(host.getCommand("DRBD.createMD"),
                                 resource);
        if (command.indexOf(DEVICE_PH) > -1) {
            command = command.replaceAll(DEVICE_PH, device);
        }
        execCommand(host, command, execCallback, true);
    }

    /**
     * Creates a drbd meta-data on the specified host, resource and block
     * device. Before that, it DESTROYS the old file system.
     */
    public static void createMDDestroyData(final Host host,
                                           final String resource,
                                           final String device) {
        createMDDestroyData(host, resource, device, null);
    }

    /**
     * Creates a drbd meta-data on the specified host, resource and block
     * device and calls the callback function.
     * Before that, it DESTROYS the old file system.
     */
    public static void createMDDestroyData(final Host host,
                                           final String resource,
                                           final String device,
                                           final ExecCallback execCallback) {
        String command = convert(host.getCommand("DRBD.createMDDestroyData"),
                                 resource);
        if (command.indexOf(DEVICE_PH) > -1) {
            command = command.replaceAll(DEVICE_PH, device);
        }
        execCommand(host, command, execCallback, true);
    }

    /**
     * Makes specified filesystem on the specified host and block device.
     */
    public static void makeFilesystem(final Host host,
                                      final String blockDevice,
                                      final String filesystem) {
        makeFilesystem(host, blockDevice, filesystem, null);
    }

    /**
     * Makes specified filesystem on the specified host and block device and
     * calls the callback function.
     */
    public static void makeFilesystem(final Host host,
                                      final String blockDevice,
                                      final String filesystem,
                                      final ExecCallback execCallback) {
        String command = host.getCommand("DRBD.makeFilesystem");

        if (command.indexOf(DRBDDEV_PH) > -1) {
            command = command.replaceAll(DRBDDEV_PH, blockDevice);
        }
        if (command.indexOf(FILESYSTEM_PH) > -1) {
            command = command.replaceAll(FILESYSTEM_PH, filesystem);
        }

        execCommand(host, command, execCallback, true);
    }

    /**
     * Executes the drbdadm -- --overwrite-data-of-peer connect on the specified
     * host.
     */
    public static void forcePrimary(final Host host, final String resource) {
        forcePrimary(host, resource, null);
    }

    /**
     * Executes the drbdadm -- --overwrite-data-of-peer connect on the specified
     * host and resource and calls the callback function.
     */
    public static void forcePrimary(final Host host,
                                    final String resource,
                                    final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.forcePrimary"),
                                       resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Executes the drbdadm invalidate on the specified host and resource.
     */
    public static void invalidate(final Host host, final String resource) {
        invalidate(host, resource, null);
    }

    /**
     * Executes the drbdadm invalidate on the specified host and resource
     * and calls the callback function.
     */
    public static void invalidate(final Host host,
                                  final String resource,
                                  final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.invalidate"),
                                       resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Executes the drbdadm -- --discard-my-data connect on the specified
     * host and resource.
     */
    public static void discardData(final Host host, final String resource) {
        discardData(host, resource, null);
    }

    /**
     * Executes the drbdadm -- --discard-my-data connect on the specified
     * host and resource and calls the callback function.
     */
    public static void discardData(final Host host,
                                   final String resource,
                                   final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.discardData"),
                                 resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Executes the drbdadm resize on the specified host and resource.
     */
    public static void resize(final Host host, final String resource) {
        resize(host, resource, null);
    }

    /**
     * Executes the drbdadm resize on the specified host and resource and
     * calls the callback function.
     */
    public static void resize(final Host host,
                              final String resource,
                              final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.resize"),
                                 resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Executes the drbdadm adjust on the specified host and resource.
     */
    public static int adjust(final Host host, final String resource) {
        return adjust(host, resource, null);
    }

    /**
     * Executes the drbdadm adjust on the specified host and resource and
     * calls the callback function.
     */
    public static int adjust(final Host host,
                             final String resource,
                             final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.adjust"),
                                 resource);
        final String ret = Tools.execCommandProgressIndicator(
                                     host,
                                     command,
                                     execCallback,
                                     false,
                                     Tools.getString("DRBD.ExecutingCommand")
                                     + " " + command + "...");
        final Pattern p = Pattern.compile(".*Failure: \\((\\d+)\\).*",
                                          Pattern.DOTALL);
        final Matcher m = p.matcher(ret);

        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    /**
     * Executes the drbdadm adjust on the specified host and resource
     * This is done without actually to make an
     * adjust with -d option to catch possible changes.
     */
    public static void adjustDryrun(final Host host, final String resource) {
        adjustDryrun(host, resource, null);
    }

    /**
     * Executes the drbdadm adjust on the specified host and resource and
     * calls the callback function. This is done without actually to make an
     * adjust with -d option to catch possible changes.
     */
    public static void adjustDryrun(final Host host,
                                    final String resource,
                                    final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.adjust.dryrun"),
                                 resource);
        execCommand(host, command, execCallback, true);

    }

    /**
     * Executes the drbdadm down on the specified host and resource.
     */
    public static void down(final Host host, final String resource) {
        down(host, resource, null);
    }

    /**
     * Executes the drbdadm down on the specified host and resource and
     * calls the callback function.
     */
    public static void down(final Host host,
                            final String resource,
                            final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.down"),
                                 resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Executes the drbdadm up on the specified host and resource.
     */
    public static void up(final Host host, final String resource) {
        up(host, resource, null);
    }

    /**
     * Executes the drbdadm up on the specified host and resource and calls the
     * callback function.
     */
    public static void up(final Host host,
                          final String resource,
                          final ExecCallback execCallback) {
        final String command = convert(host.getCommand("DRBD.up"),
                                 resource);
        execCommand(host, command, execCallback, true);
    }

    /**
     * Start the drbd. Probably /etc/init.d/drbd start.
     */
    public static void start(final Host host) {
        start(host, null);
    }

    /**
     * Start the drbd. Probably /etc/init.d/drbd start and calls the callback
     * function after it is done.
     */
    public static void start(final Host host,
                             final ExecCallback execCallback) {
        final String command = host.getCommand("DRBD.start");
        execCommand(host, command, execCallback, true);
    }

    /**
     * Executes load drbd command on the specified host.
     */
    public static void load(final Host host) {
        load(host, null);
    }

    /**
     * Executes load drbd command on the specified host and calls the callback
     * function after it is done.
     */
    public static void load(final Host host,
                            final ExecCallback execCallback) {
        final String command = host.getCommand("DRBD.load");
        execCommand(host, command, execCallback, true);
    }
}
