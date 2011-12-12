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

package lcmc.gui;

import lcmc.utilities.Tools;
import lcmc.utilities.Unit;
import lcmc.utilities.PatternDocument;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.gui.resources.Info;
import lcmc.utilities.MyButton;

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
import javax.swing.text.AbstractDocument;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.ComboBoxEditor;
import javax.swing.SpringLayout;
import javax.swing.event.DocumentListener;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

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
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;
import java.awt.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
public final class GuiComboBox extends JPanel {
    /** Widget type. */
    public enum Type { LABELFIELD, TEXTFIELD, PASSWDFIELD, COMBOBOX,
                       RADIOGROUP, CHECKBOX, TEXTFIELDWITHUNIT };
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
    private JTextField textFieldPart = null;
    /** Combo box with units. */
    private JComboBox unitComboBox = null;
    /** Pattern that matches value and unit. */
    private final Pattern unitPattern = Pattern.compile("^(\\d+)(\\D*)$");
    /** File chooser button or some other button. */
    private MyButton fieldButton;
    /** Component part of field with button. */
    private JComponent componentPart = null;
    /** Name for the 'true' value. */
    private String checkBoxTrue = Tools.getString("Boolean.True");
    /** Name for the 'false' value. */
    private String checkBoxFalse = Tools.getString("Boolean.False");
    /** Background of the field if the value is wrong. */
    private static final Color ERROR_VALUE_BACKGROUND =
                            Tools.getDefaultColor("GuiComboBox.ErrorValue");
    /** Background of the field if the value has changed. */
    private static final Color CHANGED_VALUE_COLOR =
                            Tools.getDefaultColor("GuiComboBox.ChangedValue");
    /** Background of the field if the value is default. */
    private static final Color DEFAULT_VALUE_COLOR =
                            Tools.getDefaultColor("GuiComboBox.DefaultValue");
    /** Background of the field if the value is saved. */
    private static final Color SAVED_VALUE_COLOR =
                            Tools.getDefaultColor("GuiComboBox.SavedValue");
    /** No scrollbar ever. */
    private static final int SCROLLBAR_MAX_ROWS = 10;
    /** Widget default height. */
    private static final int WIDGET_HEIGHT = 28;
    /** Widget enclosing component default height. */
    private static final int WIDGET_COMPONENT_HEIGHT = 30;
    /** Radio group hash, from string that is displayed to the object. */
    private final Map<String, Object> radioGroupHash =
                                                 new HashMap<String, Object>();
    /** Name to component hash. */
    private final Map<String, JComponent> componentsHash =
                                             new HashMap<String, JComponent>();
    /** group components lock. */
    private final ReadWriteLock mComponentsLock = new ReentrantReadWriteLock();
    private final Lock mComponentsReadLock = mComponentsLock.readLock();
    private final Lock mComponentsWriteLock = mComponentsLock.writeLock();
    /** Nothing selected string, that returns null, if selected. */
    public static final String NOTHING_SELECTED =
                                Tools.getString("GuiComboBox.NothingSelected");
    /** Label of this component. */
    private JLabel label = null;
    /** Whether the component should be enabled. */
    private boolean enablePredicate = true;
    /** Whether the unit combo box should be enabled. */
    private boolean unitEnabled = true;
    /** Whether the extra text field button should be enabled. */
    private boolean tfButtonEnabled = true;
    /** Access Type for this component to become enabled. */
    private AccessMode enableAccessMode = new AccessMode(
                                                    ConfigData.AccessType.RO,
                                                    false);
    /** Tooltip if element is enabled. */
    private String toolTipText = null;
    /** Tooltip for label if it is enabled. */
    private String labelToolTipText = null;
    /** getValue setValue lock. */
    private final ReadWriteLock mValueLock = new ReentrantReadWriteLock();
    private final Lock mValueReadLock = mValueLock.readLock();
    private final Lock mValueWriteLock = mValueLock.writeLock();
    /** Regexp that this field must match. */
    private final String regexp;
    /** Reason why it is disabled. */
    private String disabledReason = null;

    /** Prepares a new <code>GuiComboBox</code> object. */
    public GuiComboBox(final String selectedValue,
                       final Object[] items,
                       final Unit[] units,
                       final Type type,
                       final String regexp,
                       final int width,
                       final Map<String, String> abbreviations,
                       final AccessMode enableAccessMode) {
        this(selectedValue,
             items,
             units,
             type,
             regexp,
             width,
             abbreviations,
             enableAccessMode,
             null); /* without button */
    }

