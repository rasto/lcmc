package lcmc.common.domain.util;

import java.util.Map;
import javax.swing.JCheckBox;

import lcmc.LCMC;
import lcmc.common.domain.UserConfig;
import lcmc.common.ui.main.MainPresenter;
import lcmc.common.ui.main.ProgressIndicator;
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
    private MainPresenter mainPresenter;
    private ProgressIndicator progressIndicator;
    private UserConfig userConfig;

    @Before
    public void setUp() {
        testSuite = LCMC.getInstance(IntegrationTestLauncher.class);
        testSuite.initTestCluster();
        userConfig = LCMC.getInstance(UserConfig.class);
        mainPresenter = LCMC.getInstance(MainPresenter.class);
        progressIndicator = LCMC.getInstance(ProgressIndicator.class);
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
        assertNull(Tools.loadFile(mainPresenter, "JUNIT_TEST_FILE_CLICK_OK", false));
        final String testFile = "/tmp/lcmc-test-file";
        userConfig.saveConfig(testFile, false);
        final String file = Tools.loadFile(mainPresenter, testFile, false);
        assertNotNull(file);
        assertFalse("".equals(file));
    }

    @Test
    @Category(GuiTest.class)
    public void testStartProgressIndicator() {
        for (int i = 0; i < 10; i++) {
            progressIndicator.startProgressIndicator(null);
            progressIndicator.startProgressIndicator("test");
            progressIndicator.startProgressIndicator("test2");
            progressIndicator.startProgressIndicator("test3");
            progressIndicator.startProgressIndicator(null, "test4");
            progressIndicator.startProgressIndicator("name", "test4");
            progressIndicator.startProgressIndicator("name2", "test4");
            progressIndicator.startProgressIndicator("name2", null);
            progressIndicator.startProgressIndicator(null, null);
            progressIndicator.stopProgressIndicator(null, null);
            progressIndicator.stopProgressIndicator("name2", null);
            progressIndicator.stopProgressIndicator("name2", "test4");
            progressIndicator.stopProgressIndicator("name", "test4");
            progressIndicator.stopProgressIndicator(null, "test4");
            progressIndicator.stopProgressIndicator("test3");
            progressIndicator.stopProgressIndicator("test2");
            progressIndicator.stopProgressIndicator("test");
            progressIndicator.stopProgressIndicator(null);
        }
    }

    @Test
    @Category(GuiTest.class)
    public void testProgressIndicatorFailed() {
        progressIndicator.progressIndicatorFailed(null, "fail3");
        progressIndicator.progressIndicatorFailed("name", "fail2");
        progressIndicator.progressIndicatorFailed("name", null);
        progressIndicator.progressIndicatorFailed("fail1");
        progressIndicator.progressIndicatorFailed(null);

        progressIndicator.progressIndicatorFailed("fail two seconds", 2);
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
