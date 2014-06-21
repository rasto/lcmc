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

package lcmc.gui.resources.crm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import lcmc.data.AccessMode;
import lcmc.data.Application;
import lcmc.data.Cluster;
import lcmc.data.ClusterStatus;
import lcmc.data.Host;
import lcmc.data.PtestData;
import lcmc.data.Subtext;
import lcmc.gui.Browser;
import lcmc.gui.CRMGraph;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.HostBrowser;
import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.Info;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.CRM;
import lcmc.utilities.ComponentWithTest;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.ssh.ExecCommandConfig;
import lcmc.utilities.ssh.Ssh;

/**
 * This class holds info data for a host.
 * It shows host view, just like in the host tab.
 */
public class HostInfo extends Info {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(HostInfo.class);
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
    /** Host data. */
    private final Host host;
    /** whether crm info is showing. */
    private volatile boolean crmInfo = false;
    /** whether crm show is in progress. */
    private volatile boolean crmShowInProgress = true;
    /** Prepares a new {@code HostInfo} object. */
    public HostInfo(final Host host, final Browser browser) {
        super(host.getName(), browser);
        this.host = host;
    }

    /** Returns browser object of this info. */
    @Override
    public HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    /** Returns a host icon for the menu. */
    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
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
    public ImageIcon getCategoryIcon(final Application.RunMode runMode) {
        return HostBrowser.HOST_ICON;
    }

    /** Returns tooltip for the host. */
    @Override
    public String getToolTipForGraph(final Application.RunMode runMode) {
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
                                Tools.getApplication().scaled(12));
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
                                    new AccessMode(Application.AccessType.ADMIN,
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
                public void done(final String answer) {
                    ta.setText(answer);
                    Tools.invokeLater(new Runnable() {
                    @Override
                        public void run() {
                            crmConfigureShowButton.setEnabled(true);
                            hostInfoButton.setEnabled(true);
                            crmShowInProgress = false;
                        }
                    });
                }

                @Override
                public void doneError(final String answer, final int errorCode) {
                    ta.setText(answer);
                    LOG.sshError(host, "", answer, "", errorCode);
                    Tools.invokeLater(new Runnable() {
                    @Override
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
                                new AccessMode(Application.AccessType.GOD,
                                               false));
                crmInfo = true;
                hostInfoButton.setEnabled(false);
                crmConfigureCommitButton.setEnabled(false);
                String command = "HostBrowser.getHostInfo";
                if (!host.isCsInit()) {
                    command = "HostBrowser.getHostInfoHeartbeat";
                }
                host.execCommand(new ExecCommandConfig().commandString(command)
                                                        .execCallback(execCallback)
                                                        .silentCommand()
                                                        .silentOutput()
                                                        .sshCommandTimeout(Ssh.DEFAULT_COMMAND_TIMEOUT));
            }
        });
        host.registerEnableOnConnect(hostInfoButton);

        crmConfigureShowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                registerComponentEditAccessMode(ta, new AccessMode(Application.AccessType.ADMIN, false));
                updateAdvancedPanels();
                crmShowInProgress = true;
                crmInfo = false;
                crmConfigureShowButton.setEnabled(false);
                crmConfigureCommitButton.setEnabled(false);
                host.execCommand(new ExecCommandConfig().commandString("HostBrowser.getCrmConfigureShow")
                                                        .execCallback(execCallback)
                                                        .sshCommandTimeout(Ssh.DEFAULT_COMMAND_TIMEOUT));
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

            @Override
            public void changedUpdate(final DocumentEvent e) {
                update();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
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
                return !Tools.versionBeforePacemaker(host);
            }

            @Override
            public void mouseOut(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                crmg.stopTestAnimation((JComponent) component);
                component.setToolTipText("");
            }

            @Override
            public void mouseOver(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                component.setToolTipText(
                                        ClusterBrowser.STARTING_PTEST_TOOLTIP);
                component.setToolTipBackground(
                            Tools.getDefaultColor(
                                    "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                crmg.startTestAnimation((JComponent) component,
                                        startTestLatch);
                final Host dcHost =
                                  getBrowser().getClusterBrowser().getDCHost();
                getBrowser().getClusterBrowser().ptestLockAcquire();
                try {
                    final ClusterStatus clStatus =
                            getBrowser().getClusterBrowser().getClusterStatus();
                    clStatus.setPtestData(null);
                    CRM.crmConfigureCommit(host, ta.getText(), Application.RunMode.TEST);
                    final PtestData ptestData =
                                           new PtestData(CRM.getPtest(dcHost));
                    component.setToolTipText(ptestData.getToolTip());
                    clStatus.setPtestData(ptestData);
                } finally {
                    getBrowser().getClusterBrowser().ptestLockRelease();
                }
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
                            try {
                                CRM.crmConfigureCommit(host,
                                                       ta.getText(),
                                                       Application.RunMode.LIVE);
                            } finally {
                                getBrowser().getClusterBrowser()
                                                    .clStatusUnlock();
                            }
                        }
                    }
                );
                thread.start();
            }
        });

        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(HostBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        mainPanel.add(buttonPanel);

        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.LINE_END);
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
        host.execCommand(new ExecCommandConfig().commandString(command)
                                                .execCallback(execCallback)
                                                .silentCommand()
                                                .silentOutput()
                                                .sshCommandTimeout(Ssh.DEFAULT_COMMAND_TIMEOUT));
        return mainPanel;
    }

    /** Gets host. */
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
        final HostMenu hostMenu = new HostMenu(this);
        return hostMenu.getPulldownMenu();
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
    public Subtext[] getSubtextsForGraph(final Application.RunMode runMode) {
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
    public String getIconTextForGraph(final Application.RunMode runMode) {
        if (!getHost().isConnected()) {
            return Tools.getString("HostBrowser.Hb.NoInfoAvailable");
        }
        return null;
    }

    /** Returns whether this host is in stand by. */
    public boolean isStandby(final Application.RunMode runMode) {
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        return b != null && b.isStandby(host, runMode);
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
    public Subtext getRightCornerTextForGraph(final Application.RunMode runMode) {
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
            if (isStandby(runMode)) {
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

    @Override
    public String getValueForConfig() {
        return host.getName();
    }
}
