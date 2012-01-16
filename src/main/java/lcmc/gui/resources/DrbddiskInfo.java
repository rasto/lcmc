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
import lcmc.data.ResourceAgent;
import lcmc.data.Host;

import java.util.Map;

/**
 * DrbddiskInfo class is used for drbddisk heartbeat service that is
 * treated in special way.
 */
final class DrbddiskInfo extends ServiceInfo {

    /** Creates new DrbddiskInfo object. */
    DrbddiskInfo(final String name,
                 final ResourceAgent ra,
                 final Browser browser) {
        super(name, ra, browser);
    }

    /** Creates new DrbddiskInfo object. */
    DrbddiskInfo(final String name,
                 final ResourceAgent ra,
                 final String resourceName,
                 final Browser browser) {
        super(name, ra, browser);
        getResource().setValue("1", resourceName);
    }

    /** Creates new DrbddiskInfo object. */
    DrbddiskInfo(final String name,
                 final ResourceAgent ra,
                 final String hbId,
                 final Map<String, String> resourceNode,
                 final Browser browser) {
        super(name, ra, hbId, resourceNode, browser);
    }

    /** Returns string representation of the drbddisk service. */
    @Override
    public String toString() {
        return getName() + " (" + getParamSaved("1") + ")";
    }

    /** Returns resource name / parameter "1". */
    String getResourceName() {
        return getParamSaved("1");
    }

    /** Sets resource name / parameter "1". TODO: not used? */
    void setResourceName(final String resourceName) {
        getResource().setValue("1", resourceName);
    }

    /** Removes the drbddisk service. */
    @Override
    public void removeMyselfNoConfirm(final Host dcHost,
                                      final boolean testOnly) {
        super.removeMyselfNoConfirm(dcHost, testOnly);
        final DrbdResourceInfo dri =
                        getBrowser().getDrbdResHash().get(getResourceName());
        getBrowser().putDrbdResHash();
        if (dri != null) {
            dri.setUsedByCRM(null);
        }
    }

    /** Sets service parameters with values from resourceNode hash. */
    void setParameters(final Map<String, String> resourceNode) {
        super.setParameters(resourceNode);
        final DrbdResourceInfo dri =
                        getBrowser().getDrbdResHash().get(getResourceName());
        getBrowser().putDrbdResHash();
        if (dri != null) {
            if (isManaged(false) && !getService().isOrphaned()) {
                dri.setUsedByCRM(this);
            } else {
                dri.setUsedByCRM(null);
            }
            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    dri.updateMenus(null);
                }
            });
            t.start();
        }
    }
}
