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

package lcmc.host.ui;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.common.domain.Application;
import lcmc.gui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.utilities.ConvertCmdCallback;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.Tools;
import lcmc.utilities.ssh.ExecCommandConfig;
import lcmc.utilities.ssh.Ssh;

/**
 * An implementation of a dialog where openais with pacemaker is installed.
 */
@Named
final class PacemakerInst extends DialogHost {
    @Inject
    private Provider<CheckInstallation> checkInstallationProvider;
    private CheckInstallation checkInstallationDialog = null;
    @Inject
    private Application application;

    /**
     * Checks the answer of the installation and enables/disables the
     * components accordingly.
     */
    void checkAnswer(final String ans, final String installMethod) {
        // TODO: check if it really failes
        checkInstallationDialog = checkInstallationProvider.get();
        checkInstallationDialog.init(getPreviousDialog().getPreviousDialog(), getHost(), getDrbdInstallation());
        progressBarDone();
        answerPaneSetText(Tools.getString("Dialog.Host.PacemakerInst.InstOk"));
        enableComponents(new JComponent[]{buttonClass(backButton())});
        buttonClass(nextButton()).requestFocus();
        if (application.getAutoOptionHost("pminst") != null) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    @Override
    protected void initDialogAfterVisible() {
        installPm();
    }

    private void installPm() {
        String arch = getHost().getDistString("PmInst.install." + getHost().getArch());
        if (arch == null) {
            arch = getHost().getArch();
        }
        final String archString = arch.replaceAll("i686", "i386");
        String installCommand = "PmInst.install";
        final String installMethod = getHost().getPacemakerInstallMethodIndex();
        if (installMethod != null) {
            installCommand = "PmInst.install." + installMethod;
            final String filesStr = getHost().getDistString("PmInst.install.files." + installMethod);
            if (filesStr != null) {
                final String[] parts = filesStr.split(":");
                /* install files if specified */
                int i = 0;
                while (i < parts.length) {
                    final String fileName = "/help-progs/" + parts[i];
                    final String to = parts[i + 1];
                    final String perm = parts[i + 2];
                    final String file = Tools.getFile(fileName);
                    if (file != null) {
                        getHost().getSSH().scp(file, to, perm, true, null, null, null);
                    }
                    i += 3;
                }
            }
        }
        application.setLastHbPmInstalledMethod(getHost().getDistString("PmInst.install.text." + installMethod));
        application.setLastInstalledClusterStack(Application.COROSYNC_NAME);

        getHost().execCommandInBash(new ExecCommandConfig()
                         .commandString(installCommand)
                         .progressBar(getProgressBar())
                         .execCallback(new ExecCallback() {
                             @Override
                             public void done(final String answer) {
                                 checkAnswer(answer, installMethod);
                             }
                             @Override
                             public void doneError(final String answer, final int errorCode) {
                                 printErrorAndRetry(Tools.getString("Dialog.Host.PacemakerInst.InstError"),
                                                    answer,
                                                    errorCode);
                             }
                         })
                         .convertCmdCallback(new ConvertCmdCallback() {
                             @Override
                             public String convert(final String command) {
                                 return command.replaceAll("@ARCH@", archString);
                             }
                         })
                         .sshCommandTimeout(Ssh.DEFAULT_COMMAND_TIMEOUT_LONG));
    }

    @Override
    public WizardDialog nextDialog() {
        return checkInstallationDialog;
    }

    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.PacemakerInst.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.PacemakerInst.Description");
    }

    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(Tools.getString("Dialog.Host.PacemakerInst.Executing")));
        SpringUtilities.makeCompactGrid(pane, 2, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad

        return pane;
    }
}
