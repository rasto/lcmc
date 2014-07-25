/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * by Rasto Levrinc.
 *
 * Copyright (C) 2009, Rastislav Levrinc
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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

package lcmc.model.drbd;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lcmc.model.Host;
import lcmc.utilities.Tools;
import org.apache.commons.collections15.map.MultiKeyMap;

/**
 * This class holds data that were retrieved while running drbdadm -d commands.
 */
public final class DRBDtestData {
    /** Pattern for dry run output: drbdsetup 0 disconnect. */
    private static final Pattern DRBDSETUP_PATTERN = Pattern.compile(".*drbdsetup\\s+(\\S+)\\s+(\\S+).*");
    private final String toolTip;
    /** Hash with host and drbd resource, that will be connected. */
    private final MultiKeyMap<String, Integer> connectedHash = new MultiKeyMap<String, Integer>();
    /** Hash with host and drbd resource, that will be disconnected. */
    private final MultiKeyMap<String, Integer> disconnectedHash = new MultiKeyMap<String, Integer>();
    /** Hash with host and drbd resource, that will be attached. */
    private final MultiKeyMap<String, Integer> attachedHash = new MultiKeyMap<String, Integer>();
    /** Hash with host and drbd resource, that will be dettached. */
    private final MultiKeyMap<String, Integer> disklessHash = new MultiKeyMap<String, Integer>();

    public DRBDtestData(final Map<Host, String> testOutput) {
        if (testOutput == null) {
            toolTip = null;
            return;
        }
        final StringBuilder sb = new StringBuilder(300);
        sb.append("<html><b>");
        sb.append(Tools.getString("DRBDtestData.ToolTip"));
        sb.append("</b><br>");
        boolean isToolTip = false;
        for (final Map.Entry<Host, String> hostStringEntry : testOutput.entrySet()) {
            sb.append("<b>");
            sb.append(hostStringEntry.getKey().getName());
            sb.append("</b><br>");
            final String raw = hostStringEntry.getValue();
            if (raw == null) {
                continue;
            }
            for (final String line : raw.split("\\r?\\n")) {
                final Matcher m = DRBDSETUP_PATTERN.matcher(line);
                if (m.matches()) {
                    final String resOrVol = m.group(1);
                    final String action = m.group(2);
                    if ("disconnect".equals(action)) {
                        disconnectedHash.put(hostStringEntry.getKey().getName(), resOrVol, 1);
                    } else if ("net".equals(action)) {
                        connectedHash.put(hostStringEntry.getKey().getName(), "/dev/drbd" + resOrVol, 1);
                    } else if ("detach".equals(action)) {
                        disklessHash.put(hostStringEntry.getKey().getName(), "/dev/drbd" + resOrVol, 1);
                    } else if ("disk".equals(action)) {
                        attachedHash.put(hostStringEntry.getKey().getName(), "/dev/drbd" + resOrVol, 1);
                    }
                }
                final int index = line.indexOf("--set-defaults");
                if (index >= 0) {
                    sb.append(line.substring(0, index));
                } else {
                    sb.append(line);
                }
                sb.append("<br>");
                isToolTip = true;
            }
        }
        if (!isToolTip) {
            sb.append(Tools.getString("DRBDtestData.NoToolTip"));
        }
        sb.append("</html>");
        toolTip = sb.toString();
    }

    public String getToolTip() {
        return toolTip;
    }

    /** Returns whether the device will be disconnected on the host. */
    public boolean isDisconnected(final Host host, final String resource) {
        return disconnectedHash.get(host.getName(), resource) != null;
    }

    /** Returns whether the device will be connected on the host. */
    public boolean isConnected(final Host host, final String resource) {
        return connectedHash.get(host.getName(), resource) != null;
    }

    /** Returns whether the drbd device is diskless on the host. */
    public boolean isDiskless(final Host host, final String dev) {
        return disklessHash.get(host.getName(), dev) != null;
    }

    /** Returns whether the drbd device is diskless on the host. */
    public boolean isAttached(final Host host, final String dev) {
        return attachedHash.get(host.getName(), dev) != null;
    }
}
