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

import java.io.File;
import java.io.IOException;
import lcmc.cluster.ui.SSHGui;
import lcmc.common.domain.Application;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;

import javax.inject.Inject;
import javax.inject.Named;

import com.trilead.ssh2.KnownHosts;
import com.trilead.ssh2.ServerHostKeyVerifier;

/**
 * This ServerHostKeyVerifier asks the user on how to proceed if a key
 * cannot be found in the in-memory database.
 */
@Named
public class PopupHostKeyVerifier implements ServerHostKeyVerifier {
    private static final Logger LOG = LoggerFactory.getLogger(PopupHostKeyVerifier.class);

    private SSHGui sshGui;
    @Inject
    private Application application;

    public void init(final SSHGui sshGui) {
        this.sshGui = sshGui;
    }

    /** Verifies the keys. */
    @Override
    public boolean verifyServerHostKey(final String hostname,
                                       final int port,
                                       final String serverHostKeyAlgorithm,
                                       final byte[] serverHostKey) throws Exception {
        final int hostKeyResult = application.getKnownHosts().verifyHostkey(hostname,
                                                                            serverHostKeyAlgorithm,
                                                                            serverHostKey);
        if (hostKeyResult == KnownHosts.HOSTKEY_IS_OK) {
            return true;
        }
        if (hostKeyResult != KnownHosts.HOSTKEY_IS_NEW && hostKeyResult != KnownHosts.HOSTKEY_HAS_CHANGED) {
            throw new IllegalStateException();
        }
        final String message = createHostKeyMessage(hostname, serverHostKeyAlgorithm, serverHostKey, hostKeyResult);
        final int choice = sshGui.getConfirmDialogChoice(message);
        if (sshGui.isConfirmDialogYes(choice)) {
            addHostKeyToDatabase(hostname, serverHostKeyAlgorithm,
                    serverHostKey);
            return true;
        }
        if (sshGui.isConfirmDialogCancel(choice)) {
            throw new Exception("The user aborted the server hostkey verification.");
        }
        return false;
    }

    private void addHostKeyToDatabase(final String hostname,
                                      final String serverHostKeyAlgorithm,
                                      final byte[] serverHostKey)
            throws IOException {
        final String hashedHostname = KnownHosts.createHashedHostname(hostname);
        application.getKnownHosts().addHostkey(new String[]{hashedHostname}, serverHostKeyAlgorithm, serverHostKey);
        /* Also try to add the key to a known_host file */
        /* It does this only in Linux.
         * TODO: do this also for other OSes, when I find out the
         * known_hosts locations.
         */
        if (Tools.isWindows()) {
            LOG.debug("verifyServerHostKey: not using known_hosts" + " file, because this is Windows.");
        } else {
            try {
                KnownHosts.addHostkeyToFile(new File(application.getKnownHostPath()),
                                            new String[]{hashedHostname},
                                            serverHostKeyAlgorithm,
                                            serverHostKey);
            } catch (final IOException ignore) {
                LOG.appWarning("verifyServerHostKey: SSH " + "addHostKeyToFile failed " + ignore.getMessage());
            }
        }
    }

    private String createHostKeyMessage(final String hostname,
                                        final String serverHostKeyAlgorithm,
                                        final byte[] serverHostKey,
                                        final int hostKeyResult) {
        final StringBuilder message = new StringBuilder(200);
        if (KnownHosts.HOSTKEY_IS_NEW == hostKeyResult) {
                message.append("Do you want to accept the hostkey (type ");
                message.append(serverHostKeyAlgorithm);
                message.append(") from ");
                message.append(hostname);
                message.append(" ?\n");
        } else if (KnownHosts.HOSTKEY_HAS_CHANGED == hostKeyResult) {
                message.append("WARNING! Hostkey for ");
                message.append(hostname);
                message.append(" has changed!\nAccept anyway?\n");
        }
        final String hexFingerprint = KnownHosts.createHexFingerprint(serverHostKeyAlgorithm, serverHostKey);
        final String bubblebabbleFingerprint = KnownHosts.createBubblebabbleFingerprint(serverHostKeyAlgorithm,
                                                                                        serverHostKey);
        message.append("Hex Fingerprint: ");
        message.append(hexFingerprint);
        message.append("\nBubblebabble Fingerprint: ");
        message.append(bubblebabbleFingerprint);
        return message.toString();
    }
}
