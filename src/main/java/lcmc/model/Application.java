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

package lcmc.model;

import ch.ethz.ssh2.KnownHosts;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lcmc.robotest.Test;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;
import org.apache.commons.collections15.map.MultiKeyMap;

/**
 * Application
 *
 * Holds data, that are used globaly in the application and provides some
 * functions for this data.
 */
public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    public static final String OP_MODE_READONLY = Tools.getString("Application.OpMode.RO");
    private static final String OP_MODE_OPERATOR = Tools.getString("Application.OpMode.OP");
    private static final String OP_MODE_ADMIN = Tools.getString("Application.OpMode.ADMIN");
    public static final String OP_MODE_GOD = Tools.getString("Application.OpMode.GOD");
    public static final Map<AccessType, String> OP_MODES_MAP = new EnumMap<AccessType, String>(AccessType.class);
    public static final Map<String, AccessType> ACCESS_TYPE_MAP = new LinkedHashMap<String, AccessType>();
    public static final String HEARTBEAT_NAME = "Heartbeat";
    public static final String COROSYNC_NAME = "Corosync/OpenAIS";
    public static final String PM_CLONE_SET_NAME = "Clone Set";
    public static final String PM_MASTER_SLAVE_SET_NAME = "Master/Slave Set";
    public static final String PACEMAKER_GROUP_NAME = "Group";
    public static final float DEFAULT_ANIM_FPS = 20.0f;
    static {
        OP_MODES_MAP.put(AccessType.RO, OP_MODE_READONLY);
        OP_MODES_MAP.put(AccessType.OP, OP_MODE_OPERATOR);
        OP_MODES_MAP.put(AccessType.ADMIN, OP_MODE_ADMIN);
        OP_MODES_MAP.put(AccessType.GOD, OP_MODE_GOD);

        ACCESS_TYPE_MAP.put(OP_MODE_READONLY, AccessType.RO);
        ACCESS_TYPE_MAP.put(OP_MODE_OPERATOR, AccessType.OP);
        ACCESS_TYPE_MAP.put(OP_MODE_ADMIN, AccessType.ADMIN);
        ACCESS_TYPE_MAP.put(OP_MODE_GOD, AccessType.GOD);
    }

    public static boolean isLive(final RunMode runMode) {
        return RunMode.LIVE == runMode;
    }

    public static boolean isTest(final RunMode runMode) {
        return RunMode.TEST == runMode;
    }
    private final Hosts allHosts;
    private final Clusters allClusters;
    private String downloadUser = Tools.getDefault("DownloadLogin.User");
    private String downloadPassword = Tools.getDefault("DownloadLogin.Password");
    private String savedDownloadUser = "";
    private String savedDownloadPassword = "";
    private boolean loginSave = true;

    private boolean advancedMode = false;
    private String defaultSaveFile = Tools.getDefault("MainMenu.DrbdGuiFiles.Default");
    private final KnownHosts knownHosts = new KnownHosts();
    private String knownHostPath;
    private String idDSAPath;
    private String idRSAPath;
    private String lastInstalledClusterStack = null;
    private String lastHbPmInstalledMethod = null;
    private String lastDrbdInstalledMethod = null;
    private String lastEnteredUser = null;
    private Boolean lastEnteredUseSudo = null;
    private String lastEnteredSSHPort = null;
    /** Whether drbd gui helper should be overwritten. */
    private boolean keepHelper = false;
    private final List<String> autoHosts = new ArrayList<String>();
    private final List<String> autoClusters = new ArrayList<String>();
    /** Auto options, that make automatic actions in the gui. */
    private final MultiKeyMap<String, String> autoOptions = new MultiKeyMap<String, String>();
    private int vncPortOffset = 0;
    private boolean useTightvnc = false;
    private boolean useUltravnc = false;
    private boolean useRealvnc = false;
    private boolean stagingDrbd = false;
    private boolean stagingPacemaker = false;
    private boolean hideLRM = false;
    private float animFPS = DEFAULT_ANIM_FPS;
    private AccessType accessType = AccessType.ADMIN;
    private AccessType maxAccessType = AccessType.ADMIN;
    private boolean upgradeCheckEnabled = true;
    private boolean bigDRBDConf = false;
    private boolean oneHostCluster = false;
    private int scale = 100;
    private boolean noPassphrase = false;
    private boolean embedApplet = Tools.isLinux();
    private boolean cmdLog = false;
    private Test autoTest = null;
    private boolean checkSwing = false;

    public Application() {
        allHosts = new Hosts();
        allClusters = new Clusters();
    }

    public Hosts getAllHosts() {
        return allHosts;
    }

    public int danglingHostsCount() {
        final Hosts hosts0 = Tools.getApplication().getAllHosts();
        int c = 0;
        for (final Host host : hosts0.getHostSet()) {
            if (!host.isInCluster()) {
                c++;
            }
        }
        return c;
    }

    public Clusters getAllClusters() {
        return allClusters;
    }

    public String getDownloadUser() {
        if (savedDownloadUser != null && !savedDownloadUser.isEmpty()) {
            downloadUser = savedDownloadUser;
            savedDownloadUser = "";
        }
        return downloadUser;
    }

    public String getDownloadPassword() {
        if (savedDownloadPassword != null && !savedDownloadPassword.isEmpty()) {
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

    public void setDownloadLogin(final String downloadUser, final String downloadPassword, final boolean loginSave) {
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

    public boolean existsHost(final Host host) {
        return allHosts.isHostInHosts(host);
    }

    public void addHostToHosts(final Host host) {
        allHosts.addHost(host);
    }

    public void removeHostFromHosts(final Host host) {
        allHosts.removeHost(host);
    }

    public boolean existsCluster(final Cluster cluster) {
        return allClusters.isClusterInClusters(cluster);
    }

    public void addClusterToClusters(final Cluster cluster) {
        allClusters.addCluster(cluster);
    }

    public void removeClusterFromClusters(final Cluster cluster) {
        allClusters.removeCluster(cluster);
    }

    public void disconnectAllHosts() {
        allHosts.disconnectAllHosts();
    }

    public void setAdvancedMode(final boolean advancedMode) {
        this.advancedMode = advancedMode;
    }

    public boolean isAdvancedMode() {
        return advancedMode;
    }

    public void setDefaultSaveFile(final String defaultSaveFile) {
        this.defaultSaveFile = defaultSaveFile;
    }

    public String getDefaultSaveFile() {
        return defaultSaveFile;
    }

    /** Returns file name where gui data were saved the last time. The old
     * location, drbd-gui.drbdg. */
    public String getSaveFileOld() {
        return Tools.getDefault("MainMenu.DrbdGuiFiles.Old");
    }

    public String getKnownHostPath() {
        return knownHostPath;
    }

    public void setKnownHostPath(final String knownHostPath) {
        this.knownHostPath = knownHostPath;
        final File knownHostFile = new File(knownHostPath);
        if (knownHostFile.exists()) {
            try {
                knownHosts.addHostkeys(knownHostFile);
            } catch (final IOException e) {
                LOG.appError("setKnownHostPath: known host file does not exist", "", e);
            }
        }
    }

    public String getIdDSAPath() {
        return idDSAPath;
    }

    public void setIdDSAPath(final String idDSAPath) {
        this.idDSAPath = idDSAPath;
    }

    public String getIdRSAPath() {
        return idRSAPath;
    }

    public void setIdRSAPath(final String idRSAPath) {
        this.idRSAPath = idRSAPath;
    }

    public KnownHosts getKnownHosts() {
        return knownHosts;
    }

    public void setLastInstalledClusterStack(final String lastInstalledClusterStack) {
        this.lastInstalledClusterStack = lastInstalledClusterStack;
    }

    public String getLastInstalledClusterStack() {
        return lastInstalledClusterStack;
    }

    /** Sets last installed method of either Openais or Heartbeat. */
    public void setLastHbPmInstalledMethod(final String lastHbPmInstalledMethod) {
        this.lastHbPmInstalledMethod = lastHbPmInstalledMethod;
    }

    /** Returns last installed method of either Openais or Heartbeat. */
    public String getLastHbPmInstalledMethod() {
        return lastHbPmInstalledMethod;
    }

    public void setLastDrbdInstalledMethod(final String lastDrbdInstalledMethod) {
        this.lastDrbdInstalledMethod = lastDrbdInstalledMethod;
    }

    public String getLastDrbdInstalledMethod() {
        return lastDrbdInstalledMethod;
    }

    public void setLastEnteredUser(final String lastEnteredUser) {
        this.lastEnteredUser = lastEnteredUser;
    }

    public String getLastEnteredUser() {
        return lastEnteredUser;
    }

    public void setLastEnteredUseSudo(final Boolean lastEnteredUseSudo) {
        this.lastEnteredUseSudo = lastEnteredUseSudo;
    }

    public Boolean getLastEnteredUseSudo() {
        return lastEnteredUseSudo;
    }

    public void setLastEnteredSSHPort(final String lastEnteredSSHPort) {
        this.lastEnteredSSHPort = lastEnteredSSHPort;
    }

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
    public void addAutoOption(final String hostOrCluster, final String option, final String value) {
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

    public void removeAutoCluster() {
        if (!autoClusters.isEmpty()) {
            autoClusters.remove(0);
        }
    }

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

    public String getAutoOptionGlobal(final String option) {
        return autoOptions.get("global", option);
    }

    public int getVncPortOffset() {
        return vncPortOffset;
    }

    /** Sets remote port offset when making ssh tunnel for vnc. */
    public void setVncPortOffset(final int vncPortOffset) {
        this.vncPortOffset = vncPortOffset;
    }

    public void setUseTightvnc(final boolean useTightvnc) {
        this.useTightvnc = useTightvnc;
    }

    public void setUseUltravnc(final boolean useUltravnc) {
        this.useUltravnc = useUltravnc;
    }

    public void setUseRealvnc(final boolean useRealvnc) {
        this.useRealvnc = useRealvnc;
    }

    public boolean isUseTightvnc() {
        return useTightvnc;
    }

    public void setHideLRM(final boolean hideLRM) {
        this.hideLRM = hideLRM;
    }

    public boolean isHideLRM() {
        return hideLRM;
    }

    /**
     * Sets whether the drbd packages should be downloaded from staging
     * directory for testing.
     */
    public void setStagingDrbd(final boolean stagingDrbd) {
        this.stagingDrbd = stagingDrbd;
    }

    public void setStagingPacemaker(final boolean stagingPacemaker) {
        this.stagingPacemaker = stagingPacemaker;
    }

    public boolean isStagingPacemaker() {
        return stagingPacemaker;
    }

    public boolean isStagingDrbd() {
        return stagingDrbd;
    }

    public boolean isUseUltravnc() {
        return useUltravnc;
    }

    public boolean isUseRealvnc() {
        return useRealvnc;
    }

    public float getAnimFPS() {
        return animFPS;
    }

    public boolean isSlow() {
        return animFPS < DEFAULT_ANIM_FPS;
    }

    public boolean isFast() {
        return animFPS > DEFAULT_ANIM_FPS;
    }

    public void setAnimFPS(final float animFPS) {
        this.animFPS = animFPS;
    }

    public void setAccessType(final AccessType accessType) {
        this.accessType = accessType;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public void setMaxAccessType(final AccessType maxAccessType) {
        this.maxAccessType = maxAccessType;
    }

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

    public String[] getOperatingModes() {
        final List<String> modes = new ArrayList<String>();
        for (final AccessType at : Application.OP_MODES_MAP.keySet()) {
            modes.add(OP_MODES_MAP.get(at));
            if (at.equals(maxAccessType)) {
                break;
            }
        }
        return modes.toArray(new String[modes.size()]);
    }

    public void setUpgradeCheckEnabled(final boolean upgradeCheckEnabled) {
        this.upgradeCheckEnabled = upgradeCheckEnabled;
    }

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

    public boolean isOneHostCluster() {
        return oneHostCluster;
    }

    public void setOneHostCluster(final boolean oneHostCluster) {
        this.oneHostCluster = oneHostCluster;
    }

    public int scaled(final int size) {
        return size * scale / 100;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(final int scale) {
        this.scale = scale;
    }

    public void setNoPassphrase(final boolean noPassphrase) {
        this.noPassphrase = noPassphrase;
    }

    public boolean isNoPassphrase() {
        return noPassphrase;
    }

    public void setEmbedApplet(final boolean embedApplet) {
        this.embedApplet = embedApplet;
    }

    public boolean isEmbedApplet() {
        return embedApplet;
    }

    public void setAutoTest(final Test autoTest) {
        this.autoTest = autoTest;
    }

    public Test getAutoTest() {
        return autoTest;
    }

    public void setCmdLog(final boolean cmdLog) {
        this.cmdLog = cmdLog;
    }

    public boolean isCmdLog() {
        return cmdLog;
    }

    public boolean isCheckSwing() {
        return checkSwing;
    }

    public void setCheckSwing(final boolean checkSwing) {
        this.checkSwing = checkSwing;
    }

    public enum AccessType {
        RO,
        OP,
        ADMIN,
        GOD,
        NEVER
    }

    /**
     * Run mode. TEST does shows changes in the GUI, but does not change the
     * cluster
     */
    public enum RunMode {
        LIVE,
        TEST
    }
}
