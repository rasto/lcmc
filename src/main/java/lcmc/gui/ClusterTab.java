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

import lcmc.data.Cluster;
import lcmc.utilities.Tools;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * An implementation of a cluster tab, that contains host views of the hosts,
 * that are in the cluster.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class ClusterTab extends JPanel {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Cluster data object. */
    private final Cluster cluster;

    /** Prepares a new <code>ClusterTab</code> object. */
    ClusterTab(final Cluster cluster) {
        super(new BorderLayout());
        setBackground(Tools.getDefaultColor("ViewPanel.Status.Background"));
        this.cluster = cluster;
        if (cluster == null) {
            final EmptyViewPanel p = new EmptyViewPanel();
            p.setDisabledDuringLoad(false);
            add(p);
        }
    }

    /** adds host views to the desktop. */
    public void addClusterView() {
        if (cluster.hostsCount() > 0) {
            add(new ClusterViewPanel(cluster));
        }
        repaint();
    }

    /** Returns cluster object. */
    Cluster getCluster() {
        return cluster;
    }

    /** Returns name of the cluster. */
    @Override
    public String getName() {
        if (cluster == null) {
            return null;
        }
        return cluster.getName();
    }
}
