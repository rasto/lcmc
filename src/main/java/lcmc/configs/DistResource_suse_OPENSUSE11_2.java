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
 * Here are commands for opensuse 11.2.
 */
public final class DistResource_suse_OPENSUSE11_2
                                        extends ListResourceBundle {

    private static final Object[][] contents = {
        /* Kernel versions and their counterpart in @KERNELVERSION@ variable in
         * the donwload url. Must begin with "kernel:" keyword. deprecated */

        /* distribution name that is used in the drbd download url */
        {"distributiondir", "sles11"},
        {"Support", "suse-OPENSUSE11_2"},
        {"DRBD.load",
         DistResource.SUDO + "sed -i 's/\\(allow_unsupported_modules \\)0/\\11/'"
         + " /etc/modprobe.d/unsupported-modules;"
         + DistResource.SUDO + "/sbin/modprobe drbd"},

        /* Drbd install method 2 */
        {"DrbdInst.install.text.2",
         "zypper install: 8.3.x"},

        {"DrbdInst.install.2",
         "zypper -n in drbd drbd-kmp-desktop"},

        {"DrbdInst.install.method.2",
         ""},

        /* Drbd install method 3 */
        {"DrbdInst.install.text.3",
         "from the source tarball"},

        {"DrbdInst.install.method.3",
         "source"},

        {"DrbdInst.install.staging.3", "true"},
        {"DrbdInst.install.3",
         "/bin/mkdir -p /tmp/drbdinst && "
         + "/usr/bin/wget --directory-prefix=/tmp/drbdinst/"
         + " http://oss.linbit.com/drbd/@VERSIONSTRING@ && "
         + "cd /tmp/drbdinst && "
         + "/bin/tar xfzp drbd-@VERSION@.tar.gz && "
         + "cd drbd-@VERSION@ && "
         /* removing -pae etc. from uname -r */
         + "/usr/bin/zypper -n in kernel-`uname -r|sed 's/.*-\\([a-z]\\)/\\1/'`"
         + "-devel=`uname -r|sed s/-[a-z].*//;` && "
         + "/usr/bin/zypper -n in make flex gcc && "
         + "if [ -e configure ]; then"
         + " ./configure --prefix=/usr --with-km --localstatedir=/var"
         + " --sysconfdir=/etc;"
         + " fi && "
         + "make && make install DESTDIR=/ && "
         + "/bin/rm -rf /tmp/drbdinst"},

        /* Corosync/Openais/Pacemaker clusterlabs */
        {"PmInst.install.text.1",
         "clusterlabs repo: 1.0.x/1.0.x" },
        {"PmInst.install.1",
         "wget -N -nd -P /etc/zypp/repos.d/"
         + " http://www.clusterlabs.org/rpm/opensuse-11.2/clusterlabs.repo && "
         + "zypper -n --no-gpg-check install pacemaker corosync"
         + " && if [ -e /etc/corosync/corosync.conf ];then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},

        /* Openais/Pacemaker native */
        {"PmInst.install.text.2", "zypper install: 1.0.x/0.80.x" },
        {"PmInst.install.2",
         "zypper -n install pacemaker"
         + " && if [ -e /etc/ais/openais.conf ];then"
         + " mv /etc/ais/openais.conf /etc/ais/openais.conf.orig; fi"},

        /* Heartbeat/Pacemaker Clusterlabs */
        {"HbPmInst.install.text.1",
         "clusterlabs repo: 1.0.x/3.0.x" },
        {"HbPmInst.install.1",
         "wget -N -nd -P /etc/zypp/repos.d/"
         + " http://www.clusterlabs.org/rpm/opensuse-11.2/clusterlabs.repo && "
         + "zypper -n --no-gpg-check install heartbeat pacemaker"},

        /* Heartbeat/Pacemaker native */
        {"HbPmInst.install.text.2", "zypper install: 1.0.x/2.99.x" },
        {"HbPmInst.install.2",
         "zypper -n install heartbeat pacemaker"},
    };

    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
