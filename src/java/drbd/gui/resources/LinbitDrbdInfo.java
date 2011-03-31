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
import drbd.data.ResourceAgent;
import drbd.data.Host;

import java.util.Map;

/**
 * linbit::drbd info class is used for drbd pacemaker service that is
 * treated in special way.
 */
final class LinbitDrbdInfo extends ServiceInfo {
    /** Creates new LinbitDrbdInfo object. */
    LinbitDrbdInfo(final String name,
                   final ResourceAgent ra,
                   final Browser browser) {
        super(name, ra, browser);
    }

    /** Creates new linbit::drbd info object. */
    LinbitDrbdInfo(final String name,
                   final ResourceAgent ra,
                   final String hbId,
                   final Map<String, String> resourceNode,
                   final Browser browser) {
        super(name, ra, hbId, resourceNode, browser);
    }

    /** Returns string representation of the linbit::drbd service. */
    @Override public String toString() {
        final StringBuffer s = new StringBuffer(30);
        final String provider = getResourceAgent().getProvider();
        if (!HB_HEARTBEAT_PROVIDER.equals(provider)
            && !"".equals(provider)) {
            s.append(provider);
            s.append(':');
        }
        s.append(getName());
        final String string = getParamSaved("drbd_resource");
        if (string == null) {
            s.insert(0, "new ");
        } else {
            if (!"".equals(string)) {
                s.append(" (");
                s.append(string);
                s.append(')');
            }
        }
        return s.toString();
    }

    /** Returns resource name. */
    String getResourceName() {
        return getParamSaved("drbd_resource");
    }

    /** Sets resource name. TODO: not used? */
    void setResourceName(final String resourceName) {
        getResource().setValue("drbd_resource", resourceName);
    }

    /** Removes the linbit::drbd service. */
    @Override public void removeMyselfNoConfirm(final Host dcHost,
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
    @Override void setParameters(final Map<String, String> resourceNode) {
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
            //final Thread t = new Thread(new Runnable() {
            //    @Override public void run() {
            //        dri.updateMenus(null);
            //    }
            //});
            //t.start();
        }
    }
}
