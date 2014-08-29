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

import java.awt.Font;
import java.awt.Insets;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import lcmc.gui.ClusterBrowser;
import lcmc.gui.GUIData;
import lcmc.gui.dialog.ConfirmDialog;
import lcmc.robotest.Test;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;
import org.apache.commons.collections15.map.MultiKeyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Provider;
import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * Application
 *
 * Holds data, that are used globaly in the application and provides some
 * functions for this data.
 */
@Component
public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    public static final String OP_MODE_READONLY = Tools.getString("Application.OpMode.RO");
    public static final boolean CHECK_SWING_THREAD = true;
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
    @Autowired
    private Hosts allHosts;
    @Autowired
    private Clusters allClusters;
    @Autowired
    private GUIData guiData;
    @Autowired
    private UserConfig userConfig;
    @Autowired
    private Provider<ConfirmDialog> confirmDialogProvider;

    public int danglingHostsCount() {
        int c = 0;
        for (final Host host : allHosts.getHostSet()) {
            if (!host.isInCluster()) {
                c++;
            }
        }
        return c;
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

    /** Removes the specified clusters from the gui. */
    public void removeClusters(final Iterable<Cluster> selectedClusters) {
        for (final Cluster cluster : selectedClusters) {
            LOG.debug1("removeClusters: remove hosts from cluster: " + cluster.getName());
            removeClusterFromClusters(cluster);
            for (final Host host : cluster.getHosts()) {
                host.removeFromCluster();
            }
        }
    }

    /** Removes all the hosts and clusters from all the panels and data. */
    public void removeEverything() {
        guiData.startProgressIndicator(Tools.getString("MainMenu.RemoveEverything"));
        disconnectAllHosts();
        guiData.getClustersPanel().removeAllTabs();
        guiData.stopProgressIndicator(Tools.getString("MainMenu.RemoveEverything"));
    }

    /**
     * @param saveAll whether to save clusters specified from the command line
     */
    public void saveConfig(final String filename,
                           final boolean saveAll) {
        LOG.debug1("save: start");
        final String text = Tools.getString("Tools.Saving").replaceAll("@FILENAME@",
                                            Matcher.quoteReplacement(filename));
        guiData.startProgressIndicator(text);
        try {
            final FileOutputStream fileOut = new FileOutputStream(filename);
            userConfig.saveXML(fileOut, saveAll);
            LOG.debug("save: filename: " + filename);
        } catch (final IOException e) {
            LOG.appError("save: error saving: " + filename, "", e);
        } finally {
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            for (final Cluster cluster : allClusters.getClusterSet()) {
                final ClusterBrowser cb = cluster.getBrowser();
                if (cb != null) {
                    cb.saveGraphPositions();
                }
            }
            guiData.stopProgressIndicator(text);
        }
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

    /**
     * Returns default value for integer option from AppDefaults resource
     * bundle and scales it according the --scale option.
     */
    public int getDefaultSize(final String option) {
        return scaled(Tools.getDefaultInt(option));
    }

    /** Starts Real VNC viewer. */
    public void startRealVncViewer(final Host host, final int remotePort) {
        final int localPort = prepareVncViewer(host, remotePort);
        if (localPort < 0) {
            return;
        }
        final vncviewer.VNCViewer v = new vncviewer.VNCViewer(new String[]{"127.0.0.1:"
                + (Integer.toString(localPort - 5900))});

        v.start();
        v.join();
        cleanupVncViewer(host, localPort);
    }

    /**
     * Prepares vnc viewer, gets the port and creates ssh tunnel. Returns true
     * if ssh tunnel was created.
     */
    private int prepareVncViewer(final Host host, final int remotePort) {
        if (remotePort < 0 || host == null) {
            return -1;
        }
        if (Tools.isLocalIp(host.getIpAddress())) {
            return remotePort;
        }
        final int localPort = remotePort + getVncPortOffset();
        LOG.debug("prepareVncViewer: start port forwarding " + remotePort + " -> " + localPort);
        try {
            host.getSSH().startVncPortForwarding(host.getIpAddress(), remotePort);
        } catch (final IOException e) {
            LOG.error("prepareVncViewer: unable to create the tunnel "
                    + remotePort + " -> " + localPort
                    + ": " + e.getMessage()
                    + "\ntry the --vnc-port-offset option");
            return -1;
        }
        return localPort;
    }

    /** Cleans up after vnc viewer. It stops ssh tunnel. */
    private void cleanupVncViewer(final Host host, final int localPort) {
        if (Tools.isLocalIp(host.getIpAddress())) {
            return;
        }
        final int remotePort = localPort - getVncPortOffset();
        LOG.debug("cleanupVncViewer: stop port forwarding " + remotePort);
        try {
            host.getSSH().stopVncPortForwarding(remotePort);
        } catch (final IOException e) {
            LOG.appError("cleanupVncViewer: unable to close tunnel", e);
        }
    }

    /** Starts Tight VNC viewer. */
    public void startTightVncViewer(final Host host, final int remotePort) {
        final int localPort = prepareVncViewer(host, remotePort);
        if (localPort < 0) {
            return;
        }
        final tightvnc.VncViewer v = new tightvnc.VncViewer(new String[]{"HOST",
                "127.0.0.1",
                "PORT",
                Integer.toString(localPort)},
                false,
                true);
        v.init();
        v.start();
        v.join();
        cleanupVncViewer(host, localPort);
    }

    /** Starts Ultra VNC viewer. */
    public void startUltraVncViewer(final Host host, final int remotePort) {
        final int localPort = prepareVncViewer(host, remotePort);
        if (localPort < 0) {
            return;
        }
        final JavaViewer.VncViewer v = new JavaViewer.VncViewer(new String[]{"HOST",
                                                                             "127.0.0.1",
                                                                             "PORT",
                                                                             Integer.toString(localPort)},
                                                                             false,
                                                                             true);

        v.init();
        v.start();
        v.join();
        cleanupVncViewer(host, localPort);
    }

    /** Makes the buttons font smaller. */
    public void makeMiniButton(final AbstractButton ab) {
        final Font font = ab.getFont();
        final String name = font.getFontName();
        final int style = font.getStyle();
        ab.setFont(new Font(name, style, scaled(10)));
        ab.setMargin(new Insets(2, 2, 2, 2));
        ab.setIconTextGap(0);
    }

    /**
     * Resize all fonts. Must be called before GUI is started.
     * @param scale in percent 100% - is the same size.
     */
    public void resizeFonts(int scale) {
        if (scale == 100) {
            return;
        }
        if (scale < 5) {
            scale = 5;
        }
        if (scale > 10000) {
            scale = 10000;
        }
        for (final Enumeration<Object> e = UIManager.getDefaults().keys(); e.hasMoreElements();) {
            final Object key = e.nextElement();
            final Object value = UIManager.get(key);
            if (value instanceof Font)    {
                final Font f = (Font) value;
                UIManager.put(key, new FontUIResource(f.getName(), f.getStyle(), scaled(f.getSize())));
            }
        }
    }

    public void setMaxAccessType(final Application.AccessType maxAccessType) {
        this.maxAccessType = maxAccessType;
        setAccessType(maxAccessType);
        checkAccessOfEverything();
    }

    public void checkAccessOfEverything() {
        for (final Cluster c : allClusters.getClusterSet()) {
            final ClusterBrowser cb = c.getBrowser();
            if (cb != null) {
                cb.checkAccessOfEverything();
            }
        }
    }

    /**
     * Print stack trace if it's not in a swing thread.
     */
    public void isSwingThread() {
        if (!isCheckSwing()) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            System.out.println("not a swing thread: " + Tools.getStackTrace());
        }
    }

    /**
     * Print stack trace if it's in a swing thread.
     */
    public void isNotSwingThread() {
        if (!isCheckSwing()) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            System.out.println("swing thread: " + Tools.getStackTrace());
        }
    }

    /** Wait for next swing threads to finish. It's used for synchronization */
    public void waitForSwing() {
        invokeAndWait(new Runnable() {
            @Override
            public void run() {
                /* just wait */
            }
        });
    }

    /**
     * Convenience invoke and wait function if not already in an event
     * dispatch thread.
     */
    public void invokeAndWaitIfNeeded(final Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            invokeAndWait(runnable);
        }
    }

    public void invokeAndWait(final Runnable runnable) {
        isNotSwingThread();
        try {
            SwingUtilities.invokeAndWait(runnable);
        } catch (final InterruptedException ix) {
            Thread.currentThread().interrupt();
        } catch (final InvocationTargetException x) {
            LOG.appError("invokeAndWait: exception", x);
        }
    }

    public void invokeLater(final Runnable runnable) {
        invokeLater(CHECK_SWING_THREAD, runnable);
    }

    public void invokeLater(final boolean checkSwingThread, final Runnable runnable) {
        if (checkSwingThread) {
            isNotSwingThread();
        }
        SwingUtilities.invokeLater(runnable);
    }

    public int getServiceLabelWidth() {
        return getDefaultSize("ClusterBrowser.ServiceLabelWidth");
    }
    public int getServiceFieldWidth() {
        return getDefaultSize("ClusterBrowser.ServiceFieldWidth");
    }

    /**
     * Shows confirm dialog with yes and no options and returns true if yes
     * button was pressed.
     */
    public boolean confirmDialog(final String title, final String desc, final String yesButton, final String noButton) {
        final ConfirmDialog confirmDialog = confirmDialogProvider.get();
        confirmDialog.init(title, desc, yesButton, noButton);
        confirmDialog.showDialog();
        return confirmDialog.isPressedYesButton();
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
