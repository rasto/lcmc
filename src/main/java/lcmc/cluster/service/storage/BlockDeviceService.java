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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;

import lcmc.ClusterEventBus;
import lcmc.HwEventBus;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.ui.resource.ClusterViewFactory;
import lcmc.cluster.ui.resource.CommonBlockDevInfo;
import lcmc.common.domain.util.Tools;
import lcmc.drbd.domain.BlockDevice;
import lcmc.event.BlockDevicesChangedEvent;
import lcmc.event.CommonBlockDevicesChangedEvent;
import lcmc.event.HwBlockDevicesChangedEvent;
import lcmc.event.HwBlockDevicesDiskSpaceEvent;
import lcmc.event.HwDrbdStatusChangedEvent;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostBlockDevices;

@Named
@Singleton
public class BlockDeviceService {
    @Inject
    private HwEventBus hwEventBus;
    @Inject
    private ClusterEventBus clusterEventBus;
    private final Map<Host, HostBlockDevices> hostBlockDevicesByHost = new ConcurrentHashMap<>();
    private final Map<Cluster, List<String>> commonBlockDevicesByCluster = new ConcurrentHashMap<>();
    @Inject
    private ClusterViewFactory clusterViewFactory;

    private Collection<CommonBlockDevInfo> commonBlockDevViews;

    public void init() {
        hwEventBus.register(this);
    }

    @Subscribe
    public void blockDevicesChanged(final HwBlockDevicesChangedEvent event) {
        final HostBlockDevices hostBlockDevices = new HostBlockDevices();
        hostBlockDevices.setBlockDevices(event.getBlockDevices());
        hostBlockDevicesByHost.put(event.getHost(), hostBlockDevices);
        updateCommonBlockDeviceNames(Optional.ofNullable(event.getHost().getCluster()));
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
        return Optional.empty();
    }

    public Collection<CommonBlockDevInfo> getCommonBlockDevViews() {
        return ImmutableList.copyOf(commonBlockDevViews);
    }

    private void updateCommonBlockDeviceNames(final Optional<Cluster> cluster) {
        if (cluster.isEmpty()) {
            return;
        }
        final List<String> commonBlockDeviceNames = getCommonBlockDeviceNames(cluster.get().getHosts());
        final List<String> oldCommonBlockDeviceNames = commonBlockDevicesByCluster.get(cluster.get());
        commonBlockDevicesByCluster.put(cluster.get(), commonBlockDeviceNames);

        if (oldCommonBlockDeviceNames == null
                || oldCommonBlockDeviceNames.isEmpty()
                || !Tools.equalCollections(commonBlockDeviceNames, oldCommonBlockDeviceNames)) {
            final Collection<CommonBlockDevInfo> newCommonBlockDevViews =
                    createCommonBlockDevViews(cluster.get(), commonBlockDeviceNames);
            commonBlockDevViews = newCommonBlockDevViews;
            clusterEventBus.post(new CommonBlockDevicesChangedEvent(cluster.get(), newCommonBlockDevViews));
        }
    }

    private List<String> getCommonBlockDeviceNames(final Set<Host> hosts) {
        Optional<List<String>> namesIntersection = Optional.empty();
        for (final Host host : hosts) {
            final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(host);
            if (hostBlockDevices != null) {
                namesIntersection = hostBlockDevices.getBlockDevicesNamesIntersection(namesIntersection);
            }
        }
        return namesIntersection.orElse(new ArrayList<>());
    }


    private Collection<CommonBlockDevInfo> createCommonBlockDevViews(
            final Cluster cluster,
            final List<String> commonBlockDevicesNames) {
        final List<CommonBlockDevInfo> newCommonBlockDevViews = new ArrayList<>();
        for (final String commonBlockDevice : commonBlockDevicesNames) {
            final CommonBlockDevInfo commonBlockDevInfo =
                    clusterViewFactory.createCommonBlockDevView(cluster, commonBlockDevice);
            newCommonBlockDevViews.add(commonBlockDevInfo);
        }
        return newCommonBlockDevViews;
    }
}
