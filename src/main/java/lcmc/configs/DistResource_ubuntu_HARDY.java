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
 * Here are commands for ubuntu hardy heron.
 */
public final class DistResource_ubuntu_HARDY extends ListResourceBundle {

    private static final Object[][] contents = {
        {"Support",            "ubuntu-HARDY"},
        {"distributiondir",    "ubuntu-hardy-server"},

        /* Corosync/Openais/Pacemaker Opensuse */
        {"PmInst.install.text.1", "LaunchPad.net repo: 1.0.x/1.0.x"},

        {"PmInst.install.1",
         "echo 'deb http://ppa.launchpad.net/ubuntu-ha/ppa/ubuntu hardy main'"
         + " > /etc/apt/sources.list.d/ha-clustering.list "
         + " && apt-get update"
         + " && apt-get -y -q  --allow-unauthenticated install -o 'DPkg::Options::force=--force-confnew' pacemaker-openais"
         + " && (grep 'START=no' /etc/default/corosync && echo 'START=yes'>>/etc/default/corosync; true)"
         + " && if [ -e /etc/ais/openais.conf ];then"
         + " mv /etc/ais/openais.conf /etc/ais/openais.conf.orig; fi"
         + " && if [ -e /etc/corosync/corosync.conf ];then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig; fi"},

        /* Heartbeat/Pacemaker LaunchPad */
        {"HbPmInst.install.text.1", "LaunchPad.net repo: 1.0.x/2.99.x"},

        {"HbPmInst.install.1",
         "echo 'deb http://ppa.launchpad.net/ubuntu-ha/ppa/ubuntu hardy main'"
         + " > /etc/apt/sources.list.d/ha-clustering.list "
         + " && apt-get update"
         + " && apt-get -y -q  --allow-unauthenticated install -o 'DPkg::Options::force=--force-confnew' pacemaker-heartbeat"},

        {"HbPmInst.install.text.2", "apt-get install: HB 2.1.3 (obsolete)"},
        {"HbPmInst.install.2",
         "apt-get update && /usr/bin/apt-get -y install -o 'DPkg::Options::force=--force-confnew' heartbeat-2"},

        /* Drbd install method 2 */
        {"DrbdInst.install.text.2",
         "from the source tarball"},

        {"DrbdInst.install.staging.2", "true"},
        {"DrbdInst.install.2",
         "/bin/mkdir -p /tmp/drbdinst && "
         + "/usr/bin/wget --directory-prefix=/tmp/drbdinst/"
         + " http://oss.linbit.com/drbd/@VERSIONSTRING@ && "
         + "cd /tmp/drbdinst && "
         + "/bin/tar xfzp drbd-@VERSION@.tar.gz && "
         + "cd drbd-@VERSION@ && "
         + "/usr/bin/apt-get update && "
         + "/usr/bin/apt-get -y install build-essential flex linux-headers-`uname -r` && "
         + "dpkg-divert --add --rename --package drbd8-module-`uname -r` "
           + "/lib/modules/`uname -r`/ubuntu/block/drbd/drbd.ko && "
         + "if [ -e configure ]; then"
         + " ./configure --prefix=/usr --with-km --localstatedir=/var"
         + " --sysconfdir=/etc;"
         + " fi && "
         + "make && make install DESTDIR=/ && "
         + "/bin/rm -rf /tmp/drbdinst"},

        {"DrbdInst.install.method.2",
         "source"},

        /* Drbd install method 3 */
        {"DrbdInst.install.text.3",
         "apt-get install: 8.0.x (obsolete)"},

        {"Openais.reloadOpenais",
         DistResource.SUDO + "/etc/init.d/openais force-reload"},
        {"Corosync.reloadCorosync",
         DistResource.SUDO + "/etc/init.d/corosync force-reload"},

    };

    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
