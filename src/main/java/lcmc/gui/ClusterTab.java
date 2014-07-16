/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.gui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import lcmc.data.Cluster;
import lcmc.utilities.Tools;
import org.springframework.stereotype.Component;

/**
 * An implementation of a cluster tab, that contains host views of the hosts,
 * that are in the cluster.
 */
@Component
public final class ClusterTab extends JPanel {
    private Cluster cluster;

    public void initWithCluster(final Cluster cluster0) {
        this.cluster = cluster0;
        if (cluster != null) {
            cluster.setClusterTab(this);
        }
        setLayout(new BorderLayout());
        setBackground(Tools.getDefaultColor("ViewPanel.Status.Background"));
        if (cluster == null) {
            final EmptyViewPanel p = new EmptyViewPanel();
            p.setDisabledDuringLoad(false);
            add(p);
        }
    }

    public void addClusterView() {
        if (cluster.hostsCount() > 0) {
            add(new ClusterViewPanel(cluster));
        }
        repaint();
    }

    Cluster getCluster() {
        return cluster;
    }

    @Override
    public String getName() {
        return getClusterName();
    }

    private String getClusterName() {
        if (cluster == null) {
            return null;
        }
        return cluster.getName();
    }
}
