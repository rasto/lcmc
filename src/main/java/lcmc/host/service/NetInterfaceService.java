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
import com.google.common.eventbus.Subscribe;
import lcmc.ClusterEventBus;
import lcmc.common.domain.Value;
import lcmc.drbd.domain.NetInterface;
import lcmc.event.BridgesChangedEvent;
import lcmc.event.NetInterfacesChangedEvent;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostNetworks;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Named
@Singleton
public class NetInterfaceService {
    @Inject
    private ClusterEventBus eventBus;
    private Map<Host, HostNetworks> hostNetInterfacesByHost = new ConcurrentHashMap<Host, HostNetworks>();

    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onNetInterfacesChanged(final NetInterfacesChangedEvent event) {
        final HostNetworks hostNetworks = new HostNetworks();
        hostNetworks.setNetworkIntefaces(event.getNetInterfaces());
        hostNetInterfacesByHost.put(event.getHost(), hostNetworks);
    }

    @Subscribe
    public void onBridgesChanged(final BridgesChangedEvent event) {
        final HostNetworks hostNetworks = hostNetInterfacesByHost.get(event.getHost());
        if (hostNetworks != null) {
            hostNetworks.setBridges(event.getBridges());
        }

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
}
