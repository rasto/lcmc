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


package drbd.gui.dialog;

import drbd.utilities.MyButton;
import drbd.utilities.Heartbeat;
import drbd.data.Host;
import drbd.data.Cluster;
import drbd.data.CastAddress;
import drbd.data.resources.NetInterface;
import drbd.data.resources.UcastLink;
import drbd.utilities.Tools;
import drbd.utilities.SSH.ExecCommandThread;
import drbd.gui.SpringUtilities;
import drbd.gui.GuiComboBox;
import drbd.utilities.ExecCallback;
import drbd.gui.ProgressBar;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;
import java.util.ArrayList;
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
 * An implementation of a dialog where heartbeat is initialized on all hosts.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class ClusterHbConfig extends DialogCluster {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
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
        new MyButton(Tools.getString("Dialog.ClusterHbConfig.CreateHbConfig"));
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
    private final JPanel configPanel = new JPanel(new SpringLayout());
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
    private static final int REMOVE_BUTTON_WIDTH  = 80;
    /** Height of the remove button. */
    private static final int REMOVE_BUTTON_HEIGHT = 14;
    /** Checkbox text (Edit the config). */
    private static final String EDIT_CONFIG_STRING = Tools.getString(
                                 "Dialog.ClusterHbConfig.Checkbox.EditConfig");
    /** Checkbox text (See existing). */
    private static final String SEE_EXISTING_STRING = Tools.getString(
                                 "Dialog.ClusterHbConfig.Checkbox.SeeExisting");
    /** /etc/ha.d/ha.cf read error string. */
    private static final String HA_CF_ERROR_STRING = "error: read error";
    /** Newline. */
    private static final String NEWLINE = "\\r?\\n";
    /** Config scroll pane. */
    private volatile JScrollPane configScrollPane = null;

    /**
     * Prepares a new <code>ClusterHbConfig</code> object.
     */
    public ClusterHbConfig(final WizardDialog previousDialog,
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
                                StringBuffer config = hbConfigHead(false);
                                config.append('\n');
                                config.append(hbConfigAddr());
                                config.append(hbConfigDopd(
                                                    dopdCB.isSelected()));
                                config.append(hbConfigMgmtd(
                                                    mgmtdCB.isSelected()));

                                Heartbeat.createHBConfig(hosts, config);
                                boolean configOk = updateOldHbConfig();
                                Heartbeat.reloadHeartbeats(hosts);
                                enableComponents();
                                if (configOk) {
                                    hideRetryButton();
                                    nextButtonSetEnabled(true);
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
        return new ClusterInit(this, getCluster());
    }

    /**
     * Returns title of this dialog.
     */
    protected final String getClusterDialogTitle() {
        return Tools.getString("Dialog.ClusterHbConfig.Title");
    }

    /**
     * Returns description of this dialog.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.ClusterHbConfig.Description");
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
                    boolean configOk = updateOldHbConfig();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            makeConfigButton.setEnabled(false);
                        }
                    });
                    enableComponents();
                    if (configOk) {
                        nextButtonSetEnabled(true);
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
        /* config[0] ## generated by drbd-gui */
        final Pattern p = Pattern.compile("## generated by drbd-gui.*");
        final Matcher m = p.matcher(config[0]);
        if (m.matches()) {
            final Pattern bcastP  = Pattern.compile("(bcast) (\\w+)");
            final Pattern mcastP  = Pattern.compile("(mcast) (\\w+) (.*)");
            final Pattern serialP = Pattern.compile("(serial) (.*)");
            final Pattern ucastP  = Pattern.compile("(ucast) (\\w+) (.*)");
            castAddresses.clear();
            for (String line : config) {
                final Matcher bcastM  = bcastP.matcher(line);
                final Matcher mcastM  = mcastP.matcher(line);
                final Matcher ucastM  = ucastP.matcher(line);
                final Matcher serialM = serialP.matcher(line);
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
                } else {
                    continue;
                }
                if (!"".equals(type)) {
                    castAddresses.add(new CastAddress(type,
                                                      iface,
                                                      addr,
                                                      serial));
                }
            }
        }
        //updateConfigPanelEditable(false);
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
        configStatus.setText(Tools.getString("Dialog.ClusterHbConfig.Loading"));
        int i = 0;

        for (Host h : hosts) {
            final int index = i;
            ts[i] = h.execCommand("Heartbeat.getHbConfig",
                             (ProgressBar) null,
                             new ExecCallback() {
                                 public void done(final String ans) {
                                     configs[index] = ans;
                                 }
                                 public void doneError(final String ans,
                                                       final int exitCode) {
                                     configs[index] = HA_CF_ERROR_STRING;
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

        if (configs[0].equals(HA_CF_ERROR_STRING)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    configStatus.setText(hosts[0] + ": " + Tools.getString(
                                      "Dialog.ClusterHbConfig.NoConfigFound"));
                }
            });
            retry();
        } else {
            noConfigs = false;
            int j;
            for (j = 1; j < configs.length; j++) {
                final Host host = hosts[j];
                if (configs[j].equals(HA_CF_ERROR_STRING)) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            configStatus.setText(host + ": "
                                                 + Tools.getString(
                                      "Dialog.ClusterHbConfig.NoConfigFound"));
                        }
                    });
                    break;
                } else if (!configs[0].equals(configs[j])) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            configStatus.setText(Tools.getString(
                                  "Dialog.ClusterHbConfig.ConfigsNotTheSame"));
                        }
                    });
                    break;
                }
            }
            if (j < configs.length) {
                retry();
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        configStatus.setText(
                            Tools.getString("Dialog.ClusterHbConfig.ha.cf.ok"));
                        configCheckbox.setText(SEE_EXISTING_STRING);
                        configCheckbox.setSelected(false);
                        statusPanel.setMaximumSize(
                                    statusPanel.getPreferredSize());
                    }
                });
                setNewConfig(configs[0]);
                updateConfigPanelEditable(false);
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
                setNewConfig(configs[0]);
                updateConfigPanelEditable(false);
            } else {
                updateConfigPanelExisting();
            }
        }
        return configOk;
    }

    /**
     * Shows all ha.cf config files.
     */
    private void updateConfigPanelExisting() {
        final Host[] hosts = getCluster().getHostsArray();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                makeConfigButton.setEnabled(false);
                configPanel.removeAll();
                int cols = 0;
                for (int i = 0; i < hosts.length; i++) {
                    if (HA_CF_ERROR_STRING.equals(configs[i])) {
                        configs[i] =
                            Tools.getString(
                                    "Dialog.ClusterHbConfig.NoConfigFound");
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
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (!configChanged) {
                    makeConfigButton.setEnabled(false);
                }
                configPanel.removeAll();
                int rows = 0;
                /* head */
                final String[] head =
                                hbConfigHead(true).toString().split(NEWLINE);
                for (String line : head) {
                    configPanel.add(new JLabel(line));
                    configPanel.add(new JLabel(" "));
                    rows++;
                }
                configPanel.add(new JLabel(""));
                configPanel.add(new JLabel(" "));
                rows++;
                if (castAddresses.size() < 2) {
                    JLabel l;
                    if (castAddresses.size() < 1) {
                        l = new JLabel(Tools.getString(
                                "Dialog.ClusterHbConfig.WarningAtLeastTwoInt"));
                    } else {
                        l = new JLabel(Tools.getString(
                        "Dialog.ClusterHbConfig.WarningAtLeastTwoInt.OneMore"));
                    }
                    l.setForeground(Color.RED);
                    configPanel.add(l);
                    configPanel.add(new JLabel(""));
                    rows++;
                    final JLabel label = l;
                    l.addFocusListener(new FocusListener() {
                        public void focusGained(final FocusEvent e) {
                            if (configScrollPane != null) {
                                configScrollPane.getViewport().setViewPosition(
                                                  label.getBounds().getLocation());
                                label.removeFocusListener(this); /* only once */
                            }
                        }
                        public void focusLost(final FocusEvent e) {
                            /* nothing */
                        }
                    });
                    l.requestFocus();
                }
                /* addresses */
                for (final CastAddress c : castAddresses) {
                    configPanel.add(new JLabel(c.getConfigString()));
                    configPanel.add(getRemoveButton(c));
                    rows++;
                }
                configPanel.add(new JLabel(""));
                configPanel.add(new JLabel(" "));
                rows++;
                /* mcast etc combo boxes */
                configPanel.add(mcast);
                configPanel.add(new JLabel(""));
                rows++;
                /* dopd */
                final String[] dopdLines =
                        hbConfigDopd(
                           dopdCB.isSelected()).toString().split(NEWLINE);
                boolean checkboxDone = false;
                for (String line : dopdLines) {
                    configPanel.add(new JLabel(line));
                    if (checkboxDone) {
                        configPanel.add(new JLabel(" "));
                    } else {
                        configPanel.add(dopdCB);
                        checkboxDone = true;
                    }
                    rows++;
                }

                /* mgmtd */
                final String[] mgmtdLines =
                        hbConfigMgmtd(
                           mgmtdCB.isSelected()).toString().split(NEWLINE);
                checkboxDone = false;
                for (String line : mgmtdLines) {
                    configPanel.add(new JLabel(line));
                    if (checkboxDone) {
                        configPanel.add(new JLabel(" "));
                    } else {
                        configPanel.add(mgmtdCB);
                        checkboxDone = true;
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
                    makeConfigButton.setEnabled(!castAddresses.isEmpty());
                }
            }
        });
    }

    /**
     * Returns remove address button.
     */
    private MyButton getRemoveButton(final CastAddress c) {
        final MyButton removeButton = new MyButton(
                    Tools.getString("Dialog.ClusterHbConfig.RemoveIntButton"));
        removeButton.setMaximumSize(new Dimension(REMOVE_BUTTON_WIDTH,
                                                  REMOVE_BUTTON_HEIGHT));
        removeButton.setPreferredSize(new Dimension(REMOVE_BUTTON_WIDTH,
                                                    REMOVE_BUTTON_HEIGHT));
        removeButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final Thread t = new Thread(new Runnable() {
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
            if (c.equals(type, iface, addr, serial)) {
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
     * Returns the head of the hb config.
     */
    private StringBuffer hbConfigHead(final boolean fake) {
        final StringBuffer config = new StringBuffer(130);
        if (fake) {
            config.append("## to be generated by drbd-gui ");
        } else {
            config.append("## generated by drbd-gui ");
        }
        config.append(Tools.getRelease());
        config.append("\n\ncrm yes\nnode");
        final Host[] hosts = getCluster().getHostsArray();
        for (Host host : hosts) {
            config.append(' ');
            config.append(host.getHostname());
        }
        config.append("\nlogfacility local0\n");
        return config;
    }

    /**
     * Returns the part of the hb config with addresses.
     */
    private StringBuffer hbConfigAddr() {
        final StringBuffer config = new StringBuffer(80);
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
    private StringBuffer hbConfigDopd(final boolean useDopd) {
        final StringBuffer config = new StringBuffer(120);
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
    private StringBuffer hbConfigMgmtd(final boolean useMgmt) {
        final StringBuffer config = new StringBuffer(120);
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

    /**
     * Adds interface to the config panel. It must be called from a thread.
     */
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

    /**
     * Returns panel where user can edit the config.
     */
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        final Host[] hosts = getCluster().getHostsArray();
        final String[] types = {MCAST_TYPE,
                                BCAST_TYPE,
                                UCAST_TYPE,
                                SERIAL_TYPE};

        typeCB = new GuiComboBox(MCAST_TYPE,
                                 types,
                                 null,
                                 null,
                                 TYPE_COMBOBOX_WIDTH);

        final NetInterface[] ni = hosts[0].getNetInterfaces();
        ifaceCB = new GuiComboBox(null, ni, null, null, INTF_COMBOBOX_WIDTH);

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

        ucastLink1CB = new GuiComboBox(null,
                                       ucastLinks,
                                       null,
                                       null,
                                       LINK_COMBOBOX_WIDTH);
        ucastLink2CB = new GuiComboBox(null,
                                       ucastLinks,
                                       null,
                                       null,
                                       LINK_COMBOBOX_WIDTH);

        /* serial links */
        final String[] serialDevs = {"/dev/ttyS0",
                                     "/dev/ttyS1",
                                     "/dev/ttyS2",
                                     "/dev/ttyS3"};

        serialCB = new GuiComboBox(null,
                                   serialDevs,
                                   null,
                                   null,
                                   LINK_COMBOBOX_WIDTH);

        /* this matches something like this: 225.0.0.43 694 1 0
         * if you think that the regexp is too complicated for that, consider,
         * that it must match also during the thing is written.
         */
        final String regexp = "^\\d{1,3}(\\.\\d{0,3}(\\d\\.\\d{0,3}"
                              + "(\\d\\.\\d{0,3})( \\d{0,3}(\\d \\d{0,3}"
                              + "(\\d \\d{0,3})?)?)?)?)?$";
        addrCB = new GuiComboBox("239.192.0.0 694 1 0",
                                 null, null, regexp, ADDR_COMBOBOX_WIDTH);

        final ItemListener typeL = new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    final String type = typeCB.getStringValue();
                    if (type != null) {
                        Thread thread = new Thread(new Runnable() {
                            public void run() {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
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

        final ItemListener serialL = new ItemListener() {
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
        serialCB.setVisible(false);

        serialCB.addListeners(serialL, null);

        final ItemListener ucastLinkL = new ItemListener() {
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
        ucastLink1CB.setVisible(false);
        ucastLink2CB.setVisible(false);

        ucastLink1CB.addListeners(ucastLinkL, null);
        ucastLink2CB.addListeners(ucastLinkL, null);

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
                       Tools.getString("Dialog.ClusterHbConfig.AddIntButton"));
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
        mcast.add(serialCB);
        mcast.add(ucastLink1CB);
        mcast.add(ucastLink2CB);
        mcast.add(addButton);
        mcast.setMaximumSize(mcast.getPreferredSize());
        /* dopd */
        dopdCB = new JCheckBox(
                    Tools.getString("Dialog.ClusterHbConfig.UseDopdCheckBox"),
                    null,
                    false);
        dopdCB.setToolTipText(
            Tools.getString("Dialog.ClusterHbConfig.UseDopdCheckBox.ToolTip"));
        dopdCB.addItemListener(
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

        /* mgmtd */
        mgmtdCB = new JCheckBox(
                    Tools.getString("Dialog.ClusterHbConfig.UseMgmtdCheckBox"),
                    null,
                    false);
        mgmtdCB.setToolTipText(
            Tools.getString("Dialog.ClusterHbConfig.UseMgmtdCheckBox.ToolTip"));
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
