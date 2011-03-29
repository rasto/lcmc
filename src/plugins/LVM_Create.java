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
import drbd.gui.resources.DrbdResourceInfo;
import drbd.gui.resources.HostDrbdInfo;

import drbd.utilities.Tools;
import drbd.utilities.RemotePlugin;
import drbd.utilities.MyButton;
import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Unit;
import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.data.Host;
import drbd.data.Cluster;
import drbd.gui.dialog.WizardDialog;
import drbd.gui.GuiComboBox;

import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JMenu;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
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
    /** Description. */
    private static final String DESCRIPTION =
                   "Manage logical volumes.";
    /** Description LV create. */
    private static final String DESCRIPTION_CREATE =
                   "Create a logical volume in an existing volume group";

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
        if (info instanceof HostDrbdInfo) {
            final JMenu menu = info.getMenu();
            if (menu != null) {
                info.addPluginMenuItem(getLVMItem((HostDrbdInfo) info));
                info.addPluginActionMenuItem(getLVMItem(
                                                      (HostDrbdInfo) info));
            }
        }
    }

    /** Adds menu items from the plugin. */
    @Override public void addPluginMenuItems(final Info info,
                                             final List<UpdatableItem> items) {
        if (items != null) {
            if (info instanceof HostDrbdInfo) {
                items.add(getLVMItem((HostDrbdInfo) info));
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
    public final void addLVMMenu(final MyMenu submenu,
                                 final HostDrbdInfo hostDrbdInfo) {
        Tools.invokeAndWait(new Runnable() {
            @Override public void run() {
                submenu.removeAll();
            }
        });
        for (final String vg : hostDrbdInfo.getHost().getVolumeGroupNames()) {
            final MyMenuItem vgItem = new LVMCreateItem(
                              LV_CREATE_MENU_ITEM + vg,
                              null,
                              LV_CREATE_MENU_ITEM + vg,
                              new AccessMode(ConfigData.AccessType.OP, false),
                              new AccessMode(ConfigData.AccessType.OP, false),
                              vg,
                              hostDrbdInfo);
            submenu.add(vgItem);
        }
    }

    /** LVM create menu item. (can't use anonymous classes). */
    private final class LVMCreateItem extends MyMenuItem {
        private static final long serialVersionUID = 1L;
        private final HostDrbdInfo hostDrbdInfo;
        final String volumeGroup;

        public LVMCreateItem(final String text,
                             final ImageIcon icon,
                             final String shortDesc,
                             final AccessMode enableAccessMode,
                             final AccessMode visibleAccessMode,
                             final String volumeGroup,
                             final HostDrbdInfo hostDrbdInfo) {
            super(text, icon, shortDesc, enableAccessMode, visibleAccessMode);
            this.hostDrbdInfo = hostDrbdInfo;
            this.volumeGroup = volumeGroup;
        }
        public boolean predicate() {
            return true;
        }

        public boolean visiblePredicate() {
            return true;
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

        protected void initDialog() {
            super.initDialog();
            enableComponents();
        }

        protected String getDialogTitle() {
            return "LVM Create " + Tools.getRelease();
        }

        protected String getDescription() {
            return "You can now use LVM menu items in the "
                   + "\"Storage (DRBD)\" view.<br><br>" + DESCRIPTION;
        }

        protected JComponent getInputPane() {
            return null;
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
            return DESCRIPTION;
        }

        public final String cancelButton() {
            return "Close";
        }

        /** Inits the dialog. */
        protected final void initDialog() {
            super.initDialog();
            enableComponentsLater(
                              new JComponent[]{});
            enableComponents();
            sizeCB.requestFocus();
            SwingUtilities.invokeLater(new SizeRequestFocusRunnable());
        }

        private class SizeRequestFocusRunnable implements Runnable {
            public SizeRequestFocusRunnable() {
                super();
            }

            @Override public void run() {
                sizeCB.requestFocus();
            }
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
                            hostDrbdInfo.getHost()
                                        .getLogicalVolumesFromVolumeGroup(
                                                                  volumeGroup);
                        if (lvs != null
                            && lvs.contains(lvNameCB.getStringValue())) {
                            e = false;
                        }
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

        /** Returns the input pane. */
        protected final JComponent getInputPane() {
            createButton.setEnabled(false);
            final JPanel pane = new JPanel(new SpringLayout());
            final JPanel inputPane = new JPanel(new SpringLayout());

            inputPane.add(new JLabel("Group"));
            inputPane.add(new JLabel(volumeGroup));
            inputPane.add(new JLabel());
            /* find next free logical volume name */
            String defaultName;
            final Set<String> volumeGroups =
                   hostDrbdInfo.getHost().getLogicalVolumesFromVolumeGroup(
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
            sizeCB.addListeners(new SizeItemListener(), 
                                new SizeDocumentListener());

            SpringUtilities.makeCompactGrid(inputPane, 4, 3,  // rows, cols
                                                       1, 1,  // initX, initY
                                                       1, 1); // xPad, yPad

            pane.add(inputPane);
            pane.add(getProgressBarPane(null));
            pane.add(getAnswerPane(""));
            SpringUtilities.makeCompactGrid(pane, 3, 1,  // rows, cols
                                                  0, 0,  // initX, initY
                                                  0, 0); // xPad, yPad
            checkButtons();
            return pane;
        }

        /** Size combo box item listener. */
        private class SizeItemListener implements ItemListener {
            public SizeItemListener() {
                super();
            }
            @Override public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
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
                SwingUtilities.invokeLater(new EnableCreateRunnable(false));
                lvCreate(lvNameCB.getStringValue(),
                         sizeCB.getStringValue());
            }
        }

        /** LV Create. */
        private void lvCreate(final String lvName, final String size) {
            final boolean ret = hostDrbdInfo.lvCreate(lvName,
                                                      volumeGroup,
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
            final Host host = hostDrbdInfo.getHost();
            host.getBrowser().getClusterBrowser().updateHWInfo(host);
            setComboBoxes();
        }

        /** Returns maximum block size available in the group. */
        private String getMaxBlockSize() {
            final long free =
               hostDrbdInfo.getHost().getFreeInVolumeGroup(volumeGroup) / 1024;
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
