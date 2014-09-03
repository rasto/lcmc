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
import java.util.Map;
import java.util.Set;

import lcmc.LCMC;
import lcmc.gui.GUIData;
import lcmc.gui.ProgressIndicatorPanel;
import lcmc.gui.TerminalPanel;
import lcmc.model.Application;
import lcmc.model.Cluster;
import lcmc.model.Host;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;
import lcmc.view.ClusterTabFactory;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.inject.Provider;

import static org.junit.Assert.assertEquals;

/**
 * This class provides tools for testing.
 */
@Component
public class TestUtils {
    /** Whether to connect to test1,test2... clusters. ant -Dcluster=true. */
    public static final String PASSWORD = "rastislav";
    public static final String ID_DSA_KEY = "rastislav";
    public static final String ID_RSA_KEY = "rastislav";

    public static final String INFO_STRING       = "INFO    : ";
    public static final String APPWARNING_STRING = "WARN    : ";
    public static final int NUMBER_OF_HOSTS = 2;
    public static final List<Host> HOSTS = new ArrayList<Host>();
    public static final String TEST_USERNAME = "root";

    private static volatile boolean clusterLoaded = false;

    @Autowired
    private GUIData guiData;
    @Autowired
    private Application application;
    @Autowired
    private Provider<Host> hostProvider;
    @Autowired
    private TerminalPanel terminalPanel;
    @Autowired
    private Provider<Cluster> clusterProvider;
    @Autowired
    private ProgressIndicatorPanel glassPane;
    @Autowired
    private LCMC lcmc;
    @Autowired
    private ClusterTabFactory clusterTabFactory;

    /** Check that not even one value is null. */
    public static <T> boolean noValueIsNull(final List<T> list) {
        for (final T o : list) {
            if (o == null) {
                return false;
            }
        }
        return true;
    }

    /** Check that not even one value is null. */
    public static <T> boolean noValueIsNull(final Set<T> list) {
        for (final T o : list) {
            if (o == null) {
                return false;
            }
        }
        return true;
    }

    /** Check that not even one value is null. */
    public static <T> boolean noValueIsNull(final T[] strings) {
        for (final T s : strings) {
            if (s == null) {
                return false;
            }
        }
        return true;
    }

    /** Print error and exit. */
    public void error(final String s) {
        System.out.println(s);
        System.exit(10);
    }

    public synchronized void initMain() {
        lcmc.launch(new String[]{"--no-upgrade-check"});
        guiData.setTerminalPanel(null);
        application.waitForSwing();
    }
    
    public void initTestCluster() {
        initCluster();
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
        String username;
        boolean useSudo;
        if (TEST_USERNAME == null) {
            username = "root";
            useSudo = false;
        } else {
            username = TEST_USERNAME;
            useSudo = true;
        }
        final Cluster cluster = clusterProvider.get();
        cluster.setName("test");
        for (int i = 1; i <= NUMBER_OF_HOSTS; i++) {
            final String hostName = "test" + i;
            final Host host = hostProvider.get();
            host.init();

            initHost(host, hostName, username, useSudo);
            HOSTS.add(host);
            host.setCluster(cluster);
            cluster.addHost(host);
            final String saveFile = application.getDefaultSaveFile();
            application.saveConfig(saveFile, false);
        }
        for (final Host host : HOSTS) {
            host.disconnect();
        }
        cluster.connect(null, true, 0);
        for (final Host host : HOSTS) {
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
        for (final Host host : HOSTS) {
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

    /** Returns test hosts. */
    public List<Host> getHosts() {
        final List<Host> hosts = Collections.unmodifiableList(HOSTS);
        assertEquals(NUMBER_OF_HOSTS, hosts.size());
        return hosts;
    }

    /**
     * Wait for condition 'timeout' seconds, check every 'interval' second.
     * Return false if it ran to the timeout.
     */
    public boolean waitForCondition(final Condition condition, final int interval, final int timeout) {
        int timeoutLeft = timeout;
        while (!condition.passed() && timeout >= 0) {
            Tools.sleep(interval);
            timeoutLeft -= interval;
        }
        return timeoutLeft >= 0;
    }

    /** Check that not even one value is null. */
    public <T, K> boolean noValueIsNull(final Map<T, K> map) {
        for (final T key : map.keySet()) {
            if (key == null) {
                return false;
            }
            if (map.get(key) == null) {
                return false;
            }
        }
        return true;
    }

    /** Specify a condition to be passed to the waitForCondition function. */
    public interface Condition {
        /** returns true if condition is true. */
        boolean passed();
    }
}
