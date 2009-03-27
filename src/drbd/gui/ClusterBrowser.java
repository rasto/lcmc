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

import drbd.AddDrbdConfigDialog;
import drbd.AddDrbdSplitBrainDialog;
import drbd.AddHostDialog;

import drbd.utilities.Tools;
import drbd.utilities.Unit;
import drbd.utilities.DRBD;
import drbd.utilities.MyButton;
import drbd.utilities.UpdatableItem;
import drbd.Exceptions;
import drbd.gui.dialog.ClusterLogs;
import drbd.gui.dialog.ServiceLogs;
import drbd.gui.dialog.ClusterDrbdLogs;

import drbd.data.Host;
import drbd.data.Cluster;
import drbd.data.HeartbeatStatus;
import drbd.data.HeartbeatOCF;
import drbd.data.DrbdXML;
import drbd.utilities.NewOutputCallback;

import drbd.utilities.ExecCallback;
import drbd.utilities.Heartbeat;
import drbd.data.resources.Resource;
import drbd.data.resources.Service;
import drbd.data.resources.CommonBlockDevice;
import drbd.data.resources.BlockDevice;
import drbd.data.resources.Network;
import drbd.data.resources.DrbdResource;
import drbd.data.HeartbeatService;
import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;

import drbd.gui.HostBrowser.BlockDevInfo;
import drbd.gui.HostBrowser.HostInfo;

import java.awt.Color;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.SpringLayout;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import org.apache.commons.collections.map.MultiKeyMap;

