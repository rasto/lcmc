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
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.ResourceValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.*;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.treemenu.ClusterTreeMenu;
import lcmc.common.ui.utils.ButtonCallback;
import lcmc.common.ui.utils.ComponentWithTest;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.crm.domain.ClusterStatus;
import lcmc.crm.domain.PtestData;
import lcmc.crm.service.CRM;
import lcmc.host.domain.Host;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * This class describes a connection between two heartbeat services.
 * It can be order, colocation or both.
 */
public class HbConnectionInfo extends EditableInfo {
    private final Supplier<HbColocationInfo> colocationInfoProvider;
    private final Supplier<HbOrderInfo> orderInfoProvider;
    private final Application application;
    private final SwingUtils swingUtils;
    private final HbConnectionMenu hbConnectionMenu;
    private final ClusterTreeMenu clusterTreeMenu;

    private JComponent infoPanel = null;
    /** Constraints. */
    private final Collection<HbConstraintInterface> constraints = new ArrayList<HbConstraintInterface>();
    private final ReadWriteLock mConstraintsLock = new ReentrantReadWriteLock();
    private final Lock mConstraintsReadLock = mConstraintsLock.readLock();
    private final Lock mConstraintsWriteLock = mConstraintsLock.writeLock();
    private ServiceInfo lastServiceInfoRsc = null;
    private ServiceInfo lastServiceInfoWithRsc = null;
    private ServiceInfo lastServiceInfoParent = null;
    private ServiceInfo lastServiceInfoChild = null;
    private final Map<String, HbColocationInfo> colocationIds = new LinkedHashMap<String, HbColocationInfo>();
    private final Map<String, HbOrderInfo> orderIds = new LinkedHashMap<String, HbOrderInfo>();

    public HbConnectionInfo(Application application, SwingUtils swingUtils, Access access, MainData mainData, WidgetFactory widgetFactory, Supplier<HbColocationInfo> colocationInfoProvider, Supplier<HbOrderInfo> orderInfoProvider, HbConnectionMenu hbConnectionMenu, ClusterTreeMenu clusterTreeMenu) {
        super(application, swingUtils, access, mainData, widgetFactory);
        this.colocationInfoProvider = colocationInfoProvider;
        this.orderInfoProvider = orderInfoProvider;
        this.application = application;
        this.swingUtils = swingUtils;
        this.hbConnectionMenu = hbConnectionMenu;
        this.clusterTreeMenu = clusterTreeMenu;
    }

    public void init(final Browser browser) {
        super.einit(Optional.<ResourceValue>absent(), "HbConnectionInfo", browser);
    }

    /** Returns browser object of this info. */
    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** Returns whether one of the services are newly added. */
    public final boolean isNew() {
        return (lastServiceInfoRsc != null && lastServiceInfoRsc.getService().isNew())
                || (lastServiceInfoWithRsc != null && lastServiceInfoWithRsc.getService().isNew())
                || (lastServiceInfoParent != null && lastServiceInfoParent.getService().isNew())
                || (lastServiceInfoChild != null && lastServiceInfoChild.getService().isNew());
    }

    @Override
    protected final String getParamLongDesc(final String param) {
        return null;
    }

    @Override
    protected final String getParamShortDesc(final String param) {
        return null;
    }

    @Override
    protected final boolean checkParam(final String param, final Value newValue) {
        return false;
    }

    @Override
    protected final Value getParamDefault(final String param) {
        return null;
    }

    @Override
    protected final Value getParamPreferred(final String param) {
        return null;
    }

    @Override
    public final String[] getParametersFromXML() {
        return null;
    }

    @Override
    protected final Value[] getParamPossibleChoices(final String param) {
        return null;
    }

    @Override
    protected final String getParamType(final String param) {
        return null;
    }

    @Override
    protected final String getSection(final String param) {
        return null;
    }

    @Override
    protected final boolean isCheckBox(final String param) {
        return false;
    }

    @Override
    protected final boolean isTimeType(final String param) {
        return false;
    }

    @Override
    protected final boolean isInteger(final String param) {
        return false;
    }

    @Override
    protected final boolean isLabel(final String param) {
        return false;
    }

