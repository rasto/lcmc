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
import drbd.data.Host;
import drbd.data.CRMXML;
import drbd.data.ClusterStatus;
import drbd.data.resources.Service;
import drbd.data.ConfigData;
import drbd.utilities.CRM;
import drbd.utilities.Tools;
import drbd.utilities.UpdatableItem;
import drbd.utilities.MyMenuItem;

import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Component;
import java.awt.BorderLayout;

/**
 * Object that holds an order constraint information.
 */
public class ConstraintPHInfo extends ServiceInfo {
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Name of this object. */
    public final static String NAME = "PH";
    /** Resource set connection data for colocation. */
    private CRMXML.RscSetConnectionData rscSetConnectionDataCol = null;
    /** Resource set connection data for order. */
    private CRMXML.RscSetConnectionData rscSetConnectionDataOrd = null;

    /**
     * Prepares a new <code>ConstraintPHInfo</code> object.
     */
    public ConstraintPHInfo(
                    final Browser browser,
                    final CRMXML.RscSetConnectionData rscSetConnectionData) {
        super(NAME, null, browser);
        if (rscSetConnectionData != null) {
            if (rscSetConnectionData.isColocation()) {
                this.rscSetConnectionDataCol = rscSetConnectionData;
            } else {
                this.rscSetConnectionDataOrd = rscSetConnectionData;
            }
        }
        //setResource(new Service("constraintplaceholder"));
    }

    /** Returns resource set colocation data. */
    public final CRMXML.RscSetConnectionData getRscSetConnectionDataCol() {
        return rscSetConnectionDataCol;
    }

    /** Returns resource set order data. */
    public final CRMXML.RscSetConnectionData getRscSetConnectionDataOrd() {
        return rscSetConnectionDataOrd;
    }

    /** Sets resource set connection data. */
    public final void setRscSetConnectionData(
                    final CRMXML.RscSetConnectionData rscSetConnectionData) {
        if (rscSetConnectionData.isColocation()) {
            this.rscSetConnectionDataCol = rscSetConnectionData;
        } else {
            this.rscSetConnectionDataOrd = rscSetConnectionData;
        }
    }

    /** Resets resource set connection data, setting the other one to null. */
    public final void resetRscSetConnectionData(
                    final CRMXML.RscSetConnectionData rscSetConnectionData) {
        if (rscSetConnectionData.isColocation()) {
            this.rscSetConnectionDataCol = rscSetConnectionData;
            this.rscSetConnectionDataOrd = null;
        } else {
            this.rscSetConnectionDataOrd = rscSetConnectionData;
            this.rscSetConnectionDataCol = null;
        }
    }

    ///** Returns browser object of this info. */
    //protected final ClusterBrowser getBrowser() {
    //    return (ClusterBrowser) super.getBrowser();
    //}


    /** Sets the order's parameters. */
    public final void setParameters() {
    //    final ClusterStatus clStatus = getBrowser().getClusterStatus();
    //    final String ordId = getService().getHeartbeatId();
    //    final CRMXML.OrderData orderData = clStatus.getOrderData(ordId);

    //    final String score = orderData.getScore();
    //    final String symmetrical = orderData.getSymmetrical();
    //    final String firstAction = orderData.getFirstAction();
    //    final String thenAction = orderData.getThenAction();

    //    final Map<String, String> resourceNode = new HashMap<String, String>();
    //    resourceNode.put(CRMXML.SCORE_STRING, score);
    //    resourceNode.put("symmetrical", symmetrical);
    //    resourceNode.put("first-action", firstAction);
    //    resourceNode.put("then-action", thenAction);

    //    final String[] params = getBrowser().getCRMXML().getOrderParameters();
    //    if (params != null) {
    //        for (String param : params) {
    //            String value = resourceNode.get(param);
    //            if (value == null) {
    //                value = getParamDefault(param);
    //            }
    //            if ("".equals(value)) {
    //                value = null;
    //            }
    //            final String oldValue = getParamSaved(param);
    //            if ((value == null && value != oldValue)
    //                || (value != null && !value.equals(oldValue))) {
    //                getResource().setValue(param, value);
    //            }
    //        }
    //    }
    }

    /**
     * Returns long description of the parameter, that is used for
     * tool tips.
     */
    protected final String getParamLongDesc(final String param) {
    //    final String text =
    //                    getBrowser().getCRMXML().getOrderParamLongDesc(param);
    //    return text.replaceAll("@FIRST-RSC@", serviceInfoParent.toString())
    //               .replaceAll("@THEN-RSC@", serviceInfoChild.toString());
        return null;
    }

