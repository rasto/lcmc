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

import lcmc.gui.dialog.drbd.SplitBrain;
import lcmc.gui.dialog.drbdConfig.DrbdConfig;
import lcmc.gui.resources.drbd.VolumeInfo;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

/**
 * AddDrbdSplitBrainDialog.
 *
 * Show step by step dialogs that resolve a drbd split-brain.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class AddDrbdSplitBrainDialog {
    /** Logger. */
    private static final Logger LOG =
                       LoggerFactory.getLogger(AddDrbdSplitBrainDialog.class);
    /** Whether the wizard was canceled. */
    private boolean canceled = false;
    /** Drbd resource info object. */
    private final VolumeInfo dvi;

    /** Prepares a new {@code AddDrbdSplitBrainDialog} object. */
    public AddDrbdSplitBrainDialog(final VolumeInfo dvi) {
        this.dvi = dvi;
    }

    /** Shows step by step dialogs that resolve a drbd split-brain. */
    public void showDialogs() {
        DrbdConfig dialog = new SplitBrain(null, dvi);
        Tools.getGUIData().expandTerminalSplitPane(0);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName());
            final DrbdConfig newdialog = (DrbdConfig) dialog.showDialog();
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
