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
import lcmc.drbd.domain.DrbdInstallation;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.drbd.ui.resource.GlobalInfo;
import lcmc.common.domain.ConvertCmdCallback;
import lcmc.common.domain.ExecCallback;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;
import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.Ssh;

/**
 * An implementation of a dialog where drbd is installed.
 */
@Named
final class DrbdCommandInst extends DialogHost {
    private static final Logger LOG = LoggerFactory.getLogger(DrbdCommandInst.class);
    @Inject
    private Provider<CheckInstallation> checkInstallationProvider;
    private CheckInstallation checkInstallation;
    @Inject
    private Application application;

    /**
     * Checks the answer of the installation and enables/disables the
     * components accordingly.
     */
    void checkAnswer(final String ans) {
        final ClusterBrowser clusterBrowser = getHost().getBrowser().getClusterBrowser();
        if (clusterBrowser != null) {
            clusterBrowser.getHostDrbdParameters().clear();
            final GlobalInfo globalInfo = clusterBrowser.getGlobalInfo();
            globalInfo.clearPanelLists();
            globalInfo.updateDrbdInfo();
            globalInfo.resetInfoPanel();
            globalInfo.getInfoPanel();
        }
        checkInstallation = checkInstallationProvider.get();
        checkInstallation.init(getPreviousDialog().getPreviousDialog().getPreviousDialog(),
                               getHost(),
                               getDrbdInstallation());
        progressBarDone();
        answerPaneSetText(Tools.getString("Dialog.Host.DrbdCommandInst.InstOk"));
        enableComponents(new JComponent[]{buttonClass(backButton())});
        buttonClass(nextButton()).requestFocus();
        if (application.getAutoOptionHost("drbdinst") != null) {
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
        getProgressBar().start(50000);
        installDrbd();
    }

    private void installDrbd() {
        String arch = getHost().getDistString("DrbdInst.install." + getHost().getHostParser().getArch());
        if (arch == null) {
            arch = getHost().getHostParser().getArch();
        }
        final String archString = arch;
        String installCommand = "DrbdInst.install";
        final DrbdInstallation drbdInstallation = getDrbdInstallation();
        final String installMethod = drbdInstallation.getDrbdInstallMethodIndex();
        if (installMethod != null) {
            installCommand = "DrbdInst.install." + installMethod;
        }
        final String drbdVersion;
        final String drbdVersionUrlString;
        final String utilVersion;
        final String utilFileName;

        final DrbdVersions drbdVersions = drbdInstallation.getDrbdToInstall();
        if (drbdVersions == null) {
            drbdVersion = null;
            drbdVersionUrlString = null;
            utilVersion = null;
            utilFileName = null;
        } else {
            drbdVersion = drbdVersions.getModuleVersion();
            drbdVersionUrlString = drbdVersions.getModuleFileName();
            utilVersion = drbdVersions.getUtilVersion();
            utilFileName = drbdVersions.getUtilFileName();
        }

        application.setLastDrbdInstalledMethod(getHost().getDistString("DrbdInst.install.text." + installMethod));
        LOG.debug1("installDrbd: cmd: " + installCommand
                   + " arch: " + archString
                   + " version: " + drbdVersionUrlString
                   + '/' + drbdVersion);
        getHost().execCommandInBash(new ExecCommandConfig()
                         .commandString(installCommand + ";;;DRBD.load")
                         .progressBar(getProgressBar())
                         .execCallback(new ExecCallback() {
                             @Override
                             public void done(final String answer) {
                                 LOG.debug1("installDrbd: done: " + answer);
                                 checkAnswer(answer);
                             }

                             @Override
                             public void doneError(final String answer, final int errorCode) {
                                 LOG.debug1("installDrbd: done error: " + errorCode + " / " + answer);
                                 printErrorAndRetry(Tools.getString("Dialog.Host.DrbdCommandInst.InstError"),
                                                    answer,
                                                    errorCode);
                             }
                         })
                         .convertCmdCallback(new ConvertCmdCallback() {
                             @Override
                             public String convert(final String command) {
                                 String replaced = command.replaceAll("@ARCH@", archString);
                                 replaced = replaced.replaceAll("@VERSIONSTRING@", drbdVersionUrlString);
                                 replaced = replaced.replaceAll("@VERSION@", drbdVersion);
                                 if (utilFileName != null) {
                                     replaced = replaced.replaceAll("@UTIL-VERSIONSTRING@", utilFileName);
                                 }
                                 if (utilVersion != null) {
                                     replaced = replaced.replaceAll("@UTIL-VERSION@", utilVersion);
                                 }
                                 return replaced;
                             }
                         })
                         .sshCommandTimeout(Ssh.DEFAULT_COMMAND_TIMEOUT_LONG));
    }

    @Override
    public WizardDialog nextDialog() {
        return checkInstallation;
    }

    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DrbdCommandInst.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.DrbdCommandInst.Description");
    }

    /**
     * Returns the input pane with info about the installation progress.
     */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(Tools.getString("Dialog.Host.DrbdCommandInst.Executing")));
        SpringUtilities.makeCompactGrid(pane, 2, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad

        return pane;
    }
}
