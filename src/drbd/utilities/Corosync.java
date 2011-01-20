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
/**
 * This class provides corosync commands. There are commands that
 * operate on /etc/init.d/corosync script and commands etc.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
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

    /**
     * No instantiation.
     */
    private Corosync() { }

    /**
     * Executes specified command on the host.
     */
    private static void execCommand(final Host host,
                                    final String command,
                                    final boolean outputVisible) {
        Tools.execCommandProgressIndicator(
                                host,
                                command,
                                null,
                                outputVisible,
                                Tools.getString("Corosync.ExecutingCommand"),
                                SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /**
     * Stops the heartbeat and starts the corosync on the specified host.
     */
    public static void switchToCorosync(final Host host) {
        final String command = host.getDistCommand(
                                            "Heartbeat.deleteFromRc"
                                            + ";;;Corosync.addToRc"
                                            + ";;;Corosync.startCorosync",
                                            (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /**
     * Starts corosync on host.
     * /etc/init.d/corosync start
     */
    public static void startCorosync(final Host host) {
        final String command = host.getDistCommand("Corosync.startCorosync",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /**
     * Stops corosync on host.
     * /etc/init.d/corosync stop
     */
    public static void stopCorosync(final Host host) {
        final String command = host.getDistCommand("Corosync.stopCorosync",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /**
     * Starts Corosync on host and adds it to the rc.
     */
    public static void startCorosyncRc(final Host host) {
        final String command = host.getDistCommand(
                                            "Corosync.startCorosync"
                                            + ";;;Corosync.addToRc",
                                            (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /**
     * Adds Corosync to the rc.
     */
    public static void addCorosyncToRc(final Host host) {
        final String command = host.getDistCommand("Corosync.addToRc",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /**
     * Reloads Corosync's configuration on host.
     * /etc/init.d/corosync reload
     */
    public static void reloadCorosync(final Host host) {
        final String command = host.getDistCommand("Corosync.reloadCorosync",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /**
     * Creates Corosync config on specified hosts.
     */
    public static void createCorosyncConfig(final Host[] hosts,
                                       final StringBuffer config) {
        /* write heartbeat config on all hosts */
        Tools.createConfigOnAllHosts(hosts,
                                     config.toString(),
                                     COROSYNC_CONF_NAME,
                                     COROSYNC_CONF_DIR,
                                     COROSYNC_CONF_PERMS,
                                     true);
        final StringBuffer authkeys =
                                  new StringBuffer(Tools.getRandomSecret(128));
        Tools.createConfigOnAllHosts(hosts,
                                     authkeys.toString(),
                                     AUTHKEYS_CONF_NAME,
                                     COROSYNC_CONF_DIR,
                                     AUTHKEYS_CONF_PERMS,
                                     true);

    }

    /**
     * Reloads Corosync daemons on all nodes.
     */
    public static void reloadCorosyncs(final Host[] hosts) {
        for (Host host : hosts) {
            reloadCorosync(host);
        }
    }
}
