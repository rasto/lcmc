/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rastislav Levrinc
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

/**
 * This class holds data of host locations.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HostLocation {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Host score. */
    private final String score;
    /** Operation, eq. "eq", "ne". */
    private final String op;
    /** Value. */
    private final String value;

    /**
     * Prepares a new <code>AisCastAddress</code> object.
     */
    public HostLocation(final String score,
                        final String op,
                        final String value) {
        this.score = score;
        this.op    = op;
        this.value = value;
    }

    /** Returns score. */
    public final String getScore() {
        return score;
    }

    /** Returns score. */
    public final String getOperation() {
        return op;
    }

    /** Returns value. */
    public final String getValue() {
        return value;
    }

    /**
     * Return whether the two objects are equal.
     */
    public final boolean equals(final Object other) {
        if (other == null) {
            return (score == null || "".equals(score))
                   && (op == null || "".equals(op));
        }
        final HostLocation otherHL = (HostLocation) other;
        if (score == null) {
            if (otherHL.getScore() != null && !"".equals(otherHL.getScore())) {
                return false;
            }
        } else {
            if (!score.equals(otherHL.getScore())) {
                return false;
            }
        }
        if (op == null
            || "".equals(op)
            || "eq".equals(op)) {
            return otherHL.getOperation() == null
                   || "".equals(otherHL.getOperation())
                   || "eq".equals(otherHL.getOperation());
        } else {
            return op.equals(otherHL.getOperation());
        }
    }
}
