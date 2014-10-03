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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.tree.DefaultMutableTreeNode;
import lcmc.Exceptions;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.configs.AppDefaults;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.drbd.domain.DRBDtestData;
import lcmc.drbd.domain.DrbdProxy;
import lcmc.drbd.domain.DrbdXml;
import lcmc.drbd.domain.DrbdXml.HostProxy;
import lcmc.drbd.domain.DrbdResource;
import lcmc.drbd.domain.NetInterface;
import lcmc.common.ui.Browser;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.host.ui.HostBrowser;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.Info;
import lcmc.cluster.ui.resource.NetInfo;
import lcmc.crm.ui.resource.ServiceInfo;
import lcmc.cluster.ui.widget.Check;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.ui.utils.ComponentWithTest;
import lcmc.drbd.service.DRBD;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.common.ui.utils.WidgetListener;

/**
 * this class holds info data, menus and configuration
 * for a drbd resource.
 */
@Named
public class ResourceInfo extends AbstractDrbdInfo {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceInfo.class);
    static final String DRBD_RES_PARAM_NAME = "name";
    private static final String SECTION_PROXY = "proxy";
    private static final String SECTION_PROXY_PORTS = Tools.getString("ResourceInfo.ProxyPorts");
    private static final Value PROXY_DEFAULT_PROTOCOL = DrbdXml.PROTOCOL_A;
    private static final Value PROXY_DEFAULT_PING_TIMEOUT = new StringValue("100");

    /** Parse ip and host name. */
    public static final Pattern DRBDP_ADDRESS = Pattern.compile('^' + ProxyNetInfo.PROXY_PREFIX
                                                                + "(\\d+\\.\\d+\\.\\d+\\.\\d+)(\\s+\\S+\\s+(\\S+))$");

    public static final String FAMILY_SDP = "sdp";
    public static final String FAMILY_SSOCKS = "ssocks";
    private final Set<VolumeInfo> drbdVolumes = new LinkedHashSet<VolumeInfo>();
    /** Cache for getInfoPanel method. */
    private JComponent infoPanel = null;
    /** Whether the meta-data has to be created or not. */
    private boolean haveToCreateMD = false;
    private Map<Host, Widget> addressComboBoxHash = new HashMap<Host, Widget>();
    private Map<Host, Widget> insideIpComboBoxHash = new HashMap<Host, Widget>();
    private Map<Host, Widget> outsideIpComboBoxHash = new HashMap<Host, Widget>();
    private Map<Host, Widget> addressComboBoxHashWizard = new HashMap<Host, Widget>();
    private Map<Host, Widget> insideIpComboBoxHashWizard = new HashMap<Host, Widget>();
    private Map<Host, Widget> outsideIpComboBoxHashWizard = new HashMap<Host, Widget>();
    private final Map<Host, Value> savedHostAddresses = new HashMap<Host, Value>();
    private final Map<Host, Value> savedInsideIps = new HashMap<Host, Value>();
    private final Map<Host, Value> savedOutsideIps = new HashMap<Host, Value>();
    /** Saved port, that is the same for both hosts. */
    private Value savedPort = null;
    /** Saved proxy inside port, that is the same for both hosts. */
    private Value savedInsidePort = null;
    /** Saved proxy outside port, that is the same for both hosts. */
    private Value savedOutsidePort = null;
    /** Port combo box. */
    private Widget portComboBox = null;
    /** Port combo box wizard. */
    private Widget portComboBoxWizard = null;
    /** Proxy inside port combo box. */
    private Widget insidePortComboBox = null;
    /** Proxy outside port combo box. */
    private Widget outsidePortComboBox = null;
    /** Proxy inside port combo box for wizard. */
    private Widget insidePortComboBoxWizard = null;
    /** Proxy outside port combo box for wizard. */
    private Widget outsidePortComboBoxWizard = null;
    /** Resync-after combobox. */
    private Widget resyncAfterParamWi = null;
    private ServiceInfo isUsedByCRM;
    private Set<Host> hosts;
    private final Collection<Host> selectedProxyHosts = new HashSet<Host>();
    private GlobalInfo globalInfo;
    @Inject
    private Application application;
    @Inject
    private ResourceMenu resourceMenu;
    @Inject
    private WidgetFactory widgetFactory;
    @Inject
    private Provider<ProxyNetInfo> proxyNetInfoProvider;
    @Inject
    private TreeMenuController treeMenuController;

    public void init(final String name, final Set<Host> hosts, final Browser browser) {
        super.init(name, browser);
        this.hosts = hosts;
        setResource(new DrbdResource(name));
        globalInfo = ((ClusterBrowser) browser).getGlobalInfo();
    }

    public void addDrbdVolume(final VolumeInfo drbdVolume) {
        drbdVolumes.add(drbdVolume);
    }

    String proxyConfig(final ProxyNetInfo proxyNetInfo) {
        final StringBuilder config = new StringBuilder(50);
        /*
                proxy on centos6-a.site {
                        inside 127.0.0.1:7788;
                        outside 192.168.133.191:7788;
                }
        */
        final Host host = proxyNetInfo.getProxyHost();
        final String insideIp = getIp(insideIpComboBoxHash.get(host).getValue());
        final String insidePort = insidePortComboBox.getStringValue();
        final String outsideIp = getIp(outsideIpComboBoxHash.get(host).getValue());
        final String outsidePort = outsidePortComboBox.getStringValue();
        config.append("\n\t\tproxy on ");
        config.append(host.getName());
        config.append(" {\n");
        config.append("\t\t\tinside ");
        config.append(getNetInterfaceWithPort(insideIp, insidePort));
        config.append(";\n\t\t\toutside ");
        config.append(getNetInterfaceWithPort(outsideIp, outsidePort));
        config.append(";\n\t\t}");
        return config.toString();
    }

    /** Creates and returns drbd config for resources. */
    String drbdResourceConfig(final Host configOnHost)
    throws Exceptions.DrbdConfigException, UnknownHostException {
        final StringBuilder config = new StringBuilder(50);
        config.append("resource ").append(getName()).append(" {\n");
        final String[] params = getBrowser().getDrbdXml().getSectionParams("resource");
        for (final String param : params) {
            final Value value = getComboBoxValue(param);
            if (Tools.areEqual(value, getParamDefault(param))) {
                continue;
            }
            config.append('\t');
            config.append(param);
            config.append('\t');
            config.append(value.getValueForConfigWithUnit());
            config.append(";\n");
        }
        if (params.length != 0) {
            config.append('\n');
        }
        /* section config */
        config.append(drbdSectionsConfig(configOnHost));
        /*
            <host name="alpha">
                <volume vnr="0">
                    <device minor="0"></device>
                    <disk>/dev/foo</disk>
                    <flexible-meta-disk>/dev/bar</flexible-meta-disk>
                </volume>
                <volume vnr="1">

                    <device minor="1"></device>
                    <disk>/dev/foo1</disk>
                    <flexible-meta-disk>/dev/bar1</flexible-meta-disk>
                </volume>
                <address family="ipv4" port="7793">192.168.23.21</address>
            </host>
        */
        final boolean volumesAvailable = configOnHost.hasVolumes();
        for (final Host host : getHosts()) {
            final Collection<String> volumeConfigs = new ArrayList<String>();
            for (final VolumeInfo dvi : drbdVolumes) {
                final String volumeConfig = dvi.drbdVolumeConfig(host, volumesAvailable);
                if (volumeConfig != null && !volumeConfig.isEmpty()) {
                    volumeConfigs.add(volumeConfig);
                }
            }
            if (!volumeConfigs.isEmpty()) {
                config.append("\ton ");
                config.append(host.getName());
                config.append(" {\n\t\t");
                config.append(Tools.join("\n\n\t\t", volumeConfigs));
                final Widget awi = addressComboBoxHash.get(host);
                final Widget pwi = portComboBox;
                if (awi != null && pwi != null) {
                    final Value o = awi.getValue();
                    LOG.debug1("drbdResourceConfig: host: " + host.getName() + " ni: " + o);
                    for (final NetInterface ni : host.getNetInterfacesWithBridges()) {
                        LOG.debug1("drbdResourceConfig: host: " + host.getName() + " nis: " + ni.getIp());
                    }
                    if (o == null) {
                        throw new Exceptions.DrbdConfigException("Address not defined in "
                                                                 + getCluster().getName()
                                                                 + " (" + getName() + ')');
                    }
                    final String ip = getIp(o);
                    config.append("\n\t\taddress\t\t");
                    config.append(getNetInterfaceWithPort(ip, pwi.getStringValue()));
                    config.append(';');

                    if (awi.getValue() instanceof ProxyNetInfo) {
                        config.append(proxyConfig((ProxyNetInfo) awi.getValue()));
                    } else if (isProxyAddress(awi.getValue())) {
                        final Matcher drbdpM = DRBDP_ADDRESS.matcher(awi.getValue().toString());
                        if (drbdpM.matches()) {
                            final String proxyIp = drbdpM.group(1);
                            final String hostName = drbdpM.group(3);
                            final Host proxyHost = getCluster().getProxyHostByName(hostName);
                            final ProxyNetInfo proxyNetInfo = proxyNetInfoProvider.get();
                            proxyNetInfo.init(
                                    "",
                                    new NetInterface("", proxyIp, null, false, NetInterface.AddressFamily.IPV4),
                                    getBrowser(),
                                    proxyHost);
                            config.append(proxyConfig(proxyNetInfo));
                        }
                    }
                }
                config.append("\n\t}\n");
            }
        } config.append('}');
        getDrbdResource().setCommited(true);
        return config.toString();
    }

    /** Clears info panel cache. */
    @Override
    public boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    @Override
    public JPanel getGraphicalView() {
        return getBrowser().getDrbdGraph().getGraphPanel();
    }

    @Override
    public String[] getParametersFromXML() {
        return getEnabledSectionParams(getBrowser().getDrbdXml().getParameters());
    }

    /**
     * Checks the new value of the parameter if it is conforms to its type
     * and other constraints.
     */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        if (DRBD_RES_PARAM_AFTER.equals(param) || DRBD_RES_PARAM_AFTER_8_3.equals(param)) {
            /* drbdsetup xml syncer says it should be numeric, but in
               /etc/drbd.conf it is not. */
            return true;
        }
        return getBrowser().getDrbdXml().checkParam(param, newValue);
    }

    @Override
    public Value getParamDefault(final String param) {
        final Value common = globalInfo.getParamSaved(param);
        if (common != null) {
            return common;
        }
        return getBrowser().getDrbdXml().getParamDefault(param);
    }

    @Override
    protected String getSection(final String param) {
        return getBrowser().getDrbdXml().getSection(param);
    }

    @Override
    protected String isEnabled(final String param) {
        if (getDrbdResource().isCommited() && DRBD_RES_PARAM_NAME.equals(param)) {
            return "";
        }
        return null;
    }

    @Override
    protected Widget createWidget(final String param, final String prefix, final int width) {
        final Widget paramWi;
        if (DRBD_RES_PARAM_NAME.equals(param)) {
            final Value resName;
            if (getParamSaved(DRBD_RES_PARAM_NAME) == null) {
                resName = getResource().getDefaultValue(DRBD_RES_PARAM_NAME);
            } else {
                resName = new StringValue(getResource().getName());
            }
            paramWi = widgetFactory.createInstance(
                                      Widget.GUESS_TYPE,
                                      resName,
                                      Widget.NO_ITEMS,
                                      "^\\S+$",
                                      width,
                                      Widget.NO_ABBRV,
                                      new AccessMode(getAccessType(param), isEnabledOnlyInAdvancedMode(param)),
                                      Widget.NO_BUTTON);
            paramWi.setEnabled(!getDrbdResource().isCommited());
            widgetAdd(param, prefix, paramWi);
        } else if (DRBD_RES_PARAM_AFTER.equals(param) || DRBD_RES_PARAM_AFTER_8_3.equals(param)) {
            final List<Value> l = new ArrayList<Value>();
            final Value defaultItem = getParamSaved(param);
            final Value di = new StringValue("-1", Tools.getString("ClusterBrowser.None"));
            l.add(di);
            final Map<String, ResourceInfo> drbdResHash = getBrowser().getDrbdResourceNameHash();
            for (final Map.Entry<String, ResourceInfo> drbdResEntry : drbdResHash.entrySet()) {
                final ResourceInfo r = drbdResEntry.getValue();
                ResourceInfo odri = r;
                boolean cyclicRef = false;
                while (true) {
                    final Value valueS = odri.getParamSaved(param);
                    if (valueS == null) {
                        break;
                    }
                    odri = drbdResHash.get(valueS.getValueForConfig());
                    if (odri == null) {
                        break;
                    }
                    if (odri == this) {
                        cyclicRef = true;
                    }
                }
                if (r != this && !cyclicRef) {
                    l.add(r);
                }
            }
            getBrowser().putDrbdResHash();
            resyncAfterParamWi = widgetFactory.createInstance(
                                      Widget.Type.COMBOBOX,
                                      defaultItem,
                                      l.toArray(new Value[l.size()]),
                                      Widget.NO_REGEXP,
                                      width,
                                      Widget.NO_ABBRV,
                                      new AccessMode(getAccessType(param), isEnabledOnlyInAdvancedMode(param)),
                                      Widget.NO_BUTTON);
            paramWi = resyncAfterParamWi;
            widgetAdd(param, prefix, paramWi);
        } else {
            paramWi = super.createWidget(param, prefix, width);
        }
        return paramWi;
    }

    /** Returns the DrbdResource object of this drbd resource. */
    DrbdResource getDrbdResource() {
        return (DrbdResource) getResource();
    }

    /** Applies changes that user made to the drbd resource fields. */
    public void apply(final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            final String[] params = getParametersFromXML();
            getBrowser().getDrbdResourceNameHash().remove(getName());
            getBrowser().putDrbdResHash();
            storeComboBoxValues(params);

            final String name = getParamSaved(DRBD_RES_PARAM_NAME).getValueForConfig();
            getDrbdResource().setName(name);
            setName(name);

            getBrowser().getDrbdResourceNameHash().put(name, this);
            getBrowser().putDrbdResHash();
            getBrowser().getDrbdGraph().repaint();
            globalInfo.setAllApplyButtons();
            globalInfo.reloadDRBDResourceComboBoxes();
            getResource().setNew(false);
        }
    }

    /** Set all apply buttons. */
    void setAllApplyButtons() {
        for (final VolumeInfo dvi : drbdVolumes) {
            dvi.setAllApplyButtons();
        }
        setApplyButtons(null, getParametersFromXML());
    }

    /** Returns panel with form to configure a drbd resource. */
    @Override
    public JComponent getInfoPanel() {
        application.isSwingThread();
        if (infoPanel != null) {
            infoPanelDone();
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
                final Map<Host, String> testOutput = new LinkedHashMap<Host, String>();
                try {
                    globalInfo.createConfigDryRun(testOutput);
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

        mainPanel.add(buttonPanel);

        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.LINE_END);

        /* resource name */
        getResource().setValue(DRBD_RES_PARAM_NAME, new StringValue(getDrbdResource().getName()));
        final String[] params = getParametersFromXML();
        /* address combo boxes */
        addHostAddresses(optionsPanel,
                         application.getServiceLabelWidth(),
                         application.getServiceFieldWidth(),
                         false,
                         getApplyButton());
        enableSection(SECTION_PROXY, !DrbdProxy.PROXY, !WIZARD);
        addParams(optionsPanel,
                  params,
                  application.getDefaultSize("ClusterBrowser.DrbdResLabelWidth"),
                  application.getDefaultSize("ClusterBrowser.DrbdResFieldWidth"),
                  null);

        getApplyButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                LOG.debug1("getInfoPanel: BUTTON: apply");
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        application.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                getApplyButton().setEnabled(false);
                                getRevertButton().setEnabled(false);
                            }
                        });
                        getBrowser().drbdStatusLock();
                        try {
                            globalInfo.createDrbdConfigLive();
                            for (final Host h : getHosts()) {
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
        });

        getRevertButton().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    LOG.debug1("getInfoPanel: BUTTON: revert");
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getBrowser().drbdStatusLock();
                            try {
                                revert();
                            } finally {
                                getBrowser().drbdStatusUnlock();
                            }
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
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.PAGE_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(application.getDefaultSize("ClusterBrowser.DrbdResLabelWidth")
                                         + application.getDefaultSize("ClusterBrowser.DrbdResFieldWidth") + 4));
        newPanel.add(new JScrollPane(mainPanel));
        infoPanel = newPanel;
        setProxyPanels(!WIZARD);
        infoPanelDone();
        return infoPanel;
    }

    /** Remove drbd volume from all hashes. */
    public void removeDrbdVolumeFromHashes(final VolumeInfo drbdVolume) {
        getBrowser().getDrbdDeviceHash().remove(drbdVolume.getDevice());
        getBrowser().putDrbdDevHash();
        for (final BlockDevInfo bdi : drbdVolume.getBlockDevInfos()) {
            bdi.removeFromDrbd();
        }
    }

    /** Returns string of the drbd resource. */
    @Override
    public String toString() {
        String name = getName();
        if (name == null || name.isEmpty()) {
            name = Tools.getString("ClusterBrowser.DrbdResUnconfigured");
        }
        return "drbd: " + name;
    }

    /** Returns common file systems. */
    public Value[] getCommonFileSystems(final Value defaultValue) {
        return getBrowser().getCommonFileSystems(defaultValue);
    }

    /** Returns whether the specified host has this drbd resource. */
    boolean resourceInHost(final Host host) {
        for (final VolumeInfo dvi : drbdVolumes) {
            for (final BlockDevInfo bdi : dvi.getBlockDevInfos()) {
                if (bdi.getHost() == host) {
                    return true;
                }
            }
        }
        return false;
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


    /** Set networks addresses and port. */
    private void setNetworkParameters(final DrbdXml dxml) {
        Value hostPort = null;
        final boolean infoPanelOk = infoPanel != null;
        for (final Host host : getHosts()) {
            String ha = dxml.getVirtualInterface(host.getName(), getName());
            final String family = dxml.getVirtualInterfaceFamily(host.getName(), getName());
            if (FAMILY_SDP.equals(family) || FAMILY_SSOCKS.equals(family)) {
                ha = family + ' ' + ha;
            }
            final Value hostAddress = new StringValue(ha);
            final Value hp = new StringValue(dxml.getVirtualInterfacePort(host.getName(), getName()));
            if (hostPort != null && !Tools.areEqual(hostPort, hp)) {
                LOG.appWarning("setNetworkParameters: more ports in " + getName() + ' ' + hp + ' ' + hostPort);
            }
            hostPort = hp;
            final Value savedAddress = savedHostAddresses.get(host);
            if (!Tools.areEqual(hostAddress, savedAddress)) {
                if (hostAddress.isNothingSelected()) {
                    savedHostAddresses.remove(host);
                } else {
                    savedHostAddresses.put(host, hostAddress);
                }
                if (infoPanelOk) {
                    final Widget wi = addressComboBoxHash.get(host);
                    if (wi != null) {
                        wi.setValueAndWait(hostAddress);
                    }
                }
            }
        }

        /* set port */
        if (!Tools.areEqual(hostPort, savedPort)) {
            savedPort = hostPort;
            for (final Host host : getHosts()) {
                host.getBrowser().getUsedPorts().add(savedPort.getValueForConfig());
            }
            if (infoPanelOk) {
                final Widget wi = portComboBox;
                if (wi != null) {
                    wi.setValueAndWait(hostPort);
                }
            }
        }
    }

    /** Set proxy parameters. */
    private void setProxyParameters(final DrbdXml dxml) {
        Value hostInsidePort = null;
        Value hostOutsidePort = null;
        final boolean infoPanelOk = infoPanel != null;
        final Set<Host> configuredProxyHosts = getConfiguredProxyHosts();
        for (final Host host : getHosts()) {
            final HostProxy hostProxy = dxml.getHostProxy(host.getName(), getName());
            final Host proxyHost;
            final Value insideIp;
            final Value outsideIp;
            final Value insidePort;
            final Value outsidePort;
            if (hostProxy != null) {
                insideIp = hostProxy.getInsideIp();
                outsideIp = hostProxy.getOutsideIp();
                insidePort = hostProxy.getInsidePort();
                outsidePort = hostProxy.getOutsidePort();
                proxyHost = getCluster().getProxyHostByName(hostProxy.getProxyHostName());
            } else {
                insideIp = new StringValue();
                outsideIp = new StringValue();
                insidePort = new StringValue();
                outsidePort = new StringValue();
                proxyHost = host;
            }

            if (insidePort != null && hostInsidePort != null && !Tools.areEqual(insidePort, hostInsidePort)) {
                LOG.appWarning("setProxyParameters: multiple proxy inside ports in "
                               + getName()
                               + ' '
                               + insidePort
                               + ' '
                               + hostInsidePort);
            }
            hostInsidePort = insidePort;

            if (outsidePort != null && hostOutsidePort != null && !Tools.areEqual(outsidePort, hostOutsidePort)) {
                LOG.appWarning("setProxyParameters: multiple proxy outside ports in "
                               + getName()
                               + ' '
                               + outsidePort
                               + ' '
                               + hostOutsidePort);
            }
            hostOutsidePort = outsidePort;

            final Value savedInsideIp = savedInsideIps.get(proxyHost);
            if (!Tools.areEqual(insideIp, savedInsideIp)) {
                if (insideIp == null || insideIp.isNothingSelected()) {
                    savedInsideIps.remove(proxyHost);
                } else {
                    savedInsideIps.put(proxyHost, insideIp);
                }
                if (infoPanelOk) {
                    final Widget wi = insideIpComboBoxHash.get(proxyHost);
                    if (wi != null) {
                        wi.setValue(insideIp);
                    }
                }
            }

            final Value savedOutsideIp = savedOutsideIps.get(proxyHost);
            if (!Tools.areEqual(outsideIp, savedOutsideIp)) {
                if (outsideIp == null || outsideIp.isNothingSelected()) {
                    savedOutsideIps.remove(proxyHost);
                } else {
                    savedOutsideIps.put(proxyHost, outsideIp);
                }
                if (infoPanelOk) {
                    final Widget wi = outsideIpComboBoxHash.get(proxyHost);
                    if (wi != null) {
                        wi.setValue(outsideIp);
                    }
                }
            }
        }

        /* set proxy ports */
        if (!Tools.areEqual(hostInsidePort, savedInsidePort)) {
            savedInsidePort = hostInsidePort;
            for (final Host host : getHosts()) {
                host.getBrowser().getUsedPorts().add(hostInsidePort.getValueForConfig());
            }
            if (infoPanelOk) {
                final Widget wi = insidePortComboBox;
                if (wi != null) {
                    wi.setValueAndWait(hostInsidePort);
                }
            }
        }

        if (!Tools.areEqual(hostOutsidePort, savedOutsidePort)) {
            savedOutsidePort = hostOutsidePort;
            for (final Host host : configuredProxyHosts) {
                host.getBrowser().getUsedProxyPorts().add(hostOutsidePort.getValueForConfig());
            }
            if (infoPanelOk) {
                final Widget wi = outsidePortComboBox;
                if (wi != null) {
                    wi.setValueAndWait(hostOutsidePort);
                }
            }
        }
    }

    public void setParameters() {
        application.isSwingThread();
        getDrbdResource().setCommited(true);
        final DrbdXml dxml = getBrowser().getDrbdXml();
        final String resName = getResource().getName();
        for (final String sectionString : dxml.getSections()) {
            /* remove -options */
            final String section = sectionString.replaceAll("-options$", "");
            for (final String param : dxml.getSectionParams(sectionString)) {
                Value value = dxml.getConfigValue(resName, section, param);
                final Value defaultValue = getParamDefault(param);
                final Value oldValue = getParamSaved(param);
                if (value == null || value.isNothingSelected()) {
                    value = defaultValue;
                }
                final Widget wi = getWidget(param, null);
                if (!Tools.areEqual(value, oldValue)) {
                    getResource().setValue(param, value);
                    if (wi != null) {
                        wi.setValueAndWait(value);
                    }
                }
            }
        }
        setNetworkParameters(dxml);
        setProxyParameters(dxml);
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    @Override
    public Check checkResourceFields(final String param, final String[] params) {
        return checkResourceFields(param, params, false);
    }

    /** Check whether it's a valid port. */
    private boolean checkPort(final String port) {
        if (!Tools.isNumber(port)) {
            return false;
        }
        final long p = Long.parseLong(port);
        return p >= 0 && p < 65536;
    }

    /** Check port. */
    private boolean checkPortCorrect() {
        /* port */
        final Widget pwi = portComboBox;
        if (pwi == null) {
            return false;
        }
        final String port = pwi.getStringValue();
        final Widget pwizardWi = portComboBoxWizard;
        boolean correct = true;
        if (checkPort(port)) {
            pwi.setBackground(null, savedPort, true);
            if (pwizardWi != null) {
                pwizardWi.setBackground(null, savedPort, true);
            }
        } else {
            correct = false;
            pwi.wrongValue();
            if (pwizardWi != null) {
                pwizardWi.wrongValue();
            }
        }
        return correct;
    }

    private boolean checkAddressCorrect() {
        final Map<Host, Widget> addressComboBoxHashClone = new HashMap<Host, Widget>(addressComboBoxHash);
        boolean correct = true;
        for (final Map.Entry<Host, Widget> hostWidgetEntry : addressComboBoxHashClone.entrySet()) {
            final Widget wi = hostWidgetEntry.getValue();
            final Widget wizardWi = addressComboBoxHashWizard.get(hostWidgetEntry.getKey());
            if (wi.getValue() == null || wi.getValue().isNothingSelected()) {
                correct = false;
                wi.wrongValue();
                if (wizardWi != null) {
                    wizardWi.wrongValue();
                }
            } else {
                wi.setBackground(null, savedHostAddresses.get(hostWidgetEntry.getKey()), true);
                if (wizardWi != null) {
                    wizardWi.setBackground(null, savedHostAddresses.get(hostWidgetEntry.getKey()), true);
                }
            }
        }
        return correct;
    }

    private boolean checkProxyPortCorrect(final Widget pwi, final Widget pWizardWi, final Value savedPort) {
        /* proxy ports */
        if (pwi == null) {
            return false;
        }
        final Value port = pwi.getValue();
        boolean correct = true;
        if (checkPort(port.getValueForConfig())) {
            pwi.setBackground(null, savedPort, true);
            if (pWizardWi != null) {
                pWizardWi.setBackground(null, savedPort, true);
            }
        } else {
            correct = false;
            pwi.wrongValue();
            if (pWizardWi != null) {
                pWizardWi.wrongValue();
            }
        }
        return correct;
    }

    /** Check proxy inside Ip. */
    private boolean checkProxyInsideIpCorrect() {
        final Map<Host, Widget> insideIpComboBoxHashClone = new HashMap<Host, Widget>(insideIpComboBoxHash);
        boolean correct = true;
        for (final Host host : getHosts()) {
            final Host proxyHost = getProxyHost(host, !WIZARD);
            if (proxyHost == null) {
                continue;
            }

            final Widget wi = insideIpComboBoxHashClone.get(proxyHost);
            final Widget wizardWi = insideIpComboBoxHashWizard.get(proxyHost);
            if (wi.getValue() == null) {
                correct = false;
                wi.wrongValue();
                if (wizardWi != null) {
                    wizardWi.wrongValue();
                }
            } else {
                final Value defaultInsideIp = getDefaultInsideIp(proxyHost);
                Value savedInsideIp = savedInsideIps.get(proxyHost);
                if (savedInsideIp == null) {
                    savedInsideIp = defaultInsideIp;
                }
                wi.setBackground(defaultInsideIp, savedInsideIp, true);
                if (wizardWi != null) {
                    wizardWi.setBackground(defaultInsideIp, savedInsideIp, true);
                }
            }
        }
        return correct;
    }

    /** Check proxy outside ip. */
    private boolean checkProxyOutsideIpCorrect() {
        final Map<Host, Widget> outsideIpComboBoxHashClone = new HashMap<Host, Widget>(outsideIpComboBoxHash);
        boolean correct = true;
        for (final Host host : getHosts()) {
            final Host proxyHost = getProxyHost(host, !WIZARD);
            if (proxyHost == null) {
                continue;
            }
            final Widget wi = outsideIpComboBoxHashClone.get(proxyHost);
            final Widget wizardWi = outsideIpComboBoxHashWizard.get(proxyHost);
            if (wi.getValue() == null) {
                correct = false;
                wi.wrongValue();
                if (wizardWi != null) {
                    wizardWi.wrongValue();
                }
            } else {
                wi.setBackground(null, savedOutsideIps.get(proxyHost), true);
                if (wizardWi != null) {
                    wizardWi.setBackground(null, savedOutsideIps.get(proxyHost), true);
                }
            }
        }
        return correct;
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    Check checkResourceFields(final String param, final String[] params, final boolean fromDrbdInfo) {
        final List<String> incorrect = new ArrayList<String>();
        final List<String> changed = new ArrayList<String>();
        final DrbdXml dxml = getBrowser().getDrbdXml();
        final Check check = new Check(incorrect, changed);
        if (dxml != null && dxml.isDrbdDisabled()) {
            incorrect.add("DRBD is disabled");
        }
        for (final VolumeInfo dvi : drbdVolumes) {
            check.addCheck(dvi.checkResourceFields(param, dvi.getParametersFromXML(), fromDrbdInfo, true));
        }

        if (!checkPortCorrect()) {
            incorrect.add("port");
        }

        if (!checkAddressCorrect()) {
            incorrect.add("address");
        }

        if (checkHostAddressesFieldsChanged()) {
            changed.add("host address");
        }

        if (isSectionEnabled(SECTION_PROXY)) {
            if (!checkProxyPortCorrect(insidePortComboBox, insidePortComboBoxWizard, savedInsidePort)) {
                incorrect.add("proxy inside port");
            }

            if (!checkProxyPortCorrect(outsidePortComboBox, outsidePortComboBoxWizard, savedOutsidePort)) {
                incorrect.add("proxy outside port");
            }

            if (!checkProxyInsideIpCorrect()) {
                incorrect.add("proxy inside IP");
            }

            if (!checkProxyOutsideIpCorrect()) {
                incorrect.add("proxy outside IP");
            }

            if (checkProxyFieldsChanged()) {
                changed.add("proxy");
            }
        }

        check.addCheck(super.checkResourceFields(param, params));
        return check;
    }

    /** Revert all values. */
    @Override
    public void revert() {
        super.revert();
        for (final VolumeInfo dvi : drbdVolumes) {
            for (final BlockDevInfo bdi : dvi.getBlockDevInfos()) {
                if (bdi != null) {
                    bdi.revert();
                }
            }
        }

        final Map<Host, Widget> addressComboBoxHashClone = new HashMap<Host, Widget>(addressComboBoxHash);
        for (final Map.Entry<Host, Widget> hostWidgetEntry : addressComboBoxHashClone.entrySet()) {
            final Widget wi = hostWidgetEntry.getValue();
            final Value haSaved = savedHostAddresses.get(hostWidgetEntry.getKey());
            if (!Tools.areEqual(wi.getValue(), haSaved)) {
                final Widget wizardWi = addressComboBoxHashWizard.get(hostWidgetEntry.getKey());
                if (wizardWi == null) {
                    wi.setValue(haSaved);
                } else {
                    wizardWi.setValue(haSaved);
                }
            }
        }

        final Widget pwi = portComboBox;
        if (!Tools.areEqual(pwi.getValue(), savedPort)) {
            final Widget wizardWi = portComboBoxWizard;
            if (wizardWi == null) {
                pwi.setValue(savedPort);
            } else {
                wizardWi.setValue(savedPort);
            }
        }

        /* proxy */
        final Map<Host, Widget> insideIpComboBoxHashClone = new HashMap<Host, Widget>(insideIpComboBoxHash);
        for (final Map.Entry<Host, Widget> hostWidgetEntry : insideIpComboBoxHashClone.entrySet()) {
            final Widget wi = hostWidgetEntry.getValue();
            Value ipSaved = savedInsideIps.get(hostWidgetEntry.getKey());
            if (ipSaved == null || ipSaved.isNothingSelected()) {
                ipSaved = getDefaultInsideIp(hostWidgetEntry.getKey());
            }
            if (!Tools.areEqual(wi.getValue(), ipSaved)) {
                final Widget wizardWi = insideIpComboBoxHashWizard.get(hostWidgetEntry.getKey());
                if (wizardWi == null) {
                    wi.setValue(ipSaved);
                } else {
                    wizardWi.setValue(ipSaved);
                }
            }
        }

        final Widget ipwi = insidePortComboBox;
        if (!Tools.areEqual(ipwi, savedInsidePort)) {
            final Widget wizardWi = insidePortComboBoxWizard;
            if (wizardWi == null) {
                ipwi.setValue(savedInsidePort);
            } else {
                wizardWi.setValue(savedInsidePort);
            }
        }

        final Map<Host, Widget> outsideIpComboBoxHashClone = new HashMap<Host, Widget>(outsideIpComboBoxHash);
        for (final Map.Entry<Host, Widget> hostWidgetEntry : outsideIpComboBoxHashClone.entrySet()) {
            final Widget wi = hostWidgetEntry.getValue();
            final Value ipSaved = savedOutsideIps.get(hostWidgetEntry.getKey());
            if (!Tools.areEqual(wi.getValue(), ipSaved)) {
                final Widget wizardWi = outsideIpComboBoxHashWizard.get(hostWidgetEntry.getKey());
                if (wizardWi == null) {
                    wi.setValue(ipSaved);
                } else {
                    wizardWi.setValue(ipSaved);
                }
            }
        }

        final Widget opwi = outsidePortComboBox;
        if (!Tools.areEqual(opwi, savedOutsidePort)) {
            final Widget wizardWi = outsidePortComboBoxWizard;
            if (wizardWi == null) {
                opwi.setValue(savedOutsidePort);
            } else {
                wizardWi.setValue(savedOutsidePort);
            }
        }
    }

    /** Sets if dialog was started. It disables the apply button. */
    @Override
    public void setDialogStarted(final boolean dialogStarted) {
        for (final VolumeInfo dvi : drbdVolumes) {
            for (final BlockDevInfo bdi : dvi.getBlockDevInfos()) {
                if (bdi != null) {
                    bdi.setDialogStarted(dialogStarted);
                }
            }
        }
        super.setDialogStarted(dialogStarted);
    }

    /** Update panels and fields of all volumes and block devices. */
    public void updateAllVolumes() {
        for (final VolumeInfo dvi : drbdVolumes) {
            for (final BlockDevInfo bdi : dvi.getBlockDevInfos()) {
                if (bdi != null) {
                    bdi.checkResourceFields(null, bdi.getParametersFromXML()).isChanged();
                    bdi.updateAdvancedPanels();
                }
            }
        }
    }

    /** Returns the last volume number + 1. */
    public String getAvailVolumeNumber() {
        int maxNr = -1;
        for (final VolumeInfo dvi : drbdVolumes) {
            final String nrString = dvi.getName();
            if (Tools.isNumber(nrString)) {
                final int nr = Integer.parseInt(nrString);
                if (nr > maxNr) {
                    maxNr = nr;
                }
            }
        }
        return Integer.toString(maxNr + 1);
    }

    /** Creates host address combo boxes with labels, one per host. */
    public void addHostAddresses(final JPanel optionsPanel,
                                 final int leftWidth,
                                 final int rightWidth,
                                 final boolean wizard,
                                 final MyButton thisApplyButton) {
        final Map<Host, Widget> newAddressComboBoxHash = new HashMap<Host, Widget>();
        final Map<Host, Widget> newInsideIpComboBoxHash = new HashMap<Host, Widget>();
        final Map<Host, Widget> newOutsideIpComboBoxHash = new HashMap<Host, Widget>();
        final JPanel panel = getParamPanel(Tools.getString("ResourceInfo.HostAddresses"));
        panel.setLayout(new SpringLayout());
        for (final Host host : getHosts()) {
            final Value haSaved = savedHostAddresses.get(host);
            final Widget wi = widgetFactory.createInstance(Widget.Type.COMBOBOX,
                                                           haSaved,
                                                           getNetInterfacesWithProxies(host.getBrowser()),
                                                           Widget.NO_REGEXP,
                                                           rightWidth,
                                                           Widget.NO_ABBRV,
                                                           new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                                           Widget.NO_BUTTON);
            newAddressComboBoxHash.put(host, wi);

        }

        /* host addresses combo boxes */
        int rows = 0;
        for (final Host host : getHosts()) {
            final Widget wi = newAddressComboBoxHash.get(host);
            final String addr = Tools.getString("ResourceInfo.AddressOnHost") + host.getName();
            final JLabel label = new JLabel(addr);
            wi.setLabel(label, addr);
            addField(panel, label, wi.getComponent(), leftWidth, rightWidth, 0);
            wi.setToolTipText(getToolTipText(null, wi));
            rows++;
        }

        /* Port */
        Value defaultPort = savedPort;
        final int defaultPortInt;
        if (defaultPort == null || defaultPort.isNothingSelected()) {
            defaultPortInt = getLowestUnusedPort();
            defaultPort = new StringValue(Integer.toString(defaultPortInt));
        } else {
            defaultPortInt = Integer.parseInt(defaultPort.getValueForConfig());
        }
        final List<Value> drbdPorts = getPossibleDrbdPorts(defaultPortInt);

        final Widget pwi = widgetFactory.createInstance(Widget.Type.COMBOBOX,
                                                        defaultPort,
                                                        drbdPorts.toArray(new Value[drbdPorts.size()]),
                                                        "^\\d*$",
                                                        leftWidth,
                                                        Widget.NO_ABBRV,
                                                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                                        Widget.NO_BUTTON);
        pwi.setAlwaysEditable(true);
        final String port = Tools.getString("ResourceInfo.NetInterfacePort");
        final JLabel label = new JLabel(port);
        addField(panel, label, pwi.getComponent(), leftWidth, rightWidth, 0);
        pwi.setLabel(label, port);
        if (wizard) {
            portComboBoxWizard = pwi;
            portComboBox.setValueAndWait(defaultPort);
        } else {
            portComboBox = pwi;
        }
        rows++;

        SpringUtilities.makeCompactGrid(panel, rows, 2, /* rows, cols */
                                        1, 1,           /* initX, initY */
                                        1, 1);          /* xPad, yPad */
        optionsPanel.add(panel);

        addProxyPorts(optionsPanel, leftWidth, rightWidth, wizard);

        addProxyIps(optionsPanel, leftWidth, rightWidth, wizard, newInsideIpComboBoxHash, newOutsideIpComboBoxHash);

        addHostAddressListener(wizard, thisApplyButton, newAddressComboBoxHash, addressComboBoxHash);

        addPortListeners(wizard, thisApplyButton, portComboBoxWizard, portComboBox);

        addPortListeners(wizard, thisApplyButton, insidePortComboBoxWizard, insidePortComboBox);

        addPortListeners(wizard, thisApplyButton, outsidePortComboBoxWizard, outsidePortComboBox);

        addIpListeners(wizard, thisApplyButton, newInsideIpComboBoxHash, insideIpComboBoxHash);

        addIpListeners(wizard, thisApplyButton, newOutsideIpComboBoxHash, outsideIpComboBoxHash);
        if (wizard) {
            addressComboBoxHashWizard = newAddressComboBoxHash;
            insideIpComboBoxHashWizard = newInsideIpComboBoxHash;
            outsideIpComboBoxHashWizard = newOutsideIpComboBoxHash;
        } else {
            addressComboBoxHash = newAddressComboBoxHash;
            insideIpComboBoxHash = newInsideIpComboBoxHash;
            outsideIpComboBoxHash = newOutsideIpComboBoxHash;
        }
    }

    /** Add proxy inside and outside ports. */
    private void addProxyPorts(final JPanel optionsPanel,
                               final int leftWidth,
                               final int rightWidth,
                               final boolean wizard) {
        final JPanel panel = getParamPanel(SECTION_PROXY_PORTS);
        addSectionPanel(SECTION_PROXY_PORTS, wizard, panel);
        enableSection(SECTION_PROXY_PORTS, !DrbdProxy.PROXY, wizard);
        panel.setLayout(new SpringLayout());
        panel.setBackground(AppDefaults.LIGHT_ORANGE);
        /* inside port */
        final int insideDefaultPortInt = getDefaultInsidePort();
        final String insideDefaultPort = Integer.toString(insideDefaultPortInt);
        final List<Value> insideDrbdPorts = getPossibleDrbdPorts(insideDefaultPortInt);
        final Widget insidePortWi = widgetFactory.createInstance(
                                                        Widget.Type.COMBOBOX,
                                                        new StringValue(insideDefaultPort),
                                                        insideDrbdPorts.toArray(new Value[insideDrbdPorts.size()]),
                                                        "^\\d*$",
                                                        leftWidth,
                                                        Widget.NO_ABBRV,
                                                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                                        Widget.NO_BUTTON);
        insidePortWi.setAlwaysEditable(true);

        final JLabel insidePortLabel = new JLabel(Tools.getString("ResourceInfo.ProxyInsidePort"));
        insidePortWi.setLabel(insidePortLabel, Tools.getString("ResourceInfo.ProxyInsidePort.ToolTip"));
        addField(panel, insidePortLabel, insidePortWi.getComponent(), leftWidth, rightWidth, 0);
        if (wizard) {
            insidePortComboBoxWizard = insidePortWi;
        } else {
            insidePortComboBox = insidePortWi;
        }
        int rows = 0;
        rows++;

        /* outside port */
        Value outsideDefaultPort = savedOutsidePort;
        int outsideDefaultPortInt;
        if (outsideDefaultPort == null || outsideDefaultPort.isNothingSelected()) {
            outsideDefaultPortInt = getLowestUnusedProxyPort();
            if (outsideDefaultPortInt < insideDefaultPortInt - 1) {
                outsideDefaultPortInt = insideDefaultPortInt - 1;
            }
            outsideDefaultPort = new StringValue(Integer.toString(outsideDefaultPortInt));
        } else {
            outsideDefaultPortInt = Integer.parseInt(outsideDefaultPort.getValueForConfig());
        }
        final List<Value> outsideDrbdPorts = getPossibleDrbdPorts(outsideDefaultPortInt);
        final Widget outsidePortWi = widgetFactory.createInstance(
                                                        Widget.Type.COMBOBOX,
                                                        outsideDefaultPort,
                                                        outsideDrbdPorts.toArray(new Value[outsideDrbdPorts.size()]),
                                                        "^\\d*$",
                                                        leftWidth,
                                                        Widget.NO_ABBRV,
                                                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                                        Widget.NO_BUTTON);
        outsidePortWi.setAlwaysEditable(true);

        final JLabel outsidePortLabel = new JLabel(Tools.getString("ResourceInfo.ProxyOutsidePort"));
        outsidePortWi.setLabel(outsidePortLabel, Tools.getString("ResourceInfo.ProxyOutsidePort.ToolTip"));
        addField(panel, outsidePortLabel, outsidePortWi.getComponent(), leftWidth, rightWidth, 0);
        if (wizard) {
            outsidePortComboBoxWizard = outsidePortWi;
        } else {
            outsidePortComboBox = outsidePortWi;
        }
        rows++;

        SpringUtilities.makeCompactGrid(panel, rows, 2, /* rows, cols */
                                        1, 1,           /* initX, initY */
                                        1, 1);          /* xPad, yPad */
        optionsPanel.add(panel);
    }

    /** Add inside and outside proxy ips. */
    private void addProxyIps(final JPanel optionsPanel,
                             final int leftWidth,
                             final int rightWidth,
                             final boolean wizard,
                             final Map<Host, Widget> newInsideIpComboBoxHash,
                             final Map<Host, Widget> newOutsideIpComboBoxHash) {
        final DrbdXml dxml = getBrowser().getDrbdXml();
        for (final Host pHost : new HashSet<Host>(getCluster().getProxyHosts())) {
            final String section = Tools.getString("ResourceInfo.Proxy") + pHost.getName();
            final JPanel sectionPanel = getParamPanel(section);
            addSectionPanel(section, wizard, sectionPanel);
            enableSection(section, !DrbdProxy.PROXY, wizard);
            sectionPanel.setBackground(AppDefaults.LIGHT_ORANGE);
            final Value[] proxyNetInterfaces = getNetInterfaces(pHost.getBrowser());
            /* inside ip */
            if (proxyNetInterfaces == null) {
                //TODO: just textfield
            }
            final Widget iIpWi = widgetFactory.createInstance(
                                        Widget.Type.COMBOBOX,
                                        Widget.NO_DEFAULT,
                                        proxyNetInterfaces,
                                        Widget.NO_REGEXP,
                                        rightWidth,
                                        Widget.NO_ABBRV,
                                        new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                        Widget.NO_BUTTON);
            iIpWi.setAlwaysEditable(!pHost.isConnected());
            newInsideIpComboBoxHash.put(pHost, iIpWi);

            final JLabel insideIpLabel = new JLabel(Tools.getString("ResourceInfo.ProxyInsideIp"));
            iIpWi.setLabel(insideIpLabel, Tools.getString("ResourceInfo.ProxyInsideIp.ToolTip"));
            final JPanel panel = new JPanel();
            addField(panel, insideIpLabel, iIpWi.getComponent(), leftWidth, rightWidth, 0);
            panel.setBackground(AppDefaults.LIGHT_ORANGE);
            panel.setLayout(new SpringLayout());
            sectionPanel.add(panel);

            /* outside ip */
            final Widget oIpWi = widgetFactory.createInstance(
                                                    Widget.Type.COMBOBOX,
                                                    Widget.NO_DEFAULT,
                                                    proxyNetInterfaces,
                                                    Widget.NO_REGEXP,
                                                    rightWidth,
                                                    Widget.NO_ABBRV,
                                                    new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                                                    Widget.NO_BUTTON);
            oIpWi.setAlwaysEditable(!pHost.isConnected());
            newOutsideIpComboBoxHash.put(pHost, oIpWi);

            final JLabel outsideIpLabel = new JLabel(Tools.getString("ResourceInfo.ProxyOutsideIp"));
            oIpWi.setLabel(outsideIpLabel, Tools.getString("ResourceInfo.ProxyOutsideIp.ToolTip"));
            addField(panel, outsideIpLabel, oIpWi.getComponent(), leftWidth, rightWidth, 0);
            SpringUtilities.makeCompactGrid(panel, 2, 2, /* rows, cols */
                                            1, 1,           /* initX, initY */
                                            1, 1);          /* xPad, yPad */
            optionsPanel.add(sectionPanel);
        }

        for (final Host host : getHosts()) {
            final HostProxy hostProxy = dxml.getHostProxy(host.getName(), getName());
            Value insideIpSaved = null;
            Value outsideIpSaved = null;
            final Host proxyHost;
            if (hostProxy != null) {
                insideIpSaved = hostProxy.getInsideIp();
                outsideIpSaved = hostProxy.getOutsideIp();
                proxyHost = getCluster().getProxyHostByName(hostProxy.getProxyHostName());
            } else {
                proxyHost = host;
            }
            final Widget iIpWi = newInsideIpComboBoxHash.get(proxyHost);
            iIpWi.setValueAndWait(insideIpSaved);
            iIpWi.setToolTipText(getToolTipText(null, iIpWi));
            final Widget oIpWi = newOutsideIpComboBoxHash.get(proxyHost);
            oIpWi.setValueAndWait(outsideIpSaved);
            oIpWi.setToolTipText(getToolTipText(null, oIpWi));
        }
    }

    private Value[] getNetInterfaces(final HostBrowser hostBrowser) {
        final List<Value> list = new ArrayList<Value>();

        list.add(null);
        if (hostBrowser == null) {
            throw new RuntimeException("getNetInterfaces: hostBrowser is null");
        }
        if (hostBrowser.getNetInterfacesNode() != null) {
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> e =
                                 hostBrowser.getNetInterfacesNode().children();

            while (e.hasMoreElements()) {
                final Value i = (Value) e.nextElement().getUserObject();
                list.add(i);
            }
        }
        return list.toArray(new Value[list.size()]);
    }

    private Value[] getNetInterfacesWithProxies(final HostBrowser hostBrowser) {
        final List<Value> list = new ArrayList<Value>();

        list.add(new StringValue());
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> n = hostBrowser.getNetInterfacesNode().children();

        while (n.hasMoreElements()) {
            final Value i = (Value) n.nextElement().getUserObject();
            list.add(i);
        }

        /* the same host */
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> np = hostBrowser.getNetInterfacesNode().children();

        while (np.hasMoreElements()) {
            final NetInfo netInfo = (NetInfo) np.nextElement().getUserObject();
            final ProxyNetInfo proxyNetInfo = proxyNetInfoProvider.get();
            proxyNetInfo.init(netInfo, hostBrowser, hostBrowser.getHost());
            list.add(proxyNetInfo);
        }

        /* other nodes */
        for (final Host h : getCluster().getProxyHosts()) {
            if (h == hostBrowser.getHost()) {
                continue;
            }
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> nph = hostBrowser.getNetInterfacesNode().children();
            if (nph.hasMoreElements()) {
                while (nph.hasMoreElements()) {
                    final NetInfo netInfo = (NetInfo) nph.nextElement().getUserObject();
                    if (netInfo.isLocalHost()) {
                        continue;
                    }
                    final ProxyNetInfo proxyNetInfo = proxyNetInfoProvider.get();
                    proxyNetInfo.init(netInfo, hostBrowser, h);
                    list.add(proxyNetInfo);
                }
            }
        }
        return list.toArray(new Value[list.size()]);
    }

    /** Returns true if some of the addresses have changed. */
    private boolean checkHostAddressesFieldsChanged() {
        boolean changed = false;
        for (final Host host : getHosts()) {
            final Widget wi = addressComboBoxHash.get(host);
            if (wi == null) {
                continue;
            }
            final Value haSaved = savedHostAddresses.get(host);
            final Value value = wi.getValue();
            if (!Tools.areEqual(haSaved, value)) {
                changed = true;
            }
        }
        /* port */
        final Widget pwi = portComboBox;
        if (pwi != null && !Tools.areEqual(savedPort, pwi.getValue())) {
            changed = true;
        }
        return changed;
    }

    /** Returns true if some of proxy fields have changed. */
    private boolean checkProxyFieldsChanged() {
        boolean changed = false;
        /* ports */
        if (insidePortComboBox != null && !Tools.areEqual(savedInsidePort, insidePortComboBox.getValue())) {
            changed = true;
        }

        if (outsidePortComboBox != null && !Tools.areEqual(savedOutsidePort, outsidePortComboBox.getValue())) {
            changed = true;
        }

        /* ips */
        final DrbdXml dxml = getBrowser().getDrbdXml();
        for (final Host host : getHosts()) {
            final Host proxyHost = getProxyHost(host, !WIZARD);
            if (proxyHost == null) {
                continue;
            }
            final Widget wi = insideIpComboBoxHash.get(proxyHost);
            if (wi == null) {
                continue;
            }
            Value ipSaved = savedInsideIps.get(proxyHost);
            final Value defaultInsideIp = getDefaultInsideIp(proxyHost);
            if (ipSaved == null) {
                ipSaved = defaultInsideIp;
            }
            final Value value = wi.getValue();
            if (!Tools.areEqual(ipSaved, value)) {
                changed = true;
            }
        }

        for (final Host host : getHosts()) {
            final HostProxy hostProxy = dxml.getHostProxy(host.getName(), getName());
            final Host proxyHost;
            if (hostProxy != null) {
                proxyHost = getCluster().getProxyHostByName(hostProxy.getProxyHostName());
            } else {
                proxyHost = host;
            }
            final Widget wi = outsideIpComboBoxHash.get(proxyHost);
            if (wi == null) {
                continue;
            }
            final Value ipSaved = savedOutsideIps.get(proxyHost);
            final Value value = wi.getValue();
            if (!Tools.areEqual(ipSaved, value)) {
                changed = true;
            }
        }
        return changed;
    }

    /** Stores addresses for host. */
    private void storeHostAddresses() {
        savedHostAddresses.clear();
        /* port */
        savedPort = portComboBox.getValue();
        /* addresses */
        for (final Host host : getHosts()) {
            final Widget wi = addressComboBoxHash.get(host);
            if (wi == null) {
                continue;
            }
            final Value address = wi.getValue();
            if (address == null || address.isNothingSelected()) {
                savedHostAddresses.remove(host);
            } else {
                savedHostAddresses.put(host, address);
            }
            host.getBrowser().getUsedPorts().add(savedPort.getValueForConfig());
        }
    }

    /** Stores addresses for host. */
    private void storeProxyInfo() {
        savedInsideIps.clear();
        savedOutsideIps.clear();
        /* ports */
        final Value sip = insidePortComboBox.getValue();
        savedInsidePort = sip;
        final Value sop = outsidePortComboBox.getValue();
        savedOutsidePort = sop;
        /* ips */
        for (final Host host : getConfiguredProxyHosts()) {
            final Widget insideWi = insideIpComboBoxHash.get(host);
            if (insideWi != null) {
                final Value insideIp = insideWi.getValue();
                if (insideIp == null || insideIp.isNothingSelected()) {
                    savedInsideIps.remove(host);
                } else {
                    savedInsideIps.put(host, insideIp);
                }
            }
            host.getBrowser().getUsedPorts().add(sip.getValueForConfig());

            final Widget outsideWi = outsideIpComboBoxHash.get(host);
            if (outsideWi != null) {
                final Value outsideIp = outsideWi.getValue();
                if (outsideIp == null || outsideIp.isNothingSelected()) {
                    savedOutsideIps.remove(host);
                } else {
                    savedOutsideIps.put(host, outsideIp);
                }
            }
            host.getBrowser().getUsedProxyPorts().add(sop.getValueForConfig());
        }
    }

    /** Return net interface with port as they appear in the drbd config. */
    private String getNetInterfaceWithPort(final String address, final String port) {
        if (address.contains(":")) {
            return "ipv6 [" + address + "]:" + port;
        } else {
            return address + ':' + port;
        }
    }

    /** Hide/show proxy panels for selected hosts. */
    public void setProxyPanels(final boolean wizard) {
        final Collection<Host> visible = new HashSet<Host>();
        final Widget insidePortCB;
        final Widget outsidePortCB;
        final Widget portCB;
        if (wizard) {
            insidePortCB = insidePortComboBoxWizard;
            outsidePortCB = insidePortComboBoxWizard;
            portCB = portComboBoxWizard;
        } else {
            insidePortCB = insidePortComboBox;
            outsidePortCB = insidePortComboBox;
            portCB = portComboBox;
        }
        for (final Host host : getHosts()) {
            final Host proxyHost = getProxyHost(host, wizard);
            if (proxyHost != null) {
                visible.add(proxyHost);
            }
        }
        visible.addAll(selectedProxyHosts);
        final boolean isProxy = !visible.isEmpty();
        for (final Host pHost : getCluster().getProxyHosts()) {
            final String section = Tools.getString("ResourceInfo.Proxy") + pHost.getName();
            enableSection(section, visible.contains(pHost), wizard);
        }
        enableSection(SECTION_PROXY, isProxy, wizard);
        enableSection(SECTION_PROXY_PORTS, isProxy, wizard);
        final String portLabel;
        if (isProxy) {
            if (insidePortCB.isNew()
                && (savedInsidePort == null || savedInsidePort.isNothingSelected())) {
                insidePortCB.setValue(new StringValue(Integer.toString(getDefaultInsidePort())));
            }
            if (outsidePortCB.isNew()
                && (savedOutsidePort == null || savedOutsidePort.isNothingSelected())) {
                outsidePortCB.setValue(savedPort);
            }
            portLabel = Tools.getString("ResourceInfo.NetInterfacePortToProxy");

            globalInfo.enableProxySection(wizard); /* never disable */
        } else {
            portLabel = Tools.getString("ResourceInfo.NetInterfacePort");
        }
        portCB.getLabel().setText(portLabel);
        final Widget protocolWi = getWidget(DrbdXml.PROTOCOL_PARAM, Widget.WIZARD_PREFIX);
        final Widget pingTimeoutWi = getWidget(DrbdXml.PING_TIMEOUT_PARAM, Widget.WIZARD_PREFIX);
        final DrbdXml dxml = getBrowser().getDrbdXml();
        if (protocolWi != null && getResource().isNew()) {
            if (isProxy) {
                protocolWi.setValue(PROXY_DEFAULT_PROTOCOL);
                pingTimeoutWi.setValue(PROXY_DEFAULT_PING_TIMEOUT);
            } else {
                protocolWi.setValue(dxml.getParamPreferred(DrbdXml.PROTOCOL_PARAM));
                pingTimeoutWi.setValue(dxml.getParamDefault(DrbdXml.PING_TIMEOUT_PARAM));
            }
        }
    }

    private void addHostAddressListener(final boolean wizard,
                                        final MyButton thisApplyButton,
                                        final Map<Host, Widget> newIpComboBoxHash,
                                        final Map<Host, Widget> ipComboBoxHash) {
        for (final Host host : getHosts()) {
            final Widget wi;
            final Widget rwi;
            if (wizard) {
                wi = newIpComboBoxHash.get(host); /* wizard cb */
                rwi = ipComboBoxHash.get(host);
            } else {
                wi = newIpComboBoxHash.get(host); /* normal cb */
                rwi = null;
            }
            if (wi == null) {
                continue;
            }
            wi.addListeners(new WidgetListener() {
                @Override
                public void check(final Value value) {
                    checkParameterFields(wi, rwi, null, null, thisApplyButton);
                    setProxyPanels(wizard);
                    if (value instanceof ProxyNetInfo && ((NetInfo) value).getNetInterface() == null) {
                        final int s = ProxyNetInfo.PROXY_PREFIX.length();
                        /* select the IP part */
                        wi.setAlwaysEditable(true);
                        application.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                wi.select(s, s + NetInfo.IP_PLACEHOLDER.length());
                            }
                        });
                    } else if (value instanceof StringValue) {
                        wi.setAlwaysEditable(true);
                    } else {
                        wi.setAlwaysEditable(false);
                    }
                    if (value instanceof ProxyNetInfo && ((NetInfo) value).isLocalHost()) {
                        final Map<Host, Widget> cb;
                        if (wizard) {
                            cb = insideIpComboBoxHashWizard;
                        } else {
                            cb = insideIpComboBoxHash;
                        }
                        final Host proxyHost = getProxyHost(host, wizard);
                        cb.get(proxyHost).setValue(new StringValue(getIp(value)));
                    }
                }
            });
        }
    }

    private void addIpListeners(final boolean wizard,
                                final MyButton thisApplyButton,
                                final Map<Host, Widget> newIpComboBoxHash,
                                final Map<Host, Widget> ipComboBoxHash) {
        for (final Host pHost : new HashSet<Host>(getCluster().getProxyHosts())) {
            final Widget wi;
            final Widget rwi;
            if (wizard) {
                wi = newIpComboBoxHash.get(pHost); /* wizard cb */
                rwi = ipComboBoxHash.get(pHost);
            } else {
                wi = newIpComboBoxHash.get(pHost); /* normal cb */
                rwi = null;
            }
            if (wi == null) {
                continue;
            }
            wi.addListeners(new WidgetListener() {
                @Override
                public void check(final Value value) {
                    checkParameterFields(wi, rwi, null, null, thisApplyButton);
                }
            });
        }
    }

    /** Adds port listeners. */
    private void addPortListeners(final boolean wizard,
                                  final MyButton thisApplyButton,
                                  final Widget newPortWi,
                                  final Widget portWi) {
        final Widget pwi;
        final Widget prwi;
        if (wizard) {
            pwi = newPortWi;
            prwi = portWi;
        } else {
            pwi = portWi;
            prwi = null;
        }
        pwi.addListeners(new WidgetListener() {
                                @Override
                                public void check(final Value value) {
                                    checkParameterFields(pwi,
                                                         prwi,
                                                         null,
                                                         null,
                                                         thisApplyButton);
                                }
                            });
    }

    @Override
    public void storeComboBoxValues(final String[] params) {
        super.storeComboBoxValues(params);
        storeHostAddresses();
        storeProxyInfo();
    }

    /** Returns true if volume exists. */
    public VolumeInfo getDrbdVolumeInfo(final String volumeNr) {
        if (volumeNr == null) {
            return null;
        }
        for (final VolumeInfo dvi : drbdVolumes) {
            if (volumeNr.equals(dvi.getName())) {
                return dvi;
            }
        }
        return null;
    }

    /** Remove drbd volume. Returns true if there are no more volumes. */
    public boolean removeDrbdVolume(final VolumeInfo dvi) {
        drbdVolumes.remove(dvi);
        return drbdVolumes.isEmpty();
    }

    /** Removes this object. */
    @Override
    public void removeMyself(final Application.RunMode runMode) {
        super.removeMyself(runMode);
        getBrowser().getDrbdXml().removeResource(getName());
        final Set<Host> hosts0 = getHosts();
        for (final Host host : hosts0) {
            host.getBrowser().getUsedPorts().remove(portComboBox.getStringValue());
            host.getBrowser().getUsedPorts().remove(insidePortComboBox.getStringValue());
            host.getBrowser().getUsedProxyPorts().remove(outsidePortComboBox.getStringValue());
        }

        final Map<String, ResourceInfo> drbdResHash = getBrowser().getDrbdResourceNameHash();
        final ResourceInfo dri = drbdResHash.get(getName());
        drbdResHash.remove(getName());
        getBrowser().putDrbdResHash();
        if (dri != null) {
            dri.setName(null);
        }
        if (Application.isLive(runMode)) {
            treeMenuController.removeNode(getNode());
        }
        globalInfo.reloadDRBDResourceComboBoxes();
    }

    /** Returns DRBD volumes. */
    public Set<VolumeInfo> getDrbdVolumes() {
        return drbdVolumes;
    }

    /** Reload combo boxes. */
    @Override
    public void reloadComboBoxes() {
        super.reloadComboBoxes();
        String param = DRBD_RES_PARAM_AFTER;
        if (!globalInfo.atLeastVersion("8.4")) {
            param = DRBD_RES_PARAM_AFTER_8_3;
        }
        final List<Value> l = new ArrayList<Value>();
        final Value di = new StringValue("-1", Tools.getString("ClusterBrowser.None"));
        l.add(di);
        final Map<String, ResourceInfo> drbdResHash = getBrowser().getDrbdResourceNameHash();
        for (final Map.Entry<String, ResourceInfo> drbdResEntry : drbdResHash.entrySet()) {
            final ResourceInfo r = drbdResEntry.getValue();
            ResourceInfo odri = r;
            boolean cyclicRef = false;
            while (true) {
                final Value valueS = odri.getParamSaved(param);
                if (valueS == null) {
                    break;
                }
                odri = drbdResHash.get(valueS.getValueForConfig());
                if (odri == null) {
                    break;
                }
                if (odri == this) {
                    cyclicRef = true;
                }
            }
            if (r != this && !cyclicRef) {
                l.add(r);
            }
        }
        getBrowser().putDrbdResHash();

        if (resyncAfterParamWi != null) {
            final Value value = resyncAfterParamWi.getValue();
            resyncAfterParamWi.reloadComboBox(value, l.toArray(new Value[l.size()]));
        }
    }
    /** Creates popup for the block device. */
    @Override
    public List<UpdatableItem> createPopup() {
        return resourceMenu.getPulldownMenu(this);
    }

    /** Sets that this drbd resource is used by crm. */
    public void setUsedByCRM(final ServiceInfo isUsedByCRM) {
        this.isUsedByCRM = isUsedByCRM;
    }

    /** Returns whether this drbd resource is used by crm. */
    public boolean isUsedByCRM() {
        return isUsedByCRM != null && isUsedByCRM.isManaged(Application.RunMode.LIVE);
    }

    /** Returns hosts from the first volume. */
    private Set<Host> getHosts() {
        return hosts;
    }

    /** Return section color. */
    @Override
    protected Color getSectionColor(final String section) {
        if (SECTION_PROXY.equals(section)) {
            return AppDefaults.LIGHT_ORANGE;
        }
        return super.getSectionColor(section);
    }

    /** Get ip from ip combo box value. */
    private String getIp(final Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof ProxyNetInfo) {
            return ((Info) o).getInternalValue();
        } else if (isProxyAddress(o.toString())) {
            final Matcher drbdpM = DRBDP_ADDRESS.matcher(o.toString());
            if (drbdpM.matches()) {
                return drbdpM.group(1);
            }
            return null;
        } else if (o instanceof String) {
            return (String) o;
        } else if (o instanceof StringValue) {
            return ((Value) o).getValueForConfig();
        } else {
            return ((Info) o).getInternalValue();
        }
    }

    /** Get proxy from ip combo box value. Null, if it's not a proxy. */
    public Host getProxyHost(final Host host, final boolean wizard) {
        final Widget addrW;
        if (wizard) {
            addrW = addressComboBoxHashWizard.get(host);
        } else {
            addrW = addressComboBoxHash.get(host);
        }
        if (addrW == null) {
            return null;
        }
        final Object o = addrW.getValue();
        if (o == null) {
            return null;
        }
        if (o instanceof ProxyNetInfo) {
            final ProxyNetInfo pni = (ProxyNetInfo) o;
            return pni.getProxyHost();
        } else if (isProxyAddress(o.toString())) {
            final Matcher drbdpM = DRBDP_ADDRESS.matcher(o.toString());
            if (drbdpM.matches()) {
                final String name = drbdpM.group(3);
                return getCluster().getProxyHostByName(name);
            }
        }
        return null;
    }

    /** Return default inside ip, that is the same as the "address" field. */
    private Value getDefaultInsideIp(final Host host) {
        final Widget wi = addressComboBoxHash.get(host);
        if (wi != null) {
            return wi.getValue();
        }
        return null;
    }

    /** Return the lowest unused port. */
    public int getLowestUnusedPort() {
        int lowest = -1;
        for (final Host host : getHosts()) {
            for (final String usedPortS : host.getBrowser().getUsedPorts()) {
                if (Tools.isNumber(usedPortS)) {
                    final int usedPort = Integer.parseInt(usedPortS);
                    if (lowest < 0 || usedPort > lowest) {
                        lowest = usedPort;
                    }
                }
            }
        }
        if (lowest < 0) {
            return Tools.getDefaultInt("HostBrowser.DrbdNetInterfacePort");
        }
        return lowest + 1;
    }

    /** Return the lowest used port. */
    public int getLowestUnusedProxyPort() {
        int lowest = -1;
        for (final Host host : getHosts()) {
            for (final String usedPortS : host.getBrowser().getUsedProxyPorts()) {
                if (Tools.isNumber(usedPortS)) {
                    final int usedPort = Integer.parseInt(usedPortS);
                    if (lowest < 0 || usedPort > lowest) {
                        lowest = usedPort;
                    }
                }
            }
        }
        if (lowest < 0) {
            return Tools.getDefaultInt("HostBrowser.DrbdNetInterfacePort");
        }
        return lowest + 1;
    }

    /** Return list of DRBD ports for combobox. */
    List<Value> getPossibleDrbdPorts(final int defaultPortInt) {
        final List<Value> drbdPorts = new ArrayList<Value>();
        drbdPorts.add(null);
        int defaultPortInt0 = defaultPortInt;
        int i = 0;
        while (i < 10) {
            final String port = Integer.toString(defaultPortInt0);
            boolean contains = false;
            for (final Host host : getHosts()) {
                if (host.getBrowser().getUsedPorts().contains(port)) {
                    contains = true;
                }
            }
            if (!contains || i == 0) {
                drbdPorts.add(new StringValue(port));
                i++;
            }
            defaultPortInt0++;
        }
        return drbdPorts;
    }

    /** Return default proxy inside port, that is smaller than the drbd port.
     */
    private int getDefaultInsidePort() {
        final Value insideDefaultPort = savedInsidePort;
        final int insideDefaultPortInt;
        if (insideDefaultPort == null
            || insideDefaultPort.isNothingSelected()) {
            if (savedPort == null || savedPort.isNothingSelected()) {
                insideDefaultPortInt = getLowestUnusedPort() + 1;
            } else {
                insideDefaultPortInt = Integer.parseInt(savedPort.getValueForConfig()) + 1;
            }
        } else {
            insideDefaultPortInt = Integer.parseInt(insideDefaultPort.getValueForConfig());
        }
        return insideDefaultPortInt;
    }

    /**
     * Return if this resource uses proxy on the specified host.
     */
    public boolean isProxy(final Host host) {
        final Widget cb = addressComboBoxHash.get(host);
        return cb != null && isProxyAddress(cb.getValue());
    }

    /**
     * Return true if the address is a proxy address.
     */
    private boolean isProxyAddress(final Object address) {
        return address instanceof ProxyNetInfo
               || (address != null && address.toString().startsWith(ProxyNetInfo.PROXY_PREFIX));
    }

    /**
     * Return proxy hosts that are used in this resource. */
    public Set<Host> getConfiguredProxyHosts() {
        final Set<Host> proxyHosts = new HashSet<Host>();
        for (final Host host : getHosts()) {
            final Host proxyHost = getProxyHost(host, !WIZARD);
            if (proxyHost != null) {
                proxyHosts.add(proxyHost);
            }
        }
        return proxyHosts;
    }

    /**
     * This causes the whole drbd resource panel to be reloaded.
     */
    public void resetDrbdResourcePanel() {
        resetInfoPanel();
        infoPanel = null;
        getInfoPanel();
        waitForInfoPanel();
        selectMyself();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (this.getValueForConfig() != null ? this.getValueForConfig().hashCode() : 0);
        hash = 59 * hash + (this.getUnit() != null ? this.getUnit().hashCode() : 0);
        hash = 59 * hash + (this.getBrowser() != null ? this.getBrowser().hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Value)) {
            return false;
        }
        final Value other = (Value) obj;
        if ((this.getValueForConfig() == null) ? (other.getValueForConfig() != null) : !this.getValueForConfig().equals(other.getValueForConfig())) {
            return false;
        }
        if (this.getUnit() != other.getUnit() && (this.getUnit() == null || !this.getUnit().equals(other.getUnit()))) {
            return false;
        }
        if (obj instanceof Info) {
            if (this.getBrowser() != ((Info) other).getBrowser() && (this.getBrowser() == null || !this.getBrowser().equals(((Info) other).getBrowser()))) {
                return false;
            }
        }
        return true;
    }
}
