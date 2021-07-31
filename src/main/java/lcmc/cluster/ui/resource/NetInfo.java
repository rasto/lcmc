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

package lcmc.cluster.ui.resource;

import javax.inject.Named;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;

import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.SshOutput;
import lcmc.common.domain.Application;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.common.ui.Info;
import lcmc.drbd.domain.NetInterface;
import lcmc.host.domain.Host;
import lcmc.host.ui.HostBrowser;

/**
 * This class holds info data for a net interface.
 */
@Named
public class NetInfo extends Info {
    public static final ImageIcon NET_INTERFACE_ICON =
                                                    Tools.createImageIcon(Tools.getDefault("HostBrowser.NetIntIcon"));
    public static final ImageIcon NET_INTERFACE_ICON_LARGE =
                                               Tools.createImageIcon(Tools.getDefault("HostBrowser.NetIntIconLarge"));
    public static final String IP_PLACEHOLDER = "--.--.--.--";
    private NetInterface netInterface;

    public void init(final String name, final NetInterface netInterface, final Browser browser) {
        this.netInterface = netInterface;
        super.init(name, browser);
    }

    @Override
    public final HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    /** Returns info of this net interface, which is updatable. */
    @Override
    public final void updateInfo(final JEditorPane ep) {
        final Runnable runnable = () -> {
            final Host host = getBrowser().getHost();
            final SshOutput ret = host.captureCommand(new ExecCommandConfig().command("/sbin/ip a l " + getName()));
            ep.setText(ret.getOutput());
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

    @Override
    public final ImageIcon getMenuIcon(final Application.RunMode runMode) {
        return NET_INTERFACE_ICON;
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

    public final NetInterface getNetInterface() {
        return netInterface;
    }

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
        hash = 59 * hash + (getValueForConfig() != null ? getValueForConfig().hashCode() : 0);
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
        return (getValueForConfig() == null) ? (other.getValueForConfig() == null) : getValueForConfig().equals(
                other.getValueForConfig());
    }
}
