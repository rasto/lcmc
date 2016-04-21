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

package lcmc.host.domain;

import lcmc.Exceptions;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.ExecCommandThread;
import lcmc.cluster.service.ssh.Ssh;
import lcmc.cluster.service.ssh.SshOutput;
import lcmc.cluster.service.storage.BlockDeviceService;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.SSHGui;
import lcmc.common.domain.Application;
import lcmc.common.domain.ConnectionCallback;
import lcmc.common.domain.ConvertCmdCallback;
import lcmc.common.domain.ExecCallback;
import lcmc.common.domain.NewOutputCallback;
import lcmc.common.domain.Unit;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.configs.DistResource;
import lcmc.drbd.domain.BlockDevice;
import lcmc.drbd.domain.DrbdHost;
import lcmc.drbd.domain.DrbdXml;
import lcmc.drbd.service.DRBD;
import lcmc.host.ui.HostBrowser;
import lcmc.host.ui.TerminalPanel;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.robotest.RoboTest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class holds host data and implementation of host related methods.
 */
@RequiredArgsConstructor
public class Host implements Comparable<Host>, Value {
    private final DrbdHost drbdHost;
    private final TerminalPanel terminalPanel;
    private final MainData mainData;
    private final ProgressIndicator progressIndicator;
    private final Ssh ssh;
    private final HostBrowser hostBrowser;
    private final Hosts allHosts;
    private final Application application;
    private final RoboTest roboTest;
    private final BlockDeviceService blockDeviceService;
    private final SwingUtils swingUtils;
    @Getter
    @Setter
    private HostParser hostParser; //TODO cycle

    private static final Logger LOG = LoggerFactory.getLogger(Host.class);
    public static final String NOT_CONNECTED_MENU_TOOLTIP_TEXT = "not connected to the host";
    public static final String PROXY_NOT_CONNECTED_MENU_TOOLTIP_TEXT = "not connected to the proxy host";
    /** Timeout after which the connection is considered to be dead. */
    private static final int PING_TIMEOUT = 40000;
    private static final int DRBD_EVENTS_TIMEOUT = 40000;
    private static final int CLUSTER_EVENTS_TIMEOUT = 40000;

    public static final String DEFAULT_HOSTNAME = "unknown";

    public static final String VM_FILESYSTEM_SOURCE_DIR_LXC = "vm.filesystem.source.dir.lxc";

    public static final String ROOT_USER = "root";
    public static final String DEFAULT_SSH_PORT = "22";

    public static final boolean UPDATE_LVM = true;

    private String name;
    private String enteredHostOrIp = Tools.getDefault("SSH.Host");
    private String ipAddress;
    /** Ips in the combo in Dialog.Host.Configuration. */
    private final Map<Integer, String[]> allIps = new HashMap<Integer, String[]>();
    private Cluster cluster = null;
    private String hostname = DEFAULT_HOSTNAME;
    private String username = null;
    private Color defaultHostColorInGraph;
    private Color savedHostColorInGraphs;
    private ExecCommandThread drbdStatusThread = null;
    private ExecCommandThread crmStatusThread = null;
    private String sshPort = null;
    private Boolean useSudo = null;
    private String sudoPassword = "";
    /** A gate that is used to synchronize the loading sequence. */
    private CountDownLatch isLoadingGate;
    private final Collection<JComponent> enableOnConnectElements = new ArrayList<JComponent>();
    private String pacemakerInstallMethodIndex;
    private String heartbeatPacemakerInstallMethodIndex;
    private String vmInfoFromServerMD5 = null;
    private int positionInTheCluster = 0;
    private volatile boolean lastConnectionCheckPositive = false;
    private boolean savable = true;
    /** Ping is set every 10s. */
    private volatile AtomicBoolean ping = new AtomicBoolean(true);
    private boolean inCluster = false;

    private boolean crmStatusOk = false;

    public void init() {
        if (allHosts.size() == 1) {
            enteredHostOrIp = Tools.getDefault("SSH.SecHost");
        }
    }

    public HostBrowser getBrowser() {
        return hostBrowser;
    }

    /**
     * Sets cluster in which this host is in. Set null,
     * if it is removed from the cluster. One host can be
     * only in one cluster.
     */
    public void setCluster(final Cluster cluster) {
        this.cluster = cluster;
        if (cluster == null) {
            LOG.debug1("setCluster: " + getName() + " set cluster: null");
        } else {
            inCluster = true;
            LOG.debug1("setCluster: " + getName() + " set cluster name: " + cluster.getName());
        }
    }

    public void removeFromCluster() {
        inCluster = false;
    }

    public Cluster getCluster() {
        return cluster;
    }

