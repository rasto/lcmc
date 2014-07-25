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

package lcmc.gui.dialog.drbdUpgrade;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.model.Host;
import lcmc.model.drbd.DrbdInstallation;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.dialog.host.DialogHost;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import lcmc.utilities.ssh.ExecCommandConfig;

/**
 * An implementation of a dialog where drbd installation is checked.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class CheckInstallation extends DialogHost {
    /** Drbd label. */
    private static final JLabel DRBD_LABEL = new JLabel(": " + Tools.getString(
                                "Dialog.Host.CheckInstallation.CheckingDrbd"));

    /** Install drbd icon. */
    private static final MyButton DRBD_BUTTON = new MyButton(
           Tools.getString("Dialog.Host.CheckInstallation.DrbdInstallButton"));
    /** Checking what is installed icon. */
    private static final ImageIcon CHECKING_ICON =
        Tools.createImageIcon(
               Tools.getDefault("Dialog.Host.CheckInstallation.CheckingIcon"));
    /** Not installed icon. */
    private static final ImageIcon NOT_INSTALLED_ICON =
        Tools.createImageIcon(
           Tools.getDefault("Dialog.Host.CheckInstallation.NotInstalledIcon"));
    /** Installed icon. */
    private static final ImageIcon INSTALLED_ICON =
        Tools.createImageIcon(
              Tools.getDefault("Dialog.Host.CheckInstallation.InstalledIcon"));
    /** Upgrade available icon. */
    private static final ImageIcon UPGR_AVAIL_ICON =
        Tools.createImageIcon(
              Tools.getDefault("Dialog.Host.CheckInstallation.UpgrAvailIcon"));
    /** Drbd icon wrapped in a JLabel. */
    private static final JLabel DRBD_ICON = new JLabel(CHECKING_ICON);
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;
    /** Whether drbd was installed without failure. */
    private boolean drbdOk = false;

    CheckInstallation(final WizardDialog previousDialog,
                      final Host host,
                      final DrbdInstallation drbdInstallation) {
        super(previousDialog, host, drbdInstallation);
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        drbdOk = false;
        final CheckInstallation thisClass = this;
        DRBD_BUTTON.setEnabled(false);
        DRBD_BUTTON.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (drbdOk) {
                        getDrbdInstallation().setDrbdWillBeUpgraded(true);
                    }
                    nextDialogObject = new LinbitLogin(thisClass, getHost(), getDrbdInstallation());
                    buttonClass(nextButton()).pressButton();
                }
            }
        );

        enableComponentsLater(new JComponent[]{buttonClass(finishButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        getHost().execCommand(new ExecCommandConfig().commandString("DrbdCheck.version")
                                                     .progressBar(getProgressBar())
                                                     .execCallback(new ExecCallback() {
                                                         @Override
                                                         public void done(final String answer) {
                                                             checkDrbd(answer);
                                                         }
                                                         @Override
                                                         public void doneError(final String answer,
                                                                               final int errorCode) {
                                                             checkDrbd(""); /* not installed */
                                                         }
                                                     }));
    }

    /** Checks if drbd installation was ok. */
    void checkDrbd(final String ans) {
        if (ans == null || ans.isEmpty() || "\n".equals(ans)) {
            DRBD_LABEL.setText(": " + Tools.getString(
                            "Dialog.Host.CheckInstallation.DrbdNotInstalled"));
            DRBD_ICON.setIcon(NOT_INSTALLED_ICON);
            DRBD_BUTTON.setEnabled(true);
        } else {
            DRBD_LABEL.setText(": " + ans.trim());
            drbdOk = true;
            if (getDrbdInstallation().isDrbdUpgradeAvailable(ans.trim())) {
                DRBD_ICON.setIcon(UPGR_AVAIL_ICON);
                DRBD_BUTTON.setText(Tools.getString(
                           "Dialog.Host.CheckInstallation.DrbdUpgradeButton"));
                DRBD_BUTTON.setEnabled(true);
            } else {
                DRBD_ICON.setIcon(INSTALLED_ICON);
            }
        }
        if (drbdOk) {
            answerPaneSetText(Tools.getString(
                                       "Dialog.Host.CheckInstallation.AllOk"));
            enableComponents();
            progressBarDone();
        } else {
            progressBarDoneError();
            printErrorAndRetry(Tools.getString(
                                  "Dialog.Host.CheckInstallation.SomeFailed"));
        }
    }

    /** Returns the next dialog object. It is set dynamicaly. */
    @Override
    public WizardDialog nextDialog() {
        return nextDialogObject;
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.Host.CheckInstallation.Title in TextResources.
     */
    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.CheckInstallation.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.Host.CheckInstallation.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.CheckInstallation.Description");
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

    /** Returns input pane with installation pane and answer pane. */
    @Override
    protected JPanel getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getInstallationPane());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(Tools.getString(
                                   "Dialog.Host.CheckInstallation.Checking")));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  //rows, cols
                                              1, 1,  //initX, initY
                                              1, 1); //xPad, yPad

        return pane;
    }
}
