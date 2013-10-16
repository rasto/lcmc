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

import lcmc.utilities.Tools;

import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.dialog.drbdUpgrade.Dist;
import lcmc.gui.resources.HostDrbdInfo;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * AddDrbdUpgradeDialog.
 *
 * Show step by step dialogs that upgrades the drbd.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class AddDrbdUpgradeDialog {
    /** Logger. */
    private static final Logger LOG =
                       LoggerFactory.getLogger(AddDrbdUpgradeDialog.class);
    /** Whether the wizard was canceled. */
    private boolean canceled = false;
    /** Host on which the drbd will be upgraded. */
    private final HostDrbdInfo hostDrbdInfo;

    /** Prepares a new <code>AddDrbdUpgradeDialog</code> object. */
    public AddDrbdUpgradeDialog(final HostDrbdInfo hostDrbdInfo) {
        this.hostDrbdInfo = hostDrbdInfo;
    }

    /** Shows step by step dialogs that upgrade the drbd. */
    public void showDialogs() {
        WizardDialog dialog = new Dist(null, hostDrbdInfo.getHost());
        Tools.getGUIData().expandTerminalSplitPane(0);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName());
            final WizardDialog newdialog = (WizardDialog) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                dialog.cancelDialog();
                canceled = true;
                Tools.getGUIData().expandTerminalSplitPane(1);
                if (newdialog == null) {
                    LOG.debug1("showDialogs: dialog: "
                               + dialog.getClass().getName() + " canceled");
                    return;
                }
            } else if (dialog.isPressedFinishButton()) {
                LOG.debug1("showDialogs: dialog: "
                           + dialog.getClass().getName() + " finished");
                break;
            }
            dialog = newdialog;
        }
        Tools.getGUIData().expandTerminalSplitPane(1);
        Tools.getGUIData().getMainFrame().requestFocus();
    }

    /** Returns whether the wizard was canceled. */
    public boolean isCanceled() {
        return canceled;
    }
}