    /** Returns color objects of this host for drbd graph. */
    public Color[] getDrbdColors() {
        if (defaultHostColorInGraph == null) {
            defaultHostColorInGraph = Tools.getDefaultColor("Host.DefaultColor");
        }
        final Color col;
        if (savedHostColorInGraphs == null) {
            col = defaultHostColorInGraph;
        } else {
            col = savedHostColorInGraphs;
        }
        final Color secColor;
        if (isConnected()) {
            if (hostParser.isDrbdStatusOk() && drbdHost.isDrbdLoaded()) {
                return new Color[]{col};
            } else {
                secColor = Tools.getDefaultColor("Host.NoStatusColor");
            }
        } else {
            secColor = Tools.getDefaultColor("Host.ErrorColor");
        }
        return new Color[]{col, secColor};
    }


    /** Returns color objects of this host. */
    public Color[] getPmColors() {
        if (defaultHostColorInGraph == null) {
            defaultHostColorInGraph = Tools.getDefaultColor("Host.DefaultColor");
        }
        final Color col;
        if (savedHostColorInGraphs == null) {
            col = defaultHostColorInGraph;
        } else {
            col = savedHostColorInGraphs;
        }
        final Color secColor;
        if (isConnected()) {
            if (isCrmStatusOk()) {
                return new Color[]{col};
            } else {
                secColor = Tools.getDefaultColor("Host.NoStatusColor");
            }
        } else {
            secColor = Tools.getDefaultColor("Host.ErrorColor");
        }
        return new Color[]{col, secColor};
    }

    public void setColor(final Color defaultColor) {
        this.defaultHostColorInGraph = defaultColor;
        if (savedHostColorInGraphs == null) {
            savedHostColorInGraphs = defaultColor;
        }
        terminalPanel.resetPromptColor();
    }

    public void setSavedHostColorInGraphs(final Color savedHostColorInGraphs) {
        this.savedHostColorInGraphs = savedHostColorInGraphs;
        terminalPanel.resetPromptColor();
    }

    public boolean isInCluster() {
        return inCluster;
    }

    /**
     * Returns true when this host is in a cluster and is different than the
     * specified cluster.
     */
    public boolean isInCluster(final Cluster otherCluster) {
        return isInCluster() && !cluster.equals(otherCluster);
    }

    /**
     * Sets hostname as entered by user, this can be also ipAddress. If
     * hostnameEntered changed, it reinitilizes the name.
     */
    public void setEnteredHostOrIp(final String enteredHostOrIp) {
        if (enteredHostOrIp != null
            && !enteredHostOrIp.equals(this.enteredHostOrIp)) {
            /* back button and hostname changed */
            setName(null);
            setIpAddress(null);
            setHostname(null);
        }
        this.enteredHostOrIp = enteredHostOrIp;
    }

    /** Sets hostname of the host. */
    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    /**
     * Sets user name for the host. This username is used to connect
     * to the host. The default is "root". If username changed disconnect
     * the old connection.
     */
    public void setUsername(final String username) {
        if (this.username != null && !username.equals(this.username)) {
            ssh.disconnect();
        }
        this.username = username;
    }

    /**
     * Sets ipAddress. If ipAddress has changed, disconnect the
     * old connection.
     */
    public void setIpAddress(final String ipAddress) {
        if (ipAddress != null) {
            if (this.ipAddress != null && !ipAddress.equals(this.ipAddress)) {
                ssh.disconnect();
            }
        } else if (this.ipAddress != null) {
            ssh.disconnect();
        }
        this.ipAddress = ipAddress;
    }

    public void setIps(final int hop, final String[] ipsForHop) {
        allIps.put(hop, ipsForHop);
    }

    public String[] getIps(final int hop) {
        return allIps.get(hop);
    }

    public void disconnect() {
        if (ssh.isConnected()) {
            ssh.forceDisconnect();
        }
        setVMInfoMD5(null);
    }

    /**
     * Executes command. Command is executed in a new thread, after command
     * is finished execCallback.done function will be called. In case of error,
     * callback.doneError is called.
     */
    public ExecCommandThread execCommand(final ExecCommandConfig execCommandConfig) {
        return ssh.execCommand(execCommandConfig);

    }

    public SshOutput captureCommand(final ExecCommandConfig execCommandConfig) {
        return ssh.captureCommand(execCommandConfig);
    }

    public SshOutput captureCommandProgressIndicator(final String text, final ExecCommandConfig execCommandConfig) {
        final String hostName = getName();
        progressIndicator.startProgressIndicator(hostName, text);
        try {
            return ssh.captureCommand(execCommandConfig);
        } finally {
            progressIndicator.stopProgressIndicator(hostName, text);
        }
    }

