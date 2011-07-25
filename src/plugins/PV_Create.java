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
import drbd.gui.resources.BlockDevInfo;

import drbd.utilities.Tools;
import drbd.utilities.RemotePlugin;
import drbd.utilities.MyButton;
import drbd.utilities.MyMenuItem;
import drbd.utilities.UpdatableItem;
import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.data.Host;
import drbd.data.Cluster;
import drbd.gui.dialog.WizardDialog;

import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JMenu;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * This class implements PV_Create plugin. Note that no anonymous classes are
 * allowed here, because caching wouldn't work.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class PV_Create implements RemotePlugin {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Name of the pv create menu item. */
    private static final String PV_CREATE_MENU_ITEM = "PV Create";
    /** Description. */
    private static final String DESCRIPTION =
                "Initialize a disk or partition for use by LVM.";
    /** Private. */
    public PV_Create() {
    }

    /** Inits the plugin. */
    @Override public void init() {
        for (final BlockDevInfo bdi
                        : Tools.getGUIData().getAllBlockDevices()) {
            registerInfo(bdi);
        }
    }

    /** Adds the menu items to the specified info object. */
    private void registerInfo(final Info info) {
        if (info instanceof BlockDevInfo) {
            final JMenu menu = info.getMenu();
            if (menu != null) {
                info.addPluginMenuItem(getCreatePVItem((BlockDevInfo) info));
                info.addPluginActionMenuItem(getCreatePVItem(
                                                      (BlockDevInfo) info));
            }
        }
    }

    /** Adds menu items from the plugin. */
    @Override public void addPluginMenuItems(final Info info,
                                             final List<UpdatableItem> items) {
        if (items != null) {
            if (info instanceof BlockDevInfo) {
                items.add(getCreatePVItem((BlockDevInfo) info));
            }
        }
    }

    /** PV create menu. */
    private MyMenuItem getCreatePVItem(final BlockDevInfo bdi) {
        final CreatePVItem pvCreateMenu =
            new CreatePVItem(PV_CREATE_MENU_ITEM,
                           null,
                           PV_CREATE_MENU_ITEM,
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, true),
                           bdi);
        return pvCreateMenu;
    }

    /** PV create menu item. (can't use anonymous classes). */
    private final class CreatePVItem extends MyMenuItem {
        private static final long serialVersionUID = 1L;
        private final BlockDevInfo blockDevInfo;

        public CreatePVItem(final String text,
                            final ImageIcon icon,
                            final String shortDesc,
                            final AccessMode enableAccessMode,
                            final AccessMode visibleAccessMode,
                            final BlockDevInfo blockDevInfo) {
            super(text, icon, shortDesc, enableAccessMode, visibleAccessMode);
            this.blockDevInfo = blockDevInfo;
        }
        public boolean predicate() {
            return true;
        }

        public boolean visiblePredicate() {
            return !blockDevInfo.isLVM();
        }

        public String enablePredicate() {
            if (blockDevInfo.getBlockDevice().isPhysicalVolume()) {
                return "already PV";
            } else if (blockDevInfo.getBlockDevice().isDrbd()) {
                return "DRBD is on it";
            }
            return null;
        }

        @Override public void action() {
            final PVCreateDialog pvCreate = new PVCreateDialog(blockDevInfo);
            while (true) {
                pvCreate.showDialog();
                if (pvCreate.isPressedCancelButton()) {
                    pvCreate.cancelDialog();
                    return;
                } else if (pvCreate.isPressedFinishButton()) {
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
            return "PV Create " + Tools.getRelease();
        }

        protected String getDescription() {
            return "You can now use LVM menu items in the "
                   + "\"Storage (DRBD)\" view.<br><br>" + DESCRIPTION;
        }

        protected JComponent getInputPane() {
            return null;
        }
    }

    /** PV create dialog. */
    private class PVCreateDialog extends WizardDialog {
        /** Block device info object. */
        private final MyButton createButton = new MyButton("PV Create");
        private final BlockDevInfo blockDevInfo;
        private Map<Host, JCheckBox> hostCheckboxes = null;
        /** Create new PVCreateDialog object. */
        public PVCreateDialog(final BlockDevInfo blockDevInfo) {
            super(null);
            this.blockDevInfo = blockDevInfo;
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
            return "PV Create ";
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
        }

        /** Enables and disabled buttons. */
        protected final void checkButtons() {
            if (!blockDevInfo.getBlockDevice().isPhysicalVolume()) {
                SwingUtilities.invokeLater(new EnableCreateRunnable(true));
            }
        }

        private class EnableCreateRunnable implements Runnable {
            private final boolean enable ;
            public EnableCreateRunnable(final boolean enable) {
                super();
                this.enable = enable;
            }

            @Override public void run() {
                final boolean e = enable;
                createButton.setEnabled(e);
            }
        }


        /** Returns the input pane. */
        protected final JComponent getInputPane() {
            createButton.setEnabled(false);
            final JPanel pane = new JPanel(new SpringLayout());
            final JPanel inputPane = new JPanel(new SpringLayout());

            inputPane.add(new JLabel("Block Device:"));
            inputPane.add(new JLabel(blockDevInfo.getName()));
            createButton.addActionListener(new CreateActionListener());
            inputPane.add(createButton);
            SpringUtilities.makeCompactGrid(inputPane, 1, 3,  // rows, cols
                                                       1, 1,  // initX, initY
                                                       1, 1); // xPad, yPad

            pane.add(inputPane);
            final JPanel hostsPane = new JPanel(
                            new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
            final Cluster cluster = blockDevInfo.getHost().getCluster();
            hostCheckboxes = Tools.getHostCheckboxes(cluster);
            hostsPane.add(new JLabel("Select Hosts: "));
            for (final Host h : hostCheckboxes.keySet()) {
                hostCheckboxes.get(h).addItemListener(
                                                new ItemChangeListener(true));
                final BlockDevInfo oBdi =
                    blockDevInfo.getBrowser().getDrbdGraph().findBlockDevInfo(
                                                      h.getName(),
                                                      blockDevInfo.getName());
                if (blockDevInfo.getHost() == h) {
                    hostCheckboxes.get(h).setEnabled(false);
                    hostCheckboxes.get(h).setSelected(true);
                } else if (oBdi == null
                           || oBdi.getBlockDevice().isPhysicalVolume()) {
                    hostCheckboxes.get(h).setEnabled(false);
                    hostCheckboxes.get(h).setSelected(false);
                } else {
                    hostCheckboxes.get(h).setEnabled(true);
                    hostCheckboxes.get(h).setSelected(false);
                }
                hostsPane.add(hostCheckboxes.get(h));
            }
            final javax.swing.JScrollPane sp = new javax.swing.JScrollPane(
                                                                   hostsPane);
            sp.setPreferredSize(new java.awt.Dimension(0, 45));
            pane.add(sp);
            pane.add(getProgressBarPane(null));
            pane.add(getAnswerPane(""));
            SpringUtilities.makeCompactGrid(pane, 4, 1,  // rows, cols
                                                  0, 0,  // initX, initY
                                                  0, 0); // xPad, yPad
            checkButtons();
            return pane;
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
                for (final Host h : hostCheckboxes.keySet()) {
                    if (hostCheckboxes.get(h).isSelected()) {
                        final BlockDevInfo oBdi =
                            blockDevInfo.getBrowser().getDrbdGraph()
                                .findBlockDevInfo(h.getName(),
                                                  blockDevInfo.getName());
                        if (oBdi != null) {
                            pvCreate(h, oBdi);
                        }
                    }
                }
                checkButtons();
            }
        }

        /** PV Create. */
        private void pvCreate(final Host host,
                              final BlockDevInfo bdi) {
            final boolean ret = bdi.pvCreate(false);
            if (ret) {
                answerPaneAddText("Physical volume "
                                  + bdi.getName()
                                  + " was successfully created "
                                  + " on " + host.getName() + ".");
            } else {
                answerPaneAddTextError("Creating of physical volume "
                                       + bdi.getName()
                                       + " on " + host.getName()
                                       + " failed.");
            }
            for (final Host h : hostCheckboxes.keySet()) {
                if (hostCheckboxes.get(h).isSelected()) {
                    h.getBrowser().getClusterBrowser().updateHWInfo(h);
                }
            }
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
    }
}
