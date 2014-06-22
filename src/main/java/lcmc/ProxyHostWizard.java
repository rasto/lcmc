/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2012-2013, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
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
import lcmc.data.drbd.DrbdInstallation;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.dialog.drbdConfig.NewProxyHostDialog;
import lcmc.gui.resources.drbd.VolumeInfo;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

/**
 * Show step by step dialogs that add and configure new proxy host.
 */
public final class ProxyHostWizard {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyHostWizard.class);
    private final VolumeInfo volumeInfo;
    private final Host host;

    public ProxyHostWizard(final Host host, final VolumeInfo volumeInfo) {
        this.host = host;
        this.volumeInfo = volumeInfo;
    }

    public void showDialogs() {
        WizardDialog dialog = new NewProxyHostDialog(null, host, volumeInfo, null, new DrbdInstallation());
        Tools.getGUIData().expandTerminalSplitPane(0);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName());
            final WizardDialog newdialog = (WizardDialog) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                dialog.cancelDialog();
                Tools.getGUIData().expandTerminalSplitPane(1);
                if (newdialog == null) {
                    LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName() + " canceled");
                    return;
                }
            } else if (dialog.isPressedFinishButton()) {
                LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName() + " finished");
                break;
            }
            dialog = newdialog;
        }
    }
}
