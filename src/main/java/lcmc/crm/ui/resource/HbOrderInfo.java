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
import lcmc.Exceptions;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.widget.Check;
import lcmc.cluster.ui.widget.Widget;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.ResourceValue;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.common.ui.EditableInfo;
import lcmc.crm.domain.ClusterStatus;
import lcmc.crm.domain.CrmXml;
import lcmc.crm.domain.Service;
import lcmc.crm.service.CRM;
import lcmc.host.domain.Host;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

import javax.inject.Named;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Object that holds an order constraint information.
 */
@Named
final class HbOrderInfo extends EditableInfo implements HbConstraintInterface {
    private static final Logger LOG = LoggerFactory.getLogger(HbOrderInfo.class);
    public static final String NOT_AVAIL_FOR_PCMK_VERSION = Tools.getString("HbOrderInfo.NotAvailableForThisVersion");
    private ServiceInfo serviceInfoParent;
    private ServiceInfo serviceInfoChild;
    /** Connection that keeps this constraint. */
    private HbConnectionInfo connectionInfo;

    void init(final HbConnectionInfo connectionInfo,
              final ServiceInfo serviceInfoParent,
              final ServiceInfo serviceInfoChild,
              final Browser browser) {
        super.einit(Optional.<ResourceValue>of(new Service("Order")), "Order", browser);
        this.connectionInfo = connectionInfo;
        this.serviceInfoParent = serviceInfoParent;
        this.serviceInfoChild = serviceInfoChild;
    }

    void setServiceInfoParent(final ServiceInfo serviceInfoParent) {
        this.serviceInfoParent = serviceInfoParent;
    }

