package lcmc.cluster.ui.widget;

import lcmc.common.domain.AccessMode;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public final class WidgetTest {
    private final WidgetFactory widgetFactory = new WidgetFactory(
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    private Widget widget;
    @Before
    public void setUp() {
        for (int i = 0; i < 10; i++) {
            widget = widgetFactory.createInstance(Widget.GUESS_TYPE,
                                                  Widget.NO_DEFAULT,
                                                  new Value[]{new StringValue("a"),
                                                              new StringValue("b"),
                                                              new StringValue("c")},
                                                  Widget.NO_REGEXP,
                                                  100, /* width */
                                                  Widget.NO_ABBRV,
                                                  new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED),
                                                  Widget.NO_BUTTON);
        }
    }

    @Test
    public void testReloadComboBox() {
        for (int i = 0; i < 3; i++) {
            widget.reloadComboBox(null, new Value[]{new StringValue("a"),
                                                    new StringValue("b")});

            widget.reloadComboBox(null, new Value[]{new StringValue("a"),
                                                    new StringValue("b"),
                                                    new StringValue("c")});

            widget.reloadComboBox(new StringValue("as"), new Value[]{new StringValue("a"),
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
