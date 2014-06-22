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

package lcmc;

/**
 * Encompassing class for all custom exceptions.
 */
public final class Exceptions {

    private Exceptions() {
    }

    public static class DrbdConfigException extends Exception {
        private static final long serialVersionUID = 1L;

        public DrbdConfigException(final String msg) {
            super(msg);
        }
    }

    public static class IllegalVersionException extends Exception {
        private static final long serialVersionUID = 1L;

        public IllegalVersionException(final String v1) {
            super("illegal version: " + v1);
        }

        public IllegalVersionException(final String v1, final String v2) {
            super("illegal version: " + v1 + ", " + v2);
        }
    }
}
