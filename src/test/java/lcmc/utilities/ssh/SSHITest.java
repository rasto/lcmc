package lcmc.utilities.ssh;

import lcmc.model.Host;
import lcmc.testutils.TestUtils;
import lcmc.testutils.annotation.type.IntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Provider;
import java.io.IOException;

@Category(IntegrationTest.class)
public final class SSHITest {

    private final TestUtils testSuite = new TestUtils();

    @Before
    public void setUp() {
        testSuite.initTestCluster();
    }

    @Test
    public void testInstallTestFiles() throws IOException {
        for (final Host host : testSuite.getHosts()) {
            host.getSSH().installTestFiles();
        }
    }
}
