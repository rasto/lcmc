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

import lcmc.utilities.MyButton;
import lcmc.utilities.Unit;
import lcmc.data.AccessMode;
import java.util.Map;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * @author Rasto Levrinc
 */
public final class WidgetFactory {
    /** Logger. */
    private static final Logger LOG =
                                 LoggerFactory.getLogger(WidgetFactory.class);
    private WidgetFactory() {
    }

    /** Without units. */
    public static Widget createInstance(final Widget.Type type,
                                        final String selectedValue,
                                        final Object[] items,
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

    public static Widget createInstance(Widget.Type type,
                                        final String selectedValue,
                                        final Object[] items,
                                        final Unit[] units,
                                        final String regexp,
                                        final int width,
                                        final Map<String, String> abbreviations,
                                        final AccessMode enableAccessMode,
                                        final MyButton fieldButton) {
        if (type != null
            && type != Widget.Type.TEXTFIELDWITHUNIT
            && units != null) {
            LOG.appError("wrong type with units: " + type);
        }
        if (type == null) {
            /* type detection */
            if (units != null) {
                type = Widget.Type.TEXTFIELDWITHUNIT;
            } else if (items == null || items.length == 0) {
                type = Widget.Type.TEXTFIELD;
            } else if (items.length == 2) {
                if (items[0] != null
                    && items[0].toString().equalsIgnoreCase(
                                                        Checkbox.CHECKBOX_TRUE)
                    && items[1] != null
                    && items[1].toString().equalsIgnoreCase(
                                                    Checkbox.CHECKBOX_FALSE)) {
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
                return new Label(selectedValue,
                                 regexp,
                                 width,
                                 enableAccessMode,
                                 fieldButton);
            case COMBOBOX:
                return new ComboBox(selectedValue,
                                    items,
                                    regexp,
                                    width,
                                    abbreviations,
                                    enableAccessMode,
                                    fieldButton);
            case PASSWDFIELD:
                return new Passwdfield(selectedValue,
                                       regexp,
                                       width,
                                       enableAccessMode,
                                       fieldButton);
            case TEXTFIELD:
                return new Textfield(selectedValue,
                                     regexp,
                                     width,
                                     abbreviations,
                                     enableAccessMode,
                                     fieldButton);
            case TEXTFIELDWITHUNIT:
                return new TextfieldWithUnit(selectedValue,
                                             units,
                                             regexp,
                                             width,
                                             abbreviations,
                                             enableAccessMode,
                                             fieldButton);
            case RADIOGROUP:
                return new RadioGroup(selectedValue,
                                      items,
                                      regexp,
                                      width,
                                      enableAccessMode,
                                      fieldButton);
            case CHECKBOX:
                return new Checkbox(selectedValue,
                                    items,
                                    regexp,
                                    width,
                                    enableAccessMode,
                                    fieldButton);
            default:
                LOG.appError("unknown type: " + type);
                return null;
        }
    }
}
