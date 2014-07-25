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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import lcmc.model.Application;
import lcmc.model.crm.CrmXml;
import lcmc.model.crm.ClusterStatus;
import lcmc.model.Host;
import lcmc.model.StringValue;
import lcmc.model.Subtext;
import lcmc.model.Value;
import lcmc.gui.Browser;
import lcmc.utilities.CRM;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

/**
 * Object that holds an order constraint information.
 */
public class ConstraintPHInfo extends ServiceInfo {
    /** Logger. */
    private static final Logger LOG =
                             LoggerFactory.getLogger(ConstraintPHInfo.class);
    /** Name of this object. */
    static final String NAME = "Placeholder";
    /** Name of the "and" constraint placeholder .*/
    private static final String CONSTRAINT_PLACEHOLDER_AND =
                                       Tools.getString("ConstraintPHInfo.And");
    /** Name of the "or" constraint placeholder .*/
    private static final String CONSTRAINT_PLACEHOLDER_OR =
                                       Tools.getString("ConstraintPHInfo.Or");
    /** Resource set connection data for colocation. */
    private CrmXml.RscSetConnectionData rscSetConnectionDataCol = null;
    /** Resource set connection data for order. */
    private CrmXml.RscSetConnectionData rscSetConnectionDataOrd = null;
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
    private volatile PcmkRscSetsInfo pcmkRscSetsInfo = null;
    /** Whether the all resources are required to be started. */
    private final Preference preference;


    /** Prepares a new {@code ConstraintPHInfo} object. */
    ConstraintPHInfo(final Browser browser,
                     final CrmXml.RscSetConnectionData rscSetConnectionData,
                     final Preference preference) {
        super(NAME, null, browser);
        this.preference = preference;
        if (rscSetConnectionData != null) {
            if (rscSetConnectionData.isColocation()) {
                rscSetConnectionDataCol = rscSetConnectionData;
            } else {
                rscSetConnectionDataOrd = rscSetConnectionData;
            }
        }
    }

    /** Returns resource set colocation data. */
    CrmXml.RscSetConnectionData getRscSetConnectionDataCol() {
        return rscSetConnectionDataCol;
    }

    /** Returns resource set order data. */
    CrmXml.RscSetConnectionData getRscSetConnectionDataOrd() {
        return rscSetConnectionDataOrd;
    }


    /** Sets connection data to zero. */
    public void resetRscSetConnectionData() {
        final CrmXml.RscSetConnectionData rodata = rscSetConnectionDataOrd;
        if (rodata != null && rodata.isEmpty()) {
            rscSetConnectionDataOrd = null;
        }
        final CrmXml.RscSetConnectionData rcdata = rscSetConnectionDataCol;
        if (rcdata != null && rcdata.isEmpty()) {
            rscSetConnectionDataCol = null;
        }
    }

