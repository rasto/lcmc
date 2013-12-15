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
package lcmc.gui.resources;

import lcmc.Exceptions;
import lcmc.ProxyHostWizard;
import lcmc.AddDrbdConfigDialog;
import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.widget.Widget;
import lcmc.data.Host;
import lcmc.data.DrbdXML;
import lcmc.data.resources.Resource;
import lcmc.data.DRBDtestData;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.data.Cluster;
import lcmc.utilities.Tools;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.DRBD;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.MyMenuItem;
import lcmc.configs.AppDefaults;
import lcmc.gui.dialog.cluster.DrbdLogs;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Iterator;
import java.net.UnknownHostException;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ImageIcon;
import lcmc.EditClusterDialog;
import lcmc.data.StringValue;
import lcmc.data.Value;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class provides drbd info. For one it shows the editable global
 * drbd config, but if a drbd block device is selected it forwards to the
 * block device info, which is defined in HostBrowser.java.
 */
public final class DrbdInfo extends DrbdGuiInfo {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(DrbdInfo.class);
    /** Selected block device. */
    private BlockDevInfo selectedBD = null;
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Common proxy section. */
    private static final String SECTION_COMMON_PROXY = "proxy";

    /** DRBD icon. */
    private static final ImageIcon DRBD_ICON = Tools.createImageIcon(
                             Tools.getDefault("ClusterBrowser.DRBDIconSmall"));
    /** Device thath matches all drbd devices in the log. */
    private static final String ALL_LOGS_PATTERN = "/dev/drbd[0-9]*";
    /** Icon of the cluster. */
    static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(
                                Tools.getDefault("ClustersPanel.ClusterIcon"));

    /** Prepares a new <code>DrbdInfo</code> object. */
    public DrbdInfo(final String name, final Browser browser) {
        super(name, browser);
        setResource(new Resource(name));
        ((ClusterBrowser) browser).getDrbdGraph().setDrbdInfo(this);
    }

