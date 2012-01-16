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

package lcmc.gui.dialog.drbdUpgrade;

import lcmc.data.Host;
import lcmc.gui.dialog.WizardDialog;

/**
 * An implementation of a dialog where available versions of drbd will be
 * determined.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
final class DrbdAvailFiles extends lcmc.gui.dialog.host.DrbdAvailFiles {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Prepares a new <code>DrbdAvailFiles</code> object. */
    DrbdAvailFiles(final WizardDialog previousDialog, final Host host) {
        super(previousDialog, host);
    }

    /** Returns the next dialog.drbdUpgrade.DrbdLinbitInst. */
    @Override
    public WizardDialog nextDialog() {
        return new DrbdLinbitInst(this, getHost());
    }
}
