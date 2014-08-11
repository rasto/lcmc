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

/** Class that holds data about virtual interfaces. */
public final class InterfaceData extends HardwareData {
    /** Type: network, bridge... */
    private final String type;
    /** Source network: default, ... */
    private final String sourceNetwork;
    /** Source bridge: br0... */
    private final String sourceBridge;
    /** Target dev: vnet0... */
    private final String targetDev;
    public static final String TYPE = "type";
    public static final String MAC_ADDRESS = "mac_address";
    public static final String SAVED_MAC_ADDRESS = "saved_mac_address";
    public static final String SOURCE_NETWORK = "source_network";
    public static final String SOURCE_BRIDGE = "source_bridge";
    public static final String SCRIPT_PATH = "script_path";
    public static final String TARGET_DEV = "target_dev";
    /** Model type: virtio... */
    public static final String MODEL_TYPE = "model_type";

    /** Creates new InterfaceData object. */
    public InterfaceData(final String type,
                         final String macAddress,
                         final String sourceNetwork,
                         final String sourceBridge,
                         final String targetDev,
                         final String modelType,
                         final String scriptPath) {
        super();
        this.type = type;
        setValue(TYPE, new StringValue(type));
        setValue(MAC_ADDRESS, new StringValue(macAddress));
        this.sourceNetwork = sourceNetwork;
        setValue(SOURCE_NETWORK, new StringValue(sourceNetwork));
        this.sourceBridge = sourceBridge;
        setValue(SOURCE_BRIDGE, new StringValue(sourceBridge));
        this.targetDev = targetDev;
        setValue(TARGET_DEV, new StringValue(targetDev));
        setValue(MODEL_TYPE, new StringValue(modelType));
        setValue(SCRIPT_PATH, new StringValue(scriptPath));
    }

    public String getType() {
        return type;
    }

    public String getSourceNetwork() {
        return sourceNetwork;
    }

    public String getSourceBridge() {
        return sourceBridge;
    }

    public String getTargetDev() {
        return targetDev;
    }
}
