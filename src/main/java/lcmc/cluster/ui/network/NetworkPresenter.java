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

import java.util.Optional;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.google.common.eventbus.Subscribe;

import lcmc.ClusterEventBus;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.domain.Network;
import lcmc.cluster.service.NetworkService;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.event.NetInterfacesChangedEvent;

public class NetworkPresenter implements InfoPresenter {

    private final Network network;
    private final NetworkModel model;
    private final NetworkView view;
    private final Cluster cluster;
    private final ClusterEventBus clusterEventBus;
    private final NetworkService networkService;
    private final ClusterBrowser clusterBrowser;

    private static final ImageIcon NETWORK_ICON = Tools.createImageIcon(Tools.getDefault("ClusterBrowser.NetworkIcon"));

    public NetworkPresenter(final Network network, final NetworkModel model, final NetworkView view,
            final ClusterEventBus clusterEventBus, final Cluster cluster,
            final NetworkService networkService, ClusterBrowser clusterBrowser) {
        this.network = network;
        this.model = model;
        this.view = view;
        this.clusterEventBus = clusterEventBus;
        this.cluster = cluster;
        this.networkService = networkService;
        this.clusterBrowser = clusterBrowser;
    }

    @Override
    public void show(final JComponent panel) {
        initModel();
        clusterEventBus.register(this);
        view.show(panel);
    }

    public void updateNetwork() {
        final Optional<Network> newNetwork = networkService.getCommonNetwork(cluster, network);
        if (newNetwork.isEmpty()) {
            return;
        }
        model.setNetwork(newNetwork.get().getName());
        model.setIps(newNetwork.get().getAllIPs());
        model.setCidr(newNetwork.get().getCidr());
        view.update();
    }

    public void close() {
        clusterEventBus.unregister(this);
    }

    @Subscribe
    public void onNetInterfacesChanged(final NetInterfacesChangedEvent event) {
        if (!event.getHost().getCluster().equals(cluster)) {
            return;
        }
        updateNetwork();
    }

    @Override
    public ImageIcon getMenuIcon(Application.RunMode live) {
        return NETWORK_ICON;
    }

    @Override
    public ImageIcon getCategoryIcon(Application.RunMode live) {
        return null;
    }

    @Override
    public JPanel getGraphicalView() {
        return null;
    }

    @Override
    public void showPopup(JComponent tree, int x, int y) {

    }

    @Override
    public Browser getBrowser() {
        return clusterBrowser;
    }

    @Override
    public String toString() {
        return network.getName();
    }

    private void initModel() {
        updateNetwork();
        model.updateTable();
    }
}
