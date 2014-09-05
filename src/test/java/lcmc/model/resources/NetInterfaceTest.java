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

package lcmc.model.resources;

import java.net.UnknownHostException;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class NetInterfaceTest {
    @Test
    public void bridgeIpv6WithCidr64ShouldSetNetworkIp() throws UnknownHostException {
        final NetInterface ni = new NetInterface("eth0 ipv6 2001:db8:0:f101::1 64");
        assertEquals("2001:db8:0:f101:0:0:0:0", ni.getNetworkIp());
    }

    @Test
    public void ipv4WithCidr23ShouldSetNetworkIp() throws UnknownHostException {
        final NetInterface ni = new NetInterface("p5p1 ipv4 192.168.1.101 23");
        assertEquals("192.168.0.0", ni.getNetworkIp());
    }

    @Test
    public void bridgeIpv4WithCidr24ShouldSetNetworkIp() throws UnknownHostException {
        final NetInterface ni = new NetInterface("virbr0 ipv4 192.168.133.1 24 bridge");
        assertEquals("192.168.133.0", ni.getNetworkIp());
    }

    @Test
    public void bridgeIpWithCidr16ShouldSetNetworkIp() throws UnknownHostException {
        final NetInterface ni = new NetInterface("virbr1 ipv4 10.10.0.1 16 bridge");
        assertEquals("10.10.0.0", ni.getNetworkIp());
    }
}
