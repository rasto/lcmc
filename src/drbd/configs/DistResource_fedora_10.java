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

        /* Openais/Pacemaker */
        {"AisPmInst.install.text.1",
         "http://download.opensuse.org" },

        {"AisPmInst.install.1",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://download.opensuse.org/repositories/server:/ha-clustering/Fedora_10/server:ha-clustering.repo && "
         + "(/usr/sbin/groupadd haclient 2>/dev/null && "
         + "/usr/sbin/useradd -g haclient hacluster 2>/dev/null;"
         + "yum -y install openais pacemaker && "
         + "/sbin/chkconfig --add openais)"},

        /* Heartbeat/Pacemaker */
        {"HbPmInst.install.text.1",
         "http://download.opensuse.org" },

        {"HbPmInst.install.1",
         "wget -N -nd -P /etc/yum.repos.d/"
         + " http://download.opensuse.org/repositories/server:/ha-clustering/Fedora_10/server:ha-clustering.repo && "
         + "(/usr/sbin/groupadd haclient 2>/dev/null && "
         + "/usr/sbin/useradd -g haclient hacluster 2>/dev/null;"
         + "yum -y install heartbeat pacemaker && "
         + "/sbin/chkconfig --add heartbeat)"},

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
         /* the fedora 10 does not keep old devel packages so for old kernels
          * the kernel-devel pacakge will be downloaded from rpmfind.net.
          */
         + "/usr/bin/yum -y install kernel`uname -r|"
         + " grep -o '\\.PAE\\|\\.xen\\|\\.kdump'"
         + "|tr . -`-devel-`uname -r|sed 's/\\.\\(PAE\\|xen\\|kdump\\)$//'` "
         + "|tee -a /dev/tty|grep 'No package'>/dev/null;"
         + "(if [ \"$?\" == 0 ]; then "
         + "wget --directory-prefix=/tmp/drbdinst/"
         + " ftp://fr.rpmfind.net/linux/fedora/updates/10/@ARCH@/kernel-devel-`uname -r`.rpm && "
         + "cd /tmp/drbdinst && "
         + "/bin/rpm -Uvh kernel-devel-`uname -r`.rpm; fi) && "
         + "/usr/bin/yum -y install flex gcc && "
         + "cd /tmp/drbdinst && "
         + "/bin/tar xfzp drbd-@VERSION@.tar.gz && "
         + "cd drbd-@VERSION@ && "
         + "make && make install && "
         + "/sbin/chkconfig --add drbd && "
         + "/bin/rm -rf /tmp/drbdinst"},
    };
}
