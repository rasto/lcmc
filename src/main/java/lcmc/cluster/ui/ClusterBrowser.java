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

package lcmc.cluster.ui;

import com.google.common.collect.Table;
import com.google.common.eventbus.Subscribe;
import lcmc.ClusterEventBus;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.domain.Network;
import lcmc.cluster.service.NetworkService;
import lcmc.cluster.ui.network.NetworkFactory;
import lcmc.cluster.ui.network.NetworkPresenter;
import lcmc.common.domain.Application;
import lcmc.common.domain.ExecCallback;
import lcmc.common.domain.NewOutputCallback;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Access;
import lcmc.common.ui.Browser;
import lcmc.common.ui.CallbackAction;
import lcmc.common.ui.CategoryInfo;
import lcmc.common.ui.Info;
import lcmc.common.ui.ResourceGraph;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.ui.utils.ComponentWithTest;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.crm.domain.ClusterStatus;
import lcmc.crm.domain.CrmXml;
import lcmc.crm.domain.PtestData;
import lcmc.crm.domain.ResourceAgent;
import lcmc.crm.domain.Service;
import lcmc.crm.service.CRM;
import lcmc.crm.service.Heartbeat;
import lcmc.crm.ui.CrmGraph;
import lcmc.crm.ui.resource.AvailableServiceInfo;
import lcmc.crm.ui.resource.AvailableServicesInfo;
import lcmc.crm.ui.resource.CRMInfo;
import lcmc.crm.ui.resource.GroupInfo;
import lcmc.crm.ui.resource.HbConnectionInfo;
import lcmc.crm.ui.resource.ResourceAgentClassInfo;
import lcmc.crm.ui.resource.ResourceUpdater;
import lcmc.crm.ui.resource.RscDefaultsInfo;
import lcmc.crm.ui.resource.ServiceInfo;
import lcmc.crm.ui.resource.ServicesInfo;
import lcmc.drbd.domain.DRBDtestData;
import lcmc.drbd.domain.DrbdXml;
import lcmc.drbd.service.DRBD;
import lcmc.drbd.ui.DrbdGraph;
import lcmc.drbd.ui.resource.BlockDevInfo;
import lcmc.drbd.ui.resource.GlobalInfo;
import lcmc.drbd.ui.resource.ResourceInfo;
import lcmc.drbd.ui.resource.VolumeInfo;
import lcmc.event.NetworkChangedEvent;
import lcmc.host.domain.Host;
import lcmc.host.ui.ClusterHostsInfo;
import lcmc.host.ui.HostBrowser;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.vm.domain.VmsXml;
import lcmc.vm.ui.resource.DomainInfo;
import lcmc.vm.ui.resource.HardwareInfo;
import lcmc.vm.ui.resource.VMListInfo;
import org.apache.commons.collections15.keyvalue.MultiKey;
import org.apache.commons.collections15.map.LinkedMap;
import org.apache.commons.collections15.map.MultiKeyMap;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class holds cluster resource data in a tree. It shows panels that allow
 * to edit data of services etc.
 * Every resource has its Info object, that accessible through the tree view.
 */
