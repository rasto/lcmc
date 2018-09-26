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

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lcmc.cluster.domain.Cluster;
import lcmc.cluster.service.NetworkService;
import lcmc.cluster.service.storage.FileSystemService;
import lcmc.testutils.IntegrationTestLauncher;
import lcmc.testutils.annotation.type.IntegrationTest;
import lcmc.common.domain.util.Tools;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public final class HostITest {
    private IntegrationTestLauncher integrationTestLauncher;
    private NetworkService networkService;
    private FileSystemService fileSystemService;

    @Before
    public void setUp() {
        integrationTestLauncher = IntegrationTestLauncher.create();
        integrationTestLauncher.initTestCluster();
        networkService = integrationTestLauncher.getNetworkService();
        fileSystemService = integrationTestLauncher.getFileSystemService();
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
            assertTrue(networkService.getNetInterfacesWithBridges(host).length > 0);
            assertNotNull(networkService.getNetInterfacesWithBridges(host)[0]);
            assertTrue(noValueIsNull(networkService.getNetInterfacesWithBridges(host)));
        }
    }

    @Test
    public void testGetBridges() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(integrationTestLauncher.getNetworkService().getBridges(host).size() >= 0);
        }
    }

    @Test
    public void testGetNetworksIntersection() {
        Map<String, Integer> commonNetworks = networkService.getNetworksIntersection(
                integrationTestLauncher.getHosts());
        if (integrationTestLauncher.getHosts().size() > 0) {
            assertNotNull(commonNetworks);
            assertTrue(!commonNetworks.isEmpty());
        }
    }

    @Test
    public void testGetIpsFromNetwork() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            final Collection<String> ips = networkService.getIpsFromNetwork(host, "192.168.133.0");
            assertTrue(!ips.isEmpty());
            for (final String ip : ips) {
                assertTrue(ip.startsWith("192.168.133."));
            }
        }
    }

    @Test
    public void testGetFileSystems() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(fileSystemService.getFileSystems(host).size() > 0);
            assertTrue(noValueIsNull(fileSystemService.getFileSystems(host)));

            assertTrue(fileSystemService.getFileSystems(host).size() > 0);
            assertTrue(noValueIsNull(fileSystemService.getFileSystems(host)));
        }
    }

    @Test
    public void testGetCryptoModules() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getHostParser().getAvailableCryptoModules().size() > 0);
            assertTrue(noValueIsNull(host.getHostParser().getAvailableCryptoModules()));
        }
    }

    @Test
    public void testGetQemuKeymaps() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getHostParser().getAvailableQemuKeymaps().size() >= 0);
            assertTrue(noValueIsNull(host.getHostParser().getAvailableQemuKeymaps()));
        }
    }

    @Test
    public void testGetCPUMapsModels() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getHostParser().getCPUMapModels().size() > 0);
            assertTrue(noValueIsNull(host.getHostParser().getCPUMapModels()));
        }
    }

    @Test
    public void testGetCPUMapVendor() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getHostParser().getCPUMapVendors().size() > 0);
            assertTrue(noValueIsNull(host.getHostParser().getCPUMapVendors()));
        }
    }

    @Test
    public void testGetDistFromDistVersion() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            val hostParser = host.getHostParser();
            assertEquals(hostParser.getDistFromDistVersion("ubuntu-lucid"), "ubuntu");
            assertEquals(hostParser.getDistFromDistVersion("fc"), "fedora");
            assertEquals(hostParser.getDistFromDistVersion("rhel"), "rhel");
            assertEquals(hostParser.getDistFromDistVersion("centos"), "rhel");
            assertEquals(hostParser.getDistFromDistVersion("xy"), null);
        }
    }

    @Test
    public void testGetKernelName() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertEquals(host.getHostParser().getKernelName(), "Linux");
        }
    }

    @Test
    public void testGetKernelVersion() {
        for (final Host host : integrationTestLauncher.getHosts()) {

            if ("openSUSE 12.1 (x86_64)/12.1".equals(
                                            host.getHostParser().getDistributionVersionString())) {
                assertTrue("kernel version"
                           + "(" + host.getHostParser().getDistributionVersionString() + ")",
                           host.getHostParser().getKernelVersion() == null);
            } else {
                assertTrue("kernel version: " + host.getHostParser().getKernelVersion()
                           + "(" + host.getHostParser().getDistributionVersionString() + ")",
                           Character.isDigit(
                                        host.getHostParser().getKernelVersion().charAt(0)));
            }
        }
    }

    @Test
    public void testGetDetectedKernelVersion() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(
                Character.isDigit(host.getHostParser().getDetectedKernelVersion().charAt(0)));
        }
    }

    @Test
    public void testGetHeartbeatLibPath() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertTrue(host.getHostParser().getHeartbeatLibPath().indexOf("/usr/") == 0);
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
            assertTrue("unknown: " + host.getHostParser().getDistributionName(), values.contains(host.getHostParser().getDistributionName()));
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
            val hostParser = host.getHostParser();
            assertTrue("unknown: " + hostParser.getDistributionVersion() + "(" + hostParser.getDistributionName() + ")",
                    values.contains(hostParser.getDistributionVersion())
                            || "fedora".equals(hostParser.getDistributionName())
                            || "suse".equals(hostParser.getDistributionName())
                            || "debian".equals(hostParser.getDistributionName())
                            || "ubuntu".equals(hostParser.getDistributionName()));
        }
    }

    @Test
    public void testGetDistVersionString() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertNotNull("cannot be null: ", host.getHostParser().getDistributionVersionString());
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
