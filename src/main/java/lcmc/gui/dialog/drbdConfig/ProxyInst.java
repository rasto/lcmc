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

package lcmc.gui.dialog.drbdConfig;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.data.Application;
import lcmc.data.Host;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.dialog.host.DialogHost;
import lcmc.gui.resources.drbd.VolumeInfo;
import lcmc.utilities.ConvertCmdCallback;
import lcmc.utilities.DRBD;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.Tools;
import lcmc.utilities.ssh.SSH;

/**
 * An implementation of a dialog where drbd proxy is installed.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class ProxyInst extends DialogHost {
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;
    /** Drbd volume info. */
    private final VolumeInfo volumeInfo;
    /** The dialog we came from. */
    private final WizardDialog origDialog;

    /** Prepares a new {@code ProxyInst} object. */
    ProxyInst(final WizardDialog previousDialog,
              final Host host,
              final VolumeInfo volumeInfo,
              final WizardDialog origDialog) {
        super(previousDialog, host);
        this.volumeInfo = volumeInfo;
        this.origDialog = origDialog;
    }

    /**
     * Checks the answer of the installation and enables/disables the
     * components accordingly.
     */
    void checkAnswer(final String ans, final String installMethod) {
        nextDialogObject = new ProxyCheckInstallation(
                                        getPreviousDialog().getPreviousDialog(),
                                        getHost(),
                                        volumeInfo,
                                        origDialog);
        DRBD.startProxy(getHost(), Application.RunMode.LIVE);
        progressBarDone();
        answerPaneSetText(Tools.getString("Dialog.Host.ProxyInst.InstOk"));
        enableComponents(new JComponent[]{buttonClass(backButton())});
        buttonClass(nextButton()).requestFocus();
        if (Tools.getApplication().getAutoOptionHost("hbinst") != null) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    /** Inits the dialog and starts the installation procedure. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        installProxy();
    }

    /** Installs the proxy. */
    private void installProxy() {
        String arch = getHost().getDistString("ProxyInst.install."
                                              + getHost().getArch());
        if (arch == null) {
            arch = getHost().getArch();
        }
        final String archString = arch.replaceAll("i686", "i386");

        String installCommand = "ProxyInst.install";
        final String installMethod = getHost().getProxyInstallMethod();
        if (installMethod != null) {
            installCommand = "ProxyInst.install." + installMethod;
        }

        getHost().execCommandInBash(
                         installCommand,
                         getProgressBar(),
                         new ExecCallback() {
                             @Override
                             public void done(final String answer) {
                                checkAnswer(answer, installMethod);
                             }
                             @Override
                             public void doneError(final String answer,
                                                   final int errorCode) {
                                 printErrorAndRetry(Tools.getString(
                                         "Dialog.Host.ProxyInst.InstError"),
                                         answer,
                                         errorCode
                                 );
                             }
                         },
                         new ConvertCmdCallback() {
                             @Override
                             public String convert(final String command) {
                                 return command.replaceAll("@ARCH@",
                                                           archString);
                             }
                         },
                         true,
                         SSH.DEFAULT_COMMAND_TIMEOUT_LONG);
    }

    /** Returns the next dialog. */
    @Override
    public WizardDialog nextDialog() {
        if (nextDialogObject == null) {
            return new ProxyCheckInstallation(this,
                                              getHost(),
                                              volumeInfo,
                                              origDialog);
        } else {
            return nextDialogObject;
        }
    }

    /** Finish dialog. */
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
                volumeInfo.getDrbdInfo().addProxyHostNode(getHost());
            }
        }
    }

    /**
     * Returns the description of the dialog defined as
     * Dialog.Host.ProxyInst.Description in TextResources.
     */
    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.ProxyInst.Title");
    }

    /**
     * Returns the description of the dialog defined as
     * Dialog.Host.ProxyInst.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.ProxyInst.Description");
    }

    /** Returns the input pane with info about the installation progress. */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(
                     Tools.getString("Dialog.Host.ProxyInst.Executing")));
        SpringUtilities.makeCompactGrid(pane, 2, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad

        return pane;
    }

    /**
     * Return dialog that comes after "cancel" button was pressed.
     */
    @Override
    protected WizardDialog dialogAfterCancel() {
        return origDialog;
    }
}
