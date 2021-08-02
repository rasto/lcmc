/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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


package lcmc.cluster.ui.wizard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import lcmc.Exceptions;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.domain.NetworkService;
import lcmc.cluster.infrastructure.ssh.ExecCommandConfig;
import lcmc.cluster.infrastructure.ssh.ExecCommandThread;
import lcmc.cluster.ui.widget.Check;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.ExecCallback;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Access;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.WidgetListener;
import lcmc.crm.domain.CastAddress;
import lcmc.crm.domain.UcastLink;
import lcmc.crm.infrastrucure.Heartbeat;
import lcmc.drbd.domain.NetInterface;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * An implementation of a dialog where heartbeat is initialized on all hosts.
 */
@Named
final class HbConfig extends DialogCluster {
    private static final Logger LOG = LoggerFactory.getLogger(HbConfig.class);
    private static final String KEEPALIVE_OPTION = "keepalive";
    private static final String WARNTIME_OPTION = "warntime";
    private static final String DEADTIME_OPTION = "deadtime";
    private static final String INITDEAD_OPTION = "initdead";
    private static final String CRM_OPTION = "crm";
    private static final String COMPRESSION_OPTION = "compression";
    private static final String COMPRESSION_THRESHOLD_OPTION = "compression_threshold";
    private static final String TRADITIONAL_COMPRESSION_OPTION = "traditional_compression";
    private static final String LOGFACILITY_OPTION = "logfacility";
    private static final String USE_LOGD_OPTION = "use_logd";
    private static final String AUTOJOIN_OPTION = "autojoin";
    private static final String NODE_OPTION = "node";
    private static final String[] OPTIONS =
            {KEEPALIVE_OPTION, WARNTIME_OPTION, DEADTIME_OPTION, INITDEAD_OPTION, CRM_OPTION, COMPRESSION_OPTION,
                    COMPRESSION_THRESHOLD_OPTION, TRADITIONAL_COMPRESSION_OPTION, LOGFACILITY_OPTION, USE_LOGD_OPTION,
                    AUTOJOIN_OPTION, NODE_OPTION};
    private static final Map<String, String> OPTION_REGEXPS = new HashMap<>();
    private static final Map<String, Value> OPTION_DEFAULTS = new HashMap<>();
    private static final Map<String, Integer> OPTION_SIZES = new HashMap<>();
    private static final Value MCAST_TYPE = new StringValue("mcast");
    private static final Value BCAST_TYPE = new StringValue("bcast");
    private static final Value UCAST_TYPE = new StringValue("ucast");
    private static final Value SERIAL_TYPE = new StringValue("serial");
    private static final int ADDR_COMBOBOX_WIDTH = 160;
    private static final int LINK_COMBOBOX_WIDTH = 130;
    private static final int TYPE_COMBOBOX_WIDTH = 80;
    private static final int INTERFACE_COMBOBOX_WIDTH = 80;
    private static final int REMOVE_BUTTON_WIDTH = 100;
    private static final int REMOVE_BUTTON_HEIGHT = 14;
    private static final String EDIT_CONFIG_STRING = Tools.getString("Dialog.Cluster.HbConfig.Checkbox.EditConfig");
    private static final String SEE_EXISTING_STRING = Tools.getString("Dialog.Cluster.HbConfig.Checkbox.SeeExisting");
    private static final String HA_CF_ERROR_STRING = "error: read error";
    private static final String NEWLINE = "\\r?\\n";
    static {
        OPTION_REGEXPS.put(KEEPALIVE_OPTION, "\\d*");
        OPTION_REGEXPS.put(WARNTIME_OPTION, "\\d*");
        OPTION_REGEXPS.put(DEADTIME_OPTION, "\\d*");
        OPTION_REGEXPS.put(INITDEAD_OPTION, "\\d*");
        OPTION_REGEXPS.put(CRM_OPTION, "\\w*");

        OPTION_REGEXPS.put(COMPRESSION_OPTION, "\\w*");
        OPTION_REGEXPS.put(COMPRESSION_THRESHOLD_OPTION, "\\d*");
        OPTION_REGEXPS.put(TRADITIONAL_COMPRESSION_OPTION, "\\w*");
        OPTION_REGEXPS.put(LOGFACILITY_OPTION, "\\w*");
        OPTION_REGEXPS.put(USE_LOGD_OPTION, "\\w*");
        OPTION_REGEXPS.put(AUTOJOIN_OPTION, "\\w*");
        OPTION_REGEXPS.put(NODE_OPTION, ".*?");
        /* defaults */
        OPTION_DEFAULTS.put(KEEPALIVE_OPTION, new StringValue("2"));
        OPTION_DEFAULTS.put(WARNTIME_OPTION, new StringValue("20"));
        OPTION_DEFAULTS.put(DEADTIME_OPTION, new StringValue("30"));
        OPTION_DEFAULTS.put(INITDEAD_OPTION, new StringValue("30"));
        OPTION_DEFAULTS.put(CRM_OPTION, new StringValue("respawn"));
        OPTION_DEFAULTS.put(COMPRESSION_OPTION, new StringValue("bz2"));
        OPTION_DEFAULTS.put(COMPRESSION_THRESHOLD_OPTION, new StringValue("20"));
        OPTION_DEFAULTS.put(TRADITIONAL_COMPRESSION_OPTION, new StringValue("on"));
        /* sizes */
        OPTION_SIZES.put(CRM_OPTION, 100);
        OPTION_SIZES.put(COMPRESSION_OPTION, 80);
        OPTION_SIZES.put(LOGFACILITY_OPTION, 80);
        OPTION_SIZES.put(USE_LOGD_OPTION, 50);
        OPTION_SIZES.put(AUTOJOIN_OPTION, 80);
        OPTION_SIZES.put(NODE_OPTION, 300);
    }

