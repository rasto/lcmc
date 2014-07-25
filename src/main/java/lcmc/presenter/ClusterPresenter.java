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

package lcmc.presenter;

import lcmc.model.Cluster;
import lcmc.utilities.Tools;
import org.springframework.stereotype.Component;

@Component
public class ClusterPresenter {
    public void onCloseCluster(final Cluster cluster) {
        disconnectCluster(cluster);
    }

    private void disconnectCluster(final Cluster cluster) {
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if (cluster.isTabClosable()) {
                    cluster.removeClusterAndDisconnect();
                    Tools.getGUIData().getEmptyBrowser().setDisconnected(cluster);
                }
            }
        });
        t.start();
    }
}
