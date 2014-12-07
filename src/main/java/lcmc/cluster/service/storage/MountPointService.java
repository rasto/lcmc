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
import lcmc.event.CommonMountPointsEvent;
import lcmc.event.HwMountPointsChangedEvent;
import lcmc.host.domain.Host;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@Named
@Singleton
public class MountPointService {
    @Inject
    private HwEventBus hwEventBus;
    @Inject
    private ClusterEventBus clusterEventBus;
    private Map<Host, Set<String>> mountPointsByHost = new ConcurrentHashMap<Host, Set<String>>();
    private Map<Cluster, Set<String>> commonMountPointsByCluster = new ConcurrentHashMap<Cluster, Set<String>>();

    public void init() {
        hwEventBus.register(this);
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
                || !Tools.equalCollections(commonMountPoints, oldCommonMountPoints)) {
            clusterEventBus.post(new CommonMountPointsEvent(cluster.get(), commonMountPoints));
        }
    }
}
