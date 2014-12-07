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

package lcmc.cluster.service.storage;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import lcmc.ClusterEventBus;
import lcmc.HwEventBus;
import lcmc.cluster.domain.Cluster;
import lcmc.common.domain.util.Tools;
import lcmc.drbd.domain.BlockDevice;
import lcmc.event.BlockDevicesChangedEvent;
import lcmc.event.CommonBlockDevicesChangedEvent;
import lcmc.event.CommonMountPointsEvent;
import lcmc.event.HwBlockDevicesChangedEvent;
import lcmc.event.HwBlockDevicesDiskSpaceEvent;
import lcmc.event.HwDrbdStatusChangedEvent;
import lcmc.event.HwMountPointsChangedEvent;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostBlockDevices;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@Named
@Singleton
public class BlockDeviceService {
    @Inject
    private HwEventBus hwEventBus;
    @Inject
    private ClusterEventBus clusterEventBus;
    private Map<Host, HostBlockDevices> hostBlockDevicesByHost = new ConcurrentHashMap<Host, HostBlockDevices>();
    private Map<Cluster, List<String>> commonBlockDevicesByCluster = new ConcurrentHashMap<Cluster, List<String>>();
    private Map<Host, Set<String>> mountPointsByHost = new ConcurrentHashMap<Host, Set<String>>();
    private Map<Cluster, Set<String>> commonMountPointsByCluster = new ConcurrentHashMap<Cluster, Set<String>>();

    public void init() {
        hwEventBus.register(this);
    }

    @Subscribe
    public void blockDevicesChanged(final HwBlockDevicesChangedEvent event) {
        final HostBlockDevices hostBlockDevices = new HostBlockDevices();
        hostBlockDevices.setBlockDevices(event.getBlockDevices());
        hostBlockDevicesByHost.put(event.getHost(), hostBlockDevices);
        updateCommonBlockDeviceNames(Optional.fromNullable(event.getHost().getCluster()));
        clusterEventBus.post(new BlockDevicesChangedEvent(event.getHost(), hostBlockDevices.getBlockDevices()));
    }

    @Subscribe
    public void drbdStatusChanged(final HwDrbdStatusChangedEvent event) {
        final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(event.getHost());
        if (hostBlockDevices != null) {
            hostBlockDevices.resetDrbdOnBlockDevices(event.isDrbdStatusOk());
            clusterEventBus.post(new BlockDevicesChangedEvent(event.getHost(), hostBlockDevices.getBlockDevices()));
        }
    }

    @Subscribe
    public void blockDevicesDiskSpaceEvent(final HwBlockDevicesDiskSpaceEvent event) {
        final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(event.getHost());
        if (hostBlockDevices != null) {
            hostBlockDevices.setDiskSpace(event.getDiskSpaces());
            clusterEventBus.post(new BlockDevicesChangedEvent(event.getHost(), hostBlockDevices.getBlockDevices()));
        }
    }

    @Subscribe
    public void mountPointsChanged(final HwMountPointsChangedEvent event) {
        mountPointsByHost.put(event.getHost(), event.getMountPoints());
        updateCommonMountPoints(Optional.fromNullable(event.getHost().getCluster()));
    }

    public Set<String> getCommonMountPoints(final Cluster cluster) {
        final Set<String> mountPoints = commonMountPointsByCluster.get(cluster);
        if (mountPoints == null) {
            return new TreeSet<String>();
        }
        return mountPoints;
    }

    private Set<String> getCommonMountPoints(final Collection<Host> hosts) {
        Optional<Set<String>> mountPointsIntersection = Optional.absent();

        for (final Host host : hosts) {
            final Set<String> mountPoints = mountPointsByHost.get(host);
            mountPointsIntersection = Tools.getIntersection(
                    Optional.fromNullable(mountPoints),
                    mountPointsIntersection);
        }
        return mountPointsIntersection.or(new TreeSet<String>());
    }

    private void updateCommonMountPoints(final Optional<Cluster> cluster) {
        if (!cluster.isPresent()) {
            return;
        }
        final Set<String> commonMountPoints = getCommonMountPoints(cluster.get().getHosts());
        final Set<String> oldCommonMountPoints = commonMountPointsByCluster.get(cluster.get());
        commonMountPointsByCluster.put(cluster.get(), commonMountPoints);
        if (oldCommonMountPoints == null
                || oldCommonMountPoints.isEmpty()
                || !equalCollections(commonMountPoints, oldCommonMountPoints)) {
            clusterEventBus.post(new CommonMountPointsEvent(cluster.get(), commonMountPoints));
        }
    }


    public Collection<BlockDevice> getBlockDevices(final Host host) {
        final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(host);
        if (hostBlockDevices == null) {
            return Collections.emptyList();
        } else {
            return hostBlockDevices.getBlockDevices();
        }
    }

    public Optional<BlockDevice> getBlockDeviceByName(final Host host, final String name) {
        final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(host);
        if (hostBlockDevices != null) {
            return hostBlockDevices.getBlockDeviceByName(name);
        }
        return Optional.absent();
    }

    public List<String> getCommonBlockDeviceNames(final Set<Host> hosts) {
        Optional<List<String>> namesIntersection = Optional.absent();
        for (final Host host : hosts) {
            final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(host);
            if (hostBlockDevices != null) {
                namesIntersection = hostBlockDevices.getBlockDevicesNamesIntersection(namesIntersection);
            }
        }
        return namesIntersection.or(new ArrayList<String>());
    }

    private void updateCommonBlockDeviceNames(final Optional<Cluster> cluster) {
        if (!cluster.isPresent()) {
            return;
        }
        final List<String> commonBlockDeviceNames = getCommonBlockDeviceNames(cluster.get().getHosts());
        final List<String> oldCommonBlockDeviceNames = commonBlockDevicesByCluster.get(cluster.get());
        commonBlockDevicesByCluster.put(cluster.get(), commonBlockDeviceNames);
        if (oldCommonBlockDeviceNames == null
                || oldCommonBlockDeviceNames.isEmpty()
                || !equalCollections(commonBlockDeviceNames, oldCommonBlockDeviceNames)) {
            clusterEventBus.post(new CommonBlockDevicesChangedEvent(cluster.get(), commonBlockDeviceNames));
        }
    }

    public boolean equalCollections(final Collection<?> collection1, final Collection<?> collection2) {
        if (collection1.size() != collection2.size()) {
            return false;
        }
        final Iterator<?> iterator1 = collection1.iterator();
        final Iterator<?> iterator2 = collection2.iterator();
        while (iterator1.hasNext()) {
            if (!iterator1.next().equals(iterator2.next())) {
                return false;
            }
        }
        return true;
    }
}
