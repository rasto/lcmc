/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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

package drbd.utilities;

/**
 * This class provides unit object with short and long name for combo boxes.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class Unit {
    /** Short name. */
    private final String shortName;
    /** Secondary short name used for comparisons. */
    private final String secShortName;
    /** Name of the unit. */
    private final String name;
    /** Plural name of the unit. */
    private final String pluralName;
    /** Whether the unit should be in plural or not. */
    private boolean plural = true;

    /**
     * Prepares new <code>Unit</code> object.
     */
    public Unit(final String shortName,
                final String secShortName,
                final String name,
                final String pluralName) {
        this.shortName    = shortName;
        this.secShortName = secShortName;
        this.name         = name;
        this.pluralName   = pluralName;
    }

    /**
     * Returns whether the unit should be in plural.
     */
    public final boolean isPlural() {
        return plural;
    }

    /**
     * Sets that the unit should be in plural.
     */
    public final void setPlural(final boolean plural) {
        this.plural = plural;
    }

    /**
     * Returns the unit, plural if it should be in plural.
     */
    @Override
    public final String toString() {
        if (plural) {
            return pluralName;
        }
        return name;
    }

    /**
     * Returns true if units are equals.
     */
    @Override
    public final boolean equals(final Object o) {
        if (o == null) {
            return false;
        }

        if (Tools.isStringClass(o)) {
            return shortName.equalsIgnoreCase((String) o)
                   || secShortName.equalsIgnoreCase((String) o);
        }

        return name.equals(((Unit) o).toString())
               || pluralName.equals(((Unit) o).toString());
    }

    /**
     * Hash code.
     */
    @Override
    public final int hashCode() {
        return name.hashCode();
    }

    /**
     * Returns short name of the unit.
     */
    public final String getShortName() {
        return shortName;
    }
}
