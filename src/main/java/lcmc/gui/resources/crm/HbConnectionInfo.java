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
package lcmc.gui.resources.crm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.tree.DefaultMutableTreeNode;
import lcmc.data.Application;
import lcmc.data.crm.ClusterStatus;
import lcmc.data.Host;
import lcmc.data.crm.PtestData;
import lcmc.data.Value;
import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.EditableInfo;
import lcmc.gui.resources.Info;
import lcmc.gui.widget.Check;
import lcmc.utilities.ButtonCallback;
import lcmc.utilities.CRM;
import lcmc.utilities.ComponentWithTest;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

/**
 * This class describes a connection between two heartbeat services.
 * It can be order, colocation or both.
 */
public class HbConnectionInfo extends EditableInfo {
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Constraints. */
    private final Collection<HbConstraintInterface> constraints =
                                   new ArrayList<HbConstraintInterface>();
    /** constraints lock. */
    private final ReadWriteLock mConstraintsLock = new ReentrantReadWriteLock();
    private final Lock mConstraintsReadLock = mConstraintsLock.readLock();
    private final Lock mConstraintsWriteLock = mConstraintsLock.writeLock();
    /** Resource 1 in colocation constraint (the last one). */
    private ServiceInfo lastServiceInfoRsc = null;
    /** Resource 2 in colocation constraint (the last one). */
    private ServiceInfo lastServiceInfoWithRsc = null;
    /** Parent resource in order constraint (the last one). */
    private ServiceInfo lastServiceInfoParent = null;
    /** Child resource in order constraint (the last one). */
    private ServiceInfo lastServiceInfoChild = null;
    /** List of colocation ids. */
    private final Map<String, HbColocationInfo> colocationIds =
                                 new LinkedHashMap<String, HbColocationInfo>();
    /** List of order ids. */
    private final Map<String, HbOrderInfo> orderIds =
                                      new LinkedHashMap<String, HbOrderInfo>();

    /** Prepares a new {@code HbConnectionInfo} object. */
    public HbConnectionInfo(final Browser browser) {
        super("HbConnectionInfo", browser);
    }

    /** Returns browser object of this info. */
    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** Returns whether one of the services are newly added. */
    public final boolean isNew() {
        return (lastServiceInfoRsc != null
                && lastServiceInfoRsc.getService().isNew())
                || (lastServiceInfoWithRsc != null
                    && lastServiceInfoWithRsc.getService().isNew())
                || (lastServiceInfoParent != null
                    && lastServiceInfoParent.getService().isNew())
                || (lastServiceInfoChild != null
                    && lastServiceInfoChild.getService().isNew());
    }

    /**
     * Returns long description of the parameter, that is used for
     * tool tips.
     */
    @Override
    protected final String getParamLongDesc(final String param) {
        return null;
    }

    /** Returns short description of the parameter, that is used as * label. */
    @Override
    protected final String getParamShortDesc(final String param) {
        return null;
    }

    /**
     * Checks if the new value is correct for the parameter type and
     * constraints.
     */
    @Override
    protected final boolean checkParam(final String param,
                                       final Value newValue) {
        return false;
    }

    /** Returns default for this parameter. */
    @Override
    protected final Value getParamDefault(final String param) {
        return null;
    }

    /** Returns preferred value for this parameter. */
    @Override
    protected final Value getParamPreferred(final String param) {
        return null;
    }

    /** Returns lsit of all parameters as an array. */
    @Override
    public final String[] getParametersFromXML() {
        return null;
    }

    /**
     * Possible choices for pulldown menus, or null if it is not a pull
     * down menu.
     */
    @Override
    protected final Value[] getParamPossibleChoices(final String param) {
        return null;
    }

    /** Returns parameter type, boolean etc. */
    @Override
    protected final String getParamType(final String param) {
        return null;
    }

    /** Returns section to which the global belongs. */
    @Override
    protected final String getSection(final String param) {
        return null;
    }

