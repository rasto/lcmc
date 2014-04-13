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

import lcmc.data.*;
import lcmc.utilities.Tools;
import lcmc.data.resources.NetInterface;
import lcmc.data.resources.BlockDevice;
import lcmc.gui.resources.FSInfo;
import lcmc.gui.resources.Info;
import lcmc.gui.resources.NetInfo;
import lcmc.gui.dialog.CmdLog;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.SSH;
import lcmc.gui.resources.CategoryInfo;
import lcmc.gui.resources.crm.HostInfo;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.gui.resources.drbd.HostDrbdInfo;
import lcmc.gui.resources.drbd.ProxyHostInfo;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.ImageIcon;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

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


/**
 * This class holds host resource data in a tree. It shows panels that allow
 * to edit data of resources, services etc., hosts and clusters.
 * Every resource has its Info object, that accessible through the tree view.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class HostBrowser extends Browser {
    /** Net interfaces node in the menu. */
    private DefaultMutableTreeNode netInterfacesNode;
    /** Block devices sytems node in the menu. */
    private DefaultMutableTreeNode blockDevicesNode;
    /** File sytems node in the menu. */
    private DefaultMutableTreeNode fileSystemsNode;

    /** List of used network interface ports. */
    private final Collection<String> usedPorts = new HashSet<String>();
    /** List of used proxy ports. */
    private final Collection<String> usedProxyPorts = new HashSet<String>();
    /** Host object. */
    private final Host host;
    /** Host info object of the host of this browser. */
    private final HostInfo hostInfo;
    /** Host info object of the host in drbd view of this browser. */
    private final HostDrbdInfo hostDrbdInfo;
    /** Proxy host info object. */
    private ProxyHostInfo proxyHostInfo = null;

    /** Host icon. */
    public static final ImageIcon HOST_ICON = Tools.createImageIcon(
                              Tools.getDefault("HostBrowser.HostIconSmall"));
    /** Host icon (turned on). */
    public static final ImageIcon HOST_ON_ICON = Tools.createImageIcon(
                              Tools.getDefault("HostBrowser.HostOnIconSmall"));
    /** Host icon (turned off). */
    public static final ImageIcon HOST_OFF_ICON = Tools.createImageIcon(
                              Tools.getDefault("HostBrowser.HostOffIconSmall"));
    /** Large host icon (turned on). */
    public static final ImageIcon HOST_ON_ICON_LARGE = Tools.createImageIcon(
                                  Tools.getDefault("HostBrowser.HostOnIcon"));
    /** Large host icon (turned on). */
    public static final ImageIcon HOST_OFF_ICON_LARGE = Tools.createImageIcon(
                                  Tools.getDefault("HostBrowser.HostOffIcon"));
    /** Large host icon. */
    public static final ImageIcon HOST_ICON_LARGE = Tools.createImageIcon(
                                  Tools.getDefault("HostBrowser.HostIcon"));
    /** Remove icon. */
    public static final ImageIcon HOST_REMOVE_ICON =
        Tools.createImageIcon(
                Tools.getDefault("HostBrowser.RemoveIcon"));
    /** Small host in cluster icon (right side). */
    public static final ImageIcon HOST_IN_CLUSTER_ICON_RIGHT_SMALL =
            Tools.createImageIcon(
               Tools.getDefault("HostBrowser.HostInClusterIconRightSmall"));
    /** Map of block devices and their info objects. */
    private final Map<BlockDevice, BlockDevInfo> blockDevInfos =
                                new LinkedHashMap<BlockDevice, BlockDevInfo>();
    /** Block device infos lock. */
    private final ReadWriteLock mBlockDevInfosLock =
                                                  new ReentrantReadWriteLock();
    /** Block device infos read lock. */
    private final Lock mBlockDevInfosReadLock = mBlockDevInfosLock.readLock();
    /** Block device infos write lock. */
    private final Lock mBlockDevInfosWriteLock = mBlockDevInfosLock.writeLock();
    /** Net Interface infos lock. */
    private final ReadWriteLock mNetInfosLock = new ReentrantReadWriteLock();
    /** Net Interface infos read lock. */
    private final Lock mNetInfosReadLock = mNetInfosLock.readLock();
    /** Net Interface infos write lock. */
    private final Lock mNetInfosWriteLock = mNetInfosLock.writeLock();
    /** File system list lock. */
    private final ReadWriteLock mFileSystemsLock = new ReentrantReadWriteLock();
    /** File system list read lock. */
    private final Lock mFileSystemsReadLock = mFileSystemsLock.readLock();
    /** File system list write lock. */
    private final Lock mFileSystemsWriteLock = mFileSystemsLock.writeLock();

    /**
     * Prepares a new {@code HostBrowser} object.
     *
     * @param host
     *          host to which this resource tree belongs
     */
    public HostBrowser(final Host host) {
        super();
        this.host = host;
        hostInfo = new HostInfo(host, this);
        hostDrbdInfo = new HostDrbdInfo(host, this);
        setTreeTop(hostInfo);
    }

    /** Returns host info for this browser. */
    public HostInfo getHostInfo() {
        return hostInfo;
    }

    /** Returns host data object for this browser. */
    public Host getHost() {
        return host;
    }

    /** Returns host for drbd view info for this browser. */
    public HostDrbdInfo getHostDrbdInfo() {
        return hostDrbdInfo;
    }

    /** Return proxy host info object. */
    public ProxyHostInfo getProxyHostInfo() {
        return proxyHostInfo;
    }

    /** Set proxy host info object. */
    public void setProxyHostInfo(final ProxyHostInfo proxyHostInfo) {
        this.proxyHostInfo = proxyHostInfo;
    }

    /** Initializes host resources for host view. */
    public void initHostResources() {
        /* net interfaces */
        netInterfacesNode = new DefaultMutableTreeNode(new CategoryInfo(
                                Tools.getString("HostBrowser.NetInterfaces"),
                                this));
        setNode(netInterfacesNode);
        topAdd(netInterfacesNode);

        /* block devices */
        blockDevicesNode = new DefaultMutableTreeNode(new CategoryInfo(
                                 Tools.getString("HostBrowser.BlockDevices"),
                                 this));
        setNode(blockDevicesNode);
        topAdd(blockDevicesNode);

        /* file systems */
        fileSystemsNode = new DefaultMutableTreeNode(new CategoryInfo(
                                  Tools.getString("HostBrowser.FileSystems"),
                                  this));
        setNode(fileSystemsNode);
        topAdd(fileSystemsNode);
    }

    /** Returns cluster browser if available. */
    public ClusterBrowser getClusterBrowser() {
        final Cluster c = host.getCluster();
        if (c == null) {
            return null;
        }
        return c.getBrowser();
    }

    /** Updates hardware resources of a host in the tree. */
    public void updateHWResources(final NetInterface[] nis,
                                  final BlockDevice[] bds,
                                  final String[] fss) {
        /* net interfaces */
        final Map<NetInterface, NetInfo> oldNetInterfaces =
                                                        getNetInterfacesMap();
        final HostBrowser thisClass = this;
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                mNetInfosWriteLock.lock();
                try {
                    netInterfacesNode.removeAllChildren();
                    for (final NetInterface ni : nis) {
                        final NetInfo nii;
                        if (oldNetInterfaces.containsKey(ni)) {
                            nii = oldNetInterfaces.get(ni);
                        } else {
                            nii = new NetInfo(ni.getName(), ni, thisClass);
                        }
                        final DefaultMutableTreeNode resource =
                                               new DefaultMutableTreeNode(nii);
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
            for (final BlockDevice bd : bds) {
                final BlockDevInfo bdi;
                if (oldBlockDevices.containsKey(bd)) {
                    bdi = oldBlockDevices.get(bd);
                    bdi.updateInfo();
                } else {
                    changed = true;
                    bdi = new BlockDevInfo(bd.getName(), bd, thisClass);
                }
                blockDevInfos.put(bd, bdi);
            }
        } finally {
            mBlockDevInfosWriteLock.unlock();
        }
        if (changed) {
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    mBlockDevInfosWriteLock.lock();
                    try {
                        blockDevicesNode.removeAllChildren();
                        for (final Map.Entry<BlockDevice, BlockDevInfo> bdEntry : blockDevInfos.entrySet()) {
                            final BlockDevInfo bdi = bdEntry.getValue();
                            final MutableTreeNode resource =
                                               new DefaultMutableTreeNode(bdi);
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
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                mFileSystemsWriteLock.lock();
                try {
                            fileSystemsNode.removeAllChildren();
                    for (final String fs : fss) {
                        final FSInfo fsi;
                        if (oldFilesystems.containsKey(fs)) {
                            fsi = oldFilesystems.get(fs);
                        } else {
                            fsi = new FSInfo(fs, thisClass);
                        }
                        final DefaultMutableTreeNode resource =
                                               new DefaultMutableTreeNode(fsi);
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

    /**
     * Return sorted list of block device info objects.
     */
    public Set<BlockDevInfo> getBlockDevInfos() {
        mBlockDevInfosReadLock.lock();
        try {
            return new LinkedHashSet<BlockDevInfo>(new TreeSet<BlockDevInfo>(blockDevInfos.values()));
        } finally {
            mBlockDevInfosReadLock.unlock();
        }
    }

    /** Returns map of net interface objects with its net info objects. */
    Map<NetInterface, NetInfo> getNetInterfacesMap() {
        final Map<NetInterface, NetInfo> netInterfaces =
                                          new HashMap<NetInterface, NetInfo>();
        mNetInfosReadLock.lock();
        try {
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> e =
                                                  netInterfacesNode.children();
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

    /** Returns map of file systems its file system info objects. */
    Map<String, FSInfo> getFilesystemsMap() {
        final Map<String, FSInfo> filesystems = new HashMap<String, FSInfo>();
        mFileSystemsReadLock.lock();
        try {
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> e =
                                                    fileSystemsNode.children();
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

    /** Adds advanced submenu to the host menus in drbd and pacemaker view. */
    public void addAdvancedMenu(final MyMenu submenu) {
        if (submenu.getItemCount() > 0) {
            return;
        }
        /* Command log */
        final MyMenuItem cmdLogMenuItem = new MyMenuItem(
                        Tools.getString("HostBrowser.CmdLog"),
                        Info.LOGFILE_ICON,
                        "",
                        new AccessMode(Application.AccessType.ADMIN, false),
                        new AccessMode(Application.AccessType.ADMIN, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (!host.isConnected()) {
                    return Host.NOT_CONNECTED_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                final CmdLog l = new CmdLog(host);
                l.showDialog();
            }
        };
        submenu.add(cmdLogMenuItem);

        /* panic */
        final MyMenuItem panicMenuItem = new MyMenuItem(
                    Tools.getString("HostBrowser.MakeKernelPanic")
                    + host.getName(),
                    null,
                    new AccessMode(Application.AccessType.GOD, false),
                    new AccessMode(Application.AccessType.ADMIN, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (!host.isConnected()) {
                    return Host.NOT_CONNECTED_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                // TODO are you sure dialog.
                final String hostName = host.getName();
                final String command = "MakeKernelPanic";
                Tools.startProgressIndicator(hostName,
                                             host.getDistString(command));
                host.execCommand(command,
                                 null,
                                 null,
                                 true,
                                 SSH.DEFAULT_COMMAND_TIMEOUT);
                Tools.stopProgressIndicator(hostName,
                                            host.getDistString(command));
            }
        };
        submenu.add(panicMenuItem);

        /* reboot */
        final MyMenuItem rebootMenuItem = new MyMenuItem(
                    Tools.getString("HostBrowser.MakeKernelReboot")
                    + host.getName(),
                    null,
                    new AccessMode(Application.AccessType.GOD, false),
                    new AccessMode(Application.AccessType.ADMIN, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                if (!host.isConnected()) {
                    return Host.NOT_CONNECTED_STRING;
                }
                return null;
            }

            @Override
            public void action() {
                // TODO are you sure dialog.
                final String hostName = host.getName();
                final String command = "MakeKernelReboot";
                Tools.startProgressIndicator(hostName,
                                             host.getDistString(command));
                host.execCommand(command,
                                 null,
                                 null,
                                 true,
                                 SSH.DEFAULT_COMMAND_TIMEOUT);
                Tools.stopProgressIndicator(hostName,
                                            host.getDistString(command));
            }
        };
        submenu.add(rebootMenuItem);
    }

    /** Returns info string about DRBD installation. */
    public String getDrbdInfo() {
        final StringBuilder tt = new StringBuilder(40);
        final String drbdV = host.getDrbdVersion();
        final String drbdModuleV = host.getDrbdModuleVersion();
        final String drbdS;
        if (drbdV == null || drbdV.isEmpty()) {
            drbdS = "not installed";
        } else {
            drbdS = drbdV;
        }

        final String drbdModuleS;
        if (drbdModuleV == null || drbdModuleV.isEmpty()) {
            drbdModuleS = "not installed";
        } else {
            drbdModuleS = drbdModuleV;
        }
        tt.append("\nDRBD ");
        tt.append(drbdS);
        if (!drbdS.equals(drbdModuleS)) {
            tt.append("\nDRBD module ");
            tt.append(drbdModuleS);
        }
        if (host.isDrbdLoaded()) {
            tt.append(" (running)");
        } else {
            tt.append(" (not loaded)");
        }
        return tt.toString();
    }

    /** Returns info string about Pacemaker installation. */
    public String getPacemakerInfo() {
        final StringBuilder tt = new StringBuilder(40);
        final String pmV = host.getPacemakerVersion();
        final String hbV = host.getHeartbeatVersion();
        final StringBuilder hbRunning = new StringBuilder(20);
        if (host.isHeartbeatRunning()) {
            hbRunning.append("running");
            if (!host.isHeartbeatRc()) {
                hbRunning.append("/no rc.d");
            }
        } else {
            hbRunning.append("not running");
        }
        if (host.isHeartbeatRc()) {
            hbRunning.append("/rc.d");
        }
        if (pmV == null) {
            if (hbV != null) {
                tt.append(" \nHeartbeat ");
                tt.append(hbV);
                tt.append(" (");
                tt.append(hbRunning);
                tt.append(')');
            }
        } else {
            final String pmRunning;
            if (host.isClStatus()) {
                pmRunning = "running";
            } else {
                pmRunning = "not running";
            }
            tt.append(" \nPacemaker ");
            tt.append(pmV);
            tt.append(" (");
            tt.append(pmRunning);
            tt.append(')');
            String corOrAis = null;
            final String corV = host.getCorosyncVersion();
            final String aisV = host.getOpenaisVersion();
            if (corV != null) {
                corOrAis = "Corosync " + corV;
            } else if (aisV != null) {
                corOrAis = "Openais " + aisV;
            }

            if (hbV != null && host.isHeartbeatRunning()) {
                tt.append(" \nHeartbeat ");
                tt.append(hbV);
                tt.append(" (");
                tt.append(hbRunning);
                tt.append(')');
            }
            if (corOrAis != null) {
                tt.append(" \n");
                tt.append(corOrAis);
                tt.append(" (");
                if (host.isCsRunning()
                    || host.isAisRunning()) {
                    tt.append("running");
                    if (!host.isCsRc() && !host.isAisRc()) {
                        tt.append("/no rc.d");
                    }
                } else {
                    tt.append("not running");
                }
                if (host.isCsRc() || host.isAisRc()) {
                    tt.append("/rc.d");
                }
                tt.append(')');
            }
            if (hbV != null && !host.isHeartbeatRunning()) {
                tt.append(" \nHeartbeat ");
                tt.append(hbV);
                tt.append(" (");
                tt.append(hbRunning);
                tt.append(')');
            }
        }
        return tt.toString();
    }

    /** Returns tooltip for host. */
    public String getHostToolTip(final Host host) {
        final StringBuilder tt = new StringBuilder(80);
        tt.append("<b>").append(host.getName()).append("</b>");
        final ClusterBrowser b = getClusterBrowser();
        if (b != null && b.isRealDcHost(host)) {
            tt.append(" (designated co-ordinator)");
        }
        if (!host.isConnected()) {
            tt.append('\n');
            tt.append(Tools.getString("ClusterBrowser.Host.Disconnected"));
        } else if (!host.isDrbdStatus() && !host.isClStatus()) {
            tt.append('\n');
            tt.append(Tools.getString("ClusterBrowser.Host.Offline"));
        }
        /* DRBD */
        tt.append(getDrbdInfo());
        /* Pacemaker */
        tt.append(getPacemakerInfo());
        return tt.toString();
    }

    /** Returns drbd graph object. */
    public DrbdGraph getDrbdGraph() {
        final ClusterBrowser b = getClusterBrowser();
        if (b == null) {
            return null;
        }
        return b.getDrbdGraph();
    }

    /** Returns a list of used network interface ports. */
    public Collection<String> getUsedPorts() {
        return usedPorts;
    }

    /** Returns a list of used proxy ports. */
    public Collection<String> getUsedProxyPorts() {
        return usedProxyPorts;
    }

    /** Lock block dev info objects. */
    public void lockBlockDevInfosRead() {
        mBlockDevInfosReadLock.lock();
    }

    /** Unlock block dev info objects. */
    public void unlockBlockDevInfosRead() {
        mBlockDevInfosReadLock.unlock();
    }

    /** Returns net interfaces node from the menu. */
    public TreeNode getNetInterfacesNode() {
        return netInterfacesNode;
    }
}
