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
 * Here are commands for rhel verson 7.
 */
public final class DistResource_redhatenterpriseserver_7 extends ListResourceBundle {

    private static final Object[][] contents = {
        /* distribution name that is used in the download url */
        {"distributiondir", "rhel7"},

        {"Support", "redhat-7"},

        {"HbPmInst.install.text.1", ""},
        {"HbPmInst.install.1", ""},

        {"PmInst.install.text.1", "yum install (centos repo)" },
        {"PmInst.install.1",
         "printf '[centos-7-base]\n"
         + "name=CentOS-$releasever - Base\n"
         + "mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=os\n"
         + "baseurl=http://mirror.centos.org/centos/7/os/$basearch/\n"
         + "enabled=1\n'"
         + " > /etc/yum.repos.d/centos-lcmc.repo"
         + " && /usr/bin/yum -y --nogpgcheck install corosync pacemaker"},
        {"Heartbeat.addToRc", DistResource.SUDO + "/bin/systemctl enable heartbeat.service"},
        {"Heartbeat.deleteFromRc", DistResource.SUDO + "/bin/systemctl disable heartbeat.service"},
        {"Corosync.addToRc", DistResource.SUDO + "/bin/systemctl enable corosync.service"},
        {"Corosync.deleteFromRc", DistResource.SUDO + "/bin/systemctl disable corosync.service"},
        {"Openais.addToRc", DistResource.SUDO + "/bin/systemctl enable openais.service"},
        {"Openais.deleteFromRc", DistResource.SUDO + "/bin/systemctl disable openais.service"},

        {"Corosync.startCorosync", DistResource.SUDO + "/sbin/service corosync start"},
        {"Corosync.startPcmk", DistResource.SUDO + "/sbin/service pacemaker start"},
        {"Corosync.stopCorosync", DistResource.SUDO + "/sbin/service corosync stop"},
        {"Corosync.stopCorosyncWithPcmk", DistResource.SUDO + "/sbin/service pacemaker stop && "
                                          + DistResource.SUDO + "/sbin/service corosync stop"},
        {"Corosync.startCorosyncWithPcmk", DistResource.SUDO + "/sbin/service corosync start;;;"
                                           + DistResource.SUDO + "/sbin/service pacemaker start"},
        {"Corosync.reloadCorosync", "if ! " + DistResource.SUDO + "/sbin/service corosync status >/dev/null 2>&1; then "
                                    + DistResource.SUDO + "/sbin/service corosync start; fi"},

    };

    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
