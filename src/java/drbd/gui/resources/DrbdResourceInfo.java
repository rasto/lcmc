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
import drbd.gui.Browser;
import drbd.gui.HostBrowser;
import drbd.gui.ClusterBrowser;
import drbd.gui.GuiComboBox;
import drbd.gui.SpringUtilities;
import drbd.data.resources.DrbdResource;
import drbd.data.Host;
import drbd.data.DrbdXML;
import drbd.data.DRBDtestData;
import drbd.data.AccessMode;
import drbd.data.ConfigData;
import drbd.utilities.Tools;
import drbd.utilities.ButtonCallback;
import drbd.utilities.DRBD;
import drbd.utilities.MyButton;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Enumeration;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

/**
 * this class holds info data, menus and configuration
 * for a drbd resource.
 */
public final class DrbdResourceInfo extends DrbdGuiInfo {
    /** List of volumes. */
    private final Set<DrbdVolumeInfo> drbdVolumes =
                                        new LinkedHashSet<DrbdVolumeInfo>();
    /** Cache for getInfoPanel method. */
    private JComponent infoPanel = null;
    /** Whether the meta-data has to be created or not. */
    private boolean haveToCreateMD = false;
    /** Name of the drbd resource name parameter. */
    static final String DRBD_RES_PARAM_NAME = "name";
    /** A map from host to the combobox with addresses. */
    private final Map<Host, GuiComboBox> addressComboBoxHash =
                                             new HashMap<Host, GuiComboBox>();
    /** A map from host to the combobox with addresses for wizard. */
    private final Map<Host, GuiComboBox> addressComboBoxHashWizard =
                                             new HashMap<Host, GuiComboBox>();
    /** A map from host to stored addresses. */
    private final Map<Host, String> savedHostAddresses =
                                                 new HashMap<Host, String>();
    /** Saved port, that is the same for both hosts. */
    private String savedPort = null;
    /** Port combo box. */
    private GuiComboBox portComboBox = null;
    /** Port combo box wizard. */
    private GuiComboBox portComboBoxWizard = null;

    /**
     * Prepares a new <code>DrbdResourceInfo</code> object.
     */
    DrbdResourceInfo(final String name,
                     final Browser browser) {
        super(name, browser);
        setResource(new DrbdResource(name));
        //getResource().setValue(DRBD_RES_PARAM_DEV, drbdDev);
    }

