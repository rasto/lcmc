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

import lcmc.utilities.PatternDocument;
import lcmc.data.AccessMode;
import lcmc.utilities.MyButton;

import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.text.Document;

/**
 * An implementation of a field where user can enter new value. The
 * field can be Textfield or combo box, depending if there are values
 * too choose from.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Passwdfield extends Textfield {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Prepares a new <code>Passwdfield</code> object. */
    public Passwdfield(final String selectedValue,
                       final String regexp,
                       final int width,
                       final AccessMode enableAccessMode,
                       final MyButton fieldButton) {
        super(selectedValue,
              regexp,
              width,
              NO_ABBRV,
              enableAccessMode,
              fieldButton);
        addComponent(getPasswdField(selectedValue, regexp), width);
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

    /** Return value, that user have chosen in the field or typed in. */
    @Override
    protected Object getValueInternal() {
        final Object value =
                 new String(((JPasswordField) getComponent()).getPassword());
        if (NOTHING_SELECTED_DISPLAY.equals(value)) {
            return null;
        }
        return value;
    }

    /** Sets item/value in the component and waits till it is set. */
    @Override
    protected void setValueAndWait0(final Object item) {
        ((JPasswordField) getComponent()).setText((String) item);
    }

    /** Returns document object of the component. */
    @Override
    public Document getDocument() {
        return ((JPasswordField) getComponent()).getDocument();
    }

    /** Requests focus if applicable. */
    @Override
    public void requestFocus() {
        ((JPasswordField) getComponent()).requestFocus();
    }

    /** Selects the whole text in the widget if applicable. */
    @Override
    void selectAll() {
        ((JPasswordField) getComponent()).selectAll();
    }
}
