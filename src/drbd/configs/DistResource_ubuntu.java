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
 * Here are commands for all ubuntus.
 */
public class DistResource_ubuntu extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support",                       "ubuntu"},
        {"version:7.04",                  ""},
        //{"version:5.0/9.10",              "KARMIC"},
        {"version:5.0/9.04",              "JAUNTY"},
        {"version:lenny/sid/8.10",        "INTREPID"},
        {"version:lenny/sid/8.04",        "HARDY"},
        {"version:testing/unstable/6.06", "DAPPER"},
        {"arch:x86_64",        "amd64"}, // convert arch to arch in the drbd download file
        /* directory capturing regexp on the website from the kernel version */
        {"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+).*"},

        {"DrbdInst.install",
         "dpkg -i --force-confold" + " /tmp/drbdinst/@DRBDPACKAGES@"},

        /* pacemaker heartbeat install method 1 */
        {"HbPmInst.install.text.1",
         "the ubuntu way: 1.0.x / 2.99.x"},

        {"HbPmInst.install.1",
         "apt-get update && /usr/bin/apt-get -y install -o"
         + " 'DPkg::Options::force=--force-confnew' pacemaker-heartbeat"},

        /* pacemaker corosync install method 1 */
        {"PmInst.install.text.1",
         "the ubuntu way: 1.0.x / 1.0.x"},

        {"PmInst.install.1",
         "apt-get update && /usr/bin/apt-get -y install -o"
         + " 'DPkg::Options::force=--force-confnew' pacemaker-openais "
         + " && (grep 'START=no' /etc/default/corosync && echo 'START=yes'>>/etc/default/corosync)"
         + " && if [ -e /etc/corosync/corosync.conf ];then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig; fi"},

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
         + "/usr/bin/apt-get update && "
         + "/usr/bin/apt-get -y install make flex linux-headers-`uname -r` && "
         + "dpkg-divert --add --rename --package drbd8-module-`uname -r` "
           + "/lib/modules/`uname -r`/kernel/ubuntu/drbd/drbd.ko && "
         + "if [ -e configure ]; then"
         + " ./configure --prefix=/usr --with-km --localstatedir=/var"
         + " --sysconfdir=/etc;"
         + " fi && "
         + "make && make install DESTDIR=/ && "
         //+ "/usr/sbin/update-rc.d drbd defaults 70 8 && "
         + "/bin/rm -rf /tmp/drbdinst"},

        /* Drbd install method 3 */
        {"DrbdInst.install.text.3",
         "the ubuntu way: 8.3.x"},

        {"DrbdInst.install.3",
         "apt-get update && /usr/bin/apt-get -y install -o "
         + "'DPkg::Options::force=--force-confnew'"
         + " linux-headers-`uname -r` drbd8-utils"},

        {"HbCheck.version",
         "@GUI-HELPER@ get-cluster-versions;"
         + "/usr/bin/dpkg-query -f='${Status} ais:${Version}\n' -W openais 2>&1|grep '^install ok installed'|cut -d ' ' -f 4"
         + "|sed 's/-.*//'"},

        {"Heartbeat.deleteFromRc",
         "/usr/sbin/update-rc.d heartbeat remove"},

        {"Heartbeat.addToRc",
         "/usr/sbin/update-rc.d heartbeat start 75 2 3 4 5 . stop 05 0 1 6 . "},

        {"Corosync.addToRc",
         "/usr/sbin/update-rc.d corosync start 75 2 3 4 5 . stop 05 0 1 6 . "},

        {"Corosync.deleteFromRc",
         "/usr/sbin/update-rc.d corosync remove"},

        {"Openais.addToRc",
         "/usr/sbin/update-rc.d openais start 75 2 3 4 5 . stop 05 0 1 6 . "},

        {"Openais.deleteFromRc",
         "/usr/sbin/update-rc.d openais remove"},

        /* corosync/pacemaker from source */
        {"PmInst.install.text.2",
         "from source: latest/1.1.x"},

        {"PmInst.install.2",
         "export LCRSODIR=/usr/libexec/lcrso;"
         + "export CLUSTER_USER=hacluster;"
         + "export CLUSTER_GROUP=haclient;"
         + "apt-get update"
         + " && apt-get -y -q  --allow-unauthenticated install"
         + " -o 'DPkg::Options::force=--force-confnew'"
         + " automake libtool make pkg-config libglib2.0-dev libxml2-dev"
         + " libbz2-dev uuid-dev libsnmp-dev subversion libxslt1-dev"
         + " libltdl3-dev libperl-dev libnss3-dev groff"
         + " && /bin/mkdir -p /tmp/pminst "
         /* cluster glue */
         + " && /usr/bin/wget -N -O /tmp/pminst/cluster-glue.tar.bz2"
         + " http://hg.linux-ha.org/glue/archive/tip.tar.bz2"
         + " && cd /tmp/pminst"
         + " && /bin/tar xfjp cluster-glue.tar.bz2"
         + " && cd `ls -dr Reusable-Cluster-Components-*`"
         + " && ./autogen.sh && ./configure"
         + " --with-daemon-user=${CLUSTER_USER}"
         + " --with-daemon-group=${CLUSTER_GROUP}"
         + " --disable-fatal-warnings"
         + " --sysconfdir=/etc --localstatedir=/var"
         + " && make && make install"
         /* resource agents */
         + " && /usr/bin/wget -N -O /tmp/pminst/resource-agents.tar.bz2"
         + " http://hg.linux-ha.org/agents/archive/tip.tar.bz2"
         + " && cd /tmp/pminst"
         + " && /bin/tar xfjp resource-agents.tar.bz2"
         + " && cd `ls -dr Cluster-Resource-Agents-*`"
         + " && ./autogen.sh && ./configure"
         + " --sysconfdir=/etc --localstatedir=/var"
         + " && make && make install"
         /* corosync */
         + " && cd /tmp/pminst"
         + " && svn co"
         + " http://svn.fedorahosted.org/svn/corosync/branches/flatiron"
         + " && cd /tmp/pminst/flatiron"
         + " && ./autogen.sh"
         + " && ./configure --with-lcrso-dir=$LCRSODIR"
         + " --sysconfdir=/etc --localstatedir=/var"
         + " && make"
         + " && make install"
         + " && (addgroup --system --group ais;"
         + " adduser --system --no-create-home --ingroup ais"
         + " --disabled-login --shell /bin/false --disabled-password ais;"
         + " addgroup --system --group haclient;"
         + " adduser --system --no-create-home --ingroup haclient"
         + " --disabled-login --shell /bin/false --disabled-password hacluster;"
         + " true)"
         + " && /usr/sbin/update-rc.d corosync start 75 2 3 4 5 ."
         + " stop 05 0 1 6 . "
         /* pacemaker */
         + " && /usr/bin/wget -N -O /tmp/pminst/pacemaker.tar.bz2"
         + " http://hg.clusterlabs.org/pacemaker/stable-1.0/archive/tip.tar.bz2"
         + " && cd /tmp/pminst"
         + " && /bin/tar xfjp pacemaker.tar.bz2"
         + " && cd `ls -dr Pacemaker-1-*`"
         + " && ./autogen.sh"
         + " && ./configure --with-lcrso-dir=$LCRSODIR"
         + " --with-ais --sysconfdir=/etc --localstatedir=/var"
         + " --disable-fatal-warnings"
         + " && make && make install"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},

         {"PmInst.install.files.2",
          "init-corosync-debian:/etc/init.d/corosync:755"
          + ":init-default-corosync-debian:/etc/default/corosync:644"},

    };
}
