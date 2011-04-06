/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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

package drbd;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.File;

import java.net.UnknownHostException;
import java.net.InetAddress;
import java.util.List;
import java.util.ArrayList;
import junit.framework.TestSuite;
import junit.framework.TestResult;
import junit.framework.TestCase;

import drbd.utilities.Tools;
import drbd.gui.SSHGui;
import drbd.gui.TerminalPanel;
import drbd.data.Host;
import drbd.data.Cluster;

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
    /** Whether to test interactive elements. ant -Dinteractive=true. */
    public static final boolean INTERACTIVE =
                        "true".equals(System.getProperty("test.interactive"));
    /** Whether to test plugins and version. ant -Dconnect=true. */
    public static final boolean CONNECT_LINBIT =
                        "true".equals(System.getProperty("test.connect"));
    /** Whether to connect to test1,test2... clusters. ant -Dcluster=true. */
    public static final boolean TESTCLUSTER =
                        "true".equals(System.getProperty("test.testcluster"));
    /** Factor that multiplies number of tests. */
    public static final String FACTOR = System.getProperty("test.factor");
    public static final String PASSWORD = System.getProperty("test.password");
    public static final String ID_DSA_KEY = System.getProperty("test.dsa");
    public static final String ID_RSA_KEY = System.getProperty("test.rsa");

    public static final String INFO_STRING = "INFO: ";
    public static final String DEBUG_STRING = "DEBUG: ";
    public static final String ERROR_STRING = "ERROR: ";
    public static final String APPWARNING_STRING = "APPWARNING: ";
    public static final String APPERROR_STRING = "APPERROR: ";
    public static final int NUMBER_OF_HOSTS = 3;
    public static final List<Host> HOSTS = new ArrayList<Host>();

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
    private static PrintStream realOut = new PrintStream(
                                    new FileOutputStream(FileDescriptor.out));

    private static StringBuilder stdout = new StringBuilder();

    private static OutputStream out = new OutputStream() {
         @Override public void write(final int b) throws IOException {
             stdout.append(String.valueOf((char) b));
         }

         @Override public void write(final byte[] b,
                                     final int off,
                                     final int len) throws IOException {
             stdout.append(new String(b, off, len));
         }

         @Override public void write(final byte[] b) throws IOException {
             write(b, 0, b.length);
         }
    };


    /** Clears stdout. Call it, if something writes to stdout. */
    public static void clearStdout() {
        stdout.delete(0, stdout.length());
    }

    /** Returns stdout as string. */
    public static String getStdout() {
        return stdout.toString();
    }

    public static void realPrintln(final String s) {
        realOut.println(s);
    }

    /** Print error and exit. */
    public static void error(final String s) {
        System.exit(10);
    }

    public static void initTest() {
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
        Tools.waitForSwing();
        clearStdout();
    }
    /** Adds test cluster to the GUI. */
    private static void initTestCluster() {
        if (Tools.getGUIData() == null) {
            if (CONNECT_LINBIT) {
                drbd.DrbdMC.main(new String[]{});
                /* plugins registration writes to stdout after a while. */
                Tools.sleep(15000);
            } else {
                drbd.DrbdMC.main(new String[]{"--no-plugin-check",
                                              "--no-upgrade-check"});
            }
        }
        Tools.setDebugLevel(-1);
        final String username = "root";
        final boolean useSudo = false;
        final Cluster cluster = new Cluster();
        cluster.setName("test");
        for (int i = 1; i <= NUMBER_OF_HOSTS; i++) {
            final Host host = initHost("test" + i, username, useSudo);
            HOSTS.add(host);
            host.setCluster(cluster);
            cluster.addHost(host);
            final String saveFile = Tools.getConfigData().getSaveFile();
            Tools.save(saveFile);
        }
        if (!Tools.getConfigData().existsCluster(cluster)) {
            Tools.getConfigData().addClusterToClusters(cluster);
            Tools.getGUIData().addClusterTab(cluster);
        }

        Tools.getGUIData().getEmptyBrowser().addClusterBox(cluster);
        final String saveFile = Tools.getConfigData().getSaveFile();
        Tools.save(saveFile);
        Tools.getGUIData().refreshClustersPanel();

        Tools.getGUIData().expandTerminalSplitPane(1);
        cluster.getClusterTab().addClusterView();
        cluster.getClusterTab().requestFocus();
        Tools.getGUIData().checkAddClusterButtons();
    }

    private static Host initHost(final String hostName,
                                 final String username,
                                 final boolean useSudo) {
        final Host host = new Host();
        host.setHostnameEntered(hostName);
        host.setUsername(username);
        host.setSSHPort("22");
        host.setUseSudo(useSudo);

        if (!Tools.getConfigData().existsHost(host)) {
            Tools.getConfigData().addHostToHosts(host);
            Tools.getGUIData().setTerminalPanel(new TerminalPanel(host));

        }
        String ip = null;
        InetAddress[] addresses = null;
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
        host.setIp(ip);
        host.setIps(0, new String[]{ip});
        final SSHGui sshGui = new SSHGui(Tools.getGUIData().getMainFrame(),
                                         host,
                                         null);
        for (int i = 0; i < getFactor(); i++) {
            host.disconnect();
            host.connect(sshGui, null);
            final boolean r = waitForCondition(new Condition() {
                                                  public boolean passed() {
                                                      return host.isConnected();
                                                  }
                                               }, 1000, 20000);
            if (!r) {
                error("could not establish connection to " + host.getName());
            }
        }

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
            if (path.endsWith("Test1.class")) {
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
        initTestCluster();

        final TestSuite suite = new TestSuite();
        final int startPos = "build/classes".length() + 1;
        for (final String c : getTest1Classes("build/classes")) {
            final String className =
              c.substring(startPos, c.length() - 6).replaceAll("[/\\\\]", ".");
            try {
                suite.addTestSuite((Class<? extends TestCase>)
                        TestCase.class.getClassLoader().loadClass(className));
            } catch (java.lang.ClassNotFoundException e) {
                error("unusable class: " + className);
            }
        }
        final TestResult testResult = new TestResult();
        junit.textui.TestRunner.run(suite);
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
}
