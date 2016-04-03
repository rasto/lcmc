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
 * Here are commands for fedora 10.
 */
public final class DistResource_fedora_10 extends ListResourceBundle {

    private static final Object[][] contents = {
        {"Support", "fedora-10"},
        {"distribution", "redhat"},

        /* directory capturing regexp on the website from the kernel version */
        {"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+.*?fc\\d+).*"},

        /* Corosync/Openais/Pacemaker clusterlabs*/
        {"PmInst.install.text.1",
         "clusterlabs repo: 1.0.x/1.2.x" },

        {"PmInst.install.1",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://www.clusterlabs.org/rpm/fedora-10/clusterlabs.repo && "
         + "(yum -y -x resource-agents-3.* -x openais-1* -x openais-0.9*"
         + " -x heartbeat-2.1* -x stonith install pacemaker corosync"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi)"},

        /* Heartbeat/Pacemaker clusterlabs*/
        {"HbPmInst.install.text.1",
         "clusterlabs repo: 1.0.x/3.0.x" },

        {"HbPmInst.install.1",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://www.clusterlabs.org/rpm/fedora-10/clusterlabs.repo && "
         + "yum -y -x resource-agents-3.* -x openais-1* -x openais-0.9*"
         + " -x heartbeat-2.1* install pacemaker heartbeat"},

        /* Heartbeat/Pacemaker native */
        {"HbPmInst.install.text.2",
         "yum install: HB 2.1.x (obsolete)" },

        {"HbPmInst.install.2",
         "/usr/bin/yum -y install heartbeat"},

        /* no native drbd */
        {"DrbdInst.install.text.1",
         ""},

        /* Drbd install method 2 */
        {"DrbdInst.install.text.2",
         "from the source tarball"},

        {"DrbdInst.install.method.2",
         "source"},

        {"DrbdInst.install.staging.2", "true"},
        {"DrbdInst.install.2",
         "/bin/mkdir -p /tmp/drbdinst && "
         + "/usr/bin/wget --directory-prefix=/tmp/drbdinst/"
         + " http://oss.linbit.com/drbd/@VERSIONSTRING@ && "
         /* it installs eather kernel-devel- or kernel-PAE-devel-, etc. */
         /* the fedora 10 does not keep old devel packages.
          */
         + "/usr/bin/yum -y install kernel`uname -r|"
         + " grep -o '\\.PAE\\|\\.xen\\|\\.kdump'"
         + "|tr . -`-devel-`uname -r|sed 's/\\.\\(PAE\\|xen\\|kdump\\)$//'` "
         + "|tee -a /dev/tty|grep 'No package'>/dev/null;"
         + "(if [ \"$?\" == 0 ]; then "
         + "echo \"you need to find and install kernel-devel-`uname -r`.rpm "
         + "package, or upgrade the kernel, sorry\";"
         + "exit 1; fi)"
         + "&& /usr/bin/yum -y install flex gcc && "
         + "cd /tmp/drbdinst && "
         + "/bin/tar xfzp drbd-@VERSION@.tar.gz && "
         + "cd drbd-@VERSION@ && "
         + "if [ -e configure ]; then"
         + " ./configure --prefix=/usr --with-km --localstatedir=/var"
         + " --sysconfdir=/etc;"
         + " fi && "
         + "make && make install DESTDIR=/ && "
         + "/bin/rm -rf /tmp/drbdinst"},

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

    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
