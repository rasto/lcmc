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


package lcmc.drbd.ui.configdialog;

import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.Exceptions;
import lcmc.cluster.domain.storage.FileSystemService;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.WidgetListener;
import lcmc.drbd.ui.resource.BlockDevInfo;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * An implementation of a dialog where drbd block devices are initialized.
 * information.
 */
@Named
final class CreateFS extends DrbdConfig {
    private static final Logger LOG = LoggerFactory.getLogger(CreateFS.class);
    /**
     * No host string. (none)
     */
    private static final Value NO_HOST_STRING = new StringValue(Tools.getString("Dialog.DrbdConfig.CreateFS.NoHostString"));
    /**
     * No file system (use existing data).
     */
    private static final Value NO_FILESYSTEM_STRING =
            new StringValue(Tools.getString("Dialog.DrbdConfig.CreateFS.SelectFilesystem"));
    private static final int COMBOBOX_WIDTH = 250;
    private static final Value SKIP_SYNC_FALSE = new StringValue("false");
    private static final Value SKIP_SYNC_TRUE = new StringValue("true");
    /**
     * Pull down menu with hosts (or no host).
     */
    private Widget hostChoiceWidget;
    /**
     * Pull down menu with file systems.
     */
    private Widget filesystemWidget;
    private Widget skipInitialSyncWidget;
    private JLabel skipInitialSyncLabel;
    private final Application application;
    private final SwingUtils swingUtils;
    private final WidgetFactory widgetFactory;
    private final FileSystemService fileSystemService;
    private MyButton makeFileSystemButton;

    public CreateFS(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData,
            FileSystemService fileSystemService, Provider<ProgressBar> progressBarProvider) {
        super(application, swingUtils, widgetFactory, mainData, progressBarProvider);
        this.application = application;
        this.swingUtils = swingUtils;
        this.widgetFactory = widgetFactory;
        this.fileSystemService = fileSystemService;
    }

    /**
     * Finishes the dialog. If primary bd was choosen it is forced to be a primary.
     */
    @Override
    protected void finishDialog() {
        final BlockDevInfo bdiPri = getPrimaryBlockDevice();
        if (bdiPri != null) {
            final Application.RunMode runMode = Application.RunMode.LIVE;
            if (SKIP_SYNC_TRUE.equals(skipInitialSyncWidget.getValue())) {
                bdiPri.skipInitialFullSync(runMode);
            }
            bdiPri.forcePrimary(runMode);
        }
    }

    public BlockDevInfo getPrimaryBlockDevice() {
        final BlockDevInfo bdi1 = getDrbdVolumeInfo().getFirstBlockDevInfo();
        final BlockDevInfo bdi2 = getDrbdVolumeInfo().getSecondBlockDevInfo();
        final String h = hostChoiceWidget.getStringValue();
        if (h.equals(bdi1.getHost().getName())) {
            return bdi1;
        } else if (h.equals(bdi2.getHost().getName())) {
            return bdi2;
        } else {
            return null;
        }
    }

    public void createFilesystem() {
        final Runnable runnable = () -> {
            getProgressBar().start(1);
            answerPaneSetText(Tools.getString("Dialog.DrbdConfig.CreateFS.MakeFS"));
            swingUtils.invokeLater(() -> {
                buttonClass(finishButton()).setEnabled(false);
                makeFileSystemButton.setEnabled(false);
            });
            final BlockDevInfo bdiPri = getPrimaryBlockDevice();
            final Application.RunMode runMode = Application.RunMode.LIVE;
            if (SKIP_SYNC_TRUE.equals(skipInitialSyncWidget.getValue())) {
                bdiPri.skipInitialFullSync(runMode);
            }
            bdiPri.forcePrimary(runMode);
            final String fs = filesystemWidget.getStringValue();
            bdiPri.makeFilesystem(fs, runMode);
            if (bdiPri.getDrbdVolumeInfo() != null) {
                /* could be canceled */
                getDrbdVolumeInfo().setCreatedFs(fs);
                bdiPri.setSecondary(runMode);
                hostChoiceWidget.setValue(NO_HOST_STRING);
                filesystemWidget.setValue(NO_FILESYSTEM_STRING);
                answerPaneSetText(Tools.getString("Dialog.DrbdConfig.CreateFS.MakeFS.Done"));
            }
            progressBarDone();
        };
        final Thread thread = new Thread(runnable);
        thread.start();
    }

    @Override
    public WizardDialog nextDialog() {
        return null;
    }

