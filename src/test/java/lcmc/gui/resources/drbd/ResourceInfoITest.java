package lcmc.gui.resources.drbd;

import java.util.Arrays;
import java.util.LinkedHashSet;
import lcmc.model.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.model.HostFactory;
import lcmc.testutils.TestUtils;
import lcmc.testutils.annotation.type.IntegrationTest;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public final class ResourceInfoITest {

    private final TestUtils testSuite = new TestUtils();

    @Before
    public void setUp() {
        testSuite.initTestCluster();
    }

    @Test
    public void testNotEqualNames() {
        final ClusterBrowser clusterBrowser = testSuite.getHosts().get(0).getBrowser().getClusterBrowser();
        final ResourceInfo r1 = new ResourceInfo();
        r1.init("name1", null, clusterBrowser);
        final ResourceInfo r2 = new ResourceInfo();
        r2.init("name2", null, clusterBrowser);

        assertFalse("not equal names", r1.equals(r2));
    }

    @Test
    public void testEqualNames() {

        final ClusterBrowser clusterBrowser = testSuite.getHosts().get(0).getBrowser().getClusterBrowser();
        final ResourceInfo r1 = new ResourceInfo();
        r1.init("name", null, clusterBrowser);
        final ResourceInfo r2 = new ResourceInfo();
        r2.init("name", null, clusterBrowser);

        assertTrue("equal names", r1.equals(r2));
    }

    @Test
    public void testNameNull() {
        final ClusterBrowser clusterBrowser = testSuite.getHosts().get(0).getBrowser().getClusterBrowser();
        final ResourceInfo r1 = new ResourceInfo();
        r1.init("name", null, clusterBrowser);

        assertFalse("equal name null", r1.getName() == null);
    }

    @Test
    public void testEqualNamesNotEqualsHosts() {
        final ClusterBrowser clusterBrowser = testSuite.getHosts().get(0).getBrowser().getClusterBrowser();

        final HostFactory hostFactory = new HostFactory();
        final ResourceInfo r1 = new ResourceInfo();
        r1.init("name", new LinkedHashSet<Host>(Arrays.asList(hostFactory.createInstance())), clusterBrowser);
        final ResourceInfo r2 = new ResourceInfo();
        r2.init("name", null, clusterBrowser);

        assertTrue("equal names", r1.equals(r2));
    }
}
