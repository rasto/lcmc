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


package lcmc.gui.dialog.drbdConfig;

import lcmc.utilities.Tools;
import lcmc.utilities.ExecCallback;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.gui.resources.drbd.DrbdVolumeInfo;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.gui.dialog.WizardDialog;
import lcmc.utilities.MyButton;
import lcmc.utilities.DRBD;
import lcmc.utilities.WidgetListener;
import lcmc.data.Application;
import lcmc.data.AccessMode;
import lcmc.data.Host;

import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.regex.Matcher;

import lcmc.data.StringValue;
import lcmc.data.Value;

/**
 * An implementation of a dialog where drbd block devices are initialized.
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class CreateMD extends DrbdConfig {
    /** Metadata pulldown choices. */
    private Widget metadataWi;
    /** Make Meta-Data button. */
    private final MyButton makeMDButton = new MyButton();
    /** Width of the combo boxes. */
    private static final int COMBOBOX_WIDTH = 250;
    /** Return code of the create md command if fs is already there. */
    private static final int CREATE_MD_FS_ALREADY_THERE_RC = 40;

    /** Prepares a new {@code CreateMD} object. */
    CreateMD(final WizardDialog previousDialog,
                       final DrbdVolumeInfo dvi) {
        super(previousDialog, dvi);
    }

    /** Creates meta-data and checks the results. */
    private void createMetadata(final boolean destroyData) {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                makeMDButton.setEnabled(false);
            }
        });
        final Thread[] thread = new Thread[2];
        final String[] answerStore = new String[2];
        final Integer[] returnCode = new Integer[2];
        final BlockDevInfo[] bdis = {
                                getDrbdVolumeInfo().getFirstBlockDevInfo(),
                                getDrbdVolumeInfo().getSecondBlockDevInfo()
                                    };
        for (int i = 0; i < 2; i++) {
            final int index = i;
            returnCode[index] = -1;
            thread[i] = new Thread(
            new Runnable() {
                @Override
                public void run() {
                    final ExecCallback execCallback =
                        new ExecCallback() {
                            @Override
                            public void done(final String answer) {
                                Tools.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        makeMDButton.setEnabled(false);
                                    }
                                });
                                answerStore[index] = answer;
                                returnCode[index] = 0;
                            }

                            @Override
                            public void doneError(final String answer,
                                                  final int errorCode) {
                                answerStore[index] = answer;
                                returnCode[index] = errorCode;
                            }

                        };
                    String drbdMetaDisk =
                        getDrbdVolumeInfo().getMetaDiskForHost(
                                                        bdis[index].getHost());
                    if ("internal".equals(drbdMetaDisk)) {
                        drbdMetaDisk = bdis[index].getName();
                    }
                    final Application.RunMode runMode = Application.RunMode.LIVE;
                    if (destroyData) {
                        DRBD.createMDDestroyData(
                                    bdis[index].getHost(),
                                    getDrbdVolumeInfo().getDrbdResourceInfo()
                                                       .getName(),
                                    getDrbdVolumeInfo().getName(),
                                    drbdMetaDisk,
                                    execCallback,
                                    runMode);
                    } else {
                        DRBD.createMD(bdis[index].getHost(),
                                      getDrbdVolumeInfo().getDrbdResourceInfo()
                                                         .getName(),
                                      getDrbdVolumeInfo().getName(),
                                      drbdMetaDisk,
                                      execCallback,
                                      runMode);
                    }
                }
            });
            thread[i].start();
        }
        boolean error = false;
        for (int i = 0; i < 2; i++) {
            try {
                thread[i].join(0);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (returnCode[i] == CREATE_MD_FS_ALREADY_THERE_RC) {
                answerStore[i] = Tools.getString(
                               "Dialog.DrbdConfig.CreateMD.CreateMD.Failed.40");
                error = true;
            } else if (returnCode[i] > 0) {
                answerStore[i] = Tools.getString(
                                  "Dialog.DrbdConfig.CreateMD.CreateMD.Failed")
                                  + answerStore[i];
                error = true;
            } else {
                answerStore[i] = Tools.getString(
                                   "Dialog.DrbdConfig.CreateMD.CreateMD.Done");
            }
            answerStore[i] = answerStore[i].replaceAll(
                         "@HOST@",
                         Matcher.quoteReplacement(bdis[i].getHost().getName()));
        }
        if (error) {
            answerPaneSetTextError(Tools.join("\n", answerStore));
        } else {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    makeMDButton.setEnabled(false);
                    buttonClass(nextButton()).setEnabled(true);
                    if (Tools.getApplication().getAutoOptionGlobal(
                                                        "autodrbd") != null) {
                        pressNextButton();
                    }
                }
            });
            answerPaneSetText(Tools.join("\n", answerStore));
        }
    }

    /**
     * Returns next dialog plus it calls drbd up command for both devices and
     * returns the drbd config create fs dialog.
     */
    @Override
    public WizardDialog nextDialog() {
        final BlockDevInfo bdi1 = getDrbdVolumeInfo().getFirstBlockDevInfo();
        final BlockDevInfo bdi2 = getDrbdVolumeInfo().getSecondBlockDevInfo();
        final String clusterName = bdi1.getHost().getCluster().getName();
        Tools.startProgressIndicator(clusterName, "scanning block devices...");
        final Application.RunMode runMode = Application.RunMode.LIVE;
        if (getDrbdVolumeInfo().getDrbdResourceInfo().isProxy(bdi1.getHost())) {
            DRBD.proxyUp(bdi1.getHost(),
                         getDrbdVolumeInfo().getDrbdResourceInfo().getName(),
                         null,
                         runMode);
        }
        if (getDrbdVolumeInfo().getDrbdResourceInfo().isProxy(bdi2.getHost())) {
            DRBD.proxyUp(bdi2.getHost(),
                         getDrbdVolumeInfo().getDrbdResourceInfo().getName(),
                         null,
                         runMode);
        }
        DRBD.adjustApply(bdi1.getHost(),
                    getDrbdVolumeInfo().getDrbdResourceInfo().getName(),
                    getDrbdVolumeInfo().getName(),
                    runMode);
        DRBD.adjustApply(bdi2.getHost(),
                    getDrbdVolumeInfo().getDrbdResourceInfo().getName(),
                    getDrbdVolumeInfo().getName(),
                    runMode);
        final String device = getDrbdVolumeInfo().getDevice();
        final ClusterBrowser browser =
                        getDrbdVolumeInfo().getDrbdResourceInfo().getBrowser();
        browser.updateHWInfo(bdi1.getHost(), !Host.UPDATE_LVM);
        browser.updateHWInfo(bdi2.getHost(), !Host.UPDATE_LVM);
        bdi1.getBlockDevice().setDrbdBlockDevice(
                                bdi1.getHost().getDrbdBlockDevice(device));
        bdi2.getBlockDevice().setDrbdBlockDevice(
                                bdi2.getHost().getDrbdBlockDevice(device));
        Tools.stopProgressIndicator(clusterName, "scanning block devices...");
        return new CreateFS(this, getDrbdVolumeInfo());
    }

    /**
     * Returns the title of the dialog. This is specified as
     * Dialog.DrbdConfig.CreateMD.Title in the TextResources.
     */
    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfig.CreateMD.Title");
    }

    /**
     * Returns the description of the dialog. This is specified as
     * Dialog.DrbdConfig.CreateMD.Description in the TextResources.
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.DrbdConfig.CreateMD.Description");
    }

    /** Inits dialog. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        makeMDButton.setBackgroundColor(
                               Tools.getDefaultColor("ConfigDialog.Button"));
        if (getDrbdVolumeInfo().getDrbdResourceInfo().isHaveToCreateMD()) {
            enableComponentsLater(new JComponent[]{});
        } else {
            enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        }
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        if (Tools.getApplication().getAutoOptionGlobal("autodrbd") != null) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    makeMDButton.pressButton();
                }
            });
        }
    }

    /** Returns input pane with choices what to do with meta-data. */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel(new SpringLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());

        /* Meta-Data */
        final JLabel metadataLabel = new JLabel(
                    Tools.getString("Dialog.DrbdConfig.CreateMD.Metadata"));
        final String useExistingMetadata = Tools.getString(
                    "Dialog.DrbdConfig.CreateMD.UseExistingMetadata");
        final String createNewMetadata = Tools.getString(
                    "Dialog.DrbdConfig.CreateMD.CreateNewMetadata");
        final String createNewMetadataDestroyData = Tools.getString(
                    "Dialog.DrbdConfig.CreateMD.CreateNewMetadataDestroyData");
        if (getDrbdVolumeInfo().getDrbdResourceInfo().isHaveToCreateMD()) {
            final Value[] choices = {new StringValue(createNewMetadata),
                                     new StringValue(createNewMetadataDestroyData)};
            makeMDButton.setEnabled(true);
            makeMDButton.setText(
                 Tools.getString("Dialog.DrbdConfig.CreateMD.CreateMDButton"));
            metadataWi = WidgetFactory.createInstance(
                                        Widget.Type.COMBOBOX,
                                        new StringValue(createNewMetadata),
                                        choices,
                                        Widget.NO_REGEXP,
                                        COMBOBOX_WIDTH,
                                        Widget.NO_ABBRV,
                                        new AccessMode(Application.AccessType.RO,
                                                       !AccessMode.ADVANCED),
                                        Widget.NO_BUTTON);
        } else {
            final Value[] choices = {new StringValue(useExistingMetadata),
                                     new StringValue(createNewMetadata),
                                     new StringValue(createNewMetadataDestroyData)};
            makeMDButton.setEnabled(false);
            makeMDButton.setText(
               Tools.getString("Dialog.DrbdConfig.CreateMD.OverwriteMDButton"));
            String metadataDefault = useExistingMetadata;
            if (Tools.getApplication().getAutoOptionGlobal("autodrbd") != null) {
                metadataDefault = createNewMetadata;
                makeMDButton.setEnabled(true);
            }
            metadataWi = WidgetFactory.createInstance(
                                        Widget.Type.COMBOBOX,
                                        new StringValue(metadataDefault),
                                        choices,
                                        Widget.NO_REGEXP,
                                        COMBOBOX_WIDTH,
                                        Widget.NO_ABBRV,
                                        new AccessMode(Application.AccessType.RO,
                                                       !AccessMode.ADVANCED),
                                        Widget.NO_BUTTON);
        }

        inputPane.add(metadataLabel);
        inputPane.add(metadataWi.getComponent());
        metadataWi.addListeners(
            new WidgetListener() {
                @Override
                public void check(final Value value) {
                    if (metadataWi.getStringValue().equals(
                                                useExistingMetadata)) {
                        makeMDButton.setEnabled(false);
                        buttonClass(nextButton()).setEnabled(true);
                    } else {
                        buttonClass(nextButton()).setEnabled(false);
                        makeMDButton.setEnabled(true);
                    }
                }
            });

        makeMDButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getProgressBar().start(10000);
                        if (metadataWi.getStringValue().equals(
                                              createNewMetadataDestroyData)) {
                            createMetadata(true);
                        } else {
                            createMetadata(false);
                        }
                        progressBarDone();
                    }
                });
                thread.start();
            }
        });
        inputPane.add(makeMDButton);


        SpringUtilities.makeCompactGrid(inputPane, 1, 3,  // rows, cols
                                                   1, 1,  // initX, initY
                                                   1, 1); // xPad, yPad

        pane.add(inputPane);
        pane.add(getProgressBarPane(null));
        pane.add(getAnswerPane(""));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  // rows, cols
                                              0, 0,  // initX, initY
                                              0, 0); // xPad, yPad

        return pane;
    }
}
