/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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

package lcmc.host.domain;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.JComponent;

import lcmc.Exceptions;
import lcmc.cluster.domain.Cluster;
import lcmc.configs.DistResource;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.GUIData;
import lcmc.gui.HostBrowser;
import lcmc.gui.ProgressBar;
import lcmc.gui.ResourceGraph;
import lcmc.gui.SSHGui;
import lcmc.gui.TerminalPanel;
import lcmc.drbd.domain.DrbdHost;
import lcmc.drbd.domain.DrbdXml;
import lcmc.common.domain.Application;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.drbd.domain.BlockDevice;
import lcmc.drbd.domain.NetInterface;
import lcmc.vm.domain.VmsXml;
import lcmc.gui.resources.CategoryInfo;
import lcmc.robotest.RoboTest;
import lcmc.utilities.ConnectionCallback;
import lcmc.utilities.ConvertCmdCallback;
import lcmc.utilities.DRBD;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.NewOutputCallback;
import lcmc.utilities.Tools;
import lcmc.utilities.Unit;
import lcmc.utilities.ssh.ExecCommandConfig;
import lcmc.utilities.ssh.Ssh;
import lcmc.utilities.ssh.ExecCommandThread;
import lcmc.utilities.ssh.SshOutput;

/**
 * This class holds host data and implementation of host related methods.
 */
@Named
public class Host implements Comparable<Host>, Value {
    private static final Logger LOG = LoggerFactory.getLogger(Host.class);
    public static final String NOT_CONNECTED_MENU_TOOLTIP_TEXT = "not connected to the host";
    public static final String PROXY_NOT_CONNECTED_MENU_TOOLTIP_TEXT = "not connected to the proxy host";
    public static final Pattern BLOCK_DEV_FILE_PATTERN = Pattern.compile("(\\D+)\\d+");
    public static final Pattern DRBD_DEV_FILE_PATTERN = Pattern.compile(".*\\/drbd\\d+$");
    public static final Pattern USED_DISK_SPACE_PATTERN = Pattern.compile("^(.*) (\\d+)$");
    /** Timeout after which the connection is considered to be dead. */
    private static final int PING_TIMEOUT = 40000;
    private static final int DRBD_EVENTS_TIMEOUT = 40000;
    private static final int CLUSTER_EVENTS_TIMEOUT = 40000;
    private static final int HW_INFO_TIMEOUT = 40000;

    public static final String DEFAULT_HOSTNAME = "unknown";

    public static final String VM_FILESYSTEM_SOURCE_DIR_LXC = "vm.filesystem.source.dir.lxc";

    public static final String ROOT_USER = "root";
    public static final String DEFAULT_SSH_PORT = "22";
    private static final String LOG_COMMANDS_ON_SERVER_OPTION = "--cmd-log";

    private static final String NET_INFO_DELIM = "net-info";
    private static final String BRIDGE_INFO_DELIM = "bridge-info";
    private static final String DISK_INFO_DELIM = "disk-info";
    private static final String DISK_SPACE_DELIM = "disk-space";
    private static final String VG_INFO_DELIM = "vg-info";
    private static final String FILESYSTEMS_INFO_DELIM = "filesystems-info";
    private static final String CRYPTO_INFO_DELIM = "crypto-info";
    private static final String QEMU_KEYMAPS_INFO_DELIM = "qemu-keymaps-info";
    private static final String CPU_MAP_MODEL_INFO_DELIM = "cpu-map-model-info";
    private static final String CPU_MAP_VENDOR_INFO_DELIM = "cpu-map-vendor-info";
    private static final String MOUNT_POINTS_INFO_DELIM = "mount-points-info";
    private static final String GUI_INFO_DELIM = "gui-info";
    private static final String INSTALLATION_INFO_DELIM = "installation-info";
    private static final String GUI_OPTIONS_INFO_DELIM = "gui-options-info";
    private static final String VERSION_INFO_DELIM = "version-info";
    private static final String DRBD_PROXY_INFO_DELIM = "drbd-proxy-info";

    private static final Collection<String> INFO_TYPES =
             new HashSet<String>(Arrays.asList(new String[]{NET_INFO_DELIM,
                     BRIDGE_INFO_DELIM,
                     DISK_INFO_DELIM,
                     DISK_SPACE_DELIM,
                     VG_INFO_DELIM,
                     FILESYSTEMS_INFO_DELIM,
                     CRYPTO_INFO_DELIM,
                     QEMU_KEYMAPS_INFO_DELIM,
                     CPU_MAP_MODEL_INFO_DELIM,
                     CPU_MAP_VENDOR_INFO_DELIM,
                     MOUNT_POINTS_INFO_DELIM,
                     GUI_INFO_DELIM,
                     INSTALLATION_INFO_DELIM,
                     GUI_OPTIONS_INFO_DELIM,
                     VERSION_INFO_DELIM,
                     DRBD_PROXY_INFO_DELIM}));
    public static final boolean UPDATE_LVM = true;
    private String name;
    private String enteredHostOrIp = Tools.getDefault("SSH.Host");
    private String ipAddress;
    /** Ips in the combo in Dialog.Host.Configuration. */
    private final Map<Integer, String[]> allIps = new HashMap<Integer, String[]>();
    private Cluster cluster = null;
    private String hostname = DEFAULT_HOSTNAME;
    private String username = null;
    private String detectedKernelName = "";
    private String detectedDist = "";
    private String detectedDistVersion = "";
    private String detectedKernelVersion = "";
    private String detectedArch = "";
    private String kernelName = "";
    private String distributionName = "";
    private String distributionVersion = "";
    private String distributionVersionString = "";
    private String kernelVersion = "";
    private String arch = "";
    private List<NetInterface> netInterfacesWithBridges = new ArrayList<NetInterface>();
    private List<Value> bridges = new ArrayList<Value>();
    private Set<String> availableFileSystems = new TreeSet<String>();
    private Set<String> availableCryptoModules = new TreeSet<String>();
    private Set<Value> availableQemuKeymaps = new TreeSet<Value>();
    private Set<Value> availableCpuMapModels = new TreeSet<Value>();
    private Set<Value> availableCpuMapVendors = new TreeSet<Value>();
    private Set<String> mountPoints = new TreeSet<String>();
    private Map<String, BlockDevice> blockDevices = new LinkedHashMap<String, BlockDevice>();
    private Map<String, BlockDevice> drbdBlockDevices = new LinkedHashMap<String, BlockDevice>();
    /** Options for GUI drop down lists. */
    private Map<String, List<String>> guiOptions = new HashMap<String, List<String>>();
    private Set<String> drbdResourcesWithProxy = new HashSet<String>();
    private Color defaultHostColorInGraph;
    private Color savedHostColorInGraphs;
    private ExecCommandThread drbdStatusThread = null;
    private ExecCommandThread crmStatusThread = null;
    private ExecCommandThread serverStatusThread = null;
    /** List of positions of the services.
     *  Question is this: the saved positions can be different on different
     *  hosts, but only one can be used in the crm graph.
     *  Only one will be used and by next save the problem solves itself.
     */
    private final Map<String, Point2D> servicePositions = new HashMap<String, Point2D>();
    private String pacemakerVersion = null;
    private String openaisVersion = null;
    private boolean commLayerStopping = false;
    private boolean commLayerStarting = false;
    private boolean pacemakerStarting = false;
    private boolean drbdProxyStarting = false;
    private boolean corosyncInRc = false;
    private boolean openaisInRc = false;
    private boolean corosyncHasInitScript = false;
    private boolean openaisHasInitScript = false;
    private boolean corosyncRunning = false;
    private boolean openaisRunning = false;
    private boolean corosyncOrOpenaisConfigExists = false;

    private boolean heartbeatInRc = false;
    private boolean heartbeatRunning = false;
    private boolean heartbeatConfigExists = false;
    private boolean heartbeatHasInitScript = false;

    private boolean pacemakerInRc = false;
    private boolean pacemakerRunning = false;
    private boolean pacemakerHasInitScript = false;
    /** Pacemaker service version. From version 1, use pacamker init script. */
    private int pcmkServiceVersion = -1;
    private String corosyncVersion = null;
    private String heartbeatVersion = null;
    private boolean crmStatusOk = false;
    private String sshPort = null;
    private Boolean useSudo = null;
    private String sudoPassword = "";
    /** A gate that is used to synchronize the loading sequence. */
    private CountDownLatch isLoadingGate;
    private final CountDownLatch waitForServerStatusLatch = new CountDownLatch(1);
    private final Collection<JComponent> enableOnConnectElements = new ArrayList<JComponent>();
    private String pacemakerInstallMethodIndex;
    private String heartbeatPacemakerInstallMethodIndex;
    private String heartbeatLibPath = null;
    private String vmInfoFromServerMD5 = null;
    private int positionInTheCluster = 0;
    private volatile boolean lastConnectionCheckPositive = false;
    private Boolean corosyncOrHeartbeatRunning = null;
    private String libvirtVersion = null;
    private List<BlockDevice> physicalVolumes = new ArrayList<BlockDevice>();
    private Map<String, Long> volumeGroups = new LinkedHashMap<String, Long>();
    private Map<String, Set<String>> volumeGroupsWithLvs = new HashMap<String, Set<String>>();
    private boolean savable = true;
    /** Ping is set every 10s. */
    private volatile AtomicBoolean ping = new AtomicBoolean(true);
    private final Lock mDRBDStatusLock = new ReentrantLock();
    private final Lock mUpdateVMSlock = new ReentrantLock();
    private final Lock mInfoTimestampLock = new ReentrantLock();
    /** Time stamp hash. */
    private final Map<String, Double> infoTimestamp = new HashMap<String, Double>();
    private boolean inCluster = false;
    /** Whether dist info was already logged. */
    private boolean distInfoAlreadyLogged = false;

    private boolean drbdStatusOk = false;

    @Inject
    private DrbdHost drbdHost;
    @Inject
    private TerminalPanel terminalPanel;
    @Inject
    private GUIData guiData;
    @Inject
    private Ssh ssh;
    @Inject
    private HostBrowser hostBrowser;
    @Inject
    private Provider<DrbdXml> drbdXmlProvider;
    @Inject
    private Hosts allHosts;
    @Inject
    private Application application;
    @Inject
    private RoboTest roboTest;

    public void init() {
        if (allHosts.size() == 1) {
            enteredHostOrIp = Tools.getDefault("SSH.SecHost");
        }
        mountPoints.add("/mnt/");

        hostBrowser.init(this);
        terminalPanel.initWithHost(this);
    }

