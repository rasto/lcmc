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

package lcmc.configs;

import java.util.Arrays;

/**
 * Here are commands for all fedoras.
 */
public final class DistResource_fedora extends java.util.ListResourceBundle {

    /** Get contents. */
    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support", "fedora"},
        {"distribution", "redhat"},
        {"version:Fedora release 10*", "10"},
        {"version:Fedora release 11*", "11"},
        {"version:Fedora release 12*", "12"},
        {"version:Fedora release 13*", "13"},
        {"version:Fedora release 14*", "14"},
        {"version:Fedora release 15*", "15"},
        {"version:Fedora release 16*", "16"},

        /* directory capturing regexp on the website from the kernel version */
        {"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+.*?fc\\d+).*"},

        {"DrbdInst.install",
         DistResource.SUDO + "/bin/rpm -Uvh /tmp/drbdinst/@DRBDPACKAGES@"},

        /* DRBD native */
        {"DrbdInst.install.text.1",
         "yum install"},

        {"DrbdInst.install.1",
         "yum -y install drbd-utils drbd-udev "
         + "&& if ( rpm -qa|grep pacemaker ); then"
         + " yum -y install drbd-pacemaker; fi"},
        {"DrbdInst.install.method.1",       ""},

        /* Heartbeat/Pacemaker native */
        {"HbPmInst.install.text.1", ""},

        {"HbPmInst.install.1", ""},
        /* at least fedora 10 and fedora11 in version 2.1.3 and 2.14 has different
           ocf path. */
        {"Heartbeat.2.1.4.getOCFParameters",
         "export OCF_RESKEY_vmxpath=a;export OCF_ROOT=/usr/share/ocf;"
         + "mv /usr/lib/ocf/resource.d/linbit"
         + " /usr/share/ocf/resource.d/ 2>/dev/null;"
         + "for prov in `ls -1 /usr/share/ocf/resource.d/`; do "
         +  "for s in `ls -1 /usr/share/ocf/resource.d/$prov/ `; do "
         +  "echo -n 'provider:'; echo $prov;"
         +  "echo -n 'master:';"
         +  "grep -wl crm_master /usr/share/ocf/resource.d/$prov/$s;echo;"
         +  "/usr/share/ocf/resource.d/$prov/$s meta-data 2>/dev/null; done;"
         + "done;"
         + "echo 'provider:heartbeat';"
         + "echo 'master:';"
         + "@GUI-HELPER@ get-old-style-resources;"
         + "@GUI-HELPER@ get-lsb-resources"},

