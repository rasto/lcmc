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

        /* TODO: does not work? */
        {"HbInst.install.text.1", "http://download.opensuse.org"},
        {"HbInst.install.1", "echo 'deb http://download.opensuse.org/repositories/server:/ha-clustering/xUbuntu_8.04/ ./' > /etc/apt/sources.list.d/ha-clustering.list "
                             + " && apt-get update"
                             + " && apt-get -y -q  --allow-unauthenticated install -o 'DPkg::Options::force=--force-confnew' heartbeat pacemaker"},

        /* Drbd install method 2 */
        {"DrbdInst.install.text.2",
         "from the source tarball"},

        {"HbInst.install.text.2", "apt-get"},
        //{"HbInst.install.version.1", "2.1.3"},
        {"HbInst.install.2", "apt-get update && /usr/bin/apt-get -y install -o 'DPkg::Options::force=--force-confnew' heartbeat-2"},

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

    };
}
