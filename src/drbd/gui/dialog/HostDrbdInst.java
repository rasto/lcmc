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
import drbd.utilities.ExecCallback;
import drbd.gui.SpringUtilities;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SpringLayout;

/**
 * An implementation of a dialog where drbd will be installed.
 * TODO: rename it to HostDrbdLinbitInst
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HostDrbdInst extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;

    /**
     * Prepares a new <code>HostDrbdInst</code> object.
     */
    public HostDrbdInst(final WizardDialog previousDialog,
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
                                        "Dialog.HostDrbdInst.MkdirError"),
                                                   ans,
                                                   exitCode);
                            }
                          },
                          null,  /* ConvertCmdCallback */
                          true); /* outputVisible */
    }

    /**
     * Checks whether the files have to be downloaded.
     */
    public final void checkFile(final String ans) {
        answerPaneSetText(Tools.getString("Dialog.HostDrbdInst.CheckingFile"));
        getHost().execCommand("DrbdInst.test",
                          getProgressBar(),
                          new ExecCallback() {
                              // TODO: exchange here done and doneError
                              // TODO: treat file exist differently as other
                              // errors.
                              public void done(final String ans) {
                                  answerPaneSetText(Tools.getString(
                                            "Dialog.HostDrbdInst.FileExists"));
                                  installDrbd();
                              }
                              public void doneError(final String ans,
                                                    final int exitCode) {
                                  downloadDrbd();
                              }
                          },
                          null,  /* ConvertCmdCallback */
                          true); /* outputVisible */
    }

    /**
     * Download the drbd packages.
     */
    public final void downloadDrbd() {
        answerPaneSetText(Tools.getString("Dialog.HostDrbdInst.Downloading"));
        getHost().execCommand("DrbdInst.wget",
                          getProgressBar(),
                          new ExecCallback() {
                            public void done(final String ans) {
                               installDrbd();
                            }
                            public void doneError(final String ans,
                                                  final int exitCode) {
                                printErrorAndRetry(Tools.getString(
                                            "Dialog.HostDrbdInst.WgetError"),
                                                   ans,
                                                   exitCode);
                            }
                          },
                          null,  /* ConvertCmdCallback */
                          true); /* outputVisible */
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
        answerPaneSetText(Tools.getString("Dialog.HostDrbdInst.Installing"));
        getHost().execCommand("DrbdInst.install;;;DRBD.load",
                          getProgressBar(),
                          new ExecCallback() {
                            public void done(final String ans) {
                               installationDone();
                            }
                            public void doneError(final String ans,
                                                  final int exitCode) {
                                printErrorAndRetry(Tools.getString(
                                    "Dialog.HostDrbdInst.InstallationFailed"),
                                                   ans,
                                                   exitCode);
                            }
                          },
                          null,  /* ConvertCmdCallback */
                          true); /* outputVisible */
    }

    /**
     * Called after the installation is completed.
     */
    public final void installationDone() {
        nextDialogObject = new HostCheckInstallation(
                   getPreviousDialog().getPreviousDialog().getPreviousDialog()
                                      .getPreviousDialog().getPreviousDialog(),
                   getHost());

        progressBarDone();
        answerPaneSetText(
                    Tools.getString("Dialog.HostDrbdInst.InstallationDone"));
        enableComponents();
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
     * Dialog.HostDrbdInst.Title in TextResources.
     */
    protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.HostDrbdInst.Title");
    }

    /**
     * Returns the description of the dialog defined as
     * Dialog.HostDrbdInst.Description in TextResources.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.HostDrbdInst.Description");
    }

    /**
     * Returns an input pane with progress of the drbd installation.
     */
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(
                            Tools.getString("Dialog.HostDrbdInst.Executing")));
        SpringUtilities.makeCompactGrid(pane, 2, 1,  //rows, cols
                                              1, 1,  //initX, initY
                                              1, 1); //xPad, yPad

        return pane;
    }
}
