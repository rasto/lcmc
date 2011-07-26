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

package plugins;

import drbd.gui.SpringUtilities;
import drbd.gui.dialog.ConfigDialog;
import drbd.gui.resources.Info;
import drbd.gui.resources.HostDrbdInfo;
import drbd.gui.resources.BlockDevInfo;

import drbd.utilities.Tools;
import drbd.utilities.RemotePlugin;
import drbd.utilities.MyButton;
import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Unit;
import drbd.utilities.LVM;
import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.data.Host;
import drbd.data.Cluster;
import drbd.gui.dialog.WizardDialog;
import drbd.gui.GuiComboBox;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JMenu;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.FlowLayout;
import java.awt.Dimension;
/**
 * This class implements LVM create plugin. Note that no anonymous classes are
 * allowed here, because caching wouldn't work.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class LVM_Create implements RemotePlugin {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Name of the LVM menu item. */
    private static final String LVM_MENU_ITEM = "LVM";
    /** Name of the create menu item. */
    private static final String LV_CREATE_MENU_ITEM = "Create LV in VG ";
    /** Name of the vg create menu item. */
    private static final String VG_CREATE_MENU_ITEM = "Create VG";
    /** Description VG create. */
    private static final String VG_CREATE_DESCRIPTION =
                                                    "Create a volume group.";
    /** Description LV create. */
    private static final String LV_CREATE_DESCRIPTION =
                       "Create a logical volume in an existing volume group.";
    /** Create LV timeout. */
    private static final int CREATE_TIMEOUT = 5000;

    /** Private. */
    public LVM_Create() {
    }

    /** Inits the plugin. */
    @Override public void init() {
        for (final Cluster cluster
                        : Tools.getConfigData().getClusters().getClusterSet()) {
            final Host[] hosts = cluster.getHostsArray();
            for (final Host host : hosts) {
                registerInfo(host.getBrowser().getHostDrbdInfo());
            }
        }
    }

    /** Adds the menu items to the specified info object. */
    private void registerInfo(final Info info) {
        final JMenu menu = info.getMenu();
        if (menu != null) {
            if (info instanceof HostDrbdInfo) {
                info.addPluginMenuItem(getLVMItem((HostDrbdInfo) info));
                info.addPluginActionMenuItem(getLVMItem((HostDrbdInfo) info));
            } else if (info instanceof BlockDevInfo) {
                final BlockDevInfo bdi = (BlockDevInfo) info;
                final HostDrbdInfo hdi =
                                bdi.getHost().getBrowser().getHostDrbdInfo();
                String vg = null;
                if (bdi.isLVM()) {
                    vg = bdi.getBlockDevice().getVolumeGroup();
                } else {
                    vg = bdi.getBlockDevice().getVolumeGroupOnPhysicalVolume();
                }
                info.addPluginMenuItem(getLVMCreateItem(hdi, vg));
                info.addPluginActionMenuItem(getLVMCreateItem(hdi, vg));
                info.addPluginMenuItem(getVGCreateItem(hdi, bdi));
                info.addPluginActionMenuItem(getVGCreateItem(hdi, bdi));
            }
        }
    }

    /** Adds menu items from the plugin. */
    @Override public void addPluginMenuItems(final Info info,
                                             final List<UpdatableItem> items) {
        if (items != null) {
            if (info instanceof HostDrbdInfo) {
                items.add(getLVMItem((HostDrbdInfo) info));
            } else if (info instanceof BlockDevInfo) {
                final BlockDevInfo bdi = (BlockDevInfo) info;
                final HostDrbdInfo hdi =
                                  bdi.getHost().getBrowser().getHostDrbdInfo();
                items.add(getVGCreateItem(hdi, bdi));
                String vg = null;
                if (bdi.isLVM()) {
                    vg = bdi.getBlockDevice().getVolumeGroup();
                } else {
                    vg = bdi.getBlockDevice().getVolumeGroupOnPhysicalVolume();
                }
                items.add(getLVMCreateItem(hdi, vg));
            }
        }
    }

    /** LVM menu. */
    private MyMenu getLVMItem(final HostDrbdInfo hostDrbdInfo) {
        final LVMMenu lvmMenu =
            new LVMMenu(LVM_MENU_ITEM,
                        new AccessMode(ConfigData.AccessType.OP, true),
                        new AccessMode(ConfigData.AccessType.OP, true),
                        hostDrbdInfo);
        return lvmMenu;
    }

    /** LVM submenu. (can't use anonymous classes). */
    private final class LVMMenu extends MyMenu {
        private static final long serialVersionUID = 1L;
        private final HostDrbdInfo hostDrbdInfo;
        public LVMMenu(final String text,
                       final AccessMode enableAccessMode,
                       final AccessMode visibleAccessMode,
                       final HostDrbdInfo hostDrbdInfo) {
            super(text, enableAccessMode, visibleAccessMode);
            this.hostDrbdInfo = hostDrbdInfo;
        }

        @Override public String enablePredicate() {
            return null;
        }

        @Override public void update() {
            super.update();
            addLVMMenu(this, hostDrbdInfo);
        }
    }

    /** Adds menus to manage LVMs. */
    public void addLVMMenu(final MyMenu submenu,
                           final HostDrbdInfo hostDrbdInfo) {
        Tools.invokeAndWait(new Runnable() {
            @Override public void run() {
                submenu.removeAll();
            }
        });
        submenu.add(getVGCreateItem(hostDrbdInfo, null));
        for (final String vg : hostDrbdInfo.getHost().getVolumeGroupNames()) {
            submenu.add(getLVMCreateItem(hostDrbdInfo, vg));
        }
    }

    /** Return create LV menu item. */
    private MyMenuItem getLVMCreateItem(final HostDrbdInfo hostDrbdInfo,
                                        final String vg) {
        return new LVMCreateItem(
                              LV_CREATE_MENU_ITEM + vg,
                              null,
                              LV_CREATE_MENU_ITEM + vg,
                              new AccessMode(ConfigData.AccessType.OP, false),
                              new AccessMode(ConfigData.AccessType.OP, false),
                              hostDrbdInfo,
                              vg);
    }

    /** Return VG create menu item. BlockDevInfo can be null, if nothing
     *  is preselected. */
    private MyMenuItem getVGCreateItem(final HostDrbdInfo hostDrbdInfo,
                                       final BlockDevInfo blockDevInfo) {
        return new VGCreateItem(
                              VG_CREATE_MENU_ITEM,
                              null,
                              VG_CREATE_MENU_ITEM,
                              new AccessMode(ConfigData.AccessType.OP, false),
                              new AccessMode(ConfigData.AccessType.OP, false),
                              hostDrbdInfo,
                              blockDevInfo);
    }

    /** VG create menu item. (can't use anonymous classes). */
    private final class VGCreateItem extends MyMenuItem {
        private static final long serialVersionUID = 1L;
        private final HostDrbdInfo hostDrbdInfo;
        private final BlockDevInfo selectedBlockDevInfo;

        public VGCreateItem(final String text,
                            final ImageIcon icon,
                            final String shortDesc,
                            final AccessMode enableAccessMode,
                            final AccessMode visibleAccessMode,
                            final HostDrbdInfo hostDrbdInfo,
                            final BlockDevInfo selectedBlockDevInfo) {
            super(text, icon, shortDesc, enableAccessMode, visibleAccessMode);
            this.hostDrbdInfo = hostDrbdInfo;
            this.selectedBlockDevInfo = selectedBlockDevInfo;
        }

        public boolean visiblePredicate() {
            return selectedBlockDevInfo == null
                   || (!selectedBlockDevInfo.getBlockDevice()
                                .isVolumeGroupOnPhysicalVolume()
                       && !selectedBlockDevInfo.isLVM());
                                                                
        }

        public String enablePredicate() {
            return null;
        }

        @Override public void action() {
            final VGCreateDialog vgCreate = new VGCreateDialog(
                                                        hostDrbdInfo,
                                                        selectedBlockDevInfo);
            while (true) {
                vgCreate.showDialog();
                if (vgCreate.isPressedCancelButton()) {
                    vgCreate.cancelDialog();
                    return;
                } else if (vgCreate.isPressedFinishButton()) {
                    break;
                }
            }
        }
    }

    /** LVM create menu item. */
    private final class LVMCreateItem extends MyMenuItem {
        private static final long serialVersionUID = 1L;
        private final HostDrbdInfo hostDrbdInfo;
        final String volumeGroup;

        public LVMCreateItem(final String text,
                             final ImageIcon icon,
                             final String shortDesc,
                             final AccessMode enableAccessMode,
                             final AccessMode visibleAccessMode,
                             final HostDrbdInfo hostDrbdInfo,
                             final String volumeGroup) {
            super(text, icon, shortDesc, enableAccessMode, visibleAccessMode);
            this.hostDrbdInfo = hostDrbdInfo;
            this.volumeGroup = volumeGroup;
        }

        public boolean visiblePredicate() {
            return volumeGroup != null && !"".equals(volumeGroup);
        }

        public String enablePredicate() {
            return null;
        }

        @Override public void action() {
            final LVCreateDialog lvCreate = new LVCreateDialog(hostDrbdInfo,
                                                               volumeGroup);
            while (true) {
                lvCreate.showDialog();
                if (lvCreate.isPressedCancelButton()) {
                    lvCreate.cancelDialog();
                    return;
                } else if (lvCreate.isPressedFinishButton()) {
                    break;
                }
            }
        }
    }

    /** Shows dialog with description. */
    @Override public void showDescription() {
        final Description description = new Description();
        description.showDialog();
    }

    /** Description dialog. */
    private final class Description extends ConfigDialog {
        /** Serial version UID. */
        private static final long serialVersionUID = 1L;

        public Description() {
            super();
        }

        protected void initDialogAfterVisible() {
            enableComponents();
        }

        protected String getDialogTitle() {
            return "LVM Create " + Tools.getRelease();
        }

        protected String getDescription() {
            return "You can now use LVM menu items in the "
                   + "\"Storage (DRBD)\" view.<br><br>";
        }

        protected JComponent getInputPane() {
            return null;
        }
    }

    /** VG create dialog. */
    private class VGCreateDialog extends WizardDialog {
        final HostDrbdInfo hostDrbdInfo;
        final BlockDevInfo selectedBlockDevInfo;
        private final MyButton createButton = new MyButton("Create VG");
        private GuiComboBox vgNameCB;
        private Map<Host, JCheckBox> hostCheckBoxes = null;
        private Map<String, JCheckBox> pvCheckBoxes = null;
        /** Create new VGCreateDialog object. */
        public VGCreateDialog(final HostDrbdInfo hostDrbdInfo,
                              final BlockDevInfo selectedBlockDevInfo) {
            super(null);
            this.hostDrbdInfo = hostDrbdInfo;
            this.selectedBlockDevInfo = selectedBlockDevInfo;
        }

        /** Finishes the dialog and sets the information. */
        protected final void finishDialog() {
        }

        /** Returns the next dialog. */
        public WizardDialog nextDialog() {
            return null;
        }

        /** Returns the title of the dialog. */
        protected final String getDialogTitle() {
            return "VG Create ";
        }

        /** Returns the description of the dialog. */
        protected final String getDescription() {
            return VG_CREATE_DESCRIPTION;
        }

        /** Inits the dialog. */
        protected final void initDialog() {
            super.initDialog();
            enableComponentsLater(new JComponent[]{});
        }

        /** Inits the dialog after it becomes visible. */
        protected void initDialogAfterVisible() {
            enableComponents();
            makeDefaultAndRequestFocusLater(vgNameCB);
        }

        /** Enables and disabled buttons. */
        protected final void checkButtons() {
            SwingUtilities.invokeLater(new EnableCreateRunnable(true));
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

        private void setComboBoxes() {
        }

        /** Returns array of volume group checkboxes. */
        private Map<String, JCheckBox> getPVCheckBoxes(
                                                    final String selectedPV) {
            final Map<String, JCheckBox> components =
                                        new LinkedHashMap<String, JCheckBox>();
            for (final String pvName
                             : hostDrbdInfo.getHost().getPhysicalVolumes()) {
                final JCheckBox button =
                               new JCheckBox(pvName, pvName.equals(selectedPV));
                button.setBackground(
                       Tools.getDefaultColor("ConfigDialog.Background.Light"));
                components.put(pvName, button);
            }
            return components;
        }

        /** Returns the input pane. */
        protected final JComponent getInputPane() {
            createButton.setEnabled(false);
            final JPanel pane = new JPanel(new SpringLayout());
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

            /* vg name */
            final JPanel inputPane = new JPanel(new SpringLayout());

            /* find next free group volume name */
            String defaultName;
            final Set<String> volumeGroups =
                                 hostDrbdInfo.getHost().getVolumeGroupNames();
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
            SpringUtilities.makeCompactGrid(inputPane, 1, 3,  // rows, cols
                                                       1, 1,  // initX, initY
                                                       1, 1); // xPad, yPad

            pane.add(inputPane);
            final JPanel hostsPane = new JPanel(
                                            new FlowLayout(FlowLayout.LEFT));
            final Cluster cluster = hostDrbdInfo.getHost().getCluster();
            hostCheckBoxes = Tools.getHostCheckBoxes(cluster);
            hostsPane.add(new JLabel("Select Hosts: "));
            for (final Host h : hostCheckBoxes.keySet()) {
                hostCheckBoxes.get(h).addItemListener(
                                                new ItemChangeListener(true));
                if (hostDrbdInfo.getHost() == h) {
                    hostCheckBoxes.get(h).setEnabled(false);
                    hostCheckBoxes.get(h).setSelected(true);
                } else if (!h.getVolumeGroupNames().contains(defaultName)) {
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
            SpringUtilities.makeCompactGrid(pane, 5, 1,  // rows, cols
                                                  0, 0,  // initX, initY
                                                  0, 0); // xPad, yPad
            checkButtons();
            return pane;
        }

        /** Size combo box item listener. */
        private class ItemChangeListener implements ItemListener {
            private final boolean onDeselect;
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
            public SizeDocumentListener() {
                super();
            }
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
            public CreateActionListener() {
                super();
            }
            @Override public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new CreateRunnable());
                thread.start();
            }
        }

        private class CreateRunnable implements Runnable {
            public CreateRunnable() {
                super();
            }

            @Override public void run() {
                Tools.invokeAndWait(new EnableCreateRunnable(false));
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
                if (oneFailed) {
                    for (final Host h : hostCheckBoxes.keySet()) {
                        if (hostCheckBoxes.get(h).isSelected()) {
                            h.getBrowser().getClusterBrowser().updateHWInfo(h);
                        }
                    }
                    setComboBoxes();
                    checkButtons();
                    progressBarDoneError();
                } else {
                    progressBarDone();
                    disposeDialog();
                    for (final Host h : hostCheckBoxes.keySet()) {
                        if (hostCheckBoxes.get(h).isSelected()) {
                            h.getBrowser().getClusterBrowser().updateHWInfo(h);
                        }
                    }
                }
            }
        }

        /** VG Create. */
        private boolean vgCreate(final Host host,
                                 final String vgName,
                                 final List<String> pvNames) {
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

    /** LV create dialog. */
    private class LVCreateDialog extends WizardDialog {
        /** Block device info object. */
        final HostDrbdInfo hostDrbdInfo;
        private final MyButton createButton = new MyButton("Create");
        private GuiComboBox lvNameCB;
        private GuiComboBox sizeCB;
        private GuiComboBox maxSizeCB;
        private final String volumeGroup;
        private Map<Host, JCheckBox> hostCheckBoxes = null;
        /** Create new LVCreateDialog object. */
        public LVCreateDialog(final HostDrbdInfo hostDrbdInfo,
                              final String volumeGroup) {
            super(null);
            this.hostDrbdInfo = hostDrbdInfo;
            this.volumeGroup = volumeGroup;
        }

        /** Finishes the dialog and sets the information. */
        protected final void finishDialog() {
        }

        /** Returns the next dialog. */
        public WizardDialog nextDialog() {
            return null;
        }

        /** Returns the title of the dialog. */
        protected final String getDialogTitle() {
            return "LV Create ";
        }

        /** Returns the description of the dialog. */
        protected final String getDescription() {
            return LV_CREATE_DESCRIPTION;
        }

        /** Close button. */
        public final String cancelButton() {
            return "Close";
        }

        /** Inits the dialog. */
        protected final void initDialog() {
            super.initDialog();
            enableComponentsLater(new JComponent[]{});
        }

        /** Inits the dialog after it becomes visible. */
        protected void initDialogAfterVisible() {
            enableComponents();
            makeDefaultAndRequestFocusLater(sizeCB);
        }

        /** Enables and disabled buttons. */
        protected final void checkButtons() {
            SwingUtilities.invokeLater(new EnableCreateRunnable(true));
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

        private void setComboBoxes() {
            final String maxBlockSize = getMaxBlockSize();
            sizeCB.setValue(Tools.convertKilobytes(Long.toString(
                                         + Long.parseLong(maxBlockSize) / 2)));
            maxSizeCB.setValue(Tools.convertKilobytes(maxBlockSize));
        }

        ///** Returns array of volume group checkboxes. */
        //private Map<String, JCheckBox> getVGCheckBoxes() {
        //    final Map<String, JCheckBox> components =
        //                                  new LinkedHashMap<String, JCheckBox>();
        //    for (final String vgName
        //                     : hostDrbdInfo.getHost().getVolumeGroupNames()) {
        //        final JCheckBox button = new JCheckBox(vgName);
        //        button.setBackground(
        //               Tools.getDefaultColor("ConfigDialog.Background.Light"));
        //        components.put(vgName, button);
        //    }
        //    return components;
        //}

        /** Returns the input pane. */
        protected final JComponent getInputPane() {
            createButton.setEnabled(false);
            final JPanel pane = new JPanel(new SpringLayout());
            ///* Volume groups. */
            //final JPanel vgsPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
            //vgCheckBoxes = getVGCheckBoxes();
            //vgsPane.add(new JLabel("Select volume groups: "));
            //for (final String vgName : vgCheckBoxes.keySet()) {
            //    vgCheckBoxes.get(vgName).addItemListener(
            //                                    new ItemChangeListener(true));
            //    vgsPane.add(vgCheckBoxes.get(vgName));
            //}
            //final JScrollPane vgSP = new JScrollPane(vgsPane);
            //vgSP.setPreferredSize(new Dimension(0, 45));
            //pane.add(vgSP);
            /* name, size etc. */
            final JPanel inputPane = new JPanel(new SpringLayout());

            inputPane.add(new JLabel("Group"));
            inputPane.add(new JLabel(volumeGroup));
            inputPane.add(new JLabel());
            /* find next free logical volume name */
            String defaultName;
            final Set<String> logicalVolumes =
                   hostDrbdInfo.getHost().getLogicalVolumesFromVolumeGroup(
                                                                  volumeGroup);
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
            createButton.addActionListener(new CreateActionListener());
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

            SpringUtilities.makeCompactGrid(inputPane, 4, 3,  // rows, cols
                                                       1, 1,  // initX, initY
                                                       1, 1); // xPad, yPad

            pane.add(inputPane);
            final JPanel hostsPane = new JPanel(
                            new FlowLayout(FlowLayout.LEFT));
            final Cluster cluster = hostDrbdInfo.getHost().getCluster();
            hostCheckBoxes = Tools.getHostCheckBoxes(cluster);
            hostsPane.add(new JLabel("Select Hosts: "));
            for (final Host h : hostCheckBoxes.keySet()) {
                hostCheckBoxes.get(h).addItemListener(
                                                new ItemChangeListener(true));
                if (hostDrbdInfo.getHost() == h) {
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
            final JScrollPane sp = new JScrollPane(
                                                                   hostsPane);
            sp.setPreferredSize(new Dimension(0, 45));
            pane.add(sp);
            pane.add(getProgressBarPane(null));
            pane.add(getAnswerPane(""));
            SpringUtilities.makeCompactGrid(pane, 4, 1,  // rows, cols
                                                  0, 0,  // initX, initY
                                                  0, 0); // xPad, yPad
            checkButtons();
            return pane;
        }

        /** Size combo box item listener. */
        private class ItemChangeListener implements ItemListener {
            private final boolean onDeselect;
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
            public SizeDocumentListener() {
                super();
            }
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
            public CreateActionListener() {
                super();
            }
            @Override public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new CreateRunnable());
                thread.start();
            }
        }

        private class CreateRunnable implements Runnable {
            public CreateRunnable() {
                super();
            }

            @Override public void run() {
                Tools.invokeAndWait(new EnableCreateRunnable(false));
                getProgressBar().start(CREATE_TIMEOUT
                                       * hostCheckBoxes.size());
                boolean oneFailed = false;
                for (final Host h : hostCheckBoxes.keySet()) {
                    if (hostCheckBoxes.get(h).isSelected()) {
                        final boolean ret = lvCreate(h,
                                                     lvNameCB.getStringValue(),
                                                     sizeCB.getStringValue());
                        if (!ret) {
                            oneFailed = true;
                        }
                    }
                }
                for (final Host h : hostCheckBoxes.keySet()) {
                    if (hostCheckBoxes.get(h).isSelected()) {
                        h.getBrowser().getClusterBrowser().updateHWInfo(h);
                    }
                }
                final String maxBlockSize = getMaxBlockSize();
                final long maxSize = Long.parseLong(maxBlockSize);
                maxSizeCB.setValue(Tools.convertKilobytes(maxBlockSize));
                if (oneFailed) {
                    progressBarDoneError();
                } else {
                    progressBarDone();
                }
            }
        }

        /** LV Create. */
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
            long free =
               hostDrbdInfo.getHost().getFreeInVolumeGroup(volumeGroup) / 1024;
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

    /** Return unit objects. */
    private Unit[] getUnits() {
        return new Unit[]{
                   //new Unit("", "", "KiByte", "KiBytes"), /* default unit */
                   new Unit("K", "K", "KiByte", "KiBytes"),
                   new Unit("M", "M", "MiByte", "MiBytes"),
                   new Unit("G",  "G",  "GiByte",      "GiBytes"),
                   new Unit("T",  "T",  "TiByte",      "TiBytes")
       };
    }
}