    public void execCommandProgressIndicator(final String text, final ExecCommandConfig execCommandConfig) {
        final String hostName = getName();
        progressIndicator.startProgressIndicator(hostName, text);
        try {
            ssh.execCommand(execCommandConfig);
        } finally {
            progressIndicator.stopProgressIndicator(hostName, text);
        }
    }

    /**
     * Executes command with bash -c. Command is executed in a new thread,
     * after command * is finished callback.done function will be called.
     * In case of error, callback.doneError is called.
     */
    public ExecCommandThread execCommandInBash(ExecCommandConfig execCommandConfig) {
        return ssh.execCommand(execCommandConfig.inBash(true).inSudo(true));
    }

    /**
     * Executes get status command which runs in the background and updates the
     * block device object. The command is 'drbdsetup /dev/drbdX events'
     * The session is stored, so that in can be stopped with 'stop' button.
     */
    public void execDrbdStatusCommand(final ExecCallback execCallback, final NewOutputCallback outputCallback) {
        if (drbdStatusThread == null) {
            drbdStatusThread = ssh.execCommand(new ExecCommandConfig()
                                                   .commandString("DRBD.getDrbdStatus")
                                                   .inBash(false)
                                                   .inSudo(false)
                                                   .execCallback(execCallback)
                                                   .newOutputCallback(outputCallback)
                                                   .silentCommand()
                                                   .silentOutput()
                                                   .sshCommandTimeout(DRBD_EVENTS_TIMEOUT));
        } else {
            LOG.appWarning("execDrbdStatusCommand: trying to start started drbd status");
        }
    }

    /** Stops drbd status background process. */
    public void stopDrbdStatus() {
        final ExecCommandThread dst = drbdStatusThread;
        if (dst == null) {
            LOG.appWarning("execDrbdStatusCommand: trying to stop stopped drbd status");
            return;
        }
        dst.cancelTheSession();
        drbdStatusThread = null;
    }

