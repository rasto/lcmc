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

package lcmc.lvm.ui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;

import lcmc.gui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.cluster.domain.Cluster;
import lcmc.host.domain.Host;
import lcmc.drbd.domain.BlockDevice;
import lcmc.gui.Browser;
import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.utilities.LVM;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;

/**
 * This class implements VG Remove dialog.
 */
@Named
public final class VGRemove extends LV {
    private static final int REMOVE_TIMEOUT = 5000;
    private static final String VG_REMOVE_DESCRIPTION = "Remove a volume group.";
    @Inject
    private WidgetFactory widgetFactory;
    private MyButton removeButton;
    private final List<BlockDevInfo> blockDevInfos = new ArrayList<BlockDevInfo>();
    private Map<Host, JCheckBox> hostCheckBoxes = null;
    private boolean multiSelection;
    @Inject
    private Application application;

    public void init(final BlockDevInfo bdi) {
        super.init(null);
        blockDevInfos.add(bdi);
        multiSelection = false;
    }

    public void init(final Collection<BlockDevInfo> bdis) {
        super.init(null);
        blockDevInfos.addAll(bdis);
        multiSelection = true;
    }

    @Override
    protected String getDialogTitle() {
        return "Remove VG";
    }

    @Override
    protected String getDescription() {
        return VG_REMOVE_DESCRIPTION;
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{});
    }

    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        makeDefaultAndRequestFocus(removeButton);
    }

    private String getVGName(final BlockDevInfo bdi) {
        if (bdi.getBlockDevice().isDrbd()) {
            return bdi.getBlockDevice().getDrbdBlockDevice().getVolumeGroupOnPhysicalVolume();
        } else {
            return bdi.getBlockDevice().getVolumeGroupOnPhysicalVolume();
        }
    }

    private Map<Host, Set<String>> getVGNames() {
        final Map<Host, Set<String>> vgNames = new LinkedHashMap<Host, Set<String>>();
        for (final BlockDevInfo bdi : blockDevInfos) {
            final Host h = bdi.getHost();
            Set<String> vgs = vgNames.get(h);
            if (vgs == null) {
                vgs = new LinkedHashSet<String>();
                vgNames.put(h, vgs);
            }
            vgs.add(getVGName(bdi));
        }
        return vgNames;
    }

    @Override
    protected JComponent getInputPane() {
        removeButton = widgetFactory.createButton("Remove VG");
        removeButton.setEnabled(false);
        final JPanel pane = new JPanel(new SpringLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setBackground(Browser.BUTTON_PANEL_BACKGROUND);

        inputPane.add(new JLabel("Volume Groups: "));
        final StringBuilder vgNamesString = new StringBuilder();

        final Map<Host, Set<String>> vgNames = getVGNames();

        for (final Map.Entry<Host, Set<String>> entry : vgNames.entrySet()) {
            final Host h = entry.getKey();
            vgNamesString.append(h.getName()).append(": ");
            final Set<String> vgs = entry.getValue();
            vgNamesString.append(Tools.join(", ", vgs)).append(' ');
        }

        inputPane.add(new JLabel(vgNamesString.toString()));
        removeButton.addActionListener(new RemoveActionListener());
        inputPane.add(removeButton);
        SpringUtilities.makeCompactGrid(inputPane, 1, 3,  /* rows, cols */
                                                   1, 1,  /* initX, initY */
                                                   1, 1); /* xPad, yPad */

        pane.add(inputPane);
        final JPanel bdPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        bdPane.add(new JLabel("Block Devices: "));
        final Collection<String> bds = new HashSet<String>();
        final Collection<Host> selectedHosts = new HashSet<Host>();
        for (final BlockDevInfo bdi : blockDevInfos) {
            for (final BlockDevice bd : bdi.getHost().getBlockDevices()) {
                final String thisVG = bd.getVolumeGroupOnPhysicalVolume();
                if (vgNames.get(bdi.getHost()).contains(thisVG)) {
                    bds.add(bd.getName());
                }
            }
            if (bdi.getBlockDevice().isDrbd()) {
                for (final BlockDevice bd : bdi.getHost().getDrbdBlockDevices()) {
                    final String thisVG = bd.getVolumeGroupOnPhysicalVolume();
                    if (vgNames.get(bdi.getHost()).contains(thisVG)) {
                        bds.add(bd.getName());
                    }
                }
            }
            selectedHosts.add(bdi.getHost());
        }
        bdPane.add(new JLabel(Tools.join(", ", bds)));
        pane.add(bdPane);
        final JPanel hostsPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        final Host host = blockDevInfos.get(0).getHost();
        final Cluster cluster = host.getCluster();
        hostCheckBoxes = Tools.getHostCheckBoxes(cluster);
        hostsPane.add(new JLabel("Select Hosts: "));
        for (final Map.Entry<Host, JCheckBox> hostEntry : hostCheckBoxes.entrySet()) {
            hostEntry.getValue().addItemListener(new ItemChangeListener(true));
            if (host == hostEntry.getKey()) {
                hostEntry.getValue().setEnabled(false);
                hostEntry.getValue().setSelected(true);
            } else if (isOneDrbd(blockDevInfos)) {
                hostEntry.getValue().setEnabled(false);
                hostEntry.getValue().setSelected(false);
            } else {
                hostEntry.getValue().setEnabled(true);
                hostEntry.getValue().setSelected(selectedHosts.contains(hostEntry.getKey()));
            }
            hostsPane.add(hostEntry.getValue());
        }
        final JScrollPane sp = new JScrollPane(hostsPane);
        sp.setPreferredSize(new Dimension(0, 45));
        pane.add(sp);
        pane.add(getProgressBarPane(null));
        pane.add(getAnswerPane(""));
        SpringUtilities.makeCompactGrid(pane, 5, 1,  /* rows, cols */
                                              0, 0,  /* initX, initY */
                                              0, 0); /* xPad, yPad */
        removeButton.setEnabled(true);
        return pane;
    }

    private boolean vgRemove(final Host host, final String vgName) {
        final boolean ret = LVM.vgRemove(host, vgName, Application.RunMode.LIVE);
        if (ret) {
            answerPaneAddText("Volume group " + vgName + " was successfully removed " + " on " + host.getName() + '.');
        } else {
            answerPaneAddTextError("Removing volume group " + vgName + " on " + host.getName() + " failed.");
        }
        return ret;
    }

    /** Remove action listener. */
    private class RemoveActionListener implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    application.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            removeButton.setEnabled(false);
                        }
                    });
                    disableComponents();
                    getProgressBar().start(REMOVE_TIMEOUT * hostCheckBoxes.size());
                    boolean oneFailed = false;

                    if (multiSelection) {
                        final Map<Host, Set<String>> vgNames = getVGNames();
                        for (final Map.Entry<Host, Set<String>> entry : vgNames.entrySet()) {
                            final Host h = entry.getKey();
                            for (final String vgName : entry.getValue()) {
                                if (hostCheckBoxes.get(h).isSelected()) {
                                    final boolean ret = vgRemove(h, vgName);
                                    if (!ret) {
                                        oneFailed = true;
                                    }
                                }
                            }
                        }
                    } else {
                        for (final Map.Entry<Host, JCheckBox> hostEntry : hostCheckBoxes.entrySet()) {
                            if (hostEntry.getValue().isSelected()) {
                                final boolean ret = vgRemove(hostEntry.getKey(), getVGName(blockDevInfos.get(0)));
                                if (!ret) {
                                    oneFailed = true;
                                }
                            }
                        }
                    }
                    for (final Map.Entry<Host, JCheckBox> hostEntry : hostCheckBoxes.entrySet()) {
                        if (hostEntry.getValue().isSelected()) {
                            hostEntry.getKey().getBrowser().getClusterBrowser().updateHWInfo(hostEntry.getKey(),
                                                                                             Host.UPDATE_LVM);
                        }
                    }
                    enableComponents();
                    if (oneFailed) {
                        application.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                removeButton.setEnabled(true);
                            }
                        });
                        progressBarDoneError();
                    } else {
                        progressBarDone();
                        disposeDialog();
                    }
                }
            });
            thread.start();
        }
    }

    /** Size combo box item listener. */
    private class ItemChangeListener implements ItemListener {
        /** Whether to check buttons on both select and deselect. */
        private final boolean onDeselect;

        ItemChangeListener(final boolean onDeselect) {
            super();
            this.onDeselect = onDeselect;
        }
        @Override
        public void itemStateChanged(final ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED || onDeselect) {
                removeButton.setEnabled(true);
            }
        }
    }
}
