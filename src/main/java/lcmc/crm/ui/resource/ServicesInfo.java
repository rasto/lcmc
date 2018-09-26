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

package lcmc.crm.ui.resource;

import com.google.common.base.Optional;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.widget.Check;
import lcmc.cluster.ui.widget.Widget;
import lcmc.common.domain.*;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.common.ui.EditableInfo;
import lcmc.common.ui.Info;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.common.ui.treemenu.ClusterTreeMenu;
import lcmc.common.ui.utils.*;
import lcmc.crm.domain.ClusterStatus;
import lcmc.crm.domain.CrmXml;
import lcmc.crm.domain.PtestData;
import lcmc.crm.domain.ResourceAgent;
import lcmc.crm.service.CRM;
import lcmc.crm.ui.CrmGraph;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * This class holds info data for services view and global heartbeat
 * config.
 */
@RequiredArgsConstructor
public class ServicesInfo extends EditableInfo {

    private final ServicesMenu servicesMenu;
    private final ProgressIndicator progressIndicator;
    private final Application application;
    private final SwingUtils swingUtils;
    private final ClusterTreeMenu clusterTreeMenu;
    private final CrmServiceFactory crmServiceFactory;
    private final Dialogs dialogs;

    private static final Logger LOG = LoggerFactory.getLogger(ServicesInfo.class);
    static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(Tools.getDefault("ClustersPanel.ClusterIcon"));
    /** Cache for the info panel. */
    private JComponent infoPanel = null;

    public void einit(final String name, final Browser browser) {
        super.einit(Optional.of(new ResourceValue(name)), name, browser);
    }

    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    void setInfoPanel(final JComponent infoPanel) {
        this.infoPanel = infoPanel;
    }

    /** Returns names of all global parameters. */
    @Override
    public String[] getParametersFromXML() {
        final CrmXml crmXml = getBrowser().getCrmXml();
        if (crmXml == null) {
            return null;
        }
        return crmXml.getGlobalParameters();
    }

    /**
     * Returns long description of the global parameter, that is used for
     * tool tips.
     */
    @Override
    protected String getParamLongDesc(final String param) {
        return getBrowser().getCrmXml().getGlobalLongDesc(param);
    }

    /**
     * Returns short description of the global parameter, that is used as
     * label.
     */
    @Override
    protected String getParamShortDesc(final String param) {
        return getBrowser().getCrmXml().getGlobalShortDesc(param);
    }

    @Override
    protected Value getParamDefault(final String param) {
        return getBrowser().getCrmXml().getGlobalParamDefault(param);
    }

