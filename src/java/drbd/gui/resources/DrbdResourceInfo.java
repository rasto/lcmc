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
import drbd.data.resources.DrbdResource;
import drbd.data.Host;
import drbd.data.DrbdXML;
import drbd.data.DRBDtestData;
import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.utilities.Tools;
import drbd.utilities.ButtonCallback;
import drbd.utilities.DRBD;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.List;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;

/**
 * this class holds info data, menus and configuration
 * for a drbd resource.
 */
public final class DrbdResourceInfo extends DrbdGuiInfo {
    /** Volumes volume nr - list of blockdevices. */
    private final Set<DrbdVolumeInfo> drbdVolumes =
                                        new LinkedHashSet<DrbdVolumeInfo>();
    /** Cache for getInfoPanel method. */
    private JComponent infoPanel = null;
    /** Whether the meta-data has to be created or not. */
    private boolean haveToCreateMD = false;
    /** Name of the drbd resource name parameter. */
    static final String DRBD_RES_PARAM_NAME = "name";

    /**
     * Prepares a new <code>DrbdResourceInfo</code> object.
     */
    DrbdResourceInfo(final String name,
                     final String drbdDev,
                     final Browser browser) {
        super(name, browser);
        setResource(new DrbdResource(name, drbdDev));
        //getResource().setValue(DRBD_RES_PARAM_DEV, drbdDev);
    }

    /** Add a drbd volume. */
    public void addDrbdVolume(final DrbdVolumeInfo drbdVolume) {
        drbdVolumes.add(drbdVolume);
    }


    ///** Returns device name, like /dev/drbd0. */
    //@Override public String getDevice() {
    //    return getDrbdResource().getDevice();
    //}

    /** Creates and returns drbd config for resources. */
    String drbdResourceConfig() throws Exceptions.DrbdConfigException {
        final StringBuilder config = new StringBuilder(50);
        config.append("resource " + getName() + " {\n");
        /* protocol... */
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
            config.append(drbdSectionsConfig());
        } catch (Exceptions.DrbdConfigException dce) {
            throw dce;
        }
        /* startup disk syncer net */
        // TODO:
        //config.append(blockDevInfo1.drbdNodeConfig(getName(), getDevice()));
        //config.append('\n');
        //config.append(blockDevInfo2.drbdNodeConfig(getName(), getDevice()));
        config.append('}');
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
        //} else if (DRBD_RES_PARAM_DEV.equals(param)) {
        //    final List<String> drbdDevices = new ArrayList<String>();
        //    if (getParamSaved(DRBD_RES_PARAM_DEV) == null) {
        //        final String defaultItem =
        //                getDrbdResource().getDefaultValue(DRBD_RES_PARAM_DEV);
        //        drbdDevices.add(defaultItem);
        //        int i = 0;
        //        int index = 0;
        //        while (i < 11) {
        //            final String drbdDevStr = "/dev/drbd"
        //                                      + Integer.toString(index);
        //            final Map<String, DrbdResourceInfo> drbdDevHash =
        //                                          getBrowser().getDrbdDevHash();
        //            if (!drbdDevHash.containsKey(drbdDevStr)) {
        //                drbdDevices.add(drbdDevStr);
        //                i++;
        //            }
        //            getBrowser().putDrbdDevHash();
        //            index++;
        //        }
        //        paramCb = new GuiComboBox(defaultItem,
        //                                  drbdDevices.toArray(
        //                                       new String[drbdDevices.size()]),
        //                                  null, /* units */
        //                                  null, /* type */
        //                                  null, /* regexp */
        //                                  width,
        //                                  null, /* abbrv */
        //                                  new AccessMode(
        //                                   getAccessType(param),
        //                                   isEnabledOnlyInAdvancedMode(param)));
        //        paramCb.setEditable(true);
        //    } else {
        //        final String defaultItem = getDevice();
        //        String regexp = null;
        //        if (isInteger(param)) {
        //            regexp = "^-?\\d*$";
        //        }
        //        paramCb = new GuiComboBox(
        //                               defaultItem,
        //                               getResource().getPossibleChoices(param),
        //                               null, /* units */
        //                               null, /* type */
        //                               regexp,
        //                               width,
        //                               null, /* abbrv */
        //                               new AccessMode(
        //                                   getAccessType(param),
        //                                   isEnabledOnlyInAdvancedMode(param)));
        //    }
        //    paramCb.setEnabled(!getDrbdResource().isCommited());
        //    paramComboBoxAdd(param, prefix, paramCb);
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
            //if (defaultItem == null || "-1".equals(defaultItem)) {
            //    defaultItem = Tools.getString("ClusterBrowser.None");
            //} else if (defaultItem != null) {
            //    final DrbdResourceInfo dri = drbdResHash.get(defaultItem);
            //    if (dri != null) {
            //        defaultItem = dri.getDevice();
            //    }
            //}

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
            //getBrowser().getDrbdDevHash().remove(getDevice());
            //getBrowser().putDrbdDevHash();
            storeComboBoxValues(params);

            final String name = getParamSaved(DRBD_RES_PARAM_NAME);
            //final String drbdDevStr = getParamSaved(DRBD_RES_PARAM_DEV);
            getDrbdResource().setName(name);
            setName(name);
            //getDrbdResource().setDevice(drbdDevStr);

            getBrowser().getDrbdResHash().put(name, this);
            getBrowser().putDrbdResHash();
            //getBrowser().getDrbdDevHash().put(drbdDevStr, this);
            //getBrowser().putDrbdDevHash();
            getBrowser().getDrbdGraph().repaint();
            getDrbdResource().setCommited(true);
            getDrbdInfo().setAllApplyButtons();
        }
    }

    /** Set all apply buttons. */
    void setAllApplyButtons() {
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
            for (final BlockDevInfo bdi : dvi.getBlockDevInfos()) {
                if (bdi != null) {
                    bdi.storeComboBoxValues(bdi.getParametersFromXML());
                    bdi.setApplyButtons(null, bdi.getParametersFromXML());
                }
            }
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
        //getResource().setValue(DRBD_RES_PARAM_DEV, getDevice());

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
        //                                                     value.toString());
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

    ///**
    // * Returns the device name that is used as the string value of
    // * the device in the filesystem resource.
    // */
    //@Override public String getStringValue() {
    //    return getDevice();
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
        final DrbdXML dxml = getBrowser().getDrbdXML();
        final String resName = getResource().getName();
        for (String section : dxml.getSections()) {
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
        for (final DrbdVolumeInfo dvi : drbdVolumes) {
            for (final BlockDevInfo bdi : dvi.getBlockDevInfos()) {
                if (bdi != null
                    && bdi.checkResourceFieldsChanged(
                                                  param,
                                                  bdi.getParametersFromXML(),
                                                  fromDrbdInfo,
                                                  true)) {
                    changed = true;
                }
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
            for (final BlockDevInfo bdi : dvi.getBlockDevInfos()) {
                if (bdi != null
                    && !bdi.getBlockDevice().isNew()
                    && !bdi.checkResourceFieldsCorrect(
                                                    param,
                                                    bdi.getParametersFromXML(),
                                                    fromDrbdInfo,
                                                    true)) {
                    correct = false;
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
}
