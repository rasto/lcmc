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

package lcmc.gui.dialog.host;

import lcmc.data.Host;
import lcmc.utilities.Tools;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.WizardDialog;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.SSH;
import lcmc.utilities.ConvertCmdCallback;

import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JComponent;

/**
 * An implementation of a dialog where drbd is installed.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class DrbdCommandInst extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;

    /** Prepares a new <code>DrbdCommandInst</code> object. */
    DrbdCommandInst(final WizardDialog previousDialog, final Host host) {
        super(previousDialog, host);
    }

    /**
     * Checks the answer of the installation and enables/disables the
     * components accordingly.
     */
    void checkAnswer(final String ans) {
        nextDialogObject = new CheckInstallation(
                   getPreviousDialog().getPreviousDialog().getPreviousDialog(),
                   getHost());
        progressBarDone();
        answerPaneSetText(
                    Tools.getString("Dialog.Host.DrbdCommandInst.InstOk"));
        enableComponents(new JComponent[]{buttonClass(backButton())});
        buttonClass(nextButton()).requestFocus();
        if (Tools.getConfigData().getAutoOptionHost("drbdinst") != null) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    /** Inits the dialog and starts the installation procedure. */
    @Override
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        getProgressBar().start(50000);
        installDrbd();
    }

    /** Installs the drbd. */
    private void installDrbd() {
        String arch = getHost().getDistString("DrbdInst.install."
                                              + getHost().getArch());
        if (arch == null) {
            arch = getHost().getArch();
        }
        final String archString = arch;
        String installCommand = "DrbdInst.install";
        final String installMethod = getHost().getDrbdInstallMethod();
        if (installMethod != null) {
            installCommand = "DrbdInst.install." + installMethod;
        }
        final String drbdVersion = getHost().getDrbdVersionToInstall();
        final String drbdVersionUrlString =
                              getHost().getDrbdVersionUrlStringToInstall();
        Tools.getConfigData().setLastDrbdInstalledMethod(
            getHost().getDistString("DrbdInst.install.text." + installMethod));
        getHost().execCommandInBash(
                         installCommand + ";;;DRBD.load",
                         getProgressBar(),
                         new ExecCallback() {
                             @Override
                             public void done(final String ans) {
                                 checkAnswer(ans);
                             }
                             @Override
                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 printErrorAndRetry(
                                    Tools.getString(
                                      "Dialog.Host.DrbdCommandInst.InstError"),
                                    ans,
                                    exitCode);
                             }
                         },
                         new ConvertCmdCallback() {
                             @Override
                             public String convert(final String command) {
                                 return command.replaceAll("@ARCH@",
                                                           archString)
                                               .replaceAll(
                                                          "@VERSIONSTRING@",
                                                          drbdVersionUrlString)
                                               .replaceAll(
                                                        "@VERSION@",
                                                        drbdVersion);
                             }
                         },
                         true,
                         SSH.DEFAULT_COMMAND_TIMEOUT_LONG);
    }

    /** Returns the next dialog. */
    @Override
    public WizardDialog nextDialog() {
        return nextDialogObject;
    }

    /**
     * Returns the description of the dialog defined as
     * Dialog.Host.DrbdCommandInst.Description in TextResources.
     */
    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DrbdCommandInst.Title");
    }

    /**
     * Returns the description of the dialog defined as
     * Dialog.Host.DrbdCommandInst.Description in TextResources.
     */
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
        pane.add(getAnswerPane(
                    Tools.getString("Dialog.Host.DrbdCommandInst.Executing")));
        SpringUtilities.makeCompactGrid(pane, 2, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad

        return pane;
    }
}
