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

import lcmc.data.Host;
import lcmc.utilities.Tools;

import lcmc.gui.dialog.host.DialogHost;
import lcmc.gui.dialog.host.NewHost;

/**
 * AddHostDialog.
 *
 * Show step by step dialogs that add and configure new host.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class AddHostDialog {
    /** Shows step by step dialogs that add and configure new host. */
    public void showDialogs() {
        Tools.getGUIData().enableAddHostButtons(false);
        final Host host = new Host();
        DialogHost dialog = new NewHost(null, host);
        Tools.getGUIData().expandTerminalSplitPane(0);
        while (true) {
            final DialogHost newdialog = (DialogHost) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                /* remove host tab from main window */
                host.disconnect();
                Tools.getConfigData().removeHostFromHosts(host);
                dialog.cancelDialog();
                Tools.getGUIData().enableAddHostButtons(true);
                Tools.getGUIData().expandTerminalSplitPane(1);
                return;
            } else if (dialog.isPressedFinishButton()) {
                Tools.getGUIData().allHostsUpdate();
                Tools.getGUIData().checkAddClusterButtons();
                break;
            }
            /* newdialog can be null, when we are out of memory I guess. */
            if (newdialog != null) {
                dialog = newdialog;
            }
        }
        Tools.getGUIData().expandTerminalSplitPane(1);
        Tools.getGUIData().enableAddHostButtons(true);
    }
}
