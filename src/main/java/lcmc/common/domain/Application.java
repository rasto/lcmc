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

package lcmc.common.domain;

import java.awt.Font;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.trilead.ssh2.KnownHosts;

import lcmc.cluster.domain.Cluster;
import lcmc.cluster.domain.Clusters;
import lcmc.common.ui.ConfirmDialog;
import lcmc.host.domain.Host;
import lcmc.host.domain.Hosts;
import lcmc.robotest.Test;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.domain.util.Tools;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.AbstractButton;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * Application
 *
 * Holds data, that are used globaly in the application and provides some
 * functions for this data.
 */
@Named
@Singleton
public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    public static final String HEARTBEAT_NAME = "Heartbeat";
    public static final String COROSYNC_NAME = "Corosync/OpenAIS";
    public static final String PM_CLONE_SET_NAME = "Clone Set";
    public static final String PM_MASTER_SLAVE_SET_NAME = "Master/Slave Set";
    public static final String PACEMAKER_GROUP_NAME = "Group";

    public static boolean isLive(final RunMode runMode) {
        return RunMode.LIVE == runMode;
    }

    public static boolean isTest(final RunMode runMode) {
        return RunMode.TEST == runMode;
    }
    private Set<String> skipNetInterfaces = Sets.newHashSet();
    private String downloadUser = Tools.getDefault("DownloadLogin.User");
    private String downloadPassword = Tools.getDefault("DownloadLogin.Password");
    private String savedDownloadUser = "";
    private String savedDownloadPassword = "";
    private boolean loginSave = true;

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
    private final Table<String, String, String> autoOptions = HashBasedTable.create();
    private int vncPortOffset = 0;
    private boolean useTightvnc = false;
    private boolean useUltravnc = false;
    private boolean useRealvnc = false;
    private boolean stagingDrbd = false;
    private boolean stagingPacemaker = false;
    private boolean hideLRM = false;
    private boolean upgradeCheckEnabled = true;
    private boolean bigDRBDConf = false;
    private boolean oneHostCluster = false;
    private int scale = 100;
    private boolean noPassphrase = false;
    private boolean embedApplet = Tools.isLinux();
    private boolean cmdLog = false;
    private Test autoTest = null;

    @Inject
    private Hosts allHosts;
    @Inject
    private Clusters allClusters;
    @Inject
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

    /**
     * Returns default value for integer option from AppDefaults resource
     * bundle and scales it according the --scale option.
     */
    public int getDefaultSize(final String option) {
        return scaled(Tools.getDefaultInt(option));
    }

    public void addSkipNetInterface(String skipNetInterface) {
        this.skipNetInterfaces.add(skipNetInterface);
    }

    public boolean isSkipNetInterface(String name) {
        return skipNetInterfaces.contains(name);
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


    /**
     * Run mode. TEST does shows changes in the GUI, but does not change the
     * cluster
     */
    public enum RunMode {
        LIVE,
        TEST
    }
}
