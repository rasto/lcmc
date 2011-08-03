/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009 - 2011, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011, Rastislav Levrinc

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

package drbd.gui.dialog.lvm;

import drbd.data.Host;
import drbd.data.AccessMode;
import drbd.data.ConfigData;
import drbd.data.Cluster;
import drbd.gui.Browser;
import drbd.gui.GuiComboBox;
import drbd.gui.resources.BlockDevInfo;
import drbd.gui.SpringUtilities;
import drbd.utilities.MyButton;
import drbd.utilities.Tools;
import drbd.utilities.LVM;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

/** Create VG dialog. */
public final class VGCreate extends LV {
    final Host host;
    /** Selected block device, can be null */
    final BlockDevInfo selectedBlockDevInfo;
    private final MyButton createButton = new MyButton("Create VG");
    private GuiComboBox vgNameCB;
    private Map<Host, JCheckBox> hostCheckBoxes = null;
    private Map<String, JCheckBox> pvCheckBoxes = null;
    /** Description create VG. */
    private static final String VG_CREATE_DESCRIPTION =
                                                    "Create a volume group.";
    /** VG create timeout. */
    private static final int CREATE_TIMEOUT = 5000;
    /** Create new VGCreate object. */
    public VGCreate(final Host host,
                    final BlockDevInfo selectedBlockDevInfo) {
        super(null);
        this.host = host;
        this.selectedBlockDevInfo = selectedBlockDevInfo;
    }

    /** Finishes the dialog and sets the information. */
    protected void finishDialog() {
        /* disable finish button */
    }

    /** Returns the title of the dialog. */
    protected String getDialogTitle() {
        return "Create VG";
    }

    /** Returns the description of the dialog. */
    protected String getDescription() {
        return VG_CREATE_DESCRIPTION;
    }

