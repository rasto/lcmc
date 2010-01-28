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
import drbd.utilities.Tools;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

/**
 * This class holds data that describe the crm as whole.
 */
public class CRMInfo extends HbCategoryInfo {
    /**
     * Prepares a new <code>ServicesInfo</code> object.
     */
    public CRMInfo(final String name, final Browser browser) {
        super(name, browser);
    }

    /**
     * Returns icon for the heartbeat menu item.
     */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        return null;
    }

    /**
     * Returns type of the info text. text/plain or text/html.
     */
    protected final String getInfoType() {
        return Tools.MIME_TYPE_TEXT_HTML;
    }
    /**
     * Returns editable info panel for global crm config.
     */
    public final JComponent getInfoPanel() {
        return
             getBrowser().getHeartbeatGraph().getServicesInfo().getInfoPanel();
    }
    ///**
    // * Returns info for the Heartbeat menu.
    // */
    //public String getInfo() {
    //    final StringBuffer s = new StringBuffer(30);
    //    s.append("<h2>");
    //    s.append(getName());
    //    if (crmXML == null) {
    //        s.append("</h2><br>info not available");
    //        return s.toString();
    //    }

    //    final Host[] hosts = getClusterHosts();
    //    int i = 0;
    //    final StringBuffer hbVersion = new StringBuffer();
    //    boolean differentHbVersions = false;
    //    for (Host host : hosts) {
    //        if (i == 0) {
    //            hbVersion.append(host.getHeartbeatVersion());
    //        } else if (!hbVersion.toString().equals(
    //                                        host.getHeartbeatVersion())) {
    //            differentHbVersions = true;
    //            hbVersion.append(", ");
    //            hbVersion.append(host.getHeartbeatVersion());
    //        }

    //        i++;
    //    }
    //    s.append(" (" + hbVersion.toString() + ")</h2><br>");
    //    if (differentHbVersions) {
    //        s.append(Tools.getString(
    //                        "ClusterBrowser.DifferentHbVersionsWarning"));
    //        s.append("<br>");
    //    }
    //    return s.toString();
    //}
}

