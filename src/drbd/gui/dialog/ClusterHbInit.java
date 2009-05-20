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
import java.awt.Dimension;
import java.awt.Color;
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
public class ClusterHbInit extends DialogCluster {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Checkbox for dopd. */
    private JCheckBox   dopdCB  = null;
    /** Set of ucast, bcast, mcast etc. addresses. */
    private final Set<CastAddress> castAddresses =
                                        new LinkedHashSet<CastAddress>();
    /** Atatus of the config. For example does not exist. */
    private final JLabel configStatus = new JLabel("");
    /** Make config button. */
    private final MyButton makeConfigButton =
           new MyButton(Tools.getString("Dialog.ClusterHbInit.CreateHbConfig"));
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
    /** Editable hearbeat config panel. */
    private final JPanel configPanel = new JPanel(new SpringLayout());

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

    /**
     * Prepares a new <code>ClusterHbInit</code> object.
     */
    public ClusterHbInit(final WizardDialog previousDialog,
                         final Cluster cluster) {
        super(previousDialog, cluster);
        final Host[] hosts = getCluster().getHostsArray();
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
                                //getProgressBar().hold();
                                //TODO: bug here, broswer is not created at
                                //this point.
                                StringBuffer config = hbConfigHead();
                                config.append('\n');
                                config.append(hbConfigAddr());
                                config.append(hbConfigDopd(
                                                    dopdCB.isSelected()));
                                config.append(hbConfigMgmt(false));

                                Heartbeat.createHBConfig(hosts, config);
                                updateOldHbConfig();
                                Heartbeat.reloadHeartbeats(hosts);
                                pressRetryButton();
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
        return Tools.getString("Dialog.ClusterHbInit.Title");
    }

