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
import lcmc.model.Cluster;
import lcmc.gui.dialog.cluster.DialogCluster;
import lcmc.gui.dialog.cluster.Name;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Show step by step dialogs that configure a cluster.
 */
@Named
public final class EditClusterDialog {
    private static final Logger LOG = LoggerFactory.getLogger(EditClusterDialog.class);
    private static final String CANCEL_BTN = Tools.getString("Dialog.Dialog.Cancel");
    private static final String FINISH_BTN = Tools.getString("Dialog.Dialog.Finish");
    @Inject
    private Name nameDialog;
    @Inject
    private GUIData guiData;

    public void showDialogs(final Cluster cluster) {
        cluster.setClusterTabClosable(false);
        DialogCluster dialog = nameDialog;
        dialog.init(null, cluster);
        guiData.expandTerminalSplitPane(GUIData.TerminalSize.EXPAND);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName());
            final DialogCluster newdialog = (DialogCluster) dialog.showDialog();
            if (dialog.isPressedButton(CANCEL_BTN)) {
                guiData.expandTerminalSplitPane(GUIData.TerminalSize.COLLAPSE);
                if (newdialog == null) {
                    LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName() + " canceled");
                    cluster.setClusterTabClosable(true);
                    return;
                }
            } else if (dialog.isPressedButton(FINISH_BTN)) {
                LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName() + " finished");
                break;
            }
            dialog = newdialog;
        }
        guiData.expandTerminalSplitPane(GUIData.TerminalSize.COLLAPSE);
        cluster.setClusterTabClosable(true);
    }
}
