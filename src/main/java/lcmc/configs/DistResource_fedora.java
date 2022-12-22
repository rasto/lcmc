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

public final class DistResource_fedora extends ListResourceBundle {

    private static final Object[][] contents = {
        {"Support", "fedora"},
        {"distribution", "redhat"},

        /* directory capturing regexp on the website from the kernel version */
        {"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+.*?fc\\d+).*"},

        {"DrbdInst.install",
         DistResource.SUDO + "/bin/rpm -Uvh /tmp/drbdinst/@DRBDPACKAGES@"},

        /* DRBD native */
        {"DrbdInst.install.text.1",
         "dnf install"},

        {"DrbdInst.install.1",
         "dnf -y install drbd-utils drbd-udev "
         + "&& if ( rpm -qa|grep pacemaker ); then"
         + " dnf -y install drbd-pacemaker; fi"},
        {"DrbdInst.install.method.1",       ""},

        /* Heartbeat/Pacemaker native */
        {"HbPmInst.install.text.1", ""},

        {"HbPmInst.install.1", ""},
        /* Corosync/Pacemaker native */
        {"PmInst.install.text.1",
         "dnf install: 1.1.x/1.4.x"},
        {"PmInst.install.1",
         "dnf -y install pacemaker corosync pcs"
         + "&& " + DistResource.SUDO + "/bin/systemctl enable corosync.service"
         + "&& if ( rpm -qa|grep drbd ); then"
         + " dnf -y install drbd-pacemaker; fi"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + "  mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},

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

        /* Proxy install method 1 */
        {"ProxyInst.install.text.1",
         "dnf install"},

        {"ProxyInst.install.1",
         "dnf install -y drbd-proxy-3.0"},

        {"ProxyCheck.version",
         "rpm -q --queryformat='%{VERSION}' drbd-proxy-3.0"},
    };

    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
