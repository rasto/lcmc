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

/** Class that holds data about virtual parallel or serial devices. */
public abstract class ParallelSerialData extends HardwareData {
    private final String type;
    /** Type: dev, file, null, pipe, pty, stdio, tcp, udp, unix, vc. */
    public static final String TYPE = "type";
    public static final String SAVED_TYPE = "saved_type";
    public static final String SOURCE_PATH = "source_path";
    /** Source mode: bind, connect. This is for tcp, because it is
     * either-or */
    public static final String SOURCE_MODE = "source_mode";
    /** Source mode: bind, connect. */
    public static final String BIND_SOURCE_MODE = "bind_source_mode";
    public static final String BIND_SOURCE_HOST = "bind_source_host";
    public static final String BIND_SOURCE_SERVICE = "bind_source_service";
    /** Source mode: bind, connect. */
    public static final String CONNECT_SOURCE_MODE = "bind_source_mode";
    public static final String CONNECT_SOURCE_HOST = "connect_source_host";
    public static final String CONNECT_SOURCE_SERVICE = "connect_source_service";
    public static final String PROTOCOL_TYPE = "protocol_type";
    public static final String TARGET_PORT = "target_port";

    public ParallelSerialData(final String type,
                              final String sourcePath,
                              final String bindSourceMode,
                              final String bindSourceHost,
                              final String bindSourceService,
                              final String connectSourceMode,
                              final String connectSourceHost,
                              final String connectSourceService,
                              final String protocolType,
                              final String targetPort) {
        super();
        this.type = type;
        setValue(TYPE, new StringValue(type));
        setValue(SOURCE_PATH, new StringValue(sourcePath));
        setValue(BIND_SOURCE_MODE, new StringValue(bindSourceMode));
        setValue(BIND_SOURCE_HOST, new StringValue(bindSourceHost));
        setValue(BIND_SOURCE_SERVICE, new StringValue(bindSourceService));
        setValue(CONNECT_SOURCE_MODE, new StringValue(connectSourceMode));
        setValue(CONNECT_SOURCE_HOST, new StringValue(connectSourceHost));
        setValue(CONNECT_SOURCE_SERVICE, new StringValue(connectSourceService));
        setValue(PROTOCOL_TYPE, new StringValue(protocolType));
        setValue(TARGET_PORT, new StringValue(targetPort));
    }

    public final String getType() {
        return type;
    }
}
