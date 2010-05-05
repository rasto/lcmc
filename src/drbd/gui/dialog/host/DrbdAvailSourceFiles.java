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
import drbd.utilities.Tools;
import drbd.utilities.ExecCallback;
import drbd.utilities.ComboInfo;
import drbd.utilities.SSH;
import drbd.gui.SpringUtilities;
import drbd.gui.GuiComboBox;
import drbd.gui.dialog.WizardDialog;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

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
 * @author Rasto Levrinc
 * @version $Id$
 */
public class DrbdAvailSourceFiles extends DialogHost {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Next dialog object. */
    private WizardDialog nextDialogObject = null;
    /** Combo box with drbd tarballs. */
    private GuiComboBox drbdTarballCombo = null;
    /** Whether the listeners where added. */
    private boolean listenersAdded = false;

    /**
     * Prepares a new <code>DrbdAvailSourceFiles</code> object. */
    public DrbdAvailSourceFiles(final WizardDialog previousDialog,
                                final Host host) {
        super(previousDialog, host);
    }

    /**
     * Inits the dialog and starts detecting the available drbd source
     * tarballs.
     */
    protected final void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        disableComponents(new JComponent[]{drbdTarballCombo});
        getProgressBar().start(2000);
        availTarballs();
    }

    /**
     * Finds abailable tarballs.
     */
    protected final void availTarballs() {
        getHost().execCommandCache(
              "DrbdAvailVersionsSource",
              null, /* ProgresBar */
              new ExecCallback() {
                public void done(final String ans) {
                    final String[] versions = ans.split("\\r?\\n");
                    if (versions.length == 0) {
                        allDone(null);
                        return;
                    }
                    final List<ComboInfo> items = new ArrayList<ComboInfo>();
                    for (final String versionString : versions) {
                        if (versionString != null
                            && versionString.length() > 16) {
                            final String version =
                                    versionString.substring(
                                                   9,
                                                   versionString.length() - 7);
                            items.add(new ComboInfo(version, versionString));
                        }
                    }
                    drbdTarballCombo.clear();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            drbdTarballCombo.reloadComboBox(
                                   items.get(0).toString(),
                                   items.toArray(new ComboInfo[items.size()]));
                            final ComboInfo selectedItem =
                                   (ComboInfo) drbdTarballCombo.getValue();
                            drbdTarballCombo.setEnabled(true);
                            allDone(selectedItem);
                        }
                    });
                }

                public void doneError(final String ans,
                                      final int exitCode) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            progressBarDoneError();
                            printErrorAndRetry(Tools.getString(
                                   "Dialog.Host.DrbdAvailSourceFiles.NoBuilds"),
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

    /**
     * Is called after everything is done. It adds listeners if called for the
     * first time.
     */
    protected final void allDone(final ComboInfo versionInfo) {
        if (versionInfo != null) {
            answerPaneSetText("http://oss.linbit.com/drbd/"
                              + versionInfo.getStringValue());
            getHost().setDrbdVersionToInstall(versionInfo.toString());
            getHost().setDrbdVersionUrlStringToInstall(
                                            versionInfo.getStringValue());
        }
        // TODO: do something different if we did not get any versions
        drbdTarballCombo.setEnabled(true);
        nextDialogObject = new DrbdCommandInst(this, getHost());
        progressBarDone();
        enableComponents();
        buttonClass(nextButton()).requestFocus();
        if (!listenersAdded) {
            addListeners();
            listenersAdded = true;
        }
        if (Tools.getConfigData().getAutoOptionHost("drbdinst") != null) {
            pressNextButton();
        }
    }

    /**
     * Returns the next dialog.
     */
    public final WizardDialog nextDialog() {
        return nextDialogObject;
    }

    /**
     * Returns the title of the dialog. It is defined as
     * Dialog.Host.DrbdAvailSourceFiles.Title in TextResources.
     */
    protected final String getHostDialogTitle() {
        return Tools.getString("Dialog.Host.DrbdAvailSourceFiles.Title");
    }

    /**
     * Returns the description of the dialog. It is defined as
     * Dialog.Host.DrbdAvailSourceFiles.Description in TextResources.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.Host.DrbdAvailSourceFiles.Description");
    }

    /**
     * Returns the panel with combo boxes.
     */
    protected final JPanel getChoiceBoxes() {
        final JPanel pane = new JPanel(new SpringLayout());

        /* build combo box */
        drbdTarballCombo = new GuiComboBox(null, /* selected value */
                                           null, /* items */
                                           null, /* units */
                                           GuiComboBox.Type.COMBOBOX,
                                           null, /* regexp */
                                           0,    /* width */
                                           null, /* abbrv */
                                           ConfigData.AccessType.RO);

        //drbdTarballCombo.setEnabled(false);
        pane.add(drbdTarballCombo);

        SpringUtilities.makeCompactGrid(pane, 1, 1,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        return pane;
    }

    /**
     * Adds listeners to all combo boxes.
     */
    private void addListeners() {
        /* tarball combo box */
        final ItemListener buildItemListener = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final ComboInfo item =
                                       (ComboInfo) drbdTarballCombo.getValue();
                    allDone(item);
                }
            }
        };
        drbdTarballCombo.addListeners(buildItemListener, null);
    }

    /**
     * Returns input pane with available drbd files.
     */
    protected final JComponent getInputPane() {
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
