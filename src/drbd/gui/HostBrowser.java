/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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


package drbd.gui;

import drbd.utilities.Tools;
import drbd.utilities.DRBD;
import drbd.Exceptions;
import drbd.EditHostDialog;
import drbd.data.resources.NetInterface;
import drbd.data.resources.BlockDevice;
import drbd.data.Host;
import drbd.data.Cluster;
import drbd.data.Subtext;
import drbd.data.DRBDtestData;
import drbd.data.ClusterStatus;
import drbd.gui.ClusterBrowser.DrbdInfo;
import drbd.gui.ClusterBrowser.DrbdResourceInfo;
import drbd.AddDrbdUpgradeDialog;
import drbd.utilities.MyButton;
import drbd.utilities.ExecCallback;
import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;
import drbd.utilities.UpdatableItem;
import drbd.utilities.CRM;
import drbd.utilities.ButtonCallback;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.ImageIcon;
import javax.swing.JTextArea;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.Box;
import javax.swing.SwingUtilities;

import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Component;
import java.awt.Dimension;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.awt.Font;
import java.awt.Color;
import javax.swing.JColorChooser;
import javax.swing.SpringLayout;
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

    /** Harddisc icon. */
    private static final ImageIcon HARDDISC_ICON = Tools.createImageIcon(
                                   Tools.getDefault("DrbdGraph.HarddiscIcon"));
    /** No harddisc icon. */
    private static final ImageIcon NO_HARDDISC_ICON = Tools.createImageIcon(
                                 Tools.getDefault("DrbdGraph.NoHarddiscIcon"));
    /** Block device harddisc icon. */
    private static final ImageIcon BD_ICON = Tools.createImageIcon(
                              Tools.getDefault("HostBrowser.BlockDeviceIcon"));
    /** Net interface icon. */
    private static final ImageIcon NET_I_ICON = Tools.createImageIcon(
                                   Tools.getDefault("HostBrowser.NetIntIcon"));
    /** File system icon. */
    private static final ImageIcon FS_ICON = Tools.createImageIcon(
                               Tools.getDefault("HostBrowser.FileSystemIcon"));
    /** Host icon. */
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(
                                  Tools.getDefault("ClusterBrowser.HostIcon"));
    /** Large host icon. */
    private static final ImageIcon HOST_ICON_LARGE = Tools.createImageIcon(
                                  Tools.getDefault("HostBrowser.HostIcon"));
    /** Host standby icon. */
    private static final ImageIcon HOST_STANDBY_ICON =
     Tools.createImageIcon(Tools.getDefault("HeartbeatGraph.HostStandbyIcon"));
    /** Host standby off icon. */
    private static final ImageIcon HOST_STANDBY_OFF_ICON =
             Tools.createImageIcon(
                        Tools.getDefault("HeartbeatGraph.HostStandbyOffIcon"));
    /** Remove icon. */
    private static final ImageIcon HOST_REMOVE_ICON =
        Tools.createImageIcon(
                Tools.getDefault("HostBrowser.RemoveIcon"));
    /** Keyword that denotes flexible meta-disk. */
    private static final String DRBD_MD_TYPE_FLEXIBLE = "Flexible";
    /** Internal parameter name of drbd meta-disk. */
    private static final String DRBD_MD_PARAM         = "DrbdMetaDisk";
    /** Internal parameter name of drbd meta-disk index. */
    private static final String DRBD_MD_INDEX_PARAM   = "DrbdMetaDiskIndex";
    /** Internal parameter name of drbd network interface. */
    private static final String DRBD_NI_PARAM         = "DrbdNetInterface";
    /** Internal parameter name of drbd network interface port. */
    private static final String DRBD_NI_PORT_PARAM    = "DrbdNetInterfacePort";
    /** Color of the most of backgrounds. */
    private static final Color PANEL_BACKGROUND =
                                 Tools.getDefaultColor("ViewPanel.Background");
    /** Color of the status backgrounds. */
    private static final Color STATUS_BACKGROUND =
                          Tools.getDefaultColor("ViewPanel.Status.Background");
    /** Standby subtext. */
    private static final Subtext STANDBY_SUBTEXT =
                                         new Subtext("STANDBY", Color.RED);
    /** Offline subtext. */
    private static final Subtext OFFLINE_SUBTEXT =
                                         new Subtext("offline", Color.BLUE);
    /** Online subtext. */
    private static final Subtext ONLINE_SUBTEXT =
                                          new Subtext("online", Color.BLUE);
    /** Meta-disk subtext. */
    private static final Subtext METADISK_SUBTEXT =
                                          new Subtext("meta-disk", Color.BLUE);
    /** Swap subtext. */
    private static final Subtext SWAP_SUBTEXT =
                                          new Subtext("swap", Color.BLUE);
    /** Mounted subtext. */
    private static final Subtext MOUNTED_SUBTEXT =
                                          new Subtext("mounted", Color.BLUE);
    /** String length after the cut. */
    private static final int MAX_RIGHT_CORNER_STRING_LENGTH = 28;
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
        hostInfo = new HostInfo();
        hostDrbdInfo = new HostDrbdInfo();
        setTreeTop(hostInfo);
    }

    /**
     * Returns host info for this browser.
     */
    public final HostInfo getHostInfo() {
        return hostInfo;
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
                                Tools.getString("HostBrowser.NetInterfaces")));
        setNode(netInterfacesNode);
        topAdd(netInterfacesNode);

        /* block devices */
        blockDevicesNode = new DefaultMutableTreeNode(new CategoryInfo(
                                 Tools.getString("HostBrowser.BlockDevices")));
        setNode(blockDevicesNode);
        topAdd(blockDevicesNode);

        /* file systems */
        fileSystemsNode = new DefaultMutableTreeNode(new CategoryInfo(
                                  Tools.getString("HostBrowser.FileSystems")));
        setNode(fileSystemsNode);
        topAdd(fileSystemsNode);
    }

    /**
     * Returns cluster browser if available.
     */
    private ClusterBrowser getClusterBrowser() {
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
                nii = new NetInfo(ni.getName(), ni);
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
                bdi = new BlockDevInfo(bd.getName(), bd);
            }
            resource = new DefaultMutableTreeNode(bdi);
            //setNode(resource);
            blockDevicesNode.add(resource);
        }
        reload(blockDevicesNode);
        mBlockDevInfosLock.release();

        /* file systems */
        final Map<String, FilesystemInfo> oldFilesystems = getFilesystemsMap();
        try {
            mFileSystemsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        fileSystemsNode.removeAllChildren();
        for (final String fs : fss) {
            FilesystemInfo fsi;
            if (oldFilesystems.containsKey(fs)) {
                fsi = oldFilesystems.get(fs);
            } else {
                fsi = new FilesystemInfo(fs);
            }
            resource = new DefaultMutableTreeNode(fsi);
            setNode(resource);
            fileSystemsNode.add(resource);
        }
        reload(fileSystemsNode);
        mFileSystemsLock.release();
    }

    /**
     * @return net interfaces tree node.
     */
    public final DefaultMutableTreeNode getNetInterfacesNode() {
        return netInterfacesNode;
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
    public final Map<String, FilesystemInfo> getFilesystemsMap() {
        final Map<String, FilesystemInfo> filesystems =
                                         new HashMap<String, FilesystemInfo>();
        try {
            mFileSystemsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final Enumeration e = fileSystemsNode.children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode fsiNode =
                                      (DefaultMutableTreeNode) e.nextElement();
            final FilesystemInfo fsi = (FilesystemInfo) fsiNode.getUserObject();
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
    private void addExpertMenu(final MyMenu submenu) {
        if (submenu.getItemCount() > 0) {
            return;
        }
        /* panic */
        final MyMenuItem panicMenuItem = new MyMenuItem(
                    Tools.getString("HostBrowser.MakeKernelPanic")
                    + host.getName(),
                    null) {
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
        Tools.getGUIData().addToEnabledInGodMode(panicMenuItem);
        submenu.add(panicMenuItem);

        /* reboot */
        final MyMenuItem rebootMenuItem = new MyMenuItem(
                    Tools.getString("HostBrowser.MakeKernelReboot")
                    + host.getName(),
                    null) {
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
        Tools.getGUIData().addToEnabledInGodMode(rebootMenuItem);
        submenu.add(rebootMenuItem);
    }

    /**
     * Returns tooltip for host.
     */
    public final String getHostToolTip(final Host host) {
        final StringBuffer tt = new StringBuffer(80);
        tt.append("<b>" + host.getName() + "</b>");
        if (host.getCluster().getBrowser().isRealDcHost(host)) {
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
        /* Pacemaker */
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
                tt.append("\nHeartbeat ");
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
            tt.append("\nPacemaker ");
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
                tt.append("\nHeartbeat ");
                tt.append(hbV);
                tt.append(" (");
                tt.append(hbRunning.toString());
                tt.append(')');
            }
            if (corOrAis != null) {
                tt.append('\n');
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
                tt.append("\nHeartbeat ");
                tt.append(hbV);
                tt.append(" (");
                tt.append(hbRunning.toString());
                tt.append(')');
            }
        }
        return tt.toString();
    }

    /**
     * Returns drbd graph object.
     */
    private DrbdGraph getDrbdGraph() {
        return (DrbdGraph) host.getCluster().getBrowser().getDrbdGraph();
    }


    /**
     * This class holds info data for a filesystem.
     */
    class FilesystemInfo extends Info {
        /** cached output from the modinfo command for the info panel. */
        private String modinfo = null;

        /**
         * Prepares a new <code>FilesystemInfo</code> object.
         *
         * @param name
         *      name that will be shown in the tree
         */
        FilesystemInfo(final String name) {
            super(name);
        }

        /**
         * Returns file system icon for the menu.
         */
        public ImageIcon getMenuIcon(final boolean testOnly) {
            return FS_ICON;
        }

        /**
         * Returns type of the info text. text/plain or text/html.
         */
        protected String getInfoType() {
            return Tools.MIME_TYPE_TEXT_HTML;
        }

        /**
         * Returns info, before it is updated.
         */
        public String getInfo() {
            return "<html><pre>" + getName() + "</html></pre>";
        }

        /**
         * Updates info of the file system.
         */
        public void updateInfo(final JEditorPane ep) {
            final Runnable runnable = new Runnable() {
                public void run() {
                    if (modinfo == null) {
                        modinfo = Tools.execCommand(
                                            host,
                                            "/sbin/modinfo "
                                            + getName(),
                                            null,   /* ExecCallback */
                                            false); /* outputVisible */
                    }
                    ep.setText("<html><pre>" + modinfo + "</html></pre>");
                }
            };
            final Thread thread = new Thread(runnable);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
    }

    /**
     * This class holds info data for a host.
     * It shows host view, just like in the host tab.
     */
    public class HostInfo extends Info {
        /**
         * Prepares a new <code>HostInfo</code> object.
         */
        HostInfo() {
            super(host.getName());
        }

        /**
         * Returns a host icon for the menu.
         */
        public final ImageIcon getMenuIcon(final boolean testOnly) {
            return HOST_ICON;
        }

        /**
         * Returns id, which is name of the host.
         */
        public final String getId() {
            return host.getName();
        }

        /**
         * Returns a host icon for the category in the menu.
         */
        public final ImageIcon getCategoryIcon() {
            return HOST_ICON;
        }

        /**
         * Returns tooltip for the host.
         */
        public final String getToolTipForGraph(final boolean testOnly) {
            return getHostToolTip(host);
        }

        /**
         * Returns info panel.
         *
         * @return info panel
         */
        public final JComponent getInfoPanel() {
            Tools.getGUIData().setTerminalPanel(host.getTerminalPanel());
            final Font f = new Font("Monospaced", Font.PLAIN, 12);
            final JTextArea ta = new JTextArea();
            ta.setFont(f);

            final ExecCallback execCallback =
                new ExecCallback() {
                    public void done(final String ans) {
                        ta.setText(ans);
                    }

                    public void doneError(final String ans,
                                          final int exitCode) {
                        ta.setText("error");
                        Tools.sshError(host, "", ans, exitCode);
                    }

                };
            final MyButton crmMonButton = new MyButton("crm_mon");
            crmMonButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    host.execCommand("HostBrowser.getCrmMon",
                                     execCallback,
                                     null,  /* ConvertCmdCallback */
                                     true); /* outputVisible */
                }
            });
            host.registerEnableOnConnect(crmMonButton);

            final MyButton crmConfigureShowButton =
                                            new MyButton("crm configure show");
            crmConfigureShowButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    host.execCommand("HostBrowser.getCrmConfigureShow",
                                     execCallback,
                                     null,  /* ConvertCmdCallback */
                                     true); /* outputVisible */
                }
            });
            host.registerEnableOnConnect(crmConfigureShowButton);

            final JPanel mainPanel = new JPanel();
            mainPanel.setBackground(PANEL_BACKGROUND);
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

            final JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.setBackground(STATUS_BACKGROUND);
            buttonPanel.setMinimumSize(new Dimension(0, 50));
            buttonPanel.setPreferredSize(new Dimension(0, 50));
            buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
            mainPanel.add(buttonPanel);

            /* Actions */
            final JMenuBar mb = new JMenuBar();
            mb.setBackground(PANEL_BACKGROUND);
            final JMenu serviceCombo = getActionsMenu();
            updateMenus(null);
            mb.add(serviceCombo);
            buttonPanel.add(mb, BorderLayout.EAST);
            final JPanel p = new JPanel(new SpringLayout());
            p.setBackground(STATUS_BACKGROUND);

            p.add(crmMonButton);
            p.add(crmConfigureShowButton);
            SpringUtilities.makeCompactGrid(p, 2, 1,  // rows, cols
                                               1, 1,  // initX, initY
                                               1, 1); // xPad, yPad
            mainPanel.setMinimumSize(new Dimension(
                        Tools.getDefaultInt(
                                        "HostBrowser.ResourceInfoArea.Width"),
                        Tools.getDefaultInt(
                                        "HostBrowser.ResourceInfoArea.Height")
                        ));
            mainPanel.setPreferredSize(new Dimension(
                        Tools.getDefaultInt(
                                        "HostBrowser.ResourceInfoArea.Width"),
                        Tools.getDefaultInt(
                                        "HostBrowser.ResourceInfoArea.Height")
                        ));
            buttonPanel.add(p);
            mainPanel.add(new JScrollPane(ta));
            //mainPanel.add(panel);
            return mainPanel;
        }

        /**
         * Gets host.
         *
         * @return host of this info
         */
        public final Host getHost() {
            return host;
        }

        /**
         * Compares this host info name with specified hostinfo's name.
         *
         * @param otherHI
         *              other host info
         * @return true if they are equal
         */
        public final boolean equals(final HostInfo otherHI) {
            if (otherHI == null) {
                return false;
            }
            return otherHI.toString().equals(host.getName());
        }

        /**
         * Returns string representation of the host. It's same as name.
         */
        public final String toString() {
            return host.getName();
        }

        /**
         * Returns name of the host.
         */
        public final String getName() {
            return host.getName();
        }

        /**
         * Creates the popup for the host.
         */
        public final List<UpdatableItem> createPopup() {
            final List<UpdatableItem>items = new ArrayList<UpdatableItem>();
            final boolean testOnly = false;
            /* host wizard */
            final MyMenuItem hostWizardItem =
                new MyMenuItem(Tools.getString("HostBrowser.HostWizard"),
                               HOST_ICON_LARGE,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return true;
                    }

                    public void action() {
                        final EditHostDialog dialog = new EditHostDialog(host);
                        dialog.showDialogs();
                    }
                };
            items.add(hostWizardItem);
            registerMenuItem(hostWizardItem);
            Tools.getGUIData().registerAddHostButton(hostWizardItem);
            /* heartbeat standby on/off */
            final HostInfo thisClass = this;
            final MyMenuItem standbyItem =
                new MyMenuItem(Tools.getString("HostBrowser.CRM.StandByOn"),
                               HOST_STANDBY_ICON,
                               null,

                               Tools.getString("HostBrowser.CRM.StandByOff"),
                               HOST_STANDBY_OFF_ICON,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean predicate() {
                        return !isStandby(testOnly);
                    }

                    public boolean enablePredicate() {
                        return getHost().isClStatus();
                    }

                    public void action() {
                        if (isStandby(testOnly)) {
                            CRM.standByOff(host, testOnly);
                        } else {
                            CRM.standByOn(host, testOnly);
                        }
                    }
                };
            final ClusterBrowser.ClMenuItemCallback standbyItemCallback =
                     getClusterBrowser().new ClMenuItemCallback(standbyItem,
                                                                host) {
                public void action(final Host host) {
                    if (isStandby(false)) {
                        CRM.standByOff(host, true);
                    } else {
                        CRM.standByOn(host, true);
                    }
                }
            };
            addMouseOverListener(standbyItem, standbyItemCallback);
            items.add(standbyItem);
            registerMenuItem(standbyItem);
            /* change host color */
            final MyMenuItem changeHostColorItem =
                new MyMenuItem(Tools.getString(
                                           "HostBrowser.Drbd.ChangeHostColor"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return true;
                    }

                    public void action() {
                        final JColorChooser tcc = new JColorChooser();
                        Color newColor = tcc.showDialog(
                                            Tools.getGUIData().getMainFrame(),
                                            "asdf",
                                            host.getPmColors()[0]);
                        if (newColor != null) {
                            host.setColor(newColor);
                        }
                    }
                };
            items.add(changeHostColorItem);
            registerMenuItem(changeHostColorItem);

            /* view logs */
            final MyMenuItem viewLogsItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.ViewLogs"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isConnected();
                    }

                    public void action() {
                        drbd.gui.dialog.Logs l =
                                            new drbd.gui.dialog.Logs(host);
                        l.showDialog();
                    }
                };
            items.add(viewLogsItem);
            registerMenuItem(viewLogsItem);
            /* remove host from gui */
            final MyMenuItem removeHostItem =
                new MyMenuItem(Tools.getString("HostBrowser.RemoveHost"),
                               HOST_REMOVE_ICON,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().getCluster() == null;
                    }

                    public void action() {
                        getHost().disconnect();
                        Tools.getConfigData().removeHostFromHosts(getHost());
                        Tools.getGUIData().allHostsUpdate();
                    }
                };
            registerMenuItem(removeHostItem);
            items.add(removeHostItem);

            /* expert options */
            final MyMenu hostExpertSubmenu = new MyMenu(
                            Tools.getString("HostBrowser.ExpertSubmenu")) {
                private static final long serialVersionUID = 1L;
                public boolean enablePredicate() {
                    return host.isConnected();
                }

                public void update() {
                    super.update();
                    addExpertMenu(this);
                }
            };
            items.add(hostExpertSubmenu);
            registerMenuItem(hostExpertSubmenu);

            return items;
        }

        /**
         * Returns Cluster graph.
         */
        private HeartbeatGraph getHeartbeatGraph() {
            final ClusterBrowser b = getClusterBrowser();
            if (b == null) {
                return null;
            }
            return (HeartbeatGraph) b.getHeartbeatGraph();
        }

        /**
         * Returns grahical view if there is any.
         */
        public final JPanel getGraphicalView() {
            final HeartbeatGraph hg = getHeartbeatGraph();
            if (hg == null) {
                return null;
            }
            return hg.getServicesInfo().getGraphicalView();
        }

        /**
         * Returns how much of this is used.
         */
        public final int getUsed() {
            // TODO: maybe the load?
            return -1;
        }

        /**
         * Returns subtexts that appears in the host vertex in the cluster
         * graph.
         */
        public final Subtext[] getSubtextsForGraph(final boolean testOnly) {
            final List<Subtext> texts = new ArrayList<Subtext>();
            if (getHost().isConnected()) {
                if (!getHost().isClStatus()) {
                   texts.add(new Subtext("waiting for cluster status...",
                                         null));
                }
            } else {
                texts.add(new Subtext("connecting...",
                                      null));
            }
            return texts.toArray(new Subtext[texts.size()]);
        }

        /**
         * Returns text that appears above the icon in the graph.
         */
        public final String getIconTextForGraph(final boolean testOnly) {
            if (!getHost().isConnected()) {
                return Tools.getString("HostBrowser.Hb.NoInfoAvailable");
            }
            return null;
        }

        /**
         * Returns whether this host is in stand by.
         */
        public final boolean isStandby(final boolean testOnly) {
            final ClusterBrowser b = getClusterBrowser();
            if (b == null) {
                return false;
            }
            return b.isStandby(host, testOnly);
        }

        /**
         * Returns cluster status.
         */
        public final ClusterStatus getClusterStatus() {
            final ClusterBrowser b = getClusterBrowser();
            if (b == null) {
                return null;
            }
            return b.getClusterStatus();
        }

        /**
         * Returns text that appears in the corner of the graph.
         */
        protected final Subtext getRightCornerTextForGraph(
                                                      final boolean testOnly) {
            if (getHost().isClStatus()) {
                if (isStandby(testOnly)) {
                    return STANDBY_SUBTEXT;
                } else {
                    return ONLINE_SUBTEXT;
                }
            } else if (getHost().isConnected()) {
                return OFFLINE_SUBTEXT;
            }
            return null;
        }
    }

    /**
     * This class holds info data for a host.
     * It shows host view, just like in the host tab.
     */
    public class HostDrbdInfo extends Info {
        /**
         * Prepares a new <code>HostDrbdInfo</code> object.
         */
        HostDrbdInfo() {
            super(host.getName());
        }

        /**
         * Returns a host icon for the menu.
         */
        public final ImageIcon getMenuIcon(final boolean testOnly) {
            return HOST_ICON;
        }

        /**
         * Returns id, which is name of the host.
         */
        public final String getId() {
            return host.getName();
        }

        /**
         * Returns a host icon for the category in the menu.
         */
        public final ImageIcon getCategoryIcon() {
            return HOST_ICON;
        }

        /**
         * Start upgrade drbd dialog.
         */
        public final void upgradeDrbd() {
            final AddDrbdUpgradeDialog adud = new AddDrbdUpgradeDialog(this);
            adud.showDialogs();
        }

        /**
         * Returns tooltip for the host.
         */
        public final String getToolTipForGraph(final boolean testOnly) {
            return getHostToolTip(host);
        }

        /**
         * Returns info panel.
         *
         * @return info panel
         */
        public final JComponent getInfoPanel() {
            Tools.getGUIData().setTerminalPanel(host.getTerminalPanel());
            final Font f = new Font("Monospaced", Font.PLAIN, 12);
            final JTextArea ta = new JTextArea();
            ta.setFont(f);

            final ExecCallback execCallback =
                new ExecCallback() {
                    public void done(final String ans) {
                        ta.setText(ans);
                    }

                    public void doneError(final String ans,
                                          final int exitCode) {
                        ta.setText("error");
                        Tools.sshError(host, "", ans, exitCode);
                    }

                };
            // TODO: disable buttons if disconnected?
            final MyButton procDrbdButton = new MyButton("/proc/drbd");
            procDrbdButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    host.execCommand("DRBD.getProcDrbd",
                                     execCallback,
                                     null,  /* ConvertCmdCallback */
                                     true); /* outputVisible */
                }
            });
            host.registerEnableOnConnect(procDrbdButton);

            final MyButton drbdProcsButton = new MyButton("DRBD Processes");
            drbdProcsButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    host.execCommand("DRBD.getProcesses",
                                     execCallback,
                                     null,  /* ConvertCmdCallback */
                                     true); /* outputVisible */
                }
            });
            host.registerEnableOnConnect(drbdProcsButton);

            final JPanel mainPanel = new JPanel();
            mainPanel.setBackground(PANEL_BACKGROUND);
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

            final JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.setBackground(STATUS_BACKGROUND);
            buttonPanel.setMinimumSize(new Dimension(0, 50));
            buttonPanel.setPreferredSize(new Dimension(0, 50));
            buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
            mainPanel.add(buttonPanel);

            /* Actions */
            final JMenuBar mb = new JMenuBar();
            mb.setBackground(PANEL_BACKGROUND);
            final JMenu serviceCombo = getActionsMenu();
            updateMenus(null);
            mb.add(serviceCombo);
            buttonPanel.add(mb, BorderLayout.EAST);
            final JPanel p = new JPanel(new SpringLayout());
            p.setBackground(STATUS_BACKGROUND);

            p.add(procDrbdButton);
            p.add(drbdProcsButton);
            SpringUtilities.makeCompactGrid(p, 2, 1,  // rows, cols
                                               1, 1,  // initX, initY
                                               1, 1); // xPad, yPad
            mainPanel.setMinimumSize(new Dimension(
                        Tools.getDefaultInt(
                                        "HostBrowser.ResourceInfoArea.Width"),
                        Tools.getDefaultInt(
                                        "HostBrowser.ResourceInfoArea.Height")
                        ));
            mainPanel.setPreferredSize(new Dimension(
                        Tools.getDefaultInt(
                                        "HostBrowser.ResourceInfoArea.Width"),
                        Tools.getDefaultInt(
                                        "HostBrowser.ResourceInfoArea.Height")
                        ));
            buttonPanel.add(p);
            mainPanel.add(new JScrollPane(ta));
            //mainPanel.add(panel);
            return mainPanel;
        }

        /**
         * Gets host.
         *
         * @return host of this info
         */
        public final Host getHost() {
            return host;
        }


        /**
         * Compares this host info name with specified hostdrbdinfo's name.
         *
         * @param otherHI
         *              other host info
         * @return true if they are equal
         */
        public final boolean equals(final HostDrbdInfo otherHI) {
            if (otherHI == null) {
                return false;
            }
            return otherHI.toString().equals(host.getName());
        }

        /**
         * Returns string representation of the host. It's same as name.
         */
        public final String toString() {
            return host.getName();
        }

        /**
         * Returns name of the host.
         */
        public final String getName() {
            return host.getName();
        }

        /**
         * Creates the popup for the host.
         */
        public final List<UpdatableItem> createPopup() {
            final List<UpdatableItem>items = new ArrayList<UpdatableItem>();

            /* host wizard */
            final MyMenuItem hostWizardItem =
                new MyMenuItem(Tools.getString("HostBrowser.HostWizard"),
                               HOST_ICON_LARGE,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return true;
                    }

                    public void action() {
                        final EditHostDialog dialog = new EditHostDialog(host);
                        dialog.showDialogs();
                    }
                };
            items.add(hostWizardItem);
            registerMenuItem(hostWizardItem);
            Tools.getGUIData().registerAddHostButton(hostWizardItem);
            final boolean testOnly = false;
            /* load drbd */
            final MyMenuItem loadItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.LoadDrbd"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isConnected()
                               && !getHost().isDrbdStatus();
                    }

                    public void action() {
                        DRBD.load(getHost(), testOnly);
                    }
                };
            items.add(loadItem);
            registerMenuItem(loadItem);

            /* start drbd */
            final MyMenuItem upAllItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.UpAll"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isDrbdStatus();
                    }

                    public void action() {
                        for (final BlockDevInfo bdi : getBlockDevInfos()) {
                            if (bdi.getBlockDevice().isDrbd()
                                && !bdi.isConnected(testOnly)
                                && !bdi.getBlockDevice().isAttached()) {
                                bdi.drbdUp(testOnly);
                            }
                        }
                    }
                };
            items.add(upAllItem);
            registerMenuItem(upAllItem);

            /* upgrade drbd */
            final MyMenuItem upgradeDrbdItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.UpgradeDrbd"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isConnected();
                    }

                    public void action() {
                        upgradeDrbd();
                    }
                };
            items.add(upgradeDrbdItem);
            registerMenuItem(upgradeDrbdItem);

            /* change host color */
            final MyMenuItem changeHostColorItem =
                new MyMenuItem(Tools.getString(
                                           "HostBrowser.Drbd.ChangeHostColor"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return true;
                    }

                    public void action() {
                        final JColorChooser tcc = new JColorChooser();
                        Color newColor = tcc.showDialog(
                                            Tools.getGUIData().getMainFrame(),
                                            "asdf",
                                            host.getPmColors()[0]);
                        if (newColor != null) {
                            host.setColor(newColor);
                        }
                    }
                };
            items.add(changeHostColorItem);
            registerMenuItem(changeHostColorItem);

            /* view logs */
            final MyMenuItem viewLogsItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.ViewLogs"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isConnected();
                    }

                    public void action() {
                        drbd.gui.dialog.DrbdsLog l =
                                            new drbd.gui.dialog.DrbdsLog(host);
                        l.showDialog();
                    }
                };
            items.add(viewLogsItem);
            registerMenuItem(viewLogsItem);

            /* connect all */
            final MyMenuItem connectAllItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.ConnectAll"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isDrbdStatus();
                    }

                    public void action() {
                        for (final BlockDevInfo bdi : getBlockDevInfos()) {
                            if (bdi.getBlockDevice().isDrbd()
                                && !bdi.isConnectedOrWF(testOnly)) {
                                bdi.connect(testOnly);
                            }
                        }
                    }
                };
            items.add(connectAllItem);
            registerMenuItem(connectAllItem);

            /* disconnect all */
            final MyMenuItem disconnectAllItem =
                new MyMenuItem(Tools.getString(
                                            "HostBrowser.Drbd.DisconnectAll"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isDrbdStatus();
                    }

                    public void action() {
                        for (final BlockDevInfo bdi : getBlockDevInfos()) {
                            if (bdi.getBlockDevice().isDrbd()
                                && bdi.isConnectedOrWF(testOnly)) {
                                bdi.disconnect(testOnly);
                            }
                        }
                    }
                };
            items.add(disconnectAllItem);
            registerMenuItem(disconnectAllItem);

            /* attach dettached */
            final MyMenuItem attachAllItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.AttachAll"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isDrbdStatus();
                    }

                    public void action() {
                        for (final BlockDevInfo bdi : getBlockDevInfos()) {
                            if (bdi.getBlockDevice().isDrbd()
                                && !bdi.getBlockDevice().isAttached()) {
                                bdi.attach(testOnly);
                            }
                        }
                    }
                };
            items.add(attachAllItem);
            registerMenuItem(attachAllItem);

            /* set all primary */
            final MyMenuItem setAllPrimaryItem =
                new MyMenuItem(Tools.getString(
                                            "HostBrowser.Drbd.SetAllPrimary"),
                                            null,
                                            null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isDrbdStatus();
                    }

                    public void action() {
                        for (final BlockDevInfo bdi : getBlockDevInfos()) {
                            if (bdi.getBlockDevice().isDrbd()
                                && bdi.getBlockDevice().isSecondary()) {
                                bdi.setPrimary(testOnly);
                            }
                        }
                    }
                };
            items.add(setAllPrimaryItem);
            registerMenuItem(setAllPrimaryItem);

            /* set all secondary */
            final MyMenuItem setAllSecondaryItem =
                new MyMenuItem(Tools.getString(
                                           "HostBrowser.Drbd.SetAllSecondary"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isDrbdStatus();
                    }

                    public void action() {
                        for (final BlockDevInfo bdi : getBlockDevInfos()) {
                            if (bdi.getBlockDevice().isDrbd()
                                && bdi.getBlockDevice().isPrimary()) {
                                bdi.setSecondary(testOnly);
                            }
                        }
                    }
                };
            items.add(setAllSecondaryItem);
            registerMenuItem(setAllSecondaryItem);

            /* remove host from gui */
            final MyMenuItem removeHostItem =
                new MyMenuItem(Tools.getString("HostBrowser.RemoveHost"),
                               HOST_REMOVE_ICON,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().getCluster() == null;
                    }

                    public void action() {
                        getHost().disconnect();
                        Tools.getConfigData().removeHostFromHosts(getHost());
                        Tools.getGUIData().allHostsUpdate();
                    }
                };
            registerMenuItem(removeHostItem);
            items.add(removeHostItem);

            /* expert options */
            final MyMenu hostExpertSubmenu = new MyMenu(
                            Tools.getString("HostBrowser.ExpertSubmenu")) {
                private static final long serialVersionUID = 1L;
                public boolean enablePredicate() {
                    return host.isConnected();
                }

                public void update() {
                    super.update();
                    addExpertMenu(this);
                }
            };
            items.add(hostExpertSubmenu);
            registerMenuItem(hostExpertSubmenu);

            return items;
        }

        /**
         * Returns grahical view if there is any.
         */
        public final JPanel getGraphicalView() {
            final DrbdGraph dg = getDrbdGraph();
            dg.getDrbdInfo().setSelectedNode(null);
            return dg.getDrbdInfo().getGraphicalView();
        }
        /**
         * Returns how much of this is used.
         */
        public final int getUsed() {
            // TODO: maybe the load?
            return -1;
        }

        /**
         * Returns subtexts that appears in the host vertex in the cluster
         * graph.
         */
        public final Subtext[] getSubtextsForGraph() {
            final List<Subtext> texts = new ArrayList<Subtext>();
            if (getHost().isConnected()) {
                if (!getHost().isClStatus()) {
                   texts.add(new Subtext("waiting for cluster status...",
                                         null));
                }
            } else {
                texts.add(new Subtext("connecting...",
                                      null));
            }
            return texts.toArray(new Subtext[texts.size()]);
        }

        /**
         * Returns subtexts that appears in the host vertex in the drbd graph.
         */
        public final Subtext[] getSubtextsForDrbdGraph(final boolean testOnly) {
            final List<Subtext> texts = new ArrayList<Subtext>();
            if (getHost().isConnected()) {
                if (!getHost().isDrbdLoaded()) {
                   texts.add(new Subtext("DRBD not loaded", null));
                } else if (!getHost().isDrbdStatus()) {
                   texts.add(new Subtext("waiting...", null));
                }
            } else {
                texts.add(new Subtext("connecting...", null));
            }
            return texts.toArray(new Subtext[texts.size()]);
        }

        /**
         * Returns text that appears above the icon in the drbd graph.
         */
        public final String getIconTextForDrbdGraph(final boolean testOnly) {
            if (!getHost().isConnected()) {
                return Tools.getString("HostBrowser.Drbd.NoInfoAvailable");
            }
            return null;
        }

        /**
         * Returns text that appears in the corner of the drbd graph.
         */
        protected final Subtext getRightCornerTextForDrbdGraph(
                                                     final boolean testOnly) {
            return null;
        }
    }

    /**
     * This class holds info data for a net interface.
     */
    class NetInfo extends Info {

        /**
         * Prepares a new <code>NetInfo</code> object.
         *
         * @param name
         *      name that will be shown in the tree
         * @param netInterface
         *      network interface
         */
        public NetInfo(final String name, final NetInterface netInterface) {
            super(name);
            setResource(netInterface);
        }

        /**
         * Returns info of this net interface, which is updatable.
         */
        public void updateInfo(final JEditorPane ep) {
            final Runnable runnable = new Runnable() {
                public void run() {
                    String text = Tools.execCommand(
                                            host,
                                            "/sbin/ifconfig "
                                            + getName(),
                                            null,   /* ExecCallback */
                                            false); /* outputVisible */
                    ep.setText("<html><pre>" + text + "</html></pre>");
                }
            };
            final Thread thread = new Thread(runnable);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }

        /**
         * Returns string representation of the net interface.
         */
        public String toString() {
            final StringBuffer s = new StringBuffer(getName());
            final String ip = getNetInterface().getIp();
            if (ip != null) {
                s.append(" (" + ip + ")");
            }
            return s.toString();
        }

        /**
         * Returns icon of the net interface for the menu.
         */
        public ImageIcon getMenuIcon(final boolean testOnly) {
            return NET_I_ICON;
        }

        /**
         * Returns ip of the net interface.
         */
        public String getStringValue() {
            return getNetInterface().getIp();
        }

        /**
         * Returns net interface resource.
         */
        public NetInterface getNetInterface() {
            return (NetInterface) getResource();
        }

        /**
         * Returns whether ips equal.
         */
        public boolean equals(final Object value) {
            if (Tools.isStringClass(value)) {
                // TODO: race is here
                return getNetInterface().getIp().equals(value.toString());
            } else {
                return toString().equals(value.toString());
            }
        }

        //public int hashCode() {
        //    return toString().hashCode();
        //}
    }

    /**
     * This class holds info data for a block device.
     */
    public class BlockDevInfo extends EditableInfo {
        /** DRBD resource in which this block device is member. */
        private DrbdResourceInfo drbdResourceInfo;
        /** Map from paremeters to the fact if the last entered value was
         * correct. */
        private final Map<String, Boolean> paramCorrectValueMap =
                                                new HashMap<String, Boolean>();
        /** Cache for the info panel. */
        private JComponent infoPanel = null;
        /** Extra options panel. */
        private final JPanel extraOptionsPanel = new JPanel();

        /**
         * Prepares a new <code>BlockDevInfo</code> object.
         *
         * @param name
         *      name that will be shown in the tree
         * @param blockDevice
         *      bock device
         */
        public BlockDevInfo(final String name, final BlockDevice blockDevice) {
            super(name);
            setResource(blockDevice);
        }

        /**
         * Sets info panel of this block devices. TODO: explain why.
         */
        public final void setInfoPanel(final JComponent infoPanel) {
            this.infoPanel = infoPanel;
        }

        /**
         * Remove this block device.
         *
         * TODO: check this
         */
        public final void removeMyself(final boolean testOnly) {
            getBlockDevice().setValue(DRBD_NI_PARAM, null);
            getBlockDevice().setValue(DRBD_NI_PORT_PARAM, null);
            getBlockDevice().setValue(DRBD_MD_PARAM, null);
            getBlockDevice().setValue(DRBD_MD_INDEX_PARAM, null);
            super.removeMyself(testOnly);
            infoPanel = null;
            Tools.unregisterExpertPanel(extraOptionsPanel);
        }

        /**
         * Returns object of the other block device that is connected via drbd
         * to this block device.
         */
        public final BlockDevInfo getOtherBlockDevInfo() {
            return drbdResourceInfo.getOtherBlockDevInfo(this);
        }

        /**
         * Returns host on which is this block device.
         */
        public final Host getHost() {
            return host;
        }

        /**
         * Returns block device icon for the menu.
         */
        public final ImageIcon getMenuIcon(final boolean testOnly) {
            return BD_ICON;
        }

        /**
         * Returns info of this block device as string.
         */
        public final String getInfo() {
            final StringBuffer ret = new StringBuffer(120);
            ret.append("Host            : ");
            ret.append(host.getName());
            ret.append("\nDevice          : ");
            ret.append(getBlockDevice().getName());
            ret.append("\nMeta disk       : ");
            ret.append(getBlockDevice().isDrbdMetaDisk());
            ret.append("\nSize            : ");
            ret.append(getBlockDevice().getBlockSize());
            ret.append(" blocks");
            if (getBlockDevice().getMountedOn() == null) {
                ret.append("\nnot mounted");
            } else {
                ret.append("\nMounted on      : ");
                ret.append(getBlockDevice().getMountedOn());
                ret.append("\nType            : ");
                ret.append(getBlockDevice().getFsType());
                if (getUsed() >= 0) {
                    ret.append("\nUsed:           : ");
                    ret.append(getUsed());
                    ret.append('%');
                }
            }
            if (getBlockDevice().isDrbd()) {
                ret.append("\nConnection state: ");
                ret.append(getBlockDevice().getConnectionState());
                ret.append("\nNode state      : ");
                ret.append(getBlockDevice().getNodeState());
                ret.append("\nDisk state      : ");
                ret.append(getBlockDevice().getDiskState());
                ret.append('\n');
            }
            return ret.toString();
        }

        /**
         * Returns tool tip for this block device.
         */
        public final String getToolTipForGraph(final boolean testOnly) {
            final StringBuffer tt = new StringBuffer(60);

            if (getBlockDevice().isDrbd()) {
                tt.append("<b>");
                tt.append(drbdResourceInfo.getDevice());
                tt.append("</b> (");
                tt.append(getBlockDevice().getName());
                tt.append(')');
            } else {
                tt.append("<b>");
                tt.append(getBlockDevice().getName());
                tt.append("</b>");
            }
            tt.append("</b>");
            if (getBlockDevice().isDrbdMetaDisk()) {
                tt.append(" (Meta Disk)\n");
                for (final BlockDevice mb:
                                getBlockDevice().getMetaDiskOfBlockDevices()) {
                    tt.append("&nbsp;&nbsp;of ");
                    tt.append(mb.getName());
                    tt.append('\n');
                }

            }

            if (getBlockDevice().isDrbd()) {
                if (host.isDrbdStatus()) {
                    String cs = getBlockDevice().getConnectionState();
                    String st = getBlockDevice().getNodeState();
                    String ds = getBlockDevice().getDiskState();
                    if (cs == null) {
                        cs = "not available";
                    }
                    if (st == null) {
                        st = "not available";
                    }
                    if (ds == null) {
                        ds = "not available";
                    }

                    tt.append("\n<table><tr><td><b>cs:</b></td><td>");
                    tt.append(cs);
                    tt.append("</td></tr><tr><td><b>ro:</b></td><td>");
                    tt.append(st);
                    tt.append("</td></tr><tr><td><b>ds:</b></td><td>");
                    tt.append(ds);
                    tt.append("</td></tr></table>");
                } else {
                    tt.append('\n');
                    tt.append(
                            Tools.getString("HostBrowser.Hb.NoInfoAvailable"));
                }
            }
            return tt.toString();
        }

        /**
         * Creates config for one node.
         */
        public final String drbdNodeConfig(final String resource,
                                           final String drbdDevice)
                throws Exceptions.DrbdConfigException {

            if (drbdDevice == null) {
                throw new Exceptions.DrbdConfigException(
                                        "Drbd device not defined for host "
                                        + host.getName()
                                        + " (" + resource + ")");
            }
            if (getBlockDevice().getDrbdNetInterfaceWithPort() == null) {
                throw new Exceptions.DrbdConfigException(
                                        "Net interface not defined for host "
                                        + host.getName()
                                        + " (" + resource + ")");
            }
            if (getBlockDevice().getName() == null) {
                throw new Exceptions.DrbdConfigException(
                                        "Block device not defined for host "
                                        + host.getName()
                                        + " (" + resource + ")");
            }

            final StringBuffer config = new StringBuffer(120);
            config.append("\ton ");
            config.append(host.getName());
            config.append(" {\n\t\tdevice\t");
            config.append(drbdDevice);
            config.append(";\n\t\tdisk\t");
            config.append(getBlockDevice().getName());
            config.append(";\n\t\taddress\t");
            config.append(getBlockDevice().getDrbdNetInterfaceWithPort(
                                        getComboBoxValue(DRBD_NI_PARAM),
                                        getComboBoxValue(DRBD_NI_PORT_PARAM)));
            //config.append(getBlockDevice().getDrbdNetInterfaceWithPort());
            config.append(";\n\t\t");
            config.append(getBlockDevice().getMetaDiskString(
                                       getComboBoxValue(DRBD_MD_PARAM),
                                       getComboBoxValue(DRBD_MD_INDEX_PARAM)));
            //config.append(getBlockDevice().getMetaDiskString());
            config.append(";\n\t}\n");
            return config.toString();
        }

        public final void selectMyself() {
            super.selectMyself();
        }

        public final void setDrbd(final boolean drbd) {
            getBlockDevice().setDrbd(drbd);
        }

        protected final String getSection(final String param) {
            return getBlockDevice().getSection(param);
        }

        protected final Object[] getPossibleChoices(final String param) {
            return getBlockDevice().getPossibleChoices(param);
        }

        protected final Object getDefaultValue(final String param) {
            return "<select>";
        }

        public final int getNextVIPort() {
            int port =
                   Tools.getDefaultInt("HostBrowser.DrbdNetInterfacePort") - 1;
            for (final String portString : drbdVIPortList) {
                final int p = Integer.valueOf(portString);
                if (p > port) {
                    port = p;
                }
            }
            return port;
        }

        public final void setDefaultVIPort(final int port) {
            final String value = Integer.toString(port);
            getBlockDevice().setValue(DRBD_NI_PORT_PARAM, value);
            drbdVIPortList.add(value);
        }

        protected final GuiComboBox getParamComboBox(final String param,
                                                     final String prefix,
                                                     final int width) {
            GuiComboBox gcb;
            if (DRBD_NI_PORT_PARAM.equals(param)) {
                final List<String> drbdVIPorts = new ArrayList<String>();
                String defaultPort = getBlockDevice().getValue(param);
                if (defaultPort == null) {
                    defaultPort = getBlockDevice().getDefaultValue(param);
                }
                drbdVIPorts.add(defaultPort);
                int i = 0;
                int index = Tools.getDefaultInt(
                                          "HostBrowser.DrbdNetInterfacePort");
                while (i < 10) {
                    final String port = Integer.toString(index);
                    if (!drbdVIPortList.contains(port)) {
                        drbdVIPorts.add(port);
                        i++;
                    }
                    index++;
                }
                String regexp = null;
                if (isInteger(param)) {
                    regexp = "^\\d*$";
                }
                gcb = new GuiComboBox(
                           defaultPort,
                           drbdVIPorts.toArray(new String[drbdVIPorts.size()]),
                           null,
                           regexp,
                           width,
                           null);
                gcb.setValue(defaultPort);
                paramComboBoxAdd(param, prefix, gcb);
                gcb.setEnabled(true);
                gcb.setAlwaysEditable(true);
            } else if (DRBD_MD_INDEX_PARAM.equals(param)) {
                gcb = super.getParamComboBox(param, prefix, width);
                gcb.setAlwaysEditable(true);
            } else {
                gcb = super.getParamComboBox(param, prefix, width);
                gcb.setEditable(false);
            }
            return gcb;
        }

        protected final boolean checkParam(final String param, String value) {
            boolean ret = true;
            if (value == null) {
                value = "";
            }
            if ("".equals(value) && isRequired(param)) {
                ret = false;
            } else if (DRBD_MD_PARAM.equals(param)) {
                final boolean internal = "internal".equals(value);
                final GuiComboBox ind = paramComboBoxGet(DRBD_MD_INDEX_PARAM,
                                                         null);
                final GuiComboBox indW = paramComboBoxGet(DRBD_MD_INDEX_PARAM,
                                                          "wizard");
                if (internal) {
                    ind.setValue(DRBD_MD_TYPE_FLEXIBLE);
                    if (indW != null) {
                        indW.setValue(DRBD_MD_TYPE_FLEXIBLE);
                    }
                }
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ind.setEnabled(!internal);
                    }
                });
                if (indW != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            indW.setEnabled(!internal);
                        }
                    });
                }
            } else if (DRBD_NI_PORT_PARAM.equals(param)) {
                if (drbdVIPortList.contains(value)
                    && !value.equals(getBlockDevice().getValue(param))) {
                    ret = false;
                }
                final Pattern p = Pattern.compile(".*\\D.*");
                final Matcher m = p.matcher(value);
                if (m.matches()) {
                    ret = false;
                }
            } else if (DRBD_MD_INDEX_PARAM.equals(param)) {
                if (drbdVIPortList.contains(value)
                    && !value.equals(getBlockDevice().getValue(param))) {
                    ret = false;
                }
                final Pattern p = Pattern.compile(".*\\D.*");
                final Matcher m = p.matcher(value);
                if (m.matches() && !DRBD_MD_TYPE_FLEXIBLE.equals(value)) {
                    ret = false;
                }
            }
            paramCorrectValueMap.remove(param);
            paramCorrectValueMap.put(param, ret);
            return ret;
        }

        protected final boolean isRequired(final String param) {
            return true;
        }

        protected final boolean isInteger(final String param) {
            if (DRBD_NI_PORT_PARAM.equals(param)) {
                return true;
            }
            return false;
        }

        protected final boolean isTimeType(final String param) {
            /* not required */
            return false;
        }


        protected final boolean isCheckBox(final String param) {
            return false;
        }

        protected final String getParamType(final String param) {
            return null;
        }

        protected final Object[] getParamPossibleChoices(final String param) {
            if (DRBD_NI_PARAM.equals(param)) {
                /* net interfaces */
                StringInfo defaultNetInterface = null;
                String netInterfaceString =
                                     getBlockDevice().getValue(DRBD_NI_PARAM);
                if (netInterfaceString == null
                    || netInterfaceString.equals("")) {
                    defaultNetInterface =
                                new StringInfo(
                                    Tools.getString(
                                       "HostBrowser.DrbdNetInterface.Select"),
                                    null);
                    netInterfaceString = defaultNetInterface.toString();
                    getBlockDevice().setDefaultValue(DRBD_NI_PARAM,
                                                     netInterfaceString);
                }
                return getNetInterfaces(defaultNetInterface,
                                        netInterfacesNode.children());
            } else if (DRBD_MD_PARAM.equals(param)) {
                /* meta disk */
                final StringInfo internalMetaDisk =
                        new StringInfo(Tools.getString(
                                            "HostBrowser.MetaDisk.Internal"),
                                       "internal");
                final String defaultMetaDiskString =
                                            internalMetaDisk.toString();

                try {
                    mBlockDevInfosLock.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                final Info[] blockDevices =
                                getAvailableBlockDevicesForMetaDisk(
                                                internalMetaDisk,
                                                getName(),
                                                blockDevicesNode.children());
                mBlockDevInfosLock.release();

                getBlockDevice().setDefaultValue(DRBD_MD_PARAM,
                                                 defaultMetaDiskString);
                return blockDevices;
            } else if (DRBD_MD_INDEX_PARAM.equals(param)) {

                String defaultMetaDiskIndex = getBlockDevice().getValue(
                                                           DRBD_MD_INDEX_PARAM);
                if ("internal".equals(defaultMetaDiskIndex)) {
                    defaultMetaDiskIndex =
                             Tools.getString("HostBrowser.MetaDisk.Internal");
                }

                String[] indeces = new String[11];
                int index = 0;
                if (defaultMetaDiskIndex == null) {
                    defaultMetaDiskIndex = DRBD_MD_TYPE_FLEXIBLE;
                } else if (!DRBD_MD_TYPE_FLEXIBLE.equals(
                                                       defaultMetaDiskIndex)) {
                    index = Integer.valueOf(defaultMetaDiskIndex) - 5;
                    if (index < 0) {
                        index = 0;
                    }
                }

                indeces[0] = DRBD_MD_TYPE_FLEXIBLE;
                for (int i = 1; i < 11; i++) {
                    indeces[i] = Integer.toString(index);
                    index++;
                }

                getBlockDevice().setDefaultValue(DRBD_MD_INDEX_PARAM,
                                                 DRBD_MD_TYPE_FLEXIBLE);
                return indeces;
            }
            return null;
        }

        protected final String getParamDefault(final String param) {
            return getBlockDevice().getDefaultValue(param);
        }

        protected final String getParamPreferred(final String param) {
            return getBlockDevice().getPreferredValue(param);
        }

        protected final boolean checkParamCache(final String param) {
            if (paramCorrectValueMap.get(param) == null) {
                return false;
            }
            return paramCorrectValueMap.get(param).booleanValue();
        }

        protected final Object[] getNetInterfaces(final Info defaultValue,
                                                  final Enumeration e) {
            final List<Object> list = new ArrayList<Object>();

            if (defaultValue != null) {
                list.add(defaultValue);
            }

            while (e.hasMoreElements()) {
                final Info i =
                        (Info) ((DefaultMutableTreeNode) e.nextElement())
                                                             .getUserObject();
                list.add(i);
            }
            return list.toArray(new Object[list.size()]);
        }

        protected final Info[] getAvailableBlockDevicesForMetaDisk(
                                                     final Info defaultValue,
                                                     final String serviceName,
                                                     final Enumeration e) {
            final List<Info> list = new ArrayList<Info>();
            final String savedMetaDisk =
                                      getBlockDevice().getValue(DRBD_MD_PARAM);

            if (defaultValue != null) {
                list.add(defaultValue);
            }

            while (e.hasMoreElements()) {
                final BlockDevInfo bdi =
                    (BlockDevInfo) ((DefaultMutableTreeNode) e.nextElement())
                                                               .getUserObject();
                final BlockDevice bd = bdi.getBlockDevice();
                if (bd.toString().equals(savedMetaDisk)
                    || (!bd.isDrbd()
                        && !bd.isUsedByCRM()
                        && !bd.isMounted())) {
                    list.add(bdi);
                }
            }
            return list.toArray(new Info[list.size()]);
        }

        public final void attach(final boolean testOnly) {
            DRBD.attach(host, drbdResourceInfo.getName(), testOnly);
        }

        public final void detach(final boolean testOnly) {
            DRBD.detach(host, drbdResourceInfo.getName(), testOnly);
        }

        public final void connect(final boolean testOnly) {
            DRBD.connect(host, drbdResourceInfo.getName(), testOnly);
        }

        public final void disconnect(final boolean testOnly) {
            DRBD.disconnect(host, drbdResourceInfo.getName(), testOnly);
        }

        public final void pauseSync(final boolean testOnly) {
            DRBD.pauseSync(host, drbdResourceInfo.getName(), testOnly);
        }

        public final void resumeSync(final boolean testOnly) {
            DRBD.resumeSync(host, drbdResourceInfo.getName(), testOnly);
        }

        public final void drbdUp(final boolean testOnly) {
            DRBD.up(host, drbdResourceInfo.getName(), testOnly);
        }

        /**
         * Sets this drbd block device to the primary state.
         */
        public final void setPrimary(final boolean testOnly) {
            DRBD.setPrimary(host, drbdResourceInfo.getName(), testOnly);
        }

        /**
         * Sets this drbd block device to the secondary state.
         */
        public final void setSecondary(final boolean testOnly) {
            DRBD.setSecondary(host, drbdResourceInfo.getName(), testOnly);
        }

        /**
         * Initializes drbd block device.
         */
        public final void initDrbd(final boolean testOnly) {
            DRBD.initDrbd(host, drbdResourceInfo.getName(), testOnly);
        }

        public final void makeFilesystem(final String filesystem,
                                         final boolean testOnly) {
            DRBD.makeFilesystem(host,
                                getDrbdResourceInfo().getDevice(),
                                filesystem,
                                testOnly);
        }

        public final void forcePrimary(final boolean testOnly) {
            DRBD.forcePrimary(host, drbdResourceInfo.getName(), testOnly);
        }

        public final void invalidateBD(final boolean testOnly) {
            DRBD.invalidate(host, drbdResourceInfo.getName(), testOnly);
        }

        public final void discardData(final boolean testOnly) {
            DRBD.discardData(host, drbdResourceInfo.getName(), testOnly);
        }

        public final void resizeDrbd(final boolean testOnly) {
            DRBD.resize(host, drbdResourceInfo.getName(), testOnly);
        }

        public final JPanel getGraphicalView() {
            if (getBlockDevice().isDrbd()) {
                drbdResourceInfo.getDrbdInfo().setSelectedNode(this);
            }
            return getDrbdGraph().getDrbdInfo().getGraphicalView();
        }

        protected final void setTerminalPanel() {
            if (host != null) {
                Tools.getGUIData().setTerminalPanel(host.getTerminalPanel());
            }
        }

        public final JComponent getInfoPanel() {
            //setTerminalPanel();
            return getInfoPanelBD();
        }

        public final String[] getParametersFromXML() {
            final String[] params = {
                                DRBD_NI_PARAM,
                                DRBD_NI_PORT_PARAM,
                                DRBD_MD_PARAM,
                                DRBD_MD_INDEX_PARAM,
                              };
            return params;
        }

        public final void apply(final boolean testOnly) {
            if (!testOnly) {
                final String[] params = getParametersFromXML();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        applyButton.setEnabled(false);
                    }
                });
                if (getBlockDevice().getMetaDisk() != null) {
                    getBlockDevice().getMetaDisk().removeMetadiskOfBlockDevice(
                                                              getBlockDevice());
                }
                drbdVIPortList.remove(
                               getBlockDevice().getValue(DRBD_NI_PORT_PARAM));

                storeComboBoxValues(params);

                drbdVIPortList.add(
                                getBlockDevice().getValue(DRBD_NI_PORT_PARAM));
                final Object o =
                              paramComboBoxGet(DRBD_MD_PARAM, null).getValue();
                if (Tools.isStringInfoClass(o)) {
                    getBlockDevice().setMetaDisk(null); /* internal */
                } else {
                    final BlockDevice metaDisk =
                                           ((BlockDevInfo) o).getBlockDevice();
                    getBlockDevice().setMetaDisk(metaDisk);
                }
                if (getBlockDevice().getMetaDisk() != null) {
                    getBlockDevice().getMetaDisk().addMetaDiskOfBlockDevice(
                                                             getBlockDevice());
                }
            }
        }

        public final JComponent getInfoPanelBD() {
            if (infoPanel != null) {
                return infoPanel;
            }
            final BlockDevInfo thisClass = this;
            final ButtonCallback buttonCallback = new ButtonCallback() {
                private volatile boolean mouseStillOver = false;
                public final void mouseOut() {
                    mouseStillOver = false;
                    final DrbdGraph drbdGraph = getDrbdGraph();
                    drbdGraph.stopTestAnimation(applyButton);
                    applyButton.setToolTipText(null);
                }

                public final void mouseOver() {
                    mouseStillOver = true;
                    applyButton.setToolTipText(
                           Tools.getString("ClusterBrowser.StartingDRBDtest"));
                    applyButton.setToolTipBackground(Tools.getDefaultColor(
                                    "ClusterBrowser.Test.Tooltip.Background"));
                    Tools.sleep(250);
                    if (!mouseStillOver) {
                        return;
                    }
                    mouseStillOver = false;
                    final CountDownLatch startTestLatch = new CountDownLatch(1);
                    final DrbdGraph drbdGraph = getDrbdGraph();
                    drbdGraph.startTestAnimation(applyButton, startTestLatch);
                    drbdtestLockAcquire();
                    thisClass.setDRBDtestData(null);
                    apply(true);
                    final Map<Host,String> testOutput =
                                             new LinkedHashMap<Host, String>();
                    try {
                        drbdResourceInfo.getDrbdInfo().createDrbdConfig(true);
                        for (final Host h : host.getCluster().getHostsArray()) {
                            DRBD.adjust(h, "all", true);
                            testOutput.put(h, DRBD.getDRBDtest());
                        }
                    } catch (Exceptions.DrbdConfigException dce) {
                        Tools.appError("config failed");
                    }
                    final DRBDtestData dtd = new DRBDtestData(testOutput);
                    applyButton.setToolTipText(dtd.getToolTip());
                    thisClass.setDRBDtestData(dtd);
                    //clusterStatus.setPtestData(ptestData);
                    drbdtestLockRelease();
                    startTestLatch.countDown();
                }
            };
            initApplyButton(buttonCallback);

            final JPanel mainPanel = new JPanel();
            mainPanel.setBackground(PANEL_BACKGROUND);
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

            final JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.setBackground(STATUS_BACKGROUND);
            buttonPanel.setMinimumSize(new Dimension(0, 50));
            buttonPanel.setPreferredSize(new Dimension(0, 50));
            buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

            final JPanel optionsPanel = new JPanel();
            optionsPanel.setBackground(PANEL_BACKGROUND);
            optionsPanel.setLayout(new BoxLayout(optionsPanel,
                                                 BoxLayout.Y_AXIS));
            optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            extraOptionsPanel.setBackground(PANEL_BACKGROUND);
            extraOptionsPanel.setLayout(new BoxLayout(extraOptionsPanel,
                                                      BoxLayout.Y_AXIS));
            extraOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            /* Actions */
            final JMenuBar mb = new JMenuBar();
            mb.setBackground(PANEL_BACKGROUND);
            final JMenu serviceCombo = getActionsMenu();
            updateMenus(null);
            mb.add(serviceCombo);

            if (getBlockDevice().isDrbd()) {
                buttonPanel.add(mb, BorderLayout.EAST);
                final String[] params = getParametersFromXML();

                addParams(optionsPanel,
                          extraOptionsPanel,
                          params,
                          Tools.getDefaultInt("HostBrowser.DrbdDevLabelWidth"),
                          Tools.getDefaultInt("HostBrowser.DrbdDevFieldWidth"));


                /* apply button */
                applyButton.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(final ActionEvent e) {
                            final Thread thread = new Thread(new Runnable() {
                                public void run() {
                                    apply(false);
                                    try {
                                        drbdResourceInfo.getDrbdInfo()
                                                      .createDrbdConfig(false);
                                    } catch (Exceptions.DrbdConfigException e) {
                                        Tools.appError("config failed");
                                    }
                                }
                            });
                            thread.start();
                        }
                    }
                );
                addApplyButton(buttonPanel);

                applyButton.setEnabled(
                    checkResourceFields(null, params)
                );

                /* expert mode */
                Tools.registerExpertPanel(extraOptionsPanel);

            }

            /* info */
            final Font f = new Font("Monospaced", Font.PLAIN, 12);
            final JPanel riaPanel = new JPanel();
            riaPanel.setBackground(PANEL_BACKGROUND);
            riaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            riaPanel.add(super.getInfoPanel());
            mainPanel.add(riaPanel);

            mainPanel.add(optionsPanel);
            mainPanel.add(extraOptionsPanel);
            final JPanel newPanel = new JPanel();
            newPanel.setBackground(PANEL_BACKGROUND);
            newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
            newPanel.add(buttonPanel);
            newPanel.add(new JScrollPane(mainPanel));
            newPanel.add(Box.createVerticalGlue());
            infoPanel = newPanel;
            return infoPanel;
        }

        public final boolean selectAutomaticallyInTreeMenu() {
            // TODO: dead code?
            return infoPanel == null;
        }

        /**
         * Sets drbd resource for this block device.
         */
        public final void setDrbdResourceInfo(
                                    final DrbdResourceInfo drbdResourceInfo) {
            this.drbdResourceInfo = drbdResourceInfo;
        }

        /**
         * Returns drbd resource info in which this block device is member.
         */
        public final DrbdResourceInfo getDrbdResourceInfo() {
            return drbdResourceInfo;
        }

        /**
         * @return block device resource object.
         */
        public final BlockDevice getBlockDevice() {
            return (BlockDevice) getResource();
        }

        /**
         * Removes this block device from drbd data structures.
         */
        public final void removeFromDrbd() {
            drbdVIPortList.remove(
                                getBlockDevice().getValue(DRBD_NI_PORT_PARAM));
            setDrbd(false);
            setDrbdResourceInfo(null);
        }

        /**
         * Returns short description of the parameter.
         */
        protected final String getParamShortDesc(final String param) {
            return Tools.getString(param);
        }

        /**
         * Returns long description of the parameter.
         */
        protected final String getParamLongDesc(final String param) {
            return Tools.getString(param + ".Long");
        }

        //public void showPopup(JComponent c, int x, int y) {
        //    drbdResourceInfo.getDrbdInfo().setSelectedNode(this);
        //    selectMyself();
        //    if (getBlockDevice().isDrbd()) {
        //        drbdResourceInfo.getDrbdInfo().showPopup(c, x, y);
        //    }
        //}

        /**
         * Returns 'add drbd resource' menu item.
         */
        private MyMenuItem addDrbdResourceMenuItem(final BlockDevInfo oBdi,
                                                   final boolean testOnly) {
            final BlockDevInfo thisClass = this;
            return new MyMenuItem(oBdi.toString()) {
                private static final long serialVersionUID = 1L;
                public void action() {
                    DrbdInfo drbdInfo = getDrbdGraph().getDrbdInfo();
                    setInfoPanel(null);
                    oBdi.setInfoPanel(null);
                    drbdInfo.addDrbdResource(
                                         null,
                                         null,
                                         thisClass,
                                         oBdi,
                                         true,
                                         testOnly);
                }
            };
        }

        /**
         * Creates popup for the block device.
         */
        public final List<UpdatableItem> createPopup() {
            final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
            final BlockDevInfo thisClass = this;
            if (!getBlockDevice().isDrbd() && !getBlockDevice().isAvailable()) {
                /* block devices are not available */
                return null;
            }
            final boolean testOnly = false;
            final MyMenu repMenuItem = new MyMenu(
                        Tools.getString("HostBrowser.Drbd.AddDrbdResource")) {
                private static final long serialVersionUID = 1L;

                public final boolean enablePredicate() {
                    return drbdResourceInfo == null
                           && getHost().isConnected()
                           && getHost().isDrbdLoaded();
                }

                public void update() {
                    super.update();
                    removeAll();
                    Cluster cluster = host.getCluster();
                    Host[] otherHosts = cluster.getHostsArray();
                    for (final Host oHost : otherHosts) {
                        if (oHost == host) {
                            continue;
                        }
                        MyMenu hostMenu = new MyMenu(oHost.getName()) {
                            private static final long serialVersionUID = 1L;

                            public final boolean enablePredicate() {
                                return oHost.isConnected()
                                       && oHost.isDrbdLoaded();
                            }

                            public final void update() {
                                super.update();
                                removeAll();
                                List<BlockDevInfo> blockDevInfos =
                                        oHost.getBrowser().getBlockDevInfos();
                                List<BlockDevInfo> blockDevInfosS =
                                                new ArrayList<BlockDevInfo>();
                                for (final BlockDevInfo oBdi : blockDevInfos) {
                                    if (oBdi.getName().equals(
                                                 getBlockDevice().getName())) {
                                        blockDevInfosS.add(0, oBdi);
                                    } else {
                                        blockDevInfosS.add(oBdi);
                                    }
                                }

                                for (final BlockDevInfo oBdi : blockDevInfosS) {
                                    if (oBdi.getDrbdResourceInfo() == null
                                        && oBdi.getBlockDevice()
                                               .isAvailable()) {
                                        add(addDrbdResourceMenuItem(oBdi,
                                                                    testOnly));
                                    }
                                    if (oBdi.getName().equals(
                                                getBlockDevice().getName())) {
                                        addSeparator();
                                    }
                                }
                            }
                        };
                        hostMenu.update();
                        add(hostMenu);
                    }
                }
            };
            items.add(repMenuItem);
            registerMenuItem(repMenuItem);
            /* attach / detach */
            final MyMenuItem attachMenu =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.Detach"),
                               NO_HARDDISC_ICON,
                               Tools.getString(
                                            "HostBrowser.Drbd.Detach.ToolTip"),

                               Tools.getString("HostBrowser.Drbd.Attach"),
                               HARDDISC_ICON,
                               Tools.getString(
                                        "HostBrowser.Drbd.Attach.ToolTip")) {
                    private static final long serialVersionUID = 1L;

                    public boolean predicate() {
                        return !getBlockDevice().isDrbd()
                               || getBlockDevice().isAttached();
                    }

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return !getBlockDevice().isSyncing();
                    }

                    public void action() {
                        if (this.getText().equals(
                                Tools.getString("HostBrowser.Drbd.Attach"))) {
                            attach(testOnly);
                        } else {
                            detach(testOnly);
                        }
                    }
                };
            final ClusterBrowser.DRBDMenuItemCallback attachItemCallback =
                   getClusterBrowser().new DRBDMenuItemCallback(attachMenu,
                                                                host) {
                public void action(final Host host) {
                    if (isDiskless(false)) {
                        attach(true);
                    } else {
                        detach(true);
                    }
                }
            };
            addMouseOverListener(attachMenu, attachItemCallback);
            items.add(attachMenu);
            registerMenuItem(attachMenu);

            /* connect / disconnect */
            final MyMenuItem connectMenu =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.Disconnect"),
                               null,
                               null,
                               Tools.getString("HostBrowser.Drbd.Connect"),
                               null,
                               null
                              ) {
                    private static final long serialVersionUID = 1L;

                    public boolean predicate() {
                        return isConnectedOrWF(testOnly);
                    }

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return !getBlockDevice().isSyncing()
                            || ((getBlockDevice().isPrimary()
                                && getBlockDevice().isSyncSource())
                                || (getOtherBlockDevInfo().getBlockDevice().
                                                                    isPrimary()
                                    && getBlockDevice().isSyncTarget()));
                    }

                    public void action() {
                        if (this.getText().equals(
                                Tools.getString("HostBrowser.Drbd.Connect"))) {
                            connect(testOnly);
                        } else {
                            disconnect(testOnly);
                        }
                    }
                };
            final ClusterBrowser.DRBDMenuItemCallback connectItemCallback =
                   getClusterBrowser().new DRBDMenuItemCallback(connectMenu,
                                                                host) {
                public void action(final Host host) {
                    if (isConnectedOrWF(false)) {
                        disconnect(true);
                    } else {
                        connect(true);
                    }
                }
            };
            addMouseOverListener(connectMenu, connectItemCallback);
            items.add(connectMenu);
            registerMenuItem(connectMenu);

            /* set primary */
            final MyMenuItem setPrimaryItem =
                new MyMenuItem(Tools.getString(
                                  "HostBrowser.Drbd.SetPrimaryOtherSecondary"),
                               null,
                               null,

                               Tools.getString("HostBrowser.Drbd.SetPrimary"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean predicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return getBlockDevice().isSecondary()
                         && getOtherBlockDevInfo().getBlockDevice().isPrimary();
                    }

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return getBlockDevice().isSecondary();
                    }

                    public void action() {
                        BlockDevInfo oBdi = getOtherBlockDevInfo();
                        if (oBdi != null && oBdi.getBlockDevice().isPrimary()) {
                            oBdi.setSecondary(testOnly);
                        }
                        setPrimary(testOnly);
                    }
                };
            items.add(setPrimaryItem);
            registerMenuItem(setPrimaryItem);

            /* set secondary */
            final MyMenuItem setSecondaryItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetSecondary"),
                               null,
                               Tools.getString(
                                    "HostBrowser.Drbd.SetSecondary.ToolTip")) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return getBlockDevice().isPrimary();
                    }

                    public void action() {
                        setSecondary(testOnly);
                    }
                };
            //enableMenu(setSecondaryItem, false);
            items.add(setSecondaryItem);
            registerMenuItem(setSecondaryItem);

            /* force primary */
            final MyMenuItem forcePrimaryItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.ForcePrimary"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return true;
                    }

                    public void action() {
                        forcePrimary(testOnly);
                    }
                };
            items.add(forcePrimaryItem);
            registerMenuItem(forcePrimaryItem);

            /* invalidate */
            final MyMenuItem invalidateItem =
                new MyMenuItem(
                       Tools.getString("HostBrowser.Drbd.Invalidate"),
                       null,
                       Tools.getString("HostBrowser.Drbd.Invalidate.ToolTip")) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return !getBlockDevice().isSyncing();
                    }

                    public void action() {
                        invalidateBD(testOnly);
                    }
                };
            items.add(invalidateItem);
            registerMenuItem(invalidateItem);

            /* resume / pause sync */
            final MyMenuItem resumeSyncItem =
                new MyMenuItem(
                       Tools.getString("HostBrowser.Drbd.ResumeSync"),
                       null,
                       Tools.getString("HostBrowser.Drbd.ResumeSync.ToolTip"),

                       Tools.getString("HostBrowser.Drbd.PauseSync"),
                       null,
                       Tools.getString("HostBrowser.Drbd.PauseSync.ToolTip")) {
                    private static final long serialVersionUID = 1L;

                    public boolean predicate() {
                        return getBlockDevice().isSyncing()
                               && getBlockDevice().isPausedSync();
                    }

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return getBlockDevice().isSyncing();
                    }

                    public void action() {
                        if (this.getText().equals(Tools.getString(
                                             "HostBrowser.Drbd.ResumeSync"))) {
                            resumeSync(testOnly);
                        } else {
                            pauseSync(testOnly);
                        }
                    }
                };
            items.add(resumeSyncItem);
            registerMenuItem(resumeSyncItem);

            /* resize */
            final MyMenuItem resizeItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.Resize"),
                               null,
                               Tools.getString(
                                          "HostBrowser.Drbd.Resize.ToolTip")) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return !getBlockDevice().isSyncing();
                    }

                    public void action() {
                        resizeDrbd(testOnly);
                    }
                };
            items.add(resizeItem);
            registerMenuItem(resizeItem);

            /* discard my data */
            final MyMenuItem discardDataItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.DiscardData"),
                               null,
                               Tools.getString(
                                     "HostBrowser.Drbd.DiscardData.ToolTip")) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return !getBlockDevice().isSyncing()
                               && !isConnected(testOnly)
                               && !getBlockDevice().isPrimary();
                    }

                    public void action() {
                        discardData(testOnly);
                    }
                };
            items.add(discardDataItem);
            registerMenuItem(discardDataItem);

            /* view log */
            final MyMenuItem viewDrbdLogItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.ViewDrbdLog"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return true;
                    }

                    public void action() {
                        String device = getDrbdResourceInfo().getDevice();
                        drbd.gui.dialog.DrbdLog l =
                                new drbd.gui.dialog.DrbdLog(getHost(), device);
                        l.showDialog();
                    }
                };
            items.add(viewDrbdLogItem);
            registerMenuItem(viewDrbdLogItem);

            return items;
        }

        /**
         * Returns how much of the block device is used.
         */
         public final int getUsed() {
             if (drbdResourceInfo != null) {
                 return drbdResourceInfo.getUsed();
             }
             return getBlockDevice().getUsed();
         }

         /**
          * Returns text that appears above the icon.
          */
        public String getIconTextForGraph(final boolean testOnly) {
            if (!getHost().isConnected()) {
                return Tools.getString("HostBrowser.Drbd.NoInfoAvailable");
            }
            if (getBlockDevice().isDrbd()) {
                return getBlockDevice().getNodeState();
            }
            return null;
        }

        /**
         * Returns text that appears in the corner of the drbd graph.
         */
        protected Subtext getRightCornerTextForDrbdGraph(
                                                      final boolean testOnly) {
             if (getBlockDevice().isDrbdMetaDisk()) {
                 return METADISK_SUBTEXT;
             } else if (getBlockDevice().isSwap()) {
                 return SWAP_SUBTEXT;
             } else if (getBlockDevice().getMountedOn() != null) {
                 return MOUNTED_SUBTEXT;
             } else if (getBlockDevice().isDrbd()) {
                 String s = getBlockDevice().getName();
                 // TODO: cache that
                 if (s.length() > MAX_RIGHT_CORNER_STRING_LENGTH) {
                     s = "..." + s.substring(
                                   s.length()
                                   - MAX_RIGHT_CORNER_STRING_LENGTH + 3,
                                   s.length());
                 }
                 return new Subtext(s, Color.BLUE);
             }
             return null;
        }
        /**
         * Returns whether this device is connected via drbd.
         */
        public final boolean isConnected(final boolean testOnly) {
            final DRBDtestData dtd = getDRBDtestData();
            if (testOnly && dtd != null) {
                return isConnectedTest(dtd)
                       && !isWFConnection(testOnly);
            } else {
                return getBlockDevice().isConnected();
            }
        }

        /**
         * Returns whether this device is connected or wait-for-c via drbd.
         */
        public final boolean isConnectedOrWF(final boolean testOnly) {
            final DRBDtestData dtd = getDRBDtestData();
            if (testOnly && dtd != null) {
                return isConnectedTest(dtd);
            } else {
                return getBlockDevice().isConnectedOrWF();
            }
        }

        /**
         * Returns whether this device is in wait-for-connection state.
         */
        public final boolean isWFConnection(final boolean testOnly) {
            final DRBDtestData dtd = getDRBDtestData();
            if (testOnly && dtd != null) {
                return isConnectedOrWF(testOnly)
                       && isConnectedTest(dtd)
                       && !getOtherBlockDevInfo().isConnectedTest(dtd);
            } else {
                return getBlockDevice().isWFConnection();
            }
        }

        /**
         * Returns whether this device will be disconnected.
         */
        public final boolean isConnectedTest(final DRBDtestData dtd) {
            return dtd.isConnected(host, drbdResourceInfo.getDevice())
                   || (!dtd.isDisconnected(host, drbdResourceInfo.getDevice())
                       && getBlockDevice().isConnectedOrWF());
        }

        /**
         * Returns whether this device is diskless.
         */
        public final boolean isDiskless(final boolean testOnly) {
            final DRBDtestData dtd = getDRBDtestData();
            final DrbdResourceInfo dri = drbdResourceInfo;
            if (testOnly && dtd != null && dri != null) {
                return dtd.isDiskless(host, drbdResourceInfo.getDevice())
                       || (!dtd.isAttached(host, drbdResourceInfo.getDevice())
                           && getBlockDevice().isDiskless());
            } else {
                return getBlockDevice().isDiskless();
            }
        }

        /**
         * Returns drbd test data.
         */
        public final DRBDtestData getDRBDtestData() {
            final ClusterBrowser b = getClusterBrowser();
            if (b == null) {
                return null;
            }
            return b.getDRBDtestData();
        }

        /**
         * Sets drbd test data.
         */
        public final void setDRBDtestData(final DRBDtestData drbdtestData) {
            final ClusterBrowser b = getClusterBrowser();
            if (b == null) {
                return;
            }
            b.setDRBDtestData(drbdtestData);
        }
    }
}
