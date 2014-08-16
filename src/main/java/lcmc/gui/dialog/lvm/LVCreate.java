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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.Cluster;
import lcmc.model.Host;
import lcmc.model.StringValue;
import lcmc.model.vm.VmsXml;
import lcmc.model.Value;
import lcmc.model.resources.BlockDevice;
import lcmc.gui.Browser;
import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.gui.widget.TextfieldWithUnit;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.LVM;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import lcmc.utilities.WidgetListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Create LV dialog. */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public final class LVCreate extends LV {
    private static final String LV_CREATE_DESCRIPTION = "Create a logical volume in an existing volume group.";
    private static final int LV_CREATE_TIMEOUT = 5000;
    private final Collection<Host> selectedHosts = new LinkedHashSet<Host>();
    private Widget lvNameWidget;
    private Widget lvSizeWidget;
    private Widget maxSizeWidget;
    private String volumeGroup;
    private Map<Host, JCheckBox> hostCheckBoxes = null;
    private final Collection<BlockDevice> selectedBlockDevices = new LinkedHashSet<BlockDevice>();
    @Autowired
    private Application application;
    @Autowired
    private WidgetFactory widgetFactory;
    private final MyButton createButton = widgetFactory.createButton("Create");

    public void init(final Host host, final String volumeGroup, final BlockDevice selectedBlockDevice) {
        super.init(null);
        selectedHosts.add(host);
        this.volumeGroup = volumeGroup;
        selectedBlockDevices.add(selectedBlockDevice);
    }

    public void init(final Iterable<BlockDevInfo> sbdis, final String volumeGroup) {
        super.init(null);
        this.volumeGroup = volumeGroup;
        for (final BlockDevInfo bdi : sbdis) {
            selectedHosts.add(bdi.getHost());
            selectedBlockDevices.add(bdi.getBlockDevice());
        }
    }

    @Override
    protected String getDialogTitle() {
        return "Create LV";
    }

    @Override
    protected String getDescription() {
        return LV_CREATE_DESCRIPTION;
    }

    @Override
    public String cancelButton() {
        return "Close";
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{});
    }

    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        makeDefaultAndRequestFocusLater(lvSizeWidget.getComponent());
        makeDefaultButton(createButton);
    }

    protected void checkButtons() {
        enableCreateButton(true);
    }

    private void enableCreateButton(boolean enable) {
        application.isSwingThread();
        if (enable) {
            final String maxBlockSize = getMaxBlockSize(getSelectedHostCbs());
            final long maxSize = Long.parseLong(maxBlockSize);
            maxSizeWidget.setValue(VmsXml.convertKilobytes(maxBlockSize));
            final long size = VmsXml.convertToKilobytes(lvSizeWidget.getValue());
            if (size > maxSize || size <= 0) {
                enable = false;
                lvSizeWidget.wrongValue();
            } else {
                lvSizeWidget.setBackground(new StringValue(), new StringValue(), true);
            }
            boolean lvNameCorrect = true;
            if (lvNameWidget.getStringValue() != null && lvNameWidget.getStringValue().isEmpty()) {
                lvNameCorrect = false;
            } else if (hostCheckBoxes != null) {
                for (final Map.Entry<Host, JCheckBox> hostEntry : hostCheckBoxes.entrySet()) {
                    if (hostEntry.getValue().isSelected()) {
                        final Set<String> lvs = hostEntry.getKey().getLogicalVolumesFromVolumeGroup(volumeGroup);
                        if (lvs != null
                            && lvs.contains(lvNameWidget.getStringValue())) {
                            lvNameCorrect = false;
                            break;
                        }
                    }
                }
            }
            if (lvNameCorrect) {
                lvNameWidget.setBackground(new StringValue(), new StringValue(), true);
            } else {
                enable = false;
                lvNameWidget.wrongValue();
            }
        }
        createButton.setEnabled(enable);
    }

    @Override
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
        final Collection<String> logicalVolumes = new LinkedHashSet<String>();
        for (final Host h : selectedHosts) {
            final Set<String> hvgs = h.getLogicalVolumesFromVolumeGroup(volumeGroup);
            if (hvgs != null) {
                logicalVolumes.addAll(hvgs);
            }
        }
        int i = 0;
        String defaultName;
        while (true) {
            defaultName = "lvol" + i;
            if (!logicalVolumes.contains(defaultName)) {
                break;
            }
            i++;
        }
        lvNameWidget = widgetFactory.createInstance(
                                      Widget.Type.TEXTFIELD,
                                      new StringValue(defaultName),
                                      Widget.NO_ITEMS,
                                      Widget.NO_REGEXP,
                                      250,
                                      Widget.NO_ABBRV,
                                      new AccessMode(Application.AccessType.OP, !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);
        inputPane.add(new JLabel("LV Name"));
        inputPane.add(lvNameWidget.getComponent());
        inputPane.add(new JLabel());
        lvNameWidget.addListeners(new WidgetListener() {
            @Override
            public void check(final Value value) {
                checkButtons();
            }
        });

        final String maxBlockSize = getMaxBlockSize(selectedHosts);
        /* size */
        final String newBlockSize = Long.toString(Long.parseLong(maxBlockSize) / 2);
        final JLabel sizeLabel = new JLabel("New Size");

        lvSizeWidget = widgetFactory.createInstance(
                                       Widget.Type.TEXTFIELDWITHUNIT,
                                       VmsXml.convertKilobytes(newBlockSize),
                                       getUnits(),
                                       Widget.NO_REGEXP,
                                       250,
                                       Widget.NO_ABBRV,
                                       new AccessMode(Application.AccessType.OP, !AccessMode.ADVANCED),
                                       Widget.NO_BUTTON);
        inputPane.add(sizeLabel);
        inputPane.add(lvSizeWidget.getComponent());
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        application.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                enableCreateButton(false);
                            }
                        });
                        disableComponents();
                        getProgressBar().start(LV_CREATE_TIMEOUT * hostCheckBoxes.size());
                        boolean oneFailed = false;
                        for (final Map.Entry<Host, JCheckBox> hostEntry : hostCheckBoxes.entrySet()) {
                            if (hostEntry.getValue().isSelected()) {
                                final boolean ret = lvCreate(hostEntry.getKey(),
                                                             lvNameWidget.getStringValue(),
                                                             lvSizeWidget.getStringValue());
                                if (!ret) {
                                    oneFailed = true;
                                }
                            }
                        }
                        for (final Host h : hostCheckBoxes.keySet()) {
                            h.getBrowser().getClusterBrowser().updateHWInfo(h, Host.UPDATE_LVM);
                        }
                        final String maxBlockSize = getMaxBlockSize(getSelectedHostCbs());
                        maxSizeWidget.setValue(VmsXml.convertKilobytes(maxBlockSize));
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
        maxSizeWidget = widgetFactory.createInstance(
                                      Widget.Type.TEXTFIELDWITHUNIT,
                                      VmsXml.convertKilobytes(maxBlockSize),
                                      getUnits(),
                                      Widget.NO_REGEXP,
                                      250,
                                      Widget.NO_ABBRV,
                                      new AccessMode(Application.AccessType.OP, !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);
        maxSizeWidget.setEnabled(false);
        inputPane.add(maxSizeLabel);
        inputPane.add(maxSizeWidget.getComponent());
        inputPane.add(new JLabel());
        lvSizeWidget.addListeners(new WidgetListener() {
            @Override
            public void check(final Value value) {
                checkButtons();
            }
        });

        SpringUtilities.makeCompactGrid(inputPane, 4, 3,  /* rows, cols */
                                                   1, 1,  /* initX, initY */
                                                   1, 1); /* xPad, yPad */

        pane.add(inputPane);
        final JPanel hostsPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        final Cluster cluster = selectedHosts.iterator().next().getCluster();
        hostCheckBoxes = Tools.getHostCheckBoxes(cluster);
        hostsPane.add(new JLabel("Select Hosts: "));
        for (final Map.Entry<Host, JCheckBox> hostEntry : hostCheckBoxes.entrySet()) {
            hostEntry.getValue().addItemListener(
                    new ItemListener() {
                        @Override
                        public void itemStateChanged(final ItemEvent e) {
                            checkButtons();
                        }
                    });
            if (selectedHosts.contains(hostEntry.getKey())) {
                hostEntry.getValue().setEnabled(false);
                hostEntry.getValue().setSelected(true);
            } else if (isOneBdDrbd(selectedBlockDevices)
                       || !hostEntry.getKey().getVolumeGroupNames().contains(volumeGroup)) {
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
        checkButtons();
        return pane;
    }

    private boolean lvCreate(final Host host, final String lvName, final String size) {
        final boolean ret = LVM.lvCreate(host, lvName, volumeGroup, size, Application.RunMode.LIVE);
        if (ret) {
            answerPaneAddText("Logical volume "
                              + lvName
                              + " was successfully created in "
                              + volumeGroup
                              + " on " + host.getName() + '.');
        } else {
            answerPaneAddTextError("Creating of logical volume " + lvName + " failed.");
        }
        return ret;
    }

    private String getMaxBlockSize(final Iterable<Host> hosts) {
        long free = -1;
        if (hosts != null) {
            for (final Host h : hosts) {
                final long hostFree = h.getFreeInVolumeGroup(volumeGroup) / 1024;
                if (free == -1 || hostFree < free) {
                    free = hostFree;
                }
            }
        }
        return Long.toString(free);
    }

    protected boolean isOneBdDrbd(final Iterable<BlockDevice> bds) {
        for (final BlockDevice bd : bds) {
            if (bd.isDrbd()) {
                return true;
            }
        }
        return false;
    }

    private Iterable<Host> getSelectedHostCbs() {
        final Collection<Host> hosts = new HashSet<Host>();
        if (hostCheckBoxes == null) {
            return hosts;
        }
        for (final Map.Entry<Host, JCheckBox> e : hostCheckBoxes.entrySet()) {
            final Host h = e.getKey();
            if (e.getValue().isSelected()) {
                hosts.add(h);
            }
        }
        return hosts;
    }
}
