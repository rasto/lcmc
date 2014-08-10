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

package lcmc.model;

import lcmc.gui.Browser;
import lcmc.gui.GUIData;
import lcmc.gui.HostBrowser;
import lcmc.gui.TerminalPanel;
import lcmc.gui.resources.drbd.GlobalInfo;
import lcmc.model.drbd.DrbdHost;
import lcmc.model.drbd.DrbdXml;
import lcmc.utilities.Tools;
import lcmc.utilities.ssh.Ssh;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Provider;

@Component
public class HostFactory {
    @Autowired
    private Provider<HostBrowser> hostBrowserProvider;
    @Autowired
    private Provider<TerminalPanel> terminalPanelProvider;
    @Autowired
    private GUIData guiData;
    @Autowired
    private Provider<Ssh> sshProvider;
    @Autowired
    private Provider<DrbdXml> drbdXmlProvider;

    public Host createInstance() {
        final TerminalPanel terminalPanel = terminalPanelProvider.get();
        final HostBrowser hostBrowser = hostBrowserProvider.get();
        final Host host = new Host(guiData, sshProvider.get(), new DrbdHost(), terminalPanel, drbdXmlProvider);
        host.setBrowser(hostBrowser);
        return host;
    }

    public Host createInstance(final String ipAddress) {
        final Host instance = createInstance();
        instance.setIpAddress(ipAddress);
        return instance;
    }
}
