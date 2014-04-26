package lcmc.utilities;

import lcmc.data.Host;
import lcmc.testutils.TestUtils;
import lcmc.testutils.annotation.type.IntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public final class SSHITest {

    private final TestUtils testSuite = new TestUtils();

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
