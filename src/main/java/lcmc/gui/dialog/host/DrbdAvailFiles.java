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

package lcmc.gui.dialog.host;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.Host;
import lcmc.data.StringValue;
import lcmc.data.Value;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.SSH;
import lcmc.utilities.Tools;
import lcmc.utilities.WidgetListener;

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
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;
    /** Combo box with drbd versions. */
    private Widget drbdVersionCombo = null;
    /** Combo box with drbd builds. (kernel, arch) */
    private Widget drbdBuildCombo = null;
    /** Whether the listeners where added. */
    private boolean listenersAdded = false;

    /** Prepares a new {@code DrbdAvailFiles} object. */
    public DrbdAvailFiles(final WizardDialog previousDialog, final Host host) {
        super(previousDialog, host);
    }

    /**
     * Inits the dialog and starts detecting the available drbd builds and
     * files.
     */
    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        listenersAdded = false;
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});

        disableComponents(new Component[]{drbdVersionCombo.getComponent(),
                                          drbdBuildCombo.getComponent()});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        getProgressBar().start(4000);
        getHost().setDrbdBuildToInstall(getHost().getDetectedKernelVersion());
        /* get drbd available versions and continue with availBuilds */
        final Value[] versions = StringValue.getValues(
                                        getHost().getAvailableDrbdVersions());
        if (versions != null && versions.length != 0) {
            final Value version = versions[versions.length - 1];
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
        getHost().execCommand(
                          "DrbdAvailBuilds",
                          null, /* ProgresBar */
                          new ExecCallback() {
                            @Override
                            public void done(final String answer) {
                                String defaultValue =
                                            getHost().getDrbdBuildToInstall();
                                final String[] items = answer.split("\\r?\\n");
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
                                drbdBuildCombo.clear();
                                drbdBuildCombo.reloadComboBox(
                                                new StringValue(defaultValue),
                                                StringValue.getValues(items));
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

                            @Override
                            public void doneError(final String answer,
                                                  final int errorCode) {
                                Tools.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        printErrorAndRetry(
                                         Tools.getString(
                                         "Dialog.Host.DrbdAvailFiles.NoBuilds"),
                                                answer,
                                                errorCode);
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
        getHost().execCommand("DrbdAvailFiles",
                      null, /* ProgresBar */
                      new ExecCallback() {
                        @Override
                        public void done(final String answer) {
                            final List<String> files = new ArrayList<String>(
                                                        Arrays.asList(
                                                         answer.split("\\r?\\n")));
                            if (files.size() >= 2) {
                                if (files.size() > 4) {
                                    /* remove the virtual package. */
                                    files.remove(0);
                                }
                                final String[] filesA = files.toArray(
                                                new String[files.size()]);
                                Tools.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        answerPaneSetText(Tools.join("\n",
                                                                     filesA));
                                    }
                                });
                                getHost().setDrbdPackagesToInstall(
                                                       Tools.shellList(filesA));
                                allDone();
                            } else {
                                Tools.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        printErrorAndRetry(Tools.getString(
                                         "Dialog.Host.DrbdAvailFiles.NoFiles"));
                                    }
                                });
                            }
                        }

                        @Override
                        public void doneError(final String answer,
                                              final int errorCode) {
                            Tools.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    printErrorAndRetry(
                                     Tools.getString(
                                      "Dialog.Host.DrbdAvailFiles.NoBuilds"),
                                            answer,
                                            errorCode);
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
        if (Tools.getApplication().getAutoOptionHost("drbdinst") != null) {
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    /** Returns the next dialog. */
    @Override
    public WizardDialog nextDialog() {
        return nextDialogObject;
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.Host.DrbdAvailFiles.Title in TextResources.
     */
    @Override
    protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DrbdAvailFiles.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.Host.DrbdAvailFiles.Description in TextResources.
     */
    @Override
    protected final String getDescription() {
        return Tools.getString("Dialog.Host.DrbdAvailFiles.Description");
    }

    /** Returns the panel with combo boxes. */
    protected final JPanel getChoiceBoxes() {
        final JPanel pane = new JPanel(new SpringLayout());

        /* drbd version combo box */
        drbdVersionCombo = WidgetFactory.createInstance(
                                      Widget.Type.COMBOBOX,
                                      Widget.NO_DEFAULT,
                                      Widget.NO_ITEMS,
                                      Widget.NO_REGEXP,
                                      0,    /* width */
                                      Widget.NO_ABBRV,
                                      new AccessMode(Application.AccessType.RO,
                                                     !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);
        pane.add(drbdVersionCombo.getComponent());

        /* build combo box */
        drbdBuildCombo = WidgetFactory.createInstance(
                                      Widget.Type.COMBOBOX,
                                      Widget.NO_DEFAULT,
                                      Widget.NO_ITEMS,
                                      Widget.NO_REGEXP,
                                      0,    /* width */
                                      Widget.NO_ABBRV,
                                      new AccessMode(Application.AccessType.RO,
                                                     !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);

        pane.add(drbdBuildCombo.getComponent());

        SpringUtilities.makeCompactGrid(pane, 1, 2,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }

    /** Adds listeners to all combo boxes. */
    private void addListeners() {
        /* drbd version combo box */
        drbdVersionCombo.addListeners(
            new WidgetListener() {
                @Override
                public void check(final Value value) {
                    enableComponentsLater(
                            new JComponent[]{buttonClass(nextButton())});
                    disableComponents(new Component[]{drbdBuildCombo.getComponent()});
                    final String item = drbdVersionCombo.getStringValue();
                    Tools.invokeLater(
                        new Runnable() {
                            @Override
                            public void run() {
                                getHost().setDrbdVersionToInstall(item);
                                availBuilds();
                            }
                        });
                }
            });

        /* build combo box */
        drbdBuildCombo.addListeners(
            new WidgetListener() {
                @Override
                public void check(final Value value) {
                    final String item = drbdBuildCombo.getStringValue();
                    getHost().setDrbdBuildToInstall(item);
                    availFiles();
                }
            });
    }

    /** Returns input pane with available drbd files. */
    @Override
    protected final JComponent getInputPane() {
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
