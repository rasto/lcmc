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

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import lcmc.AppContext;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.service.NetworkService;
import lcmc.cluster.service.storage.FileSystemService;
import lcmc.common.domain.util.Tools;
import lcmc.testutils.IntegrationTestLauncher;
import lombok.val;

@Tag("IntegrationTest")
final class HostITest {
    private IntegrationTestLauncher integrationTestLauncher;
    private NetworkService networkService;
    private FileSystemService fileSystemService;

    @BeforeEach
    void setUp() {
        integrationTestLauncher = AppContext.getBean(IntegrationTestLauncher.class);
        integrationTestLauncher.initTestCluster();
        networkService = integrationTestLauncher.getNetworkService();
        fileSystemService = integrationTestLauncher.getFileSystemService();
    }

    @Test
    void testGetBrowser() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(host.getBrowser()).isNotNull();
        }
    }

    @Test
    void testGetDrbdColors() {
        final Set<Color> hostColors = new HashSet<>();
        for (final Host host : integrationTestLauncher.getHosts()) {
            final Color[] colors = host.getDrbdColors();
            assertThat(colors).isNotNull();
            assertThat(colors.length > 0 && colors.length <= 2).isTrue();
            assertThat(hostColors.contains(colors[0])).isFalse();
            hostColors.add(colors[0]);
        }
    }

    @Test
    void testGetPmColors() {
        final Set<Color> hostColors = new HashSet<>();
        for (final Host host : integrationTestLauncher.getHosts()) {
            final Color[] colors = host.getPmColors();
            assertThat(colors).isNotNull();
            assertThat(colors.length > 0 && colors.length <= 2).isTrue();
            assertThat(hostColors.contains(colors[0])).isFalse();
            hostColors.add(colors[0]);
        }
    }

    @Test
    void testSetClStatus() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            host.setCrmStatusOk(false);
            Tools.sleep(500);
            host.setCrmStatusOk(true);
            Tools.sleep(500);
        }
    }

    @Test
    void testSetDrbdStatus() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            host.setDrbdStatusOk(false);
            Tools.sleep(500);
            host.setDrbdStatusOk(true);
            Tools.sleep(500);
        }
    }

    @Test
    void testIsClStatus() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(host.isCrmStatusOk()).isTrue();
        }
    }

    @Test
    void testIsDrbdStatus() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(host.isDrbdStatusOk()).isTrue();
        }
    }

    @Test
    void testIsInCluster() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(host.isInCluster()).isTrue();
            assertThat(host.isInCluster(null)).isTrue();
            assertThat(host.isInCluster(new Cluster(null, null, null))).isTrue();
        }
    }

    @Test
    void testGetNetInterfaces() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(networkService.getNetInterfacesWithBridges(host).length > 0).isTrue();
            assertThat(networkService.getNetInterfacesWithBridges(host)[0]).isNotNull();
            assertThat(noValueIsNull(networkService.getNetInterfacesWithBridges(host))).isTrue();
        }
    }

    @Test
    void testGetBridges() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(integrationTestLauncher.getNetworkService().getBridges(host).size() >= 0).isTrue();
        }
    }

    @Test
    void testGetNetworksIntersection() {
        Map<String, Integer> commonNetworks = networkService.getNetworksIntersection(integrationTestLauncher.getHosts());
        if (integrationTestLauncher.getHosts().size() > 0) {
            assertThat(commonNetworks).isNotNull();
            assertThat(commonNetworks).isNotEmpty();
        }
    }

    @Test
    void testGetIpsFromNetwork() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            final Collection<String> ips = networkService.getIpsFromNetwork(host, "192.168.133.0");
            assertThat(ips.isEmpty()).isFalse();
            for (final String ip : ips) {
                assertThat(ip).startsWith("192.168.133.");
            }
        }
    }

    @Test
    void testGetFileSystems() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(fileSystemService.getFileSystems(host).size() > 0).isTrue();
            assertThat(noValueIsNull(fileSystemService.getFileSystems(host))).isTrue();

            assertThat(fileSystemService.getFileSystems(host).size() > 0).isTrue();
            assertThat(noValueIsNull(fileSystemService.getFileSystems(host))).isTrue();
        }
    }

    @Test
    void testGetCryptoModules() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(host.getHostParser().getAvailableCryptoModules().size() > 0).isTrue();
            assertThat(noValueIsNull(host.getHostParser().getAvailableCryptoModules())).isTrue();
        }
    }

    @Test
    void testGetQemuKeymaps() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(host.getHostParser().getAvailableQemuKeymaps().size() > 0).isTrue();
            assertThat(noValueIsNull(host.getHostParser().getAvailableQemuKeymaps())).isTrue();
        }
    }

    @Test
    void testGetCPUMapsModels() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(host.getHostParser().getCPUMapModels().size() > 0).isTrue();
            assertThat(noValueIsNull(host.getHostParser().getCPUMapModels())).isTrue();
        }
    }

    @Test
    void testGetCPUMapVendor() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(host.getHostParser().getCPUMapVendors().size() > 0).isTrue();
            assertThat(noValueIsNull(host.getHostParser().getCPUMapVendors())).isTrue();
        }
    }

    @Test
    void testGetDistFromDistVersion() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            val hostParser = host.getHostParser();
            assertThat(hostParser.getDistFromDistVersion("ubuntu-lucid")).isEqualTo("ubuntu");
            assertThat(hostParser.getDistFromDistVersion("fc")).isEqualTo("fedora");
            assertThat(hostParser.getDistFromDistVersion("rhel")).isEqualTo("rhel");
            assertThat(hostParser.getDistFromDistVersion("centos")).isEqualTo("rhel");
            assertThat(hostParser.getDistFromDistVersion("xy")).isEqualTo("xy");
            assertThat(hostParser.getDistFromDistVersion("almalinux")).isEqualTo("redhat");
        }
    }


    @Test
    void testGetKernelName() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat("Linux").isEqualTo(host.getHostParser().getKernelName());
        }
    }

    @Test
    void testGetKernelVersion() {
        for (final Host host : integrationTestLauncher.getHosts()) {

            if ("openSUSE 12.1 (x86_64)/12.1".equals(host.getHostParser().getDistributionVersionString())) {
                assertThat(host.getHostParser().getKernelVersion() == null).describedAs(
                        "kernel version" + "(" + host.getHostParser().getDistributionVersionString() + ")").isTrue();
            } else {
                assertThat(Character.isDigit(host.getHostParser().getKernelVersion().charAt(0))).describedAs(
                        "kernel version: " + host.getHostParser().getKernelVersion() + "(" + host.getHostParser()
                                .getDistributionVersionString() + ")").isTrue();
            }
        }
    }

    @Test
    void testGetDetectedKernelVersion() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(Character.isDigit(host.getHostParser().getDetectedKernelVersion().charAt(0))).isTrue();
        }
    }

    @Test
    void testGetHeartbeatLibPath() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(host.getHostParser().getHeartbeatLibPath().indexOf("/usr/") == 0).isTrue();
        }
    }

    @Test
    void testGetDist() {
        final Set<String> values =
                new HashSet<>(Arrays.asList("suse", "redhat", "redhatenterpriseserver", "rhel", "fedora", "debian", "ubuntu"));
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(values.contains(host.getHostParser().getDistributionName())).describedAs(
                    "unknown: " + host.getHostParser().getDistributionName()).isTrue();
        }
    }

    @Test
    void testGetDistVersion() {
        final Set<String> values =
                new HashSet<>(Arrays.asList("debian-squeeze", "debian-lenny", "fedora", "rhel5", "rhel6", "rhel7"));
        for (final Host host : integrationTestLauncher.getHosts()) {
            val hostParser = host.getHostParser();
            assertThat(values.contains(hostParser.getDistributionVersion()) || "fedora".equals(hostParser.getDistributionName())
                       || "suse".equals(hostParser.getDistributionName()) || "debian".equals(hostParser.getDistributionName())
                       || "redhat".equals(hostParser.getDistributionName()) || "ubuntu".equals(
                    hostParser.getDistributionName())).describedAs(
                    "unknown: " + hostParser.getDistributionVersion() + "(" + hostParser.getDistributionName() + ")").isTrue();
        }
    }

    @Test
    void testGetDistVersionString() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            assertThat(host.getHostParser().getDistributionVersionString()).describedAs("cannot be null: ").isNotNull();
        }
    }

    @Test
    void testDisconnect() {
        for (final Host host : integrationTestLauncher.getHosts()) {
            host.disconnect();
            assertThat(host.isConnected()).isFalse();
            host.connect(null, null, null);
        }
        for (final Host host : integrationTestLauncher.getHosts()) {
            for (int i = 0; i < 180; i++) {
                if (host.isConnected()) {
                    break;
                }
                Tools.sleep(1000);
            }
            assertThat(host.isConnected()).isTrue();
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
