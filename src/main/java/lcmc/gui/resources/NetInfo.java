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
import javax.swing.JEditorPane;
import lcmc.data.Application;
import lcmc.data.Value;
import lcmc.data.resources.NetInterface;
import lcmc.gui.Browser;
import lcmc.gui.HostBrowser;
import lcmc.utilities.SSH;
import lcmc.utilities.Tools;

/**
 * This class holds info data for a net interface.
 */
@SuppressWarnings("SingleCharacterStringConcatenation")
public class NetInfo extends Info {
    /** Net interface icon. */
    public static final ImageIcon NET_I_ICON = Tools.createImageIcon(
                                   Tools.getDefault("HostBrowser.NetIntIcon"));
    /** Net interface icon. */
    public static final ImageIcon NET_I_ICON_LARGE = Tools.createImageIcon(
                              Tools.getDefault("HostBrowser.NetIntIconLarge"));
    /** Placeholder where user can enter an ip. */
    public static final String IP_PLACEHOLDER = "--.--.--.--";
    /** Prepares a new {@code NetInfo} object. */
    public NetInfo(final String name,
                   final NetInterface netInterface,
                   final Browser browser) {
        super(name, browser);
        setResource(netInterface);
    }

    /** Returns browser object of this info. */
    @Override
    public final HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    /** Returns info of this net interface, which is updatable. */
    @Override
    public final void updateInfo(final JEditorPane ep) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final SSH.SSHOutput ret =
                              Tools.execCommand(getBrowser().getHost(),
                                                "/sbin/ip a l "
                                                + getName(),
                                                null,   /* ExecCallback */
                                                false,  /* outputVisible */
                                                SSH.DEFAULT_COMMAND_TIMEOUT);
                ep.setText(ret.getOutput());
            }
        };
        final Thread thread = new Thread(runnable);
        thread.start();
    }

    /** Returns string representation of the net interface. */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(getName());
        final String ip = getNetInterface().getIp();
        if (ip != null) {
            s.append(" (").append(ip).append(')');
        }
        return s.toString();
    }

    /** Returns icon of the net interface for the menu. */
    @Override
    public final ImageIcon getMenuIcon(final Application.RunMode runMode) {
        return NET_I_ICON;
    }

    /** Returns ip of the net interface. */
    @Override
    public final String getInternalValue() {
        final NetInterface ni = getNetInterface();
        if (ni == null) {
            return IP_PLACEHOLDER;
        }
        return ni.getIp();
    }

    /** Returns net interface resource. */
    public final NetInterface getNetInterface() {
        return (NetInterface) getResource();
    }

    /**
     * Whether this interface is localhost.
     */
    public final boolean isLocalHost() {
        return "lo".equals(getName());
    }

    @Override
    public String getValueForConfig() {
        return getNetInterface().getIp();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (this.getValueForConfig() != null ? this.getValueForConfig().hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Value)) {
            return false;
        }
        final Value other = (Value) obj;
        if ((this.getValueForConfig() == null) ? (other.getValueForConfig() != null) : !this.getValueForConfig().equals(other.getValueForConfig())) {
            return false;
        }
        return true;
    }

}
