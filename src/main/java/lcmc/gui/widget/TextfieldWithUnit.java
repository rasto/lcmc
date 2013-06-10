/*
 * This file is part of LCMC
 *
 * Copyright (C) 2012, Rastislav Levrinc.
 *
 * LCMC is free software; you can redistribute it and/or
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

package lcmc.gui.widget;

import lcmc.utilities.Tools;
import lcmc.utilities.Unit;
import lcmc.utilities.PatternDocument;
import lcmc.data.AccessMode;
import lcmc.gui.resources.Info;
import lcmc.gui.SpringUtilities;
import lcmc.utilities.MyButton;
import lcmc.utilities.WidgetListener;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import javax.swing.text.Document;
import javax.swing.text.AbstractDocument;
import javax.swing.SpringLayout;
import javax.swing.event.DocumentListener;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemListener;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * An implementation of a field where user can enter new value. The
 * field can be Textfield or combo box, depending if there are values
 * too choose from.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class TextfieldWithUnit extends Widget {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Array of unit objects. */
    private final Unit[] units;
    /** Text field in widget with units. */
    private final JTextField textFieldPart;
    /** Combo box with units. */
    private final MComboBox<Object> unitComboBox;
    /** Pattern that matches value and unit. */
    private final Pattern unitPattern = Pattern.compile("^(\\d+)(\\D*)$");
    /** Whether the unit combo box should be enabled. */
    private boolean unitEnabled = true;

    /** Prepares a new <code>TextfieldWithUnit</code> object. */
    public TextfieldWithUnit(final String selectedValue,
                             final Unit[] units,
                             final String regexp,
                             final int width,
                             final Map<String, String> abbreviations,
                             final AccessMode enableAccessMode,
                             final MyButton fieldButton) {
        super(regexp,
              enableAccessMode,
              fieldButton);
        this.units = units;
        final JPanel newComp = new JPanel();
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
        unitComboBox = getComboBox(unit, units, regexp, abbreviations);

        newComp.add(unitComboBox);
        SpringUtilities.makeCompactGrid(newComp, 1, 2,
                                                 0, 0,
                                                 0, 0);
        addComponent(newComp, width);
        textFieldPart.setPreferredSize(new Dimension(width / 3, WIDGET_HEIGHT));
        textFieldPart.setMinimumSize(textFieldPart.getPreferredSize());
        textFieldPart.setMaximumSize(textFieldPart.getPreferredSize());

        unitComboBox.setPreferredSize(new Dimension(width / 3 * 2,
                                                    WIDGET_HEIGHT));
        unitComboBox.setMinimumSize(unitComboBox.getPreferredSize());
        unitComboBox.setMaximumSize(unitComboBox.getPreferredSize());
    }

    /** Return new MTextField with default value. */
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
    private MComboBox<Object> getComboBox(
                                  final String selectedValue,
                                  final Object[] items,
                                  final String regexp,
                                  final Map<String, String> abbreviations) {
        final List<Object> comboList = new ArrayList<Object>();

        final Object selectedValueInfo = ComboBox.addItems(comboList,
                                                           selectedValue,
                                                           items);
        final MComboBox<Object> cb = new MComboBox<Object>(comboList.toArray(
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
            @Override
            public void focusGained(final FocusEvent e) {
                Object o = getValue();
                if (o != null && !Tools.isStringClass(o)
                    && ((Info) o).getInternalValue() == null) {
                    o = null;
                }
                if (o == null) {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            editor.setText("");
                        }
                    });
                }
            }

            @Override
            public void focusLost(final FocusEvent e) {
                /* do nothing */
            }
        });
        return cb;
    }

    /** Set the tooltip text. */
    @Override
    public void setToolTipText(final String text) {
        super.setToolTipText(text);
        textFieldPart.setToolTipText("<html>" + text + "</html>");
        unitComboBox.setToolTipText("<html>" + text + "</html>");
    }

    /**
     * Return string value. If object value is null, return empty string (not
     * null).
     */
    @Override
    public String getStringValue() {
        final Object o = getValue();
        if (o == null) {
            return "";
        }
        final Object o0 = ((Object[]) o)[0];
        final Object o1 = ((Object[]) o)[1];
        String v = o0.toString();
        if (v != null && !"".equals(v) && o1 != null
            && o1 instanceof Unit) {
            v += ((Unit) o1).getShortName();
        }
        return v;
    }

    /** Return value, that user have chosen in the field or typed in. */
    @Override
    protected Object getValueInternal() {
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
                     Tools.getConfigData().isAccessible(getEnableAccessMode());
            if ("".equals(text)) {
                if (!u.isEmpty()) {
                    u.setEmpty(true);
                    unitEnabled = false;
                    Tools.invokeLater(!Tools.CHECK_SWING_THREAD,
                                      new Runnable() {
                        @Override
                        public void run() {
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
                        Tools.invokeLater(!Tools.CHECK_SWING_THREAD,
                                          new Runnable() {
                            @Override
                            public void run() {
                                unitComboBox.repaint();
                                unitComboBox.setEnabled(accessible);
                            }
                        });
                    }
                }
            }
        }
        final Object value = new Object[]{text, unit};

        if (NOTHING_SELECTED_DISPLAY.equals(value)) {
            return null;
        }
        return value;
    }

    /** Set component visible or invisible. */
    @Override
    protected void setComponentsVisible(final boolean visible) {
        super.setComponentsEnabled(visible);
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                textFieldPart.setVisible(visible);
                unitComboBox.setVisible(visible);
                repaint();
            }
        });
    }

    /** Set component enabled or disabled. */
    @Override
    protected void setComponentsEnabled(final boolean enabled) {
        super.setComponentsEnabled(enabled);
        textFieldPart.setEnabled(enabled);
        unitComboBox.setEnabled(enabled && unitEnabled);
    }


    /** Return whether component is editable or not. */
    @Override
    boolean isEditable() {
        return false;
    }

    /** Set item/value in the component and waits till it is set. */
    @Override
    protected void setValueAndWait0(final Object item) {
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
    }

    /** Return document object of the component. */
    @Override
    public Document getDocument() {
        return null;
    }

    /** Add item listener to the component. */
    @Override
    public void addListeners(final WidgetListener wl) {
        super.addListeners(wl);
        addDocumentListener(textFieldPart.getDocument(), wl);
        unitComboBox.addItemListener(getItemListener(wl));
    }

    @Override
    protected void setComponentBackground(final Color backgroundColor,
                                          final Color compColor) {
        textFieldPart.setBackground(Color.WHITE);
    }

    /** Request focus if applicable. */
    @Override
    public void requestFocus() {
        textFieldPart.requestFocus();
    }

    /** Select the whole text in the widget if applicable. */
    @Override
    void selectAll() {
        textFieldPart.selectAll();
    }

    /** Set background color. */
    @Override
    public void setBackgroundColor(final Color bg) {
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                setBackground(bg);
                textFieldPart.setBackground(bg);
            }
        });
    }

    /** Return item at the specified index. */
    @Override
    Object getItemAt(final int i) {
        return getComponent();
    }

    /** Cleanup whatever would cause a leak. */
    @Override
    public void cleanup() {
        getWidgetListeners().clear();
        final AbstractDocument dtfp =
                                (AbstractDocument) textFieldPart.getDocument();
        for (final DocumentListener dl : dtfp.getDocumentListeners()) {
            dtfp.removeDocumentListener(dl);
        }
        for (final ItemListener il : unitComboBox.getItemListeners()) {
            unitComboBox.removeItemListener(il);
        }
    }
}
