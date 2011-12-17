package lcmc.gui;

import junit.framework.TestCase;
import java.util.HashMap;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import lcmc.utilities.TestSuite1;
import lcmc.utilities.Tools;
import lcmc.data.Host;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;

public final class GuiComboBoxTest1 extends TestCase {
    private GuiComboBox comboBox;
    @Before
    protected void setUp() {
        TestSuite1.initTest();
        for (int i = 0; i < 10 * TestSuite1.getFactor(); i++) {
            comboBox = new GuiComboBox(
                          null,
                          new String[]{"a", "b", "c"},
                          null, /* units */
                          null, /* type */
                          null, /* regexp */
                          100, /* width */
                          new HashMap<String, String>(), /* abrv */
                          new AccessMode(ConfigData.AccessType.ADMIN, true));
        }
    }

    @After
    protected void tearDown() {
        assertEquals("", TestSuite1.getStdout());
    }

    /* ---- tests ----- */
    @Test
    public void testIsCheckBox() {
        assertFalse(comboBox.isCheckBox());
    }

    @Test
    public void testReloadComboBox() {
        for (int i = 0; i < TestSuite1.getFactor(); i++) {
            comboBox.reloadComboBox(null, new Object[]{"a", "b"});
            comboBox.reloadComboBox(null, new Object[]{"a", "b", "c"});
            comboBox.reloadComboBox("as", new Object[]{"a", "b", "c"});
            comboBox.reloadComboBox(null, new Object[]{"a", "b", "c"});
            comboBox.reloadComboBox(null, new Object[]{"a"});
        }
    }

    @Test
    public void testSetEnabled() {
        for (int i = 0; i < TestSuite1.getFactor(); i++) {
            comboBox.setEnabled(null, false);
            comboBox.setEnabled(null, true);
            comboBox.setEnabled("a", false);
            comboBox.setEnabled("a", true);
        }
    }

    @Test
    public void testSetVisible() {
        for (int i = 0; i < TestSuite1.getFactor(); i++) {
            comboBox.setEnabled(null, false);
            comboBox.setEnabled(null, true);
            comboBox.setEnabled("a", false);
            comboBox.setEnabled("a", true);
        }
    }

    @Test
    public void testSetToolTipText() {
        for (int i = 0; i < TestSuite1.getFactor(); i++) {
            comboBox.setToolTipText(null);
            comboBox.setToolTipText("");
            comboBox.setToolTipText("x");
        }
    }

    @Test
    public void testSetLabelToolTipText() {
        for (int i = 0; i < TestSuite1.getFactor(); i++) {
            comboBox.setLabelToolTipText(null);
            comboBox.setLabelToolTipText("x");
        }
    }

    @Test
    public void testSetEditable() {
        comboBox.setEditable(false);
        comboBox.setEditable(true);
        comboBox.setEditable();
    }

    @Test
    public void testGetStringValue() {
        assertEquals("a", comboBox.getStringValue());
    }

    @Test
    public void testGetValue() {
        assertEquals("a", comboBox.getValue());
    }

    @Test
    public void testClear() {
        for (int i = 0; i < TestSuite1.getFactor(); i++) {
            comboBox.clear();
            comboBox.reloadComboBox(null, new Object[]{"a", "b", "c"});
        }
    }
}
