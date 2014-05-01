package lcmc.utilities;

import java.util.Map;
import javax.swing.JCheckBox;
import lcmc.data.Host;
import lcmc.testutils.TestUtils;
import lcmc.testutils.annotation.type.IntegrationTest;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public final class ToolsITest {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(ToolsITest.class);

    private final TestUtils testSuite = new TestUtils();
    @Before
    public void setUp() {
        testSuite.initStdout();
        testSuite.initTestCluster();
    }

    @Test
    public void testSSHError() {
        for (final Host host : testSuite.getHosts()) {
            LOG.sshError(host, "cmd a", "ans a", "stack trace a", 2);
            assertTrue(
                testSuite.getStdout().indexOf("returned exit code 2") >= 0);
            testSuite.clearStdout();
        }
    }

    @Test
    public void testExecCommandProgressIndicator() {
        for (int i = 0; i < 3; i++) {
            for (final Host host : testSuite.getHosts()) {
                Tools.execCommandProgressIndicator(host,
                                                   "uname -a",
                                                   null, /* ExecCallback */
                                                   true, /* outputVisible */
                                                   "text h",
                                                   1000); /* command timeout */
            }
        }
        testSuite.clearStdout();
    }

    @Test
    public void testIsIp() {
        for (final Host host : testSuite.getHosts()) {
            assertTrue(Tools.isIp(host.getIpAddress()));
            assertFalse(Tools.isIp(host.getHostname()));
        }
    }

    @Test
    public void testStartProgressIndicator() {
        for (int i = 0; i < 10; i++) {
            for (final Host host : testSuite.getHosts()) {
                Tools.startProgressIndicator(host.getName(), "test");
            }

            for (final Host host : testSuite.getHosts()) {
                Tools.stopProgressIndicator(host.getName(), "test");
            }
        }
    }

    @Test
    public void testProgressIndicatorFailed() {
        for (final Host host : testSuite.getHosts()) {
            Tools.progressIndicatorFailed(host.getName(), "fail");
        }
        testSuite.clearStdout();
    }

    @Test
    public void testIsLocalIp() {
        for (final Host host : testSuite.getHosts()) {
            assertFalse(Tools.isLocalIp(host.getIpAddress()));
        }
    }

    @Test
    public void testGetHostCheckBoxes() {
        for (final Host host : testSuite.getHosts()) {
            final Map<Host, JCheckBox> comps =
                                    Tools.getHostCheckBoxes(host.getCluster());
            assertNotNull(comps);
            assertTrue(comps.size() == testSuite.getHosts().size());
            assertTrue(comps.containsKey(host));
        }
    }

    @Test
    public void testVersionBeforePacemaker() {
        for (final Host h : testSuite.getHosts()) {
            Tools.versionBeforePacemaker(h);
        }
    }

    @Test
    public void helperFieShouldBeLoaded() {
        final String testFile = "/help-progs/lcmc-gui-helper";
        assertTrue(Tools.getFile(testFile).indexOf("#!") == 0);
    }

    @Test
    public void nullFileShouldReturnNull() {
        assertNull(Tools.getFile(null));
    }

    @Test
    public void nonExistingFileShouldReturnNull() {
        assertNull(Tools.getFile("not_existing_file"));
    }
}
