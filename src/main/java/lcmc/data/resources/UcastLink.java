/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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


package lcmc.data.resources;

import lcmc.data.Host;
import lcmc.data.Value;
import lcmc.utilities.Unit;

/**
 * This class holds data of one ucast link for heartbeat.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class UcastLink extends Resource implements Value {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Host object. */
    private final Host host;
    /** Net interface. */
    private final NetInterface netInterface;

    /**
     * Prepares a new <code>UcastLink</code> object.
     */
    public UcastLink(final Host host, final NetInterface netInterface) {
        super();
        this.host = host;
        this.netInterface = netInterface;
    }

    /** Rerurns the string. */
    @Override
    public String toString() {
        return host.getName() + ":" + netInterface.getName();
    }

    /** Returns net interface. */
    public String getInterface() {
        return netInterface.getName();
    }

    /** Returns the ip. */
    public String getIp() {
        return netInterface.getIp();
    }

    /** Returns the host. */
    public Host getHost() {
        return host;
    }

    @Override
    public String getValueForGui() {
        return toString();
    }

    @Override
    public String getValueForConfig() {
        return getInterface();
    }

    @Override
    public boolean isNothingSelected() {
        return netInterface == null;
    }

    @Override
    public Unit getUnit() {
        return null;
    }

    @Override
    public String getValueForConfigWithUnit() {
        return getValueForConfig();
    }

    @Override
    public String getNothingSelected() {
        return NOTHING_SELECTED;
    }
}
