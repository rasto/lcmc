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
package lcmc.drbd.ui.resource;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import lcmc.Exceptions;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.resource.CommonDeviceInterface;
import lcmc.cluster.ui.widget.Check;
import lcmc.cluster.ui.wizard.DrbdLogs;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.common.ui.EditableInfo;
import lcmc.common.ui.treemenu.ClusterTreeMenu;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.ui.utils.ComponentWithTest;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.crm.ui.CrmGraph;
import lcmc.crm.ui.resource.CloneInfo;
import lcmc.crm.ui.resource.DrbddiskInfo;
import lcmc.crm.ui.resource.FilesystemRaInfo;
import lcmc.crm.ui.resource.GroupInfo;
import lcmc.crm.ui.resource.LinbitDrbdInfo;
import lcmc.crm.ui.resource.ServiceInfo;
import lcmc.drbd.domain.DRBDtestData;
import lcmc.drbd.domain.DrbdVolume;
import lcmc.drbd.domain.DrbdXml;
import lcmc.drbd.service.DRBD;
import lcmc.drbd.ui.AddDrbdSplitBrainDialog;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * This class holds info data of a DRBD volume.
 */
@Named
public class VolumeInfo extends EditableInfo implements CommonDeviceInterface {
    private static final Logger LOG = LoggerFactory.getLogger(VolumeInfo.class);
    static final String DRBD_VOL_PARAM_DEV = "device";
    static final String DRBD_VOL_PARAM_NUMBER = "number";
    /**
     * String that is displayed as a tool tip if a menu item is used by CRM.
     */
    public static final String IS_USED_BY_CRM_STRING = "it is used by cluster manager";
    /**
     * String that is displayed as a tool tip for disabled menu item.
     */
    public static final String IS_SYNCING_STRING = "it is being full-synced";
    /**
     * String that is displayed as a tool tip for disabled menu item.
     */
    public static final String IS_VERIFYING_STRING = "it is being verified";
    static final String[] PARAMS = {DRBD_VOL_PARAM_NUMBER, DRBD_VOL_PARAM_DEV};
    static final String SECTION_STRING = Tools.getString("VolumeInfo.VolumeSection");
    /**
     * Long descriptions for parameters.
     */
    private static final Map<String, String> LONG_DESC = Collections.unmodifiableMap(new HashMap<>() {
        {
            put(DRBD_VOL_PARAM_DEV, "DRBD device");
            put(DRBD_VOL_PARAM_NUMBER, "DRBD Volume number");
        }
    });
    /**
     * Short descriptions for parameters.
     */
    private static final Map<String, String> SHORT_DESC = Collections.unmodifiableMap(new HashMap<>() {
        {
            put(DRBD_VOL_PARAM_DEV, Tools.getString("VolumeInfo.Device"));
            put(DRBD_VOL_PARAM_NUMBER, Tools.getString("VolumeInfo.Number"));
        }
    });

    /**
     * Short descriptions for parameters.
     */
    private static final Map<String, Value[]> POSSIBLE_CHOICES = Collections.unmodifiableMap(new HashMap<>() {
        {
            put(DRBD_VOL_PARAM_DEV,
                    new Value[]{new StringValue("/dev/drbd0"), new StringValue("/dev/drbd1"), new StringValue("/dev/drbd2"),
                            new StringValue("/dev/drbd3"), new StringValue("/dev/drbd4"), new StringValue("/dev/drbd5"),
                            new StringValue("/dev/drbd6"), new StringValue("/dev/drbd7"), new StringValue("/dev/drbd8"),
                            new StringValue("/dev/drbd9")});
            put(DRBD_VOL_PARAM_NUMBER,
                    new Value[]{new StringValue("0"), new StringValue("1"), new StringValue("2"), new StringValue("3"),
                            new StringValue("4"), new StringValue("5")});
        }
    });
    private static final String BY_RES_DEV_DIR = "/dev/drbd/by-res/";

