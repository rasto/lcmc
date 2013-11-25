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

package lcmc.gui;

import lcmc.utilities.Tools;
import lcmc.utilities.DRBD;
import lcmc.data.PtestData;
import lcmc.data.DRBDtestData;

import lcmc.data.Host;
import lcmc.data.Cluster;
import lcmc.data.ClusterStatus;
import lcmc.data.CRMXML;
import lcmc.data.DrbdXML;
import lcmc.data.VMSXML;
import lcmc.data.ConfigData;
import lcmc.utilities.NewOutputCallback;

import lcmc.utilities.ExecCallback;
import lcmc.utilities.Heartbeat;
import lcmc.utilities.CRM;
import lcmc.data.resources.Service;
import lcmc.data.resources.Network;

import lcmc.gui.resources.DrbdResourceInfo;
import lcmc.gui.resources.DrbdVolumeInfo;
import lcmc.gui.resources.HbCategoryInfo;
import lcmc.gui.resources.HbConnectionInfo;
import lcmc.gui.resources.Info;
import lcmc.gui.resources.CategoryInfo;
import lcmc.gui.resources.ServicesInfo;
import lcmc.gui.resources.ServiceInfo;
import lcmc.gui.resources.GroupInfo;
import lcmc.gui.resources.BlockDevInfo;
import lcmc.gui.resources.NetworkInfo;
import lcmc.gui.resources.DrbdInfo;
import lcmc.gui.resources.AvailableServiceInfo;
import lcmc.gui.resources.CommonBlockDevInfo;
import lcmc.gui.resources.CRMInfo;
import lcmc.gui.resources.VMSVirtualDomainInfo;
import lcmc.gui.resources.VMSInfo;
import lcmc.gui.resources.VMSHardwareInfo;
import lcmc.gui.resources.AvailableServicesInfo;
import lcmc.gui.resources.ResourceAgentClassInfo;
import lcmc.gui.resources.ClusterHostsInfo;
import lcmc.gui.resources.RscDefaultsInfo;

import lcmc.data.ResourceAgent;
import lcmc.utilities.ComponentWithTest;
import lcmc.utilities.ButtonCallback;

import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;

