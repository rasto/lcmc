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

package lcmc.cluster.ui.resource;

import lcmc.cluster.domain.Cluster;
import lcmc.common.domain.ResourceValue;
import lcmc.common.ui.Browser;
import lcmc.common.ui.Info;
import lcmc.drbd.domain.NetInterface;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ClusterViewFactory {
    private final Supplier<NetInfo> netInfoProvider;
    private final Supplier<FSInfo> fsInfoProvider;
    private final Supplier<CommonBlockDevInfo> commonBlockDevInfoProvider;

    private final ConcurrentMap<ResourceValue, Info> viewByResource = new ConcurrentHashMap<ResourceValue, Info>();
    private final ConcurrentMap<String, Info> viewByFileSystemName = new ConcurrentHashMap<String, Info>();

    private final Lock viewLock = new ReentrantLock();

    public FSInfo createFileSystemView(final String fileSystem, final Browser browser) {
        final FSInfo fsInfo = fsInfoProvider.get();
        fsInfo.init(fileSystem, browser);
        viewByFileSystemName.put(fileSystem, fsInfo);
        return fsInfo;
    }

    public Info getNetView(NetInterface netInterface, final Browser browser) {
        viewLock.lock();
        try {
            final Info view = viewByResource.get(netInterface);
            if (view == null) {
                return createNetView(netInterface, browser);
            }
            return view;
        } finally {
             viewLock.unlock();
        }
    }

    public CommonBlockDevInfo createCommonBlockDevView(final Cluster cluster, final String commonBlockDevice) {
        final CommonBlockDevInfo commonBlockDevInfo = commonBlockDevInfoProvider.get();
        commonBlockDevInfo.init(
                commonBlockDevice,
                cluster.getHostBlockDevices(commonBlockDevice),
                cluster.getBrowser());
        return commonBlockDevInfo;
    }

    private Info createNetView(final NetInterface netInterface, final Browser browser) {
        final NetInfo netInfo = netInfoProvider.get();
        viewByResource.put(netInterface, netInfo);
        netInfo.init(netInterface.getName(), netInterface, browser);
        return netInfo;
    }
}
