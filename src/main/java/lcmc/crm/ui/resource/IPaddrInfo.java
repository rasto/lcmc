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
package lcmc.crm.ui.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import lcmc.cluster.domain.Network;
import lcmc.cluster.domain.NetworkService;
import lcmc.cluster.ui.widget.Check;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Access;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.common.ui.treemenu.ClusterTreeMenu;
import lcmc.common.ui.utils.SwingUtils;

/**
 * This class holds info about IPaddr/IPaddr2 heartbeat service. It adds a better ip entering capabilities.
 */
@Named
final class IPaddrInfo extends ServiceInfo {
    private final WidgetFactory widgetFactory;
    private final NetworkService networkService;

    public IPaddrInfo(Application application, SwingUtils swingUtils, Access access, MainData mainData, WidgetFactory widgetFactory,
            ProgressIndicator progressIndicator, ServiceMenu serviceMenu, Provider<CloneInfo> cloneInfoProvider,
            ClusterTreeMenu clusterTreeMenu, CrmServiceFactory crmServiceFactory, NetworkService networkService) {
        super(application, swingUtils, access, mainData, widgetFactory, progressIndicator, serviceMenu, cloneInfoProvider,
                clusterTreeMenu, crmServiceFactory);
        this.widgetFactory = widgetFactory;
        this.networkService = networkService;
    }

    /**
     * Returns whether all the parameters are correct. If param is null, all paremeters will be checked, otherwise only the param,
     * but other parameters will be checked only in the cache. This is good if only one value is changed and we don't want to check
     * everything.
     */
    @Override
    public Check checkResourceFields(final String param, final String[] params) {
        final List<String> incorrect = new ArrayList<>();
        final List<String> changed = new ArrayList<>();
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
    protected Widget createWidget(final String param, final String prefix, final int width) {
        final Widget paramWi;
        if ("ip".equals(param)) {
            /* get networks */
            Value ip = getPreviouslySelected(param, prefix);
            if (ip == null) {
                ip = getParamSaved(param);
            }
            final Value defaultValue;
            if (ip.isNothingSelected()) {
                defaultValue = new StringValue(ip.getValueForConfig(),
                                               Tools.getString("ClusterBrowser.SelectNetInterface"));
            } else {
                defaultValue = new StringValue(ip.getValueForConfig());
            }
            final Collection<Network> networks = networkService.getCommonNetworks(getBrowser().getCluster());

            final Value[] networkValues = new Value[networks.size() + 1];
            networkValues[0] = defaultValue;
            int i = 1;
            for (final Network network : networks) {
                networkValues[i] = new StringValue(network.getName());
                i++;
            }

            final String regexp = "^[\\d.*]*|Select\\.\\.\\.$";
            paramWi = widgetFactory.createInstance(
                                 Widget.Type.COMBOBOX,
                                 ip,
                                 networkValues,
                                 regexp,
                                 width,
                                 Widget.NO_ABBRV,
                                 new AccessMode(getAccessType(param), isEnabledOnlyInAdvancedMode(param)),
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
            ip = new StringValue(Tools.getString("ClusterBrowser.Ip.Unconfigured"));
        }
        s.append(" (");
        s.append(inside);
        s.append(ip.getValueForConfig());
        s.append(')');
        return s.toString();
    }
}
