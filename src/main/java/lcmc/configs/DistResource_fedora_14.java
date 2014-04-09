/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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

package lcmc.configs;

import java.util.Arrays;
import java.util.ListResourceBundle;

/**
 * Here are commands for fedora 14.
 */
public final class DistResource_fedora_14 extends ListResourceBundle {

    /** Get contents. */
    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static final Object[][] contents = {
        {"Support", "fedora-14"},
        /* Heartbeat/Pacemaker native */
        {"HbPmInst.install.text.1",
         "yum install: 1.1.x/3.0.x"},

        {"HbPmInst.install.1",
         "yum -y install pacemaker heartbeat "
         + "&& if ( rpm -qa|grep drbd ); then"
         + " yum -y install drbd-pacemaker; fi"},

        {"Heartbeat.deleteFromRc",
         DistResource.SUDO + "/sbin/chkconfig --del heartbeat"},

        {"Heartbeat.addToRc",
         DistResource.SUDO + "/sbin/chkconfig --add heartbeat"},

        {"Corosync.addToRc",
         DistResource.SUDO + "/sbin/chkconfig --level 2345 corosync on "
         + "&& " + DistResource.SUDO + "/sbin/chkconfig --level 016 corosync off"},

        {"Corosync.deleteFromRc",
         DistResource.SUDO + "/sbin/chkconfig --del corosync"},

        {"Openais.addToRc",
         DistResource.SUDO + "/sbin/chkconfig --level 2345 openais on "
         + "&& " + DistResource.SUDO + "/sbin/chkconfig --level 016 openais off"},

        {"Openais.deleteFromRc",
         DistResource.SUDO + "/sbin/chkconfig --del openais"},

        {"Heartbeat.startHeartbeat",
         DistResource.SUDO + "/etc/init.d/heartbeat start"},

        {"Heartbeat.stopHeartbeat",
         DistResource.SUDO + "/etc/init.d/heartbeat stop"},

        {"Corosync.startCorosync",
         DistResource.SUDO + "/etc/init.d/corosync start"},

        {"Corosync.startPcmk",
         DistResource.SUDO + "/etc/init.d/pacemaker start"},

        {"Corosync.stopCorosync",
         DistResource.SUDO + "/etc/init.d/corosync stop"},

        {"Corosync.stopCorosyncWithPcmk",
         DistResource.SUDO + "/etc/init.d/pacemaker stop && "
         + DistResource.SUDO + "/etc/init.d/corosync stop"},
        {"Corosync.startCorosyncWithPcmk",
         DistResource.SUDO + "/etc/init.d/corosync start;;;"
         + DistResource.SUDO + "/etc/init.d/pacemaker start"},
        {"Corosync.reloadCorosync",
         "if ! " + DistResource.SUDO + "/etc/init.d/corosync status >/dev/null 2>&1; then "
         + DistResource.SUDO + "/etc/init.d/corosync start; fi"},
        {"Heartbeat.reloadHeartbeat",
         "if ! " + DistResource.SUDO + "/etc/init.d/heartbeat status >/dev/null 2>&1; then "
         + DistResource.SUDO + "/etc/init.d/heartbeat start; fi"},
    };
}
