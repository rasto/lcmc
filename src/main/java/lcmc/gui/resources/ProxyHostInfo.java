/*
 * This file is part of Linux Cluster Management Console
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2012-2013, Rasto Levrinc
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.gui.resources;

import lcmc.ProxyHostWizard;
import lcmc.gui.Browser;
import lcmc.gui.HostBrowser;
import lcmc.gui.DrbdGraph;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.drbd.DrbdsLog;
import lcmc.data.Host;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.DRBD;
import lcmc.utilities.SSH;

import java.util.List;
import java.util.ArrayList;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;

/**
 * This class holds info data for a host.
 */
public final class ProxyHostInfo extends Info {
    /** Host data. */
    private final Host host;
    /** Name prefix that appears in the menu. */
    private static final String NAME_PREFIX =
                                    Tools.getString("ProxyHostInfo.NameInfo");
    /** Not connectable. */
    private static final String NOT_CONNECTABLE_STRING =
                               Tools.getString("ProxyHostInfo.NotConnectable");
    /** Prepares a new <code>ProxyHostInfo</code> object. */
    public ProxyHostInfo(final Host host, final Browser browser) {
        super(host.getName(), browser);
        this.host = host;
    }

    /** Returns browser object of this info. */
    @Override
    protected HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    /** Returns a host icon for the menu. */
    @Override
    public ImageIcon getMenuIcon(final boolean testOnly) {
        return HostBrowser.HOST_ICON;
    }

    /** Returns id, which is name of the host. */
    @Override
    public String getId() {
        return host.getName();
    }

    /** Returns a host icon for the category in the menu. */
    @Override
    public ImageIcon getCategoryIcon(final boolean testOnly) {
        return HostBrowser.HOST_ICON;
    }