    public void waitForDrbdStatusFinish() {
        final ExecCommandThread dst = drbdStatusThread;
        if (dst != null) {
            try {
                /* it probably hangs after this timeout, so it will be
                 * killed. */
                dst.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            stopDrbdStatus();
        }
    }

    public void execCrmStatusCommand(final ExecCallback execCallback,
                                     final NewOutputCallback outputCallback) {
        if (crmStatusThread == null) {
            crmStatusThread = ssh.execCommand(new ExecCommandConfig()
                                                 .commandString("Heartbeat.getClStatus")
                                                 .inBash(false)
                                                 .inSudo(false)
                                                 .execCallback(execCallback)
                                                 .newOutputCallback(outputCallback)
                                                 .silentCommand()
                                                 .silentOutput()
                                                 .sshCommandTimeout(CLUSTER_EVENTS_TIMEOUT));
        } else {
            LOG.appWarning("execClStatusCommand: trying to start started status");
        }
    }

    public void waitForCrmStatusFinish() {
        final ExecCommandThread cst = crmStatusThread;
        if (cst == null) {
            return;
        }
        try {
            cst.join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        crmStatusThread = null;
    }

    public void stopCrmStatus() {
        final ExecCommandThread cst = crmStatusThread;
        if (cst == null) {
            LOG.appWarning("stopClStatus: trying to stop stopped status");
            return;
        }
        cst.cancelTheSession();
    }

    /** Gets ipAddress. There can be more ips, delimited with "," */
    public String getIpAddress() {
        return ipAddress;
    }

    /** Returns the ipAddress for the hop. */
    public String getIp(final int hop) {
        if (ipAddress == null) {
            return null;
        }
        final String[] ipsA = ipAddress.split(",");
        if (ipsA.length < hop + 1) {
            return null;
        }
        return ipsA[hop];
    }

    /** Return first hop ipAddress. */
    public String getFirstIp() {
        if (ipAddress == null) {
            return null;
        }
        final String[] ipsA = ipAddress.split(",");
        return ipsA[0];
    }

    public String getUsername() {
        return username;
    }

    /** Returns first username in a hop. */
    public String getFirstUsername() {
        final String[] usernames = username.split(",");
        return usernames[0];
    }

    public String getEnteredHostOrIp() {
        return enteredHostOrIp;
    }

    String getSudoPrefix(final boolean sudoTest) {
        if (useSudo != null && useSudo) {
            if (sudoTest) {
                return "sudo -E -n ";
            } else {
                return "sudo -E -p '" + Ssh.SUDO_PROMPT + "' ";
            }
        } else {
            return "";
        }
    }
    /** Returns command exclosed in sh -c "". */
    public String getSudoCommand(final String command, final boolean sudoTest) {
        if (useSudo != null && useSudo) {
            final String sudoPrefix = getSudoPrefix(sudoTest);
            return command.replaceAll(DistResource.SUDO, sudoPrefix);
        } else {
            return command.replaceAll(DistResource.SUDO, " "); /* must be " " */
        }
    }

    /**
     * Returns command with all the sshs that will be hopped.
     *
     * ssh -A   -tt -l root x.x.x.x "ssh -A   -tt -l root x.x.x.x \"ssh
     * -A   -tt -l root x.x.x.x \\\"ls\\\"\""
     */
    public String getHoppedCommand(final String command) {
        final int hops = Tools.charCount(ipAddress, ',') + 1;
        final String[] usernames = username.split(",");
        final String[] ipsA = ipAddress.split(",");
        final StringBuilder s = new StringBuilder(200);
        if (hops > 1) {
            String sshAgentPid = "";
            String sshAgentSock = "";
            final Map<String, String> variables = System.getenv();
            for (final String var : variables.keySet()) {
                final String value = variables.get(var);
                if ("SSH_AGENT_PID".equals(var)) {
                    sshAgentPid = value;
                } else if ("SSH_AUTH_SOCK".equals(var)) {
                    sshAgentSock = value;
                }
            }

            s.append("SSH_AGENT_PID=");
            s.append(sshAgentPid);
            s.append(" SSH_AUTH_SOCK=");
            s.append(sshAgentSock);
            s.append(' ');
        }
        for (int i = 1; i < hops; i++) {
            s.append("ssh -q -A -tt -o 'StrictHostKeyChecking no' -o 'ForwardAgent yes' -l ");
            if (i < usernames.length) {
                s.append(usernames[i]);
            } else {
                s.append(ROOT_USER);
            }
            s.append(' ');
            s.append(ipsA[i]);
            s.append(' ');
            s.append(Tools.escapeQuotes("\"", i - 1));
        }

        s.append(Tools.escapeQuotes(command, hops - 1));

        for (int i = hops - 1; i > 0; i--) {
            s.append(Tools.escapeQuotes("\"", i - 1));
        }
        return s.toString();
    }

    public String getHostname() {
        return hostname;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Gets name, that is shown in the tab. Name is either host name, if it is
     * set or ipAddress.
     */
    public String getName() {
        if (name == null) {
            final String nodeName;
            if (hostname != null) {
                final int i = hostname.indexOf(',');
                if (i > 0) {
                    nodeName = hostname.substring(i + 1);
                } else {
                    nodeName = hostname;
                }
            } else if (enteredHostOrIp != null) {
                final int i = enteredHostOrIp.indexOf(',');
                if (i > 0) {
                    nodeName = enteredHostOrIp.substring(i + 1);
                } else {
                    nodeName = enteredHostOrIp;
                }
            } else {
                return ipAddress;
            }
            return nodeName;
        } else {
            return name;
        }
    }

    /** Sets name of the host as it will be identified. */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets string with user and hostname as used in prompt or ssh like
     * rasto@linbit.at.
     */
    public String getUserAtHost() {
        return username + '@' + getHostname();
    }

    public Ssh getSSH() {
        return ssh;
    }

    public TerminalPanel getTerminalPanel() {
        return terminalPanel;
    }

    /**
     * Connects host with ssh. Dialog is needed, in case if password etc.
     * has to be entered. Connection is made in the background, after
     * connection is established, callback.done() is called. In case
     * of error callback.doneError() is called.
     */
    public void connect(SSHGui sshGui, final ConnectionCallback callback) {
        if (sshGui == null) {
            sshGui = new SSHGui(mainData.getMainFrame(), this, null);
        }
        ssh.connect(sshGui, callback, this);
    }

    /**
     * Connects host with ssh. Dialog is needed, in case if password etc.
     * has to be entered. Connection is made in the background, after
     * connection is established, callback.done() is called. In case
     * of error callback.doneError() is called.
     *
     * @param sshGui
     *          ssh gui dialog
     *
     * @param progressBar
     *          progress bar that is used to show progress through connecting
     *
     * @param callback
     *          callback class that implements ConnectionCallback interface
     */
    public void connect(final SSHGui sshGui, final ProgressBar progressBar, final ConnectionCallback callback) {
        LOG.debug1("connect: host: " + sshGui);
        ssh.connect(sshGui, progressBar, callback, this);
    }

    /**
     * Register a component that will be enabled if the host connected and
     * disabled if disconnected.
     */
    public void registerEnableOnConnect(final JComponent c) {
        if (!enableOnConnectElements.contains(c)) {
            enableOnConnectElements.add(c);
        }
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                c.setEnabled(isConnected());
            }
        });
    }

