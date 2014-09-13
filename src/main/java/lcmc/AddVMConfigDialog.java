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

import lcmc.gui.GUIData;
import lcmc.model.Application;
import lcmc.gui.dialog.vm.Domain;
import lcmc.gui.dialog.vm.VMConfig;
import lcmc.gui.resources.vms.DomainInfo;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Show step by step dialogs that add and configure new virtual domain.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public final class AddVMConfigDialog {
    private static final Logger LOG = LoggerFactory.getLogger(AddVMConfigDialog.class);
    private DomainInfo vmsVirtualDomainInfo;
    @Autowired
    private GUIData guiData;
    @Autowired
    private Domain domainDialog;

    public void init(final DomainInfo vmsVirtualDomainInfo) {
        this.vmsVirtualDomainInfo = vmsVirtualDomainInfo;
    }

    public void showDialogs() {
        vmsVirtualDomainInfo.setDialogStarted(true);
        VMConfig dialog = domainDialog;
        dialog.init(null, vmsVirtualDomainInfo);
        guiData.expandTerminalSplitPane(GUIData.TerminalSize.EXPAND);
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
        guiData.expandTerminalSplitPane(GUIData.TerminalSize.COLLAPSE);
        guiData.getMainFrame().requestFocus();
    }
}
