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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.eventbus.Subscribe;

import lcmc.ClusterEventBus;
import lcmc.HwEventBus;
import lcmc.cluster.domain.Cluster;
import lcmc.common.domain.util.Tools;
import lcmc.event.CommonFileSystemsChangedEvent;
import lcmc.event.FileSystemsChangedEvent;
import lcmc.event.HwFileSystemsChangedEvent;
import lcmc.host.domain.Host;

@Named
@Singleton
public class FileSystemService {
    private final HwEventBus hwEventBus;
    private final ClusterEventBus clusterEventBus;
    private final Map<Host, Set<String>> fileSystemsByHost = new ConcurrentHashMap<>();
    private final Map<Cluster, Set<String>> commonFileSystemsByCluster = new ConcurrentHashMap<>();

    public FileSystemService(HwEventBus hwEventBus, ClusterEventBus clusterEventBus) {
        this.hwEventBus = hwEventBus;
        this.clusterEventBus = clusterEventBus;
    }

    public void init() {
        hwEventBus.register(this);
    }

    @Subscribe
    public void onFileSystemsChanged(final HwFileSystemsChangedEvent event) {
        fileSystemsByHost.put(event.getHost(), event.getFileSystems());
        clusterEventBus.post(new FileSystemsChangedEvent(event.getHost(), event.getFileSystems()));
        updateCommonFileSystems(Optional.ofNullable(event.getHost().getCluster()));
    }

    public Set<String> getCommonFileSystems(final Cluster cluster) {
        final Set<String> fileSystems = commonFileSystemsByCluster.get(cluster);
        if (fileSystems == null) {
            return new TreeSet<>();
        }
        return fileSystems;
    }

    public Set<String> getFileSystems(final Host host) {
        return fileSystemsByHost.get(host);
    }

    private Set<String> getCommonFileSystems(final Collection<Host> hosts) {
        Optional<Set<String>> fileSystemsIntersection = Optional.empty();

        for (final Host host : hosts) {
            final Set<String> fileSystems = fileSystemsByHost.get(host);
            fileSystemsIntersection = Tools.getIntersection(Optional.ofNullable(fileSystems), fileSystemsIntersection);
        }
        return fileSystemsIntersection.orElse(new TreeSet<>());
    }

    private void updateCommonFileSystems(final Optional<Cluster> cluster) {
        if (cluster.isEmpty()) {
            return;
        }
        final Set<String> commonFileSystems = getCommonFileSystems(cluster.get().getHosts());
        final Set<String> oldCommonFileSystems = commonFileSystemsByCluster.get(cluster.get());
        commonFileSystemsByCluster.put(cluster.get(), commonFileSystems);
        if (oldCommonFileSystems == null
                || oldCommonFileSystems.isEmpty()
                || !Tools.equalCollections(commonFileSystems, oldCommonFileSystems)) {
            clusterEventBus.post(new CommonFileSystemsChangedEvent(cluster.get(), commonFileSystems));
        }
    }
}
