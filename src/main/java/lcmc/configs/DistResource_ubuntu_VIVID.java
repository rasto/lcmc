/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2015, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.configs;

import java.util.Arrays;
import java.util.ListResourceBundle;

public final class DistResource_ubuntu_VIVID extends ListResourceBundle {

    private static final Object[][] contents = {
        {"Support",                       "ubuntu-VIVID"},
        /* pacemaker heartbeat install method 1 */
        {"HbPmInst.install.text.1",
         "apt-get install: 1.0.x / 3.0.x"},

        {"HbPmInst.install.1",
         "apt-get update && /usr/bin/apt-get -y install -o"
         + " 'DPkg::Options::force=--force-confnew' pacemaker heartbeat"
         + " && systemctl disable corosync"},

        /* pacemaker corosync install method 1 */
        {"PmInst.install.text.1",
         "apt-get install: 1.0.x / 1.2.x"},

        {"PmInst.install.1",
         "apt-get update && /usr/bin/apt-get -y install -o"
         + " 'DPkg::Options::force=--force-confnew' pacemaker corosync "
         + " && mkdir /var/log/cluster"
         + " && (grep 'START=no' /etc/default/corosync && echo 'START=yes'>>/etc/default/corosync; true)"
         + " && if [ -e /etc/corosync/corosync.conf ];then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig; fi"},

        {"HbCheck.version",
         DistResource.SUDO + "@GUI-HELPER@ get-cluster-versions;"
         + "/usr/bin/dpkg-query -f='${Status} ais:${Version}\n' -W openais 2>&1|grep '^install ok installed'|cut -d ' ' -f 4"
         + "|sed 's/-.*//'"},

        {"Heartbeat.deleteFromRc",
         DistResource.SUDO + "systemctl disable heartbeat"},

        {"Heartbeat.addToRc",
         DistResource.SUDO + "systemctl enable heartbeat"},

        {"Corosync.addToRc",
         DistResource.SUDO + "systemctl enable corosync"},

        {"Corosync.deleteFromRc",
         DistResource.SUDO + "systemctl disable corosync"},

        {"Openais.addToRc",
         DistResource.SUDO + "systemctl enable openais"},

        {"Openais.deleteFromRc",
         DistResource.SUDO + "systemctl disable openais"},
    };

    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
