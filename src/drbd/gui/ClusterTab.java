/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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


package drbd.gui;

import drbd.data.Cluster;
import drbd.utilities.Tools;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.SwingUtilities;

/**
 * An implementation of a cluster tab, that contains host views of the hosts,
 * that are in the cluster.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class ClusterTab extends JPanel {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Cluster data object. */
    private final Cluster cluster;

    /**
     * Prepares a new <code>ClusterTab</code> object.
     */
    public ClusterTab(final Cluster cluster) {
        super(new BorderLayout());
        setBackground(Tools.getDefaultColor("ViewPanel.Status.Background"));
        this.cluster = cluster;
        if (cluster == null) {
            final EmptyViewPanel p = new EmptyViewPanel();
            p.setDisabledDuringLoad(false);
            add(p);
        }
    }

    /**
     * adds host views to the desktop.
     */
    public final void addClusterView() {
        if (cluster.hostsCount() > 0) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    add(new ClusterViewPanel(cluster));
                }
            });
        }
        repaint();
    }

    /**
     * Returns cluster object.
     */
    public final Cluster getCluster() {
        return cluster;
    }

    /**
     * Returns name of the cluster.
     */
    public final String getName() {
        if (cluster == null) {
            return null;
        }
        return cluster.getName();
    }
}
