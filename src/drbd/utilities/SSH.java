/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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


package drbd.utilities;

import drbd.data.Host;
import drbd.gui.SSHGui;
import drbd.gui.ProgressBar;

import java.util.Map;
import java.util.HashMap;

import javax.swing.SwingUtilities;

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

import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * Verifying server hostkeys with an existing known_hosts file
 * Displaying fingerprints of server hostkeys.
 * Adding a server hostkey to a known_hosts file (+hashing the hostname
 * for security).
 * Authentication with DSA, RSA, password and keyboard-interactive methods.
 */
public class SSH {
    /** SSHGui object for enter password dialogs etc. */
    private SSHGui sshGui;
    /** Callback when connection is failed or properly closed. */
    private ConnectionCallback callback;
    /** Host data object. */
    private Host host;
    /** This SSH connection object. */
    private volatile Connection connection = null;
    /** Connection thread object. */
    private volatile ConnectionThread connectionThread = null;
    /** Progress bar object. */
    private ProgressBar progressBar = null;
    /** Cache for the commands. */
    private final Map<String, String> commandCache =
                                                new HashMap<String, String>();
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
    private final Mutex mConnectionLock = new Mutex();
    /** Connection thread mutex. */
    private final Mutex mConnectionThreadLock = new Mutex();
    /** Local port forwarder. */
    private LocalPortForwarder localPortForwarder = null;
    /** Default timeout for SSH commands. */
    public static final int DEFAULT_COMMAND_TIMEOUT =
                                    Tools.getDefaultInt("SSH.Command.Timeout");
    /** Default timeout for SSH commands. */
    public static final int DEFAULT_COMMAND_TIMEOUT_LONG =
                               Tools.getDefaultInt("SSH.Command.Timeout.Long");

