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
 * This class holds data for multicast and broadcast addresses for heartbeat
 * communication.
 */
public final class CastAddress {
    /** Type of the cast address,  bcast, mcast or ucast */
    private final String type;
    /** Interface like eth0. */
    private final String iface;
    /** An IP address. */
    private final String ipAddress;
    /** Serial device. */
    private final String serialDevice;

    public CastAddress(final String type, final String iface, final String ipAddress, final String serialDevice) {
        this.type       = type;
        this.iface      = iface;
        this.ipAddress = ipAddress;
        this.serialDevice = serialDevice;
    }

    /** Convert the info to the line as it appears in the ha.cf. */
    private String convert(final String type, final String iface, final String address, final String serial) {
        if ("mcast".equals(type) || "ucast".equals(type)) {

            return type + ' ' + iface + ' ' + address;
        }
        if ("bcast".equals(type)) {
            return type + ' ' + iface;
        }
        if ("serial".equals(type)) {
            return type + ' ' + serial;
        }
        return "";

    }

    /**
     * Convert the info of this object to the line as it appears in the ha.cf.
     */
    public String getConfigString() {
        return convert(type, iface, ipAddress, serialDevice);
    }

    public boolean equals(final String thatType,
                          final String thatIface,
                          final String thatIpAddress,
                          final String thatSerialDevice) {
        return getConfigString().equals(convert(thatType, thatIface, thatIpAddress, thatSerialDevice));
    }
}
