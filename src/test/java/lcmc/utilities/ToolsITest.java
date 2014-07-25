package lcmc.utilities;

import java.util.Map;
import javax.swing.JCheckBox;
import lcmc.model.Host;
import lcmc.testutils.TestUtils;
import lcmc.testutils.annotation.type.GuiTest;
import lcmc.testutils.annotation.type.IntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

@Category(IntegrationTest.class)
public final class ToolsITest {
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
            assertTrue(testSuite.getStdout().indexOf("returned exit code 2") >= 0);
            testSuite.clearStdout();
        }
    }

    @Test
    public void testIsIp() {
        for (final Host host : testSuite.getHosts()) {
            assertTrue(Tools.isIp(host.getIpAddress()));
            assertFalse(Tools.isIp(host.getHostname()));
        }
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
            final Map<Host, JCheckBox> comps = Tools.getHostCheckBoxes(host.getCluster());
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

    @Test
    @Category(GuiTest.class)
    public void testLoadFile() {
        testSuite.initMain();
        assertNull(Tools.loadFile("JUNIT_TEST_FILE_CLICK_OK", false));
        final String testFile = "/tmp/lcmc-test-file";
        Tools.save(testFile, false);
        final String file = Tools.loadFile(testFile, false);
        assertNotNull(file);
        testSuite.clearStdout();
        assertFalse("".equals(file));
    }

    @Test
    @Category(GuiTest.class)
    public void testStartProgressIndicator() {
        testSuite.initMain();
        for (int i = 0; i < 10; i++) {
            Tools.startProgressIndicator(null);
            Tools.startProgressIndicator("test");
            Tools.startProgressIndicator("test2");
            Tools.startProgressIndicator("test3");
            Tools.startProgressIndicator(null, "test4");
            Tools.startProgressIndicator("name", "test4");
            Tools.startProgressIndicator("name2", "test4");
            Tools.startProgressIndicator("name2", null);
            Tools.startProgressIndicator(null, null);
            Tools.stopProgressIndicator(null, null);
            Tools.stopProgressIndicator("name2", null);
            Tools.stopProgressIndicator("name2", "test4");
            Tools.stopProgressIndicator("name", "test4");
            Tools.stopProgressIndicator(null, "test4");
            Tools.stopProgressIndicator("test3");
            Tools.stopProgressIndicator("test2");
            Tools.stopProgressIndicator("test");
            Tools.stopProgressIndicator(null);
        }
    }

    @Test
    @Category(GuiTest.class)
    public void testProgressIndicatorFailed() {
        testSuite.initMain();
        Tools.progressIndicatorFailed(null, "fail3");
        Tools.progressIndicatorFailed("name", "fail2");
        Tools.progressIndicatorFailed("name", null);
        Tools.progressIndicatorFailed("fail1");
        Tools.progressIndicatorFailed(null);

        Tools.progressIndicatorFailed("fail two seconds", 2);
        testSuite.clearStdout();
    }

    @Test
    @Category(GuiTest.class)
    public void testIsLinux() {
        testSuite.initMain();
        if (System.getProperty("os.name").indexOf("Windows") >= 0
            || System.getProperty("os.name").indexOf("windows") >= 0) {
            assertFalse(Tools.isLinux());
        }
        if (System.getProperty("os.name").indexOf("Linux") >= 0
            || System.getProperty("os.name").indexOf("linux") >= 0) {
            assertTrue(Tools.isLinux());
        }
    }

    @Test
    @Category(GuiTest.class)
    public void testIsWindows() {
        testSuite.initMain();
        if (System.getProperty("os.name").indexOf("Windows") >= 0
            || System.getProperty("os.name").indexOf("windows") >= 0) {
            assertTrue(Tools.isWindows());
        }
        if (System.getProperty("os.name").indexOf("Linux") >= 0
            || System.getProperty("os.name").indexOf("linux") >= 0) {
            assertFalse(Tools.isWindows());
        }
    }

    @Test
    @Category(GuiTest.class)
    public void testGetUnixPath() {
        testSuite.initMain();
        assertEquals("/bin", Tools.getUnixPath("/bin"));
        if (Tools.isWindows()) {
            assertEquals("/bin/dir/file", Tools.getUnixPath("d:\\bin\\dir\\file"));
        }
    }
}
