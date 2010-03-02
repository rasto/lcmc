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

import drbd.EditHostDialog;
import drbd.gui.Browser;
import drbd.gui.HostBrowser;
import drbd.gui.ClusterBrowser;
import drbd.gui.HeartbeatGraph;
import drbd.gui.SpringUtilities;
import drbd.data.Host;
import drbd.data.Cluster;
import drbd.utilities.UpdatableItem;
import drbd.data.Subtext;
import drbd.data.ClusterStatus;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import drbd.utilities.ExecCallback;
import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;
import drbd.utilities.CRM;

import java.util.List;
import java.util.ArrayList;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Color;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import javax.swing.JScrollPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JColorChooser;

/**
 * This class holds info data for a host.
 * It shows host view, just like in the host tab.
 */
public class HostInfo extends Info {
    /** Host data. */
    private final Host host;
    /** Host standby icon. */
    private static final ImageIcon HOST_STANDBY_ICON =
     Tools.createImageIcon(Tools.getDefault("HeartbeatGraph.HostStandbyIcon"));
    /** Host standby off icon. */
    private static final ImageIcon HOST_STANDBY_OFF_ICON =
             Tools.createImageIcon(
                        Tools.getDefault("HeartbeatGraph.HostStandbyOffIcon"));
    /** Offline subtext. */
    private static final Subtext OFFLINE_SUBTEXT =
                                         new Subtext("offline", Color.BLUE);
    /** Online subtext. */
    private static final Subtext ONLINE_SUBTEXT =
                                          new Subtext("online", Color.BLUE);
    /** Standby subtext. */
    private static final Subtext STANDBY_SUBTEXT =
                                         new Subtext("STANDBY", Color.RED);
    /**
     * Prepares a new <code>HostInfo</code> object.
     */
    public HostInfo(final Host host, final Browser browser) {
        super(host.getName(), browser);
        this.host = host;
    }

