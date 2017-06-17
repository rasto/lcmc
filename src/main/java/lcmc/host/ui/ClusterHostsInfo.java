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
package lcmc.host.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import lcmc.common.ui.CategoryInfo;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.host.domain.Host;
import lcmc.common.ui.Browser;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.crm.ui.resource.HostInfo;
import lcmc.common.ui.utils.MyButton;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This class holds the information hosts in this cluster.
 */
@Named
public final class ClusterHostsInfo extends CategoryInfo {
    @Inject
    private WidgetFactory widgetFactory;

    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    @Override
    protected String[] getColumnNames(final String tableName) {
        return new String[]{"Host", "DRBD", "Cluster Software"};
    }

    @Override
    protected Object[][] getTableData(final String tableName) {
        final List<Object[]> rows = new ArrayList<Object[]>();
        for (final Host host : getBrowser().getClusterHosts()) {
            final MyButton hostLabel = widgetFactory.createButton(host.getName(), HostBrowser.HOST_ICON_LARGE);
            hostLabel.setOpaque(true);
            rows.add(new Object[]{hostLabel,
                                  host.getDrbdInfoAboutInstallation(),
                                  host.getPacemakerInfo()});
        }
        return rows.toArray(new Object[rows.size()][]);
    }

    @Override
    protected void rowClicked(final String tableName, final String key, final int column) {
        // TODO: does not work
        final Host host = getBrowser().getCluster().getHostByName(key);
        final HostInfo hi = host.getBrowser().getHostInfo();
        if (hi != null) {
            hi.selectMyself();
        }
    }

    @Override
    protected Color getTableRowColor(final String tableName, final String key) {
        final Host host = getBrowser().getCluster().getHostByName(key);
        final Color c = host.getPmColors()[0];
        if (c == null) {
            return Browser.PANEL_BACKGROUND;
        } else {
            return c;
        }
    }
}
