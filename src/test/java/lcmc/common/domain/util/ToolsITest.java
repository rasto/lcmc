package lcmc.common.domain.util;

import java.util.Map;
import javax.swing.JCheckBox;

import lcmc.LCMC;
import lcmc.common.ui.GUIData;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.testutils.IntegrationTestLauncher;
import lcmc.testutils.annotation.type.GuiTest;
import lcmc.testutils.annotation.type.IntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Jsr330ScopeMetadataResolver;

import static org.junit.Assert.*;

@Category(IntegrationTest.class)
public final class ToolsITest {
    private IntegrationTestLauncher testSuite;
    private GUIData guiData;
    private Application application;

    @Before
    public void setUp() {
        testSuite = LCMC.getInstance(IntegrationTestLauncher.class);
        testSuite.initTestCluster();
        application = LCMC.getInstance(Application.class);
        guiData = LCMC.getInstance(GUIData.class);
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
        assertNull(Tools.loadFile(guiData, "JUNIT_TEST_FILE_CLICK_OK", false));
        final String testFile = "/tmp/lcmc-test-file";
        application.saveConfig(testFile, false);
        final String file = Tools.loadFile(guiData, testFile, false);
        assertNotNull(file);
        assertFalse("".equals(file));
    }

    @Test
    @Category(GuiTest.class)
    public void testStartProgressIndicator() {
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
        guiData.progressIndicatorFailed(null, "fail3");
        guiData.progressIndicatorFailed("name", "fail2");
        guiData.progressIndicatorFailed("name", null);
        guiData.progressIndicatorFailed("fail1");
        guiData.progressIndicatorFailed(null);

        guiData.progressIndicatorFailed("fail two seconds", 2);
    }

    @Test
    @Category(GuiTest.class)
    public void testIsLinux() {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            assertFalse(Tools.isLinux());
        }
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            assertTrue(Tools.isLinux());
        }
    }

    @Test
    @Category(GuiTest.class)
    public void testIsWindows() {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            assertTrue(Tools.isWindows());
        }
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            assertFalse(Tools.isWindows());
        }
    }

    @Test
    @Category(GuiTest.class)
    public void testGetUnixPath() {
        assertEquals("/bin", Tools.getUnixPath("/bin"));
        if (Tools.isWindows()) {
            assertEquals("/bin/dir/file", Tools.getUnixPath("d:\\bin\\dir\\file"));
        }
    }

    @Configuration
    @ComponentScan(basePackages = "lcmc", scopeResolver = Jsr330ScopeMetadataResolver.class)
    static class TestConfig {
    }
}
