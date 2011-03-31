/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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

package drbd.gui.dialog.host;

import drbd.data.Host;
import drbd.gui.SpringUtilities;
import drbd.gui.dialog.WizardDialog;
import drbd.utilities.Tools;

import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JComponent;

/**
 * An implementation of a dialog that shows which distribution was detected.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class DistDetection extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Prepares a new <code>DistDetection</code> object. */
    DistDetection(final WizardDialog previousDialog, final Host host) {
        super(previousDialog, host);
    }

    /** Inits dialog and starts the distribution detection. */
    @Override protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});

        final String support =
                      Tools.getDistString("Support",
                                          getHost().getDist(),
                                          getHost().getDistVersionString(),
                                          getHost().getArch());
        final String answerText = "\nversion: " + getHost().getDetectedInfo()
                        + " (support file: "
                        + support + ")";
        answerPaneSetText(answerText);
        enableComponents();
        if (!Tools.getConfigData().getAutoHosts().isEmpty()) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    /** Returns the next dialog which is CheckInstallation. */
    @Override public WizardDialog nextDialog() {
        return new CheckInstallation(this, getHost());
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.Host.DistDetection.Title in TextResources.
     */
    @Override protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DistDetection.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.Host.DistDetection.Description in TextResources.
     */
    @Override protected String getDescription() {
        return Tools.getString("Dialog.Host.DistDetection.Description");
    }

    /** Returns the input pane with check boxes and other info. */
    @Override protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getAnswerPane(Tools.getString(
                                      "Dialog.Host.DistDetection.Executing")));
        SpringUtilities.makeCompactGrid(pane, 1, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }
}
