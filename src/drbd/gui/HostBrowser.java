/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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


package drbd.gui;

import drbd.utilities.Tools;
import drbd.data.resources.NetInterface;
import drbd.data.resources.BlockDevice;
import drbd.data.Host;
import drbd.data.Cluster;
import drbd.data.ConfigData;
import drbd.gui.resources.BlockDevInfo;
import drbd.gui.resources.FSInfo;
import drbd.gui.resources.HostDrbdInfo;
import drbd.gui.resources.HostInfo;
import drbd.gui.resources.NetInfo;

import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;
import drbd.gui.resources.CategoryInfo;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.ImageIcon;

import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import EDU.oswego.cs.dl.util.concurrent.Mutex;


/**
 * This class holds host resource data in a tree. It shows panels that allow
 * to edit data of resources, services etc., hosts and clusters.
 * Every resource has its Info object, that accessible through the tree view.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class HostBrowser extends Browser {
    /** Net interfaces node in the menu. */
    private DefaultMutableTreeNode netInterfacesNode;
    /** Block devices sytems node in the menu. */
    private DefaultMutableTreeNode blockDevicesNode;
    /** File sytems node in the menu. */
    private DefaultMutableTreeNode fileSystemsNode;

    /** List of used network interface ports. */
    private final List<String> drbdVIPortList = new ArrayList<String>();
    /** Host object. */
    private final Host host;
    /** Host info object of the host of this browser. */
    private final HostInfo hostInfo;
    /** Host info object of the host in drbd view of this browser. */
    private final HostDrbdInfo hostDrbdInfo;

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
    /** Small host in cluster icon (left side). */
    public static final ImageIcon HOST_IN_CLUSTER_ICON_LEFT_SMALL =
            Tools.createImageIcon(
               Tools.getDefault("HostBrowser.HostInClusterIconLeftSmall"));
    /** Block device infos lock. */
    private final Mutex mBlockDevInfosLock = new Mutex();
    /** Net Interface infos lock. */
    private final Mutex mNetInfosLock = new Mutex();
    /** File system list lock. */
    private final Mutex mFileSystemsLock = new Mutex();

    /**
     * Prepares a new <code>HostBrowser</code> object.
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

    /**
     * Returns host info for this browser.
     */
    public final HostInfo getHostInfo() {
        return hostInfo;
    }

    /**
     * Returns host data object for this browser.
     */
    public final Host getHost() {
        return host;
    }

    /**
     * Returns host for drbd view info for this browser.
     */
    public final HostDrbdInfo getHostDrbdInfo() {
        return hostDrbdInfo;
    }

    /**
     * Initializes host resources for host view.
     */
    public final void initHostResources() {
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

    /**
     * Returns cluster browser if available.
     */
    public final ClusterBrowser getClusterBrowser() {
        final Cluster c = host.getCluster();
        if (c == null) {
            return null;
        }
        return c.getBrowser();
    }

    /**
     * Updates hardware resources of a host in the tree.
     */
    public final void updateHWResources(final NetInterface[] nis,
                                        final BlockDevice[] bds,
                                        final String[] fss) {
        DefaultMutableTreeNode resource = null;
        /* net interfaces */
        final Map<NetInterface, NetInfo> oldNetInterfaces =
                                                        getNetInterfacesMap();
        try {
            mNetInfosLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        netInterfacesNode.removeAllChildren();
        for (final NetInterface ni : nis) {
            NetInfo nii;
            if (oldNetInterfaces.containsKey(ni)) {
                nii = oldNetInterfaces.get(ni);
            } else {
                nii = new NetInfo(ni.getName(), ni, this);
            }
            resource = new DefaultMutableTreeNode(nii);
            setNode(resource);
            netInterfacesNode.add(resource);
        }
        reload(netInterfacesNode);
        mNetInfosLock.release();

        /* block devices */
        final Map<BlockDevice, BlockDevInfo> oldBlockDevices =
                                                          getBlockDevicesMap();
        try {
            mBlockDevInfosLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        blockDevicesNode.removeAllChildren();
        for (final BlockDevice bd : bds) {
            BlockDevInfo bdi;
            if (oldBlockDevices.containsKey(bd)) {
                bdi = oldBlockDevices.get(bd);
                bdi.updateInfo();
            } else {
                bdi = new BlockDevInfo(bd.getName(), bd, this);
            }
            resource = new DefaultMutableTreeNode(bdi);
            //setNode(resource);
            blockDevicesNode.add(resource);
        }
        reload(blockDevicesNode);
        mBlockDevInfosLock.release();

        /* file systems */
        final Map<String, FSInfo> oldFilesystems = getFilesystemsMap();
        try {
            mFileSystemsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        fileSystemsNode.removeAllChildren();
        for (final String fs : fss) {
            FSInfo fsi;
            if (oldFilesystems.containsKey(fs)) {
                fsi = oldFilesystems.get(fs);
            } else {
                fsi = new FSInfo(fs, this);
            }
            resource = new DefaultMutableTreeNode(fsi);
            setNode(resource);
            fileSystemsNode.add(resource);
        }
        reload(fileSystemsNode);
        mFileSystemsLock.release();
    }

    /**
     * Return list of block device info objects.
     */
    public final List<BlockDevInfo> getBlockDevInfos() {
        final List<BlockDevInfo> blockDevInfos = new ArrayList<BlockDevInfo>();
        try {
            mBlockDevInfosLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final Enumeration e = blockDevicesNode.children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode bdNode =
                                      (DefaultMutableTreeNode) e.nextElement();
            final BlockDevInfo bdi = (BlockDevInfo) bdNode.getUserObject();
            blockDevInfos.add(bdi);
        }
        mBlockDevInfosLock.release();
        return blockDevInfos;
    }

    /**
     * Returns map of block device objects with its block device info objects.
     */
    public final Map<BlockDevice, BlockDevInfo> getBlockDevicesMap() {
        final Map<BlockDevice, BlockDevInfo> blockDevices =
                                      new HashMap<BlockDevice, BlockDevInfo>();
        try {
            mBlockDevInfosLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final Enumeration e = blockDevicesNode.children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode bdNode =
                                      (DefaultMutableTreeNode) e.nextElement();
            final BlockDevInfo bdi = (BlockDevInfo) bdNode.getUserObject();
            blockDevices.put(bdi.getBlockDevice(), bdi);
        }
        mBlockDevInfosLock.release();
        return blockDevices;
    }

    /**
     * Returns map of net interface objects with its net info objects.
     */
    public final Map<NetInterface, NetInfo> getNetInterfacesMap() {
        final Map<NetInterface, NetInfo> netInterfaces =
                                          new HashMap<NetInterface, NetInfo>();
        try {
            mNetInfosLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final Enumeration e = netInterfacesNode.children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode niNode =
                                      (DefaultMutableTreeNode) e.nextElement();
            final NetInfo nii = (NetInfo) niNode.getUserObject();
            netInterfaces.put(nii.getNetInterface(), nii);
        }
        mNetInfosLock.release();
        return netInterfaces;
    }

    /**
     * Returns map of file systems its file system info objects.
     */
    public final Map<String, FSInfo> getFilesystemsMap() {
        final Map<String, FSInfo> filesystems = new HashMap<String, FSInfo>();
        try {
            mFileSystemsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final Enumeration e = fileSystemsNode.children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode fsiNode =
                                      (DefaultMutableTreeNode) e.nextElement();
            final FSInfo fsi = (FSInfo) fsiNode.getUserObject();
            filesystems.put(fsi.getName(), fsi);
        }
        mFileSystemsLock.release();
        return filesystems;
    }

    /**
     * @return list of network interfaces.
     */
    public final List<NetInfo> getNetInfos() {
        final Enumeration e = netInterfacesNode.children();
        final List<NetInfo> netInfos = new ArrayList<NetInfo>();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode niNode =
                                (DefaultMutableTreeNode) e.nextElement();
            final NetInfo ni = (NetInfo) niNode.getUserObject();
            netInfos.add(ni);
        }
        return netInfos;
    }

    /**
     * Adds expert submenu to the host menus in drbd and pacemaker view.
     */
    public final void addExpertMenu(final MyMenu submenu) {
        if (submenu.getItemCount() > 0) {
            return;
        }
        /* panic */
        final MyMenuItem panicMenuItem = new MyMenuItem(
                    Tools.getString("HostBrowser.MakeKernelPanic")
                    + host.getName(),
                    null,
                    ConfigData.AccessType.GOD,
                    ConfigData.AccessType.ADMIN) {
            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                return host.isConnected();
            }

            public void action() {
                // TODO are you sure dialog.
                final String hostName = host.getName();
                final String command = "MakeKernelPanic";
                Tools.startProgressIndicator(hostName,
                                             host.getDistString(command));
                host.execCommand(command, null, null, true);
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
                    ConfigData.AccessType.GOD,
                    ConfigData.AccessType.ADMIN) {
            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                return host.isConnected();
            }

            public void action() {
                // TODO are you sure dialog.
                final String hostName = host.getName();
                final String command = "MakeKernelReboot";
                Tools.startProgressIndicator(hostName,
                                             host.getDistString(command));
                host.execCommand(command, null, null, true);
                Tools.stopProgressIndicator(hostName,
                                            host.getDistString(command));
            }
        };
        submenu.add(rebootMenuItem);
    }

    /**
     * Returns info string about DRBD installation.
     */
    public final String getDrbdInfo() {
        final StringBuffer tt = new StringBuffer(40);
        final String drbdV = host.getDrbdVersion();
        final String drbdModuleV = host.getDrbdModuleVersion();
        String drbdS = null;
        if (drbdV == null || "".equals(drbdV)) {
            drbdS = "not installed";
        } else {
            drbdS = drbdV;
        }

        String drbdModuleS = null;
        if (drbdModuleV == null || "".equals(drbdModuleV)) {
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

    /**
     * Returns info string about Pacemaker installation.
     */
    public final String getPacemakerInfo() {
        final StringBuffer tt = new StringBuffer(40);
        final String pmV = host.getPacemakerVersion();
        final String hbV = host.getHeartbeatVersion();
        final StringBuffer hbRunning = new StringBuffer(20);
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
                tt.append(hbRunning.toString());
                tt.append(')');
            }
        } else {
            String pmRunning;
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
                tt.append(hbRunning.toString());
                tt.append(')');
            }
            if (corOrAis != null) {
                tt.append(" \n");
                tt.append(corOrAis);
                tt.append(" (");
                if (host.isCsAisRunning()) {
                    tt.append("running");
                    if (!host.isCsAisRc()) {
                        tt.append("/no rc.d");
                    }
                } else {
                    tt.append("not running");
                }
                if (host.isCsAisRc()) {
                    tt.append("/rc.d");
                }
                tt.append(')');
            }
            if (hbV != null && !host.isHeartbeatRunning()) {
                tt.append(" \nHeartbeat ");
                tt.append(hbV);
                tt.append(" (");
                tt.append(hbRunning.toString());
                tt.append(')');
            }
        }
        return tt.toString();
    }

    /**
     * Returns tooltip for host.
     */
    public final String getHostToolTip(final Host host) {
        final StringBuffer tt = new StringBuffer(80);
        tt.append("<b>" + host.getName() + "</b>");
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

    /**
     * Returns drbd graph object.
     */
    public final DrbdGraph getDrbdGraph() {
        final ClusterBrowser b = getClusterBrowser();
        if (b == null) {
            return null;
        }
        return b.getDrbdGraph();
    }

    /**
     * Returns a list of used network interface ports.
     */
    public final List<String> getDrbdVIPortList() {
        return drbdVIPortList;
    }

    /**
     * Lock block dev info objects.
     */
    public final void lockBlockDevInfos() {
        try {
            mBlockDevInfosLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Unlock block dev info objects.
     */
    public final void unlockBlockDevInfos() {
        mBlockDevInfosLock.release();
    }

    /**
     * Returns block devices node from the menu.
     */
    public final DefaultMutableTreeNode getBlockDevicesNode() {
        return blockDevicesNode;
    }

    /**
     * Returns net interfaces node from the menu.
     */
    public final DefaultMutableTreeNode getNetInterfacesNode() {
        return netInterfacesNode;
    }
}
