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

/** Class that holds data about virtual disks. */
public final class DiskData extends HardwareData {
    /** Type: file, block... */
    private final String type;
    /** Target device: hda, hdb, hdc, sda... */
    private final String targetDev;
    private final String sourceFile;
    /** Source device: /dev/drbd0... */
    private final String sourceDev;
    private final String sourceProtocol;
    private final String sourceName;
    private final String sourceHostName;
    private final String sourceHostPort;
    private final String authUsername;
    private final String authSecretType;
    private final String authSecretUuid;
    /** Target bus: ide... and type: disk..., delimited with, */
    private final String targetBusType;
    /** Driver name: qemu... */
    private final String driverName;
    /** Driver type: raw... */
    private final String driverType;
    /** Driver cache: none... */
    private final String driverCache;
    private final boolean readonly;
    private final boolean shareable;
    public static final String TYPE = "type";
    public static final String TARGET_DEVICE = "target_device";
    public static final String SAVED_TARGET_DEVICE = "saved_target_device";
    public static final String SOURCE_FILE = "source_file";
    public static final String SOURCE_DEVICE = "source_dev";
    public static final String SOURCE_PROTOCOL = "source_protocol";
    public static final String SOURCE_NAME = "source_name";
    public static final String SOURCE_HOST_NAME = "source_host_name";
    public static final String SOURCE_HOST_PORT = "source_host_port";
    public static final String AUTH_USERNAME = "auth_username";
    public static final String AUTH_SECRET_TYPE = "auth_secret_type";
    public static final String AUTH_SECRET_UUID = "auth_secret_uuid";
    public static final String TARGET_BUS_TYPE = "target_bus_type";
    public static final String TARGET_BUS = "target_bus";
    public static final String TARGET_TYPE = "target_type";
    public static final String DRIVER_NAME = "driver_name";
    public static final String DRIVER_TYPE = "driver_type";
    public static final String DRIVER_CACHE = "driver_cache";
    public static final String READONLY = "readonly";
    public static final String SHAREABLE = "shareable";

    public DiskData(final String type,
                    final String targetDev,
                    final String sourceFile,
                    final String sourceDev,
                    final String sourceProtocol,
                    final String sourceName,
                    final String sourceHostName,
                    final String sourceHostPort,
                    final String authUsername,
                    final String authSecretType,
                    final String authSecretUuid,
                    final String targetBusType,
                    final String driverName,
                    final String driverType,
                    final String driverCache,
                    final boolean readonly,
       final boolean shareable) {
        super();
        this.type = type;
        setValue(TYPE, new StringValue(type));
        this.targetDev = targetDev;
        setValue(TARGET_DEVICE, new StringValue(targetDev));
        this.sourceFile = sourceFile;
        setValue(SOURCE_FILE, new StringValue(sourceFile));
        this.sourceDev = sourceDev;
        setValue(SOURCE_DEVICE, new StringValue(sourceDev));

        this.sourceProtocol = sourceProtocol;
        setValue(SOURCE_PROTOCOL, new StringValue(sourceProtocol));
        this.sourceName = sourceName;
        setValue(SOURCE_NAME, new StringValue(sourceName));
        this.sourceHostName = sourceHostName;
        setValue(SOURCE_HOST_NAME, new StringValue(sourceHostName));
        this.sourceHostPort = sourceHostPort;
        setValue(SOURCE_HOST_PORT, new StringValue(sourceHostPort));
        this.authUsername = authUsername;
        setValue(AUTH_USERNAME, new StringValue(authUsername));
        this.authSecretType = authSecretType;
        setValue(AUTH_SECRET_TYPE, new StringValue(authSecretType));
        this.authSecretUuid = authSecretUuid;
        setValue(AUTH_SECRET_UUID, new StringValue(authSecretUuid));

        this.targetBusType = targetBusType;
        setValue(TARGET_BUS_TYPE, new StringValue(targetBusType));
        this.driverName = driverName;
        setValue(DRIVER_NAME, new StringValue(driverName));
        this.driverType = driverType;
        setValue(DRIVER_TYPE, new StringValue(driverType));
        this.driverCache = driverCache;
        setValue(DRIVER_CACHE, new StringValue(driverCache));
        this.readonly = readonly;
        if (readonly) {
            setValue(READONLY, new StringValue("True"));
        } else {
            setValue(READONLY, new StringValue("False"));
        }
        this.shareable = shareable;
        if (shareable) {
            setValue(SHAREABLE, new StringValue("True"));
        } else {
            setValue(SHAREABLE, new StringValue("False"));
        }
    }

    public String getType() {
        return type;
    }

    public String getTargetDev() {
        return targetDev;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getSourceDev() {
        return sourceDev;
    }

    public String getSourceProtocol() {
        return sourceProtocol;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSourceHostName() {
        return sourceHostName;
    }

    public String getSourceHostPort() {
        return sourceHostPort;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public String getAuthSecretType() {
        return authSecretType;
    }

    public String getAuthSecretUuid() {
        return authSecretUuid;
    }

    public String getTargetBusType() {
        return targetBusType;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDriverType() {
        return driverType;
    }

    public String getDriverCache() {
        return driverCache;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public boolean isShareable() {
        return shareable;
    }
}
