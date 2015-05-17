package lcmc.common.ui;

import lcmc.LCMC;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.testutils.IntegrationTestLauncher;
import lcmc.testutils.annotation.type.IntegrationTest;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public final class GUIDataITest {
    private IntegrationTestLauncher integrationTestLauncher;
    private GUIData guiData;
    private Application application;

    @Before
    public void setUp() {
        integrationTestLauncher = LCMC.getInstance(IntegrationTestLauncher.class);
        integrationTestLauncher.initTestCluster();
        guiData = LCMC.getInstance(GUIData.class);
        application = LCMC.getInstance(Application.class);
    }

    /** Tests, that the terminal area doesn't expand too much. */
    @Test
    public void testExpandTerminalSplitPane() {
        float count = 200;
        float errors = 0;
        for (int i = 0; i < count; i++) {
            guiData.expandTerminalSplitPane(GUIData.TerminalSize.COLLAPSE);
            for (final Host host : integrationTestLauncher.getHosts()) {
                guiData.setTerminalPanel(host.getTerminalPanel());
                guiData.expandTerminalSplitPane(GUIData.TerminalSize.EXPAND);
            }
            application.waitForSwing();
            if (i > 0 && guiData.getTerminalPanelPos() < 100) {
                errors++;
            }
        }
        if (errors > 0) {
            assertTrue("terminal area size error: " + (errors / count * 100) + "%", false);
        }
    }
}
