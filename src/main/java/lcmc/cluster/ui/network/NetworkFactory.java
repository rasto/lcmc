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

package lcmc.cluster.ui.network;

import lcmc.ClusterEventBus;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.domain.Network;
import lcmc.cluster.service.NetworkService;
import lcmc.cluster.ui.ClusterBrowser;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;

@RequiredArgsConstructor
public class NetworkFactory {
    private final ClusterEventBus clusterEventBus;
    private final NetworkService networkService;

    public NetworkPresenter createPresenter(final Cluster cluster, final Network network, ClusterBrowser clusterBrowser) {
        final NetworkModel model = new NetworkModel();
        final NetworkView view = new NetworkView(model);
        return new NetworkPresenter(
                network,
                model,
                view,
                clusterEventBus,
                cluster,
                networkService,
                clusterBrowser);
    }
}