    public HostBrowser getBrowser() {
        return hostBrowser;
    }

    /**
     * Sets cluster in which this host is in. Set null,
     * if it is removed from the cluster. One host can be
     * only in one cluster.
     */
    public void setCluster(final Cluster cluster) {
        this.cluster = cluster;
        if (cluster == null) {
            LOG.debug1("setCluster: " + getName() + " set cluster: null");
        } else {
            inCluster = true;
            LOG.debug1("setCluster: " + getName() + " set cluster name: " + cluster.getName());
        }
    }

    public void removeFromCluster() {
        inCluster = false;
    }

    public Cluster getCluster() {
        return cluster;
    }

    /** Returns color objects of this host for drbd graph. */
    public Color[] getDrbdColors() {
        if (defaultHostColorInGraph == null) {
            defaultHostColorInGraph = Tools.getDefaultColor("Host.DefaultColor");
        }
        final Color col;
        if (savedHostColorInGraphs == null) {
            col = defaultHostColorInGraph;
        } else {
            col = savedHostColorInGraphs;
        }
        final Color secColor;
        if (isConnected()) {
            if (isDrbdStatusOk() && drbdHost.isDrbdLoaded()) {
                return new Color[]{col};
            } else {
                secColor = Tools.getDefaultColor("Host.NoStatusColor");
            }
        } else {
            secColor = Tools.getDefaultColor("Host.ErrorColor");
        }
        return new Color[]{col, secColor};
    }


    /** Returns color objects of this host. */
    public Color[] getPmColors() {
        if (defaultHostColorInGraph == null) {
            defaultHostColorInGraph = Tools.getDefaultColor("Host.DefaultColor");
        }
        final Color col;
        if (savedHostColorInGraphs == null) {
            col = defaultHostColorInGraph;
        } else {
            col = savedHostColorInGraphs;
        }
        final Color secColor;
        if (isConnected()) {
            if (isCrmStatusOk()) {
                return new Color[]{col};
            } else {
                secColor = Tools.getDefaultColor("Host.NoStatusColor");
            }
        } else {
            secColor = Tools.getDefaultColor("Host.ErrorColor");
        }
        return new Color[]{col, secColor};
    }

    public void setColor(final Color defaultColor) {
        this.defaultHostColorInGraph = defaultColor;
        if (savedHostColorInGraphs == null) {
            savedHostColorInGraphs = defaultColor;
        }
        terminalPanel.resetPromptColor();
    }

    public void setSavedHostColorInGraphs(final Color savedHostColorInGraphs) {
        this.savedHostColorInGraphs = savedHostColorInGraphs;
        terminalPanel.resetPromptColor();
    }

    public void setCrmStatusOk(final boolean crmStatusOk) {
        this.crmStatusOk = crmStatusOk;
    }

    public boolean isCrmStatusOk() {
        return crmStatusOk && isConnected();
    }

    public boolean isInCluster() {
        return inCluster;
    }

    /**
     * Returns true when this host is in a cluster and is different than the
     * specified cluster.
     */
    public boolean isInCluster(final Cluster otherCluster) {
        return isInCluster() && !cluster.equals(otherCluster);
    }

    /**
     * Sets hostname as entered by user, this can be also ipAddress. If
     * hostnameEntered changed, it reinitilizes the name.
     */
    public void setEnteredHostOrIp(final String enteredHostOrIp) {
        if (enteredHostOrIp != null
            && !enteredHostOrIp.equals(this.enteredHostOrIp)) {
            /* back button and hostname changed */
            setName(null);
            setIpAddress(null);
            setHostname(null);
        }
        this.enteredHostOrIp = enteredHostOrIp;
    }

    /** Sets hostname of the host. */
    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    /**
     * Sets user name for the host. This username is used to connect
     * to the host. The default is "root". If username changed disconnect
     * the old connection.
     */
    public void setUsername(final String username) {
        if (this.username != null && !username.equals(this.username)) {
            ssh.disconnect();
        }
        this.username = username;
    }

    /**
     * Sets ipAddress. If ipAddress has changed, disconnect the
     * old connection.
     */
    public void setIpAddress(final String ipAddress) {
        if (ipAddress != null) {
            if (this.ipAddress != null && !ipAddress.equals(this.ipAddress)) {
                ssh.disconnect();
            }
        } else if (this.ipAddress != null) {
            ssh.disconnect();
        }
        this.ipAddress = ipAddress;
    }

    public void setIps(final int hop, final String[] ipsForHop) {
        allIps.put(hop, ipsForHop);
    }

    public NetInterface[] getNetInterfacesWithBridges() {
        return netInterfacesWithBridges.toArray(new NetInterface[netInterfacesWithBridges.size()]);
    }

    public List<Value> getBridges() {
        return new ArrayList<Value>(bridges);
    }

    public BlockDevice[] getBlockDevices() {
        return blockDevices.values().toArray(new BlockDevice[blockDevices.size()]);
    }

    /**
     * Returns blockDevices as array list of device names. Removes the
     * ones that are in the drbd and are already used in CRM.
     */
    public List<String> getBlockDevicesNames() {
        final List<String> blockDevicesNames = new ArrayList<String>();
        for (final String bdName : blockDevices.keySet()) {
            final BlockDevice bd = blockDevices.get(bdName);
            if (!bd.isDrbd() && !bd.isUsedByCRM()) {
                blockDevicesNames.add(bdName);
            }
        }
        return blockDevicesNames;
    }

    /**
     * Returns blockDevices as array list of device names.
     *
     * @param otherBlockDevices
     *          list of block devices with which the intersection with
     *          block devices of this host is made.
     *
     */
    public List<String> getBlockDevicesNamesIntersection(final Iterable<String> otherBlockDevices) {
        final List<String> blockDevicesIntersection = new ArrayList<String>();
        if (otherBlockDevices == null) {
            return getBlockDevicesNames();
        }
        for (final String otherBd : otherBlockDevices) {
            final BlockDevice bd = blockDevices.get(otherBd);
            if (bd != null && !bd.isDrbd()) {
                blockDevicesIntersection.add(otherBd);
            }
        }
        return blockDevicesIntersection;
    }

    public Map<String, Integer> getNetworkIps() {
        final Map<String, Integer> networkIps = new LinkedHashMap<String, Integer>();
        for (final NetInterface ni : netInterfacesWithBridges) {
            final String netIp = ni.getNetworkIp();
            networkIps.put(netIp, ni.getCidr());
        }
        return networkIps;
    }

    /** Returns list of networks that exist on all hosts. */
    public Map<String, Integer> getNetworksIntersection(final Map<String, Integer> otherNetworkIps) {
        if (otherNetworkIps == null) {
            return getNetworkIps();
        }
        final Map<String, Integer> networksIntersection = new LinkedHashMap<String, Integer>();
        for (final NetInterface ni : netInterfacesWithBridges) {
            if (ni.isLocalHost()) {
                continue;
            }
            final String networkIp = ni.getNetworkIp();
            if (otherNetworkIps.containsKey(networkIp)
                && !networksIntersection.containsKey(networkIp)) {
                networksIntersection.put(networkIp, ni.getCidr());
            }
        }
        return networksIntersection;
    }

    public List<String> getIpsFromNetwork(final String netIp) {
        final List<String> networkIps = new ArrayList<String>();
        for (final NetInterface ni : netInterfacesWithBridges) {
            if (netIp.equals(ni.getNetworkIp())) {
                networkIps.add(ni.getIp());
            }
        }
        return networkIps;
    }

    public BlockDevice getBlockDevice(final String device) {
        return blockDevices.get(device);
    }

    public void removeFileSystems() {
        availableFileSystems.clear();
    }

    public String[] getAvailableFileSystems() {
        return availableFileSystems.toArray(new String[availableFileSystems.size()]);
    }

    public Set<String> getFileSystemsList() {
        return availableFileSystems;
    }

    public Set<String> getAvailableCryptoModules() {
        return availableCryptoModules;
    }

    public Set<Value> getAvailableQemuKeymaps() {
        return availableQemuKeymaps;
    }

    public Set<Value> getCPUMapModels() {
        return availableCpuMapModels;
    }

    public Set<Value> getCPUMapVendors() {
        return availableCpuMapVendors;
    }

    public Set<String> getMountPointsList() {
        return mountPoints;
    }

    public String[] getIps(final int hop) {
        return allIps.get(hop);
    }

    /**
     * Sets distribution info for this host from array of strings.
     * Array consists of: kernel name, kernel version, arch, os, version
     * and distribution.
     */
    @SuppressWarnings("fallthrough")
    void setDistInfo(final String[] info) {
        if (info == null) {
            LOG.debug("setDistInfo: " + getName() + " dist info is null");
            return;
        }
        if (!distInfoAlreadyLogged) {
            for (final String di : info) {
                LOG.debug1("setDistInfo: dist info: " + di);
            }
        }

        /* no breaks in the switch statement are intentional */
        String lsbVersion = null;
        String lsbDist = null;
        switch (info.length) {
            case 9:
                lsbVersion = info[8]; // TODO: not used
            case 8:
                lsbDist = info[7];
            case 7:
                lsbVersion = info[6]; // TODO: not used
            case 6:
                lsbDist = info[5];
            case 5:
                if (lsbDist == null || "linux".equals(lsbDist)) {
                    detectedDist = info[4];
                } else {
                    detectedDist = lsbDist;
                }
            case 4:
                if (lsbVersion == null) {
                    detectedDistVersion = info[3];
                } else {
                    detectedDistVersion = info[3] + '/' + lsbVersion;
                }
            case 3:
                detectedKernelVersion = info[2];
            case 2:
                detectedArch = info[1];
            case 1:
                detectedKernelName = info[0];
            case 0:
                break;
            default:
                LOG.appError("setDistInfo: list: ", Arrays.asList(info).toString());
                break;
        }
        distributionName = detectedDist;
        distributionVersion = detectedDistVersion;
        initDistInfo();
        if (!distInfoAlreadyLogged) {
            LOG.debug1("setDistInfo: kernel name: " + detectedKernelName);
            LOG.debug1("setDistInfo: kernel version: " + detectedKernelVersion);
            LOG.debug1("setDistInfo: arch: " + detectedArch);
            LOG.debug1("setDistInfo: dist version: " + detectedDistVersion);
            LOG.debug1("setDistInfo: dist: " + detectedDist);
        }
        distInfoAlreadyLogged = true;
    }

