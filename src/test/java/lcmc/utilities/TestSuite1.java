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

package lcmc.utilities;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.File;

import java.net.UnknownHostException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import lcmc.gui.TerminalPanel;
import lcmc.data.Host;
import lcmc.data.Cluster;

/**
 * This class provides tools for testing.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class TestSuite1 {
    /** Singleton. */
    private static TestSuite1 instance = null;
    /** Whether to connect to test1,test2... clusters. ant -Dcluster=true. */
    public static final boolean CLUSTER =
                        "true".equals(System.getProperty("test.cluster"));
    /** Factor that multiplies number of tests. */
    public static final String FACTOR = System.getProperty("test.factor");
    public static final String PASSWORD = System.getProperty("test.password");
    public static final String ID_DSA_KEY = System.getProperty("test.dsa");
    public static final String ID_RSA_KEY = System.getProperty("test.rsa");

    public static final String INFO_STRING       = "INFO    : ";
    public static final String DEBUG_STRING      = "DEBUG";
    public static final String ERROR_STRING      = "ERROR   : ";
    public static final String APPWARNING_STRING = "WARN    : ";
    public static final String APPERROR_STRING   = "APPERROR: ";
    public static final int NUMBER_OF_HOSTS;
    static {
        if (Tools.isNumber(System.getProperty("test.count"))) {
            NUMBER_OF_HOSTS = Integer.parseInt(
                                            System.getProperty("test.count"));
        } else {
            NUMBER_OF_HOSTS = 3;
        }
    }
    public static final List<Host> HOSTS = new ArrayList<Host>();
    public static final String TEST_HOSTNAME =
                                            System.getenv("LCMC_TEST_HOSTNAME");
    public static final String TEST_USERNAME =
                                            System.getenv("LCMC_TEST_USERNAME");

    /** Private constructor. */
    private TestSuite1() {
        /* no instantiation possible. */
    }

    /** This is to make this class a singleton. */
    public static TestSuite1 getInstance() {
        synchronized (TestSuite1.class) {
            if (instance == null) {
                instance = new TestSuite1();
            }
        }
        return instance;
    }

    //private static PrintStream realOut = System.out;
    private static final PrintStream realOut = new PrintStream(
                                    new FileOutputStream(FileDescriptor.out));

    private static StringBuilder stdout = new StringBuilder();

    private static final OutputStream out = new OutputStream() {
         @Override
         public void write(final int b) throws IOException {
             stdout.append(String.valueOf((char) b));
         }

         @Override
         public void write(final byte[] b,
                           final int off,
                           final int len) throws IOException {
             stdout.append(new String(b, off, len));
         }

         @Override
         public void write(final byte[] b) throws IOException {
             write(b, 0, b.length);
         }
    };


    /** Clears stdout. Call it, if something writes to stdout. */
    public static void clearStdout() {
        realPrint(stdout.toString());
        stdout.delete(0, stdout.length());
    }

    /** Returns stdout as string. */
    public static String getStdout() {
        return stdout.toString();
    }

    public static void realPrintln(final String s) {
        realOut.println(s);
    }

    public static void realPrint(final String s) {
        realOut.print(s);
    }

    /** Print error and exit. */
    public static void error(final String s) {
        System.out.println(s);
        System.exit(10);
    }

    public static void initTest() {
        initTestCluster();
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
        Tools.waitForSwing();
        clearStdout();
    }
    /** Adds test cluster to the GUI. */
    public static void initTestCluster() {
        if (Tools.getGUIData() == null) {
            lcmc.LCMC.main(new String[]{"--no-upgrade-check"});
        } else {
            return;
        }
        Tools.waitForSwing();

        LoggerFactory.setDebugLevel(-1);
        if (CLUSTER) {
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
                    hostName = TEST_HOSTNAME + "-" + (char) ('a' - 1 + i);
                }
                final Host host = initHost(hostName, username, useSudo);
                HOSTS.add(host);
                host.setCluster(cluster);
                cluster.addHost(host);
                final String saveFile = Tools.getApplication().getSaveFile();
                Tools.save(saveFile, false);
            }
            for (int i = 0; i <= getFactor() / 100; i++) {
                for (final Host host : HOSTS) {
                    host.disconnect();
                }
                cluster.connect(null, true, i);
                for (final Host host : HOSTS) {
                    final boolean r = waitForCondition(
                                            new Condition() {
                                                @Override
                                                public boolean passed() {
                                                    return host.isConnected();
                                                }
                                            }, 300, 20000);
                    if (!r) {
                        error("could not establish connection to "
                              + host.getName());
                    }
                }
            }
            if (!Tools.getApplication().existsCluster(cluster)) {
                Tools.getApplication().addClusterToClusters(cluster);
                Tools.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        Tools.getGUIData().addClusterTab(cluster);
                    }
                });
            }

            Tools.getGUIData().getEmptyBrowser().addClusterBox(cluster);
            final String saveFile = Tools.getApplication().getSaveFile();
            Tools.save(saveFile, false);
            Tools.getGUIData().refreshClustersPanel();

            Tools.getGUIData().expandTerminalSplitPane(1);
            cluster.getClusterTab().addClusterView();
            cluster.getClusterTab().requestFocus();
            Tools.getGUIData().checkAddClusterButtons();
            for (final Host host : HOSTS) {
                host.waitForServerStatusLatch();
            }
        }
    }

    private static Host initHost(final String hostName,
                                 final String username,
                                 final boolean useSudo) {
        final Host host = new Host();
        host.setHostnameEntered(hostName);
        host.setUsername(username);
        host.setSSHPort("22");
        host.setUseSudo(useSudo);

        if (!Tools.getApplication().existsHost(host)) {
            Tools.getApplication().addHostToHosts(host);
            Tools.getGUIData().setTerminalPanel(new TerminalPanel(host));

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
    public static List<Host> getHosts() {
        return HOSTS;
    }

    /** Returns factor for number of iteractions. */
    public static int getFactor() {
        if (FACTOR != null && Tools.isNumber(FACTOR)) {
            return Integer.parseInt(FACTOR);
        }
        return 1;
    }

    public static List<String> getTest1Classes(final String path) {
        final List<String> fileList = new ArrayList<String>();
        if (path == null) {
            return fileList;
        }
        final File dir = new File(path);
        if (!dir.isDirectory()) {
            if (path.endsWith("Test1.class") && !path.contains("robotest")) {
                fileList.add(path);
            }
            return fileList;
        }
        for (final String fn : dir.list()) {
            if (fn.charAt(0) != '.') {
                fileList.addAll(getTest1Classes(path + "/" + fn));
            }
        }
        return fileList;
    }

    @SuppressWarnings("unchecked")
    public static void main(final String[] args) {
        final TestSuite suite = new TestSuite();
        for (final String classes : new String[]{"build/classes",
                                                 "target/test-classes"}) {
        final int startPos = classes.length() + 1;
        for (final String c : getTest1Classes(classes)) {
            final String className =
              c.substring(startPos, c.length() - 6).replaceAll("[/\\\\]", ".");
            try {
                suite.addTestSuite((Class<? extends TestCase>)
                        TestCase.class.getClassLoader().loadClass(className));
            } catch (java.lang.ClassNotFoundException e) {
                error("unusable class: " + className);
            }
        }
        }
        junit.textui.TestRunner.run(suite);
        System.exit(0);
    }

    /** Specify a condition to be passed to the waitForCondition function. */
    public interface Condition {
        /** returns true if condition is true. */
        boolean passed();
    }

    /**
     * Wait for condition 'timeout' seconds, check every 'interval' second.
     * Return false if it ran to the timeout.
     */
    public static boolean waitForCondition(final Condition condition,
                                           final int interval,
                                           final int timeout) {
        int timeoutLeft = timeout;
        while (!condition.passed() && timeout >= 0) {
            Tools.sleep(interval);
            timeoutLeft -= interval;
        }
        return timeoutLeft >= 0;
    }

    /** Check that not even one value is null. */
    public static <T, K> boolean noValueIsNull(final Map<T, K> map) {
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
}
