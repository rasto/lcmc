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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import lcmc.Exceptions;
import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.CastAddress;
import lcmc.data.Cluster;
import lcmc.data.Host;
import lcmc.data.StringValue;
import lcmc.data.Value;
import lcmc.data.resources.NetInterface;
import lcmc.data.resources.UcastLink;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.widget.Check;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.Heartbeat;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.MyButton;
import lcmc.utilities.ssh.ExecCommandConfig;
import lcmc.utilities.ssh.Ssh;
import lcmc.utilities.ssh.ExecCommandThread;
import lcmc.utilities.Tools;
import lcmc.utilities.WidgetListener;

/**
 * An implementation of a dialog where heartbeat is initialized on all hosts.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class HbConfig extends DialogCluster {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(HbConfig.class);
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
    private static final Map<String, Widget.Type> OPTION_TYPES =
                                       new HashMap<String, Widget.Type>();
    /** Option regexps. */
    private static final Map<String, String> OPTION_REGEXPS =
                                                 new HashMap<String, String>();
    /** Default values. */
    private static final Map<String, Value> OPTION_DEFAULTS =
                                                 new HashMap<String, Value>();
    /** Option sizes. */
    private static final Map<String, Integer> OPTION_SIZES =
                                                new HashMap<String, Integer>();
    /** Multicast type string. */
    private static final Value MCAST_TYPE = new StringValue("mcast");
    /** Broadcast type string. */
    private static final Value BCAST_TYPE = new StringValue("bcast");
    /** Unicast type string. */
    private static final Value UCAST_TYPE = new StringValue("ucast");
    /** Serial type. */
    private static final Value SERIAL_TYPE = new StringValue("serial");
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
    static {
        //OPTION_TYPES.put(CRM, Widget.Type.COMBOBOX);
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
        OPTION_DEFAULTS.put(KEEPALIVE, new StringValue("2"));
        OPTION_DEFAULTS.put(WARNTIME, new StringValue("20"));
        OPTION_DEFAULTS.put(DEADTIME, new StringValue("30"));
        OPTION_DEFAULTS.put(INITDEAD, new StringValue("30"));
        OPTION_DEFAULTS.put(CRM, new StringValue("respawn"));
        OPTION_DEFAULTS.put(COMPRESSION, new StringValue("bz2"));
        OPTION_DEFAULTS.put(COMPRESSION_THRESHOLD, new StringValue("20"));
        OPTION_DEFAULTS.put(TRADITIONAL_COMPRESSION, new StringValue("on"));
        /* sizes */
        OPTION_SIZES.put(CRM, 100);
        OPTION_SIZES.put(COMPRESSION, 80);
        OPTION_SIZES.put(LOGFACILITY, 80);
        OPTION_SIZES.put(USE_LOGD, 50);
        OPTION_SIZES.put(AUTOJOIN, 80);
        OPTION_SIZES.put(NODE, 300);
    }
    private final Map<String, Value> optionDefaults =
                                   new HashMap<String, Value>(OPTION_DEFAULTS);
    /** Option values. */
    private final Map<String, Value[]> optionValues =
                                               new HashMap<String, Value[]>();
    /** Option checkboxes. */
    private final Map<String, Widget> optionsW =
                                            new HashMap<String, Widget>();

    /** Checkbox for dopd. */
    private JCheckBox dopdW  = null;
    /** Checkbox for mgmtd. */
    private JCheckBox mgmtdW  = null;
    /** Panel for mcast addresses. */
    private JPanel mcast;
    /** Set of ucast, bcast, mcast etc. addresses. */
    private final Collection<CastAddress> castAddresses =
                                        new LinkedHashSet<CastAddress>();
    /** Atatus of the config. For example does not exist. */
    private final JLabel configStatus = new JLabel("");
    /** Make config button. */
    private final MyButton makeConfigButton =
        new MyButton(Tools.getString("Dialog.Cluster.HbConfig.CreateHbConfig"));
    /** Connection type pulldown menu: ucast, bcast, mcast ... */
    private Widget typeW;
    /** Interface pulldown menu. */
    private Widget ifaceW;
    /** Serial device pulldown menu. */
    private Widget serialW;
    /** First ucast link. */
    private Widget ucastLink1W;
    /** Second ucast link. */
    private Widget ucastLink2W;
    /** Address field. */
    private Widget addrW;
    /** Add address button. */
    private MyButton addButton;
    /** Array with /etc/ha.d/ha.cf configs from all hosts. */
    private final String[] configs;
    /** Status panel. */
    private JPanel statusPanel;
    /** Check box that allows to edit a new config are see the existing
     * configs. */
    private JCheckBox configCheckbox;
    /** Editable hearbeat config panel. */
    private final JPanel configPanel = new JPanel();
    /** Whether the config was changed by the user. */
    private boolean configChanged = false;
    /** Config scroll pane. */
    private volatile JScrollPane configScrollPane = null;
    /** Whether the config pane was already moved to the position. */
    private volatile boolean alreadyMoved = false;
    /** When to start to check the fields. */
    private CountDownLatch fieldCheckLatch = new CountDownLatch(1);

    /** Prepares a new {@code HbConfig} object. */
    HbConfig(final WizardDialog previousDialog,
             final Cluster cluster) {
        super(previousDialog, cluster);
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
        optionValues.put(NODE, new Value[]{new StringValue(config.toString()),
                                           new StringValue()});
        optionDefaults.put(NODE, new StringValue(config.toString()));
        optionValues.put(CRM, new Value[]{new StringValue("respawn"),
                                          new StringValue("on"),
                                          new StringValue("off")});
        optionValues.put(COMPRESSION, new Value[]{new StringValue(),
                                                  new StringValue("zlib"),
                                                  new StringValue("bz2")});
        optionValues.put(TRADITIONAL_COMPRESSION,
                         new Value[]{new StringValue(),
                                     new StringValue("on"),
                                     new StringValue("off")});
        optionValues.put(LOGFACILITY, new Value[]{new StringValue("local0"),
                                                  new StringValue("local1"),
                                                  new StringValue("local2"),
                                                  new StringValue("local3"),
                                                  new StringValue("local4"),
                                                  new StringValue("local5"),
                                                  new StringValue("local6"),
                                                  new StringValue("local7"),
                                                  new StringValue("none")});
        optionValues.put(USE_LOGD, new Value[]{new StringValue(),
                                               new StringValue("on"),
                                               new StringValue("off")});
        optionValues.put(AUTOJOIN, new Value[]{new StringValue(),
                                               new StringValue("any"),
                                               new StringValue("other"),
                                               new StringValue("none")});
        configs = new String[hosts.length];
        makeConfigButton.setBackgroundColor(
                              Tools.getDefaultColor("ConfigDialog.Button"));
        makeConfigButton.setEnabled(false);
        makeConfigButton.addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                fieldCheckLatch = new CountDownLatch(1);
                                Tools.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
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
                                                    dopdW.isSelected()));
                                config.append(hbConfigMgmtd(
                                                    mgmtdW.isSelected()));

                                Heartbeat.createHBConfig(hosts, config);
                                final boolean configOk = updateOldHbConfig();
                                if (dopdW.isSelected()) {
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
                                         final Exceptions.IllegalVersionException e) {
                                            LOG.appWarning("run: "
                                                           + e.getMessage(), e);
                                        }
                                        Heartbeat.enableDopd(h, wa);
                                    }
                                }
                                Heartbeat.reloadHeartbeats(hosts);
                                enableComponents();
                                final List<String> incorrect =
                                                     new ArrayList<String>();
                                final List<String> changed =
                                                     new ArrayList<String>();
                                if (configOk) {
                                    hideRetryButton();
                                } else {
                                    incorrect.add("config failed");
                                }
                                nextButtonSetEnabled(new Check(incorrect, changed));

                                if (configOk) {
                                    if (!Tools.getApplication()
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
        return Tools.getString("Dialog.Cluster.HbConfig.Title");
    }

    /** Returns description of this dialog. */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Cluster.HbConfig.Description");
    }

    /** Returns localized string of Next button. */
    @Override
    public String nextButton() {
        return Tools.getString("Dialog.Cluster.HbConfig.NextButton");
    }

    /** Inits the dialog. */
    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.PAGE_AXIS));
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
                    final boolean configOk = updateOldHbConfig();
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            makeConfigButton.setEnabled(false);
                        }
                    });
                    enableComponents();
                    final List<String> incorrect = new ArrayList<String>();
                    final List<String> changed = new ArrayList<String>();
                    if (!configOk) {
                        incorrect.add("config failed");
                    }
                    nextButtonSetEnabled(new Check(incorrect, changed));
                    if (configOk) {
                        if (!Tools.getApplication().getAutoClusters()
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
                         Pattern.compile("^\\s*respawn hacluster .*/dopd$");
        final Pattern mgmtdP =
                          Pattern.compile("^\\s*respawn root .*/mgmtd -v$");
        castAddresses.clear();
        final Map<String, String> opValues = new HashMap<String, String>();
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
                dopdW.setSelected(true);
                continue;
            } else if (mgmtdM.matches()) {
                mgmtdW.setSelected(true);
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
                castAddresses.add(new CastAddress(type,
                                                  iface,
                                                  addr,
                                                  serial));
            }
        }
        for (final String option : OPTIONS) {
            if (opValues.containsKey(option)) {
                optionsW.get(option).setValue(new StringValue(opValues.get(option)));
            } else {
                optionsW.get(option).setValue(new StringValue());
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
        final ExecCommandThread[] ts = new ExecCommandThread[hosts.length];
        configStatus.setText(
                          Tools.getString("Dialog.Cluster.HbConfig.Loading"));
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
                                 public void doneError(final String answer,
                                                       final int errorCode) {
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
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    configStatus.setText(hosts[0] + ": " + Tools.getString(
                                     "Dialog.Cluster.HbConfig.NoConfigFound"));
                }
            });
            retry();
            if (!Tools.getApplication().getAutoClusters().isEmpty()) {
                Tools.sleep(1000);
                addButton.pressButton();
            }
        } else {
            noConfigs = false;
            int j;
            for (j = 1; j < configs.length; j++) {
                final Host host = hosts[j];
                if (configs[j].equals(HA_CF_ERROR_STRING)) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            configStatus.setText(host + ": "
                                                 + Tools.getString(
                                      "Dialog.Cluster.HbConfig.NoConfigFound"));
                        }
                    });
                    break;
                } else if (!configs[0].equals(configs[j])) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
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
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
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
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                fieldCheckLatch.countDown();
            }
        });
        return configOk;
    }

    /** Shows all ha.cf config files. */
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
                    if (HA_CF_ERROR_STRING.equals(configs[i])) {
                        configs[i] =
                            Tools.getString(
                                    "Dialog.Cluster.HbConfig.NoConfigFound");
                    }
                    final JLabel l = new JLabel(hosts[i].getName() + ':');
                    l.setBackground(Color.WHITE);
                    final JPanel labelP = new JPanel();
                    labelP.setBackground(
                            Tools.getDefaultColor("ConfigDialog.Background"));
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
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!configChanged) {
                    makeConfigButton.setEnabled(false);
                }
                configPanel.removeAll();
                /* head */
                final String[] head =
                                hbConfigHead(true).toString().split(NEWLINE);
                for (final String line : head) {
                    configPanel.add(new JLabel(line));
                }
                /* timeouts */
                for (final String option : OPTIONS) {
                    configPanel.add(getComponentPanel(option,
                                                      optionsW.get(option).getComponent()));
                }
                configPanel.add(new JLabel(" "));
                if (castAddresses.size() < 2) {
                    final JLabel l;
                    if (castAddresses.size() < 1) {
                        l = new JLabel(Tools.getString(
                               "Dialog.Cluster.HbConfig.WarningAtLeastTwoInt"));
                    } else {
                        l = new JLabel(Tools.getString(
                       "Dialog.Cluster.HbConfig.WarningAtLeastTwoInt.OneMore"));
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
                            if (alreadyMoved) {
                                return;
                            }
                            alreadyMoved = true;
                            configScrollPane.getViewport().setViewPosition(
                                    l.getBounds().getLocation());
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
                    configPanel.add(getComponentPanel(c.getConfigString(),
                                                      getRemoveButton(c)));
                }
                configPanel.add(new JLabel(" "));
                /* mcast etc combo boxes */
                configPanel.add(mcast);
                /* dopd */
                final String[] dopdLines =
                        hbConfigDopd(
                           dopdW.isSelected()).toString().split(NEWLINE);
                boolean checkboxDone = false;
                for (final String line : dopdLines) {
                    if (checkboxDone) {
                        configPanel.add(new JLabel(line));
                    } else {
                        configPanel.add(getComponentPanel(line, dopdW));
                        checkboxDone = true;
                    }
                }

                /* mgmtd */
                final String[] mgmtdLines =
                        hbConfigMgmtd(
                           mgmtdW.isSelected()).toString().split(NEWLINE);
                checkboxDone = false;
                for (final String line : mgmtdLines) {
                    if (checkboxDone) {
                        configPanel.add(new JLabel(line));
                    } else {
                        configPanel.add(getComponentPanel(line, mgmtdW));
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
                                            Application.AccessType.ADMIN);
                    }
                    if (!Tools.getApplication().getAutoClusters().isEmpty()
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
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
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
        final Value type = typeW.getValue();
        String addr       = "";
        String iface      = "";
        String serial     = "";

        if (BCAST_TYPE.equals(type)) {
            iface = ifaceW.getStringValue();
        } else if (MCAST_TYPE.equals(type)) {
            iface = ifaceW.getStringValue();
            addr = addrW.getStringValue();
        } else if (SERIAL_TYPE.equals(type)) {
            serial = serialW.getStringValue();
        } else if (UCAST_TYPE.equals(type)) {
            final UcastLink ucastLink1 = (UcastLink) ucastLink1W.getValue();
            final UcastLink ucastLink2 = (UcastLink) ucastLink2W.getValue();
            if (ucastLink1 == null
                || ucastLink2 == null
                || ucastLink1.getHost() == ucastLink2.getHost()) {
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        addButton.setEnabled(false);
                    }
                });
                return;
            }
            iface = ucastLink1.getInterface();
            addr = ucastLink2.getIp();
        }

        for (final CastAddress c : castAddresses) {
            if (c.equals(type.getValueForConfig(), iface, addr, serial)) {
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

    /** Returns the head of the hb config. */
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
            final String value = optionsW.get(option).getStringValue();
            if (value != null && !value.isEmpty()) {
                config.append(option);
                config.append(' ');
                config.append(optionsW.get(option).getStringValue());
                config.append('\n');
            }
        }
        return config;
    }

    /** Returns the part of the hb config with addresses. */
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
    private CharSequence hbConfigMgmtd(final boolean useMgmt) {
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
    private void addInterface(final Value type) {
        String iface      = "";
        String addr       = "";
        String serial     = "";
        if (MCAST_TYPE.equals(type)) {
            iface  = ifaceW.getStringValue();
            addr = addrW.getStringValue();
        } else if (BCAST_TYPE.equals(type)) {
            iface  = ifaceW.getStringValue();
        } else if (UCAST_TYPE.equals(type)) {
            iface = ((UcastLink) ucastLink1W.getValue()).getInterface();
            addr = ((UcastLink) ucastLink2W.getValue()).getIp();
        } else if (SERIAL_TYPE.equals(type)) {
            serial = serialW.getStringValue();
        }
        castAddresses.add(new CastAddress(type.getValueForConfig(), iface, addr, serial));
        updateConfigPanelEditable(true);
        checkInterface();
    }

    /** Returns panel where user can edit the config. */
    @Override
    protected JComponent getInputPane() {
        optionsW.clear();
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        final Host[] hosts = getCluster().getHostsArray();
        final Value[] types = {MCAST_TYPE,
                               BCAST_TYPE,
                               UCAST_TYPE,
                               SERIAL_TYPE};

        typeW = WidgetFactory.createInstance(
                                      Widget.GUESS_TYPE,
                                      MCAST_TYPE,
                                      types,
                                      Widget.NO_REGEXP,
                                      TYPE_COMBOBOX_WIDTH,
                                      Widget.NO_ABBRV,
                                      new AccessMode(Application.AccessType.RO,
                                                     !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);

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
                                      new AccessMode(Application.AccessType.RO,
                                                     !AccessMode.ADVANCED),
                                      Widget.NO_BUTTON);

        /* ucast links */
        final List<UcastLink> ulList = new ArrayList<UcastLink>();
        for (final Host host : hosts) {
            final NetInterface[] netInterfaces = host.getNetInterfaces();
            for (final NetInterface n : netInterfaces) {
                ulList.add(new UcastLink(host, n));
            }
        }
        final UcastLink[] ucastLinks =
                                ulList.toArray(new UcastLink[ulList.size()]);

        ucastLink1W = WidgetFactory.createInstance(
                                     Widget.GUESS_TYPE,
                                     Widget.NO_DEFAULT,
                                     ucastLinks,
                                     Widget.NO_REGEXP,
                                     LINK_COMBOBOX_WIDTH,
                                     Widget.NO_ABBRV,
                                     new AccessMode(Application.AccessType.RO,
                                                    !AccessMode.ADVANCED),
                                     Widget.NO_BUTTON);
        ucastLink2W = WidgetFactory.createInstance(
                                     Widget.GUESS_TYPE,
                                     Widget.NO_DEFAULT,
                                     ucastLinks,
                                     Widget.NO_REGEXP,
                                     LINK_COMBOBOX_WIDTH,
                                     Widget.NO_ABBRV,
                                     new AccessMode(Application.AccessType.RO,
                                                    !AccessMode.ADVANCED),
                                     Widget.NO_BUTTON);

        /* serial links */
        final Value[] serialDevs = {new StringValue("/dev/ttyS0"),
                                    new StringValue("/dev/ttyS1"),
                                    new StringValue("/dev/ttyS2"),
                                    new StringValue("/dev/ttyS3")};

        serialW = WidgetFactory.createInstance(
                                     Widget.GUESS_TYPE,
                                     Widget.NO_DEFAULT,
                                     serialDevs,
                                     Widget.NO_REGEXP,
                                     LINK_COMBOBOX_WIDTH,
                                     Widget.NO_ABBRV,
                                     new AccessMode(Application.AccessType.RO,
                                                    !AccessMode.ADVANCED),
                                     Widget.NO_BUTTON);

        /* this matches something like this: 225.0.0.43 694 1 0
         * if you think that the regexp is too complicated for that, consider,
         * that it must match also during the thing is written.
         * TODO: it does not work very good anyway
         */
        final String regexp = "^\\d{1,3}(\\.\\d{0,3}(\\d\\.\\d{0,3}"
                              + "(\\d\\.\\d{0,3})( \\d{0,3}(\\d \\d{0,3}"
                              + "(\\d \\d{0,3})?)?)?)?)?$";
        addrW = WidgetFactory.createInstance(
                                     Widget.GUESS_TYPE,
                                     new StringValue("239.192.0.0 694 1 0"),
                                     Widget.NO_ITEMS,
                                     regexp,
                                     ADDR_COMBOBOX_WIDTH,
                                     Widget.NO_ABBRV,
                                     new AccessMode(Application.AccessType.RO,
                                                    !AccessMode.ADVANCED),
                                     Widget.NO_BUTTON);

        typeW.addListeners(
            new WidgetListener() {
                @Override
                public void check(final Value value) {
                    final Value type = typeW.getValue();
                    if (type != null) {
                        if (MCAST_TYPE.equals(type)
                            || BCAST_TYPE.equals(type)) {
                            ifaceW.setVisible(true);
                        } else {
                            ifaceW.setVisible(false);
                        }

                        if (MCAST_TYPE.equals(type)) {
                            addrW.setVisible(true);
                        } else {
                            addrW.setVisible(false);
                        }
                        if (SERIAL_TYPE.equals(type)) {
                            serialW.setVisible(true);
                        } else {
                            serialW.setVisible(false);
                        }
                        if (UCAST_TYPE.equals(type)) {
                            ucastLink1W.setVisible(true);
                            ucastLink2W.setVisible(true);
                        } else {
                            ucastLink1W.setVisible(false);
                            ucastLink2W.setVisible(false);
                        }
                        Tools.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                mcast.setMaximumSize(
                                            mcast.getPreferredSize());
                            }
                        });
                        checkInterface();
                    }
                }
            });

        ifaceW.addListeners(new WidgetListener() {
                                @Override
                                public void check(final Value value) {
                                    checkInterface();
                                }
                            });

        serialW.setVisible(false);

        serialW.addListeners(new WidgetListener() {
                                 @Override
                                 public void check(final Value value) {
                                     checkInterface();
                                 }
                             });

        ucastLink1W.setVisible(false);
        ucastLink2W.setVisible(false);

        ucastLink1W.addListeners(new WidgetListener() {
                                     @Override
                                     public void check(final Value value) {
                                         checkInterface();
                                     }
                                 });
        ucastLink2W.addListeners(new WidgetListener() {
                                     @Override
                                     public void check(final Value value) {
                                         checkInterface();
                                     }
                                 });
        addrW.addListeners(new WidgetListener() {
                               @Override
                               public void check(final Value value) {
                                   checkInterface();
                               }
                           });

        addButton = new MyButton(
                      Tools.getString("Dialog.Cluster.HbConfig.AddIntButton"));
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
                                         Application.AccessType.ADMIN);
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
        mcast = new JPanel(new FlowLayout(FlowLayout.LEADING));
        mcast.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        mcast.add(new JLabel("# "));
        mcast.add(typeW.getComponent());
        mcast.add(ifaceW.getComponent());
        mcast.add(addrW.getComponent());
        mcast.add(serialW.getComponent());
        mcast.add(ucastLink1W.getComponent());
        mcast.add(ucastLink2W.getComponent());
        mcast.add(addButton);
        mcast.setMaximumSize(mcast.getPreferredSize());
        mcast.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (final String option : OPTIONS) {
            final int size;
            if (OPTION_SIZES.containsKey(option)) {
                size = OPTION_SIZES.get(option);
            } else {
                size = 40;
            }
            final Widget w = WidgetFactory.createInstance(
                                    OPTION_TYPES.get(option),
                                    optionDefaults.get(option),
                                    optionValues.get(option),
                    '^' + OPTION_REGEXPS.get(option) + "\\s*$",
                                    size,
                                    Widget.NO_ABBRV,
                                    new AccessMode(Application.AccessType.ADMIN,
                                                   !AccessMode.ADVANCED),
                                    Widget.NO_BUTTON);
            optionsW.put(option, w);
            w.setAlwaysEditable(true);
            w.addListeners(getOptionListener());
        }

        /* dopd */
        dopdW = new JCheckBox(
                    Tools.getString("Dialog.Cluster.HbConfig.UseDopdCheckBox"),
                    null,
                    false);
        dopdW.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        dopdW.setToolTipText(
            Tools.getString("Dialog.Cluster.HbConfig.UseDopdCheckBox.ToolTip"));
        dopdW.addItemListener(
            new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            updateConfigPanelEditable(true);
                        }
                    });
                    thread.start();
                }
            });

        /* mgmtd */
        mgmtdW = new JCheckBox(
                    Tools.getString("Dialog.Cluster.HbConfig.UseMgmtdCheckBox"),
                    null,
                    false);
        mgmtdW.setBackground(Tools.getDefaultColor("ConfigDialog.Background"));
        mgmtdW.setToolTipText(
           Tools.getString("Dialog.Cluster.HbConfig.UseMgmtdCheckBox.ToolTip"));
        mgmtdW.addItemListener(
            new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override
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

    /** Enable skip button. */
    @Override
    protected boolean skipButtonEnabled() {
        return true;
    }

    /** Returns widget listener for textfields. */
    private WidgetListener getOptionListener() {
        return new WidgetListener() {
            @Override
            public void check(final Value value) {
                if (fieldCheckLatch.getCount() > 0) {
                    return;
                }
                for (final String option : OPTIONS) {
                    final Widget w = optionsW.get(option);
                    if (w != null) {
                        if (checkRegexp(w.getRegexp(),
                                        w.getStringValue())) {
                            w.setBackground(null, null, true);
                        } else {
                            w.wrongValue();
                        }
                    }
                }
                Tools.getGUIData().setAccessible(
                                        makeConfigButton,
                                        Application.AccessType.ADMIN);
            }
        };
    }

    /** Checks regexp. */
    private boolean checkRegexp(final String regexp,
                                final CharSequence value) {
        if (regexp != null) {
            final Pattern p = Pattern.compile(regexp);
            final Matcher m = p.matcher(value);
            return m.matches();
        }
        return true;
    }
}
