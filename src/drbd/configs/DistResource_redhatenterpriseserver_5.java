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
        {"HbPmInst.install.text.1",
         "opensuse:ha-clustering repo: 1.0.x/2.99.x" },

        /* Heartbeat/Pacemaker opensuse */
        {"HbPmInst.install.1",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://download.opensuse.org/repositories/server:/ha-clustering/RHEL_5/server:ha-clustering.repo && "
         + "yum -y install resource-agents heartbeat.`uname -m"
         + "|sed s/i.86/i386/` pacemaker.`uname -m|sed s/i.86/i386/` && "
         + "/sbin/chkconfig --add heartbeat"},

        {"HbPmInst.install.1.i686",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://download.opensuse.org/repositories/server:/ha-clustering/RHEL_5/server:ha-clustering.repo && "
         + "yum -y install resource-agents heartbeat.`uname -m"
         + "|sed s/i.86/i386/` pacemaker.`uname -m|sed s/i.86/i386/` && "
         + "chmod g+w /var/run/heartbeat/ccm/ &&"
         + "/sbin/chkconfig --add heartbeat"},

        /* Corosync/Openais/Pacemaker clusterlabs */
        {"PmInst.install.text.1",
         "clusterlabs repo: 1.0.x/1.1.x" },

        {"PmInst.install.1",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://www.clusterlabs.org/rpm/epel-5/clusterlabs.repo && "
         + " rpm -Uvh http://download.fedora.redhat.com/pub/epel/5/i386"
         + "/epel-release-5-3.noarch.rpm && "
         + "(yum -y -x resource-agents-3.* -x openais-1* -x openais-0.9*"
         + " -x heartbeat-2.1* install pacemaker corosync"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi)"
         + " && (/sbin/chkconfig --del heartbeat;"
         + " /sbin/chkconfig --level 2345 corosync on"
         + " && /sbin/chkconfig --level 016 corosync off)"},

        /* Corosync/Openais/Pacemaker opensuse */
        {"PmInst.install.text.2",
         "opensuse:ha-clustering repo: 1.0.x/0.80.x" },

        {"PmInst.install.2.i686",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://download.opensuse.org/repositories/server:/ha-clustering/RHEL_5/server:ha-clustering.repo && "
         + "yum -y install resource-agents.`uname -m"
         + "|sed s/i.86/i386/` pacemaker.`uname -m|sed s/i.86/i386/` && "
         + "chmod g+w /var/run/heartbeat/ccm/ &&"
         + "((/sbin/chkconfig --level 2345 corosync on"
         + "  && /sbin/chkconfig --level 016 corosync off)"
         + " || (/sbin/chkconfig --level 2345 openais on"
         + "     && /sbin/chkconfig --level 016 openais off));"
         + " if [ -e /etc/ais/openais.conf ];then"
         + " mv /etc/ais/openais.conf /etc/ais/openais.conf.orig; fi;"
         + " if [ -e /etc/corosync/corosync.conf ];then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig; fi;"},

        {"PmInst.install.2",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://download.opensuse.org/repositories/server:/ha-clustering/RHEL_5/server:ha-clustering.repo && "
         + "yum -y install resource-agents.`uname -m"
         + "|sed s/i.86/i386/` pacemaker.`uname -m|sed s/i.86/i386/` && "
         + "((/sbin/chkconfig --level 2345 corosync on"
         + "  && /sbin/chkconfig --level 016 corosync off)"
         + " || (/sbin/chkconfig --level 2345 openais on"
         + "     && /sbin/chkconfig --level 016 openais off));"
         + " if [ -e /etc/ais/openais.conf ];then"
         + " mv /etc/ais/openais.conf /etc/ais/openais.conf.orig; fi;"
         + " if [ -e /etc/corosync/corosync.conf ];then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig; fi;"},

        /* Workaround, where aisexec hangs the gui if called directly. */
        {"Openais.startOpenais.i686",
         "echo '/etc/init.d/openais start'|at now"},
        {"Openais.reloadOpenais.i686",
         "echo '/etc/init.d/openais reload'|at now"},
    };
}