    /** Sets stored parameters. */
    public void setParameters() {
        final DrbdXML dxml = getBrowser().getDrbdXML();
        final Cluster cluster = getCluster();
        for (final String hostName : dxml.getProxyHostNames()) {

            final Host proxyHost = cluster.getProxyHostByName(hostName);
            if (proxyHost == null) {
                final Host hp = new Host();
                hp.setHostname(hostName);
                cluster.addProxyHost(hp);
                addProxyHostNode(hp);
            } else {
                final ProxyHostInfo phi =
                                     proxyHost.getBrowser().getProxyHostInfo();
                if (phi != null && phi.getNode() == null) {
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
            if (DrbdXML.GLOBAL_SECTION.equals(section)) {
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
                    value = DrbdXML.CONFIG_YES;
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
        this.selectedBD = bdi;
    }

    /**
     * Gets combo box for paremeter in te global config. usage-count is
     * disabled.
     */
    @Override
    protected Widget createWidget(final String param,
                                  final String prefix,
                                  final int width) {
        final Widget wi = super.createWidget(param, prefix, width);
        if ("usage-count".equals(param)) {
            wi.setEnabled(false);
        }
        return wi;
    }

    /** Creates drbd config. */
    public void createDrbdConfig(final boolean testOnly)
               throws Exceptions.DrbdConfigException, UnknownHostException {
        /* resources */
        final Set<Host> hosts = new LinkedHashSet<Host>(
                                                    getCluster().getHosts());
        hosts.addAll(getCluster().getProxyHosts());
        for (Host host : hosts) {
            final StringBuilder globalConfig = new StringBuilder(160);
            globalConfig.append("## generated by drbd-gui\n\n");

            final StringBuilder global = new StringBuilder(80);
            final DrbdXML dxml = getBrowser().getDrbdXML();
            /* global options */
            final String[] params =
                                dxml.getSectionParams(DrbdXML.GLOBAL_SECTION);
            global.append("global {\n");
            final boolean volumesAvailable = host.hasVolumes();
            for (final String param : params) {
                Value value = getComboBoxValue(param);
                if (value == null || value.isNothingSelected()) {
                    if ("usage-count".equals(param)) {
                        value = DrbdXML.CONFIG_YES;
                    } else {
                        continue;
                    }
                }
                if (!value.equals(dxml.getParamDefault(param))) {
                    if ("disable-ip-verification".equals(param)
                        || (!volumesAvailable
                            && (isCheckBox(param)
                                || "booleanhandler".equals(
                                                       getParamType(param))))) {
                        if (value.equals(DrbdXML.CONFIG_YES)) {
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
            if (!"".equals(common)) {
                commonSectionConfig = "\ncommon {\n" + common + "}";
            }

            final Map<String, String> resConfigs =
                                           new LinkedHashMap<String, String>();
            final Set<Host> proxyHosts = getCluster().getProxyHosts();
            for (final DrbdResourceInfo dri
                                       : getBrowser().getDrbdResHashValues()) {
                if (dri.resourceInHost(host) || proxyHosts.contains(host)) {
                    try {
                        final String rConf = dri.drbdResourceConfig(host);
                        resConfigs.put(dri.getName(), rConf);
                    } catch (Exceptions.DrbdConfigException dce) {
                        throw dce;
                    }
                }
            }
            String dir;
            String configName;
            boolean makeBackup;
            String preCommand;
            boolean bigDRBDConf = true;
            try {
                bigDRBDConf =
                  Tools.getConfigData().getBigDRBDConf()
                  || Tools.compareVersions(host.getDrbdVersion(), "8.3.7") < 0;
            } catch (Exceptions.IllegalVersionException e) {
                LOG.appWarning("createDrbdConfig: " + e.getMessage(), e);
            }
            if (testOnly) {
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
                    preCommand =
                        "mv /etc/drbd.d{,.bak.`date +'%s'`} 2>/dev/null";
                } else {
                    preCommand =
                        "cp -r /etc/drbd.d{,.bak.`date +'%s'`} 2>/dev/null";
                }
            }
            if (bigDRBDConf) {
                /* one big drbd.conf */
                host.getSSH().createConfig(globalConfig.toString()
                                           + commonSectionConfig
                                           + "\n\n"
                                           + Tools.join("\n",
                                                        resConfigs.values()),
                                           configName,
                                           dir,
                                           "0600",
                                           makeBackup,
                                           preCommand,
                                           null);
            } else {
                /* global */
                host.getSSH().createConfig(globalConfig.toString()
                                           + commonSectionConfig,
                                           "global_common.conf",
                                           dir + "drbd.d.temp/",
                                           "0600",
                                           false,
                                           preCommand,
                                           null);
                /* *.res */
                for (final String resConfigName : resConfigs.keySet()) {
                    host.getSSH().createConfig(
                                   resConfigs.get(resConfigName).toString(),
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
                tempDRBDConf.append("## generated by drbd-gui\n\n");
                tempDRBDConf.append("include \"drbd.d/global_common.conf\";\n");
                tempDRBDConf.append("include \"drbd.d/*.res\";");
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
                drbdConf.append(
                            "include \"drbd.d.temp/global_common.conf\";\n");
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

    /** Returns lsit of all parameters as an array. */
    @Override
    public String[] getParametersFromXML() {
        final DrbdXML drbdXML = getBrowser().getDrbdXML();
        if (drbdXML == null) {
            return null;
        }
        return getEnabledSectionParams(drbdXML.getGlobalParams());
    }

    /** Section name that is displayed. */
    @Override
    protected String getSectionDisplayName(final String section) {
        if (DrbdXML.GLOBAL_SECTION.equals(section)) {
            return super.getSectionDisplayName(section);
        } else {
            return Tools.getString("DrbdInfo.CommonSection")
                   + super.getSectionDisplayName(section);
        }
    }

    /**
     * Returns section to which this parameter belongs.
     * This is used for grouping in the info panel.
     */
    @Override
    protected String getSection(final String param) {
        return getBrowser().getDrbdXML().getSection(param);
    }

    /** Applies changes made in the info panel by user. */
    public void apply(final boolean testOnly) {
        if (!testOnly) {
            final String[] params = getParametersFromXML();
            Tools.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                    getApplyButton().setToolTipText("");
                }
            });
            storeComboBoxValues(params);
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (final DrbdResourceInfo dri : getDrbdResources()) {
                        dri.setParameters();
                    }
                    setAllApplyButtons();
                }
            });
        }
    }

    /**
     * Returns info panel for drbd. If a block device was selected, its
     * info panel is shown.
     */
    @Override
    public JComponent getInfoPanel() {
        if (selectedBD != null) { /* block device is not in drbd */
            final JComponent c = selectedBD.getInfoPanel();
            infoPanelDone();
            return c;
        }
        if (infoPanel != null) {
            infoPanelDone();
            return infoPanel;
        }
        final JPanel mainPanel = new JPanel();
        if (getBrowser().getDrbdXML() == null) {
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
                if (dcHost == null) {
                    return false;
                }
                if (Tools.versionBeforePacemaker(dcHost)) {
                    return false;
                }
                return true;
            }

            @Override
            public void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                getBrowser().getDrbdGraph().stopTestAnimation(getApplyButton());
                getApplyButton().setToolTipText("");
            }

            @Override
            public void mouseOver() {
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
                    createDrbdConfig(true);
                    for (final Host h : getCluster().getHostsArray()) {
                        DRBD.adjustApply(h, DRBD.ALL, null, true);
                        testOutput.put(h, DRBD.getDRBDtest());
                    }
                    final DRBDtestData dtd = new DRBDtestData(testOutput);
                    getApplyButton().setToolTipText(dtd.getToolTip());
                    getBrowser().setDRBDtestData(dtd);
                } catch (Exceptions.DrbdConfigException dce) {
                    LOG.appError("getInfoPanel: config failed", dce);
                } catch (UnknownHostException e) {
                    LOG.appError("getInfoPanel: config failed", e);
                } finally {
                    getBrowser().drbdtestLockRelease();
                    startTestLatch.countDown();
                }
            }
        };
        initApplyButton(buttonCallback);
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        mainPanel.add(buttonPanel);

        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.EAST);

        final String[] params = getParametersFromXML();
        enableSection(SECTION_COMMON_PROXY, false, !WIZARD);
        addParams(optionsPanel,
                  params,
                  Tools.getDefaultSize("ClusterBrowser.DrbdResLabelWidth"),
                  Tools.getDefaultSize("ClusterBrowser.DrbdResFieldWidth"),
                  null);

        getApplyButton().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    LOG.debug1("getInfoPanel: BUTTON: apply");
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Tools.invokeAndWait(new Runnable() {
                                @Override
                                public void run() {
                                    getApplyButton().setEnabled(false);
                                    getRevertButton().setEnabled(false);
                                }
                            });
                            getBrowser().drbdStatusLock();
                            try {
                                createDrbdConfig(false);
                                for (final Host h : getCluster().getHosts()) {
                                    DRBD.adjustApply(h, DRBD.ALL, null, false);
                                }
                                apply(false);
                            } catch (final Exceptions.DrbdConfigException dce) {
                                LOG.appError("getInfoPanel: config failed", dce);
                                return;
                            } catch (final UnknownHostException e) {
                                LOG.appError("getInfoPanel: config failed", e);
                                return;
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
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(
               Tools.getDefaultSize("ClusterBrowser.DrbdResLabelWidth")
               + Tools.getDefaultSize("ClusterBrowser.DrbdResFieldWidth") + 4));
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
    boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /** Returns drbd graph in a panel. */
    @Override
    public JPanel getGraphicalView() {
        if (selectedBD != null) {
            getBrowser().getDrbdGraph().pickBlockDevice(selectedBD);
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
        if (selectedBD == null || !selectedBD.getBlockDevice().isDrbd()) {
            getBrowser().reload(getNode(), true);
            getBrowser().nodeChanged(getNode());
        } else {
            getBrowser().reload(selectedBD.getNode(), true);
            getBrowser().nodeChanged(selectedBD.getNode());
        }
    }

    /** Returns new drbd resource index, the one that is not used . */
    private int getNewDrbdResourceIndex() {
        final Iterator<String> it =
                         getBrowser().getDrbdResHash().keySet().iterator();
        int index = -1;

        while (it.hasNext()) {
            final String name = it.next();
            // TODO: should not assume r0
            final Pattern p = Pattern.compile("^" + "r" + "(\\d+)$");
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
    public DrbdVolumeInfo addDrbdVolume(final DrbdResourceInfo dri,
                                        final String volumeNr,
                                        final String drbdDevStr,
                                        final List<BlockDevInfo> blockDevInfos,
                                        final boolean testOnly) {
        final DrbdVolumeInfo dvi = new DrbdVolumeInfo(volumeNr,
                                                      drbdDevStr,
                                                      dri,
                                                      blockDevInfos,
                                                      getBrowser());
        dri.addDrbdVolume(dvi);
        addDrbdVolume(dvi);
        return dvi;
    }

    /** Add drbd volume. */
    public void addDrbdVolume(final BlockDevInfo bd1,
                              final BlockDevInfo bd2,
                              final boolean interactive,
                              final boolean testOnly) {
        if (interactive) {
            if (bd1 != null) {
                bd1.getBlockDevice().setNew(true);
            }
            if (bd2 != null) {
                bd2.getBlockDevice().setNew(true);
            }
            final DrbdInfo thisClass = this;
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    //getBrowser().reload(drbdResourceNode, true);
                    AddDrbdConfigDialog adrd = new AddDrbdConfigDialog(
                                                                    thisClass,
                                                                    bd1,
                                                                    bd2);
                    adrd.showDialogs();
                    /* remove wizard parameters from hashes. */
                    for (final String p : bd1.getParametersFromXML()) {
                        bd1.widgetRemove(p, Widget.WIZARD_PREFIX);
                        bd2.widgetRemove(p, Widget.WIZARD_PREFIX);
                    }
                    if (adrd.isCanceled()) {
                        final DrbdVolumeInfo dvi = bd1.getDrbdVolumeInfo();
                        if (dvi != null) {
                            dvi.removeMyself(testOnly);
                        }
                        getBrowser().getDrbdGraph().stopAnimation(bd1);
                        getBrowser().getDrbdGraph().stopAnimation(bd2);
                        return;
                    }

                    getBrowser().updateCommonBlockDevices();
                    final DrbdXML newDrbdXML =
                        new DrbdXML(getCluster().getHostsArray(),
                                    getBrowser().getDrbdParameters());
                    final String configString1 =
                                           newDrbdXML.getConfig(bd1.getHost());
                    if (configString1 != null) {
                        newDrbdXML.update(configString1);
                    }
                    final String configString2 =
                                           newDrbdXML.getConfig(bd2.getHost());
                    if (configString2 != null) {
                        newDrbdXML.update(configString2);
                    }
                    getBrowser().setDrbdXML(newDrbdXML);
                    getBrowser().resetFilesystems();
                }
            });
            thread.start();
        } else {
            getBrowser().resetFilesystems();
        }
    }

    /** Return new DRBD resoruce info object. */
    public DrbdResourceInfo getNewDrbdResource(final Set<Host> hosts) {
        final int index = getNewDrbdResourceIndex();
        final String name = "r" + Integer.toString(index);
        /* search for next available drbd device */
        final DrbdResourceInfo dri =
                                new DrbdResourceInfo(name, hosts, getBrowser());
        dri.getResource().setNew(true);
        return dri;
    }

    /** Return new DRBD volume info object. */
    public DrbdVolumeInfo getNewDrbdVolume(
                                    final DrbdResourceInfo dri,
                                    final List<BlockDevInfo> blockDevInfos) {
        final Map<String, DrbdVolumeInfo> drbdDevHash =
                                                getBrowser().getDrbdDevHash();
        int index = 0;
        String drbdDevStr = "/dev/drbd" + Integer.toString(index);

        while (drbdDevHash.containsKey(drbdDevStr)) {
            index++;
            drbdDevStr = "/dev/drbd" + Integer.toString(index);
        }
        getBrowser().putDrbdDevHash();
        final String volumeNr = dri.getAvailVolumeNumber();
        final DrbdVolumeInfo dvi =
             new DrbdVolumeInfo(volumeNr,
                                drbdDevStr,
                                dri,
                                blockDevInfos,
                                getBrowser());
        return dvi;
    }

    /** Add DRBD resource. */
    public void addDrbdResource(final DrbdResourceInfo dri) {
        final String name = dri.getName();
        dri.getDrbdResource().setDefaultValue(
                                        DrbdResourceInfo.DRBD_RES_PARAM_NAME,
                                        new StringValue(name));
        getBrowser().getDrbdResHash().put(name, dri);
        getBrowser().putDrbdResHash();

        final DefaultMutableTreeNode drbdResourceNode =
                                           new DefaultMutableTreeNode(dri);
        getBrowser().reload(getBrowser().getDrbdNode(), true);
        dri.setNode(drbdResourceNode);
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                getBrowser().getDrbdNode().add(drbdResourceNode);
                getBrowser().reloadAndWait(drbdResourceNode, true);
            }
        });
    }

