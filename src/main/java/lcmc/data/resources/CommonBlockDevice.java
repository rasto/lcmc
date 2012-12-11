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


package lcmc.data.resources;

import lcmc.utilities.Tools;

/**
 * This class holds data of one block device, that is the same
 * on all hosts. Unless it is used by drbd.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class CommonBlockDevice extends Resource {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Prepares a new <code>CommonBlockDevice</code> object.
     *
     * @param name
     *          block device name
     */
    public CommonBlockDevice(final String name) {
        super(name);
    }

    /** Return device name. */
    public String getDevice() {
        return getName();
    }

    /** Return value for parameter. */
    @Override
    public String getValue(final String parameter) {
        if ("device".equals(parameter)) {
            return getName();
        } else {
            Tools.appError("Unknown parameter: " + parameter, "");
            return "";
        }
    }

    /**
     * Returns possible choices for the parameter. Here it makes no sense,
     * don't call this method.
     *
     * @param param
     *          param for which you shoudn't call this method.
     *
     * @return null
     */
    @Override
    public String[] getPossibleChoices(final String param) {
        Tools.appError("Wrong call to getPossibleValues");
        return new String[]{};
    }

}
