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
 * Here are commands for all openfilers.
 */
public class DistResource_openfiler extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        {"Support",                       "openfiler"},
        {"version:Openfiler NSA 2.3",     "2"},
        ///* directory capturing regexp on the website from the kernel version */
        //{"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+).*"},
        /* Drbd install method 2 */
        //{"DrbdInst.install.text.2",
        // "from the source tarball"},

        //{"DrbdInst.install.method.2",
        // "source"},
        //
        ///* TODO: It does not install because openfiler has patched kernel.
        // */
        //{"DrbdInst.install.2",
        // "/bin/mkdir -p /tmp/drbdinst && "
        // + "/usr/bin/wget --directory-prefix=/tmp/drbdinst/"
        // + " http://oss.linbit.com/drbd/@VERSIONSTRING@ && "
        // + "cd /tmp/drbdinst && "
        // + "/bin/tar xfzp drbd-@VERSION@.tar.gz && "
        // + "cd drbd-@VERSION@ && "
        // + "/usr/bin/conary update flex gcc glibc:devel && "
        // + "make && make install && "
        // + "/bin/rm -rf /tmp/drbdinst"},

        {"Heartbeat.deleteFromRc",
         "/sbin/chkconfig --del heartbeat"},

        {"Heartbeat.addToRc",
         "/sbin/chkconfig --level 2345 heartbeat on"},

        {"Corosync.addToRc",
         "/sbin/chkconfig --level 2345 corosync on"},

        {"Corosync.deleteFromRc",
         "/sbin/chkconfig --del corosync"},

        {"Openais.addToRc",
         "/sbin/chkconfig --level 2345 openais on"},

        {"Openais.deleteFromRc",
         "/sbin/chkconfig --del openais"},
    };
}