    private final Map<String, Value> optionDefaults = new HashMap<>(OPTION_DEFAULTS);
    private final Map<String, Value[]> optionValues = new HashMap<>();
    private final Map<String, Widget> optionsWidgets = new HashMap<>();

    private JCheckBox dopdWidget = null;
    private JCheckBox mgmtdWidget = null;
    private JPanel mcastPanel;
    /**
     * Set of ucast, bcast, mcast etc. addresses.
     */
    private final Collection<CastAddress> castAddresses = new LinkedHashSet<>();
    private final JLabel configStatus = new JLabel("");
    /**
     * Connection type pulldown menu: ucast, bcast, mcast ...
     */
    private Widget typeWidget;
    private Widget ifaceWidget;
    private Widget serialWidget;
    private Widget ucastLink1Widget;
    private Widget ucastLink2Widget;
    private Widget addrWidget;
    private MyButton addAddressButton;
    /**
     * Array with /etc/ha.d/ha.cf configs from all hosts.
     */
    private String[] configs;
    private JPanel statusPanel;
    /**
     * Check box that allows to edit a new config are see the existing configs.
     */
    private JCheckBox configCheckbox;
    private final JPanel configPanel = new JPanel();
    private boolean configChangedByUser = false;
    private volatile JScrollPane configScrollPane = null;
    private volatile boolean configAlreadyScrolled = false;
    private CountDownLatch fieldCheckLatch = new CountDownLatch(1);
    private final Application application;
    private final SwingUtils swingUtils;
    private final WidgetFactory widgetFactory;
    private MyButton makeConfigButton;
    private final InitCluster initCluster;
    private final NetworkService networkService;
    private final Access access;

    public HbConfig(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData,
            InitCluster initCluster, NetworkService networkService, Access access, Provider<ProgressBar> progressBarProvider) {
        super(application, swingUtils, widgetFactory, mainData, progressBarProvider);
        this.application = application;
        this.swingUtils = swingUtils;
        this.widgetFactory = widgetFactory;
        this.initCluster = initCluster;
        this.networkService = networkService;
        this.access = access;
    }

    @Override
    public void init(final WizardDialog previousDialog, final Cluster cluster) {
        super.init(previousDialog, cluster);
        makeConfigButton = widgetFactory.createButton(Tools.getString("Dialog.Cluster.HbConfig.CreateHbConfig"));
        final Host[] hosts = getCluster().getHostsArray();
        final StringBuilder config = new StringBuilder();
        boolean first = true;
        for (final Host host : hosts) {
            if (!first) {
                config.append(' ');
            }
            first = false;
            config.append(host.getHostname());
        }
        /* choices */
        optionValues.put(NODE_OPTION, new Value[]{new StringValue(config.toString()), new StringValue()});
        optionDefaults.put(NODE_OPTION, new StringValue(config.toString()));
        optionValues.put(CRM_OPTION, new Value[]{new StringValue("respawn"),
                                                 new StringValue("on"),
                                                 new StringValue("off")});
        optionValues.put(COMPRESSION_OPTION, new Value[]{new StringValue(),
                                                         new StringValue("zlib"),
                                                         new StringValue("bz2")});
        optionValues.put(TRADITIONAL_COMPRESSION_OPTION,
                         new Value[]{new StringValue(), new StringValue("on"), new StringValue("off")});
        optionValues.put(LOGFACILITY_OPTION, new Value[]{new StringValue("local0"),
                                                         new StringValue("local1"),
                                                         new StringValue("local2"),
                                                         new StringValue("local3"),
                                                         new StringValue("local4"),
                                                         new StringValue("local5"),
                                                         new StringValue("local6"),
                                                         new StringValue("local7"),
                                                         new StringValue("none")});
        optionValues.put(USE_LOGD_OPTION, new Value[]{new StringValue(), new StringValue("on"), new StringValue("off")});
        optionValues.put(AUTOJOIN_OPTION,
                new Value[]{new StringValue(), new StringValue("any"), new StringValue("other"), new StringValue("none")});
        configs = new String[hosts.length];
        makeConfigButton.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
        makeConfigButton.setEnabled(false);
        makeConfigButton.addActionListener(e -> {
            final Thread thread = new Thread(() -> {
                fieldCheckLatch = new CountDownLatch(1);
                swingUtils.invokeLater(() -> makeConfigButton.setEnabled(false));
                disableComponents();
                final StringBuilder config1 = hbConfigHead(false);
                config1.append(hbConfigOptions());
                config1.append('\n');
                config1.append(hbConfigAddr());
                config1.append(hbConfigDopd(dopdWidget.isSelected()));
                config1.append(hbConfigMgmtd(mgmtdWidget.isSelected()));

                Heartbeat.createHBConfig(hosts, config1);
                final boolean configOk = updateOldHbConfig();
                if (dopdWidget.isSelected()) {
                    for (final Host h : hosts) {
                        final String hbV = h.getHostParser()
                                            .getHeartbeatVersion();
                        boolean wa = false;
                        try {
                            if (hbV != null && Tools.compareVersions(hbV, "3.0.2") <= 0) {
                                wa = true;
                            }
                        } catch (final Exceptions.IllegalVersionException e1) {
                            LOG.appWarning("run: " + e1.getMessage(), e1);
                        }
                        Heartbeat.enableDopd(h, wa);
                    }
                }
                Heartbeat.reloadHeartbeats(hosts);
                enableComponents();
                final List<String> incorrect = new ArrayList<>();
                final List<String> changed = new ArrayList<>();
                if (configOk) {
                    hideRetryButton();
                } else {
                    incorrect.add("config failed");
                }
                nextButtonSetEnabled(new Check(incorrect, changed));

                if (configOk && !application.getAutoClusters()
                                            .isEmpty()) {
                    Tools.sleep(1000);
                    pressNextButton();
                }
            });
            thread.start();
        });
    }

