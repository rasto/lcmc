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
 * Here are commands for centos verson 9.
 */
public final class DistResource_redhat_9 extends ListResourceBundle {

    private static final Object[][] contents = {
        /* Kernel versions and their counterpart in @KERNELVERSION@ variable in
         * the donwload url. Must begin with "kernel:" keyword. deprecated */

        /* distribution name that is used in the download url */
        {"distributiondir", "rhel9"},

        {"Support", "redhat-9"},

        /* pacamker / corosync / yum */
        {"PmInst.install.text.2", "dnf install" },

        {"PmInst.install.2", " dnf --enablerepo=resilientstorage -y install pacemaker pcs"
                + " && systemctl enable --now pcsd"},

        /* Drbd install method 3 */
        {"DrbdInst.install.text.3",
         "dnf install"},

        {"DrbdInst.install.3",
         "dnf -y install https://www.elrepo.org/elrepo-release-9.el9.elrepo.noarch.rpm"
                + " && rpm --import https://www.elrepo.org/RPM-GPG-KEY-elrepo.org"
                + " && dnf -y install drbd-utils kmod-drbd9x drbd-pacemaker drbd-udev"},
    };

    @Override
    protected Object[][] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }
}
