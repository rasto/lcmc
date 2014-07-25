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

package lcmc.utilities.ssh;

import java.io.IOException;
import lcmc.model.Host;
import lcmc.gui.ProgressBar;
import lcmc.gui.SSHGui;
import lcmc.utilities.ConnectionCallback;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

public class ConnectionThread extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionThread.class);
    private final String hostname;
    private final SSHGui sshGui;
    private final Host host;
    private final ProgressBar progressBar;
    /** Callback when connection is failed or properly closed. */
    private final ConnectionCallback connectionCallback;
    private final Authentication authentication;

    private SshConnection sshConnection = null;

    private volatile boolean connectionFailed;
    private volatile boolean connectionEstablished = false;

    ConnectionThread(final Host host,
                     final SSHGui sshGui,
                     final ProgressBar progressBar,
                     final ConnectionCallback connectionCallback,
                     final Authentication authentication) {
        super();
        this.host = host;
        this.sshGui = sshGui;
        this.progressBar = progressBar;
        this.connectionCallback = connectionCallback;
        hostname = host.getFirstIp();
        this.authentication = authentication;
    }

    /** Cancel the connecting. */
    void cancel() {
        sshConnection.cancel();
    }

    /** Start connection in the thread. */
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
        final String[] hostkeyAlgos = Tools.getApplication().getKnownHosts()
                                           .getPreferredServerHostkeyAlgorithmOrder(hostname);
        if (hostkeyAlgos != null) {
            newSshConnection.setServerHostKeyAlgorithms(hostkeyAlgos);
        }
        final int connectTimeout = Tools.getDefaultInt("SSH.ConnectTimeout");
        final int kexTimeout = Tools.getDefaultInt("SSH.KexTimeout");
        if (progressBar != null) {
            final int timeout = (connectTimeout < kexTimeout) ? connectTimeout : kexTimeout;
            progressBar.start(timeout);
        }
        LOG.debug2("run: connect");
        newSshConnection.connect(new PopupHostKeyVerifier(sshGui), connectTimeout, kexTimeout);
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
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                host.getTerminalPanel().nextCommand();
            }
        });
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (connectionCallback != null) {
                    connectionCallback.done(0);
                }
            }
        });
        thread.start();
        LOG.debug1("authenticate: " + host.getName() + ": authentication ok");
    }
}
