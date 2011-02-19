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
import drbd.AddDrbdConfigDialog;
import drbd.gui.Browser;
import drbd.gui.ClusterBrowser;
import drbd.gui.GuiComboBox;
import drbd.data.Host;
import drbd.data.DrbdXML;
import drbd.data.resources.Resource;
import drbd.data.DRBDtestData;
import drbd.utilities.Tools;
import drbd.utilities.ButtonCallback;
import drbd.utilities.DRBD;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Iterator;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;

/**
 * This class provides drbd info. For one it shows the editable global
 * drbd config, but if a drbd block device is selected it forwards to the
 * block device info, which is defined in HostBrowser.java.
 */
public class DrbdInfo extends DrbdGuiInfo {
    /** Selected block device. */
    private BlockDevInfo selectedBD = null;
    /** Cache for the info panel. */
    private JComponent infoPanel = null;

    /** Prepares a new <code>DrbdInfo</code> object. */
    public DrbdInfo(final String name, final Browser browser) {
        super(name, browser);
        setResource(new Resource(name));
        ((ClusterBrowser) browser).getDrbdGraph().setDrbdInfo(this);
    }

    /** Sets stored parameters. */
    public final void setParameters() {
        final DrbdXML dxml = getBrowser().getDrbdXML();
        for (final String param : getParametersFromXML()) {
            final String section = dxml.getSection(param);
            String value;
            final String defaultValue = getParamDefault(param);
            if (DrbdXML.GLOBAL_SECTION.equals(section)) {
                value = dxml.getGlobalConfigValue(param);
                if (value == null) {
                    value = defaultValue;
                }
                if (value == null) {
                    value = "";
                }
            } else {
                value = dxml.getCommonConfigValue(section, param);
                if ("".equals(value)) {
                    value = defaultValue;
                }
            }
            if ("".equals(value) && "usage-count".equals(param)) {
                value = "yes"; /* we don't get this parameter from
                                  the dump. */
            }
            final String oldValue = getParamSaved(param);
            final GuiComboBox cb = paramComboBoxGet(param, null);
            if (!Tools.areEqual(value, oldValue)) {
                getResource().setValue(param, value);
                if (cb != null) {
                    cb.setValue(value);
                }
            }
        }
    }

    /** Sets selected block device. */
    public final void setSelectedNode(final BlockDevInfo bdi) {
        this.selectedBD = bdi;
    }

    /**
     * Gets combo box for paremeter in te global config. usage-count is
     * disabled.
     */
    @Override protected final GuiComboBox getParamComboBox(final String param,
                                                           final String prefix,
                                                           final int width) {
        final GuiComboBox cb = super.getParamComboBox(param, prefix, width);
        if ("usage-count".equals(param)) {
            cb.setEnabled(false);
        }
        return cb;
    }

