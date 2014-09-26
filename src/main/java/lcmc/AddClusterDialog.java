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

import lcmc.gui.GUIData;
import lcmc.model.Application;
import lcmc.model.Cluster;
import lcmc.gui.dialog.cluster.DialogCluster;
import lcmc.gui.dialog.cluster.Name;
import lcmc.model.Hosts;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Shows step by step dialogs that add and configure new cluster.
 */
@Named
public final class AddClusterDialog {
    private static final Logger LOG = LoggerFactory.getLogger(AddClusterDialog.class);

    @Inject
    Name nameDialog;
    @Inject
    private GUIData guiData;
    @Inject
    private Cluster cluster;
    @Inject
    private Application application;
    @Inject
    private Hosts allHosts;

    /**
     * Must always be called from new thread.
     */
    public void showDialogs() {
        guiData.enableAddClusterButtons(false);
        cluster.setClusterTabClosable(false);
        DialogCluster dialog = nameDialog;
        dialog.init(null, cluster);
        guiData.expandTerminalSplitPane(GUIData.TerminalSize.EXPAND);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName());
            final DialogCluster newDialog = (DialogCluster) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                guiData.removeSelectedClusterTab();
                allHosts.removeHostsFromCluster(cluster);
                application.removeClusterFromClusters(cluster);
                dialog.cancelDialog();
                application.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        guiData.checkAddClusterButtons();
                    }
                });
                guiData.expandTerminalSplitPane(GUIData.TerminalSize.COLLAPSE);
                if (newDialog == null) {
                    LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName() + " canceled");
                    cluster.setClusterTabClosable(true);
                    return;
                }
            } else if (dialog.isPressedFinishButton()) {
                LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName() + " finished");
                break;
            }
            if (newDialog != null) {
                dialog = newDialog;
            }
        }
        guiData.expandTerminalSplitPane(GUIData.TerminalSize.COLLAPSE);
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                cluster.getClusterTab().addClusterView();
                cluster.getClusterTab().requestFocus();
            }
        });
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                guiData.checkAddClusterButtons();
            }
        });
        cluster.setClusterTabClosable(true);
    }
}