    void setServiceInfoChild(final ServiceInfo serviceInfoChild) {
        this.serviceInfoChild = serviceInfoChild;
    }

    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }


    void setParameters() {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        final String ordId = getService().getCrmId();
        final Map<String, Value> resourceNode = new HashMap<String, Value>();

        if (serviceInfoParent == null || serviceInfoChild == null) {
            /* rsc set placeholder */
            final CrmXml.OrderData orderData = clStatus.getOrderData(ordId);
            final String score = orderData.getScore();
            resourceNode.put(CrmXml.SCORE_CONSTRAINT_PARAM, new StringValue(score));
        } else if (serviceInfoParent.isConstraintPlaceholder() || serviceInfoChild.isConstraintPlaceholder()) {
            /* rsc set edge */
            final ConstraintPHInfo cphi;
            final CrmXml.RscSet rscSet;
            if (serviceInfoParent.isConstraintPlaceholder()) {
                cphi = (ConstraintPHInfo) serviceInfoParent;
                rscSet = cphi.getRscSetConnectionDataOrder().getRscSet2();
            } else {
                cphi = (ConstraintPHInfo) serviceInfoChild;
                rscSet = cphi.getRscSetConnectionDataOrder().getRscSet1();
            }
            resourceNode.put("sequential", new StringValue(rscSet.getSequential()));
            resourceNode.put(CrmXml.REQUIRE_ALL_ATTR, new StringValue(rscSet.getRequireAll()));
            resourceNode.put("action", new StringValue(rscSet.getOrderAction()));
        } else {
            final CrmXml.OrderData orderData = clStatus.getOrderData(ordId);
            if (orderData != null) {
                final String score = orderData.getScore();
                final String symmetrical = orderData.getSymmetrical();
                final String firstAction = orderData.getFirstAction();
                final String thenAction = orderData.getThenAction();

                resourceNode.put(CrmXml.SCORE_CONSTRAINT_PARAM, new StringValue(score));
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
        final String text = getBrowser().getCrmXml().getOrderParamLongDesc(param);
        if (serviceInfoParent != null && serviceInfoChild != null) {
            return text.replaceAll("@FIRST-RSC@", Matcher.quoteReplacement(serviceInfoParent.toString()))
                       .replaceAll("@THEN-RSC@", Matcher.quoteReplacement(serviceInfoChild.toString()));
        } else {
            return text;
        }
    }

    @Override
    protected String getParamShortDesc(final String param) {
        return getBrowser().getCrmXml().getOrderParamShortDesc(param);
    }

    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        return getBrowser().getCrmXml().checkOrderParam(param, newValue);
    }

    @Override
    protected Value getParamDefault(final String param) {
        return getBrowser().getCrmXml().getOrderParamDefault(param);
    }

    @Override
    protected Value getParamPreferred(final String param) {
        return getBrowser().getCrmXml().getOrderParamPreferred(param);
    }

    @Override
    public String[] getParametersFromXML() {
        if (serviceInfoParent == null || serviceInfoChild == null) {
            /* rsc set order */
            return getBrowser().getCrmXml().getResourceSetOrderParameters();
        } else if (serviceInfoParent.isConstraintPlaceholder() || serviceInfoChild.isConstraintPlaceholder()) {
            /* when rsc set edges are clicked */
            return getBrowser().getCrmXml().getRscSetOrdConnectionParameters();
        } else {
            return getBrowser().getCrmXml().getOrderParameters();
        }
    }

    private boolean isRscSetMaster() {
        final ConstraintPHInfo cphi;
        final CrmXml.RscSet rscSet;
        if (serviceInfoParent.isConstraintPlaceholder()) {
            cphi = (ConstraintPHInfo) serviceInfoParent;
            rscSet = cphi.getRscSetConnectionDataOrder().getRscSet2();
        } else {
            cphi = (ConstraintPHInfo) serviceInfoChild;
            rscSet = cphi.getRscSetConnectionDataOrder().getRscSet1();
        }
        return getBrowser().isOneMaster(rscSet.getRscIds());
    }

    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        if ("action".equals(param)) {
            /* rsc set */
            return getBrowser().getCrmXml().getOrderParamPossibleChoices(param, isRscSetMaster());
        } else if ("first-action".equals(param)) {
            return getBrowser().getCrmXml().getOrderParamPossibleChoices(
                                param,
                                serviceInfoParent.getService().isMaster());
        } else if ("then-action".equals(param)) {
            return getBrowser().getCrmXml().getOrderParamPossibleChoices(
                                param,
                                serviceInfoChild.getService().isMaster());
        } else {
            return getBrowser().getCrmXml().getOrderParamPossibleChoices(param, false);
        }
    }

    @Override
    protected String getParamType(final String param) {
        return getBrowser().getCrmXml().getOrderParamType(param);
    }

    @Override
    protected String getSection(final String param) {
        return getBrowser().getCrmXml().getOrderSectionToDisplay(param);
    }

    @Override
    protected boolean isCheckBox(final String param) {
        return getBrowser().getCrmXml().isOrderBoolean(param);
    }

    @Override
    protected boolean isTimeType(final String param) {
        return getBrowser().getCrmXml().isOrderTimeType(param);
    }

    @Override
    protected boolean isInteger(final String param) {
        return getBrowser().getCrmXml().isOrderInteger(param);
    }

    @Override
    protected boolean isLabel(final String param) {
        return getBrowser().getCrmXml().isOrderLabel(param);
    }

    @Override
    protected boolean isRequired(final String param) {
        return getBrowser().getCrmXml().isOrderRequired(param);
    }

    protected Map<String, String> getAttributes() {
        final String[] params = getParametersFromXML();
        final Map<String, String> attrs = new LinkedHashMap<String, String>();
        for (final String param : params) {
            final Value value = getComboBoxValue(param);
            if (value != null && !Tools.areEqual(value, getParamDefault(param))) {
                attrs.put(param, value.getValueForConfig());
            }
        }
        return attrs;
    }

    @Override
    public void apply(final Host dcHost, final Application.RunMode runMode) {
        final String[] params = getParametersFromXML();
        final Map<String, String> attrs = new LinkedHashMap<String, String>();
        boolean changed = false;
        for (final String param : params) {
            final Value value = getComboBoxValue(param);
            if (!Tools.areEqual(value, getParamSaved(param))) {
                changed = true;
            }
            if (value != null && !value.equals(getParamDefault(param))) {
                attrs.put(param, value.getValueForConfig());
            }
        }
        if (changed) {
            final String ordId = getService().getCrmId();
            if (serviceInfoParent == null || serviceInfoChild == null) {
                /* rsc set order */
                final PcmkRscSetsInfo prsi = (PcmkRscSetsInfo) connectionInfo;
                CRM.setRscSet(dcHost,
                              null,
                              false,
                              ordId,
                              false,
                              null,
                              prsi.getAllAttributes(dcHost, null, null, false, runMode),
                              attrs,
                              runMode);
            } else if (serviceInfoParent.isConstraintPlaceholder() || serviceInfoChild.isConstraintPlaceholder()) {
                final ConstraintPHInfo cphi;
                final CrmXml.RscSet rscSet;
                if (serviceInfoParent.isConstraintPlaceholder()) {
                    cphi = (ConstraintPHInfo) serviceInfoParent;
                    rscSet = cphi.getRscSetConnectionDataOrder().getRscSet2();
                } else {
                    cphi = (ConstraintPHInfo) serviceInfoChild;
                    rscSet = cphi.getRscSetConnectionDataOrder().getRscSet1();
                }
                final PcmkRscSetsInfo prsi = cphi.getPcmkRscSetsInfo();

                CRM.setRscSet(dcHost,
                              null,
                              false,
                              ordId,
                              false,
                              null,
                              prsi.getAllAttributes(dcHost, rscSet, attrs, false, runMode),
                              prsi.getOrderAttributes(ordId),
                              runMode);
            } else {
                CRM.addOrder(dcHost,
                             ordId,
                             serviceInfoParent.getHeartbeatId(runMode),
                             serviceInfoChild.getHeartbeatId(runMode),
                             attrs,
                             runMode);
            }
            if (Application.isLive(runMode)) {
                storeComboBoxValues(params);
            }
        }
    }

    /** Returns service that belongs to this info object. */
    @Override
    public Service getService() {
        return (Service) getResource();
    }

    @Override
    public String getRsc1Name() {
        return "first";
    }

    @Override
    public String getRsc2Name() {
        return "then";
    }

    @Override
    public String getRsc1() {
        return serviceInfoParent.toString();
    }

    @Override
    public String getRsc2() {
        return serviceInfoChild.toString();
    }

    @Override
    public ServiceInfo getRscInfo1() {
        return serviceInfoParent;
    }

    @Override
    public ServiceInfo getRscInfo2() {
        return serviceInfoChild;
    }

    @Override
    protected boolean isAdvanced(final String param) {
        return !CrmXml.SCORE_CONSTRAINT_PARAM.equals(param);
    }

    @Override
    protected String isEnabled(final String param) {
        if (CrmXml.REQUIRE_ALL_ATTR.equals(param)) {
            final String pmV = getBrowser().getDCHost().getHostParser().getPacemakerVersion();
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

    @Override
    protected AccessMode.Type getAccessType(final String param) {
        return AccessMode.ADMIN;
    }

    int getScore() {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        final String ordId = getService().getCrmId();
        final CrmXml.OrderData data = clStatus.getOrderData(ordId);
        if (data == null) {
            return 0;
        }
        final String score = data.getScore();
        if (score == null) {
            return 0;
        } else if (CrmXml.INFINITY_VALUE.getValueForConfig().equals(score)
                   || CrmXml.PLUS_INFINITY_VALUE.getValueForConfig().equals(score)) {
            return 1000000;
        } else if (CrmXml.MINUS_INFINITY_VALUE.getValueForConfig().equals(score)) {
            return -1000000;
        }
        return Integer.parseInt(score);
    }

    @Override
    protected AccessMode.Mode isEnabledOnlyInAdvancedMode(final String param) {
         return AccessMode.NORMAL;
    }

    @Override
    public Check checkResourceFields(final String param, final String[] params) {
        return checkResourceFields(param, params, false);
    }

    @Override
    public Check checkResourceFields(final String param, final String[] params, final boolean fromUp) {
        if (fromUp) {
            return super.checkResourceFields(param, params);
        } else {
            return connectionInfo.checkResourceFields(param, null);
        }
    }
}