    /** Inits the dialog. */
    protected void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{});
    }

    /** Inits the dialog after it becomes visible. */
    protected void initDialogAfterVisible() {
        enableComponents();
        makeDefaultAndRequestFocusLater(vgNameCB);
        makeDefaultButton(createButton);
    }

    /** Enables and disabled buttons. */
    protected void checkButtons() {
        boolean enable = true;
        for (final Host h : hostCheckBoxes.keySet()) {
            if (hostCheckBoxes.get(h).isSelected() && !hostHasPVS(h)) {
                enable = false;
                break;
            }
        }
        SwingUtilities.invokeLater(new EnableCreateRunnable(enable));
    }

    private class EnableCreateRunnable implements Runnable {
        private final boolean enable ;
        public EnableCreateRunnable(final boolean enable) {
            super();
            this.enable = enable;
        }

        @Override public void run() {
            boolean e = enable;
            if (enable) {
                boolean vgNameCorrect = true;
                if ("".equals(vgNameCB.getStringValue())) {
                    vgNameCorrect = false;
                } else if (hostCheckBoxes != null) {
                    for (final Host h : hostCheckBoxes.keySet()) {
                        if (hostCheckBoxes.get(h).isSelected()) {
                            final Set<String> vgs =
                                h.getVolumeGroupNames();
                            if (vgs != null && vgs.contains(
                                            vgNameCB.getStringValue())) {
                                vgNameCorrect = false;
                                break;
                            }
                        }
                    }
                }
                if (vgNameCorrect) {
                    vgNameCB.setBackground("", "", true);
                } else {
                    e = false;
                    vgNameCB.wrongValue();
                }
            }
            createButton.setEnabled(e);
        }
    }

    /** Returns array of volume group checkboxes. */
    private Map<String, JCheckBox> getPVCheckBoxes(final String selectedPV) {
        final Map<String, JCheckBox> components =
                                    new LinkedHashMap<String, JCheckBox>();
        for (final String pvName : host.getPhysicalVolumes()) {
            final JCheckBox button =
                           new JCheckBox(pvName, pvName.equals(selectedPV));
            button.setBackground(
                   Tools.getDefaultColor("ConfigDialog.Background.Light"));
            components.put(pvName, button);
        }
        return components;
    }

    /** Returns true if the specified host has specified PVs without VGs. */
    private boolean hostHasPVS(final Host host) {
        final List<String> oPVS = host.getPhysicalVolumes();
        final Set<String> pvs = pvCheckBoxes.keySet();
        int selected = 0;
        for (final String pv : pvs) {
            if (!pvCheckBoxes.get(pv).isSelected()) {
                continue;
            }
            selected++;
            if (!oPVS.contains(pv)) {
                return false;
            }
            final BlockDevInfo oBdi =
                host.getBrowser().getDrbdGraph().findBlockDevInfo(
                                                             host.getName(),
                                                             pv);
            if (oBdi == null
                || !oBdi.getBlockDevice().isPhysicalVolume()
                || oBdi.getBlockDevice().isVolumeGroupOnPhysicalVolume()) {
                return false;
            }
        }
        return selected > 0;
    }

    /** Returns the input pane. */
    @Override protected JComponent getInputPane() {
        createButton.setEnabled(false);
        final JPanel pane = new JPanel(new SpringLayout());
        /* vg name */
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setBackground(Browser.STATUS_BACKGROUND);

        /* find next free group volume name */
        String defaultName;
        final Set<String> volumeGroups = host.getVolumeGroupNames();
        int i = 0;
        while (true) {
            defaultName = "vg" + String.format("%02d", i);
            if (volumeGroups == null
                || !volumeGroups.contains(defaultName)) {
                break;
            }
            i++;
        }
        vgNameCB = new GuiComboBox(defaultName,
                                   null,
                                   null, /* units */
                                   GuiComboBox.Type.TEXTFIELD,
                                   null, /* regexp */
                                   250,
                                   null, /* abbrv */
                                   new AccessMode(ConfigData.AccessType.OP,
                                                  false)); /* only adv. */
        inputPane.add(new JLabel("VG Name"));
        inputPane.add(vgNameCB);

        createButton.addActionListener(new CreateActionListener());
        inputPane.add(createButton);
        SpringUtilities.makeCompactGrid(inputPane, 1, 3,  /* rows, cols */
                                                   1, 1,  /* initX, initY */
                                                   1, 1); /* xPad, yPad */

        pane.add(inputPane);
        /* Volume groups. */
        final JPanel pvsPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String selectedPV = null;
        if (selectedBlockDevInfo != null) {
            selectedPV = selectedBlockDevInfo.getName();
        }
        pvCheckBoxes = getPVCheckBoxes(selectedPV);
        pvsPane.add(new JLabel("Select physical volumes: "));
        for (final String pvName : pvCheckBoxes.keySet()) {
            pvCheckBoxes.get(pvName).addItemListener(
                                            new ItemChangeListener(true));
            pvsPane.add(pvCheckBoxes.get(pvName));
        }
        final JScrollPane pvSP = new JScrollPane(pvsPane);
        pvSP.setPreferredSize(new Dimension(0, 45));
        pane.add(pvSP);

        final JPanel hostsPane = new JPanel(
                                        new FlowLayout(FlowLayout.LEFT));
        final Cluster cluster = host.getCluster();
        hostCheckBoxes = Tools.getHostCheckBoxes(cluster);
        hostsPane.add(new JLabel("Select Hosts: "));
        for (final Host h : hostCheckBoxes.keySet()) {
            hostCheckBoxes.get(h).addItemListener(
                                            new ItemChangeListener(true));
            if (host == h) {
                hostCheckBoxes.get(h).setEnabled(false);
                hostCheckBoxes.get(h).setSelected(true);
            } else if (hostHasPVS(h)) {
                hostCheckBoxes.get(h).setEnabled(true);
                hostCheckBoxes.get(h).setSelected(false);
            } else {
                hostCheckBoxes.get(h).setEnabled(false);
                hostCheckBoxes.get(h).setSelected(false);
            }
            hostsPane.add(hostCheckBoxes.get(h));
        }
        final JScrollPane sp = new JScrollPane(hostsPane);
        sp.setPreferredSize(new Dimension(0, 45));
        pane.add(sp);
        pane.add(getProgressBarPane(null));
        pane.add(getAnswerPane(""));
        SpringUtilities.makeCompactGrid(pane, 5, 1,  /* rows, cols */
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
        @Override public void itemStateChanged(final ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED
                || onDeselect) {
                checkButtons();
            }
        }
    }

    /** Size combo box action listener. */
    private class SizeDocumentListener implements DocumentListener {
        private void check() {
            checkButtons();
        }

        @Override public void insertUpdate(final DocumentEvent e) {
            check();
        }

        @Override public void removeUpdate(final DocumentEvent e) {
            check();
        }

        @Override public void changedUpdate(final DocumentEvent e) {
            check();
        }
    }

    /** Create action listener. */
    private class CreateActionListener implements ActionListener {
        @Override public void actionPerformed(final ActionEvent e) {
            final Thread thread = new Thread(new CreateRunnable());
            thread.start();
        }
    }

    private class CreateRunnable implements Runnable {
        @Override public void run() {
            Tools.invokeAndWait(new EnableCreateRunnable(false));
            disableComponents();
            getProgressBar().start(CREATE_TIMEOUT
                                   * hostCheckBoxes.size());
            boolean oneFailed = false;
            for (final Host h : hostCheckBoxes.keySet()) {
                if (hostCheckBoxes.get(h).isSelected()) {
                    final List<String> pvNames = new ArrayList<String>();
                    for (final String pv : pvCheckBoxes.keySet()) {
                        if (pvCheckBoxes.get(pv).isSelected()) {
                            pvNames.add(pv);
                        }
                    }
                    final boolean ret =
                        vgCreate(h, vgNameCB.getStringValue(), pvNames);
                    if (!ret) {
                        oneFailed = true;
                    }
                        
                }
            }
            enableComponents();
            if (oneFailed) {
                for (final Host h : hostCheckBoxes.keySet()) {
                    h.getBrowser().getClusterBrowser().updateHWInfo(h);
                }
                checkButtons();
                progressBarDoneError();
            } else {
                progressBarDone();
                disposeDialog();
                for (final Host h : hostCheckBoxes.keySet()) {
                    h.getBrowser().getClusterBrowser().updateHWInfo(h);
                }
            }
        }
    }

    /** Create VG. */
    private boolean vgCreate(final Host host,
                             final String vgName,
                             final List<String> pvNames) {
        for (final String pv : pvNames) {
            final BlockDevInfo bdi =
                host.getBrowser().getDrbdGraph().findBlockDevInfo(
                                                             host.getName(),
                                                             pv);
            if (bdi != null) {
                bdi.getBlockDevice().setVolumeGroupOnPhysicalVolume(vgName);
                bdi.getBrowser().getDrbdGraph().startAnimation(bdi);
            }
        }
        final boolean ret = LVM.vgCreate(host, vgName, pvNames, false);
        if (ret) {
            answerPaneAddText("Volume group "
                              + vgName
                              + " was successfully created "
                              + " on " + host.getName() + ".");
        } else {
            answerPaneAddTextError("Creating of volume group "
                                   + vgName
                                   + " failed.");
        }
        return ret;
    }
}
