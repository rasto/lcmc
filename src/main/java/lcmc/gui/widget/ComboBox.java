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

import lcmc.data.StringValue;
import lcmc.data.Value;
import lcmc.utilities.Tools;
import lcmc.utilities.PatternDocument;
import lcmc.data.AccessMode;
import lcmc.utilities.MyButton;
import lcmc.utilities.WidgetListener;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;
import javax.swing.text.Document;
import javax.swing.text.AbstractDocument;
import javax.swing.event.DocumentListener;
import javax.swing.ComboBoxEditor;

import java.awt.Color;
import java.awt.event.ItemListener;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * An implementation of a field where user can enter new value. The
 * field can be Textfield or combo box, depending if there are values
 * too choose from.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
//TODO: public final class ComboBox<E> extends Widget {
public final class ComboBox extends GenericWidget<MComboBox<Value>> {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Scrollbar max rows. */
    private static final int CB_SCROLLBAR_MAX_ROWS = 10;

    /** Prepares a new <code>ComboBox</code> object. */
    public ComboBox(final Value selectedValue,
                    final Value[] items,
                    final String regexp,
                    final int width,
                    final Map<String, String> abbreviations,
                    final AccessMode enableAccessMode,
                    final MyButton fieldButton) {
        super(regexp,
              enableAccessMode,
              fieldButton);
        addComponent(getComboBox(selectedValue,
                                 items,
                                 regexp,
                                 abbreviations),
                     width);
    }

    /** Returns combo box with items in the combo and selectedValue on top. */
    private MComboBox<Value> getComboBox(
                                  final Value selectedValue,
                                  final Value[] items,
                                  final String regexp,
                                  final Map<String, String> abbreviations) {
        final List<Value> comboList = new ArrayList<Value>();

        final Value selectedValueInfo = addItems(comboList,
                                                  selectedValue,
                                                  items);
        final MComboBox<Value> cb = new MComboBox<Value>(comboList.toArray(
                                            new Value[comboList.size()]));
        final JTextComponent editor =
                        (JTextComponent) cb.getEditor().getEditorComponent();
        if (regexp != null) {
            editor.setDocument(new PatternDocument(regexp, abbreviations));
        }
        cb.setMaximumRowCount(CB_SCROLLBAR_MAX_ROWS);
        if (selectedValueInfo != null) {
            cb.setSelectedItem(selectedValueInfo);
        }
        /* workround, so that default button works */
        editor.addKeyListener(new ActivateDefaultButtonListener<Value>(cb));

        /* removing select... keyword */
        editor.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent e) {
                Value v = getValue();
                if (v == null || v.isNothingSelected()) {
                    editor.setText("");
                }
            }