    /** Add a drbd volume. */
    public void addDrbdVolume(final DrbdVolumeInfo drbdVolume) {
        drbdVolumes.add(drbdVolume);
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
        for (final Host host : getCluster().getHostsArray()) {
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
                final GuiComboBox acb = addressComboBoxHash.get(host);
                final GuiComboBox pcb = portComboBox;
                if (acb != null && pcb != null) {
                    final NetInfo ni = (NetInfo) acb.getValue();
                    if (ni == null) {
                        throw new Exceptions.DrbdConfigException(
                                    "Address not defined in "
                                    + getCluster().getName()
                                    + " (" + getName() + ")");
                    }
                    config.append("\n\t\taddress\t\t");
                    config.append(getDrbdNetInterfaceWithPort(
                                            ni.getNetInterface().getIp(),
                                            pcb.getStringValue()));
                    config.append(";");

                }
                config.append("\n\t}\n");
            }
        } config.append("}");
        getDrbdResource().setCommited(true);
        return config.toString();
    }

    /** Clears info panel cache. */
    @Override public boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
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
        if (DRBD_RES_PARAM_AFTER.equals(param)
            || DRBD_RES_PARAM_AFTER_8_3.equals(param)) {
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
            && DRBD_RES_PARAM_NAME.equals(param)) {
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
        } else if (DRBD_RES_PARAM_AFTER.equals(param)
                   || DRBD_RES_PARAM_AFTER_8_3.equals(param)) {
            // TODO: has to be reloaded
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
            Tools.invokeAndWait(new Runnable() {
                @Override public void run() {
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
    @Override public JComponent getInfoPanel() {
        //getBrowser().getDrbdGraph().pickInfo(this);
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
                    getDrbdInfo().createDrbdConfig(true);
                    for (final Host h : getCluster().getHostsArray()) {
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
        final String[] params = getParametersFromXML();
        /* address combo boxes */
        addHostAddresses(optionsPanel,
                         ClusterBrowser.SERVICE_LABEL_WIDTH,
                         ClusterBrowser.SERVICE_FIELD_WIDTH,
                         false);
        addHostAddressListeners(false, getApplyButton());
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

    //@Override int hashCode() {
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

    /** Sets stored parameters. */
    public void setParameters() {
        getDrbdResource().setCommited(true);
        final DrbdXML dxml = getBrowser().getDrbdXML();
        final String resName = getResource().getName();
        for (final String sectionString : dxml.getSections()) {
            /* remove -options */
            final String section = sectionString.replaceAll("-options$", "");
            for (final String param : dxml.getSectionParams(section)) {
                //if (DRBD_RES_PARAM_DEV.equals(param)) {
                //    continue;
                //}
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
        /* set networks addresses */
        String hostPort = null;
        final boolean infoPanelOk = infoPanel != null;
        for (final Host host : getBrowser().getClusterHosts()) {
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
                    final GuiComboBox cb = addressComboBoxHash.get(host);
                    if (cb != null) {
                        cb.setValue(hostAddress);
                    }
                }
            }
        }

        /* set port */
        if (!Tools.areEqual(hostPort, savedPort)) {
            savedPort = hostPort;
            for (final Host host : getBrowser().getClusterHosts()) {
                host.getBrowser().getDrbdVIPortList().add(savedPort);
            }
            if (infoPanelOk) {
                final GuiComboBox cb = portComboBox;
                if (cb != null) {
                    if (hostPort == null) {
                        cb.setValue(GuiComboBox.NOTHING_SELECTED);
                    } else {
                        cb.setValue(hostPort);
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
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
            if (!dvi.checkResourceFieldsCorrect(param,
                                                dvi.getParametersFromXML(),
                                                fromDrbdInfo,
                                                true)) {
                correct = false;
            }
        }
        final String port = portComboBox.getStringValue();
        final GuiComboBox pcb = portComboBox;
        final GuiComboBox pwizardCb = portComboBoxWizard;
        if (Tools.isNumber(port)) {
            final long p = Long.parseLong(port);
            if (p >= 0 && p < 65536) {
                pcb.setBackground(null, savedPort, true);
                if (pwizardCb != null) {
                    pwizardCb.setBackground(null, savedPort, true);
                }
            } else {
                correct = false;
                pcb.wrongValue();
                if (pwizardCb != null) {
                    pwizardCb.wrongValue();
                }
            }
        } else {
            correct = false;
            pcb.wrongValue();
            if (pwizardCb != null) {
                pwizardCb.wrongValue();
            }
        }
        for (final Host host : addressComboBoxHash.keySet()) {
            final GuiComboBox cb = addressComboBoxHash.get(host);
            final GuiComboBox wizardCb = addressComboBoxHashWizard.get(host);
            if (cb.getValue() == null) {
                correct = false;
                cb.wrongValue();
                if (wizardCb != null) {
                    wizardCb.wrongValue();
                }
            } else {
                cb.setBackground(null, savedHostAddresses.get(host), true);
                if (wizardCb != null) {
                    wizardCb.setBackground(null,
                                           savedHostAddresses.get(host),
                                           true);
                }
            }
        }
        return super.checkResourceFieldsCorrect(param, params) && correct;
    }

    /** Revert all values. */
    @Override public void revert() {
        super.revert();
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
            for (final BlockDevInfo bdi : dvi.getBlockDevInfos()) {
                if (bdi != null) {
                    bdi.revert();
                }
            }
        }
        for (final Host host : addressComboBoxHash.keySet()) {
            final GuiComboBox cb = addressComboBoxHash.get(host);
            final String haSaved = savedHostAddresses.get(host);
            if (!Tools.areEqual(cb.getValue(), haSaved)) {
                final GuiComboBox wizardCb =
                                         addressComboBoxHashWizard.get(host);
                if (wizardCb == null) {
                    cb.setValue(haSaved);
                } else {
                    wizardCb.setValue(haSaved);
                }
            }
        }
        final GuiComboBox pcb = portComboBox;
        if (!pcb.getStringValue().equals(savedPort)) {
            final GuiComboBox wizardCb = portComboBoxWizard;
            if (wizardCb == null) {
                pcb.setValue(savedPort);
            } else {
                wizardCb.setValue(savedPort);
            }
        }
    }

    /** Sets if dialog was started. It disables the apply button. */
    @Override public void setDialogStarted(final boolean dialogStarted) {
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
                                 final boolean wizard) {
        int rows = 0;
        if (wizard) {
            addressComboBoxHashWizard.clear();
        } else {
            addressComboBoxHash.clear();
        }

        final JPanel panel =
             getParamPanel(Tools.getString("DrbdResourceInfo.HostAddresses"));
        panel.setLayout(new SpringLayout());

        for (final Host host : getBrowser().getClusterHosts()) {
            final GuiComboBox cb =
                    new GuiComboBox(
                        null,
                        getNetInterfaces(host.getBrowser()),
                        null, /* units */
                        null, /* type */
                        null, /* regexp */
                        rightWidth,
                        null, /* abbreviations */
                        new AccessMode(ConfigData.AccessType.ADMIN, false));
            cb.setEditable(true);
            final String haSaved = savedHostAddresses.get(host);
            cb.setValue(haSaved);
            if (wizard) {
                addressComboBoxHashWizard.put(host, cb);
            } else {
                addressComboBoxHash.put(host, cb);
            }

        }

        /* host addresses combo boxes */
        for (final Host host : getBrowser().getClusterHosts()) {
            GuiComboBox cb = addressComboBoxHash.get(host);
            if (wizard) {
                cb = addressComboBoxHashWizard.get(host);
            } else {
                cb = addressComboBoxHash.get(host);
            }
            final JLabel label = new JLabel(
                            Tools.getString("DrbdResourceInfo.AddressOnHost")
                            + host.getName());
            cb.setLabel(label, "");
            addField(panel,
                     label,
                     cb,
                     leftWidth,
                     rightWidth,
                     0);
            rows++;
        }

        /* Port */
        final List<String> drbdVIPorts = new ArrayList<String>();
        String dp = savedPort;
        int index = -1;
        for (final Host host : getBrowser().getClusterHosts()) {
            for (final String port : host.getBrowser().getDrbdVIPortList()) {
                if (Tools.isNumber(port)) {
                    final int p = Integer.parseInt(port);
                    if (index < 0 || p < index) {
                        index = p;
                    }
                }
            }
        }
        if (index < 0) {
            index = Tools.getDefaultInt("HostBrowser.DrbdNetInterfacePort");
        }
        if (dp == null) {
            dp = Integer.toString(index);
        } else {
            drbdVIPorts.add(dp);
        }
        int i = 0;
        while (i < 10) {
            final String port = Integer.toString(index);
            boolean contains = false;
            for (final Host host : getBrowser().getClusterHosts()) {
                if (host.getBrowser().getDrbdVIPortList().contains(port)) {
                    contains = true;
                }
            }
            if (!contains) {
                drbdVIPorts.add(port);
                i++;
            }
            index++;
        }
        final String defaultPort = drbdVIPorts.get(0);
        final GuiComboBox pcb = new GuiComboBox(
                   defaultPort,
                   drbdVIPorts.toArray(new String[drbdVIPorts.size()]),
                   null, /* units */
                   null, /* type */
                   "^\\d*$",
                   leftWidth,
                   null, /* abbrv */
                   new AccessMode(ConfigData.AccessType.ADMIN, false));
        pcb.setAlwaysEditable(true);
        final JLabel label = new JLabel(
                        Tools.getString("DrbdResourceInfo.NetInterfacePort"));
        addField(panel,
                 label,
                 pcb,
                 leftWidth,
                 rightWidth,
                 0);
        pcb.setLabel(label, "");
        if (wizard) {
            portComboBoxWizard = pcb;
            portComboBox.setValue(defaultPort);
        } else {
            portComboBox = pcb;
        }
        rows++;

        SpringUtilities.makeCompactGrid(panel, rows, 2, /* rows, cols */
                                        1, 1,           /* initX, initY */
                                        1, 1);          /* xPad, yPad */
        optionsPanel.add(panel);
    }

    /** Ruturns all net interfaces. */
    private Object[] getNetInterfaces(final HostBrowser hostBrowser) {
        final List<Object> list = new ArrayList<Object>();

        list.add(null);
        final Enumeration e = hostBrowser.getNetInterfacesNode().children();

        while (e.hasMoreElements()) {
            final Info i =
              (Info) ((DefaultMutableTreeNode) e.nextElement()).getUserObject();
            list.add(i);
        }
        return list.toArray(new Object[list.size()]);
    }

    /** Returns true if some of the addresses have changed. */
    private boolean checkHostAddressesFieldsChanged() {
        boolean changed = false;
        for (final Host host : getBrowser().getClusterHosts()) {
            final GuiComboBox cb = addressComboBoxHash.get(host);
            if (cb == null) {
                continue;
            }
            final String haSaved = savedHostAddresses.get(host);
            final Object value = cb.getValue();
            if (!Tools.areEqual(haSaved, value)) {
                changed = true;
            }
            cb.setBackground(null, haSaved, false);
        }
        /* port */
        final GuiComboBox pcb = portComboBox;
        if (pcb != null) {
            if (!Tools.areEqual(savedPort, pcb.getValue())) {
                changed = true;
            }
            pcb.setBackground(null,
                              savedPort,
                              false);
        }
        return changed;
    }

    /** Stores addresses for host. */
    private void storeHostAddresses() {
        savedHostAddresses.clear();
        /* port */
        savedPort = portComboBox.getStringValue();
        /* addresses */
        for (final Host host : getBrowser().getClusterHosts()) {
            final GuiComboBox cb = addressComboBoxHash.get(host);
            final String address = cb.getStringValue();
            if (address == null || "".equals(address)) {
                savedHostAddresses.remove(host);
            } else {
                savedHostAddresses.put(host, address);
            }
            host.getBrowser().getDrbdVIPortList().add(savedPort);
        }
    }

    /** Return net interface with port as they appear in the drbd config. */
    private String getDrbdNetInterfaceWithPort(final String address,
                                               final String port) {
        return address + ":" + port;
    }

    /** Adds host address listeners. */
    public void addHostAddressListeners(final boolean wizard,
                                           final MyButton thisApplyButton) {
        final String[] params = getParametersFromXML();
        for (final Host host : getBrowser().getClusterHosts()) {
            GuiComboBox cb;
            GuiComboBox rcb;
            if (wizard) {
                cb = addressComboBoxHashWizard.get(host);
                rcb = addressComboBoxHash.get(host);
            } else {
                cb = addressComboBoxHash.get(host);
                rcb = null;
            }
            final GuiComboBox comboBox = cb;
            final GuiComboBox realComboBox = rcb;
            comboBox.addListeners(
                new ItemListener() {
                    @Override public void itemStateChanged(final ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            final Thread thread = new Thread(new Runnable() {
                                @Override public void run() {
                                    if (e.getStateChange()
                                        == ItemEvent.SELECTED) {
                                        checkParameterFields(comboBox,
                                                             realComboBox,
                                                             null,
                                                             null,
                                                             thisApplyButton);
                                    }
                                }
                            });
                            thread.start();
                        }
                    }
                },

                new DocumentListener() {
                    private void check() {
                        final Thread thread = new Thread(new Runnable() {
                            @Override public void run() {
                                checkParameterFields(comboBox,
                                                     realComboBox,
                                                     null,
                                                     null,
                                                     thisApplyButton);
                            }
                        });
                        thread.start();
                    }

                    @Override public void insertUpdate(final DocumentEvent e) {
                        check();
                    }

                    @Override public void removeUpdate(final DocumentEvent e) {
                        check();
                    }

                    @Override public void changedUpdate(final DocumentEvent e) {
                        check();
                    }
                }
            );
        }
        GuiComboBox pcb;
        GuiComboBox prcb;
        if (wizard) {
            pcb = portComboBoxWizard;
            prcb = portComboBox;
        } else {
            pcb = portComboBox;
            prcb = null;
        }
        final GuiComboBox comboBox = pcb;
        final GuiComboBox realComboBox = prcb;
        pcb.addListeners(
            new ItemListener() {
                @Override public void itemStateChanged(final ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override public void run() {
                                if (e.getStateChange() == ItemEvent.SELECTED) {
                                    checkParameterFields(comboBox,
                                                         realComboBox,
                                                         null,
                                                         null,
                                                         thisApplyButton);
                                }
                            }
                        });
                        thread.start();
                    }
                }
            },

            new DocumentListener() {
                private void check() {
                    final Thread thread = new Thread(new Runnable() {
                        @Override public void run() {
                            checkParameterFields(comboBox,
                                                 realComboBox,
                                                 null,
                                                 null,
                                                 thisApplyButton);
                        }
                    });
                    thread.start();
                }

                @Override public void insertUpdate(final DocumentEvent e) {
                    check();
                }

                @Override public void removeUpdate(final DocumentEvent e) {
                    check();
                }

                @Override public void changedUpdate(final DocumentEvent e) {
                    check();
                }
            });
    }

    /** Stores values in the combo boxes in the component c. */
    protected void storeComboBoxValues(final String[] params) {
        super.storeComboBoxValues(params);
        storeHostAddresses();
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
        for (final Host host : getCluster().getHostsArray()) {
            host.getBrowser().getDrbdVIPortList().remove(savedPort);
        }

        final Map<String, DrbdResourceInfo> drbdResHash =
                                                getBrowser().getDrbdResHash();
        final DrbdResourceInfo dri = drbdResHash.get(getName());
        drbdResHash.remove(getName());
        getBrowser().putDrbdResHash();
        dri.setName(null);
    }

    /** Returns DRBD volumes. */
    public Set<DrbdVolumeInfo> getDrbdVolumes() {
        return drbdVolumes;
    }
}
