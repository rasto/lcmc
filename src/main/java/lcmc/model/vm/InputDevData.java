/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.model.vm;

import lcmc.model.StringValue;

/** Class that holds data about virtual input devices. */
public final class InputDevData extends HardwareData {
    /** Type: tablet, mouse... */
    private final String type;
    /** Bus: usb... */
    private final String bus;
    public static final String TYPE = "type";
    public static final String BUS = "bus";
    public static final String SAVED_TYPE = "saved_type";
    public static final String SAVED_BUS = "saved_bus";

    public InputDevData(final String type, final String bus) {
        super();
        this.type = type;
        setValue(TYPE, new StringValue(type));
        this.bus = bus;
        setValue(BUS, new StringValue(bus));
    }

    public String getType() {
        return type;
    }

    public String getBus() {
        return bus;
    }
}