    /**
     * Returns description of this dialog.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.ClusterHbInit.Description");
    }

    /**
     * Inits the dialog.
     */
    protected final void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{});

        final Thread thread = new Thread(
            new Runnable() {
                public void run() {
                    updateOldHbConfig();
                    //oldHbConfigTextArea.setEditable(false);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            makeConfigButton.setEnabled(false);
                        }
                    });
                }
            });
        thread.start();
    }

    /**
     * Parses the old config and sets the new one with old and new information.
     */
    private void setNewConfig(final String oldConfig) {
        final String[] config = oldConfig.split("\\r?\\n");
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
        updateConfigPanel();
    }

    /**
     * Checks whether the old config is the same on all hosts, if it exists at
     * all and enable the components accordingly.
     */
    private void updateOldHbConfig() { /* is run in a thread */
        final Host[] hosts = getCluster().getHostsArray();
        final String[] configs = new String[hosts.length];
        ExecCommandThread[] ts = new ExecCommandThread[hosts.length];
        configStatus.setText(Tools.getString("Dialog.ClusterHbInit.Loading"));
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
                                     configs[index] = "error";
                                 }
                             },
                             null,   /* ConvertCmdCallback */
                             false); /* outputVisible */
            i++;
        }
        for (ExecCommandThread t : ts) {
            // wait for all of them
            try {
                t.join();
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (configs[0].equals("error")) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    configStatus.setText(hosts[0] + ": " + Tools.getString(
                                        "Dialog.ClusterHbInit.NoConfigFound"));
                }
            });
            retry();
        } else {
            int j;
            for (j = 1; j < configs.length; j++) {
                final Host host = hosts[j];
                if (configs[j].equals("error")) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            configStatus.setText(host + ": "
                                                 + Tools.getString(
                                        "Dialog.ClusterHbInit.NoConfigFound"));
                        }
                    });
                    break;
                } else if (!configs[0].equals(configs[j])) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            configStatus.setText(Tools.getString(
                                    "Dialog.ClusterHbInit.ConfigsNotTheSame"));
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
                        configStatus.setText("/etc/ha.d/ha.cf");
                    }
                });
                setNewConfig(configs[0]);
                updateConfigPanel();
                nextButtonSetEnabled(true);
                enableComponents();
            }
        }
    }

    /**
     * Updates the config panel.
     */
    private void updateConfigPanel() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                configPanel.removeAll();
                int rows = 0;
                /* head */
                final String[] head =
                                hbConfigHead().toString().split("\\r?\\n");
                for (String line : head) {
                    configPanel.add(new JLabel(line));
                    configPanel.add(new JLabel(" "));
                    rows++;
                }
                configPanel.add(new JLabel(""));
                configPanel.add(new JLabel(" "));
                rows++;
                /* addresses */
                for (final CastAddress c : castAddresses) {
                    configPanel.add(new JLabel(c.getConfigString()));
                    configPanel.add(getRemoveButton(c));
                    rows++;
                }

                if (castAddresses.size() < 2) {
                    final JLabel l = new JLabel(
                        Tools.getString(
                            "Dialog.ClusterHbInit.WarningAtLeastTwoInt"));
                    l.setForeground(Color.RED);
                    configPanel.add(l);
                    configPanel.add(new JLabel(""));
                    rows++;
                }
                configPanel.add(new JLabel(""));
                configPanel.add(new JLabel(" "));
                rows++;
                /* dopd */
                final String[] dopdLines =
                        hbConfigDopd(
                           dopdCB.isSelected()).toString().split("\\r?\\n");
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
                if (rows > 0) {
                    SpringUtilities.makeCompactGrid(configPanel,
                                                    rows, 2,
                                                    1, 1,
                                                    1, 1);
                }
                configPanel.revalidate();
                configPanel.repaint();
            }
        });
    
    }

    /**
     * Returns remove address button.
     */
    private MyButton getRemoveButton(final CastAddress c) {
        final MyButton removeButton = new MyButton(
                    Tools.getString("Dialog.ClusterHbInit.RemoveIntButton"));
        removeButton.setMaximumSize(new Dimension(REMOVE_BUTTON_WIDTH,
                                                  REMOVE_BUTTON_HEIGHT));
        removeButton.setPreferredSize(new Dimension(REMOVE_BUTTON_WIDTH,
                                                    REMOVE_BUTTON_HEIGHT));
        removeButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            castAddresses.remove(c);
                            updateConfigPanel();
                            if (castAddresses.isEmpty()) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        makeConfigButton.setEnabled(false);
                                    }
                                });
                            } else {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        makeConfigButton.setEnabled(true);
                                    }
                                });
                            }
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
    private StringBuffer hbConfigHead() {
        final StringBuffer config = new StringBuffer(130);
        config.append("## generated by drbd-gui ");
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
     * TODO: for newer heartbeats it must be turned on.
     */
    private StringBuffer hbConfigMgmt(final boolean useMgmt) {
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
     * Returns panel where user can edit the config.
     */
    protected final JComponent getInputPane() {
        final JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.setBackground(Color.RED);
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
        addrCB = new GuiComboBox("230.0.0.71 694 1 1",
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

        addButton =
            new MyButton(Tools.getString("Dialog.ClusterHbInit.AddIntButton"));
        addButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final String type = typeCB.getStringValue();
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
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
                            castAddresses.add(new CastAddress(type,
                                                              iface,
                                                              addr,
                                                              serial));
                            updateConfigPanel();
                            checkInterface();
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    makeConfigButton.setEnabled(true);
                                }
                            });
                        }
                    });
                    thread.start();
                }
            });

        final JScrollPane sNew = new JScrollPane(
                                    configPanel,
                                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                                   );
        pane.add(configStatus);
        pane.add(sNew);

        final JPanel mcast = new JPanel();
        mcast.setLayout(new BoxLayout(mcast, BoxLayout.X_AXIS));
        mcast.setAlignmentX(Component.RIGHT_ALIGNMENT);
        mcast.add(typeCB);
        mcast.add(ifaceCB);
        mcast.add(addrCB);
        mcast.add(serialCB);
        mcast.add(ucastLink1CB);
        mcast.add(ucastLink2CB);
        mcast.add(addButton);
        mcast.setPreferredSize(mcast.getMinimumSize());
        pane.add(mcast);
        dopdCB = new JCheckBox(
                    Tools.getString("Dialog.ClusterHbInit.UseDopdCheckBox"),
                    null,
                    true);
        dopdCB.setToolTipText(
            Tools.getString("Dialog.ClusterHbInit.UseDopdCheckBox.ToolTip"));
        dopdCB.addItemListener(
            new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    // TODO: more logic here is required. E.g it should not
                    // enable the button if something is wrong
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    makeConfigButton.setEnabled(true);
                                }
                            });
                            updateConfigPanel();
                        }
                    });
                    thread.start();
                }
            });
        makeConfigButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        pane.add(makeConfigButton);
        return pane;
    }

    /**
     * Enable skip button.
     */
    protected boolean skipButtonEnabled() {
        return true;
    }
}
