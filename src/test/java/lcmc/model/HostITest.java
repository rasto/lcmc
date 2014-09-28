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

import java.awt.Color;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lcmc.AppContext;
import lcmc.cluster.domain.Cluster;
import lcmc.host.domain.Host;
import lcmc.testutils.IntegrationTestLauncher;
import lcmc.testutils.annotation.type.IntegrationTest;
import lcmc.common.domain.util.Tools;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public final class HostITest {
    private IntegrationTestLauncher integrationTestLauncher;

    @Before
    public void setUp() {
        integrationTestLauncher = AppContext.getBean(IntegrationTestLauncher.class);
        integrationTestLauncher.initTestCluster();
    }

    @Test
    public void testGetBrowser() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertNotNull(host.getBrowser());
        }
    }

    @Test
    public void testGetDrbdColors() {
        final Set<Color> hostColors = new HashSet<Color>();
        for (final Host host : integrationTestLauncher.getHosts()) {
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
        for (final Host host : integrationTestLauncher.getHosts()) {
            final Color[] colors = host.getPmColors();
            assertNotNull(colors);
            assertTrue(colors.length > 0 && colors.length <= 2);
            assertFalse(hostColors.contains(colors[0]));
            hostColors.add(colors[0]);
        }
    }

    @Test
    public void testSetClStatus() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            host.setCrmStatusOk(false);
            Tools.sleep(500); host.setCrmStatusOk(true);
            Tools.sleep(500);
        }
    }

    @Test
    public void testSetDrbdStatus() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            host.setDrbdStatusOk(false);
            Tools.sleep(500);
            host.setDrbdStatusOk(true);
            Tools.sleep(500);
        }
    }

    @Test
    public void testIsClStatus() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.isCrmStatusOk());
        }
    }

    @Test
    public void testIsDrbdStatus() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.isDrbdStatusOk());
        }
    }

    @Test
    public void testIsInCluster() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.isInCluster());
            assertTrue(host.isInCluster(null));
            assertTrue(host.isInCluster(new Cluster()));
        }
    }

    @Test
    public void testGetNetInterfaces() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getNetInterfacesWithBridges().length > 0);
            assertNotNull(host.getNetInterfacesWithBridges()[0]);
            assertTrue(noValueIsNull(host.getNetInterfacesWithBridges()));
        }
    }

    @Test
    public void testGetBridges() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getBridges().size() >= 0);
            assertTrue(noValueIsNull(host.getBridges()));
        }
    }

    @Test
    public void testGetBlockDevices() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getBlockDevices().length > 0);
            assertTrue(noValueIsNull(host.getBlockDevices()));
        }
    }

    @Test
    public void testGetBlockDeviceNames() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getBlockDevicesNames().size() > 0);
            assertTrue(noValueIsNull(host.getBlockDevicesNames()));
            for (final String bd : host.getBlockDevicesNames()) {
                assertTrue(host.getBlockDevice(bd) != null);
            }
        }
    }

    @Test
    public void testGetBlockDeviceNamesIntersection() {
        List<String> otherBlockDevices = null;
        for (final Host host : integrationTestLauncher.getHosts()) {
            otherBlockDevices =
                    host.getBlockDevicesNamesIntersection(otherBlockDevices);
            assertTrue(noValueIsNull(otherBlockDevices));
        }
        if (integrationTestLauncher.getHosts().size() > 0) {
            assertNotNull(otherBlockDevices);
            assertTrue(!otherBlockDevices.isEmpty());
        }
    }

    @Test
    public void testGetNetworkIps() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getNetworkIps().size() > 0);
            assertTrue(noValueIsNull(host.getNetworkIps()));
        }
    }

    @Test
    public void testGetNetworksIntersection() {
        Map<String, Integer> otherNetworks = null;
        for (final Host host : integrationTestLauncher.getHosts()) {
            otherNetworks = host.getNetworksIntersection(otherNetworks);
            assertTrue(noValueIsNull(otherNetworks));
        }
        if (integrationTestLauncher.getHosts().size() > 0) {
            assertNotNull(otherNetworks);
            assertTrue(!otherNetworks.isEmpty());
        }
    }

    @Test
    public void testGetIpsFromNetwork() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            final List<String> ips = host.getIpsFromNetwork("192.168.133.0");
            assertTrue(!ips.isEmpty());
            assertTrue(noValueIsNull(ips));
            for (final String ip : ips) {
                assertTrue(ip.startsWith("192.168.133."));
            }
        }
    }

    @Test
    public void testGetFileSystems() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getAvailableFileSystems().length > 0);
            assertTrue(noValueIsNull(host.getAvailableFileSystems()));

            assertTrue(host.getFileSystemsList().size() > 0);
            assertTrue(noValueIsNull(host.getFileSystemsList()));
        }
    }

    @Test
    public void testGetCryptoModules() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getAvailableCryptoModules().size() > 0);
            assertTrue(noValueIsNull(host.getAvailableCryptoModules()));
        }
    }

    @Test
    public void testGetQemuKeymaps() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getAvailableQemuKeymaps().size() >= 0);
            assertTrue(noValueIsNull(host.getAvailableQemuKeymaps()));
        }
    }

    @Test
    public void testGetCPUMapsModels() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getCPUMapModels().size() > 0);
            assertTrue(noValueIsNull(host.getCPUMapModels()));
        }
    }

    @Test
    public void testGetCPUMapVendor() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getCPUMapVendors().size() > 0);
            assertTrue(noValueIsNull(host.getCPUMapVendors()));
        }
    }

    @Test
    public void testGetMountPointsList() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getMountPointsList().size() > 0);
            assertTrue(noValueIsNull(host.getMountPointsList()));
        }
    }

    @Test
    public void testGetDistFromDistVersion() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertEquals(host.getDistFromDistVersion("ubuntu-lucid"), "ubuntu");
            assertEquals(host.getDistFromDistVersion("fc"), "fedora");
            assertEquals(host.getDistFromDistVersion("rhel"), "rhel");
            assertEquals(host.getDistFromDistVersion("centos"), "rhel");
            assertEquals(host.getDistFromDistVersion("xy"), null);
        }
    }

    @Test
    public void testGetKernelName() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertEquals(host.getKernelName(), "Linux");
        }
    }

    @Test
    public void testGetKernelVersion() {
        for (final Host host : integrationTestLauncher.getHosts()) {

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
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(
                Character.isDigit(host.getDetectedKernelVersion().charAt(0)));
        }
    }

    @Test
    public void testGetHeartbeatLibPath() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getHeartbeatLibPath().indexOf("/usr/") == 0);
        }
    }

    @Test
    public void testGetDist() {
        final Set<String> values = new HashSet<String>(Arrays.asList("suse",
                                                                     "redhat",
                                                                     "redhatenterpriseserver",
                                                                     "rhel",
                                                                     "fedora",
                                                                     "debian",
                                                                     "ubuntu"));
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue("unknown: " + host.getDistributionName(), values.contains(host.getDistributionName()));
        }
    }

    @Test
    public void testGetDistVersion() {
        final Set<String> values = new HashSet<String>(Arrays.asList("debian-squeeze",
                                                                     "debian-lenny",
                                                                     "fedora",
                                                                     "rhel5",
                                                                     "rhel6",
                                                                     "rhel7"
                                                                     ));
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue("unknown: " + host.getDistributionVersion() + "(" + host.getDistributionName() + ")",
                    values.contains(host.getDistributionVersion())
                            || "fedora".equals(host.getDistributionName())
                            || "suse".equals(host.getDistributionName())
                            || "debian".equals(host.getDistributionName())
                            || "ubuntu".equals(host.getDistributionName()));
        }
    }

    @Test
    public void testGetDistVersionString() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertNotNull("cannot be null: ", host.getDistributionVersionString());
        }
    }

    @Test
    public void testDisconnect() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            host.disconnect();
            assertFalse(host.isConnected());
            host.connect(null, null, null);
        }
        for (final Host host : integrationTestLauncher.getHosts()) {
            for (int i = 0; i < 180; i++) {
                if (host.isConnected()) {
                    break;
                }
                Tools.sleep(1000);
            }
            assertTrue(host.isConnected());
        }
    }

    /** Check that not even one value is null. */
    private <T, K> boolean noValueIsNull(final Map<T, K> map) {
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
    private <T> boolean noValueIsNull(final T[] strings) {
        for (final T s : strings) {
            if (s == null) {
                return false;
            }
        }
        return true;
    }

    /** Check that not even one value is null. */
    private <T> boolean noValueIsNull(final List<T> list) {
        for (final T o : list) {
            if (o == null) {
                return false;
            }
        }
        return true;
    }

    /** Check that not even one value is null. */
    private <T> boolean noValueIsNull(final Set<T> list) {
        for (final T o : list) {
            if (o == null) {
                return false;
            }
        }
        return true;
    }
}