    @Override
    protected String getDialogTitle() {
        return Tools.getString("Dialog.DrbdConfig.CreateFS.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.DrbdConfig.CreateFS.Description");
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        makeFileSystemButton.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
        enableComponentsLater(new JComponent[]{buttonClass(finishButton())});
    }

    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        if (application.getAutoOptionGlobal("autodrbd") != null) {
            swingUtils.invokeLater(() -> makeFileSystemButton.pressButton());
        }
    }

    /**
     * Enables and disables the make fs and finish buttons depending on what was chosen by user.
     */
    private void checkButtons() {
        final boolean noHost = hostChoiceWidget.getValue().equals(NO_HOST_STRING);
        final boolean noFileSystem = filesystemWidget.getValue().equals(NO_FILESYSTEM_STRING);
        swingUtils.invokeLater(() -> {
            if (noHost) {
                skipInitialSyncWidget.setEnabled(false);
                skipInitialSyncLabel.setEnabled(false);
                skipInitialSyncWidget.setValue(SKIP_SYNC_FALSE);
            } else {
                if (skipSyncAvailable()) {
                    skipInitialSyncWidget.setEnabled(true);
                    skipInitialSyncLabel.setEnabled(true);
                }
            }
        });
        if (noFileSystem) {
            swingUtils.invokeLater(() -> {
                buttonClass(finishButton()).setEnabled(true);
                makeFileSystemButton.setEnabled(false);
                skipInitialSyncWidget.setValue(SKIP_SYNC_FALSE);
            });
        } else if (noHost) {
            swingUtils.invokeLater(() -> buttonClass(finishButton()).setEnabled(false));
            makeFileSystemButton.setEnabled(false);
        } else {
            swingUtils.invokeLater(() -> {
                buttonClass(finishButton()).setEnabled(false);
                makeFileSystemButton.setEnabled(true);
                if (skipSyncAvailable()) {
                    skipInitialSyncWidget.setValue(SKIP_SYNC_TRUE);
                    skipInitialSyncWidget.setEnabled(true);
                }
            });
        }
    }

    @Override
    protected JComponent getInputPane() {
        makeFileSystemButton = widgetFactory.createButton(Tools.getString("Dialog.DrbdConfig.CreateFS.CreateFsButton"));
        makeFileSystemButton.setEnabled(false);
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
        final JLabel hostLabel = new JLabel(Tools.getString("Dialog.DrbdConfig.CreateFS.ChooseHost"));
        Value defaultHost = NO_HOST_STRING;
        if (application.getAutoOptionGlobal("autodrbd") != null) {
            defaultHost = hosts[1];
        }
        hostChoiceWidget = widgetFactory.createInstance(Widget.Type.COMBOBOX,
                                                        defaultHost,
                                                        hosts,
                                                        Widget.NO_REGEXP,
                                                        COMBOBOX_WIDTH,
                                                        Widget.NO_ABBRV,
                                                        new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                                                        Widget.NO_BUTTON);
        hostChoiceWidget.addListeners(new WidgetListener() {
            @Override
            public void check(final Value value) {
                checkButtons();
            }
        });
        inputPane.add(hostLabel);
        inputPane.add(hostChoiceWidget.getComponent());
        inputPane.add(new JLabel(""));

        /* Filesystem */
        final JLabel filesystemLabel = new JLabel(Tools.getString("Dialog.DrbdConfig.CreateFS.Filesystem"));
        final Value defaultValue = NO_FILESYSTEM_STRING;

        final Set<String> fileSystems =
                fileSystemService.getCommonFileSystems(getDrbdVolumeInfo().getBrowser().getCluster());
        final Value[] filesystems = Tools.getCommonFileSystemsWithDefault(fileSystems, defaultValue);

        filesystemWidget = widgetFactory.createInstance(Widget.Type.COMBOBOX,
                                                        defaultValue,
                                                        filesystems,
                                                        Widget.NO_REGEXP,
                                                        COMBOBOX_WIDTH,
                                                        Widget.NO_ABBRV,
                                                        new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                                                        Widget.NO_BUTTON);
        if (application.getAutoOptionGlobal("autodrbd") != null) {
            filesystemWidget.setValueAndWait(new StringValue("ext3"));
        }
        inputPane.add(filesystemLabel);
        inputPane.add(filesystemWidget.getComponent());
        filesystemWidget.addListeners(new WidgetListener() {
            @Override
            public void check(final Value value) {
                if (NO_HOST_STRING.equals(hostChoiceWidget.getValue())
                    && !NO_FILESYSTEM_STRING.equals(filesystemWidget.getValue())) {
                    hostChoiceWidget.setValue(hosts[1]);
                } else {
                    checkButtons();
                }
            }
        });

        makeFileSystemButton.addActionListener(e -> createFilesystem());
        inputPane.add(makeFileSystemButton);
        /* skip initial full sync */
        skipInitialSyncLabel = new JLabel(Tools.getString("Dialog.DrbdConfig.CreateFS.SkipSync"));
        skipInitialSyncLabel.setEnabled(false);
        skipInitialSyncWidget = widgetFactory.createInstance(Widget.Type.CHECKBOX,
                                                             SKIP_SYNC_FALSE,
                                                             new Value[]{SKIP_SYNC_TRUE, SKIP_SYNC_FALSE},
                                                             Widget.NO_REGEXP,
                                                             COMBOBOX_WIDTH,
                                                             Widget.NO_ABBRV,
                                                             new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                                                             Widget.NO_BUTTON);
        skipInitialSyncWidget.setEnabled(false);
        skipInitialSyncWidget.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Background.Light"));
        inputPane.add(skipInitialSyncLabel);
        inputPane.add(skipInitialSyncWidget.getComponent());
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

    private boolean skipSyncAvailable() {
        final BlockDevInfo bdi1 = getDrbdVolumeInfo().getFirstBlockDevInfo();
        final BlockDevInfo bdi2 = getDrbdVolumeInfo().getSecondBlockDevInfo();
        try {
            return bdi1.getHost().drbdVersionHigherOrEqual("8.3.2") && bdi2.getHost().drbdVersionHigherOrEqual("8.3.2");
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("skipSyncAvailable: " + e.getMessage(), e);
            return false;
        }
    }
}
