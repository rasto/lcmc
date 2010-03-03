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
import drbd.gui.GuiComboBox;
import drbd.gui.HeartbeatGraph;
import drbd.gui.dialog.ClusterLogs;
import drbd.data.Host;
import drbd.data.ResourceAgent;
import drbd.data.ClusterStatus;
import drbd.data.resources.Resource;
import drbd.data.PtestData;
import drbd.data.CRMXML;
import drbd.data.ConfigData;
import drbd.utilities.Unit;
import drbd.utilities.UpdatableItem;
import drbd.utilities.CRM;
import drbd.utilities.Tools;
import drbd.utilities.ButtonCallback;
import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;
import drbd.utilities.MyList;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.Box;
import javax.swing.JScrollPane;
import javax.swing.DefaultListModel;

/**
 * This class holds info data for services view and global heartbeat
 * config.
 */
public class ServicesInfo extends EditableInfo {
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Extra options panel. */
    private final JPanel extraOptionsPanel = new JPanel();

    /**
     * Prepares a new <code>ServicesInfo</code> object.
     */
    public ServicesInfo(final String name, final Browser browser) {
        super(name, browser);
        setResource(new Resource(name));
        ((ClusterBrowser) browser).getHeartbeatGraph().setServicesInfo(this);
    }

    /**
     * Returns browser object of this info.
     */
    protected final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /**
     * Sets info panel.
     */
    public final void setInfoPanel(final JComponent infoPanel) {
        this.infoPanel = infoPanel;
    }

