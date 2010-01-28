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

import drbd.utilities.Tools;

import drbd.gui.dialog.DrbdConfig;
import drbd.gui.dialog.DrbdSplitBrain;
import drbd.gui.resources.DrbdResourceInfo;

/**
 * AddDrbdSplitBrainDialog.
 *
 * Show step by step dialogs that resolve a drbd split-brain.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class AddDrbdSplitBrainDialog {

    /** Whether the wizard was canceled. */
    private boolean canceled = false;
    /** Drbd resource info object. */
    private final DrbdResourceInfo dri;

    /**
     * Prepares a new <code>AddDrbdSplitBrainDialog</code> object.
     */
    public AddDrbdSplitBrainDialog(final DrbdResourceInfo dri) {
        this.dri = dri;
    }

    /**
     * Shows step by step dialogs that resolve a drbd split-brain.
     */
    public final void showDialogs() {
        DrbdConfig dialog = new DrbdSplitBrain(null, dri);
        while (true) {
            final DrbdConfig newdialog = (DrbdConfig) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                dialog.cancelDialog();
                canceled = true;
                return;
            } else if (dialog.isPressedFinishButton()) {
                break;
            }
            dialog = newdialog;
        }
        Tools.getGUIData().getMainFrame().requestFocus();
    }

    /**
     * Returns whether the wizard was canceled.
     */
    public final boolean isCanceled() {
        return canceled;
    }
}
