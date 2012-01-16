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


package lcmc.data.resources;

import lcmc.utilities.Tools;

/**
 * This class holds data of one cluster network.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Network extends Resource {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** List of all ips in the network. */
    private final String[] ips;
    /** Net mask. */
    private final String netMask;

    /**
     * Prepares a new <code>Network</code> object.
     *
     * @param name
     *          ip with *
     * @param ips
     *          ips that are in the network
     */
    public Network(final String name,
                   final String[] ips,
                   final String netMask) {
        super(name);
        this.ips = ips;
        this.netMask = netMask;
    }

    /** Returns list of ips delimited with comma. */
    public String getIps() {
        return Tools.join(", ", ips);
    }

    /** Returns net mask of this network. */
    public String getNetMask() {
        return netMask;
    }

    /** Returns value of the parameter. */
    @Override
    public String getValue(final String parameter) {
        Tools.appError("Wrong call to getValue");
        return "???";
    }
}
