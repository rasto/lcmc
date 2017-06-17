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
 * Here are commands for rhel version 6.
 */
public final class DistResource_redhatenterpriseserver_6
                                        extends ListResourceBundle {

    private static final Object[][] contents = {
        /* distribution name that is used in the download url */
        {"distributiondir", "rhel6"},

        /* support */
        {"Support", "redhatenterpriseserver-6"},

        /* Corosync/Openais/Pacemaker native */
        {"PmInst.install.text.1",
         "yum install: 1.1.x/1.2.x" },

        {"PmInst.install.1",
         "yum -y install pacemaker corosync"},

        /* Heartbeat/Pacemaker native */
        {"HbPmInst.install.text.1", "" },

        {"HbPmInst.install.1", ""},

        {"kerneldir", "(\\d+\\.\\d+\\.\\d+-\\d+.*?el\\d+.*)"},

        /* Workaround, where aisexec hangs the gui if called directly. */
        {"Openais.startOpenais.i686",
         "echo '/etc/init.d/openais start'|at now"},
        {"Openais.reloadOpenais.i686",
         "echo '/etc/init.d/openais reload'|at now"},

        /* Drbd install method 2 */
        {"DrbdInst.install.text.2",
         "from the source tarball"},

        {"DrbdInst.install.method.2",
         "source"},

        {"DrbdInst.install.staging.2", "true"},

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
    };

    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
