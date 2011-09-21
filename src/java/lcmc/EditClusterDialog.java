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


package lcmc;

import lcmc.data.Cluster;
import lcmc.utilities.Tools;
import lcmc.gui.dialog.cluster.DialogCluster;
import lcmc.gui.dialog.cluster.Name;

/**
 * EditClusterDialog.
 *
 * Show step by step dialogs that configure a cluster.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class EditClusterDialog {
    /** Cluster object. */
    private final Cluster cluster;

    /** Prepares new <code>EditClusterDialog</code> object. */
    public EditClusterDialog(final Cluster cluster) {
        this.cluster = cluster;
    }

    /** Shows step by step dialogs that configure a new cluster. */
    public void showDialogs() {
        DialogCluster dialog = new Name(null, cluster);
        Tools.getGUIData().expandTerminalSplitPane(0);
        while (true) {
            final DialogCluster newdialog = (DialogCluster) dialog.showDialog();
            if (dialog.isPressedButton("Cancel")) {
                Tools.getGUIData().expandTerminalSplitPane(1);
                return;
            } else if (dialog.isPressedButton("Finish")) {
                break;
            }
            dialog = newdialog;
        }
        Tools.getGUIData().expandTerminalSplitPane(1);
    }
}
