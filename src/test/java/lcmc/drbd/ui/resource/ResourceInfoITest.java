package lcmc.drbd.ui.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import lcmc.AppContext;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.host.domain.HostFactory;
import lcmc.testutils.IntegrationTestLauncher;

@Tag("IntegrationTest")
final class ResourceInfoITest {
    private IntegrationTestLauncher integrationTestLauncher;

    @BeforeEach
    void setUp() {
        integrationTestLauncher = AppContext.getBean(IntegrationTestLauncher.class);
        integrationTestLauncher.initTestCluster();
    }

    @Test
    void testNotEqualNames() {
        final ClusterBrowser clusterBrowser = integrationTestLauncher.getHosts()
                                                                     .get(0)
                                                                     .getBrowser()
                                                                     .getClusterBrowser();
        final ResourceInfo r1 = createResourceInfo();
        r1.init("name1", null, clusterBrowser);
        final ResourceInfo r2 = createResourceInfo();
        r2.init("name2", null, clusterBrowser);

        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void testEqualNames() {
        final ClusterBrowser clusterBrowser = integrationTestLauncher.getHosts()
                                                                     .get(0)
                                                                     .getBrowser()
                                                                     .getClusterBrowser();
        final ResourceInfo r1 = createResourceInfo();
        r1.init("name", null, clusterBrowser);
        final ResourceInfo r2 = createResourceInfo();
        r2.init("name", null, clusterBrowser);

        assertThat(r1).isEqualTo(r2);
    }

    @Test
    void testNameNull() {
        final ClusterBrowser clusterBrowser = integrationTestLauncher.getHosts()
                                                                     .get(0)
                                                                     .getBrowser()
                                                                     .getClusterBrowser();
        final ResourceInfo r1 = createResourceInfo();
        r1.init("name", null, clusterBrowser);

        assertThat(r1.getName()).isNotNull();
    }

    @Test
    void testEqualNamesNotEqualsHosts() {
        final ClusterBrowser clusterBrowser = integrationTestLauncher.getHosts()
                                                                     .get(0)
                                                                     .getBrowser()
                                                                     .getClusterBrowser();
        final HostFactory hostFactory = AppContext.getBean(HostFactory.class);

        final ResourceInfo r1 = createResourceInfo();
        r1.init("name", new LinkedHashSet<>(List.of(hostFactory.createInstance())), clusterBrowser);
        final ResourceInfo r2 = createResourceInfo();
        r2.init("name", null, clusterBrowser);

        assertThat(r1).isEqualTo(r2);
    }

    private ResourceInfo createResourceInfo() {
        return new ResourceInfo(null, null, null, null, null, null, null, null, null, null);
    }
}
