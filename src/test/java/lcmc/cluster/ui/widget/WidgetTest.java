package lcmc.cluster.ui.widget;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lcmc.AppContext;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;

final class WidgetTest {
    private final WidgetFactory widgetFactory = AppContext.getBean(WidgetFactory.class);

    private Widget widget;

    @BeforeEach
    public void setUp() {
        for (int i = 0; i < 10; i++) {
            widget = widgetFactory.createInstance(Widget.GUESS_TYPE, Widget.NO_DEFAULT,
                    new Value[]{new StringValue("a"), new StringValue("b"),
                                                              new StringValue("c")},
                                                  Widget.NO_REGEXP,
                                                  100, /* width */
                                                  Widget.NO_ABBRV,
                                                  new AccessMode(AccessMode.ADMIN, AccessMode.ADVANCED),
                                                  Widget.NO_BUTTON);
        }
    }

    @Test
    void testReloadComboBox() {
        for (int i = 0; i < 3; i++) {
            widget.reloadComboBox(null, new Value[]{new StringValue("a"), new StringValue("b")});

            widget.reloadComboBox(null, new Value[]{new StringValue("a"), new StringValue("b"), new StringValue("c")});

            widget.reloadComboBox(new StringValue("as"), new Value[]{new StringValue("a"), new StringValue("b"),
                                                                     new StringValue("c")});

            widget.reloadComboBox(null, new Value[]{new StringValue("a"),
                                                    new StringValue("b"),
                                                    new StringValue("c")});

            widget.reloadComboBox(null, new Value[]{new StringValue("a")});
        }
    }

    @Test
    void testSetToolTipText() {
        widget.setToolTipText(null);
        widget.setToolTipText("");
        widget.setToolTipText("x");
    }

    @Test
    void testSetEditable() {
        widget.setEditable(false);
        widget.setEditable(true);
        widget.setEditable();
    }

    @Test
    void testGetStringValue() {
        widget.setValueAndWait(new StringValue("a"));
        assertThat(widget.getStringValue()).isEqualTo("a");
    }

    @Test
    void testGetValue() {
        widget.setValueAndWait(new StringValue("a"));
        assertThat(widget.getValue().getValueForConfig()).isEqualTo("a");
    }

    @Test
    void testClear() {
        widget.clear();
        widget.reloadComboBox(null, new Value[]{new StringValue("a"), new StringValue("b"), new StringValue("c")});
    }
}
