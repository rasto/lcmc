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
import lcmc.gui.resources.DrbdVolumeInfo;

import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;
import lcmc.utilities.LVM;
import lcmc.utilities.WidgetListener;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.data.Host;
import lcmc.data.Cluster;
import lcmc.data.resources.BlockDevice;
import lcmc.gui.Widget;
import lcmc.gui.Browser;

import java.util.Set;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.JCheckBox;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * This class implements LVM resize dialog.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class LVResize extends LV {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Description LVM resize. */
    private static final String DESCRIPTION =
                   "Resize the LVM volume. You can make it bigger, but not"
                   + " smaller for now. If this volume is replicated by"
                   + " DRBD, volumes on both nodes will be resized and"
                   + " drbdadm resize will be called. If you have something"
                   + " like filesystem on the DRBD, you have to resize the"
                   + " filesystem yourself.";
    /** Resize LV timeout. */
    private static final int RESIZE_TIMEOUT = 5000;
    /** Block device info object. */
    private final BlockDevInfo blockDevInfo;
    /** Resize button. */
    private final MyButton resizeButton = new MyButton("Resize");
    /** Size combo box. */
    private Widget sizeWi;
    /** Old size combo box. */
    private Widget oldSizeWi;
    /** Max size combo box. */
    private Widget maxSizeWi;
    /** Map from host to the checkboxes for these hosts. */
    private Map<Host, JCheckBox> hostCheckBoxes = null;
    /** Create new LVResize object. */
    public LVResize(final BlockDevInfo blockDevInfo) {
        super(null);
        this.blockDevInfo = blockDevInfo;
    }

    /** Finishes the dialog and sets the information. */
    protected void finishDialog() {
        /* disable finish button */
    }

    /** Returns the title of the dialog. */
    protected String getDialogTitle() {
        return "LVM Resize";
    }

    /** Returns the description of the dialog. */
    protected String getDescription() {
        return DESCRIPTION;
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
        if (checkDRBD()) {
            makeDefaultAndRequestFocusLater(sizeWi);
        }
    }

    /** Check if it is DRBD device and if it could be resized. */
    private boolean checkDRBD() {
        if (blockDevInfo.getBlockDevice().isDrbd()) {
            final DrbdVolumeInfo dvi = blockDevInfo.getDrbdVolumeInfo();
            final BlockDevInfo oBDI = blockDevInfo.getOtherBlockDevInfo();
            if (!dvi.isConnected(false)) {
                printErrorAndRetry(
                              "Not resizing. DRBD resource is not connected.");
                sizeWi.setEnabled(false);
                resizeButton.setEnabled(false);
                return false;
            } else if (dvi.isSyncing()) {
                printErrorAndRetry(
                                "Not resizing. DRBD resource is syncing.");
                sizeWi.setEnabled(false);
                resizeButton.setEnabled(false);
                return false;
            } else if (!oBDI.getBlockDevice().isAttached()) {
                printErrorAndRetry(
                        "Not resizing. DRBD resource is not attached on "
                        + oBDI.getHost() + ".");
                sizeWi.setEnabled(false);
                resizeButton.setEnabled(false);
                return false;
            } else if (!blockDevInfo.getBlockDevice().isAttached()) {
                printErrorAndRetry(
                        "Not resizing. DRBD resource is not attached on "
                        + blockDevInfo.getHost() + ".");
                sizeWi.setEnabled(false);
                resizeButton.setEnabled(false);
                return false;
            } else if (!oBDI.getBlockDevice().isPrimary()
                       && !blockDevInfo.getBlockDevice().isPrimary()) {
                printErrorAndRetry(
                   "Not resizing. Must be primary at least on one node.");
                sizeWi.setEnabled(false);
                resizeButton.setEnabled(false);
                return false;
            }
        }
        return true;
    }

    /** Enables and disabled buttons. */
    protected void checkButtons() {
        SwingUtilities.invokeLater(new EnableResizeRunnable(true));
    }

    private class EnableResizeRunnable implements Runnable {
        private final boolean enable;
        public EnableResizeRunnable(final boolean enable) {
            super();
            this.enable = enable;
        }

        @Override
        public void run() {
            boolean e = enable;
            if (enable) {
                final long oldSize = Tools.convertToKilobytes(
                                               oldSizeWi.getStringValue());
                final long size = Tools.convertToKilobytes(
                                                  sizeWi.getStringValue());
                final String maxBlockSize = getMaxBlockSize();
                final long maxSize = Long.parseLong(maxBlockSize);
                maxSizeWi.setValue(Tools.convertKilobytes(maxBlockSize));

                if (oldSize >= size || size > maxSize) {
                    e = false;
                    sizeWi.wrongValue();
                } else {
                    sizeWi.setBackground("", "", true);
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
        oldSizeWi.setValue(Tools.convertKilobytes(oldBlockSize));
        sizeWi.setValue(Tools.convertKilobytes(Long.toString(
                                    (Long.parseLong(oldBlockSize)
                                     + Long.parseLong(maxBlockSize)) / 2)));
        maxSizeWi.setValue(Tools.convertKilobytes(maxBlockSize));
    }

    /** Returns the input pane. */
    protected JComponent getInputPane() {
        resizeButton.setEnabled(false);
        final JPanel pane = new JPanel(new SpringLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setBackground(Browser.BUTTON_PANEL_BACKGROUND);
        /* old size */
        final JLabel oldSizeLabel = new JLabel("Current Size");
        oldSizeLabel.setEnabled(false);

        final String oldBlockSize =
                            blockDevInfo.getBlockDevice().getBlockSize();
        oldSizeWi = new Widget(Tools.convertKilobytes(oldBlockSize),
                               null,
                               getUnits(),
                               Widget.Type.TEXTFIELDWITHUNIT,
                               null, /* regexp */
                               250,
                               null, /* abbrv */
                               new AccessMode(ConfigData.AccessType.OP,
                                             false)); /* only adv. */
        oldSizeWi.setEnabled(false);
        inputPane.add(oldSizeLabel);
        inputPane.add(oldSizeWi);
        inputPane.add(new JLabel());

        final String maxBlockSize = getMaxBlockSize();
        /* size */
        final String newBlockSize = Long.toString(
                                     (Long.parseLong(oldBlockSize)
                                      + Long.parseLong(maxBlockSize)) / 2);
        final JLabel sizeLabel = new JLabel("New Size");

        sizeWi = new Widget(Tools.convertKilobytes(newBlockSize),
                            null,
                            getUnits(), /* units */
                            Widget.Type.TEXTFIELDWITHUNIT,
                            null, /* regexp */
                            250,
                            null, /* abbrv */
                            new AccessMode(ConfigData.AccessType.OP,
                                             false)); /* only adv. */
        inputPane.add(sizeLabel);
        inputPane.add(sizeWi);
        resizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (checkDRBD()) {
                            Tools.invokeAndWait(
                                               new EnableResizeRunnable(false));
                            disableComponents();
                            getProgressBar().start(RESIZE_TIMEOUT
                                                   * hostCheckBoxes.size());
                            final boolean ret = resize(sizeWi.getStringValue());
                            final Host host = blockDevInfo.getHost();
                            host.getBrowser().getClusterBrowser().updateHWInfo(
                                                                          host);
                            setComboBoxes();
                            if (ret) {
                                progressBarDone();
                            } else {
                                progressBarDoneError();
                            }
                            enableComponents();
                        }
                    }
                });
                thread.start();
            }
        });

        inputPane.add(resizeButton);
        /* max size */
        final JLabel maxSizeLabel = new JLabel("Max Size");
        maxSizeLabel.setEnabled(false);
        maxSizeWi = new Widget(Tools.convertKilobytes(maxBlockSize),
                               null,
                               getUnits(),
                               Widget.Type.TEXTFIELDWITHUNIT,
                               null, /* regexp */
                               250,
                               null, /* abbrv */
                               new AccessMode(ConfigData.AccessType.OP,
                                              false)); /* only adv. */
        maxSizeWi.setEnabled(false);
        inputPane.add(maxSizeLabel);
        inputPane.add(maxSizeWi);
        inputPane.add(new JLabel());
        sizeWi.addListeners(new WidgetListener() {
                                @Override
                                public void check(final Object value) {
                                    checkButtons();
                                }
                            });

        SpringUtilities.makeCompactGrid(inputPane, 3, 3,  /* rows, cols */
                                                   1, 1,  /* initX, initY */
                                                   1, 1); /* xPad, yPad */

        pane.add(inputPane);
        final JPanel hostsPane = new JPanel(
                        new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        final Cluster cluster = blockDevInfo.getHost().getCluster();
        hostCheckBoxes = Tools.getHostCheckBoxes(cluster);
        hostsPane.add(new JLabel("Select Hosts: "));
        final Host host = blockDevInfo.getHost();
        final String lv = blockDevInfo.getBlockDevice().getLogicalVolume();
        for (final Host h : hostCheckBoxes.keySet()) {
            final Set<String> allLVS = h.getAllLogicalVolumes();
            hostCheckBoxes.get(h).addItemListener(
                        new ItemListener() {
                            @Override
                            public void itemStateChanged(final ItemEvent e) {
                                checkButtons();
                            }
                        });
            if (host == h) {
                hostCheckBoxes.get(h).setEnabled(false);
                hostCheckBoxes.get(h).setSelected(true);
            } else if (blockDevInfo.getBlockDevice().isDrbd()
                       && blockDevInfo.getOtherBlockDevInfo().getHost()
                          == h) {
                hostCheckBoxes.get(h).setEnabled(false);
                hostCheckBoxes.get(h).setSelected(true);
            } else if (!blockDevInfo.getBlockDevice().isDrbd()
                       && !allLVS.contains(lv)) {
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

    /** LVM Resize and DRBD Resize. */
    private boolean resize(final String size) {
        final boolean ret = LVM.resize(
                                   blockDevInfo.getHost(),
                                   blockDevInfo.getBlockDevice().getName(),
                                   size,
                                   false);
        if (ret) {
            answerPaneSetText("Lodical volume was successfully resized on "
                              + blockDevInfo.getHost() + ".");
            /* resize lvm volume on the other node. */
            final String lv =
                    blockDevInfo.getBlockDevice().getLogicalVolume();
            final BlockDevInfo oBDI = blockDevInfo.getOtherBlockDevInfo();
            boolean resizingFailed = false;
            for (final Host h : hostCheckBoxes.keySet()) {
                if (h == blockDevInfo.getHost()
                    || !hostCheckBoxes.get(h).isSelected()) {
                    continue;
                }
                for (final BlockDevice b : h.getBlockDevices()) {
                    if (lv.equals(b.getLogicalVolume())
                        || (oBDI != null && oBDI.getBlockDevice() == b)) {
                        /* drbd or selected other host */
                        final boolean oRet = LVM.resize(h,
                                                        b.getName(),
                                                        size,
                                                        false);
                        if (oRet) {
                            answerPaneAddText("Lodical volume was successfully"
                                              + " resized on "
                                              + h.getName() + ".");
                        } else {
                            answerPaneAddTextError("Resizing of "
                                                   + b.getName()
                                                   + " on host "
                                                   + h.getName()
                                                   + " failed.");
                            resizingFailed = true;
                        }
                        break;
                    }
                    if (resizingFailed) {
                        break;
                    }
                }
            }
            if (oBDI != null && !resizingFailed) {
                final boolean dRet = blockDevInfo.resizeDrbd(false);
                if (dRet) {
                    answerPaneAddText(
                         "DRBD resource "
                         + blockDevInfo.getDrbdVolumeInfo().getName()
                         + " was successfully resized.");
                } else {
                    answerPaneAddTextError(
                         "DRBD resource "
                         + blockDevInfo.getDrbdVolumeInfo().getName()
                         + " resizing failed.");
                }
            }
        } else {
            answerPaneAddTextError("Resizing of "
                                   + blockDevInfo.getName()
                                   + " on host "
                                   + blockDevInfo.getHost()
                                   + " failed.");
        }
        return ret;
    }

    /** Returns maximum block size available in the group. */
    private String getMaxBlockSize() {
        final long free = blockDevInfo.getFreeInVolumeGroup() / 1024;

        String maxBlockSize = "0";
        try {
            final long taken = Long.parseLong(
                             blockDevInfo.getBlockDevice().getBlockSize());
            final BlockDevInfo oBDI = blockDevInfo.getOtherBlockDevInfo();
            long max = free + taken;
            final String lv =
                        blockDevInfo.getBlockDevice().getLogicalVolume();
            if (hostCheckBoxes != null) {
                for (final Host h : hostCheckBoxes.keySet()) {
                    if (blockDevInfo.getHost() == h) {
                        continue;
                    }
                    if (hostCheckBoxes.get(h).isSelected()) {
                        for (final BlockDevice b : h.getBlockDevices()) {
                            if (lv.equals(b.getLogicalVolume())
                                || (oBDI != null
                                    && oBDI.getBlockDevice() == b)) {
                                final long oFree =
                                 h.getFreeInVolumeGroup(b.getVolumeGroup())
                                 / 1024;
                                final long oTaken = Long.parseLong(
                                                         b.getBlockSize());
                                if (oFree + oTaken < max) {
                                    /* take the smaller maximum. */
                                    max = oFree + oTaken;
                                }
                            }
                        }
                    }
                }
            }
            maxBlockSize = Long.toString(max);
        } catch (final Exception e) {
            Tools.appWarning("could not get max size");
            /* ignore */
        }
        return maxBlockSize;
    }
}
