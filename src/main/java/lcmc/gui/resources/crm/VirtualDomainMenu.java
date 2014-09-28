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

package lcmc.gui.resources.crm;

import java.util.Collection;
import java.util.List;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.vm.domain.VmsXml;
import lcmc.gui.resources.vms.DomainInfo;
import lcmc.utilities.EnablePredicate;
import lcmc.utilities.MenuAction;
import lcmc.utilities.MenuFactory;
import lcmc.utilities.UpdatableItem;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class VirtualDomainMenu extends ServiceMenu {
    
    private VirtualDomainInfo virtualDomainInfo;

    private DomainInfo domainInfo;
    @Inject
    private Application application;
    @Inject
    private MenuFactory menuFactory;

    @Override
    public List<UpdatableItem> getPulldownMenu(final ServiceInfo serviceInfo) {
        virtualDomainInfo = (VirtualDomainInfo) serviceInfo;
        domainInfo = virtualDomainInfo.getDomainInfo();
        final List<UpdatableItem> items = super.getPulldownMenu(virtualDomainInfo);
        addVncViewersToTheMenu(items);
        return items;
    }

    /** Adds vnc viewer menu items. */
    private void addVncViewersToTheMenu(final Collection<UpdatableItem> items) {
        if (application.isUseTightvnc()) {
            /* tight vnc test menu */
            final UpdatableItem tightvncViewerMenu = menuFactory.createMenuItem(
                            "start TIGHT VNC viewer",
                            null,
                            null,
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false))
                    .enablePredicate(new EnablePredicate() {
                        @Override
                        public String check() {
                            final VmsXml vxml = virtualDomainInfo.getVMSXML(getRunningOnHost());
                            if (vxml == null || domainInfo == null) {
                                return "VM is not available";
                            }
                            final int remotePort = vxml.getRemotePort(domainInfo.getName());
                            if (remotePort <= 0) {
                                return "remote port is not greater than 0";
                            }
                            return null;
                        }
                    })
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    virtualDomainInfo.hidePopup();
                    final DomainInfo vvdi = domainInfo;
                    final VmsXml vxml = virtualDomainInfo.getVMSXML(getRunningOnHost());
                    if (vxml != null && vvdi != null) {
                        final int remotePort = vxml.getRemotePort(vvdi.getName());
                        final Host host = vxml.getDefinedOnHost();
                        if (host != null && remotePort > 0) {
                            application.startTightVncViewer(host, remotePort);
                        }
                    }
                }});
            items.add(tightvncViewerMenu);
        }

        if (application.isUseUltravnc()) {
            /* ultra vnc test menu */
            final UpdatableItem ultravncViewerMenu = menuFactory.createMenuItem(
                            "start ULTRA VNC viewer",
                            null,
                            null,
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false))
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    final VmsXml vxml = virtualDomainInfo.getVMSXML(getRunningOnHost());
                    if (vxml == null || domainInfo == null) {
                        return "VM is not available";
                    }
                    final int remotePort = vxml.getRemotePort(domainInfo.getName());
                    if (remotePort <= 0) {
                        return "remote port is not greater than 0";
                    }
                    return null;
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    virtualDomainInfo.hidePopup();
                    final DomainInfo vvdi = domainInfo;
                    final VmsXml vxml = virtualDomainInfo.getVMSXML(getRunningOnHost());
                    if (vxml != null && vvdi != null) {
                        final int remotePort = vxml.getRemotePort(vvdi.getName());
                        final Host host = vxml.getDefinedOnHost();
                        if (host != null && remotePort > 0) {
                            application.startUltraVncViewer(host, remotePort);
                        }
                    }
                }});
            items.add(ultravncViewerMenu);
        }

        if (application.isUseRealvnc()) {
            /* real vnc test menu */
            final UpdatableItem realvncViewerMenu = menuFactory.createMenuItem(
                            "start REAL VNC test",
                            null,
                            null,
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false))
                    .enablePredicate(new EnablePredicate() {
                            @Override
                            public String check() {
                    final VmsXml vxml = virtualDomainInfo.getVMSXML(getRunningOnHost());
                    if (vxml == null || domainInfo == null) {
                        return "VM is not available";
                    }
                    final int remotePort = vxml.getRemotePort(domainInfo.getName());
                    if (remotePort <= 0) {
                        return "remote port is not greater than 0";
                    }
                    return null;
                }})
                .addAction(new MenuAction() {
                        @Override
                        public void run(final String text) {
                    virtualDomainInfo.hidePopup();
                    final DomainInfo vvdi = domainInfo;
                    final VmsXml vxml = virtualDomainInfo.getVMSXML(getRunningOnHost());
                    if (vxml != null && vvdi != null) {
                        final int remotePort = vxml.getRemotePort(vvdi.getName());
                        final Host host = vxml.getDefinedOnHost();
                        if (host != null && remotePort > 0) {
                            application.startRealVncViewer(host, remotePort);
                        }
                    }
                }});
            items.add(realvncViewerMenu);
        }
    }

    /** Returns the first on which this vm is running. */
    private Host getRunningOnHost() {
        final List<String> nodes = virtualDomainInfo.getRunningOnNodes(Application.RunMode.LIVE);
        if (nodes != null && !nodes.isEmpty()) {
            return virtualDomainInfo.getBrowser().getCluster().getHostByName(nodes.get(0));
        }
        return null;
    }
}
