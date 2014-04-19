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
 * Here are commands for centos verson 6.
 */
public final class DistResource_redhat_6 extends ListResourceBundle {

    /** Contents. */
    private static final Object[][] contents = {
        /* Kernel versions and their counterpart in @KERNELVERSION@ variable in
         * the donwload url. Must begin with "kernel:" keyword. deprecated */

        /* distribution name that is used in the download url */
        {"distributiondir", "rhel6"},

        /* support */
        {"Support", "redhat-6"},

        /* pacamker / corosync / yum */
        {"PmInst.install.text.2", "yum install" },

        {"PmInst.install.2",
         "/usr/bin/yum -y install corosync pacemaker"},

        /* Corosync/Pacemaker clusterlabs */
        {"PmInst.install.text.3",
         "clusterlabs repo: 1.1.x/1.4.x" },

        {"PmInst.install.3",
         "yum -y install wget && wget -N -nd -P /etc/yum.repos.d/"
         + " http://www.clusterlabs.org/rpm-next/rhel-6/clusterlabs.repo "
         + " && yum -y install pacemaker cman"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},

        /* Corosync/Pacemaker clusterlabs test */
        {"PmInst.install.text.4",
         "clusterlabs repo: 1.1.x/1.4.x" },

        {"PmInst.install.4",
         "yum -y install wget && wget -N -nd -P /etc/yum.repos.d/"
         + " http://www.clusterlabs.org/rpm-test-next/rhel-6/clusterlabs.repo "
         + " && yum -y install pacemaker cman"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},

        {"Corosync.addToRc",
         DistResource.SUDO + "/sbin/chkconfig --level 2345 corosync on "
         + "&& " + DistResource.SUDO + "/sbin/chkconfig --level 016 corosync off"},

    };

    /** Get contents. */
    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
