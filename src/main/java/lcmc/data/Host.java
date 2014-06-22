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

package lcmc.data;

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

import javax.swing.JComponent;

import lcmc.Exceptions;
import lcmc.configs.DistResource;
import lcmc.data.drbd.DrbdInstallation;
import lcmc.data.drbd.DrbdHost;
import lcmc.data.drbd.DrbdXML;
import lcmc.data.resources.BlockDevice;
import lcmc.data.resources.NetInterface;
import lcmc.data.vm.VMSXML;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.HostBrowser;
import lcmc.gui.ProgressBar;
import lcmc.gui.ResourceGraph;
import lcmc.gui.SSHGui;
import lcmc.gui.TerminalPanel;
import lcmc.gui.resources.CategoryInfo;
import lcmc.robotest.RoboTest;
import lcmc.utilities.*;
import lcmc.utilities.ssh.ExecCommandConfig;
import lcmc.utilities.ssh.Ssh;
import lcmc.utilities.ssh.ExecCommandThread;
import lcmc.utilities.ssh.SshOutput;


/**
 * This class holds host data and implementation of host related methods.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class Host implements Comparable<Host>, Value {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(Host.class);
    /** String that is displayed as a tool tip for disabled menu item. */
    public static final String NOT_CONNECTED_STRING =
                                                   "not connected to the host";
    /** String that is displayed as a tool tip for disabled menu item. */
    public static final String PROXY_NOT_CONNECTED_STRING =
                                             "not connected to the proxy host";
    /** Block device with number pattern. */
    public static final Pattern BDP = Pattern.compile("(\\D+)\\d+");
    /** DRBD bd pattern. */
    public static final Pattern DRBDP = Pattern.compile(".*\\/drbd\\d+$");
    /** Block device / used pattern. */
    public static final Pattern DISK_SPACE_P =
                                            Pattern.compile("^(.*) (\\d+)$");
    /** Timeout after which the connection is considered to be dead. */
    private static final int PING_TIMEOUT           = 40000;
    private static final int DRBD_EVENTS_TIMEOUT    = 40000;
    private static final int CLUSTER_EVENTS_TIMEOUT = 40000;
    private static final int HW_INFO_TIMEOUT        = 40000;

    public static final String DEFAULT_HOSTNAME = "unknown";

    /** Choices for gui drop down menus. */
    public static final String VM_FILESYSTEM_SOURCE_DIR_LXC =
                                                "vm.filesystem.source.dir.lxc";

    /** Root user name. */
    public static final String ROOT_USER = "root";
    /** Default Ssh port. */
    public static final String DEFAULT_SSH_PORT = "22";
    /** Log commands on the servers. */
    private static final String GUI_HELPER_CMD_LOG_OP = "--cmd-log";

    private static final String NET_INFO            = "net-info";
    private static final String BRIDGE_INFO         = "bridge-info";
    private static final String DISK_INFO           = "disk-info";
    private static final String DISK_SPACE          = "disk-space";
    private static final String VG_INFO             = "vg-info";
    private static final String FILESYSTEMS_INFO    = "filesystems-info";
    private static final String CRYPTO_INFO         = "crypto-info";
    private static final String QEMU_KEYMAPS_INFO   = "qemu-keymaps-info";
    private static final String CPU_MAP_MODEL_INFO  = "cpu-map-model-info";
    private static final String CPU_MAP_VENDOR_INFO = "cpu-map-vendor-info";
    private static final String MOUNT_POINTS_INFO   = "mount-points-info";
    private static final String GUI_INFO            = "gui-info";
    private static final String INSTALLATION_INFO   = "installation-info";
    private static final String GUI_OPTIONS_INFO    = "gui-options-info";
    private static final String VERSION_INFO        = "version-info";
    private static final String DRBD_PROXY_INFO     = "drbd-proxy-info";

    private static final Collection<String> INFO_TYPES =
             new HashSet<String>(Arrays.asList(new String[]{NET_INFO,
                                                            BRIDGE_INFO,
                                                            DISK_INFO,
                                                            DISK_SPACE,
                                                            VG_INFO,
                                                            FILESYSTEMS_INFO,
                                                            CRYPTO_INFO,
                                                            QEMU_KEYMAPS_INFO,
                                                            CPU_MAP_MODEL_INFO,
                                                            CPU_MAP_VENDOR_INFO,
                                                            MOUNT_POINTS_INFO,
                                                            GUI_INFO,
                                                            INSTALLATION_INFO,
                                                            GUI_OPTIONS_INFO,
                                                            VERSION_INFO,
                                                            DRBD_PROXY_INFO}));
    public static final boolean UPDATE_LVM = true;
    /** Name of the host. */
    private String name;
    /** Hostname as entered by the user. Could be ipAddress, hostname with or without
     * the domain name. */
    private String hostnameEntered = Tools.getDefault("SSH.Host");
    /** Ip of the host. */
    private String ipAddress;
    /** Ips in the combo in Dialog.Host.Configuration. */
    private final Map<Integer, String[]> ips = new HashMap<Integer, String[]>();
    private Cluster cluster = null;
    /** Hostname of the host. */
    private String hostname = DEFAULT_HOSTNAME;
    /** Username, root most of the times. */
    private String username = null;
    /** Detected kernel name. */
    private String detectedKernelName = "";
    /** Detected distribution. */
    private String detectedDist = "";
    /** Detected distribution version. */
    private String detectedDistVersion = "";
    /** Detected kernel version. */
    private String detectedKernelVersion = "";
    /** Detected kernel architecture. */
    private String detectedArch = "";
    /** Kernel name (could be different than detected). */
    private String kernelName = "";
    /** Distribution (could be different than detected). */
    private String dist = "";
    /** Distribution version (could be different than detected). */
    private String distVersion = "";
    /** Distribution version string (could be different than detected). */
    private String distVersionString = "";
    /** Kernel version (could be different than detected). */
    private String kernelVersion = "";
    /** Kernel architecture (could be different than detected). */
    private String arch = "";
    /** Map of network interfaces of this host with bridges. */
    private List<NetInterface> netInterfaces = new ArrayList<NetInterface>();
    /** Bridges. */
    private List<Value> bridges = new ArrayList<Value>();
    /** Available file systems. */
    private Set<String> fileSystems = new TreeSet<String>();
    /** Available crypto modules. */
    private Set<String> cryptoModules = new TreeSet<String>();
    /** Available qemu keymaps. */
    private Set<Value> qemuKeymaps = new TreeSet<Value>();
    /** Available libvirt cpu models. */
    private Set<Value> cpuMapModels = new TreeSet<Value>();
    /** Available libvirt cpu vendors. */
    private Set<Value> cpuMapVendors = new TreeSet<Value>();
    /** Mount points that exist in /mnt dir. */
    private Set<String> mountPoints = new TreeSet<String>();
    /** List of block devices of this host. */
    private Map<String, BlockDevice> blockDevices =
                                      new LinkedHashMap<String, BlockDevice>();
    /** List of drbd block devices of this host. */
    private Map<String, BlockDevice> drbdBlockDevices =
                                      new LinkedHashMap<String, BlockDevice>();
    /** Options for GUI drop down lists. */
    private Map<String, List<String>> guiOptions =
                                          new HashMap<String, List<String>>();
    /** Resources on which proxy connection is up. */
    private Set<String> drbdResProxy = new HashSet<String>();
    /** Color of this host in graphs. */
    private Color defaultColor;
    /** Color of this host in graphs. */
    private Color savedColor;
    /** Thread where drbd status command is running. */
    private ExecCommandThread drbdStatusThread = null;
    /** Thread where hb status command is running. */
    private ExecCommandThread clStatusThread = null;
    /** Thread where server status command is running. */
    private ExecCommandThread serverStatusThread = null;
    /** List of positions of the services.
     *  Question is this: the saved positions can be different on different
     *  hosts, but only one can be used in the hb graph.
     *  Only one will be used and by next save the problem solves itself.
     */
    private final Map<String, Point2D> servicePositions =
                                            new HashMap<String, Point2D>();
    /** Pacemaker version. */
    private String pacemakerVersion = null;
    /** Openais version. */
    private String openaisVersion = null;
    /** Whether the comm layer is stopping. */
    private boolean commLayerStopping = false;
    /** Whether the comm layer is starting. */
    private boolean commLayerStarting = false;
    /** Whether the pcmk is starting. */
    private boolean pcmkStarting = false;
    /** Whether the drbd proxy is starting. */
    private boolean drbdProxyStarting = false;
    /** Is "on" if corosync is in rc. */
    private boolean csIsRc = false;
    /** Is "on" if openais is in rc. */
    private boolean aisIsRc = false;
    /** Is "on" if heartbeat has an init script. */
    private boolean heartbeatInit = false;
    /** Is "on" if corosync has an init script. */
    private boolean csInit = false;
    /** Is "on" if openais has an init script. */
    private boolean aisInit = false;
    /** Is "on" if corosync is running. */
    private boolean csRunning = false;
    /** Is "on" if openais is running. */
    private boolean aisRunning = false;
    /** Is "on" if corosync/openais config exists. */
    private boolean csAisConf = false;
    /** Is "on" if heartbeat is in rc. */
    private boolean heartbeatIsRc = false;
    /** Is "on" if heartbeat is running. */
    private boolean heartbeatRunning = false;
    /** Is "on" if heartbeat config exists. */
    private boolean heartbeatConf = false;
    /** Is "on" if pacemaker is in rc. */
    private boolean pcmkIsRc = false;
    /** Is "on" if pacemaker is running. */
    private boolean pcmkRunning = false;
    /** Is "on" if pacemaker has an init script. */
    private boolean pcmkInit = false;
    /** Pacemaker service version. From version 1, use pacamker init script. */
    private int pcmkServiceVersion = -1;
    /** Corosync version. */
    private String corosyncVersion = null;
    /** Heartbeat version. */
    private String heartbeatVersion = null;
    /** Whether heartbeat status is ok. */
    private boolean clStatus = false;

    /** Ssh object of the connection to this host. */
    private final Ssh ssh = new Ssh();
    /** Terminal panel of this host. */
    private TerminalPanel terminalPanel = null;
    /** Ssh port. */
    private String sshPort = null;
    /** Whether sudo should be used. */
    private Boolean useSudo = null;
    /** Sudo password. */
    private String sudoPassword = "";
    /** Browser panel (the one with menus and all the logic) of this host. */
    private HostBrowser browser;
    /** A gate that is used to synchronize the loading sequence. */
    private CountDownLatch isLoadingGate;
    /** A gate that waits for server status. */
    private final CountDownLatch serverStatusLatch = new CountDownLatch(1);
    /** List of gui elements that are to be enabled if the host is connected.*/
    private final Collection<JComponent> enableOnConnectList =
                                                   new ArrayList<JComponent>();
    /** Corosync/Openais/pacemaker installation method index. */
    private String pmInstallMethod;
    /** Heartbeat/pacemaker installation method index. */
    private String hbPmInstallMethod;
    /** Heartbeat lib path. */
    private String hbLibPath = null;
    /** MD5 checksum of VM Info from server. */
    private String vmInfoMD5 = null;
    /** Index of this host in its cluster. */
    private int index = 0;
    /** Whether the last connection check was positive. */
    private volatile boolean lastConnected = false;
    /** Whether corosync or heartbeat is running. */
    private Boolean corosyncHeartbeatRunning = null;
    /** Libvirt version. */
    private String libvirtVersion = null;
    /** Physical volumes on this host. */
    private List<BlockDevice> physicalVolumes = new ArrayList<BlockDevice>();
    /** Volume group information on this host. */
    private Map<String, Long> volumeGroups = new LinkedHashMap<String, Long>();
    /** Volume group with all lvs in it. */
    private Map<String, Set<String>> volumeGroupsLVS =
                                            new HashMap<String, Set<String>>();
    /** Whether this cluster should be saved. */
    private boolean savable = true;
    /** Ping is set every 10s. */
    private volatile AtomicBoolean ping = new AtomicBoolean(true);
    /** Global drbd status lock. */
    private final Lock mDRBDStatusLock = new ReentrantLock();
    /** Update VMS lock. */
    private final Lock mUpdateVMSlock = new ReentrantLock();
    /** Time stamp lock. */
    private final Lock mInfoTimestampLock = new ReentrantLock();
    /** Time stamp hash. */
    private final Map<String, Double> infoTimestamp =
                                                new HashMap<String, Double>();
    /** Whether the host is member of the cluster. */
    private boolean inCluster = false;
    /** Whether dist info was already logged. */
    private boolean distInfoLogged = false;

    private final DrbdHost drbdHost;
    /** Whether drbd status is ok. */
    private boolean drbdStatus = false;

    private Host(final DrbdHost drbdHost) {
        this.drbdHost = drbdHost;
        if (Tools.getApplication().getHosts().size() == 1) {
            hostnameEntered = Tools.getDefault("SSH.SecHost");
        }
        browser = new HostBrowser(this);
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                browser.initHostResources();
            }
        });
        mountPoints.add("/mnt/");
    }

    public static Host createInstance() {
        return new Host(new DrbdHost());
    }

    public static Host createInstance(final String ipAddress) {
        final Host instance = createInstance();
        instance.ipAddress = ipAddress;
        return instance;
    }

    public HostBrowser getBrowser() {
        return browser;
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
            LOG.debug1("setCluster: " + getName() + " set cluster name: "
                       + cluster.getName());
        }
    }

    /** Remove host from the cluster. */
    public void removeFromCluster() {
        inCluster = false;
    }

    /** Returns the cluster data object. */
    public Cluster getCluster() {
        return cluster;
    }

    /** Returns color objects of this host for drbd graph. */
    public Color[] getDrbdColors() {
        if (defaultColor == null) {
            defaultColor = Tools.getDefaultColor("Host.DefaultColor");
        }
        final Color col;
        if (savedColor == null) {
            col = defaultColor;
        } else {
            col = savedColor;
        }
        final Color secColor;
        if (isConnected()) {
            if (isDrbdStatus() && drbdHost.isDrbdLoaded()) {
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
        if (defaultColor == null) {
            defaultColor = Tools.getDefaultColor("Host.DefaultColor");
        }
        final Color col;
        if (savedColor == null) {
            col = defaultColor;
        } else {
            col = savedColor;
        }
        final Color secColor;
        if (isConnected()) {
            if (isClStatus()) {
                return new Color[]{col};
            } else {
                secColor = Tools.getDefaultColor("Host.NoStatusColor");
            }
        } else {
            secColor = Tools.getDefaultColor("Host.ErrorColor");
        }
        return new Color[]{col, secColor};
    }

    /** Sets color of the host. */
    void setColor(final Color defaultColor) {
        this.defaultColor = defaultColor;
        if (savedColor == null) {
            savedColor = defaultColor;
        }
        if (terminalPanel != null) {
            terminalPanel.resetPromptColor();
        }
    }

    /** Sets color of the host, when it was saved. */
    public void setSavedColor(final Color savedColor) {
        this.savedColor = savedColor;
        if (terminalPanel != null) {
            terminalPanel.resetPromptColor();
        }
    }

    /** Sets if hb status failed or not. */
    public void setClStatus(final boolean clStatus) {
        this.clStatus = clStatus;
    }

    /** Returns whether cluster status is available. */
    public boolean isClStatus() {
        return clStatus && isConnected();
    }

    /** Returns true when this host is in a cluster. */
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
    public void setHostnameEntered(final String hostnameEntered) {
        if (hostnameEntered != null
            && !hostnameEntered.equals(this.hostnameEntered)) {
            /* back button and hostname changed */
            setName(null);
            setIpAddress(null);
            setHostname(null);
        }
        this.hostnameEntered = hostnameEntered;
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

    /** Sets ips. */
    public void setIps(final int hop, final String[] ipsForHop) {
        ips.put(hop, ipsForHop);
    }

    /** Returns net interfaces. */
    public NetInterface[] getNetInterfaces() {
        return netInterfaces.toArray(new NetInterface[netInterfaces.size()]);
    }

    /** Get net interfaces that are bridges. */
    public List<Value> getBridges() {
        return new ArrayList<Value>(bridges);
    }

    /** Returns blockDevices. */
    public BlockDevice[] getBlockDevices() {
        return blockDevices.values().toArray(
                                    new BlockDevice[blockDevices.size()]);
    }

    /**
     * Returns blockDevices as array list of device names. Removes the
     * ones that are in the drbd and are already used in CRM.
     */
    List<String> getBlockDevicesNames() {
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
    List<String> getBlockDevicesNamesIntersection(
                                        final Iterable<String> otherBlockDevices) {
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

    /** Returns network ips as array list. */
    Map<String, Integer> getNetworkIps() {
        final Map<String, Integer> networkIps =
                                         new LinkedHashMap<String, Integer>();
        for (final NetInterface ni : netInterfaces) {
            final String netIp = ni.getNetworkIp();
            networkIps.put(netIp, ni.getCidr());
        }
        return networkIps;
    }

    /** Returns list of networks that exist on all hosts. */
    public Map<String, Integer> getNetworksIntersection(
                                final Map<String, Integer> otherNetworkIps) {
        if (otherNetworkIps == null) {
            return getNetworkIps();
        }
        final Map<String, Integer> networksIntersection =
                                         new LinkedHashMap<String, Integer>();
        for (final NetInterface ni : netInterfaces) {
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

    /** Returns ips that belong the the network. */
    List<String> getIpsFromNetwork(final String netIp) {
        final List<String> networkIps = new ArrayList<String>();
        for (final NetInterface ni : netInterfaces) {
            if (netIp.equals(ni.getNetworkIp())) {
                networkIps.add(ni.getIp());
            }
        }
        return networkIps;
    }

    /** Returns BlockDevice object identified with device name. */
    BlockDevice getBlockDevice(final String device) {
        return blockDevices.get(device);
    }

    /** Removes file system from the list of file systems. */
    void removeFileSystems() {
        fileSystems.clear();
    }

    /** Returns available file systems. */
    String[] getFileSystems() {
        return fileSystems.toArray(new String [fileSystems.size()]);
    }

    /** Returns available file systems devices as a list of strings. */
    Set<String> getFileSystemsList() {
        return fileSystems;
    }

    /** Returns available crypto modules as a list of strings. */
    public Set<String> getCryptoModules() {
        return cryptoModules;
    }

    /** Returns available qemu keymaps as a list of strings. */
    public Set<Value> getQemuKeymaps() {
        return qemuKeymaps;
    }

    /** Returns available libvirt's cpu map models. */
    public Set<Value> getCPUMapModels() {
        return cpuMapModels;
    }

    /** Returns available libvirt's cpu map vendors. */
    public Set<Value> getCPUMapVendors() {
        return cpuMapVendors;
    }

    /** Returns mount points as a list of strings. */
    Set<String> getMountPointsList() {
        return mountPoints;
    }

    /** Returns ips of this host. */
    public String[] getIps(final int hop) {
        return ips.get(hop);
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
        if (!distInfoLogged) {
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
        dist = detectedDist;
        distVersion = detectedDistVersion;
        initDistInfo();
        if (!distInfoLogged) {
            LOG.debug1("setDistInfo: kernel name: " + detectedKernelName);
            LOG.debug1("setDistInfo: kernel version: " + detectedKernelVersion);
            LOG.debug1("setDistInfo: arch: " + detectedArch);
            LOG.debug1("setDistInfo: dist version: " + detectedDistVersion);
            LOG.debug1("setDistInfo: dist: " + detectedDist);
        }
        distInfoLogged = true;
    }

    /** Initializes dist info. Must be called after setDistInfo. */
    void initDistInfo() {
        if (!"Linux".equals(detectedKernelName)) {
            LOG.appWarning("initDistInfo: detected kernel not linux: "
                             + detectedKernelName);
        }
        setKernelName("Linux");

        if (!dist.equals(detectedDist)) {
            LOG.appError("initDistInfo: dist: " + dist + " does not match " + detectedDist);
        }
        distVersionString = Tools.getDistVersionString(dist, distVersion);
        distVersion = Tools.getDistString("distributiondir",
                                          detectedDist,
                                          distVersionString,
                                          null);
        setKernelVersion(Tools.getKernelDownloadDir(detectedKernelVersion,
                                                    getDist(),
                                                    distVersionString,
                                                    null));
        String arch0 = Tools.getDistString("arch:" + detectedArch,
                                           getDist(),
                                           distVersionString,
                                           null);
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
    String getDistFromDistVersion(final String dV) {
        /* remove numbers */
        if ("No Match".equals(dV)) {
            return null;
        }
        LOG.debug1("getDistFromDistVersion:" + dV.replaceFirst("\\d.*", ""));
        return Tools.getDistString("dist:" + dV.replaceFirst("\\d.*", ""),
                                   "",
                                   "",
                                   null);
    }


    /** Sets distribution name. */
    void setDist(final String dist) {
        this.dist = dist;
    }

    /** Sets distribution version. */
    void setDistVersion(final String distVersion) {
        this.distVersion = distVersion;
        distVersionString = Tools.getDistVersionString(dist, distVersion);
        dist = getDistFromDistVersion(distVersion);
    }

    /** Sets arch, e.g. "i386". */
    public void setArch(final String arch) {
        this.arch = arch;
    }

    /** Sets kernel name, e.g. "linux". */
    void setKernelName(final String kernelName) {
        this.kernelName = kernelName;
    }

    /** Sets kernel version. */
    void setKernelVersion(final String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    /** Gets kernel name. Normaly "Linux" for this application. */
    String getKernelName() {
        return kernelName;
    }

    /**
     * Gets kernel version. Usually some version,
     * like: "2.6.13.2ws-k7-up-lowmem".
     */
    public String getKernelVersion() {
        return kernelVersion;
    }

    /** Returns the detected kernel version. */
    public String getDetectedKernelVersion() {
        return detectedKernelVersion;
    }

    /** Gets architecture like i686. */
    public String getArch() {
        return arch;
    }

    /** Returns heartbeat lib path. */
    public String getHeartbeatLibPath() {
        if (hbLibPath != null) {
            return hbLibPath;
        }
        if ("".equals(arch)) {
            LOG.appWarning("getHeartbeatLibPath: called to soon: unknown arch");
        } else if ("x86_64".equals(arch) || "amd64".equals(arch)) {
            return "/usr/lib64/heartbeat";
        }
        return "/usr/lib/heartbeat";
    }

    /** Returns lxc lib path. */
    public String getLxcLibPath() {
        return getDistString("libvirt.lxc.libpath");
    }

    /** Returns xen lib path. */
    public String getXenLibPath() {
        return getDistString("libvirt.xen.libpath");
    }

    /** Gets distribution, e.g., debian. */
    public String getDist() {
        return dist;
    }

    /** Gets distribution version. */
    public String getDistVersion() {
        return distVersion;
    }

    /** Gets distribution version string. */
    public String getDistVersionString() {
        return distVersionString;
    }

    /** Disconnects this host. */
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
    public String getDistCommand(final String commandString,
                                 final ConvertCmdCallback convertCmdCallback) {
        return Tools.getDistCommand(commandString,
                                    dist,
                                    distVersionString,
                                    arch,
                                    convertCmdCallback,
                                    false,  /* in bash */
                                    false); /* sudo */
    }

    /** Converts a string that is specific to the distribution distribution. */
    public String getDistString(final String commandString) {
        return Tools.getDistString(commandString,
                                   dist,
                                   distVersionString,
                                   arch);
    }

    /**
     *  Gets list of strings that are specific to the distribution
     *  distribution.
     */
    public List<String> getDistStrings(final String commandString) {
        return Tools.getDistStrings(commandString,
                                    dist,
                                    distVersionString,
                                    arch);
    }


    /**
     * Converts command string to real command for a distribution, specifying
     * what-with-what hash.
     */
    public String getDistCommand(final String commandString,
                                 final Map<String, String> replaceHash) {
        return Tools.getDistCommand(
                    commandString,
                    dist,
                    distVersionString,
                    arch,
                    new ConvertCmdCallback() {
                        @Override
                        public String convert(String command) {
                            for (final String tag : replaceHash.keySet()) {
                                if (tag != null && command.indexOf(tag) > -1) {
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
        Tools.startProgressIndicator(hostName, text);
        try {
            return ssh.captureCommand(execCommandConfig);
        } finally {
            Tools.stopProgressIndicator(hostName, text);
        }
    }

    public void execCommandProgressIndicator(final String text, final ExecCommandConfig execCommandConfig) {
        final String hostName = getName();
        Tools.startProgressIndicator(hostName, text);
        try {
            ssh.execCommand(execCommandConfig);
        } finally {
            Tools.stopProgressIndicator(hostName, text);
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
    public void execDrbdStatusCommand(final ExecCallback execCallback,
                                      final NewOutputCallback outputCallback) {
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

    /** Waits till the drbd status command finishes. */
    public void waitOnDrbdStatus() {
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

    /** Executes an hb status command. */
    public void execClStatusCommand(final ExecCallback execCallback,
                                    final NewOutputCallback outputCallback) {
        if (clStatusThread == null) {
            clStatusThread = ssh.execCommand(new ExecCommandConfig()
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

    /** Waits while the hb status thread finishes. */
    public void waitOnClStatus() {
        final ExecCommandThread cst = clStatusThread;
        if (cst == null) {
            return;
        }
        try {
            cst.join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clStatusThread = null;
    }

    /** Stops hb status background process. */
    public void stopClStatus() {
        final ExecCommandThread cst = clStatusThread;
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
    /** Returns username. */
    public String getUsername() {
        return username;
    }

    /** Returns first username in a hop. */
    public String getFirstUsername() {
        final String[] usernames = username.split(",");
        return usernames[0];
    }

    /** Gets hostname as entered by user. */
    public String getHostnameEntered() {
        return hostnameEntered;
    }

    /** Returns sudo prefix. */
    String getSudoPrefix(final boolean sudoTest) {
        if (useSudo != null && useSudo) {
            if (sudoTest) {
                return "sudo -E -n ";
            } else {
                return "sudo -E -p '"
                       + Ssh.SUDO_PROMPT + "' ";
            }
        } else {
            return "";
        }
    }
    /** Returns command exclosed in sh -c "". */
    public String getSudoCommand(final String command,
                                 final boolean sudoTest) {
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
            s.append("ssh -q -A -tt -o 'StrictHostKeyChecking no' "
                     + "-o 'ForwardAgent yes' -l ");
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

    /** Returns hostname of this host. */
    public String getHostname() {
        return hostname;
    }

    /** Returns the host name. */
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
            } else if (hostnameEntered != null) {
                final int i = hostnameEntered.indexOf(',');
                if (i > 0) {
                    nodeName = hostnameEntered.substring(i + 1);
                } else {
                    nodeName = hostnameEntered;
                }
            } else {
                return ipAddress;
            }

            //final int index = nodeName.indexOf('.');
            //if (index > 0 && !Tools.checkIp(nodeName)) {
            //    return nodeName.substring(0, index);
            //} else {
            //    return nodeName;
            //}
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

    /** Gets Ssh object. */
    public Ssh getSSH() {
        return ssh;
    }


    /**
     * Sets terminal panel object. This is the panel where the commands and
     * their results are shown for every host.
     */
    public void setTerminalPanel(final TerminalPanel terminalPanel) {
        this.terminalPanel = terminalPanel;
    }

    /** Gets terminal panel object. */
    public TerminalPanel getTerminalPanel() {
        return terminalPanel;
    }

    /**
     * Connects host with ssh. Dialog is needed, in case if password etc.
     * has to be entered. Connection is made in the background, after
     * connection is established, callback.done() is called. In case
     * of error callback.doneError() is called.
     */
    public void connect(SSHGui sshGui,
                        final ConnectionCallback callback) {
        if (sshGui == null) {
            sshGui = new SSHGui(Tools.getGUIData().getMainFrame(),
                                this,
                                null);
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
    public void connect(final SSHGui sshGui,
                        final ProgressBar progressBar,
                        final ConnectionCallback callback) {
        LOG.debug1("connect: host: " + sshGui);
        ssh.connect(sshGui, progressBar, callback, this);
    }

    /**
     * Register a component that will be enabled if the host connected and
     * disabled if disconnected.
     */
    public void registerEnableOnConnect(final JComponent c) {
        if (!enableOnConnectList.contains(c)) {
            enableOnConnectList.add(c);
        }
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
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
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final JComponent c : enableOnConnectList) {
                    c.setEnabled(con);
                }
            }
        });
        if (lastConnected != con) {
            lastConnected = con;
            if (con) {
               LOG.info("setConnected: " + getName() + ": connection established");
            } else {
               LOG.info("setConnected: " + getName() + ": connection lost");
            }
            final ClusterBrowser cb = getBrowser().getClusterBrowser();
            if (cb != null) {
                cb.getCRMGraph().repaint();
                cb.getDrbdGraph().repaint();
            }
        }
    }

    /** Make an ssh connection to the host. */
    void connect(SSHGui sshGui,
                 final boolean progressIndicator,
                 final int index) {
        if (!isConnected()) {
            final String hostName = getName();
            if (progressIndicator) {
                Tools.startProgressIndicator(
                                hostName,
                                Tools.getString("Dialog.Host.SSH.Connecting")
                                + " (" + index + ')');
            }
            if (sshGui == null) {
                sshGui = new SSHGui(Tools.getGUIData().getMainFrame(),
                                    this,
                                    null);
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
                                Tools.stopProgressIndicator(
                                  hostName,
                                  Tools.getString("Dialog.Host.SSH.Connecting")
                                  + " (" + index + ')');
                            }
                        }

                        @Override
                        public void doneError(final String errorText) {
                            setLoadingError();
                            setConnected();
                            if (progressIndicator) {
                                Tools.stopProgressIndicator(
                                  hostName,
                                  Tools.getString("Dialog.Host.SSH.Connecting")
                                  + " (" + index + ')');
                                Tools.progressIndicatorFailed(
                                  hostName,
                                  Tools.getString("Dialog.Host.SSH.Connecting")
                                  + " (" + index + ')');
                                Tools.stopProgressIndicator(
                                  hostName,
                                  Tools.getString("Dialog.Host.SSH.Connecting")
                                  + " (" + index + ')');
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
    public void getHWInfo(final CategoryInfo[] infosToUpdate,
                          final ResourceGraph[] graphs,
                          final boolean updateLVM) {
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
                && (!infoTimestamp.containsKey(type)
                    || timestamp >= infoTimestamp.get(type))) {
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
    public void startHWInfoDaemon(final CategoryInfo[] infosToUpdate,
                                  final ResourceGraph[] graphs) {
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
                                 if (host.isServerStatusLatch()) {
                                     final ClusterBrowser cb =
                                              getBrowser().getClusterBrowser();
                                     cb.updateServerStatus(host);
                                 }
                                 setLoadingDone();
                             }

                             @Override
                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 if (host.isServerStatusLatch()) {
                                     final ClusterBrowser cb =
                                              getBrowser().getClusterBrowser();
                                     cb.updateServerStatus(host);
                                 }
                                 setLoadingError();
                             }
                         })
                         .newOutputCallback(new NewOutputCallback() {
                             private final StringBuffer outputBuffer =
                                                        new StringBuffer(300);
                             @Override
                             public void output(final String output) {
                                 outputBuffer.append(output);
                                 final ClusterBrowser cb =
                                              getBrowser().getClusterBrowser();
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
                                     drbdConfig = getOutput("drbd",
                                                            outputBuffer);
                                     if (drbdConfig != null) {
                                         drbdUpdate = drbdConfig;
                                     }
                                     drbdStatusUnlock();
                                 } while (hw != null
                                          || vm != null
                                          || drbdConfig != null);

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
                                     final VMSXML newVMSXML =
                                                        new VMSXML(host);
                                     if (newVMSXML.update(vmUpdate)) {
                                         cb.vmsXMLPut(host, newVMSXML);
                                         cb.updateVMS();
                                     }
                                 }
                                 if (drbdUpdate != null) {
                                     final DrbdXML dxml =
                                           new DrbdXML(cluster.getHostsArray(),
                                                       cb.getDrbdParameters());
                                     dxml.update(drbdUpdate);
                                     cb.setDrbdXML(dxml);
                                     Tools.invokeLater(new Runnable() {
                                         @Override
                                         public void run() {
                                             cb.getDrbdGraph().getDrbdInfo().setParameters();
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
                                 if (isServerStatusLatch()) {
                                     cb.updateServerStatus(host);
                                 }
                                 setLoadingDone();
                             }
                         })
                         .silentCommand()
                         .silentOutput()
                         .sshCommandTimeout(HW_INFO_TIMEOUT)).block();
    }

    /** Starts connection status. */
    public void startConnectionStatus() {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (ping.get()) {
                       LOG.debug2("startConnectionStatus: connection ok on "
                                  + getName());
                       setConnected();
                       ping.set(false);
                    } else {
                       LOG.debug2("startConnectionStatus: connection lost on "
                                  + getName());
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
        if (command.indexOf("@USER@") > -1) {
            command = command.replaceAll(
                                    "@USER@",
                                    Tools.getApplication().getDownloadUser());
        }
        if (command.indexOf("@PASSWORD@") > -1) {
            if (hidePassword) {
                command = command.replaceAll("@PASSWORD@", "*****");
            } else {
                command = command.replaceAll(
                                   "@PASSWORD@",
                                   Tools.getApplication().getDownloadPassword());
            }
        }
        String supportDir = "support";
        if (Tools.getApplication().isStagingDrbd()) {
            supportDir = "support/staging";
        }
        if (kernelVersion != null
            && command.indexOf("@KERNELVERSIONDIR@") > -1) {
            command = command.replaceAll("@KERNELVERSIONDIR@", kernelVersion);
        }
        if (distVersion != null
            && command.indexOf("@DISTRIBUTION@") > -1) {
            command = command.replaceAll("@DISTRIBUTION@", distVersion);
        }
        if (arch != null
            && command.indexOf("@ARCH@") > -1) {
            command = command.replaceAll("@ARCH@", arch);
        }
        if (command.indexOf("@SUPPORTDIR@") > -1) {
            command = command.replaceAll("@SUPPORTDIR@", supportDir);
        }
        if (command.indexOf("@DRBDDIR@") > -1) {
            final String drbdDir = "drbd";
            command = command.replaceAll("@DRBDDIR@", drbdDir);
        }
        if (command.indexOf("@GUI-HELPER@") > -1) {
            final StringBuilder helperProg = new StringBuilder(
                                         "/usr/local/bin/lcmc-gui-helper-");
            helperProg.append(Tools.getRelease());
            if (Tools.getApplication().isCmdLog()) {
                helperProg.append(' ');
                helperProg.append(GUI_HELPER_CMD_LOG_OP);
            }
            command = command.replaceAll("@GUI-HELPER@", helperProg.toString());
        }
        if (command.indexOf("@GUI-HELPER-PROG@") > -1) {
            command = command.replaceAll("@GUI-HELPER-PROG@",
                                         "/usr/local/bin/lcmc-gui-helper-"
                                         + Tools.getRelease());
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

    /** Parses the host info. */
    public void parseHostInfo(final String ans) {
        LOG.debug1("parseHostInfo: updating host info: " + getName());
        final String[] lines = ans.split("\\r?\\n");
        final List<String> versionLines = new ArrayList<String>();
        final Map<String, BlockDevice> newBlockDevices =
                                     new LinkedHashMap<String, BlockDevice>();
        final Map<String, BlockDevice> newDrbdBlockDevices =
                                     new LinkedHashMap<String, BlockDevice>();
        final List<NetInterface> newNetInterfaces =
                                                new ArrayList<NetInterface>();
        final List<Value> newBridges = new ArrayList<Value>();
        final Map<String, Long> newVolumeGroups =
                                     new LinkedHashMap<String, Long>();
        final Map<String, Set<String>> newVolumeGroupsLVS =
                                     new HashMap<String, Set<String>>();
        final List<BlockDevice> newPhysicalVolumes =
                                                 new ArrayList<BlockDevice>();
        final Set<String> newFileSystems = new TreeSet<String>();
        final Set<String> newCryptoModules = new TreeSet<String>();
        final Set<Value> newQemuKeymaps = new TreeSet<Value>();
        final Set<Value> newCpuMapModels = new TreeSet<Value>();
        final Set<Value> newCpuMapVendors = new TreeSet<Value>();
        final Set<String> newMountPoints = new TreeSet<String>();

        final Map<String, List<String>> newGuiOptions =
                                          new HashMap<String, List<String>>();
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
                    if (netInterface.getIp() != null
                        && !"".equals(netInterface.getIp())) {
                        newNetInterfaces.add(netInterface);
                    }
                } catch (final UnknownHostException e) {
                    LOG.appWarning("parseHostInfo: cannot parse: net-info: "
                                   + line);
                }
            } else if (BRIDGE_INFO.equals(type)) {
                newBridges.add(new StringValue(line));
            } else if ("disk-info".equals(type)) {
                BlockDevice blockDevice = new BlockDevice(line);
                final String bdName = blockDevice.getName();
                if (bdName != null) {
                    final Matcher drbdM = DRBDP.matcher(bdName);
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
                            final Matcher m = BDP.matcher(bdName);
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
            } else if (DISK_SPACE.equals(type)) {
                final Matcher dsM = DISK_SPACE_P.matcher(line);
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
                guiOptionName = parseGuiOptionsInfo(line,
                                                    guiOptionName,
                                                    newGuiOptions);
            } else if ("version-info".equals(type)) {
                versionLines.add(line);
            } else if ("drbd-proxy-info".equals(type)) {
                /* res-other.host-this.host */
                final Cluster cl = getCluster();
                if (cl != null) {
                    String res = null;
                    if (line.startsWith("up:")) {
                        for (final Host otherHost
                                              : getCluster().getProxyHosts()) {
                            if (otherHost == this) {
                                continue;
                            }
                            final String hostsPart = '-' + otherHost.getName()
                                                     + '-' + getName();
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
                   + ", drbd: "        + drbdHost.getDrbdVersion()
                   + ", drbd module: " + drbdHost.getDrbdModuleVersion());

        if (changedTypes.contains(NET_INFO)) {
            netInterfaces = newNetInterfaces;
        }

        if (changedTypes.contains(BRIDGE_INFO)) {
            bridges = newBridges;
        }

        if (changedTypes.contains(DISK_INFO)) {
            blockDevices = newBlockDevices;
            drbdBlockDevices = newDrbdBlockDevices;
            physicalVolumes = newPhysicalVolumes;
            volumeGroupsLVS = newVolumeGroupsLVS;
        }
        if (changedTypes.contains(DISK_SPACE)) {
            for (final Map.Entry<String, String> entry
                                                : diskSpaces.entrySet()) {
                final BlockDevice bd = blockDevices.get(entry.getKey());
                if (bd != null) {
                    bd.setUsed(entry.getValue());
                }
            }
        }

        if (changedTypes.contains(VG_INFO)) {
            volumeGroups = newVolumeGroups;
        }

        if (changedTypes.contains(FILESYSTEMS_INFO)) {
            fileSystems = newFileSystems;
        }

        if (changedTypes.contains(CRYPTO_INFO)) {
            cryptoModules = newCryptoModules;
        }

        if (changedTypes.contains(QEMU_KEYMAPS_INFO)) {
            qemuKeymaps = newQemuKeymaps;
        }

        if (changedTypes.contains(CPU_MAP_MODEL_INFO)) {
            cpuMapModels = newCpuMapModels;
        }

        if (changedTypes.contains(CPU_MAP_VENDOR_INFO)) {
            cpuMapVendors = newCpuMapVendors;
        }

        if (changedTypes.contains(MOUNT_POINTS_INFO)) {
            mountPoints = newMountPoints;
        }

        if (changedTypes.contains(VERSION_INFO)) {
            setDistInfo(versionLines.toArray(new String[versionLines.size()]));
        }

        if (changedTypes.contains(GUI_OPTIONS_INFO)) {
            guiOptions = newGuiOptions;
        }

        if (changedTypes.contains(DRBD_PROXY_INFO)) {
            drbdResProxy = newDrbdResProxy;
        }

        getBrowser().updateHWResources(getNetInterfaces(),
                                       getBlockDevices(),
                                       getFileSystems());
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
            servicePositions.put(id, new Point2D.Double(
                                                  new Double(x).doubleValue(),
                                                  new Double(y).doubleValue()));
        }
    }
    /** Parses the gui options info. */
    public String parseGuiOptionsInfo(
                                    final String line,
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

    /** Parses the installation info. */
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
                aisIsRc = "on".equals(tokens[1].trim());
            } else {
                aisIsRc = false;
            }
        } else if ("cs-rc".equals(tokens[0])) {
            if (tokens.length == 2) {
                csIsRc = "on".equals(tokens[1].trim());
            } else {
                csIsRc = false;
            }
        } else if ("cs-ais-conf".equals(tokens[0])) {
            if (tokens.length == 2) {
                csAisConf = "on".equals(tokens[1].trim());
            } else {
                csAisConf = false;
            }
        } else if ("cs-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                csRunning = "on".equals(tokens[1].trim());
            } else {
                csRunning = false;
            }
        } else if ("ais-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                aisRunning = "on".equals(tokens[1].trim());
                commLayerStarting = false;
                pcmkStarting = false;
            } else {
                aisRunning = false;
            }
        } else if ("cs-init".equals(tokens[0])) {
            if (tokens.length == 2) {
                csInit = "on".equals(tokens[1].trim());
            } else {
                csInit = false;
            }
        } else if ("ais-init".equals(tokens[0])) {
            if (tokens.length == 2) {
                aisInit = "on".equals(tokens[1].trim());
            } else {
                aisInit = false;
            }
        } else if ("hb".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatVersion = tokens[1].trim();
            } else {
                heartbeatVersion = null;
            }
        } else if ("hb-init".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatInit = "on".equals(tokens[1].trim());
            } else {
                heartbeatInit = false;
            }
        } else if ("hb-rc".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatIsRc = "on".equals(tokens[1].trim());
            } else {
                heartbeatIsRc = false;
            }
        } else if ("hb-conf".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatConf = "on".equals(tokens[1].trim());
            } else {
                heartbeatConf = false;
            }
        } else if ("hb-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatRunning = "on".equals(tokens[1].trim());
            } else {
                heartbeatRunning = false;
            }
        } else if ("pcmk-rc".equals(tokens[0])) {
            if (tokens.length == 2) {
                pcmkIsRc = "on".equals(tokens[1].trim());
            } else {
                pcmkIsRc = false;
            }
        } else if ("pcmk-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                pcmkRunning = "on".equals(tokens[1].trim());
            } else {
                pcmkRunning = false;
            }
        } else if ("drbdp-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                drbdHost.setDrbdProxyRunning("on".equals(tokens[1].trim()));
            } else {
                drbdHost.setDrbdProxyRunning(false);
            }
        } else if ("pcmk-init".equals(tokens[0])) {
            if (tokens.length == 2) {
                pcmkInit = "on".equals(tokens[1].trim());
            } else {
                pcmkInit = false;
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
                hbLibPath = tokens[1].trim();
            } else {
                hbLibPath = null;
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
                drbdHost.setDrbdVersion(tokens[1].trim());
            } else {
                drbdHost.setDrbdVersion(null);
            }
        } else if ("drbd-mod".equals(tokens[0])) {
            if (tokens.length == 2) {
                drbdHost.setDrbdModuleVersion(tokens[1].trim());
            } else {
                drbdHost.setDrbdModuleVersion(null);
            }
        }
        corosyncHeartbeatRunning = heartbeatRunning || csRunning || aisRunning;
        if (commLayerStarting
            && (csRunning || aisRunning || heartbeatRunning)) {
            commLayerStarting = false;
        }
        if (pcmkStarting && pcmkRunning) {
            pcmkStarting = false;
        }
        if (drbdProxyStarting && drbdHost.isDrbdProxyRunning()) {
            drbdProxyStarting = false;
        }
        if (commLayerStopping
            && !csRunning
            && !aisRunning
            && !heartbeatRunning) {
            commLayerStopping = false;
        }
    }

    /** Returns the graph position of id. */
    public Point2D getGraphPosition(final String id) {
        return servicePositions.get(id);
    }

    /** Resets the graph positions. */
    public void resetGraphPosition(final String id) {
        servicePositions.remove(id);
    }

    /** Saves the positions in the graphs. */
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

    /** Sets the heartbeat version. */
    public void setHeartbeatVersion(final String heartbeatVersion) {
        this.heartbeatVersion = heartbeatVersion;
    }

    /** Sets the corosync version. */
    public void setCorosyncVersion(final String corosyncVersion) {
        this.corosyncVersion = corosyncVersion;
    }

    /** Sets the pacemaker version. */
    public void setPacemakerVersion(final String pacemakerVersion) {
        this.pacemakerVersion = pacemakerVersion;
    }

    /** Sets the openais version. */
    public void setOpenaisVersion(final String openaisVersion) {
        this.openaisVersion = openaisVersion;
    }

    /** Returns the pacemaker version. */
    public String getPacemakerVersion() {
        return pacemakerVersion;
    }

    /** Returns the corosync version. */
    public String getCorosyncVersion() {
        return corosyncVersion;
    }

    /** Returns whether corosync is installed. */
    public boolean isCorosync() {
        return corosyncVersion != null;
    }

    /** Returns whether openais is a wrapper. */
    public boolean isOpenaisWrapper() {
        return "wrapper".equals(openaisVersion);
    }


    /** Returns the openais version. */
    public String getOpenaisVersion() {
        return openaisVersion;
    }

    /** Returns the heartbeat version. */
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
    void setLoadingDone() {
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
            serverStatusLatch.await();
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /** The latch is set when the server status is run for the first time. */
    public void serverStatusLatchDone() {
        serverStatusLatch.countDown();
    }

    /** Returns true if latch is set. */
    public boolean isServerStatusLatch() {
        return serverStatusLatch.getCount() == 1;
    }

    /** Returns ssh port. */
    public String getSSHPort() {
        return sshPort;
    }

    /** Returns ssh port as integer. */
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

    /** Returns sudo password. */
    public String getSudoPassword() {
        return sudoPassword;
    }

    /** Sets sudo password. */
    public void setSudoPassword(final String sudoPassword) {
        this.sudoPassword = sudoPassword;
    }

    /** Returns whether sudo is used. */
    public Boolean isUseSudo() {
        return useSudo;
    }

    /** Sets whether sudo should be used. */
    public void setUseSudo(final Boolean useSudo) {
        this.useSudo = useSudo;
    }

    /** Sets openais/pacemaker installation method index. */
    public void setPmInstallMethod(final String pmInstallMethod) {
        this.pmInstallMethod = pmInstallMethod;
    }

    /** Returns openais/pacemaker installation method. */
    public String getPmInstallMethod() {
        return pmInstallMethod;
    }

    /** Sets heartbeat/pacemaker installation method index. */
    public void setHbPmInstallMethod(final String hbPmInstallMethod) {
        this.hbPmInstallMethod = hbPmInstallMethod;
    }

    /** Returns heartbeat/pacemaker installation method. */
    public String getHbPmInstallMethod() {
        return hbPmInstallMethod;
    }

    /** Returns whether Corosync is rc script. */
    public boolean isCsRc() {
       return csIsRc;
    }

    /** Returns whether Openais is rc script. */
    public boolean isAisRc() {
       return aisIsRc;
    }

    /** Returns whether Pacemaker is rc script. */
    public boolean isPcmkRc() {
       return pcmkIsRc;
    }

    /** Returns whether Heartbeat has an init script. */
    public boolean isHeartbeatInit() {
       return heartbeatInit;
    }

    /** Returns whether Corosync has an init script. */
    public boolean isCsInit() {
       return csInit;
    }

    /** Returns whether Openais has an init script. */
    public boolean isAisInit() {
       return aisInit;
    }

    /** Returns whether Pacemaker has an init script. */
    public boolean isPcmkInit() {
       return pcmkInit;
    }


    /** Returns whether Corosync is running script. */
    public boolean isCsRunning() {
       return csRunning;
    }

    /** Returns whether Pacemakerd is running. */
    public boolean isPcmkRunning() {
       return pcmkRunning;
    }

    /** Returns whether Openais is running script. */
    public boolean isAisRunning() {
       return aisRunning;
    }

    /** Returns whether Corosync/Openais config exists. */
    public boolean isCsAisConf() {
       return csAisConf;
    }

    /** Returns whether Heartbeat is rc script. */
    public boolean isHeartbeatRc() {
       return heartbeatIsRc;
    }

    /** Returns whether Heartbeat is running script. */
    public boolean isHeartbeatRunning() {
       return heartbeatRunning;
    }

    /** Returns whether Heartbeat config exists. */
    public boolean isHeartbeatConf() {
       return heartbeatConf;
    }

    /** Returns MD5 checksum of VM Info from server. */
    public String getVMInfoMD5() {
        return vmInfoMD5;
    }

    /** Sets MD5 checksum of VM Info from server. */
    public void setVMInfoMD5(final String vmInfoMD5) {
        this.vmInfoMD5 = vmInfoMD5;
    }

    /** Sets index of this host in cluster. */
    void setIndex(final int index) {
        this.index = index;
    }

    /** Returns index of this host in cluster. */
    int getIndex() {
        return index;
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
        final String indexString =
                            Double.toString(index).replaceFirst("\\.0+$", "");
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
            //out = getSSH().execCommandAndWait(command.toString(),
            //        false,
            //        false,
            //        60000);
            /* 10 - no such file */
            if (out.getExitCode() == 0 || out.getExitCode() == 10) {
                break;
            }
            i++;
            RoboTest.sleepNoFactor(i * 2000);
        } while (i < 5);
        String nameS = ' ' + name;
        if (name == null) {
            nameS = "";
        }
        if (i > 0) {
            RoboTest.info(getName() + ' '
                           + test + ' ' + index + nameS + " tries: " + (i + 1));
        }
        RoboTest.info(getName() + ' '
                       + test + ' ' + index + nameS + ' ' + out.getOutput());
        return out.getExitCode() == 0;
    }

    /** This is part of testsuite, it checks Pacemaker. */
    public boolean checkPCMKTest(final String test, final double index) {
        return checkTest("gui-test", test, index, null, 0);
    }

    /** This is part of testsuite, it checks DRBD. */
    public boolean checkDRBDTest(final String test, final double index) {
        final StringBuilder testName = new StringBuilder(20);
        if (Tools.getApplication().getBigDRBDConf()) {
            testName.append("big-");
        }
        if (!hasVolumes()) {
            testName.append("novolumes-");
        }
        testName.append(test);
        return checkTest("gui-drbd-test", testName.toString(), index, null, 2);
    }

    /** This is part of testsuite, it checks VMs. */
    public boolean checkVMTest(final String test,
                               final double index,
                               final String name) {
        return checkTest("gui-vm-test", test, index, name, 0);
    }

    /** Returns color of this host. Null if it is default color. */
    String getColor() {
        if (savedColor == null || defaultColor == savedColor) {
            return null;
        }
        return Integer.toString(savedColor.getRGB());
    }

    /** Sets color of this host. Don't if it is default color. */
    public void setSavedColor(final String colorString) {
        try {
            savedColor = new Color(Integer.parseInt(colorString));
        } catch (final NumberFormatException e) {
            LOG.appWarning("setSavedColor: could not parse: " + colorString);
            /* ignore it */
        }
    }

    /** Returns how much is free space in a volume group. */
    public long getFreeInVolumeGroup(final String volumeGroup) {
        final Long f = volumeGroups.get(volumeGroup);
        if (f == null) {
            return 0;
        }
        return f;
    }

    /** Returns all volume groups. */
    public Set<String> getVolumeGroupNames() {
        return volumeGroups.keySet();
    }

    /** Returns if corosync or heartbeat is running, null for unknown. */
    public Boolean getCorosyncHeartbeatRunning() {
        return corosyncHeartbeatRunning;
    }

    /** Sets corosyncHeartbeatRunning. */
    public void setCorosyncHeartbeatRunning(
                                     final Boolean corosyncHeartbeatRunning) {
        this.corosyncHeartbeatRunning = corosyncHeartbeatRunning;
    }

    /** Returns true if comm layer is stopping. */
    public boolean isCommLayerStopping() {
        return commLayerStopping;
    }

    /** Sets whether the comm layer is stopping. */
    public void setCommLayerStopping(final boolean commLayerStopping) {
        this.commLayerStopping = commLayerStopping;
    }

    /** Returns true if comm layer is starting. */
    public boolean isCommLayerStarting() {
        return commLayerStarting;
    }

    /** Sets whether the comm layer is starting. */
    public void setCommLayerStarting(final boolean commLayerStarting) {
        this.commLayerStarting = commLayerStarting;
    }

    /** Returns true if pcmk is starting. */
    public boolean isPcmkStarting() {
        return pcmkStarting;
    }

    /** Sets whether the pcmk is starting. */
    public void setPcmkStarting(final boolean pcmkStarting) {
        this.pcmkStarting = pcmkStarting;
    }

    /** Returns true if drbd proxy is starting. */
    public boolean isDrbdProxyStarting() {
        return drbdProxyStarting;
    }

    /** Sets whether the drbd proxy is starting. */
    public void setDrbdProxyStarting(final boolean drbdProxyStarting) {
        this.drbdProxyStarting = drbdProxyStarting;
    }

    /** Returns whether pacemaker is started by corosync. */
    public boolean isPcmkStartedByCorosync() {
        return pcmkServiceVersion == 0;
    }

    /** Sets libvirt version. */
    public void setLibvirtVersion(final String libvirtVersion) {
        this.libvirtVersion = libvirtVersion;
    }

    /** Returns libvirt version. */
    String getLibvirtVersion() {
        return libvirtVersion;
    }

    /** Returns logical volumes from volume group. */
    public Set<String> getLogicalVolumesFromVolumeGroup(final String vg) {
        return volumeGroupsLVS.get(vg);
    }

    /** Returns all logical volumes. */
    public Set<String> getAllLogicalVolumes() {
        final Set<String> allLVS = new LinkedHashSet<String>();
        for (final String vg : volumeGroups.keySet()) {
            final Set<String> lvs = volumeGroupsLVS.get(vg);
            if (lvs != null) {
                allLVS.addAll(lvs);
            }
        }
        return allLVS;
    }

    /** Returns whether DRBD has volume feature. */
    public boolean hasVolumes() {
        try {
            return Tools.compareVersions(drbdHost.getDrbdVersion(), "8.4") >= 0;
        } catch (final Exceptions.IllegalVersionException e) {
            LOG.appWarning("hasVolumes: " + e.getMessage(), e);
        }
        return true;
    }

    /** Returns physical volumes. */
    public Iterable<BlockDevice> getPhysicalVolumes() {
        return physicalVolumes;
    }

    /** Set whether this host should be saved. */
    public void setSavable(final boolean savable) {
        this.savable = savable;
    }

    /** Return whether this host should be saved. */
    public boolean isSavable() {
        return savable;
    }

    /** Return block device object of the drbd device. */
    public BlockDevice getDrbdBlockDevice(final String device) {
        return drbdBlockDevices.get(device);
    }

    /** Return DRBD block device objects. */
    public Iterable<BlockDevice> getDrbdBlockDevices() {
        return drbdBlockDevices.values();
    }

    /** Return list of block devices that have the specified VG. */
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

    /** drbdStatusTryLock global lock. */
    public boolean drbdStatusTryLock() {
        return mDRBDStatusLock.tryLock();
    }

    /** drbdStatusLock global lock. */
    public void drbdStatusLock() {
        mDRBDStatusLock.lock();
    }

    /** drbdStatusLock global unlock. */
    public void drbdStatusUnlock() {
        mDRBDStatusLock.unlock();
    }

    /** vmStatusLock global lock. */
    public void vmStatusLock() {
        mUpdateVMSlock.lock();
    }

    /** vmStatusLock try global lock. */
    public boolean vmStatusTryLock() {
        return mUpdateVMSlock.tryLock();
    }

    /** vmStatusLock global unlock. */
    public void vmStatusUnlock() {
        mUpdateVMSlock.unlock();
    }

    /** Return whether the user is root. */
    public boolean isRoot() {
        return ROOT_USER.equals(username);
    }

    /** Return options for GUI elements. */
    public Iterable<String> getGuiOptions(final String name) {
        final List<String> opts = guiOptions.get(name);
        if (opts == null) {
            return new ArrayList<String>();
        }
        return new ArrayList<String>(guiOptions.get(name));
    }

    /** Return the DRBD proxy is up for this DRBD resource. */
    public boolean isDrbdProxyUp(final String res) {
        return drbdResProxy.contains(res);
    }

    /** Update drbd parameters. */
    public void updateDrbdParameters() {
        final ClusterBrowser cb = getBrowser().getClusterBrowser();
        final DrbdXML drbdXML = cb.getDrbdXML();
        final String output = drbdXML.updateDrbdParameters(this);
        if (output != null) {
            drbdXML.parseDrbdParameters(this, output, cb.getClusterHosts());
            cb.getDrbdParameters().put(this, output);
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

    /** Sets if drbd status failed or not. */
    public void setDrbdStatus(final boolean drbdStatus) {
        this.drbdStatus = drbdStatus;
        resetDrbdOnBlockDevices(drbdStatus);
    }

    /** Returns whether drbd status is available. */
    public boolean isDrbdStatus() {
        return drbdStatus;
    }

    public String isDrbdUtilCompatibleWithDrbdModule() {
        if (!DRBD.compatibleVersions(drbdHost.getDrbdVersion(),
                                     drbdHost.getDrbdModuleVersion())) {
            return "DRBD util and module versions are not compatible: "
                    + drbdHost.getDrbdVersion()
                    + " / "
                    + drbdHost.getDrbdModuleVersion();
        }
        return null;
    }

    /** Returns info string about DRBD installation. */
    public String getDrbdInfoAboutInstallation() {
        final StringBuilder tt = new StringBuilder(40);
        final String drbdV = drbdHost.getDrbdVersion();
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
        return Tools.compareVersions(drbdHost.getDrbdVersion(), drbdVersion) >= 0;
    }

    public boolean drbdVersionSmaller(final String drbdVersion) throws Exceptions.IllegalVersionException {
        return Tools.compareVersions(drbdHost.getDrbdVersion(), drbdVersion) < 0;
    }

    public boolean isDrbdLoaded() {
        return drbdHost.isDrbdLoaded();
    }

    public boolean isDrbdProxyRunning() {
        return drbdHost.isDrbdProxyRunning();
    }

    public boolean hasDrbd() {
        return drbdHost.getDrbdVersion() != null;
    }

    public boolean drbdVersionSmallerOrEqual(final String drbdVersion) throws Exceptions.IllegalVersionException {
        return Tools.compareVersions(drbdHost.getDrbdVersion(), drbdVersion) <= 0;
    }

    private void resetDrbdOnBlockDevices(boolean drbdStatus) {
        if (!drbdStatus) {
            for (final BlockDevice b : getBlockDevices()) {
                b.resetDrbd();
            }
        }
    }

}
