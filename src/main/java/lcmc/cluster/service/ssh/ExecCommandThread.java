/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.cluster.service.ssh;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Session;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lcmc.common.ui.MainPanel;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.configs.DistResource;
import lcmc.host.domain.Host;
import lcmc.cluster.ui.SSHGui;
import lcmc.common.domain.ExecCallback;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.NewOutputCallback;
import lcmc.common.domain.util.Tools;

/** This class is a thread that executes commands. */
public final class ExecCommandThread extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(ExecCommandThread.class);

    private final Host host;
    private final ConnectionThread connectionThread;
    private final SSHGui sshGui;
    private final String command;
    private final ExecCallback execCallback;
    private final NewOutputCallback newOutputCallback;
    private final boolean outputVisible;
    private final boolean commandVisible;
    private final MainPanel mainPanel;
    private final ProgressIndicator progressIndicator;

    private volatile boolean cancelIt = false;
    private final Lock mSessionLock = new ReentrantLock();
    private Session sess = null;
    private final int sshCommandTimeout;

    private static final int ERROR_EXIT_CODE = 255;
    private static final int EXEC_OUTPUT_BUFFER_SIZE = 8192;
    private static final int DEFAULT_EXIT_CODE = 100;
    private static final String ENCODING = "UTF-8";

    ExecCommandThread(final MainPanel mainPanel,
                      final ProgressIndicator progressIndicator,
                      final ExecCommandConfig execCommandConfig) {
        this.mainPanel = mainPanel;
        this.progressIndicator = progressIndicator;

        this.host = execCommandConfig.getHost();
        this.connectionThread = execCommandConfig.getConnectionThread();
        this.sshGui = execCommandConfig.getSshGui();

        this.execCallback = execCommandConfig.getExecCallback();
        this.newOutputCallback = execCommandConfig.getNewOutputCallback();
        this.commandVisible = execCommandConfig.isCommandVisible();
        this.sshCommandTimeout = execCommandConfig.getSshCommandTimeout();

        if (execCommandConfig.getCommand().length() > 9
            && "NOOUTPUT:".equals(execCommandConfig.getCommand().substring(0, 9))) {
            this.outputVisible = false;
            this.command = execCommandConfig.getCommand().substring(9, execCommandConfig.getCommand().length());
        } else if (execCommandConfig.getCommand().length() > 7
                   && "OUTPUT:".equals(execCommandConfig.getCommand().substring(0, 7))) {
            this.outputVisible = true;
            this.command = execCommandConfig.getCommand().substring(7, execCommandConfig.getCommand().length());
        } else {
            this.outputVisible = execCommandConfig.isOutputVisible();
            this.command = execCommandConfig.getCommand();
        }
        LOG.debug2("ExecCommandThread: command: " + command);
    }

    /**
     * Reconnects, connects if there is no connection and executes a
     * command.
     */
    @Override
    public void run() {
        if (!connectionThread.isConnectionEstablished()) {
            if (execCallback != null) {
                execCallback.doneError("not connected", 139);
            }
        } else {
            if (commandVisible || outputVisible) {
                mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.EXPAND);
            }
            exec();
            if (commandVisible || outputVisible) {
                mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.COLLAPSE);
            }
        }
    }

    public void cancelTheSession() {
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

    static private class ConnectionTimeout {
        private boolean timeout = false;

        private void setTimeout() {
            timeout = true;
        }

        private boolean wasTimeout() {
            return timeout;
        }
    }

    public ExecCommandThread block() {
        try {
            join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return this;
    }

    private void exec() {
        // ;;; separates commands, that are to be executed one after one,
        // if previous command has finished successfully.
        final String[] commands = command.split(";;;");
        final StringBuilder ans = new StringBuilder("");
        for (final String command1 : commands) {
            final ConnectionTimeout connectionTimeout = setupConnectionTimeout();
            try {
                openSshSession(connectionTimeout);
            } catch (final IOException e) {
                handleSshSessionFailure();
                break;
            }
            final String cmd = command1.trim();
            writeCommandToTerminal(cmd);
            final SshOutput ret = execOneCommand(cmd);
            ans.append(ret.getOutput());
            final int exitCode = ret.getExitCode();
            if (exitCode != 0) {
                handleCommandFailure(ans, exitCode);
                // skip the reset
                return;
            }
        }
        if (execCallback != null) {
            execCallback.done(ans.toString());
        }
    }

    private void handleCommandFailure(final StringBuilder ans,
            final int exitCode) {
        if (execCallback != null) {
            if (commandVisible || outputVisible) {
                mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.EXPAND);
            }
            execCallback.doneError(ans.toString(), exitCode);
        }
    }

    private void writeCommandToTerminal(final String cmd) {
        if (commandVisible) {
            final String consoleCommand = host.replaceVars(cmd, true);
            host.getTerminalPanel().addCommand(consoleCommand.replaceAll(DistResource.SUDO, " "));
        }
    }

    private void handleSshSessionFailure() {
        connectionThread.closeConnection();
        if (execCallback != null) {
            execCallback.doneError("could not open session", 45);
        }
    }

    private void openSshSession(final ConnectionTimeout connectionTimeout)
            throws IOException {
        /* it may hang here if we lost connection, so it will be
         * interrupted after a timeout. */
        final Session newSession = connectionThread.getConnection().openSession();
        mSessionLock.lock();
        try {
            sess = newSession;
        } finally {
            mSessionLock.unlock();
        }
        if (connectionTimeout.wasTimeout()) {
            throw new IOException("open session failed");
        }
        connectionTimeout.setTimeout();
    }

    private ConnectionTimeout setupConnectionTimeout() {
        final ConnectionTimeout connectionTimeout = new ConnectionTimeout();
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Tools.sleep(Tools.getDefaultInt("SSH.ConnectTimeout"));
                if (!connectionTimeout.wasTimeout()) {
                    LOG.debug1("run: " + host.getName() + ": open ssh session: timeout");
                    connectionTimeout.setTimeout();
                    try {
                        final SshConnection sshConnection = connectionThread.getConnection();
                        if (sshConnection != null) {
                            sshConnection.cancel();
                        }
                    } catch (IOException e) {
                        LOG.appWarning("run: " + host.getName() + ": setting timeout failed");
                    }
                }
            }
        });
        thread.start();
        return connectionTimeout;
    }

    private SshOutput execOneCommand(final String oneCommand) {
        if (sshCommandTimeout > 0 && sshCommandTimeout < 2000) {
            LOG.appWarning("execOneCommand: timeout: " + sshCommandTimeout + " to small for timeout? " + command);
        }
        if (!connectionThread.isConnectionEstablished()) {
            return new SshOutput("SSH.NotConnected", 1);
        }

        int exitCode = DEFAULT_EXIT_CODE;
        String outputString = "";
        try {
            mSessionLock.lock();
            final Session thisSession;
            try {
                thisSession = sess;
            } finally {
                mSessionLock.unlock();
            }
            if (thisSession == null) {
                return new SshOutput("", 130);
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
                       + host.getSudoCommand(host.getHoppedCommand(oneCommand), true));
            thisSession.execCommand("bash -c '"
                                    + Tools.escapeSingleQuotes("export LC_ALL=C;"
                                                               + host.getSudoCommand(host.getHoppedCommand(oneCommand),
                                                                                     false), 1) + '\'');
            outputString = execCommandAndCaptureOutput(oneCommand, thisSession);
            if (cancelIt) {
                return new SshOutput("", 130);
            }
            if (commandVisible) {
                host.getTerminalPanel().nextCommand();
            }
            thisSession.waitForCondition(ChannelCondition.EXIT_STATUS, 10000);
            final Integer ec = thisSession.getExitStatus();
            if (ec != null) {
                exitCode = ec;
            }
            thisSession.close();
            sess = null;
        } catch (final IOException e) {
            LOG.appWarning("execOneCommand: " + host.getName() + ':' + e.getMessage() + ':' + oneCommand);
            exitCode = ERROR_EXIT_CODE;
            cancelTheSession();
        }
        LOG.debug2("execOneCommand: output: " + exitCode + ": " + host.getName() + ": " + outputString);
        return new SshOutput(outputString, exitCode);
    }

    private String execCommandAndCaptureOutput(final String oneCommand, final Session thisSession) throws IOException {
        final InputStream stdout = thisSession.getStdout();
        final OutputStream stdin = thisSession.getStdin();
        final InputStream stderr = thisSession.getStderr();
        final byte[] buff = new byte[EXEC_OUTPUT_BUFFER_SIZE];
        boolean skipNextLine = false;
        final StringBuilder res = new StringBuilder("");
        while (true) {
            final String sudoPwd = host.getSudoPassword();
            if ((stdout.available() == 0) && (stderr.available() == 0)) {
                /* Even though currently there is no data available,
                 * it may be that new data arrives and the session's
                 * underlying channel is closed before we call
                 * waitForCondition(). This means that EOF and
                 * STDOUT_DATA (or STDERR_DATA, or both) may be set
                 * together.
                 */
                int conditions = 0;
                if (!cancelIt) {
                    conditions = thisSession.waitForCondition(ChannelCondition.STDOUT_DATA
                                                              | ChannelCondition.STDERR_DATA
                                                              | ChannelCondition.EOF,
                                                              sshCommandTimeout);
                }
                if (cancelIt) {
                    LOG.info("execOneCommand: SSH cancel");
                    throw new IOException("Canceled while waiting for data from peer.");
                }
                if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                    /* A timeout occured. */
                    LOG.appWarning("execOneCommand: SSH timeout: " + oneCommand);
                    progressIndicator.progressIndicatorFailed(host.getName(),
                            "SSH timeout: " + oneCommand.replaceAll(DistResource.SUDO, ""));
                    throw new IOException("Timeout while waiting for data from peer.");
                }
                /* Here we do not need to check separately for CLOSED,
                 * since CLOSED implies EOF */
                if ((conditions & ChannelCondition.EOF) != 0
                    && (conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {
                    /* The remote side won't send us further data... */
                    /* ... and we have consumed all data in the
                     * ... local arrival window. */
                    break;
                }
                /* OK, either STDOUT_DATA or STDERR_DATA (or both) */
                /* ... is set. */
            }
            final StringBuilder output = readStdout(stdout, buff);
            if (output.indexOf(Ssh.SUDO_PROMPT) >= 0) {
                if (sudoPwd == null) {
                    enterSudoPassword();
                }
                final String pwd = host.getSudoPassword() + '\n';
                stdin.write(pwd.getBytes());
                skipNextLine = true;
                continue;
            } else if (output.indexOf(Ssh.SUDO_FAIL) >= 0) {
                host.setSudoPassword(null);
            } else {
                if (skipNextLine) {
                    /* this is the "enter" after pwd */
                    skipNextLine = false;
                    if (output.charAt(0) == 13 && output.charAt(1) == 10) {
                        output.delete(0, 2);
                        if (output.length() == 0) {
                            continue;
                        }
                    }
                }
            }
            final StringBuilder errOutput = readStderr(stderr, buff);
            res.append(errOutput);
            if (newOutputCallback != null && !cancelIt) {
                LOG.debug2("execOneCommand: output: "
                           + ": "
                           + host.getName()
                           + ": "
                           + output.toString());
                newOutputCallback.output(output.toString());
            }
            if (cancelIt) {
                return res.toString();
            }
            if (newOutputCallback == null) {
                res.append(output);
            }
        }
        return res.toString();
    }

    /** If you below replace "while" with "if", then the way
     * the output appears on the local stdout and stder streams
     * is more "balanced". Addtionally reducing the buffer size
     * will also improve the interleaving, but performance will
     * slightly suffer. OKOK, that all matters only if you get
     * HUGE amounts of stdout and stderr data =)
     */
    private StringBuilder readStdout(final InputStream stdout, final byte[] buff) throws IOException {
        final StringBuilder output = new StringBuilder();
        while (stdout.available() > 0 && !cancelIt) {
            final int len = stdout.read(buff);
            if (len > 0) {
                final String buffString = new String(buff, 0, len, ENCODING);
                output.append(buffString);
                if (outputVisible) {
                    host.getTerminalPanel().addContent(buffString);
                }
            }
        }
        return output;
    }

    private StringBuilder readStderr(final InputStream stderr, final byte[] buff) throws IOException {
        final StringBuilder output = new StringBuilder();
        while (stderr.available() > 0 && !cancelIt) {
            // this is unreachable.
            // stdout and stderr are mixed in the stdout
            // if pty is requested.
            final int len = stderr.read(buff);
            if (len > 0) {
                final String buffString = new String(buff, 0, len, ENCODING);
                output.append(buffString);
                if (outputVisible) {
                    host.getTerminalPanel().addContentErr(buffString);
                }
            }
        }
        return output;
    }

    /**
     * Return whether the dialog was cancelled.
     */
    private boolean enterSudoPassword() {
        if (host.isUseSudo() != null && host.isUseSudo()) {
            final String lastError = "";
            final String sudoPwd = sshGui.enterSomethingDialog(Tools.getString("SSH.SudoAuthentication"),
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
}
