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
import java.util.Map;
import java.util.HashMap;

/**
 * This class provides virsh commands.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class VIRSH {
    /** Virsh command map */
    private static Map<String, String> COMMANDS =
                                                 new HashMap<String, String>();
    static {
        COMMANDS.put("vcpu", "/usr/bin/virsh setvcpus @DOMAIN@ @VALUE@");
        COMMANDS.put("memory", "/usr/bin/virsh setmaxmem @DOMAIN@ @VALUE@"
                               + " && /usr/bin/virsh setmem @DOMAIN@ @VALUE@");
        COMMANDS.put("autostart", "/usr/bin/virsh autostart @VALUE@ @DOMAIN@");
    }
    /**
     * Private constructor, cannot be instantiated.
     */
    private VIRSH() {
        /* Cannot be instantiated. */
    }

    /**
     * Executes the specified virsh commands on the specified hosts
     */
    private static void execCommand(final Host[] hosts,
                                      final String commands,
                                      final boolean outputVisible) {

        for (final Host host : hosts) {
            if (host.isConnected()) {
                Tools.execCommandProgressIndicator(
                                     host,
                                     commands,
                                     null,
                                     outputVisible,
                                     Tools.getString("VIRSH.ExecutingCommand")
                                     + " " + commands + "...");
            }
        }
    }

    /**
     * Sets paramters with virsh command.
     */
    public static void setParameters(final Host[] hosts,
                                     final String domainName,
                                     final Map<String, String> parameters) {
        final StringBuffer commands = new StringBuffer(100);
        for (final String param : parameters.keySet()) {
            String command = COMMANDS.get(param); 
            if (command == null) {
                continue;
            }
            if (command.indexOf("@DOMAIN@") >= 0) {
                command = command.replaceAll("@DOMAIN@", domainName);
            }
            if (command.indexOf("@VALUE@") >= 0) {
                String value = parameters.get(param);
                if ("autostart".equals(param)) {
                    if ("true".equalsIgnoreCase(value)) {
                        value = "";
                    } else {
                        value = "--disable";
                    }
                }
                command = command.replaceAll("@VALUE@", value);
            }
            if (commands.length() > 0) {
                commands.append(" && ");
            }
            commands.append(command);
        }
        execCommand(hosts, commands.toString(), true);
    }
}
