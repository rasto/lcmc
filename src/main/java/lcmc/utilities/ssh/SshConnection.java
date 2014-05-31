package lcmc.utilities.ssh;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.channel.ChannelManager;

/** Connection class that can cancel it's connection during openSession. */
public class SshConnection extends Connection {
    private boolean closed = false;

    /** Creates new MyConnection object. */
    SshConnection(final String hostname, final int port) {
        super(hostname, port);
    }

    public void setClosed() {
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    /** Cancel from application. */
    void dmcCancel() {
        /* public getChannelManager() { return cm }
        has to be added to the Connection.java till
        it's sorted out. */
        final ChannelManager cm = getChannelManager();
        if (cm != null) {
            cm.closeAllChannels();
        }
    }
    
}
