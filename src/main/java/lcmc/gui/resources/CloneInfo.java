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
package lcmc.gui.resources;

import lcmc.data.*;
import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.awt.Color;
import java.awt.geom.Point2D;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JMenuItem;
import javax.swing.tree.DefaultMutableTreeNode;

import lcmc.gui.widget.Check;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.CRM;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;


/**
 * This class holds clone service info object.
 */
final class CloneInfo extends ServiceInfo {
    /** Service that belongs to this clone. */
    private ServiceInfo containedService = null;
    /** Creates new CloneInfo object. */
    CloneInfo(final ResourceAgent ra,
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
    void addCloneServicePanel(final ServiceInfo newServiceInfo) {
        containedService = newServiceInfo;
        newServiceInfo.getService().setResourceClass(
                    newServiceInfo.getResourceAgent().getResourceClass());
        newServiceInfo.setCloneInfo(this);

        getBrowser().addToHeartbeatIdList(newServiceInfo);
        getBrowser().addNameToServiceInfoHash(newServiceInfo);
        final DefaultMutableTreeNode newServiceNode =
                                new DefaultMutableTreeNode(newServiceInfo);
        newServiceInfo.setNode(newServiceNode);
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                final DefaultMutableTreeNode node = getNode();
                if (node != null) {
                    getBrowser().reloadAndWait(node, false);
                    node.add(newServiceNode);
                }
                getBrowser().reloadAndWait(newServiceNode, true);
            }
        });
    }

    /**
     * Adds service to this clone set it is called when the submenu
     * was already initialized but it was changed to be m/s set.
     */
    void setCloneServicePanel(final ServiceInfo newServiceInfo) {
        containedService = newServiceInfo;
        newServiceInfo.setCloneInfo(this);
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(this);
        setNode(node);
        Tools.isSwingThread();
        getBrowser().getServicesNode().add(node);
        node.add(newServiceInfo.getNode());
        getBrowser().reloadAndWait(getBrowser().getServicesNode(), false);
        getBrowser().reloadAndWait(node, true);
    }

    /** Returns info panel. */
    @Override
    public JComponent getInfoPanel() {
        final ServiceInfo cs = containedService;
        final JComponent panel;
        if (cs == null) {
            panel = new JPanel();
        } else {
            panel = cs.getInfoPanel();
        }
        infoPanelDone();
        return panel;
    }

    /** Returns whether the resource has failed to start. */
    @Override
    public boolean isFailed(final Application.RunMode runMode) {
        final ServiceInfo ci = containedService;
        return ci != null && ci.isFailed(runMode);
    }

    /** Returns fail count. */
    @Override
    protected String getFailCount(final String hostName,
                                  final Application.RunMode runMode) {
        final ServiceInfo ci = containedService;
        if (ci != null) {
            return ci.getFailCount(hostName, runMode);
        }
        return "";
    }

    /** Returns the main text that appears in the graph. */
    @Override
    public String getMainTextForGraph() {
        final ServiceInfo cs = containedService;
        if (cs == null) {
            return super.getMainTextForGraph();
        } else {
            return cs.getMainTextForGraph();
        }
    }

    /** Returns name of this resource, that is used in logs. */
    @Override
    String getNameForLog() {
        final ServiceInfo cs = containedService;
        if (cs == null) {
            return super.getName();
        } else {
            return cs.getName();
        }
    }

    /** Returns node name of the host where this service is slave. */
    List<String> getSlaveOnNodes(final Application.RunMode runMode) {
        final ServiceInfo cs = containedService;
        if (cs == null) {
            return null;
        }
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        if (clStatus == null) {
            return null;
        }
        if (cs.getResourceAgent().isGroup()) {
            final List<String> resources = clStatus.getGroupResources(
                                                   cs.getHeartbeatId(runMode),
                                                   runMode);
            if (resources == null) {
                return null;
            }
            final Set<String> slaves = new TreeSet<String>();
            for (final String hbId : resources) {
                final List<String> slNodes =
                                      clStatus.getSlaveOnNodes(hbId, runMode);
                if (slNodes != null) {
                    slaves.addAll(slNodes);
                }
            }
            return new ArrayList<String>(slaves);
        } else {
            return clStatus.getSlaveOnNodes(cs.getHeartbeatId(runMode),
                                            runMode);
        }
    }


    /** Returns node name of the host where this cloned service is running. */
    @Override
    List<String> getRunningOnNodes(final Application.RunMode runMode) {
        final ServiceInfo cs = containedService;
        if (cs != null) {
            if (getService().isMaster()) {
                return cs.getMasterOnNodes(runMode);
            } else {
                return cs.getRunningOnNodes(runMode);
            }
        }
        return null;
    }

    /** Returns whether it is slave on all nodes. */
    @Override
    protected boolean isSlaveOnAllNodes(final Application.RunMode runMode) {
         final List<String> slaves = getSlaveOnNodes(runMode);
         return slaves != null
                && slaves.size() == getBrowser().getClusterHosts().length;
    }

    /** Returns color for the host vertex. */
    @Override
    public List<Color> getHostColors(final Application.RunMode runMode) {
         List<String> nodes = getRunningOnNodes(runMode);
         final List<String> slaves = getSlaveOnNodes(runMode);
         int nodesCount = 0;
         if (nodes == null) {
             nodes = new ArrayList<String>();
         } else {
             nodesCount = nodes.size();
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

    /** Returns fail count string that appears in the graph. */
    private String getFailCountString(final String hostName,
                                      final Application.RunMode runMode) {
        String fcString = "";
        final ServiceInfo cs = containedService;
        if (cs != null) {
            final String failCount = cs.getFailCount(hostName, runMode);
            if (failCount != null) {
                if (CRMXML.INFINITY_STRING.getValueForConfig().equals(failCount)) {
                    fcString = " failed";
                } else {
                    fcString = " failed: " + failCount;
                }
            }
        }
        return fcString;
    }

    /** Returns text with lines as array that appears in the cluster graph. */
    @Override
    public Subtext[] getSubtextsForGraph(final Application.RunMode runMode) {
        final List<Subtext> texts = new ArrayList<Subtext>();
        final Map<String, String> notRunningOnNodes =
                                        new LinkedHashMap<String, String>();
        for (final Host h : getBrowser().getClusterHosts()) {
            notRunningOnNodes.put(h.getName().toLowerCase(Locale.US),
                                  h.getName());
        }
        texts.add(new Subtext(toString(), null, Color.BLACK));
        final ServiceInfo cs = getContainedService();
        if (cs != null && cs.getResourceAgent().isGroup()) {
            final ClusterStatus clStatus = getBrowser().getClusterStatus();
            final List<String> resources = clStatus.getGroupResources(
                                                   cs.getHeartbeatId(runMode),
                                                   runMode);
            if (resources != null) {
                for (final String hbId : resources) {
                    final ServiceInfo si =
                                   getBrowser().getServiceInfoFromCRMId(hbId);
                    if (si == null) {
                        texts.add(new Subtext("   unknown",
                                              null,
                                              Color.BLACK));
                    } else {
                        texts.add(new Subtext("   " + si,
                                              null,
                                              Color.BLACK));
                    }
                }
            }
        }
        if (getBrowser().allHostsDown()) {
            return texts.toArray(new Subtext[texts.size()]);
        }
        final List<String> runningOnNodes = getRunningOnNodes(runMode);
        if (runningOnNodes != null && !runningOnNodes.isEmpty()) {
            if (cs != null && cs.getResourceAgent().isLinbitDrbd()) {
                texts.add(new Subtext("primary on:", null, Color.BLACK));
            } else if (getService().isMaster()) {
                texts.add(new Subtext("master on:", null, Color.BLACK));
            } else {
                texts.add(new Subtext("running on:", null, Color.BLACK));
            }
            final List<Color> colors =
                    getBrowser().getCluster().getHostColors(runningOnNodes);
            int i = 0;
            for (final String n : runningOnNodes) {
                final Color color;
                if (i < colors.size()) {
                    color = colors.get(i);
                } else {
                    color = Color.GRAY;
                }
                texts.add(new Subtext(ClusterBrowser.IDENT_4 + n
                                      + getPingCountString(n, runMode)
                                      + getFailCountString(n, runMode),
                                      color, Color.BLACK));
                notRunningOnNodes.remove(n.toLowerCase(Locale.US));
                i++;
            }
        }
        if (getService().isMaster()) {
            final List<String> slaveOnNodes = getSlaveOnNodes(runMode);
            if (slaveOnNodes != null && !slaveOnNodes.isEmpty()) {
                final List<Color> colors =
                        getBrowser().getCluster().getHostColors(slaveOnNodes);
                if (cs != null && cs.getResourceAgent().isLinbitDrbd()) {
                    texts.add(new Subtext("secondary on:", null, Color.BLACK));
                } else {
                    texts.add(new Subtext("slave on:", null, Color.BLACK));
                }
                int i = 0;
                for (final String n : slaveOnNodes) {
                    final Color color;
                    if (i < colors.size()) {
                        color = colors.get(i);
                    } else {
                        color = Color.GRAY;
                    }
                    texts.add(new Subtext(ClusterBrowser.IDENT_4 + n
                                          + getFailCountString(n, runMode),
                                          color,
                                          Color.BLACK));
                    notRunningOnNodes.remove(n.toLowerCase(Locale.US));
                    i++;
                }
            }
        }
        if (!notRunningOnNodes.isEmpty()) {
            final Color nColor = ClusterBrowser.FILL_PAINT_STOPPED;
            if (isStopped(runMode)) {
                texts.add(new Subtext("stopped", nColor, Color.BLACK));
            } else {
                texts.add(new Subtext("not running on:", nColor, Color.BLACK));
                for (final Map.Entry<String, String> notRunningEntry : notRunningOnNodes.entrySet()) {
                    final String hostName = notRunningEntry.getValue();
                    Color color = nColor;
                    if (failedOnHost(hostName, runMode)) {
                        color = null;
                    }
                    texts.add(new Subtext(ClusterBrowser.IDENT_4
                                          + hostName
                                          + getFailCountString(hostName,
                                                               runMode),
                                          color,
                                          Color.BLACK));
                }
            }
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /** Returns fail ping string that appears in the graph. */
    @Override
    protected String getPingCountString(final String hostName,
                                        final Application.RunMode runMode) {
        final ServiceInfo cs = getContainedService();
        if (cs != null) {
            return cs.getPingCountString(hostName, runMode);
        }
        return "";
    }

    /** Returns service that belongs to this clone. */
    @Override
    public ServiceInfo getContainedService() {
        return containedService;
    }

    /**
     * Remove contained service and from there this clone service
     * will be removed.
     */
    @Override
    public void removeMyself(final Application.RunMode runMode) {
        final ServiceInfo cs = containedService;
        if (getService().isNew()) {
            removeMyselfNoConfirm(getBrowser().getDCHost(), runMode);
            getService().setNew(false);
            if (cs != null) {
                cs.removeInfo();
            }
            removeInfo();
            getService().doneRemoving();
            return;
        }
        if (cs != null) {
            cs.removeMyself(runMode);
        }
        getBrowser().selectServices();
    }

    /** Removes the service without confirmation dialog. */
    @Override
    protected void removeMyselfNoConfirm(final Host dcHost,
                                         final Application.RunMode runMode) {
        super.removeMyselfNoConfirm(dcHost, runMode);
        setUpdated(false);
    }

    /** In clone resource check its containing service. */
    @Override
    public Check checkResourceFields(final String param,
                                     final String[] params) {
        return checkResourceFields(param, params, false);
    }

    /** In clone resource check its containing service. */
    Check checkResourceFields(final String param,
                              final String[] params,
                              final boolean fromServicesInfo) {
        final ServiceInfo cs = containedService;
        final List<String> incorrect = new ArrayList<String>();
        final List<String> changed = new ArrayList<String>();
        final Check check = new Check(incorrect, changed);
        check.addCheck(super.checkResourceFields(param,
                                                 params,
                                                 fromServicesInfo,
                                                 true,
                                                 false));
        if (cs == null) {
            incorrect.add("no service inside");
        } else {
            check.addCheck(cs.checkResourceFields(param,
                                                  cs.getParametersFromXML(),
                                                  fromServicesInfo,
                                                  true,
                                                  false));
        }
        return check;
    }

    /** Returns whether service is started. */
    @Override
    boolean isStarted(final Application.RunMode runMode) {
        final Host dcHost = getBrowser().getDCHost();
        if (Tools.versionBeforePacemaker(dcHost)) {
            return super.isStarted(runMode);
        } else {
            final ServiceInfo cs = containedService;
            return cs != null && cs.isStarted(runMode) && super.isStarted(runMode);
        }
    }

    /** Returns whether the service was set to be in slave role. */
    @Override
    public boolean isEnslaved(final Application.RunMode runMode) {
        final Host dcHost = getBrowser().getDCHost();
        if (Tools.versionBeforePacemaker(dcHost)) {
            return super.isEnslaved(runMode);
        } else {
            final ServiceInfo cs = containedService;
            return cs != null && (cs.isEnslaved(runMode) || super.isEnslaved(runMode));
        }
    }

    /** Returns whether service is started. */
    @Override
    public boolean isStopped(final Application.RunMode runMode) {
        final Host dcHost = getBrowser().getDCHost();
        if (Tools.versionBeforePacemaker(dcHost)) {
            return super.isStopped(runMode);
        } else {
            final ServiceInfo cs = containedService;
            return cs != null && (cs.isStopped(runMode) || super.isStopped(runMode));
        }
    }

    /** Returns whether service is managed. */
    @Override
    public boolean isManaged(final Application.RunMode runMode) {
        final Host dcHost = getBrowser().getDCHost();
        if (Tools.versionBeforePacemaker(dcHost)) {
            return super.isManaged(runMode);
        } else {
            final ServiceInfo cs = containedService;
            return cs != null && cs.isManaged(runMode);
        }
    }

    /** Cleans up the resource. */
    @Override
    void cleanupResource(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        final ServiceInfo cs = containedService;
        if (cs != null) {
            if (Tools.versionBeforePacemaker(dcHost)) {
                for (int i = 0;
                     i < getBrowser().getClusterHosts().length; i++) {
                    CRM.cleanupResource(dcHost,
                                        cs.getHeartbeatId(runMode)
                                        + ':' + Integer.toString(i),
                                        getBrowser().getClusterHosts(),
                                        runMode);
                }
            } else {
                super.cleanupResource(dcHost, runMode);
            }
        }
    }

    /** Starts resource in crm. */
    @Override
    void startResource(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        if (Tools.versionBeforePacemaker(dcHost)) {
            super.startResource(dcHost, runMode);
        } else {
            final ServiceInfo cs = containedService;
            if (cs != null) {
                cs.startResource(dcHost, runMode);
            }
        }
    }

    /** Adds migrate and unmigrate menu items. */
    @Override
    protected void addMigrateMenuItems(final List<UpdatableItem> items) {
        super.addMigrateMenuItems(items);
        if (!getService().isMaster()) {
            return;
        }
        final Application.RunMode runMode = Application.RunMode.LIVE;
        for (final Host host : getBrowser().getClusterHosts()) {
            final String hostName = host.getName();
            final MyMenuItem migrateFromMenuItem =
               new MyMenuItem(Tools.getString(
                                   "ClusterBrowser.Hb.MigrateFromResource")
                                   + ' ' + hostName + " (stop)",
                              MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,

                              Tools.getString(
                                   "ClusterBrowser.Hb.MigrateFromResource")
                                   + ' ' + hostName + " (stop) (offline)",
                              MIGRATE_ICON,
                              ClusterBrowser.STARTING_PTEST_TOOLTIP,
                              new AccessMode(Application.AccessType.OP, false),
                              new AccessMode(Application.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean predicate() {
                        return host.isClStatus();
                    }

                    @Override
                    public boolean visiblePredicate() {
                        return !host.isClStatus()
                               || enablePredicate() == null;
                    }

                    @Override
                    public String enablePredicate() {
                        final List<String> runningOnNodes =
                                               getRunningOnNodes(runMode);
                        if (runningOnNodes == null
                            || runningOnNodes.size() < 1) {
                            return "must run";
                        }
                        boolean runningOnNode = false;
                        for (final String ron : runningOnNodes) {
                            if (hostName.toLowerCase(Locale.US).equals(
                                               ron.toLowerCase(Locale.US))) {
                                runningOnNode = true;
                                break;
                            }
                        }
                        if (!getBrowser().clStatusFailed()
                               && getService().isAvailable()
                               && runningOnNode
                               && host.isClStatus()) {
                            return null;
                        } else {
                            return ""; /* is not visible anyway */
                        }
                    }

                    @Override
                    public void action() {
                        hidePopup();
                        if (getService().isMaster()) {
                            /* without role=master */
                            superMigrateFromResource(getBrowser().getDCHost(),
                                                      hostName,
                                                      runMode);
                        } else {
                            migrateFromResource(getBrowser().getDCHost(),
                                                hostName,
                                                runMode);
                        }
                    }
                };
            final ButtonCallback migrateItemCallback =
               getBrowser().new ClMenuItemCallback(null) {
                @Override
                public void action(final Host dcHost) {
                    if (getService().isMaster()) {
                        /* without role=master */
                        superMigrateFromResource(dcHost,
                                                 hostName,
                                                 Application.RunMode.TEST);
                    } else {
                        migrateFromResource(dcHost,
                                            hostName,
                                            Application.RunMode.TEST);
                    }
                }
            };
            addMouseOverListener(migrateFromMenuItem, migrateItemCallback);
            items.add(migrateFromMenuItem);
        }
    }

    /** Stops resource in crm. */
    @Override
    void stopResource(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        if (Tools.versionBeforePacemaker(dcHost)) {
            super.stopResource(dcHost, runMode);
        } else {
            final ServiceInfo cs = containedService;
            if (cs != null) {
                cs.stopResource(dcHost, runMode);
            }
        }
    }

    /** Workaround to call method from super. */
    private void superMigrateFromResource(final Host dcHost,
                                          final String fromHost,
                                          final Application.RunMode runMode) {
        super.migrateFromResource(dcHost, fromHost, runMode);
    }
    /** Migrates resource in heartbeat from current location. */
    @Override
    void migrateFromResource(final Host dcHost,
                             final String fromHost,
                             final Application.RunMode runMode) {
        String role = null;
        if (getService().isMaster()) {
            role = "Master";
        }
        final HostLocation hostLoc =
                                new HostLocation(CRMXML.MINUS_INFINITY_STRING.getValueForConfig(),
                                                 "eq",
                                                 null,
                                                 role);
        final String action;
        if (getMigratedFrom(runMode) == null) {
            action = "migration";
        } else {
            action = "remigration";
        }
        CRM.setLocation(dcHost,
                        getHeartbeatId(runMode),
                        fromHost,
                        hostLoc,
                        action,
                        runMode);
    }

    /** Sets whether the service is managed. */
    @Override
    void setManaged(final boolean isManaged,
                    final Host dcHost,
                    final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            setUpdated(true);
        }
        if (Tools.versionBeforePacemaker(dcHost)) {
            super.setManaged(isManaged, dcHost, runMode);
        } else {
            final ServiceInfo cs = containedService;
            if (cs != null) {
                cs.setManaged(isManaged, dcHost, runMode);
            }
        }
    }

    /** Adds "migrate from" and "force migrate" menuitems to the submenu. */
    @Override
    protected void addMoreMigrateMenuItems(final MyMenu submenu) {
        /* no migrate / unmigrate menu advanced items for clones. */
    }

    /** Returns items for the clone popup. */
    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = super.createPopup();
        final ServiceInfo cs = containedService;
        if (cs == null) {
            return items;
        }
        final UpdatableItem csMenu = new MyMenu(
                                     cs.toString(),
                                     new AccessMode(Application.AccessType.RO,
                                                    false),
                                     new AccessMode(Application.AccessType.RO,
                                                    false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void updateAndWait() {
                Tools.isSwingThread();
                removeAll();
                final ServiceInfo cs0 = containedService;
                if (cs0 != null) {
                    for (final UpdatableItem u : cs0.createPopup()) {
                        add((JMenuItem) u);
                        u.updateAndWait();
                    }
                }
                super.updateAndWait();
            }
        };
        items.add(csMenu);
        return items;
    }

    /** Returns whether info panel is already created. */
    @Override
    boolean isInfoPanelOk() {
        final ServiceInfo cs = containedService;
        return cs != null && cs.isInfoPanelOk();
    }

    /** Update menus with positions and calles their update methods. */
    @Override
    public void updateMenus(final Point2D pos) {
        super.updateMenus(pos);
        final ServiceInfo cs = containedService;
        if (cs != null) {
            cs.updateMenus(pos);
        }
    }

    /** Returns section to which the specified parameter belongs. */
    @Override
    protected String getSection(final String param) {
        final ServiceInfo cs = containedService;
        if (cs != null) {
            final String name;
            if (getService().isMaster()) {
                name = MASTER_SLAVE_TYPE_STRING.getValueForConfig();
            } else {
                name = CLONE_TYPE_STRING.getValueForConfig();
            }
            return name + ' ' + super.getSection(param);
        }
        return super.getSection(param);
    }

    /** Returns possible choices for drop down lists. */
    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        if (isCheckBox(param)) {
            return crmXML.getCheckBoxChoices(getResourceAgent(), param);
        } else {
            return crmXML.getParamPossibleChoices(getResourceAgent(),
                                                  param,
                                                  getService().isMaster());
        }
    }

    /** Returns the icon for the category. */
    @Override
    public ImageIcon getCategoryIcon(final Application.RunMode runMode) {
        return getMenuIcon(runMode);
    }

    void setContainedService(final ServiceInfo containedService) {
        this.containedService = containedService;
    }
}
