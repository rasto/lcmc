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

package lcmc.event;

import lcmc.cluster.domain.Cluster;
import lcmc.cluster.ui.resource.CommonBlockDevInfo;

import java.util.Collection;

public class CommonBlockDevicesChangedEvent {
    private final Cluster cluster;
    private final Collection<CommonBlockDevInfo> commonBlockDevViews;

    public CommonBlockDevicesChangedEvent(
            final Cluster cluster,
            final Collection<CommonBlockDevInfo> commonBlockDevViews) {
        this.cluster = cluster;
        this.commonBlockDevViews = commonBlockDevViews;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public Collection<CommonBlockDevInfo> getCommonBlockDevViews() {
        return commonBlockDevViews;
    }
}
