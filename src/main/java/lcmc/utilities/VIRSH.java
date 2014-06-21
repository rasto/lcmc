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

import java.util.HashMap;
import java.util.Map;

import lcmc.configs.DistResource;
import lcmc.data.Host;
import lcmc.utilities.ssh.ExecCommandConfig;
import lcmc.utilities.ssh.Ssh;
import lcmc.utilities.ssh.SshOutput;

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
        VIRSH_COMMANDS.put("autostart", "VIRSH.Autostart");
    }

    /** Executes the specified virsh commands on the specified host. */
    private static boolean execCommand(final Host host, final String commands) {
        if (host.isConnected()) {
            final SshOutput ret = host.captureCommandProgressIndicator(Tools.getString("VIRSH.ExecutingCommand")
                                                                       + ' '
                                                                       + commands.replaceAll(DistResource.SUDO, " ")
                                                                       + "...",
                                                                       new ExecCommandConfig().command(commands));
            if (ret.getExitCode() != 0) {
                return false;
            }
        }
        return true;
    }


    /** Executes the specified virsh commands on the specified hosts. */
    private static boolean execCommand(final Host[] hosts, final Map<Host, String> hostCommands) {
        for (final Host host : hosts) {
            final String commands = hostCommands.get(host);
            if (!commands.isEmpty()) {
                if (!execCommand(host, commands)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Sets paramters with virsh command. */
    public static void setParameters(final Host[] hosts, final String domainName, final Map<String, String> parameters, final String options) {
        final Map<Host, String> hostCommands = new HashMap<Host, String>();
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domainName);
        replaceHash.put("@OPTIONS@", options);
        for (final Host host : hosts) {
            final StringBuilder commands = new StringBuilder(100);
            for (final Map.Entry<String, String> paramEntry : parameters.entrySet()) {
                String command = host.getDistCommand(VIRSH_COMMANDS.get(paramEntry.getKey()),
                                                     replaceHash);
                if (command == null) {
                    continue;
                }
                if (command.contains("@VALUE@")) {
                    String value = paramEntry.getValue();
                    if ("autostart".equals(paramEntry.getKey())) {
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
        execCommand(hosts, hostCommands);
    }

    /** Starts virtual domain. */
    public static boolean start(final Host host, final String domain, final String options) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domain);
        replaceHash.put("@OPTIONS@", options);
        final String command = host.getDistCommand("VIRSH.Start", replaceHash);
        return execCommand(host, command);
    }

    /** Shuts down virtual domain. */
    public static boolean shutdown(final Host host, final String domain, final String options) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domain);
        replaceHash.put("@OPTIONS@", options);
        final String command = host.getDistCommand("VIRSH.Shutdown", replaceHash);
        return execCommand(host, command);
    }

    /** Reboots virtual domain. */
    public static boolean reboot(final Host host, final String domain, final String options) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domain);
        replaceHash.put("@OPTIONS@", options);
        final String command = host.getDistCommand("VIRSH.Reboot", replaceHash);
        return execCommand(host, command);
    }

    /** Destroys virtual domain. */
    public static boolean destroy(final Host host, final String domain, final String options) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domain);
        replaceHash.put("@OPTIONS@", options);
        final String command = host.getDistCommand("VIRSH.Destroy", replaceHash);
        return execCommand(host, command);
    }

    /** Suspends virtual domain. */
    public static boolean suspend(final Host host, final String domain, final String options) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domain);
        replaceHash.put("@OPTIONS@", options);
        final String command = host.getDistCommand("VIRSH.Suspend", replaceHash);
        return execCommand(host, command);
    }

    /** Resumes virtual domain. */
    public static boolean resume(final Host host, final String domain, final String options) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domain);
        replaceHash.put("@OPTIONS@", options);
        final String command = host.getDistCommand("VIRSH.Resume", replaceHash);
        return execCommand(host, command);
    }

    /**
     * Defines virtual domain. It rereads the config from XML, but does not
     * start the domain like "create" would.
     */
    public static boolean define(final Host host, final String config, final String options) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@CONFIG@", config);
        replaceHash.put("@OPTIONS@", options);
        final String command = host.getDistCommand("VIRSH.Define", replaceHash);
        return execCommand(host, command);
    }

    /** Returns command that defines virtual domain. */
    public static String getDefineCommand(final Host host, final String config, final String options) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@CONFIG@", config);
        replaceHash.put("@OPTIONS@", options);
        return host.getDistCommand("VIRSH.Define",
                                   replaceHash);
    }

    /** Undefines virtual domain. It removes the config. */
    public static boolean undefine(final Host host, final String domain, final String options) {
        final Map<String, String> replaceHash = new HashMap<String, String>();
        replaceHash.put("@DOMAIN@", domain);
        replaceHash.put("@OPTIONS@", options);
        final String command = host.getDistCommand("VIRSH.Undefine",
                                                   replaceHash);
        return execCommand(host, command);
    }

    /** Private constructor, cannot be instantiated. */
    private VIRSH() {
        /* Cannot be instantiated. */
    }
}
