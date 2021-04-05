package lcmc.common.ui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lcmc.AppContext;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.testutils.IntegrationTestLauncher;
import lcmc.testutils.annotation.type.IntegrationTest;

@Category(IntegrationTest.class)
final class MainPanelITest {
    private IntegrationTestLauncher integrationTestLauncher;
    private MainPanel mainPanel;
    private SwingUtils swingUtils;

    @BeforeEach
    void setUp() {
        integrationTestLauncher = AppContext.getBean(IntegrationTestLauncher.class);
        integrationTestLauncher.initTestCluster();
        mainPanel = AppContext.getBean(MainPanel.class);
        swingUtils = AppContext.getBean(SwingUtils.class);
    }

    /**
     * Tests, that the terminal area doesn't expand too much.
     */
    @Test
    void testExpandTerminalSplitPane() {
        float count = 200;
        float errors = 0;
        for (int i = 0; i < count; i++) {
            mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.COLLAPSE);
            for (final Host host : integrationTestLauncher.getHosts()) {
                mainPanel.setTerminalPanel(host.getTerminalPanel());
                mainPanel.expandTerminalSplitPane(MainPanel.TerminalSize.EXPAND);
            }
            swingUtils.waitForSwing();
            if (i > 0 && mainPanel.getTerminalPanelPos() < 100) {
                errors++;
            }
        }
        if (errors > 0) {
            assertThat(false).describedAs("terminal area size error: " + (errors / count * 100) + "%").isTrue();
        }
    }
}
