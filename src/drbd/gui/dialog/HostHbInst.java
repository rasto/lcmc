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
import drbd.gui.SpringUtilities;
import drbd.utilities.ExecCallback;
import drbd.utilities.ConvertCmdCallback;

import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JComponent;

/**
 * An implementation of a dialog where heartbeat is installed.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HostHbInst extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;

    /**
     * Prepares a new <code>HostHbInst</code> object.
     */
    public HostHbInst(final WizardDialog previousDialog,
                      final Host host) {
        super(previousDialog, host);
    }

    /**
     * Checks the answer of the installation and enables/disables the
     * components accordingly.
     */
    public final void checkAnswer(final String ans,
                                  final String installMethod) {
        // TODO: check if it really failes
        nextDialogObject = new HostCheckInstallation(
                                        getPreviousDialog().getPreviousDialog(),
                                        getHost());
        progressBarDone();
        answerPaneSetText(Tools.getString("Dialog.HostHbInst.InstOk"));
        enableComponents();
        buttonClass(nextButton()).requestFocus();
        if (Tools.getConfigData().getAutoOptionHost("hbinst") != null) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    /**
     * Inits the dialog and starts the installation procedure.
     */
    protected final void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        installHeartbeat();
    }

    /**
     * Installs the heartbeat.
     */
    private void installHeartbeat() {
        String arch = getHost().getDistString("HbPmInst.install."
                                              + getHost().getArch());
        if (arch == null) {
            arch = getHost().getArch();
        }
        final String archString = arch.replaceAll("i686", "i386");

        String installCommand = "HbPmInst.install";
        final String installMethod = getHost().getHbPmInstallMethod();
        if (installMethod != null) {
            installCommand = "HbPmInst.install." + installMethod;
        }
        Tools.getConfigData().setLastHbPmInstalledMethod(
            getHost().getDistString("HbPmInst.install.text." + installMethod));
        Tools.getConfigData().setLastInstalledClusterStack(
                                        Tools.getConfigData().HEARTBEAT_NAME);

        getHost().execCommand(installCommand,
                         getProgressBar(),
                         new ExecCallback() {
                             public void done(final String ans) {
                                 checkAnswer(ans, installMethod);
                             }
                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 printErrorAndRetry(Tools.getString(
                                                "Dialog.HostHbInst.InstError"),
                                                    ans,
                                                    exitCode);
                             }
                         },
                         new ConvertCmdCallback() {
                             public final String convert(final String command) {
                                 return command.replaceAll("@ARCH@",
                                                           archString);
                             }
                         },
                         true);
    }

    /**
     * Returns the next dialog.
     */
    public final WizardDialog nextDialog() {
        return nextDialogObject;
    }

    /**
     * Returns the description of the dialog defined as
     * Dialog.HostHbInst.Description in TextResources.
     */
    protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.HostHbInst.Title");
    }

    /**
     * Returns the description of the dialog defined as
     * Dialog.HostHbInst.Description in TextResources.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.HostHbInst.Description");
    }

    /**
     * Returns the input pane with info about the installation progress.
     */
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(Tools.getString("Dialog.HostHbInst.Executing")));
        SpringUtilities.makeCompactGrid(pane, 2, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad

        return pane;
    }
}
