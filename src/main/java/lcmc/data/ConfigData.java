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

import java.io.IOException;
import java.io.File;
import java.util.EnumMap;
import lcmc.utilities.Tools;
import lcmc.utilities.RoboTest;
import ch.ethz.ssh2.KnownHosts;
import org.apache.commons.collections15.map.MultiKeyMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * ConfigData
 *
 * Holds data, that are used globaly in the application and provides some
 * functions for this data.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public final class ConfigData {
    /** Logger. */
    private static final Logger LOG =
                                   LoggerFactory.getLogger(ConfigData.class);
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** access type. */
    public static enum AccessType { RO, OP, ADMIN, GOD, NEVER };
    /** Read only operating mode. */
    public static final String OP_MODE_RO =
                                        Tools.getString("ConfigData.OpMode.RO");

    /** Operator Level 1 operating mode. */
    private static final String OP_MODE_OP =
                                       Tools.getString("ConfigData.OpMode.OP");

    /** Administrator Level 1 operating mode. */
    private static final String OP_MODE_ADMIN =
                                    Tools.getString("ConfigData.OpMode.ADMIN");

    /** Developer Level 10 operating mode. */
    public static final String OP_MODE_GOD =
                                       Tools.getString("ConfigData.OpMode.GOD");
    /** Map from access type to its string representation. */
    public static final Map<AccessType, String> OP_MODES_MAP =
                                    new EnumMap<AccessType, String>(AccessType.class);
    /** String representation to its access type. */
    public static final Map<String, AccessType> ACCESS_TYPE_MAP =
                                       new LinkedHashMap<String, AccessType>();
    static {
        OP_MODES_MAP.put(AccessType.RO, OP_MODE_RO);
        OP_MODES_MAP.put(AccessType.OP, OP_MODE_OP);
        OP_MODES_MAP.put(AccessType.ADMIN, OP_MODE_ADMIN);
        OP_MODES_MAP.put(AccessType.GOD, OP_MODE_GOD);

        ACCESS_TYPE_MAP.put(OP_MODE_RO, AccessType.RO);
        ACCESS_TYPE_MAP.put(OP_MODE_OP, AccessType.OP);
        ACCESS_TYPE_MAP.put(OP_MODE_ADMIN, AccessType.ADMIN);
        ACCESS_TYPE_MAP.put(OP_MODE_GOD, AccessType.GOD);
    }
    /** All hosts object. */
    private final Hosts hosts;
    /** All clusters object. */
    private final Clusters clusters;
    /** Default user for download login. */
    private String downloadUser = Tools.getDefault("DownloadLogin.User");
    /** Default password for download login. */
    private String downloadPassword =
                                    Tools.getDefault("DownloadLogin.Password");
    /** User for download login. */
    private String savedDownloadUser = "";
    /** Password for download login. */
    private String savedDownloadPassword = "";
    /** If set to true user and password will be saved. */
    private boolean loginSave = true;

    /** Whether it is an advanced mode. */
    private boolean advancedMode = false;
    /** Default save file. */
    private String saveFile = Tools.getDefault("MainMenu.DrbdGuiFiles.Default");
    /** Known hosts object. */
    private final KnownHosts knownHosts = new KnownHosts();
    /** Known hosts path. */
    private String knownHostPath;
    /** Id dsa path. */
    private String idDSAPath;
    /** Id rsa path. */
    private String idRSAPath;
    /** Last installed clusterStack. */
    private String lastInstalledClusterStack = null;
    /** Last installed method either Openais or Heartbeat with pacemaker. */
    private String lastHbPmInstalledMethod = null;
    /** Last installed drbd method. */
    private String lastDrbdInstalledMethod = null;
    /** Last entered user. */
    private String lastEnteredUser = null;
    /** Last use sudo. */
    private Boolean lastEnteredUseSudo = null;
    /** Last ssh port. */
    private String lastEnteredSSHPort = null;
    /** Whether drbd gui helper should be overwritten. */
    private boolean keepHelper = false;
    /** Hosts that have auto options. */
    private final List<String> autoHosts = new ArrayList<String>();
    /** Clusters that have auto options. */
    private final List<String> autoClusters = new ArrayList<String>();
    /** Auto options, that make automatic actions in the gui. */
    private final MultiKeyMap<String, String> autoOptions =
                                            new MultiKeyMap<String, String>();
    /** Name of the Heartbeat comm stack. */
    public static final String HEARTBEAT_NAME = "Heartbeat";
    /** Name of the Corosync/Openais comm stack. */
    public static final String COROSYNC_NAME = "Corosync/OpenAIS";
    /** Name of the clone set pacemaker object. */
    public static final String PM_CLONE_SET_NAME = "Clone Set";
    /** Name of the Master/Slave set pacemaker object. */
    public static final String PM_MASTER_SLAVE_SET_NAME = "Master/Slave Set";
    /** Name of the group pacemaker object. */
    public static final String PM_GROUP_NAME = "Group";
    /** Default frames per second for animations. */
    public static final float DEFAULT_ANIM_FPS = 20.0f;
    /** Remote port offset when making ssh tunnel for vnc. */
    private int vncPortOffset = 0;
    /** Whether tight vnc viewer should be used. */
    private boolean tightvnc = false;
    /** Whether ultra vnc viewer should be used. */
    private boolean ultravnc = false;
    /** Whether real vnc viewer should be used. */
    private boolean realvnc = false;
    /** Whether the drbd packages should be downloaded from staging dir. */
    private boolean stagingDrbd = false;
    /** Whether more pacemaker installation options should appear. */
    private boolean stagingPacemaker = false;
    /** Do not show resources that are only in LRM. */
    private boolean noLRM = false;
    /** Frames per second for animations. */
    private float animFPS = DEFAULT_ANIM_FPS;
    /** Access type of the application at the moment. */
    private AccessType accessType = AccessType.ADMIN;
    /** Maximum allowed access type of the application. */
    private AccessType maxAccessType = AccessType.ADMIN;
    /** Whether the upgrade check is enabled. */
    private boolean upgradeCheckEnabled = true;
    /** Whether big drbd.conf and not drbd.d/ should be used. */
    private boolean bigDRBDConf = false;
    /** Allow one host cluster. */
    private boolean oneHostCluster = false;
    /** Scale for fonts and GUI elements. 100 is the same size. */
    private int scale = 100;
    /** Whether no passphrase should be tried first. */
    private boolean noPassphrase = false;
    /** Whether to embed applet in the browser. Embed in Linux by default. */
    private boolean embed = Tools.isLinux();
    /** Whether to log commands on the servers. */
    private boolean cmdLog = false;
    /** Auto test, null no auto test. */
    private RoboTest.Test autoTest = null;
    /** Check swing threads. Print stack traces. */
    private boolean checkSwing = false;

    /**
     * Prepares a new <code>ConfigData</code> object and creates new hosts
     * and clusters objects.
     */
    public ConfigData() {
        hosts = new Hosts();
        clusters = new Clusters();
    }

    /** Gets hosts object. */
    public Hosts getHosts() {
        return hosts;
    }

    /** Returns number of hosts that are not part of any cluster. */
    public int danglingHostsCount() {
        final Hosts hosts0 = Tools.getConfigData().getHosts();
        int c = 0;
        for (final Host host : hosts0.getHostSet()) {
            if (!host.isInCluster()) {
                c++;
            }
        }
        return c;
    }

    /** Gets clusters object. */
    public Clusters getClusters() {
        return clusters;
    }

    /** Gets user for download area. */
    public String getDownloadUser() {
        if (savedDownloadUser != null && !savedDownloadUser.equals("")) {
            downloadUser = savedDownloadUser;
            savedDownloadUser = "";
        }
        return downloadUser;
    }

    /** Gets password for download area. */
    public String getDownloadPassword() {
        if (savedDownloadPassword != null
            && !savedDownloadPassword.equals("")) {
            downloadPassword = savedDownloadPassword;
            savedDownloadPassword = "";
        }
        return downloadPassword;
    }

    /**
     * Returns whether the user and password for download area, shuld be saved.
     */
    public boolean getLoginSave() {
        return loginSave;
    }

    /** Sets user and password for download area. */
    public void setDownloadLogin(final String downloadUser,
                                 final String downloadPassword,
                                 final boolean loginSave) {
        this.downloadUser = downloadUser;
        this.downloadPassword = downloadPassword;
        this.loginSave = loginSave;
        if (loginSave) {
            savedDownloadUser = downloadUser;
            savedDownloadPassword = downloadPassword;
        } else {
            savedDownloadUser = "";
            savedDownloadPassword = "";
        }
    }

    /** Return whether host exists in the hosts. */
    public boolean existsHost(final Host host) {
        return hosts.existsHost(host);
    }

    /** Adds host object to the hosts object. */
    public void addHostToHosts(final Host host) {
        hosts.addHost(host);
    }

    /** Removes host object from hosts object. */
    public void removeHostFromHosts(final Host host) {
        hosts.removeHost(host);
    }

    /** Return whether cluster exists in the clusters. */
    public boolean existsCluster(final Cluster cluster) {
        return clusters.existsCluster(cluster);
    }

    /** Adds cluster object to the clusters object. */
    public void addClusterToClusters(final Cluster cluster) {
        clusters.addCluster(cluster);
    }

    /** Removes cluster object from clusters object. */
    public void removeClusterFromClusters(final Cluster cluster) {
        clusters.removeCluster(cluster);
    }

    /** Disconnects all hosts. */
    public void disconnectAllHosts() {
        hosts.disconnectAllHosts();
    }

    /** Sets global advanced mode. */
    public void setAdvancedMode(final boolean advancedMode) {
        this.advancedMode = advancedMode;
    }

    /** Gets advanced mode. */
    public boolean isAdvancedMode() {
        return advancedMode;
    }

    /** Sets file name where gui data are saved. */
    public void setSaveFile(final String saveFile) {
        this.saveFile = saveFile;
    }

    /** Returns file name where gui data were saved the last time. */
    public String getSaveFile() {
        return saveFile;
    }

    /** Returns file name where gui data were saved the last time. The old
     * location, drbd-gui.drbdg. */
    public String getSaveFileOld() {
        return Tools.getDefault("MainMenu.DrbdGuiFiles.Old");
    }

    /** Returns path of the known host file. */
    public String getKnownHostPath() {
        return knownHostPath;
    }

    /** Sets path of the known host file. */
    public void setKnownHostPath(final String knownHostPath) {
        this.knownHostPath = knownHostPath;
        final File knownHostFile = new File(knownHostPath);
        if (knownHostFile.exists()) {
            try {
                knownHosts.addHostkeys(knownHostFile);
            } catch (IOException e) {
                LOG.appError("SSH.knowHostFile.NotExists", "", e);
            }
        }
    }

    /** Returns Id DSA path. */
    public String getIdDSAPath() {
        return idDSAPath;
    }

    /** Sets Id DSA path. */
    public void setIdDSAPath(final String idDSAPath) {
        this.idDSAPath = idDSAPath;
    }

    /** Returns Id RSA path. */
    public String getIdRSAPath() {
        return idRSAPath;
    }

    /** Sets Id RSA path. */
    public void setIdRSAPath(final String idRSAPath) {
        this.idRSAPath = idRSAPath;
    }

    /** Returns the known hosts object. */
    public KnownHosts getKnownHosts() {
        return knownHosts;
    }

    /** Sets what was the last installed cluster stack. */
    public void setLastInstalledClusterStack(
                                    final String lastInstalledClusterStack) {
        this.lastInstalledClusterStack = lastInstalledClusterStack;
    }

    /** Returns what was the last installed cluster stack. */
    public String getLastInstalledClusterStack() {
        return lastInstalledClusterStack;
    }

    /** Sets last installed method of either Openais or Heartbeat. */
    public void setLastHbPmInstalledMethod(
                                        final String lastHbPmInstalledMethod) {
        this.lastHbPmInstalledMethod = lastHbPmInstalledMethod;
    }

    /** Returns last installed method of either Openais or Heartbeat. */
    public String getLastHbPmInstalledMethod() {
        return lastHbPmInstalledMethod;
    }

    /** Sets last drbd installed method. */
    public void setLastDrbdInstalledMethod(
                                        final String lastDrbdInstalledMethod) {
        this.lastDrbdInstalledMethod = lastDrbdInstalledMethod;
    }

    /** Returns last drbd installed method. */
    public String getLastDrbdInstalledMethod() {
        return lastDrbdInstalledMethod;
    }

    /** Sets last entered user. */
    public void setLastEnteredUser(final String lastEnteredUser) {
        this.lastEnteredUser = lastEnteredUser;
    }

    /** Gets last entered user. */
    public String getLastEnteredUser() {
        return lastEnteredUser;
    }

    /** Sets last used sudo. */
    public void setLastEnteredUseSudo(final Boolean lastEnteredUseSudo) {
        this.lastEnteredUseSudo = lastEnteredUseSudo;
    }

    /** Returns last entered sudo. */
    public Boolean getLastEnteredUseSudo() {
        return lastEnteredUseSudo;
    }

    /** Sets last ssh port. */
    public void setLastEnteredSSHPort(final String lastEnteredSSHPort) {
        this.lastEnteredSSHPort = lastEnteredSSHPort;
    }

    /** Returns last ssh port. */
    public String getLastEnteredSSHPort() {
        return lastEnteredSSHPort;
    }

    /**
     * Sets whether the drbd gui helper should be kept or overwritten all
     * the time.
     */
    public void setKeepHelper(final boolean keepHelper) {
        this.keepHelper = keepHelper;
    }

    /**
     * Returns whether the drbd gui helper should be kept or overwritten
     * all the time.
     */
    public boolean getKeepHelper() {
        return keepHelper;
    }

    /** Adds auto option that starts automatic actions in the gui. */
    public void addAutoOption(final String hostOrCluster,
                              final String option,
                              final String value) {
        autoOptions.put(hostOrCluster, option, value);
    }

    /** Adds host on which automatic actions will be performed. */
    public void addAutoHost(final String host) {
        autoHosts.add(host);
    }

    /** Returns hosts on which automatic actions will be performed. */
    public List<String> getAutoHosts() {
        return autoHosts;
    }

    /** Removes host after it is done. */
    public void removeAutoHost() {
        if (!autoHosts.isEmpty()) {
            autoHosts.remove(0);
        }
    }

    /** Adds cluster on which automatic actions will be performed. */
    public void addAutoCluster(final String cluster) {
        autoClusters.add(cluster);
    }

    /** Returns clusters on which automatic actions will be performed. */
    public List<String> getAutoClusters() {
        return autoClusters;
    }

    /** Removes cluster after it is done. */
    public void removeAutoCluster() {
        if (!autoClusters.isEmpty()) {
            autoClusters.remove(0);
        }
    }

    /** Returns an auto option for gui testing. */
    String getAutoOption(final String hostOrCluster, final String option) {
        return autoOptions.get(hostOrCluster, option);
    }

    /** Returns an auto option for the first host in the list. */
    public String getAutoOptionHost(final String option) {
        if (autoHosts.isEmpty()) {
            return null;
        }
        return autoOptions.get(autoHosts.get(0), option);
    }

    /** Returns an auto option for first cluster in the list. */
    public String getAutoOptionCluster(final String option) {
        if (autoClusters.isEmpty()) {
            return null;
        }
        return autoOptions.get(autoClusters.get(0), option);
    }

    /** Returns a global option. */
    public String getAutoOptionGlobal(final String option) {
        return autoOptions.get("global", option);
    }

    /** Returns remote port offset when making ssh tunnel for vnc. */
    public int getVncPortOffset() {
        return vncPortOffset;
    }

    /** Sets remote port offset when making ssh tunnel for vnc. */
    public void setVncPortOffset(final int vncPortOffset) {
        this.vncPortOffset = vncPortOffset;
    }

    /** Sets whether tight vnc viewer should be used. */
    public void setTightvnc(final boolean tightvnc) {
        this.tightvnc = tightvnc;
    }

    /** Sets whether ultra vnc viewer should be used. */
    public void setUltravnc(final boolean ultravnc) {
        this.ultravnc = ultravnc;
    }

    /** Sets whether real vnc viewer should be used. */
    public void setRealvnc(final boolean realvnc) {
        this.realvnc = realvnc;
    }

    /** Returns whether tight vnc viewer should be used. */
    public boolean isTightvnc() {
        return tightvnc;
    }

    /** Set whether to show resources that are only in LRM. */
    public void setNoLRM(final boolean noLRM) {
        this.noLRM = noLRM;
    }

    /** Return whether to show resources that are only in LRM. */
    public boolean isNoLRM() {
        return noLRM;
    }


    /**
     * Sets whether the drbd packages should be downloaded from staging
     * directory for testing.
     */
    public void setStagingDrbd(final boolean stagingDrbd) {
        this.stagingDrbd = stagingDrbd;
    }

    /** Sets whether more pacemaker installation options should be shown. */
    public void setStagingPacemaker(final boolean stagingPacemaker) {
        this.stagingPacemaker = stagingPacemaker;
    }

    /** Returns whether more pacemaker installation options should be shown. */
    public boolean isStagingPacemaker() {
        return stagingPacemaker;
    }

    /**
     * Returns whether the drbd packages should be downloaded from staging
     * directory for testing.
     */
    public boolean isStagingDrbd() {
        return stagingDrbd;
    }

    /** Returns whether ultra vnc viewer should be used. */
    public boolean isUltravnc() {
        return ultravnc;
    }

    /** Returns whether real vnc viewer should be used. */
    public boolean isRealvnc() {
        return realvnc;
    }

    /** Returns frames per second for animations. */
    public float getAnimFPS() {
        return animFPS;
    }

    /** Returns whether anim fps is slower than default. */
    public boolean isSlow() {
        return animFPS < DEFAULT_ANIM_FPS;
    }

    /** Returns whether anim fps is faster than default. */
    public boolean isFast() {
        return animFPS > DEFAULT_ANIM_FPS;
    }

    /** Sets frames per second for animations. */
    public void setAnimFPS(final float animFPS) {
        this.animFPS = animFPS;
    }

    /** Sets access type. */
    public void setAccessType(final AccessType accessType) {
        this.accessType = accessType;
    }

    /** Returns access type. */
    public AccessType getAccessType() {
        return accessType;
    }

    /** Sets maximum allowed access type of the application. */
    public void setMaxAccessType(final AccessType maxAccessType) {
        this.maxAccessType = maxAccessType;
    }

    /** Gets maximum allowed access type of the application. */
    AccessType getMaxAccessType() {
        return maxAccessType;
    }

    /**
     * Returns true if the access type is greater than the one that is
     * required and advanced mode is required and we are not in advanced mode.
     */
    public boolean isAccessible(final AccessMode required) {
        return getAccessType().compareTo(required.getAccessType()) > 0
               || (getAccessType().compareTo(required.getAccessType()) == 0
                   && (advancedMode || !required.isAdvancedMode()));
    }

    /** Returns available operating modes. */
    public String[] getOperatingModes() {
        final List<String> modes = new ArrayList<String>();
        for (final AccessType at : ConfigData.OP_MODES_MAP.keySet()) {
            modes.add(OP_MODES_MAP.get(at));
            if (at.equals(maxAccessType)) {
                break;
            }
        }
        return modes.toArray(new String[modes.size()]);
    }

    /** Sets whether the upgrade check should be enabled. */
    public void setUpgradeCheckEnabled(final boolean upgradeCheckEnabled) {
        this.upgradeCheckEnabled = upgradeCheckEnabled;
    }

    /** Returns whether the upgrade check is enabled. */
    public boolean isUpgradeCheckEnabled() {
        return upgradeCheckEnabled;
    }

    /** Returns whether big drbd.conf and not drbd.d/ should be used. */
    public boolean getBigDRBDConf() {
        return bigDRBDConf;
    }

    /** Sets whether big drbd.conf and not drbd.d/ should be used. */
    public void setBigDRBDConf(final boolean bigDRBDConf) {
        this.bigDRBDConf = bigDRBDConf;
    }

    /** Return whether one host cluster is allowed. */
    public boolean isOneHostCluster() {
        return oneHostCluster;
    }

    /** Set whether one host cluster is allowed. */
    public void setOneHostCluster(final boolean oneHostCluster) {
        this.oneHostCluster = oneHostCluster;
    }

    /** Return scaled size. */
    public int scaled(final int size) {
        return size * scale / 100;
    }

    /** Returns scale for fonts and GUI elements. */
    public int getScale() {
        return scale;
    }

    /** Sets scale. */
    public void setScale(final int scale) {
        this.scale = scale;
    }

    /** Set whether no passphrase should be tried first. */
    public void setNoPassphrase(final boolean noPassphrase) {
        this.noPassphrase = noPassphrase;
    }

    /** Return whether no passphrase should be tried first. */
    public boolean isNoPassphrase() {
        return noPassphrase;
    }

    /** Set whether to embed applet in the browser. */
    public void setEmbed(final boolean embed) {
        this.embed = embed;
    }

    /** Return whether to embed applet in the browser. */
    public boolean isEmbed() {
        return embed;
    }

    /** Set auto test. */
    public void setAutoTest(final RoboTest.Test autoTest) {
        this.autoTest = autoTest;
    }

    /** Get auto test. */
    public RoboTest.Test getAutoTest() {
        return autoTest;
    }

    /** Set whether to log commands on the servers. */
    public void setCmdLog(final boolean cmdLog) {
        this.cmdLog = cmdLog;
    }

    /** Return whether to log commands on the servers. */
    public boolean isCmdLog() {
        return cmdLog;
    }

    /** Return whether to check swing threads. Testing only. */
    public boolean isCheckSwing() {
        return checkSwing;
    }

    /** Set whether to check swing threads. Testing only. */
    public void setCheckSwing(final boolean checkSwing) {
        this.checkSwing = checkSwing;
    }
}