        /* Corosync/Pacemaker native */
        {"PmInst.install.text.1",
         "yum install: 1.1.x/1.4.x"},
        {"PmInst.install.1",
         "yum -y install pacemaker corosync"
         + "&& if ( rpm -qa|grep drbd ); then"
         + " yum -y install drbd-pacemaker; fi"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + "  mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},
        /* corosync/pacemaker from source */
        {"PmInst.install.text.9",
         "from source: latest/1.2.x"},

        {"PmInst.install.staging.9", "true"},

        {"PmInst.install.9",
         "export LCRSODIR=/usr/libexec/lcrso;"
         + "export CLUSTER_USER=hacluster;"
         + "export CLUSTER_GROUP=haclient;"
         + "/usr/bin/yum -y install autoconf automake libtool glib2-devel"
         + " libxml2-devel bzip2-devel libtool-ltdl-devel e2fsprogs-devel"
         + " net-snmp-devel subversion libxslt-devel libuuid-devel wget git"
         + " mercurial nss-devel"
         + " && /bin/mkdir -p /tmp/pminst "
         /* cluster glue */
         + " && cd /tmp/pminst/"
         + " && hg clone http://hg.linux-ha.org/glue"
         + " && cd glue/"
         + " && ./autogen.sh && ./configure"
         + " --with-daemon-user=${CLUSTER_USER}"
         + " --with-daemon-group=${CLUSTER_GROUP}"
         + " --disable-fatal-warnings"
         + " --sysconfdir=/etc --localstatedir=/var"
         + " && make && make install"
         /* resource agents */
         + " && cd /tmp/pminst/"
         + " && git clone git://github.com/ClusterLabs/resource-agents.git"
         + " && cd resource-agents"
         + " && sed 's/\\(SUBDIRS.*\\)doc/\\1/' Makefile.am>Makefile.am.new "
         + " && mv Makefile.am{.new,}"
         + " && ./autogen.sh && ./configure"
         + " --sysconfdir=/etc --localstatedir=/var"
         + " && make && make install"
         /* corosync */
         + " && cd /tmp/pminst"
         + " && export CORO_VERSION=1.4.2"
         + " && /usr/bin/wget"
         + " ftp://ftp:downloads@ftp.corosync.org/downloads/"
         + "corosync-$CORO_VERSION/corosync-$CORO_VERSION.tar.gz"
         + " && tar xvfzp corosync-*.tar.gz"
         + " && cd /tmp/pminst/corosync-*"
         + " && ./autogen.sh"
         + " && ./configure --with-lcrso-dir=$LCRSODIR"
         + " --sysconfdir=/etc --localstatedir=/var"
         + " && make"
         + " && make install"
         + " && (cp init/generic /etc/init.d/corosync"
         + "    || cp init/redhat /etc/init.d/corosync)"
         + " && chmod a+x /etc/init.d/corosync"
         + " && (groupadd ais;"
         + " useradd -g ais --shell /bin/false ais;"
         + " groupadd haclient;"
         + " useradd -g haclient --shell /bin/false hacluster;"
         + " true)"
         /* pacemaker */
         + " && cd /tmp/pminst"
         + " && git clone https://github.com/ClusterLabs/pacemaker"
         + " && cd /tmp/pminst/pacemaker"
         + " && ./autogen.sh"
         + " && ./configure --with-acl=yes --with-lcrso-dir=$LCRSODIR"
         + " --with-ais --sysconfdir=/etc --localstatedir=/var"
         + " --disable-fatal-warnings"
         + " && make && make install"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig; fi"},

        {"HbCheck.version",
         DistResource.SUDO + "@GUI-HELPER@ get-cluster-versions;"
         + "/bin/rpm -q -i openais|perl -lne"
         + " 'print \"ais:$1\" if /^Version\\s+:\\s+(\\S+)/';"
         + "/bin/rpm -q -i corosync|perl -lne"
         + " 'print \"cs:$1\" if /^Version\\s+:\\s+(\\S+)/'"},

        {"Heartbeat.addToRc",
         DistResource.SUDO + "/bin/systemctl enable heartbeat.service"},

        {"Heartbeat.deleteFromRc",
         DistResource.SUDO + "/bin/systemctl disable heartbeat.service"},

        {"Corosync.addToRc",
         DistResource.SUDO + "/bin/systemctl enable corosync.service"},

        {"Corosync.deleteFromRc",
         DistResource.SUDO + "/bin/systemctl disable corosync.service"},

        {"Openais.addToRc",
         DistResource.SUDO + "/bin/systemctl enable openais.service"},

        {"Openais.deleteFromRc",
         DistResource.SUDO + "/bin/systemctl disable openais.service"},

        {"KVM.emulator",    "/usr/bin/qemu-kvm"},

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
         + "/usr/bin/yum -y install kernel`uname -r|"
         + " grep -o '\\.PAEdebug\\|\\.PAE'"
         + "|tr . -`-devel-`uname -r|sed 's/\\.\\(PAEdebug\\|PAE\\)$//'` "
         + "|tee -a /dev/tty|grep 'No package'>/dev/null;"
         + "(if [ \"$?\" == 0 ]; then "
         + "echo \"you need to find and install kernel-devel-`uname -r`.rpm "
         + "package, or upgrade the kernel, sorry\";"
         + "exit 1; fi)"
         + "&& /usr/bin/yum -y install flex gcc make which && "
         + "cd /tmp/drbdinst && "
         + "/bin/tar xfzp drbd-@VERSION@.tar.gz && "
         + "cd drbd-@VERSION@ && "
         + "if [ -e configure ]; then"
         + " ./configure --prefix=/usr --with-km --localstatedir=/var"
         + " --sysconfdir=/etc;"
         + " fi && "
         + "make && make install DESTDIR=/ && "
         + "/bin/rm -rf /tmp/drbdinst"},

        {"libvirt.lxc.libpath", "/usr/libexec"},
        {"libvirt.xen.libpath", "/usr/lib/xen"},

        {"Corosync.startCorosync",
         DistResource.SUDO + "/sbin/service corosync start"},

        {"Corosync.startPcmk",
         DistResource.SUDO + "/sbin/service pacemaker start"},

        {"Corosync.stopCorosync",
         DistResource.SUDO + "/sbin/service corosync stop"},

        {"Corosync.stopCorosyncWithPcmk",
         DistResource.SUDO + "/sbin/service pacemaker stop && "
         + DistResource.SUDO + "/sbin/service corosync stop"},
        {"Corosync.startCorosyncWithPcmk",
         DistResource.SUDO + "/sbin/service corosync start;;;"
         + DistResource.SUDO + "/sbin/service pacemaker start"},
        {"Corosync.reloadCorosync",
         "if ! " + DistResource.SUDO + "/sbin/service corosync status >/dev/null 2>&1; then "
         + DistResource.SUDO + "/sbin/service corosync start; fi"},
    };

}
