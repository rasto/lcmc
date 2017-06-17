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
package lcmc.cluster.ui.widget;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.ui.Access;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.PatternDocument;
import lcmc.common.domain.Unit;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.WidgetListener;

/**
 * An implementation of a field where user can enter new value. The
 * field can be Textfield or combo box, depending if there are values
 * too choose from.
 */
@Named
public final class TextfieldWithUnit extends GenericWidget<JComponent> {
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private Access access;
    /** Text field in widget with units. */
    private JTextField textFieldPart;
    private MComboBox<Unit> unitComboBox;
    private boolean unitEnabled = true;

    public void init(final Value selectedValue,
                     final Unit[] units,
                     final String regexp,
                     final int width,
                     final Map<String, String> abbreviations,
                     final AccessMode enableAccessMode,
                     final MyButton fieldButton) {
        super.init(regexp, enableAccessMode, fieldButton);
        final JPanel newComp = new JPanel();
        newComp.setLayout(new SpringLayout());

        final String number;
        final Unit unit;
        if (selectedValue == null) {
            number = null;
            unit = null;
        } else {
            number = selectedValue.getValueForConfig();
            unit = selectedValue.getUnit();
        }

        /* text field */
        textFieldPart = (JTextField) getTextField(number, regexp, abbreviations);
        newComp.add(textFieldPart);

        /* unit combo box */
        unitComboBox = getComboBox(unit, units, regexp, abbreviations);

        newComp.add(unitComboBox);
        SpringUtilities.makeCompactGrid(newComp, 1, 2, 0, 0, 0, 0);
        addComponent(newComp, width);
        textFieldPart.setPreferredSize(new Dimension(width / 3, WIDGET_HEIGHT));
        textFieldPart.setMinimumSize(textFieldPart.getPreferredSize());
        textFieldPart.setMaximumSize(textFieldPart.getPreferredSize());

        unitComboBox.setPreferredSize(new Dimension(width / 3 << 1, WIDGET_HEIGHT));
        unitComboBox.setMinimumSize(unitComboBox.getPreferredSize());
        unitComboBox.setMaximumSize(unitComboBox.getPreferredSize());
    }

    private Unit addItems(final Collection<Unit> comboList, final Unit selectedValue, final Unit[] items) {
        Unit selectedUnit = null;
        if (items != null) {
            for (final Unit item : items) {
                if (item.equals(selectedValue)) {
                    selectedUnit = item;
                }
                comboList.add(item);
            }
            if (selectedUnit == null && selectedValue != null) {
                comboList.add(selectedValue);
                selectedUnit = selectedValue;
            }
        }
        return selectedUnit;
    }

    private JComponent getTextField(final String value, final String regexp, final Map<String, String> abbreviations) {
        final MTextField tf;
        if (regexp == null) {
            tf = new MTextField(value);
        } else {
            tf = new MTextField(new PatternDocument(regexp, abbreviations), value, 0);
        }
        return tf;
    }

    private MComboBox<Unit> getComboBox(final Unit selectedValue,
                                        final Unit[] items,
                                        final String regexp,
                                        final Map<String, String> abbreviations) {
        final List<Unit> comboList = new ArrayList<Unit>();

        final Unit selectedValueInfo = addItems(comboList, selectedValue, items);
        final MComboBox<Unit> cb = new MComboBox<Unit>(comboList.toArray(new Unit[comboList.size()]));
        final JTextComponent editor = (JTextComponent) cb.getEditor().getEditorComponent();
        if (regexp != null) {
            editor.setDocument(new PatternDocument(regexp, abbreviations));
        }
        cb.setMaximumRowCount(SCROLLBAR_MAX_ROWS);
        if (selectedValueInfo != null) {
            cb.setSelectedItem(selectedValueInfo);
        }
        /* workround, so that default button works */
        editor.addKeyListener(new ActivateDefaultButtonListener<Unit>(cb));

        /* removing select... keyword */
        editor.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent e) {
                final Value v = getValue();
                if (v.isNothingSelected()) {
                    swingUtils.invokeLater(new Runnable() {
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
        final Value v = getValue();
        return v.getValueForConfigWithUnit();
    }

    /** Return value, that user have chosen in the field or typed in. */
    @Override
    protected Value getValueInternal() {
        final String text = textFieldPart.getText();
        final Unit unit = (Unit) unitComboBox.getSelectedItem();
        if (unit != null) {
            if (unit.isPlural() == "1".equals(text)) {
                unit.setPlural(!"1".equals(text));
                unitComboBox.repaint();
            }
            final boolean accessible = access.isAccessible(getEnableAccessMode());
            if (text == null || text.isEmpty()) {
                if (!unit.isEmpty()) {
                    unit.setEmpty(true);
                    unitEnabled = false;
                    swingUtils.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            unitComboBox.repaint();
                            unitComboBox.setEnabled(false);
                        }
                    });
                }
            } else {
                if (unit.isEmpty()) {
                    unit.setEmpty(false);
                    if (textFieldPart.isEnabled()) {
                        unitEnabled = true;
                        swingUtils.invokeLater(new Runnable() {
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
        if (text == null || text.isEmpty()) {
            return new StringValue();
        } else {
            return new StringValue(text, unit);
        }
    }

    @Override
    protected void setComponentsVisible(final boolean visible) {
        super.setComponentsEnabled(visible);
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                textFieldPart.setVisible(visible);
                unitComboBox.setVisible(visible);
                repaint();
            }
        });
    }

    @Override
    protected void setComponentsEnabled(final boolean enabled) {
        super.setComponentsEnabled(enabled);
        textFieldPart.setEnabled(enabled);
        unitComboBox.setEnabled(enabled && unitEnabled);
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    protected void setValueAndWait0(final Value item) {
        if (item == null) {
            textFieldPart.setText(null);
        } else {
            textFieldPart.setText(item.getValueForConfig());
            unitComboBox.setSelectedItem(item.getUnit());
        }
    }

    @Override
    public Document getDocument() {
        return null;
    }

    @Override
    public void addListeners(final WidgetListener widgetListener) {
        super.addListeners(widgetListener);
        addDocumentListener(textFieldPart.getDocument(), widgetListener);
        unitComboBox.addItemListener(getItemListener(widgetListener));
    }

    @Override
    protected void setComponentBackground(final Color backgroundColor, final Color compColor) {
        textFieldPart.setBackground(Color.WHITE);
    }

    @Override
    public void requestFocus() {
        textFieldPart.requestFocus();
    }

    @Override
    public void selectAll() {
        textFieldPart.selectAll();
    }

    @Override
    public void setBackgroundColor(final Color bg) {
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                setBackground(bg);
                textFieldPart.setBackground(bg);
            }
        });
    }

    /** Cleanup whatever would cause a leak. */
    @Override
    public void cleanup() {
        getWidgetListeners().clear();
        final AbstractDocument dtfp = (AbstractDocument) textFieldPart.getDocument();
        for (final DocumentListener dl : dtfp.getDocumentListeners()) {
            dtfp.removeDocumentListener(dl);
        }
        for (final ItemListener il : unitComboBox.getItemListeners()) {
            unitComboBox.removeItemListener(il);
        }
    }
}
