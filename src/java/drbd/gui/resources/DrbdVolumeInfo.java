/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package drbd.gui.resources;

import drbd.gui.Browser;
import drbd.utilities.Tools;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.util.List;
import java.util.Collections;


/**
 * This class holds info data of a DRBD volume.
 */
public final class DrbdVolumeInfo extends Info {
    /** Drbd resource in which is this volume defined. */
    private final DrbdResourceInfo drbdResourceInfo;
    /** Block devices that are in this DRBD volume. */
    private final List<BlockDevInfo> blockDevInfos;
    /** Volume icon. */
    private static final ImageIcon VOLUME_ICON =
                    Tools.createImageIcon(
                            Tools.getDefault("ClusterBrowser.NetworkIcon"));
    /** Prepares a new <code>DrbdVolumeInfo</code> object. */
    public DrbdVolumeInfo(final String name,
                          final DrbdResourceInfo drbdResourceInfo,
                          final List<BlockDevInfo> blockDevInfos,
                          final Browser browser) {
        super(name, browser);
        assert(drbdResourceInfo != null);
        assert(blockDevInfos.size() >= 2);

        this.drbdResourceInfo = drbdResourceInfo;
        this.blockDevInfos = Collections.unmodifiableList(blockDevInfos);
    }

    /** Returns info panel. */
    @Override public JComponent getInfoPanel() {
        return drbdResourceInfo.getInfoPanel();
    }

    /** Returns menu icon. */
    @Override public ImageIcon getMenuIcon(final boolean testOnly) {
        return VOLUME_ICON;
    }
}
