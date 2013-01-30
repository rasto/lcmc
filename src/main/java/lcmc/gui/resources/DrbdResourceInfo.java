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
import lcmc.gui.Browser;
import lcmc.gui.HostBrowser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.gui.SpringUtilities;
import lcmc.data.resources.DrbdResource;
import lcmc.data.resources.NetInterface;
import lcmc.data.Host;
import lcmc.data.DrbdXML;
import lcmc.data.DrbdXML.HostProxy;
import lcmc.data.DRBDtestData;
import lcmc.data.AccessMode;
import lcmc.data.ConfigData;
import lcmc.data.DrbdProxy;
import lcmc.utilities.Tools;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.DRBD;
import lcmc.utilities.MyButton;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.MyMenu;
import lcmc.utilities.WidgetListener;
import lcmc.configs.AppDefaults;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Enumeration;
import java.util.HashSet;
import javax.swing.SwingUtilities;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.JMenuItem;
import javax.swing.tree.DefaultMutableTreeNode;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


/**
 * this class holds info data, menus and configuration
 * for a drbd resource.
 */
public final class DrbdResourceInfo extends DrbdGuiInfo {
    /** List of volumes. */
    private final Set<DrbdVolumeInfo> drbdVolumes =
                                        new LinkedHashSet<DrbdVolumeInfo>();
    /** Proxy on host panels. */
    //private final Map<Host, JPanel> proxyPanels = new HashMap<Host, JPanel>();
    ///** Common proxy ports panel. */
    //private JPanel commonProxyPortsPanel = null;
    /** Cache for getInfoPanel method. */
    private JComponent infoPanel = null;
    /** Whether the meta-data has to be created or not. */
    private boolean haveToCreateMD = false;
    /** Name of the drbd resource name parameter. */
    static final String DRBD_RES_PARAM_NAME = "name";
    /** A map from host to the combobox with addresses. */
    private Map<Host, Widget> addressComboBoxHash =
                                             new HashMap<Host, Widget>();
    /** A map from host to the combobox with inside ips. */
    private Map<Host, Widget> insideIpComboBoxHash =
                                             new HashMap<Host, Widget>();
    /** A map from host to the combobox with outside ips. */
    private Map<Host, Widget> outsideIpComboBoxHash =
                                             new HashMap<Host, Widget>();
    /** A map from host to the combobox with addresses for wizard. */
    private Map<Host, Widget> addressComboBoxHashWizard =
                                             new HashMap<Host, Widget>();
    /** A map from host to the combobox with inside ips for wizard. */
    private Map<Host, Widget> insideIpComboBoxHashWizard =
                                             new HashMap<Host, Widget>();
    /** A map from host to the combobox with outside ips for wizard. */
    private Map<Host, Widget> outsideIpComboBoxHashWizard =
                                             new HashMap<Host, Widget>();
    /** A map from host to stored addresses. */
    private final Map<Host, String> savedHostAddresses =
                                                 new HashMap<Host, String>();
    /** A map from host to stored inside ip. */
    private final Map<Host, String> savedInsideIps =
                                                 new HashMap<Host, String>();
    /** A map from host to stored outside ips. */
    private final Map<Host, String> savedOutsideIps =
                                                 new HashMap<Host, String>();
    /** Saved port, that is the same for both hosts. */
    private String savedPort = null;
    /** Saved proxy inside port, that is the same for both hosts. */
    private String savedInsidePort = null;
    /** Saved proxy outside port, that is the same for both hosts. */
    private String savedOutsidePort = null;
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
    /** Whether the drbd resource used by CRM. */
    private ServiceInfo isUsedByCRM;
    /** Hosts. */
    private final Set<Host> hosts;
    /** Proxy section. */
    private static final String SECTION_PROXY = "proxy";
    /** Proxy ports section. */
    private static final String SECTION_PROXY_PORTS =
                                 Tools.getString("DrbdResourceInfo.ProxyPorts");

    /** Parse ip and host name. */
    public static final Pattern DRBDP_ADDRESS =
       Pattern.compile("^" + ProxyNetInfo.PROXY_PREFIX
                       + "(\\d+\\.\\d+\\.\\d+\\.\\d+)(\\s+on\\s+(\\S+))$");
    /**
     * Prepares a new <code>DrbdResourceInfo</code> object.
     */
    DrbdResourceInfo(final String name,
                     final Set<Host> hosts,
                     final Browser browser) {
        super(name, browser);
        this.hosts = hosts;
        setResource(new DrbdResource(name));
        //getResource().setValue(DRBD_RES_PARAM_DEV, drbdDev);
    }

    /** Add a drbd volume. */
    public void addDrbdVolume(final DrbdVolumeInfo drbdVolume) {
        drbdVolumes.add(drbdVolume);
    }

    /** Creates and returns proxy config. */
    String proxyConfig(final ProxyNetInfo proxyNetInfo)
    throws Exceptions.DrbdConfigException {
        final StringBuilder config = new StringBuilder(50);
        /*
                proxy on centos6-a.site {
                        inside 127.0.0.1:7788;
                        outside 192.168.133.191:7788;
                }
        */
        final Host host = proxyNetInfo.getProxyHost();
        final String insideIp =
                            getIp(insideIpComboBoxHash.get(host).getValue());
        final String insidePort = insidePortComboBox.getStringValue();
        final String outsideIp =
                            getIp(outsideIpComboBoxHash.get(host).getValue());
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
    throws Exceptions.DrbdConfigException {
        final StringBuilder config = new StringBuilder(50);
        config.append("resource " + getName() + " {\n");
        final String[] params = getBrowser().getDrbdXML().getSectionParams(
                                                                   "resource");
        for (String param : params) {
            //if (DRBD_RES_PARAM_DEV.equals(param)) {
            //    continue;
            //}
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
            config.append(drbdSectionsConfig(configOnHost));
        } catch (Exceptions.DrbdConfigException dce) {
            throw dce;
        }
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
            final List<String> volumeConfigs = new ArrayList<String>();
            for (final DrbdVolumeInfo dvi : drbdVolumes) {
                final String volumeConfig = dvi.drbdVolumeConfig(
                                                             host,
                                                             volumesAvailable);
                if (!"".equals(volumeConfig)) {
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
                    final Object o = awi.getValue();
                    if (o == null) {
                        throw new Exceptions.DrbdConfigException(
                                    "Address not defined in "
                                    + getCluster().getName()
                                    + " (" + getName() + ")");
                    }
                    final String ip = getIp(o);
                    config.append("\n\t\taddress\t\t");
                    config.append(getNetInterfaceWithPort(
                                                        ip,
                                                        pwi.getStringValue()));
                    config.append(";");

                    if (awi.getValue() instanceof ProxyNetInfo) {
                        config.append(
                                  proxyConfig((ProxyNetInfo) awi.getValue()));
                    } else if (isProxyAddress(awi.getValue())) {
                        final Matcher drbdpM = DRBDP_ADDRESS.matcher(
                                                    awi.getValue().toString());
                        if (drbdpM.matches()) {
                            final String proxyIp = drbdpM.group(1);
                            final String hostName = drbdpM.group(3);
                            final Host proxyHost =
                                    getDrbdInfo().getProxyHostByName(hostName);
                            config.append(
                              proxyConfig(
                                  new ProxyNetInfo(
                                   "",
                                   new NetInterface("", proxyIp, "", "", false),
                                   getBrowser(),
                                   proxyHost)));
                        }
                    }
                }
                config.append("\n\t}\n");
            }
        } config.append("}");
        getDrbdResource().setCommited(true);
        return config.toString();
    }