    /**
     * Reconnect.
     */
    public final boolean reconnect() {
        try {
            mConnectionThreadLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (connectionThread != null) {
            try {
                final ConnectionThread ct = connectionThread;
                mConnectionThreadLock.release();
                ct.join();
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            mConnectionThreadLock.release();
        }
        if (disconnectForGood) {
            return false;
        }
        if (!isConnected()) {
            Tools.debug(this, "connecting: " + host.getName(), 1);
            this.callback = null;
            this.progressBar = null;
            this.sshGui = new SSHGui(Tools.getGUIData().getMainFrame(),
                                     host,
                                     null);
            host.getTerminalPanel().addCommand("ssh " + host.getUserAtHost());
            final ConnectionThread ct = new ConnectionThread();
            try {
                mConnectionThreadLock.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            connectionThread = ct;
            mConnectionThreadLock.release();
            ct.start();
        }
        return true;
    }

    /**
     * Connects the host.
     */
    public final void connect(final SSHGui sshGui,
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
        try {
            mConnectionThreadLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        connectionThread = ct;
        mConnectionThreadLock.release();
        ct.start();
    }

    /**
     * Sets passwords that will be tried first while connecting, but only if
     * they were not set before.
     */
    public final void setPasswords(final String lastDSAKey,
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

    /**
     * Returns last successful dsa key.
     */
    public final String getLastDSAKey() {
        return lastDSAKey;
    }

    /**
     * Returns last successful rsa key.
     */
    public final String getLastRSAKey() {
        return lastRSAKey;
    }

    /**
     * Returns last successful password.
     */
    public final String getLastPassword() {
        return lastPassword;
    }

    /**
     * Waits till connection is established or is failed.
     */
    public final void waitForConnection() {
        try {
            mConnectionThreadLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (connectionThread != null) {
            try {
                final ConnectionThread ct = connectionThread;
                mConnectionThreadLock.release();
                ct.join();
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            mConnectionThreadLock.release();
        }
    }


    /**
     * Connects the host.
     */
    public final void connect(final SSHGui sshGuiT,
                              final ProgressBar progressBar,
                              final ConnectionCallback callbackT,
                              final Host hostT) {
        this.progressBar = progressBar;
        connect(sshGuiT, callbackT, hostT);
    }

    /**
     * Cancels the creating of connection to the sshd.
     */
    public final void cancelConnection() {
        try {
            mConnectionThreadLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (connectionThread != null) {
            connectionThread.cancel();
        }
        mConnectionThreadLock.release();
        Tools.debug(this, "SSH cancel", 1);
        final String message = "canceled";
        Tools.debug(this, message, 1);
        host.getTerminalPanel().addCommandOutput(message + "\n");
        host.getTerminalPanel().nextCommand();
        connectionFailed = true;
        if (callback != null) {
              callback.doneError(message);
        }

        // connection will be established anyway after cancel in the
        // background and it will be closed after that, because conn.connect
        // call cannot be interrupted for now.
    }

    /**
     * Cancels the session (execution of command).
     */
    public final void cancelSession(final ExecCommandThread execCommandThread) {
        execCommandThread.cancel();
        Tools.debug(this, "session cancel", 1);
        final String message = "canceled";
        Tools.debug(this, message, 1);
        //sess.close();
        //sess = null;
        host.getTerminalPanel().addCommandOutput("\n");
        host.getTerminalPanel().nextCommand();
    }

    /**
     * Disconnects this host if it has been connected.
     */
    public final void disconnect() {
        try {
            mConnectionLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (connection != null) {
            disconnectForGood = true;
            connection.close();
            connection = null;
            mConnectionLock.release();
            Tools.debug(this, "disconnecting: " + host.getName(), 0);
            host.getTerminalPanel().addCommand("logout");
            host.getTerminalPanel().nextCommand();
        } else {
            mConnectionLock.release();
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
    public final void forceReconnect() {
        try {
            mConnectionLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (connection != null) {
            Tools.printStackTrace("force reconnect");
            connection = null;
            mConnectionLock.release();
            Tools.debug(this, "force reconnecting: " + host.getName(), 0);
            host.getTerminalPanel().addCommand("logout");
            host.getTerminalPanel().nextCommand();
        } else {
            mConnectionLock.release();
        }
    }

    /**
     * Force disconnection.
     */
    public final void forceDisconnect() {
        try {
            mConnectionLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (connection != null) {
            disconnectForGood = true;
            connection = null;
            mConnectionLock.release();
            Tools.debug(this, "force disconnecting: " + host.getName(), 0);
            host.getTerminalPanel().addCommand("logout");
            host.getTerminalPanel().nextCommand();
        } else {
            mConnectionLock.release();
        }
    }

    /**
     * Returns true if connection is established.
     */
    public final boolean isConnected() {
        try {
            mConnectionLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final boolean ret = connection != null;
        mConnectionLock.release();
        return ret;
    }

    /**
     * Returns true if connection is established.
     */
    public final boolean isConnectionFailed() {
        return connectionFailed;
    }

    /**
     * This class is a thread that executes commands.
     */
    public class ExecCommandThread extends Thread {
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
            this.command = command;
            this.outputVisible = outputVisible;
            int exitCode = 100;
            final StringBuffer res = new StringBuffer("");
            try {
                mConnectionLock.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (connection == null) {
                mConnectionLock.release();
                return new SSHOutput("SSH.NotConnected", 1);
            } else {
                mConnectionLock.release();
            }
            if ("installGuiHelper".equals(command)) {
                installGuiHelper();
                return new SSHOutput("", 0);
            }
            try {
                final Session thisSession = sess;
                if (thisSession == null) {
                    return new SSHOutput("", 130);
                }
                /* requestPTY mixes stdout and strerr together, but it works
                better at the moment. */
                if (command.indexOf("/etc/init.d/openais start") < 0
                    && command.indexOf("/etc/init.d/openais reload") < 0) {
                    /* aisexec does not work when pty is requested for some
                     * reason, so here is the workaround. */
                    thisSession.requestPTY("dumb", 0, 0, 0, 0, null);
                }
                Tools.debug(this, "exec command: "
                                  + host.getName()
                                  + ": "
                                  + host.getSudoCommand(
                                               host.getHoppedCommand(command)),
                                  2);
                thisSession.execCommand(host.getSudoCommand(
                                            host.getHoppedCommand(command)));

                final InputStream stdout = thisSession.getStdout();
                final InputStream stderr = thisSession.getStderr();
                //byte[] buff = new byte[8192];
                final byte[] buff = new byte[EXEC_OUTPUT_BUFFER_SIZE];
                while (true) {
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
                            throw new IOException(
                                "Canceled while waiting for data from peer.");
                        }

                        if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                                /* A timeout occured. */
                                Tools.info("SSH timeout");
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
                    final StringBuffer output = new StringBuffer("");
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

                    /* stderr */
                    final StringBuffer errOutput = new StringBuffer("");
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
                //Tools.debug(this, "exitCode: " + exitCode);
                thisSession.close();
                sess = null;
            } catch (IOException e) {
                exitCode = ERROR_EXIT_CODE;
                //e.printStackTrace();
            }
            if (exitCode == 0) {
                commandCache.put(command, res.toString());
            }
            return new SSHOutput(res.toString(), exitCode);
        }

        /**
         * Cancel the session.
         */
        public final void cancel() {
            cancelIt = true;
            //TODO need lock for session
            final Session thisSession = sess;
            sess = null;
            if (thisSession != null) {
                thisSession.close();
            }
        }

        /**
         * Executes a command in a thread.
         */
        public ExecCommandThread(final String command,
                                 final ExecCallback execCallback,
                                 final NewOutputCallback newOutputCallback,
                                 final boolean outputVisible,
                                 final boolean commandVisible,
                                 final int sshCommandTimeout)
        throws java.io.IOException {
            super();
            this.command = command;
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
        public final void run() {
            if (reconnect()) {
                try {
                    mConnectionLock.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (connection == null) {
                    mConnectionLock.release();
                    if (execCallback != null) {
                        execCallback.doneError("not connected", 139);
                    }
                } else {
                    final Connection conn = connection;
                    mConnectionLock.release();
                    exec(conn);
                }
            }
        }

        /**
         * Executes the command.
         */
        private void exec(final Connection conn) {
            // ;;; separates commands, that are to be executed one after one,
            // if previous command has finished successfully.
            final String[] commands = command.split(";;;");
            final StringBuffer ans = new StringBuffer("");
            for (int i = 0; i < commands.length; i++) {
                try {
                    // TODO: it may hang here if we lost connection
                    sess = conn.openSession();
                } catch (java.io.IOException e) {
                    try {
                        mConnectionLock.acquire();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    connection = null;
                    mConnectionLock.release();
                    Tools.appWarning("Could not open session");
                    break;
                }
                commands[i].trim();
                //Tools.commandLock();
                if (outputVisible) {
                    final String consoleCommand = host.replaceVars(commands[i],
                                                                   true);
                    if (outputVisible && commandVisible) {
                        host.getTerminalPanel().addCommand(consoleCommand);
                    }
                }
                final SSHOutput ret = execOneCommand(commands[i],
                                                     outputVisible);
                ans.append(ret.getOutput());

                final int exitCode = ret.getExitCode();
                // don't execute after error
                if (exitCode != 0) {
                    if (execCallback != null) {
                        if (outputVisible && commandVisible) {
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
    public final SSHOutput execCommandAndWait(final String command,
                                              final boolean outputVisible,
                                              final boolean commandVisible,
                                              final int sshCommandTimeout) {

        if (host == null) {
            return new SSHOutput("", 101);
        }
        ExecCommandThread execCommandThread;
        final String[] answer = new String[]{""};
        final Integer[] exitCode = new Integer[]{100};
        try {
            execCommandThread = new ExecCommandThread(
                            command,
                            new ExecCallback() {
                                public void done(final String ans) {
                                    answer[0] = ans;
                                    exitCode[0] = 0;
                                }
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
        } catch (java.io.IOException e) {
            Tools.appError("Can not execute command: " + command, "", e);
            return new SSHOutput("", 102);
        }
        execCommandThread.start();
        try {
            execCommandThread.join();
        } catch (java.lang.InterruptedException e) {
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
     * @param cacheIt
     *          whether the output of the command should be cached
     * @param outputVisible
     *          whether the output of the command should be visible
     *
     * @return command thread
     */
    public final ExecCommandThread execCommand(final String command,
                                               final ExecCallback execCallback,
                                               final boolean cacheIt,
                                               final boolean outputVisible,
                                               final boolean commandVisible,
                                               final int sshCommandTimeout) {

        if (host == null) {
            return null;
        }
        final String realCommand = host.replaceVars(command);
        Tools.debug(this, "real command: " + realCommand, 2);
        if (cacheIt && commandCache.containsKey(realCommand)) {
            execCallback.done(commandCache.get(realCommand));
            return null;
        }
        ExecCommandThread execCommandThread;
        try {
            execCommandThread = new ExecCommandThread(realCommand,
                                                      execCallback,
                                                      null,
                                                      outputVisible,
                                                      commandVisible,
                                                      sshCommandTimeout);
        } catch (java.io.IOException e) {
            Tools.appError("Can not execute command: " + realCommand, "", e);
            return null;
        }
        execCommandThread.setPriority(Thread.MIN_PRIORITY);
        execCommandThread.start();
        return execCommandThread;
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
     * @param commandVisible
     *          whether the command should be visible
     *
     * @return thread
     */
    public final ExecCommandThread execCommand(final String command,
                                               final ExecCallback execCallback,
                                               final boolean outputVisible,
                                               final boolean commandVisible,
                                               final int sshCommandTimeout) {
        return execCommand(command,
                           execCallback,
                           false,
                           outputVisible,
                           commandVisible,
                           sshCommandTimeout);
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
    public final ExecCommandThread execCommand(
                                     final String command,
                                     final ExecCallback execCallback,
                                     final NewOutputCallback newOutputCallback,
                                     final boolean outputVisible,
                                     final boolean commandVisible,
                                     final int sshCommandTimeout) {
        final String realCommand = host.replaceVars(command);
        ExecCommandThread execCommandThread;
        try {
            execCommandThread = new ExecCommandThread(realCommand,
                                                      execCallback,
                                                      newOutputCallback,
                                                      outputVisible,
                                                      commandVisible,
                                                      sshCommandTimeout);
        } catch (java.io.IOException e) {
            Tools.appError("Can not execute command: " + realCommand, "", e);
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
     * @param cacheIt
     *          whether the output of the command should be cached
     * @param outputVisible
     *          whether the output of the command should be visible
     * @param commandVisible
     *          whether the command should be visible
     *
     * @return command thread
     */
    public final ExecCommandThread execCommand(final String command,
                                               final ProgressBar progressBar,
                                               final ExecCallback execCallback,
                                               final boolean cacheIt,
                                               final boolean outputVisible,
                                               final boolean commandVisible,
                                               final int sshCommandTimeout) {
        Tools.debug(this, "execCommand with progress bar", 2);
        this.progressBar = progressBar;
        if (progressBar != null) {
            progressBar.start(0);
            progressBar.hold();
        }
        return execCommand(command,
                           execCallback,
                           cacheIt,
                           outputVisible,
                           commandVisible,
                           sshCommandTimeout);
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
    public final ExecCommandThread execCommand(final String command,
                                               final ProgressBar pB,
                                               final ExecCallback execCallback,
                                               final boolean outputVisible,
                                               final boolean commandVisible,
                                               final int sshCommandTimeout) {
        return execCommand(command,
                           pB,
                           execCallback,
                           false,
                           outputVisible,
                           commandVisible,
                           sshCommandTimeout);
    }

    /**
     * This ServerHostKeyVerifier asks the user on how to proceed if a key
     * cannot befound in the in-memory database.
     */
    class AdvancedVerifier implements ServerHostKeyVerifier {
        /** Verifies the keys. */
        public boolean verifyServerHostKey(final String hostname,
                                           final int port,
                                           final String serverHostKeyAlgorithm,
                                           final byte[] serverHostKey)
        throws Exception {
            final String hostString = hostname;
            final String algo = serverHostKeyAlgorithm;
            final StringBuffer message = new StringBuffer(200);

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
                if (Tools.isLinux()) {
                    try {
                        KnownHosts.addHostkeyToFile(
                              new File(
                                    Tools.getConfigData().getKnownHostPath()),
                              new String[] {hashedHostname},
                              serverHostKeyAlgorithm,
                              serverHostKey);
                    } catch (IOException ignore) {
                        Tools.appError("SSH.AddHostKeyToFile.Failed",
                                       "",
                                       ignore);
                    }
                } else {
                    Tools.debug(this, "Not using known_hosts file, because"
                                      + " this is not a Linux.");
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
    class InteractiveLogic implements InteractiveCallback {
        /** Prompt count. */
        private int promptCount = 0;
        /** To show error only once.  */
        private String lastError;

        /**
         * Prepares a new <code>InteractiveLogic</code> object.
         */
        public InteractiveLogic(final String lastError) {
            this.lastError = lastError;
        }

        /**
         * The callback may be invoked several times, depending on how many
         * questions-sets the server sends. */
        public String[] replyToChallenge(final String name,
                                         final String instruction,
                                         final int numPrompts,
                                         final String[] prompt,
                                         final boolean[] echo)
        throws IOException {
            String[] result = new String[numPrompts];

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
                String ans;
                if (lastPassword == null) {
                    ans = sshGui.enterSomethingDialog(
                                        "Keyboard Interactive Authentication",
                                        content,
                                        null,
                                        null,
                                        !echo[i]);
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
        public int getPromptCount() {
            return promptCount;
        }
    }

    /**
     * The SSH-2 connection is established in this thread.
     * If we would not use a separate thread (e.g., put this code in
     * the event handler of the "Login" button) then the GUI would not
     * be reponsive (missing window repaints if you move the window etc.)
     */
    class ConnectionThread extends Thread {
        /** Username with which it will be connected. */
        private final String username;
        /** Hostname of the host to which it will be connect. */
        private final String hostname;
        /** Cancel the connecting. */
        private boolean cancelIt = false;

        /** Prepares a new <code>ConnectionThread</code> object. */
        public ConnectionThread() {
            super();
            username = host.getFirstUsername();
            hostname = host.getFirstIp();
        }

        /** Cancel the connecting. */
        public void cancel() {
            cancelIt = true;
        }

        /** Start connection in the thread. */
        public void run() {
            if (callback != null && isConnected()) {
                callback.done(1);
            }
            host.setSudoPassword("");
            final Connection conn = new Connection(hostname,
                                                   host.getSSHPortInt());
            disconnectForGood = false;

            try {
                /* connect and verify server host key (with callback) */
                Tools.debug(this, "verify host keys: " + hostname, 1);
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
                boolean enableKeyboardInteractive = true;
                boolean enablePublicKey = true;
                String lastError = null;
                int publicKeyTry = 3; /* how many times to try the public key
                                         authentification */
                int passwdTry = 3;    /* how many times to try the password
                                         authentification */
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
                            && conn.isAuthMethodAvailable(username,
                                                          "publickey")) {
                            final File dsaKey = new File(
                                         Tools.getConfigData().getIdDSAPath());
                            final File rsaKey = new File(
                                         Tools.getConfigData().getIdRSAPath());
                            boolean res = false;
                            if (dsaKey.exists() || rsaKey.exists()) {
                                String key = "";
                                if (lastDSAKey != null) {
                                    key = lastDSAKey;
                                }
                                /* try first passwordless authentication.  */
                                if (lastRSAKey == null && dsaKey.exists()) {
                                    try {
                                        res = conn.authenticateWithPublicKey(
                                                                      username,
                                                                      dsaKey,
                                                                      key);
                                    } catch (Exception e) {
                                        lastDSAKey = null;
                                        Tools.debug(this,
                                                    "dsa passwordless failed");
                                    }
                                    if (res) {
                                        Tools.debug(
                                           this,
                                           "dsa passwordless auth successful");
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
                                        res =
                                           conn.authenticateWithPublicKey(
                                                                      username,
                                                                      rsaKey,
                                                                      key);
                                    } catch (Exception e) {
                                        lastRSAKey = null;
                                        Tools.debug(this,
                                                    "rsa passwordless failed");
                                    }
                                    if (res) {
                                        Tools.debug(
                                           this,
                                           "rsa passwordless auth successful");
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

                                key = sshGui.enterSomethingDialog(
                                        Tools.getString(
                                                 "SSH.RSA.DSA.Authentication"),
                                        new String[] {lastError,
                                                      "<html>"
                                                      + Tools.getString(
                                                       "SSH.Enter.passphrase")
                                                      + "</html>",

                                                      },
                                        "<html>"
                                        + Tools.getString(
                                                    "SSH.Enter.passphrase2")
                                        + "</html>",
                                        Tools.getDefault("SSH.PublicKey"),
                                        true);
                                if (key == null) {
                                    cancelIt = true;
                                    break;
                                }
                                if ("".equals(key)) {
                                    publicKeyTry = 0;
                                }
                                if (dsaKey.exists()) {
                                    try {
                                        res =
                                           conn.authenticateWithPublicKey(
                                                                      username,
                                                                      dsaKey,
                                                                      key);
                                    } catch (Exception e) {
                                            lastDSAKey = null;
                                            Tools.debug(this,
                                                        "dsa key auth failed");
                                    }
                                    if (res) {
                                        Tools.debug(this,
                                                    "dsa key auth successful");
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
                                        res =
                                           conn.authenticateWithPublicKey(
                                                                      username,
                                                                      rsaKey,
                                                                      key);
                                    } catch (Exception e) {
                                        lastRSAKey = null;
                                        Tools.debug(this,
                                                    "rsa key auth failed");
                                    }
                                    if (res) {
                                        Tools.debug(this,
                                                    "rsa key auth successful");
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
                        && conn.isAuthMethodAvailable(
                                                    username,
                                                    "keyboard-interactive")) {
                        final InteractiveLogic il =
                                               new InteractiveLogic(lastError);

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
                            lastError = "Keyboard-interactive does not work.";

                            /* do not try this again */
                            enableKeyboardInteractive = false;
                        } else {
                            /* try again, if possible */
                            lastError = "Keyboard-interactive auth failed.";
                        }
                        continue;
                    }

                    if (conn.isAuthMethodAvailable(username, "password")) {
                        String ans;
                        if (lastPassword != null) {
                            ans = lastPassword;
                        } else {
                            ans = sshGui.enterSomethingDialog(
                                    "Password Authentication",
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
                        }

                        if (ans == null) {
                            throw new IOException("Login aborted by user");
                        }
                        if ("".equals(ans)) {
                            passwdTry = 0;
                        }
                        final boolean res =
                                        conn.authenticateWithPassword(username,
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
                        lastError = "Password authentication failed.";
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
                    Tools.debug(
                      this,
                      "closing established connection because it was canceled");
                    try {
                        mConnectionThreadLock.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    connectionThread = null;
                    mConnectionThreadLock.release();
                    host.setConnected();
                    if (callback != null) {
                        callback.doneError("");
                    }
                    try {
                        mConnectionLock.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    connection = null;
                    mConnectionLock.release();
                    host.setConnected();
                } else {
                    //  authentication ok.
                    try {
                        mConnectionLock.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    connection = conn;
                    mConnectionLock.release();
                    host.setConnected();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            host.getTerminalPanel().nextCommand();
                        }
                    });
                    try {
                        mConnectionThreadLock.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    connectionThread = null;
                    mConnectionThreadLock.release();
                    if (host.isUseSudo() != null && host.isUseSudo()) {
                        lastError = "";
                        while (true) {
                            final String lastSudoPwd = host.getSudoPassword();
                            if (lastSudoPwd != null
                                && !"".equals(lastSudoPwd)) {
                                final SSHOutput ret =
                                    execCommandAndWait(
                                                 "true",
                                                 true,
                                                 false,
                                                 10000);
                                final int ec = ret.getExitCode();
                                if (ec == 0) {
                                    break;
                                }
                                host.setSudoPassword(null);
                            }
                            final String sudoPwd = sshGui.enterSomethingDialog(
                                    "Sudo Authentication",
                                    new String[] {lastError,
                                                  "<html>"
                                                  + host.getName()
                                                  + Tools.getString(
                                                      "SSH.Enter.sudoPassword")
                                                  + "</html>"},
                                    null,
                                    null,
                                    true);
                            host.setSudoPassword(sudoPwd);
                            if (sudoPwd == null) {
                                try {
                                    mConnectionLock.acquire();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                connection = null;
                                mConnectionLock.release();
                                host.setConnected();
                                break;
                            }
                            lastError = "wrong password";
                        }
                    }
                    host.setConnected();
                    if (callback != null) {
                        callback.done(0);
                    }
                    Tools.debug(this, "authentication ok");
                }
            } catch (IOException e) {
                Tools.debug(this, "connecting: " + e.getMessage(), 1);
                connectionFailed = true;
                if (!cancelIt) {
                    host.getTerminalPanel().addCommandOutput(e.getMessage()
                                                             + "\n");
                    host.getTerminalPanel().nextCommand();
                    if (callback != null) {
                        callback.doneError(e.getMessage());
                    }
                }
                try {
                    mConnectionThreadLock.acquire();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                connectionThread = null;
                mConnectionThreadLock.release();
                try {
                    mConnectionLock.acquire();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                connection = null;
                mConnectionLock.release();
                host.setConnected();
            }
        }
    }

    /**
     * Installs gui-helper on the remote host.
     */
    public final void installGuiHelper() {
        if (!Tools.getConfigData().getKeepHelper()) {
            final String fileName = "/help-progs/drbd-gui-helper";
            final String file = Tools.getFile(fileName);
            if (file != null) {
                scp(file,
                    "@GUI-HELPER@",
                    "0700",
                    false);
            }
        }
    }

    /**
     * Installs test suite on the remote host.
     */
    public final void installTestFiles(final int index) {
        final String fileName = "drbd-mc-test.tar";
        final Connection conn = connection;
        if (conn == null) {
            return;
        }
        final SCPClient scpClient = new SCPClient(conn);
        final String file = Tools.getFile("/" + fileName);
        try {
            scpClient.put(file.getBytes(), fileName, "/tmp");
        } catch (IOException e) {
            Tools.appError("could not copy: " + fileName, "", e);
            return;
        }
        final SSHOutput ret = execCommandAndWait(
                                       "tar xf /tmp/drbd-mc-test.tar -C /tmp/",
                                       false,
                                       false,
                                       60);
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
    public final void createConfig(final String config,
                                   final String fileName,
                                   final String dir,
                                   final String mode,
                                   final boolean makeBackup) {
        scp(config, dir + fileName, mode, makeBackup);
    }

    /**
     * Copies file to the /tmp/ dir on the remote host.
     *
     * @param fileContent
     *          content of the file as string
     * @param remoteFilename
     *          new file name on the other host
     */
    public final void scp(final String fileContent,
                          final String remoteFilename,
                          final String mode,
                          final boolean makeBackup) {
        final StringBuffer commands = new StringBuffer(40);
        if (makeBackup) {
            commands.append("cp ");
            commands.append(remoteFilename);
            commands.append("{,.`date +'%s'`};");
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
        final Thread t = execCommand(
                            commands.toString()
                            + "echo \""
                            + host.escapeQuotes(fileContent, 1)
                            + "\">" + remoteFilename
                            + ";"
                            + "chmod " + mode + " " + remoteFilename,
                            new ExecCallback() {
                                public void done(final String ans) {
                                    /* ok */
                                }
                                public void doneError(final String ans,
                                                      final int exitCode) {
                                    Tools.sshError(host, "scp", ans, exitCode);
                                }
                            },
                            false,
                            true,
                            10000); /* smaller timeout */
        try {
            t.join();
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Starts port forwarding for vnc.
     */
    public final void startVncPortForwarding(final String remoteHost,
                                             final int remotePort)
        throws java.io.IOException {
        final int localPort =
                        remotePort + Tools.getConfigData().getVncPortOffset();
        try {
            localPortForwarder =
                connection.createLocalPortForwarder(localPort,
                                                    "127.0.0.1",
                                                    remotePort);
        } catch (final java.io.IOException e) {
            throw e;
        }
    }

    /**
     * Stops port forwarding for vnc.
     */
    public final void stopVncPortForwarding(final int remotePort)
        throws java.io.IOException {
        final int localPort =
                        remotePort + Tools.getConfigData().getVncPortOffset();
        try {
            localPortForwarder.close();
        } catch (final java.io.IOException e) {
            throw e;
        }
    }

    /**
     * Class that holds output of ssh command.
     */
    public final class SSHOutput {
        /** Output string. */
        private final String output;
        /** Exit code. */
        private final int exitCode;
        /** Creates new SSHOutput object. */
        public SSHOutput(final String output, final int exitCode) {
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
}
