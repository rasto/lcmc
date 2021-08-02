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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lcmc.ClusterEventBus;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.domain.Network;
import lcmc.cluster.domain.NetworkService;
import lcmc.cluster.ui.ClusterBrowser;

@ExtendWith(MockitoExtension.class)
class NetworkPresenterTest {
    private static final String ANY_NAME = "ANY_NAME";
    private static final String[] ANY_ALL_IPS = null;
    private static final Integer ANY_CIDR = null;
    private NetworkPresenter presenter;
    private final Network network = new Network(ANY_NAME, ANY_ALL_IPS, ANY_CIDR);
    @Mock
    private NetworkModel model;
    @Mock
    private NetworkView view;
    @Mock
    private NetworkService networkService;
    @Mock
    private ClusterBrowser clusterBrowser;

    private final ClusterEventBus clusterEventBus = new ClusterEventBus();

    final Cluster cluster = new Cluster(null, null, null);

    @BeforeEach
    void setupNetworkPresenter() {
        presenter = new NetworkPresenter(network, model, view, clusterEventBus, cluster, networkService, clusterBrowser);

    }

    @Test
    void viewShouldBeUpdated() {
        final Optional<Network> newNetwork = Optional.of(network);
        when(networkService.getCommonNetwork(cluster, network)).thenReturn(newNetwork);

        presenter.updateNetwork();

        verify(view).update();
    }
}