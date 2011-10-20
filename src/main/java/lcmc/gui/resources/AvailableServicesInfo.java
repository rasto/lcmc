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
import lcmc.gui.ClusterBrowser;
import lcmc.utilities.MyButton;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

/**
 * This class holds the information about available resource agent classes.
 */
public final class AvailableServicesInfo extends HbCategoryInfo {
    /** Prepares a new <code>AvailableServicesInfo</code> object. */
    public AvailableServicesInfo(final String name, final Browser browser) {
        super(name, browser);
    }

    /** Returns columns for the table. */
    @Override protected String[] getColumnNames(final String tableName) {
        return new String[]{"Name", "Description"};
    }

    /** Returns data for the table. */
    @Override protected Object[][] getTableData(final String tableName) {
        final List<Object[]> rows = new ArrayList<Object[]>();
        /** Get classes */
        for (final String cl : ClusterBrowser.HB_CLASSES) {
            final MyButton className = new MyButton(cl.toUpperCase(Locale.US));
            rows.add(new Object[]{className,
                                  ClusterBrowser.HB_CLASS_MENU.get(cl)});
        }
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Execute when row in the table was clicked. */
    @Override protected void rowClicked(final String tableName,
                                        final String key,
                                        final int column) {
        final ResourceAgentClassInfo raci =
                     getBrowser().getClassInfoMap(key.toLowerCase(Locale.US));
        if (raci != null) {
            raci.selectMyself();
        }
    }
}
