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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
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

import lcmc.Exceptions.IllegalVersionException;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.service.NetworkService;
import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.ExecCommandThread;
import lcmc.cluster.ui.widget.Check;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.cluster.ui.wizard.corosync.CorosyncPacemakerConfig;
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
import lcmc.crm.domain.AisCastAddress;
import lcmc.crm.service.Corosync;
import lcmc.crm.service.Openais;
import lcmc.drbd.domain.NetInterface;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * An implementation of a dialog where corosync/openais is initialized on all
 * hosts.
 */
@Named
final class CoroConfig extends DialogCluster {
    private static final Logger LOG = LoggerFactory.getLogger(CoroConfig.class);
    private static final Value MCAST_TYPE = new StringValue("mcast");
    private static final int ADDR_COMBOBOX_WIDTH = 100;
    private static final int PORT_COMBOBOX_WIDTH = 60;
    private static final int TYPE_COMBOBOX_WIDTH = 80;
    private static final int INTERFACE_COMBOBOX_WIDTH = 80;
    private static final int REMOVE_BUTTON_WIDTH  = 100;
    private static final int REMOVE_BUTTON_HEIGHT = 14;
    private static final String CHECKBOX_EDIT_CONFIG_STRING = Tools.getString(
                                                                    "Dialog.Cluster.CoroConfig.Checkbox.EditConfig");
    private static final String CHECKBOX_SEE_EXISTING_STRING = Tools.getString(
                                                                    "Dialog.Cluster.CoroConfig.Checkbox.SeeExisting");
    private static final String OPENAIS_CONF_READ_ERROR_STRING = "error: read error";
    private static final String NEWLINE = "\\r?\\n";
    private static final String SPACE_TAB = "        ";
    private JPanel mcastPanel;
    /** Set of mcast etc. addresses. */
    private final Collection<AisCastAddress> aisCastAddresses = new LinkedHashSet<>();
    private final JLabel configStatus = new JLabel("");
    /**
     * Connection type pulldown menu: mcast ...
     */
    private Widget typeWidget;
    private Widget ifaceWidget;
    private Widget addrWidget;
    private Widget portWidget;
    private MyButton addAddressButton;
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

    private final InitCluster initClusterDialog;
    private final Application application;
    private final SwingUtils swingUtils;
    private final WidgetFactory widgetFactory;
    private MyButton makeConfigButton;
    private final NetworkService networkService;
    private final Access access;

    public CoroConfig(MainData mainData, InitCluster initClusterDialog, Application application, SwingUtils swingUtils,
            WidgetFactory widgetFactory, NetworkService networkService, Access access, Provider<ProgressBar> progressBarProvider) {
        super(application, swingUtils, widgetFactory, mainData, progressBarProvider);
        this.initClusterDialog = initClusterDialog;
        this.application = application;
        this.swingUtils = swingUtils;
        this.widgetFactory = widgetFactory;
        this.networkService = networkService;
        this.access = access;
    }

    @Override
    public void init(final WizardDialog previousDialog, final Cluster cluster) {
        super.init(previousDialog, cluster);
        makeConfigButton = widgetFactory.createButton(Tools.getString("Dialog.Cluster.CoroConfig.CreateAisConfig"));
        final Host[] hosts = getCluster().getHostsArray();
        configs = new String[hosts.length];
        makeConfigButton.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
        makeConfigButton.addActionListener(e -> {
            final Thread thread = new Thread(() -> {
                swingUtils.invokeLater(() -> makeConfigButton.setEnabled(false));
                disableComponents();
                final StringBuilder config = aisConfigHead(false);
                int ringnumber = 0;
                for (final AisCastAddress ca : aisCastAddresses) {
                    config.append('\n');
                    config.append(ca.getConfigString(ringnumber, "\t"));
                    config.append('\n');
                    ringnumber++;
                }
                config.append("}\n");
                final String serviceVersion = hosts[0].getDistString("Pacemaker.Service.Ver");
                String corosyncVersion = hosts[0].getHostParser().getCorosyncVersion();
                config.append(new CorosyncPacemakerConfig("\t", serviceVersion, corosyncVersion, hosts).create());
                if (hosts[0].getHostParser().isCorosyncInstalled()) {
                    Corosync.createCorosyncConfig(hosts, config);
                } else {
                    Openais.createAISConfig(hosts, config);
                }
                final boolean configOk = updateOldAisConfig();
                if (hosts[0].getHostParser().isCorosyncInstalled() && !hosts[0].getHostParser().isOpenaisWrapper()) {
                    Corosync.reloadCorosyncs(hosts);
                } else {
                    Openais.reloadOpenaises(hosts);
                }
                enableComponents();
                final List<String> incorrect = new ArrayList<>();
                final List<String> changed = new ArrayList<>();
                if (configOk) {
                    hideRetryButton();
                } else {
                    incorrect.add("config failed");
                }
                nextButtonSetEnabled(new Check(incorrect, changed));
                if (configOk && !application.getAutoClusters().isEmpty()) {
                    Tools.sleep(1000);
                    pressNextButton();
                }
            });
            thread.start();
        });
    }

