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

import java.awt.Color;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.Document;
import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.StringValue;
import lcmc.model.Value;
import lcmc.utilities.MyButton;
import lcmc.utilities.WidgetListener;

/**
 * An implementation of a field where user can enter new value. The
 * field can be Textfield or combo box, depending if there are values
 * too choose from.
 */
@Named
public final class Label extends GenericWidget<JComponent> {
    @Inject
    private Application application;

    public void init(final Value selectedValue,
                     final String regexp,
                     final int width,
                     final AccessMode enableAccessMode,
                     final MyButton fieldButton) {
        super.init(regexp, enableAccessMode, fieldButton);
        addComponent(getLabelField(selectedValue), width);
    }

    private JComponent getLabelField(final Value value) {
        if (value == null || value.isNothingSelected()) {
            return new JLabel("");
        }
        return new JLabel(value.getValueForGui());
    }

    /**
     * Returns string value. If object value is null, returns empty string (not null).
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

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    protected void setValueAndWait0(final Value item) {
        ((JLabel) getInternalComponent()).setText(item.getValueForGui());
    }

    @Override
    public Document getDocument() {
        return null;
    }

    @Override
    public void addListeners(final WidgetListener widgetListener) {
        getWidgetListeners().add(widgetListener);
    }

    @Override
    protected void setComponentBackground(final Color backgroundColor, final Color compColor) {
        getInternalComponent().setBackground(backgroundColor);
    }

    @Override
    public void setBackgroundColor(final Color bg) {
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                setBackground(bg);
                getInternalComponent().setBackground(bg);
            }
        });
    }

    /** Cleanup whatever would cause a leak. */
    @Override
    public void cleanup() {
        getWidgetListeners().clear();
    }
}
