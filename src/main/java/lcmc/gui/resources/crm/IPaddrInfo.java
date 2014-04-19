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
package lcmc.gui.resources.crm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lcmc.data.AccessMode;
import lcmc.data.ResourceAgent;
import lcmc.data.StringValue;
import lcmc.data.Value;
import lcmc.gui.Browser;
import lcmc.gui.widget.Check;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.Tools;

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
    public Check checkResourceFields(final String param,
                                     final String[] params) {
        final List<String> incorrect = new ArrayList<String>();
        final List<String> changed = new ArrayList<String>();
        final Check check = new Check(incorrect, changed);
        check.addCheck(super.checkResourceFields(param, params));
        final Widget wi;
        if (getResourceAgent().isHeartbeatClass()) {
            wi = getWidget("1", null);
        } else if (getResourceAgent().isOCFClass()) {
            wi = getWidget("ip", null);
        } else {
            return check;
        }
        if (wi == null) {
            return check;
        }
        wi.setEditable(true);
        wi.selectSubnet();
        final String ip = wi.getStringValue();
        if (!Tools.isIp(ip)) {
            incorrect.add("wrong ip");
        }
        return check;
    }

    /** Returns combo box for parameter. */
    @Override
    protected Widget createWidget(final String param,
                                  final String prefix,
                                  final int width) {
        final Widget paramWi;
        if ("ip".equals(param)) {
            /* get networks */
            Value ip = getPreviouslySelected(param, prefix);
            if (ip == null) {
                ip = getParamSaved(param);
            }
            final Value defaultValue;
            if (ip.isNothingSelected()) {
                defaultValue = new StringValue(
                        ip.getValueForConfig(),
                        Tools.getString("ClusterBrowser.SelectNetInterface"));
            } else {
                defaultValue = new StringValue(ip.getValueForConfig());
            }
            @SuppressWarnings("unchecked")
            final Value[] networks = enumToInfoArray(
                                    defaultValue,
                                    getName(),
                                    getBrowser().getNetworksNode().children());

            final String regexp = "^[\\d.*]*|Select\\.\\.\\.$";
            paramWi = WidgetFactory.createInstance(
                                 Widget.Type.COMBOBOX,
                                 ip,
                                 networks,
                                 regexp,
                                 width,
                                 Widget.NO_ABBRV,
                                 new AccessMode(
                                           getAccessType(param),
                                           isEnabledOnlyInAdvancedMode(param)),
                                 Widget.NO_BUTTON);

            paramWi.setAlwaysEditable(true);
            widgetAdd(param, prefix, paramWi);
        } else {
            paramWi = super.createWidget(param, prefix, width);
        }
        return paramWi;
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
        Value ip = getParamSaved("ip");
        if (ip == null || ip.isNothingSelected()) {
            ip = new StringValue(
                          Tools.getString("ClusterBrowser.Ip.Unconfigured"));
        }
        s.append(" (");
        s.append(inside);
        s.append(ip.getValueForConfig());
        s.append(')');
        return s.toString();
    }
}