import java.awt.Color;
import java.awt.geom.Point2D;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.commons.collections15.map.MultiKeyMap;
import org.apache.commons.collections15.map.LinkedMap;
import org.apache.commons.collections15.keyvalue.MultiKey;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lcmc.data.StringValue;
import lcmc.data.Value;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;


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
    /** Logger. */
    private static final Logger LOG =
                               LoggerFactory.getLogger(ClusterBrowser.class);
    /**
     * Cluster object that holds data of the cluster. (One Browser belongs to
     * one cluster).
     */
    private final Cluster cluster;
    /** Menu's all hosts in the cluster node. */
    private DefaultMutableTreeNode clusterHostsNode;
    /** Menu's networks node. */
    private DefaultMutableTreeNode networksNode;
    /** Menu's common block devices node. */
    private DefaultMutableTreeNode commonBlockDevicesNode;
    /** Menu's available heartbeat services node. */
    private DefaultMutableTreeNode availableServicesNode;
    /** Heartbeat node. */
    private DefaultMutableTreeNode crmNode;
    /** Menu's heartbeat services node. */
    private DefaultMutableTreeNode servicesNode;
    /** Menu's drbd node. */
    private DefaultMutableTreeNode drbdNode;
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
    private final Lock mNameToServiceLock = new ReentrantLock();
    /** DRBD resource hash lock. */
    private final Lock mDrbdResHashLock = new ReentrantLock();
    /** DRBD resource name string to drbd resource info hash. */
    private final Map<String, DrbdResourceInfo> drbdResHash =
                                new HashMap<String, DrbdResourceInfo>();
    /** DRBD device hash lock. */
    private final Lock mDrbdDevHashLock = new ReentrantLock();
    /** DRBD resource device string to drbd resource info hash. */
    private final Map<String, DrbdVolumeInfo> drbdDevHash =
                                new HashMap<String, DrbdVolumeInfo>();
    /** Heartbeat id to service lock. */
    private final Lock mHeartbeatIdToService = new ReentrantLock();
    /** Heartbeat id to service info hash. */
    private final Map<String, ServiceInfo> heartbeatIdToServiceInfo =
                                          new HashMap<String, ServiceInfo>();
    /** Heartbeat graph. */
    private final CRMGraph crmGraph;
    /** DRBD graph. */
    private final DrbdGraph drbdGraph;
    /** object that holds current heartbeat status. */
    private ClusterStatus clusterStatus;
    /** Object that holds hb ocf data. */
    private CRMXML crmXML;
    /** Object that holds drbd status and data. */
    private DrbdXML drbdXML;
    /** VMS lock. */
    private final ReadWriteLock mVMSLock = new ReentrantReadWriteLock();
    /** VMS read lock. */
    private final Lock mVMSReadLock = mVMSLock.readLock();
    /** VMS write lock. */
    private final Lock mVMSWriteLock = mVMSLock.writeLock();
    /** Update lock. */
    private final Lock mVMSUpdateLock = new ReentrantLock();
    /** Object that hosts status of all VMs. */
    private final Map<Host, VMSXML> vmsXML = new HashMap<Host, VMSXML>();
    /** Object that has drbd test data. */
    private DRBDtestData drbdtestData;
    /** Whether drbd status was canceled by user. */
    private boolean drbdStatusCanceled = false;
    /** Whether hb status was canceled by user. */
    private boolean clStatusCanceled = false;
    /** Ptest lock. */
    private final Lock mPtestLock = new ReentrantLock();
    /** DRBD test data lock. */
    private final Lock mDRBDtestdataLock = new ReentrantLock();
    /** Can be used to cancel server status. */
    private volatile boolean serverStatusCanceled = false;
    /** last dc host detected. */
    private Host lastDcHost = null;
    /** dc host as reported by crm. */
    private Host realDcHost = null;
    /** Panel that holds this browser. */
    private ClusterViewPanel clusterViewPanel = null;
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
    /** Global hb status lock. */
    private final Lock mClStatusLock = new ReentrantLock();
    /** Remove icon. */
    public static final ImageIcon REMOVE_ICON =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.RemoveIcon"));
    /** Remove icon small. */
    public static final ImageIcon REMOVE_ICON_SMALL =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.RemoveIconSmall"));

    /** Hash that holds all hb classes with descriptions that appear in the
     * pull down menus. */
    public static final Map<String, String> HB_CLASS_MENU =
                                                new HashMap<String, String>();
    static {
        HB_CLASS_MENU.put(ResourceAgent.OCF_CLASS, "OCF Resource Agents");
        HB_CLASS_MENU.put(ResourceAgent.HEARTBEAT_CLASS,
                          "Heartbeat 1 RAs (deprecated)");
        HB_CLASS_MENU.put(ResourceAgent.LSB_CLASS, "LSB Init Scripts");
        HB_CLASS_MENU.put(ResourceAgent.STONITH_CLASS, "Stonith Devices");
        HB_CLASS_MENU.put(ResourceAgent.SERVICE_CLASS,
                          "Upstart/Systemd Scripts");
        HB_CLASS_MENU.put(ResourceAgent.SYSTEMD_CLASS, "Systemd Scripts");
        HB_CLASS_MENU.put(ResourceAgent.UPSTART_CLASS, "Upstart Scripts");
    }
    /** Width of the label in the info panel. */
    public static final int SERVICE_LABEL_WIDTH =
                    Tools.getDefaultSize("ClusterBrowser.ServiceLabelWidth");
    /** Width of the field in the info panel. */
    public static final int SERVICE_FIELD_WIDTH =
                    Tools.getDefaultSize("ClusterBrowser.ServiceFieldWidth");
    /** Color for stopped services. */
    public static final Color FILL_PAINT_STOPPED =
                      Tools.getDefaultColor("CRMGraph.FillPaintStopped");
    /** Identation. */
    public static final String IDENT_4 = "    ";
    /** Name of the boolean type in drbd. */
    public static final String DRBD_RES_BOOL_TYPE_NAME = "boolean";
    /** String array with all hb classes. */
    public static final List<String> HB_CLASSES = new ArrayList<String>();
    static {
        HB_CLASSES.add(ResourceAgent.OCF_CLASS);
        HB_CLASSES.add(ResourceAgent.HEARTBEAT_CLASS);
        for (final String c : ResourceAgent.SERVICE_CLASSES) {
            HB_CLASSES.add(c);
        }
        HB_CLASSES.add(ResourceAgent.STONITH_CLASS);
    }

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
    /** Not advanced operations. */
    private final MultiKeyMap<String, Integer> hbOpNotAdvanced =
                          MultiKeyMap.decorate(
                                 new LinkedMap<MultiKey<String>, Integer>());
    /** Map with drbd parameters for every host. */
    private final Map<Host, String> drbdParameters =
                                                  new HashMap<Host, String>();
    /** All parameters for the hb operations, so that it is possible to create
     * arguments for up_rsc_full_ops. */
    public static final String[] HB_OPERATION_PARAM_LIST = {
                                                        HB_PAR_DESC,
                                                        HB_PAR_INTERVAL,
                                                        HB_PAR_TIMEOUT,
                                                        CRMXML.PAR_CHECK_LEVEL,
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
    /** Default operation parameters. */
    private static final List<String> DEFAULT_OP_PARAMS =
                                       new ArrayList<String>(
                                               Arrays.asList(HB_PAR_TIMEOUT,
                                                             HB_PAR_INTERVAL));
    private static final String RESET_STRING = "---reset---\r\n";
    private static final int RESET_STRING_LEN = RESET_STRING.length();
    /** Match ...by-res/r0 or by-res/r0/0 from DRBD 8.4. */
    private static final Pattern BY_RES_PATTERN =
                    Pattern.compile("^/dev/drbd/by-res/([^/]+)(?:/(\\d+))?$");
    /** Prepares a new <code>CusterBrowser</code> object. */
    public ClusterBrowser(final Cluster cluster) {
        super();
        this.cluster = cluster;
        crmGraph = new CRMGraph(this);
        drbdGraph = new DrbdGraph(this);
        setTreeTop();

    }

    /** Inits operations. */
    private void initOperations() {
        hbOpNotAdvanced.put(HB_OP_START, HB_PAR_TIMEOUT, 1);
        hbOpNotAdvanced.put(HB_OP_STOP, HB_PAR_TIMEOUT, 1);
        hbOpNotAdvanced.put(HB_OP_MONITOR, HB_PAR_TIMEOUT, 1);
        hbOpNotAdvanced.put(HB_OP_MONITOR, HB_PAR_INTERVAL, 1);
        hbOpNotAdvanced.put(HB_OP_MONITOR, CRMXML.PAR_CHECK_LEVEL, 1);

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
        if (Tools.versionBeforePacemaker(dcHost)) {
            crmOperationParams.put(HB_OP_MONITOR,
                                new ArrayList<String>(
                                    Arrays.asList(HB_PAR_TIMEOUT,
                                                  HB_PAR_INTERVAL,
                                                  CRMXML.PAR_CHECK_LEVEL)));
        } else {
            crmOperationParams.put(HB_OP_MONITOR,
                                new ArrayList<String>(
                                        Arrays.asList(HB_PAR_TIMEOUT,
                                                      HB_PAR_INTERVAL,
                                                      CRMXML.PAR_CHECK_LEVEL,
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

    /** CRM operation parameters for one operations. */
    public List<String> getCRMOperationParams(final String op) {
        final List<String> params = crmOperationParams.get(op);
        if (params == null) {
            return DEFAULT_OP_PARAMS;
        } else {
            return params;
        }
    }

    /** Returns whether operation parameter is advanced. */
    public boolean isCRMOperationAdvanced(final String op, final String param) {
        if (!crmOperationParams.containsKey(op)) {
            return !HB_PAR_TIMEOUT.equals(param);
        }
        return !hbOpNotAdvanced.containsKey(op, param);
    }

    /** Sets the cluster view panel. */
    void setClusterViewPanel(final ClusterViewPanel clusterViewPanel) {
        this.clusterViewPanel = clusterViewPanel;
    }

    /** Returns cluster view panel. */
    public ClusterViewPanel getClusterViewPanel() {
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
        if (crmGraph != null) {
            crmGraph.getPositions(positions);
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
    public CRMGraph getCRMGraph() {
        return crmGraph;
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
        mHeartbeatIdToServiceLock();
        for (final String id : heartbeatIdToServiceInfo.keySet()) {
            final ServiceInfo si = heartbeatIdToServiceInfo.get(id);
            if (si.getResourceAgent().isDrbddisk()) {
                mHeartbeatIdToServiceUnlock();
                return true;
            }
        }
        mHeartbeatIdToServiceUnlock();
        return false;
    }

    /** Returns whether there is at least one drbddisk resource. */
    public boolean isOneLinbitDrbd() {
        mHeartbeatIdToServiceLock();
        for (final String id : heartbeatIdToServiceInfo.keySet()) {
            final ServiceInfo si = heartbeatIdToServiceInfo.get(id);
            if (si.getResourceAgent().isLinbitDrbd()) {
                mHeartbeatIdToServiceUnlock();
                return true;
            }
        }
        mHeartbeatIdToServiceUnlock();
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
        LOG.debug1("initClusterBrowser: start");
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
        crmNode = new DefaultMutableTreeNode(crmInfo);
        setNode(crmNode);
        topAdd(crmNode);

        /* available services */
        availableServicesNode = new DefaultMutableTreeNode(
            new AvailableServicesInfo(
                Tools.getString("ClusterBrowser.availableServices"),
                this));
        setNode(availableServicesNode);
        addNode(crmNode, availableServicesNode);

        /* block devices / shared disks, TODO: */
        commonBlockDevicesNode = new DefaultMutableTreeNode(
            new HbCategoryInfo(
                Tools.getString("ClusterBrowser.CommonBlockDevices"), this));
        setNode(commonBlockDevicesNode);
        /* addNode(crmNode, commonBlockDevicesNode); */

        /* resource defaults */
        rscDefaultsInfo = new RscDefaultsInfo("rsc_defaults", this);
        /* services */
        servicesInfo =
                new ServicesInfo(Tools.getString("ClusterBrowser.Services"),
                                 this);
        servicesNode = new DefaultMutableTreeNode(servicesInfo);
        setNode(servicesNode);
        addNode(crmNode, servicesNode);
        addVMSNode();
        selectPath(new Object[]{getTreeTop(), crmNode});
        addDrbdProxyNodes();
        LOG.debug1("initClusterBrowser: end");
    }

    void addDrbdProxyNodes() {
        final Set<Host> clusterHosts = getCluster().getHosts();
        for (final Host pHost : getCluster().getProxyHosts()) {
            if (!clusterHosts.contains(pHost)) {
                getDrbdGraph().getDrbdInfo().addProxyHostNode(pHost);
            }
        }
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
        LOG.debug1("start: update cluster resources");
        this.commonFileSystems = commonFileSystems.clone();
        this.commonMountPoints = commonMountPoints.clone();
        DefaultMutableTreeNode resource;

        /* cluster hosts */
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                clusterHostsNode.removeAllChildren();
            }
        });
        for (Host clusterHost : clusterHosts) {
            final HostBrowser hostBrowser = clusterHost.getBrowser();
            resource = hostBrowser.getTreeTop();
            setNode(resource);
            addNode(clusterHostsNode, resource);
            crmGraph.addHost(hostBrowser.getHostInfo());
        }

        reload(clusterHostsNode, false);

        /* block devices */
        updateCommonBlockDevices();

        /* networks */
        updateNetworks();
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                crmGraph.scale();
            }
        });
        updateHeartbeatDrbdThread();
        LOG.debug1("end: update cluster resources");
    }

    /** Returns mountpoints that exist on all servers. */
    public String[] getCommonMountPoints() {
        return commonMountPoints.clone();
    }

    /** Starts everything. */
    private void updateHeartbeatDrbdThread() {
        LOG.debug("updateHeartbeatDrbdThread: load cluster");
        final Thread tt = new Thread(new Runnable() {
            @Override
            public void run() {
                final Host[] hosts = cluster.getHostsArray();
                for (final Host host : hosts) {
                    host.waitForServerStatusLatch();
                    Tools.stopProgressIndicator(
                        host.getName(),
                        Tools.getString("ClusterBrowser.UpdatingServerInfo"));
                }
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        getClusterViewPanel().setDisabledDuringLoad(
                                                                false);
                        selectServices();
                    }
                });
            }
        });
        tt.start();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Host firstHost = null;
                final Host[] hosts = cluster.getHostsArray();
                Tools.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        for (final Host host : hosts) {
                            final HostBrowser hostBrowser = host.getBrowser();
                            drbdGraph.addHost(hostBrowser.getHostDrbdInfo());
                        }
                    }
                });
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
                if (!firstHost.isInCluster()) {
                    return;
                }

                LOG.debug1("updateHeartbeatDrbdThread: first host: " + firstHost);
                crmXML = new CRMXML(firstHost, getServicesInfo());
                clusterStatus = new ClusterStatus(firstHost, crmXML);
                initOperations();
                final DrbdXML newDrbdXML = new DrbdXML(cluster.getHostsArray(),
                                                       drbdParameters);
                drbdXML = newDrbdXML;
                /* available services */
                final String clusterName = getCluster().getName();
                Tools.startProgressIndicator(clusterName,
                        Tools.getString("ClusterBrowser.HbUpdateResources"));

                updateAvailableServices();
                Tools.stopProgressIndicator(clusterName,
                    Tools.getString("ClusterBrowser.HbUpdateResources"));
                Tools.startProgressIndicator(clusterName,
                    Tools.getString("ClusterBrowser.DrbdUpdate"));
                Tools.stopProgressIndicator(clusterName,
                    Tools.getString("ClusterBrowser.DrbdUpdate"));
                cluster.getBrowser().startConnectionStatus();
                cluster.getBrowser().startServerStatus();
                cluster.getBrowser().startDrbdStatus();
                cluster.getBrowser().startClStatus();
                LOG.debug1("updateHeartbeatDrbdThread: cluster loading done");
            }
        };
        final Thread thread = new Thread(runnable);
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
                @Override
                public void run() {
                    startServerStatus(host);
                }
            });
            thread.start();
        }
    }

    private void startPing(final Host host) {
        while (true) {
            host.startPing();
            Tools.sleep(10000);
            if (serverStatusCanceled) {
                break;
            }
        }
    }

    /** Start polling of the server status on one host. */
    void startServerStatus(final Host host) {
        final String hostName = host.getName();
        final CategoryInfo[] infosToUpdate =
                                        new CategoryInfo[]{clusterHostsInfo};
        while (true) {
            if (host.isServerStatusLatch()) {
                Tools.startProgressIndicator(
                         hostName,
                         Tools.getString("ClusterBrowser.UpdatingServerInfo"));
            }

            host.setIsLoading();
            host.startHWInfoDaemon(infosToUpdate,
                                   new ResourceGraph[]{drbdGraph, crmGraph});
            if (serverStatusCanceled) {
                break;
            }
            Tools.sleep(10000);
            if (serverStatusCanceled) {
                break;
            }
        }
    }

    public void updateServerStatus(final Host host) {
        final String hostName = host.getName();
        Tools.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                drbdGraph.addHost(host.getBrowser().getHostDrbdInfo());
            }
        });
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                drbdGraph.scale();
            }
        });
        if (host.isServerStatusLatch()) {
            LOG.debug("updateServerStatus: " + host.getName()
                      + " loading done");
        }
        host.serverStatusLatchDone();
        clusterHostsInfo.updateTable(CategoryInfo.MAIN_TABLE);
        for (final ResourceGraph g : new ResourceGraph[]{drbdGraph, crmGraph}) {
            if (g != null) {
                g.repaint();
                g.updatePopupMenus();
            }
        }
    }

    /** Updates VMs info. */
    public void periodicalVMSUpdate(final Host host) {
        final VMSXML newVMSXML = new VMSXML(host);
        if (newVMSXML.update()) {
            vmsXMLPut(host, newVMSXML);
            updateVMS();
        }
    }

    /** Updates VMs info. */
    public void periodicalVMSUpdate(final Host[] hosts) {
        periodicalVMSUpdate(Arrays.asList(hosts));
    }

    /** Updates VMs info. */
    public void periodicalVMSUpdate(final List<Host> hosts) {
        boolean updated = false;
        for (final Host host : hosts) {
            final VMSXML newVMSXML = new VMSXML(host);
            if (newVMSXML.update()) {
                vmsXMLPut(host, newVMSXML);
                updated = true;
            }
        }
        if (updated) {
            updateVMS();
        }
    }

    /** Adds new vmsxml object to the hash. */
    public void vmsXMLPut(final Host host, final VMSXML newVMSXML) {
        mVMSWriteLock.lock();
        try {
            vmsXML.put(host, newVMSXML);
        } finally {
            mVMSWriteLock.unlock();
        }
    }

    /** Returns wheter server status. */
    public boolean isCancelServerStatus() {
        return serverStatusCanceled;
    }

    /** Starts connection status on all hosts. */
    void startConnectionStatus() {
        final Host[] hosts = cluster.getHostsArray();
        for (final Host host : hosts) {
            final Thread pingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    startPing(host);
                }
            });
            pingThread.start();
            host.startConnectionStatus();
        }
    }

    /** Starts drbd status on all hosts. */
    void startDrbdStatus() {
        final Host[] hosts = cluster.getHostsArray();
        for (final Host host : hosts) {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
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
            @Override
            public void run() {
                try {
                    firstTime.await();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        drbdGraph.scale();
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
                       @Override
                       public void done(final String ans) {
                           firstTime.countDown();
                           if (!host.isDrbdStatus()) {
                               host.setDrbdStatus(true);
                               drbdGraph.repaint();
                               LOG.debug1("startDrbdStatus: host: " + host.getName());
                               clusterHostsInfo.updateTable(
                                                ClusterHostsInfo.MAIN_TABLE);
                           }
                       }

                       @Override
                       public void doneError(final String ans,
                                             final int exitCode) {
                           firstTime.countDown();
                           LOG.debug1("startDrbdStatus: failed: " + host.getName() + " exit code: " + exitCode);
                           if (exitCode != 143 && exitCode != 100) {
                               // TODO: exit code is null -> 100 all of the
                               // sudden
                               /* was killed intentionally */
                               if (host.isDrbdStatus()) {
                                   host.setDrbdStatus(false);
                                   LOG.debug1("startDrbdStatus: host: "
                                              + host.getName());
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
                       private StringBuffer outputBuffer =
                                                        new StringBuffer(300);
                       @Override
                       public void output(final String output) {
                           if ("--nm--".equals(output.trim())) {
                               if (host.isDrbdStatus()) {
                                   LOG.debug1("startDrbdStatus: host: "
                                              + host.getName());
                                   host.setDrbdStatus(false);
                                   drbdGraph.repaint();
                                   clusterHostsInfo.updateTable(
                                                ClusterHostsInfo.MAIN_TABLE);
                               }
                               firstTime.countDown();
                               return;
                           }
                           firstTime.countDown();
                           if (!host.isDrbdStatus()) {
                               LOG.debug1("startDrbdStatus: host: " + host.getName());
                               host.setDrbdStatus(true);
                               drbdGraph.repaint();
                               clusterHostsInfo.updateTable(
                                                  ClusterHostsInfo.MAIN_TABLE);
                           }
                           outputBuffer.append(output);
                           String drbdConfig, event;
                           boolean drbdUpdate = false;
                           boolean eventUpdate = false;
                           do {
                               host.drbdStatusLock();
                               drbdConfig =
                                        host.getOutput("drbd", outputBuffer);
                               if (drbdConfig != null) {
                                   final DrbdXML newDrbdXML =
                                            new DrbdXML(cluster.getHostsArray(),
                                                        drbdParameters);
                                   newDrbdXML.update(drbdConfig);
                                   drbdXML = newDrbdXML;
                                   drbdUpdate = true;
                                   firstTime.countDown();
                               }
                               host.drbdStatusUnlock();
                               event = host.getOutput("event", outputBuffer);
                               if (event != null) {
                                   if (drbdXML.parseDrbdEvent(host.getName(),
                                                              drbdGraph,
                                                              event)) {
                                       host.setDrbdStatus(true);
                                       eventUpdate = true;
                                   }
                               }
                           } while (event != null || drbdConfig != null);
                           Tools.chomp(outputBuffer);
                           if (drbdUpdate) {
                               Tools.invokeLater(new Runnable() {
                            @Override
                                   public void run() {
                                       getDrbdGraph().getDrbdInfo().setParameters();
                                       updateDrbdResources();
                                   }
                               });
                           }
                           if (eventUpdate) {
                               drbdGraph.repaint();
                               LOG.debug1("drbd status update: " + host.getName());
                               clusterHostsInfo.updateTable(
                                                ClusterHostsInfo.MAIN_TABLE);
                               firstTime.countDown();
                               final Thread thread = new Thread(
                                   new Runnable() {
                                       @Override
                                       public void run() {
                                           repaintSplitPane();
                                           drbdGraph.updatePopupMenus();
                                           Tools.invokeLater(
                                               new Runnable() {
                                                   @Override
                                                   public void run() {
                                                       repaintTree();
                                                   }
                                               }
                                           );
                                       }
                                   });
                               thread.start();
                           }
                       }
                   });
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
        for (final Host host : hosts) {
            host.stopClStatus();
        }
    }

    /** Stops server status. */
    public void stopServerStatus() {
        serverStatusCanceled = true;
        final Host[] hosts = cluster.getHostsArray();
        for (final Host host : hosts) {
            host.stopServerStatus();
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
    void processClusterOutput(final String output,
                              final StringBuffer clusterStatusOutput,
                              final Host host,
                              final CountDownLatch firstTime,
                              final boolean testOnly) {
        final ClusterStatus clStatus = clusterStatus;
        clStatusLock();
        if (clStatusCanceled || clStatus == null) {
            clStatusUnlock();
            firstTime.countDown();
            return;
        }
        if (output == null || "".equals(output)) {
            clStatus.setOnlineNode(host.getName(), "no");
            setClStatus(host, false);
            firstTime.countDown();
        } else {
            // TODO: if we get ERROR:... show it somewhere
            clusterStatusOutput.append(output);
            /* removes the string from the output. */
            int s = clusterStatusOutput.indexOf(RESET_STRING);
            while (s >= 0) {
                clusterStatusOutput.delete(s, s + RESET_STRING_LEN);
                s = clusterStatusOutput.indexOf(RESET_STRING);
            }
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
                                clStatus.setOnlineNode(host.getName(),
                                                            "no");
                                setClStatus(host, false);
                                if (oldStatus) {
                                   crmGraph.repaint();
                                }
                            } else {
                                if (clStatus.parseStatus(status)) {
                                    LOG.debug1("processClusterOutput: host: "
                                               + host.getName());
                                    final ServicesInfo ssi = servicesInfo;
                                    rscDefaultsInfo.setParameters(
                                      clStatus.getRscDefaultsValuePairs());
                                    ssi.setGlobalConfig(clStatus);
                                    ssi.setAllResources(clStatus, testOnly);
                                    if (firstTime.getCount() == 1) {
                                        /* one more time so that id-refs work.*/
                                        ssi.setAllResources(clStatus, testOnly);
                                    }
                                    repaintTree();
                                    clusterHostsInfo.updateTable(
                                                ClusterHostsInfo.MAIN_TABLE);
                                }
                                final String online =
                                    clStatus.isOnlineNode(host.getName());
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
            Tools.chomp(clusterStatusOutput);
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
            @Override
            public void run() {
                try {
                    firstTime.await();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                if (clStatusFailed()) {
                     Tools.progressIndicatorFailed(
                        clusterName,
                        Tools.getString("ClusterBrowser.ClusterStatusFailed"));
                } else {
                    Tools.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                           crmGraph.scale();
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
                     @Override
                     public void done(final String ans) {
                         final String online =
                                    clusterStatus.isOnlineNode(host.getName());
                         setClStatus(host, "yes".equals(online));
                         firstTime.countDown();
                     }

                     @Override
                     public void doneError(final String ans,
                                           final int exitCode) {
                         if (firstTime.getCount() == 1) {
                             LOG.debug2("startClStatus: status failed: "
                                        + host.getName()
                                        + ", ec: " + exitCode);
                         }
                         clStatusLock();
                         clusterStatus.setOnlineNode(host.getName(), "no");
                         setClStatus(host, false);
                         clusterStatus.setDC(null);
                         clStatusUnlock();
                         if (exitCode == 255) {
                             /* looks like connection was lost */
                             //crmGraph.repaint();
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
                     @Override
                     public void output(final String output) {
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
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
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
            final ClusterBrowser thisBrowser = this;
            Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                @Override
                public void run() {
                    final List<String> bd = cluster.getCommonBlockDevices();
                    @SuppressWarnings("unchecked")
                    final Enumeration<DefaultMutableTreeNode> e =
                                             commonBlockDevicesNode.children();
                    final List<DefaultMutableTreeNode> nodesToRemove =
                                       new ArrayList<DefaultMutableTreeNode>();
                    while (e.hasMoreElements()) {
                        final DefaultMutableTreeNode node = e.nextElement();
                        final Info cbdi = (Info) node.getUserObject();
                        if (bd.contains(cbdi.getName())) {
                            /* keeping */
                            bd.remove(bd.indexOf(cbdi.getName()));
                        } else {
                            /* remove not existing block devices */
                            cbdi.setNode(null);
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
                        final DefaultMutableTreeNode resource =
                            new DefaultMutableTreeNode(
                                 new CommonBlockDevInfo(
                                          device,
                                          cluster.getHostBlockDevices(device),
                                          thisBrowser));
                        setNode(resource);
                        addNode(commonBlockDevicesNode, resource);
                    }
                    if (!bd.isEmpty() || !nodesToRemove.isEmpty()) {
                        reload(commonBlockDevicesNode, false);
                        reloadAllComboBoxes(null);
                    }
                }
            });

        }
    }

    /** Updates available services. */
    private void updateAvailableServices() {
        DefaultMutableTreeNode resource;
        LOG.debug("updateAvailableServices: start");
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                availableServicesNode.removeAllChildren();
            }
        });
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
                addNode(classNode, resource);
            }
            setNode(classNode);
            addNode(availableServicesNode, classNode);
        }
    }

    /** Updates VM nodes. */
    public void updateVMS() {
        LOG.debug1("updateVMS: status update");
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

        mVMSUpdateLock.lock();
        if (vmsNode != null) {
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> ee = vmsNode.children();
            while (ee.hasMoreElements()) {
                final DefaultMutableTreeNode node = ee.nextElement();
                final VMSVirtualDomainInfo vmsvdi =
                                  (VMSVirtualDomainInfo) node.getUserObject();
                if (domainNames.contains(vmsvdi.toString())) {
                    /* keeping */
                    currentVMSVDIs.add(vmsvdi);
                    domainNames.remove(vmsvdi.toString());
                    vmsvdi.updateParameters(); /* update old */
                } else {
                    if (!vmsvdi.getResource().isNew()) {
                        /* remove not existing vms */
                        vmsvdi.setNode(null);
                        nodesToRemove.add(node);
                        nodeChanged = true;
                    }
                }
            }
        }

        /* remove nodes */
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final DefaultMutableTreeNode node : nodesToRemove) {
                    node.removeFromParent();
                }
            }
        });

        if (vmsNode == null) {
            mVMSUpdateLock.unlock();
            return;
        }
        for (final String domainName : domainNames) {
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> e = vmsNode.children();
            int i = 0;
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode node = e.nextElement();
                final VMSVirtualDomainInfo vmsvdi =
                                  (VMSVirtualDomainInfo) node.getUserObject();
                final String name = vmsvdi.getName();
                if (domainName != null
                    && name != null
                    && domainName.compareTo(vmsvdi.getName()) < 0) {
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
                @Override
                public void run() {
                    vmsNode.insert(resource, index);
                }
            });
            nodeChanged = true;
        }
        mVMSUpdateLock.unlock();
        if (nodeChanged) {
            reload(vmsNode, false);
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
    }

    /** Returns vmsinfo object. */
    public VMSInfo getVMSInfo() {
        return (VMSInfo) vmsNode.getUserObject();
    }

    /** Updates drbd resources. */
    public void updateDrbdResources() {
        Tools.isSwingThread();
        final boolean testOnly = false;
        final DrbdInfo drbdInfo = drbdGraph.getDrbdInfo();
        boolean atLeastOneAdded = false;
        drbdStatusLock();
        final DrbdXML dxml = drbdXML;
        if (dxml == null) {
            drbdStatusUnlock();
            return;
        }
        for (final Object k : dxml.getResourceDeviceMap().keySet()) {
            final String resName = (String) ((MultiKey) k).getKey(0);
            final String volumeNr = (String) ((MultiKey) k).getKey(1);
            final String drbdDev = dxml.getDrbdDevice(resName, volumeNr);
            final Map<String, String> hostDiskMap =
                                                dxml.getHostDiskMap(resName,
                                                                    volumeNr);
            BlockDevInfo bd1 = null;
            BlockDevInfo bd2 = null;
            if (hostDiskMap == null) {
                continue;
            }
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
                        LOG.appWarning("updateDrbdResources: could not find disk: " + disk + " on host: " + hostName);
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
            if (bd1 != null && bd2 != null) {
                /* add DRBD resource */
                DrbdResourceInfo dri = getDrbdResHash().get(resName);
                putDrbdResHash();
                final List<BlockDevInfo> bdis =
                                new ArrayList<BlockDevInfo>(Arrays.asList(bd1,
                                                                          bd2));
                if (dri == null) {
                    dri = drbdInfo.addDrbdResource(
                               resName,
                               DrbdVolumeInfo.getHostsFromBlockDevices(bdis),
                               testOnly);
                    atLeastOneAdded = true;
                }
                DrbdVolumeInfo dvi = dri.getDrbdVolumeInfo(volumeNr);
                if (dvi == null) {
                    dvi = drbdInfo.addDrbdVolume(
                                           dri,
                                           volumeNr,
                                           drbdDev,
                                           bdis,
                                           testOnly);
                    atLeastOneAdded = true;
                }
                dri.setParameters();
                dvi.setParameters();
                final DrbdResourceInfo dri0 = dri;
                dri0.getInfoPanel();
            }
        }
        //TODO: it would remove it during drbd wizards
        //killRemovedVolumes(dxml.getResourceDeviceMap());
        drbdStatusUnlock();
        if (atLeastOneAdded) {
            drbdInfo.getInfoPanel();
            drbdInfo.setAllApplyButtons();
            drbdInfo.reloadDRBDResourceComboBoxes();
            drbdGraph.scale();
        }
    }

    /**
     * Kill removed volumes. (removed outside of GUI)
     * TODO: not used at the moment
     */

    private void killRemovedVolumes(
                                final MultiKeyMap<String, String> deviceMap) {
        for (final DrbdVolumeInfo dvi
                         : getDrbdGraph().getDrbdVolumeToEdgeMap().keySet()) {
            if (!deviceMap.containsKey(dvi.getDrbdResourceInfo().getName(),
                                       dvi.getName())) {
                getDrbdXML().removeVolume(dvi.getDrbdResourceInfo().getName(),
                                          dvi.getDevice(),
                                          dvi.getName());
                getDrbdGraph().removeDrbdVolume(dvi);
                final boolean lastVolume =
                            dvi.getDrbdResourceInfo().removeDrbdVolume(dvi);
                getDrbdDevHash().remove(dvi.getDevice());
                putDrbdDevHash();
                for (final BlockDevInfo bdi : dvi.getBlockDevInfos()) {
                    bdi.removeFromDrbd();
                    bdi.removeMyself(DRBD.LIVE);
                }
                if (lastVolume) {
                    dvi.getDrbdResourceInfo().removeMyself(CRM.LIVE);
                }
            }
        }
    }

    /** Updates networks. */
    private void updateNetworks() {
        if (networksNode != null) {
            DefaultMutableTreeNode resource;
            final Network[] networks = cluster.getCommonNetworks();
            Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                @Override
                public void run() {
                    networksNode.removeAllChildren();
                }
            });
            for (int i = 0; i < networks.length; i++) {
                resource = new DefaultMutableTreeNode(
                                    new NetworkInfo(networks[i].getName(),
                                                    networks[i],
                                                    this));
                setNode(resource);
                addNode(networksNode, resource);
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
        //LOG.appError("Could not find any hosts");
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

    /** drbdStatusLock global lock. */
    public void drbdStatusLock() {
        for (final Host h : getClusterHosts()) {
            h.drbdStatusLock();
        }
    }

    /** drbdStatusLock global unlock. */
    public void drbdStatusUnlock() {
        final Host[] hosts = getClusterHosts();
        for (int i = hosts.length - 1; i >= 0; i--) {
            hosts[i].drbdStatusUnlock();
        }
    }

    /** vmStatusLock global lock. */
    public void vmStatusLock() {
        for (final Host h : getClusterHosts()) {
            h.vmStatusLock();
        }
    }

    /** vmStatusLock global unlock. */
    public void vmStatusUnlock() {
        final Host[] hosts = getClusterHosts();
        for (int i = hosts.length - 1; i >= 0; i--) {
            hosts[i].vmStatusUnlock();
        }
    }


    /** Highlights drbd node. */
    void selectDrbd() {
        reload(drbdNode, true);
    }

    /** Highlights services. */
    public void selectServices() {
        if (getClusterViewPanel().isDisabledDuringLoad()) {
            return;
        }
        selectPath(new Object[]{getTreeTop(), crmNode, servicesNode});
    }

    /** Returns ServiceInfo object from crm id. */
    public ServiceInfo getServiceInfoFromCRMId(final String crmId) {
        mHeartbeatIdToServiceLock();
        final ServiceInfo si = heartbeatIdToServiceInfo.get(crmId);
        mHeartbeatIdToServiceUnlock();
        return si;
    }

    /** Returns if the crm id is already taken. */
    public boolean isCRMId(final String crmId) {
        mHeartbeatIdToServiceLock();
        final boolean ret = heartbeatIdToServiceInfo.containsKey(crmId);
        mHeartbeatIdToServiceUnlock();
        return ret;
    }

    /** Locks heartbeatIdToServiceInfo hash. */
    public void mHeartbeatIdToServiceLock() {
        mHeartbeatIdToService.lock();
    }

    /** Unlocks heartbeatIdToServiceInfo hash. */
    public void mHeartbeatIdToServiceUnlock() {
        mHeartbeatIdToService.unlock();
    }

    /** Returns heartbeatIdToServiceInfo hash. You have to lock it. */
    public Map<String, ServiceInfo> getHeartbeatIdToServiceInfo() {
        return heartbeatIdToServiceInfo;
    }

    /** Returns ServiceInfo object identified by name and id. */
    public ServiceInfo getServiceInfoFromId(final String name,
                                            final String id) {
        lockNameToServiceInfo();
        final Map<String, ServiceInfo> idToInfoHash =
                                               nameToServiceInfoHash.get(name);
        if (idToInfoHash == null) {
            unlockNameToServiceInfo();
            return null;
        }
        final ServiceInfo si = idToInfoHash.get(id);
        unlockNameToServiceInfo();
        return si;
    }

    /** Returns the name, id to service info hash. */
    public Map<String, Map<String, ServiceInfo>> getNameToServiceInfoHash() {
        return nameToServiceInfoHash;
    }

    /** Locks the nameToServiceInfoHash. */
    public void lockNameToServiceInfo() {
        mNameToServiceLock.lock();
    }

    /** Unlocks the nameToServiceInfoHash. */
    public void unlockNameToServiceInfo() {
        mNameToServiceLock.unlock();
    }

    /** Returns 'existing service' list for graph popup menu. */
    public List<ServiceInfo> getExistingServiceList(final ServiceInfo p) {
        final List<ServiceInfo> existingServiceList =
                                                  new ArrayList<ServiceInfo>();
        lockNameToServiceInfo();
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
                if (p == null || !getCRMGraph().existsInThePath(sigi, p)) {
                    existingServiceList.add(si);
                }
            }
        }
        unlockNameToServiceInfo();
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
        lockNameToServiceInfo();
        final Map<String, ServiceInfo> idToInfoHash =
                                 nameToServiceInfoHash.get(service.getName());
        if (idToInfoHash != null) {
            idToInfoHash.remove(service.getId());
            if (idToInfoHash.isEmpty()) {
                nameToServiceInfoHash.remove(service.getName());
            }
        }
        unlockNameToServiceInfo();
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
            mHeartbeatIdToServiceLock();
            heartbeatIdToServiceInfo.put(newPmId, si);
            mHeartbeatIdToServiceUnlock();
        } else {
            mHeartbeatIdToServiceLock();
            if (heartbeatIdToServiceInfo.get(pmId) == null) {
                heartbeatIdToServiceInfo.put(pmId, si);
            }
            mHeartbeatIdToServiceUnlock();
        }
    }

    /**
     * Deletes caches of all Filesystem infoPanels.
     * This is useful if something have changed.
     */
    public void resetFilesystems() {
        mHeartbeatIdToServiceLock();
        for (String hbId : heartbeatIdToServiceInfo.keySet()) {
            final ServiceInfo si = heartbeatIdToServiceInfo.get(hbId);
            if (si.getName().equals("Filesystem")) {
                si.setInfoPanel(null);
            }
        }
        mHeartbeatIdToServiceUnlock();
    }

    /** Check if the id exists for the service already, if so add _$index
     * to it.
     * @param id can be null. */
    public String getFreeId(final String serviceName, final String id) {
        if (id == null) {
            return id;
        }
        String newId = id;
        lockNameToServiceInfo();
        try {
            final Map<String, ServiceInfo> idToInfoHash =
                                     nameToServiceInfoHash.get(serviceName);
            int index = 2;
            if (idToInfoHash != null) {
                while (idToInfoHash.containsKey(newId)) {
                    newId = id + "_" + index;
                    index++;
                }
            }
        } finally {
            unlockNameToServiceInfo();
        }
        return newId;
    }

    /**
     * Adds ServiceInfo in the name to ServiceInfo hash. Id and name
     * are taken from serviceInfo object. nameToServiceInfoHash
     * contains a hash with id as a key and ServiceInfo as a value.
     */
    public void addNameToServiceInfoHash(final ServiceInfo serviceInfo) {
        /* add to the hash with service name and id as keys */
        final Service service = serviceInfo.getService();
        lockNameToServiceInfo();
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
                int index = 0;
                for (final String id : idToInfoHash.keySet()) {
                    Pattern p;
                    if (csPmId == null) {
                        p = Pattern.compile("^(\\d+)$");
                    } else {
                        /* ms */
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
                            LOG.appWarning("addNameToServiceInfoHash: could not parse: " + m.group(1));
                        }
                    }
                }

                if (csPmId == null) {
                    service.setId(Integer.toString(index + 1));
                } else {
                    /* ms */
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
        unlockNameToServiceInfo();
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
        return Tools.versionBeforePacemaker(getDCHost());
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
        @Override
        public boolean isEnabled() {
            if (clusterStatus == null) {
                return false;
            }
            Host h;
            if (menuHost == null) {
                h = getDCHost();
            } else {
                h = menuHost;
            }
            if (Tools.versionBeforePacemaker(h)) {
                return false;
            }
            return true;
        }

        /** Mouse out, stops animation. */
        @Override
        public final void mouseOut() {
            if (isEnabled()) {
                mouseStillOver = false;
                crmGraph.stopTestAnimation((JComponent) component);
                component.setToolTipText("");
            }
        }

        /** Mouse over, starts animation, calls action() and sets tooltip. */
        @Override
        public final void mouseOver() {
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
                crmGraph.startTestAnimation((JComponent) component,
                                            startTestLatch);
                ptestLockAcquire();
                try {
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
                } finally {
                    ptestLockRelease();
                }
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
        @Override
        public final boolean isEnabled() {
            return true;
        }

        /** Mouse out, stops animation. */
        @Override
        public final void mouseOut() {
            if (!isEnabled()) {
                return;
            }
            mouseStillOver = false;
            drbdGraph.stopTestAnimation((JComponent) component);
            component.setToolTipText("");
        }

        /** Mouse over, starts animation, calls action() and sets tooltip. */
        @Override
        public final void mouseOver() {
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
            if (menuHost == null) {
                for (final Host h : cluster.getHostsArray()) {
                    action(h);
                    testOutput.put(h, DRBD.getDRBDtest());
                }
            } else {
                action(menuHost);
                testOutput.put(menuHost, DRBD.getDRBDtest());
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
     * Returns common file systems on all nodes as StringValue array.
     * The defaultValue is stored as the first item in the array.
     */
    public Value[] getCommonFileSystems(final String defaultValue) {
        Value[] cfs =  new Value[commonFileSystems.length + 2];
        cfs[0] = new StringValue(null, defaultValue);
        int i = 1;
        for (String cf : commonFileSystems) {
            cfs[i] = new StringValue(cf);
            i++;
        }
        cfs[i] = new StringValue("none");
            i++;
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
        mPtestLock.lock();
    }

    /** Release ptest lock. */
    public void ptestLockRelease() {
        mPtestLock.unlock();
    }

    /** Acquire drbd test data lock. */
    protected void drbdtestdataLockAcquire() {
        mDRBDtestdataLock.lock();
    }

    /** Release drbd test data lock. */
    protected void drbdtestdataLockRelease() {
        mDRBDtestdataLock.unlock();
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
     * Returns a hash from drbd device to drbd volume info. putDrbdDevHash
     * must follow after you're done. */
    public Map<String, DrbdVolumeInfo> getDrbdDevHash() {
        mDrbdDevHashLock.lock();
        return drbdDevHash;
    }

    /** Unlock drbd dev hash. */
    public void putDrbdDevHash() {
        mDrbdDevHashLock.unlock();
    }

    /**
     * Return volume info object from the drbd block device name.
     * /dev/drbd/by-res/r0
     * /dev/drbd/by-res/r0/0
     * /dev/drbd0
     */
    public DrbdVolumeInfo getDrbdVolumeFromDev(final String dev) {
        if (dev == null) {
            return null;
        }
        final Matcher m = BY_RES_PATTERN.matcher(dev);
        if (m.matches()) {
            final String res = m.group(1);
            String vol;
            if (m.groupCount() > 2) {
                vol = m.group(2);
            } else {
                vol = "0";
            }
            final DrbdResourceInfo dri = getDrbdResHash().get(res);
            putDrbdResHash();
            if (dri != null) {
                return dri.getDrbdVolumeInfo(vol);
            }
        }
        return null;
    }

    /**
     * Returns a hash from resource name to drbd resource info hash.
     * Get locks the hash and put unlocks it
     */
    public Map<String, DrbdResourceInfo> getDrbdResHash() {
        mDrbdResHashLock.lock();
        return drbdResHash;
    }

    /** Done using drbdResHash. */
    public void putDrbdResHash() {
        mDrbdResHashLock.unlock();
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
        lockNameToServiceInfo();
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
        unlockNameToServiceInfo();
    }

    /** Returns object that holds data of all VMs. */
    public VMSXML getVMSXML(final Host host) {
        mVMSReadLock.lock();
        try {
            return vmsXML.get(host);
        } finally {
            mVMSReadLock.unlock();
        }
    }

    /**
     * Finds VMSVirtualDomainInfo object that contains the VM specified by
     * name.
     */
    public VMSVirtualDomainInfo findVMSVirtualDomainInfo(final String name) {
        if (vmsNode != null && name != null) {
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> e = vmsNode.children();
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode node = e.nextElement();
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
    public void checkAccessOfEverything() {
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
            dri.updateAllVolumes();
        }

        if (vmsNode != null) {
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> e = vmsNode.children();
            while (e.hasMoreElements()) {
                final DefaultMutableTreeNode node = e.nextElement();
                final VMSVirtualDomainInfo vmsvdi =
                                  (VMSVirtualDomainInfo) node.getUserObject();
                vmsvdi.checkResourceFieldsChanged(
                                                null,
                                                vmsvdi.getParametersFromXML());
                vmsvdi.updateAdvancedPanels();
                @SuppressWarnings("unchecked")
                final Enumeration<DefaultMutableTreeNode> ce = node.children();
                while (ce.hasMoreElements()) {
                    final DefaultMutableTreeNode cnode = ce.nextElement();
                    final VMSHardwareInfo vmshi =
                                  (VMSHardwareInfo) cnode.getUserObject();
                    vmshi.checkResourceFieldsChanged(
                                                null,
                                                vmshi.getParametersFromXML());
                    vmshi.updateAdvancedPanels();
                }
            }
        }

        for (final HbConnectionInfo hbci : crmGraph.getAllHbConnections()) {
            hbci.checkResourceFieldsChanged(null, hbci.getParametersFromXML());
            hbci.updateAdvancedPanels();
        }

        for (final Host clusterHost : getClusterHosts()) {
            final HostBrowser hostBrowser = clusterHost.getBrowser();
            hostBrowser.getHostInfo().updateAdvancedPanels();
        }
    }


    /** Returns when at least one resource in the list of resources can be
        promoted. */
    public boolean isOneMaster(final List<String> rscs) {
        for (final String id : rscs) {
            mHeartbeatIdToServiceLock();
            final ServiceInfo si = heartbeatIdToServiceInfo.get(id);
            mHeartbeatIdToServiceUnlock();
            if (si == null) {
                continue;
            }
            if (si.getService().isMaster()) {
                return true;
            }
        }
        return false;
    }

    /** Updates host hardware info on all cluster hosts immediately. */
    public void updateHWInfo(boolean updateLVM) {
        for (final Host h : getClusterHosts()) {
            updateHWInfo(h, updateLVM);
        }
    }

    /** Updates host hardware info immediately. */
    public void updateHWInfo(final Host host, boolean updateLVM) {
        host.setIsLoading();
        host.getHWInfo(new CategoryInfo[]{clusterHostsInfo},
                       new ResourceGraph[]{drbdGraph, crmGraph},
                       updateLVM);
        Tools.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                drbdGraph.addHost(host.getBrowser().getHostDrbdInfo());
            }
        });
        updateCommonBlockDevices();
        drbdGraph.repaint();
    }

    /** Updates proxy host hardware info immediately. */
    public void updateProxyHWInfo(final Host host) {
        host.setIsLoading();
        host.getHWInfo(new CategoryInfo[]{clusterHostsInfo},
                       new ResourceGraph[]{drbdGraph, crmGraph},
                       !Host.UPDATE_LVM);
        updateCommonBlockDevices();
        drbdGraph.repaint();
    }

    /** Returns DRBD parameter hash. */
    public Map<Host, String> getDrbdParameters() {
        return drbdParameters;
    }

    /** clStatusLock global lock. */
    public void clStatusLock() {
        mClStatusLock.lock();
    }

    /** clStatusLock global unlock. */
    public void clStatusUnlock() {
        mClStatusLock.unlock();
    }

    /** Return name of the classes in the menu. */
    public static String getClassMenu(final String cl) {
        final String name = ClusterBrowser.HB_CLASS_MENU.get(cl);
        if (name == null) {
            return Tools.ucfirst(cl) + " scripts";
        }
        return name;
    }

    /** Return null if DRBD info is availble, or the reason why not. */
    public String isDrbdAvailable(final Host host) {
        if (drbdParameters.get(host) == null) {
            return "no suitable man pages";
        }
        if (!DRBD.compatibleVersions(host.getDrbdVersion(),
                                     host.getDrbdModuleVersion())) {
            return "DRBD util and module versions are not compatible: "
                   + host.getDrbdVersion() + " / "
                   + host.getDrbdModuleVersion();
        }
        return null;
    }
}
