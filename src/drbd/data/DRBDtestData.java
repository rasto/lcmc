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

package drbd.data;

import drbd.utilities.Tools;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.commons.collections.map.MultiKeyMap;

/**
 * This class holds data that were retrieved while running drbdadm -d commands.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class DRBDtestData {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Tool tip. */
    private final String toolTip;
    /** Pattern for dry run output: drbdsetup 0 disconnect. */
    private static final Pattern DRBD_D_PATTERN =
          Pattern.compile(".*drbdsetup\\s+(\\d+)\\s+(\\S+).*");
    /** Hash with host and drbd resource, that will be connected. */
    private final MultiKeyMap connectedHash = new MultiKeyMap();
    /** Hash with host and drbd resource, that will be disconnected. */
    private final MultiKeyMap disconnectedHash = new MultiKeyMap();
    /** Hash with host and drbd resource, that will be dettached. */
    private final MultiKeyMap disklessHash = new MultiKeyMap();

    /**
     * Prepares a new <code>DRBDtestData</code> object.
     */
    public DRBDtestData(final Map<Host, String> testOutput) {
        if (testOutput == null) {
            this.toolTip = null;
            return;
        }
        final StringBuffer sb = new StringBuffer(300);
        sb.append("<html><b>");
        sb.append(Tools.getString("DRBDtestData.ToolTip"));
        sb.append("</b><br>");
        boolean isToolTip = false;
        for (final Host host : testOutput.keySet()) {
            sb.append("<b>");
            sb.append(host.getName());
            sb.append("</b><br>");
            final String raw = testOutput.get(host);
            for (final String line : raw.split("\\r?\\n")) {
                final Matcher m = DRBD_D_PATTERN.matcher(line);
                if (m.matches()) {
                    final String res = m.group(1);
                    final String action = m.group(2);
                    if ("disconnect".equals(action)) {
                        disconnectedHash.put(host, "/dev/drbd" + res, 1);
                    } else if ("net".equals(action)) {
                        connectedHash.put(host, "/dev/drbd" + res, 1);
                    } else if ("detach".equals(action)) {
                        disklessHash.put(host, "/dev/drbd" + res, 1);
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
            sb.append("no actions");
        }
        sb.append("</html>");
        this.toolTip = sb.toString();
    }

    /**
     * Returns tooltip.
     */
    public final String getToolTip() {
        return toolTip;
    }

    /**
     * Returns whether the device will be disconnected on the host.
     */
    public final boolean isDisconnected(final Host host, final String dev) {
        return disconnectedHash.get(host, dev) != null;
    }

    /**
     * Returns whether the device will be connected on the host.
     */
    public final boolean isConnected(final Host host, final String dev) {
        return connectedHash.get(host, dev) != null;
    }

    /**
     * Returns whether the drbd device is diskless on the host.
     */
    public final boolean isDiskless(final Host host, final String dev) {
        return disklessHash.get(host, dev) != null;
    }
}
