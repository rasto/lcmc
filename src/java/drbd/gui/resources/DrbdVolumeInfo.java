/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
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
package drbd.gui.resources;

import drbd.gui.Browser;
import drbd.gui.ClusterBrowser;
import drbd.utilities.Tools;

import javax.swing.JComponent;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import drbd.utilities.UpdatableItem;
import drbd.utilities.MyMenuItem;
import drbd.utilities.DRBD;
import drbd.data.AccessMode;
import drbd.data.ConfigData;
import drbd.data.resources.DrbdVolume;
import drbd.data.Host;
import drbd.gui.dialog.cluster.DrbdLogs;
import drbd.Exceptions;
import drbd.AddDrbdSplitBrainDialog;
import drbd.gui.HeartbeatGraph;
import drbd.data.DRBDtestData;
import drbd.utilities.ButtonCallback;

import java.awt.geom.Point2D;
import java.util.Map;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashMap;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.concurrent.CountDownLatch;
import java.util.LinkedHashMap;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;


/**
 * This class holds info data of a DRBD volume.
 */
public final class DrbdVolumeInfo extends EditableInfo
                                  implements CommonDeviceInterface {
    /** Drbd resource in which is this volume defined. */
    private final DrbdResourceInfo drbdResourceInfo;
    /** Block devices that are in this DRBD volume. */
    private final List<BlockDevInfo> blockDevInfos;
    /** Device name. TODO: */
    final String device;
    /** Last created filesystem. */
    private String createdFs = null;
    /**
     * Whether the block device is used by heartbeat via Filesystem service.
     */
    private ServiceInfo isUsedByCRM;
    /** Cache for getInfoPanel method. */
    private JComponent infoPanel = null;
    /** Name of the drbd device parameter. */
    static final String DRBD_VOL_PARAM_DEV = "device";
    /** Name of the drbd volume number parameter. */
    static final String DRBD_VOL_PARAM_NUMBER = "number";
    /** String that is displayed as a tool tip if a menu item is used by CRM. */
    static final String IS_USED_BY_CRM_STRING = "it is used by cluster manager";
    /** String that is displayed as a tool tip for disabled menu item. */
    static final String IS_SYNCING_STRING = "it is being full-synced";
    /** String that is displayed as a tool tip for disabled menu item. */
    static final String IS_VERIFYING_STRING = "it is being verified";
    /** Parameters. */
    static final String[] PARAMS = {DRBD_VOL_PARAM_NUMBER, DRBD_VOL_PARAM_DEV};
    /** Section name */
    static final String SECTION_STRING =
                               Tools.getString("DrbdVolumeInfo.VolumeSection");
    /** Long descriptions for parameters. */
    private static final Map<String, String> LONG_DESC =
                Collections.unmodifiableMap(new HashMap<String, String>() {
                    private static final long serialVersionUID = 1L;
                {
                    put(DRBD_VOL_PARAM_DEV, "DRBD device");
                    put(DRBD_VOL_PARAM_NUMBER, "DRBD Volume number");
                }});
    /** Short descriptions for parameters. */
    private static final Map<String, String> SHORT_DESC =
            Collections.unmodifiableMap(new HashMap<String, String>() {
                private static final long serialVersionUID = 1L;
            {
                put(DRBD_VOL_PARAM_DEV,
                    Tools.getString("DrbdVolumeInfo.Device"));
                put(DRBD_VOL_PARAM_NUMBER,
                    Tools.getString("DrbdVolumeInfo.Number"));
            }});

    /** Short descriptions for parameters. */
    private static final Map<String, Object[]> POSSIBLE_CHOICES =
                Collections.unmodifiableMap(new HashMap<String, Object[]>() {
                    private static final long serialVersionUID = 1L;
                {
                    put(DRBD_VOL_PARAM_DEV, new String[]{"/dev/drbd0",
                                                         "/dev/drbd1",
                                                         "/dev/drbd2",
                                                         "/dev/drbd3",
                                                         "/dev/drbd4",
                                                         "/dev/drbd5",
                                                         "/dev/drbd6",
                                                         "/dev/drbd7",
                                                         "/dev/drbd8",
                                                         "/dev/drbd9"});
                    put(DRBD_VOL_PARAM_NUMBER,
                        new String[]{"0", "1", "2", "3", "4", "5"});
                }});

    /** Prepares a new <code>DrbdVolumeInfo</code> object. */
    DrbdVolumeInfo(final String name,
                   final String device,
                   final DrbdResourceInfo drbdResourceInfo,
                   final List<BlockDevInfo> blockDevInfos,
                   final Browser browser) {
        super(name, browser);
        assert(drbdResourceInfo != null);
        assert(blockDevInfos.size() >= 2);

        this.drbdResourceInfo = drbdResourceInfo;
        this.blockDevInfos = Collections.unmodifiableList(blockDevInfos);
        this.device = device;
        setResource(new DrbdVolume(name));
        getResource().setValue(DRBD_VOL_PARAM_DEV, device);
    }

    /** Returns info panel. */
    @Override public JComponent getInfoPanel() {
        getBrowser().getDrbdGraph().pickInfo(this);
        if (infoPanel != null) {
            return infoPanel;
        }
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;
            /**
             * Whether the whole thing should be enabled.
             */
            @Override public boolean isEnabled() {
                return true;
            }

            @Override public void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                getBrowser().getDrbdGraph().stopTestAnimation(getApplyButton());
                getApplyButton().setToolTipText(null);
            }

            @Override public void mouseOver() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                getApplyButton().setToolTipText(
                       Tools.getString("ClusterBrowser.StartingDRBDtest"));
                getApplyButton().setToolTipBackground(Tools.getDefaultColor(
                                "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                getBrowser().getDrbdGraph().startTestAnimation(getApplyButton(),
                                                               startTestLatch);
                getBrowser().drbdtestLockAcquire();
                getBrowser().setDRBDtestData(null);
                final Map<Host, String> testOutput =
                                            new LinkedHashMap<Host, String>();
                try {
                    getBrowser().getDrbdGraph().getDrbdInfo().createDrbdConfig(true);
                    for (final Host h : getDrbdResourceInfo().getCluster().getHostsArray()) {
                        DRBD.adjust(h, "all", null, true);
                        testOutput.put(h, DRBD.getDRBDtest());
                    }
                } catch (Exceptions.DrbdConfigException dce) {
                    getBrowser().drbdtestLockRelease();
                    return;
                }
                final DRBDtestData dtd = new DRBDtestData(testOutput);
                getApplyButton().setToolTipText(dtd.getToolTip());
                getBrowser().setDRBDtestData(dtd);
                getBrowser().drbdtestLockRelease();
                startTestLatch.countDown();
            }
        };
        initApplyButton(buttonCallback,
                        Tools.getString("Browser.ApplyDRBDResource"));

        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        mainPanel.add(buttonPanel);

        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        final JMenu serviceCombo = getActionsMenu();
        mb.add(serviceCombo);
        buttonPanel.add(mb, BorderLayout.EAST);

        /* resource name */
        getResource().setValue(DRBD_VOL_PARAM_NUMBER,
                               getResource().getName());
        getResource().setValue(DRBD_VOL_PARAM_DEV, getDevice());

        final String[] params = getParametersFromXML();
        addParams(optionsPanel,
                  params,
                  Tools.getDefaultInt("ClusterBrowser.DrbdResLabelWidth"),
                  Tools.getDefaultInt("ClusterBrowser.DrbdResFieldWidth"),
                  null);

        getApplyButton().addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new Runnable() {
                    @Override public void run() {
                        Tools.invokeAndWait(new Runnable() {
                            @Override public void run() {
                                getApplyButton().setEnabled(false);
                                getRevertButton().setEnabled(false);
                            }
                        });
                        getBrowser().drbdStatusLock();
                        try {
                            getBrowser().getDrbdGraph().getDrbdInfo().createDrbdConfig(false);
                            for (final Host h : getDrbdResourceInfo().getCluster().getHostsArray()) {
                                DRBD.adjust(h, "all", null, false);
                            }
                        } catch (Exceptions.DrbdConfigException dce) {
                            getBrowser().drbdStatusUnlock();
                            Tools.appError("config failed");
                            return;
                        }
                        apply(false);
                        getBrowser().drbdStatusUnlock();
                    }
                });
                thread.start();
            }
        });

        getRevertButton().addActionListener(
            new ActionListener() {
                @Override public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override public void run() {
                            getBrowser().drbdStatusLock();
                            revert();
                            getBrowser().drbdStatusUnlock();
                        }
                    });
                    thread.start();
                }
            }
        );


        addApplyButton(buttonPanel);
        addRevertButton(buttonPanel);

        mainPanel.add(optionsPanel);

        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(
              Tools.getDefaultInt("ClusterBrowser.DrbdResLabelWidth")
              + Tools.getDefaultInt("ClusterBrowser.DrbdResFieldWidth") + 4));
        newPanel.add(new JScrollPane(mainPanel));
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    /** Return the first block devices. */
    public BlockDevInfo getFirstBlockDevInfo() {
        if (blockDevInfos.size() > 0) {
            return blockDevInfos.get(0);
        } else {
            return null;
        }
    }

    /** Return the second block devices. */
    public BlockDevInfo getSecondBlockDevInfo() {
        if (blockDevInfos.size() > 1) {
            return blockDevInfos.get(1);
        } else {
            return null;
        }
    }

    /** Return all block devices of this volume. */
    public List<BlockDevInfo> getBlockDevInfos() {
        return blockDevInfos;
    }

    /** Returns true if this is first block dev info. */
    public boolean isFirstBlockDevInfo(final BlockDevInfo bdi) {
        return getFirstBlockDevInfo() == bdi;
    }

    /** Returns the list of items for the popup menu for drbd volume. */
    @Override public List<UpdatableItem> createPopup() {
        final boolean testOnly = false;
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final DrbdVolumeInfo thisClass = this;

        final MyMenuItem connectMenu = new MyMenuItem(
            Tools.getString("ClusterBrowser.Drbd.ResourceConnect"),
            null,
            Tools.getString("ClusterBrowser.Drbd.ResourceConnect.ToolTip"),

            Tools.getString("ClusterBrowser.Drbd.ResourceDisconnect"),
            null,
            Tools.getString("ClusterBrowser.Drbd.ResourceDisconnect.ToolTip"),
            new AccessMode(ConfigData.AccessType.OP, true),
            new AccessMode(ConfigData.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override public boolean predicate() {
                return !isConnected(testOnly);
            }

            @Override public String enablePredicate() {
                if (!Tools.getConfigData().isAdvancedMode() && isUsedByCRM()) {
                    return IS_USED_BY_CRM_STRING;
                }
                if (isSyncing()) {
                    return IS_SYNCING_STRING;
                }
                return null;
            }

            @Override public void action() {
                BlockDevInfo sourceBDI =
                              getBrowser().getDrbdGraph().getSource(thisClass);
                BlockDevInfo destBDI =
                                getBrowser().getDrbdGraph().getDest(thisClass);
                if (this.getText().equals(Tools.getString(
                                    "ClusterBrowser.Drbd.ResourceConnect"))) {
                    if (!destBDI.isConnectedOrWF(testOnly)) {
                        destBDI.connect(testOnly);
                    }
                    if (!sourceBDI.isConnectedOrWF(testOnly)) {
                        sourceBDI.connect(testOnly);
                    }
                } else {
                    destBDI.disconnect(testOnly);
                    sourceBDI.disconnect(testOnly);
                }
            }
        };
        final ClusterBrowser.DRBDMenuItemCallback connectItemCallback =
               getBrowser().new DRBDMenuItemCallback(connectMenu, null) {
            @Override public void action(final Host host) {
                final BlockDevInfo sourceBDI =
                              getBrowser().getDrbdGraph().getSource(thisClass);
                final BlockDevInfo destBDI =
                                getBrowser().getDrbdGraph().getDest(thisClass);
                BlockDevInfo bdi;
                if (sourceBDI.getHost() == host) {
                    bdi = sourceBDI;
                } else if (destBDI.getHost() == host) {
                    bdi = destBDI;
                } else {
                    return;
                }
                if (sourceBDI.isConnected(false)
                    && destBDI.isConnected(false)) {
                    bdi.disconnect(true);
                } else {
                    bdi.connect(true);
                }
            }
        };
        addMouseOverListener(connectMenu, connectItemCallback);
        items.add(connectMenu);

        final MyMenuItem resumeSync = new MyMenuItem(
           Tools.getString("ClusterBrowser.Drbd.ResourceResumeSync"),
           null,
           Tools.getString("ClusterBrowser.Drbd.ResourceResumeSync.ToolTip"),

           Tools.getString("ClusterBrowser.Drbd.ResourcePauseSync"),
           null,
           Tools.getString("ClusterBrowser.Drbd.ResourcePauseSync.ToolTip"),
           new AccessMode(ConfigData.AccessType.OP, false),
           new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override public boolean predicate() {
                return isPausedSync();
            }

            @Override public String enablePredicate() {
                if (!isSyncing()) {
                    return "it is not syncing";
                }
                return null;
            }
            @Override public void action() {
                BlockDevInfo sourceBDI =
                              getBrowser().getDrbdGraph().getSource(thisClass);
                BlockDevInfo destBDI =
                                getBrowser().getDrbdGraph().getDest(thisClass);
                if (this.getText().equals(Tools.getString(
                            "ClusterBrowser.Drbd.ResourceResumeSync"))) {
                    if (destBDI.getBlockDevice().isPausedSync()) {
                        destBDI.resumeSync(testOnly);
                    }
                    if (sourceBDI.getBlockDevice().isPausedSync()) {
                        sourceBDI.resumeSync(testOnly);
                    }
                } else {
                    sourceBDI.pauseSync(testOnly);
                    destBDI.pauseSync(testOnly);
                }
            }
        };
        items.add(resumeSync);

        /* resolve split-brain */
        final MyMenuItem splitBrainMenu = new MyMenuItem(
                Tools.getString("ClusterBrowser.Drbd.ResolveSplitBrain"),
                null,
                Tools.getString(
                            "ClusterBrowser.Drbd.ResolveSplitBrain.ToolTip"),
                new AccessMode(ConfigData.AccessType.OP, false),
                new AccessMode(ConfigData.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override public String enablePredicate() {
                if (isSplitBrain()) {
                    return null;
                } else {
                    return "";
                }
            }

            @Override public void action() {
                resolveSplitBrain();
            }
        };
        items.add(splitBrainMenu);

        /* start online verification */
        final MyMenuItem verifyMenu = new MyMenuItem(
                Tools.getString("ClusterBrowser.Drbd.Verify"),
                null,
                Tools.getString("ClusterBrowser.Drbd.Verify.ToolTip"),
                new AccessMode(ConfigData.AccessType.OP, false),
                new AccessMode(ConfigData.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override public String enablePredicate() {
                if (!isConnected(testOnly)) {
                    return "not connected";
                }
                if (isSyncing()) {
                    return IS_SYNCING_STRING;
                }
                if (isVerifying()) {
                    return IS_VERIFYING_STRING;
                }
                return null;
            }

            @Override public void action() {
                verify(testOnly);
            }
        };
        items.add(verifyMenu);
        /* remove resource */
        final MyMenuItem removeResMenu = new MyMenuItem(
                        Tools.getString("ClusterBrowser.Drbd.RemoveEdge"),
                        ClusterBrowser.REMOVE_ICON,
                        Tools.getString(
                                "ClusterBrowser.Drbd.RemoveEdge.ToolTip"),
                        new AccessMode(ConfigData.AccessType.ADMIN, false),
                        new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override public void action() {
                /* this drbdResourceInfo remove myself and this calls
                   removeDrbdResource in this class, that removes the edge
                   in the graph. */
                removeMyself(testOnly);
            }

            @Override public String enablePredicate() {
                if (!Tools.getConfigData().isAdvancedMode() && isUsedByCRM()) {
                    return IS_USED_BY_CRM_STRING;
                }
                return null;
            }
        };
        items.add(removeResMenu);

        /* view log */
        final MyMenuItem viewLogMenu = new MyMenuItem(
                           Tools.getString("ClusterBrowser.Drbd.ViewLogs"),
                           LOGFILE_ICON,
                           null,
                           new AccessMode(ConfigData.AccessType.RO, false),
                           new AccessMode(ConfigData.AccessType.RO, false)) {

            private static final long serialVersionUID = 1L;

            @Override public String enablePredicate() {
                return null;
            }

            @Override public void action() {
                hidePopup();
                final String device = getDevice();
                DrbdLogs l = new DrbdLogs(getDrbdResourceInfo().getCluster(), device);
                l.showDialog();
            }
        };
        items.add(viewLogMenu);
        return items;
    }

    /** Returns tool tip when mouse is over the volume edge. */
    @Override public String getToolTipForGraph(final boolean testOnly) {
        final StringBuilder s = new StringBuilder(50);
        s.append("<html><b>Resource: ");
        s.append(getDrbdResourceInfo().getName());
        s.append("volume: ");
        s.append(getName());
        s.append("</b><br>");
        if (isSyncing()) {
            final String spString = getSyncedProgress();
            final String bsString = getFirstBlockDevInfo()
                                            .getBlockDevice().getBlockSize();
            final String rateString = getResource().getValue("rate");
            if (spString != null && bsString != null && rateString != null) {
                final double sp = Double.parseDouble(spString);
                final double bs = Double.parseDouble(bsString);
                final Object[] rateObj = Tools.extractUnit(rateString);
                double rate = Double.parseDouble((String) rateObj[0]);
                if ("k".equalsIgnoreCase((String) rateObj[1])) {
                } else if ("m".equalsIgnoreCase((String) rateObj[1])) {
                    rate *= 1024;
                } else if ("g".equalsIgnoreCase((String) rateObj[1])) {
                    rate *= 1024 * 1024;
                } else if ("".equalsIgnoreCase((String) rateObj[1])) {
                    rate /= 1024;
                } else {
                    rate = 0;
                }
                if (rate > 0) {
                    s.append("\nremaining at least: ");
                    final double seconds = ((100 - sp) / 100 * bs) / rate;
                    if (seconds < 60 * 5) {
                        s.append((int) seconds);
                        s.append(" Seconds");
                    } else {
                        s.append((int) (seconds / 60));
                        s.append(" Minutes");
                    }
                }
            }
        }
        s.append("</html>");
        return s.toString();
    }

    /**
     * Removes this object from jtree and from list of drbd volume
     * infos without confirmation dialog.
     */
    void removeMyselfNoConfirm(final boolean testOnly) {
        getBrowser().drbdStatusLock();
        getBrowser().getDrbdXML().removeResource(getName());
        getBrowser().getDrbdGraph().removeDrbdVolume(this);
        final Host[] hosts = getDrbdResourceInfo().getCluster().getHostsArray();
        for (final Host host : hosts) {
            DRBD.down(host,
                      getDrbdResourceInfo().getName(),
                      getName(),
                      testOnly);
        }
        super.removeMyself(testOnly);
        //final Map<String, DrbdResourceInfo> drbdResHash =
        //                                        getBrowser().getDrbdResHash();
        //final DrbdResourceInfo dri = drbdResHash.get(getName());
        //drbdResHash.remove(getName());
        //getBrowser().putDrbdResHash();
        //dri.setName(null);
        getBrowser().reload(getBrowser().getDrbdNode(), true);
        getBrowser().getDrbdDevHash().remove(getDevice());
        getBrowser().putDrbdDevHash();
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            bdi.removeFromDrbd();
            bdi.removeMyself(testOnly);
        }
        getBrowser().updateCommonBlockDevices();

        try {
            getBrowser().getDrbdGraph().getDrbdInfo().createDrbdConfig(
                                                                     testOnly);
        } catch (Exceptions.DrbdConfigException dce) {
            getBrowser().drbdStatusUnlock();
            Tools.appError("config failed", dce);
            return;
        }
        getBrowser().getDrbdGraph().getDrbdInfo().setSelectedNode(null);
        getBrowser().getDrbdGraph().getDrbdInfo().selectMyself();
        getBrowser().getDrbdGraph().updatePopupMenus();
        getBrowser().resetFilesystems();
        getBrowser().drbdStatusUnlock();
    }

    /** Removes this drbd resource with confirmation dialog. */
    @Override public void removeMyself(final boolean testOnly) {
        String desc = Tools.getString(
                       "ClusterBrowser.confirmRemoveDrbdResource.Description");
        desc = desc.replaceAll("@RESOURCE@", getName());
        if (Tools.confirmDialog(
              Tools.getString("ClusterBrowser.confirmRemoveDrbdResource.Title"),
              desc,
              Tools.getString("ClusterBrowser.confirmRemoveDrbdResource.Yes"),
              Tools.getString("ClusterBrowser.confirmRemoveDrbdResource.No"))) {
            removeMyselfNoConfirm(testOnly);
        }
    }

    /** Returns drbd resource info object. */
    public DrbdResourceInfo getDrbdResourceInfo() {
        return drbdResourceInfo;
    }

    /** Returns device name, like /dev/drbd0. */
    @Override public String getDevice() {
        // TODO: make it part of block device
        return device;
    }

    /** Sets that this drbd resource is used by hb. */
    @Override public void setUsedByCRM(final ServiceInfo isUsedByCRM) {
        this.isUsedByCRM = isUsedByCRM;
    }

    /** Returns whether this drbd resource is used by crm. */
    @Override public boolean isUsedByCRM() {
        return isUsedByCRM != null && isUsedByCRM.isManaged(false);
    }

    /** Returns the last created filesystem. */
    @Override public String getCreatedFs() {
        return createdFs;
    }

    /** Sets the last created filesystem. */
    public void setCreatedFs(final String createdFs) {
        this.createdFs = createdFs;
    }

    /** Returns sync progress in percent. */
    public String getSyncedProgress() {
        return getFirstBlockDevInfo().getBlockDevice().getSyncedProgress();
    }

    /** Returns whether the cluster is syncing. */
    public boolean isSyncing() {
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            if (bdi.getBlockDevice().isSyncing()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether any of the sides in the drbd resource are in
     * split-brain.
     */
    public boolean isSplitBrain() {
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            if (bdi.getBlockDevice().isSplitBrain()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the resources is connected, meaning both devices are
     * connected.
     */
    public boolean isConnected(final boolean testOnly) {
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            if (bdi.isConnected(testOnly)) {
                return true;
            }
        }
        return false;
    }

    /** Returns whether the cluster is being verified. */
    public boolean isVerifying() {
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            if (bdi.getBlockDevice().isVerifying()) {
                return true;
            }
        }
        return false;
    }

    /** Returns other block device in the drbd cluster. */
    public BlockDevInfo getOtherBlockDevInfo(final BlockDevInfo thisBDI) {
        if (thisBDI.equals(getFirstBlockDevInfo())) {
            return getSecondBlockDevInfo();
        }
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            if (bdi == getFirstBlockDevInfo()) {
                /* skip first */
                continue;
            }
            if (thisBDI.equals(bdi)) {
                return getFirstBlockDevInfo();
            }

        }
        return null;
    }

    /** Returns how much diskspace is used on the primary. */
    @Override public int getUsed() {
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            if (bdi.getBlockDevice().isPrimary()) {
                return bdi.getBlockDevice().getUsed();
            }
        }
        return -1;
    }

    /**
     * Returns whether any of the sides in the drbd resource are in
     * paused-sync state.
     */
    boolean isPausedSync() {
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            if (bdi.getBlockDevice().isPausedSync()) {
                return true;
            }
        }
        return false;
    }

    /** Starts online verification. */
    void verify(final boolean testOnly) {
        getFirstBlockDevInfo().verify(testOnly);
    }

    /** Remove drbddisk heartbeat service. */
    void removeDrbdDisk(final FilesystemInfo fi,
                        final Host dcHost,
                        final boolean testOnly) {
        final DrbddiskInfo drbddiskInfo = fi.getDrbddiskInfo();
        if (drbddiskInfo != null) {
            drbddiskInfo.removeMyselfNoConfirm(dcHost, testOnly);
        }
    }

    /** Remove drbddisk heartbeat service. */
    void removeLinbitDrbd(final FilesystemInfo fi,
                          final Host dcHost,
                          final boolean testOnly) {
        final LinbitDrbdInfo linbitDrbdInfo = fi.getLinbitDrbdInfo();
        if (linbitDrbdInfo != null) {
            linbitDrbdInfo.removeMyselfNoConfirm(dcHost, testOnly);
        }
    }

    /** Adds old style drbddisk service in the heartbeat and graph. */
    void addDrbdDisk(final FilesystemInfo fi,
                            final Host dcHost,
                            final boolean testOnly) {
        final Point2D p = null;
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        final DrbddiskInfo di =
            (DrbddiskInfo) getBrowser().getServicesInfo().addServicePanel(
                                    getBrowser().getCRMXML().getHbDrbddisk(),
                                    p,
                                    true,
                                    null,
                                    null,
                                    testOnly);
        di.setGroupInfo(fi.getGroupInfo());
        getBrowser().addToHeartbeatIdList(di);
        fi.setDrbddiskInfo(di);
        final GroupInfo giFi = fi.getGroupInfo();
        if (giFi == null) {
            hg.addColocation(null, fi, di);
            hg.addOrder(null, di, fi);
        } else {
            hg.addColocation(null, giFi, di);
            hg.addOrder(null, di, giFi);
        }
        di.waitForInfoPanel();
        di.paramComboBoxGet("1", null).setValueAndWait(getName());
        di.apply(dcHost, testOnly);
    }

    /** Adds linbit::drbd service in the pacemaker graph. */
    void addLinbitDrbd(final FilesystemInfo fi,
                       final Host dcHost,
                       final boolean testOnly) {
        final Point2D p = null;
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        final LinbitDrbdInfo ldi =
         (LinbitDrbdInfo) getBrowser().getServicesInfo().addServicePanel(
                                     getBrowser().getCRMXML().getHbLinbitDrbd(),
                                     p,
                                     true,
                                     null,
                                     null,
                                     testOnly);
        Tools.waitForSwing();
        ldi.setGroupInfo(fi.getGroupInfo());
        getBrowser().addToHeartbeatIdList(ldi);
        fi.setLinbitDrbdInfo(ldi);
        /* it adds coloation only to the graph. */
        final CloneInfo ci = ldi.getCloneInfo();
        final GroupInfo giFi = fi.getGroupInfo();
        if (giFi == null) {
            hg.addColocation(null, fi, ci);
            hg.addOrder(null, ci, fi);
        } else {
            hg.addColocation(null, giFi, ci);
            hg.addOrder(null, ci, giFi);
        }
        /* this must be executed after the getInfoPanel is executed. */
        ldi.waitForInfoPanel();
        ldi.paramComboBoxGet("drbd_resource", null).setValueAndWait(getName());
        /* apply gets parents from graph and adds colocations. */
        Tools.waitForSwing();
        ldi.apply(dcHost, testOnly);
    }

    /** Starts resolve split brain dialog. */
    void resolveSplitBrain() {
        final AddDrbdSplitBrainDialog adrd = new AddDrbdSplitBrainDialog(this);
        adrd.showDialogs();
    }

    /** Connect block device from the specified host. */
    public void connect(final Host host, final boolean testOnly) {
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            if (bdi.getHost() == host
                && !bdi.isConnectedOrWF(false)) {
                bdi.connect(testOnly);
                break;
            }
        }
    }

    /** Returns both hosts of the drbd connection, sorted alphabeticaly. */
    public Set<Host> getHosts() {
        final TreeSet<Host> hosts = new TreeSet<Host>(new Comparator<Host>() {
            @Override public int compare(final Host h1, final Host h2) {
                return h1.getName().compareToIgnoreCase(h2.getName());
            }
        });
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            hosts.add(bdi.getHost());
        }
        return hosts;
    }

    /** Returns meta-disk device for the specified host. */
    public String getMetaDiskForHost(final Host host) {
        return getBrowser().getDrbdXML().getMetaDisk(host.getName(),
                                                     getDrbdResourceInfo().getName(),
                                                     getName());
    }

    /** Returns string of the drbd volume. */
    @Override public String toString() {
        String resName = getDrbdResourceInfo().getName();
        String name = getName();
        if (resName == null || name == null || "".equals(name)) {
            name = Tools.getString("ClusterBrowser.DrbdResUnconfigured");
        }
        return resName + "/" + name;
    }

    /** Returns drbd graphical view. */
    @Override public JPanel getGraphicalView() {
        return getBrowser().getDrbdGraph().getGraphPanel();
    }

    /** Returns all parameters. */
    @Override public String[] getParametersFromXML() {
        return PARAMS;
    }

    @Override public boolean checkResourceFieldsCorrect(final String param,
                                                        final String[] params) {
        return super.checkResourceFieldsCorrect(param, params);
    }

    @Override public boolean checkResourceFieldsChanged(final String param,
                                                        final String[] params) {
        return super.checkResourceFieldsChanged(param, params);
    }

    /** Applies changes that user made to the drbd volume fields. */
    public void apply(final boolean testOnly) {
        if (!testOnly) {
            final String[] params = getParametersFromXML();
            Tools.invokeAndWait(new Runnable() {
                @Override public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                }
            });
            getInfoPanel();
            waitForInfoPanel();
            getBrowser().getDrbdDevHash().remove(getDevice());
            getBrowser().putDrbdDevHash();
            storeComboBoxValues(params);

            final String drbdDevStr = getParamSaved(DRBD_VOL_PARAM_DEV);
            //getDrbdResource().setDevice(drbdDevStr);

            getBrowser().getDrbdDevHash().put(drbdDevStr, this);
            getBrowser().putDrbdDevHash();
            getBrowser().getDrbdGraph().repaint();
            //getDrbdResource().setCommited(true);
            //getDrbdInfo().setAllApplyButtons();
        }
    }

    /** Returns section to which this drbd parameter belongs. */
    @Override protected String getSection(final String param) {
        return SECTION_STRING;
    }

    @Override protected boolean isInteger(final String param) {
        if (DRBD_VOL_PARAM_NUMBER.equals(param)) {
            return true;
        }
        return false;
    }

    /** Returns browser object of this info. */
    @Override public final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }
    
    /**
     * Returns a long description of the parameter that is used for tool tip.
     */
    @Override protected final String getParamLongDesc(final String param) {
        return LONG_DESC.get(param);
    }

    /**
     * Returns the short description of the drbd parameter that is used as
     * a label.
     */
    @Override protected final String getParamShortDesc(final String param) {
        return SHORT_DESC.get(param);
    }

    /** Returns the preferred value for the drbd parameter. */
    @Override protected final String getParamPreferred(final String param) {
        return null;
    }

    /** Returns default value of the parameter. */
    @Override public String getParamDefault(final String param) {
        return null;
    }

    /**
     * Checks the new value of the parameter if it is conforms to its type
     * and other constraints.
     */
    @Override protected boolean checkParam(final String param,
                                       final String newValue) {
        return getBrowser().getDrbdXML().checkParam(param, newValue);
    }

    /** Returns the possible values for the pulldown menus, if applicable. */
    @Override protected final Object[] getParamPossibleChoices(
                                                        final String param) {
        return POSSIBLE_CHOICES.get(param);
    }

    /** Returns the type of the parameter (like boolean). */
    @Override protected final String getParamType(final String param) {
        return null;
    }

    /**
     * Returns whether the parameter is of the boolean type and needs the
     * checkbox.
     */
    @Override protected final boolean isCheckBox(final String param) {
        return false;
    }

    /** Returns whether this drbd parameter is of time type. */
    @Override protected final boolean isTimeType(final String param) {
        return false;
    }

    /** Returns whether this drbd parameter is of label type. */
    @Override protected final boolean isLabel(final String param) {
        return false;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override protected final boolean isEnabledOnlyInAdvancedMode(
                                                         final String param) {
        return false;
    }

    /** Returns access type of this parameter. */
    @Override protected final ConfigData.AccessType getAccessType(
                                                         final String param) {
        return ConfigData.AccessType.ADMIN;
    }

    /** Whether the parameter should be enabled. */
    @Override protected String isEnabled(final String param) {
        return null;
    }

    /** Returns whether this parameter is advanced. */
    @Override protected boolean isAdvanced(final String param) {
        return false;
    }

    /** Returns whether this drbd parameter is required parameter. */
    @Override protected final boolean isRequired(final String param) {
        return true;
    }

    /** Creates and returns drbd config for volumes. */
    String drbdVolumeConfig(final Host host, final boolean volumesAvailable)
                                        throws Exceptions.DrbdConfigException {
        final StringBuilder config = new StringBuilder(50);
        for (final BlockDevInfo bdi : blockDevInfos) {
            if (bdi.getHost() == host) {
                if (volumesAvailable) {
                    config.append("volume " + getName() + " {\n");
                }
                config.append(bdi.drbdBDConfig(getName(),
                              getDevice(),
                              volumesAvailable));
                config.append('\n');
                if (volumesAvailable) {
                    config.append("\t\t}");
                }
            }
        }
        return config.toString().trim();
    }
}
