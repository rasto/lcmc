/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2013, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.drbd.ui.configdialog;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.drbd.domain.DrbdInstallation;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.host.ui.DialogHost;
import lcmc.drbd.ui.resource.VolumeInfo;
import lcmc.common.domain.ConvertCmdCallback;
import lcmc.drbd.service.DRBD;
import lcmc.common.domain.ExecCallback;
import lcmc.common.domain.util.Tools;
import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.Ssh;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

/**
 * An implementation of a dialog where drbd proxy is installed.
 */
@RequiredArgsConstructor
final class ProxyInst extends DialogHost {
    private final Supplier<ProxyCheckInstallation> proxyCheckInstallationProvider;
    private final Application application;

    private WizardDialog nextDialogObject = null;
    private VolumeInfo volumeInfo;
    private WizardDialog origDialog;

    void init(final WizardDialog previousDialog,
              final Host host,
              final VolumeInfo volumeInfo,
              final WizardDialog origDialog,
              final DrbdInstallation drbdInstallation) {
        init(previousDialog, host, drbdInstallation);
        this.volumeInfo = volumeInfo;
        this.origDialog = origDialog;
    }

    void checkAnswer(final String ans, final String installMethod) {
        final ProxyCheckInstallation proxyCheckInstallationDialog = proxyCheckInstallationProvider.get();
        proxyCheckInstallationDialog.init(getPreviousDialog().getPreviousDialog(),
                                          getHost(),
                                          volumeInfo,
                                          origDialog,
                                          getDrbdInstallation());
        nextDialogObject = proxyCheckInstallationDialog;
        DRBD.startProxy(getHost(), Application.RunMode.LIVE);
        progressBarDone();
        answerPaneSetText(Tools.getString("Dialog.Host.ProxyInst.InstOk"));
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
        installProxy();
    }

    private void installProxy() {
        String arch = getHost().getDistString("ProxyInst.install." + getHost().getHostParser().getArch());
        if (arch == null) {
            arch = getHost().getHostParser().getArch();
        }
        final String archString = arch.replaceAll("i686", "i386");

        String installCommand = "ProxyInst.install";
        final String installMethod = getDrbdInstallation().getProxyInstallMethodIndex();
        if (installMethod != null) {
            installCommand = "ProxyInst.install." + installMethod;
        }

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
                                 printErrorAndRetry(Tools.getString("Dialog.Host.ProxyInst.InstError"),
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
        if (nextDialogObject == null) {
            final ProxyCheckInstallation proxyCheckInstallationDialog = proxyCheckInstallationProvider.get();
            proxyCheckInstallationDialog.init(this, getHost(), volumeInfo, origDialog, getDrbdInstallation());
            return proxyCheckInstallationDialog;
        } else {
            return nextDialogObject;
        }
    }

    @Override
    protected void finishDialog() {
        super.finishDialog();
        if (isPressedFinishButton()) {
            if (origDialog != null) {
                nextDialogObject = origDialog;
                setPressedButton(nextButton());
            }
            getHost().getCluster().addProxyHost(getHost());
            if (volumeInfo != null) {
                volumeInfo.getDrbdResourceInfo().resetDrbdResourcePanel();
                volumeInfo.getBrowser().getGlobalInfo().addProxyHostNode(getHost());
            }
        }
    }

    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.ProxyInst.Title");
    }
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.ProxyInst.Description");
    }

    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(Tools.getString("Dialog.Host.ProxyInst.Executing")));
        SpringUtilities.makeCompactGrid(pane, 2, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad

        return pane;
    }

    @Override
    protected WizardDialog dialogAfterCancel() {
        return origDialog;
    }
}
