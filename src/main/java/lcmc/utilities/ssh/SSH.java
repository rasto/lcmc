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

package lcmc.utilities.ssh;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.LocalPortForwarder;
import ch.ethz.ssh2.SCPClient;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lcmc.configs.DistResource;
import lcmc.data.Host;
import lcmc.gui.ProgressBar;
import lcmc.gui.SSHGui;
import lcmc.utilities.ConnectionCallback;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.NewOutputCallback;
import lcmc.utilities.Tools;

/**
 * Verifying server hostkeys with an existing known_hosts file
 * Displaying fingerprints of server hostkeys.
 * Adding a server hostkey to a known_hosts file (+hashing the hostname
 * for security).
 * Authentication with DSA, RSA, password and keyboard-interactive methods.
 */
public final class SSH {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(SSH.class);
    /** Default timeout for SSH commands. */
    public static final int DEFAULT_COMMAND_TIMEOUT =
                                    Tools.getDefaultInt("SSH.Command.Timeout");
    /** Default timeout for SSH commands. */
    public static final int DEFAULT_COMMAND_TIMEOUT_LONG =
                               Tools.getDefaultInt("SSH.Command.Timeout.Long");
    /** No timeout for SSH commands. */
    public static final int NO_COMMAND_TIMEOUT = 0;
    /** Sudo prompt. */
    public static final String SUDO_PROMPT = "DRBD MC sudo pwd: ";
    public static final String SUDO_FAIL = "Sorry, try again";
    /** SSHGui object for enter password dialogs etc. */
    private SSHGui sshGui;
    /** Callback when connection is failed or properly closed. */
    private ConnectionCallback callback;
    /** Host data object. */
    private Host host;
    /** This SSH connection object. */
    private volatile SshConnection connection = null;
    /** Connection thread object. */
    private volatile ConnectionThread connectionThread = null;
    /** Progress bar object. */
    private ProgressBar progressBar = null;
    /** Connection failed flag. */
    private boolean connectionFailed;
    /** Whether we are disconnected manually and should not reconnect. */
    private volatile boolean disconnectForGood = true;
    /** Last successful password. */
    private String lastPassword = null;
    /** Last successful rsa key. */
    private String lastRSAKey = null;
    /** Last successful dsa key. */
    private String lastDSAKey = null;
    /** Connection mutex. */
    private final Lock mConnectionLock = new ReentrantLock();
    /** Connection thread mutex. */
    private final Lock mConnectionThreadLock = new ReentrantLock();
    /** Local port forwarder. */
    private LocalPortForwarder localPortForwarder = null;

    /** Reconnect. */
    boolean reconnect() {
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
        if (disconnectForGood) {
            return false;
        }
        if (!isConnected()) {
            LOG.debug1("reconnect: connecting: " + host.getName());
            this.callback = null;
            this.progressBar = null;
            this.sshGui = new SSHGui(Tools.getGUIData().getMainFrame(),
                                     host,
                                     null);
            host.getTerminalPanel().addCommand("ssh " + host.getUserAtHost());
            final ConnectionThread ct = new ConnectionThread();
            mConnectionThreadLock.lock();
            try {
                connectionThread = ct;
            } finally {
                mConnectionThreadLock.unlock();
            }
            ct.start();
        }
        return true;
    }

    /** Connects the host. */
    public void connect(final SSHGui sshGui,
                 final ConnectionCallback callback,
                 final Host host) {
        this.sshGui = sshGui;
        this.callback = callback;
        this.host = host;
        connectionFailed = false;
        if (connection != null) {
            // already connected
            if (callback != null) {
                callback.done(1);
            }
            return;
        }

        host.getTerminalPanel().addCommand("ssh " + host.getUserAtHost());
        final ConnectionThread ct = new ConnectionThread();
        mConnectionThreadLock.lock();
        try {
            connectionThread = ct;
        } finally {
            mConnectionThreadLock.unlock();
        }
        ct.start();
    }