    @Override
    protected Value getParamPreferred(final String param) {
        return getBrowser().getCrmXml().getGlobalPreferredValue(param);
    }

    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        return getBrowser().getCrmXml().getGlobalComboBoxChoices(param);
    }

    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        return getBrowser().getCrmXml().checkGlobalParam(param, newValue);
    }

    @Override
    protected boolean isInteger(final String param) {
        return getBrowser().getCrmXml().isGlobalInteger(param);
    }
    @Override
    protected boolean isLabel(final String param) {
        return getBrowser().getCrmXml().isGlobalLabel(param);
    }

    @Override
    protected boolean isTimeType(final String param) {
        return getBrowser().getCrmXml().isGlobalTimeType(param);
    }

    @Override
    protected boolean isAdvanced(final String param) {
        return Tools.areEqual(getParamDefault(param), getParamSaved(param))
               && getBrowser().getCrmXml().isGlobalParamAdvanced(param);
    }

    @Override
    protected AccessMode.Type getAccessType(final String param) {
        return getBrowser().getCrmXml().getGlobalParamAccessType(param);
    }

    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    @Override
    protected AccessMode.Mode isEnabledOnlyInAdvancedMode(final String param) {
        return AccessMode.NORMAL;
    }

    @Override
    protected boolean isRequired(final String param) {
        return getBrowser().getCrmXml().isGlobalRequired(param);
    }

    /**
     * Returns whether the global parameter is of boolean type and
     * requires a checkbox.
     */
    @Override
    protected boolean isCheckBox(final String param) {
        return getBrowser().getCrmXml().isGlobalBoolean(param);
    }

    @Override
    protected String getParamType(final String param) {
        return getBrowser().getCrmXml().getGlobalType(param);
    }

    @Override
    protected String getSection(final String param) {
        return getBrowser().getCrmXml().getGlobalSectionForDisplay(param);
    }

    void apply(final Host dcHost, final Application.RunMode runMode) {
        LOG.debug1("apply: start: test: " + runMode);
        final String[] params = getParametersFromXML();
        if (Application.isLive(runMode)) {
            swingUtils.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                    getApplyButton().setToolTipText("");
                }
            });
        }
        getInfoPanel();
        waitForInfoPanel();

        /* update pacemaker */
        final Map<String, String> args = new HashMap<String, String>();
        for (final String param : params) {
            final Value value = getComboBoxValue(param);
            if (Tools.areEqual(value, getParamDefault(param))) {
                continue;
            }

            if (value == null || value.isNothingSelected()) {
                continue;
            }
            args.put(param, value.getValueForConfig());
        }
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        final String[] rdiParams = rdi.getParametersFromXML();
        final Map<String, String> rdiMetaArgs = new LinkedHashMap<String, String>();
        for (final String param : rdiParams) {
            final Value value = rdi.getComboBoxValue(param);
            if (Tools.areEqual(value, rdi.getParamDefault(param))) {
                continue;
            }
            if (value != null && !value.isNothingSelected()) {
                rdiMetaArgs.put(param, value.getValueForConfig());
            }
        }
        final String rscDefaultsId = getBrowser().getClusterStatus().getRscDefaultsId(runMode);
        CRM.setGlobalParameters(dcHost, args, rdiMetaArgs, rscDefaultsId, runMode);
        if (Application.isLive(runMode)) {
            storeComboBoxValues(params);
            rdi.storeComboBoxValues(rdiParams);
        }
        for (final ServiceInfo si : getBrowser().getExistingServiceList(null)) {
            final Check check = si.checkResourceFields(null, si.getParametersFromXML(), true, false, false);
            if (check.isCorrect() && check.isChanged()) {
                si.apply(dcHost, runMode);
            }
        }
        if (Application.isLive(runMode)) {
            setApplyButtons(null, params);
        }
        LOG.debug1("apply: end: test: " + runMode);
    }

    /** Sets heartbeat global parameters after they were obtained. */
    public void setGlobalConfig(final ClusterStatus clStatus) {
        final String[] params = getParametersFromXML();
        for (final String param : params) {
            final String valueS = clStatus.getGlobalParam(param);
            if (valueS == null) {
                continue;
            }
            final Value value = new StringValue(valueS);
            final Value oldValue = getParamSaved(param);
            if (!Tools.areEqual(value, oldValue)) {
                getResource().setValue(param, value);
                final Widget wi = getWidget(param, null);
                if (wi != null) {
                    wi.setValue(value);
                }
            }
        }
        if (infoPanel == null) {
            swingUtils.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getInfoPanel();
                }
            });
        }
    }

    /** Clears the info panel cache, forcing it to reload. */
    @Override
    public boolean selectAutomaticallyInTreeMenu() {
        return infoPanel == null;
    }

    /** Returns type of the info text. text/plain or text/html. */
    @Override
    protected String getInfoMimeType() {
        return MainData.MIME_TYPE_TEXT_HTML;
    }

    /**
     * Returns info for info panel, that hb status failed or null, in which
     * case the getInfoPanel() function will show.
     */
    @Override
    public String getInfo() {
        if (getBrowser().crmStatusFailed()) {
            return Tools.getString("ClusterBrowser.ClStatusFailed");
        }
        return null;
    }

    /** Creates rsc_defaults panel. */
    private void addRscDefaultsPanel(final JPanel optionsPanel, final int leftWidth, final int rightWidth) {
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        rdi.widgetClear();
        final String[] params = rdi.getParametersFromXML();
        rdi.addParams(optionsPanel, params, leftWidth, rightWidth, null);
    }

    /** Returns editable info panel for global crm config. */
    @Override
    public JComponent getInfoPanel() {
        /* if don't have hb status we don't have all the info we need here.
         * TODO: OR we need to get hb status only once
         */
        if (getBrowser().crmStatusFailed()) {
            return super.getInfoPanel();
        }
        final CrmGraph crmGraph = getBrowser().getCrmGraph();
        if (infoPanel != null) {
            crmGraph.pickBackground();
            return infoPanel;
        }
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.PAGE_AXIS));
        if (getBrowser().getCrmXml() == null || getBrowser().getClusterStatus() == null) {
            return newPanel;
        }
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;

            /**
             * Whether the whole thing should be enabled.
             */
            @Override
            public boolean isEnabled() {
                final Host dcHost = getBrowser().getDCHost();
                return dcHost != null && !Tools.versionBeforePacemaker(dcHost);
            }

            @Override
            public void mouseOut(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = false;
                crmGraph.stopTestAnimation((JComponent) component);
                component.setToolTipText("");
            }

            @Override
            public void mouseOver(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                component.setToolTipText(ClusterBrowser.STARTING_PTEST_TOOLTIP);
                component.setToolTipBackground(Tools.getDefaultColor("ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                crmGraph.startTestAnimation((JComponent) component, startTestLatch);
                final Host dcHost = getBrowser().getDCHost();
                getBrowser().ptestLockAcquire();
                try {
                    final ClusterStatus clStatus = getBrowser().getClusterStatus();
                    clStatus.setPtestResult(null);
                    apply(dcHost, Application.RunMode.TEST);
                    final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
                    component.setToolTipText(ptestData.getToolTip());
                    clStatus.setPtestResult(ptestData);
                } finally {
                    getBrowser().ptestLockRelease();
                }
                startTestLatch.countDown();
            }
        };
        initCommitButton(buttonCallback);
        getBrowser().getRscDefaultsInfo().setApplyButton(getApplyButton());
        getBrowser().getRscDefaultsInfo().setRevertButton(getRevertButton());
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        buttonPanel.add(getActionsButton(), BorderLayout.LINE_END);

        newPanel.add(buttonPanel);

        final String[] params = getParametersFromXML();
        addParams(optionsPanel,
                  params,
                  application.getDefaultSize("ClusterBrowser.DrbdResLabelWidth"),
                  application.getDefaultSize("ClusterBrowser.DrbdResFieldWidth"),
                  null);

        addRscDefaultsPanel(optionsPanel,
                            application.getDefaultSize("ClusterBrowser.DrbdResLabelWidth"),
                            application.getDefaultSize("ClusterBrowser.DrbdResFieldWidth"));
        getApplyButton().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    LOG.debug1("actionPerformed: BUTTON: apply");
                    final Thread thread = new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(getBrowser().getDCHost(), Application.RunMode.LIVE);
                                getBrowser().clStatusUnlock();
                            }
                        }
                    );
                    thread.start();
                }
            }
        );
        getRevertButton().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    LOG.debug1("actionPerformed: BUTTON: revert");
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getBrowser().clStatusLock();
                            revert();
                            getBrowser().clStatusUnlock();
                        }
                    });
                    thread.start();
                }
            }
        );

        /* apply button */
        addApplyButton(buttonPanel);
        addRevertButton(buttonPanel);
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                setApplyButtons(null, params);
            }
        });

        mainPanel.add(optionsPanel);

        newPanel.add(getMoreOptionsPanel(application.getServiceLabelWidth() + application.getServiceFieldWidth() + 4));
        newPanel.add(new JScrollPane(mainPanel));

        crmGraph.pickBackground();
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    /** Returns heartbeat graph. */
    @Override
    public JPanel getGraphicalView() {
        return getBrowser().getCrmGraph().getGraphPanel();
    }

    /**
     * Adds service to the list of services.
     * TODO: are they both used?
     */
    public ServiceInfo addServicePanel(final ResourceAgent newResourceAgent,
                                       final Point2D pos,
                                       final boolean reloadNode,
                                       final String heartbeatId,
                                       final CloneInfo newCi,
                                       final Application.RunMode runMode) {
        final ServiceInfo newServiceInfo = crmServiceFactory.createFromResourceAgent(
                newResourceAgent,
                getBrowser().getClusterStatus().isMaster(heartbeatId),
                getBrowser());
        if (heartbeatId != null) {
            newServiceInfo.getService().setCrmId(heartbeatId);
            getBrowser().addToHeartbeatIdList(newServiceInfo);
        }
        if (newCi == null) {
            addServicePanel(newServiceInfo, pos, reloadNode, true, runMode);
        }
        return newServiceInfo;
    }

    /**
     * Adds new service to the specified position. If position is null, it
     * will be computed later. reloadNode specifies if the node in
     * the menu should be reloaded and get uptodate.
     */
    public void addServicePanel(final ServiceInfo newServiceInfo,
                                final Point2D pos,
                                final boolean reloadNode,
                                final boolean interactive,
                                final Application.RunMode runMode) {
        swingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                newServiceInfo.getService().setResourceClass(newServiceInfo.getResourceAgent().getResourceClass());
                final CrmGraph hg = getBrowser().getCrmGraph();
                getBrowser().addNameToServiceInfoHash(newServiceInfo);
                if (!hg.addResource(newServiceInfo,
                                    null,
                                    pos,
                                    false, /* colocation only */
                                    false, /* order only */
                                    runMode)) {
                    final DefaultMutableTreeNode newServiceNode = clusterTreeMenu.createMenuItem(
                            getBrowser().getServicesNode(),
                            newServiceInfo);
                    if (interactive) {
                        if (newServiceInfo.getResourceAgent().isProbablyMasterSlave()) {
                            /* only if it was added manually. */
                            newServiceInfo.changeType(ServiceInfo.MASTER_SLAVE_TYPE_STRING);
                        } else if (newServiceInfo.getResourceAgent().isProbablyClone()) {
                            newServiceInfo.changeType(ServiceInfo.CLONE_TYPE_STRING);
                        }
                    }
                    if (reloadNode) {
                        /* show it */
                        clusterTreeMenu.reloadNodeDontSelect(getBrowser().getServicesNode());
                        clusterTreeMenu.reloadNode(newServiceNode);
                    }
                    getBrowser().reloadAllComboBoxes(newServiceInfo);
                    hg.scale();
                }
                hg.reloadServiceMenus();
            }
        });
    }

    /** Returns 'add service' list for graph popup menu. */
    List<ResourceAgent> getAddServiceList(final String cl) {
        return getBrowser().globalGetAddServiceList(cl);
    }

    /**
     * Returns background popup. Click on background represents cluster as
     * whole.
     */
    @Override
    public List<UpdatableItem> createPopup() {
        return servicesMenu.getPulldownMenu(this);
    }
    /**
     * Returns whether all the parameters are correct. If param is null,
     * all parameters will be checked, otherwise only the param, but other
     * parameters will be checked only in the cache. This is good if only
     * one value is changed and we don't want to check everything.
     */
    @Override
    public Check checkResourceFields(final String param, final String[] params) {
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        final Check check = new Check(new ArrayList<String>(), new ArrayList<String>());
        check.addCheck(rdi.checkResourceFields(param, rdi.getParametersFromXML(), true));
        check.addCheck(super.checkResourceFields(param, params));
        for (final ServiceInfo si : getBrowser().getExistingServiceList(null)) {
            check.addCheck(si.checkResourceFields(null, si.getParametersFromXML(), true, false, false));
        }
        return check;
    }

    /** Revert all values. */
    @Override
    public void revert() {
        super.revert();
        final RscDefaultsInfo rdi = getBrowser().getRscDefaultsInfo();
        rdi.revert();
        for (final ServiceInfo si : getBrowser().getExistingServiceList(null)) {
            if (si.checkResourceFields(null, si.getParametersFromXML(), true, false, false).isChanged()) {
                si.revert();
            }
        }
        //TODO: should remove new resources and constraints
    }

    /**
     * Copy/paste field from one field to another.
     */
    private void copyPasteField(final Widget oldWi, final Widget newWi) {
        if (newWi == null || oldWi == null) {
            return;
        }
        final Value oldValue = oldWi.getValue();
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (oldValue == null || oldValue.isNothingSelected()) {
                    newWi.setValueNoListeners(null);
                } else {
                    newWi.setValueNoListeners(oldValue);
                }
            }
        });
    }

    private void copyPasteFields(final ServiceInfo oldSi, final ServiceInfo newSi) {
        /* parameters */
        for (final String param : oldSi.getParametersFromXML()) {
            if (ServiceInfo.GUI_ID.equals(param) || ServiceInfo.PCMK_ID.equals(param)) {
                if (getBrowser().isCrmId(oldSi.getService().getCrmId())) {
                    continue;
                }
            }
            copyPasteField(oldSi.getWidget(param, null), newSi.getWidget(param, null));
        }

        /* operations */
        copyPasteField(oldSi.getSameAsOperationsWi(), newSi.getSameAsOperationsWi());

        for (final String op : oldSi.getResourceAgent().getOperationNames()) {
            for (final String param : getBrowser().getCrmOperationParams(op)) {
                copyPasteField(oldSi.getOperationsComboBox(op, param), newSi.getOperationsComboBox(op, param));
            }
        }

        /* locations */
        for (final Host host : getBrowser().getClusterHosts()) {
            final HostInfo hi = host.getBrowser().getHostInfo();
            copyPasteField(oldSi.getScoreComboBoxHash().get(hi), newSi.getScoreComboBoxHash().get(hi));
        }
        /* ping */
        copyPasteField(oldSi.getPingComboBox(), newSi.getPingComboBox());
    }

    public void pasteServices(final List<Info> oldInfos) {
        if (oldInfos.isEmpty()) {
            return;
        }
        final String cn = getBrowser().getCluster().getName();
        progressIndicator.startProgressIndicator(cn, "paste");
        final ClusterBrowser otherBrowser = (ClusterBrowser) oldInfos.get(0).getBrowser();
        getBrowser().setDisabledDuringLoad(true);
        otherBrowser.setDisabledDuringLoad(true);
        for (Info oldI : oldInfos) {
            CloneInfo oci = null;
            if (oldI instanceof CloneInfo) {
                oci = (CloneInfo) oldI;
                oldI = oci.getContainedService();
            }
            final CloneInfo oldCi = oci;
            if (oldI instanceof ServiceInfo) {
                final ServiceInfo oldSi = (ServiceInfo) oldI;
                final ServiceInfo newSi =
                    addServicePanel(oldSi.getResourceAgent(),
                                    null, /* pos */
                                    true,
                                    null, /* clone id */
                                    null,
                                    Application.RunMode.LIVE);
                swingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!(newSi instanceof CloneInfo)) {
                            oldSi.getInfoPanel();
                            newSi.getInfoPanel();
                            oldSi.waitForInfoPanel();
                            newSi.waitForInfoPanel();
                        }
                        if (oldCi != null) {
                            final Value v = newSi.getTypeRadioGroup().getValue();
                            if (oldCi.getService().isMaster()) {
                                if (!ServiceInfo.MASTER_SLAVE_TYPE_STRING.equals(v)) {
                                    newSi.getTypeRadioGroup().setValue(ServiceInfo.MASTER_SLAVE_TYPE_STRING);
                                }
                            } else {
                                if (!ServiceInfo.CLONE_TYPE_STRING.equals(v)) {
                                    newSi.getTypeRadioGroup().setValue(ServiceInfo.CLONE_TYPE_STRING);
                                }
                            }
                        }
                        copyPasteFields(oldSi, newSi);
                    }
                });

                /* clone parameters */
                final CloneInfo newCi = newSi.getCloneInfo();
                if (newCi != null) {
                    for (final String param : oldCi.getParametersFromXML()) {
                        if (ServiceInfo.GUI_ID.equals(param) || ServiceInfo.PCMK_ID.equals(param)) {
                            if (getBrowser().isCrmId(oldCi.getService().getCrmId())) {
                                continue;
                            }
                        }
                        swingUtils.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                copyPasteField(oldCi.getWidget(param, null), newCi.getWidget(param, null));
                            }
                        });
                    }
                }
                if (oldI instanceof GroupInfo) {
                    final GroupInfo oldGi = (GroupInfo) oldI;
                    final GroupInfo newGi = (GroupInfo) newSi;

                    swingUtils.invokeInEdt(new Runnable() {
                        @Override
                        public void run() {
                            for (final ServiceInfo oldChild : oldGi.getSubServices()) {
                                oldChild.getInfoPanel();
                                final ServiceInfo newChild =
                                        newGi.addGroupServicePanel(oldChild.getResourceAgent(), false);
                                newChild.getInfoPanel();
                                copyPasteFields(oldChild, newChild);
                            }
                            clusterTreeMenu.reloadNodeDontSelect(newGi.getNode());
                        }
                    });
                }
            }
        }
        progressIndicator.stopProgressIndicator(cn, "paste");
        otherBrowser.setDisabledDuringLoad(false);
        getBrowser().setDisabledDuringLoad(false);
    }

    public void exportGraphAsPng() {
        final Optional<String> savePath = dialogs.getFileName("lcmc-pcmk");
        if (savePath.isPresent()) {
            new Thread() {
                public void run() {
                    BufferedImage image = getBrowser().getCrmGraph().createImage();
                    Tools.writeImage(savePath.get(), image, "PNG");
                }
            }.start();
        }
    }

    public void cleanupServiceMenu(final List<ServiceInfo> groupServiceIsPresent) {
        for (final Object info : clusterTreeMenu.nodesToInfos(getNode().children())) {
            final ServiceInfo serviceInfo = (ServiceInfo) info;
            for (final ServiceInfo subService : serviceInfo.getSubServices()) {
                if (!groupServiceIsPresent.contains(subService) && !subService.getService().isNew()) { //TODO: NPE
                    /* remove the group service from the menu
                       that does not exist anymore. */
                    subService.removeInfo();
                }
            }
        }
    }

    public void reloadNode() {
        clusterTreeMenu.reloadNodeDontSelect(getBrowser().getServicesNode());
    }

    public void moveNodeToPosition(int pos, DefaultMutableTreeNode node) {
        clusterTreeMenu.moveNodeUpToPosition(node, pos);
    }
}
