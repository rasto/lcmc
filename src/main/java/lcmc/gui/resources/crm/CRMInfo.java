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

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import lcmc.model.Application;
import lcmc.gui.Browser;
import lcmc.utilities.Tools;

/**
 * This class holds data that describe the crm as whole.
 */
public final class CRMInfo extends HbCategoryInfo {
    /** Cluster manager icon. */
    private static final ImageIcon CRM_ICON =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.PacemakerIconSmall"));
    /** Prepares a new {@code ServicesInfo} object. */
    public CRMInfo(final String name, final Browser browser) {
        super(name, browser);
    }

    /** Returns icon for the heartbeat menu item. */
    @Override
    public ImageIcon getCategoryIcon(final Application.RunMode runMode) {
        return CRM_ICON;
    }

    /** Returns editable info panel for global crm config. */
    @Override
    public JComponent getInfoPanel() {
        return getBrowser().getServicesInfo().getInfoPanel();
    }
}
