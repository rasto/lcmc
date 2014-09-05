package lcmc.gui;

import lcmc.AppContext;
import lcmc.model.Application;
import lcmc.model.Host;
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
        integrationTestLauncher = AppContext.getBean(IntegrationTestLauncher.class);
        integrationTestLauncher.initTestCluster();
        guiData = AppContext.getBean(GUIData.class);
        application = AppContext.getBean(Application.class);
    }

    /** Tests, that the terminal area doesn't expand too much. */
    @Test
    public void testExpandTerminalSplitPane() {
        float count = 200;
        float errors = 0;
        for (int i = 0; i < count; i++) {
            guiData.expandTerminalSplitPane(1);
            for (final Host host : integrationTestLauncher.getHosts()) {
                guiData.setTerminalPanel(host.getTerminalPanel());
                guiData.expandTerminalSplitPane(0);
            }
            application.waitForSwing();
            if (guiData.getTerminalPanelPos() < 100) {
                errors++;
            }
        }
        if (errors > 0) {
            assertTrue("terminal area size error: " + (errors / count * 100) + "%", false);
        }
    }
}
