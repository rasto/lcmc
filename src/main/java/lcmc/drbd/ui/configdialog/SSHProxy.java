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

import javax.inject.Named;
import javax.inject.Provider;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.drbd.domain.DrbdInstallation;
import lcmc.drbd.ui.resource.VolumeInfo;
import lcmc.host.domain.Host;
import lcmc.host.ui.Devices;
import lcmc.host.ui.SSH;

/**
 * An implementation of a dialog where ssh connection will be established.
 */
@Named
public final class SSHProxy extends SSH {
    private VolumeInfo volumeInfo;
    private WizardDialog origDialog;
    private WizardDialog nextDialogObject = null;
    private final DevicesProxy devicesProxyDialog;

    public SSHProxy(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData, Devices devices,
            DevicesProxy devicesProxyDialog, Provider<ProgressBar> progressBarProvider) {
        super(application, swingUtils, widgetFactory, mainData, devices, progressBarProvider);
        this.devicesProxyDialog = devicesProxyDialog;
    }

    public void init(final WizardDialog previousDialog, final Host host, final VolumeInfo volumeInfo, final WizardDialog origDialog,
            final DrbdInstallation drbdInstallation) {
        super.init(previousDialog, host, drbdInstallation);
        this.volumeInfo = volumeInfo;
        this.origDialog = origDialog;
    }

    @Override
    public WizardDialog nextDialog() {
        if (nextDialogObject == null) {
            devicesProxyDialog.init(this, getHost(), volumeInfo, origDialog, getDrbdInstallation());
            return devicesProxyDialog;
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
