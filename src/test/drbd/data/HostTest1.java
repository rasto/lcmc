package drbd.utilities;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Vector;
import java.awt.Dimension;
import java.awt.Color;
import javax.swing.JPanel;
import java.net.InetAddress;
import drbd.TestSuite1;
import drbd.data.Host;

public final class HostTest1 extends TestCase {
    @Before
    protected void setUp() {
        TestSuite1.initTest();
    }

    @After
    protected void tearDown() {
        assertEquals("", TestSuite1.getStdout());
    }

    /* ---- tests ----- */

    @Test
    public void testGetBrowser() {
        for (final Host host : TestSuite1.getHosts()) {
            assertNotNull(host.getBrowser());
        }
    }

    @Test
    public void testGetDrbdColors() {
        final Set<Color> hostColors = new HashSet<Color>();
        for (final Host host : TestSuite1.getHosts()) {
            final Color[] colors = host.getDrbdColors();
            assertNotNull(colors);
            assertTrue(colors.length > 0 && colors.length <= 2);
            assertFalse(hostColors.contains(colors[0]));
            hostColors.add(colors[0]);
        }
    }

    @Test
    public void testGetPmColors() {
        final Set<Color> hostColors = new HashSet<Color>();
        for (final Host host : TestSuite1.getHosts()) {
            final Color[] colors = host.getPmColors();
            assertNotNull(colors);
            assertTrue(colors.length > 0 && colors.length <= 2);
            assertFalse(hostColors.contains(colors[0]));
            hostColors.add(colors[0]);
        }
    }

}
