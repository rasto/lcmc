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
 * Here are commands for all redhats.
 */
public final class DistResource_redhatenterpriseserver
                                        extends ListResourceBundle {

    private static final Object[][] contents = {
        {"Support", "redhatenterpriseserver"},
        {"arch:i686", "i686"},
        {"distribution", "rhel"},
        {"version:Red Hat Enterprise Linux ES release 4 (Nahant*", "4"},
        {"version:Red Hat Enterprise Linux Server release 5*", "5"},
        {"version:Red Hat Enterprise Linux Server release 6*", "6"},
        {"version:Red Hat Enterprise Linux Server release 7*", "7"},
        {"version:Scientific Linux release 5*", "5"},
        {"version:Scientific Linux SL release 5*", "5"},
        {"version:Scientific Linux release 6*", "6"},

        /* directory capturing regexp on the website from the kernel version */
        {"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+.*?el\\d+).*"},

        {"DrbdInst.install",
         DistResource.SUDO + "/bin/rpm -Uvh /tmp/drbdinst/@DRBDPACKAGES@"},

        {"HbPmInst.install.text.1", "yum install: HB 2.1.4 (obsolete)"},
        {"HbPmInst.install.1", "/usr/bin/yum -y install heartbeat"},

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
         + "/usr/bin/yum -y install kernel`uname -r|"
         + " grep -o '5PAE\\|5xen\\|5debug'"
         + "|tr 5 -`-devel-`uname -r|sed 's/\\(PAE\\|xen\\|debug\\)$//'` &&"
         + "/usr/bin/yum -y install flex gcc make && "
         + "make && make install && "

         + "if [[ @UTIL-VERSION@ ]]; then "
         + "  /usr/bin/yum -y install libxslt && "
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

        {"HbCheck.version",
         DistResource.SUDO + "@GUI-HELPER@ get-cluster-versions;"
         + "/bin/rpm -q -i openais|perl -lne"
         + " 'print \"ais:$1\" if /^Version\\s+:\\s+(\\S+)/'"},

        {"Heartbeat.deleteFromRc",
         DistResource.SUDO + "/sbin/chkconfig --del heartbeat"},

        {"Heartbeat.addToRc",
         DistResource.SUDO + "/sbin/chkconfig --add heartbeat"},

        {"Corosync.addToRc",
         DistResource.SUDO + "/sbin/chkconfig --level 2345 corosync on "
         + "&& " + DistResource.SUDO + "/sbin/chkconfig --level 016 corosync off"},

        {"Corosync.deleteFromRc",
         DistResource.SUDO + "/sbin/chkconfig --del corosync"},

        {"Openais.addToRc",
         DistResource.SUDO + "/sbin/chkconfig --level 2345 openais on "
         + "&& " + DistResource.SUDO + "/sbin/chkconfig --level 016 openais off"},

        {"Openais.deleteFromRc",
         DistResource.SUDO + "/sbin/chkconfig --del openais"},
        {"KVM.emulator",    "/usr/libexec/qemu-kvm"},
        {"libvirt.lxc.libpath", "/usr/libexec"},
        {"libvirt.xen.libpath", "/usr/lib/xen"},

        /* Proxy install method 1 */
        {"ProxyInst.install.text.1",
         "yum install"},

        {"ProxyInst.install.1",
         "yum install -y drbd-proxy-3.0"},

        {"ProxyCheck.version",
         "rpm -q --queryformat='%{VERSION}' drbd-proxy-3.0"},
    };

    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
