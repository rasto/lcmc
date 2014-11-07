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
import lcmc.HwEventBus;
import lcmc.drbd.domain.BlockDevice;
import lcmc.event.BlockDevicesChangedEvent;
import lcmc.event.HwBlockDevicesChangedEvent;
import lcmc.event.HwBlockDevicesDiskSpaceEvent;
import lcmc.event.HwDrbdStatusChangedEvent;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostBlockDevices;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Named
@Singleton
public class BlockDeviceService {
    @Inject
    private HwEventBus hwEventBus;
    @Inject
    private ClusterEventBus clusterEventBus;
    private Map<Host, HostBlockDevices> hostBlockDevicesByHost = new ConcurrentHashMap<Host, HostBlockDevices>();

    public void init() {
        hwEventBus.register(this);
    }

    @Subscribe
    public void blockDevicesChanged(final HwBlockDevicesChangedEvent event) {
        final HostBlockDevices hostBlockDevices = new HostBlockDevices();
        hostBlockDevices.setBlockDevices(event.getBlockDevices());
        hostBlockDevicesByHost.put(event.getHost(), hostBlockDevices);
        clusterEventBus.post(new BlockDevicesChangedEvent(event.getHost(), hostBlockDevices.getBlockDevices()));
    }

    @Subscribe
    public void drbdStatusChanged(final HwDrbdStatusChangedEvent event) {
        final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(event.getHost());
        if (hostBlockDevices != null) {
            hostBlockDevices.resetDrbdOnBlockDevices(event.isDrbdStatusOk());
        }
        clusterEventBus.post(new BlockDevicesChangedEvent(event.getHost(), hostBlockDevices.getBlockDevices()));
    }

    @Subscribe
    public void blockDevicesDiskSpaceEvent(final HwBlockDevicesDiskSpaceEvent event) {
        final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(event.getHost());
        if (hostBlockDevices != null) {
            hostBlockDevices.setDiskSpace(event.getDiskSpaces());
        }
        clusterEventBus.post(new BlockDevicesChangedEvent(event.getHost(), hostBlockDevices.getBlockDevices()));
    }

    public Collection<BlockDevice> getBlockDevices(final Host host) {
        final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(host);
        if (hostBlockDevices == null) {
            return Collections.emptyList();
        } else {
            return hostBlockDevices.getBlockDevices();
        }
    }

    public Collection<String> getCommonBlockDeviceNames(final Iterable<Host> hosts) {
        Optional<Collection<String>> namesIntersection = Optional.absent();
        for (final Host host : hosts) {
            final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(host);
            if (hostBlockDevices != null) {
                namesIntersection = hostBlockDevices.getBlockDevicesNamesIntersection(namesIntersection);
            }
        }
        return namesIntersection.or(new ArrayList<String>());
    }

    public Optional<BlockDevice> getBlockDeviceByName(final Host host, final String name) {
        final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(host);
        if (hostBlockDevices != null) {
            return hostBlockDevices.getBlockDeviceByName(name);
        }
        return Optional.absent();
    }
}
