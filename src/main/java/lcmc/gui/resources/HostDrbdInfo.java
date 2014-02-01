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

package lcmc.gui.resources;

import lcmc.AddDrbdUpgradeDialog;
import lcmc.EditHostDialog;
import lcmc.ProxyHostWizard;
import lcmc.gui.Browser;
import lcmc.gui.HostBrowser;
import lcmc.gui.DrbdGraph;
import lcmc.gui.SpringUtilities;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.dialog.lvm.VGCreate;
import lcmc.gui.dialog.lvm.LVCreate;
import lcmc.gui.dialog.drbd.DrbdsLog;
import lcmc.data.Host;
import lcmc.data.Subtext;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.data.resources.BlockDevice;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.DRBD;
import lcmc.utilities.SSH;

import java.util.List;
import java.util.ArrayList;
import java.awt.Font;
import java.awt.Color;
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
import javax.swing.JColorChooser;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class holds info data for a host.
 * It shows host view, just like in the host tab.
 */
public final class HostDrbdInfo extends Info {
    /** Logger. */
    private static final Logger LOG =
                                  LoggerFactory.getLogger(HostDrbdInfo.class);
    /** Host data. */
    private final Host host;
    /** String that is displayed as a tool tip for disabled menu item. */
    static final String NO_DRBD_STATUS_STRING = "drbd status is not available";
    /** LVM menu. */
    private static final String LVM_MENU = "LVM";
    /** Name of the create VG menu item. */
    private static final String VG_CREATE_MENU_ITEM = "Create VG";
    /** Description create VG. */
    private static final String VG_CREATE_MENU_DESCRIPTION =
                                                    "Create a volume group.";
    /** Name of the create menu item. */
    private static final String LV_CREATE_MENU_ITEM = "Create LV in VG ";
    /** Description create LV. */
    private static final String LV_CREATE_MENU_DESCRIPTION =
                                                    "Create a logical volume.";
    /** Prepares a new <code>HostDrbdInfo</code> object. */
    public HostDrbdInfo(final Host host, final Browser browser) {
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

    /** Start upgrade drbd dialog. */
    void upgradeDrbd() {
        final AddDrbdUpgradeDialog adud = new AddDrbdUpgradeDialog(this);
        adud.showDialogs();
    }

    /** Returns tooltip for the host. */
    @Override
    public String getToolTipForGraph(final boolean testOnly) {
        return getBrowser().getHostToolTip(host);
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
                    LOG.sshError(host, "", ans, stacktrace, exitCode);
                }

            };
        // TODO: disable buttons if disconnected?
        final MyButton procDrbdButton = new MyButton("/proc/drbd");
        procDrbdButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                host.execCommand("DRBD.getProcDrbd",
                                 execCallback,
                                 null,  /* ConvertCmdCallback */
                                 false,  /* outputVisible */
                                 SSH.DEFAULT_COMMAND_TIMEOUT);
            }
        });
        host.registerEnableOnConnect(procDrbdButton);
        final MyButton drbdProcsButton = new MyButton("DRBD Processes");
        drbdProcsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                host.execCommand("DRBD.getProcesses",
                                 execCallback,
                                 null,  /* ConvertCmdCallback */
                                 false,  /* outputVisible */
                                 SSH.DEFAULT_COMMAND_TIMEOUT);
            }
        });
        host.registerEnableOnConnect(drbdProcsButton);

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
        p.add(drbdProcsButton);
        SpringUtilities.makeCompactGrid(p, 2, 1,  // rows, cols
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
        host.execCommand("DRBD.getProcDrbd",
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

    /** Returns string representation of the host. It's same as name. */
    @Override
    public String toString() {
        return host.getName();
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

        /* host wizard */
        final MyMenuItem hostWizardItem =
            new MyMenuItem(Tools.getString("HostBrowser.HostWizard"),
                           HostBrowser.HOST_ICON_LARGE,
                           Tools.getString("HostBrowser.HostWizard"),
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
                    final EditHostDialog dialog = new EditHostDialog(host);
                    dialog.showDialogs();
                }
            };
        items.add(hostWizardItem);
        Tools.getGUIData().registerAddHostButton(hostWizardItem);

        /* proxy host wizard */
        final MyMenuItem proxyHostWizardItem =
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
        items.add(proxyHostWizardItem);
        Tools.getGUIData().registerAddHostButton(proxyHostWizardItem);
        final boolean testOnly = false;
        /* load drbd */
        final MyMenuItem loadItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.LoadDrbd"),
                           null,
                           Tools.getString("HostBrowser.Drbd.LoadDrbd"),
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getHost().isConnected()) {
                        if (getHost().isDrbdLoaded()) {
                            return "already loaded";
                        } else {
                            return null;
                        }
                    } else {
                        return Host.NOT_CONNECTED_STRING;
                    }
                }

                @Override
                public void action() {
                    DRBD.load(getHost(), testOnly);
                    getBrowser().getClusterBrowser().updateHWInfo(
                                                        host,
                                                        !Host.UPDATE_LVM);
                }
            };
        items.add(loadItem);

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
                public void action() {
                    if (getHost().isDrbdProxyRunning()) {
                        DRBD.stopProxy(getHost(), testOnly);
                    } else {
                        DRBD.startProxy(getHost(), testOnly);
                    }
                    getBrowser().getClusterBrowser().updateHWInfo(
                                                        host,
                                                        !Host.UPDATE_LVM);
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
                    return host.isDrbdProxyRunning();
                }

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void action() {
                    DRBD.proxyUp(host, DRBD.ALL, null, testOnly);
                    getBrowser().getClusterBrowser().updateHWInfo(
                                                        host,
                                                        !Host.UPDATE_LVM);
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
                    return host.isDrbdProxyRunning();
                }

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void action() {
                    DRBD.proxyDown(host, DRBD.ALL, null, testOnly);
                    getBrowser().getClusterBrowser().updateHWInfo(
                                                        host,
                                                        !Host.UPDATE_LVM);
                }
            };
        items.add(allProxyDownItem);

        /* load DRBD config / adjust all */
        final MyMenuItem adjustAllItem =
            new MyMenuItem(
                   Tools.getString("HostBrowser.Drbd.AdjustAllDrbd"),
                   null,
                   Tools.getString("HostBrowser.Drbd.AdjustAllDrbd.ToolTip"),
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getHost().isConnected()) {
                        return null;
                    } else {
                        return Host.NOT_CONNECTED_STRING;
                    }
                }

                @Override
                public void action() {
                    DRBD.adjust(getHost(), DRBD.ALL, null, testOnly);
                    getBrowser().getClusterBrowser().updateHWInfo(
                                                        host,
                                                        !Host.UPDATE_LVM);
                }
            };
        items.add(adjustAllItem);
        final ClusterBrowser cb = getBrowser().getClusterBrowser();
        if (cb != null) {
            final ClusterBrowser.DRBDMenuItemCallback adjustAllItemCallback =
                            cb.new DRBDMenuItemCallback(adjustAllItem,
                                                        getHost()) {
                @Override
                public void action(final Host host) {
                    DRBD.adjust(getHost(), DRBD.ALL, null, true);
                }
            };
            addMouseOverListener(adjustAllItem, adjustAllItemCallback);
        }

        /* start drbd */
        final MyMenuItem upAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.UpAll"),
                           null,
                           Tools.getString("HostBrowser.Drbd.UpAll"),
                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (!getHost().isDrbdStatus()) {
                        return NO_DRBD_STATUS_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    DRBD.up(getHost(), DRBD.ALL, null, testOnly);
                }
            };
        items.add(upAllItem);
        if (cb != null) {
            final ClusterBrowser.DRBDMenuItemCallback upAllItemCallback =
                            cb.new DRBDMenuItemCallback(upAllItem,
                                                        getHost()) {
                @Override
                public void action(final Host host) {
                    DRBD.up(getHost(), DRBD.ALL, null, true);
                }
            };
            addMouseOverListener(upAllItem, upAllItemCallback);
        }

        /* upgrade drbd */
        final MyMenuItem upgradeDrbdItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.UpgradeDrbd"),
                           null,
                           Tools.getString("HostBrowser.Drbd.UpgradeDrbd"),
                           new AccessMode(ConfigData.AccessType.GOD,
                                          false), // TODO: does not work yet
                           new AccessMode(ConfigData.AccessType.ADMIN, false)) {
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
                    upgradeDrbd();
                }
            };
        items.add(upgradeDrbdItem);

        /* change host color */
        final MyMenuItem changeHostColorItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ChangeHostColor"),
                           null,
                           Tools.getString("HostBrowser.Drbd.ChangeHostColor"),
                           new AccessMode(ConfigData.AccessType.RO, false),
                           new AccessMode(ConfigData.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void action() {
                    Color newColor = JColorChooser.showDialog(
                                            Tools.getGUIData().getMainFrame(),
                                            "Choose " + host.getName()
                                            + " color",
                                            host.getPmColors()[0]);
                    if (newColor != null) {
                        host.setSavedColor(newColor);
                    }
                }
            };
        items.add(changeHostColorItem);

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

        /* connect all */
        final MyMenuItem connectAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ConnectAll"),
                           null,
                           Tools.getString("HostBrowser.Drbd.ConnectAll"),
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getHost().isDrbdStatus()) {
                        return null;
                    } else {
                        return NO_DRBD_STATUS_STRING;
                    }
                }

                @Override
                public void action() {
                    DRBD.connect(getHost(), DRBD.ALL, null, true);
                }
            };
        items.add(connectAllItem);
        if (cb != null) {
            final ClusterBrowser.DRBDMenuItemCallback connectAllItemCallback =
                            cb.new DRBDMenuItemCallback(connectAllItem,
                                                        getHost()) {
                @Override
                public void action(final Host host) {
                    DRBD.connect(getHost(), DRBD.ALL, null, true);
                }
            };
            addMouseOverListener(connectAllItem, connectAllItemCallback);
        }

        /* disconnect all */
        final MyMenuItem disconnectAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.DisconnectAll"),
                           null,
                           Tools.getString("HostBrowser.Drbd.DisconnectAll"),
                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getHost().isDrbdStatus()) {
                        return null;
                    } else {
                        return NO_DRBD_STATUS_STRING;
                    }
                }

                @Override
                public void action() {
                    DRBD.disconnect(getHost(), DRBD.ALL, null, testOnly);
                }
            };
        items.add(disconnectAllItem);
        if (cb != null) {
            final ClusterBrowser.DRBDMenuItemCallback
                    disconnectAllItemCallback =
                            cb.new DRBDMenuItemCallback(disconnectAllItem,
                                                        getHost()) {
                @Override
                public void action(final Host host) {
                    DRBD.disconnect(getHost(), DRBD.ALL, null, true);
                }
            };
            addMouseOverListener(disconnectAllItem, disconnectAllItemCallback);
        }

        /* attach dettached */
        final MyMenuItem attachAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.AttachAll"),
                           null,
                           Tools.getString("HostBrowser.Drbd.AttachAll"),
                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getHost().isDrbdStatus()) {
                        return null;
                    } else {
                        return NO_DRBD_STATUS_STRING;
                    }
                }

                @Override
                public void action() {
                    DRBD.attach(getHost(), DRBD.ALL, null, testOnly);
                }
            };
        items.add(attachAllItem);
        if (cb != null) {
            final ClusterBrowser.DRBDMenuItemCallback
                    attachAllItemCallback =
                            cb.new DRBDMenuItemCallback(attachAllItem,
                                                        getHost()) {
                @Override
                public void action(final Host host) {
                    DRBD.attach(getHost(), DRBD.ALL, null, true);
                }
            };
            addMouseOverListener(attachAllItem, attachAllItemCallback);
        }

        /* detach */
        final MyMenuItem detachAllItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.DetachAll"),
                           null,
                           Tools.getString("HostBrowser.Drbd.DetachAll"),
                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getHost().isDrbdStatus()) {
                        return null;
                    } else {
                        return NO_DRBD_STATUS_STRING;
                    }
                }

                @Override
                public void action() {
                    DRBD.detach(getHost(), DRBD.ALL, null, testOnly);
                }
            };
        items.add(detachAllItem);
        if (cb != null) {
            final ClusterBrowser.DRBDMenuItemCallback
                    detachAllItemCallback =
                            cb.new DRBDMenuItemCallback(detachAllItem,
                                                        getHost()) {
                @Override
                public void action(final Host host) {
                    DRBD.detach(getHost(), DRBD.ALL, null, true);
                }
            };
            addMouseOverListener(detachAllItem, detachAllItemCallback);
        }

        /* set all primary */
        final MyMenuItem setAllPrimaryItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetAllPrimary"),
                           null,
                           Tools.getString("HostBrowser.Drbd.SetAllPrimary"),
                           new AccessMode(
                                   ConfigData.AccessType.ADMIN,
                                   false),
                           new AccessMode(
                                   ConfigData.AccessType.OP,
                                   false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getHost().isDrbdStatus()) {
                        return null;
                    } else {
                        return NO_DRBD_STATUS_STRING;
                    }
                }

                @Override
                public void action() {
                    DRBD.setPrimary(getHost(), DRBD.ALL, null, testOnly);
                }
            };
        items.add(setAllPrimaryItem);
        if (cb != null) {
            final ClusterBrowser.DRBDMenuItemCallback
                    setAllPrimaryItemCallback =
                            cb.new DRBDMenuItemCallback(setAllPrimaryItem,
                                                        getHost()) {
                @Override
                public void action(final Host host) {
                    DRBD.setPrimary(getHost(), DRBD.ALL, null, true);
                }
            };
            addMouseOverListener(setAllPrimaryItem, setAllPrimaryItemCallback);
        }

        /* set all secondary */
        final MyMenuItem setAllSecondaryItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetAllSecondary"),
                           null,
                           Tools.getString("HostBrowser.Drbd.SetAllSecondary"),
                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    if (getHost().isDrbdStatus()) {
                        return null;
                    } else {
                        return NO_DRBD_STATUS_STRING;
                    }
                }

                @Override
                public void action() {
                    DRBD.setSecondary(getHost(), DRBD.ALL, null, testOnly);
                }
            };
        items.add(setAllSecondaryItem);
        if (cb != null) {
            final ClusterBrowser.DRBDMenuItemCallback
                    setAllSecondaryItemCallback =
                            cb.new DRBDMenuItemCallback(setAllSecondaryItem,
                                                        getHost()) {
                @Override
                public void action(final Host host) {
                    DRBD.setSecondary(getHost(), DRBD.ALL, null, true);
                }
            };
            addMouseOverListener(setAllSecondaryItem,
                                 setAllSecondaryItemCallback);
        }

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
                    if (!getHost().isInCluster()) {
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

        /* advanced options */
        final MyMenu hostAdvancedSubmenu = new MyMenu(
                                Tools.getString("HostBrowser.AdvancedSubmenu"),
                                new AccessMode(ConfigData.AccessType.OP, false),
                                new AccessMode(ConfigData.AccessType.OP,
                                               false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public String enablePredicate() {
                if (!host.isConnected()) {
                    return Host.NOT_CONNECTED_STRING;
                }
                return null;
            }

            @Override
            public void updateAndWait() {
                super.updateAndWait();
                getBrowser().addAdvancedMenu(this);
            }
        };
        items.add(hostAdvancedSubmenu);
        items.add(getLVMMenu());
        return items;
    }

    /** Returns lvm menu. */
    private MyMenu getLVMMenu() {
        return new MyMenu(LVM_MENU,
                          new AccessMode(ConfigData.AccessType.OP, true),
                          new AccessMode(ConfigData.AccessType.OP, true)) {
            private static final long serialVersionUID = 1L;
            @Override
            public String enablePredicate() {
                return null;
            }

            @Override
            public void updateAndWait() {
                super.updateAndWait();
                addLVMMenu(this);
            }
        };
    }

    /** Adds menus to manage LVMs. */
    public void addLVMMenu(final MyMenu submenu) {
        submenu.removeAll();
        submenu.add(getVGCreateItem());
        for (final BlockDevice bd : getHost().getBlockDevices()) {
            String vg;
            final BlockDevice drbdBD = bd.getDrbdBlockDevice();
            if (drbdBD == null) {
                vg = bd.getVolumeGroupOnPhysicalVolume();
            } else {
                vg = drbdBD.getVolumeGroupOnPhysicalVolume();
            }
            if (vg != null) {
                submenu.add(getLVMCreateItem(vg, bd));
            }
        }
    }

    /** Return "Create VG" menu item. */
    private MyMenuItem getVGCreateItem() {
        final MyMenuItem mi = new MyMenuItem(
                            VG_CREATE_MENU_ITEM,
                            null,
                            VG_CREATE_MENU_DESCRIPTION,
                            new AccessMode(ConfigData.AccessType.OP, false),
                            new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean visiblePredicate() {
                return true;
            }

            @Override
            public String enablePredicate() {
                return null;
            }

            @Override
            public void action() {
                final VGCreate vgCreate = new VGCreate(getHost());
                while (true) {
                    vgCreate.showDialog();
                    if (vgCreate.isPressedCancelButton()) {
                        vgCreate.cancelDialog();
                        return;
                    } else if (vgCreate.isPressedFinishButton()) {
                        break;
                    }
                }
            }
        };
        mi.setToolTipText(VG_CREATE_MENU_DESCRIPTION);
        return mi;
    }

    /** Return create LV menu item. */
    private MyMenuItem getLVMCreateItem(final String volumeGroup,
                                        final BlockDevice blockDevice) {
        final String name = LV_CREATE_MENU_ITEM + volumeGroup;
        final MyMenuItem mi = new MyMenuItem(
                             name,
                             null,
                             LV_CREATE_MENU_DESCRIPTION,
                             new AccessMode(ConfigData.AccessType.OP, false),
                             new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean visiblePredicate() {
                return !"".equals(volumeGroup)
                       && getHost().getVolumeGroupNames().contains(volumeGroup);
            }

            @Override
            public String enablePredicate() {
                return null;
            }

            @Override
            public void action() {
                final LVCreate lvCreate = new LVCreate(getHost(),
                                                       volumeGroup,
                                                       blockDevice);
                while (true) {
                    lvCreate.showDialog();
                    if (lvCreate.isPressedCancelButton()) {
                        lvCreate.cancelDialog();
                        return;
                    } else if (lvCreate.isPressedFinishButton()) {
                        break;
                    }
                }
            }

            @Override
            public void updateAndWait() {
                setText1(LV_CREATE_MENU_ITEM + volumeGroup);
                super.updateAndWait();
            }
        };

        mi.setToolTipText(LV_CREATE_MENU_DESCRIPTION);
        return mi;
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

    /** Returns how much of this is used. */
    public int getUsed() {
        // TODO: maybe the load?
        return -1;
    }

    /**
     * Returns subtexts that appears in the host vertex in the cluster
     * graph.
     */
    Subtext[] getSubtextsForGraph() {
        final List<Subtext> texts = new ArrayList<Subtext>();
        if (getHost().isConnected()) {
            if (!getHost().isClStatus()) {
               texts.add(new Subtext("waiting for DRBD...",
                                     null,
                                     Color.BLACK));
            }
        } else {
            texts.add(new Subtext("connecting...", null, Color.BLACK));
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /** Returns subtexts that appears in the host vertex in the drbd graph. */
    public Subtext[] getSubtextsForDrbdGraph(final boolean testOnly) {
        final List<Subtext> texts = new ArrayList<Subtext>();
        if (getHost().isConnected()) {
            if (!getHost().isDrbdLoaded()) {
               texts.add(new Subtext("DRBD not loaded", null, Color.BLACK));
            } else if (!getHost().isDrbdStatus()) {
               texts.add(new Subtext("waiting...", null, Color.BLACK));
            }
        } else {
            texts.add(new Subtext("connecting...", null, Color.BLACK));
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /** Returns text that appears above the icon in the drbd graph. */
    public String getIconTextForDrbdGraph(final boolean testOnly) {
        if (!getHost().isConnected()) {
            return Tools.getString("HostBrowser.Drbd.NoInfoAvailable");
        }
        return null;
    }

    /** Returns text that appears in the corner of the drbd graph. */
    public Subtext getRightCornerTextForDrbdGraph(final boolean testOnly) {
        return null;
    }

    /** Tool tip for menu items. */
    private String getMenuToolTip(final String cmd, final String res) {
        return getHost().getDistString(cmd).replaceAll("@RES-VOL@", res)
                                           .replaceAll("@.*?@", "");
    }

    @Override
    public String getValueForConfig() {
        return host.getName();
    }
}
