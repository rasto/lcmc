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

package lcmc.host.ui;

import lcmc.common.domain.Value;
import lcmc.utilities.Unit;

public class DrbdVersions implements Value {
    private final String moduleFileName;
    private final String moduleVersion;
    private final String utilFileName;
    private final String utilVersion;

    public DrbdVersions(final String moduleFileName,
                        final String moduleVersion,
                        final String utilFileName,
                        final String utilVersion) {
        this.moduleFileName = moduleFileName;
        this.moduleVersion = moduleVersion;
        this.utilFileName = utilFileName;
        this.utilVersion = utilVersion;
    }

    public String getModuleFileName() {
        return moduleFileName;
    }

    public String getModuleVersion() {
        return moduleVersion;
    }

    public String getUtilFileName() {
        return utilFileName;
    }

    public String getUtilVersion() {
        return utilVersion;
    }

    @Override
    public String getValueForGui() {
        final StringBuilder value = new StringBuilder(moduleVersion);
        if (utilVersion != null) {
            value.append('/');
            value.append(utilVersion);
        }
        return value.toString();
    }

    @Override
    public String toString() {
        return getValueForGui();
    }

    @Override
    public String getValueForConfig() {
        return null;
    }

    @Override
    public boolean isNothingSelected() {
        return false;
    }

    @Override
    public Unit getUnit() {
        return null;
    }

    @Override
    public String getValueForConfigWithUnit() {
        return null;
    }

    @Override
    public String getNothingSelected() {
        return null;
    }
}
