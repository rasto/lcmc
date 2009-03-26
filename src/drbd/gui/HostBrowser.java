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
import drbd.gui.ClusterBrowser.DrbdInfo;
import drbd.gui.ClusterBrowser.DrbdResourceInfo;
import drbd.AddDrbdUpgradeDialog;
import drbd.utilities.MyButton;
import drbd.utilities.ExecCallback;
import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Heartbeat;

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
    private static final ImageIcon HOST_ICON_LARGE = Tools.createImageIcon(
                                  Tools.getDefault("HostBrowser.HostIcon"));
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

    /**
     * Prepares a new <code>HostBrowser</code> object.
     */
    //public HostBrowser() {
    //    super();
    //}

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
        setTreeTop(hostInfo);
    }

    /**
     * Returns host info for this browser.
     */
    public final HostInfo getHostInfo() {
        return hostInfo;
    }

    /**
     * Initializes host resources for host view.
     */
    public final void initHostResources() {
        /* net interfaces */
        netInterfacesNode = new DefaultMutableTreeNode(new CategoryInfo(Tools.getString("HostBrowser.NetInterfaces")));
        setNode(netInterfacesNode);
        topAdd(netInterfacesNode);

        /* block devices */
        blockDevicesNode = new DefaultMutableTreeNode(new CategoryInfo(Tools.getString("HostBrowser.BlockDevices")));
        setNode(blockDevicesNode);
        topAdd(blockDevicesNode);

        /* file systems */
        fileSystemsNode = new DefaultMutableTreeNode(new CategoryInfo(Tools.getString("HostBrowser.FileSystems")));
        setNode(fileSystemsNode);
        topAdd(fileSystemsNode);
    }

    /**
     * Updates hardware resources of a host in the tree.
     *
     * @param ni
     *          array of network interfaces
     * @param bd
     *          array of block devices
     * @param fs
     *          array of filesystems
     */
    public final void updateHWResources(final NetInterface[] ni,
                                  final BlockDevice[] bd,
                                  final String[] fs) {
        DefaultMutableTreeNode resource = null;

        /* net interfaces */
        netInterfacesNode.removeAllChildren();
        for (int i = 0; i < ni.length; i++) {
            resource = new DefaultMutableTreeNode(new NetInfo(ni[i].getName(), ni[i]));
            setNode(resource);
            netInterfacesNode.add(resource);
        }
        reload(netInterfacesNode);

        /* block devices */
        blockDevicesNode.removeAllChildren();
        for (int i = 0; i < bd.length; i++) {
            resource = new DefaultMutableTreeNode(new BlockDevInfo(bd[i].getName(), bd[i]));
            setNode(resource);
            blockDevicesNode.add(resource);
        }
        reload(blockDevicesNode);

        /* file systems */
        fileSystemsNode.removeAllChildren();
        for (int i = 0; i < fs.length; i++) {
            resource = new DefaultMutableTreeNode(new FilesystemInfo(fs[i]));
            setNode(resource);
            fileSystemsNode.add(resource);
        }
        reload(fileSystemsNode);
    }

    /**
     * @return net interfaces tree node.
     */
    public final DefaultMutableTreeNode getNetInterfacesNode() {
        return netInterfacesNode;
    }

    /**
     * @return list of block devices.
     */
    public final List<BlockDevInfo> getBlockDevInfos() {
        final List<BlockDevInfo> blockDevInfos = new ArrayList<BlockDevInfo>();
        final Enumeration e = blockDevicesNode.children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode bdNode = (DefaultMutableTreeNode) e.nextElement();
            final BlockDevInfo bdi = (BlockDevInfo) bdNode.getUserObject();
            blockDevInfos.add(bdi);
        }
        return blockDevInfos;
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
        public ImageIcon getMenuIcon() {
            return FS_ICON;
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
                        modinfo = Tools.execCommand(host, "/sbin/modinfo " + getName(), null, false);
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
        /** Graph that will be shown by this host. It's eigher heartbeat or
         * drbd graph. */
        private ResourceGraph graph = null;
        /**
         * Prepares a new <code>HostInfo</code> object.
         */
        HostInfo() {
            super(host.getName());
        }

        /**
         * Returns a host icon for the menu.
         */
        public final ImageIcon getMenuIcon() {
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
        public final String getToolTipForGraph() {
            final StringBuffer tt = new StringBuffer(80);
            tt.append("<b>" + host.getName() + "</b>");
            if (host.getCluster().getBrowser().isRealDcHost(host)) {
                tt.append(" (DC)");
            }
            if (!host.isDrbdStatus() && !host.isHbStatus()) {
                tt.append('\n');
                tt.append(Tools.getString("ClusterBrowser.Host.Offline"));
            }
            return tt.toString();
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

            final ExecCallback callback =
                new ExecCallback() {
                    public void done(final String ans) {
                        ta.setText(ans);
                    }

                    public void doneError(final String ans, final int exitCode) {
                        ta.setText("error");
                        Tools.sshError(host, "", ans, exitCode);
                    }

                };
            // TODO: disable buttons if disconnected?
            final MyButton procDrbdButton = new MyButton("/proc/drbd");
            procDrbdButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    host.execCommand("DRBD.getProcDrbd", callback, true);
                }
            });
            host.registerEnableOnConnect(procDrbdButton);

            final MyButton drbdProcsButton = new MyButton("DRBD Processes");
            drbdProcsButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    host.execCommand("DRBD.getProcesses", callback, true);
                }
            });
            host.registerEnableOnConnect(drbdProcsButton);

            final MyButton crmMonButton = new MyButton("crm_mon");
            crmMonButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    host.execCommand("Heartbeat.getCrmMon", callback, true);
                }
            });
            host.registerEnableOnConnect(crmMonButton);

            final MyButton hbProcsButton = new MyButton("Heartbeat Processes");
            hbProcsButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    host.execCommand("Heartbeat.getProcesses", callback, true);
                }
            });
            host.registerEnableOnConnect(hbProcsButton);

            final JPanel mainPanel = new JPanel();
            mainPanel.setBackground(PANEL_BACKGROUND);
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

            final JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.setBackground(Tools.getDefaultColor("ViewPanel.Status.Background"));
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
            p.setBackground(PANEL_BACKGROUND);

            p.add(procDrbdButton);
            p.add(drbdProcsButton);
            p.add(crmMonButton);
            p.add(hbProcsButton);
            SpringUtilities.makeCompactGrid(p, 2, 2,  // rows, cols
                                               1, 1,  // initX, initY
                                               1, 1); // xPad, yPad
            //JPanel panel = new JPanel();
            mainPanel.setMinimumSize(new Dimension(
                        Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                        Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")
                        ));
            mainPanel.setPreferredSize(new Dimension(
                        Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                        Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")
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
                        DRBD.load(getHost());
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
                                && !bdi.getBlockDevice().isConnected()
                                && !bdi.getBlockDevice().isAttached()) {
                                bdi.drbdUp();
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
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.ChangeHostColor"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return true;
                    }

                    public void action() {
                        final JColorChooser tcc = new JColorChooser();
                        Color newColor = tcc.showDialog(Tools.getGUIData().getMainFrame(), "asdf", host.getColor());
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
                        drbd.gui.dialog.DrbdsLog l = new drbd.gui.dialog.DrbdsLog(host);
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
                            if (bdi.getBlockDevice().isDrbd() && !bdi.getBlockDevice().isConnectedOrWF()) {
                                bdi.connect();
                            }
                        }
                    }
                };
            items.add(connectAllItem);
            registerMenuItem(connectAllItem);

            /* disconnect all */
            final MyMenuItem disconnectAllItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.DisconnectAll"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isDrbdStatus();
                    }

                    public void action() {
                        for (final BlockDevInfo bdi : getBlockDevInfos()) {
                            if (bdi.getBlockDevice().isDrbd() && bdi.getBlockDevice().isConnectedOrWF()) {
                                bdi.disconnect();
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
                            if (bdi.getBlockDevice().isDrbd() && !bdi.getBlockDevice().isAttached()) {
                                bdi.attach();
                            }
                        }
                    }
                };
            items.add(attachAllItem);
            registerMenuItem(attachAllItem);

            /* set all primary */
            final MyMenuItem setAllPrimaryItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetAllPrimary"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isDrbdStatus();
                    }

                    public void action() {
                        for (final BlockDevInfo bdi : getBlockDevInfos()) {
                            if (bdi.getBlockDevice().isDrbd() && bdi.getBlockDevice().isSecondary()) {
                                bdi.setPrimary();
                            }
                        }
                    }
                };
            items.add(setAllPrimaryItem);
            registerMenuItem(setAllPrimaryItem);

            /* set all secondary */
            final MyMenuItem setAllSecondaryItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetAllSecondary"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isDrbdStatus();
                    }

                    public void action() {
                        for (final BlockDevInfo bdi : getBlockDevInfos()) {
                            if (bdi.getBlockDevice().isDrbd() && bdi.getBlockDevice().isPrimary()) {
                                bdi.setSecondary();
                            }
                        }
                    }
                };
            items.add(setAllSecondaryItem);
            registerMenuItem(setAllSecondaryItem);

            /* heartbeat standby on */
            final MyMenuItem standByOnItem =
                new MyMenuItem(Tools.getString("HostBrowser.Heartbeat.StandByOn"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isHbStatus();
                    }

                    public void action() {
                        Heartbeat.standByOn(host);
                    }
                };
            items.add(standByOnItem);
            registerMenuItem(standByOnItem);

            /* heartbeat standby off */
            final MyMenuItem standByOffItem =
                new MyMenuItem(Tools.getString("HostBrowser.Heartbeat.StandByOff"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getHost().isHbStatus();
                    }

                    public void action() {
                        Heartbeat.standByOff(host);
                    }
                };
            registerMenuItem(standByOffItem);
            items.add(standByOffItem);

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

            return items;
        }

        /**
         * Sets the graphical view for this host.
         */
        public final void setGraph(final ResourceGraph graph) {
            this.graph = graph;
        }

        /**
         * Returns grahical view if there is any.
         */
        public final JPanel getGraphicalView() {
            if (graph == null) {
                return null;
            }
            return graph.getGraphPanel();
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
                    String text = Tools.execCommand(host, "/sbin/ifconfig " + getName(), null, false);
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
        public ImageIcon getMenuIcon() {
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
        /** drbd resource in which this block device is member. */
        private DrbdResourceInfo drbdResourceInfo;
        /** map from paremeters to the fact if the last entered value was
         * correct. */
        private final Map<String, Boolean> paramCorrectValueMap = new HashMap<String, Boolean>();
        /** cache for the info panel. */
        private JComponent infoPanel = null;

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

            initApplyButton();
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
        public final void removeMyself() {
            getBlockDevice().setValue(DRBD_NI_PARAM, null);
            getBlockDevice().setValue(DRBD_NI_PORT_PARAM, null);
            getBlockDevice().setValue(DRBD_MD_PARAM, null);
            getBlockDevice().setValue(DRBD_MD_INDEX_PARAM, null);
            super.removeMyself();
            infoPanel = null;
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
        public final ImageIcon getMenuIcon() {
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
            ret.append(" blocks\n");
            if (getBlockDevice().getMountedOn() == null) {
                ret.append("not mounted\n");
            } else {
                ret.append("Mounted on      : ");
                ret.append(getBlockDevice().getMountedOn());
                ret.append("\nType            : ");
                ret.append(getBlockDevice().getFsType());
            }
            if (getBlockDevice().isDrbd()) {
                ret.append("Connection state: ");
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
        public final String getToolTipForGraph() {
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
                tt.append("(Meta Disk)\n");
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
                    tt.append(Tools.getString("HostBrowser.NoInfoAvailable"));
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
                throw new Exceptions.DrbdConfigException("Drbd device not defined for host " + host.getName() + " (" + resource + ")");
            }
            if (getBlockDevice().getDrbdNetInterfaceWithPort() == null) {
                throw new Exceptions.DrbdConfigException("Net interface not defined for host " + host.getName() + " (" + resource + ")");
            }
            if (getBlockDevice().getName() == null) {
                throw new Exceptions.DrbdConfigException("Block device not defined for host " + host.getName() + " (" + resource + ")");
            }

            final StringBuffer config = new StringBuffer(120);
            config.append("\ton ");
            config.append(host.getName());
            config.append(" {\n\t\tdevice\t");
            config.append(drbdDevice);
            config.append(";\n\t\tdisk\t");
            config.append(getBlockDevice().getName());
            config.append(";\n\t\taddress\t");
            config.append(getBlockDevice().getDrbdNetInterfaceWithPort());
            config.append(";\n\t\t");
            config.append(getBlockDevice().getMetaDiskString());
            config.append(";\n\t}\n");
            return config.toString();
        }

        public final void selectMyself() {
            nodeChanged(getNode());
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
            int port = Tools.getDefaultInt("HostBrowser.DrbdNetInterfacePort") - 1;
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
                int index = Tools.getDefaultInt("HostBrowser.DrbdNetInterfacePort");
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
                gcb = new GuiComboBox(defaultPort,
                                      drbdVIPorts.toArray(new String[drbdVIPorts.size()]), null, regexp, width);
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
                final GuiComboBox ind = paramComboBoxGet(DRBD_MD_INDEX_PARAM, null);
                final GuiComboBox indW = paramComboBoxGet(DRBD_MD_INDEX_PARAM, "wizard");
                if (internal) {
                    ind.setValue(DRBD_MD_TYPE_FLEXIBLE);
                    if (indW != null) {
                        indW.setValue(DRBD_MD_TYPE_FLEXIBLE);
                    }
                }
                ind.setEnabled(!internal);
                if (indW != null) {
                    indW.setEnabled(!internal);
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
                String netInterfaceString = getBlockDevice().getValue(DRBD_NI_PARAM);
                if (netInterfaceString == null || netInterfaceString.equals("")) {
                    defaultNetInterface = new StringInfo(Tools.getString("HostBrowser.DrbdNetInterface.Select"), null);
                    netInterfaceString = defaultNetInterface.toString();
                    getBlockDevice().setDefaultValue(DRBD_NI_PARAM, netInterfaceString);
                }
                return getNetInterfaces(defaultNetInterface, netInterfacesNode.children());
            } else if (DRBD_MD_PARAM.equals(param)) {
                /* meta disk */
                final StringInfo internalMetaDisk = new StringInfo(Tools.getString("HostBrowser.MetaDisk.Internal"), "internal");
                final String defaultMetaDiskString = internalMetaDisk.getStringValue();

                final Info[] blockDevices = getAvailableBlockDevicesForMetaDisk(internalMetaDisk, getName(), blockDevicesNode.children());

                getBlockDevice().setDefaultValue(DRBD_MD_PARAM, defaultMetaDiskString);
                return blockDevices;
            } else if (DRBD_MD_INDEX_PARAM.equals(param)) {

                String defaultMetaDiskIndex = getBlockDevice().getValue(DRBD_MD_INDEX_PARAM);

                String[] indeces = new String[11];
                int index = 0;
                if (defaultMetaDiskIndex == null) {
                    defaultMetaDiskIndex = DRBD_MD_TYPE_FLEXIBLE;
                } else if (!DRBD_MD_TYPE_FLEXIBLE.equals(defaultMetaDiskIndex)) {
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

        protected final boolean checkParamCache(final String param) {
            if (paramCorrectValueMap.get(param) == null) {
                return false;
            }
            return paramCorrectValueMap.get(param).booleanValue();
        }

        protected final Object[] getNetInterfaces(final Info defaultValue, final Enumeration e) {
            final List<Object> list = new ArrayList<Object>();

            if (defaultValue != null) {
                list.add(defaultValue);
            }

            while (e.hasMoreElements()) {
                final Info i = (Info) ((DefaultMutableTreeNode) e.nextElement()).getUserObject();
                list.add(i);
            }
            return list.toArray(new Object[list.size()]);
        }

        protected final Info[] getAvailableBlockDevicesForMetaDisk(final Info defaultValue,
                                                             final String serviceName,
                                                             final Enumeration e) {
            final List<Info> list = new ArrayList<Info>();
            final String savedMetaDisk = getBlockDevice().getValue(DRBD_MD_PARAM);

            if (defaultValue != null) {
                list.add(defaultValue);
            }

            while (e.hasMoreElements()) {
                final BlockDevInfo bdi = (BlockDevInfo) ((DefaultMutableTreeNode) e.nextElement()).getUserObject();
                final BlockDevice bd = bdi.getBlockDevice();
                if (bd.toString().equals(savedMetaDisk)
                    || (!bd.isDrbdMetaDisk() && !bd.isDrbd() && !bd.isUsedByHeartbeat() && !bd.isMounted())) {
                    list.add(bdi);
                }
            }
            return list.toArray(new Info[list.size()]);
        }

        public final void attach() {
            DRBD.attach(host, drbdResourceInfo.getName());
        }

        public final void detach() {
            DRBD.detach(host, drbdResourceInfo.getName());
        }

        public final void connect() {
            DRBD.connect(host, drbdResourceInfo.getName());
        }

        public final void disconnect() {
            DRBD.disconnect(host, drbdResourceInfo.getName());
        }

        public final void pauseSync() {
            DRBD.pauseSync(host, drbdResourceInfo.getName());
        }

        public final void resumeSync() {
            DRBD.resumeSync(host, drbdResourceInfo.getName());
        }

        public final void drbdUp() {
            DRBD.up(host, drbdResourceInfo.getName());
        }

        /**
         * Sets this drbd block device to the primary state.
         */
        public final void setPrimary() {
            DRBD.setPrimary(host, drbdResourceInfo.getName());
        }

        /**
         * Sets this drbd block device to the secondary state.
         */
        public final void setSecondary() {
            DRBD.setSecondary(host, drbdResourceInfo.getName());
        }

        /**
         * Initializes drbd block device.
         */
        public final void initDrbd() {
            DRBD.initDrbd(host, drbdResourceInfo.getName());
        }

        public final void makeFilesystem(final String filesystem) {
            DRBD.makeFilesystem(host, getDrbdResourceInfo().getDevice(), filesystem);
        }

        public final void forcePrimary() {
            DRBD.forcePrimary(host, drbdResourceInfo.getName());
        }

        public final void invalidateBD() {
            DRBD.invalidate(host, drbdResourceInfo.getName());
        }

        public final void discardData() {
            DRBD.discardData(host, drbdResourceInfo.getName());
        }

        public final void resizeDrbd() {
            DRBD.resize(host, drbdResourceInfo.getName());
        }

        public final JPanel getGraphicalView() {
            if (getBlockDevice().isDrbd()) {
                drbdResourceInfo.getDrbdInfo().setSelectedNode(this);
                return drbdResourceInfo.getDrbdInfo().getGraphicalView();
            }
            return null;
        }

        protected final void setTerminalPanel() {
            if (host != null) {
                Tools.getGUIData().setTerminalPanel(host.getTerminalPanel());
            }
        }

        public final JComponent getInfoPanel() {
            setTerminalPanel();
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

        public final void apply() {
            final String[] params = getParametersFromXML();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    applyButton.setEnabled(false);
                }
            });
            if (getBlockDevice().getMetaDisk() != null) {
                getBlockDevice().getMetaDisk().setIsDrbdMetaDisk(false);
            }
            drbdVIPortList.remove(getBlockDevice().getValue(DRBD_NI_PORT_PARAM));

            storeComboBoxValues(params);

            drbdVIPortList.add(getBlockDevice().getValue(DRBD_NI_PORT_PARAM));
            final Object o = paramComboBoxGet(DRBD_MD_PARAM, null).getValue();
            if (Tools.isStringInfoClass(o)) {
                getBlockDevice().setMetaDisk(null); /* internal */
            } else {
                final BlockDevice metaDisk = ((BlockDevInfo) o).getBlockDevice();
                getBlockDevice().setMetaDisk(metaDisk);
            }
            if (getBlockDevice().getMetaDisk() != null) {
                getBlockDevice().getMetaDisk().setIsDrbdMetaDisk(true);
            }
        }

        public final JComponent getInfoPanelBD() {
            if (infoPanel != null) {
                return infoPanel;
            }

            final JPanel mainPanel = new JPanel();
            mainPanel.setBackground(PANEL_BACKGROUND);
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

            final JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.setBackground(Tools.getDefaultColor("ViewPanel.Status.Background"));
            buttonPanel.setMinimumSize(new Dimension(0, 50));
            buttonPanel.setPreferredSize(new Dimension(0, 50));
            buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

            final JPanel optionsPanel = new JPanel();
            optionsPanel.setBackground(PANEL_BACKGROUND);
            optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
            optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            final JPanel extraOptionsPanel = new JPanel();
            extraOptionsPanel.setBackground(PANEL_BACKGROUND);
            extraOptionsPanel.setLayout(new BoxLayout(extraOptionsPanel, BoxLayout.Y_AXIS));
            extraOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            if (getBlockDevice().isDrbd()) {
                /* expert mode */
                buttonPanel.add(Tools.expertModeButton(extraOptionsPanel), BorderLayout.WEST);
            }

            /* Actions */
            final JMenuBar mb = new JMenuBar();
            mb.setBackground(PANEL_BACKGROUND);
            final JMenu serviceCombo = getActionsMenu();
            updateMenus(null);
            mb.add(serviceCombo);
            buttonPanel.add(mb, BorderLayout.EAST);


            if (getBlockDevice().isDrbd()) {
                final String[] params = getParametersFromXML();

                addParams(optionsPanel,
                          extraOptionsPanel,
                          params,
                          Tools.getDefaultInt("HostBrowser.DrbdDevLabelWidth"),
                          Tools.getDefaultInt("HostBrowser.DrbdDevFieldWidth"));


                /* apply button */
                addApplyButton(mainPanel);

                applyButton.setEnabled(
                    checkResourceFields(null, params)
                );

                applyButton.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(final ActionEvent e) {
                            Thread thread = new Thread(new Runnable() {
                                public void run() {
                                    apply();
                                    try {
                                        drbdResourceInfo.getDrbdInfo().createDrbdConfig();
                                    } catch (Exceptions.DrbdConfigException dce) {
                                        Tools.appError("config failed");
                                    }
                                }
                            });
                        }
                    }
                );
            }

            /* info */
            final Font f = new Font("Monospaced", Font.PLAIN, 12);
            final JEditorPane resourceInfoArea = new JEditorPane("text/plain", getInfo());
            resourceInfoArea.setEditable(false);
            resourceInfoArea.setFont(f);
            resourceInfoArea.setPreferredSize(new Dimension(
                        Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                        Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")
                        ));
            resourceInfoArea.setMaximumSize(resourceInfoArea.getPreferredSize());
            final JPanel riaPanel = new JPanel();
            riaPanel.setBackground(PANEL_BACKGROUND);
            riaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            riaPanel.add(resourceInfoArea);
            mainPanel.add(riaPanel);

            ///* drbd node */
            //if (getBlockDevice().isDrbd()) {
            //    /* expert mode */
            //    mainPanel.add(Tools.expertModeButton(extraOptionsPanel));

            //}
            mainPanel.add(optionsPanel);
            mainPanel.add(extraOptionsPanel);
            infoPanel = new JPanel();
            infoPanel.setBackground(PANEL_BACKGROUND);
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.add(buttonPanel);
            infoPanel.add(new JScrollPane(mainPanel));
            infoPanel.add(Box.createVerticalGlue());
            return infoPanel;
        }

        public final boolean selectAutomaticallyInTreeMenu() {
            // TODO: dead code?
            return infoPanel == null;
        }

        /**
         * Sets drbd resource for this block device.
         */
        public final void setDrbdResourceInfo(final DrbdResourceInfo drbdResourceInfo) {
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
            drbdVIPortList.remove(getBlockDevice().getValue(DRBD_NI_PORT_PARAM));
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
         * Creates popup for the block device.
         */
        public final List<UpdatableItem> createPopup() {
            final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
            final BlockDevInfo thisClass = this;
            if (!getBlockDevice().isAvailable()) {
                /* block devices are not available */
                return null;
            }
            final MyMenu repMenuItem = new MyMenu(Tools.getString("HostBrowser.Drbd.AddDrbdResource")) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return (drbdResourceInfo == null);
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

                            public void update() {
                                super.update();
                                removeAll();
                                List<BlockDevInfo> blockDevInfos = oHost.getBrowser().getBlockDevInfos();
                                List<BlockDevInfo> blockDevInfosS = new ArrayList<BlockDevInfo>();
                                for (final BlockDevInfo otherBdi : blockDevInfos) {
                                    if (otherBdi.getName().equals(getBlockDevice().getName())) {
                                        blockDevInfosS.add(0, otherBdi);
                                    } else {
                                        blockDevInfosS.add(otherBdi);
                                    }
                                }

                                for (final BlockDevInfo otherBdi : blockDevInfosS) {
                                    if (otherBdi.getDrbdResourceInfo() == null && otherBdi.getBlockDevice().isAvailable()) {
                                        add(new MyMenuItem(otherBdi.toString()) {
                                            private static final long serialVersionUID = 1L;
                                            public void action() {
                                                DrbdInfo drbdInfo = ((DrbdGraph) host.getCluster().getBrowser().getDrbdGraph()).getDrbdInfo();
                                                setInfoPanel(null);
                                                otherBdi.setInfoPanel(null);
                                                drbdInfo.addDrbdResource(null, null, thisClass, otherBdi, true);
                                                //drbdResourceInfo.getDrbdInfo().addDrbdResource(null, null, thisClass, otherBdi, true);
                                                //SwingUtilities.invokeLater(new Runnable() { public void run() {
                                                //    scale();
                                                //}});
                                            }
                                        });
                                    }
                                    if (otherBdi.getName().equals(getBlockDevice().getName())) {
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
                               HARDDISC_ICON,
                               Tools.getString("HostBrowser.Drbd.Detach.ToolTip"),

                               Tools.getString("HostBrowser.Drbd.Attach"),
                               NO_HARDDISC_ICON,
                               Tools.getString("HostBrowser.Drbd.Attach.ToolTip")
                              ) {
                    private static final long serialVersionUID = 1L;

                    public boolean predicate() {
                        return !getBlockDevice().isDrbd() || getBlockDevice().isAttached();
                    }

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return !getBlockDevice().isSyncing();
                    }

                    public void action() {
                        if (this.getText().equals(Tools.getString("HostBrowser.Drbd.Attach"))) {
                            attach();
                        } else {
                            detach();
                        }
                    }
                };
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
                        return getBlockDevice().isConnectedOrWF();
                    }

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return !getBlockDevice().isSyncing()
                            || ((getBlockDevice().isPrimary()
                                && getBlockDevice().isSyncSource())
                                || (getOtherBlockDevInfo().getBlockDevice().isPrimary()
                                    && getBlockDevice().isSyncTarget()));
                    }

                    public void action() {
                        if (this.getText().equals(Tools.getString("HostBrowser.Drbd.Connect"))) {
                            connect();
                        } else {
                            disconnect();
                        }
                    }
                };
            items.add(connectMenu);
            registerMenuItem(connectMenu);

            /* set primary */
            final MyMenuItem setPrimaryItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetPrimaryOtherSecondary"),
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
                            oBdi.setSecondary();
                        }
                        setPrimary();
                    }
                };
            items.add(setPrimaryItem);
            registerMenuItem(setPrimaryItem);

            /* set secondary */
            final MyMenuItem setSecondaryItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.SetSecondary"),
                               null,
                               Tools.getString("HostBrowser.Drbd.SetSecondary.ToolTip")) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return getBlockDevice().isPrimary();
                    }

                    public void action() {
                        setSecondary();
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
                        forcePrimary();
                    }
                };
            items.add(forcePrimaryItem);
            registerMenuItem(forcePrimaryItem);

            /* invalidate */
            final MyMenuItem invalidateItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.Invalidate"),
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
                        invalidateBD();
                    }
                };
            items.add(invalidateItem);
            registerMenuItem(invalidateItem);

            /* resume / pause sync */
            final MyMenuItem resumeSyncItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.ResumeSync"),
                               null,
                               Tools.getString("HostBrowser.Drbd.ResumeSync.ToolTip"),

                               Tools.getString("HostBrowser.Drbd.PauseSync"),
                               null,
                               Tools.getString("HostBrowser.Drbd.PauseSync.ToolTip")) {
                    private static final long serialVersionUID = 1L;

                    public boolean predicate() {
                        return getBlockDevice().isSyncing() && getBlockDevice().isPausedSync();
                    }

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return getBlockDevice().isSyncing();
                    }

                    public void action() {
                        if (this.getText().equals(Tools.getString("HostBrowser.Drbd.ResumeSync"))) {
                            resumeSync();
                        } else {
                            pauseSync();
                        }
                    }
                };
            items.add(resumeSyncItem);
            registerMenuItem(resumeSyncItem);

            /* resize */
            final MyMenuItem resizeItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.Resize"),
                               null,
                               Tools.getString("HostBrowser.Drbd.Resize.ToolTip")) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return !getBlockDevice().isSyncing();
                    }

                    public void action() {
                        resizeDrbd();
                    }
                };
            items.add(resizeItem);
            registerMenuItem(resizeItem);

            /* discard my data */
            final MyMenuItem discardDataItem =
                new MyMenuItem(Tools.getString("HostBrowser.Drbd.DiscardData"),
                               null,
                               Tools.getString("HostBrowser.Drbd.DiscardData.ToolTip")) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        if (!getBlockDevice().isDrbd()) {
                            return false;
                        }
                        return !getBlockDevice().isSyncing()
                               && !getBlockDevice().isConnected()
                               && !getBlockDevice().isPrimary();
                    }

                    public void action() {
                        discardData();
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
                        drbd.gui.dialog.DrbdLog l = new drbd.gui.dialog.DrbdLog(getHost(), device);
                        l.showDialog();
                    }
                };
            items.add(viewDrbdLogItem);
            registerMenuItem(viewDrbdLogItem);

            return items;
        }
    }
}
