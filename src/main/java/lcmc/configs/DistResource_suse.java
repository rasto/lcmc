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

/**
 * Here are commands for all suses.
 */
public final class DistResource_suse extends java.util.ListResourceBundle {

    /** Get contents. */
    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support", "suse"},
        {"version:SUSE LINUX Enterprise Server 9 (i586)", "SLES9"}, // detected
        //{"version:SUSE LINUX Enterprise Server 10 (i586)", "SLES10"}, // detected
        {"version:9*",     "SLES9"}, // detected
        {"version:SUSE Linux Enterprise Server 10*", "SLES10"}, // detected
        {"version:openSUSE 11.1*",    "OPENSUSE11_1"}, // detected
        {"version:openSUSE 11.2*",    "OPENSUSE11_2"}, // detected
        {"version:openSUSE 11.3*",    "OPENSUSE11_3"}, // detected
        {"version:openSUSE 11.4*",    "OPENSUSE11_4"}, // detected
        {"version:SUSE Linux Enterprise Server 11*", "SLES11"}, // detected
        {"version:sles9",  "SLES9"}, // chosen
        {"version:sles10", "SLES10"}, // chosen
        {"version:sles11", "SLES11"}, // chosen

        {"kerneldir", "(\\d+\\.\\d+\\.\\d+\\.\\d+-[.0-9]+).*"},
        /* drbd donwload and installation */
        // { "DrbdCheck.version", "/bin/rpm -qa|grep drbd- | sed s/drbd-//" },
        {"DrbdInst.install",
         DistResource.SUDO + "/bin/rpm -Uvh /tmp/drbdinst/@DRBDPACKAGES@"},
        /* heartbeat donwload and installation */
        //{ "HbCheck.version", "/bin/rpm -qa|grep heartbeat | sed s/.*heartbeat-//" },
        {"HbPmInst.install.i386", "i586" },
        {"HbPmInst.install.i486", "i586" },
        {"HbPmInst.install.i586", "i586" },
        {"PmInst.install.i386", "i586" },
        {"PmInst.install.i486", "i586" },
        {"PmInst.install.i586", "i586" },

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
         /* removing -pae etc. from uname -r */
         + "/usr/bin/zypper -n in kernel-source=`uname -r"
         + "|sed s/-[a-z].*//;` && "
         + "/usr/bin/zypper -n in flex gcc && "
         + "if [ -e configure ]; then"
         + " ./configure --prefix=/usr --with-km --localstatedir=/var"
         + " --sysconfdir=/etc;"
         + " fi && "
         + "make && make install DESTDIR=/ && "
         + "/bin/rm -rf /tmp/drbdinst"},

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

        {"DrbdLog.log",
         "grep @GREPPATTERN@ /var/log/messages | tail -500"},
        {"KVM.emulator",   "/usr/bin/qemu-kvm"},

        /* Openais/Pacemaker native */
        {"PmInst.install.text.2", "zypper install" },
        {"PmInst.install.2",
         "zypper -n install pacemaker"
         + " && if [ -e /etc/ais/openais.conf ];then"
         + " mv /etc/ais/openais.conf /etc/ais/openais.conf.orig; fi"},

        /* Drbd install method 2 */
        {"DrbdInst.install.text.2",
         "zypper install"},

        {"DrbdInst.install.2",
         "zypper -n in drbd"},

        {"DrbdInst.install.method.2",
         ""},

        /* Drbd install method 3 */
        {"DrbdInst.install.text.3",
         "from the source tarball"},

        {"DrbdInst.install.method.3",
         "source"},

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

        {"libvirt.lxc.libpath.x86_64", "/usr/lib64/libvirt"},
        {"libvirt.xen.libpath", "/usr/lib/xen"},
    };
}