    @Override
    public WizardDialog nextDialog() {
        final DialogCluster nextDialog = initCluster;
        nextDialog.init(this, getCluster());
        return nextDialog;
    }

    @Override
    protected String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.HbConfig.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Cluster.HbConfig.Description");
    }

    @Override
    public String nextButton() {
        return Tools.getString("Dialog.Cluster.HbConfig.NextButton");
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.PAGE_AXIS));
        configPanel.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    @Override
    protected void initDialogAfterVisible() {
        final Thread thread = new Thread(() -> {
            final boolean configOk = updateOldHbConfig();
            swingUtils.invokeLater(() -> makeConfigButton.setEnabled(false));
            enableComponents();
            final List<String> incorrect = new ArrayList<>();
            final List<String> changed = new ArrayList<>();
            if (!configOk) {
                incorrect.add("config failed");
            }
            nextButtonSetEnabled(new Check(incorrect, changed));
            if (configOk && !application.getAutoClusters()
                                        .isEmpty()) {
                Tools.sleep(1000);
                pressNextButton();
            }
        });
        thread.start();
    }

    /**
     * Parses the old config and sets the new one with old and new information.
     */
    private void setNewConfig(final String oldConfig) {
        final String[] config = oldConfig.split(NEWLINE);
        final Pattern bcastP = Pattern.compile("(bcast) (\\w+)");
        final Pattern mcastP = Pattern.compile("(mcast) (\\w+) (.*)");
        final Pattern serialP = Pattern.compile("(serial) (.*)");
        final Pattern ucastP = Pattern.compile("(ucast) (\\w+) (.*)");
        final Map<String, Pattern> optionPatterns = new HashMap<>();
        for (final String option : OPTIONS) {
            optionPatterns.put(option,
                               Pattern.compile("^\\s*" + option + "\\s+(" + OPTION_REGEXPS.get(option) + ")\\s*$"));
        }
        final Pattern dopdP = Pattern.compile("^\\s*respawn hacluster .*/dopd$");
        final Pattern mgmtdP = Pattern.compile("^\\s*respawn root .*/mgmtd -v$");
        castAddresses.clear();
        final Map<String, String> opValues = new HashMap<>();
        for (final String line : config) {
            final Matcher bcastM  = bcastP.matcher(line);
            final Matcher mcastM  = mcastP.matcher(line);
            final Matcher ucastM  = ucastP.matcher(line);
            final Matcher serialM = serialP.matcher(line);
            final Matcher dopdM = dopdP.matcher(line);
            final Matcher mgmtdM = mgmtdP.matcher(line);
            final String type;
            String iface      = "";
            String addr       = "";
            String serial     = "";
            if (bcastM.matches()) {
                type  = bcastM.group(1);
                iface = bcastM.group(2);
            } else if (mcastM.matches()) {
                type  = mcastM.group(1);
                iface = mcastM.group(2);
                addr  = mcastM.group(3);
            } else if (serialM.matches()) {
                type   = serialM.group(1);
                serial = serialM.group(2);
            } else if (ucastM.matches()) {
                type  = ucastM.group(1);
                iface = ucastM.group(2);
                addr  = ucastM.group(3);
            } else if (dopdM.matches()) {
                dopdWidget.setSelected(true);
                continue;
            } else if (mgmtdM.matches()) {
                mgmtdWidget.setSelected(true);
                continue;
            } else {
                for (final String option : OPTIONS) {
                    final Matcher m = optionPatterns.get(option).matcher(line);
                    if (m.matches()) {
                        opValues.put(option, m.group(1).trim());
                    }
                }
                continue;
            }
            if (type != null && !type.isEmpty()) {
                castAddresses.add(new CastAddress(type, iface, addr, serial));
            }
        }
        for (final String option : OPTIONS) {
            if (opValues.containsKey(option)) {
                optionsWidgets.get(option).setValue(new StringValue(opValues.get(option)));
            } else {
                optionsWidgets.get(option).setValue(new StringValue());
            }
        }
    }

    /**
     * Checks whether the old config is the same on all hosts, if it exists at
     * all and enable the components accordingly.
     * Returns whether the configs are ok and the same on all hosts.
     */
    private boolean updateOldHbConfig() { /* is run in a thread */
        final Host[] hosts = getCluster().getHostsArray();
        final var ts = new ExecCommandThread[hosts.length];
        configStatus.setText(Tools.getString("Dialog.Cluster.HbConfig.Loading"));
        int i = 0;

        for (final Host h : hosts) {
            final int index = i;
            ts[i] = h.execCommand(new ExecCommandConfig().commandString("Heartbeat.getHbConfig")
                                                         .execCallback(new ExecCallback() {
                                                             @Override
                                                             public void done(final String answer) {
                                                                 configs[index] = answer;
                                                             }

                                                             @Override
                                                             public void doneError(final String answer, final int errorCode) {
                                                                 configs[index] = HA_CF_ERROR_STRING;
                                                             }
                                                         })
                                                         .silentCommand()
                                                         .silentOutput());
            i++;
        }
        for (final ExecCommandThread t : ts) {
            /* wait for all of them */
            try {
                t.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        boolean configOk = false;
        boolean noConfigs = true;
        if (configs[0].equals(HA_CF_ERROR_STRING)) {
            swingUtils.invokeLater(
                    () -> configStatus.setText(hosts[0] + ": " + Tools.getString("Dialog.Cluster.HbConfig.NoConfigFound")));
            retry();
            if (!application.getAutoClusters()
                            .isEmpty()) {
                Tools.sleep(1000);
                addAddressButton.pressButton();
            }
        } else {
            noConfigs = false;
            int j;
            for (j = 1; j < configs.length; j++) {
                final Host host = hosts[j];
                if (configs[j].equals(HA_CF_ERROR_STRING)) {
                    swingUtils.invokeLater(
                            () -> configStatus.setText(host + ": " + Tools.getString("Dialog.Cluster.HbConfig.NoConfigFound")));
                    break;
                } else if (!configs[0].equals(configs[j])) {
                    swingUtils.invokeLater(
                            () -> configStatus.setText(Tools.getString("Dialog.Cluster.HbConfig.ConfigsNotTheSame")));
                    break;
                }
            }
            if (j < configs.length) {
                retry();
            } else {
                boolean generated = false;
                final Pattern p = Pattern.compile("## generated by (drbd-gui|LCMC).*", Pattern.DOTALL);
                final Matcher m = p.matcher(configs[0]);
                if (m.matches()) {
                    generated = true;
                }
                final boolean editableConfig = generated;
                swingUtils.invokeLater(() -> {
                    configStatus.setText(Tools.getString("Dialog.Cluster.HbConfig.ha.cf.ok"));
                    configCheckbox.setSelected(false);
                    if (editableConfig) {
                        configCheckbox.setText(SEE_EXISTING_STRING);
                    } else {
                        configCheckbox.setText(EDIT_CONFIG_STRING);
                    }
                    statusPanel.setMaximumSize(statusPanel.getPreferredSize());
                });
                setNewConfig(configs[0]);
                if (editableConfig) {
                    updateConfigPanelEditable(false);
                } else {
                    updateConfigPanelExisting();
                }
                hideRetryButton();
                configOk = true;
            }
        }
        if (!configOk) {
            final boolean noConfigsF = noConfigs;
            swingUtils.invokeLater(() -> {
                configCheckbox.setText(noConfigsF ? SEE_EXISTING_STRING : EDIT_CONFIG_STRING);
                configCheckbox.setSelected(false);
                statusPanel.setMaximumSize(statusPanel.getPreferredSize());
            });
            if (noConfigs) {
                updateConfigPanelEditable(false);
            } else {
                updateConfigPanelExisting();
            }
        }
        swingUtils.invokeLater(() -> fieldCheckLatch.countDown());
        return configOk;
    }

    /** Shows all ha.cf config files. */
    private void updateConfigPanelExisting() {
        final Host[] hosts = getCluster().getHostsArray();
        swingUtils.invokeLater(() -> {
            makeConfigButton.setEnabled(false);
            configPanel.removeAll();
            final JPanel insideConfigPanel = new JPanel(new SpringLayout());
            int cols = 0;
            for (int i = 0; i < hosts.length; i++) {
                if (HA_CF_ERROR_STRING.equals(configs[i])) {
                    configs[i] = Tools.getString("Dialog.Cluster.HbConfig.NoConfigFound");
                }
                final JLabel l = new JLabel(hosts[i].getName() + ':');
                l.setBackground(Color.WHITE);
                final JPanel labelP = new JPanel();
                labelP.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
                labelP.setLayout(new BoxLayout(labelP, BoxLayout.PAGE_AXIS));
                labelP.setAlignmentX(java.awt.Component.TOP_ALIGNMENT);
                labelP.add(l);
                insideConfigPanel.add(labelP);
                final JTextArea ta = new JTextArea(configs[i]);
                ta.setEditable(false);
                insideConfigPanel.add(ta);
                cols += 2;
            }
            if (cols > 0) {
                SpringUtilities.makeCompactGrid(insideConfigPanel, 1, cols, 1, 1, 1, 1);
                configPanel.add(insideConfigPanel);
            }
            configPanel.revalidate();
            configPanel.repaint();
        });
    }

    /** Updates the config panel. */
    private void updateConfigPanelEditable(final boolean configChanged) {
        if (configChanged && fieldCheckLatch.getCount() > 0) {
            return;
        }
        configChangedByUser = configChanged;
        swingUtils.invokeLater(() -> {
            if (!configChanged) {
                makeConfigButton.setEnabled(false);
            }
            configPanel.removeAll();
            /* head */
            final String[] head = hbConfigHead(true).toString()
                                                    .split(NEWLINE);
            for (final String line : head) {
                configPanel.add(new JLabel(line));
            }
            /* timeouts */
            for (final String option : OPTIONS) {
                configPanel.add(getComponentPanel(option, optionsWidgets.get(option)
                                                                        .getComponent()));
            }
            configPanel.add(new JLabel(" "));
            if (castAddresses.size() < 2) {
                final JLabel l;
                if (castAddresses.isEmpty()) {
                    l = new JLabel(Tools.getString("Dialog.Cluster.HbConfig.WarningAtLeastTwoInt"));
                } else {
                    l = new JLabel(Tools.getString("Dialog.Cluster.HbConfig.WarningAtLeastTwoInt.OneMore"));
                }
                l.setForeground(Color.RED);
                configPanel.add(l);
                l.addComponentListener(new ComponentListener() {
                    @Override
                    public void componentHidden(final ComponentEvent e) {
                        /* do nothing */
                    }

                    @Override
                    public void componentMoved(final ComponentEvent e) {
                        if (configAlreadyScrolled) {
                            return;
                        }
                        configAlreadyScrolled = true;
                        configScrollPane.getViewport()
                                        .setViewPosition(l.getBounds()
                                                          .getLocation());
                    }

                    @Override
                    public void componentResized(final ComponentEvent e) {
                        /* do nothing */
                    }

                    @Override
                    public void componentShown(final ComponentEvent e) {
                        /* do nothing */
                    }
                });
            }
            /* addresses */
            for (final CastAddress c : castAddresses) {
                configPanel.add(getComponentPanel(c.getConfigString(), getRemoveButton(c)));
            }
            configPanel.add(new JLabel(" "));
            /* mcast etc combo boxes */
            configPanel.add(mcastPanel);
            /* dopd */
            final String[] dopdLines = hbConfigDopd(dopdWidget.isSelected()).toString()
                                                                            .split(NEWLINE);
            boolean checkboxDone = false;
            for (final String line : dopdLines) {
                if (checkboxDone) {
                    configPanel.add(new JLabel(line));
                } else {
                    configPanel.add(getComponentPanel(line, dopdWidget));
                    checkboxDone = true;
                }
            }

            /* mgmtd */
            final String[] mgmtdLines = hbConfigMgmtd(mgmtdWidget.isSelected()).toString()
                                                                               .split(NEWLINE);
            checkboxDone = false;
            for (final String line : mgmtdLines) {
                if (checkboxDone) {
                    configPanel.add(new JLabel(line));
                } else {
                    configPanel.add(getComponentPanel(line, mgmtdWidget));
                    checkboxDone = true;
                }
            }
            configPanel.revalidate();
            configPanel.repaint();
            if (configChanged) {
                if (castAddresses.isEmpty()) {
                    makeConfigButton.setEnabled(false);
                } else {
                    access.setAccessible(makeConfigButton, AccessMode.ADMIN);
                }
                if (!application.getAutoClusters()
                                .isEmpty() && !castAddresses.isEmpty()) {
                    Tools.sleep(1000);
                    makeConfigButton.pressButton();
                }
            }
        });
    }

    private MyButton getRemoveButton(final CastAddress c) {
        final MyButton removeButton = widgetFactory.createButton(Tools.getString("Dialog.Cluster.HbConfig.RemoveIntButton"));
        removeButton.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
        removeButton.setMaximumSize(new Dimension(REMOVE_BUTTON_WIDTH, REMOVE_BUTTON_HEIGHT));
        removeButton.setPreferredSize(new Dimension(REMOVE_BUTTON_WIDTH, REMOVE_BUTTON_HEIGHT));
        removeButton.addActionListener(e -> {
            final Thread t = new Thread(() -> {
                castAddresses.remove(c);
                updateConfigPanelEditable(true);
                checkInterface();
            });
            t.start();
        });
        return removeButton;
    }

    /**
     * Checks interface if it already exists and enables/disables the 'add
     * button' accordingly.
     */
    private void checkInterface() {
        final Value type = typeWidget.getValue();
        String addr       = "";
        String iface      = "";
        String serial     = "";

        if (BCAST_TYPE.equals(type)) {
            iface = ifaceWidget.getStringValue();
        } else if (MCAST_TYPE.equals(type)) {
            iface = ifaceWidget.getStringValue();
            addr = addrWidget.getStringValue();
        } else if (SERIAL_TYPE.equals(type)) {
            serial = serialWidget.getStringValue();
        } else if (UCAST_TYPE.equals(type)) {
            final UcastLink ucastLink1 = (UcastLink) ucastLink1Widget.getValue();
            final UcastLink ucastLink2 = (UcastLink) ucastLink2Widget.getValue();
            if (ucastLink1 == null || ucastLink2 == null || ucastLink1.getHost() == ucastLink2.getHost()) {
                swingUtils.invokeLater(() -> addAddressButton.setEnabled(false));
                return;
            }
            iface = ucastLink1.getInterface();
            addr = ucastLink2.getIp();
        }

        for (final CastAddress c : castAddresses) {
            if (c.equals(type.getValueForConfig(), iface, addr, serial)) {
                swingUtils.invokeLater(() -> addAddressButton.setEnabled(false));
                return;
            }
        }
        swingUtils.invokeLater(() -> addAddressButton.setEnabled(true));
    }

    private StringBuilder hbConfigHead(final boolean fake) {
        final StringBuilder config = new StringBuilder(130);
        if (fake) {
            config.append("## to be generated by LCMC ");
        } else {
            config.append("## generated by LCMC ");
        }
        return config;
    }

    /** Returns timeouts. */
    private CharSequence hbConfigOptions() {
        final StringBuilder config = new StringBuilder(130);
        config.append(Tools.getRelease());
        config.append("\n\n");
        for (final String option : OPTIONS) {
            final String value = optionsWidgets.get(option).getStringValue();
            if (value != null && !value.isEmpty()) {
                config.append(option);
                config.append(' ');
                config.append(optionsWidgets.get(option).getStringValue());
                config.append('\n');
            }
        }
        return config;
    }

    private CharSequence hbConfigAddr() {
        final StringBuilder config = new StringBuilder(80);
        for (final CastAddress ca : castAddresses) {
            config.append(ca.getConfigString());
            config.append('\n');
        }
        return config;
    }

    /**
     * Returns the part of the config that turns on dopd. To turn it off, the
     * dopd config is commented out.
     */
    private CharSequence hbConfigDopd(final boolean useDopd) {
        final StringBuilder config = new StringBuilder(120);
        if (!useDopd) {
            config.append("# ");
        }
        config.append("respawn hacluster ");
        final Host[] hosts = getCluster().getHostsArray();
        config.append(hosts[0].getHostParser().getHeartbeatLibPath());
        config.append("/dopd\n");
        if (!useDopd) {
            config.append("# ");
        }
        config.append("apiauth dopd gid=haclient uid=hacluster\n");
        return config;
    }

    /**
     * Returns the part of the config that turns on mgmt. To turn it off, the
     * mgmt config is commented out.
     */
    private CharSequence hbConfigMgmtd(final boolean useMgmt) {
        final StringBuilder config = new StringBuilder(120);
        if (!useMgmt) {
            config.append("# ");
        }
        config.append("respawn root ");
        final Host[] hosts = getCluster().getHostsArray();
        config.append(hosts[0].getHostParser().getHeartbeatLibPath());
        config.append("/mgmtd -v\n");
        if (!useMgmt) {
            config.append("# ");
        }
        config.append("apiauth mgmtd uid=root\n");
        return config;
    }

    /** Adds interface to the config panel. It must be called from a thread. */
    private void addInterface(final Value type) {
        String iface      = "";
        String addr       = "";
        String serial     = "";
        if (MCAST_TYPE.equals(type)) {
            iface  = ifaceWidget.getStringValue();
            addr = addrWidget.getStringValue();
        } else if (BCAST_TYPE.equals(type)) {
            iface  = ifaceWidget.getStringValue();
        } else if (UCAST_TYPE.equals(type)) {
            iface = ((UcastLink) ucastLink1Widget.getValue()).getInterface();
            addr = ((UcastLink) ucastLink2Widget.getValue()).getIp();
        } else if (SERIAL_TYPE.equals(type)) {
            serial = serialWidget.getStringValue();
        }
        castAddresses.add(new CastAddress(type.getValueForConfig(), iface, addr, serial));
        updateConfigPanelEditable(true);
        checkInterface();
    }

    @Override
    protected JComponent getInputPane() {
        optionsWidgets.clear();
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        final Host[] hosts = getCluster().getHostsArray();
        final Value[] types = {MCAST_TYPE, BCAST_TYPE, UCAST_TYPE, SERIAL_TYPE};

        typeWidget = widgetFactory.createInstance(
                                      Widget.GUESS_TYPE,
                                      MCAST_TYPE,
                                      types,
                                      Widget.NO_REGEXP,
                                      TYPE_COMBOBOX_WIDTH,
                                      Widget.NO_ABBRV,
                                      new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                                      Widget.NO_BUTTON);

        final NetInterface[] ni = networkService.getNetInterfacesWithBridges(hosts[0]);
        NetInterface defaultNi = null;
        for (final NetInterface n : ni) {
            /* skip lo */
            if (!n.isLocalHost()) {
                defaultNi = n;
                break;
            }
        }
        if (defaultNi == null) {
            LOG.appError("getInputPane: " + hosts[0].getName() + ": missing network interfaces");
        }
        ifaceWidget = widgetFactory.createInstance(Widget.Type.COMBOBOX,
                                                   defaultNi,
                                                   ni,
                                                   Widget.NO_REGEXP,
                                                   INTERFACE_COMBOBOX_WIDTH,
                                                   Widget.NO_ABBRV,
                                                   new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                                                   Widget.NO_BUTTON);

        /* ucast links */
        final List<UcastLink> ulList = new ArrayList<>();
        for (final Host host : hosts) {
            final NetInterface[] netInterfaces = networkService.getNetInterfacesWithBridges(host);
            for (final NetInterface n : netInterfaces) {
                ulList.add(new UcastLink(host, n));
            }
        }
        final UcastLink[] ucastLinks = ulList.toArray(new UcastLink[0]);

        ucastLink1Widget = widgetFactory.createInstance(
                                             Widget.GUESS_TYPE,
                                             Widget.NO_DEFAULT,
                                             ucastLinks,
                                             Widget.NO_REGEXP,
                                             LINK_COMBOBOX_WIDTH,
                                             Widget.NO_ABBRV,
                                             new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                                             Widget.NO_BUTTON);
        ucastLink2Widget = widgetFactory.createInstance(
                                             Widget.GUESS_TYPE,
                                             Widget.NO_DEFAULT,
                                             ucastLinks,
                                             Widget.NO_REGEXP,
                                             LINK_COMBOBOX_WIDTH,
                                             Widget.NO_ABBRV,
                                             new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                                             Widget.NO_BUTTON);

        /* serial links */
        final Value[] serialDevs = {new StringValue("/dev/ttyS0"),
                                    new StringValue("/dev/ttyS1"),
                                    new StringValue("/dev/ttyS2"),
                                    new StringValue("/dev/ttyS3")};

        serialWidget = widgetFactory.createInstance(
                                             Widget.GUESS_TYPE,
                                             Widget.NO_DEFAULT,
                                             serialDevs,
                                             Widget.NO_REGEXP,
                                             LINK_COMBOBOX_WIDTH,
                                             Widget.NO_ABBRV,
                                             new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                                             Widget.NO_BUTTON);

        /* this matches something like this: 225.0.0.43 694 1 0
         * if you think that the regexp is too complicated for that, consider,
         * that it must match also during the thing is written.
         * TODO: it does not work very good anyway
         */
        final String regexp = "^\\d{1,3}(\\.\\d{0,3}(\\d\\.\\d{0,3}"
                              + "(\\d\\.\\d{0,3})( \\d{0,3}(\\d \\d{0,3}"
                              + "(\\d \\d{0,3})?)?)?)?)?$";
        addrWidget = widgetFactory.createInstance(
                                     Widget.GUESS_TYPE,
                                     new StringValue("239.192.0.0 694 1 0"),
                                     Widget.NO_ITEMS,
                                     regexp,
                                     ADDR_COMBOBOX_WIDTH,
                                     Widget.NO_ABBRV,
                                     new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                                     Widget.NO_BUTTON);

        typeWidget.addListeners(
                new WidgetListener() {
                    @Override
                    public void check(final Value value) {
                        final Value type = typeWidget.getValue();
                        if (type != null) {
                            ifaceWidget.setVisible(MCAST_TYPE.equals(type) || BCAST_TYPE.equals(type));

                            addrWidget.setVisible(MCAST_TYPE.equals(type));
                            serialWidget.setVisible(SERIAL_TYPE.equals(type));
                            if (UCAST_TYPE.equals(type)) {
                                ucastLink1Widget.setVisible(true);
                                ucastLink2Widget.setVisible(true);
                            } else {
                                ucastLink1Widget.setVisible(false);
                                ucastLink2Widget.setVisible(false);
                            }
                            swingUtils.invokeLater(() -> mcastPanel.setMaximumSize(mcastPanel.getPreferredSize()));
                            checkInterface();
                        }
                    }
                });

        ifaceWidget.addListeners(new WidgetListener() {
            @Override
            public void check(final Value value) {
                checkInterface();
            }
        });

        serialWidget.setVisible(false);

        serialWidget.addListeners(new WidgetListener() {
            @Override
            public void check(final Value value) {
                checkInterface();
            }
        });

        ucastLink1Widget.setVisible(false);
        ucastLink2Widget.setVisible(false);

        ucastLink1Widget.addListeners(new WidgetListener() {
            @Override
            public void check(final Value value) {
                checkInterface();
            }
        });
        ucastLink2Widget.addListeners(new WidgetListener() {
            @Override
            public void check(final Value value) {
                checkInterface();
            }
        });
        addrWidget.addListeners(new WidgetListener() {
            @Override
            public void check(final Value value) {
                checkInterface();
            }
        });

        addAddressButton = widgetFactory.createButton(Tools.getString("Dialog.Cluster.HbConfig.AddIntButton"));
        addAddressButton.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
        addAddressButton.addActionListener(e -> {
            final Value type = typeWidget.getValue();
            final Thread thread = new Thread(() -> addInterface(type));
            thread.start();
        });

        configScrollPane =
                new JScrollPane(configPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        configScrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE, 150));
        statusPanel = new JPanel();
        statusPanel.add(configStatus);
        configCheckbox = new JCheckBox("-----", true);
        configCheckbox.setBackground(Tools.getDefaultColor("ConfigDialog.Background.Light"));
        access.setAccessible(configCheckbox, AccessMode.ADMIN);
        configCheckbox.addItemListener(e -> {
            final String text = configCheckbox.getText();
            if (e.getStateChange() == ItemEvent.SELECTED) {
                final Thread thread = new Thread(() -> {
                    if (EDIT_CONFIG_STRING.equals(text)) {
                        updateConfigPanelEditable(configChangedByUser);
                        swingUtils.invokeLater(() -> {
                            configCheckbox.setText(SEE_EXISTING_STRING);
                            configCheckbox.setSelected(false);
                            statusPanel.setMaximumSize(statusPanel.getPreferredSize());
                        });
                    } else if (SEE_EXISTING_STRING.equals(text)) {
                        updateConfigPanelExisting();
                        swingUtils.invokeLater(() -> {
                            configCheckbox.setText(EDIT_CONFIG_STRING);
                            configCheckbox.setSelected(false);
                            statusPanel.setMaximumSize(statusPanel.getPreferredSize());
                        });
                    }
                });
                thread.start();
            }
        });
        statusPanel.add(configCheckbox);
        statusPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pane.add(statusPanel);
        pane.add(configScrollPane);
        configScrollPane.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        mcastPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        mcastPanel.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        mcastPanel.add(new JLabel("# "));
        mcastPanel.add(typeWidget.getComponent());
        mcastPanel.add(ifaceWidget.getComponent());
        mcastPanel.add(addrWidget.getComponent());
        mcastPanel.add(serialWidget.getComponent());
        mcastPanel.add(ucastLink1Widget.getComponent());
        mcastPanel.add(ucastLink2Widget.getComponent());
        mcastPanel.add(addAddressButton);
        mcastPanel.setMaximumSize(mcastPanel.getPreferredSize());
        mcastPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        for (final String option : OPTIONS) {
            final int size;
            size = OPTION_SIZES.getOrDefault(option, 40);
            final Widget w = widgetFactory.createInstance(null, optionDefaults.get(option), optionValues.get(option),
                    '^' + OPTION_REGEXPS.get(option) + "\\s*$", size, Widget.NO_ABBRV,
                    new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL), Widget.NO_BUTTON);
            optionsWidgets.put(option, w);
            w.setAlwaysEditable(true);
            w.addListeners(getOptionListener());
        }

        /* dopd */
        dopdWidget = new JCheckBox(Tools.getString("Dialog.Cluster.HbConfig.UseDopdCheckBox"), null, false);
        dopdWidget.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        dopdWidget.setToolTipText(Tools.getString("Dialog.Cluster.HbConfig.UseDopdCheckBox.ToolTip"));
        dopdWidget.addItemListener(e -> {
            final Thread thread = new Thread(() -> updateConfigPanelEditable(true));
            thread.start();
        });

        /* mgmtd */
        mgmtdWidget = new JCheckBox(Tools.getString("Dialog.Cluster.HbConfig.UseMgmtdCheckBox"), null, false);
        mgmtdWidget.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        mgmtdWidget.setToolTipText(Tools.getString("Dialog.Cluster.HbConfig.UseMgmtdCheckBox.ToolTip"));
        mgmtdWidget.addItemListener(e -> {
            final Thread thread = new Thread(() -> updateConfigPanelEditable(true));
            thread.start();
        });
        makeConfigButton.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pane.add(makeConfigButton);
        return pane;
    }

    @Override
    protected boolean skipButtonEnabled() {
        return true;
    }

    private WidgetListener getOptionListener() {
        return new WidgetListener() {
            @Override
            public void check(final Value value) {
                if (fieldCheckLatch.getCount() > 0) {
                    return;
                }
                for (final String option : OPTIONS) {
                    final Widget w = optionsWidgets.get(option);
                    if (w != null) {
                        if (checkRegexp(w.getRegexp(), w.getStringValue())) {
                            w.setBackground(null, null, true);
                        } else {
                            w.wrongValue();
                        }
                    }
                }
                access.setAccessible(makeConfigButton, AccessMode.ADMIN);
            }
        };
    }

    /** Checks regexp. */
    private boolean checkRegexp(final String regexp, final CharSequence value) {
        if (regexp != null) {
            final Pattern p = Pattern.compile(regexp);
            final Matcher m = p.matcher(value);
            return m.matches();
        }
        return true;
    }
}