            @Override
            public void focusLost(final FocusEvent e) {
                /* do nothing */
            }
        });
        return cb;
    }

    /** Returns true if combo box has changed. */
    private boolean comboBoxChanged(final Value[] items) {
        final MComboBox<Value> cb = getInternalComponent();
        if (items.length != cb.getItemCount()) {
            return true;
        }

        for (int i = 0; i < items.length; i++) {
            if (!Tools.areEqual(items[i], cb.getItemAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** Reloads combo box with items and selects supplied value. */
    @Override
    public void reloadComboBox(final Value selectedValue,
                               final Value[] items) {
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                final MComboBox<Value> cb = getInternalComponent();
                final Value selectedItem = (Value) cb.getSelectedItem();
                boolean selectedChanged = false;
                if (selectedValue == null
                    && (selectedItem != null
                         && !selectedItem.isNothingSelected())) {
                    selectedChanged = true;
                } else if (selectedValue != null
                           && !selectedValue.equals(selectedItem)) {
                    selectedChanged = true;
                }
                final boolean itemsChanged = comboBoxChanged(items);
                if (!selectedChanged && !itemsChanged) {
                    return;
                }

                cb.setPreferredSize(null);
                /* removing dupicates */

                final List<Value> comboList = new ArrayList<Value>();
                final Value selectedValueInfo = addItems(comboList,
                                                          selectedValue,
                                                          items);

                if (itemsChanged) {
                    final HashSet<String> itemCache = new HashSet<String>();
                    cb.setSelectedIndex(-1);
                    cb.removeAllItems();
                    for (final Value item : comboList) {
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

    /** Add items to the combo box. */
    protected static Value addItems(final List<Value> comboList,
                                     final Value selectedValue,
                                     final Value[] items) {
        Value selectedValueInfo = null;
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                if (Tools.areEqual(items[i], selectedValue)) {
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

    /** Set combo box editable. */
    @Override
    public void setEditable(final boolean editable) {
        super.setEditable(editable);
        final JComponent comp = getInternalComponent();
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                Value v = getValue();
                if (isAlwaysEditable()) {
                    ((MComboBox) comp).setEditable(true);
                    final JTextComponent editor = getTextComponent();
                    if (v.isNothingSelected()) {
                        editor.selectAll();
                    }
                } else {
                    if (v == null || v.isNothingSelected()) {
                        ((MComboBox) comp).setEditable(false);
                    } else {
                        ((MComboBox) comp).setEditable(editable);
                    }
                }
            }
        });
    }

    /**
     * Returns string value. If object value is null, returns empty string (not
     * null).
     */
    @Override
    public String getStringValue() {
        final Value v = getValue();
        if (v == null) {
            return "";
        }
        return v.getValueForConfig();
    }

    /** Return value, that user have chosen in the field or typed in. */
    @Override
    protected Value getValueInternal() {
        final MComboBox<Value> cb = getInternalComponent();
        Value value;
        if (cb.isEditable()) {
            final JTextComponent editor =
                        (JTextComponent) cb.getEditor().getEditorComponent();
            String text = editor.getText();
            if (text == null) {
                text = "";
            }
            value = (Value) cb.getSelectedItem();
            if (value == null || !text.equals(value.getValueForConfig())) {
                value = new StringValue(text);
            }

            if (value.isNothingSelected()) {
                return value;
            }
        } else {
            value = (Value) cb.getSelectedItem();
        }
        if (value.isNothingSelected()) {
            return null;
        }
        return value;
    }

    /** Clears the combo box. */
    @Override
    public void clear() {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                ((MComboBox) getInternalComponent()).removeAllItems();
            }
        });
    }

    /** Returns whether component is editable or not. */
    @Override
    boolean isEditable() {
        return ((MComboBox) getInternalComponent()).isEditable();
    }

    /** Set item/value in the component and waits till it is set. */
    @Override
    protected void setValueAndWait0(final Value item) {
        Tools.isSwingThread();
        final MComboBox<Value> cb = getInternalComponent();
        cb.setSelectedItem(item);
        //if (Tools.isStringClass(item)) {
        //    Value selectedObject = null;
        //    for (int i = 0; i < cb.getItemCount(); i++) {
        //        final Value it = cb.getItemAt(i);
        //        if (it.equals(item)) {
        //            selectedObject = it;
        //            cb.setSelectedItem(it);
        //            break;
        //        }
        //    }
        //    if (selectedObject == null) {
        //        cb.addItem(item);
        //        cb.setSelectedItem(item);
        //    }
        //}
    }

    /** Set selected index. */
    @Override
    public void setSelectedIndex(final int index) {
        final MComboBox<Value> cb = getInternalComponent();
        cb.setSelectedIndex(index);
    }

    /** Returns document object of the component. */
    @Override
    public Document getDocument() {
        final JTextComponent tc = getTextComponent();
        return tc.getDocument();
    }

    /** Selects part after first '*' in the ip. */
    @Override
    public void selectSubnet() {
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
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    tc.select(pos, ip.length());
                }
            });
        }
    }

    /** Add item listener to the component. */
    @Override
    public void addListeners(final WidgetListener wl) {
        getWidgetListeners().add(wl);
        addDocumentListener(getDocument(), wl);
        ((MComboBox) getInternalComponent()).addItemListener(getItemListener(wl));
    }

    @Override
    protected void setComponentBackground(final Color backgroundColor,
                                          final Color compColor) {
        setBackground(Color.WHITE);
    }

    /** Set background color. */
    @Override
    public void setBackgroundColor(final Color bg) {
        final JComponent comp = getInternalComponent();
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                setBackground(bg);
            }
        });
    }

    /** Returns item at the specified index. */
    //@Override
    //Value getItemAt(final int i) {
    //    return (Value) ((MComboBox) getComponent()).getItemAt(i);
    //}

    /** Cleanup whatever would cause a leak. */
    @Override
    public void cleanup() {
        getWidgetListeners().clear();
        final MComboBox<Value> thisCB = getInternalComponent();
        final AbstractDocument dc = (AbstractDocument) getDocument();
        for (final DocumentListener dl : dc.getDocumentListeners()) {
            dc.removeDocumentListener(dl);
        }
        for (final ItemListener il : thisCB.getItemListeners()) {
            thisCB.removeItemListener(il);
        }
    }

    /** Returns the text component of the combo box. */
    private JTextComponent getTextComponent() {
        final JComponent comp = getInternalComponent();
        final ComboBoxEditor editor = ((MComboBox) comp).getEditor();
        return (JTextComponent) editor.getEditorComponent();
    }

    /** Select the text component. */
    @Override
    public void select(final int selectionStart, final int selectionEnd) {
        getTextComponent().select(selectionStart, selectionEnd);
    }
}