    /**
     * Returns short description of the parameter, that is used as * label.
     */
    protected final String getParamShortDesc(final String param) {
        //return getBrowser().getCRMXML().getOrderParamShortDesc(param);
        return null;
    }

    /**
     * Checks if the new value is correct for the parameter type and
     * constraints.
     */
    protected final boolean checkParam(final String param,
                                       final String newValue) {
        //return getBrowser().getCRMXML().checkOrderParam(param, newValue);
        return true;
    }

    /**
     * Returns default for this parameter.
     */
    protected final String getParamDefault(final String param) {
        //return getBrowser().getCRMXML().getOrderParamDefault(param);
        return "default";
    }

    /**
     * Returns preferred value for this parameter.
     */
    protected final String getParamPreferred(final String param) {
        //return getBrowser().getCRMXML().getOrderParamPreferred(param);
        return null;
    }

    /**
     * Returns lsit of all parameters as an array.
     */
    public final String[] getParametersFromXML() {
        //return getBrowser().getCRMXML().getOrderParameters();
        return new String[]{};
    }

    /**
     * Possible choices for pulldown menus, or null if it is not a pull
     * down menu.
     */
    protected final Object[] getParamPossibleChoices(final String param) {
        //if ("first-action".equals(param)) {
        //    return getBrowser().getCRMXML().getOrderParamPossibleChoices(
        //                        param,
        //                        serviceInfoParent.getService().isMaster());
        //} else if ("then-action".equals(param)) {
        //    return getBrowser().getCRMXML().getOrderParamPossibleChoices(
        //                        param,
        //                        serviceInfoChild.getService().isMaster());
        //} else {
        //    return getBrowser().getCRMXML().getOrderParamPossibleChoices(param,
        //                                                                 false);
        //}
        return null;
    }

    /**
     * Returns parameter type, boolean etc.
     */
    protected final String getParamType(final String param) {
        //return getBrowser().getCRMXML().getOrderParamType(param);
        return null;
    }

    /**
     * Returns section to which the global belongs.
     */
    protected final String getSection(final String param) {
        //return getBrowser().getCRMXML().getOrderSection(param);
        return null;
    }

    /**
     * Returns whether the parameter is of the boolean type and needs the
     * checkbox.
     */
    protected final boolean isCheckBox(final String param) {
        //return getBrowser().getCRMXML().isOrderBoolean(param);
        return false;
    }

    /**
     * Returns true if the specified parameter is of time type.
     */
    protected final boolean isTimeType(final String param) {
        //return getBrowser().getCRMXML().isOrderTimeType(param);
        return false;
    }

    /**
     * Returns true if the specified parameter is integer.
     */
    protected final boolean isInteger(final String param) {
        //return getBrowser().getCRMXML().isOrderInteger(param);
        return false;
    }

    /**
     * Returns true if the specified parameter is required.
     */
    protected final boolean isRequired(final String param) {
        //return getBrowser().getCRMXML().isOrderRequired(param);
        return true;
    }

    /**
     * Checks resource fields of all constraints that are in this
     * connection with this constraint.
     */
    public final boolean checkResourceFields(final String param,
                                             final String[] params) {
        //return connectionInfo.checkResourceFields(param, null);
        return true;
    }

    /**
     * Applies changes to the order parameters.
     */
    public final void apply(final Host dcHost, final boolean testOnly) {
        final String[] params = getParametersFromXML();
        //final Map<String, String> attrs = new LinkedHashMap<String, String>();
        //boolean changed = false;
        //for (final String param : params) {
        //    final String value = getComboBoxValue(param);
        //    if (!value.equals(getParamSaved(param))) {
        //        changed = true;
        //    }
        //    if (!value.equals(getParamDefault(param))) {
        //        attrs.put(param, value);
        //    }
        //}
        //if (changed) {
        //    CRM.addOrder(dcHost,
        //                 getService().getHeartbeatId(),
        //                 serviceInfoParent.getHeartbeatId(testOnly),
        //                 serviceInfoChild.getHeartbeatId(testOnly),
        //                 attrs,
        //                 testOnly);
        //}
        //if (!testOnly) {
        //    storeComboBoxValues(params);
        //    checkResourceFields(null, params);
        //}
    }

    ///**
    // * Returns service that belongs to this info object.
    // */
    //public final Service getService() {
    //    return (Service) getResource();
    //}

    /** Returns whether this parameter is advanced. */
    protected final boolean isAdvanced(final String param) {
        return true;
    }

    /** Returns access type of this parameter. */
    protected final ConfigData.AccessType getAccessType(final String param) {
        return ConfigData.AccessType.ADMIN;
    }

