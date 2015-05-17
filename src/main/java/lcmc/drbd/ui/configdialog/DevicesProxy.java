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

import lcmc.host.domain.Host;
import lcmc.drbd.domain.DrbdInstallation;
import lcmc.common.ui.WizardDialog;
import lcmc.host.ui.Devices;
import lcmc.drbd.ui.resource.VolumeInfo;
import lcmc.common.ui.utils.MyButton;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * An implementation of a dialog where hardware information is collected.
 */
@Named
final class DevicesProxy extends Devices {
    private VolumeInfo volumeInfo;
    private WizardDialog origDialog;
    private WizardDialog nextDialogObject = null;
    @Inject
    private ProxyCheckInstallation proxyCheckInstallationDialog;

    void init(final WizardDialog previousDialog,
              final Host host,
              final VolumeInfo volumeInfo,
              final WizardDialog origDialog,
              final DrbdInstallation drbdInstallation) {
        super.init(previousDialog, host, drbdInstallation);
        this.volumeInfo = volumeInfo;
        this.origDialog = origDialog;
    }

    @Override
    public WizardDialog nextDialog() {
        if (nextDialogObject == null) {
            proxyCheckInstallationDialog.init(this, getHost(), volumeInfo, origDialog, getDrbdInstallation());
            return proxyCheckInstallationDialog;
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
