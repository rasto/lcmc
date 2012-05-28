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
 * Here are commands for all redhats.
 */
public final class DistResource_redhat extends java.util.ListResourceBundle {

    /** Get contents. */
    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support", "redhat"},
        {"distribution", "redhat"},
        {"version:Red Hat Enterprise Linux ES release 4 (Nahant Update 2)", "4_Nahant_2"},
        {"version:CentOS release 5*", "5"},
        {"version:CentOS Linux release 6*", "6"},
        {"version:CentOS release 6*", "6"},
        /* directory capturing regexp on the website from the kernel version */
        {"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+.*?el\\d+).*"},

        {"DrbdInst.install",
         DistResource.SUDO + "/bin/rpm -Uvh /tmp/drbdinst/@DRBDPACKAGES@"},

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
         + "/usr/bin/yum -y install flex gcc make && "
         + "if [ -e configure ]; then"
         + " ./configure --prefix=/usr --with-km --localstatedir=/var"
         + " --sysconfdir=/etc;"
         + " fi && "
         + "make && make install DESTDIR=/ && "
         + "/bin/rm -rf /tmp/drbdinst"},

        /* Drbd install method 3 */
        {"DrbdInst.install.text.3",
         "yum install: 8.3.x"},

        {"DrbdInst.install.3",
         "/usr/bin/yum -y install kmod-drbd83 drbd83"},

        {"HbCheck.version",
         DistResource.SUDO + "@GUI-HELPER@ get-cluster-versions;"
         + "/bin/rpm -q -i openais|perl -lne"
         + " 'print \"ais:$1\" if /^Version\\s+:\\s+(\\S+)/'"},

        {"Heartbeat.deleteFromRc",
         DistResource.SUDO + "/sbin/chkconfig --del heartbeat"},

        {"Heartbeat.addToRc",
         DistResource.SUDO + "/sbin/chkconfig --add heartbeat"},

        {"Corosync.addToRc",
         DistResource.SUDO + "/sbin/chkconfig --add corosync"},

        {"Corosync.deleteFromRc",
         DistResource.SUDO + "/sbin/chkconfig --del corosync"},

        {"Openais.addToRc",
         DistResource.SUDO + "/sbin/chkconfig --add openais"},

        {"Openais.deleteFromRc",
         DistResource.SUDO + "/sbin/chkconfig --del openais"},

        /* corosync/pacemaker from source */
        {"PmInst.install.text.9",
         "from source: latest/1.1.x"},

        {"PmInst.install.staging.9", "true"},

        {"PmInst.install.9",
         "export LCRSODIR=/usr/libexec/lcrso;"
         + "export CLUSTER_USER=hacluster;"
         + "export CLUSTER_GROUP=haclient;"
         + "/usr/bin/yum -y install gcc autoconf automake libtool pkgconfig"
         + " glib2-devel libxml2-devel bzip2-devel e2fsprogs-devel"
         + " net-snmp-devel openssl-devel subversion libxslt-devel"
         + " libxml2-devel zlib-devel wget mercurial git make"
         + " && /bin/mkdir -p /tmp/pminst "
         /* cluster glue */
         + " && cd /tmp/pminst/"
         + " && hg clone http://hg.linux-ha.org/glue"
         + " && cd glue/"
         + " && ./autogen.sh && ./configure"
         + " --sysconfdir=/etc --localstatedir=/var"
         + " --with-daemon-user=${CLUSTER_USER}"
         + " --with-daemon-group=${CLUSTER_GROUP}"
         + " --disable-fatal-warnings"
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
         + " && svn co"
         + " http://svn.fedorahosted.org/svn/corosync/branches/flatiron"
         + " && cd /tmp/pminst/flatiron"
         + " && ./autogen.sh"
         + " && ./configure --with-lcrso-dir=$LCRSODIR"
         + " --sysconfdir=/etc --localstatedir=/var"
         + " && make"
         + " && make install"
         + " && cp init/redhat /etc/init.d/corosync"
         + " && chmod a+x /etc/init.d/corosync"
         + " && (groupadd ais;"
         + " useradd -g ais --shell /bin/false ais;"
         + " groupadd haclient;"
         + " useradd -g haclient --shell /bin/false hacluster;"
         + " true)"
         /* pacemaker */
         + " && /usr/bin/wget -N -O /tmp/pminst/pacemaker.tar.bz2"
         + " http://hg.clusterlabs.org/pacemaker/1.1/archive/tip.tar.bz2"
         + " && cd /tmp/pminst"
         + " && /bin/tar xfjp pacemaker.tar.bz2"
         + " && cd `ls -dr Pacemaker-1-*`"
         + " && echo 'docdir = ${datadir}/doc/${PACKAGE}'>>doc/Makefile.am"
         + " && echo 'docdir = ${datadir}/doc/${PACKAGE}'>>Makefile.am"
         + " && ./autogen.sh"
         + " && ./configure --with-lcrso-dir=$LCRSODIR"
         + " --with-ais --sysconfdir=/etc --localstatedir=/var"
         + " --disable-fatal-warnings"
         + " && make && make install"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig; fi"},
        {"KVM.emulator",    "/usr/libexec/qemu-kvm"},
        {"libvirt.lxc.libpath", "/usr/libexec"},
        {"libvirt.xen.libpath", "/usr/lib/xen"},
    };
}
