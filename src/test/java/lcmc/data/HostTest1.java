package lcmc.data;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.awt.Color;
import lcmc.utilities.TestSuite1;
import lcmc.utilities.Tools;
import lcmc.utilities.SSH;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.SSH.ExecCommandThread;

public final class HostTest1 extends TestCase {
    @Before
    protected void setUp() {
        TestSuite1.initTest();
    }

    @After
    protected void tearDown() {
        assertEquals("", TestSuite1.getStdout());
    }

    /* ---- tests ----- */

    @Test
    public void testGetBrowser() {
        for (final Host host : TestSuite1.getHosts()) {
            assertNotNull(host.getBrowser());
        }
    }

    @Test
    public void testGetDrbdColors() {
        final Set<Color> hostColors = new HashSet<Color>();
        for (final Host host : TestSuite1.getHosts()) {
            final Color[] colors = host.getDrbdColors();
            assertNotNull(colors);
            assertTrue(colors.length > 0 && colors.length <= 2);
            assertFalse(hostColors.contains(colors[0]));
            hostColors.add(colors[0]);
        }
    }

    @Test
    public void testGetPmColors() {
        final Set<Color> hostColors = new HashSet<Color>();
        for (final Host host : TestSuite1.getHosts()) {
            final Color[] colors = host.getPmColors();
            assertNotNull(colors);
            assertTrue(colors.length > 0 && colors.length <= 2);
            assertFalse(hostColors.contains(colors[0]));
            hostColors.add(colors[0]);
        }
    }

    @Test
    public void testSetClStatus() {
        for (final Host host : TestSuite1.getHosts()) {
            host.setClStatus(false);
            Tools.sleep(500); host.setClStatus(true);
            Tools.sleep(500);
        }
    }

    @Test
    public void testSetDrbdStatus() {
        for (final Host host : TestSuite1.getHosts()) {
            host.setDrbdStatus(false);
            Tools.sleep(500);
            host.setDrbdStatus(true);
            Tools.sleep(500);
        }
    }