    /** Returns the info panel. */
    @Override
    public JComponent getInfoPanel() {
        final Font f = new Font("Monospaced",
                                Font.PLAIN,
                                Tools.getConfigData().scaled(12));
        final JTextArea ta = new JTextArea();
        ta.setFont(f);

        final String stacktrace = Tools.getStackTrace();
        final ExecCallback execCallback =
            new ExecCallback() {
                @Override
                public void done(final String ans) {
                    ta.setText(ans);
                }

                @Override
                public void doneError(final String ans, final int exitCode) {
                    ta.setText("error");
                    Tools.sshError(host, "", ans, stacktrace, exitCode);
                }

            };
        // TODO: disable buttons if disconnected?
        final MyButton procDrbdButton = new MyButton("Show Proxy Info");
        procDrbdButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                host.execCommand("DRBD.showProxyInfo",
                                 execCallback,
                                 null,  /* ConvertCmdCallback */
                                 false,  /* outputVisible */
                                 SSH.DEFAULT_COMMAND_TIMEOUT);
            }
        });
        host.registerEnableOnConnect(procDrbdButton);

        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(HostBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(
                        new Dimension(0, Tools.getConfigData().scaled(50)));
        buttonPanel.setPreferredSize(
                        new Dimension(0, Tools.getConfigData().scaled(50)));
        buttonPanel.setMaximumSize(
             new Dimension(Short.MAX_VALUE, Tools.getConfigData().scaled(50)));
        mainPanel.add(buttonPanel);

        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.EAST);
        final JPanel p = new JPanel(new SpringLayout());
        p.setBackground(HostBrowser.BUTTON_PANEL_BACKGROUND);

        p.add(procDrbdButton);
        SpringUtilities.makeCompactGrid(p, 1, 1,  // rows, cols
                                           1, 1,  // initX, initY
                                           1, 1); // xPad, yPad
        mainPanel.setMinimumSize(new Dimension(
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        mainPanel.setPreferredSize(new Dimension(
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        buttonPanel.add(p);
        mainPanel.add(new JScrollPane(ta));
        host.execCommand("DRBD.showProxyInfo",
                         execCallback,
                         null,  /* ConvertCmdCallback */
                         false,  /* outputVisible */
                         SSH.DEFAULT_COMMAND_TIMEOUT);
        return mainPanel;
    }

    /** Returns host. */
    public Host getHost() {
        return host;
    }


    /**
     * Compares this host info name with specified ProxyHostInfo's name.
     *
     * @param otherHI
     *              other host info
     * @return true if they are equal
     */
    boolean equals(final ProxyHostInfo otherHI) {
        if (otherHI == null) {
            return false;
        }
        return otherHI.toString().equals(host.getName());
    }

    /** Returns string representation of the host. It's same as name. */
    @Override
    public String toString() {
        return NAME_PREFIX + host.getName();
    }

    /** Returns name of the host. */
    @Override
    public String getName() {
        return host.getName();
    }

    /** Creates the popup for the host. */
    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

        /* connect */
        final MyMenuItem connectItem =
            new MyMenuItem(Tools.getString("HostDrbdInfo.Connect"),
                           null,
                           Tools.getString("HostDrbdInfo.Connect"),
                           Tools.getString("HostDrbdInfo.Disconnect"),
                           null,
                           Tools.getString("HostDrbdInfo.Disconnect"),
                           new AccessMode(ConfigData.AccessType.RO,
                                          !AccessMode.ADVANCED),
                           new AccessMode(ConfigData.AccessType.RO,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return !getHost().isConnected();
                }

                @Override
                public String enablePredicate() {
                    if (getHost().getUsername() == null) {
                        return NOT_CONNECTABLE_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    if (getHost().isConnected()) {
                        getHost().disconnect();
                    } else {
                        host.connect(null, null);
                    }
                    getBrowser().getClusterBrowser().updateProxyHWInfo(host);
                }
            };
        items.add(connectItem);

        /* host wizard */
        final MyMenuItem hostWizardItem =
            new MyMenuItem(Tools.getString("HostBrowser.ProxyHostWizard"),
                           HostBrowser.HOST_ICON_LARGE,
                           Tools.getString("HostBrowser.ProxyHostWizard"),
                           new AccessMode(ConfigData.AccessType.RO,
                                          false),
                           new AccessMode(ConfigData.AccessType.RO,
                                          false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void action() {
                    final ProxyHostWizard dialog = new ProxyHostWizard(host,
                                                                       null);
                    dialog.showDialogs();
                }
            };
        items.add(hostWizardItem);
        Tools.getGUIData().registerAddHostButton(hostWizardItem);
        final boolean testOnly = false;

        /* proxy start/stop */
        final MyMenuItem proxyItem =
            new MyMenuItem(Tools.getString("HostDrbdInfo.Drbd.StopProxy"),
                           null,
                           getMenuToolTip("DRBD.stopProxy", ""),
                           Tools.getString("HostDrbdInfo.Drbd.StartProxy"),
                           null,
                           getMenuToolTip("DRBD.startProxy", ""),
                           new AccessMode(ConfigData.AccessType.ADMIN,
                                          !AccessMode.ADVANCED),
                           new AccessMode(ConfigData.AccessType.OP,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return getHost().isDrbdProxyRunning();
                }

                @Override
                public String enablePredicate() {
                    if (!getHost().isConnected()) {
                        return Host.NOT_CONNECTED_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    if (getHost().isDrbdProxyRunning()) {
                        DRBD.stopProxy(getHost(), testOnly);
                    } else {
                        DRBD.startProxy(getHost(), testOnly);
                    }
                    getBrowser().getClusterBrowser().updateProxyHWInfo(host);
                }
            };
        items.add(proxyItem);

        /* all proxy connections up */
        final MyMenuItem allProxyUpItem =
            new MyMenuItem(Tools.getString("HostDrbdInfo.Drbd.AllProxyUp"),
                           null,
                           getMenuToolTip("DRBD.proxyUp", DRBD.ALL),
                           new AccessMode(ConfigData.AccessType.ADMIN,
                                          !AccessMode.ADVANCED),
                           new AccessMode(ConfigData.AccessType.OP,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    if (!host.isConnected()) {
                        return false;
                    }
                    if (!host.isDrbdProxyRunning()) {
                        return false;
                    }
                    return true;
                }

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void action() {
                    DRBD.proxyUp(host, DRBD.ALL, null, testOnly);
                    getBrowser().getClusterBrowser().updateProxyHWInfo(host);
                }
            };
        items.add(allProxyUpItem);

        /* all proxy connections down */
        final MyMenuItem allProxyDownItem =
            new MyMenuItem(Tools.getString("HostDrbdInfo.Drbd.AllProxyDown"),
                           null,
                           getMenuToolTip("DRBD.proxyDown", DRBD.ALL),
                           new AccessMode(ConfigData.AccessType.ADMIN,
                                          AccessMode.ADVANCED),
                           new AccessMode(ConfigData.AccessType.OP,
                                          !AccessMode.ADVANCED)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    if (!host.isConnected()) {
                        return false;
                    }
                    if (!host.isDrbdProxyRunning()) {
                        return false;
                    }
                    return true;
                }

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void action() {
                    DRBD.proxyDown(host, DRBD.ALL, null, testOnly);
                    getBrowser().getClusterBrowser().updateProxyHWInfo(host);
                }
            };
        items.add(allProxyDownItem);

        /* view logs */
        final MyMenuItem viewLogsItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ViewLogs"),
                           LOGFILE_ICON,
                           Tools.getString("HostBrowser.Drbd.ViewLogs"),
                           new AccessMode(ConfigData.AccessType.RO, false),
                           new AccessMode(ConfigData.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (!getHost().isConnected()) {
                        return Host.NOT_CONNECTED_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    DrbdsLog l = new DrbdsLog(host);
                    l.showDialog();
                }
            };
        items.add(viewLogsItem);

        /* remove host from gui */
        final MyMenuItem removeHostItem =
            new MyMenuItem(Tools.getString("HostBrowser.RemoveHost"),
                           HostBrowser.HOST_REMOVE_ICON,
                           Tools.getString("HostBrowser.RemoveHost"),
                           new AccessMode(ConfigData.AccessType.RO, false),
                           new AccessMode(ConfigData.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getHost().isInCluster()) {
                        return "it is a member of a cluster";
                    }
                    return null;
                }

                @Override
                public void action() {
                    getHost().disconnect();
                    Tools.getConfigData().removeHostFromHosts(getHost());
                    Tools.getGUIData().allHostsUpdate();
                }
            };
        items.add(removeHostItem);

        return items;
    }

    /** Returns grahical view if there is any. */
    @Override
    public JPanel getGraphicalView() {
        final DrbdGraph dg = getBrowser().getDrbdGraph();
        if (dg == null) {
            return null;
        }
        dg.getDrbdInfo().setSelectedNode(null);
        return dg.getDrbdInfo().getGraphicalView();
    }

    /** Tool tip for menu items. */
    private String getMenuToolTip(final String cmd, final String res) {
        return getHost().getDistString(cmd).replaceAll("@RES-VOL@", res)
                                           .replaceAll("@.*?@", "");
    }
}
