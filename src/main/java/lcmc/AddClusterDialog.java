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

package lcmc;

import lcmc.data.Clusters;
import lcmc.data.Cluster;
import lcmc.gui.dialog.cluster.DialogCluster;
import lcmc.gui.dialog.cluster.Name;
import lcmc.utilities.Tools;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * AddClusterDialog.
 *
 * Shows step by step dialogs that add and configure new cluster.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */

public final class AddClusterDialog {
    /** Logger. */
    private static final Logger LOG =
                              LoggerFactory.getLogger(AddClusterDialog.class);
    /** Whether the wizard was canceled. */
    private boolean canceled = false;

    /**
     * Shows step by step dialogs that add and configure new cluster.
     * Must allways be called from thread.
     */
    public void showDialogs() {
        Tools.getGUIData().enableAddClusterButtons(false);
        final Cluster cluster = new Cluster();
        cluster.setTabClosable(false);
        DialogCluster dialog = new Name(null, cluster);
        Tools.getGUIData().expandTerminalSplitPane(0);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName());
            final DialogCluster newdialog = (DialogCluster) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                Tools.getGUIData().removeSelectedClusterTab();
                Tools.getApplication().getHosts().removeHostsFromCluster(
                                                                      cluster);
                Tools.getApplication().removeClusterFromClusters(cluster);
                canceled = true;
                dialog.cancelDialog();
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        Tools.getGUIData().checkAddClusterButtons();
                    }
                });
                Tools.getGUIData().expandTerminalSplitPane(1);
                if (newdialog == null) {
                    LOG.debug1("showDialogs: dialog: "
                               + dialog.getClass().getName() + " canceled");
                    cluster.setTabClosable(true);
                    return;
                }
            } else if (dialog.isPressedFinishButton()) {
                LOG.debug1("showDialogs: dialog: "
                           + dialog.getClass().getName() + " finished");
                break;
            }
            if (newdialog != null) {
                dialog = newdialog;
            }
        }
        Tools.getGUIData().expandTerminalSplitPane(1);
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                cluster.getClusterTab().addClusterView();
                cluster.getClusterTab().requestFocus();
            }
        });
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                Tools.getGUIData().checkAddClusterButtons();
            }
        });
        cluster.setTabClosable(true);
    }

    /** Whether the wizard was canceled. */
    boolean canceled() {
        return canceled;
    }

    /** Returns the clusters. */
    Clusters getClusters() {
        return null;
    }
}
