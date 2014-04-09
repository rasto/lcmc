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
import lcmc.data.AccessMode;
import lcmc.utilities.MyButton;
import lcmc.utilities.WidgetListener;
import lcmc.utilities.Tools;

import javax.swing.JComponent;
import javax.swing.text.Document;
import javax.swing.JLabel;

import java.awt.Color;

/**
 * An implementation of a field where user can enter new value. The
 * field can be Textfield or combo box, depending if there are values
 * too choose from.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Label extends GenericWidget<JComponent> {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Prepares a new {@code Label} object. */
    public Label(final Value selectedValue,
                 final String regexp,
                 final int width,
                 final AccessMode enableAccessMode,
                 final MyButton fieldButton) {
        super(regexp,
              enableAccessMode,
              fieldButton);
        addComponent(getLabelField(selectedValue), width);
    }

    /** Returns new MTextField with default value. */
    private JComponent getLabelField(final Value value) {
        if (value == null || value.isNothingSelected()) {
            return new JLabel("");
        }
        return new JLabel(value.getValueForGui());
    }

    /**
     * Returns string value. If object value is null, returns empty string (not
     * null).
     */
    @Override
    public String getStringValue() {
        final Object o = getValue();
        if (o == null) {
            return "";
        }
        return o.toString();
    }

    /** Return value, that user have chosen in the field or typed in. */
    @Override
    protected Value getValueInternal() {
        return new StringValue(((JLabel) getInternalComponent()).getText());
    }

    /** Returns whether component is editable or not. */
    @Override
    public boolean isEditable() {
        return false;
    }

    /** Sets item/value in the component and waits till it is set. */
    @Override
    protected void setValueAndWait0(final Value item) {
        ((JLabel) getInternalComponent()).setText(item.getValueForGui());
    }

    /** Returns document object of the component. */
    @Override
    public Document getDocument() {
        return null;
    }

    /** Adds item listener to the component. */
    @Override
    public void addListeners(final WidgetListener widgetListener) {
        getWidgetListeners().add(widgetListener);
    }

    @Override
    protected void setComponentBackground(final Color backgroundColor,
                                          final Color compColor) {
        getInternalComponent().setBackground(backgroundColor);
    }

    /** Sets background color. */
    @Override
    public void setBackgroundColor(final Color bg) {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                setBackground(bg);
                getInternalComponent().setBackground(bg);
            }
        });
    }

    /** Returns item at the specified index. */
    //@Override
    //Component getItemAt(final int i) {
    //    return getComponent();
    //}

    /** Cleanup whatever would cause a leak. */
    @Override
    public void cleanup() {
        getWidgetListeners().clear();
    }
}
