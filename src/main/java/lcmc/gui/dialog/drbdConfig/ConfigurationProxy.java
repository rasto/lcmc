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
import lcmc.gui.dialog.host.Configuration;
import lcmc.gui.resources.DrbdInfo;
import lcmc.gui.resources.DrbdVolumeInfo;
import lcmc.gui.resources.DrbdResourceInfo;
import javax.swing.JComponent;

/**
 * An implementation of a dialog where entered ip or the host is looked up
 * with dns.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class ConfigurationProxy extends Configuration {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Drbd info. */
    private final DrbdInfo drbdInfo;
    /** Drbd volume info. */
    private final DrbdVolumeInfo drbdVolumeInfo;

    /** Prepares a new <code>ConfigurationProxy</code> object. */
    ConfigurationProxy(final WizardDialog previousDialog,
                       final Host host,
                       final DrbdInfo drbdInfo,
                       final DrbdVolumeInfo drbdVolumeInfo) {
        super(previousDialog, host);
        this.drbdInfo = drbdInfo;
        this.drbdVolumeInfo = drbdVolumeInfo;
    }

    /**
     * Returns the next dialog. Depending on if the host is already connected
     * it is the SSH or it is skipped and Devices is the next dialog.
     */
    @Override
    public WizardDialog nextDialog() {
        if (getHost().isConnected()) {
            resetDrbdResourcePanel();
            return new Resource(this, drbdVolumeInfo);
        } else {
            return new SSHProxy(this, getHost(), drbdInfo, drbdVolumeInfo);
        }
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.Host.Configuration.Title in TextResources.
     */
    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.Configuration.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.Host.Configuration.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.Configuration.Description");
    }

    /**
     * This causes the whole drbd resource panel to be reloaded.
     */
    private void resetDrbdResourcePanel() {
        if (drbdVolumeInfo != null) {
            final DrbdResourceInfo dri = drbdVolumeInfo.getDrbdResourceInfo();
            dri.resetInfoPanel();
            dri.getInfoPanel();
            dri.waitForInfoPanel();
            dri.selectMyself();
        }
    }

    /** Buttons that are enabled/disabled during checks. */
    @Override
    protected JComponent[] nextButtons() {
        return new JComponent[]{buttonClass(nextButton()),
                                buttonClass(finishButton())};
    }
}
