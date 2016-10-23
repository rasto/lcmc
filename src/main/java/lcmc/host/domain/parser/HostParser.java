/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2016, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.host.domain.parser;

import java.awt.geom.Point2D;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Provider;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lcmc.Exceptions;
import lcmc.HwEventBus;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.service.ssh.ExecCommandConfig;
import lcmc.cluster.service.ssh.ExecCommandThread;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.common.domain.Application;
import lcmc.common.domain.ConvertCmdCallback;
import lcmc.common.domain.ExecCallback;
import lcmc.common.domain.NewOutputCallback;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.CategoryInfo;
import lcmc.common.ui.ResourceGraph;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.drbd.domain.BlockDevice;
import lcmc.drbd.domain.DrbdHost;
import lcmc.drbd.domain.DrbdXml;
import lcmc.drbd.domain.NetInterface;
import lcmc.event.HwBlockDevicesChangedEvent;
import lcmc.event.HwBlockDevicesDiskSpaceEvent;
import lcmc.event.HwBridgesChangedEvent;
import lcmc.event.HwDrbdStatusChangedEvent;
import lcmc.event.HwFileSystemsChangedEvent;
import lcmc.event.HwMountPointsChangedEvent;
import lcmc.event.HwNetInterfacesChangedEvent;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.vm.domain.VmsXml;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class HostParser {
    private final Host              host;
    private final DrbdHost          drbdHost;
    private final HwEventBus        hwEventBus;
    private final Provider<VmsXml>  vmsXmlProvider;
    private final Provider<DrbdXml> drbdXmlProvider;
    private final SwingUtils        swingUtils;
    private final Application       application;
    private final DistributionDetector distributionDetector;

    private static final Logger LOG = LoggerFactory.getLogger(Host.class);

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

    public static final Pattern BLOCK_DEV_FILE_PATTERN = Pattern.compile("(\\D+)\\d+");
    public static final Pattern DRBD_DEV_FILE_PATTERN = Pattern.compile(".*\\/drbd\\d+$");
    public static final Pattern USED_DISK_SPACE_PATTERN = Pattern.compile("^(.*) (\\d+)$");

    private static final String LOG_COMMANDS_ON_SERVER_OPTION = "--cmd-log";

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

    private Set<String> availableCryptoModules = Sets.newTreeSet();
    private Set<Value> availableQemuKeymaps = new TreeSet<Value>();
    private Set<Value> availableCpuMapModels = new TreeSet<Value>();
    private Set<Value> availableCpuMapVendors = new TreeSet<Value>();
    private Map<String, BlockDevice> drbdBlockDevices = Maps.newLinkedHashMap();
    /** Options for GUI drop down lists. */
    private Map<String, List<String>> guiOptions = Maps.newHashMap();
    private Set<String> drbdResourcesWithProxy = Sets.newHashSet();
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
    private Boolean corosyncOrHeartbeatRunning = null;
    private String libvirtVersion = null;
    private List<BlockDevice> physicalVolumes = new ArrayList<BlockDevice>();
    private Map<String, Long> volumeGroups = new LinkedHashMap<String, Long>();
    private Map<String, Set<String>> volumeGroupsWithLvs = Maps.newHashMap();
    private String heartbeatLibPath = null;

    private final Lock mInfoTimestampLock = new ReentrantLock();
    private final Lock mUpdateVMSlock = new ReentrantLock();
    private final Lock mDRBDStatusLock = new ReentrantLock();
    private ExecCommandThread serverStatusThread = null;
    private final CountDownLatch waitForServerStatusLatch = new CountDownLatch(1);
    /** Time stamp hash. */
    private final Map<String, Double> infoTimestamp = Maps.newHashMap();
    private boolean drbdStatusOk = false;

    private static final String TOKEN_DISK_ID = "disk-id";
    private static final String TOKEN_UUID    = "uuid";
    private static final String TOKEN_SIZE    = "size";
    private static final String TOKEN_MP      = "mp";
    private static final String TOKEN_FS      = "fs";
    private static final String TOKEN_VG      = "vg";
    private static final String TOKEN_LV      = "lv";
    private static final String TOKEN_PV      = "pv";

    private static final int HW_INFO_TIMEOUT = 40000;
    /** List of positions of the services.
     *  Question is this: the saved positions can be different on different
     *  hosts, but only one can be used in the crm graph.
     *  Only one will be used and by next save the problem solves itself.
     */
    private final Map<String, Point2D> servicePositions = Maps.newHashMap();

    public void parseHostInfo(final String ans) {
        LOG.debug1("parseHostInfo: updating host info: " + host.getName());
        final String[] lines = ans.split("\\r?\\n");
        final List<String> versionLines = Lists.newArrayList();
        final Map<String, BlockDevice> newBlockDevices = Maps.newLinkedHashMap();
        final Map<String, BlockDevice> newDrbdBlockDevices = Maps.newLinkedHashMap();
        final List<NetInterface> newNetInterfaces = Lists.newArrayList();
        final List<Value> newBridges = Lists.newArrayList();
        final Map<String, Long> newVolumeGroups = Maps.newLinkedHashMap();
        final Map<String, Set<String>> newVolumeGroupsLVS = Maps.newHashMap();
        final List<BlockDevice> newPhysicalVolumes = Lists.newArrayList();
        final Set<String> fileSystems = Sets.newTreeSet();
        final Set<String> newCryptoModules = Sets.newTreeSet();
        final Set<Value> newQemuKeymaps = new TreeSet<Value>();
        final Set<Value> newCpuMapModels = new TreeSet<Value>();
        final Set<Value> newCpuMapVendors = new TreeSet<Value>();
        final Set<String> mountPoints = Sets.newTreeSet();

        final Map<String, List<String>> newGuiOptions = Maps.newHashMap();
        final Set<String> newDrbdResProxy = Sets.newHashSet();

        final Collection<String> changedTypes = Sets.newHashSet();

        final Map<String, String> diskSpaces = Maps.newHashMap();

        mountPoints.add("/mnt/");
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
            } else if (DISK_INFO_DELIM.equals(type)) {
                final Optional<BlockDevice> blockDevice = createBlockDevice(line);
                if (!blockDevice.isPresent()) {
                    continue;
                }
                final String bdName = blockDevice.get().getName();
                if (bdName != null) {
                    final Matcher drbdM = DRBD_DEV_FILE_PATTERN.matcher(bdName);
                    if (drbdM.matches()) {
                        if (drbdBlockDevices.containsKey(bdName)) {
                            drbdBlockDevices.get(bdName).updateFrom(blockDevice.get());
                        } else {
                            newDrbdBlockDevices.put(bdName, blockDevice.get());
                        }
                    } else {
                        newBlockDevices.put(bdName, blockDevice.get());
                        if (blockDevice.get().getVolumeGroup() == null
                                && bdName.length() > 5 && bdName.indexOf('/', 5) < 0) {
                            final Matcher m = BLOCK_DEV_FILE_PATTERN.matcher(bdName);
                            if (m.matches()) {
                                newBlockDevices.remove(m.group(1));
                            }
                        }
                    }
                }
                final String vg = blockDevice.get().getVolumeGroup();
                if (vg != null) {
                    Set<String> logicalVolumes = newVolumeGroupsLVS.get(vg);
                    if (logicalVolumes == null) {
                        logicalVolumes = new HashSet<String>();
                        newVolumeGroupsLVS.put(vg, logicalVolumes);
                    }
                    final String lv = blockDevice.get().getLogicalVolume();
                    if (lv != null) {
                        logicalVolumes.add(lv);
                    }
                }
                if (blockDevice.get().isPhysicalVolume()) {
                    newPhysicalVolumes.add(blockDevice.get());
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
                fileSystems.add(line);
            } else if ("crypto-info".equals(type)) {
                newCryptoModules.add(line);
            } else if ("qemu-keymaps-info".equals(type)) {
                newQemuKeymaps.add(new StringValue(line));
            } else if ("cpu-map-model-info".equals(type)) {
                newCpuMapModels.add(new StringValue(line));
            } else if ("cpu-map-vendor-info".equals(type)) {
                newCpuMapVendors.add(new StringValue(line));
            } else if ("mount-points-info".equals(type)) {
                mountPoints.add(line);
            } else if ("gui-info".equals(type)) {
                parseGuiInfo(line);
            } else if ("installation-info".equals(type)) {
                parseInstallationInfo(line);
            } else if ("gui-options-info".equals(type)) {
                guiOptionName = parseGuiOptionsInfo(line, guiOptionName, newGuiOptions);
            } else if (VERSION_INFO_DELIM.equals(type)) {
                versionLines.add(line);
            } else if ("drbd-proxy-info".equals(type)) {
                /* res-other.host-this.host */
                final Cluster cluster = host.getCluster();
                if (cluster != null) {
                    String res = null;
                    if (line.startsWith("up:")) {
                        for (final Host otherHost : cluster.getProxyHosts()) {
                            if (otherHost == host) {
                                continue;
                            }
                            final String hostsPart = '-' + otherHost.getName() + '-' + host.getName();
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
                + host.getName()
                + ", pacemaker: " + pacemakerVersion
                + ", corosync: " + corosyncVersion
                + ", heartbeat: " + heartbeatVersion
                + ", drbd: " + drbdHost.getDrbdUtilVersion()
                + ", drbd module: " + drbdHost.getDrbdModuleVersion());

        if (changedTypes.contains(NET_INFO_DELIM)) {
            hwEventBus.post(new HwNetInterfacesChangedEvent(host, newNetInterfaces));
        }

        if (changedTypes.contains(BRIDGE_INFO_DELIM)) {
            hwEventBus.post(new HwBridgesChangedEvent(host, newBridges));
        }

        if (changedTypes.contains(DISK_INFO_DELIM)) {
            drbdBlockDevices = newDrbdBlockDevices;
            physicalVolumes = newPhysicalVolumes;
            volumeGroupsWithLvs = newVolumeGroupsLVS;
        }

        if (changedTypes.contains(DISK_SPACE_DELIM)) {
            hwEventBus.post(new HwBlockDevicesDiskSpaceEvent(host, diskSpaces));
        }

        if (changedTypes.contains(VG_INFO_DELIM)) {
            volumeGroups = newVolumeGroups;
        }

        if (changedTypes.contains(FILESYSTEMS_INFO_DELIM)) {
            hwEventBus.post(new HwFileSystemsChangedEvent(host, fileSystems));
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
            hwEventBus.post(new HwMountPointsChangedEvent(host, mountPoints));
        }

        if (changedTypes.contains(VERSION_INFO_DELIM)) {
            distributionDetector.detect(ImmutableList.copyOf(versionLines));
        }

        if (changedTypes.contains(GUI_OPTIONS_INFO_DELIM)) {
            guiOptions = newGuiOptions;
        }

        if (changedTypes.contains(DRBD_PROXY_INFO_DELIM)) {
            drbdResourcesWithProxy = newDrbdResProxy;
        }

        if (changedTypes.contains(DISK_INFO_DELIM) || changedTypes.contains(VG_INFO_DELIM)) {
            hwEventBus.post(new HwBlockDevicesChangedEvent(host, newBlockDevices.values()));
        }
    }

    public String getArch() {
		return distributionDetector.getArch();
    }

	public String getDetectedInfo() {
		return distributionDetector.getDetectedInfo();
	}

    public String getDistFromDistVersion(final String dV) {
        return distributionDetector.getDistFromDistVersion(dV);
    }

    public String getKernelName() {
        return distributionDetector.getKernelName();
    }

    public String getKernelVersion() {
        return distributionDetector.getKernelVersion();

    }

    public String getDetectedKernelVersion() {
        return distributionDetector.getDetectedKernelVersion();
    }

    public String getDistCommand(final String text, final ConvertCmdCallback convertCmdCallback, final boolean inBash, final boolean inSudo) {
        return distributionDetector.getDistCommand(text, convertCmdCallback, inBash, inSudo);
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
            servicePositions.put(id, new Point2D.Double(Double.parseDouble(x), Double.parseDouble(y)));
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
                host.setHostname(tokens[1].trim());
            } else {
                host.setHostname(null);
            }
            host.setName(host.getHostname());
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

    public String getHeartbeatLibPath() {
        if (heartbeatLibPath != null) {
            return heartbeatLibPath;
        }
        val arch = distributionDetector.getArch();
        if ("".equals(arch)) {
            LOG.appWarning("getHeartbeatLibPath: called to soon: unknown arch");
        } else if ("x86_64".equals(arch) || "amd64".equals(arch)) {
            return "/usr/lib64/heartbeat";
        }
        return "/usr/lib/heartbeat";
    }

    /** Gets distribution, e.g., debian. */
    public String getDistributionName() {
        return distributionDetector.getDistributionName();
    }

    public String getDistributionVersion() {
        return distributionDetector.getDistributionVersion();
    }

    public String getDistributionVersionString() {
        return distributionDetector.getDistributionVersionString();
    }

    /**
     * Converts command string to real command for a distribution, specifying
     * the convert command callback.
     */
    public String getDistCommand(final String commandString, final ConvertCmdCallback convertCmdCallback) {
        return Tools.getDistCommand(commandString,
                                    this,
                                    convertCmdCallback,
                                    false,  /* in bash */
                                    false); /* sudo */
    }

    /** Converts a string that is specific to the distribution distribution. */
    public String getDistString(final String commandString) {
        return Tools.getDistString(commandString,
                                   distributionDetector.getDistributionName(),
                                   distributionDetector.getDistributionVersionString(),
                                   distributionDetector.getArch());
    }

    /**
     *  Gets list of strings that are specific to the distribution
     *  distribution.
     */
    public List<String> getDistStrings(final String commandString) {
        return Tools.getDistStrings(commandString,
                                    distributionDetector.getDistributionName(),
                                    distributionDetector.getDistributionVersionString(),
                                    distributionDetector.getArch());
    }


    /**
     * Converts command string to real command for a distribution, specifying
     * what-with-what hash.
     */
    public String getDistCommand(final String commandString, final Map<String, String> replaceHash) {
        return Tools.getDistCommand(
                commandString,
				this,
                command -> {
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
				},
                false,  /* in bash */
                false); /* sudo */
    }

    /** Gets and stores info about the host. */
    public void getAllInfo() {
        host.execCommand(new ExecCommandConfig().commandString("GetHostAllInfo")
                .execCallback(new ExecCallback() {
                    @Override
                    public void done(final String ans) {
                        parseHostInfo(ans);
                        host.setLoadingDone();
                    }

                    @Override
                    public void doneError(final String ans, final int exitCode) {
                        host.setLoadingError();
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
        host.execCommand(new ExecCommandConfig().commandString(cmd)
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
                        host.setLoadingDone();
                    }

                    @Override
                    public void doneError(final String ans, final int exitCode) {
                        host.setLoadingError();
                        host.getSSH().forceReconnect();
                    }
                })
                .sshCommandTimeout(HW_INFO_TIMEOUT)
                .silentCommand()
                .silentOutput()).block();
    }

    /** Gets and stores hardware info about the host. */
    public void startHWInfoDaemon(final CategoryInfo[] infosToUpdate, final ResourceGraph[] graphs) {
        LOG.debug1("startHWInfoDaemon: " + host.getName());
        serverStatusThread = host.getSSH().execCommand(new ExecCommandConfig()
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
                        if (getWaitForServerStatusLatch()) {
                            final ClusterBrowser cb = host.getBrowser().getClusterBrowser();
                            cb.updateServerStatus(host);
                        }
                        host.setLoadingDone();
                    }

                    @Override
                    public void doneError(final String ans, final int exitCode) {
                        if (getWaitForServerStatusLatch()) {
                            final ClusterBrowser cb = host.getBrowser().getClusterBrowser();
                            cb.updateServerStatus(host);
                        }
                        host.setLoadingError();
                    }
                })
                .newOutputCallback(new NewOutputCallback() {
                    private final StringBuffer outputBuffer = new StringBuffer(300);
                    @Override
                    public void output(final String output) {
                        outputBuffer.append(output);
                        final ClusterBrowser cb = host.getBrowser().getClusterBrowser();
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
                            final VmsXml newVmsXml = vmsXmlProvider.get();
                            newVmsXml.init(host);
                            if (newVmsXml.parseXml(vmUpdate)) {
                                cb.vmsXmlPut(host, newVmsXml);
                                cb.updateVms();
                            }
                        }
                        if (drbdUpdate != null) {
                            final DrbdXml dxml = drbdXmlProvider.get();
                            dxml.init(host.getCluster().getHostsArray(), cb.getHostDrbdParameters());
                            dxml.update(drbdUpdate);
                            cb.setDrbdXml(dxml);
                            swingUtils.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    host.getBrowser().getClusterBrowser().getGlobalInfo().setParameters();
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
                        host.setLoadingDone();
                    }
                })
                .silentCommand()
                .silentOutput()
                .sshCommandTimeout(HW_INFO_TIMEOUT)).block();
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

    public void vmStatusLock() {
        mUpdateVMSlock.lock();
    }

    public boolean vmStatusTryLock() {
        return mUpdateVMSlock.tryLock();
    }

    public void vmStatusUnlock() {
        mUpdateVMSlock.unlock();
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

    /** The latch is set when the server status is run for the first time. */
    public void serverStatusLatchDone() {
        waitForServerStatusLatch.countDown();
    }

    /** Returns true if latch is set. */
    public boolean getWaitForServerStatusLatch() {
        return waitForServerStatusLatch.getCount() == 1;
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

    /** Waits for the server status latch. */
    public void waitForServerStatusLatch() {
        try {
            waitForServerStatusLatch.await();
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
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
            if (vg.equals(b.getVgOnPhysicalVolume())) {
                bds.add(b);
            }
        }
        return bds;
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

    public String getLxcLibPath() {
        return getDistString("libvirt.lxc.libpath");
    }

    /** Returns xen lib path. */
    public String getXenLibPath() {
        return getDistString("libvirt.xen.libpath");
    }

    public Point2D getGraphPosition(final String id) {
        return servicePositions.get(id);
    }

    public void resetGraphPosition(final String id) {
        servicePositions.remove(id);
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
        if (distributionDetector.getKernelVersion() != null
                && command.contains("@KERNELVERSIONDIR@")) {
            command = command.replaceAll("@KERNELVERSIONDIR@", distributionDetector.getKernelVersion());
        }
        if (distributionDetector.getDistributionVersion() != null
                && command.contains("@DISTRIBUTION@")) {
            command = command.replaceAll("@DISTRIBUTION@", distributionDetector.getDistributionVersion());
        }
        if (distributionDetector.getArch() != null
                && command.contains("@ARCH@")) {
            command = command.replaceAll("@ARCH@", distributionDetector.getArch());
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

    public void setDrbdStatusOk(final boolean drbdStatusOk) {
        this.drbdStatusOk = drbdStatusOk;
        hwEventBus.post(new HwDrbdStatusChangedEvent(host, drbdStatusOk));
    }

    public boolean isDrbdStatusOk() {
        return drbdStatusOk;
    }

    private Optional<BlockDevice> createBlockDevice(final String line) {
        final Pattern p = Pattern.compile("([^:]+):(.*)");
        final String[] cols = line.split(" ");
        if (cols.length < 2) {
            LOG.appWarning("update: cannot parse block device line: " + line);
            return Optional.absent();
        } else {
            final Collection<String> diskIds = new HashSet<String>();
            final String device = cols[0];
            final Map<String, String> tokens = Maps.newHashMap();
            for (int i = 1; i < cols.length; i++) {
                final Matcher m = p.matcher(cols[i]);
                if (m.matches()) {
                    if (TOKEN_DISK_ID.equals(m.group(1))) {
                        diskIds.add(m.group(2));
                    } else {
                        tokens.put(m.group(1), m.group(2));
                    }
                } else {
                    LOG.appWarning("update: could not parse: " + line);
                }
            }
            final BlockDevice blockDevice = new BlockDevice(host, device);
            blockDevice.setDiskUuid(tokens.get(TOKEN_UUID));
            blockDevice.setBlockSize(tokens.get(TOKEN_SIZE));
            blockDevice.setMountedOn(tokens.get(TOKEN_MP));
            blockDevice.setFsType(tokens.get(TOKEN_FS));
            blockDevice.setVolumeGroup(tokens.get(TOKEN_VG));
            blockDevice.setLogicalVolume(tokens.get(TOKEN_LV));
            blockDevice.setVgOnPhysicalVolume(tokens.get(TOKEN_PV));
            blockDevice.setDiskIds(diskIds);
            return Optional.of(blockDevice);
        }
    }

}
