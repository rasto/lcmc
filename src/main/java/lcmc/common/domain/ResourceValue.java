/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
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


package lcmc.common.domain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class holds resource data.
 */
public class ResourceValue {
    private String name = null;
    private final Map<String, Value> savedValue = new HashMap<String, Value>();
    private final Map<String, Value[]> possibleChoicesMap = new HashMap<String, Value[]>();
    private final Map<String, Value> defaultValueMap = new HashMap<String, Value>();
    private final Map<String, Value> preferredValueMap = new HashMap<String, Value>();

    private boolean newResource = false;

    public ResourceValue() {
    }

    public ResourceValue(final String name) {
        this.name = name;
    }

    public final void setValue(final String param, final Value value) {
        savedValue.put(param, value);
    }

    public Value getValue(final String param) {
        return savedValue.get(param);
    }

    public final String getName() {
        return name;
    }

    public final void setName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
    /**
     * Creates default values list and returns them as a List object.
     * It uses the selected value as first item in the list if it exists.
     * It removes this value if it is in the values array. If there is selected
     * value it also removes empty string from values array.
     */
    protected List<Value> getPossibleChoices(final String param, final Value[] values) {
        final List<Value> possibleChoices = new ArrayList<Value>();
        if (values == null) {
            return possibleChoices;
        }
        possibleChoices.addAll(Arrays.asList(values));
        return possibleChoices;
    }

    public final void setPossibleChoices(final String param, final Value[] possibleChoices) {
        possibleChoicesMap.remove(param);
        possibleChoicesMap.put(param, possibleChoices);
    }

    public Value[] getPossibleChoices(final String param) {
        final List<Value> possibleChoices = getPossibleChoices(param, possibleChoicesMap.get(param));
        return possibleChoices.toArray(new Value[possibleChoices.size()]);
    }

    public final void setDefaultValue(final String param, final Value defaultValue) {
        defaultValueMap.put(param, defaultValue);
    }

    public final Value getDefaultValue(final String param) {
        return defaultValueMap.get(param);
    }

    final void setPreferredValue(final String param, final Value preferredValue) {
        preferredValueMap.put(param, preferredValue);
    }

    public final Value getPreferredValue(final String param) {
        return preferredValueMap.get(param);
    }

    public final void setNew(final boolean newResource) {
        this.newResource = newResource;
    }

    public final boolean isNew() {
        return newResource;
    }
}
