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
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.ResourceValue;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Access;
import lcmc.common.ui.Browser;
import lcmc.common.ui.EditableInfo;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.crm.domain.ClusterStatus;
import lcmc.crm.domain.CrmXml;
import lcmc.crm.domain.Service;
import lcmc.crm.service.CRM;
import lcmc.host.domain.Host;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Object that holds a colocation constraint information.
 */
final class HbColocationInfo extends EditableInfo implements HbConstraintInterface {
    private ServiceInfo serviceInfoRsc;
    private ServiceInfo serviceInfoWithRsc;
    private HbConnectionInfo connectionInfo;

    public HbColocationInfo(Application application, SwingUtils swingUtils, Access access, MainData mainData, WidgetFactory widgetFactory) {
        super(application, swingUtils, access, mainData, widgetFactory);
    }

    void init(final HbConnectionInfo connectionInfo,
              final ServiceInfo serviceInfoRsc,
              final ServiceInfo serviceInfoWithRsc,
              final Browser browser) {
        super.einit(Optional.<ResourceValue>of(new Service("Colocation")), "Colocation", browser);
        this.connectionInfo = connectionInfo;
        this.serviceInfoRsc = serviceInfoRsc;
        this.serviceInfoWithRsc = serviceInfoWithRsc;
    }

    void setServiceInfoRsc(final ServiceInfo serviceInfoRsc) {
        this.serviceInfoRsc = serviceInfoRsc;
    }

