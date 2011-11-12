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


package lcmc.gui.dialog.cluster;

import lcmc.utilities.MyButton;
import lcmc.utilities.Heartbeat;
import lcmc.utilities.Tools;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.SSH.ExecCommandThread;
import lcmc.utilities.SSH;
import lcmc.data.Host;
import lcmc.data.Cluster;
import lcmc.data.ConfigData;
import lcmc.data.CastAddress;
import lcmc.data.resources.NetInterface;
import lcmc.data.resources.UcastLink;
import lcmc.data.AccessMode;
import lcmc.gui.SpringUtilities;
import lcmc.gui.GuiComboBox;
import lcmc.gui.ProgressBar;
import lcmc.gui.dialog.WizardDialog;
import lcmc.Exceptions;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.CountDownLatch;

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
 * An implementation of a dialog where heartbeat is initialized on all hosts.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class HbConfig extends DialogCluster {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Keepalive option. */
    private static final String KEEPALIVE = "keepalive";
    /** Warntime option. */
    private static final String WARNTIME = "warntime";
    /** Deadtime option. */
    private static final String DEADTIME = "deadtime";
    /** Initdead option. */
    private static final String INITDEAD = "initdead";
    /** CRM option. */
    private static final String CRM = "crm";
    /** Compression option. */
    private static final String COMPRESSION = "compression";
    /** Compression threshold option. */
    private static final String COMPRESSION_THRESHOLD =
                                                    "compression_threshold";
    /** Traditional compression threshold option. */
    private static final String TRADITIONAL_COMPRESSION =
                                                    "traditional_compression";
    /** Logfacility option. */
    private static final String LOGFACILITY = "logfacility";
    /** use_logd option. */
    private static final String USE_LOGD = "use_logd";
    /** Autojoin option. */
    private static final String AUTOJOIN = "autojoin";
    /** Node option. */
    private static final String NODE = "node";
    /** All options. */
    private static final String[] OPTIONS = {KEEPALIVE,
                                             WARNTIME,
                                             DEADTIME,
                                             INITDEAD,
                                             CRM,
                                             COMPRESSION,
                                             COMPRESSION_THRESHOLD,
                                             TRADITIONAL_COMPRESSION,
                                             LOGFACILITY,
                                             USE_LOGD,
                                             AUTOJOIN,
                                             NODE};
    /** Option types. */
    private static final Map<String, GuiComboBox.Type> OPTION_TYPES =
                                       new HashMap<String, GuiComboBox.Type>();
    /** Option regexps. */
    private static final Map<String, String> OPTION_REGEXPS =
                                                 new HashMap<String, String>();
    /** Default values. */
    private static final Map<String, String> OPTION_DEFAULTS =
                                                 new HashMap<String, String>();
    /** Option sizes. */
    private static final Map<String, Integer> OPTION_SIZES =
                                                new HashMap<String, Integer>();
    static {
        //OPTION_TYPES.put(CRM, GuiComboBox.Type.COMBOBOX);
        OPTION_REGEXPS.put(KEEPALIVE, "\\d*");
        OPTION_REGEXPS.put(WARNTIME, "\\d*");
        OPTION_REGEXPS.put(DEADTIME, "\\d*");
        OPTION_REGEXPS.put(INITDEAD, "\\d*");
        OPTION_REGEXPS.put(CRM, "\\w*");

        OPTION_REGEXPS.put(COMPRESSION, "\\w*");
        OPTION_REGEXPS.put(COMPRESSION_THRESHOLD, "\\d*");
        OPTION_REGEXPS.put(TRADITIONAL_COMPRESSION, "\\w*");
        OPTION_REGEXPS.put(LOGFACILITY, "\\w*");
        OPTION_REGEXPS.put(USE_LOGD, "\\w*");
        OPTION_REGEXPS.put(AUTOJOIN, "\\w*");
        OPTION_REGEXPS.put(NODE, ".*?");
        /* defaults */
        OPTION_DEFAULTS.put(KEEPALIVE, "2");
        OPTION_DEFAULTS.put(WARNTIME, "20");
        OPTION_DEFAULTS.put(DEADTIME, "30");
        OPTION_DEFAULTS.put(INITDEAD, "30");
        OPTION_DEFAULTS.put(CRM, "respawn");
        OPTION_DEFAULTS.put(COMPRESSION, "bz2");
        OPTION_DEFAULTS.put(COMPRESSION_THRESHOLD, "20");
        OPTION_DEFAULTS.put(TRADITIONAL_COMPRESSION, "on");
        /* sizes */
        OPTION_SIZES.put(CRM, 100);
        OPTION_SIZES.put(COMPRESSION, 80);
        OPTION_SIZES.put(LOGFACILITY, 80);
        OPTION_SIZES.put(USE_LOGD, 50);
        OPTION_SIZES.put(AUTOJOIN, 80);
        OPTION_SIZES.put(NODE, 300);
    }
    /** Option values. */
    private final Map<String, String[]> optionValues =
                                               new HashMap<String, String[]>();
    /** Option checkboxes. */
    private final Map<String, GuiComboBox> optionsCB =
                                            new HashMap<String, GuiComboBox>();

    /** Checkbox for dopd. */
    private JCheckBox dopdCB  = null;
    /** Checkbox for mgmtd. */
    private JCheckBox mgmtdCB  = null;
    /** Panel for mcast addresses. */
    private JPanel mcast;
    /** Set of ucast, bcast, mcast etc. addresses. */
    private final Set<CastAddress> castAddresses =
                                        new LinkedHashSet<CastAddress>();
    /** Atatus of the config. For example does not exist. */
    private final JLabel configStatus = new JLabel("");
    /** Make config button. */
    private final MyButton makeConfigButton =
        new MyButton(Tools.getString("Dialog.Cluster.HbConfig.CreateHbConfig"));
    /** Connection type pulldown menu: ucast, bcast, mcast ... */
    private GuiComboBox typeCB;
    /** Interface pulldown menu. */
    private GuiComboBox ifaceCB;
    /** Serial device pulldown menu. */
    private GuiComboBox serialCB;
    /** First ucast link. */
    private GuiComboBox ucastLink1CB;
    /** Second ucast link. */
    private GuiComboBox ucastLink2CB;
    /** Address field. */
    private GuiComboBox addrCB;
    /** Add address button. */
    private MyButton addButton;
    /** Array with /etc/ha.d/ha.cf configs from all hosts. */
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
    private static final String MCAST_TYPE = "mcast";
    /** Broadcast type string. */
    private static final String BCAST_TYPE = "bcast";
    /** Unicast type string. */
    private static final String UCAST_TYPE = "ucast";
    /** Serial type. */
    private static final String SERIAL_TYPE = "serial";
    /** Width of the address combobox. */
    private static final int ADDR_COMBOBOX_WIDTH = 160;
    /** Width of the link combobox. */
    private static final int LINK_COMBOBOX_WIDTH = 130;
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
                               "Dialog.Cluster.HbConfig.Checkbox.EditConfig");
    /** Checkbox text (See existing). */
    private static final String SEE_EXISTING_STRING = Tools.getString(
                               "Dialog.Cluster.HbConfig.Checkbox.SeeExisting");
    /** /etc/ha.d/ha.cf read error string. */
    private static final String HA_CF_ERROR_STRING = "error: read error";
    /** Newline. */
    private static final String NEWLINE = "\\r?\\n";
    /** Config scroll pane. */
    private volatile JScrollPane configScrollPane = null;
    /** Whether the config pane was already moved to the position. */
    private volatile boolean alreadyMoved = false;
    /** When to start to check the fields. */
    private CountDownLatch fieldCheckLatch = new CountDownLatch(1);

    /** Prepares a new <code>HbConfig</code> object. */
    HbConfig(final WizardDialog previousDialog,
             final Cluster cluster) {
        super(previousDialog, cluster);
        final Host[] hosts = getCluster().getHostsArray();
        final StringBuilder config = new StringBuilder();
        boolean first = true;
        for (Host host : hosts) {
            if (!first) {
                config.append(' ');
            }
            first = false;
            config.append(host.getHostname());
        }
        /* choices */
        optionValues.put(NODE, new String[]{config.toString(), ""});
        optionValues.put(CRM, new String[]{"respawn", "on", "off"});
        optionValues.put(COMPRESSION, new String[]{"", "zlib", "bz2"});
        optionValues.put(TRADITIONAL_COMPRESSION,
                         new String[]{"", "on", "off"});
        optionValues.put(LOGFACILITY, new String[]{"local0",
                                                   "local1",
                                                   "local2",
                                                   "local3",
                                                   "local4",
                                                   "local5",
                                                   "local6",
                                                   "local7",
                                                   "none"});
        optionValues.put(USE_LOGD, new String[]{"", "on", "off"});
        optionValues.put(AUTOJOIN, new String[]{"", "any", "other", "none"});
        configs = new String[hosts.length];
        makeConfigButton.setBackgroundColor(
                              Tools.getDefaultColor("ConfigDialog.Button"));
        makeConfigButton.setEnabled(false);
        makeConfigButton.addActionListener(
            new ActionListener() {
                @Override public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(
                        new Runnable() {
                            @Override public void run() {
                                fieldCheckLatch = new CountDownLatch(1);
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override public void run() {
                                        makeConfigButton.setEnabled(false);
                                    }
                                });
                                disableComponents();
                                final StringBuilder config =
                                                          hbConfigHead(false);
                                config.append(hbConfigOptions());
                                config.append('\n');
                                config.append(hbConfigAddr());
                                config.append(hbConfigDopd(
                                                    dopdCB.isSelected()));
                                config.append(hbConfigMgmtd(
                                                    mgmtdCB.isSelected()));

                                Heartbeat.createHBConfig(hosts, config);
                                boolean configOk = updateOldHbConfig();
                                if (dopdCB.isSelected()) {
                                    for (final Host h : hosts) {
                                        final String hbV =
                                                    h.getHeartbeatVersion();
                                        boolean wa = false;
                                        try {
                                            if (hbV != null
                                                && Tools.compareVersions(
                                                            hbV,
                                                            "3.0.2") <= 0) {
                                                wa = true;
                                            }
                                        } catch (
                                         Exceptions.IllegalVersionException e) {
                                            Tools.appWarning(e.getMessage(), e);
                                        }
                                        Heartbeat.enableDopd(h, wa);
                                    }
                                }
                                Heartbeat.reloadHeartbeats(hosts);
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
    @Override public WizardDialog nextDialog() {
        return new Init(this, getCluster());
    }

    /** Returns title of this dialog. */
    @Override protected String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.HbConfig.Title");
    }

    /** Returns description of this dialog. */
    @Override protected String getDescription() {
        return Tools.getString("Dialog.Cluster.HbConfig.Description");
    }

    /** Returns localized string of Next button. */
    @Override public String nextButton() {
        return Tools.getString("Dialog.Cluster.HbConfig.NextButton");
    }

    /** Inits the dialog. */
    @Override protected void initDialog() {
        super.initDialog();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background"));
        enableComponentsLater(new JComponent[]{buttonClass(nextButton())});
    }

    /** Inits the dialog after it becomes visible. */
    @Override protected void initDialogAfterVisible() {
        final Thread thread = new Thread(
            new Runnable() {
                @Override public void run() {
                    boolean configOk = updateOldHbConfig();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
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
        final Pattern bcastP     = Pattern.compile("(bcast) (\\w+)");
        final Pattern mcastP     = Pattern.compile("(mcast) (\\w+) (.*)");
        final Pattern serialP    = Pattern.compile("(serial) (.*)");
        final Pattern ucastP     = Pattern.compile("(ucast) (\\w+) (.*)");
        final Map<String, Pattern> optionPatterns =
                                               new HashMap<String, Pattern>();
        for (final String option : OPTIONS) {
            optionPatterns.put(option,
                               Pattern.compile("^\\s*"
                                               + option
                                               + "\\s+("
                                               + OPTION_REGEXPS.get(option)
                                               + ")\\s*$"));
        }
        final Pattern dopdP =
                         Pattern.compile("^\\s*respawn hacluster .*\\/dopd$");
        final Pattern mgmtdP =
                          Pattern.compile("^\\s*respawn root .*\\/mgmtd -v$");
        castAddresses.clear();
        final Map<String, String> optionValues = new HashMap<String, String>();
        for (String line : config) {
            final Matcher bcastM  = bcastP.matcher(line);
            final Matcher mcastM  = mcastP.matcher(line);
            final Matcher ucastM  = ucastP.matcher(line);
            final Matcher serialM = serialP.matcher(line);
            final Matcher dopdM = dopdP.matcher(line);
            final Matcher mgmtdM = mgmtdP.matcher(line);
            String type       = typeCB.getStringValue();
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
                dopdCB.setSelected(true);
                continue;
            } else if (mgmtdM.matches()) {
                mgmtdCB.setSelected(true);
                continue;
            } else {
                for (final String option : OPTIONS) {
                    final Matcher m = optionPatterns.get(option).matcher(line);
                    if (m.matches()) {
                        optionValues.put(option, m.group(1).trim());
                        continue;
                    }
                }
                continue;
            }
            if (!"".equals(type)) {
                castAddresses.add(new CastAddress(type,
                                                  iface,
                                                  addr,
                                                  serial));
            }
        }
        for (final String option : OPTIONS) {
            if (optionValues.containsKey(option)) {
                optionsCB.get(option).setValue(optionValues.get(option));
            } else {
                optionsCB.get(option).setValue("");
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
        boolean configOk = false;
        boolean noConfigs = true;
        ExecCommandThread[] ts = new ExecCommandThread[hosts.length];
        configStatus.setText(
                          Tools.getString("Dialog.Cluster.HbConfig.Loading"));
        int i = 0;

        for (Host h : hosts) {
            final int index = i;
            ts[i] = h.execCommand("Heartbeat.getHbConfig",
                             (ProgressBar) null,
                             new ExecCallback() {
                                 @Override public void done(final String ans) {
                                     configs[index] = ans;
                                 }
                                 @Override public void doneError(
                                                         final String ans,
                                                         final int exitCode) {
                                     configs[index] = HA_CF_ERROR_STRING;
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

        if (configs[0].equals(HA_CF_ERROR_STRING)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    configStatus.setText(hosts[0] + ": " + Tools.getString(
                                     "Dialog.Cluster.HbConfig.NoConfigFound"));
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
                if (configs[j].equals(HA_CF_ERROR_STRING)) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            configStatus.setText(host + ": "
                                                 + Tools.getString(
                                      "Dialog.Cluster.HbConfig.NoConfigFound"));
                        }
                    });
                    break;
                } else if (!configs[0].equals(configs[j])) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            configStatus.setText(Tools.getString(
                                  "Dialog.Cluster.HbConfig.ConfigsNotTheSame"));
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
                    @Override public void run() {
                        configStatus.setText(
                           Tools.getString("Dialog.Cluster.HbConfig.ha.cf.ok"));
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
                @Override public void run() {
                    if (noConfigsF) {
                        configCheckbox.setText(SEE_EXISTING_STRING);
                        configCheckbox.setSelected(false);
                        statusPanel.setMaximumSize(
                                    statusPanel.getPreferredSize());
                    } else {
                        configCheckbox.setText(EDIT_CONFIG_STRING);
                        configCheckbox.setSelected(false);
                        statusPanel.setMaximumSize(
                                    statusPanel.getPreferredSize());
                    }
                }
            });
            if (noConfigs) {
                updateConfigPanelEditable(false);
            } else {
                updateConfigPanelExisting();
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                fieldCheckLatch.countDown();
            }
        });
        return configOk;
    }

    /** Shows all ha.cf config files. */
    private void updateConfigPanelExisting() {
        final Host[] hosts = getCluster().getHostsArray();
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                makeConfigButton.setEnabled(false);
                configPanel.removeAll();
                final JPanel insideConfigPanel = new JPanel(
                                                        new SpringLayout());
                int cols = 0;
                for (int i = 0; i < hosts.length; i++) {
                    if (HA_CF_ERROR_STRING.equals(configs[i])) {
                        configs[i] =
                            Tools.getString(
                                    "Dialog.Cluster.HbConfig.NoConfigFound");
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
        if (configChanged && fieldCheckLatch.getCount() > 0) {
            return;
        }
        this.configChanged = configChanged;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (!configChanged) {
                    makeConfigButton.setEnabled(false);
                }
                configPanel.removeAll();
                /* head */
                final String[] head =
                                hbConfigHead(true).toString().split(NEWLINE);
                for (String line : head) {
                    configPanel.add(new JLabel(line));
                }
                /* timeouts */
                for (final String option : OPTIONS) {
                    configPanel.add(getComponentPanel(option,
                                                      optionsCB.get(option)));
                }
                configPanel.add(new JLabel(" "));
                if (castAddresses.size() < 2) {
                    JLabel l;
                    if (castAddresses.size() < 1) {
                        l = new JLabel(Tools.getString(
                               "Dialog.Cluster.HbConfig.WarningAtLeastTwoInt"));
                    } else {
                        l = new JLabel(Tools.getString(
                       "Dialog.Cluster.HbConfig.WarningAtLeastTwoInt.OneMore"));
                    }
                    l.setForeground(Color.RED);
                    configPanel.add(l);
                    final JLabel label = l;
                    label.addComponentListener(new ComponentListener() {
                        @Override public void componentHidden(
                                                    final ComponentEvent e) {
                            /* do nothing */
                        }

                        @Override public void componentMoved(
                                                      final ComponentEvent e) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override public void run() {
                                    if (alreadyMoved) {
                                        return;
                                    }
                                    alreadyMoved = true;
                                    configScrollPane.getViewport()
                                                    .setViewPosition(
                                            label.getBounds().getLocation());
                                }
                            });
                        }

                        @Override public void componentResized(
                                                      final ComponentEvent e) {
                            /* do nothing */
                        }

                        @Override public void componentShown(
                                                      final ComponentEvent e) {
                            /* do nothing */
                        }
                    });
                }
                /* addresses */
                for (final CastAddress c : castAddresses) {
                    configPanel.add(getComponentPanel(c.getConfigString(),
                                                      getRemoveButton(c)));
                }
                configPanel.add(new JLabel(" "));
                /* mcast etc combo boxes */
                configPanel.add(mcast);
                /* dopd */
                final String[] dopdLines =
                        hbConfigDopd(
                           dopdCB.isSelected()).toString().split(NEWLINE);
                boolean checkboxDone = false;
                for (String line : dopdLines) {
                    if (checkboxDone) {
                        configPanel.add(new JLabel(line));
                    } else {
                        configPanel.add(getComponentPanel(line, dopdCB));
                        checkboxDone = true;
                    }
                }

                /* mgmtd */
                final String[] mgmtdLines =
                        hbConfigMgmtd(
                           mgmtdCB.isSelected()).toString().split(NEWLINE);
                checkboxDone = false;
                for (String line : mgmtdLines) {
                    if (checkboxDone) {
                        configPanel.add(new JLabel(line));
                    } else {
                        configPanel.add(getComponentPanel(line, mgmtdCB));
                        checkboxDone = true;
                    }
                }
                configPanel.revalidate();
                configPanel.repaint();
                if (configChanged) {
                    if (castAddresses.isEmpty()) {
                        makeConfigButton.setEnabled(false);
                    } else {
                        Tools.getGUIData().setAccessible(
                                            makeConfigButton,
                                            ConfigData.AccessType.ADMIN);
                    }
                    if (!Tools.getConfigData().getAutoClusters().isEmpty()
                        && !castAddresses.isEmpty()) {
                        Tools.sleep(1000);
                        makeConfigButton.pressButton();
                    }
                }
            }
        });
    }

    /** Returns remove address button. */
    private MyButton getRemoveButton(final CastAddress c) {
        final MyButton removeButton = new MyButton(
                   Tools.getString("Dialog.Cluster.HbConfig.RemoveIntButton"));
        removeButton.setBackgroundColor(
                                Tools.getDefaultColor("ConfigDialog.Button"));
        removeButton.setMaximumSize(new Dimension(REMOVE_BUTTON_WIDTH,
                                                  REMOVE_BUTTON_HEIGHT));
        removeButton.setPreferredSize(new Dimension(REMOVE_BUTTON_WIDTH,
                                                    REMOVE_BUTTON_HEIGHT));
        removeButton.addActionListener(
            new ActionListener() {
                @Override public void actionPerformed(final ActionEvent e) {
                    final Thread t = new Thread(new Runnable() {
                        @Override public void run() {
                            castAddresses.remove(c);
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
        final String type = typeCB.getStringValue();
        String addr       = "";
        String iface      = "";
        String serial     = "";
        UcastLink ucastLink1 = null;
        UcastLink ucastLink2 = null;

        if (BCAST_TYPE.equals(type)) {
            iface = ifaceCB.getStringValue();
        } else if (MCAST_TYPE.equals(type)) {
            iface = ifaceCB.getStringValue();
            addr = addrCB.getStringValue();
        } else if (SERIAL_TYPE.equals(type)) {
            serial = serialCB.getStringValue();
        } else if (UCAST_TYPE.equals(type)) {
            ucastLink1 = (UcastLink) ucastLink1CB.getValue();
            ucastLink2 = (UcastLink) ucastLink2CB.getValue();
            if (ucastLink1.getHost() == ucastLink2.getHost()) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        addButton.setEnabled(false);
                    }
                });
                return;
            }
            iface = ucastLink1.getInterface();
            addr = ucastLink2.getIp();
        }

        for (final CastAddress c : castAddresses) {
            if (c.equals(type, iface, addr, serial)) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        addButton.setEnabled(false);
                    }
                });
                return;
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                addButton.setEnabled(true);
            }
        });
    }

    /** Returns the head of the hb config. */
    private StringBuilder hbConfigHead(final boolean fake) {
        final StringBuilder config = new StringBuilder(130);
        if (fake) {
            config.append("## to be generated by drbd-gui ");
        } else {
            config.append("## generated by drbd-gui ");
        }
        return config;
    }

    /** Returns timeouts. */
    private StringBuilder hbConfigOptions() {
        final StringBuilder config = new StringBuilder(130);
        config.append(Tools.getRelease());
        config.append("\n\n");
        for (final String option : OPTIONS) {
            final String value = optionsCB.get(option).getStringValue();
            if (value != null && !"".equals(value)) {
                config.append(option);
                config.append(' ');
                config.append(optionsCB.get(option).getStringValue());
                config.append('\n');
            }
        }
        return config;
    }

    /** Returns the part of the hb config with addresses. */
    private StringBuilder hbConfigAddr() {
        final StringBuilder config = new StringBuilder(80);
        for (CastAddress ca : castAddresses) {
            config.append(ca.getConfigString());
            config.append('\n');
        }
        return config;
    }

    /**
     * Returns the part of the config that turns on dopd. To turn it off, the
     * dopd config is commented out.
     */
    private StringBuilder hbConfigDopd(final boolean useDopd) {
        final StringBuilder config = new StringBuilder(120);
        if (!useDopd) {
            config.append("# ");
        }
        config.append("respawn hacluster ");
        final Host[] hosts = getCluster().getHostsArray();
        config.append(hosts[0].getHeartbeatLibPath());
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
    private StringBuilder hbConfigMgmtd(final boolean useMgmt) {
        final StringBuilder config = new StringBuilder(120);
        if (!useMgmt) {
            config.append("# ");
        }
        config.append("respawn root ");
        final Host[] hosts = getCluster().getHostsArray();
        config.append(hosts[0].getHeartbeatLibPath());
        config.append("/mgmtd -v\n");
        if (!useMgmt) {
            config.append("# ");
        }
        config.append("apiauth mgmtd uid=root\n");
        return config;
    }

    /** Adds interface to the config panel. It must be called from a thread. */
    private void addInterface(final String type) {
        String iface      = "";
        String addr       = "";
        String serial     = "";
        if (MCAST_TYPE.equals(type)) {
            iface  = ifaceCB.getStringValue();
            addr = addrCB.getStringValue();
        } else if (BCAST_TYPE.equals(type)) {
            iface  = ifaceCB.getStringValue();
        } else if (UCAST_TYPE.equals(type)) {
            iface = ((UcastLink) ucastLink1CB.getValue()).getInterface();
            addr = ((UcastLink) ucastLink2CB.getValue()).getIp();
        } else if (SERIAL_TYPE.equals(type)) {
            serial = serialCB.getStringValue();
        }
        castAddresses.add(new CastAddress(type, iface, addr, serial));
        updateConfigPanelEditable(true);
        checkInterface();
    }

    /** Returns panel where user can edit the config. */
    @Override protected JComponent getInputPane() {
        optionsCB.clear();
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        final Host[] hosts = getCluster().getHostsArray();
        final String[] types = {MCAST_TYPE,
                                BCAST_TYPE,
                                UCAST_TYPE,
                                SERIAL_TYPE};

        typeCB = new GuiComboBox(MCAST_TYPE,
                                 types,
                                 null, /* units */
                                 null, /* type */
                                 null, /* regexp */
                                 TYPE_COMBOBOX_WIDTH,
                                 null, /* abbrv */
                                 new AccessMode(ConfigData.AccessType.RO,
                                                false)); /* only adv. mode */

        final NetInterface[] ni = hosts[0].getNetInterfaces();
        ifaceCB = new GuiComboBox(null, /* selected value */
                                  ni,
                                  null, /* units */
                                  null, /* type */
                                  null, /* regexp */
                                  INTF_COMBOBOX_WIDTH,
                                  null, /* abbrv */
                                  new AccessMode(ConfigData.AccessType.RO,
                                                 false)); /* only adv. mode */

        /* ucast links */
        final List<UcastLink> ulList = new ArrayList<UcastLink>();
        for (Host host : hosts) {
            final NetInterface[] netInterfaces = host.getNetInterfaces();
            for (NetInterface n : netInterfaces) {
                ulList.add(new UcastLink(host, n));
            }
        }
        final UcastLink[] ucastLinks =
                                ulList.toArray(new UcastLink[ulList.size()]);

        ucastLink1CB = new GuiComboBox(null, /* selected value */
                                       ucastLinks,
                                       null, /* units */
                                       null, /* type */
                                       null, /* regexp */
                                       LINK_COMBOBOX_WIDTH,
                                       null, /* abbrv */
                                       new AccessMode(ConfigData.AccessType.RO,
                                                      false)); /* only adv. */
        ucastLink2CB = new GuiComboBox(null, /* selected value */
                                       ucastLinks,
                                       null, /* units */
                                       null, /* type */
                                       null, /* regexp */
                                       LINK_COMBOBOX_WIDTH,
                                       null, /* abbrv */
                                       new AccessMode(ConfigData.AccessType.RO,
                                                      false)); /* only adv. */

        /* serial links */
        final String[] serialDevs = {"/dev/ttyS0",
                                     "/dev/ttyS1",
                                     "/dev/ttyS2",
                                     "/dev/ttyS3"};

        serialCB = new GuiComboBox(null, /* selected value */
                                   serialDevs,
                                   null, /* units */
                                   null, /* type */
                                   null, /* regexp */
                                   LINK_COMBOBOX_WIDTH,
                                   null, /* abbrv */
                                   new AccessMode(ConfigData.AccessType.RO,
                                                  false)); /* only adv. mode */

        /* this matches something like this: 225.0.0.43 694 1 0
         * if you think that the regexp is too complicated for that, consider,
         * that it must match also during the thing is written.
         * TODO: it does not work very good anyway
         */
        final String regexp = "^\\d{1,3}(\\.\\d{0,3}(\\d\\.\\d{0,3}"
                              + "(\\d\\.\\d{0,3})( \\d{0,3}(\\d \\d{0,3}"
                              + "(\\d \\d{0,3})?)?)?)?)?$";
        addrCB = new GuiComboBox("239.192.0.0 694 1 0",
                                 null, /* items */
                                 null, /* units */
                                 null, /* type */
                                 regexp,
                                 ADDR_COMBOBOX_WIDTH,
                                 null, /* abbrv */
                                 new AccessMode(ConfigData.AccessType.RO,
                                                false)); /* only adv. mode */

        final ItemListener typeL = new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final String type = typeCB.getStringValue();
                    if (type != null) {
                        Thread thread = new Thread(new Runnable() {
                            @Override public void run() {
                                if (MCAST_TYPE.equals(type)
                                    || BCAST_TYPE.equals(type)) {
                                    ifaceCB.setVisible(true);
                                } else {
                                    ifaceCB.setVisible(false);
                                }

                                if (MCAST_TYPE.equals(type)) {
                                    addrCB.setVisible(true);
                                } else {
                                    addrCB.setVisible(false);
                                }
                                if (SERIAL_TYPE.equals(type)) {
                                    serialCB.setVisible(true);
                                } else {
                                    serialCB.setVisible(false);
                                }
                                if (UCAST_TYPE.equals(type)) {
                                    ucastLink1CB.setVisible(true);
                                    ucastLink2CB.setVisible(true);
                                } else {
                                    ucastLink1CB.setVisible(false);
                                    ucastLink2CB.setVisible(false);
                                }
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override public void run() {
                                        mcast.setMaximumSize(
                                                    mcast.getPreferredSize());
                                    }
                                });
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
            @Override public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Thread thread = new Thread(new Runnable() {
                        @Override public void run() {
                            checkInterface();
                        }
                    });
                    thread.start();
                }
            }
        };

        ifaceCB.addListeners(ifaceL, null);

        final ItemListener serialL = new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Thread thread = new Thread(new Runnable() {
                        @Override public void run() {
                            checkInterface();
                        }
                    });
                    thread.start();
                }
            }
        };
        serialCB.setVisible(false);

        serialCB.addListeners(serialL, null);

        final ItemListener ucastLinkL = new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Thread thread = new Thread(new Runnable() {
                        @Override public void run() {
                            checkInterface();
                        }
                    });
                    thread.start();
                }
            }
        };
        ucastLink1CB.setVisible(false);
        ucastLink2CB.setVisible(false);

        ucastLink1CB.addListeners(ucastLinkL, null);
        ucastLink2CB.addListeners(ucastLinkL, null);

        final DocumentListener addrL = new DocumentListener() {
                        private void check() {
                            Thread thread = new Thread(new Runnable() {
                                @Override public void run() {
                                    checkInterface();
                                }
                            });
                            thread.start();
                        }

                        @Override public void insertUpdate(
                                                       final DocumentEvent e) {
                            check();
                        }

                        @Override public void removeUpdate(
                                                       final DocumentEvent e) {
                            check();
                        }

                        @Override public void changedUpdate(
                                                       final DocumentEvent e) {
                            check();
                        }
                    };

        addrCB.addListeners(null, addrL);

        addButton = new MyButton(
                      Tools.getString("Dialog.Cluster.HbConfig.AddIntButton"));
        addButton.setBackgroundColor(
                                Tools.getDefaultColor("ConfigDialog.Button"));
        addButton.addActionListener(
            new ActionListener() {
                @Override public void actionPerformed(final ActionEvent e) {
                    final String type = typeCB.getStringValue();
                    final Thread thread = new Thread(new Runnable() {
                        @Override public void run() {
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
        //configScrollPane.setMaximumSize(new Dimension(Short.MAX_VALUE,
        //                                              150));
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
            @Override public void itemStateChanged(final ItemEvent e) {
                final String text = configCheckbox.getText();
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override public void run() {
                            if (EDIT_CONFIG_STRING.equals(text)) {
                                updateConfigPanelEditable(configChanged);
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override public void run() {
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
                                    @Override public void run() {
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
        mcast.add(typeCB);
        mcast.add(ifaceCB);
        mcast.add(addrCB);
        mcast.add(serialCB);
        mcast.add(ucastLink1CB);
        mcast.add(ucastLink2CB);
        mcast.add(addButton);
        mcast.setMaximumSize(mcast.getPreferredSize());
        mcast.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (final String option : OPTIONS) {
            int size;
            if (OPTION_SIZES.containsKey(option)) {
                size = OPTION_SIZES.get(option);
            } else {
                size = 40;
            }
            final GuiComboBox cb = new GuiComboBox(
                                      OPTION_DEFAULTS.get(option),
                                      optionValues.get(option),
                                      null, /* units */
                                      OPTION_TYPES.get(option),
                                      "^" + OPTION_REGEXPS.get(option)
                                          + "\\s*$",
                                      size,
                                      null,
                                        new AccessMode(
                                               ConfigData.AccessType.ADMIN,
                                               false));
            optionsCB.put(option, cb);
            cb.setAlwaysEditable(true);
            cb.addListeners(null, getDocumentListener());
        }

        /* dopd */
        dopdCB = new JCheckBox(
                    Tools.getString("Dialog.Cluster.HbConfig.UseDopdCheckBox"),
                    null,
                    false);
        dopdCB.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        dopdCB.setToolTipText(
            Tools.getString("Dialog.Cluster.HbConfig.UseDopdCheckBox.ToolTip"));
        dopdCB.addItemListener(
            new ItemListener() {
                @Override public void itemStateChanged(final ItemEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override public void run() {
                            updateConfigPanelEditable(true);
                        }
                    });
                    thread.start();
                }
            });

        /* mgmtd */
        mgmtdCB = new JCheckBox(
                    Tools.getString("Dialog.Cluster.HbConfig.UseMgmtdCheckBox"),
                    null,
                    false);
        mgmtdCB.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        mgmtdCB.setToolTipText(
           Tools.getString("Dialog.Cluster.HbConfig.UseMgmtdCheckBox.ToolTip"));
        mgmtdCB.addItemListener(
            new ItemListener() {
                @Override public void itemStateChanged(final ItemEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override public void run() {
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

    /** Enable skip button. */
    @Override protected boolean skipButtonEnabled() {
        return true;
    }

    /** Returns document listener for textfields. */
    private DocumentListener getDocumentListener() {
        return new DocumentListener() {
            private void check() {
                if (fieldCheckLatch.getCount() > 0) {
                    return;
                }
                final Thread t = new Thread(new Runnable() {
                    @Override public void run() {
                        for (final String option : OPTIONS) {
                            final GuiComboBox cb = optionsCB.get(option);
                            if (cb != null) {
                                if (checkRegexp(cb.getRegexp(),
                                                cb.getStringValue())) {
                                    cb.setBackground(null, null, true);
                                } else {
                                    cb.wrongValue();
                                }
                            }
                        }
                        Tools.getGUIData().setAccessible(
                                                makeConfigButton,
                                                ConfigData.AccessType.ADMIN);
                    }
                });
                t.start();
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
        };
    }

    /** Checks regexp. */
    private boolean checkRegexp(final String regexp,
                                final String value) {
        if (regexp != null) {
            final Pattern p = Pattern.compile(regexp);
            final Matcher m = p.matcher(value);
            if (m.matches()) {
                return true;
            }
            return false;
        }
        return true;
    }
}
