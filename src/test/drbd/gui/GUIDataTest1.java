package drbd.gui;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import drbd.TestSuite1;
import drbd.utilities.Tools;
import drbd.data.Host;

public final class GUIDataTest1 extends TestCase {
    @Before
    protected void setUp() {
        TestSuite1.initTest();
    }

    @After
    protected void tearDown() {
        assertEquals("", TestSuite1.getStdout());
    }

    /* ---- tests ----- */

    /** Tests, that the terminal area doesn't expand too much. */
    @Test
    public void testExpandTerminalSplitPane() {
        final float count = 50 * TestSuite1.getFactor();
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
