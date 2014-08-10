package lcmc.gui;

import lcmc.model.Host;
import lcmc.testutils.TestUtils;
import lcmc.testutils.annotation.type.IntegrationTest;
import lcmc.utilities.Tools;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Category(IntegrationTest.class)
@Component
public final class GUIDataITest {

    private final TestUtils testSuite = new TestUtils();
    @Autowired
    private GUIData guiData;

    @Before
    public void setUp() {
        testSuite.initTestCluster();
    }

    /** Tests, that the terminal area doesn't expand too much. */
    @Test
    public void testExpandTerminalSplitPane() {
        float count = 200;
        float errors = 0;
        for (int i = 0; i < count; i++) {
            guiData.expandTerminalSplitPane(1);
            for (final Host host : testSuite.getHosts()) {
                guiData.setTerminalPanel(host.getTerminalPanel());
                guiData.expandTerminalSplitPane(0);
            }
            Tools.waitForSwing();
            if (guiData.getTerminalPanelPos() < 100) {
                errors++;
            }
        }
        if (errors > 0) {
            assertTrue("terminal area size error: " + (errors / count * 100) + "%", false);
        }
    }
}
