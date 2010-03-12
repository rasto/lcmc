/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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


package drbd.gui.dialog.cluster;

import drbd.utilities.MyButton;
import drbd.utilities.Openais;
import drbd.utilities.Corosync;
import drbd.data.Host;
import drbd.data.ConfigData;
import drbd.data.Cluster;
import drbd.data.AisCastAddress;
import drbd.data.resources.NetInterface;
import drbd.utilities.Tools;
import drbd.utilities.SSH.ExecCommandThread;
import drbd.gui.SpringUtilities;
import drbd.gui.GuiComboBox;
import drbd.utilities.ExecCallback;
import drbd.gui.ProgressBar;
import drbd.gui.dialog.WizardDialog;

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
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.SwingUtilities;

import javax.swing.JComponent;
import java.awt.Component;

/**
 * An implementation of a dialog where corosync/openais is initialized on all
 * hosts.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class CoroConfig extends DialogCluster {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Checkbox for mgmtd. */
    private JCheckBox mgmtdCB  = null;
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
    private GuiComboBox typeCB;
    /** Interface pulldown menu. */
    private GuiComboBox ifaceCB;
    /** Address field. */
    private GuiComboBox addrCB;
    /** Port field. */
    private GuiComboBox portCB;
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
    private final JPanel configPanel = new JPanel(new SpringLayout());
    /** Whether the config was changed by the user. */
    private boolean configChanged = false;
    /** Multicast type string. */
    private static final String MCAST_TYPE = "mcast";
    /** Width of the address combobox. */
    private static final int ADDR_COMBOBOX_WIDTH = 100;
    /** Width of the port combobox. */
    private static final int PORT_COMBOBOX_WIDTH = 60;
    /** Width of the type combobox. */
    private static final int TYPE_COMBOBOX_WIDTH = 80;
    /** Width of the interface combobox. */
    private static final int INTF_COMBOBOX_WIDTH = 80;
    /** Width of the remove button. */
    private static final int REMOVE_BUTTON_WIDTH  = 80;
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

    /**
     * Prepares a new <code>CoroConfig</code> object.
     */
    public CoroConfig(final WizardDialog previousDialog,
                      final Cluster cluster) {
        super(previousDialog, cluster);
        final Host[] hosts = getCluster().getHostsArray();
        configs = new String[hosts.length];
        makeConfigButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(
                        new Runnable() {
                            public void run() {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        makeConfigButton.setEnabled(false);
                                    }
                                });
                                disableComponents();
                                StringBuffer config = aisConfigHead(false);
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
                                                    mgmtdCB.isSelected(),
                                                    serviceVersion));
                                if (hosts[0].isCorosync()) {
                                    Corosync.createCorosyncConfig(hosts,
                                                                  config);
                                } else {
                                    Openais.createAISConfig(hosts, config);
                                }
                                boolean configOk = updateOldAisConfig();
                                if (hosts[0].isCorosync()) {
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

    /**
     * Returns the successor of this dialog.
     */
    public final WizardDialog nextDialog() {
        return new Init(this, getCluster());
    }

    /**
     * Returns title of this dialog.
     */
    protected final String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.CoroConfig.Title");
    }

    /**
     * Returns description of this dialog.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.Cluster.CoroConfig.Description");
    }

    /**
     * Inits the dialog.
     */
    protected final void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
        final Thread thread = new Thread(
            new Runnable() {
                public void run() {
                    boolean configOk = updateOldAisConfig();
                    SwingUtilities.invokeLater(new Runnable() {
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
        final Pattern endParenthesesP = Pattern.compile("^\\s*}\\s*");
        final Pattern pattern =
                            Pattern.compile("\\s*(\\S+):\\s*(\\S+)\\s*");
        aisCastAddresses.clear();
        boolean inTotem = false;
        boolean inInterface = false;
        String mcastaddr = null;
        String mcastport = null;
        String bindnetaddr = null;
        for (String line : config) {
            final Matcher totemM = totemP.matcher(line);
            if (!inTotem && totemM.matches()) {
                inTotem = true;
            } else if (inTotem && !inInterface) {
                final Matcher interfaceM = interfaceP.matcher(line);
                if (interfaceM.matches()) {
                    inInterface = true;
                }
            } else if (inInterface) {
                final Matcher endParenthesesM =
                                            endParenthesesP.matcher(line);
                if (endParenthesesM.matches()) {
                    aisCastAddresses.add(new AisCastAddress(MCAST_TYPE,
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
                                 public void done(final String ans) {
                                     configs[index] = ans;
                                 }
                                 public void doneError(final String ans,
                                                       final int exitCode) {
                                     configs[index] = AIS_CONF_ERROR_STRING;
                                 }
                             },
                             null,   /* ConvertCmdCallback */
                             false); /* outputVisible */
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
            SwingUtilities.invokeLater(new Runnable() {
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
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            configStatus.setText(host + ": "
                                 + configFile
                                 + Tools.getString(
                                    "Dialog.Cluster.CoroConfig.NoConfigFound"));
                        }
                    });
                    break;
                } else if (!configs[0].equals(configs[j])) {
                    SwingUtilities.invokeLater(new Runnable() {
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
                final Pattern p = Pattern.compile("## generated by drbd-gui.*");
                final Matcher m = p.matcher(configs[0]);
                if (m.matches()) {
                    generated = true;
                }
                final boolean editableConfig = generated;
                SwingUtilities.invokeLater(new Runnable() {
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
            SwingUtilities.invokeLater(new Runnable() {
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
                setNewConfig(configs[0]);
                updateConfigPanelExisting();
            }
        }
        return configOk;
    }

    /**
     * Shows all corosync or openais.conf config files.
     */
    private void updateConfigPanelExisting() {
        final Host[] hosts = getCluster().getHostsArray();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                makeConfigButton.setEnabled(false);
                configPanel.removeAll();
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
                    labelP.setBackground(Color.WHITE);
                    labelP.setLayout(new BoxLayout(labelP, BoxLayout.Y_AXIS));
                    labelP.setAlignmentX(Component.TOP_ALIGNMENT);
                    labelP.add(l);
                    configPanel.add(labelP);
                    final JTextArea ta = new JTextArea(configs[i]);
                    ta.setEditable(false);
                    configPanel.add(ta);
                    cols += 2;
                }
                if (cols > 0) {
                    SpringUtilities.makeCompactGrid(configPanel,
                                                    1, cols,
                                                    1, 1,
                                                    1, 1);
                }
                configPanel.revalidate();
                configPanel.repaint();
            }
        });
    }

    /**
     * Updates the config panel.
     */
    private void updateConfigPanelEditable(final boolean configChanged) {
        this.configChanged = configChanged;
        final Host[] hosts = getCluster().getHostsArray();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (!configChanged) {
                    makeConfigButton.setEnabled(false);
                }
                configPanel.removeAll();
                int rows = 0;
                /* head */
                final String[] head =
                           aisConfigHead(true).toString().split(NEWLINE);
                for (String line : head) {
                    configPanel.add(new JLabel(line));
                    configPanel.add(new JLabel(" "));
                    rows++;
                }
                /* addresses */
                int ringnumber = 0;
                for (final AisCastAddress c : aisCastAddresses) {
                    configPanel.add(new JLabel(""));
                    configPanel.add(new JLabel(" "));
                    rows++;
                    final String[] interfaceLines =
                                c.getConfigString(ringnumber++,
                                                  SPACE_TAB).split(NEWLINE);
                    boolean firstLine = true;
                    for (final String interfaceLine : interfaceLines) {
                        configPanel.add(new JLabel(interfaceLine));
                        if (firstLine) {
                            configPanel.add(getRemoveButton(c));
                            firstLine = false;
                        } else {
                            configPanel.add(new JLabel(" "));
                        }
                        rows++;
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
                    rows++;
                    final JLabel label = l;
                    label.addComponentListener(new ComponentListener() {
                        public final void componentHidden(
                                                    final ComponentEvent e) {
                        }

                        public final void componentMoved(
                                                      final ComponentEvent e) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    configScrollPane.getViewport()
                                                    .setViewPosition(
                                            label.getBounds().getLocation());
                                }
                            });
                        }

                        public final void componentResized(
                                                      final ComponentEvent e) {
                        }

                        public final void componentShown(
                                                      final ComponentEvent e) {
                        }
                    });
                }

                configPanel.add(new JLabel(""));
                configPanel.add(new JLabel(" "));
                rows++;
                /* mcast etc combo boxes */
                configPanel.add(mcast);
                configPanel.add(new JLabel(""));
                rows++;
                configPanel.add(new JLabel("}"));
                configPanel.add(new JLabel(""));
                rows++;

                /* service pacemaker */
                final String serviceVersion =
                            hosts[0].getDistString(
                                    "Pacemaker.Service.Ver");
                final String[] pacemakerLines =
                        aisConfigPacemaker(SPACE_TAB,
                                           mgmtdCB.isSelected(),
                                           serviceVersion).toString()
                                                          .split(NEWLINE);
                final Pattern p = Pattern.compile("\\s*use_mgmtd\\s*:.*");
                for (String line : pacemakerLines) {
                    configPanel.add(new JLabel(line));
                    final Matcher m = p.matcher(line);
                    if (m.matches()) {
                        configPanel.add(mgmtdCB);
                    } else {
                        configPanel.add(new JLabel(" "));
                    }
                    rows++;
                }
                if (rows > 0) {
                    SpringUtilities.makeCompactGrid(configPanel,
                                                    rows, 2,
                                                    1, 1,
                                                    1, 1);
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

    /**
     * Returns remove address button.
     */
    private MyButton getRemoveButton(final AisCastAddress c) {
        final MyButton removeButton = new MyButton(
              Tools.getString("Dialog.Cluster.CoroConfig.RemoveIntButton"));
        removeButton.setMaximumSize(new Dimension(REMOVE_BUTTON_WIDTH,
                                                  REMOVE_BUTTON_HEIGHT));
        removeButton.setPreferredSize(new Dimension(REMOVE_BUTTON_WIDTH,
                                                    REMOVE_BUTTON_HEIGHT));
        removeButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final Thread t = new Thread(new Runnable() {
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
        final String type  = typeCB.getStringValue();
        String address     = "";
        String bindnetaddr = "";
        String port        = "";

        if (MCAST_TYPE.equals(type)) {
            final NetInterface iface = (NetInterface) ifaceCB.getValue();
            bindnetaddr = iface.getBindnetaddr();
            address = addrCB.getStringValue();
            port = portCB.getStringValue();
        }

        for (final AisCastAddress c : aisCastAddresses) {
            if (c.equals("\t", type, bindnetaddr, address, port)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        addButton.setEnabled(false);
                    }
                });
                return;
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                addButton.setEnabled(true);
            }
        });
    }

    /**
     * Returns the head of the corosync or openais config.
     */
    private StringBuffer aisConfigHead(final boolean fake) {
        final StringBuffer config = new StringBuffer(500);
        String tab;
        if (fake) {
            config.append("## to be generated by drbd-gui ");
            tab = SPACE_TAB;
        } else {
            config.append("## generated by drbd-gui ");
            tab = "\t";
        }
        config.append(Tools.getRelease());
        config.append("\n\naisexec {\n");
        config.append(tab);
        config.append("user: root\n");
        config.append(tab);
        config.append("group: root\n}\n\ncorosync {\n");
        config.append(tab);
        config.append("user: root\n");
        config.append(tab);
        config.append("group: root\n}\n\namf {\n");
        config.append(tab);
        config.append("mode: disabled\n}\n\nlogging {\n");
        config.append(tab);
        config.append("to_stderr: yes\n");
        config.append(tab);
        config.append("debug: off\n");
        config.append(tab);
        config.append("timestamp: on\n");
        config.append(tab);
        config.append("to_file: no\n");
        config.append(tab);
        config.append("to_syslog: yes\n");
        config.append(tab);
        config.append("syslog_facility: daemon\n}\n\ntotem {\n");
        config.append(tab);
        config.append("version: 2\n");
        config.append(tab);
        config.append("token: 3000\n");
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
        config.append("secauth: on\n");
        config.append(tab);
        config.append("threads: 0\n");
        config.append(tab);
        config.append("# nodeid: 1234\n");
        config.append(tab);
        config.append("rrp_mode: none\n");
        return config;
    }


    /**
     * Returns the part of the config that turns on mgmt. To turn it off, the
     * mgmt config is commented out.
     */
    private StringBuffer aisConfigPacemaker(final String tab,
                                            final boolean useMgmt,
                                            final String serviceVersion) {
        final StringBuffer config = new StringBuffer(120);
        config.append("\nservice {\n");
        config.append(tab);
        config.append("ver: ");
        config.append(serviceVersion);
        config.append('\n');
        config.append(tab);
        config.append("name: pacemaker\n");
        if (useMgmt) {
            config.append(tab);
            config.append("use_mgmtd: yes\n");
        } else {
            config.append(tab);
            config.append("use_mgmtd: no\n");
        }
        config.append("}\n");
        return config;
    }

    /**
     * Adds interface to the config panel. It must be called from a thread.
     */
    private void addInterface(final String type) {
        String bindnetaddr = "";
        String addr        = "";
        String port        = "";
        if (MCAST_TYPE.equals(type)) {
            final NetInterface iface  = (NetInterface) ifaceCB.getValue();
            bindnetaddr = iface.getBindnetaddr();
            addr = addrCB.getStringValue();
            port = portCB.getStringValue();
        }
        aisCastAddresses.add(
            new AisCastAddress(type,
                               bindnetaddr,
                               addr,
                               port));
        updateConfigPanelEditable(true);
        checkInterface();
    }

    /**
     * Returns panel where user can edit the config.
     */
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        final Host[] hosts = getCluster().getHostsArray();
        final String[] types = {MCAST_TYPE};

        typeCB = new GuiComboBox(MCAST_TYPE,
                                 types,
                                 null, /* units */
                                 null, /* type */
                                 null, /* regexp */
                                 TYPE_COMBOBOX_WIDTH,
                                 null, /* abbrv */
                                 ConfigData.AccessType.RO);
        typeCB.setEnabled(false);

        final NetInterface[] ni = hosts[0].getNetInterfaces();
        ifaceCB = new GuiComboBox(null, /* selected value */
                                  ni,
                                  null, /* units */
                                  null, /* type */
                                  null, /* regexp */
                                  INTF_COMBOBOX_WIDTH,
                                  null, /* abbrv */
                                  ConfigData.AccessType.RO);

        /* this matches something like this: 225.0.0.43 694 1 0
         * if you think that the regexp is too complicated for that, consider,
         * that it must match also during the thing is written.
         */
        final String regexp = "^[\\d.]+$";
        addrCB = new GuiComboBox(
              Tools.getDefault("Dialog.Cluster.CoroConfig.DefaultMCastAddress"),
              null, /* items */
              null, /* units */
              null, /* type */
              regexp,
              ADDR_COMBOBOX_WIDTH,
              null, /* abbrv */
              ConfigData.AccessType.RO);

        final ItemListener typeL = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final String type = typeCB.getStringValue();
                    if (type != null) {
                        Thread thread = new Thread(new Runnable() {
                            public void run() {
                                checkInterface();
                            }
                        });
                        thread.start();
                    }
                }
            }
        };

        typeCB.addListeners(typeL, null);

        final ItemListener ifaceL = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            checkInterface();
                        }
                    });
                    thread.start();
                }
            }
        };

        ifaceCB.addListeners(ifaceL, null);

        final ItemListener portL = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            checkInterface();
                        }
                    });
                    thread.start();
                }
            }
        };

        final String portRegexp = "^\\d+$";
        portCB = new GuiComboBox(
                Tools.getDefault("Dialog.Cluster.CoroConfig.DefaultMCastPort"),
                null, /* items */
                null, /* units */
                null, /* type */
                portRegexp,
                PORT_COMBOBOX_WIDTH,
                null, /* abbrv */
                ConfigData.AccessType.RO);
        portCB.addListeners(portL, null);

        final DocumentListener addrL = new DocumentListener() {
            private void check() {
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        checkInterface();
                    }
                });
                thread.start();
            }

            public void insertUpdate(final DocumentEvent e) {
                check();
            }

            public void removeUpdate(final DocumentEvent e) {
                check();
            }

            public void changedUpdate(final DocumentEvent e) {
                check();
            }
        };

        addrCB.addListeners(null, addrL);

        addButton = new MyButton(
                     Tools.getString("Dialog.Cluster.CoroConfig.AddIntButton"));
        addButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final String type = typeCB.getStringValue();
                    final Thread thread = new Thread(new Runnable() {
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
        statusPanel = new JPanel();
        statusPanel.add(configStatus);
        configCheckbox = new JCheckBox("-----", true);
        Tools.getGUIData().setAccessible(configCheckbox,
                                         ConfigData.AccessType.ADMIN);
        configCheckbox.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final String text = configCheckbox.getText();
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final Thread thread = new Thread(new Runnable() {
                        public void run() {
                            if (EDIT_CONFIG_STRING.equals(text)) {
                                updateConfigPanelEditable(configChanged);
                                SwingUtilities.invokeLater(new Runnable() {
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
                                SwingUtilities.invokeLater(new Runnable() {
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
        mcast.add(new JLabel("# "));
        mcast.add(typeCB);
        mcast.add(ifaceCB);
        mcast.add(addrCB);
        mcast.add(portCB);
        mcast.add(addButton);
        mcast.setMaximumSize(mcast.getPreferredSize());
        /* mgmtd */
        mgmtdCB = new JCheckBox(
                Tools.getString("Dialog.Cluster.CoroConfig.UseMgmtdCheckBox"),
                null,
                false);
        mgmtdCB.setToolTipText(Tools.getString(
                        "Dialog.Cluster.CoroConfig.UseMgmtdCheckBox.ToolTip"));
        mgmtdCB.addItemListener(
            new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        public void run() {
                            updateConfigPanelEditable(true);
                        }
                    });
                    thread.start();
                }
            });
        makeConfigButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        pane.add(makeConfigButton);
        return pane;
    }

    /**
     * Enable skip button.
     */
    protected final boolean skipButtonEnabled() {
        return true;
    }
}
