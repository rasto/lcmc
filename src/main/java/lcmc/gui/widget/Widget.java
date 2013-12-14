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

/**
 */
public interface Widget {
    /** Widget type. */
    public enum Type { LABELFIELD, TEXTFIELD, PASSWDFIELD, COMBOBOX,
                       RADIOGROUP, CHECKBOX, TEXTFIELDWITHUNIT };

    public static final Type GUESS_TYPE = null;
    public static final Value NO_DEFAULT = new StringValue(null);
    public static final Value[] NO_ITEMS = null;
    public static final String NO_REGEXP = null;
    public static final Map<String, String> NO_ABBRV = null;
    public static final MyButton NO_BUTTON = null;
    public static final String WIZARD_PREFIX = "wizard";
    /** Background of the field if the value is wrong. */
    public static final Color ERROR_VALUE_BACKGROUND =
                            Tools.getDefaultColor("Widget.ErrorValue");
    /** Background of the field if the value has changed. */
    public static final Color CHANGED_VALUE_COLOR =
                            Tools.getDefaultColor("Widget.ChangedValue");
    /** Background of the field if the value is default. */
    public static final Color DEFAULT_VALUE_COLOR =
                            Tools.getDefaultColor("Widget.DefaultValue");
    /** Background of the field if the value is saved. */
    public static final Color SAVED_VALUE_COLOR =
                            Tools.getDefaultColor("Widget.SavedValue");
    /** No scrollbar ever. */
    public static final int SCROLLBAR_MAX_ROWS = 10;
    /** Widget default height. */
    public static final int WIDGET_HEIGHT = 28;
    /** Widget enclosing component default height. */
    public static final int WIDGET_COMPONENT_HEIGHT = 30;
    /** Nothing selected string, that returns null, if selected. */
    public static final String NOTHING_SELECTED_DISPLAY =
                                Tools.getString("Widget.NothingSelected");
    /** Nothing selected string, that returns null, if selected. */
    public static final Value NOTHING_SELECTED_INTERNAL = null;
    /** Returns this widget, so that the interface Widget can be used in other
     *  components. */
    public Component getComponent();

    public void setEnabled(String s, boolean enabled);

    public void setBackgroundColor(Color bg);

    public String getStringValue();

    public void setEnabled(boolean enabled);

    public void addListeners(WidgetListener widgetListener);

    public void setVisible(boolean visible);

    public void setAlwaysEditable(boolean alwaysEditable);

    public void setBackground(Value defaultValue,
                              Value savedValue,
                              boolean required);
    /**
     * Sets background of the component depending if the value is the same
     * as its default value and if it is a required argument.
     * Must be called after combo box was already added to some panel.
     *
     * It also disables, hides the component depending on the access type.
     * TODO: rename the function
     */
    public void setBackground(String defaultLabel,
                              Value defaultValue,
                              String savedLabel,
                              Value savedValue,
                              boolean required);

    public void wrongValue();

    public void requestFocus();

    public String getRegexp();
    /** Returns document object of the component. */
    public Document getDocument();

    public Value getValue();

    public void setValue(Value item);

    public void setToolTipText(String toolTip);

    public void reloadComboBox(Value selectedValue, Value[] items);

    public void setSelectedIndex(int index);
    /** Clears the combo box. */
    public void clear();
    /** Sets the width of the widget. */
    public void setWidth(int newWidth);
    /** Sets item/value in the component and waits till it is set. */
    public void setValueAndWait(Value item);
    /** Sets combo box editable. */
    public void setEditable(boolean editable);
    /** Sets label for this component. */
    public void setLabel(JLabel label, String labelToolTipText);
    /** Return whether this widget was never set. */
    public boolean isNew();
    /** Returns label for this component. */
    public JLabel getLabel();
    /** Select the text component. */
    public void select(int selectionStart, int selectionEnd);

    public void selectAll();
    /** Sets reason why it is disabled. */
    public void setDisabledReason(String disabledReason);
    /** Sets this item enabled and visible according to its access type. */
    public void processAccessMode();
    /** Selects part after first '*' in the ip. */
    public void selectSubnet();
    /** Cleanup whatever would cause a leak. */
    public void cleanup();
    /** Sets item/value in the component, disable listeners. */
    public void setValueNoListeners(Value item);
    /** Sets the field editable. */
    public void setEditable();
    /** Sets extra button enabled. */
    public void setTFButtonEnabled(boolean tfButtonEnabled);

    public boolean isEditable();

    public void setText(final String text);
}
