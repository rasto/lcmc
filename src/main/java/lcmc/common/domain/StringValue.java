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

package lcmc.common.domain;

import java.util.Objects;

public class StringValue implements Value, Comparable<Value> {

    private static final String NOTHING_VALUE = null;

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
    /** Internal string, used in configs. */
    private final String valueForConfig;
    /** Value visible in the GUI. */
    private final String valueForGui;
    
    private final Unit unit;
    
    /** Constructor for "nothing selected" */
    public StringValue() {
        this(NOTHING_VALUE);
    }

    public StringValue(final String valueForConfig, final Unit unit) {
        this(valueForConfig, valueForConfig, unit);
    }

    public StringValue(final String valueForConfig, final String valueForGui) {
        this(valueForConfig, valueForGui, null);
    }

    public StringValue(final String valueForConfig, final String valueForGui, final Unit unit) {
        if (valueForConfig == null || valueForConfig.isEmpty()) {
            this.valueForConfig = NOTHING_VALUE;
        } else {
            this.valueForConfig = valueForConfig;
        }
        this.valueForGui = valueForGui;
        this.unit = unit;
    }

    public StringValue(final String valueForConfig) {
        if (valueForConfig == null || valueForConfig.isEmpty()) {
            this.valueForConfig = NOTHING_VALUE;
        } else {
            this.valueForConfig = valueForConfig;
        }
        if (isNothingSelected()) {
            valueForGui = null;
        } else {
            valueForGui = valueForConfig;
        }
        unit = null;
    }

    /** Returns the display name. It will be shown in the GUI. */
    @Override
    public String toString() {
        return getValueForGui();
    }

    @Override
    public String getValueForConfig() {
        return valueForConfig;
    }

    @Override
    public String getValueForGui() {
        if (valueForGui == null) {
            return getNothingSelected();
        }
        return valueForGui;
    }

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
    public final boolean isNothingSelected() {
        return valueForConfig == null;
    }

    @Override
    public String getNothingSelected() {
        return NOTHING_SELECTED;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (valueForConfig != null ? valueForConfig.hashCode() : 0);
        hash = 59 * hash + (unit != null ? unit.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Value)) {
            return false;
        }
        final Value other = (Value) obj;
        if ((valueForConfig == null) ? (other.getValueForConfig() != null) : !valueForConfig.equals(other.getValueForConfig())) {
            return false;
        }
        return unit == other.getUnit() || (unit != null && unit.equals(other.getUnit()));
    }

    @Override
    public int compareTo(final Value o) {
        String otherValue = null;
        if (o != null) {
            otherValue = o.getValueForConfig();
        }
        if (otherValue == null) {
            otherValue = "";
        }
        return Objects.requireNonNullElse(valueForConfig, "").compareTo(otherValue);
    }
}
