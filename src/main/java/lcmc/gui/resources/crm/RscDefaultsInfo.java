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

import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.resources.EditableInfo;
import lcmc.gui.widget.Widget;
import lcmc.data.resources.Resource;
import lcmc.data.CRMXML;
import lcmc.data.ClusterStatus;
import lcmc.data.Application;
import lcmc.utilities.Tools;

import java.util.Collection;
import java.util.Map;

import javax.swing.JPanel;

import lcmc.data.StringValue;
import lcmc.data.Value;
import lcmc.gui.widget.Check;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class is for resource defaults or rsc_defaults.
 */
public final class RscDefaultsInfo extends EditableInfo {
    /** Logger. */
    private static final Logger LOG =
                              LoggerFactory.getLogger(RscDefaultsInfo.class);
    /**
     * Prepares a new {@code RscDefaultsInfo} object and creates
     * new rsc defaults object.
     */
    public RscDefaultsInfo(final String name,
                           final Browser browser) {
        super(name, browser);
        setResource(new Resource(name));
    }

    /** Returns browser object of this info. */
    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** Sets default parameters with values from resourceNode hash. */
    public void setParameters(final Map<String, String> resourceNode) {
        if (resourceNode == null) {
            return;
        }
        final CRMXML crmXML = getBrowser().getCRMXML();
        if (crmXML == null) {
            LOG.appError("setParameters: crmXML is null");
            return;
        }
        /* Attributes */
        final String[] params = getParametersFromXML();
        if (params != null) {
            for (final String param : params) {
                Value value = new StringValue(resourceNode.get(param));
                final Value defaultValue = getParamDefault(param);
                if (value.isNothingSelected()) {
                    value = defaultValue;
                }
                final Value oldValue = getParamSaved(param);
                final Widget wi = getWidget(param, null);
                final boolean haveChanged =
                   !Tools.areEqual(value, oldValue)
                   || !Tools.areEqual(defaultValue,
                                      getResource().getDefaultValue(param));
                if (haveChanged) {
                    getResource().setValue(param, value);
                    getResource().setDefaultValue(param, defaultValue);
                    if (wi != null) {
                        wi.setValue(value);
                    }
                }
            }
        }
    }

    /** Returns parameters. */
    @Override
    public String[] getParametersFromXML() {
        LOG.debug1("getParametersFromXML: start");
        final CRMXML crmXML = getBrowser().getCRMXML();
        if (crmXML == null) {
            return null;
        }
        final Collection<String> params =
                                   crmXML.getRscDefaultsParameters().keySet();
        return params.toArray(new String[params.size()]);
    }

    /** Returns true if the value of the parameter is ok. */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        if (newValue == null) {
            return crmXML.checkMetaAttrParam(param, null);
        } else {
            return crmXML.checkMetaAttrParam(param, newValue);
        }
    }

    /** Returns default value for specified parameter. */
    @Override
    protected Value getParamDefault(final String param) {
        if ("resource-stickiness".equals(param)) {
            return getBrowser().getServicesInfo().getResource().getValue(
                                                "default-resource-stickiness");
        }
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getRscDefaultsDefault(param);
    }

    /** Returns saved value for specified parameter. */
    @Override
    public Value getParamSaved(final String param) {
        final ClusterStatus clStatus = getBrowser().getClusterStatus();
        Value value = super.getParamSaved(param);
        if (value == null) {
            value = new StringValue(clStatus.getRscDefaultsParameter(param, Application.RunMode.LIVE));
            if (value.isNothingSelected()) {
                value = getParamPreferred(param);
                if (value == null) {
                    return getParamDefault(param);
                }
            }
        }
        return value;
    }

    /** Returns preferred value for specified parameter. */
    @Override
    protected Value getParamPreferred(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getRscDefaultsPreferred(param);
    }

    /** Returns possible choices for drop down lists. */
    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        if (isCheckBox(param)) {
            return crmXML.getRscDefaultsCheckBoxChoices(param);
        } else {
            return crmXML.getRscDefaultsPossibleChoices(param);
        }
    }

    /** Returns short description of the specified parameter. */
    @Override
    protected String getParamShortDesc(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getRscDefaultsShortDesc(param);
    }

    /** Returns long description of the specified parameter. */
    @Override
    protected String getParamLongDesc(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getRscDefaultsLongDesc(param);
    }

    /** Returns section to which the specified parameter belongs. */
    @Override
    protected String getSection(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getRscDefaultsSection(param);
    }

    /** Returns true if the specified parameter is advanced. */
    @Override
    protected boolean isAdvanced(final String param) {
        if (!Tools.areEqual(getParamDefault(param),
                            getParamSaved(param))) {
            /* it changed, show it */
            return false;
        }
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isRscDefaultsAdvanced(param);
    }

    /** Returns access type of this parameter. */
    @Override
    protected Application.AccessType getAccessType(final String param) {
        return getBrowser().getCRMXML().getRscDefaultsAccessType(param);
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override
    protected boolean isEnabledOnlyInAdvancedMode(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is required. */
    @Override
    protected boolean isRequired(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isRscDefaultsRequired(param);
    }

    /** Returns true if the specified parameter is integer. */
    @Override
    protected boolean isInteger(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isRscDefaultsInteger(param);
    }

    /** Returns true if the specified parameter is label. */
    @Override
    protected boolean isLabel(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isRscDefaultsLabel(param);
    }

    /** Returns true if the specified parameter is of time type. */
    @Override
    protected boolean isTimeType(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isRscDefaultsTimeType(param);
    }

    /** Returns whether parameter is checkbox. */
    @Override
    protected boolean isCheckBox(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.isRscDefaultsBoolean(param);
    }

    /** Returns the type of the parameter according to the OCF. */
    @Override
    protected String getParamType(final String param) {
        final CRMXML crmXML = getBrowser().getCRMXML();
        return crmXML.getRscDefaultsType(param);
    }

    /** Returns panel with graph. */
    @Override
    public JPanel getGraphicalView() {
        return getBrowser().getCRMGraph().getGraphPanel();
    }

    /** Check the fields. */
    @Override
    public Check checkResourceFields(final String param, final String[] params) {
        return checkResourceFields(param, params, false);
    }

    /** Check the fields. */
    Check checkResourceFields(final String param,
                              final String[] params,
                              final boolean fromServicesInfo) {
        if (fromServicesInfo) {
            return super.checkResourceFields(param, params);
        } else {
            final ServicesInfo ssi = getBrowser().getServicesInfo();
            return ssi.checkResourceFields(param, ssi.getParametersFromXML());
        }
    }
}
