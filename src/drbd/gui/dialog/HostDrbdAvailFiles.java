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
import drbd.utilities.ExecCallback;
import drbd.gui.SpringUtilities;
import drbd.gui.GuiComboBox;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

/**
 * An implementation of a dialog where available versions of drbd will be
 * determined.
 *
 * TODO: this class needs to be made better
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HostDrbdAvailFiles extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;
    /** Combo box with drbd versions. */
    private GuiComboBox drbdVersionCombo = null;
    /** Combo box with drbd builds. (kernel, arch) */
    private GuiComboBox drbdBuildCombo = null;
    /** Whether the listeners where added. */
    private boolean listenersAdded = false;

    /**
     * Prepares a new <code>HostDrbdAvailFiles</code> object. */
    public HostDrbdAvailFiles(final WizardDialog previousDialog,
                              final Host host) {
        super(previousDialog, host);
    }

    /**
     * Inits the dialog and starts detecting the available drbd builds and
     * files.
     */
    protected void initDialog() {
        super.initDialog();
        listenersAdded = false;
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});

        disableComponents(new JComponent[]{drbdVersionCombo, drbdBuildCombo});
        getProgressBar().start(4000);
        getHost().setDrbdBuildToInstall(getHost().getDetectedKernelVersion());
        /* get drbd available versions and continue with availBuilds */
        final String[] versions = getHost().getAvailableDrbdVersions();
        if (versions != null && versions.length != 0) {
            final String version = versions[versions.length - 1];
            drbdVersionCombo.reloadComboBox(version, versions);
        }
        final String selectedItem = drbdVersionCombo.getStringValue();
        if (selectedItem == null) {
            allDone();
        } else {
            getHost().setDrbdVersionToInstall(selectedItem);
            availBuilds();
        }
    }

    /**
     * Finds abailable builds.
     */
    protected void availBuilds() {
        getHost().execCommandCache(
                          "DrbdAvailBuilds",
                          null, /* ProgresBar */
                          new ExecCallback() {
                            public void done(final String ans) {
                                String defaultValue =
                                            getHost().getDrbdBuildToInstall();
                                final String[] items = ans.split("\\r?\\n");
                                boolean found = false;
                                for (final String item : items) {
                                    if (item.equals(defaultValue)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    /* try it with underscores */
                                    defaultValue =
                                            defaultValue.replaceAll("-", "_");
                                }
                                final String defaultValueCopy = defaultValue;
                                drbdBuildCombo.clear();
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        drbdBuildCombo.reloadComboBox(
                                                              defaultValueCopy,
                                                              items);
                                        final String selectedItem =
                                               drbdBuildCombo.getStringValue();
                                        drbdBuildCombo.setEnabled(true);
                                        if (selectedItem == null) {
                                            allDone();
                                        } else {
                                            getHost().setDrbdBuildToInstall(
                                                                 selectedItem);
                                            if (!listenersAdded) {
                                                availFiles();
                                            }
                                        }
                                    }
                                });
                            }

                            public void doneError(final String ans,
                                                  final int exitCode) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        printErrorAndRetry(
                                         Tools.getString(
                                          "Dialog.HostDrbdAvailFiles.NoBuilds"),
                                         ans,
                                         exitCode);
                                    }
                                });
                            }
                          },
                          null,   /* ConvertCmdCallback */
                          false); /* outputVisible */
    }

    /**
     * Finds available files.
     */
    protected void availFiles() {

        drbdBuildCombo.setEnabled(true);
        getHost().execCommandCache("DrbdAvailFiles",
                      null, /* ProgresBar */
                      new ExecCallback() {
                        public void done(final String ans) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    answerPaneSetText(ans);
                                }
                            });
                            final String[] files = ans.split("\\r?\\n");
                            if (files.length == 2) {
                                getHost().setDrbdPackageToInstall(files[0]);
                                getHost().setDrbdModulePackageToInstall(
                                                                 files[1]);
                                allDone();
                            } else {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        printErrorAndRetry(Tools.getString(
                                         "Dialog.HostDrbdAvailFiles.NoFiles"));
                                    }
                                });
                            }
                        }

                        public void doneError(final String ans,
                                              final int exitCode) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    printErrorAndRetry(
                                     Tools.getString(
                                      "Dialog.HostDrbdAvailFiles.NoBuilds"),
                                     ans,
                                     exitCode);
                                }
                            });
                        }
                      },
                      null,  /* ConvertCmdCallback */
                      true); /* outputVisible */
    }

    /**
     * Is called after everything is done. It adds listeners if called for the
     * first time.
     */
    protected void allDone() {

        //nextDialogObject = new HostDrbdInst(this, getHost());
        nextDialogObject = new HostLinbitLogin(this, getHost());
        progressBarDone();
        enableComponents();
        buttonClass(nextButton()).requestFocus();
        if (!listenersAdded) {
            addListeners();
            listenersAdded = true;
        }
    }

    /**
     * Returns the next dialog.
     */
    public WizardDialog nextDialog() {
        return nextDialogObject;
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.HostDrbdAvailFiles.Title in TextResources.
     */
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.HostDrbdAvailFiles.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.HostDrbdAvailFiles.Description in TextResources.
     */
    protected String getDescription() {
        return Tools.getString("Dialog.HostDrbdAvailFiles.Description");
    }

    /**
     * Returns the panel with combo boxes.
     */
    protected JPanel getChoiceBoxes() {
        final JPanel pane = new JPanel(new SpringLayout());

        /* drbd version combo box */
        drbdVersionCombo = new GuiComboBox(null,
                                           null,
                                           GuiComboBox.Type.COMBOBOX,
                                           null,
                                           0);
        //drbdVersionCombo.setEnabled(false);
        pane.add(drbdVersionCombo);

        /* build combo box */
        drbdBuildCombo = new GuiComboBox(null,
                                         null,
                                         GuiComboBox.Type.COMBOBOX,
                                         null,
                                         0);

        //drbdBuildCombo.setEnabled(false);
        pane.add(drbdBuildCombo);

        SpringUtilities.makeCompactGrid(pane, 1, 2,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }

    /**
     * Adds listeners to all combo boxes.
     */
    private void addListeners() {
        /* drbd version combo box */
        final ItemListener versionItemListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    //drbdBuildCombo.setEnabled(false);
                    enableComponentsLater(
                            new JComponent[]{buttonClass(nextButton())});
                    disableComponents(new JComponent[]{drbdBuildCombo});
                    final String item = drbdVersionCombo.getStringValue();
                    SwingUtilities.invokeLater(
                        new Runnable() {
                            public void run() {
                                getHost().setDrbdVersionToInstall(item);
                                availBuilds();
                            }
                        });
                }
            }
        };
        drbdVersionCombo.addListeners(versionItemListener, null);

        /* build combo box */
        final ItemListener buildItemListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final String item = drbdBuildCombo.getStringValue();
                    getHost().setDrbdBuildToInstall(item);
                    availFiles();
                }
            }
        };
        drbdBuildCombo.addListeners(buildItemListener, null);
    }

    /**
     * Returns input pane with available drbd files.
     */
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getProgressBarPane());
        pane.add(getChoiceBoxes());
        pane.add(getAnswerPane(
                    Tools.getString("Dialog.HostDrbdAvailFiles.Executing")));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }
}
