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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.StringValue;
import lcmc.model.Value;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.Tools;
import lcmc.utilities.WidgetListener;
import lcmc.utilities.ssh.ExecCommandConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An implementation of a dialog where available versions of drbd will be
 * determined.
 */
@Component
public class DrbdAvailFiles extends DialogHost {
    @Autowired
    private LinbitLogin linbitLogin;
    private Widget drbdVersionCombo = null;
    /** Combo box with drbd builds. (kernel, arch) */
    private Widget drbdBuildCombo = null;
    private boolean listenersAdded = false;

    @Override
    protected final void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        listenersAdded = false;
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});

        disableComponents(new java.awt.Component[]{drbdVersionCombo.getComponent(), drbdBuildCombo.getComponent()});
    }

    @Override
    protected void initDialogAfterVisible() {
        getProgressBar().start(4000);
        getDrbdInstallation().setDrbdBuildToInstall(getHost().getDetectedKernelVersion());
        /* get drbd available versions and continue with availBuilds */
        final Value[] versions = StringValue.getValues(getDrbdInstallation().getAvailableDrbdVersions());
        if (versions != null && versions.length != 0) {
            final Value version = versions[versions.length - 1];
            drbdVersionCombo.reloadComboBox(version, versions);
            Tools.waitForSwing();
        }
        final String selectedItem = drbdVersionCombo.getStringValue();
        if (selectedItem == null) {
            allDone();
        } else {
            getDrbdInstallation().setDrbdVersionToInstall(selectedItem);
            availBuilds();
        }
    }

    protected final void availBuilds() {
        getHost().execCommand(new ExecCommandConfig().commandString("DrbdAvailBuilds")
                                                     .convertCmdCallback(getDrbdInstallationConvertCmdCallback())
                                                     .execCallback(new ExecCallback() {
                @Override
                public void done(final String answer) {
                    String defaultValue = getDrbdInstallation().getDrbdBuildToInstall();
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
                        defaultValue = defaultValue.replaceAll("-", "_");
                    }
                    drbdBuildCombo.clear();
                    drbdBuildCombo.reloadComboBox(new StringValue(defaultValue), StringValue.getValues(items));
                    Tools.waitForSwing();
                    final String selectedItem = drbdBuildCombo.getStringValue();
                    drbdBuildCombo.setEnabled(true);
                    if (selectedItem == null) {
                        allDone();
                    } else {
                        getDrbdInstallation().setDrbdBuildToInstall(selectedItem);
                        if (!listenersAdded) {
                            availFiles();
                        }
                    }
                }

                @Override
                public void doneError(final String answer, final int errorCode) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            printErrorAndRetry(Tools.getString("Dialog.Host.DrbdAvailFiles.NoBuilds"), answer, errorCode);
                        }
                    });
                }
            }));
    }

    /** Finds available files. */
    protected final void availFiles() {
        drbdBuildCombo.setEnabled(true);
        getHost().execCommand(new ExecCommandConfig().commandString("DrbdAvailFiles")
                                                     .convertCmdCallback(getDrbdInstallationConvertCmdCallback())
                                                     .execCallback(new ExecCallback() {
            @Override
            public void done(final String answer) {
                final List<String> files = new ArrayList<String>(Arrays.asList(answer.split("\\r?\\n")));
                if (files.size() >= 2) {
                    if (files.size() > 4) {
                        /* remove the virtual package. */
                        files.remove(0);
                    }
                    final String[] filesA = files.toArray(new String[files.size()]);
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            answerPaneSetText(Tools.join("\n", filesA));
                        }
                    });
                    getDrbdInstallation().setDrbdPackagesToInstall(Tools.shellList(filesA));
                    allDone();
                } else {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            printErrorAndRetry(Tools.getString("Dialog.Host.DrbdAvailFiles.NoFiles"));
                        }
                    });
                }
            }

            @Override
            public void doneError(final String answer, final int errorCode) {
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        printErrorAndRetry(Tools.getString("Dialog.Host.DrbdAvailFiles.NoBuilds"), answer, errorCode);
                    }
                });
            }
          }));
    }

    /**
     * Is called after everything is done. It adds listeners if called for the
     * first time.
     */
    protected final void allDone() {
        linbitLogin.init(this, getHost(), getDrbdInstallation());
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

    @Override
    public WizardDialog nextDialog() {
        return linbitLogin;
    }

    @Override
    protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DrbdAvailFiles.Title");
    }

    @Override
    protected final String getDescription() {
        return Tools.getString("Dialog.Host.DrbdAvailFiles.Description");
    }

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
                                      new AccessMode(Application.AccessType.RO, !AccessMode.ADVANCED),
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
                                      new AccessMode(Application.AccessType.RO, !AccessMode.ADVANCED),
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
                    enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
                    disableComponents(new java.awt.Component[]{drbdBuildCombo.getComponent()});
                    final String item = drbdVersionCombo.getStringValue();
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            getDrbdInstallation().setDrbdVersionToInstall(item);
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
                    getDrbdInstallation().setDrbdBuildToInstall(item);
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
        pane.add(getAnswerPane(Tools.getString("Dialog.Host.DrbdAvailFiles.Executing")));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad
        return pane;
    }
}
