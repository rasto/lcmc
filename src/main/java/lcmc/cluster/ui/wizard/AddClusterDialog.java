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

package lcmc.cluster.ui.wizard;

import javax.inject.Named;

import lcmc.cluster.domain.Cluster;
import lcmc.common.domain.Application;
import lcmc.common.ui.MainPanel;
import lcmc.common.ui.main.MainPresenter;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Hosts;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * Shows step by step dialogs that add and configure new cluster.
 */
@Named
public final class AddClusterDialog {
    private static final Logger LOG = LoggerFactory.getLogger(AddClusterDialog.class);

    final Name nameDialog;
    private final MainPresenter mainPresenter;
    private final MainPanel mainPanel;
    private final Cluster cluster;
    private final Application application;
    private final SwingUtils swingUtils;
    private final Hosts allHosts;

    public AddClusterDialog(Name nameDialog, MainPresenter mainPresenter, MainPanel mainPanel, Cluster cluster,
            Application application, SwingUtils swingUtils, Hosts allHosts) {
        this.nameDialog = nameDialog;
        this.mainPresenter = mainPresenter;
        this.mainPanel = mainPanel;
        this.cluster = cluster;
        this.application = application;
        this.swingUtils = swingUtils;
        this.allHosts = allHosts;
    }

    /**
     * Must always be called from new thread.
     */
    public void showDialogs() {
        mainPresenter.enableAddClusterButtons(false);
        cluster.setClusterTabClosable(false);
        DialogCluster dialog = nameDialog;
        dialog.init(null, cluster);
        mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.EXPAND);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName());
            final DialogCluster newDialog = (DialogCluster) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                mainPresenter.removeSelectedClusterTab();
                allHosts.removeHostsFromCluster(cluster);
                application.removeClusterFromClusters(cluster);
                dialog.cancelDialog();
                swingUtils.invokeLater(mainPresenter::checkAddClusterButtons);
                mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.COLLAPSE);
                if (newDialog == null) {
                    LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName() + " canceled");
                    cluster.setClusterTabClosable(true);
                    return;
                }
            } else if (dialog.isPressedFinishButton()) {
                LOG.debug1("showDialogs: dialog: " + dialog.getClass()
                                                           .getName() + " finished");
                break;
            }
            if (newDialog != null) {
                dialog = newDialog;
            }
        }
        mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.COLLAPSE);
        swingUtils.invokeLater(() -> cluster.getClusterTab()
                                            .ifPresent(clusterTab -> {
                                                clusterTab.addClusterView();
                                                clusterTab.requestFocus();
                                            }));
        swingUtils.invokeLater(mainPresenter::checkAddClusterButtons);
        cluster.setClusterTabClosable(true);
    }
}
