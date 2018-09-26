/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
 *
 * RBD Management Console is free software; you can redistribute it and/or
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
package lcmc.host.ui;

import com.google.common.eventbus.Subscribe;
import lcmc.ClusterEventBus;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.resource.ClusterViewFactory;
import lcmc.cluster.ui.resource.FSInfo;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.EnablePredicate;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.common.ui.CategoryInfo;
import lcmc.common.ui.CmdLog;
import lcmc.common.ui.Info;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.common.ui.utils.*;
import lcmc.crm.ui.resource.HostInfo;
import lcmc.drbd.domain.BlockDevice;
import lcmc.drbd.domain.NetInterface;
import lcmc.drbd.ui.DrbdGraph;
import lcmc.drbd.ui.resource.BlockDevInfo;
import lcmc.drbd.ui.resource.HostDrbdInfo;
import lcmc.event.BlockDevicesChangedEvent;
import lcmc.event.FileSystemsChangedEvent;
import lcmc.event.NetInterfacesChangedEvent;
import lcmc.host.domain.Host;

import javax.inject.Provider;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class holds host resource data in a tree. It shows panels that allow
 * to edit data of resources, services etc., hosts and clusters.
 * Every resource has its Info object, that accessible through the tree view.
 */
public class HostBrowser extends Browser {

    private final ProgressIndicator progressIndicator;
    private final Provider<BlockDevInfo> blockDevInfoFactory;
    private final SwingUtils swingUtils;
    private final MenuFactory menuFactory;
    private final Provider<CmdLog> cmdLogProvider;
    private final CategoryInfo netInterfacesCategory;
    private final CategoryInfo blockDevicesCategory;
    private final CategoryInfo fileSystemsCategory;
    private final TreeMenuController treeMenuController;
    private final ClusterEventBus clusterEventBus;
    private final ClusterViewFactory clusterViewFactory;
    private final HostInfo hostInfo;
    private final HostDrbdInfo hostDrbdInfo;

    public static final ImageIcon HOST_ICON = Tools.createImageIcon(Tools.getDefault("HostBrowser.HostIconSmall"));
    public static final ImageIcon HOST_ON_ICON = Tools.createImageIcon(
            Tools.getDefault("HostBrowser.HostOnIconSmall"));
    public static final ImageIcon HOST_OFF_ICON = Tools.createImageIcon(
            Tools.getDefault("HostBrowser.HostOffIconSmall"));
    public static final ImageIcon HOST_ON_ICON_LARGE = Tools.createImageIcon(
            Tools.getDefault("HostBrowser.HostOnIcon"));
    public static final ImageIcon HOST_OFF_ICON_LARGE = Tools.createImageIcon(
            Tools.getDefault("HostBrowser.HostOffIcon"));
    public static final ImageIcon HOST_ICON_LARGE = Tools.createImageIcon(Tools.getDefault("HostBrowser.HostIcon"));
    public static final ImageIcon HOST_REMOVE_ICON = Tools.createImageIcon(Tools.getDefault("HostBrowser.RemoveIcon"));
    /**
     * Small host in cluster icon (right side).
     */
    public static final ImageIcon HOST_IN_CLUSTER_ICON_RIGHT_SMALL =
            Tools.createImageIcon(Tools.getDefault("HostBrowser.HostInClusterIconRightSmall"));
    private DefaultMutableTreeNode netInterfacesNode;
    private DefaultMutableTreeNode blockDevicesNode;
    private DefaultMutableTreeNode fileSystemsNode;

    /**
     * List of used network interface ports.
     */
    private final Collection<String> usedPorts = new HashSet<String>();
    private final Collection<String> usedProxyPorts = new HashSet<String>();
    private Host host;
    private final Map<BlockDevice, BlockDevInfo> blockDevInfos = new LinkedHashMap<BlockDevice, BlockDevInfo>();
    private final ReadWriteLock mBlockDevInfosLock = new ReentrantReadWriteLock();
    private final Lock mBlockDevInfosReadLock = mBlockDevInfosLock.readLock();
    private final Lock mBlockDevInfosWriteLock = mBlockDevInfosLock.writeLock();
    private DefaultMutableTreeNode treeTop;

    public HostBrowser(Application application, HostInfo hostInfo, HostDrbdInfo hostDrbdInfo, ProgressIndicator progressIndicator, Provider<BlockDevInfo> blockDevInfoFactory, SwingUtils swingUtils, MenuFactory menuFactory, Provider<CmdLog> cmdLogProvider, CategoryInfo netInterfacesCategory, CategoryInfo blockDevicesCategory, CategoryInfo fileSystemsCategory, TreeMenuController treeMenuController, ClusterEventBus clusterEventBus, ClusterViewFactory clusterViewFactory) {
        super(application);
        this.hostInfo = hostInfo;
        this.hostDrbdInfo = hostDrbdInfo;
        this.progressIndicator = progressIndicator;
        this.blockDevInfoFactory = blockDevInfoFactory;
        this.swingUtils = swingUtils;
        this.menuFactory = menuFactory;
        this.cmdLogProvider = cmdLogProvider;
        this.netInterfacesCategory = netInterfacesCategory;
        this.blockDevicesCategory = blockDevicesCategory;
        this.fileSystemsCategory = fileSystemsCategory;
        this.treeMenuController = treeMenuController;
        this.clusterEventBus = clusterEventBus;
        this.clusterViewFactory = clusterViewFactory;
    }

