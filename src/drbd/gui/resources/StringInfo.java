/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
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
package drbd.gui.resources;

import drbd.gui.Browser;

/**
 * This class is used for elements, that have their appearence (name)
 * different than is their stored value (string).
 */
public class StringInfo extends Info {
    /** Internal string. */
    private final String string;

    /**
     * Creates new <code>StringInfo</code> object.
     *
     * @param name
     *              user visible name
     * @param string
     *              string representation
     */
    public StringInfo(final String name,
                      final String string,
                      final Browser browser) {
        super(name, browser);
        this.string = string;
    }

    /**
     * Returns the name. It will be shown to the user.
     */
    public final String toString() {
        return getName();
    }

    /** Returns the string that is used internally. */
    public final String getStringValue() {
        return string;
    }
}

