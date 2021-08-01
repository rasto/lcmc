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

package lcmc.drbd.ui.configdialog;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.drbd.ui.resource.VolumeInfo;

/**
 * DrbdConfig super class from which all the drbd config wizards can be extended. It just adds VolumeInfo field.
 */
public abstract class DrbdConfig extends WizardDialog {
    private VolumeInfo volumeInfo;

    public DrbdConfig(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData) {
        super(application, swingUtils, widgetFactory, mainData);
    }

    public void init(final WizardDialog previousDialog, final VolumeInfo volumeInfo) {
        setPreviousDialog(previousDialog);
        this.volumeInfo = volumeInfo;
    }

    protected final VolumeInfo getDrbdVolumeInfo() {
        return volumeInfo;
    }
}