    public void init(final Host host) {
        this.host = host;
        hostInfo.init(host, this);
        hostDrbdInfo.init(host, this);
        treeTop = treeMenuController.createMenuTreeTop(hostInfo);
        swingUtils.invokeInEdt(new Runnable() {
            @Override
            public void run() {
                initHostResources();
            }
        });
        clusterEventBus.register(this);
    }

    public HostInfo getHostInfo() {
        return hostInfo;
    }

    public Host getHost() {
        return host;
    }

    public DefaultMutableTreeNode getTreeTop() {
        return treeTop;
    }

    public HostDrbdInfo getHostDrbdInfo() {
        return hostDrbdInfo;
    }

    public void initHostResources() {
        netInterfacesCategory.init(Tools.getString("HostBrowser.NetInterfaces"), this);
        netInterfacesNode = treeMenuController.createMenuItem(treeTop, netInterfacesCategory);

        /* block devices */
        blockDevicesCategory.init(Tools.getString("HostBrowser.BlockDevices"), this);
        blockDevicesNode = treeMenuController.createMenuItem(treeTop, blockDevicesCategory);

        /* file systems */
        fileSystemsCategory.init(Tools.getString("HostBrowser.FileSystems"), this);
        fileSystemsNode = treeMenuController.createMenuItem(treeTop, fileSystemsCategory);
    }

    public ClusterBrowser getClusterBrowser() {
        final Cluster c = host.getCluster();
        if (c == null) {
            return null;
        }
        return c.getBrowser();
    }

    @Subscribe
    public void updateFileSystemsNode(final FileSystemsChangedEvent event) {
        if (event.getHost() != host) {
            return;
        }
        final Set<String> fileSystems = event.getFileSystems();
        treeMenuController.removeChildren(fileSystemsNode);
        for (final String fileSystem : fileSystems) {
            final FSInfo fileSystemInfo = clusterViewFactory.createFileSystemView(fileSystem, this);
            treeMenuController.createMenuItem(fileSystemsNode, fileSystemInfo);
        }
        treeMenuController.reloadNodeDontSelect(fileSystemsNode);
    }

    @Subscribe
    public void updateBlockDevicesNodes(final BlockDevicesChangedEvent event) {
        if (event.getHost() != host) {
            return;
        }
        final Collection<BlockDevice> blockDevices = event.getBlockDevices();
        mBlockDevInfosWriteLock.lock();
        boolean changed = false;
        try {
            final Map<BlockDevice, BlockDevInfo> oldBlockDevices = new HashMap<BlockDevice, BlockDevInfo>(blockDevInfos);
            if (oldBlockDevices.size() != blockDevices.size()) {
                changed = true;
            }
            blockDevInfos.clear();
            for (final BlockDevice blockDevice : blockDevices) {
                final BlockDevInfo blockDevInfo;
                if (oldBlockDevices.containsKey(blockDevice)) {
                    blockDevInfo = oldBlockDevices.get(blockDevice);
                    ((BlockDevice) blockDevInfo.getResource()).updateFrom(blockDevice);
                    blockDevInfo.updateInfo();
                } else {
                    changed = true;
                    blockDevInfo = blockDevInfoFactory.get();
                    blockDevInfo.init(blockDevice.getName(), blockDevice, this);
                }
                blockDevInfos.put(blockDevice, blockDevInfo);
            }
        } finally {
            mBlockDevInfosWriteLock.unlock();
        }
        if (changed) {
            mBlockDevInfosWriteLock.lock();
            try {
                treeMenuController.removeChildren(blockDevicesNode);
                for (final Map.Entry<BlockDevice, BlockDevInfo> bdEntry : blockDevInfos.entrySet()) {
                    final BlockDevInfo bdi = bdEntry.getValue();
                    treeMenuController.createMenuItem(blockDevicesNode, bdi);
                }
                treeMenuController.reloadNodeDontSelect(blockDevicesNode);
            } finally {
                mBlockDevInfosWriteLock.unlock();
            }
        }
    }

