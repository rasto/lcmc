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
import drbd.data.ConfigData;
import drbd.data.Subtext;
import drbd.data.AccessMode;
import drbd.utilities.CRM;
import drbd.utilities.Tools;
import drbd.utilities.UpdatableItem;
import drbd.utilities.MyMenuItem;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;

import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * Object that holds an order constraint information.
 */
public class ConstraintPHInfo extends ServiceInfo {
    /** Name of this object. */
    public static final String NAME = "Placeholder";
    /** Resource set connection data for colocation. */
    private CRMXML.RscSetConnectionData rscSetConnectionDataCol = null;
    /** Resource set connection data for order. */
    private CRMXML.RscSetConnectionData rscSetConnectionDataOrd = null;
    /** Whether the direction of colocation should be reversed, meaning it is
     * from this placeholder, when it was new. */
    private boolean reverseCol = false;
    /** Whether the direction of order should be reversed, meaning it is from
     * this placeholder, when it was new. */
    private boolean reverseOrd = false;
    /** Whether the colocation was reversed. */
    private boolean reversedCol = false;
    /** Whether the order was reversed. */
    private boolean reversedOrd = false;
    /** Resource set info object for this placeholder. More placeholders can
     * have on resource set info object. */
    private PcmkRscSetsInfo pcmkRscSetsInfo = null;
    /** Rsc set info object lock. */
    private final Mutex mPcmkRscSetsLock = new Mutex();

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
    }

    /** Returns resource set colocation data. */
    public final CRMXML.RscSetConnectionData getRscSetConnectionDataCol() {
        return rscSetConnectionDataCol;
    }

    /** Returns resource set order data. */
    public final CRMXML.RscSetConnectionData getRscSetConnectionDataOrd() {
        return rscSetConnectionDataOrd;
    }


    /** Sets connection data to zero. */
    public final void resetRscSetConnectionData() {
        final CRMXML.RscSetConnectionData rodata = rscSetConnectionDataOrd;
        if (rodata != null && rodata.isEmpty()) {
            rscSetConnectionDataOrd = null;
        }
        final CRMXML.RscSetConnectionData rcdata = rscSetConnectionDataCol;
        if (rcdata != null && rcdata.isEmpty()) {
            rscSetConnectionDataCol = null;
        }
    }

    /** Sets resource set connection data. */
    public final void setRscSetConnectionData(
                    final CRMXML.RscSetConnectionData rscSetConnectionData) {
        if (rscSetConnectionData.isColocation()) {
            if (reverseCol) {
                if (rscSetConnectionData.getRscSet2() == null
                    && rscSetConnectionData.getRscSet1() != null) {
                    reversedCol = true;
                    rscSetConnectionData.reverse();
                    reverseCol = false;
                }
            } else if (rscSetConnectionDataCol != null) {
                if (rscSetConnectionData.getRscSet2() == null
                    && rscSetConnectionData.getRscSet1() != null
                    && rscSetConnectionDataCol.getRscSet2() != null
                    && (rscSetConnectionData.getRscSet1().isSubsetOf(
                                     rscSetConnectionDataCol.getRscSet2())
                        || rscSetConnectionDataCol.getRscSet2().isSubsetOf(
                                     rscSetConnectionData.getRscSet1()))) {
                    /* upside down */
                    reversedCol = true;
                    rscSetConnectionData.reverse();
                }
            } else {
                reversedCol = false;
            }
            this.rscSetConnectionDataCol = rscSetConnectionData;
        } else {
            if (reverseOrd) {
                if (rscSetConnectionData.getRscSet2() == null
                    && rscSetConnectionData.getRscSet1() != null) {
                    Tools.debug(this, "force reverse ord", 3);
                    Tools.debug(this,
                                " data rscset1: "
                                + rscSetConnectionData.getRscSet1().getRscIds(),
                                3);
                    reversedOrd = true;
                    rscSetConnectionData.reverse();
                    reverseOrd = false;
                }
            } else if (rscSetConnectionDataOrd != null) {
                if (rscSetConnectionData.getRscSet2() == null
                    && rscSetConnectionData.getRscSet1() != null
                    && rscSetConnectionDataOrd.getRscSet2() != null
                    && (rscSetConnectionData.getRscSet1().isSubsetOf(
                                     rscSetConnectionDataOrd.getRscSet2())
                        || rscSetConnectionDataOrd.getRscSet2().isSubsetOf(
                                     rscSetConnectionData.getRscSet1()))) {
                    Tools.debug(this, "data rscset1: "
                                      + rscSetConnectionData.getRscSet1(), 3);
                    if (rscSetConnectionData.getRscSet1() != null) {
                        Tools.debug(
                            this,
                            "data rscset1 ids: "
                            + rscSetConnectionData.getRscSet1().getRscIds(),
                            3);
                    }

                    Tools.debug(this, "data rscset2: "
                                      + rscSetConnectionData.getRscSet2(), 3);
                    if (rscSetConnectionData.getRscSet2() != null) {
                        Tools.debug(
                              this,
                              "data rscset2 ids: "
                              + rscSetConnectionData.getRscSet2().getRscIds(),
                              3);
                    }

                    Tools.debug(this,
                                "ord rscset1: "
                                + rscSetConnectionDataOrd.getRscSet1(),
                                3);
                    if (rscSetConnectionDataOrd.getRscSet1() != null) {
                        Tools.debug(
                           this,
                           "ord rscset1 ids: "
                           + rscSetConnectionDataOrd.getRscSet1().getRscIds(),
                           3);
                    }

                    Tools.debug(this,
                                "ord rscset2: "
                                + rscSetConnectionDataOrd.getRscSet2(),
                                3);
                    if (rscSetConnectionDataOrd.getRscSet2() != null) {
                        Tools.debug(
                           this,
                           "ord rscset2 ids: "
                           + rscSetConnectionDataOrd.getRscSet2().getRscIds(),
                           3);
                    }
                    Tools.debug(this, "reverse ord", 3);
                    reversedOrd = true;
                    /* upside down */
                    rscSetConnectionData.reverse();
                }
            } else {
                reversedOrd = false;
            }

            this.rscSetConnectionDataOrd = rscSetConnectionData;
        }
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
        return true;
    }

    /**
     * Returns default for this parameter.
     */
    protected final String getParamDefault(final String param) {
        return "default";
    }

    /** Returns preferred value for this parameter. */
    protected final String getParamPreferred(final String param) {
        return null;
    }

    /** Returns lsit of all parameters as an array. */
    public final String[] getParametersFromXML() {
        return new String[]{};
    }

    /**
     * Possible choices for pulldown menus, or null if it is not a pull
     * down menu.
     */
    protected final Object[] getParamPossibleChoices(final String param) {
        return null;
    }

    /** Returns parameter type, boolean etc. */
    protected final String getParamType(final String param) {
        return null;
    }

    /** Returns section to which the global belongs. */
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

    /** Returns true if the specified parameter is of time type. */
    protected final boolean isTimeType(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is integer. */
    protected final boolean isInteger(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is required. */
    protected final boolean isRequired(final String param) {
        return true;
    }

    /**
     * Checks resource fields of all constraints that are in this
     * connection with this constraint.
     */
    public final boolean checkResourceFields(final String param,
                                             final String[] params) {
        return true;
    }

    /** Applies changes to the placeholder. */
    public final void apply(final Host dcHost, final boolean testOnly) {
        /* apply is in resource set info object. */
    }

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
        return getName() +  " (" + getService().getId() + ")";
    }

    /**
     * Returns id of the service, which is heartbeatId.
     * TODO: this id is used for stored position info, should be named
     * differently.
     */
    public final String getId() {
        String ordId = "";
        String colId = "";
        final CRMXML.RscSetConnectionData rodata = rscSetConnectionDataOrd;
        if (rodata != null) {
            ordId = rodata.getConstraintId() + "-" + rodata.getConnectionPos();
        }
        final CRMXML.RscSetConnectionData rcdata = rscSetConnectionDataCol;
        if (rcdata != null) {
            colId = rcdata.getConstraintId() + "-" + rcdata.getConnectionPos();
        }
        return "ph_" + ordId + "_" + colId;
    }

    /** Return information panel. */
    public final JComponent getInfoPanel() {
        try {
            mPcmkRscSetsLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final PcmkRscSetsInfo prsi = pcmkRscSetsInfo;
        mPcmkRscSetsLock.release();
        return prsi.getInfoPanel(this);
    }

    /** Returns tool tip for the placeholder. */
    public final String getToolTipText(final boolean testOnly) {
        return "Resource set placeholder";
    }

    /** Return list of popup items. */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final boolean testOnly = false;
        addDependencyMenuItems(items, true, testOnly);
        /* remove the placeholder and all constraints associated with it. */
        final MyMenuItem removeMenuItem = new MyMenuItem(
                    "Remove",
                    ClusterBrowser.REMOVE_ICON,
                    ClusterBrowser.STARTING_PTEST_TOOLTIP,
                    new AccessMode(ConfigData.AccessType.ADMIN, false),
                    new AccessMode(ConfigData.AccessType.OP, false)) {
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
        if (getService().isNew()) {
            if (!testOnly) {
                setUpdated(true);
                getService().setRemoved(true);
                final HbConnectionInfo[] hbcis =
                       getBrowser().getHeartbeatGraph().getHbConnections(this);
                for (final HbConnectionInfo hbci : hbcis) {
                    getBrowser().getHeartbeatGraph().removeOrder(hbci,
                                                                 dcHost,
                                                                 testOnly);
                    getBrowser().getHeartbeatGraph().removeColocation(hbci,
                                                                      dcHost,
                                                                      testOnly);
                }
                getService().setNew(false);
                getBrowser().removeFromServiceInfoHash(this);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getBrowser().getHeartbeatGraph().killRemovedVertices();
                        getService().doneRemoving();
                    }
                });
            }
        } else {
            if (rscSetConnectionDataOrd != null) {
                final String ordId = rscSetConnectionDataOrd.getConstraintId();
                if (ordId != null) {
                    CRM.removeOrder(dcHost, ordId, testOnly);
                }
            }
            if (rscSetConnectionDataCol != null) {
                final String colId = rscSetConnectionDataCol.getConstraintId();
                if (colId != null) {
                    CRM.removeColocation(dcHost, colId, testOnly);
                }
            }
            if (!testOnly) {
                setUpdated(true);
                getService().setRemoved(true);
                getBrowser().removeFromServiceInfoHash(this);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getBrowser().getHeartbeatGraph().killRemovedVertices();
                        getService().doneRemoving();
                    }
                });
            }
        }
    }

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

    /** Sets whether the direction of order was reversed, meaning it is from
     * this placeholder, when it was new. */
    public final void reverseOrder() {
        reverseOrd = true;
    }

    /** Sets whether the direction of colocation was reversed, meaning it is
     * from this placeholder, when it was new. */
    public final void reverseColocation() {
        reverseCol = true;
    }

    /** Returns whether the colocation was reversed. */
    public final boolean isReversedCol() {
        return reversedCol;
    }

    /** Returns whether the order was reversed. */
    public final boolean isReversedOrd() {
        return reversedOrd;
    }

    /** Adds constraint to or from placeholder. */
    protected final List<CRMXML.RscSet> addConstraintWithPlaceholder(
                                      final List<ServiceInfo> servicesAll,
                                      final List<ServiceInfo> servicesFrom,
                                      final boolean colocationOnly,
                                      final boolean orderOnly,
                                      final Host dcHost,
                                      final boolean force,
                                      final boolean testOnly) {
        boolean createCol = false;
        boolean createOrd = false;
        String colId = null;
        String ordId = null;
        final CRMXML.RscSetConnectionData rdataCol =
                                                getRscSetConnectionDataCol();
        final CRMXML.RscSetConnectionData rdataOrd =
                                                getRscSetConnectionDataOrd();
        CRMXML.RscSet colRscSet1 = null;
        CRMXML.RscSet colRscSet2 = null;
        CRMXML.RscSet ordRscSet1 = null;
        CRMXML.RscSet ordRscSet2 = null;

        if (rdataCol != null) {
            if (!orderOnly) {
                colId = rdataCol.getConstraintId();
            }
            colRscSet1 = rdataCol.getRscSet1();
            colRscSet2 = rdataCol.getRscSet2();
        }
        if (rdataOrd != null) {
            if (!colocationOnly) {
                ordId = rdataOrd.getConstraintId();
            }
            ordRscSet1 = rdataOrd.getRscSet1();
            ordRscSet2 = rdataOrd.getRscSet2();
        }
        if (servicesFrom.isEmpty()) {
            if (!colocationOnly) {
                reverseOrder();
            }
            if (!orderOnly) {
                reverseColocation();
            }
        }
        CRMXML.RscSet outOrdRscSet1 = null;
        CRMXML.RscSet outOrdRscSet2 = null;
        CRMXML.RscSet outColRscSet1 = null;
        CRMXML.RscSet outColRscSet2 = null;
        for (final ServiceInfo serviceInfo : servicesAll) {
            final boolean isFrom = servicesFrom.contains(serviceInfo);
            final String idToAdd = serviceInfo.getService().getHeartbeatId();
            if (!orderOnly) {
                final ClusterStatus clStatus = getBrowser().getClusterStatus();
                /* colocation */
                if (colId == null) {
                    final List<String> rscIds = new ArrayList<String>();
                    rscIds.add(idToAdd);
                    int colIdInt = Integer.parseInt(getService().getId());
                    colId = "c" + colIdInt;
                    while (clStatus.getRscSetsCol(colId) != null) {
                        colIdInt++;
                        colId = "c" + colIdInt;
                    }
                    createCol = true;
                    if (isFrom) {
                        colRscSet2 = getBrowser().getCRMXML().new RscSet(
                                                       colId,
                                                       new ArrayList<String>(),
                                                       "false",
                                                       null,
                                                       null);
                        colRscSet1 = getBrowser().getCRMXML().new RscSet(
                                                                      colId,
                                                                      rscIds,
                                                                      "false",
                                                                      null,
                                                                      null);
                        outColRscSet1 = colRscSet1;
                    } else {
                        colRscSet2 = getBrowser().getCRMXML().new RscSet(
                                                                      colId,
                                                                      rscIds,
                                                                      "false",
                                                                      null,
                                                                      null);
                        colRscSet1 = getBrowser().getCRMXML().new RscSet(
                                                       colId,
                                                       new ArrayList<String>(),
                                                       "false",
                                                       null,
                                                       null);
                        outColRscSet2 = colRscSet2;
                    }
                } else {
                    boolean colRscSetAdded = false;
                    CRMXML.RscSet toRscSet;
                    if (isFrom) {
                        if (outColRscSet1 == null) {
                            toRscSet = colRscSet1;
                        } else {
                            toRscSet = outColRscSet1;
                        }
                    } else {
                        if (outColRscSet2 == null) {
                            toRscSet = colRscSet2;
                        } else {
                            toRscSet = outColRscSet2;
                        }
                    }
                    final List<CRMXML.RscSet> rscSetsColList =
                                                 clStatus.getRscSetsCol(colId);
                    if (rscSetsColList != null) {
                        for (final CRMXML.RscSet rscSet : rscSetsColList) {
                            if (rscSet.equals(toRscSet)) {
                                final List<String> newRscIds =
                                                       new ArrayList<String>();
                                newRscIds.addAll(rscSet.getRscIds());

                                newRscIds.add(0, idToAdd);
                                final CRMXML.RscSet newRscSet =
                                    getBrowser().getCRMXML().new RscSet(
                                                   rscSet.getId(),
                                                   newRscIds,
                                                   rscSet.getSequential(),
                                                   rscSet.getOrderAction(),
                                                   rscSet.getColocationRole());
                                if (isFrom) {
                                    outColRscSet1 = newRscSet;
                                } else {
                                    outColRscSet2 = newRscSet;
                                }
                                colRscSetAdded = true;
                            } else {
                                if (isFrom) {
                                    outColRscSet2 = rscSet;
                                } else {
                                    outColRscSet1 = rscSet;
                                }
                            }
                        }
                    }
                    if (!colRscSetAdded) {
                        final List<String> newRscIds = new ArrayList<String>();
                                getBrowser().getCRMXML().new RscSet(
                                                   colId,
                                                   new ArrayList<String>(),
                                                   "false",
                                                   null,
                                                   null);
                        final CRMXML.RscSet newRscSet;
                        if (toRscSet == null) {
                            newRscSet =
                                getBrowser().getCRMXML().new RscSet(
                                                   colId,
                                                   newRscIds,
                                                   "false",
                                                   null,
                                                   null);
                        } else {
                            newRscIds.addAll(toRscSet.getRscIds());
                            newRscSet =
                                getBrowser().getCRMXML().new RscSet(
                                               toRscSet.getId(),
                                               newRscIds,
                                               toRscSet.getSequential(),
                                               toRscSet.getOrderAction(),
                                               toRscSet.getColocationRole());
                        }
                        newRscSet.addRscId(idToAdd);
                        if (isFrom) {
                            outColRscSet1 = newRscSet;
                        } else {
                            outColRscSet2 = newRscSet;
                        }
                    }
                }
            }

            if (!colocationOnly) {
                /* order */
                final ClusterStatus clStatus = getBrowser().getClusterStatus();
                if (ordId == null) {
                    final List<String> rscIds = new ArrayList<String>();
                    rscIds.add(idToAdd);
                    int ordIdInt = Integer.parseInt(getService().getId());
                    ordId = "o" + ordIdInt;
                    while (clStatus.getRscSetsOrd(ordId) != null) {
                        ordIdInt++;
                        ordId = "o" + ordIdInt;
                    }
                    createOrd = true;
                    if (isFrom) {
                        ordRscSet1 = getBrowser().getCRMXML().new RscSet(
                                                                      ordId,
                                                                      rscIds,
                                                                      "false",
                                                                      null,
                                                                      null);
                        ordRscSet2 = getBrowser().getCRMXML().new RscSet(
                                                      ordId,
                                                      new ArrayList<String>(),
                                                      "false",
                                                      null,
                                                      null);
                        outOrdRscSet1 = ordRscSet1;
                    } else {
                        ordRscSet1 = getBrowser().getCRMXML().new RscSet(
                                                      ordId,
                                                      new ArrayList<String>(),
                                                      "false",
                                                      null,
                                                      null);
                        ordRscSet2 = getBrowser().getCRMXML().new RscSet(
                                                                      ordId,
                                                                      rscIds,
                                                                      "false",
                                                                      null,
                                                                      null);
                        outOrdRscSet2 = ordRscSet2;
                    }
                } else {
                    boolean ordRscSetAdded = false;
                    CRMXML.RscSet toRscSet;
                    if (isFrom) {
                        if (outOrdRscSet1 == null) {
                            toRscSet = ordRscSet1;
                        } else {
                            toRscSet = outOrdRscSet1;
                        }
                    } else {
                        if (outOrdRscSet2 == null) {
                            toRscSet = ordRscSet2;
                        } else {
                            toRscSet = outOrdRscSet2;
                        }
                    }
                    final List<CRMXML.RscSet> rscSetsOrdList =
                                                 clStatus.getRscSetsOrd(ordId);
                    if (rscSetsOrdList != null) {
                        for (final CRMXML.RscSet rscSet : rscSetsOrdList) {
                            if (rscSet.equals(toRscSet)) {
                                final List<String> newRscIds =
                                                       new ArrayList<String>();
                                newRscIds.addAll(rscSet.getRscIds());

                                newRscIds.add(idToAdd);
                                final CRMXML.RscSet newRscSet =
                                    getBrowser().getCRMXML().new RscSet(
                                                   rscSet.getId(),
                                                   newRscIds,
                                                   rscSet.getSequential(),
                                                   rscSet.getOrderAction(),
                                                   rscSet.getColocationRole());
                                if (isFrom) {
                                    outOrdRscSet1 = newRscSet;
                                } else {
                                    outOrdRscSet2 = newRscSet;
                                }
                                ordRscSetAdded = true;
                            } else {
                                if (isFrom) {
                                    outOrdRscSet2 = rscSet;
                                } else {
                                    outOrdRscSet1 = rscSet;
                                }
                            }
                        }
                    }
                    if (!ordRscSetAdded) {
                        final List<String> newRscIds = new ArrayList<String>();
                        CRMXML.RscSet newRscSet;
                        if (toRscSet == null) {
                            newRscSet =
                                getBrowser().getCRMXML().new RscSet(
                                                   ordId,
                                                   newRscIds,
                                                   "false",
                                                   null,
                                                   null);
                        } else {
                            newRscIds.addAll(toRscSet.getRscIds());
                            newRscSet =
                                getBrowser().getCRMXML().new RscSet(
                                               toRscSet.getId(),
                                               newRscIds,
                                               toRscSet.getSequential(),
                                               toRscSet.getOrderAction(),
                                               toRscSet.getColocationRole());
                        }
                        newRscSet.addRscId(idToAdd);
                        if (isFrom) {
                            outOrdRscSet1 = newRscSet;
                        } else {
                            outOrdRscSet2 = newRscSet;
                        }
                    }
                }
            }
        }
        if (!testOnly) {
            setUpdated(false);
        }
        final List<CRMXML.RscSet> sets = new ArrayList<CRMXML.RscSet>();
        sets.add(outColRscSet2);
        sets.add(outColRscSet1);
        sets.add(outOrdRscSet1);
        sets.add(outOrdRscSet2);
        if (force) {
            final Map<String, String> attrs =
                                          new LinkedHashMap<String, String>();
            attrs.put(CRMXML.SCORE_STRING, CRMXML.INFINITY_STRING);
            final Map<CRMXML.RscSet, Map<String, String>> rscSetsColAttrs =
                       new LinkedHashMap<CRMXML.RscSet, Map<String, String>>();
            final Map<CRMXML.RscSet, Map<String, String>> rscSetsOrdAttrs =
                       new LinkedHashMap<CRMXML.RscSet, Map<String, String>>();

            rscSetsColAttrs.put(outColRscSet2, null);
            rscSetsColAttrs.put(outColRscSet1, null);
            rscSetsOrdAttrs.put(outOrdRscSet1, null);
            rscSetsOrdAttrs.put(outOrdRscSet2, null);
            CRM.setRscSet(dcHost,
                          colId,
                          createCol,
                          ordId,
                          createOrd,
                          rscSetsColAttrs,
                          rscSetsOrdAttrs,
                          attrs,
                          testOnly);
        }
        return sets;
    }

    /** Whether the id of this constraint is the same or there is no id in this
     * object. */
    public final boolean sameConstraintId(
                                final CRMXML.RscSetConnectionData otherRdata) {
        if (otherRdata.isColocation()) {
            final CRMXML.RscSetConnectionData rdataCol =
                                                       rscSetConnectionDataCol;
            return rdataCol == null
                   || rdataCol.getConstraintId() == null
                   || rdataCol.getConstraintId().equals(
                                                 otherRdata.getConstraintId());
        } else {
            final CRMXML.RscSetConnectionData rdataOrd =
                                                       rscSetConnectionDataOrd;
            return rdataOrd == null
                   || rdataOrd.getConstraintId() == null
                   || rdataOrd.getConstraintId().equals(
                                                 otherRdata.getConstraintId());
        }
    }

    /** Sets rsc sets info object. */
    public final void setPcmkRscSetsInfo(
                                     final PcmkRscSetsInfo pcmkRscSetsInfo) {
        try {
            mPcmkRscSetsLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        if (this.pcmkRscSetsInfo != pcmkRscSetsInfo) {
            this.pcmkRscSetsInfo = pcmkRscSetsInfo;
        }
        mPcmkRscSetsLock.release();
    }

    /** Gets rsc sets info object. */
    public final PcmkRscSetsInfo getPcmkRscSetsInfo() {
        try {
            mPcmkRscSetsLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final PcmkRscSetsInfo prsi = pcmkRscSetsInfo;
        mPcmkRscSetsLock.release();
        return prsi;
    }

    /** Hide/Show advanced panels. */
    public final void updateAdvancedPanels() {
        super.updateAdvancedPanels();
        try {
            mPcmkRscSetsLock.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        final PcmkRscSetsInfo prsi = pcmkRscSetsInfo;
        mPcmkRscSetsLock.release();
        if (prsi != null) {
            prsi.updateAdvancedPanels();
        }
    }

    /** Returns whether the placeholder has any connections at all. */
    public final boolean isEmpty() {
        final CRMXML.RscSetConnectionData rdataOrd =
                                            getRscSetConnectionDataOrd();
        final CRMXML.RscSetConnectionData rdataCol =
                                            getRscSetConnectionDataCol();
        return (rdataOrd == null || rdataOrd.isEmpty())
                && (rdataCol == null || rdataCol.isEmpty());
    }

    /** Returns attributes for resource_set tag. */
    public final void getAttributes(
                  final boolean isCol,
                  final boolean first,
                  final Map<CRMXML.RscSet, Map<String, String>> rscSetsAttrs) {
        CRMXML.RscSetConnectionData rscd;
        if (isCol) {
            rscd = rscSetConnectionDataCol;
        } else {
            rscd = rscSetConnectionDataOrd;
        }
        if (rscd == null) {
            return;
        }
        final Map<String, String> attrs = new LinkedHashMap<String, String>();
        CRMXML.RscSet rscSet;
        if (first) {
            rscSet = rscd.getRscSet1();
        } else {
            rscSet = rscd.getRscSet2();
        }
        rscSetsAttrs.put(rscSet, null);
    }

    /** Returns resource that is next in sequence in the resource set. */
    public final ServiceInfo nextInSequence(final ServiceInfo si,
                                            final boolean isCol) {
        CRMXML.RscSetConnectionData rscd;
        if (isCol) {
            rscd = rscSetConnectionDataCol;
        } else {
            rscd = rscSetConnectionDataOrd;
        }
        if (rscd == null) {
            return null;
        }
        for (final CRMXML.RscSet rscSet
                : new CRMXML.RscSet[]{rscd.getRscSet1(), rscd.getRscSet2()}) {
            if (rscSet == null) {
                continue;
            }
            if (!rscSet.isSequential()) {
                continue;
            }
            final List<String> ids = rscSet.getRscIds();
            for (int i = 0; i < ids.size(); i++) {
                if (i < ids.size() - 1
                    && ids.get(i).equals(si.getHeartbeatId(true))) {
                    return getBrowser().getServiceInfoFromCRMId(ids.get(i + 1));
                }
            }
        }
        return null;
    }

    /** Returns resource that is before in sequence in the resource set. */
    public final ServiceInfo prevInSequence(final ServiceInfo si,
                                            final boolean isCol) {
        CRMXML.RscSetConnectionData rscd;
        if (isCol) {
            rscd = rscSetConnectionDataCol;
        } else {
            rscd = rscSetConnectionDataOrd;
        }
        if (rscd == null) {
            return null;
        }
        for (final CRMXML.RscSet rscSet
                : new CRMXML.RscSet[]{rscd.getRscSet1(), rscd.getRscSet2()}) {
            if (rscSet == null) {
                continue;
            }
            if (!rscSet.isSequential()) {
                continue;
            }
            final List<String> ids = rscSet.getRscIds();
            for (int i = ids.size() - 1; i >= 0; i--) {
                if (i > 0 && ids.get(i).equals(si.getHeartbeatId(true))) {
                    return getBrowser().getServiceInfoFromCRMId(ids.get(i - 1));
                }
            }
        }
        return null;
    }

    /** Returns the main text that appears in the graph. */
    public final String getMainTextForGraph() {
        return getService().getId();
    }

    /** Returns text that appears above the icon in the graph. */
    public final String getIconTextForGraph(final boolean testOnly) {
        return "   PH";
    }
    /** Returns text with lines as array that appears in the cluster graph. */
    public final Subtext[] getSubtextsForGraph(final boolean testOnly) {
        return null;
    }
}
