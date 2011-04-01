/*
 * This file is part of DRBD Management Console by Rasto Levrinc,
 * LINBIT HA-Solutions GmbH
 *
 * Copyright (C) 2009, Rastislav Levrinc.
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
import drbd.data.PtestData;
import drbd.data.DRBDtestData;

import drbd.data.Host;
import drbd.data.Cluster;
import drbd.data.ClusterStatus;
import drbd.data.CRMXML;
import drbd.data.DrbdXML;
import drbd.data.VMSXML;
import drbd.data.ConfigData;
import drbd.utilities.NewOutputCallback;

import drbd.utilities.ExecCallback;
import drbd.utilities.Heartbeat;
import drbd.utilities.CRM;
import drbd.data.resources.Service;
import drbd.data.resources.Network;

import drbd.gui.resources.DrbdResourceInfo;
import drbd.gui.resources.HbCategoryInfo;
import drbd.gui.resources.HbConnectionInfo;
import drbd.gui.resources.Info;
import drbd.gui.resources.CategoryInfo;
import drbd.gui.resources.AllHostsInfo;
import drbd.gui.resources.ServicesInfo;
import drbd.gui.resources.ServiceInfo;
import drbd.gui.resources.GroupInfo;
import drbd.gui.resources.StringInfo;
import drbd.gui.resources.BlockDevInfo;
import drbd.gui.resources.NetworkInfo;
import drbd.gui.resources.DrbdInfo;
import drbd.gui.resources.AvailableServiceInfo;
import drbd.gui.resources.CommonBlockDevInfo;
import drbd.gui.resources.CRMInfo;
import drbd.gui.resources.VMSVirtualDomainInfo;
import drbd.gui.resources.VMSInfo;
import drbd.gui.resources.VMSHardwareInfo;
import drbd.gui.resources.AvailableServicesInfo;
import drbd.gui.resources.ResourceAgentClassInfo;
import drbd.gui.resources.ClusterHostsInfo;
import drbd.gui.resources.RscDefaultsInfo;

import drbd.data.ResourceAgent;
import drbd.utilities.ComponentWithTest;
import drbd.utilities.ButtonCallback;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;

import java.awt.Color;
import java.awt.geom.Point2D;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.CountDownLatch;
import java.util.Locale;
import EDU.oswego.cs.dl.util.concurrent.Mutex;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.collections.map.LinkedMap;


/**
 * This class holds cluster resource data in a tree. It shows panels that allow
 * to edit data of services etc.
 * Every resource has its Info object, that accessible through the tree view.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class ClusterBrowser extends Browser {
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
    /** Heartbeat node. */
    private DefaultMutableTreeNode heartbeatNode;
    /** Menu's heartbeat services node. */
    private DefaultMutableTreeNode servicesNode;
    /** Menu's drbd node. */
    private DefaultMutableTreeNode drbdNode;
    /** Update VMS lock. */
    private final Mutex mUpdateVMSlock = new Mutex();
    /** Menu's VMs node. */
    private DefaultMutableTreeNode vmsNode = null;
    /** Common file systems on all cluster nodes. */
    private String[] commonFileSystems;
    /** Common mount points on all cluster nodes. */
    private String[] commonMountPoints;

    /** name (hb type) + id to service info hash. */
    private final Map<String, Map<String, ServiceInfo>> nameToServiceInfoHash =
                                new TreeMap<String, Map<String, ServiceInfo>>(
                                                String.CASE_INSENSITIVE_ORDER);
    /** Name to service hash lock. */
    private final Mutex mNameToServiceLock = new Mutex();
    /** DRBD resource hash lock. */
    private final Mutex mDrbdResHashLock = new Mutex();
    /** DRBD resource name string to drbd resource info hash. */
    private final Map<String, DrbdResourceInfo> drbdResHash =
                                new HashMap<String, DrbdResourceInfo>();
    /** DRBD device hash lock. */
    private final Mutex mDrbdDevHashLock = new Mutex();
    /** DRBD resource device string to drbd resource info hash. */
    private final Map<String, DrbdResourceInfo> drbdDevHash =
                                new HashMap<String, DrbdResourceInfo>();
    /** Heartbeat id to service lock. */
    private final Mutex mHeartbeatIdToService = new Mutex();
    /** Heartbeat id to service info hash. */
    private final Map<String, ServiceInfo> heartbeatIdToServiceInfo =
                                          new HashMap<String, ServiceInfo>();
    /** Heartbeat graph. */
    private final HeartbeatGraph heartbeatGraph;
    /** Drbd graph. */
    private final DrbdGraph drbdGraph;
    /** object that holds current heartbeat status. */
    private ClusterStatus clusterStatus;
    /** Object that holds hb ocf data. */
    private CRMXML crmXML;
    /** Object that holds drbd status and data. */
    private DrbdXML drbdXML;
    /** VMS lock. */
    private final Mutex mVMSLock = new Mutex();
    /** Object that hosts status of all VMs. */
    private final Map<Host, VMSXML> vmsXML = new HashMap<Host, VMSXML>();
    /** Object that has drbd test data. */
    private DRBDtestData drbdtestData;
    /** Whether drbd status was canceled by user. */
    private boolean drbdStatusCanceled = false;
    /** Whether hb status was canceled by user. */
    private boolean clStatusCanceled = false;
    /** Global hb status lock. */
    private final Mutex mClStatusLock = new Mutex();
    /** Global drbd status lock. */
    private final Mutex mDRBDStatusLock = new Mutex();
    /** Ptest lock. */
    private final Mutex mPtestLock = new Mutex();
    /** DRBD test data lock. */
    private final Mutex mDRBDtestdataLock = new Mutex();
    /** Can be used to cancel server status. */
    private volatile boolean serverStatus = true;
    /** last dc host detected. */
    private Host lastDcHost = null;
    /** dc host as reported by crm. */
    private Host realDcHost = null;
    /** Panel that holds this browser. */
    private ClusterViewPanel clusterViewPanel = null;
    /** Previous drbd configs. */
    private final Map<Host, String> oldDrbdConfigString =
                                                  new HashMap<Host, String>();
    /** Map to ResourceAgentClassInfo. */
    private final Map<String, ResourceAgentClassInfo> classInfoMap =
                                new HashMap<String, ResourceAgentClassInfo>();
    /** Map from ResourceAgent to AvailableServicesInfo. */
    private final Map<ResourceAgent, AvailableServiceInfo>
                    availableServiceMap =
                           new HashMap<ResourceAgent, AvailableServiceInfo>();
    /** Cluster hosts info object. */
    private ClusterHostsInfo clusterHostsInfo;
    /** Services info object. */
    private ServicesInfo servicesInfo = null;
    /** Rsc defaults info object. */
    private RscDefaultsInfo rscDefaultsInfo = null;
    /** Remove icon. */
    public static final ImageIcon REMOVE_ICON =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.RemoveIcon"));
    /** Remove icon small. */
    public static final ImageIcon REMOVE_ICON_SMALL =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.RemoveIconSmall"));
    /** Name of ocf style resource (heartbeat 2). */
    private static final String HB_OCF_CLASS = "ocf";
    /** Name of heartbeat style resource (heartbeat 1). */
    public static final String HB_HEARTBEAT_CLASS = "heartbeat";
    /** Name of lsb style resource (/etc/init.d/*). */
    public static final String HB_LSB_CLASS = "lsb";
    /** Name of stonith device class. */
    public static final String HB_STONITH_CLASS = "stonith";

    /** Hash that holds all hb classes with descriptions that appear in the
     * pull down menus. */
    public static final Map<String, String> HB_CLASS_MENU =
                                                new HashMap<String, String>();
    static {
        HB_CLASS_MENU.put(HB_OCF_CLASS,       "OCF Resource Agents");
        HB_CLASS_MENU.put(HB_HEARTBEAT_CLASS, "Heartbeat 1 RAs (deprecated)");
        HB_CLASS_MENU.put(HB_LSB_CLASS,       "LSB Init Scripts");
        HB_CLASS_MENU.put(HB_STONITH_CLASS,   "Stonith Devices");
    }
    /** Width of the label in the info panel. */
    public static final int SERVICE_LABEL_WIDTH =
                    Tools.getDefaultInt("ClusterBrowser.ServiceLabelWidth");
    /** Width of the field in the info panel. */
    public static final int SERVICE_FIELD_WIDTH =
                    Tools.getDefaultInt("ClusterBrowser.ServiceFieldWidth");
    /** Color for stopped services. */
    public static final Color FILL_PAINT_STOPPED =
                      Tools.getDefaultColor("HeartbeatGraph.FillPaintStopped");
    /** Identation. */
    public static final String IDENT_4 = "    ";
    /** Name of the boolean type in drbd. */
    public static final String DRBD_RES_BOOL_TYPE_NAME = "boolean";
    /** String array with all hb classes. */
    public static final String[] HB_CLASSES = {HB_OCF_CLASS,
                                               HB_HEARTBEAT_CLASS,
                                               HB_LSB_CLASS,
                                               HB_STONITH_CLASS};

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
    /** Promote operation. */
    public static final String HB_OP_PROMOTE = "promote";
    /** Demote operation. */
    public static final String HB_OP_DEMOTE = "demote";

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
    public static final String[] HB_OPERATIONS = {HB_OP_START,
                                                  HB_OP_PROMOTE,
                                                  HB_OP_DEMOTE,
                                                  HB_OP_STOP,
                                                  HB_OP_STATUS,
                                                  HB_OP_MONITOR,
                                                  HB_OP_META_DATA,
                                                  HB_OP_VALIDATE_ALL};
    /** Not advanced operations. */
    public static final MultiKeyMap HB_OP_NOT_ADVANCED = MultiKeyMap.decorate(
                                                              new LinkedMap());
    static {
        HB_OP_NOT_ADVANCED.put(HB_OP_START, HB_PAR_TIMEOUT, 1);
        HB_OP_NOT_ADVANCED.put(HB_OP_STOP, HB_PAR_TIMEOUT, 1);
        HB_OP_NOT_ADVANCED.put(HB_OP_MONITOR, HB_PAR_TIMEOUT, 1);
        HB_OP_NOT_ADVANCED.put(HB_OP_MONITOR, HB_PAR_INTERVAL, 1);
    }
    /** Operations that should not have default values. */
    public static final List<String> HB_OP_IGNORE_DEFAULT =
                                                      new ArrayList<String>();
    static {
        HB_OP_IGNORE_DEFAULT.add(HB_OP_STATUS);
        HB_OP_IGNORE_DEFAULT.add(HB_OP_META_DATA);
        HB_OP_IGNORE_DEFAULT.add(HB_OP_VALIDATE_ALL);
    }
    /** Parameters for the hb operations. */
    private final Map<String, List<String>> crmOperationParams =
                                     new LinkedHashMap<String, List<String>>();
    /** All parameters for the hb operations, so that it is possible to create
     * arguments for up_rsc_full_ops. */
    public static final String[] HB_OPERATION_PARAM_LIST = {HB_PAR_DESC,
                                                            HB_PAR_INTERVAL,
                                                            HB_PAR_TIMEOUT,
                                                            HB_PAR_START_DELAY,
                                                            HB_PAR_DISABLED,
                                                            HB_PAR_ROLE,
                                                            HB_PAR_PREREQ,
                                                            HB_PAR_ON_FAIL};
    /** Starting ptest tooltip. */
    public static final String STARTING_PTEST_TOOLTIP =
                                Tools.getString("ClusterBrowser.StartingPtest");
    /** Cluster status error string. */
    private static final String CLUSTER_STATUS_ERROR =
                                  "---start---\r\nerror\r\n\r\n---done---\r\n";
    /** Small cluster icon. */
    static final ImageIcon CLUSTER_ICON_SMALL = Tools.createImageIcon(
                          Tools.getDefault("ClusterBrowser.ClusterIconSmall"));
    /** String that appears as a tooltip in menu items if status was disabled.*/
    public static final String UNKNOWN_CLUSTER_STATUS_STRING =
                                                     "unknown cluster status";
    /** Prepares a new <code>CusterBrowser</code> object. */
    public ClusterBrowser(final Cluster cluster) {
        super();
        this.cluster = cluster;
        heartbeatGraph = new HeartbeatGraph(this);
        drbdGraph = new DrbdGraph(this);
        setTreeTop();

    }

    /** Inits operations. */
    private void initOperations() {
        crmOperationParams.put(HB_OP_START,
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL)));
        crmOperationParams.put(HB_OP_STOP,
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL)));
        crmOperationParams.put(HB_OP_META_DATA,
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL)));
        crmOperationParams.put(HB_OP_VALIDATE_ALL,
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL)));

        crmOperationParams.put(HB_OP_STATUS,
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL)));

        // TODO: need two monitors for role='Slave' and 'Master' in
        // master/slave resources
        final Host dcHost = getDCHost();
        final String hbV = dcHost.getHeartbeatVersion();
        final String pmV = dcHost.getPacemakerVersion();
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.1.4") <= 0) {
            crmOperationParams.put(HB_OP_MONITOR,
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL)));
        } else {
            crmOperationParams.put(HB_OP_MONITOR,
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL,
                                                          HB_PAR_START_DELAY)));
        }
        crmOperationParams.put(HB_OP_PROMOTE,
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL)));
        crmOperationParams.put(HB_OP_DEMOTE,
                                new ArrayList<String>(
                                            Arrays.asList(HB_PAR_TIMEOUT,
                                                          HB_PAR_INTERVAL)));
    }

    /** CRM operation parameters. */
    public Map<String, List<String>> getCRMOperationParams() {
        return crmOperationParams;
    }

    /** Sets the cluster view panel. */
    void setClusterViewPanel(final ClusterViewPanel clusterViewPanel) {
        this.clusterViewPanel = clusterViewPanel;
    }

    /** Returns cluster view panel. */
    ClusterViewPanel getClusterViewPanel() {
        return clusterViewPanel;
    }

    /** Returns all nodes that belong to this cluster. */
    public Host[] getClusterHosts() {
        return cluster.getHostsArray();
    }

    /** Returns cluster data object. */
    public Cluster getCluster() {
        return cluster;
    }

    /** Sets the info panel component in the cluster view panel. */
    public void setRightComponentInView(final Info i) {
        clusterViewPanel.setRightComponentInView(this, i);
    }

    /**
     * Saves positions of service and block devices from the heartbeat and drbd
     * graphs to the config files on every node.
     */
    public void saveGraphPositions() {
        final Map<String, Point2D> positions = new HashMap<String, Point2D>();
        if (drbdGraph != null) {
            drbdGraph.getPositions(positions);
        }
        if (positions.isEmpty()) {
            return;
        }
        if (heartbeatGraph != null) {
            heartbeatGraph.getPositions(positions);
        }
        if (positions.isEmpty()) {
            return;
        }

        final Host[] hosts = getClusterHosts();
        for (Host host : hosts) {
            host.saveGraphPositions(positions);
        }
    }

    /** Returns heartbeat graph for this cluster. */
    public HeartbeatGraph getHeartbeatGraph() {
        return heartbeatGraph;
    }

    /** Returns drbd graph for this cluster. */
    public DrbdGraph getDrbdGraph() {
        return drbdGraph;
    }

    /** Returns all hosts are don't get cluster status. */
    public boolean allHostsDown() {
       boolean hostsDown = true;
       final Host[] hosts = cluster.getHostsArray();
       for (Host host : hosts) {
           if (host.isClStatus()) {
               hostsDown = false;
               break;
           }
       }
       return hostsDown;
    }

    /** Returns whether there is at least one drbddisk resource. */
    public boolean atLeastOneDrbddisk() {
        try {
            mHeartbeatIdToService.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        for (final String id : heartbeatIdToServiceInfo.keySet()) {
            final ServiceInfo si = heartbeatIdToServiceInfo.get(id);
            if (si.getResourceAgent().isDrbddisk()) {
                mHeartbeatIdToService.release();
                return true;
            }
        }
        mHeartbeatIdToService.release();
        return false;
    }

    /** Returns whether there is at least one drbddisk resource. */
    public boolean isOneLinbitDrbd() {
        try {
            mHeartbeatIdToService.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        for (final String id : heartbeatIdToServiceInfo.keySet()) {
            final ServiceInfo si = heartbeatIdToServiceInfo.get(id);
            if (si.getResourceAgent().isLinbitDrbd()) {
                mHeartbeatIdToService.release();
                return true;
            }
        }
        mHeartbeatIdToService.release();
        return false;
    }

    /** Adds VMs node. */
    void addVMSNode() {
        /* VMs */
        if (vmsNode == null) {
            vmsNode = new DefaultMutableTreeNode(
                     new VMSInfo(Tools.getString("ClusterBrowser.VMs"), this));
            setNode(vmsNode);
            topAdd(vmsNode);
            reload(getTreeTop(), true);
        }
    }

    /** Initializes cluster resources for cluster view. */
    void initClusterBrowser() {
        /* all hosts */
        allHostsNode = new DefaultMutableTreeNode(
                                new AllHostsInfo(
                                        Tools.getGUIData().getEmptyBrowser()));
        setNode(allHostsNode);
        topAdd(allHostsNode);
        /* hosts */
        clusterHostsInfo =
           new ClusterHostsInfo(Tools.getString("ClusterBrowser.ClusterHosts"),
                                this);
        clusterHostsNode = new DefaultMutableTreeNode(clusterHostsInfo);
        setNode(clusterHostsNode);
        topAdd(clusterHostsNode);

        /* networks */
        networksNode = new DefaultMutableTreeNode(
            new CategoryInfo(Tools.getString("ClusterBrowser.Networks"),
                             this));
        setNode(networksNode);
        topAdd(networksNode);

        /* drbd */
        drbdNode = new DefaultMutableTreeNode(
            new DrbdInfo(Tools.getString("ClusterBrowser.Drbd"),
                         this));
        setNode(drbdNode);
        topAdd(drbdNode);

        /* CRM */
        final CRMInfo crmInfo = new CRMInfo(
                              Tools.getString("ClusterBrowser.ClusterManager"),
                              this);
        heartbeatNode = new DefaultMutableTreeNode(crmInfo);
        setNode(heartbeatNode);
        topAdd(heartbeatNode);

        /* available services */
        availableServicesNode = new DefaultMutableTreeNode(
            new AvailableServicesInfo(
                Tools.getString("ClusterBrowser.availableServices"),
                this));
        setNode(availableServicesNode);
        heartbeatNode.add(availableServicesNode);

        /* block devices / shared disks, TODO: */
        commonBlockDevicesNode = new DefaultMutableTreeNode(
            new HbCategoryInfo(
                Tools.getString("ClusterBrowser.CommonBlockDevices"), this));
        setNode(commonBlockDevicesNode);
        /* heartbeatNode.add(commonBlockDevicesNode); */

        /* resource defaults */
        rscDefaultsInfo = new RscDefaultsInfo("rsc_defaults", this);
        /* services */
        servicesInfo =
                new ServicesInfo(Tools.getString("ClusterBrowser.Services"),
                                 this);
        servicesNode = new DefaultMutableTreeNode(servicesInfo);
        setNode(servicesNode);
        heartbeatNode.add(servicesNode);
        addVMSNode();
        selectPath(new Object[]{getTreeTop(), heartbeatNode});
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
    void updateClusterResources(final Host[] clusterHosts,
                                final String[] commonFileSystems,
                                final String[] commonMountPoints) {
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
        reload(allHostsNode, false);

        /* cluster hosts */
        clusterHostsNode.removeAllChildren();
        for (Host clusterHost : clusterHosts) {
            final HostBrowser hostBrowser = clusterHost.getBrowser();
            resource = hostBrowser.getTreeTop();
            setNode(resource);
            clusterHostsNode.add(resource);
            heartbeatGraph.addHost(hostBrowser.getHostInfo());
        }

        reload(clusterHostsNode, false);

        /* block devices */
        updateCommonBlockDevices();

        /* networks */
        updateNetworks();
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                heartbeatGraph.scale();
            }
        });
        updateHeartbeatDrbdThread();
    }

    /** Returns mountpoints that exist on all servers. */
    public String[] getCommonMountPoints() {
        return commonMountPoints;
    }

    /** Starts everything. */
    private void updateHeartbeatDrbdThread() {
        final Thread tt = new Thread(new Runnable() {
            @Override public void run() {
                final Host[] hosts = cluster.getHostsArray();
                for (final Host host : hosts) {
                    host.waitForServerStatusLatch();
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        getClusterViewPanel().setDisabledDuringLoad(
                                                                false);
                        selectServices();
                    }
                });
            }
        });
        tt.start();
        final Runnable runnable = new Runnable() {
            @Override public void run() {
                Host firstHost = null;
                final Host[] hosts = cluster.getHostsArray();
                for (final Host host : hosts) {
                    final HostBrowser hostBrowser = host.getBrowser();
                    drbdGraph.addHost(hostBrowser.getHostDrbdInfo());
                }
                int notConnectedCount = 0;
                do { /* wait here until a host is connected. */
                    boolean notConnected = true;
                    for (final Host host : hosts) {
                        // TODO: fix that, use latches or callback
                        if (host.isConnected()) {
                            /* at least one connected. */
                            notConnected = false;
                            break;
                        }
                    }
                    if (notConnected) {
                        notConnectedCount++;
                    } else {
                        firstHost = getFirstHost();
                    }
                    if (firstHost == null) {
                        try {
                            Thread.sleep(30000);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        final boolean ok =
                                 cluster.connect(null,
                                                 notConnectedCount < 1,
                                                 notConnectedCount + 1);
                        if (!ok) {
                            break;
                        }
                    }
                } while (firstHost == null);
                if (firstHost == null) {
                    return;
                }

                crmXML = new CRMXML(firstHost);
                clusterStatus = new ClusterStatus(firstHost, crmXML);
                initOperations();
                final DrbdXML newDrbdXML = new DrbdXML(cluster.getHostsArray());
                for (final Host h : cluster.getHostsArray()) {
                    final String configString = newDrbdXML.getConfig(h);
                    newDrbdXML.update(configString);
                }
                drbdXML = newDrbdXML;
                getDrbdGraph().getDrbdInfo().setParameters();
                /* available services */
                final String clusterName = getCluster().getName();
                Tools.startProgressIndicator(clusterName,
                        Tools.getString("ClusterBrowser.HbUpdateResources"));

                updateAvailableServices();
                Tools.stopProgressIndicator(clusterName,
                    Tools.getString("ClusterBrowser.HbUpdateResources"));
                Tools.startProgressIndicator(clusterName,
                    Tools.getString("ClusterBrowser.DrbdUpdate"));

                updateDrbdResources();
                //SwingUtilities.invokeLater(new Runnable() {
                //    @Override public void run() {
                //        drbdGraph.scale();
                //    }
                //});
                //try { Thread.sleep(10000); }
                //catch (InterruptedException ex) {}
                //SwingUtilities.invokeLater(new Runnable() {
                //    @Override public void run() {
                //        drbdGraph.getDrbdInfo().getInfoPanel();
                //    }
                //});
                Tools.stopProgressIndicator(clusterName,
                    Tools.getString("ClusterBrowser.DrbdUpdate"));
                cluster.getBrowser().startConnectionStatus();
                cluster.getBrowser().startServerStatus();
                cluster.getBrowser().startDrbdStatus();
                cluster.getBrowser().startClStatus();
            }
        };
        final Thread thread = new Thread(runnable);
        //thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /**
     * Starts polling of the server status on all hosts, for all the stuff
     * that can change on the server on the fly, like for example the block
     * devices.
     */
    void startServerStatus() {
        final Host[] hosts = cluster.getHostsArray();
        for (final Host host : hosts) {
            final Thread thread = new Thread(new Runnable() {
                @Override public void run() {
                    startServerStatus(host);
                }
            });
            thread.start();
        }
    }

    /** Starts polling of the server status on one host. */
    void startServerStatus(final Host host) {
        final String hostName = host.getName();
        final CategoryInfo[] infosToUpdate =
                                        new CategoryInfo[]{clusterHostsInfo};
        long count = 0;
        while (true) {
            if (host.isServerStatusLatch()) {
                Tools.startProgressIndicator(
                         hostName,
                         Tools.getString("ClusterBrowser.UpdatingServerInfo"));
            }

            host.setIsLoading();
            boolean changed = false;
            if (host.isServerStatusLatch() || count % 5 == 0) {
                host.getHWInfo(infosToUpdate,
                               new ResourceGraph[]{drbdGraph, heartbeatGraph});
            } else {
                host.getHWInfoLazy(infosToUpdate,
                                   new ResourceGraph[]{drbdGraph,
                                                       heartbeatGraph});
            }
            drbdGraph.addHost(host.getBrowser().getHostDrbdInfo());
            updateDrbdResources();
            if (host.isServerStatusLatch()) {
                Tools.stopProgressIndicator(
                        hostName,
                        Tools.getString("ClusterBrowser.UpdatingServerInfo"));
                Tools.startProgressIndicator(
                        hostName,
                        Tools.getString("ClusterBrowser.UpdatingVMsStatus"));
            }
            periodicalVMSUpdate(host);
            if (host.isServerStatusLatch()) {
                Tools.stopProgressIndicator(
                        hostName,
                        Tools.getString("ClusterBrowser.UpdatingVMsStatus"));

            }
            SwingUtilities.invokeLater(
                new Runnable() {
                    @Override public void run() {
                        drbdGraph.scale(); // TODO: ?
                    }
                });
            host.serverStatusLatchDone();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            count++;
            if (!serverStatus) {
                break;
            }
        }
    }

    /** Updates VMs info. */
    public void periodicalVMSUpdate(final Host host) {
        final VMSXML newVMSXML = new VMSXML(host);
        if (newVMSXML.update()) {
            try {
                mVMSLock.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            vmsXML.put(host, newVMSXML);
            mVMSLock.release();
            updateVMS();
        }
    }

    /** Adds new vmsxml object to the hash. */
    public void vmsXMLPut(final Host host, final VMSXML newVMSXML) {
        try {
            mVMSLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        vmsXML.put(host, newVMSXML);
        mVMSLock.release();
    }

    /** Cancels the server status. */
    public void cancelServerStatus() {
        serverStatus = false;
    }

    /** Starts connection status on all hosts. */
    void startConnectionStatus() {
        final Host[] hosts = cluster.getHostsArray();
        for (final Host host : hosts) {
            final Thread thread = new Thread(new Runnable() {
                @Override public void run() {
                    startConnectionStatus(host);
                }
            });
            thread.start();
        }
    }

    /** Starts drbd status on all hosts. */
    void startDrbdStatus() {
        final Host[] hosts = cluster.getHostsArray();
        for (final Host host : hosts) {
            final Thread thread = new Thread(new Runnable() {
                @Override public void run() {
                    startDrbdStatus(host);
                }
            });
            thread.start();
        }
    }

    /** Starts drbd status. */
    public void stopDrbdStatus() {
        drbdStatusCanceled = true;
        final Host[] hosts = cluster.getHostsArray();
        for (Host host : hosts) {
            host.stopDrbdStatus();
        }
        for (Host host : hosts) {
            host.waitOnDrbdStatus();
        }
    }

    /** Starts connection status. */
    void startConnectionStatus(final Host host) {
        final Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                while (true) {
                    host.execConnectionStatusCommand(
                          new ExecCallback() {
                               @Override public void done(final String ans) {
                                   Tools.debug(this, "connection ok on "
                                                     + host.getName(),
                                               2);
                                   host.setConnected();
                               }

                               @Override public void doneError(
                                                         final String ans,
                                                         final int exitCode) {
                                   Tools.debug(this, "connection lost on "
                                                     + host.getName()
                                                     + "exit code: "
                                                     + exitCode,
                                               2);
                                   host.getSSH().forceReconnect();
                                   host.setConnected();
                               }
                           });
                    Tools.sleep(10000);
                }
            }
        });
        thread.start();
    }

    /** Starts drbd status on host. */
    void startDrbdStatus(final Host host) {
        final CountDownLatch firstTime = new CountDownLatch(1);
        host.setDrbdStatus(false);
        final String hostName = host.getName();
        /* now what we do if the status finished for the first time. */
        Tools.startProgressIndicator(
                        hostName,
                        Tools.getString("ClusterBrowser.UpdatingDrbdStatus"));
        final Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    firstTime.await();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        drbdGraph.scale();
                        drbdGraph.getDrbdInfo().selectMyself();
                    }
                });
                Tools.stopProgressIndicator(
                         hostName,
                         Tools.getString("ClusterBrowser.UpdatingDrbdStatus"));
            }
        });
        thread.start();

        drbdStatusCanceled = false;
        while (true) {
            host.execDrbdStatusCommand(
                  new ExecCallback() {
                       @Override public void done(final String ans) {
                           if (!host.isDrbdStatus()) {
                               host.setDrbdStatus(true);
                               drbdGraph.repaint();
                               Tools.debug(this, "drbd status update: "
                                                     + host.getName(), 1);
                               clusterHostsInfo.updateTable(
                                                ClusterHostsInfo.MAIN_TABLE);
                           }
                       }

                       @Override public void doneError(final String ans,
                                                       final int exitCode) {
                           Tools.debug(this, "drbd status failed: "
                                             + host.getName()
                                             + " exit code: "
                                             + exitCode,
                                       1);
                           if (exitCode != 143 && exitCode != 100) {
                               // TODO: exit code is null -> 100 all of the
                               // sudden
                               /* was killed intentionally */
                               if (host.isDrbdStatus()) {
                                   host.setDrbdStatus(false);
                                   Tools.debug(this, "drbd status update: "
                                                     + host.getName(), 1);
                                   drbdGraph.repaint();
                                   clusterHostsInfo.updateTable(
                                                ClusterHostsInfo.MAIN_TABLE);
                               }
                               if (exitCode == 255) {
                                   /* looks like connection was lost */
                                   //host.getSSH().forceReconnect();
                                   //host.setConnected();
                               }
                           }
                           //TODO: repaint ok?
                           //repaintSplitPane();
                           //drbdGraph.updatePopupMenus();
                           //drbdGraph.repaint();
                       }
                   },

                   new NewOutputCallback() {
                       @Override public void output(final String output) {
                           drbdStatusLock();
                           firstTime.countDown();
                           boolean updated = false;
                           if ("nm".equals(output.trim())) {
                               if (host.isDrbdStatus()) {
                                   Tools.debug(this, "drbd status update: "
                                                 + host.getName(), 1);
                                   host.setDrbdStatus(false);
                                   drbdGraph.repaint();
                                   clusterHostsInfo.updateTable(
                                                ClusterHostsInfo.MAIN_TABLE);
                               }
                               drbdStatusUnlock();
                               return;
                           } else {
                               if (!host.isDrbdStatus()) {
                                   Tools.debug(this, "drbd status update: "
                                                 + host.getName(), 1);
                                   host.setDrbdStatus(true);
                                   drbdGraph.repaint();
                                   clusterHostsInfo.updateTable(
                                                ClusterHostsInfo.MAIN_TABLE);
                               }
                           }
                           final String[] lines = output.split("\n");
                           final DrbdXML newDrbdXML =
                                          new DrbdXML(cluster.getHostsArray());

                           final String configString =
                                                   newDrbdXML.getConfig(host);
                           boolean configUpdated = false;
                           if (!Tools.areEqual(configString,
                                           oldDrbdConfigString.get(host))) {
                               oldDrbdConfigString.put(host, configString);
                               configUpdated = true;
                           }
                           newDrbdXML.update(configString);
                           for (final String line : lines) {
                               if (newDrbdXML.parseDrbdEvent(host.getName(),
                                                             drbdGraph,
                                                             line)) {
                                   updated = true;
                                   host.setDrbdStatus(true);
                               }
                           }
                           if (updated || configUpdated) {
                               drbdXML = newDrbdXML;
                               getDrbdGraph().getDrbdInfo().setParameters();
                               drbdGraph.repaint();
                               Tools.debug(this, "drbd status update: "
                                             + host.getName(), 1);
                               clusterHostsInfo.updateTable(
                                                ClusterHostsInfo.MAIN_TABLE);
                               final Thread thread = new Thread(
                                   new Runnable() {
                                       @Override public void run() {
                                           repaintSplitPane();
                                           drbdGraph.updatePopupMenus();
                                           SwingUtilities.invokeLater(
                                               new Runnable() {
                                                   @Override public void run() {
                                                       repaintTree();
                                                   }
                                               }
                                           );
                                       }
                                   });
                               thread.start();
                           }
                           drbdStatusUnlock();
                       }
                   });
            firstTime.countDown();
            while (!host.isConnected() || !host.isDrbdLoaded()) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            host.waitOnDrbdStatus();
            if (drbdStatusCanceled) {
                break;
            }
        }
    }

    /** Stops hb status. */
    public void stopClStatus() {
        clStatusCanceled = true;
        final Host[] hosts = cluster.getHostsArray();
        for (Host host : hosts) {
            host.stopClStatus();
        }
    }


    /** Returns true if hb status on all hosts failed. */
    public boolean clStatusFailed() {
        final Host[] hosts = cluster.getHostsArray();
        for (Host host : hosts) {
            if (host.isClStatus()) {
                return false;
            }
        }
        return true;
    }

    /** Sets hb status (failed / not failed for every node). */
    void setClStatus() {
        final Host[] hosts = cluster.getHostsArray();
        for (Host host : hosts) {
            final String online = clusterStatus.isOnlineNode(host.getName());
            if ("yes".equals(online)) {
                setClStatus(host, true);
            } else {
                setClStatus(host, false);
            }
        }
    }

    /** Starts hb status progress indicator. */
    void startClStatusProgressIndicator(final String clusterName) {
        Tools.startProgressIndicator(
                            clusterName,
                            Tools.getString("ClusterBrowser.HbUpdateStatus"));
    }

    /** Stops hb status progress indicator. */
    void stopClStatusProgressIndicator(final String clusterName) {
        Tools.stopProgressIndicator(
                            clusterName,
                            Tools.getString("ClusterBrowser.HbUpdateStatus"));
    }

    /** Sets status and checks if it changes and if it does some action will be
     * performed. */
    private void setClStatus(final Host host, final boolean status) {
        final boolean oldStatus = host.isClStatus();
        host.setClStatus(status);
        if (oldStatus != status) {
            nodeChanged(servicesNode);
        }
    }

    /** Process output from cluster. */
    private void processClusterOutput(final String output,
                                      final StringBuffer clusterStatusOutput,
                                      final Host host,
                                      final CountDownLatch firstTime,
                                      final boolean testOnly) {
        clStatusLock();
        if (clStatusCanceled) {
            clStatusUnlock();
            firstTime.countDown();
            return;
        }
        if (output == null) {
            clusterStatus.setOnlineNode(host.getName(), "no");
            setClStatus(host, false);
            firstTime.countDown();
        } else {
            // TODO: if we get ERROR:... show it somewhere
            clusterStatusOutput.append(output);
            if (clusterStatusOutput.length() > 12) {
                final String e = clusterStatusOutput.substring(
                                           clusterStatusOutput.length() - 12);
                if (e.trim().equals("---done---")) {
                    final int i =
                                clusterStatusOutput.lastIndexOf("---start---");
                    if (i >= 0) {
                        if (clusterStatusOutput.indexOf("is stopped") >= 0) {
                            /* TODO: heartbeat's not running. */
                        } else {
                            final String status =
                                              clusterStatusOutput.substring(i);
                            clusterStatusOutput.delete(
                                                 0,
                                                 clusterStatusOutput.length());
                            if (CLUSTER_STATUS_ERROR.equals(status)) {
                                final boolean oldStatus = host.isClStatus();
                                clusterStatus.setOnlineNode(host.getName(),
                                                            "no");
                                setClStatus(host, false);
                                if (oldStatus) {
                                   heartbeatGraph.repaint();
                                }
                            } else {
                                if (clusterStatus.parseStatus(status)) {
                                    Tools.debug(this,
                                                "update cluster status: "
                                                + host.getName(), 1);
                                    final ServicesInfo ssi = servicesInfo;
                                    rscDefaultsInfo.setParameters(
                                      clusterStatus.getRscDefaultsValuePairs());
                                    ssi.setGlobalConfig();
                                    ssi.setAllResources(testOnly);
                                    if (firstTime.getCount() == 1) {
                                        /* one more time so that id-refs work.*/
                                        ssi.setAllResources(testOnly);
                                    }
                                    repaintTree();
                                    clusterHostsInfo.updateTable(
                                                ClusterHostsInfo.MAIN_TABLE);
                                }
                                final String online =
                                    clusterStatus.isOnlineNode(host.getName());
                                if ("yes".equals(online)) {
                                    setClStatus(host, true);
                                    setClStatus();
                                } else {
                                    setClStatus(host, false);
                                }
                            }
                        }
                        firstTime.countDown();
                    }
                }
            }
        }
        clStatusUnlock();
    }

    /** Starts hb status. */
    void startClStatus() {
        final CountDownLatch firstTime = new CountDownLatch(1);
        final String clusterName = getCluster().getName();
        startClStatusProgressIndicator(clusterName);
        final boolean testOnly = false;
        final Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    firstTime.await();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                if (clStatusFailed()) {
                     Tools.progressIndicatorFailed(clusterName,
                                                   "Cluster status failed");
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                           heartbeatGraph.scale();
                       }
                    });
                }
                stopClStatusProgressIndicator(clusterName);
            }
        });
        thread.start();
        clStatusCanceled = false;
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
            final String hostName = host.getName();
            //clStatusCanceled = false;
            host.execClStatusCommand(
                 new ExecCallback() {
                     @Override public void done(final String ans) {
                         final String online =
                                    clusterStatus.isOnlineNode(host.getName());
                         setClStatus(host, "yes".equals(online));
                         firstTime.countDown();
                     }

                     @Override public void doneError(final String ans,
                                                     final int exitCode) {
                         if (firstTime.getCount() == 1) {
                             Tools.debug(this, "hb status failed: "
                                           + host.getName()
                                           + ", ec: "
                                           + exitCode, 2);
                         }
                         clStatusLock();
                         clusterStatus.setOnlineNode(host.getName(), "no");
                         setClStatus(host, false);
                         clusterStatus.setDC(null);
                         clStatusUnlock();
                         if (exitCode == 255) {
                             /* looks like connection was lost */
                             //heartbeatGraph.repaint();
                             //host.getSSH().forceReconnect();
                             //host.setConnected();
                         }
                         firstTime.countDown();
                     }
                 },

                 new NewOutputCallback() {
                     //TODO: check this buffer's size
                     private StringBuffer clusterStatusOutput =
                                                        new StringBuffer(300);
                     @Override public void output(final String output) {
                         processClusterOutput(output,
                                              clusterStatusOutput,
                                              host,
                                              firstTime,
                                              testOnly);
                     }
                 });
            host.waitOnClStatus();
            if (clStatusCanceled) {
                break;
            }
            final boolean hbSt = clStatusFailed();
            if (hbSt) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /** Returns 'add service' list for menus. */
    public List<ResourceAgent> globalGetAddServiceList(final String cl) {
        return crmXML.getServices(cl);
    }

    /** Updates common block devices. */
    public void updateCommonBlockDevices() {
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
                                          cluster.getHostBlockDevices(device),
                                          this));
                setNode(resource);
                commonBlockDevicesNode.add(resource);
            }
            reload(commonBlockDevicesNode, false);
        }
    }

    /** Updates available services. */
    private void updateAvailableServices() {
        DefaultMutableTreeNode resource;
        Tools.debug(this, "update available services");
        availableServicesNode.removeAllChildren();
        for (final String cl : HB_CLASSES) {
            final ResourceAgentClassInfo raci =
                                          new ResourceAgentClassInfo(cl, this);
            classInfoMap.put(cl, raci);
            final DefaultMutableTreeNode classNode =
                    new DefaultMutableTreeNode(raci);
            for (final ResourceAgent ra : crmXML.getServices(cl)) {
                final AvailableServiceInfo asi =
                                            new AvailableServiceInfo(ra, this);
                availableServiceMap.put(ra, asi);
                resource = new DefaultMutableTreeNode(asi);
                setNode(resource);
                classNode.add(resource);
            }
            setNode(classNode);
            availableServicesNode.add(classNode);
        }
    }

    /** Updates VM nodes. */
    void updateVMS() {
        try {
            if (!mUpdateVMSlock.attempt(0)) {
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Tools.debug(this, "VM status update", 1);
        final Set<String> domainNames = new TreeSet<String>();
        for (final Host host : getClusterHosts()) {
            final VMSXML vxml = getVMSXML(host);
            if (vxml != null) {
                domainNames.addAll(vxml.getDomainNames());
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        boolean nodeChanged = false;
        final List<VMSVirtualDomainInfo> currentVMSVDIs =
                                        new ArrayList<VMSVirtualDomainInfo>();

        if (vmsNode != null) {
            final Enumeration ee = vmsNode.children();
            while (ee.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) ee.nextElement();
                final VMSVirtualDomainInfo vmsvdi =
                                  (VMSVirtualDomainInfo) node.getUserObject();
                if (domainNames.contains(vmsvdi.toString())) {
                    /* keeping */
                    currentVMSVDIs.add(vmsvdi);
                    domainNames.remove(vmsvdi.toString());
                    vmsvdi.updateParameters(); /* update old */
                } else {
                    /* remove not existing vms */
                    nodesToRemove.add(node);
                    nodeChanged = true;
                }
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        if (vmsNode == null) {
            mUpdateVMSlock.release();
            return;
        }
        for (final String domainName : domainNames) {
            final Enumeration e = vmsNode.children();
            int i = 0;
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                     (DefaultMutableTreeNode) e.nextElement();
                final VMSVirtualDomainInfo vmsvdi =
                                  (VMSVirtualDomainInfo) node.getUserObject();
                if (domainName.compareTo(vmsvdi.getName()) < 0) {
                    break;
                }
                i++;
            }
            /* add new vms nodes */
            final VMSVirtualDomainInfo vmsvdi =
                                   new VMSVirtualDomainInfo(domainName, this);
            currentVMSVDIs.add(vmsvdi);
            final DefaultMutableTreeNode resource =
                                            new DefaultMutableTreeNode(vmsvdi);
            setNode(resource);
            vmsvdi.updateParameters();
            final int index = i;
            Tools.invokeAndWait(new Runnable() {
                public void run() {
                    vmsNode.insert(resource, index);
                }
            });
            nodeChanged = true;
        }
        if (nodeChanged) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    reload(vmsNode, false);
                }
            });
        }
        for (final ServiceInfo si : getExistingServiceList(null)) {
            final VMSVirtualDomainInfo vmsvdi = si.connectWithVMS();
            if (vmsvdi != null) {
                /* keep the not connected ones.*/
                currentVMSVDIs.remove(vmsvdi);
            }
        }
        for (final VMSVirtualDomainInfo vmsvdi : currentVMSVDIs) {
            vmsvdi.setUsedByCRM(false);
        }
        final VMSInfo vmsi = (VMSInfo) vmsNode.getUserObject();
        if (vmsi != null) {
            vmsi.updateTable(VMSInfo.MAIN_TABLE);
        }
        mUpdateVMSlock.release();
    }

    /** Returns vmsinfo object. */
    public VMSInfo getVMSInfo() {
        return (VMSInfo) vmsNode.getUserObject();
    }

    /** Updates drbd resources. */
    private void updateDrbdResources() {
        final DrbdXML dxml = drbdXML;
        final String[] drbdResources = dxml.getResources();
        final boolean testOnly = false;
        final DrbdInfo drbdInfo = drbdGraph.getDrbdInfo();
        boolean atLeastOneAdded = false;
        for (int i = 0; i < drbdResources.length; i++) {
            final String resName = drbdResources[i];
            final String drbdDev = dxml.getDrbdDevice(resName);
            final Map<String, String> hostDiskMap =
                                                dxml.getHostDiskMap(resName);
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
                    if (getDrbdDevHash().containsKey(disk)) {
                        /* TODO: ignoring stacked device */
                        putDrbdDevHash();
                        continue;
                    } else {
                        putDrbdDevHash();
                        Tools.appWarning("could not find disk: " + disk
                                         + " on host: " + hostName);
                        continue;
                    }
                }
                bdi.setParameters(resName);
                if (bd1 == null) {
                    bd1 = bdi;
                } else {
                    bd2 = bdi;
                }
            }
            if (bd1 != null
                && bd2 != null) {
                final boolean added = drbdInfo.addDrbdResource(resName,
                                                               drbdDev,
                                                               bd1,
                                                               bd2,
                                                               false,
                                                               testOnly);
                if (added) {
                    atLeastOneAdded = true;
                } else {
                    bd1.getDrbdResourceInfo().setParameters();
                }
            }
        }
        if (atLeastOneAdded) {
            drbdInfo.getInfoPanel();
            Tools.invokeAndWait(new Runnable() {
                @Override public void run() {
                    drbdInfo.setAllApplyButtons();
                }
            });
        }
    }

    /** Updates networks. */
    private void updateNetworks() {
        if (networksNode != null) {
            DefaultMutableTreeNode resource;
            final Network[] networks = cluster.getCommonNetworks();
            networksNode.removeAllChildren();
            for (int i = 0; i < networks.length; i++) {
                resource = new DefaultMutableTreeNode(
                                    new NetworkInfo(networks[i].getName(),
                                                    networks[i],
                                                    this));
                setNode(resource);
                networksNode.add(resource);
            }
            reload(networksNode, false);
        }
    }

    /**
     * Returns first host. Used for heartbeat commands, that can be
     * executed on any host.
     * It changes terminal panel to this host.
     */
    Host getFirstHost() {
        /* TODO: if none of the hosts is connected the null causes error during
         * loading. */
        final Host[] hosts = getClusterHosts();
        for (final Host host : hosts) {
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

    /** Returns whether the host is in stand by. */
    public boolean isStandby(final Host host, final boolean testOnly) {
        // TODO: make it more efficient
        final ClusterStatus cl = clusterStatus;
        if (cl == null) {
            return false;
        }
        final String standby = cl.getNodeParameter(
                                       host.getName().toLowerCase(Locale.US),
                                       "standby",
                                       testOnly);
        return "on".equals(standby) || "true".equals(standby);
    }

    /** Returns whether the host is the real dc host as reported by dc. */
    boolean isRealDcHost(final Host host) {
        return host.equals(realDcHost);
    }

    /**
     * Finds and returns DC host.
     * TODO: document what's going on.
     */
    public Host getDCHost() {
        Host dcHost = null;
        String dc = null;
        final ClusterStatus cl = clusterStatus;
        if (cl != null) {
            dc = cl.getDC();
        }
        final List<Host> hosts = new ArrayList<Host>();
        int lastHostIndex = 0;
        int i = 0;
        for (Host host : getClusterHosts()) {
            if (host == lastDcHost) {
                lastHostIndex = i;
            }
            if (host.getName().equals(dc)
                && host.isClStatus()
                && !host.isCommLayerStarting()
                && !host.isCommLayerStopping()
                && (host.isHeartbeatRunning()
                    || host.isCsRunning()
                    || host.isAisRunning())) {
                dcHost = host;
                break;
            }
            hosts.add(host);

            i++;
        }
        if (dcHost == null) {
            int ix = lastHostIndex;
            do {
                ix++;
                if (ix > hosts.size() - 1) {
                    ix = 0;
                }
                if (hosts.get(ix).isConnected()
                    && (hosts.get(ix).isHeartbeatRunning()
                        || hosts.get(ix).isCsRunning()
                        || hosts.get(ix).isAisRunning())) {
                    lastDcHost = hosts.get(ix);
                    break;
                }
            } while (ix != lastHostIndex);
            dcHost = lastDcHost;
            realDcHost = null;
            if (dcHost == null) {
                dcHost = hosts.get(0);
            }
        } else {
            realDcHost = dcHost;
        }

        lastDcHost = dcHost;
        return dcHost;
    }

    /** clStatusLock global lock. */
    public void clStatusLock() {
        try {
            mClStatusLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** clStatusLock global unlock. */
    public void clStatusUnlock() {
        mClStatusLock.release();
    }

    /** drbdStatusLock global lock. */
    public void drbdStatusLock() {
        try {
            mDRBDStatusLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** drbdStatusLock global unlock. */
    public void drbdStatusUnlock() {
        mDRBDStatusLock.release();
    }

    /** Highlights drbd node. */
    void selectDrbd() {
        reload(drbdNode, true);
    }

    /** Highlights services. */
    void selectServices() {
        // this fires treeStructureChanged in ViewPanel.
        //nodeChanged(servicesNode);
        if (getClusterViewPanel().isDisabledDuringLoad()) {
            return;
        }
        selectPath(new Object[]{getTreeTop(), heartbeatNode, servicesNode});
    }

    /** Returns ServiceInfo object from crm id. */
    public ServiceInfo getServiceInfoFromCRMId(final String crmId) {
        try {
            mHeartbeatIdToService.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final ServiceInfo si = heartbeatIdToServiceInfo.get(crmId);
        mHeartbeatIdToService.release();
        return si;
    }

    /** Locks heartbeatIdToServiceInfo hash. */
    public void mHeartbeatIdToServiceLock() {
        try {
            mHeartbeatIdToService.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** Unlocks heartbeatIdToServiceInfo hash. */
    public void mHeartbeatIdToServiceUnlock() {
        mHeartbeatIdToService.release();
    }

    /** Returns heartbeatIdToServiceInfo hash. You have to lock it. */
    public Map<String, ServiceInfo> getHeartbeatIdToServiceInfo() {
        return heartbeatIdToServiceInfo;
    }

    /** Returns ServiceInfo object identified by name and id. */
    public ServiceInfo getServiceInfoFromId(final String name,
                                            final String id) {
        try {
            mNameToServiceLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final Map<String, ServiceInfo> idToInfoHash =
                                               nameToServiceInfoHash.get(name);
        if (idToInfoHash == null) {
            mNameToServiceLock.release();
            return null;
        }
        final ServiceInfo si = idToInfoHash.get(id);
        mNameToServiceLock.release();
        return si;
    }

    /** Returns the name, id to service info hash. */
    public Map<String, Map<String, ServiceInfo>> getNameToServiceInfoHash() {
        return nameToServiceInfoHash;
    }

    /** Locks the nameToServiceInfoHash. */
    public void lockNameToServiceInfo() {
        try {
            mNameToServiceLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Unlocks the nameToServiceInfoHash. */
    public void unlockNameToServiceInfo() {
        mNameToServiceLock.release();
    }

    /** Returns 'existing service' list for graph popup menu. */
    public List<ServiceInfo> getExistingServiceList(final ServiceInfo p) {
        final List<ServiceInfo> existingServiceList =
                                                  new ArrayList<ServiceInfo>();
        try {
            mNameToServiceLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final String name : nameToServiceInfoHash.keySet()) {
            final Map<String, ServiceInfo> idHash =
                                            nameToServiceInfoHash.get(name);
            for (final String id : idHash.keySet()) {
                final ServiceInfo si = idHash.get(id);
                if (si.getService().isOrphaned()) {
                    continue;
                }
                final GroupInfo gi = si.getGroupInfo();
                ServiceInfo sigi = si;
                if (gi != null) {
                    sigi = gi;
                    // TODO: it does not work here
                }
                if (p == null
                    || !getHeartbeatGraph().existsInThePath(sigi, p)) {
                    existingServiceList.add(si);
                }
            }
        }
        mNameToServiceLock.release();
        return existingServiceList;
    }


    /**
     * Removes ServiceInfo from the ServiceInfo hash.
     *
     * @param serviceInfo
     *              service info object
     */
    public void removeFromServiceInfoHash(final ServiceInfo serviceInfo) {
        // TODO: it comes here twice sometimes
        final Service service = serviceInfo.getService();
        try {
            mNameToServiceLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final Map<String, ServiceInfo> idToInfoHash =
                                 nameToServiceInfoHash.get(service.getName());
        if (idToInfoHash != null) {
            idToInfoHash.remove(service.getId());
        }
        mNameToServiceLock.release();
    }

    /** Returns nameToServiceInfoHash for the specified service.
     *  You must lock it when you use it. */
    public Map<String, ServiceInfo> getNameToServiceInfoHash(
                                                        final String name) {
        return nameToServiceInfoHash.get(name);
    }

    /**
     * Adds heartbeat id from service to the list. If service does not have an
     * id it is generated.
     */
    public void addToHeartbeatIdList(final ServiceInfo si) {
        final String id = si.getService().getId();
        String pmId = si.getService().getHeartbeatId();
        if (pmId == null) {
            if (ConfigData.PM_GROUP_NAME.equals(si.getService().getName())) {
                pmId = Service.GRP_ID_PREFIX;
            } else if (ConfigData.PM_CLONE_SET_NAME.equals(
                                                    si.getService().getName())
                       || ConfigData.PM_MASTER_SLAVE_SET_NAME.equals(
                                                si.getService().getName())) {
                if (si.getService().isMaster()) {
                    pmId = Service.MS_ID_PREFIX;
                } else {
                    pmId = Service.CL_ID_PREFIX;
                }
            } else if (si.getResourceAgent().isStonith())  {
                pmId = Service.STONITH_ID_PREFIX
                       + si.getService().getName()
                       + "_";
            } else {
                pmId = Service.RES_ID_PREFIX + si.getService().getName() + "_";
            }
            String newPmId;
            if (id == null) {
                /* first time, no pm id is set */
                newPmId = pmId + "1";
                si.getService().setId("1");
            } else {
                newPmId = pmId + id;
                si.getService().setHeartbeatId(newPmId);
            }
            try {
                mHeartbeatIdToService.acquire();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            heartbeatIdToServiceInfo.put(newPmId, si);
            mHeartbeatIdToService.release();
        } else {
            try {
                mHeartbeatIdToService.acquire();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            if (heartbeatIdToServiceInfo.get(pmId) == null) {
                heartbeatIdToServiceInfo.put(pmId, si);
            }
            mHeartbeatIdToService.release();
        }
    }

    /**
     * Deletes caches of all Filesystem infoPanels.
     * This is usefull if something have changed.
     */
    public void resetFilesystems() {
        try {
            mHeartbeatIdToService.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        for (String hbId : heartbeatIdToServiceInfo.keySet()) {
            final ServiceInfo si = heartbeatIdToServiceInfo.get(hbId);
            if (si.getName().equals("Filesystem")) {
                si.setInfoPanel(null);
            }
        }
        mHeartbeatIdToService.release();
    }

    /**
     * Adds ServiceInfo in the name to ServiceInfo hash. Id and name
     * are taken from serviceInfo object. nameToServiceInfoHash
     * contains a hash with id as a key and ServiceInfo as a value.
     */
    public void addNameToServiceInfoHash(final ServiceInfo serviceInfo) {
        /* add to the hash with service name and id as keys */
        final Service service = serviceInfo.getService();
        try {
            mNameToServiceLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Map<String, ServiceInfo> idToInfoHash =
                                nameToServiceInfoHash.get(service.getName());
        String csPmId = null;
        final ServiceInfo cs = serviceInfo.getContainedService();
        if (cs != null) {
            csPmId = cs.getService().getName() + "_" + cs.getService().getId();
        }
        if (idToInfoHash == null) {
            idToInfoHash = new TreeMap<String, ServiceInfo>(
                                                String.CASE_INSENSITIVE_ORDER);
            if (service.getId() == null) {
                if (csPmId == null) {
                    service.setId("1");
                } else {
                    service.setIdAndCrmId(csPmId);
                }
            }
        } else {
            if (service.getId() == null) {
                final Iterator<String> it = idToInfoHash.keySet().iterator();
                int index = 0;
                while (it.hasNext()) {
                    final String id =
                      idToInfoHash.get(it.next()).getService().getId();
                    Pattern p;
                    if (csPmId == null) {
                        p = Pattern.compile("^(\\d+)$");
                    } else {
                        p = Pattern.compile("^" + csPmId + "_(\\d+)$");
                        if (csPmId.equals(id)) {
                            index++;
                        }
                    }

                    final Matcher m = p.matcher(id);
                    if (m.matches()) {
                        try {
                            final int i = Integer.parseInt(m.group(1));
                            if (i > index) {
                                index = i;
                            }
                        } catch (NumberFormatException nfe) {
                            /* not a number */
                        }
                    }
                }
                if (csPmId == null) {
                    service.setId(Integer.toString(index + 1));
                } else {
                    if (index == 0) {
                        service.setIdAndCrmId(csPmId);
                    } else {
                        service.setIdAndCrmId(csPmId
                                      + "_"
                                      + Integer.toString(index + 1));
                    }
                }
            }
        }
        idToInfoHash.put(service.getId(), serviceInfo);
        nameToServiceInfoHash.put(service.getName(), idToInfoHash);
        mNameToServiceLock.release();
    }

    /**
     * Returns true if user wants the heartbeat:drbd, which is not recommended.
     */
    public boolean hbDrbdConfirmDialog() {
        final Host dcHost = getDCHost();
        final String hbV = dcHost.getHeartbeatVersion();
        final String pmV = dcHost.getPacemakerVersion();
        return Tools.confirmDialog(
           Tools.getString("ClusterBrowser.confirmHbDrbd.Title"),
           Tools.getString("ClusterBrowser.confirmHbDrbd.Description"),
           Tools.getString("ClusterBrowser.confirmHbDrbd.Yes"),
           Tools.getString("ClusterBrowser.confirmHbDrbd.No"));
    }

    /** Returns whether drbddisk RA is preferred. */
    public boolean isDrbddiskPreferred() {
        final Host dcHost = getDCHost();
        final String hbV = dcHost.getHeartbeatVersion();
        final String pmV = dcHost.getPacemakerVersion();
        return (pmV == null
                && hbV != null
                && Tools.compareVersions(hbV, "2.1.4") <= 0);
    }

    /**
     * Returns true if user wants the linbit:drbd even, for old version of
     * hb or simply true if we have pacemaker.
     */
    public boolean linbitDrbdConfirmDialog() {
        if (isDrbddiskPreferred()) {
            final String desc =
                Tools.getString("ClusterBrowser.confirmLinbitDrbd.Description");

            final Host dcHost = getDCHost();
            final String hbV = dcHost.getHeartbeatVersion();
            return Tools.confirmDialog(
               Tools.getString("ClusterBrowser.confirmLinbitDrbd.Title"),
               desc.replaceAll("@VERSION@", hbV),
               Tools.getString("ClusterBrowser.confirmLinbitDrbd.Yes"),
               Tools.getString("ClusterBrowser.confirmLinbitDrbd.No"));
        }
        return true;
    }

    /** Starts heartbeats on all nodes. */
    void startHeartbeats() {
        final Host[] hosts = cluster.getHostsArray();
        for (Host host : hosts) {
            Heartbeat.startHeartbeat(host);
        }
    }

    /** Callback to service menu items, that show ptest results in tooltips. */
    public abstract class ClMenuItemCallback implements ButtonCallback {
        /** Menu component on which this callback works. */
        private final ComponentWithTest component;
        /** Host if over a menu item that belongs to a host. */
        private final Host menuHost;
        /** Whether the mouse is still over. */
        private volatile boolean mouseStillOver = false;

        /** Creates new ClMenuItemCallback object. */
        public ClMenuItemCallback(final ComponentWithTest component,
                                  final Host menuHost) {
            this.component = component;
            this.menuHost = menuHost;
        }

        /** Can be overwritten to disable the whole thing. */
        @Override public boolean isEnabled() {
            Host h;
            if (menuHost == null) {
                h = getDCHost();
            } else {
                h = menuHost;
            }
            final String hbV = h.getHeartbeatVersion();
            final String pmV = h.getPacemakerVersion();
            if (pmV == null
                && hbV != null
                && Tools.compareVersions(hbV, "2.1.4") <= 0) {
                return false;
            }
            return true;
        }

        /** Mouse out, stops animation. */
        @Override public final void mouseOut() {
            if (isEnabled()) {
                mouseStillOver = false;
                heartbeatGraph.stopTestAnimation((JComponent) component);
                component.setToolTipText(null);
            }
        }

        /** Mouse over, starts animation, calls action() and sets tooltip. */
        @Override public final void mouseOver() {
            if (isEnabled()) {
                mouseStillOver = true;
                component.setToolTipText(STARTING_PTEST_TOOLTIP);
                component.setToolTipBackground(
                  Tools.getDefaultColor(
                                   "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                heartbeatGraph.startTestAnimation((JComponent) component,
                                                  startTestLatch);
                ptestLockAcquire();
                clusterStatus.setPtestData(null);
                Host h;
                if (menuHost == null) {
                    h = getDCHost();
                } else {
                    h = menuHost;
                }
                action(h);
                final PtestData ptestData = new PtestData(CRM.getPtest(h));
                component.setToolTipText(ptestData.getToolTip());
                clusterStatus.setPtestData(ptestData);
                ptestLockRelease();
                startTestLatch.countDown();
            }
        }

        /** Action that is caried out on the host. */
        protected abstract void action(final Host dcHost);
    }

    /** Callback to service menu items, that show ptest results in tooltips. */
    public abstract class DRBDMenuItemCallback implements ButtonCallback {
        /** Menu component on which this callback works. */
        private final ComponentWithTest component;
        /** Host if over a menu item that belongs to a host. */
        private final Host menuHost;
        /** Whether the mouse is still over. */
        private volatile boolean mouseStillOver = false;

        /** Creates new DRBDMenuItemCallback object. */
        public DRBDMenuItemCallback(final ComponentWithTest component,
                                    final Host menuHost) {
            this.component = component;
            this.menuHost = menuHost;
        }

        /** Whether the whole thing should be enabled. */
        @Override public final boolean isEnabled() {
            return true;
        }

        /** Mouse out, stops animation. */
        @Override public final void mouseOut() {
            if (!isEnabled()) {
                return;
            }
            mouseStillOver = false;
            drbdGraph.stopTestAnimation((JComponent) component);
            component.setToolTipText(null);
        }

        /** Mouse over, starts animation, calls action() and sets tooltip. */
        @Override public final void mouseOver() {
            if (!isEnabled()) {
                return;
            }
            mouseStillOver = true;
            component.setToolTipText(
                          Tools.getString("ClusterBrowser.StartingDRBDtest"));
            component.setToolTipBackground(
              Tools.getDefaultColor("ClusterBrowser.Test.Tooltip.Background"));
            Tools.sleep(250);
            if (!mouseStillOver) {
                return;
            }
            mouseStillOver = false;
            final CountDownLatch startTestLatch = new CountDownLatch(1);
            drbdGraph.startTestAnimation((JComponent) component,
                                         startTestLatch);
            drbdtestLockAcquire();
            final Map<Host, String> testOutput =
                                             new LinkedHashMap<Host, String>();
            if (menuHost != null) {
                action(menuHost);
                testOutput.put(menuHost, DRBD.getDRBDtest());
            } else {
                for (final Host h : cluster.getHostsArray()) {
                    action(h);
                    testOutput.put(h, DRBD.getDRBDtest());
                }
            }
            final DRBDtestData dtd = new DRBDtestData(testOutput);
            component.setToolTipText(dtd.getToolTip());
            drbdtestdataLockAcquire();
            drbdtestData = dtd;
            drbdtestdataLockRelease();
            //clusterStatus.setPtestData(ptestData);
            drbdtestLockRelease();
            startTestLatch.countDown();
        }

        /** Action that is caried out on the host. */
        protected abstract void action(final Host dcHost);
    }

    /**
     * Returns common file systems on all nodes as StringInfo array.
     * The defaultValue is stored as the first item in the array.
     */
    public StringInfo[] getCommonFileSystems(final String defaultValue) {
        StringInfo[] cfs =  new StringInfo[commonFileSystems.length + 1];
        cfs[0] = new StringInfo(defaultValue, null, this);
        int i = 1;
        for (String cf : commonFileSystems) {
            cfs[i] = new StringInfo(cf, cf, this);
            i++;
        }
        return cfs;
    }
    /**
     * Returns nw hb connection info object. This is called from heartbeat
     * graph.
     */
    HbConnectionInfo getNewHbConnectionInfo() {
        final HbConnectionInfo hbci = new HbConnectionInfo(this);
        //hbci.getInfoPanel();
        return hbci;
    }

    /** Returns cluster status object. */
    public ClusterStatus getClusterStatus() {
        return clusterStatus;
    }

    /** Returns drbd test data. */
    public DRBDtestData getDRBDtestData() {
        drbdtestdataLockAcquire();
        final DRBDtestData dtd = drbdtestData;
        drbdtestdataLockRelease();
        return dtd;
    }

    /** Sets drbd test data. */
    public void setDRBDtestData(final DRBDtestData drbdtestData) {
        drbdtestdataLockAcquire();
        this.drbdtestData = drbdtestData;
        drbdtestdataLockRelease();
    }

    /** Acquire ptest lock. */
    public void ptestLockAcquire() {
        try {
            mPtestLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Release ptest lock. */
    public void ptestLockRelease() {
        mPtestLock.release();
    }

    /** Acquire drbd test data lock. */
    protected void drbdtestdataLockAcquire() {
        try {
            mDRBDtestdataLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Release drbd test data lock. */
    protected void drbdtestdataLockRelease() {
        mDRBDtestdataLock.release();
    }

    /** Returns xml from cluster manager. */
    public CRMXML getCRMXML() {
        return crmXML;
    }

    /** Returns xml from drbd. */
    public DrbdXML getDrbdXML() {
        return drbdXML;
    }

    /** Sets xml from drbd. */
    public void setDrbdXML(final DrbdXML drbdXML) {
        this.drbdXML = drbdXML;
    }

    /** Returns drbd node from the menu. */
    public DefaultMutableTreeNode getDrbdNode() {
        return drbdNode;
    }

    /** Returns common blockdevices node from the menu. */
    public DefaultMutableTreeNode getCommonBlockDevicesNode() {
        return commonBlockDevicesNode;
    }

    /** Returns cluster hosts node from the menu. */
    public DefaultMutableTreeNode getClusterHostsNode() {
        return clusterHostsNode;
    }

    /** Returns services node from the menu. */
    public DefaultMutableTreeNode getServicesNode() {
        return servicesNode;
    }

    /** Returns services node from the menu. */
    public DefaultMutableTreeNode getNetworksNode() {
        return networksNode;
    }

    /**
     * Returns a hash from drbd device to drbd resource info. putDrbdDevHash
     * must follow after you're done. */
    public Map<String, DrbdResourceInfo> getDrbdDevHash() {
        try {
            mDrbdDevHashLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        return drbdDevHash;
    }

    /** Unlock drbd dev hash. */
    public void putDrbdDevHash() {
        mDrbdDevHashLock.release();
    }

    /**
     * Returns a hash from resource name to drbd resource info hash.
     * Get locks the hash and put unlocks it
     */
    public Map<String, DrbdResourceInfo> getDrbdResHash() {
        try {
            mDrbdResHashLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        return drbdResHash;
    }

    /** Done using drbdResHash. */
    public void putDrbdResHash() {
        mDrbdResHashLock.release();
    }

    /** Returns (shallow) copy of all drbdresource info objects. */
    public List<DrbdResourceInfo> getDrbdResHashValues() {
        final List<DrbdResourceInfo> values =
                   new ArrayList<DrbdResourceInfo>(getDrbdResHash().values());
        putDrbdResHash();
        return values;
    }

    /** Reloads all combo boxes that need to be reloaded. */
    public void reloadAllComboBoxes(final ServiceInfo exceptThisOne) {
        try {
            mNameToServiceLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final String name : nameToServiceInfoHash.keySet()) {
            final Map<String, ServiceInfo> idToInfoHash =
                                             nameToServiceInfoHash.get(name);
            for (final String id : idToInfoHash.keySet()) {
                final ServiceInfo si = idToInfoHash.get(id);
                if (si != exceptThisOne) {
                    si.reloadComboBoxes();
                }
            }
        }
        mNameToServiceLock.release();
    }

    /** Returns object that holds data of all VMs. */
    public VMSXML getVMSXML(final Host host) {
        try {
            mVMSLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final VMSXML vxml = vmsXML.get(host);
        mVMSLock.release();
        return vxml;
    }

    /**
     * Finds VMSVirtualDomainInfo object that contains the VM specified by
     * name.
     */
    public VMSVirtualDomainInfo findVMSVirtualDomainInfo(final String name) {
        if (vmsNode != null && name != null) {
            final Enumeration e = vmsNode.children();
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) e.nextElement();
                final VMSVirtualDomainInfo vmsvdi =
                                  (VMSVirtualDomainInfo) node.getUserObject();
                if (name.equals(vmsvdi.getName())) {
                    return vmsvdi;
                }
            }
        }
        return null;
    }

    /** Returns map to ResourceAgentClassInfo. */
    public ResourceAgentClassInfo getClassInfoMap(final String cl) {
        return classInfoMap.get(cl);
    }

    /** Returns map to AvailableServiceInfo. */
    public AvailableServiceInfo getAvailableServiceInfoMap(
                                                    final ResourceAgent ra) {
        return availableServiceMap.get(ra);
    }

    /** Returns available services info object. */
    public AvailableServicesInfo getAvailableServicesInfo() {
        return (AvailableServicesInfo) availableServicesNode.getUserObject();
    }

    /** Returns the services info object. */
    public ServicesInfo getServicesInfo() {
        return servicesInfo;
    }

    /** Returns rsc defaults info object. */
    public RscDefaultsInfo getRscDefaultsInfo() {
        return rscDefaultsInfo;
    }

    /** Checks all fields in the application. */
    void checkAccessOfEverything() {
        servicesInfo.checkResourceFieldsChanged(
                                         null,
                                         servicesInfo.getParametersFromXML());
        servicesInfo.updateAdvancedPanels();
        rscDefaultsInfo.updateAdvancedPanels();
        Tools.getGUIData().updateGlobalItems();
        for (final ServiceInfo si : getExistingServiceList(null)) {
            si.checkResourceFieldsChanged(null, si.getParametersFromXML());
            si.updateAdvancedPanels();
        }

        drbdGraph.getDrbdInfo().checkResourceFieldsChanged(
                                null,
                                drbdGraph.getDrbdInfo().getParametersFromXML());
        drbdGraph.getDrbdInfo().updateAdvancedPanels();
        for (final DrbdResourceInfo dri : getDrbdResHashValues()) {
            dri.checkResourceFieldsChanged(null, dri.getParametersFromXML());
            dri.updateAdvancedPanels();
            final BlockDevInfo bdi1 = dri.getFirstBlockDevInfo();
            if (bdi1 != null) {
                bdi1.checkResourceFieldsChanged(null,
                                                bdi1.getParametersFromXML());
                bdi1.updateAdvancedPanels();
            }
            final BlockDevInfo bdi2 = dri.getSecondBlockDevInfo();
            if (bdi2 != null) {
                bdi2.checkResourceFieldsChanged(null,
                                                bdi2.getParametersFromXML());
                bdi2.updateAdvancedPanels();
            }
        }

        if (vmsNode != null) {
            final Enumeration e = vmsNode.children();
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) e.nextElement();
                final VMSVirtualDomainInfo vmsvdi =
                                  (VMSVirtualDomainInfo) node.getUserObject();
                vmsvdi.checkResourceFieldsChanged(
                                                null,
                                                vmsvdi.getParametersFromXML());
                vmsvdi.updateAdvancedPanels();
                final Enumeration ce = node.children();
                while (ce.hasMoreElements()) {
                    final DefaultMutableTreeNode cnode =
                                     (DefaultMutableTreeNode) ce.nextElement();
                    final VMSHardwareInfo vmshi =
                                  (VMSHardwareInfo) cnode.getUserObject();
                    vmshi.checkResourceFieldsChanged(
                                                null,
                                                vmshi.getParametersFromXML());
                    vmshi.updateAdvancedPanels();
                }
            }
        }

        for (final HbConnectionInfo hbci
                                    : heartbeatGraph.getAllHbConnections()) {
            hbci.checkResourceFieldsChanged(null, hbci.getParametersFromXML());
            hbci.updateAdvancedPanels();
        }
    }


    /** Returns when at least one resource in the list of resouces can be
        promoted. */
    public boolean isOneMaster(final List<String> rscs) {
        for (final String id : rscs) {
            try {
                mHeartbeatIdToService.acquire();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            final ServiceInfo si = heartbeatIdToServiceInfo.get(id);
            mHeartbeatIdToService.release();
            if (si.getService().isMaster()) {
                return true;
            }
        }
        return false;
    }

    /** Returns all block devices from this clusters. */
    List<BlockDevInfo> getAllBlockDevices() {
        final List<BlockDevInfo> bdis = new ArrayList<BlockDevInfo>();
        for (final Host host : cluster.getHostsArray()) {
            bdis.addAll(host.getBrowser().getBlockDevInfos());
        }
        return bdis;
    }

    /** Updates host hardware info immediatly. */
    public void updateHWInfo(final Host host) {
        host.setIsLoading();
        host.getHWInfo(new CategoryInfo[]{clusterHostsInfo},
                       new ResourceGraph[]{drbdGraph, heartbeatGraph});
        drbdGraph.addHost(host.getBrowser().getHostDrbdInfo());
        updateDrbdResources();
        drbdGraph.repaint();
    }
}
