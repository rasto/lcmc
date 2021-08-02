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

package lcmc.crm.infrastrucure;

import lcmc.cluster.infrastructure.ssh.ExecCommandConfig;
import lcmc.common.domain.ConvertCmdCallback;
import lcmc.common.domain.util.Tools;
import lcmc.host.domain.Host;

/**
 * This class provides corosync commands. There are commands that
 * operate on /etc/init.d/corosync script and commands etc.
 */
public final class Corosync {
    /** Directory that contains Corosync config files. */
    private static final String COROSYNC_CONF_DIR = "/etc/corosync/";
    /** Main corosync.conf config file. */
    private static final String COROSYNC_CONF_NAME = "corosync.conf";
    /** Permissions of the corosync.conf config file. */
    private static final String COROSYNC_CONF_PERMS = "0600";
    /** Authkeys config file. */
    private static final String AUTHKEYS_CONF_NAME = "authkey";
    /** Permissions of the authkeys config file. */
    private static final String AUTHKEYS_CONF_PERMS = "0400";

    private static final String PROGRESS_TEXT = Tools.getString("Corosync.ExecutingCommand");

    /** Stops the heartbeat and starts the corosync on the specified host. */
    public static void switchToCorosync(final Host host) {
        final String command = host.getDistCommand("Heartbeat.deleteFromRc"
                                                   + ";;;Corosync.addToRc"
                                                   + ";;;Corosync.startCorosync",
                                                   (ConvertCmdCallback) null);
        host.execCommandProgressIndicator(PROGRESS_TEXT, new ExecCommandConfig().command(command));
    }

    /**
     * Starts corosync on host.
     * /etc/init.d/corosync start
     */
    public static void startCorosync(final Host host) {
        final String command = host.getDistCommand("Corosync.startCorosync", (ConvertCmdCallback) null);
        host.execCommandProgressIndicator(PROGRESS_TEXT, new ExecCommandConfig().command(command));
    }

    /**
     * Starts corosync with pacemaker on host.
     * /etc/init.d/corosync start && /etc/init.d/pacemaker start
     */
    public static void startCorosyncWithPcmk(final Host host) {
        final String command = host.getDistCommand("Corosync.startCorosyncWithPcmk", (ConvertCmdCallback) null);
        host.execCommandProgressIndicator(PROGRESS_TEXT, new ExecCommandConfig().command(command));
    }

    /**
     * Starts pacemaker on host.
     * /etc/init.d/pacemaker start
     */
    public static void startPacemaker(final Host host) {
        final String command = host.getDistCommand("Corosync.startPcmk", (ConvertCmdCallback) null);
        host.execCommandProgressIndicator(PROGRESS_TEXT, new ExecCommandConfig().command(command));
    }


    /**
     * Stops corosync on host.
     * /etc/init.d/corosync stop
     */
    public static void stopCorosync(final Host host) {
        final String command = host.getDistCommand("Corosync.stopCorosync", (ConvertCmdCallback) null);
        host.execCommandProgressIndicator(PROGRESS_TEXT, new ExecCommandConfig().command(command));
    }

    /**
     * Stops corosync with pacemaker on host.
     * /etc/init.d/corosync stop && /etc/init.d/pacemaker stop
     */
    public static void stopCorosyncWithPcmk(final Host host) {
        final String command = host.getDistCommand("Corosync.stopCorosyncWithPcmk", (ConvertCmdCallback) null);
        host.execCommandProgressIndicator(PROGRESS_TEXT, new ExecCommandConfig().command(command));
    }

    /** Starts Corosync on host and adds it to the rc. */
    public static void startCorosyncRc(final Host host) {
        final String command = host.getDistCommand("Corosync.startCorosync"
                                                   + ";;;Corosync.addToRc",
                                                   (ConvertCmdCallback) null);
        host.execCommandProgressIndicator(PROGRESS_TEXT, new ExecCommandConfig().command(command));
    }

    /** Adds Corosync to the rc. */
    public static void addCorosyncToRc(final Host host) {
        final String command = host.getDistCommand("Corosync.addToRc", (ConvertCmdCallback) null);
        host.execCommandProgressIndicator(PROGRESS_TEXT, new ExecCommandConfig().command(command));
    }

    /**
     * Reloads Corosync's configuration on host.
     * /etc/init.d/corosync reload
     */
    public static void reloadCorosync(final Host host) {
        final String command = host.getDistCommand("Corosync.reloadCorosync", (ConvertCmdCallback) null);
        host.execCommandProgressIndicator(PROGRESS_TEXT, new ExecCommandConfig().command(command));
    }

    /** Creates Corosync config on specified hosts. */
    public static void createCorosyncConfig(final Host[] hosts, final StringBuilder config) {
        /* write heartbeat config on all hosts */
        Tools.createConfigOnAllHosts(hosts,
                                     config.toString(),
                                     COROSYNC_CONF_NAME,
                                     COROSYNC_CONF_DIR,
                                     COROSYNC_CONF_PERMS,
                                     true);
        Tools.createConfigOnAllHosts(hosts,
                                     Tools.getRandomSecret(128),
                                     AUTHKEYS_CONF_NAME,
                                     COROSYNC_CONF_DIR,
                                     AUTHKEYS_CONF_PERMS,
                                     true);

    }

    /** Reloads Corosync daemons on all nodes. */
    public static void reloadCorosyncs(final Host[] hosts) {
        for (final Host host : hosts) {
            reloadCorosync(host);
        }
    }

    /** No instantiation. */
    private Corosync() { }
}