    /** Add DRBD volume. */
    public void addDrbdVolume(final DrbdVolumeInfo dvi) {
        /* We want next port number on both devices to be the same,
         * although other port numbers may not be the same on both. */
        final BlockDevInfo bdi1 = dvi.getFirstBlockDevInfo();
        final BlockDevInfo bdi2 = dvi.getSecondBlockDevInfo();
        final String device = dvi.getDevice();

        if (bdi1 != null) {
            bdi1.setDrbd(true);
            bdi1.setDrbdVolumeInfo(dvi);
            bdi1.getBlockDevice().setDrbdBlockDevice(
                                    bdi1.getHost().getDrbdBlockDevice(device));
            bdi1.cleanup();
            bdi1.resetInfoPanel();
            bdi1.setInfoPanel(null); /* reload panel */
            bdi1.getInfoPanel();
        }
        if (bdi2 != null) {
            bdi2.setDrbd(true);
            bdi2.setDrbdVolumeInfo(dvi);
            bdi2.getBlockDevice().setDrbdBlockDevice(
                                    bdi2.getHost().getDrbdBlockDevice(device));
            bdi2.cleanup();
            bdi2.resetInfoPanel();
            bdi2.setInfoPanel(null); /* reload panel */
            bdi2.getInfoPanel();
        }

        final DefaultMutableTreeNode drbdVolumeNode =
                                           new DefaultMutableTreeNode(dvi);
        dvi.setNode(drbdVolumeNode);

        Tools.isSwingThread();
        dvi.getDrbdResourceInfo().getNode().add(drbdVolumeNode);

        final DefaultMutableTreeNode drbdBDNode1 =
                                           new DefaultMutableTreeNode(bdi1);
        bdi1.setNode(drbdBDNode1);
        final DefaultMutableTreeNode drbdBDNode2 =
                                           new DefaultMutableTreeNode(bdi2);
        bdi2.setNode(drbdBDNode2);
        drbdVolumeNode.add(drbdBDNode1);
        drbdVolumeNode.add(drbdBDNode2);

        getBrowser().getDrbdGraph().addDrbdVolume(dvi, bdi1, bdi2);
        getBrowser().reload(drbdVolumeNode, true);
        getBrowser().resetFilesystems();
    }

