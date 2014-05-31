/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package lcmc.utilities.ssh;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Session;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lcmc.configs.DistResource;
import lcmc.data.Host;
import lcmc.gui.SSHGui;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.NewOutputCallback;
import lcmc.utilities.Tools;

/** This class is a thread that executes commands. */
public final class ExecCommandThread extends Thread {
    private static final Logger LOG =
                              LoggerFactory.getLogger(ExecCommandThread.class);
    private final Host host;
    private final SshConnection connection;
    private final SSHGui sshGui;
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
    private final SSH outer;
    /** Exit code if command failed. */
    private static final int ERROR_EXIT_CODE = 255;
    /** Size of the buffer for output of commads. */
    private static final int EXEC_OUTPUT_BUFFER_SIZE = 8192;

    /** Executes a command in a thread. */
    ExecCommandThread(final Host host,
                      final SshConnection connection,
                      final SSHGui sshGui,
                      final String command,
                      final ExecCallback execCallback,
                      final NewOutputCallback newOutputCallback,
                      final boolean outputVisible,
                      final boolean commandVisible,
                      final int sshCommandTimeout,
                      final SSH outer) throws IOException {
        super();
        this.host = host;
        this.connection = connection;
        this.sshGui = sshGui;
        this.outer = outer;
        this.command = command;
        LOG.debug2("ExecCommandThread: command: " + command);
        this.execCallback = execCallback;
        this.newOutputCallback = newOutputCallback;
        this.outputVisible = outputVisible;
        this.commandVisible = commandVisible;
        this.sshCommandTimeout = sshCommandTimeout;
        if (command.length() > 9 && command.substring(0, 9).equals("NOOUTPUT:")) {
            this.outputVisible = false;
            this.command = command.substring(9, command.length());
        } else if (command.length() > 7 && command.substring(0, 7).
            equals("OUTPUT:")) {
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
        if (connection == null) {
            if (execCallback != null) {
                execCallback.doneError("not connected", 139);
            }
        } else {
            exec(connection);
        }
    }

    /** Executes the command. */
    private void exec(final SshConnection conn) {
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
                        LOG.debug1("exec: " + host.getName() + ": open ssh session: timeout");
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
                throw new RuntimeException("cannot open session");
            }
            final String cmd = command1.trim();
            //Tools.commandLock();
            if (commandVisible && outputVisible) {
                final String consoleCommand = host.replaceVars(cmd, true);
                host.getTerminalPanel().
                    addCommand(consoleCommand.replaceAll(DistResource.SUDO, " "));
            }
            final SshOutput ret = execOneCommand(cmd, outputVisible);
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

    /**
     * Executes command on the host.
     *
     * @param command
     *          command that will be executed.
     * @param outputVisible
     *          whether the output of the command should be visible
     */
    private SshOutput execOneCommand(final String command,
                                         final boolean outputVisible) {
        if (sshCommandTimeout > 0 && sshCommandTimeout < 2000) {
            LOG.appWarning("execOneCommand: timeout: " + sshCommandTimeout + " to small for timeout? " + command);
        }
        this.command = command;
        this.outputVisible = outputVisible;
        final StringBuilder res = new StringBuilder("");
        if (connection.isClosed()) {
            return new SshOutput("SSH.NotConnected", 1);
        }
        if ("installGuiHelper".equals(command)) {
            outer.installGuiHelper();
            return new SshOutput("", 0);
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
                return new SshOutput("", 130);
            }
            /* requestPTY mixes stdout and strerr together, but it works
            better at the moment.
            With pty, the sudo wouldn't work, because we don't want
            to enter sudo password by every command.
            (It would be exposed) */
            thisSession.requestPTY("dumb", 0, 0, 0, 0, null);
            LOG.debug2("execOneCommand: command: " + host.getName() + ": " + host.getSudoCommand(host.getHoppedCommand(command),
                                                                                                                 true));
            thisSession.execCommand("bash -c '" + Tools.escapeSingleQuotes("export LC_ALL=C;" + host.getSudoCommand(host.getHoppedCommand(command),
                                                                                                                          false),
                                                                           1) + '\'');
            final InputStream stdout = thisSession.getStdout();
            final OutputStream stdin = thisSession.getStdin();
            final InputStream stderr = thisSession.getStderr();
            //byte[] buff = new byte[8192];
            final byte[] buff = new byte[EXEC_OUTPUT_BUFFER_SIZE];
            boolean skipNextLine = false;
            boolean cancelSudo = false;
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
                        conditions = thisSession.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA | ChannelCondition.EOF,
                                                                  sshCommandTimeout);
                    }
                    if (cancelIt) {
                        LOG.info("execOneCommand: SSH cancel");
                        throw new IOException("Canceled while waiting for data from peer.");
                    }
                    if ((conditions & ChannelCondition.TIMEOUT) != 0) {
                        /* A timeout occured. */
                        LOG.appWarning("execOneCommand: SSH timeout: " + command);
                        Tools.progressIndicatorFailed(host.getName(),
                                                      "SSH timeout: " + command.replaceAll(DistResource.SUDO,
                                                                                           ""));
                        throw new IOException("Timeout while waiting for data from peer.");
                    }
                    /* Here we do not need to check separately for CLOSED,
                     * since CLOSED implies EOF */
                    if ((conditions & ChannelCondition.EOF) != 0 && (conditions & (ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == 0) {
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
                        final String buffString = new String(buff, 0, len,
                                                             "UTF-8");
                        output.append(buffString);
                        if (outputVisible) {
                            host.getTerminalPanel().addContent(buffString);
                        }
                    }
                }
                if (output.indexOf(SSH.SUDO_PROMPT) >= 0) {
                    if (sudoPwd == null) {
                        cancelSudo = enterSudoPassword();
                    }
                    final String pwd = host.getSudoPassword() + '\n';
                    stdin.write(pwd.getBytes());
                    skipNextLine = true;
                    continue;
                } else if (output.indexOf(SSH.SUDO_FAIL) >= 0) {
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
                /* stderr */
                final CharSequence errOutput = new StringBuilder("");
                while (stderr.available() > 0 && !cancelIt) {
                    // this is unreachable.
                    // stdout and stderr are mixed in the stdout
                    // if pty is requested.
                    final int len = stderr.read(buff);
                    if (len > 0) {
                        final String buffString = new String(buff, 0, len,
                                                             "UTF-8");
                        output.append(buffString);
                        if (outputVisible) {
                            host.getTerminalPanel().
                                addContentErr(buffString);
                        }
                    }
                }
                res.append(errOutput);
                if (newOutputCallback != null && !cancelIt) {
                    LOG.debug2("execOneCommand: output: " + exitCode + ": " + host.getName() + ": " + output.toString());
                    newOutputCallback.output(output.toString());
                }
                if (cancelIt) {
                    return new SshOutput("", 130);
                }
                if (newOutputCallback == null) {
                    res.append(output);
                }
            }
            if (outputVisible) {
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
            LOG.appWarning("execOneCommand: " + host.getName() + ':' + e.getMessage() + ':' + command);
            exitCode = ERROR_EXIT_CODE;
            cancel();
        }
        final String outputString = res.toString();
        LOG.debug2("execOneCommand: output: " + exitCode + ": " + host.getName() + ": " + outputString);
        return new SshOutput(outputString, exitCode);
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

    /**
     * Enter sudo password.
     * Return whether the dialog was cancelled.
     */
    private boolean enterSudoPassword() {
        if (host.isUseSudo() != null && host.isUseSudo()) {
            final String lastError = "";
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

}
