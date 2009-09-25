/*
 * This file is part of DRBD Management Console by Rasto Levrinc
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
package drbd.data;

import java.awt.Color;

/**
 * Subtext for graph with its colors.
 */
public class Subtext {
    /** Subtext. */
    private final String subtext;
    /** Color. */
    private final Color color;
    /**
     * Creates new Subtext object.
     */
    public Subtext(final String subtext, final Color color) {
        this.subtext = subtext;
        this.color = color;
    }

    /**
     * Returns subtext.
     */
    public final String getSubtext() {
        return subtext;
    }

    /**
     * Returns color.
     */
    public final Color getColor() {
        return color;
    }
}
