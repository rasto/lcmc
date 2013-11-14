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

package lcmc.data;

import lcmc.gui.ClusterTab;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.SSHGui;
import lcmc.data.resources.BlockDevice;
import lcmc.data.resources.Network;
import lcmc.utilities.Tools;
import lcmc.Exceptions;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import java.awt.Color;
import java.awt.Window;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class holds cluster data and implementation of cluster related
 * methods.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Cluster implements Comparable<Cluster> {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(Cluster.class);
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Name of the cluster. */
    private String name = null;
    /** Hosts that belong to this cluster. */
    private final Set<Host> hosts = new LinkedHashSet<Host>();
    /** Cluster tab of this cluster. */
    private ClusterTab clusterTab = null;
    /** Cluster browser of this cluster. */
    private ClusterBrowser clusterBrowser;
    /** Default colors of the hosts. */
    private final Color[] hostColors = {
                                  new Color(228, 228, 32),
                                  new Color(102, 204, 255), /* blue */
                                  Color.PINK,
                                  new Color(255, 100, 0), /* orange */
                                  Color.WHITE,
                                 };
    /** Whether this cluster should be saved. */
    private boolean savable = true;
    /** Whether the cluster tab is closable, e.g. during the cluster wizard.*/
    private boolean tabClosable = true;
    /**
     * Proxy hosts. More can be added in the DRBD config
     * wizard. */
    private final Set<Host> proxyHosts = new LinkedHashSet<Host>();


    /** Prepares a new <code>Cluster</code> object. */
    public Cluster() {
        /* do nothing */
    }

    /** Prepares a new <code>Cluster</code> object. */
    public Cluster(final String name) {
        this.name = name;
    }

    /** Creates a new cluster browser object. */
    public void createClusterBrowser() {
        clusterBrowser = new ClusterBrowser(this);
    }

    /** Sets name of this cluster. */
    public void setName(final String name) {
        this.name = name;
    }

    /** returns resource tree for this cluster. */
    public ClusterBrowser getBrowser() {
        return clusterBrowser;
    }

    /** Removes the cluster. */
    public void removeCluster() {
        LOG.debug1("removeCluster: " + getName());
        final ClusterBrowser cb = clusterBrowser;
        if (cb != null) {
            cb.stopServerStatus();
            cb.stopDrbdStatus();
            cb.stopClStatus();
        }
    }

    /** Adds host to hosts, that are part of this cluster. */
    public void addHost(final Host host) {
        final int id = hosts.size();
        host.setIndex(id);
        if (id < hostColors.length) {
            host.setColor(hostColors[id]);
        }
        hosts.add(host);
        proxyHosts.add(host);
    }

    /** Gets set of hosts that are part of this cluster. */
    public Set<Host> getHosts() {
        return hosts;
    }

    /**
     * Gets set of hosts that are part of this cluster as an array of strings.
     */
    public Host[] getHostsArray() {
        return hosts.toArray(new Host [hosts.size()]);
    }

    /** Returns names of the hosts in this cluster. */
    public String[] getHostNames() {
        final List<String> hostNames = new ArrayList<String>();
        for (Host host : hosts) {
            hostNames.add(host.getName());
        }
        return hostNames.toArray(new String[hostNames.size()]);
    }

    /** Removes all hosts. */
    public void clearHosts() {
        hosts.clear();
    }

    /** Returns number of hosts. */
    public int hostsCount() {
        return hosts.size();
    }

    /** Gets name of this cluster. */
    public String getName() {
        return name;
    }

    /** Sets cluster panel, that contains host views. */
    public void setClusterTab(final ClusterTab clusterTab) {
        this.clusterTab = clusterTab;
    }

    /** Gets cluster panel. */
    public ClusterTab getClusterTab() {
        return clusterTab;
    }

    /**
     * Gets block devices that are common on all hosts in the cluster.
     * The block devices, that are already in the heartbeat or are used by
     * drbd are not returned.
     */
    public List<String> getCommonBlockDevices() {
        List<String> blockDevicesNamesIntersection = null;

        for (final Host host : hosts) {
            blockDevicesNamesIntersection =
                                host.getBlockDevicesNamesIntersection(
                                                blockDevicesNamesIntersection);
        }

        final List<String> commonBlockDevices = new ArrayList<String>();
        for (final String i : blockDevicesNamesIntersection) {
            commonBlockDevices.add(i);
        }
        return commonBlockDevices;
    }

    /** Returns block device objects of all hosts. */
    public BlockDevice[] getHostBlockDevices(final String device) {
        final List<BlockDevice> list = new ArrayList<BlockDevice>();
        for (final Host host : hosts) {
            final BlockDevice bd = host.getBlockDevice(device);
            list.add(bd);
        }
        return list.toArray(new BlockDevice [list.size()]);
    }

    /** Returns true if cluster contains the host. */
    public boolean contains(final String hostName) {
        for (final Host host : hosts) {
            if (hostName != null && hostName.equals(host.getName())) {
                return true;
            }
        }
        return false;
    }

    /** Gets networks that are common on all hosts in the cluster. */
    public Network[] getCommonNetworks() {
        Map<String, Integer> networksIntersection = null;
        for (final Host host : hosts) {
            networksIntersection =
                            host.getNetworksIntersection(networksIntersection);
        }

        final List<Network> commonNetworks = new ArrayList<Network>();
        for (final String netIp : networksIntersection.keySet()) {
            final List<String> ips = new ArrayList<String>();
            for (final Host host : hosts) {
                ips.addAll(host.getIpsFromNetwork(netIp));
            }
            final Integer cidr = networksIntersection.get(netIp);
            final Network network =
                            new Network(netIp,
                                        ips.toArray(new String[ips.size()]),
                                        cidr);
            commonNetworks.add(network);
        }

        return commonNetworks.toArray(new Network[commonNetworks.size()]);
    }

    /** Gets filesystems that are common on all hosts in the cluster. */
    public String[] getCommonFileSystems() {
        Set<String> intersection = null;
        for (final Host host : hosts) {
            intersection = Tools.getIntersection(host.getFileSystemsList(),
                                                 intersection);
        }
        return intersection.toArray(new String[intersection.size()]);
    }

    /** Gets mount points that are common on all hosts in the cluster. */
    public String[] getCommonMountPoints() {
        Set<String> intersection = null;

        for (final Host host : hosts) {
            intersection = Tools.getIntersection(host.getMountPointsList(),
                                                 intersection);
        }
        return intersection.toArray(new String[intersection.size()]);
    }

    /** Returns the color for graph for the specified host. */
    public List<Color> getHostColors(final List<String> nodes) {
        final List<Color> colors = new ArrayList<Color>();
        if (nodes == null || nodes.isEmpty()) {
            colors.add(Tools.getDefaultColor("CRMGraph.FillPaintStopped"));
            return colors;
        }
        for (final String node : nodes) {
            for (final Host host : hosts) {
                if (node.equalsIgnoreCase(host.getName())) {
                    colors.add(host.getPmColors()[0]);
                }
            }
        }
        if (colors.isEmpty()) {
            colors.add(Color.WHITE);
        }
        // TODO: checking against name is wrong
        //Tools.appError("Error in getHostColor");
        return colors;
    }

    /** Returns the host object with the specified name. */
    public Host getHostByName(final String name) {
        for (final Host host : hosts) {
            if (name.equals(host.getName())) {
                return host;
            }
        }
        return null;
    }

    /** Connect all hosts in the cluster. Returns false, if it was canceled. */
    public boolean connect(final Window rootPane,
                           final boolean progressIndicator,
                           final int index) {
        boolean first = true;
        String dsaKey = null;
        String rsaKey = null;
        String pwd = null;
        for (final Host host : getHosts()) {
            host.setIsLoading();
            if (host.isConnected()) {
                host.setLoadingDone();
                continue;
            }
            if (!first) {
                host.getSSH().setPasswords(dsaKey, rsaKey, pwd);
            }
            if (rootPane == null) {
                host.connect(null, progressIndicator, index);
            } else {
                host.connect(new SSHGui(rootPane, host, null),
                             progressIndicator,
                             index);
            }
            host.getSSH().waitForConnection();
            if (first) {
                /* wait till it's connected and try the others with the
                 * same password/key. */
                if (host.getSSH().isConnectionCanceled()) {
                    return false;
                }
                if (host.isConnected()) {
                    dsaKey = host.getSSH().getLastDSAKey();
                    rsaKey = host.getSSH().getLastRSAKey();
                    pwd = host.getSSH().getLastPassword();
                }
            }
            first = false;
        }
        return true;
    }

    /**
     * Get the smallest libvirt version. Returns null, if it is not installed
     * anywhere.
     */
    public String getMinLibvirtVersion() {
        String minVersion = null;
        for (final Host host : hosts) {
            final String version = host.getLibvirtVersion();
            if (version == null) {
                /* not installed */
                continue;
            }
            try {
                if (minVersion == null) {
                    minVersion = version;
                } else if (Tools.compareVersions(version, minVersion) < 0) {
                    minVersion = version;
                }
            } catch (Exceptions.IllegalVersionException e) {
                LOG.appWarning("getMinLibvirtVersion: " + e.getMessage(), e);
            }
        }
        return minVersion;
    }

    /** Set whether this cluster should be saved. */
    public void setSavable(final boolean savable) {
        this.savable = savable;
    }

    /** Return whether this cluster should be saved. */
    public boolean isSavable() {
        return savable;
    }

    /** Return all proxy hosts. */
    public Set<Host> getProxyHosts() {
        return proxyHosts;
    }

    /** Add proxy host. */
    public void addProxyHost(final Host host) {
        proxyHosts.add(host);
        host.setCluster(this);
    }

    /** Return proxy host by name. */
    public Host getProxyHostByName(final String name) {
        for (final Host h : proxyHosts) {
            if (h.getName().equals(name)) {
                return h;
            }
        }
        return null;
    }

    /** Compares ignoring case. */
    @Override
    public int compareTo(final Cluster c) {
        return Tools.compareNames(getName(), c.getName());
    }

    /** Return whether the cluster tab is closable. */
    public boolean isTabClosable() {
        return tabClosable;
    }

    /** Set whether the cluster tab is closable. */
    public void setTabClosable(final boolean tabClosable) {
        this.tabClosable = tabClosable;
    }
}