    @Override
    protected final boolean isRequired(final String param) {
        return true;
    }

    public final ServiceInfo getLastServiceInfoParent() {
        return lastServiceInfoParent;
    }

    public final ServiceInfo getLastServiceInfoChild() {
        return lastServiceInfoChild;
    }

    public final ServiceInfo getLastServiceInfoRsc() {
        return lastServiceInfoRsc;
    }

    public final ServiceInfo getLastServiceInfoWithRsc() {
        return lastServiceInfoWithRsc;
    }

    @Override
    public final JPanel getGraphicalView() {
        return getBrowser().getCrmGraph().getGraphPanel();
    }

    void apply(final Host dcHost, final Application.RunMode runMode) {
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
        final Collection<HbConstraintInterface> constraintsCopy = new ArrayList<HbConstraintInterface>();
        mConstraintsReadLock.lock();
        try {
            for (final HbConstraintInterface c : constraints) {
                constraintsCopy.add(c);
            }
        } finally {
            mConstraintsReadLock.unlock();
        }
        for (final HbConstraintInterface c : constraintsCopy) {
            c.apply(dcHost, runMode);
        }
        if (Application.isLive(runMode)) {
            setApplyButtons(null, null);
            getBrowser().setRightComponentInView(this);
        }
    }

    @Override
    public Check checkResourceFields(final String param, final String[] params) {
        mConstraintsReadLock.lock();
        final Check check = new Check(new ArrayList<String>(), new ArrayList<String>());
        try {
            for (final HbConstraintInterface c : constraints) {
                check.addCheck(c.checkResourceFields(param, c.getParametersFromXML(), true));
            }
        } finally {
            mConstraintsReadLock.unlock();
        }
        return check;
    }

    /** Returns panal with user visible info. */
    protected JPanel getLabels(final HbConstraintInterface c) {
        final JPanel panel = getParamPanel(c.getName());
        panel.setLayout(new SpringLayout());
        final int height = application.getDefaultSize("Browser.LabelFieldHeight");
        c.addLabelField(panel,
                        Tools.getString("ClusterBrowser.HeartbeatId"),
                        c.getService().getCrmId(),
                        application.getServiceLabelWidth(),
                        application.getServiceFieldWidth(),
                        height);
        c.addLabelField(panel,
                        c.getRsc1Name(),
                        c.getRsc1(),
                        application.getServiceLabelWidth(),
                        application.getServiceFieldWidth(),
                        height);
        c.addLabelField(panel,
                        c.getRsc2Name(),
                        c.getRsc2(),
                        application.getServiceLabelWidth(),
                        application.getServiceFieldWidth(),
                        height);
        final int rows = 3;
        SpringUtilities.makeCompactGrid(panel, rows, 2, /* rows, cols */
                                        1, 1,        /* initX, initY */
                                        1, 1);       /* xPad, yPad */
        return panel;
    }

