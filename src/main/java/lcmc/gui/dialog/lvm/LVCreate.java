/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009 - 2011, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011, Rastislav Levrinc
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

import lcmc.gui.Browser;
import lcmc.gui.GuiComboBox;
import lcmc.gui.SpringUtilities;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import lcmc.utilities.LVM;
import lcmc.data.Host;
import lcmc.data.Cluster;
import lcmc.data.AccessMode;
import lcmc.data.ConfigData;

import java.util.Map;
import java.util.Set;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SpringLayout;

/** Create LV dialog. */
public final class LVCreate extends LV {
    /** Block device info object. */
    private final Host host;
    /** Create button. */
    private final MyButton createButton = new MyButton("Create");
    /** Name of the logical volume. */
    private GuiComboBox lvNameCB;
    /** New size of the logical volume. */
    private GuiComboBox sizeCB;
    /** Maximum size of the logical volume. */
    private GuiComboBox maxSizeCB;
    /** Volume group. */
    private final String volumeGroup;
    /** Checkboxes with all hosts in the cluster. */
    private Map<Host, JCheckBox> hostCheckBoxes = null;
    /** Description create LV. */
    private static final String LV_CREATE_DESCRIPTION =
                       "Create a logical volume in an existing volume group.";
    /** LV create timeout. */
    private static final int CREATE_TIMEOUT = 5000;
    /** Create new LVCreate object. */
    public LVCreate(final Host host, final String volumeGroup) {
        super(null);
        this.host = host;
        this.volumeGroup = volumeGroup;
    }

    /** Finishes the dialog and sets the information. */
    protected void finishDialog() {
        /* disable finish button */
    }

    /** Returns the title of the dialog. */
    protected String getDialogTitle() {
        return "Create LV";
    }

    /** Returns the description of the dialog. */
    protected String getDescription() {
        return LV_CREATE_DESCRIPTION;
    }

    /** Close button. */
    public String cancelButton() {
        return "Close";
    }

