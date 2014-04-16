package lcmc.gui.widget;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import lcmc.utilities.TestSuite1;
import lcmc.data.Application;
import lcmc.data.Value;
import lcmc.data.StringValue;
import lcmc.data.AccessMode;

public final class WidgetTest1 extends TestCase {
    private Widget widget;
    @Before
    @Override
    protected void setUp() {
        TestSuite1.initTest();
        for (int i = 0; i < 10; i++) {
            widget = WidgetFactory.createInstance(
                          Widget.GUESS_TYPE,
                          Widget.NO_DEFAULT,
                          new Value[]{new StringValue("a"),
                                      new StringValue("b"),
                                      new StringValue("c")},
                          Widget.NO_REGEXP,
                          100, /* width */
                          Widget.NO_ABBRV,
                          new AccessMode(Application.AccessType.ADMIN,
                                         AccessMode.ADVANCED),
                          Widget.NO_BUTTON);
        }
    }

    @After
    @Override
    protected void tearDown() {
        assertEquals("", TestSuite1.getStdout());
    }

    @Test
    public void testReloadComboBox() {
        for (int i = 0; i < 3; i++) {
            widget.reloadComboBox(null, new Value[]{new StringValue("a"),
                                                    new StringValue("b")});

            widget.reloadComboBox(null, new Value[]{new StringValue("a"),
                                                    new StringValue("b"),
                                                    new StringValue("c")});

            widget.reloadComboBox(new StringValue("as"),
                                  new Value[]{new StringValue("a"),
                                              new StringValue("b"),
                                              new StringValue("c")});

            widget.reloadComboBox(null, new Value[]{new StringValue("a"),
                                                    new StringValue("b"),
                                                    new StringValue("c")});

            widget.reloadComboBox(null, new Value[]{new StringValue("a")});
        }
    }

    @Test
    public void testSetToolTipText() {
        widget.setToolTipText(null);
        widget.setToolTipText("");
        widget.setToolTipText("x");
    }

    @Test
    public void testSetEditable() {
        widget.setEditable(false);
        widget.setEditable(true);
        widget.setEditable();
    }

    @Test
    public void testGetStringValue() {
        widget.setValueAndWait(new StringValue("a"));
        assertEquals("a", widget.getStringValue());
    }

    @Test
    public void testGetValue() {
        widget.setValueAndWait(new StringValue("a"));
        assertEquals("a", widget.getValue().getValueForConfig());
    }

    @Test
    public void testClear() {
        widget.clear();
        widget.reloadComboBox(null, new Value[]{new StringValue("a"),
                                                new StringValue("b"),
                                                new StringValue("c")});
    }
}
