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
import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import drbd.gui.SpringUtilities;
import drbd.utilities.ExecCallback;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SpringLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * An implementation of a dialog where drbd installation is checked.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HostCheckInstallationForUpgrade extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;
    /** Drbd label. */
    private static final JLabel DRBD_LABEL = new JLabel(": " + Tools.getString(
                                "Dialog.HostCheckInstallation.CheckingDrbd"));

    /** Install drbd icon. */
    private static final MyButton DRBD_BUTTON = new MyButton(
            Tools.getString("Dialog.HostCheckInstallation.DrbdInstallButton"));
    /** Checking what is installed icon. */
    private static final ImageIcon CHECKING_ICON =
        Tools.createImageIcon(
                Tools.getDefault("Dialog.HostCheckInstallation.CheckingIcon"));
    /** Not installed icon. */
    private static final ImageIcon NOT_INSTALLED_ICON =
        Tools.createImageIcon(
            Tools.getDefault("Dialog.HostCheckInstallation.NotInstalledIcon"));
    /** Installed icon. */
    private static final ImageIcon INSTALLED_ICON =
        Tools.createImageIcon(
                Tools.getDefault("Dialog.HostCheckInstallation.InstalledIcon"));
    /** Upgrade available icon. */
    private static final ImageIcon UPGR_AVAIL_ICON =
        Tools.createImageIcon(
                Tools.getDefault("Dialog.HostCheckInstallation.UpgrAvailIcon"));
    /** Drbd icon wrapped in a JLabel. */
    private static final JLabel DRBD_ICON = new JLabel(CHECKING_ICON);
    /** Whether drbd was installed without failure. */
    private boolean drbdOk = false;


    /**
     * Prepares a new <code>HostCheckInstallationForUpgrade</code> object.
     */
    public HostCheckInstallationForUpgrade(final WizardDialog previousDialog,
                                           final Host host) {
        super(previousDialog, host);
    }

    /**
     * Inits the dialog.
     */
    protected void initDialog() {
        super.initDialog();
        drbdOk = false;
        final HostCheckInstallationForUpgrade thisClass = this;
        DRBD_BUTTON.setEnabled(false);
        DRBD_BUTTON.addActionListener(
            new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    if (drbdOk) {
                        getHost().setDrbdWillBeUpgraded(true);
                    }
                    nextDialogObject = new HostLoginForUpgrade(thisClass,
                                                               getHost());
                    ((MyButton) buttonClass(nextButton())).pressButton();
                }
            }
        );

        enableComponentsLater(new JComponent[]{buttonClass(finishButton())});

        getHost().execCommand("DrbdCheck.version",
                         getProgressBar(),
                         new ExecCallback() {
                             public void done(final String ans) {
                                 checkDrbd(ans);
                             }
                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 checkDrbd(""); /* not installed */
                             }
                         }, false);
    }

    /**
     * Checks if drbd installation was ok.
     */
    public void checkDrbd(final String ans) {
        if ("".equals(ans) || "\n".equals(ans)) {
            DRBD_LABEL.setText(": " + Tools.getString(
                            "Dialog.HostCheckInstallation.DrbdNotInstalled"));
            DRBD_ICON.setIcon(NOT_INSTALLED_ICON);
            DRBD_BUTTON.setEnabled(true);
        } else {
            DRBD_LABEL.setText(": " + ans.trim());
            drbdOk = true;
            if (getHost().isDrbdUpgradeAvailable(ans.trim())) {
                DRBD_ICON.setIcon(UPGR_AVAIL_ICON);
                DRBD_BUTTON.setText(Tools.getString(
                            "Dialog.HostCheckInstallation.DrbdUpgradeButton"));
                DRBD_BUTTON.setEnabled(true);
            } else {
                DRBD_ICON.setIcon(INSTALLED_ICON);
            }
        }
        if (drbdOk) {
            answerPaneSetText(Tools.getString(
                                        "Dialog.HostCheckInstallation.AllOk"));
            enableComponents();
            progressBarDone();
        } else {
            progressBarDoneError();
            printErrorAndRetry(Tools.getString(
                                    "Dialog.HostCheckInstallation.SomeFailed"));
        }
    }

    /**
     * Returns the next dialog object. It is set dynamicaly.
     */
    public WizardDialog nextDialog() {
        return nextDialogObject;
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.HostCheckInstallation.Title in TextResources.
     */
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.HostCheckInstallation.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.HostCheckInstallation.Description in TextResources.
     */
    protected String getDescription() {
        return Tools.getString("Dialog.HostCheckInstallation.Description");
    }

    /**
     * Returns the pane, that checks the installation of different
     * components and provides buttons to update or upgrade.
     */
    private JPanel getInstallationPane() {
        final JPanel pane = new JPanel(new SpringLayout());

        pane.add(new JLabel("Drbd"));
        pane.add(DRBD_LABEL);
        pane.add(DRBD_BUTTON);
        pane.add(DRBD_ICON);
        SpringUtilities.makeCompactGrid(pane, 1, 4,  //rows, cols
                                              1, 1,  //initX, initY
                                              1, 1); //xPad, yPad
        return pane;
    }

    /**
     * Returns input pane with installation pane and answer pane.
     */
    protected JPanel getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getInstallationPane());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(Tools.getString(
                                    "Dialog.HostCheckInstallation.Checking")));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  //rows, cols
                                              1, 1,  //initX, initY
                                              1, 1); //xPad, yPad

        return pane;
    }
}
