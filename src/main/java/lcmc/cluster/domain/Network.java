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


package lcmc.cluster.domain;

import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;

/**
 * This class holds data of one cluster network.
 */
public final class Network {
    private static final Logger LOG = LoggerFactory.getLogger(Network.class);
    private final String name;
    /** List of all ips in the network. */
    private final String[] allIPs;
    private final Integer cidr;

    public Network(final String name, final String[] allIPs, final Integer cidr) {
//        super(name);
        this.name = name;
        this.allIPs = allIPs;
        this.cidr = cidr;
    }

    public String getName() {
        return name;
    }

    public String getAllIPs() {
        return Tools.join(", ", allIPs);
    }

    public Integer getCidr() {
        return cidr;
    }

//    @Override
    public Value getValue(final String parameter) {
        LOG.appError("getValue: wrong call to getValue");
        return new StringValue("???");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Network network = (Network) o;

        if (!name.equals(network.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
