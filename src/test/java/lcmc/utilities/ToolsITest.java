package lcmc.utilities;

import java.util.Map;
import javax.swing.JCheckBox;

import lcmc.gui.GUIData;
import lcmc.model.Host;
import lcmc.model.UserConfig;
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

    private final GUIData guiData = new GUIData();
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
        assertNull(Tools.loadFile(guiData, "JUNIT_TEST_FILE_CLICK_OK", false));
        final String testFile = "/tmp/lcmc-test-file";
        Tools.save(guiData, new UserConfig(), testFile, false);
        final String file = Tools.loadFile(guiData, testFile, false);
        assertNotNull(file);
        testSuite.clearStdout();
        assertFalse("".equals(file));
    }

    @Test
    @Category(GuiTest.class)
    public void testStartProgressIndicator() {
        testSuite.initMain();
        for (int i = 0; i < 10; i++) {
            guiData.startProgressIndicator(null);
            guiData.startProgressIndicator("test");
            guiData.startProgressIndicator("test2");
            guiData.startProgressIndicator("test3");
            guiData.startProgressIndicator(null, "test4");
            guiData.startProgressIndicator("name", "test4");
            guiData.startProgressIndicator("name2", "test4");
            guiData.startProgressIndicator("name2", null);
            guiData.startProgressIndicator(null, null);
            guiData.stopProgressIndicator(null, null);
            guiData.stopProgressIndicator("name2", null);
            guiData.stopProgressIndicator("name2", "test4");
            guiData.stopProgressIndicator("name", "test4");
            guiData.stopProgressIndicator(null, "test4");
            guiData.stopProgressIndicator("test3");
            guiData.stopProgressIndicator("test2");
            guiData.stopProgressIndicator("test");
            guiData.stopProgressIndicator(null);
        }
    }

    @Test
    @Category(GuiTest.class)
    public void testProgressIndicatorFailed() {
        testSuite.initMain();
        guiData.progressIndicatorFailed(null, "fail3");
        guiData.progressIndicatorFailed("name", "fail2");
        guiData.progressIndicatorFailed("name", null);
        guiData.progressIndicatorFailed("fail1");
        guiData.progressIndicatorFailed(null);

        guiData.progressIndicatorFailed("fail two seconds", 2);
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
