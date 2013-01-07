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

import lcmc.EditHostDialog;
import lcmc.gui.Browser;
import lcmc.gui.HostBrowser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.SpringUtilities;
import lcmc.gui.dialog.HostLogs;
import lcmc.data.Host;
import lcmc.data.Cluster;
import lcmc.utilities.UpdatableItem;
import lcmc.data.Subtext;
import lcmc.data.ClusterStatus;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.data.PtestData;
import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.CRM;
import lcmc.utilities.SSH;
import lcmc.utilities.Corosync;
import lcmc.utilities.Openais;
import lcmc.utilities.Heartbeat;
import lcmc.utilities.ButtonCallback;
import lcmc.gui.CRMGraph;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
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
import javax.swing.JColorChooser;
import javax.swing.text.Document;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;

/**
 * This class holds info data for a host.
 * It shows host view, just like in the host tab.
 */
public final class HostInfo extends Info {
    /** Host data. */
    private final Host host;
    /** Host standby icon. */
    static final ImageIcon HOST_STANDBY_ICON =
     Tools.createImageIcon(Tools.getDefault("CRMGraph.HostStandbyIcon"));
    /** Host standby off icon. */
    static final ImageIcon HOST_STANDBY_OFF_ICON =
             Tools.createImageIcon(
                        Tools.getDefault("CRMGraph.HostStandbyOffIcon"));
    /** Stop comm layer icon. */
    static final ImageIcon HOST_STOP_COMM_LAYER_ICON =
             Tools.createImageIcon(
                     Tools.getDefault("CRMGraph.HostStopCommLayerIcon"));
    /** Start comm layer icon. */
    static final ImageIcon HOST_START_COMM_LAYER_ICON =
             Tools.createImageIcon(
                    Tools.getDefault("CRMGraph.HostStartCommLayerIcon"));
    /** Offline subtext. */
    private static final Subtext OFFLINE_SUBTEXT =
                                      new Subtext("offline", null, Color.BLUE);
    /** Pending subtext. */
    private static final Subtext PENDING_SUBTEXT =
                                      new Subtext("pending", null, Color.BLUE);
    /** Fenced/unclean subtext. */
    private static final Subtext FENCED_SUBTEXT =
                                    new Subtext("fencing...", null, Color.RED);
    /** Corosync stopped subtext. */
    private static final Subtext CORO_STOPPED_SUBTEXT =
                                      new Subtext("stopped", null, Color.RED);
    /** Pacemaker stopped subtext. */
    private static final Subtext PCMK_STOPPED_SUBTEXT =
                                  new Subtext("pcmk stopped", null, Color.RED);
    /** Unknown subtext. */
    private static final Subtext UNKNOWN_SUBTEXT =
                                      new Subtext("wait...", null, Color.BLUE);
    /** Online subtext. */
    private static final Subtext ONLINE_SUBTEXT =
                                       new Subtext("online", null, Color.BLUE);
    /** Standby subtext. */
    private static final Subtext STANDBY_SUBTEXT =
                                       new Subtext("STANDBY", null, Color.RED);
    /** Stopping subtext. */
    private static final Subtext STOPPING_SUBTEXT =
                                   new Subtext("stopping...", null, Color.RED);
    /** Starting subtext. */
    private static final Subtext STARTING_SUBTEXT =
                                  new Subtext("starting...", null, Color.BLUE);
    /** String that is displayed as a tool tip for disabled menu item. */
    static final String NO_PCMK_STATUS_STRING =
                                             "cluster status is not available";
    /** whether crm info is showing. */
    private volatile boolean crmInfo = false;
    /** whether crm show is in progress. */
    private volatile boolean crmShowInProgress = true;
    /** Prepares a new <code>HostInfo</code> object. */
    public HostInfo(final Host host, final Browser browser) {
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
        final Cluster cl = host.getCluster();
        if (cl != null) {
            return HostBrowser.HOST_IN_CLUSTER_ICON_RIGHT_SMALL;
        }
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

    /** Returns tooltip for the host. */
    @Override
    public String getToolTipForGraph(final boolean testOnly) {
        return getBrowser().getHostToolTip(host);
    }

    /** Returns info panel. */
    @Override
    public JComponent getInfoPanel() {
        if (getBrowser().getClusterBrowser() == null) {
            return new JPanel();
        }
        final Font f = new Font("Monospaced",
                                Font.PLAIN,
                                Tools.getConfigData().scaled(12));
        crmShowInProgress = true;
        final JTextArea ta = new JTextArea(
                                  Tools.getString("HostInfo.crmShellLoading"));
        ta.setEditable(false);
        ta.setFont(f);

        final MyButton crmConfigureCommitButton =
                new MyButton(Tools.getString("HostInfo.crmShellCommitButton"),
                             Browser.APPLY_ICON);
        registerComponentEnableAccessMode(
                                    crmConfigureCommitButton,
                                    new AccessMode(ConfigData.AccessType.ADMIN,
                                                   false));
        final MyButton hostInfoButton =
                new MyButton(Tools.getString("HostInfo.crmShellStatusButton"));
        hostInfoButton.miniButton();

        final MyButton crmConfigureShowButton =
                  new MyButton(Tools.getString("HostInfo.crmShellShowButton"));
        crmConfigureShowButton.miniButton();
        crmConfigureCommitButton.setEnabled(false);
        final ExecCallback execCallback =
            new ExecCallback() {
                @Override
                public void done(final String ans) {
                    ta.setText(ans);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            crmConfigureShowButton.setEnabled(true);
                            hostInfoButton.setEnabled(true);
                            crmShowInProgress = false;
                        }
                    });
                }

