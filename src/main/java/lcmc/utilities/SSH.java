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

package lcmc.utilities;

import lcmc.data.Host;
import lcmc.gui.SSHGui;
import lcmc.gui.ProgressBar;
import lcmc.configs.DistResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.InteractiveCallback;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.LocalPortForwarder;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.channel.ChannelManager;

import java.io.OutputStream;
import java.lang.InterruptedException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    /** SSHGui object for enter password dialogs etc. */
    private SSHGui sshGui;
    /** Callback when connection is failed or properly closed. */
    private ConnectionCallback callback;
    /** Host data object. */
    private Host host;
    /** This SSH connection object. */
    private volatile MyConnection connection = null;
    /** Connection thread object. */
    private volatile ConnectionThread connectionThread = null;
    /** Progress bar object. */
    private ProgressBar progressBar = null;
    /** Connection failed flag. */
    private boolean connectionFailed;
    /** Whether we are disconnected manually and should not reconnect. */
    private volatile boolean disconnectForGood = true;
    /** Exit code if command failed. */
    private static final int ERROR_EXIT_CODE = 255;
    /** Size of the buffer for output of commads. */
    private static final int EXEC_OUTPUT_BUFFER_SIZE = 8192;
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

    /** This class is a thread that executes commands. */
    public final class ExecCommandThread extends Thread {
        /** Command that should be executed. */
        private String command;
        /** After the exec callback. */
        private final ExecCallback execCallback;
        /** After a new output is available callback. */
        private final NewOutputCallback newOutputCallback;
        /** Cancel the execution flag. */
        private volatile boolean cancelIt = false;
        /** Whether the output should be visible in the terminal area. */
        private boolean outputVisible;
        /** Whether the command should be visible in the terminal area. */
        private final boolean commandVisible;
        /** Session lock. */
        private final Lock mSessionLock = new ReentrantLock();
        /** Execution session object. */
        private Session sess = null;
        /** Timeout for ssh command. */
        private final int sshCommandTimeout;

       /**
        * Executes command on the host.
        *
        * @param command
        *          command that will be executed.
        * @param outputVisible
        *          whether the output of the command should be visible
        */
        private SSHOutput execOneCommand(final String command,
                                         final boolean outputVisible) {
            if (sshCommandTimeout > 0 && sshCommandTimeout < 2000) {
                LOG.appWarning("execOneCommand: timeout: "
                               + sshCommandTimeout
                               + " to small for timeout? "
                               + command);
            }
            this.command = command;
            this.outputVisible = outputVisible;
            final StringBuilder res = new StringBuilder("");
            mConnectionLock.lock();
            if (connection == null) {
                mConnectionLock.unlock();
                return new SSHOutput("SSH.NotConnected", 1);
            } else {
                mConnectionLock.unlock();
            }
            if ("installGuiHelper".equals(command)) {
                installGuiHelper();
                return new SSHOutput("", 0);
            }
            int exitCode = 100;
            try {
                mSessionLock.lock();
                final Session thisSession;
                try {
                    thisSession = sess;
                } finally {
                    mSessionLock.unlock();
                }
                if (thisSession == null) {
                    return new SSHOutput("", 130);
                }
                /* requestPTY mixes stdout and strerr together, but it works
                   better at the moment.
                   With pty, the sudo wouldn't work, because we don't want
                   to enter sudo password by every command.
                   (It would be exposed) */
                thisSession.requestPTY("dumb", 0, 0, 0, 0, null);
                LOG.debug2("execOneCommand: command: "
                           + host.getName()
                           + ": "
                           + host.getSudoCommand(
                                        host.getHoppedCommand(command),
                                        true));
                thisSession.execCommand("bash -c '"
                                        + Tools.escapeSingleQuotes(
                                                            "export LC_ALL=C;"
                                        + host.getSudoCommand(
                                               host.getHoppedCommand(command),
                                               false), 1) + '\'');
                final InputStream stdout = thisSession.getStdout();
                final OutputStream stdin = thisSession.getStdin();
                final InputStream stderr = thisSession.getStderr();
                //byte[] buff = new byte[8192];
                final byte[] buff = new byte[EXEC_OUTPUT_BUFFER_SIZE];
                boolean skipNextLine = false;
                boolean cancelSudo = false;
                while (true) {
                    final String sudoPwd = host.getSudoPassword();
                    if ((stdout.available() == 0)
                        && (stderr.available() == 0)) {
                        /* Even though currently there is no data available,
                         * it may be that new data arrives and the session's
                         * underlying channel is closed before we call
                         * waitForCondition(). This means that EOF and
                         * STDOUT_DATA (or STDERR_DATA, or both) may be set
                         * together.
                         */
                        int conditions = 0;
                        if (!cancelIt) {
                            conditions = thisSession.waitForCondition(
                                            ChannelCondition.STDOUT_DATA
                                            | ChannelCondition.STDERR_DATA
                                            | ChannelCondition.EOF,
                                            sshCommandTimeout);
                        }
                        if (cancelIt) {
                            LOG.info("execOneCommand: SSH cancel");
                            throw new IOException(
                                "Canceled while waiting for data from peer.");
                        }

                        if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                                /* A timeout occured. */
                                LOG.appWarning("execOneCommand: SSH timeout: "
                                               + command);
                                Tools.progressIndicatorFailed(
                                   host.getName(),
                                   "SSH timeout: "
                                   + command.replaceAll(DistResource.SUDO, ""));
                                throw new IOException(
                                  "Timeout while waiting for data from peer.");
                        }


                        /* Here we do not need to check separately for CLOSED,
                         * since CLOSED implies EOF */
                        if ((conditions & ChannelCondition.EOF) != 0
                            && (conditions
                                & (ChannelCondition.STDOUT_DATA
                                   | ChannelCondition.STDERR_DATA)) == 0) {
                            /* The remote side won't send us further data... */
                            /* ... and we have consumed all data in the
                             * ... local arrival window. */
                            break;
                        }

                        /* OK, either STDOUT_DATA or STDERR_DATA (or both) */
                        /* ... is set. */

                    }

                    /* If you below replace "while" with "if", then the way
                     * the output appears on the local stdout and stder streams
                     * is more "balanced". Addtionally reducing the buffer size
                     * will also improve the interleaving, but performance will
                     * slightly suffer. OKOK, that all matters only if you get
                     * HUGE amounts of stdout and stderr data =)
                     */

                    /* stdout */
                    final StringBuilder output = new StringBuilder("");
                    while (stdout.available() > 0 && !cancelIt) {
                        final int len = stdout.read(buff);

                        if (len > 0) {
                            final String buffString =
                                            new String(buff, 0, len, "UTF-8");
                            output.append(buffString);

                            if (outputVisible) {
                                host.getTerminalPanel().addContent(buffString);
                            }
                        }
                    }
                    if (output.indexOf(SUDO_PROMPT) >= 0) {
                        if (sudoPwd == null) {
                            cancelSudo = enterSudoPassword();
                        }
                        final String pwd = host.getSudoPassword() + '\n';
                        stdin.write(pwd.getBytes());
                        skipNextLine = true;
                        continue;
                    } else if (output.indexOf(SUDO_FAIL) >= 0) {
                        host.setSudoPassword(null);
                    } else {
                        if (skipNextLine) {
                            /* this is the "enter" after pwd */
                            skipNextLine = false;
                            if (output.charAt(0) == 13
                                && output.charAt(1) == 10) {
                                output.delete(0, 2);
                                if (output.length() == 0) {
                                    continue;
                                }
                            }
                        }
                    }

                    /* stderr */
                    final CharSequence errOutput = new StringBuilder("");
                    while (stderr.available() > 0 && !cancelIt) {
                        // this is unreachable.
                        // stdout and stderr are mixed in the stdout
                        // if pty is requested.
                        final int len = stderr.read(buff);
                        if (len > 0) {
                            final String buffString =
                                            new String(buff, 0, len, "UTF-8");
                            output.append(buffString);

                            if (outputVisible) {
                                host.getTerminalPanel().addContentErr(
                                                                   buffString);
                            }
                        }
                    }
                    res.append(errOutput);

                    if (newOutputCallback != null && !cancelIt) {
                        LOG.debug2("execOneCommand: output: " + exitCode + ": "
                                   + host.getName()
                                   + ": "
                                   + output.toString());
                        newOutputCallback.output(output.toString());
                    }

                    if (cancelIt) {
                        return new SSHOutput("", 130);
                    }

                    if (newOutputCallback == null) {
                        res.append(output);
                    }
                }

                if (outputVisible) {
                    host.getTerminalPanel().nextCommand();
                }
                thisSession.waitForCondition(ChannelCondition.EXIT_STATUS,
                                             10000);
                final Integer ec = thisSession.getExitStatus();
                if (ec != null) {
                    exitCode = ec;
                }
                thisSession.close();
                sess = null;
            } catch (final IOException e) {
                LOG.appWarning("execOneCommand: "
                               + host.getName() + ':' + e.getMessage()
                               + ':' + command);
                exitCode = ERROR_EXIT_CODE;
                cancel();
            }
            final String outputString = res.toString();
            LOG.debug2("execOneCommand: output: " + exitCode + ": "
                       + host.getName()
                       + ": "
                       + outputString);
            return new SSHOutput(outputString, exitCode);
        }

        /** Cancel the session. */
        public void cancel() {
            cancelIt = true;
            mSessionLock.lock();
            final Session thisSession;
            try {
                thisSession = sess;
                sess = null;
            } finally {
                mSessionLock.unlock();
            }
            if (thisSession != null) {
                thisSession.close();
            }
        }

        /** Executes a command in a thread. */
        ExecCommandThread(final String command,
                          final ExecCallback execCallback,
                          final NewOutputCallback newOutputCallback,
                          final boolean outputVisible,
                          final boolean commandVisible,
                          final int sshCommandTimeout)
        throws IOException {
            super();
            this.command = command;
            LOG.debug2("ExecCommandThread: command: " + command);
            this.execCallback = execCallback;
            this.newOutputCallback = newOutputCallback;
            this.outputVisible = outputVisible;
            this.commandVisible = commandVisible;
            this.sshCommandTimeout = sshCommandTimeout;
            if (command.length() > 9
                && command.substring(0, 9).equals("NOOUTPUT:")) {
                this.outputVisible = false;
                this.command = command.substring(9, command.length());
            } else if (command.length() > 7
                       && command.substring(0, 7).equals("OUTPUT:")) {
                this.outputVisible = true;
                this.command = command.substring(7, command.length());
            } else {
                this.outputVisible = outputVisible;
                this.command = command;
            }
        }

        /**
         * Reconnects, connects if there is no connection and executes a
         * command.
         */
        @Override
        public void run() {
            if (reconnect()) {
                mConnectionLock.lock();
                if (connection == null) {
                    mConnectionLock.unlock();
                    if (execCallback != null) {
                        execCallback.doneError("not connected", 139);
                    }
                } else {
                    final MyConnection conn = connection;
                    mConnectionLock.unlock();
                    exec(conn);
                }
            }
        }

        /** Executes the command. */
        private void exec(final MyConnection conn) {
            // ;;; separates commands, that are to be executed one after one,
            // if previous command has finished successfully.
            final String[] commands = command.split(";;;");
            final StringBuilder ans = new StringBuilder("");
            for (final String command1 : commands) {
                final Boolean[] cancelTimeout = new Boolean[1];
                cancelTimeout[0] = false;
                final Thread tt = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Tools.sleep(Tools.getDefaultInt("SSH.ConnectTimeout"));
                        if (!cancelTimeout[0]) {
                            LOG.debug1("exec: " + host.getName()
                                    + ": open ssh session: timeout");
                            cancelTimeout[0] = true;
                            conn.dmcCancel();
                        }
                    }
                });
                tt.start();
                try {
                    /* it may hang here if we lost connection, so it will be
                    * interrupted after a timeout. */
                    final Session newSession = conn.openSession();
                    mSessionLock.lock();
                    try {
                        sess = newSession;
                    } finally {
                        mSessionLock.unlock();
                    }
                    if (cancelTimeout[0]) {
                        throw new IOException("open session failed");
                    }
                    cancelTimeout[0] = true;
                } catch (final IOException e) {
                    mConnectionLock.lock();
                    try {
                        connection = null;
                    } finally {
                        mConnectionLock.unlock();
                    }
                    if (execCallback != null) {
                        execCallback.doneError("could not open session", 45);
                    }
                    break;
                }
                final String cmd = command1.trim();
                //Tools.commandLock();
                if (commandVisible && outputVisible) {
                    final String consoleCommand = host.replaceVars(cmd, true);
                    host.getTerminalPanel().addCommand(
                            consoleCommand.replaceAll(DistResource.SUDO, " "));
                }
                final SSHOutput ret = execOneCommand(cmd, outputVisible);
                ans.append(ret.getOutput());
                final int exitCode = ret.getExitCode();
                // don't execute after error
                if (exitCode != 0) {
                    if (execCallback != null) {
                        if (outputVisible) {
                            Tools.getGUIData().expandTerminalSplitPane(0);
                        }
                        execCallback.doneError(ans.toString(), exitCode);
                    }
                    return;
                }
            }
            if (execCallback != null) {
                execCallback.done(ans.toString());
            }
        }
    }

    /**
     * Executes command and returns an exit code.
     * 100 is timeout
     * 101 no host
     * 102 no io error
     */
    public SSHOutput execCommandAndWait(final String command,
                                        final boolean outputVisible,
                                        final boolean commandVisible,
                                        final int sshCommandTimeout) {
        if (host == null) {
            return new SSHOutput("", 101);
        }
        final ExecCommandThread execCommandThread;
        final String[] answer = new String[]{""};
        final Integer[] exitCode = new Integer[]{100};
        try {
            execCommandThread = new ExecCommandThread(
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
                            sshCommandTimeout);
        } catch (final IOException e) {
            LOG.appError("execCommandThread: Can not execute command: "
                         + command, "", e);
            return new SSHOutput("", 102);
        }
        execCommandThread.start();
        try {
            execCommandThread.join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new SSHOutput(answer[0], exitCode[0]);
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
            execCommandThread = new ExecCommandThread(realCommand,
                                                      execCallback,
                                                      null,
                                                      outputVisible,
                                                      commandVisible,
                                                      sshCommandTimeout);
        } catch (final IOException e) {
            LOG.appError("execCommand: Can not execute command: "
                         + realCommand, "", e);
            return null;
        }
        execCommandThread.setPriority(Thread.MIN_PRIORITY);
        execCommandThread.start();
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
            execCommandThread = new ExecCommandThread(realCommand,
                                                      execCallback,
                                                      newOutputCallback,
                                                      outputVisible,
                                                      commandVisible,
                                                      sshCommandTimeout);
        } catch (final IOException e) {
            LOG.appError("execCommand: can not execute command: "
                         + realCommand, "", e);
            return null;
        }
        execCommandThread.setPriority(Thread.MIN_PRIORITY);
        execCommandThread.start();
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

    /**
     * This ServerHostKeyVerifier asks the user on how to proceed if a key
     * cannot befound in the in-memory database.
     */
    private class AdvancedVerifier implements ServerHostKeyVerifier {
        /** Verifies the keys. */
        @Override
        public boolean verifyServerHostKey(final String hostname,
                                           final int port,
                                           final String serverHostKeyAlgorithm,
                                           final byte[] serverHostKey)
        throws Exception {
            final String hostString = hostname;
            final String algo = serverHostKeyAlgorithm;
            final StringBuilder message = new StringBuilder(200);

            /* Check database */
            final int result =
                        Tools.getConfigData().getKnownHosts().verifyHostkey(
                                                      hostname,
                                                      serverHostKeyAlgorithm,
                                                      serverHostKey);

            switch (result) {
            case KnownHosts.HOSTKEY_IS_OK:
                return true;

            case KnownHosts.HOSTKEY_IS_NEW:
                message.append("Do you want to accept the hostkey (type ");
                message.append(algo);
                message.append(") from ");
                message.append(host);
                message.append(" ?\n");
                break;

            case KnownHosts.HOSTKEY_HAS_CHANGED:
                message.append("WARNING! Hostkey for ");
                message.append(hostString);
                message.append(" has changed!\nAccept anyway?\n");
                break;

            default:
                throw new IllegalStateException();
            }

            /* Include the fingerprints in the message */
            final String hexFingerprint =
                        KnownHosts.createHexFingerprint(serverHostKeyAlgorithm,
                                                        serverHostKey);
            final String bubblebabbleFingerprint =
                                    KnownHosts.createBubblebabbleFingerprint(
                                                      serverHostKeyAlgorithm,
                                                      serverHostKey);

            message.append("Hex Fingerprint: ");
            message.append(hexFingerprint);
            message.append("\nBubblebabble Fingerprint: ");
            message.append(bubblebabbleFingerprint);

            /* Now ask the user */
            final int choice =
                            sshGui.getConfirmDialogChoice(message.toString());
            if (sshGui.isConfirmDialogYes(choice)) {

                /* Be really paranoid. We use a hashed hostname entry */
                final String hashedHostname =
                                     KnownHosts.createHashedHostname(hostname);

                /* Add the hostkey to the in-memory database */
                Tools.getConfigData().getKnownHosts().addHostkey(
                                                new String[] {hashedHostname},
                                                serverHostKeyAlgorithm,
                                                serverHostKey);

                /* Also try to add the key to a known_host file */
                /* It dows this only in Linux.
                 * TODO: do this also for other OSes, when I find out the
                 * known_hosts locations.
                 */
                if (Tools.isWindows()) {
                    LOG.debug("verifyServerHostKey: not using known_hosts"
                              + " file, because this is Windows.");
                } else {
                    try {
                        KnownHosts.addHostkeyToFile(
                              new File(
                                    Tools.getConfigData().getKnownHostPath()),
                              new String[] {hashedHostname},
                              serverHostKeyAlgorithm,
                              serverHostKey);
                    } catch (final IOException ignore) {
                        LOG.appWarning("verifyServerHostKey: SSH "
                                       + "addHostKeyToFile failed "
                                       + ignore.getMessage());
                    }
                }

                return true;
            }

            if (sshGui.isConfirmDialogCancel(choice)) {
                throw new Exception(
                          "The user aborted the server hostkey verification.");
            }
            return false;
        }
    }

    /**
     * The logic that one has to implement if "keyboard-interactive"
     * autentication shall be supported.
     */
    private class InteractiveLogic implements InteractiveCallback {
        /** Prompt count. */
        private int promptCount = 0;
        /** To show error only once.  */
        private String lastError;

        /** Prepares a new {@code InteractiveLogic} object. */
        InteractiveLogic(final String lastError) {
            this.lastError = lastError;
        }

        /**
         * The callback may be invoked several times, depending on how many
         * questions-sets the server sends.
         */
        @Override
        public String[] replyToChallenge(final String name,
                                         final String instruction,
                                         final int numPrompts,
                                         final String[] prompt,
                                         final boolean[] echo)
        throws IOException {
            final String[] result = new String[numPrompts];

            for (int i = 0; i < numPrompts; i++) {
                /* Often, servers just send empty strings for "name" and
                 * "instruction" */
                final String[] content = new String[]{lastError,
                                                      name,
                                                      instruction,
                                                      "<html><font color=red>"
                                                      + prompt[i] + "</font>"
                                                      + "</html>"};

                if (lastError != null) {
                    /* show lastError only once */
                    lastError = null;
                }
                final String ans;
                if (lastPassword == null) {
                    ans = sshGui.enterSomethingDialog(
                                        "Keyboard Interactive Authentication",
                                        content,
                                        null,
                                        null,
                                        !echo[i]);

                    if (ans == null) {
                        throw new IOException("cancelled");
                    }
                    lastPassword = ans;
                    host.setSudoPassword(lastPassword);
                } else {
                    ans = lastPassword;
                    host.setSudoPassword(lastPassword);
                }
                result[i] = ans;
                promptCount++;
            }

            return result;
        }

        /**
         * We maintain a prompt counter - this enables the detection of
         * situations where the ssh server is signaling
         * "authentication failed" even though it did not send a single
         * prompt.
         */
        int getPromptCount() {
            return promptCount;
        }
    }

    /** Connection class that can cancel it's connection during openSession. */
    private static class MyConnection extends Connection {
        /** Creates new MyConnection object. */
        MyConnection(final String hostname, final int port) {
            super(hostname, port);
        }

        /** Cancel from application. */
        void dmcCancel() {
            /* public getChannelManager() { return cm }
               has to be added to the Connection.java till
               it's sorted out. */
            final ChannelManager cm = getChannelManager();
            if (cm != null) {
                cm.closeAllChannels();
            }
        }
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

    /**
     * The SSH-2 connection is established in this thread.
     * If we would not use a separate thread (e.g., put this code in
     * the event handler of the "Login" button) then the GUI would not
     * be responsive (missing window repaints if you move the window etc.)
     */
    private class ConnectionThread extends Thread {
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

        private void authenticate(final MyConnection conn) throws IOException {
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
            final boolean noPassphrase = Tools.getConfigData().isNoPassphrase();
            while (!cancelIt) {
                if (lastPassword == null) {
                    lastPassword =
                                Tools.getConfigData().getAutoOptionHost("pw");
                    if (lastPassword == null) {
                        lastPassword =
                              Tools.getConfigData().getAutoOptionCluster("pw");
                    }
                }
                if (lastPassword == null) {
                    if (enablePublicKey
                        && conn.isAuthMethodAvailable(username, "publickey")) {
                        final File dsaKey = new File(
                                         Tools.getConfigData().getIdDSAPath());
                        final File rsaKey = new File(
                                         Tools.getConfigData().getIdRSAPath());
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
                                    } catch (final Exception e) {
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
                                    conn.connect(new AdvancedVerifier(),
                                                 connectTimeout,
                                                 kexTimeout);
                                }


                                if (rsaKey.exists()) {
                                    try {
                                        res = conn.authenticateWithPublicKey(
                                                                      username,
                                                                      rsaKey,
                                                                      key);
                                    } catch (final Exception e) {
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
                                    conn.connect(new AdvancedVerifier(),
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
                                } catch (final Exception e) {
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
                                conn.connect(new AdvancedVerifier(),
                                             connectTimeout,
                                             kexTimeout);
                            }

                            if (rsaKey.exists()) {
                                try {
                                    res = conn.authenticateWithPublicKey(
                                                                      username,
                                                                      rsaKey,
                                                                      key);
                                } catch (final Exception e) {
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
                                conn.connect(new AdvancedVerifier(),
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
                    final InteractiveLogic il = new InteractiveLogic(lastError);

                    final boolean res =
                             conn.authenticateWithKeyboardInteractive(username,
                                                                      il);

                    if (res) {
                        lastRSAKey = null;
                        lastDSAKey = null;
                        break;
                    } else {
                        lastPassword = null;
                    }

                    if (il.getPromptCount() == 0) {
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
                mConnectionLock.lock();
                try {
                    connection = null;
                } finally {
                    mConnectionLock.unlock();
                }
                host.setConnected();
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
            final MyConnection conn = new MyConnection(hostname,
                                                       host.getSSHPortInt());
            disconnectForGood = false;

            try {
                if (hostname == null) {
                    throw new IOException("hostname is not set");
                }
                /* connect and verify server host key (with callback) */
                LOG.debug2("run: verify host keys: " + hostname);
                final String[] hostkeyAlgos =
                    Tools.getConfigData().getKnownHosts().
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
                conn.connect(new AdvancedVerifier(),
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
                mConnectionLock.lock();
                try {
                    connection = null;
                } finally {
                    mConnectionLock.unlock();
                }
                host.setConnected();
            }
        }
    }

    /**
     * Enter sudo password.
     * Return whether the dialog was cancelled.
     */
    private boolean enterSudoPassword() {
        if (host.isUseSudo() != null && host.isUseSudo()) {
            final String lastError = "";
            final String lastSudoPwd = host.getSudoPassword();
            final String sudoPwd = sshGui.enterSomethingDialog(
                     Tools.getString("SSH.SudoAuthentication"),
                    new String[] {lastError,
                                  "<html>"
                                  + host.getName()
                                  + Tools.getString(
                                      "SSH.Enter.sudoPassword")
                                  + "</html>"},
                    null,
                    null,
                    true);
            if (sudoPwd == null) {
                /* cancelled */
                return true;
            } else {
                host.setSudoPassword(sudoPwd);
            }
        }
        return false;
    }

    /** Installs gui-helper on the remote host. */
    public void installGuiHelper() {
        if (!Tools.getConfigData().getKeepHelper()) {
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
        final SSHOutput ret = execCommandAndWait(
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
        final String stacktrace = Tools.getStackTrace();
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
    void startVncPortForwarding(final String remoteHost,
                                final int remotePort)
        throws IOException {
        final int localPort =
                        remotePort + Tools.getConfigData().getVncPortOffset();
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
    void stopVncPortForwarding(final int remotePort)
        throws IOException {
        final int localPort =
                        remotePort + Tools.getConfigData().getVncPortOffset();
        try {
            localPortForwarder.close();
        } catch (final IOException e) {
            throw e;
        }
    }

    /** Class that holds output of ssh command. */
    public static final class SSHOutput {
        /** Output string. */
        private final String output;
        /** Exit code. */
        private final int exitCode;
        /** Creates new SSHOutput object. */
        SSHOutput(final String output, final int exitCode) {
            this.output = output;
            this.exitCode = exitCode;
        }

        /** Returns output string. */
        public String getOutput() {
            return output;
        }

        /** Returns exit code. */
        public int getExitCode() {
            return exitCode;
        }

    }

    /** Returns whether connection was canceled. */
    public boolean isConnectionCanceled() {
        return disconnectForGood;
    }
}
