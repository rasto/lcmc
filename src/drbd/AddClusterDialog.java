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


package drbd;

import drbd.data.Clusters;
import drbd.data.Cluster;
import drbd.gui.dialog.cluster.DialogCluster;
import drbd.gui.dialog.cluster.Name;
import drbd.utilities.Tools;

/**
 * AddClusterDialog.
 *
 * Shows step by step dialogs that add and configure new cluster.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */

public class AddClusterDialog {
    /** All clusters object. */
    private Clusters clusters;
    /** Whether the wizard was canceled. */
    private boolean canceled = false;

    /**
     * Shows step by step dialogs that add and configure new cluster.
     * Must allways be called from thread.
     */
    public final void showDialogs() {
        Tools.getGUIData().enableAddClusterButtons(false);
        final Cluster cluster = new Cluster();
        DialogCluster dialog = new Name(null, cluster);
        while (true) {
            final DialogCluster newdialog = (DialogCluster) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                Tools.getGUIData().removeSelectedClusterTab();
                Tools.getConfigData().getHosts().removeHostsFromCluster(
                                                                      cluster);
                Tools.getConfigData().removeClusterFromClusters(cluster);
                canceled = true;
                dialog.cancelDialog();
                Tools.getGUIData().checkAddClusterButtons();
                return;
            } else if (dialog.isPressedFinishButton()) {
                break;
            }
            if (newdialog != null) {
                dialog = newdialog;
            }
        }
        cluster.getClusterTab().addClusterView();
        cluster.getClusterTab().requestFocus();
        Tools.getGUIData().checkAddClusterButtons();
    }

    /**
     * Whether the wizard was canceled.
     */
    public final boolean canceled() {
        return canceled;
    }

    /**
     * Returns the clusters.
     */
    public final Clusters getClusters() {
        return clusters;
    }
}
