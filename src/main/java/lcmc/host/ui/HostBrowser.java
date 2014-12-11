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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import com.google.common.eventbus.Subscribe;
import lcmc.ClusterEventBus;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.resource.ClusterViewFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.cluster.domain.Cluster;
import lcmc.common.ui.Browser;
import lcmc.common.ui.GUIData;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.drbd.ui.DrbdGraph;
import lcmc.event.BlockDevicesChangedEvent;
import lcmc.event.CommonFileSystemsChangedEvent;
import lcmc.event.FileSystemsChangedEvent;
import lcmc.event.HwBlockDevicesChangedEvent;
import lcmc.event.HwNetInterfacesChangedEvent;
import lcmc.event.NetInterfacesChangedEvent;
import lcmc.host.domain.Host;
import lcmc.drbd.domain.BlockDevice;
import lcmc.drbd.domain.NetInterface;
import lcmc.common.ui.CmdLog;
import lcmc.common.ui.CategoryInfo;
import lcmc.cluster.ui.resource.FSInfo;
import lcmc.common.ui.Info;
import lcmc.cluster.ui.resource.NetInfo;
import lcmc.crm.ui.resource.HostInfo;
import lcmc.drbd.ui.resource.BlockDevInfo;
import lcmc.drbd.ui.resource.HostDrbdInfo;
import lcmc.common.domain.EnablePredicate;
import lcmc.common.ui.utils.MenuAction;
import lcmc.common.ui.utils.MenuFactory;
import lcmc.common.ui.utils.MyMenu;
import lcmc.common.ui.utils.MyMenuItem;
import lcmc.common.domain.util.Tools;
import lcmc.cluster.service.ssh.ExecCommandConfig;

/**
 * This class holds host resource data in a tree. It shows panels that allow
 * to edit data of resources, services etc., hosts and clusters.
 * Every resource has its Info object, that accessible through the tree view.
 */
@Named
public class HostBrowser extends Browser {
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
    public Host host;
    @Inject
    private HostInfo hostInfo;
    /**
     * Host info object of the host in drbd view of this browser.
     */
    @Inject
    private HostDrbdInfo hostDrbdInfo;
    /**
     * Map of block devices and their info objects.
     */
    private final Map<BlockDevice, BlockDevInfo> blockDevInfos = new LinkedHashMap<BlockDevice, BlockDevInfo>();
    private final ReadWriteLock mBlockDevInfosLock = new ReentrantReadWriteLock();
    private final Lock mBlockDevInfosReadLock = mBlockDevInfosLock.readLock();
    private final Lock mBlockDevInfosWriteLock = mBlockDevInfosLock.writeLock();
    private final ReadWriteLock mNetInfosLock = new ReentrantReadWriteLock();
    private final Lock mNetInfosReadLock = mNetInfosLock.readLock();
    private final Lock mNetInfosWriteLock = mNetInfosLock.writeLock();
    private final ReadWriteLock mFileSystemsLock = new ReentrantReadWriteLock();
    private final Lock mFileSystemsReadLock = mFileSystemsLock.readLock();
    private final Lock mFileSystemsWriteLock = mFileSystemsLock.writeLock();
    private DefaultMutableTreeNode treeTop;
    @Inject
    private GUIData guiData;
    @Inject
    private Provider<BlockDevInfo> blockDevInfoFactory;
    @Inject
    private Application application;
    @Inject
    private MenuFactory menuFactory;
    @Inject
    private Provider<CmdLog> cmdLogProvider;
    @Resource(name="categoryInfo")
    private CategoryInfo netInterfacesCategory;
    @Resource(name="categoryInfo")
    private CategoryInfo blockDevicesCategory;
    @Resource(name="categoryInfo")
    private CategoryInfo fileSystemsCategory;
    @Inject
    private TreeMenuController treeMenuController;
    @Inject
    private ClusterEventBus clusterEventBus;
    @Inject
    private ClusterViewFactory clusterViewFactory;

