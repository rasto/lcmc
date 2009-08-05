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

package drbd.gui.dialog;

import drbd.data.Host;
import drbd.gui.SpringUtilities;
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
public class HostDistDetection extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Prepares a new <code>HostDistDetection</code> object.
     */
    public HostDistDetection(final WizardDialog previousDialog,
                             final Host host) {
        super(previousDialog, host);
    }

    /**
     * Inits dialog and starts the distribution detection.
     */
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});

        final String support =
                      Tools.getDistString("Support",
                                          getHost().getDist(),
                                          getHost().getDistVersionString());
        final String answerText = "\nversion: " + getHost().getDetectedInfo()
                        + " (support file: "
                        + support + ")";
        answerPaneSetText(answerText);
        enableComponents();
    }

    /**
     * Returns the next dialog which is HostCheckInstallation.
     */
    public WizardDialog nextDialog() {
        return new HostCheckInstallation(this, getHost());
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.HostDistDetection.Title in TextResources.
     */
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.HostDistDetection.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.HostDistDetection.Description in TextResources.
     */
    protected String getDescription() {
        return Tools.getString("Dialog.HostDistDetection.Description");
    }

    /**
     * Returns the input pane with check boxes and other info.
     */
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getAnswerPane(Tools.getString(
                                       "Dialog.HostDistDetection.Executing")));
        SpringUtilities.makeCompactGrid(pane, 1, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }
}