    /**
     * Returns icon for services menu item.
     */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        return null;
    }

    /**
     * Returns names of all global parameters.
     */
    public final String[] getParametersFromXML() {
        return getBrowser().getCRMXML().getGlobalParameters();
    }

    /**
     * Returns long description of the global parameter, that is used for
     * tool tips.
     */
    protected final String getParamLongDesc(final String param) {
        return getBrowser().getCRMXML().getGlobalParamLongDesc(param);
    }

    /**
     * Returns short description of the global parameter, that is used as
     * label.
     */
    protected final String getParamShortDesc(final String param) {
        return getBrowser().getCRMXML().getGlobalParamShortDesc(param);
    }

    /**
     * Returns default for this global parameter.
     */
    protected final String getParamDefault(final String param) {
        return getBrowser().getCRMXML().getGlobalParamDefault(param);
    }

    /**
     * Returns preferred value for this global parameter.
     */
    protected final String getParamPreferred(final String param) {
        return getBrowser().getCRMXML().getGlobalParamPreferred(param);
    }

    /**
     * Returns possible choices for pulldown menus if applicable.
     */
    protected final Object[] getParamPossibleChoices(final String param) {
        return getBrowser().getCRMXML().getGlobalParamPossibleChoices(param);
    }

    /**
     * Checks if the new value is correct for the parameter type and
     * constraints.
     */
    protected final boolean checkParam(final String param,
                                       final String newValue) {
        return getBrowser().getCRMXML().checkGlobalParam(param, newValue);
    }

    /**
     * Returns whether the global parameter is of the integer type.
     */
    protected final boolean isInteger(final String param) {
        return getBrowser().getCRMXML().isGlobalInteger(param);
    }

    /**
     * Returns whether the global parameter is of the time type.
     */
    protected final boolean isTimeType(final String param) {
        return getBrowser().getCRMXML().isGlobalTimeType(param);
    }

    /**
     * Returns whether the global parameter is required.
     */
    protected final boolean isRequired(final String param) {
        return getBrowser().getCRMXML().isGlobalRequired(param);
    }

    /**
     * Returns whether the global parameter is of boolean type and
     * requires a checkbox.
     */
    protected final boolean isCheckBox(final String param) {
        return getBrowser().getCRMXML().isGlobalBoolean(param);
    }

    /**
     * Returns type of the global parameter.
     */
    protected final String getParamType(final String param) {
        return getBrowser().getCRMXML().getGlobalParamType(param);
    }

    /**
     * Returns section to which the global parameter belongs.
     */
    protected final String getSection(final String param) {
        return getBrowser().getCRMXML().getGlobalSection(param);
    }

    /**
     * Applies changes that user has entered.
     */
    public final void apply(final Host dcHost, final boolean testOnly) {
        final String[] params = getParametersFromXML();
        if (!testOnly) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    applyButton.setEnabled(false);
                    applyButton.setToolTipText(null);
                }
            });
        }

        /* update heartbeat */
        final Map<String, String> args = new HashMap<String, String>();
        for (String param : params) {
            final String value = getComboBoxValue(param);
            if (value.equals(getParamDefault(param))) {
                continue;
            }

            if ("".equals(value)) {
                continue;
            }
            args.put(param, value);
        }
        CRM.setGlobalParameters(dcHost, args, testOnly);
        if (!testOnly) {
            storeComboBoxValues(params);
            checkResourceFields(null, params);
        }
    }

    /**
     * Sets heartbeat global parameters after they were obtained.
     */
    public final void setGlobalConfig() {
        final String[] params = getParametersFromXML();
        for (String param : params) {
            final String value =
                         getBrowser().getClusterStatus().getGlobalParam(param);
            final String oldValue = getParamSaved(param);
            if (value != null && !value.equals(oldValue)) {
                getResource().setValue(param, value);
                final GuiComboBox cb = paramComboBoxGet(param, null);
                if (cb != null) {
                    cb.setValue(value);
                }
            }
        }
        if (infoPanel == null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    getInfoPanel();
                }
            });
        }
    }

    /**
     * Check if this connection is filesystem with drbd ra and if so, set
     * it.
     */
    private void setFilesystemWithDrbd(final ServiceInfo siP,
                                       final ServiceInfo si) {
        DrbdResourceInfo dri;
        if (siP.getResourceAgent().isLinbitDrbd()) {
            /* linbit::drbd -> Filesystem */
            ((FilesystemInfo) si).setLinbitDrbdInfo((LinbitDrbdInfo) siP);
            dri = getBrowser().getDrbdResHash().get(
                                    ((LinbitDrbdInfo) siP).getResourceName());
        } else {
            /* drbddisk -> Filesystem */
            ((FilesystemInfo) si).setDrbddiskInfo((DrbddiskInfo) siP);
            dri = getBrowser().getDrbdResHash().get(
                                       ((DrbddiskInfo) siP).getResourceName());
        }
        if (dri != null) {
            dri.setUsedByCRM(true);
        }
    }

    /**
     * Sets clone info object.
     */
    private CloneInfo setCreateCloneInfo(final String cloneId,
                                         final boolean testOnly) {
        CloneInfo newCi = null;
        newCi = (CloneInfo) getBrowser().getServiceInfoFromCRMId(cloneId);
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        if (newCi == null) {
            final Point2D p = null;
            newCi =
               (CloneInfo) hg.getServicesInfo().addServicePanel(
                                        getBrowser().getCRMXML().getHbClone(),
                                        p,
                                        false,
                                        cloneId,
                                        null,
                                        testOnly);
            newCi.getService().setNew(false);
            getBrowser().addToHeartbeatIdList(newCi);
            final Map<String, String> resourceNode =
                                  clStatus.getParamValuePairs(
                                          newCi.getHeartbeatId(testOnly));
            newCi.setParameters(resourceNode);
        } else {
            final Map<String, String> resourceNode =
                                  clStatus.getParamValuePairs(
                                          newCi.getHeartbeatId(testOnly));
            newCi.setParameters(resourceNode);
            if (!testOnly) {
                newCi.setUpdated(false);
                hg.repaint();
            }
        }
        hg.setVertexIsPresent(newCi);
        return newCi;
    }
    /**
     * Sets group info object.
     */
    private GroupInfo setCreateGroupInfo(final String group,
                                         final CloneInfo newCi,
                                         final boolean testOnly) {
        GroupInfo newGi = null;
        newGi = (GroupInfo) getBrowser().getServiceInfoFromCRMId(group);
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        if (newGi == null) {
            final Point2D p = null;
            newGi =
              (GroupInfo) hg.getServicesInfo().addServicePanel(
                                     getBrowser().getCRMXML().getHbGroup(),
                                     p,
                                     false,
                                     group,
                                     newCi,
                                     testOnly);
            newGi.getService().setNew(false);
            final Map<String, String> resourceNode =
                                  clStatus.getParamValuePairs(
                                      newGi.getHeartbeatId(testOnly));
            newGi.setParameters(resourceNode);
            if (newCi != null) {
                newCi.addCloneServicePanel(newGi);
            }
        } else {
            final Map<String, String> resourceNode =
                                    clStatus.getParamValuePairs(
                                      newGi.getHeartbeatId(testOnly));
            newGi.setParameters(resourceNode);
            if (!testOnly) {
                newGi.setUpdated(false);
                hg.repaint();
            }
        }
        return newGi;
    }

    /**
     * Sets or create all resources.
     */
    private void setGroupResources(
                           final Set<String> allGroupsAndClones,
                           final String grpOrCloneId,
                           final GroupInfo newGi,
                           final CloneInfo newCi,
                           final List<ServiceInfo> groupServiceIsPresent,
                           final boolean testOnly) {
        final Map<ServiceInfo, Map<String, String>> setParametersHash =
                           new HashMap<ServiceInfo, Map<String, String>>();
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        if (newCi != null) {
            setParametersHash.put(
                            newCi,
                            clStatus.getParamValuePairs(grpOrCloneId));
        } else if (newGi != null) {
            setParametersHash.put(
                            newGi,
                            clStatus.getParamValuePairs(grpOrCloneId));
        }
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        for (String hbId : clStatus.getGroupResources(grpOrCloneId,
                                                           testOnly)) {
            if (allGroupsAndClones.contains(hbId)) {
                if (newGi != null) {
                    Tools.appWarning("group in group not implemented");
                    continue;
                }
                /* clone group */
                final GroupInfo gi = setCreateGroupInfo(hbId,
                                                        newCi,
                                                        testOnly);
                setGroupResources(allGroupsAndClones,
                                  hbId,
                                  gi,
                                  null,
                                  groupServiceIsPresent,
                                  testOnly);
                continue;
            }
            final ResourceAgent newRA = clStatus.getResourceType(hbId);
            if (newRA == null) {
                /* This is bad. There is a service but we do not have
                 * the heartbeat script of this service or the we look
                 * in the wrong places.
                 */
                Tools.appWarning(hbId + ": could not find resource agent");
                continue;
            }
            /* continue of creating/updating of the
             * service in the gui.
             */
            ServiceInfo newSi = getBrowser().getServiceInfoFromCRMId(hbId);
            final Map<String, String> resourceNode =
                                       clStatus.getParamValuePairs(hbId);
            if (newSi == null) {
                // TODO: get rid of the service name? (everywhere)
                final String serviceName = newRA.getName();
                if (newRA.isFilesystem()) {
                    newSi = new FilesystemInfo(serviceName,
                                               newRA,
                                               hbId,
                                               resourceNode,
                                               getBrowser());
                } else if (newRA.isLinbitDrbd()) {
                    newSi = new LinbitDrbdInfo(serviceName,
                                               newRA,
                                               hbId,
                                               resourceNode,
                                               getBrowser());
                } else if (newRA.isDrbddisk()) {
                    newSi = new DrbddiskInfo(serviceName,
                                             newRA,
                                             hbId,
                                             resourceNode,
                                             getBrowser());
                } else if (newRA.isIPaddr()) {
                    newSi = new IPaddrInfo(serviceName,
                                           newRA,
                                           hbId,
                                           resourceNode,
                                           getBrowser());
                } else if (newRA.isVirtualDomain()) {
                    newSi = new VirtualDomainInfo(serviceName,
                                                  newRA,
                                                  hbId,
                                                  resourceNode,
                                                  getBrowser());
                } else {
                    newSi = new ServiceInfo(serviceName,
                                            newRA,
                                            hbId,
                                            resourceNode,
                                            getBrowser());
                }
                newSi.getService().setHeartbeatId(hbId);
                getBrowser().addToHeartbeatIdList(newSi);
                final Point2D p = null;
                if (newGi != null) {
                    newGi.addGroupServicePanel(newSi, false);
                } else if (newCi != null) {
                    newCi.addCloneServicePanel(newSi);
                } else {
                    hg.getServicesInfo().addServicePanel(newSi,
                                                         p,
                                                         false,
                                                         false,
                                                         testOnly);
                }
            } else {
                setParametersHash.put(newSi, resourceNode);
            }
            newSi.getService().setNew(false);
            //newSi.getTypeRadioGroup().setEnabled(false);
            hg.setVertexIsPresent(newSi);
            if (newGi != null || newCi != null) {
                groupServiceIsPresent.add(newSi);
            }
        }

        for (final ServiceInfo newSi : setParametersHash.keySet()) {
            newSi.setParameters(setParametersHash.get(newSi));
            if (!testOnly) {
                newSi.setUpdated(false);
            }
        }
        hg.repaint();
    }

    /**
     * This functions goes through all services, constrains etc. in
     * clusterStatus and updates the internal structures and graph.
     */
    public final void setAllResources(final boolean testOnly) {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        final Set<String> allGroupsAndClones = clStatus.getAllGroups();
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        hg.clearVertexIsPresentList();
        final List<ServiceInfo> groupServiceIsPresent =
                                                new ArrayList<ServiceInfo>();
        groupServiceIsPresent.clear();
        for (final String groupOrClone : allGroupsAndClones) {
            CloneInfo newCi = null;
            GroupInfo newGi = null;
            if (clStatus.isClone(groupOrClone)) {
                /* clone */
                newCi = setCreateCloneInfo(groupOrClone, testOnly);
            } else if (!"none".equals(groupOrClone)) {
                /* group */
                final GroupInfo gi =
                         (GroupInfo) getBrowser().getServiceInfoFromCRMId(
                                                                 groupOrClone);
                if (gi != null && gi.getCloneInfo() != null) {
                    /* cloned group is already done */
                    groupServiceIsPresent.add(gi);
                    continue;
                }
                newGi = setCreateGroupInfo(groupOrClone, newCi, testOnly);
                hg.setVertexIsPresent(newGi);
            }
            setGroupResources(allGroupsAndClones,
                              groupOrClone,
                              newGi,
                              newCi,
                              groupServiceIsPresent,
                              testOnly);
        }
        hg.clearColocationList();
        final Map<String, List<String>> colocationMap =
                                            clStatus.getColocationMap();
        for (final String heartbeatIdP : colocationMap.keySet()) {
            final List<String> tos = colocationMap.get(heartbeatIdP);
            for (final String heartbeatId : tos) {
                final ServiceInfo si  = getBrowser().getServiceInfoFromCRMId(
                                                                  heartbeatId);
                final ServiceInfo siP = getBrowser().getServiceInfoFromCRMId(
                                                                 heartbeatIdP);
                hg.addColocation(siP, si);
            }
        }

        hg.clearOrderList();
        final Map<String, List<String>> orderMap = clStatus.getOrderMap();
        for (final String heartbeatIdP : orderMap.keySet()) {
            for (final String heartbeatId : orderMap.get(heartbeatIdP)) {
                final ServiceInfo si =
                        getBrowser().getServiceInfoFromCRMId(heartbeatId);
                if (si != null) { /* not yet complete */
                    final ServiceInfo siP =
                            getBrowser().getServiceInfoFromCRMId(heartbeatIdP);
                    if (siP != null && siP.getResourceAgent() != null) {
                        /* dangling orders and colocations */
                        if ((siP.getResourceAgent().isDrbddisk()
                             || siP.getResourceAgent().isLinbitDrbd())
                            && si.getName().equals("Filesystem")) {
                            final List<String> colIds =
                                           colocationMap.get(heartbeatIdP);
                            // TODO: race here
                            if (colIds != null) {
                                for (String colId : colIds) {
                                    if (colId != null
                                        && colId.equals(heartbeatId)) {
                                        setFilesystemWithDrbd(siP, si);
                                    }
                                }
                            }
                        }
                        hg.addOrder(siP, si);
                    }
                }
            }
        }

        final Enumeration e = getNode().children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode n =
                      (DefaultMutableTreeNode) e.nextElement();
            final ServiceInfo g = (ServiceInfo) n.getUserObject();
            if (g.getResourceAgent().isGroup()
                || g.getResourceAgent().isClone()) {
                final Enumeration ge = g.getNode().children();
                while (ge.hasMoreElements()) {
                    final DefaultMutableTreeNode gn =
                              (DefaultMutableTreeNode) ge.nextElement();
                    final ServiceInfo s = (ServiceInfo) gn.getUserObject();
                    if (!groupServiceIsPresent.contains(s)
                        && !s.getService().isNew()) {
                        /* remove the group service from the menu that does
                         * not exist anymore. */
                        s.removeInfo();
                    }
                }
            }
        }
        hg.killRemovedEdges();
        hg.killRemovedVertices();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                hg.scale();
            }
        });
    }

    /**
     * Clears the info panel cache, forcing it to reload.
     */
    public final boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /**
     * Returns type of the info text. text/plain or text/html.
     */
    protected final String getInfoType() {
        return Tools.MIME_TYPE_TEXT_HTML;
    }

    /**
     * Returns info for info panel, that hb status failed or null, in which
     * case the getInfoPanel() function will show.
     */
    public final String getInfo() {
        if (getBrowser().clStatusFailed()) {
            return Tools.getString("ClusterBrowser.ClStatusFailed");
        }
        return null;
    }

    /**
     * Returns editable info panel for global crm config.
     */
    public final JComponent getInfoPanel() {
        /* if don't have hb status we don't have all the info we need here.
         * TODO: OR we need to get hb status only once
         */
        if (getBrowser().clStatusFailed()) {
            return super.getInfoPanel();
        }
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        if (infoPanel != null) {
            hg.pickBackground();
            return infoPanel;
        }
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        if (getBrowser().getCRMXML() == null) {
            return newPanel;
        }
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;

            /**
             * Whether the whole thing should be enabled.
             */
            public final boolean isEnabled() {
                final Host dcHost = getBrowser().getDCHost();
                if (dcHost == null) {
                    return false;
                }
                final String pmV = dcHost.getPacemakerVersion();
                final String hbV = dcHost.getHeartbeatVersion();
                if (pmV == null
                    && hbV != null
                    && Tools.compareVersions(hbV, "2.1.4") <= 0) {
                    return false;
                }
                return true;
            }

            public final void mouseOut() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                hg.stopTestAnimation(applyButton);
                applyButton.setToolTipText(null);
            }

            public final void mouseOver() {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                applyButton.setToolTipText(
                                        ClusterBrowser.STARTING_PTEST_TOOLTIP);
                applyButton.setToolTipBackground(Tools.getDefaultColor(
                                    "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                hg.startTestAnimation(applyButton, startTestLatch);
                final Host dcHost = getBrowser().getDCHost();
                getBrowser().ptestLockAcquire();
                final ClusterStatus clStatus = getBrowser().getClusterStatus();
                clStatus.setPtestData(null);
                apply(dcHost, true);
                final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
                applyButton.setToolTipText(ptestData.getToolTip());
                clStatus.setPtestData(ptestData);
                getBrowser().ptestLockRelease();
                startTestLatch.countDown();
            }
        };
        initApplyButton(buttonCallback);
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        extraOptionsPanel.setBackground(ClusterBrowser.EXTRA_PANEL_BACKGROUND);
        extraOptionsPanel.setLayout(new BoxLayout(extraOptionsPanel,
                                                  BoxLayout.Y_AXIS));
        extraOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        final JMenu serviceCombo = getActionsMenu();
        mb.add(serviceCombo);
        buttonPanel.add(mb, BorderLayout.EAST);

        newPanel.add(buttonPanel);

        final String[] params = getParametersFromXML();
        addParams(optionsPanel,
                  extraOptionsPanel,
                  params,
                  Tools.getDefaultInt("ClusterBrowser.DrbdResLabelWidth"),
                  Tools.getDefaultInt("ClusterBrowser.DrbdResFieldWidth"));

        applyButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(
                        new Runnable() {
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(getBrowser().getDCHost(), false);
                                getBrowser().clStatusUnlock();
                            }
                        }
                    );
                    thread.start();
                }
            }
        );

        /* apply button */
        addApplyButton(buttonPanel);
        applyButton.setEnabled(checkResourceFields(null, params));
        /* expert mode */
        Tools.registerExpertPanel(extraOptionsPanel);

        mainPanel.add(optionsPanel);
        mainPanel.add(extraOptionsPanel);

        newPanel.add(new JScrollPane(mainPanel));
        newPanel.add(Box.createVerticalGlue());

        hg.pickBackground();
        infoPanel = newPanel;
        return infoPanel;
    }

    /**
     * Returns heartbeat graph.
     */
    public final JPanel getGraphicalView() {
        return getBrowser().getHeartbeatGraph().getGraphPanel();
    }

    /**
     * Adds service to the list of services.
     * TODO: are they both used?
     */
    public final ServiceInfo addServicePanel(final ResourceAgent newRA,
                                             final Point2D pos,
                                             final boolean reloadNode,
                                             final String heartbeatId,
                                             final CloneInfo newCi,
                                             final boolean testOnly) {
        ServiceInfo newServiceInfo;
        final String name = newRA.getName();
        if (newRA.isFilesystem()) {
            newServiceInfo = new FilesystemInfo(name, newRA, getBrowser());
        } else if (newRA.isLinbitDrbd()) {
            newServiceInfo = new LinbitDrbdInfo(name, newRA, getBrowser());
        } else if (newRA.isDrbddisk()) {
            newServiceInfo = new DrbddiskInfo(name, newRA, getBrowser());
        } else if (newRA.isIPaddr()) {
            newServiceInfo = new IPaddrInfo(name, newRA, getBrowser());
        } else if (newRA.isVirtualDomain()) {
            newServiceInfo = new VirtualDomainInfo(name, newRA, getBrowser());
        } else if (newRA.isGroup()) {
            newServiceInfo = new GroupInfo(newRA, getBrowser());
        } else if (newRA.isClone()) {
            final boolean master =
                         getBrowser().getClusterStatus().isMaster(heartbeatId);
            String cloneName;
            if (master) {
                cloneName = ConfigData.PM_MASTER_SLAVE_SET_NAME;
            } else {
                cloneName = ClusterBrowser.PM_CLONE_SET_NAME;
            }
            newServiceInfo = new CloneInfo(newRA,
                                           cloneName,
                                           master,
                                           getBrowser());
        } else {
            newServiceInfo = new ServiceInfo(name, newRA, getBrowser());
        }
        if (heartbeatId != null) {
            newServiceInfo.getService().setHeartbeatId(heartbeatId);
            getBrowser().addToHeartbeatIdList(newServiceInfo);
        }
        if (newCi == null) {
            addServicePanel(newServiceInfo,
                            pos,
                            reloadNode,
                            true,
                            testOnly);
        }
        return newServiceInfo;
    }

    /**
     * Adds new service to the specified position. If position is null, it
     * will be computed later. reloadNode specifies if the node in
     * the menu should be reloaded and get uptodate.
     */
    public final void addServicePanel(final ServiceInfo newServiceInfo,
                                      final Point2D pos,
                                      final boolean reloadNode,
                                      final boolean interactive,
                                      final boolean testOnly) {
        newServiceInfo.getService().setResourceClass(
                    newServiceInfo.getResourceAgent().getResourceClass());
        final HeartbeatGraph hg = getBrowser().getHeartbeatGraph();
        if (!hg.addResource(newServiceInfo, null, pos, testOnly)) {
            getBrowser().addNameToServiceInfoHash(newServiceInfo);
            final DefaultMutableTreeNode newServiceNode =
                                new DefaultMutableTreeNode(newServiceInfo);
            newServiceInfo.setNode(newServiceNode);
            getBrowser().getServicesNode().add(newServiceNode);
            if (interactive
                && newServiceInfo.getResourceAgent().isMasterSlave()) {
                /* only if it was added manually. */
                newServiceInfo.changeType(ServiceInfo.MASTER_SLAVE_TYPE_STRING);
            }
            if (reloadNode) {
                /* show it */
                getBrowser().reload(getBrowser().getServicesNode());
                getBrowser().reload(newServiceNode);
            }
            getBrowser().reloadAllComboBoxes(newServiceInfo);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    hg.scale();
                }
            });
        }
        hg.reloadServiceMenus();
    }

    /**
     * Returns 'add service' list for graph popup menu.
     */
    public final List<ResourceAgent> getAddServiceList(final String cl) {
        return getBrowser().globalGetAddServiceList(cl);
    }

    /**
     * Returns background popup. Click on background represents cluster as
     * whole.
     */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final boolean testOnly = false;
        /* add group */
        final MyMenuItem addGroupMenuItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.AddGroup"),
                           null,
                           null) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return !getBrowser().clStatusFailed();
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
                    final StringInfo gi = new StringInfo(
                                                ClusterBrowser.PM_GROUP_NAME,
                                                ClusterBrowser.PM_GROUP_NAME,
                                                getBrowser());
                    addServicePanel(getBrowser().getCRMXML().getHbGroup(),
                                    getPos(),
                                    true,
                                    null,
                                    null,
                                    testOnly);
                    getBrowser().getHeartbeatGraph().repaint();
                }
            };
        items.add((UpdatableItem) addGroupMenuItem);
        registerMenuItem((UpdatableItem) addGroupMenuItem);

        /* add service */
        final MyMenu addServiceMenuItem = new MyMenu(
                        Tools.getString("ClusterBrowser.Hb.AddService")) {
            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                return !getBrowser().clStatusFailed();
            }

            public void update() {
                super.update();
                removeAll();
                Point2D pos = getPos();
                final CRMXML crmXML = getBrowser().getCRMXML();
                final ResourceAgent fsService = crmXML.getResourceAgent(
                                        "Filesystem",
                                        ServiceInfo.HB_HEARTBEAT_PROVIDER,
                                        "ocf");
                if (crmXML.isLinbitDrbdPresent()) { /* just skip it,
                                                       if it is not */
                    final ResourceAgent linbitDrbdService =
                                                   crmXML.getHbLinbitDrbd();
                    final MyMenuItem ldMenuItem = new MyMenuItem(
                     Tools.getString("ClusterBrowser.linbitDrbdMenuName")) {
                        private static final long serialVersionUID = 1L;
                        public void action() {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    getPopup().setVisible(false);
                                }
                            });
                            if (!getBrowser().linbitDrbdConfirmDialog()) {
                                return;
                            }

                            final FilesystemInfo fsi = (FilesystemInfo)
                                                           addServicePanel(
                                                                fsService,
                                                                getPos(),
                                                                true,
                                                                null,
                                                                null,
                                                                testOnly);
                            fsi.setDrbddiskIsPreferred(false);
                            getBrowser().getHeartbeatGraph().repaint();
                        }
                    };
                    if (getBrowser().atLeastOneDrbddisk()
                        || !crmXML.isLinbitDrbdPresent()) {
                        ldMenuItem.setEnabled(false);
                    }
                    ldMenuItem.setPos(pos);
                    add(ldMenuItem);
                }
                if (crmXML.isDrbddiskPresent()) { /* just skip it,
                                                     if it is not */
                    final ResourceAgent drbddiskService =
                                                crmXML.getHbDrbddisk();
                    final MyMenuItem ddMenuItem = new MyMenuItem(
                     Tools.getString("ClusterBrowser.DrbddiskMenuName")) {
                        private static final long serialVersionUID = 1L;
                        public void action() {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    getPopup().setVisible(false);
                                }
                            });
                            final FilesystemInfo fsi = (FilesystemInfo)
                                                           addServicePanel(
                                                                fsService,
                                                                getPos(),
                                                                true,
                                                                null,
                                                                null,
                                                                testOnly);
                            fsi.setDrbddiskIsPreferred(true);
                            getBrowser().getHeartbeatGraph().repaint();
                        }
                    };
                    if (getBrowser().isOneLinbitDrbd()
                        || !crmXML.isDrbddiskPresent()) {
                        ddMenuItem.setEnabled(false);
                    }
                    ddMenuItem.setPos(pos);
                    add(ddMenuItem);
                }
                final ResourceAgent ipService = crmXML.getResourceAgent(
                                         "IPaddr2",
                                         ServiceInfo.HB_HEARTBEAT_PROVIDER,
                                         "ocf");
                if (ipService != null) { /* just skip it, if it is not*/
                    final MyMenuItem ipMenuItem =
                           new MyMenuItem(ipService.getMenuName()) {
                        private static final long serialVersionUID = 1L;
                        public void action() {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    getPopup().setVisible(false);
                                }
                            });
                            addServicePanel(ipService,
                                            getPos(),
                                            true,
                                            null,
                                            null,
                                            testOnly);
                            getBrowser().getHeartbeatGraph().repaint();
                        }
                    };
                    ipMenuItem.setPos(pos);
                    add(ipMenuItem);
                }
                for (final String cl : ClusterBrowser.HB_CLASSES) {
                    final MyMenu classItem =
                            new MyMenu(ClusterBrowser.HB_CLASS_MENU.get(cl));
                    DefaultListModel dlm = new DefaultListModel();
                    for (final ResourceAgent ra : getAddServiceList(cl)) {
                        final MyMenuItem mmi =
                                new MyMenuItem(ra.getMenuName()) {
                            private static final long serialVersionUID = 1L;
                            public void action() {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        getPopup().setVisible(false);
                                    }
                                });
                                if (ra.isLinbitDrbd()
                                    &&
                                     !getBrowser().linbitDrbdConfirmDialog()) {
                                    return;
                                } else if (ra.isHbDrbd()
                                    &&
                                     !getBrowser().hbDrbdConfirmDialog()) {
                                    return;
                                }
                                addServicePanel(ra,
                                                getPos(),
                                                true,
                                                null,
                                                null,
                                                testOnly);
                                getBrowser().getHeartbeatGraph().repaint();
                            }
                        };
                        mmi.setPos(pos);
                        dlm.addElement(mmi);
                    }
                    final JScrollPane jsp = Tools.getScrollingMenu(
                                               classItem,
                                               dlm,
                                               new MyList(dlm, getBackground()),
                                               null);
                    if (jsp == null) {
                        classItem.setEnabled(false);
                    } else {
                        classItem.add(jsp);
                    }
                    add(classItem);
                }
            }
        };
        items.add((UpdatableItem) addServiceMenuItem);
        registerMenuItem((UpdatableItem) addServiceMenuItem);
        /* remove all services. */
        final MyMenuItem removeMenuItem = new MyMenuItem(
                Tools.getString("ClusterBrowser.Hb.RemoveAllServices"),
                ClusterBrowser.REMOVE_ICON) {
            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                return !getBrowser().clStatusFailed()
                       && !getBrowser().getExistingServiceList(null).isEmpty();
            }

            public void action() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getPopup().setVisible(false);
                    }
                });
                if (Tools.confirmDialog(
                     Tools.getString(
                         "ClusterBrowser.confirmRemoveAllServices.Title"),
                     Tools.getString(
                     "ClusterBrowser.confirmRemoveAllServices.Description"),
                     Tools.getString(
                         "ClusterBrowser.confirmRemoveAllServices.Yes"),
                     Tools.getString(
                         "ClusterBrowser.confirmRemoveAllServices.No"))) {
                    final Host dcHost = getBrowser().getDCHost();
                    for (ServiceInfo si
                            : getBrowser().getExistingServiceList(null)) {
                        if (si.getGroupInfo() == null) {
                            si.removeMyselfNoConfirm(dcHost, false);
                        }
                    }
                    getBrowser().getHeartbeatGraph().repaint();
                }
            }
        };
        items.add((UpdatableItem) removeMenuItem);
        registerMenuItem((UpdatableItem) removeMenuItem);


        /* view logs */
        final MyMenuItem viewLogsItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.ViewLogs"),
                           null,
                           null) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return true;
                }

                public void action() {
                    ClusterLogs l = new ClusterLogs(getBrowser().getCluster());
                    l.showDialog();
                }
            };
        items.add((UpdatableItem) viewLogsItem);
        registerMenuItem((UpdatableItem) viewLogsItem);
        return items;
    }

    /**
     * Returns units.
     */
    protected final Unit[] getUnits() {
        return new Unit[]{
            new Unit("", "s", "Second", "Seconds"), /* default unit */
            new Unit("ms",  "ms", "Millisecond", "Milliseconds"),
            new Unit("us",  "us", "Microsecond", "Microseconds"),
            new Unit("s",   "s",  "Second",      "Seconds"),
            new Unit("min", "m",  "Minute",      "Minutes"),
            new Unit("h",   "h",  "Hour",        "Hours")
        };
    }

    /**
     * Removes this services info.
     * TODO: is not called yet
     */
    public final void removeMyself(final boolean testOnly) {
        super.removeMyself(testOnly);
        Tools.unregisterExpertPanel(extraOptionsPanel);
    }
}
