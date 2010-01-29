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


package drbd.gui.dialog.cluster;

import drbd.data.Cluster;
import drbd.gui.dialog.WizardDialog;

/**
 * DialogCluster.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public abstract class DialogCluster extends WizardDialog {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Cluster object. */
    private final Cluster cluster;

    /**
     * Prepares a new <code>DialogCluster</code> object.
     */
    public DialogCluster(final WizardDialog previousDialog,
                         final Cluster cluster) {
        super(previousDialog);
        this.cluster = cluster;
    }

    /**
     * Returns cluster object.
     */
    protected final Cluster getCluster() {
        return cluster;
    }

    /**
     * Returns dialog title with cluster name attached.
     */
    protected final String getDialogTitle() {
        final StringBuffer s = new StringBuffer(40);
        s.append(getClusterDialogTitle());
        if (cluster != null && cluster.getName() != null) {
            s.append(" (");
            s.append(cluster.getName());
            s.append(')');
        }
        return s.toString();
    }

    /**
     * Returns title for the getDialogTitle() function.
     */
    protected abstract String getClusterDialogTitle();
}
