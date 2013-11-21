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


package lcmc.gui.dialog.cluster;

import lcmc.utilities.MyButton;
import lcmc.utilities.Openais;
import lcmc.utilities.Corosync;
import lcmc.utilities.Tools;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.SSH.ExecCommandThread;
import lcmc.utilities.SSH;
import lcmc.utilities.WidgetListener;
import lcmc.data.Host;
import lcmc.data.ConfigData;
import lcmc.data.Cluster;
import lcmc.data.AisCastAddress;
import lcmc.data.resources.NetInterface;
import lcmc.data.AccessMode;
import lcmc.gui.SpringUtilities;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.gui.ProgressBar;
import lcmc.gui.dialog.WizardDialog;
import lcmc.Exceptions.IllegalVersionException;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.SpringLayout;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import javax.swing.JComponent;
import java.awt.Component;
import lcmc.data.StringValue;
import lcmc.data.Value;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * An implementation of a dialog where corosync/openais is initialized on all
 * hosts.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class CoroConfig extends DialogCluster {
    /** Logger. */
    private static final Logger LOG =
                                   LoggerFactory.getLogger(CoroConfig.class);
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Panel for mcast addresses. */
    private JPanel mcast;
    /** Set of mcast etc. addresses. */
    private final Set<AisCastAddress> aisCastAddresses =
                                        new LinkedHashSet<AisCastAddress>();
    /** Atatus of the config. For example does not exist. */
    private final JLabel configStatus = new JLabel("");
    /** Make config button. */
    private final MyButton makeConfigButton = new MyButton(
                Tools.getString("Dialog.Cluster.CoroConfig.CreateAisConfig"));
    /** Connection type pulldown menu: mcast ... */
    private Widget typeW;
    /** Interface pulldown menu. */
    private Widget ifaceW;
    /** Address field. */
    private Widget addrW;
    /** Port field. */
    private Widget portW;
    /** Add address button. */
    private MyButton addButton;
    /** Array with corosync.conf or openais.conf configs from all hosts. */
    private String[] configs;
    /** Status panel. */
    private JPanel statusPanel;
    /** Check box that allows to edit a new config are see the existing
     * configs. */
    private JCheckBox configCheckbox;
    /** Editable hearbeat config panel. */
    private final JPanel configPanel = new JPanel();
    /** Whether the config was changed by the user. */
    private boolean configChanged = false;
    /** Multicast type string. */
    private static final Value MCAST_TYPE = new StringValue("mcast");
    /** Width of the address combobox. */
    private static final int ADDR_COMBOBOX_WIDTH = 100;
    /** Width of the port combobox. */
    private static final int PORT_COMBOBOX_WIDTH = 60;
    /** Width of the type combobox. */
    private static final int TYPE_COMBOBOX_WIDTH = 80;
    /** Width of the interface combobox. */
    private static final int INTF_COMBOBOX_WIDTH = 80;
    /** Width of the remove button. */
    private static final int REMOVE_BUTTON_WIDTH  = 100;
    /** Height of the remove button. */
    private static final int REMOVE_BUTTON_HEIGHT = 14;
    /** Checkbox text (Edit the config). */
    private static final String EDIT_CONFIG_STRING = Tools.getString(
                             "Dialog.Cluster.CoroConfig.Checkbox.EditConfig");
    /** Checkbox text (See existing). */
    private static final String SEE_EXISTING_STRING = Tools.getString(
                             "Dialog.Cluster.CoroConfig.Checkbox.SeeExisting");
    /** openais read error string. */
    private static final String AIS_CONF_ERROR_STRING = "error: read error";
    /** Newline. */
    private static final String NEWLINE = "\\r?\\n";
    /** Tabulator made from spaces. */
    private static final String SPACE_TAB = "        ";
    /** Config scroll pane. */
    private volatile JScrollPane configScrollPane = null;
    /** Whether the config pane was already moved to the position. */
    private volatile boolean alreadyMoved = false;

    /** Prepares a new <code>CoroConfig</code> object. */
    CoroConfig(final WizardDialog previousDialog, final Cluster cluster) {
        super(previousDialog, cluster);
        final Host[] hosts = getCluster().getHostsArray();
        configs = new String[hosts.length];
        makeConfigButton.setBackgroundColor(
                                  Tools.getDefaultColor("ConfigDialog.Button"));
        makeConfigButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                Tools.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        makeConfigButton.setEnabled(false);
                                    }
                                });
                                disableComponents();
                                final StringBuilder config =
                                                        aisConfigHead(false);
                                int ringnumber = 0;
                                for (AisCastAddress ca : aisCastAddresses) {
                                    config.append('\n');
                                    config.append(ca.getConfigString(
                                                                ringnumber++,
                                                                "\t"));
                                    config.append('\n');
                                }
                                config.append("}\n");
                                final String serviceVersion =
                                            hosts[0].getDistString(
                                                    "Pacemaker.Service.Ver");
                                config.append(aisConfigPacemaker(
                                                    "\t",
                                                    serviceVersion));
                                if (hosts[0].isCorosync()) {
                                    Corosync.createCorosyncConfig(hosts,
                                                                  config);
                                } else {
                                    Openais.createAISConfig(hosts, config);
                                }
                                boolean configOk = updateOldAisConfig();
                                if (hosts[0].isCorosync()
                                    && !hosts[0].isOpenaisWrapper()) {
                                    Corosync.reloadCorosyncs(hosts);
                                } else {
                                    Openais.reloadOpenaises(hosts);
                                }
                                enableComponents();
                                if (configOk) {
                                    hideRetryButton();
                                    nextButtonSetEnabled(true);
                                    if (!Tools.getConfigData()
                                              .getAutoClusters().isEmpty()) {
                                        Tools.sleep(1000);
                                        pressNextButton();
                                    }
                                }
                            }
                        }
                    );
                    thread.start();
                }
            });
    }

    /** Returns the successor of this dialog. */
    @Override
    public WizardDialog nextDialog() {
        return new Init(this, getCluster());
    }

    /** Returns title of this dialog. */
    @Override
    protected String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.CoroConfig.Title");
    }

    /** Returns description of this dialog. */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Cluster.CoroConfig.Description");
    }

    /** Returns localized string of Next button. */
    @Override
    public String nextButton() {
        return Tools.getString("Dialog.Cluster.CoroConfig.NextButton");
    }

    /** Inits the dialog. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setBackground(
                           Tools.getDefaultColor("ConfigDialog.Background"));
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        final Thread thread = new Thread(
            new Runnable() {
                @Override
                public void run() {
                    boolean configOk = updateOldAisConfig();
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            makeConfigButton.setEnabled(false);
                        }
                    });
                    enableComponents();
                    if (configOk) {
                        nextButtonSetEnabled(true);
                        if (!Tools.getConfigData().getAutoClusters()
                                                  .isEmpty()) {
                            Tools.sleep(1000);
                            pressNextButton();
                        }
                    }
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
        final Pattern interfaceP =
                            Pattern.compile("\\s*interface\\s+\\{\\s*");
        final Pattern serviceP = Pattern.compile("\\s*service\\s*\\{\\s*");
        final Pattern endParenthesesP = Pattern.compile("^\\s*}\\s*");
        final Pattern pattern =
                            Pattern.compile("\\s*(\\S+):\\s*(\\S+)\\s*");
        aisCastAddresses.clear();
        boolean inTotem = false;
        boolean inInterface = false;
        boolean inService = false;
        String mcastaddr = null;
        String mcastport = null;
        String bindnetaddr = null;
        for (String line : config) {
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
                    final Matcher endParenthesesM =
                                            endParenthesesP.matcher(line);
                    if (endParenthesesM.matches()) {
                        inTotem = false;
                    }
                }
            } else if (inInterface) {
                final Matcher endParenthesesM =
                                            endParenthesesP.matcher(line);
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
                final Matcher endParenthesesM =
                                            endParenthesesP.matcher(line);
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
        boolean noConfigs = true;
        boolean configOk = false;
        ExecCommandThread[] ts = new ExecCommandThread[hosts.length];
        configStatus.setText(
                   Tools.getString("Dialog.Cluster.CoroConfig.Loading"));
        int i = 0;

        String cf = "/etc/corosync/corosync.conf";
        String command = "Corosync.getAisConfig";
        if (!hosts[0].isCorosync()) {
            cf = "/etc/ais/openais.conf";
            command = "OpenAIS.getAisConfig";
        }
        final String configFile = cf;
        for (Host h : hosts) {
            final int index = i;
            ts[i] = h.execCommand(
                             command,
                             (ProgressBar) null,
                             new ExecCallback() {
                                 @Override
                                 public void done(final String ans) {
                                     configs[index] = ans;
                                 }
                                 @Override
                                 public void doneError(final String ans,
                                                       final int exitCode) {
                                     configs[index] = AIS_CONF_ERROR_STRING;
                                 }
                             },
                             null,   /* ConvertCmdCallback */
                             false,  /* outputVisible */
                             SSH.DEFAULT_COMMAND_TIMEOUT);
            i++;
        }
        for (ExecCommandThread t : ts) {
            /* wait for all of them */
            try {
                t.join();
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (configs[0].equals(AIS_CONF_ERROR_STRING)) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    configStatus.setText(hosts[0] + Tools.getString(
                                  "Dialog.Cluster.CoroConfig.NoConfigFound"));
                }
            });
            retry();
            if (!Tools.getConfigData().getAutoClusters().isEmpty()) {
                Tools.sleep(1000);
                addButton.pressButton();
            }
        } else {
            noConfigs = false;
            int j;
            for (j = 1; j < configs.length; j++) {
                final Host host = hosts[j];
                if (configs[j].equals(AIS_CONF_ERROR_STRING)) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            configStatus.setText(host + ": "
                                 + configFile
                                 + Tools.getString(
                                    "Dialog.Cluster.CoroConfig.NoConfigFound"));
                        }
                    });
                    break;
                } else if (!configs[0].equals(configs[j])) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            configStatus.setText(Tools.getString(
                               "Dialog.Cluster.CoroConfig.ConfigsNotTheSame"));
                        }
                    });
                    break;
                }
            }
            if (j < configs.length) {
                retry();
            } else {
                boolean generated = false;
                final Pattern p = Pattern.compile(
                                        "## generated by (drbd-gui|LCMC).*",
                                        Pattern.DOTALL);
                final Matcher m = p.matcher(configs[0]);
                if (m.matches()) {
                    generated = true;
                }
                final boolean editableConfig = generated;
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        configStatus.setText(
                              configFile + Tools.getString(
                                    "Dialog.Cluster.CoroConfig.ais.conf.ok"));
                        configCheckbox.setSelected(false);
                        if (editableConfig) {
                            configCheckbox.setText(SEE_EXISTING_STRING);
                        } else {
                            configCheckbox.setText(EDIT_CONFIG_STRING);
                        }
                        statusPanel.setMaximumSize(
                                    statusPanel.getPreferredSize());
                    }
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
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (noConfigsF) {
                        configCheckbox.setText(SEE_EXISTING_STRING);
                    } else {
                        configCheckbox.setText(EDIT_CONFIG_STRING);
                    }
                    configCheckbox.setSelected(false);
                    statusPanel.setMaximumSize(
                                statusPanel.getPreferredSize());
                }
            });
            if (noConfigs) {
                updateConfigPanelEditable(false);
            } else {
                //setNewConfig(configs[0]);
                updateConfigPanelExisting();
            }
        }
        return configOk;
    }

    /** Shows all corosync or openais.conf config files. */
    private void updateConfigPanelExisting() {
        final Host[] hosts = getCluster().getHostsArray();
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                makeConfigButton.setEnabled(false);
                configPanel.removeAll();
                final JPanel insideConfigPanel = new JPanel(
                                                        new SpringLayout());
                int cols = 0;
                for (int i = 0; i < hosts.length; i++) {
                    if (AIS_CONF_ERROR_STRING.equals(configs[i])) {
                        configs[i] =
                            Tools.getString(
                                   "Dialog.Cluster.CoroConfig.NoConfigFound");
                    }
                    final JLabel l = new JLabel(hosts[i].getName() + ":");
                    l.setBackground(Color.WHITE);
                    final JPanel labelP = new JPanel();
                    labelP.setBackground(
                             Tools.getDefaultColor("ConfigDialog.Background"));
                    labelP.setLayout(new BoxLayout(labelP, BoxLayout.Y_AXIS));
                    labelP.setAlignmentX(Component.TOP_ALIGNMENT);
                    labelP.add(l);
                    insideConfigPanel.add(labelP);
                    final JTextArea ta = new JTextArea(configs[i]);
                    ta.setEditable(false);
                    insideConfigPanel.add(ta);
                    cols += 2;
                }
                if (cols > 0) {
                    SpringUtilities.makeCompactGrid(insideConfigPanel,
                                                    1, cols,
                                                    1, 1,
                                                    1, 1);
                    configPanel.add(insideConfigPanel);
                }
                configPanel.revalidate();
                configPanel.repaint();
            }
        });
    }

    /** Updates the config panel. */
    private void updateConfigPanelEditable(final boolean configChanged) {
        this.configChanged = configChanged;
        final Host[] hosts = getCluster().getHostsArray();
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!configChanged) {
                    makeConfigButton.setEnabled(false);
                }
                configPanel.removeAll();
                /* head */
                final String[] head =
                           aisConfigHead(true).toString().split(NEWLINE);
                for (String line : head) {
                    configPanel.add(new JLabel(line));
                }
                /* addresses */
                int ringnumber = 0;
                for (final AisCastAddress c : aisCastAddresses) {
                    configPanel.add(new JLabel(""));
                    final String[] interfaceLines =
                                c.getConfigString(ringnumber++,
                                                  SPACE_TAB).split(NEWLINE);
                    boolean firstLine = true;
                    for (final String interfaceLine : interfaceLines) {
                        if (firstLine) {
                            configPanel.add(getComponentPanel(
                                                          interfaceLine,
                                                          getRemoveButton(c)));
                            firstLine = false;
                        } else {
                            configPanel.add(new JLabel(interfaceLine));
                        }
                    }
                }

                if (aisCastAddresses.size() < 2) {
                    JLabel l;
                    if (aisCastAddresses.size() < 1) {
                        l = new JLabel(Tools.getString(
                            "Dialog.Cluster.CoroConfig.WarningAtLeastTwoInt"));
                    } else {
                        // TODO: we need to check if there is bond interface
                        // and one is enough
                        l = new JLabel(Tools.getString(
                     "Dialog.Cluster.CoroConfig.WarningAtLeastTwoInt.OneMore"));
                    }
                    l.setForeground(Color.RED);
                    configPanel.add(l);
                    configPanel.add(new JLabel(""));
                    final JLabel label = l;
                    label.addComponentListener(new ComponentListener() {
                        @Override
                        public void componentHidden(final ComponentEvent e) {
                            /* do nothing */
                        }

                        @Override
                        public void componentMoved(final ComponentEvent e) {
                            if (alreadyMoved) {
                                return;
                            }
                            alreadyMoved = true;
                            configScrollPane.getViewport().setViewPosition(
                                              label.getBounds().getLocation());
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
                configPanel.add(mcast);
                configPanel.add(new JLabel("}"));

                /* service pacemaker */
                final String serviceVersion =
                            hosts[0].getDistString(
                                    "Pacemaker.Service.Ver");
                final String[] pacemakerLines =
                        aisConfigPacemaker(SPACE_TAB,
                                           serviceVersion).toString()
                                                          .split(NEWLINE);
                for (String line : pacemakerLines) {
                    configPanel.add(new JLabel(line));
                }
                configPanel.revalidate();
                configPanel.repaint();
                if (configChanged) {
                    if (aisCastAddresses.isEmpty()) {
                        makeConfigButton.setEnabled(false);
                    } else {
                        Tools.getGUIData().setAccessible(
                                                makeConfigButton,
                                                ConfigData.AccessType.ADMIN);
                    }
                    if (!Tools.getConfigData().getAutoClusters().isEmpty()
                        && !aisCastAddresses.isEmpty()) {
                        Tools.sleep(1000);
                        makeConfigButton.pressButton();
                    }
                }
            }
        });
    }

    /** Returns remove address button. */
    private MyButton getRemoveButton(final AisCastAddress c) {
        final MyButton removeButton = new MyButton(
              Tools.getString("Dialog.Cluster.CoroConfig.RemoveIntButton"));
        removeButton.setBackgroundColor(
                            Tools.getDefaultColor("ConfigDialog.Button"));
        removeButton.setMaximumSize(new Dimension(REMOVE_BUTTON_WIDTH,
                                                  REMOVE_BUTTON_HEIGHT));
        removeButton.setPreferredSize(new Dimension(REMOVE_BUTTON_WIDTH,
                                                    REMOVE_BUTTON_HEIGHT));
        removeButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            aisCastAddresses.remove(c);
                            updateConfigPanelEditable(true);
                            checkInterface();
                        }
                    });
                    t.start();
                }
            });
        return removeButton;
    }

    /**
     * Checks interface if it already exists and enables/disables the 'add
     * button' accordingly.
     */
    private void checkInterface() {
        final Value type  = typeW.getValue();
        String address     = "";
        String bindnetaddr = "";
        String port        = "";

        if (MCAST_TYPE.equals(type)) {
            final NetInterface iface = (NetInterface) ifaceW.getValue();
            bindnetaddr = iface.getBindnetaddr();
            address = addrW.getStringValue();
            port = portW.getStringValue();
        }

        for (final AisCastAddress c : aisCastAddresses) {
            if (c.equals("\t", type.getValueForConfig(), bindnetaddr, address, port)) {
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        addButton.setEnabled(false);
                    }
                });
                return;
            }
        }
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                addButton.setEnabled(true);
            }
        });
    }

    /** Plugins, not used from corosync 2.0. */
    private StringBuilder plugins(final boolean fake) {
        final StringBuilder config = new StringBuilder(500);
        String tab;
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
        String tab;
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
           corosync2 =
                Tools.compareVersions(hosts[0].getCorosyncVersion(), "2") >= 0;
        } catch (IllegalVersionException e) {
            LOG.appWarning("aisConfigHead: cannot compare corosync version: "
                           + hosts[0].getCorosyncVersion());
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


    /**
     * Returns the part of the config.
     */
    private StringBuilder aisConfigPacemaker(final String tab,
                                             final String serviceVersion) {
        final StringBuilder config = new StringBuilder(120);
        final Host[] hosts = getCluster().getHostsArray();
        boolean corosync2 = false;
        try {
           corosync2 =
                Tools.compareVersions(hosts[0].getCorosyncVersion(), "2") >= 0;
        } catch (IllegalVersionException e) {
            LOG.appWarning("aisConfigPacemaker: cannot compare corosync version: "
                           + hosts[0].getCorosyncVersion());
        }
        if (corosync2) {
            config.append("\nquorum {\n");
            config.append(tab);
            config.append("provider: corosync_votequorum\n");
            config.append(tab);
            config.append("expected_votes: ");
            config.append(hosts.length);
            config.append("\n}\n");
        } else {
            config.append("\nservice {\n");
            config.append(tab);
            config.append("ver: ");
            config.append(serviceVersion);
            config.append('\n');
            config.append(tab);
            config.append("name: pacemaker\n");
            config.append(tab);
            config.append("use_mgmtd: no\n");
            config.append("}\n");
        }
        return config;
    }

    /** Adds interface to the config panel. It must be called from a thread. */
    private void addInterface(final Value type) {
        String bindnetaddr = "";
        String addr        = "";
        String port        = "";
        if (MCAST_TYPE.equals(type)) {
            final NetInterface iface  = (NetInterface) ifaceW.getValue();
            if (iface == null) {
                LOG.appWarning("addInterface: cannot add null interface");
                return;
            }
            bindnetaddr = iface.getBindnetaddr();
            addr = addrW.getStringValue();
            port = portW.getStringValue();
        }
        aisCastAddresses.add(
            new AisCastAddress(type.getValueForConfig(),
                               bindnetaddr,
                               addr,
                               port));
        updateConfigPanelEditable(true);
        checkInterface();
    }

    /** Returns panel where user can edit the config. */
    @Override
    protected JComponent getInputPane() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        final Host[] hosts = getCluster().getHostsArray();
        final Value[] types = {MCAST_TYPE};

        typeW = WidgetFactory.createInstance(
                                   Widget.GUESS_TYPE,
                                   MCAST_TYPE,
                                   types,
                                   Widget.NO_REGEXP,
                                   TYPE_COMBOBOX_WIDTH,
                                   Widget.NO_ABBRV,
                                   new AccessMode(ConfigData.AccessType.RO,
                                                  !AccessMode.ADVANCED),
                                   Widget.NO_BUTTON);
        typeW.setEnabled(false);

        final NetInterface[] ni = hosts[0].getNetInterfaces();
        NetInterface defaultNi = null;
        for (final NetInterface n : ni) {
            /* skip lo */
            if (!n.isLocalHost()) {
                defaultNi = n;
                break;
            }
        }
        if (defaultNi == null) {
            LOG.appError("getInputPane: " + hosts[0].getName()
                         + ": missing network interfaces");
        }
        ifaceW = WidgetFactory.createInstance(
                                    Widget.Type.COMBOBOX,
                                    defaultNi,
                                    ni,
                                    Widget.NO_REGEXP,
                                    INTF_COMBOBOX_WIDTH,
                                    Widget.NO_ABBRV,
                                    new AccessMode(ConfigData.AccessType.RO,
                                                   false), /* only adv. mode */
                                    Widget.NO_BUTTON);

        /* this matches something like this: 225.0.0.43 694 1 0
         * if you think that the regexp is too complicated for that, consider,
         * that it must match also during the thing is written.
         */
        final String regexp = "^[\\d.]+$";
        addrW = WidgetFactory.createInstance(
              Widget.GUESS_TYPE,
              new StringValue(Tools.getDefault("Dialog.Cluster.CoroConfig.DefaultMCastAddress")),
              Widget.NO_ITEMS,
              regexp,
              ADDR_COMBOBOX_WIDTH,
              Widget.NO_ABBRV,
              new AccessMode(ConfigData.AccessType.RO,
                             !AccessMode.ADVANCED),
              Widget.NO_BUTTON);

        typeW.addListeners(new WidgetListener() {
                                @Override
                                public void check(final Object value) {
                                    checkInterface();
                                }
                            });

        ifaceW.addListeners(new WidgetListener() {
                                 @Override
                                 public void check(final Object value) {
                                     checkInterface();
                                 }
                             });

        final String portRegexp = "^\\d+$";
        portW = WidgetFactory.createInstance(
                Widget.GUESS_TYPE,
                new StringValue(Tools.getDefault("Dialog.Cluster.CoroConfig.DefaultMCastPort")),
                Widget.NO_ITEMS,
                portRegexp,
                PORT_COMBOBOX_WIDTH,
                Widget.NO_ABBRV,
                new AccessMode(ConfigData.AccessType.RO,
                               !AccessMode.ADVANCED),
                Widget.NO_BUTTON);
        portW.addListeners(new WidgetListener() {
                                 @Override
                                 public void check(final Object value) {
                                     checkInterface();
                                 }
                             });

        addrW.addListeners(new WidgetListener() {
                                 @Override
                                 public void check(final Object value) {
                                     checkInterface();
                                 }
                             });

        addButton = new MyButton(
                     Tools.getString("Dialog.Cluster.CoroConfig.AddIntButton"));
        addButton.setBackgroundColor(
                                Tools.getDefaultColor("ConfigDialog.Button"));
        addButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Value type = typeW.getValue();
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            addInterface(type);
                        }
                    });
                    thread.start();
                }
            });

        configScrollPane = new JScrollPane(
                                    configPanel,
                                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                                   );
        configScrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE,
                                                        150));
        statusPanel = new JPanel();
        statusPanel.add(configStatus);
        configCheckbox = new JCheckBox("-----", true);
        configCheckbox.setBackground(
                       Tools.getDefaultColor("ConfigDialog.Background.Light"));

        Tools.getGUIData().setAccessible(configCheckbox,
                                         ConfigData.AccessType.ADMIN);
        configCheckbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                final String text = configCheckbox.getText();
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (EDIT_CONFIG_STRING.equals(text)) {
                                updateConfigPanelEditable(configChanged);
                                Tools.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        configCheckbox.setText(
                                                        SEE_EXISTING_STRING);
                                        configCheckbox.setSelected(false);
                                        statusPanel.setMaximumSize(
                                               statusPanel.getPreferredSize());
                                    }
                                });
                            } else if (SEE_EXISTING_STRING.equals(text)) {
                                updateConfigPanelExisting();
                                Tools.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        configCheckbox.setText(
                                                        EDIT_CONFIG_STRING);
                                        configCheckbox.setSelected(false);
                                        statusPanel.setMaximumSize(
                                               statusPanel.getPreferredSize());
                                    }
                                });
                            }
                        }
                    });
                    thread.start();
                }
            }
        });
        statusPanel.add(configCheckbox);
        statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        pane.add(statusPanel);
        pane.add(configScrollPane);
        configScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        mcast = new JPanel(new FlowLayout(FlowLayout.LEFT));
        mcast.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        mcast.add(new JLabel("# "));
        mcast.add(typeW);
        mcast.add(ifaceW);
        mcast.add(addrW);
        mcast.add(portW);
        mcast.add(addButton);
        mcast.setPreferredSize(mcast.getMinimumSize());
        mcast.setAlignmentX(Component.LEFT_ALIGNMENT);
//        makeConfigButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        pane.add(makeConfigButton);
        return pane;
    }

    /** Enable skip button. */
    @Override
    protected boolean skipButtonEnabled() {
        return true;
    }
}
