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


package lcmc.gui.dialog.host;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import lcmc.data.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.resources.drbd.GlobalInfo;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.SSH;
import lcmc.utilities.Tools;

/**
 * An implementation of a dialog where drbd will be installed.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class DrbdLinbitInst extends DialogHost {
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;

    /** Prepares a new {@code DrbdLinbitInst} object. */
    public DrbdLinbitInst(final WizardDialog previousDialog, final Host host) {
        super(previousDialog, host);
    }

    /** Inits dialog and starts the drbd install procedure. */
    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        getProgressBar().start(50000);

        getHost().execCommand("DrbdInst.mkdir",
                          getProgressBar(),
                          new ExecCallback() {
                            @Override
                            public void done(final String answer) {
                               checkFile(answer);
                            }
                            @Override
                            public void doneError(final String answer,
                                                  final int errorCode) {
                                printErrorAndRetry(Tools.getString(
                                      "Dialog.Host.DrbdLinbitInst.MkdirError"),
                                        answer,
                                        errorCode);
                            }
                          },
                          null,  /* ConvertCmdCallback */
                          true,  /* outputVisible */
                          SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /** Checks whether the files have to be downloaded. */
    final void checkFile(final String ans) {
        answerPaneSetText(
                   Tools.getString("Dialog.Host.DrbdLinbitInst.CheckingFile"));
        getHost().execCommand("DrbdInst.test",
                          getProgressBar(),
                          new ExecCallback() {
                              // TODO: exchange here done and doneError
                              // TODO: treat file exist differently as other
                              // errors.
                              @Override
                              public void done(final String answer) {
                                  answerPaneSetText(Tools.getString(
                                     "Dialog.Host.DrbdLinbitInst.FileExists"));
                                  installDrbd();
                              }
                              @Override
                              public void doneError(final String answer,
                                                    final int errorCode) {
                                  downloadDrbd();
                              }
                          },
                          null,  /* ConvertCmdCallback */
                          true,  /* outputVisible */
                          SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /** Download the drbd packages. */
    final void downloadDrbd() {
        answerPaneSetText(
                    Tools.getString("Dialog.Host.DrbdLinbitInst.Downloading"));
        getHost().execCommand("DrbdInst.wget",
                          getProgressBar(),
                          new ExecCallback() {
                            @Override
                            public void done(final String answer) {
                               installDrbd();
                            }
                            @Override
                            public void doneError(final String answer,
                                                  final int errorCode) {
                                printErrorAndRetry(Tools.getString(
                                        "Dialog.Host.DrbdLinbitInst.WgetError"),
                                        answer,
                                        errorCode);
                            }
                          },
                          null,  /* ConvertCmdCallback */
                          true,  /* outputVisible */
                          SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /** Install the drbd packages. */
    final void installDrbd() {
        getHost().setDrbdWasInstalled(true); /* even if we fail */
        Tools.getApplication().setLastDrbdInstalledMethod(
                                            getHost().getDrbdInstallMethod());
        Tools.getApplication().setLastDrbdInstalledMethod(
                         getHost().getDistString("DrbdInst.install.text."
                         + getHost().getDrbdInstallMethod()));
        answerPaneSetText(
                    Tools.getString("Dialog.Host.DrbdLinbitInst.Installing"));
        getHost().execCommandInBash("DrbdInst.install;;;DRBD.load",
                          getProgressBar(),
                          new ExecCallback() {
                            @Override
                            public void done(final String answer) {
                               installationDone();
                            }
                            @Override
                            public void doneError(final String answer,
                                                  final int errorCode) {
                                printErrorAndRetry(Tools.getString(
                               "Dialog.Host.DrbdLinbitInst.InstallationFailed"),
                                        answer,
                                        errorCode);
                            }
                          },
                          null,  /* ConvertCmdCallback */
                          true,  /* outputVisible */
                          SSH.DEFAULT_COMMAND_TIMEOUT_LONG);
    }

    /** Called after the installation is completed. */
    final void installationDone() {
        final ClusterBrowser clusterBrowser =
                                   getHost().getBrowser().getClusterBrowser();
        if (clusterBrowser != null) {
            clusterBrowser.getDrbdParameters().clear();
            final GlobalInfo globalInfo =
                                  clusterBrowser.getDrbdGraph().getDrbdInfo();
            globalInfo.clearPanelLists();
            globalInfo.updateDrbdInfo();
            globalInfo.resetInfoPanel();
            globalInfo.getInfoPanel();
        }
        nextDialogObject = new CheckInstallation(
                   getPreviousDialog().getPreviousDialog().getPreviousDialog()
                                      .getPreviousDialog().getPreviousDialog(),
                   getHost());
        progressBarDone();
        answerPaneSetText(
               Tools.getString("Dialog.Host.DrbdLinbitInst.InstallationDone"));
        enableComponents(new JComponent[]{buttonClass(backButton())});
        if (Tools.getApplication().getAutoOptionHost("drbdinst") != null) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    /** Returns the next dialog object. */
    @Override
    public WizardDialog nextDialog() {
        return nextDialogObject;
    }

    /**
     * Returns the title of the dialog defined as
     * Dialog.Host.DrbdLinbitInst.Title in TextResources.
     */
    @Override
    protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DrbdLinbitInst.Title");
    }

    /**
     * Returns the description of the dialog defined as
     * Dialog.Host.DrbdLinbitInst.Description in TextResources.
     */
    @Override
    protected final String getDescription() {
        return Tools.getString("Dialog.Host.DrbdLinbitInst.Description");
    }

    /** Returns an input pane with progress of the drbd installation. */
    @Override
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