                @Override
                public void doneError(final String ans, final int exitCode) {
                    ta.setText(ans);
                    Tools.sshError(host, "", ans, "", exitCode);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            crmConfigureCommitButton.setEnabled(false);
                        }
                    });
                }

            };
        hostInfoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                registerComponentEditAccessMode(
                                ta,
                                new AccessMode(ConfigData.AccessType.GOD,
                                               false));
                crmInfo = true;
                hostInfoButton.setEnabled(false);
                crmConfigureCommitButton.setEnabled(false);
                String command = "HostBrowser.getHostInfo";
                if (!host.isCsInit()) {
                    command = "HostBrowser.getHostInfoHeartbeat";
                }
                host.execCommand(command,
                                 execCallback,
                                 null,  /* ConvertCmdCallback */
                                 false,  /* outputVisible */
                                 SSH.DEFAULT_COMMAND_TIMEOUT);
            }
        });
        host.registerEnableOnConnect(hostInfoButton);

        crmConfigureShowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                registerComponentEditAccessMode(
                                    ta,
                                    new AccessMode(ConfigData.AccessType.ADMIN,
                                                   false));
                updateAdvancedPanels();
                crmShowInProgress = true;
                crmInfo = false;
                crmConfigureShowButton.setEnabled(false);
                crmConfigureCommitButton.setEnabled(false);
                host.execCommand("HostBrowser.getCrmConfigureShow",
                                 execCallback,
                                 null,  /* ConvertCmdCallback */
                                 false,  /* outputVisible */
                                 SSH.DEFAULT_COMMAND_TIMEOUT);
            }
        });
        final CRMGraph crmg = getBrowser().getClusterBrowser().getCRMGraph();
        final Document taDocument = ta.getDocument();
        taDocument.addDocumentListener(new DocumentListener() {
            private void update() {
                if (!crmShowInProgress && !crmInfo) {
                    crmConfigureCommitButton.setEnabled(true);
                }
            }

            public void changedUpdate(final DocumentEvent documentEvent) {
                update();
            }

            public void insertUpdate(final DocumentEvent documentEvent) {
                update();
            }

            public void removeUpdate(final DocumentEvent documentEvent) {
                update();
            }
        });

        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;

            /**
             * Whether the whole thing should be enabled.
             */
            @Override
            public boolean isEnabled() {
                if (Tools.versionBeforePacemaker(host)) {
                    return false;
                }
                return true;
            }

            @Override
            public void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                crmg.stopTestAnimation(crmConfigureCommitButton);
                crmConfigureCommitButton.setToolTipText(null);
            }

            @Override
            public void mouseOver() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                crmConfigureCommitButton.setToolTipText(
                                        ClusterBrowser.STARTING_PTEST_TOOLTIP);
                crmConfigureCommitButton.setToolTipBackground(
                            Tools.getDefaultColor(
                                    "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                crmg.startTestAnimation(crmConfigureCommitButton, startTestLatch);
                final Host dcHost =
                                  getBrowser().getClusterBrowser().getDCHost();
                getBrowser().getClusterBrowser().ptestLockAcquire();
                final ClusterStatus clStatus =
                            getBrowser().getClusterBrowser().getClusterStatus();
                clStatus.setPtestData(null);
                CRM.crmConfigureCommit(host, ta.getText(), true);
                final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
                crmConfigureCommitButton.setToolTipText(ptestData.getToolTip());
                clStatus.setPtestData(ptestData);
                getBrowser().getClusterBrowser().ptestLockRelease();
                startTestLatch.countDown();
            }
        };
        addMouseOverListener(crmConfigureCommitButton, buttonCallback);
        crmConfigureCommitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                crmConfigureCommitButton.setEnabled(false);
                final Thread thread = new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            getBrowser().getClusterBrowser().clStatusLock();
                            final String ret =
                             CRM.crmConfigureCommit(host, ta.getText(), false);
                            getBrowser().getClusterBrowser().clStatusUnlock();
                        }
                    }
                );
                thread.start();
            }
        });

        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(HostBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        mainPanel.add(buttonPanel);

        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.EAST);
        final JPanel p = new JPanel(new SpringLayout());
        p.setBackground(HostBrowser.BUTTON_PANEL_BACKGROUND);

        p.add(hostInfoButton);
        p.add(crmConfigureShowButton);
        p.add(crmConfigureCommitButton);

        p.add(new JLabel(""));
        SpringUtilities.makeCompactGrid(p, 1, 3,  // rows, cols
                                           1, 1,  // initX, initY
                                           1, 1); // xPad, yPad
        mainPanel.setMinimumSize(new Dimension(
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        mainPanel.setPreferredSize(new Dimension(
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        buttonPanel.add(p);
        mainPanel.add(new JLabel(Tools.getString("HostInfo.crmShellInfo")));
        mainPanel.add(new JScrollPane(ta));
        String command = "HostBrowser.getHostInfo";
        if (!host.isCsInit()) {
            command = "HostBrowser.getHostInfoHeartbeat";
        }
        host.execCommand(command,
                         execCallback,
                         null,  /* ConvertCmdCallback */
                         false,  /* outputVisible */
                         SSH.DEFAULT_COMMAND_TIMEOUT);
        return mainPanel;
    }

    /** Gets host. */
    public Host getHost() {
        return host;
    }

    /**
     * Compares this host info name with specified hostinfo's name.
     *
     * @param otherHI
     *              other host info
     * @return true if they are equal
     */
    boolean equals(final HostInfo otherHI) {
        if (otherHI == null) {
            return false;
        }
        return otherHI.toString().equals(host.getName());
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
        final boolean testOnly = false;
        /* host wizard */
        final MyMenuItem hostWizardItem =
            new MyMenuItem(Tools.getString("HostBrowser.HostWizard"),
                           HostBrowser.HOST_ICON_LARGE,
                           "",
                           new AccessMode(ConfigData.AccessType.RO, false),
                           new AccessMode(ConfigData.AccessType.RO, false)) {
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
        /* cluster manager standby on/off */
        final HostInfo thisClass = this;
        final MyMenuItem standbyItem =
            new MyMenuItem(Tools.getString("HostBrowser.CRM.StandByOn"),
                           HOST_STANDBY_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           Tools.getString("HostBrowser.CRM.StandByOff"),
                           HOST_STANDBY_OFF_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return !isStandby(testOnly);
                }

                @Override
                public String enablePredicate() {
                    if (!getHost().isClStatus()) {
                        return NO_PCMK_STATUS_STRING;
                    }
                    return null;
                }

                @Override
                public void action() {
                    final Host dcHost =
                                  getBrowser().getClusterBrowser().getDCHost();
                    if (isStandby(testOnly)) {
                        CRM.standByOff(dcHost, host, testOnly);
                    } else {
                        CRM.standByOn(dcHost, host, testOnly);
                    }
                }
            };
        final ClusterBrowser cb = getBrowser().getClusterBrowser();
        if (cb != null) {
            final ClusterBrowser.ClMenuItemCallback standbyItemCallback =
                              cb.new ClMenuItemCallback(standbyItem, host) {
                @Override
                public void action(final Host dcHost) {
                    if (isStandby(false)) {
                        CRM.standByOff(dcHost, host, true);
                    } else {
                        CRM.standByOn(dcHost, host, true);
                    }
                }
            };
            addMouseOverListener(standbyItem, standbyItemCallback);
        }
        items.add(standbyItem);

        /* Migrate all services from this host. */
        final MyMenuItem allMigrateFromItem =
            new MyMenuItem(Tools.getString("HostInfo.CRM.AllMigrateFrom"),
                           HOST_STANDBY_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,
                           new AccessMode(ConfigData.AccessType.OP, false),
                           new AccessMode(ConfigData.AccessType.OP, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    return true;
                }

                @Override
                public String enablePredicate() {
                    if (!getHost().isClStatus()) {
                        return NO_PCMK_STATUS_STRING;
                    }
                    if (getBrowser().getClusterBrowser()
                                    .getExistingServiceList(null).isEmpty()) {
                        return "there are no services to migrate";
                    }
                    return null;
                }

                @Override
                public void action() {
                    for (final ServiceInfo si
                            : cb.getExistingServiceList(null)) {
                        if (!si.isConstraintPH()
                            && si.getGroupInfo() == null
                            && si.getCloneInfo() == null) {
                            final List<String> runningOnNodes =
                                                   si.getRunningOnNodes(false);
                            if (runningOnNodes != null
                                && runningOnNodes.contains(
                                                        getHost().getName())) {
                                final Host dcHost = getHost();
                                si.migrateFromResource(dcHost,
                                                       getHost().getName(),
                                                       false);
                            }
                        }
                    }
                }
            };
        if (cb != null) {
            final ClusterBrowser.ClMenuItemCallback allMigrateFromItemCallback =
                    cb.new ClMenuItemCallback(allMigrateFromItem, host) {
                @Override
                public void action(final Host dcHost) {
                    for (final ServiceInfo si
                            : cb.getExistingServiceList(null)) {
                        if (!si.isConstraintPH() && si.getGroupInfo() == null) {
                            final List<String> runningOnNodes =
                                                   si.getRunningOnNodes(false);
                            if (runningOnNodes != null
                                && runningOnNodes.contains(
                                                        host.getName())) {
                                si.migrateFromResource(dcHost,
                                                       host.getName(),
                                                       true);
                            }
                        }
                    }
                }
            };
            addMouseOverListener(allMigrateFromItem,
                                 allMigrateFromItemCallback);
        }
        items.add(allMigrateFromItem);
        /* Stop corosync/openais. */
        final MyMenuItem stopCorosyncItem =
            new MyMenuItem(Tools.getString("HostInfo.StopCorosync"),
                           HOST_STOP_COMM_LAYER_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           Tools.getString("HostInfo.StopOpenais"),
                           HOST_STOP_COMM_LAYER_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(ConfigData.AccessType.ADMIN, true),
                           new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean predicate() {
                    /* when both are running it's openais. */
                    return getHost().isCsRunning() && !getHost().isAisRunning();
                }

                @Override
                public boolean visiblePredicate() {
                    return getHost().isCsRunning()
                           || getHost().isAisRunning();
                }

                @Override
                public void action() {
                    if (Tools.confirmDialog(
                         Tools.getString("HostInfo.confirmCorosyncStop.Title"),
                         Tools.getString("HostInfo.confirmCorosyncStop.Desc"),
                         Tools.getString("HostInfo.confirmCorosyncStop.Yes"),
                         Tools.getString("HostInfo.confirmCorosyncStop.No"))) {
                        final Host host = getHost();
                        host.setCommLayerStopping(true);
                        if (!host.isPcmkStartedByCorosync()
                            && host.isPcmkInit()
                            && host.isPcmkRunning()) {
                            Corosync.stopCorosyncWithPcmk(host);
                        } else {
                            Corosync.stopCorosync(host);
                        }

                        getBrowser().getClusterBrowser().updateHWInfo(host);
                    }
                }
            };
        if (cb != null) {
            final ClusterBrowser.ClMenuItemCallback stopCorosyncItemCallback =
                            cb.new ClMenuItemCallback(stopCorosyncItem, host) {
                @Override
                public void action(final Host dcHost) {
                    if (!isStandby(false)) {
                        CRM.standByOn(dcHost, host, true);
                    }
                }
            };
            addMouseOverListener(stopCorosyncItem,
                                 stopCorosyncItemCallback);
        }
        items.add(stopCorosyncItem);
        /* Stop heartbeat. */
        final MyMenuItem stopHeartbeatItem =
            new MyMenuItem(Tools.getString("HostInfo.StopHeartbeat"),
                           HOST_STOP_COMM_LAYER_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(ConfigData.AccessType.ADMIN, true),
                           new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    return getHost().isHeartbeatRunning();
                }

                @Override
                public void action() {
                    if (Tools.confirmDialog(
                         Tools.getString("HostInfo.confirmHeartbeatStop.Title"),
                         Tools.getString("HostInfo.confirmHeartbeatStop.Desc"),
                         Tools.getString("HostInfo.confirmHeartbeatStop.Yes"),
                         Tools.getString("HostInfo.confirmHeartbeatStop.No"))) {
                        getHost().setCommLayerStopping(true);
                        Heartbeat.stopHeartbeat(getHost());
                        getBrowser().getClusterBrowser().updateHWInfo(host);
                    }
                }
            };
        if (cb != null) {
            final ClusterBrowser.ClMenuItemCallback stopHeartbeatItemCallback =
                            cb.new ClMenuItemCallback(stopHeartbeatItem, host) {
                @Override
                public void action(final Host dcHost) {
                    if (!isStandby(false)) {
                        CRM.standByOn(dcHost, host, true);
                    }
                }
            };
            addMouseOverListener(stopHeartbeatItem,
                                 stopHeartbeatItemCallback);
        }
        items.add(stopHeartbeatItem);
        /* Start corosync. */
        final MyMenuItem startCorosyncItem =
            new MyMenuItem(Tools.getString("HostInfo.StartCorosync"),
                           HOST_START_COMM_LAYER_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    final Host h = getHost();
                    return h.isCorosync()
                           && h.isCsInit()
                           && h.isCsAisConf()
                           && !h.isCsRunning()
                           && !h.isAisRunning()
                           && !h.isHeartbeatRunning()
                           && !h.isHeartbeatRc();
                }

                @Override
                public String enablePredicate() {
                    final Host h = getHost();
                    if (h.isAisRc() && !h.isCsRc()) {
                        return "Openais is in rc.d";
                    }
                    return null;
                }

                @Override
                public void action() {
                    getHost().setCommLayerStarting(true);
                    if (getHost().isPcmkRc()) {
                        Corosync.startCorosyncWithPcmk(getHost());
                    } else {
                        Corosync.startCorosync(getHost());
                    }
                    getBrowser().getClusterBrowser().updateHWInfo(host);
                }
            };
        if (cb != null) {
            final ClusterBrowser.ClMenuItemCallback startCorosyncItemCallback =
                            cb.new ClMenuItemCallback(startCorosyncItem, host) {
                @Override
                public void action(final Host host) {
                    //TODO
                }
            };
            addMouseOverListener(startCorosyncItem,
                                 startCorosyncItemCallback);
        }
        items.add(startCorosyncItem);
        /* Start openais. */
        final MyMenuItem startOpenaisItem =
            new MyMenuItem(Tools.getString("HostInfo.StartOpenais"),
                           HOST_START_COMM_LAYER_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    final Host h = getHost();
                    return h.isAisInit()
                           && h.isCsAisConf()
                           && !h.isCsRunning()
                           && !h.isAisRunning()
                           && !h.isHeartbeatRunning()
                           && !h.isHeartbeatRc();
                }

                @Override
                public String enablePredicate() {
                    final Host h = getHost();
                    if (h.isCsRc() && !h.isAisRc()) {
                        return "Corosync is in rc.d";
                    }
                    return null;
                }

                @Override
                public void action() {
                    getHost().setCommLayerStarting(true);
                    Openais.startOpenais(getHost());
                    getBrowser().getClusterBrowser().updateHWInfo(host);
                }
            };
        if (cb != null) {
            final ClusterBrowser.ClMenuItemCallback startOpenaisItemCallback =
                            cb.new ClMenuItemCallback(startOpenaisItem, host) {
                @Override
                public void action(final Host host) {
                    //TODO
                }
            };
            addMouseOverListener(startOpenaisItem,
                                 startOpenaisItemCallback);
        }
        items.add(startOpenaisItem);
        /* Start heartbeat. */
        final MyMenuItem startHeartbeatItem =
            new MyMenuItem(Tools.getString("HostInfo.StartHeartbeat"),
                           HOST_START_COMM_LAYER_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    final Host h = getHost();
                    return h.isHeartbeatInit()
                           && h.isHeartbeatConf()
                           && !h.isCsRunning()
                           && !h.isAisRunning()
                           && !h.isHeartbeatRunning();
                           //&& !h.isAisRc()
                           //&& !h.isCsRc(); TODO should check /etc/defaults/
                }

                @Override
                public void action() {
                    getHost().setCommLayerStarting(true);
                    Heartbeat.startHeartbeat(getHost());
                    getBrowser().getClusterBrowser().updateHWInfo(host);
                }
            };
        if (cb != null) {
            final ClusterBrowser.ClMenuItemCallback startHeartbeatItemCallback =
                          cb.new ClMenuItemCallback(startHeartbeatItem, host) {
                @Override
                public void action(final Host host) {
                    //TODO
                }
            };
            addMouseOverListener(startHeartbeatItem,
                                 startHeartbeatItemCallback);
        }
        items.add(startHeartbeatItem);

        /* Start pacemaker. */
        final MyMenuItem startPcmkItem =
            new MyMenuItem(Tools.getString("HostInfo.StartPacemaker"),
                           HOST_START_COMM_LAYER_ICON,
                           ClusterBrowser.STARTING_PTEST_TOOLTIP,

                           new AccessMode(ConfigData.AccessType.ADMIN, false),
                           new AccessMode(ConfigData.AccessType.ADMIN, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean visiblePredicate() {
                    final Host h = getHost();
                    return !h.isPcmkStartedByCorosync()
                           && !h.isPcmkRunning()
                           && (h.isCsRunning()
                               || h.isAisRunning())
                           && !h.isHeartbeatRunning();
                }

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void action() {
                    host.setPcmkStarting(true);
                    Corosync.startPacemaker(host);
                    getBrowser().getClusterBrowser().updateHWInfo(host);
                }
            };
        if (cb != null) {
            final ClusterBrowser.ClMenuItemCallback startPcmkItemCallback =
                         cb.new ClMenuItemCallback(startPcmkItem, host) {
                @Override
                public void action(final Host host) {
                    //TODO
                }
            };
            addMouseOverListener(startPcmkItem,
                                 startPcmkItemCallback);
        }
        items.add(startPcmkItem);
        /* change host color */
        final MyMenuItem changeHostColorItem =
            new MyMenuItem(Tools.getString("HostBrowser.Drbd.ChangeHostColor"),
                           null,
                           "",
                           new AccessMode(ConfigData.AccessType.RO, false),
                           new AccessMode(ConfigData.AccessType.RO, false)) {
                private static final long serialVersionUID = 1L;

                @Override
                public String enablePredicate() {
                    return null;
                }

                @Override
                public void action() {
                    final Color newColor = JColorChooser.showDialog(
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
                           "",
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
                    HostLogs l = new HostLogs(host);
                    l.showDialog();
                }
            };
        items.add(viewLogsItem);
        /* advacend options */
        final MyMenu hostAdvancedSubmenu = new MyMenu(
                        Tools.getString("HostBrowser.AdvancedSubmenu"),
                        new AccessMode(ConfigData.AccessType.OP, false),
                        new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (!host.isConnected()) {
                    return Host.NOT_CONNECTED_STRING;
                }
                return null;
            }

            @Override
            public void update() {
                super.update();
                getBrowser().addAdvancedMenu(this);
            }
        };
        items.add(hostAdvancedSubmenu);

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
                    if (getHost().getCluster() != null) {
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
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        if (b == null) {
            return null;
        }
        return b.getServicesInfo().getGraphicalView();
    }

    /** Returns how much of this is used. */
    public int getUsed() {
        // TODO: maybe the load?
        return -1;
    }

    /**
     * Returns subtexts that appears in the host vertex in the cluster graph.
     */
    public Subtext[] getSubtextsForGraph(final boolean testOnly) {
        final List<Subtext> texts = new ArrayList<Subtext>();
        if (getHost().isConnected()) {
            if (!getHost().isClStatus()) {
               texts.add(new Subtext("waiting for Pacemaker...",
                                     null,
                                     Color.BLACK));
            }
        } else {
            texts.add(new Subtext("connecting...", null, Color.BLACK));
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /** Returns text that appears above the icon in the graph. */
    public String getIconTextForGraph(final boolean testOnly) {
        if (!getHost().isConnected()) {
            return Tools.getString("HostBrowser.Hb.NoInfoAvailable");
        }
        return null;
    }

    /** Returns whether this host is in stand by. */
    public boolean isStandby(final boolean testOnly) {
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        if (b == null) {
            return false;
        }
        return b.isStandby(host, testOnly);
    }

    /** Returns cluster status. */
    ClusterStatus getClusterStatus() {
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        if (b == null) {
            return null;
        }
        return b.getClusterStatus();
    }

    /** Returns text that appears in the corner of the graph. */
    public Subtext getRightCornerTextForGraph(final boolean testOnly) {
        if (getHost().isCommLayerStopping()) {
            return STOPPING_SUBTEXT;
        } else if (getHost().isCommLayerStarting()) {
            return STARTING_SUBTEXT;
        } else if (getHost().isPcmkStarting()) {
            return STARTING_SUBTEXT;
        }
        final ClusterStatus cs = getClusterStatus();
        if (cs != null && cs.isFencedNode(host.getName())) {
            return FENCED_SUBTEXT;
        } else if (getHost().isClStatus()) {
            if (isStandby(testOnly)) {
                return STANDBY_SUBTEXT;
            } else {
                return ONLINE_SUBTEXT;
            }
        } else if (getHost().isConnected()) {
            final Boolean running = getHost().getCorosyncHeartbeatRunning();
            if (running == null) {
                return UNKNOWN_SUBTEXT;
            } else if (!running) {
                return CORO_STOPPED_SUBTEXT;
            }
            if (cs != null && cs.isPendingNode(host.getName())) {
                return PENDING_SUBTEXT;
            } else if (!getHost().isPcmkRunning()) {
                return PCMK_STOPPED_SUBTEXT;
            } else if (cs != null
                       && "no".equals(cs.isOnlineNode(host.getName()))) {
                return OFFLINE_SUBTEXT;
            } else {
                return UNKNOWN_SUBTEXT;
            }
        }
        return null;
    }

    /** Selects the node in the menu and reloads everything underneath. */
    @Override
    public void selectMyself() {
        super.selectMyself();
        getBrowser().nodeChanged(getNode());
    }
}