    /**
     * Sets passwords that will be tried first while connecting, but only if
     * they were not set before.
     */
    public void setPasswords(final String lastDSAKey,
                             final String lastRSAKey,
                             final String lastPassword) {
        if (this.lastDSAKey == null
            && this.lastRSAKey == null
            && this.lastPassword == null) {
            this.lastDSAKey = lastDSAKey;
            this.lastRSAKey = lastRSAKey;
            this.lastPassword = lastPassword;
        }
    }

    /** Returns last successful dsa key. */
    public String getLastDSAKey() {
        return lastDSAKey;
    }

    /** Returns last successful rsa key. */
    public String getLastRSAKey() {
        return lastRSAKey;
    }

    /** Returns last successful password. */
    public String getLastPassword() {
        return lastPassword;
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
        final String message = "canceled";
        LOG.debug1("cancelConnection: message: " + message);
        host.getTerminalPanel().addCommandOutput(message + '\n');
        host.getTerminalPanel().nextCommand();
        connectionFailed = true;
        if (callback != null) {
              callback.doneError(message);
        }

        // connection will be established anyway after cancel in the
        // background and it will be closed after that, because conn.connect
        // call cannot be interrupted for now.
    }

    /** Cancels the session (execution of command). */
    public void cancelSession(final ExecCommandThread execCommandThread) {
        execCommandThread.cancel();
        final String message = "canceled";
        LOG.debug1("cancelSession: message" + message);
        //sess.close();
        //sess = null;
        host.getTerminalPanel().addCommandOutput("\n");
        host.getTerminalPanel().nextCommand();
    }

    /** Disconnects this host if it has been connected. */
    public void disconnect() {
        mConnectionLock.lock();
        if (connection == null) {
            mConnectionLock.unlock();
        } else {
            disconnectForGood = true;
            connection.close();
            connection = null;
            mConnectionLock.unlock();
            LOG.debug("disconnect: host: " + host.getName());
            host.getTerminalPanel().addCommand("logout");
            host.getTerminalPanel().nextCommand();
        }
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
        if (connection == null) {
            mConnectionLock.unlock();
        } else {
            connection.setClosed();
            connection = null;
            mConnectionLock.unlock();
            LOG.debug("forceReconnect: host: " + host.getName());
            host.getTerminalPanel().addCommand("logout");
            host.getTerminalPanel().nextCommand();
        }
    }

    /** Force disconnection. */
    public void forceDisconnect() {
        mConnectionLock.lock();
        if (connection == null) {
            mConnectionLock.unlock();
        } else {
            disconnectForGood = true;
            connection.setClosed();
            connection = null;
            mConnectionLock.unlock();
            LOG.debug("forceDisconnect: host: " + host.getName());
            host.getTerminalPanel().addCommand("logout");
            host.getTerminalPanel().nextCommand();
        }
    }

    /** Returns true if connection is established. */
    public boolean isConnected() {
        mConnectionLock.lock();
        try {
            return connection != null;
        } finally {
            mConnectionLock.unlock();
        }
    }

    /** Returns true if connection is established. */
    public boolean isConnectionFailed() {
        return connectionFailed;
    }

