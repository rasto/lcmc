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

import com.trilead.ssh2.Connection;

/** Connection class that can cancel it's connection during openSession. */
public class SshConnection extends Connection {
    private boolean canceled = false;
    private boolean disconnectForGood = false;

    SshConnection(final String hostname, final int port) {
        super(hostname, port);
    }

    void cancel() {
        canceled = true;
        /* public getChannelManager() { return cm }
        has to be added to the Connection.java till
        it's sorted out. */
        //FIXME
//        final ChannelManager cm = getChannelManager();
//        if (cm != null) {
//            cm.closeAllChannels();
//        }
    }

    boolean isCanceled() {
        return canceled;
    }

    void disconnectForGood() {
        disconnectForGood = true;
    }

    boolean isDisconnectedForGood() {
        return disconnectForGood;
    }
}