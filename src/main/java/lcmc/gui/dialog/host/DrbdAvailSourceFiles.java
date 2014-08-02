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
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.Host;
import lcmc.model.StringValue;
import lcmc.model.Value;
import lcmc.model.drbd.DrbdInstallation;
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
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
@Component
final class DrbdAvailSourceFiles extends DialogHost {
    @Autowired
    private DrbdCommandInst drbdCommandInst = null;
    /** Combo box with drbd tarballs. */
    private Widget drbdTarballCombo = null;
    /** Whether the listeners where added. */
    private boolean listenersAdded = false;

    /**
     * Inits the dialog and starts detecting the available drbd source
     * tarballs.
     */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        disableComponents(new java.awt.Component[]{drbdTarballCombo.getComponent()});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        getProgressBar().start(2000);
        availTarballs();
    }

    /** Finds abailable tarballs. */
    protected void availTarballs() {
        getHost().execCommand(new ExecCommandConfig()
                .commandString("DrbdAvailVersionsSource")
                .convertCmdCallback(getDrbdInstallationConvertCmdCallback())
                .execCallback(new ExecCallback() {
                    @Override
                    public void done(final String answer) {
                        if (answer == null || answer.isEmpty()) {
                            doneError(null, 1);
                            return;
                        }
                        final String[] versions = answer.split("\\r?\\n");
                        if (versions.length == 0) {
                            doneError(null, 1);
                            return;
                        }
                        final List<Value> items = new ArrayList<Value>();
                        for (final String versionString : versions) {
                            if (versionString != null
                                    && versionString.length() > 16) {
                                final String version =
                                        versionString.substring(
                                                9,
                                                versionString.length() - 7);
                                items.add(
                                        new StringValue(versionString, version));
                            }
                        }
                        drbdTarballCombo.clear();
                        Tools.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                if (!items.isEmpty()) {
                                    drbdTarballCombo.reloadComboBox(
                                            items.get(0),
                                            items.toArray(new Value[items.size()]));
                                }
                                final Value selectedItem =
                                        drbdTarballCombo.getValue();
                                drbdTarballCombo.setEnabled(true);
                                allDone(selectedItem);
                            }
                        });
                    }

                    @Override
                    public void doneError(final String answer,
                                          final int errorCode) {
                        Tools.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                progressBarDoneError();
                                printErrorAndRetry(Tools.getString(
                                                "Dialog.Host.DrbdAvailSourceFiles.NoBuilds"),
                                        answer,
                                        errorCode);
                            }
                        });
                    }
                }));
    }

    /**
     * Is called after everything is done. It adds listeners if called for the
     * first time.
     */
    protected void allDone(final Value versionInfo) {
        if (versionInfo != null) {
            answerPaneSetText("http://oss.linbit.com/drbd/"
                              + versionInfo.getValueForGui());
            final DrbdInstallation drbdInstallation = getDrbdInstallation();
            drbdInstallation.setDrbdVersionToInstall(versionInfo.toString());
            drbdInstallation.setDrbdVersionUrlStringToInstall(
                                            versionInfo.getValueForConfig());
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
        if (Tools.getApplication().getAutoOptionHost("drbdinst") != null) {
            pressNextButton();
        }
    }

    /** Returns the next dialog. */
    @Override
    public WizardDialog nextDialog() {
        return drbdCommandInst;
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.Host.DrbdAvailSourceFiles.Title in TextResources.
     */
    @Override
    protected String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DrbdAvailSourceFiles.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.Host.DrbdAvailSourceFiles.Description in TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Host.DrbdAvailSourceFiles.Description");
    }

    /** Returns the panel with combo boxes. */
    protected JPanel getChoiceBoxes() {
        final JPanel pane = new JPanel(new SpringLayout());

        /* build combo box */
        drbdTarballCombo = WidgetFactory.createInstance(
                                      Widget.Type.COMBOBOX,
                                      Widget.NO_DEFAULT,
                                      Widget.NO_ITEMS,
                                      Widget.NO_REGEXP,
                                      0,    /* width */
                                      Widget.NO_ABBRV,
                                      new AccessMode(Application.AccessType.RO,
                                                     !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);

        //drbdTarballCombo.setEnabled(false);
        pane.add(drbdTarballCombo.getComponent());

        SpringUtilities.makeCompactGrid(pane, 1, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }

    /** Adds listeners to all combo boxes. */
    private void addListeners() {
        /* tarball combo box */
        drbdTarballCombo.addListeners(new WidgetListener() {
            @Override
            public void check(final Value value) {
                final Value item = drbdTarballCombo.getValue();
                    allDone(item);
            }
        });

    }

    /** Returns input pane with available drbd files. */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        pane.add(getChoiceBoxes());
        pane.add(getProgressBarPane());
        pane.add(
            getAnswerPane(
               Tools.getString("Dialog.Host.DrbdAvailSourceFiles.Executing")));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad
        return pane;
    }
}
