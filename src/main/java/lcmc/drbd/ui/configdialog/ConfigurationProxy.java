/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2013, Rastislav Levrinc.
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


package lcmc.drbd.ui.configdialog;

import lcmc.model.Host;
import lcmc.model.drbd.DrbdInstallation;
import lcmc.common.ui.WizardDialog;
import lcmc.host.ui.Configuration;
import lcmc.gui.resources.drbd.VolumeInfo;
import lcmc.utilities.MyButton;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * An implementation of a dialog where entered ip or the host is looked up
 * with dns.
 */
@Named
final class ConfigurationProxy extends Configuration {
    private VolumeInfo volumeInfo;
    private WizardDialog origDialog;
    private WizardDialog nextDialogObject = null;
    @Inject
    private SSHProxy sshProxyDialog;

    void init(final WizardDialog previousDialog,
              final Host host,
              final VolumeInfo volumeInfo,
              final WizardDialog origDialog,
              final DrbdInstallation drbdInstallation) {
        super.init(previousDialog, host, drbdInstallation);
        this.volumeInfo = volumeInfo;
        this.origDialog = origDialog;
    }

    /**
     * Returns the next dialog. Depending on if the host is already connected
     * it is the SSH or it is skipped and Devices is the next dialog.
     */
    @Override
    public WizardDialog nextDialog() {
        if (nextDialogObject == null) {
            sshProxyDialog.init(this, getHost(), volumeInfo, origDialog, getDrbdInstallation());
            return sshProxyDialog;
        } else {
            return nextDialogObject;
        }
    }

    @Override
    protected void finishDialog() {
        super.finishDialog();
        if (isPressedFinishButton()) {
            if (origDialog != null) {
                nextDialogObject = origDialog;
                setPressedButton(nextButton());
            }
            getHost().getCluster().addProxyHost(getHost());
            if (volumeInfo != null) {
                volumeInfo.getDrbdResourceInfo().resetDrbdResourcePanel();
                volumeInfo.getBrowser().getGlobalInfo().addProxyHostNode(getHost());
            }
        }
    }

    @Override
    protected MyButton[] nextButtons() {
        return new MyButton[]{buttonClass(finishButton())};
    }

    @Override
    protected WizardDialog dialogAfterCancel() {
        return origDialog;
    }
}
