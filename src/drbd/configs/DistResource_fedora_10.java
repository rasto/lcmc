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
 * Here are commands for fedora 10.
 */
public class DistResource_fedora_10 extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
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
         + " fi)"
         + " && (/sbin/chkconfig --del heartbeat;"
         + " /sbin/chkconfig --level 2345 corosync on"
         + " && /sbin/chkconfig --level 016 corosync off)"},

        /* Heartbeat/Pacemaker clusterlabs*/
        {"HbPmInst.install.text.1",
         "clusterlabs repo: 1.0.x/3.0.x" },

        {"HbPmInst.install.1",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://www.clusterlabs.org/rpm/fedora-10/clusterlabs.repo && "
         + "yum -y -x resource-agents-3.* -x openais-1* -x openais-0.9*"
         + " -x heartbeat-2.1* install pacemaker heartbeat"
         + " && /sbin/chkconfig --del corosync;"
         + " /sbin/chkconfig --level 2345 heartbeat on"
         + " && /sbin/chkconfig --level 016 heartbeat off"},

        /* Heartbeat/Pacemaker native */
        {"HbPmInst.install.text.2",
         "the fedora way: HB 2.1.x (obsolete)" },

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
         //+ "/sbin/chkconfig --add drbd && "
         + "/bin/rm -rf /tmp/drbdinst"},
    };
}
