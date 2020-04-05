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

package lcmc.cluster.service.ssh;

import ch.ethz.ssh2.LocalPortForwarder;
import ch.ethz.ssh2.SCPClient;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lcmc.common.domain.util.GuiHelperFiles;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.MainPanel;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.configs.DistResource;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.common.ui.ProgressBar;
import lcmc.cluster.ui.SSHGui;
import lcmc.common.domain.ConnectionCallback;
import lcmc.common.domain.ExecCallback;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

@Named
public class Ssh {
    private static final Logger LOG = LoggerFactory.getLogger(Ssh.class);
    public static final int DEFAULT_COMMAND_TIMEOUT = Tools.getDefaultInt("SSH.Command.Timeout");
    public static final int DEFAULT_COMMAND_TIMEOUT_LONG =
                                                    Tools.getDefaultInt("SSH.Command.Timeout.Long");
    public static final int NO_COMMAND_TIMEOUT = 0;
    public static final String SUDO_PROMPT = "DRBD MC sudo pwd: ";
    public static final String SUDO_FAIL = "Sorry, try again";
    private static final ConnectionCallback NO_CONNECTION_CALLBACK = null;
    private static final ProgressBar NO_PROGRESS_BAR = null;
    private static final String MESSAGE_CANCELED = "canceled";
    private static final String LOGOUT_COMMAND = "logout";
    private static final SshOutput NOT_CONNECTED_ERROR = new SshOutput("", 112);
    /** SSHGui object for enter password dialogs etc. */
    private SSHGui sshGui;
    /** Callback when connection is failed or properly closed. */
    private ConnectionCallback connectionCallback;
    private Host host;
    @Inject
    private Provider<ConnectionThread> connectionThreadProvider;
    private ConnectionThread connectionThread;
    private ProgressBar progressBar = null;

    private final LastSuccessfulPassword lastSuccessfulPassword = new LastSuccessfulPassword();
    private final Lock mConnectionLock = new ReentrantLock();
    private final Lock mConnectionThreadLock = new ReentrantLock();
    private LocalPortForwarder localPortForwarder = null;
    @Inject
    private MainData mainData;
    @Inject
    private MainPanel mainPanel;
    @Inject
    private ProgressIndicator progressIndicator;
    @Inject
    private Application application;
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private Provider<Authentication> authenticationProvider;
    private final GuiHelperFiles guiHelperFiles = new GuiHelperFiles();

