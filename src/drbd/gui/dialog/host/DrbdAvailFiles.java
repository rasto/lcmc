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

package drbd.gui.dialog.host;

import drbd.data.Host;
import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.utilities.Tools;
import drbd.utilities.ExecCallback;
import drbd.utilities.SSH;
import drbd.gui.SpringUtilities;
import drbd.gui.GuiComboBox;
import drbd.gui.dialog.WizardDialog;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

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
 */
public class DrbdAvailFiles extends DialogHost {
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

    /** Prepares a new <code>DrbdAvailFiles</code> object. */
    public DrbdAvailFiles(final WizardDialog previousDialog, final Host host) {
        super(previousDialog, host);
    }

    /**
     * Inits the dialog and starts detecting the available drbd builds and
     * files.
     */
    @Override protected final void initDialog() {
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
            Tools.waitForSwing();
        }
        final String selectedItem = drbdVersionCombo.getStringValue();
        if (selectedItem == null) {
            allDone();
        } else {
            getHost().setDrbdVersionToInstall(selectedItem);
            availBuilds();
        }
    }

    /** Finds abailable builds. */
    protected final void availBuilds() {
        getHost().execCommandCache(
                          "DrbdAvailBuilds",
                          null, /* ProgresBar */
                          new ExecCallback() {
                            @Override public void done(final String ans) {
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
                                drbdBuildCombo.reloadComboBox(defaultValueCopy,
                                                              items);
                                Tools.waitForSwing();
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

                            @Override public void doneError(
                                                          final String ans,
                                                          final int exitCode) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override public void run() {
                                        printErrorAndRetry(
                                         Tools.getString(
                                         "Dialog.Host.DrbdAvailFiles.NoBuilds"),
                                         ans,
                                         exitCode);
                                    }
                                });
                            }
                          },
                          null,   /* ConvertCmdCallback */
                          false,  /* outputVisible */
                          SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /** Finds available files. */
    protected final void availFiles() {
        drbdBuildCombo.setEnabled(true);
        getHost().execCommandCache("DrbdAvailFiles",
                      null, /* ProgresBar */
                      new ExecCallback() {
                        @Override public void done(final String ans) {
                            final List<String> files = new ArrayList<String>(
                                                        Arrays.asList(
                                                         ans.split("\\r?\\n")));
                            if (files.size() >= 2) {
                                if (files.size() > 4) {
                                    /* remove the virtual package. */
                                    files.remove(0);
                                }
                                final String[] filesA = files.toArray(
                                                new String[files.size()]);
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override public void run() {
                                        answerPaneSetText(Tools.join("\n",
                                                                     filesA));
                                    }
                                });
                                getHost().setDrbdPackagesToInstall(
                                                       Tools.shellList(filesA));
                                allDone();
                            } else {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override public void run() {
                                        printErrorAndRetry(Tools.getString(
                                         "Dialog.Host.DrbdAvailFiles.NoFiles"));
                                    }
                                });
                            }
                        }

                        @Override public void doneError(final String ans,
                                                        final int exitCode) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override public void run() {
                                    printErrorAndRetry(
                                     Tools.getString(
                                      "Dialog.Host.DrbdAvailFiles.NoBuilds"),
                                     ans,
                                     exitCode);
                                }
                            });
                        }
                      },
                      null,  /* ConvertCmdCallback */
                      true,  /* outputVisible */
                      SSH.DEFAULT_COMMAND_TIMEOUT);
    }

    /**
     * Is called after everything is done. It adds listeners if called for the
     * first time.
     */
    protected final void allDone() {
        nextDialogObject = new LinbitLogin(this, getHost());
        progressBarDone();
        enableComponents();
        buttonClass(nextButton()).requestFocus();
        if (!listenersAdded) {
            addListeners();
            listenersAdded = true;
        }
        if (Tools.getConfigData().getAutoOptionHost("drbdinst") != null) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    /** Returns the next dialog. */
    @Override public WizardDialog nextDialog() {
        return nextDialogObject;
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.Host.DrbdAvailFiles.Title in TextResources.
     */
    @Override protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DrbdAvailFiles.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.Host.DrbdAvailFiles.Description in TextResources.
     */
    @Override protected final String getDescription() {
        return Tools.getString("Dialog.Host.DrbdAvailFiles.Description");
    }

    /** Returns the panel with combo boxes. */
    protected final JPanel getChoiceBoxes() {
        final JPanel pane = new JPanel(new SpringLayout());

        /* drbd version combo box */
        drbdVersionCombo = new GuiComboBox(null, /* selected value */
                                           null, /* items */
                                           null, /* units */
                                           GuiComboBox.Type.COMBOBOX,
                                           null, /* regexp */
                                           0,    /* width */
                                           null, /* abbrv */
                                           new AccessMode(
                                                  ConfigData.AccessType.RO,
                                                  false)); /* only adv. mode */
        pane.add(drbdVersionCombo);

        /* build combo box */
        drbdBuildCombo = new GuiComboBox(null, /* selected value */
                                         null, /* items */
                                         null, /* units */
                                         GuiComboBox.Type.COMBOBOX,
                                         null, /* regexp */
                                         0,    /* width */
                                         null, /* abbrv */
                                         new AccessMode(
                                                  ConfigData.AccessType.RO,
                                                  false)); /* only adv. mode */

        pane.add(drbdBuildCombo);

        SpringUtilities.makeCompactGrid(pane, 1, 2,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }

    /** Adds listeners to all combo boxes. */
    private void addListeners() {
        /* drbd version combo box */
        final ItemListener versionItemListener = new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    //drbdBuildCombo.setEnabled(false);
                    enableComponentsLater(
                            new JComponent[]{buttonClass(nextButton())});
                    disableComponents(new JComponent[]{drbdBuildCombo});
                    final String item = drbdVersionCombo.getStringValue();
                    SwingUtilities.invokeLater(
                        new Runnable() {
                            @Override public void run() {
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
            @Override public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final String item = drbdBuildCombo.getStringValue();
                    getHost().setDrbdBuildToInstall(item);
                    availFiles();
                }
            }
        };
        drbdBuildCombo.addListeners(buildItemListener, null);
    }

    /** Returns input pane with available drbd files. */
    @Override protected final JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getChoiceBoxes());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(
                    Tools.getString("Dialog.Host.DrbdAvailFiles.Executing")));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad
        return pane;
    }
}
