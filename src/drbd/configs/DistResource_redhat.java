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
 * Here are commands for all redhats.
 */
public class DistResource_redhat extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support", "redhat"},
        {"distribution", "redhat"},
        {"version:Red Hat Enterprise Linux ES release 4 (Nahant Update 2)", "4_Nahant_2"},
        {"version:CentOS release 5*", "5"},
        /* directory capturing regexp on the website from the kernel version */
        {"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+.*?el\\d+).*"},

        {"DrbdInst.install", "/bin/rpm -Uvh /tmp/drbdinst/@DRBDPACKAGE@ /tmp/drbdinst/@DRBDMODULEPACKAGE@"},
        {"HbPmInst.install.text.1", "the centos way: possibly too old"},
        {"HbPmInst.install.1", "/usr/bin/yum -y install heartbeat"},

        /* Drbd install method 2 */
        {"DrbdInst.install.text.2",
         "from the source tarball"},

        {"DrbdInst.install.method.2",
         "source"},

        {"DrbdInst.install.2",
         "/bin/mkdir -p /tmp/drbdinst && "
         + "/usr/bin/wget --directory-prefix=/tmp/drbdinst/"
         + " http://oss.linbit.com/drbd/@VERSIONSTRING@ && "
         + "cd /tmp/drbdinst && "
         + "/bin/tar xfzp drbd-@VERSION@.tar.gz && "
         + "cd drbd-@VERSION@ && "
         + "/usr/bin/yum -y install kernel`uname -r|"
             + " grep -o '5PAE\\|5xen\\|5debug'"
             + "|tr 5 -`-devel-`uname -r|sed 's/\\(PAE\\|xen\\|debug\\)$//'` &&"
         + "/usr/bin/yum -y install flex gcc && "
         + "make && make install && "
         + "/sbin/chkconfig --add drbd && "
         + "/bin/rm -rf /tmp/drbdinst"},

        /* Drbd install method 3 */
        {"DrbdInst.install.text.3",
         "the centos way: possibly too old"},

        {"DrbdInst.install.3",
         "/usr/bin/yum -y install kmod-drbd82 drbd82"},

        {"HbCheck.version",
         "/usr/local/bin/drbd-gui-helper get-cluster-versions;"
         + "/bin/rpm -q -i openais|perl -lne"
         + " 'print \"ais:$1\" if /^Version\\s+:\\s+(\\S+)/'"},

        {"Openais.removeHeartbeatAddOpenais",
         "/etc/init.d/heartbeat stop;/sbin/chkconfig --del heartbeat;"
         + "/etc/init.d/openais start && "
         + "/sbin/chkconfig --add openais"},

        {"Heartbeat.removeOpenaisAddHeartbeat",
         "/etc/init.d/openais stop;/sbin/chkconfig --del openais;"
         + "/etc/init.d/heartbeat start && "
         + "/sbin/chkconfig --add heartbeat"},

        {"Openais.addOpenaisToRc",
         "/sbin/chkconfig --add openais"},

        {"Heartbeat.addHeartbeatToRc",
         "/sbin/chkconfig --add heartbeat"},

        {"Openais.startOpenaisRc",
         "/etc/init.d/openais start;/sbin/chkconfig --add openais"},

        {"Heartbeat.startHeartbeatRc",
         "/etc/init.d/heartbeat start;/sbin/chkconfig --add heartbeat"},
    };
}
