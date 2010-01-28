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
import drbd.data.CRMXML;
import drbd.data.ResourceAgent;
import drbd.utilities.Tools;
import javax.swing.ImageIcon;

/**
 * This class holds the information about heartbeat service from the ocfs,
 * to show it to the user.
 */
public class AvailableServiceInfo extends HbCategoryInfo {
    /** Info about the service. */
    private final ResourceAgent resourceAgent;
    /** Available services icon. */
    private static final ImageIcon AVAIL_SERVICES_ICON =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.ServiceStoppedIcon"));

    /**
     * Prepares a new <code>AvailableServiceInfo</code> object.
     */
    public AvailableServiceInfo(final ResourceAgent resourceAgent,
                                final Browser browser) {
        super(resourceAgent.getName(), browser);
        this.resourceAgent = resourceAgent;
    }

    /**
     * Returns heartbeat service class.
     */
    public final ResourceAgent getResourceAgent() {
        return resourceAgent;
    }

    /**
     * Returns icon for this menu category.
     */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        return AVAIL_SERVICES_ICON;
    }

    /**
     * Returns type of the info text. text/plain or text/html.
     */
    protected final String getInfoType() {
        return Tools.MIME_TYPE_TEXT_HTML;
    }

    /**
     * Returns the info about the service.
     */
    public final String getInfo() {
        final StringBuffer s = new StringBuffer(30);
        final CRMXML crmXML = getBrowser().getCRMXML();
        s.append("<h2>");
        s.append(getName());
        s.append(" (");
        s.append(crmXML.getVersion(resourceAgent));
        s.append(")</h2><h3>");
        s.append(crmXML.getShortDesc(resourceAgent));
        s.append("</h3>");
        s.append(crmXML.getLongDesc(resourceAgent));
        final String[] params = crmXML.getParameters(resourceAgent);
        for (final String param : params) {
            s.append(crmXML.getParamLongDesc(resourceAgent, param));
            s.append("<br>");
        }
        return s.toString();
    }
}
