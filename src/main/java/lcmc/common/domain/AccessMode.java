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

import java.util.LinkedHashMap;
import java.util.Map;

import lcmc.common.domain.util.Tools;

/**
 * This class holds access type and mode (advanced or not advanced).
 */
public class AccessMode {

    public static final Map<Type, String> OP_MODES_MAP = new LinkedHashMap<>();
    public static final Map<String, AccessMode.Type> ACCESS_TYPE_MAP = new LinkedHashMap<>();
    public static final String OP_MODE_READONLY = Tools.getString("Application.OpMode.RO");
    private static final String OP_MODE_OPERATOR = Tools.getString("Application.OpMode.OP");
    private static final String OP_MODE_ADMIN = Tools.getString("Application.OpMode.ADMIN");
    public static final String OP_MODE_GOD = Tools.getString("Application.OpMode.GOD");

    public static final Mode ADVANCED = Mode.Advanced;
    public static final Mode NORMAL = Mode.Normal;
    public static final Type RO = Type.RO;
    public static final Type OP = Type.OP;
    public static final Type ADMIN = Type.ADMIN;
    public static final Type GOD = Type.GOD;
    public static final Type NEVER = Type.NEVER;

    static {
        OP_MODES_MAP.put(RO, OP_MODE_READONLY);
        OP_MODES_MAP.put(OP, OP_MODE_OPERATOR);
        OP_MODES_MAP.put(ADMIN, OP_MODE_ADMIN);
        OP_MODES_MAP.put(GOD, OP_MODE_GOD);

        ACCESS_TYPE_MAP.put(OP_MODE_READONLY, RO);
        ACCESS_TYPE_MAP.put(OP_MODE_OPERATOR, OP);
        ACCESS_TYPE_MAP.put(OP_MODE_ADMIN, ADMIN);
        ACCESS_TYPE_MAP.put(OP_MODE_GOD, GOD);
    }

    private final Type type;
    private final Mode advancedMode;

    public AccessMode(final Type accessType, final Mode advancedMode) {
        type = accessType;
        this.advancedMode = advancedMode;
    }

    public Type getType() {
        return type;
    }

    public boolean isAdvancedMode() {
        return advancedMode == ADVANCED;
    }

    public enum Mode {
        Advanced,
        Normal
    }

    public enum Type {
        RO,
        OP,
        ADMIN,
        GOD,
        NEVER
    }
}