    /**
     * Is called after the host is connected or disconnected and
     * enables/disables the conponents that are registered to be enabled on
     * connect.
     */
    public void setConnected() {
        final boolean con = isConnected();
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final JComponent c : enableOnConnectElements) {
                    c.setEnabled(con);
                }
            }
        });
        if (lastConnectionCheckPositive != con) {
            lastConnectionCheckPositive = con;
            if (con) {
               LOG.info("setConnected: " + getName() + ": connection established");
            } else {
               LOG.info("setConnected: " + getName() + ": connection lost");
            }
            final ClusterBrowser cb = getBrowser().getClusterBrowser();
            if (cb != null) {
                cb.getCrmGraph().repaint();
                cb.getDrbdGraph().repaint();
            }
        }
    }

    /** Make an ssh connection to the host. */
    public void connect(SSHGui sshGui, final boolean useProgressIndicator, final int index) {
        if (!isConnected()) {
            final String hostName = getName();
            if (useProgressIndicator) {
                progressIndicator.startProgressIndicator(hostName,
                        Tools.getString("Dialog.Host.SSH.Connecting") + " (" + index + ')');
            }
            if (sshGui == null) {
                sshGui = new SSHGui(mainData.getMainFrame(), this, null);
            }

            connect(sshGui,
                    new ConnectionCallback() {
                        @Override
                        public void done(final int flag) {
                            setConnected();
                            getSSH().execCommandAndWait(new ExecCommandConfig()
                                                            .command(":") /* activate sudo */
                                                            .silentCommand()
                                                            .silentOutput()
                                                            .sshCommandTimeout(10000));
                                    getSSH().installGuiHelper();
                            hostParser.getAllInfo();
                            if (useProgressIndicator) {
                                progressIndicator.stopProgressIndicator(
                                        hostName,
                                        Tools.getString("Dialog.Host.SSH.Connecting") + " (" + index + ')');
                            }
                        }

                        @Override
                        public void doneError(final String errorText) {
                            setLoadingError();
                            setConnected();
                            if (useProgressIndicator) {
                                progressIndicator.stopProgressIndicator(
                                        hostName,
                                        Tools.getString("Dialog.Host.SSH.Connecting") + " (" + index + ')');
                                progressIndicator.progressIndicatorFailed(
                                        hostName,
                                        Tools.getString("Dialog.Host.SSH.Connecting") + " (" + index + ')');
                                progressIndicator.stopProgressIndicator(
                                        hostName,
                                        Tools.getString("Dialog.Host.SSH.Connecting") + " (" + index + ')');
                            }
                        }
                    });
        }
    }

    public void startPing() {
        ssh.execCommand(new ExecCommandConfig()
                         .commandString("PingCommand")
                         .inBash(true)
                         .inSudo(false)
                         .execCallback(new ExecCallback() {
                             @Override
                             public void done(final String ans) {
                             }

                             @Override
                             public void doneError(final String ans, final int exitCode) {
                             }
                         })
                         .newOutputCallback(new NewOutputCallback() {
                             @Override
                             public void output(final String output) {
                                 ping.set(true);
                             }
                         })
                         .silentCommand()
                         .silentOutput()
                         .sshCommandTimeout(PING_TIMEOUT)).block();
    }

    public void startConnectionStatus() {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (ping.get()) {
                       LOG.debug2("startConnectionStatus: connection ok on " + getName());
                       setConnected();
                       ping.set(false);
                    } else {
                       LOG.debug2("startConnectionStatus: connection lost on " + getName());
                       getSSH().forceReconnect();
                       setConnected();
                    }
                    Tools.sleep(PING_TIMEOUT);
                    final ClusterBrowser cb = getBrowser().getClusterBrowser();
                    /* cluster could be removed */
                    if (cb == null || cb.isCancelServerStatus()) {
                        break;
                    }
                }
            }
        });
        thread.start();
    }

    /** Returns whether host ssh connection was established. */
    public boolean isConnected() {
        if (ssh == null) {
            return false;
        }
        return ssh.isConnected();
    }


    public void saveGraphPositions(final Map<String, Point2D> positions) {
        final StringBuilder lines = new StringBuilder();
        for (final String id : positions.keySet()) {
            final Point2D p = positions.get(id);
            double x = p.getX();
            if (x < 0) {
                x = 0;
            }
            double y = p.getY();
            if (y < 0) {
                y = 0;
            }
            lines.append(id).append(";x=").append(x).append(";y=").append(y).append('\n');
        }
        getSSH().createConfig(lines.toString(),
                              "drbdgui.cf",
                              "/var/lib/heartbeat/",
                              "0600",
                              false,
                              null,
                              null);
    }

    /**
     * Sets the 'is loading' latch, so that something can wait while the load
     * sequence is running.
     */
    public void setIsLoading() {
        isLoadingGate = new CountDownLatch(1);
    }

    /** Waits on the 'is loading' latch. */
    public void waitOnLoading() {
        if (isLoadingGate == null) {
            return;
        }
        try {
            isLoadingGate.await();
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * When loading is done, this latch is opened and whatever is waiting on it
     * is notified.
     */
    public void setLoadingDone() {
        isLoadingGate.countDown();
    }

    /**
     * When loading is done but with an error. Currently it is the same as
     * setLoadingDone().
     */
    void setLoadingError() {
        isLoadingGate.countDown();
    }

    public String getSSHPort() {
        return sshPort;
    }

    public int getSSHPortInt() {
        return Integer.valueOf(sshPort);
    }

    /** Sets ssh port. */
    public void setSSHPort(final String sshPort) {
        if (this.sshPort != null && !sshPort.equals(this.sshPort)) {
            ssh.disconnect();
        }
        this.sshPort = sshPort;
    }

    public String getSudoPassword() {
        return sudoPassword;
    }

    public void setSudoPassword(final String sudoPassword) {
        this.sudoPassword = sudoPassword;
    }

    public Boolean isUseSudo() {
        return useSudo;
    }

    public void setUseSudo(final Boolean useSudo) {
        this.useSudo = useSudo;
    }

    public void setPacemakerInstallMethodIndex(final String pacemakerInstallMethodIndex) {
        this.pacemakerInstallMethodIndex = pacemakerInstallMethodIndex;
    }

    public String getPacemakerInstallMethodIndex() {
        return pacemakerInstallMethodIndex;
    }

    public void setHeartbeatPacemakerInstallMethodIndex(final String heartbeatPacemakerInstallMethodIndex) {
        this.heartbeatPacemakerInstallMethodIndex = heartbeatPacemakerInstallMethodIndex;
    }

    public String getHeartbeatPacemakerInstallMethodIndex() {
        return heartbeatPacemakerInstallMethodIndex;
    }

    /** Returns MD5 checksum of VM Info from server. */
    public String getVMInfoMD5() {
        return vmInfoFromServerMD5;
    }

    /** Sets MD5 checksum of VM Info from server. */
    public void setVMInfoMD5(final String vmInfoMD5) {
        this.vmInfoFromServerMD5 = vmInfoMD5;
    }

    public void setPositionInTheCluster(final int positionInTheCluster) {
        this.positionInTheCluster = positionInTheCluster;
    }

    public int getPositionInTheCluster() {
        return positionInTheCluster;
    }

    /** This is part of testsuite. */
    boolean checkTest(final String checkCommand,
                      final String test,
                      final double index,
                      final String name,
                      final int maxHosts) {
        Tools.sleep(1500);
        final StringBuilder command = new StringBuilder(50);
        command.append(DistResource.SUDO).append(hostParser.replaceVars("@GUI-HELPER@"));
        command.append(' ');
        command.append(checkCommand);
        command.append(' ');
        command.append(test);
        command.append(' ');
        final String indexString = Double.toString(index).replaceFirst("\\.0+$", "");
        command.append(indexString);
        if (name != null) {
            command.append(' ');
            command.append(name);
        }
        int h = 1;
        for (final Host host : getCluster().getHosts()) {
            LOG.debug1("checkTest: host" + h + " = " + host.getName());
            command.append(' ');
            command.append(host.getName());
            if (maxHosts > 0 && h >= maxHosts) {
                break;
            }
            h++;
        }
        command.append(" 2>&1");
        int i = 0;
        SshOutput out;
        do {
            out = getSSH().execCommandAndWait(new ExecCommandConfig().command(command.toString())
                                                                     .sshCommandTimeout(60000));
            if (out.getExitCode() == 0 || out.getExitCode() == 10) {
                break;
            }
            i++;
            roboTest.sleepNoFactor(i * 2000);
        } while (i < 5);
        String nameS = ' ' + name;
        if (name == null) {
            nameS = "";
        }
        if (i > 0) {
            roboTest.info(getName() + ' ' + test + ' ' + index + nameS + " tries: " + (i + 1));
        }
        roboTest.info(getName() + ' ' + test + ' ' + index + nameS + ' ' + out.getOutput());
        return out.getExitCode() == 0;
    }

    /** This is part of testsuite, it checks Pacemaker. */
    public boolean checkPCMKTest(final String test, final double index) {
        return checkTest("gui-test", test, index, null, 0);
    }

    /** This is part of testsuite, it checks DRBD. */
    public boolean checkDRBDTest(final String test, final double index) {
        final StringBuilder testName = new StringBuilder(20);
        if (application.getBigDRBDConf()) {
            testName.append("big-");
        }
        if (!hostParser.hasVolumes()) {
            testName.append("novolumes-");
        }
        testName.append(test);
        return checkTest("gui-drbd-test", testName.toString(), index, null, 2);
    }

    /** This is part of testsuite, it checks VMs. */
    public boolean checkVMTest(final String test, final double index, final String name) {
        return checkTest("gui-vm-test", test, index, name, 0);
    }

    /** Returns color of this host. Null if it is default color. */
    public String getColor() {
        if (savedHostColorInGraphs == null || defaultHostColorInGraph == savedHostColorInGraphs) {
            return null;
        }
        return Integer.toString(savedHostColorInGraphs.getRGB());
    }

    /** Sets color of this host. Don't if it is default color. */
    public void setSavedColor(final String colorString) {
        try {
            savedHostColorInGraphs = new Color(Integer.parseInt(colorString));
        } catch (final NumberFormatException e) {
            LOG.appWarning("setSavedColor: could not parse: " + colorString);
            /* ignore it */
        }
    }

    public void setSavable(final boolean savable) {
        this.savable = savable;
    }

    public boolean isSavable() {
        return savable;
    }

    public boolean isRoot() {
        return ROOT_USER.equals(username);
    }

    public void updateDrbdParameters() {
        final ClusterBrowser cb = getBrowser().getClusterBrowser();
        final DrbdXml drbdXml = cb.getDrbdXml();
        final String output = drbdXml.updateDrbdParameters(this);
        if (output != null) {
            drbdXml.parseDrbdParameters(this, output, cb.getClusterHosts());
            cb.getHostDrbdParameters().put(this, output);
        }
    }

    /** Compares ignoring case. */
    @Override
    public int compareTo(final Host h) {
        return Tools.compareNames(getName(), h.getName());
    }

    @Override
    public String getValueForGui() {
        return getName();
    }

    @Override
    public String getValueForConfig() {
        return getName();
    }

    @Override
    public boolean isNothingSelected() {
        return getName() == null;
    }

    @Override
    public Unit getUnit() {
        return null;
    }

    @Override
    public String getValueForConfigWithUnit() {
        return getValueForConfig();
    }

    @Override
    public String getNothingSelected() {
        return NOTHING_SELECTED;
    }

    public String isDrbdUtilCompatibleWithDrbdModule() {
        if (!DRBD.compatibleVersions(drbdHost.getDrbdUtilVersion(), drbdHost.getDrbdModuleVersion())) {
            return "DRBD util and module versions are not compatible: "
                    + drbdHost.getDrbdUtilVersion()
                    + " / "
                    + drbdHost.getDrbdModuleVersion();
        }
        return null;
    }

    public String getDrbdInfoAboutInstallation() {
        final StringBuilder tt = new StringBuilder(40);
        final String drbdV = drbdHost.getDrbdUtilVersion();
        final String drbdModuleV = drbdHost.getDrbdModuleVersion();
        final String drbdS;
        if (drbdV == null || drbdV.isEmpty()) {
            drbdS = "not installed";
        } else {
            drbdS = drbdV;
        }

        final String drbdModuleS;
        if (drbdModuleV == null || drbdModuleV.isEmpty()) {
            drbdModuleS = "not installed";
        } else {
            drbdModuleS = drbdModuleV;
        }
        tt.append("\nDRBD ");
        tt.append(drbdS);
        if (!drbdS.equals(drbdModuleS)) {
            tt.append("\nDRBD module ");
            tt.append(drbdModuleS);
        }
        if (drbdHost.isDrbdLoaded()) {
            tt.append(" (running)");
        } else {
            tt.append(" (not loaded)");
        }
        return tt.toString();
    }

    public void waitForHostAndDrbd() {
        while (!isConnected() || !drbdHost.isDrbdLoaded()) {
            try {
                Thread.sleep(10000);
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
    public boolean drbdVersionHigherOrEqual(final String drbdVersion) throws Exceptions.IllegalVersionException {
        return Tools.compareVersions(drbdHost.getDrbdUtilVersion(), drbdVersion) >= 0;
    }

    public boolean drbdVersionSmaller(final String drbdVersion) throws Exceptions.IllegalVersionException {
        return Tools.compareVersions(drbdHost.getDrbdUtilVersion(), drbdVersion) < 0;
    }

    public boolean isDrbdLoaded() {
        return drbdHost.isDrbdLoaded();
    }

    public boolean isDrbdProxyRunning() {
        return drbdHost.isDrbdProxyRunning();
    }

    public boolean hasDrbd() {
        return drbdHost.getDrbdUtilVersion() != null;
    }

    public boolean drbdVersionSmallerOrEqual(final String drbdVersion) throws Exceptions.IllegalVersionException {
        return Tools.compareVersions(drbdHost.getDrbdUtilVersion(), drbdVersion) <= 0;
    }

    public Collection<BlockDevice> getBlockDevices() {
        return blockDeviceService.getBlockDevices(this);
    }

    public void setCrmStatusOk(final boolean crmStatusOk) {
        this.crmStatusOk = crmStatusOk;
    }

    public boolean isCrmStatusOk() {
        return crmStatusOk && isConnected();
    }

    public String getDistString(String command) {
        return hostParser.getDistString(command);
    }

    /**
     * Returns info string about Pacemaker installation.
     */
    public String getPacemakerInfo() {
        final StringBuilder pacemakerInfo = new StringBuilder(40);
        final String pmV = hostParser.getPacemakerVersion();
        final String hbV = hostParser.getHeartbeatVersion();
        final StringBuilder hbRunning = new StringBuilder(20);
        if (hostParser.isHeartbeatRunning()) {
            hbRunning.append("running");
            if (!hostParser.isHeartbeatInRc()) {
                hbRunning.append("/no rc.d");
            }
        } else {
            hbRunning.append("not running");
        }
        if (hostParser.isHeartbeatInRc()) {
            hbRunning.append("/rc.d");
        }
        if (pmV == null) {
            if (hbV != null) {
                pacemakerInfo.append(" \nHeartbeat ");
                pacemakerInfo.append(hbV);
                pacemakerInfo.append(" (");
                pacemakerInfo.append(hbRunning);
                pacemakerInfo.append(')');
            }
        } else {
            final String pmRunning;
            if (isCrmStatusOk()) {
                pmRunning = "running";
            } else {
                pmRunning = "not running";
            }
            pacemakerInfo.append(" \nPacemaker ");
            pacemakerInfo.append(pmV);
            pacemakerInfo.append(" (");
            pacemakerInfo.append(pmRunning);
            pacemakerInfo.append(')');
            String corOrAis = null;
            final String corV = hostParser.getCorosyncVersion();
            final String aisV = hostParser.getOpenaisVersion();
            if (corV != null) {
                corOrAis = "Corosync " + corV;
            } else if (aisV != null) {
                corOrAis = "Openais " + aisV;
            }

            if (hbV != null && hostParser.isHeartbeatRunning()) {
                pacemakerInfo.append(" \nHeartbeat ");
                pacemakerInfo.append(hbV);
                pacemakerInfo.append(" (");
                pacemakerInfo.append(hbRunning);
                pacemakerInfo.append(')');
            }
            if (corOrAis != null) {
                pacemakerInfo.append(" \n");
                pacemakerInfo.append(corOrAis);
                pacemakerInfo.append(" (");
                if (hostParser.isCorosyncRunning()
                        || hostParser.isOpenaisRunning()) {
                    pacemakerInfo.append("running");
                    if (!hostParser.isCorosyncInRc() && !hostParser.isOpenaisInRc()) {
                        pacemakerInfo.append("/no rc.d");
                    }
                } else {
                    pacemakerInfo.append("not running");
                }
                if (hostParser.isCorosyncInRc() || hostParser.isOpenaisInRc()) {
                    pacemakerInfo.append("/rc.d");
                }
                pacemakerInfo.append(')');
            }
            if (hbV != null && !hostParser.isHeartbeatRunning()) {
                pacemakerInfo.append(" \nHeartbeat ");
                pacemakerInfo.append(hbV);
                pacemakerInfo.append(" (");
                pacemakerInfo.append(hbRunning);
                pacemakerInfo.append(')');
            }
        }
        return pacemakerInfo.toString();
    }

    public boolean hasVolumes() {
        return hostParser.hasVolumes();
    }

    public boolean isDrbdStatusOk() {
        return hostParser.isDrbdStatusOk();
    }

    public void setDrbdStatusOk(boolean status) {
        hostParser.setDrbdStatusOk(status);
    }

    public void drbdStatusLock() {
        hostParser.drbdStatusLock();
    }

    public void drbdStatusUnlock() {
        hostParser.drbdStatusUnlock();
    }

    public void vmStatusLock() {
        hostParser.vmStatusLock();
    }

    public void vmStatusUnlock() {
        hostParser.vmStatusUnlock();

    }

    public String getDistCommand(final String command, final ConvertCmdCallback convertCmdCallback) {
        return hostParser.getDistCommand(command, convertCmdCallback);
    }

    public String getDistCommand(final String command, final Map<String, String> resVolReplaceHash) {
        return hostParser.getDistCommand(command, resVolReplaceHash);
    }

    public String getArch() {
        return hostParser.getArch();
    }

    public Set<String> getAvailableCryptoModules() {
        return hostParser.getAvailableCryptoModules();
    }

    public String getHeartbeatLibPath() {
        return hostParser.getHeartbeatLibPath();
    }
}
