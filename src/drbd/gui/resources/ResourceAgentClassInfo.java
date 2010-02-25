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
package drbd.gui.resources;

import drbd.gui.Browser;
import drbd.gui.ClusterBrowser;
import drbd.data.CRMXML;
import drbd.data.ResourceAgent;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import javax.swing.ImageIcon;
import java.util.List;
import java.util.ArrayList;

/**
 * This class holds the information about resource agent class and its
 * services.
 */
public class ResourceAgentClassInfo extends HbCategoryInfo {
    /**
     * Prepares a new <code>ResourceAgentClassInfo</code> object.
     */
    public ResourceAgentClassInfo(final String name, final Browser browser) {
        super(name, browser);
    }

    /**
     * Returns columns for the table.
     */
    protected final String[] getColumnNames(final String tableName) {
        return new String[]{"Name", "Provider"};
    }
    /**
     * Returns data for the table.
     */
    protected final Object[][] getTableData(final String tableName) {
        final List<Object[]> rows = new ArrayList<Object[]>();
        /** Get classes */
        for (final ResourceAgent ra : getBrowser().getCRMXML().getServices(
                                                                  getName())) {
            final MyButton nameLabel = new MyButton(ra.getName());
            rows.add(new Object[]{nameLabel,
                                  ra.getProvider()});
        }
        return rows.toArray(new Object[rows.size()][]);
    }

    public final String toString() {
        return getName().toUpperCase();
    }
}
