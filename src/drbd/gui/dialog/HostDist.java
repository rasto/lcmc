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
import javax.swing.SwingUtilities;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
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
public class HostDist extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Combo box with distributions. */
    private GuiComboBox drbdDistCombo = null;
    /** Combo box with available kernel versions for this distribution. */
    private GuiComboBox drbdKernelDirCombo = null;
    /** Combo box with available architectures versions for this distribution.
     */
    private GuiComboBox drbdArchCombo = null;
    /** Whether the listeners where already added to the combo boxes. */
    private boolean listenersAdded = false;
    /** No match string. */
    private static final String NO_MATCH_STRING = "No Match";
    /** Newline. */
    private static final String NEWLINE = "\\r?\\n";
    /** Height of the choice boxes. */
    private static final int CHOICE_BOX_HEIGHT = 30;

    /**
     * Prepares a new <code>HostDist</code> object.
     */
    public HostDist(final WizardDialog previousDialog, final Host host) {
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
            answerText = "HostDist.NoInfoAvailable";
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
                         + Tools.getString("Dialog.HostDist.NotALinux");
            answerPaneSetText(answerText);
        }
        availVersions();
    }

    /**
     * Checks the available drbd verisions.
     */
    protected void availVersions() {
        /* get drbd available versions,
         * they are independent from distribution and kernel version and
         * are first directory part in the download area.*/
        drbdDistCombo.setEnabled(false);
        drbdKernelDirCombo.setEnabled(false);
        drbdArchCombo.setEnabled(false);
        final ExecCommandThread t = getHost().execCommandCache(
                          "DrbdAvailVersions",
                          null,
                          new ExecCallback() {
                            public void done(final String ans) {
                                String[] items = ans.split(NEWLINE);
                                /* all drbd versions are stored in form
                                 * {version1,version2,...}. This will be
                                 * later expanded by shell. */
                                StringBuffer item = new StringBuffer("{");
                                for (int i = 0; i < items.length - 1; i++) {
                                    item.append("'" + items[i] + "',");
                                }
                                if (items.length != 0) {
                                    item.append("'"
                                                + items[items.length - 1]
                                                + "'");
                                }
                                item.append('}');
                                getHost().setDrbdVersionToInstall(
                                                               item.toString());
                                availDistributions();
                            }
                            public void doneError(final String ans,
                                                  final int exitCode) {
                                printErrorAndRetry(Tools.getString(
                                                "Dialog.HostDist.NoVersions"),
                                                   ans,
                                                   exitCode);
                            }
                          }, false);
        setCommandThread(t);
    }

    /**
     * Checks the available distributions.
     */
    protected void availDistributions() {
        drbdKernelDirCombo.setEnabled(false);
        drbdArchCombo.setEnabled(false);
        final ExecCommandThread t = getHost().execCommandCache(
                          "DrbdAvailDistributions",
                          null,
                          new ExecCallback() {
                            public void done(String ans) {
                                ans = NO_MATCH_STRING + "\n" + ans;
                                // TODO: should be "\n" ?
                                String[] items = ans.split(NEWLINE);
                                drbdDistCombo.reloadComboBox(
                                                    getHost().getDistVersion(),
                                                    items);
                                String selectedItem =
                                                drbdDistCombo.getStringValue();
                                drbdDistCombo.setEnabled(true);
                                availKernels();
                            }
                            public void doneError(final String ans,
                                                  final int exitCode) {
                                printErrorAndRetry(Tools.getString(
                                            "Dialog.HostDist.NoDistributions"),
                                                   ans,
                                                   exitCode);
                            }
                          }, false);
        setCommandThread(t);
    }

    /**
     * Checks what are the available kernels for this distribution.
     */
    protected void availKernels() {
        final String distVersion = getHost().getDistVersion();
        if (distVersion == null || distVersion.equals(NO_MATCH_STRING)) {
            drbdKernelDirCombo.reloadComboBox(null,
                                              new String[]{NO_MATCH_STRING});
            drbdKernelDirCombo.setEnabled(false);
            getHost().setKernelVersion(null);
            availArchs();
            return;
        }
        drbdArchCombo.setEnabled(false);
        final ExecCommandThread t = getHost().execCommandCache(
                          "DrbdAvailKernels",
                          null,
                          new ExecCallback() {
                            public void done(String ans) {
                                ans = NO_MATCH_STRING + "\n" + ans;
                                String[] items = ans.split(NEWLINE);
                                drbdKernelDirCombo.reloadComboBox(
                                                getHost().getKernelVersion(),
                                                items);
                                String selectedItem =
                                            drbdKernelDirCombo.getStringValue();
                                getHost().setKernelVersion(selectedItem);
                                drbdKernelDirCombo.setEnabled(true);
                                availArchs();
                            }

                            public void doneError(final String ans,
                                                  final int exitCode) {
                                Tools.debug(this, "doneError");
                                printErrorAndRetry(
                                   Tools.getString("Dialog.HostDist.NoKernels"),
                                   ans,
                                   exitCode);
                            }
                          }, false);
        setCommandThread(t);
    }

    /**
     * Checks what are the available architectures for this distribution.
     */
    protected void availArchs() {
        final String kernelVersion = getHost().getKernelVersion();
        if (kernelVersion == null || kernelVersion.equals(NO_MATCH_STRING)) {
            drbdArchCombo.reloadComboBox(null, new String[]{NO_MATCH_STRING});
            drbdArchCombo.setEnabled(false);
            getHost().setArch(null);
            allDone(null);
            return;
        }
        final ExecCommandThread t = getHost().execCommandCache(
                          "DrbdAvailArchs",
                          null,
                          new ExecCallback() {
                            public void done(String ans) {
                                ans = NO_MATCH_STRING + "\n" + ans;
                                String defaultValue = getHost().getArch();
                                String[] items = ans.split(NEWLINE);
                                drbdArchCombo.reloadComboBox(
                                                        getHost().getArch(),
                                                        items);
                                String selectedItem =
                                                drbdArchCombo.getStringValue();
                                drbdArchCombo.setEnabled(true);
                                getHost().setArch(selectedItem);
                                if (selectedItem == null
                                    || selectedItem.equals(NO_MATCH_STRING)) {
                                    allDone(null);
                                } else {
                                    availVersionsForDist();
                                }
                            }

                            public void doneError(final String ans,
                                                  final int exitCode) {
                                printErrorAndRetry(
                                    Tools.getString("Dialog.HostDist.NoArchs"),
                                    ans,
                                    exitCode);
                            }
                          }, false);
        setCommandThread(t);
    }

    /**
     * Checks what are the avail drbd versions for this distribution.
     */
    protected void availVersionsForDist() {
        final ExecCommandThread t = getHost().execCommandCache(
                          "DrbdAvailVersionsForDist",
                          null,
                          new ExecCallback() {
                            public void done(final String ans) {
                                allDone(ans);
                            }

                            public void doneError(final String ans,
                                                  final int exitCode) {
                                printErrorAndRetry(
                                    Tools.getString("Dialog.HostDist.NoArchs"),
                                    ans,
                                    exitCode);
                            }
                          }, false);
        setCommandThread(t);
    }

    /**
     * Is called after all is done. It adds the listeners if it is the first
     * time it is called.
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
        if (ans == null) {
            answerPaneSetText(answerText + "\n"
                              + Tools.getString(
                                       "Dialog.HostDist.DownloadNotAvailable"));
        } else {
            final String[] versions = ans.split(NEWLINE);
            getHost().setAvailableDrbdVersions(versions);
            answerPaneSetText(answerText + "\n"
                              + Tools.getString("Dialog.HostDist.AvailVersions")
                              + " " + Tools.join(", ", versions));
        }
        if (!listenersAdded) {
            addListeners();
            listenersAdded = true;
        }
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
                    drbdDistCombo.setEnabled(false);
                    drbdKernelDirCombo.setEnabled(false);
                    drbdArchCombo.setEnabled(false);

                    disableComponents();
                    getProgressBar().start(20000);
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
                                                "Dialog.HostDist.NoDist"),
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
        if (getHost().isDrbdUpgraded()) {
            return new HostCheckInstallation(this, getHost());
        } else {
            return new HostDrbdAvailFiles(this, getHost());
            //return new HostLogin(this, getHost());
        }
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.HostDist.Title in TextResources.
     */
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.HostDist.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.HostDist.Description in TextResources.
     */
    protected String getDescription() {
        return Tools.getString("Dialog.HostDist.Description");
    }

    /**
     * Returns the pane with all combo boxes.
     */
    protected JPanel getChoiceBoxes() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
        final int maxX = (int) pane.getMaximumSize().getWidth();
        pane.setMaximumSize(new Dimension(maxX, CHOICE_BOX_HEIGHT));

        /* combo boxes */
        drbdDistCombo = new GuiComboBox(null,
                                        null,
                                        GuiComboBox.Type.COMBOBOX,
                                        null,
                                        0);

        drbdDistCombo.setEnabled(false);
        pane.add(drbdDistCombo);
        drbdKernelDirCombo = new GuiComboBox(null,
                                             null,
                                             GuiComboBox.Type.COMBOBOX,
                                             null,
                                             0);

        drbdKernelDirCombo.setEnabled(false);
        pane.add(drbdKernelDirCombo);
        drbdArchCombo = new GuiComboBox(null,
                                        null,
                                        GuiComboBox.Type.COMBOBOX,
                                        null,
                                        0);

        drbdArchCombo.setEnabled(false);
        pane.add(drbdArchCombo);
        pane.add(Box.createHorizontalGlue());
        pane.add(Box.createRigidArea(new Dimension(10, 0)));
        return pane;
    }

    /**
     * Adds listeners to the check boxes.
     */
    private void addListeners() {
        /* listeners */
        /* distribution combo box */
        final ItemListener distItemListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    drbdKernelDirCombo.setEnabled(false);
                    drbdArchCombo.setEnabled(false);
                    final String item = drbdDistCombo.getStringValue();
                    SwingUtilities.invokeLater(
                        new Runnable() {
                            public void run() {
                                getHost().setDistVersion(item);
                                availKernels();
                            }
                        });
                }
            }
        };
        drbdDistCombo.addListeners(distItemListener, null);

        /* kernel version combo box */
        final ItemListener kernelItemListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    drbdArchCombo.setEnabled(false);
                    final String item = drbdKernelDirCombo.getStringValue();
                    SwingUtilities.invokeLater(
                        new Runnable() {
                            public void run() {
                                getHost().setKernelVersion(item);
                                availArchs();
                            }
                        });
                }
            }
        };
        drbdKernelDirCombo.addListeners(kernelItemListener, null);

        /* arch combo box */
        final ItemListener archItemListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final String item = drbdArchCombo.getStringValue();
                    SwingUtilities.invokeLater(
                        new Runnable() {
                            public void run() {
                                getHost().setArch(item);
                                availVersionsForDist();
                            }
                        });
                }
            }
        };
        drbdArchCombo.addListeners(archItemListener, null);

    }

    /**
     * Returns the input pane with check boxes and other info.
     */
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getChoiceBoxes());
        pane.add(getAnswerPane(Tools.getString("Dialog.HostDist.Executing")));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }
}
