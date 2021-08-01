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

package lcmc.host.ui;

import javax.annotation.Resource;
import javax.inject.Named;

import lcmc.common.domain.Application;
import lcmc.common.ui.MainPanel;
import lcmc.common.ui.main.MainPresenter;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.drbd.domain.DrbdInstallation;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * Show step by step dialogs that add and configure new host.
 */
@Named
public final class AddHostDialog {
    private static final Logger LOG = LoggerFactory.getLogger(AddHostDialog.class);
    private Host host;
    @Resource(name = "newHostDialog")
    private NewHostDialog newHostDialog;
    private final MainPresenter mainPresenter;
    private final MainPanel mainPanel;
    private final Application application;
    private final SwingUtils swingUtils;

    public AddHostDialog(MainPresenter mainPresenter, MainPanel mainPanel, Application application, SwingUtils swingUtils) {
        this.mainPresenter = mainPresenter;
        this.mainPanel = mainPanel;
        this.application = application;
        this.swingUtils = swingUtils;
    }

    public void showDialogs(final Host host) {
        mainPresenter.enableAddHostButtons(false);
        DialogHost dialog = newHostDialog;
        dialog.init(null, host, new DrbdInstallation());
        mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.EXPAND);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass()
                                                       .getName());
            final DialogHost newdialog = (DialogHost) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                /* remove host tab from main window */
                host.disconnect();
                application.removeHostFromHosts(host);
                dialog.cancelDialog();
                mainPresenter.enableAddHostButtons(true);
                mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.COLLAPSE);
                if (newdialog == null) {
                    LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName() + " canceled");
                    return;
                }
            } else if (dialog.isPressedFinishButton()) {
                LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName() + " finished");
                mainPresenter.allHostsUpdate();
                swingUtils.invokeLater(() -> mainPresenter.checkAddClusterButtons());
                break;
            }
            dialog = newdialog;
        }
        mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.COLLAPSE);
        mainPresenter.enableAddHostButtons(true);
    }
}
