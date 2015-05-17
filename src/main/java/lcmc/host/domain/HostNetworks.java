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

package lcmc.host.domain;

import com.google.common.base.Optional;
import lcmc.common.domain.Value;
import lcmc.drbd.domain.NetInterface;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Named
public class HostNetworks {
    private List<NetInterface> netInterfacesWithBridges = new ArrayList<NetInterface>();
    private List<Value> bridges = new ArrayList<Value>();

    public NetInterface[] getNetInterfacesWithBridges() {
        return netInterfacesWithBridges.toArray(new NetInterface[netInterfacesWithBridges.size()]);
    }

    public List<NetInterface> getNetInterfaces() {
        return netInterfacesWithBridges;
    }

    public List<Value> getBridges() {
        return new ArrayList<Value>(bridges);
    }

    private Map<String, Integer> getNetworkIps() {
        final Map<String, Integer> networkIps = new LinkedHashMap<String, Integer>();
        for (final NetInterface ni : netInterfacesWithBridges) {
            final String netIp = ni.getNetworkIp();

            networkIps.put(netIp, ni.getCidr());
        }
        return networkIps;
    }

    /** Returns list of networks that exist on all hosts. */
    public Optional<Map<String, Integer>> getNetworksIntersection(final Optional<Map<String, Integer>> otherNetworkIps) {
        if (!otherNetworkIps.isPresent()) {
            return Optional.of(getNetworkIps());
        }
        final Map<String, Integer> networksIntersection = new LinkedHashMap<String, Integer>();
        for (final NetInterface netInterface : netInterfacesWithBridges) {
            if (netInterface.isLocalHost()) {
                continue;
            }
            final String networkIp = netInterface.getNetworkIp();
            if (otherNetworkIps.get().containsKey(networkIp) && !networksIntersection.containsKey(networkIp)) {
                networksIntersection.put(networkIp, netInterface.getCidr());
            }
        }
        return Optional.of(networksIntersection);
    }

    public List<String> getIpsFromNetwork(final String netIp) {
        final List<String> networkIps = new ArrayList<String>();
        for (final NetInterface ni : netInterfacesWithBridges) {
            if (netIp.equals(ni.getNetworkIp())) {
                networkIps.add(ni.getIp());
            }
        }
        return networkIps;
    }

    public void setNetworkIntefaces(final List<NetInterface> networkIntefaces) {
        this.netInterfacesWithBridges = networkIntefaces;
    }

    public void setBridges(List<Value> bridges) {
        this.bridges = bridges;
    }
}
