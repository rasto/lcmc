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

import drbd.gui.dialog.vm.VMConfig;
import drbd.gui.dialog.vm.Domain;
import drbd.gui.resources.VMSVirtualDomainInfo;

/**
 * AddVMConfigDialog.
 *
 * Show step by step dialogs that add and configure new virtual domain.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class AddVMConfigDialog {
    /** Whether the wizard was canceled. */
    private boolean canceled = false;
    /** VMS virtual domain info object. */
    private final VMSVirtualDomainInfo vmsVirtualDomainInfo;

    /** Prepares new <code>AddVMConfigDialog</code> object. */
    public AddVMConfigDialog(final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        this.vmsVirtualDomainInfo = vmsVirtualDomainInfo;
    }

    /** Shows step by step dialogs that add and configure new vm domain. */
    public final void showDialogs() {
        VMConfig dialog = new Domain(null, vmsVirtualDomainInfo);
        Tools.getGUIData().expandTerminalSplitPane(0);
        while (true) {
            final VMConfig newdialog = (VMConfig) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                dialog.cancelDialog();
                canceled = true;
                vmsVirtualDomainInfo.getBrowser().reloadAllComboBoxes(null);
                vmsVirtualDomainInfo.removeMyself(false);
                return;
            } else if (dialog.isPressedFinishButton()) {
                break;
            }
            dialog = newdialog;
        }
        vmsVirtualDomainInfo.getBrowser().reloadAllComboBoxes(null);
        Tools.getGUIData().expandTerminalSplitPane(1);
        Tools.getGUIData().getMainFrame().requestFocus();
    }

    /** Returns whether the wizard was canceled. */
    public final boolean isCanceled() {
        return canceled;
    }
}
