package lcmc.data.resources;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import java.net.UnknownHostException;
import lcmc.utilities.TestSuite1;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

public final class NetInterfaceTest1 extends TestCase {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(NetInterfaceTest1.class);
    @Before
    @Override
    protected void setUp() {
        TestSuite1.initTest();
    }

    @After
    @Override
    protected void tearDown() {
        assertEquals("", TestSuite1.getStdout());
    }

    /* ---- tests ----- */

    @Test
    public void testInit() {
        try {
            NetInterface ni = new NetInterface("");
        } catch (UnknownHostException e) {
            fail();
        }
        assertTrue(TestSuite1.getStdout().contains("cannot parse"));
        TestSuite1.clearStdout();
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

        TestSuite1.clearStdout();
    }
}
