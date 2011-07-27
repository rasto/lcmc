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
import drbd.gui.Browser;
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
 * This class implements PV_Remove plugin. Note that no anonymous classes are
 * allowed here, because caching wouldn't work.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class PV_Remove implements RemotePlugin {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Name of the remove PV menu item. */
    private static final String PV_REMOVE_MENU_ITEM = "Remove PV";
    /** Description. */
    private static final String DESCRIPTION = "Remove a physical volume.";
    /** Remove PV timeout. */
    private static final int REMOVE_TIMEOUT = 5000;
    /** Private. */
    public PV_Remove() {
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
                info.addPluginMenuItem(getRemovePVItem((BlockDevInfo) info));
                info.addPluginActionMenuItem(getRemovePVItem(
                                                      (BlockDevInfo) info));
            }
        }
    }

    /** Adds menu items from the plugin. */
    @Override public void addPluginMenuItems(final Info info,
                                             final List<UpdatableItem> items) {
        if (items != null) {
            if (info instanceof BlockDevInfo) {
                items.add(getRemovePVItem((BlockDevInfo) info));
            }
        }
    }

    /** Remove PV menu. */
    private MyMenuItem getRemovePVItem(final BlockDevInfo bdi) {
        final RemovePVItem pvRemoveMenu =
            new RemovePVItem(PV_REMOVE_MENU_ITEM,
                             null,
                             DESCRIPTION,
                             new AccessMode(ConfigData.AccessType.OP, true),
                             new AccessMode(ConfigData.AccessType.OP, true),
                             bdi);
        pvRemoveMenu.setToolTipText(DESCRIPTION);
        return pvRemoveMenu;
    }

    /** Remove PV menu item. (can't use anonymous classes). */
    private final class RemovePVItem extends MyMenuItem {
        private static final long serialVersionUID = 1L;
        private final BlockDevInfo blockDevInfo;

        public RemovePVItem(final String text,
                            final ImageIcon icon,
                            final String shortDesc,
                            final AccessMode enableAccessMode,
                            final AccessMode visibleAccessMode,
                            final BlockDevInfo blockDevInfo) {
            super(text, icon, shortDesc, enableAccessMode, visibleAccessMode);
            this.blockDevInfo = blockDevInfo;
        }

        public boolean visiblePredicate() {
            return blockDevInfo.getBlockDevice().isPhysicalVolume()
                   && !blockDevInfo.getBlockDevice()
                                    .isVolumeGroupOnPhysicalVolume();
        }

        public String enablePredicate() {
            if (blockDevInfo.getBlockDevice().isDrbd()) {
                return "DRBD is on it";
            }
            return null;
        }

        @Override public void action() {
            final PVRemoveDialog pvRemove = new PVRemoveDialog(blockDevInfo);
            while (true) {
                pvRemove.showDialog();
                if (pvRemove.isPressedCancelButton()) {
                    pvRemove.cancelDialog();
                    return;
                } else if (pvRemove.isPressedFinishButton()) {
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

        /** Inits the dialog after it becomes visible. */
        protected void initDialogAfterVisible() {
            enableComponents();
        }

        protected String getDialogTitle() {
            return "Remove PV";
        }

        protected String getDescription() {
            return "You can now use LVM menu items in the "
                   + "\"Storage (DRBD)\" view.<br><br>" + DESCRIPTION;
        }

        protected JComponent getInputPane() {
            return null;
        }
    }

    /** Remove PV dialog. */
    private class PVRemoveDialog extends WizardDialog {
        /** Block device info object. */
        private final MyButton removeButton = new MyButton("Remove PV");
        private final BlockDevInfo blockDevInfo;
        private Map<Host, JCheckBox> hostCheckBoxes = null;
        /** Remove new PVRemoveDialog object. */
        public PVRemoveDialog(final BlockDevInfo blockDevInfo) {
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
            return "Remove PV";
        }

        /** Returns the description of the dialog. */
        protected final String getDescription() {
            return DESCRIPTION;
        }

        /** Inits the dialog. */
        protected final void initDialog() {
            super.initDialog();
            enableComponentsLater(new JComponent[]{});
        }

        /** Inits the dialog after it becomes visible. */
        protected void initDialogAfterVisible() {
            enableComponents();
            makeDefaultAndRequestFocusLater(removeButton);
        }

        /** Enables and disabled buttons. */
        protected final void checkButtons() {
            if (blockDevInfo.getBlockDevice().isPhysicalVolume()) {
                SwingUtilities.invokeLater(new EnableRemoveRunnable(true));
            }
        }

        private class EnableRemoveRunnable implements Runnable {
            private final boolean enable ;
            public EnableRemoveRunnable(final boolean enable) {
                super();
                this.enable = enable;
            }

            @Override public void run() {
                final boolean e = enable;
                removeButton.setEnabled(e);
            }
        }


        /** Returns the input pane. */
        protected final JComponent getInputPane() {
            removeButton.setEnabled(false);
            final JPanel pane = new JPanel(new SpringLayout());
            final JPanel inputPane = new JPanel(new SpringLayout());
            inputPane.setBackground(Browser.STATUS_BACKGROUND);

            inputPane.add(new JLabel("Block Device:"));
            inputPane.add(new JLabel(blockDevInfo.getName()));
            removeButton.addActionListener(new RemoveActionListener());
            inputPane.add(removeButton);
            SpringUtilities.makeCompactGrid(inputPane, 1, 3,  /* rows, cols */
                                                       1, 1,  /* initX, initY */
                                                       1, 1); /* xPad, yPad */

            pane.add(inputPane);
            final JPanel hostsPane = new JPanel(
                            new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
            final Cluster cluster = blockDevInfo.getHost().getCluster();
            hostCheckBoxes = Tools.getHostCheckBoxes(cluster);
            hostsPane.add(new JLabel("Select Hosts: "));
            for (final Host h : hostCheckBoxes.keySet()) {
                hostCheckBoxes.get(h).addItemListener(
                                                new ItemChangeListener(true));
                final BlockDevInfo oBdi =
                    blockDevInfo.getBrowser().getDrbdGraph().findBlockDevInfo(
                                                      h.getName(),
                                                      blockDevInfo.getName());
                if (blockDevInfo.getHost() == h) {
                    hostCheckBoxes.get(h).setEnabled(false);
                    hostCheckBoxes.get(h).setSelected(true);
                } else if (oBdi == null
                           || !oBdi.getBlockDevice().isPhysicalVolume()) {
                    hostCheckBoxes.get(h).setEnabled(false);
                    hostCheckBoxes.get(h).setSelected(false);
                } else {
                    hostCheckBoxes.get(h).setEnabled(true);
                    hostCheckBoxes.get(h).setSelected(false);
                }
                hostsPane.add(hostCheckBoxes.get(h));
            }
            final javax.swing.JScrollPane sp = new javax.swing.JScrollPane(
                                                                   hostsPane);
            sp.setPreferredSize(new java.awt.Dimension(0, 45));
            pane.add(sp);
            pane.add(getProgressBarPane(null));
            pane.add(getAnswerPane(""));
            SpringUtilities.makeCompactGrid(pane, 4, 1,  /* rows, cols */
                                                  0, 0,  /* initX, initY */
                                                  0, 0); /* xPad, yPad */
            checkButtons();
            return pane;
        }

        /** Remove action listener. */
        private class RemoveActionListener implements ActionListener {
            public RemoveActionListener() {
                super();
            }
            @Override public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new RemoveRunnable());
                thread.start();
            }
        }

        private class RemoveRunnable implements Runnable {
            public RemoveRunnable() {
                super();
            }

            @Override public void run() {
                Tools.invokeAndWait(new EnableRemoveRunnable(false));
                disableComponents();
                getProgressBar().start(REMOVE_TIMEOUT
                                       * hostCheckBoxes.size());
                boolean oneFailed = false;
                for (final Host h : hostCheckBoxes.keySet()) {
                    if (hostCheckBoxes.get(h).isSelected()) {
                        final BlockDevInfo oBdi =
                            blockDevInfo.getBrowser().getDrbdGraph()
                                .findBlockDevInfo(h.getName(),
                                                  blockDevInfo.getName());
                        if (oBdi != null) {
                            final boolean ret = pvRemove(h, oBdi);
                            if (!ret) {
                                oneFailed = true;
                            }
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

        /** Remove PV. */
        private boolean pvRemove(final Host host,
                                 final BlockDevInfo bdi) {
            final boolean ret = bdi.pvRemove(false);
            if (ret) {
                answerPaneAddText("Labels on physical volume "
                                  + bdi.getName()
                                  + " were successfully removed "
                                  + " on " + host.getName() + ".");
            } else {
                answerPaneAddTextError("Removing labels on physical volume "
                                        + bdi.getName()
                                        + " on " + host.getName()
                                        + " failed.");
            }
            return ret;
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
