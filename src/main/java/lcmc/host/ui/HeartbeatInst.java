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

import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.cluster.infrastructure.ssh.ExecCommandConfig;
import lcmc.cluster.infrastructure.ssh.Ssh;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.domain.ExecCallback;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;

/**
 * An implementation of a dialog where heartbeat is installed.
 */
@Named
final class HeartbeatInst extends DialogHost {
    private CheckInstallation checkInstallationDialog;
    private final Provider<CheckInstallation> checkInstallationFactory;
    private final Application application;

    public HeartbeatInst(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData,
            Provider<CheckInstallation> checkInstallationFactory, Provider<ProgressBar> progressBarProvider) {
        super(application, swingUtils, widgetFactory, mainData, progressBarProvider);
        this.checkInstallationFactory = checkInstallationFactory;
        this.application = application;
    }

    /**
     * Checks the answer of the installation and enables/disables the components accordingly.
     */
    void checkAnswer(final String ans, final String installMethod) {
        // TODO: check if it really failes
        checkInstallationDialog = checkInstallationFactory.get();
        checkInstallationDialog.init(getPreviousDialog().getPreviousDialog(), getHost(), getDrbdInstallation());
        progressBarDone();
        answerPaneSetText(Tools.getString("Dialog.Host.HeartbeatInst.InstOk"));
        enableComponents(new JComponent[]{buttonClass(backButton())});
        buttonClass(nextButton()).requestFocus();
        if (application.getAutoOptionHost("hbinst") != null) {
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
        installHeartbeat();
    }

    private void installHeartbeat() {
        String arch = getHost().getDistString("HbPmInst.install." + getHost().getHostParser().getArch());
        if (arch == null) {
            arch = getHost().getHostParser().getArch();
        }
        final String archString = arch.replaceAll("i686", "i386");

        String installCommand = "HbPmInst.install";
        final String installMethod = getHost().getHeartbeatPacemakerInstallMethodIndex();
        if (installMethod != null) {
            installCommand = "HbPmInst.install." + installMethod;
        }
        application.setLastHbPmInstalledMethod(getHost().getDistString("HbPmInst.install.text." + installMethod));
        application.setLastInstalledClusterStack(Application.HEARTBEAT_NAME);

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
                        printErrorAndRetry(Tools.getString("Dialog.Host.HeartbeatInst.InstError"), answer, errorCode);
                    }
                }).convertCmdCallback(command -> command.replaceAll("@ARCH@", archString))
                .sshCommandTimeout(Ssh.DEFAULT_COMMAND_TIMEOUT_LONG));
    }

    @Override
    public WizardDialog nextDialog() {
        return checkInstallationDialog;
    }

    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.HeartbeatInst.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.HeartbeatInst.Description");
    }

    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(Tools.getString("Dialog.Host.HeartbeatInst.Executing")));
        SpringUtilities.makeCompactGrid(pane, 2, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad

        return pane;
    }
}
