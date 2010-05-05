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
import drbd.utilities.Tools;
import drbd.utilities.ExecCallback;
import drbd.utilities.SSH;
import drbd.gui.SpringUtilities;
import drbd.gui.dialog.WizardDialog;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SpringLayout;

/**
 * An implementation of a dialog where drbd will be installed.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class DrbdLinbitInst extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;

    /**
     * Prepares a new <code>DrbdLinbitInst</code> object.
     */
    public DrbdLinbitInst(final WizardDialog previousDialog,
                          final Host host) {
        super(previousDialog, host);
    }

    /**
     * Inits dialog and starts the drbd install procedure.
     */
    protected final void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        getProgressBar().start(50000);

        getHost().execCommand("DrbdInst.mkdir",
                          getProgressBar(),
                          new ExecCallback() {
                            public void done(final String ans) {
                               checkFile(ans);
                            }
                            public void doneError(final String ans,
                                                  final int exitCode) {
                                printErrorAndRetry(Tools.getString(
                                      "Dialog.Host.DrbdLinbitInst.MkdirError"),
                                                   ans,
                                                   exitCode);
                            }
                          },
                          null,  /* ConvertCmdCallback */
                          true,  /* outputVisible */
                          SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /**
     * Checks whether the files have to be downloaded.
     */
    public final void checkFile(final String ans) {
        answerPaneSetText(
                   Tools.getString("Dialog.Host.DrbdLinbitInst.CheckingFile"));
        getHost().execCommand("DrbdInst.test",
                          getProgressBar(),
                          new ExecCallback() {
                              // TODO: exchange here done and doneError
                              // TODO: treat file exist differently as other
                              // errors.
                              public void done(final String ans) {
                                  answerPaneSetText(Tools.getString(
                                     "Dialog.Host.DrbdLinbitInst.FileExists"));
                                  installDrbd();
                              }
                              public void doneError(final String ans,
                                                    final int exitCode) {
                                  downloadDrbd();
                              }
                          },
                          null,  /* ConvertCmdCallback */
                          true,  /* outputVisible */
                          SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /**
     * Download the drbd packages.
     */
    public final void downloadDrbd() {
        answerPaneSetText(
                    Tools.getString("Dialog.Host.DrbdLinbitInst.Downloading"));
        getHost().execCommand("DrbdInst.wget",
                          getProgressBar(),
                          new ExecCallback() {
                            public void done(final String ans) {
                               installDrbd();
                            }
                            public void doneError(final String ans,
                                                  final int exitCode) {
                                printErrorAndRetry(Tools.getString(
                                        "Dialog.Host.DrbdLinbitInst.WgetError"),
                                                   ans,
                                                   exitCode);
                            }
                          },
                          null,  /* ConvertCmdCallback */
                          true,  /* outputVisible */
                          SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /**
     * Install the drbd packages.
     */
    public final void installDrbd() {
        getHost().setDrbdWasInstalled(true); /* even if we fail */
        Tools.getConfigData().setLastDrbdInstalledMethod(
                                            getHost().getDrbdInstallMethod());
        Tools.getConfigData().setLastDrbdInstalledMethod(
                         getHost().getDistString("DrbdInst.install.text."
                         + getHost().getDrbdInstallMethod()));
        answerPaneSetText(
                    Tools.getString("Dialog.Host.DrbdLinbitInst.Installing"));
        getHost().execCommand("DrbdInst.install;;;DRBD.load",
                          getProgressBar(),
                          new ExecCallback() {
                            public void done(final String ans) {
                               installationDone();
                            }
                            public void doneError(final String ans,
                                                  final int exitCode) {
                                printErrorAndRetry(Tools.getString(
                               "Dialog.Host.DrbdLinbitInst.InstallationFailed"),
                                                   ans,
                                                   exitCode);
                            }
                          },
                          null,  /* ConvertCmdCallback */
                          true,  /* outputVisible */
                          SSH.DEFAULT_COMMAND_TIMEOUT_LONG);
    }

    /**
     * Called after the installation is completed.
     */
    public final void installationDone() {
        nextDialogObject = new CheckInstallation(
                   getPreviousDialog().getPreviousDialog().getPreviousDialog()
                                      .getPreviousDialog().getPreviousDialog(),
                   getHost());
        progressBarDone();
        answerPaneSetText(
               Tools.getString("Dialog.Host.DrbdLinbitInst.InstallationDone"));
        enableComponents(new JComponent[]{buttonClass(backButton())});
        if (Tools.getConfigData().getAutoOptionHost("drbdinst") != null) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    /**
     * Returns the next dialog object.
     */
    public WizardDialog nextDialog() {
        return nextDialogObject;
    }

    /**
     * Returns the title of the dialog defined as
     * Dialog.Host.DrbdLinbitInst.Title in TextResources.
     */
    protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DrbdLinbitInst.Title");
    }

    /**
     * Returns the description of the dialog defined as
     * Dialog.Host.DrbdLinbitInst.Description in TextResources.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.Host.DrbdLinbitInst.Description");
    }

    /**
     * Returns an input pane with progress of the drbd installation.
     */
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(
                     Tools.getString("Dialog.Host.DrbdLinbitInst.Executing")));
        SpringUtilities.makeCompactGrid(pane, 2, 1,  //rows, cols
                                              0, 0,  //initX, initY
                                              0, 0); //xPad, yPad

        return pane;
    }
}
