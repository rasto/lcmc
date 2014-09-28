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

import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.gui.SSHGui;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

@Named
public class Authentication {
    private static final Logger LOG = LoggerFactory.getLogger(Authentication.class);

    private LastSuccessfulPassword lastSuccessfulPassword;
    private Host host;
    private SSHGui sshGui;

    private boolean enablePublicKey = true;
    private int publicKeyTry = 3;
    private final int connectTimeout = Tools.getDefaultInt("SSH.ConnectTimeout");
    private final int kexTimeout = Tools.getDefaultInt("SSH.KexTimeout");
    private String lastError = null;
    private boolean authenticated = false;
    private int passwdTry = 3;
    private boolean enableKeyboardInteractive = true;
    @Inject
    private Application application;
    @Inject
    private Provider<PopupHostKeyVerifier> popupHostKeyVerifierProvider;

    public void init(final LastSuccessfulPassword lastSuccessfulPassword, final Host host, final SSHGui sshGui) {
        this.lastSuccessfulPassword = lastSuccessfulPassword;
        this.host = host;
        this.sshGui = sshGui;
    }

    public void authenticate(final SshConnection sshConnection) throws IOException {
        LOG.debug2("authenticate: start");
        final String username = host.getFirstUsername();
        while (!sshConnection.isCanceled() && !authenticated) {
            if (lastSuccessfulPassword.getPassword() == null) {
                String lastPassword = application.getAutoOptionHost("pw");
                if (lastPassword == null) {
                    lastPassword = application.getAutoOptionCluster("pw");
                }
                lastSuccessfulPassword.setPassword(lastPassword);
            }
            if (lastSuccessfulPassword.getPassword() == null
                && enablePublicKey && sshConnection.isAuthMethodAvailable(username, "publickey")) {
                authenticateWithKey(sshConnection, username);
            } else if (enableKeyboardInteractive
                       && sshConnection.isAuthMethodAvailable(username, "keyboard-interactive")) {
                authenticateWithKeyboardInteractive(sshConnection, username);
            } else if (sshConnection.isAuthMethodAvailable(username, "password")) {
                authenticateWithPassword(sshConnection, username);
            } else {
                throw new IOException("No supported authentication methods available.");
            }
        }
    }

    private void authenticateWithKey(final SshConnection sshConnection, final String username) throws IOException {
        final File dsaKey = new File(application.getIdDSAPath());
        final File rsaKey = new File(application.getIdRSAPath());
        if (dsaKey.exists() || rsaKey.exists()) {
            String key = "";
            if (lastSuccessfulPassword.getDsaKey() != null) {
                key = lastSuccessfulPassword.getDsaKey();
            } else if (lastSuccessfulPassword.getRsaKey() != null) {
                key = lastSuccessfulPassword.getRsaKey();
            }
            /* Passwordless auth */
            if (application.isNoPassphrase() || !"".equals(key)) {
                if (lastSuccessfulPassword.getRsaKey() == null && dsaKey.exists()) {
                    authenticateWithDsaKey(sshConnection, username, dsaKey, key);
                    if (authenticated) {
                        return;
                    }
                }
                if (rsaKey.exists()) {
                    authenticateWithRsaKey(sshConnection, username, rsaKey, key);
                    if (authenticated) {
                        return;
                    }
                }
            }
            key = getKeyFromUser(lastError);
            if (key == null) {
                sshConnection.cancel();
                sshConnection.disconnectForGood();
                return;
            }
            if ("".equals(key)) {
                publicKeyTry = 0;
            }
            if (dsaKey.exists()) {
                authenticateWithDsaKey(sshConnection, username, dsaKey, key);
                if (authenticated) {
                    return;
                }
            }
            if (rsaKey.exists()) {
                authenticateWithRsaKey(sshConnection, username, rsaKey, key);
                if (authenticated) {
                    return;
                }
            }
            lastError = Tools.getString("SSH.Publickey.Authentication.Failed");
        } else {
            publicKeyTry = 0;
        }
        publicKeyTry--;
        if (publicKeyTry <= 0) {
            // do not try again
            enablePublicKey = false;
            publicKeyTry = 3;
        }
    }

