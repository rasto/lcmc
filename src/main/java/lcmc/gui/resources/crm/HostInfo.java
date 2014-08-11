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

import lcmc.model.*;
import lcmc.model.crm.ClusterStatus;
import lcmc.model.crm.PtestData;
import lcmc.model.ColorText;
import lcmc.gui.*;
import lcmc.gui.CrmGraph;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * This class holds info data for a host.
 * It shows host view, just like in the host tab.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class HostInfo extends Info {
    private static final Logger LOG = LoggerFactory.getLogger(HostInfo.class);
    static final ImageIcon HOST_STANDBY_ICON =
     Tools.createImageIcon(Tools.getDefault("CRMGraph.HostStandbyIcon"));
    static final ImageIcon HOST_STANDBY_OFF_ICON =
                                     Tools.createImageIcon(Tools.getDefault("CRMGraph.HostStandbyOffIcon"));
    static final ImageIcon HOST_STOP_COMM_LAYER_ICON =
                                     Tools.createImageIcon(Tools.getDefault("CRMGraph.HostStopCommLayerIcon"));
    static final ImageIcon HOST_START_COMM_LAYER_ICON =
                                     Tools.createImageIcon(Tools.getDefault("CRMGraph.HostStartCommLayerIcon"));
    private static final ColorText OFFLINE_COLOR_TEXT = new ColorText("offline", null, Color.BLUE);
    private static final ColorText PENDING_COLOR_TEXT = new ColorText("pending", null, Color.BLUE);
    private static final ColorText FENCED_COLOR_TEXT = new ColorText("fencing...", null, Color.RED);
    private static final ColorText CORO_STOPPED_COLOR_TEXT = new ColorText("stopped", null, Color.RED);
    private static final ColorText PCMK_STOPPED_COLOR_TEXT = new ColorText("pcmk stopped", null, Color.RED);
    private static final ColorText UNKNOWN_COLOR_TEXT = new ColorText("wait...", null, Color.BLUE);
    private static final ColorText ONLINE_COLOR_TEXT = new ColorText("online", null, Color.BLUE);
    private static final ColorText STANDBY_COLOR_TEXT = new ColorText("STANDBY", null, Color.RED);
    private static final ColorText STOPPING_COLOR_TEXT = new ColorText("stopping...", null, Color.RED);
    private static final ColorText STARTING_COLOR_TEXT = new ColorText("starting...", null, Color.BLUE);

    static final String NO_PCMK_STATUS_STRING = "cluster status is not available";
    private Host host;
    private volatile boolean crmInfoShowing = false;
    private volatile boolean crmShowInProgress = true;
    @Autowired
    private HostMenu hostMenu;

    public void init(final Host host, final Browser browser) {
        super.init(host.getName(), browser);
        this.host = host;
    }

    @Override
    public HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        final Cluster cl = host.getCluster();
        if (cl != null) {
            return HostBrowser.HOST_IN_CLUSTER_ICON_RIGHT_SMALL;
        }
        return HostBrowser.HOST_ICON;
    }

    @Override
    public String getId() {
        return host.getName();
    }

    @Override
    public ImageIcon getCategoryIcon(final Application.RunMode runMode) {
        return HostBrowser.HOST_ICON;
    }

    @Override
    public String getToolTipForGraph(final Application.RunMode runMode) {
        return getBrowser().getHostToolTip(host);
    }

    @Override
    public JComponent getInfoPanel() {
        if (getBrowser().getClusterBrowser() == null) {
            return new JPanel();
        }
        final Font f = new Font("Monospaced", Font.PLAIN, Tools.getApplication().scaled(12));
        crmShowInProgress = true;
        final JTextArea ta = new JTextArea(Tools.getString("HostInfo.crmShellLoading"));
        ta.setEditable(false);
        ta.setFont(f);

        final MyButton crmConfigureCommitButton =
                                new MyButton(Tools.getString("HostInfo.crmShellCommitButton"), Browser.APPLY_ICON);
        registerComponentEnableAccessMode(crmConfigureCommitButton,
                                          new AccessMode(Application.AccessType.ADMIN, false));
        final MyButton hostInfoButton = new MyButton(Tools.getString("HostInfo.crmShellStatusButton"));
        hostInfoButton.miniButton();

        final MyButton crmConfigureShowButton = new MyButton(Tools.getString("HostInfo.crmShellShowButton"));
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
                registerComponentEditAccessMode(ta, new AccessMode(Application.AccessType.GOD, false));
                crmInfoShowing = true;
                hostInfoButton.setEnabled(false);
                crmConfigureCommitButton.setEnabled(false);
                String command = "HostBrowser.getHostInfo";
                if (!host.hasCorosyncInitScript()) {
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
                crmInfoShowing = false;
                crmConfigureShowButton.setEnabled(false);
                crmConfigureCommitButton.setEnabled(false);
                host.execCommand(new ExecCommandConfig().commandString("HostBrowser.getCrmConfigureShow")
                                                        .execCallback(execCallback)
                                                        .sshCommandTimeout(Ssh.DEFAULT_COMMAND_TIMEOUT));
            }
        });
        final CrmGraph crmg = getBrowser().getClusterBrowser().getCrmGraph();
        final Document taDocument = ta.getDocument();
        taDocument.addDocumentListener(new DocumentListener() {
            private void update() {
                if (!crmShowInProgress && !crmInfoShowing) {
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
                component.setToolTipText(ClusterBrowser.STARTING_PTEST_TOOLTIP);
                component.setToolTipBackground(Tools.getDefaultColor("ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                crmg.startTestAnimation((JComponent) component, startTestLatch);
                final Host dcHost = getBrowser().getClusterBrowser().getDCHost();
                getBrowser().getClusterBrowser().ptestLockAcquire();
                try {
                    final ClusterStatus clStatus = getBrowser().getClusterBrowser().getClusterStatus();
                    clStatus.setPtestResult(null);
                    CRM.crmConfigureCommit(host, ta.getText(), Application.RunMode.TEST);
                    final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
                    component.setToolTipText(ptestData.getToolTip());
                    clStatus.setPtestResult(ptestData);
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
                                CRM.crmConfigureCommit(host, ta.getText(), Application.RunMode.LIVE);
                            } finally {
                                getBrowser().getClusterBrowser().clStatusUnlock();
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
        mainPanel.setMinimumSize(new Dimension(Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                                               Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        mainPanel.setPreferredSize(new Dimension(Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                                                 Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        buttonPanel.add(p);
        mainPanel.add(new JLabel(Tools.getString("HostInfo.crmShellInfo")));
        mainPanel.add(new JScrollPane(ta));
        String command = "HostBrowser.getHostInfo";
        if (!host.hasCorosyncInitScript()) {
            command = "HostBrowser.getHostInfoHeartbeat";
        }
        host.execCommand(new ExecCommandConfig().commandString(command)
                                                .execCallback(execCallback)
                                                .silentCommand()
                                                .silentOutput()
                                                .sshCommandTimeout(Ssh.DEFAULT_COMMAND_TIMEOUT));
        return mainPanel;
    }

    public Host getHost() {
        return host;
    }

    @Override
    public String toString() {
        return host.getName();
    }

    @Override
    public String getName() {
        return host.getName();
    }

    @Override
    public List<UpdatableItem> createPopup() {
        return hostMenu.getPulldownMenu(this);
    }

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

    public ColorText[] getSubtextsForGraph(final Application.RunMode runMode) {
        final List<ColorText> texts = new ArrayList<ColorText>();
        if (getHost().isConnected()) {
            if (!getHost().isCrmStatusOk()) {
               texts.add(new ColorText("waiting for Pacemaker...", null, Color.BLACK));
            }
        } else {
            texts.add(new ColorText("connecting...", null, Color.BLACK));
        }
        return texts.toArray(new ColorText[texts.size()]);
    }

    public String getIconTextForGraph(final Application.RunMode runMode) {
        if (!getHost().isConnected()) {
            return Tools.getString("HostBrowser.Hb.NoInfoAvailable");
        }
        return null;
    }

    public boolean isStandby(final Application.RunMode runMode) {
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        return b != null && b.isStandby(host, runMode);
    }

    ClusterStatus getClusterStatus() {
        final ClusterBrowser b = getBrowser().getClusterBrowser();
        if (b == null) {
            return null;
        }
        return b.getClusterStatus();
    }

    public ColorText getRightCornerTextForGraph(final Application.RunMode runMode) {
        if (getHost().isCommLayerStopping()) {
            return STOPPING_COLOR_TEXT;
        } else if (getHost().isCommLayerStarting()) {
            return STARTING_COLOR_TEXT;
        } else if (getHost().isPacemakerStarting()) {
            return STARTING_COLOR_TEXT;
        }
        final ClusterStatus cs = getClusterStatus();
        if (cs != null && cs.isFencedNode(host.getName())) {
            return FENCED_COLOR_TEXT;
        } else if (getHost().isCrmStatusOk()) {
            if (isStandby(runMode)) {
                return STANDBY_COLOR_TEXT;
            } else {
                return ONLINE_COLOR_TEXT;
            }
        } else if (getHost().isConnected()) {
            final Boolean running = getHost().getCorosyncOrHeartbeatRunning();
            if (running == null) {
                return UNKNOWN_COLOR_TEXT;
            } else if (!running) {
                return CORO_STOPPED_COLOR_TEXT;
            }
            if (cs != null && cs.isPendingNode(host.getName())) {
                return PENDING_COLOR_TEXT;
            } else if (!getHost().isPacemakerRunning()) {
                return PCMK_STOPPED_COLOR_TEXT;
            } else if (cs != null && "no".equals(cs.isOnlineNode(host.getName()))) {
                return OFFLINE_COLOR_TEXT;
            } else {
                return UNKNOWN_COLOR_TEXT;
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
