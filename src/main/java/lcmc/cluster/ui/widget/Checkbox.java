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
import java.awt.ItemSelectable;
import java.awt.event.ItemListener;

import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.text.Document;

import lcmc.common.domain.AccessMode;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.WidgetListener;

/**
 * An implementation of a field where user can enter new value. The
 * field can be Textfield or combo box, depending if there are values
 * too choose from.
 */
@Named
public class Checkbox extends GenericWidget<JComponent> {
    static final String CHECKBOX_TRUE = "True";
    static final String CHECKBOX_FALSE = "False";
    /** Name for the 'true' value. */
    private Value checkBoxTrue = new StringValue(CHECKBOX_TRUE);
    /** Name for the 'false' value. */
    private Value checkBoxFalse = new StringValue(CHECKBOX_FALSE);
    @Inject
    private SwingUtils swingUtils;

    public void init(final Value selectedValue,
                    final Value[] items,
                    final String regexp,
                    final int width,
                    final AccessMode enableAccessMode,
                    final MyButton fieldButton) {
        super.init(regexp, enableAccessMode, fieldButton);
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
        final Value value;
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

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    protected void setValueAndWait0(final Value item) {
        if (item != null) {
            ((AbstractButton) getInternalComponent()).setSelected(item.equals(checkBoxTrue));
        }
    }

    @Override
    public Document getDocument() {
        return null;
    }

    @Override
    public void addListeners(final WidgetListener wl) {
        getWidgetListeners().add(wl);
        ((ItemSelectable) getInternalComponent()).addItemListener(getItemListener(wl));
    }

    @Override
    protected void setComponentBackground(final Color backgroundColor, final Color compColor) {
        getInternalComponent().setBackground(backgroundColor);
    }

    @Override
    public void setBackgroundColor(final Color bg) {
        swingUtils.invokeLater(() -> {
            setBackground(bg);
            getInternalComponent().setBackground(bg);
        });
    }

    /** Cleanup whatever would cause a leak. */
    @Override
    public void cleanup() {
        getWidgetListeners().clear();
        for (final ItemListener il : ((AbstractButton) getInternalComponent()).getItemListeners()) {
            ((ItemSelectable) getInternalComponent()).removeItemListener(il);
        }
    }

    @Override
    protected ItemListener getItemListener(final WidgetListener wl) {
        return e -> {
            if (wl.isEnabled()) {
                final Value value;
                if (((AbstractButton) e.getItem()).isSelected()) {
                    value = checkBoxTrue;
                } else {
                    value = checkBoxFalse;
                }
                final Thread t = new Thread(() -> wl.check(value));
                t.start();
            }
        };
    }
}
