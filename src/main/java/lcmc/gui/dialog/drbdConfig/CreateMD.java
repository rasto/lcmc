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
import lcmc.gui.resources.BlockDevInfo;
import lcmc.gui.resources.DrbdVolumeInfo;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.gui.dialog.WizardDialog;
import lcmc.utilities.MyButton;
import lcmc.utilities.DRBD;
import lcmc.utilities.WidgetListener;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;

import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.regex.Matcher;

/**
 * An implementation of a dialog where drbd block devices are initialized.
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class CreateMD extends DrbdConfig {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;
    /** Metadata pulldown choices. */
    private Widget metadataWi;
    /** Make Meta-Data button. */
    private final MyButton makeMDButton = new MyButton();
    /** Width of the combo boxes. */
    private static final int COMBOBOX_WIDTH = 250;
    /** Return code of the create md command if fs is already there. */
    private static final int CREATE_MD_FS_ALREADY_THERE_RC = 40;

    /** Prepares a new <code>CreateMD</code> object. */
    CreateMD(final WizardDialog previousDialog,
                       final DrbdVolumeInfo dvi) {
        super(previousDialog, dvi);
    }

    /** Creates meta-data and checks the results. */
    private void createMetadata(final boolean destroyData) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                makeMDButton.setEnabled(false);
            }
        });
        final Thread[] thread = new Thread[2];
        final String[] answer = new String[2];
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
                            public void done(final String ans) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        makeMDButton.setEnabled(false);
                                    }
                                });
                                answer[index] = ans;
                                returnCode[index] = 0;
                            }

                            @Override
                            public void doneError(final String ans,
                                                  final int exitCode) {
                                answer[index] = ans;
                                returnCode[index] = exitCode;
                            }

                        };
                    String drbdMetaDisk =
                        getDrbdVolumeInfo().getMetaDiskForHost(
                                                        bdis[index].getHost());
                    if ("internal".equals(drbdMetaDisk)) {
                        drbdMetaDisk = bdis[index].getName();
                    }
                    final boolean testOnly = false;
                    if (destroyData) {
                        DRBD.createMDDestroyData(
                                    bdis[index].getHost(),
                                    getDrbdVolumeInfo().getDrbdResourceInfo()
                                                       .getName(),
                                    getDrbdVolumeInfo().getName(),
                                    drbdMetaDisk,
                                    execCallback,
                                    testOnly);
                    } else {
                        DRBD.createMD(bdis[index].getHost(),
                                      getDrbdVolumeInfo().getDrbdResourceInfo()
                                                         .getName(),
                                      getDrbdVolumeInfo().getName(),
                                      drbdMetaDisk,
                                      execCallback,
                                      testOnly);
                    }
                }
            });
            thread[i].start();
        }
        boolean error = false;
        for (int i = 0; i < 2; i++) {
            try {
                thread[i].join(0);
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (returnCode[i] == CREATE_MD_FS_ALREADY_THERE_RC) {
                answer[i] = Tools.getString(
                               "Dialog.DrbdConfig.CreateMD.CreateMD.Failed.40");
                error = true;
            } else if (returnCode[i] > 0) {
                answer[i] = Tools.getString(
                                  "Dialog.DrbdConfig.CreateMD.CreateMD.Failed")
                                  + answer[i];
                error = true;
            } else {
                answer[i] = Tools.getString(
                                   "Dialog.DrbdConfig.CreateMD.CreateMD.Done");
            }
            answer[i] = answer[i].replaceAll(
                         "@HOST@",
                         Matcher.quoteReplacement(bdis[i].getHost().getName()));
        }
        if (error) {
            answerPaneSetTextError(Tools.join("\n", answer));
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    makeMDButton.setEnabled(false);
                    buttonClass(nextButton()).setEnabled(true);
                    if (Tools.getConfigData().getAutoOptionGlobal(
                                                        "autodrbd") != null) {
                        pressNextButton();
                    }
                }
            });
            answerPaneSetText(Tools.join("\n", answer));
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
        final boolean testOnly = false;
        final String clusterName = bdi1.getHost().getCluster().getName();
        Tools.startProgressIndicator(clusterName, "scanning block devices...");
        if (getDrbdVolumeInfo().getDrbdResourceInfo().isProxy(bdi1.getHost())) {
            DRBD.proxyUp(bdi1.getHost(),
                         getDrbdVolumeInfo().getDrbdResourceInfo().getName(),
                         null,
                         testOnly);
        }
        if (getDrbdVolumeInfo().getDrbdResourceInfo().isProxy(bdi2.getHost())) {
            DRBD.proxyUp(bdi2.getHost(),
                         getDrbdVolumeInfo().getDrbdResourceInfo().getName(),
                         null,
                         testOnly);
        }
        DRBD.adjustApply(bdi1.getHost(),
                    getDrbdVolumeInfo().getDrbdResourceInfo().getName(),
                    getDrbdVolumeInfo().getName(),
                    testOnly);
        DRBD.adjustApply(bdi2.getHost(),
                    getDrbdVolumeInfo().getDrbdResourceInfo().getName(),
                    getDrbdVolumeInfo().getName(),
                    testOnly);
        final String device = getDrbdVolumeInfo().getDevice();
        final ClusterBrowser browser =
                        getDrbdVolumeInfo().getDrbdResourceInfo().getBrowser();
        browser.updateHWInfo(bdi1.getHost());
        browser.updateHWInfo(bdi2.getHost());
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
    protected void initDialog() {
        super.initDialog();
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
        if (Tools.getConfigData().getAutoOptionGlobal("autodrbd") != null) {
            SwingUtilities.invokeLater(new Runnable() {
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
            final String[] choices = {createNewMetadata,
                                      createNewMetadataDestroyData};
            makeMDButton.setEnabled(true);
            makeMDButton.setText(
                 Tools.getString("Dialog.DrbdConfig.CreateMD.CreateMDButton"));
            final String metadataDefault = createNewMetadata;
            metadataWi = WidgetFactory.createInstance(
                                        Widget.Type.COMBOBOX,
                                        metadataDefault,
                                        choices,
                                        Widget.NO_REGEXP,
                                        COMBOBOX_WIDTH,
                                        Widget.NO_ABBRV,
                                        new AccessMode(ConfigData.AccessType.RO,
                                                       !AccessMode.ADVANCED),
                                        Widget.NO_BUTTON);
        } else {
            final String[] choices = {useExistingMetadata,
                                      createNewMetadata,
                                      createNewMetadataDestroyData};
            makeMDButton.setEnabled(false);
            makeMDButton.setText(
               Tools.getString("Dialog.DrbdConfig.CreateMD.OverwriteMDButton"));
            String metadataDefault = useExistingMetadata;
            if (Tools.getConfigData().getAutoOptionGlobal("autodrbd") != null) {
                metadataDefault = createNewMetadata;
                makeMDButton.setEnabled(true);
            }
            metadataWi = WidgetFactory.createInstance(
                                        Widget.Type.COMBOBOX,
                                        metadataDefault,
                                        choices,
                                        Widget.NO_REGEXP,
                                        COMBOBOX_WIDTH,
                                        Widget.NO_ABBRV,
                                        new AccessMode(ConfigData.AccessType.RO,
                                                       !AccessMode.ADVANCED),
                                        Widget.NO_BUTTON);
        }

        inputPane.add(metadataLabel);
        inputPane.add(metadataWi);
        metadataWi.addListeners(
            new WidgetListener() {
                @Override
                public void check(final Object value) {
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
