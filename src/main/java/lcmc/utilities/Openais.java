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

import lcmc.model.Host;
import lcmc.utilities.ssh.ExecCommandConfig;

/**
 * This class provides openais commands. There are commands that
 * operate on /etc/init.d/openais script and commands etc.
 */
public final class Openais {
    /** Directory that contains ais config files. */
    private static final String AIS_CONF_DIR = "/etc/ais/";
    /** Main openais.conf config file. */
    private static final String AIS_CONF_NAME = "openais.conf";
    /** Permissions of the openais.conf config file. */
    private static final String AIS_CONF_PERMS = "0600";
    /** Authkeys config file. */
    private static final String AUTHKEYS_CONF_NAME = "authkey";
    /** Permissions of the authkeys config file. */
    private static final String AUTHKEYS_CONF_PERMS = "0400";

    /** Executes specified command on the host. */
    private static void execCommand(final Host host, final String command) {
        host.execCommandProgressIndicator(Tools.getString("Openais.ExecutingCommand"),
                                          new ExecCommandConfig().command(command)
                                                                 .sshCommandTimeout(180000));
    }

    /** Stops the heartbeat and starts the openais on the specified host. */
    public static void switchToOpenais(final Host host) {
        final String command = host.getDistCommand("Heartbeat.deleteFromRc"
                                                   + ";;;Openais.addToRc"
                                                   + ";;;Openais.startOpenais",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command);
    }

    /**
     * /etc/init.d/openais start
     */
    public static void startOpenais(final Host host) {
        final String command = host.getDistCommand("Openais.startOpenais", (ConvertCmdCallback) null);
        execCommand(host, command);
    }

    /**
     * /etc/init.d/openais stop
     */
    public static void stopOpenais(final Host host) {
        final String command = host.getDistCommand("Openais.stopOpenais", (ConvertCmdCallback) null);
        execCommand(host, command);
    }

    /**
     * /etc/init.d/openais stop && /etc/init.d/pacemaker stop
     */
    public static void stopOpenaisWithPcmk(final Host host) {
        final String command = host.getDistCommand("Openais.stopOpenaisWithPcmk", (ConvertCmdCallback) null);
        execCommand(host, command);
    }

    public static void startOpenaisRc(final Host host) {
        final String command = host.getDistCommand("Openais.startOpenais" + ";;;Openais.addToRc",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command);
    }

    public static void addOpenaisToRc(final Host host) {
        final String command = host.getDistCommand("Openais.addToRc", (ConvertCmdCallback) null);
        execCommand(host, command);
    }

    /**
     * /etc/init.d/openais reload
     */
    public static void reloadOpenais(final Host host) {
        final String command = host.getDistCommand("Openais.reloadOpenais", (ConvertCmdCallback) null);
        execCommand(host, command);
    }

    public static void createAISConfig(final Host[] hosts, final StringBuilder config) {
        /* write heartbeat config on all hosts */
        Tools.createConfigOnAllHosts(hosts, config.toString(), AIS_CONF_NAME, AIS_CONF_DIR, AIS_CONF_PERMS, true);
        final StringBuilder authkeys = new StringBuilder(Tools.getRandomSecret(128));
        Tools.createConfigOnAllHosts(hosts,
                                     authkeys.toString(),
                                     AUTHKEYS_CONF_NAME,
                                     AIS_CONF_DIR,
                                     AUTHKEYS_CONF_PERMS,
                                     true);

    }

    public static void reloadOpenaises(final Host[] hosts) {
        for (final Host host : hosts) {
            reloadOpenais(host);
        }
    }

    private Openais() { }
}
