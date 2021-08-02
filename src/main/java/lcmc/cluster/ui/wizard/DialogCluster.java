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


package lcmc.cluster.ui.wizard;

import javax.inject.Provider;

import lcmc.cluster.domain.Cluster;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;

public abstract class DialogCluster extends WizardDialog {
    private Cluster cluster;

    public DialogCluster(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData,
            Provider<ProgressBar> progressBarProvider) {
        super(application, swingUtils, widgetFactory, mainData, progressBarProvider);
    }

    public void init(final WizardDialog previousDialog, final Cluster cluster0) {
        setPreviousDialog(previousDialog);
        cluster = cluster0;
    }

    protected final Cluster getCluster() {
        return cluster;
    }

    /** Returns dialog title with cluster name attached. */
    @Override
    protected final String getDialogTitle() {
        final StringBuilder s = new StringBuilder(40);
        s.append(getClusterDialogTitle());
        if (cluster != null && cluster.getName() != null) {
            s.append(" (");
            s.append(cluster.getName());
            s.append(')');
        }
        return s.toString();
    }

    /** Returns title for the getDialogTitle() function. */
    protected abstract String getClusterDialogTitle();
}
