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

package lcmc.drbd.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Named;

import lcmc.common.ui.MainPanel;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.drbd.ui.configdialog.Resource;
import lcmc.drbd.ui.configdialog.Start;
import lcmc.drbd.ui.resource.BlockDevInfo;
import lcmc.drbd.ui.resource.GlobalInfo;
import lcmc.drbd.ui.resource.ResourceInfo;
import lcmc.drbd.ui.resource.VolumeInfo;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * Show step by step dialogs that add and configure new host.
 */
@Named
public final class AddDrbdConfigDialog {
    private static final Logger LOG = LoggerFactory.getLogger(AddDrbdConfigDialog.class);
    private boolean wizardCanceled = false;
    private GlobalInfo globalInfo;
    private BlockDevInfo blockDevInfo1;
    private BlockDevInfo blockDevInfo2;
    private final MainPanel mainPanel;
    private final Start startDialog;
    private final Resource resourceDialog;
    private final SwingUtils swingUtils;

    public AddDrbdConfigDialog(MainPanel mainPanel, Start startDialog, Resource resourceDialog, SwingUtils swingUtils) {
        this.mainPanel = mainPanel;
        this.startDialog = startDialog;
        this.resourceDialog = resourceDialog;
        this.swingUtils = swingUtils;
    }

    public void init(final GlobalInfo globalInfo, final BlockDevInfo blockDevInfo1, final BlockDevInfo blockDevInfo2) {
        this.globalInfo = globalInfo;
        this.blockDevInfo1 = blockDevInfo1;
        this.blockDevInfo2 = blockDevInfo2;
    }

    public void showDialogs() {
        WizardDialog dialog;
        if (!globalInfo.getDrbdResources()
                       .isEmpty() && globalInfo.atLeastVersion("8.4")) {
            startDialog.init(null, blockDevInfo1, blockDevInfo2);
            dialog = startDialog;
        } else {
            final List<BlockDevInfo> blockDevices = new ArrayList<>(Arrays.asList(blockDevInfo1, blockDevInfo2));
            final ResourceInfo resourceInfo = globalInfo.getNewDrbdResource(
                                                            VolumeInfo.getHostsFromBlockDevices(blockDevices));
            final VolumeInfo dvi = globalInfo.getNewDrbdVolume(resourceInfo, blockDevices);
            resourceInfo.addDrbdVolume(dvi);
            globalInfo.addDrbdResource(resourceInfo);
            swingUtils.invokeAndWait(() -> globalInfo.addDrbdVolume(dvi));
            resourceDialog.init(null, dvi);
            dialog = resourceDialog;
        }
        mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.EXPAND);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName());
            final WizardDialog newdialog = (WizardDialog) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                dialog.cancelDialog();
                wizardCanceled = true;
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

    public boolean isWizardCanceled() {
        return wizardCanceled;
    }
}
