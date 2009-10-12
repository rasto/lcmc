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

import java.io.Serializable;
import java.io.IOException;
import java.io.File;
import drbd.utilities.Tools;
import com.trilead.ssh2.KnownHosts;
import org.apache.commons.collections.map.MultiKeyMap;
import java.util.List;
import java.util.ArrayList;

/**
 * ConfigData
 *
 * Holds data, that are used globaly in the application and provides some
 * functions for this data.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class ConfigData implements Serializable {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
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
    /** Whether it is an expert mode. */
    private boolean expertMode = true;
    /** Default save file. */
    private String saveFile = Tools.getDefault("MainMenu.DrbdGuiFiles.Default");
    /** Known hosts object. */
    private final KnownHosts knownHosts = new KnownHosts();
    /** Known hosts path. */
    private final String knownHostPath;
    /** Id dsa path. */
    private final String idDSAPath;
    /** Id rsa path. */
    private final String idRSAPath;
    /** Last installed clusterStack. */
    private String lastInstalledClusterStack = null;
    /** Last installed method either Openais or Heartbeat with pacemaker. */
    private String lastHbPmInstalledMethod = null;
    /** Last installed drbd method. */
    private String lastDrbdInstalledMethod = null;
    /** Whether drbd gui helper should be overwritten. */
    private boolean keepHelper = false;
    /** Hosts that have auto options. */
    private final List<String> autoHosts = new ArrayList<String>();
    /** Clusters that have auto options. */
    private final List<String> autoClusters = new ArrayList<String>();
    /** Auto options, that make automatic actions in the gui. */
    private final MultiKeyMap autoOptions = new MultiKeyMap();
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
    

    /**
     * Prepares a new <code>ConfigData</code> object and creates new hosts
     * and clusters objects.
     */
    public ConfigData() {
        hosts = new Hosts();
        clusters = new Clusters();
        final String pwd = System.getProperty("user.home");
        knownHostPath = pwd + "/.ssh/known_hosts";
        idDSAPath     = pwd + "/.ssh/id_dsa";
        idRSAPath     = pwd + "/.ssh/id_rsa";

        final File knownHostFile = new File(knownHostPath);
        if (knownHostFile.exists()) {
            try {
                knownHosts.addHostkeys(knownHostFile);
            } catch (IOException e) {
                Tools.appError("SSH.knowHostFile.NotExists", "", e);
            }
        }
    }

    /**
     * Gets hosts object.
     */
    public final Hosts getHosts() {
        return hosts;
    }

    /**
     * Returns number of hosts that are not part of any cluster.
     */
    public final int danglingHostsCount() {
        final Hosts hosts = Tools.getConfigData().getHosts();
        int c = 0;
        for (final Host host : hosts.getHostSet()) {
            if (!host.isInCluster()) {
                c++;
            }
        }
        return c;
    }

    /**
     * Gets clusters object.
     */
    public final Clusters getClusters() {
        return clusters;
    }

    /**
     * Gets user for download area.
     */
    public final String getDownloadUser() {
        if (savedDownloadUser != null && !savedDownloadUser.equals("")) {
            downloadUser = savedDownloadUser;
            savedDownloadUser = "";
        }
        return downloadUser;
    }

    /**
     * Gets password for download area.
     */
    public final String getDownloadPassword() {
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
    public final boolean getLoginSave() {
        return loginSave;
    }

    /**
     * Sets user and password for download area.
     */
    public final void setDownloadLogin(final String downloadUser,
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

    /**
     * Return whether host exists in the hosts.
     */
    public final boolean existsHost(final Host host) {
        return hosts.existsHost(host);
    }

    /**
     * Adds host object to the hosts object.
     */
    public final void addHostToHosts(final Host host) {
        hosts.addHost(host);
    }

    /**
     * removes host object from hosts object.
     */
    public final void removeHostFromHosts(final Host host) {
        hosts.removeHost(host);
    }

    /**
     * Return whether cluster exists in the clusters.
     */
    public final boolean existsCluster(final Cluster cluster) {
        return clusters.existsCluster(cluster);
    }

    /**
     * Adds cluster object to the clusters object.
     */
    public final void addClusterToClusters(final Cluster cluster) {
        clusters.addCluster(cluster);
    }

    /**
     * Removes cluster object from clusters object.
     */
    public final void removeClusterFromClusters(final Cluster cluster) {
        clusters.removeCluster(cluster);
    }

    /**
     * Disconnects all hosts.
     */
    public final void disconnectAllHosts() {
        hosts.disconnectAllHosts();
    }

    /**
     * Sets global expert mode.
     */
    public final void setExpertMode(final boolean expertMode) {
        this.expertMode = expertMode;
    }

    /**
     * Gets expert mode.
     */
    public final boolean getExpertMode() {
        return expertMode;
    }

    /**
     * Sets file name where gui data are saved.
     */
    public final void setSaveFile(final String saveFile) {
        this.saveFile = saveFile;
    }

    /**
     * Returns file name where gui data were saved the last time.
     */
    public final String getSaveFile() {
        return saveFile;
    }

    /**
     * Returns path of the known host file.
     */
    public final String getKnownHostPath() {
        return knownHostPath;
    }

    /**
     * Returns Id DSA path.
     */
    public final String getIdDSAPath() {
        return idDSAPath;
    }

    /**
     * Returns Id RSA path.
     */
    public final String getIdRSAPath() {
        return idRSAPath;
    }

    /**
     * Returns the known hosts object.
     */
    public final KnownHosts getKnownHosts() {
        return knownHosts;
    }

    /**
     * Sets what was the last installed cluster stack.
     */
    public final void setLastInstalledClusterStack(
                                       final String lastInstalledClusterStack) {
        this.lastInstalledClusterStack = lastInstalledClusterStack;
    }

    /**
     * Returns what was the last installed cluster stack.
     */
     public final String getLastInstalledClusterStack() {
         return lastInstalledClusterStack;
     }

    /**
     * Sets last installed method of either Openais or Heartbeat.
     */
    public final void setLastHbPmInstalledMethod(
                                    final String lastHbPmInstalledMethod) {
        this.lastHbPmInstalledMethod = lastHbPmInstalledMethod;
    }

    /**
     * Returns last installed method of either Openais or Heartbeat.
     */
     public final String getLastHbPmInstalledMethod() {
         return lastHbPmInstalledMethod;
     }

    /**
     * Sets last drbd installed method.
     */
    public final void setLastDrbdInstalledMethod(
                                    final String lastDrbdInstalledMethod) {
        this.lastDrbdInstalledMethod = lastDrbdInstalledMethod;
    }

    /**
     * Returns last drbd installed method.
     */
     public final String getLastDrbdInstalledMethod() {
         return lastDrbdInstalledMethod;
     }

     /**
      * Sets whether the drbd gui helper should be kept or overwritten all
      * the time.
      */
      public final void setKeepHelper(final boolean keepHelper) {
          this.keepHelper = keepHelper;
      }

     /**
      * Returns whether the drbd gui helper should be kept or overwritten
      * all the time.
      */
      public final boolean getKeepHelper() {
          return keepHelper;
      }

      /**
       * Adds auto option that starts automatic actions in the gui.
       */
      public final void addAutoOption(final String hostOrCluster,
                                      final String option,
                                      final String value) {
          autoOptions.put(hostOrCluster, option, value);
      }

      /**
       * Adds host on which automatic actions will be performed.
       */
      public final void addAutoHost(final String host) {
          autoHosts.add(host);
      }

      /**
       * Returns hosts on which automatic actions will be performed.
       */
      public final List<String> getAutoHosts() {
          return autoHosts;
      }

      /**
       * Removes host after it is done.
       */
      public final void removeAutoHost() {
          if (!autoHosts.isEmpty()) {
              autoHosts.remove(0);
          }
      }

      /**
       * Adds cluster on which automatic actions will be performed.
       */
      public final void addAutoCluster(final String cluster) {
          autoClusters.add(cluster);
      }

      /**
       * Returns clusters on which automatic actions will be performed.
       */
      public final List<String> getAutoClusters() {
          return autoClusters;
      }

      /**
       * Removes cluster after it is done.
       */
      public final void removeAutoCluster() {
          if (!autoClusters.isEmpty()) {
              autoClusters.remove(0);
          }
      }

      /**
       * Returns an auto option for gui testing.
       */
      public final String getAutoOption(final String hostOrCluster,
                                        final String option) {
          return (String) autoOptions.get(hostOrCluster, option);
      }

      /**
       * Returns an auto option for the first host in the list.
       */
      public final String getAutoOptionHost(final String option) {
          if (autoHosts.isEmpty()) {
              return null;
          }
          return (String) autoOptions.get(autoHosts.get(0), option);
      }

      /**
       * Returns an auto option for first cluster in the list.
       */
      public final String getAutoOptionCluster(final String option) {
          if (autoClusters.isEmpty()) {
              return null;
          }
          return (String) autoOptions.get(autoClusters.get(0), option);
      }
}
