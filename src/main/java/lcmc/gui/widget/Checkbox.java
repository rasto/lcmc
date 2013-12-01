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
import lcmc.data.AccessMode;
import lcmc.utilities.MyButton;
import lcmc.utilities.WidgetListener;

import javax.swing.JComponent;
import javax.swing.text.Document;
import javax.swing.JCheckBox;


import java.awt.Color;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

/**
 * An implementation of a field where user can enter new value. The
 * field can be Textfield or combo box, depending if there are values
 * too choose from.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class Checkbox extends GenericWidget<JComponent> {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    static final String CHECKBOX_TRUE = "True";
    static final String CHECKBOX_FALSE = "False";
    /** Name for the 'true' value. */
    private Value checkBoxTrue = new StringValue(CHECKBOX_TRUE);
    /** Name for the 'false' value. */
    private Value checkBoxFalse = new StringValue(CHECKBOX_FALSE);

    /** Prepares a new <code>Checkbox</code> object. */
    public Checkbox(final Value selectedValue,
                    final Value[] items,
                    final String regexp,
                    final int width,
                    final AccessMode enableAccessMode,
                    final MyButton fieldButton) {
        super(regexp,
              enableAccessMode,
              fieldButton);
        if (items != null && items.length == 2) {
            checkBoxTrue  = items[0];
            checkBoxFalse = items[1];
        }
        addComponent(getCheckBox(selectedValue), width);
    }

    /** Returns check box for boolean values. */
    private JComponent getCheckBox(final Value selectedValue) {
        final JCheckBox cb = new JCheckBox();
        if (selectedValue != null) {
            cb.setSelected(selectedValue.equals(checkBoxTrue));
        }
        return cb;
    }

    ///** Returns whether this field is a check box. */
    //@Override
    //public boolean isCheckBox() {
    //    return true;
    //}

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
        Value value;
        final JCheckBox cbox = (JCheckBox) getInternalComponent();
        if (cbox.getSelectedObjects() == null) {
            value = checkBoxFalse;
        } else {
            value = checkBoxTrue;
        }
        if (value.isNothingSelected()) {
            return null;
        }
        return value;
    }

    /** Returns whether component is editable or not. */
    @Override
    public boolean isEditable() {
        return false;
    }

    /** Sets item/value in the component and waits till it is set. */
    @Override
    protected void setValueAndWait0(final Value item) {
        if (item != null) {
            ((JCheckBox) getInternalComponent()).setSelected(item.equals(checkBoxTrue));
        }
    }

    /** Returns document object of the component. */
    @Override
    public Document getDocument() {
        return null;
    }

    /** Adds item listener to the component. */
    @Override
    public void addListeners(final WidgetListener wl) {
        getWidgetListeners().add(wl);
        ((JCheckBox) getInternalComponent()).addItemListener(getItemListener(wl));
    }

    @Override
    protected void setComponentBackground(final Color backgroundColor,
                                          final Color compColor) {
        getInternalComponent().setBackground(backgroundColor);
    }

    /** Sets background color. */
    @Override
    public void setBackgroundColor(final Color bg) {
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                setBackground(bg);
                getInternalComponent().setBackground(bg);
            }
        });
    }

    /** Returns item at the specified index. */
    //@Override
    //Value getItemAt(final int i) {
    //    return getComponent();
    //}

    /** Cleanup whatever would cause a leak. */
    @Override
    public void cleanup() {
        getWidgetListeners().clear();
        for (final ItemListener il
                            : ((JCheckBox) getInternalComponent()).getItemListeners()) {
            ((JCheckBox) getInternalComponent()).removeItemListener(il);
        }
    }

    @Override
    protected ItemListener getItemListener(final WidgetListener wl) {
        return new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (wl.isEnabled()) {
                    Value v;
                    if (((JCheckBox) e.getItem()).isSelected()) {
                        v = checkBoxTrue;
                    } else {
                        v = checkBoxFalse;
                    }
                    final Value value = v;
                    final Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            wl.check(value);
                        }
                    });
                    t.start();
                }
            }
        };
    }
}
