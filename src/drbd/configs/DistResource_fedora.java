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
 * Here are commands for all fedoras.
 */
public class DistResource_fedora extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support", "fedora"},
        {"distribution", "redhat"},
        {"version:Fedora release 10*", "10"},
        {"version:Fedora release 11*", "11"},

        /* directory capturing regexp on the website from the kernel version */
        {"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+.*?fc\\d+).*"},

        {"DrbdInst.install",
         "/bin/rpm -Uvh /tmp/drbdinst/@DRBDPACKAGE@ /tmp/drbdinst/@DRBDMODULEPACKAGE@"},

        {"HbPmInst.install.text.2",
         "the fedora way: possibly too old" },

        {"HbPmInst.install.2",
         "/usr/bin/yum -y install heartbeat"},
        /* at least fedora 10 and fedora11 in version 2.1.3 and 2.14 has different
           ocf path. */
        {"Heartbeat.2.1.4.getOCFParameters",
         "export OCF_RESKEY_vmxpath=a;export OCF_ROOT=/usr/share/ocf;"
         + "mv /usr/lib/ocf/resource.d/linbit"
         + " /usr/share/ocf/resource.d/ 2>/dev/null;"
         + "for prov in `ls -1 /usr/share/ocf/resource.d/`; do "
         +  "for s in `ls -1 /usr/share/ocf/resource.d/$prov/ `; do "
         +  "echo -n 'provider:'; echo $prov;"
         +  "echo -n 'master:';"
         +  "grep -wl crm_master /usr/share/ocf/resource.d/$prov/$s;echo;"
         +  "/usr/share/ocf/resource.d/$prov/$s meta-data 2>/dev/null; done;"
         + "done;"
         + "echo 'provider:heartbeat';"
         + "echo 'master:';"
         + "/usr/local/bin/drbd-gui-helper get-old-style-resources;"
         + "/usr/local/bin/drbd-gui-helper get-lsb-resources"},

        {"HbCheck.version",
         "/usr/local/bin/drbd-gui-helper get-cluster-versions;"
         + "/bin/rpm -q -i openais|perl -lne"
         + " 'print \"ais:$1\" if /^Version\\s+:\\s+(\\S+)/';"
         + "/bin/rpm -q -i corosync|perl -lne"
         + " 'print \"cs:$1\" if /^Version\\s+:\\s+(\\S+)/'"},

        {"Heartbeat.deleteFromRc",
         "/sbin/chkconfig --del heartbeat"},

        {"Heartbeat.addToRc",
         "/sbin/chkconfig --add heartbeat"},

        {"Corosync.addToRc",
         "/sbin/chkconfig --level 2345 corosync on "
         + "&& /sbin/chkconfig --level 016 corosync off"},

        {"Corosync.deleteFromRc",
         "/sbin/chkconfig --del corosync"},

        {"Openais.addToRc",
         "/sbin/chkconfig --level 2345 openais on "
         + "&& /sbin/chkconfig --level 016 openais off"},

        {"Openais.deleteFromRc",
         "/sbin/chkconfig --del openais"},
    };
}
