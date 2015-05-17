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
 * Here are commands for rhel version 4.
 */
public final class DistResource_redhatenterpriseas_4
                                        extends ListResourceBundle {

    private static final Object[][] contents = {
        /* distribution name that is used in the download url */
        {"distributiondir", "rhel4"},

        /* support */
        {"Support", "redhatenterpriseas-4"},

        /* Corosync/Openais/Pacemaker Opensuse */
        {"PmInst.install.text.1",
         "opensuse:ha-clustering repo: 1.0.x/0.80.x" },

        {"PmInst.install.1",
         "rm -rf /tmp/drbd-mc-hbinst/; "
         + "mkdir /tmp/drbd-mc-hbinst/"
         + " && wget -nd -r -np -P /tmp/drbd-mc-hbinst/ http://download.opensuse.org/repositories/server:/ha-clustering/RHEL_4/@ARCH@/"
         + " && rm /tmp/drbd-mc-hbinst/pacemaker-mgmt-*.rpm"
         + " && rm /tmp/drbd-mc-hbinst/ldirectord-*.rpm"
         + " && up2date lm_sensors net-snmp-libs libtool-libs perl-TimeDate"
         + " OpenIPMI-libs"
         + " && rpm -Uvh /tmp/drbd-mc-hbinst/*.rpm"
         + " && mv /etc/ais/openais.conf /etc/ais/openais.conf.orig;"
         + " if [ -e /etc/ais/openais.conf ];then"
         + " mv /etc/ais/openais.conf /etc/ais/openais.conf.orig; fi;"
         + " if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig; fi"},

        /* Heartbeat/Pacemaker Opensuse */
        {"HbPmInst.install.text.1",
         "opensuse:ha-clustering repo: 1.0.x/2.99.x" },

        {"HbPmInst.install.1",
         "rm -rf /tmp/drbd-mc-hbinst/; "
         + "mkdir /tmp/drbd-mc-hbinst/"
         + " && wget -nd -r -np -P /tmp/drbd-mc-hbinst/ http://download.opensuse.org/repositories/server:/ha-clustering/RHEL_4/@ARCH@/"
         + " && rm /tmp/drbd-mc-hbinst/pacemaker-mgmt-*.rpm"
         + " && rm /tmp/drbd-mc-hbinst/ldirectord-*.rpm"
         + " && rm /tmp/drbd-mc-hbinst/*-devel-*.rpm"
         + " && up2date lm_sensors net-snmp-libs libtool-libs perl-TimeDate"
         + " OpenIPMI-libs"
         + " && rpm -Uvh /tmp/drbd-mc-hbinst/*.rpm"
         + " && rm -rf /tmp/drbd-mc-hbinst/"},
    };

    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
