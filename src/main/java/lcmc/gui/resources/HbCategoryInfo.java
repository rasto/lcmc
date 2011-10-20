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
import lcmc.utilities.Tools;

import javax.swing.JPanel;
/**
 * This class is used for all kind of categories in the heartbeat
 * hierarchy. Its point is to show heartbeat graph all the time, ane
 * heartbeat category is clicked.
 */
public class HbCategoryInfo extends CategoryInfo {
    /** Creates the new HbCategoryInfo object with name of the category. */
    public HbCategoryInfo(final String name, final Browser browser) {
        super(name, browser);
    }

    /** Returns browser object of this info. */
    @Override protected ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }


    /** Returns type of the info text. text/plain or text/html. */
    @Override protected String getInfoType() {
        return Tools.MIME_TYPE_TEXT_HTML;
    }

    /** Returns heartbeat graph. */
    @Override public final JPanel getGraphicalView() {
        return getBrowser().getHeartbeatGraph().getGraphPanel();
    }
}
