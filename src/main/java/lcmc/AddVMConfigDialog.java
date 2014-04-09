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

import lcmc.data.Application;
import lcmc.utilities.Tools;

import lcmc.gui.dialog.vm.VMConfig;
import lcmc.gui.dialog.vm.Domain;
import lcmc.gui.resources.VMSVirtualDomainInfo;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * AddVMConfigDialog.
 *
 * Show step by step dialogs that add and configure new virtual domain.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class AddVMConfigDialog {
    /** Logger. */
    private static final Logger LOG =
                             LoggerFactory.getLogger(AddVMConfigDialog.class);
    /** Whether the wizard was canceled. */
    private boolean canceled = false;
    /** VMS virtual domain info object. */
    private final VMSVirtualDomainInfo vmsVirtualDomainInfo;

    /** Prepares new {@code AddVMConfigDialog} object. */
    public AddVMConfigDialog(final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        this.vmsVirtualDomainInfo = vmsVirtualDomainInfo;
    }

    /** Shows step by step dialogs that add and configure new vm domain. */
    public void showDialogs() {
        vmsVirtualDomainInfo.setDialogStarted(true);
        VMConfig dialog = new Domain(null, vmsVirtualDomainInfo);
        Tools.getGUIData().expandTerminalSplitPane(0);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName());
            final VMConfig newdialog = (VMConfig) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                dialog.cancelDialog();
                canceled = true;
                vmsVirtualDomainInfo.getBrowser().reloadAllComboBoxes(null);
                vmsVirtualDomainInfo.removeMyself(Application.RunMode.LIVE);
                vmsVirtualDomainInfo.setDialogStarted(false);
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
        vmsVirtualDomainInfo.setDialogStarted(false);
        vmsVirtualDomainInfo.getBrowser().reloadAllComboBoxes(null);
        Tools.getGUIData().expandTerminalSplitPane(1);
        Tools.getGUIData().getMainFrame().requestFocus();
    }

    /** Returns whether the wizard was canceled. */
    public boolean isCanceled() {
        return canceled;
    }
}