    private void authenticateWithRsaKey(final SshConnection sshConnection,
                                        final String username,
                                        final File rsaKey,
                                        final String key) throws IOException {
        boolean res;
        try {
            res = sshConnection.authenticateWithPublicKey(username, rsaKey, key);
            if (res) {
                LOG.debug("authenticate: rsa key auth successful");
                lastSuccessfulPassword.setDsaKey(null);
                lastSuccessfulPassword.setRsaKey(key);
                lastSuccessfulPassword.setPassword(null);
                authenticated = true;
                return;
            }
        } catch (final IOException e) {
            lastSuccessfulPassword.setRsaKey(null);
            LOG.debug("authenticate: rsa key failed");
        }
        sshConnection.close();
        final PopupHostKeyVerifier popupHostKeyVerifier = popupHostKeyVerifierProvider.get();
        popupHostKeyVerifier.init(sshGui);
        sshConnection.connect(popupHostKeyVerifier, connectTimeout, kexTimeout);
    }

    private void authenticateWithDsaKey(final SshConnection sshConnection,
                                        final String username,
                                        final File dsaKey,
                                        final String key) throws IOException {
        boolean res;
        try {
            res = sshConnection.authenticateWithPublicKey(username, dsaKey, key);
            if (res) {
                LOG.debug("authenticate: dsa key auth successful");
                lastSuccessfulPassword.setDsaKey(key);
                lastSuccessfulPassword.setRsaKey(null);
                lastSuccessfulPassword.setPassword(null);
                authenticated = true;
                return;
            }
        } catch (final IOException e) {
            lastSuccessfulPassword.setDsaKey(null);
            LOG.debug("authenticate: dsa key failed");
        }
        sshConnection.close();
        final PopupHostKeyVerifier popupHostKeyVerifier = popupHostKeyVerifierProvider.get();
        popupHostKeyVerifier.init(sshGui);
        sshConnection.connect(popupHostKeyVerifier, connectTimeout, kexTimeout);
    }

    private void authenticateWithKeyboardInteractive(final SshConnection sshConnection,
                                                     final String username) throws IOException {
        final InteractiveLogic interactiveLogic = new InteractiveLogic(lastError,
                                                                       host,
                                                                       lastSuccessfulPassword,
                                                                       sshGui);
        final boolean res = sshConnection.authenticateWithKeyboardInteractive(username, interactiveLogic);
        if (res) {
            lastSuccessfulPassword.setRsaKey(null);
            lastSuccessfulPassword.setDsaKey(null);
            authenticated = true;
            return;
        } else {
            lastSuccessfulPassword.setPassword(null);
        }
        if (interactiveLogic.getPromptCount() == 0) {
             /* "keyboard-interactive" denied */
            lastError = Tools.getString("SSH.KeyboardInteractive.DoesNotWork");
            /* do not try this again */
            enableKeyboardInteractive = false;
        } else {
            /* try again, if possible */
            lastError = Tools.getString("SSH.KeyboardInteractive.Failed");
        }
    }

    private void authenticateWithPassword(final SshConnection sshConnection, final String username) throws IOException {
        final String ans;
        if (lastSuccessfulPassword.getPassword() == null) {
            ans = sshGui.enterSomethingDialog(Tools.getString("SSH.PasswordAuthentication"),
                                              new String[]{lastError,
                                                           "<html>"
                                                           + host.getUserAtHost()
                                                           + Tools.getString("SSH.Enter.password")
                                                           + "</html>"},
                                              null,
                                              null,
                                              true);
            if (ans == null) {
                sshConnection.cancel();
                return;
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
        final boolean res = sshConnection.authenticateWithPassword(username, ans);
        if (res) {
            lastSuccessfulPassword.setPassword(ans);
            host.setSudoPassword(ans);
            lastSuccessfulPassword.setRsaKey(null);
            lastSuccessfulPassword.setDsaKey(null);
            authenticated = true;
            return;
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
