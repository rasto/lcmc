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
import lcmc.drbd.domain.BlockDevice;
import lcmc.event.BlockDevicesChangedEvent;
import lcmc.event.BlockDevicesDiskSpaceEvent;
import lcmc.event.DrbdStatusChangedEvent;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostBlockDevices;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Named
@Singleton
public class BlockDeviceService {
    @Inject
    private ClusterEventBus eventBus;
    private Map<Host, HostBlockDevices> hostBlockDevicesByHost = new ConcurrentHashMap<Host, HostBlockDevices>();

    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void blockDevicesChanged(final BlockDevicesChangedEvent event) {
        final HostBlockDevices hostBlockDevices = new HostBlockDevices();
        hostBlockDevices.setBlockDevices(event.getBlockDevices());
        hostBlockDevicesByHost.put(event.getHost(), hostBlockDevices);
    }

    @Subscribe
    public void drbdStatusChanged(final DrbdStatusChangedEvent event) {
        final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(event.getHost());
        if (hostBlockDevices != null) {
            hostBlockDevices.resetDrbdOnBlockDevices(event.isDrbdStatusOk());
        }
    }

    @Subscribe
    public void blockDevicesDiskSpaceEvent(final BlockDevicesDiskSpaceEvent event) {
        final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(event.getHost());
        if (hostBlockDevices != null) {
            hostBlockDevices.setDiskSpace(event.getDiskSpaces());
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

    public List<String> getCommonBlockDeviceNames(final Iterable<Host> hosts) {
        List<String> namesIntersection = new ArrayList<String>();
        for (final Host host : hosts) {
            final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(host);
            if (hostBlockDevices != null) {
                namesIntersection = hostBlockDevices.getBlockDevicesNamesIntersection(namesIntersection);
            }
        }
        return namesIntersection;
    }

    public Optional<BlockDevice> getBlockDeviceByName(final Host host, final String name) {
        final HostBlockDevices hostBlockDevices = hostBlockDevicesByHost.get(host);
        if (hostBlockDevices != null) {
            return hostBlockDevices.getBlockDeviceByName(name);
        }
        return Optional.absent();
    }
}
