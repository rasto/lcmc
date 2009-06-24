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
 * Here are commands for suse sles 10.
 */
public class DistResource_suse_SLES10 extends
            java.util.ListResourceBundle {

    /** Get contents. */
    protected final Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    /** Contents. */
    private static Object[][] contents = {
        /* Kernel versions and their counterpart in @KERNELVERSION@ variable in
         * the donwload url. Must begin with "kernel:" keyword. deprecated */

        /* distribution name that is used in the download url */
        {"distributiondir", "sles10"},
        {"Support", "suse-SLES10"},

        /* Openais/Pacemaker Opensuse*/
        {"AisPmInst.install.text.1", "http://download.opensuse.org: rug" },
        {"AisPmInst.install.1",
         "rug service-delete ha-clustering; "
         + "rug key-add 'server:ha-clustering OBS Project <server:ha-clustering@build.opensuse.org>' 083814151D362AEB E4A6B602AB088B3173853924083814151D362AEB"
         + " && rug service-add -t zypp http://download.opensuse.org/repositories/server:/ha-clustering/SLES_10 ha-clustering"
         + " && /usr/bin/zypper -n --no-gpg-checks install openais pacemaker" 
         + " && /sbin/chkconfig --add openais"},

        /* Heartbeat/Pacemaker Opensuse*/
        {"HbPmInst.install.text.1", "http://download.opensuse.org: rug" },
        {"HbPmInst.install.1",
         "rug service-delete ha-clustering; "
         + "rug key-add 'server:ha-clustering OBS Project <server:ha-clustering@build.opensuse.org>' 083814151D362AEB E4A6B602AB088B3173853924083814151D362AEB"
         + " && rug service-add -t zypp http://download.opensuse.org/repositories/server:/ha-clustering/SLES_10 ha-clustering"
         + " && /usr/bin/zypper -n --no-gpg-checks install heartbeat pacemaker" 
         + " && /sbin/chkconfig --add heartbeat"},

        /* Heartbeat */
        {"HbPmInst.install.text.2", "the suse way: possibly too old" },
        {"HbPmInst.install.2",
         "/usr/bin/zypper -n --no-gpg-checks install heartbeat &&" 
         + "/sbin/chkconfig --add heartbeat"},

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
         + "/usr/bin/zypper -n --no-gpg-checks in kernel-source && "
         + "/usr/bin/zypper -n --no-gpg-checks in flex gcc && "
         + "make && make install && "
         + "/sbin/chkconfig --add drbd && "
         + "/bin/rm -rf /tmp/drbdinst"},

        /* Drbd install method 3 */
        {"DrbdInst.install.text.3",
         "the suse way: possibly too old"},

        {"DrbdInst.install.3",
         "/usr/bin/zypper -n --no-gpg-checks install drbd drbd-kmp-default"},

    };
}