    @Subscribe
    public void onNetInterfacesChanged(final NetInterfacesChangedEvent event) {
        if (event.getHost() != host) {
            return;
        }
        final Collection<NetInterface> netInterfaces = event.getNetInterfaces();
        treeMenuController.removeChildren(netInterfacesNode);
        for (final NetInterface netInterface : netInterfaces) {
            final Info netInfo = clusterViewFactory.getNetView(netInterface, this);
            treeMenuController.createMenuItem(netInterfacesNode, netInfo);
        }
        treeMenuController.reloadNodeDontSelect(netInterfacesNode);
    }


    public Set<BlockDevInfo> getSortedBlockDevInfos() {
        mBlockDevInfosReadLock.lock();
        try {
            return new TreeSet<BlockDevInfo>(blockDevInfos.values());
        } finally {
            mBlockDevInfosReadLock.unlock();
        }
    }

    /**
     * Adds advanced submenu to the host menus in drbd and pacemaker view.
     */
    public void addAdvancedMenu(final MyMenu submenu) {
        if (submenu.getItemCount() > 0) {
            return;
        }
        /* Command log */
        final MyMenuItem cmdLogMenuItem = menuFactory.createMenuItem(
                Tools.getString("HostBrowser.CmdLog"),
                Info.LOGFILE_ICON,
                "",
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL))

                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (!host.isConnected()) {
                            return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                        }
                        return null;
                    }
                })

                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        final CmdLog cmdLog = cmdLogProvider.get();
                        cmdLog.init(host);
                        cmdLog.showDialog();
                    }
                });
        submenu.add(cmdLogMenuItem);

        /* panic */
        final MyMenuItem panicMenuItem = menuFactory.createMenuItem(
                Tools.getString("HostBrowser.MakeKernelPanic") + host.getName(),
                null,
                new AccessMode(AccessMode.GOD, AccessMode.NORMAL),
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (!host.isConnected()) {
                            return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                        }
                        return null;
                    }
                })
                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        final String hostName = host.getName();
                        final String command = "MakeKernelPanic";
                        progressIndicator.startProgressIndicator(hostName, host.getDistString(command));
                        host.execCommand(new ExecCommandConfig().commandString(command));
                        progressIndicator.stopProgressIndicator(hostName, host.getDistString(command));
                    }
                });
        submenu.add(panicMenuItem);

        /* reboot */
        final MyMenuItem rebootMenuItem = menuFactory.createMenuItem(
                Tools.getString("HostBrowser.MakeKernelReboot") + host.getName(),
                null,
                new AccessMode(AccessMode.GOD, AccessMode.NORMAL),
                new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL))
                .enablePredicate(new EnablePredicate() {
                    @Override
                    public String check() {
                        if (!host.isConnected()) {
                            return Host.NOT_CONNECTED_MENU_TOOLTIP_TEXT;
                        }
                        return null;
                    }
                })

                .addAction(new MenuAction() {
                    @Override
                    public void run(final String text) {
                        final String hostName = host.getName();
                        final String command = "MakeKernelReboot";
                        progressIndicator.startProgressIndicator(hostName, host.getDistString(command));
                        host.execCommand(new ExecCommandConfig().commandString(command));
                        progressIndicator.stopProgressIndicator(hostName, host.getDistString(command));
                    }
                });
        submenu.add(rebootMenuItem);
    }


    public String getHostToolTip(final Host host) {
        final StringBuilder hostToolTip = new StringBuilder(80);
        hostToolTip.append("<b>").append(host.getName()).append("</b>");
        final ClusterBrowser b = getClusterBrowser();
        if (b != null && b.isRealDcHost(host)) {
            hostToolTip.append(" (designated co-ordinator)");
        }
        if (!host.isConnected()) {
            hostToolTip.append('\n');
            hostToolTip.append(Tools.getString("ClusterBrowser.Host.Disconnected"));
        } else if (!host.getHostParser().isDrbdStatusOk() && !host.isCrmStatusOk()) {
            hostToolTip.append('\n');
            hostToolTip.append(Tools.getString("ClusterBrowser.Host.Offline"));
        }
        hostToolTip.append(host.getDrbdInfoAboutInstallation());
        hostToolTip.append(host.getPacemakerInfo());
        return hostToolTip.toString();
    }

    public DrbdGraph getDrbdGraph() {
        final ClusterBrowser b = getClusterBrowser();
        if (b == null) {
            return null;
        }
        return b.getDrbdGraph();
    }

    /**
     * Returns a list of used network interface ports.
     */
    public Collection<String> getUsedPorts() {
        return usedPorts;
    }

    public Collection<String> getUsedProxyPorts() {
        return usedProxyPorts;
    }

    public void lockBlockDevInfosRead() {
        mBlockDevInfosReadLock.lock();
    }

    public void unlockBlockDevInfosRead() {
        mBlockDevInfosReadLock.unlock();
    }

    @Override
    public void fireEventInViewPanel(final DefaultMutableTreeNode node) {
        if (node != null) {
            treeMenuController.reloadNode(node);
            treeMenuController.nodeChanged(node);
        }
    }
}
