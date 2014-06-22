/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2014, Rastislav Levrinc.
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

package lcmc.data.drbd;

import lcmc.Exceptions;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DrbdHost {
    private static final Logger LOG = LoggerFactory.getLogger(DrbdHost.class);

    /** Drbd version of drbdadm tool. */
    private String drbdVersion = null;
    private String drbdModuleVersion = null;
    private boolean drbdProxyRunning = false;
    private boolean drbdLoaded = false;

    public String getDrbdVersion() {
        return drbdVersion;
    }

    public String getDrbdModuleVersion() {
        return drbdModuleVersion;
    }

    /** Returns whether drbd module is loaded. */
    public boolean isDrbdLoaded() {
        return drbdLoaded;
    }

    public boolean isDrbdProxyRunning() {
        return drbdProxyRunning;
    }

    public void setDrbdVersion(String drbdVersion) {
        this.drbdVersion = drbdVersion;
    }

    public void setDrbdProxyRunning(boolean drbdProxyRunning) {
        this.drbdProxyRunning = drbdProxyRunning;
    }

    public void setDrbdModuleVersion(String drbdModuleVersion) {
        this.drbdModuleVersion = drbdModuleVersion;
    }

    public void setDrbdLoaded(boolean drbdLoaded) {
        this.drbdLoaded = drbdLoaded;
    }
}