    /** Creates drbd config. */
    public final void createDrbdConfig(final boolean testOnly)
               throws Exceptions.DrbdConfigException {
        final StringBuffer globalConfig = new StringBuffer(160);
        globalConfig.append("## generated by drbd-gui\n\n");

        final StringBuffer global = new StringBuffer(80);
        final DrbdXML dxml = getBrowser().getDrbdXML();
        /* global options */
        final String[] params = dxml.getSectionParams(DrbdXML.GLOBAL_SECTION);
        global.append("global {\n");
        for (String param : params) {
            String value = getComboBoxValue(param);
            if ("usage-count".equals(param)
                && (value == null || "".equals(value))) {
                value = "yes";
            }
            if (value == null || "".equals(value)) {
                continue;
            }
            if (!value.equals(dxml.getParamDefault(param))) {
                if (isCheckBox(param)
                    || "booleanhandler".equals(getParamType(param))) {
                    if (value.equals(Tools.getString("Boolean.True"))) {
                        /* boolean parameter */
                        global.append("\t\t" + param + ";\n");
                    }
                } else {
                    global.append("\t\t");
                    global.append(param);
                    global.append('\t');
                    global.append(Tools.escapeConfig(value));
                    global.append(";\n");
                }
            }
        }
        global.append("}\n");
        if (global.length() > 0) {
            globalConfig.append(global);
        }

        /* common section */
        final String common = drbdSectionsConfig();
        String commonSectionConfig = "";
        if (!"".equals(common)) {
            commonSectionConfig = "\ncommon {\n" + common + "}";
        }

        /* resources */
        final Host[] hosts = getBrowser().getCluster().getHostsArray();
        for (Host host : hosts) {
            final Map<String, String> resConfigs =
                                           new LinkedHashMap<String, String>();
            for (final DrbdResourceInfo dri
                                       : getBrowser().getDrbdResHashValues()) {
                if (dri.resourceInHost(host)) {
                    try {
                        final String rConf = dri.drbdResourceConfig();
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
            final String drbdV = host.getDrbdVersion();
            boolean bigDRBDConf = Tools.getConfigData().getBigDRBDConf()
                                  || Tools.compareVersions(drbdV, "8.3.0") < 0;
            if (testOnly) {
                dir = "/var/lib/drbd/";
                configName = "drbd.conf-drbd-mc-test";
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
                final StringBuffer tempDRBDConf = new StringBuffer(200);
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
                final StringBuffer drbdConf = new StringBuffer(200);
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
    @Override public final String[] getParametersFromXML() {
        final DrbdXML drbdXML = getBrowser().getDrbdXML();
        if (drbdXML == null) {
            return null;
        }
        return drbdXML.getGlobalParams();
    }

    /**
     * Returns section to which this parameter belongs.
     * This is used for grouping in the info panel.
     */
    @Override protected final String getSection(final String param) {
        final String section = getBrowser().getDrbdXML().getSection(param);
        if (DrbdXML.GLOBAL_SECTION.equals(section)) {
            return section;
        } else {
            return Tools.getString("DrbdInfo.CommonSection") + section;
        }
    }

    /** Applies changes made in the info panel by user. */
    public final void apply(final boolean testOnly) {
        if (!testOnly) {
            final String[] params = getParametersFromXML();
            Tools.invokeAndWait(new Runnable() {
                @Override public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                    getApplyButton().setToolTipText(null);
                }
            });
            storeComboBoxValues(params);
            for (final DrbdResourceInfo dri : getDrbdResources()) {
                dri.setParameters();
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setAllApplyButtons();
                }
            });
        }
    }

    /**
     * Returns info panel for drbd. If a block device was selected, its
     * info panel is shown.
     */
    @Override public final JComponent getInfoPanel() {
        if (selectedBD != null) { /* block device is not in drbd */
            return selectedBD.getInfoPanel();
        }
        if (infoPanel != null) {
            return infoPanel;
        }
        final JPanel mainPanel = new JPanel();
        if (getBrowser().getDrbdXML() == null) {
            mainPanel.add(new JLabel("drbd info not available"));
            return mainPanel;
        }
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;

            /** Whether the whole thing should be enabled. */
            @Override public final boolean isEnabled() {
                final Host dcHost = getBrowser().getDCHost();
                if (dcHost == null) {
                    return false;
                }
                final String pmV = dcHost.getPacemakerVersion();
                final String hbV = dcHost.getHeartbeatVersion();
                if (pmV == null
                    && hbV != null
                    && Tools.compareVersions(hbV, "2.1.4") <= 0) {
                    return false;
                }
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
                    createDrbdConfig(true);
                    for (final Host h
                                : getBrowser().getCluster().getHostsArray()) {
                        DRBD.adjust(h, "all", true);
                        testOutput.put(h, DRBD.getDRBDtest());
                    }
                } catch (Exceptions.DrbdConfigException dce) {
                    getBrowser().drbdtestLockRelease();
                    Tools.appError("config failed");
                    return;
                }
                final DRBDtestData dtd = new DRBDtestData(testOutput);
                getApplyButton().setToolTipText(dtd.getToolTip());
                getBrowser().setDRBDtestData(dtd);
                getBrowser().drbdtestLockRelease();
                startTestLatch.countDown();
            }
        };
        initApplyButton(buttonCallback);
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

        final String[] params = getParametersFromXML();
        addParams(optionsPanel,
                  params,
                  Tools.getDefaultInt("ClusterBrowser.DrbdResLabelWidth"),
                  Tools.getDefaultInt("ClusterBrowser.DrbdResFieldWidth"),
                  null);

        getApplyButton().addActionListener(
            new ActionListener() {
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
                                createDrbdConfig(false);
                                for (final Host h
                                       : getBrowser().getCluster().getHosts()) {
                                    DRBD.adjust(h, "all", false);
                                }
                            } catch (
                                final Exceptions.DrbdConfigException dce) {
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
            }
        );
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

        /* apply button */
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
        return infoPanel;
    }

    /**
     * Clears info panel cache.
     * TODO: should select something.
     */
    @Override public final boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /** Returns drbd graph in a panel. */
    @Override public final JPanel getGraphicalView() {
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
    @Override public final void selectMyself() {
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

    /**
     * Adds drbd resource. If resource name and drbd device are null.
     * They will be created with first unused index. E.g. r0, r1 ...
     * /dev/drbd0, /dev/drbd1 ... etc.
     *
     * @param name
     *              resource name.
     * @param drbdDevStr
     *              drbd device like /dev/drbd0
     * @param bd1
     *              block device
     * @param bd2
     *              block device
     * @param interactive
     *              whether dialog box will be displayed
     */
    public final boolean addDrbdResource(String name,
                                         String drbdDevStr,
                                         final BlockDevInfo bd1,
                                         final BlockDevInfo bd2,
                                         final boolean interactive,
                                         final boolean testOnly) {
        if (getBrowser().getDrbdResHash().containsKey(name)) {
            getBrowser().putDrbdResHash();
            return false;
        }
        getBrowser().putDrbdResHash();
        DrbdResourceInfo dri;
        if (bd1 == null || bd2 == null) {
            return false;
        }
        final DrbdXML dxml = getBrowser().getDrbdXML();
        if (name == null && drbdDevStr == null) {
            int index = getNewDrbdResourceIndex();
            name = "r" + Integer.toString(index);
            drbdDevStr = "/dev/drbd" + Integer.toString(index);

            /* search for next available drbd device */
            final Map<String, DrbdResourceInfo> drbdDevHash =
                                                 getBrowser().getDrbdDevHash();
            while (drbdDevHash.containsKey(drbdDevStr)) {
                index++;
                drbdDevStr = "/dev/drbd" + Integer.toString(index);
            }
            getBrowser().putDrbdDevHash();
            dri = new DrbdResourceInfo(name,
                                       drbdDevStr,
                                       bd1,
                                       bd2,
                                       getBrowser());
        } else {
            dri = new DrbdResourceInfo(name,
                                       drbdDevStr,
                                       bd1,
                                       bd2,
                                       getBrowser());
            final String[] sections = dxml.getSections();
            for (String section : sections) {
                final String[] params = dxml.getSectionParams(section);
                for (String param : params) {
                    String value = dxml.getConfigValue(name, section, param);
                    if ("".equals(value)) {
                        value = dxml.getParamDefault(param);
                    }
                    dri.getDrbdResource().setValue(param, value);
                }
            }
            dri.getDrbdResource().setCommited(true);
        }
        /* We want next port number on both devices to be the same,
         * although other port numbers may not be the same on both. */
        final int viPort1 = bd1.getNextVIPort();
        final int viPort2 = bd2.getNextVIPort();
        final int viPort;
        if (viPort1 > viPort2) {
            viPort = viPort1;
        } else {
            viPort = viPort2;
        }
        bd1.setDefaultVIPort(viPort + 1);
        bd2.setDefaultVIPort(viPort + 1);

        dri.getDrbdResource().setDefaultValue(
                                        DrbdResourceInfo.DRBD_RES_PARAM_NAME,
                                        name);
        dri.getDrbdResource().setDefaultValue(
                                        DrbdResourceInfo.DRBD_RES_PARAM_DEV,
                                        drbdDevStr);
        getBrowser().getDrbdResHash().put(name, dri);
        getBrowser().putDrbdResHash();
        getBrowser().getDrbdDevHash().put(drbdDevStr, dri);
        getBrowser().putDrbdDevHash();

        if (bd1 != null) {
            bd1.setDrbd(true);
            bd1.setDrbdResourceInfo(dri);
            bd1.cleanup();
            bd1.setInfoPanel(null); /* reload panel */
            bd1.getInfoPanel();
        }
        if (bd2 != null) {
            bd2.setDrbd(true);
            bd2.setDrbdResourceInfo(dri);
            bd2.cleanup();
            bd2.setInfoPanel(null); /* reload panel */
            bd2.getInfoPanel();
        }

        final DefaultMutableTreeNode drbdResourceNode =
                                           new DefaultMutableTreeNode(dri);
        dri.setNode(drbdResourceNode);

        getBrowser().getDrbdNode().add(drbdResourceNode);

        final DefaultMutableTreeNode drbdBDNode1 =
                                           new DefaultMutableTreeNode(bd1);
        bd1.setNode(drbdBDNode1);
        final DefaultMutableTreeNode drbdBDNode2 =
                                           new DefaultMutableTreeNode(bd2);
        bd2.setNode(drbdBDNode2);
        drbdResourceNode.add(drbdBDNode1);
        drbdResourceNode.add(drbdBDNode2);

        getBrowser().getDrbdGraph().addDrbdResource(dri, bd1, bd2);
        dri.getInfoPanel();
        final DrbdResourceInfo driF = dri;
        if (interactive) {
            if (bd1 != null) {
                bd1.getBlockDevice().setNew(true);
            }
            if (bd2 != null) {
                bd2.getBlockDevice().setNew(true);
            }
            final Thread thread = new Thread(new Runnable() {
                @Override public void run() {
                    getBrowser().reload(drbdResourceNode, true);
                    AddDrbdConfigDialog adrd = new AddDrbdConfigDialog(driF);
                    adrd.showDialogs();
                    /* remove wizard parameters from hashes. */
                    for (final String p : bd1.getParametersFromXML()) {
                        bd1.paramComboBoxRemove(p, "wizard");
                        bd2.paramComboBoxRemove(p, "wizard");
                    }
                    for (final String p : driF.getParametersFromXML()) {
                        driF.paramComboBoxRemove(p, "wizard");
                    }
                    if (adrd.isCanceled()) {
                        driF.removeMyselfNoConfirm(testOnly);
                        getBrowser().getDrbdGraph().stopAnimation(bd1);
                        getBrowser().getDrbdGraph().stopAnimation(bd2);
                        return;
                    }

                    getBrowser().updateCommonBlockDevices();
                    final DrbdXML newDrbdXML =
                        new DrbdXML(getBrowser().getCluster().getHostsArray());
                    final String configString1 =
                                    newDrbdXML.getConfig(bd1.getHost());
                    newDrbdXML.update(configString1);
                    final String configString2 =
                                    newDrbdXML.getConfig(bd2.getHost());
                    newDrbdXML.update(configString2);
                    getBrowser().setDrbdXML(newDrbdXML);
                    getBrowser().resetFilesystems();
                    driF.selectMyself();
                }
            });
            thread.start();
        } else {
            getBrowser().resetFilesystems();
        }
        return true;
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed. If param is null, only param will be checked,
     * otherwise all parameters will be checked.
     */
    @Override public boolean checkResourceFieldsChanged(final String param,
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
    public boolean checkResourceFieldsCorrect(final String param,
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

    /** Revert all values. */
    @Override public final void revert() {
        super.revert();
        for (final DrbdResourceInfo dri : getDrbdResources()) {
            dri.revert();
        }
    }

    /** Set all apply buttons. */
    public final void setAllApplyButtons() {
        for (final DrbdResourceInfo dri : getDrbdResources()) {
            dri.storeComboBoxValues(dri.getParametersFromXML());
            dri.setAllApplyButtons();
        }
    }

    /** Returns all drbd resources in this cluster. */
    public final Set<DrbdResourceInfo> getDrbdResources() {
        final Set<DrbdResourceInfo> resources =
                                        new LinkedHashSet<DrbdResourceInfo>();
        final Host[] hosts = getBrowser().getCluster().getHostsArray();
        for (final DrbdResourceInfo dri : getBrowser().getDrbdResHashValues()) {
            for (final Host host : hosts) {
                if (dri.resourceInHost(host)) {
                    resources.add(dri);
                }
            }
        }
        return resources;
    }
}
