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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.gui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.common.domain.Application;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.ssh.ExecCommandConfig;
import lcmc.utilities.ssh.ExecCommandThread;
import lcmc.utilities.Tools;

/**
 * An implementation of a dialog where hardware information is collected.
 */
@Named
public class Devices extends DialogHost {
    @Inject
    private DistDetection distDetection;
    @Inject
    private Application application;

    /** Checks the answer and makes it visible to the user. */
    final void checkAnswer(final String ans) {
        enableComponents();
        final List<String> incorrect = new ArrayList<String>();
        final List<String> changed = new ArrayList<String>();
        if (ans != null && ans.isEmpty() || "\n".equals(ans)) {
            progressBarDoneError();
            final String error = Tools.getString("Dialog.Host.Devices.CheckError");
            answerPaneSetTextError(error);
            incorrect.add(error);
        } else {
            getHost().parseHostInfo(ans);
            progressBarDone();
            answerPaneSetText(ans);
            buttonClass(nextButton()).requestFocus();
        }
        enableNextButtons(incorrect, changed);
        if (!application.getAutoHosts().isEmpty()) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
    }

    @Override
    protected final void initDialogAfterVisible() {
        makeDefaultAndRequestFocus(buttonClass(nextButton()));
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                getHost().getSSH().installGuiHelper();
                getAllInfo();
            }
        });
        t.start();
    }

    protected final void getAllInfo() {
        final ExecCommandThread t = getHost().execCommand(
                         new ExecCommandConfig().commandString("GetHostAllInfo")
                         .progressBar(getProgressBar())
                         .execCallback(new ExecCallback() {
                             @Override
                             public void done(final String answer) {
                                 checkAnswer(answer);
                             }

                             @Override
                             public void doneError(final String answer, final int errorCode) {
                                 printErrorAndRetry(Tools.getString("Dialog.Host.Devices.CheckError"),
                                                    answer,
                                                    errorCode);
                             }
                         })
                         .silentCommand()
                         .silentOutput());
        setCommandThread(t);
    }

    @Override
    public WizardDialog nextDialog() {
        distDetection.init(this, getHost(), getDrbdInstallation());
        return distDetection;
    }

    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.Devices.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.Devices.Description");
    }

    @Override
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(Tools.getString("Dialog.Host.Devices.Executing")));
        SpringUtilities.makeCompactGrid(pane, 2, 1, 0, 0, 0, 0);

        return pane;
    }
}
