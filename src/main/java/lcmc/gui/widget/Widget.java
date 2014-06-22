/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2013, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package lcmc.gui.widget;

import java.awt.Color;
import java.awt.Component;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.text.Document;
import lcmc.data.StringValue;
import lcmc.data.Value;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import lcmc.utilities.WidgetListener;

public interface Widget {
    enum Type {
        LABELFIELD,
        TEXTFIELD,
        PASSWDFIELD,
        COMBOBOX,
        RADIOGROUP,
        CHECKBOX,
        TEXTFIELDWITHUNIT
    }

    Type GUESS_TYPE = null;
    Value NO_DEFAULT = new StringValue(null);
    Value[] NO_ITEMS = null;
    String NO_REGEXP = null;
    Map<String, String> NO_ABBRV = null;
    MyButton NO_BUTTON = null;
    String WIZARD_PREFIX = "wizard";
    Color ERROR_VALUE_BACKGROUND = Tools.getDefaultColor("Widget.ErrorValue");
    Color CHANGED_VALUE_COLOR = Tools.getDefaultColor("Widget.ChangedValue");
    Color DEFAULT_VALUE_COLOR = Tools.getDefaultColor("Widget.DefaultValue");
    Color SAVED_VALUE_COLOR = Tools.getDefaultColor("Widget.SavedValue");
    int SCROLLBAR_MAX_ROWS = 10;
    int WIDGET_HEIGHT = 28;
    int WIDGET_COMPONENT_HEIGHT = 30;
    String NOTHING_SELECTED_DISPLAY = Tools.getString("Widget.NothingSelected");

    Component getComponent();

    void setEnabled(String s, boolean enabled);

    void setBackgroundColor(Color bg);

    String getStringValue();

    void setEnabled(boolean enabled);

    void addListeners(WidgetListener widgetListener);

    void setVisible(boolean visible);

    void setAlwaysEditable(boolean alwaysEditable);

    void setBackground(Value defaultValue, Value savedValue, boolean required);
    /**
     * Sets background of the component depending if the value is the same
     * as its default value and if it is a required argument.
     * Must be called after combo box was already added to some panel.
     *
     * It also disables, hides the component depending on the access type.
     * TODO: rename the function
     */
    void setBackground(String defaultLabel,
                       Value defaultValue,
                       String savedLabel,
                       Value savedValue,
                       boolean required);

    void wrongValue();

    void requestFocus();

    String getRegexp();

    Document getDocument();

    Value getValue();

    void setValue(Value item);

    void setToolTipText(String toolTip);

    void reloadComboBox(Value selectedValue, Value[] items);

    void setSelectedIndex(int index);
    /** Clears the combo box. */
    void clear();
    /** Sets the width of the widget. */
    void setWidth(int newWidth);
    void setValueAndWait(Value item);
    /** Sets combo box editable. */
    void setEditable(boolean editable);
    void setLabel(JLabel label, String labelToolTipText);
    /** Return whether this widget was never set. */
    boolean isNew();
    JLabel getLabel();
    /** Select the text component. */
    void select(int selectionStart, int selectionEnd);

    void selectAll();
    void setDisabledReason(String disabledReason);
    /** Sets this item enabled and visible according to its access type. */
    void processAccessMode();
    /** Selects part after first '*' in the ip. */
    void selectSubnet();
    /** Cleanup whatever would cause a leak. */
    void cleanup();
    /** Sets item/value in the component, disable listeners. */
    void setValueNoListeners(Value item);
    void setEditable();
    /** Sets extra button enabled. */
    void setTFButtonEnabled(boolean tfButtonEnabled);

    boolean isEditable();

    void setText(final String text);
}
