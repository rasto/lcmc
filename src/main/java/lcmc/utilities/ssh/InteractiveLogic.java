package lcmc.utilities.ssh;

import ch.ethz.ssh2.InteractiveCallback;
import java.io.IOException;
import lcmc.data.Host;
import lcmc.gui.SSHGui;

/**
 * The logic that one has to implement if "keyboard-interactive"
 * autentication shall be supported.
 */
public class InteractiveLogic implements InteractiveCallback {
    /** Prompt count. */
    private int promptCount = 0;
    /** To show error only once.  */
    private String lastError;
    private final Host host;
    private final SSHGui sshGui;
    private String lastPassword;

    InteractiveLogic(final String lastError,
                     final Host host,
                     final String lastPassword,
                     final SSHGui sshGui) {
        this.lastError = lastError;
        this.host = host;
        this.lastPassword = lastPassword;
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
            final String[] content = new String[]{lastError, name, instruction, "<html><font color=red>" + prompt[i] + "</font>" + "</html>"};
            if (lastError != null) {
                /* show lastError only once */
                lastError = null;
            }
            final String ans;
            if (lastPassword == null) {
                ans = sshGui.enterSomethingDialog("Keyboard Interactive Authentication",
                                                  content,
                                                  null,
                                                  null,
                                                  !echo[i]);
                if (ans == null) {
                    throw new IOException("cancelled");
                }
                lastPassword = ans;
                host.setSudoPassword(lastPassword);
            } else {
                ans = lastPassword;
                host.setSudoPassword(lastPassword);
            }
            result[i] = ans;
            promptCount++;
        }
        return result;
    }

    public String getLastPassword() {
        return lastPassword;
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
