/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH. *
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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
 * Here are commands for centos verson 5.
 */
public final class DistResource_redhat_5 extends ListResourceBundle {

    private static final Object[][] contents = {
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
         + " rpm -Uvh http://dl.fedoraproject.org/pub/epel/5/i386"
         + "/epel-release-5-4.noarch.rpm ; "
         + "(yum -y -x resource-agents-3.* -x openais-1* -x openais-0.9*"
         + " -x heartbeat-2.1* install pacemaker.@ARCH@ corosync.@ARCH@"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi)"},

        ///* Next Corosync/Openais/Pacemaker clusterlabs */
        //{"PmInst.install.text.2",
        // "clusterlabs test repo: 1.1.x/1.2.x" },

        //{"PmInst.install.staging.2", "true"},

        //{"PmInst.install.2",
        // "wget -N -nd -P /etc/yum.repos.d/"
        // + " http://www.clusterlabs.org/rpm-next/epel-5/clusterlabs.repo && "
        // + " rpm -Uvh http://dl.fedoraproject.org/pub/epel/5/i386"
        // + "/epel-release-5-4.noarch.rpm ; "
        // + "(yum -y -x resource-agents-3.* -x openais-1* -x openais-0.9*"
        // + " -x heartbeat-2.1* install pacemaker.@ARCH@ corosync.@ARCH@"
        // + " && if [ -e /etc/corosync/corosync.conf ]; then"
        // + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
        // + " fi)"},

        /* Corosync/Pacemaker clusterlabs next */
        {"PmInst.install.text.2",
         "clusterlabs repo: 1.1.x/1.4.x" },

        {"PmInst.install.2",
         "yum -y install wget && wget -N -nd -P /etc/yum.repos.d/"
         + " http://www.clusterlabs.org/rpm-next/rhel-5/clusterlabs.repo "
         + "&& rpm -Uvh http://dl.fedoraproject.org/pub/epel/5/i386"
         + "/epel-release-5-4.noarch.rpm ; "
         + " yum -y install pacemaker corosync"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},


        /* Heartbeat/Pacemaker clusterlabs */
        {"HbPmInst.install.text.1",
         "clusterlabs repo: 1.0.x/3.0.x" },

        {"HbPmInst.install.1",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://www.clusterlabs.org/rpm/epel-5/clusterlabs.repo && "
         + " rpm -Uvh http://dl.fedoraproject.org/pub/epel/5/i386"
         + "/epel-release-5-4.noarch.rpm ; "
         + "yum -y -x resource-agents-3.* -x openais-1* -x openais-0.9*"
         + " -x heartbeat-2.1* install pacemaker.@ARCH@ heartbeat.@ARCH@"},

        /* old heartbeat */
        {"HbPmInst.install.text.2", "yum install: HB 2.1.3 (obsolete)" },

        {"HbPmInst.install.2",
         "/usr/sbin/useradd hacluster 2>/dev/null; "
         + "/usr/bin/yum -y install heartbeat "},

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
         + " rpm -Uvh http://dl.fedoraproject.org/pub/epel/5/i386"
         + "/epel-release-5-4.noarch.rpm ; "
         + "yum -y -x resource-agents-3.* -x openais-1* -x openais-0.9*"
         + " -x heartbeat-2.1* install pacemaker.@ARCH@ heartbeat.@ARCH@"},
    };

    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
