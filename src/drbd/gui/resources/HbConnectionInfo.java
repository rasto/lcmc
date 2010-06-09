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
import drbd.gui.SpringUtilities;
import drbd.data.Host;
import drbd.data.PtestData;
import drbd.data.ClusterStatus;
import drbd.data.ConfigData;
import drbd.utilities.UpdatableItem;
import drbd.utilities.ButtonCallback;
import drbd.utilities.Tools;
import drbd.utilities.CRM;
import drbd.utilities.MyMenuItem;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.SwingUtilities;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.SpringLayout;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * This class describes a connection between two heartbeat services.
 * It can be order, colocation or both.
 */
public class HbConnectionInfo extends EditableInfo {
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Constraints. */
    private final List<HbConstraintInterface> constraints =
                                   new ArrayList<HbConstraintInterface>();
    /** constraints lock. */
    private final Mutex mConstraintsLock = new Mutex();
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
    /** Colocation Score type. */
    public enum ColScoreType { MIXED,
                               INFINITY,
                               MINUS_INFINITY,
                               IS_NULL,
                               NEGATIVE,
                               POSITIVE };

    /**
     * Prepares a new <code>HbConnectionInfo</code> object.
     */
    public HbConnectionInfo(final Browser browser) {
        super("HbConnectionInfo", browser);
    }

    /**
     * Returns browser object of this info.
     */
    protected final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /**
     * Returns whether one of the services are newly added.
     */
    public final boolean isNew() {
        if ((lastServiceInfoRsc != null
             && lastServiceInfoRsc.getService().isNew())
            || (lastServiceInfoWithRsc != null
                && lastServiceInfoWithRsc.getService().isNew())
            || (lastServiceInfoParent != null
                && lastServiceInfoParent.getService().isNew())
            || (lastServiceInfoChild != null
                && lastServiceInfoChild.getService().isNew())) {
            return true;
        }
        return false;
    }

    /**
     * Returns long description of the parameter, that is used for
     * tool tips.
     */
    protected final String getParamLongDesc(final String param) {
        return null;
    }

    /**
     * Returns short description of the parameter, that is used as * label.
     */
    protected final String getParamShortDesc(final String param) {
        return null;
    }

    /**
     * Checks if the new value is correct for the parameter type and
     * constraints.
     */
    protected final boolean checkParam(final String param,
                                       final String newValue) {
        return false;
    }

    /**
     * Returns default for this parameter.
     */
    protected final String getParamDefault(final String param) {
        return null;
    }

    /**
     * Returns preferred value for this parameter.
     */
    protected final String getParamPreferred(final String param) {
        return null;
    }

    /**
     * Returns lsit of all parameters as an array.
     */
    public final String[] getParametersFromXML() {
        return null;
    }

    /**
     * Possible choices for pulldown menus, or null if it is not a pull
     * down menu.
     */
    protected final Object[] getParamPossibleChoices(final String param) {
        return null;
    }

    /**
     * Returns parameter type, boolean etc.
     */
    protected final String getParamType(final String param) {
        return null;
    }

    /**
     * Returns section to which the global belongs.
     */
    protected final String getSection(final String param) {
        return null;
    }

    /**
     * Returns whether the parameter is of the boolean type and needs the
     * checkbox.
     */
    protected final boolean isCheckBox(final String param) {
        return false;
    }