    /**
     * Executes command and returns an exit code.
     * 100 is timeout
     * 101 no host
     * 102 no io error
     */
    public SshOutput execCommandAndWait(final String command,
                                        final boolean outputVisible,
                                        final boolean commandVisible,
                                        final int sshCommandTimeout) {
        if (host == null) {
            return new SshOutput("", 101);
        }
        final ExecCommandThread execCommandThread;
        final String[] answer = new String[]{""};
        final Integer[] exitCode = new Integer[]{100};
        try {
            execCommandThread = new ExecCommandThread(
                            host,
                            connection,
                            sshGui,
                            command,
                            new ExecCallback() {
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
                            },
                            null,
                            outputVisible,
                            commandVisible,
                            sshCommandTimeout, this);
        } catch (final IOException e) {
            LOG.appError("execCommandThread: Can not execute command: "
                         + command + " " + e.getMessage());
            closeSshConnection();
            return new SshOutput("", 102);
        }
        if (reconnect()) {
            try {
                execCommandThread.start();
            } catch (RuntimeException e) {
                LOG.appWarning("execCommandAndWait: " + e.getMessage());
                closeSshConnection();
            }
            try {
                execCommandThread.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return new SshOutput(answer[0], exitCode[0]);
    }

    /**
     * Executes command. Command is executed in a new thread, after command
     * is finished execCallback.done function will be called. In case of error,
     * execCallback.doneError is called.
     *
     * @param command
     *          command that is to be executed.
     * @param execCallback
     *          callback that implements ExecCallback interface.
     * @param outputVisible
     *          whether the output of the command should be visible
     *
     * @return command thread
     */
    public ExecCommandThread execCommand(final String command,
                                         final ExecCallback execCallback,
                                         final boolean outputVisible,
                                         final boolean commandVisible,
                                         final int sshCommandTimeout) {
        if (host == null) {
            return null;
        }
        final String realCommand = host.replaceVars(command);
        LOG.debug2("execCommand: real command: " + realCommand);
        final ExecCommandThread execCommandThread;
        try {
            execCommandThread = new ExecCommandThread(host,
                                                      connection,
                                                      sshGui,
                                                      realCommand,
                                                      execCallback,
                                                      null,
                                                      outputVisible,
                                                      commandVisible,
                                                      sshCommandTimeout,
                                                      this);
        } catch (final IOException e) {
            LOG.appError("execCommand: Can not execute command: "
                           + realCommand + " " + e.getMessage());
            closeSshConnection();
            return null;
        }
        if (reconnect()) {
            try {
                execCommandThread.start();
            } catch (RuntimeException e) {
                LOG.appWarning("execCommand: " + e.getMessage());
                closeSshConnection();
            }
        }
        return execCommandThread;
    }

    /**
     * Executes command. Command is executed in a new thread, after command
     * is finished execCallback.done function will be called. In case of error,
     * execCallback.doneError is called. During any new output a output
     * callback will be called.
     *
     * @param command
     *          command that is to be executed.
     * @param execCallback
     *          callback that implements ExecCallback interface.
     * @param newOutputCallback
     *          callback that is called after a new output is available
     * @param outputVisible
     *          whether the output of the command should be visible
     * @param commandVisible
     *          whether the command should be visible
     *
     * @return thread
     */
    public ExecCommandThread execCommand(
                               final String command,
                               final ExecCallback execCallback,
                               final NewOutputCallback newOutputCallback,
                               final boolean outputVisible,
                               final boolean commandVisible,
                               final int sshCommandTimeout) {
        final String realCommand = host.replaceVars(command);
        final ExecCommandThread execCommandThread;
        try {
            execCommandThread = new ExecCommandThread(host,
                                                      connection,
                                                      sshGui,
                                                      realCommand,
                                                      execCallback,
                                                      newOutputCallback,
                                                      outputVisible,
                                                      commandVisible,
                                                      sshCommandTimeout, this);
        } catch (final IOException e) {
            LOG.appError("execCommand: can not execute command: "
                         + realCommand + " " + e.getMessage());
            closeSshConnection();
            return null;
        }
        if (reconnect()) {
            try {
                execCommandThread.start();
            } catch (RuntimeException e) {
                LOG.appWarning("execCommand: " + e.getMessage());
                closeSshConnection();
            }
        }
        return execCommandThread;
    }

    /**
     * Executes command and manages a progress bar. Command is executed in a
     * new thread, after command is finished execCallback.done function will
     * be called. In case of error, execCallback.doneError is called.
     *
     * @param command
     *          command that is to be executed.
     * @param progressBar
     *
     * @param execCallback
     *          callback that implements ExecCallback interface.
     * @param outputVisible
     *          whether the output of the command should be visible
     * @param commandVisible
     *          whether the command should be visible
     *
     * @return command thread
     */
    public ExecCommandThread execCommand(final String command,
                                         final ProgressBar progressBar,
                                         final ExecCallback execCallback,
                                         final boolean outputVisible,
                                         final boolean commandVisible,
                                         final int sshCommandTimeout) {
        LOG.debug2("execCommand: with progress bar");
        this.progressBar = progressBar;
        if (progressBar != null) {
            progressBar.start(0);
            progressBar.hold();
        }
        return execCommand(command,
                           execCallback,
                           outputVisible,
                           commandVisible,
                           sshCommandTimeout);
    }

    private String getKeyFromUser(final String lastError) {
        return sshGui.enterSomethingDialog(
                                Tools.getString("SSH.RSA.DSA.Authentication"),
                                new String[] {lastError,
                                              "<html>"
                                              + Tools.getString(
                                               "SSH.Enter.passphrase")
                                              + "</html>",

                                              },
                                "<html>"
                                + Tools.getString("SSH.Enter.passphrase2")
                                + "</html>",
                                Tools.getDefault("SSH.PublicKey"),
                                true);
    }

    /** Installs gui-helper on the remote host. */
    public void installGuiHelper() {
        if (!Tools.getApplication().getKeepHelper()) {
            final String fileName = "/help-progs/lcmc-gui-helper";
            final String file = Tools.getFile(fileName);
            if (file != null) {
                scp(file, "@GUI-HELPER-PROG@", "0700", false, null, null, null);
            }
        }
    }

    /** Installs test suite on the remote host. */
    public void installTestFiles() {
        final Connection conn = connection;
        if (conn == null) {
            return;
        }
        final SCPClient scpClient = new SCPClient(conn);
        final String fileName = "lcmc-test.tar";
        final String file = Tools.getFile('/' + fileName);
        try {
            scpClient.put(file.getBytes(), fileName, "/tmp");
        } catch (final IOException e) {
            LOG.appError("installTestFiles: could not copy: "
                         + fileName, "", e);
            return;
        }
        final SshOutput ret = execCommandAndWait(
                                       "tar xf /tmp/lcmc-test.tar -C /tmp/",
                                       false,
                                       false,
                                       60000);
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
        scp(config,
            dir + fileName,
            mode,
            makeBackup,
            null, /* install command */
            preCommand,
            postCommand);
    }

    /**
     * Copies file to the /tmp/ dir on the remote host.
     *
     * @param fileContent
     *          content of the file as string
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
        if  (!isConnected()) {
            return;
        }
        String modeString = "";
        if (mode != null) {
            modeString = " && chmod " + mode + ' ' + remoteFilename + ".new";
        }
        String postCommandString = "";
        if (postCommand != null) {
            postCommandString = " && " + postCommand;
        }
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
        if (installCommand == null) {
            installCommand = "mv " + remoteFilename + ".new " + remoteFilename;
        }
        final String commandTail = "\">" + remoteFilename + ".new"
                                   + modeString

                                   + "&& "
                                   + installCommand

                                   + postCommandString
                                   + backupString.toString();
        LOG.debug1("scp: " + commands.toString() + "echo \"..." + commandTail);
        final Thread t = execCommand(
                            DistResource.SUDO + "bash -c \""
                            + Tools.escapeQuotes(
                                commands.toString()
                                + "echo \""
                                + Tools.escapeQuotes(fileContent, 1)
                                + commandTail, 1)
                            + '"',
                            new ExecCallback() {
                                @Override
                                public void done(final String ans) {
                                    /* ok */
                                }
                                @Override
                                public void doneError(
                                                         final String ans,
                                                         final int exitCode) {
                                    if (ans == null) {
                                        return;
                                    }
                                    for (final String line
                                                   : ans.split("\n")) {
                                        if (line.indexOf(
                                                    "error:") != 0) {
                                            continue;
                                        }
                                        final Thread t = new Thread(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                Tools.progressIndicatorFailed(
                                                                host.getName(),
                                                                line,
                                                                3000);
                                            }
                                        });
                                        t.start();
                                    }
                                }
                            },
                            false,
                            false,
                            10000); /* smaller timeout */
        try {
            t.join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Starts port forwarding for vnc. */
    public void startVncPortForwarding(final String remoteHost,
                                final int remotePort)
        throws IOException {
        final int localPort =
                        remotePort + Tools.getApplication().getVncPortOffset();
        try {
            localPortForwarder =
                connection.createLocalPortForwarder(localPort,
                                                    "127.0.0.1",
                                                    remotePort);
        } catch (final IOException e) {
            throw e;
        }
    }

    /** Stops port forwarding for vnc. */
    public void stopVncPortForwarding(final int remotePort)
        throws IOException {
        try {
            localPortForwarder.close();
        } catch (final IOException e) {
            throw e;
        }
    }

    /** Returns whether connection was canceled. */
    public boolean isConnectionCanceled() {
        return disconnectForGood;
    }




    /**
     * The SSH-2 connection is established in this thread.
     * If we would not use a separate thread (e.g., put this code in
     * the event handler of the "Login" button) then the GUI would not
     * be responsive (missing window repaints if you move the window etc.)
     */
    public class ConnectionThread extends Thread {
        /** Username with which it will be connected. */
        private final String username;
        /** Hostname of the host to which it will be connect. */
        private final String hostname;
        /** Cancel the connecting. */
        private boolean cancelIt = false;

        /** Prepares a new {@code ConnectionThread} object. */
        ConnectionThread() {
            super();
            username = host.getFirstUsername();
            hostname = host.getFirstIp();
        }

        private void authenticate(final SshConnection conn) throws IOException {
            boolean enableKeyboardInteractive = true;
            boolean enablePublicKey = true;
            String lastError = null;
            int publicKeyTry = 3; /* how many times to try the public key
                                     authentification */
            int passwdTry = 3;    /* how many times to try the password
                                     authentification */
            final int connectTimeout =
                                    Tools.getDefaultInt("SSH.ConnectTimeout");
            final int kexTimeout = Tools.getDefaultInt("SSH.KexTimeout");
            final boolean noPassphrase = Tools.getApplication().isNoPassphrase();
            while (!cancelIt) {
                if (lastPassword == null) {
                    lastPassword =
                                Tools.getApplication().getAutoOptionHost("pw");
                    if (lastPassword == null) {
                        lastPassword =
                              Tools.getApplication().getAutoOptionCluster("pw");
                    }
                }
                if (lastPassword == null) {
                    if (enablePublicKey
                        && conn.isAuthMethodAvailable(username, "publickey")) {
                        final File dsaKey = new File(
                                         Tools.getApplication().getIdDSAPath());
                        final File rsaKey = new File(
                                         Tools.getApplication().getIdRSAPath());
                        if (dsaKey.exists() || rsaKey.exists()) {
                            String key = "";
                            if (lastDSAKey != null) {
                                key = lastDSAKey;
                            } else if (lastRSAKey != null) {
                                key = lastRSAKey;
                            }
                            /* Passwordless auth */

                            boolean res = false;
                            if (noPassphrase || !"".equals(key)) {
                                /* try first passwordless authentication.  */
                                if (lastRSAKey == null && dsaKey.exists()) {
                                    try {
                                        res = conn.authenticateWithPublicKey(
                                                                      username,
                                                                      dsaKey,
                                                                      key);
                                    } catch (final IOException e) {
                                        lastDSAKey = null;
                                        LOG.debug("authenticate: dsa passwordless failed");
                                    }
                                    if (res) {
                                        LOG.debug("authenticate: dsa passwordless auth successful");
                                        lastDSAKey = key;
                                        lastRSAKey = null;
                                        lastPassword = null;
                                        break;
                                    }

                                    conn.close();
                                    conn.connect(new PopupHostKeyVerifier(sshGui),
                                                 connectTimeout,
                                                 kexTimeout);
                                }


                                if (rsaKey.exists()) {
                                    try {
                                        res = conn.authenticateWithPublicKey(
                                                                      username,
                                                                      rsaKey,
                                                                      key);
                                    } catch (final IOException e) {
                                        lastRSAKey = null;
                                        LOG.debug("authenticate: rsa passwordless failed");
                                    }
                                    if (res) {
                                        LOG.debug("authenticate: rsa passwordless auth successful");
                                        lastRSAKey = key;
                                        lastDSAKey = null;
                                        lastPassword = null;
                                        break;
                                    }

                                    conn.close();
                                    conn.connect(new PopupHostKeyVerifier(sshGui),
                                                 connectTimeout,
                                                 kexTimeout);
                                }
                            }
                            key = getKeyFromUser(lastError);
                            if (key == null) {
                                cancelIt = true;
                                disconnectForGood = true;
                                break;
                            }
                            if ("".equals(key)) {
                                publicKeyTry = 0;
                            }
                            if (dsaKey.exists()) {
                                try {
                                    res = conn.authenticateWithPublicKey(
                                                                      username,
                                                                      dsaKey,
                                                                      key);
                                } catch (final IOException e) {
                                        lastDSAKey = null;
                                        LOG.debug("authenticate: dsa key auth failed");
                                }
                                if (res) {
                                    LOG.debug("authenticate: dsa key auth successful");
                                    lastRSAKey = null;
                                    lastDSAKey = key;
                                    lastPassword = null;
                                    break;
                                }
                                conn.close();
                                conn.connect(new PopupHostKeyVerifier(sshGui),
                                             connectTimeout,
                                             kexTimeout);
                            }

                            if (rsaKey.exists()) {
                                try {
                                    res = conn.authenticateWithPublicKey(
                                                                      username,
                                                                      rsaKey,
                                                                      key);
                                } catch (final IOException e) {
                                    lastRSAKey = null;
                                    LOG.debug("authenticate: rsa key auth failed");
                                }
                                if (res) {
                                    LOG.debug("authenticate: rsa key auth successful");
                                    lastRSAKey = key;
                                    lastDSAKey = null;
                                    lastPassword = null;
                                    break;
                                }
                                conn.close();
                                conn.connect(new PopupHostKeyVerifier(sshGui),
                                             connectTimeout,
                                             kexTimeout);
                            }

                            lastError = Tools.getString(
                                        "SSH.Publickey.Authentication.Failed");
                        } else {
                            publicKeyTry = 0;
                        }
                        publicKeyTry--;
                        if (publicKeyTry <= 0) {
                            enablePublicKey = false; // do not try again
                            publicKeyTry = 3;
                        }
                        continue;
                    }
                }

                if (enableKeyboardInteractive
                    && conn.isAuthMethodAvailable(username,
                                                  "keyboard-interactive")) {
                    final InteractiveLogic interactiveLogic = new InteractiveLogic(
                                                                     lastError,
                                                                     host,
                                                                     lastPassword,
                                                                     sshGui);

                    final boolean res =
                             conn.authenticateWithKeyboardInteractive(username,
                                                                      interactiveLogic);

                    if (res) {
                        lastRSAKey = null;
                        lastDSAKey = null;
                        lastPassword = interactiveLogic.getLastPassword();
                        break;
                    } else {
                        lastPassword = null;
                    }

                    if (interactiveLogic.getPromptCount() == 0) {
                        /* aha. the server announced that it supports
                         * "keyboard-interactive", but when we asked for
                         * it, it just denied the request without sending
                         * us any prompt. That happens with some server
                         * versions/configurations. We just disable the
                         * "keyboard-interactive" method and notify the
                         * user.
                         */
                        lastError = Tools.getString(
                                        "SSH.KeyboardInteractive.DoesNotWork");

                        /* do not try this again */
                        enableKeyboardInteractive = false;
                    } else {
                        /* try again, if possible */
                        lastError = Tools.getString(
                                             "SSH.KeyboardInteractive.Failed");
                    }
                    continue;
                }

                if (conn.isAuthMethodAvailable(username, "password")) {
                    final String ans;
                    if (lastPassword == null) {
                        ans = sshGui.enterSomethingDialog(
                                Tools.getString("SSH.PasswordAuthentication"),
                                new String[] {lastError,
                                              "<html>"
                                              + host.getUserAtHost()
                                              + Tools.getString(
                                                   "SSH.Enter.password")
                                              + "</html>"},
                                null,
                                null,
                                true);
                        if (ans == null) {
                            cancelIt = true;
                            break;
                        }
                    } else {
                        ans = lastPassword;
                    }

                    if (ans == null) {
                        throw new IOException("Login aborted by user");
                    }
                    if ("".equals(ans)) {
                        passwdTry = 0;
                    }
                    final boolean res = conn.authenticateWithPassword(username,
                                                                      ans);
                    if (res) {
                        lastPassword = ans;
                        host.setSudoPassword(lastPassword);
                        lastRSAKey = null;
                        lastDSAKey = null;
                        break;
                    } else {
                        lastPassword = null;
                    }

                    /* try again, if possible */
                    lastError = Tools.getString(
                                        "SSH.Password.Authentication.Failed");
                    passwdTry--;
                    if (passwdTry <= 0) {
                        enablePublicKey = true;
                        passwdTry = 3;
                    }

                    continue;
                }

                throw new IOException(
                            "No supported authentication methods available.");
            }
            if (cancelIt) {
                // since conn.connect call is not interrupted, we get
                // here only after connection is esteblished or after
                // timeout.
                conn.close();
                LOG.debug( "authenticate: closing canceled connection");
                mConnectionThreadLock.lock();
                try {
                    connectionThread = null;
                } finally {
                    mConnectionThreadLock.unlock();
                }
                host.setConnected();
                if (callback != null) {
                    callback.doneError("");
                }
                closeSshConnection();
            } else {
                //  authentication ok.
                mConnectionLock.lock();
                try {
                    connection = conn;
                } finally {
                    mConnectionLock.unlock();
                }
                host.setConnected();
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        host.getTerminalPanel().nextCommand();
                    }
                });
                mConnectionThreadLock.lock();
                try {
                    connectionThread = null;
                } finally {
                    mConnectionThreadLock.unlock();
                }
                host.setConnected();
                if (callback != null) {
                    callback.done(0);
                }
                LOG.debug1("authenticate: " + host.getName()
                           + ": authentication ok");
            }
        }

        /** Cancel the connecting. */
        void cancel() {
            cancelIt = true;
        }

        /** Start connection in the thread. */
        @Override
        public void run() {
            if (callback != null && isConnected()) {
                callback.done(1);
            }
            host.setSudoPassword("");
            final SshConnection conn = new SshConnection(hostname,
                                                       host.getSSHPortInt());
            disconnectForGood = false;

            try {
                if (hostname == null) {
                    throw new IOException("hostname is not set");
                }
                /* connect and verify server host key (with callback) */
                LOG.debug2("run: verify host keys: " + hostname);
                final String[] hostkeyAlgos =
                    Tools.getApplication().getKnownHosts().
                        getPreferredServerHostkeyAlgorithmOrder(hostname);

                if (hostkeyAlgos != null) {
                    conn.setServerHostKeyAlgorithms(hostkeyAlgos);
                }
                final int connectTimeout =
                                    Tools.getDefaultInt("SSH.ConnectTimeout");
                final int kexTimeout = Tools.getDefaultInt("SSH.KexTimeout");
                if (progressBar != null) {
                    final int timeout = (connectTimeout < kexTimeout)
                                        ? connectTimeout : kexTimeout;
                    progressBar.start(timeout);
                }
                /* ConnectionMonitor does not work if we lost a connection */
                //final ConnectionMonitor connectionMonitor =
                //                               new ConnectionMonitor() {
                //    public void connectionLost(java.lang.Throwable reason) {
                //        if (!disconnectForGood) {
                //            connection = null;
                //        }
                //    }
                //};
                //conn.addConnectionMonitor(connectionMonitor);
                conn.connect(new PopupHostKeyVerifier(sshGui),
                             connectTimeout,
                             kexTimeout);

                /* authentication phase */
                authenticate(conn);
            } catch (final IOException e) {
                LOG.appWarning("run: connecting failed: " + e.getMessage());
                connectionFailed = true;
                if (!cancelIt) {
                    host.getTerminalPanel().addCommandOutput(e.getMessage()
                                                             + '\n');
                    host.getTerminalPanel().nextCommand();
                    if (callback != null) {
                        callback.doneError(e.getMessage());
                    }
                }
                mConnectionThreadLock.lock();
                try {
                    connectionThread = null;
                } finally {
                    mConnectionThreadLock.unlock();
                }
                closeSshConnection();
            }
        }
    }

    private void closeSshConnection() {
        mConnectionLock.lock();
        try {
            if (connection == null) {
                return;
            }
            connection.setClosed();
            connection = null;
        } finally {
            mConnectionLock.unlock();
        }
        host.setConnected();
    }
}
