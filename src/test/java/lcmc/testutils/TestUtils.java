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

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lcmc.gui.GUIData;
import lcmc.model.Cluster;
import lcmc.model.Host;
import lcmc.model.HostFactory;
import lcmc.model.UserConfig;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;
import lcmc.view.ClusterTabFactory;

import static org.junit.Assert.assertEquals;

/**
 * This class provides tools for testing.
 */
public class TestUtils {
    /** Whether to connect to test1,test2... clusters. ant -Dcluster=true. */
    public static final String PASSWORD;
    public static final String ID_DSA_KEY;
    public static final String ID_RSA_KEY;

    public static final String INFO_STRING       = "INFO    : ";
    public static final String APPWARNING_STRING = "WARN    : ";
    public static final int NUMBER_OF_HOSTS = 2;
    public static final List<Host> HOSTS = new ArrayList<Host>();
    public static final String TEST_HOSTNAME = System.getenv("LCMC_TEST_HOSTNAME");
    public static final String TEST_USERNAME = System.getenv("LCMC_TEST_USERNAME");

    private static volatile boolean clusterLoaded = false;

    private final GUIData guiData = new GUIData();

    static {
        if (System.getProperty("test.password") == null) {
            PASSWORD = "rastislav";
        } else {
            PASSWORD = System.getProperty("test.password");
        }
    }
    static {
        if (System.getProperty("test.dsa") == null) {
            ID_DSA_KEY = "rastislav";
        } else {
            ID_DSA_KEY = System.getProperty("test.dsa");
        }
    }
    static {
        if (System.getProperty("test.rsa") == null) {
            ID_RSA_KEY = "rastislav";
        } else {
            ID_RSA_KEY = System.getProperty("test.rsa");
        }
    }

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

    private final PrintStream realOut = new PrintStream(new FileOutputStream(FileDescriptor.out));

    private StringBuilder stdout = new StringBuilder();

    private final OutputStream out = new OutputStream() {
         @Override
         public void write(final int b) throws IOException {
             stdout.append(String.valueOf((char) b));
         }

         @Override
         public void write(final byte[] b, final int off, final int len) throws IOException {
             stdout.append(new String(b, off, len));
         }

         @Override
         public void write(final byte[] b) throws IOException {
             write(b, 0, b.length);
         }
    };

    /** Clears stdout. Call it, if something writes to stdout. */
    public void clearStdout() {
        realPrint(stdout.toString());
        stdout.delete(0, stdout.length());
    }

    /** Returns stdout as string. */
    public String getStdout() {
        return stdout.toString();
    }

    public void realPrintln(final String s) {
        realOut.println(s);
    }

    public void realPrint(final String s) {
        realOut.print(s);
    }

    /** Print error and exit. */
    public void error(final String s) {
        System.out.println(s);
        System.exit(10);
        clearStdout();
    }

    public synchronized void initMain() {
        lcmc.LCMC.main(new String[]{"--no-upgrade-check"});
        guiData.setTerminalPanel(null);
        Tools.waitForSwing();
    }
    
    public void initStdout() {
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    public void initTestCluster() {
        initCluster();
        initStdout();
        Tools.waitForSwing();
        clearStdout();
    }

    /** Adds test cluster to the GUI. */
    private synchronized void initCluster() {
        if (clusterLoaded) {
            return;
        }
        initMain();
        Tools.waitForSwing();
        
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
        final Cluster cluster = new Cluster();
        cluster.setName("test");
        for (int i = 1; i <= NUMBER_OF_HOSTS; i++) {
            String hostName;
            if (TEST_HOSTNAME == null) {
                hostName = "test" + i;
            } else {
                hostName = TEST_HOSTNAME + "-" + ('a' - 1 + i);
            }
            final Host host = initHost(hostName, username, useSudo);
            HOSTS.add(host);
            host.setCluster(cluster);
            cluster.addHost(host);
            final String saveFile = Tools.getApplication().getDefaultSaveFile();
            Tools.save(guiData, new UserConfig(), saveFile, false);
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
        Tools.getApplication().addClusterToClusters(cluster);
        Tools.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                final ClusterTabFactory clusterTabFactory = new ClusterTabFactory();
                clusterTabFactory.createClusterTab(cluster);
            }
        });
        
        //guiData.getEmptyBrowser().addClusterBox(cluster);
        final String saveFile = Tools.getApplication().getDefaultSaveFile();
        Tools.save(guiData, new UserConfig(), saveFile, false);
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

    private Host initHost(final String hostName, final String username, final boolean useSudo) {
        final HostFactory hostFactory = new HostFactory();
        final Host host = hostFactory.createInstance();
        host.setEnteredHostOrIp(hostName);
        host.setUsername(username);
        host.setSSHPort("22");
        host.setUseSudo(useSudo);
        
        if (!Tools.getApplication().existsHost(host)) {
            Tools.getApplication().addHostToHosts(host);
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
