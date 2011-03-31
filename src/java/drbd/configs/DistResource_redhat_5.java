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
 * Here are commands for centos verson 5.
 */
public final class DistResource_redhat_5 extends java.util.ListResourceBundle {

    /** Get contents. */
    @Override protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        /* Kernel versions and their counterpart in @KERNELVERSION@ variable in
         * the donwload url. Must begin with "kernel:" keyword. deprecated */

        /* distribution name that is used in the download url */
        {"distributiondir", "rhel5"},
        {"arch:i686", "i[3-6]86"},

        /* support */
        {"Support", "redhat-5"},

        /* Corosync/Openais/Pacemaker clusterlabs */
        {"PmInst.install.text.1",
         "clusterlabs repo: 1.0.x/1.2.x" },

        {"PmInst.install.1",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://www.clusterlabs.org/rpm/epel-5/clusterlabs.repo && "
         + " rpm -Uvh http://download.fedora.redhat.com/pub/epel/5/i386"
         + "/epel-release-5-4.noarch.rpm ; "
         + "(yum -y -x resource-agents-3.* -x openais-1* -x openais-0.9*"
         + " -x heartbeat-2.1* install pacemaker.@ARCH@ corosync.@ARCH@"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi)"
         + " && (/sbin/chkconfig --del heartbeat;"
         + " /sbin/chkconfig --level 2345 corosync on"
         + " && /sbin/chkconfig --level 016 corosync off)"},

        /* Next Corosync/Openais/Pacemaker clusterlabs */
        {"PmInst.install.text.2",
         "clusterlabs test repo: 1.1.x/1.2.x" },

        {"PmInst.install.staging.2", "true"},

        {"PmInst.install.2",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://www.clusterlabs.org/rpm-next/epel-5/clusterlabs.repo && "
         + " rpm -Uvh http://download.fedora.redhat.com/pub/epel/5/i386"
         + "/epel-release-5-4.noarch.rpm ; "
         + "(yum -y -x resource-agents-3.* -x openais-1* -x openais-0.9*"
         + " -x heartbeat-2.1* install pacemaker.@ARCH@ corosync.@ARCH@"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi)"
         + " && (/sbin/chkconfig --del heartbeat;"
         + " /sbin/chkconfig --level 2345 corosync on"
         + " && /sbin/chkconfig --level 016 corosync off)"},


        /* Heartbeat/Pacemaker clusterlabs */
        {"HbPmInst.install.text.1",
         "clusterlabs repo: 1.0.x/3.0.x" },

        {"HbPmInst.install.1",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://www.clusterlabs.org/rpm/epel-5/clusterlabs.repo && "
         + " rpm -Uvh http://download.fedora.redhat.com/pub/epel/5/i386"
         + "/epel-release-5-4.noarch.rpm ; "
         + "yum -y -x resource-agents-3.* -x openais-1* -x openais-0.9*"
         + " -x heartbeat-2.1* install pacemaker.@ARCH@ heartbeat.@ARCH@"
         + " && /sbin/chkconfig --add heartbeat"},

        /* old heartbeat */
        {"HbPmInst.install.text.2", "the centos way: HB 2.1.3 (obsolete)" },

        {"HbPmInst.install.2",
         "/usr/sbin/useradd hacluster 2>/dev/null; "
         + "/usr/bin/yum -y install heartbeat "
         + "&& /sbin/chkconfig --add heartbeat"},

        {"Openais.startOpenais.i686",
         DistResource.SUDO + "/etc/init.d/openais start"},
        {"Openais.reloadOpenais.i686",
         DistResource.SUDO + "/etc/init.d/openais reload"},

        /* Next Heartbeat/Pacemaker clusterlabs */
        {"HbPmInst.install.text.3",
         "clusterlabs next repo: 1.1.x/3.0.x" },

        {"HbPmInst.install.staging.3", "true"},

        {"HbPmInst.install.3",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://www.clusterlabs.org/rpm-next/epel-5/clusterlabs.repo && "
         + " rpm -Uvh http://download.fedora.redhat.com/pub/epel/5/i386"
         + "/epel-release-5-4.noarch.rpm ; "
         + "yum -y -x resource-agents-3.* -x openais-1* -x openais-0.9*"
         + " -x heartbeat-2.1* install pacemaker.@ARCH@ heartbeat.@ARCH@"
         + " && /sbin/chkconfig --add heartbeat"},
    };
}
