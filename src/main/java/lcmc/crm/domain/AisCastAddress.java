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

package lcmc.crm.domain;

/**
 * This class holds data for multicast and broadcast addresses for openais
 * communication.
 */
public final class AisCastAddress {
    /** Only mcast at the moment*/
    private final String type;
    /** Bind net address like 192.168.122.0. */
    private final String bindNetAddr;
    private final String multicastAddr;
    private final String port;

    public AisCastAddress(final String type,
                          final String bindNetAddr,
                          final String multicastAddr,
                          final String port) {
        this.type        = type;
        this.bindNetAddr = bindNetAddr;
        this.multicastAddr = multicastAddr;
        this.port        = port;
    }

    /**
     * Convert the info to the lines as they appear in the openais.conf without
     * the ringnumber so that two interfaces can be compared.
     */
    private String convert(final String tab, final String type,
                           final String bindnetaddr,
                           final String address,
                           final String port) {
        if ("mcast".equals(type)) {
            return tab + "interface {\n"
                 + tab + tab + "bindnetaddr: " + bindnetaddr + '\n'
                 + tab + tab + "mcastaddr: "   + address + '\n'
                 + tab + tab + "mcastport: "   + port + '\n'
                 + tab + '}';
        } else {
            return "";
        }
    }

    /**
     * Convert the info of this object to the line as it appears in the
     * openais.conf.
     */
    public String getConfigString(final int ringnumber, final String tab) {
        if ("mcast".equals(type)) {
            return tab + "interface {\n"
                 + tab + tab + "ringnumber: "  + Integer.toString(ringnumber)
                 + '\n'
                 + tab + tab + "bindnetaddr: " + bindNetAddr + '\n'
                 + tab + tab + "mcastaddr: "   + multicastAddr + '\n'
                 + tab + tab + "mcastport: "   + port + '\n'
                 + tab + '}';
        } else {
            return "";
        }
    }

    public boolean equals(final String tab,
                          final String thatType,
                          final String thatBindNetAddr,
                          final String thatMulticastAddr,
                          final String thatPort) {
        return convert(tab, type, bindNetAddr, multicastAddr, port).equals(
                                            convert(tab, thatType, thatBindNetAddr, thatMulticastAddr, thatPort));
    }
}
