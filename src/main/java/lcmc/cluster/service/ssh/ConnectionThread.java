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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import lcmc.cluster.ui.SSHGui;
import lcmc.common.domain.Application;
import lcmc.common.domain.ConnectionCallback;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

@Named
public class ConnectionThread extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionThread.class);
    private String hostname;
    private SSHGui sshGui;
    private Host host;
    private ProgressBar progressBar;
    /**
     * Callback when connection is failed or properly closed.
     */
    private ConnectionCallback connectionCallback;
    private Authentication authentication;

    private volatile SshConnection sshConnection = null;

    private volatile boolean connectionFailed;
    private volatile boolean connectionEstablished = false;
    private final Application application;
    private final SwingUtils swingUtils;
    private final Provider<PopupHostKeyVerifier> popupHostKeyVerifierProvider;

    @Inject
    public ConnectionThread(Application application, SwingUtils swingUtils,
            Provider<PopupHostKeyVerifier> popupHostKeyVerifierProvider) {
        this.application = application;
        this.swingUtils = swingUtils;
        this.popupHostKeyVerifierProvider = popupHostKeyVerifierProvider;
    }

    void init(final Host host, final SSHGui sshGui, final ProgressBar progressBar, final ConnectionCallback connectionCallback,
            final Authentication authentication) {
        this.host = host;
        this.sshGui = sshGui;
        this.progressBar = progressBar;
        this.connectionCallback = connectionCallback;
        hostname = host.getFirstIp();
        this.authentication = authentication;
    }

    /**
     * Cancel the connecting.
     */
    void cancel() {
        sshConnection.cancel();
    }

    /**
     * Start connection in the thread.
     */
    @Override
    public void run() {
        LOG.debug2("run: start");
        if (connectionCallback != null && isConnectionEstablished()) {
            connectionCallback.done(1);
        }
        host.setSudoPassword("");
        final SshConnection newSshConnection = new SshConnection(hostname, host.getSSHPortInt());
        try {
            if (hostname == null) {
                throw new IOException("hostname is not set");
            }
            connect(newSshConnection);
            authenticate(newSshConnection);
        } catch (final IOException e) {
            handleFailedConnection(e.getMessage());
        }
    }

    public void closeConnection() {
        connectionEstablished = false;
    }

    public boolean isConnectionEstablished() {
        return connectionEstablished;
    }

    public void closeConnectionForGood() {
        closeConnection();
        sshConnection.disconnectForGood();
    }

    public boolean isConnectionFailed() {
        return connectionFailed;
    }

    public void setConnectionFailed(final boolean connectionFailed) {
        this.connectionFailed = connectionFailed;
        if (connectionFailed) {
            closeConnection();
        }
    }

    public SshConnection getConnection() throws IOException {
        if (!connectionEstablished) {
            throw new IOException("getConnection: connection closed");
        }
        return sshConnection;
    }

    public boolean isDisconnectedForGood() {
        return sshConnection != null && sshConnection.isDisconnectedForGood();
    }

    public void disconnectForGood() {
        if (sshConnection != null) {
            sshConnection.disconnectForGood();
        }
    }

    private void connect(final SshConnection newSshConnection) throws IOException {
        LOG.debug2("run: verify host keys: " + hostname);
        final String[] hostKeyAlgorithms = application.getKnownHosts().getPreferredServerHostkeyAlgorithmOrder(hostname);
        if (hostKeyAlgorithms != null) {
            newSshConnection.setServerHostKeyAlgorithms(hostKeyAlgorithms);
        }
        final int connectTimeout = Tools.getDefaultInt("SSH.ConnectTimeout");
        final int kexTimeout = Tools.getDefaultInt("SSH.KexTimeout");
        if (progressBar != null) {
            final int timeout = Math.min(connectTimeout, kexTimeout);
            progressBar.start(timeout);
        }
        LOG.debug2("run: connect");
        final PopupHostKeyVerifier popupHostKeyVerifier = popupHostKeyVerifierProvider.get();
        popupHostKeyVerifier.init(sshGui);
        newSshConnection.connect(popupHostKeyVerifier, connectTimeout, kexTimeout);
    }

    private void handleFailedConnection(final String message) {
        LOG.appWarning("run: connecting failed: " + message);
        connectionFailed = true;
        if (!connectionEstablished || !sshConnection.isCanceled()) {
            host.getTerminalPanel().addCommandOutput(message + '\n');
            host.getTerminalPanel().nextCommand();
            if (connectionCallback != null) {
                connectionCallback.doneError(message);
            }
        }
        closeConnection();
    }

    private void authenticate(final SshConnection newSshConnection) throws IOException {
        LOG.debug2("run: authenticate");
        authentication.authenticate(newSshConnection);
        LOG.debug2("run: authenticate: end");
        if (newSshConnection.isCanceled()) {
            authenticationCanceledOrTimeout(newSshConnection);
        } else {
            authenticationOk(newSshConnection);
        }
    }

    private void authenticationCanceledOrTimeout(final SshConnection newSshConnection) {
        newSshConnection.close();
        LOG.debug("authenticate: closing canceled connection");
        closeConnection();
        host.setConnected();
        if (connectionCallback != null) {
            connectionCallback.doneError("");
        }
    }

    private void authenticationOk(final SshConnection newSshConnection) {
        sshConnection = newSshConnection;
        connectionEstablished = true;
        host.setConnected();
        swingUtils.invokeLater(() -> host.getTerminalPanel().nextCommand());
        final Thread thread = new Thread(() -> {
            if (connectionCallback != null) {
                connectionCallback.done(0);
            }
        });
        thread.start();
        LOG.debug1("authenticate: " + host.getName() + ": authentication ok");
    }
}
