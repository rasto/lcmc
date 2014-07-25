/*
 * This file is part of DRBD Management Console by Rasto Levrinc
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
package lcmc.model;

import java.awt.Color;

/**
 * Subtext for graph with its colors.
 */
 //TODO: rename to ColorText
public final class Subtext {
    /** Subtext. */
    private final String subtext;
    /** Color. */
    private final Color color;
    /** Text color. */
    private final Color textColor;

    /** Creates new Subtext object. */
    public Subtext(final String subtext,
                   final Color color,
                   final Color textColor) {
        this.subtext = subtext;
        this.color = color;
        this.textColor = textColor;
    }

    /** Returns subtext. */
    public String getSubtext() {
        return subtext;
    }

    /** Returns color. */
    public Color getColor() {
        return color;
    }

    /** Returns text color. */
    public Color getTextColor() {
        return textColor;
    }
}
