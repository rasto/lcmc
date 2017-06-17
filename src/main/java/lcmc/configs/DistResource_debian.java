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
 * Here are commands for all debians.
 */
public final class DistResource_debian extends ListResourceBundle {

    private static final Object[][] contents = {
        {"Support",             "debian"},
        //{"distribution",        "debian"},
        {"version:3*",          "3"},
        {"version:5*",          "LENNY"},
        {"version:6*",          "SQUEEZE"},
        {"version:8*",          "JESSIE"},
        {"version:debian-etch", "ETCH"},
        {"version:4*",          "ETCH"},

        {"arch:x86_64",       "amd64"}, // convert arch to arch in the drbd download file

        /* directory capturing regexp on the website from the kernel version */
        {"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+).*"},

        /* heartbeat/pacemaker */
        {"HbPmInst.install.text.1", "apt-get install"},
        {"HbPmInst.install.1",
         "apt-get update && /usr/bin/apt-get -y -q install -o"
         + " 'DPkg::Options::force=--force-confnew' heartbeat pacemaker"},

        /* corosync/pacemaker */
        {"PmInst.install.text.1", "apt-get install"},
        {"PmInst.install.1",
         "apt-get update && /usr/bin/apt-get -y -q install -o"
         + " 'DPkg::Options::force=--force-confnew' corosync pacemaker"
         + " && sed -i 's/\\(START=\\)no/\\1yes/' /etc/default/corosync"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},


        {"DrbdInst.install",
         DistResource.SUDO
         + "dpkg-divert --add --rename --package drbd8-module-`uname -r` "
         + "/lib/modules/`uname -r`/kernel/drivers/block/drbd/drbd.ko; "
         + "echo|"
         + DistResource.SUDO + "dpkg -i --force-confold /tmp/drbdinst/@DRBDPACKAGES@"},

        /* Drbd install method 2 */
        {"DrbdInst.install.text.2",
         "from the source tarball"},

        {"DrbdInst.install.staging.2", "true"},
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
         + "make && make install && "

         + "if [[ @UTIL-VERSION@ ]]; then "
         + "  /usr/bin/apt-get -y install xsltproc && "
         + "  /usr/bin/wget --directory-prefix=/tmp/drbdinst/ http://oss.linbit.com/drbd/@UTIL-VERSIONSTRING@ && "
         + "  cd /tmp/drbdinst && "
         + "  /bin/tar xfzp drbd-utils-@UTIL-VERSION@.tar.gz && "
         + "  cd drbd-utils-@UTIL-VERSION@ && "
         + "  if [ -e configure ]; then"
         + "    ./configure --prefix=/usr --localstatedir=/var --sysconfdir=/etc;"
         + "    make && make install ; "
         + "    if ! grep -ql udevrulesdir configure; then"
         + "        cp /lib/udev/65-drbd.rules /lib/udev/rules.d/;"
         + "    fi; "
         + "  fi; "
         + "fi; "
         + "/bin/rm -rf /tmp/drbdinst"},

        /* Drbd install method 3 */
        {"DrbdInst.install.text.3",
         "apt-get install"},

        {"DrbdInst.install.3",
         "apt-get update && /usr/bin/apt-get -y install -o "
         + "'DPkg::Options::force=--force-confnew' drbd8-utils"},

        {"HbCheck.version",
         DistResource.SUDO + "@GUI-HELPER@ get-cluster-versions;"
         + "/usr/bin/dpkg-query -f='${Status} ais:${Version}\n' -W openais 2>&1|grep '^install ok installed'|cut -d ' ' -f 4"
         + "|sed 's/-.*//'"},

        {"Heartbeat.deleteFromRc",
         DistResource.SUDO + "/usr/sbin/update-rc.d -f heartbeat remove"},

        {"Heartbeat.addToRc",
         DistResource.SUDO + "/usr/sbin/update-rc.d heartbeat start 75 2 3 4 5 . stop 05 0 1 6 . "},

        {"Corosync.addToRc",
         DistResource.SUDO + "/usr/sbin/update-rc.d corosync start 75 2 3 4 5 . stop 05 0 1 6 . "},

        {"Corosync.deleteFromRc",
         DistResource.SUDO + "/usr/sbin/update-rc.d -f corosync remove"},

        {"Openais.addToRc",
         DistResource.SUDO + "/usr/sbin/update-rc.d openais start 75 2 3 4 5 . stop 05 0 1 6 . "},

        {"Openais.deleteFromRc",
         DistResource.SUDO + "/usr/sbin/update-rc.d -f openais remove"},

        /* corosync/pacemaker from source */
        {"PmInst.install.text.3",
         "from source: latest/1.1.x"},

        {"PmInst.install.staging.3", "true"},

