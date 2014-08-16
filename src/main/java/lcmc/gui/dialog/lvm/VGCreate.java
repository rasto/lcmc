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

package lcmc.gui.dialog.lvm;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import lcmc.model.resources.BlockDevice;
import lcmc.gui.Browser;
import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.LVM;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Create VG dialog. */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public final class VGCreate extends LV {
    private static final String VG_CREATE_DESCRIPTION = "Create a volume group.";
    private static final int CREATE_TIMEOUT = 5000;
    private Host host;
    private final Collection<BlockDevInfo> selectedBlockDevInfos = new ArrayList<BlockDevInfo>();
    private Widget vgNameWi;
    private Map<Host, JCheckBox> hostCheckBoxes = null;
    private Map<String, JCheckBox> pvCheckBoxes = null;
    @Autowired
    private Application application;
    @Autowired
    private WidgetFactory widgetFactory;
    private final MyButton createButton = widgetFactory.createButton("Create VG");

    public void init(final Host host) {
        super.init(null);
        this.host = host;
    }

    public void init(final Host host, final BlockDevInfo sbdi) {
        super.init(null);
        this.host = host;
        selectedBlockDevInfos.add(sbdi);
    }

    public void init(final Host host, final Collection<BlockDevInfo> sbdis) {
        super.init(null);
        this.host = host;
        selectedBlockDevInfos.addAll(sbdis);
    }

    @Override
    protected String getDialogTitle() {
        return "Create VG";
    }

    @Override
    protected String getDescription() {
        return VG_CREATE_DESCRIPTION;
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        enableComponentsLater(new JComponent[]{});
    }

    @Override
    protected void initDialogAfterVisible() {
        enableComponents();
        makeDefaultAndRequestFocusLater(vgNameWi.getComponent());
        makeDefaultButton(createButton);
    }

    protected void checkButtons() {
        boolean enable = true;
        for (final Map.Entry<Host, JCheckBox> hostEntry : hostCheckBoxes.entrySet()) {
            if (hostEntry.getValue().isSelected() && !hostHasPVSWithoutVGs(hostEntry.getKey())) {
                enable = false;
                break;
            }
        }
        enableCreateButton(enable);
    }

    private void enableCreateButton(boolean enable) {
        if (enable) {
            boolean vgNameCorrect = true;
            if (vgNameWi.getStringValue() != null && vgNameWi.getStringValue().isEmpty()) {
                vgNameCorrect = false;
            } else if (hostCheckBoxes != null) {
                for (final Map.Entry<Host, JCheckBox> hostEntry : hostCheckBoxes.entrySet()) {
                    if (hostEntry.getValue().isSelected()) {
                        final Set<String> vgs = hostEntry.getKey().getVolumeGroupNames();
                        if (vgs != null && vgs.contains(vgNameWi.getStringValue())) {
                            vgNameCorrect = false;
                            break;
                        }
                    }
                }
            }
            if (vgNameCorrect) {
                vgNameWi.setBackground(new StringValue(), new StringValue(), true);
            } else {
                enable = false;
                vgNameWi.wrongValue();
            }
        }
        createButton.setEnabled(enable);
    }

    private Map<String, JCheckBox> getPVCheckBoxes(final Collection<String> selectedPVs) {
        final Map<String, JCheckBox> components = new LinkedHashMap<String, JCheckBox>();
        for (final BlockDevice pv : host.getPhysicalVolumes()) {
            final String pvName = pv.getName();
            final JCheckBox button = new JCheckBox(pvName, selectedPVs.contains(pvName));
            button.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Light"));
            components.put(pvName, button);
        }
        return components;
    }

    private boolean hostHasPVSWithoutVGs(final Host host) {
        final Map<String, BlockDevice> oPVS = new HashMap<String, BlockDevice>();
        for (final BlockDevice bd : host.getPhysicalVolumes()) {
            oPVS.put(bd.getName(), bd);
        }
        int selected = 0;
        for (final Map.Entry<String, JCheckBox> pvEntry : pvCheckBoxes.entrySet()) {
            if (!pvEntry.getValue().isSelected()) {
                continue;
            }
            selected++;
            final BlockDevice opv = oPVS.get(pvEntry.getKey());
            if (opv == null) {
                return false;
            }
            if (!opv.isPhysicalVolume() || opv.isVolumeGroupOnPhysicalVolume()) {
                return false;
            }
        }
        return selected > 0;
    }

    @Override
    protected JComponent getInputPane() {
        createButton.setEnabled(false);
        final JPanel pane = new JPanel(new SpringLayout());
        /* vg name */
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setBackground(Browser.BUTTON_PANEL_BACKGROUND);

        /* find next free group volume name */
        String defaultName;
        final Set<String> volumeGroups = host.getVolumeGroupNames();
        int i = 0;
        while (true) {
            defaultName = "vg" + String.format("%02d", i);
            if (volumeGroups == null || !volumeGroups.contains(defaultName)) {
                break;
            }
            i++;
        }
        vgNameWi = widgetFactory.createInstance(
                                      Widget.Type.TEXTFIELD,
                                      new StringValue(defaultName),
                                      Widget.NO_ITEMS,
                                      Widget.NO_REGEXP,
                                      250,
                                      Widget.NO_ABBRV,
                                      new AccessMode(Application.AccessType.OP, !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);
        inputPane.add(new JLabel("VG Name"));
        inputPane.add(vgNameWi.getComponent());

        createButton.addActionListener(new CreateActionListener());
        inputPane.add(createButton);
        SpringUtilities.makeCompactGrid(inputPane, 1, 3,  /* rows, cols */
                                                   1, 1,  /* initX, initY */
                                                   1, 1); /* xPad, yPad */

        pane.add(inputPane);
        /* Volume groups. */
        final JPanel pvsPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        final Collection<String> selectedPVs = new HashSet<String>();
        final Collection<Host> selectedHosts = new HashSet<Host>();
        for (final BlockDevInfo sbdi : selectedBlockDevInfos) {
            if (sbdi.getBlockDevice().isDrbd()) {
                selectedPVs.add(sbdi.getBlockDevice().getDrbdBlockDevice().getName());
            } else {
                selectedPVs.add(sbdi.getName());
            }
            selectedHosts.add(sbdi.getHost());
        }
        pvCheckBoxes = getPVCheckBoxes(selectedPVs);
        pvsPane.add(new JLabel("Select physical volumes: "));
        for (final Map.Entry<String, JCheckBox> pvEntry : pvCheckBoxes.entrySet()) {
            pvEntry.getValue().addItemListener(new ItemChangeListener(true));
            pvsPane.add(pvEntry.getValue());
        }
        final JScrollPane pvSP = new JScrollPane(pvsPane);
        pvSP.setPreferredSize(new Dimension(0, 45));
        pane.add(pvSP);

        final JPanel hostsPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
        final Cluster cluster = host.getCluster();
        hostCheckBoxes = Tools.getHostCheckBoxes(cluster);
        hostsPane.add(new JLabel("Select Hosts: "));
        for (final Map.Entry<Host, JCheckBox> hostEntry : hostCheckBoxes.entrySet()) {
            hostEntry.getValue().addItemListener(new ItemChangeListener(true));
            if (host == hostEntry.getKey()) {
                hostEntry.getValue().setEnabled(false);
                hostEntry.getValue().setSelected(true);
            } else if (isOneDrbd(selectedBlockDevInfos)) {
                hostEntry.getValue().setEnabled(false);
                hostEntry.getValue().setSelected(false);
            } else if (hostHasPVSWithoutVGs(hostEntry.getKey())) {
                hostEntry.getValue().setEnabled(true);
                hostEntry.getValue().setSelected(selectedHosts.contains(hostEntry.getKey()));
            } else {
                hostEntry.getValue().setEnabled(false);
                hostEntry.getValue().setSelected(false);
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
        checkButtons();
        return pane;
    }

    private boolean vgCreate(final Host host, final String vgName, final Collection<String> pvNames) {
        for (final String pv : pvNames) {
            final BlockDevInfo bdi = host.getBrowser().getDrbdGraph().findBlockDevInfo(host.getName(), pv);
            if (bdi != null) {
                bdi.getBlockDevice().setVolumeGroupOnPhysicalVolume(vgName);
                bdi.getBrowser().getDrbdGraph().startAnimation(bdi);
            }
        }
        final boolean ret = LVM.vgCreate(host, vgName, pvNames, Application.RunMode.LIVE);
        if (ret) {
            answerPaneAddText("Volume group " + vgName + " was successfully created " + " on " + host.getName() + '.');
        } else {
            answerPaneAddTextError("Creating of volume group " + vgName + " failed.");
        }
        return ret;
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
                checkButtons();
            }
        }
    }

    /** Create action listener. */
    private class CreateActionListener implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            final Thread thread = new Thread(new CreateRunnable());
            thread.start();
        }
    }

    private class CreateRunnable implements Runnable {
        @Override
        public void run() {
            application.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    enableCreateButton(false);
                }
            });
                    
            disableComponents();
            getProgressBar().start(CREATE_TIMEOUT * hostCheckBoxes.size());
            boolean oneFailed = false;
            for (final Map.Entry<Host, JCheckBox> hostEntry : hostCheckBoxes.entrySet()) {
                if (hostEntry.getValue().isSelected()) {
                    final Collection<String> pvNames = new ArrayList<String>();
                    for (final Map.Entry<String, JCheckBox> pvEntry : pvCheckBoxes.entrySet()) {
                        if (pvEntry.getValue().isSelected()) {
                            pvNames.add(pvEntry.getKey());
                        }
                    }
                    final boolean ret = vgCreate(hostEntry.getKey(), vgNameWi.getStringValue(), pvNames);
                    if (!ret) {
                        oneFailed = true;
                    }
                }
            }
            enableComponents();
            if (oneFailed) {
                for (final Host h : hostCheckBoxes.keySet()) {
                    h.getBrowser().getClusterBrowser().updateHWInfo(h, Host.UPDATE_LVM);
                }
                application.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        checkButtons();
                    }
                });
                progressBarDoneError();
            } else {
                progressBarDone();
                disposeDialog();
                for (final Host h : hostCheckBoxes.keySet()) {
                    h.getBrowser().getClusterBrowser().updateHWInfo(h, Host.UPDATE_LVM);
                }
            }
        }
    }
}
