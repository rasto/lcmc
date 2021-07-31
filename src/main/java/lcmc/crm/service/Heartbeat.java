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

package lcmc.crm.service;

import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.common.domain.ConvertCmdCallback;
import lcmc.common.domain.util.Tools;
import lcmc.host.domain.Host;

/**
 * This class provides heartbeat commands. There are commands that
 * operate on /etc/init.d/heartbeat script etc.
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

    /** Executes specified command on the host. */
    private static void execCommand(final Host host, final String command) {
        host.execCommandProgressIndicator(Tools.getString("Heartbeat.ExecutingCommand"),
                                          new ExecCommandConfig().command(command)
                                                                 .sshCommandTimeout(180000));
    }

    /**
     * Starts heartbeat on host.
     * /etc/init.d/heartbeat start
     */
    public static void startHeartbeat(final Host host) {
        final String command = host.getDistCommand("Heartbeat.startHeartbeat", (ConvertCmdCallback) null);
        execCommand(host, command);
    }

    /**
     * Stops heartbeat on host.
     * /etc/init.d/heartbeat stop
     */
    public static void stopHeartbeat(final Host host) {
        final String command = host.getDistCommand("Heartbeat.stopHeartbeat", (ConvertCmdCallback) null);
        execCommand(host, command);
    }

    /** Starts heartbeat on host and adds it to the rc. */
    public static void startHeartbeatRc(final Host host) {
        final String command = host.getDistCommand("Heartbeat.startHeartbeat;;;Heartbeat.addToRc",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command);
    }

    /** Stops the corosync and starts the heartbeat on the specified host. */
    public static void switchFromCorosyncToHeartbeat(final Host host) {
        final String command = host.getDistCommand("Corosync.deleteFromRc"
                                                   + ";;;Heartbeat.addToRc"
                                                   + ";;;Heartbeat.startHeartbeat",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command);
    }

    /** Stops the openais and starts the heartbeat on the specified host. */
    public static void switchFromOpenaisToHeartbeat(final Host host) {
        final String command = host.getDistCommand("Openais.deleteFromRc"
                                                   + ";;;Heartbeat.addToRc"
                                                   + ";;;Heartbeat.startHeartbeat",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command);
    }

    /** Adds heartbeat to the rc. */
    public static void addHeartbeatToRc(final Host host) {
        final String command = host.getDistCommand("Heartbeat.addToRc", (ConvertCmdCallback) null);
        execCommand(host, command);
    }

    /**
     * Reloads heartbeat's configuration on host.
     * /etc/init.d/heartbeat reload
     */
    public static void reloadHeartbeat(final Host host) {
        final String command = host.getDistCommand("Heartbeat.reloadHeartbeat", (ConvertCmdCallback) null);
        execCommand(host, command);
    }

    /** Creates heartbeat config on specified hosts. */
    public static void createHBConfig(final Host[] hosts, final StringBuilder config) {
        /* write heartbeat config on all hosts */
        Tools.createConfigOnAllHosts(hosts, config.toString(), HA_CONF_NAME, HA_CONF_DIR, HA_CONF_PERMS, true);

        String authkeys = "## generated by drbd-gui\n\n" + "auth 1\n" + "1 sha1 " + Tools.getRandomSecret(32) + '\n';
        Tools.createConfigOnAllHosts(hosts, authkeys, AUTHKEYS_CONF_NAME, HA_CONF_DIR, AUTHKEYS_CONF_PERMS, true);
    }

    /** Reloads heartbeats on all nodes. */
    public static void reloadHeartbeats(final Host[] hosts) {
        for (final Host host : hosts) {
            reloadHeartbeat(host);
        }
    }

    /**
     * Enables dopd.
     * With workaround for dopd, that needs /var/run/heartbeat/crm directory.
     */
    public static void enableDopd(final Host host, final boolean workAround) {
        final String cmd;
        if (workAround) {
            cmd = "Heartbeat.dopdWorkaround;;;Heartbeat.enableDopd";
        } else {
            cmd = "Heartbeat.enableDopd";
        }
        host.execCommand(new ExecCommandConfig().commandString(cmd)).block();
    }

    /** No instantiation. */
    private Heartbeat() { }
}
