package lcmc.drbd.ui.resource;

import java.util.Arrays;
import java.util.LinkedHashSet;

import lcmc.host.domain.Host;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.host.domain.HostFactory;
import lcmc.testutils.IntegrationTestLauncher;
import lcmc.testutils.annotation.type.IntegrationTest;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public final class ResourceInfoITest {
    private IntegrationTestLauncher integrationTestLauncher;

    @Before
    public void setUp() {
        integrationTestLauncher = IntegrationTestLauncher.create();
        integrationTestLauncher.initTestCluster();
    }

    @Test
    public void testNotEqualNames() {
        final ClusterBrowser clusterBrowser = integrationTestLauncher.getHosts().get(0).getBrowser().getClusterBrowser();
        final ResourceInfo r1 = new ResourceInfo(
                null,
                null,
                null,
                null,
                null,
                null
        );
        r1.init("name1", null, clusterBrowser);
        final ResourceInfo r2 = new ResourceInfo(
                null,
                null,
                null,
                null,
                null,
                null
        );
        r2.init("name2", null, clusterBrowser);

        assertFalse("not equal names", r1.equals(r2));
    }

    @Test
    public void testEqualNames() {
        final ClusterBrowser clusterBrowser = integrationTestLauncher.getHosts().get(0).getBrowser().getClusterBrowser();
        final ResourceInfo r1 = new ResourceInfo(
                null,
                null,
                null,
                null,
                null,
                null
        );
        r1.init("name", null, clusterBrowser);
        final ResourceInfo r2 = new ResourceInfo(
                null,
                null,
                null,
                null,
                null,
                null
        );
        r2.init("name", null, clusterBrowser);

        assertTrue("equal names", r1.equals(r2));
    }

    @Test
    public void testNameNull() {
        final ClusterBrowser clusterBrowser = integrationTestLauncher.getHosts().get(0).getBrowser().getClusterBrowser();
        final ResourceInfo r1 = new ResourceInfo(
                null,
                null,
                null,
                null,
                null,
                null
        );
        r1.init("name", null, clusterBrowser);

        assertFalse("equal name null", r1.getName() == null);
    }

    @Test
    public void testEqualNamesNotEqualsHosts() {
        final ClusterBrowser clusterBrowser = integrationTestLauncher.getHosts().get(0).getBrowser().getClusterBrowser();
        final HostFactory hostFactory = integrationTestLauncher.getHostFactory();

        final ResourceInfo r1 = new ResourceInfo(
                null,
                null,
                null,
                null,
                null,
                null
        );
        r1.init("name", new LinkedHashSet<Host>(Arrays.asList(hostFactory.createInstance())), clusterBrowser);
        final ResourceInfo r2 = new ResourceInfo(
                null,
                null,
                null,
                null,
                null,
                null
        );
        r2.init("name", null, clusterBrowser);

        assertTrue("equal names", r1.equals(r2));
    }
}
