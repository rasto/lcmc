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
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.ServerHostKeyVerifier;
import com.trilead.ssh2.InteractiveCallback;
import com.trilead.ssh2.Session;
import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.KnownHosts;
import com.trilead.ssh2.ConnectionMonitor;

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

    /**
     * Reconnect.
     */
    public final boolean reconnect() {
        if (connectionThread != null) {
            try {
                connectionThread.join();
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (disconnectForGood) {
            return false;
        }
        if (!isConnected()) {
            Tools.debug(this, "connecting: " + host.getName());
            this.callback = null;
            this.progressBar = null;
            this.sshGui = new SSHGui(Tools.getGUIData().getMainFrame(),
                                     host,
                                     null);
            host.getTerminalPanel().addCommand("ssh " + host.getUserAtHost());
            connectionThread = new ConnectionThread();
            connectionThread.start();
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
        connectionThread = new ConnectionThread();
        connectionThread.start();
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
        if (connectionThread != null) {
            try {
                connectionThread.join();
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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
        if (connectionThread != null) {
            connectionThread.cancel();
        }
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
        if (connection != null) {
            disconnectForGood = true;
            Tools.debug(this, "disconnecting: " + host.getName(), 0);
            connection.close();
            connection = null;
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
    public final void forceReconnect() {
        if (connection != null) {
            Tools.debug(this, "force reconnecting: " + host.getName(), 0);
            connection = null;
            host.getTerminalPanel().addCommand("logout");
            host.getTerminalPanel().nextCommand();
        }
    }

    /**
     * Force disconnection.
     */
    public final void forceDisconnect() {
        if (connection != null) {
            disconnectForGood = true;
            Tools.debug(this, "force disconnecting: " + host.getName(), 0);
            connection = null;
            host.getTerminalPanel().addCommand("logout");
            host.getTerminalPanel().nextCommand();
        }
    }

    /**
     * Returns true if connection is established.
     */
    public final boolean isConnected() {
        return connection != null;
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
        /** Timeout for ssh command */
        private int sshCommandTimeout;

       /**
        * Executes command on the host.
        *
        * @param command
        *          command that will be executed.
        * @param outputVisible
        *          whether the output of the command should be visible
        */
        private Object[] execOneCommand(final String command,
                                        final boolean outputVisible) {
            this.command = command;
            this.outputVisible = outputVisible;
            Integer exitCode = 100;
            final StringBuffer res = new StringBuffer("");
            if (connection == null) {
                return new Object[]{"SSH.NotConnected", 1};
            }
            if ("installGuiHelper".equals(command)) {
                installGuiHelper();
                return new Object[]{"", 0};
            }
            try {
                if (sess == null) {
                    return new Object[]{"", 130};
                }
                sess.requestPTY("dumb", 90, 30, 0, 0, null);
                Tools.debug(this, "exec command: "
                                  + host.getName()
                                  + ": "
                                  + host.getHoppedCommand(command),
                                  1);
                sess.execCommand(host.getHoppedCommand(command));
                final InputStream stdout = sess.getStdout();
                final InputStream stderr = sess.getStderr();
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
                            conditions = sess.waitForCondition(
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
                    if (newOutputCallback != null && !cancelIt) {
                        newOutputCallback.output(output.toString());
                    }

                    if (cancelIt) {
                        return new Object[]{"", 130};
                    }

                    if (newOutputCallback == null) {
                        res.append(output);
                    }

                    /* stderr */
                    final StringBuffer errOutput = new StringBuffer("");
                    //while (stderr.available() > 0) {
                    //    // this is unreachable.
                    //    // stdout and stderr are mixed in the stdout
                    //    // if pty is requested.
                    //    int len = stderr.read(buff);
                    //    if (len > 0) {
                    //        for (int i = 0; i<len; i++) {
                    //            char c = (char) (buff[i] & 0xFF);
                    //            errOutput += c;
                    //        }
                    //        if (outputVisible)
                    //            host.getTerminalPanel().addContentErr(buff,
                    //                                                  len);
                    //    }
                    //}
                    res.append(errOutput);
                }
                if (outputVisible) {
                    host.getTerminalPanel().nextCommand();
                }
                sess.waitForCondition(ChannelCondition.EXIT_STATUS, 10000);
                exitCode = sess.getExitStatus();
                //Tools.debug(this, "exitCode: " + exitCode);
                sess.close();
                sess = null;
            } catch (IOException e) {
                exitCode = ERROR_EXIT_CODE;
                //e.printStackTrace();
            }
            if (exitCode == null) {
                exitCode = 0;
            }
            if (exitCode == 0) {
                commandCache.put(command, res.toString());
            }
            return new Object[]{res, exitCode};
        }

        /**
         * Cancel the session.
         */
        public final void cancel() {
            cancelIt = true;
            if (sess != null) {
                sess.close();
                sess = null;
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
                if (connection == null) {
                    if (execCallback != null) {
                        execCallback.doneError("not connected", 0);
                    }
                } else {
                    try {
                        sess = connection.openSession();
                    } catch (java.io.IOException e) {
                        connection = null;
                        Tools.appWarning("Could not open session");
                    }
                    exec();
                }
            }
        }

        /**
         * Executes the command.
         */
        private void exec() {
            // ;; separates commands, that are to be executed one after one,
            // if previous command has finished successfully.
            final String[] commands = command.split(";;");
            final StringBuffer ans = new StringBuffer("");
            for (int i = 0; i < commands.length; i++) {
                commands[i].trim();
                //Tools.commandLock();
                if (outputVisible) {
                    final String consoleCommand = host.replaceVars(commands[i],
                                                                   true);
                    if (outputVisible && commandVisible) {
                        host.getTerminalPanel().addCommand(consoleCommand);
                    }
                }

                final Object[] ret = execOneCommand(commands[i],
                                                    outputVisible);
                ans.append(ret[0]);
                //Tools.commandUnlock();

                final Integer exitCode = (Integer) ret[1];
                // don't execute after error
                if (exitCode != 0) {
                    if (execCallback != null) {
                        execCallback.doneError(ans.toString(), exitCode);
                    }
                    return;
                }
            }
            if (execCallback != null) {
                execCallback.done(ans.toString());
            }
        }

        /**
         * Returns the session object.
         */
        public final Session getSession() {
            return sess;
        }
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
        Tools.debug(this, "real command: " + realCommand, 1);
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
        /**
         * Verifies the keys.
         */
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
                              new File(Tools.getConfigData().getKnownHostPath()),
                              new String[] {hashedHostname},
                              serverHostKeyAlgorithm,
                              serverHostKey);
                    } catch (IOException ignore) {
                        Tools.appError("SSH.AddHostKeyToFile.Failed", "", ignore);
                    }
                } else {
                    Tools.debug(this, "Not using known_hosts file, because this is not a Linux.");
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
                                                      prompt[i]};

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
                                        !echo[i]);
                    lastPassword = ans;
                } else {
                    ans = lastPassword;
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

        /**
         * Prepares a new <code>ConnectionThread</code> object.
         */
        public ConnectionThread() {
            super();
            username = host.getFirstUsername();
            hostname = host.getFirstIp();
        }

        /**
         * Cancel the connecting.
         */
        public void cancel() {
            cancelIt = true;
        }

        /**
         * Start connection in the thread.
         */
        public void run() {
            if (isConnected()) {
                if (callback != null) {
                    callback.done(1);
                }
            }
            final Connection conn = new Connection(hostname,
                                                   host.getSSHPortInt());
            disconnectForGood = false;

            try {
                /* connect and verify server host key (with callback) */
                Tools.debug(this, "verify host keys: " + hostname, 1);
                final String[] hostkeyAlgos = Tools.getConfigData().getKnownHosts().getPreferredServerHostkeyAlgorithmOrder(hostname);

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
                //        System.out.println(host.getName() + ": connection lost");
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
                        if (enablePublicKey
                            && conn.isAuthMethodAvailable(username, "publickey")) {
                            final File dsaKey =
                                         new File(
                                             Tools.getConfigData().getIdDSAPath());
                            final File rsaKey =
                                         new File(
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
                                        res =
                                           conn.authenticateWithPublicKey(username,
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
                                           conn.authenticateWithPublicKey(username,
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
                                                      Tools.getString(
                                                          "SSH.Enter.passphrase")},
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
                                           conn.authenticateWithPublicKey(username,
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
                                           conn.authenticateWithPublicKey(username,
                                                                          rsaKey,
                                                                          key);
                                    } catch (Exception e) {
                                        lastRSAKey = null;
                                        Tools.debug(this, "rsa key auth failed");
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

                                lastError =
                                        Tools.getString(
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
                                                  host.getUserAtHost()
                                                  + "'s password:"},
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
                    connectionThread = null;
                    host.setConnected();
                    if (callback != null) {
                        callback.doneError("");
                    }
                    connection = null;
                    host.setConnected();
                } else {
                    //  authentication ok.
                    connection = conn;
                    host.setConnected();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            host.getTerminalPanel().nextCommand();
                        }
                    });
                    connectionThread = null;
                    host.setConnected();
                    if (callback != null) {
                        callback.done(0);
                    }
                    Tools.debug(this, "authentication ok");
                }
            } catch (IOException e) {
                Tools.debug(this, e.getMessage(), 0);
                connectionFailed = true;
                if (!cancelIt) {
                    host.getTerminalPanel().addCommandOutput(e.getMessage()
                                                             + "\n");
                    host.getTerminalPanel().nextCommand();
                    if (callback != null) {
                        callback.doneError(e.getMessage());
                    }
                }
                connectionThread = null;
                connection = null;
                host.setConnected();
            }
        }
    }

    /**
     * Installs drbd-gui-helper on the remote host.
     */
    public final void installGuiHelper() {
        final String fileName = "/help-progs/drbd-gui-helper";
        try {
            final BufferedReader br =
                        new BufferedReader(
                            new InputStreamReader(
                               getClass().getResource(fileName).openStream()));
            final StringBuffer file = new StringBuffer("");
            while (br.ready()) {
                file.append(br.readLine());
                file.append('\n');
            }
            System.out.println("scp");
            scp(file.toString(),
                "/usr/local/bin/drbd-gui-helper",
                "0700",
                false);
        } catch (IOException e) {
            Tools.appError("install gui helper error", "", e);
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
        String backupCommand = "";
        if (makeBackup) {
            backupCommand = "cp " + remoteFilename + "{,.`date +'%s'`};" ;
        }
        final Thread t = host.execCommandRaw(
                                backupCommand
                                + "echo \""
                                + host.escapeQuotes(fileContent, 1)
                                + "\">" + remoteFilename
                                + ";chmod " + mode + " "
                                + remoteFilename,
                            new ExecCallback() {
                                public void done(final String ans) {
                                    /* ok */
                                }
                                public void doneError(final String ans,
                                                      final int exitCode) {
                                    Tools.sshError(host, "scp", ans, exitCode);
                                }
                            }, false, true);
        try {
            t.join();
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
