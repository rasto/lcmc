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
 * Here are commands for debian verson 4 (etch).
 */
public class DistResource_debian_ETCH extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        /* distribution name that is used in the download url */
        {"distributiondir", "debian-etch"},

        /* support */
        {"Support", "debian-4"},

        {"DrbdAvailFiles",
         "/usr/bin/wget --no-check-certificate -q http://www.linbit.com/"
         + "@SUPPORTDIR@/@DRBDDIR@-@DRBDVERSION@/@DISTRIBUTION@/@KERNELVERSIONDIR@/"
         + " -O - |perl -ple '"
         + "(undef, $ver) = split /\\s+/, `dpkg-query -W linux-image-\\`uname -r\\``;"
         + "($_) = m!href=\"(drbd8?-(?:plus8?-)?(?:utils)?"
         + "(?:(?:km|module|utils)[_-]@BUILD@)?[-_]?@DRBDVERSION@"
         + "-0(?:\\+$ver.*?)?[._]@ARCH@"
         + "\\.(?:rpm|deb))\"! or goto LINE'"
        },

        /* Heartbeat/Pacemaker Opensuse */
        {"HbPmInst.install.text.1", "http://download.opensuse.org repository"},
        {"HbPmInst.install.1",
         "echo 'deb http://download.opensuse.org/repositories/server:/ha-clustering/Debian_Etch/ ./' > /etc/apt/sources.list.d/ha-clustering.list "
         + " && apt-get update"
         + " && apt-get -y -q --allow-unauthenticated install"
         + " -o 'DPkg::Options::force=--force-confnew' heartbeat pacemaker"
         + " && /usr/sbin/update-rc.d heartbeat start 75 2 3 4 5 . stop 05 0 1 6 . "},

        {"HbPmInst.install.text.2", "the debian way"},
        {"HbPmInst.install.2",
         "apt-get update && /usr/bin/apt-get -y -q install -o 'DPkg::Options::force=--force-confnew' heartbeat-2"},

        /* Corosync/Heartbeat/Pacemaker Opensuse */
        {"PmInst.install.text.1", "http://download.opensuse.org repository"},
        {"PmInst.install.1",
         "echo 'deb http://download.opensuse.org/repositories/server:/ha-clustering/Debian_Etch/ ./' > /etc/apt/sources.list.d/ha-clustering.list "
         + " && apt-get update"
         + " && apt-get -y -q --allow-unauthenticated install"
         + " -o 'DPkg::Options::force=--force-confnew' pacemaker"
         + " && (grep 'START=no' /etc/default/openais && echo 'START=yes'>>/etc/default/openais)"
         + " && /usr/sbin/update-rc.d -f heartbeat remove"
         + " && if [ -e /etc/ais/openais.conf ];then"
         + " mv /etc/ais/openais.conf /etc/ais/openais.conf.orig; fi);"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig; fi)"},

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
         + "/usr/bin/apt-get -y install make gcc libc-dev flex linux-headers-`uname -r` && "
         + "make && make install && "
         + "/usr/sbin/update-rc.d drbd defaults 70 8 && "
         + "/bin/rm -rf /tmp/drbdinst"},

        /* disable drbd install method 3 */
        {"DrbdInst.install.text.3",
         ""},
    };
}
