package lcmc.common.ui;

import lcmc.AppContext;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.testutils.IntegrationTestLauncher;
import lcmc.testutils.annotation.type.IntegrationTest;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public final class MainPanelITest {
    private IntegrationTestLauncher integrationTestLauncher;
    private MainPanel mainPanel;
    private SwingUtils swingUtils;

    @Before
    public void setUp() {
        integrationTestLauncher = IntegrationTestLauncher.create();
        integrationTestLauncher.initTestCluster();
        mainPanel = integrationTestLauncher.getMainPanel();
        swingUtils = integrationTestLauncher.getSwingUtils();
    }

    /** Tests, that the terminal area doesn't expand too much. */
    @Test
    public void testExpandTerminalSplitPane() {
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
            assertTrue("terminal area size error: " + (errors / count * 100) + "%", false);
        }
    }
}