    private ResourceInfo resourceInfo;
    private List<BlockDevInfo> blockDevInfos;
    /**
     * Device name. TODO:
     */
    private String device;
    /**
     * Last created filesystem.
     */
    private String createdFs = null;
    private JComponent infoPanel = null;
    private Set<Host> hosts;
    @Inject
    private AddDrbdSplitBrainDialog addDrbdSplitBrainDialog;
    @Inject
    private Provider<DrbdXml> drbdXmlProvider;
    @Inject
    private Application application;
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private VolumeMenu volumeMenu;
    @Inject
    private Provider<DrbdLogs> drbdLogsProvider;
    @Inject
    private ClusterTreeMenu clusterTreeMenu;

    void init(final String name, final String device, final ResourceInfo resourceInfo, final List<BlockDevInfo> blockDevInfos,
            final Browser browser) {
        super.einit(Optional.of(new DrbdVolume(name)), name, browser);
        assert resourceInfo != null;
        assert blockDevInfos.size() >= 2;

        this.resourceInfo = resourceInfo;
        this.blockDevInfos = Collections.unmodifiableList(blockDevInfos);
        this.device = device;
        hosts = getHostsFromBlockDevices(blockDevInfos);
        getResource().setValue(DRBD_VOL_PARAM_DEV, new StringValue(device));
        getResource().setNew(true);
    }

