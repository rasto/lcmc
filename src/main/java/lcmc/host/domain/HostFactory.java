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

package lcmc.host.domain;

import lcmc.HwEventBus;
import lcmc.cluster.service.ssh.Ssh;
import lcmc.cluster.service.storage.BlockDeviceService;
import lcmc.common.domain.Application;
import lcmc.host.ui.TerminalPanel;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.drbd.domain.DrbdHost;
import lcmc.drbd.domain.DrbdXml;
import lcmc.host.ui.HostBrowser;
import lcmc.robotest.RoboTest;
import lcmc.vm.domain.VmsXml;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

@Named
@Singleton
public class HostFactory {
    @Inject
    private HwEventBus hwEventBus;
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private Application application;
    @Inject
    private MainData mainData;
    @Inject
    private ProgressIndicator progressIndicator;
    @Inject
    private Hosts allHosts;
    @Inject
    private RoboTest roboTest;
    @Inject
    private BlockDeviceService blockDeviceService;
    @Inject
    private Provider<VmsXml> vmsXmlProvider;
    @Inject
    private Provider<DrbdXml> drbdXmlProvider;
    @Inject
    private Provider<TerminalPanel> terminalPanelProvider;
    @Inject
    private Provider<Ssh> sshProvider;
    @Inject
    private Provider<HostBrowser> hostBrowserProvider;

    public Host createInstance() {
        val drbdHost = new DrbdHost();
        val hostBrowser = hostBrowserProvider.get();
        val terminalPanel = terminalPanelProvider.get();
        val ssh = sshProvider.get();

        val host = new Host(
                drbdHost,
                terminalPanel,
                mainData,
                progressIndicator,
                ssh,
                hostBrowser,
                allHosts,
                application,
                roboTest,
                blockDeviceService,
                swingUtils);

        terminalPanel.initWithHost(host);
        host.init();
        val hostParser = new HostParser(
                host,
                drbdHost,
                hwEventBus,
                vmsXmlProvider,
                drbdXmlProvider,
                swingUtils,
                application);

        hostBrowser.init(host);
        host.setHostParser(hostParser);
        hwEventBus.register(hostParser);

        host.setHostParser(hostParser);
        return host;
    }

    public Host createInstance(final String ipAddress) {
        final Host instance = createInstance();
        instance.setIpAddress(ipAddress);
        return instance;
    }
}