    /** Sets resource set connection data. */
    void setRscSetConnectionData(
                    final CrmXml.RscSetConnectionData rscSetConnectionData) {
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
            rscSetConnectionDataCol = rscSetConnectionData;
        } else {
            if (reverseOrd) {
                if (rscSetConnectionData.getRscSet2() == null
                    && rscSetConnectionData.getRscSet1() != null) {
                    LOG.trace("setRscSetConnectionData: force reverse ord");
                    LOG.trace("setRscSetConnectionData: data rscset1: "
                              + rscSetConnectionData.getRscSet1().getRscIds());
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
                    LOG.trace("setRscSetConnectionData: data rscset1: "
                              + rscSetConnectionData.getRscSet1());
                    if (rscSetConnectionData.getRscSet1() != null) {
                        LOG.trace(
                               "setRscSetConnectionData: data rscset1 ids: "
                               + rscSetConnectionData.getRscSet1().getRscIds());
                    }

                    LOG.trace("setRscSetConnectionData: data rscset2: "
                              + rscSetConnectionData.getRscSet2());
                    if (rscSetConnectionData.getRscSet2() != null) {
                        LOG.trace(
                               "setRscSetConnectionData: data rscset2 ids: "
                               + rscSetConnectionData.getRscSet2().getRscIds());
                    }

                    LOG.trace("setRscSetConnectionData: ord rscset1: "
                              + rscSetConnectionDataOrd.getRscSet1());
                    if (rscSetConnectionDataOrd.getRscSet1() != null) {
                        LOG.trace(
                            "setRscSetConnectionData: ord rscset1 ids: "
                            + rscSetConnectionDataOrd.getRscSet1().getRscIds());
                    }

                    LOG.trace("setRscSetConnectionData: ord rscset2: "
                              + rscSetConnectionDataOrd.getRscSet2());
                    if (rscSetConnectionDataOrd.getRscSet2() != null) {
                        LOG.trace(
                            "setRscSetConnectionData: ord rscset2 ids: "
                            + rscSetConnectionDataOrd.getRscSet2().getRscIds());
                    }
                    LOG.trace("setRscSetConnectionData: reverse ord");
                    reversedOrd = true;
                    /* upside down */
                    rscSetConnectionData.reverse();
                }
            } else {
                reversedOrd = false;
            }

            rscSetConnectionDataOrd = rscSetConnectionData;
        }
    }

    /**
     * Returns long description of the parameter, that is used for
     * tool tips.
     */
    @Override
    protected String getParamLongDesc(final String param) {
        return null;
    }

    /** Returns short description of the parameter, that is used as * label. */
    @Override
    protected String getParamShortDesc(final String param) {
        return null;
    }

    /**
     * Checks if the new value is correct for the parameter type and
     * constraints.
     */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        return true;
    }

    /** Returns default for this parameter. */
    @Override
    public Value getParamDefault(final String param) {
        return new StringValue("default");
    }

    /** Returns preferred value for this parameter. */
    @Override
    protected Value getParamPreferred(final String param) {
        return null;
    }

    /** Returns lsit of all parameters as an array. */
    @Override
    public String[] getParametersFromXML() {
        return new String[]{};
    }

    /**
     * Possible choices for pulldown menus, or null if it is not a pull
     * down menu.
     */
    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        return null;
    }

    /** Returns parameter type, boolean etc. */
    @Override
    protected String getParamType(final String param) {
        return null;
    }

    /** Returns section to which the global belongs. */
    @Override
    protected String getSection(final String param) {
        return null;
    }

    /**
     * Returns whether the parameter is of the boolean type and needs the
     * checkbox.
     */
    @Override
    protected boolean isCheckBox(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is of time type. */
    @Override
    protected boolean isTimeType(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is integer. */
    @Override
    protected boolean isInteger(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is required. */
    @Override
    protected boolean isRequired(final String param) {
        return true;
    }

    /** Applies changes to the placeholder. */
    @Override
    public void apply(final Host dcHost, final Application.RunMode runMode) {
        /* apply is in resource set info object. */
    }

    /** Returns whether this parameter is advanced. */
    @Override
    protected boolean isAdvanced(final String param) {
        return true;
    }

    /** Returns access type of this parameter. */
    @Override
    protected Application.AccessType getAccessType(final String param) {
        return Application.AccessType.ADMIN;
    }

    /** Returns name of this placeholder. */
    @Override
    public String toString() {
        return getName() +  " (" + getService().getId() + ')';
    }

    /**
     * Returns id of the service, which is heartbeatId.
     * TODO: this id is used for stored position info, should be named
     * differently.
     */
    @Override
    public String getId() {
        String ordId = "";
        final CrmXml.RscSetConnectionData rodata = rscSetConnectionDataOrd;
        if (rodata != null) {
            ordId = rodata.getConstraintId() + '-' + rodata.getConnectionPos();
        }
        final CrmXml.RscSetConnectionData rcdata = rscSetConnectionDataCol;
        String colId = "";
        if (rcdata != null) {
            colId = rcdata.getConstraintId() + '-' + rcdata.getConnectionPos();
        }
        return "ph_" + ordId + '_' + colId;
    }

    /** Return information panel. */
    @Override
    public JComponent getInfoPanel() {
        final PcmkRscSetsInfo prsi = pcmkRscSetsInfo;
        return prsi.getInfoPanel(this);
    }

    /** Returns tool tip for the placeholder. */
    @Override
    public String getToolTipText(final Application.RunMode runMode) {
        return Tools.getString("ConstraintPHInfo.ToolTip");
    }

    /** Return list of popup items. */
    @Override
    public List<UpdatableItem> createPopup() {
        final ConstraintPHMenu constraintPHMenu = new ConstraintPHMenu(this);
        return constraintPHMenu.getPulldownMenu();
    }

    /** Removes the placeholder without confirmation dialog. */
    @Override
    protected void removeMyselfNoConfirm(final Host dcHost,
                                         final Application.RunMode runMode) {
        if (getService().isNew()) {
            if (Application.isLive(runMode)) {
                setUpdated(true);
                getService().setRemoved(true);
                final HbConnectionInfo[] hbcis =
                       getBrowser().getCrmGraph().getHbConnections(this);
                for (final HbConnectionInfo hbci : hbcis) {
                    getBrowser().getCrmGraph().removeConnection(hbci,
                                                                dcHost,
                                                                runMode);
                }
                getService().setNew(false);
                getBrowser().removeFromServiceInfoHash(this);
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        getBrowser().getCrmGraph().killRemovedVertices();
                        getService().doneRemoving();
                    }
                });
            }
        } else {
            if (rscSetConnectionDataOrd != null) {
                final String ordId = rscSetConnectionDataOrd.getConstraintId();
                if (ordId != null) {
                    CRM.removeOrder(dcHost, ordId, runMode);
                }
            }
            if (rscSetConnectionDataCol != null) {
                final String colId = rscSetConnectionDataCol.getConstraintId();
                if (colId != null) {
                    CRM.removeColocation(dcHost, colId, runMode);
                }
            }
            if (Application.isLive(runMode)) {
                setUpdated(true);
                getService().setRemoved(true);
            }
        }
    }

    /** Removes this placeholder from the crm with confirmation dialog. */
    @Override
    public void removeMyself(final Application.RunMode runMode) {
        if (getService().isNew()) {
            removeMyselfNoConfirm(getBrowser().getDCHost(), runMode);
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
            removeMyselfNoConfirm(getBrowser().getDCHost(), runMode);
            getService().setNew(false);
        }
    }

    /** Sets whether the info object is being updated. */
    @Override
    public void setUpdated(final boolean updated) {
        if (updated && !isUpdated()) {
            getBrowser().getCrmGraph().startAnimation(this);
        } else if (!updated) {
            getBrowser().getCrmGraph().stopAnimation(this);
        }
        super.setUpdated(updated);
    }

    /** Whether this class is a constraint placeholder. */
    @Override
    public boolean isConstraintPH() {
        return true;
    }

    /** Sets whether the direction of order was reversed, meaning it is from
     * this placeholder, when it was new. */
    void reverseOrder() {
        reverseOrd = true;
    }

    /** Sets whether the direction of colocation was reversed, meaning it is
     * from this placeholder, when it was new. */
    void reverseColocation() {
        reverseCol = true;
    }

    /** Returns whether the colocation was reversed. */
    boolean isReversedCol() {
        return reversedCol;
    }

    /** Returns whether the order was reversed. */
    boolean isReversedOrd() {
        return reversedOrd;
    }

    /** Adds constraint to or from placeholder. */
    protected List<CrmXml.RscSet> addConstraintWithPlaceholder(
                                      final Iterable<ServiceInfo> servicesAll,
                                      final Collection<ServiceInfo> servicesFrom,
                                      final boolean colocation,
                                      final boolean order,
                                      final Host dcHost,
                                      final boolean force,
                                      final Application.RunMode runMode) {
        String colId = null;
        final CrmXml.RscSetConnectionData rdataCol =
                                                getRscSetConnectionDataCol();
        final CrmXml.RscSetConnectionData rdataOrd =
                                                getRscSetConnectionDataOrd();
        CrmXml.RscSet colRscSet1 = null;
        CrmXml.RscSet colRscSet2 = null;

        if (rdataCol != null) {
            if (colocation) {
                colId = rdataCol.getConstraintId();
            }
            colRscSet1 = rdataCol.getRscSet1();
            colRscSet2 = rdataCol.getRscSet2();
        }
        String ordId = null;
        CrmXml.RscSet ordRscSet1 = null;
        CrmXml.RscSet ordRscSet2 = null;
        if (rdataOrd != null) {
            if (order) {
                ordId = rdataOrd.getConstraintId();
            }
            ordRscSet1 = rdataOrd.getRscSet1();
            ordRscSet2 = rdataOrd.getRscSet2();
        }
        if (servicesFrom.isEmpty()) {
            if (order && Application.isLive(runMode)) {
                reverseOrder();
            }
            if (colocation && Application.isLive(runMode)) {
                reverseColocation();
            }
        }
        CrmXml.RscSet outOrdRscSet1 = null;
        CrmXml.RscSet outOrdRscSet2 = null;
        CrmXml.RscSet outColRscSet1 = null;
        CrmXml.RscSet outColRscSet2 = null;
        final String requireAll = (preference == Preference.AND)
                                                    ? CrmXml.REQUIRE_ALL_TRUE_VALUE.getValueForConfig()
                                                    : CrmXml.REQUIRE_ALL_FALSE_VALUE.getValueForConfig();
        boolean createCol = false;
        boolean createOrd = false;
        for (final ServiceInfo serviceInfo : servicesAll) {
            final boolean isFrom = servicesFrom.contains(serviceInfo);
            final String idToAdd = serviceInfo.getService().getHeartbeatId();
            if (colocation) {
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
                        /* require all for col is noop in pcmk 1.1.7 */
                        colRscSet2 = new CrmXml.RscSet(colId,
                                                       new ArrayList<String>(),
                                                       "false",
                                                       requireAll,
                                                       null,
                                                       null);
                        colRscSet1 = new CrmXml.RscSet(colId,
                                                       rscIds,
                                                       "false",
                                                       CrmXml.REQUIRE_ALL_TRUE_VALUE.getValueForConfig(),
                                                       null,
                                                       null);
                        outColRscSet1 = colRscSet1;
                    } else {
                        colRscSet2 = new CrmXml.RscSet(colId,
                                                       rscIds,
                                                       "false",
                                                       CrmXml.REQUIRE_ALL_TRUE_VALUE.getValueForConfig(),
                                                       null,
                                                       null);
                        colRscSet1 = new CrmXml.RscSet(colId,
                                                       new ArrayList<String>(),
                                                       "false",
                                                       requireAll,
                                                       null,
                                                       null);
                        outColRscSet2 = colRscSet2;
                    }
                } else {
                    final CrmXml.RscSet toRscSet;
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
                    final List<CrmXml.RscSet> rscSetsColList =
                                                 clStatus.getRscSetsCol(colId);
                    boolean colRscSetAdded = false;
                    if (rscSetsColList != null) {
                        for (final CrmXml.RscSet rscSet : rscSetsColList) {
                            if (rscSet.equals(toRscSet)) {
                                final List<String> newRscIds =
                                                       new ArrayList<String>();
                                newRscIds.addAll(rscSet.getRscIds());

                                newRscIds.add(0, idToAdd);
                                final CrmXml.RscSet newRscSet =
                                    new CrmXml.RscSet(
                                                   rscSet.getId(),
                                                   newRscIds,
                                                   rscSet.getSequential(),
                                                   rscSet.getRequireAll(),
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
                        final CrmXml.RscSet newRscSet;
                        if (toRscSet == null) {
                            newRscSet =
                                new CrmXml.RscSet(colId,
                                                  newRscIds,
                                                  "false",
                                                  requireAll,
                                                  null,
                                                  null);
                        } else {
                            newRscIds.addAll(toRscSet.getRscIds());
                            newRscSet =
                                new CrmXml.RscSet(toRscSet.getId(),
                                                  newRscIds,
                                                  toRscSet.getSequential(),
                                                  toRscSet.getRequireAll(),
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

            if (order) {
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
                        ordRscSet1 = new CrmXml.RscSet(ordId,
                                                       rscIds,
                                                       "false",
                                                       CrmXml.REQUIRE_ALL_TRUE_VALUE.getValueForConfig(),
                                                       null,
                                                       null);
                        ordRscSet2 = new CrmXml.RscSet(ordId,
                                                       new ArrayList<String>(),
                                                       "false",
                                                       requireAll,
                                                       null,
                                                       null);
                        outOrdRscSet1 = ordRscSet1;
                    } else {
                        ordRscSet1 = new CrmXml.RscSet(ordId,
                                                       new ArrayList<String>(),
                                                       "false",
                                                       requireAll,
                                                       null,
                                                       null);
                        ordRscSet2 = new CrmXml.RscSet(ordId,
                                                       rscIds,
                                                       "false",
                                                       CrmXml.REQUIRE_ALL_TRUE_VALUE.getValueForConfig(),
                                                       null,
                                                       null);
                        outOrdRscSet2 = ordRscSet2;
                    }
                } else {
                    final CrmXml.RscSet toRscSet;
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
                    final List<CrmXml.RscSet> rscSetsOrdList =
                                                 clStatus.getRscSetsOrd(ordId);
                    boolean ordRscSetAdded = false;
                    if (rscSetsOrdList != null) {
                        for (final CrmXml.RscSet rscSet : rscSetsOrdList) {
                            if (rscSet.equals(toRscSet)) {
                                final List<String> newRscIds =
                                                       new ArrayList<String>();
                                newRscIds.addAll(rscSet.getRscIds());

                                newRscIds.add(idToAdd);
                                final CrmXml.RscSet newRscSet =
                                    new CrmXml.RscSet(
                                                   rscSet.getId(),
                                                   newRscIds,
                                                   rscSet.getSequential(),
                                                   rscSet.getRequireAll(),
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
                        final CrmXml.RscSet newRscSet;
                        if (toRscSet == null) {
                            newRscSet = new CrmXml.RscSet(ordId,
                                                  newRscIds,
                                                  "false",
                                                  requireAll,
                                                  null,
                                                  null);
                        } else {
                            newRscIds.addAll(toRscSet.getRscIds());
                            newRscSet = new CrmXml.RscSet(
                                               toRscSet.getId(),
                                               newRscIds,
                                               toRscSet.getSequential(),
                                               toRscSet.getRequireAll(),
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
        if (Application.isLive(runMode)) {
            setUpdated(false);
        }
        final List<CrmXml.RscSet> sets = new ArrayList<CrmXml.RscSet>();
        sets.add(outColRscSet2);
        sets.add(outColRscSet1);
        sets.add(outOrdRscSet1);
        sets.add(outOrdRscSet2);
        if (force) {
            final Map<String, String> attrs =
                                          new LinkedHashMap<String, String>();
            attrs.put(CrmXml.SCORE_CONSTRAINT_PARAM, CrmXml.INFINITY_VALUE.getValueForConfig());
            final Map<CrmXml.RscSet, Map<String, String>> rscSetsColAttrs =
                       new LinkedHashMap<CrmXml.RscSet, Map<String, String>>();
            final Map<CrmXml.RscSet, Map<String, String>> rscSetsOrdAttrs =
                       new LinkedHashMap<CrmXml.RscSet, Map<String, String>>();

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
                          runMode);
        }
        return sets;
    }

    /** Whether the id of this constraint is the same or there is no id in this
     * object. */
    boolean sameConstraintId(final CrmXml.RscSetConnectionData otherRdata) {
        if (otherRdata.isColocation()) {
            final CrmXml.RscSetConnectionData rdataCol =
                                                       rscSetConnectionDataCol;
            return rdataCol == null
                   || rdataCol.getConstraintId() == null
                   || rdataCol.getConstraintId().equals(
                                                 otherRdata.getConstraintId());
        } else {
            final CrmXml.RscSetConnectionData rdataOrd =
                                                       rscSetConnectionDataOrd;
            return rdataOrd == null
                   || rdataOrd.getConstraintId() == null
                   || rdataOrd.getConstraintId().equals(
                                                 otherRdata.getConstraintId());
        }
    }

    /** Sets rsc sets info object. */
    void setPcmkRscSetsInfo(final PcmkRscSetsInfo pcmkRscSetsInfo) {
        this.pcmkRscSetsInfo = pcmkRscSetsInfo;
    }

    /** Gets rsc sets info object. */
    PcmkRscSetsInfo getPcmkRscSetsInfo() {
        return pcmkRscSetsInfo;
    }

    /** Hide/Show advanced panels. */
    @Override
    public void updateAdvancedPanels() {
        super.updateAdvancedPanels();
        final PcmkRscSetsInfo prsi = pcmkRscSetsInfo;
        if (prsi != null) {
            prsi.updateAdvancedPanels();
        }
    }

    /** Returns whether the placeholder has any connections at all. */
    boolean isEmpty() {
        final CrmXml.RscSetConnectionData rdataOrd =
                                            getRscSetConnectionDataOrd();
        final CrmXml.RscSetConnectionData rdataCol =
                                            getRscSetConnectionDataCol();
        return (rdataOrd == null || rdataOrd.isEmpty())
                && (rdataCol == null || rdataCol.isEmpty());
    }

    /** Returns attributes for resource_set tag. */
    void getAttributes(
                  final boolean isCol,
                  final boolean first,
                  final Map<CrmXml.RscSet, Map<String, String>> rscSetsAttrs) {
        final CrmXml.RscSetConnectionData rscd;
        if (isCol) {
            rscd = rscSetConnectionDataCol;
        } else {
            rscd = rscSetConnectionDataOrd;
        }
        if (rscd == null) {
            return;
        }
        final CrmXml.RscSet rscSet;
        if (first) {
            rscSet = rscd.getRscSet1();
        } else {
            rscSet = rscd.getRscSet2();
        }
        rscSetsAttrs.put(rscSet, null);
    }

    /** Returns resource that is next in sequence in the resource set. */
    public ServiceInfo nextInSequence(final ServiceInfo si,
                                      final boolean isCol) {
        final CrmXml.RscSetConnectionData rscd;
        if (isCol) {
            rscd = rscSetConnectionDataCol;
        } else {
            rscd = rscSetConnectionDataOrd;
        }
        if (rscd == null) {
            return null;
        }
        for (final CrmXml.RscSet rscSet
                : new CrmXml.RscSet[]{rscd.getRscSet1(), rscd.getRscSet2()}) {
            if (rscSet == null) {
                continue;
            }
            if (!rscSet.isSequential()) {
                continue;
            }
            final List<String> ids = rscSet.getRscIds();
            for (int i = 0; i < ids.size(); i++) {
                if (i < ids.size() - 1
                    && ids.get(i).equals(si.getHeartbeatId(Application.RunMode.TEST))) {
                    return getBrowser().getServiceInfoFromCRMId(ids.get(i + 1));
                }
            }
        }
        return null;
    }

    /** Returns resource that is before in sequence in the resource set. */
    public ServiceInfo prevInSequence(final ServiceInfo si,
                                      final boolean isCol) {
        final CrmXml.RscSetConnectionData rscd;
        if (isCol) {
            rscd = rscSetConnectionDataCol;
        } else {
            rscd = rscSetConnectionDataOrd;
        }
        if (rscd == null) {
            return null;
        }
        for (final CrmXml.RscSet rscSet
                : new CrmXml.RscSet[]{rscd.getRscSet1(), rscd.getRscSet2()}) {
            if (rscSet == null) {
                continue;
            }
            if (!rscSet.isSequential()) {
                continue;
            }
            final List<String> ids = rscSet.getRscIds();
            for (int i = ids.size() - 1; i >= 0; i--) {
                if (i > 0 && ids.get(i).equals(
                    si.getHeartbeatId(Application.RunMode.TEST))) {
                    return getBrowser().getServiceInfoFromCRMId(ids.get(i - 1));
                }
            }
        }
        return null;
    }

    /** Returns the main text that appears in the graph. */
    @Override
    public String getMainTextForGraph() {
        final CrmXml.RscSetConnectionData rscd = rscSetConnectionDataOrd;
        if (getService().isNew()) {
            return (preference == Preference.AND) ? CONSTRAINT_PLACEHOLDER_AND
                                                  : CONSTRAINT_PLACEHOLDER_OR;
        }
        if (rscd == null || rscd.isColocation()) {
            /* if there's colocation require-all has no effect in pcmk 1.1.7 */
            return CONSTRAINT_PLACEHOLDER_AND;
        }
        final CrmXml.RscSet rscSet = rscd.getRscSet1();
        if (rscSet != null && !rscSet.isRequireAll()) {
            return CONSTRAINT_PLACEHOLDER_OR;
        }
        return CONSTRAINT_PLACEHOLDER_AND;
    }

    /** Returns text that appears above the icon in the graph. */
    @Override
    public String getIconTextForGraph(final Application.RunMode runMode) {
        return "   " + getService().getId();
    }
    /** Returns text with lines as array that appears in the cluster graph. */
    @Override
    public Subtext[] getSubtextsForGraph(final Application.RunMode runMode) {
        return null;
    }

    /** Stops resource in crm. */
    @Override
    void stopResource(final Host dcHost, final Application.RunMode runMode) {
        /* cannot stop placeholder */
    }

    public enum Preference {
        AND,
        OR
    }
}
