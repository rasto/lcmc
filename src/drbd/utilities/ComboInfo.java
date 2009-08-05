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
 * This class provides an object that can be used in boxes where name and its
 * string representation is different.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class ComboInfo {
    /** Internal String. */
    private final String string;
    /** Name. */
    private final String name;

    /**
     * Creates a new ComboInfo object.
     */
    public ComboInfo(final String name, final String string) {
        this.name = name;
        this.string = string;
    }

    /**
     * Returns the name. It will be shown to the user.
     */
    public final String toString() {
        return name;
    }

    /**
     * Returns the string that is used internally.
     */
    public final String getStringValue() {
        return string;
    }
}