    /** Inits the dialog. */
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{});
    }

    /** Inits the dialog after it becomes visible. */
    protected void initDialogAfterVisible() {
        enableComponents();
        makeDefaultAndRequestFocusLater(sizeCB);
        makeDefaultButton(createButton);
    }

    /** Enables and disabled buttons. */
    protected void checkButtons() {
        SwingUtilities.invokeLater(new EnableCreateRunnable(true));
    }

    private class EnableCreateRunnable implements Runnable {
        private final boolean enable;
        public EnableCreateRunnable(final boolean enable) {
            super();
            this.enable = enable;
        }

        @Override
        public void run() {
            boolean e = enable;
            if (enable) {
                final String maxBlockSize = getMaxBlockSize();
                final long maxSize = Long.parseLong(maxBlockSize);
                maxSizeCB.setValue(Tools.convertKilobytes(maxBlockSize));
                final long size = Tools.convertToKilobytes(
                                                  sizeCB.getStringValue());
                if (size > maxSize || size <= 0) {
                    e = false;
                    sizeCB.wrongValue();
                } else {
                    sizeCB.setBackground("", "", true);
                }
                boolean lvNameCorrect = true;
                if ("".equals(lvNameCB.getStringValue())) {
                    lvNameCorrect = false;
                } else if (hostCheckBoxes != null) {
                    for (final Host h : hostCheckBoxes.keySet()) {
                        if (hostCheckBoxes.get(h).isSelected()) {
                            final Set<String> lvs =
                                h.getLogicalVolumesFromVolumeGroup(
                                                              volumeGroup);
                            if (lvs != null
                                && lvs.contains(
                                        lvNameCB.getStringValue())) {
                                lvNameCorrect = false;
                                break;
                            }
                        }
                    }
                }
                if (lvNameCorrect) {
                    lvNameCB.setBackground("", "", true);
                } else {
                    e = false;
                    lvNameCB.wrongValue();
                }
            }
            createButton.setEnabled(e);
        }
    }

    /** Returns the input pane. */
    protected JComponent getInputPane() {
        createButton.setEnabled(false);
        final JPanel pane = new JPanel(new SpringLayout());
        /* name, size etc. */
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setBackground(Browser.BUTTON_PANEL_BACKGROUND);

        inputPane.add(new JLabel("Group"));
        inputPane.add(new JLabel(volumeGroup));
        inputPane.add(new JLabel());
        /* find next free logical volume name */
        String defaultName;
        final Set<String> logicalVolumes =
                            host.getLogicalVolumesFromVolumeGroup(volumeGroup);
        int i = 0;
        while (true) {
            defaultName = "lvol" + i;
            if (logicalVolumes == null
                || !logicalVolumes.contains(defaultName)) {
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
        lvNameCB.addListeners(null, new SizeDocumentListener());

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
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Tools.invokeAndWait(new EnableCreateRunnable(false));
                        disableComponents();
                        getProgressBar().start(CREATE_TIMEOUT
                                               * hostCheckBoxes.size());
                        boolean oneFailed = false;
                        for (final Host h : hostCheckBoxes.keySet()) {
                            if (hostCheckBoxes.get(h).isSelected()) {
                                final boolean ret = lvCreate(
                                                      h,
                                                      lvNameCB.getStringValue(),
                                                      sizeCB.getStringValue());
                                if (!ret) {
                                    oneFailed = true;
                                }
                            }
                        }
                        for (final Host h : hostCheckBoxes.keySet()) {
                            h.getBrowser().getClusterBrowser().updateHWInfo(h);
                        }
                        final String maxBlockSize = getMaxBlockSize();
                        final long maxSize = Long.parseLong(maxBlockSize);
                        maxSizeCB.setValue(Tools.convertKilobytes(
                                                                maxBlockSize));
                        enableComponents();
                        if (oneFailed) {
                            progressBarDoneError();
                        } else {
                            progressBarDone();
                        }
                    }
                });
                thread.start();
            }
        });
        inputPane.add(createButton);
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
        sizeCB.addListeners(new ItemChangeListener(false),
                            new SizeDocumentListener());

        SpringUtilities.makeCompactGrid(inputPane, 4, 3,  /* rows, cols */
                                                   1, 1,  /* initX, initY */
                                                   1, 1); /* xPad, yPad */

        pane.add(inputPane);
        final JPanel hostsPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final Cluster cluster = host.getCluster();
        hostCheckBoxes = Tools.getHostCheckBoxes(cluster);
        hostsPane.add(new JLabel("Select Hosts: "));
        for (final Host h : hostCheckBoxes.keySet()) {
            hostCheckBoxes.get(h).addItemListener(new ItemChangeListener(true));
            if (host == h) {
                hostCheckBoxes.get(h).setEnabled(false);
                hostCheckBoxes.get(h).setSelected(true);
            } else if (!h.getVolumeGroupNames().contains(volumeGroup)) {
                hostCheckBoxes.get(h).setEnabled(false);
                hostCheckBoxes.get(h).setSelected(false);
            } else {
                hostCheckBoxes.get(h).setEnabled(true);
                hostCheckBoxes.get(h).setSelected(false);
            }
            hostsPane.add(hostCheckBoxes.get(h));
        }
        final JScrollPane sp = new JScrollPane(hostsPane);
        sp.setPreferredSize(new Dimension(0, 45));
        pane.add(sp);
        pane.add(getProgressBarPane(null));
        pane.add(getAnswerPane(""));
        SpringUtilities.makeCompactGrid(pane, 4, 1,  /* rows, cols */
                                              0, 0,  /* initX, initY */
                                              0, 0); /* xPad, yPad */
        checkButtons();
        return pane;
    }

    /** Size combo box item listener. */
    private class ItemChangeListener implements ItemListener {
        /** Whether to check buttons on both select and deselect. */
        private final boolean onDeselect;
        /** Create ItemChangeListener object. */
        public ItemChangeListener(final boolean onDeselect) {
            super();
            this.onDeselect = onDeselect;
        }
        @Override
        public void itemStateChanged(final ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED
                || onDeselect) {
                checkButtons();
            }
        }
    }

    /** Size combo box action listener. */
    private class SizeDocumentListener implements DocumentListener {
        /** Check. */
        private void check() {
            checkButtons();
        }

        @Override
        public void insertUpdate(final DocumentEvent e) {
            check();
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
            check();
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {
            check();
        }
    }

    /** Create LV. */
    private boolean lvCreate(final Host host,
                             final String lvName,
                             final String size) {
        final boolean ret = LVM.lvCreate(host,
                                         lvName,
                                         volumeGroup,
                                         size,
                                         false);
        if (ret) {
            answerPaneAddText("Logical volume "
                              + lvName
                              + " was successfully created in "
                              + volumeGroup
                              + " on " + host.getName() + ".");
        } else {
            answerPaneAddTextError("Creating of logical volume "
                                   + lvName
                                   + " failed.");
        }
        return ret;
    }

    /** Returns maximum block size available in the group. */
    private String getMaxBlockSize() {
        long free = host.getFreeInVolumeGroup(volumeGroup) / 1024;
        if (hostCheckBoxes != null) {
            for (final Host h : hostCheckBoxes.keySet()) {
                if (hostCheckBoxes.get(h).isSelected()
                    && h.getFreeInVolumeGroup(volumeGroup) / 1024 < free) {
                    free = h.getFreeInVolumeGroup(volumeGroup) / 1024;
                }
            }
        }
        return Long.toString(free);
    }
}

