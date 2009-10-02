/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */


package drbd.gui;

import drbd.utilities.Tools;
import drbd.utilities.Unit;
import drbd.utilities.PatternDocument;
import drbd.gui.Browser.Info;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.text.JTextComponent;
import javax.swing.text.Document;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.ComboBoxEditor;

import java.awt.BorderLayout;

import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.Component;
import javax.swing.SpringLayout;

import javax.swing.event.DocumentListener;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * An implementation of a field where user can enter new value. The
 * field can be Textfield or combo box, depending if there are values
 * too choose from.
 * TODO: rename it to GuiWidget or something
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class GuiComboBox extends JPanel {

    /** Widget type. */
    public enum Type { TEXTFIELD, PASSWDFIELD, COMBOBOX, RADIOGROUP, CHECKBOX,
                       TEXTFIELDWITHUNIT };

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Component of this widget. */
    private JComponent component;
    /** Type of the widget. */
    private Type type;
    /** Whether the field is editable. */
    private boolean editable = false;
    /** Value of the radio group. */
    private String radioGroupValue;
    /** Whether the field should be always editable. */
    private boolean alwaysEditable = false;
    /** Array of unit objects. */
    private Unit[] units = null;
    /** Text field in widget with units. */
    private JTextField textFieldWithoutUnit = null;
    /** Combo box with units. */
    private JComboBox unitComboBox = null;
    /** Pattern that matches value and unit. */
    private final Pattern unitPattern = Pattern.compile("^(\\d+)(\\D*)$");
    /** Name for the 'true' value. */
    private String checkBoxTrue = Tools.getString("Boolean.True");
    /** Name for the 'false' value. */
    private String checkBoxFalse = Tools.getString("Boolean.False");
    /** Background of the field if the value is wrong. */
    private static final Color ERROR_VALUE_BACKGROUND =
                            Tools.getDefaultColor("GuiComboBox.ErrorValue");
    /** No scrollbar ever. */
    private static final int SCROLLBAR_MAX_ROWS = 10000;
    /** Widget default height. */
    private static final int WIDGET_HEIGHT = 28;
    /** Widget enclosing component default height. */
    private static final int WIDGET_COMPONENT_HEIGHT = 30;
    /** Name to component hash. */
    private final Map<String, JComponent> componentsHash =
                                             new HashMap<String, JComponent>();
    /** Nothing selected string, that returns null, if selected. */
    public static final String NOTHING_SELECTED =
                                Tools.getString("GuiComboBox.NothingSelected");
    /**
     * Prepares a new <code>GuiComboBox</code> object with specified units.
     */
    public GuiComboBox(final String selectedValue,
                       final Object[] items,
                       final Unit[] units,
                       final Type type,
                       String regexp,
                       final int width,
                       final Map<String, String> abbreviations) {
        super();
        this.units = units;
        //setLayout(new FlowLayout(FlowLayout.CENTER, 1, 1));
        setLayout(new BorderLayout(0, 0));
        if (regexp != null && regexp.indexOf("@NOTHING_SELECTED@") > -1) {
            regexp = regexp.replaceAll("@NOTHING_SELECTED@", NOTHING_SELECTED);
        }
        if (type == null) {
            if (items == null) {
                this.type = Type.TEXTFIELD;
            } else if (items.length == 0) {
                this.type = Type.TEXTFIELD;
            } else if (items.length == 2) {
                if (items[0].toString().equalsIgnoreCase(
                            Tools.getString("Boolean.True"))
                    && items[1].toString().equalsIgnoreCase(
                                        Tools.getString("Boolean.False"))) {
                    this.type = type.CHECKBOX;
                } else {
                    this.type = Type.COMBOBOX;
                }
            } else {
                this.type = Type.COMBOBOX;
            }
        } else {
            this.type = type;
        }

        switch(this.type) {
            case TEXTFIELD:
                component = getTextField(selectedValue, regexp, abbreviations);
                break;
            case PASSWDFIELD:
                component = getPasswdField(selectedValue, regexp);
                break;
            case COMBOBOX:
                component = getComboBox(selectedValue,
                                        items,
                                        regexp,
                                        abbreviations);
                break;
            case TEXTFIELDWITHUNIT:
                component = new JPanel();
                component.setLayout(new SpringLayout());

                final Matcher m = unitPattern.matcher(selectedValue);
                String number = "";
                String unit = "";
                if (m.matches()) {
                    number = m.group(1);
                    final String parsedUnit = m.group(2);
                    if (!"".equals(parsedUnit)) {
                        unit = parsedUnit;
                    }
                }

                /* text field */
                textFieldWithoutUnit = (JTextField) getTextField(number,
                                                                 regexp,
                                                                 abbreviations);
                component.add(textFieldWithoutUnit);

                /* unit combo box */
                unitComboBox = (JComboBox) getComboBox(unit,
                                                       units,
                                                       regexp,
                                                       abbreviations);

                component.add(unitComboBox);
                SpringUtilities.makeCompactGrid(component, 1, 2,
                                                           0, 0,
                                                           0, 0);
                break;
            case RADIOGROUP:
                component = getRadioGroup(selectedValue, items);
                break;
            case CHECKBOX:
                if (items != null && items.length == 2) {
                    checkBoxTrue  = (String) items[0];
                    checkBoxFalse = (String) items[1];
                }
                component = getCheckBox(selectedValue);
                break;
            default:
        }
        if (this.type == Type.TEXTFIELDWITHUNIT) {
            textFieldWithoutUnit.setMinimumSize(new Dimension(width / 3,
                                                              WIDGET_HEIGHT));
            textFieldWithoutUnit.setMaximumSize(new Dimension(width / 3,
                                                              WIDGET_HEIGHT));
            textFieldWithoutUnit.setPreferredSize(new Dimension(width / 3,
                                                                WIDGET_HEIGHT));
            unitComboBox.setMinimumSize(new Dimension(width / 3 * 2,
                                                      WIDGET_HEIGHT));
            unitComboBox.setMaximumSize(new Dimension(width / 3 * 2,
                                                      WIDGET_HEIGHT));
            unitComboBox.setPreferredSize(new Dimension(width / 3 * 2,
                                                        WIDGET_HEIGHT));
        }
        component.setPreferredSize(new Dimension(width,
                                                 WIDGET_HEIGHT));
        setPreferredSize(new Dimension(width, WIDGET_COMPONENT_HEIGHT));
        if (width != 0) {
            component.setMaximumSize(new Dimension(width,
                                                   WIDGET_HEIGHT));
            setMaximumSize(new Dimension(width, WIDGET_COMPONENT_HEIGHT));
        }

        add(Box.createRigidArea(new Dimension(0, 1)), BorderLayout.PAGE_START);
        add(component, BorderLayout.CENTER);
        add(Box.createRigidArea(new Dimension(0, 1)), BorderLayout.PAGE_END);
    }

    /**
     * Prepares a new <code>GuiComboBox</code> object.
     */
    public GuiComboBox(final String selectedValue,
                       final Object[] items,
                       final Type typeOfBox,
                       final String regexp,
                       final int width,
                       final Map<String, String> abbreviations) {
        this(selectedValue,
             items,
             null,
             typeOfBox,
             regexp,
             width,
             abbreviations);
    }

    /**
     * Returns whether this field is a check box.
     */
    public final boolean isCheckBox() {
        return (type == Type.CHECKBOX);
    }

    /**
     * Returns new JPasswordField with default value.
     */
    private JComponent getPasswdField(final String value,
                                      final String regexp) {
        JPasswordField pf;
        if (regexp == null) {
            pf = new JPasswordField(value);
        } else {
            pf = new JPasswordField(new PatternDocument(regexp),
                                    value,
                                    0);
        }
        return pf;
    }

    /**
     * Returns new MTextField with default value.
     */
    private JComponent getTextField(final String value,
                                    final String regexp,
                                    final Map<String, String> abbreviations) {
        MTextField tf;
        if (regexp == null) {
            tf = new MTextField(value);
        } else {
            tf = new MTextField(new PatternDocument(regexp, abbreviations),
                                value,
                                0);
        }
        return tf;
    }

    /**
     * Returns combo box with items in the combo and selectedValue on top.
     */
    private JComponent getComboBox(final String selectedValue,
                                   final Object[] items,
                                   final String regexp,
                                   final Map<String, String> abbreviations) {
        final List<Object> comboList = new ArrayList<Object>();

        final Object selectedValueInfo = addItems(comboList,
                                                  selectedValue,
                                                  items);
        final JComboBox cb = new JComboBox(comboList.toArray(
                                            new Object[comboList.size()]));
        final JTextComponent editor =
                        (JTextComponent) cb.getEditor().getEditorComponent();
        if (regexp != null) {
            editor.setDocument(new PatternDocument(regexp, abbreviations));
        }
        cb.setMaximumRowCount(SCROLLBAR_MAX_ROWS);
        if (selectedValueInfo != null) {
            cb.setSelectedItem(selectedValueInfo);
        }
        /* workround, so that default button works */
        editor.addKeyListener(new ActivateDefaultButtonListener(cb));

        /* removing select... keyword */
        editor.addFocusListener(new FocusListener() {
            public void focusGained(final FocusEvent e) {
                Object o = getValue();
                if (o != null && !Tools.isStringClass(o)
                    && ((Info) o).getStringValue() == null) {
                    o = null;
                }
                if (o == null) {
                    editor.setText("");
                }
            }

            public void focusLost(final FocusEvent e) {
                /* do nothing */
            }
        });
        return cb;
    }

    /**
     * Reloads combo box with items and selects supplied value.
     */
    public final void reloadComboBox(final String selectedValue,
                                     final Object[] items) {
        if (type != Type.COMBOBOX) {
            return;
        }

        component.setPreferredSize(null);
        /* removing dupicates */
        final HashSet<String> itemCache = new HashSet<String>();

        final List<Object> comboList = new ArrayList<Object>();
        final Object selectedValueInfo = addItems(comboList,
                                                  selectedValue,
                                                  items);

        final JComboBox cb = (JComboBox) component;
        cb.setSelectedIndex(-1);
        for (final Object item : comboList) {
            if (!itemCache.contains(item.toString())) {
                cb.addItem(item);
                itemCache.add(item.toString());
            }
        }
        if (selectedValueInfo != null) {
            cb.setSelectedItem(selectedValueInfo);
        }
    }

    /**
     * Adds items to the combo box.
     */
    private Object addItems(final List<Object> comboList,
                            final String selectedValue,
                            final Object[] items) {
        Object selectedValueInfo = null;
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                if (items[i] == null) {
                    items[i] = GuiComboBox.NOTHING_SELECTED;
                }
                if (items[i].toString().equals(selectedValue)
                    || items[i].equals(selectedValue)) {
                    selectedValueInfo = items[i];
                }
                comboList.add(items[i]);
            }
            if (selectedValueInfo == null && selectedValue != null) {
                /* adds selected value to the list, if it is not there,
                   but only for string class. */
                if (items.length > 0 && Tools.isStringClass(items[0])) {
                    comboList.add(0, selectedValue);
                    selectedValueInfo = selectedValue;
                } else {
                    Tools.appWarning("cannot select: " + selectedValue);
                }
            }
        }
        return selectedValueInfo;
    }

    /**
     * Returns radio group with selected value.
     */
    private JComponent getRadioGroup(final String selectedValue,
                                     final Object[] items) {
        final ButtonGroup group = new ButtonGroup();
        final JPanel radioPanel = new JPanel(new GridLayout(1, 0));
        componentsHash.clear();
        for (int i = 0; i < items.length; i++) {
            final String item = items[i].toString();
            final JRadioButton rb = new JRadioButton(item);
            componentsHash.put(item, rb);
            rb.setActionCommand(item);
            group.add(rb);
            radioPanel.add(rb);

            if (items[i].equals(selectedValue)) {
                rb.setSelected(true);
                radioGroupValue = selectedValue;
            }

            rb.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    radioGroupValue = e.getActionCommand();
                }
            });
        }

        return radioPanel;
    }

    /**
     * Enables/Disables component in a group of components identified by
     * specified string. This works only with RADIOGROUP in a moment.
     */
    public void setEnabled(final String s, final boolean enabled) {
        if (componentsHash.containsKey(s)) {
            componentsHash.get(s).setEnabled(enabled);
        }
    }

    /**
     * Returns check box for boolean values.
     */
    private JComponent getCheckBox(final String selectedValue) {
        final JCheckBox cb = new JCheckBox();
        if (selectedValue != null) {
            cb.setSelected(selectedValue.equals(checkBoxTrue));
        }
        return cb;
    }

    /**
     * Sets the tooltip text.
     */
    public final void setToolTipText(final String text) {
        component.setToolTipText(text);
    }

    /**
     * Sets the field editable.
     */
    public final void setEditable() {
        setEditable(editable);
    }

    /**
     * Sets combo box editable.
     */
    public final void setEditable(final boolean editable) {
        this.editable = editable;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                switch(type) {
                    case TEXTFIELD:
                        break;
                    case PASSWDFIELD:
                        break;
                    case COMBOBOX:
                        Object o = getValue();
                        if (o != null
                            && !Tools.isStringClass(o)
                            && ((Info) o).getStringValue() == null) {
                            o = null;
                        }
                        if (alwaysEditable) {
                            ((JComboBox) component).setEditable(true);
                            final JTextComponent editor = getTextComponent();
                            if (o == null) {
                                editor.selectAll();
                            }
                        } else {
                            if (o == null) {
                                ((JComboBox) component).setEditable(false);
                            } else {
                                ((JComboBox) component).setEditable(editable);
                            }
                        }
                        break;
                    case RADIOGROUP:
                        break;
                    case CHECKBOX:
                        break;
                    default:
                }
            }
        });
    }

    /**
     * Returns string value. If object value is null, returns empty string (not
     * null).
     */
    public final String getStringValue() {
        final Object o = getValue();
        if (o == null) {
            return "";
        } else {
            return o.toString();
        }
    }

    /**
     * Returns value, that use chose in the combo box or typed in.
     */
    public final Object getValue() {
        Object value = null;
        switch(type) {
            case TEXTFIELD:
                value = ((MTextField) component).getText();
                break;
            case PASSWDFIELD:
                value = new String(((JPasswordField) component).getPassword());
                break;
            case COMBOBOX:
                final JComboBox cb = (JComboBox) component;
                if (cb.isEditable()) {
                    final JTextComponent editor =
                        (JTextComponent) cb.getEditor().getEditorComponent();
                    final String text = editor.getText();
                    value = cb.getSelectedItem();
                    if (value == null || !text.equals(value.toString())) {
                        value = text;
                    }

                    if ("".equals(value)) {
                        return null;
                    }
                } else {
                    value = cb.getSelectedItem();
                }
                break;
            case RADIOGROUP:
                value = radioGroupValue;
                break;
            case CHECKBOX:
                final JCheckBox cbox = (JCheckBox) component;
                if (cbox.getSelectedObjects() == null) {
                    value = checkBoxFalse;
                } else {
                    value = checkBoxTrue;
                }
                break;
            case TEXTFIELDWITHUNIT:
                final String text = textFieldWithoutUnit.getText();
                final StringBuffer s = new StringBuffer(text);
                final Object unit = unitComboBox.getSelectedItem();
                if (!Tools.isStringClass(unit)) {
                    final Unit u = (Unit) unit;
                    if (u.isPlural() == "1".equals(s.toString())) {
                        u.setPlural(!"1".equals(s.toString()));
                        unitComboBox.repaint();
                    }
                    if ("".equals(text)) {
                        if (!u.isEmpty()) {
                            u.setEmpty(true);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    unitComboBox.repaint();
                                    unitComboBox.setEnabled(false);
                                }
                            });
                        }
                    } else {
                        if (u.isEmpty()) {
                            u.setEmpty(false);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    unitComboBox.repaint();
                                    unitComboBox.setEnabled(true);
                                }
                            });
                        }
                        s.append(u.getShortName());
                    }
                }
                value = s.toString();
                break;
            default:
                // error
        }
        if (NOTHING_SELECTED.equals(value)) {
            return null;
        }
        return value;
    }

    /**
     * Clears the combo box.
     */
    public final void clear() {
        switch(type) {
            case TEXTFIELD:
                break;
            case PASSWDFIELD:
                break;

            case COMBOBOX:
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ((JComboBox) component).removeAllItems();
                    }
                });
                break;

            case RADIOGROUP:
                break;

            case CHECKBOX:
                break;
            default:
                // nothing
        }
    }

    /**
     * Sets component editable or unsets.
     */
    public final void setEnabled(final boolean enabled) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                component.setEnabled(enabled);
                for (final JComponent c : componentsHash.values()) {
                    c.setEnabled(enabled);
                }
            }
        });
    }

    /**
     * Returns whether component is editable or not.
     */
    public final boolean isEditable() {
        switch(type) {
            case TEXTFIELD:
                return true;
            case PASSWDFIELD:
                return true;
            case COMBOBOX:
                return ((JComboBox) component).isEditable();
            case RADIOGROUP:
                return false;
            case CHECKBOX:
                return false;
            default:
                break;
        }
        return false;
    }

    /**
     * Sets item/value in the component and waits till it is set.
     */
    public final void setValueAndWait(final Object item) {
        switch(type) {
            case TEXTFIELD:
                ((MTextField) component).setText((String) item);
                break;
            case PASSWDFIELD:
                ((JPasswordField) component).setText((String) item);
                break;
            case COMBOBOX:
                final JComboBox cb = (JComboBox) component;
                cb.setSelectedItem(item);
                if (cb.isEditable()) {
                    final JTextComponent tc =
                        (JTextComponent) cb.getEditor().getEditorComponent();
                    tc.setText((String) item);
                } else if (Tools.isStringClass(item)) {
                    Object selectedObject = null;
                    for (int i = 0; i < cb.getItemCount(); i++) {
                        final Object it = cb.getItemAt(i);
                        if (it.toString().equals(item)
                            || it.equals(item)
                            || (NOTHING_SELECTED.equals(it) && item == null)) {
                            selectedObject = it;
                            cb.setSelectedItem(it);
                            break;
                        }
                    }
                    if (selectedObject == null) {
                        cb.addItem(item);
                        cb.setSelectedItem(item);
                    }
                }
                break;

            case RADIOGROUP:
                if (item != null) {
                    final JRadioButton rb =
                                    (JRadioButton) componentsHash.get(item);
                    rb.setSelected(true);
                    radioGroupValue = (String) item;
                }
                break;

            case CHECKBOX:
                if (item != null) {
                    ((JCheckBox) component).setSelected(
                                                item.equals(checkBoxTrue));
                }
                break;

            case TEXTFIELDWITHUNIT:
                final Matcher m = unitPattern.matcher((String) item);
                String number = "";
                String unit = "";
                if (m.matches()) {
                    number = m.group(1);
                    unit = m.group(2);
                }

                textFieldWithoutUnit.setText(number);

                Object selectedUnitInfo = null;
                for (Unit u : units) {
                    if (u.equals(unit)) {
                        selectedUnitInfo = u;
                    }
                }

                unitComboBox.setSelectedItem(selectedUnitInfo);
                break;
            default:
                Tools.appError("impossible type");
        }
    }

    /**
     * Sets item/value in the component.
     */
    public final void setValue(final Object item) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setValueAndWait(item);
            }
        });
    }

    /**
     * Returns document object of the component.
     */
    public final Document getDocument() {
        switch(type) {
            case TEXTFIELD:
                return ((MTextField) component).getDocument();
            case PASSWDFIELD:
                return ((JPasswordField) component).getDocument();
            case COMBOBOX:
                final JTextComponent tc = getTextComponent();
                return tc.getDocument();
            case RADIOGROUP:
                return null;
            case CHECKBOX:
                return null;
            case TEXTFIELDWITHUNIT:
                return null;
            default:
                return null;
        }
    }

    /**
     * Returns the text component of the combo box.
     */
    private JTextComponent getTextComponent() {
        final ComboBoxEditor editor = ((JComboBox) component).getEditor();
        return (JTextComponent) editor.getEditorComponent();
    }

    /**
     * Selects part after first '*' in the ip.
     */
    public final void selectSubnet() {
        switch(type) {
            case TEXTFIELD:
                //ip = ((MTextField) component).getText();
                break;
            case PASSWDFIELD:
                break;
            case COMBOBOX:
                final JTextComponent tc = getTextComponent();
                final String ip = tc.getText();
                final int pos = ip.indexOf('*');
                if (pos >= 0) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            tc.select(pos, ip.length());
                        }
                    });
                }
                break;
            case RADIOGROUP:
                break;
            case CHECKBOX:
                break;
            default:
                Tools.appError("impossible type");
        }
    }

    /**
     * Adds item listener to the component.
     */
    public final void addListeners(final ItemListener il,
                             final DocumentListener dl) {
        switch(type) {
            case TEXTFIELD:
                if (dl != null) {
                    getDocument().addDocumentListener(dl);
                }
                break;
            case PASSWDFIELD:
                if (dl != null) {
                    getDocument().addDocumentListener(dl);
                }
                break;
            case COMBOBOX:
                if (il != null) {
                    ((JComboBox) component).addItemListener(il);
                }
                if (dl != null) {
                    getDocument().addDocumentListener(dl);
                }
                break;
            case RADIOGROUP:
                if (il != null) {
                    for (final JComponent c : componentsHash.values()) {
                        ((JRadioButton) c).addItemListener(il);
                    }
                }
                break;
            case CHECKBOX:
                if (il != null) {
                    ((JCheckBox) component).addItemListener(il);
                }
                break;
            case TEXTFIELDWITHUNIT:
                if (dl != null) {
                    textFieldWithoutUnit.getDocument().addDocumentListener(dl);
                }
                if (il != null) {
                    unitComboBox.addItemListener(il);
                }
                break;
            default:
                // error
        }
    }

    /**
     * Sets the background for the component which value is incorrect (failed).
     */
    public final void wrongValue() {
        switch(type) {
            case TEXTFIELD:
                component.setBackground(ERROR_VALUE_BACKGROUND);
                break;
            case PASSWDFIELD:
                component.setBackground(ERROR_VALUE_BACKGROUND);
                break;
            case COMBOBOX:
                setBackground(ERROR_VALUE_BACKGROUND);
                break;
            case RADIOGROUP:
                component.setBackground(ERROR_VALUE_BACKGROUND);
                for (final JComponent c : componentsHash.values()) {
                    c.setBackground(ERROR_VALUE_BACKGROUND);
                }
                break;
            case CHECKBOX:
                component.setBackground(ERROR_VALUE_BACKGROUND);
                break;
            case TEXTFIELDWITHUNIT:
                textFieldWithoutUnit.setBackground(ERROR_VALUE_BACKGROUND);
                break;
            default:
                // error
        }
    }

    /**
     * Sets background of the component depending if the value is the same
     * as its default value and if it is a required argument.
     * Must be called after combo box was already added to some panel.
     */
    public final void setBackground(final String defaultValue,
                                    final boolean required) {
        final String value = getStringValue();

        Color compColor = Color.WHITE;
        if (getParent() == null) {
            Tools.appError("wrong call to setBackground");
        }
        final Color backgroundColor = getParent().getBackground();
        if (value.equals(defaultValue)) {
            compColor = Tools.getDefaultColor("GuiComboBox.DefaultValue");
        }
        switch(type) {
            case TEXTFIELD:
                /* change color possibly set by wrongValue() */
                component.setBackground(compColor);
                break;
            case PASSWDFIELD:
                component.setBackground(compColor);
                break;
            case COMBOBOX:
                setBackground(Color.WHITE);
                break;
            case RADIOGROUP:
                component.setBackground(backgroundColor);
                for (final JComponent c : componentsHash.values()) {
                    c.setBackground(backgroundColor);
                }
                break;
            case CHECKBOX:
                component.setBackground(backgroundColor);
                break;
            case TEXTFIELDWITHUNIT:
                textFieldWithoutUnit.setBackground(Color.WHITE);
                break;
            default:
                // error
        }
        Color c;
        if (required) {
            //TODO: required fields should be marked differently
            c = Color.BLACK;
        } else  {
            c = backgroundColor;
        }
        switch(type) {
            case TEXTFIELD:
                setBackground(c);
                break;
            case PASSWDFIELD:
                setBackground(c);
                break;
            case COMBOBOX:
                setBackground(c);
                break;
            case RADIOGROUP:
                setBackground(c);
                break;
            case CHECKBOX:
                setBackground(c);
                break;
            case TEXTFIELDWITHUNIT:
                setBackground(c);
                break;
            default:
                // error
        }
    }

    /**
     * Workaround for jcombobox so that it works with default button.
     */
    public class ActivateDefaultButtonListener extends KeyAdapter
                                               implements ActionListener {
        /** Combobox, that should work with default button. */
        private final JComboBox box;

        /** Creates new ActivateDefaultButtonListener. */
        public ActivateDefaultButtonListener(final JComboBox box) {
            super();
            this.box = box;
        }

        /**
         * Is called when a key was pressed.
         */
        public final void keyPressed(final KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                // Simulte click on default button.
                doClick(e);
            }
        }

        /**
         * Is called when an action was performed.
         */
        public final void actionPerformed(final ActionEvent e) {
            doClick(e);
        }

        /**
         * Do click.
         */
        private void doClick(final java.util.EventObject e) {
            final Component c = (Component) e.getSource();

            final JRootPane rootPane = SwingUtilities.getRootPane(c);

            if (rootPane != null) {
                final JButton defaultButton = rootPane.getDefaultButton();

                if (defaultButton != null && !box.isPopupVisible()) {
                    final Object selection = box.getEditor().getItem();
                    box.setSelectedItem(selection);
                    defaultButton.doClick();
                }
            }
        }
    }

    /**
     * TextField that selects all when focused.
     */
    private class MTextField extends JTextField {
        /** Serial Version UID. */
        private static final long serialVersionUID = 1L;
        /** To select all only once. */
        private volatile boolean selected = false;

        /**
         * Creates a new MTextField object.
         */
        public MTextField(final String text) {
            super(text);
        }

        /**
         * Creates a new MTextField object.
         */
        public MTextField(final Document doc,
                          final String text,
                          final int columns) {
            super(doc, text, columns);
        }

        /**
         * Focus event.
         */
        protected void processFocusEvent(FocusEvent e) {
            super.processFocusEvent(e);
            if (!selected) {
                selected = true;
                if (e.getID() == FocusEvent.FOCUS_GAINED) {
                    selectAll();
                }
            }
        }
    }

    /**
     * Sets flag that determines whether the combo box is always editable.
     */
    public final void setAlwaysEditable(final boolean alwaysEditable) {
        this.alwaysEditable = alwaysEditable;
        setEditable(alwaysEditable);
    }

    /**
     * Requests focus if applicable.
     */
    public final void requestFocus() {

        switch(type) {
            case TEXTFIELD:
                ((MTextField) component).requestFocus();
                break;
            case PASSWDFIELD:
                ((JPasswordField) component).requestFocus();
                break;
            case COMBOBOX:
                break;
            case RADIOGROUP:
                break;
            case CHECKBOX:
                break;
            case TEXTFIELDWITHUNIT:
                textFieldWithoutUnit.requestFocus();
                break;
            default:
                break;
        }
    }

    /**
     * Selects the whole text in the widget if applicable.
     */
    public final void selectAll() {

        switch(type) {
            case TEXTFIELD:
                ((MTextField) component).selectAll();
                break;
            case PASSWDFIELD:
                ((JPasswordField) component).selectAll();
                break;
            case COMBOBOX:
                break;
            case RADIOGROUP:
                break;
            case CHECKBOX:
                break;
            case TEXTFIELDWITHUNIT:
                textFieldWithoutUnit.selectAll();
                break;
            default:
                break;
        }
    }

    /**
     * Sets the width of the widget.
     */
    public final void setWidth(final int newWidth) {
        final int hc = (int) component.getPreferredSize().getHeight();
        final int h = (int) getPreferredSize().getHeight();
        component.setMinimumSize(new Dimension(newWidth, hc));
        component.setPreferredSize(new Dimension(newWidth, hc));
        component.setMaximumSize(new Dimension(newWidth, hc));
        setMinimumSize(new Dimension(newWidth, h));
        setPreferredSize(new Dimension(newWidth, h));
        setMaximumSize(new Dimension(newWidth, h));
        revalidate();
        component.revalidate();
        repaint();
        component.repaint();
    }

    /**
     * Returns its component.
     */
     public final JComponent getJComponent() {
         return component;
     }

    /**
     * Sets background color.
     */
    public final void setBackgroundColor(final Color bg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setBackground(bg);
                component.setBackground(bg);
                for (final JComponent c : componentsHash.values()) {
                    c.setBackground(bg);
                }
            }
        });
    }
}
