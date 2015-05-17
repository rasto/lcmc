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

/** Class that holds data about networks. */
public final class NetworkData extends HardwareData {
    private final String name;
    private final String uuid;
    private final boolean autostart;
    private final String forwardMode;
    private final String bridgeName;
    private final String bridgeSTP;
    private final String bridgeDelay;
    private final String bridgeForwardDelay;
    static final String AUTOSTART = "autostart";
    static final String FORWARD_MODE = "forward_mode";
    static final String BRIDGE_NAME = "bridge_name";
    static final String BRIDGE_STP = "bridge_stp";
    static final String BRIDGE_DELAY = "bridge_delay";
    static final String BRIDGE_FORWARD_DELAY = "bridge_forward_delay";

    public NetworkData(final String name,
                       final String uuid,
                       final boolean autostart,
                       final String forwardMode,
                       final String bridgeName,
                       final String bridgeSTP,
                       final String bridgeDelay,
                       final String bridgeForwardDelay) {
        super();
        this.name = name;
        this.uuid = uuid;
        this.autostart = autostart;
        if (autostart) {
            setValue(AUTOSTART, new StringValue("true"));
        } else {
            setValue(AUTOSTART, new StringValue("false"));
        }
        this.forwardMode = forwardMode;
        setValue(FORWARD_MODE, new StringValue(forwardMode));
        this.bridgeName = bridgeName;
        setValue(BRIDGE_NAME, new StringValue(bridgeName));
        this.bridgeSTP = bridgeSTP;
        setValue(BRIDGE_STP, new StringValue(bridgeSTP));
        this.bridgeDelay = bridgeDelay;
        setValue(BRIDGE_DELAY, new StringValue(bridgeDelay));
        this.bridgeForwardDelay = bridgeForwardDelay;
        setValue(BRIDGE_FORWARD_DELAY, new StringValue(bridgeForwardDelay));
    }
    boolean isAutostart() {
        return autostart;
    }

    String getForwardMode() {
        return forwardMode;
    }

    String getBridgeName() {
        return bridgeName;
    }

    String getBridgeSTP() {
        return bridgeSTP;
    }

    String getBridgeDelay() {
        return bridgeDelay;
    }

    String getBridgeForwardDelay() {
        return bridgeForwardDelay;
    }
}
