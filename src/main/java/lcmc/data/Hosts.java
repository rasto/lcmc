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


package lcmc.data;

import java.util.Set;
import java.util.LinkedHashSet;

/**
 * This class holds a set of all hosts.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Hosts {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Hosts set. */
    private final Set<Host> hosts = new LinkedHashSet<Host>();

    /**
     * adds new host to the hosts.
     *          host, that will be added to the hosts.
     */
    void addHost(final Host host) {
        hosts.add(host);
    }

    /** Returns number of all hosts. */
    int size() {
        return hosts.size();
    }

    /** Removes host from the hosts. */
    void removeHost(final Host host) {
        hosts.remove(host);
    }

    /** Returns true if host is in the hosts or false if it is not. */
    boolean existsHost(final Host host) {
        return hosts.contains(host);
    }

    /** Gets the host set. */
    public Set<Host> getHostSet() {
        return hosts;
    }

    /** Gets an array of all hosts. */
    public Host[] getHostsArray() {
        return hosts.toArray(new Host [hosts.size()]);
    }

    /** Disconnects all hosts. This is called after application closes. */
    void disconnectAllHosts() {
        for (final Host host : hosts) {
            host.disconnect();
        }
    }

    /** Removes references to the cluster from all hosts. */
    public void removeHostsFromCluster(final Cluster cluster) {
        for (final Host host : hosts) {
            if (host.getCluster() == cluster) {
                host.setCluster(null);
            }
        }
    }
}
