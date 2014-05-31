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

import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import java.io.File;
import java.io.IOException;
import lcmc.gui.SSHGui;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

/**
 * This ServerHostKeyVerifier asks the user on how to proceed if a key
 * cannot be found in the in-memory database.
 */
public class PopupHostKeyVerifier implements ServerHostKeyVerifier {
    private static final Logger LOG = LoggerFactory.getLogger(PopupHostKeyVerifier.class);

    private final SSHGui sshGui;

    public PopupHostKeyVerifier(final SSHGui sshGui) {
        this.sshGui = sshGui;
    }

    /** Verifies the keys. */
    @Override
    public boolean verifyServerHostKey(final String hostname,
                                       final int port,
                                       final String serverHostKeyAlgorithm,
                                       final byte[] serverHostKey) throws Exception {
        final StringBuilder message = new StringBuilder(200);
        /* Check database */
        final int result = Tools.getApplication().getKnownHosts().
            verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);
        switch (result) {
            case KnownHosts.HOSTKEY_IS_OK:
                return true;
            case KnownHosts.HOSTKEY_IS_NEW:
                message.append("Do you want to accept the hostkey (type ");
                message.append(serverHostKeyAlgorithm);
                message.append(") from ");
                message.append(hostname);
                message.append(" ?\n");
                break;
            case KnownHosts.HOSTKEY_HAS_CHANGED:
                message.append("WARNING! Hostkey for ");
                message.append(hostname);
                message.append(" has changed!\nAccept anyway?\n");
                break;
            default:
                throw new IllegalStateException();
        }
        /* Include the fingerprints in the message */
        final String hexFingerprint = KnownHosts.createHexFingerprint(serverHostKeyAlgorithm, serverHostKey);
        final String bubblebabbleFingerprint = KnownHosts.createBubblebabbleFingerprint(serverHostKeyAlgorithm,
                                                                                        serverHostKey);
        message.append("Hex Fingerprint: ");
        message.append(hexFingerprint);
        message.append("\nBubblebabble Fingerprint: ");
        message.append(bubblebabbleFingerprint);
        /* Now ask the user */
        final int choice = sshGui.getConfirmDialogChoice(message.toString());
        if (sshGui.isConfirmDialogYes(choice)) {
            /* Be really paranoid. We use a hashed hostname entry */
            final String hashedHostname = KnownHosts.createHashedHostname(hostname);
            /* Add the hostkey to the in-memory database */
            Tools.getApplication().getKnownHosts().addHostkey(new String[]{hashedHostname},
                                                              serverHostKeyAlgorithm,
                                                              serverHostKey);
            /* Also try to add the key to a known_host file */
            /* It does this only in Linux.
             * TODO: do this also for other OSes, when I find out the
             * known_hosts locations.
             */
            if (Tools.isWindows()) {
                LOG.debug("verifyServerHostKey: not using known_hosts" + " file, because this is Windows.");
            } else {
                try {
                    KnownHosts.addHostkeyToFile(new File(Tools.getApplication().
                        getKnownHostPath()), new String[]{hashedHostname},
                                                          serverHostKeyAlgorithm,
                                                          serverHostKey);
                } catch (final IOException ignore) {
                    LOG.appWarning("verifyServerHostKey: SSH " + "addHostKeyToFile failed " + ignore.getMessage());
                }
            }
            return true;
        }
        if (sshGui.isConfirmDialogCancel(choice)) {
            throw new Exception("The user aborted the server hostkey verification.");
        }
        return false;
    }
}