    /** Adds existing drbd resource to the GUI. */
    public DrbdResourceInfo addDrbdResource(final String name,
                                            final Set<Host> hosts,
                                            final boolean testOnly) {
        final DrbdXML dxml = getBrowser().getDrbdXML();
        final DrbdResourceInfo dri =
                               new DrbdResourceInfo(name, hosts, getBrowser());
        final String[] sections = dxml.getSections();
        for (final String sectionString : sections) {
            /* remove -options */
            final String section = sectionString.replaceAll("-options$", "");
            final String[] params = dxml.getSectionParams(sectionString);
            for (String param : params) {
                Value value = dxml.getConfigValue(name, section, param);
                if (value == null || value.isNothingSelected()) {
                    value = dxml.getParamDefault(param);
                }
                dri.getDrbdResource().setValue(param, value);
            }
        }
        dri.getDrbdResource().setCommited(true);
        addDrbdResource(dri);
        getBrowser().resetFilesystems();
        return dri;
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    @Override
    public boolean checkResourceFieldsChanged(final String param,
                                              final String[] params) {
        boolean changed = false;
        for (final DrbdResourceInfo dri : getDrbdResources()) {
            if (dri.checkResourceFieldsChanged(param,
                                               dri.getParametersFromXML(),
                                               true)) {
                changed = true;
            }
        }
        return super.checkResourceFieldsChanged(param, params) || changed;
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    //TODO: dead code?
    boolean checkResourceFieldsCorrect(final String param,
                                       final String[] params,
                                       final boolean fromDrbdInfo) {
        boolean correct = true;
        for (final DrbdResourceInfo dri : getDrbdResources()) {
            if (!dri.checkResourceFieldsCorrect(param,
                                                dri.getParametersFromXML(),
                                                true)) {
                correct = false;
            }
        }
        return super.checkResourceFieldsCorrect(param, params) && correct;
    }

    /** Returns true if all fields are correct. */
    @Override
    boolean checkResourceFieldsCorrect(final String param,
                                       final String[] params) {
        if (getBrowser().getDrbdResHash().isEmpty()) {
            getBrowser().putDrbdResHash();
            return false;
        }
        getBrowser().putDrbdResHash();
        final DrbdXML dxml = getBrowser().getDrbdXML();
        if (dxml != null && dxml.isDrbdDisabled()) {
            return false;
        }
        return super.checkResourceFieldsCorrect(param, params);
    }

    /** Revert all values. */
    @Override
    public void revert() {
        super.revert();
        for (final DrbdResourceInfo dri : getDrbdResources()) {
            dri.revert();
        }
    }

    /** Set all apply buttons. */
    public void setAllApplyButtons() {
        for (final DrbdResourceInfo dri : getDrbdResources()) {
            dri.storeComboBoxValues(dri.getParametersFromXML());
            dri.setAllApplyButtons();
        }
    }

    /** Returns all drbd resources in this cluster. */
    public Set<DrbdResourceInfo> getDrbdResources() {
        final Set<DrbdResourceInfo> resources =
                                        new LinkedHashSet<DrbdResourceInfo>();
        final Host[] hosts = getCluster().getHostsArray();
        for (final DrbdResourceInfo dri : getBrowser().getDrbdResHashValues()) {
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
            final String hostDrbdVersion = host.getDrbdVersion();
            if (hostDrbdVersion == null) {
                continue;
            }
            try {
                if (Tools.compareVersions(hostDrbdVersion, drbdVersion) < 0) {
                    return false;
                }
            } catch (Exceptions.IllegalVersionException e) {
                LOG.appWarning("atLeastVersion: " + e.getMessage(), e);
            }
        }
        return true;
    }

    /** Reload DRBD resource combo boxes. (resync-after). */
    public void reloadDRBDResourceComboBoxes() {
        for (final DrbdResourceInfo dri : getDrbdResources()) {
            dri.reloadComboBoxes();
        }
    }

    /** Returns menu icon for drbd resource. */
    @Override
    public ImageIcon getMenuIcon(final boolean testOnly) {
        return DRBD_ICON;
    }

    /** Menu icon. */
    @Override
    public ImageIcon getCategoryIcon(final boolean testOnly) {
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
        final ProxyHostInfo proxyHostInfo =
                                    new ProxyHostInfo(host, host.getBrowser());
        host.getBrowser().setProxyHostInfo(proxyHostInfo);
        final DefaultMutableTreeNode proxyHostNode =
                                   new DefaultMutableTreeNode(proxyHostInfo);
        getBrowser().reload(getBrowser().getDrbdNode(), true);
        proxyHostInfo.setNode(proxyHostNode);
        Tools.isSwingThread();
        getBrowser().getDrbdNode().add(proxyHostNode);
        getBrowser().reload(proxyHostNode, true);
    }

    /**
     * Returns background popup. Click on background represents cluster as
     * whole.
     */
    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final boolean testOnly = false;

        /** Add proxy host */
        final MyMenuItem addProxyHostMenu = new MyMenuItem(
                Tools.getString("DrbdInfo.AddProxyHost"),
                null,
                Tools.getString("DrbdInfo.AddProxyHost"),
                new AccessMode(ConfigData.AccessType.OP, false),
                new AccessMode(ConfigData.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override
            public void action() {
                addProxyHostWizard();
            }
        };
        items.add(addProxyHostMenu);

        /* cluster wizard */
        final MyMenuItem clusterWizardItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.ClusterWizard"),
                           CLUSTER_ICON,
                           null,
                           new AccessMode(ConfigData.AccessType.ADMIN,
                                          AccessMode.ADVANCED),
                           new AccessMode(ConfigData.AccessType.ADMIN,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void action() {
                    final EditClusterDialog dialog =
                              new EditClusterDialog(getBrowser().getCluster());
                    dialog.showDialogs();
                }
            };
        items.add((UpdatableItem) clusterWizardItem);

        /* Rescan LVM */
        final MyMenuItem rescanLvmItem =
            new MyMenuItem(Tools.getString("DrbdInfo.RescanLvm"),
                           null, /* icon */
                           null,
                           new AccessMode(ConfigData.AccessType.OP,
                                          !AccessMode.ADVANCED),
                           new AccessMode(ConfigData.AccessType.OP,
                                          AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void action() {
                    getBrowser().updateHWInfo(Host.UPDATE_LVM);
                }
            };
        items.add((UpdatableItem) rescanLvmItem);


        /* view log */
        final MyMenuItem viewLogMenu = new MyMenuItem(
                           Tools.getString("ClusterBrowser.Drbd.ViewLogs"),
                           LOGFILE_ICON,
                           null,
                           new AccessMode(ConfigData.AccessType.RO, false),
                           new AccessMode(ConfigData.AccessType.RO, false)) {

            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                return null;
            }

            @Override
            public void action() {
                hidePopup();
                DrbdLogs l = new DrbdLogs(getCluster(), ALL_LOGS_PATTERN);
                l.showDialog();
            }
        };
        items.add(viewLogMenu);
        return items;
    }

    private void addProxyHostWizard() {
        final Host proxyHost = new Host();
        proxyHost.setCluster(getCluster());
        final ProxyHostWizard w = new ProxyHostWizard(proxyHost, null);
        w.showDialogs();
    }
}
