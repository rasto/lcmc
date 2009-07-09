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


package drbd.data.resources;

import drbd.utilities.Tools;

/**
 * This class holds data of one network interface.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class NetInterface extends Resource {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Ip address. */
    private String ip = null;
    /** Mac address. */
    private String macAddr = null;
    /** Net mask. */
    private String netMask = null;

    /**
     * Prepares a new <code>NetInterface</code> object.
     *
     * @param line
     *          line with interface, ip, mac addr and net mask  delimited
     *          with space
     */
    public NetInterface(final String line) {
        super();
        final String[] cols = line.split(" ");
        if (cols.length < 4) {
            Tools.appError("cannot parse: " + line);
        }
        String iface = "unknown";
        if (cols.length > 0) {
            iface = cols[0];
        }
        if (cols.length > 1) {
            this.ip = cols[1];
        }
        if (cols.length > 2) {
            this.macAddr = cols[2];
        }
        if (cols.length > 3) {
            this.netMask = cols[3];
        }
        setName(iface);
    }

    /**
     * Creates a new <code>NetInterface</code> object.
     *
     * @param iface
     *          network interface
     * @param ip
     *          ip address
     * @param macAddr
     *          mac address
     * @param netMask
     *          network mask
     */
    public NetInterface(final String iface,
                        final String ip,
                        final String macAddr,
                        final String netMask) {
        super(iface);
        this.ip      = ip;
        this.macAddr = macAddr;
        this.netMask = netMask;
    }

    /**
     * Returns mac address.
     */
    public final String getMacAddr() {
        return macAddr;
    }

    /**
     * Returns ip.
     */
    public final String getIp() {
        return ip;
    }

    /**
     * Returns network mask.
     */
    public final String getNetMask() {
        return netMask;
    }

    /**
     * Returns network ip. The ip has '*' instead of bytes, that are
     * not part of the network. e.g. 192.168.1.1 and mask 255.255.255.0 gives
     * 192.168.1.*
     */
    public final String getNetworkIp() {
        final String[] ipParts = ip.split("\\.");
        if (netMask == null) {
            return null;
        }
        final String[] netMaskParts = netMask.split("\\.");
        String[] networkIpParts = new String[4];
        if (ipParts.length != 4 && netMaskParts.length != 4) {
            return "";
        }
        for (int i = 0; i < 4; i++) {
            if (netMaskParts[i].equals("255")) {
                networkIpParts[i] = ipParts[i];
            } else {
                networkIpParts[i] = "*";
            }
        }
        return Tools.join(".", networkIpParts);
    }

    /**
     * Returns value for parameter.
     */
    public final String getValue(final String parameter) {
        if ("ip".equals(parameter)) {
            return ip;
        }
        if ("String".equals(parameter)) {
            return ip;
        } else {
            Tools.appError("Unknown parameter: " + parameter, "");
            return "";
        }
    }

    /**
     * Returns bindnetaddr.
     */
    public final String getBindnetaddr() {
        final String[] ipParts = ip.split("\\.");
        if (netMask == null) {
            return null;
        }
        final String[] netMaskParts = netMask.split("\\.");
        String[] networkIpParts = new String[4];
        if (ipParts.length != 4 && netMaskParts.length != 4) {
            return "";
        }
        for (int i = 0; i < 4; i++) {
            networkIpParts[i] =
                         Integer.toString(Integer.parseInt(ipParts[i])
                                          & Integer.parseInt(netMaskParts[i]));
        }
        return Tools.join(".", networkIpParts);
    }
}