    /**
     * Returns info panel for hb connection (order and/or colocation
     * constraint.
     */
    @Override
    public final JComponent getInfoPanel() {
        swingUtils.isSwingThread();
        if (infoPanel != null) {
            return infoPanel;
        }
        final ButtonCallback buttonCallback = new ButtonCallback() {
            private volatile boolean mouseStillOver = false;

            /** Whether the whole thing should be enabled. */
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
                getBrowser().getCrmGraph().stopTestAnimation((JComponent) component);
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
                getBrowser().getCrmGraph().startTestAnimation((JComponent) component, startTestLatch);
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
        initApplyButton(buttonCallback);
        for (final Map.Entry<String, HbColocationInfo> colocationEntry : colocationIds.entrySet()) {
            colocationEntry.getValue().setApplyButton(getApplyButton());
            colocationEntry.getValue().setRevertButton(getRevertButton());
        }
        for (final Map.Entry<String, HbOrderInfo> orderEntry : orderIds.entrySet()) {
            orderEntry.getValue().setApplyButton(getApplyButton());
            orderEntry.getValue().setRevertButton(getRevertButton());
        }
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        mainPanel.add(buttonPanel);

        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.LINE_END);

        /* params */
        mConstraintsReadLock.lock();
        EditableInfo firstConstraint = null;
        try {
            for (final HbConstraintInterface c : constraints) {
                if (firstConstraint == null) {
                    firstConstraint = (EditableInfo) c;
                }
                final String[] params = c.getParametersFromXML();
                final JPanel panel = getLabels(c);

                optionsPanel.add(panel);
                c.addParams(optionsPanel,
                            params,
                            application.getServiceLabelWidth(),
                            application.getServiceFieldWidth(),
                            null);
            }
        } finally {
            mConstraintsReadLock.unlock();
        }
        getApplyButton().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getBrowser().clStatusLock();
                            apply(getBrowser().getDCHost(), Application.RunMode.LIVE);
                            getBrowser().clStatusUnlock();
                        }
                    });
                    thread.start();
                }
            }
        );
        getRevertButton().addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
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
                setApplyButtons(null, null);
            }
        });
        mainPanel.add(optionsPanel);

        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.PAGE_AXIS));
        newPanel.add(buttonPanel);
        if (firstConstraint != null) {
        newPanel.add(firstConstraint.getMoreOptionsPanel(application.getServiceLabelWidth()
                                                         + application.getServiceFieldWidth() + 4));
        }
        newPanel.add(new JScrollPane(mainPanel));
        newPanel.setMinimumSize(new Dimension(
                                application.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                                application.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        newPanel.setPreferredSize(new Dimension(
                                  application.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                                  application.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    /**
     * Creates popup menu for heartbeat order and colocation dependencies.
     * These are the edges in the graph.
     */
    @Override
    public List<UpdatableItem> createPopup() {
        return hbConnectionMenu.getPulldownMenu(this);
    }

    /** Removes colocations or orders. */
    private void removeOrdersOrColocations(final boolean isOrder) {
        final Collection<HbConstraintInterface> constraintsToRemove = new ArrayList<HbConstraintInterface>();
        mConstraintsWriteLock.lock();
        boolean changed = false;
        try {
            for (final HbConstraintInterface c : constraints) {
                if (c.isOrder() == isOrder) {
                    constraintsToRemove.add(c);
                    changed = true;
                }
            }
            for (final HbConstraintInterface c : constraintsToRemove) {
               if (isOrder) {
                   orderIds.remove(c.getService().getCrmId());
               } else {
                   colocationIds.remove(c.getService().getCrmId());
               }
               constraints.remove(c);
            }
        } finally {
            mConstraintsWriteLock.unlock();
        }
        if (changed) {
            infoPanel = null;
            selectMyself();
        }
    }

    public final void removeOrders() {
        removeOrdersOrColocations(true);
    }

    public final void removeColocations() {
        removeOrdersOrColocations(false);
    }

    public final void addOrder(final String ordId,
                               final ServiceInfo serviceInfoParent,
                               final ServiceInfo serviceInfoChild) {
        lastServiceInfoParent = serviceInfoParent;
        lastServiceInfoChild = serviceInfoChild;
        if (ordId == null) {
            /* we'll get it later with id. */
            return;
        }
        if (orderIds.containsKey(ordId)) {
            final HbOrderInfo hoi = orderIds.get(ordId);
            hoi.setServiceInfoParent(serviceInfoParent);
            hoi.setServiceInfoChild(serviceInfoChild);
            hoi.setParameters();
            return;
        }
        final HbOrderInfo orderInfo = orderInfoProvider.get();
        orderInfo.init(this, serviceInfoParent, serviceInfoChild, getBrowser());
        orderInfo.setApplyButton(getApplyButton());
        orderInfo.setRevertButton(getRevertButton());
        orderIds.put(ordId, orderInfo);
        orderInfo.getService().setCrmId(ordId);
        orderInfo.setParameters();
        mConstraintsWriteLock.lock();
        try {
            constraints.add(orderInfo);
        } finally {
            mConstraintsWriteLock.unlock();
        }
        infoPanel = null;
        selectMyself();
    }

    /** Adds a new colocation. */
    public final void addColocation(final String colId,
                                    final ServiceInfo serviceInfoRsc,
                                    final ServiceInfo serviceInfoWithRsc) {
        lastServiceInfoRsc = serviceInfoRsc;
        lastServiceInfoWithRsc = serviceInfoWithRsc;
        if (colId == null) {
            /* we'll get it later with id. */
            return;
        }
        if (colocationIds.containsKey(colId)) {
            final HbColocationInfo hci = colocationIds.get(colId);
            hci.setServiceInfoRsc(serviceInfoRsc);
            hci.setServiceInfoWithRsc(serviceInfoWithRsc);
            hci.setParameters();
            return;
        }
        final HbColocationInfo colocationInfo = colocationInfoProvider.get();
        colocationInfo.init(this, serviceInfoRsc, serviceInfoWithRsc, getBrowser());
        colocationInfo.setApplyButton(getApplyButton());
        colocationInfo.setRevertButton(getRevertButton());
        colocationIds.put(colId, colocationInfo);
        colocationInfo.getService().setCrmId(colId);
        colocationInfo.setParameters();
        mConstraintsWriteLock.lock();
        try {
            constraints.add(colocationInfo);
        } finally {
            mConstraintsWriteLock.unlock();
        }
        infoPanel = null;
        selectMyself();
    }

    /**
     * Returns whether the colocation score is negative. Order of rsc1 rsc2 is
     * not important.
     */
    public final ColScoreType getColocationScoreType(final ServiceInfo rsc1, final ServiceInfo rsc2) {
        int score = 0;
        boolean plusInf = false;
        boolean minusInf = false;
        for (final Map.Entry<String, HbColocationInfo> colocationEntry : colocationIds.entrySet()) {
            final HbColocationInfo hbci = colocationEntry.getValue();
            if (hbci == null) {
                continue;
            }
            if ((rsc1 != null && rsc2 != null)
                && (hbci.getRscInfo1() != rsc1 || hbci.getRscInfo2() != rsc2)
                && (hbci.getRscInfo1() != rsc2 || hbci.getRscInfo2() != rsc1)) {
                continue;
            }

            final int s = hbci.getScore();
            if (s == 1000000) {
                plusInf = true;
            } else if (s == -1000000) {
                minusInf = true;
            }
            score += s;
        }
        if ((plusInf && minusInf) || colocationIds.isEmpty()) {
            return ColScoreType.MIXED;
        } else if (plusInf) {
            return ColScoreType.INFINITY;
        } else if (minusInf) {
            return ColScoreType.MINUS_INFINITY;
        } else if (score == 0) {
            return ColScoreType.IS_NULL;
        } else if (score < 0) {
            return ColScoreType.NEGATIVE;
        } else {
            return ColScoreType.POSITIVE;
        }
    }

    /** Returns whether the order score is negative. */
    public final boolean isOrdScoreNull(final ServiceInfo first, final ServiceInfo then) {
        if (isNew() || orderIds.isEmpty()) {
            return false;
        }
        int score = 0;
        for (final Map.Entry<String, HbOrderInfo> orderEntry : orderIds.entrySet()) {
            final HbOrderInfo hoi = orderEntry.getValue();
            if (hoi == null) {
                continue;
            }
            if (first != null && hoi.getRscInfo1() != first) {
                continue;
            }
            if (then != null && hoi.getRscInfo2() != then) {
                continue;
            }
            int s = hoi.getScore();
            if (s < 0) {
                s = 0;
            }
            score += s;
        }
        return score == 0;
    }

    /** Selects the node in the menu and reloads everything underneath. */
    @Override
    public final void selectMyself() {
        super.selectMyself();
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) clusterTreeMenu.getMenuTree().getLastSelectedPathComponent();
        if (node != null) {
            // TODO: do this differently, don't need to select it, only reload
            final Info prev = (Info) node.getUserObject();
            getBrowser().setRightComponentInView(this); /* just to reload */
            getBrowser().setRightComponentInView(prev); /* back were we were */
        }
    }

    @Override
    protected final boolean isAdvanced(final String param) {
        return false;
    }

    @Override
    protected final AccessMode.Type getAccessType(final String param) {
        return AccessMode.ADMIN;
    }

    @Override
    protected final String isEnabled(final String param) {
        return null;
    }

    @Override
    protected final AccessMode.Mode isEnabledOnlyInAdvancedMode(final String param) {
         return AccessMode.NORMAL;
    }

    @Override
    public final void updateAdvancedPanels() {
        super.updateAdvancedPanels();
        mConstraintsReadLock.lock();
        try {
            for (final HbConstraintInterface c : constraints) {
                c.updateAdvancedPanels();
            }
        } finally {
            mConstraintsReadLock.unlock();
        }
    }

    /** Returns whether this resource is resource 1 in colocation constraint. */
    public final boolean isWithRsc(final ServiceInfo si) {
        mConstraintsReadLock.lock();
        try {
            for (final HbConstraintInterface c : constraints) {
                if (!c.isOrder()) {
                    ServiceInfo rsc2 = c.getRscInfo2();
                    final GroupInfo gi = rsc2.getGroupInfo();
                    if (gi != null) {
                        rsc2 = gi;
                    }
                    if (rsc2.equals(si)) {
                        return true;
                    }
                }
            }
        } finally {
            mConstraintsReadLock.unlock();
        }
        return true;
    }

    /**
     * Returns whether colocation or order have at least two different
     * directions.
     */
    private boolean isTwoDirections(final boolean isOrder) {
        mConstraintsReadLock.lock();
        try {
            ServiceInfo allRsc1 = null;
            ServiceInfo allRsc2 = null;
            for (final HbConstraintInterface c : constraints) {
                if (c.isOrder() == isOrder) {
                    ServiceInfo rsc1 = c.getRscInfo1();
                    ServiceInfo rsc2 = c.getRscInfo2();
                    final GroupInfo gi1 = rsc1.getGroupInfo();
                    if (gi1 != null) {
                        rsc1 = gi1;
                    }
                    final GroupInfo gi2 = rsc2.getGroupInfo();
                    if (gi2 != null) {
                        rsc2 = gi2;
                    }
                    if (allRsc1 == null) {
                        allRsc1 = rsc1;
                    } else if (!rsc1.equals(allRsc1)) {
                        return true;
                    }
                    if (allRsc2 == null) {
                        allRsc2 = rsc2;
                    } else if (!rsc2.equals(allRsc2)) {
                        return true;
                    }
                }
            }
        } finally {
            mConstraintsReadLock.unlock();
        }
        return false;
    }

    /** Returns whether there are different directions of orders. */
    public final boolean isOrderTwoDirections() {
        return isTwoDirections(true);
    }

    /** Returns whether there are different directions of colocations. */
    public final boolean isColocationTwoDirections() {
        return isTwoDirections(false);
    }

    /** Returns whether this service has a colocation or order. */
    final boolean hasColocationOrOrder(final ServiceInfo si) {
        mConstraintsReadLock.lock();
        try {
            for (final HbConstraintInterface c : constraints) {
                final ServiceInfo rsc1 = c.getRscInfo1();
                final ServiceInfo rsc2 = c.getRscInfo2();
                if (si.equals(rsc1) || si.equals(rsc2)) {
                    return true;
                }
            }
        } finally {
            mConstraintsReadLock.unlock();
        }
        return false;
    }

    /** Returns colocation attributes knowing the col id. */
    final Map<String, String> getColocationAttributes(final String colId) {
        final HbColocationInfo hci = colocationIds.get(colId);
        if (hci != null) {
            return hci.getAttributes();
        }
        return null;
    }

    /** Returns order attributes knowing the ord id. */
    final Map<String, String> getOrderAttributes(final String ordId) {
        final HbOrderInfo hoi = orderIds.get(ordId);
        if (hoi != null) {
            return hoi.getAttributes();
        }
        return null;
    }

    @Override
    public final void revert() {
        super.revert();
        final Collection<HbConstraintInterface> constraintsCopy = new ArrayList<HbConstraintInterface>();
        mConstraintsReadLock.lock();
        try {
            for (final HbConstraintInterface c : constraints) {
                constraintsCopy.add(c);
            }
        } finally {
            mConstraintsReadLock.unlock();
        }
        for (final HbConstraintInterface c : constraintsCopy) {
            c.revert();
        }
    }

    public enum ColScoreType {
        MIXED,
        INFINITY,
        MINUS_INFINITY,
        IS_NULL,
        NEGATIVE,
        POSITIVE
    }
}
