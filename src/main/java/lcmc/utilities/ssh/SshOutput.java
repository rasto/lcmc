package lcmc.utilities.ssh;

/** Class that holds output of ssh command. */
public final class SshOutput {
    /** Output string. */
    private final String output;
    /** Exit code. */
    private final int exitCode;

    /** Creates new SSHOutput object. */
    public SshOutput(final String output, final int exitCode) {
        this.output = output;
        this.exitCode = exitCode;
    }

    /** Returns output string. */
    public String getOutput() {
        return output;
    }

    /** Returns exit code. */
    public int getExitCode() {
        return exitCode;
    }
    
}
