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

import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.Exceptions;
import lcmc.cluster.infrastructure.ssh.ExecCommandConfig;
import lcmc.cluster.infrastructure.ssh.SshOutput;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.WidgetListener;
import lcmc.drbd.domain.DrbdInstallation;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * An implementation of a dialog where available versions of drbd will be determined.
 */
@Named
final class DrbdAvailSourceFiles extends DialogHost {
    private static final String DRBD_VERSION_AFTER_UTIL_SPLIT = "8.4.5";
    private final DrbdCommandInst drbdCommandInst;
    private Widget drbdTarballCombo = null;
    private boolean listenersAdded = false;
    private final Application application;
    private final SwingUtils swingUtils;
    private final WidgetFactory widgetFactory;
    private static final Logger drbdVersions = LoggerFactory.getLogger(DrbdAvailSourceFiles.class);

    public DrbdAvailSourceFiles(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData,
            DrbdCommandInst drbdCommandInst, Provider<ProgressBar> progressBarProvider) {
        super(application, swingUtils, widgetFactory, mainData, progressBarProvider);
        this.drbdCommandInst = drbdCommandInst;
        this.application = application;
        this.swingUtils = swingUtils;
        this.widgetFactory = widgetFactory;
    }

    /**
     * Inits the dialog and starts detecting the available drbd source tarballs.
     */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        disableComponents(new java.awt.Component[]{drbdTarballCombo.getComponent()});
    }

    @Override
    protected void initDialogAfterVisible() {
        getProgressBar().start(2000);
        findAvailTarballs();
    }

    private void findAvailTarballs() {
        final SshOutput ret = getHost().captureCommand(new ExecCommandConfig().commandString("DrbdAvailVersionsSource")
                                                                              .convertCmdCallback(
                                                                                      getDrbdInstallationConvertCmdCallback())
                                                                              .silentOutput());
        final int exitCode = ret.getExitCode();
        final String output = ret.getOutput();
        if (exitCode == 0) {
            if (output == null || output.isEmpty()) {
                progressBarDoneError();
                printErrorAndRetry(Tools.getString("Dialog.Host.DrbdAvailSourceFiles.NoBuilds"), output, exitCode);
                return;
            }
            final String[] moduleFileNames = output.split("\\r?\\n");
            if (moduleFileNames.length == 0) {
                progressBarDoneError();
                printErrorAndRetry(Tools.getString("Dialog.Host.DrbdAvailSourceFiles.NoBuilds"), output, exitCode);
                return;
            }
            final List<DrbdVersions> items = new ArrayList<>();
            for (final String moduleFileName : moduleFileNames) {
                if (moduleFileName != null && moduleFileName.length() > 16) {
                    String moduleVersion = moduleFileName.substring(9, moduleFileName.length() - 7);
                    String utilFileName = null;
                    String utilVersion = null;
                    try {
                        if (Tools.compareVersions(moduleVersion, DRBD_VERSION_AFTER_UTIL_SPLIT) >= 0) {
                            utilFileName = getLastDrbdUtilFileName();
                            if (utilFileName != null) {
                                utilVersion = getDrbdUtilVersion(utilFileName);
                            }
                        }
                    } catch (Exceptions.IllegalVersionException e) {
                        drbdVersions.appWarning("can't compare version: " + e);
                    }
                    items.add(new DrbdVersions(moduleFileName, moduleVersion, utilFileName, utilVersion));
                }
            }
            drbdTarballCombo.clear();
            swingUtils.invokeLater(() -> {
                if (!items.isEmpty()) {
                    drbdTarballCombo.reloadComboBox(items.get(0), items.toArray(new DrbdVersions[0]));
                }
                final DrbdVersions selectedItem = (DrbdVersions) drbdTarballCombo.getValue();
                drbdTarballCombo.setEnabled(true);
                allDone(selectedItem);
            });

        } else {
            swingUtils.invokeLater(() -> {
                progressBarDoneError();
                printErrorAndRetry(Tools.getString("Dialog.Host.DrbdAvailSourceFiles.NoBuilds"), output, exitCode);
            });
        }
    }

    private String getLastDrbdUtilFileName() {
        final SshOutput ret = getHost().captureCommand(new ExecCommandConfig()
                .commandString("DrbdUtilAvailVersionsSource")
                .convertCmdCallback(getDrbdInstallationConvertCmdCallback()).silentOutput());
        final int exitCode = ret.getExitCode();
        final String output = ret.getOutput();
        if (exitCode == 0) {
            if (output == null || output.isEmpty()) {
                progressBarDoneError();
                printErrorAndRetry(Tools.getString("Dialog.Host.DrbdAvailSourceFiles.NoBuilds"), output, exitCode);
                return null;
            }
            final String[] versions = output.split("\\r?\\n");
            if (versions.length == 0) {
                progressBarDoneError();
                printErrorAndRetry(Tools.getString("Dialog.Host.DrbdAvailSourceFiles.NoBuilds"), output, exitCode);
                return null;
            }
            return versions[0];
        } else {
            swingUtils.invokeLater(() -> {
                progressBarDoneError();
                printErrorAndRetry(Tools.getString("Dialog.Host.DrbdAvailSourceFiles.NoBuilds"), output, exitCode);
            });
            return null;
        }
    }

    private String getDrbdUtilVersion(final String utilFileName) {
        if (utilFileName != null && utilFileName.length() > 21) {
            return utilFileName.substring(11, utilFileName.length() - 7);
        } else {
            return null;
        }
    }

    /**
     * Is called after everything is done. It adds listeners if called for the first time.
     */
    private void allDone(final DrbdVersions drbdVersions) {
        if (drbdVersions != null) {
            answerPaneSetText("http://oss.linbit.com/drbd/" + drbdVersions.getModuleFileName());
            final DrbdInstallation drbdInstallation = getDrbdInstallation();

            drbdInstallation.setDrbdToInstall(drbdVersions);
        }
        // TODO: do something different if we did not get any versions
        drbdTarballCombo.setEnabled(true);
        drbdCommandInst.init(this, getHost(), getDrbdInstallation());
        progressBarDone();
        enableComponents();
        buttonClass(nextButton()).requestFocus();
        if (!listenersAdded) {
            addListeners();
            listenersAdded = true;
        }
        if (application.getAutoOptionHost("drbdinst") != null) {
            pressNextButton();
        }
    }

    @Override
    public WizardDialog nextDialog() {
        return drbdCommandInst;
    }

    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DrbdAvailSourceFiles.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.DrbdAvailSourceFiles.Description");
    }

    private JPanel getChoiceBoxes() {
        final JPanel pane = new JPanel(new SpringLayout());

        /* build combo box */
        drbdTarballCombo = widgetFactory.createInstance(Widget.Type.COMBOBOX, Widget.NO_DEFAULT, Widget.NO_ITEMS, Widget.NO_REGEXP,
                0,    /* width */
                Widget.NO_ABBRV, new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                                      Widget.NO_BUTTON);

        //drbdTarballCombo.setEnabled(false);
        pane.add(drbdTarballCombo.getComponent());

        SpringUtilities.makeCompactGrid(pane, 1, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }

    private void addListeners() {
        /* tarball combo box */
        drbdTarballCombo.addListeners(new WidgetListener() {
            @Override
            public void check(final Value value) {
                final DrbdVersions drbdVersions = (DrbdVersions) drbdTarballCombo.getValue();
                allDone(drbdVersions);
            }
        });
    }

    /** Returns input pane with available drbd files. */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getChoiceBoxes());
        pane.add(getProgressBarPane());
        pane.add(getAnswerPane(Tools.getString("Dialog.Host.DrbdAvailSourceFiles.Executing")));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad
        return pane;
    }
}
