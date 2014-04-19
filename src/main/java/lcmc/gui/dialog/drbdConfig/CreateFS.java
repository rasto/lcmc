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

import lcmc.Exceptions;
import lcmc.utilities.Tools;
import lcmc.data.Host;
import lcmc.data.Application;
import lcmc.data.AccessMode;
import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.gui.resources.drbd.VolumeInfo;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.gui.dialog.WizardDialog;
import lcmc.utilities.MyButton;
import lcmc.utilities.WidgetListener;

import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import lcmc.data.StringValue;
import lcmc.data.Value;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * An implementation of a dialog where drbd block devices are initialized.
 * information.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class CreateFS extends DrbdConfig {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(CreateFS.class);
    /** No host string. (none) */
    private static final Value NO_HOST_STRING =
                    new StringValue(Tools.getString("Dialog.DrbdConfig.CreateFS.NoHostString"));
    /** No file system (use existing data). */
    private static final Value NO_FILESYSTEM_STRING =
                new StringValue(Tools.getString("Dialog.DrbdConfig.CreateFS.SelectFilesystem"));
    /** Width of the combo boxes. */
    private static final int COMBOBOX_WIDTH = 250;
    /** Skip sync false. */
    private static final Value SKIP_SYNC_FALSE = new StringValue("false");
    /** Skip sync true. */
    private static final Value SKIP_SYNC_TRUE = new StringValue("true");
    /** Pull down menu with hosts (or no host). */
    private Widget hostW;
    /** Pull down menu with file systems. */
    private Widget filesystemW;
    /** Whether to skip the initial full sync. */
    private Widget skipSyncW;
    /** Whether to skip the initial full sync label. */
    private JLabel skipSyncLabel;
    /** Make file system button. */
    private final MyButton makeFsButton = new MyButton(
                Tools.getString("Dialog.DrbdConfig.CreateFS.CreateFsButton"));

    /** Prepares a new {@code CreateFS} object. */
    CreateFS(final WizardDialog previousDialog,
             final VolumeInfo volumeInfo) {
        super(previousDialog, volumeInfo);
    }

    /**
     * Finishes the dialog. If primary bd was choosen it is forced to be a
     * primary.
     */
    @Override
    protected void finishDialog() {
        final BlockDevInfo bdiPri = getPrimaryBD();
        if (bdiPri != null) {
            final Application.RunMode runMode = Application.RunMode.LIVE;
            if (SKIP_SYNC_TRUE.equals(skipSyncW.getValue())) {
                bdiPri.skipInitialFullSync(runMode);
            }
            bdiPri.forcePrimary(runMode);
        }
    }

    /** Returns the primary block device. */
    protected BlockDevInfo getPrimaryBD() {
        final BlockDevInfo bdi1 = getDrbdVolumeInfo().getFirstBlockDevInfo();
        final BlockDevInfo bdi2 = getDrbdVolumeInfo().getSecondBlockDevInfo();
        final String h = hostW.getStringValue();
        if (h.equals(bdi1.getHost().getName())) {
            return bdi1;
        } else if (h.equals(bdi2.getHost().getName())) {
            return bdi2;
        } else {
            return null;
        }
    }

    /** Creates the file system. */
    protected void createFilesystem() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                getProgressBar().start(1);
                answerPaneSetText(
                        Tools.getString("Dialog.DrbdConfig.CreateFS.MakeFS"));
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        buttonClass(finishButton()).setEnabled(false);
                        makeFsButton.setEnabled(false);
                    }
                });
                final BlockDevInfo bdiPri = getPrimaryBD();
                final Application.RunMode runMode = Application.RunMode.LIVE;
                if (SKIP_SYNC_TRUE.equals(skipSyncW.getValue())) {
                    bdiPri.skipInitialFullSync(runMode);
                }
                bdiPri.forcePrimary(runMode);
                final String fs = filesystemW.getStringValue();
                bdiPri.makeFilesystem(fs, runMode);
                if (bdiPri.getDrbdVolumeInfo() != null) {
                    /* could be canceled */
                    getDrbdVolumeInfo().setCreatedFs(fs);
                    bdiPri.setSecondary(runMode);
                    hostW.setValue(NO_HOST_STRING);
                    filesystemW.setValue(NO_FILESYSTEM_STRING);
                    answerPaneSetText(
                     Tools.getString("Dialog.DrbdConfig.CreateFS.MakeFS.Done"));
                }
                progressBarDone();
            }
        };
        final Thread thread = new Thread(runnable);
        thread.start();
    }

    /** Returns the next dialog, null in this dialog. */
    @Override
    public WizardDialog nextDialog() {
        return null;
    }

    /**
     * Returns title of the dialog.
     * It is defined in TextResources as "Dialog.DrbdConfig.CreateFS.Title"
     */
    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfig.CreateFS.Title");
    }

    /**
     * Returns description of the dialog.
     * It is defined in TextResources as
     * "Dialog.DrbdConfig.CreateFS.Description"
     */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.DrbdConfig.CreateFS.Description");
    }

    /** Inits dialog. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        makeFsButton.setBackgroundColor(
                               Tools.getDefaultColor("ConfigDialog.Button"));
        enableComponentsLater(new JComponent[]{buttonClass(finishButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        if (Tools.getApplication().getAutoOptionGlobal("autodrbd") != null) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    makeFsButton.pressButton();
                }
            });
        }
    }

    /**
     * Enables and disables the make fs and finish buttons depending on what
     * was chosen by user.
     */
    protected void checkButtons() {
        final boolean noHost = hostW.getValue().equals(NO_HOST_STRING);
        final boolean noFileSystem = filesystemW.getValue().equals(
                                                        NO_FILESYSTEM_STRING);
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (noHost) {
                    skipSyncW.setEnabled(false);
                    skipSyncLabel.setEnabled(false);
                    skipSyncW.setValue(SKIP_SYNC_FALSE);
                } else {
                    if (skipSyncAvailable()) {
                        skipSyncW.setEnabled(true);
                        skipSyncLabel.setEnabled(true);
                    }
                }
            }
        });
        if (noFileSystem) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(finishButton()).setEnabled(true);
                    makeFsButton.setEnabled(false);
                    skipSyncW.setValue(SKIP_SYNC_FALSE);
                }
            });
        } else if (noHost) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(finishButton()).setEnabled(false);
                }
            });
            makeFsButton.setEnabled(false);
        } else {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    buttonClass(finishButton()).setEnabled(false);
                    makeFsButton.setEnabled(true);
                    if (skipSyncAvailable()) {
                        skipSyncW.setValue(SKIP_SYNC_TRUE);
                        skipSyncW.setEnabled(true);
                    }
                }
            });
        }
    }

    /**
     * Returns input pane, where file system can be created on the selected
     * host.
     */
    @Override
    protected JComponent getInputPane() {
        makeFsButton.setEnabled(false);
        final JPanel pane = new JPanel(new SpringLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());

        /* host */
        final Value[] hosts = new Value[3];
        hosts[0] = NO_HOST_STRING;
        int i = 1;
        for (final Host host : getDrbdVolumeInfo().getHosts()) {
            hosts[i] = host;
            i++;
        }
        final JLabel hostLabel = new JLabel(
                    Tools.getString("Dialog.DrbdConfig.CreateFS.ChooseHost"));
        Value defaultHost = NO_HOST_STRING;
        if (Tools.getApplication().getAutoOptionGlobal("autodrbd") != null) {
            defaultHost = hosts[1];
        }
        hostW = WidgetFactory.createInstance(
                                       Widget.Type.COMBOBOX,
                                       defaultHost,
                                       hosts,
                                       Widget.NO_REGEXP,
                                       COMBOBOX_WIDTH,
                                       Widget.NO_ABBRV,
                                       new AccessMode(Application.AccessType.RO,
                                                      !AccessMode.ADVANCED),
                                       Widget.NO_BUTTON);
        hostW.addListeners(new WidgetListener() {
                                @Override
                                public void check(final Value value) {
                                    checkButtons();
                                }
                            });
        inputPane.add(hostLabel);
        inputPane.add(hostW.getComponent());
        inputPane.add(new JLabel(""));

        /* Filesystem */
        final JLabel filesystemLabel = new JLabel(
                    Tools.getString("Dialog.DrbdConfig.CreateFS.Filesystem"));
        final Value defaultValue = NO_FILESYSTEM_STRING;
        final Value[] filesystems =
            getDrbdVolumeInfo().getDrbdResourceInfo().getCommonFileSystems(
                                                                defaultValue);

        filesystemW = WidgetFactory.createInstance(
                                     Widget.Type.COMBOBOX,
                                     defaultValue,
                                     filesystems,
                                     Widget.NO_REGEXP,
                                     COMBOBOX_WIDTH,
                                     Widget.NO_ABBRV,
                                     new AccessMode(Application.AccessType.RO,
                                                    !AccessMode.ADVANCED),
                                     Widget.NO_BUTTON);
        if (Tools.getApplication().getAutoOptionGlobal("autodrbd") != null) {
            filesystemW.setValueAndWait(new StringValue("ext3"));
        }
        inputPane.add(filesystemLabel);
        inputPane.add(filesystemW.getComponent());
        filesystemW.addListeners(new WidgetListener() {
                            @Override
                            public void check(final Value value) {
                                if (NO_HOST_STRING.equals(
                                                hostW.getValue())
                                    && !NO_FILESYSTEM_STRING.equals(
                                            filesystemW.getValue())) {
                                    hostW.setValue(hosts[1]);
                                } else {
                                    checkButtons();
                                }
                            }
                        });

        makeFsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                createFilesystem();
            }
        });
        inputPane.add(makeFsButton);
        /* skip initial full sync */
        skipSyncLabel = new JLabel(
                    Tools.getString("Dialog.DrbdConfig.CreateFS.SkipSync"));
        skipSyncLabel.setEnabled(false);
        skipSyncW = WidgetFactory.createInstance(
                                      Widget.Type.CHECKBOX,
                                      SKIP_SYNC_FALSE,
                                      new Value[]{SKIP_SYNC_TRUE,
                                                  SKIP_SYNC_FALSE},
                                      Widget.NO_REGEXP,
                                      COMBOBOX_WIDTH,
                                      Widget.NO_ABBRV,
                                      new AccessMode(Application.AccessType.RO,
                                                     !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);
        skipSyncW.setEnabled(false);
        skipSyncW.setBackgroundColor(
                       Tools.getDefaultColor("ConfigDialog.Background.Light"));
        inputPane.add(skipSyncLabel);
        inputPane.add(skipSyncW.getComponent());
        inputPane.add(new JLabel(""));

        SpringUtilities.makeCompactGrid(inputPane, 3, 3,  // rows, cols
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

    /** Returns whether skip sync is available. */
    private boolean skipSyncAvailable() {
        final BlockDevInfo bdi1 = getDrbdVolumeInfo().getFirstBlockDevInfo();
        final BlockDevInfo bdi2 = getDrbdVolumeInfo().getSecondBlockDevInfo();
        try {
            return Tools.compareVersions(
                                bdi1.getHost().getDrbdVersion(), "8.3.2") >= 0
                   && Tools.compareVersions(
                                bdi2.getHost().getDrbdVersion(), "8.3.2") >= 0;
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("skipSyncAvailable: " + e.getMessage(), e);
            return false;
        }
    }
}
