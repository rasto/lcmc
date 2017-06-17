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

package lcmc.drbd.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lcmc.Exceptions.IllegalVersionException;
import lcmc.configs.DistResource;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.common.domain.ConvertCmdCallback;
import lcmc.common.domain.ExecCallback;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;
import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.SshOutput;

/**
 * y
 * This class provides drbd commands.
 */
public final class DRBD {
    private static final Logger LOG = LoggerFactory.getLogger(DRBD.class);
    private static final String DEVICE_PLACE_HOLDER = "@DEVICE@";
    private static final String RESOURCE_VOLUME_PLACE_HOLDER = "@RES-VOL@";
    private static final String DRBDDEV_PLACE_HOLDER = "@DRBDDEV@";
    private static final String FILESYSTEM_PLACE_HOLDER = "@FILESYSTEM@";
    private static volatile String drbdtestOutput = null;
    private static final ReadWriteLock M_DRBD_TEST_LOCK = new ReentrantReadWriteLock();
    private static final Lock M_DRBD_TEST_READLOCK = M_DRBD_TEST_LOCK.readLock();
    private static final Lock M_DRBD_TEST_WRITELOCK = M_DRBD_TEST_LOCK.writeLock();
    /** "All resources" string for drbdadm commands. */
    public static final String ALL_DRBD_RESOURCES = "all";
    /** To compare versions like 8.3 and 8.4. */
    private static final Pattern DRBD_VERSION_MAJ = Pattern.compile("^(\\d+\\.\\d+)\\..*");

