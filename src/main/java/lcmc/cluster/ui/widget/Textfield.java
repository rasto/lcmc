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
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JComponent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.PatternDocument;
import lcmc.common.ui.utils.WidgetListener;

/**
 * An implementation of a field where user can enter new value. The
 * field can be Textfield or combo box, depending if there are values
 * too choose from.
 */
@Named
public class Textfield extends GenericWidget<JComponent> {
    @Inject
    private Application application;

    public void init(final Value selectedValue,
                     final String regexp,
                     final int width,
                     final Map<String, String> abbreviations,
                     final AccessMode enableAccessMode,
                     final MyButton fieldButton) {
        super.init(regexp, enableAccessMode, fieldButton);
        addComponent(getTextField(selectedValue, regexp, abbreviations), width);
    }

    private JComponent getTextField(final Value value, final String regexp, final Map<String, String> abbreviations) {
        final String valueS;
        if (value == null) {
            valueS = null;
        } else {
            valueS = value.getValueForConfig();
        }

        final MTextField tf;
        if (regexp == null) {
            tf = new MTextField(valueS);
        } else {
            tf = new MTextField(new PatternDocument(regexp, abbreviations), valueS, 0);
        }
        return tf;
    }

    /**
     * Returns string value. If object value is null, returns empty string (not null).
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
        final Value value = new StringValue(((JTextComponent) getInternalComponent()).getText());
        if (value.isNothingSelected()) {
            return null;
        }
        return value;
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    protected void setValueAndWait0(final Value item) {
        if (item == null) {
            ((JTextComponent) getInternalComponent()).setText(null);
        } else {
            ((JTextComponent) getInternalComponent()).setText(item.getValueForConfig());
        }
    }

    @Override
    public Document getDocument() {
        return ((JTextComponent) getInternalComponent()).getDocument();
    }

    @Override
    public void addListeners(final WidgetListener widgetListener) {
        getWidgetListeners().add(widgetListener);
        addDocumentListener(getDocument(), widgetListener);
    }

    @Override
    protected void setComponentBackground(final Color backgroundColor, final Color compColor) {
        getInternalComponent().setBackground(compColor);
    }

    @Override
    public void requestFocus() {
        getInternalComponent().requestFocus();
    }

    @Override
    public void selectAll() {
        ((JTextComponent) getInternalComponent()).selectAll();
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
        final AbstractDocument d = (AbstractDocument) getDocument();
        for (final DocumentListener dl : d.getDocumentListeners()) {
            d.removeDocumentListener(dl);
        }
    }
}