    /** Returns sorted hosts from the specified blockdevices. */
    public static Set<Host> getHostsFromBlockDevices(final Iterable<BlockDevInfo> bdis) {
        final Set<Host> hosts = new TreeSet<>((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        for (final BlockDevInfo bdi : bdis) {
            hosts.add(bdi.getHost());
        }
        return hosts;
    }

    @Override
    public JComponent getInfoPanel() {
        swingUtils.isSwingThread();
        getBrowser().getDrbdGraph().pickInfo(this);
        final JComponent driPanel = getDrbdResourceInfo().getInfoPanel();
        getInfoPanelVolume();
        infoPanelDone();
        return driPanel;
    }

    private JComponent getInfoPanelVolume() {
        getBrowser().getDrbdGraph().pickInfo(this);
        if (infoPanel != null) {
            return infoPanel;
        }
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public void mouseOut(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                getBrowser().getDrbdGraph().stopTestAnimation((JComponent) component);
                component.setToolTipText("");
            }

            @Override
            public void mouseOver(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                component.setToolTipText(Tools.getString("ClusterBrowser.StartingDRBDtest"));
                component.setToolTipBackground(Tools.getDefaultColor("ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                getBrowser().getDrbdGraph().startTestAnimation((JComponent) component, startTestLatch);
                getBrowser().drbdtestLockAcquire();
                getBrowser().setDRBDtestData(null);
                final Map<Host, String> testOutput = new LinkedHashMap<>();
                try {
                    getBrowser().getGlobalInfo().createConfigDryRun(testOutput);
                    final DRBDtestData dtd = new DRBDtestData(testOutput);
                    component.setToolTipText(dtd.getToolTip());
                    getBrowser().setDRBDtestData(dtd);
                } finally {
                    getBrowser().drbdtestLockRelease();
                    startTestLatch.countDown();
                }
            }
        };
        initApplyButton(buttonCallback, Tools.getString("Browser.ApplyDRBDResource"));

        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        mainPanel.add(buttonPanel);

        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.LINE_END);

        /* resource name */
        getResource().setValue(DRBD_VOL_PARAM_NUMBER, new StringValue(getResource().getName()));
        getResource().setValue(DRBD_VOL_PARAM_DEV, new StringValue(getDevice()));

        final String[] params = getParametersFromXML();
        addParams(optionsPanel, params, application.getDefaultSize("ClusterBrowser.DrbdResLabelWidth"),
                application.getDefaultSize("ClusterBrowser.DrbdResFieldWidth"), null);

        getApplyButton().addActionListener(e -> {
            LOG.debug1("getInfoPanelVolume: BUTTON: apply");
            final Thread thread = new Thread(() -> {
                swingUtils.invokeAndWait(() -> {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                });
                getBrowser().drbdStatusLock();
                try {
                    getBrowser().getGlobalInfo().createDrbdConfigLive();
                    for (final Host h : getHosts()) {
                        DRBD.adjustApply(h, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.LIVE);
                    }
                    apply(Application.RunMode.LIVE);
                } catch (final Exceptions.DrbdConfigException | UnknownHostException dce) {
                    LOG.appError("getInfoPanelVolume: config failed", dce);
                } finally {
                    getBrowser().drbdStatusUnlock();
                }
            });
            thread.start();
        });

        getRevertButton().addActionListener(e -> {
            LOG.debug1("getInfoPanelVolume: BUTTON: revert");
            final Thread thread = new Thread(() -> {
                getBrowser().drbdStatusLock();
                try {
                    revert();
                } finally {
                    getBrowser().drbdStatusUnlock();
                }
            });
            thread.start();
        });


        addApplyButton(buttonPanel);
        addRevertButton(buttonPanel);

        mainPanel.add(optionsPanel);

        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.PAGE_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(Tools.getDefaultInt("ClusterBrowser.DrbdResLabelWidth")
                                         + Tools.getDefaultInt("ClusterBrowser.DrbdResFieldWidth") + 4));
        newPanel.add(new JScrollPane(mainPanel));
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    /** Return the first block devices. */
    public BlockDevInfo getFirstBlockDevInfo() {
        if (blockDevInfos.isEmpty()) {
            return null;
        } else {
            return blockDevInfos.get(0);
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
    public Iterable<BlockDevInfo> getBlockDevInfos() {
        return blockDevInfos;
    }

    /** Returns true if this is first block dev info. */
    public boolean isFirstBlockDevInfo(final BlockDevInfo bdi) {
        return getFirstBlockDevInfo() == bdi;
    }
    
    /** Returns the list of items for the popup menu for drbd volume. */
    @Override
    public List<UpdatableItem> createPopup() {
        return volumeMenu.getPulldownMenu(this);
    }
    
    /** Returns tool tip when mouse is over the volume edge. */
    @Override
    public String getToolTipForGraph(final Application.RunMode runMode) {
        final StringBuilder s = new StringBuilder(50);
        s.append("<html><b>Resource: ");
        s.append(getDrbdResourceInfo().getName());
        s.append(" volume: ");
        s.append(getName());
        s.append("</b><br>");
        if (isSyncing()) {
            final String spString = getSyncedProgress();
            final String bsString = getFirstBlockDevInfo()
                .getBlockDevice().getBlockSize();
            final Value rateValue =
                getDrbdResourceInfo().getResource().getValue("rate");
            if (spString != null && bsString != null && rateValue != null) {
                final double sp = Double.parseDouble(spString);
                final double bs = Double.parseDouble(bsString);
                double rate = Double.parseDouble(rateValue.getValueForConfig());
                final String unit = rateValue.getUnit().getShortName();
                if ("k".equalsIgnoreCase(unit)) {
                } else if ("m".equalsIgnoreCase(unit)) {
                    rate *= 1024;
                } else if ("g".equalsIgnoreCase(unit)) {
                    rate *= 1024 * 1024;
                } else if ("".equalsIgnoreCase(unit)) {
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
    private void removeMyselfNoConfirm(final Application.RunMode runMode) {
        swingUtils.isNotSwingThread();
        final ClusterBrowser clusterBrowser = getBrowser();
        removeMyselfNoConfirm0(runMode, clusterBrowser);
    }

    /**
     * Removes this object from jtree and from list of drbd volume
     * infos without confirmation dialog.
     */
    private void removeMyselfNoConfirm0(final Application.RunMode runMode, final ClusterBrowser clusterBrowser) {
        clusterBrowser.drbdStatusLock();
        clusterBrowser.getDrbdXml().removeVolume(getDrbdResourceInfo().getName(), getDevice(), getName());
        clusterBrowser.getDrbdGraph().removeDrbdVolume(this);
        final Set<Host> hosts0 = getHosts();
        final boolean lastVolume = getDrbdResourceInfo().removeDrbdVolume(this);
        if (getDrbdVolume().isCommited()) {
            for (final Host host : hosts0) {
                DRBD.setSecondary(host, getDrbdResourceInfo().getName(), getName(), runMode);
                if (!host.hasVolumes()) {
                    DRBD.disconnect(host, getDrbdResourceInfo().getName(), null, runMode);
                }
                for (final BlockDevInfo bdi : getBlockDevInfos()) {
                    if (bdi.getHost() == host) {
                        if (bdi.getBlockDevice().isAttached()) {
                            DRBD.detach(host, getDrbdResourceInfo().getName(), getName(), runMode);
                        }
                        break;
                    }
                }
                if (host.hasVolumes()) {
                    DRBD.delMinor(host, getDevice(), runMode);
                    if (lastVolume) {
                        DRBD.delConnection(host, getDrbdResourceInfo().getName(), runMode);
                    }
                }
            }
        }
        super.removeMyself(runMode);
        clusterTreeMenu.reloadNode(clusterBrowser.getDrbdNode());

        clusterBrowser.getDrbdDeviceHash().remove(getDevice());
        clusterBrowser.putDrbdDevHash();
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            bdi.removeFromDrbd();
            bdi.removeMyself(runMode);
        }
        if (lastVolume) {
            final ResourceInfo dri = getDrbdResourceInfo();
            for (final BlockDevInfo bdi : getBlockDevInfos()) {
                if (dri.isProxy(bdi.getHost())) {
                    DRBD.proxyDown(bdi.getHost(), dri.getName(), null, runMode);
                }
            }
            dri.removeMyself(runMode);
        }

        try {
            getBrowser().getGlobalInfo().createDrbdConfig(runMode);
            getBrowser().getGlobalInfo().setSelectedNode(null);
            getBrowser().getGlobalInfo().selectMyself();
            clusterBrowser.resetFilesystems();

            final DrbdXml dxml = drbdXmlProvider.get();
            dxml.init(hosts0.toArray(new Host[0]), clusterBrowser.getHostDrbdParameters());
            for (final Host host : hosts0) {
                final String conf = dxml.getConfig(host);
                if (conf != null) {
                    dxml.update(conf);
                }
            }
            clusterBrowser.setDrbdXml(dxml);
            swingUtils.invokeInEdt(clusterBrowser::updateDrbdResources);
            if (Application.isLive(runMode)) {
                swingUtils.invokeInEdt(() -> {
                    clusterBrowser.getDrbdGraph().updatePopupMenus();
                    clusterTreeMenu.removeNode(getNode());
                    clusterBrowser.getDrbdGraph().scale();
                });
            }
        } catch (final Exceptions.DrbdConfigException | UnknownHostException dce) {
            LOG.appError("removeMyselfNoConfirm: config failed", dce);
        } finally {
            clusterBrowser.drbdStatusUnlock();
        }
    }

    /** Removes this drbd resource with confirmation dialog. */
    @Override
    public void removeMyself(final Application.RunMode runMode) {
        if (!getDrbdVolume().isCommited()) {
            removeMyselfNoConfirm(runMode);
            return;
        }
        String desc = Tools.getString("ClusterBrowser.confirmRemoveDrbdResource.Description");
        desc = desc.replaceAll("@RESOURCE@", Matcher.quoteReplacement(getDrbdResourceInfo() + "/" + getName()));
        if (application.confirmDialog(Tools.getString("ClusterBrowser.confirmRemoveDrbdResource.Title"),
                                desc,
                                Tools.getString("ClusterBrowser.confirmRemoveDrbdResource.Yes"),
                                Tools.getString("ClusterBrowser.confirmRemoveDrbdResource.No"))) {
            removeMyselfNoConfirm(runMode);
        }
    }

    /** Returns drbd resource info object. */
    public ResourceInfo getDrbdResourceInfo() {
        return resourceInfo;
    }

    /** Returns device name, like /dev/drbd0. */
    @Override
    public String getDevice() {
        return device;
    }

    /** Return DRBD device in /dev/drbd/by-res...form. */
    public String getDeviceByRes() {
        if (getBrowser().getGlobalInfo().atLeastVersion("8.4")) {
            return BY_RES_DEV_DIR + getDrbdResourceInfo().getName() + '/' + getName();
        } else {
            return BY_RES_DEV_DIR + getDrbdResourceInfo().getName();
        }
    }

    /** Returns the last created filesystem. */
    @Override
    public String getLastCreatedFs() {
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
     * Returns whether the resources are connected.
     * If all devices are waiting for connection it returns false
     */
    public boolean isConnected(final Application.RunMode runMode) {
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            if (bdi.isConnected(runMode)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Returns whether the resources are connected or waiting for connection.
     */
    public boolean isConnectedOrWF(final Application.RunMode runMode) {
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            if (!bdi.isConnectedOrWF(runMode)) {
                return false;
            }
        }
        return true;
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
    @Override
    public int getUsed() {
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
    void verify(final Application.RunMode runMode) {
        getFirstBlockDevInfo().verify(runMode);
    }

    public void removeDrbdDisk(final FilesystemRaInfo fi, final Host dcHost, final Application.RunMode runMode) {
        final DrbddiskInfo drbddiskInfo = fi.getDrbddiskInfo();
        if (drbddiskInfo != null) {
            drbddiskInfo.removeMyselfNoConfirm(dcHost, runMode);
        }
    }

    public void removeLinbitDrbd(final FilesystemRaInfo fi, final Host dcHost, final Application.RunMode runMode) {
        final LinbitDrbdInfo linbitDrbdInfo = fi.getLinbitDrbdInfo();
        if (linbitDrbdInfo != null) {
            linbitDrbdInfo.removeMyselfNoConfirm(dcHost, runMode);
        }
    }

    /** Adds old style drbddisk service in the heartbeat and graph. */
    public void addDrbdDisk(final FilesystemRaInfo fi, final Host dcHost, final String drbdId, final Application.RunMode runMode) {
        final Point2D p = null;
        final CrmGraph crmg = getBrowser().getCrmGraph();
        final DrbddiskInfo di =
            (DrbddiskInfo) getBrowser().getServicesInfo().addServicePanel(getBrowser().getCrmXml().getDrbddiskResourceAgent(),
                                                                          p,
                                                                          true,
                                                                          drbdId,
                                                                          null,
                                                                          runMode);
        di.setGroupInfo(fi.getGroupInfo());
        getBrowser().addToHeartbeatIdList(di);
        fi.setDrbddiskInfo(di);
        final GroupInfo giFi = fi.getGroupInfo();
        if (giFi == null) {
            crmg.addColocation(null, fi, di);
            crmg.addOrder(null, di, fi);
        } else {
            crmg.addColocation(null, giFi, di);
            crmg.addOrder(null, di, giFi);
        }
        di.waitForInfoPanel();
        di.getWidget("1", null).setValueAndWait(new StringValue(getDrbdResourceInfo().getName()));
        di.apply(dcHost, runMode);
        di.getResource().setNew(false);
        swingUtils.invokeInEdt(() -> di.setApplyButtons(null, di.getParametersFromXML()));
    }

    /** Adds linbit::drbd service in the pacemaker graph. */
    public void addLinbitDrbd(final FilesystemRaInfo fi,
                              final Host dcHost,
                              final String drbdId,
                              final Application.RunMode runMode) {
        final Point2D p = null;
        final CrmGraph crmg = getBrowser().getCrmGraph();
        final LinbitDrbdInfo ldi =
            (LinbitDrbdInfo) getBrowser().getServicesInfo().addServicePanel(
                getBrowser().getCrmXml().getLinbitDrbdResourceAgent(),
                                     p,
                                     true,
                                     drbdId,
                                     null,
                                     runMode);
        swingUtils.waitForSwing();
        ldi.setGroupInfo(fi.getGroupInfo());
        getBrowser().addToHeartbeatIdList(ldi);
        fi.setLinbitDrbdInfo(ldi);
        /* it adds coloation only to the graph. */
        final CloneInfo ci = ldi.getCloneInfo();
        final GroupInfo giFi = fi.getGroupInfo();
        if (giFi == null) {
            crmg.addColocation(null, fi, ci);
            crmg.addOrder(null, ci, fi);
        } else {
            crmg.addColocation(null, giFi, ci);
            crmg.addOrder(null, ci, giFi);
        }
        /* this must be executed after the getInfoPanel is executed. */
        ldi.waitForInfoPanel();
        ldi.getWidget("drbd_resource", null).setValueAndWait(new StringValue(getDrbdResourceInfo().getName()));
        /* apply gets parents from graph and adds colocations. */
        swingUtils.waitForSwing();
        ldi.apply(dcHost, runMode);
        ldi.getResource().setNew(false);
        swingUtils.invokeInEdt(() -> ldi.setApplyButtons(null, ldi.getParametersFromXML()));
    }

    /** Starts resolve split brain dialog. */
    void resolveSplitBrain() {
        addDrbdSplitBrainDialog.init(this);
        addDrbdSplitBrainDialog.showDialogs();
    }

    /** Connect block device from the specified host. */
    public void connect(final Host host, final Application.RunMode runMode) {
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            if (bdi.getHost() == host
                && !bdi.isConnectedOrWF(Application.RunMode.LIVE)) {
                bdi.connect(runMode);
                break;
            }
        }
    }

    /** Returns both hosts of the drbd connection, sorted alphabeticaly. */
    public Set<Host> getHosts() {
        return hosts;
    }

    /** Returns meta-disk device for the specified host. */
    public String getMetaDiskForHost(final Host host) {
        return getBrowser().getDrbdXml().getMetaDisk(host.getName(), getDrbdResourceInfo().getName(), getName());
    }

    /** Returns string of the drbd volume. */
    @Override
    public String toString() {
        final String resName = getDrbdResourceInfo().getName();
        final String name = getName();
        if (resName == null || name == null || name.isEmpty()) {
            return Tools.getString("ClusterBrowser.DrbdResUnconfigured");
        }
        return "DRBD: " + resName + '/' + name;
    }

    /** Returns drbd graphical view. */
    @Override
    public JPanel getGraphicalView() {
        return getBrowser().getDrbdGraph().getGraphPanel();
    }

    /** Returns all parameters. */
    @Override
    public String[] getParametersFromXML() {
        return PARAMS;
    }

    @Override
    public Check checkResourceFields(final String param, final String[] params) {
        return checkResourceFields(param, params, false, false);
    }

    /**
     * Returns whether all the parameters are correct. If param is null, all paremeters will be checked, otherwise only the param,
     * but other parameters will be checked only in the cache. This is good if only one value is changed and we don't want to check
     * everything.
     */
    Check checkResourceFields(final String param, final String[] params, final boolean fromDrbdInfo,
            final boolean fromDrbdResourceInfo) {
        final DrbdXml dxml = getBrowser().getDrbdXml();
        final GlobalInfo di = getBrowser().getGlobalInfo();
        if (di != null && !fromDrbdInfo && !fromDrbdResourceInfo) {
            di.setApplyButtons(null, di.getParametersFromXML());
        }
        final List<String> incorrect = new ArrayList<>();
        final List<String> changed = new ArrayList<>();
        if (dxml != null && dxml.isDrbdDisabled()) {
            incorrect.add("DRBD is disabled");
        }
        final Check check = new Check(incorrect, changed);
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            if (bdi != null) {
                check.addCheck(
                        bdi.checkResourceFields(param, bdi.getParametersFromXML(), fromDrbdInfo, fromDrbdResourceInfo, true));
            }
        }
        check.addCheck(super.checkResourceFields(param, params));
        return check;
    }

    /** Applies changes that user made to the drbd volume fields. */
    public void apply(final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            final String[] params = getParametersFromXML();
            getBrowser().getDrbdDeviceHash().remove(getDevice());
            getBrowser().putDrbdDevHash();
            storeComboBoxValues(params);

            final String volumeNr = getParamSaved(DRBD_VOL_PARAM_NUMBER).getValueForConfig();
            setName(volumeNr);
            final String drbdDevStr = getParamSaved(DRBD_VOL_PARAM_DEV).getValueForConfig();
            device = drbdDevStr;

            getBrowser().getDrbdDeviceHash().put(drbdDevStr, this);
            getBrowser().putDrbdDevHash();
            getBrowser().getDrbdGraph().repaint();
            getBrowser().getGlobalInfo().setAllApplyButtons();
            getResource().setNew(false);
        }
    }

    @Override
    protected String getSection(final String param) {
        return SECTION_STRING;
    }

    @Override
    protected boolean isInteger(final String param) {
        return DRBD_VOL_PARAM_NUMBER.equals(param);
    }

    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /**
     * Returns a long description of the parameter that is used for tool tip.
     */
    @Override
    protected String getParamLongDesc(final String param) {
        return LONG_DESC.get(param);
    }

    /**
     * Returns the short description of the drbd parameter that is used as
     * a label.
     */
    @Override
    protected String getParamShortDesc(final String param) {
        return SHORT_DESC.get(param);
    }

    @Override
    protected Value getParamPreferred(final String param) {
        return null;
    }

    @Override
    public Value getParamDefault(final String param) {
        return null;
    }

    /**
     * Checks the new value of the parameter if it is conforms to its type
     * and other constraints.
     */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        if (getResource().isNew() && DRBD_VOL_PARAM_DEV.equals(param)) {
            if (getBrowser().getDrbdDeviceHash().containsKey(newValue.getValueForConfig())) {
                getBrowser().putDrbdDevHash();
                return false;
            }
            getBrowser().putDrbdDevHash();
        }
        return getBrowser().getDrbdXml().checkParam(param, newValue);
    }

    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        return POSSIBLE_CHOICES.get(param);
    }

    @Override
    protected String getParamType(final String param) {
        return null;
    }

    @Override
    protected boolean isCheckBox(final String param) {
        return false;
    }

    @Override
    protected boolean isTimeType(final String param) {
        return false;
    }

    @Override
    protected boolean isLabel(final String param) {
        return false;
    }

    @Override
    protected AccessMode.Mode isEnabledOnlyInAdvancedMode(final String param) {
        return AccessMode.NORMAL;
    }

    @Override
    protected AccessMode.Type getAccessType(final String param) {
        return AccessMode.ADMIN;
    }

    @Override
    protected String isEnabled(final String param) {
        if (DRBD_VOL_PARAM_NUMBER.equals(param) && !getBrowser().getGlobalInfo().atLeastVersion("8.4")) {
            return "available in DRBD 8.4";
        }
        if (getDrbdVolume().isCommited()) {
            return ""; /* disabled */
        } else {
            return null;
        }
    }

    @Override
    protected boolean isAdvanced(final String param) {
        return false;
    }

    @Override
    protected boolean isRequired(final String param) {
        return true;
    }

    /** Creates and returns drbd config for volumes. */
    String drbdVolumeConfig(final Host host, final boolean volumesAvailable) throws Exceptions.DrbdConfigException {
        final StringBuilder config = new StringBuilder(50);
        for (final BlockDevInfo bdi : blockDevInfos) {
            if (bdi.getHost() == host) {
                if (volumesAvailable) {
                    config.append("volume ").append(getName()).append(" {\n");
                }
                config.append(bdi.drbdBDConfig(getName(), getDevice(), volumesAvailable));
                config.append('\n');
                if (volumesAvailable) {
                    config.append("\t\t}");
                }
            }
        }
        getDrbdVolume().setCommited(true);
        return config.toString().trim();
    }

    /** Set all apply buttons. */
    void setAllApplyButtons() {
        for (final BlockDevInfo bdi : getBlockDevInfos()) {
            if (bdi != null) {
                bdi.storeComboBoxValues(bdi.getParametersFromXML());
                bdi.setApplyButtons(null, bdi.getParametersFromXML());
            }
        }
        setApplyButtons(null, getParametersFromXML());
    }

    /** Returns name that is displayed in the graph. */
    public String getNameForGraph() {
        final StringBuilder n = new StringBuilder(20);
        final ResourceInfo dri = getDrbdResourceInfo();
        n.append(dri.getName());
        if (dri.getDrbdVolumes().size() > 1) {
            n.append('/');
            n.append(getName());
        }
        return n.toString();
    }

    /** Returns the DrbdVolume resource object of this drbd volume. */
    DrbdVolume getDrbdVolume() {
        return (DrbdVolume) getResource();
    }

    /**
     * Returns device name that is used as the string value of the device in
     * the filesystem resource.
     */
    @Override
    public String getInternalValue() {
        return getDeviceByRes();
    }

    /** Sets stored parameters. */
    public void setParameters() {
        swingUtils.isSwingThread();
        getBrowser().getDrbdDeviceHash().put(device, this);
        getBrowser().putDrbdDevHash();
        getDrbdVolume().setCommited(true);
        getResource().setNew(false);
    }

    @Override
    public void setUsedByCRM(final ServiceInfo isUsedByCRM) {
        getDrbdResourceInfo().setUsedByCRM(isUsedByCRM);
    }

    @Override
    public boolean isUsedByCRM() {
        return getDrbdResourceInfo().isUsedByCRM();
    }

    @Override
    public String getValueForConfig() {
        return getDeviceByRes();
    }

    public void startDrbdLogsDialog() {
        final DrbdLogs drbdLogs = drbdLogsProvider.get();
        drbdLogs.init(getDrbdResourceInfo().getCluster(), getDevice());
        drbdLogs.showDialog();
    }
}
