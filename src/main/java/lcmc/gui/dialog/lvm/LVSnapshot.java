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

package lcmc.gui.dialog.lvm;

import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.BlockDevInfo;

import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;
import lcmc.utilities.WidgetListener;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.data.Host;
import lcmc.gui.GuiComboBox;
import lcmc.gui.Browser;

import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
/**
 * This class implements LVM snapshot dialog.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class LVSnapshot extends LV {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** LV Snapshot timeout. */
    private static final int SNAPSHOT_TIMEOUT = 5000;
    /** Block device info object. */
    private final BlockDevInfo blockDevInfo;
    private final MyButton snapshotButton = new MyButton("Create Snapshot");
    private GuiComboBox lvNameCB;
    private GuiComboBox sizeCB;
    private GuiComboBox maxSizeCB;
    /** Description LV snapshot. */
    private static final String SNAPSHOT_DESCRIPTION =
                                    "Create a snapshot of the logical volume.";
    /** Create new LVSnapshot object. */
    public LVSnapshot(final BlockDevInfo blockDevInfo) {
        super(null);
        this.blockDevInfo = blockDevInfo;
    }

    /** Finishes the dialog and sets the information. */
    @Override
    protected void finishDialog() {
        /* disable finish button */
    }

    /** Returns the title of the dialog. */
    @Override
    protected String getDialogTitle() {
        return "LV Snapshot ";
    }

    /** Returns the description of the dialog. */
    @Override
    protected String getDescription() {
        return SNAPSHOT_DESCRIPTION;
    }

    /** Inits the dialog. */
    @Override
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        makeDefaultAndRequestFocusLater(sizeCB);
    }

    /** Enables and disabled buttons. */
    protected void checkButtons() {
        SwingUtilities.invokeLater(new EnableSnapshotRunnable(true));
    }

    private class EnableSnapshotRunnable implements Runnable {
        private final boolean enable;
        public EnableSnapshotRunnable(final boolean enable) {
            super();
            this.enable = enable;
        }

        @Override
        public void run() {
            boolean e = enable;
            if (enable) {
                final long size = Tools.convertToKilobytes(
                                                  sizeCB.getStringValue());
                final long maxSize = Tools.convertToKilobytes(
                                               maxSizeCB.getStringValue());
                if (size > maxSize) {
                    e = false;
                } else if (size <= 0) {
                    e = false;
                } else {
                    final Set<String> lvs =
                        blockDevInfo.getHost()
                                    .getLogicalVolumesFromVolumeGroup(
                           blockDevInfo.getBlockDevice().getVolumeGroup());
                    if (lvs != null
                        && lvs.contains(lvNameCB.getStringValue())) {
                        e = false;
                    }
                }
            }
            snapshotButton.setEnabled(e);
        }
    }

    private void setComboBoxes() {
        final String maxBlockSize = getMaxBlockSize();
        sizeCB.setValue(Tools.convertKilobytes(Long.toString(
                                     Long.parseLong(maxBlockSize) / 2)));
        maxSizeCB.setValue(Tools.convertKilobytes(maxBlockSize));
    }

    /** Returns the input pane. */
    protected JComponent getInputPane() {
        snapshotButton.setEnabled(false);
        final JPanel pane = new JPanel(new SpringLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setBackground(Browser.BUTTON_PANEL_BACKGROUND);

        final String volumeGroup =
                            blockDevInfo.getBlockDevice().getVolumeGroup();
        inputPane.add(new JLabel("Group"));
        inputPane.add(new JLabel(volumeGroup));
        inputPane.add(new JLabel());
        /* find next free logical volume name */
        String defaultName;
        final Set<String> volumeGroups =
               blockDevInfo.getHost().getLogicalVolumesFromVolumeGroup(
                                                              volumeGroup);
        int i = 0;
        while (true) {
            defaultName = "lvol" + i;
            if (volumeGroups == null
                || !volumeGroups.contains(defaultName)) {
                break;
            }
            i++;
        }
        lvNameCB = new GuiComboBox(defaultName,
                                   null,
                                   null, /* units */
                                   GuiComboBox.Type.TEXTFIELD,
                                   null, /* regexp */
                                   250,
                                   null, /* abbrv */
                                   new AccessMode(ConfigData.AccessType.OP,
                                                  false)); /* only adv. */
        inputPane.add(new JLabel("LV Name"));
        inputPane.add(lvNameCB);
        inputPane.add(new JLabel());
        lvNameCB.addListeners(new WidgetListener() {
                                  @Override
                                  public void check(final Object value) {
                                      checkButtons();
                                  }
                              });

        final String maxBlockSize = getMaxBlockSize();
        /* size */
        final String newBlockSize = Long.toString(
                                      Long.parseLong(maxBlockSize) / 2);
        final JLabel sizeLabel = new JLabel("New Size");

        sizeCB = new GuiComboBox(Tools.convertKilobytes(newBlockSize),
                                 null,
                                 getUnits(), /* units */
                                 GuiComboBox.Type.TEXTFIELDWITHUNIT,
                                 null, /* regexp */
                                 250,
                                 null, /* abbrv */
                                 new AccessMode(ConfigData.AccessType.OP,
                                                  false)); /* only adv. */
        inputPane.add(sizeLabel);
        inputPane.add(sizeCB);
        snapshotButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Tools.invokeAndWait(new EnableSnapshotRunnable(false));
                        disableComponents();
                        getProgressBar().start(SNAPSHOT_TIMEOUT);
                        final boolean ret = lvSnapshot(
                                                    lvNameCB.getStringValue(),
                                                    sizeCB.getStringValue());
                        final Host host = blockDevInfo.getHost();
                        host.getBrowser().getClusterBrowser().updateHWInfo(
                                                                         host);
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
        maxSizeCB = new GuiComboBox(Tools.convertKilobytes(maxBlockSize),
                                    null,
                                    getUnits(),
                                    GuiComboBox.Type.TEXTFIELDWITHUNIT,
                                    null, /* regexp */
                                    250,
                                    null, /* abbrv */
                                    new AccessMode(ConfigData.AccessType.OP,
                                                   false)); /* only adv. */
        maxSizeCB.setEnabled(false);
        inputPane.add(maxSizeLabel);
        inputPane.add(maxSizeCB);
        inputPane.add(new JLabel());
        sizeCB.addListeners(new WidgetListener() {
                                @Override
                                public void check(final Object value) {
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

    /** LV Snapshot. */
    private boolean lvSnapshot(final String lvName, final String size) {
        final String volumeGroup =
                            blockDevInfo.getBlockDevice().getVolumeGroup();
        final boolean ret = blockDevInfo.lvSnapshot(lvName,
                                                    size,
                                                    false);
        if (ret) {
            answerPaneSetText("Logical volume "
                              + lvName
                              + " was successfully created on "
                              + volumeGroup + ".");
        } else {
            answerPaneSetTextError("Creating of logical volume "
                                   + lvName
                                   + " failed.");
        }
        return ret;
    }

    /** Returns maximum block size available in the group. */
    private String getMaxBlockSize() {
        final String volumeGroup =
                            blockDevInfo.getBlockDevice().getVolumeGroup();
        final long free =
           blockDevInfo.getHost().getFreeInVolumeGroup(volumeGroup) / 1024;
        return Long.toString(free);
    }
}
