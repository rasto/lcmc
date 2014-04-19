/*
 * This file is part of the LCMC by Rasto Levrinc.
 *
 * Copyright (C) 2013, Rastislav Levrinc.
 *
 * The is free software; you can redistribute it and/or
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

package lcmc.gui.dialog.drbdConfig;

import lcmc.data.Host;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.dialog.host.NewHost;
import lcmc.gui.resources.drbd.VolumeInfo;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;

/**
 * An implementation of a dialog where user can enter either ip or hostname of
 * the host and user name.
 *
 * @author Rasto Levrinc
 *
 */
public final class NewProxyHost extends NewHost {
    /** Drbd volume info. */
    private final VolumeInfo volumeInfo;
    /** The dialog we came from. */
    private final WizardDialog origDialog;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;


    /** Prepares a new {@code NewProxyHost} object. */
    public NewProxyHost(final WizardDialog previousDialog,
                        final Host host,
                        final VolumeInfo volumeInfo,
                        final WizardDialog origDialog) {
        super(previousDialog, host);
        this.volumeInfo = volumeInfo;
        this.origDialog = origDialog;
    }

    /** SetsnextDialog to Configuration. */
    @Override
    public WizardDialog nextDialog() {
        if (nextDialogObject == null) {
            return new ConfigurationProxy(this,
                                          getHost(),
                                          volumeInfo,
                                          origDialog);
        } else {
            return nextDialogObject;
        }
    }

    /** Finish dialog. */
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
                volumeInfo.getDrbdInfo().addProxyHostNode(getHost());
            }
        }
    }

    /**
     * Returns the title of the dialog, defined as
     * Dialog.Host.NewProxyHost.Title in TextResources.
     */
    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.NewProxyHost.Title");
    }

    /**
     * Returns the description of the dialog, defined as
     * Dialog.Host.NewProxyHost.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.NewProxyHost.Description");
    }

    /** Buttons that are enabled/disabled during check. */
    @Override
    protected MyButton[] nextButtons() {
        return new MyButton[]{buttonClass(finishButton())};
    }

    /**
     * Return dialog that comes after "cancel" button was pressed.
     */
    @Override
    protected WizardDialog dialogAfterCancel() {
        return origDialog;
    }
}