    public void init(final Host host) {
        this.host = host;
        hostInfo.init(host, this);
        hostDrbdInfo.init(host, this);
        treeTop = treeMenuController.createMenuTreeTop(hostInfo);
        application.invokeInEdt(new Runnable() {
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
        final Map<String, FSInfo> oldFileSystems = getFilesystemsMap();
        mFileSystemsWriteLock.lock();
        try {
            treeMenuController.removeChildren(fileSystemsNode);
            for (final String fileSystem : fileSystems) {
                final FSInfo fileSystemInfo;
                if (oldFileSystems.containsKey(fileSystem)) {
                    fileSystemInfo = oldFileSystems.get(fileSystem);
                } else {
                    fileSystemInfo = clusterViewFactory.createFileSystemView(fileSystem, this);
                }
                treeMenuController.createMenuItem(fileSystemsNode, fileSystemInfo);
            }
            treeMenuController.reloadNode(fileSystemsNode, false);
        } finally {
            mFileSystemsWriteLock.unlock();
        }
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
                treeMenuController.reloadNode(blockDevicesNode, false);
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
        final Map<NetInterface, NetInfo> oldNetInterfaces = getNetInterfacesMap();
        mNetInfosWriteLock.lock();
        try {
            treeMenuController.removeChildren(netInterfacesNode);
            for (final NetInterface netInterface : netInterfaces) {
                final NetInfo netInfo;
                if (oldNetInterfaces.containsKey(netInterface)) {
                    netInfo = oldNetInterfaces.get(netInterface);
                } else {
                    netInfo = clusterViewFactory.createNetView(netInterface, this);
                }
                final DefaultMutableTreeNode resource = treeMenuController.createMenuItem(netInterfacesNode, netInfo);
            }
            treeMenuController.reloadNode(netInterfacesNode, false);
        } finally {
            mNetInfosWriteLock.unlock();
        }
    }


    public Set<BlockDevInfo> getSortedBlockDevInfos() {
        mBlockDevInfosReadLock.lock();
        try {
            return new TreeSet<BlockDevInfo>(blockDevInfos.values());
        } finally {
            mBlockDevInfosReadLock.unlock();
        }
    }

    Map<NetInterface, NetInfo> getNetInterfacesMap() {
        final Map<NetInterface, NetInfo> netInterfaces = new HashMap<NetInterface, NetInfo>();
        mNetInfosReadLock.lock();
        try {
            for (final Object info : treeMenuController.nodesToInfos(netInterfacesNode.children())) {
                final NetInfo netInfo = (NetInfo) info;
                netInterfaces.put(netInfo.getNetInterface(), netInfo);
            }
        } finally {
            mNetInfosReadLock.unlock();
        }
        return netInterfaces;
    }

    Map<String, FSInfo> getFilesystemsMap() {
        final Map<String, FSInfo> filesystems = new HashMap<String, FSInfo>();
        mFileSystemsReadLock.lock();
        try {
            for (final Object info : treeMenuController.nodesToInfos(fileSystemsNode.children())) {
                final FSInfo fsInfo = (FSInfo) info;
                filesystems.put(fsInfo.getName(), fsInfo);
            }
        } finally {
            mFileSystemsReadLock.unlock();
        }
        return filesystems;
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
                        guiData.startProgressIndicator(hostName, host.getDistString(command));
                        host.execCommand(new ExecCommandConfig().commandString(command));
                        guiData.stopProgressIndicator(hostName, host.getDistString(command));
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
                        guiData.startProgressIndicator(hostName, host.getDistString(command));
                        host.execCommand(new ExecCommandConfig().commandString(command));
                        guiData.stopProgressIndicator(hostName, host.getDistString(command));
                    }
                });
        submenu.add(rebootMenuItem);
    }

    /**
     * Returns info string about Pacemaker installation.
     */
    public String getPacemakerInfo() {
        final StringBuilder pacemakerInfo = new StringBuilder(40);
        final String pmV = host.getPacemakerVersion();
        final String hbV = host.getHeartbeatVersion();
        final StringBuilder hbRunning = new StringBuilder(20);
        if (host.isHeartbeatRunning()) {
            hbRunning.append("running");
            if (!host.isHeartbeatInRc()) {
                hbRunning.append("/no rc.d");
            }
        } else {
            hbRunning.append("not running");
        }
        if (host.isHeartbeatInRc()) {
            hbRunning.append("/rc.d");
        }
        if (pmV == null) {
            if (hbV != null) {
                pacemakerInfo.append(" \nHeartbeat ");
                pacemakerInfo.append(hbV);
                pacemakerInfo.append(" (");
                pacemakerInfo.append(hbRunning);
                pacemakerInfo.append(')');
            }
        } else {
            final String pmRunning;
            if (host.isCrmStatusOk()) {
                pmRunning = "running";
            } else {
                pmRunning = "not running";
            }
            pacemakerInfo.append(" \nPacemaker ");
            pacemakerInfo.append(pmV);
            pacemakerInfo.append(" (");
            pacemakerInfo.append(pmRunning);
            pacemakerInfo.append(')');
            String corOrAis = null;
            final String corV = host.getCorosyncVersion();
            final String aisV = host.getOpenaisVersion();
            if (corV != null) {
                corOrAis = "Corosync " + corV;
            } else if (aisV != null) {
                corOrAis = "Openais " + aisV;
            }

            if (hbV != null && host.isHeartbeatRunning()) {
                pacemakerInfo.append(" \nHeartbeat ");
                pacemakerInfo.append(hbV);
                pacemakerInfo.append(" (");
                pacemakerInfo.append(hbRunning);
                pacemakerInfo.append(')');
            }
            if (corOrAis != null) {
                pacemakerInfo.append(" \n");
                pacemakerInfo.append(corOrAis);
                pacemakerInfo.append(" (");
                if (host.isCorosyncRunning()
                        || host.isOpenaisRunning()) {
                    pacemakerInfo.append("running");
                    if (!host.isCorosyncInRc() && !host.isOpenaisInRc()) {
                        pacemakerInfo.append("/no rc.d");
                    }
                } else {
                    pacemakerInfo.append("not running");
                }
                if (host.isCorosyncInRc() || host.isOpenaisInRc()) {
                    pacemakerInfo.append("/rc.d");
                }
                pacemakerInfo.append(')');
            }
            if (hbV != null && !host.isHeartbeatRunning()) {
                pacemakerInfo.append(" \nHeartbeat ");
                pacemakerInfo.append(hbV);
                pacemakerInfo.append(" (");
                pacemakerInfo.append(hbRunning);
                pacemakerInfo.append(')');
            }
        }
        return pacemakerInfo.toString();
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
        } else if (!host.isDrbdStatusOk() && !host.isCrmStatusOk()) {
            hostToolTip.append('\n');
            hostToolTip.append(Tools.getString("ClusterBrowser.Host.Offline"));
        }
        hostToolTip.append(host.getDrbdInfoAboutInstallation());
        hostToolTip.append(getPacemakerInfo());
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

    public TreeNode getNetInterfacesNode() {
        return netInterfacesNode;
    }
}
