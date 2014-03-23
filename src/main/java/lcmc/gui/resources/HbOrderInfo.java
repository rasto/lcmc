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
import lcmc.gui.widget.Widget;
import lcmc.data.resources.Service;
import lcmc.utilities.CRM;
import lcmc.utilities.Tools;
import lcmc.Exceptions;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;

import lcmc.gui.widget.Check;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * Object that holds an order constraint information.
 */
final class HbOrderInfo extends EditableInfo
                         implements HbConstraintInterface {
    /** Logger. */
    private static final Logger LOG =
                                  LoggerFactory.getLogger(HbOrderInfo.class);
    /** Parent resource in order constraint. */
    private ServiceInfo serviceInfoParent;
    /** Child resource in order constraint. */
    private ServiceInfo serviceInfoChild;
    /** Connection that keeps this constraint. */
    private final HbConnectionInfo connectionInfo;
    /** Text of disabled item. */
    public static final String NOT_AVAIL_FOR_PCMK_VERSION =
                    Tools.getString("HbOrderInfo.NotAvailableForThisVersion");

    /** Prepares a new {@code HbOrderInfo} object. */
    HbOrderInfo(final HbConnectionInfo connectionInfo,
                final ServiceInfo serviceInfoParent,
                final ServiceInfo serviceInfoChild,
                final Browser browser) {
        super("Order", browser);
        setResource(new Service("Order"));
        this.connectionInfo = connectionInfo;
        this.serviceInfoParent = serviceInfoParent;
        this.serviceInfoChild = serviceInfoChild;
    }

    /** Sets "first" parent service info. */
    void setServiceInfoParent(final ServiceInfo serviceInfoParent) {
        this.serviceInfoParent = serviceInfoParent;
    }

    /** Sets "then" child service info. */
    void setServiceInfoChild(final ServiceInfo serviceInfoChild) {
        this.serviceInfoChild = serviceInfoChild;
    }

    /** Returns browser object of this info. */
    @Override
    protected ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }


    /** Sets the order's parameters. */
    void setParameters() {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        final String ordId = getService().getHeartbeatId();
        final Map<String, Value> resourceNode = new HashMap<String, Value>();

        if (serviceInfoParent == null || serviceInfoChild == null) {
            /* rsc set placeholder */
            final CRMXML.OrderData orderData = clStatus.getOrderData(ordId);
            final String score = orderData.getScore();
            resourceNode.put(CRMXML.SCORE_STRING, new StringValue(score));
        } else if (serviceInfoParent.isConstraintPH()
                   || serviceInfoChild.isConstraintPH()) {
            /* rsc set edge */
            final ConstraintPHInfo cphi;
            final CRMXML.RscSet rscSet;
            if (serviceInfoParent.isConstraintPH()) {
                cphi = (ConstraintPHInfo) serviceInfoParent;
                rscSet = cphi.getRscSetConnectionDataOrd().getRscSet2();
            } else {
                cphi = (ConstraintPHInfo) serviceInfoChild;
                rscSet = cphi.getRscSetConnectionDataOrd().getRscSet1();
            }
            resourceNode.put("sequential", new StringValue(rscSet.getSequential()));
            resourceNode.put(CRMXML.REQUIRE_ALL_ATTR, new StringValue(rscSet.getRequireAll()));
            resourceNode.put("action", new StringValue(rscSet.getOrderAction()));
        } else {
            final CRMXML.OrderData orderData = clStatus.getOrderData(ordId);
            if (orderData != null) {
                final String score = orderData.getScore();
                final String symmetrical = orderData.getSymmetrical();
                final String firstAction = orderData.getFirstAction();
                final String thenAction = orderData.getThenAction();

                resourceNode.put(CRMXML.SCORE_STRING, new StringValue(score));
                resourceNode.put("symmetrical", new StringValue(symmetrical));
                resourceNode.put("first-action", new StringValue(firstAction));
                resourceNode.put("then-action", new StringValue(thenAction));
            }
        }

        final String[] params = getParametersFromXML();
        if (params != null) {
            for (final String param : params) {
                Value value = resourceNode.get(param);
                if (value == null || value.isNothingSelected()) {
                    value = getParamDefault(param);
                }
                final Value oldValue = getParamSaved(param);
                if (!Tools.areEqual(value, oldValue)) {
                    getResource().setValue(param, value);
                    final Widget wi = getWidget(param, null);
                    if (wi != null) {
                        wi.setValue(value);
                    }
                }
            }
        }
    }

    /** Returns that this is order constraint. */
    @Override
    public boolean isOrder() {
        return true;
    }

    /**
     * Returns long description of the parameter, that is used for
     * tool tips.
     */
    @Override
    protected String getParamLongDesc(final String param) {
        final String text =
                        getBrowser().getCRMXML().getOrderParamLongDesc(param);
        if (serviceInfoParent != null && serviceInfoChild != null) {
            return text.replaceAll(
                         "@FIRST-RSC@",
                         Matcher.quoteReplacement(serviceInfoParent.toString()))
                       .replaceAll(
                         "@THEN-RSC@",
                         Matcher.quoteReplacement(serviceInfoChild.toString()));
        } else {
            return text;
        }
    }

    /** Returns short description of the parameter, that is used as * label. */
    @Override
    protected String getParamShortDesc(final String param) {
        return getBrowser().getCRMXML().getOrderParamShortDesc(param);
    }

    /**
     * Checks if the new value is correct for the parameter type and
     * constraints.
     */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        return getBrowser().getCRMXML().checkOrderParam(param, newValue);
    }

    /** Returns default for this parameter. */
    @Override
    protected Value getParamDefault(final String param) {
        return getBrowser().getCRMXML().getOrderParamDefault(param);
    }

    /** Returns preferred value for this parameter. */
    @Override
    protected Value getParamPreferred(final String param) {
        return getBrowser().getCRMXML().getOrderParamPreferred(param);
    }

    /** Returns lsit of all parameters as an array. */
    @Override
    public String[] getParametersFromXML() {
        if (serviceInfoParent == null || serviceInfoChild == null) {
            /* rsc set order */
            return getBrowser().getCRMXML().getRscSetOrderParameters();
        } else if (serviceInfoParent.isConstraintPH()
                   || serviceInfoChild.isConstraintPH()) {
            /* when rsc set edges are clicked */
            return getBrowser().getCRMXML().getRscSetOrdConnectionParameters();
        } else {
            return getBrowser().getCRMXML().getOrderParameters();
        }
    }

    /** Returns when at least one resource in rsc set can be promoted. */
    private boolean isRscSetMaster() {
        final ConstraintPHInfo cphi;
        final CRMXML.RscSet rscSet;
        if (serviceInfoParent.isConstraintPH()) {
            cphi = (ConstraintPHInfo) serviceInfoParent;
            rscSet = cphi.getRscSetConnectionDataOrd().getRscSet2();
        } else {
            cphi = (ConstraintPHInfo) serviceInfoChild;
            rscSet = cphi.getRscSetConnectionDataOrd().getRscSet1();
        }
        return getBrowser().isOneMaster(rscSet.getRscIds());
    }

    /**
     * Possible choices for pulldown menus, or null if it is not a pull
     * down menu.
     */
    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        if ("action".equals(param)) {
            /* rsc set */
            return getBrowser().getCRMXML().getOrderParamPossibleChoices(
                                                            param,
                                                            isRscSetMaster());
        } else if ("first-action".equals(param)) {
            return getBrowser().getCRMXML().getOrderParamPossibleChoices(
                                param,
                                serviceInfoParent.getService().isMaster());
        } else if ("then-action".equals(param)) {
            return getBrowser().getCRMXML().getOrderParamPossibleChoices(
                                param,
                                serviceInfoChild.getService().isMaster());
        } else {
            return getBrowser().getCRMXML().getOrderParamPossibleChoices(param,
                                                                         false);
        }
    }

    /** Returns parameter type, boolean etc. */
    @Override
    protected String getParamType(final String param) {
        return getBrowser().getCRMXML().getOrderParamType(param);
    }

    /** Returns section to which the global belongs. */
    @Override
    protected String getSection(final String param) {
        return getBrowser().getCRMXML().getOrderSection(param);
    }

    /**
     * Returns whether the parameter is of the boolean type and needs the
     * checkbox.
     */
    @Override
    protected boolean isCheckBox(final String param) {
        return getBrowser().getCRMXML().isOrderBoolean(param);
    }

    /** Returns true if the specified parameter is of time type. */
    @Override
    protected boolean isTimeType(final String param) {
        return getBrowser().getCRMXML().isOrderTimeType(param);
    }

    /** Returns true if the specified parameter is integer. */
    @Override
    protected boolean isInteger(final String param) {
        return getBrowser().getCRMXML().isOrderInteger(param);
    }

    /** Returns true if the specified parameter is label. */
    @Override
    protected boolean isLabel(final String param) {
        return getBrowser().getCRMXML().isOrderLabel(param);
    }

    /** Returns true if the specified parameter is required. */
    @Override
    protected boolean isRequired(final String param) {
        return getBrowser().getCRMXML().isOrderRequired(param);
    }

    /** Returns attributes of this colocation. */
    //TODO: what uses it?
    protected Map<String, String> getAttributes() {
        final String[] params = getParametersFromXML();
        final Map<String, String> attrs = new LinkedHashMap<String, String>();
        for (final String param : params) {
            final Value value = getComboBoxValue(param);
            if (!value.equals(getParamDefault(param))) {
                attrs.put(param, value.getValueForConfig());
            }
        }
        return attrs;
    }

    /** Applies changes to the order parameters. */
    @Override
    public void apply(final Host dcHost, final boolean testOnly) {
        final String[] params = getParametersFromXML();
        final Map<String, String> attrs = new LinkedHashMap<String, String>();
        boolean changed = false;
        for (final String param : params) {
            final Value value = getComboBoxValue(param);
            if (!value.equals(getParamSaved(param))) {
                changed = true;
            }
            if (!value.equals(getParamDefault(param))) {
                attrs.put(param, value.getValueForConfig());
            }
        }
        if (changed) {
            final String ordId = getService().getHeartbeatId();
            if (serviceInfoParent == null || serviceInfoChild == null) {
                /* rsc set order */
                final PcmkRscSetsInfo prsi = (PcmkRscSetsInfo) connectionInfo;
                CRM.setRscSet(dcHost,
                              null,
                              false,
                              ordId,
                              false,
                              null,
                              prsi.getAllAttributes(dcHost,
                                                    null,
                                                    null,
                                                    false,
                                                    testOnly),
                              attrs,
                              testOnly);
            } else if (serviceInfoParent.isConstraintPH()
                       || serviceInfoChild.isConstraintPH()) {
                final ConstraintPHInfo cphi;
                final CRMXML.RscSet rscSet;
                if (serviceInfoParent.isConstraintPH()) {
                    cphi = (ConstraintPHInfo) serviceInfoParent;
                    rscSet = cphi.getRscSetConnectionDataOrd().getRscSet2();
                } else {
                    cphi = (ConstraintPHInfo) serviceInfoChild;
                    rscSet = cphi.getRscSetConnectionDataOrd().getRscSet1();
                }
                final PcmkRscSetsInfo prsi = cphi.getPcmkRscSetsInfo();

                CRM.setRscSet(dcHost,
                              null,
                              false,
                              ordId,
                              false,
                              null,
                              prsi.getAllAttributes(dcHost,
                                                    rscSet,
                                                    attrs,
                                                    false,
                                                    testOnly),
                              prsi.getOrderAttributes(ordId),
                              testOnly);
            } else {
                CRM.addOrder(dcHost,
                             ordId,
                             serviceInfoParent.getHeartbeatId(testOnly),
                             serviceInfoChild.getHeartbeatId(testOnly),
                             attrs,
                             testOnly);
            }
            if (!testOnly) {
                storeComboBoxValues(params);
            }
        }
    }

    /** Returns service that belongs to this info object. */
    @Override
    public Service getService() {
        return (Service) getResource();
    }

    /** Returns name of the rsc1 attribute. */
    @Override
    public String getRsc1Name() {
        return "first";
    }

    /** Returns name of the rsc2 attribute. */
    @Override
    public String getRsc2Name() {
        return "then";
    }

    /** Get parent resource in order constraint. */
    @Override
    public String getRsc1() {
        return serviceInfoParent.toString();
    }

    /** Get child resource in order constraint. */
    @Override
    public String getRsc2() {
        return serviceInfoChild.toString();
    }

    /** Get parent resource in order constraint. */
    @Override
    public ServiceInfo getRscInfo1() {
        return serviceInfoParent;
    }

    /** Get child resource in order constraint. */
    @Override
    public ServiceInfo getRscInfo2() {
        return serviceInfoChild;
    }

    /** Returns whether this parameter is advanced. */
    @Override
    protected boolean isAdvanced(final String param) {
        return !CRMXML.SCORE_STRING.equals(param);
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected String isEnabled(final String param) {
        if (CRMXML.REQUIRE_ALL_ATTR.equals(param)) {
            final String pmV = getBrowser().getDCHost().getPacemakerVersion();
            try {
                //TODO: get this from constraints-.rng files
                if (pmV == null || Tools.compareVersions(pmV, "1.1.7") <= 0) {
                    return NOT_AVAIL_FOR_PCMK_VERSION;
                }
            } catch (final Exceptions.IllegalVersionException e) {
                LOG.appWarning("isEnabled: unkonwn version: " + pmV);
                /* enable it, if version check doesn't work */
            }
        }
        return null;
    }

    /** Returns access type of this parameter. */
    @Override
    protected Application.AccessType getAccessType(
                                                        final String param) {
        return Application.AccessType.ADMIN;
    }

    /** Returns the score of this order. */
    int getScore() {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        final String ordId = getService().getHeartbeatId();
        final CRMXML.OrderData data = clStatus.getOrderData(ordId);
        if (data == null) {
            return 0;
        }
        final String score = data.getScore();
        if (score == null) {
            return 0;
        } else if (CRMXML.INFINITY_STRING.getValueForConfig().equals(score)
                   || CRMXML.PLUS_INFINITY_STRING.getValueForConfig().equals(score)) {
            return 1000000;
        } else if (CRMXML.MINUS_INFINITY_STRING.getValueForConfig().equals(score)) {
            return -1000000;
        }
        return Integer.parseInt(score);
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override
    protected boolean isEnabledOnlyInAdvancedMode(final String param) {
         return false;
    }

    /**
     * Checks resource fields of all constraints that are in this
     * connection with this constraint.
     */
    @Override
    Check checkResourceFields(final String param, final String[] params) {
        return checkResourceFields(param, params, false);
    }

    /**
     * Checks resource fields of all constraints that are in this
     * connection with this constraint.
     */
    @Override
    public Check checkResourceFields(final String param,
                                     final String[] params,
                                     final boolean fromUp) {
        if (fromUp) {
            return super.checkResourceFields(param, params);
        } else {
            return connectionInfo.checkResourceFields(param, null);
        }
    }
}