    @Test
    public void testIsClStatus() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.isClStatus());
        }
    }

    @Test
    public void testIsDrbdStatus() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.isDrbdStatus());
        }
    }

    @Test
    public void testIsInCluster() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.isInCluster());
            assertTrue(host.isInCluster(null));
            assertTrue(host.isInCluster(new lcmc.data.Cluster()));
        }
    }

    @Test
    public void testGetNetInterfaces() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.getNetInterfaces().length > 0);
            assertNotNull(host.getNetInterfaces()[0]);
            assertTrue(TestSuite1.noValueIsNull(host.getNetInterfaces()));
        }
    }

    @Test
    public void testGetBridges() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.getBridges().size() >= 0);
            assertTrue(TestSuite1.noValueIsNull(host.getBridges()));
        }
    }

    @Test
    public void testGetBlockDevices() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.getBlockDevices().length > 0);
            assertTrue(TestSuite1.noValueIsNull(host.getBlockDevices()));
        }
    }

    @Test
    public void testGetBlockDeviceNames() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.getBlockDevicesNames().size() > 0);
            assertTrue(TestSuite1.noValueIsNull(host.getBlockDevicesNames()));
            for (final String bd : host.getBlockDevicesNames()) {
                assertTrue(host.getBlockDevice(bd) != null);
            }
        }
    }

    @Test
    public void testGetBlockDeviceNamesIntersection() {
        List<String> otherBlockDevices = null;
        for (final Host host : TestSuite1.getHosts()) {
            otherBlockDevices =
                    host.getBlockDevicesNamesIntersection(otherBlockDevices);
            assertTrue(TestSuite1.noValueIsNull(otherBlockDevices));
        }
        if (TestSuite1.getHosts().size() > 0) {
            assertTrue(!otherBlockDevices.isEmpty());
        }
    }

    @Test
    public void testGetNetworkIps() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.getNetworkIps().size() > 0);
            assertTrue(TestSuite1.noValueIsNull(host.getNetworkIps()));
        }
    }

    @Test
    public void testGetNetworksIntersection() {
        Map<String, String> otherNetworks = null;
        for (final Host host : TestSuite1.getHosts()) {
            otherNetworks = host.getNetworksIntersection(otherNetworks);
            assertTrue(TestSuite1.noValueIsNull(otherNetworks));
        }
        if (TestSuite1.getHosts().size() > 0) {
            assertTrue(!otherNetworks.isEmpty());
        }
    }

    @Test
    public void testGetIpsFromNetwork() {
        for (final Host host : TestSuite1.getHosts()) {
            final List<String> ips = host.getIpsFromNetwork("192.168.133.0");
            assertTrue(!ips.isEmpty());
            assertTrue(TestSuite1.noValueIsNull(ips));
            for (final String ip : ips) {
                assertTrue(ip.startsWith("192.168.133."));
            }
        }
    }

    @Test
    public void testGetFileSystems() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.getFileSystems().length > 0);
            assertTrue(TestSuite1.noValueIsNull(host.getFileSystems()));

            assertTrue(host.getFileSystemsList().size() > 0);
            assertTrue(TestSuite1.noValueIsNull(host.getFileSystemsList()));
        }
    }

    @Test
    public void testGetCryptoModules() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.getCryptoModules().size() > 0);
            assertTrue(TestSuite1.noValueIsNull(host.getCryptoModules()));
        }
    }

    @Test
    public void testGetQemuKeymaps() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.getQemuKeymaps().size() >= 0);
            assertTrue(TestSuite1.noValueIsNull(host.getQemuKeymaps()));
        }
    }

    @Test
    public void testGetCPUMapsModels() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.getCPUMapModels().size() > 0);
            assertTrue(TestSuite1.noValueIsNull(host.getCPUMapModels()));
        }
    }

    @Test
    public void testGetCPUMapVendor() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.getCPUMapVendors().size() > 0);
            assertTrue(TestSuite1.noValueIsNull(host.getCPUMapVendors()));
        }
    }

    @Test
    public void testGetMountPointsList() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.getMountPointsList().size() > 0);
            assertTrue(TestSuite1.noValueIsNull(host.getMountPointsList()));
        }
    }

    @Test
    public void testGetAvailableDrbdVersions() {
        for (final Host host : TestSuite1.getHosts()) {
            assertNull(host.getAvailableDrbdVersions());
        }
    }

    @Test
    public void testIsDrbdUpgradeAvailable() {
        for (final Host host : TestSuite1.getHosts()) {
            assertFalse(host.isDrbdUpgradeAvailable("8.3.1"));
            if (!TestSuite1.CONNECT_LINBIT) {
                continue;
            }
            final ExecCommandThread t = host.execCommand(
                          "DrbdAvailVersions",
                          null, /* ProgressBar */
                          new ExecCallback() {
                            @Override
                            public void done(final String ans) {
                                final String[] items = ans.split("\\r?\\n");
                                host.setDrbdVersionToInstall(
                                                        Tools.shellList(items));
                            }
                            @Override
                            public void doneError(final String ans,
                                                  final int exitCode) {
                                Tools.info("error");
                            }
                          },
                          null,   /* ConvertCmdCallback */
                          false,  /* outputVisible */
                          SSH.DEFAULT_COMMAND_TIMEOUT);
            try {
                t.join(0);
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final ExecCommandThread t2 = host.execCommand(
                          "DrbdAvailVersionsForDist",
                          null, /* ProgressBar */
                          new ExecCallback() {
                            @Override
                            public void done(final String ans) {
                                host.setAvailableDrbdVersions(
                                                         ans.split("\\r?\\n"));
                            }

                            @Override
                            public void doneError(final String ans,
                                                  final int exitCode) {
                                Tools.info("error");
                            }
                          },
                          null,   /* ConvertCmdCallback */
                          false,  /* outputVisible */
                          SSH.DEFAULT_COMMAND_TIMEOUT);
            try {
                t2.join(0);
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            assertTrue(host.isDrbdUpgradeAvailable("8.3.1"));
            assertTrue(host.isDrbdUpgradeAvailable("8.2.100"));
            assertTrue(host.isDrbdUpgradeAvailable("7.100.1"));

            assertTrue(host.isDrbdUpgradeAvailable("7"));
            assertTrue(host.isDrbdUpgradeAvailable("7.1"));
            assertTrue(host.isDrbdUpgradeAvailable("7.1.1"));

            assertFalse(host.isDrbdUpgradeAvailable("100.1.1"));
            assertFalse(host.isDrbdUpgradeAvailable(null));
            assertFalse(host.isDrbdUpgradeAvailable("21"));
            assertFalse(host.isDrbdUpgradeAvailable("21.1"));
            assertFalse(host.isDrbdUpgradeAvailable("21.1.1"));
        }
    }

    @Test
    public void testGetDrbdVersion() {
        for (final Host host : TestSuite1.getHosts()) {
            assertNotNull(host.getDrbdVersion());
            assertEquals(host.getDrbdVersion(), host.getDrbdModuleVersion());
        }
    }

    @Test
    public void testGetDistFromDistVersion() {
        for (final Host host : TestSuite1.getHosts()) {
            assertEquals(host.getDistFromDistVersion("ubuntu-lucid"), "ubuntu");
            assertEquals(host.getDistFromDistVersion("fc"), "fedora");
            assertEquals(host.getDistFromDistVersion("rhel"), "rhel");
            assertEquals(host.getDistFromDistVersion("centos"), "rhel");
            assertEquals(host.getDistFromDistVersion("xy"), null);
        }
    }

    @Test
    public void testGetKernelName() {
        for (final Host host : TestSuite1.getHosts()) {
            assertEquals(host.getKernelName(), "Linux");
        }
    }

    @Test
    public void testGetKernelVersion() {
        for (final Host host : TestSuite1.getHosts()) {

            if ("openSUSE 12.1 (x86_64)/12.1".equals(
                                            host.getDistVersionString())) {
                assertTrue("kernel version"
                           + "(" + host.getDistVersionString() + ")",
                           host.getKernelVersion() == null);
            } else {
                assertTrue("kernel version: " + host.getKernelVersion()
                           + "(" + host.getDistVersionString() + ")",
                           Character.isDigit(
                                        host.getKernelVersion().charAt(0)));
            }
        }
    }

    @Test
    public void testGetDetectedKernelVersion() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(
                Character.isDigit(host.getDetectedKernelVersion().charAt(0)));
        }
    }

    @Test
    public void testGetHeartbeatLibPath() {
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue(host.getHeartbeatLibPath().indexOf("/usr/") == 0);
        }
    }

    @Test
    public void testGetDist() {
        final Set<String> values = new HashSet<String>(
                Arrays.asList("suse",
                             "redhat",
                             "redhatenterpriseserver",
                             "rhel",
                             "fedora",
                             "debian",
                             "ubuntu"));
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue("unknown: " + host.getDist(),
                       values.contains(host.getDist()));
        }
    }

    @Test
    public void testGetDistVersion() {
        final Set<String> values = new HashSet<String>(
                Arrays.asList("debian-squeeze",
                              "debian-lenny",
                              "fedora",
                              "rhel5",
                              "rhel6",
                              "rhel7"
                              ));
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue("unknown: " + host.getDistVersion()
                       + "(" + host.getDist() + ")",
                       values.contains(host.getDistVersion())
                       || "fedora".equals(host.getDist())
                       || "suse".equals(host.getDist())
                       || "debian".equals(host.getDist())
                       || "ubuntu".equals(host.getDist()));
        }
    }

    @Test
    public void testGetDistVersionString() {
        final Set<String> values = new HashSet<String>(
                Arrays.asList("SQUEEZE",
                              "LENNY", 
                              "LUCID", 
                              "HARDY", 
                              "wheezy/sid/12.10",
                              "wheezy/sid/12.04",
                              "wheezy/sid/11.10",
                              "wheezy/sid/testing",
                              "openSUSE 12.1 (x86_64)/12.1",
                              "squeeze/sid/11.04",
                              "Fedora release 17 (Beefy Miracle)/17",
                              "OPENSUSE11_4",
                              "5",
                              "6",
                              "7",
                              "16",
                              "17"));
        for (final Host host : TestSuite1.getHosts()) {
            assertTrue("unknown: " + host.getDistVersionString(),
                       values.contains(host.getDistVersionString()));
        }
    }

    @Test
    public void testDisconnect() {
        for (final Host host : TestSuite1.getHosts()) {
            host.disconnect();
            assertFalse(host.isConnected());
            host.connect(null, null, null);
        }
        for (final Host host : TestSuite1.getHosts()) {
            for (int i = 0; i < 180; i++) {
                if (host.isConnected()) {
                    break;
                }
                Tools.sleep(1000);
            }
            assertTrue(host.isConnected());
        }
    }

}
