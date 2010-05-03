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
import drbd.data.Cluster;
import drbd.data.resources.DrbdResource;
import drbd.data.Host;
import drbd.data.DrbdXML;
import drbd.data.DRBDtestData;
import drbd.data.ConfigData;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;
import drbd.utilities.Unit;
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
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;

/**
 * this class holds info data, menus and configuration
 * for a drbd resource.
 */
public class DrbdResourceInfo extends EditableInfo
                              implements CommonDeviceInterface {
    /** BlockDevInfo object of the first block device. */
    private final BlockDevInfo blockDevInfo1;
    /** BlockDevInfo object of the second block device. */
    private final BlockDevInfo blockDevInfo2;
    /**
     * Whether the block device is used by heartbeat via Filesystem service.
     */
    private boolean isUsedByCRM;
    /** Cache for getInfoPanel method. */
    private JComponent infoPanel = null;
    /** Whether the meta-data has to be created or not. */
    private boolean haveToCreateMD = false;
    /** Last created filesystem. */
    private String createdFs = null;
    /** Name of the drbd after parameter. */
    private static final String DRBD_RES_PARAM_AFTER = "after";

    /**
     * Prepares a new <code>DrbdResourceInfo</code> object.
     */
    public DrbdResourceInfo(final String name,
                            final String drbdDev,
                            final BlockDevInfo blockDevInfo1,
                            final BlockDevInfo blockDevInfo2,
                            final Browser browser) {
        super(name, browser);
        setResource(new DrbdResource(name, null)); // TODO: ?
        setResource(new DrbdResource(name, drbdDev)); // TODO: ?
        // TODO: drbdresource
        getResource().setValue(ClusterBrowser.DRBD_RES_PARAM_DEV, drbdDev);
        this.blockDevInfo1 = blockDevInfo1;
        this.blockDevInfo2 = blockDevInfo2;
    }

    /**
     * Returns device name, like /dev/drbd0.
     */
    public final String getDevice() {
        return getDrbdResource().getDevice();
    }

    /**
     * Returns browser object of this info.
     */
    protected final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /**
     * Returns menu icon for drbd resource.
     */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        return null;
    }

    /**
     * Returns cluster object to resource belongs.
     */
    public final Cluster getCluster() {
        return getBrowser().getCluster();
    }

    /**
     * Returns other block device in the drbd cluster.
     */
    public final BlockDevInfo getOtherBlockDevInfo(final BlockDevInfo bdi) {
        if (bdi.equals(blockDevInfo1)) {
            return blockDevInfo2;
        } else if (bdi.equals(blockDevInfo2)) {
            return blockDevInfo1;
        } else {
            return null;
        }
    }

    /**
     * Returns first block dev info.
     */
    public final BlockDevInfo getFirstBlockDevInfo() {
        return blockDevInfo1;
    }

    /**
     * Returns second block dev info.
     */
    public final BlockDevInfo getSecondBlockDevInfo() {
        return blockDevInfo2;
    }

    /**
     * Returns true if this is first block dev info.
     */
    public final boolean isFirstBlockDevInfo(final BlockDevInfo bdi) {
        return blockDevInfo1 == bdi;
    }

    /**
     * Creates drbd config for sections and returns it. Removes 'drbd: '
     * from the 'after' parameter.
     * TODO: move this out of gui
     */
    private String drbdSectionsConfig()
                                    throws Exceptions.DrbdConfigException {
        final StringBuffer config = new StringBuffer("");
        final DrbdXML dxml = getBrowser().getDrbdXML();
        final String[] sections = dxml.getSections();
        for (String section : sections) {
            if ("resource".equals(section) || "global".equals(section)) {
                // TODO: Tools.getString
                continue;
            }
            final String[] params = dxml.getSectionParams(section);

            if (params.length != 0) {
                final StringBuffer sectionConfig = new StringBuffer("");
                for (String param : params) {
                    //final String value = getResource().getValue(param);
                    final String value = getComboBoxValue(param);
                    if (value == null) {
                        Tools.debug(this, "section: " + section
                                           + ", param " + param + " ("
                                           + getName() + ") not defined");
                        throw new Exceptions.DrbdConfigException("param "
                                                + param + " (" + getName()
                                                + ") not defined");
                    }
                    if (!value.equals(getParamDefault(param))) {
                        if (isCheckBox(param)
                            || "booleanhandler".equals(getParamType(param))) {
                            if (value.equals(Tools.getString("Boolean.True"))) {
                                /* boolean parameter */
                                sectionConfig.append("\t\t" + param + ";\n");
                            }
                        } else if (DRBD_RES_PARAM_AFTER.equals(param)) {
                            /* after parameter */
                            /* we get drbd device here, so it is converted
                             * to the resource. */
                            if (!value.equals(Tools.getString(
                                                "ClusterBrowser.None"))) {
                                final String v =
                                     getBrowser().getDrbdDevHash().get(
                                                              value).getName();
                                sectionConfig.append("\t\t");
                                sectionConfig.append(param);
                                sectionConfig.append('\t');
                                sectionConfig.append(Tools.escapeConfig(v));
                                sectionConfig.append(";\n");
                            }
                        } else { /* name value parameter */
                            sectionConfig.append("\t\t");
                            sectionConfig.append(param);
                            sectionConfig.append('\t');
                            sectionConfig.append(Tools.escapeConfig(value));
                            sectionConfig.append(";\n");
                        }
                    }
                }

                if (sectionConfig.length() > 0) {
                    config.append("\t" + section + " {\n");
                    config.append(sectionConfig);
                    config.append("\t}\n\n");
                }
            }
        }
        return config.toString();
    }
    /**
     * Creates and returns drbd config for resources.
     *
     * TODO: move this out of gui
     */
    public final String drbdResourceConfig()
                                    throws Exceptions.DrbdConfigException {
        final StringBuffer config = new StringBuffer(50);
        config.append("resource " + getName() + " {\n");
        /* protocol... */
        final String[] params = getBrowser().getDrbdXML().getSectionParams(
                                                                   "resource");
        for (String param : params) {
            if ("device".equals(param)) {
                continue;
            }
            config.append('\t');
            config.append(param);
            config.append('\t');
            //config.append(getResource().getValue(param));
            config.append(getComboBoxValue(param));
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
        config.append('\n');
        config.append(blockDevInfo1.drbdNodeConfig(getName(), getDevice()));
        config.append('\n');
        config.append(blockDevInfo2.drbdNodeConfig(getName(), getDevice()));
        config.append("}\n");
        return config.toString();
    }

    /**
     * Clears info panel cache.
     */
    public final boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /**
     * Returns sync progress in percent.
     */
    public final String getSyncedProgress() {
        return blockDevInfo1.getBlockDevice().getSyncedProgress();
    }

    /**
     * Returns whether the cluster is syncing.
     */
    public final boolean isSyncing() {
        return blockDevInfo1.getBlockDevice().isSyncing()
               || blockDevInfo2.getBlockDevice().isSyncing();
    }

    /**
     * Returns whether the cluster is being verified.
     */
    public final boolean isVerifying() {
        return blockDevInfo1.getBlockDevice().isVerifying()
               || blockDevInfo2.getBlockDevice().isVerifying();
    }

    /**
     * Connect block device from the specified host.
     */
    public final void connect(final Host host, final boolean testOnly) {
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
    public final boolean isConnected(final boolean testOnly) {
        return blockDevInfo1.isConnected(testOnly)
               && blockDevInfo2.isConnected(testOnly);
    }

    /**
     * Returns whether any of the sides in the drbd resource are in
     * paused-sync state.
     */
    public final boolean isPausedSync() {
        return blockDevInfo1.getBlockDevice().isPausedSync()
               || blockDevInfo2.getBlockDevice().isPausedSync();
    }

    /**
     * Returns whether any of the sides in the drbd resource are in
     * split-brain.
     */
    public final boolean isSplitBrain() {
        return blockDevInfo1.getBlockDevice().isSplitBrain()
               || blockDevInfo2.getBlockDevice().isSplitBrain();
    }

    /**
     * Returns drbd graphical view.
     */
    public final JPanel getGraphicalView() {
        return getBrowser().getDrbdGraph().getGraphPanel();
    }

    /**
     * Returns the DrbdInfo object (for all drbds).
     */
    public final DrbdInfo getDrbdInfo() {
        return getBrowser().getDrbdGraph().getDrbdInfo();
    }

    /**
     * Returns all parameters.
     */
    public final String[] getParametersFromXML() {
        return getBrowser().getDrbdXML().getParameters();
    }

    /**
     * Checks the new value of the parameter if it is conforms to its type
     * and other constraints.
     */
    protected final boolean checkParam(final String param,
                                       final String newValue) {
        if (DRBD_RES_PARAM_AFTER.equals(param)) {
            /* drbdsetup xml syncer says it should be numeric, but in
               /etc/drbd.conf it is not. */
            return true;
        }
        return getBrowser().getDrbdXML().checkParam(param, newValue);
    }

    /**
     * Returns the default value for the drbd parameter.
     */
    protected final String getParamDefault(final String param) {
        return getBrowser().getDrbdXML().getParamDefault(param);
    }

    /**
     * Returns the preferred value for the drbd parameter.
     */
    protected final String getParamPreferred(final String param) {
        return getBrowser().getDrbdXML().getParamPreferred(param);
    }

    /**
     * Returns the possible values for the pulldown menus, if applicable.
     */
    protected final Object[] getParamPossibleChoices(final String param) {
        return getBrowser().getDrbdXML().getPossibleChoices(param);
    }

    /**
     * Returns the short description of the drbd parameter that is used as
     * a label.
     */
    protected final String getParamShortDesc(final String param) {
        return getBrowser().getDrbdXML().getParamShortDesc(param);
    }

    /**
     * Returns a long description of the parameter that is used for tool
     * tip.
     */
    protected final String getParamLongDesc(final String param) {
        return getBrowser().getDrbdXML().getParamLongDesc(param);
    }

    /**
     * Returns section to which this drbd parameter belongs.
     */
    protected final String getSection(final String param) {
        return getBrowser().getDrbdXML().getSection(param);
    }

    /** Returns whether this drbd parameter is required parameter. */
    protected final boolean isRequired(final String param) {
        return getBrowser().getDrbdXML().isRequired(param);
    }

    /** Returns whether this parameter is advanced. */
    protected final boolean isAdvanced(final String param) {
        if (!Tools.areEqual(getParamDefault(param),
                            getParamSaved(param))) {
            /* it changed, show it */
            return false;
        }
        return getBrowser().getDrbdXML().isAdvanced(param);
    }
    /** Returns access type of this parameter. */
    protected final ConfigData.AccessType getAccessType(final String param) {
        return getBrowser().getDrbdXML().getAccessType(param);
    }

    /**
     * Returns whether this drbd parameter is of integer type.
     */
    protected final boolean isInteger(final String param) {
        return getBrowser().getDrbdXML().isInteger(param);
    }

    /**
     * Returns whether this drbd parameter is of time type.
     */
    protected final boolean isTimeType(final String param) {
        /* not required */
        return false;
    }

    /**
     * Returns whether this parameter has a unit prefix.
     */
    protected final boolean hasUnitPrefix(final String param) {
        return getBrowser().getDrbdXML().hasUnitPrefix(param);
    }

    /**
     * Returns the long unit name.
     */
    protected final String getUnitLong(final String param) {
        return getBrowser().getDrbdXML().getUnitLong(param);
    }

    /**
     * Returns the default unit for the parameter.
     */
    protected final String getDefaultUnit(final String param) {
        return getBrowser().getDrbdXML().getDefaultUnit(param);
    }

    /**
     * Returns whether the parameter is of the boolean type and needs the
     * checkbox.
     */
    protected final boolean isCheckBox(final String param) {
        final String type = getBrowser().getDrbdXML().getParamType(param);
        if (type == null) {
            return false;
        }
        if (ClusterBrowser.DRBD_RES_BOOL_TYPE_NAME.equals(type)) {
            return true;
        }
        return false;
    }

    /**
     * Returns the type of the parameter (like boolean).
     */
    protected final String getParamType(final String param) {
        return getBrowser().getDrbdXML().getParamType(param);
    }

    /**
     * Returns the widget that is used to edit this parameter.
     */
    protected final GuiComboBox getParamComboBox(final String param,
                                                 final String prefix,
                                                 final int width) {
        GuiComboBox paramCb;
        final Object[] possibleChoices = getParamPossibleChoices(param);
        getResource().setPossibleChoices(param, possibleChoices);
        if (ClusterBrowser.DRBD_RES_PARAM_NAME.equals(param)) {
            String resName;
            if (getParamSaved(ClusterBrowser.DRBD_RES_PARAM_NAME) == null) {
                resName = getResource().getDefaultValue(
                                            ClusterBrowser.DRBD_RES_PARAM_NAME);
            } else {
                resName = getResource().getName();
            }
            paramCb = new GuiComboBox(resName,
                                      null, /* items */
                                      null, /* units */
                                      null, /* type */
                                      null, /* regexp */
                                      width,
                                      null, /* abbrv */
                                      getAccessType(param));
            paramCb.setEnabled(!getDrbdResource().isCommited());
            paramComboBoxAdd(param, prefix, paramCb);
        } else if (ClusterBrowser.DRBD_RES_PARAM_DEV.equals(param)) {
            final List<String> drbdDevices = new ArrayList<String>();
            if (getParamSaved(ClusterBrowser.DRBD_RES_PARAM_DEV) == null) {
                final String defaultItem =
                        getDrbdResource().getDefaultValue(
                                            ClusterBrowser.DRBD_RES_PARAM_DEV);
                drbdDevices.add(defaultItem);
                int i = 0;
                int index = 0;
                while (i < 11) {
                    final String drbdDevStr = "/dev/drbd"
                                              + Integer.toString(index);
                    if (!getBrowser().getDrbdDevHash().containsKey(
                                                                 drbdDevStr)) {
                        drbdDevices.add(drbdDevStr);
                        i++;
                    }
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
                                          getAccessType(param));
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
                                       getAccessType(param));
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
            if (defaultItem == null || "-1".equals(defaultItem)) {
                defaultItem = Tools.getString("ClusterBrowser.None");
            } else if (defaultItem != null) {
                final DrbdResourceInfo dri =
                                getBrowser().getDrbdResHash().get(defaultItem);
                if (dri != null) {
                    defaultItem = dri.getDevice();
                }
            }

            for (final String drbdRes
                                    : getBrowser().getDrbdResHash().keySet()) {
                final DrbdResourceInfo r =
                                    getBrowser().getDrbdResHash().get(drbdRes);
                DrbdResourceInfo odri = r;
                boolean cyclicRef = false;
                while ((odri = getBrowser().getDrbdResHash().get(
                       odri.getParamSaved(DRBD_RES_PARAM_AFTER))) != null) {
                    if (odri == this) {
                        cyclicRef = true;
                    }
                }
                if (r != this && !cyclicRef) {
                    l.add(r);
                }
            }
            paramCb = new GuiComboBox(defaultItem,
                                      l.toArray(new Info[l.size()]),
                                      null, /* units */
                                      null, /* type */
                                      null, /* regexp */
                                      width,
                                      null, /* abbrv */
                                      getAccessType(param));

            paramComboBoxAdd(param, prefix, paramCb);
        } else if (hasUnitPrefix(param)) {
            String selectedValue = getParamSaved(param);
            if (selectedValue == null) {
                selectedValue = getParamPreferred(param);
                if (selectedValue == null) {
                    selectedValue = getParamDefault(param);
                }
            }
            String unit = getUnitLong(param);
            if (unit == null) {
                unit = "";
            }

            final int index = unit.indexOf('/');
            String unitPart = "";
            if (index > -1) {
                unitPart = unit.substring(index);
            }

            final Unit[] units = {
                new Unit("", "", "Byte", "Bytes"),

                new Unit("K",
                         "k",
                         "KiByte" + unitPart,
                         "KiBytes" + unitPart),

                new Unit("M",
                         "m",
                         "MiByte" + unitPart,
                         "MiBytes" + unitPart),

                new Unit("G",
                         "g",
                         "GiByte" + unitPart,
                         "GiBytes" + unitPart),

                new Unit("s",
                         "s",
                         "Sector" + unitPart,
                         "Sectors" + unitPart)
            };

            String regexp = null;
            if (isInteger(param)) {
                regexp = "^-?\\d*$";
            }
            paramCb = new GuiComboBox(selectedValue,
                                      getPossibleChoices(param),
                                      units,
                                      GuiComboBox.Type.TEXTFIELDWITHUNIT,
                                      regexp,
                                      width,
                                      null, /* abbrv */
                                      getAccessType(param));

            paramComboBoxAdd(param, prefix, paramCb);
        } else {
            paramCb = super.getParamComboBox(param, prefix, width);
            if (possibleChoices != null
                && !getBrowser().getDrbdXML().isStringType(param)) {
                paramCb.setEditable(false);
            }
        }
        return paramCb;
    }

    /**
     * Returns the DrbdResource object of this drbd resource.
     */
    public DrbdResource getDrbdResource() {
        return (DrbdResource) getResource();
    }

    /**
     * Applies changes that user made to the drbd resource fields.
     */
    public final void apply(final boolean testOnly) {
        if (!testOnly) {
            final String[] params = getParametersFromXML();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    applyButton.setEnabled(false);
                }
            });
            getBrowser().getDrbdResHash().remove(getName());
            getBrowser().getDrbdDevHash().remove(getDevice());
            storeComboBoxValues(params);

            final String name = getParamSaved(
                                           ClusterBrowser.DRBD_RES_PARAM_NAME);
            final String drbdDevStr = getParamSaved(
                                            ClusterBrowser.DRBD_RES_PARAM_DEV);
            getDrbdResource().setName(name);
            setName(name);
            getDrbdResource().setDevice(drbdDevStr);

            getBrowser().getDrbdResHash().put(name, this);
            getBrowser().getDrbdDevHash().put(drbdDevStr, this);
            getBrowser().getDrbdGraph().repaint();
            checkResourceFields(null, params);
        }
    }

    /**
     * Returns panel with form to configure a drbd resource.
     */
    public final JComponent getInfoPanel() {
        getBrowser().getDrbdGraph().pickInfo(this);
        if (infoPanel != null) {
            return infoPanel;
        }
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;
            /**
             * Whether the whole thing should be enabled.
             */
            public final boolean isEnabled() {
                return true;
            }

            public final void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                getBrowser().getDrbdGraph().stopTestAnimation(applyButton);
                applyButton.setToolTipText(null);
            }

            public final void mouseOver() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                applyButton.setToolTipText(
                       Tools.getString("ClusterBrowser.StartingDRBDtest"));
                applyButton.setToolTipBackground(Tools.getDefaultColor(
                                "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                getBrowser().getDrbdGraph().startTestAnimation(applyButton,
                                                               startTestLatch);
                getBrowser().drbdtestLockAcquire();
                getBrowser().setDRBDtestData(null);
                apply(true);
                final Map<Host,String> testOutput =
                                         new LinkedHashMap<Host, String>();
                try {
                    getDrbdInfo().createDrbdConfig(true);
                    for (final Host h : getCluster().getHostsArray()) {
                        DRBD.adjust(h, "all", true);
                        testOutput.put(h, DRBD.getDRBDtest());
                    }
                } catch (Exceptions.DrbdConfigException dce) {
                    getBrowser().clStatusUnlock();
                    Tools.appError("config failed");
                }
                final DRBDtestData dtd = new DRBDtestData(testOutput);
                applyButton.setToolTipText(dtd.getToolTip());
                getBrowser().setDRBDtestData(dtd);
                getBrowser().drbdtestLockRelease();
                startTestLatch.countDown();
            }
        };
        initApplyButton(buttonCallback);

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
        getResource().setValue(ClusterBrowser.DRBD_RES_PARAM_NAME,
                               getDrbdResource().getName());
        getResource().setValue(ClusterBrowser.DRBD_RES_PARAM_DEV, getDevice());

        final String[] params = getParametersFromXML();
        addParams(optionsPanel,
                  params,
                  Tools.getDefaultInt("ClusterBrowser.DrbdResLabelWidth"),
                  Tools.getDefaultInt("ClusterBrowser.DrbdResFieldWidth"),
                  null);

        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(new Runnable() {
                    public void run() {
                        getBrowser().clStatusLock();
                        apply(false);
                        try {
                            getDrbdInfo().createDrbdConfig(false);
                            for (final Host h : getCluster().getHostsArray()) {
                                DRBD.adjust(h, "all", false);
                            }
                        } catch (Exceptions.DrbdConfigException dce) {
                            getBrowser().clStatusUnlock();
                            Tools.appError("config failed");
                        }
                        getBrowser().clStatusUnlock();
                    }
                });
                thread.start();
            }
        });

        addApplyButton(buttonPanel);
        applyButton.setEnabled(checkResourceFields(null, params));

        mainPanel.add(optionsPanel);

        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(new JScrollPane(mainPanel));
        infoPanel = newPanel;
        return infoPanel;
    }

    /**
     * Removes this drbd resource with confirmation dialog.
     */
    public final void removeMyself(final boolean testOnly) {
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

    public final void removeFromHashes() {
        getBrowser().getDrbdResHash().remove(getName());
        getBrowser().getDrbdDevHash().remove(getDevice());
        blockDevInfo1.removeFromDrbd();
        blockDevInfo2.removeFromDrbd();
    }

    /**
     * removes this object from jtree and from list of drbd resource
     * infos without confirmation dialog.
     */
    public final void removeMyselfNoConfirm(final boolean testOnly) {
        getBrowser().getDrbdXML().removeResource(getName());
        getBrowser().getDrbdGraph().removeDrbdResource(this);
        final Host[] hosts = getCluster().getHostsArray();
        for (final Host host : hosts) {
            DRBD.down(host, getName(), testOnly);
        }
        super.removeMyself(testOnly);
        final DrbdResourceInfo dri = getBrowser().getDrbdResHash().get(
                                                                    getName());
        getBrowser().getDrbdResHash().remove(getName());
        dri.setName(null);
        getBrowser().reload(getBrowser().getServicesNode());
        getBrowser().getDrbdDevHash().remove(getDevice());
        blockDevInfo1.removeFromDrbd();
        blockDevInfo2.removeFromDrbd();
        blockDevInfo1.removeMyself(testOnly);
        blockDevInfo2.removeMyself(testOnly);
        getBrowser().updateCommonBlockDevices();

        try {
            getBrowser().getDrbdGraph().getDrbdInfo().createDrbdConfig(
                                                                     testOnly);
        } catch (Exceptions.DrbdConfigException dce) {
            Tools.appError("config failed");
        }
        getBrowser().getDrbdGraph().getDrbdInfo().setSelectedNode(null);
        getBrowser().getDrbdGraph().getDrbdInfo().selectMyself();
        getBrowser().getDrbdGraph().updatePopupMenus();
        getBrowser().resetFilesystems();
        infoPanel = null;
        getBrowser().reload(getBrowser().getDrbdNode());
    }

    /**
     * Returns string of the drbd resource.
     */
    public final String toString() {
        String name = getName();
        if (name == null || "".equals(name)) {
            name = Tools.getString("ClusterBrowser.DrbdResUnconfigured");
        }
        return "drbd: " + name;
    }

    /**
     * Returns whether two drbd resources are equal.
     */
    public final boolean equals(final Object value) {
        if (value == null) {
            return false;
        }
        if (Tools.isStringClass(value)) {
            return getDrbdResource().getValue(
                    ClusterBrowser.DRBD_RES_PARAM_DEV).equals(value.toString());
        } else {
            if (toString() == null) {
                return false;
            }
            return toString().equals(value.toString());
        }
    }

    //public int hashCode() {
    //    return toString().hashCode();
    //}

    /**
     * Returns the device name that is used as the string value of
     * the device in the filesystem resource.
     */
    public final String getStringValue() {
        return getDevice();
    }

    /**
     * Adds old style drbddisk service in the heartbeat and graph.
     */
    public final void addDrbdDisk(final FilesystemInfo fi,
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
        hg.addColocation(null, fi, di);
        hg.addOrder(null, di, fi);
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
    public final void addLinbitDrbd(final FilesystemInfo fi,
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
        ldi.setGroupInfo(fi.getGroupInfo());
        getBrowser().addToHeartbeatIdList(ldi);
        fi.setLinbitDrbdInfo(ldi);
        /* it adds coloation only to the graph. */
        hg.addColocation(null, fi, ldi.getCloneInfo());
        hg.addOrder(null, ldi.getCloneInfo(), fi);
        /* this must be executed after the getInfoPanel is executed. */
        ldi.waitForInfoPanel();
        ldi.paramComboBoxGet("drbd_resource", null).setValueAndWait(getName());
        /* apply gets parents from graph and adds colocations. */
        ldi.apply(dcHost, testOnly);
    }


    /**
     * Remove drbddisk heartbeat service.
     */
    public final void removeDrbdDisk(final FilesystemInfo fi,
                                     final Host dcHost,
                                     final boolean testOnly) {
        final DrbddiskInfo drbddiskInfo = fi.getDrbddiskInfo();
        if (drbddiskInfo != null) {
            drbddiskInfo.removeMyselfNoConfirm(dcHost, testOnly);
        }
    }

    /**
     * Remove drbddisk heartbeat service.
     */
    public final void removeLinbitDrbd(final FilesystemInfo fi,
                                       final Host dcHost,
                                       final boolean testOnly) {
        final LinbitDrbdInfo linbitDrbdInfo = fi.getLinbitDrbdInfo();
        if (linbitDrbdInfo != null) {
            linbitDrbdInfo.removeMyselfNoConfirm(dcHost, testOnly);
        }
    }

    /**
     * Sets that this drbd resource is used by hb.
     */
    public final void setUsedByCRM(final boolean isUsedByCRM) {
        this.isUsedByCRM = isUsedByCRM;
    }

    /**
     * Returns whether this drbd resource is used by crm.
     */
    public final boolean isUsedByCRM() {
        return isUsedByCRM;
    }

    /**
     * Returns common file systems.
     */
    public final StringInfo[] getCommonFileSystems(final String defaultValue) {
        return getBrowser().getCommonFileSystems(defaultValue);
    }

    /**
     * Returns both hosts of the drbd connection, sorted alphabeticaly.
     */
    public final Host[] getHosts() {
        final Host h1 = blockDevInfo1.getHost();
        final Host h2 = blockDevInfo2.getHost();
        if (h1.getName().compareToIgnoreCase(h2.getName()) < 0) {
            return new Host[]{h1, h2};
        } else {
            return new Host[]{h2, h1};
        }
    }

    /**
     * Starts resolve split brain dialog.
     */
    public final void resolveSplitBrain() {
        final AddDrbdSplitBrainDialog adrd = new AddDrbdSplitBrainDialog(this);
        adrd.showDialogs();
    }

    /**
     * Starts online verification.
     */
    public final void verify(final boolean testOnly) {
        blockDevInfo1.verify(testOnly);
    }

    /**
     * Returns whether the specified host has this drbd resource.
     */
    public final boolean resourceInHost(final Host host) {
        if (blockDevInfo1.getHost() == host
            || blockDevInfo2.getHost() == host) {
            return true;
        }
        return false;
    }

    /**
     * Returns the list of items for the popup menu for drbd resource.
     */
    public final List<UpdatableItem> createPopup(
                              final List<UpdatableItem> registeredMenuItem) {
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
            ConfigData.AccessType.OP,
            ConfigData.AccessType.OP) {

            private static final long serialVersionUID = 1L;

            public boolean predicate() {
                return !isConnected(testOnly);
            }

            public boolean enablePredicate() {
                return !isSyncing();
            }

            public void action() {
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
            public void action(final Host host) {
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
        registeredMenuItem.add(connectMenu);
        items.add(connectMenu);

        final MyMenuItem resumeSync = new MyMenuItem(
           Tools.getString("ClusterBrowser.Drbd.ResourceResumeSync"),
           null,
           Tools.getString("ClusterBrowser.Drbd.ResourceResumeSync.ToolTip"),

           Tools.getString("ClusterBrowser.Drbd.ResourcePauseSync"),
           null,
           Tools.getString("ClusterBrowser.Drbd.ResourcePauseSync.ToolTip"),
           ConfigData.AccessType.OP,
           ConfigData.AccessType.OP) {
            private static final long serialVersionUID = 1L;

            public boolean predicate() {
                return isPausedSync();
            }

            public boolean enablePredicate() {
                return isSyncing();
            }
            public void action() {
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
        registeredMenuItem.add(resumeSync);
        items.add(resumeSync);

        /* resolve split-brain */
        final MyMenuItem splitBrainMenu = new MyMenuItem(
                Tools.getString("ClusterBrowser.Drbd.ResolveSplitBrain"),
                null,
                Tools.getString(
                            "ClusterBrowser.Drbd.ResolveSplitBrain.ToolTip"),
                ConfigData.AccessType.OP,
                ConfigData.AccessType.OP) {

            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                return isSplitBrain();
            }

            public void action() {
                resolveSplitBrain();
            }
        };
        registeredMenuItem.add(splitBrainMenu);
        items.add(splitBrainMenu);

        /* start online verification */
        final MyMenuItem verifyMenu = new MyMenuItem(
                Tools.getString("ClusterBrowser.Drbd.Verify"),
                null,
                Tools.getString("ClusterBrowser.Drbd.Verify.ToolTip"),
                ConfigData.AccessType.OP,
                ConfigData.AccessType.OP) {

            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                return isConnected(testOnly)
                       && !isSyncing()
                       && !isVerifying();
            }

            public void action() {
                verify(testOnly);
            }
        };
        registeredMenuItem.add(verifyMenu);
        items.add(verifyMenu);
        /* remove resource */
        final MyMenuItem removeResMenu = new MyMenuItem(
                        Tools.getString("ClusterBrowser.Drbd.RemoveEdge"),
                        ClusterBrowser.REMOVE_ICON,
                        Tools.getString(
                                "ClusterBrowser.Drbd.RemoveEdge.ToolTip"),
                        ConfigData.AccessType.ADMIN,
                        ConfigData.AccessType.OP) {
            private static final long serialVersionUID = 1L;
            public void action() {
                /* this drbdResourceInfo remove myself and this calls
                   removeDrbdResource in this class, that removes the edge
                   in the graph. */
                removeMyself(testOnly);
            }

            public boolean enablePredicate() {
                return !isUsedByCRM();
            }
        };
        registeredMenuItem.add(removeResMenu);
        items.add(removeResMenu);

        /* view log */
        final MyMenuItem viewLogMenu = new MyMenuItem(
                               Tools.getString("ClusterBrowser.Drbd.ViewLogs"),
                               LOGFILE_ICON,
                               null,
                               ConfigData.AccessType.RO,
                               ConfigData.AccessType.RO) {

            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                return true;
            }

            public void action() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getPopup().setVisible(false);
                    }
                });
                final String device = getDevice();
                DrbdLogs l = new DrbdLogs(getCluster(), device);
                l.showDialog();
            }
        };
        registeredMenuItem.add(viewLogMenu);
        items.add(viewLogMenu);
        return items;
    }

    /**
     * Sets whether the meta-data have to be created, meaning there are no
     * existing meta-data for this resource on both nodes.
     */
    public final void setHaveToCreateMD(final boolean haveToCreateMD) {
        this.haveToCreateMD = haveToCreateMD;
    }

    /**
     * Returns whether the md has to be created or not.
     */
    public final boolean isHaveToCreateMD() {
        return haveToCreateMD;
    }

    /**
     * Returns meta-disk device for the specified host.
     */
    public final String getMetaDiskForHost(final Host host) {
        return getBrowser().getDrbdXML().getMetaDisk(host.getName(), getName());
    }

    /**
     * Returns tool tip when mouse is over the resource edge.
     */
    public String getToolTipForGraph(final boolean testOnly) {
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

    /**
     * Returns the last created filesystem.
     */
    public final String getCreatedFs() {
        return createdFs;
    }

    /**
     * Sets the last created filesystem.
     */
    public final void setCreatedFs(final String createdFs) {
        this.createdFs = createdFs;
    }

    /**
     * Returns how much diskspace is used on the primary.
     */
    public final int getUsed() {
        if (blockDevInfo1.getBlockDevice().isPrimary()) {
            return blockDevInfo1.getBlockDevice().getUsed();
        } else if (blockDevInfo2.getBlockDevice().isPrimary()) {
            return blockDevInfo2.getBlockDevice().getUsed();
        }
        return -1;
    }
}
