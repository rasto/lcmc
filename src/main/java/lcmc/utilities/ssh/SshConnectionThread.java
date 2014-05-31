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

import java.io.File;
import java.io.IOException;
import lcmc.data.Host;
import lcmc.gui.ProgressBar;
import lcmc.gui.SSHGui;
import lcmc.utilities.ConnectionCallback;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

/**
 * The SSH-2 connection is established in this thread.
 * If we would not use a separate thread (e.g., put this code in
 * the event handler of the "Login" button) then the GUI would not
 * be responsive (missing window repaints if you move the window etc.)
 */
public class SshConnectionThread extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(SshConnectionThread.class);
    /** Username with which it will be connected. */
    private final String username;
    /** Hostname of the host to which it will be connect. */
    private final String hostname;
    private final SSHGui sshGui;
    private final Host host;
    private final LastSuccessfulPassword lastSuccessfulPassword;
    private final ProgressBar progressBar;
    /** Callback when connection is failed or properly closed. */
    private final ConnectionCallback connectionCallback;
    /** Cancel the connecting. */
    private boolean cancelIt = false;

    private SshConnection sshConnection = null;

    private volatile boolean disconnectForGood = true;
    private volatile boolean connectionFailed;

    /** Prepares a new {@code ConnectionThread} object. */
    SshConnectionThread(final Host host,
                        final LastSuccessfulPassword lastSuccessfulPassword,
                        final SSHGui sshGui,
                        final ProgressBar progressBar,
                        final ConnectionCallback connectionCallback) {
        super();
        this.host = host;
        this.lastSuccessfulPassword = lastSuccessfulPassword;
        this.sshGui = sshGui;
        this.progressBar = progressBar;
        this.connectionCallback = connectionCallback;
        username = host.getFirstUsername();
        hostname = host.getFirstIp();
    }

    private void authenticate(final SshConnection conn) throws IOException {
        LOG.debug2("authenticate: start");
        boolean enableKeyboardInteractive = true;
        boolean enablePublicKey = true;
        String lastError = null;
        int publicKeyTry = 3; /* how many times to try the public key authentification */
        int passwdTry = 3; /* how many times to try the password authentification */
        final int connectTimeout = Tools.getDefaultInt("SSH.ConnectTimeout");
        final int kexTimeout = Tools.getDefaultInt("SSH.KexTimeout");
        final boolean noPassphrase = Tools.getApplication().isNoPassphrase();
        while (!cancelIt) {
            if (lastSuccessfulPassword.getPassword() == null) {
                String lastPassword = Tools.getApplication().getAutoOptionHost("pw");
                if (lastPassword == null) {
                    lastPassword = Tools.getApplication().getAutoOptionCluster("pw");
                }
                lastSuccessfulPassword.setPassword(lastPassword);
            }
            if (lastSuccessfulPassword.getPassword() == null) {
                if (enablePublicKey && conn.isAuthMethodAvailable(username, "publickey")) {
                    final File dsaKey = new File(Tools.getApplication().getIdDSAPath());
                    final File rsaKey = new File(Tools.getApplication().getIdRSAPath());
                    if (dsaKey.exists() || rsaKey.exists()) {
                        String key = "";
                        if (lastSuccessfulPassword.getDsaKey() != null) {
                            key = lastSuccessfulPassword.getDsaKey();
                        } else if (lastSuccessfulPassword.getRsaKey() != null) {
                            key = lastSuccessfulPassword.getRsaKey();
                        }
                        /* Passwordless auth */
                        boolean res = false;
                        if (noPassphrase || !"".equals(key)) {
                            /* try first passwordless authentication.  */
                            if (lastSuccessfulPassword.getRsaKey() == null && dsaKey.exists()) {
                                try {
                                    res = conn.authenticateWithPublicKey(username, dsaKey, key);
                                } catch (final IOException e) {
                                    lastSuccessfulPassword.setDsaKey(null);
                                    LOG.debug("authenticate: dsa passwordless failed");
                                }
                                if (res) {
                                    LOG.debug("authenticate: dsa passwordless auth successful");
                                    lastSuccessfulPassword.setDsaKey(key);
                                    lastSuccessfulPassword.setRsaKey(null);
                                    lastSuccessfulPassword.setPassword(null);
                                    break;
                                }
                                conn.close();
                                conn.connect(new PopupHostKeyVerifier(sshGui), connectTimeout, kexTimeout);
                            }
                            if (rsaKey.exists()) {
                                try {
                                    res = conn.authenticateWithPublicKey(username, rsaKey, key);
                                } catch (final IOException e) {
                                    lastSuccessfulPassword.setRsaKey(null);
                                    LOG.debug("authenticate: rsa passwordless failed");
                                }
                                if (res) {
                                    LOG.debug("authenticate: rsa passwordless auth successful");
                                    lastSuccessfulPassword.setDsaKey(null);
                                    lastSuccessfulPassword.setRsaKey(key);
                                    lastSuccessfulPassword.setPassword(null);
                                    break;
                                }
                                conn.close();
                                conn.connect(new PopupHostKeyVerifier(sshGui), connectTimeout, kexTimeout);
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
                                res = conn.authenticateWithPublicKey(username, dsaKey, key);
                            } catch (final IOException e) {
                                lastSuccessfulPassword.setDsaKey(null);
                                LOG.debug("authenticate: dsa key auth failed");
                            }
                            if (res) {
                                LOG.debug("authenticate: dsa key auth successful");
                                lastSuccessfulPassword.setRsaKey(null);
                                lastSuccessfulPassword.setDsaKey(key);
                                lastSuccessfulPassword.setPassword(null);
                                break;
                            }
                            conn.close();
                            conn.connect(new PopupHostKeyVerifier(sshGui),
                                         connectTimeout, kexTimeout);
                        }
                        if (rsaKey.exists()) {
                            try {
                                res = conn.authenticateWithPublicKey(username,
                                                                     rsaKey, key);
                            } catch (final IOException e) {
                                lastSuccessfulPassword.setRsaKey(null);
                                LOG.debug("authenticate: rsa key auth failed");
                            }
                            if (res) {
                                LOG.debug("authenticate: rsa key auth successful");
                                lastSuccessfulPassword.setRsaKey(key);
                                lastSuccessfulPassword.setDsaKey(null);
                                lastSuccessfulPassword.setPassword(null);
                                break;
                            }
                            conn.close();
                            conn.connect(new PopupHostKeyVerifier(sshGui),
                                         connectTimeout, kexTimeout);
                        }
                        lastError = Tools.getString("SSH.Publickey.Authentication.Failed");
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
            if (enableKeyboardInteractive && conn.isAuthMethodAvailable(username,
                                                                        "keyboard-interactive")) {
                final InteractiveLogic interactiveLogic = new InteractiveLogic(lastError,
                                                                               host,
                                                                               lastSuccessfulPassword,
                                                                               sshGui);
                final boolean res = conn.authenticateWithKeyboardInteractive(username,
                                                                             interactiveLogic);
                if (res) {
                    lastSuccessfulPassword.setRsaKey(null);
                    lastSuccessfulPassword.setDsaKey(null);
                    break;
                } else {
                    lastSuccessfulPassword.setPassword(null);
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
                    lastError = Tools.getString("SSH.KeyboardInteractive.DoesNotWork");
                    /* do not try this again */
                    enableKeyboardInteractive = false;
                } else {
                    /* try again, if possible */
                    lastError = Tools.getString("SSH.KeyboardInteractive.Failed");
                }
                continue;
            }
            if (conn.isAuthMethodAvailable(username, "password")) {
                final String ans;
                if (lastSuccessfulPassword.getPassword() == null) {
                    ans = sshGui.enterSomethingDialog(Tools.getString("SSH.PasswordAuthentication"),
                                                            new String[]{lastError, "<html>" + host.getUserAtHost() + Tools.getString("SSH.Enter.password") + "</html>"},
                                                            null, null, true);
                    if (ans == null) {
                        cancelIt = true;
                        break;
                    }
                } else {
                    ans = lastSuccessfulPassword.getPassword();
                }
                if (ans == null) {
                    throw new IOException("Login aborted by user");
                }
                if ("".equals(ans)) {
                    passwdTry = 0;
                }
                final boolean res = conn.authenticateWithPassword(username, ans);
                if (res) {
                    lastSuccessfulPassword.setPassword(ans);
                    host.setSudoPassword(ans);
                    lastSuccessfulPassword.setRsaKey(null);
                    lastSuccessfulPassword.setDsaKey(null);
                    break;
                } else {
                    lastSuccessfulPassword.setPassword(null);
                }
                /* try again, if possible */
                lastError = Tools.getString("SSH.Password.Authentication.Failed");
                passwdTry--;
                if (passwdTry <= 0) {
                    enablePublicKey = true;
                    passwdTry = 3;
                }
                continue;
            }
            throw new IOException("No supported authentication methods available.");
        }
        if (cancelIt) {
            // since conn.connect call is not interrupted, we get
            // here only after connection is esteblished or after
            // timeout.
            conn.close();
            LOG.debug("authenticate: closing canceled connection");
            closeConnection();
            host.setConnected();
            if (connectionCallback != null) {
                connectionCallback.doneError("");
            }
        } else {
            //  authentication ok.
            sshConnection = conn;
            host.setConnected();
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    host.getTerminalPanel().nextCommand();
                }
            });
            host.setConnected();
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

    /** Cancel the connecting. */
    void cancel() {
        cancelIt = true;
    }

    /** Start connection in the thread. */
    @Override
    public void run() {
        LOG.debug2("run: start");
        if (connectionCallback != null && isConnectionEstablished()) {
            connectionCallback.done(1);
        }
        host.setSudoPassword("");
        final SshConnection conn = new SshConnection(hostname, host.getSSHPortInt());
        disconnectForGood = false;
        try {
            if (hostname == null) {
                throw new IOException("hostname is not set");
            }
            /* connect and verify server host key (with callback) */
            LOG.debug2("run: verify host keys: " + hostname);
            final String[] hostkeyAlgos = Tools.getApplication().getKnownHosts()
                                               .getPreferredServerHostkeyAlgorithmOrder(hostname);
            if (hostkeyAlgos != null) {
                conn.setServerHostKeyAlgorithms(hostkeyAlgos);
            }
            final int connectTimeout = Tools.getDefaultInt("SSH.ConnectTimeout");
            final int kexTimeout = Tools.getDefaultInt("SSH.KexTimeout");
            if (progressBar != null) {
                final int timeout = (connectTimeout < kexTimeout) ? connectTimeout : kexTimeout;
                progressBar.start(timeout);
            }
            LOG.debug2("run: connect");
            conn.connect(new PopupHostKeyVerifier(sshGui), connectTimeout, kexTimeout);
            LOG.debug2("run: authenticate");
            /* authentication phase */
            authenticate(conn);
            LOG.debug2("run: authenticate: end");
        } catch (final IOException e) {
            LOG.appWarning("run: connecting failed: " + e.getMessage());
            connectionFailed = true;
            if (!cancelIt) {
                host.getTerminalPanel().addCommandOutput(e.getMessage() + '\n');
                host.getTerminalPanel().nextCommand();
                if (connectionCallback != null) {
                    connectionCallback.doneError(e.getMessage());
                }
            }
            closeConnection();
        }
    }

    public void closeConnection() {
        sshConnection = null;
    }

    public boolean isConnectionEstablished() {
        return sshConnection != null;
    }

    public void closeConnectionForGood() {
        closeConnection();
        disconnectForGood = true;
    }

    public boolean isConnectionFailed() {
        return connectionFailed;
    }

    public void setConnectionFailed(final boolean connectionFailed) {
        this.connectionFailed = connectionFailed;
    }

    public SshConnection getConnection() {
        return sshConnection;
    }

    public boolean isDisconnectedForGood() {
        return disconnectForGood;
    }

    public void setDisconnectForGood(final boolean disconnectForGood) {
        this.disconnectForGood = disconnectForGood;
    }

    private String getKeyFromUser(final String lastError) {
        return sshGui.enterSomethingDialog(Tools.getString("SSH.RSA.DSA.Authentication"),
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
}
