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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lcmc.gui.GUIData;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.dialog.drbdConfig.Resource;
import lcmc.gui.dialog.drbdConfig.Start;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.gui.resources.drbd.GlobalInfo;
import lcmc.gui.resources.drbd.ResourceInfo;
import lcmc.gui.resources.drbd.VolumeInfo;
import lcmc.model.Application;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

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
    @Inject
    private GUIData guiData;
    @Inject
    private Start startDialog;
    @Inject
    private Resource resourceDialog;
    @Inject
    private Application application;

    public void init(final GlobalInfo globalInfo, final BlockDevInfo blockDevInfo1, final BlockDevInfo blockDevInfo2) {
        this.globalInfo = globalInfo;
        this.blockDevInfo1 = blockDevInfo1;
        this.blockDevInfo2 = blockDevInfo2;
    }

    public void showDialogs() {
        WizardDialog dialog;
        if (!globalInfo.getDrbdResources().isEmpty() && globalInfo.atLeastVersion("8.4")) {
            startDialog.init(null, blockDevInfo1, blockDevInfo2);
            dialog = startDialog;
        } else {
            final List<BlockDevInfo> blockDevices = new ArrayList<BlockDevInfo>(Arrays.asList(blockDevInfo1,
                                                                                              blockDevInfo2));
            final ResourceInfo resourceInfo = globalInfo.getNewDrbdResource(
                                                            VolumeInfo.getHostsFromBlockDevices(blockDevices));
            final VolumeInfo dvi = globalInfo.getNewDrbdVolume(resourceInfo, blockDevices);
            resourceInfo.addDrbdVolume(dvi);
            globalInfo.addDrbdResource(resourceInfo);
            application.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    globalInfo.addDrbdVolume(dvi);
                }
            });
            resourceDialog.init(null, dvi);
            dialog = resourceDialog;
        }
        guiData.expandTerminalSplitPane(GUIData.TerminalSize.EXPAND);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName());
            final WizardDialog newdialog = (WizardDialog) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                dialog.cancelDialog();
                wizardCanceled = true;
                guiData.expandTerminalSplitPane(GUIData.TerminalSize.COLLAPSE);
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