        {"PmInst.install.3",
         "export LCRSODIR=/usr/libexec/lcrso;"
         + "export CLUSTER_USER=hacluster;"
         + "export CLUSTER_GROUP=haclient;"
         + "apt-get update"
         + " && apt-get -y -q  --allow-unauthenticated install"
         + " -o 'DPkg::Options::force=--force-confnew'"
         + " bzip2 automake libtool make pkg-config libglib2.0-dev libxml2-dev"
         + " libbz2-dev uuid-dev libsnmp-dev subversion libxslt1-dev psmisc"
         + " libltdl3-dev libnss3-dev groff git mercurial"
         + " && /bin/mkdir -p /tmp/pminst "
         /* cluster glue */
         + " && cd /tmp/pminst/"
         + " && hg clone http://hg.linux-ha.org/glue"
         + " && cd glue/"
         + " && ./autogen.sh && ./configure "
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
         + " && ./autogen.sh && ./configure "
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
         /* pacemaker */
         + " && /usr/bin/wget -N -O /tmp/pminst/pacemaker.tar.bz2"
         + " http://hg.clusterlabs.org/pacemaker/1.1/archive/tip.tar.bz2"
         + " && cd /tmp/pminst"
         + " && /bin/tar xfjp pacemaker.tar.bz2"
         + " && cd `ls -dr Pacemaker-1-*`"
         + " && ./autogen.sh"
         + " && ./configure --with-lcrso-dir=$LCRSODIR"
         + " --with-ais --disable-fatal-warnings"
         + " --sysconfdir=/etc --localstatedir=/var"
         + " && make && make install"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},

         {"PmInst.install.files.2",
          "init-corosync-debian:/etc/init.d/corosync:755"
          + ":init-default-corosync-debian:/etc/default/corosync:644"},

        /* Heartbeat/pacemaker from source */
        {"HbPmInst.install.text.3",
         "from source: latest/3.0.x"},

        {"HbPmInst.install.staging.3", "true"},

        {"HbPmInst.install.3",
         "export LCRSODIR=/usr/libexec/lcrso;"
         + "export CLUSTER_USER=hacluster;"
         + "export CLUSTER_GROUP=haclient;"
         + "apt-get update"
         + " && apt-get -y -q  --allow-unauthenticated install"
         + " -o 'DPkg::Options::force=--force-confnew'"
         + " bzip2 automake libtool make pkg-config libglib2.0-dev libxml2-dev"
         + " libbz2-dev uuid-dev libsnmp-dev subversion libxslt1-dev psmisc"
         + " libltdl3-dev libnss3-dev groff xsltproc"
         + " && /bin/mkdir -p /tmp/pminst "
         /* cluster glue */
         + " && /usr/bin/wget -N -O /tmp/pminst/cluster-glue.tar.bz2"
         + " http://hg.linux-ha.org/glue/archive/tip.tar.bz2"
         + " && cd /tmp/pminst"
         + " && /bin/tar xfjp cluster-glue.tar.bz2"
         + " && cd `ls -dr Reusable-Cluster-Components-*`"
         + " && ./autogen.sh && ./configure "
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
         + " && ./autogen.sh && ./configure "
         + " --sysconfdir=/etc --localstatedir=/var"
         + " && make && make install"
         /* Heartbeat */
         + " && /usr/bin/wget -N -O /tmp/pminst/heartbeat.tar.bz2"
         + " http://hg.linux-ha.org/dev/archive/tip.tar.bz2"
         + " && cd /tmp/pminst"
         + " && /bin/tar xfjp heartbeat.tar.bz2"
         + " && cd `ls -dr Heartbeat-*`"
         + " && ./ConfigureMe install"
         + " && (addgroup --system --group haclient;"
         + " adduser --system --no-create-home --ingroup haclient"
         + " --disabled-login --shell /bin/false --disabled-password hacluster;"
         + " true)"
         /* Pacemaker */
         + " && /usr/bin/wget -N -O /tmp/pminst/pacemaker.tar.bz2"
         + " http://hg.clusterlabs.org/pacemaker/1.1/archive/tip.tar.bz2"
         + " && cd /tmp/pminst"
         + " && /bin/tar xfjp pacemaker.tar.bz2"
         + " && cd `ls -dr Pacemaker-1-*`"
         + " && ./autogen.sh"
         + " && ./configure --with-lcrso-dir=$LCRSODIR"
         + " --disable-fatal-warnings"
         + " --with-heartbeat --sysconfdir=/etc --localstatedir=/var"
         + " && make && make install"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},

        /* Proxy install method 1 */
        {"ProxyInst.install.text.1",
         "apt-get install"},

        {"ProxyInst.install.1",
         "apt-get install -y drbd-proxy"},

        {"ProxyCheck.version",
         "dpkg-query -W -f '${status}:${version}' drbd-proxy"
         + "|grep '^install ok installed:'|cut -d ':' -f 2"},
    };

    /** Get contents. */
    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
