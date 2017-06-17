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


package lcmc.host.domain;

import java.util.LinkedHashSet;
import java.util.Set;

import lcmc.cluster.domain.Cluster;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * This class holds a set of all hosts.
 */
@Named
@Singleton
public class Hosts {
    private static final Logger LOG = LoggerFactory.getLogger(Hosts.class);
    private final Set<Host> hosts = new LinkedHashSet<Host>();

    public void addHost(final Host host) {
        hosts.add(host);
    }

    public int size() {
        return hosts.size();
    }

    public void removeHost(final Host host) {
        hosts.remove(host);
    }

    public boolean isHostInHosts(final Host host) {
        return hosts.contains(host);
    }

    public Set<Host> getHostSet() {
        return hosts;
    }

    public Host[] getHostsArray() {
        return hosts.toArray(new Host [hosts.size()]);
    }

    public void disconnectAllHosts() {
        for (final Host host : hosts) {
            host.disconnect();
        }
    }

    public void removeHostsFromCluster(final Cluster cluster) {
        LOG.debug1("removeHostsFromCluster: cluster: " + cluster.getName());
        for (final Host host : hosts) {
            if (host.getCluster() == cluster) {
                host.removeFromCluster();
            }
        }
    }
}
