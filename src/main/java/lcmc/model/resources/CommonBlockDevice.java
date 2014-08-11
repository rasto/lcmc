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


package lcmc.model.resources;

import lcmc.model.StringValue;
import lcmc.model.Value;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class holds data of one block device, that is the same
 * on all hosts. Unless it is used by drbd.
 */
public final class CommonBlockDevice extends Resource {
    private static final Logger LOG = LoggerFactory.getLogger(CommonBlockDevice.class);

    public CommonBlockDevice(final String blockDeviceName) {
        super(blockDeviceName);
    }

    /** Return device name. */
    public String getDeviceName() {
        return getName();
    }

    @Override
    public Value getValue(final String parameter) {
        if ("device".equals(parameter)) {
            return new StringValue(getName());
        } else {
            LOG.appError("getValue: unknown parameter: " + parameter, "");
            return null;
        }
    }

    @Override
    public Value[] getPossibleChoices(final String param) {
        LOG.appError("getPossibleChoices: wrong call");
        return new Value[]{};
    }

}