/**
 * This class holds cluster resource data in a tree. It shows panels that allow
 * to edit data of services etc.
 * Every resource has its Info object, that accessible through the tree view.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class ClusterBrowser extends Browser {
    /**
     * Cluster object that holds data of the cluster. (One Browser belongs to
     * one cluster).
     */
    private final Cluster cluster;
    /** Menu's all hosts node. */
    private DefaultMutableTreeNode allHostsNode;
    /** Menu's all hosts in the cluster node. */
    private DefaultMutableTreeNode clusterHostsNode;
    /** Menu's networks node. */
    private DefaultMutableTreeNode networksNode;
    /** Menu's common block devices node. */
    private DefaultMutableTreeNode commonBlockDevicesNode;
    /** Menu's available heartbeat services node. */
    private DefaultMutableTreeNode availableServicesNode;
    /** Menu's heartbeat services node. */
    private DefaultMutableTreeNode servicesNode;
    /** Menu's heartbeat scores node. */
    private DefaultMutableTreeNode scoresNode;
    /** Menu's drbd node. */
    private DefaultMutableTreeNode drbdNode;
    /** Common file systems on all cluster nodes. */
    private String[] commonFileSystems;
    /** Common mount points on all cluster nodes. */
    private String[] commonMountPoints;

    /** name (hb type) + id to service info hash. */
    private final Map<String, Map<String, ServiceInfo>> nameToServiceInfoHash =
                                new HashMap<String, Map<String, ServiceInfo>>();
    /** drbd resource name string to drbd resource info hash. */
    private final Map<String, DrbdResourceInfo> drbdResHash                  =
                                new HashMap<String, DrbdResourceInfo>();
    /** drbd resource device string to drbd resource info hash. */
    private final Map<String, DrbdResourceInfo> drbdDevHash                  =
                                new HashMap<String, DrbdResourceInfo>();
    /** Heartbeat id to service info hash. */
    private final Map<String, ServiceInfo> heartbeatIdToServiceInfo =
                                          new HashMap<String, ServiceInfo>();
    /** Score to its info panel hash. */
    private final Map<String, HostScoreInfo> hostScoreInfoMap =
                                        new HashMap<String, HostScoreInfo>();

    /** List of heartbeat ids of all services. */
    private final List<String> heartbeatIdList = new ArrayList<String>();

    /** Heartbeat graph. */
    private final HeartbeatGraph heartbeatGraph;
    /** Drbd graph. */
    private final DrbdGraph drbdGraph;
    /** object that holds current heartbeat status. */
    private final HeartbeatStatus heartbeatStatus;
    /** Object that holds hb ocf data. */
    private HeartbeatOCF hbOCF;
    /** Object that drbd status and data. */
    private DrbdXML drbdXML;

    /** Common block device icon. */
    private static final ImageIcon COMMON_BD_ICON =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.CommonBlockDeviceIcon"));
    /** Started service icon. */
    private static final ImageIcon SERVICE_STARTED_ICON =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.ServiceStartedIcon"));
    /** Stopped service icon. */
    private static final ImageIcon SERVICE_STOPPED_ICON =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.ServiceStoppedIcon"));
    /** Network icon. */
    private static final ImageIcon NETWORK_ICON =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.NetworkIcon"));
    /** Available services icon. */
    private static final ImageIcon AVAIL_SERVICES_ICON =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.ServiceStoppedIcon"));
    /** Remove icon. */
    private static final ImageIcon REMOVE_ICON =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.RemoveIcon"));
    /** Migrate icon. */
    private static final ImageIcon MIGRATE_ICON =
        Tools.createImageIcon(
                Tools.getDefault("HeartbeatGraph.MigrateIcon"));
    /** Running service icon. */
    private static final ImageIcon SERVICE_RUNNING_ICON =
        Tools.createImageIcon(
                Tools.getDefault("HeartbeatGraph.ServiceRunningIcon"));
    /** Not running service icon. */
    private static final ImageIcon SERVICE_NOT_RUNNING_ICON =
        Tools.createImageIcon(
                Tools.getDefault("HeartbeatGraph.ServiceNotRunningIcon"));
    /** Add host icon. */
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(
                                        Tools.getDefault("HostTab.HostIcon"));
    /** Start service icon. */
    private static final ImageIcon START_ICON = SERVICE_RUNNING_ICON;
    /** Stop service icon. */
    private static final ImageIcon STOP_ICON  = SERVICE_NOT_RUNNING_ICON;

    /** Whether drbd status was canceled by user. */
    private boolean drbdStatusCanceled = true;
    /** Whether hb status was canceled by user. */
    private boolean hbStatusCanceled = true;
    /** Tree menu root. */
    private JTree treeMenu;
    /** Global hb status lock. */
    private final Mutex mHbStatusLock = new Mutex();

    /** last dc host detected. */
    private Host lastDcHost = null;

    /** dc host as reported by heartbeat. */
    private Host realDcHost = null;

    /** Panel that holds this browser. */
    private ClusterViewPanel clusterViewPanel = null;

    /** Hash that holds all hb classes with descriptions that appear in the
     * pull down menus. */
    private static final Map<String, String> HB_CLASS_MENU =
                                                new HashMap<String, String>();

    /** Whether the hb status is run for the first time. (For the progress
     * indicator. */
    private boolean hbStatusFirstTime;


    /** Width of the label in the info panel. */
    private static final int SERVICE_LABEL_WIDTH =
                    Tools.getDefaultInt("ClusterBrowser.ServiceLabelWidth");
    /** Width of the field in the info panel. */
    private static final int SERVICE_FIELD_WIDTH =
                    Tools.getDefaultInt("ClusterBrowser.ServiceFieldWidth");

    /** Color of the most of backgrounds. */
    private static final Color PANEL_BACKGROUND =
                    Tools.getDefaultColor("ViewPanel.Background");
    /** Color of the extra (advanced options) panel background. */
    private static final Color EXTRA_PANEL_BACKGROUND =
                    Tools.getDefaultColor("ViewPanel.Status.Background");
    /** Color of the status bar background. */
    private static final Color STATUS_BACKGROUND =
                    Tools.getDefaultColor("ViewPanel.Status.Background");

    /** Name of the drbd resource name parameter. */
    private static final String DRBD_RES_PARAM_NAME     = "name";
    /** Name of the drbd device parameter. */
    private static final String DRBD_RES_PARAM_DEV      = "device";
    /** Name of the boolean type in drbd. */
    private static final String DRBD_RES_BOOL_TYPE_NAME = "boolean";
    /** Name of the drbd after parameter. */
    private static final String DRBD_RES_PARAM_AFTER    = "after";

    /** Name of the empty parameter, that is used while passing the parameters
     * to the drbd-gui-helper script. */
    private static final String HB_NONE_ARG             = "..none..";
    /** Name of the group hearbeat service. */
    private static final String HB_GROUP_NAME           = "Group";
    /** Name of ocf style resource (heartbeat 2). */
    private static final String HB_OCF_CLASS            = "ocf";
    /** Name of heartbeat style resource (heartbeat 1). */
    private static final String HB_HEARTBEAT_CLASS      = "heartbeat";
    /** Name of lsb style resource (/etc/init.d/*). */
    private static final String HB_LSB_CLASS            = "lsb";

    /** Name of the provider. TODO: other providers? */
    private static final String HB_HEARTBEAT_PROVIDER   = "heartbeat";

    /** String array with all hb classes. */
    private static final String[] HB_CLASSES = {HB_OCF_CLASS,
                                                HB_HEARTBEAT_CLASS,
                                                HB_LSB_CLASS};

    /** Hb start operation. */
    private static final String HB_OP_START = "start";
    /** Hb stop operation. */
    private static final String HB_OP_STOP = "stop";
    /** Hb status operation. */
    private static final String HB_OP_STATUS = "status";
    /** Hb monitor operation. */
    private static final String HB_OP_MONITOR = "monitor";
    /** Hb meta-data operation. */
    private static final String HB_OP_META_DATA = "meta-data";
    /** Hb validate-all operation. */
    private static final String HB_OP_VALIDATE_ALL = "validate-all";

    /** Hb desc parameter. */
    private static final String HB_PAR_DESC = "description";
    /** Hb interval parameter. */
    private static final String HB_PAR_INTERVAL = "interval";
    /** Hb timeout parameter. */
    private static final String HB_PAR_TIMEOUT = "timeout";
    /** Hb start-delay parameter. */
    private static final String HB_PAR_START_DELAY = "start-delay";
    /** Hb disabled parameter. */
    private static final String HB_PAR_DISABLED = "disabled";
    /** Hb role parameter. */
    private static final String HB_PAR_ROLE = "role";
    /** Hb prereq parameter. */
    private static final String HB_PAR_PREREQ = "prereq";
    /** Hb on-fail parameter. */
    private static final String HB_PAR_ON_FAIL = "on-fail";

    /** String array with all hb operations. */
    private static final String[] HB_OPERATIONS = {HB_OP_START,
                                                   HB_OP_STOP,
                                                   HB_OP_STATUS,
                                                   HB_OP_MONITOR,
                                                   HB_OP_META_DATA,
                                                   HB_OP_VALIDATE_ALL};
    /** Which operations are basic and do not go to the advanced section. */
    private static final List<String> HB_OP_BASIC =
                    Arrays.asList(new String[]{HB_OP_START, HB_OP_STOP});
    /** Parameters for the hb operations. */
    private static final Map<String, List<String>> HB_OPERATION_PARAMS =
                                           new HashMap<String, List<String>>();
    /** All parameters for the hb operations, so that it is possible to create
     * arguments for up_rsc_full_ops. */
    private static final String[] HB_OPERATION_PARAM_LIST = {HB_PAR_DESC,
                                                             HB_PAR_INTERVAL,
                                                             HB_PAR_TIMEOUT,
                                                             HB_PAR_START_DELAY,
                                                             HB_PAR_DISABLED,
                                                             HB_PAR_ROLE,
                                                             HB_PAR_PREREQ,
                                                             HB_PAR_ON_FAIL};

    /**
     * Prepares a new <code>CusterBrowser</code> object.
     */
    public ClusterBrowser(final Cluster cluster) {
        super();
        this.cluster = cluster;
        heartbeatGraph = new HeartbeatGraph(this);
        drbdGraph = new DrbdGraph(this);
        heartbeatStatus = new HeartbeatStatus();
        setTreeTop();
        HB_CLASS_MENU.put(HB_OCF_CLASS,       "heartbeat 2 (ocf)");
        HB_CLASS_MENU.put(HB_HEARTBEAT_CLASS, "heartbeat 1 (hb)");
        HB_CLASS_MENU.put(HB_LSB_CLASS,       "lsb (init.d)");

        HB_OPERATION_PARAMS.put(HB_OP_START,
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL)));
        HB_OPERATION_PARAMS.put(HB_OP_STOP,
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL)));
        HB_OPERATION_PARAMS.put(HB_OP_META_DATA,
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL)));
        HB_OPERATION_PARAMS.put(HB_OP_VALIDATE_ALL,
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL)));

        HB_OPERATION_PARAMS.put(HB_OP_STATUS, 
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL)));

        HB_OPERATION_PARAMS.put(HB_OP_MONITOR,
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL,
                                                          HB_PAR_START_DELAY
                                                         )));
    }

    /**
     * Sets the cluster view panel.
     */
    public final void setClusterViewPanel(
                                    final ClusterViewPanel clusterViewPanel) {
        this.clusterViewPanel = clusterViewPanel;
    }

    /**
     * Returns cluster view panel.
     */
    public final ClusterViewPanel getClusterViewPanel() {
        return clusterViewPanel;
    }

    /**
     * Returns all nodes that belong to this cluster.
     */
    public final Host[] getClusterHosts() {
        return cluster.getHostsArray();
    }

    /**
     * Returns cluster data object.
     */
    public final Cluster getCluster() {
        return cluster;
    }

    /**
     * Sets the info panel component in the cluster view panel.
     */
    public final void setRightComponentInView(final Info i) {
        clusterViewPanel.setRightComponentInView(this, i);
    }

    /**
     * Saves positions of service and block devices from the heartbeat and drbd
     * graphs to the config files on every node.
     */
    public final void saveGraphPositions() {
        final Map<String, Point2D> positions = new HashMap<String, Point2D>();
        if (drbdGraph != null) {
            drbdGraph.getPositions(positions);
        }
        if (heartbeatGraph != null) {
            heartbeatGraph.getPositions(positions);
        }

        final Host[] hosts = getClusterHosts();
        for (Host host : hosts) {
            host.saveGraphPositions(positions);
        }
    }

    /**
     * Returns heartbeat graph for this cluster.
     */
    public final ResourceGraph getHeartbeatGraph() {
        return heartbeatGraph;
    }

    /**
     * Returns drbd graph for this cluster.
     */
    public final ResourceGraph getDrbdGraph() {
        return drbdGraph;
    }

    /**
     * Initializes cluster resources for cluster view.
     */
    public final void initClusterResources() {

        /* all hosts */
        allHostsNode = new DefaultMutableTreeNode(new AllHostsInfo());
        setNode(allHostsNode);
        topAdd(allHostsNode);
        /* hosts */
        clusterHostsNode = new DefaultMutableTreeNode(
            new CategoryInfo(Tools.getString("ClusterBrowser.ClusterHosts")));
        setNode(clusterHostsNode);
        topAdd(clusterHostsNode);

        /* networks */
        networksNode = new DefaultMutableTreeNode(
            new CategoryInfo(Tools.getString("ClusterBrowser.Networks")));
        setNode(networksNode);
        topAdd(networksNode);

        /* drbd */
        drbdNode = new DefaultMutableTreeNode(
            new DrbdInfo(Tools.getString("ClusterBrowser.Drbd")));
        setNode(drbdNode);
        topAdd(drbdNode);

        /* heartbeat */
        final HeartbeatInfo heartbeatInfo =
            new HeartbeatInfo(
                    Tools.getString("ClusterBrowser.Heartbeat"));
        final DefaultMutableTreeNode heartbeatNode =
                            new DefaultMutableTreeNode(heartbeatInfo);
        setNode(heartbeatNode);
        topAdd(heartbeatNode);

        /* available services */
        availableServicesNode = new DefaultMutableTreeNode(
            new HbCategoryInfo(
                Tools.getString("ClusterBrowser.availableServices")));
        setNode(availableServicesNode);
        heartbeatNode.add(availableServicesNode);

        /* block devices / shared disks, TODO: */
        commonBlockDevicesNode = new DefaultMutableTreeNode(
            new HbCategoryInfo(
                Tools.getString("ClusterBrowser.CommonBlockDevices")));
        setNode(commonBlockDevicesNode);
        /* heartbeatNode.add(commonBlockDevicesNode); */

        /* scores TODO: make the scores editable? */
        scoresNode = new DefaultMutableTreeNode(
            new HbCategoryInfo(Tools.getString("ClusterBrowser.Scores")));
        setNode(scoresNode);
        /* heartbeatNode.add(scoresNode); */

        /* scores */
        final String[] scores = {"100",
                           "INFINITY",
                           "-INFINITY",
                           "-100",
                           "0"};
        for (String score : scores) {
            final HostScoreInfo hsi = new HostScoreInfo(score);
            final DefaultMutableTreeNode hostScoreNode =
                                            new DefaultMutableTreeNode(hsi);
            hostScoreInfoMap.put(hsi.getName(), hsi);
            setNode(hostScoreNode);
            scoresNode.add(hostScoreNode);
        }

        /* services */
        final ServicesInfo servicesInfo =
                new ServicesInfo(Tools.getString("ClusterBrowser.Services"));
        servicesNode = new DefaultMutableTreeNode(servicesInfo);
        setNode(servicesNode);
        heartbeatNode.add(servicesNode);
    }

    /**
     * Updates resources of a cluster in the tree.
     *
     * @param clusterHosts
     *          hosts in this cluster
     * @param commonFileSystems
     *          filesystems that are common on both hosts
     * @param commonMountPoints
     *          mount points that are common on both hosts
     */
    public final void updateClusterResources(final JTree treeMenu,
                                             final Host[] clusterHosts,
                                             final String[] commonFileSystems,
                                             final String[] commonMountPoints,
                                      final ClusterViewPanel clusterViewPanel) {
        this.treeMenu = treeMenu;
        this.commonFileSystems = commonFileSystems;
        this.commonMountPoints = commonMountPoints;
        DefaultMutableTreeNode resource;

        /* all hosts */
        final Host[] allHosts =
                               Tools.getConfigData().getHosts().getHostsArray();
        allHostsNode.removeAllChildren();
        for (Host host : allHosts) {
            final HostBrowser hostBrowser = host.getBrowser();
            resource = new DefaultMutableTreeNode(hostBrowser.getHostInfo());
            setNode(resource);
            allHostsNode.add(resource);
        }
        reload(allHostsNode);

        /* cluster hosts */
        clusterHostsNode.removeAllChildren();
        for (Host clusterHost : clusterHosts) {
            final HostBrowser hostBrowser = clusterHost.getBrowser();
            resource = new DefaultMutableTreeNode(hostBrowser.getHostInfo());
            setNode(resource);
            clusterHostsNode.add(resource);
            //drbdGraph.addHost(hostBrowser.getHostInfo());
            heartbeatGraph.addHost(hostBrowser.getHostInfo());
        }

        reload(clusterHostsNode);

        /* block devices */
        updateCommonBlockDevices();

        /* networks */
        updateNetworks();

        updateHeartbeatDrbdThread(clusterViewPanel);
    }

    /**
     * Starts everything.
     */
    private void updateHeartbeatDrbdThread(
                                    final ClusterViewPanel clusterViewPanel) {
        final Runnable runnable = new Runnable() {
            public void run() {
                Host firstHost = null;
                final Host[] hosts = cluster.getHostsArray();
                do { /* wait here until a host is connected. */
                    boolean notConnected = false;
                    for (final Host host : hosts) {
                        // TODO: fix that, use latches or callback
                        if (!host.isConnected()) {
                            notConnected = true;
                            break;
                        } else {
                            final HostBrowser hostBrowser = host.getBrowser();
                            drbdGraph.addHost(hostBrowser.getHostInfo());
                        }
                    }
                    if (!notConnected) {
                        firstHost = getFirstHost();
                    }
                    if (firstHost == null) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } while (firstHost == null);

                hbOCF = new HeartbeatOCF(firstHost);
                drbdXML = new DrbdXML(cluster.getHostsArray());
                /* available services */
                Tools.startProgressIndicator(getCluster(),
                        Tools.getString("ClusterBrowser.HbUpdateResources"));

                updateAvailableServices();
                Tools.stopProgressIndicator(getCluster(),
                    Tools.getString("ClusterBrowser.HbUpdateResources"));
                Tools.startProgressIndicator(getCluster(),
                    Tools.getString("ClusterBrowser.DrbdUpdate"));

                updateDrbdResources();
                //SwingUtilities.invokeLater(new Runnable() { public void run() {
                   drbdGraph.scale();
                //} });
                //try { Thread.sleep(10000); }
                //catch (InterruptedException ex) {}
                Tools.stopProgressIndicator(getCluster(),
                    Tools.getString("ClusterBrowser.DrbdUpdate"));
                cluster.getBrowser().startDrbdStatus(clusterViewPanel);
                cluster.getBrowser().startHbStatus(clusterViewPanel);
            }
        };
        final Thread thread = new Thread(runnable);
        //thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /**
     * Starts drbd status on all hosts.
     */
    public final void startDrbdStatus(final ClusterViewPanel clusterViewPanel) {
        final Host[] hosts = cluster.getHostsArray();
        for (final Host host : hosts) {
            final Thread thread = new Thread(new Runnable() {
                public void run() {
                    startDrbdStatus(host, clusterViewPanel);
                }
            });
            thread.start();
        }
    }

    /**
     * Starts drbd status.
     */
    public final void stopDrbdStatus() {
        drbdStatusCanceled = true;
        final Host[] hosts = cluster.getHostsArray();
        for (Host host : hosts) {
            host.stopDrbdStatus();
        }
        for (Host host : hosts) {
            host.waitOnDrbdStatus();
        }
    }

    /**
     * Starts drbd status on host.
     */
    public final void startDrbdStatus(final Host host,
                                final ClusterViewPanel clusterViewPanel) {
        boolean firstTime = true; // TODO: can use a latch for this shit too.
        host.setDrbdStatus(true);
        while (true) {
            final boolean ft = firstTime;
            drbdStatusCanceled = false;
            host.execDrbdStatusCommand(
                                  new ExecCallback() {
                                       public void done(final String ans) {
                                       }

                                       public void doneError(final String ans, final int exitCode) {
                                           Tools.debug(this, "drbd status failed: " + host.getName() + "exit code: " + exitCode, 2);
                                           if (exitCode != 143) {
                                               /* was killed intentionally */
                                               host.setDrbdStatus(false);
                                           }
                                           //TODO: repaint ok?
                                           //repaintSplitPane();
                                           //drbdGraph.updatePopupMenus();
                                           drbdGraph.repaint();
                                       }
                                   },

                                   new NewOutputCallback() {
                                       public void output(final String output) {
                                           if (output.indexOf("No response from the DRBD driver") >= 0) {
                                               host.setDrbdStatus(false);
                                               return;
                                           } else {
                                               host.setDrbdStatus(true);
                                           }
                                           if (ft) {
                                               Tools.startProgressIndicator(host, host.getName() + ": updating drbd status...");
                                           }
                                           final String[] lines = output.split("\n");
                                           drbdXML.update(host);
                                           host.setDrbdStatus(true);
                                           for (int i = 0; i < lines.length; i++) {
                                               parseDrbdEvent(host.getName(), lines[i]);
                                           }

                                           final Thread thread = new Thread(
                                               new Runnable() {
                                                   public void run() {
                                                       repaintSplitPane();
                                                       drbdGraph.updatePopupMenus();
                                                   }
                                               });
                                           thread.start();
                                           if (ft) {
                                               Tools.stopProgressIndicator(host, host.getName() + ": updating drbd status...");
                                           }
                                       }
                                   });
            clusterViewPanel.drbdStatusButtonEnable();
            if (!host.isDrbdStatus()) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            host.waitOnDrbdStatus();
            firstTime = false;
            if (drbdStatusCanceled) {
                firstTime = true;
                break;
            }
        }
    }

    /**
     * Stops hb status.
     */
    public final void stopHbStatus() {
        hbStatusCanceled = true;
        final Host[] hosts = cluster.getHostsArray();
        for (Host host : hosts) {
            host.stopHbStatus();
        }
    }


    /**
     * Returns true if hb status on all hosts failed.
     */
    public final boolean hbStatusFailed() {
        final Host[] hosts = cluster.getHostsArray();
        for (Host host : hosts) {
            if (host.isHbStatus()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets hb status (failed / not failed for every node).
     */
    public final void setHbStatus() {
        final Host[] hosts = cluster.getHostsArray();
        for (Host host : hosts) {
            host.setHbStatus(heartbeatStatus.isActiveNode(host.getName()));
        }
    }

    /**
     * Starts hb status progress indicator.
     */
    public final void startHbStatusProgressIndicator(final Host host) {
        // TODO; hbStatusFirstTime closure?
        Tools.startProgressIndicator(
                            host,
                            Tools.getString("ClusterBrowser.HbUpdateStatus"));
    }

    /**
     * Stops hb status progress indicator.
     */
    public final void stopHbStatusProgressIndicator(final Host host) {
        Tools.stopProgressIndicator(
                            host,
                            Tools.getString("ClusterBrowser.HbUpdateStatus"));
    }

    /**
     * Starts hb status.
     */
    public final void startHbStatus(final ClusterViewPanel clusterViewPanel) {
        hbStatusFirstTime = true;
        while (true) {
            final Host host = getDCHost();
            if (host == null) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }
            if (hbStatusFirstTime) {
                startHbStatusProgressIndicator(host);
            }
            hbStatusCanceled = false;
            host.execHbStatusCommand(
                                     new ExecCallback() {
                                         public void done(final String ans) {
                                             if (hbStatusFirstTime) {
                                                 hbStatusFirstTime = false;
                                                 selectServices();
                                                 //SwingUtilities.invokeLater(new Runnable() { public void run() {
                                                 //    heartbeatGraph.scale();
                                                 //} });
                                                 stopHbStatusProgressIndicator(host);
                                             }
                                         }

                                         public void doneError(final String ans, final int exitCode) {
                                             Tools.progressIndicatorFailed(host, "Heartbeat status failed");
                                             if (hbStatusFirstTime) {
                                                Tools.debug(this, "hb status failed: " + host.getName());
                                             }
                                             hbStatusLock();
                                             final boolean prevHbStatusFailed = hbStatusFailed();
                                             host.setHbStatus(false);
                                             heartbeatStatus.setDC(null);
                                             hbStatusUnlock();
                                             if (prevHbStatusFailed != hbStatusFailed()) {
                                                 heartbeatGraph.getServicesInfo().selectMyself();
                                             }
                                             done(ans);
                                         }
                                     },

                                     new NewOutputCallback() {
                                         //TODO: check this buffer's size
                                         private StringBuffer heartbeatStatusOutput = new StringBuffer(300);
                                         public void output(final String output) {
                                             hbStatusLock();
                                             final boolean prevHbStatusFailed = hbStatusFailed();
                                             if (hbStatusCanceled) {
                                                 hbStatusUnlock();
                                                 if (hbStatusFirstTime) {
                                                     hbStatusFirstTime = false;
                                                     selectServices();
                                                     //SwingUtilities.invokeLater(new Runnable() { public void run() {
                                                         heartbeatGraph.scale();
                                                     //} });
                                                     stopHbStatusProgressIndicator(host);
                                                 }
                                                 return;
                                             }
                                             if (output == null) {
                                                 host.setHbStatus(false);
                                                 if (prevHbStatusFailed != hbStatusFailed()) {
                                                     heartbeatGraph.getServicesInfo().selectMyself();
                                                 }
                                             } else {
                                                 heartbeatStatusOutput.append(output);
                                                 if (heartbeatStatusOutput.length() > 12) {
                                                     //host.setHbStatus(true);
                                                     final String e = heartbeatStatusOutput.substring(heartbeatStatusOutput.length() - 12);
                                                     if (e.trim().equals("---done---")) {
                                                         final int i = heartbeatStatusOutput.lastIndexOf("---start---");
                                                         if (i >= 0) {
                                                             if (heartbeatStatusOutput.indexOf("is stopped") >= 0) {
                                                                 /* TODO: heartbeat's not running. */
                                                             } else {
                                                                 final String status = heartbeatStatusOutput.substring(i);
                                                                 heartbeatStatusOutput.delete(0, heartbeatStatusOutput.length() - 1);
                                                                 if ("---start---\r\nerror\r\n\r\n---done---\r\n".equals(status)) {
                                                                     host.setHbStatus(false);
                                                                 } else {
                                                                     heartbeatStatus.parseStatus(status, hbOCF);
                                                                     // TODO; servicesInfo can be null
                                                                     heartbeatGraph.getServicesInfo().setGlobalConfig();
                                                                     heartbeatGraph.getServicesInfo().setAllResources();
                                                                 }
                                                             }
                                                         }
                                                     }
                                                     setHbStatus();
                                                 } else {
                                                    host.setHbStatus(false);
                                                 }
                                             }
                                            if (prevHbStatusFailed != hbStatusFailed()) {
                                                 heartbeatGraph.getServicesInfo().selectMyself();
                                            }
                                             if (hbStatusFirstTime) {
                                                 hbStatusFirstTime = false;
                                                 selectServices();
                                                 //SwingUtilities.invokeLater(new Runnable() { public void run() {
                                                 //    heartbeatGraph.scale();
                                                 //} });
                                                 stopHbStatusProgressIndicator(host);
                                             }
                                             hbStatusUnlock();
                                         }
                                     });
            clusterViewPanel.hbStatusButtonEnable();
            host.waitOnHbStatus();
            if (hbStatusCanceled) {
                hbStatusFirstTime = true;
                break;
            }
            final boolean hbSt = hbStatusFailed();
            if (hbSt) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Returns 'add service' list for menus.
     */
    public final List<HeartbeatService> globalGetAddServiceList(
                                                            final String cl) {
        return hbOCF.getServices(cl);
    }

    /**
     * Updates common block devices.
     */
    private void updateCommonBlockDevices() {
        if (commonBlockDevicesNode != null) {
            DefaultMutableTreeNode resource;
            final List<String> bd = cluster.getCommonBlockDevices();
            final Enumeration e = commonBlockDevicesNode.children();
            final List<DefaultMutableTreeNode> nodesToRemove =
                                        new ArrayList<DefaultMutableTreeNode>();
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) e.nextElement();
                final Info cbdi = (Info) node.getUserObject();
                if (bd.contains(cbdi.getName())) {
                    /* keeping */
                    bd.remove(bd.indexOf(cbdi.getName()));
                } else {
                    /* remove not existing block devices */
                    nodesToRemove.add(node);
                }
            }

            /* remove nodes */
            for (DefaultMutableTreeNode node : nodesToRemove) {
                node.removeFromParent();
            }

            /* block devices */
            for (String device : bd) {
                /* add new block devices */
                resource = new DefaultMutableTreeNode(
                     new CommonBlockDevInfo(
                                          device,
                                          cluster.getHostBlockDevices(device)));
                setNode(resource);
                commonBlockDevicesNode.add(resource);
            }
            reload(commonBlockDevicesNode);
        }
    }

    /**
     * Updates available services.
     */
    private void updateAvailableServices() {
        DefaultMutableTreeNode resource;
        availableServicesNode.removeAllChildren();
        for (final String cl : HB_CLASSES) {
            final DefaultMutableTreeNode classNode =
                    new DefaultMutableTreeNode(
                        new HbCategoryInfo(cl.toUpperCase()));
            for (HeartbeatService hbService : hbOCF.getServices(cl)) {
                resource = new DefaultMutableTreeNode(
                                    new AvailableServiceInfo(hbService));
                setNode(resource);
                classNode.add(resource);
            }
            availableServicesNode.add(classNode);
        }
        //reload(availableServicesNode);
    }

    /**
     * Updates drbd resources.
     */
    private void updateDrbdResources() {
        final String[] drbdResources = drbdXML.getResources();
        for (int i = 0; i < drbdResources.length; i++) {
            final String resName = drbdResources[i];
            final String drbdDev = drbdXML.getDrbdDevice(resName);
            final Map<String, String> hostDiskMap =
                                                drbdXML.getHostDiskMap(resName);
            BlockDevInfo bd1 = null;
            BlockDevInfo bd2 = null;
            for (String hostName : hostDiskMap.keySet()) {
                if (!cluster.contains(hostName)) {
                    continue;
                }
                final String disk = hostDiskMap.get(hostName);
                final BlockDevInfo bdi = drbdGraph.findBlockDevInfo(hostName,
                                                                    disk);
                if (bdi == null) {
                    if (drbdDevHash.containsKey(disk)) {
                        /* TODO: ignoring stacked device */
                        continue;
                    } else {
                        Tools.appWarning("could not find disk: " + disk
                                         + " on host: " + hostName);
                        continue;
                    }
                }
                bdi.getBlockDevice().setValue(
                                      "DrbdNetInterfacePort",
                                      drbdXML.getVirtualInterfacePort(hostName,
                                                                      resName));
                bdi.getBlockDevice().setValue(
                                          "DrbdNetInterface",
                                          drbdXML.getVirtualInterface(hostName,
                                                                      resName));
                final String drbdMetaDisk = drbdXML.getMetaDisk(hostName,
                                                                resName);
                bdi.getBlockDevice().setValue("DrbdMetaDisk", drbdMetaDisk);
                bdi.getBlockDevice().setValue(
                                            "DrbdMetaDiskIndex",
                                            drbdXML.getMetaDiskIndex(hostName,
                                                                     resName));
                if (drbdMetaDisk != null && !drbdMetaDisk.equals("internal")) {
                    final BlockDevInfo mdI =
                                drbdGraph.findBlockDevInfo(hostName,
                                                           drbdMetaDisk);
                    mdI.getBlockDevice().setIsDrbdMetaDisk(true);
                    bdi.getBlockDevice().setMetaDisk(mdI.getBlockDevice());
                }
                if (bd1 == null) {
                    bd1 = bdi;
                } else {
                    bd2 = bdi;
                }
            }
            drbdGraph.getDrbdInfo().addDrbdResource(resName,
                                                    drbdDev,
                                                    bd1,
                                                    bd2,
                                                    false);
        }
    }

    /**
     * Updates networks.
     */
    private void updateNetworks() {
        if (networksNode != null) {
            DefaultMutableTreeNode resource;
            final Network[] networks = cluster.getCommonNetworks();
            networksNode.removeAllChildren();
            for (int i = 0; i < networks.length; i++) {
                resource = new DefaultMutableTreeNode(
                                    new NetworkInfo(networks[i].getName(),
                                                    networks[i]));
                setNode(resource);
                networksNode.add(resource);
            }
            reload(networksNode);
        }
    }

    /**
     * Returns first host. Used for heartbeat commands, that can be
     * executed on any host.
     * It changes terminal panel to this host.
     */
    public final Host getFirstHost() {
        /* TODO: if none of the hosts is connected the null causes error during
         * loading. */
        Host[] hosts = getClusterHosts();
        for (Host host : hosts) {
            if (host.isConnected()) {
                return host;
            }
        }
        //if (hosts != null && hosts.length > 0) {
        //    return hosts[0];
        //}
        //Tools.appError("Could not find any hosts");
        return null;
    }

    /**
     * Returns whether the host is the real dc host as reported by heartbeat.
     */
    public final boolean isRealDcHost(final Host host) {
        return host.equals(realDcHost);
    }

    /**
     * Finds and returns DC host.
     * TODO: document what's going on.
     */
    public final Host getDCHost() {
        Host dcHost = null;
        final String dc = heartbeatStatus.getDC();
        final List<Host> hosts = new ArrayList<Host>();
        int lastHostIndex = -1;
        int i = 0;
        for (Host host : getClusterHosts()) {
            if (host.getName().equals(dc)) {
                dcHost = host;
                break;
            }
            hosts.add(host);

            if (host == lastDcHost) {
                lastHostIndex = i;
            }
            i++;
        }

        if (dcHost == null) {
            int ix = lastHostIndex;
            do {
                ix++;
                if (ix > hosts.size() - 1) {
                    ix = 0;
                }
                if (hosts.get(ix).isConnected()) {
                    lastDcHost = hosts.get(ix);
                    break;
                }
            } while (ix == lastHostIndex);
            dcHost = lastDcHost;
            realDcHost = null;
        } else {
            realDcHost = dcHost;
        }

        lastDcHost = dcHost;
        return dcHost;
    }

    /**
     * hbStatusLock global lock.
     */
    public final void hbStatusLock() {
        try {
            mHbStatusLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * hbStatusLock global unlock.
     */
    public final void hbStatusUnlock() {
        mHbStatusLock.release();
    }

    /**
     * Parses drbd event from host.
     */
    public final void parseDrbdEvent(final String hostName,
                                     final String output) {
        if (drbdXML == null) {
            return;
        }
        drbdXML.parseDrbdEvent(hostName, drbdGraph, output);
    }

    /**
     * Highlights drbd node.
     */
    public final void selectDrbd() {
        reload(drbdNode);
    }

    /**
     * Highlights services.
     */
    public final void selectServices() {
        // this fires treeStructureChanged in ViewPanel.
        reload(servicesNode);
    }

    /**
     * Returns ServiceInfo object identified by name and id.
     */
    protected final ServiceInfo getServiceInfoFromId(final String name,
                                                     final String id) {
        final Map<String, ServiceInfo> idToInfoHash =
                                                nameToServiceInfoHash.get(name);
        if (idToInfoHash == null) {
            return null;
        }
        return idToInfoHash.get(id);
    }

    /**
     * Removes ServiceInfo from the ServiceInfo hash.
     *
     * @param serviceInfo
     *              service info object
     */
    protected final void removeFromServiceInfoHash(
                                                final ServiceInfo serviceInfo) {
        final Service service = serviceInfo.getService();
        final Map<String, ServiceInfo> idToInfoHash =
                        nameToServiceInfoHash.get(service.getName());
        if (idToInfoHash != null) {
            idToInfoHash.remove(service.getId());
        }
    }

    /**
     * Adds heartbeat id from service to the list. If service does not have an
     * id it is generated.
     *
     * @param si
     *          service info object
     */
    private void addToHeartbeatIdList(final ServiceInfo si) {
        String id = si.getService().getHeartbeatId();
        if (id == null) {
            if (HB_GROUP_NAME.equals(si.getName())) {
                id = "grp_";
            } else {
                id = "res_" + si.getName() + "_";
            }
            int i = 1;
            while (heartbeatIdList.contains(id + Integer.toString(i))) {
                i++;
            }
            final String newId = id + Integer.toString(i);
            heartbeatIdList.add(newId);
            si.getService().setHeartbeatId(newId);
            heartbeatIdToServiceInfo.put(newId, si);
        } else {
            if (!heartbeatIdList.contains(id)) {
                heartbeatIdList.add(id);
            }
            if (heartbeatIdToServiceInfo.get(id) == null) {
                heartbeatIdToServiceInfo.put(id, si);
            }
        }
    }

    /**
     * Deletes caches of all Filesystem infoPanels.
     * This is usefull if something have changed.
     */
    public final void resetFilesystems() {
        for (String hbId : heartbeatIdToServiceInfo.keySet()) {
            final ServiceInfo si = heartbeatIdToServiceInfo.get(hbId);
            if (si.getName().equals("Filesystem")) {
                si.setInfoPanel(null);
            }
        }
    }

    /**
     * Adds ServiceInfo in the name to ServiceInfo hash. Id and name
     * are taken from serviceInfo object. nameToServiceInfoHash
     * contains a hash with id as a key and ServiceInfo as a value.
     *
     * @param serviceInfo
     *              service info object
     */
    protected final void addNameToServiceInfoHash(
                                                final ServiceInfo serviceInfo) {
        /* add to the hash with service name and id as
         * keys */
        final Service service = serviceInfo.getService();
        Map<String, ServiceInfo> idToInfoHash =
                                nameToServiceInfoHash.get(service.getName());
        if (idToInfoHash == null) {
            idToInfoHash = new HashMap<String, ServiceInfo>();
            if (service.getId() == null) {
               service.setId("1");
            }
        } else {
            if (service.getId() == null) {
                final Iterator it = idToInfoHash.keySet().iterator();
                int index = 0;
                while (it.hasNext()) {
                    final String id =
                      idToInfoHash.get((String) it.next()).getService().getId();
                    try {
                        final int i = Integer.parseInt(id);
                        if (i > index) {
                            index = i;
                        }
                    } catch (NumberFormatException nfe) {
                        /* not a number */
                    }
                }
                service.setId(Integer.toString(index + 1));
            }
        }
        idToInfoHash.remove(service.getId());
        idToInfoHash.put(service.getId(), serviceInfo);
        nameToServiceInfoHash.remove(service.getName());
        nameToServiceInfoHash.put(service.getName(), idToInfoHash);
    }

    /**
     * Starts heartbeats on all nodes.
     */
    public final void startHeartbeats() {
        final Host[] hosts = cluster.getHostsArray();
        for (Host host : hosts) {
            Heartbeat.start(host);
        }
    }

    /**
     * This class holds info data for a preferred host score.
     * Nothing is displayed.
     */
    class HostScoreInfo extends HbCategoryInfo {
        /** Heartbeat score to keep a service on a host. It can be number,
         * INFINITY or -INFINITY.
         */
        private final String score;

        /**
         * Prepares a new <code>ScoreInfo</code> object.
         *
         * @param score
         *      host score, it is either number, INFINITY or -INFINITY
         */
        HostScoreInfo(final String score) {
            super(Tools.scoreToString(score));
            this.score = score;
        }

        /**
         * Returns text about host score info.
         */
        public String getInfo() {
            return null;
        }

        /**
         * Returns score as string.
         */
        public String getScore() {
            return score;
        }
    }

    /**
     * This class holds info data for a network.
     */
    class NetworkInfo extends Info {

        /**
         * Prepares a new <code>NetworkInfo</code> object.
         */
        public NetworkInfo(final String name, final Network network) {
            super(name);
            setResource(network);
        }

        /**
         * Returns network info.
         */
        public String getInfo() {
            final String ret = "Net info: " + getNetwork().getName() + "\n"
                               + "     IPs: " + getNetwork().getIps()  + "\n"
                               + "Net mask: " + getNetwork().getNetMask()
                               + "\n";
            return ret;
        }

        /**
         * Returns network resource object.
         */
        public Network getNetwork() {
            return (Network) getResource();
        }

        /**
         * Returns menu icon for network.
         */
        public ImageIcon getMenuIcon() {
            return NETWORK_ICON;
        }
    }

    /**
     * This interface provides getDevice function for drbd block devices or
     * block devices that don't have drbd over them but are used by heartbeat.
     */
    public interface CommonDeviceInterface {
        /** Returns the name. */
        String getName();
        /** Returns the device name. */
        String getDevice();
        /** Sets whether the device is used by heartbeat. */
        void setUsedByHeartbeat(boolean isUsedByHeartbeat);
        /** Returns whether the device is used by heartbeat. */
        boolean isUsedByHeartbeat();
    }

    /**
     * Returns common file systems on all nodes as StringInfo array.
     * The defaultValue is stored as the first item in the array.
     */
    public final StringInfo[] getCommonFileSystems(final String defaultValue) {
        StringInfo[] cfs =  new StringInfo[commonFileSystems.length + 1];
        cfs[0] = new StringInfo(defaultValue, null);
        int i = 1;
        for (String cf : commonFileSystems) {
            cfs[i] = new StringInfo(cf, cf);
            i++;
        }
        return cfs;
    }

    /**
     * this class holds info data, menus and configuration
     * for a drbd resource.
     */
    public class DrbdResourceInfo extends EditableInfo
                                  implements CommonDeviceInterface {
        /** BlockDevInfo object of the first block device. */
        private final BlockDevInfo blockDevInfo1;
        /** BlockDevInfo object of the second block device. */
        private final BlockDevInfo blockDevInfo2;
        /**
         * Whether the block device is used by heartbeat via Filesystem
         * service.
         */
        private boolean isUsedByHeartbeat;
        /** Cache for getInfoPanel method. */
        private JComponent infoPanel = null;
        /** Whether the meta-data has to be created or not. */
        private boolean haveToCreateMD = false;

        /**
         * Prepares a new <code>DrbdResourceInfo</code> object.
         *
         * @param name
         *      name that will be shown in the tree
         * @param drbdDev
         *      drbd device
         * @param blockDevInfo1
         *      first block device info object belonging to this drbd device
         * @param blockDevInfo2
         *      second block device info object belonging to this drbd device
         */
        public DrbdResourceInfo(final String name,
                                final String drbdDev,
                                final BlockDevInfo blockDevInfo1,
                                final BlockDevInfo blockDevInfo2) {
            super(name);
            setResource(new DrbdResource(name, null));
            initApplyButton();
            setResource(new DrbdResource(name, drbdDev));
            // TODO: drbdresource
            getResource().setValue(DRBD_RES_PARAM_DEV, drbdDev);
            this.blockDevInfo1 = blockDevInfo1;
            this.blockDevInfo2 = blockDevInfo2;
            initApplyButton();
        }

        /**
         * Returns device name, like /dev/drbd0.
         */
        public final String getDevice() {
            return getDrbdResource().getDevice();
        }

        /**
         * Returns menu icon for drbd resource.
         */
        public final ImageIcon getMenuIcon() {
            return null;
        }

        /**
         * Returns cluster object to resource belongs.
         */
        public final Cluster getCluster() {
            return cluster;
        }

        /**
         * Returns other block device in the drbd cluster.
         */
        public final BlockDevInfo getOtherBlockDevInfo(final BlockDevInfo bdi) {
            if (bdi.equals(blockDevInfo1)) {
                return blockDevInfo2;
            } else if (bdi.equals(blockDevInfo2)) {
                return blockDevInfo1;
            } else {
                return null;
            }
        }

        /**
         * Returns first block dev info.
         */
        public final BlockDevInfo getFirstBlockDevInfo() {
            return blockDevInfo1;
        }

        /**
         * Returns second block dev info.
         */
        public final BlockDevInfo getSecondBlockDevInfo() {
            return blockDevInfo2;
        }

        /**
         * Returns true if this is first block dev info.
         */
        public final boolean isFirstBlockDevInfo(final BlockDevInfo bdi) {
            return blockDevInfo1 == bdi;
        }

        /**
         * Creates drbd config for sections and returns it. Removes 'drbd: '
         * from the 'after' parameter.
         * TODO: move this out of gui
         */
        private String drbdSectionsConfig()
                                        throws Exceptions.DrbdConfigException {
            final StringBuffer config = new StringBuffer("");
            final String[] sections = drbdXML.getSections();
            for (String section : sections) {
                if ("resource".equals(section) || "global".equals(section)) {
                    // TODO: Tools.getString
                    continue;
                }
                final String[] params = drbdXML.getSectionParams(section);

                if (params.length != 0) {
                    final StringBuffer sectionConfig = new StringBuffer("");
                    for (String param : params) {
                        final String value = getResource().getValue(param);
                        if (value == null) {
                            Tools.debug(this, "section: " + section
                                    + ", param " + param + " ("
                                    + getName() + ") not defined");
                            throw new Exceptions.DrbdConfigException("param "
                                                    + param + " (" + getName()
                                                    + ") not defined");
                        }
                        if (!value.equals(drbdXML.getParamDefault(param))) {
                            if (isCheckBox(param)
                                && value.equals(
                                            Tools.getString("Boolean.True"))) {
                                /* boolean parameter */
                                sectionConfig.append("\t\t" + param + ";\n");
                            } else if (DRBD_RES_PARAM_AFTER.equals(param)) {
                                /* after parameter */
                                if (!value.equals(Tools.getString(
                                                    "ClusterBrowser.None"))) {
                                    sectionConfig.append("\t\t");
                                    sectionConfig.append(param);
                                    sectionConfig.append('\t');
                                    sectionConfig.append(
                                                    Tools.escapeConfig(value));
                                    sectionConfig.append(";\n");
                                }
                            } else { /* name value parameter */
                                sectionConfig.append("\t\t");
                                sectionConfig.append(param);
                                sectionConfig.append('\t');
                                sectionConfig.append(Tools.escapeConfig(value));
                                sectionConfig.append(";\n");
                            }
                        }
                    }

                    if (sectionConfig.length() > 0) {
                        config.append("\t" + section + " {\n");
                        config.append(sectionConfig);
                        config.append("\t}\n\n");
                    }
                }
            }
            return config.toString();
        }
        /**
         * Creates and returns drbd config for resources.
         *
         * TODO: move this out of gui
         */
        public final String drbdResourceConfig()
                                        throws Exceptions.DrbdConfigException {
            final StringBuffer config = new StringBuffer(50);
            config.append("resource " + getName() + " {\n");
            /* protocol... */
            final String[] params = drbdXML.getSectionParams("resource");
            for (String param : params) {
                config.append('\t');
                config.append(param);
                config.append('\t');
                config.append(getResource().getValue(param));
                config.append(";\n");
            }
            if (params.length != 0) {
                config.append('\n');
            }
            /* section config */
            try {
                config.append(drbdSectionsConfig());
            } catch (Exceptions.DrbdConfigException dce) {
                throw dce;
            }
            // startup
            // disk
            // syncer
            // net
            config.append('\n');
            config.append(blockDevInfo1.drbdNodeConfig(getName(), getDevice()));
            config.append('\n');
            config.append(blockDevInfo2.drbdNodeConfig(getName(), getDevice()));
            config.append("}\n");
            return config.toString();
        }

        /**
         * Clears info panel cache.
         */
        public final boolean selectAutomaticallyInTreeMenu() {
            return infoPanel == null;
        }

        /**
         * Returns sync progress in percent.
         */
        public final String getSyncedProgress() {
            return blockDevInfo1.getBlockDevice().getSyncedProgress();
        }

        /**
         * Returns whether the cluster is syncing.
         */
        public final boolean isSyncing() {
            return blockDevInfo1.getBlockDevice().isSyncing();
        }

        /**
         * Connect block device from the specified host.
         */
        public final void connect(final Host host) {
            if (blockDevInfo1.getHost() == host
                && !blockDevInfo1.getBlockDevice().isConnectedOrWF()) {
                blockDevInfo1.connect();
            } else if (blockDevInfo2.getHost() == host
                       && !blockDevInfo2.getBlockDevice().isConnectedOrWF()) {
                blockDevInfo2.connect();
            }
        }

        /**
         * Returns whether the resources is connected, meaning both devices are
         * connected.
         */
        public final boolean isConnected() {
            return blockDevInfo1.getBlockDevice().isConnected()
                   && blockDevInfo2.getBlockDevice().isConnected();
        }

        /**
         * Returns whether any of the sides in the drbd resource are in
         * paused-sync state.
         */
        public final boolean isPausedSync() {
            return blockDevInfo1.getBlockDevice().isPausedSync()
                   || blockDevInfo2.getBlockDevice().isPausedSync();
        }

        /**
         * Returns whether any of the sides in the drbd resource are in
         * split-brain.
         */
        public final boolean isSplitBrain() {
            return blockDevInfo1.getBlockDevice().isSplitBrain()
                   || blockDevInfo2.getBlockDevice().isSplitBrain();
        }

        /**
         * Returns drbd graphical view.
         */
        public final JPanel getGraphicalView() {
            return drbdGraph.getGraphPanel();
        }

        /**
         * Returns the DrbdInfo object (for all drbds).
         */
        public final DrbdInfo getDrbdInfo() {
            return drbdGraph.getDrbdInfo();
        }

        /**
         * Returns all parameters.
         */
        public final String[] getParametersFromXML() {
            return drbdXML.getParameters();
        }

        /**
         * Checks the new value of the parameter if it is conforms to its type
         * and other constrains.
         */
        protected final boolean checkParam(final String param,
                                           final String newValue) {
            return drbdXML.checkParam(param, newValue);
        }

        /**
         * Returns the default value for the drbd parameter.
         */
        protected final String getParamDefault(final String param) {
            return drbdXML.getParamDefault(param);
        }

        /**
         * Returns the possible values for the pulldown menus, if applicable.
         */
        protected final Object[] getParamPossibleChoices(final String param) {
            return drbdXML.getPossibleChoices(param);
        }

        /**
         * Returns the short description of the drbd parameter that is used as
         * a label.
         */
        protected final String getParamShortDesc(final String param) {
            return drbdXML.getParamShortDesc(param);
        }

        /**
         * Returns a long description of the parameter that is used for tool
         * tip.
         */
        protected final String getParamLongDesc(final String param) {
            return drbdXML.getParamLongDesc(param);
        }

        /**
         * Returns section to which this drbd parameter belongs.
         */
        protected final String getSection(final String param) {
            return drbdXML.getSection(param);
        }

        /**
         * Returns whether this drbd parameter is required parameter.
         */
        protected final boolean isRequired(final String param) {
            return drbdXML.isRequired(param);
        }

        /**
         * Returns whether this drbd parameter is of integer type.
         */
        protected final boolean isInteger(final String param) {
            return drbdXML.isInteger(param);
        }

        /**
         * Returns whether this drbd parameter is of time type.
         */
        protected final boolean isTimeType(final String param) {
            /* not required */
            return false;
        }

        /**
         * Returns whether this parameter has a unit prefix.
         */
        protected final boolean hasUnitPrefix(final String param) {
            return drbdXML.hasUnitPrefix(param);
        }

        /**
         * Returns the long unit name.
         */
        protected final String getUnitLong(final String param) {
            return drbdXML.getUnitLong(param);
        }

        /**
         * Returns the default unit for the parameter.
         */
        protected final String getDefaultUnit(final String param) {
            return drbdXML.getDefaultUnit(param);
        }

        /**
         * Returns whether the parameter is of the boolean type and needs the
         * checkbox.
         */
        protected final boolean isCheckBox(final String param) {
            final String type = drbdXML.getParamType(param);
            if (type == null) {
                return false;
            }
            if (DRBD_RES_BOOL_TYPE_NAME.equals(type)) {
                return true;
            }
            return false;
        }

        /**
         * Returns the type of the parameter (like boolean).
         */
        protected final String getParamType(final String param) {
            return drbdXML.getParamType(param);
        }

        /**
         * Returns the widget that is used to edit this parameter.
         */
        protected final GuiComboBox getParamComboBox(final String param,
                                                     final String prefix,
                                                     final int width) {
            GuiComboBox paramCb;
            final Object[] possibleChoices = getParamPossibleChoices(param);
            getResource().setPossibleChoices(param, possibleChoices);
            if (DRBD_RES_PARAM_NAME.equals(param)) {
                String resName;
                if (getResource().getValue(DRBD_RES_PARAM_NAME) == null) {
                    resName =
                            getResource().getDefaultValue(DRBD_RES_PARAM_NAME);
                } else {
                    resName = getResource().getName();
                }
                paramCb = new GuiComboBox(resName, null, null, null, width);
                paramCb.setEnabled(!getDrbdResource().isCommited());
                paramComboBoxAdd(param, prefix, paramCb);
            } else if (DRBD_RES_PARAM_DEV.equals(param)) {
                final List<String> drbdDevices = new ArrayList<String>();
                if (getResource().getValue(DRBD_RES_PARAM_DEV) == null) {
                    final String defaultItem =
                        getDrbdResource().getDefaultValue(DRBD_RES_PARAM_DEV);
                    drbdDevices.add(defaultItem);
                    int i = 0;
                    int index = 0;
                    while (i < 11) {
                        final String drbdDevStr = "/dev/drbd" 
                                                  + Integer.toString(index);
                        if (!drbdDevHash.containsKey(drbdDevStr)) {
                            drbdDevices.add(drbdDevStr);
                            i++;
                        }
                        index++;
                    }
                    paramCb = new GuiComboBox(defaultItem,
                                              drbdDevices.toArray(
                                                new String[drbdDevices.size()]),
                                              null,
                                              null,
                                              width);
                    paramCb.setEditable(true);
                } else {
                    final String defaultItem = getDevice();
                    String regexp = null;
                    if (isInteger(param)) {
                        regexp = "^-?\\d*$";
                    }
                    paramCb = new GuiComboBox(
                                        defaultItem,
                                        getResource().getPossibleChoices(param),
                                        null,
                                        regexp,
                                        width);
                }
                paramCb.setEnabled(!getDrbdResource().isCommited());
                paramComboBoxAdd(param, prefix, paramCb);

            } else if (DRBD_RES_PARAM_AFTER.equals(param)) {
                final List<Object> l = new ArrayList<Object>();
                String defaultItem =
                                getResource().getValue(DRBD_RES_PARAM_AFTER);
                final StringInfo di = new StringInfo(
                                        Tools.getString("ClusterBrowser.None"),
                                        "-1");
                l.add(di);
                if (defaultItem == null) {
                    defaultItem = Tools.getString("ClusterBrowser.None");
                }

                for (final String drbdRes : drbdResHash.keySet()) {
                    final DrbdResourceInfo r = drbdResHash.get(drbdRes);
                    DrbdResourceInfo odri = r;
                    boolean cyclicRef = false;
                    while ((odri = drbdResHash.get(
                              odri.getResource().getValue("after"))) != null) {
                        if (odri == this) {
                            cyclicRef = true;
                            // cyclic reference
                            //Tools.appError("cyclic reference: "
                            //               + odri.toString());
                        }
                    }
                    if (r != this && !cyclicRef) {
                        l.add(r);
                    }
                }

                paramCb = new GuiComboBox(defaultItem,
                                          l.toArray(new Object[l.size()]),
                                          null,
                                          null,
                                          width);

                paramComboBoxAdd(param, prefix, paramCb);
            } else if (hasUnitPrefix(param)) {
                String selectedValue = getResource().getValue(param);
                if (selectedValue == null) {
                    selectedValue = getParamDefault(param);
                }
                String unit = getUnitLong(param);
                if (unit == null) {
                    unit = "";
                }

                final int index = unit.indexOf('/');
                String unitPart = "";
                if (index > -1) {
                    unitPart = unit.substring(index);
                }

                final Unit[] units = {
                    new Unit("", "", "Byte", "Bytes"),

                    new Unit("K",
                             "k",
                             "KiByte" + unitPart,
                             "KiBytes" + unitPart),

                    new Unit("M",
                             "m",
                             "MiByte" + unitPart,
                             "MiBytes" + unitPart),

                    new Unit("G",
                             "g",
                             "GiByte" + unitPart,
                             "GiBytes" + unitPart),

                    new Unit("S",
                             "s",
                             "Sector" + unitPart,
                             "Sectors" + unitPart)
                };

                String regexp = null;
                if (isInteger(param)) {
                    regexp = "^-?\\d*$";
                }
                paramCb = new GuiComboBox(selectedValue,
                                          getPossibleChoices(param),
                                          units,
                                          GuiComboBox.Type.TEXTFIELDWITHUNIT,
                                          regexp,
                                          width);

                paramComboBoxAdd(param, prefix, paramCb);
            } else {
                paramCb = super.getParamComboBox(param, prefix, width);
                if (possibleChoices != null) {
                    paramCb.setEditable(false);
                }
            }
            return paramCb;
        }

        /**
         * Returns the DrbdResource object of this drbd resource.
         */
        private DrbdResource getDrbdResource() {
            return (DrbdResource) getResource();
        }

        /**
         * Applies changes that user made to the drbd resource fields.
         */
        public final void apply() {
            final String[] params = getParametersFromXML();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    applyButton.setEnabled(false);
                }
            });
            drbdResHash.remove(getName());
            drbdDevHash.remove(getDevice());
            storeComboBoxValues(params);

            final String name = getResource().getValue(DRBD_RES_PARAM_NAME);
            final String drbdDevStr =
                            getResource().getValue(DRBD_RES_PARAM_DEV);
            getDrbdResource().setName(name);
            setName(name);
            getDrbdResource().setDevice(drbdDevStr);

            drbdResHash.put(name, this);
            drbdDevHash.put(drbdDevStr, this);
            drbdGraph.repaint();
        }

        /**
         * Returns panel with form to configure a drbd resource.
         */
        public final JComponent getInfoPanel() {
            drbdGraph.pickInfo(this);
            if (infoPanel != null) {
                return infoPanel;
            }

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

            final JPanel extraOptionsPanel = new JPanel();
            extraOptionsPanel.setBackground(EXTRA_PANEL_BACKGROUND);
            extraOptionsPanel.setLayout(new BoxLayout(extraOptionsPanel,
                                                      BoxLayout.Y_AXIS));
            extraOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            mainPanel.add(buttonPanel);

            /* expert mode */
            buttonPanel.add(Tools.expertModeButton(extraOptionsPanel),
                                                   BorderLayout.WEST);

            /* Actions */
            final JMenuBar mb = new JMenuBar();
            mb.setBackground(PANEL_BACKGROUND);
            final JMenu serviceCombo = getActionsMenu();
            updateMenus(null);
            mb.add(serviceCombo);
            buttonPanel.add(mb, BorderLayout.EAST);

            /* resource name */
            getResource().setValue(DRBD_RES_PARAM_NAME,
                                   getDrbdResource().getName());
            getResource().setValue(DRBD_RES_PARAM_DEV,
                                   getDevice());


            final String[] params = getParametersFromXML();
            addParams(optionsPanel,
                      extraOptionsPanel,
                      params,
                      Tools.getDefaultInt("ClusterBrowser.DrbdResLabelWidth"),
                      Tools.getDefaultInt("ClusterBrowser.DrbdResFieldWidth")
                      );

            applyButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        Thread thread = new Thread(new Runnable() {
                            public void run() {
                                hbStatusLock();
                                apply();
                                try {
                                    getDrbdInfo().createDrbdConfig();
                                    for (final Host h : cluster.getHostsArray()) {
                                        DRBD.adjust(h, "all");
                                    }
                                } catch (Exceptions.DrbdConfigException dce) {
                                    hbStatusUnlock();
                                    Tools.appError("config failed");
                                }
                                hbStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );

            addApplyButton(mainPanel);
            applyButton.setEnabled(checkResourceFields(null, params));

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

        /**
         * Removes this drbd resource with confirmation dialog.
         */
        public final void removeMyself() {
            String desc = Tools.getString(
                        "ClusterBrowser.confirmRemoveDrbdResource.Description");
            desc = desc.replaceAll("@RESOURCE@", getName());
            if (Tools.confirmDialog(
                    Tools.getString(
                          "ClusterBrowser.confirmRemoveDrbdResource.Title"),
                    desc,
                    Tools.getString(
                          "ClusterBrowser.confirmRemoveDrbdResource.Yes"),
                    Tools.getString(
                          "ClusterBrowser.confirmRemoveDrbdResource.No"))) {
                removeMyselfNoConfirm();
            }
        }

        /**
         * removes this object from jtree and from list of drbd resource
         * infos without confirmation dialog.
         */
        public final void removeMyselfNoConfirm() {
            drbdGraph.removeDrbdResource(this);
            final Host[] hosts = cluster.getHostsArray();
            for (Host host : hosts) {
                DRBD.down(host, getName());
            }
            super.removeMyself();
            final DrbdResourceInfo dri = drbdResHash.get(getName());
            drbdResHash.remove(getName());
            dri.setName(null);
            reload(servicesNode);
            //reload(drbdNode);
            //getDrbdInfo().selectMyself();
            drbdDevHash.remove(getDevice());
            blockDevInfo1.removeFromDrbd();
            blockDevInfo2.removeFromDrbd();
            blockDevInfo1.removeMyself();
            blockDevInfo2.removeMyself();

            updateCommonBlockDevices();

            try {
                drbdGraph.getDrbdInfo().createDrbdConfig();
            } catch (Exceptions.DrbdConfigException dce) {
                Tools.appError("config failed");
            }
            drbdGraph.getDrbdInfo().setSelectedNode(null);
            drbdGraph.getDrbdInfo().selectMyself();
            //SwingUtilities.invokeLater(new Runnable() { public void run() {
                drbdGraph.updatePopupMenus();
            //} });
            resetFilesystems();
            infoPanel = null;
            reload(drbdNode);
        }

        /**
         * Returns string of the drbd resource.
         */
        public final String toString() {
            String name = getName();
            if (name == null) {
                name = Tools.getString("ClusterBrowser.DrbdResUnconfigured");
            }
            return "drbd: " + name;
        }

        /**
         * Returns whether two drbd resources are equal.
         */
        public final boolean equals(final Object value) {
            if (value == null) {
                return false;
            }
            if (Tools.isStringClass(value)) {
                return getDrbdResource().getValue(DRBD_RES_PARAM_DEV).equals(
                                                              value.toString());
            } else {
                if (toString() == null) {
                    return false;
                }
                return toString().equals(value.toString());
            }
        }

        //public int hashCode() {
        //    return toString().hashCode();
        //}

        /**
         * Returns the device name that is used as the string value of the drbd
         * resource in Filesystem hb service.
         */
        public final String getStringValue() {
            return getDevice();
        }

        /**
         * Adds drbddisk service in the heartbeat and graph.
         *
         * @param fi
         *              File system before which this drbd info should be
         *              started
         */
        public final void addDrbdDisk(final FilesystemInfo fi) {
            final Point2D p = null;
            final DrbddiskInfo di = (DrbddiskInfo) heartbeatGraph.getServicesInfo().addServicePanel(hbOCF.getHbDrbddisk(), p, true, null);
            //di.setResourceName(getName());
            di.setGroupInfo(fi.getGroupInfo());
            addToHeartbeatIdList(di);
            fi.setDrbddiskInfo(di);
            di.getInfoPanel();
            di.paramComboBoxGet("1", null).setValueAndWait(getName());
            di.apply();
            fi.addColocation(di);
            fi.addOrder(di);
            heartbeatGraph.addColocation(di, fi);
        }

        /**
         * Remove drbddisk heartbeat service.
         */
        public final void removeDrbdDisk(final FilesystemInfo fi) {
            final DrbddiskInfo drbddiskInfo = fi.getDrbddiskInfo();
            if (drbddiskInfo != null) {
                drbddiskInfo.removeMyselfNoConfirm();
            }
        }

        /**
         * Sets that this drbd resource is used by hb.
         */
        public final void setUsedByHeartbeat(final boolean isUsedByHeartbeat) {
            this.isUsedByHeartbeat = isUsedByHeartbeat;
        }

        /**
         * Returns whether this drbd resource is used by heartbeat.
         */
        public final boolean isUsedByHeartbeat() {
            return isUsedByHeartbeat;
        }

        /**
         * Returns common file systems. This is call from a dialog and it calls
         * the normal getCommonFileSystems function. TODO: It's a hack.
         */
        public final StringInfo[] getCommonFileSystems2(
                                                    final String defaultValue) {
            return getCommonFileSystems(defaultValue);
        }

        /**
         * Returns both hosts of the drbd connection, sorted alphabeticaly.
         */
        public final Host[] getHosts() {
            final Host h1 = blockDevInfo1.getHost();
            final Host h2 = blockDevInfo2.getHost();
            if (h1.getName().compareToIgnoreCase(h2.getName()) < 0) {
                return new Host[]{h1, h2};
            } else {
                return new Host[]{h2, h1};
            }
        }

        /**
         * Starts resolve split brain dialog.
         */
        public final void resolveSplitBrain() {
            final AddDrbdSplitBrainDialog adrd =
                                            new AddDrbdSplitBrainDialog(this);
            adrd.showDialogs();
        }

        /**
         * Returns whether the specified host has this drbd resource.
         */
        public final boolean resourceInHost(final Host host) {
            if (blockDevInfo1.getHost() == host
                || blockDevInfo2.getHost() == host) {
                return true;
            }
            return false;
        }

        /**
         * Returns the list of items for the popup menu for drbd resource.
         */
        public final List<UpdatableItem> createPopup() {
            final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

            final MyMenuItem removeResMenu = new MyMenuItem(
                            Tools.getString("ClusterBrowser.Drbd.RemoveEdge"),
                            REMOVE_ICON,
                            Tools.getString(
                                    "ClusterBrowser.Drbd.RemoveEdge.ToolTip")
                           ) {
                private static final long serialVersionUID = 1L;
                public void action() {
                    // this drbdResourceInfo remove myself and this calls
                    // removeDrbdResource in this class, that removes the edge in
                    // the graph.
                    removeMyself();
                }

                public boolean enablePredicate() {
                    return !isUsedByHeartbeat();
                }
            };
            registerMenuItem(removeResMenu);
            items.add(removeResMenu);
            final DrbdResourceInfo thisClass = this;

            final MyMenuItem connectMenu = new MyMenuItem(
                        Tools.getString("ClusterBrowser.Drbd.ResourceConnect"),
                        null,
                        Tools.getString(
                                "ClusterBrowser.Drbd.ResourceConnect.ToolTip"),

                        Tools.getString(
                                "ClusterBrowser.Drbd.ResourceDisconnect"),
                        null,
                        Tools.getString(
                            "ClusterBrowser.Drbd.ResourceDisconnect.ToolTip")
                       ) {

                private static final long serialVersionUID = 1L;

                public boolean predicate() {
                    return !isConnected();
                }

                public boolean enablePredicate() {
                    return !isSyncing();
                }

                public void action() {
                    BlockDevInfo sourceBDI = drbdGraph.getSource(thisClass);
                    BlockDevInfo destBDI   = drbdGraph.getDest(thisClass);
                    if (this.getText().equals(Tools.getString(
                                    "ClusterBrowser.Drbd.ResourceConnect"))) {
                        if (!destBDI.getBlockDevice().isConnectedOrWF()) {
                            destBDI.connect();
                        }
                        if (!sourceBDI.getBlockDevice().isConnectedOrWF()) {
                            sourceBDI.connect();
                        }
                    } else {
                        destBDI.disconnect();
                        sourceBDI.disconnect();
                    }
                }
            };
            registerMenuItem(connectMenu);
            items.add(connectMenu);

            final MyMenuItem resumeSync = new MyMenuItem(
                   Tools.getString("ClusterBrowser.Drbd.ResourceResumeSync"),
                   null,
                   Tools.getString(
                            "ClusterBrowser.Drbd.ResourceResumeSync.ToolTip"),

                   Tools.getString("ClusterBrowser.Drbd.ResourcePauseSync"),
                   null,
                   Tools.getString(
                            "ClusterBrowser.Drbd.ResourcePauseSync.ToolTip")
                  ) {
                private static final long serialVersionUID = 1L;

                public boolean predicate() {
                    return isPausedSync();
                }

                public boolean enablePredicate() {
                    return isSyncing();
                }
                public void action() {
                    BlockDevInfo sourceBDI = drbdGraph.getSource(thisClass);
                    BlockDevInfo destBDI   = drbdGraph.getDest(thisClass);
                    if (this.getText().equals(Tools.getString(
                                "ClusterBrowser.Drbd.ResourceResumeSync"))) {
                        if (destBDI.getBlockDevice().isPausedSync()) {
                            destBDI.resumeSync();
                        }
                        if (sourceBDI.getBlockDevice().isPausedSync()) {
                            sourceBDI.resumeSync();
                        }
                    } else {
                            sourceBDI.pauseSync();
                            destBDI.pauseSync();
                    }
                }
            };
            registerMenuItem(resumeSync);
            items.add(resumeSync);

            /* resolve split-brain */
            final MyMenuItem splitBrainMenu = new MyMenuItem(
                    Tools.getString("ClusterBrowser.Drbd.ResolveSplitBrain"),
                    null,
                    Tools.getString(
                                "ClusterBrowser.Drbd.ResolveSplitBrain.ToolTip")
                   ) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return isSplitBrain();
                }

                public void action() {
                    resolveSplitBrain();
                }
            };
            registerMenuItem(splitBrainMenu);
            items.add(splitBrainMenu);

            /* view log */
            final MyMenuItem viewLogMenu = new MyMenuItem(
                                Tools.getString("ClusterBrowser.Drbd.ViewLogs"),
                                null,
                                null) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return true;
                }

                public void action() {
                    final String device = getDevice();
                    final Thread thread = new Thread(
                        new Runnable() {
                            public void run() {
                                ClusterDrbdLogs l =
                                            new ClusterDrbdLogs(getCluster(),
                                                                device);
                                l.showDialog();
                            }
                        });
                    thread.start();
                }
            };
            registerMenuItem(viewLogMenu);
            items.add(viewLogMenu);
            return items;
        }

        /**
         * Sets whether the meta-data have to be created, meaning there are no
         * existing meta-data for this resource on both nodes.
         */
        public final void setHaveToCreateMD(final boolean haveToCreateMD) {
            this.haveToCreateMD = haveToCreateMD;
        }

        /**
         * Returns whether the md has to be created or not.
         */
        public final boolean isHaveToCreateMD() {
            return haveToCreateMD;
        }

        /**
         * Returns meta-disk device for the specified host.
         */
        public final String getMetaDiskForHost(final Host host) {
            return drbdXML.getMetaDisk(host.getName(), getName());
        }

        /**
         * Returns tool tip when mouse is over the resource edge.
         */
        public String getToolTipForGraph() {
            return getName();
        }
    }

    /**
     * This class holds the information about heartbeat service from the ocfs,
     * to show it to the user.
     */
    class AvailableServiceInfo extends HbCategoryInfo {
        /** Info about the service. */
        private final HeartbeatService hbService;

        /**
         * Prepares a new <code>AvailableServiceInfo</code> object.
         */
        public AvailableServiceInfo(final HeartbeatService hbService) {
            super(hbService.getName());
            this.hbService = hbService;
        }

        /**
         * Returns heartbeat service class.
         */
        public HeartbeatService getHeartbeatService() {
            return hbService;
        }

        /**
         * Returns icon for this menu category.
         */
        public ImageIcon getMenuIcon() {
            return AVAIL_SERVICES_ICON;
        }

        /**
         * Returns the info about the service.
         */
        public String getInfo() {
            final StringBuffer s = new StringBuffer(30);
            s.append("<h2>");
            s.append(getName());
            s.append(" (");
            s.append(hbOCF.getVersion(hbService));
            s.append(")</h2><h3>");
            s.append(hbOCF.getShortDesc(hbService));
            s.append("</h3>");
            s.append(hbOCF.getLongDesc(hbService));
            final String[] params = hbOCF.getParameters(hbService);
            for (String param : params) {
                s.append(hbOCF.getParamLongDesc(hbService, param));
                s.append("<br>");
            }
            return s.toString();
        }
    }

    /**
     * This class holds info data for a block device that is common
     * in all hosts in the cluster and can be chosen in the scrolling list in
     * the filesystem service.
     */
    class CommonBlockDevInfo extends HbCategoryInfo
                             implements CommonDeviceInterface {
        /** block devices of this common block device on all nodes. */
        private final BlockDevice[] blockDevices;

        /**
         * Prepares a new <code>CommonBlockDevInfo</code> object.
         */
        public CommonBlockDevInfo(final String name,
                                  final BlockDevice[] blockDevices) {
            super(name);
            setResource(new CommonBlockDevice(name));
            this.blockDevices = blockDevices;
        }

        /**
         * Returns icon for common block devices menu category.
         */
        public ImageIcon getMenuIcon() {
            return COMMON_BD_ICON;
        }

        /**
         * Returns device name of this block device.
         */
        public String getDevice() {
            return getCommonBlockDevice().getDevice();
        }

        /**
         * Returns info for this block device.
         */
        public String getInfo() {
            return "Device    : " + getCommonBlockDevice().getName() + "\n";
        }

        /**
         * Returns string representation of the block devices, used in the pull
         * down menu.
         */
        public String toString() {
            String name = getName();
            if (name == null) {
                name = Tools.getString(
                                   "ClusterBrowser.CommonBlockDevUnconfigured");
            }
            return name;
        }

        /**
         * Sets this block device on all nodes ass used by heartbeat.
         */
        public void setUsedByHeartbeat(final boolean isUsedByHeartbeat) {
            for (BlockDevice bd : blockDevices) {
                bd.setUsedByHeartbeat(isUsedByHeartbeat);
            }
        }

        /**
         * Returns if all of the block devices are used by heartbeat.
         * TODO: or any is used by hb?
         */
        public boolean isUsedByHeartbeat() {
            boolean is = true;
            for (int i = 0; i < blockDevices.length; i++) {
                is = is && blockDevices[i].isUsedByHeartbeat();
            }
            return is;
        }

        /**
         * Retruns resource object of this block device.
         */
        public CommonBlockDevice getCommonBlockDevice() {
            return (CommonBlockDevice) getResource();
        }
    }

    /**
     * This class holds info about IPaddr/IPaddr2 heartbeat service. It adds a
     * better ip entering capabilities.
     */
    class IPaddrInfo extends ServiceInfo {
        /**
         * Creates new IPaddrInfo object.
         */
        public IPaddrInfo(final String name, final HeartbeatService hbService) {
            super(name, hbService);
        }

        /**
         * Creates new IPaddrInfo object.
         */
        public IPaddrInfo(final String name,
                          final HeartbeatService hbService,
                          final String hbId,
                          final Map<String, String> resourceNode) {
            super(name, hbService, hbId, resourceNode);
        }

        /**
         * Adds if field.
         */
        protected void addIdField(final JPanel panel,
                                  final int leftWidth,
                                  final int rightWidth) {
            super.addIdField(panel, leftWidth, rightWidth);
        }

        /**
         * Returns whether all the parameters are correct. If param is null,
         * all paremeters will be checked, otherwise only the param, but other
         * parameters will be checked only in the cache. This is good if only
         * one value is changed and we don't want to check everything.
         */
        public boolean checkResourceFieldsCorrect(final String param,
                                                  final String[] params) {
            boolean ret = super.checkResourceFieldsCorrect(param, params);
            final GuiComboBox cb;
            if (getHeartbeatService().isHeartbeatClass()) {
                cb = paramComboBoxGet("1", null);
            } else if (getHeartbeatService().isOCFClass()) {
                cb = paramComboBoxGet("ip", null);
            } else {
                return true;
            }
            if (cb == null) {
                return false;
            }
            cb.setEditable(true);
            cb.selectSubnet();
            if (ret) {
                final String ip = cb.getStringValue();
                if (!Tools.checkIp(ip)) {
                    ret = false;
                }
            }
            return ret;
        }

        /**
         * Returns combo box for parameter.
         */
        protected GuiComboBox getParamComboBox(final String param,
                                               final String prefix,
                                               final int width) {
            GuiComboBox paramCb;
            if ("ip".equals(param)) {
                /* get networks */
                final String ip = getResource().getValue("ip");
                Info defaultValue;
                if (ip == null) {
                    defaultValue = new StringInfo(
                        Tools.getString("ClusterBrowser.SelectNetInterface"),
                        null);
                } else {
                    defaultValue = new StringInfo(ip, ip);
                }
                final Info[] networks =
                                    enumToInfoArray(defaultValue,
                                                    getName(),
                                                    networksNode.children());

                final String regexp = "^[\\d.*]*|Select\\.\\.\\.$";
                paramCb = new GuiComboBox(ip,
                                          networks,
                                          GuiComboBox.Type.COMBOBOX,
                                          regexp,
                                          width);

                paramCb.setAlwaysEditable(true);
                paramComboBoxAdd(param, prefix, paramCb);
            } else {
                paramCb = super.getParamComboBox(param, prefix, width);
            }
            return paramCb;
        }

        /**
         * Returns string representation of the ip address.
         * In the form of 'ip (interface)'
         */
        public String toString() {
            final String id = getService().getId();
            if (id == null) {
                return super.toString(); // this is for 'new IPaddrInfo'
            }

            final StringBuffer s = new StringBuffer(getName());
            String inside = "";
            if (!id.matches("^\\d+$")) {
                inside = id + " / ";
            }
            String ip = getResource().getValue("ip");
            if (ip == null) {
                ip = Tools.getString("ClusterBrowser.Ip.Unconfigured");
            }
            s.append(" (" + inside + ip + ")");

            return s.toString();
        }
    }

    /**
     * This class holds info about Filesystem service. It is treated in special
     * way, so that it can use block device information and drbd devices. If
     * drbd device is selected, the drbddisk service will be added too.
     */
    class FilesystemInfo extends ServiceInfo {
        /** drbddisk service object. */
        private DrbddiskInfo drbddiskInfo = null;

        /**
         * Creates the FilesystemInfo object.
         */
        public FilesystemInfo(final String name,
                              final HeartbeatService hbService) {
            super(name, hbService);
        }

        /**
         * Creates the FilesystemInfo object.
         */
        public FilesystemInfo(final String name,
                              final HeartbeatService hbService,
                              final String hbId,
                              final Map<String, String> resourceNode) {
            super(name, hbService, hbId, resourceNode);
        }

        /**
         * Sets DrbddiskInfo object for this Filesystem service if it uses drbd
         * block device.
         */
        public void setDrbddiskInfo(final DrbddiskInfo drbddiskInfo) {
            this.drbddiskInfo = drbddiskInfo;
        }

        /**
         * Returns DrbddiskInfo object that is associated with the drbd device
         * or null if it is not a drbd device.
         */
        public DrbddiskInfo getDrbddiskInfo() {
            return drbddiskInfo;
        }

        /**
         * Adds id field.
         */
        protected void addIdField(final JPanel panel,
                                  final int leftWidth,
                                  final int rightWidth) {
            super.addIdField(panel, leftWidth, rightWidth);
        }

        /**
         * Returns whether all the parameters are correct. If param is null,
         * all paremeters will be checked, otherwise only the param, but other
         * parameters will be checked only in the cache. This is good if only
         * one value is changed and we don't want to check everything.
         */
        public boolean checkResourceFieldsCorrect(final String param,
                                                  final String[] params) {
            final boolean ret = super.checkResourceFieldsCorrect(param, params);
            if (!ret) {
                return false;
            }
            final GuiComboBox cb = paramComboBoxGet(DRBD_RES_PARAM_DEV, null);
            if (cb == null || cb.getValue() == null) {
                return false;
            }
            return true;
        }

        /**
         * Applies changes to the Filesystem service paramters.
         */
        public void apply() {
            final String dir = getComboBoxValue("directory");
            for (Host host : getClusterHosts()) {
                final String ret = Tools.execCommandProgressIndicator(
                                                       host,
                                                       "stat -c \"%F\" " + dir,
                                                       null,
                                                       true);

                if (ret == null || !"directory\r\n".equals(ret)) {
                    String title =
                            Tools.getString("ClusterBrowser.CreateDir.Title");
                    String desc  =
                        Tools.getString("ClusterBrowser.CreateDir.Description");
                    title = title.replaceAll("@DIR@", dir);
                    title = title.replaceAll("@HOST@", host.getName());
                    desc  = desc.replaceAll("@DIR@", dir);
                    desc  = desc.replaceAll("@HOST@", host.getName());
                    if (Tools.confirmDialog(
                            title,
                            desc,
                            Tools.getString("ClusterBrowser.CreateDir.Yes"),
                            Tools.getString("ClusterBrowser.CreateDir.No"))) {
                        Tools.execCommandProgressIndicator(host, "mkdir " + dir, null, true);
                    }
                }
            }
            super.apply();
            //TODO: escape dir
        }

        /**
         * Returns editable element for the parameter.
         */
        protected GuiComboBox getParamComboBox(final String param,
                                               final String prefix,
                                               final int width) {
            GuiComboBox paramCb;
            if (DRBD_RES_PARAM_DEV.equals(param)) {
                final String selectedValue =
                                    getResource().getValue(DRBD_RES_PARAM_DEV);
                Info defaultValue = null;
                if (selectedValue == null) {
                    defaultValue = new StringInfo(
                            Tools.getString("ClusterBrowser.SelectBlockDevice"),
                            null);
                }
                final Info[] commonBlockDevInfos =
                                        getCommonBlockDevInfos(defaultValue,
                                                               getName());
                paramCb = new GuiComboBox(selectedValue,
                                          commonBlockDevInfos,
                                          null,
                                          null,
                                          width);

                paramComboBoxAdd(param, prefix, paramCb);
            } else if ("fstype".equals(param)) {
                final String defaultValue =
                            Tools.getString("ClusterBrowser.SelectFilesystem");
                final String selectedValue = getResource().getValue("fstype");
                paramCb = new GuiComboBox(selectedValue,
                                          getCommonFileSystems(defaultValue),
                                          null,
                                          null,
                                          width);

                paramComboBoxAdd(param, prefix, paramCb);
                paramCb.setEditable(false);
            } else if ("directory".equals(param)) {
                Object[] items = new Object[commonMountPoints.length + 1];
                System.arraycopy(commonMountPoints,
                                 0,
                                 items,
                                 1,
                                 commonMountPoints.length);
                items[0] = new StringInfo(
                            Tools.getString("ClusterBrowser.SelectMountPoint"),
                            null);
                //for (int i = 0; i < commonMountPoints.length; i++) {
                //    items[i + 1] = commonMountPoints[i];
                //}
                getResource().setPossibleChoices(param, items);
                final String selectedValue =
                                            getResource().getValue("directory");
                final String regexp = "^/.*$";
                paramCb = new GuiComboBox(selectedValue,
                                          items,
                                          null,
                                          regexp,
                                          width);
                paramComboBoxAdd(param, prefix, paramCb);
                paramCb.setAlwaysEditable(true);
            } else {
                paramCb = super.getParamComboBox(param, prefix, width);
            }
            return paramCb;
        }

        /**
         * Returns string representation of the filesystem service.
         */
        public String toString() {
            String id = getService().getId();
            if (id == null) {
                return super.toString(); // this is for 'new Filesystem'
            }

            final StringBuffer s = new StringBuffer(getName());
            final DrbdResourceInfo dri =
                drbdDevHash.get(getResource().getValue(DRBD_RES_PARAM_DEV));
            if (dri == null) {
                id = getResource().getValue(DRBD_RES_PARAM_DEV);
            } else {
                id = dri.getName();
                s.delete(0, s.length());
                s.append("Filesystem / Drbd");
            }
            if (id == null) {
                id = Tools.getString(
                            "ClusterBrowser.ClusterBlockDevice.Unconfigured");
            }
            s.append(" (" + id + ")");

            return s.toString();
        }

        /**
         * Adds DrbddiskInfo before the filesysteminfo is added.
         */
        public void addResourceBefore() {
            final DrbdResourceInfo oldDri =
                    drbdDevHash.get(getResource().getValue(DRBD_RES_PARAM_DEV));
            final DrbdResourceInfo newDri =
                    drbdDevHash.get(getComboBoxValue(DRBD_RES_PARAM_DEV));
            if (newDri.equals(oldDri)) {
                return;
            }
            final DrbddiskInfo oddi = getDrbddiskInfo();
            if (oldDri != null) {
                oldDri.removeDrbdDisk(this);
                oldDri.setUsedByHeartbeat(false);
                setDrbddiskInfo(null);
            }

            if (newDri != null) {
                newDri.setUsedByHeartbeat(true);
                newDri.addDrbdDisk(this);
            }
            //if (oddi != null) {
            //    oddi.cleanupResource();
            //}
        }
    }

    /**
     * DrbddiskInfo class is used for drbddisk heartbeat service that is
     * treated in special way.
     */
    class DrbddiskInfo extends ServiceInfo {

        /**
         * Creates new DrbddiskInfo object.
         */
        public DrbddiskInfo(final String name,
                            final HeartbeatService hbService) {
            super(name, hbService);
        }

        /**
         * Creates new DrbddiskInfo object.
         */
        public DrbddiskInfo(final String name,
                            final HeartbeatService hbService,
                            final String resourceName) {
            super(name, hbService);
            getResource().setValue("1", resourceName);
        }

        /**
         * Creates new DrbddiskInfo object.
         */
        public DrbddiskInfo(final String name,
                            final HeartbeatService hbService,
                            final String hbId,
                            final Map<String, String> resourceNode) {
            super(name, hbService, hbId, resourceNode);
        }

        /**
         * Returns string representation of the drbddisk service.
         */
        public String toString() {
            return getName() + " (" + getResource().getValue("1") + ")";
        }

        /**
         * Returns resource name / parameter "1".
         */
        public String getResourceName() {
            return getResource().getValue("1");
        }
 
        /**
         * Sets resource name / parameter "1".
         */
        public void setResourceName(final String resourceName) {
            getResource().setValue("1", resourceName);
        }

        /**
         * Removes the drbddisk service.
         */
        public void removeMyselfNoConfirm() {
            super.removeMyselfNoConfirm();
            final DrbdResourceInfo dri = drbdResHash.get(getResourceName());
            if (dri != null) {
                dri.setUsedByHeartbeat(false);
            }
        }
    }

    /**
     * GroupInfo class holds data for heartbeat group, that is in some ways
     * like normal service, but it can contain other services.
     */
    class GroupInfo extends ServiceInfo {
        // should extend EditableInfo: TODO

        /**
         * Creates new GroupInfo object.
         */
        public GroupInfo(final HeartbeatService hbService) {
            super(HB_GROUP_NAME, hbService);
        }

        /**
         * Returns all group parameters. (empty)
         */
        public String[] getParametersFromXML() {
            return new String[]{};
        }

        /**
         * Applies the changes to the group parameters.
         */
        public void apply() {
            final String[] params = getParametersFromXML();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    applyButton.setEnabled(false);
                    idField.setEnabled(false);
                }
            });

            /* add myself to the hash with service name and id as
             * keys */
            removeFromServiceInfoHash(this);
            final String oldHeartbeatId = getService().getHeartbeatId();
            if (oldHeartbeatId != null) {
                heartbeatIdToServiceInfo.remove(oldHeartbeatId);
                heartbeatIdList.remove(oldHeartbeatId);
            }
            final String id = idField.getStringValue();
            getService().setId(id);
            addToHeartbeatIdList(this);
            addNameToServiceInfoHash(this);

            /*
                MSG_ADD_GRP group
                        param_id1 param_name1 param_value1
                        param_id2 param_name2 param_value2
                        ...
                        param_idn param_namen param_valuen
            */
            final String heartbeatId     = getService().getHeartbeatId();
            if (getService().isNew()) {
                final String[] parents = heartbeatGraph.getParents(this);

                Heartbeat.setOrderAndColocation(getDCHost(),
                                                heartbeatId,
                                                parents);
            } else {
                // update parameters
                //String heartbeatId = getService().getHeartbeatId();
                final StringBuffer args = new StringBuffer(heartbeatId);
                for (String param : params) {
                    final String oldValue = getResource().getValue(param);
                    String value = getComboBoxValue(param);
                    if (value.equals(oldValue)) {
                        continue;
                    }
                    if (value.equals(getParamDefault(param))) {
                        continue;
                    }

                    if ("".equals(value)) {
                        value = getParamDefault(param);
                    }
                    args.append(' ');
                    args.append(heartbeatId);
                    args.append('-');
                    args.append(param);
                    args.append(' ');
                    args.append(param);
                    args.append(" \"");
                    args.append(value);
                    args.append('"');
                }
                Heartbeat.setGroupParameters(getDCHost(),
                                             heartbeatId,
                                             args.toString());
            }
            setLocations(heartbeatId);
            storeComboBoxValues(params);

            reload(getNode());
            heartbeatGraph.repaint();
        }

        /**
         * Returns the list of services that can be added to the group.
         */
        public List<HeartbeatService> getAddGroupServiceList(final String cl) {
            return hbOCF.getServices(cl);
        }

        /**
         * Adds service to this group. Adds it in the submenu in the menu tree
         * and initializes it.
         *
         * @param newServiceInfo
         *      service info object of the new service
         */
        public void addGroupServicePanel(final ServiceInfo newServiceInfo) {
            newServiceInfo.getService().setHeartbeatClass(
                    newServiceInfo.getHeartbeatService().getHeartbeatClass());
            newServiceInfo.setGroupInfo(this);

            addToHeartbeatIdList(newServiceInfo);

            final DefaultMutableTreeNode newServiceNode =
                                    new DefaultMutableTreeNode(newServiceInfo);
            newServiceInfo.setNode(newServiceNode);
            getNode().add(newServiceNode);
            reload(getNode()); // TODO: something will not work I guess
            reload(newServiceNode);
        }

        /**
         * Adds service to this group and creates new service info object.
         */
        public void addGroupServicePanel(final HeartbeatService newHbService) {
            ServiceInfo newServiceInfo;

            final String name = newHbService.getName();
            if (newHbService.isFilesystem()) {
                newServiceInfo = new FilesystemInfo(name, newHbService);
            } else if (newHbService.isDrbddisk()) {
                newServiceInfo = new DrbddiskInfo(name, newHbService);
            } else if (newHbService.isIPaddr()) {
                newServiceInfo = new IPaddrInfo(name, newHbService);
            } else if (newHbService.isGroup()) {
                Tools.appError("No groups in group allowed");
                return;
            } else {
                newServiceInfo = new ServiceInfo(name, newHbService);
            }
            addGroupServicePanel(newServiceInfo);
        }

        /**
         * Returns on which node this group is running, meaning on which node
         * all the services are running. Null if they running on different
         * nodes or not at all.
         */
        public String getRunningOnNode() {
            String node = null;
            final List<String> resources = heartbeatStatus.getGroupResources(
                                                getService().getHeartbeatId());
            if (resources != null) {
                for (final String hbId : resources) {
                    final String n = heartbeatStatus.getRunningOnNode(hbId);
                    if (node == null) {
                        node = n;
                    } else if (!node.toLowerCase().equals(n.toLowerCase())) {
                        return null;
                    }
                }
            }
            return node;
        }

        /**
         * Returns items for the group popup.
         */
        public List<UpdatableItem> createPopup() {
            final List<UpdatableItem>items = super.createPopup();
            /* add group service */
            final MyMenu addGroupServiceMenuItem =new MyMenu(
                        Tools.getString("ClusterBrowser.Hb.AddGroupService")) {
                private static final long serialVersionUID = 1L;

                public void update() {
                    super.update();

                    removeAll();
                    for (final String cl : HB_CLASSES) {
                        final MyMenu classItem =
                                            new MyMenu(HB_CLASS_MENU.get(cl));
                        DefaultListModel m = new DefaultListModel();
                        for (final HeartbeatService hbService 
                                                : getAddGroupServiceList(cl)) {
                            final MyMenuItem mmi =
                                    new MyMenuItem(hbService.getMenuName()) {
                                private static final long serialVersionUID = 1L;
                                public void action() {
                                    getPopup().setVisible(false);
                                    addGroupServicePanel(hbService);
                                    repaint();
                                }
                            };
                            m.addElement(mmi);
                        }
                        classItem.add(Tools.getScrollingMenu(classItem, m));
                        add(classItem);
                    }
                }
            };
            items.add(1, (UpdatableItem) addGroupServiceMenuItem);
            registerMenuItem((UpdatableItem) addGroupServiceMenuItem);

            return items;
        }

        /**
         * Removes this group from the heartbeat.
         */
        public void removeMyself() {
            getService().setRemoved(true);
            String desc = Tools.getString(
                            "ClusterBrowser.confirmRemoveGroup.Description");

            final StringBuffer services = new StringBuffer();

            final Enumeration e = getNode().children();
            while (e.hasMoreElements()) {
                DefaultMutableTreeNode n =
                                    (DefaultMutableTreeNode) e.nextElement();
                final ServiceInfo child = (ServiceInfo) n.getUserObject();
                services.append(child.toString());
                if (e.hasMoreElements()) {
                    services.append(", ");
                }

            }

            desc  = desc.replaceAll("@GROUP@", "'" + toString() + "'");
            desc  = desc.replaceAll("@SERVICES@", services.toString());
            if (Tools.confirmDialog(
                    Tools.getString("ClusterBrowser.confirmRemoveGroup.Title"),
                    desc,
                    Tools.getString("ClusterBrowser.confirmRemoveGroup.Yes"),
                    Tools.getString("ClusterBrowser.confirmRemoveGroup.No"))) {
                removeMyselfNoConfirm();
            }
            getService().doneRemoving();
            getService().setNew(false);
        }

        /**
         * Remove all the services in the group and the group.
         */
        public void removeMyselfNoConfirm() {
            super.removeMyselfNoConfirm();
            if (!getService().isNew()) {
                for (String hbId : heartbeatStatus.getGroupResources(
                                            getService().getHeartbeatId())) {
                    final ServiceInfo child =
                                            heartbeatIdToServiceInfo.get(hbId);
                    child.removeMyselfNoConfirm();
                }
            }
        }

        /**
         * Removes the group, but not the services.
         */
        public void removeMyselfNoConfirmFromChild() {
            super.removeMyselfNoConfirm();
        }

        /**
         * Returns tool tip for the group vertex.
         */
        public String getToolTipText() {
            String hostName = getRunningOnNode();
            if (hostName == null) {
                hostName = "none";
            }
            final StringBuffer sb = new StringBuffer(220);
            sb.append("<b>");
            sb.append(toString());
            sb.append(" running on node: ");
            sb.append(hostName);
            sb.append("</b>");

            final Enumeration e = getNode().children();
            while (e.hasMoreElements()) {
                DefaultMutableTreeNode n =
                                    (DefaultMutableTreeNode) e.nextElement();
                final ServiceInfo child = (ServiceInfo) n.getUserObject();
                sb.append('\n');
                sb.append(child.getToolTipText());
            }

            return sb.toString();
        }
    }

    /**
     * This class holds info data for one hearteat service and allows to enter
     * its arguments and execute operations on it.
     */
    class ServiceInfo extends EditableInfo {
        /** This is a map from host to the combobox with scores. */
        private final Map<HostInfo, GuiComboBox> scoreComboBoxHash =
                                    new HashMap<HostInfo, GuiComboBox>();
        /** A map from host to stored score. */
        private Map<HostInfo, HostScoreInfo> savedHostScoreInfos =
                                    new HashMap<HostInfo, HostScoreInfo>();
        /** A map from operation to the stored value. First key is
         * operation name like "start" and second key is parameter like
         * "timeout". */
        private MultiKeyMap savedOperation = new MultiKeyMap();
        /** A map from operation to its combo box. */
        private MultiKeyMap operationsComboBoxHash = new MultiKeyMap();
        /** idField text field. */
        protected GuiComboBox idField = null;
        /** Cache for the info panel. */
        private JComponent infoPanel = null;
        /** Group info object of the group this service is in or null, if it is
         * not in any group. */
        private GroupInfo groupInfo = null;
        /** HeartbeatService object of the service, with name, ocf informations
         * etc. */
        private final HeartbeatService hbService;

        /**
         * Prepares a new <code>ServiceInfo</code> object and creates
         * new service object.
         */
        public ServiceInfo(final String name,
                           final HeartbeatService hbService) {
            super(name);
            this.hbService = hbService;
            setResource(new Service(name));

            /* init save button */
            initApplyButton();

            getService().setNew(true);
        }

        /**
         * Prepares a new <code>ServiceInfo</code> object and creates
         * new service object. It also initializes parameters along with
         * heartbeat id with values from xml stored in resourceNode.
         */
        public ServiceInfo(final String name,
                           final HeartbeatService hbService,
                           final String heartbeatId,
                           final Map<String, String> resourceNode) {
            this(name, hbService);
            getService().setHeartbeatId(heartbeatId);
            setParameters(resourceNode);
        }

        /**
         * Returns id of the service, which is heartbeatId.
         */
        public String getId() {
            return getService().getHeartbeatId();
        }

        /**
         * Sets info panel of the service.
         * TODO: is it used?
         */
        public void setInfoPanel(final JPanel infoPanel) {
            this.infoPanel = infoPanel;
        }

        /**
         * Returns true if the node is active.
         */
        public boolean isActiveNode(final String node) {
            return heartbeatStatus.isActiveNode(node);
        }

        /**
         * Returns whether all the parameters are correct. If param is null,
         * all paremeters will be checked, otherwise only the param, but other
         * parameters will be checked only in the cache. This is good if only
         * one value is changed and we don't want to check everything.
         */
        public boolean checkResourceFieldsCorrect(final String param,
                                                  final String[] params) {
            if (!super.checkResourceFieldsCorrect(param, params)) {
                return false;
            }
            if (idField == null) {
                return false;
            }
            final String id = idField.getStringValue();
            // TODO: check uniq id
            if (id == null || id.equals("")) {
                return false;
            }
            return true;
        }

        /**
         * Returns whether the specified parameter or any of the parameters
         * have changed. If param is null, only param will be checked,
         * otherwise all parameters will be checked.
         */
        protected boolean checkResourceFieldsChanged(final String param,
                                                     final String[] params) {
            boolean ret;
            if (super.checkResourceFieldsChanged(param, params)) {
                ret = true;
            } else {
                final String id = idField.getStringValue();
                final String heartbeatId = getService().getHeartbeatId();
                if (HB_GROUP_NAME.equals(getName())) {
                    if (heartbeatId.equals("grp_" + id)
                        || heartbeatId.equals(id)) {
                        ret = checkHostScoreFieldsChanged() || checkOperationFieldsChanged();
                    } else {
                        ret = true;
                    }
                } else {
                    if (HB_GROUP_NAME.equals(getName())) {
                        if (heartbeatId.equals("grp_" + id)
                            || heartbeatId.equals(id)) {
                            ret = checkHostScoreFieldsChanged() || checkOperationFieldsChanged();
                        } else {
                            ret = true;
                        }
                    } else {
                        if (heartbeatId.equals("res_" + getName() + "_" + id)
                            || heartbeatId.equals(id)) {
                            ret = checkHostScoreFieldsChanged() || checkOperationFieldsChanged();
                        } else {
                            ret = true;
                        }
                    }
                }
            }
            final String cl = getService().getHeartbeatClass();
            if (cl != null && cl.equals(HB_HEARTBEAT_CLASS)) {
                /* in old style resources don't show all the textfields */
                boolean visible = false;
                GuiComboBox cb = null;
                for (int i = params.length - 1; i >= 0; i--) {
                    final GuiComboBox prevCb = paramComboBoxGet(params[i],
                                                                null);
                    if (prevCb == null) {
                        continue;
                    }
                    if (!visible && !prevCb.getStringValue().equals("")) {
                        visible = true;
                    }
                    if (cb != null && cb.isVisible() != visible) {
                        final boolean v = visible;
                        final GuiComboBox c = cb;
                        SwingUtilities.invokeLater(
                            new Runnable() {
                                public void run() {
                                    c.setVisible(v);
                                    getLabel(c).setVisible(v);
                                }
                            });
                    }
                    cb = prevCb;
                }
            }
            return ret;
        }

        /**
         * Sets service parameters with values from resourceNode hash.
         */
        public void setParameters(final Map<String, String> resourceNode) {
            if (hbOCF == null) {
                Tools.appError("hbOCF is null");
                return;
            }
            final String[] params = hbOCF.getParameters(hbService);
            if (params != null) {
                for (String param : params) {
                    String value = resourceNode.get(param);
                    if (value == null) {
                        value = getParamDefault(param);
                    }
                    if (value == null) {
                        value = "";
                    }
                    if (!value.equals(getResource().getValue(param))) {
                        getResource().setValue(param, value);
                        if (infoPanel != null) {
                            final GuiComboBox cb = paramComboBoxGet(param,
                                                                    null);
                            if (cb != null) {
                                cb.setValue(value);
                            }
                        }
                    }
                }
            }

            /* scores */
            for (Host host : getClusterHosts()) {
                final HostInfo hi = host.getBrowser().getHostInfo();
                String score = heartbeatStatus.getScore(
                                            getService().getHeartbeatId(),
                                            hi.getName());
                if (score == null) {
                    score = "0";
                }
                final String scoreString = Tools.scoreToString(score);
                if (!hostScoreInfoMap.get(scoreString).equals(savedHostScoreInfos.get(hi))) {
                    final GuiComboBox cb = scoreComboBoxHash.get(hi);
                    savedHostScoreInfos.put(hi, hostScoreInfoMap.get(scoreString));
                    if (cb != null) {
                        cb.setValue(scoreString);
                    }
                }
            }

            /* operations */
            for (final String op : HB_OPERATIONS) {
                for (final String param : HB_OPERATION_PARAMS.get(op)) {
                    final GuiComboBox cb =
                            (GuiComboBox) operationsComboBoxHash.get(op, param);
                    String value = heartbeatStatus.getOperation(
                                                getService().getHeartbeatId(),
                                                op,
                                                param);
                    if (value == null) {
                        value = "";
                    }
                    if (!value.equals(savedOperation.get(op, param))) {
                        savedOperation.put(op, param, value);
                        if (cb != null && value != null) {
                            cb.setValue(value);
                        }
                    }
                }
            }

            getService().setAvailable();
        }

        /**
         * Returns a name of the service with id in the parentheses.
         * It adds prefix 'new' if id is null.
         */
        public String toString() {
            final StringBuffer s = new StringBuffer(getName());
            final String string = getService().getId();

            /* 'string' contains the last string if there are more dependent
             * resources, although there is usually only one. */
            if (string == null) {
                s.insert(0, "new ");
            } else {
                if (!"".equals(string)) {
                    s.append(" (" + string + ")");
                }
            }
            return s.toString();
        }

        /**
         * Sets id in the service object.
         */
        public void setId(final String id) {
            getService().setId(id);
        }

        /**
         * Returns node name of the host where this service is running.
         */
        public String getRunningOnNode() {
            return heartbeatStatus.getRunningOnNode(
                                                getService().getHeartbeatId());
        }

       /**
        * Returns whether service is started.
        */
        public boolean isStarted() {
            final String targetRole =
                heartbeatStatus.getParameter(getService().getHeartbeatId(),
                                             "target_role");
            if (targetRole != null && targetRole.equals("started")) {
                return true;
            }
            return false;
        }

       /**
        * Returns whether service is stopped.
        */
        public boolean isStopped() {
            final String targetRole =
                heartbeatStatus.getParameter(getService().getHeartbeatId(),
                                             "target_role");
            if (targetRole != null && targetRole.equals("stopped")) {
                return true;
            }
            return false;
        }

        /**
         * Returns whether service is managed.
         * TODO: "default" value
         */
        final public boolean isManaged() {
            String status =
                      heartbeatStatus.getStatus(getService().getHeartbeatId());
            return status != null && !status.equals("unmanaged");
            //final String isManaged =
            //    heartbeatStatus.getParameter(getService().getHeartbeatId(),
            //                                 "is_managed");
            //if (isManaged == null || isManaged.equals("true")) {
            //    return true;
            //}
            //return false;
        }

        /**
         * Returns whether the resource has failed to start. As far as I know
         * you can say only if service should be managed but is unmanaged.
         */
        final public boolean isFailed() {
            String status =
                      heartbeatStatus.getStatus(getService().getHeartbeatId());
            return isStarted() && status != null && status.equals("not running");
        }

        /**
         * Sets whether the service is managed.
         */
        final public void setManaged(final String isManaged) {
            Heartbeat.setManaged(getDCHost(),
                                 getService().getHeartbeatId(),
                                 isManaged);
        }

        /**
         * Returns color for the host vertex.
         */
        public Color getHostColor() {
            return cluster.getHostColor(getRunningOnNode());
        }

        /**
         * Returns service icon in the menu. It can be started or stopped.
         * TODO: broken icon, not managed icon.
         */
        public ImageIcon getMenuIcon() {
            if (isStopped()) {
                return SERVICE_STOPPED_ICON;
            } else if (isStarted()) {
                return SERVICE_STARTED_ICON;
            }
            if (getRunningOnNode() == null) {
                return SERVICE_STOPPED_ICON;
            }
            return SERVICE_STARTED_ICON;
        }

        /**
         * Saves the host score infos.
         * TODO: check it.
         */
        public void setSavedHostScoreInfos(
                                    final Map<HostInfo, HostScoreInfo> hsi) {
            savedHostScoreInfos = hsi;
        }

        /**
         * Gets saved host score infos.
         */
        public Map<HostInfo, HostScoreInfo> getSavedHostScoreInfos() {
            return savedHostScoreInfos;
        }


        /**
         * Returns list of all host names in this cluster.
         */
        public List<String> getHostNames() {
            final List<String> hostNames = new ArrayList<String>();
            final Enumeration e = clusterHostsNode.children();
            while (e.hasMoreElements()) {
                DefaultMutableTreeNode n =
                                    (DefaultMutableTreeNode) e.nextElement();
                final String hostName =
                                    ((HostInfo) n.getUserObject()).getName();
                hostNames.add(hostName);
            }
            return hostNames;
        }

        /**
         * TODO: wrong doku
         * Converts enumeration to the info array, get objects from
         * hash if they exist.
         */
        protected Info[] enumToInfoArray(final Info defaultValue,
                                         final String serviceName,
                                         final Enumeration e) {
            final List<Info> list = new ArrayList<Info>();
            if (defaultValue != null) {
                list.add(defaultValue);
            }

            while (e.hasMoreElements()) {
                DefaultMutableTreeNode n =
                                      (DefaultMutableTreeNode) e.nextElement();
                final Info i =(Info) n.getUserObject();
                final String name = i.getName();
                final ServiceInfo si = getServiceInfoFromId(serviceName,
                                                            i.getName());

                if (si == null && !name.equals(defaultValue)) {
                    list.add(i);
                }
            }
            return list.toArray(new Info[list.size()]);
        }

        /**
         * Stores score infos for host.
         */
        private void storeHostScoreInfos() {
            savedHostScoreInfos.clear();
            for (Host host : getClusterHosts()) {
                final HostInfo hi = host.getBrowser().getHostInfo();
                final GuiComboBox cb = scoreComboBoxHash.get(hi);
                final HostScoreInfo hsi = (HostScoreInfo) cb.getValue();
                savedHostScoreInfos.put(hi, hsi);
            }
            // TODO: rename this
            heartbeatGraph.setHomeNode(this, savedHostScoreInfos);
        }

        /**
         * Returns thrue if an operation field changed.
         */
        private boolean checkOperationFieldsChanged() {
            for (final String op : HB_OPERATIONS) {
                for (final String param : HB_OPERATION_PARAMS.get(op)) {
                    final GuiComboBox cb =
                            (GuiComboBox) operationsComboBoxHash.get(op, param);
                    if (cb == null) {
                        return false;
                    }
                    final String value = cb.getStringValue();
                    final String savedOp =
                                        (String) savedOperation.get(op, param);
                    final String defaultValue =
                                    hbService.getOperationDefault(op, param);
                    if (savedOp == null) {
                        if (value != null && !value.equals(defaultValue)) {
                            return true;
                        }
                    } else if (!savedOp.equals(value)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Returns true if some of the scores have changed.
         */
        private boolean checkHostScoreFieldsChanged() {
            for (Host host : getClusterHosts()) {
                final HostInfo hi = host.getBrowser().getHostInfo();
                final GuiComboBox cb = scoreComboBoxHash.get(hi);
                final HostScoreInfo hsiSaved = savedHostScoreInfos.get(hi);
                if (cb == null) {
                    return false;
                }
                final HostScoreInfo hsi = (HostScoreInfo) cb.getValue();
                if (hsiSaved == null && !hsi.getScore().equals("0")) {
                    return true;
                } else if (hsiSaved != null && !hsi.equals(hsiSaved)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns the list of all services, that can be used in the 'add
         * service' action.
         */
        public List<HeartbeatService> getAddServiceList(final String cl) {
            return globalGetAddServiceList(cl);
        }

        /**
         * Returns 'existing service' list for graph popup menu.
         */
        public List<ServiceInfo> getExistingServiceList(final ServiceInfo p) {
            final List<ServiceInfo> existingServiceList =
                                                   new ArrayList<ServiceInfo>();
            for (final String name : nameToServiceInfoHash.keySet()) {
                final Map<String, ServiceInfo> idHash =
                                                nameToServiceInfoHash.get(name);
                for (final String id : idHash.keySet()) {
                    final ServiceInfo si = idHash.get(id);
                    if (!heartbeatGraph.existsInThePath(si, p)) {
                        existingServiceList.add(si);
                    }
                }
            }
            return existingServiceList;
        }

        /**
         * Returns info object of all block devices on all hosts that have the
         * same names and other attributes.
         */
        Info[] getCommonBlockDevInfos(final Info defaultValue,
                                      final String serviceName) {
            final List<Info> list = new ArrayList<Info>();

            /* drbd resources */
            final Enumeration drbdResources = drbdNode.children();

            if (defaultValue != null) {
                list.add(defaultValue);
            }
            while (drbdResources.hasMoreElements()) {
                DefaultMutableTreeNode n =
                          (DefaultMutableTreeNode) drbdResources.nextElement();
                final CommonDeviceInterface drbdRes =
                                    (CommonDeviceInterface) n.getUserObject();
                list.add((Info) drbdRes);
            }

            /* block devices that are the same on all hosts */
            final Enumeration cbds = commonBlockDevicesNode.children();
            while (cbds.hasMoreElements()) {
                DefaultMutableTreeNode n =
                                   (DefaultMutableTreeNode) cbds.nextElement();
                final CommonDeviceInterface cbd =
                                    (CommonDeviceInterface) n.getUserObject();
                list.add((Info) cbd);
            }

            return list.toArray(new Info[list.size()]);
        }

        /**
         * Selects the node in the menu and reloads everything underneath.
         */
        public void selectMyself() {
            super.selectMyself();
            nodeChanged(getNode());
        }

        /**
         * Creates id text field with label and adds it to the panel.
         */
        protected void addIdField(final JPanel optionsPanel,
                                  final int leftWidth,
                                  final int rightWidth) {
            final JPanel panel = getParamPanel("ID");
            final String regexp = "^[\\w-]+$";
            final String id = getService().getId();
            idField = new GuiComboBox(id, null, null, regexp, rightWidth);
            idField.setValue(id);
            final String[] params = getParametersFromXML();
            idField.getDocument().addDocumentListener(
                new DocumentListener() {
                    private void check() {
                        Thread thread = new Thread(new Runnable() {
                            public void run() {
                                final boolean enable =
                                          checkResourceFields("id",
                                                              params);
                                SwingUtilities.invokeLater(
                                new Runnable() {
                                    public void run() {
                                        applyButton.setEnabled(enable);
                                    }
                                });
                            }
                        });
                        thread.start();
                    }

                    public void insertUpdate(final DocumentEvent e) {
                        check();
                    }

                    public void removeUpdate(final DocumentEvent e) {
                        check();
                    }

                    public void changedUpdate(final DocumentEvent e) {
                        check();
                    }
                }
            );
            paramComboBoxAdd("id", null, idField);
            addField(panel, new JLabel("id"), idField, leftWidth, rightWidth);
            SpringUtilities.makeCompactGrid(panel, 1, 2, // rows, cols
                                            1, 1,        // initX, initY
                                            1, 1);       // xPad, yPad
            optionsPanel.add(panel);
            if (!getService().isNew()) {
                idField.setEnabled(false);
            }
        }

        /**
         * Sets value for id field.
         */
        protected void setIdField(final String id) {
            idField.setValue(id);
        }

        /**
         * Creates heartbeat id and group text field with label and adds them
         * to the panel.
         */
        protected void addHeartbeatFields(final JPanel optionsPanel,
                                          final int leftWidth,
                                          final int rightWidth) {
            final JLabel heartbeatIdLabel =
                                    new JLabel(getService().getHeartbeatId());
            final JPanel panel = getParamPanel("Heartbeat");
            addField(panel,
                     new JLabel(Tools.getString("ClusterBrowser.HeartbeatId")),
                     heartbeatIdLabel,
                     leftWidth,
                     rightWidth);
            final JLabel heartbeatClassLabel =
                                 new JLabel(getService().getHeartbeatClass());
            addField(panel,
                     new JLabel(Tools.getString("ClusterBrowser.HeartbeatClass")),
                     heartbeatClassLabel,
                     leftWidth,
                     rightWidth);
            int rows = 2;

            if (groupInfo != null) {
                final String groupId = groupInfo.getService().getHeartbeatId();
                final JLabel groupLabel = new JLabel(groupId);
                addField(panel,
                         new JLabel(Tools.getString("ClusterBrowser.Group")),
                         groupLabel,
                         leftWidth,
                         rightWidth);
                rows++;
            }
            SpringUtilities.makeCompactGrid(panel, rows, 2, // rows, cols
                                                   1, 1,    // initX, initY
                                                   1, 1);   // xPad, yPad
            optionsPanel.add(panel);
        }

        /**
         * Creates host score combo boxes with labels, one per host.
         */
        protected void addHostScores(final JPanel optionsPanel,
                                     final int leftWidth,
                                     final int rightWidth) {
            int rows = 0;
            scoreComboBoxHash.clear();

            final JPanel panel =
                    getParamPanel(Tools.getString("ClusterBrowser.HostScores"));

            for (Host host : getClusterHosts()) {
                final HostInfo hi = host.getBrowser().getHostInfo();
                final Info[] scores = enumToInfoArray(null,
                                                      getName(),
                                                      scoresNode.children());
                int defaultScore = scores.length - 1; // score )

                final GuiComboBox cb =
                            new GuiComboBox(scores[defaultScore].toString(),
                                            scores,
                                            GuiComboBox.Type.COMBOBOX,
                                            null,
                                            rightWidth);

                defaultScore = 0; // score -infinity
                scoreComboBoxHash.put(hi, cb);

                /* set selected host scores in the combo box from
                 * savedHostScoreInfos */
                final HostScoreInfo hsiSaved = savedHostScoreInfos.get(hi);
                if (hsiSaved != null) {
                    cb.setValue(hsiSaved);
                }
            }

            /* host score combo boxes */
            for (Host host : getClusterHosts()) {
                final HostInfo hi = host.getBrowser().getHostInfo();
                final GuiComboBox cb = scoreComboBoxHash.get(hi);
                addField(panel,
                         new JLabel("on " + hi.getName()),
                         cb,
                         leftWidth,
                         rightWidth);
                rows++;
            }

            SpringUtilities.makeCompactGrid(panel, rows, 2, // rows, cols
                                            1, 1,           // initX, initY
                                            1, 1);          // xPad, yPad
            optionsPanel.add(panel);
        }

        /**
         * Creates operations combo boxes with labels,
         */
        protected void addOperations(final JPanel optionsPanel,
                                     final JPanel extraOptionsPanel,
                                     final int leftWidth,
                                     final int rightWidth) {
            int rows = 0;
            int extraRows = 0;
            operationsComboBoxHash.clear();

            final JPanel panel = getParamPanel(
                                Tools.getString("ClusterBrowser.Operations"));
            final JPanel extraPanel = getParamPanel(
                        Tools.getString("ClusterBrowser.AdvancedOperations"),
                        EXTRA_PANEL_BACKGROUND
                        );

            for (final String op : HB_OPERATIONS) {
                for (final String param : HB_OPERATION_PARAMS.get(op)) {
                    GuiComboBox.Type type;
                    Unit[] units = null; 
                    final String regexp = "^-?\\d*$";
                    type = GuiComboBox.Type.TEXTFIELDWITHUNIT; 
                    // TODO: having this on two places
                    units = new Unit[] {
                        new Unit("", "", "", ""),
                        new Unit("ms",
                                 "ms",
                                 "Millisecond",
                                 "Milliseconds"),
                        new Unit("us",
                                 "us",
                                 "Microsecond",
                                 "Microseconds"),
                        new Unit("s",  "s",  "Second",      "Seconds"),
                        new Unit("m",  "m",  "Minute",      "Minutes"),
                        new Unit("h",   "h",  "Hour",        "Hours"),
                    };
                    String defaultValue =
                                      hbService.getOperationDefault(op, param);
                    // TODO: old style resources 
                    if (defaultValue == null) {
                        defaultValue = "0";
                    }
                    final GuiComboBox cb =
                                new GuiComboBox(defaultValue,
                                                null,
                                                units,
                                                type,
                                                regexp,
                                                rightWidth);

                    operationsComboBoxHash.put(op, param, cb);
                    final String savedValue =
                                        (String) savedOperation.get(op, param);
                    if (savedValue != null) {
                        cb.setValue(savedValue);
                    }

                    JPanel p;
                    if (HB_OP_BASIC.contains(op)) {
                        p = panel;
                        rows++;
                    } else {
                        p = extraPanel;
                        extraRows++;
                    }

                    addField(p,
                             new JLabel(Tools.ucfirst(op) 
                                        + " / " + Tools.ucfirst(param)),
                             cb,
                             leftWidth,
                             rightWidth);
                }
            }

            SpringUtilities.makeCompactGrid(panel, rows, 2, // rows, cols
                                            1, 1,           // initX, initY
                                            1, 1);          // xPad, yPad
            SpringUtilities.makeCompactGrid(extraPanel, extraRows, 2,
                                            1, 1,           // initX, initY
                                            1, 1);          // xPad, yPad
            optionsPanel.add(panel);
            extraOptionsPanel.add(extraPanel);
        }

        /**
         * Returns parameters.
         */
        public String[] getParametersFromXML() {
            return hbOCF.getParameters(hbService);
        }

        /**
         * Returns true if the value of the parameter is ok.
         */
        protected boolean checkParam(final String param,
                                     final String newValue) {
            if (param.equals("ip")
                && newValue != null
                && !Tools.checkIp(newValue)) {
                return false;
            }
            return hbOCF.checkParam(hbService,
                                    param,
                                    newValue);
        }

        /**
         * Returns default value for specified parameter.
         */
        protected String getParamDefault(final String param) {
            return hbOCF.getParamDefault(hbService,
                                         param);
        }

        /**
         * Returns possible choices for drop down lists.
         */
        protected Object[] getParamPossibleChoices(final String param) {
            if (isCheckBox(param)) {
                return hbOCF.getCheckBoxChoices(hbService, param);
            } else {
                // TODO: this does nothing, I think
                return hbOCF.getParamPossibleChoices(hbService, param);
            }
        }

        /**
         * Returns short description of the specified parameter.
         */
        protected String getParamShortDesc(final String param) {
            return hbOCF.getParamShortDesc(hbService,
                                           param);
        }

        /**
         * Returns long description of the specified parameter.
         */
        protected String getParamLongDesc(final String param) {
            return hbOCF.getParamLongDesc(hbService,
                                          param);
        }


        /**
         * Returns section to which the specified parameter belongs.
         */
        protected String getSection(final String param) {
            return hbOCF.getSection(hbService,
                                    param);
        }

        /**
         * Returns true if the specified parameter is required.
         */
        protected boolean isRequired(final String param) {
            return hbOCF.isRequired(hbService,
                                    param);
        }

        /**
         * Returns true if the specified parameter is integer.
         */
        protected boolean isInteger(final String param) {
            return hbOCF.isInteger(hbService,
                                   param);
        }

        /**
         * Returns true if the specified parameter is of time type.
         */
        protected boolean isTimeType(final String param) {
            return hbOCF.isTimeType(hbService,
                                    param);
        }

        /**
         * Returns whether parameter is checkbox.
         */
        protected boolean isCheckBox(final String param) {
            final String type = hbOCF.getParamType(hbService,
                                                   param);
            if (type == null) {
                return false;
            }
            if (DRBD_RES_BOOL_TYPE_NAME.equals(type)) {
                return true;
            }
            return false;
        }

        /**
         * Returns the type of the parameter according to the OCF.
         */
        protected String getParamType(final String param) {
            return hbOCF.getParamType(hbService,
                                      param);
        }

        /**
         * Is called before the service is added. This is for example used by
         * FilesystemInfo so that it can add DrbddiskInfo before it adds
         * itself.
         */
        public void addResourceBefore() {
        }

        /**
         * Returns info panel with comboboxes for service parameters.
         */
        public JComponent getInfoPanel() {
            heartbeatGraph.pickInfo(this);
            if (infoPanel != null) {
                return infoPanel;
            }
            /* main, button and options panels */
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

            final JPanel extraOptionsPanel = new JPanel();
            extraOptionsPanel.setBackground(EXTRA_PANEL_BACKGROUND);
            extraOptionsPanel.setLayout(new BoxLayout(extraOptionsPanel,
                                        BoxLayout.Y_AXIS));
            extraOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            /* expert mode */
            buttonPanel.add(Tools.expertModeButton(extraOptionsPanel),
                            BorderLayout.WEST);

            /* Actions */
            final JMenuBar mb = new JMenuBar();
            mb.setBackground(PANEL_BACKGROUND);
            final JMenu serviceCombo = getActionsMenu();
            updateMenus(null);
            mb.add(serviceCombo);
            buttonPanel.add(mb, BorderLayout.EAST);



            /* id textfield */
            addIdField(optionsPanel, SERVICE_LABEL_WIDTH, SERVICE_FIELD_WIDTH);

            /* heartbeat fields */
            addHeartbeatFields(optionsPanel,
                               SERVICE_LABEL_WIDTH,
                               SERVICE_FIELD_WIDTH);

            /* score combo boxes */
            addHostScores(optionsPanel,
                          SERVICE_LABEL_WIDTH,
                          SERVICE_FIELD_WIDTH);

            /* get dependent resources and create combo boxes for ones, that
             * need parameters */
            paramComboBoxClear();
            final String[] params = getParametersFromXML();
            addParams(optionsPanel,
                      extraOptionsPanel,
                      params,
                      SERVICE_LABEL_WIDTH,
                      SERVICE_FIELD_WIDTH);
            /* Operations */
            addOperations(optionsPanel,
                          extraOptionsPanel,
                          SERVICE_LABEL_WIDTH,
                          SERVICE_FIELD_WIDTH);


            /* add item listeners to the host scores combos */
            for (Host host : getClusterHosts()) {
                final HostInfo hi = host.getBrowser().getHostInfo();
                final GuiComboBox cb = scoreComboBoxHash.get(hi);
                cb.addListeners(
                    new ItemListener() {
                        public void itemStateChanged(final ItemEvent e) {

                            if (cb.isCheckBox()
                                || e.getStateChange() == ItemEvent.SELECTED) {
                                Thread thread = new Thread(new Runnable() {
                                    public void run() {
                                        final boolean enable =
                                                  checkResourceFields("cached",
                                                                      params);
                                        SwingUtilities.invokeLater(
                                        new Runnable() {
                                            public void run() {
                                                applyButton.setEnabled(enable);
                                            }
                                        });
                                    }
                                });
                                thread.start();
                            }
                        }
                    },

                    new DocumentListener() {
                        private void check() {
                            Thread thread = new Thread(new Runnable() {
                                public void run() {
                                    final boolean enable =
                                        checkResourceFields("cached", params);
                                    SwingUtilities.invokeLater(
                                    new Runnable() {
                                        public void run() {
                                            applyButton.setEnabled(enable);
                                        }
                                    });
                                }
                            });
                            thread.start();
                        }

                        public void insertUpdate(final DocumentEvent e) {
                            check();
                        }

                        public void removeUpdate(final DocumentEvent e) {
                            check();
                        }

                        public void changedUpdate(final DocumentEvent e) {
                            check();
                        }
                    }
                );
            }

            /* add item listeners to the operations combos */
            for (final String op : HB_OPERATIONS) {
                for (final String param : HB_OPERATION_PARAMS.get(op)) {
                    final GuiComboBox cb =
                            (GuiComboBox) operationsComboBoxHash.get(op, param);
                    cb.addListeners(
                        new ItemListener() {
                            public void itemStateChanged(final ItemEvent e) {

                                if (cb.isCheckBox()
                                    || e.getStateChange() == ItemEvent.SELECTED) {
                                    Thread thread = new Thread(new Runnable() {
                                        public void run() {
                                            final boolean enable =
                                                checkResourceFields("cached", params);
                                            SwingUtilities.invokeLater(
                                            new Runnable() {
                                                public void run() {
                                                    applyButton.setEnabled(enable);
                                                }
                                            });
                                        }
                                    });
                                    thread.start();
                                }
                            }
                        },

                        new DocumentListener() {
                            private void check() {
                                Thread thread = new Thread(new Runnable() {
                                    public void run() {
                                        final boolean enable =
                                            checkResourceFields("cached",
                                                                params);
                                        SwingUtilities.invokeLater(
                                        new Runnable() {
                                            public void run() {
                                                applyButton.setEnabled(enable);
                                            }
                                        });
                                    }
                                });
                                thread.start();
                            }

                            public void insertUpdate(final DocumentEvent e) {
                                check();
                            }

                            public void removeUpdate(final DocumentEvent e) {
                                check();
                            }

                            public void changedUpdate(final DocumentEvent e) {
                                check();
                            }
                        }
                    );
                }
            }

            /* add item listeners to the apply button. */
            applyButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(
                            new Runnable() {
                                public void run() {
                                    hbStatusLock();
                                    apply();
                                    hbStatusUnlock();
                                }
                            }
                        );
                        thread.start();
                    }
                }
            );

            /* apply button */
            addApplyButton(mainPanel);
            applyButton.setEnabled(
                checkResourceFields(null, params)
            );
            mainPanel.add(optionsPanel);
            mainPanel.add(extraOptionsPanel);
            infoPanel = new JPanel();
            infoPanel.setBackground(PANEL_BACKGROUND);
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.add(buttonPanel);
            infoPanel.add(new JScrollPane(mainPanel));
            infoPanel.add(Box.createVerticalGlue());
            /* if id textfield was changed and this id is not used,
             * enable apply button */
            return infoPanel;
        }

        /**
         * Clears the info panel cache, forcing it to reload.
         */
        public boolean selectAutomaticallyInTreeMenu() {
            return infoPanel == null;
        }

        /**
         * Goes through the scores and sets preferred locations.
         */
        protected void setLocations(final String heartbeatId) {
            for (Host host : getClusterHosts()) {
                final HostInfo hi = host.getBrowser().getHostInfo();
                final GuiComboBox cb = scoreComboBoxHash.get(hi);
                final HostScoreInfo hsi = (HostScoreInfo) cb.getValue();
                final HostScoreInfo hsiSaved = savedHostScoreInfos.get(hi);
                if (!hsi.equals(hsiSaved)) {
                    final String onHost = hi.getName();
                    final String score = hsi.getScore();
                    final String locationId =
                              heartbeatStatus.getLocationId(
                                                getService().getHeartbeatId(),
                                                onHost);
                    Heartbeat.setLocation(getDCHost(),
                                          getService().getHeartbeatId(),
                                          onHost,
                                          score,
                                          locationId);
                }
            }
            storeHostScoreInfos();
        }

        /**
         * Returns hash with changed operation ids and all name, value pairs.
         * This works for new heartbeats >= 2.99.0
         */
        private Map<String, Map<String, String>> getOperations(
                                                        String heartbeatId) {
            final Map<String, Map<String, String>> operations =
                                  new HashMap<String, Map<String, String>>();

            for (final String op : HB_OPERATIONS) {
                final Map<String, String> opHash =
                                                new HashMap<String, String>();
                String opId = heartbeatStatus.getOpId(heartbeatId, op);
                if (opId == null) {
                    /* generate one */
                    opId = "op-" + heartbeatId + "-" + op;
                }
                
                for (final String param : HB_OPERATION_PARAM_LIST) {
                    if (HB_OPERATION_PARAMS.get(op).contains(param)) {
                        final GuiComboBox cb =
                            (GuiComboBox) operationsComboBoxHash.get(op,
                                                                     param);
                        final String value = cb.getStringValue();
                        String savedOp =
                            (String) savedOperation.get(op, param);
                        if (savedOp == null) {
                            savedOp = "";
                        }
                        if (!value.equals(savedOp)) {
                            opHash.put(param, value);
                        }
                    }
                    if (opHash.size() > 0) {
                        operations.put(op, opHash); 
                        opHash.put("id", opId);
                        opHash.put("name", op);
                    }
                }
            }
            return operations;
        }

        /**
         * Goes through the operations and sets the ones that changed to the
         * heartbeat.
         * This works for old heartbeats <=2.1.4
         */
        private void setOperations(final String heartbeatId) {
            for (final String op : HB_OPERATIONS) {
                boolean somethingChanged = false;
                final List<String> args = new ArrayList<String>();
                args.add(op);
                for (final String param : HB_OPERATION_PARAM_LIST) {
                    String arg = HB_NONE_ARG;
                    if (HB_OPERATION_PARAMS.get(op).contains(param)) {
                        final GuiComboBox cb =
                            (GuiComboBox) operationsComboBoxHash.get(op,
                                                                     param);
                        final String value = cb.getStringValue();
                        String savedOp =
                            (String) savedOperation.get(op, param);
                        if (savedOp == null) {
                            savedOp = "";
                        }
                        if (!value.equals(savedOp)) {
                            //String defaultValue =
                            //          hbService.getOperationDefault(op, param);
                            //if (defaultValue == null) {
                            //    defaultValue = "";
                            //}
                            //if (!value.equals(defaultValue)) {
                                somethingChanged = true;
                            //}
                        }
                        arg = value;
                    }
                    args.add(arg);
                }
                if (somethingChanged) {
                    Heartbeat.addOperation(getDCHost(),
                                           heartbeatId,
                                           op,
                                           args);
                }
            }
        }

        /**
         * Applies the changes to the service parameters.
         */
        public void apply() {
            final String[] params = getParametersFromXML();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    applyButton.setEnabled(false);
                    idField.setEnabled(false);
                }
            });

            /* add myself to the hash with service name and id as
             * keys */
            removeFromServiceInfoHash(this);
            final String oldHeartbeatId = getService().getHeartbeatId();
            if (oldHeartbeatId != null) {
                heartbeatIdToServiceInfo.remove(oldHeartbeatId);
                heartbeatIdList.remove(oldHeartbeatId);
            }
            final String id = idField.getStringValue();
            getService().setId(id);
            addToHeartbeatIdList(this);
            addNameToServiceInfoHash(this);

            addResourceBefore();

            /*
                MSG_ADD_RSC rsc_id rsc_class rsc_type rsc_provider group("" for NONE)
                        advance(""|"clone"|"master") advance_id clone_max
                        clone_node_max master_max master_node_max
                        param_id1 param_name1 param_value1
                        param_id2 param_name2 param_value2
                        ...
                        param_idn param_namen param_valuen

                in heartbeat 2.1.3 there is additional new_group parameter (sometimes it is, sometime not 2.1.3-2)
            */
            Map<String,String> pacemakerResAttrs =
                                                new HashMap<String,String>();
            Map<String,String> pacemakerResArgs = new HashMap<String,String>();
            final String hbClass         = getService().getHeartbeatClass();
            final String type            = getResource().getName();
            String heartbeatId     = getService().getHeartbeatId();
            pacemakerResAttrs.put("id",       heartbeatId);
            pacemakerResAttrs.put("class",    hbClass);
            pacemakerResAttrs.put("type",     type);
            String group;
            String groupId = null; /* for pacemaker */
            if (groupInfo == null) {
                group = HB_NONE_ARG;
            } else {
                group = groupInfo.getService().getHeartbeatId();
                groupId = group;
            }

            if (getService().isNew()) {
                final String provider = HB_HEARTBEAT_PROVIDER;
                if (hbClass.equals("ocf")) {
                    pacemakerResAttrs.put("provider", provider);
                }
                final String advance       = HB_NONE_ARG;
                final String advanceId     = HB_NONE_ARG;
                final String cloneMax      = HB_NONE_ARG;
                final String cloneNodeMax  = HB_NONE_ARG;
                final String masterMax     = HB_NONE_ARG;
                final String masterNodeMax = HB_NONE_ARG;
                final String newGroup      = HB_NONE_ARG;

                final StringBuffer args = new StringBuffer(120);

                args.append(heartbeatId);
                args.append(' ');
                args.append(hbClass);
                args.append(' ');
                args.append(type);
                args.append(' ');
                args.append(provider);
                args.append(' ');
                args.append(group);
                args.append(' ');
                args.append(advance);
                args.append(' ');
                args.append(advanceId);
                args.append(' ');
                args.append(cloneMax);
                args.append(' ');
                args.append(cloneNodeMax);
                args.append(' ');
                args.append(masterMax);
                args.append(' ');
                args.append(masterNodeMax);

                /* TODO: there are more attributes. */

                final String hbV = getDCHost().getHeartbeatVersion();
                if (Tools.compareVersions(hbV, "2.1.3") >= 0) {
                    args.append(' ');
                    args.append(newGroup);
                }
                for (String param : params) {
                    String value = getComboBoxValue(param);
                    if (value.equals(getParamDefault(param))) {
                        continue;
                    }
                    if ("".equals(value)) {
                        value = HB_NONE_ARG;
                    } else {
                        /* for pacemaker */
                        pacemakerResArgs.put(param, value);
                    }
                    args.append(' ');
                    args.append(heartbeatId);
                    args.append('-');
                    args.append(param);
                    args.append(' ');
                    args.append(param);
                    args.append(" \"");
                    args.append(value);
                    args.append('"');
                }
                Heartbeat.addResource(getDCHost(),
                                      heartbeatId,
                                      groupId,
                                      args.toString(),
                                      pacemakerResAttrs,
                                      pacemakerResArgs,
                                      null,
                                      null,
                                      getOperations(heartbeatId),
                                      null);
                if (groupInfo == null) {
                    final String[] parents = heartbeatGraph.getParents(this);
                    Heartbeat.setOrderAndColocation(getDCHost(),
                                                    heartbeatId,
                                                    parents);
                }
            } else {
                // update parameters
                final StringBuffer args = new StringBuffer("");
                for (String param : params) {
                    final String oldValue = getResource().getValue(param);
                    String value = getComboBoxValue(param);
                    if (value.equals(oldValue)) {
                        continue;
                    }

                    if ("".equals(value)) {
                        value = getParamDefault(param);
                    } else {
                        pacemakerResArgs.put(param, value);
                    }
                    args.append(' ');
                    args.append(heartbeatId);
                    args.append('-');
                    args.append(param);
                    args.append(' ');
                    args.append(param);
                    args.append(" \"");
                    args.append(value);
                    args.append('"');
                }
                args.insert(0, heartbeatId);

                Heartbeat.setParameters(
                        getDCHost(),
                        heartbeatId,
                        groupId,
                        args.toString(),
                        pacemakerResAttrs,
                        pacemakerResArgs,
                        heartbeatStatus.getResourceInstanceAttrId(heartbeatId),
                        heartbeatStatus.getParametersNvpairsIds(heartbeatId),
                        getOperations(heartbeatId),
                        heartbeatStatus.getOperationsId(heartbeatId));
            }
            if (groupInfo != null && groupInfo.getService().isNew()) {
                groupInfo.apply();
            }

            if (groupInfo == null) {
                setLocations(heartbeatId);
            }
            final String hbV = getDCHost().getHeartbeatVersion();
            if (Tools.compareVersions(hbV, "2.99.0") < 0) {
                setOperations(heartbeatId);
            }
            storeComboBoxValues(params);

            reload(getNode());
            heartbeatGraph.repaint();

        }

        /**
         * Removes order.
         */
        public void removeOrder(final ServiceInfo parent) {
            final String parentHbId = parent.getService().getHeartbeatId();
            final String orderId =
                    heartbeatStatus.getOrderId(parentHbId,
                                               getService().getHeartbeatId());
            final String score =
                heartbeatStatus.getOrderScore(
                                    parent.getService().getHeartbeatId(), 
                                    getService().getHeartbeatId());
            final String symmetrical =
                heartbeatStatus.getOrderSymmetrical(
                                    parent.getService().getHeartbeatId(), 
                                    getService().getHeartbeatId());
            Heartbeat.removeOrder(getDCHost(),
                                  orderId,
                                  parentHbId,
                                  getService().getHeartbeatId(),
                                  score,
                                  symmetrical);
        }

        /**
         * Adds order constraint from this service to the parent.
         */
        public void addOrder(final ServiceInfo parent) {
            final String parentHbId = parent.getService().getHeartbeatId();
            Heartbeat.addOrder(getDCHost(),
                               getService().getHeartbeatId(),
                               parentHbId);
        }

        /**
         * Removes colocation.
         */
        public void removeColocation(final ServiceInfo parent) {
            final String parentHbId = parent.getService().getHeartbeatId();
            final String colocationId =
                heartbeatStatus.getColocationId(parentHbId,
                                                getService().getHeartbeatId());
            final String score =
                heartbeatStatus.getColocationScore(
                                    parent.getService().getHeartbeatId(), 
                                    getService().getHeartbeatId());
            Heartbeat.removeColocation(getDCHost(),
                                       colocationId,
                                       parentHbId, /* from */
                                       getService().getHeartbeatId(), /* to */
                                       score
                                       );
        }

        /**
         * Adds colocation constraint from this service to the parent. The
         * parent - child order is here important, in case colocation
         * constraint is used along with order constraint.
         */
        public void addColocation(final ServiceInfo parent) {
            final String parentHbId = parent.getService().getHeartbeatId();
            Heartbeat.addColocation(getDCHost(),
                                    getService().getHeartbeatId(),
                                    parentHbId);
        }

        /**
         * Returns panel with graph.
         */
        public JPanel getGraphicalView() {
            return heartbeatGraph.getGraphPanel();
        }

        /**
         * Adds service panel to the position 'pos'.
         */
        public ServiceInfo addServicePanel(final HeartbeatService newHbService,
                                           final Point2D pos,
                                           final boolean reloadNode) {
            ServiceInfo newServiceInfo;

            final String name = newHbService.getName();
            if (newHbService.isFilesystem()) {
                newServiceInfo = new FilesystemInfo(name, newHbService);
            } else if (newHbService.isDrbddisk()) {
                newServiceInfo = new DrbddiskInfo(name, newHbService);
            } else if (newHbService.isIPaddr()) {
                newServiceInfo = new IPaddrInfo(name, newHbService);
            } else if (newHbService.isGroup()) {
                newServiceInfo = new GroupInfo(newHbService);
            } else {
                newServiceInfo = new ServiceInfo(name, newHbService);
            }
            addToHeartbeatIdList(newServiceInfo);

            addServicePanel(newServiceInfo, pos, reloadNode);
            return newServiceInfo;
        }

        /**
         * Adds service panel to the position 'pos'.
         * TODO: is it used?
         */
        public void addServicePanel(final ServiceInfo serviceInfo,
                                    final Point2D pos,
                                    final boolean reloadNode) {

            serviceInfo.getService().setHeartbeatClass(
                        serviceInfo.getHeartbeatService().getHeartbeatClass());
            if (heartbeatGraph.addResource(serviceInfo, this, pos)) {
                // edge added
                final String heartbeatId =
                                    serviceInfo.getService().getHeartbeatId();
                final String[] parents = heartbeatGraph.getParents(serviceInfo);
                Heartbeat.setOrderAndColocation(getDCHost(),
                                                heartbeatId,
                                                parents);
            } else {
                addNameToServiceInfoHash(serviceInfo);
                final DefaultMutableTreeNode newServiceNode =
                                        new DefaultMutableTreeNode(serviceInfo);
                serviceInfo.setNode(newServiceNode);

                servicesNode.add(newServiceNode);
                if (reloadNode) {
                    reload(servicesNode);
                    reload(newServiceNode);
                }
            }
            heartbeatGraph.reloadServiceMenus();
        }

        /**
         * Returns service that belongs to this info object.
         */
        public Service getService() {
            return (Service) getResource();
        }

        /**
         * Starts resource in heartbeat.
         */
        public void startResource() {
            Heartbeat.startResource(getDCHost(),
                                    getService().getHeartbeatId());
        }

        /**
         * Moves resource up in the group.
         */
        public void moveGroupResUp() {
            Heartbeat.moveGroupResUp(getDCHost(),
                                     getService().getHeartbeatId());
        }

        /**
         * Moves resource down in the group.
         */
        public void moveGroupResDown() {
            Heartbeat.moveGroupResDown(getDCHost(),
                                       getService().getHeartbeatId());
        }

        /**
         * Stops resource in heartbeat.
         */
        public void stopResource() {
            Heartbeat.stopResource(getDCHost(),
                                   getService().getHeartbeatId());
        }

        /**
         * Migrates resource in heartbeat from current location.
         */
        public void migrateResource(final String onHost) {
            Heartbeat.migrateResource(getDCHost(),
                                      getService().getHeartbeatId(),
                                      onHost);
        }

        /**
         * Removes constraints created by resource migrate command.
         */
        public void unmigrateResource() {
            Heartbeat.unmigrateResource(getDCHost(),
                                        getService().getHeartbeatId());
        }

        /**
         * Cleans up the resource.
         */
        public void cleanupResource() {
            Heartbeat.cleanupResource(getDCHost(),
                                      getService().getHeartbeatId(),
                                      getClusterHosts());
        }

        /**
         * Removes the service without confirmation dialog.
         */
        protected void removeMyselfNoConfirm() {
            getService().setRemoved(true);

            //super.removeMyself();
            if (getService().isNew()) {
                heartbeatGraph.getServicesInfo().setAllResources();
            } else {
                if (groupInfo == null) {
                    final String[] parents = heartbeatGraph.getParents(this);
                    for (String parent : parents) {
                        final String colocationId =
                            heartbeatStatus.getColocationId(
                                    parent, getService().getHeartbeatId());
                        final String colScore =
                            heartbeatStatus.getColocationScore(
                                           parent, 
                                           getService().getHeartbeatId());
                        Heartbeat.removeColocation(getDCHost(),
                                                   colocationId,
                                                   parent,
                                                   getService().getHeartbeatId(),
                                                   colScore);
                        final String orderId =
                                    heartbeatStatus.getOrderId(
                                        parent, getService().getHeartbeatId());

                        final String ordScore =
                            heartbeatStatus.getOrderScore(
                                           parent, 
                                           getService().getHeartbeatId());
                        final String symmetrical =
                            heartbeatStatus.getOrderSymmetrical(
                                           parent, 
                                           getService().getHeartbeatId());
                        Heartbeat.removeOrder(getDCHost(),
                                              orderId,
                                              parent,
                                              getService().getHeartbeatId(),
                                              ordScore,
                                              symmetrical);
                    }

                    final String[] children = heartbeatGraph.getChildren(this);
                    for (String child : children) {
                        final String colocationId =
                                       heartbeatStatus.getColocationId(
                                          child, getService().getHeartbeatId());
                        final String colScore =
                            heartbeatStatus.getColocationScore(
                                           getService().getHeartbeatId(),
                                           child);
                        Heartbeat.removeColocation(getDCHost(),
                                                   colocationId,
                                                   getService().getHeartbeatId(),
                                                   child,
                                                   colScore);
                        final String orderId = heartbeatStatus.getOrderId(child,
                                                getService().getHeartbeatId());
                        final String ordScore =
                            heartbeatStatus.getOrderScore(
                                           getService().getHeartbeatId(),
                                           child);
                        final String symmetrical =
                            heartbeatStatus.getOrderSymmetrical(
                                           getService().getHeartbeatId(),
                                           child);
                        Heartbeat.removeOrder(getDCHost(),
                                              orderId,
                                              getService().getHeartbeatId(),
                                              child,
                                              ordScore,
                                              symmetrical);
                    }

                    for (String locId : heartbeatStatus.getLocationIds(getService().getHeartbeatId())) {
                        final String locScore =
                            heartbeatStatus.getLocationScore(locId);
                        Heartbeat.removeLocation(getDCHost(),
                                                 locId,
                                                 getService().getHeartbeatId(),
                                                 locScore);
                    }
                }
                if (!getHeartbeatService().isGroup()) {
                    String groupId = null; /* for pacemaker */
                    if (groupInfo != null) {
                        /* get group id only if there is only one resource in a
                         * group.
                         */
                        final String group = groupInfo.getService().getHeartbeatId();
                        final Enumeration e = groupInfo.getNode().children();
                        while (e.hasMoreElements()) {
                            DefaultMutableTreeNode n =
                                      (DefaultMutableTreeNode) e.nextElement();
                            final ServiceInfo child =
                                               (ServiceInfo) n.getUserObject();
                            child.getService().setModified(true);
                            child.getService().doneModifying();
                        }
                        if (heartbeatStatus.getGroupResources(group).size() == 1) {
                            groupInfo.getService().setRemoved(true);
                            groupInfo.removeMyselfNoConfirmFromChild();
                            groupId = group;
                            groupInfo.getService().doneRemoving();
                        }
                    } 
                    Heartbeat.removeResource(getDCHost(),
                                             getService().getHeartbeatId(),
                                             groupId);
                }
            }
            //if (groupInfo == null)
            //    heartbeatGraph.removeInfo(this);
            removeFromServiceInfoHash(this);
            infoPanel = null;
            getService().doneRemoving();
        }

        /**
         * Removes this service from the heartbeat with confirmation dialog.
         */
        public void removeMyself() {
            String desc = Tools.getString(
                            "ClusterBrowser.confirmRemoveService.Description");

            desc  = desc.replaceAll("@SERVICE@", toString());
            if (Tools.confirmDialog(
                   Tools.getString("ClusterBrowser.confirmRemoveService.Title"),
                   desc,
                   Tools.getString("ClusterBrowser.confirmRemoveService.Yes"),
                   Tools.getString("ClusterBrowser.confirmRemoveService.No"))) {
                removeMyselfNoConfirm();
            }
            getService().setNew(false);
        }

        /**
         * Removes the service from some global hashes and lists.
         */
        public void removeInfo() {
            heartbeatIdToServiceInfo.remove(getService().getHeartbeatId());
            heartbeatIdList.remove(getService().getHeartbeatId());
            removeFromServiceInfoHash(this);
            super.removeMyself();
        }

        /**
         * Sets this service as part of a group.
         */
        public void setGroupInfo(final GroupInfo groupInfo) {
            this.groupInfo = groupInfo;
        }

        /**
         * Returns the group to which this service belongs or null, if it is
         * not in any group.
         */
        public GroupInfo getGroupInfo() {
            return groupInfo;
        }

        /**
         * Returns list of items for service popup menu with actions that can
         * be executed on the heartbeat services.
         */
        public List<UpdatableItem> createPopup() {
            final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
            /* remove service */
            final MyMenuItem removeMenuItem = new MyMenuItem(
                        Tools.getString(
                                "ClusterBrowser.Hb.RemoveService"),
                        REMOVE_ICON) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    // TODO: if it was migrated
                    return !getService().isRemoved();
                }

                public void action() {
                    removeMyself();
                    heartbeatGraph.getVisualizationViewer().repaint();
                }
            };
            items.add((UpdatableItem) removeMenuItem);
            registerMenuItem((UpdatableItem) removeMenuItem);

            if (groupInfo == null) {
                /* add new group and dependency*/
                final MyMenuItem addGroupMenuItem =
                    new MyMenuItem(Tools.getString(
                                        "ClusterBrowser.Hb.AddDependentGroup"),
                                   null,
                                   null) {
                        private static final long serialVersionUID = 1L;

                        public boolean enablePredicate() {
                            return !getService().isRemoved();
                        }

                        public void action() {
                            final StringInfo gi = new StringInfo(HB_GROUP_NAME,
                                                                 HB_GROUP_NAME);
                            addServicePanel(hbOCF.getHbGroup(), getPos(), true);
                            heartbeatGraph.getVisualizationViewer().repaint();
                        }
                    };
                items.add((UpdatableItem) addGroupMenuItem);
                registerMenuItem((UpdatableItem) addGroupMenuItem);

                /* add new service and dependency*/
                final MyMenu addServiceMenuItem = new MyMenu(
                                    Tools.getString(
                                        "ClusterBrowser.Hb.AddDependency")) {
                    private static final long serialVersionUID = 1L;

                    public void update() {
                        super.update();
                        removeAll();
                        final Point2D pos = getPos();
                        final HeartbeatService fsService =
                                        hbOCF.getHbService("Filesystem",
                                                                  "ocf");
                        if (fsService != null) { /* just skip it, if it is not*/
                            final MyMenuItem fsMenuItem =
                                   new MyMenuItem(fsService.getMenuName()) {
                                private static final long serialVersionUID = 1L;
                                public void action() {
                                    getPopup().setVisible(false);
                                    addServicePanel(fsService,
                                                    getPos(),
                                                    true);
                                    heartbeatGraph.getVisualizationViewer().repaint();
                                }
                            };
                            fsMenuItem.setPos(pos);
                            add(fsMenuItem);
                        }
                        final HeartbeatService ipService =
                                        hbOCF.getHbService("IPaddr2",
                                                                  "ocf");
                        if (ipService != null) { /* just skip it, if it is not*/
                            final MyMenuItem ipMenuItem =
                                   new MyMenuItem(ipService.getMenuName()) {
                                private static final long serialVersionUID = 1L;
                                public void action() {
                                    getPopup().setVisible(false);
                                    addServicePanel(ipService,
                                                    getPos(),
                                                    true);
                                    heartbeatGraph.getVisualizationViewer().repaint();
                                }
                            };
                            ipMenuItem.setPos(pos);
                            add(ipMenuItem);
                        }
                        for (final String cl : HB_CLASSES) {
                            final MyMenu classItem =
                                            new MyMenu(HB_CLASS_MENU.get(cl));
                            DefaultListModel m = new DefaultListModel();
                            //Point2D pos = getPos();
                            for (final HeartbeatService hbService : getAddServiceList(cl)) {
                                final MyMenuItem mmi =
                                       new MyMenuItem(hbService.getMenuName()) {
                                    private static final long serialVersionUID = 1L;
                                    public void action() {
                                        getPopup().setVisible(false);
                                        addServicePanel(hbService,
                                                        getPos(),
                                                        true);
                                        heartbeatGraph.getVisualizationViewer().repaint();
                                    }
                                };
                                mmi.setPos(pos);
                                m.addElement(mmi);
                            }
                            classItem.add(Tools.getScrollingMenu(classItem, m));
                            add(classItem);
                        }
                    }
                };
                items.add((UpdatableItem) addServiceMenuItem);
                registerMenuItem((UpdatableItem) addServiceMenuItem);

                /* add existing service dependency*/
                final ServiceInfo thisClass = this;
                final MyMenu existingServiceMenuItem =
                                new MyMenu(
                                    Tools.getString(
                                        "ClusterBrowser.Hb.AddStartBefore")) {
                    private static final long serialVersionUID = 1L;

                    public void update() {
                        super.update();
                        removeAll();

                        DefaultListModel m = new DefaultListModel();
                        for (final ServiceInfo asi : getExistingServiceList(thisClass)) {
                            final MyMenuItem mmi =
                                                new MyMenuItem(asi.toString()) {
                                private static final long serialVersionUID = 1L;
                                public void action() {
                                    getPopup().setVisible(false);
                                    addServicePanel(asi, null, true);
                                    repaint();
                                }
                            };
                            m.addElement(mmi);
                        }
                        add(Tools.getScrollingMenu(this, m));
                    }
                };
                items.add((UpdatableItem) existingServiceMenuItem);
                registerMenuItem((UpdatableItem) existingServiceMenuItem);
            } else { /* group service */
                final MyMenuItem moveUpMenuItem =
                    new MyMenuItem(Tools.getString(
                                            "ClusterBrowser.Hb.ResGrpMoveUp"),
                                   null, // upIcon,
                                   null) {
                        private static final long serialVersionUID = 1L;

                        public boolean enablePredicate() {
                            // TODO: don't if it is up
                            return getService().isAvailable();
                        }

                        public void action() {
                            moveGroupResUp();
                        }
                    };
                items.add(moveUpMenuItem);
                registerMenuItem(moveUpMenuItem);

                /* move down */
                final MyMenuItem moveDownMenuItem =
                    new MyMenuItem(Tools.getString(
                                            "ClusterBrowser.Hb.ResGrpMoveDown"),
                                   null, // TODO: downIcon,
                                   null) {
                        private static final long serialVersionUID = 1L;

                        public boolean enablePredicate() {
                            // TODO: don't if it is down
                            return getService().isAvailable();
                        }

                        public void action() {
                            moveGroupResDown();
                        }
                    };
                items.add(moveDownMenuItem);
                registerMenuItem(moveDownMenuItem);
            }

            /* start resource */
            final MyMenuItem startMenuItem =
                new MyMenuItem(
                      Tools.getString("ClusterBrowser.Hb.StartResource"),
                      START_ICON,
                      Tools.getString("ClusterBrowser.Hb.StartResource.ToolTip")
                     ) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getService().isAvailable() && !isStarted();
                    }

                    public void action() {
                        startResource();
                    }
                };
            items.add((UpdatableItem) startMenuItem);
            registerMenuItem((UpdatableItem) startMenuItem);

            /* stop resource */
            final MyMenuItem stopMenuItem =
                new MyMenuItem(
                       Tools.getString("ClusterBrowser.Hb.StopResource"),
                       STOP_ICON,
                       Tools.getString("ClusterBrowser.Hb.StopResource.ToolTip")
                      ) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getService().isAvailable() && !isStopped();
                    }

                    public void action() {
                        stopResource();
                    }
                };
            items.add((UpdatableItem) stopMenuItem);
            registerMenuItem((UpdatableItem) stopMenuItem);

            /* manage resource */
            final MyMenuItem manageMenuItem =
                new MyMenuItem(
                      Tools.getString("ClusterBrowser.Hb.ManageResource"),
                      START_ICON, // TODO: icons
                      Tools.getString("ClusterBrowser.Hb.ManageResource.ToolTip"),

                      Tools.getString("ClusterBrowser.Hb.UnmanageResource"),
                      STOP_ICON, // TODO: icons
                      Tools.getString("ClusterBrowser.Hb.UnmanageResource.ToolTip")
                     ) {
                    private static final long serialVersionUID = 1L;

                    public boolean predicate() {
                        return !isManaged();
                    }
                    public boolean enablePredicate() {
                        return getService().isAvailable();
                    }

                    public void action() {
                        if (this.getText().equals(Tools.getString(
                                        "ClusterBrowser.Hb.ManageResource"))) {
                            setManaged("true");
                        } else {
                            setManaged("false");
                        }
                    }
                };
            items.add((UpdatableItem) manageMenuItem);
            registerMenuItem((UpdatableItem) manageMenuItem);

            /* migrate resource */
            for (final String hostName : getHostNames()) {
                final MyMenuItem migrateMenuItem =
                    new MyMenuItem(
                            Tools.getString(
                                 "ClusterBrowser.Hb.MigrateResource")
                                 + " " + hostName,
                            MIGRATE_ICON,
                            Tools.getString(
                                 "ClusterBrowser.Hb.MigrateResource.ToolTip"),

                            Tools.getString(
                                 "ClusterBrowser.Hb.MigrateResource")
                                 + " " + hostName + " (inactive)",
                            MIGRATE_ICON,
                            Tools.getString(
                                 "ClusterBrowser.Hb.MigrateResource.ToolTip")
                           ) {
                        private static final long serialVersionUID = 1L;

                        public boolean predicate() {
                            return isActiveNode(hostName);
                        }

                        public boolean enablePredicate() {
                            String runningOnNode = getRunningOnNode();
                            if (runningOnNode != null) {
                                runningOnNode = runningOnNode.toLowerCase();
                            }
                            return getService().isAvailable()
                                   && !hostName.toLowerCase().equals(
                                             runningOnNode)
                                   && isActiveNode(hostName);
                        }

                        public void action() {
                            migrateResource(hostName);
                        }
                    };

                items.add((UpdatableItem) migrateMenuItem);
                registerMenuItem((UpdatableItem) migrateMenuItem);
            }

            /* unmigrate resource */
            final MyMenuItem unmigrateMenuItem =
                new MyMenuItem(Tools.getString(
                                    "ClusterBrowser.Hb.UnmigrateResource")) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        // TODO: if it was migrated
                        return getService().isAvailable();
                    }

                    public void action() {
                        unmigrateResource();
                    }
                };
            items.add((UpdatableItem) unmigrateMenuItem);
            registerMenuItem((UpdatableItem) unmigrateMenuItem);

            /* clean up resource */
            final MyMenuItem cleanupMenuItem =
                new MyMenuItem(Tools.getString(
                                        "ClusterBrowser.Hb.CleanUpResource")) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return getService().isAvailable();
                    }

                    public void action() {
                        cleanupResource();
                    }
                };
            items.add((UpdatableItem) cleanupMenuItem);
            registerMenuItem((UpdatableItem) cleanupMenuItem);

            /* view log */
            final MyMenuItem viewLogMenu = new MyMenuItem(
                            Tools.getString("ClusterBrowser.Hb.ViewServiceLog"),
                            null,
                            null) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return !getService().isNew();
                }

                public void action() {
                    final Thread thread = new Thread(
                        new Runnable() {
                            public void run() {
                                ServiceLogs l =
                                    new ServiceLogs(getCluster(),
                                                getService().getHeartbeatId());
                                l.showDialog();
                            }
                        });
                    thread.start();
                }
            };
            registerMenuItem(viewLogMenu);
            items.add(viewLogMenu);

            return items;
        }

        /**
         * Returns tool tip for the hearbeat service.
         */
        public String getToolTipText() {
            String node = getRunningOnNode();
            if (node == null) {
                node = "none";
            }
            final StringBuffer sb = new StringBuffer(200);
            sb.append(toString());
            sb.append(" running on node: ");
            sb.append(node);
            sb.append("<br>is managed by heartbeat: ");
            sb.append(isManaged());
            return sb.toString();
        }

        /**
         * Returns heartbeat service class.
         */
        public HeartbeatService getHeartbeatService() {
            return hbService;
        }
    }

    /**
     * This class is used for all kind of categories in the heartbeat
     * hierarchy. Its point is to show heartbeat graph all the time, ane
     * heartbeat category is clicked.
     */
    class HbCategoryInfo extends CategoryInfo {
        /**
         * Creates the new HbCategoryInfo object with name of the category.
         */
        HbCategoryInfo(final String name) {
            super(name);
        }

        /**
         * Returns info for the category.
         */
        public String getInfo() {
            return "<h2>" + getName() + "</h2>";
        }

        /**
         * Returns heartbeat graph.
         */
        public JPanel getGraphicalView() {
            return heartbeatGraph.getGraphPanel();
        }
    }

    /**
     * This class holds data that describe the heartbeat as whole.
     */
    class HeartbeatInfo extends HbCategoryInfo {
        /**
         * Prepares a new <code>ServicesInfo</code> object.
         *
         * @param name
         *      name that will be shown in the tree
         */
        public HeartbeatInfo(final String name) {
            super(name);
        }

        /**
         * Returns icon for the heartbeat menu item.
         */
        public ImageIcon getMenuIcon() {
            return null;
        }

        /**
         * Returns info for the Heartbeat menu.
         */
        public String getInfo() {
            final StringBuffer s = new StringBuffer(30);
            s.append("<h2>");
            s.append(getName());
            if (hbOCF == null) {
                s.append("</h2><br>info not available");
                return s.toString();
            }

            final Host[] hosts = getClusterHosts();
            int i = 0;
            final StringBuffer hbVersion = new StringBuffer();
            boolean differentHbVersions = false;
            for (Host host : hosts) {
                if (i == 0) {
                    hbVersion.append(host.getHeartbeatVersion());
                } else if (!hbVersion.toString().equals(
                                                host.getHeartbeatVersion())) {
                    differentHbVersions = true;
                    hbVersion.append(", ");
                    hbVersion.append(host.getHeartbeatVersion());
                }

                i++;
            }
            s.append(" (" + hbVersion.toString() + ")</h2><br>");
            if (differentHbVersions) {
                s.append(Tools.getString(
                                "ClusterBrowser.DifferentHbVersionsWarning"));
                s.append("<br>");
            }
            return s.toString();
        }
    }

    /**
     * This class holds info data for services view and global heartbeat
     * config.
     */
    class ServicesInfo extends EditableInfo {
        /** Cache for the info panel. */
        private JComponent infoPanel = null;

        /**
         * Prepares a new <code>ServicesInfo</code> object.
         *
         * @param name
         *      name that will be shown in the tree
         */
        public ServicesInfo(final String name) {
            super(name);
            setResource(new Resource(name));
            heartbeatGraph.setServicesInfo(this);
            initApplyButton();
        }

        /**
         * Sets info panel.
         * TODO: dead code?
         */
        public void setInfoPanel(final JComponent infoPanel) {
            this.infoPanel = infoPanel;
        }

        /**
         * Returns icon for services menu item.
         */
        public ImageIcon getMenuIcon() {
            return null;
        }

        /**
         * Returns names of all global parameters.
         */
        public String[] getParametersFromXML() {
            return hbOCF.getGlobalParameters();
        }

        /**
         * Returns long description of the global parameter, that is used for
         * tool tips.
         */
        protected String getParamLongDesc(final String param) {
            return hbOCF.getGlobalParamLongDesc(param);
        }

        /**
         * Returns short description of the gloval parameter, that is used as
         * label.
         */
        protected String getParamShortDesc(final String param) {
            return hbOCF.getGlobalParamShortDesc(param);
        }

        /**
         * Returns default for this global parameter.
         */
        protected String getParamDefault(final String param) {
            return hbOCF.getGlobalParamDefault(param);
        }

        /**
         * Returns possible choices for pulldown menus if applicable.
         */
        protected Object[] getParamPossibleChoices(final String param) {
            return hbOCF.getGlobalParamPossibleChoices(param);
        }

        /**
         * Checks if the new value is correct for the parameter type and
         * constraints.
         */
        protected boolean checkParam(final String param,
                                     final String newValue) {
            return hbOCF.checkGlobalParam(param, newValue);
        }

        /**
         * Returns whether the global parameter is of the integer type.
         */
        protected boolean isInteger(final String param) {
            return hbOCF.isGlobalInteger(param);
        }

        /**
         * Returns whether the global parameter is of the time type.
         */
        protected boolean isTimeType(final String param) {
            return hbOCF.isGlobalTimeType(param);
        }

        /**
         * Returns whether the global parameter is required.
         */
        protected boolean isRequired(final String param) {
            return hbOCF.isGlobalRequired(param);
        }

        /**
         * Returns whether the global parameter is of boolean type and
         * requires a checkbox.
         */
        protected boolean isCheckBox(final String param) {
            final String type = hbOCF.getGlobalParamType(param);
            if (type == null) {
                return false;
            }
            if (DRBD_RES_BOOL_TYPE_NAME.equals(type)) {
                return true;
            }
            return false;
        }

        /**
         * Returns type of the global parameter.
         */
        protected String getParamType(final String param) {
            return hbOCF.getGlobalParamType(param);
        }

        /**
         * Returns section to which the global parameter belongs.
         */
        protected String getSection(final String param) {
            return hbOCF.getGlobalSection(param);
        }

        /**
         * Applies changes that user has entered.
         */
        public void apply() {
            final String[] params = getParametersFromXML();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    applyButton.setEnabled(false);
                }
            });


            /* update heartbeat */
            final Map<String,String> args = new HashMap<String,String>();
            for (String param : params) {
                final String oldValue = getResource().getValue(param);
                String value = getComboBoxValue(param);
                if (value.equals(oldValue)) {
                    continue;
                }
                if (oldValue == null && value.equals(getParamDefault(param))) {
                    continue;
                }

                if ("".equals(value)) {
                    value = getParamDefault(param);
                }
                args.put(param, value);
            }
            if (!args.isEmpty()) {
                Heartbeat.setGlobalParameters(getDCHost(),
                                    args);
            }
            storeComboBoxValues(params);
        }

        /**
         * Sets heartbeat global parameters after they were obtained.
         */
        public void setGlobalConfig() {
            final String[] params = getParametersFromXML();
            for (String param : params) {
                String value = heartbeatStatus.getGlobalParam(param);
                final String oldValue = getResource().getValue(param);
                //if (value == null) {
                //    value = "";
                //}
                if (value != null && !value.equals(oldValue)) {
                    getResource().setValue(param, value);
                    final GuiComboBox cb = paramComboBoxGet(param, null);
                    if (cb != null) {
                        cb.setValue(value);
                    }
                }
            }
            final Host[] hosts = cluster.getHostsArray();
            for (Host host : hosts) {
                if (!heartbeatStatus.isActiveNode(host.getName())) {
                    //TODO: something's missing here
                    System.out.println("is active node: " + host.getName());
                }
            }
            if (infoPanel == null) {
                selectMyself();
            }
        }

        /**
         * This functions goes through all services, constrains etc. in
         * heartbeatStatus and updates the internal structures and graph.
         */
        public void setAllResources() {
            final String[] allGroups = heartbeatStatus.getAllGroups();
            heartbeatGraph.clearVertexIsPresentList();
            List<ServiceInfo> groupServiceIsPresent =
                                                new ArrayList<ServiceInfo>();
            groupServiceIsPresent.clear();
            for (String group : allGroups) {
                //TODO: need hb class here
                for (final String hbId : heartbeatStatus.getGroupResources(group)) {
                    final HeartbeatService newHbService =
                                        heartbeatStatus.getResourceType(hbId);
                    if (newHbService == null) {
                        /* This is bad. There is a service but we do not have
                         * the heartbeat script of this service.
                         */
                        continue;
                    }
                    GroupInfo newGi = null;
                    if (!"none".equals(group)) {
                        newGi = (GroupInfo) heartbeatIdToServiceInfo.get(group);
                        if (newGi == null) {
                            final Point2D p = null;
                            newGi = (GroupInfo) heartbeatGraph.getServicesInfo().addServicePanel(
                                        hbOCF.getHbGroup(),
                                        p,
                                        true,
                                        group);
                            //newGi.getService().setHeartbeatId(group); // TODO: to late
                            newGi.getService().setNew(false);
                            addToHeartbeatIdList(newGi);
                        } else {
                            final Map<String, String> resourceNode =
                                            heartbeatStatus.getParamValuePairs(hbId);
                            newGi.setParameters(resourceNode);
                            heartbeatGraph.repaint();
                        }
                        heartbeatGraph.setVertexIsPresent(newGi);
                    }
                    /* continue of creating/updating of the
                     * service in the gui.
                     */
                    ServiceInfo newSi = heartbeatIdToServiceInfo.get(hbId);
                    final Map<String, String> resourceNode =
                                    heartbeatStatus.getParamValuePairs(hbId);
                    if (newSi == null) {
                        // TODO: get rid of the service name? (everywhere)
                        final String serviceName = newHbService.getName();
                        if (newHbService.isFilesystem()) {
                            newSi = new FilesystemInfo(serviceName,
                                                       newHbService,
                                                       hbId,
                                                       resourceNode);
                        } else if (newHbService.isDrbddisk()) {
                            newSi = new DrbddiskInfo(serviceName,
                                                     newHbService,
                                                     hbId,
                                                     resourceNode);

                        } else if (newHbService.isIPaddr()) {
                            newSi = new IPaddrInfo(serviceName,
                                                   newHbService,
                                                   hbId,
                                                   resourceNode);

                        } else {
                            newSi = new ServiceInfo(serviceName,
                                                    newHbService,
                                                    hbId,
                                                    resourceNode);
                        }
                        newSi.getService().setHeartbeatId(hbId);
                        addToHeartbeatIdList(newSi);
                        final Point2D p = null;

                        if (newGi == null) {
                            heartbeatGraph.getServicesInfo().addServicePanel(
                                                                        newSi,
                                                                        p,
                                                                        true);
                        } else {
                            newGi.addGroupServicePanel(newSi);
                        }
                    } else {
                        newSi.setParameters(resourceNode);
                        heartbeatGraph.repaint();
                    }
                    newSi.getService().setNew(false);
                    heartbeatGraph.setVertexIsPresent(newSi);
                    if (newGi != null) {
                        groupServiceIsPresent.add(newSi);
                    }
                }
            }
            heartbeatGraph.clearColocationList();
            final Map<String, List<String>> colocationMap =
                                            heartbeatStatus.getColocationMap();
            for (final String heartbeatIdP : colocationMap.keySet()) {
                final List<String> tos = colocationMap.get(heartbeatIdP);
                for (final String heartbeatId : tos) {
                    //final String heartbeatId = colocationMap.get(heartbeatIdP);
                    final ServiceInfo si  =
                                    heartbeatIdToServiceInfo.get(heartbeatId);
                    final ServiceInfo siP =
                                    heartbeatIdToServiceInfo.get(heartbeatIdP);
                    heartbeatGraph.addColocation(siP, si);
                }
            }

            heartbeatGraph.clearOrderList();
            final Map<String, List<String>> orderMap =
                                            heartbeatStatus.getOrderMap();
            for (final String heartbeatIdP : orderMap.keySet()) {
                for (final String heartbeatId : orderMap.get(heartbeatIdP)) {
                    final ServiceInfo si =
                            heartbeatIdToServiceInfo.get(heartbeatId);
                    if (si != null) { /* not yet complete */
                        final ServiceInfo siP =
                                    heartbeatIdToServiceInfo.get(heartbeatIdP);
                        if (siP != null && siP.getName() != null) { 
                            /* dangling orders and colocations */
                            if (siP.getName().equals("drbddisk")
                                && si.getName().equals("Filesystem")) {
                                final List<String> colIds =
                                                colocationMap.get(heartbeatIdP);
                                // TODO: race here
                                if (colIds != null) {
                                    for (String colId : colIds) {
                                        if (colId != null
                                            && colId.equals(heartbeatId)) {
                                            /* drbddisk -> Filesystem */
                                            ((FilesystemInfo) si).setDrbddiskInfo((DrbddiskInfo) siP);
                                            final DrbdResourceInfo dri = drbdResHash.get(((DrbddiskInfo) siP).getResourceName());
                                            if (dri != null) {
                                                dri.setUsedByHeartbeat(true);
                                            }
                                        }
                                    }
                                }
                            }
                            heartbeatGraph.addOrder(siP, si);
                        }
                    }
                }
            }
           
            final Enumeration e = getNode().children();
            while (e.hasMoreElements()) {
                DefaultMutableTreeNode n =
                          (DefaultMutableTreeNode) e.nextElement();
                final ServiceInfo g =
                                   (ServiceInfo) n.getUserObject();
                if (g.getHeartbeatService().isGroup()) {
                    final Enumeration ge = g.getNode().children();
                    while (ge.hasMoreElements()) {
                        DefaultMutableTreeNode gn =
                                  (DefaultMutableTreeNode) ge.nextElement();
                        final ServiceInfo s =
                                           (ServiceInfo) gn.getUserObject();
                        if (!groupServiceIsPresent.contains(s)
                            && !s.getService().isNew()) {
                            /* remove the group service from the menu that does
                             * not exist anymore. */
                            s.removeInfo();
                        }
                    }
                }
            }
            heartbeatGraph.killRemovedEdges();
            heartbeatGraph.killRemovedVertices();
        }

        /**
         * Clears the info panel cache, forcing it to reload.
         */
        public boolean selectAutomaticallyInTreeMenu() {
            return infoPanel == null;
        }


        /**
         * Returns info for info panel, that hb status failed or null, in which
         * case the getInfoPanel() function will show.
         */
        public String getInfo() {
            if (hbStatusFailed()) {
                return Tools.getString("ClusterBrowser.HbStatusFailed");
            }
            return null;
        }

        /**
         * Returns editable info panel for global heartbeat config.
         */
        public JComponent getInfoPanel() {
            /* if don't have hb status we don't have all the info we need here.
             * TODO: OR we need to get hb status only once
             */
            if (hbStatusFailed()) {
                return super.getInfoPanel();
            }
            if (infoPanel != null) {
                heartbeatGraph.pickBackground();
                return infoPanel;
            }
            infoPanel = new JPanel();
            infoPanel.setBackground(PANEL_BACKGROUND);
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            //infoPanel.add(getButtonPanel());
            if (hbOCF == null) {
                return infoPanel;
            }
            final JPanel mainPanel = new JPanel();
            mainPanel.setBackground(PANEL_BACKGROUND);
            mainPanel.setLayout(new BoxLayout(mainPanel,
                                              BoxLayout.Y_AXIS));

            final JPanel optionsPanel = new JPanel();
            optionsPanel.setBackground(PANEL_BACKGROUND);
            optionsPanel.setLayout(new BoxLayout(optionsPanel,
                                                 BoxLayout.Y_AXIS));
            optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            final JPanel extraOptionsPanel = new JPanel();
            extraOptionsPanel.setBackground(EXTRA_PANEL_BACKGROUND);
            extraOptionsPanel.setLayout(new BoxLayout(extraOptionsPanel,
                                                      BoxLayout.Y_AXIS));
            extraOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);


            final JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.setBackground(STATUS_BACKGROUND);
            buttonPanel.setMinimumSize(new Dimension(0, 50));
            buttonPanel.setPreferredSize(new Dimension(0, 50));
            buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
            /* expert mode */
            buttonPanel.add(Tools.expertModeButton(extraOptionsPanel),
                                                   BorderLayout.WEST);

            final JMenuBar mb = new JMenuBar();
            mb.setBackground(PANEL_BACKGROUND);
            final JMenu serviceCombo = getActionsMenu();
            updateMenus(null);
            mb.add(serviceCombo);
            buttonPanel.add(mb, BorderLayout.EAST);

            infoPanel.add(buttonPanel);

            final String[] params = getParametersFromXML();
            addParams(optionsPanel,
                      extraOptionsPanel,
                      params,
                      Tools.getDefaultInt("ClusterBrowser.DrbdResLabelWidth"),
                      Tools.getDefaultInt("ClusterBrowser.DrbdResFieldWidth")
                      );

            applyButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(
                            new Runnable() {
                                public void run() {
                                    hbStatusLock();
                                    apply();
                                    hbStatusUnlock();
                                }
                            }
                        );
                        thread.start();
                    }
                }
            );

            /* apply button */
            addApplyButton(mainPanel);
            applyButton.setEnabled(checkResourceFields(null, params));
            mainPanel.add(optionsPanel);
            mainPanel.add(extraOptionsPanel);

            infoPanel.add(new JScrollPane(mainPanel));
            infoPanel.add(Box.createVerticalGlue());

            heartbeatGraph.pickBackground();
            return infoPanel;
        }

        /**
         * Returns heartbeat graph.
         */
        public JPanel getGraphicalView() {
            return heartbeatGraph.getGraphPanel();
        }

        /**
         * Adds service to the list of services.
         * TODO: are they both used?
         */
        public ServiceInfo addServicePanel(final HeartbeatService newHbService,
                                           final Point2D pos,
                                           final boolean reloadNode,
                                           final String heartbeatId) {
            ServiceInfo newServiceInfo;

            final String name = newHbService.getName();
            if (newHbService.isFilesystem()) {
                newServiceInfo = new FilesystemInfo(name, newHbService);
            } else if (newHbService.isDrbddisk()) {
                newServiceInfo = new DrbddiskInfo(name, newHbService);
            } else if (newHbService.isIPaddr()) {
                newServiceInfo = new IPaddrInfo(name, newHbService);
            } else if (newHbService.isGroup()) {
                newServiceInfo = new GroupInfo(newHbService);
            } else {
                newServiceInfo = new ServiceInfo(name, newHbService);
            }
            if (heartbeatId != null) {
                addToHeartbeatIdList(newServiceInfo);
                newServiceInfo.getService().setHeartbeatId(heartbeatId);
            }
            addServicePanel(newServiceInfo, pos, reloadNode);
            return newServiceInfo;
        }

        /**
         * Adds new service to the specified position. If position is null, it
         * will be computed later. ReloadNode specifies if the node in
         * the menu should be reloaded and get uptodate.
         */
        public void addServicePanel(final ServiceInfo newServiceInfo,
                                    final Point2D pos,
                                    final boolean reloadNode) {

            newServiceInfo.getService().setHeartbeatClass(
                    newServiceInfo.getHeartbeatService().getHeartbeatClass());

            if (!heartbeatGraph.addResource(newServiceInfo, null, pos)) {
                addNameToServiceInfoHash(newServiceInfo);
                final DefaultMutableTreeNode newServiceNode =
                                    new DefaultMutableTreeNode(newServiceInfo);
                newServiceInfo.setNode(newServiceNode);
                servicesNode.add(newServiceNode);
                if (reloadNode) {
                    /* show it */
                    reload(servicesNode);
                    reload(newServiceNode);
                }
                //heartbeatGraph.scale();
            }
            heartbeatGraph.reloadServiceMenus();
        }


        /**
         * Returns 'add service' list for graph popup menu.
         */
        public List<HeartbeatService> getAddServiceList(final String cl) {
            return globalGetAddServiceList(cl);
        }

        /**
         * Returns background popup. Click on background represents cluster as
         * whole.
         */
        public List<UpdatableItem> createPopup() {
            final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

            final MyMenuItem removeMenuItem = new MyMenuItem(
                    Tools.getString(
                            "ClusterBrowser.Hb.RemoveAllServices"),
                    REMOVE_ICON) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return !getExistingServiceList().isEmpty();
                }

                public void action() {
                    if (Tools.confirmDialog(
                         Tools.getString(
                             "ClusterBrowser.confirmRemoveAllServices.Title"),
                         Tools.getString(
                         "ClusterBrowser.confirmRemoveAllServices.Description"),
                         Tools.getString(
                             "ClusterBrowser.confirmRemoveAllServices.Yes"),
                         Tools.getString(
                             "ClusterBrowser.confirmRemoveAllServices.No"))) {

                        for (ServiceInfo si : getExistingServiceList()) {
                            si.removeMyselfNoConfirm();
                        }
                        heartbeatGraph.getVisualizationViewer().repaint();
                    }
                }
            };
            items.add((UpdatableItem) removeMenuItem);
            registerMenuItem((UpdatableItem) removeMenuItem);

            /* add group */
            final MyMenuItem addGroupMenuItem =
                new MyMenuItem(Tools.getString("ClusterBrowser.Hb.AddGroup"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return true;
                    }

                    public void action() {
                        final StringInfo gi = new StringInfo(HB_GROUP_NAME,
                                                             HB_GROUP_NAME);
                        addServicePanel(hbOCF.getHbGroup(), getPos(), true, null);
                        heartbeatGraph.getVisualizationViewer().repaint();
                    }
                };
            items.add((UpdatableItem) addGroupMenuItem);
            registerMenuItem((UpdatableItem) addGroupMenuItem);

            /* add service */
            final MyMenu addServiceMenuItem = new MyMenu(
                            Tools.getString("ClusterBrowser.Hb.AddService")) {
                private static final long serialVersionUID = 1L;

                public void update() {
                    super.update();
                    removeAll();
                    Point2D pos = getPos();
                    final HeartbeatService fsService =
                                    hbOCF.getHbService("Filesystem",
                                                              "ocf");
                    if (fsService != null) { /* just skip it, if it is not*/
                        final MyMenuItem fsMenuItem =
                               new MyMenuItem(fsService.getMenuName()) {
                            private static final long serialVersionUID = 1L;
                            public void action() {
                                getPopup().setVisible(false);
                                addServicePanel(fsService,
                                                getPos(),
                                                true, null);
                                heartbeatGraph.getVisualizationViewer().repaint();
                            }
                        };
                        fsMenuItem.setPos(pos);
                        add(fsMenuItem);
                    }
                    final HeartbeatService ipService =
                                    hbOCF.getHbService("IPaddr2",
                                                              "ocf");
                    if (ipService != null) { /* just skip it, if it is not*/
                        final MyMenuItem ipMenuItem =
                               new MyMenuItem(ipService.getMenuName()) {
                            private static final long serialVersionUID = 1L;
                            public void action() {
                                getPopup().setVisible(false);
                                addServicePanel(ipService,
                                                getPos(),
                                                true, null);
                                heartbeatGraph.getVisualizationViewer().repaint();
                            }
                        };
                        ipMenuItem.setPos(pos);
                        add(ipMenuItem);
                    }
                    for (final String cl : HB_CLASSES) {
                        final MyMenu classItem =
                                            new MyMenu(HB_CLASS_MENU.get(cl));
                        DefaultListModel m = new DefaultListModel();
                        for (final HeartbeatService s : getAddServiceList(cl)) {
                            final MyMenuItem mmi =
                                    new MyMenuItem(s.getMenuName()) {
                                private static final long serialVersionUID = 1L;
                                public void action() {
                                    getPopup().setVisible(false);
                                    addServicePanel(s, getPos(), true, null);
                                    heartbeatGraph.repaint();
                                }
                            };
                            mmi.setPos(pos);
                            m.addElement(mmi);
                        }
                        classItem.add(Tools.getScrollingMenu(classItem, m));
                        add(classItem);
                    }
                }
            };
            items.add((UpdatableItem) addServiceMenuItem);
            registerMenuItem((UpdatableItem) addServiceMenuItem);

            /* view logs */
            final MyMenuItem viewLogsItem =
                new MyMenuItem(Tools.getString("ClusterBrowser.Hb.ViewLogs"),
                               null,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return true;
                    }

                    public void action() {
                        ClusterLogs l = new ClusterLogs(getCluster());
                        l.showDialog();
                    }
                };
            items.add((UpdatableItem) viewLogsItem);
            registerMenuItem((UpdatableItem) viewLogsItem);
            return items;
        }

        /**
         * Return list of all services.
         */
        public List<ServiceInfo> getExistingServiceList() {
            final List<ServiceInfo> existingServiceList =
                                            new ArrayList<ServiceInfo>();
            for (final String name : nameToServiceInfoHash.keySet()) {
                final Map<String, ServiceInfo> idHash =
                                            nameToServiceInfoHash.get(name);
                for (final String id : idHash.keySet()) {
                    final ServiceInfo si = idHash.get(id);
                    existingServiceList.add(si);
                }
            }
            return existingServiceList;
        }
    }

    /**
     * Returns nw hb connection info object. This is called from heartbeat
     * graph.
     */
    public final HbConnectionInfo getNewHbConnectionInfo(final ServiceInfo si,
                                                         final ServiceInfo p) {
        return new HbConnectionInfo(si, p);
    }

    /**
     * This class describes a connection between two heartbeat services.
     * It can be order, colocation or both. Colocation although technically
     * don't have child, parent relationship, they are stored as such.
     */
    public class HbConnectionInfo extends Info {

        /** Cache for the info panel. */
        private JComponent infoPanel = null;
        /** Connected child service. */
        private ServiceInfo serviceInfo;
        /** Connected parent service. */
        private ServiceInfo serviceInfoParent;

        /**
         * Prepares a new <code>HbConnectionInfo</code> object.
         */
        public HbConnectionInfo(final ServiceInfo serviceInfo,
                                final ServiceInfo serviceInfoParent) {
            super("HbConnectionInfo");
            this.serviceInfo = serviceInfo;
            this.serviceInfoParent = serviceInfoParent;
        }

        /**
         * Returns service info object of the child in this connection.
         */
        public final ServiceInfo getServiceInfo() {
            return serviceInfo;
        }

        /**
         * Returns heartbeat graphical view.
         */
        public final JPanel getGraphicalView() {
            return heartbeatGraph.getGraphPanel();
        }

        /**
         * Returns parent that is connected to this service with constraint.
         */
        public final ServiceInfo getServiceInfoParent() {
            return serviceInfoParent;
        }

        /**
         * Returns info panel for hb connection (order and/or colocation
         * constraint.
         */
        public final JComponent getInfoPanel() {
            if (infoPanel != null) {
                return infoPanel;
            }

            final JPanel buttonPanel = new JPanel(new BorderLayout());
            buttonPanel.setBackground(STATUS_BACKGROUND);
            buttonPanel.setMinimumSize(new Dimension(0, 50));
            buttonPanel.setPreferredSize(new Dimension(0, 50));
            buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

            /* Actions */
            final JMenuBar mb = new JMenuBar();
            mb.setBackground(PANEL_BACKGROUND);
            final JMenu serviceCombo = getActionsMenu();
            updateMenus(null);
            mb.add(serviceCombo);
            buttonPanel.add(mb, BorderLayout.EAST);

            infoPanel = new JPanel();
            infoPanel.setBackground(PANEL_BACKGROUND);
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.add(buttonPanel);
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.setMinimumSize(new Dimension(
                    Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                    Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")
                    ));
            infoPanel.setPreferredSize(new Dimension(
                    Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                    Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")
                    ));
            return infoPanel;
        }

        /**
         * Creates popup menu for heartbeat order and colocation dependencies.
         * These are the edges in the graph.
         */
        public final List<UpdatableItem> createPopup() {
            final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

            final HbConnectionInfo thisClass = this;

            final MyMenuItem removeEdgeItem = new MyMenuItem(
                         Tools.getString("ClusterBrowser.Hb.RemoveEdge"),
                         REMOVE_ICON,
                         Tools.getString("ClusterBrowser.Hb.RemoveEdge.ToolTip")
                        ) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return true;
                }

                public void action() {
                    heartbeatGraph.removeConnection(thisClass);
                }
            };
            registerMenuItem(removeEdgeItem);
            items.add(removeEdgeItem);

            /* remove/add order */
            final MyMenuItem removeOrderItem =
                new MyMenuItem(Tools.getString("ClusterBrowser.Hb.RemoveOrder"),
                    REMOVE_ICON,
                    Tools.getString("ClusterBrowser.Hb.RemoveOrder.ToolTip"),

                    Tools.getString("ClusterBrowser.Hb.AddOrder"),
                    null,
                    Tools.getString("ClusterBrowser.Hb.AddOrder.ToolTip")) {
                private static final long serialVersionUID = 1L;

                public boolean predicate() {
                    return heartbeatGraph.isOrder(thisClass);
                }

                public boolean enablePredicate() {
                    return true;
                }

                public void action() {
                    //Vertex v = (Vertex)p.getSecond();
                    //Vertex parent = (Vertex)p.getFirst();
                    //ServiceInfo si = (ServiceInfo)getInfo(v);
                    //ServiceInfo siP = (ServiceInfo)getInfo(parent);
                    if (this.getText().equals(Tools.getString(
                                            "ClusterBrowser.Hb.RemoveOrder"))) {
                        heartbeatGraph.removeOrder(thisClass);
                        //edgeIsOrderList.remove(edge);
                        //si.removeOrder(siP);
                    } else {
                        heartbeatGraph.addOrder(thisClass);
                        //addOrder(siP, si);
                        //si.addOrder(siP);
                    }
                }
            };

            registerMenuItem(removeOrderItem);
            items.add(removeOrderItem);

            /* remove/add colocation */
            final MyMenuItem removeColocationItem =
                    new MyMenuItem(
                        Tools.getString("ClusterBrowser.Hb.RemoveColocation"),
                        REMOVE_ICON,
                        Tools.getString(
                                "ClusterBrowser.Hb.RemoveColocation.ToolTip"),

                        Tools.getString("ClusterBrowser.Hb.AddColocation"),
                        null,
                        Tools.getString(
                                "ClusterBrowser.Hb.AddColocation.ToolTip")
                       ) {
                private static final long serialVersionUID = 1L;

                public boolean predicate() {
                    //return edgeIsColocationList.contains(edge);
                    return heartbeatGraph.isColocation(thisClass);
                }

                public boolean enablePredicate() {
                    return true;
                }

                public void action() {
                    if (this.getText().equals(Tools.getString(
                                       "ClusterBrowser.Hb.RemoveColocation"))) {
                        heartbeatGraph.removeColocation(thisClass);
                    } else {
                        heartbeatGraph.addColocation(thisClass);
                    }
                }
            };

            registerMenuItem(removeColocationItem);
            items.add(removeColocationItem);

            /* reverse order */
            final MyMenuItem reverseOrderItem =
                new MyMenuItem(
                    Tools.getString("ClusterBrowser.Hb.ReverseOrder"),
                    null,
                    Tools.getString("ClusterBrowser.Hb.ReverseOrder.ToolTip")) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return heartbeatGraph.isOrder(thisClass);
                }

                public void action() {
                    heartbeatGraph.removeOrder(thisClass);
                    ServiceInfo t = serviceInfo;
                    serviceInfo = serviceInfoParent;
                    serviceInfoParent = t;
                    heartbeatGraph.addOrder(thisClass);
                }
            };
            registerMenuItem(reverseOrderItem);
            items.add(reverseOrderItem);
            return items;
        }
    }

    /**
     * This class provides drbd info. For one it shows the editable global
     * drbd config, but if a drbd block device is selected it forwards to the
     * block device info, which is defined in HostBrowser.java.
     */
    public class DrbdInfo extends EditableInfo {
        /** Selected block device. */
        private BlockDevInfo selectedBD = null;
        /** Cache for the info panel. */
        private JComponent infoPanel    = null;

        /**
         * Prepares a new <code>DrbdInfo</code> object.
         *
         * @param name
         *      name that will be shown in the tree
         */
        public DrbdInfo(final String name) {
            super(name);
            setResource(new Resource(name));
            drbdGraph.setDrbdInfo(this);
            initApplyButton();
        }

        /**
         * Returns menu drbd icon.
         */
        public final ImageIcon getMenuIcon() {
            return null;
        }

        /**
         * Sets selected block device.
         */
        public final void setSelectedNode(final BlockDevInfo bdi) {
            this.selectedBD = bdi;
        }

        /**
         * Gets combo box for paremeter in te global config. usage-count is
         * disabled.
         */
        protected final GuiComboBox getParamComboBox(final String param,
                                                     final String prefix,
                                                     final int width) {
            final GuiComboBox cb = super.getParamComboBox(param, prefix, width);
            if ("usage-count".equals(param)) {
                cb.setEnabled(false);
            }
            return cb;
        }

        /**
         * Creates drbd config.
         */
        public final void createDrbdConfig()
                   throws Exceptions.DrbdConfigException {
            final StringBuffer config = new StringBuffer(160);
            config.append("## generated by drbd-gui ");
            config.append(Tools.getRelease());
            config.append("\n\n");

            final StringBuffer global = new StringBuffer(80);
            final String[] params = drbdXML.getGlobalParams();
            global.append("global {\n");
            for (String param : params) {
                if ("usage-count".equals(param)) {
                    final String value = "yes";
                    global.append("\t\t");
                    global.append(param);
                    global.append('\t');
                    global.append(Tools.escapeConfig(value));
                    global.append(";\n");
                } else {
                    String value = getResource().getValue(param);
                    if (value == null && "usage-count".equals(param)) {
                        value = "yes";
                    }
                    if (value == null) {
                        continue;
                    }
                    if (!value.equals(drbdXML.getParamDefault(param))) {
                        global.append("\t\t");
                        global.append(param);
                        global.append('\t');
                        global.append(Tools.escapeConfig(value));
                        global.append(";\n");
                    }
                }
            }
            global.append("}\n");
            if (global.length() > 0) {
                config.append(global);
            }
            final Host[] hosts = cluster.getHostsArray();
            for (Host host : hosts) {
                final StringBuffer resConfig = new StringBuffer("");
                final Enumeration drbdResources = drbdNode.children();
                while (drbdResources.hasMoreElements()) {
                    DefaultMutableTreeNode n =
                           (DefaultMutableTreeNode) drbdResources.nextElement();
                    final DrbdResourceInfo drbdRes =
                                        (DrbdResourceInfo) n.getUserObject();
                    if (drbdRes.resourceInHost(host)) {
                        resConfig.append('\n');
                        try {
                            resConfig.append(drbdRes.drbdResourceConfig());
                        } catch (Exceptions.DrbdConfigException dce) {
                            throw dce;
                        }
                    }
                }
                host.getSSH().createConfig(config.toString()
                                             + resConfig.toString(),
                                           "drbd.conf",
                                           "/etc/",
                                           "0600");
                //DRBD.adjust(host, "all"); // it can't be here
            }
        }

        /**
         * Returns lsit of all parameters as an array.
         */
        public final String[] getParametersFromXML() {
            return drbdXML.getGlobalParams();
        }

        /**
         * Checks parameter's new value if it is correct.
         */
        protected final boolean checkParam(final String param,
                                           final String newValue) {
            return drbdXML.checkParam(param, newValue);
        }

        /**
         * Returns default value of the parameter.
         */
        protected final String getParamDefault(final String param) {
            return drbdXML.getParamDefault(param);
        }

        /**
         * Possible choices for pulldown menus, or null if it is not a pull
         * down menu.
         */
        protected final Object[] getParamPossibleChoices(final String param) {
            return drbdXML.getPossibleChoices(param);
        }

        /**
         * Returns paramter short description, for user visible text.
         */
        protected final String getParamShortDesc(final String param) {
            return drbdXML.getParamShortDesc(param);
        }

        /**
         * Returns parameter long description, for tool tips.
         */
        protected final String getParamLongDesc(final String param) {
            return drbdXML.getParamLongDesc(param);
        }

        /**
         * Returns section to which this parameter belongs.
         * This is used for grouping in the info panel.
         */
        protected final String getSection(final String param) {
            return drbdXML.getSection(param);
        }

        /**
         * Returns whether the parameter is required.
         */
        protected final boolean isRequired(final String param) {
            return drbdXML.isRequired(param);
        }

        /**
         * Returns whether the parameter is of the integer type.
         */
        protected final boolean isInteger(final String param) {
            return drbdXML.isInteger(param);
        }

        /**
         * Returns whether the parameter is of the time type.
         */
        protected final boolean isTimeType(final String param) {
            /* not required */
            return false;
        }

        /**
         * Returns true if unit has prefix.
         */
        protected final boolean hasUnitPrefix(final String param) {
            return drbdXML.hasUnitPrefix(param);
        }

        /**
         * Returns long name of the unit, for user visible uses.
         */
        protected final String getUnitLong(final String param) {
            return drbdXML.getUnitLong(param);
        }

        /**
         * Returns the default unit of the parameter.
         */
        protected final String getDefaultUnit(final String param) {
            return drbdXML.getDefaultUnit(param);
        }

        /**
         * Returns whether the parameter is check box.
         */
        protected final boolean isCheckBox(final String param) {
            final String type = drbdXML.getParamType(param);
            if (type == null) {
                return false;
            }
            if (DRBD_RES_BOOL_TYPE_NAME.equals(type)) {
                return true;
            }
            return false;
        }

        /**
         * Returns parameter type, boolean etc.
         */
        protected final String getParamType(final String param) {
            return drbdXML.getParamType(param);
        }

        /**
         * Applies changes made in the info panel by user.
         */
        public final void apply() {
            final String[] params = getParametersFromXML();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    applyButton.setEnabled(false);
                }
            });
            storeComboBoxValues(params);
        }

        /**
         * Returns info panel for drbd. If a block device was selected, its
         * info panel is shown.
         */
        public final JComponent getInfoPanel() {
            if (selectedBD != null) { /* block device is not in drbd */
                return selectedBD.getInfoPanel();
            }
            if (infoPanel != null) {
                return infoPanel;
            }
            final JPanel mainPanel = new JPanel();
            if (drbdXML == null) {
                mainPanel.add(new JLabel("drbd info not available"));
                return mainPanel;
            }
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

            final JPanel extraOptionsPanel = new JPanel();
            extraOptionsPanel.setBackground(EXTRA_PANEL_BACKGROUND);
            extraOptionsPanel.setLayout(new BoxLayout(extraOptionsPanel,
                                                      BoxLayout.Y_AXIS));
            extraOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            mainPanel.add(buttonPanel);

            /* expert mode */
            buttonPanel.add(Tools.expertModeButton(extraOptionsPanel),
                            BorderLayout.WEST);

            /* Actions */
            final JMenuBar mb = new JMenuBar();
            mb.setBackground(PANEL_BACKGROUND);
            final JMenu serviceCombo = getActionsMenu();
            updateMenus(null);
            mb.add(serviceCombo);
            buttonPanel.add(mb, BorderLayout.EAST);

            final String[] params = getParametersFromXML();
            addParams(optionsPanel,
                      extraOptionsPanel,
                      params,
                      Tools.getDefaultInt("ClusterBrowser.DrbdResLabelWidth"),
                      Tools.getDefaultInt("ClusterBrowser.DrbdResFieldWidth")
                      );

            applyButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        Thread thread = new Thread(new Runnable() {
                            public void run() {
                                hbStatusLock();
                                apply();
                                hbStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );

            /* apply button */
            addApplyButton(mainPanel);
            applyButton.setEnabled(checkResourceFields(null, params));
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

        /**
         * Clears info panel cache.
         * TODO: should select something.
         */
        public final boolean selectAutomaticallyInTreeMenu() {
            return infoPanel == null;
        }

        /**
         * Returns drbd graph in a panel.
         */
        public final JPanel getGraphicalView() {
            if (selectedBD != null) {
                drbdGraph.pickBlockDevice(selectedBD);
            }
            return drbdGraph.getGraphPanel();
        }

        /**
         * Selects and highlights this node. This function is overwritten
         * because block devices don't have here their own node, but
         * views change depending on selectedNode variable.
         */
        public final void selectMyself() {
            if (selectedBD == null || !selectedBD.getBlockDevice().isDrbd()) {
                reload(getNode());
                nodeChanged(getNode());
            } else {
                reload(selectedBD.getNode());
                nodeChanged(selectedBD.getNode());
            }
        }

        /**
         * Returns new drbd resource index, the one that is not used .
         */
        private int getNewDrbdResourceIndex() {
            final Iterator it = drbdResHash.keySet().iterator();
            int index = -1;

            while (it.hasNext()) {
                final String name = (String) it.next();
                // TODO: should not assume r0
                final Pattern p = Pattern.compile("^" + "r" + "(\\d+)$");
                final Matcher m = p.matcher(name);

                if (m.matches()) {
                    final int i = Integer.parseInt(m.group(1));
                    if (i > index) {
                        index = i;
                    }
                }
            }
            return index + 1;
        }

        /**
         * Adds drbd resource. If resource name and drbd device are null.
         * They will be created with first unused index. E.g. r0, r1 ...
         * /dev/drbd0, /dev/drbd1 ... etc.
         *
         * @param name
         *              resource name.
         * @param drbdDevStr
         *              drbd device like /dev/drbd0
         * @param bd1
         *              block device
         * @param bd2
         *              block device
         * @param interactive
         *              whether dialog box will be displayed
         */
        public final void addDrbdResource(String name,
                             String drbdDevStr,
                             final BlockDevInfo bd1,
                             final BlockDevInfo bd2,
                             final boolean interactive) {
            DrbdResourceInfo dri;
            if (bd1 == null || bd2 == null) {
                return;
            }

            if (name == null && drbdDevStr == null) {
                int index = getNewDrbdResourceIndex();
                name = "r" + Integer.toString(index);
                drbdDevStr = "/dev/drbd" + Integer.toString(index);

                /* search for next available drbd device */
                while (drbdDevHash.containsKey(drbdDevStr)) {
                    index++;
                    drbdDevStr = "/dev/drbd" + Integer.toString(index);
                }
                dri = new DrbdResourceInfo(name, drbdDevStr, bd1, bd2);
            } else {
                dri = new DrbdResourceInfo(name, drbdDevStr, bd1, bd2);

                final String[] sections = drbdXML.getSections();
                for (String section : sections) {
                    final String[] params = drbdXML.getSectionParams(section);
                    for (String param : params) {
                        String value =
                            drbdXML.getConfigValue(name, section, param);
                        if ("".equals(value)) {
                            value = drbdXML.getParamDefault(param);
                        }
                        dri.getDrbdResource().setValue(param, value);
                    }
                }
                dri.getDrbdResource().setCommited(true);
            }
            /* We want next port number on both devices to be the same,
             * although other port numbers may not be the same on both. */
            final int viPort1 = bd1.getNextVIPort();
            final int viPort2 = bd2.getNextVIPort();
            final int viPort;
            if (viPort1 > viPort2) {
                viPort = viPort1;
            } else {
                viPort = viPort2;
            }
            bd1.setDefaultVIPort(viPort + 1);
            bd2.setDefaultVIPort(viPort + 1);

            dri.getDrbdResource().setDefaultValue(DRBD_RES_PARAM_NAME,
                                                  name);
            dri.getDrbdResource().setDefaultValue(DRBD_RES_PARAM_DEV,
                                                  drbdDevStr);
            drbdResHash.put(name, dri);
            drbdDevHash.put(drbdDevStr, dri);

            if (bd1 != null) {
                bd1.setDrbd(true);
                bd1.setDrbdResourceInfo(dri);
                bd1.setInfoPanel(null); /* reload panel */
                bd1.selectMyself();
            }
            if (bd2 != null) {
                bd2.setDrbd(true);
                bd2.setDrbdResourceInfo(dri);
                bd2.setInfoPanel(null); /* reload panel */
                bd2.selectMyself();
            }

            final DefaultMutableTreeNode drbdResourceNode =
                new DefaultMutableTreeNode(dri);
            dri.setNode(drbdResourceNode);

            drbdNode.add(drbdResourceNode);

            final DefaultMutableTreeNode drbdBDNode1 =
                new DefaultMutableTreeNode(bd1);
            bd1.setNode(drbdBDNode1);
            final DefaultMutableTreeNode drbdBDNode2 =
                new DefaultMutableTreeNode(bd2);
            bd2.setNode(drbdBDNode2);
            drbdResourceNode.add(drbdBDNode1);
            drbdResourceNode.add(drbdBDNode2);

            //reload(getNode());
            //reload(drbdResourceNode);
            drbdGraph.addDrbdResource(dri, bd1, bd2);
            final DrbdResourceInfo driF = dri;
            if (interactive) {
                final Thread thread = new Thread(
                    new Runnable() {
                        public void run() {
                            AddDrbdConfigDialog adrd
                                = new AddDrbdConfigDialog(driF);
                            adrd.showDialogs();
                            if (adrd.isCanceled()) {
                                driF.removeMyselfNoConfirm();
                                return;
                            }

                            updateCommonBlockDevices();
                            drbdXML.update(bd1.getHost());
                            drbdXML.update(bd2.getHost());
                            resetFilesystems();
                        } });
                thread.start();
            } else {
                resetFilesystems();
            }
        }
    }

    /**
     * This class holds all hosts that are added to the GUI as opposite to all
     * hosts in a cluster.
     */
    private class AllHostsInfo extends Info {
        /** infoPanel cache. */
        private JPanel infoPanel = null;

        /**
         * Creates a new AllHostsInfo instance.
         */
        public AllHostsInfo() {
            super(Tools.getString("ClusterBrowser.AllHosts"));
        }

        /**
         * Returns info panel of all hosts menu item. If a host is selected,
         * its tab is selected.
         */
        public final JComponent getInfoPanel() {
            if (infoPanel != null) {
                return infoPanel;
            }
            infoPanel = new JPanel();

            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setBackground(PANEL_BACKGROUND);
            final JPanel bPanel =
                           new JPanel(new BorderLayout());
            bPanel.setMaximumSize(new Dimension(10000, 60));
            bPanel.setBackground(STATUS_BACKGROUND);
            final JMenuBar mb = new JMenuBar();
            mb.setBackground(PANEL_BACKGROUND);
            final JMenu actionsMenu = getActionsMenu();
            updateMenus(null);
            mb.add(actionsMenu);
            bPanel.add(mb, BorderLayout.EAST);
            infoPanel.add(bPanel);
            return infoPanel;
        }

        /**
         * Creates the popup for all hosts.
         */
        public final List<UpdatableItem> createPopup() {
            final List<UpdatableItem>items = new ArrayList<UpdatableItem>();

            /* host wizard */
            final MyMenuItem newHostWizardItem =
                new MyMenuItem(Tools.getString("EmptyBrowser.NewHostWizard"),
                               HOST_ICON,
                               null) {
                    private static final long serialVersionUID = 1L;

                    public boolean enablePredicate() {
                        return true;
                    }

                    public void action() {
                        final AddHostDialog dialog = new AddHostDialog();
                        dialog.showDialogs();
                    }
                };
            items.add(newHostWizardItem);
            registerMenuItem(newHostWizardItem);
            Tools.getGUIData().registerAddHostButton(newHostWizardItem);
            return items;
        }
    }
}
