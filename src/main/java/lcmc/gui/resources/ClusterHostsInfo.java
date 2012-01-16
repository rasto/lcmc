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
import lcmc.gui.HostBrowser;
import lcmc.gui.ClusterBrowser;
import lcmc.data.Host;
import lcmc.utilities.MyButton;
import java.util.List;
import java.util.ArrayList;
import java.awt.Color;

/**
 * This class holds the information hosts in this cluster.
 */
public final class ClusterHostsInfo extends CategoryInfo {
    /** Prepares a new <code>ClusterHostsInfo</code> object. */
    public ClusterHostsInfo(final String name, final Browser browser) {
        super(name, browser);
    }

    /** Returns browser object of this info. */
    @Override
    protected ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** Returns columns for the table. */
    @Override
    protected String[] getColumnNames(final String tableName) {
        return new String[]{"Host", "DRBD", "Cluster Software"};
    }

    /** Returns data for the table. */
    @Override
    protected Object[][] getTableData(final String tableName) {
        final List<Object[]> rows = new ArrayList<Object[]>();
        for (final Host host : getBrowser().getClusterHosts()) {
            final MyButton hostLabel = new MyButton(
                                                host.getName(),
                                                HostBrowser.HOST_ICON_LARGE);
            hostLabel.setOpaque(true);
            rows.add(new Object[]{hostLabel,
                                  host.getBrowser().getDrbdInfo(),
                                  host.getBrowser().getPacemakerInfo()});
        }
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Execute when row in the table was clicked. */
    @Override
    protected void rowClicked(final String tableName,
                              final String key,
                              final int column) {
        // TODO: does not work
        final Host host = getBrowser().getCluster().getHostByName(key);
        final HostInfo hi = host.getBrowser().getHostInfo();
        if (hi != null) {
            hi.selectMyself();
        }
    }

    /** Retrurns color for some rows. */
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
