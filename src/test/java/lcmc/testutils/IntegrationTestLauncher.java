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

package lcmc.testutils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lcmc.LCMC;
import lcmc.gui.GUIData;
import lcmc.gui.ProgressIndicatorPanel;
import lcmc.model.Application;
import lcmc.model.Cluster;
import lcmc.model.Host;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;
import lcmc.view.ClusterTabFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.inject.Provider;

import static org.junit.Assert.assertEquals;

/**
 * This class provides tools for testing.
 */
@Component
public class IntegrationTestLauncher {
    private static final String PASSWORD = "rastislav";
    private static final String ID_DSA_KEY = "rastislav";
    private static final String ID_RSA_KEY = "rastislav";

    private static final int NUMBER_OF_HOSTS = 2;
    private static final String TEST_USERNAME = "root";

    private final List<Host> hosts = new ArrayList<Host>();
    private volatile boolean clusterLoaded = false;

    @Autowired
    private GUIData guiData;
    @Autowired
    private Application application;
    @Autowired
    private Provider<Host> hostProvider;
    @Autowired
    private Provider<Cluster> clusterProvider;
    @Autowired
    private ProgressIndicatorPanel glassPane;
    @Autowired
    private LCMC lcmc;
    @Autowired
    private ClusterTabFactory clusterTabFactory;

    public void initTestCluster() {
        initCluster();
        application.waitForSwing();
    }

    /** Returns test hosts. */
    public List<Host> getHosts() {
        return Collections.unmodifiableList(this.hosts);
    }

    public boolean isClusterLoaded() {
        return clusterLoaded;
    }

    /** Print error and exit. */
    private void error(final String s) {
        System.out.println(s);
        System.exit(10);
    }

    private synchronized void initMain() {
        lcmc.launch(new String[]{"--no-upgrade-check"});
        application.waitForSwing();
    }

    /** Adds test cluster to the GUI. */
    private synchronized void initCluster() {
        if (clusterLoaded) {
            return;
        }
        initMain();
        application.waitForSwing();

        LoggerFactory.setDebugLevel(-1);
        final String username = TEST_USERNAME;
        final boolean useSudo = false;
        final Cluster cluster = clusterProvider.get();
        cluster.setName("test");
        for (int i = 1; i <= NUMBER_OF_HOSTS; i++) {
            final String hostName = "test" + i;
            final Host host = hostProvider.get();
            host.init();

            initHost(host, hostName, username, useSudo);
            hosts.add(host);
            host.setCluster(cluster);
            cluster.addHost(host);
            final String saveFile = application.getDefaultSaveFile();
            application.saveConfig(saveFile, false);
        }
        for (final Host host : hosts) {
            host.disconnect();
        }
        cluster.connect(null, true, 0);
        for (final Host host : hosts) {
            final boolean r = waitForCondition(
                new Condition() {
                    @Override
                    public boolean passed() {
                        return host.isConnected();
                    }
                }, 300, 20000);
            if (!r) {
                error("could not establish connection to " + host.getName());
            }
        }
        application.addClusterToClusters(cluster);
        application.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                clusterTabFactory.createClusterTab(cluster);
            }
        });
        
        //guiData.getEmptyBrowser().addClusterBox(cluster);
        final String saveFile = application.getDefaultSaveFile();
        application.saveConfig(saveFile, false);
        guiData.refreshClustersPanel();
        
        guiData.expandTerminalSplitPane(1);
        cluster.getClusterTab().addClusterView();
        cluster.getClusterTab().requestFocus();
        guiData.checkAddClusterButtons();
        for (final Host host : hosts) {
            host.waitForServerStatusLatch();
        }
        clusterLoaded = true;
    }

    private Host initHost(final Host host, final String hostName, final String username, final boolean useSudo) {
        host.setEnteredHostOrIp(hostName);
        host.setUsername(username);
        host.setSSHPort("22");
        host.setUseSudo(useSudo);
        
        if (!application.existsHost(host)) {
            application.addHostToHosts(host);
        }
        String ip;
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(hostName);
            ip = addresses[0].getHostAddress();
            if (ip != null) {
                host.setHostname(InetAddress.getByName(ip).getHostName());
            }
        } catch (UnknownHostException e) {
            error("cannot resolve: " + hostName);
            return null;
        }
        host.getSSH().setPasswords(ID_DSA_KEY, ID_RSA_KEY, PASSWORD);
        host.setIpAddress(ip);
        host.setIps(0, new String[]{ip});
        return host;
    }

    /**
     * Wait for condition 'timeout' seconds, check every 'interval' second.
     * Return false if it ran to the timeout.
     */
    private boolean waitForCondition(final Condition condition, final int interval, final int timeout) {
        int timeoutLeft = timeout;
        while (!condition.passed() && timeout >= 0) {
            Tools.sleep(interval);
            timeoutLeft -= interval;
        }
        return timeoutLeft >= 0;
    }

    /** Specify a condition to be passed to the waitForCondition function. */
    private interface Condition {
        /** returns true if condition is true. */
        boolean passed();
    }
}
