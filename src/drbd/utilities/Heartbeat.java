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
 * This class provides heartbeat commands. There are commands that
 * operate on /etc/init.d/heartbeat script etc.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Heartbeat {
    /** Directory that contains ha config files. */
    private static final String HA_CONF_DIR = "/etc/ha.d/";
    /** Main heartbeat config file. */
    private static final String HA_CONF_NAME = "ha.cf";
    /** Permissions of the main heartbeat config file. */
    private static final String HA_CONF_PERMS = "0600";
    /** Authkeys config file. */
    private static final String AUTHKEYS_CONF_NAME = "authkeys";
    /** Permissions of the authkeys config file. */
    private static final String AUTHKEYS_CONF_PERMS = "0600";

    /** No instantiation. */
    private Heartbeat() { }

    /** Executes specified command on the host. */
    private static void execCommand(final Host host,
                                    final String command,
                                    final boolean outputVisible) {
        Tools.execCommandProgressIndicator(
                                host,
                                command,
                                null,
                                outputVisible,
                                Tools.getString("Heartbeat.ExecutingCommand"),
                                180000);
    }

    /**
     * Starts heartbeat on host.
     * /etc/init.d/heartbeat start
     */
    public static void startHeartbeat(final Host host) {
        final String command = host.getDistCommand("Heartbeat.startHeartbeat",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /**
     * Stops heartbeat on host.
     * /etc/init.d/heartbeat stop
     */
    public static void stopHeartbeat(final Host host) {
        final String command = host.getDistCommand("Heartbeat.stopHeartbeat",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /** Starts heartbeat on host and adds it to the rc. */
    public static void startHeartbeatRc(final Host host) {
        final String command = host.getDistCommand("Heartbeat.startHeartbeat"
                                                   + ";;;Heartbeat.addToRc",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /** Stops the corosync and starts the heartbeat on the specified host. */
    public static void switchFromCorosyncToHeartbeat(final Host host) {
        final String command = host.getDistCommand(
                                    "Corosync.deleteFromRc"
                                    + ";;;Heartbeat.addToRc"
                                    + ";;;Heartbeat.startHeartbeat",
                                    (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /** Stops the openais and starts the heartbeat on the specified host. */
    public static void switchFromOpenaisToHeartbeat(final Host host) {
        final String command = host.getDistCommand(
                                    "Openais.deleteFromRc"
                                    + ";;;Heartbeat.addToRc"
                                    + ";;;Heartbeat.startHeartbeat",
                                    (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /** Adds heartbeat to the rc. */
    public static void addHeartbeatToRc(final Host host) {
        final String command = host.getDistCommand("Heartbeat.addToRc",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /**
     * Reloads heartbeat's configuration on host.
     * /etc/init.d/heartbeat reload
     */
    public static void reloadHeartbeat(final Host host) {
        final String command = host.getDistCommand("Heartbeat.reloadHeartbeat",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /** Creates heartbeat config on specified hosts. */
    public static void createHBConfig(final Host[] hosts,
                                      final StringBuffer config) {
        /* write heartbeat config on all hosts */
        Tools.createConfigOnAllHosts(hosts,
                                     config.toString(),
                                     HA_CONF_NAME,
                                     HA_CONF_DIR,
                                     HA_CONF_PERMS,
                                     true);

        final StringBuffer authkeys =
            new StringBuffer("## generated by drbd-gui\n\n"
                             + "auth 1\n"
                             + "1 sha1 ");
        authkeys.append(Tools.getRandomSecret(32));
        authkeys.append('\n');
        Tools.createConfigOnAllHosts(hosts,
                                     authkeys.toString(),
                                     AUTHKEYS_CONF_NAME,
                                     HA_CONF_DIR,
                                     AUTHKEYS_CONF_PERMS,
                                     true);
    }

    /** Reloads heartbeats on all nodes. */
    public static void reloadHeartbeats(final Host[] hosts) {
        for (Host host : hosts) {
            reloadHeartbeat(host);
        }
    }

    /**
     * Enables dopd.
     * With workaround for dopd, that needs /var/run/heartbeat/crm directory.
     */
    public static void enableDopd(final Host host, final boolean workAround) {
        String cmd;
        if (workAround) {
            cmd = "Heartbeat.dopdWorkaround;;;Heartbeat.enableDopd";
        } else {
            cmd = "Heartbeat.enableDopd";
        }
        final Thread t = host.execCommand(cmd,
                                          null,
                                          null,
                                          true,
                                          SSH.DEFAULT_COMMAND_TIMEOUT);
        try {
            t.join();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