    void setServiceInfoWithRsc(final ServiceInfo serviceInfoWithRsc) {
        this.serviceInfoWithRsc = serviceInfoWithRsc;
    }

    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    void setParameters() {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        final String colId = getService().getCrmId();
        final Map<String, Value> resourceNode = new HashMap<String, Value>();

        if (serviceInfoRsc == null || serviceInfoWithRsc == null) {
            /* rsc set placeholder */
            final CrmXml.ColocationData colocationData = clStatus.getColocationData(colId);
            final String score = colocationData.getScore();
            resourceNode.put(CrmXml.SCORE_CONSTRAINT_PARAM, new StringValue(score));
        } else if (serviceInfoRsc.isConstraintPlaceholder()
                   || serviceInfoWithRsc.isConstraintPlaceholder()) {
            /* rsc set edge */
            final ConstraintPHInfo cphi;
            final CrmXml.RscSet rscSet;
            if (serviceInfoRsc.isConstraintPlaceholder()) {
                cphi = (ConstraintPHInfo) serviceInfoRsc;
                rscSet = cphi.getRscSetConnectionDataColocation().getRscSet1();
            } else {
                cphi = (ConstraintPHInfo) serviceInfoWithRsc;
                rscSet = cphi.getRscSetConnectionDataColocation().getRscSet2();
            }
            resourceNode.put("sequential", new StringValue(rscSet.getSequential()));
            resourceNode.put("role", new StringValue(rscSet.getColocationRole()));
        } else {
            final CrmXml.ColocationData colocationData = clStatus.getColocationData(colId);
            if (colocationData != null) {
                final String score = colocationData.getScore();
                final String rscRole = colocationData.getRscRole();
                final String withRscRole = colocationData.getWithRscRole();

                resourceNode.put(CrmXml.SCORE_CONSTRAINT_PARAM, new StringValue(score));
                resourceNode.put("rsc-role", new StringValue(rscRole));
                resourceNode.put("with-rsc-role", new StringValue(withRscRole));
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
        return false;
    }

    /**
     * Returns long description of the parameter, that is used for
     * tool tips.
     */
    @Override
    protected String getParamLongDesc(final String param) {
        final String text = getBrowser().getCrmXml().getColocationParamLongDesc(param);
        if (serviceInfoRsc != null && serviceInfoWithRsc != null) {
            return text.replaceAll("@RSC@", Matcher.quoteReplacement(serviceInfoRsc.toString()))
                       .replaceAll("@WITH-RSC@", Matcher.quoteReplacement(serviceInfoWithRsc.toString()));
        }
        return text;
    }

    /** Returns short description of the parameter, that is used as * label. */
    @Override
    protected String getParamShortDesc(final String param) {
        return getBrowser().getCrmXml().getColocationParamShortDesc(param);
    }

    /**
     * Checks if the new value is correct for the parameter type and
     * constraints.
     */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        return getBrowser().getCrmXml().checkColocationParam(param, newValue);
    }

    @Override
    protected Value getParamDefault(final String param) {
        return getBrowser().getCrmXml().getColocationParamDefault(param);
    }

    @Override
    protected Value getParamPreferred(final String param) {
        return getBrowser().getCrmXml().getColocationParamPreferred(param);
    }

    @Override
    public String[] getParametersFromXML() {
        if (serviceInfoRsc == null || serviceInfoWithRsc == null) {
            /* rsc set colocation */
            return getBrowser().getCrmXml().getResourceSetColocationParameters();
        } else if (serviceInfoRsc.isConstraintPlaceholder() || serviceInfoWithRsc.isConstraintPlaceholder()) {
            /* when rsc set edges are clicked */
            return getBrowser().getCrmXml().getResourceSetColConnectionParameters();
        } else {
            return getBrowser().getCrmXml().getColocationParameters();
        }
    }

    /** Returns when at least one resource in rsc set can be promoted. */
    private boolean isRscSetMaster() {
        final ConstraintPHInfo cphi;
        final CrmXml.RscSet rscSet;
        if (serviceInfoRsc.isConstraintPlaceholder()) {
            cphi = (ConstraintPHInfo) serviceInfoRsc;
            rscSet = cphi.getRscSetConnectionDataColocation().getRscSet1();
        } else {
            cphi = (ConstraintPHInfo) serviceInfoWithRsc;
            rscSet = cphi.getRscSetConnectionDataColocation().getRscSet2();
        }
        return rscSet != null && getBrowser().isOneMaster(rscSet.getRscIds());
    }

    /**
     * Possible choices for pulldown menus, or null if it is not a pull
     * down menu.
     */
    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        if ("role".equals(param)) {
            return getBrowser().getCrmXml().getColocationParamComboBoxChoices(param, isRscSetMaster());
        } else if ("with-rsc-role".equals(param)) {
            return getBrowser().getCrmXml().getColocationParamComboBoxChoices(param,
                                                                        serviceInfoWithRsc.getService().isMaster());
        } else if ("rsc-role".equals(param)) {
            return getBrowser().getCrmXml().getColocationParamComboBoxChoices(param,
                                                                              serviceInfoRsc.getService().isMaster());
        } else {
            return getBrowser().getCrmXml().getColocationParamComboBoxChoices(param, false);
        }
    }

    /** Returns parameter type, boolean etc. */
    @Override
    protected String getParamType(final String param) {
        return getBrowser().getCrmXml().getColocationParamType(param);
    }

    /** Returns section to which the global belongs. */
    @Override
    protected String getSection(final String param) {
        return getBrowser().getCrmXml().getColocationSectionForDisplay(param);
    }

    /**
     * Returns whether the parameter is of the boolean type and needs the
     * checkbox.
     */
    @Override
    protected boolean isCheckBox(final String param) {
        return getBrowser().getCrmXml().isColocationBoolean(param);
    }

    /** Returns true if the specified parameter is of time type. */
    @Override
    protected boolean isTimeType(final String param) {
        return getBrowser().getCrmXml().isColocationTimeType(param);
    }

    /** Returns true if the specified parameter is integer. */
    @Override
    protected boolean isInteger(final String param) {
        return getBrowser().getCrmXml().isColocationInteger(param);
    }

    /** Returns true if the specified parameter is label. */
    @Override
    protected boolean isLabel(final String param) {
        return getBrowser().getCrmXml().isColocationLabel(param);
    }

    /** Returns true if the specified parameter is required. */
    @Override
    protected boolean isRequired(final String param) {
        return getBrowser().getCrmXml().isColocationRequired(param);
    }

