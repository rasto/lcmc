/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
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
package lcmc.gui.resources;

import lcmc.gui.Browser;
import lcmc.gui.GuiComboBox;
import lcmc.data.ResourceAgent;
import lcmc.data.AccessMode;
import lcmc.utilities.Tools;

import java.util.Map;

/**
 * This class holds info about IPaddr/IPaddr2 heartbeat service. It adds a
 * better ip entering capabilities.
 */
final class IPaddrInfo extends ServiceInfo {
    /** Creates new IPaddrInfo object. */
    IPaddrInfo(final String name,
               final ResourceAgent ra,
               final Browser browser) {
        super(name, ra, browser);
    }

    /** Creates new IPaddrInfo object. */
    IPaddrInfo(final String name,
               final ResourceAgent ra,
               final String hbId,
               final Map<String, String> resourceNode,
               final Browser browser) {
        super(name, ra, hbId, resourceNode, browser);
    }

    /**
     * Returns whether all the parameters are correct. If param is null,
     * all paremeters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    @Override
    boolean checkResourceFieldsCorrect(final String param,
                                       final String[] params) {
        boolean ret = super.checkResourceFieldsCorrect(param, params);
        final GuiComboBox cb;
        if (getResourceAgent().isHeartbeatClass()) {
            cb = paramComboBoxGet("1", null);
        } else if (getResourceAgent().isOCFClass()) {
            cb = paramComboBoxGet("ip", null);
        } else {
            return true;
        }
        if (cb == null) {
            return false;
        }
        cb.setEditable(true);
        cb.selectSubnet();
        if (ret) {
            final String ip = cb.getStringValue();
            if (!Tools.isIp(ip)) {
                ret = false;
            }
        }
        return ret;
    }

    /** Returns combo box for parameter. */
    @Override
    protected GuiComboBox getParamComboBox(final String param,
                                           final String prefix,
                                           final int width) {
        GuiComboBox paramCb;
        if ("ip".equals(param)) {
            /* get networks */
            String ip = getPreviouslySelected(param, prefix);
            if (ip == null) {
                ip = getParamSaved(param);
            }
            Info defaultValue;
            if (ip == null || "".equals(ip)) {
                defaultValue = new StringInfo(
                        Tools.getString("ClusterBrowser.SelectNetInterface"),
                        ip,
                        getBrowser());
            } else {
                defaultValue = new StringInfo(ip, ip, getBrowser());
            }
            final Info[] networks = enumToInfoArray(
                                    defaultValue,
                                    getName(),
                                    getBrowser().getNetworksNode().children());

            final String regexp = "^[\\d.*]*|Select\\.\\.\\.$";
            paramCb = new GuiComboBox(ip,
                                      networks,
                                      null, /* units */
                                      GuiComboBox.Type.COMBOBOX,
                                      regexp,
                                      width,
                                      null, /* abbrv */
                                      new AccessMode(
                                           getAccessType(param),
                                           isEnabledOnlyInAdvancedMode(param)));

            paramCb.setAlwaysEditable(true);
            paramComboBoxAdd(param, prefix, paramCb);
        } else {
            paramCb = super.getParamComboBox(param, prefix, width);
        }
        return paramCb;
    }

    /**
     * Returns string representation of the ip address.
     * In the form of 'ip (interface)'
     */
    @Override
    public String toString() {
        final String id = getService().getId();
        if (id == null) {
            return super.toString(); /* this is for 'new IPaddrInfo' */
        }

        final StringBuilder s = new StringBuilder(getName());
        final String inside = id + " / ";
        String ip = getParamSaved("ip");
        if (ip == null || "".equals(ip)) {
            ip = Tools.getString("ClusterBrowser.Ip.Unconfigured");
        }
        s.append(" (");
        s.append(inside);
        s.append(ip);
        s.append(')');
        return s.toString();
    }
}
