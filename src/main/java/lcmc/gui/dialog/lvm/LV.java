
/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009 - 2011, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011, Rastislav Levrinc
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

package lcmc.gui.dialog.lvm;

import javax.swing.JComponent;
import lcmc.gui.dialog.WizardDialog;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.utilities.Unit;

/** LVM dialogs. */
class LV extends WizardDialog {
    protected static Unit[] getUnits() {
        return new Unit[]{
            new Unit("K", "K", "KiByte", "KiBytes"),
                     new Unit("M", "M", "MiByte", "MiBytes"),
                     new Unit("G",  "G",  "GiByte",      "GiBytes"),
                     new Unit("T",  "T",  "TiByte",      "TiBytes")
        };
    }

    protected void init(final WizardDialog previousDialog) {
        setPreviousDialog(previousDialog);
    }

    @Override
    public final WizardDialog nextDialog() {
        return null;
    }

    @Override
    protected JComponent getInputPane() {
        return null;
    }

    @Override
    protected String getDialogTitle() {
        return null;
    }


    @Override
    protected String getDescription() {
        return null;
    }

    protected boolean isOneDrbd(final Iterable<BlockDevInfo> bdis) {
        for (final BlockDevInfo bdi : bdis) {
            if (bdi.getBlockDevice().isDrbd()) {
                return true;
            }
        }
        return false;
    }
}