    /** Clears info panel cache. */
    @Override
    public boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /** Returns drbd graphical view. */
    @Override
    public JPanel getGraphicalView() {
        return getBrowser().getDrbdGraph().getGraphPanel();
    }

    /** Returns all parameters. */
    @Override
    public String[] getParametersFromXML() {
        return getEnabledSectionParams(
                                    getBrowser().getDrbdXML().getParameters());
    }

    /**
     * Checks the new value of the parameter if it is conforms to its type
     * and other constraints.
     */
    @Override
    protected boolean checkParam(final String param,
                                                 final String newValue) {
        if (DRBD_RES_PARAM_AFTER.equals(param)
            || DRBD_RES_PARAM_AFTER_8_3.equals(param)) {
            /* drbdsetup xml syncer says it should be numeric, but in
               /etc/drbd.conf it is not. */
            return true;
        }
        return getBrowser().getDrbdXML().checkParam(param, newValue);
    }

    /** Returns the default value for the drbd parameter. */
    @Override
    public String getParamDefault(final String param) {
        final String common = getDrbdInfo().getParamSaved(param);
        if (common != null) {
            return common;
        }
        return getBrowser().getDrbdXML().getParamDefault(param);
    }

    /** Returns section to which this drbd parameter belongs. */
    @Override
    protected String getSection(final String param) {
        return getBrowser().getDrbdXML().getSection(param);
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected String isEnabled(final String param) {
        if (getDrbdResource().isCommited()
            && DRBD_RES_PARAM_NAME.equals(param)) {
            return "";
        }
        return null;
    }

    /** Returns the widget that is used to edit this parameter. */
    @Override
    protected Widget createWidget(final String param,
                                  final String prefix,
                                  final int width) {
        Widget paramWi;
        if (DRBD_RES_PARAM_NAME.equals(param)) {
            String resName;
            if (getParamSaved(DRBD_RES_PARAM_NAME) == null) {
                resName = getResource().getDefaultValue(DRBD_RES_PARAM_NAME);
            } else {
                resName = getResource().getName();
            }
            paramWi = WidgetFactory.createInstance(
                                      Widget.GUESS_TYPE,
                                      resName,
                                      Widget.NO_ITEMS,
                                      "^\\S+$",
                                      width,
                                      Widget.NO_ABBRV,
                                      new AccessMode(
                                           getAccessType(param),
                                           isEnabledOnlyInAdvancedMode(param)),
                                      Widget.NO_BUTTON);
            paramWi.setEnabled(!getDrbdResource().isCommited());
            widgetAdd(param, prefix, paramWi);
        } else if (DRBD_RES_PARAM_AFTER.equals(param)
                   || DRBD_RES_PARAM_AFTER_8_3.equals(param)) {
            final List<Info> l = new ArrayList<Info>();
            final String defaultItem = getParamSaved(param);
            final StringInfo di = new StringInfo(
                                        Tools.getString("ClusterBrowser.None"),
                                        "-1",
                                        getBrowser());
            l.add(di);
            final Map<String, DrbdResourceInfo> drbdResHash =
                                                getBrowser().getDrbdResHash();
            for (final String drbdRes : drbdResHash.keySet()) {
                final DrbdResourceInfo r = drbdResHash.get(drbdRes);
                DrbdResourceInfo odri = r;
                boolean cyclicRef = false;
                while ((odri = drbdResHash.get(
                       odri.getParamSaved(param))) != null) {
                    if (odri == this) {
                        cyclicRef = true;
                    }
                }
                if (r != this && !cyclicRef) {
                    l.add(r);
                }
            }
            getBrowser().putDrbdResHash();
            resyncAfterParamWi = WidgetFactory.createInstance(
                                      Widget.Type.COMBOBOX,
                                      defaultItem,
                                      l.toArray(new Info[l.size()]),
                                      Widget.NO_REGEXP,
                                      width,
                                      Widget.NO_ABBRV,
                                      new AccessMode(
                                           getAccessType(param),
                                           isEnabledOnlyInAdvancedMode(param)),
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
    public void apply(final boolean testOnly) {
        if (!testOnly) {
            final String[] params = getParametersFromXML();
            Tools.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                }
            });
            getInfoPanel();
            waitForInfoPanel();
            getBrowser().getDrbdResHash().remove(getName());
            getBrowser().putDrbdResHash();
            storeComboBoxValues(params);

            final String name = getParamSaved(DRBD_RES_PARAM_NAME);
            getDrbdResource().setName(name);
            setName(name);

            getBrowser().getDrbdResHash().put(name, this);
            getBrowser().putDrbdResHash();
            getBrowser().getDrbdGraph().repaint();
            getDrbdInfo().setAllApplyButtons();
            getDrbdInfo().reloadDRBDResourceComboBoxes();
        }
    }

    /** Set all apply buttons. */
    void setAllApplyButtons() {
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
            dvi.setAllApplyButtons();
        }
        setApplyButtons(null, getParametersFromXML());
    }

