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

public class DrbdHost {
    private String drbdUtilVersion = null;
    private String drbdModuleVersion = null;
    private boolean drbdProxyRunning = false;
    private boolean drbdLoaded = false;

    public String getDrbdUtilVersion() {
        return drbdUtilVersion;
    }

    public String getDrbdModuleVersion() {
        return drbdModuleVersion;
    }

    public boolean isDrbdLoaded() {
        return drbdLoaded;
    }

    public boolean isDrbdProxyRunning() {
        return drbdProxyRunning;
    }

    public void setDrbdUtilVersion(String drbdUtilVersion) {
        this.drbdUtilVersion = drbdUtilVersion;
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
