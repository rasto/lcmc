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
import drbd.gui.SpringUtilities;
import drbd.utilities.Tools;
import drbd.utilities.ExecCallback;
import drbd.gui.GuiComboBox;
import drbd.utilities.SSH.ExecCommandThread;

import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JComponent;

import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.Box;

/**
 * An implementation of a dialog where user can choose a distribution of the
 * host.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HostDistDetection extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** No match string. */
    private static final String NO_MATCH_STRING = "No Match";
    /** Newline. */
    private static final String NEWLINE = "\\r?\\n";
    /** Height of the choice boxes. */
    private static final int CHOICE_BOX_HEIGHT = 30;
    /** Combo box with detectable distributions. */
    private GuiComboBox distCombo = null;

    /**
     * Prepares a new <code>HostDistDetection</code> object.
     */
    public HostDistDetection(final WizardDialog previousDialog,
                             final Host host) {
        super(previousDialog, host);
    }

    /**
     * checks answer from host about distribution and sets answer text in
     * answerLabel.
     *
     * answer comes as lines of text with one token per line.
     * tokens:
     *
     * 0: kernel name    : Linux
     * 1: kernel version : 2.6.15-1-1-p3-smp-highmem
     * 2: arch           : i686
     * 3: dist:          : debian
     * 4: dist version   : 3.1
     *
     * @param ans
     *          answer from host.
     */
    public void checkAnswer(final String ans) {
        final String[] result = ans.split(NEWLINE);
        String answerText = "";

        getHost().setDistInfo(result);
        if (result.length < 1) {
            answerText = "HostDistDetection.NoInfoAvailable";
            answerPaneSetText(answerText);
        } else if (getHost().getKernelName().equals("Linux")) {
            final String support =
                            Tools.getCommand("Support",
                                             getHost().getDist(),
                                             getHost().getDistVersionString());
            answerText = getHost().getDist() + "\nversion: "
                         + getHost().getDistVersion() + " (support file: "
                         + support + ")";
            buttonClass(nextButton()).requestFocus();
            answerPaneSetText(answerText);
        } else {
            answerText = getHost().getKernelName() + " "
                         + Tools.getString("Dialog.HostDistDetection.NotALinux");
            answerPaneSetText(answerText);
        }
        allDone("");
    }

    /**
     * Is called after all is done.
     */
    protected void allDone(final String ans) {
        progressBarDone();

        enableComponents();
        final String support =
                         Tools.getCommand("Support",
                                          getHost().getDist(),
                                          getHost().getDistVersionString());
        final String answerText = "detected: " + getHost().getDetectedInfo()
                                  + "\n" + getHost().getDist()
                                  + "\nversion: " + getHost().getDistVersion()
                                  + " (support file: " + support + ")";
        //if (ans == null) {
        //    answerPaneSetText(answerText + "\n"
        //                      + Tools.getString(
        //                               "Dialog.HostDistDetection.DownloadNotAvailable"));
        //} else {
        //    final String[] versions = ans.split(NEWLINE);
        //    getHost().setAvailableDrbdVersions(versions);
        //    answerPaneSetText(answerText + "\n"
        //                      + Tools.getString("Dialog.HostDistDetection.AvailVersions")
        //                      + " " + Tools.join(", ", versions));
        //}
    }

    /**
     * Inits dialog and starts the distribution detection.
     */
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});

        final Thread thread = new Thread(
            new Runnable() {
                public void run() {
                    disableComponents();
                    getProgressBar().start(4000);
                    //getProgressBar().hold();
                    ExecCommandThread t = getHost().execCommandCache(
                             "WhichDist",
                             null,
                             new ExecCallback() {
                                public void done(final String ans) {
                                    checkAnswer(ans);
                                }
                                public void doneError(final String ans,
                                                      final int exitCode) {
                                    printErrorAndRetry(Tools.getString(
                                            "Dialog.HostDistDetection.NoDist"),
                                                       ans,
                                                       exitCode);
                                }
                             }, true);
                    setCommandThread(t);
                }
            });
        thread.start();
    }

    /**
     * Returns the next dialog which is HostCheckInstallation.
     */
    public WizardDialog nextDialog() {
        return new HostCheckInstallation(this, getHost());
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.HostDistDetection.Title in TextResources.
     */
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.HostDistDetection.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.HostDistDetection.Description in TextResources.
     */
    protected String getDescription() {
        return Tools.getString("Dialog.HostDistDetection.Description");
    }

    /** 
     * Returns the pane with combo box with available distributions.
     */
    protected JPanel getComboBox() {
        // TODO: not implemented
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
        final int maxX = (int) pane.getMaximumSize().getWidth();
        pane.setMaximumSize(new Dimension(maxX, CHOICE_BOX_HEIGHT));

        distCombo = new GuiComboBox(null,
                                    null,
                                    GuiComboBox.Type.COMBOBOX,
                                    null,
                                    0);

        distCombo.setEnabled(false);
        pane.add(distCombo);
        pane.add(Box.createHorizontalGlue());
        pane.add(Box.createRigidArea(new Dimension(800, 0)));
        return pane;
    }

    /**
     * Returns the input pane with check boxes and other info.
     */
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getComboBox());
        pane.add(getAnswerPane(Tools.getString("Dialog.HostDistDetection.Executing")));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }
}
