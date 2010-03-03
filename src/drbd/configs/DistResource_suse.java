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
 * Here are commands for all suses.
 */
public class DistResource_suse extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
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
        {"version:SUSE Linux Enterprise Server 11*", "SLES11"}, // detected
        {"version:sles9",  "SLES9"}, // chosen
        {"version:sles10", "SLES10"}, // chosen
        {"version:sles11", "SLES11"}, // chosen

        {"kerneldir", "(\\d+\\.\\d+\\.\\d+\\.\\d+-[.0-9]+).*"},
        /* drbd donwload and installation */
        // { "DrbdCheck.version", "/bin/rpm -qa|grep drbd- | sed s/drbd-//" },
        {"DrbdInst.install",
         "/bin/rpm -Uvh /tmp/drbdinst/@DRBDPACKAGES@"},
        /* heartbeat donwload and installation */
        //{ "HbCheck.version", "/bin/rpm -qa|grep heartbeat | sed s/.*heartbeat-//" },
        {"HbPmInst.install.i386", "i586" },
        {"HbPmInst.install.i486", "i586" },
        {"HbPmInst.install.i586", "i586" },
        {"PmInst.install.i386", "i586" },
        {"PmInst.install.i486", "i586" },
        {"PmInst.install.i586", "i586" },

        {"HbPmInst.install.text.1", "CD" },
        {"HbPmInst.install.1", "zypper -n --no-gpg-checks install heartbeat"},

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
         //+ "/sbin/chkconfig --add drbd && "
         + "/bin/rm -rf /tmp/drbdinst"},

        {"HbCheck.version",
         "@GUI-HELPER@ get-cluster-versions;"
         + "/bin/rpm -q -i openais|perl -lne"
         + " 'print \"ais:$1\" if /^Version\\s+:\\s+(\\S+)/'"},

        {"Heartbeat.deleteFromRc",
         "/sbin/chkconfig --del heartbeat"},

        {"Heartbeat.addToRc",
         "/sbin/chkconfig --add heartbeat"},

        {"Corosync.addToRc",
         "/sbin/chkconfig --add corosync"},

        {"Corosync.deleteFromRc",
         "/sbin/chkconfig --del corosync"},

        {"Openais.addToRc",
         "/sbin/chkconfig --add openais"},

        {"Openais.deleteFromRc",
         "/sbin/chkconfig --del openais"},
    };
}
