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
import lcmc.gui.dialog.drbd.SplitBrain;
import lcmc.gui.dialog.drbdConfig.DrbdConfig;
import lcmc.gui.resources.drbd.VolumeInfo;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Show step by step dialogs that resolve a drbd split-brain.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public final class AddDrbdSplitBrainDialog {
    private static final Logger LOG = LoggerFactory.getLogger(AddDrbdSplitBrainDialog.class);
    private VolumeInfo volumeInfo;
    @Autowired
    private GUIData guiData;
    @Autowired
    private SplitBrain splitBrainDialog;

    public void init(final VolumeInfo volumeInfo) {
        this.volumeInfo = volumeInfo;
    }

    public void showDialogs() {
        splitBrainDialog.init(null, volumeInfo);
        DrbdConfig dialog = splitBrainDialog;
        guiData.expandTerminalSplitPane(GUIData.TerminalSize.EXPAND);
        while (true) {
            LOG.debug1("showDialogs: dialog: " + dialog.getClass().getName());
            final DrbdConfig newdialog = (DrbdConfig) dialog.showDialog();
            if (dialog.isPressedCancelButton()) {
                dialog.cancelDialog();
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
        guiData.expandTerminalSplitPane(GUIData.TerminalSize.COLLAPSE);
        guiData.getMainFrame().requestFocus();
    }
}
