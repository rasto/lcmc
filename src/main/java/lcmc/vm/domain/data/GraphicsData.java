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

package lcmc.vm.domain.data;

import lcmc.common.domain.StringValue;

/** Class that holds data about virtual displays. */
public final class GraphicsData extends HardwareData {
    private final String type;
    /** Type: vnc, sdl... */
    public static final String TYPE = "type";
    public static final String SAVED_TYPE = "saved_type";
    public static final String AUTOPORT = "autoport";
    /** Port: -1 for auto. */
    public static final String PORT = "port";
    /** Listen: ip, on which interface to listen. */
    public static final String LISTEN = "listen";
    public static final String PASSWD = "passwd";
    public static final String KEYMAP = "keymap";
    /** Display / SDL. */
    public static final String DISPLAY = "display";
    /** Xauth file / SDL. */
    public static final String XAUTH = "xauth";

    public GraphicsData(final String type,
                        final String port,
                        final String listen,
                        final String passwd,
                        final String keymap,
                        final String display,
                        final String xauth) {
        super();
        this.type = type;
        setValue(TYPE, new StringValue(type));
        setValue(PORT, new StringValue(port));
        setValue(LISTEN, new StringValue(listen));
        setValue(PASSWD, new StringValue(passwd));
        setValue(KEYMAP, new StringValue(keymap));
        setValue(DISPLAY, new StringValue(display));
        setValue(XAUTH, new StringValue(xauth));
    }

    public String getType() {
        return type;
    }
}
