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

package lcmc.drbd.ui;

import javax.inject.Named;

import lcmc.common.ui.MainPanel;
import lcmc.common.ui.WizardDialog;
import lcmc.drbd.domain.DrbdInstallation;
import lcmc.drbd.ui.configdialog.NewProxyHostDialog;
import lcmc.drbd.ui.resource.VolumeInfo;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * Show step by step dialogs that add and configure new proxy host.
 */
@Named
public final class ProxyHostWizard {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyHostWizard.class);
    private VolumeInfo volumeInfo;
    private Host host;
    private final MainPanel mainPanel;
    private final NewProxyHostDialog newProxyHostDialog;

    public ProxyHostWizard(MainPanel mainPanel, NewProxyHostDialog newProxyHostDialog) {
        this.mainPanel = mainPanel;
        this.newProxyHostDialog = newProxyHostDialog;
    }

    public void init(final Host host, final VolumeInfo volumeInfo) {
        this.host = host;
        this.volumeInfo = volumeInfo;
    }

    public void showDialogs() {
        newProxyHostDialog.init(null, host, volumeInfo, null, new DrbdInstallation());
        WizardDialog dialog = newProxyHostDialog;
        mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.EXPAND);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName());
            final WizardDialog newdialog = (WizardDialog) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                dialog.cancelDialog();
                mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.COLLAPSE);
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
