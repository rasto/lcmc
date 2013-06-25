package lcmc.gui;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import lcmc.utilities.TestSuite1;
import lcmc.utilities.Tools;
import lcmc.data.Host;

public final class GUIDataTest1 extends TestCase {
    @Before
    @Override
    protected void setUp() {
        TestSuite1.initTest();
    }

    @After
    @Override
    protected void tearDown() {
        assertEquals("", TestSuite1.getStdout());
    }

    /* ---- tests ----- */

    /** Tests, that the terminal area doesn't expand too much. */
    @Test
    public void testExpandTerminalSplitPane() {
        float count = 50 * TestSuite1.getFactor();
        if (TestSuite1.QUICK) {
            count = 3 * TestSuite1.getFactor();
        }
        float errors = 0;
        for (int i = 0; i < count; i++) {
            Tools.getGUIData().expandTerminalSplitPane(1);
            for (final Host host : TestSuite1.getHosts()) {
                Tools.getGUIData().setTerminalPanel(host.getTerminalPanel());
                Tools.getGUIData().expandTerminalSplitPane(0);
            }
            Tools.waitForSwing();
            if (Tools.getGUIData().getTerminalPanelPos() < 100) {
                errors++;
            }
        }
        if (errors > 0) {
            assertTrue("terminal area size error: " + (errors / count * 100)
                       + "%",
                       false);
        }
    }
}
