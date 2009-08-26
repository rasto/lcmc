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
 * This class provides openais commands. There are commands that
 * operate on /etc/init.d/openais script and commands etc.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Openais {
    /** Directory that contains ais config files. */
    private static final String AIS_CONF_DIR = "/etc/ais/";
    /** Main openais.conf config file. */
    private static final String AIS_CONF_NAME = "openais.conf";
    /** Permissions of the openais.conf config file. */
    private static final String AIS_CONF_PERMS = "0600";

    /**
     * No instantiation.
     */
    private Openais() { }

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
                                Tools.getString("Openais.ExecutingCommand"));
    }

    /**
     * Stops the heartbeat and starts the openais on the specified host.
     */
    public static void switchToOpenais(final Host host) {
        final String command = host.getDistCommand(
                                                "Heartbeat.deleteFromRc"
                                                + ";;;Openais.addToRc"
                                                + ";;;Openais.startOpenais",
                                                (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /**
     * Starts openais on host.
     * /etc/init.d/openais start
     */
    public static void startOpenais(final Host host) {
        final String command = host.getDistCommand("Openais.startOpenais",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /**
     * Starts openais on host and adds it to the rc.
     */
    public static void startOpenaisRc(final Host host) {
        final String command = host.getDistCommand("Openais.startOpenais"
                                                   + ";;;Openais.addToRc",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /**
     * Adds openais to the rc.
     */
    public static void addOpenaisToRc(final Host host) {
        final String command = host.getDistCommand("Openais.addToRc",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /**
     * Reloads openais's configuration on host.
     * /etc/init.d/openais reload
     */
    public static void reloadOpenais(final Host host) {
        final String command = host.getDistCommand("Openais.reloadOpenais",
                                                   (ConvertCmdCallback) null);
        execCommand(host, command, true);
    }

    /**
     * Creates OpenAIS config on specified hosts.
     */
    public static void createAISConfig(final Host[] hosts,
                                       final StringBuffer config) {
        /* write heartbeat config on all hosts */
        Tools.createConfigOnAllHosts(hosts,
                                     config.toString(),
                                     AIS_CONF_NAME,
                                     AIS_CONF_DIR,
                                     AIS_CONF_PERMS,
                                     true);

    }

    /**
     * Reloads openais daemons on all nodes.
     */
    public static void reloadOpenaises(final Host[] hosts) {
        for (Host host : hosts) {
            reloadOpenais(host);
        }
    }
}
