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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.Icon;

import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Unit;
import lcmc.common.domain.Value;
import lcmc.common.ui.utils.MyButton;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

@Named
@Singleton
public final class WidgetFactory {
    private final Logger LOG = LoggerFactory.getLogger(WidgetFactory.class);
    @Inject
    private Provider<Label> labelProvider;
    @Inject
    private Provider<ComboBox> comboBoxProvider;
    @Inject @Named("passwdfield")
    private Provider<Passwdfield> passwdFieldProvider;
    @Inject @Named("textfield")
    private Provider<Textfield> textfieldInstance;
    @Inject
    private Provider<TextfieldWithUnit> textFieldWithUnitProvider;
    @Inject
    private Provider<RadioGroup> radioGroupProvider;
    @Inject
    private Provider<Checkbox> checkboxProvider;

    /** Without units. */
    public Widget createInstance(final Widget.Type type,
                                 final Value selectedValue,
                                 final Value[] items,
                                 final String regexp,
                                 final int width,
                                 final Map<String, String> abbreviations,
                                 final AccessMode enableAccessMode,
                                 final MyButton fieldButton) {
        return createInstance(type,
                              selectedValue,
                              items,
                              null,
                              regexp,
                              width,
                              abbreviations,
                              enableAccessMode,
                              fieldButton);
    }

    public Widget createInstance(Widget.Type type,
                                 final Value selectedValue,
                                 final Value[] items,
                                 final Unit[] units,
                                 final String regexp,
                                 final int width,
                                 final Map<String, String> abbreviations,
                                 final AccessMode enableAccessMode,
                                 final MyButton fieldButton) {
        if (type != null && type != Widget.Type.TEXTFIELDWITHUNIT && units != null) {
            LOG.appError("createInstance: wrong type with units: " + type);
        }
        if (type == Widget.GUESS_TYPE) {
            /* type detection */
            if (units != null) {
                type = Widget.Type.TEXTFIELDWITHUNIT;
            } else if (items == null || items.length == 0) {
                type = Widget.Type.TEXTFIELD;
            } else if (items.length == 2) {
                if (items[0] != null && items[0].toString().equalsIgnoreCase(Checkbox.CHECKBOX_TRUE) && items[1] != null
                    && items[1].toString().equalsIgnoreCase(Checkbox.CHECKBOX_FALSE)) {
                    type = Widget.Type.CHECKBOX;
                } else {
                    type = Widget.Type.COMBOBOX;
                }
            } else {
                type = Widget.Type.COMBOBOX;
            }
        }
        switch(type) {
            case LABELFIELD:
                final Label label = labelProvider.get();
                label.init(selectedValue, regexp, width, enableAccessMode, fieldButton);
                return label;
            case COMBOBOX:
                final ComboBox comboBox = comboBoxProvider.get();
                comboBox.init(selectedValue, items, regexp, width, abbreviations, enableAccessMode, fieldButton);
                return comboBox;
            case PASSWDFIELD:
                final Passwdfield passwdfield = passwdFieldProvider.get();
                passwdfield.init(selectedValue, regexp, width, enableAccessMode, fieldButton);
                return passwdfield;
            case TEXTFIELD:
                final Textfield textfield = textfieldInstance.get();
                textfield.init(selectedValue, regexp, width, abbreviations, enableAccessMode, fieldButton);
                return textfield;
            case TEXTFIELDWITHUNIT:
                final TextfieldWithUnit textfieldWithUnit = textFieldWithUnitProvider.get();
                textfieldWithUnit.init(selectedValue,
                                       units,
                                       regexp,
                                       width,
                                       abbreviations,
                                       enableAccessMode,
                                       fieldButton);
                return textfieldWithUnit;
            case RADIOGROUP:
                final RadioGroup radioGroup = radioGroupProvider.get();
                radioGroup.init(selectedValue, items, regexp, width, enableAccessMode, fieldButton);
                return radioGroup;
            case CHECKBOX:
                final Checkbox checkbox = checkboxProvider.get();
                checkbox.init(selectedValue, items, regexp, width, enableAccessMode, fieldButton);
                return checkbox;
            default:
                LOG.appError("createInstance: unknown type: " + type);
                return null;
        }
    }

    public MyButton createButton() {
        return new MyButton();
    }

    public MyButton createButton(final String text) {
        return new MyButton(text);
    }

    public MyButton createButton(final String text, final Icon icon) {
        return new MyButton(text, icon);
    }

    public MyButton createButton(final String text, final Icon icon, final String toolTipText) {
        return new MyButton(text, icon, toolTipText);
    }
}
