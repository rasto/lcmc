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

package drbd.data;

import drbd.utilities.Tools;
import drbd.utilities.SSH;
import drbd.utilities.SSH.ExecCommandThread;
import drbd.utilities.ExecCallback;
import drbd.utilities.ConvertCmdCallback;
import drbd.utilities.ConnectionCallback;
import drbd.utilities.NewOutputCallback;
import drbd.gui.ProgressBar;
import drbd.gui.TerminalPanel;
import drbd.gui.SSHGui;
import drbd.gui.HostBrowser;
import drbd.gui.resources.CategoryInfo;
import drbd.data.resources.NetInterface;
import drbd.data.resources.BlockDevice;
import java.awt.geom.Point2D;

import java.awt.Color;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * This class holds host data and implementation of host related methods.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class Host implements Serializable {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Name of the host. */
    private String name;
    /** Hostname as entered by the user. Could be ip, hostname with or without
     * the domain name. */
    private String hostnameEntered = Tools.getDefault("SSH.Host");
    /** Ip of the host. */
    private String ip;
    /** Ips in the combo in Dialog.Host.Configuration. */
    private final Map<Integer, String[]> ips = new HashMap<Integer, String[]>();
    /** Hostname of the host. */
    private String hostname = "unknown";
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
    /** Available drbd versions. */
    private List<String> availableDrbdVersions = null;
    /** Drbd version, that will be installed. */
    private String drbdVersionToInstall = null;
    /** Drbd version of the source tarball, that will be installed . */
    private String drbdVersionUrlStringToInstall = null;
    /** Build, which will be installed. */
    private String drbdBuildToInstall = null;
    /** Drbd packages, that will be installed. */
    private String drbdPackagesToInstall = null;
    /** Cluster data object. */
    private Cluster cluster = null;
    /** Drbd version of drbdadm tool. */
    private String drbdVersion = null;
    /** Drbd version of drbd module. */
    private String drbdModuleVersion = null;
    /** Installed drbd version. TODO */
    private final String installedDrbdVersion = null;
    /** Map of network interfaces of this host. */
    private Map<String, NetInterface> netInterfaces =
                                     new LinkedHashMap<String, NetInterface>();
    /** Available file systems. */
    private final Set<String> fileSystems = new TreeSet<String>();
    /** Available crypto modules. */
    private final Set<String> cryptoModules = new TreeSet<String>();
    /** Mount points that exist in /mnt dir. */
    private final Set<String> mountPoints = new TreeSet<String>();
    /** List of block devices of this host. */
    private Map<String, BlockDevice> blockDevices =
                                      new LinkedHashMap<String, BlockDevice>();
    /** Color of this host in graphs. */
    private Color defaultColor;
    /** Color of this host in graphs. */
    private Color savedColor;
    /** Thread where drbd status command is running. */
    private ExecCommandThread drbdStatusThread = null;
    /** Thread where hb status command is running. */
    private ExecCommandThread clStatusThread = null;
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
    /** Is "on" if corosync/openais is in rc. */
    private String csAisIsRc = null;
    /** Is "on" if corosync/openais is running. */
    private String csAisRunning = null;
    /** Is "on" if corosync/openais config exists. */
    private String csAisConf = null;
    /** Is "on" if heartbeat is in rc. */
    private String heartbeatIsRc = null;
    /** Is "on" if heartbeat is running. */
    private String heartbeatRunning = null;
    /** Is "on" if heartbeat config exists. */
    private String heartbeatConf = null;
    /** Is "on" if drbd module is loaded. */
    private String drbdLoaded = null;
    /** Corosync version. */
    private String corosyncVersion = null;
    /** Heartbeat version. */
    private String heartbeatVersion = null;
    /** Whether drbd will be upgraded. */
    private boolean drbdWillBeUpgraded = false;
    /** Whether drbd was newly installed. */
    private boolean drbdWasInstalled = false;
    /** Whether heartbeat status is ok. */
    private boolean clStatus = false;
    /** Whether drbd status is ok. */
    private boolean drbdStatus = false;

    /** SSH object of the connection to this host. */
    private final SSH ssh = new SSH();
    /** Terminal panel of this host. */
    private TerminalPanel terminalPanel = null;
    /** SSH port. */
    private String sshPort = null;
    /** Whether sudo should be used. */
    private Boolean useSudo = null;
    /** Sudo password. */
    private String sudoPassword = "";
    /** Browser panel (the one with menus and all the logic) of this host. */
    private HostBrowser browser;
    /** Timeout after which the drbd status is considered hanging and will be
     * killed. Drbd should return about after 20 seconds. Timing here is
     * important. This timeout must be greater, that timeout of the drbd status
     * in the helper script.
     */
    private static final int DRBD_STATUS_TIMEOUT = 30000;
    /** A gate that is used to synchronize the loading sequence. */
    private CountDownLatch isLoadingGate;
    /** List of gui elements that are to be enabled if the host is connected.*/
    private final List<JComponent> enableOnConnectList =
                                                   new ArrayList<JComponent>();
    /** Corosync/Openais/pacemaker installation method index. */
    private String pmInstallMethod;
    /** Heartbeat/pacemaker installation method index. */
    private String hbPmInstallMethod;
    /** Drbd installation method index. */
    private String drbdInstallMethod;
    /** MD5 checksum of VM Info from server. */
    private String vmInfoMD5 = null;
    /** Previous hw info output. */
    private String oldHwInfo = null;
    /** Index of this host in its cluster. */
    private int index = 0;
    /** String that is displayed as a tool tip for disabled menu item. */
    public static final String NOT_CONNECTED_STRING =
                                                   "not connected to the host";
    /**
     * Prepares a new <code>Host</code> object. Initializes host browser and
     * host's resources.
     */
    public Host() {
        if (Tools.getConfigData().getHosts().size() == 1) {
            hostnameEntered = Tools.getDefault("SSH.SecHost");
        }
        browser = new HostBrowser(this);
        browser.initHostResources();
        addMountPoint("/mnt/");
    }

    /**
     * Prepares a new <code>Host</code> object.
     */
    public Host(final String ip) {
        this();
        this.ip = ip;
    }

    /**
     * Returns borwser object for this host.
     */
    public final HostBrowser getBrowser() {
        return browser;
    }

    /**
     * Sets cluster in which this host is in. Set null,
     * if it is removed from the cluster. One host can be
     * only in one cluster.
     */
    public final void setCluster(final Cluster cluster) {
        this.cluster = cluster;
    }

    /**
     * Returns the cluster data object.
     */
    public final Cluster getCluster() {
        return cluster;
    }

    /**
     * Returns color objects of this host for drbd graph.
     */
    public final Color[] getDrbdColors() {
        if (defaultColor == null) {
            defaultColor = Tools.getDefaultColor("Host.DefaultColor");
        }
        Color col;
        Color secColor;
        if (savedColor == null) {
            col = defaultColor;
        } else {
            col = savedColor;
        }
        if (!isConnected()) {
            secColor = Tools.getDefaultColor("Host.ErrorColor");
        } else {
            if (isDrbdStatus() && isDrbdLoaded()) {
                return new Color[]{col};
            } else {
                secColor = Tools.getDefaultColor("Host.NoStatusColor");
            }
        }
        return new Color[]{col, secColor};
    }


    /**
     * Returns color objects of this host.
     */
    public final Color[] getPmColors() {
        if (defaultColor == null) {
            defaultColor = Tools.getDefaultColor("Host.DefaultColor");
        }
        Color col;
        Color secColor;
        if (savedColor == null) {
            col = defaultColor;
        } else {
            col = savedColor;
        }
        if (!isConnected()) {
            secColor = Tools.getDefaultColor("Host.ErrorColor");
        } else {
            if (isClStatus()) {
                return new Color[]{col};
            } else {
                secColor = Tools.getDefaultColor("Host.NoStatusColor");
            }
        }
        return new Color[]{col, secColor};
    }

    /** Sets color of the host. */
    public final void setColor(final Color defaultColor) {
        this.defaultColor = defaultColor;
        if (savedColor == null) {
            savedColor = defaultColor;
        }
        if (terminalPanel != null) {
            terminalPanel.resetPromptColor();
        }
    }

    /** Sets color of the host, when it was saved. */
    public final void setSavedColor(final Color savedColor) {
        this.savedColor = savedColor;
        if (terminalPanel != null) {
            terminalPanel.resetPromptColor();
        }
    }

    /**
     * Sets if hb status failed or not.
     */
    public final void setClStatus(final boolean clStatus) {
        this.clStatus = clStatus;
    }

    /**
     * Sets if drbd status failed or not.
     */
    public final void setDrbdStatus(final boolean drbdStatus) {
        this.drbdStatus = drbdStatus;
        if (!drbdStatus) {
            for (BlockDevice b : getBlockDevices()) {
                b.resetDrbd();
            }
        }
    }

    /**
     * Returns whether cluster status is available.
     */
    public final boolean isClStatus() {
        return clStatus && isConnected();
    }

    /**
     * Returns whether drbd status is available.
     */
    public final boolean isDrbdStatus() {
        return drbdStatus;
    }

    /**
     * Returns true when this host is in a cluster.
     */
    public final boolean isInCluster() {
        return cluster != null;
    }

    /**
     * Returns true when this host is in a cluster and is different than the
     * specified cluster.
     */
    public final boolean isInCluster(final Cluster otherCluster) {
        return cluster != null && !cluster.equals(otherCluster);
    }

    /**
     * Sets hostname as entered by user, this can be also ip. If
     * hostnameEntered changed, it reinitilizes the name.
     */
    public final void setHostnameEntered(final String hostnameEntered) {
        Tools.debug(this, "h e: " + hostnameEntered + " != "
                          + this.hostnameEntered, 1);
        if (hostnameEntered != null
            && !hostnameEntered.equals(this.hostnameEntered)) {
            /* back button and hostname changed */
            setName(null);
            setIp(null);
            setHostname(null);
        }
        this.hostnameEntered = hostnameEntered;
    }

    /**
     * Sets hostname of the host.
     */
    public final void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    /**
     * Sets user name for the host. This username is used to connect
     * to the host. The default is "root". If username changed disconnect
     * the old connection.
     */
    public final void setUsername(final String username) {
        if (!username.equals(this.username)) {
            ssh.disconnect();
        }
        this.username = username;
    }

    /**
     * Sets ip. If ip has changed, disconnect the
     * old connection.
     */
    public final void setIp(final String ip) {
        if (ip != null) {
            if (!ip.equals(this.ip)) {
                ssh.disconnect();
            }
        } else if (this.ip != null) {
            ssh.disconnect();
        }
        this.ip = ip;
    }

    /**
     * Sets ips.
     */
    public final void setIps(final int hop, final String[] ipsForHop) {
        ips.put(hop, ipsForHop);
    }

    /** Returns net interfaces. */
    public final NetInterface[] getNetInterfaces() {
        return netInterfaces.values().toArray(
                                    new NetInterface[netInterfaces.size()]);
    }

    /** Get net interfaces that are bridges. */
    public final List<String> getBridges() {
        final List<String> bridges = new ArrayList<String>();
        for (final NetInterface ni : netInterfaces.values()) {
            if (ni.isBridge()) {
                bridges.add(ni.getName());
            }
        }
        return bridges;
    }

    /** Returns blockDevices. */
    public final BlockDevice[] getBlockDevices() {
        return blockDevices.values().toArray(
                                    new BlockDevice[blockDevices.size()]);
    }

    /**
     * Returns blockDevices as array list of device names. Removes the
     * ones that are in the drbd and are already used in CRM.
     */
    public final List<String> getBlockDevicesNames() {
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
    public final List<String> getBlockDevicesNamesIntersection(
                                        final List<String> otherBlockDevices) {
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

    /**
     * Returns network ips as array list.
     */
    public final Map<String, String> getNetworkIps() {
        final Map<String, String> networkIps =
                                         new LinkedHashMap<String, String>();
        for (final NetInterface ni : netInterfaces.values()) {
            final String netIp = ni.getNetworkIp();
            networkIps.put(netIp, ni.getNetmask());
        }
        return networkIps;
    }

    /** Returns list of networks that exist on all hosts. */
    public final Map<String, String> getNetworksIntersection(
                                   final Map<String, String> otherNetworkIps) {
        if (otherNetworkIps == null) {
            return getNetworkIps();
        }
        final Map<String, String> networksIntersection =
                                           new LinkedHashMap<String, String>();
        for (final NetInterface ni : netInterfaces.values()) {
            final String networkIp = ni.getNetworkIp();
            if (otherNetworkIps.containsKey(networkIp)
                && !networksIntersection.containsKey(networkIp)) {
                networksIntersection.put(networkIp, ni.getNetmask());
            }
        }
        return networksIntersection;
    }

    /** Returns ips that belong the the network. */
    public final List<String> getIpsFromNetwork(final String netIp) {
        final List<String> networkIps = new ArrayList<String>();
        for (final NetInterface ni : netInterfaces.values()) {
            if (netIp.equals(ni.getNetworkIp())) {
                networkIps.add(ni.getIp());
            }
        }
        return networkIps;
    }

    /**
     * Returns BlockDevice object identified with device name.
     */
    public final BlockDevice getBlockDevice(final String device) {
        return blockDevices.get(device);
    }

    /**
     * Removes file system from the list of file systems.
     */
    public final void removeFileSystems() {
        fileSystems.clear();
    }

    /**
     * Returns available file systems.
     */
    public final String[] getFileSystems() {
        return fileSystems.toArray(new String [fileSystems.size()]);
    }

    /**
     * Returns available file systems devices as a list of strings.
     */
    public final Set<String> getFileSystemsList() {
        return fileSystems;
    }

    /**
     * Adds file system to the list of file systems.
     */
    public final void addFileSystem(final String fileSystem) {
        fileSystems.add(fileSystem);
    }

    /**
     * Returns available crypto modules as a list of strings.
     */
    public final Set<String> getCryptoModules() {
        return cryptoModules;
    }


    /**
     * Adds crypto module to the list of crypto modules.
     */
    public final void addCryptoModule(final String cryptoModule) {
        cryptoModules.add(cryptoModule);
    }

    /**
     * Returns mount points as a list of strings.
     */
    public final Set<String> getMountPointsList() {
        return mountPoints;
    }

    /**
     * Adds mount point to the list of mount points.
     */
    private void addMountPoint(final String mountPoint) {
        mountPoints.add(mountPoint);
    }

    /**
     * Returns ips of this host.
     */
    public final String[] getIps(final int hop) {
        return ips.get(hop);
    }

    /**
     * Sets available drbd versions.
     */
    public final void setAvailableDrbdVersions(final String[] versions) {
        availableDrbdVersions = new ArrayList<String>(Arrays.asList(versions));
    }

    /**
     * Retruns available drbd versions as array of strings.
     */
    public final String[] getAvailableDrbdVersions() {
        if (availableDrbdVersions == null) {
            return null;
        }
        return availableDrbdVersions.toArray(
                                    new String [availableDrbdVersions.size()]);
    }

    /**
     * Returns whether version v1 is greater than version 2.
     */
    private boolean versionGreater(final String v1, final String v2) {
        final Pattern p = Pattern.compile("^(\\d+)rc(\\d+)$");
        String[] v1a = (v1 + ".999999").split("\\.");
        String[] v2a = (v2 + ".999999").split("\\.");
        if (v1a.length != 4 || v2a.length != 4) {
            Tools.appWarning("wrong versions: " + v1 + ", " + v2);
            return false;
        }

        /* release candidate */
        final Matcher m = p.matcher(v1a[2]);
        if (m.matches()) {
            v1a[2] = m.group(1);
            v1a[3] = m.group(2);
        }

        final Matcher m2 = p.matcher(v2a[2]);
        if (m2.matches()) {
            v2a[2] = m2.group(1);
            v2a[3] = m2.group(2);
        }

        int i = 0;
        for (String v1p : v1a) {
            final String v2p = v2a[i];
            if (Integer.valueOf(v1p) > Integer.valueOf(v2p)) {
                return true;
            } else if (Integer.valueOf(v1p) < Integer.valueOf(v2p)) {
                return false;
            }
            i++;
        }
        return false;
    }

    /**
     * Returns whether there is a drbd upgrade available.
     */
    public final boolean isDrbdUpgradeAvailable(final String versionString) {
        if (availableDrbdVersions == null) {
            return false;
        }
        final String version = versionString.split(" ")[0];
        for (final String v : availableDrbdVersions) {
            if (versionGreater(v, version)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns installed drbd version.
     */
    public final String getDrbdVersion() {
        return drbdVersion;
    }

    /**
     * Returns installed drbd module version.
     */
    public final String getDrbdModuleVersion() {
        return drbdModuleVersion;
    }

    /**
     * Sets drbdVersionToInstall. This version is one that is to be installed.
     * If drbd is already installed, installedDrbdVersion contains
     * its version.
     */
    public final void setDrbdVersionToInstall(
                                        final String drbdVersionToInstall) {
        this.drbdVersionToInstall = drbdVersionToInstall;
    }

    /**
     * Sets the drbd version in the form that is in the source tarball on
     * linbit website, like so: "8.3/drbd-8.3.1.tar.gz".
     */
    public final void setDrbdVersionUrlStringToInstall(
                                  final String drbdVersionUrlStringToInstall) {
        this.drbdVersionUrlStringToInstall = drbdVersionUrlStringToInstall;
    }

    /**
     * Gets drbdVersionToInstall. This version is one that is to be installed.
     * If drbd is already installed, installedDrbdVersion contains
     * its version.
     */
    public final String getDrbdVersionToInstall() {
        return drbdVersionToInstall;
    }

    /**
     * Gets drbd version of the source tarball in the form as it is on
     * the linbit website: "8.3/drbd-8.3.1.tar.gz".
     */
    public final String getDrbdVersionUrlStringToInstall() {
        return drbdVersionUrlStringToInstall;
    }

    /**
     * Sets drbdBuildToInstall. This build is the one that is to be installed.
     */
    public final void setDrbdBuildToInstall(final String drbdBuildToInstall) {
        this.drbdBuildToInstall = drbdBuildToInstall;
    }

    /**
     * Returns the drbd build to be installed.
     */
    public final String getDrbdBuildToInstall() {
        return drbdBuildToInstall;
    }

    /**
     * Sets drbd packages to install.
     */
    public final void setDrbdPackagesToInstall(
                                        final String drbdPackagesToInstall) {
        this.drbdPackagesToInstall = drbdPackagesToInstall;
    }

    /**
     * Sets distribution info for this host from array of strings.
     * Array consists of: kernel name, kernel version, arch, os, version
     * and distribution.
     */
    @SuppressWarnings("fallthrough")
    public final void setDistInfo(final String[] info) {
        if (info == null) {
            return;
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
                if (lsbDist == null) {
                    detectedDist = info[4];
                } else {
                    detectedDist = lsbDist;
                }
            case 4:
                if (lsbVersion != null) {
                    detectedDistVersion = info[3] + "/" + lsbVersion;
                } else {
                    detectedDistVersion = info[3];
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
                Tools.appError("Error.SetDistInfo.CannotSetInfo",
                               java.util.Arrays.asList(info).toString());
                break;
        }
        dist = detectedDist;
        distVersion = detectedDistVersion;
        initDistInfo();
        Tools.debug(this, "kernel name: " + detectedKernelName, 1);
        Tools.debug(this, "kernel version: " + detectedKernelVersion, 1);
        Tools.debug(this, "arch: " + detectedArch, 1);
        Tools.debug(this, "dist version: " + detectedDistVersion, 1);
        Tools.debug(this, "dist: " + detectedDist, 1);
    }

    /**
     * Initializes dist info. Must be called after setDistInfo.
     */
    public final void initDistInfo() {
        if (!"Linux".equals(detectedKernelName)) {
            Tools.appWarning("detected kernel not linux: "
                             + detectedKernelName);
        }
        setKernelName("Linux");

        if (!dist.equals(detectedDist)) {
            Tools.appError("dist: " + dist + " does not match " + detectedDist);
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
        setArch(Tools.getDistString("arch:" + detectedArch,
                                    getDist(),
                                    distVersionString,
                                    null));
    }

    /**
     * Returns the detected info to show.
     */
    public final String getDetectedInfo() {
        return detectedDist + " " + detectedDistVersion;
    }

    /**
     * Gets distribution name from distribution version. E.g. suse from sles9.
     * This is used only when the distribution is selected in the pulldown menu,
     * not when it is detected.
     * The conversion rules for distributions are defined in DistResource.java,
     * with 'dist:' prefix.
     */
    public final String getDistFromDistVersion(final String dV) {
        /* remove numbers */
        if ("No Match".equals(dV)) {
            return null;
        }
        Tools.debug(this, "getDistVersionString dist:"
                          + dV.replaceFirst("\\d.*", ""), 0);
        return Tools.getDistString("dist:" + dV.replaceFirst("\\d.*", ""),
                                   "",
                                   "",
                                   null);
    }


    /**
     * Sets distribution name.
     */
    public final void setDist(final String dist) {
        this.dist = dist;
    }

    /**
     * Sets distribution version.
     */
    public final void setDistVersion(final String distVersion) {
        this.distVersion = distVersion;
        distVersionString = Tools.getDistVersionString(dist, distVersion);
        dist = getDistFromDistVersion(distVersion);
    }

    /**
     * Sets arch, e.g. "i386".
     */
    public final void setArch(final String arch) {
        this.arch = arch;
    }

    /**
     * Sets kernel name, e.g. "linux".
     */
    public final void setKernelName(final String kernelName) {
        this.kernelName = kernelName;
    }

    /**
     * Sets kernel version.
     */
    public final void setKernelVersion(final String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    /**
     * Gets kernel name. Normaly "Linux" for this application.
     */
    public final String getKernelName() {
        return kernelName;
    }

    /**
     * Gets kernel version. Usually some version,
     * like: "2.6.13.2ws-k7-up-lowmem".
     */
    public final String getKernelVersion() {
        return kernelVersion;
    }

    /**
     * Returns the detected kernel version.
     */
    public final String getDetectedKernelVersion() {
        return detectedKernelVersion;
    }

    /**
     * Gets architecture like i686.
     */
    public final String getArch() {
        return arch;
    }

    /**
     * Returns heartbeat lib path.
     */
    public final String getHeartbeatLibPath() {
        if ("".equals(arch)) {
            Tools.appWarning(
                        "getHeartbeatLibPath() called to soon: unknown arch");
        } else if ("x86_64".equals(arch)) {
            return "/usr/lib64/heartbeat";
        }
        return "/usr/lib/heartbeat";
    }


    /**
     * Gets distribution, e.g., debian.
     */
    public final String getDist() {
        return dist;
    }

    /**
     * Gets distribution version.
     */
    public final String getDistVersion() {
        return distVersion;
    }

    /**
     * Gets distribution version string.
     */
    public final String getDistVersionString() {
        return distVersionString;
    }

    /**
     * Disconnects this host.
     */
    public final void disconnect() {
        if (ssh.isConnected()) {
            ssh.forceDisconnect();
        }
    }

    /**
     * Converts command string to real command for a distribution, specifying
     * the convert command callback.
     */
    public final String getDistCommand(
                                 final String commandString,
                                 final ConvertCmdCallback convertCmdCallback) {
        return Tools.getDistCommand(commandString,
                                    dist,
                                    distVersionString,
                                    arch,
                                    convertCmdCallback);
    }

    /**
     * Converts a string that is specific to the distribution distribution.
     */
    public final String getDistString(
                                 final String commandString) {
        return Tools.getDistString(commandString,
                                   dist,
                                   distVersionString,
                                   arch);
    }

    /**
     * Converts command string to real command for a distribution, specifying
     * what-with-what hash.
     */
    public final String getDistCommand(final String commandString,
                                       final Map<String, String> replaceHash) {
        return Tools.getDistCommand(
                    commandString,
                    dist,
                    distVersionString,
                    arch,
                    new ConvertCmdCallback() {
                        public final String convert(String command) {
                            for (final String tag : replaceHash.keySet()) {
                                if (command.indexOf(tag) > -1) {
                                    command =
                                        command.replaceAll(
                                                        tag,
                                                        replaceHash.get(tag));
                                }
                            }
                            return command;
                        }
                    });
    }

    /**
     * Executes command. Command is executed in a new thread, after command
     * is finished execCallback.done function will be called. In case of error,
     * callback.doneError is called.
     */
    public final ExecCommandThread execCommand(
                                final String commandString,
                                final ExecCallback execCallback,
                                final ConvertCmdCallback convertCmdCallback,
                                final boolean outputVisible,
                                final int commandTimeout) {
        if (outputVisible) {
            Tools.getGUIData().setTerminalPanel(getTerminalPanel());
        }
        return ssh.execCommand(Tools.getDistCommand(commandString,
                                                    dist,
                                                    distVersionString,
                                                    arch,
                                                    convertCmdCallback),
                               execCallback,
                               outputVisible,
                               true,
                               commandTimeout);
    }

    /**
     * Executes command. Command is executed in a new thread, after command
     * is finished execCallback.done function will be called. In case of error,
     * callback.doneError is called. After every line of output
     * newOutputCallback.output function is called. Use this function for
     * commands that do not return, but run in the background and occasionaly
     * print a line to the stdout.
     */
    public final ExecCommandThread execCommand(
                                final String commandString,
                                final ExecCallback execCallback,
                                final ConvertCmdCallback convertCmdCallback,
                                final NewOutputCallback newOutputCallback,
                                final boolean outputVisible,
                                final int commandTimeout) {
        if (outputVisible) {
            Tools.getGUIData().setTerminalPanel(getTerminalPanel());
        }
        return ssh.execCommand(Tools.getDistCommand(commandString,
                                                    dist,
                                                    distVersionString,
                                                    arch,
                                                    convertCmdCallback),
                               execCallback,
                               newOutputCallback,
                               outputVisible,
                               true,
                               commandTimeout);
    }


    /**
     * Executes command. Command is not converted for different distributions
     * and is executed in a new thread, after command is finished callback.done
     * function will be called. In case of error, callback.doneError is called.
     */
    public final ExecCommandThread execCommandRaw(final String command,
                                            final ExecCallback callback,
                                            final boolean outputVisible,
                                            final boolean commandVisible,
                                            final int commandTimeout) {
        if (outputVisible) {
            Tools.getGUIData().setTerminalPanel(getTerminalPanel());
        }
        return ssh.execCommand(command,
                               callback,
                               outputVisible,
                               commandVisible,
                               commandTimeout);
    }

    /**
     * Executes command with parameters. Command is executed in a new thread,
     * after command is finished callback.done function will be called.
     * In case of error, callback.doneError is called.
     * Parameters will be passed directly as they are to the command.
     */
    public final ExecCommandThread execCommand(
                                final String commandString,
                                final String params,
                                final ExecCallback callback,
                                final ConvertCmdCallback convertCmdCallback,
                                final boolean outputVisible,
                                final int commandTimeout) {
        if (outputVisible) {
            Tools.getGUIData().setTerminalPanel(getTerminalPanel());
        }
        return ssh.execCommand(Tools.getDistCommand(commandString,
                                                    dist,
                                                    distVersionString,
                                                    arch,
                                                    convertCmdCallback)
                               + params,
                               callback,
                               outputVisible,
                               true,
                               commandTimeout);
    }

    /**
     * Executes command. Command is executed in a new thread, after command
     * is finished callback.done function will be called. In case of error,
     * callback.doneError is called.
     */
    public final ExecCommandThread execCommand(
                                final String commandString,
                                final ProgressBar progressBar,
                                final ExecCallback callback,
                                final ConvertCmdCallback convertCmdCallback,
                                final boolean outputVisible,
                                final int commandTimeout) {
        if (outputVisible) {
            Tools.getGUIData().setTerminalPanel(getTerminalPanel());
        }
        return ssh.execCommand(Tools.getDistCommand(commandString,
                                                    dist,
                                                    distVersionString,
                                                    arch,
                                                    convertCmdCallback),
                               progressBar,
                               callback,
                               outputVisible,
                               true,
                               commandTimeout);
    }

    /**
     * Executes command. Command is not converted for different distributions
     * and is executed in a new thread, after command is finished callback.done
     * function will be called. In case of error, callback.doneError is called.
     */
    public final ExecCommandThread execCommandRaw(
                                                final String command,
                                                final ProgressBar progressBar,
                                                final ExecCallback execCallback,
                                                final boolean outputVisible,
                                                final boolean commandVisible,
                                                final int commandTimeout) {
        if (outputVisible) {
            Tools.getGUIData().setTerminalPanel(getTerminalPanel());
        }
        return ssh.execCommand(command,
                               progressBar,
                               execCallback,
                               outputVisible,
                               commandVisible,
                               commandTimeout);
    }

    /**
     * Executes command or return output from cache if it was alread run.
     * Command is executed in a new thread, after command
     * is finished callback.done function will be called. In case of error,
     * callback.doneError is called.
     */
    public final ExecCommandThread execCommandCache(
                                final String commandString,
                                final ProgressBar progressBar,
                                final ExecCallback callback,
                                final ConvertCmdCallback convertCmdCallback,
                                final boolean outputVisible,
                                final int commandTimeout) {
        if (outputVisible) {
            Tools.getGUIData().setTerminalPanel(getTerminalPanel());
        }
        return ssh.execCommand(Tools.getDistCommand(commandString,
                                                    dist,
                                                    distVersionString,
                                                    arch,
                                                    convertCmdCallback),
                               progressBar,
                               callback,
                               true,
                               outputVisible,
                               true,
                               commandTimeout);
    }

    /**
     * Executes get status command which runs in the background and updates the
     * block device object. The command is 'drbdsetup /dev/drbdX events'
     * The session is stored, so that in can be stopped with 'stop' button.
     */
    public final void execDrbdStatusCommand(
                                final ExecCallback execCallback,
                                final NewOutputCallback outputCallback) {
        if (drbdStatusThread == null) {
            drbdStatusThread = ssh.execCommand(
                                Tools.getDistCommand(
                                                "DRBD.getDrbdStatus",
                                                dist,
                                                distVersionString,
                                                arch,
                                                null), /* ConvertCmdCallback */
                                    execCallback,
                                    outputCallback,
                                    false,
                                    false,
                                    45000);
        } else {
            Tools.appWarning("trying to start started drbd status");
        }
    }

    /**
     * Stops drbd status background process.
     */
    public final void stopDrbdStatus() {
        if (drbdStatusThread == null) {
            Tools.appWarning("trying to stop stopped drbd status");
            return;
        }
        drbdStatusThread.cancel();
        drbdStatusThread = null;
    }

    /**
     * Waits till the drbd status command finishes.
     */
    public final void waitOnDrbdStatus() {
        if (drbdStatusThread != null) {
            try {
                /* it probably hangs after this timeout, so it will be
                 * killed. */
                drbdStatusThread.join(DRBD_STATUS_TIMEOUT);
            } catch (java.lang.InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            stopDrbdStatus();
        }
    }

    /**
     * Executes an hb status command.
     */
    public final void execClStatusCommand(
                                final ExecCallback execCallback,
                                final NewOutputCallback outputCallback) {
        if (clStatusThread == null) {
            clStatusThread = ssh.execCommand(
                            Tools.getDistCommand("Heartbeat.getClStatus",
                                                 dist,
                                                 distVersionString,
                                                 arch,
                                                 null), /* ConvertCmdCallback */
                                execCallback,
                                outputCallback,
                                false,
                                false,
                                30000);
        } else {
            Tools.appWarning("trying to start started hb status");
        }
    }

    /**
     * Waits while the hb status thread finishes.
     */
    public final void waitOnClStatus() {
        try {
            clStatusThread.join();
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        clStatusThread = null;
    }

    /**
     * Stops hb status background process.
     */
    public final void stopClStatus() {
        if (clStatusThread == null) {
            Tools.appWarning("trying to stop stopped hb status");
            return;
        }
        clStatusThread.cancel();
    }

    /**
     * Gets ip. There can be more ips, delimited with ","
     */
    public final String getIp() {
        return ip;
    }

    /**
     * Returns the ip for the hop.
     */
    public final String getIp(final int hop) {
        if (ip == null) {
            return null;
        }
        final String[] ips = ip.split(",");
        if (ips.length < hop + 1) {
            return null;
        }
        return ips[hop];
    }

    /**
     * Return first hop ip.
     */
    public final String getFirstIp() {
        final String[] ips = ip.split(",");
        return ips[0];
    }
    /**
     * Returns username.
     */
    public final String getUsername() {
        return username;
    }

    /**
     * Returns first username in a hop.
     */
    public final String getFirstUsername() {
        final String[] usernames = username.split(",");
        return usernames[0];
    }

    /**
     * Gets hostname as entered by user.
     */
    public final String getHostnameEntered() {
        return hostnameEntered;
    }

    /**
     * Escapes the quotes for the stacked ssh commands.
     */
    public final String escapeQuotes(final String s, final int count) {
        if (s == null) {
            return null;
        }
        if (count <= 0) {
            return s;
        }
        final StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '"' || c == '$' || c == '`') {
                sb.append('\\');
                sb.append(c);
            } else {
                sb.append(c);
            }
        }
        return escapeQuotes(sb.toString(), count - 1);
    }

    /** Returns sudo prefix. */
    public final String getSudoPrefix() {
        if (useSudo != null && useSudo) {
            return "echo \""
                   + escapeQuotes(sudoPassword, 1)
                   + "\"|sudo -S -p '' ";
        } else {
            return "";
        }
    }
    /** Returns command exclosed in sh -c "". */
    public final String getSudoCommand(final String command) {
        if (useSudo != null && useSudo) {
            return "trap - SIGPIPE;"
                   + getSudoPrefix()
                   + "bash -c \"trap - SIGPIPE; { "
                   + escapeQuotes(command, 1) + "; } 2>&1\" 2>/dev/null";
        } else {
            return command;
        }
    }

    /**
     * Returns command with all the sshs that will be hopped.
     *
     * ssh -A   -tt -l root x.x.x.x "ssh -A   -tt -l root x.x.x.x \"ssh
     * -A   -tt -l root x.x.x.x \\\"ls\\\"\""
     */
    public final String getHoppedCommand(final String command) {
        final int hops = Tools.charCount(ip, ',') + 1;
        final String[] usernames = username.split(",");
        final String[] ips = ip.split(",");
        final StringBuffer s = new StringBuffer(200);
        if (hops > 1) {
            String sshAgentPid = "";
            String sshAgentSock = "";
            final Map<String, String> variables = System.getenv();
            for (final String name : variables.keySet()) {
                final String value = variables.get(name);
                if ("SSH_AGENT_PID".equals(name)) {
                    sshAgentPid = value;
                } else if ("SSH_AUTH_SOCK".equals(name)) {
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
            s.append(usernames[i]);
            s.append(' ');
            s.append(ips[i]);
            s.append(' ');
            s.append(escapeQuotes("\"", i - 1));
        }

        s.append(escapeQuotes(command, hops - 1));

        for (int i = hops - 1; i > 0; i--) {
            s.append(escapeQuotes("\"", i - 1));
        }
        return s.toString();
    }

    /**
     * Returns hostname of this host.
     */
    public final String getHostname() {
        return hostname;
    }

    /**
     * Returns the host name.
     */
    public final String toString() {
        return getName();
    }

    /**
     * Gets name, that is shown in the tab. Name is either host name, if it is
     * set or ip.
     */
    public final String getName() {
        if (name == null) {
            String nodeName;
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
                return ip;
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

    /**
     * Sets name of the host as it will be identified.
     */
    public final void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets string with user and hostname as used in prompt or ssh like
     * rasto@linbit.at.
     */
    public final String getUserAtHost() {
        return username + "@" + getHostname();
    }

    /**
     * Gets SSH object.
     */
    public final SSH getSSH() {
        return ssh;
    }


    /**
     * Sets terminal panel object. This is the panel where the commands and
     * their results are shown for every host.
     */
    public final void setTerminalPanel(final TerminalPanel terminalPanel) {
        this.terminalPanel = terminalPanel;
    }

    /**
     * Gets terminal panel object.
     */
    public final TerminalPanel getTerminalPanel() {
        return terminalPanel;
    }

    /**
     * Connects host with ssh. Dialog is needed, in case if password etc.
     * has to be entered. Connection is made in the background, after
     * connection is established, callback.done() is called. In case
     * of error callback.doneError() is called.
     */
    public final void connect(final SSHGui sshGui,
                              final ConnectionCallback callback) {
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
    public final void connect(final SSHGui sshGui,
                        final ProgressBar progressBar,
                        final ConnectionCallback callback) {
        Tools.debug(this, "host connect: " + sshGui, 1);
        ssh.connect(sshGui, progressBar, callback, this);
    }

    /**
     * Register a component that will be enabled if the host connected and
     * disabled if disconnected.
     */
    public final void registerEnableOnConnect(final JComponent c) {
        if (!enableOnConnectList.contains(c)) {
            enableOnConnectList.add(c);
        }
        c.setEnabled(isConnected());
    }

    /**
     * Is called after the host is connected or disconnected and
     * enables/disables the conponents that are registered to be enabled on
     * connect.
     */
    public final void setConnected() {
        for (final JComponent c : enableOnConnectList) {
            c.setEnabled(isConnected());
        }
    }

    /**
     * Make an ssh connection to the host.
     */
    public final void connect(SSHGui sshGui) {
        if (!isConnected()) {
            final String hostName = getName();
            Tools.startProgressIndicator(hostName, "Connecting...");
            if (sshGui == null) {
                sshGui = new SSHGui(Tools.getGUIData().getMainFrame(),
                                    this,
                                    null);
            }

            connect(sshGui,
                    new ConnectionCallback() {
                        public void done(final int flag) {
                            setConnected();
                            getSSH().installGuiHelper();
                            getAllInfo();
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    Tools.stopProgressIndicator(
                                                          hostName,
                                                          "Connecting...");
                                }
                            });
                        }

                        public void doneError(
                                        final String errorText) {
                            setLoadingError();
                            setConnected();
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    Tools.stopProgressIndicator(
                                                hostName,
                                                "Connecting..."); }
                            });
                        }
                    });
        }
    }

    /**
     * Gets and stores info about the host.
     */
    public final void getAllInfo() {
        final Thread t = execCommand("GetHostAllInfo",
                         new ExecCallback() {
                             public void done(final String ans) {
                                 parseHostInfo(ans);
                                 setLoadingDone();
                             }

                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 setLoadingError();
                             }
                         },
                         null, /* ConvertCmdCallback */
                         false,
                         SSH.DEFAULT_COMMAND_TIMEOUT);
        try {
            t.join(0);
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Gets and stores hardware info about the host.
     */
    public final void getHWInfo(final CategoryInfo[] infosToUpdate) {
        final Thread t = execCommand("GetHostHWInfo",
                         new ExecCallback() {
                             public void done(final String ans) {
                                 if (!ans.equals(oldHwInfo)) {
                                    parseHostInfo(ans);
                                    oldHwInfo = ans;
                                    for (final CategoryInfo ci
                                                        : infosToUpdate) {
                                        ci.updateTable(CategoryInfo.MAIN_TABLE);
                                    }
                                 }
                                 setLoadingDone();
                             }

                             public void doneError(final String ans,
                                                   final int exitCode) {
                                 setLoadingError();
                             }
                         },
                         null, /* ConvertCmdCallback */
                         false,
                         SSH.DEFAULT_COMMAND_TIMEOUT);
        try {
            t.join(0);
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }



    /**
     * Returns whether host ssh connection was established.
     */
    public final boolean isConnected() {
        if (ssh == null) {
            ////TODO: maybe try to reconnect here
            //ssh = new SSH();
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
    public final String replaceVars(String command,
                                    final boolean hidePassword) {
        if (command.indexOf("@USER@") > -1) {
            command = command.replaceAll(
                                    "@USER@",
                                    Tools.getConfigData().getDownloadUser());
        }
        if (command.indexOf("@PASSWORD@") > -1) {
            if (hidePassword) {
                command = command.replaceAll("@PASSWORD@", "*****");
            } else {
                command = command.replaceAll(
                                   "@PASSWORD@",
                                   Tools.getConfigData().getDownloadPassword());
            }
        }
        String supportDir = "support";
        if (Tools.getConfigData().isStagingDrbd()) {
            supportDir = "support/staging";
        }
        final String drbdDir = "drbd";
        if (kernelVersion != null
            && command.indexOf("@KERNELVERSIONDIR@") > -1) {
            command = command.replaceAll("@KERNELVERSIONDIR@", kernelVersion);
        }
        if (drbdVersionToInstall != null
            && command.indexOf("@DRBDVERSION@") > -1) {
            command = command.replaceAll("@DRBDVERSION@", drbdVersionToInstall);
        }
        if (distVersion != null
            && command.indexOf("@DISTRIBUTION@") > -1) {
            command = command.replaceAll("@DISTRIBUTION@", distVersion);
        }
        if (arch != null
            && command.indexOf("@ARCH@") > -1) {
            command = command.replaceAll("@ARCH@", arch);
        }
        if (drbdBuildToInstall != null
            && command.indexOf("@BUILD@") > -1) {
            command = command.replaceAll("@BUILD@", drbdBuildToInstall);
        }
        if (drbdPackagesToInstall != null
            && command.indexOf("@DRBDPACKAGES@") > -1) {
            command = command.replaceAll("@DRBDPACKAGES@",
                                         drbdPackagesToInstall);
        }
        if (command.indexOf("@SUPPORTDIR@") > -1) {
            command = command.replaceAll("@SUPPORTDIR@", supportDir);
        }
        if (command.indexOf("@DRBDDIR@") > -1) {
            command = command.replaceAll("@DRBDDIR@", drbdDir);
        }
        if (command.indexOf("@GUI-HELPER@") > -1) {
            command = command.replaceAll("@GUI-HELPER@",
                                         "/usr/local/bin/drbd-gui-helper-"
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
    public final String replaceVars(final String command) {
        return replaceVars(command, false);
    }

    /**
     * Parses the host info.
     */
    public final void parseHostInfo(final String ans) {
        Tools.debug(this, "updating host info: " + getName(), 1);
        final String[] lines = ans.split("\\r?\\n");
        String type = "";
        final List<String> versionLines = new ArrayList<String>();
        final Map<String, BlockDevice> newBlockDevices =
                                     new LinkedHashMap<String, BlockDevice>();
        final Map<String, NetInterface> newNetInterfaces =
                                     new LinkedHashMap<String, NetInterface>();
        final Pattern bdP = Pattern.compile("(\\D+)\\d+");
        for (String line : lines) {
            if (line.indexOf("ERROR:") == 0) {
                break;
            } else if (line.indexOf("WARNING:") == 0) {
                continue;
            }
            if ("net-info".equals(line)
                || "disk-info".equals(line)
                || "filesystems-info".equals(line)
                || "crypto-info".equals(line)
                || "mount-points-info".equals(line)
                || "gui-info".equals(line)
                || "installation-info".equals(line)
                || "version-info".equals(line)) {
                type = line;
                continue;
            }
            if ("net-info".equals(type)) {
                NetInterface netInterface = new NetInterface(line);
                if (netInterfaces.containsKey(netInterface.getName())) {
                    netInterface = netInterfaces.get(netInterface.getName());
                }
                newNetInterfaces.put(netInterface.getName(), netInterface);
            } else if ("disk-info".equals(type)) {
                BlockDevice blockDevice = new BlockDevice(line);
                final String name = blockDevice.getName();
                if (blockDevices.containsKey(name)) {
                    /* get the existing block device object,
                       forget the new one. */
                    blockDevice = blockDevices.get(name);
                    blockDevice.update(line);
                }
                newBlockDevices.put(name, blockDevice);
                final Matcher m = bdP.matcher(name);
                if (m.matches()) {
                    newBlockDevices.remove(m.group(1));
                }
            } else if ("filesystems-info".equals(type)) {
                addFileSystem(line);
            } else if ("crypto-info".equals(type)) {
                addCryptoModule(line);
            } else if ("mount-points-info".equals(type)) {
                addMountPoint(line);
            } else if ("gui-info".equals(type)) {
                parseGuiInfo(line);
            } else if ("installation-info".equals(type)) {
                parseInstallationInfo(line);
            } else if ("version-info".equals(type)) {
                versionLines.add(line);
            }
        }
        setDistInfo(versionLines.toArray(new String[versionLines.size()]));
        blockDevices = newBlockDevices;
        netInterfaces = newNetInterfaces;
        getBrowser().updateHWResources(getNetInterfaces(),
                                       getBlockDevices(),
                                       getFileSystems());
    }

    /**
     * Parses the gui info, with drbd and heartbeat graph positions.
     */
    private void parseGuiInfo(final String line) {
        final String[] tokens = line.split(";");
        String id = null;
        String x = null;
        String y = null;
        for (String token : tokens) {
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

    /**
     * Parses the installation info.
     */
    public final void parseInstallationInfo(final String line) {
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
        } else if ("cs-ais-rc".equals(tokens[0])) {
            if (tokens.length == 2) {
                csAisIsRc = tokens[1].trim();
            } else {
                csAisIsRc = null;
            }
        } else if ("cs-ais-conf".equals(tokens[0])) {
            if (tokens.length == 2) {
                csAisConf = tokens[1].trim();
            } else {
                csAisConf = null;
            }
        } else if ("cs-ais-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                csAisRunning = tokens[1].trim();
            } else {
                csAisRunning = null;
            }
        } else if ("hb".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatVersion = tokens[1].trim();
            } else {
                heartbeatVersion = null;
            }
        } else if ("hb-rc".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatIsRc = tokens[1].trim();
            } else {
                heartbeatIsRc = null;
            }
        } else if ("hb-conf".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatConf = tokens[1].trim();
            } else {
                heartbeatConf = null;
            }
        } else if ("hb-running".equals(tokens[0])) {
            if (tokens.length == 2) {
                heartbeatRunning = tokens[1].trim();
            } else {
                heartbeatRunning = null;
            }
        } else if ("drbd-loaded".equals(tokens[0])) {
            if (tokens.length == 2) {
                drbdLoaded = tokens[1].trim();
            } else {
                drbdLoaded = null;
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
                drbdVersion = tokens[1].trim();
            } else {
                drbdVersion = null;
            }
        } else if ("drbd-mod".equals(tokens[0])) {
            if (tokens.length == 2) {
                drbdModuleVersion = tokens[1].trim();
            } else {
                drbdModuleVersion = null;
            }
        }
    }

    /**
     * Returns the graph position of id.
     */
    public final Point2D getGraphPosition(final String id) {
        if (servicePositions.containsKey(id)) {
            return servicePositions.get(id);
        }
        return null;
    }

    /**
     * Resets the graph positions.
     */
    public final void resetGraphPosition(final String id) {
        servicePositions.remove(id);
    }

    /**
     * Saves the positions in the graphs.
     */
    public final void saveGraphPositions(final Map<String, Point2D> positions) {
        final StringBuffer lines = new StringBuffer();
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
            lines.append(id + ";x=" + x + ";y=" + y + "\n");
        }
        getSSH().createConfig(lines.toString(),
                              "drbdgui.cf",
                              "/var/lib/heartbeat/",
                              "0600",
                              false);
    }

    /**
     * Sets the heartbeat version.
     */
    public final void setHeartbeatVersion(final String heartbeatVersion) {
        this.heartbeatVersion = heartbeatVersion;
    }

    /**
     * Sets the corosync version.
     */
    public final void setCorosyncVersion(final String corosyncVersion) {
        this.corosyncVersion = corosyncVersion;
    }

    /**
     * Sets the pacemaker version.
     */
    public final void setPacemakerVersion(final String pacemakerVersion) {
        this.pacemakerVersion = pacemakerVersion;
    }

    /**
     * Sets the openais version.
     */
    public final void setOpenaisVersion(final String openaisVersion) {
        this.openaisVersion = openaisVersion;
    }

    /**
     * Returns the pacemaker version.
     */
    public final String getPacemakerVersion() {
        return pacemakerVersion;
    }

    /**
     * Returns the corosync version.
     */
    public final String getCorosyncVersion() {
        return corosyncVersion;
    }

    /**
     * Returns whether corosync is installed.
     */
    public final boolean isCorosync() {
        return corosyncVersion != null;
    }

    /**
     * Returns the openais version.
     */
    public final String getOpenaisVersion() {
        return openaisVersion;
    }

    /**
     * Returns the heartbeat version.
     */
    public final String getHeartbeatVersion() {
        return heartbeatVersion;
    }

    /**
     * Sets that drbd will be upgraded.
     */
    public final void setDrbdWillBeUpgraded(final boolean drbdWillBeUpgraded) {
        this.drbdWillBeUpgraded = drbdWillBeUpgraded;
    }

    /**
     * Sets that drbd was installed.
     */
    public final void setDrbdWasInstalled(final boolean drbdWasInstalled) {
        this.drbdWasInstalled = drbdWasInstalled;
    }

    /**
     * Returns true if drbd will be upgraded and drbd was installed.
     * TODO: ???
     */
    public final boolean isDrbdUpgraded() {
        return drbdWillBeUpgraded && drbdWasInstalled;
    }

    /**
     * Sets the 'is loading' latch, so that something can wait while the load
     * sequence is running.
     */
    public final void setIsLoading() {
        isLoadingGate = new CountDownLatch(1);
    }

    /**
     * Waits on the 'is loading' latch.
     */
    public final void waitOnLoading() {
        try {
            isLoadingGate.await();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * When loading is done, this latch is opened and whatever is waiting on it
     * is notified.
     */
    public final void setLoadingDone() {
        isLoadingGate.countDown();
    }

    /**
     * When loading is done but with an error. Currently it is the same as
     * setLoadingDone().
     */
    public final void setLoadingError() {
        isLoadingGate.countDown();
    }

    /** Returns ssh port. */
    public final String getSSHPort() {
        return sshPort;
    }

    /** Returns ssh port as integer. */
    public final int getSSHPortInt() {
        return Integer.valueOf(sshPort);
    }

    /** Sets ssh port. */
    public final void setSSHPort(final String sshPort) {
        this.sshPort = sshPort;
    }

    /** Returns sudo password. */
    public final String getSudoPassword() {
        return sudoPassword;
    }

    /** Sets sudo password. */
    public final void setSudoPassword(final String sudoPassword) {
        this.sudoPassword = sudoPassword;
    }

    /** Returns whether sudo is used. */
    public final Boolean isUseSudo() {
        return useSudo;
    }

    /** Sets whether sudo should be used. */
    public final void setUseSudo(final Boolean useSudo) {
        this.useSudo = useSudo;
    }

    /**
     * Sets openais/pacemaker installation method index.
     */
    public final void setPmInstallMethod(final String pmInstallMethod) {
        this.pmInstallMethod = pmInstallMethod;
    }

    /**
     * Returns openais/pacemaker installation method.
     */
    public final String getPmInstallMethod() {
        return pmInstallMethod;
    }

    /**
     * Sets heartbeat/pacemaker installation method index.
     */
    public final void setHbPmInstallMethod(final String hbPmInstallMethod) {
        this.hbPmInstallMethod = hbPmInstallMethod;
    }

    /**
     * Returns heartbeat/pacemaker installation method.
     */
    public final String getHbPmInstallMethod() {
        return hbPmInstallMethod;
    }

    /**
     * Sets drbd installation method index.
     */
    public final void setDrbdInstallMethod(final String drbdInstallMethod) {
        this.drbdInstallMethod = drbdInstallMethod;
    }

    /**
     * Returns drbd installation method.
     */
    public final String getDrbdInstallMethod() {
        return drbdInstallMethod;
    }

    /**
     * Returns whether Corosync/Openais is rc script.
     */
    public final boolean isCsAisRc() {
       return csAisIsRc != null && csAisIsRc.equals("on");
    }

    /**
     * Returns whether Corosync/Openais is running script.
     */
    public final boolean isCsAisRunning() {
       return csAisRunning != null && csAisRunning.equals("on");
    }

    /**
     * Returns whether Corosync/Openais config exists.
     */
    public final boolean isCsAisConf() {
       return csAisConf != null && csAisConf.equals("on");
    }

    /**
     * Returns whether Heartbeat is rc script.
     */
    public final boolean isHeartbeatRc() {
       return heartbeatIsRc != null && heartbeatIsRc.equals("on");
    }

    /**
     * Returns whether Heartbeat is running script.
     */
    public final boolean isHeartbeatRunning() {
       return heartbeatRunning != null && heartbeatRunning.equals("on");
    }

    /**
     * Returns whether Heartbeat config exists.
     */
    public final boolean isHeartbeatConf() {
       return heartbeatConf != null && heartbeatConf.equals("on");
    }

    /**
     * Returns whether drbd module is loaded.
     */
    public final boolean isDrbdLoaded() {
       return drbdLoaded != null && drbdLoaded.equals("on");
    }

    /**
     * Returns MD5 checksum of VM Info from server.
     */
    public final String getVMInfoMD5() {
        return vmInfoMD5;
    }

    /**
     * Sets MD5 checksum of VM Info from server.
     */
    public final void setVMInfoMD5(final String vmInfoMD5) {
        this.vmInfoMD5 = vmInfoMD5;
    }

    /**
     * Sets index of this host in cluster.
     */
    public final void setIndex(final int index) {
        this.index = index;
    }

    /**
     * Returns index of this host in cluster.
     */
    public final int getIndex() {
        return index;
    }

    /** This is part of testsuite, it checks cib. */
    public final boolean checkTest(final String test, final double index) {
        Tools.sleep(1500);
        final StringBuffer command = new StringBuffer(50);
        command.append(replaceVars("@GUI-HELPER@"));
        command.append(" gui-test ");
        command.append(test);
        command.append(' ');
        final String indexString =
                            Double.toString(index).replaceFirst("\\.0+$", "");
        command.append(indexString);
        for (final Host host : getCluster().getHosts()) {
            command.append(' ');
            command.append(host.getName());
        }
        command.append(" 2>&1");
        int i = 0;
        SSH.SSHOutput out = null;
        while (i < 8) {
            out = getSSH().execCommandAndWait(command.toString(),
                                              false,
                                              false,
                                              60000);
            /* 10 - no such file */
            if (out.getExitCode() == 0 || out.getExitCode() == 10) {
                break;
            }
            i++;
            Tools.sleep(i * 2000);
        }
        if (i > 0) {
            Tools.info(test + " " + index + " tries: " + (i + 1));
        }
        Tools.info(test + " " + index + " " + out.getOutput());
        return out.getExitCode() == 0;
    }

    /** Returns color of this host. Null if it is default color. */
    final public String getColor() {
        if (savedColor == null || defaultColor == savedColor) {
            return null;
        }
        return Integer.toString(savedColor.getRGB());
    }

    /** Sets color of this host. Don't if it is default color. */
    final public void setSavedColor(final String colorString) {
        try {
            savedColor = new Color(Integer.parseInt(colorString));
        } catch (java.lang.NumberFormatException e) {
            /* ignore it */
        }
    }
}
