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
 * This class implements LVM resize plugin. Note that no anonymous classes are
 * allowed here, because caching wouldn't work.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class LVM_Resize implements RemotePlugin {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Name of the LVM menu item. */
    private static final String LVM_MENU_ITEM = "LVM";
    /** Name of the resize menu item. */
    private static final String LV_RESIZE_MENU_ITEM = "LV Resize";
    /** Description. */
    private static final String DESCRIPTION =
                   "Manage logical volumes.";
    /** Description LVM resize. */
    private static final String DESCRIPTION_RESIZE =
                   "Resize the LVM volume. You can make it bigger, but not"
                   + " smaller for now. If this volume is replicated by"
                   + " DRBD, volumes on both nodes will be resized and"
                   + " drbdadm resize will be called. If you have something"
                   + " like filesystem on the DRBD, you have to resize the"
                   + " filesystem yourself.";

    /** Private. */
    public LVM_Resize() {
    }

    /** Inits the plugin. */
    @Override public void init() {
        for (final BlockDevInfo bdi : Tools.getGUIData().getAllBlockDevices()) {
            registerInfo(bdi);
        }
    }

    /** Adds the menu items to the specified info object. */
    private void registerInfo(final Info info) {
        if (info instanceof BlockDevInfo) {
            final JMenu menu = info.getMenu();
            if (menu != null) {
                info.addPluginMenuItem(getResizeLVMItem((BlockDevInfo) info));
                info.addPluginActionMenuItem(getResizeLVMItem(
                                                      (BlockDevInfo) info));
            }
        }
    }

    /** Adds menu items from the plugin. */
    @Override public void addPluginMenuItems(final Info info,
                                             final List<UpdatableItem> items) {
        if (items != null) {
            if (info instanceof BlockDevInfo) {
                items.add(getResizeLVMItem((BlockDevInfo) info));
            }
        }
    }

    /** Resize menu. */
    private MyMenuItem getResizeLVMItem(final BlockDevInfo bdi) {
        final ResizeItem resizeMenu =
            new ResizeItem(LV_RESIZE_MENU_ITEM,
                           null,
                           LV_RESIZE_MENU_ITEM,
                           new AccessMode(ConfigData.AccessType.OP, true),
                           new AccessMode(ConfigData.AccessType.OP, true),
                           bdi);
        return resizeMenu;
    }

    /** Resize menu item. (can't use anonymous classes). */
    private final class ResizeItem extends MyMenuItem {
        private static final long serialVersionUID = 1L;
        private final BlockDevInfo bdi;

        public ResizeItem(final String text,
                          final ImageIcon icon,
                          final String shortDesc,
                          final AccessMode enableAccessMode,
                          final AccessMode visibleAccessMode,
                          final BlockDevInfo bdi) {
            super(text, icon, shortDesc, enableAccessMode, visibleAccessMode);
            this.bdi = bdi;
        }
        public boolean predicate() {
            return true;
        }

        public boolean visiblePredicate() {
            return bdi.isLVM();
        }

        public String enablePredicate() {
            return null;
        }

        @Override public void action() {
            final LVResizeDialog lvmrd = new LVResizeDialog(bdi);
            while (true) {
                lvmrd.showDialog();
                if (lvmrd.isPressedCancelButton()) {
                    lvmrd.cancelDialog();
                    return;
                } else if (lvmrd.isPressedFinishButton()) {
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
            return "LVM Resize " + Tools.getRelease();
        }

        protected String getDescription() {
            return "You can now use LVM menu items in the "
                   + "\"Storage (DRBD)\" view.<br><br>" + DESCRIPTION;
        }

        protected JComponent getInputPane() {
            return null;
        }
    }

    /** LVM Resize dialog. */
    private class LVResizeDialog extends WizardDialog {
        /** Block device info object. */
        final BlockDevInfo blockDevInfo;
        private final MyButton resizeButton = new MyButton("Resize");
        private GuiComboBox sizeCB;
        private GuiComboBox oldSizeCB;
        private GuiComboBox maxSizeCB;
        /** Create new LVResizeDialog object. */
        public LVResizeDialog(final BlockDevInfo blockDevInfo) {
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
            return "LVM Resize";
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
            if (checkDRBD()) {
                sizeCB.requestFocus();
            }
            SwingUtilities.invokeLater(new SizeRequestFocusRunnable());
        }

        /** Check if it is DRBD device and if it could be resized. */
        private boolean checkDRBD() {
            if (blockDevInfo.getBlockDevice().isDrbd()) {
                final DrbdResourceInfo dri = blockDevInfo.getDrbdResourceInfo();
                final BlockDevInfo oBDI = blockDevInfo.getOtherBlockDevInfo();
                if (!dri.isConnected(false)) {
                    printErrorAndRetry(
                                    "Not resizing. DRBD resource is not connected.");
                    sizeCB.setEnabled(false);
                    resizeButton.setEnabled(false);
                    return false;
                } else if (dri.isSyncing()) {
                    printErrorAndRetry(
                                    "Not resizing. DRBD resource is syncing.");
                    sizeCB.setEnabled(false);
                    resizeButton.setEnabled(false);
                    return false;
                } else if (!oBDI.getBlockDevice().isAttached()) {
                    printErrorAndRetry(
                            "Not resizing. DRBD resource is not attached on "
                            + oBDI.getHost() + ".");
                    sizeCB.setEnabled(false);
                    resizeButton.setEnabled(false);
                    return false;
                } else if (!blockDevInfo.getBlockDevice().isAttached()) {
                    printErrorAndRetry(
                            "Not resizing. DRBD resource is not attached on "
                            + blockDevInfo.getHost() + ".");
                    sizeCB.setEnabled(false);
                    resizeButton.setEnabled(false);
                    return false;
                } else if (!oBDI.getBlockDevice().isPrimary()
                           && !blockDevInfo.getBlockDevice().isPrimary()) {
                    printErrorAndRetry(
                       "Not resizing. Must be primary at least on one node.");
                    sizeCB.setEnabled(false);
                    resizeButton.setEnabled(false);
                    return false;
                }
            }
            return true;
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
            SwingUtilities.invokeLater(new EnableResizeRunnable(true));
        }

        private class EnableResizeRunnable implements Runnable {
            private final boolean enable ;
            public EnableResizeRunnable(final boolean enable) {
                super();
                this.enable = enable;
            }

            @Override public void run() {
                boolean e = enable;
                if (enable) {
                    final long oldSize = Tools.convertToKilobytes(
                                                   oldSizeCB.getStringValue());
                    final long size = Tools.convertToKilobytes(
                                                      sizeCB.getStringValue()); 
                    final long maxSize = Tools.convertToKilobytes(
                                                   maxSizeCB.getStringValue());
                    if (oldSize >= size || size > maxSize) {
                        e = false;
                    }
                }
                resizeButton.setEnabled(e);
            }
        }

        /** Set combo boxes with new values. */
        private void setComboBoxes() {
            final String oldBlockSize =
                                blockDevInfo.getBlockDevice().getBlockSize();
            final String maxBlockSize = getMaxBlockSize();
            oldSizeCB.setValue(Tools.convertKilobytes(oldBlockSize));
            sizeCB.setValue(Tools.convertKilobytes(Long.toString(
                                        (Long.parseLong(oldBlockSize)
                                         + Long.parseLong(maxBlockSize)) / 2)));
            maxSizeCB.setValue(Tools.convertKilobytes(maxBlockSize));
        }

        /** Returns the input pane. */
        protected final JComponent getInputPane() {
            resizeButton.setEnabled(false);
            final JPanel pane = new JPanel(new SpringLayout());
            final JPanel inputPane = new JPanel(new SpringLayout());
            /* old size */
            final JLabel oldSizeLabel = new JLabel("Current Size");
            oldSizeLabel.setEnabled(false);

            final String oldBlockSize =
                                blockDevInfo.getBlockDevice().getBlockSize();
            oldSizeCB = new GuiComboBox(Tools.convertKilobytes(oldBlockSize),
                                        null,
                                        getUnits(),
                                        GuiComboBox.Type.TEXTFIELDWITHUNIT,
                                        null, /* regexp */
                                        250,
                                        null, /* abbrv */
                                        new AccessMode(ConfigData.AccessType.OP,
                                                      false)); /* only adv. */
            oldSizeCB.setEnabled(false);
            inputPane.add(oldSizeLabel);
            inputPane.add(oldSizeCB);
            inputPane.add(new JLabel());

            final String maxBlockSize = getMaxBlockSize();
            /* size */
            final String newBlockSize = Long.toString(
                                         (Long.parseLong(oldBlockSize)
                                          + Long.parseLong(maxBlockSize)) / 2);
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
            resizeButton.addActionListener(new ResizeActionListener());
            inputPane.add(resizeButton);
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

            SpringUtilities.makeCompactGrid(inputPane, 3, 3,  // rows, cols
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

        /** Resize action listener. */
        private class ResizeActionListener implements ActionListener {
            public ResizeActionListener() {
                super();
            }
            @Override public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new ResizeRunnable());
                thread.start();
            }
        }

        private class ResizeRunnable implements Runnable {
            public ResizeRunnable() {
                super();
            }

            @Override public void run() {
                if (checkDRBD()) {
                    SwingUtilities.invokeLater(new EnableResizeRunnable(false));
                    resize(sizeCB.getStringValue());
                }
            }
        }

        /** LVM Resize and DRBD Resize. */
        private void resize(final String size) {
            final boolean ret = blockDevInfo.lvResize(size, false);
            if (ret) {
                answerPaneSetText("Lodical volume was successfully resized on "
                                  + blockDevInfo.getHost() + ".");
                /* resize lvm volume on the other node. */
                final BlockDevInfo oBDI = blockDevInfo.getOtherBlockDevInfo();
                if (oBDI != null) {
                    final boolean oRet = oBDI.lvResize(size, false);
                    if (oRet) {
                        answerPaneAddText("Lodical volume was successfully"
                                          + " resized on "
                                          + oBDI.getHost() + ".");
                        final boolean dRet = blockDevInfo.resizeDrbd(false);
                        if (dRet) {
                            answerPaneAddText(
                                 "DRBD resource "
                                 + blockDevInfo.getDrbdResourceInfo().getName()
                                 + " was successfully resized.");
                        } else {
                            answerPaneAddTextError(
                                 "DRBD resource "
                                 + blockDevInfo.getDrbdResourceInfo().getName()
                                 + " resizing failed.");
                        }
                    } else {
                        answerPaneAddTextError("Resizing of "
                                               + oBDI.getName()
                                               + " on host "
                                               + oBDI.getHost()
                                               + " failed.");
                    }
                }
            } else {
                answerPaneSetTextError("Resizing of "
                                       + blockDevInfo.getName()
                                       + " on host "
                                       + blockDevInfo.getHost()
                                       + " failed.");
            }
            final Host host = blockDevInfo.getHost();
            host.getBrowser().getClusterBrowser().updateHWInfo(host);
            setComboBoxes();
        }

        /** Returns maximum block size available in the group. */
        private String getMaxBlockSize() {
            final long free = blockDevInfo.getFreeInVolumeGroup() / 1024;

            String maxBlockSize = "0";
            try {
                final long taken = Long.parseLong(
                                 blockDevInfo.getBlockDevice().getBlockSize());
                final BlockDevInfo oBDI = blockDevInfo.getOtherBlockDevInfo();
                if (oBDI == null) {
                    maxBlockSize = Long.toString(free + taken);
                } else {
                    final long oFree = oBDI.getFreeInVolumeGroup() / 1024;
                    final long oTaken = Long.parseLong(
                                         oBDI.getBlockDevice().getBlockSize());
                    if (free + taken < oFree + oTaken) {
                        /* take the smaller maximum. */
                        maxBlockSize = Long.toString(free + taken);
                    } else {
                        maxBlockSize = Long.toString(oFree + oTaken);
                    }
                }
            } catch (final Exception e) {
                /* ignore */
            }
            return maxBlockSize;
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
