/*
 * This file is part of Linux Cluster Management Console
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2012, Rastislav Levrinc
 *
 * LCMC is free software; you can redistribute it and/or
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
 * Here are commands for fedora 17.
 */
public final class DistResource_fedora_17 extends ListResourceBundle {

    private static final Object[][] contents = {
        {"Support", "fedora-17"},

        /* Corosync/Pacemaker clusterlabs */
        {"PmInst.install.text.2",
         "clusterlabs repo: 1.1.8/2.0.x" },

        {"PmInst.install.2",
         "yum -y install wget && wget -N -nd -P /etc/yum.repos.d/"
         + " http://www.clusterlabs.org/rpm-next/fedora-17/clusterlabs.repo "
         + " && yum -y install pacemaker corosync"
         + " && if [ -e /etc/corosync/corosync.conf ]; then"
         + " mv /etc/corosync/corosync.conf /etc/corosync/corosync.conf.orig;"
         + " fi"},
    };

    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