@Named
public class ClusterBrowser extends Browser {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterBrowser.class);
    public static final ImageIcon REMOVE_ICON = Tools.createImageIcon(Tools.getDefault("ClusterBrowser.RemoveIcon"));
    public static final ImageIcon REMOVE_ICON_SMALL = Tools.createImageIcon(
                                                                Tools.getDefault("ClusterBrowser.RemoveIconSmall"));

    @Inject
    private Application application;
    @Inject
    private SwingUtils swingUtils;
    public static final Color SERVICE_STOPPED_FILL_PAINT = Tools.getDefaultColor("CRMGraph.FillPaintStopped");
    public static final String IDENT_4 = "    ";
    public static final String DRBD_RESOURCE_BOOL_TYPE_NAME = "boolean";
    public static final Collection<String> CRM_CLASSES = new ArrayList<String>();

    private static final String CRM_START_OPERATOR = "start";
    private static final String CRM_STOP_OPERATOR = "stop";
    private static final String CRM_STATUS_OPERATOR = "status";
    private static final String CRM_MONITOR_OPERATOR = "monitor";
    private static final String CRM_META_DATA_OPERATOR = "meta-data";
    private static final String CRM_VALIDATE_ALL_OPERATOR = "validate-all";
    public static final String CRM_PROMOTE_OPERATOR = "promote";
    public static final String CRM_DEMOTE_OPERATOR = "demote";

    private static final String CRM_DESC_PARAMETER = "description";
    private static final String CRM_INTERVAL_PARAMETER = "interval";
    private static final String CRM_TIMEOUT_PARAMETER = "timeout";
    private static final String CRM_START_DELAY_PARAMETER = "start-delay";
    private static final String CRM_DISABLED_PARAMETER = "disabled";
    private static final String CRM_ROLE_PARAMETER = "role";
    private static final String CRM_PREREQ_PARAMETER = "prereq";
    private static final String CRM_ON_FAIL_PARAMETER = "on-fail";

    public static final String[] CRM_OPERATIONS = {CRM_START_OPERATOR,
                                                   CRM_PROMOTE_OPERATOR,
                                                   CRM_DEMOTE_OPERATOR,
                                                   CRM_STOP_OPERATOR,
                                                   CRM_STATUS_OPERATOR,
                                                   CRM_MONITOR_OPERATOR,
                                                   CRM_META_DATA_OPERATOR,
                                                   CRM_VALIDATE_ALL_OPERATOR};
    public static final Collection<String> CRM_OPERATIONS_WITH_IGNORED_DEFAULT = new ArrayList<String>();
    /** All parameters for the crm operations, so that it is possible to create
     * arguments for up_rsc_full_ops. */
    public static final String[] HB_OPERATION_PARAM_LIST = {CRM_DESC_PARAMETER,
                                                            CRM_INTERVAL_PARAMETER,
                                                            CRM_TIMEOUT_PARAMETER,
                                                            CrmXml.PARAM_OCF_CHECK_LEVEL,
                                                            CRM_START_DELAY_PARAMETER,
                                                            CRM_DISABLED_PARAMETER,
                                                            CRM_ROLE_PARAMETER,
                                                            CRM_PREREQ_PARAMETER,
                                                            CRM_ON_FAIL_PARAMETER};
    public static final String STARTING_PTEST_TOOLTIP = Tools.getString("ClusterBrowser.StartingPtest");
    private static final String CLUSTER_STATUS_ERROR = "---start---\r\nerror\r\n\r\n---done---\r\n";
    public static final ImageIcon CLUSTER_ICON_SMALL = Tools.createImageIcon(
                                                              Tools.getDefault("ClusterBrowser.ClusterIconSmall"));
    /** String that appears as a tooltip in menu items if status was disabled.*/
    public static final String UNKNOWN_CLUSTER_STATUS_STRING = "unknown cluster status";
    private static final Collection<String> DEFAULT_OPERATION_PARAMS =
                                   new ArrayList<String>(Arrays.asList(CRM_TIMEOUT_PARAMETER, CRM_INTERVAL_PARAMETER));
    private static final String RESET_STRING = "---reset---\r\n";
    private static final int RESET_STRING_LEN = RESET_STRING.length();
    /** Match ...by-res/r0 or by-res/r0/0 from DRBD 8.4. */
    private static final Pattern DEV_DRBD_BY_RES_PATTERN = Pattern.compile("^/dev/drbd/by-res/([^/]+)(?:/(\\d+))?$");
    /** Hash that holds all hb classes with descriptions that appear in the
     * pull down menus. */
    public static final Map<String, String> CRM_CLASS_MENU = new HashMap<String, String>();
    static {
        CRM_CLASS_MENU.put(ResourceAgent.OCF_CLASS_NAME, "OCF Resource Agents");
        CRM_CLASS_MENU.put(ResourceAgent.HEARTBEAT_CLASS_NAME, "Heartbeat 1 RAs (deprecated)");
        CRM_CLASS_MENU.put(ResourceAgent.LSB_CLASS_NAME, "LSB Init Scripts");
        CRM_CLASS_MENU.put(ResourceAgent.STONITH_CLASS_NAME, "Stonith Devices");
        CRM_CLASS_MENU.put(ResourceAgent.SERVICE_CLASS_NAME, "Upstart/Systemd Scripts");
        CRM_CLASS_MENU.put(ResourceAgent.SYSTEMD_CLASS_NAME, "Systemd Scripts");
        CRM_CLASS_MENU.put(ResourceAgent.UPSTART_CLASS_NAME, "Upstart Scripts");
    }
    static {
        CRM_CLASSES.add(ResourceAgent.OCF_CLASS_NAME);
        CRM_CLASSES.add(ResourceAgent.HEARTBEAT_CLASS_NAME);
        for (final String c : ResourceAgent.SERVICE_CLASSES) {
            CRM_CLASSES.add(c);
        }
        CRM_CLASSES.add(ResourceAgent.STONITH_CLASS_NAME);
    }
    static {
        CRM_OPERATIONS_WITH_IGNORED_DEFAULT.add(CRM_STATUS_OPERATOR);
        CRM_OPERATIONS_WITH_IGNORED_DEFAULT.add(CRM_META_DATA_OPERATOR);
        CRM_OPERATIONS_WITH_IGNORED_DEFAULT.add(CRM_VALIDATE_ALL_OPERATOR);
    }

    @Inject
    private Provider<DomainInfo> domainInfoProvider;
    @Inject @Named("hbConnectionInfo")
    private Provider<HbConnectionInfo> connectionInfoProvider;
    @Inject
    private Provider<AvailableServiceInfo> availableServiceInfoProvider;
    @Inject
    private Provider<DrbdXml> drbdXmlProvider;
    @Inject
    private Provider<ClusterStatus> clusterStatusProvider;
    @Resource(name="categoryInfo")
    private CategoryInfo networksCategory;
    @Inject
    private Provider<ResourceAgentClassInfo> resourceAgentClassInfoProvider;
    @Inject
    private AvailableServicesInfo availableServicesInfo;
    @Inject
    private Provider<CRMInfo> crmInfoProvider;
    @Inject
    private Provider<VmsXml> vmsXmlProvider;
    @Inject
    private TreeMenuController treeMenuController;
    @Inject
    private ClusterEventBus clusterEventBus;
    @Inject
    private NetworkService networkService;
    @Inject
    private NetworkFactory networkFactory;
    @Inject
    private Provider<ResourceUpdater> resourceUpdaterProvider;

    public static String getClassMenuName(final String cl) {
        final String name = CRM_CLASS_MENU.get(cl);
        if (name == null) {
            return Tools.ucfirst(cl) + " scripts";
        }
        return name;
    }

    private Cluster cluster;
    private DefaultMutableTreeNode clusterHostsNode;
    private DefaultMutableTreeNode networksNode;
    private DefaultMutableTreeNode availableServicesNode;
    private DefaultMutableTreeNode crmNode;
    private DefaultMutableTreeNode servicesNode;
    private DefaultMutableTreeNode drbdNode;
    private DefaultMutableTreeNode vmsNode = null;

    /** name (hb type) + id to service info hash. */
    private final Map<String, Map<String, ServiceInfo>> nameToServiceInfoHash =
                                new TreeMap<String, Map<String, ServiceInfo>>(String.CASE_INSENSITIVE_ORDER);
    private final Lock mNameToServiceLock = new ReentrantLock();
    private final Lock mDrbdResHashLock = new ReentrantLock();
    private final Map<String, ResourceInfo> drbdResourceNameHash = new HashMap<String, ResourceInfo>();
    private final Lock mDrbdDevHashLock = new ReentrantLock();
    /** DRBD resource device string to drbd resource info hash. */
    private final Map<String, VolumeInfo> drbdDeviceHash = new HashMap<String, VolumeInfo>();
    private final Lock mHeartbeatIdToService = new ReentrantLock();
    /** Heartbeat id to service info hash. */
    private final Map<String, ServiceInfo> heartbeatIdToServiceInfo = new HashMap<String, ServiceInfo>();
    @Inject
    private CrmGraph crmGraph;
    @Inject
    private DrbdGraph drbdGraph;
    private ClusterStatus clusterStatus;
    @Inject
    private CrmXml crmXml;
    @Inject
    private Access access;
    private DrbdXml drbdXml;
    private final ReadWriteLock mVmsLock = new ReentrantReadWriteLock();
    private final Lock mVmsReadLock = mVmsLock.readLock();
    private final Lock mVmsWriteLock = mVmsLock.writeLock();
    private final Lock mVmsUpdateLock = new ReentrantLock();
    private final Map<Host, VmsXml> vmsXML = new HashMap<Host, VmsXml>();
    private DRBDtestData drbdtestData;
    private boolean drbdStatusCanceledByUser = false;
    /** Whether hb status was canceled by user. */
    private boolean crmStatusCanceledByUser = false;
    private final Lock mPtestLock = new ReentrantLock();
    private final Lock mDrbdTestDataLock = new ReentrantLock();
    private volatile boolean serverStatusCanceled = false;
    private Host lastDcHostDetected = null;
    /** dc host as reported by crm. */
    private Host dcHostReportedByCrm = null;
    private ClusterViewPanel clusterViewPanel = null;
    /** Map to ResourceAgentClassInfo. */
    private final Map<String, ResourceAgentClassInfo> classInfoMap = new HashMap<String, ResourceAgentClassInfo>();
    /** Map from ResourceAgent to AvailableServicesInfo. */
    private final Map<ResourceAgent, AvailableServiceInfo> availableServiceMap =
                                                               new HashMap<ResourceAgent, AvailableServiceInfo>();
    @Inject
    private ClusterHostsInfo clusterHostsInfo;
    @Inject
    private ServicesInfo servicesInfo;
    private RscDefaultsInfo rscDefaultsInfo = null;
    @Inject
    private Provider<RscDefaultsInfo> rscDefaultsInfoProvider;
    private final Lock mCrmStatusLock = new ReentrantLock();
    private final Map<String, List<String>> crmOperationParams = new LinkedHashMap<String, List<String>>();
    /** Not advanced operations. */
    private final MultiKeyMap<String, Integer> notAdvancedOperations = MultiKeyMap.decorate(
                                                                     new LinkedMap<MultiKey<String>, Integer>());
    private final Map<Host, String> hostDrbdParameters = new HashMap<Host, String>();
    private DefaultMutableTreeNode treeTop;
    @Inject
    private MainData mainData;
    @Inject
    private ProgressIndicator progressIndicator;
    @Inject
    private GlobalInfo globalInfo;

    @Inject
    private VMListInfo vmListInfo;

    public void init(final Cluster cluster) {
        this.cluster = cluster;
        clusterEventBus.register(this);
        crmGraph.initGraph(this);
        drbdGraph.initGraph(this);
        globalInfo.init(Tools.getString("ClusterBrowser.Drbd"), this);
        treeTop = treeMenuController.createMenuTreeTop();
    }

    private void initOperations() {
        notAdvancedOperations.put(CRM_START_OPERATOR, CRM_TIMEOUT_PARAMETER, 1);
        notAdvancedOperations.put(CRM_STOP_OPERATOR, CRM_TIMEOUT_PARAMETER, 1);
        notAdvancedOperations.put(CRM_MONITOR_OPERATOR, CRM_TIMEOUT_PARAMETER, 1);
        notAdvancedOperations.put(CRM_MONITOR_OPERATOR, CRM_INTERVAL_PARAMETER, 1);
        notAdvancedOperations.put(CRM_MONITOR_OPERATOR, CrmXml.PARAM_OCF_CHECK_LEVEL, 1);

        crmOperationParams.put(CRM_START_OPERATOR,
                               new ArrayList<String>(Arrays.asList(CRM_TIMEOUT_PARAMETER, CRM_INTERVAL_PARAMETER)));
        crmOperationParams.put(CRM_STOP_OPERATOR,
                               new ArrayList<String>(Arrays.asList(CRM_TIMEOUT_PARAMETER, CRM_INTERVAL_PARAMETER)));
        crmOperationParams.put(CRM_META_DATA_OPERATOR,
                               new ArrayList<String>(Arrays.asList(CRM_TIMEOUT_PARAMETER, CRM_INTERVAL_PARAMETER)));
        crmOperationParams.put(CRM_VALIDATE_ALL_OPERATOR,
                               new ArrayList<String>(Arrays.asList(CRM_TIMEOUT_PARAMETER, CRM_INTERVAL_PARAMETER)));

        crmOperationParams.put(CRM_STATUS_OPERATOR,
                               new ArrayList<String>(Arrays.asList(CRM_TIMEOUT_PARAMETER, CRM_INTERVAL_PARAMETER)));

        // TODO: need two monitors for role='Slave' and 'Master' in
        // master/slave resources
        final Host dcHost = getDCHost();
        if (Tools.versionBeforePacemaker(dcHost)) {
            crmOperationParams.put(CRM_MONITOR_OPERATOR,
                                   new ArrayList<String>(Arrays.asList(CRM_TIMEOUT_PARAMETER,
                                                                       CRM_INTERVAL_PARAMETER,
                                                                       CrmXml.PARAM_OCF_CHECK_LEVEL)));
        } else {
            crmOperationParams.put(CRM_MONITOR_OPERATOR,
                                   new ArrayList<String>(Arrays.asList(CRM_TIMEOUT_PARAMETER,
                                                                       CRM_INTERVAL_PARAMETER,
                                                                       CrmXml.PARAM_OCF_CHECK_LEVEL,
                                                                       CRM_START_DELAY_PARAMETER)));
        }
        crmOperationParams.put(CRM_PROMOTE_OPERATOR,
                new ArrayList<String>(Arrays.asList(CRM_TIMEOUT_PARAMETER, CRM_INTERVAL_PARAMETER)));
        crmOperationParams.put(CRM_DEMOTE_OPERATOR,
                               new ArrayList<String>(Arrays.asList(CRM_TIMEOUT_PARAMETER, CRM_INTERVAL_PARAMETER)));
    }

    public Collection<String> getCrmOperationParams(final String operation) {
        final List<String> params = crmOperationParams.get(operation);
        if (params == null) {
            return DEFAULT_OPERATION_PARAMS;
        } else {
            return params;
        }
    }

    public boolean isCrmOperationAdvanced(final String operation, final String param) {
        if (!crmOperationParams.containsKey(operation)) {
            return !CRM_TIMEOUT_PARAMETER.equals(param);
        }
        return !notAdvancedOperations.containsKey(operation, param);
    }

    void setClusterViewPanel(final ClusterViewPanel clusterViewPanel) {
        this.clusterViewPanel = clusterViewPanel;
    }

    public ClusterViewPanel getClusterViewPanel() {
        return clusterViewPanel;
    }

    public Host[] getClusterHosts() {
        return cluster.getHostsArray();
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setRightComponentInView(final Info component) {
        clusterViewPanel.setRightComponentInView(this, component);
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
        for (final Host host : hosts) {
            host.saveGraphPositions(positions);
        }
    }

    public CrmGraph getCrmGraph() {
        return crmGraph;
    }

    public DrbdGraph getDrbdGraph() {
        return drbdGraph;
    }

    public boolean allHostsWithoutClusterStatus() {
       boolean hostsDown = true;
       final Host[] hosts = cluster.getHostsArray();
       for (final Host host : hosts) {
           if (host.isCrmStatusOk()) {
               hostsDown = false;
               break;
           }
       }
       return hostsDown;
    }

    public boolean atLeastOneDrbddiskConfigured() {
        mHeartbeatIdToServiceLock();
        for (final Map.Entry<String, ServiceInfo>serviceInfoEntry : heartbeatIdToServiceInfo.entrySet()) {
            final ServiceInfo si = serviceInfoEntry.getValue();
            if (si.getResourceAgent().isDrbddisk()) {
                mHeartbeatIdToServiceUnlock();
                return true;
            }
        }
        mHeartbeatIdToServiceUnlock();
        return false;
    }

    public boolean isOneLinbitDrbdRaConfigured() {
        mHeartbeatIdToServiceLock();
        for (final Map.Entry<String, ServiceInfo> serviceInfoEntry : heartbeatIdToServiceInfo.entrySet()) {
            final ServiceInfo si = serviceInfoEntry.getValue();
            if (si.getResourceAgent().isLinbitDrbd()) {
                mHeartbeatIdToServiceUnlock();
                return true;
            }
        }
        mHeartbeatIdToServiceUnlock();
        return false;
    }

    void addVmsNode() {
        if (vmsNode == null) {
            vmListInfo.init(Tools.getString("ClusterBrowser.VMs"), this);
            vmsNode = treeMenuController.createMenuItem(treeTop, vmListInfo);
            treeMenuController.reloadNode(treeTop, true);
        }
    }

    /** Initializes cluster resources for cluster view. */
    void initClusterBrowser() {
        LOG.debug1("initClusterBrowser: start");
        /* hosts */
        clusterHostsInfo.init(Tools.getString("ClusterBrowser.ClusterHosts"), this);
        clusterHostsNode = treeMenuController.createMenuItem(treeTop, clusterHostsInfo);

        /* networks */
        networksCategory.init(Tools.getString("ClusterBrowser.Networks"), this);
        networksNode = treeMenuController.createMenuItem(treeTop, networksCategory);
        updateCommonNetworks(networkService.getCommonNetworks(cluster));

        /* drbd */
        drbdNode = treeMenuController.createMenuItem(treeTop, globalInfo);

        /* CRM */
        final CRMInfo crmInfo = crmInfoProvider.get();
        crmInfo.init(Tools.getString("ClusterBrowser.ClusterManager"), this);
        crmNode = treeMenuController.createMenuItem(treeTop, crmInfo);

        /* available services */
        availableServicesInfo.init(Tools.getString("ClusterBrowser.availableServices"), this);
        availableServicesNode = treeMenuController.createMenuItem(crmNode, availableServicesInfo);

        /* resource defaults */
        rscDefaultsInfo = rscDefaultsInfoProvider.get();
        rscDefaultsInfo.init("rsc_defaults", this);
        /* services */
        servicesInfo.init(Tools.getString("ClusterBrowser.Services"), this);
        servicesNode = treeMenuController.createMenuItem(crmNode, servicesInfo);
        addVmsNode();
        treeMenuController.selectPath(new Object[]{treeTop, crmNode});
        addDrbdProxyNodes();
        LOG.debug1("initClusterBrowser: end");
    }

    void addDrbdProxyNodes() {
        final Set<Host> clusterHosts = getCluster().getHosts();
        for (final Host pHost : getCluster().getProxyHosts()) {
            if (!clusterHosts.contains(pHost)) {
                globalInfo.addProxyHostNode(pHost);
            }
        }
    }

    void updateClusterResources(final Host[] clusterHosts) {
        LOG.debug1("start: update cluster resources");

        /* cluster hosts */
        treeMenuController.removeChildren(clusterHostsNode);
        for (final Host clusterHost : clusterHosts) {
            final HostBrowser hostBrowser = clusterHost.getBrowser();
            final DefaultMutableTreeNode resource = hostBrowser.getTreeTop();
            treeMenuController.addChild(clusterHostsNode, resource);
            crmGraph.addHost(hostBrowser.getHostInfo());
        }

        treeMenuController.reloadNode(clusterHostsNode, false);

        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                crmGraph.scale();
            }
        });
        updateHeartbeatDrbdThread();
        LOG.debug1("end: update cluster resources");
    }

    /** Starts everything. */
    private void updateHeartbeatDrbdThread() {
        LOG.debug("updateHeartbeatDrbdThread: load cluster");
        final Thread tt = new Thread(new Runnable() {
            @Override
            public void run() {
                final Host[] hosts = cluster.getHostsArray();
                for (final Host host : hosts) {
                    host.getHostParser().waitForServerStatusLatch();
                    progressIndicator.stopProgressIndicator(
                            host.getName(),
                            Tools.getString("ClusterBrowser.UpdatingServerInfo"));
                }
                swingUtils.invokeInEdt(new Runnable() {
                    @Override
                    public void run() {
                        getClusterViewPanel().setDisabledDuringLoad(false);
                        highlightServices();
                    }
                });
            }
        });
        tt.start();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final Host[] hosts = cluster.getHostsArray();
                swingUtils.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        for (final Host host : hosts) {
                            final HostBrowser hostBrowser = host.getBrowser();
                            drbdGraph.addHost(hostBrowser.getHostDrbdInfo());
                        }
                    }
                });
                int notConnectedCount = 0;
                Host firstHost = null;
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
                        } catch (final InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        final boolean ok =
                                 cluster.connect(null, notConnectedCount < 1, notConnectedCount + 1);
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
                crmXml.init(firstHost, getServicesInfo());
                final ClusterStatus newClusterStatus = clusterStatusProvider.get();
                newClusterStatus.init(firstHost, crmXml);
                clusterStatus = newClusterStatus;
                initOperations();
                drbdXml = drbdXmlProvider.get();
                drbdXml.init(cluster.getHostsArray(), hostDrbdParameters);
                /* available services */
                final String clusterName = getCluster().getName();
                progressIndicator.startProgressIndicator(clusterName, Tools.getString("ClusterBrowser.HbUpdateResources"));

                updateAvailableServices();
                progressIndicator.stopProgressIndicator(clusterName, Tools.getString("ClusterBrowser.HbUpdateResources"));
                progressIndicator.startProgressIndicator(clusterName, Tools.getString("ClusterBrowser.DrbdUpdate"));
                progressIndicator.stopProgressIndicator(clusterName, Tools.getString("ClusterBrowser.DrbdUpdate"));
                cluster.getBrowser().startConnectionStatusOnAllHosts();
                cluster.getBrowser().startServerStatus();
                cluster.getBrowser().startDrbdStatusOnAllHosts();
                cluster.getBrowser().startCrmStatus();
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
        final CategoryInfo[] infosToUpdate = new CategoryInfo[]{clusterHostsInfo};
        while (true) {
            if (host.getHostParser().getWaitForServerStatusLatch()) {
                progressIndicator.startProgressIndicator(hostName, Tools.getString("ClusterBrowser.UpdatingServerInfo"));
            }

            host.setIsLoading();
            host.getHostParser().startHWInfoDaemon(infosToUpdate, new ResourceGraph[]{drbdGraph, crmGraph});
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
        swingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                drbdGraph.addHost(host.getBrowser().getHostDrbdInfo());
            }
        });
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                drbdGraph.scale();
            }
        });
        if (host.getHostParser().getWaitForServerStatusLatch()) {
            LOG.debug("updateServerStatus: " + host.getName() + " loading done");
        }
        host.getHostParser().serverStatusLatchDone();
        clusterHostsInfo.updateTable(CategoryInfo.MAIN_TABLE);
        for (final ResourceGraph graph : new ResourceGraph[]{drbdGraph, crmGraph}) {
            if (graph != null) {
                graph.repaint();
                graph.updatePopupMenus();
            }
        }
    }

    /** Updates VMs info. */
    public void periodicalVmsUpdate(final Host host) {
        final VmsXml newVmsXml = vmsXmlProvider.get();
        newVmsXml.init(host);
        if (newVmsXml.parseXml()) {
            vmsXmlPut(host, newVmsXml);
            updateVms();
        }
    }

    /** Updates VMs info. */
    public void periodicalVmsUpdate(final Host[] hosts) {
        periodicalVmsUpdate(Arrays.asList(hosts));
    }

    /** Updates VMs info. */
    public void periodicalVmsUpdate(final Iterable<Host> hosts) {
        boolean updated = false;
        for (final Host host : hosts) {
            final VmsXml newVmsXml = vmsXmlProvider.get();
            newVmsXml.init(host);
            if (newVmsXml.parseXml()) {
                vmsXmlPut(host, newVmsXml);
                updated = true;
            }
        }
        if (updated) {
            updateVms();
        }
    }

    /** Adds new vmsxml object to the hash. */
    public void vmsXmlPut(final Host host, final VmsXml newVmsXml) {
        mVmsWriteLock.lock();
        try {
            vmsXML.put(host, newVmsXml);
        } finally {
            mVmsWriteLock.unlock();
        }
    }

    public boolean isCancelServerStatus() {
        return serverStatusCanceled;
    }

    void startConnectionStatusOnAllHosts() {
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

    void startDrbdStatusOnAllHosts() {
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

    public void stopDrbdStatusOnAllHosts() {
        drbdStatusCanceledByUser = true;
        final Host[] hosts = cluster.getHostsArray();
        for (final Host host : hosts) {
            host.stopDrbdStatus();
        }
        for (final Host host : hosts) {
            host.waitForDrbdStatusFinish();
        }
    }

    void startDrbdStatus(final Host host) {
        final CountDownLatch firstTime = new CountDownLatch(1);
        host.setDrbdStatusOk(false);
        final String hostName = host.getName();
        /* now what we do if the status finished for the first time. */
        progressIndicator.startProgressIndicator( hostName, Tools.getString("ClusterBrowser.UpdatingDrbdStatus"));
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    firstTime.await();
                } catch (final InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                swingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        drbdGraph.scale();
                    }
                });
                progressIndicator.stopProgressIndicator(hostName, Tools.getString("ClusterBrowser.UpdatingDrbdStatus"));
            }
        });
        thread.start();

        drbdStatusCanceledByUser = false;
        while (true) {
            host.execDrbdStatusCommand(
                  new ExecCallback() {
                       @Override
                       public void done(final String ans) {
                           firstTime.countDown();
                           if (!host.isDrbdStatusOk()) {
                               host.setDrbdStatusOk(true);
                               drbdGraph.repaint();
                               LOG.debug1("startDrbdStatus: host: " + host.getName());
                               clusterHostsInfo.updateTable(ClusterHostsInfo.MAIN_TABLE);
                           }
                       }

                       @Override
                       public void doneError(final String answer, final int exitCode) {
                           firstTime.countDown();
                           LOG.debug1("startDrbdStatus: failed: " + host.getName() + " exit code: " + exitCode);
                           if (exitCode != 143 && exitCode != 100) {
                               // TODO: exit code is null -> 100 all of the
                               // sudden
                               /* was killed intentionally */
                               if (host.isDrbdStatusOk()) {
                                   host.setDrbdStatusOk(false);
                                   LOG.debug1("startDrbdStatus: host: " + host.getName());
                                   drbdGraph.repaint();
                                   clusterHostsInfo.updateTable(ClusterHostsInfo.MAIN_TABLE);
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
                       private final StringBuffer outputBuffer = new StringBuffer(300);
                       @Override
                       public void output(final String output) {
                           if ("--nm--".equals(output.trim())) {
                               if (host.isDrbdStatusOk()) {
                                   LOG.debug1("startDrbdStatus: host: " + host.getName());
                                   host.setDrbdStatusOk(false);
                                   drbdGraph.repaint();
                                   clusterHostsInfo.updateTable(ClusterHostsInfo.MAIN_TABLE);
                               }
                               firstTime.countDown();
                               return;
                           }
                           firstTime.countDown();
                           if (!host.isDrbdStatusOk()) {
                               LOG.debug1("startDrbdStatus: host: " + host.getName());
                               host.setDrbdStatusOk(true);
                               drbdGraph.repaint();
                               clusterHostsInfo.updateTable(ClusterHostsInfo.MAIN_TABLE);
                           }
                           outputBuffer.append(output);
                           String drbdConfig, event;
                           boolean drbdUpdate = false;
                           boolean eventUpdate = false;
                           do {
                               host.drbdStatusLock();
                               drbdConfig = host.getHostParser().getOutput("drbd", outputBuffer);
                               if (drbdConfig != null) {
                                   final DrbdXml newDrbdXml = drbdXmlProvider.get();
                                   newDrbdXml.init(cluster.getHostsArray(), hostDrbdParameters);
                                   newDrbdXml.update(drbdConfig);
                                   drbdXml = newDrbdXml;
                                   drbdUpdate = true;
                                   firstTime.countDown();
                               }
                               host.drbdStatusUnlock();
                               event = host.getHostParser().getOutput("event", outputBuffer);
                               if (event != null) {
                                   if (drbdXml.parseDrbdEvent(host.getName(), drbdGraph, event)) {
                                       host.setDrbdStatusOk(true);
                                       eventUpdate = true;
                                   }
                               }
                           } while (event != null || drbdConfig != null);
                           Tools.chomp(outputBuffer);
                           if (drbdUpdate) {
                               swingUtils.invokeLater(new Runnable() {
                                   @Override
                                   public void run() {
                                       globalInfo.setParameters();
                                       updateDrbdResources();
                                   }
                               });
                           }
                           if (eventUpdate) {
                               drbdGraph.repaint();
                               LOG.debug1("drbd status update: " + host.getName());
                               clusterHostsInfo.updateTable(ClusterHostsInfo.MAIN_TABLE);
                               firstTime.countDown();
                               final Thread thread = new Thread(
                                   new Runnable() {
                                       @Override
                                       public void run() {
                                           repaintSplitPane();
                                           drbdGraph.updatePopupMenus();
                                           swingUtils.invokeInEdt(
                                               new Runnable() {
                                                   @Override
                                                   public void run() {
                                                       treeMenuController.repaintMenuTree();
                                                   }
                                               }
                                           );
                                       }
                                   });
                               thread.start();
                           }
                       }
                   });
            host.waitForHostAndDrbd();
            host.waitForDrbdStatusFinish();
            if (drbdStatusCanceledByUser) {
                break;
            }
        }
    }

    public void stopCrmStatus() {
        crmStatusCanceledByUser = true;
        final Host[] hosts = cluster.getHostsArray();
        for (final Host host : hosts) {
            host.stopCrmStatus();
        }
    }

    public void stopServerStatus() {
        serverStatusCanceled = true;
        final Host[] hosts = cluster.getHostsArray();
        for (final Host host : hosts) {
            host.getHostParser().stopServerStatus();
        }
    }

    public boolean crmStatusFailed() {
        final Host[] hosts = cluster.getHostsArray();
        for (final Host host : hosts) {
            if (host.isCrmStatusOk()) {
                return false;
            }
        }
        return true;
    }

    /** Sets crm status (failed / not failed for every node). */
    void setCrmStatus() {
        final Host[] hosts = cluster.getHostsArray();
        for (final Host host : hosts) {
            final String online = clusterStatus.isOnlineNode(host.getName());
            if ("yes".equals(online)) {
                setCrmStatus(host, true);
            } else {
                setCrmStatus(host, false);
            }
        }
    }

    void startClStatusProgressIndicator(final String clusterName) {
        progressIndicator.startProgressIndicator(clusterName, Tools.getString("ClusterBrowser.HbUpdateStatus"));
    }

    void stopClStatusProgressIndicator(final String clusterName) {
        progressIndicator.stopProgressIndicator(clusterName, Tools.getString("ClusterBrowser.HbUpdateStatus"));
    }

    /** Sets status and checks if it changes and if it does some action will be
     * performed. */
    private void setCrmStatus(final Host host, final boolean status) {
        final boolean oldStatus = host.isCrmStatusOk();
        host.setCrmStatusOk(status);
        if (oldStatus != status) {
            treeMenuController.nodeChanged(servicesNode);
        }
    }

    public void parseClusterOutput(final String output,
                            final StringBuffer clusterStatusOutput,
                            final Host host,
                            final CountDownLatch firstTime,
                            final Application.RunMode runMode) {
        final ClusterStatus clusterStatus0 = this.clusterStatus;
        clStatusLock();
        if (crmStatusCanceledByUser || clusterStatus0 == null) {
            clStatusUnlock();
            firstTime.countDown();
            return;
        }
        if (output == null || "".equals(output)) {
            clusterStatus0.setOnlineNode(host.getName(), "no");
            setCrmStatus(host, false);
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
                final String e = clusterStatusOutput.substring(clusterStatusOutput.length() - 12);
                if (e.trim().equals("---done---")) {
                    final int i = clusterStatusOutput.lastIndexOf("---start---");
                    if (i >= 0) {
                        if (clusterStatusOutput.indexOf("is stopped") >= 0) {
                            /* TODO: heartbeat's not running. */
                        } else {
                            final String status = clusterStatusOutput.substring(i);
                            clusterStatusOutput.delete(0, clusterStatusOutput.length());
                            if (CLUSTER_STATUS_ERROR.equals(status)) {
                                final boolean oldStatus = host.isCrmStatusOk();
                                clusterStatus0.setOnlineNode(host.getName(), "no");
                                setCrmStatus(host, false);
                                if (oldStatus) {
                                   crmGraph.repaint();
                                }
                            } else {
                                if (clusterStatus0.parseStatus(status)) {
                                    LOG.debug1("processClusterOutput: host: " + host.getName());
                                    final ServicesInfo ssi = servicesInfo;
                                    rscDefaultsInfo.setParameters(clusterStatus0.getRscDefaultsValuePairs());
                                    ssi.setGlobalConfig(clusterStatus0);
                                    resourceUpdaterProvider.get().setAllResources(ssi, ssi.getBrowser(), clusterStatus0, runMode);
                                    if (firstTime.getCount() == 1) {
                                        /* one more time so that id-refs work.*/
                                        resourceUpdaterProvider.get().setAllResources(ssi, ssi.getBrowser(), clusterStatus0, runMode);
                                    }
                                    treeMenuController.repaintMenuTree();
                                    clusterHostsInfo.updateTable(ClusterHostsInfo.MAIN_TABLE);
                                }
                                final String online = clusterStatus0.isOnlineNode(host.getName());
                                if ("yes".equals(online)) {
                                    setCrmStatus(host, true);
                                    setCrmStatus();
                                } else {
                                    setCrmStatus(host, false);
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

    void startCrmStatus() {
        final CountDownLatch firstTime = new CountDownLatch(1);
        final String clusterName = getCluster().getName();
        startClStatusProgressIndicator(clusterName);
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    firstTime.await();
                } catch (final InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                if (crmStatusFailed()) {
                     progressIndicator.progressIndicatorFailed(clusterName, Tools.getString("ClusterBrowser.ClusterStatusFailed"));
                } else {
                    swingUtils.invokeLater(new Runnable() {
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
        crmStatusCanceledByUser = false;
        final Application.RunMode runMode = Application.RunMode.LIVE;
        while (true) {
            final Host host = getDCHost();
            if (host == null) {
                try {
                    Thread.sleep(5000);
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }
            //clStatusCanceled = false;
            host.execCrmStatusCommand(
                    new ExecCallback() {
                        @Override
                        public void done(final String answer) {
                            final String online = clusterStatus.isOnlineNode(host.getName());
                            setCrmStatus(host, "yes".equals(online));
                            firstTime.countDown();
                        }

                        @Override
                        public void doneError(final String answer, final int exitCode) {
                            if (firstTime.getCount() == 1) {
                                LOG.debug2("startClStatus: status failed: " + host.getName() + ", ec: " + exitCode);
                            }
                            clStatusLock();
                            clusterStatus.setOnlineNode(host.getName(), "no");
                            setCrmStatus(host, false);
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
                        private final StringBuffer clusterStatusOutput = new StringBuffer(300);

                        @Override
                        public void output(final String output) {
                            parseClusterOutput(output, clusterStatusOutput, host, firstTime, runMode);
                        }
                    });
            host.waitForCrmStatusFinish();
            if (crmStatusCanceledByUser) {
                break;
            }
            try {
                Thread.sleep(5000);
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Returns 'add service' list for menus. */
    public List<ResourceAgent> globalGetAddServiceList(final String cl) {
        return crmXml.getServices(cl);
    }

    /** Updates available services. */
    private void updateAvailableServices() {
        LOG.debug("updateAvailableServices: start");
        treeMenuController.removeChildren(availableServicesNode);
        for (final String crmClass : CRM_CLASSES) {
            final ResourceAgentClassInfo raci = resourceAgentClassInfoProvider.get();
            raci.init(crmClass, this);
            classInfoMap.put(crmClass, raci);
            final DefaultMutableTreeNode classNode = treeMenuController.createMenuItem(availableServicesNode, raci);
            for (final ResourceAgent resourceAgent : crmXml.getServices(crmClass)) {
                final AvailableServiceInfo availableService = availableServiceInfoProvider.get();
                availableService.init(resourceAgent, this);
                availableServiceMap.put(resourceAgent, availableService);
                treeMenuController.createMenuItem(classNode, availableService);
            }
        }
    }

    /** Updates VM nodes. */
    public void updateVms() {
        LOG.debug1("updateVMS: status update");
        final Collection<String> domainNames = new TreeSet<String>();
        for (final Host host : getClusterHosts()) {
            final VmsXml vmsXml = getVmsXml(host);
            if (vmsXml != null) {
                domainNames.addAll(vmsXml.getDomainNames());
            }
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove = new ArrayList<DefaultMutableTreeNode>();
        final Collection<DomainInfo> currentVMSVDIs = new ArrayList<DomainInfo>();

        mVmsUpdateLock.lock();
        boolean nodeChanged = false;
        if (vmsNode != null) {
            for (final Object info : treeMenuController.nodesToInfos(vmsNode.children())) {
                final DomainInfo domainInfo = (DomainInfo) info;
                if (domainNames.contains(domainInfo.toString())) {
                    /* keeping */
                    currentVMSVDIs.add(domainInfo);
                    domainNames.remove(domainInfo.toString());
                    domainInfo.updateParameters(); /* update old */
                } else {
                    if (!domainInfo.getResource().isNew()) {
                        /* remove not existing vms */
                        nodesToRemove.add(domainInfo.getNode());
                        domainInfo.setNode(null);
                        nodeChanged = true;
                    }
                }
            }
        }

        treeMenuController.removeFromParent(nodesToRemove);
        if (vmsNode == null) {
            mVmsUpdateLock.unlock();
            return;
        }
        for (final String domainName : domainNames) {
            int i = 0;
            for (final Object info : treeMenuController.nodesToInfos(vmsNode.children())) {
                final DomainInfo domainInfo = (DomainInfo) info;
                final String name = domainInfo.getName();
                if (domainName != null && name != null && domainName.compareTo(domainInfo.getName()) < 0) {
                    break;
                }
                i++;
            }
            /* add new vms nodes */
            final DomainInfo domainInfo = domainInfoProvider.get();
            domainInfo.init(domainName, this);
            currentVMSVDIs.add(domainInfo);
            treeMenuController.createMenuItem(vmsNode, domainInfo, i);
            domainInfo.updateParameters();
            nodeChanged = true;
        }
        mVmsUpdateLock.unlock();
        if (nodeChanged) {
            treeMenuController.reloadNode(vmsNode, false);
        }
        for (final ServiceInfo si : getExistingServiceList(null)) {
            final DomainInfo vmsvdi = si.connectWithVMS();
            if (vmsvdi != null) {
                /* keep the not connected ones.*/
                currentVMSVDIs.remove(vmsvdi);
            }
        }
        for (final DomainInfo vmsvdi : currentVMSVDIs) {
            vmsvdi.setUsedByCRM(false);
        }
        final VMListInfo vmsi = (VMListInfo) vmsNode.getUserObject();
        if (vmsi != null) {
            vmsi.updateTable(VMListInfo.MAIN_TABLE);
        }
    }

    public VMListInfo getVmsInfo() {
        return (VMListInfo) vmsNode.getUserObject();
    }

    public void updateDrbdResources() {
        swingUtils.isSwingThread();
        drbdStatusLock();
        final DrbdXml dxml = drbdXml;
        if (dxml == null) {
            drbdStatusUnlock();
            return;
        }
        final Application.RunMode runMode = Application.RunMode.LIVE;
        boolean atLeastOneAdded = false;
        for (final Table.Cell<String, String, String> cell : dxml.getResourceDeviceMap().cellSet()) {
            final String resName = cell.getRowKey();
            final String volumeNr = cell.getColumnKey();
            final String drbdDev = cell.getValue();

            final Map<String, String> hostDiskMap = dxml.getHostDiskMap(resName, volumeNr);
            if (hostDiskMap == null) {
                continue;
            }
            BlockDevInfo bd1 = null;
            BlockDevInfo bd2 = null;
            for (final String hostName : hostDiskMap.keySet()) {
                if (!cluster.contains(hostName)) {
                    continue;
                }
                final String disk = hostDiskMap.get(hostName);
                final BlockDevInfo bdi = drbdGraph.findBlockDevInfo(hostName, disk);
                if (bdi == null) {
                    if (getDrbdDeviceHash().containsKey(disk)) {
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
                ResourceInfo dri = getDrbdResourceNameHash().get(resName);
                putDrbdResHash();
                final List<BlockDevInfo> bdis = new ArrayList<BlockDevInfo>(Arrays.asList(bd1, bd2)); if (dri == null) {
                    dri = globalInfo.addDrbdResource(resName, VolumeInfo.getHostsFromBlockDevices(bdis), runMode);
                    atLeastOneAdded = true;
                }
                VolumeInfo dvi = dri.getDrbdVolumeInfo(volumeNr);
                if (dvi == null) {
                    dvi = globalInfo.addDrbdVolume(dri, volumeNr, drbdDev, bdis, runMode);
                    atLeastOneAdded = true;
                }
                dri.setParameters();
                dvi.setParameters();
                final ResourceInfo dri0 = dri;
                dri0.getInfoPanel();
            }
        }
        //TODO: it would remove it during drbd wizards
        //killRemovedVolumes(dxml.getResourceDeviceMap());
        drbdStatusUnlock();
        if (atLeastOneAdded) {
            globalInfo.getInfoPanel();
            globalInfo.setAllApplyButtons();
            globalInfo.reloadDRBDResourceComboBoxes();
            drbdGraph.scale();
        }
    }

    @Subscribe
    public void onUpdateCommonNetworks(final NetworkChangedEvent event) {
        if (cluster != event.getCluster()) {
            return;
        }
        updateCommonNetworks(event.getCommonNetworks());
    }

    private void updateCommonNetworks(final Collection<Network> networks) {
        treeMenuController.removeChildren(networksNode);
        for (final Network network : networks) {
            final NetworkPresenter networkPresenter = networkFactory.createPresenter(cluster, network);
            treeMenuController.createMenuItem(networksNode, networkPresenter);
        }
        treeMenuController.reloadNode(networksNode, false);
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
    public boolean isStandby(final Host host, final Application.RunMode runMode) {
        final ClusterStatus cl = clusterStatus;
        if (cl == null) {
            return false;
        }
        final String standby = cl.getNodeParameter(host.getName().toLowerCase(Locale.US), "standby", runMode);
        return "on".equals(standby) || "true".equals(standby);
    }

    /** Returns whether the host is the real dc host as reported by dc. */
    public boolean isRealDcHost(final Host host) {
        return host.equals(dcHostReportedByCrm);
    }

    /**
     * Finds and returns DC host.
     * TODO: document what's going on.
     */
    public Host getDCHost() {
        String dc = null;
        final ClusterStatus cl = clusterStatus;
        if (cl != null) {
            dc = cl.getDC();
        }
        final List<Host> hosts = new ArrayList<Host>();
        int lastHostIndex = 0;
        int i = 0;
        Host dcHost = null;
        for (final Host host : getClusterHosts()) {
            if (host == lastDcHostDetected) {
                lastHostIndex = i;
            }
            if (host.getName().equals(dc)
                && host.isCrmStatusOk()
                && !host.getHostParser().isCommLayerStarting()
                && !host.getHostParser().isCommLayerStopping()
                && (host.getHostParser().isHeartbeatRunning() || host.getHostParser().isCorosyncRunning() || host.getHostParser().isOpenaisRunning())) {
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
                    && (hosts.get(ix).getHostParser().isHeartbeatRunning()
                        || hosts.get(ix).getHostParser().isCorosyncRunning()
                        || hosts.get(ix).getHostParser().isOpenaisRunning())) {
                    lastDcHostDetected = hosts.get(ix);
                    break;
                }
            } while (ix != lastHostIndex);
            dcHost = lastDcHostDetected;
            dcHostReportedByCrm = null;
            if (dcHost == null) {
                dcHost = hosts.get(0);
            }
        } else {
            dcHostReportedByCrm = dcHost;
        }

        lastDcHostDetected = dcHost;
        return dcHost;
    }

    public void drbdStatusLock() {
        for (final Host host : getClusterHosts()) {
            host.drbdStatusLock();
        }
    }

    public void drbdStatusUnlock() {
        final Host[] hosts = getClusterHosts();
        for (int i = hosts.length - 1; i >= 0; i--) {
            hosts[i].drbdStatusUnlock();
        }
    }

    public void vmStatusLock() {
        for (final Host host : getClusterHosts()) {
            host.vmStatusLock();
        }
    }

    public void vmStatusUnlock() {
        final Host[] hosts = getClusterHosts();
        for (int i = hosts.length - 1; i >= 0; i--) {
            hosts[i].vmStatusUnlock();
        }
    }


    void highlightDrbd() {
        treeMenuController.reloadNode(drbdNode, true);
    }

    public void highlightServices() {
        if (getClusterViewPanel().isDisabledDuringLoad()) {
            return;
        }
        treeMenuController.selectPath(new Object[]{treeTop, crmNode, servicesNode});
    }

    public ServiceInfo getServiceInfoFromCRMId(final String crmId) {
        mHeartbeatIdToServiceLock();
        final ServiceInfo serviceInfo = heartbeatIdToServiceInfo.get(crmId);
        mHeartbeatIdToServiceUnlock();
        return serviceInfo;
    }

    public boolean isCrmId(final String crmId) {
        mHeartbeatIdToServiceLock();
        final boolean ret = heartbeatIdToServiceInfo.containsKey(crmId);
        mHeartbeatIdToServiceUnlock();
        return ret;
    }

    public void mHeartbeatIdToServiceLock() {
        mHeartbeatIdToService.lock();
    }

    public void mHeartbeatIdToServiceUnlock() {
        mHeartbeatIdToService.unlock();
    }

    /** Returns heartbeatIdToServiceInfo hash. You have to lock it. */
    public Map<String, ServiceInfo> getHeartbeatIdToServiceInfo() {
        return heartbeatIdToServiceInfo;
    }

    /** Returns ServiceInfo object identified by name and id. */
    public ServiceInfo getServiceInfoFromId(final String name, final String id) {
        lockNameToServiceInfo();
        final Map<String, ServiceInfo> idToInfoHash = nameToServiceInfoHash.get(name);
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

    public void lockNameToServiceInfo() {
        mNameToServiceLock.lock();
    }

    public void unlockNameToServiceInfo() {
        mNameToServiceLock.unlock();
    }

    /** Returns 'existing service' list for graph popup menu. */
    public List<ServiceInfo> getExistingServiceList(final ServiceInfo p) {
        final List<ServiceInfo> existingServiceList = new ArrayList<ServiceInfo>();
        lockNameToServiceInfo();
        for (final String name : nameToServiceInfoHash.keySet()) {
            final Map<String, ServiceInfo> idHash = nameToServiceInfoHash.get(name);
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
                if (p == null || !getCrmGraph().existsInThePath(sigi, p)) {
                    existingServiceList.add(si);
                }
            }
        }
        unlockNameToServiceInfo();
        return existingServiceList;
    }


    public void removeFromServiceInfoHash(final ServiceInfo serviceInfo) {
        // TODO: it comes here twice sometimes
        final Service service = serviceInfo.getService();
        lockNameToServiceInfo();
        final Map<String, ServiceInfo> idToInfoHash = nameToServiceInfoHash.get(service.getName());
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
    public Map<String, ServiceInfo> getNameToServiceInfoHash(final String name) {
        return nameToServiceInfoHash.get(name);
    }

    /**
     * Adds heartbeat id from service to the list. If service does not have an
     * id it is generated.
     */
    public void addToHeartbeatIdList(final ServiceInfo si) {
        final String id = si.getService().getId();
        String pmId = si.getService().getCrmId();
        if (pmId == null) {
            if (Application.PACEMAKER_GROUP_NAME.equals(si.getService().getName())) {
                pmId = Service.GRP_ID_PREFIX;
            } else if (Application.PM_CLONE_SET_NAME.equals(si.getService().getName())
                       || Application.PM_MASTER_SLAVE_SET_NAME.equals(si.getService().getName())) {
                if (si.getService().isMaster()) {
                    pmId = Service.MS_ID_PREFIX;
                } else {
                    pmId = Service.CL_ID_PREFIX;
                }
            } else if (si.getResourceAgent().isStonith())  {
                pmId = Service.STONITH_ID_PREFIX + si.getService().getName() + '_';
            } else {
                pmId = Service.RES_ID_PREFIX + si.getService().getName() + '_';
            }
            final String newPmId;
            if (id == null) {
                /* first time, no pm id is set */
                newPmId = pmId + '1';
                si.getService().setId("1");
            } else {
                newPmId = pmId + id;
                si.getService().setCrmId(newPmId);
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
    @Deprecated
    public void resetFilesystems() {
        mHeartbeatIdToServiceLock();
        for (final String hbId : heartbeatIdToServiceInfo.keySet()) {
            final ServiceInfo si = heartbeatIdToServiceInfo.get(hbId);
            if (si.getName().equals("Filesystem")) {
                si.setInfoPanel(null);
            }
        }
        mHeartbeatIdToServiceUnlock();
    }

    /** Check if the id exists for the service already, if so add _$index
     * to it. */
    public String getFreeId(final String serviceName, final String id) {
        if (id == null) {
            return null;
        }
        String newId = id;
        lockNameToServiceInfo();
        try {
            final Map<String, ServiceInfo> idToInfoHash = nameToServiceInfoHash.get(serviceName);
            if (idToInfoHash != null) {
                int index = 2;
                while (idToInfoHash.containsKey(newId)) {
                    newId = id + '_' + index;
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
        Map<String, ServiceInfo> idToInfoHash = nameToServiceInfoHash.get(service.getName());
        String csPmId = null;
        final ServiceInfo cs = serviceInfo.getContainedService();
        if (cs != null) {
            csPmId = cs.getService().getName() + '_' + cs.getService().getId();
        }
        if (idToInfoHash == null) {
            idToInfoHash = new TreeMap<String, ServiceInfo>(String.CASE_INSENSITIVE_ORDER);
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
                    final Pattern p;
                    if (csPmId == null) {
                        p = Pattern.compile("^(\\d+)$");
                    } else {
                        /* ms */
                        p = Pattern.compile('^' + csPmId + "_(\\d+)$");
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
                        } catch (final NumberFormatException nfe) {
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
                        service.setIdAndCrmId(csPmId + '_' + Integer.toString(index + 1));
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
        return application.confirmDialog(
           Tools.getString("ClusterBrowser.confirmHbDrbd.Title"),
           Tools.getString("ClusterBrowser.confirmHbDrbd.Description"),
           Tools.getString("ClusterBrowser.confirmHbDrbd.Yes"),
           Tools.getString("ClusterBrowser.confirmHbDrbd.No"));
    }

    public boolean isDrbddiskRAPreferred() {
        return Tools.versionBeforePacemaker(getDCHost());
    }

    /**
     * Returns true if user wants the linbit:drbd even, for old version of
     * hb or simply true if we have pacemaker.
     */
    public boolean linbitDrbdConfirmDialog() {
        if (isDrbddiskRAPreferred()) {
            final String desc = Tools.getString("ClusterBrowser.confirmLinbitDrbd.Description");

            final Host dcHost = getDCHost();
            final String hbV = dcHost.getHostParser().getHeartbeatVersion();
            return application.confirmDialog(Tools.getString("ClusterBrowser.confirmLinbitDrbd.Title"),
                                       desc.replaceAll("@VERSION@", hbV),
                                       Tools.getString("ClusterBrowser.confirmLinbitDrbd.Yes"),
                                       Tools.getString("ClusterBrowser.confirmLinbitDrbd.No"));
        }
        return true;
    }

    void startHeartbeatsOnAllNodes() {
        final Host[] hosts = cluster.getHostsArray();
        for (final Host host : hosts) {
            Heartbeat.startHeartbeat(host);
        }
    }

    /**
     * This is called from crm graph.
     */
    public HbConnectionInfo getNewHbConnectionInfo() {
        final HbConnectionInfo connectionInfo = connectionInfoProvider.get();
        connectionInfo.init(this);
        //connectionInfo.getInfoPanel();
        return connectionInfo;
    }

    public ClusterStatus getClusterStatus() {
        return clusterStatus;
    }

    public DRBDtestData getDRBDtestData() {
        drbdTestDataLockAcquire();
        final DRBDtestData dtd = drbdtestData;
        drbdTestDataLockRelease();
        return dtd;
    }

    public void setDRBDtestData(final DRBDtestData drbdtestData) {
        drbdTestDataLockAcquire();
        this.drbdtestData = drbdtestData;
        drbdTestDataLockRelease();
    }

    public void ptestLockAcquire() {
        mPtestLock.lock();
    }

    public void ptestLockRelease() {
        mPtestLock.unlock();
    }

    protected void drbdTestDataLockAcquire() {
        mDrbdTestDataLock.lock();
    }

    protected void drbdTestDataLockRelease() {
        mDrbdTestDataLock.unlock();
    }

    /** Returns xml from cluster manager. */
    public CrmXml getCrmXml() {
        return crmXml;
    }

    public DrbdXml getDrbdXml() {
        return drbdXml;
    }

    public void setDrbdXml(final DrbdXml drbdXml) {
        this.drbdXml = drbdXml;
    }

    public DefaultMutableTreeNode getDrbdNode() {
        return drbdNode;
    }

    public TreeNode getClusterHostsNode() {
        return clusterHostsNode;
    }

    public DefaultMutableTreeNode getServicesNode() {
        return servicesNode;
    }

    public TreeNode getNetworksNode() {
        return networksNode;
    }

    /**
     * Returns a hash from drbd device to drbd volume info. putDrbdDevHash
     * must follow after you're done. */
    public Map<String, VolumeInfo> getDrbdDeviceHash() {
        mDrbdDevHashLock.lock();
        return drbdDeviceHash;
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
    public VolumeInfo getDrbdVolumeFromDev(final CharSequence dev) {
        if (dev == null) {
            return null;
        }
        final Matcher m = DEV_DRBD_BY_RES_PATTERN.matcher(dev);
        if (m.matches()) {
            final String res = m.group(1);
            final String vol;
            if (m.groupCount() > 2) {
                vol = m.group(2);
            } else {
                vol = "0";
            }
            final ResourceInfo dri = getDrbdResourceNameHash().get(res);
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
    public Map<String, ResourceInfo> getDrbdResourceNameHash() {
        mDrbdResHashLock.lock();
        return drbdResourceNameHash;
    }

    public void putDrbdResHash() {
        mDrbdResHashLock.unlock();
    }

    /** Returns (shallow) copy of all drbdresource info objects. */
    public Iterable<ResourceInfo> getDrbdResHashValues() {
        final Iterable<ResourceInfo> values = new ArrayList<ResourceInfo>(getDrbdResourceNameHash().values());
        putDrbdResHash();
        return values;
    }

    public void reloadAllComboBoxes(final ServiceInfo exceptThisOne) {
        swingUtils.invokeInEdt(new Runnable() {
            @Override
            public void run() {
                lockNameToServiceInfo();
                for (final String name : nameToServiceInfoHash.keySet()) {
                    final Map<String, ServiceInfo> idToInfoHash = nameToServiceInfoHash.get(name);
                    for (final Map.Entry<String, ServiceInfo> serviceEntry : idToInfoHash.entrySet()) {
                        final ServiceInfo si = serviceEntry.getValue();
                        if (si != exceptThisOne) {
                            si.reloadComboBoxes();
                        }
                    }
                }
                unlockNameToServiceInfo();
            }
        });
    }

    /** Returns object that holds data of all VMs. */
    public VmsXml getVmsXml(final Host host) {
        mVmsReadLock.lock();
        try {
            return vmsXML.get(host);
        } finally {
            mVmsReadLock.unlock();
        }
    }

    /**
     * Finds DomainInfo object that contains the VM specified by
     * name.
     */
    public DomainInfo findVMSVirtualDomainInfo(final String name) {
        if (vmsNode != null && name != null) {
            for (final Object info : treeMenuController.nodesToInfos(vmsNode.children())) {
                final DomainInfo domainInfo = (DomainInfo) info;
                if (name.equals(domainInfo.getName())) {
                    return domainInfo;
                }
            }
        }
        return null;
    }

    public ResourceAgentClassInfo getClassInfoMap(final String raClass) {
        return classInfoMap.get(raClass);
    }

    public AvailableServiceInfo getAvailableServiceInfoMap(final ResourceAgent resourceAgent) {
        return availableServiceMap.get(resourceAgent);
    }

    public AvailableServicesInfo getAvailableServicesInfo() {
        return (AvailableServicesInfo) availableServicesNode.getUserObject();
    }

    public ServicesInfo getServicesInfo() {
        return servicesInfo;
    }

    public RscDefaultsInfo getRscDefaultsInfo() {
        return rscDefaultsInfo;
    }

    public void checkAccessOfEverything() {
        servicesInfo.checkResourceFields(null, servicesInfo.getParametersFromXML());
        servicesInfo.updateAdvancedPanels();
        rscDefaultsInfo.updateAdvancedPanels();
        access.updateGlobalItems();
        for (final ServiceInfo si : getExistingServiceList(null)) {
            si.checkResourceFields(null, si.getParametersFromXML());
            si.updateAdvancedPanels();
        }

        globalInfo.checkResourceFields(null, globalInfo.getParametersFromXML());
        globalInfo.updateAdvancedPanels();
        for (final ResourceInfo dri : getDrbdResHashValues()) {
            dri.checkResourceFields(null, dri.getParametersFromXML());
            dri.updateAdvancedPanels();
            dri.updateAllVolumes();
        }

        if (vmsNode != null) {
            for (final Object info : treeMenuController.nodesToInfos(vmsNode.children())) {
                final DomainInfo domainInfo = (DomainInfo) info;
                domainInfo.checkResourceFields(null, domainInfo.getParametersFromXML());
                domainInfo.updateAdvancedPanels();
                for (final Object grandChild : treeMenuController.nodesToInfos(domainInfo.getNode().children())) {
                    final HardwareInfo hardwareInfo = (HardwareInfo) grandChild;
                    hardwareInfo.checkResourceFields(null, hardwareInfo.getParametersFromXML());
                    hardwareInfo.updateAdvancedPanels();
                }
            }
        }

        for (final HbConnectionInfo hbci : crmGraph.getAllHbConnections()) {
            hbci.checkResourceFields(null, hbci.getParametersFromXML());
            hbci.updateAdvancedPanels();
        }

        for (final Host clusterHost : getClusterHosts()) {
            final HostBrowser hostBrowser = clusterHost.getBrowser();
            hostBrowser.getHostInfo().updateAdvancedPanels();
        }
    }

    /** Returns when at least one resource in the list of resources can be
        promoted. */
    public boolean isOneMaster(final Iterable<String> rscs) {
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
    public void updateHWInfo(final boolean updateLVM) {
        for (final Host h : getClusterHosts()) {
            updateHWInfo(h, updateLVM);
        }
    }

    /** Updates host hardware info immediately. */
    public void updateHWInfo(final Host host, final boolean updateLVM) {
        host.setIsLoading();
        host.getHostParser().getHWInfo(new CategoryInfo[]{clusterHostsInfo},
                new ResourceGraph[]{drbdGraph, crmGraph},
                updateLVM);
        swingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                drbdGraph.addHost(host.getBrowser().getHostDrbdInfo());
            }
        });
        drbdGraph.repaint();
    }

    /** Updates proxy host hardware info immediately. */
    public void updateProxyHWInfo(final Host host) {
        host.setIsLoading();
        host.getHostParser().getHWInfo(new CategoryInfo[]{clusterHostsInfo},
                new ResourceGraph[]{drbdGraph, crmGraph},
                !Host.UPDATE_LVM);
        drbdGraph.repaint();
    }

    /** Returns DRBD parameter hash. */
    public Map<Host, String> getHostDrbdParameters() {
        return hostDrbdParameters;
    }

    public void clStatusLock() {
        mCrmStatusLock.lock();
    }

    public void clStatusUnlock() {
        mCrmStatusLock.unlock();
    }

    /** Return null if DRBD info is availble, or the reason why not. */
    public String isDrbdAvailable(final Host host) {
        if (hostDrbdParameters.get(host) == null) {
            return "no suitable man pages";
        }
        return host.isDrbdUtilCompatibleWithDrbdModule();
    }

    /** Callback to service menu items, that show ptest results in tooltips. */
    public class ClMenuItemCallback implements ButtonCallback {
        /** Host if over a menu item that belongs to a host. */
        private final Host menuHost;
        private volatile boolean mouseStillOver = false;

        private CallbackAction action;

        public ClMenuItemCallback(final Host menuHost) {
            this.menuHost = menuHost;
        }

        @Override
        public boolean isEnabled() {
            if (clusterStatus == null) {
                return false;
            }
            final Host h;
            if (menuHost == null) {
                h = getDCHost();
            } else {
                h = menuHost;
            }
            return !Tools.versionBeforePacemaker(h);
        }

        /** Mouse out, stops animation. */
        @Override
        public final void mouseOut(final ComponentWithTest component) {
            if (isEnabled()) {
                mouseStillOver = false;
                crmGraph.stopTestAnimation((JComponent) component);
                component.setToolTipText("");
            }
        }

        /** Mouse over, starts animation, calls action() and sets tooltip. */
        @Override
        public final void mouseOver(final ComponentWithTest component) {
            if (isEnabled()) {
                mouseStillOver = true;
                component.setToolTipText(STARTING_PTEST_TOOLTIP);
                component.setToolTipBackground(Tools.getDefaultColor("ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                crmGraph.startTestAnimation((JComponent) component, startTestLatch);
                ptestLockAcquire();
                try {
                    clusterStatus.setPtestResult(null);
                    final Host h;
                    if (menuHost == null) {
                        h = getDCHost();
                    } else {
                        h = menuHost;
                    }
                    action.run(h);
                    final PtestData ptestData = new PtestData(CRM.getPtest(h));
                    component.setToolTipText(ptestData.getToolTip());
                    clusterStatus.setPtestResult(ptestData);
                } finally {
                    ptestLockRelease();
                }
                startTestLatch.countDown();
            }
        }

        public ClMenuItemCallback addAction(final CallbackAction action) {
            this.action = action;
            return this;
        }

        ///** Action that is caried out on the host. */
        //protected abstract void action(final Host dcHost);
    }

    /** Callback to service menu items, that show ptest results in tooltips. */
    public class DRBDMenuItemCallback implements ButtonCallback {
        /** Host if over a menu item that belongs to a host. */
        private final Host menuHost;
        private volatile boolean mouseStillOver = false;

        private CallbackAction action;

        public DRBDMenuItemCallback(final Host menuHost) {
            this.menuHost = menuHost;
        }

        @Override
        public final boolean isEnabled() {
            return true;
        }

        /** Mouse out, stops animation. */
        @Override
        public final void mouseOut(final ComponentWithTest component) {
            if (!isEnabled()) {
                return;
            }
            mouseStillOver = false;
            drbdGraph.stopTestAnimation((JComponent) component);
            component.setToolTipText("");
        }

        /** Mouse over, starts animation, calls action() and sets tooltip. */
        @Override
        public final void mouseOver(final ComponentWithTest component) {
            if (!isEnabled()) {
                return;
            }
            mouseStillOver = true;
            component.setToolTipText(Tools.getString("ClusterBrowser.StartingDRBDtest"));
            component.setToolTipBackground(Tools.getDefaultColor("ClusterBrowser.Test.Tooltip.Background"));
            Tools.sleep(250);
            if (!mouseStillOver) {
                return;
            }
            mouseStillOver = false;
            final CountDownLatch startTestLatch = new CountDownLatch(1);
            drbdGraph.startTestAnimation((JComponent) component, startTestLatch);
            drbdtestLockAcquire();
            final Map<Host, String> testOutput = new LinkedHashMap<Host, String>();
            if (menuHost == null) {
                for (final Host h : cluster.getHostsArray()) {
                    action.run(h);
                    testOutput.put(h, DRBD.getDRBDtest());
                }
            } else {
                action.run(menuHost);
                testOutput.put(menuHost, DRBD.getDRBDtest());
            }
            final DRBDtestData dtd = new DRBDtestData(testOutput);
            component.setToolTipText(dtd.getToolTip());
            drbdTestDataLockAcquire();
            drbdtestData = dtd;
            drbdTestDataLockRelease();
            //clusterStatus.setPtestData(ptestData);
            drbdtestLockRelease();
            startTestLatch.countDown();
        }

        ///** Action that is caried out on the host. */
        //protected abstract void action(final Host dcHost);

        public DRBDMenuItemCallback addAction(final CallbackAction action) {
            this.action = action;
            return this;
        }
    }

    public GlobalInfo getGlobalInfo() {
        return globalInfo;
    }
}