    /** Returns panel with form to configure a drbd resource. */
    @Override
    public JComponent getInfoPanel() {
        //getBrowser().getDrbdGraph().pickInfo(this);
        if (infoPanel != null) {
            return infoPanel;
        }
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;
            /**
             * Whether the whole thing should be enabled.
             */
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                getBrowser().getDrbdGraph().stopTestAnimation(getApplyButton());
                getApplyButton().setToolTipText(null);
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
                    getDrbdInfo().createDrbdConfig(true);
                    for (final Host h : getHosts()) {
                        DRBD.adjust(h, DRBD.ALL, null, true);
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

        /* resource name */
        getResource().setValue(DRBD_RES_PARAM_NAME,
                               getDrbdResource().getName());
        final String[] params = getParametersFromXML();
        /* address combo boxes */
        addHostAddresses(optionsPanel,
                         ClusterBrowser.SERVICE_LABEL_WIDTH,
                         ClusterBrowser.SERVICE_FIELD_WIDTH,
                         false,
                         getApplyButton());
        enableSection(SECTION_PROXY, !DrbdProxy.PROXY, !WIZARD);
        addParams(optionsPanel,
                  params,
                  Tools.getDefaultSize("ClusterBrowser.DrbdResLabelWidth"),
                  Tools.getDefaultSize("ClusterBrowser.DrbdResFieldWidth"),
                  null);

        getApplyButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
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
                            getDrbdInfo().createDrbdConfig(false);
                            for (final Host h : getHosts()) {
                                DRBD.adjust(h, DRBD.ALL, null, false);
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
                @Override
                public void actionPerformed(final ActionEvent e) {
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
        setProxyPanels(!WIZARD);
        infoPanelDone();
        return infoPanel;
    }

    /** Remove myself from all hashes. */
    public void removeFromHashes() {
        getBrowser().getDrbdResHash().remove(getName());
        getBrowser().putDrbdResHash();
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
            removeDrbdVolumeFromHashes(dvi);
        }
    }

    /** Remove drbd volume from all hashes. */
    public void removeDrbdVolumeFromHashes(final DrbdVolumeInfo drbdVolume) {
        getBrowser().getDrbdDevHash().remove(drbdVolume.getDevice());
        getBrowser().putDrbdDevHash();
        for (final BlockDevInfo bdi : drbdVolume.getBlockDevInfos()) {
            bdi.removeFromDrbd();
        }
    }

    /** Returns string of the drbd resource. */
    @Override
    public String toString() {
        String name = getName();
        if (name == null || "".equals(name)) {
            name = Tools.getString("ClusterBrowser.DrbdResUnconfigured");
        }
        return "drbd: " + name;
    }

    /** Returns whether two drbd resources are equal. */
    @Override
    public boolean equals(final Object value) {
        if (value == null) {
            return false;
        }
        //if (Tools.isStringClass(value)) {
        //    return getDrbdResource().getValue(DRBD_RES_PARAM_DEV).equals(
        //                                                   value.toString());
        //} else {
            if (toString() == null) {
                return false;
            }
            return toString().equals(value.toString());
        //}
    }

    //@Override
    //int hashCode() {
    //    return toString().hashCode();
    //}

    /** Returns common file systems. */
    public StringInfo[] getCommonFileSystems(final String defaultValue) {
        return getBrowser().getCommonFileSystems(defaultValue);
    }

    /** Returns whether the specified host has this drbd resource. */
    boolean resourceInHost(final Host host) {
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
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
    private void setNetworkParameters(final DrbdXML dxml) {
        String hostPort = null;
        final boolean infoPanelOk = infoPanel != null;
        for (final Host host : getHosts()) {
            final String hostAddress =
                            dxml.getVirtualInterface(host.getName(), getName());
            final String hp =
                       dxml.getVirtualInterfacePort(host.getName(), getName());
            if (hostPort != null && !hostPort.equals(hp)) {
                Tools.appWarning("more ports in " + getName() + " "
                                 + hp + " " + hostPort);
            }
            hostPort = hp;
            final String savedAddress = savedHostAddresses.get(host);
            if (!Tools.areEqual(hostAddress, savedAddress)) {
                if (hostAddress == null) {
                    savedHostAddresses.remove(host);
                } else {
                    savedHostAddresses.put(host, hostAddress);
                }
                if (infoPanelOk) {
                    final Widget wi = addressComboBoxHash.get(host);
                    if (wi != null) {
                        wi.setValue(hostAddress);
                    }
                }
            }
        }

        /* set port */
        if (!Tools.areEqual(hostPort, savedPort)) {
            savedPort = hostPort;
            for (final Host host : getHosts()) {
                host.getBrowser().getUsedPorts().add(savedPort);
            }
            if (infoPanelOk) {
                final Widget wi = portComboBox;
                if (wi != null) {
                    if (hostPort == null) {
                        wi.setValue(Widget.NOTHING_SELECTED);
                    } else {
                        wi.setValue(hostPort);
                    }
                }
            }
        }
    }

    /** Set proxy parameters. */
    private void setProxyParameters(final DrbdXML dxml) {
        String hostInsidePort = null;
        String hostOutsidePort = null;
        final boolean infoPanelOk = infoPanel != null;
        final Set<Host> configuredProxyHosts = getConfiguredProxyHosts();
        for (final Host host : configuredProxyHosts) {
            final HostProxy hostProxy =
                                 dxml.getHostProxy(host.getName(), getName());
            String insideIp = null;
            String outsideIp = null;
            String insidePort = null;
            String outsidePort = null;
            if (hostProxy != null) {
                insideIp = hostProxy.getInsideIp();
                outsideIp = hostProxy.getOutsideIp();
                insidePort = hostProxy.getInsidePort();
                outsidePort = hostProxy.getOutsidePort();
            }

            if (insidePort != null && !insidePort.equals(hostInsidePort)) {
                Tools.appWarning("more proxy inside ports in " + getName()
                                 + " " + insidePort + " " + hostInsidePort);
            }
            hostInsidePort = insidePort;

            if (outsidePort != null && !outsidePort.equals(hostOutsidePort)) {
                Tools.appWarning("more proxy outside ports in " + getName()
                                 + " " + outsidePort + " " + hostOutsidePort);
            }
            hostOutsidePort = outsidePort;

            final String savedInsideIp = savedInsideIps.get(host);
            if (!Tools.areEqual(insideIp, savedInsideIp)) {
                if (insideIp == null) {
                    savedInsideIps.remove(host);
                } else {
                    savedInsideIps.put(host, insideIp);
                }
                if (infoPanelOk) {
                    final Widget wi = insideIpComboBoxHash.get(host);
                    if (wi != null) {
                        wi.setValue(insideIp);
                    }
                }
            }

            final String savedOutsideIp = savedOutsideIps.get(host);
            if (!Tools.areEqual(outsideIp, savedOutsideIp)) {
                if (outsideIp == null) {
                    savedOutsideIps.remove(host);
                } else {
                    savedOutsideIps.put(host, outsideIp);
                }
                if (infoPanelOk) {
                    final Widget wi = outsideIpComboBoxHash.get(host);
                    if (wi != null) {
                        wi.setValue(outsideIp);
                    }
                }
            }
        }

        /* set proxy ports */
        if (!Tools.areEqual(hostInsidePort, savedInsidePort)) {
            savedInsidePort = hostInsidePort;
            for (final Host host : configuredProxyHosts) {
                if (!isProxy(host)) {
                    continue;
                }
                host.getBrowser().getUsedPorts().add(hostInsidePort);
            }
            if (infoPanelOk) {
                final Widget wi = insidePortComboBox;
                if (wi != null) {
                    if (hostInsidePort == null) {
                        wi.setValue(Widget.NOTHING_SELECTED);
                    } else {
                        wi.setValue(hostInsidePort);
                    }
                }
            }
        }

        if (!Tools.areEqual(hostOutsidePort, savedOutsidePort)) {
            savedOutsidePort = hostOutsidePort;
            for (final Host host : configuredProxyHosts) {
                host.getBrowser().getUsedProxyPorts().add(hostOutsidePort);
            }
            if (infoPanelOk) {
                final Widget wi = outsidePortComboBox;
                if (wi != null) {
                    if (hostOutsidePort == null) {
                        wi.setValue(Widget.NOTHING_SELECTED);
                    } else {
                        wi.setValue(hostOutsidePort);
                    }
                }
            }
        }
    }

    /** Sets stored parameters. */
    public void setParameters() {
        getDrbdResource().setCommited(true);
        final DrbdXML dxml = getBrowser().getDrbdXML();
        final String resName = getResource().getName();
        for (final String sectionString : dxml.getSections()) {
            /* remove -options */
            final String section = sectionString.replaceAll("-options$", "");
            for (final String param : dxml.getSectionParams(section)) {
                String value = dxml.getConfigValue(resName, section, param);
                final String defaultValue = getParamDefault(param);
                final String oldValue = getParamSaved(param);
                if ("".equals(value)) {
                    value = defaultValue;
                }
                final Widget wi = getWidget(param, null);
                if (!Tools.areEqual(value, oldValue)) {
                    getResource().setValue(param, value);
                    if (wi != null) {
                        wi.setValue(value);
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
    public boolean checkResourceFieldsChanged(final String param,
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
        boolean changed = false;
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
            if (dvi.checkResourceFieldsChanged(param,
                                               dvi.getParametersFromXML(),
                                               fromDrbdInfo,
                                               true)) {
                changed = true;
            }
        }
        if (checkHostAddressesFieldsChanged()) {
            changed = true;
        }

        if (isSectionEnabled(SECTION_PROXY) && checkProxyFieldsChanged()) {
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
    @Override
    public boolean checkResourceFieldsCorrect(final String param,
                                              final String[] params) {
        return checkResourceFieldsCorrect(param, params, false);
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
        final String port = portComboBox.getStringValue();
        final Widget pwi = portComboBox;
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

    /** Check addresses. */
    private boolean checkAddressCorrect() {
        final Map<Host, Widget> addressComboBoxHashClone =
                           new HashMap<Host, Widget>(addressComboBoxHash);
        boolean correct = true;
        for (final Host host : addressComboBoxHashClone.keySet()) {
            final Widget wi = addressComboBoxHashClone.get(host);
            final Widget wizardWi = addressComboBoxHashWizard.get(host);
            if (wi.getValue() == null) {
                correct = false;
                wi.wrongValue();
                if (wizardWi != null) {
                    wizardWi.wrongValue();
                }
            } else {
                wi.setBackground(null, savedHostAddresses.get(host), true);
                if (wizardWi != null) {
                    wizardWi.setBackground(null,
                                           savedHostAddresses.get(host),
                                           true);
                }
            }
        }
        return correct;
    }

    /** Check proxy port. */
    private boolean checkProxyPortCorrect(final Widget pwi,
                                          final Widget pWizardWi,
                                          final String savedPort) {
        /* proxy ports */
        final String port = pwi.getStringValue();
        boolean correct = true;
        if (checkPort(port)) {
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
        final Map<Host, Widget> insideIpComboBoxHashClone =
                           new HashMap<Host, Widget>(insideIpComboBoxHash);
        boolean correct = true;
        for (final Host host : getConfiguredProxyHosts()) {
            final Widget wi = insideIpComboBoxHashClone.get(host);
            final Widget wizardWi = insideIpComboBoxHashWizard.get(host);
            if (wi.getValue() == null) {
                correct = false;
                wi.wrongValue();
                if (wizardWi != null) {
                    wizardWi.wrongValue();
                }
            } else {
                final String defaultInsideIp = getDefaultInsideIp(host);
                String savedInsideIp = savedInsideIps.get(host);
                if (savedInsideIp == null) {
                    savedInsideIp = defaultInsideIp;
                }
                wi.setBackground(defaultInsideIp, savedInsideIp, true);
                if (wizardWi != null) {
                    wizardWi.setBackground(defaultInsideIp,
                                           savedInsideIp,
                                           true);
                }
            }
        }
        return correct;
    }

    /** Check proxy outside ip. */
    private boolean checkProxyOutsideIpCorrect() {
        final Map<Host, Widget> outsideIpComboBoxHashClone =
                           new HashMap<Host, Widget>(outsideIpComboBoxHash);
        boolean correct = true;
        for (final Host host : getConfiguredProxyHosts()) {
            final Widget wi = outsideIpComboBoxHashClone.get(host);
            final Widget wizardWi = outsideIpComboBoxHashWizard.get(host);
            if (wi.getValue() == null) {
                correct = false;
                wi.wrongValue();
                if (wizardWi != null) {
                    wizardWi.wrongValue();
                }
            } else {
                wi.setBackground(null, savedOutsideIps.get(host), true);
                if (wizardWi != null) {
                    wizardWi.setBackground(null,
                                           savedOutsideIps.get(host),
                                           true);
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
    boolean checkResourceFieldsCorrect(final String param,
                                       final String[] params,
                                       final boolean fromDrbdInfo) {
        final DrbdInfo di = getDrbdInfo();
        boolean correct = true;
        final DrbdXML dxml = getBrowser().getDrbdXML();
        if (dxml != null && dxml.isDrbdDisabled()) {
            correct = false;
        }
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
            if (!dvi.checkResourceFieldsCorrect(param,
                                                dvi.getParametersFromXML(),
                                                fromDrbdInfo,
                                                true)) {
                correct = false;
            }
        }

        if (!checkPortCorrect()) {
            correct = false;
        }

        if (!checkAddressCorrect()) {
            correct = false;
        }

        if (isSectionEnabled(SECTION_PROXY)) {
            if (!checkProxyPortCorrect(insidePortComboBox,
                                       insidePortComboBoxWizard,
                                       savedInsidePort)) {
                correct = false;
            }

            if (!checkProxyPortCorrect(outsidePortComboBox,
                                       outsidePortComboBoxWizard,
                                       savedOutsidePort)) {
                correct = false;
            }

            if (!checkProxyInsideIpCorrect()) {
                correct = false;
            }

            if (!checkProxyOutsideIpCorrect()) {
                correct = false;
            }
        }

        return super.checkResourceFieldsCorrect(param, params) && correct;
    }

    /** Revert all values. */
    @Override
    public void revert() {
        super.revert();
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
            for (final BlockDevInfo bdi : dvi.getBlockDevInfos()) {
                if (bdi != null) {
                    bdi.revert();
                }
            }
        }

        final Map<Host, Widget> addressComboBoxHashClone =
                          new HashMap<Host, Widget>(addressComboBoxHash);
        for (final Host host : addressComboBoxHashClone.keySet()) {
            final Widget wi = addressComboBoxHashClone.get(host);
            final String haSaved = savedHostAddresses.get(host);
            if (!Tools.areEqual(wi.getValue(), haSaved)) {
                final Widget wizardWi = addressComboBoxHashWizard.get(host);
                if (wizardWi == null) {
                    wi.setValue(haSaved);
                } else {
                    wizardWi.setValue(haSaved);
                }
            }
        }

        final Widget pwi = portComboBox;
        if (!pwi.getStringValue().equals(savedPort)) {
            final Widget wizardWi = portComboBoxWizard;
            if (wizardWi == null) {
                pwi.setValue(savedPort);
            } else {
                wizardWi.setValue(savedPort);
            }
        }

        /* proxy */
        final Map<Host, Widget> insideIpComboBoxHashClone =
                          new HashMap<Host, Widget>(insideIpComboBoxHash);
        for (final Host host : insideIpComboBoxHashClone.keySet()) {
            final Widget wi = insideIpComboBoxHashClone.get(host);
            String ipSaved = savedInsideIps.get(host);
            if (ipSaved == null) {
                ipSaved = getDefaultInsideIp(host);
            }
            if (!Tools.areEqual(wi.getValue(), ipSaved)) {
                final Widget wizardWi = insideIpComboBoxHashWizard.get(host);
                if (wizardWi == null) {
                    wi.setValue(ipSaved);
                } else {
                    wizardWi.setValue(ipSaved);
                }
            }
        }

        final Widget ipwi = insidePortComboBox;
        if (!ipwi.getStringValue().equals(savedInsidePort)) {
            final Widget wizardWi = insidePortComboBoxWizard;
            if (wizardWi == null) {
                ipwi.setValue(savedInsidePort);
            } else {
                wizardWi.setValue(savedInsidePort);
            }
        }

        final Map<Host, Widget> outsideIpComboBoxHashClone =
                          new HashMap<Host, Widget>(outsideIpComboBoxHash);
        for (final Host host : outsideIpComboBoxHashClone.keySet()) {
            final Widget wi = outsideIpComboBoxHashClone.get(host);
            final String ipSaved = savedOutsideIps.get(host);
            if (!Tools.areEqual(wi.getValue(), ipSaved)) {
                final Widget wizardWi = outsideIpComboBoxHashWizard.get(host);
                if (wizardWi == null) {
                    wi.setValue(ipSaved);
                } else {
                    wizardWi.setValue(ipSaved);
                }
            }
        }

        final Widget opwi = outsidePortComboBox;
        if (!opwi.getStringValue().equals(savedOutsidePort)) {
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
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
            for (final BlockDevInfo bdi : dvi.getBlockDevInfos()) {
                if (bdi != null) {
                    bdi.setDialogStarted(dialogStarted);
                }
            }
        }
        super.setDialogStarted(dialogStarted);
    }

    /** Returns volume number of the block device. */
    String getVolumeNr(final BlockDevInfo thisBDI) {
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
            for (final BlockDevInfo bdi : dvi.getBlockDevInfos()) {
                if (bdi == thisBDI) {
                    return dvi.getName();
                }
            }
        }
        Tools.appWarning("could not get volume nr for: " + thisBDI.getName());
        return null;
    }

    /** Update panels and fields of all volumes and block devices. */
    public void updateAllVolumes() {
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
            for (final BlockDevInfo bdi : dvi.getBlockDevInfos()) {
                if (bdi != null) {
                    bdi.checkResourceFieldsChanged(null,
                                                   bdi.getParametersFromXML());
                    bdi.updateAdvancedPanels();
                }
            }
        }
    }

    /** Returns the last volume number + 1. */
    public String getAvailVolumeNumber() {
        int maxNr = -1;
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
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
        int rows = 0;
        final Map<Host, Widget> newAddressComboBoxHash =
                                             new HashMap<Host, Widget>();
        final JPanel panel =
             getParamPanel(Tools.getString("DrbdResourceInfo.HostAddresses"));
        panel.setLayout(new SpringLayout());
        for (final Host host : getHosts()) {
            final Widget wi = WidgetFactory.createInstance(
                           Widget.Type.COMBOBOX,
                           Widget.NO_DEFAULT,
                           getNetInterfacesWithProxies(host.getBrowser()),
                           Widget.NO_REGEXP,
                           rightWidth,
                           Widget.NO_ABBRV,
                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           Widget.NO_BUTTON);
            final String haSaved = savedHostAddresses.get(host);
            wi.setValueAndWait(haSaved);
            newAddressComboBoxHash.put(host, wi);

        }

        /* host addresses combo boxes */
        for (final Host host : getHosts()) {
            final Widget wi = newAddressComboBoxHash.get(host);
            final String addr =
                            Tools.getString("DrbdResourceInfo.AddressOnHost")
                            + host.getName();
            final JLabel label = new JLabel(addr);
            wi.setLabel(label, addr);
            addField(panel,
                     label,
                     wi,
                     leftWidth,
                     rightWidth,
                     0);
            rows++;
        }

        /* Port */
        String defaultPort = savedPort;
        int defaultPortInt;
        if (defaultPort == null) {
            defaultPortInt = getLowestUnusedPort();
            defaultPort = Integer.toString(defaultPortInt);
        } else {
            defaultPortInt = Integer.parseInt(defaultPort);
        }
        final List<String> drbdPorts = getPossibleDrbdPorts(defaultPortInt);

        final Widget pwi = WidgetFactory.createInstance(
                          Widget.Type.COMBOBOX,
                          defaultPort,
                          drbdPorts.toArray(new String[drbdPorts.size()]),
                          "^\\d*$",
                          leftWidth,
                          Widget.NO_ABBRV,
                          new AccessMode(ConfigData.AccessType.ADMIN, false),
                          Widget.NO_BUTTON);
        pwi.setAlwaysEditable(true);
        final String port =
                          Tools.getString("DrbdResourceInfo.NetInterfacePort");
        final JLabel label = new JLabel(port);
        addField(panel,
                 label,
                 pwi,
                 leftWidth,
                 rightWidth,
                 0);
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

        addProxyIps(optionsPanel, leftWidth, rightWidth, wizard);

        addHostAddressListener(wizard,
                               thisApplyButton,
                               newAddressComboBoxHash,
                               addressComboBoxHash);

        addPortListeners(wizard,
                         thisApplyButton,
                         portComboBoxWizard,
                         portComboBox);

        addPortListeners(wizard,
                         thisApplyButton,
                         insidePortComboBoxWizard,
                         insidePortComboBox);

        addPortListeners(wizard,
                         thisApplyButton,
                         outsidePortComboBoxWizard,
                         outsidePortComboBox);

        addIpListeners(wizard,
                       thisApplyButton,
                       insideIpComboBoxHashWizard,
                       insideIpComboBoxHash);

        addIpListeners(wizard,
                       thisApplyButton,
                       outsideIpComboBoxHashWizard,
                       outsideIpComboBoxHash);
        if (wizard) {
            addressComboBoxHashWizard = newAddressComboBoxHash;
        } else {
            addressComboBoxHash = newAddressComboBoxHash;
        }
    }

    /** Add proxy inside and outside ports. */
    private void addProxyPorts(final JPanel optionsPanel,
                               final int leftWidth,
                               final int rightWidth,
                               final boolean wizard) {
        int rows = 0;
        final JPanel panel = getParamPanel(SECTION_PROXY_PORTS);
        addSectionPanel(SECTION_PROXY_PORTS, wizard, panel);
        enableSection(SECTION_PROXY_PORTS, !DrbdProxy.PROXY, wizard);
        panel.setLayout(new SpringLayout());
        panel.setBackground(AppDefaults.LIGHT_ORANGE);
        /* inside port */
        final int insideDefaultPortInt = getDefaultInsidePort();
        final String insideDefaultPort = Integer.toString(insideDefaultPortInt);
        final List<String> insideDrbdPorts =
                                    getPossibleDrbdPorts(insideDefaultPortInt);
        final Widget insidePortWi = WidgetFactory.createInstance(
                          Widget.Type.COMBOBOX,
                          insideDefaultPort,
                          insideDrbdPorts.toArray(
                                           new String[insideDrbdPorts.size()]),
                          "^\\d*$",
                          leftWidth,
                          Widget.NO_ABBRV,
                          new AccessMode(ConfigData.AccessType.ADMIN, false),
                          Widget.NO_BUTTON);
        insidePortWi.setAlwaysEditable(true);

        final String insidePort =
                           Tools.getString("DrbdResourceInfo.ProxyInsidePort");
        final JLabel insidePortLabel = new JLabel(insidePort);
        addField(panel,
                 insidePortLabel,
                 insidePortWi,
                 leftWidth,
                 rightWidth,
                 0);
        insidePortWi.setLabel(insidePortLabel, insidePort);
        if (wizard) {
            insidePortComboBoxWizard = insidePortWi;
        } else {
            insidePortComboBox = insidePortWi;
        }
        rows++;

        /* outside port */
        String outsideDefaultPort = savedOutsidePort;
        int outsideDefaultPortInt;
        if (outsideDefaultPort == null) {
            outsideDefaultPortInt = getLowestUnusedProxyPort();
            if (outsideDefaultPortInt < insideDefaultPortInt - 1) {
                outsideDefaultPortInt = insideDefaultPortInt - 1;
            }
            outsideDefaultPort = Integer.toString(outsideDefaultPortInt);
        } else {
            outsideDefaultPortInt = Integer.parseInt(outsideDefaultPort);
        }
        final List<String> outsideDrbdPorts =
                                   getPossibleDrbdPorts(outsideDefaultPortInt);
        final Widget outsidePortWi = WidgetFactory.createInstance(
                          Widget.Type.COMBOBOX,
                          outsideDefaultPort,
                          outsideDrbdPorts.toArray(
                                          new String[outsideDrbdPorts.size()]),
                          "^\\d*$",
                          leftWidth,
                          Widget.NO_ABBRV,
                          new AccessMode(ConfigData.AccessType.ADMIN, false),
                          Widget.NO_BUTTON);
        outsidePortWi.setAlwaysEditable(true);

        final String outsidePort =
                        Tools.getString("DrbdResourceInfo.ProxyOutsidePort");
        final JLabel outsidePortLabel = new JLabel(outsidePort);
        addField(panel,
                 outsidePortLabel,
                 outsidePortWi,
                 leftWidth,
                 rightWidth,
                 0);
        outsidePortWi.setLabel(outsidePortLabel, outsidePort);
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
        //commonProxyPortsPanel = panel;
    }

    /** Add inside and outside proxy ips. */
    private void addProxyIps(final JPanel optionsPanel,
                             final int leftWidth,
                             final int rightWidth,
                             final boolean wizard) {
        final Map<Host, Widget> newInsideIpComboBoxHash =
                                             new HashMap<Host, Widget>();
        final Map<Host, Widget> newOutsideIpComboBoxHash =
                                             new HashMap<Host, Widget>();
        for (final Host pHost : getDrbdInfo().getAllProxyHosts()) {
            final HostProxy hostProxy =
                      getBrowser().getDrbdXML().getHostProxy(pHost.getName(),
                                                             getName());
            String insideIpSaved = null;
            String outsideIpSaved = null;
            if (hostProxy != null) {
                insideIpSaved = hostProxy.getInsideIp();
                outsideIpSaved = hostProxy.getOutsideIp();
            }
            final String section = Tools.getString("DrbdResourceInfo.Proxy")
                                   + pHost.getName();
            final JPanel sectionPanel = getParamPanel(section);
            addSectionPanel(section, wizard, sectionPanel);
            enableSection(section, !DrbdProxy.PROXY, wizard);
            sectionPanel.setBackground(AppDefaults.LIGHT_ORANGE);
            final Object[] proxyNetInterfaces =
                                    getNetInterfaces(pHost.getBrowser());
            /* inside ip */
            if (proxyNetInterfaces == null) {
                //TODO: just textfield
            }
            final Widget iIpWi = WidgetFactory.createInstance(
                                   Widget.Type.COMBOBOX,
                                   Widget.NO_DEFAULT,
                                   proxyNetInterfaces,
                                   Widget.NO_REGEXP,
                                   rightWidth,
                                   Widget.NO_ABBRV,
                                   new AccessMode(ConfigData.AccessType.ADMIN,
                                                  !AccessMode.ADVANCED),
                                   Widget.NO_BUTTON);
            iIpWi.setAlwaysEditable(!pHost.isConnected());
            newInsideIpComboBoxHash.put(pHost, iIpWi);
            iIpWi.setValueAndWait(insideIpSaved);

            final String insideIp =
                            Tools.getString("DrbdResourceInfo.ProxyInsideIp");
            final JLabel insideIpLabel = new JLabel(insideIp);
            iIpWi.setLabel(insideIpLabel, insideIp);
            final JPanel panel = new JPanel();
            addField(panel,
                     insideIpLabel,
                     iIpWi,
                     leftWidth,
                     rightWidth,
                     0);
            panel.setBackground(AppDefaults.LIGHT_ORANGE);
            panel.setLayout(new SpringLayout());
            sectionPanel.add(panel);

            /* outside ip */
            final Widget oIpWi = WidgetFactory.createInstance(
                           Widget.Type.COMBOBOX,
                           Widget.NO_DEFAULT,
                           proxyNetInterfaces,
                           Widget.NO_REGEXP,
                           rightWidth,
                           Widget.NO_ABBRV,
                           new AccessMode(ConfigData.AccessType.ADMIN,
                                          !AccessMode.ADVANCED),
                           Widget.NO_BUTTON);
            oIpWi.setAlwaysEditable(!pHost.isConnected());
            newOutsideIpComboBoxHash.put(pHost, oIpWi);
            oIpWi.setValueAndWait(outsideIpSaved);

            final String outsideIp =
                            Tools.getString("DrbdResourceInfo.ProxyOutsideIp");
            final JLabel outsideIpLabel = new JLabel(outsideIp);
            oIpWi.setLabel(outsideIpLabel, outsideIp);
            addField(panel,
                     outsideIpLabel,
                     oIpWi,
                     leftWidth,
                     rightWidth,
                     0);
            SpringUtilities.makeCompactGrid(panel, 2, 2, /* rows, cols */
                                            1, 1,           /* initX, initY */
                                            1, 1);          /* xPad, yPad */
            optionsPanel.add(sectionPanel);
        }
        if (wizard) {
            insideIpComboBoxHashWizard = newInsideIpComboBoxHash;
            outsideIpComboBoxHashWizard = newOutsideIpComboBoxHash;
        } else {
            insideIpComboBoxHash = newInsideIpComboBoxHash;
            outsideIpComboBoxHash = newOutsideIpComboBoxHash;
        }

    }

    /** Ruturns all net interfaces. */
    private Object[] getNetInterfaces(final HostBrowser hostBrowser) {
        final List<Object> list = new ArrayList<Object>();

        list.add(null);
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> e =
                                hostBrowser.getNetInterfacesNode().children();

        while (e.hasMoreElements()) {
            final Info i = (Info) e.nextElement().getUserObject();
            list.add(i);
        }
        return list.toArray(new Object[list.size()]);
    }

    /** Ruturns all net interfaces. */
    private Object[] getNetInterfacesWithProxies(
                                               final HostBrowser hostBrowser) {
        final List<Object> list = new ArrayList<Object>();

        list.add(null);
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> n =
                                 hostBrowser.getNetInterfacesNode().children();

        while (n.hasMoreElements()) {
            final Info i = (Info) n.nextElement().getUserObject();
            list.add(i);
        }

        /* the same host */
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> np =
                                 hostBrowser.getNetInterfacesNode().children();

        while (np.hasMoreElements()) {
            final NetInfo i = (NetInfo) np.nextElement().getUserObject();
            list.add(new ProxyNetInfo(i, hostBrowser, hostBrowser.getHost()));
        }

        /* other nodes */
        for (final Host h : getDrbdInfo().getAllProxyHosts()) {
            if (h == hostBrowser.getHost()) {
                continue;
            }
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> nph =
                         hostBrowser.getNetInterfacesNode().children();
            if (nph.hasMoreElements()) {
                while (nph.hasMoreElements()) {
                    final NetInfo i =
                                   (NetInfo) nph.nextElement().getUserObject();
                    if (i.isLocalHost()) {
                        continue;
                    }
                    list.add(new ProxyNetInfo(i, hostBrowser, h));
                }
            }
        }
        return list.toArray(new Object[list.size()]);
    }

    /** Returns true if some of the addresses have changed. */
    private boolean checkHostAddressesFieldsChanged() {
        boolean changed = false;
        for (final Host host : getHosts()) {
            final Widget wi = addressComboBoxHash.get(host);
            if (wi == null) {
                continue;
            }
            final String haSaved = savedHostAddresses.get(host);
            final Object value = wi.getValue();
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
        if (insidePortComboBox != null
            && !Tools.areEqual(savedInsidePort,
                               insidePortComboBox.getValue())) {
            changed = true;
        }

        if (outsidePortComboBox != null
            && !Tools.areEqual(savedOutsidePort,
                               outsidePortComboBox.getValue())) {
            changed = true;
        }

        /* ips */
        final Set<Host> configuredProxyHosts = getConfiguredProxyHosts();
        for (final Host host : configuredProxyHosts) {
            final Widget wi = insideIpComboBoxHash.get(host);
            if (wi == null) {
                continue;
            }
            String ipSaved = savedInsideIps.get(host);
            final String defaultInsideIp = getDefaultInsideIp(host);
            if (ipSaved == null) {
                ipSaved = defaultInsideIp;
            }
            final Object value = wi.getValue();
            if (!Tools.areEqual(ipSaved, value)) {
                changed = true;
            }
        }

        for (final Host host : configuredProxyHosts) {
            final Widget wi = outsideIpComboBoxHash.get(host);
            if (wi == null) {
                continue;
            }
            final String ipSaved = savedOutsideIps.get(host);
            final Object value = wi.getValue();
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
        savedPort = portComboBox.getStringValue();
        /* addresses */
        for (final Host host : getHosts()) {
            final Widget wi = addressComboBoxHash.get(host);
            if (wi == null) {
                continue;
            }
            final String address = getIp(wi.getValue());
            if (address == null || "".equals(address)) {
                savedHostAddresses.remove(host);
            } else {
                savedHostAddresses.put(host, address);
            }
            host.getBrowser().getUsedPorts().add(savedPort);
        }
    }

    /** Stores addresses for host. */
    private void storeProxyInfo() {
        savedInsideIps.clear();
        savedOutsideIps.clear();
        /* ports */
        final String sip = insidePortComboBox.getStringValue();
        savedInsidePort = sip;
        final String sop = outsidePortComboBox.getStringValue();
        savedOutsidePort = sop;
        /* ips */
        for (final Host host : getConfiguredProxyHosts()) {
            if (!isProxy(host)) {
                continue;
            }
            final Widget insideWi = insideIpComboBoxHash.get(host);
            if (insideWi != null) {
                final String defaultInsideIp = getDefaultInsideIp(host);
                final String insideIp = getIp(insideWi.getValue());
                if (insideIp == null) {
                    savedInsideIps.remove(host);
                } else {
                    savedInsideIps.put(host, insideIp);
                }
            }
            host.getBrowser().getUsedPorts().add(sip);

            final Widget outsideWi = outsideIpComboBoxHash.get(host);
            if (outsideWi != null) {
                final String outsideIp = getIp(outsideWi.getValue());
                if (outsideIp == null || "".equals(outsideIp)) {
                    savedOutsideIps.remove(host);
                } else {
                    savedOutsideIps.put(host, outsideIp);
                }
            }
            host.getBrowser().getUsedProxyPorts().add(sop);
        }
    }

    /** Return net interface with port as they appear in the drbd config. */
    private String getNetInterfaceWithPort(final String address,
                                           final String port) {
        return address + ":" + port;
    }

    /** Hide/show proxy panels for selected hosts. */
    private void setProxyPanels(final boolean wizard) {
        final Set<Host> visible = new HashSet<Host>();
        Map<Host, Widget> addressHash;
        Widget insidePortCB;
        Widget outsidePortCB;
        Widget portCB;
        if (wizard) {
            addressHash = addressComboBoxHashWizard;
            insidePortCB = insidePortComboBoxWizard;
            outsidePortCB = insidePortComboBoxWizard;
            portCB = portComboBoxWizard;
        } else {
            addressHash = addressComboBoxHash;
            insidePortCB = insidePortComboBox;
            outsidePortCB = insidePortComboBox;
            portCB = portComboBox;
        }
        for (final Host host : getHosts()) {
            final Host proxyHost =
                                getProxyHost(addressHash.get(host).getValue());
            if (proxyHost != null) {
                visible.add(proxyHost);
            }
        }
        final boolean isProxy = !visible.isEmpty();
        for (final Host pHost : getDrbdInfo().getAllProxyHosts()) {
            final String section = Tools.getString("DrbdResourceInfo.Proxy")
                                   + pHost.getName();
            enableSection(section, visible.contains(pHost), wizard);
        }
        enableSection(SECTION_PROXY, isProxy, wizard);
        enableSection(SECTION_PROXY_PORTS, isProxy, wizard);
        String portLabel;
        if (isProxy) {
            if (insidePortCB.isNew()
                && (savedInsidePort == null || "".equals(savedInsidePort))) {
                insidePortCB.setValue(
                                 Integer.toString(getDefaultInsidePort()));
            }
            if (outsidePortCB.isNew()
                && (savedOutsidePort == null || "".equals(savedOutsidePort))) {
                outsidePortCB.setValue(savedPort);
            }
            portLabel =
                   Tools.getString("DrbdResourceInfo.NetInterfacePortToProxy");

            getDrbdInfo().enableProxySection(wizard); /* never disable */
        } else {
            portLabel = Tools.getString("DrbdResourceInfo.NetInterfacePort");
        }
        portCB.getLabel().setText(portLabel);
    }

    /** Adds host address listener. */
    private void addHostAddressListener(final boolean wizard,
                                final MyButton thisApplyButton,
                                final Map<Host, Widget> newIpComboBoxHash,
                                final Map<Host, Widget> ipComboBoxHash) {
        for (final Host host : getHosts()) {
            Widget wi;
            Widget rwi;
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
            final Widget comboBox = wi;
            final Widget realComboBox = rwi;
            comboBox.addListeners(new WidgetListener() {
                @Override
                public void check(final Object value) {
                    checkParameterFields(comboBox,
                                         realComboBox,
                                         null,
                                         null,
                                         thisApplyButton);
                    setProxyPanels(wizard);
                    if (value instanceof ProxyNetInfo
                        && ((ProxyNetInfo) value).getNetInterface() == null) {
                        final int s = ProxyNetInfo.PROXY_PREFIX.length();
                        /* select the IP part */
                        comboBox.setAlwaysEditable(true);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                comboBox.select(
                                       s, s + NetInfo.IP_PLACEHOLDER.length());
                            }
                        });
                    } else if (value instanceof String) {
                        comboBox.setAlwaysEditable(true);
                    } else {
                        comboBox.setAlwaysEditable(false);
                    }
                    final Host proxyHost = getProxyHost(comboBox.getValue());
                    if (proxyHost != null
                        && savedInsideIps.get(proxyHost) == null) {
                        Map<Host, Widget> cb;
                        if (wizard) {
                            cb = insideIpComboBoxHashWizard;
                        } else {
                            cb = insideIpComboBoxHash;
                        }
                        cb.get(proxyHost).setValueAndWait(
                                                   getIp(comboBox.getValue()));
                    }
                }
            });
        }
    }

    /** Adds ip listeners. */
    private void addIpListeners(final boolean wizard,
                                final MyButton thisApplyButton,
                                final Map<Host, Widget> newIpComboBoxHash,
                                final Map<Host, Widget> ipComboBoxHash) {
        for (final Host host : getDrbdInfo().getAllProxyHosts()) {
            Widget wi;
            Widget rwi;
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
            final Widget comboBox = wi;
            final Widget realComboBox = rwi;
            comboBox.addListeners(new WidgetListener() {
                                @Override
                                public void check(final Object value) {
                                    checkParameterFields(comboBox,
                                                         realComboBox,
                                                         null,
                                                         null,
                                                         thisApplyButton);
                                }
                            });
        }
    }

    /** Adds port listeners. */
    private void addPortListeners(final boolean wizard,
                                  final MyButton thisApplyButton,
                                  final Widget newPortWi,
                                  final Widget portWi) {
        Widget pwi;
        Widget prwi;
        if (wizard) {
            pwi = newPortWi;
            prwi = portWi;
        } else {
            pwi = portWi;
            prwi = null;
        }
        final Widget comboBox = pwi;
        final Widget realComboBox = prwi;
        pwi.addListeners(new WidgetListener() {
                                @Override
                                public void check(final Object value) {
                                    checkParameterFields(comboBox,
                                                         realComboBox,
                                                         null,
                                                         null,
                                                         thisApplyButton);
                                }
                            });
    }

    /** Stores values in the combo boxes in the component c. */
    protected void storeComboBoxValues(final String[] params) {
        super.storeComboBoxValues(params);
        storeHostAddresses();
        storeProxyInfo();
    }

    /** Returns true if volume exists. */
    public DrbdVolumeInfo getDrbdVolumeInfo(final String volumeNr) {
        if (volumeNr == null) {
            return null;
        }
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
            if (volumeNr.equals(dvi.getName())) {
                return dvi;
            }
        }
        return null;
    }

    /** Remove drbd volume. Returns true if there are no more volumes. */
    public boolean removeDrbdVolume(final DrbdVolumeInfo dvi) {
        drbdVolumes.remove(dvi);
        return drbdVolumes.isEmpty();
    }

    /** Removes this object. */
    public void removeMyself(final boolean testOnly) {
        super.removeMyself(testOnly);
        getBrowser().getDrbdXML().removeResource(getName());
        final Set<Host> hosts = getHosts();
        for (final Host host : hosts) {
            host.getBrowser().getUsedPorts().remove(
                                                portComboBox.getStringValue());
            host.getBrowser().getUsedPorts().remove(
                                          insidePortComboBox.getStringValue());
            host.getBrowser().getUsedProxyPorts().remove(
                                         outsidePortComboBox.getStringValue());
        }

        final Map<String, DrbdResourceInfo> drbdResHash =
                                                getBrowser().getDrbdResHash();
        final DrbdResourceInfo dri = drbdResHash.get(getName());
        drbdResHash.remove(getName());
        getBrowser().putDrbdResHash();
        if (dri != null) {
            dri.setName(null);
        }
        if (!testOnly) {
            removeNode();
        }
        getDrbdInfo().reloadDRBDResourceComboBoxes();
    }

    /** Returns DRBD volumes. */
    public Set<DrbdVolumeInfo> getDrbdVolumes() {
        return drbdVolumes;
    }

    /** Reload combo boxes. */
    @Override
    public void reloadComboBoxes() {
        super.reloadComboBoxes();
        String param = DRBD_RES_PARAM_AFTER;
        if (!getDrbdInfo().atLeastVersion("8.4")) {
            param = DRBD_RES_PARAM_AFTER_8_3;
        }
        final List<Info> l = new ArrayList<Info>();
        final String defaultItem = getParamSaved(param);
        final StringInfo di = new StringInfo(
                                    Tools.getString("ClusterBrowser.None"),
                                    "-1",
                                    getBrowser());
        l.add(di);
        final Map<String, DrbdResourceInfo> drbdResHash =
                                            getBrowser().getDrbdResHash();
        for (final String drbdRes : drbdResHash.keySet()) {
            final DrbdResourceInfo r = drbdResHash.get(drbdRes);
            DrbdResourceInfo odri = r;
            boolean cyclicRef = false;
            while ((odri = drbdResHash.get(
                   odri.getParamSaved(param))) != null) {
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
            final String value = resyncAfterParamWi.getStringValue();
            resyncAfterParamWi.reloadComboBox(value,
                                              l.toArray(new Info[l.size()]));
        }
    }
    /** Creates popup for the block device. */
    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
            final MyMenu volumesMenu = new MyMenu(
                            dvi.toString(),
                            new AccessMode(ConfigData.AccessType.RO, false),
                            new AccessMode(ConfigData.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;
                private final Lock mUpdateLock = new ReentrantLock();

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void update() {
                    final Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (mUpdateLock.tryLock()) {
                                try {
                                    updateThread();
                                } finally {
                                    mUpdateLock.unlock();
                                }
                            }
                        }
                    });
                    t.start();
                }

                public void updateThread() {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setEnabled(false);
                        }
                    });
                    Tools.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            removeAll();
                        }
                    });
                    final List<UpdatableItem> volumeMenus =
                                    new ArrayList<UpdatableItem>();
                    for (final UpdatableItem u : dvi.createPopup()) {
                        volumeMenus.add(u);
                        u.update();
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            for (final UpdatableItem u
                                                 : volumeMenus) {
                                add((JMenuItem) u);
                            }
                        }
                    });
                    super.update();
                }
            };
            items.add((UpdatableItem) volumesMenu);
        }
        return items;
    }

    /** Sets that this drbd resource is used by crm. */
    public void setUsedByCRM(final ServiceInfo isUsedByCRM) {
        this.isUsedByCRM = isUsedByCRM;
    }

    /** Returns whether this drbd resource is used by crm. */
    public boolean isUsedByCRM() {
        return isUsedByCRM != null && isUsedByCRM.isManaged(false);
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
            return ((ProxyNetInfo) o).getStringValue();
        } else if (isProxyAddress(o.toString())) {
            final Matcher drbdpM = DRBDP_ADDRESS.matcher(o.toString());
            if (drbdpM.matches()) {
                return drbdpM.group(1);
            }
            return null;
        } else if (o instanceof String) {
            return (String) o;
        } else {
            return ((NetInfo) o).getStringValue();
        }
    }

    /** Get proxy from ip combo box value. Null, if it's not a proxy. */
    private Host getProxyHost(final Object o) {
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
                return getDrbdInfo().getProxyHostByName(name);
            }
        }
        return null;
    }

    /** Return default inside ip, that is the same as the "address" field. */
    private String getDefaultInsideIp(final Host host) {
        final Widget wi = addressComboBoxHash.get(host);
        if (wi != null) {
            return getIp(wi.getValue());
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
            for (final String usedPortS
                                    : host.getBrowser().getUsedProxyPorts()) {
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
    List<String> getPossibleDrbdPorts(int defaultPortInt) {
        int i = 0;
        final List<String> drbdPorts = new ArrayList<String>();
        drbdPorts.add(null);
        while (i < 10) {
            final String port = Integer.toString(defaultPortInt);
            boolean contains = false;
            for (final Host host : getHosts()) {
                if (host.getBrowser().getUsedPorts().contains(port)) {
                    contains = true;
                }
            }
            if (!contains || i == 0) {
                drbdPorts.add(port);
                i++;
            }
            defaultPortInt++;
        }
        return drbdPorts;
    }

    /** Return default proxy inside port, that is smaller than the drbd port.
     */
    private int getDefaultInsidePort() {
        final String insideDefaultPort = savedInsidePort;
        int insideDefaultPortInt;
        if (insideDefaultPort == null || "".equals(insideDefaultPort)) {
            if (savedPort == null || "".equals(savedPort)) {
                insideDefaultPortInt = getLowestUnusedPort() + 1;
            } else {
                insideDefaultPortInt = Integer.parseInt(savedPort) + 1;
            }
        } else {
            insideDefaultPortInt = Integer.parseInt(insideDefaultPort);
        }
        return insideDefaultPortInt;
    }

    /**
     * Return if this resource uses proxy on the specified host.
     */
    public boolean isProxy(final Host host) {
        final Widget cb = addressComboBoxHash.get(host);
        if (cb != null) {
            return isProxyAddress(cb.getValue());
        }
        return false;
    }

    /**
     * Return true if the address is a proxy address.
     */
    private boolean isProxyAddress(final Object address) {
        return address instanceof ProxyNetInfo
               || (address != null
                   && address.toString().startsWith(
                                                 ProxyNetInfo.PROXY_PREFIX));
    }

    /** Reset the info panel. */
    @Override
    public void resetInfoPanel() {
        super.resetInfoPanel();
        infoPanel = null;
    }

    /**
     * Return proxy hosts that are used in this resource. */
    private Set<Host> getConfiguredProxyHosts() {
        final Set<Host> proxyHosts = new HashSet<Host>();
        final Map<Host, Widget> addressHash = addressComboBoxHash;
        for (final Host host : getHosts()) {
            final Widget addrW = addressHash.get(host);
            if (addrW == null) {
                continue;
            }
            final Host proxyHost = getProxyHost(addrW.getValue());
            if (proxyHost != null) {
                proxyHosts.add(proxyHost);
            }
        }
        return proxyHosts;
    }
}
