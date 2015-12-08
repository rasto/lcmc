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

import com.google.common.base.Optional;
import lcmc.Exceptions;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.widget.Check;
import lcmc.cluster.ui.widget.Widget;
import lcmc.common.domain.Application;
import lcmc.common.domain.Application.RunMode;
import lcmc.common.domain.ResourceValue;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.ui.utils.ComponentWithTest;
import lcmc.common.ui.utils.Dialogs;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.configs.AppDefaults;
import lcmc.drbd.domain.DRBDtestData;
import lcmc.drbd.domain.DrbdXml;
import lcmc.drbd.service.DRBD;
import lcmc.drbd.ui.AddDrbdConfigDialog;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostFactory;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides drbd info. For one it shows the editable global
 * drbd config, but if a drbd block device is selected it forwards to the
 * block device info, which is defined in HostBrowser.java.
 */
@Named
public class GlobalInfo extends AbstractDrbdInfo {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalInfo.class);
    private static final String SECTION_COMMON_PROXY = "proxy";

    private static final ImageIcon DRBD_ICON = Tools.createImageIcon(Tools.getDefault("ClusterBrowser.DRBDIconSmall"));
    static final String ALL_LOGS_PATTERN = "/dev/drbd[0-9]*";
    static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(Tools.getDefault("ClustersPanel.ClusterIcon"));
    private BlockDevInfo selectedBlockDevice = null;
    private JComponent infoPanel = null;
    @Inject
    private GlobalMenu globalMenu;
    @Inject
    private HostFactory hostFactory;
    @Inject
    private Provider<VolumeInfo> volumeInfoProvider;
    @Inject
    private Provider<AddDrbdConfigDialog> addDrbdConfigDialogProvider;
    private ProxyHostInfo proxyHostInfo = null;
    @Inject
    private Provider<ProxyHostInfo> proxyHostInfoProvider;
    @Inject
    private Provider<ResourceInfo> resourceInfoProvider;
    @Inject
    private Provider<DrbdXml> drbdXmlProvider;
    @Inject
    private Application application;
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private TreeMenuController treeMenuController;
    @Inject
    private Dialogs dialogs;

    public void einit(final String name, final Browser browser) {
        super.einit(Optional.of(new ResourceValue(name)), name, browser);
    }

    public void setParameters() {
        final DrbdXml dxml = getBrowser().getDrbdXml();
        final Cluster cluster = getCluster();
        for (final String hostName : dxml.getProxyHostNames()) {
            final Host proxyHost = cluster.getProxyHostByName(hostName);
            if (proxyHost == null) {
                final Host hp = hostFactory.createInstance();
                hp.setHostname(hostName);
                cluster.addProxyHost(hp);
                addProxyHostNode(hp);
            } else {
                if (proxyHostInfo != null && proxyHostInfo.getNode() == null) {
                    addProxyHostNode(proxyHost);
                }
            }
        }
        for (final String param : getParametersFromXML()) {
            final String sectionString = dxml.getSection(param);
            /* remove -options */
            final String section = sectionString.replaceAll("-options$", "");
            Value value;
            final Value defaultValue = getParamDefault(param);
            if (DrbdXml.GLOBAL_SECTION.equals(section)) {
                value = dxml.getGlobalConfigValue(param);
                if (value == null) {
                    value = defaultValue;
                }
            } else {
                value = dxml.getCommonConfigValue(section, param);
                if (value == null || value.isNothingSelected()) {
                    value = defaultValue;
                }
            }
            if ("usage-count".equals(param)) {
                value = getComboBoxValue(param);
                if (value == null || value.isNothingSelected()) {
                    /* we don't get this parameter from the dump. */
                    value = DrbdXml.CONFIG_YES;
                }
            }
            final Value oldValue = getParamSaved(param);
            final Widget wi = getWidget(param, null);
            if (!Tools.areEqual(value, oldValue)) {
                getResource().setValue(param, value);
                if (wi != null) {
                    wi.setValueAndWait(value);
                }
            }
        }
    }

    /** Sets selected block device. */
    public void setSelectedNode(final BlockDevInfo bdi) {
        selectedBlockDevice = bdi;
    }

    /**
     * Gets combo box for paremeter in te global config. usage-count is
     * disabled.
     */
    @Override
    protected Widget createWidget(final String param, final String prefix, final int width) {
        final Widget wi = super.createWidget(param, prefix, width);
        if ("usage-count".equals(param)) {
            wi.setEnabled(false);
        }
        return wi;
    }

    /** Creates drbd config. */
    public void createDrbdConfigLive() throws Exceptions.DrbdConfigException, UnknownHostException {
        final Map<Host, String> testOutput = new LinkedHashMap<Host, String>();
        if (!createConfigDryRun(testOutput)) {
            createConfigError(testOutput);
            return;
        }
        createDrbdConfig(RunMode.LIVE);
    }

    /** Returns lsit of all parameters as an array. */
    @Override
    public String[] getParametersFromXML() {
        final DrbdXml drbdXml = getBrowser().getDrbdXml();
        if (drbdXml == null) {
            return null;
        }
        return getEnabledSectionParams(drbdXml.getGlobalParams());
    }

    /** Section name that is displayed. */
    @Override
    protected String getSectionDisplayName(final String section) {
        if (DrbdXml.GLOBAL_SECTION.equals(section)) {
            return super.getSectionDisplayName(section);
        } else {
            return Tools.getString("GlobalInfo.CommonSection") + super.getSectionDisplayName(section);
        }
    }

    /**
     * Returns section to which this parameter belongs.
     * This is used for grouping in the info panel.
     */
    @Override
    protected String getSection(final String param) {
        return getBrowser().getDrbdXml().getSection(param);
    }

    /** Applies changes made in the info panel by user. */
    public void apply(final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            final String[] params = getParametersFromXML();
            storeComboBoxValues(params);
            swingUtils.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (final ResourceInfo dri : getDrbdResources()) {
                        dri.setParameters();
                    }
                    setAllApplyButtons();
                }
            });
        }
    }

    public boolean createConfigDryRun(final Map<Host, String> testOutput) {
        try {
            createDrbdConfig(Application.RunMode.TEST);
            boolean allOk = true;
            for (final Host h : getCluster().getHostsArray()) {
                int ret = DRBD.adjustApply(h, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.TEST);
                final String output = DRBD.getDRBDtest();
                testOutput.put(h, output);
                if (ret != 0) {
                    allOk = false;
                }
            }
            return allOk;
        } catch (final Exceptions.DrbdConfigException dce) {
            LOG.appError("getInfoPanel: config failed", dce);
        } catch (final UnknownHostException e) {
            LOG.appError("getInfoPanel: config failed", e);
        }
        return false;
    }

    /**
     * Returns info panel for drbd. If a block device was selected, its
     * info panel is shown.
     */
    @Override
    public JComponent getInfoPanel() {
        if (selectedBlockDevice != null) { /* block device is not in drbd */
            final JComponent c = selectedBlockDevice.getInfoPanel();
            infoPanelDone();
            return c;
        }
        if (infoPanel != null) {
            infoPanelDone();
            return infoPanel;
        }
        final JPanel mainPanel = new JPanel();
        if (getBrowser().getDrbdXml() == null) {
            mainPanel.add(new JLabel("drbd info not available"));
            infoPanelDone();
            return mainPanel;
        }
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;

            /** Whether the whole thing should be enabled. */
            @Override
            public boolean isEnabled() {
                final Host dcHost = getBrowser().getDCHost();
                return dcHost != null && !Tools.versionBeforePacemaker(dcHost);
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
                try {
                    getBrowser().setDRBDtestData(null);
                    final Map<Host, String> testOutput = new LinkedHashMap<Host, String>();
                    createConfigDryRun(testOutput);
                    final DRBDtestData dtd = new DRBDtestData(testOutput);
                    component.setToolTipText(dtd.getToolTip());
                    getBrowser().setDRBDtestData(dtd);
                } finally {
                    getBrowser().drbdtestLockRelease();
                    startTestLatch.countDown();
                }
            }
        };
        initApplyButton(buttonCallback);
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

        final String[] params = getParametersFromXML();
        enableSection(SECTION_COMMON_PROXY, false, !WIZARD);
        addParams(optionsPanel,
                  params,
                  application.getDefaultSize("ClusterBrowser.DrbdResLabelWidth"),
                  application.getDefaultSize("ClusterBrowser.DrbdResFieldWidth"),
                  null);

        getApplyButton().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    LOG.debug1("getInfoPanel: BUTTON: apply");
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            swingUtils.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    getApplyButton().setEnabled(false);
                                    getRevertButton().setEnabled(false);
                                    getApplyButton().setToolTipText("");
                                }
                            });
                            getBrowser().drbdStatusLock();
                            try {
                                createDrbdConfigLive();
                                for (final Host h : getCluster().getHosts()) {
                                    DRBD.adjustApply(h, DRBD.ALL_DRBD_RESOURCES, null, Application.RunMode.LIVE);
                                }
                                apply(Application.RunMode.LIVE);
                            } catch (final Exceptions.DrbdConfigException dce) {
                                LOG.appError("getInfoPanel: config failed", dce);
                            } catch (final UnknownHostException uhe) {
                                LOG.appError("getInfoPanel: config failed", uhe);
                            } finally {
                                getBrowser().drbdStatusUnlock();
                            }
                        }
                    });
                    thread.start();
                }
            }
        );
        getRevertButton().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    LOG.debug1("getInfoPanel: BUTTON: revert");
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getBrowser().drbdStatusLock();
                            revert();
                            getBrowser().drbdStatusUnlock();
                        }
                    });
                    thread.start();
                }
            }
        );

        /* apply button */
        addApplyButton(buttonPanel);
        addRevertButton(buttonPanel);

        mainPanel.add(optionsPanel);

        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.PAGE_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(
               application.getDefaultSize("ClusterBrowser.DrbdResLabelWidth")
               + application.getDefaultSize("ClusterBrowser.DrbdResFieldWidth") + 4));
        newPanel.add(new JScrollPane(mainPanel));
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    /**
     * Clears info panel cache.
     * TODO: should select something.
     */
    @Override
    public boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /** Returns drbd graph in a panel. */
    @Override
    public JPanel getGraphicalView() {
        if (selectedBlockDevice != null) {
            getBrowser().getDrbdGraph().pickBlockDevice(selectedBlockDevice);
        }
        return getBrowser().getDrbdGraph().getGraphPanel();
    }

    /**
     * Selects and highlights this node. This function is overwritten
     * because block devices don't have here their own node, but
     * views change depending on selectedNode variable.
     */
    @Override
    public void selectMyself() {
        if (selectedBlockDevice == null || !selectedBlockDevice.getBlockDevice().isDrbd()) {
            treeMenuController.reloadNode(getNode(), true);
            treeMenuController.nodeChanged(getNode());
        } else {
            treeMenuController.reloadNode(selectedBlockDevice.getNode(), true);
            treeMenuController.nodeChanged(selectedBlockDevice.getNode());
        }
    }

    /** Returns new drbd resource index, the one that is not used . */
    private int getNewDrbdResourceIndex() {
        final Iterator<String> it = getBrowser().getDrbdResourceNameHash().keySet().iterator();
        int index = -1;

        while (it.hasNext()) {
            final String name = it.next();
            // TODO: should not assume r0
            final Pattern p = Pattern.compile("^r(\\d+)$");
            final Matcher m = p.matcher(name);

            if (m.matches()) {
                final int i = Integer.parseInt(m.group(1));
                if (i > index) {
                    index = i;
                }
            }
        }
        getBrowser().putDrbdResHash();
        return index + 1;
    }

    /** Adds existing drbd volume to the GUI. */
    public VolumeInfo addDrbdVolume(final ResourceInfo dri,
                                    final String volumeNr,
                                    final String drbdDevStr,
                                    final List<BlockDevInfo> blockDevInfos,
                                    final Application.RunMode runMode) {
        final VolumeInfo volumeInfo = volumeInfoProvider.get();
        volumeInfo.init(volumeNr, drbdDevStr, dri, blockDevInfos, getBrowser());
        dri.addDrbdVolume(volumeInfo);
        addDrbdVolume(volumeInfo);
        return volumeInfo;
    }

    /** Add drbd volume. */
    public void addDrbdVolume(final BlockDevInfo bd1,
                              final BlockDevInfo bd2,
                              final boolean interactive,
                              final Application.RunMode runMode) {
        if (interactive) {
            if (bd1 != null) {
                bd1.getBlockDevice().setNew(true);
            }
            if (bd2 != null) {
                bd2.getBlockDevice().setNew(true);
            }
            final GlobalInfo thisClass = this;
            final AddDrbdConfigDialog addDrbdConfigDialog = addDrbdConfigDialogProvider.get();
            addDrbdConfigDialog.init(thisClass, bd1, bd2);
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    addDrbdConfigDialog.init(thisClass, bd1, bd2);
                    addDrbdConfigDialog.showDialogs();
                    /* remove wizard parameters from hashes. */
                    for (final String p : bd1.getParametersFromXML()) {
                        bd1.widgetRemove(p, Widget.WIZARD_PREFIX);
                        bd2.widgetRemove(p, Widget.WIZARD_PREFIX);
                    }
                    if (addDrbdConfigDialog.isWizardCanceled()) {
                        final VolumeInfo dvi = bd1.getDrbdVolumeInfo();
                        if (dvi != null) {
                            dvi.removeMyself(runMode);
                        }
                        getBrowser().getDrbdGraph().stopAnimation(bd1);
                        getBrowser().getDrbdGraph().stopAnimation(bd2);
                        return;
                    }

                    updateDrbdInfo();
                }
            });
            thread.start();
        } else {
            getBrowser().resetFilesystems();
        }
    }

    /** Return new DRBD resoruce info object. */
    public ResourceInfo getNewDrbdResource(final Set<Host> hosts) {
        final int index = getNewDrbdResourceIndex();
        final String name = 'r' + Integer.toString(index);
        /* search for next available drbd device */
        final ResourceInfo resourceInfo = resourceInfoProvider.get();
        resourceInfo.init(name, hosts, getBrowser());
        resourceInfo.getResource().setNew(true);
        return resourceInfo;
    }

    /** Return new DRBD volume info object. */
    public VolumeInfo getNewDrbdVolume(final ResourceInfo dri, final List<BlockDevInfo> blockDevInfos) {
        final Map<String, VolumeInfo> drbdDevHash = getBrowser().getDrbdDeviceHash();
        int index = 0;
        String drbdDevStr = "/dev/drbd" + Integer.toString(index);

        while (drbdDevHash.containsKey(drbdDevStr)) {
            index++;
            drbdDevStr = "/dev/drbd" + Integer.toString(index);
        }
        getBrowser().putDrbdDevHash();
        final String volumeNr = dri.getAvailVolumeNumber();
        final VolumeInfo volumeInfo = volumeInfoProvider.get();
        volumeInfo.init(volumeNr, drbdDevStr, dri, blockDevInfos, getBrowser());
        return volumeInfo;
    }

    /** Add DRBD resource. */
    public void addDrbdResource(final ResourceInfo dri) {
        final String name = dri.getName();
        dri.getDrbdResource().setDefaultValue(ResourceInfo.DRBD_RES_PARAM_NAME, new StringValue(name));
        getBrowser().getDrbdResourceNameHash().put(name, dri);
        getBrowser().putDrbdResHash();

        final DefaultMutableTreeNode drbdResourceNode = treeMenuController.createMenuItem(getBrowser().getDrbdNode(), dri);
        treeMenuController.reloadNode(getBrowser().getDrbdNode(), true);
        treeMenuController.reloadNode(drbdResourceNode, true);
    }

    /** Add DRBD volume. */
    public void addDrbdVolume(final VolumeInfo dvi) {
        /* We want next port number on both devices to be the same,
         * although other port numbers may not be the same on both. */
        final BlockDevInfo bdi1 = dvi.getFirstBlockDevInfo();
        final BlockDevInfo bdi2 = dvi.getSecondBlockDevInfo();
        final String device = dvi.getDevice();

        bdi1.setDrbd(true);
        bdi1.setDrbdVolumeInfo(dvi);
        bdi1.getBlockDevice().setDrbdBlockDevice(bdi1.getHost().getHostParser().getDrbdBlockDevice(device));
        bdi1.cleanup();
        bdi1.resetInfoPanel();
        bdi1.setInfoPanel(null); /* reload panel */

        bdi1.getInfoPanel();

        bdi2.setDrbd(true);
        bdi2.setDrbdVolumeInfo(dvi);
        bdi2.getBlockDevice().setDrbdBlockDevice(bdi2.getHost().getHostParser().getDrbdBlockDevice(device));
        bdi2.cleanup();
        bdi2.resetInfoPanel();
        bdi2.setInfoPanel(null); /* reload panel */

        bdi2.getInfoPanel();

        final DefaultMutableTreeNode drbdVolumeNode = treeMenuController.createMenuItem(
                dvi.getDrbdResourceInfo().getNode(),
                dvi);

        swingUtils.isSwingThread();

        final DefaultMutableTreeNode drbdBDNode1 = treeMenuController.createMenuItem(drbdVolumeNode, bdi1);
        final DefaultMutableTreeNode drbdBDNode2 = treeMenuController.createMenuItem(drbdVolumeNode, bdi2);

        getBrowser().getDrbdGraph().addDrbdVolume(dvi, bdi1, bdi2);
        treeMenuController.reloadNode(drbdVolumeNode, true);
        getBrowser().resetFilesystems();
    }

    /** Adds existing drbd resource to the GUI. */
    public ResourceInfo addDrbdResource(final String name, final Set<Host> hosts, final Application.RunMode runMode) {
        final DrbdXml dxml = getBrowser().getDrbdXml();
        final ResourceInfo resourceInfo = resourceInfoProvider.get();
        resourceInfo.init(name, hosts, getBrowser());
        final String[] sections = dxml.getSections();
        for (final String sectionString : sections) {
            /* remove -options */
            final String section = sectionString.replaceAll("-options$", "");
            final String[] params = dxml.getSectionParams(sectionString);
            for (final String param : params) {
                Value value = dxml.getConfigValue(name, section, param);
                if (value == null || value.isNothingSelected()) {
                    value = dxml.getParamDefault(param);
                }
                resourceInfo.getDrbdResource().setValue(param, value);
            }
        }
        resourceInfo.getDrbdResource().setCommited(true);
        addDrbdResource(resourceInfo);
        getBrowser().resetFilesystems();
        return resourceInfo;
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    @Override
    public Check checkResourceFields(final String param, final String[] params) {
        final List<String> incorrect = new ArrayList<String>();
        final List<String> changed = new ArrayList<String>();
        final Check check = new Check(incorrect, changed);
        for (final ResourceInfo dri : getDrbdResources()) {
            check.addCheck(dri.checkResourceFields(param, dri.getParametersFromXML(), true));
        }
        if (getBrowser().getDrbdResourceNameHash().isEmpty()) {
            getBrowser().putDrbdResHash();
            incorrect.add("no resources inside");
        } else {
            getBrowser().putDrbdResHash();
        }

        final DrbdXml dxml = getBrowser().getDrbdXml();
        if (dxml != null && dxml.isDrbdDisabled()) {
            incorrect.add("DRBD is disabled");
        }

        check.addCheck(super.checkResourceFields(param, params));
        return check;
    }

    /** Revert all values. */
    @Override
    public void revert() {
        super.revert();
        for (final ResourceInfo dri : getDrbdResources()) {
            dri.revert();
        }
    }

    /** Set all apply buttons. */
    public void setAllApplyButtons() {
        for (final ResourceInfo dri : getDrbdResources()) {
            dri.storeComboBoxValues(dri.getParametersFromXML());
            dri.setAllApplyButtons();
        }
    }

    /** Returns all drbd resources in this cluster. */
    public Collection<ResourceInfo> getDrbdResources() {
        final Collection<ResourceInfo> resources = new LinkedHashSet<ResourceInfo>();
        final Host[] hosts = getCluster().getHostsArray();
        for (final ResourceInfo dri : getBrowser().getDrbdResHashValues()) {
            for (final Host host : hosts) {
                if (dri.resourceInHost(host)) {
                    resources.add(dri);
                }
            }
        }
        return resources;
    }

    /**
     * Returns true, if all version on all connected hosts are higher are
     * equal the specified version.
     */
    public boolean atLeastVersion(final String drbdVersion) {
        for (final Host host : getCluster().getHostsArray()) {
            try {
                if (host.hasDrbd() && host.drbdVersionSmaller(drbdVersion)) {
                    return false;
                }
            } catch (final Exceptions.IllegalVersionException e) {
                LOG.appWarning("atLeastVersion: " + e.getMessage(), e);
            }
        }
        return true;
    }

    /** Reload DRBD resource combo boxes. (resync-after). */
    public void reloadDRBDResourceComboBoxes() {
        for (final ResourceInfo dri : getDrbdResources()) {
            dri.reloadComboBoxes();
        }
    }

    /** Returns menu icon for drbd resource. */
    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        return DRBD_ICON;
    }

    /** Menu icon. */
    @Override
    public ImageIcon getCategoryIcon(final Application.RunMode runMode) {
        return DRBD_ICON;
    }

    /** Return section color. */
    @Override
    protected Color getSectionColor(final String section) {
        if (SECTION_COMMON_PROXY.equals(section)) {
            return AppDefaults.LIGHT_ORANGE;
        }
        return super.getSectionColor(section);
    }

    /** Enable proxy section. It stays till the next restart. */
    void enableProxySection(final boolean wizard) {
        enableSection(SECTION_COMMON_PROXY, true, wizard);
    }

    /** Add DRBD resource. */
    public void addProxyHostNode(final Host host) {
        proxyHostInfo = proxyHostInfoProvider.get();
        proxyHostInfo.init(host, host.getBrowser());
        final DefaultMutableTreeNode proxyHostNode = treeMenuController.createMenuItem(getBrowser().getDrbdNode(), proxyHostInfo);
        treeMenuController.reloadNode(getBrowser().getDrbdNode(), true);
        treeMenuController.reloadNode(proxyHostNode, true);
    }

    /**
     * Returns background popup. Click on background represents cluster as
     * whole.
     */
    @Override
    public List<UpdatableItem> createPopup() {
        return globalMenu.getPulldownMenu(this);
    }

    /** Reset info panel. */
    @Override
    public void resetInfoPanel() {
        super.resetInfoPanel();
        infoPanel = null;
    }

    public void updateDrbdInfo() {
        final DrbdXml newDrbdXml = drbdXmlProvider.get();
        newDrbdXml.init(getCluster().getHostsArray(), getBrowser().getHostDrbdParameters());
        for (final Host host : getCluster().getHosts()) {
            final String configString = newDrbdXml.getConfig(host);
            if (configString != null) {
                newDrbdXml.update(configString);
            }
        }
        getBrowser().setDrbdXml(newDrbdXml);
        getBrowser().resetFilesystems();
    }

    private void createConfigError(final Map<Host, String> testOutput) {
        final StringBuilder message = new StringBuilder(100);
        message.append("createConfigError: failed to create config\n");
        for (final Map.Entry<Host, String> entry : testOutput.entrySet()) {
            final Host host = entry.getKey();
            final String output = entry.getValue();
            message.append("host: ").append(host.getName());
            message.append('\n');
            message.append("output: ").append(output);
            message.append('\n');
        }
        LOG.appError(message.toString());
    }

    /** Creates drbd config without dry run. */
    public void createDrbdConfig(final Application.RunMode runMode)
               throws Exceptions.DrbdConfigException, UnknownHostException {
        /* resources */
        final Collection<Host> hosts = new LinkedHashSet<Host>(
                                                    getCluster().getHosts());
        hosts.addAll(getCluster().getProxyHosts());
        for (final Host host : hosts) {
            final StringBuilder globalConfig = new StringBuilder(160);
            globalConfig.append("## generated by drbd-gui\n\n");

            final StringBuilder global = new StringBuilder(80);
            final DrbdXml dxml = getBrowser().getDrbdXml();
            /* global options */
            final String[] params = dxml.getSectionParams(DrbdXml.GLOBAL_SECTION);
            global.append("global {\n");
            final boolean volumesAvailable = host.hasVolumes();
            for (final String param : params) {
                Value value = getComboBoxValue(param);
                if (value == null || value.isNothingSelected()) {
                    if ("usage-count".equals(param)) {
                        value = DrbdXml.CONFIG_YES;
                    } else {
                        continue;
                    }
                }
                if (!value.equals(dxml.getParamDefault(param))) {
                    if ("disable-ip-verification".equals(param)
                        || (!volumesAvailable && (isCheckBox(param) || "booleanhandler".equals(getParamType(param))))) {
                        if (value.equals(DrbdXml.CONFIG_YES)) {
                            /* boolean parameter */
                            global.append("\t\t").append(param).append(";\n");
                        }
                    } else {
                        /* also boolean parameter since 8.4 */
                        /* except of disable-ip-verification */
                        global.append("\t\t");
                        global.append(param);
                        global.append('\t');
                        global.append(Tools.escapeConfig(value.getValueForConfigWithUnit()));
                        global.append(";\n");
                    }
                }
            }
            global.append("}\n");
            if (global.length() > 0) {
                globalConfig.append(global);
            }

            /* common section */
            final String common = drbdSectionsConfig(host);
            String commonSectionConfig = "";
            if (common != null && !common.isEmpty()) {
                commonSectionConfig = "\ncommon {\n" + common + '}';
            }

            final Map<String, String> resConfigs = new LinkedHashMap<String, String>();
            final Set<Host> proxyHosts = getCluster().getProxyHosts();
            for (final ResourceInfo dri : getBrowser().getDrbdResHashValues()) {
                if (dri.resourceInHost(host) || proxyHosts.contains(host)) {
                    final String rConf = dri.drbdResourceConfig(host);
                    resConfigs.put(dri.getName(), rConf);
                }
            }
            boolean bigDRBDConf = true;
            try {
                bigDRBDConf = application.getBigDRBDConf() || host.drbdVersionSmaller("8.3.7");
            } catch (final Exceptions.IllegalVersionException e) {
                LOG.appWarning("createDrbdConfig: " + e.getMessage(), e);
            }
            final String dir;
            final String configName;
            final boolean makeBackup;
            final String preCommand;
            if (Application.isTest(runMode)) {
                dir = "/var/lib/drbd/";
                configName = "drbd.conf-lcmc-test";
                makeBackup = false;
                preCommand = null;
                bigDRBDConf = true;
            } else {
                dir = "/etc/";
                configName = "drbd.conf";
                makeBackup = true;
                if (bigDRBDConf) {
                    preCommand = "mv /etc/drbd.d{,.bak.`date +'%s'`} 2>/dev/null";
                } else {
                    preCommand = "cp -r /etc/drbd.d{,.bak.`date +'%s'`} 2>/dev/null";
                }
            }
            if (bigDRBDConf) {
                /* one big drbd.conf */
                host.getSSH().createConfig(globalConfig
                                           + commonSectionConfig
                                           + "\n\n"
                                           + Tools.join("\n", resConfigs.values()),
                                           configName,
                                           dir,
                                           "0600",
                                           makeBackup,
                                           preCommand,
                                           null);
            } else {
                /* global */
                host.getSSH().createConfig(globalConfig + commonSectionConfig,
                                           "global_common.conf",
                                           dir + "drbd.d.temp/",
                                           "0600",
                                           false,
                                           preCommand,
                                           null);
                /* *.res */
                for (final String resConfigName : resConfigs.keySet()) {
                    host.getSSH().createConfig(
                                   resConfigs.get(resConfigName),
                                   resConfigName + ".res",
                                   dir + "drbd.d.temp/",
                                   "0600",
                                   false,
                                   null,
                                   null);
                }
                /* drbd.conf */
                final StringBuilder tempDRBDConf = new StringBuilder(200);
                /* drbd.conf.temp -> drbd.d/ */
                tempDRBDConf.append("## generated by drbd-gui\n\n")
                            .append("include \"drbd.d/global_common.conf\";\n")
                            .append("include \"drbd.d/*.res\";");
                host.getSSH().createConfig(tempDRBDConf.toString(),
                                           configName + ".temp",
                                           dir,
                                           "0600",
                                           false,
                                           null,
                                           null);
                final StringBuilder drbdConf = new StringBuilder(200);
                /* drbd.conf -> drbd.d.temp/ (new config) */
                drbdConf.append("## generated by drbd-gui\n\n");
                drbdConf.append("include \"drbd.d.temp/global_common.conf\";\n");
                drbdConf.append("include \"drbd.d.temp/*.res\";");
                host.getSSH().createConfig(
                               drbdConf.toString(),
                               configName,
                               dir,
                               "0600",
                               makeBackup,
                               null,
                               "rm -rf /etc/drbd.d 2>/dev/null; "
                               + "cp -r /etc/drbd.d{.temp,} && "
                               + "mv /etc/drbd.conf{.temp,} && "
                               + "rm -rf /etc/drbd.d.temp/");
                               /* all this is to stay atomic. */
            }
        }
    }

    public void exportGraphAsPng() {
        final Optional<String> savePath = dialogs.getFileName("lcmc-drbd");
        if (savePath.isPresent()) {
            new Thread() {
                public void run() {
                    BufferedImage image = getBrowser().getDrbdGraph().createImage();
                    Tools.writeImage(savePath.get(), image, "PNG");
                }
            }.start();
        }
    }
}
