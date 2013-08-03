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

package lcmc.utilities;

import lcmc.data.Host;
import lcmc.configs.DistResource;
import lcmc.Exceptions;
import lcmc.Exceptions.IllegalVersionException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import lcmc.utilities.SSH.SSHOutput;

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
    private static final String RESOURCE_VOLUME_PH   = "@RES-VOL@";
    /** Drbd device placeholder. */
    private static final String DRBDDEV_PH    = "@DRBDDEV@";
    /** Filesystem placeholder. */
    private static final String FILESYSTEM_PH = "@FILESYSTEM@";
    /** Output of the drbd test. */
    private static volatile String drbdtestOutput = null;
    /** DRBD test lock. */
    private static final ReadWriteLock M_DRBD_TEST_LOCK =
                                                  new ReentrantReadWriteLock();
    /** DRBD test read lock. */
    private static final Lock M_DRBD_TEST_READLOCK =
                                                   M_DRBD_TEST_LOCK.readLock();
    /** DRBD test write lock. */
    private static final Lock M_DRBD_TEST_WRITELOCK =
                                                  M_DRBD_TEST_LOCK.writeLock();
    /** "All resources" string for drbdadm commands. */
    public static final String ALL = "all";
    /** Test only boolean variable. */
    public static final boolean TESTONLY = true;
    /** Live boolean variable. */
    public static final boolean LIVE = false;
    /** To compare versions like 8.3 and 8.4 */
    private static final Pattern DRBD_VERSION_MAJ =
                                      Pattern.compile("^(\\d+\\.\\d+)\\..*");

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
        if (testOnly) {
            if (command.indexOf("@DRYRUN@") < 0) {
                /* it would be very bad */
                Tools.appError("dry run not available");
                return null;
            }
            String cmd = command.replaceAll("@DRYRUN@", "-d");
            if (cmd.indexOf("@DRYRUNCONF@") >= 0) {
                cmd = cmd.replaceAll("@DRYRUNCONF@",
                                     "-c /var/lib/drbd/drbd.conf-lcmc-test");
            }
            final SSHOutput output = Tools.execCommand(
                                                host,
                                                cmd,
                                                null,
                                                false,
                                                SSH.DEFAULT_COMMAND_TIMEOUT);
            M_DRBD_TEST_WRITELOCK.lock();
            try {
                if (drbdtestOutput == null) {
                    drbdtestOutput = output.getOutput();
                } else {
                    drbdtestOutput += output.getOutput();
                }
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
        try {
            final String out = drbdtestOutput;
            drbdtestOutput = null;
            return out;
        } finally {
            M_DRBD_TEST_READLOCK.unlock();
        }

    }

    /** Executes the drbdadm attach on the specified host and resource. */
    public static boolean attach(final Host host,
                                 final String resource,
                                 final String volume,
                                 final boolean testOnly) {
        return attach(host, resource, volume, null, testOnly);
    }

    /** Returns hash that replaces placeholder in commands. */
    private static Map<String, String> getResVolReplaceHash(
                                                      final Host host,
                                                      final String resource,
                                                      final String volume) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        if (volume != null && host.hasVolumes()) {
            replaceHash.put(RESOURCE_VOLUME_PH, resource + "/" + volume);
        } else {
            replaceHash.put(RESOURCE_VOLUME_PH, resource);
        }
        return replaceHash;
    }

    /**
     * Executes the drbdadm attach on the specified host and resource
     * and calls the callback function.
     */
    public static boolean attach(final Host host,
                                 final String resource,
                                 final String volume,
                                 final ExecCallback execCallback,
                                 final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.attach",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm detach on the specified host and resource. */
    public static boolean detach(final Host host,
                                 final String resource,
                                 final String volume,
                                 final boolean testOnly) {
        return detach(host, resource, volume, null, testOnly);
    }

    /**
     * Executes the drbdadm detach on the specified host and resource
     * and calls the callback function.
     */
    public static boolean detach(final Host host,
                                 final String resource,
                                 final String volume,
                                 final ExecCallback execCallback,
                                 final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.detach",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm connect on the specified host and resource. */
    public static boolean connect(final Host host,
                                  final String resource,
                                  final String volume,
                                  final boolean testOnly) {
        return connect(host, resource, volume, null, testOnly);
    }

    /**
     * Executes the drbdadm connect on the specified host and resource
     * and calls the callback function.
     */
    public static boolean connect(final Host host,
                                  final String resource,
                                  final String volume,
                                  final ExecCallback execCallback,
                                  final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.connect",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm disconnect on the specified host and resource. */
    public static boolean disconnect(final Host host,
                                     final String resource,
                                     final String volume,
                                     final boolean testOnly) {
        return disconnect(host, resource, volume, null, testOnly);
    }

    /**
     * Executes the drbdadm disconnect on the specified host and resource
     * and calls the callback function.
     */
    public static boolean disconnect(final Host host,
                                     final String resource,
                                     final String volume,
                                     final ExecCallback execCallback,
                                     final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.disconnect",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm pause-sync on the specified host and resource. */
    public static boolean pauseSync(final Host host,
                                    final String resource,
                                    final String volume,
                                    final boolean testOnly) {
        return pauseSync(host, resource, volume, null, testOnly);
    }

    /**
     * Executes the drbdadm pause-sync on the specified host and resource
     * and calls the callback function.
     */
    public static boolean pauseSync(final Host host,
                                    final String resource,
                                    final String volume,
                                    final ExecCallback execCallback,
                                    final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.pauseSync",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm resume-sync on the specified host and resource. */
    public static boolean resumeSync(final Host host,
                                     final String resource,
                                     final String volume,
                                     final boolean testOnly) {
        return resumeSync(host, resource, volume, null, testOnly);
    }

    /**
     * Executes the drbdadm resume-sync on the specified host and resource
     * and calls the callback function.
     */
    public static boolean resumeSync(final Host host,
                                     final String resource,
                                     final String volume,
                                     final ExecCallback execCallback,
                                     final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.resumeSync",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm primary on the specified host and resource. */
    public static boolean setPrimary(final Host host,
                                     final String resource,
                                     final String volume,
                                     final boolean testOnly) {
        return setPrimary(host, resource, volume, null, testOnly);
    }

    /**
     * Executes the drbdadm primary on the specified host and resource
     * and calls the callback function.
     */
    public static boolean setPrimary(final Host host,
                                     final String resource,
                                     final String volume,
                                     final ExecCallback execCallback,
                                     final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.setPrimary",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm secondary on the specified host and resource. */
    public static boolean setSecondary(final Host host,
                                       final String resource,
                                       final String volume,
                                       final boolean testOnly) {
        return setSecondary(host, resource, volume, null, testOnly);
    }

    /**
     * Executes the drbdadm secondary on the specified host and resource
     * and calls the callback function.
     */
    public static boolean setSecondary(final Host host,
                                       final String resource,
                                       final String volume,
                                       final ExecCallback execCallback,
                                       final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.setSecondary",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
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
                                   final String volume,
                                   final boolean testOnly) {
        return initDrbd(host, resource, volume, null, testOnly);
    }

    /**
     * Loads the drbd, executes the drbdadm create-md and up on the specified
     * host and resource and calls the callback function.
     */
    public static boolean initDrbd(final Host host,
                                   final String resource,
                                   final String volume,
                                   final ExecCallback execCallback,
                                   final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.initDrbd",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
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
                                   final String volume,
                                   final String device,
                                   final boolean testOnly) {
        return createMD(host, resource, volume, device, null, testOnly);
    }

    /**
     * Creates a drbd meta-data on the specified host, resource and block
     * device and calls the callback function.
     */
    public static boolean createMD(final Host host,
                                   final String resource,
                                   final String volume,
                                   final String device,
                                   final ExecCallback execCallback,
                                   final boolean testOnly) {
        final Map<String, String> replaceHash = getResVolReplaceHash(host,
                                                                     resource,
                                                                     volume);
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
                                              final String volume,
                                              final String device,
                                              final boolean testOnly) {
        return createMDDestroyData(host,
                                   resource,
                                   volume,
                                   device,
                                   null,
                                   testOnly);
    }

    /**
     * Creates a drbd meta-data on the specified host, resource and block
     * device and calls the callback function.
     * Before that, it DESTROYS the old file system.
     */
    public static boolean createMDDestroyData(final Host host,
                                              final String resource,
                                              final String volume,
                                              final String device,
                                              final ExecCallback execCallback,
                                              final boolean testOnly) {
        final Map<String, String> replaceHash = getResVolReplaceHash(host,
                                                                     resource,
                                                                     volume);
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

    /** Executes the drbdadm -- --clear-bitmap new-current-uuid. */
    public static boolean skipInitialFullSync(final Host host,
                                              final String resource,
                                              final String volume,
                                              final boolean testOnly) {
        return skipInitialFullSync(host, resource, volume, null, testOnly);
    }

    /** Executes the drbdadm -- --clear-bitmap new-current-uuid. */
    public static boolean skipInitialFullSync(final Host host,
                                              final String resource,
                                              final String volume,
                                              final ExecCallback execCallback,
                                              final boolean testOnly) {
        try {
            String command;
            final String drbdV = host.getDrbdVersion();
            if (Tools.compareVersions(host.getDrbdVersion(), "8.3") <= 0) {
                command = host.getDistCommand("DRBD.skipInitSync.8.3",
                                              getResVolReplaceHash(host,
                                                                   resource,
                                                                   volume));
            } else {
                command = host.getDistCommand("DRBD.skipInitSync",
                                              getResVolReplaceHash(host,
                                                                   resource,
                                                                   volume));
            }
            final SSH.SSHOutput ret =
                      execCommand(host, command, execCallback, true, testOnly);
            return ret.getExitCode() == 0;
        } catch (Exceptions.IllegalVersionException e) {
            Tools.appWarning(e.getMessage(), e);
            return false;
        }
    }


    /**
     * Executes the drbdadm -- --overwrite-data-of-peer connect on the specified
     * host.
     */
    public static boolean forcePrimary(final Host host,
                                       final String resource,
                                       final String volume,
                                       final boolean testOnly) {
        return forcePrimary(host, resource, volume, null, testOnly);
    }

    /**
     * Executes the drbdadm -- --overwrite-data-of-peer connect on the specified
     * host and resource and calls the callback function.
     */
    public static boolean forcePrimary(final Host host,
                                       final String resource,
                                       final String volume,
                                       final ExecCallback execCallback,
                                       final boolean testOnly) {
        try {
            String command;
            final String drbdV = host.getDrbdVersion();
            if (Tools.compareVersions(host.getDrbdVersion(), "8.3.7") <= 0) {
                command = host.getDistCommand("DRBD.forcePrimary.8.3.7",
                                              getResVolReplaceHash(host,
                                                                   resource,
                                                                   volume));
            } else if (Tools.compareVersions(host.getDrbdVersion(),
                                            "8.3") <= 0) {
                command = host.getDistCommand("DRBD.forcePrimary.8.3",
                                              getResVolReplaceHash(host,
                                                                   resource,
                                                                   volume));
            } else {
                command = host.getDistCommand("DRBD.forcePrimary",
                                              getResVolReplaceHash(host,
                                                                   resource,
                                                                   volume));
            }
            final SSH.SSHOutput ret =
                      execCommand(host, command, execCallback, true, testOnly);
            return ret.getExitCode() == 0;
        } catch (Exceptions.IllegalVersionException e) {
            Tools.appWarning(e.getMessage(), e);
            return false;
        }
    }

    /** Executes the drbdadm invalidate on the specified host and resource. */
    public static boolean invalidate(final Host host,
                                     final String resource,
                                     final String volume,
                                     final boolean testOnly) {
        return invalidate(host, resource, volume, null, testOnly);
    }

    /**
     * Executes the drbdadm invalidate on the specified host and resource
     * and calls the callback function.
     */
    public static boolean invalidate(final Host host,
                                     final String resource,
                                     final String volume,
                                     final ExecCallback execCallback,
                                     final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.invalidate",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
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
                                      final String volume,
                                      final boolean testOnly) {
        return discardData(host, resource, volume, null, testOnly);
    }

    /**
     * Executes the drbdadm -- --discard-my-data connect on the specified
     * host and resource and calls the callback function.
     */
    public static boolean discardData(final Host host,
                                      final String resource,
                                      final String volume,
                                      final ExecCallback execCallback,
                                      final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.discardData",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm resize on the specified host and resource. */
    public static boolean resize(final Host host,
                                 final String resource,
                                 final String volume,
                                 final boolean testOnly) {
        return resize(host, resource, volume, null, testOnly);
    }


    /**
     * Executes the drbdadm resize on the specified host and resource and
     * calls the callback function.
     */
    public static boolean resize(final Host host,
                                 final String resource,
                                 final String volume,
                                 final ExecCallback execCallback,
                                 final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.resize",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm verify on the specified host and resource. */
    public static boolean verify(final Host host,
                                 final String resource,
                                 final String volume,
                                 final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.verify",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
        final SSH.SSHOutput ret =
                        execCommand(host, command, null, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm adjust on the specified host and resource. */
    public static int adjust(final Host host,
                             final String resource,
                             final String volume,
                             final boolean testOnly) {
        return adjustApply(host, resource, null, null, testOnly);
    }

    /**
     * Executes the drbdadm adjust on the specified host and resource and
     * calls the callback function.
     */
    public static int adjust(final Host host,
                             final String resource,
                             final String volume,
                             final ExecCallback execCallback,
                             final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.adjust",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
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

    /** Executes the drbdadm adjust on the specified host and resource. */
    public static int adjustApply(final Host host,
                             final String resource,
                             final String volume,
                             final boolean testOnly) {
        return adjustApply(host, resource, null, null, testOnly);
    }

    /**
     * Executes the drbdadm adjust on the specified host and resource and
     * calls the callback function.
     */
    public static int adjustApply(final Host host,
                             final String resource,
                             final String volume,
                             final ExecCallback execCallback,
                             final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.adjust.apply",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
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

    /** Executes the drbdadm proxy-up on the specified host and resource. */
    public static int proxyUp(final Host host,
                              final String resource,
                              final String volume,
                              final boolean testOnly) {
        return proxyUp(host, resource, null, null, testOnly);
    }

    /**
     * Executes the drbdadm proxy-up on the specified host and resource and
     * calls the callback function.
     */
    public static int proxyUp(final Host host,
                              final String resource,
                              final String volume,
                              final ExecCallback execCallback,
                              final boolean testOnly) {
        final String command = getDistCommand("DRBD.proxyUp",
                                              host,
                                              resource,
                                              volume);
        final SSH.SSHOutput ret = execCommand(host,
                                              command,
                                              execCallback,
                                              false,
                                              testOnly);

        return ret.getExitCode();
    }

    /** Executes the drbdadm proxy-down on the specified host and resource. */
    public static int proxyDown(final Host host,
                                final String resource,
                                final String volume,
                                final boolean testOnly) {
        return proxyDown(host, resource, null, null, testOnly);
    }

    /**
     * Executes the drbdadm proxy-up on the specified host and resource and
     * calls the callback function.
     */
    public static int proxyDown(final Host host,
                                final String resource,
                                final String volume,
                                final ExecCallback execCallback,
                                final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.proxyDown",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
        final SSH.SSHOutput ret = execCommand(host,
                                              command,
                                              execCallback,
                                              false,
                                              testOnly);

        return ret.getExitCode();
    }

    /**
     * Executes the drbdadm get-gi on the specified host and resource
     * and return the result or null if there are meta-data on the block
     * device.
     */
    public static String getGI(final Host host,
                               final String resource,
                               final String volume,
                               final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.get-gi",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
        final SSH.SSHOutput ret = execCommand(host,
                                              command,
                                              null,
                                              false,
                                              testOnly);
        if (ret.getExitCode() == 0) {
            return ret.getOutput();
        }
        return null;
    }

    /** Executes the drbdadm down on the specified host and resource. */
    public static boolean down(final Host host,
                               final String resource,
                               final String volume,
                               final boolean testOnly) {
        return down(host, resource, volume, null, testOnly);
    }

    /**
     * Executes the drbdadm down on the specified host and resource and
     * calls the callback function.
     */
    public static boolean down(final Host host,
                               final String resource,
                               final String volume,
                               final ExecCallback execCallback,
                               final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.down",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm up on the specified host and resource. */
    public static boolean up(final Host host,
                             final String resource,
                             final String volume,
                             final boolean testOnly) {
        return up(host, resource, volume, null, testOnly);
    }

    /**
     * Executes the drbdadm up on the specified host and resource and calls the
     * callback function.
     */
    public static boolean up(final Host host,
                             final String resource,
                             final String volume,
                             final ExecCallback execCallback,
                             final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.up",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      volume));
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

    /** Start DRBD proxy on the specified host. */
    public static boolean startProxy(final Host host, final boolean testOnly) {
        return startProxy(host, null, testOnly);
    }

    /**
     * Start DRBD proxy on the specified host and call the callback function
     * after it is done.
     */
    public static boolean startProxy(final Host host,
                                     final ExecCallback execCallback,
                                     final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.startProxy",
                                                   (ConvertCmdCallback) null);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        host.updateDrbdParameters(); /* deamon could be started even if ret != 0
                                      */
        return ret.getExitCode() == 0;
    }

    /** Stop DRBD proxy on the specified host. */
    public static boolean stopProxy(final Host host, final boolean testOnly) {
        return stopProxy(host, null, testOnly);
    }

    /**
     * Stop DRBD proxy on the specified host and call the callback function
     * after it is done.
     */
    public static boolean stopProxy(final Host host,
                                    final ExecCallback execCallback,
                                    final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.stopProxy",
                                                   (ConvertCmdCallback) null);
        final SSH.SSHOutput ret =
                    execCommand(host, command, execCallback, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Delete minor, from DRBD 8.4. */
    public static boolean delMinor(final Host host,
                                   final String blockDevice,
                                   final boolean testOnly) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(DRBDDEV_PH, blockDevice);
        final String command = host.getDistCommand("DRBD.delMinor",
                                                   replaceHash);
        final SSH.SSHOutput ret =
                    execCommand(host, command, null, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Delete connection, from DRBD 8.4. */
    public static boolean delConnection(final Host host,
                                        final String resource,
                                        final boolean testOnly) {
        final String command = host.getDistCommand("DRBD.resDelConnection",
                                                   getResVolReplaceHash(
                                                                      host,
                                                                      resource,
                                                                      null));
        final SSH.SSHOutput ret =
                    execCommand(host, command, null, true, testOnly);
        return ret.getExitCode() == 0;
    }

    /** Return command with replaced placeholders. */
    public static String getDistCommand(final String cmd,
                                        final Host host,
                                        final String resource,
                                        final String volume) {
        return host.getDistCommand(cmd,
                                   getResVolReplaceHash(host,
                                                        resource,
                                                        volume));
    }

    /** Return whether DRBD util and module versions are compatible. */
    public static boolean compatibleVersions(final String utilV,
                                             final String moduleV) {
        if (utilV == null || moduleV == null) {
            return false;
        }
        String uV;
        final Matcher matcherU = DRBD_VERSION_MAJ.matcher(utilV);
        if (matcherU.matches()) {
            uV = matcherU.group(1);
        } else {
            return false;
        }

        String mV;
        final Matcher matcherM = DRBD_VERSION_MAJ.matcher(moduleV);
        if (matcherM.matches()) {
            mV = matcherM.group(1);
        } else {
            return false;
        }
        try {
            return Tools.compareVersions(mV, uV) == 0;
        } catch (IllegalVersionException e) {
            return false;
        }
    }
}
