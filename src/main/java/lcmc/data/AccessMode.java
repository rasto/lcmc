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


package lcmc.data;

/**
 * This class holds access type and mode (advanced or not advanced).
 *
 * @author Rasto Levrinc
 *
 */
public final class AccessMode {
    /** Access type. */
    private final Application.AccessType accessType;
    /** Whether it is advanced mode or not. */
    private final boolean advancedMode;
    /** Advanced mode. */
    public static final boolean ADVANCED = true;

    /** Prepares a new {@code AccessMode} object. */
    public AccessMode(final Application.AccessType accessType,
                      final boolean advancedMode) {
        this.accessType = accessType;
        this.advancedMode = advancedMode;
    }

    /** Returns access type. */
    public Application.AccessType getAccessType() {
        return accessType;
    }

    /** Returns advanced mode. */
    public boolean isAdvancedMode() {
        return advancedMode == ADVANCED;
    }
}