    /**
     * Executes the specified drbd command on the specified host and calls the
     * supplied callback function.
     *
     * @param outputVisible
     *          The flag whether the output should appear in
     *          the terminal panel.
     */
    private static SshOutput execCommand(final Host host,
                                         final String command,
                                         final ExecCallback execCallback,
                                         final boolean outputVisible,
                                         final Application.RunMode runMode) {
        if (Application.isTest(runMode)) {
            if (!command.contains("@DRYRUN@")) {
                /* it would be very bad */
                LOG.appError("execCommand: dry run not available");
                return null;
            }
            String cmd = command.replaceAll("@DRYRUN@", "-d");
            if (cmd.contains("@DRYRUNCONF@")) {
                cmd = cmd.replaceAll("@DRYRUNCONF@", "-c /var/lib/drbd/drbd.conf-lcmc-test");
            }
            final SshOutput output = host.captureCommand(new ExecCommandConfig().command(cmd)
                                                                                .silentCommand()
                                                                                .silentOutput());
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
            if (command.contains("@DRYRUN@")) {
                cmd = command.replaceAll("@DRYRUN@", "");
            } else {
                cmd = command;
            }
            cmd = cmd.replaceAll("@DRYRUNCONF@", "");
            final String progressText = Tools.getString("DRBD.ExecutingCommand")
                                        + ' '
                                        + cmd.replaceAll(DistResource.SUDO, " ")
                                        + "...";
            return host.captureCommandProgressIndicator(progressText,
                                                        new ExecCommandConfig().command(cmd)
                                                                               .execCallback(execCallback)
                                                                               .commandVisible(outputVisible)
                                                                               .outputVisible(outputVisible));
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
                                 final Application.RunMode runMode) {
        return attach(host, resource, volume, null, runMode);
    }

    /** Returns hash that replaces placeholder in commands. */
    private static Map<String, String> getResVolReplaceHash(final Host host,
                                                            final String resource,
                                                            final String volume) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        if (volume != null && host.hasVolumes()) {
            replaceHash.put(RESOURCE_VOLUME_PLACE_HOLDER, resource + '/' + volume);
        } else {
            replaceHash.put(RESOURCE_VOLUME_PLACE_HOLDER, resource);
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
                                 final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.attach", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm detach on the specified host and resource. */
    public static boolean detach(final Host host,
                                 final String resource,
                                 final String volume,
                                 final Application.RunMode runMode) {
        return detach(host, resource, volume, null, runMode);
    }

    /**
     * Executes the drbdadm detach on the specified host and resource
     * and calls the callback function.
     */
    public static boolean detach(final Host host,
                                 final String resource,
                                 final String volume,
                                 final ExecCallback execCallback,
                                 final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.detach", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm connect on the specified host and resource. */
    public static boolean connect(final Host host,
                                  final String resource,
                                  final String volume,
                                  final Application.RunMode runMode) {
        return connect(host, resource, volume, null, runMode);
    }

    /**
     * Executes the drbdadm connect on the specified host and resource
     * and calls the callback function.
     */
    public static boolean connect(final Host host,
                                  final String resource,
                                  final String volume,
                                  final ExecCallback execCallback,
                                  final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.connect", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm disconnect on the specified host and resource. */
    public static boolean disconnect(final Host host,
                                     final String resource,
                                     final String volume,
                                     final Application.RunMode runMode) {
        return disconnect(host, resource, volume, null, runMode);
    }

    /**
     * Executes the drbdadm disconnect on the specified host and resource
     * and calls the callback function.
     */
    public static boolean disconnect(final Host host,
                                     final String resource,
                                     final String volume,
                                     final ExecCallback execCallback,
                                     final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.disconnect", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm pause-sync on the specified host and resource. */
    public static boolean pauseSync(final Host host,
                                    final String resource,
                                    final String volume,
                                    final Application.RunMode runMode) {
        return pauseSync(host, resource, volume, null, runMode);
    }

    /**
     * Executes the drbdadm pause-sync on the specified host and resource
     * and calls the callback function.
     */
    public static boolean pauseSync(final Host host,
                                    final String resource,
                                    final String volume,
                                    final ExecCallback execCallback,
                                    final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.pauseSync", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm resume-sync on the specified host and resource. */
    public static boolean resumeSync(final Host host,
                                     final String resource,
                                     final String volume,
                                     final Application.RunMode runMode) {
        return resumeSync(host, resource, volume, null, runMode);
    }

    /**
     * Executes the drbdadm resume-sync on the specified host and resource
     * and calls the callback function.
     */
    public static boolean resumeSync(final Host host,
                                     final String resource,
                                     final String volume,
                                     final ExecCallback execCallback,
                                     final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.resumeSync", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm primary on the specified host and resource. */
    public static boolean setPrimary(final Host host,
                                     final String resource,
                                     final String volume,
                                     final Application.RunMode runMode) {
        return setPrimary(host, resource, volume, null, runMode);
    }

    /**
     * Executes the drbdadm primary on the specified host and resource
     * and calls the callback function.
     */
    public static boolean setPrimary(final Host host,
                                     final String resource,
                                     final String volume,
                                     final ExecCallback execCallback,
                                     final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.setPrimary", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm secondary on the specified host and resource. */
    public static boolean setSecondary(final Host host,
                                       final String resource,
                                       final String volume,
                                       final Application.RunMode runMode) {
        return setSecondary(host, resource, volume, null, runMode);
    }

    /**
     * Executes the drbdadm secondary on the specified host and resource
     * and calls the callback function.
     */
    public static boolean setSecondary(final Host host,
                                       final String resource,
                                       final String volume,
                                       final ExecCallback execCallback,
                                       final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.setSecondary", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /**
     * Loads the drbd, executes the drbdadm create-md and up on the specified
     * host and resource.
     */
    public static boolean initDrbd(final Host host,
                                   final String resource,
                                   final String volume,
                                   final Application.RunMode runMode) {
        return initDrbd(host, resource, volume, null, runMode);
    }

    /**
     * Loads the drbd, executes the drbdadm create-md and up on the specified
     * host and resource and calls the callback function.
     */
    public static boolean initDrbd(final Host host,
                                   final String resource,
                                   final String volume,
                                   final ExecCallback execCallback,
                                   final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.initDrbd", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
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
                                   final Application.RunMode runMode) {
        final Map<String, String> replaceHash = getResVolReplaceHash(host, resource, volume);
        replaceHash.put(DEVICE_PLACE_HOLDER, device);
        final String command = host.getDistCommand("DRBD.createMD", replaceHash);
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
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
                                              final Application.RunMode runMode) {
        final Map<String, String> replaceHash = getResVolReplaceHash(host, resource, volume);
        replaceHash.put(DEVICE_PLACE_HOLDER, device);
        final String command = host.getDistCommand("DRBD.createMDDestroyData", replaceHash);
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Makes specified filesystem on the specified host and block device. */
    public static boolean makeFilesystem(final Host host,
                                         final String blockDevice,
                                         final String filesystem,
                                         final Application.RunMode runMode) {
        return makeFilesystem(host, blockDevice, filesystem, null, runMode);
    }

    /**
     * Makes specified filesystem on the specified host and block device and
     * calls the callback function.
     */
    public static boolean makeFilesystem(final Host host,
                                         final String blockDevice,
                                         final String filesystem,
                                         final ExecCallback execCallback,
                                         final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(DRBDDEV_PLACE_HOLDER, blockDevice);
        if ("jfs".equals(filesystem) || "reiserfs".equals(filesystem)) {
            replaceHash.put(FILESYSTEM_PLACE_HOLDER, filesystem + " -q");
        } else {
            replaceHash.put(FILESYSTEM_PLACE_HOLDER, filesystem);
        }
        final String command = host.getDistCommand("DRBD.makeFilesystem", replaceHash);
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm -- --clear-bitmap new-current-uuid. */
    public static boolean skipInitialFullSync(final Host host,
                                              final String resource,
                                              final String volume,
                                              final Application.RunMode runMode) {
        return skipInitialFullSync(host, resource, volume, null, runMode);
    }

    /** Executes the drbdadm -- --clear-bitmap new-current-uuid. */
    public static boolean skipInitialFullSync(final Host host,
                                              final String resource,
                                              final String volume,
                                              final ExecCallback execCallback,
                                              final Application.RunMode runMode) {
        try {
            final String command;
            if (host.drbdVersionSmallerOrEqual("8.3")) {
                command = host.getDistCommand("DRBD.skipInitSync.8.3", getResVolReplaceHash(host, resource, volume));
            } else {
                command = host.getDistCommand("DRBD.skipInitSync", getResVolReplaceHash(host, resource, volume));
            }
            final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
            return ret.getExitCode() == 0;
        } catch (final IllegalVersionException e) {
            LOG.appWarning("skipInitialFullSync: " + e.getMessage(), e);
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
                                       final Application.RunMode runMode) {
        return forcePrimary(host, resource, volume, null, runMode);
    }

    /**
     * Executes the drbdadm -- --overwrite-data-of-peer connect on the specified
     * host and resource and calls the callback function.
     */
    public static boolean forcePrimary(final Host host,
                                       final String resource,
                                       final String volume,
                                       final ExecCallback execCallback,
                                       final Application.RunMode runMode) {
        try {
            final String command;
            if (host.drbdVersionSmallerOrEqual("8.3.7")) {
                command = host.getDistCommand("DRBD.forcePrimary.8.3.7", getResVolReplaceHash(host, resource, volume));
            } else if (host.drbdVersionSmallerOrEqual("8.3")) {
                command = host.getDistCommand("DRBD.forcePrimary.8.3", getResVolReplaceHash(host, resource, volume));
            } else {
                command = host.getDistCommand("DRBD.forcePrimary", getResVolReplaceHash(host, resource, volume));
            }
            final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
            return ret.getExitCode() == 0;
        } catch (final IllegalVersionException e) {
            LOG.appWarning("forcePrimary: " + e.getMessage(), e);
            return false;
        }
    }

    /** Executes the drbdadm invalidate on the specified host and resource. */
    public static boolean invalidate(final Host host,
                                     final String resource,
                                     final String volume,
                                     final Application.RunMode runMode) {
        return invalidate(host, resource, volume, null, runMode);
    }

    /**
     * Executes the drbdadm invalidate on the specified host and resource
     * and calls the callback function.
     */
    public static boolean invalidate(final Host host,
                                     final String resource,
                                     final String volume,
                                     final ExecCallback execCallback,
                                     final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.invalidate", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /**
     * Executes the drbdadm -- --discard-my-data connect on the specified
     * host and resource.
     */
    public static boolean discardData(final Host host,
                                      final String resource,
                                      final String volume,
                                      final Application.RunMode runMode) {
        return discardData(host, resource, volume, null, runMode);
    }

    /**
     * Executes the drbdadm -- --discard-my-data connect on the specified
     * host and resource and calls the callback function.
     */
    public static boolean discardData(final Host host,
                                      final String resource,
                                      final String volume,
                                      final ExecCallback execCallback,
                                      final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.discardData", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm resize on the specified host and resource. */
    public static boolean resize(final Host host,
                                 final String resource,
                                 final String volume,
                                 final Application.RunMode runMode) {
        return resize(host, resource, volume, null, runMode);
    }

    /**
     * Executes the drbdadm resize on the specified host and resource and
     * calls the callback function.
     */
    public static boolean resize(final Host host,
                                 final String resource,
                                 final String volume,
                                 final ExecCallback execCallback,
                                 final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.resize", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }


    /** Executes the drbdadm verify on the specified host and resource. */
    public static boolean verify(final Host host,
                                 final String resource,
                                 final String volume,
                                 final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.verify", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, null, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm adjust on the specified host and resource. */
    public static int adjust(final Host host,
                             final String resource,
                             final String volume,
                             final Application.RunMode runMode) {
        return adjustApply(host, resource, null, null, runMode);
    }

    /** Executes the drbdadm adjust on the specified host and resource. */
    public static int adjustApply(final Host host,
                                  final String resource,
                                  final String volume,
                                  final Application.RunMode runMode) {
        return adjustApply(host, resource, null, null, runMode);
    }

    /**
     * Executes the drbdadm adjust on the specified host and resource and
     * calls the callback function.
     */
    public static int adjustApply(final Host host,
                                  final String resource,
                                  final String volume,
                                  final ExecCallback execCallback,
                                  final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.adjust.apply", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, false, runMode);
        
        final Pattern p = Pattern.compile(".*Failure: \\((\\d+)\\).*", Pattern.DOTALL);
        final Matcher m = p.matcher(ret.getOutput());
        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        }
        return ret.getExitCode();
    }

    /** Executes the drbdadm proxy-up on the specified host and resource. */
    public static int proxyUp(final Host host,
                              final String resource,
                              final String volume,
                              final Application.RunMode runMode) {
        return proxyUp(host, resource, null, null, runMode);
    }

    /**
     * Executes the drbdadm proxy-up on the specified host and resource and
     * calls the callback function.
     */
    public static int proxyUp(final Host host,
                              final String resource,
                              final String volume,
                              final ExecCallback execCallback,
                              final Application.RunMode runMode) {
        final String command = getDistCommand("DRBD.proxyUp", host, resource, volume);
        final SshOutput ret = execCommand(host, command, execCallback, false, runMode);
        
        return ret.getExitCode();
    }

    /** Executes the drbdadm proxy-down on the specified host and resource. */
    public static int proxyDown(final Host host,
                                final String resource,
                                final String volume,
                                final Application.RunMode runMode) {
        return proxyDown(host, resource, null, null, runMode);
    }

    /**
     * Executes the drbdadm proxy-up on the specified host and resource and
     * calls the callback function.
     */
    public static int proxyDown(final Host host,
                                final String resource,
                                final String volume,
                                final ExecCallback execCallback,
                                final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.proxyDown", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, false, runMode);
        
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
                               final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.get-gi", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, null, false, runMode);
        if (ret.getExitCode() == 0) {
            return ret.getOutput();
        }
        return null;
    }

    /** Executes the drbdadm down on the specified host and resource. */
    public static boolean down(final Host host,
                               final String resource,
                               final String volume,
                               final Application.RunMode runMode) {
        return down(host, resource, volume, null, runMode);
    }

    /**
     * Executes the drbdadm down on the specified host and resource and
     * calls the callback function.
     */
    public static boolean down(final Host host,
                               final String resource,
                               final String volume,
                               final ExecCallback execCallback,
                               final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.down", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Executes the drbdadm up on the specified host and resource. */
    public static boolean up(final Host host,
                             final String resource,
                             final String volume,
                             final Application.RunMode runMode) {
        return up(host, resource, volume, null, runMode);
    }

    /**
     * Executes the drbdadm up on the specified host and resource and calls the
     * callback function.
     */
    public static boolean up(final Host host,
                             final String resource,
                             final String volume,
                             final ExecCallback execCallback,
                             final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.up", getResVolReplaceHash(host, resource, volume));
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Start the drbd. Probably /etc/init.d/drbd start. */
    public static boolean start(final Host host, final Application.RunMode runMode) {
        return start(host, null, runMode);
    }

    /**
     * Start the drbd. Probably /etc/init.d/drbd start and calls the callback
     * function after it is done.
     */
    public static boolean start(final Host host, final ExecCallback execCallback, final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.start", (ConvertCmdCallback) null);
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;

    }

    /** Executes load drbd command on the specified host. */
    public static boolean load(final Host host, final Application.RunMode runMode) {
        return load(host, null, runMode);
    }

    /**
     * Executes load drbd command on the specified host and calls the callback
     * function after it is done.
     */
    public static boolean load(final Host host, final ExecCallback execCallback, final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.load", (ConvertCmdCallback) null);
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Start DRBD proxy on the specified host. */
    public static boolean startProxy(final Host host, final Application.RunMode runMode) {
        return startProxy(host, null, runMode);
    }

    /**
     * Start DRBD proxy on the specified host and call the callback function
     * after it is done.
     */
    public static boolean startProxy(final Host host,
                                     final ExecCallback execCallback,
                                     final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.startProxy", (ConvertCmdCallback) null);
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        host.updateDrbdParameters(); /* deamon could be started even if ret != 0
        */
        return ret.getExitCode() == 0;
    }

    /** Stop DRBD proxy on the specified host. */
    public static boolean stopProxy(final Host host, final Application.RunMode runMode) {
        return stopProxy(host, null, runMode);
    }

    /**
     * Stop DRBD proxy on the specified host and call the callback function
     * after it is done.
     */
    public static boolean stopProxy(final Host host,
                                    final ExecCallback execCallback,
                                    final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.stopProxy", (ConvertCmdCallback) null);
        final SshOutput ret = execCommand(host, command, execCallback, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Delete minor, from DRBD 8.4. */
    public static boolean delMinor(final Host host, final String blockDevice, final Application.RunMode runMode) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put(DRBDDEV_PLACE_HOLDER, blockDevice);
        final String command = host.getDistCommand("DRBD.delMinor", replaceHash);
        final SshOutput ret = execCommand(host, command, null, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Delete connection, from DRBD 8.4. */
    public static boolean delConnection(final Host host, final String resource, final Application.RunMode runMode) {
        final String command = host.getDistCommand("DRBD.resDelConnection", getResVolReplaceHash(host, resource, null));
        final SshOutput ret = execCommand(host, command, null, true, runMode);
        return ret.getExitCode() == 0;
    }

    /** Return command with replaced placeholders. */
    public static String getDistCommand(final String cmd, final Host host, final String resource, final String volume) {
        return host.getDistCommand(cmd, getResVolReplaceHash(host, resource, volume));
    }

    /** Return whether DRBD util and module versions are compatible. */
    public static boolean compatibleVersions(final String utilV, final String moduleV) {
        if (utilV == null || moduleV == null) {
            return false;
        }
        final String uV;
        final Matcher matcherU = DRBD_VERSION_MAJ.matcher(utilV);
        if (matcherU.matches()) {
            uV = matcherU.group(1);
        } else {
            return false;
        }

        final String mV;
        final Matcher matcherM = DRBD_VERSION_MAJ.matcher(moduleV);
        if (matcherM.matches()) {
            mV = matcherM.group(1);
        } else {
            return false;
        }
        try {
            if (Tools.compareVersions(uV, "8.9") >= 0) {
                return Tools.compareVersions(moduleV, "8.4.3") >= 0;
            }
            return Tools.compareVersions(mV, uV) == 0;
        } catch (final IllegalVersionException e) {
            return false;
        }
    }

    /** Private constructor, cannot be instantiated. */
    private DRBD() {
        /* Cannot be instantiated. */
    }
}
