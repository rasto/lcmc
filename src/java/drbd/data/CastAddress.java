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


package drbd.data;

/**
 * This class holds data for multicast and broadcast addresses for heartbeat
 * communication.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class CastAddress {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Type of the cast address. */
    private final String type; /* bcast, mcast or ucast */
    /** Interface like eth0. */
    private final String iface;
    /** An IP address. */
    private final String address;
    /** Serial device. */
    private final String serial;

    /** Prepares a new <code>CastAddress</code> object. */
    public CastAddress(final String type,
                       final String iface,
                       final String address,
                       final String serial) {
        this.type       = type;
        this.iface      = iface;
        this.address    = address;
        this.serial     = serial;
    }

    /** Convert the info to the line as it appears in the ha.cf. */
    private String convert(final String type,
                           final String iface,
                           final String address,
                           final String serial) {
        if ("mcast".equals(type) || "ucast".equals(type)) {
            return type + " " + iface + " " + address;
        } else if ("bcast".equals(type)) {
            return type + " " + iface;
        } else if ("serial".equals(type)) {
            return type + " " + serial;
        }
        return "";

    }

    /**
     * Convert the info of this object to the line as it appears in the ha.cf.
     */
    public String getConfigString() {
        return convert(type, iface, address, serial);
    }

    /** Compares two cast addresses if they are the same. */
    public boolean equals(final String t,
                          final String i,
                          final String a,
                          final String s) {
        return getConfigString().equals(convert(t, i, a, s));
    }
}
