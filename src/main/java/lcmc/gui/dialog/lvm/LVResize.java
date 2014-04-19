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

package lcmc.gui.dialog.lvm;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.Cluster;
import lcmc.data.Host;
import lcmc.data.StringValue;
import lcmc.data.VMSXML;
import lcmc.data.Value;
import lcmc.data.resources.BlockDevice;
import lcmc.gui.Browser;
import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.gui.resources.drbd.VolumeInfo;
import lcmc.gui.widget.TextfieldWithUnit;
import lcmc.gui.widget.Widget;
import lcmc.utilities.LVM;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import lcmc.utilities.WidgetListener;

/**
 * This class implements LVM resize dialog.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class LVResize extends LV {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(LVResize.class);
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

    /** Returns the title of the dialog. */
    @Override
    protected String getDialogTitle() {
        return "LVM Resize";
    }

    /** Returns the description of the dialog. */
    @Override
    protected String getDescription() {
        return DESCRIPTION;
    }

    /** Close button. */
    @Override
    public String cancelButton() {
        return "Close";
    }


    /** Inits the dialog. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        if (checkDRBD()) {
            makeDefaultAndRequestFocusLater(sizeWi.getComponent());
        }
    }

    /** Check if it is DRBD device and if it could be resized. */
    private boolean checkDRBD() {
        if (blockDevInfo.getBlockDevice().isDrbd()) {
            final VolumeInfo dvi = blockDevInfo.getDrbdVolumeInfo();
            final BlockDevInfo oBDI = blockDevInfo.getOtherBlockDevInfo();
            if (!dvi.isConnected(Application.RunMode.LIVE)) {
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
                        + oBDI.getHost() + '.');
                sizeWi.setEnabled(false);
                resizeButton.setEnabled(false);
                return false;
            } else if (!blockDevInfo.getBlockDevice().isAttached()) {
                printErrorAndRetry(
                        "Not resizing. DRBD resource is not attached on "
                        + blockDevInfo.getHost() + '.');
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

    /** Enable remove button. */
    private void enableResizeButton(boolean enable) {
        if (enable) {
            final long oldSize = VMSXML.convertToKilobytes(
                                           oldSizeWi.getValue());
            final long size = VMSXML.convertToKilobytes(
                                              sizeWi.getValue());
            final String maxBlockSize = getMaxBlockSize();
            final long maxSize = Long.parseLong(maxBlockSize);
            maxSizeWi.setValue(VMSXML.convertKilobytes(maxBlockSize));

            if (oldSize >= size || size > maxSize) {
                enable = false;
                sizeWi.wrongValue();
            } else {
                sizeWi.setBackground(new StringValue(), new StringValue(), true);
            }
        }
        resizeButton.setEnabled(enable);
    }

    /** Set combo boxes with new values. */
    private void setComboBoxes() {
        final String oldBlockSize =
                            blockDevInfo.getBlockDevice().getBlockSize();
        final String maxBlockSize = getMaxBlockSize();
        oldSizeWi.setValue(VMSXML.convertKilobytes(oldBlockSize));
        sizeWi.setValue(VMSXML.convertKilobytes(Long.toString(
                                    (Long.parseLong(oldBlockSize)
                                     + Long.parseLong(maxBlockSize)) / 2)));
        maxSizeWi.setValue(VMSXML.convertKilobytes(maxBlockSize));
    }

    /** Returns the input pane. */
    @Override
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
        oldSizeWi = new TextfieldWithUnit(
                      VMSXML.convertKilobytes(oldBlockSize),
                      getUnits(),
                      Widget.NO_REGEXP,
                      250,
                      Widget.NO_ABBRV,
                      new AccessMode(Application.AccessType.OP,
                                     !AccessMode.ADVANCED),
                      Widget.NO_BUTTON);
        oldSizeWi.setEnabled(false);
        inputPane.add(oldSizeLabel);
        inputPane.add(oldSizeWi.getComponent());
        inputPane.add(new JLabel());

        final String maxBlockSize = getMaxBlockSize();
        /* size */
        final String newBlockSize = Long.toString(
                                     (Long.parseLong(oldBlockSize)
                                      + Long.parseLong(maxBlockSize)) / 2);
        final JLabel sizeLabel = new JLabel("New Size");

        sizeWi = new TextfieldWithUnit(
                       VMSXML.convertKilobytes(newBlockSize),
                       getUnits(),
                       Widget.NO_REGEXP,
                       250,
                       Widget.NO_ABBRV,
                       new AccessMode(Application.AccessType.OP,
                                      !AccessMode.ADVANCED),
                       Widget.NO_BUTTON);
        inputPane.add(sizeLabel);
        inputPane.add(sizeWi.getComponent());
        resizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (checkDRBD()) {
                            Tools.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    enableResizeButton(false);
                                }
                            });
                            disableComponents();
                            getProgressBar().start(RESIZE_TIMEOUT
                                                   * hostCheckBoxes.size());
                            final boolean ret = resize(sizeWi.getStringValue());
                            final Host host = blockDevInfo.getHost();
                            host.getBrowser().getClusterBrowser().updateHWInfo(
                                                          host,
                                                          Host.UPDATE_LVM);
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
        maxSizeWi = new TextfieldWithUnit(
                       VMSXML.convertKilobytes(maxBlockSize),
                       getUnits(),
                       Widget.NO_REGEXP,
                       250,
                       Widget.NO_ABBRV,
                       new AccessMode(Application.AccessType.OP,
                                      !AccessMode.ADVANCED),
                       Widget.NO_BUTTON);
        maxSizeWi.setEnabled(false);
        inputPane.add(maxSizeLabel);
        inputPane.add(maxSizeWi.getComponent());
        inputPane.add(new JLabel());
        sizeWi.addListeners(new WidgetListener() {
                                @Override
                                public void check(final Value value) {
                                    enableResizeButton(true);
                                }
                            });

        SpringUtilities.makeCompactGrid(inputPane, 3, 3,  /* rows, cols */
                                                   1, 1,  /* initX, initY */
                                                   1, 1); /* xPad, yPad */

        pane.add(inputPane);
        final JPanel hostsPane = new JPanel(
                        new FlowLayout(FlowLayout.LEADING));
        final Cluster cluster = blockDevInfo.getHost().getCluster();
        hostCheckBoxes = Tools.getHostCheckBoxes(cluster);
        hostsPane.add(new JLabel("Select Hosts: "));
        final Host host = blockDevInfo.getHost();
        final String lv = blockDevInfo.getBlockDevice().getLogicalVolume();
        for (final Map.Entry<Host, JCheckBox> hostEntry : hostCheckBoxes.entrySet()) {
            final Set<String> allLVS = hostEntry.getKey().getAllLogicalVolumes();
            hostEntry.getValue().addItemListener(
                    new ItemListener() {
                        @Override
                        public void itemStateChanged(final ItemEvent e) {
                            enableResizeButton(true);
                        }
                    });
            if (host == hostEntry.getKey()
                || blockDevInfo.getBlockDevice().isDrbd()
                && blockDevInfo.getOtherBlockDevInfo().getHost()
                   == hostEntry.getKey()) {
                hostEntry.getValue().setEnabled(false);
                hostEntry.getValue().setSelected(true);
            } else if (!blockDevInfo.getBlockDevice().isDrbd()
                       && !allLVS.contains(lv)) {
                hostEntry.getValue().setEnabled(false);
                hostEntry.getValue().setSelected(false);
            } else {
                hostEntry.getValue().setEnabled(true);
                hostEntry.getValue().setSelected(false);
            }
            hostsPane.add(hostEntry.getValue());
        }
        final JScrollPane sp = new JScrollPane(hostsPane);
        sp.setPreferredSize(new Dimension(0, 45));
        pane.add(sp);
        pane.add(getProgressBarPane(null));
        pane.add(getAnswerPane(""));
        SpringUtilities.makeCompactGrid(pane, 4, 1,  /* rows, cols */
                                              0, 0,  /* initX, initY */
                                              0, 0); /* xPad, yPad */
        enableResizeButton(true);
        return pane;
    }

    /** LVM Resize and DRBD Resize. */
    private boolean resize(final String size) {
        final boolean ret = LVM.resize(blockDevInfo.getHost(),
                                       blockDevInfo.getBlockDevice().getName(),
                                       size,
                                       Application.RunMode.LIVE);
        if (ret) {
            answerPaneSetText("Logical volume was successfully resized on "
                              + blockDevInfo.getHost() + '.');
            /* resize lvm volume on the other node. */
            final String lvm = blockDevInfo.getBlockDevice().getName();
            final BlockDevInfo oBDI = blockDevInfo.getOtherBlockDevInfo();
            boolean resizingFailed = false;
            for (final Map.Entry<Host, JCheckBox> hostEntry : hostCheckBoxes.entrySet()) {
                if (hostEntry.getKey() == blockDevInfo.getHost()
                    || !hostEntry.getValue().isSelected()) {
                    continue;
                }
                for (final BlockDevice b : hostEntry.getKey().getBlockDevices()) {
                    if (lvm.equals(b.getName())
                        || (oBDI != null && oBDI.getBlockDevice() == b)) {
                        /* drbd or selected other host */
                        final boolean oRet = LVM.resize(hostEntry.getKey(),
                                                        b.getName(),
                                                        size,
                                                        Application.RunMode.LIVE);
                        if (oRet) {
                            answerPaneAddText("Logical volume was successfully"
                                              + " resized on "
                                              + hostEntry.getKey().getName() + '.');
                        } else {
                            answerPaneAddTextError("Resizing of "
                                                   + b.getName()
                                                   + " on host "
                                                   + hostEntry.getKey().getName()
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
                final boolean dRet = blockDevInfo.resizeDrbd(Application.RunMode.LIVE);
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
            final String lvm =
                        blockDevInfo.getBlockDevice().getName();
            if (hostCheckBoxes != null) {
                for (final Map.Entry<Host, JCheckBox> hostEntry : hostCheckBoxes.entrySet()) {
                    if (blockDevInfo.getHost() == hostEntry.getKey()) {
                        continue;
                    }
                    if (hostEntry.getValue().isSelected()) {
                        for (final BlockDevice b : hostEntry.getKey().getBlockDevices()) {
                            if (lvm.equals(b.getName())
                                || (oBDI != null
                                    && oBDI.getBlockDevice() == b)) {
                                final long oFree =
                                        hostEntry.getKey().getFreeInVolumeGroup(b.getVolumeGroup())
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
        } catch (final NumberFormatException e) {
            LOG.appWarning("getMaxBlockSize: could not get max size");
            /* ignore */
        }
        return maxBlockSize;
    }
}
