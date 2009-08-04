/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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

package drbd.configs;

import java.util.Arrays;

/**
 * Here are commands for rhel version 5.
 * TODO: rename it to rhel
 */
public class DistResource_redhatenterpriseserver_5 extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        /* distribution name that is used in the download url */
        {"distributiondir", "rhel5"},

        /* support */
        {"Support", "redhatenterpriseserver-5"},
        /* Openais/Pacemaker opensuse */
        {"AisPmInst.install.text.1", "http://download.opensuse.org" },

        {"AisPmInst.install.1",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://download.opensuse.org/repositories/server:/ha-clustering/RHEL_5/server:ha-clustering.repo && "
         + "yum -y install heartbeat.`uname -m"
         + "|sed s/i.86/i386/` pacemaker.`uname -m|sed s/i.86/i386/` && "
         + "/sbin/chkconfig --add openais && "
         + "mv /etc/ais/openais.conf /etc/ais/openais.conf.orig"},

        /* Openais/Pacemaker opensuse */
        {"HbPmInst.install.text.1",
         "http://download.opensuse.org" },

        {"HbPmInst.install.1",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://download.opensuse.org/repositories/server:/ha-clustering/RHEL_5/server:ha-clustering.repo && "
         + "yum -y install openais.`uname -m"
         + "|sed s/i.86/i386/` pacemaker.`uname -m|sed s/i.86/i386/` && "
         + "/sbin/chkconfig --add heartbeat"},

        /* old Heartbeat */
        {"HbPmInst.install.text.2", "the redhat way: possibly too old"},

        {"HbPmInst.install.2", "/usr/bin/yum -y install heartbeat"},
    };
}
