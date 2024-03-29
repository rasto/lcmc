/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, Rastislav Levrinc
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
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

package lcmc.vm.ui.configdialog;

import javax.inject.Named;
import javax.inject.Provider;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.ui.ProgressBar;
import lcmc.common.ui.WizardDialog;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.vm.ui.resource.DomainInfo;

/**
 * VMConfig super class from which all the vm wizards can be extended.
 */
@Named
public abstract class VMConfig extends WizardDialog {
    private DomainInfo domainInfo;

    public VMConfig(Application application, SwingUtils swingUtils, WidgetFactory widgetFactory, MainData mainData,
            Provider<ProgressBar> progressBarProvider) {
        super(application, swingUtils, widgetFactory, mainData, progressBarProvider);
    }

    public void init(final WizardDialog previousDialog, final DomainInfo domainInfo) {
        setPreviousDialog(previousDialog);
        this.domainInfo = domainInfo;
    }

    protected final DomainInfo getVMSVirtualDomainInfo() {
        return domainInfo;
    }
}
