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

import lcmc.model.drbd.DrbdInstallation;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.resources.drbd.GlobalInfo;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.Tools;
import lcmc.utilities.ssh.ExecCommandConfig;
import lcmc.utilities.ssh.Ssh;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An implementation of a dialog where drbd will be installed.
 */
@Component
public class DrbdLinbitInst extends DialogHost {
    @Autowired
    private CheckInstallation checkInstallationDialog;

    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    @Override
    protected void initDialogAfterVisible() {
        getProgressBar().start(50000);

        getHost().execCommand(new ExecCommandConfig().commandString("DrbdInst.mkdir")
                          .progressBar(getProgressBar())
                          .execCallback(new ExecCallback() {
                              @Override
                              public void done(final String answer) {
                                 checkFile(answer);
                              }
                              @Override
                              public void doneError(final String answer, final int errorCode) {
                                  printErrorAndRetry(Tools.getString("Dialog.Host.DrbdLinbitInst.MkdirError"),
                                                     answer,
                                                     errorCode);
                              }
                          }));
    }

    /** Checks whether the files have to be downloaded. */
    final void checkFile(final String ans) {
        answerPaneSetText(Tools.getString("Dialog.Host.DrbdLinbitInst.CheckingFile"));
        getHost().execCommand(new ExecCommandConfig().commandString("DrbdInst.test")
                          .progressBar(getProgressBar())
                          .execCallback(new ExecCallback() {
                              // TODO: exchange here done and doneError
                              // TODO: treat file exist differently as other
                              // errors.
                              @Override
                              public void done(final String answer) {
                                  answerPaneSetText(Tools.getString("Dialog.Host.DrbdLinbitInst.FileExists"));
                                  installDrbd();
                              }
                              @Override
                              public void doneError(final String answer, final int errorCode) {
                                  downloadDrbd();
                              }
                          }));
    }

    final void downloadDrbd() {
        answerPaneSetText(Tools.getString("Dialog.Host.DrbdLinbitInst.Downloading"));
        getHost().execCommand(new ExecCommandConfig().commandString("DrbdInst.wget")
                          .progressBar(getProgressBar())
                          .execCallback(new ExecCallback() {
                               @Override
                               public void done(final String answer) {
                                  installDrbd();
                               }
                               @Override
                               public void doneError(final String answer, final int errorCode) {
                                   printErrorAndRetry(Tools.getString("Dialog.Host.DrbdLinbitInst.WgetError"),
                                                      answer,
                                                      errorCode);
                               }
                          }));
    }

    final void installDrbd() {
        final DrbdInstallation drbdInstallation = getDrbdInstallation();
        drbdInstallation.setDrbdWasNewlyInstalled(true); /* even if we fail */
        Tools.getApplication().setLastDrbdInstalledMethod(drbdInstallation.getDrbdInstallMethodIndex());
        Tools.getApplication().setLastDrbdInstalledMethod(
                           getHost().getDistString("DrbdInst.install.text."
                                                   + drbdInstallation.getDrbdInstallMethodIndex()));
        answerPaneSetText(Tools.getString("Dialog.Host.DrbdLinbitInst.Installing"));
        getHost().execCommandInBash(new ExecCommandConfig().commandString("DrbdInst.install;;;DRBD.load")
                          .progressBar(getProgressBar())
                          .execCallback(new ExecCallback() {
                               @Override
                               public void done(final String answer) {
                                  installationDone();
                               }
                               @Override
                               public void doneError(final String answer, final int errorCode) {
                                   printErrorAndRetry(Tools.getString("Dialog.Host.DrbdLinbitInst.InstallationFailed"),
                                                      answer,
                                                      errorCode);
                               }
                          })
                          .sshCommandTimeout(Ssh.DEFAULT_COMMAND_TIMEOUT_LONG));
    }

    final void installationDone() {
        final ClusterBrowser clusterBrowser = getHost().getBrowser().getClusterBrowser();
        if (clusterBrowser != null) {
            clusterBrowser.getHostDrbdParameters().clear();
            final GlobalInfo globalInfo = clusterBrowser.getDrbdGraph().getDrbdInfo();
            globalInfo.clearPanelLists();
            globalInfo.updateDrbdInfo();
            globalInfo.resetInfoPanel();
            globalInfo.getInfoPanel();
        }
        checkInstallationDialog.init(getPreviousDialog().getPreviousDialog().getPreviousDialog().getPreviousDialog().getPreviousDialog(),
                                     getHost(),
                                     getDrbdInstallation());
        progressBarDone();
        answerPaneSetText(Tools.getString("Dialog.Host.DrbdLinbitInst.InstallationDone"));
        enableComponents(new JComponent[]{buttonClass(backButton())});
        if (Tools.getApplication().getAutoOptionHost("drbdinst") != null) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    @Override
    public WizardDialog nextDialog() {
        return checkInstallationDialog;
    }

    @Override
    protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DrbdLinbitInst.Title");
    }

    @Override
    protected final String getDescription() {
        return Tools.getString("Dialog.Host.DrbdLinbitInst.Description");
    }

    @Override
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(Tools.getString("Dialog.Host.DrbdLinbitInst.Executing")));
        SpringUtilities.makeCompactGrid(pane, 2, 1,  //rows, cols
                                              0, 0,  //initX, initY
                                              0, 0); //xPad, yPad

        return pane;
    }
}