    /** Prepares a new <code>GuiComboBox</code> object. */
    public GuiComboBox(final String selectedValue,
                       final Object[] items,
                       final Unit[] units,
                       final Type type,
                       final String regexp,
                       final int width,
                       final Map<String, String> abbreviations,
                       final AccessMode enableAccessMode,
                       final MyButton fieldButton) {
        super();
        this.units = units;
        this.enableAccessMode = enableAccessMode;
        this.fieldButton = fieldButton;
        setLayout(new BorderLayout(0, 0));
        if (regexp != null && regexp.indexOf("@NOTHING_SELECTED@") > -1) {
            this.regexp =
                    regexp.replaceAll("@NOTHING_SELECTED@", NOTHING_SELECTED);
        } else {
            this.regexp = regexp;
        }
        if (type == null) {
            if (items == null) {
                this.type = Type.TEXTFIELD;
            } else if (items.length == 0) {
                this.type = Type.TEXTFIELD;
            } else if (items.length == 2) {
                if (items[0] != null && items[0].toString().equalsIgnoreCase(
                            Tools.getString("Boolean.True"))
                    && items[1] != null
                    && items[1].toString().equalsIgnoreCase(
                                        Tools.getString("Boolean.False"))) {
                    this.type = Type.CHECKBOX;
                } else {
                    this.type = Type.COMBOBOX;
                }
            } else {
                this.type = Type.COMBOBOX;
            }
        } else {
            this.type = type;
        }

        JComponent newComp = null;
        switch(this.type) {
            case LABELFIELD:
                newComp = getLabelField(selectedValue);
                break;
            case TEXTFIELD:
                newComp = getTextField(selectedValue, regexp, abbreviations);
                break;
            case PASSWDFIELD:
                newComp = getPasswdField(selectedValue, regexp);
                break;
            case COMBOBOX:
                newComp = getComboBox(selectedValue,
                                      items,
                                      regexp,
                                      abbreviations);
                break;
            case TEXTFIELDWITHUNIT:
                newComp = new JPanel();
                newComp.setLayout(new SpringLayout());

                String number = "";
                String unit = "";
                if (selectedValue != null) {
                    final Matcher m = unitPattern.matcher(selectedValue);
                    if (m.matches()) {
                        number = m.group(1);
                        final String parsedUnit = m.group(2);
                        if (!"".equals(parsedUnit)) {
                            unit = parsedUnit;
                        }
                    }
                }

                /* text field */
                textFieldPart = (JTextField) getTextField(number,
                                                          regexp,
                                                          abbreviations);
                newComp.add(textFieldPart);

                /* unit combo box */
                unitComboBox = (JComboBox) getComboBox(unit,
                                                       units,
                                                       regexp,
                                                       abbreviations);

                newComp.add(unitComboBox);
                SpringUtilities.makeCompactGrid(newComp, 1, 2,
                                                         0, 0,
                                                         0, 0);
                break;
            //case TEXTFIELDWITHBUTTON:
            //    newComp = new JPanel();
            //    newComp.setLayout(new SpringLayout());

            //    /* text field */
            //    textFieldPart = (JTextField) getTextField(selectedValue,
            //                                              regexp,
            //                                              abbreviations);
            //    newComp.add(textFieldPart);
            //    /** add button */
            //    SpringUtilities.makeCompactGrid(newComp, 1, 2,
            //                                               0, 0,
            //                                               0, 0);
            //    break;
            case RADIOGROUP:
                newComp = getRadioGroup(selectedValue, items);
                break;
            case CHECKBOX:
                if (items != null && items.length == 2) {
                    checkBoxTrue  = (String) items[0];
                    checkBoxFalse = (String) items[1];
                }
                newComp = getCheckBox(selectedValue);
                break;
            default:
        }
        if (fieldButton == null) {
            component = newComp;
        } else {
            componentPart = newComp;
            componentPart.setPreferredSize(new Dimension(width / 3 * 2,
                                                         WIDGET_HEIGHT));
            componentPart.setMinimumSize(componentPart.getPreferredSize());
            componentPart.setMaximumSize(componentPart.getPreferredSize());
            component = new JPanel();
            component.setLayout(new SpringLayout());

            component.add(newComp);
            component.add(fieldButton);
            /** add button */
            SpringUtilities.makeCompactGrid(component, 1, 2,
                                                       0, 0,
                                                       0, 0);
        }
        if (this.type == Type.TEXTFIELDWITHUNIT) {
            textFieldPart.setPreferredSize(new Dimension(width / 3,
                                                         WIDGET_HEIGHT));
            textFieldPart.setMinimumSize(textFieldPart.getPreferredSize());
            textFieldPart.setMaximumSize(textFieldPart.getPreferredSize());

            unitComboBox.setPreferredSize(new Dimension(width / 3 * 2,
                                                        WIDGET_HEIGHT));
            unitComboBox.setMinimumSize(unitComboBox.getPreferredSize());
            unitComboBox.setMaximumSize(unitComboBox.getPreferredSize());
        }
        component.setPreferredSize(new Dimension(width,
                                                 WIDGET_HEIGHT));
        if (componentPart != null) {
            componentPart.setPreferredSize(new Dimension(width,
                                                         WIDGET_HEIGHT));
        }
        setPreferredSize(new Dimension(width, WIDGET_COMPONENT_HEIGHT));
        if (width != 0) {
            component.setMaximumSize(new Dimension(width,
                                                   WIDGET_HEIGHT));
            setMaximumSize(new Dimension(width, WIDGET_COMPONENT_HEIGHT));
        }

        add(Box.createRigidArea(new Dimension(0, 1)), BorderLayout.PAGE_START);
        add(component, BorderLayout.CENTER);
        add(Box.createRigidArea(new Dimension(0, 1)), BorderLayout.PAGE_END);
        processAccessMode();
    }

    /** Returns whether this field is a check box. */
    public boolean isCheckBox() {
        return (type == Type.CHECKBOX);
    }

    /** Returns new JPasswordField with default value. */
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

    /** Returns new MTextField with default value. */
    private JComponent getLabelField(final String value) {
        return new JLabel(value);
    }

    /** Returns new MTextField with default value. */
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

