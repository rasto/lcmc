/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009-2010, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2009-2010, Rasto Levrinc
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
package drbd.gui.resources;

import drbd.gui.Browser;
import drbd.gui.ClusterBrowser;
import drbd.data.ResourceAgent;
import drbd.data.Subtext;
import drbd.data.Host;
import drbd.data.ClusterStatus;
import drbd.data.CRMXML;
import drbd.data.ConfigData;
import drbd.utilities.CRM;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;
import drbd.utilities.MyMenu;

import java.util.Set;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.awt.Color;
import java.awt.geom.Point2D;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JMenuItem;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.SwingUtilities;

/**
 * This class holds clone service info object.
 */
public class CloneInfo extends ServiceInfo {
    /** Service that belongs to this clone. */
    private ServiceInfo containedService = null;
    /**
     * Creates new CloneInfo object.
     */
    public CloneInfo(final ResourceAgent ra,
                     final String name,
                     final boolean master,
                     final Browser browser) {
        super(name, ra, browser);
        getService().setMaster(master);
    }

    /**
     * Adds service to this clone set. Adds it in the submenu in
     * the menu tree and initializes it.
     */
    public final void addCloneServicePanel(final ServiceInfo newServiceInfo) {
        containedService = newServiceInfo;
        newServiceInfo.getService().setResourceClass(
                    newServiceInfo.getResourceAgent().getResourceClass());
        newServiceInfo.setCloneInfo(this);

        getBrowser().addToHeartbeatIdList(newServiceInfo);
        getBrowser().addNameToServiceInfoHash(newServiceInfo);
        final DefaultMutableTreeNode newServiceNode =
                                new DefaultMutableTreeNode(newServiceInfo);
        newServiceInfo.setNode(newServiceNode);
        getBrowser().reload(getNode());
        getNode().add(newServiceNode);
        getBrowser().reload(newServiceNode);
    }

    /**
     * Adds service to this clone set it is called when the submenu
     * was already initialized but it was changed to be m/s set.
     */
    public final void setCloneServicePanel(final ServiceInfo newServiceInfo) {
        containedService = newServiceInfo;
        getBrowser().addNameToServiceInfoHash(this);
        getBrowser().addToHeartbeatIdList(this);
        newServiceInfo.setCloneInfo(this);
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(this);
        setNode(node);
        getBrowser().getServicesNode().add(node);
        node.add(newServiceInfo.getNode());
        getBrowser().reload(node);
    }

    /**
     * Returns info panel.
     */
    public final JComponent getInfoPanel() {
        final ServiceInfo cs = containedService;
        if (cs != null) {
            return cs.getInfoPanel();
        } else {
            return new JPanel();
        }
    }

    /**
     * Returns whether the resource has failed to start.
     */
    public final boolean isFailed(final boolean testOnly) {
        final ServiceInfo ci = containedService;
        if (ci != null) {
            return ci.isFailed(testOnly);
        }
        return false;
    }
    /**
     * Returns fail count.
     */
    protected final String getFailCount(final String hostName,
                                        final boolean testOnly) {
        final ServiceInfo ci = containedService;
        if (ci != null) {
            return ci.getFailCount(hostName, testOnly);
        }
        return "";
    }

    /**
     * Returns the main text that appears in the graph.
     */
    public final String getMainTextForGraph() {
        final ServiceInfo cs = containedService;
        if (cs == null) {
            return super.getMainTextForGraph();
        } else {
            return cs.getMainTextForGraph();
        }
    }

    /**
     * Returns node name of the host where this service is slave.
     */
    public final List<String> getSlaveOnNodes(final boolean testOnly) {
        final ServiceInfo cs = containedService;
        if (cs != null) {
            final ClusterStatus clStatus = getBrowser().getClusterStatus();
            return clStatus.getSlaveOnNodes(cs.getHeartbeatId(testOnly),
                                                 testOnly);
        }
        return null;
    }


    /**
     * Returns node name of the host where this cloned service is running.
     */
    public final List<String> getRunningOnNodes(final boolean testOnly) {
        final ServiceInfo cs = containedService;
        if (cs != null) {
            final ClusterStatus clStatus = getBrowser().getClusterStatus();
            if (getService().isMaster()) {
                return cs.getMasterOnNodes(testOnly);
            } else {
                return cs.getRunningOnNodes(testOnly);
            }
        }
        return null;
    }

