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
import drbd.gui.ProgressBar;
import drbd.utilities.ExecCallback;
import drbd.utilities.SSH.ExecCommandThread;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SpringLayout;

/**
 * An implementation of a dialog where hardware information is collected.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HostDevices extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Prepares a new <code>HostDevices</code> object.
     */
    public HostDevices(final WizardDialog previousDialog,
                       final Host host) {
        super(previousDialog, host);
    }

    /**
     * Checks the answer and makes it visible to the user. */
    public void checkAnswer(final String ans) {
        if ("".equals(ans) || "\n".equals(ans)) {
            progressBarDoneError();
            answerPaneSetTextError(Tools.getString("Dialog.HostDevices.Error"));
            enableComponents();
            buttonClass(nextButton()).requestFocus();
        } else {
            getHost().parseHostInfo(ans);
            progressBarDone();
            answerPaneSetText(ans);
            enableComponents();
            buttonClass(nextButton()).requestFocus();
        }
    }

    /**
     * Inits the dialog and starts the info collecting thread.
     */
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});

        final Thread thread = new Thread(
            new Runnable() {
                public void run() {
                    getHost().removeNetInterfaces();
                    getHost().removeBlockDevices();
                    getProgressBar().start(6000);
                    final ExecCommandThread t = getHost().execCommand("installGuiHelper",
                                     (ProgressBar)null, //getProgressBar(),
                                     new ExecCallback() {
                                         public void done(final String ans) {
                                             getInfo();
                                         }
                                         public void doneError(final String ans,
                                                               final int exitCode) {
                                             /* in case of error, the next command will
                                                find out, so it's not checked here. Gui
                                                Helper can be installed anyway. */
                                             getInfo();
                                         }
                                     }, false);
                    setCommandThread(t);
                }
            });
        thread.start();
    }

    /**
     * Returns info for input pane.
     */
    protected void getInfo() {
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});

        getHost().removeNetInterfaces();
        final ExecCommandThread t = getHost().execCommand("GetHostInfo",
                         (ProgressBar)null, //getProgressBar(),
                         new ExecCallback() {
                             public void done(final String ans) {
                                 checkAnswer(ans);
                             }

                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 printErrorAndRetry(Tools.getString(
                                            "Dialog.HostDevices.CheckError"),
                                                    ans,
                                                    exitCode);
                             }
                         }, true);
        setCommandThread(t);
    }

    /**
     * Returns the next dialog object.
     */
    public WizardDialog nextDialog() {
        return new HostDistDetection(this, getHost());
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.HostDevices.Title in TextResources.
     */
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.HostDevices.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.HostDevices.Description in TextResources.
     */
    protected String getDescription() {
        return Tools.getString("Dialog.HostDevices.Description");
    }

    /**
     * Returns pane where collected info is displayed.
     */
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(
                            Tools.getString("Dialog.HostDevices.Executing")));
        SpringUtilities.makeCompactGrid(pane, 2, 1,  //rows, cols
                                              1, 1,  //initX, initY
                                              1, 1); //xPad, yPad

        return pane;
    }
}
