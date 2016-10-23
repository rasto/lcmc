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

import lcmc.common.ui.MainPanel;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.host.domain.Host;
import lcmc.common.ui.ProgressBar;
import lcmc.cluster.ui.SSHGui;
import lcmc.common.domain.ConvertCmdCallback;
import lcmc.common.domain.ExecCallback;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.NewOutputCallback;
import lcmc.common.domain.util.Tools;

public class ExecCommandConfig {
    private static final Logger LOG = LoggerFactory.getLogger(ExecCommandConfig.class);

    private Host host;
    private ConnectionThread connectionThread;
    private SSHGui sshGui;
    private String command = null;
    private String commandString = null;
    private ExecCallback execCallback = null;
    private NewOutputCallback newOutputCallback = null;
    private boolean commandVisible = true;
    private boolean outputVisible = true;
    private int sshCommandTimeout = Ssh.DEFAULT_COMMAND_TIMEOUT;

    private ConvertCmdCallback convertCmdCallback = null;
    private boolean inBash;
    private boolean inSudo;

    public ExecCommandConfig host(final Host host) {
        this.host = host;
        return this;
    }

    public ExecCommandConfig connectionThread(final ConnectionThread connectionThread) {
        this.connectionThread = connectionThread;
        return this;
    }

    public ExecCommandConfig sshGui(final SSHGui sshGui) {
        this.sshGui = sshGui;
        return this;
    }

    public ExecCommandConfig command(final String command) {
        this.command = command;
        return this;
    }

    public ExecCommandConfig commandString(final String commandString) {
        this.commandString = commandString;
        return this;
    }

    public ExecCommandConfig execCallback(final ExecCallback execCallback) {
        this.execCallback = execCallback;
        return this;
    }

    public ExecCommandConfig newOutputCallback(final NewOutputCallback newOutputCallback) {
        this.newOutputCallback = newOutputCallback;
        return this;
    }

    public ExecCommandConfig silentCommand() {
        this.commandVisible = false;
        return this;
    }

    public ExecCommandConfig silentOutput() {
        this.outputVisible = false;
        return this;
    }

    public ExecCommandConfig sshCommandTimeout(final int sshCommandTimeout) {
        this.sshCommandTimeout = sshCommandTimeout;
        return this;
    }

    public ExecCommandConfig convertCmdCallback(final ConvertCmdCallback convertCmdCallback) {
        this.convertCmdCallback = convertCmdCallback;
        return this;
    }

    public ExecCommandConfig inBash(final boolean inBash) {
        this.inBash = inBash;
        return this;
    }

    public ExecCommandConfig inSudo(final boolean inSudo) {
        this.inSudo = inSudo;
        return this;
    }

    public ExecCommandConfig progressBar(final ProgressBar progressBar) {
        if (progressBar != null) {
            progressBar.start(0);
            progressBar.hold();
        }
        return this;
    }

    public ExecCommandThread execute(final MainPanel mainPanel, final ProgressIndicator progressIndicator) {
        if (isOutputVisible()) {
            mainPanel.setTerminalPanel(host.getTerminalPanel());
        }
        final ExecCommandThread execCommandThread = new ExecCommandThread(mainPanel, progressIndicator, this);
        execCommandThread.start();
        return execCommandThread;
    }

    public SshOutput capture(final MainPanel mainPanel, final ProgressIndicator progressIndicator) {
        final StringBuilder output = new StringBuilder("");
        final Integer[] exitCodeHolder = new Integer[]{0};
        if (execCallback == null) {
            final String stackTrace = Tools.getStackTrace();
            execCallback = new ExecCallback() {
                @Override
                public void done(final String answer) {
                    output.append(answer);
                }

                @Override
                public void doneError(final String answer, final int errorCode) {
                    if (outputVisible) {
                        LOG.sshError(host, command, answer, stackTrace, errorCode);
                    }
                    exitCodeHolder[0] = errorCode;
                    output.append(answer);
                }
            };
        }
        final ExecCommandThread execCommandThread = execute(mainPanel, progressIndicator);
        execCommandThread.block();
        return new SshOutput(output.toString(), exitCodeHolder[0]);
    }

    public Host getHost() {
        return host;
    }

    public ConnectionThread getConnectionThread() {
        return connectionThread;
    }

    public SSHGui getSshGui() {
        return sshGui;
    }

    public String getCommand() {
        if (commandString != null) {
            return host.getHostParser().replaceVars(host.getHostParser().getDistCommand(commandString,
                                                                                        convertCmdCallback,
                                                                                        inBash,
                                                                                        inSudo));
        }
        return host.getHostParser().replaceVars(command);
    }

    public ExecCallback getExecCallback() {
        return execCallback;
    }

    public NewOutputCallback getNewOutputCallback() {
        return newOutputCallback;
    }

    public boolean isCommandVisible() {
        return commandVisible;
    }

    public boolean isOutputVisible() {
        return outputVisible;
    }

    public int getSshCommandTimeout() {
        return sshCommandTimeout;
    }

    public ExecCommandConfig outputVisible(boolean outputVisible) {
        this.outputVisible = outputVisible;
        return this;
    }

    public ExecCommandConfig commandVisible(boolean commandVisible) {
        this.commandVisible = commandVisible;
        return this;
    }
}
