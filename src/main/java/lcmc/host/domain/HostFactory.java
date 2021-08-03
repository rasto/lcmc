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

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import lcmc.HwEventBus;
import lcmc.cluster.domain.storage.BlockDeviceService;
import lcmc.cluster.infrastructure.ssh.Ssh;
import lcmc.common.domain.Application;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.drbd.domain.DrbdHost;
import lcmc.drbd.domain.DrbdXml;
import lcmc.host.domain.parser.DistributionDetector;
import lcmc.host.domain.parser.HostParser;
import lcmc.host.ui.HostBrowser;
import lcmc.host.ui.TerminalPanel;
import lcmc.robotest.RoboTest;
import lcmc.vm.domain.VmsXml;
import lombok.val;

@Named
@Singleton
public class HostFactory {
    private final HwEventBus hwEventBus;
    private final SwingUtils swingUtils;
    private final Application application;
    private final MainData mainData;
    private final ProgressIndicator progressIndicator;
    private final Hosts allHosts;
    private final RoboTest roboTest;
    private final BlockDeviceService blockDeviceService;
    private final Provider<VmsXml> vmsXmlProvider;
    private final Provider<DrbdXml> drbdXmlProvider;
    private final Provider<TerminalPanel> terminalPanelProvider;
    private final Provider<Ssh> sshProvider;
    private final Provider<HostBrowser> hostBrowserProvider;

    public HostFactory(MainData mainData, HwEventBus hwEventBus, SwingUtils swingUtils, Application application,
            ProgressIndicator progressIndicator, Hosts allHosts, RoboTest roboTest, BlockDeviceService blockDeviceService,
            Provider<VmsXml> vmsXmlProvider, Provider<DrbdXml> drbdXmlProvider, Provider<Ssh> sshProvider,
            Provider<HostBrowser> hostBrowserProvider, Provider<TerminalPanel> terminalPanelProvider) {
        this.mainData = mainData;
        this.hwEventBus = hwEventBus;
        this.swingUtils = swingUtils;
        this.application = application;
        this.progressIndicator = progressIndicator;
        this.allHosts = allHosts;
        this.roboTest = roboTest;
        this.blockDeviceService = blockDeviceService;
        this.vmsXmlProvider = vmsXmlProvider;
        this.drbdXmlProvider = drbdXmlProvider;
        this.sshProvider = sshProvider;
        this.hostBrowserProvider = hostBrowserProvider;
        this.terminalPanelProvider = terminalPanelProvider;
    }

    public Host createInstance() {
        val drbdHost = new DrbdHost();
        val hostBrowser = hostBrowserProvider.get();
        val terminalPanel = terminalPanelProvider.get();
        val ssh = sshProvider.get();

        val host = new Host(drbdHost, terminalPanel,
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
        val distributionDetector = new DistributionDetector(host);
        val hostParser = new HostParser(
                host,
                drbdHost,
                hwEventBus,
                vmsXmlProvider,
                drbdXmlProvider,
                swingUtils,
                application,
                distributionDetector);

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