    boolean reconnect() {
        swingUtils.isNotSwingThread();
        mConnectionThreadLock.lock();
        if (connectionThread == null) {
            mConnectionThreadLock.unlock();
        } else {
            try {
                final ConnectionThread ct = connectionThread;
                mConnectionThreadLock.unlock();
                ct.join(20000);
                if (ct.isAlive()) {
                    return false;
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (connectionThread.isDisconnectedForGood()) {
            return false;
        }
        if (!isConnected()) {
            LOG.debug1("reconnect: connecting: " + host.getName());
            this.connectionCallback = NO_CONNECTION_CALLBACK;
            this.progressBar = NO_PROGRESS_BAR;
            this.sshGui = new SSHGui(mainData.getMainFrame(), host, null);
            authenticateAndConnect();
        }
        return true;
    }

    /** Connects the host. */
    public void connect(final SSHGui sshGui, final ConnectionCallback connectionCallback, final Host host) {
        this.sshGui = sshGui;
        this.connectionCallback = connectionCallback;
        this.host = host;
        if (connectionThread != null && connectionThread.isConnectionEstablished()) {
            connectionThread.setConnectionFailed(false);
            // already connected
            if (connectionCallback != null) {
                connectionCallback.done(1);
            }
            return;
        }
        authenticateAndConnect();
    }

    /**
     * Sets passwords that will be tried first while connecting, but only if
     * they were not set before.
     */
    public void setPasswords(final String lastDsaKey,
                             final String lastRsaKey,
                             final String lastPassword) {

        lastSuccessfulPassword.setPasswordsIfNoneIsSet(lastDsaKey, lastRsaKey, lastPassword);
    }

    public String getLastSuccessfulDsaKey() {
        return lastSuccessfulPassword.getDsaKey();
    }

    public String getLastSuccessfulRsaKey() {
        return lastSuccessfulPassword.getRsaKey();
    }

    /** Returns last successful password. */
    public String getLastSuccessfulPassword() {
        return lastSuccessfulPassword.getPassword();
    }

    /** Waits till connection is established or is failed. */
    public void waitForConnection() {
        mConnectionThreadLock.lock();
        if (connectionThread == null) {
            mConnectionThreadLock.unlock();
        } else {
            try {
                final ConnectionThread ct = connectionThread;
                mConnectionThreadLock.unlock();
                ct.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    /** Connects the host. */
    public void connect(final SSHGui sshGuiT,
                        final ProgressBar progressBar,
                        final ConnectionCallback callbackT,
                        final Host hostT) {
        this.progressBar = progressBar;
        connect(sshGuiT, callbackT, hostT);
    }

    /** Cancels the creating of connection to the sshd. */
    public void cancelConnection() {
        mConnectionThreadLock.lock();
        final ConnectionThread ct;
        try {
            ct = connectionThread;
        } finally {
            mConnectionThreadLock.unlock();
        }
        if (ct != null) {
            ct.cancel();
        }
        final String message = MESSAGE_CANCELED;
        LOG.debug1("cancelConnection: message: " + message);
        host.getTerminalPanel().addCommandOutput(message + '\n');
        host.getTerminalPanel().nextCommand();
        connectionThread.setConnectionFailed(true);
        if (connectionCallback != null) {
            connectionCallback.doneError(message);
        }
        // connection will be established anyway after cancel in the
        // background and it will be closed after that, because conn.connect
        // call cannot be interrupted
    }

    /** Cancels the session (execution of command). */
    public void cancelSession(final ExecCommandThread execCommandThread) {
        execCommandThread.cancelTheSession();
        final String message = MESSAGE_CANCELED;
        LOG.debug1("cancelSession: message" + message);
        host.getTerminalPanel().addCommandOutput("\n");
        host.getTerminalPanel().nextCommand();
    }

    /** Disconnects this host if it has been connected. */
    public void disconnect() {
        mConnectionLock.lock();
        try {
            if (connectionThread == null || !connectionThread.isConnectionEstablished()) {
                return;
            }
            connectionThread.disconnectForGood();
            connectionThread.closeConnectionForGood();
        } finally {
            mConnectionLock.unlock();
        }
        LOG.debug("disconnect: host: " + host.getName());
        host.getTerminalPanel().addCommand(LOGOUT_COMMAND);
        host.getTerminalPanel().nextCommand();
    }

    /**
     * Disconnects this host if it was connected. This should be called if
     * there is an assumption that the connection was lost.There is no way
     * with ganymed ssh library to find out if connection was lost at the
     * moment. The difference to the normal disconnect is that the
     * Connection.close is not called which would hang. The old connection
     * thread will probably will stay there, so here is an infrequent leak.
     *
     * After this method is called a reconnect will work as expected.
     */
    public void forceReconnect() {
        mConnectionLock.lock();
        try {
            if (!connectionThread.isConnectionEstablished()) {
                return;
            }
            connectionThread.closeConnection();
        } finally {
            mConnectionLock.unlock();
        }
        LOG.debug("forceReconnect: host: " + host.getName());
        host.getTerminalPanel().addCommand(LOGOUT_COMMAND);
        host.getTerminalPanel().nextCommand();
    }

    /** Force disconnection. */
    public void forceDisconnect() {
        mConnectionLock.lock();
        try {
           if (!connectionThread.isConnectionEstablished()) {
               return;
           }
           connectionThread.disconnectForGood();
           connectionThread.closeConnectionForGood();
        } finally {
            mConnectionLock.unlock();
        }
        LOG.debug("forceDisconnect: host: " + host.getName());
        host.getTerminalPanel().addCommand("logout");
        host.getTerminalPanel().nextCommand();
    }

    /** Returns true if connection is established. */
    public boolean isConnected() {
        mConnectionLock.lock();
        try {
            if (connectionThread == null) {
                return false;
            }
            return connectionThread.isConnectionEstablished();
        } finally {
            mConnectionLock.unlock();
        }
    }

    public boolean isConnectionFailed() {
        return connectionThread.isConnectionFailed();
    }

    /**
     * Executes command and returns an exit code.
     * 100 is timeout
     * 101 no host
     * 102 no io error
     */
    public SshOutput execCommandAndWait(final ExecCommandConfig execCommandConfig) {
        if (host == null) {
            return new SshOutput("", 101);
        }

        if (!reconnect()) {
            return NOT_CONNECTED_ERROR;
        }

        final String[] answer = new String[]{""};
        final Integer[] exitCode = new Integer[]{100};
        final ExecCallback execCallback = new ExecCallback() {
            @Override
            public void done(final String ans) {
                answer[0] = ans;
                exitCode[0] = 0;
            }
            @Override
            public void doneError(final String ans,
                                  final int ec) {
                answer[0] = ans;
                exitCode[0] = ec;
            }
        };

        execCommandConfig.host(host)
                         .connectionThread(connectionThread)
                         .sshGui(sshGui)
                         .execCallback(execCallback)
                         .execute(mainPanel, progressIndicator).block();
        return new SshOutput(answer[0], exitCode[0]);
    }

    /**
     * Executes command. Command is executed in a new thread, after command
     * is finished execCallback.done function will be called. In case of error,
     * execCallback.doneError is called.
     */
    public ExecCommandThread execCommand(final ExecCommandConfig execCommandConfig) {
        reconnect();
        return execCommandConfig.host(host)
                                .connectionThread(connectionThread)
                                .sshGui(sshGui)
                                .execute(mainPanel, progressIndicator);
    }

    public SshOutput captureCommand(final ExecCommandConfig execCommandConfig) {
        reconnect();
        return execCommandConfig.host(host)
                                .connectionThread(connectionThread)
                                .sshGui(sshGui)
                                .capture(mainPanel, progressIndicator);
    }

    /** Installs gui-helper on the remote host. */
    public void installGuiHelper() {
        if (!application.getKeepHelper()) {
            scp(guiHelperFiles.readGuiHelper(), "@GUI-HELPER-PROG@", "0700", false, null, null, null);
        }
    }

    /** Installs test suite on the remote host. */
    public void installTestFiles() throws IOException {
        if (!connectionThread.isConnectionEstablished()) {
            return;
        }
        final SCPClient scpClient = new SCPClient(connectionThread.getConnection());
        final String fileName = "lcmc-test.tar";
        final String file = Tools.readFile('/' + fileName);
        try {
            scpClient.put(file.getBytes("UTF-8"), fileName, "/tmp");
        } catch (final IOException e) {
            LOG.appError("installTestFiles: could not copy: " + fileName, "", e);
            return;
        }

        final SshOutput sshOutput = execCommandAndWait(new ExecCommandConfig()
                                                           .command("tar xf /tmp/lcmc-test.tar -C /tmp/")
                                                           .sshCommandTimeout(60000));
        if (!sshOutput.isSuccess()) {
            LOG.appWarning("installing test files failed: " + sshOutput.getExitCode());
        }
    }

    /**
     * Creates config on the host with specified name in the specified
     * directory.
     *
     * @param config
     *          config content as a string
     * @param fileName
     *          file name of the config
     * @param dir
     *          directory where the config should be stored
     * @param mode
     *          mode, e.g. "0700"
     * @param makeBackup
                whether to make backup or not
     */
    public void createConfig(final String config,
                             final String fileName,
                             final String dir,
                             final String mode,
                             final boolean makeBackup,
                             final String preCommand,
                             final String postCommand) {
        LOG.debug1("createConfig: " + dir + fileName + "\n" + config);
        scp(config, dir + fileName, mode, makeBackup, null, /* install command */ preCommand, postCommand);
    }

    /**
     * Copies file to the /tmp/ dir on the remote host.
     *
     * @param remoteFilename
     *          new file name on the other host
     */
    public void scp(final String fileContent,
                    final String remoteFilename,
                    final String mode,
                    final boolean makeBackup,
                    String installCommand,
                    final String preCommand,
                    final String postCommand) {
        if  (!isConnected()) {
            return;
        }
        final String commands = buildScpCommand(remoteFilename, makeBackup, preCommand);
        String modeString = "";
        if (mode != null) {
            modeString = " && chmod " + mode + ' ' + remoteFilename + ".new";
        }
        String postCommandString = "";
        if (postCommand != null) {
            postCommandString = " && " + postCommand;
        }
        final String backupString = buildBackupScpCommands(remoteFilename, makeBackup);
        if (installCommand == null) {
            installCommand = "mv " + remoteFilename + ".new " + remoteFilename;
        }
        final String commandTail = "\">" + remoteFilename + ".new"
                                   + modeString

                                   + "&& "
                                   + installCommand

                                   + postCommandString
                                   + backupString;
        LOG.debug1("scp: " + commands + "echo \"..." + commandTail);
        final String escapedBashCommand = DistResource.SUDO
                                          + "bash -c \""
                                          + Tools.escapeQuotes(commands
                                                               + "echo \""
                                                               + Tools.escapeQuotes(fileContent, 1)
                                                               + commandTail, 1)
                                          + '"';
        execCommand(new ExecCommandConfig()
                        .command(escapedBashCommand)
                        .execCallback(new ExecCallback() {
                                          @Override
                                          public void done(final String ans) {
                                              /* ok */
                                          }

                                          @Override
                                          public void doneError(final String ans, final int exitCode) {
                                              if (ans == null) {
                                                  return;
                                              }
                                              scpCommandFailed(ans);
                                          }
                                      })
                        .sshCommandTimeout(10000) /* smaller timeout */
                        .silentCommand()
                        .silentOutput()).block();
    }

    public void startVncPortForwarding(final String remoteHost, final int remotePort) throws IOException {
        final int localPort = remotePort + application.getVncPortOffset();
        try {
            localPortForwarder = connectionThread.getConnection().createLocalPortForwarder(localPort,
                                                                                           "127.0.0.1",
                                                                                           remotePort);
        } catch (final IOException e) {
            throw e;
        }
    }

    public void stopVncPortForwarding(final int remotePort)
        throws IOException {
        try {
            localPortForwarder.close();
        } catch (final IOException e) {
            throw e;
        }
    }

    public boolean isConnectionCanceled() {
        return connectionThread != null && connectionThread.isDisconnectedForGood();
    }

    private void scpCommandFailed(final String ans) {
        for (final String line : ans.split("\n")) {
             if (line.indexOf("error:") != 0) {
                 continue;
             }
             final Thread t = new Thread(
             new Runnable() {
                 @Override
                 public void run() {
                     progressIndicator.progressIndicatorFailed(host.getName(), line, 3000);
                 }
             });
             t.start();
         }
    }

    private void authenticateAndConnect() {
        host.getTerminalPanel().addCommand("ssh " + host.getUserAtHost());
        final Authentication authentication = authenticationProvider.get();
        authentication.init(lastSuccessfulPassword, host, sshGui);
        final ConnectionThread newConnectionThread = connectionThreadProvider.get();
        newConnectionThread.init(host, sshGui, progressBar, connectionCallback, authentication);
        newConnectionThread.start();
        connectionThread = newConnectionThread;
    }

    private String buildScpCommand(final String remoteFilename, final boolean makeBackup, final String preCommand) {
        final StringBuilder commands = new StringBuilder(40);
        if (preCommand != null) {
            commands.append(preCommand);
            commands.append(';');
        }
        if (makeBackup) {
            commands.append("cp ");
            commands.append(remoteFilename);
            commands.append("{,.bak} 2>/dev/null;");
        }
        final int index = remoteFilename.lastIndexOf('/');
        if (index > 0) {
            final String dir = remoteFilename.substring(0, index + 1);
            commands.append("mkdir -p ");
            commands.append(dir);
            commands.append(';');
        }
        return commands.toString();
    }

    private String buildBackupScpCommands(final String remoteFilename, final boolean makeBackup) {
        final StringBuilder backupString = new StringBuilder(50);
        if (makeBackup) {
            backupString.append(" && if ! diff ");
            backupString.append(remoteFilename);
            backupString.append("{,.bak}>/dev/null 2>&1; then ");
            backupString.append("mv ");
            backupString.append(remoteFilename);
            backupString.append("{.bak,.`date +'%s'`} 2>/dev/null;true;");
            backupString.append(" else ");
            backupString.append("rm -f ");
            backupString.append(remoteFilename);
            backupString.append(".bak;");
            backupString.append(" fi ");
        }
        return backupString.toString();
    }
}