    /**
     * Returns true if the specified parameter is of time type.
     */
    protected final boolean isTimeType(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is integer. */
    protected final boolean isInteger(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is label. */
    protected final boolean isLabel(final String param) {
        return false;
    }

    /**
     * Returns true if the specified parameter is required.
     */
    protected final boolean isRequired(final String param) {
        return true;
    }

    /**
     * Returns parent resource in order constraint.
     */
    public final ServiceInfo getLastServiceInfoParent() {
        return lastServiceInfoParent;
    }

    /**
     * Returns child resource in order constraint.
     */
    public final ServiceInfo getLastServiceInfoChild() {
        return lastServiceInfoChild;
    }

    /**
     * Returns resource 1 in colocation constraint.
     */
    public final ServiceInfo getLastServiceInfoRsc() {
        return lastServiceInfoRsc;
    }

    /**
     * Returns resource 2 in colocation constraint.
     */
    public final ServiceInfo getLastServiceInfoWithRsc() {
        return lastServiceInfoWithRsc;
    }

    /**
     * Returns heartbeat graphical view.
     */
    public final JPanel getGraphicalView() {
        return getBrowser().getHeartbeatGraph().getGraphPanel();
    }

    /**
     * Applies the changes to the constraints.
     */
    public final void apply(final Host dcHost, final boolean testOnly) {
        try {
            mConstraintsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final HbConstraintInterface c : constraints) {
            c.apply(dcHost, testOnly);
        }
        mConstraintsLock.release();
        if (!testOnly) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    applyButton.setEnabled(false);
                    applyButton.setToolTipText(null);
                }
            });
        }
    }

    /**
     * Check order and colocation constraints.
     */
    public final boolean checkResourceFields(final String param,
                                             final String[] params) {
        boolean correct = true;
        try {
            mConstraintsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final HbConstraintInterface c : constraints) {
            final boolean cor = c.checkResourceFieldsCorrect(
                                                  param,
                                                  c.getParametersFromXML());
            if (!cor) {
                correct = false;
                break;
            }
        }
        boolean changed = false;
        for (final HbConstraintInterface c : constraints) {
            final boolean chg = c.checkResourceFieldsChanged(
                                              param,
                                              c.getParametersFromXML());
            if (chg) {
                changed = true;
                break;
            }
        }
        mConstraintsLock.release();
        return correct && changed;
    }

    /**
     * Returns info panel for hb connection (order and/or colocation
     * constraint.
     */
    public final JComponent getInfoPanel() {
        if (infoPanel != null) {
            return infoPanel;
        }
        final HbConnectionInfo thisClass = this;
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
                getBrowser().getHeartbeatGraph().stopTestAnimation(applyButton);
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
                getBrowser().getHeartbeatGraph().startTestAnimation(
                                                            applyButton,
                                                            startTestLatch);
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
        for (final String col : colocationIds.keySet()) {
            colocationIds.get(col).applyButton = applyButton;
        }
        for (final String ord : orderIds.keySet()) {
            orderIds.get(ord).applyButton = applyButton;
        }
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        mainPanel.add(buttonPanel);

        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        final JMenu serviceCombo = getActionsMenu();
        mb.add(serviceCombo);
        buttonPanel.add(mb, BorderLayout.EAST);

        /* params */
        final int height = Tools.getDefaultInt("Browser.LabelFieldHeight");
        EditableInfo firstConstraint = null;
        try {
            mConstraintsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final HbConstraintInterface c : constraints) {
            if (firstConstraint == null) {
                firstConstraint = (EditableInfo) c;
            }
            final String[] params = c.getParametersFromXML();
            /* heartbeat id */
            final JPanel panel = getParamPanel(c.getName());
            panel.setLayout(new SpringLayout());
            final int rows = 3;
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
            SpringUtilities.makeCompactGrid(panel, rows, 2, /* rows, cols */
                                            1, 1,        /* initX, initY */
                                            1, 1);       /* xPad, yPad */

            optionsPanel.add(panel);
            c.addParams(optionsPanel,
                        params,
                        ClusterBrowser.SERVICE_LABEL_WIDTH,
                        ClusterBrowser.SERVICE_FIELD_WIDTH,
                        null);
        }
        mConstraintsLock.release();

        applyButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final Thread thread = new Thread(new Runnable() {
                        public void run() {
                            getBrowser().clStatusLock();
                            apply(getBrowser().getDCHost(), false);
                            getBrowser().clStatusUnlock();
                        }
                    });
                    thread.start();
                }
            }
        );

        /* apply button */
        addApplyButton(buttonPanel);
        applyButton.setEnabled(checkResourceFields(null, null));
        mainPanel.add(optionsPanel);

        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        if (firstConstraint != null) {
        newPanel.add(firstConstraint.getMoreOptionsPanel(
                                  ClusterBrowser.SERVICE_LABEL_WIDTH
                                  + ClusterBrowser.SERVICE_FIELD_WIDTH + 4));
        }
        newPanel.add(new JScrollPane(mainPanel));
        newPanel.setMinimumSize(new Dimension(
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")));
        newPanel.setPreferredSize(new Dimension(
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultInt("HostBrowser.ResourceInfoArea.Height")));
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    /**
     * Creates popup menu for heartbeat order and colocation dependencies.
     * These are the edges in the graph.
     */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();

        final HbConnectionInfo thisClass = this;
        final boolean testOnly = false;

        final MyMenuItem removeEdgeItem = new MyMenuItem(
                     Tools.getString("ClusterBrowser.Hb.RemoveEdge"),
                     ClusterBrowser.REMOVE_ICON,
                     Tools.getString("ClusterBrowser.Hb.RemoveEdge.ToolTip"),
                     ConfigData.AccessType.ADMIN,
                     ConfigData.AccessType.OP) {
            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                return !getBrowser().clStatusFailed();
            }

            public void action() {
                getBrowser().getHeartbeatGraph().removeConnection(
                                                      thisClass,
                                                      getBrowser().getDCHost(),
                                                      testOnly);
            }
        };
        final ClusterBrowser.ClMenuItemCallback removeEdgeCallback =
                  getBrowser().new ClMenuItemCallback(removeEdgeItem, null) {
            public final boolean isEnabled() {
                return super.isEnabled() && !isNew();
            }
            public void action(final Host dcHost) {
                if (!isNew()) {
                    getBrowser().getHeartbeatGraph().removeConnection(
                                                      thisClass,
                                                      dcHost,
                                                      true);
                }
            }
        };
        addMouseOverListener(removeEdgeItem, removeEdgeCallback);
        items.add(removeEdgeItem);

        /* remove/add order */
        final MyMenuItem removeOrderItem =
            new MyMenuItem(Tools.getString("ClusterBrowser.Hb.RemoveOrder"),
                ClusterBrowser.REMOVE_ICON,
                Tools.getString("ClusterBrowser.Hb.RemoveOrder.ToolTip"),

                Tools.getString("ClusterBrowser.Hb.AddOrder"),
                null,
                Tools.getString("ClusterBrowser.Hb.AddOrder.ToolTip"),
                ConfigData.AccessType.ADMIN,
                ConfigData.AccessType.OP) {
            private static final long serialVersionUID = 1L;

            public boolean predicate() {
                return getBrowser().getHeartbeatGraph().isOrder(thisClass);
            }

            public boolean enablePredicate() {
                return !getBrowser().clStatusFailed();
            }

            public void action() {
                if (this.getText().equals(Tools.getString(
                                       "ClusterBrowser.Hb.RemoveOrder"))) {
                    getBrowser().getHeartbeatGraph().removeOrder(
                                                     thisClass,
                                                     getBrowser().getDCHost(),
                                                     testOnly);
                } else {
                    /* there is colocation constraint so let's get the
                     * endpoints from it. */
                    addOrder(null,
                             getLastServiceInfoRsc(),
                             getLastServiceInfoWithRsc());
                    getBrowser().getHeartbeatGraph().addOrder(
                                                  thisClass,
                                                  getBrowser().getDCHost(),
                                                  testOnly);
                }
            }
        };

        final ClusterBrowser.ClMenuItemCallback removeOrderCallback =
                 getBrowser().new ClMenuItemCallback(removeOrderItem, null) {
            public final boolean isEnabled() {
                return super.isEnabled() && !isNew();
            }
            public void action(final Host dcHost) {
                if (!isNew()) {
                    if (getBrowser().getHeartbeatGraph().isOrder(thisClass)) {
                        getBrowser().getHeartbeatGraph().removeOrder(
                                                     thisClass,
                                                     dcHost,
                                                     true);
                    } else {
                        /* there is colocation constraint so let's get the
                         * endpoints from it. */
                        addOrder(null,
                                 getLastServiceInfoRsc(),
                                 getLastServiceInfoWithRsc());
                        getBrowser().getHeartbeatGraph().addOrder(
                                                      thisClass,
                                                      dcHost,
                                                      true);
                    }
                }
            }
        };
        addMouseOverListener(removeOrderItem, removeOrderCallback);
        items.add(removeOrderItem);

        /* remove/add colocation */
        final MyMenuItem removeColocationItem =
                new MyMenuItem(
                    Tools.getString("ClusterBrowser.Hb.RemoveColocation"),
                    ClusterBrowser.REMOVE_ICON,
                    Tools.getString(
                            "ClusterBrowser.Hb.RemoveColocation.ToolTip"),

                    Tools.getString("ClusterBrowser.Hb.AddColocation"),
                    null,
                    Tools.getString(
                            "ClusterBrowser.Hb.AddColocation.ToolTip"),
                    ConfigData.AccessType.ADMIN,
                    ConfigData.AccessType.OP) {
            private static final long serialVersionUID = 1L;

            public boolean predicate() {
                return getBrowser().getHeartbeatGraph().isColocation(thisClass);
            }

            public boolean enablePredicate() {
                return !getBrowser().clStatusFailed();
            }

            public void action() {
                if (this.getText().equals(Tools.getString(
                                  "ClusterBrowser.Hb.RemoveColocation"))) {
                    getBrowser().getHeartbeatGraph().removeColocation(
                                                   thisClass,
                                                   getBrowser().getDCHost(),
                                                   testOnly);
                } else {
                    /* add colocation */
                    /* there is order constraint so let's get the endpoints
                     * from it. */
                    addColocation(null,
                                  getLastServiceInfoParent(),
                                  getLastServiceInfoChild());
                    getBrowser().getHeartbeatGraph().addColocation(
                                                   thisClass,
                                                   getBrowser().getDCHost(),
                                                   testOnly);
                }
            }
        };

        final ClusterBrowser.ClMenuItemCallback removeColocationCallback =
            getBrowser().new ClMenuItemCallback(removeColocationItem, null) {

            public final boolean isEnabled() {
                return super.isEnabled() && !isNew();
            }
            public final void action(final Host dcHost) {
                if (!isNew()) {
                    if (getBrowser().getHeartbeatGraph().isColocation(
                                                                thisClass)) {
                        getBrowser().getHeartbeatGraph().removeColocation(
                                                       thisClass,
                                                       dcHost,
                                                       true);
                    } else {
                        /* add colocation */
                        /* there is order constraint so let's get the endpoints
                         * from it. */
                        addColocation(null,
                                      getLastServiceInfoParent(),
                                      getLastServiceInfoChild());
                        getBrowser().getHeartbeatGraph().addColocation(
                                                       thisClass,
                                                       dcHost,
                                                       true);
                    }
                }
            }
        };
        addMouseOverListener(removeColocationItem, removeColocationCallback);
        items.add(removeColocationItem);
        return items;
    }

    /**
     * Removes colocations or orders.
     */
    private void removeOrdersOrColocations(final boolean isOrder) {
        final List<HbConstraintInterface> constraintsToRemove =
                                    new ArrayList<HbConstraintInterface>();
        boolean changed = false;
        try {
            mConstraintsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
        mConstraintsLock.release();
        infoPanel = null;
        if (changed) {
            selectMyself();
        }
    }

    /**
     * Removes all orders.
     */
    public final void removeOrders() {
        removeOrdersOrColocations(true);
    }

    /**
     * Removes all colocations.
     */
    public final void removeColocations() {
        removeOrdersOrColocations(false);
    }

    /**
     * Adds a new order.
     */
    public final void addOrder(final String ordId,
                               final ServiceInfo serviceInfoParent,
                               final ServiceInfo serviceInfoChild) {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
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
        oi.applyButton = applyButton;
        orderIds.put(ordId, oi);
        oi.getService().setHeartbeatId(ordId);
        oi.setParameters();
        try {
            mConstraintsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        constraints.add(oi);
        mConstraintsLock.release();
        infoPanel = null;
        selectMyself();
    }

    /**
     * Adds a new colocation.
     */
    public final void addColocation(final String colId,
                                    final ServiceInfo serviceInfoRsc,
                                    final ServiceInfo serviceInfoWithRsc) {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
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
        ci.applyButton = applyButton;
        colocationIds.put(colId, ci);
        ci.getService().setHeartbeatId(colId);
        ci.setParameters();
        try {
            mConstraintsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        constraints.add(ci);
        mConstraintsLock.release();
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
        for (final String colId : colocationIds.keySet()) {
            final HbColocationInfo hbci = colocationIds.get(colId);
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
        if (plusInf && minusInf) {
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

    /**
     * Returns whether the order score is negative.
     */
    public final boolean isOrdScoreNull(final ServiceInfo first,
                                        final ServiceInfo then) {
        if (isNew()) {
            return false;
        }
        int score = 0;
        for (final String ordId : orderIds.keySet()) {
            final HbOrderInfo hoi = orderIds.get(ordId);
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

    /**
     * Removes this connection.
     */
    public final void removeMyself(final boolean testOnly) {
        super.removeMyself(testOnly);
    }

    /**
     * Selects the node in the menu and reloads everything underneath.
     */
    public final void selectMyself() {
        super.selectMyself();
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        getBrowser().getTree().getLastSelectedPathComponent();
        final Info prev = (Info) node.getUserObject();
        // TODO: do this differently, don't need to select it, only reload
        getBrowser().setRightComponentInView(this); /* just to reload */
        getBrowser().setRightComponentInView(prev); /* back were we were */
    }

    /** Returns whether this parameter is advanced. */
    protected final boolean isAdvanced(final String param) {
        return false;
    }
    /** Returns access type of this parameter. */
    protected final ConfigData.AccessType getAccessType(final String param) {
        return ConfigData.AccessType.ADMIN;
    }

    /**
     * Hide/Show advanced panels.
     */
    public final void updateAdvancedPanels() {
        super.updateAdvancedPanels();
        try {
            mConstraintsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final HbConstraintInterface c : constraints) {
            c.updateAdvancedPanels();
        }
        mConstraintsLock.release();
    }

    /**
     * Returns whether this resource is resource 1 in colocation constraint.
     */
    public final boolean isWithRsc(final ServiceInfo si) {
        try {
            mConstraintsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final HbConstraintInterface c : constraints) {
            if (!c.isOrder()) {
                ServiceInfo rsc2 = ((HbColocationInfo) c).getRscInfo2();
                final GroupInfo gi = rsc2.getGroupInfo();
                if (gi != null) {
                    rsc2 = gi;
                }
                if (rsc2.equals(si)) {
                    mConstraintsLock.release();
                    return true;
                }
            }
        }
        mConstraintsLock.release();
        return true;
    }

    /**
     * Returns whether colocation or order have at least two different
     * directions.
     */
    private boolean isTwoDirections(final boolean isOrder) {
        ServiceInfo allRsc1 = null;
        ServiceInfo allRsc2 = null;
        try {
            mConstraintsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
                    mConstraintsLock.release();
                    return true;
                }
                if (allRsc2 == null) {
                    allRsc2 = rsc2;
                } else if (!rsc2.equals(allRsc2)) {
                    mConstraintsLock.release();
                    return true;
                }
            }
        }
        mConstraintsLock.release();
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
    public final boolean hasColocationOrOrder(final ServiceInfo si) {
        try {
            mConstraintsLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (final HbConstraintInterface c : constraints) {
            final ServiceInfo rsc1 = c.getRscInfo1();
            final ServiceInfo rsc2 = c.getRscInfo2();
            if (si.equals(rsc1) || si.equals(rsc2)) {
                mConstraintsLock.release();
                return true;
            }
        }
        mConstraintsLock.release();
        return false;
    }
}
