/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
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
package lcmc.crm.ui.resource;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.ui.Access;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.common.ui.treemenu.ClusterTreeMenu;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.drbd.ui.resource.ResourceInfo;
import lcmc.host.domain.Host;

/**
 * DrbddiskInfo class is used for drbddisk heartbeat service that is treated in special way.
 */
@Named
public class DrbddiskInfo extends ServiceInfo {
    public DrbddiskInfo(Application application, SwingUtils swingUtils, Access access, MainData mainData,
            WidgetFactory widgetFactory, ProgressIndicator progressIndicator, ServiceMenu serviceMenu,
            Provider<CloneInfo> cloneInfoProvider, ClusterTreeMenu clusterTreeMenu, CrmServiceFactory crmServiceFactory) {
        super(application, swingUtils, access, mainData, widgetFactory, progressIndicator, serviceMenu, cloneInfoProvider,
                clusterTreeMenu, crmServiceFactory);
    }

    /** Returns string representation of the drbddisk service. */
    @Override
    public String toString() {
        return getName() + " (" + getParamSaved("1") + ')';
    }

    /** Returns resource name / parameter "1". */
    String getResourceName() {
        return getParamSaved("1").getValueForConfig();
    }

    @Override
    public void removeMyselfNoConfirm(final Host dcHost, final Application.RunMode runMode) {
        super.removeMyselfNoConfirm(dcHost, runMode);
        final ResourceInfo dri = getBrowser().getDrbdResourceNameHash().get(getResourceName());
        getBrowser().putDrbdResHash();
        if (dri != null) {
            dri.setUsedByCRM(null);
        }
    }

    @Override
    public void setParameters(final Map<String, String> resourceNode) {
        super.setParameters(resourceNode);
        final ResourceInfo dri = getBrowser().getDrbdResourceNameHash().get(getResourceName());
        getBrowser().putDrbdResHash();
        if (dri != null) {
            if (isManaged(Application.RunMode.LIVE) && !getService().isOrphaned()) {
                dri.setUsedByCRM(this);
            } else {
                dri.setUsedByCRM(null);
            }
            final Thread thread = new Thread(() -> dri.updateMenus(null));
            thread.start();
        }
    }
}