    /** Returns attributes of this colocation. */
    protected Map<String, String> getAttributes() {
        final String[] params = getParametersFromXML();
        final Map<String, String> attrs = new LinkedHashMap<String, String>();
        for (final String param : params) {
            final Value value = getComboBoxValue(param);
            if (value != null) {
                attrs.put(param, value.getValueForConfig());
            }
        }
        return attrs;
    }

    /** Applies changes to the colocation parameters. */
    @Override
    public void apply(final Host dcHost, final Application.RunMode runMode) {
        final String[] params = getParametersFromXML();
        final Map<String, String> attrs = new LinkedHashMap<String, String>();
        boolean changed = true;
        for (final String param : params) {
            final Value value = getComboBoxValue(param);
            if (!Tools.areEqual(value, getParamSaved(param))) {
                changed = true;
            }
            if (value != null) {
                attrs.put(param, value.getValueForConfig());
            }
        }
        if (changed) {
            final String colId = getService().getCrmId();
            if (serviceInfoRsc == null || serviceInfoWithRsc == null) {
                /* placeholder */
                final PcmkRscSetsInfo prsi = (PcmkRscSetsInfo) connectionInfo;
                CRM.setRscSet(dcHost,
                              colId,
                              false,
                              null,
                              false,
                              prsi.getAllAttributes(dcHost, null, null, true, runMode),
                              null,
                              attrs,
                              runMode);
            } else if (serviceInfoRsc.isConstraintPlaceholder() || serviceInfoWithRsc.isConstraintPlaceholder()) {
                /* edge */
                final ConstraintPHInfo cphi;
                final CrmXml.RscSet rscSet;
                if (serviceInfoRsc.isConstraintPlaceholder()) {
                    cphi = (ConstraintPHInfo) serviceInfoRsc;
                    rscSet = cphi.getRscSetConnectionDataColocation().getRscSet1();
                } else {
                    cphi = (ConstraintPHInfo) serviceInfoWithRsc;
                    rscSet = cphi.getRscSetConnectionDataColocation().getRscSet2();
                }
                final PcmkRscSetsInfo prsi = cphi.getPcmkRscSetsInfo();

                CRM.setRscSet(dcHost,
                              colId,
                              false,
                              null,
                              false,
                              prsi.getAllAttributes(dcHost, rscSet, attrs, true, runMode),
                              null,
                              prsi.getColocationAttributes(colId),
                              runMode);
            } else {
                CRM.addColocation(dcHost,
                                  colId,
                                  serviceInfoRsc.getHeartbeatId(runMode),
                                  serviceInfoWithRsc.getHeartbeatId(runMode),
                                  attrs,
                                  runMode);
            }
        }
        if (Application.isLive(runMode)) {
            storeComboBoxValues(params);
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
        return "rsc";
    }

    /** Returns name of the rsc2 attribute. */
    @Override
    public String getRsc2Name() {
        return "with-rsc";
    }

    /** Resource 1 in colocation constraint. */
    @Override
    public String getRsc1() {
        return serviceInfoRsc.toString();
    }

    /** Resource 2 in colocation constraint. */
    @Override
    public String getRsc2() {
        return serviceInfoWithRsc.toString();
    }

    /** Resource 1 object in colocation constraint. */
    @Override
    public ServiceInfo getRscInfo1() {
        return serviceInfoRsc;
    }

    /** Resource 2 object in colocation constraint. */
    @Override
    public ServiceInfo getRscInfo2() {
        return serviceInfoWithRsc;
    }

    /** Returns the score of this colocation. */
    int getScore() {
        //final String rsc = serviceInfoRsc.getService().getHeartbeatId();
        //final String withRsc =
        //                  serviceInfoWithRsc.getService().getHeartbeatId();
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        final String colId = getService().getCrmId();
        final CrmXml.ColocationData data = clStatus.getColocationData(colId);
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
    protected boolean isAdvanced(final String param) {
        return true;
    }

    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    @Override
    protected AccessMode.Type getAccessType(final String param) {
        return AccessMode.ADMIN;
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