    /**
     * Returns browser object of this info.
     */
    protected final HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    /**
     * Returns a host icon for the menu.
     */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        final Cluster cl = host.getCluster();
        if (cl != null) {
            return HostBrowser.HOST_IN_CLUSTER_ICON_RIGHT_SMALL;
        }
        return HostBrowser.HOST_ICON;
    }

    /**
     * Returns id, which is name of the host.
     */
    public final String getId() {
        return host.getName();
    }

    /**
     * Returns a host icon for the category in the menu.
     */
    public final ImageIcon getCategoryIcon() {
        return HostBrowser.HOST_ICON;
    }

    /**
     * Returns tooltip for the host.
     */
    public final String getToolTipForGraph(final boolean testOnly) {
        return getBrowser().getHostToolTip(host);
    }

    /**
     * Returns info panel.
     *
     * @return info panel
     */
    public final JComponent getInfoPanel() {
        Tools.getGUIData().setTerminalPanel(host.getTerminalPanel());
        final Font f = new Font("Monospaced", Font.PLAIN, 12);
        final JTextArea ta = new JTextArea();
        ta.setFont(f);

        final ExecCallback execCallback =
            new ExecCallback() {
                public void done(final String ans) {
                    ta.setText(ans);
                }

                public void doneError(final String ans, final int exitCode) {
                    ta.setText("error");
                    Tools.sshError(host, "", ans, exitCode);
                }

            };
        final MyButton crmMonButton = new MyButton("crm_mon");
        crmMonButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                host.execCommand("HostBrowser.getCrmMon",
                                 execCallback,
                                 null,  /* ConvertCmdCallback */
                                 true); /* outputVisible */
            }
        });
        host.registerEnableOnConnect(crmMonButton);
        final MyButton crmConfigureShowButton =
                                        new MyButton("crm configure show");
        crmConfigureShowButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                host.execCommand("HostBrowser.getCrmConfigureShow",
                                 execCallback,
                                 null,  /* ConvertCmdCallback */
                                 true); /* outputVisible */
            }
        });
        host.registerEnableOnConnect(crmConfigureShowButton);

        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(HostBrowser.STATUS_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        mainPanel.add(buttonPanel);

        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(HostBrowser.PANEL_BACKGROUND);
        final JMenu serviceCombo = getActionsMenu();
        mb.add(serviceCombo);
        buttonPanel.add(mb, BorderLayout.EAST);
        final JPanel p = new JPanel(new SpringLayout());
        p.setBackground(HostBrowser.STATUS_BACKGROUND);

        p.add(crmMonButton);
        p.add(crmConfigureShowButton);
        SpringUtilities.makeCompactGrid(p, 2, 1,  // rows, cols
                                           1, 1,  // initX, initY
                                           1, 1); // xPad, yPad
        mainPanel.setMinimumSize(new Dimension(
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")));
        mainPanel.setPreferredSize(new Dimension(
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")));
        buttonPanel.add(p);
        mainPanel.add(new JScrollPane(ta));
        //mainPanel.add(panel);
        return mainPanel;
    }

    /**
     * Gets host.
     *
     * @return host of this info
     */
    public final Host getHost() {
        return host;
    }

    /**
     * Compares this host info name with specified hostinfo's name.
     *
     * @param otherHI
     *              other host info
     * @return true if they are equal
     */
    public final boolean equals(final HostInfo otherHI) {
        if (otherHI == null) {
            return false;
        }
        return otherHI.toString().equals(host.getName());
    }

    /**
     * Returns string representation of the host. It's same as name.
     */
    public final String toString() {
        return host.getName();
    }

    /**
     * Returns name of the host.
     */
    public final String getName() {
        return host.getName();
    }

    /**
     * Creates the popup for the host.
     */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem>items = new ArrayList<UpdatableItem>();
        final boolean testOnly = false;
        /* host wizard */
        final MyMenuItem hostWizardItem =
            new MyMenuItem(Tools.getString("HostBrowser.HostWizard"),
                           HostBrowser.HOST_ICON_LARGE,
                           null) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return true;
                }

                public void action() {
                    final EditHostDialog dialog = new EditHostDialog(host);
                    dialog.showDialogs();
                }
            };
        items.add(hostWizardItem);
        registerMenuItem(hostWizardItem);
        Tools.getGUIData().registerAddHostButton(hostWizardItem);
        /* heartbeat standby on/off */
        final HostInfo thisClass = this;
        final MyMenuItem standbyItem =
            new MyMenuItem(Tools.getString("HostBrowser.CRM.StandByOn"),
                           HOST_STANDBY_ICON,
                           null,

                           Tools.getString("HostBrowser.CRM.StandByOff"),
                           HOST_STANDBY_OFF_ICON,
                           null) {
                private static final long serialVersionUID = 1L;

                public boolean predicate() {
                    return !isStandby(testOnly);
                }

                public boolean enablePredicate() {
                    return getHost().isClStatus();
                }

                public void action() {
                    if (isStandby(testOnly)) {
                        CRM.standByOff(host, testOnly);
                    } else {
                        CRM.standByOn(host, testOnly);
                    }
                }
            };
        final ClusterBrowser cb = getBrowser().getClusterBrowser();
        if (cb != null) {
            final ClusterBrowser.ClMenuItemCallback standbyItemCallback =
                              cb.new ClMenuItemCallback(standbyItem, host) {
                public void action(final Host host) {
                    if (isStandby(false)) {
                        CRM.standByOff(host, true);
                    } else {
                        CRM.standByOn(host, true);
                    }
                }
            };
            addMouseOverListener(standbyItem, standbyItemCallback);
        }
        items.add(standbyItem);
        registerMenuItem(standbyItem);
        /* change host color */
        final MyMenuItem changeHostColorItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ChangeHostColor"),
                           null,
                           null) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return true;
                }

                public void action() {
                    final JColorChooser tcc = new JColorChooser();
                    Color newColor = tcc.showDialog(
                                        Tools.getGUIData().getMainFrame(),
                                        "asdf",
                                        host.getPmColors()[0]);
                    if (newColor != null) {
                        host.setColor(newColor);
                    }
                }
            };
        items.add(changeHostColorItem);
        registerMenuItem(changeHostColorItem);

        /* view logs */
        final MyMenuItem viewLogsItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ViewLogs"),
                           null,
                           null) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return getHost().isConnected();
                }

                public void action() {
                    drbd.gui.dialog.HostLogs l =
                                           new drbd.gui.dialog.HostLogs(host);
                    l.showDialog();
                }
            };
        items.add(viewLogsItem);
        registerMenuItem(viewLogsItem);
        /* expert options */
        final MyMenu hostExpertSubmenu = new MyMenu(
                        Tools.getString("HostBrowser.ExpertSubmenu")) {
            private static final long serialVersionUID = 1L;
            public boolean enablePredicate() {
                return host.isConnected();
            }

            public void update() {
                super.update();
                getBrowser().addExpertMenu(this);
            }
        };
        items.add(hostExpertSubmenu);
        registerMenuItem(hostExpertSubmenu);

        /* remove host from gui */
        final MyMenuItem removeHostItem =
            new MyMenuItem(Tools.getString("HostBrowser.RemoveHost"),
                           HostBrowser.HOST_REMOVE_ICON,
                           null) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return getHost().getCluster() == null;
                }

                public void action() {
                    getHost().disconnect();
                    Tools.getConfigData().removeHostFromHosts(getHost());
                    Tools.getGUIData().allHostsUpdate();
                }
            };
        registerMenuItem(removeHostItem);
        items.add(removeHostItem);


        return items;
    }

    /**
     * Returns Cluster graph.
     */
    private HeartbeatGraph getHeartbeatGraph() {
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        if (b == null) {
            return null;
        }
        return b.getHeartbeatGraph();
    }

    /**
     * Returns grahical view if there is any.
     */
    public final JPanel getGraphicalView() {
        final HeartbeatGraph hg = getHeartbeatGraph();
        if (hg == null) {
            return null;
        }
        return hg.getServicesInfo().getGraphicalView();
    }

    /**
     * Returns how much of this is used.
     */
    public final int getUsed() {
        // TODO: maybe the load?
        return -1;
    }

    /**
     * Returns subtexts that appears in the host vertex in the cluster
     * graph.
     */
    public final Subtext[] getSubtextsForGraph(final boolean testOnly) {
        final List<Subtext> texts = new ArrayList<Subtext>();
        if (getHost().isConnected()) {
            if (!getHost().isClStatus()) {
               texts.add(new Subtext("waiting for cluster status...", null));
            }
        } else {
            texts.add(new Subtext("connecting...", null));
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /**
     * Returns text that appears above the icon in the graph.
     */
    public final String getIconTextForGraph(final boolean testOnly) {
        if (!getHost().isConnected()) {
            return Tools.getString("HostBrowser.Hb.NoInfoAvailable");
        }
        return null;
    }

    /**
     * Returns whether this host is in stand by.
     */
    public final boolean isStandby(final boolean testOnly) {
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        if (b == null) {
            return false;
        }
        return b.isStandby(host, testOnly);
    }

    /**
     * Returns cluster status.
     */
    public final ClusterStatus getClusterStatus() {
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        if (b == null) {
            return null;
        }
        return b.getClusterStatus();
    }

    /**
     * Returns text that appears in the corner of the graph.
     */
    public final Subtext getRightCornerTextForGraph(final boolean testOnly) {
        if (getHost().isClStatus()) {
            if (isStandby(testOnly)) {
                return STANDBY_SUBTEXT;
            } else {
                return ONLINE_SUBTEXT;
            }
        } else if (getHost().isConnected()) {
            return OFFLINE_SUBTEXT;
        }
        return null;
    }

    /**
     * Selects the node in the menu and reloads everything underneath.
     */
    public void selectMyself() {
        super.selectMyself();
        getBrowser().nodeChanged(getNode());
    }
}
