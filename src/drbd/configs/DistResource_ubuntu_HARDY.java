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
 * Here are commands for ubuntu hardy heron.
 */
public class DistResource_ubuntu_HARDY extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support",            "ubuntu-HARDY"},
        {"distributiondir",    "ubuntu-hardy-server"},

        /* Corosync/Openais/Pacemaker Opensuse */
        {"PmInst.install.text.1", "http://LaunchPad.net"},

        {"PmInst.install.1",
         "echo 'deb http://ppa.launchpad.net/ubuntu-ha/ppa/ubuntu hardy main'"
         + " > /etc/apt/sources.list.d/ha-clustering.list "
         + " && apt-get update"
         + " && apt-get -y -q  --allow-unauthenticated install -o 'DPkg::Options::force=--force-confnew' pacemaker-openais"
         + " && (grep 'START=no' /etc/default/corosync && echo 'START=yes'>>/etc/default/corosync)"
         + " && if [ -e /etc/ais/openais.conf ];then"
         + " mv /etc/ais/openais.conf /etc/ais/openais.conf.orig; fi"
         + " && if [ -e /etc/corosync/corosync.conf ];then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig; fi"},

        {"PmInst.install.text.2", "http://download.opensuse.org"},

        {"PmInst.install.2",
         "echo 'deb http://download.opensuse.org/repositories/server:/ha-clustering/xUbuntu_8.04/ ./' > /etc/apt/sources.list.d/ha-clustering.list "
         + " && apt-get update"
         + " && apt-get -y -q  --allow-unauthenticated install"
         + " -o 'DPkg::Options::force=--force-confnew' pacemaker"
         + " && (grep 'START=no' /etc/default/openais && echo 'START=yes'>>/etc/default/openais)"
         + " && if [ -e /etc/ais/openais.conf ];then"
         + " mv /etc/ais/openais.conf /etc/ais/openais.conf.orig; fi"
         + " && if [ -e /etc/corosync/corosync.conf ];then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig; fi"},

        /* Heartbeat/Pacemaker LaunchPad */
        {"HbPmInst.install.text.1", "http://LaunchPad.net"},

        {"HbPmInst.install.1",
         "echo 'deb http://ppa.launchpad.net/ubuntu-ha/ppa/ubuntu hardy main'"
         + " > /etc/apt/sources.list.d/ha-clustering.list "
         + " && apt-get update"
         + " && apt-get -y -q  --allow-unauthenticated install -o 'DPkg::Options::force=--force-confnew' pacemaker-heartbeat"},

        {"HbPmInst.install.text.3", "http://download.opensuse.org"},

        {"HbPmInst.install.2",
         "echo 'deb http://download.opensuse.org/repositories/server:/ha-clustering/xUbuntu_8.04/ ./'"
         + " > /etc/apt/sources.list.d/ha-clustering.list "
         + " && apt-get update"
         + " && apt-get -y -q  --allow-unauthenticated install -o 'DPkg::Options::force=--force-confnew' heartbeat pacemaker"},

        {"HbPmInst.install.text.3", "apt-get"},
        {"HbPmInst.install.3",
         "apt-get update && /usr/bin/apt-get -y install -o 'DPkg::Options::force=--force-confnew' heartbeat-2"},

        /* Drbd install method 2 */
        {"DrbdInst.install.text.2",
         "from the source tarball"},

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
         + "make && make install && "
         + "/usr/sbin/update-rc.d drbd defaults 70 8 && "
         + "/bin/rm -rf /tmp/drbdinst"},

        {"DrbdInst.install.method.2",
         "source"},

        {"Openais.reloadOpenais",  "/etc/init.d/openais force-reload"},

    };
}