    /** Returns combo box with items in the combo and selectedValue on top. */
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
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    cb.setSelectedItem(selectedValueInfo);
                }
            });
        }
        /* workround, so that default button works */
        editor.addKeyListener(new ActivateDefaultButtonListener(cb));

        /* removing select... keyword */
        editor.addFocusListener(new FocusListener() {
            @Override public void focusGained(final FocusEvent e) {
                //Object o = (GuiComboBox) e.getSource()).getValue();
                Object o = getValue();
                if (o != null && !Tools.isStringClass(o)
                    && ((Info) o).getStringValue() == null) {
                    o = null;
                }
                if (o == null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            editor.setText("");
                        }
                    });
                }
            }

            @Override public void focusLost(final FocusEvent e) {
                /* do nothing */
            }
        });
        cb.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuCanceled(final PopupMenuEvent pe) {
                /* do nothing */
            }
            @Override public void popupMenuWillBecomeInvisible(
                                                     final PopupMenuEvent pe) {
                /* do nothing */
            }
            @Override public void popupMenuWillBecomeVisible(
                                                    final PopupMenuEvent pe) {
                /* workaround to have items with bigger widths than jcombobox */
                final JComboBox thisCB = (JComboBox) pe.getSource();
                final Object c = thisCB.getUI().getAccessibleChild(thisCB, 0);
                if (!(c instanceof JPopupMenu)) {
                    return;
                }
                final JScrollPane scrollPane =
                            (JScrollPane) ((JPopupMenu) c).getComponent(0);
                final Dimension size = scrollPane.getPreferredSize();
                final JComponent view =
                               (JComponent) scrollPane.getViewport().getView();
                final int newSize = view.getPreferredSize().width;
                if (newSize > size.width) {
                    size.width = newSize;
                    scrollPane.setPreferredSize(size);
                    scrollPane.setMaximumSize(size);
                }
            }
        });
        return cb;
    }

    /** Returns true if combo box has changed. */
    private boolean comboBoxChanged(final Object[] items) {
        final JComboBox cb = (JComboBox) component;
        if (items.length != cb.getItemCount()) {
            return true;
        }

        for (int i = 0; i < items.length; i++) {
            Object item;
            if (items[i] == null) {
                item = GuiComboBox.NOTHING_SELECTED;
            } else {
                item = items[i];
            }
            if (!Tools.areEqual(item, cb.getItemAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** Reloads combo box with items and selects supplied value. */
    public void reloadComboBox(final String selectedValue,
                               final Object[] items) {
        if (type != Type.COMBOBOX) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                final JComboBox cb = (JComboBox) component;
                final Object selectedItem = cb.getSelectedItem();
                boolean selectedChanged = false;
                if (selectedValue == null
                    && (selectedItem != null
                         && selectedItem != GuiComboBox.NOTHING_SELECTED)) {
                    selectedChanged = true;
                } else if (selectedValue != null
                           && !selectedValue.equals(selectedItem)) {
                    selectedChanged = true;
                }
                final boolean itemsChanged = comboBoxChanged(items);
                if (!selectedChanged && !itemsChanged) {
                    return;
                }

                component.setPreferredSize(null);
                /* removing dupicates */

                final List<Object> comboList = new ArrayList<Object>();
                final Object selectedValueInfo = addItems(comboList,
                                                          selectedValue,
                                                          items);

                if (itemsChanged) {
                    final HashSet<String> itemCache = new HashSet<String>();
                    cb.setSelectedIndex(-1);
                    cb.removeAllItems();
                    for (final Object item : comboList) {
                        if (!itemCache.contains(item.toString())) {
                            cb.addItem(item);
                            itemCache.add(item.toString());
                        }
                    }
                }
                if (selectedValueInfo != null) {
                    cb.setSelectedItem(selectedValueInfo);
                }
            }
        });
    }

    /** Adds items to the combo box. */
    private Object addItems(final List<Object> comboList,
                            final String selectedValue,
                            final Object[] items) {
        Object selectedValueInfo = null;
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                if (items[i] == null) {
                    items[i] = GuiComboBox.NOTHING_SELECTED;
                }
                if (items[i] instanceof Info
                    && ((Info) items[i]).getStringValue() != null
                    && ((Info) items[i]).getStringValue().equals(
                                                             selectedValue)) {
                    selectedValueInfo = items[i];
                } else if (items[i] instanceof Unit
                    && ((Unit) items[i]).equals(selectedValue)) {
                    selectedValueInfo = items[i];
                } else if (items[i].toString().equals(selectedValue)
                    || items[i].equals(selectedValue)) {
                    selectedValueInfo = items[i];
                }
                comboList.add(items[i]);
            }
            if (selectedValueInfo == null && selectedValue != null) {
                comboList.add(selectedValue);
                selectedValueInfo = selectedValue;
            }
        }
        return selectedValueInfo;
    }

    /** Returns radio group with selected value. */
    private JComponent getRadioGroup(final String selectedValue,
                                     final Object[] items) {
        final ButtonGroup group = new ButtonGroup();
        final JPanel radioPanel = new JPanel(new GridLayout(1, 1));
        mComponentsWriteLock.lock();
        componentsHash.clear();
        for (int i = 0; i < items.length; i++) {
            final Object item = items[i];
            final JRadioButton rb = new JRadioButton(item.toString());
            radioGroupHash.put(item.toString(), item);
            rb.setActionCommand(item.toString());
            group.add(rb);
            radioPanel.add(rb);

            String v;
            if (item instanceof Info) {
                v = ((Info) item).getStringValue();
            } else {
                v = item.toString();
            }
            componentsHash.put(v, rb);
            if (v.equals(selectedValue)) {
                rb.setSelected(true);
                radioGroupValue = selectedValue;
            }

            rb.addActionListener(new ActionListener() {
                @Override public void actionPerformed(final ActionEvent e) {
                    mComponentsReadLock.lock();
                    final Object item =
                                    radioGroupHash.get(e.getActionCommand());
                    mComponentsReadLock.unlock();
                    String v;
                    if (item instanceof Info) {
                        v = ((Info) item).getStringValue();
                    } else {
                        v = item.toString();
                    }
                    radioGroupValue = v;
                }
            });
        }
        mComponentsWriteLock.unlock();

        return radioPanel;
    }

    /**
     * Enables/Disables component in a group of components identified by
     * specified string. This works only with RADIOGROUP in a moment.
     */
    public void setEnabled(final String s, final boolean enabled) {
        enablePredicate = enabled;
        final boolean accessible =
                       Tools.getConfigData().isAccessible(enableAccessMode);
        mComponentsReadLock.lock();
        final JComponent c = componentsHash.get(s);
        mComponentsReadLock.unlock();
        if (c != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    c.setEnabled(enablePredicate && accessible);
                }
            });
        }
        if (label != null) {
            label.setEnabled(accessible);
        }
    }

    /**
     * Sets visible/invisible a component in a group of components identified
     * by specified string. This works only with RADIOGROUP in a moment.
     */
    public void setVisible(final String s, final boolean visible) {
        mComponentsReadLock.lock();
        final JComponent c = componentsHash.get(s);
        mComponentsReadLock.unlock();
        if (c != null) {
            c.setVisible(visible);
        }
    }

    /** Returns check box for boolean values. */
    private JComponent getCheckBox(final String selectedValue) {
        final JCheckBox cb = new JCheckBox();
        if (selectedValue != null) {
            cb.setSelected(selectedValue.equals(checkBoxTrue));
        }
        return cb;
    }

    /** Sets the tooltip text. */
    @Override public void setToolTipText(String text) {
        toolTipText = text;
        final String disabledReason0 = disabledReason;
        if (disabledReason0 != null) {
            text = text + "<br>" + disabledReason0;
        }
        if (enableAccessMode.getAccessType() != ConfigData.AccessType.NEVER) {
            final boolean accessible =
                     Tools.getConfigData().isAccessible(enableAccessMode);
            if (!accessible) {
                text = text + "<br>" + getDisabledTooltip();
            }
        }
        if (type == Type.TEXTFIELDWITHUNIT) {
            textFieldPart.setToolTipText("<html>" + text + "</html>");
            unitComboBox.setToolTipText("<html>" + text + "</html>");
        //} else if (type == Type.TEXTFIELDWITHBUTTON) {
        //    textFieldPart.setToolTipText("<html>" + text + "</html>");
        //    fieldButton.setToolTipText("<html>" + text + "</html>");
        } else {
            component.setToolTipText("<html>" + text + "</html>");
        }
        if (fieldButton != null) {
            componentPart.setToolTipText("<html>" + text + "</html>");
            fieldButton.setToolTipText("<html>" + text + "</html>");
        }
    }

    /** Sets label tooltip text. */
    void setLabelToolTipText(String text) {
        labelToolTipText = text;
        String disabledTooltip = null;
        if (enableAccessMode.getAccessType() != ConfigData.AccessType.NEVER) {
            final boolean accessible =
                     Tools.getConfigData().isAccessible(enableAccessMode);
            if (!accessible) {
                disabledTooltip = getDisabledTooltip();
            }
        }
        final String disabledReason0 = disabledReason;
        if (disabledReason0 != null || disabledTooltip != null) {
            final StringBuilder tt = new StringBuilder(40);
            if (disabledReason0 != null) {
                tt.append(disabledReason0);
                tt.append("<br>");
            }
            if (disabledTooltip != null) {
                tt.append(disabledTooltip);
            }
            if (text.length() > 6 && "<html>".equals(text.substring(0, 6))) {
                text = "<html>" + tt.toString() + "<br>" + "<br>"
                       + text.substring(6);
            } else {
                text = Tools.html(text + "<br>" + tt.toString());
            }
        }
        label.setToolTipText(text);
    }

    /** Returns tooltip for disabled element. */
    private String getDisabledTooltip() {
        String advanced = "";
        if (enableAccessMode.isAdvancedMode()) {
            advanced = "Advanced ";
        }
        final StringBuilder sb = new StringBuilder(100);
        sb.append("editable in \"");
        sb.append(advanced);
        sb.append(
                ConfigData.OP_MODES_MAP.get(enableAccessMode.getAccessType()));
        sb.append("\" mode");

        if (disabledReason != null) {
            /* yet another reason */
            sb.append(' ');
            sb.append(disabledReason);
        }

        return sb.toString();
    }

    /** Sets the field editable. */
    public void setEditable() {
        setEditable(editable);
    }

    /** Sets combo box editable. */
    public void setEditable(final boolean editable) {
        this.editable = editable;
        JComponent c;
        if (fieldButton == null) {
            c = component;
        } else {
            c = componentPart;
        }
        final JComponent comp = c;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                switch(type) {
                    case LABELFIELD:
                        break;
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
                            ((JComboBox) comp).setEditable(true);
                            final JTextComponent editor = getTextComponent();
                            if (o == null) {
                                editor.selectAll();
                            }
                        } else {
                            if (o == null) {
                                ((JComboBox) comp).setEditable(false);
                            } else {
                                ((JComboBox) comp).setEditable(editable);
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
    public String getStringValue() {
        final Object o = getValue();
        if (o == null) {
            return "";
        } else {
            if (type == Type.TEXTFIELDWITHUNIT) {
                final Object o0 = ((Object[]) o)[0];
                final Object o1 = ((Object[]) o)[1];
                String v = o0.toString();
                if (v != null && !"".equals(v) && o1 != null
                    && o1 instanceof Unit) {
                    v += ((Unit) o1).getShortName();
                }
                return v;
            } else {
                return o.toString();
            }
        }
    }

    /** Returns value, that use chose in the combo box or typed in. */
    public Object getValue() {
        Object value = null;
        JComponent comp;
        mValueReadLock.lock();
        if (fieldButton == null) {
            comp = component;
        } else {
            comp = componentPart;
        }
        switch(type) {
            case LABELFIELD:
                value = ((JLabel) comp).getText();
                break;
            case TEXTFIELD:
                value = ((MTextField) comp).getText();
                break;
            case PASSWDFIELD:
                value = new String(((JPasswordField) comp).getPassword());
                break;
            case COMBOBOX:
                final JComboBox cb = (JComboBox) comp;
                if (cb.isEditable()) {
                    final JTextComponent editor =
                        (JTextComponent) cb.getEditor().getEditorComponent();
                    String text = editor.getText();
                    if (text == null) {
                        text = "";
                    }
                    value = cb.getSelectedItem();
                    if (value == null || !text.equals(value.toString())) {
                        value = text;
                    }

                    if ("".equals(value)) {
                        mValueReadLock.unlock();
                        return "";
                    }
                } else {
                    value = cb.getSelectedItem();
                }
                break;
            case RADIOGROUP:
                value = radioGroupValue;
                break;
            case CHECKBOX:
                final JCheckBox cbox = (JCheckBox) comp;
                if (cbox.getSelectedObjects() == null) {
                    value = checkBoxFalse;
                } else {
                    value = checkBoxTrue;
                }
                break;
            case TEXTFIELDWITHUNIT:
                String text = textFieldPart.getText();
                if (text == null) {
                    text = "";
                }
                final Object unit = unitComboBox.getSelectedItem();
                if (!Tools.isStringClass(unit)) {
                    final Unit u = (Unit) unit;
                    if (u.isPlural() == "1".equals(text)) {
                        u.setPlural(!"1".equals(text));
                        unitComboBox.repaint();
                    }
                    final boolean accessible =
                      Tools.getConfigData().isAccessible(enableAccessMode);
                    if ("".equals(text)) {
                        if (!u.isEmpty()) {
                            u.setEmpty(true);
                            unitEnabled = false;
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override public void run() {
                                    unitComboBox.repaint();
                                    unitComboBox.setEnabled(false);
                                }
                            });
                        }
                    } else {
                        if (u.isEmpty()) {
                            u.setEmpty(false);
                            if (textFieldPart.isEnabled()) {
                                unitEnabled = true;
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override public void run() {
                                        unitComboBox.repaint();
                                        unitComboBox.setEnabled(accessible);
                                    }
                                });
                            }
                        }
                    }
                }
                value = new Object[]{text, unit};
                break;
            //case TEXTFIELDWITHBUTTON:
            //    value = textFieldPart.getText();
            //    break;
            default:
                /* error */
        }
        mValueReadLock.unlock();
        if (NOTHING_SELECTED.equals(value)) {
            return null;
        }
        return value;
    }

    /** Clears the combo box. */
    public void clear() {
        switch(type) {
            case LABELFIELD:
                break;
            case TEXTFIELD:
                break;
            case PASSWDFIELD:
                break;

            case COMBOBOX:
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        ((JComboBox) component).removeAllItems();
                    }
                });
                break;

            case RADIOGROUP:
                break;

            case CHECKBOX:
                break;
            default:
                /* nothing */
        }
    }

    /** Sets component visible or invisible and remembers this state. */
    @Override public void setVisible(final boolean visible) {
        setComponentsVisible(visible);
    }

    /** Sets component visible or invisible. */
    private void setComponentsVisible(final boolean visible) {
        JComponent c;
        if (fieldButton == null) {
            c = component;
        } else {
            c = componentPart;
        }
        final JComponent comp = c;
        super.setVisible(visible);
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (label != null) {
                    label.setVisible(visible);
                }
                comp.setVisible(visible);
                mComponentsReadLock.lock();
                for (final JComponent c : componentsHash.values()) {
                    c.setVisible(visible);
                }
                mComponentsReadLock.unlock();
                switch(type) {
                    case TEXTFIELDWITHUNIT:
                        textFieldPart.setVisible(visible);
                        unitComboBox.setVisible(visible);
                        break;
                    default:
                        break;
                }
                //if (fieldButton != null) {
                //    fieldButton.setVisible(visible);
                //}
                repaint();
            }
        });
    }


    /** Sets component enabled or disabled and remembers this state. */
    @Override public void setEnabled(final boolean enabled) {
        enablePredicate = enabled;
        setComponentsEnabled(
                   enablePredicate
                   && Tools.getConfigData().isAccessible(enableAccessMode));
    }

    /** Sets extra button enabled. */
    public void setTFButtonEnabled(final boolean tfButtonEnabled) {
        this.tfButtonEnabled = tfButtonEnabled;
        fieldButton.setEnabled(tfButtonEnabled);
    }

    /** Sets component enabled or disabled. */
    private void setComponentsEnabled(final boolean enabled) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                component.setEnabled(enabled);
                mComponentsReadLock.lock();
                for (final JComponent c : componentsHash.values()) {
                    c.setEnabled(enabled);
                }
                mComponentsReadLock.unlock();
                switch(type) {
                    case TEXTFIELDWITHUNIT:
                        textFieldPart.setEnabled(enabled);
                        unitComboBox.setEnabled(enabled && unitEnabled);
                        break;
                    default:
                        /* nothing */
                }
                if (fieldButton != null) {
                    componentPart.setEnabled(enabled);
                    fieldButton.setEnabled(enabled && tfButtonEnabled);
                }
            }
        });
    }


    /** Returns whether component is editable or not. */
    boolean isEditable() {
        switch(type) {
            case LABELFIELD:
                return false;
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

    /** Sets item/value in the component and waits till it is set. */
    public void setValueAndWait(final Object item) {
        mValueWriteLock.lock();
        JComponent comp;
        if (fieldButton == null) {
            comp = component;
        } else {
            comp = componentPart;
        }
        switch(type) {
            case LABELFIELD:
                ((JLabel) comp).setText((String) item);
                break;
            case TEXTFIELD:
                ((MTextField) comp).setText((String) item);
                break;
            case PASSWDFIELD:
                ((JPasswordField) comp).setText((String) item);
                break;
            case COMBOBOX:
                final JComboBox cb = (JComboBox) comp;
                cb.setSelectedItem(item);
                //if (cb.isEditable()) {
                //    final JTextComponent tc =
                //        (JTextComponent) cb.getEditor().getEditorComponent();
                //    tc.setText((String) item);
                if (Tools.isStringClass(item)) {
                    Object selectedObject = null;
                    for (int i = 0; i < cb.getItemCount(); i++) {
                        final Object it = cb.getItemAt(i);
                        if (it == item
                            || it.toString().equals(item)
                            || it.equals(item)
                            || ((it instanceof Info)
                                && Tools.areEqual(((Info) it).getStringValue(),
                                                  item))
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
                    mComponentsReadLock.lock();
                    final JRadioButton rb =
                                    (JRadioButton) componentsHash.get(item);
                    mComponentsReadLock.unlock();
                    if (rb != null) {
                        rb.setSelected(true);
                    }
                    if (item instanceof Info) {
                        radioGroupValue = ((Info) item).getStringValue();
                    } else {
                        radioGroupValue = (String) item;
                    }
                }
                break;

            case CHECKBOX:
                if (item != null) {
                    ((JCheckBox) comp).setSelected(
                                                item.equals(checkBoxTrue));
                }
                break;

            case TEXTFIELDWITHUNIT:
                Matcher m = null;
                if (item != null) {
                    m = unitPattern.matcher((String) item);
                }
                String number = "";
                String unit = "";
                if (m != null && m.matches()) {
                    number = m.group(1);
                    final String parsedUnit = m.group(2);
                    if (!"".equals(parsedUnit)) {
                        unit = parsedUnit;
                    }
                }

                textFieldPart.setText(number);

                Object selectedUnitInfo = null;
                for (Unit u : units) {
                    if (u.equals(unit)) {
                        selectedUnitInfo = u;
                    }
                }

                unitComboBox.setSelectedItem(selectedUnitInfo);
                break;
            //case TEXTFIELDWITHBUTTON:
            //    if (item != null) {
            //        textFieldPart.setText((String) item);
            //    }
            //    break;
            default:
                Tools.appError("impossible type");
        }
        mValueWriteLock.unlock();
    }

    /** Sets item/value in the component. */
    public void setValue(final Object item) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                setValueAndWait(item);
            }
        });
    }

    /** Sets selected index. */
    public void setSelectedIndex(final int index) {
        JComponent comp;
        if (fieldButton == null) {
            comp = component;
        } else {
            comp = componentPart;
        }
        switch(type) {
            case LABELFIELD:
                break;
            case TEXTFIELD:
                break;
            case PASSWDFIELD:
                break;
            case COMBOBOX:
                final JComboBox cb = (JComboBox) comp;
                cb.setSelectedIndex(index);
                break;

            case RADIOGROUP:
                break;

            case CHECKBOX:
                break;

            case TEXTFIELDWITHUNIT:
                break;
            //case TEXTFIELDWITHBUTTON:
            //    break;
            default:
                Tools.appError("impossible type");
        }
    }

    /** Returns document object of the component. */
    public Document getDocument() {
        JComponent comp;
        if (fieldButton == null) {
            comp = component;
        } else {
            comp = componentPart;
        }
        switch(type) {
            case LABELFIELD:
                return null;
            case TEXTFIELD:
                return ((MTextField) comp).getDocument();
            case PASSWDFIELD:
                return ((JPasswordField) comp).getDocument();
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

    /** Returns the text component of the combo box. */
    private JTextComponent getTextComponent() {
        JComponent comp;
        if (fieldButton == null) {
            comp = component;
        } else {
            comp = componentPart;
        }
        final ComboBoxEditor editor = ((JComboBox) comp).getEditor();
        return (JTextComponent) editor.getEditorComponent();
    }

    /** Selects part after first '*' in the ip. */
    public void selectSubnet() {
        switch(type) {
            case LABELFIELD:
                break;
            case TEXTFIELD:
                break;
            case PASSWDFIELD:
                break;
            case COMBOBOX:
                final JTextComponent tc = getTextComponent();
                final String ip = tc.getText();
                int p = ip.length() - 2;
                while (p >= 0
                       && Tools.isIp(ip)
                       && ".0".equals(ip.substring(p, p + 2))) {
                    p -= 2;
                }
                final int pos = p + 3;
                if (pos >= 0 && pos < ip.length()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
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

    /** Adds item listener to the component. */
    public void addListeners(final ItemListener il, final DocumentListener dl) {
        JComponent comp;
        if (fieldButton == null) {
            comp = component;
        } else {
            comp = componentPart;
        }
        switch(type) {
            case LABELFIELD:
                break;
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
                    ((JComboBox) comp).addItemListener(il);
                }
                if (dl != null) {
                    getDocument().addDocumentListener(dl);
                }
                break;
            case RADIOGROUP:
                if (il != null) {
                    mComponentsReadLock.lock();
                    for (final JComponent c : componentsHash.values()) {
                        ((JRadioButton) c).addItemListener(il);
                    }
                    mComponentsReadLock.unlock();
                }
                break;
            case CHECKBOX:
                if (il != null) {
                    ((JCheckBox) comp).addItemListener(il);
                }
                break;
            case TEXTFIELDWITHUNIT:
                if (dl != null) {
                    textFieldPart.getDocument().addDocumentListener(dl);
                }
                if (il != null) {
                    unitComboBox.addItemListener(il);
                }
                break;
            //case TEXTFIELDWITHBUTTON:
            //    if (dl != null) {
            //        textFieldPart.getDocument().addDocumentListener(dl);
            //    }
            //    break;
            default:
                /* error */
        }
    }

    /**
     * Sets the background for the component which value is incorrect (failed).
     */
    public void wrongValue() {
        setBackgroundColor(ERROR_VALUE_BACKGROUND);
        if (label != null) {
            label.setForeground(Color.RED);
        }
    }

    /** Sets background without considering the label. */
    public void setBackground(final Object defaultValue,
                              final Object savedValue,
                              final boolean required) {
        setBackground(null, defaultValue, null, savedValue, required);
    }

    /**
     * Sets background of the component depending if the value is the same
     * as its default value and if it is a required argument.
     * Must be called after combo box was already added to some panel.
     *
     * It also disables, hides the component depending on the access type.
     * TODO: rename the function
     */
    public void setBackground(final String defaultLabel,
                              final Object defaultValue,
                              final String savedLabel,
                              final Object savedValue,
                              final boolean required) {
        if (getParent() == null) {
            return;
        }
        JComponent comp;
        if (fieldButton == null) {
            comp = component;
        } else {
            comp = componentPart;
        }
        final Object value = getValue();
        String labelText = null;
        if (savedLabel != null) {
            labelText = label.getText();
        }

        final Color backgroundColor = getParent().getBackground();
        final Color compColor = Color.WHITE;
        if (!Tools.areEqual(value, savedValue)
            || (savedLabel != null && !Tools.areEqual(labelText, savedLabel))) {
            if (label != null) {
                Tools.debug(this, "changed label: " + labelText + " != "
                                   + savedLabel, 1);
                Tools.debug(this, "changed: " + value + " != "
                                         + savedValue, 1);
                /*
                   Tools.printStackTrace("changed: " + value + " != "
                                         + savedValue);
                 */
                label.setForeground(CHANGED_VALUE_COLOR);
            }
        } else if (Tools.areEqual(value, defaultValue)
                   && (savedLabel == null
                       || Tools.areEqual(labelText, defaultLabel))) {
            if (label != null) {
                label.setForeground(DEFAULT_VALUE_COLOR);
            }
        } else {
            if (label != null) {
                label.setForeground(SAVED_VALUE_COLOR);
            }
        }
        setBackground(backgroundColor);
        switch(type) {
            case LABELFIELD:
                comp.setBackground(backgroundColor);
                break;
            case TEXTFIELD:
                /* change color possibly set by wrongValue() */
                comp.setBackground(compColor);
                break;
            case PASSWDFIELD:
                comp.setBackground(compColor);
                break;
            case COMBOBOX:
                setBackground(Color.WHITE);
                break;
            case RADIOGROUP:
                comp.setBackground(backgroundColor);
                mComponentsReadLock.lock();
                for (final JComponent c : componentsHash.values()) {
                    c.setBackground(backgroundColor);
                }
                mComponentsReadLock.unlock();
                break;
            case CHECKBOX:
                comp.setBackground(backgroundColor);
                break;
            case TEXTFIELDWITHUNIT:
                textFieldPart.setBackground(Color.WHITE);
                break;
            //case TEXTFIELDWITHBUTTON:
            //    textFieldPart.setBackground(Color.WHITE);
            //    break;
            default:
                /* error */
        }
        processAccessMode();
    }

    /** Workaround for jcombobox so that it works with default button. */
    static class ActivateDefaultButtonListener extends KeyAdapter
                                               implements ActionListener {
        /** Combobox, that should work with default button. */
        private final JComboBox box;

        /** Creates new ActivateDefaultButtonListener. */
        ActivateDefaultButtonListener(final JComboBox box) {
            super();
            this.box = box;
        }

        /** Is called when a key was pressed. */
        @Override public void keyPressed(final KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                /* Simulte click on default button. */
                doClick(e);
            }
        }

        /** Is called when an action was performed. */
        @Override public void actionPerformed(final ActionEvent e) {
            doClick(e);
        }

        /** Do click. */
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

        /** Creates a new MTextField object. */
        MTextField(final String text) {
            super(text);
        }

        /** Creates a new MTextField object. */
        MTextField(final Document doc,
                   final String text,
                   final int columns) {
            super(doc, text, columns);
        }

        /** Focus event. */
        @Override protected void processFocusEvent(final FocusEvent e) {
            super.processFocusEvent(e);
            if (!selected) {
                selected = true;
                if (e.getID() == FocusEvent.FOCUS_GAINED) {
                    selectAll();
                }
            }
        }
    }

    /** Sets flag that determines whether the combo box is always editable. */
    public void setAlwaysEditable(final boolean alwaysEditable) {
        this.alwaysEditable = alwaysEditable;
        setEditable(alwaysEditable);
    }

    /** Requests focus if applicable. */
    @Override public void requestFocus() {
        JComponent comp;
        if (fieldButton == null) {
            comp = component;
        } else {
            comp = componentPart;
        }
        switch(type) {
            case LABELFIELD:
                break;
            case TEXTFIELD:
                ((MTextField) comp).requestFocus();
                break;
            case PASSWDFIELD:
                ((JPasswordField) comp).requestFocus();
                break;
            case COMBOBOX:
                break;
            case RADIOGROUP:
                break;
            case CHECKBOX:
                break;
            case TEXTFIELDWITHUNIT:
                textFieldPart.requestFocus();
                break;
            //case TEXTFIELDWITHBUTTON:
            //    textFieldPart.requestFocus();
            //    break;
            default:
                break;
        }
    }

    /** Selects the whole text in the widget if applicable. */
    void selectAll() {
        JComponent comp;
        if (fieldButton == null) {
            comp = component;
        } else {
            comp = componentPart;
        }
        switch(type) {
            case TEXTFIELD:
                ((MTextField) comp).selectAll();
                break;
            case PASSWDFIELD:
                ((JPasswordField) comp).selectAll();
                break;
            case COMBOBOX:
                break;
            case RADIOGROUP:
                break;
            case CHECKBOX:
                break;
            case TEXTFIELDWITHUNIT:
                textFieldPart.selectAll();
                break;
            //case TEXTFIELDWITHBUTTON:
            //    textFieldPart.selectAll();
            //    break;
            default:
                break;
        }
    }

    /** Sets the width of the widget. */
    public void setWidth(final int newWidth) {
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

    /** Returns its component. */
    JComponent getJComponent() {
        return component;
    }

    /** Sets background color. */
    public void setBackgroundColor(final Color bg) {
        JComponent c;
        if (fieldButton == null) {
            c = component;
        } else {
            c = componentPart;
        }
        final JComponent comp = c;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                setBackground(bg);
                switch(type) {
                    case LABELFIELD:
                        comp.setBackground(bg);
                        break;
                    case TEXTFIELD:
                        comp.setBackground(bg);
                        break;
                    case PASSWDFIELD:
                        comp.setBackground(bg);
                        break;
                    case COMBOBOX:
                        break;
                    case RADIOGROUP:
                        comp.setBackground(bg);
                        mComponentsReadLock.lock();
                        for (final JComponent c : componentsHash.values()) {
                            c.setBackground(bg);
                        }
                        mComponentsReadLock.unlock();
                        break;
                    case CHECKBOX:
                        comp.setBackground(bg);
                        break;
                    case TEXTFIELDWITHUNIT:
                        textFieldPart.setBackground(bg);
                        break;
                    //case TEXTFIELDWITHBUTTON:
                    //    textFieldPart.setBackground(bg);
                    //    break;
                    default:
                        /* error */
                }
            }
        });
    }

    /** Sets label for this component. */
    public void setLabel(final JLabel label,
                         final String labelToolTipText) {
        this.label = label;
        this.labelToolTipText = labelToolTipText;
    }

    /** Returns label for this component. */
    public JLabel getLabel() {
        return label;
    }

    /** Returns type of this widget. */
    public Type getType() {
        return type;
    }

    /** Sets this item enabled and visible according to its access type. */
    public void processAccessMode() {
        final boolean accessible =
                       Tools.getConfigData().isAccessible(enableAccessMode);
        setComponentsEnabled(enablePredicate && accessible);
        if (toolTipText != null) {
            setToolTipText(toolTipText);
        }
        if (label != null) {
            if (labelToolTipText != null) {
                setLabelToolTipText(labelToolTipText);
            }
            label.setEnabled(enablePredicate && accessible);
        }
    }

    /** Returns item at the specified index. */
    Object getItemAt(final int i) {
        if (type == Type.COMBOBOX) {
            return ((JComboBox) component).getItemAt(i);
        } else {
            return component;
        }
    }

    /** Cleanup whatever would cause a leak. */
    public void cleanup() {
        JComponent comp;
        if (fieldButton == null) {
            comp = component;
        } else {
            comp = componentPart;
        }
        switch(type) {
            case TEXTFIELD:
                final AbstractDocument d = (AbstractDocument) getDocument();
                for (final DocumentListener dl : d.getDocumentListeners()) {
                    d.removeDocumentListener(dl);
                }
                break;
            case PASSWDFIELD:
                final AbstractDocument dp = (AbstractDocument) getDocument();
                for (final DocumentListener dl : dp.getDocumentListeners()) {
                    dp.removeDocumentListener(dl);
                }
                break;
            case COMBOBOX:
                final JComboBox thisCB = ((JComboBox) comp);
                final AbstractDocument dc = (AbstractDocument) getDocument();
                for (final DocumentListener dl : dc.getDocumentListeners()) {
                    dc.removeDocumentListener(dl);
                }
                for (final ItemListener il : thisCB.getItemListeners()) {
                    thisCB.removeItemListener(il);
                }
                break;
            case RADIOGROUP:
                mComponentsReadLock.lock();
                for (final JComponent c : componentsHash.values()) {
                    for (final ItemListener il
                                    : ((JRadioButton) c).getItemListeners()) {
                        ((JRadioButton) c).removeItemListener(il);
                    }
                }
                mComponentsReadLock.unlock();
                break;
            case CHECKBOX:
                for (final ItemListener il
                                    : ((JCheckBox) comp).getItemListeners()) {
                    ((JCheckBox) comp).removeItemListener(il);
                }
                break;
            case TEXTFIELDWITHUNIT:
                final AbstractDocument dtfp =
                                (AbstractDocument) textFieldPart.getDocument();
                for (final DocumentListener dl : dtfp.getDocumentListeners()) {
                    dtfp.removeDocumentListener(dl);
                }
                for (final ItemListener il : unitComboBox.getItemListeners()) {
                    unitComboBox.removeItemListener(il);
                }
                break;
            default:
                /* error */
        }
    }

    /** Returns regexp of this field. */
    public String getRegexp() {
        return regexp;
    }

    /** Sets reason why it is disabled. */
    public void setDisabledReason(final String disabledReason) {
        this.disabledReason = disabledReason;
    }
}