    @Override
    public WizardDialog nextDialog() {
        initClusterDialog.init(this, getCluster());
        return initClusterDialog;
    }

    @Override
    protected String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.CoroConfig.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Cluster.CoroConfig.Description");
    }

    @Override
    public String nextButton() {
        return Tools.getString("Dialog.Cluster.CoroConfig.NextButton");
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
            final boolean configOk = updateOldAisConfig();
            swingUtils.invokeLater(() -> makeConfigButton.setEnabled(false));
            enableComponents();
            final List<String> incorrect = new ArrayList<>();
            final List<String> changed = new ArrayList<>();
            if (!configOk) {
                incorrect.add("config failed");
            }
            nextButtonSetEnabled(new Check(incorrect, changed));
            if (configOk && !application.getAutoClusters().isEmpty()) {
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
        /*
          interface {
                  ringnumber: 0
                  bindnetaddr: 192.168.2.0
                  mcastaddr: 226.94.1.1
                  mcastport: 5405
          }
        */
        final Pattern totemP = Pattern.compile("\\s*totem\\s*\\{\\s*");
        final Pattern interfaceP = Pattern.compile("\\s*interface\\s+\\{\\s*");
        final Pattern serviceP = Pattern.compile("\\s*service\\s*\\{\\s*");
        final Pattern endParenthesesP = Pattern.compile("^\\s*}\\s*");
        final Pattern pattern = Pattern.compile("\\s*(\\S+):\\s*(\\S+)\\s*");
        aisCastAddresses.clear();
        boolean inTotem = false;
        boolean inInterface = false;
        boolean inService = false;
        String mcastaddr = null;
        String mcastport = null;
        String bindnetaddr = null;
        for (final String line : config) {
            final Matcher totemM = totemP.matcher(line);
            final Matcher serviceM = serviceP.matcher(line);
            if (!inService && serviceM.matches()) {
                inService = true;
            } else if (!inTotem && totemM.matches()) {
                inTotem = true;
            } else if (inTotem && !inInterface) {
                final Matcher interfaceM = interfaceP.matcher(line);
                if (interfaceM.matches()) {
                    inInterface = true;
                } else {
                    final Matcher endParenthesesM = endParenthesesP.matcher(line);
                    if (endParenthesesM.matches()) {
                        inTotem = false;
                    }
                }
            } else if (inInterface) {
                final Matcher endParenthesesM = endParenthesesP.matcher(line);
                if (endParenthesesM.matches()) {
                    aisCastAddresses.add(new AisCastAddress(MCAST_TYPE.getValueForConfig(),
                                                            bindnetaddr,
                                                            mcastaddr,
                                                            mcastport));
                    inInterface = false;
                    mcastaddr = null;
                    mcastport = null;
                    bindnetaddr = null;
                } else {
                    final Matcher lineM = pattern.matcher(line);
                    if (lineM.matches()) {
                        final String name  = lineM.group(1);
                        final String value = lineM.group(2);
                        if ("mcastaddr".equals(name)) {
                            mcastaddr = value;
                        } else if ("mcastport".equals(name)) {
                            mcastport = value;
                        } else if ("bindnetaddr".equals(name)) {
                            bindnetaddr = value;
                        }
                    }
                }
            } else if (inService) {
                final Matcher endParenthesesM = endParenthesesP.matcher(line);
                if (endParenthesesM.matches()) {
                    inService = false;
                } else {
                    final Matcher lineM = pattern.matcher(line);
                    if (lineM.matches()) {
                        final String name  = lineM.group(1);
                        final String value = lineM.group(2);
                        //TODO: nothing is here, yet
                    }
                }
            }
        }
        checkInterface();
    }

    /**
     * Checks whether the old config is the same on all hosts, if it exists at
     * all and enable the components accordingly.
     * Returns whether the configs are ok and the same on all hosts.
     */
    private boolean updateOldAisConfig() { /* is run in a thread */
        final Host[] hosts = getCluster().getHostsArray();
        final ExecCommandThread[] ts = new ExecCommandThread[hosts.length];
        configStatus.setText(Tools.getString("Dialog.Cluster.CoroConfig.Loading"));

        String cf = "/etc/corosync/corosync.conf";
        String command = "Corosync.getAisConfig";
        if (!hosts[0].getHostParser().isCorosyncInstalled()) {
            cf = "/etc/ais/openais.conf";
            command = "OpenAIS.getAisConfig";
        }
        final String configFile = cf;
        int i = 0;
        for (final Host h : hosts) {
            final int index = i;
            ts[i] = h.execCommand(new ExecCommandConfig()
                                      .commandString(command)
                                      .execCallback(new ExecCallback() {
                                          @Override
                                          public void done(final String answer) {
                                              configs[index] = answer;
                                          }

                                          @Override
                                          public void doneError(final String answer, final int errorCode) {
                                              configs[index] = OPENAIS_CONF_READ_ERROR_STRING;
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

        boolean noConfigs = true;
        boolean configOk = false;
        if (configs[0].equals(OPENAIS_CONF_READ_ERROR_STRING)) {
            swingUtils.invokeLater(
                    () -> configStatus.setText(hosts[0] + Tools.getString("Dialog.Cluster.CoroConfig.NoConfigFound")));
            retry();
            if (!application.getAutoClusters().isEmpty()) {
                Tools.sleep(1000);
                addAddressButton.pressButton();
            }
        } else {
            noConfigs = false;
            int j;
            for (j = 1; j < configs.length; j++) {
                final Host host = hosts[j];
                if (configs[j].equals(OPENAIS_CONF_READ_ERROR_STRING)) {
                    swingUtils.invokeLater(() -> configStatus.setText(
                            host + ": " + configFile + Tools.getString("Dialog.Cluster.CoroConfig.NoConfigFound")));
                    break;
                } else if (!configs[0].equals(configs[j])) {
                    swingUtils.invokeLater(
                            () -> configStatus.setText(Tools.getString("Dialog.Cluster.CoroConfig.ConfigsNotTheSame")));
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
                    configStatus.setText(configFile + Tools.getString("Dialog.Cluster.CoroConfig.ais.conf.ok"));
                    configCheckbox.setSelected(false);
                    if (editableConfig) {
                        configCheckbox.setText(CHECKBOX_SEE_EXISTING_STRING);
                    } else {
                        configCheckbox.setText(CHECKBOX_EDIT_CONFIG_STRING);
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
                if (noConfigsF) {
                    configCheckbox.setText(CHECKBOX_SEE_EXISTING_STRING);
                } else {
                    configCheckbox.setText(CHECKBOX_EDIT_CONFIG_STRING);
                }
                configCheckbox.setSelected(false);
                statusPanel.setMaximumSize(statusPanel.getPreferredSize());
            });
            if (noConfigs) {
                updateConfigPanelEditable(false);
            } else {
                updateConfigPanelExisting();
            }
        }
        return configOk;
    }

    /** Shows all corosync or openais.conf config files. */
    private void updateConfigPanelExisting() {
        final Host[] hosts = getCluster().getHostsArray();
        swingUtils.invokeLater(() -> {
            makeConfigButton.setEnabled(false);
            configPanel.removeAll();
            final JPanel insideConfigPanel = new JPanel(new SpringLayout());
            int cols = 0;
            for (int i = 0; i < hosts.length; i++) {
                if (OPENAIS_CONF_READ_ERROR_STRING.equals(configs[i])) {
                    configs[i] = Tools.getString("Dialog.Cluster.CoroConfig.NoConfigFound");
                }
                final JLabel l = new JLabel(hosts[i].getName() + ':');
                l.setBackground(Color.WHITE);
                final JPanel labelP = new JPanel();
                labelP.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
                labelP.setLayout(new BoxLayout(labelP, BoxLayout.PAGE_AXIS));
                labelP.setAlignmentX(Component.TOP_ALIGNMENT);
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

    private void updateConfigPanelEditable(final boolean configChanged) {
        configChangedByUser = configChanged;
        final Host[] hosts = getCluster().getHostsArray();
        swingUtils.invokeLater(() -> {
            if (!configChanged) {
                makeConfigButton.setEnabled(false);
            }
            configPanel.removeAll();
            /* head */
            final String[] head = aisConfigHead(true).toString().split(NEWLINE);
            for (final String line : head) {
                configPanel.add(new JLabel(line));
            }
            /* addresses */
            int ringnumber = 0;
            for (final AisCastAddress c : aisCastAddresses) {
                configPanel.add(new JLabel(""));
                final String[] interfaceLines = c.getConfigString(ringnumber, SPACE_TAB).split(NEWLINE);
                ringnumber++;
                boolean firstLine = true;
                for (final String interfaceLine : interfaceLines) {
                    if (firstLine) {
                        configPanel.add(getComponentPanel(interfaceLine, getRemoveButton(c)));
                        firstLine = false;
                    } else {
                        configPanel.add(new JLabel(interfaceLine));
                    }
                }
            }

            if (aisCastAddresses.size() < 2) {
                final JLabel l;
                if (aisCastAddresses.isEmpty()) {
                    l = new JLabel(Tools.getString("Dialog.Cluster.CoroConfig.WarningAtLeastTwoInt"));
                } else {
                    // TODO: we need to check if there is bond interface
                    // and one is enough
                    l = new JLabel(Tools.getString("Dialog.Cluster.CoroConfig.WarningAtLeastTwoInt.OneMore"));
                }
                l.setForeground(Color.RED);
                configPanel.add(l);
                configPanel.add(new JLabel(""));
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
                        configScrollPane.getViewport().setViewPosition(l.getBounds().getLocation());
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

            configPanel.add(new JLabel(""));
            /* mcast etc combo boxes */
            configPanel.add(mcastPanel);
            configPanel.add(new JLabel("}"));

            /* service pacemaker */
            final String serviceVersion = hosts[0].getDistString("Pacemaker.Service.Ver");
            String corosyncVersion = hosts[0].getHostParser().getCorosyncVersion();
            final String[] pacemakerLines =
                    new CorosyncPacemakerConfig(SPACE_TAB, serviceVersion, corosyncVersion, hosts).create().split(NEWLINE);
            for (final String line : pacemakerLines) {
                configPanel.add(new JLabel(line));
            }
            configPanel.revalidate();
            configPanel.repaint();
            if (configChanged) {
                if (aisCastAddresses.isEmpty()) {
                    makeConfigButton.setEnabled(false);
                } else {
                    access.setAccessible(makeConfigButton, AccessMode.ADMIN);
                }
                if (!application.getAutoClusters().isEmpty() && !aisCastAddresses.isEmpty()) {
                    Tools.sleep(1000);
                    makeConfigButton.pressButton();
                }
            }
        });
    }

    private MyButton getRemoveButton(final AisCastAddress c) {
        final MyButton removeButton = widgetFactory.createButton(Tools.getString("Dialog.Cluster.CoroConfig.RemoveIntButton"));
        removeButton.setBackgroundColor(Tools.getDefaultColor("ConfigDialog.Button"));
        removeButton.setMaximumSize(new Dimension(REMOVE_BUTTON_WIDTH, REMOVE_BUTTON_HEIGHT));
        removeButton.setPreferredSize(new Dimension(REMOVE_BUTTON_WIDTH, REMOVE_BUTTON_HEIGHT));
        removeButton.addActionListener(e -> {
            final Thread t = new Thread(() -> {
                aisCastAddresses.remove(c);
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
        final Value type  = typeWidget.getValue();
        String address     = "";
        String bindnetaddr = "";
        String port        = "";

        if (MCAST_TYPE.equals(type)) {
            final NetInterface iface = (NetInterface) ifaceWidget.getValue();
            bindnetaddr = iface.getBindnetaddr();
            address = addrWidget.getStringValue();
            port = portWidget.getStringValue();
        }

        for (final AisCastAddress c : aisCastAddresses) {
            if (c.equals("\t", type.getValueForConfig(), bindnetaddr, address, port)) {
                swingUtils.invokeLater(() -> addAddressButton.setEnabled(false));
                return;
            }
        }
        swingUtils.invokeLater(() -> addAddressButton.setEnabled(true));
    }

    /** Plugins, not used from corosync 2.0. */
    private CharSequence plugins(final boolean fake) {
        final StringBuilder config = new StringBuilder(500);
        final String tab;
        if (fake) {
            tab = SPACE_TAB;
        } else {
            tab = "\t";
        }
        config.append("aisexec {\n");
        config.append(tab);
        config.append("user: root\n");
        config.append(tab);
        config.append("group: root\n}\n\ncorosync {\n");
        config.append(tab);
        config.append("user: root\n");
        config.append(tab);
        config.append("group: root\n}\n\namf {\n");
        config.append(tab);
        config.append("mode: disabled\n}\n\n");
        return config;
    }

    /** Returns the head of the corosync or openais config. */
    private StringBuilder aisConfigHead(final boolean fake) {
        final StringBuilder config = new StringBuilder(500);
        final String tab;
        if (fake) {
            config.append("## to be generated by LCMC ");
            tab = SPACE_TAB;
        } else {
            config.append("## generated by LCMC ");
            tab = "\t";
        }
        config.append(Tools.getRelease());
        config.append("\n\n");
        final Host[] hosts = getCluster().getHostsArray();
        boolean corosync2 = false;
        try {
           corosync2 = Tools.compareVersions(hosts[0].getHostParser().getCorosyncVersion(), "2") >= 0;
        } catch (final IllegalVersionException e) {
            LOG.appWarning("aisConfigHead: cannot compare corosync version: " + hosts[0].getHostParser().getCorosyncVersion());
        }
        if (!corosync2) {
            config.append(plugins(fake));
        }
        config.append("logging {\n");
        if (corosync2) {
            config.append(tab);
            config.append("fileline: off\n");
            config.append(tab);
            config.append("to_logfile: yes\n");
            config.append(tab);
            config.append("logfile: /var/log/cluster/corosync.log\n");
        }
        config.append(tab);
        config.append("to_stderr: no\n");
        config.append(tab);
        config.append("debug: off\n");
        config.append(tab);
        config.append("timestamp: on\n");
        config.append(tab);
        config.append("to_syslog: yes\n");
        if (corosync2) {
            config.append(tab);
            config.append("logger_subsys {\n");
            config.append(tab);
            config.append(tab);
            config.append("subsys: QUORUM\n");
            config.append(tab);
            config.append(tab);
            config.append("debug: off\n");
            config.append(tab);
            config.append("}\n");
        } else {
            config.append(tab);
            config.append("to_file: no\n");
            config.append(tab);
            config.append("syslog_facility: daemon\n");
        }
        config.append("}\n\ntotem {\n");
        config.append(tab);
        config.append("version: 2\n");
        config.append(tab);
        config.append("token: 3000\n");
        config.append(tab);
        config.append("secauth: on\n");
        if (!corosync2) {
            config.append(tab);
            config.append("token_retransmits_before_loss_const: 10\n");
            config.append(tab);
            config.append("join: 60\n");
            config.append(tab);
            config.append("consensus: 4000\n");
            config.append(tab);
            config.append("vsftype: none\n");
            config.append(tab);
            config.append("max_messages: 20\n");
            config.append(tab);
            config.append("clear_node_high_bit: yes\n");
            config.append(tab);
            config.append("threads: 0\n");
            config.append(tab);
            config.append("# nodeid: 1234\n");
        }
        config.append(tab);
        config.append("rrp_mode: active\n");
        return config;
    }


    /** Adds interface to the config panel. It must be called from a thread. */
    private void addInterface(final Value type) {
        String bindnetaddr = "";
        String addr        = "";
        String port        = "";
        if (MCAST_TYPE.equals(type)) {
            final NetInterface iface  = (NetInterface) ifaceWidget.getValue();
            if (iface == null) {
                LOG.appWarning("addInterface: cannot add null interface");
                return;
            }
            bindnetaddr = iface.getBindnetaddr();
            addr = addrWidget.getStringValue();
            port = portWidget.getStringValue();
        }
        aisCastAddresses.add(new AisCastAddress(type.getValueForConfig(), bindnetaddr, addr, port));
        updateConfigPanelEditable(true);
        checkInterface();
    }

    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        final Host[] hosts = getCluster().getHostsArray();
        final Value[] types = {MCAST_TYPE};

        typeWidget = widgetFactory.createInstance(Widget.GUESS_TYPE,
                                                  MCAST_TYPE,
                                                  types,
                                                  Widget.NO_REGEXP,
                                                  TYPE_COMBOBOX_WIDTH,
                                                  Widget.NO_ABBRV,
                                                  new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                                                  Widget.NO_BUTTON);
        typeWidget.setEnabled(false);

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
        ifaceWidget = widgetFactory.createInstance(
                                          Widget.Type.COMBOBOX,
                                          defaultNi,
                                          ni,
                                          Widget.NO_REGEXP,
                                          INTERFACE_COMBOBOX_WIDTH,
                                          Widget.NO_ABBRV,
                                          new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                                          Widget.NO_BUTTON);

        /* this matches something like this: 225.0.0.43 694 1 0
         * if you think that the regexp is too complicated for that, consider,
         * that it must match also during the thing is written.
         */
        final String regexp = "^[\\d.]+$";
        addrWidget = widgetFactory.createInstance(
                              Widget.GUESS_TYPE,
                              new StringValue(Tools.getDefault("Dialog.Cluster.CoroConfig.DefaultMCastAddress")),
                              Widget.NO_ITEMS,
                              regexp,
                              ADDR_COMBOBOX_WIDTH,
                              Widget.NO_ABBRV,
                              new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                              Widget.NO_BUTTON);

        typeWidget.addListeners(new WidgetListener() {
            @Override
            public void check(final Value value) {
                checkInterface();
            }
        });

        ifaceWidget.addListeners(new WidgetListener() {
            @Override
            public void check(final Value value) {
                checkInterface();
            }
        });

        final String portRegexp = "^\\d+$";
        portWidget = widgetFactory.createInstance(
                Widget.GUESS_TYPE,
                new StringValue(Tools.getDefault("Dialog.Cluster.CoroConfig.DefaultMCastPort")),
                Widget.NO_ITEMS,
                portRegexp,
                PORT_COMBOBOX_WIDTH,
                Widget.NO_ABBRV,
                new AccessMode(AccessMode.RO, AccessMode.NORMAL),
                Widget.NO_BUTTON);
        portWidget.addListeners(new WidgetListener() {
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

        addAddressButton = widgetFactory.createButton(Tools.getString("Dialog.Cluster.CoroConfig.AddIntButton"));
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
                    if (CHECKBOX_EDIT_CONFIG_STRING.equals(text)) {
                        updateConfigPanelEditable(configChangedByUser);
                        swingUtils.invokeLater(() -> {
                            configCheckbox.setText(CHECKBOX_SEE_EXISTING_STRING);
                            configCheckbox.setSelected(false);
                            statusPanel.setMaximumSize(statusPanel.getPreferredSize());
                        });
                    } else if (CHECKBOX_SEE_EXISTING_STRING.equals(text)) {
                        updateConfigPanelExisting();
                        swingUtils.invokeLater(() -> {
                            configCheckbox.setText(CHECKBOX_EDIT_CONFIG_STRING);
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
        mcastPanel.add(portWidget.getComponent());
        mcastPanel.add(addAddressButton);
        mcastPanel.setPreferredSize(mcastPanel.getMinimumSize());
        mcastPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        pane.add(makeConfigButton);
        return pane;
    }

    @Override
    protected boolean skipButtonEnabled() {
        return true;
    }
}
