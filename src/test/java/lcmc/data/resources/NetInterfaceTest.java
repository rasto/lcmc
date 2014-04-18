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

package lcmc.data.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;

import lcmc.testutils.TestUtils;
import lcmc.utilities.Tools;

import org.junit.Before;
import org.junit.Test;

public final class NetInterfaceTest {

    private final TestUtils testSuite = new TestUtils();

    @Before
    public void setUp() {
        testSuite.initStdout();
        Tools.init();
    }

    @Test
    public void testInit() {
        try {
            new NetInterface("");
        } catch (UnknownHostException e) {
            fail();
        }
        assertTrue(testSuite.getStdout().contains("cannot parse"));
        testSuite.clearStdout();
        //lo ipv6 ::1 128
        //    lo ipv4 127.0.0.1 8
        try {
            final NetInterface ni =
                           new NetInterface("eth0 ipv6 2001:db8:0:f101::1 64");
            assertEquals("2001:db8:0:f101:0:0:0:0", ni.getNetworkIp());
        } catch (UnknownHostException e) {
            fail();
        }

        try {
            final NetInterface ni
                              = new NetInterface("p5p1 ipv4 192.168.1.101 24");
            assertEquals("192.168.1.0", ni.getNetworkIp());
        } catch (UnknownHostException e) {
            fail();
        }

        try {
            final NetInterface ni =
                                new NetInterface("p5p1 ipv4 192.168.1.101 23");
            assertEquals("192.168.0.0", ni.getNetworkIp());
        } catch (UnknownHostException e) {
            fail();
        }

        try {
            final NetInterface ni =
                       new NetInterface("virbr0 ipv4 192.168.133.1 24 bridge");
            assertEquals("192.168.133.0", ni.getNetworkIp());
        } catch (UnknownHostException e) {
            fail();
        }

        try {
            final NetInterface ni =
                           new NetInterface("virbr1 ipv4 10.10.0.1 16 bridge");
            assertEquals("10.10.0.0", ni.getNetworkIp());
        } catch (UnknownHostException e) {
            fail();
        }

        testSuite.clearStdout();
    }
}