    /**
     * Returns color for the host vertex.
     */
    public final List<Color> getHostColors(final boolean testOnly) {
         List<String> nodes = getRunningOnNodes(testOnly);
         final List<String> slaves = getSlaveOnNodes(testOnly);
         int nodesCount = 0;
         if (nodes != null) {
             nodesCount = nodes.size();
         } else {
             nodes = new ArrayList<String>();
         }
         int slavesCount = 0;
         if (slaves != null) {
             slavesCount = slaves.size();
         }
         if (nodesCount + slavesCount < getBrowser().getClusterHosts().length) {
             final List<Color> colors = new ArrayList<Color>();
             colors.add(ClusterBrowser.FILL_PAINT_STOPPED);
             return colors;
         } else {
             return getBrowser().getCluster().getHostColors(nodes);
         }
    }

    /**
     * Returns fail count string that appears in the graph.
     */
    private String getFailCountString(final String hostName,
                                      final boolean testOnly) {
        String fcString = "";
        if (containedService != null) {
            final String failCount =
                         containedService.getFailCount(hostName, testOnly);
            if (failCount != null) {
                if (CRMXML.INFINITY_STRING.equals(failCount)) {
                    fcString = " failed";
                } else {
                    fcString = " failed: " + failCount;
                }
            }
        }
        return fcString;
    }

