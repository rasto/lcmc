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

import lcmc.gui.GUIData;
import lcmc.model.Application;
import lcmc.model.Host;
import lcmc.model.drbd.DrbdInstallation;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Show step by step dialogs that add and configure new host.
 */
@Named
public final class AddHostDialog {
    private static final Logger LOG = LoggerFactory.getLogger(AddHostDialog.class);
    private Host host;
    @Resource(name="newHostDialog")
    private NewHostDialog newHostDialog;
    @Inject
    private GUIData guiData;
    @Inject
    private Application application;

    public void showDialogs(final Host host) {
        guiData.enableAddHostButtons(false);
        DialogHost dialog = newHostDialog;
        dialog.init(null, host, new DrbdInstallation());
        guiData.expandTerminalSplitPane(GUIData.TerminalSize.EXPAND);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName());
            final DialogHost newdialog = (DialogHost) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                /* remove host tab from main window */
                host.disconnect();
                application.removeHostFromHosts(host);
                dialog.cancelDialog();
                guiData.enableAddHostButtons(true);
                guiData.expandTerminalSplitPane(GUIData.TerminalSize.COLLAPSE);
                if (newdialog == null) {
                    LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName() + " canceled");
                    return;
                }
            } else if (dialog.isPressedFinishButton()) {
                LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName() + " finished");
                guiData.allHostsUpdate();
                application.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        guiData.checkAddClusterButtons();
                    }
                });
                break;
            }
            dialog = newdialog;
        }
        guiData.expandTerminalSplitPane(GUIData.TerminalSize.COLLAPSE);
        guiData.enableAddHostButtons(true);
    }
}
