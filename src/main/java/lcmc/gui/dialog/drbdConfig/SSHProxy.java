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

package lcmc.gui.dialog.drbdConfig;

import lcmc.data.Host;
import lcmc.utilities.Tools;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.dialog.host.SSH;
import lcmc.gui.resources.DrbdVolumeInfo;

/**
 * An implementation of a dialog where ssh connection will be established.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class SSHProxy extends SSH {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Drbd volume info. */
    private final DrbdVolumeInfo drbdVolumeInfo;

    /** Prepares a new <code>SSHProxy</code> object. */
    public SSHProxy(final WizardDialog previousDialog,
                    final Host host,
                    final DrbdVolumeInfo drbdVolumeInfo) {
        super(previousDialog, host);
        this.drbdVolumeInfo = drbdVolumeInfo;
    }

    /** Returns the next dialog. Devices */
    @Override
    public WizardDialog nextDialog() {
        //final DrbdResourceInfo dri = drbdVolumeInfo.getDrbdResourceInfo();
        //dri.getDrbdInfo().addProxyHost(getHost());
        //System.out.println("add proxy host: " + getHost());
        //dri.resetInfoPanel();
        //dri.getInfoPanel();
        //dri.waitForInfoPanel();
        //dri.selectMyself();
        return new DevicesProxy(this, getHost(), drbdVolumeInfo);
    }

    /**
     * Returns the title of the dialog, defined as
     * Dialog.Host.SSH.Title in TextResources.
     */
    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.SSH.Title");
    }

    /**
     * Returns the description of the dialog, defined as
     * Dialog.Host.SSH.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.SSH.Description");
    }
}
