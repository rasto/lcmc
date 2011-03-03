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

import drbd.Exceptions;
import drbd.AddDrbdSplitBrainDialog;
import drbd.gui.Browser;
import drbd.gui.ClusterBrowser;
import drbd.gui.GuiComboBox;
import drbd.gui.HeartbeatGraph;
import drbd.gui.dialog.cluster.DrbdLogs;
import drbd.data.resources.DrbdResource;
import drbd.data.Host;
import drbd.data.DrbdXML;
import drbd.data.DRBDtestData;
import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;
import drbd.utilities.ButtonCallback;
import drbd.utilities.DRBD;
import drbd.utilities.MyMenuItem;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.LinkedHashMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;

/**
 * this class holds info data, menus and configuration
 * for a drbd resource.
 */
public final class DrbdResourceInfo extends DrbdGuiInfo
                                    implements CommonDeviceInterface {
    /** BlockDevInfo object of the first block device. */
    private final BlockDevInfo blockDevInfo1;
    /** BlockDevInfo object of the second block device. */
    private final BlockDevInfo blockDevInfo2;
    /**
     * Whether the block device is used by heartbeat via Filesystem service.
     */
    private ServiceInfo isUsedByCRM;
    /** Cache for getInfoPanel method. */
    private JComponent infoPanel = null;
    /** Whether the meta-data has to be created or not. */
    private boolean haveToCreateMD = false;
    /** Last created filesystem. */
    private String createdFs = null;
    /** Name of the drbd resource name parameter. */
    static final String DRBD_RES_PARAM_NAME = "name";
    /** Name of the drbd device parameter. */
    static final String DRBD_RES_PARAM_DEV = "device";
    /** String that is displayed as a tool tip if a menu item is used by CRM. */
    static final String IS_USED_BY_CRM_STRING = "it is used by cluster manager";
    /** String that is displayed as a tool tip for disabled menu item. */
    static final String IS_SYNCING_STRING = "it is being full-synced";
    /** String that is displayed as a tool tip for disabled menu item. */
    static final String IS_VERIFYING_STRING = "it is being verified";

    /**
     * Prepares a new <code>DrbdResourceInfo</code> object.
     */
    DrbdResourceInfo(final String name,
                     final String drbdDev,
                     final BlockDevInfo blockDevInfo1,
                     final BlockDevInfo blockDevInfo2,
                     final Browser browser) {
        super(name, browser);
        setResource(new DrbdResource(name, null)); // TODO: ?
        setResource(new DrbdResource(name, drbdDev)); // TODO: ?
        // TODO: drbdresource
        getResource().setValue(DRBD_RES_PARAM_DEV, drbdDev);
        this.blockDevInfo1 = blockDevInfo1;
        this.blockDevInfo2 = blockDevInfo2;
    }

    /** Returns device name, like /dev/drbd0. */
    @Override public String getDevice() {
        return getDrbdResource().getDevice();
    }

    /** Returns other block device in the drbd cluster. */
    public BlockDevInfo getOtherBlockDevInfo(final BlockDevInfo bdi) {
        if (bdi.equals(blockDevInfo1)) {
            return blockDevInfo2;
        } else if (bdi.equals(blockDevInfo2)) {
            return blockDevInfo1;
        } else {
            return null;
        }
    }

    /** Returns first block dev info. */
    public BlockDevInfo getFirstBlockDevInfo() {
        return blockDevInfo1;
    }

    /** Returns second block dev info. */
    public BlockDevInfo getSecondBlockDevInfo() {
        return blockDevInfo2;
    }

    /** Returns true if this is first block dev info. */
    public boolean isFirstBlockDevInfo(final BlockDevInfo bdi) {
        return blockDevInfo1 == bdi;
    }

    /** Creates and returns drbd config for resources. */
    String drbdResourceConfig() throws Exceptions.DrbdConfigException {
        final StringBuffer config = new StringBuffer(50);
        config.append("resource " + getName() + " {\n");
        /* protocol... */
        final String[] params = getBrowser().getDrbdXML().getSectionParams(
                                                                   "resource");
        for (String param : params) {
            if (DRBD_RES_PARAM_DEV.equals(param)) {
                continue;
            }
            final String value = getComboBoxValue(param);
            if (value != null && value.equals(getParamDefault(param))) {
                continue;
            }
            config.append('\t');
            config.append(param);
            config.append('\t');
            config.append(value);
            config.append(";\n");
        }
        if (params.length != 0) {
            config.append('\n');
        }
        /* section config */
        try {
            config.append(drbdSectionsConfig());
        } catch (Exceptions.DrbdConfigException dce) {
            throw dce;
        }
        /* startup disk syncer net */
        config.append(blockDevInfo1.drbdNodeConfig(getName(), getDevice()));
        config.append('\n');
        config.append(blockDevInfo2.drbdNodeConfig(getName(), getDevice()));
        config.append('}');
        return config.toString();
    }

    /** Clears info panel cache. */
    @Override public boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /** Returns sync progress in percent. */
    public String getSyncedProgress() {
        return blockDevInfo1.getBlockDevice().getSyncedProgress();
    }

    /** Returns whether the cluster is syncing. */
    public boolean isSyncing() {
        return blockDevInfo1.getBlockDevice().isSyncing()
               || blockDevInfo2.getBlockDevice().isSyncing();
    }

    /** Returns whether the cluster is being verified. */
    public boolean isVerifying() {
        return blockDevInfo1.getBlockDevice().isVerifying()
               || blockDevInfo2.getBlockDevice().isVerifying();
    }

    /** Connect block device from the specified host. */
    public void connect(final Host host, final boolean testOnly) {
        if (blockDevInfo1.getHost() == host
            && !blockDevInfo1.isConnectedOrWF(false)) {
            blockDevInfo1.connect(testOnly);
        } else if (blockDevInfo2.getHost() == host
                   && !blockDevInfo2.isConnectedOrWF(false)) {
            blockDevInfo2.connect(testOnly);
        }
    }

    /**
     * Returns whether the resources is connected, meaning both devices are
     * connected.
     */
    public boolean isConnected(final boolean testOnly) {
        return blockDevInfo1.isConnected(testOnly)
               && blockDevInfo2.isConnected(testOnly);
    }

    /**
     * Returns whether any of the sides in the drbd resource are in
     * paused-sync state.
     */
    boolean isPausedSync() {
        return blockDevInfo1.getBlockDevice().isPausedSync()
               || blockDevInfo2.getBlockDevice().isPausedSync();
    }

    /**
     * Returns whether any of the sides in the drbd resource are in
     * split-brain.
     */
    public boolean isSplitBrain() {
        return blockDevInfo1.getBlockDevice().isSplitBrain()
               || blockDevInfo2.getBlockDevice().isSplitBrain();
    }

    /** Returns drbd graphical view. */
    @Override public JPanel getGraphicalView() {
        return getBrowser().getDrbdGraph().getGraphPanel();
    }

    /** Returns all parameters. */
    @Override public String[] getParametersFromXML() {
        return getBrowser().getDrbdXML().getParameters();
    }

    /**
     * Checks the new value of the parameter if it is conforms to its type
     * and other constraints.
     */
    @Override protected boolean checkParam(final String param,
                                                 final String newValue) {
        if (DRBD_RES_PARAM_AFTER.equals(param)) {
            /* drbdsetup xml syncer says it should be numeric, but in
               /etc/drbd.conf it is not. */
            return true;
        }
        return getBrowser().getDrbdXML().checkParam(param, newValue);
    }

    /** Returns the default value for the drbd parameter. */
    @Override public String getParamDefault(final String param) {
        final String common = getDrbdInfo().getParamSaved(param);
        if (common != null) {
            return common;
        }
        return getBrowser().getDrbdXML().getParamDefault(param);
    }

    /** Returns section to which this drbd parameter belongs. */
    @Override protected String getSection(final String param) {
        return getBrowser().getDrbdXML().getSection(param);
    }

    /** Whether the parameter should be enabled. */
    @Override protected String isEnabled(final String param) {
        if (getDrbdResource().isCommited()
            && (DRBD_RES_PARAM_NAME.equals(param)
                || DRBD_RES_PARAM_DEV.equals(param))) {
            return "";
        }
        return null;
    }

    /** Returns the widget that is used to edit this parameter. */
    @Override protected GuiComboBox getParamComboBox(final String param,
                                                           final String prefix,
                                                           final int width) {
        GuiComboBox paramCb;
        if (DRBD_RES_PARAM_NAME.equals(param)) {
            String resName;
            if (getParamSaved(DRBD_RES_PARAM_NAME) == null) {
                resName = getResource().getDefaultValue(DRBD_RES_PARAM_NAME);
            } else {
                resName = getResource().getName();
            }
            paramCb = new GuiComboBox(resName,
                                      null, /* items */
                                      null, /* units */
                                      null, /* type */
                                      "^\\S+$", /* regexp */
                                      width,
                                      null, /* abbrv */
                                      new AccessMode(
                                           getAccessType(param),
                                           isEnabledOnlyInAdvancedMode(param)));
            paramCb.setEnabled(!getDrbdResource().isCommited());
            paramComboBoxAdd(param, prefix, paramCb);
        } else if (DRBD_RES_PARAM_DEV.equals(param)) {
            final List<String> drbdDevices = new ArrayList<String>();
            if (getParamSaved(DRBD_RES_PARAM_DEV) == null) {
                final String defaultItem =
                        getDrbdResource().getDefaultValue(DRBD_RES_PARAM_DEV);
                drbdDevices.add(defaultItem);
                int i = 0;
                int index = 0;
                while (i < 11) {
                    final String drbdDevStr = "/dev/drbd"
                                              + Integer.toString(index);
                    final Map<String, DrbdResourceInfo> drbdDevHash =
                                                  getBrowser().getDrbdDevHash();
                    if (!drbdDevHash.containsKey(drbdDevStr)) {
                        drbdDevices.add(drbdDevStr);
                        i++;
                    }
                    getBrowser().putDrbdDevHash();
                    index++;
                }
                paramCb = new GuiComboBox(defaultItem,
                                          drbdDevices.toArray(
                                               new String[drbdDevices.size()]),
                                          null, /* units */
                                          null, /* type */
                                          null, /* regexp */
                                          width,
                                          null, /* abbrv */
                                          new AccessMode(
                                           getAccessType(param),
                                           isEnabledOnlyInAdvancedMode(param)));
                paramCb.setEditable(true);
            } else {
                final String defaultItem = getDevice();
                String regexp = null;
                if (isInteger(param)) {
                    regexp = "^-?\\d*$";
                }
                paramCb = new GuiComboBox(
                                       defaultItem,
                                       getResource().getPossibleChoices(param),
                                       null, /* units */
                                       null, /* type */
                                       regexp,
                                       width,
                                       null, /* abbrv */
                                       new AccessMode(
                                           getAccessType(param),
                                           isEnabledOnlyInAdvancedMode(param)));
            }
            paramCb.setEnabled(!getDrbdResource().isCommited());
            paramComboBoxAdd(param, prefix, paramCb);
        } else if (DRBD_RES_PARAM_AFTER.equals(param)) {
            // TODO: has to be reloaded
            final List<Info> l = new ArrayList<Info>();
            String defaultItem = getParamSaved(DRBD_RES_PARAM_AFTER);
            final StringInfo di = new StringInfo(
                                        Tools.getString("ClusterBrowser.None"),
                                        "-1",
                                        getBrowser());
            l.add(di);
            final Map<String, DrbdResourceInfo> drbdResHash =
                                                getBrowser().getDrbdResHash();
            if (defaultItem == null || "-1".equals(defaultItem)) {
                defaultItem = Tools.getString("ClusterBrowser.None");
            } else if (defaultItem != null) {
                final DrbdResourceInfo dri = drbdResHash.get(defaultItem);
                if (dri != null) {
                    defaultItem = dri.getDevice();
                }
            }

            for (final String drbdRes : drbdResHash.keySet()) {
                final DrbdResourceInfo r = drbdResHash.get(drbdRes);
                DrbdResourceInfo odri = r;
                boolean cyclicRef = false;
                while ((odri = drbdResHash.get(
                       odri.getParamSaved(DRBD_RES_PARAM_AFTER))) != null) {
                    if (odri == this) {
                        cyclicRef = true;
                    }
                }
                if (r != this && !cyclicRef) {
                    l.add(r);
                }
            }
            getBrowser().putDrbdResHash();
            paramCb = new GuiComboBox(defaultItem,
                                      l.toArray(new Info[l.size()]),
                                      null, /* units */
                                      null, /* type */
                                      null, /* regexp */
                                      width,
                                      null, /* abbrv */
                                      new AccessMode(
                                           getAccessType(param),
                                           isEnabledOnlyInAdvancedMode(param)));

            paramComboBoxAdd(param, prefix, paramCb);
        } else {
            paramCb = super.getParamComboBox(param, prefix, width);
        }
        return paramCb;
    }

    /** Returns the DrbdResource object of this drbd resource. */
    DrbdResource getDrbdResource() {
        return (DrbdResource) getResource();
    }

    /** Applies changes that user made to the drbd resource fields. */
    public void apply(final boolean testOnly) {
        if (!testOnly) {
            final String[] params = getParametersFromXML();
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                }
            });
            getBrowser().getDrbdResHash().remove(getName());
            getBrowser().putDrbdResHash();
            getBrowser().getDrbdDevHash().remove(getDevice());
            getBrowser().putDrbdDevHash();
            storeComboBoxValues(params);

            final String name = getParamSaved(DRBD_RES_PARAM_NAME);
            final String drbdDevStr = getParamSaved(DRBD_RES_PARAM_DEV);
            getDrbdResource().setName(name);
            setName(name);
            getDrbdResource().setDevice(drbdDevStr);

            getBrowser().getDrbdResHash().put(name, this);
            getBrowser().putDrbdResHash();
            getBrowser().getDrbdDevHash().put(drbdDevStr, this);
            getBrowser().putDrbdDevHash();
            getBrowser().getDrbdGraph().repaint();
            getDrbdResource().setCommited(true);
            getDrbdInfo().setAllApplyButtons();
        }
    }

    /** Set all apply buttons. */
    void setAllApplyButtons() {
        final BlockDevInfo bdi1 = blockDevInfo1;
        if (bdi1 != null) {
            bdi1.storeComboBoxValues(bdi1.getParametersFromXML());
            bdi1.setApplyButtons(null, bdi1.getParametersFromXML());
        }

        final BlockDevInfo bdi2 = blockDevInfo2;
        if (bdi2 != null) {
            bdi2.storeComboBoxValues(bdi2.getParametersFromXML());
            bdi2.setApplyButtons(null, bdi2.getParametersFromXML());
        }
        setApplyButtons(null, getParametersFromXML());
    }

    /** Returns panel with form to configure a drbd resource. */
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
            @Override public final boolean isEnabled() {
                return true;
            }

            @Override public final void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                getBrowser().getDrbdGraph().stopTestAnimation(getApplyButton());
                getApplyButton().setToolTipText(null);
            }

            @Override public final void mouseOver() {
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
                final Map<Host,String> testOutput =
                                         new LinkedHashMap<Host, String>();
                try {
                    getDrbdInfo().createDrbdConfig(true);
                    for (final Host h : getCluster().getHostsArray()) {
                        DRBD.adjust(h, "all", true);
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
        getResource().setValue(DRBD_RES_PARAM_NAME,
                               getDrbdResource().getName());
        getResource().setValue(DRBD_RES_PARAM_DEV, getDevice());

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
                            getDrbdInfo().createDrbdConfig(false);
                            for (final Host h : getCluster().getHostsArray()) {
                                DRBD.adjust(h, "all", false);
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

    /** Remove myself from all hashes. */
    public void removeFromHashes() {
        getBrowser().getDrbdResHash().remove(getName());
        getBrowser().putDrbdResHash();
        getBrowser().getDrbdDevHash().remove(getDevice());
        getBrowser().putDrbdDevHash();
        blockDevInfo1.removeFromDrbd();
        blockDevInfo2.removeFromDrbd();
    }

    /**
     * removes this object from jtree and from list of drbd resource
     * infos without confirmation dialog.
     */
    void removeMyselfNoConfirm(final boolean testOnly) {
        getBrowser().drbdStatusLock();
        getBrowser().getDrbdXML().removeResource(getName());
        getBrowser().getDrbdGraph().removeDrbdResource(this);
        final Host[] hosts = getCluster().getHostsArray();
        for (final Host host : hosts) {
            DRBD.down(host, getName(), testOnly);
        }
        super.removeMyself(testOnly);
        final Map<String, DrbdResourceInfo> drbdResHash =
                                                getBrowser().getDrbdResHash();
        final DrbdResourceInfo dri = drbdResHash.get(getName());
        drbdResHash.remove(getName());
        getBrowser().putDrbdResHash();
        dri.setName(null);
        getBrowser().reload(getBrowser().getDrbdNode(), true);
        getBrowser().getDrbdDevHash().remove(getDevice());
        getBrowser().putDrbdDevHash();
        blockDevInfo1.removeFromDrbd();
        blockDevInfo2.removeFromDrbd();
        blockDevInfo1.removeMyself(testOnly);
        blockDevInfo2.removeMyself(testOnly);
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

    /** Returns string of the drbd resource. */
    @Override public String toString() {
        String name = getName();
        if (name == null || "".equals(name)) {
            name = Tools.getString("ClusterBrowser.DrbdResUnconfigured");
        }
        return "drbd: " + name;
    }

    /** Returns whether two drbd resources are equal. */
    @Override public boolean equals(final Object value) {
        if (value == null) {
            return false;
        }
        if (Tools.isStringClass(value)) {
            return getDrbdResource().getValue(DRBD_RES_PARAM_DEV).equals(
                                                             value.toString());
        } else {
            if (toString() == null) {
                return false;
            }
            return toString().equals(value.toString());
        }
    }

    //@Override int hashCode() {
    //    return toString().hashCode();
    //}

    /**
     * Returns the device name that is used as the string value of
     * the device in the filesystem resource.
     */
    @Override public String getStringValue() {
        return getDevice();
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

    /**
     * Adds linbit::drbd service in the pacemaker graph.
     *
     * @param fi
     *              File system before which this drbd info should be
     *              started
     */
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

    /** Sets that this drbd resource is used by hb. */
    @Override public void setUsedByCRM(final ServiceInfo isUsedByCRM) {
        this.isUsedByCRM = isUsedByCRM;
    }

    /** Returns whether this drbd resource is used by crm. */
    @Override public boolean isUsedByCRM() {
        return isUsedByCRM != null && isUsedByCRM.isManaged(false);
    }

    /** Returns common file systems. */
    public StringInfo[] getCommonFileSystems(final String defaultValue) {
        return getBrowser().getCommonFileSystems(defaultValue);
    }

    /** Returns both hosts of the drbd connection, sorted alphabeticaly. */
    public Host[] getHosts() {
        final Host h1 = blockDevInfo1.getHost();
        final Host h2 = blockDevInfo2.getHost();
        if (h1.getName().compareToIgnoreCase(h2.getName()) < 0) {
            return new Host[]{h1, h2};
        } else {
            return new Host[]{h2, h1};
        }
    }

    /** Starts resolve split brain dialog. */
    void resolveSplitBrain() {
        final AddDrbdSplitBrainDialog adrd = new AddDrbdSplitBrainDialog(this);
        adrd.showDialogs();
    }

    /** Starts online verification. */
    void verify(final boolean testOnly) {
        blockDevInfo1.verify(testOnly);
    }

    /** Returns whether the specified host has this drbd resource. */
    boolean resourceInHost(final Host host) {
        if (blockDevInfo1.getHost() == host
            || blockDevInfo2.getHost() == host) {
            return true;
        }
        return false;
    }

    /** Returns the list of items for the popup menu for drbd resource. */
    @Override public List<UpdatableItem> createPopup() {
        final boolean testOnly = false;
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final DrbdResourceInfo thisClass = this;

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

            @Override public final String enablePredicate() {
                if (!Tools.getConfigData().isAdvancedMode() && isUsedByCRM()) {
                    return IS_USED_BY_CRM_STRING;
                }
                if (isSyncing()) {
                    return IS_SYNCING_STRING;
                }
                return null;
            }

            @Override public final void action() {
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

            @Override public final boolean predicate() {
                return isPausedSync();
            }

            @Override public final String enablePredicate() {
                if (!isSyncing()) {
                    return "it is not syncing";
                }
                return null;
            }
            @Override public final void action() {
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

            @Override public final String enablePredicate() {
                if (isSplitBrain()) {
                    return null;
                } else {
                    return "";
                }
            }

            @Override public final void action() {
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

            @Override public final String enablePredicate() {
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

            @Override public final void action() {
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
            @Override public final void action() {
                /* this drbdResourceInfo remove myself and this calls
                   removeDrbdResource in this class, that removes the edge
                   in the graph. */
                removeMyself(testOnly);
            }

            @Override public final String enablePredicate() {
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

            @Override public final String enablePredicate() {
                return null;
            }

            @Override public final void action() {
                hidePopup();
                final String device = getDevice();
                DrbdLogs l = new DrbdLogs(getCluster(), device);
                l.showDialog();
            }
        };
        items.add(viewLogMenu);
        return items;
    }

    /**
     * Sets whether the meta-data have to be created, meaning there are no
     * existing meta-data for this resource on both nodes.
     */
    public void setHaveToCreateMD(final boolean haveToCreateMD) {
        this.haveToCreateMD = haveToCreateMD;
    }

    /** Returns whether the md has to be created or not. */
    public boolean isHaveToCreateMD() {
        return haveToCreateMD;
    }

    /** Returns meta-disk device for the specified host. */
    public String getMetaDiskForHost(final Host host) {
        return getBrowser().getDrbdXML().getMetaDisk(host.getName(), getName());
    }

    /** Returns tool tip when mouse is over the resource edge. */
    @Override public String getToolTipForGraph(final boolean testOnly) {
        final StringBuffer s = new StringBuffer(50);
        s.append("<html><b>");
        s.append(getName());
        s.append("</b><br>");
        if (isSyncing()) {
            final String spString = getSyncedProgress();
            final String bsString =
                                blockDevInfo1.getBlockDevice().getBlockSize();
            final String rateString = getResource().getValue("rate");
            if (spString != null && bsString != null &&
                rateString != null) {
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
                    if (seconds < 60*5) {
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

    /** Returns the last created filesystem. */
    @Override public String getCreatedFs() {
        return createdFs;
    }

    /** Sets the last created filesystem. */
    public void setCreatedFs(final String createdFs) {
        this.createdFs = createdFs;
    }

    /** Returns how much diskspace is used on the primary. */
    @Override public int getUsed() {
        if (blockDevInfo1.getBlockDevice().isPrimary()) {
            return blockDevInfo1.getBlockDevice().getUsed();
        } else if (blockDevInfo2.getBlockDevice().isPrimary()) {
            return blockDevInfo2.getBlockDevice().getUsed();
        }
        return -1;
    }

    /** Sets stored parameters. */
    public void setParameters() {
        final DrbdXML dxml = getBrowser().getDrbdXML();
        final String resName = getResource().getName();
        for (String section : dxml.getSections()) {
            for (final String param : dxml.getSectionParams(section)) {
                if (DRBD_RES_PARAM_DEV.equals(param)) {
                    continue;
                }
                String value = dxml.getConfigValue(resName, section, param);
                final String defaultValue = getParamDefault(param);
                final String oldValue = getParamSaved(param);
                if ("".equals(value)) {
                    value = defaultValue;
                }
                final GuiComboBox cb = paramComboBoxGet(param, null);
                if (!Tools.areEqual(value, oldValue)) {
                    getResource().setValue(param, value);
                    if (cb != null) {
                        cb.setValue(value);
                    }
                }
            }
        }
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    @Override public boolean checkResourceFieldsChanged(final String param,
                                                        final String[] params) {
        return checkResourceFieldsChanged(param, params, false);
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    boolean checkResourceFieldsChanged(final String param,
                                       final String[] params,
                                              final boolean fromDrbdInfo) {
        final DrbdInfo di = getDrbdInfo();
        if (di != null && !fromDrbdInfo) {
            di.setApplyButtons(null, di.getParametersFromXML());
        }
        boolean changed = false;
        final BlockDevInfo bdi1 = blockDevInfo1;
        if (bdi1 != null
            && bdi1.checkResourceFieldsChanged(param,
                                               bdi1.getParametersFromXML(),
                                               fromDrbdInfo,
                                               true)) {
            changed = true;
        }

        final BlockDevInfo bdi2 = blockDevInfo2;
        if (bdi2 != null
            && bdi2.checkResourceFieldsChanged(param,
                                               bdi2.getParametersFromXML(),
                                               fromDrbdInfo,
                                               true)) {
            changed = true;
        }
        return super.checkResourceFieldsChanged(param, params) || changed;
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    @Override public boolean checkResourceFieldsCorrect(final String param,
                                                        final String[] params) {
        return checkResourceFieldsCorrect(param, params, false);
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    boolean checkResourceFieldsCorrect(final String param,
                                       final String[] params,
                                       final boolean fromDrbdInfo) {
        final DrbdInfo di = getDrbdInfo();
        boolean correct = true;
        final BlockDevInfo bdi1 = blockDevInfo1;
        if (bdi1 != null
            && !bdi1.getBlockDevice().isNew()
            && !bdi1.checkResourceFieldsCorrect(param,
                                                bdi1.getParametersFromXML(),
                                                fromDrbdInfo,
                                                true)) {
            correct = false;
        }

        final BlockDevInfo bdi2 = blockDevInfo2;
        if (bdi2 != null
            && !bdi2.getBlockDevice().isNew()
            && !bdi2.checkResourceFieldsCorrect(param,
                                                bdi2.getParametersFromXML(),
                                                fromDrbdInfo,
                                                true)) {
            correct = false;
        }
        return super.checkResourceFieldsCorrect(param, params) && correct;
    }

    /** Revert all values. */
    @Override public void revert() {
        super.revert();
        final BlockDevInfo bdi1 = blockDevInfo1;
        if (bdi1 != null) {
            bdi1.revert();
        }
        final BlockDevInfo bdi2 = blockDevInfo2;
        if (bdi2 != null) {
            bdi2.revert();
        }
    }

    /** Sets if dialog was started. It disables the apply button. */
    @Override public void setDialogStarted(final boolean dialogStarted) {
        final BlockDevInfo bdi1 = blockDevInfo1;
        if (bdi1 != null) {
            bdi1.setDialogStarted(dialogStarted);
        }
        final BlockDevInfo bdi2 = blockDevInfo2;
        if (bdi2 != null) {
            bdi2.setDialogStarted(dialogStarted);
        }
        super.setDialogStarted(dialogStarted);
    }
}
