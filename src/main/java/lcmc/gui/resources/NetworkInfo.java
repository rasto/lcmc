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
package lcmc.gui.resources;

import javax.swing.ImageIcon;
import lcmc.model.Application;
import lcmc.model.resources.Network;
import lcmc.gui.Browser;
import lcmc.utilities.Tools;

/**
 * This class holds info data for a network.
 */
public final class NetworkInfo extends Info {
    private static final ImageIcon NETWORK_ICON = Tools.createImageIcon(Tools.getDefault("ClusterBrowser.NetworkIcon"));

    public NetworkInfo(final String name, final Network network, final Browser browser) {
        super.init(name, browser);
        setResource(network);
    }

    /** Returns network info. */
    @Override
    public String getInfo() {
        return "Network: " + getNetwork().getName()
                           + "\n IPs: " + getNetwork().getAllIPs()
                           + "\nCIDR: " + getNetwork().getCidr()
                           + '\n';
    }

    Network getNetwork() {
        return (Network) getResource();
    }

    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        return NETWORK_ICON;
    }
}
