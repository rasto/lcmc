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
import java.util.Map;
import java.util.HashMap;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

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
    /** Output of the drbd test. */
    private volatile static String drbdtestOutput = null;
    /** DRBD test lock. */
    private final static Mutex mDRBDtestLock = new Mutex();

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
    private static String execCommand(final Host host,
                                      final String command,
                                      final ExecCallback execCallback,
                                      final boolean outputVisible,
                                      final boolean testOnly) {
        try {
            mDRBDtestLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        drbdtestOutput = null;
        mDRBDtestLock.release();
        if (testOnly) {
            if (command.indexOf("@DRYRUN@") < 0) {
                /* it would be very bad */
                Tools.appError("dry run not available");
                return null;
            }
            String cmd = command.replaceAll("@DRYRUN@", "-d");
            if (cmd.indexOf("@DRYRUNCONF@") >= 0) {
                cmd = cmd.replaceAll("@DRYRUNCONF@",
                                     "-c /var/lib/drbd/drbd.conf-drbd-mc-test");
            }
            final String output = Tools.execCommand(host, cmd, null, false);
            try {
                mDRBDtestLock.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            drbdtestOutput = output;
            mDRBDtestLock.release();

            return output;
        } else {
            String cmd;
            if (command.indexOf("@DRYRUN@") >= 0) {
                cmd = command.replaceAll("@DRYRUN@", "");
            } else {
                cmd = command;
            }
            if (cmd.indexOf("@DRYRUNCONF@") >= 0) {
                cmd = cmd.replaceAll("@DRYRUNCONF@", "");
            }
            return Tools.execCommandProgressIndicator(
                                     host,
                                     cmd,
                                     execCallback,
                                     outputVisible,
                                     Tools.getString("DRBD.ExecutingCommand")
                                     + " " + command + "...");
        }
    }

    /**
     * Returns results of previous dry run.
     */
    public static String getDRBDtest() {
        try {
            mDRBDtestLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final String out = drbdtestOutput;
        mDRBDtestLock.release();
        return out;
    }

    /**
     * Executes the drbdadm attach on the specified host and resource.
     */
    public static void attach(final Host host,
                              final String resource,
                              final boolean testOnly) {
        attach(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm attach on the specified host and resource
     * and calls the callback function.
     */
    public static void attach(final Host host,
                              final String resource,
                              final ExecCallback execCallback,
                              final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.attach",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Executes the drbdadm detach on the specified host and resource.
     */
    public static void detach(final Host host,
                              final String resource,
                              final boolean testOnly) {
        detach(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm detach on the specified host and resource
     * and calls the callback function.
     */
    public static void detach(final Host host,
                              final String resource,
                              final ExecCallback execCallback,
                              final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.detach",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Executes the drbdadm connect on the specified host and resource.
     */
    public static void connect(final Host host,
                               final String resource,
                               final boolean testOnly) {
        connect(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm connect on the specified host and resource
     * and calls the callback function.
     */
    public static void connect(final Host host,
                               final String resource,
                               final ExecCallback execCallback,
                               final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.connect",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Executes the drbdadm disconnect on the specified host and resource.
     */
    public static void disconnect(final Host host,
                                  final String resource,
                                  final boolean testOnly) {
        disconnect(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm disconnect on the specified host and resource
     * and calls the callback function.
     */
    public static void disconnect(final Host host,
                                  final String resource,
                                  final ExecCallback execCallback,
                                  final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.disconnect",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Executes the drbdadm pause-sync on the specified host and resource.
     */
    public static void pauseSync(final Host host,
                                 final String resource,
                                 final boolean testOnly) {
        pauseSync(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm pause-sync on the specified host and resource
     * and calls the callback function.
     */
    public static void pauseSync(final Host host,
                                 final String resource,
                                 final ExecCallback execCallback,
                                 final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.pauseSync",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Executes the drbdadm resume-sync on the specified host and resource.
     */
    public static void resumeSync(final Host host,
                                  final String resource,
                                  final boolean testOnly) {
        resumeSync(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm resume-sync on the specified host and resource
     * and calls the callback function.
     */
    public static void resumeSync(final Host host,
                                  final String resource,
                                  final ExecCallback execCallback,
                                  final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.resumeSync",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Executes the drbdadm primary on the specified host and resource.
     */
    public static void setPrimary(final Host host,
                                  final String resource,
                                  final boolean testOnly) {
        setPrimary(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm primary on the specified host and resource
     * and calls the callback function.
     */
    public static void setPrimary(final Host host,
                                  final String resource,
                                  final ExecCallback execCallback,
                                  final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.setPrimary",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Executes the drbdadm secondary on the specified host and resource.
     */
    public static void setSecondary(final Host host,
                                    final String resource,
                                    final boolean testOnly) {
        setSecondary(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm secondary on the specified host and resource
     * and calls the callback function.
     */
    public static void setSecondary(final Host host,
                                    final String resource,
                                    final ExecCallback execCallback,
                                    final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.setSecondary",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Loads the drbd, executes the drbdadm create-md and up on the specified
     * host and resource.
     */
    public static void initDrbd(final Host host,
                                final String resource,
                                final boolean testOnly) {
        initDrbd(host, resource, null, testOnly);
    }

    /**
     * Loads the drbd, executes the drbdadm create-md and up on the specified
     * host and resource and calls the callback function.
     */
    public static void initDrbd(final Host host,
                                final String resource,
                                final ExecCallback execCallback,
                                final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.initDrbd",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Creates a drbd meta-data on the specified host, resource and block
     * device.
     */
    public static void createMD(final Host host,
                                final String resource,
                                final String device,
                                final boolean testOnly) {
        createMD(host, resource, device, null, testOnly);
    }

    /**
     * Creates a drbd meta-data on the specified host, resource and block
     * device and calls the callback function.
     */
    public static void createMD(final Host host,
                                final String resource,
                                final String device,
                                final ExecCallback execCallback,
                                final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        replaceHash.put(DEVICE_PH, device);
        final String command = host.getDistCommand("DRBD.createMD",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Creates a drbd meta-data on the specified host, resource and block
     * device. Before that, it DESTROYS the old file system.
     */
    public static void createMDDestroyData(final Host host,
                                           final String resource,
                                           final String device,
                                           final boolean testOnly) {
        createMDDestroyData(host, resource, device, null, testOnly);
    }

    /**
     * Creates a drbd meta-data on the specified host, resource and block
     * device and calls the callback function.
     * Before that, it DESTROYS the old file system.
     */
    public static void createMDDestroyData(final Host host,
                                           final String resource,
                                           final String device,
                                           final ExecCallback execCallback,
                                           final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        replaceHash.put(DEVICE_PH, device);
        final String command = host.getDistCommand("DRBD.createMDDestroyData",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Makes specified filesystem on the specified host and block device.
     */
    public static void makeFilesystem(final Host host,
                                      final String blockDevice,
                                      final String filesystem,
                                      final boolean testOnly) {
        makeFilesystem(host, blockDevice, filesystem, null, testOnly);
    }

    /**
     * Makes specified filesystem on the specified host and block device and
     * calls the callback function.
     */
    public static void makeFilesystem(final Host host,
                                      final String blockDevice,
                                      final String filesystem,
                                      final ExecCallback execCallback,
                                      final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(DRBDDEV_PH, blockDevice);
        if ("jfs".equals(filesystem)
            || "reiserfs".equals(filesystem)) {
            replaceHash.put(FILESYSTEM_PH, filesystem + " -q");
        } else {
            replaceHash.put(FILESYSTEM_PH, filesystem);
        }
        final String command = host.getDistCommand("DRBD.makeFilesystem",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Executes the drbdadm -- --overwrite-data-of-peer connect on the specified
     * host.
     */
    public static void forcePrimary(final Host host,
                                    final String resource,
                                    final boolean testOnly) {
        forcePrimary(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm -- --overwrite-data-of-peer connect on the specified
     * host and resource and calls the callback function.
     */
    public static void forcePrimary(final Host host,
                                    final String resource,
                                    final ExecCallback execCallback,
                                    final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.forcePrimary",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Executes the drbdadm invalidate on the specified host and resource.
     */
    public static void invalidate(final Host host,
                                  final String resource,
                                  final boolean testOnly) {
        invalidate(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm invalidate on the specified host and resource
     * and calls the callback function.
     */
    public static void invalidate(final Host host,
                                  final String resource,
                                  final ExecCallback execCallback,
                                  final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.invalidate",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Executes the drbdadm -- --discard-my-data connect on the specified
     * host and resource.
     */
    public static void discardData(final Host host,
                                   final String resource,
                                   final boolean testOnly) {
        discardData(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm -- --discard-my-data connect on the specified
     * host and resource and calls the callback function.
     */
    public static void discardData(final Host host,
                                   final String resource,
                                   final ExecCallback execCallback,
                                   final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.discardData",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Executes the drbdadm resize on the specified host and resource.
     */
    public static void resize(final Host host,
                              final String resource,
                              final boolean testOnly) {
        resize(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm resize on the specified host and resource and
     * calls the callback function.
     */
    public static void resize(final Host host,
                              final String resource,
                              final ExecCallback execCallback,
                              final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.resize",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Executes the drbdadm adjust on the specified host and resource.
     */
    public static int adjust(final Host host,
                             final String resource,
                             final boolean testOnly) {
        return adjust(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm adjust on the specified host and resource and
     * calls the callback function.
     */
    public static int adjust(final Host host,
                             final String resource,
                             final ExecCallback execCallback,
                             final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.adjust",
                                                   replaceHash);
        final String ret = execCommand(host,
                                       command,
                                       execCallback,
                                       false,
                                       testOnly);

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
     * TODO: obsolete
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
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.adjust.dryrun",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, false);

    }

    /**
     * Executes the drbdadm down on the specified host and resource.
     */
    public static void down(final Host host,
                            final String resource,
                            final boolean testOnly) {
        down(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm down on the specified host and resource and
     * calls the callback function.
     */
    public static void down(final Host host,
                            final String resource,
                            final ExecCallback execCallback,
                            final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.down",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Executes the drbdadm up on the specified host and resource.
     */
    public static void up(final Host host,
                          final String resource,
                          final boolean testOnly) {
        up(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm up on the specified host and resource and calls the
     * callback function.
     */
    public static void up(final Host host,
                          final String resource,
                          final ExecCallback execCallback,
                          final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.up",
                                                   replaceHash);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Start the drbd. Probably /etc/init.d/drbd start.
     */
    public static void start(final Host host, final boolean testOnly) {
        start(host, null, testOnly);
    }

    /**
     * Start the drbd. Probably /etc/init.d/drbd start and calls the callback
     * function after it is done.
     */
    public static void start(final Host host,
                             final ExecCallback execCallback,
                             final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.start",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, execCallback, true, testOnly);
    }

    /**
     * Executes load drbd command on the specified host.
     */
    public static void load(final Host host, final boolean testOnly) {
        load(host, null, testOnly);
    }

    /**
     * Executes load drbd command on the specified host and calls the callback
     * function after it is done.
     */
    public static void load(final Host host,
                            final ExecCallback execCallback,
                            final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.load",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, execCallback, true, testOnly);
    }
}
