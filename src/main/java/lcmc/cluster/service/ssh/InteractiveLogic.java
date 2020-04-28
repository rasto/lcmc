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

import com.trilead.ssh2.InteractiveCallback;

import lcmc.host.domain.Host;
import lcmc.cluster.ui.SSHGui;

/**
 * The logic that one has to implement if "keyboard-interactive"
 * authentication shall be supported.
 */
public class InteractiveLogic implements InteractiveCallback {
    private int promptCount = 0;
    /** To show error only once.  */
    private String lastError;
    private final Host host;
    private final SSHGui sshGui;
    private final LastSuccessfulPassword lastSuccessfulPassword;

    InteractiveLogic(final String lastError,
                     final Host host,
                     final LastSuccessfulPassword lastSuccessfulPassword,
                     final SSHGui sshGui) {
        this.lastError = lastError;
        this.host = host;
        this.lastSuccessfulPassword = lastSuccessfulPassword;
        this.sshGui = sshGui;
    }

    /**
     * The callback may be invoked several times, depending on how many
     * questions-sets the server sends.
     */
    @Override
    public String[] replyToChallenge(final String name,
                                     final String instruction,
                                     final int numPrompts,
                                     final String[] prompt,
                                     final boolean[] echo) throws IOException {
        final String[] result = new String[numPrompts];
        for (int i = 0; i < numPrompts; i++) {
            /* Often, servers just send empty strings for "name" and
             * "instruction" */
            final String[] content = new String[]{lastError,
                                                  name,
                                                  instruction,
                                                  "<html><font color=red>" + prompt[i] + "</font>" + "</html>"};
            if (lastError != null) {
                /* show lastError only once */
                lastError = null;
            }
            final String ans;
            if (lastSuccessfulPassword.getPassword() == null) {
                ans = sshGui.enterSomethingDialog("Keyboard Interactive Authentication",
                                                  content,
                                                  null,
                                                  null,
                                                  !echo[i]);
                if (ans == null) {
                    throw new IOException("cancelled");
                }
                lastSuccessfulPassword.setPassword(ans);
                host.setSudoPassword(ans);
            } else {
                ans = lastSuccessfulPassword.getPassword();
                host.setSudoPassword(ans);
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
    int getPromptCount() {
        return promptCount;
    }
}