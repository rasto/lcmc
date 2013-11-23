/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2013, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.data;

import lcmc.utilities.Unit;
import lcmc.utilities.Tools;

public final class StringValue implements Value {
    /** Internal string, used in configs. */
    private final String valueForConfig;
    /** Value visible in the GUI. */
    private final String valueForGui;
    
    private final Unit unit;

    private static final String NOTHING_SELECTED =
                                Tools.getString("Widget.NothingSelected");

    private static final String NOTHING_VALUE = null;
    
    /** Constructor for "nothing selected" */
    public StringValue() {
        this(NOTHING_VALUE);
    }

    public StringValue(final String valueForConfig,
                       final String valueForGui) {
        if (valueForGui == null) {
            throw new RuntimeException(
                                "Value: valueForGui is null, valueForConfig: "
                                 + valueForConfig);
        }
        this.valueForConfig = valueForConfig;
        this.valueForGui = valueForGui;
        this.unit = null;
    }

    public StringValue(final String valueForConfig,
                       final String valueForGui,
                       final Unit unit) {
        if (valueForGui == null) {
            throw new RuntimeException("Value: valueForGui is null, valueForConfig: "
                                       + valueForConfig);
        }
        this.valueForConfig = valueForConfig;
        this.valueForGui = valueForGui;
        this.unit = unit;
    }

    public StringValue(final String valueForConfig) {
        if (valueForConfig == null) {
            this.valueForConfig = NOTHING_VALUE;
        } else {
            this.valueForConfig = valueForConfig;
        }
        if (isNothingSelected()) { //TODO can be ""
            this.valueForGui = getNothingSelected();
        } else {
            this.valueForGui = valueForConfig;
        }
        this.unit = null;
    }

    /** Returns the display name. It will be shown in the GUI. */
    @Override
    public String toString() {
        return valueForGui;
    }

    /** Returns the string that is used for config. */
    @Override
    public String getValueForConfig() {
        return valueForConfig;
    }

    @Override
    public String getValueForGui() {
        return valueForGui;
    }

    /** Returns the string that is used for config. */
    @Override
    public String getValueForConfigWithUnit() {
        if (unit == null) {
            return valueForConfig;
        } else {
            return valueForConfig + unit.getShortName();
        }
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    @Override
    public boolean isNothingSelected() {
        final String nv = getNothingValue();
        if (nv == null && valueForConfig == null) {
            return true;
        }
        if (nv != null && nv.equals(valueForConfig)) {
            return true;
        }
        return false;
    }

    public String getNothingSelected() {
        return NOTHING_SELECTED;
    }

    public String getNothingValue() {
        return NOTHING_VALUE;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (this.valueForConfig != null ? this.valueForConfig.hashCode() : 0);
        hash = 59 * hash + (this.unit != null ? this.unit.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StringValue other = (StringValue) obj;
        if ((this.valueForConfig == null) ? (other.valueForConfig != null) : !this.valueForConfig.equals(other.valueForConfig)) {
            return false;
        }
        if (this.unit != other.unit && (this.unit == null || !this.unit.equals(other.unit))) {
            return false;
        }
        return true;
    }

    public static Value[] getValues(final String[] strings) {
        if (strings == null) {
            return null;
        }
        final Value[] values = new Value[strings.length];
        int i = 0;
        for (final String s : strings) {
            values[i] = new StringValue(s);
            i++;
        }
        return values;
    }

    public static String getValueForConfig(final Value v) {
        if (v == null) {
            return NOTHING_VALUE;
        } else {
            return v.getValueForConfig();
        }
    }
}