    /** Initializes dist info. Must be called after setDistInfo. */
    void initDistInfo() {
        if (!"Linux".equals(detectedKernelName)) {
            LOG.appWarning("initDistInfo: detected kernel not linux: " + detectedKernelName);
        }
        setKernelName("Linux");

        if (!distributionName.equals(detectedDist)) {
            LOG.appError("initDistInfo: dist: " + distributionName + " does not match " + detectedDist);
        }
        distributionVersionString = Tools.getDistVersionString(distributionName, distributionVersion);
        distributionVersion = Tools.getDistString("distributiondir", detectedDist, distributionVersionString, null);
        setKernelVersion(Tools.getKernelDownloadDir(detectedKernelVersion, getDistributionName(), distributionVersionString, null));
        String arch0 = Tools.getDistString("arch:" + detectedArch, getDistributionName(), distributionVersionString, null);
        if (arch0 == null) {
            arch0 = detectedArch;
        }
        setArch(arch0);
    }

    /** Returns the detected info to show. */
    public String getDetectedInfo() {
        return detectedDist + ' ' + detectedDistVersion;
    }

    /**
     * Gets distribution name from distribution version. E.g. suse from sles9.
     * This is used only when the distribution is selected in the pulldown menu,
     * not when it is detected.
     * The conversion rules for distributions are defined in DistResource.java,
     * with 'dist:' prefix.
     * TODO: remove it?
     */
    public String getDistFromDistVersion(final String dV) {
        /* remove numbers */
        if ("No Match".equals(dV)) {
            return null;
        }
        LOG.debug1("getDistFromDistVersion:" + dV.replaceFirst("\\d.*", ""));
        return Tools.getDistString("dist:" + dV.replaceFirst("\\d.*", ""), "", "", null);
    }

    void setDistributionName(final String dist) {
        this.distributionName = dist;
    }

    void setDistributionVersion(final String distVersion) {
        this.distributionVersion = distVersion;
        distributionVersionString = Tools.getDistVersionString(distributionName, distVersion);
        distributionName = getDistFromDistVersion(distVersion);
    }

    /** Sets arch, e.g. "i386". */
    public void setArch(final String arch) {
        this.arch = arch;
    }

    /** Sets kernel name, e.g. "linux". */
    void setKernelName(final String kernelName) {
        this.kernelName = kernelName;
    }