    /** Returns name of this placeholder. */
    public final String toString() {
        return getName() +  "(" + getService().getId() + ")";
    }

    public final JComponent getInfoPanel() {
        if (infoPanel != null) {
            return infoPanel;
        }
        //final ButtonCallback buttonCallback = new ButtonCallback() {
        //    private volatile boolean mouseStillOver = false;

        //    /**
        //     * Whether the whole thing should be enabled.
        //     */
        //    public final boolean isEnabled() {
        //        final Host dcHost = getBrowser().getDCHost();
        //        if (dcHost == null) {
        //            return false;
        //        }
        //        final String pmV = dcHost.getPacemakerVersion();
        //        final String hbV = dcHost.getHeartbeatVersion();
        //        if (pmV == null
        //            && hbV != null
        //            && Tools.compareVersions(hbV, "2.1.4") <= 0) {
        //            return false;
        //        }
        //        return true;
        //    }

        //    public final void mouseOut() {
        //        if (!isEnabled()) {
        //            return;
        //        }
        //        mouseStillOver = false;
        //        getBrowser().getHeartbeatGraph().stopTestAnimation(applyButton);
        //        applyButton.setToolTipText(null);
        //    }

        //    public final void mouseOver() {
        //        if (!isEnabled()) {
        //            return;
        //        }
        //        mouseStillOver = true;
        //        applyButton.setToolTipText(
        //                                ClusterBrowser.STARTING_PTEST_TOOLTIP);
        //        applyButton.setToolTipBackground(Tools.getDefaultColor(
        //                            "ClusterBrowser.Test.Tooltip.Background"));
        //        Tools.sleep(250);
        //        if (!mouseStillOver) {
        //            return;
        //        }
        //        mouseStillOver = false;
        //        final CountDownLatch startTestLatch = new CountDownLatch(1);
        //        getBrowser().getHeartbeatGraph().startTestAnimation(
        //                                                    applyButton,
        //                                                    startTestLatch);
        //        final Host dcHost = getBrowser().getDCHost();
        //        getBrowser().ptestLockAcquire();
        //        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        //        clStatus.setPtestData(null);
        //        apply(dcHost, true);
        //        final PtestData ptestData = new PtestData(CRM.getPtest(dcHost));
        //        applyButton.setToolTipText(ptestData.getToolTip());
        //        clStatus.setPtestData(ptestData);
        //        getBrowser().ptestLockRelease();
        //        startTestLatch.countDown();
        //    }
        //};
        //initApplyButton(buttonCallback);
        //for (final String col : colocationIds.keySet()) {
        //    colocationIds.get(col).applyButton = applyButton;
        //}
        //for (final String ord : orderIds.keySet()) {
        //    orderIds.get(ord).applyButton = applyButton;
        //}
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
        //EditableInfo firstConstraint = null;
        //for (final HbConstraintInterface c : constraints) {
        //    if (firstConstraint == null) {
        //        firstConstraint = (EditableInfo) c;
        //    }
        //    final String[] params = c.getParametersFromXML();
        //    /* heartbeat id */
        //    final JPanel panel = getParamPanel(c.getName());
        //    panel.setLayout(new SpringLayout());
        //    final int rows = 3;
        //    c.addLabelField(panel,
        //                    Tools.getString("ClusterBrowser.HeartbeatId"),
        //                    c.getService().getHeartbeatId(),
        //                    ClusterBrowser.SERVICE_LABEL_WIDTH,
        //                    ClusterBrowser.SERVICE_FIELD_WIDTH,
        //                    height);
        //    c.addLabelField(panel,
        //                    c.getRsc1Name(),
        //                    c.getRsc1(),
        //                    ClusterBrowser.SERVICE_LABEL_WIDTH,
        //                    ClusterBrowser.SERVICE_FIELD_WIDTH,
        //                    height);
        //    c.addLabelField(panel,
        //                    c.getRsc2Name(),
        //                    c.getRsc2(),
        //                    ClusterBrowser.SERVICE_LABEL_WIDTH,
        //                    ClusterBrowser.SERVICE_FIELD_WIDTH,
        //                    height);
        //    SpringUtilities.makeCompactGrid(panel, rows, 2, /* rows, cols */
        //                                    1, 1,        /* initX, initY */
        //                                    1, 1);       /* xPad, yPad */

        //    optionsPanel.add(panel);
        //    c.addParams(optionsPanel,
        //                params,
        //                ClusterBrowser.SERVICE_LABEL_WIDTH,
        //                ClusterBrowser.SERVICE_FIELD_WIDTH,
        //                null);
        //}

        //applyButton.addActionListener(
        //    new ActionListener() {
        //        public void actionPerformed(final ActionEvent e) {
        //            final Thread thread = new Thread(new Runnable() {
        //                public void run() {
        //                    getBrowser().clStatusLock();
        //                    apply(getBrowser().getDCHost(), false);
        //                    getBrowser().clStatusUnlock();
        //                }
        //            });
        //            thread.start();
        //        }
        //    }
        //);

        /* apply button */
        //addApplyButton(buttonPanel);
        //applyButton.setEnabled(checkResourceFields(null, null));
        mainPanel.add(optionsPanel);

        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        //if (firstConstraint != null) {
        //newPanel.add(firstConstraint.getMoreOptionsPanel(
        //                          ClusterBrowser.SERVICE_LABEL_WIDTH
        //                          + ClusterBrowser.SERVICE_FIELD_WIDTH + 4));
        //}
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

    /** Returns tool tip for the placeholder. */
    public final String getToolTipText(final boolean testOnly) {
        return "Resource set placeholder";
    }

    /** Return list of popup items. */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final boolean testOnly = false;
        addDependencyMenuItems(items, testOnly);
        /* remove the placeholder and all constraints associated with it. */
        final MyMenuItem removeMenuItem = new MyMenuItem(
                    "Remove",
                    ClusterBrowser.REMOVE_ICON,
                    ClusterBrowser.STARTING_PTEST_TOOLTIP,
                    ConfigData.AccessType.ADMIN,
                    ConfigData.AccessType.OP) {
            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                if (getBrowser().clStatusFailed()
                    || getService().isRemoved()) {
                    return false;
                }
                return true;
            }

            public void action() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getPopup().setVisible(false);
                    }
                });
                removeMyself(false);
                getBrowser().getHeartbeatGraph().repaint();
            }
        };
        final ClusterBrowser.ClMenuItemCallback removeItemCallback =
                getBrowser().new ClMenuItemCallback(removeMenuItem, null) {
            public final boolean isEnabled() {
                return super.isEnabled() && !getService().isNew();
            }
            public final void action(final Host dcHost) {
                removeMyselfNoConfirm(dcHost, true); /* test only */
            }
        };
        addMouseOverListener(removeMenuItem, removeItemCallback);
        items.add((UpdatableItem) removeMenuItem);
        return items;
    }

    /** Removes the placeholder without confirmation dialog. */
    protected final void removeMyselfNoConfirm(final Host dcHost,
                                         final boolean testOnly) {
        if (!testOnly) {
            setUpdated(true);
            getService().setRemoved(true);
        }

        if (getService().isNew()) {
            if (!testOnly) {
                getService().setNew(false);
                getBrowser().getHeartbeatGraph().killRemovedVertices();
            }
        } else {
            //getBrowser().getHeartbeatGraph().removeOrder(
            //                             thisClass,
            //                             getBrowser().getDCHost(),
            //                             true);
        }
        if (!testOnly) {
            getBrowser().removeFromServiceInfoHash(this);
            infoPanel = null;
            getService().doneRemoving();
            //getBrowser().reloadAllComboBoxes(this);
        }
    }

    ///** Returns panel with graph. */
    //public JPanel getGraphicalView() {
    //    return getBrowser().getHeartbeatGraph().getGraphPanel();
    //}

    /** Removes this placeholder from the crm with confirmation dialog. */
    public final void removeMyself(final boolean testOnly) {
        if (getService().isNew()) {
            removeMyselfNoConfirm(getBrowser().getDCHost(), testOnly);
            getService().setNew(false);
            return;
        }
        final String desc = Tools.getString(
                        "ConstraintPHInfo.confirmRemove.Description");

        if (Tools.confirmDialog(
                     Tools.getString("ConstraintPHInfo.confirmRemove.Title"),
                     desc,
                     Tools.getString("ConstraintPHInfo.confirmRemove.Yes"),
                     Tools.getString("ConstraintPHInfo.confirmRemove.No"))) {
            removeMyselfNoConfirm(getBrowser().getDCHost(), testOnly);
            getService().setNew(false);
        }
    }

    /** Sets whether the info object is being updated. */
    public final void setUpdated(final boolean updated) {
        if (updated && !isUpdated()) {
            getBrowser().getHeartbeatGraph().startAnimation(this);
        } else if (!updated) {
            getBrowser().getHeartbeatGraph().stopAnimation(this);
        }
        super.setUpdated(updated);
    }

    /** Whether this class is a constraint placeholder. */
    public final boolean isConstraintPH() {
        return true;
    }
}
