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
package lcmc.gui;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import lcmc.model.AccessMode;
import lcmc.model.Application;
import lcmc.model.Cluster;
import lcmc.model.Host;
import lcmc.model.resources.BlockDevice;
import lcmc.model.resources.NetInterface;
import lcmc.common.ui.CmdLog;
import lcmc.gui.resources.CategoryInfo;
import lcmc.gui.resources.FSInfo;
import lcmc.gui.resources.Info;
import lcmc.gui.resources.NetInfo;
import lcmc.gui.resources.crm.HostInfo;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.gui.resources.drbd.HostDrbdInfo;
import lcmc.utilities.EnablePredicate;
import lcmc.utilities.MenuAction;
import lcmc.utilities.MenuFactory;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.ssh.ExecCommandConfig;

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

    @Inject @Named("netInfo")
    private Provider<NetInfo> netInfoProvider;
    @Inject
    private Provider<FSInfo> fsInfoProvider;

    public void init(final Host host) {
        this.host = host;
        hostInfo.init(host, this);
        hostDrbdInfo.init(host, this);
        setMenuTreeTop(hostInfo);
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                initHostResources();
            }
        });
    }

    public HostInfo getHostInfo() {
        return hostInfo;
    }

    public Host getHost() {
        return host;
    }

    public HostDrbdInfo getHostDrbdInfo() {
        return hostDrbdInfo;
    }

    public void initHostResources() {
        netInterfacesCategory.init(Tools.getString("HostBrowser.NetInterfaces"), this);

        netInterfacesNode = new DefaultMutableTreeNode(netInterfacesCategory);
        setNode(netInterfacesNode);
        topLevelAdd(netInterfacesNode);

        /* block devices */
        blockDevicesCategory.init(Tools.getString("HostBrowser.BlockDevices"), this);
        blockDevicesNode = new DefaultMutableTreeNode(blockDevicesCategory);
        setNode(blockDevicesNode);
        topLevelAdd(blockDevicesNode);

        /* file systems */
        fileSystemsCategory.init(Tools.getString("HostBrowser.FileSystems"), this);
        fileSystemsNode = new DefaultMutableTreeNode(fileSystemsCategory);
        setNode(fileSystemsNode);
        topLevelAdd(fileSystemsNode);
    }

    public ClusterBrowser getClusterBrowser() {
        final Cluster c = host.getCluster();
        if (c == null) {
            return null;
        }
        return c.getBrowser();
    }

    public void updateHWResources(
            final NetInterface[] netInterfaces,
            final BlockDevice[] blockDevices,
            final String[] fileSystems) {
        /* net interfaces */
        final Map<NetInterface, NetInfo> oldNetInterfaces = getNetInterfacesMap();
        final HostBrowser thisClass = this;
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                mNetInfosWriteLock.lock();
                try {
                    netInterfacesNode.removeAllChildren();
                    for (final NetInterface netInterface : netInterfaces) {
                        final NetInfo netInfo;
                        if (oldNetInterfaces.containsKey(netInterface)) {
                            netInfo = oldNetInterfaces.get(netInterface);
                        } else {
                            netInfo = netInfoProvider.get();
                            netInfo.init(netInterface.getName(), netInterface, thisClass);
                        }
                        final DefaultMutableTreeNode resource = new DefaultMutableTreeNode(netInfo);
                        setNode(resource);
                        netInterfacesNode.add(resource);
                    }
                    reloadAndWait(netInterfacesNode, false);
                } finally {
                    mNetInfosWriteLock.unlock();
                }
            }
        });

        /* block devices */
        mBlockDevInfosWriteLock.lock();
        boolean changed = false;
        try {
            final Map<BlockDevice, BlockDevInfo> oldBlockDevices =
                    new HashMap<BlockDevice, BlockDevInfo>(blockDevInfos);
            if (oldBlockDevices.size() != blockDevInfos.size()) {
                changed = true;
            }
            blockDevInfos.clear();
            for (final BlockDevice bd : blockDevices) {
                final BlockDevInfo blockDevInfo;
                if (oldBlockDevices.containsKey(bd)) {
                    blockDevInfo = oldBlockDevices.get(bd);
                    blockDevInfo.updateInfo();
                } else {
                    changed = true;
                    blockDevInfo = blockDevInfoFactory.get();
                    blockDevInfo.init(bd.getName(), bd, thisClass);
                }
                blockDevInfos.put(bd, blockDevInfo);
            }
        } finally {
            mBlockDevInfosWriteLock.unlock();
        }
        if (changed) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    mBlockDevInfosWriteLock.lock();
                    try {
                        blockDevicesNode.removeAllChildren();
                        for (final Map.Entry<BlockDevice, BlockDevInfo> bdEntry : blockDevInfos.entrySet()) {
                            final BlockDevInfo bdi = bdEntry.getValue();
                            final MutableTreeNode resource = new DefaultMutableTreeNode(bdi);
                            blockDevicesNode.add(resource);
                        }
                        reloadAndWait(blockDevicesNode, false);
                    } finally {
                        mBlockDevInfosWriteLock.unlock();
                    }
                }
            });
        }

        /* file systems */
        final Map<String, FSInfo> oldFilesystems = getFilesystemsMap();
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                mFileSystemsWriteLock.lock();
                try {
                    fileSystemsNode.removeAllChildren();
                    for (final String fs : fileSystems) {
                        final FSInfo fsi;
                        if (oldFilesystems.containsKey(fs)) {
                            fsi = oldFilesystems.get(fs);
                        } else {

                            fsi = fsInfoProvider.get();
                            fsi.init(fs, thisClass);
                        }
                        final DefaultMutableTreeNode resource = new DefaultMutableTreeNode(fsi);
                        setNode(resource);
                        fileSystemsNode.add(resource);
                    }
                    reloadAndWait(fileSystemsNode, false);
                } finally {
                    mFileSystemsWriteLock.unlock();
                }
            }
        });
    }

    public Set<BlockDevInfo> getSortedBlockDevInfos() {
        mBlockDevInfosReadLock.lock();
        try {
            return new LinkedHashSet<BlockDevInfo>(new TreeSet<BlockDevInfo>(blockDevInfos.values()));
        } finally {
            mBlockDevInfosReadLock.unlock();
        }
    }

    Map<NetInterface, NetInfo> getNetInterfacesMap() {
        final Map<NetInterface, NetInfo> netInterfaces = new HashMap<NetInterface, NetInfo>();
        mNetInfosReadLock.lock();
        try {
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> e = netInterfacesNode.children();
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode niNode = e.nextElement();
                final NetInfo nii = (NetInfo) niNode.getUserObject();
                netInterfaces.put(nii.getNetInterface(), nii);
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
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> e = fileSystemsNode.children();
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode fsiNode = e.nextElement();
                final FSInfo fsi = (FSInfo) fsiNode.getUserObject();
                filesystems.put(fsi.getName(), fsi);
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
                new AccessMode(Application.AccessType.ADMIN, false),
                new AccessMode(Application.AccessType.ADMIN, false))

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
                new AccessMode(Application.AccessType.GOD, false),
                new AccessMode(Application.AccessType.ADMIN, false))
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
                new AccessMode(Application.AccessType.GOD, false),
                new AccessMode(Application.AccessType.ADMIN, false))
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
