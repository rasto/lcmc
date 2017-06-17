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

package lcmc.vm.ui;

import lcmc.common.ui.main.MainData;
import lcmc.common.domain.Application;
import lcmc.common.ui.MainPanel;
import lcmc.vm.ui.configdialog.Domain;
import lcmc.vm.ui.configdialog.VMConfig;
import lcmc.vm.ui.resource.DomainInfo;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Show step by step dialogs that add and configure new virtual domain.
 */
@Named
public final class AddVMConfigDialog {
    private static final Logger LOG = LoggerFactory.getLogger(AddVMConfigDialog.class);
    private DomainInfo vmsVirtualDomainInfo;
    @Inject
    private MainData mainData;
    @Inject
    private MainPanel mainPanel;
    @Inject
    private Domain domainDialog;

    public void init(final DomainInfo vmsVirtualDomainInfo) {
        this.vmsVirtualDomainInfo = vmsVirtualDomainInfo;
    }

    public void showDialogs() {
        vmsVirtualDomainInfo.setDialogStarted(true);
        VMConfig dialog = domainDialog;
        dialog.init(null, vmsVirtualDomainInfo);
        mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.EXPAND);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName());
            final VMConfig newdialog = (VMConfig) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                dialog.cancelDialog();
                vmsVirtualDomainInfo.getBrowser().reloadAllComboBoxes(null);
                vmsVirtualDomainInfo.removeMyself(Application.RunMode.LIVE);
                vmsVirtualDomainInfo.setDialogStarted(false);
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
        vmsVirtualDomainInfo.setDialogStarted(false);
        vmsVirtualDomainInfo.getBrowser().reloadAllComboBoxes(null);
        mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.COLLAPSE);
        mainData.getMainFrame().requestFocus();
    }
}
