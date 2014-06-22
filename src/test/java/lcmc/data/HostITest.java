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

package lcmc.data;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lcmc.testutils.TestUtils;
import lcmc.testutils.annotation.type.IntegrationTest;
import lcmc.utilities.Tools;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public final class HostITest {

    private final TestUtils testSuite = new TestUtils();

    @Before
    public void setUp() {
        testSuite.initTestCluster();
    }

    @Test
    public void testGetBrowser() {
        for (final Host host : getHosts()) {
            assertNotNull(host.getBrowser());
        }
    }

    @Test
    public void testGetDrbdColors() {
        final Set<Color> hostColors = new HashSet<Color>();
        for (final Host host : getHosts()) {
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
        for (final Host host : getHosts()) {
            final Color[] colors = host.getPmColors();
            assertNotNull(colors);
            assertTrue(colors.length > 0 && colors.length <= 2);
            assertFalse(hostColors.contains(colors[0]));
            hostColors.add(colors[0]);
        }
    }

    @Test
    public void testSetClStatus() {
        for (final Host host : getHosts()) {
            host.setCrmStatusOk(false);
            Tools.sleep(500); host.setCrmStatusOk(true);
            Tools.sleep(500);
        }
    }

    @Test
    public void testSetDrbdStatus() {
        for (final Host host : getHosts()) {
            host.setDrbdStatusOk(false);
            Tools.sleep(500);
            host.setDrbdStatusOk(true);
            Tools.sleep(500);
        }
    }

    @Test
    public void testIsClStatus() {
        for (final Host host : getHosts()) {
            assertTrue(host.isCrmStatusOk());
        }
    }

    @Test
    public void testIsDrbdStatus() {
        for (final Host host : getHosts()) {
            assertTrue(host.isDrbdStatusOk());
        }
    }

    @Test
    public void testIsInCluster() {
        for (final Host host : getHosts()) {
            assertTrue(host.isInCluster());
            assertTrue(host.isInCluster(null));
            assertTrue(host.isInCluster(new lcmc.data.Cluster()));
        }
    }

    @Test
    public void testGetNetInterfaces() {
        for (final Host host : getHosts()) {
            assertTrue(host.getNetInterfacesWithBridges().length > 0);
            assertNotNull(host.getNetInterfacesWithBridges()[0]);
            assertTrue(TestUtils.noValueIsNull(host.getNetInterfacesWithBridges()));
        }
    }

    @Test
    public void testGetBridges() {
        for (final Host host : getHosts()) {
            assertTrue(host.getBridges().size() >= 0);
            assertTrue(TestUtils.noValueIsNull(host.getBridges()));
        }
    }

    @Test
    public void testGetBlockDevices() {
        for (final Host host : getHosts()) {
            assertTrue(host.getBlockDevices().length > 0);
            assertTrue(TestUtils.noValueIsNull(host.getBlockDevices()));
        }
    }

    @Test
    public void testGetBlockDeviceNames() {
        for (final Host host : getHosts()) {
            assertTrue(host.getBlockDevicesNames().size() > 0);
            assertTrue(TestUtils.noValueIsNull(host.getBlockDevicesNames()));
            for (final String bd : host.getBlockDevicesNames()) {
                assertTrue(host.getBlockDevice(bd) != null);
            }
        }
    }

    @Test
    public void testGetBlockDeviceNamesIntersection() {
        List<String> otherBlockDevices = null;
        for (final Host host : getHosts()) {
            otherBlockDevices =
                    host.getBlockDevicesNamesIntersection(otherBlockDevices);
            assertTrue(TestUtils.noValueIsNull(otherBlockDevices));
        }
        if (testSuite.getHosts().size() > 0) {
            assertNotNull(otherBlockDevices);
            assertTrue(!otherBlockDevices.isEmpty());
        }
    }

    @Test
    public void testGetNetworkIps() {
        for (final Host host : getHosts()) {
            assertTrue(host.getNetworkIps().size() > 0);
            assertTrue(testSuite.noValueIsNull(host.getNetworkIps()));
        }
    }

    @Test
    public void testGetNetworksIntersection() {
        Map<String, Integer> otherNetworks = null;
        for (final Host host : getHosts()) {
            otherNetworks = host.getNetworksIntersection(otherNetworks);
            assertTrue(testSuite.noValueIsNull(otherNetworks));
        }
        if (getHosts().size() > 0) {
            assertNotNull(otherNetworks);
            assertTrue(!otherNetworks.isEmpty());
        }
    }

    @Test
    public void testGetIpsFromNetwork() {
        for (final Host host : getHosts()) {
            final List<String> ips = host.getIpsFromNetwork("192.168.133.0");
            assertTrue(!ips.isEmpty());
            assertTrue(TestUtils.noValueIsNull(ips));
            for (final String ip : ips) {
                assertTrue(ip.startsWith("192.168.133."));
            }
        }
    }

    @Test
    public void testGetFileSystems() {
        for (final Host host : getHosts()) {
            assertTrue(host.getAvailableFileSystems().length > 0);
            assertTrue(TestUtils.noValueIsNull(host.getAvailableFileSystems()));

            assertTrue(host.getFileSystemsList().size() > 0);
            assertTrue(TestUtils.noValueIsNull(host.getFileSystemsList()));
        }
    }

    @Test
    public void testGetCryptoModules() {
        for (final Host host : getHosts()) {
            assertTrue(host.getAvailableCryptoModules().size() > 0);
            assertTrue(TestUtils.noValueIsNull(host.getAvailableCryptoModules()));
        }
    }

    @Test
    public void testGetQemuKeymaps() {
        for (final Host host : getHosts()) {
            assertTrue(host.getAvailableQemuKeymaps().size() >= 0);
            assertTrue(TestUtils.noValueIsNull(host.getAvailableQemuKeymaps()));
        }
    }

    @Test
    public void testGetCPUMapsModels() {
        for (final Host host : getHosts()) {
            assertTrue(host.getCPUMapModels().size() > 0);
            assertTrue(TestUtils.noValueIsNull(host.getCPUMapModels()));
        }
    }

    @Test
    public void testGetCPUMapVendor() {
        for (final Host host : getHosts()) {
            assertTrue(host.getCPUMapVendors().size() > 0);
            assertTrue(TestUtils.noValueIsNull(host.getCPUMapVendors()));
        }
    }

    @Test
    public void testGetMountPointsList() {
        for (final Host host : getHosts()) {
            assertTrue(host.getMountPointsList().size() > 0);
            assertTrue(TestUtils.noValueIsNull(host.getMountPointsList()));
        }
    }

    @Test
    public void testGetDistFromDistVersion() {
        for (final Host host : getHosts()) {
            assertEquals(host.getDistFromDistVersion("ubuntu-lucid"), "ubuntu");
            assertEquals(host.getDistFromDistVersion("fc"), "fedora");
            assertEquals(host.getDistFromDistVersion("rhel"), "rhel");
            assertEquals(host.getDistFromDistVersion("centos"), "rhel");
            assertEquals(host.getDistFromDistVersion("xy"), null);
        }
    }

    @Test
    public void testGetKernelName() {
        for (final Host host : getHosts()) {
            assertEquals(host.getKernelName(), "Linux");
        }
    }

    @Test
    public void testGetKernelVersion() {
        for (final Host host : getHosts()) {

            if ("openSUSE 12.1 (x86_64)/12.1".equals(
                                            host.getDistributionVersionString())) {
                assertTrue("kernel version"
                           + "(" + host.getDistributionVersionString() + ")",
                           host.getKernelVersion() == null);
            } else {
                assertTrue("kernel version: " + host.getKernelVersion()
                           + "(" + host.getDistributionVersionString() + ")",
                           Character.isDigit(
                                        host.getKernelVersion().charAt(0)));
            }
        }
    }

    @Test
    public void testGetDetectedKernelVersion() {
        for (final Host host : getHosts()) {
            assertTrue(
                Character.isDigit(host.getDetectedKernelVersion().charAt(0)));
        }
    }

    @Test
    public void testGetHeartbeatLibPath() {
        for (final Host host : getHosts()) {
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
        for (final Host host : getHosts()) {
            assertTrue("unknown: " + host.getDistributionName(),
                       values.contains(host.getDistributionName()));
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
        for (final Host host : getHosts()) {
            assertTrue("unknown: " + host.getDistributionVersion()
                       + "(" + host.getDistributionName() + ")",
                       values.contains(host.getDistributionVersion())
                       || "fedora".equals(host.getDistributionName())
                       || "suse".equals(host.getDistributionName())
                       || "debian".equals(host.getDistributionName())
                       || "ubuntu".equals(host.getDistributionName()));
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
                              "wheezy/sid/13.04",
                              "openSUSE 12.1 (x86_64)/12.1",
                              "squeeze/sid/11.04",
                              "Fedora release 17 (Beefy Miracle)/17",
                              "Fedora release 18 (Spherical Cow)/18",
                              "OPENSUSE11_4",
                              "openSUSE 12.3 (x86_64)/12.3",
                              "5",
                              "6",
                              "7",
                              "7.1/7.1",
                              "7.2/7.2",
                              "7.3/7.3",
                              "7.4/7.4",
                              "7.5/7.5",
                              "16",
                              "17"));
        for (final Host host : getHosts()) {
            assertTrue("unknown: " + host.getDistributionVersionString(),
                       values.contains(host.getDistributionVersionString()));
        }
    }

    @Test
    public void testDisconnect() {
        for (final Host host : getHosts()) {
            host.disconnect();
            assertFalse(host.isConnected());
            host.connect(null, null, null);
        }
        for (final Host host : getHosts()) {
            for (int i = 0; i < 180; i++) {
                if (host.isConnected()) {
                    break;
                }
                Tools.sleep(1000);
            }
            assertTrue(host.isConnected());
        }
    }

    private String stripVersion(final String v) {
        final int i = v.lastIndexOf('.');
        if (i < 0) {
            return v;
        } else {
            return v.substring(0, i);
        }
    }
    
    private List<Host> getHosts() {
        return testSuite.getHosts();
    }
}
