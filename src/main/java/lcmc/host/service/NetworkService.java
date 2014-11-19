/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.host.service;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import lcmc.ClusterEventBus;
import lcmc.HwEventBus;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.domain.Network;
import lcmc.common.domain.Value;
import lcmc.drbd.domain.NetInterface;
import lcmc.event.HwBridgesChangedEvent;
import lcmc.event.HwNetInterfacesChangedEvent;
import lcmc.event.NetInterfacesChangedEvent;
import lcmc.event.NetworkChangedEvent;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostNetworks;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Named
@Singleton
public class NetworkService {
    @Inject
    private HwEventBus hwEventBus;
    @Inject
    private ClusterEventBus clusterEventBus;
    private Map<Host, HostNetworks> hostNetInterfacesByHost = new ConcurrentHashMap<Host, HostNetworks>();
    private Map<Cluster, List<Network>> networksByCluster = new ConcurrentHashMap<Cluster, List<Network>>();

    public void init() {
        hwEventBus.register(this);
        clusterEventBus.register(this);
    }

    @Subscribe
    public void onNetInterfacesChanged(final HwNetInterfacesChangedEvent event) {
        final HostNetworks hostNetworks = new HostNetworks();
        hostNetworks.setNetworkIntefaces(event.getNetInterfaces());
        hostNetInterfacesByHost.put(event.getHost(), hostNetworks);
        updateCommonNetworks(Optional.fromNullable(event.getHost().getCluster()));
        clusterEventBus.post(new NetInterfacesChangedEvent(event.getHost(), hostNetworks.getNetInterfaces()));
    }

    @Subscribe
    public void onBridgesChanged(final HwBridgesChangedEvent event) {
        final HostNetworks hostNetworks = hostNetInterfacesByHost.get(event.getHost());
        if (hostNetworks != null) {
            hostNetworks.setBridges(event.getBridges());
        }
        clusterEventBus.post(new NetInterfacesChangedEvent(event.getHost(), hostNetworks.getNetInterfaces()));
    }

    public Collection<Value> getBridges(final Host host) {
        final HostNetworks hostNetworks = hostNetInterfacesByHost.get(host);
        if (hostNetworks == null) {
            return Collections.emptyList();
        } else {
            return hostNetworks.getBridges();
        }
    }

    public Map<String,Integer> getNetworksIntersection(final Collection<Host> hosts) {
        Optional<Map<String, Integer>> networksIntersection = Optional.absent();

        for (final Host host : hosts) {
            final HostNetworks hostNetworks = hostNetInterfacesByHost.get(host);
            if (hostNetworks != null) {
                networksIntersection = hostNetworks.getNetworksIntersection(networksIntersection);
            }
        }
        return networksIntersection.or(new HashMap<String, Integer>());
    }

    public NetInterface[] getNetInterfacesWithBridges(final Host host) {
        final HostNetworks hostNetworks = hostNetInterfacesByHost.get(host);
        if (hostNetworks == null) {
            return new NetInterface[]{};
        } else {
            return hostNetworks.getNetInterfacesWithBridges();
        }
    }

    public Collection<String> getIpsFromNetwork(final Host host, final String netIp) {
        final HostNetworks hostNetworks = hostNetInterfacesByHost.get(host);
        if (hostNetworks == null) {
            return new ArrayList<String>();
        } else {
            return hostNetworks.getIpsFromNetwork(netIp);
        }
    }

    public List<Network> getCommonNetworks(final Cluster cluster) {
        return ImmutableList.copyOf(networksByCluster.get(cluster));
    }

    public Optional<Network> getCommonNetwork(final Cluster cluster, final Network network) {
        final List<Network> networks = getCommonNetworks(cluster);
        final int index = networks.indexOf(network);
        if (index >= 0) {
            return Optional.of(networks.get(index));
        }
        return Optional.absent();
    }


    private List<Network> getCommonNetworks(final Set<Host> hosts) {
        final Map<String, Integer> networksIntersection = getNetworksIntersection(hosts);

        final List<Network> commonNetworks = new ArrayList<Network>();
        for (final Map.Entry<String, Integer> stringIntegerEntry : networksIntersection.entrySet()) {
            final List<String> ips = new ArrayList<String>();
            for (final Host host : hosts) {
                ips.addAll(getIpsFromNetwork(host, stringIntegerEntry.getKey()));
            }
            final Integer cidr = stringIntegerEntry.getValue();
            final Network network = new Network(stringIntegerEntry.getKey(), ips.toArray(new String[ips.size()]), cidr);
            commonNetworks.add(network);
        }

        return commonNetworks;
    }

    private void updateCommonNetworks(final Optional<Cluster> cluster) {
        if (!cluster.isPresent()) {
            return;
        }
        final List<Network> commonNetworks = getCommonNetworks(cluster.get().getHosts());
        final List<Network> oldCommonNetworks = networksByCluster.get(cluster.get());
        networksByCluster.put(cluster.get(), commonNetworks);
        if (oldCommonNetworks == null
                || oldCommonNetworks.isEmpty()
                || !equalCollections(commonNetworks, oldCommonNetworks)) {
            clusterEventBus.post(new NetworkChangedEvent(cluster.get(), commonNetworks));
        }
    }

    private boolean equalCollections(final List<Network> collection1, final List<Network> collection2) {
        if (collection1.size() != collection2.size()) {
            return false;
        }
        for (int i = 0; i < collection1.size(); i++) {
            if (!collection1.get(i).equals(collection2.get(i))) {
                return false;
            }
        }
        return true;
    }
}