    /**
     * Returns text with lines as array that appears in the cluster graph.
     */
    public final Subtext[] getSubtextsForGraph(final boolean testOnly) {
        final List<Subtext> texts = new ArrayList<Subtext>();
        final Set<String> notRunningOnNodes = new LinkedHashSet<String>();
        for (final Host h : getBrowser().getClusterHosts()) {
            notRunningOnNodes.add(h.getName());
        }
        texts.add(new Subtext(toString(), null));
        final ServiceInfo cs = getContainedService();
        if (cs != null && cs.getResourceAgent().isGroup()) {
            final ClusterStatus clStatus = getBrowser().getClusterStatus();
            final List<String> resources = clStatus.getGroupResources(
                                                   cs.getHeartbeatId(testOnly),
                                                   testOnly);
            if (resources != null) {
                for (final String hbId : resources) {
                    final ServiceInfo si =
                                   getBrowser().getServiceInfoFromCRMId(hbId);
                    texts.add(new Subtext("   " + si.toString(), null));
                }
            }
        }
        if (getBrowser().allHostsDown()) {
            return texts.toArray(new Subtext[texts.size()]);
        }
        final Host dcHost = getBrowser().getDCHost();
        final List<String> runningOnNodes = getRunningOnNodes(testOnly);
        if (runningOnNodes != null && !runningOnNodes.isEmpty()) {
            if (containedService != null
                && containedService.getResourceAgent().isLinbitDrbd()) {
                texts.add(new Subtext("primary on:", null));
            } else if (getService().isMaster()) {
                texts.add(new Subtext("master on:", null));
            } else {
                texts.add(new Subtext("running on:", null));
            }
            final List<Color> colors =
                    getBrowser().getCluster().getHostColors(runningOnNodes);
            int i = 0;
            for (final String n : runningOnNodes) {
                texts.add(new Subtext(ClusterBrowser.IDENT_4 + n
                                      + getFailCountString(n, testOnly),
                                      colors.get(i)));
                notRunningOnNodes.remove(n);
                i++;
            }
        }
        if (getService().isMaster()) {
            final List<String> slaveOnNodes = getSlaveOnNodes(testOnly);
            if (slaveOnNodes != null && !slaveOnNodes.isEmpty()) {
                final List<Color> colors =
                        getBrowser().getCluster().getHostColors(slaveOnNodes);
                int i = 0;
                if (containedService != null
                    && containedService.getResourceAgent().isLinbitDrbd()) {
                    texts.add(new Subtext("secondary on:", null));
                } else {
                    texts.add(new Subtext("slave on:", null));
                }
                for (final String n : slaveOnNodes) {
                    texts.add(new Subtext(ClusterBrowser.IDENT_4 + n
                                          + getFailCountString(n, testOnly),
                                          colors.get(i)));
                    notRunningOnNodes.remove(n);
                    i++;
                }
            }
        }
        if (!notRunningOnNodes.isEmpty()) {
            final Color nColor = ClusterBrowser.FILL_PAINT_STOPPED;
            if (isStopped(testOnly)) {
                texts.add(new Subtext("stopped", nColor));
            } else {
                texts.add(new Subtext("not running on:", nColor));
                for (final String n : notRunningOnNodes) {
                    Color color = nColor;
                    if (failedOnHost(n, testOnly)) {
                        color = null;
                    }
                    texts.add(new Subtext(ClusterBrowser.IDENT_4
                                          + n
                                          + getFailCountString(n, testOnly),
                                          color));
                }
            }
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /**
     * Returns service that belongs to this clone.
     */
    public final ServiceInfo getContainedService() {
        return containedService;
    }

    /**
     * Remove contained service and from there this clone service
     * will be removed.
     */
    public final void removeMyself(final boolean testOnly) {
        if (getService().isNew()) {
            removeMyselfNoConfirm(getBrowser().getDCHost(), testOnly);
            getService().setNew(false);
            getService().doneRemoving();
            return;
        }
        containedService.removeMyself(testOnly);
    }

    /**
     * In clone resource check its conaining service.
     */
    public final boolean checkResourceFields(final String param,
                                             final String[] params) {
        final boolean ccor = containedService.checkResourceFieldsCorrect(
                                  param,
                                  containedService.getParametersFromXML());
        final boolean cchanged = containedService.checkResourceFieldsChanged(
                                  param,
                                  containedService.getParametersFromXML());
        final boolean changed = checkResourceFieldsChanged(
                                                      param,
                                                      getParametersFromXML());
        return ccor && (cchanged || changed);
    }

    /**
     * Returns whether service is started.
     */
    public final boolean isStarted(final boolean testOnly) {
        final Host dcHost = getBrowser().getDCHost();
        final String hbV = dcHost.getHeartbeatVersion();
        final String pmV = dcHost.getPacemakerVersion();
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.1.4") <= 0) {
            return super.isStarted(testOnly);
        } else {
            final ServiceInfo cs = containedService;
            if (cs != null) {
                return cs.isStarted(testOnly);
            }
            return false;
        }
    }

    /**
     * Returns whether service is started.
     */
    public final boolean isStopped(final boolean testOnly) {
        final Host dcHost = getBrowser().getDCHost();
        final String hbV = dcHost.getHeartbeatVersion();
        final String pmV = dcHost.getPacemakerVersion();
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.1.4") <= 0) {
            return super.isStopped(testOnly);
        } else {
            final ServiceInfo cs = containedService;
            if (cs != null) {
                return cs.isStopped(testOnly);
            }
            return false;
        }
    }

