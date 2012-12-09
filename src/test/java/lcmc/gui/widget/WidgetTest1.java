package lcmc.gui.widget;

import junit.framework.TestCase;
import java.util.HashMap;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import lcmc.utilities.TestSuite1;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;

public final class WidgetTest1 extends TestCase {
    private Widget widget;
    @Before
    protected void setUp() {
        TestSuite1.initTest();
        for (int i = 0; i < 10 * TestSuite1.getFactor(); i++) {
            widget = WidgetFactory.createInstance(
                          Widget.GUESS_TYPE,
                          Widget.NO_DEFAULT,
                          new String[]{"a", "b", "c"},
                          Widget.NO_REGEXP,
                          100, /* width */
                          Widget.NO_ABBRV,
                          new AccessMode(ConfigData.AccessType.ADMIN,
                                         AccessMode.ADVANCED),
                          Widget.NO_BUTTON);
        }
    }

    @After
    protected void tearDown() {
        assertEquals("", TestSuite1.getStdout());
    }

    @Test
    public void testReloadComboBox() {
        for (int i = 0; i < TestSuite1.getFactor(); i++) {
            widget.reloadComboBox(null, new Object[]{"a", "b"});
            widget.reloadComboBox(null, new Object[]{"a", "b", "c"});
            widget.reloadComboBox("as", new Object[]{"a", "b", "c"});
            widget.reloadComboBox(null, new Object[]{"a", "b", "c"});
            widget.reloadComboBox(null, new Object[]{"a"});
        }
    }

    @Test
    public void testSetEnabled() {
        for (int i = 0; i < TestSuite1.getFactor(); i++) {
            //widget.setEnabled(null, false);
            //widget.setEnabled(null, true);
            //widget.setEnabled("a", false);
            //widget.setEnabled("a", true);
        }
    }

    @Test
    public void testSetVisible() {
        for (int i = 0; i < TestSuite1.getFactor(); i++) {
            //widget.setEnabled(null, false);
            //widget.setEnabled(null, true);
            //widget.setEnabled("a", false);
            //widget.setEnabled("a", true);
        }
    }

    @Test
    public void testSetToolTipText() {
        for (int i = 0; i < TestSuite1.getFactor(); i++) {
            widget.setToolTipText(null);
            widget.setToolTipText("");
            widget.setToolTipText("x");
        }
    }

    @Test
    public void testSetLabelToolTipText() {
        //for (int i = 0; i < TestSuite1.getFactor(); i++) {
        //    widget.setLabelToolTipText(null);
        //    widget.setLabelToolTipText("x");
        //}
    }

    @Test
    public void testSetEditable() {
        widget.setEditable(false);
        widget.setEditable(true);
        widget.setEditable();
    }

    @Test
    public void testGetStringValue() {
        assertEquals("a", widget.getStringValue());
    }

    @Test
    public void testGetValue() {
        assertEquals("a", widget.getValue());
    }

    @Test
    public void testClear() {
        for (int i = 0; i < TestSuite1.getFactor(); i++) {
            widget.clear();
            widget.reloadComboBox(null, new Object[]{"a", "b", "c"});
        }
    }
}