    /**
     * Returns whether the parameter is of the boolean type and needs the
     * checkbox.
     */
    @Override
    protected final boolean isCheckBox(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is of time type. */
    @Override
    protected final boolean isTimeType(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is integer. */
    @Override
    protected final boolean isInteger(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is label. */
    @Override
    protected final boolean isLabel(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is required. */
    @Override
    protected final boolean isRequired(final String param) {
        return true;
    }

    /** Returns parent resource in order constraint. */
    public final ServiceInfo getLastServiceInfoParent() {
        return lastServiceInfoParent;
    }

    /** Returns child resource in order constraint. */
    public final ServiceInfo getLastServiceInfoChild() {
        return lastServiceInfoChild;
    }

    /** Returns resource 1 in colocation constraint. */
    public final ServiceInfo getLastServiceInfoRsc() {
        return lastServiceInfoRsc;
    }

    /** Returns resource 2 in colocation constraint. */
    public final ServiceInfo getLastServiceInfoWithRsc() {
        return lastServiceInfoWithRsc;
    }

    /** Returns heartbeat graphical view. */
    @Override
    public final JPanel getGraphicalView() {
        return getBrowser().getCRMGraph().getGraphPanel();
    }

    /** Applies the changes to the constraints. */
    void apply(final Host dcHost, final Application.RunMode runMode) {
        if (Application.isLive(runMode)) {
            Tools.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    getApplyButton().setEnabled(false);
                    getRevertButton().setEnabled(false);
                    getApplyButton().setToolTipText("");
                }
            });
        }
        final Collection<HbConstraintInterface> constraintsCopy
                                    = new ArrayList<HbConstraintInterface>();
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

    /** Check order and colocation constraints. */
    @Override
    public Check checkResourceFields(final String param,
                                     final String[] params) {
        mConstraintsReadLock.lock();
        final Check check = new Check(new ArrayList<String>(), 
                                      new ArrayList<String>());
        try {
            for (final HbConstraintInterface c : constraints) {
                check.addCheck(c.checkResourceFields(param,
                                                     c.getParametersFromXML(),
                                                     true));
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
        final int height = Tools.getDefaultSize("Browser.LabelFieldHeight");
        c.addLabelField(panel,
                        Tools.getString("ClusterBrowser.HeartbeatId"),
                        c.getService().getHeartbeatId(),
                        ClusterBrowser.SERVICE_LABEL_WIDTH,
                        ClusterBrowser.SERVICE_FIELD_WIDTH,
                        height);
        c.addLabelField(panel,
                        c.getRsc1Name(),
                        c.getRsc1(),
                        ClusterBrowser.SERVICE_LABEL_WIDTH,
                        ClusterBrowser.SERVICE_FIELD_WIDTH,
                        height);
        c.addLabelField(panel,
                        c.getRsc2Name(),
                        c.getRsc2(),
                        ClusterBrowser.SERVICE_LABEL_WIDTH,
                        ClusterBrowser.SERVICE_FIELD_WIDTH,
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
        Tools.isSwingThread();
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
                getBrowser().getCRMGraph().stopTestAnimation((JComponent) component);
                component.setToolTipText("");
            }

            @Override
            public void mouseOver(final ComponentWithTest component) {
                if (!isEnabled()) {
                    return;
                }
                mouseStillOver = true;
                component.setToolTipText(
                                        ClusterBrowser.STARTING_PTEST_TOOLTIP);
                component.setToolTipBackground(Tools.getDefaultColor(
                                    "ClusterBrowser.Test.Tooltip.Background"));
                Tools.sleep(250);
                if (!mouseStillOver) {
                    return;
                }
                mouseStillOver = false;
                final CountDownLatch startTestLatch = new CountDownLatch(1);
                getBrowser().getCRMGraph().startTestAnimation((JComponent) component,
                                                              startTestLatch);
                final Host dcHost = getBrowser().getDCHost();
                getBrowser().ptestLockAcquire();
                try {
                    final ClusterStatus clStatus = getBrowser().getClusterStatus();
                    clStatus.setPtestData(null);
                    apply(dcHost, Application.RunMode.TEST);
                    final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
                    component.setToolTipText(ptestData.getToolTip());
                    clStatus.setPtestData(ptestData);
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
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

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
                            ClusterBrowser.SERVICE_LABEL_WIDTH,
                            ClusterBrowser.SERVICE_FIELD_WIDTH,
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
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
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
        newPanel.add(firstConstraint.getMoreOptionsPanel(
                                  ClusterBrowser.SERVICE_LABEL_WIDTH
                                  + ClusterBrowser.SERVICE_FIELD_WIDTH + 4));
        }
        newPanel.add(new JScrollPane(mainPanel));
        newPanel.setMinimumSize(new Dimension(
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        newPanel.setPreferredSize(new Dimension(
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
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
        final HbConnectionMenu hbConnectionMenu = new HbConnectionMenu(this);
        return hbConnectionMenu.getPulldownMenu();
    }

    /** Removes colocations or orders. */
    private void removeOrdersOrColocations(final boolean isOrder) {
        final Collection<HbConstraintInterface> constraintsToRemove =
                                    new ArrayList<HbConstraintInterface>();
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
                   orderIds.remove(c.getService().getHeartbeatId());
               } else {
                   colocationIds.remove(c.getService().getHeartbeatId());
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

    /** Removes all orders. */
    public final void removeOrders() {
        removeOrdersOrColocations(true);
    }

    /** Removes all colocations. */
    public final void removeColocations() {
        removeOrdersOrColocations(false);
    }

    /** Adds a new order. */
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
        final HbOrderInfo oi = new HbOrderInfo(this,
                                               serviceInfoParent,
                                               serviceInfoChild,
                                               getBrowser());
        oi.setApplyButton(getApplyButton());
        oi.setRevertButton(getRevertButton());
        orderIds.put(ordId, oi);
        oi.getService().setHeartbeatId(ordId);
        oi.setParameters();
        mConstraintsWriteLock.lock();
        try {
            constraints.add(oi);
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
        final HbColocationInfo ci = new HbColocationInfo(this,
                                                         serviceInfoRsc,
                                                         serviceInfoWithRsc,
                                                         getBrowser());
        ci.setApplyButton(getApplyButton());
        ci.setRevertButton(getRevertButton());
        colocationIds.put(colId, ci);
        ci.getService().setHeartbeatId(colId);
        ci.setParameters();
        mConstraintsWriteLock.lock();
        try {
            constraints.add(ci);
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
    public final ColScoreType getColocationScoreType(final ServiceInfo rsc1,
                                               final ServiceInfo rsc2) {
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
    public final boolean isOrdScoreNull(final ServiceInfo first,
                                  final ServiceInfo then) {
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
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        getBrowser().getTree().getLastSelectedPathComponent();
        if (node != null) {
            // TODO: do this differently, don't need to select it, only reload
            final Info prev = (Info) node.getUserObject();
            getBrowser().setRightComponentInView(this); /* just to reload */
            getBrowser().setRightComponentInView(prev); /* back were we were */
        }
    }

    /** Returns whether this parameter is advanced. */
    @Override
    protected final boolean isAdvanced(final String param) {
        return false;
    }

    /** Returns access type of this parameter. */
    @Override
    protected final Application.AccessType getAccessType(final String param) {
        return Application.AccessType.ADMIN;
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected final String isEnabled(final String param) {
        return null;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override
    protected final boolean isEnabledOnlyInAdvancedMode(final String param) {
         return false;
    }

    /** Hide/Show advanced panels. */
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

    /** Revert all values. */
    @Override
    public final void revert() {
        super.revert();
        final Collection<HbConstraintInterface> constraintsCopy
                                    = new ArrayList<HbConstraintInterface>();
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
    /** Colocation Score type. */
    public enum ColScoreType { MIXED,
                               INFINITY,
                               MINUS_INFINITY,
                               IS_NULL,
                               NEGATIVE,
                               POSITIVE }
}
