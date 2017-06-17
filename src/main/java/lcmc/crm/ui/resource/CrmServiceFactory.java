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

package lcmc.crm.ui.resource;

import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.Application;
import lcmc.common.ui.Browser;
import lcmc.crm.domain.ResourceAgent;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Map;

@Named
public class CrmServiceFactory {
    @Inject
    private Provider<FilesystemRaInfo> filesystemRaInfoProvider;
    @Inject
    private Provider<LinbitDrbdInfo> linbitDrbdInfoProvider;
    @Inject
    private Provider<DrbddiskInfo> drbddiskInfoProvider;
    @Inject
    private Provider<IPaddrInfo> ipaddrInfoProvider;
    @Inject
    private Provider<VirtualDomainInfo> virtualDomainInfoProvider;
    @Inject
    private Provider<GroupInfo> groupInfoProvider;
    @Inject
    private Provider<CloneInfo> cloneInfoProvider;
    @Inject @Named("serviceInfo")
    private Provider<ServiceInfo> serviceInfoProvider;

    public ServiceInfo createFromResourceAgent(
            final ResourceAgent newResourceAgent,
            final boolean master,
            final ClusterBrowser browser) {
        final ServiceInfo newServiceInfo;

        final String name = newResourceAgent.getServiceName();
        if (newResourceAgent.isFilesystem()) {
            newServiceInfo = filesystemRaInfoProvider.get();
            newServiceInfo.init(name, newResourceAgent, browser);
        } else if (newResourceAgent.isLinbitDrbd()) {
            newServiceInfo = linbitDrbdInfoProvider.get();
            newServiceInfo.init(name, newResourceAgent, browser);
        } else if (newResourceAgent.isDrbddisk()) {
            newServiceInfo = drbddiskInfoProvider.get();
            newServiceInfo.init(name, newResourceAgent, browser);
        } else if (newResourceAgent.isIPaddr()) {
            newServiceInfo = ipaddrInfoProvider.get();
            newServiceInfo.init(name, newResourceAgent, browser);
        } else if (newResourceAgent.isVirtualDomain()) {
            newServiceInfo = virtualDomainInfoProvider.get();
            newServiceInfo.init(name, newResourceAgent, browser);
        } else if (newResourceAgent.isGroup()) {
            newServiceInfo = groupInfoProvider.get();
            newServiceInfo.init(name, newResourceAgent, browser);
        } else if (newResourceAgent.isClone()) {
            final String cloneName;
            if (master) {
                cloneName = Application.PM_MASTER_SLAVE_SET_NAME;
            } else {
                cloneName = Application.PM_CLONE_SET_NAME;
            }
            final CloneInfo newCloneInfo = cloneInfoProvider.get();
            newCloneInfo.init(newResourceAgent, cloneName, master, browser);
            newServiceInfo = newCloneInfo;
        } else {
            newServiceInfo = serviceInfoProvider.get();
            newServiceInfo.init(name, newResourceAgent, browser);
        }
        return newServiceInfo;
    }

    public ServiceInfo createServiceWithParameters(
            final String hbId,
            final ResourceAgent newResourceAgent,
            final Map<String, String> resourceNode,
            final Browser browser) {
        final String serviceName;
        if (newResourceAgent == null) {
            serviceName = hbId;
        } else {
            serviceName = newResourceAgent.getServiceName();
        }
        ServiceInfo newServiceInfo;
        if (newResourceAgent == null) {
            newServiceInfo = serviceInfoProvider.get();
        } else if (newResourceAgent.isFilesystem()) {
            newServiceInfo = filesystemRaInfoProvider.get();
        } else if (newResourceAgent.isLinbitDrbd()) {
            newServiceInfo = linbitDrbdInfoProvider.get();
        } else if (newResourceAgent.isDrbddisk()) {
            newServiceInfo = drbddiskInfoProvider.get();
        } else if (newResourceAgent.isIPaddr()) {
            newServiceInfo = ipaddrInfoProvider.get();
        } else if (newResourceAgent.isVirtualDomain()) {
            newServiceInfo = virtualDomainInfoProvider.get();
        } else {
            newServiceInfo = serviceInfoProvider.get();
        }
        newServiceInfo.init(serviceName, newResourceAgent, hbId, resourceNode, browser);
        return newServiceInfo;
    }
}