    /**
     * Returns whether service is managed.
     */
    public final boolean isManaged(final boolean testOnly) {
        final Host dcHost = getBrowser().getDCHost();
        final String hbV = dcHost.getHeartbeatVersion();
        final String pmV = dcHost.getPacemakerVersion();
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.1.4") <= 0) {
            return super.isManaged(testOnly);
        } else {
            final ServiceInfo cs = containedService;
            if (cs != null) {
                return cs.isManaged(testOnly);
            }
            return false;
        }
    }

    /**
     * Cleans up the resource.
     */
    public final void cleanupResource(final Host dcHost,
                                      final boolean testOnly) {
        final ServiceInfo cs = containedService;
        if (cs != null) {
            final String hbV = dcHost.getHeartbeatVersion();
            final String pmV = dcHost.getPacemakerVersion();
            if (pmV == null
                && hbV != null
                && Tools.compareVersions(hbV, "2.1.4") <= 0) {
                for (int i = 0;
                     i < getBrowser().getClusterHosts().length; i++) {
                    CRM.cleanupResource(dcHost,
                                        cs.getHeartbeatId(testOnly)
                                        + ":" + Integer.toString(i),
                                        getBrowser().getClusterHosts(),
                                        testOnly);
                }
            } else {
                super.cleanupResource(dcHost, testOnly);
            }
        }
    }

    /**
     * Starts resource in crm.
     */
    public final void startResource(final Host dcHost, final boolean testOnly) {
        final String hbV = dcHost.getHeartbeatVersion();
        final String pmV = dcHost.getPacemakerVersion();
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.1.4") <= 0) {
            super.startResource(dcHost, testOnly);
        } else {
            final ServiceInfo cs = containedService;
            if (cs != null) {
                cs.startResource(dcHost, testOnly);
            }
        }
    }

    /**
     * Stops resource in crm.
     */
    public final void stopResource(final Host dcHost, final boolean testOnly) {
        final String hbV = dcHost.getHeartbeatVersion();
        final String pmV = dcHost.getPacemakerVersion();
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.1.4") <= 0) {
            super.stopResource(dcHost, testOnly);
        } else {
            final ServiceInfo cs = containedService;
            if (cs != null) {
                cs.stopResource(dcHost, testOnly);
            }
        }
    }

    /**
     * Migrates resource in heartbeat from current location.
     */
    public final void migrateResource(final String onHost,
                                      final Host dcHost,
                                      final boolean testOnly) {
        final ServiceInfo cs = containedService;
        if (cs != null) {
            cs.migrateResource(onHost, dcHost, testOnly);
        }
    }

    /**
     * Removes constraints created by resource migrate command.
     */
    public final void unmigrateResource(final Host dcHost,
                                        final boolean testOnly) {
        final ServiceInfo cs = containedService;
        if (cs != null) {
            cs.unmigrateResource(dcHost, testOnly);
        }
    }

    /**
     * Sets whether the service is managed.
     */
    public final void setManaged(final boolean isManaged,
                                 final Host dcHost,
                                 final boolean testOnly) {
        final String hbV = dcHost.getHeartbeatVersion();
        final String pmV = dcHost.getPacemakerVersion();
        if (pmV == null
            && hbV != null
            && Tools.compareVersions(hbV, "2.1.4") <= 0) {
            super.setManaged(isManaged, dcHost, testOnly);
        } else {
            final ServiceInfo cs = containedService;
            if (cs != null) {
                cs.setManaged(isManaged, dcHost, testOnly);
            }
        }
    }

    /**
     * Adds migrate and unmigrate menu items.
     */
    protected final void addMigrateMenuItems(final List<UpdatableItem> items) {
        /* no migrate / unmigrate menu items for clones. */
    }

    /**
     * Returns items for the clone popup.
     */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = super.createPopup();
        final ServiceInfo cs = containedService;
        if (cs == null) {
            return items;
        }
        final MyMenu csMenu = new MyMenu(cs.toString(),
                                         ConfigData.AccessType.RO,
                                         ConfigData.AccessType.RO) {
            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                return true;
            }

            public void update() {
                super.update();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        removeAll();
                    }
                });
                for (final UpdatableItem u : cs.createPopup()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            add((JMenuItem) u);
                            u.update();
                        }
                    });
                }
            }
        };
        items.add((UpdatableItem) csMenu);
        registerMenuItem((UpdatableItem) csMenu);
        return items;
    }

    /**
     * Returns whether info panel is already created.
     */
    public final boolean isInfoPanelOk() {
        final ServiceInfo cs = containedService;
        if (cs != null) {
            return cs.isInfoPanelOk();
        }
        return false;
    }

    /**
     * Update menus with positions and calles their update methods.
     */
    public final void updateMenus(final Point2D pos) {
        super.updateMenus(pos);
        final ServiceInfo cs = containedService;
        if (cs != null) {
            cs.updateMenus(pos);
        }
    }

    /**
     * Returns section to which the specified parameter belongs.
     */
    protected final String getSection(final String param) {
        final ServiceInfo cs = containedService;
        if (cs != null) {
            String name;
            if (getService().isMaster()) {
                name = MASTER_SLAVE_TYPE_STRING;
            } else {
                name = CLONE_TYPE_STRING;
            }
            return name + " " + super.getSection(param);
        }
        return super.getSection(param);
    }
}