    void setKernelVersion(final String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    /** Gets kernel name. Normaly "Linux" for this application. */
    public String getKernelName() {
        return kernelName;
    }

    /**
     * Gets kernel version. Usually some version,
     * like: "2.6.13.2ws-k7-up-lowmem".
     */
    public String getKernelVersion() {
        return kernelVersion;
    }

    public String getDetectedKernelVersion() {
        return detectedKernelVersion;
    }

    /** Gets architecture like i686. */
    public String getArch() {
        return arch;
    }

    public String getHeartbeatLibPath() {
        if (heartbeatLibPath != null) {
            return heartbeatLibPath;
        }
        if ("".equals(arch)) {
            LOG.appWarning("getHeartbeatLibPath: called to soon: unknown arch");
        } else if ("x86_64".equals(arch) || "amd64".equals(arch)) {
            return "/usr/lib64/heartbeat";
        }
        return "/usr/lib/heartbeat";
    }

    public String getLxcLibPath() {
        return getDistString("libvirt.lxc.libpath");
    }

    /** Returns xen lib path. */
    public String getXenLibPath() {
        return getDistString("libvirt.xen.libpath");
    }

    /** Gets distribution, e.g., debian. */
    public String getDistributionName() {
        return distributionName;
    }

    public String getDistributionVersion() {
        return distributionVersion;
    }

    public String getDistributionVersionString() {
        return distributionVersionString;
    }

    public void disconnect() {
        if (ssh.isConnected()) {
            ssh.forceDisconnect();
        }
        setVMInfoMD5(null);
    }

    /**
     * Converts command string to real command for a distribution, specifying
     * the convert command callback.
     */
    public String getDistCommand(final String commandString, final ConvertCmdCallback convertCmdCallback) {
        return Tools.getDistCommand(commandString,
                                    distributionName,
                                    distributionVersionString,
                                    arch,
                                    convertCmdCallback,
                                    false,  /* in bash */
                                    false); /* sudo */
    }

    /** Converts a string that is specific to the distribution distribution. */
    public String getDistString(final String commandString) {
        return Tools.getDistString(commandString, distributionName, distributionVersionString, arch);
    }

    /**
     *  Gets list of strings that are specific to the distribution
     *  distribution.
     */
    public List<String> getDistStrings(final String commandString) {
        return Tools.getDistStrings(commandString, distributionName, distributionVersionString, arch);
    }


    /**
     * Converts command string to real command for a distribution, specifying
     * what-with-what hash.
     */
    public String getDistCommand(final String commandString, final Map<String, String> replaceHash) {
        return Tools.getDistCommand(
                    commandString,
                    distributionName,
                    distributionVersionString,
                    arch,
                    new ConvertCmdCallback() {
                        @Override
                        public String convert(String command) {
                            for (final String tag : replaceHash.keySet()) {
                                if (tag != null && command.contains(tag)) {
                                    String s = replaceHash.get(tag);
                                    if (s == null) {
                                        s = "";
                                    }
                                    command = command.replaceAll(tag, s);
                                }
                            }
                            return command;
                        }
                    },
                    false,  /* in bash */
                    false); /* sudo */
    }

    /**
     * Executes command. Command is executed in a new thread, after command
     * is finished execCallback.done function will be called. In case of error,
     * callback.doneError is called.
     */
    public ExecCommandThread execCommand(final ExecCommandConfig execCommandConfig) {
        return ssh.execCommand(execCommandConfig);

    }

    public SshOutput captureCommand(final ExecCommandConfig execCommandConfig) {
        return ssh.captureCommand(execCommandConfig);
    }

    public SshOutput captureCommandProgressIndicator(final String text, final ExecCommandConfig execCommandConfig) {
        final String hostName = getName();
        guiData.startProgressIndicator(hostName, text);
        try {
            return ssh.captureCommand(execCommandConfig);
        } finally {
            guiData.stopProgressIndicator(hostName, text);
        }
    }

    public void execCommandProgressIndicator(final String text, final ExecCommandConfig execCommandConfig) {
        final String hostName = getName();
        guiData.startProgressIndicator(hostName, text);
        try {
            ssh.execCommand(execCommandConfig);
        } finally {
            guiData.stopProgressIndicator(hostName, text);
        }
    }

    /**
     * Executes command with bash -c. Command is executed in a new thread,
     * after command * is finished callback.done function will be called.
     * In case of error, callback.doneError is called.
     */
    public ExecCommandThread execCommandInBash(ExecCommandConfig execCommandConfig) {
        return ssh.execCommand(execCommandConfig.inBash(true).inSudo(true));
    }

    /**
     * Executes get status command which runs in the background and updates the
     * block device object. The command is 'drbdsetup /dev/drbdX events'
     * The session is stored, so that in can be stopped with 'stop' button.
     */
    public void execDrbdStatusCommand(final ExecCallback execCallback, final NewOutputCallback outputCallback) {
        if (drbdStatusThread == null) {
            drbdStatusThread = ssh.execCommand(new ExecCommandConfig()
                                                   .commandString("DRBD.getDrbdStatus")
                                                   .inBash(false)
                                                   .inSudo(false)
                                                   .execCallback(execCallback)
                                                   .newOutputCallback(outputCallback)
                                                   .silentCommand()
                                                   .silentOutput()
                                                   .sshCommandTimeout(DRBD_EVENTS_TIMEOUT));
        } else {
            LOG.appWarning("execDrbdStatusCommand: trying to start started drbd status");
        }
    }

    /** Stops server (hw) status background process. */
    public void stopServerStatus() {
        final ExecCommandThread sst = serverStatusThread;
        if (sst == null) {
            LOG.appWarning("trying to stop stopped server status");
            return;
        }
        sst.cancelTheSession();
        serverStatusThread = null;
    }

    /** Stops drbd status background process. */
    public void stopDrbdStatus() {
        final ExecCommandThread dst = drbdStatusThread;
        if (dst == null) {
            LOG.appWarning("execDrbdStatusCommand: trying to stop stopped drbd status");
            return;
        }
        dst.cancelTheSession();
        drbdStatusThread = null;
    }

    public void waitForDrbdStatusFinish() {
        final ExecCommandThread dst = drbdStatusThread;
        if (dst != null) {
            try {
                /* it probably hangs after this timeout, so it will be
                 * killed. */
                dst.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            stopDrbdStatus();
        }
    }

    public void execCrmStatusCommand(final ExecCallback execCallback,
                                     final NewOutputCallback outputCallback) {
        if (crmStatusThread == null) {
            crmStatusThread = ssh.execCommand(new ExecCommandConfig()
                                                 .commandString("Heartbeat.getClStatus")
                                                 .inBash(false)
                                                 .inSudo(false)
                                                 .execCallback(execCallback)
                                                 .newOutputCallback(outputCallback)
                                                 .silentCommand()
                                                 .silentOutput()
                                                 .sshCommandTimeout(CLUSTER_EVENTS_TIMEOUT));
        } else {
            LOG.appWarning("execClStatusCommand: trying to start started status");
        }
    }

    public void waitForCrmStatusFinish() {
        final ExecCommandThread cst = crmStatusThread;
        if (cst == null) {
            return;
        }
        try {
            cst.join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        crmStatusThread = null;
    }

    public void stopCrmStatus() {
        final ExecCommandThread cst = crmStatusThread;
        if (cst == null) {
            LOG.appWarning("stopClStatus: trying to stop stopped status");
            return;
        }
        cst.cancelTheSession();
    }

    /** Gets ipAddress. There can be more ips, delimited with "," */
    public String getIpAddress() {
        return ipAddress;
    }

    /** Returns the ipAddress for the hop. */
    public String getIp(final int hop) {
        if (ipAddress == null) {
            return null;
        }
        final String[] ipsA = ipAddress.split(",");
        if (ipsA.length < hop + 1) {
            return null;
        }
        return ipsA[hop];
    }

    /** Return first hop ipAddress. */
    public String getFirstIp() {
        if (ipAddress == null) {
            return null;
        }
        final String[] ipsA = ipAddress.split(",");
        return ipsA[0];
    }

    public String getUsername() {
        return username;
    }

    /** Returns first username in a hop. */
    public String getFirstUsername() {
        final String[] usernames = username.split(",");
        return usernames[0];
    }

    public String getEnteredHostOrIp() {
        return enteredHostOrIp;
    }

    String getSudoPrefix(final boolean sudoTest) {
        if (useSudo != null && useSudo) {
            if (sudoTest) {
                return "sudo -E -n ";
            } else {
                return "sudo -E -p '" + Ssh.SUDO_PROMPT + "' ";
            }
        } else {
            return "";
        }
    }
    /** Returns command exclosed in sh -c "". */
    public String getSudoCommand(final String command, final boolean sudoTest) {
        if (useSudo != null && useSudo) {
            final String sudoPrefix = getSudoPrefix(sudoTest);
            return command.replaceAll(DistResource.SUDO, sudoPrefix);
        } else {
            return command.replaceAll(DistResource.SUDO, " "); /* must be " " */
        }
    }

    /**
     * Returns command with all the sshs that will be hopped.
     *
     * ssh -A   -tt -l root x.x.x.x "ssh -A   -tt -l root x.x.x.x \"ssh
     * -A   -tt -l root x.x.x.x \\\"ls\\\"\""
     */
    public String getHoppedCommand(final String command) {
        final int hops = Tools.charCount(ipAddress, ',') + 1;
        final String[] usernames = username.split(",");
        final String[] ipsA = ipAddress.split(",");
        final StringBuilder s = new StringBuilder(200);
        if (hops > 1) {
            String sshAgentPid = "";
            String sshAgentSock = "";
            final Map<String, String> variables = System.getenv();
            for (final String var : variables.keySet()) {
                final String value = variables.get(var);
                if ("SSH_AGENT_PID".equals(var)) {
                    sshAgentPid = value;
                } else if ("SSH_AUTH_SOCK".equals(var)) {
                    sshAgentSock = value;
                }
            }

            s.append("SSH_AGENT_PID=");
            s.append(sshAgentPid);
            s.append(" SSH_AUTH_SOCK=");
            s.append(sshAgentSock);
            s.append(' ');
        }
        for (int i = 1; i < hops; i++) {
            s.append("ssh -q -A -tt -o 'StrictHostKeyChecking no' -o 'ForwardAgent yes' -l ");
            if (i < usernames.length) {
                s.append(usernames[i]);
            } else {
                s.append(ROOT_USER);
            }
            s.append(' ');
            s.append(ipsA[i]);
            s.append(' ');
            s.append(Tools.escapeQuotes("\"", i - 1));
        }

        s.append(Tools.escapeQuotes(command, hops - 1));

        for (int i = hops - 1; i > 0; i--) {
            s.append(Tools.escapeQuotes("\"", i - 1));
        }
        return s.toString();
    }

    public String getHostname() {
        return hostname;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Gets name, that is shown in the tab. Name is either host name, if it is
     * set or ipAddress.
     */
    public String getName() {
        if (name == null) {
            final String nodeName;
            if (hostname != null) {
                final int i = hostname.indexOf(',');
                if (i > 0) {
                    nodeName = hostname.substring(i + 1);
                } else {
                    nodeName = hostname;
                }
            } else if (enteredHostOrIp != null) {
                final int i = enteredHostOrIp.indexOf(',');
                if (i > 0) {
                    nodeName = enteredHostOrIp.substring(i + 1);
                } else {
                    nodeName = enteredHostOrIp;
                }
            } else {
                return ipAddress;
            }
            return nodeName;
        } else {
            return name;
        }
    }

    /** Sets name of the host as it will be identified. */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets string with user and hostname as used in prompt or ssh like
     * rasto@linbit.at.
     */
    public String getUserAtHost() {
        return username + '@' + getHostname();
    }

    public Ssh getSSH() {
        return ssh;
    }

    public TerminalPanel getTerminalPanel() {
        return terminalPanel;
    }

    /**
     * Connects host with ssh. Dialog is needed, in case if password etc.
     * has to be entered. Connection is made in the background, after
     * connection is established, callback.done() is called. In case
     * of error callback.doneError() is called.
     */
    public void connect(SSHGui sshGui, final ConnectionCallback callback) {
        if (sshGui == null) {
            sshGui = new SSHGui(guiData.getMainFrame(), this, null);
        }
        ssh.connect(sshGui, callback, this);
    }

    /**
     * Connects host with ssh. Dialog is needed, in case if password etc.
     * has to be entered. Connection is made in the background, after
     * connection is established, callback.done() is called. In case
     * of error callback.doneError() is called.
     *
     * @param sshGui
     *          ssh gui dialog
     *
     * @param progressBar
     *          progress bar that is used to show progress through connecting
     *
     * @param callback
     *          callback class that implements ConnectionCallback interface
     */
    public void connect(final SSHGui sshGui, final ProgressBar progressBar, final ConnectionCallback callback) {
        LOG.debug1("connect: host: " + sshGui);
        ssh.connect(sshGui, progressBar, callback, this);
    }

    /**
     * Register a component that will be enabled if the host connected and
     * disabled if disconnected.
     */
    public void registerEnableOnConnect(final JComponent c) {
        if (!enableOnConnectElements.contains(c)) {
            enableOnConnectElements.add(c);
        }
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                c.setEnabled(isConnected());
            }
        });
    }

    /**
     * Is called after the host is connected or disconnected and
     * enables/disables the conponents that are registered to be enabled on
     * connect.
     */
    public void setConnected() {
        final boolean con = isConnected();
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final JComponent c : enableOnConnectElements) {
                    c.setEnabled(con);
                }
            }
        });
        if (lastConnectionCheckPositive != con) {
            lastConnectionCheckPositive = con;
            if (con) {
               LOG.info("setConnected: " + getName() + ": connection established");
            } else {
               LOG.info("setConnected: " + getName() + ": connection lost");
            }
            final ClusterBrowser cb = getBrowser().getClusterBrowser();
            if (cb != null) {
                cb.getCrmGraph().repaint();
                cb.getDrbdGraph().repaint();
            }
        }
    }

    /** Make an ssh connection to the host. */
    public void connect(SSHGui sshGui, final boolean progressIndicator, final int index) {
        if (!isConnected()) {
            final String hostName = getName();
            if (progressIndicator) {
                guiData.startProgressIndicator(hostName,
                                               Tools.getString("Dialog.Host.SSH.Connecting") + " (" + index + ')');
            }
            if (sshGui == null) {
                sshGui = new SSHGui(guiData.getMainFrame(), this, null);
            }

            connect(sshGui,
                    new ConnectionCallback() {
                        @Override
                        public void done(final int flag) {
                            setConnected();
                            getSSH().execCommandAndWait(new ExecCommandConfig()
                                                            .command(":") /* activate sudo */
                                                            .silentCommand()
                                                            .silentOutput()
                                                            .sshCommandTimeout(10000));
                                    getSSH().installGuiHelper();
                            getAllInfo();
                            if (progressIndicator) {
                                guiData.stopProgressIndicator(
                                  hostName,
                                  Tools.getString("Dialog.Host.SSH.Connecting") + " (" + index + ')');
                            }
                        }

                        @Override
                        public void doneError(final String errorText) {
                            setLoadingError();
                            setConnected();
                            if (progressIndicator) {
                                guiData.stopProgressIndicator(
                                  hostName,
                                  Tools.getString("Dialog.Host.SSH.Connecting") + " (" + index + ')');
                                guiData.progressIndicatorFailed(
                                  hostName,
                                  Tools.getString("Dialog.Host.SSH.Connecting") + " (" + index + ')');
                                guiData.stopProgressIndicator(
                                  hostName,
                                  Tools.getString("Dialog.Host.SSH.Connecting") + " (" + index + ')');
                            }
                        }
                    });
        }
    }

    /** Gets and stores info about the host. */
    void getAllInfo() {
        execCommand(new ExecCommandConfig().commandString("GetHostAllInfo")
                .execCallback(new ExecCallback() {
                    @Override
                    public void done(final String ans) {
                        parseHostInfo(ans);
                        setLoadingDone();
                    }

                    @Override
                    public void doneError(final String ans, final int exitCode) {
                        setLoadingError();
                    }
                })
                .sshCommandTimeout(HW_INFO_TIMEOUT)
                .silentCommand()
                .silentOutput()
                .silentCommand()).block();
    }

    /** Gets and stores hardware info about the host. */
    public void getHWInfo(final boolean updateLVM) {
        getHWInfo(new CategoryInfo[]{}, new ResourceGraph[]{}, updateLVM);
    }

    /** Gets and stores hardware info about the host. */
    public void getHWInfo(final CategoryInfo[] infosToUpdate, final ResourceGraph[] graphs, final boolean updateLVM) {
        final String cmd;
        if (updateLVM) {
            cmd = "GetHostHWInfoLVM";
        } else {
            cmd = "GetHostHWInfo";
        }
        execCommand(new ExecCommandConfig().commandString(cmd)
                         .execCallback(new ExecCallback() {
                             @Override
                             public void done(final String ans) {
                                 parseHostInfo(ans);
                                 for (final CategoryInfo ci : infosToUpdate) {
                                     ci.updateTable(CategoryInfo.MAIN_TABLE);
                                 }
                                 for (final ResourceGraph g : graphs) {
                                     if (g != null) {
                                         g.repaint();
                                     }
                                 }
                                 setLoadingDone();
                             }

                             @Override
                             public void doneError(final String ans, final int exitCode) {
                                 setLoadingError();
                                 getSSH().forceReconnect();
                             }
                         })
                         .sshCommandTimeout(HW_INFO_TIMEOUT)
                         .silentCommand()
                         .silentOutput()).block();
    }

    public String getOutput(final String type, final StringBuffer buffer) {
        final String infoStart = "--" + type + "-info-start--";
        final String infoEnd = "--" + type + "-info-end--";
        final int infoStartLength = infoStart.length();
        final int infoEndLength = infoEnd.length();
        final int s = buffer.indexOf(infoStart);
        final int s2 = buffer.indexOf("\r\n", s);
        final int e = buffer.indexOf(infoEnd, s);
        String out = null;
        if (s > -1 && s < s2 && s2 <= e) {
            Double timestamp = null;
            final String ts = buffer.substring(s + infoStartLength, s2);
            try {
                timestamp = Double.parseDouble(ts);
            }  catch (final NumberFormatException nfe) {
                LOG.debug("getOutput: could not parse: " + ts + ' ' + nfe);
            }
            mInfoTimestampLock.lock();
            if (timestamp != null
                && (!infoTimestamp.containsKey(type) || timestamp >= infoTimestamp.get(type))) {
                infoTimestamp.put(type, timestamp);
                mInfoTimestampLock.unlock();
                out = buffer.substring(s2 + 2, e);
            } else {
                mInfoTimestampLock.unlock();
            }
            buffer.delete(0, e + infoEndLength + 2);
        }
        return out;
    }

    public void startPing() {
        ssh.execCommand(new ExecCommandConfig()
                         .commandString("PingCommand")
                         .inBash(true)
                         .inSudo(false)
                         .execCallback(new ExecCallback() {
                             @Override
                             public void done(final String ans) {
                             }

                             @Override
                             public void doneError(final String ans, final int exitCode) {
                             }
                         })
                         .newOutputCallback(new NewOutputCallback() {
                             @Override
                             public void output(final String output) {
                                 ping.set(true);
                             }
                         })
                         .silentCommand()
                         .silentOutput()
                         .sshCommandTimeout(PING_TIMEOUT)).block();
    }

    /** Gets and stores hardware info about the host. */
    public void startHWInfoDaemon(final CategoryInfo[] infosToUpdate, final ResourceGraph[] graphs) {
        final Host host = this;
        LOG.debug1("startHWInfoDaemon: " + getName());
        serverStatusThread = ssh.execCommand(new ExecCommandConfig()
                         .commandString("HostHWInfoDaemon")
                         .inBash(false)
                         .inSudo(false)
                         .execCallback(new ExecCallback() {
                             @Override
                             public void done(final String ans) {
                                 parseHostInfo(ans);
                                 for (final CategoryInfo ci : infosToUpdate) {
                                     ci.updateTable(CategoryInfo.MAIN_TABLE);
                                 }
                                 for (final ResourceGraph g : graphs) {
                                     if (g != null) {
                                         g.repaint();
                                     }
                                 }
                                 if (host.getWaitForServerStatusLatch()) {
                                     final ClusterBrowser cb = getBrowser().getClusterBrowser();
                                     cb.updateServerStatus(host);
                                 }
                                 setLoadingDone();
                             }

                             @Override
                             public void doneError(final String ans, final int exitCode) {
                                 if (host.getWaitForServerStatusLatch()) {
                                     final ClusterBrowser cb = getBrowser().getClusterBrowser();
                                     cb.updateServerStatus(host);
                                 }
                                 setLoadingError();
                             }
                         })
                         .newOutputCallback(new NewOutputCallback() {
                             private final StringBuffer outputBuffer = new StringBuffer(300);
                             @Override
                             public void output(final String output) {
                                 outputBuffer.append(output);
                                 final ClusterBrowser cb = getBrowser().getClusterBrowser();
                                 String hw, vm, drbdConfig;
                                 String hwUpdate = null;
                                 String vmUpdate = null;
                                 String drbdUpdate = null;
                                 do {
                                     hw = getOutput("hw", outputBuffer);
                                     if (hw != null) {
                                         hwUpdate = hw;
                                     }
                                     vm = getOutput("vm", outputBuffer);
                                     if (vmStatusTryLock()) {
                                         if (vm != null) {
                                             vmUpdate = vm;
                                         }
                                         vmStatusUnlock();
                                     }
                                     drbdStatusLock();
                                     drbdConfig = getOutput("drbd", outputBuffer);
                                     if (drbdConfig != null) {
                                         drbdUpdate = drbdConfig;
                                     }
                                     drbdStatusUnlock();
                                 } while (hw != null || vm != null || drbdConfig != null);

                                 Tools.chomp(outputBuffer);
                                 if (hwUpdate != null) {
                                     parseHostInfo(hwUpdate);
                                     for (final ResourceGraph g : graphs) {
                                         if (g != null) {
                                             g.repaint();
                                         }
                                     }
                                 }
                                 if (vmUpdate != null) {
                                     final VmsXml newVmsXml = new VmsXml(host);
                                     if (newVmsXml.update(vmUpdate)) {
                                         cb.vmsXmlPut(host, newVmsXml);
                                         cb.updateVms();
                                     }
                                 }
                                 if (drbdUpdate != null) {
                                     final DrbdXml dxml = drbdXmlProvider.get();
                                     dxml.init(cluster.getHostsArray(), cb.getHostDrbdParameters());
                                     dxml.update(drbdUpdate);
                                     cb.setDrbdXml(dxml);
                                     application.invokeLater(new Runnable() {
                                         @Override
                                         public void run() {
                                             hostBrowser.getClusterBrowser().getGlobalInfo().setParameters();
                                             cb.updateDrbdResources();
                                         }
                                     });
                                 }
                                 if (drbdUpdate != null
                                     || vmUpdate != null) {
                                     cb.updateHWInfo(host, !Host.UPDATE_LVM);
                                 }
                                 if (drbdUpdate != null) {
                                     cb.updateServerStatus(host);
                                 }
                                 if (getWaitForServerStatusLatch()) {
                                     cb.updateServerStatus(host);
                                 }
                                 setLoadingDone();
                             }
                         })
                         .silentCommand()
                         .silentOutput()
                         .sshCommandTimeout(HW_INFO_TIMEOUT)).block();
    }

    public void startConnectionStatus() {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (ping.get()) {
                       LOG.debug2("startConnectionStatus: connection ok on " + getName());
                       setConnected();
                       ping.set(false);
                    } else {
                       LOG.debug2("startConnectionStatus: connection lost on " + getName());
                       getSSH().forceReconnect();
                       setConnected();
                    }
                    Tools.sleep(PING_TIMEOUT);
                    final ClusterBrowser cb = getBrowser().getClusterBrowser();
                    /* cluster could be removed */
                    if (cb == null || cb.isCancelServerStatus()) {
                        break;
                    }
                }
            }
        });
        thread.start();
    }

    /** Returns whether host ssh connection was established. */
    public boolean isConnected() {
        if (ssh == null) {
            return false;
        }
        return ssh.isConnected();
    }

    /**
     * replaces variables in command. For output to the user set hidePassword
     * to true, so that variables like passwords are not shown to the user.
     * This functions should not be used to really sensitive passwords, since
     * it is not secure.
     *
     * following variables are defined:
     *
     * \@USER\@           user for download area
     * \@PASSWORD\@       password for download area
     * \@KERNELVERSION\@  version of the kernel
     * \@DRBDVERSION\@    version of drbd, that will be installed
     * \@DISTRIBUTION\@   distribution for which the drbd will be installed.
     *
     * @param command
     *          command in which the variables will be replaced
     *
     * @param hidePassword
     *          if set to true all vars will be replaced except of sensitive
     *          ones.
     *
     * @return command with replaced variables
     */
    public String replaceVars(String command, final boolean hidePassword) {
        if (command.contains("@USER@")) {
            command = command.replaceAll("@USER@", application.getDownloadUser());
        }
        if (command.indexOf("@PASSWORD@") > -1) {
            if (hidePassword) {
                command = command.replaceAll("@PASSWORD@", "*****");
            } else {
                command = command.replaceAll("@PASSWORD@", application.getDownloadPassword());
            }
        }
        String supportDir = "support";
        if (application.isStagingDrbd()) {
            supportDir = "support/staging";
        }
        if (kernelVersion != null
            && command.contains("@KERNELVERSIONDIR@")) {
            command = command.replaceAll("@KERNELVERSIONDIR@", kernelVersion);
        }
        if (distributionVersion != null
            && command.contains("@DISTRIBUTION@")) {
            command = command.replaceAll("@DISTRIBUTION@", distributionVersion);
        }
        if (arch != null
            && command.contains("@ARCH@")) {
            command = command.replaceAll("@ARCH@", arch);
        }
        if (command.contains("@SUPPORTDIR@")) {
            command = command.replaceAll("@SUPPORTDIR@", supportDir);
        }
        if (command.contains("@DRBDDIR@")) {
            final String drbdDir = "drbd";
            command = command.replaceAll("@DRBDDIR@", drbdDir);
        }
        if (command.contains("@GUI-HELPER@")) {
            final StringBuilder helperProg = new StringBuilder("/usr/local/bin/lcmc-gui-helper-");
            helperProg.append(Tools.getRelease());
            if (application.isCmdLog()) {
                helperProg.append(' ');
                helperProg.append(LOG_COMMANDS_ON_SERVER_OPTION);
            }
            command = command.replaceAll("@GUI-HELPER@", helperProg.toString());
        }
        if (command.contains("@GUI-HELPER-PROG@")) {
            command = command.replaceAll("@GUI-HELPER-PROG@", "/usr/local/bin/lcmc-gui-helper-" + Tools.getRelease());
        }
        return command;
    }

    /**
     * Replaces variables in command.
     *
     * Following variables are defined:
     *
     * \@USER\@           user for download area
     * \@PASSWORD\@       password for download area
     * \@KERNELVERSION\@  version of the kernel
     * \@DRBDVERSION\@    version of drbd, that will be installed
     * \@DISTRIBUTION\@   distribution for which the drbd will be installed.
     *
     * @param command
     *          command in which the variables will be replaced
     *
     * @return command with replaced variables
     */
    public String replaceVars(final String command) {
        return replaceVars(command, false);
    }

    public void parseHostInfo(final String ans) {
        LOG.debug1("parseHostInfo: updating host info: " + getName());
        final String[] lines = ans.split("\\r?\\n");
        final List<String> versionLines = new ArrayList<String>();
        final Map<String, BlockDevice> newBlockDevices = new LinkedHashMap<String, BlockDevice>();
        final Map<String, BlockDevice> newDrbdBlockDevices = new LinkedHashMap<String, BlockDevice>();
        final List<NetInterface> newNetInterfaces = new ArrayList<NetInterface>();
        final List<Value> newBridges = new ArrayList<Value>();
        final Map<String, Long> newVolumeGroups = new LinkedHashMap<String, Long>();
        final Map<String, Set<String>> newVolumeGroupsLVS = new HashMap<String, Set<String>>();
        final List<BlockDevice> newPhysicalVolumes = new ArrayList<BlockDevice>();
        final Set<String> newFileSystems = new TreeSet<String>();
        final Set<String> newCryptoModules = new TreeSet<String>();
        final Set<Value> newQemuKeymaps = new TreeSet<Value>();
        final Set<Value> newCpuMapModels = new TreeSet<Value>();
        final Set<Value> newCpuMapVendors = new TreeSet<Value>();
        final Set<String> newMountPoints = new TreeSet<String>();

        final Map<String, List<String>> newGuiOptions = new HashMap<String, List<String>>();
        final Set<String> newDrbdResProxy = new HashSet<String>();

        final Collection<String> changedTypes = new HashSet<String>();

        final Map<String, String> diskSpaces = new HashMap<String, String>();

        newMountPoints.add("/mnt/");
        String guiOptionName = null;

        String type = "";
        for (final String line : lines) {
            if (line.indexOf("ERROR:") == 0) {
                break;
            } else if (line.indexOf("WARNING:") == 0) {
                continue;
            }
            if (INFO_TYPES.contains(line)) {
                type = line;
                changedTypes.add(type);
                continue;
            }
            if ("net-info".equals(type)) {
                try {
                    final NetInterface netInterface = new NetInterface(line);
                    if (netInterface.getIp() != null && !"".equals(netInterface.getIp())) {
                        newNetInterfaces.add(netInterface);
                    }
                } catch (final UnknownHostException e) {
                    LOG.appWarning("parseHostInfo: cannot parse: net-info: " + line);
                }
            } else if (BRIDGE_INFO_DELIM.equals(type)) {
                newBridges.add(new StringValue(line));
            } else if ("disk-info".equals(type)) {
                BlockDevice blockDevice = new BlockDevice(line);
                final String bdName = blockDevice.getName();
                if (bdName != null) {
                    final Matcher drbdM = DRBD_DEV_FILE_PATTERN.matcher(bdName);
                    if (drbdM.matches()) {
                        if (drbdBlockDevices.containsKey(bdName)) {
                            /* get the existing block device object,
                               forget the new one. */
                            blockDevice = drbdBlockDevices.get(bdName);
                            blockDevice.update(line);
                        }
                        newDrbdBlockDevices.put(bdName, blockDevice);
                    } else {
                        if (blockDevices.containsKey(bdName)) {
                            /* get the existing block device object,
                               forget the new one. */
                            blockDevice = blockDevices.get(bdName);
                            blockDevice.update(line);
                        }
                        newBlockDevices.put(bdName, blockDevice);
                        if (blockDevice.getVolumeGroup() == null
                            && bdName.length() > 5 && bdName.indexOf('/', 5) < 0) {
                            final Matcher m = BLOCK_DEV_FILE_PATTERN.matcher(bdName);
                            if (m.matches()) {
                                newBlockDevices.remove(m.group(1));
                            }
                        }
                    }
                }
                final String vg = blockDevice.getVolumeGroup();
                if (vg != null) {
                    Set<String> logicalVolumes = newVolumeGroupsLVS.get(vg);
                    if (logicalVolumes == null) {
                        logicalVolumes = new HashSet<String>();
                        newVolumeGroupsLVS.put(vg, logicalVolumes);
                    }
                    final String lv = blockDevice.getLogicalVolume();
                    if (lv != null) {
                        logicalVolumes.add(lv);
                    }
                }
                if (blockDevice.isPhysicalVolume()) {
                    newPhysicalVolumes.add(blockDevice);
                }
            } else if (DISK_SPACE_DELIM.equals(type)) {
                final Matcher dsM = USED_DISK_SPACE_PATTERN.matcher(line);
                if (dsM.matches()) {
                    final String bdName = dsM.group(1);
                    final String used = dsM.group(2);
                    diskSpaces.put(bdName, used);
                }
            } else if ("vg-info".equals(type)) {
                final String[] vgi = line.split("\\s+");
                if (vgi.length == 2) {
                    newVolumeGroups.put(vgi[0], Long.parseLong(vgi[1]));
                } else {
                    LOG.appWarning("parseHostInfo: could not parse volume info: " + line);
                }
            } else if ("filesystems-info".equals(type)) {
                newFileSystems.add(line);
            } else if ("crypto-info".equals(type)) {
                newCryptoModules.add(line);
            } else if ("qemu-keymaps-info".equals(type)) {
                newQemuKeymaps.add(new StringValue(line));
            } else if ("cpu-map-model-info".equals(type)) {
                newCpuMapModels.add(new StringValue(line));
            } else if ("cpu-map-vendor-info".equals(type)) {
                newCpuMapVendors.add(new StringValue(line));
            } else if ("mount-points-info".equals(type)) {
                newMountPoints.add(line);
            } else if ("gui-info".equals(type)) {
                parseGuiInfo(line);
            } else if ("installation-info".equals(type)) {
                parseInstallationInfo(line);
            } else if ("gui-options-info".equals(type)) {
                guiOptionName = parseGuiOptionsInfo(line, guiOptionName, newGuiOptions);
            } else if ("version-info".equals(type)) {
                versionLines.add(line);
            } else if ("drbd-proxy-info".equals(type)) {
                /* res-other.host-this.host */
                final Cluster cl = getCluster();
                if (cl != null) {
                    String res = null;
                    if (line.startsWith("up:")) {
                        for (final Host otherHost : getCluster().getProxyHosts()) {
                            if (otherHost == this) {
                                continue;
                            }
                            final String hostsPart = '-' + otherHost.getName() + '-' + getName();
                            final int i = line.indexOf(hostsPart);
                            if (i > 0) {
                                res = line.substring(3, i);
                                break;
                            }
                        }
                    }
                    if (res == null) {
                        LOG.appWarning("parseHostInfo: could not parse proxy line: " + line);
                    } else {
                        newDrbdResProxy.add(res);
                    }
                }
            }
        }

        LOG.debug1("parseHostInfo: "
                   + getName()
                   + ", pacemaker: "   + pacemakerVersion
                   + ", corosync: "    + corosyncVersion
                   + ", heartbeat: "   + heartbeatVersion
                   + ", drbd: "        + drbdHost.getDrbdUtilVersion()
                   + ", drbd module: " + drbdHost.getDrbdModuleVersion());

        if (changedTypes.contains(NET_INFO_DELIM)) {
            netInterfacesWithBridges = newNetInterfaces;
        }

        if (changedTypes.contains(BRIDGE_INFO_DELIM)) {
            bridges = newBridges;
        }

        if (changedTypes.contains(DISK_INFO_DELIM)) {
            blockDevices = newBlockDevices;
            drbdBlockDevices = newDrbdBlockDevices;
            physicalVolumes = newPhysicalVolumes;
            volumeGroupsWithLvs = newVolumeGroupsLVS;
        }
        if (changedTypes.contains(DISK_SPACE_DELIM)) {
            for (final Map.Entry<String, String> entry : diskSpaces.entrySet()) {
                final BlockDevice bd = blockDevices.get(entry.getKey());
                if (bd != null) {
                    bd.setUsed(entry.getValue());
                }
            }
        }

        if (changedTypes.contains(VG_INFO_DELIM)) {
            volumeGroups = newVolumeGroups;
        }

        if (changedTypes.contains(FILESYSTEMS_INFO_DELIM)) {
            availableFileSystems = newFileSystems;
        }

        if (changedTypes.contains(CRYPTO_INFO_DELIM)) {
            availableCryptoModules = newCryptoModules;
        }

        if (changedTypes.contains(QEMU_KEYMAPS_INFO_DELIM)) {
            availableQemuKeymaps = newQemuKeymaps;
        }

        if (changedTypes.contains(CPU_MAP_MODEL_INFO_DELIM)) {
            availableCpuMapModels = newCpuMapModels;
        }

        if (changedTypes.contains(CPU_MAP_VENDOR_INFO_DELIM)) {
            availableCpuMapVendors = newCpuMapVendors;
        }

        if (changedTypes.contains(MOUNT_POINTS_INFO_DELIM)) {
            mountPoints = newMountPoints;
        }

        if (changedTypes.contains(VERSION_INFO_DELIM)) {
            setDistInfo(versionLines.toArray(new String[versionLines.size()]));
        }

        if (changedTypes.contains(GUI_OPTIONS_INFO_DELIM)) {
            guiOptions = newGuiOptions;
        }

        if (changedTypes.contains(DRBD_PROXY_INFO_DELIM)) {
            drbdResourcesWithProxy = newDrbdResProxy;
        }

        getBrowser().updateHWResources(getNetInterfacesWithBridges(), getBlockDevices(), getAvailableFileSystems());
    }

    /** Parses the gui info, with drbd and heartbeat graph positions. */
    private void parseGuiInfo(final String line) {
        final String[] tokens = line.split(";");
        String id = null;
        String x = null;
        String y = null;
        for (final String token : tokens) {
            final String[] r = token.split("=");
            if (r.length == 2) {
                if (r[0].equals("hb") || r[0].equals("dr")) {
                    id = token;
                } else if (r[0].equals("x")) {
                    x = r[1];
                } else if (r[0].equals("y")) {
                    y = r[1];
                }
            }
        }
        if (id != null && x != null && y != null) {
            servicePositions.put(id, new Point2D.Double(new Double(x).doubleValue(), new Double(y).doubleValue()));
        }
    }
    /** Parses the gui options info. */
    public String parseGuiOptionsInfo(final String line,
                                      final String guiOptionName,
                                      final Map<String, List<String>> goptions) {
        if (line.length() > 2 && line.substring(0, 2).equals("o:")) {
            final String op = line.substring(2);
            goptions.put(op, new ArrayList<String>());
            return op;
        }
        if (guiOptionName != null) {
            final List<String> options = goptions.get(guiOptionName);
            if (options != null) {
                options.add(line);
            }
        }
        return guiOptionName;
    }

    public void parseInstallationInfo(final String line) {
        final String[] tokens = line.split(":|\\s+");
        if (tokens.length < 2) {
            return;
        }
        if ("pm".equals(tokens[0])) {
            if (tokens.length == 2) {
                pacemakerVersion = tokens[1].trim();
            } else {
                pacemakerVersion = null;
            }
        } else if ("cs".equals(tokens[0])) {
            if (tokens.length == 2) {
                corosyncVersion = tokens[1].trim();
            } else {
                corosyncVersion = null;
            }
        } else if ("ais".equals(tokens[0])) {
            if (tokens.length == 2) {
                openaisVersion = tokens[1].trim();
            } else {
                openaisVersion = null;
            }
        } else if ("ais-rc".equals(tokens[0])) {
            if (tokens.length == 2) {
                openaisInRc = "on".equals(tokens[1].trim());
            } else {
                openaisInRc = false;
            }
        } else if ("cs-rc".equals(tokens[0])) {
            if (tokens.length == 2) {
                corosyncInRc = "on".equals(tokens[1].trim());
            } else {
                corosyncInRc = false;
            }
        } else if ("cs-ais-conf".equals(tokens[0])) {
            if (tokens.length == 2) {
                corosyncOrOpenaisConfigExists = "on".equals(tokens[1].trim());
            } else {
                corosyncOrOpenaisConfigExists = false;
            }
        } else if ("cs-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                corosyncRunning = "on".equals(tokens[1].trim());
            } else {
                corosyncRunning = false;
            }
        } else if ("ais-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                openaisRunning = "on".equals(tokens[1].trim());
                commLayerStarting = false;
                pacemakerStarting = false;
            } else {
                openaisRunning = false;
            }
        } else if ("cs-init".equals(tokens[0])) {
            if (tokens.length == 2) {
                corosyncHasInitScript = "on".equals(tokens[1].trim());
            } else {
                corosyncHasInitScript = false;
            }
        } else if ("ais-init".equals(tokens[0])) {
            if (tokens.length == 2) {
                openaisHasInitScript = "on".equals(tokens[1].trim());
            } else {
                openaisHasInitScript = false;
            }
        } else if ("hb".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatVersion = tokens[1].trim();
            } else {
                heartbeatVersion = null;
            }
        } else if ("hb-init".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatHasInitScript = "on".equals(tokens[1].trim());
            } else {
                heartbeatHasInitScript = false;
            }
        } else if ("hb-rc".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatInRc = "on".equals(tokens[1].trim());
            } else {
                heartbeatInRc = false;
            }
        } else if ("hb-conf".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatConfigExists = "on".equals(tokens[1].trim());
            } else {
                heartbeatConfigExists = false;
            }
        } else if ("hb-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatRunning = "on".equals(tokens[1].trim());
            } else {
                heartbeatRunning = false;
            }
        } else if ("pcmk-rc".equals(tokens[0])) {
            if (tokens.length == 2) {
                pacemakerInRc = "on".equals(tokens[1].trim());
            } else {
                pacemakerInRc = false;
            }
        } else if ("pcmk-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                pacemakerRunning = "on".equals(tokens[1].trim());
            } else {
                pacemakerRunning = false;
            }
        } else if ("drbdp-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                drbdHost.setDrbdProxyRunning("on".equals(tokens[1].trim()));
            } else {
                drbdHost.setDrbdProxyRunning(false);
            }
        } else if ("pcmk-init".equals(tokens[0])) {
            if (tokens.length == 2) {
                pacemakerHasInitScript = "on".equals(tokens[1].trim());
            } else {
                pacemakerHasInitScript = false;
            }
        } else if ("pcmk-svc-ver".equals(tokens[0])) {
            if (tokens.length == 2) {
                try {
                    pcmkServiceVersion = Integer.parseInt(tokens[1].trim());
                } catch (final NumberFormatException e) {
                    pcmkServiceVersion = -1;
                }
            }
        } else if ("drbd-loaded".equals(tokens[0])) {
            if (tokens.length == 2) {
                drbdHost.setDrbdLoaded("on".equals(tokens[1].trim()));
            } else {
                drbdHost.setDrbdLoaded(false);
            }
        } else if ("hb-lib-path".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatLibPath = tokens[1].trim();
            } else {
                heartbeatLibPath = null;
            }
        } else if ("hn".equals(tokens[0])) {
            if (tokens.length == 2) {
                hostname = tokens[1].trim();
            } else {
                hostname = null;
            }
            setName(hostname);
        } else if ("drbd".equals(tokens[0])) {
            if (tokens.length == 2) {
                drbdHost.setDrbdUtilVersion(tokens[1].trim());
            } else {
                drbdHost.setDrbdUtilVersion(null);
            }
        } else if ("drbd-mod".equals(tokens[0])) {
            if (tokens.length == 2) {
                drbdHost.setDrbdModuleVersion(tokens[1].trim());
            } else {
                drbdHost.setDrbdModuleVersion(null);
            }
        }
        corosyncOrHeartbeatRunning = heartbeatRunning || corosyncRunning || openaisRunning;
        if (commLayerStarting && (corosyncRunning || openaisRunning || heartbeatRunning)) {
            commLayerStarting = false;
        }
        if (pacemakerStarting && pacemakerRunning) {
            pacemakerStarting = false;
        }
        if (drbdProxyStarting && drbdHost.isDrbdProxyRunning()) {
            drbdProxyStarting = false;
        }
        if (commLayerStopping && !corosyncRunning && !openaisRunning && !heartbeatRunning) {
            commLayerStopping = false;
        }
    }

    public Point2D getGraphPosition(final String id) {
        return servicePositions.get(id);
    }

    public void resetGraphPosition(final String id) {
        servicePositions.remove(id);
    }

    public void saveGraphPositions(final Map<String, Point2D> positions) {
        final StringBuilder lines = new StringBuilder();
        for (final String id : positions.keySet()) {
            final Point2D p = positions.get(id);
            double x = p.getX();
            if (x < 0) {
                x = 0;
            }
            double y = p.getY();
            if (y < 0) {
                y = 0;
            }
            lines.append(id).append(";x=").append(x).append(";y=").append(y).append('\n');
        }
        getSSH().createConfig(lines.toString(),
                              "drbdgui.cf",
                              "/var/lib/heartbeat/",
                              "0600",
                              false,
                              null,
                              null);
    }

    public void setHeartbeatVersion(final String heartbeatVersion) {
        this.heartbeatVersion = heartbeatVersion;
    }

    public void setCorosyncVersion(final String corosyncVersion) {
        this.corosyncVersion = corosyncVersion;
    }

    public void setPacemakerVersion(final String pacemakerVersion) {
        this.pacemakerVersion = pacemakerVersion;
    }

    public void setOpenaisVersion(final String openaisVersion) {
        this.openaisVersion = openaisVersion;
    }

    public String getPacemakerVersion() {
        return pacemakerVersion;
    }

    public String getCorosyncVersion() {
        return corosyncVersion;
    }

    public boolean isCorosyncInstalled() {
        return corosyncVersion != null;
    }

    public boolean isOpenaisWrapper() {
        return "wrapper".equals(openaisVersion);
    }


    public String getOpenaisVersion() {
        return openaisVersion;
    }

    public String getHeartbeatVersion() {
        return heartbeatVersion;
    }

    /**
     * Sets the 'is loading' latch, so that something can wait while the load
     * sequence is running.
     */
    public void setIsLoading() {
        isLoadingGate = new CountDownLatch(1);
    }

    /** Waits on the 'is loading' latch. */
    public void waitOnLoading() {
        if (isLoadingGate == null) {
            return;
        }
        try {
            isLoadingGate.await();
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * When loading is done, this latch is opened and whatever is waiting on it
     * is notified.
     */
    public void setLoadingDone() {
        isLoadingGate.countDown();
    }

    /**
     * When loading is done but with an error. Currently it is the same as
     * setLoadingDone().
     */
    void setLoadingError() {
        isLoadingGate.countDown();
    }

    /** Waits for the server status latch. */
    public void waitForServerStatusLatch() {
        try {
            waitForServerStatusLatch.await();
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /** The latch is set when the server status is run for the first time. */
    public void serverStatusLatchDone() {
        waitForServerStatusLatch.countDown();
    }

    /** Returns true if latch is set. */
    public boolean getWaitForServerStatusLatch() {
        return waitForServerStatusLatch.getCount() == 1;
    }

    public String getSSHPort() {
        return sshPort;
    }

    public int getSSHPortInt() {
        return Integer.valueOf(sshPort);
    }

    /** Sets ssh port. */
    public void setSSHPort(final String sshPort) {
        if (this.sshPort != null && !sshPort.equals(this.sshPort)) {
            ssh.disconnect();
        }
        this.sshPort = sshPort;
    }

    public String getSudoPassword() {
        return sudoPassword;
    }

    public void setSudoPassword(final String sudoPassword) {
        this.sudoPassword = sudoPassword;
    }

    public Boolean isUseSudo() {
        return useSudo;
    }

    public void setUseSudo(final Boolean useSudo) {
        this.useSudo = useSudo;
    }

    public void setPacemakerInstallMethodIndex(final String pacemakerInstallMethodIndex) {
        this.pacemakerInstallMethodIndex = pacemakerInstallMethodIndex;
    }

    public String getPacemakerInstallMethodIndex() {
        return pacemakerInstallMethodIndex;
    }

    public void setHeartbeatPacemakerInstallMethodIndex(final String heartbeatPacemakerInstallMethodIndex) {
        this.heartbeatPacemakerInstallMethodIndex = heartbeatPacemakerInstallMethodIndex;
    }

    public String getHeartbeatPacemakerInstallMethodIndex() {
        return heartbeatPacemakerInstallMethodIndex;
    }

    public boolean isCorosyncInRc() {
       return corosyncInRc;
    }

    public boolean isOpenaisInRc() {
       return openaisInRc;
    }

    public boolean isPacemakerInRc() {
       return pacemakerInRc;
    }

    public boolean hasHeartbeatInitScript() {
       return heartbeatHasInitScript;
    }

    public boolean hasCorosyncInitScript() {
       return corosyncHasInitScript;
    }

    public boolean hasOpenaisInitScript() {
       return openaisHasInitScript;
    }

    public boolean hasPacemakerInitScript() {
       return pacemakerHasInitScript;
    }

    public boolean isCorosyncRunning() {
       return corosyncRunning;
    }

    public boolean isPacemakerRunning() {
       return pacemakerRunning;
    }

    public boolean isOpenaisRunning() {
       return openaisRunning;
    }

    public boolean corosyncOrOpenaisConfigExists() {
       return corosyncOrOpenaisConfigExists;
    }

    public boolean isHeartbeatInRc() {
       return heartbeatInRc;
    }

    public boolean isHeartbeatRunning() {
       return heartbeatRunning;
    }

    public boolean heartbeatConfigExists() {
       return heartbeatConfigExists;
    }

    /** Returns MD5 checksum of VM Info from server. */
    public String getVMInfoMD5() {
        return vmInfoFromServerMD5;
    }

    /** Sets MD5 checksum of VM Info from server. */
    public void setVMInfoMD5(final String vmInfoMD5) {
        this.vmInfoFromServerMD5 = vmInfoMD5;
    }

    public void setPositionInTheCluster(final int positionInTheCluster) {
        this.positionInTheCluster = positionInTheCluster;
    }

    public int getPositionInTheCluster() {
        return positionInTheCluster;
    }

    /** This is part of testsuite. */
    boolean checkTest(final String checkCommand,
                      final String test,
                      final double index,
                      final String name,
                      final int maxHosts) {
        Tools.sleep(1500);
        final StringBuilder command = new StringBuilder(50);
        command.append(DistResource.SUDO).append(replaceVars("@GUI-HELPER@"));
        command.append(' ');
        command.append(checkCommand);
        command.append(' ');
        command.append(test);
        command.append(' ');
        final String indexString = Double.toString(index).replaceFirst("\\.0+$", "");
        command.append(indexString);
        if (name != null) {
            command.append(' ');
            command.append(name);
        }
        int h = 1;
        for (final Host host : getCluster().getHosts()) {
            LOG.debug1("checkTest: host" + h + " = " + host.getName());
            command.append(' ');
            command.append(host.getName());
            if (maxHosts > 0 && h >= maxHosts) {
                break;
            }
            h++;
        }
        command.append(" 2>&1");
        int i = 0;
        SshOutput out;
        do {
            out = getSSH().execCommandAndWait(new ExecCommandConfig().command(command.toString())
                                                                     .sshCommandTimeout(60000));
            if (out.getExitCode() == 0 || out.getExitCode() == 10) {
                break;
            }
            i++;
            roboTest.sleepNoFactor(i * 2000);
        } while (i < 5);
        String nameS = ' ' + name;
        if (name == null) {
            nameS = "";
        }
        if (i > 0) {
            roboTest.info(getName() + ' ' + test + ' ' + index + nameS + " tries: " + (i + 1));
        }
        roboTest.info(getName() + ' ' + test + ' ' + index + nameS + ' ' + out.getOutput());
        return out.getExitCode() == 0;
    }

    /** This is part of testsuite, it checks Pacemaker. */
    public boolean checkPCMKTest(final String test, final double index) {
        return checkTest("gui-test", test, index, null, 0);
    }

    /** This is part of testsuite, it checks DRBD. */
    public boolean checkDRBDTest(final String test, final double index) {
        final StringBuilder testName = new StringBuilder(20);
        if (application.getBigDRBDConf()) {
            testName.append("big-");
        }
        if (!hasVolumes()) {
            testName.append("novolumes-");
        }
        testName.append(test);
        return checkTest("gui-drbd-test", testName.toString(), index, null, 2);
    }

    /** This is part of testsuite, it checks VMs. */
    public boolean checkVMTest(final String test, final double index, final String name) {
        return checkTest("gui-vm-test", test, index, name, 0);
    }

    /** Returns color of this host. Null if it is default color. */
    public String getColor() {
        if (savedHostColorInGraphs == null || defaultHostColorInGraph == savedHostColorInGraphs) {
            return null;
        }
        return Integer.toString(savedHostColorInGraphs.getRGB());
    }

    /** Sets color of this host. Don't if it is default color. */
    public void setSavedColor(final String colorString) {
        try {
            savedHostColorInGraphs = new Color(Integer.parseInt(colorString));
        } catch (final NumberFormatException e) {
            LOG.appWarning("setSavedColor: could not parse: " + colorString);
            /* ignore it */
        }
    }

    public long getFreeInVolumeGroup(final String volumeGroup) {
        final Long f = volumeGroups.get(volumeGroup);
        if (f == null) {
            return 0;
        }
        return f;
    }

    public Set<String> getVolumeGroupNames() {
        return volumeGroups.keySet();
    }

    public Boolean getCorosyncOrHeartbeatRunning() {
        return corosyncOrHeartbeatRunning;
    }

    public void setCorosyncOrHeartbeatRunning(final Boolean corosyncOrHeartbeatRunning) {
        this.corosyncOrHeartbeatRunning = corosyncOrHeartbeatRunning;
    }

    public boolean isCommLayerStopping() {
        return commLayerStopping;
    }

    public void setCommLayerStopping(final boolean commLayerStopping) {
        this.commLayerStopping = commLayerStopping;
    }

    public boolean isCommLayerStarting() {
        return commLayerStarting;
    }

    public void setCommLayerStarting(final boolean commLayerStarting) {
        this.commLayerStarting = commLayerStarting;
    }

    public boolean isPacemakerStarting() {
        return pacemakerStarting;
    }

    public void setPacemakerStarting(final boolean pacemakerStarting) {
        this.pacemakerStarting = pacemakerStarting;
    }

    public boolean isDrbdProxyStarting() {
        return drbdProxyStarting;
    }

    public void setDrbdProxyStarting(final boolean drbdProxyStarting) {
        this.drbdProxyStarting = drbdProxyStarting;
    }

    public boolean isPcmkStartedByCorosync() {
        return pcmkServiceVersion == 0;
    }

    public void setLibvirtVersion(final String libvirtVersion) {
        this.libvirtVersion = libvirtVersion;
    }

    public String getLibvirtVersion() {
        return libvirtVersion;
    }

    public Set<String> getLogicalVolumesFromVolumeGroup(final String vg) {
        return volumeGroupsWithLvs.get(vg);
    }

    public Set<String> getAllLogicalVolumes() {
        final Set<String> allLVS = new LinkedHashSet<String>();
        for (final String vg : volumeGroups.keySet()) {
            final Set<String> lvs = volumeGroupsWithLvs.get(vg);
            if (lvs != null) {
                allLVS.addAll(lvs);
            }
        }
        return allLVS;
    }

    /** Returns whether DRBD has volume feature. */
    public boolean hasVolumes() {
        try {
            return Tools.compareVersions(drbdHost.getDrbdUtilVersion(), "8.4") >= 0;
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("hasVolumes: " + e.getMessage(), e);
        }
        return true;
    }

    public Iterable<BlockDevice> getPhysicalVolumes() {
        return physicalVolumes;
    }

    public void setSavable(final boolean savable) {
        this.savable = savable;
    }

    public boolean isSavable() {
        return savable;
    }

    public BlockDevice getDrbdBlockDevice(final String device) {
        return drbdBlockDevices.get(device);
    }

    public Iterable<BlockDevice> getDrbdBlockDevices() {
        return drbdBlockDevices.values();
    }

    public Iterable<BlockDevice> getPhysicalVolumes(final String vg) {
        final Collection<BlockDevice> bds = new ArrayList<BlockDevice>();
        if (vg == null) {
            return bds;
        }
        for (final BlockDevice b : physicalVolumes) {
            if (vg.equals(b.getVolumeGroupOnPhysicalVolume())) {
                bds.add(b);
            }
        }
        return bds;
    }

    public boolean drbdStatusTryLock() {
        return mDRBDStatusLock.tryLock();
    }

    public void drbdStatusLock() {
        mDRBDStatusLock.lock();
    }

    public void drbdStatusUnlock() {
        mDRBDStatusLock.unlock();
    }

    public void vmStatusLock() {
        mUpdateVMSlock.lock();
    }

    public boolean vmStatusTryLock() {
        return mUpdateVMSlock.tryLock();
    }

    public void vmStatusUnlock() {
        mUpdateVMSlock.unlock();
    }

    public boolean isRoot() {
        return ROOT_USER.equals(username);
    }

    public Iterable<String> getGuiOptions(final String name) {
        final List<String> opts = guiOptions.get(name);
        if (opts == null) {
            return new ArrayList<String>();
        }
        return new ArrayList<String>(guiOptions.get(name));
    }

    public boolean isDrbdProxyUp(final String drbdResource) {
        return drbdResourcesWithProxy.contains(drbdResource);
    }

    public void updateDrbdParameters() {
        final ClusterBrowser cb = getBrowser().getClusterBrowser();
        final DrbdXml drbdXml = cb.getDrbdXml();
        final String output = drbdXml.updateDrbdParameters(this);
        if (output != null) {
            drbdXml.parseDrbdParameters(this, output, cb.getClusterHosts());
            cb.getHostDrbdParameters().put(this, output);
        }
    }

    /** Compares ignoring case. */
    @Override
    public int compareTo(final Host h) {
        return Tools.compareNames(getName(), h.getName());
    }

    @Override
    public String getValueForGui() {
        return getName();
    }

    @Override
    public String getValueForConfig() {
        return getName();
    }

    @Override
    public boolean isNothingSelected() {
        return getName() == null;
    }

    @Override
    public Unit getUnit() {
        return null;
    }

    @Override
    public String getValueForConfigWithUnit() {
        return getValueForConfig();
    }

    @Override
    public String getNothingSelected() {
        return NOTHING_SELECTED;
    }

    public void setDrbdStatusOk(final boolean drbdStatusOk) {
        this.drbdStatusOk = drbdStatusOk;
        resetDrbdOnBlockDevices(drbdStatusOk);
    }

    public boolean isDrbdStatusOk() {
        return drbdStatusOk;
    }

    public String isDrbdUtilCompatibleWithDrbdModule() {
        if (!DRBD.compatibleVersions(drbdHost.getDrbdUtilVersion(), drbdHost.getDrbdModuleVersion())) {
            return "DRBD util and module versions are not compatible: "
                    + drbdHost.getDrbdUtilVersion()
                    + " / "
                    + drbdHost.getDrbdModuleVersion();
        }
        return null;
    }

    public String getDrbdInfoAboutInstallation() {
        final StringBuilder tt = new StringBuilder(40);
        final String drbdV = drbdHost.getDrbdUtilVersion();
        final String drbdModuleV = drbdHost.getDrbdModuleVersion();
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
        if (drbdHost.isDrbdLoaded()) {
            tt.append(" (running)");
        } else {
            tt.append(" (not loaded)");
        }
        return tt.toString();
    }

    public void waitForHostAndDrbd() {
        while (!isConnected() || !drbdHost.isDrbdLoaded()) {
            try {
                Thread.sleep(10000);
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
    public boolean drbdVersionHigherOrEqual(final String drbdVersion) throws Exceptions.IllegalVersionException {
        return Tools.compareVersions(drbdHost.getDrbdUtilVersion(), drbdVersion) >= 0;
    }

    public boolean drbdVersionSmaller(final String drbdVersion) throws Exceptions.IllegalVersionException {
        return Tools.compareVersions(drbdHost.getDrbdUtilVersion(), drbdVersion) < 0;
    }

    public boolean isDrbdLoaded() {
        return drbdHost.isDrbdLoaded();
    }

    public boolean isDrbdProxyRunning() {
        return drbdHost.isDrbdProxyRunning();
    }

    public boolean hasDrbd() {
        return drbdHost.getDrbdUtilVersion() != null;
    }

    public boolean drbdVersionSmallerOrEqual(final String drbdVersion) throws Exceptions.IllegalVersionException {
        return Tools.compareVersions(drbdHost.getDrbdUtilVersion(), drbdVersion) <= 0;
    }

    private void resetDrbdOnBlockDevices(boolean drbdStatus) {
        if (!drbdStatus) {
            for (final BlockDevice b : getBlockDevices()) {
                b.resetDrbd();
            }
        }
    }
}
