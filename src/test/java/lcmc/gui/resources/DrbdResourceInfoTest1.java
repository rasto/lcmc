package lcmc.gui.resources;

import java.util.Arrays;
import java.util.LinkedHashSet;

import junit.framework.TestCase;
import lcmc.data.Host;
import lcmc.utilities.TestSuite1;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.resources.drbd.DrbdResourceInfo;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;

public final class DrbdResourceInfoTest1 extends TestCase {
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
    public void testNotEqualNames() {

        final ClusterBrowser b =
                TestSuite1.getHosts().get(0).getBrowser().getClusterBrowser();
        final DrbdResourceInfo r1 = new DrbdResourceInfo("name1", null, b);
        final DrbdResourceInfo r2 = new DrbdResourceInfo("name2", null, b);
        assertFalse("not equal names", r1.equals(r2));
    }

    @Test
    public void testEqualNames() {

        final ClusterBrowser b =
                TestSuite1.getHosts().get(0).getBrowser().getClusterBrowser();
        final DrbdResourceInfo r1 = new DrbdResourceInfo("name", null, b);
        final DrbdResourceInfo r2 = new DrbdResourceInfo("name", null, b);
        assertTrue("equal names", r1.equals(r2));
    }

    @Test
    public void testNameNull() {

        final ClusterBrowser b =
                TestSuite1.getHosts().get(0).getBrowser().getClusterBrowser();
        final DrbdResourceInfo r1 = new DrbdResourceInfo("name", null, b);
        assertFalse("equal name null", r1.getName() == null);
    }

    @Test
    public void testEqualNamesNotEqualsHosts() {

        final ClusterBrowser b =
                TestSuite1.getHosts().get(0).getBrowser().getClusterBrowser();
        
        final DrbdResourceInfo r1 =
                new DrbdResourceInfo("name", new LinkedHashSet<Host>(
                                                 Arrays.asList(new Host())), b);
        final DrbdResourceInfo r2 = new DrbdResourceInfo("name", null, b);
        assertTrue("equal names", r1.equals(r2));
    }
}
