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

package lcmc.cluster.domain;

import java.awt.Color;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import lcmc.Exceptions;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.drbd.domain.BlockDevice;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.ClusterTab;
import lcmc.cluster.ui.SSHGui;
import lcmc.cluster.service.storage.BlockDeviceService;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This class holds cluster data and implementation of cluster related
 * methods.
 */
@Named
public class Cluster implements Comparable<Cluster> {
    private static final Logger LOG = LoggerFactory.getLogger(Cluster.class);
    private String name = null;
    private final Set<Host> hosts = new LinkedHashSet<Host>();
    private ClusterTab clusterTab = null;
    /** Default colors of the hosts. */
    private static final Color[] DEFAULT_HOST_COLORS = {new Color(228, 228, 32),
                                                   new Color(102, 204, 255), /* blue */
                                                   Color.PINK,
                                                   new Color(255, 100, 0), /* orange */
                                                   Color.WHITE,
                                                  };
    private boolean savable = true;
    private boolean clusterTabClosable = true;

    private ClusterBrowser clusterBrowser;
    @Inject
    private MainData mainData;
    @Inject
    private SwingUtils swingUtils;
    /**
     * Proxy hosts. More can be added in the DRBD config
     * wizard. */
    private final Set<Host> proxyHosts = new LinkedHashSet<Host>();
    @Inject
    private BlockDeviceService blockDeviceService;

    public void setName(final String name) {
        this.name = name;
    }

    public void setBrowser(final ClusterBrowser clusterBrowser) {
        this.clusterBrowser = clusterBrowser;
    }

    public ClusterBrowser getBrowser() {
        return clusterBrowser;
    }

    public void removeCluster() {
        LOG.debug1("removeCluster: " + name);
        final ClusterBrowser cb = clusterBrowser;
        if (cb != null) {
            cb.stopServerStatus();
            cb.stopDrbdStatusOnAllHosts();
            cb.stopCrmStatus();
        }
    }

    public void addHost(final Host host) {
        final int id = hosts.size();
        host.setPositionInTheCluster(id);
        if (id < DEFAULT_HOST_COLORS.length) {
            host.setColor(DEFAULT_HOST_COLORS[id]);
        }
        hosts.add(host);
        proxyHosts.add(host);
    }

    public Set<Host> getHosts() {
        return hosts;
    }

    public Host[] getHostsArray() {
        return hosts.toArray(new Host [hosts.size()]);
    }

    public String[] getHostNames() {
        final List<String> hostNames = new ArrayList<String>();
        for (final Host host : hosts) {
            hostNames.add(host.getName());
        }
        return hostNames.toArray(new String[hostNames.size()]);
    }

    public void removeAllHosts() {
        hosts.clear();
    }

    public int hostsCount() {
        return hosts.size();
    }

    public String getName() {
        return name;
    }

    public void setClusterTab(final ClusterTab clusterTab) {
        this.clusterTab = clusterTab;
    }

    public ClusterTab getClusterTab() {
        return clusterTab;
    }

    /** Returns block device objects of all hosts. */
    public BlockDevice[] getHostBlockDevices(final String device) {
        final List<BlockDevice> list = new ArrayList<BlockDevice>();
        for (final Host host : hosts) {
            final Optional<BlockDevice> bd = blockDeviceService.getBlockDeviceByName(host, device);
            if (bd.isPresent()) {
                list.add(bd.get());
            }
        }
        return list.toArray(new BlockDevice [list.size()]);
    }

    public boolean contains(final String hostName) {
        for (final Host host : hosts) {
            if (hostName != null && hostName.equals(host.getName())) {
                return true;
            }
        }
        return false;
    }

    public List<Color> getHostColorsInGraphs(final Collection<String> nodes) {
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

    public Host getHostByName(final String name) {
        for (final Host host : hosts) {
            if (name.equals(host.getName())) {
                return host;
            }
        }
        return null;
    }

    /** Connect all hosts in the cluster. Returns false, if it was canceled. */
    public boolean connect(final Window rootPane, final boolean progressIndicator, final int index) {
        boolean first = true;
        String dsaKey = null;
        String rsaKey = null;
        String pwd = null;
        for (final Host host : hosts) {
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
                host.connect(new SSHGui(rootPane, host, null), progressIndicator, index);
            }
            host.getSSH().waitForConnection();
            if (first) {
                /* wait till it's connected and try the others with the
                 * same password/key. */
                if (host.getSSH().isConnectionCanceled()) {
                    return false;
                }
                if (host.isConnected()) {
                    dsaKey = host.getSSH().getLastSuccessfulDsaKey();
                    rsaKey = host.getSSH().getLastSuccessfulRsaKey();
                    pwd = host.getSSH().getLastSuccessfulPassword();
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
            final String version = host.getHostParser().getLibvirtVersion();
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
            } catch (final Exceptions.IllegalVersionException e) {
                LOG.appWarning("getMinLibvirtVersion: " + e.getMessage(), e);
            }
        }
        return minVersion;
    }

    public void setSavable(final boolean savable) {
        this.savable = savable;
    }

    public boolean isSavable() {
        return savable;
    }

    public Set<Host> getProxyHosts() {
        return proxyHosts;
    }

    public void addProxyHost(final Host host) {
        proxyHosts.add(host);
        host.setCluster(this);
    }

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
    public int compareTo(final Cluster o) {
        return Tools.compareNames(name, o.name);
    }

    public boolean isClusterTabClosable() {
        return clusterTabClosable;
    }

    public void setClusterTabClosable(final boolean clusterTabClosable) {
        this.clusterTabClosable = clusterTabClosable;
    }

    public void removeClusterAndDisconnect() {
        removeCluster();
        for (final Host host : hosts) {
            host.disconnect();
        }
        final Cluster thisCluster = this;
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                mainData.getClustersPanel().removeTabWithCluster(thisCluster);
            }
        });
    }
}
