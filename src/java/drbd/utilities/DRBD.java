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
import drbd.configs.DistResource;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

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
    private static volatile String drbdtestOutput = null;
    /** DRBD test lock. */
    private static final ReadWriteLock M_DRBD_TEST_LOCK =
                                                  new ReentrantReadWriteLock();
    private static final Lock M_DRBD_TEST_READLOCK =
                                                   M_DRBD_TEST_LOCK.readLock();
    private static final Lock M_DRBD_TEST_WRITELOCK =
                                                  M_DRBD_TEST_LOCK.writeLock();

    /** Private constructor, cannot be instantiated. */
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
    private static SSH.SSHOutput execCommand(final Host host,
                                             final String command,
                                             final ExecCallback execCallback,
                                             final boolean outputVisible,
                                             final boolean testOnly) {
        M_DRBD_TEST_WRITELOCK.lock();
        try {
            drbdtestOutput = null;
        } finally {
            M_DRBD_TEST_WRITELOCK.unlock();
        }
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
            final SSH.SSHOutput output = Tools.execCommand(
                                                host,
                                                cmd,
                                                null,
                                                false,
                                                SSH.DEFAULT_COMMAND_TIMEOUT);
            M_DRBD_TEST_WRITELOCK.lock();
            try {
                drbdtestOutput = output.getOutput();
            } finally {
                M_DRBD_TEST_WRITELOCK.unlock();
            }

            return output;
        } else {
            String cmd;
            if (command.indexOf("@DRYRUN@") >= 0) {
                cmd = command.replaceAll("@DRYRUN@", "");
            } else {
                cmd = command;
            }
            cmd = cmd.replaceAll("@DRYRUNCONF@", "");
            return Tools.execCommandProgressIndicator(
                                     host,
                                     cmd,
                                     execCallback,
                                     outputVisible,
                                     Tools.getString("DRBD.ExecutingCommand")
                                     + " "
                                     + cmd.replaceAll(DistResource.SUDO, " ")
                                     + "...",
                                     SSH.DEFAULT_COMMAND_TIMEOUT);
        }
    }

    /** Returns results of previous dry run. */
    public static String getDRBDtest() {
        M_DRBD_TEST_READLOCK.lock();
        final String out = drbdtestOutput;
        M_DRBD_TEST_READLOCK.unlock();
        return out;
    }

    /** Executes the drbdadm attach on the specified host and resource. */
    public static boolean attach(final Host host,
                                 final String resource,
                                 final boolean testOnly) {
        return attach(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm attach on the specified host and resource
     * and calls the callback function.
     */
    public static boolean attach(final Host host,
                                 final String resource,
                                 final ExecCallback execCallback,
                                 final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.attach",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm detach on the specified host and resource. */
    public static boolean detach(final Host host,
                                 final String resource,
                                 final boolean testOnly) {
        return detach(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm detach on the specified host and resource
     * and calls the callback function.
     */
    public static boolean detach(final Host host,
                                 final String resource,
                                 final ExecCallback execCallback,
                                 final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.detach",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm connect on the specified host and resource. */
    public static boolean connect(final Host host,
                                  final String resource,
                                  final boolean testOnly) {
        return connect(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm connect on the specified host and resource
     * and calls the callback function.
     */
    public static boolean connect(final Host host,
                                  final String resource,
                                  final ExecCallback execCallback,
                                  final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.connect",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm disconnect on the specified host and resource. */
    public static boolean disconnect(final Host host,
                                     final String resource,
                                     final boolean testOnly) {
        return disconnect(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm disconnect on the specified host and resource
     * and calls the callback function.
     */
    public static boolean disconnect(final Host host,
                                     final String resource,
                                     final ExecCallback execCallback,
                                     final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.disconnect",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm pause-sync on the specified host and resource. */
    public static boolean pauseSync(final Host host,
                                    final String resource,
                                    final boolean testOnly) {
        return pauseSync(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm pause-sync on the specified host and resource
     * and calls the callback function.
     */
    public static boolean pauseSync(final Host host,
                                    final String resource,
                                    final ExecCallback execCallback,
                                    final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.pauseSync",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm resume-sync on the specified host and resource. */
    public static boolean resumeSync(final Host host,
                                     final String resource,
                                     final boolean testOnly) {
        return resumeSync(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm resume-sync on the specified host and resource
     * and calls the callback function.
     */
    public static boolean resumeSync(final Host host,
                                     final String resource,
                                     final ExecCallback execCallback,
                                     final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.resumeSync",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm primary on the specified host and resource. */
    public static boolean setPrimary(final Host host,
                                     final String resource,
                                     final boolean testOnly) {
        return setPrimary(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm primary on the specified host and resource
     * and calls the callback function.
     */
    public static boolean setPrimary(final Host host,
                                     final String resource,
                                     final ExecCallback execCallback,
                                     final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.setPrimary",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm secondary on the specified host and resource. */
    public static boolean setSecondary(final Host host,
                                       final String resource,
                                       final boolean testOnly) {
        return setSecondary(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm secondary on the specified host and resource
     * and calls the callback function.
     */
    public static boolean setSecondary(final Host host,
                                       final String resource,
                                       final ExecCallback execCallback,
                                       final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.setSecondary",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Loads the drbd, executes the drbdadm create-md and up on the specified
     * host and resource.
     */
    public static boolean initDrbd(final Host host,
                                   final String resource,
                                   final boolean testOnly) {
        return initDrbd(host, resource, null, testOnly);
    }

    /**
     * Loads the drbd, executes the drbdadm create-md and up on the specified
     * host and resource and calls the callback function.
     */
    public static boolean initDrbd(final Host host,
                                   final String resource,
                                   final ExecCallback execCallback,
                                   final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.initDrbd",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Creates a drbd meta-data on the specified host, resource and block
     * device.
     */
    public static boolean createMD(final Host host,
                                   final String resource,
                                   final String device,
                                   final boolean testOnly) {
        return createMD(host, resource, device, null, testOnly);
    }

    /**
     * Creates a drbd meta-data on the specified host, resource and block
     * device and calls the callback function.
     */
    public static boolean createMD(final Host host,
                                   final String resource,
                                   final String device,
                                   final ExecCallback execCallback,
                                   final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        replaceHash.put(DEVICE_PH, device);
        final String command = host.getDistCommand("DRBD.createMD",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Creates a drbd meta-data on the specified host, resource and block
     * device. Before that, it DESTROYS the old file system.
     */
    public static boolean createMDDestroyData(final Host host,
                                              final String resource,
                                              final String device,
                                              final boolean testOnly) {
        return createMDDestroyData(host, resource, device, null, testOnly);
    }

    /**
     * Creates a drbd meta-data on the specified host, resource and block
     * device and calls the callback function.
     * Before that, it DESTROYS the old file system.
     */
    public static boolean createMDDestroyData(final Host host,
                                              final String resource,
                                              final String device,
                                              final ExecCallback execCallback,
                                              final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        replaceHash.put(DEVICE_PH, device);
        final String command = host.getDistCommand("DRBD.createMDDestroyData",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Makes specified filesystem on the specified host and block device. */
    public static boolean makeFilesystem(final Host host,
                                         final String blockDevice,
                                         final String filesystem,
                                         final boolean testOnly) {
        return makeFilesystem(host, blockDevice, filesystem, null, testOnly);
    }

    /**
     * Makes specified filesystem on the specified host and block device and
     * calls the callback function.
     */
    public static boolean makeFilesystem(final Host host,
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
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Executes the drbdadm -- --overwrite-data-of-peer connect on the specified
     * host.
     */
    public static boolean forcePrimary(final Host host,
                                       final String resource,
                                       final boolean testOnly) {
        return forcePrimary(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm -- --overwrite-data-of-peer connect on the specified
     * host and resource and calls the callback function.
     */
    public static boolean forcePrimary(final Host host,
                                       final String resource,
                                       final ExecCallback execCallback,
                                       final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.forcePrimary",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm invalidate on the specified host and resource. */
    public static boolean invalidate(final Host host,
                                     final String resource,
                                     final boolean testOnly) {
        return invalidate(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm invalidate on the specified host and resource
     * and calls the callback function.
     */
    public static boolean invalidate(final Host host,
                                     final String resource,
                                     final ExecCallback execCallback,
                                     final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.invalidate",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /**
     * Executes the drbdadm -- --discard-my-data connect on the specified
     * host and resource.
     */
    public static boolean discardData(final Host host,
                                      final String resource,
                                      final boolean testOnly) {
        return discardData(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm -- --discard-my-data connect on the specified
     * host and resource and calls the callback function.
     */
    public static boolean discardData(final Host host,
                                      final String resource,
                                      final ExecCallback execCallback,
                                      final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.discardData",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm resize on the specified host and resource. */
    public static boolean resize(final Host host,
                                 final String resource,
                                 final boolean testOnly) {
        return resize(host, resource, null, testOnly);
    }


    /**
     * Executes the drbdadm resize on the specified host and resource and
     * calls the callback function.
     */
    public static boolean resize(final Host host,
                              final String resource,
                              final ExecCallback execCallback,
                              final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.resize",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm verify on the specified host and resource. */
    public static boolean verify(final Host host,
                                 final String resource,
                                 final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.verify",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                        execCommand(host, command, null, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm adjust on the specified host and resource. */
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
        final SSH.SSHOutput ret = execCommand(host,
                                              command,
                                              execCallback,
                                              false,
                                              testOnly);

        final Pattern p = Pattern.compile(".*Failure: \\((\\d+)\\).*",
                                          Pattern.DOTALL);
        final Matcher m = p.matcher(ret.getOutput());
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

    /** Executes the drbdadm down on the specified host and resource. */
    public static boolean down(final Host host,
                               final String resource,
                               final boolean testOnly) {
        return down(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm down on the specified host and resource and
     * calls the callback function.
     */
    public static boolean down(final Host host,
                               final String resource,
                               final ExecCallback execCallback,
                               final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.down",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm up on the specified host and resource. */
    public static boolean up(final Host host,
                             final String resource,
                             final boolean testOnly) {
        return up(host, resource, null, testOnly);
    }

    /**
     * Executes the drbdadm up on the specified host and resource and calls the
     * callback function.
     */
    public static boolean up(final Host host,
                             final String resource,
                             final ExecCallback execCallback,
                             final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(RESOURCE_PH, resource);
        final String command = host.getDistCommand("DRBD.up",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Start the drbd. Probably /etc/init.d/drbd start. */
    public static boolean start(final Host host, final boolean testOnly) {
        return start(host, null, testOnly);
    }

    /**
     * Start the drbd. Probably /etc/init.d/drbd start and calls the callback
     * function after it is done.
     */
    public static boolean start(final Host host,
                                final ExecCallback execCallback,
                                final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.start",
                                                   (ConvertCmdCallback) null);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;

    }

    /** Executes load drbd command on the specified host. */
    public static boolean load(final Host host, final boolean testOnly) {
        return load(host, null, testOnly);
    }

    /**
     * Executes load drbd command on the specified host and calls the callback
     * function after it is done.
     */
    public static boolean load(final Host host,
                               final ExecCallback execCallback,
                               final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.load",
                                                   (ConvertCmdCallback) null);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }
}
