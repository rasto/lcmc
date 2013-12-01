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
import lcmc.utilities.PatternDocument;
import lcmc.data.AccessMode;
import lcmc.utilities.MyButton;
import lcmc.utilities.WidgetListener;
import lcmc.utilities.Tools;

import javax.swing.JComponent;
import javax.swing.text.Document;
import javax.swing.text.AbstractDocument;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.util.Map;

/**
 * An implementation of a field where user can enter new value. The
 * field can be Textfield or combo box, depending if there are values
 * too choose from.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class Textfield extends GenericWidget<JComponent> {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Prepares a new <code>Textfield</code> object. */
    public Textfield(final Value selectedValue,
                     final String regexp,
                     final int width,
                     final Map<String, String> abbreviations,
                     final AccessMode enableAccessMode,
                     final MyButton fieldButton) {
        super(regexp,
              enableAccessMode,
              fieldButton);
        addComponent(getTextField(selectedValue, regexp, abbreviations), width);
    }

    /** Returns new MTextField with default value. */
    private JComponent getTextField(final Value value,
                                    final String regexp,
                                    final Map<String, String> abbreviations) {
        MTextField tf;
        if (regexp == null) {
            if (value == null) {
                tf = new MTextField(null);
            } else {
                tf = new MTextField(value.getValueForConfig());
            }
        } else {
            tf = new MTextField(new PatternDocument(regexp, abbreviations),
                                value.getValueForConfig(),
                                0);
        }
        return tf;
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
        final Value value = new StringValue(((MTextField) getInternalComponent()).getText());
        if (value.isNothingSelected()) {
            return null;
        }
        return value;
    }

    /** Returns whether component is editable or not. */
    @Override
    public boolean isEditable() {
        return true;
    }

    /** Sets item/value in the component and waits till it is set. */
    @Override
    protected void setValueAndWait0(final Value item) {
        if (item == null) {
            ((MTextField) getInternalComponent()).setText(null);
        } else {
            ((MTextField) getInternalComponent()).setText(item.getValueForConfig());
        }
    }

    /** Returns document object of the component. */
    @Override
    public Document getDocument() {
        return ((MTextField) getInternalComponent()).getDocument();
    }

    /** Adds item listener to the component. */
    @Override
    public void addListeners(final WidgetListener wl) {
        getWidgetListeners().add(wl);
        addDocumentListener(getDocument(), wl);
    }

    @Override
    protected void setComponentBackground(final Color backgroundColor,
                                          final Color compColor) {
        getInternalComponent().setBackground(compColor);
    }

    /** Requests focus if applicable. */
    @Override
    public void requestFocus() {
        ((MTextField) getInternalComponent()).requestFocus();
    }

    /** Selects the whole text in the widget if applicable. */
    @Override
    void selectAll() {
        ((MTextField) getInternalComponent()).selectAll();
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
        final AbstractDocument d = (AbstractDocument) getDocument();
        for (final DocumentListener dl : d.getDocumentListeners()) {
            d.removeDocumentListener(dl);
        }
    }
}
