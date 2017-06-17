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

package lcmc.lvm.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.common.domain.StringValue;
import lcmc.vm.domain.VmsXml;
import lcmc.common.domain.Value;
import lcmc.common.ui.Browser;
import lcmc.common.ui.SpringUtilities;
import lcmc.drbd.ui.resource.BlockDevInfo;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.WidgetListener;

/**
 * This class implements LVM snapshot dialog.
 */
@Named
public final class LVSnapshot extends LV {
    private static final int SNAPSHOT_TIMEOUT = 5000;
    private static final String SNAPSHOT_DESCRIPTION = "Create a snapshot of the logical volume.";
    private BlockDevInfo blockDevInfo;
    private Widget lvNameWi;
    private Widget sizeWi;
    private Widget maxSizeWi;
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private WidgetFactory widgetFactory;
    private MyButton snapshotButton;

    public void init(final BlockDevInfo blockDevInfo) {
        super.init(null);
        this.blockDevInfo = blockDevInfo;
    }

    @Override
    protected String getDialogTitle() {
        return "LV Snapshot ";
    }

    @Override
    protected String getDescription() {
        return SNAPSHOT_DESCRIPTION;
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{});
    }

    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        makeDefaultAndRequestFocusLater(sizeWi.getComponent());
    }

    protected void checkButtons() {
        swingUtils.invokeLater(new EnableSnapshotRunnable(true));
    }

    private void setComboBoxes() {
        final String maxBlockSize = getMaxBlockSizeAvailableInGroup();
        sizeWi.setValue(VmsXml.convertKilobytes(Long.toString(Long.parseLong(maxBlockSize) / 2)));
        maxSizeWi.setValue(VmsXml.convertKilobytes(maxBlockSize));
    }

    @Override
    protected JComponent getInputPane() {
        snapshotButton = widgetFactory.createButton("Create Snapshot");
        snapshotButton.setEnabled(false);
        final JPanel pane = new JPanel(new SpringLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setBackground(Browser.BUTTON_PANEL_BACKGROUND);

        final String volumeGroup = blockDevInfo.getBlockDevice().getVolumeGroup();
        inputPane.add(new JLabel("Group"));
        inputPane.add(new JLabel(volumeGroup));
        inputPane.add(new JLabel());
        /* find next free logical volume name */
        String defaultName;
        final Set<String> volumeGroups = blockDevInfo.getHost().getHostParser().getLogicalVolumesFromVolumeGroup(volumeGroup);
        int i = 0;
        while (true) {
            defaultName = "lvol" + i;
            if (volumeGroups == null || !volumeGroups.contains(defaultName)) {
                break;
            }
            i++;
        }
        lvNameWi = widgetFactory.createInstance(
                                      Widget.Type.TEXTFIELD,
                                      new StringValue(defaultName),
                                      Widget.NO_ITEMS,
                                      Widget.NO_REGEXP,
                                      250,
                                      Widget.NO_ABBRV,
                                      new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                                      Widget.NO_BUTTON);
        inputPane.add(new JLabel("LV Name"));
        inputPane.add(lvNameWi.getComponent());
        inputPane.add(new JLabel());
        lvNameWi.addListeners(new WidgetListener() {
                                  @Override
                                  public void check(final Value value) {
                                      checkButtons();
                                  }
                              });

        final String maxBlockSize = getMaxBlockSizeAvailableInGroup();
        /* size */
        final String newBlockSize = Long.toString(Long.parseLong(maxBlockSize) / 2);
        final JLabel sizeLabel = new JLabel("New Size");

        sizeWi =  widgetFactory.createInstance(
                       Widget.Type.TEXTFIELDWITHUNIT,
                       VmsXml.convertKilobytes(newBlockSize),
                       Widget.NO_ITEMS,
                       getUnits(),
                       Widget.NO_REGEXP,
                       250,
                       Widget.NO_ABBRV,
                       new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                       Widget.NO_BUTTON);
        inputPane.add(sizeLabel);
        inputPane.add(sizeWi.getComponent());
        snapshotButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        swingUtils.invokeAndWait(new EnableSnapshotRunnable(false));
                        disableComponents();
                        getProgressBar().start(SNAPSHOT_TIMEOUT);
                        final boolean ret = lvSnapshot(lvNameWi.getStringValue(), sizeWi.getStringValue());
                        final Host host = blockDevInfo.getHost();
                        host.getBrowser().getClusterBrowser().updateHWInfo(host, Host.UPDATE_LVM);
                        setComboBoxes();
                        if (ret) {
                            progressBarDone();
                            disposeDialog();
                        } else {
                            progressBarDoneError();
                        }
                        enableComponents();
                    }
                });
                thread.start();
            }
        });

        inputPane.add(snapshotButton);
        /* max size */
        final JLabel maxSizeLabel = new JLabel("Max Size");
        maxSizeLabel.setEnabled(false);
        maxSizeWi =  widgetFactory.createInstance(
                        Widget.Type.TEXTFIELDWITHUNIT,
                        VmsXml.convertKilobytes(maxBlockSize),
                        Widget.NO_ITEMS,
                        getUnits(),
                        Widget.NO_REGEXP,
                        250,
                        Widget.NO_ABBRV,
                        new AccessMode(AccessMode.OP, AccessMode.NORMAL),
                        Widget.NO_BUTTON);
        maxSizeWi.setEnabled(false);
        inputPane.add(maxSizeLabel);
        inputPane.add(maxSizeWi.getComponent());
        inputPane.add(new JLabel());
        sizeWi.addListeners(new WidgetListener() {
                                @Override
                                public void check(final Value value) {
                                    checkButtons();
                                }
                            });

        SpringUtilities.makeCompactGrid(inputPane, 4, 3,  /* rows, cols */
                                                   1, 1,  /* initX, initY */
                                                   1, 1); /* xPad, yPad */

        pane.add(inputPane);
        pane.add(getProgressBarPane(null));
        pane.add(getAnswerPane(""));
        SpringUtilities.makeCompactGrid(pane, 3, 1,  /* rows, cols */
                                              0, 0,  /* initX, initY */
                                              0, 0); /* xPad, yPad */
        checkButtons();
        return pane;
    }

    private boolean lvSnapshot(final String lvName, final String size) {
        final String volumeGroup = blockDevInfo.getBlockDevice().getVolumeGroup();
        final boolean ret = blockDevInfo.lvSnapshot(lvName, size, Application.RunMode.LIVE);
        if (ret) {
            answerPaneSetText("Logical volume " + lvName + " was successfully created on " + volumeGroup + '.');
        } else {
            answerPaneSetTextError("Creating of logical volume " + lvName + " failed.");
        }
        return ret;
    }

    private String getMaxBlockSizeAvailableInGroup() {
        final String volumeGroup = blockDevInfo.getBlockDevice().getVolumeGroup();
        final long free = blockDevInfo.getHost().getHostParser().getFreeInVolumeGroup(volumeGroup) / 1024;
        return Long.toString(free);
    }

    private class EnableSnapshotRunnable implements Runnable {
        private final boolean enable;
        EnableSnapshotRunnable(final boolean enable) {
            super();
            this.enable = enable;
        }

        @Override
        public void run() {
            boolean e = enable;
            if (enable) {
                final long size = VmsXml.convertToKilobytes(sizeWi.getValue());
                final long maxSize = VmsXml.convertToKilobytes(maxSizeWi.getValue());
                if (size > maxSize) {
                    e = false;
                } else if (size <= 0) {
                    e = false;
                } else {
                    final Set<String> lvs = blockDevInfo.getHost().getHostParser().getLogicalVolumesFromVolumeGroup(
                                                                      blockDevInfo.getBlockDevice().getVolumeGroup());
                    if (lvs != null && lvs.contains(lvNameWi.getStringValue())) {
                        e = false;
                    }
                }
            }
            snapshotButton.setEnabled(e);
        }
    }
}
