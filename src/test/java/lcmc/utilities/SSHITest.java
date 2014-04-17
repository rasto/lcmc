package lcmc.utilities;

import lcmc.data.Host;
import lcmc.testutils.TestSuite1;
import lcmc.testutils.annotation.type.IntegrationTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public final class SSHITest {

    private final TestSuite1 testSuite = new TestSuite1();

    @Before
    public void setUp() {
        testSuite.initTestCluster();
    }

    @Test
    public void testInstallTestFiles() {
        for (final Host host : testSuite.getHosts()) {
            host.getSSH().installTestFiles();
        }
    }
}
