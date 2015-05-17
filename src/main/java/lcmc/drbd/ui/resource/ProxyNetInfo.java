/*
 * This file is part of Linux Cluster Management Console
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2012-2013, Rasto Levrinc
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

package lcmc.drbd.ui.resource;

import lcmc.host.domain.Host;
import lcmc.drbd.domain.NetInterface;
import lcmc.common.ui.Browser;
import lcmc.cluster.ui.resource.NetInfo;

import javax.inject.Named;

/**
 * This class holds info data for a net interface on a drbd proxy host.
 */
@Named
public final class ProxyNetInfo extends NetInfo {
    /** Prefix in the host address field indicating a proxy address. */
    public static final String PROXY_PREFIX = "proxy: ";

    /** Reformat to the form that appears in the GUI. */
    public static String displayString(final String someIp, final String someHost, final String someProxyHost) {
        final StringBuilder s = new StringBuilder(PROXY_PREFIX);
        if (someIp != null) {
            s.append(someIp);
        }
        if (!someHost.equals(someProxyHost)) {
            s.append(" \u2192 ");
            s.append(someProxyHost);
        }
        return s.toString();
    }

    private Host proxyHost;

    public void init(final NetInfo netInfo, final Browser browser, final Host proxyHost) {
        super.init(netInfo.getName(), netInfo.getNetInterface(), browser);
        this.proxyHost = proxyHost;
    }

    public void init(final String name, final NetInterface netInterface, final Browser browser, final Host proxyHost) {
        super.init(name, netInterface, browser);
        this.proxyHost = proxyHost;
    }

    @Override
    public String toString() {
        final String ip = super.getInternalValue();
        String proxyHostName = null;
        if (proxyHost != null) {
            proxyHostName = proxyHost.getName();
        }
        return displayString(ip, getBrowser().getHost().getName(), proxyHostName);
    }

    public Host getProxyHost() {
        return proxyHost;
    }

    @Override
    public String getValueForConfig() {
        return toString();
    }
}
