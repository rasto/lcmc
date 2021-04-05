package lcmc.common.domain.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import javax.swing.JCheckBox;

import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Jsr330ScopeMetadataResolver;

import lcmc.AppContext;
import lcmc.common.domain.UserConfig;
import lcmc.common.ui.main.MainPresenter;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.host.domain.Host;
import lcmc.testutils.IntegrationTestLauncher;
import lcmc.testutils.annotation.type.GuiTest;
import lcmc.testutils.annotation.type.IntegrationTest;

@Category(IntegrationTest.class)
final class ToolsITest {
    private IntegrationTestLauncher testSuite;
    private MainPresenter mainPresenter;
    private ProgressIndicator progressIndicator;
    private UserConfig userConfig;

    @BeforeEach
    void setUp() {
        testSuite = AppContext.getBean(IntegrationTestLauncher.class);
        testSuite.initTestCluster();
        userConfig = AppContext.getBean(UserConfig.class);
        mainPresenter = AppContext.getBean(MainPresenter.class);
        progressIndicator = AppContext.getBean(ProgressIndicator.class);
    }

    @Test
    void testIsIp() {
        for (final Host host : testSuite.getHosts()) {
            assertThat(Tools.isIp(host.getIpAddress())).isTrue();
            assertThat(Tools.isIp(host.getHostname())).isFalse();
        }
    }

    @Test
    void testIsLocalIp() {
        for (final Host host : testSuite.getHosts()) {
            assertThat(Tools.isLocalIp(host.getIpAddress())).isFalse();
        }
    }

    @Test
    void testGetHostCheckBoxes() {
        for (final Host host : testSuite.getHosts()) {
            final Map<Host, JCheckBox> comps = Tools.getHostCheckBoxes(host.getCluster());
            assertThat(comps).isNotNull().hasSize(testSuite.getHosts().size()).containsKey(host);
        }
    }

    @Test
    void testVersionBeforePacemaker() {
        for (final Host h : testSuite.getHosts()) {
            Tools.versionBeforePacemaker(h);
        }
    }

    @Test
    void helperFieShouldBeLoaded() {
        final String testFile = "/help-progs/lcmc-gui-helper/Main.pl";
        assertThat(Tools.readFile(testFile).indexOf("#!")).isEqualTo(0);
    }

    @Test
    void nullFileShouldReturnNull() {
        assertThat(Tools.readFile(null)).isNull();
    }

    @Test
    void nonExistingFileShouldReturnNull() {
        assertThat(Tools.readFile("not_existing_file")).isNull();
    }

    @Test
    @Category(GuiTest.class)
    void testLoadFile() {
        assertThat(Tools.loadFile(mainPresenter, "JUNIT_TEST_FILE_CLICK_OK", false)).isNull();
        final String testFile = "/tmp/lcmc-test-file";
        userConfig.saveConfig(testFile, false);
        final String file = Tools.loadFile(mainPresenter, testFile, false);
        assertThat(file).isNotNull();
        assertThat(file).isNotEqualTo("");
    }

    @Test
    @Category(GuiTest.class)
    void testStartProgressIndicator() {
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
    void testProgressIndicatorFailed() {
        progressIndicator.progressIndicatorFailed(null, "fail3");
        progressIndicator.progressIndicatorFailed("name", "fail2");
        progressIndicator.progressIndicatorFailed("name", null);
        progressIndicator.progressIndicatorFailed("fail1");
        progressIndicator.progressIndicatorFailed(null);

        progressIndicator.progressIndicatorFailed("fail two seconds", 2);
    }

    @Test
    @Category(GuiTest.class)
    void testIsLinux() {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            assertThat(Tools.isLinux()).isFalse();
        }
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            assertThat(Tools.isLinux()).isTrue();
        }
    }

    @Test
    @Category(GuiTest.class)
    void testIsWindows() {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            assertThat(Tools.isWindows()).isTrue();
        }
        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            assertThat(Tools.isWindows()).isFalse();
        }
    }

    @Test
    @Category(GuiTest.class)
    void testGetUnixPath() {
        assertThat(Tools.getUnixPath("/bin")).isEqualTo("/bin");
        if (Tools.isWindows()) {
            assertThat(Tools.getUnixPath("d:\\bin\\dir\\file")).isEqualTo("/bin/dir/file");
        }
    }

    @Configuration
    @ComponentScan(basePackages = "lcmc", scopeResolver = Jsr330ScopeMetadataResolver.class)
    static class TestConfig {
    }
}
