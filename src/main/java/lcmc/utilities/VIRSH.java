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

package lcmc.utilities;

import lcmc.data.Host;
import lcmc.configs.DistResource;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;

/**
 * This class provides virsh commands.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class VIRSH {
    /** Virsh command map. */
    private static final Map<String, String> VIRSH_COMMANDS =
                                                 new HashMap<String, String>();
    static {
        VIRSH_COMMANDS.put("autostart",
                           "/usr/bin/virsh autostart @VALUE@ @DOMAIN@");
    }

    /** Private constructor, cannot be instantiated. */
    private VIRSH() {
        /* Cannot be instantiated. */
    }


    /** Executes the specified virsh commands on the specified host. */
    private static boolean execCommand(final Host host,
                                       final String commands,
                                       final boolean outputVisible) {
        if (host.isConnected()) {
            final SSH.SSHOutput ret =
                            Tools.execCommandProgressIndicator(
                                 host,
                                 commands,
                                 null,
                                 outputVisible,
                                 Tools.getString("VIRSH.ExecutingCommand")
                                 + " "
                                 + commands.replaceAll(DistResource.SUDO, " ")
                                 + "...",
                                 SSH.DEFAULT_COMMAND_TIMEOUT);
            if (ret.getExitCode() != 0) {
                return false;
            }
        }
        return true;
    }

    /** Executes the specified virsh commands on the specified hosts. */
    private static boolean execCommand(final Host[] hosts,
                                       final Map<Host, String> hostCommands,
                                       final boolean outputVisible) {
        boolean exitCode = false;
        for (final Host host : hosts) {
            final String commands = hostCommands.get(host);
            if (commands.length() > 0) {
                if (!execCommand(host, commands, outputVisible)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Sets paramters with virsh command. */
    public static void setParameters(final Host[] hosts,
                                     final String domainName,
                                     final Map<String, String> parameters) {
        final Map<Host, String> hostCommands = new HashMap<Host, String>();
        for (final Host host : hosts) {
            final StringBuilder commands = new StringBuilder(100);
            for (final String param : parameters.keySet()) {
                String command = VIRSH_COMMANDS.get(param);
                if (command == null) {
                    continue;
                }
                if (command.indexOf("@DOMAIN@") >= 0) {
                    command = command.replaceAll(
                                              "@DOMAIN@",
                                              Matcher.quoteReplacement(domainName));
                }
                if (command.indexOf("@VALUE@") >= 0) {
                    String value = parameters.get(param);
                    if ("autostart".equals(param)) {
                        if (value == null || !value.equals(host.getName())) {
                            value = "--disable";
                        } else {
                            value = "";
                        }
                    }
                    command = command.replaceAll("@VALUE@", value);
                }
                if (commands.length() > 0) {
                    commands.append(" && ");
                }
                commands.append(command);
                hostCommands.put(host, commands.toString());
            }
        }
        execCommand(hosts, hostCommands, true);
    }

    /** Starts virtual domain. */
    public static boolean start(final Host host, final String domain) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domain);
        final String command = host.getDistCommand("VIRSH.Start",
                                                   replaceHash);
        return execCommand(host, command, true);
    }

    /** Shuts down virtual domain. */
    public static boolean shutdown(final Host host, final String domain) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domain);
        final String command = host.getDistCommand("VIRSH.Shutdown",
                                                   replaceHash);
        return execCommand(host, command, true);
    }

    /** Reboots virtual domain. */
    public static boolean reboot(final Host host, final String domain) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domain);
        final String command = host.getDistCommand("VIRSH.Reboot",
                                                   replaceHash);
        return execCommand(host, command, true);
    }

    /** Destroys virtual domain. */
    public static boolean destroy(final Host host, final String domain) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domain);
        final String command = host.getDistCommand("VIRSH.Destroy",
                                                   replaceHash);
        return execCommand(host, command, true);
    }

    /** Suspends virtual domain. */
    public static boolean suspend(final Host host, final String domain) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domain);
        final String command = host.getDistCommand("VIRSH.Suspend",
                                                   replaceHash);
        return execCommand(host, command, true);
    }

    /** Resumes virtual domain. */
    public static boolean resume(final Host host, final String domain) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domain);
        final String command = host.getDistCommand("VIRSH.Resume",
                                                   replaceHash);
        return execCommand(host, command, true);
    }

    /**
     * Defines virtual domain. It rereads the config from XML, but does not
     * start the domain like "create" would.
     */
    public static boolean define(final Host host, final String config) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@CONFIG@", config);
        final String command = host.getDistCommand("VIRSH.Define",
                                                   replaceHash);
        return execCommand(host, command, true);
    }

    /** Returns command that defines virtual domain. */
    public static String getDefineCommand(final Host host,
                                          final String config) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@CONFIG@", config);
        final String command = host.getDistCommand("VIRSH.Define",
                                                   replaceHash);
        return command;
    }

    /** Undefines virtual domain. It removes the config. */
    public static boolean undefine(final Host host, final String domain) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domain);
        final String command = host.getDistCommand("VIRSH.Undefine",
                                                   replaceHash);
        return execCommand(host, command, true);
    }
}
