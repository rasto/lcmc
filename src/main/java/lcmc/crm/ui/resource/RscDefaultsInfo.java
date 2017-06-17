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
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

import javax.inject.Named;
import javax.swing.*;
import java.util.Collection;
import java.util.Map;

/**
 * This class is for resource defaults or rsc_defaults.
 */
@Named
public final class RscDefaultsInfo extends EditableInfo {
    private static final Logger LOG = LoggerFactory.getLogger(RscDefaultsInfo.class);
    public void einit(final String name, final Browser browser) {
        super.einit(Optional.of(new ResourceValue(name)), name, browser);
    }

    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    public void setParameters(final Map<String, String> resourceNode) {
        if (resourceNode == null) {
            return;
        }
        final CrmXml crmXML = getBrowser().getCrmXml();
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
                final boolean haveChanged = !Tools.areEqual(value, oldValue)
                                            || !Tools.areEqual(defaultValue, getResource().getDefaultValue(param));
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

    @Override
    public String[] getParametersFromXML() {
        LOG.debug1("getParametersFromXML: start");
        final CrmXml crmXML = getBrowser().getCrmXml();
        if (crmXML == null) {
            return null;
        }
        final Collection<String> params = crmXML.getRscDefaultsParameters().keySet();
        return params.toArray(new String[params.size()]);
    }

    /** Returns true if the value of the parameter is ok. */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        if (newValue == null) {
            return crmXML.checkMetaAttrParam(param, null);
        } else {
            return crmXML.checkMetaAttrParam(param, newValue);
        }
    }

    @Override
    protected Value getParamDefault(final String param) {
        if ("resource-stickiness".equals(param)) {
            return getBrowser().getServicesInfo().getResource().getValue("default-resource-stickiness");
        }
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.getRscDefaultsMetaAttrDefault(param);
    }

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

    @Override
    protected Value getParamPreferred(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.getRscDefaultsPreferred(param);
    }

    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        if (isCheckBox(param)) {
            return crmXML.getRscDefaultsCheckBoxChoices(param);
        } else {
            return crmXML.getRscDefaultsComboBoxChoices(param);
        }
    }

    @Override
    protected String getParamShortDesc(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.getRscDefaultsMetaAttrShortDesc(param);
    }

    @Override
    protected String getParamLongDesc(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.getRscDefaultsMetaAttrLongDesc(param);
    }

    @Override
    protected String getSection(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.getRscDefaultsMetaAttrSection(param);
    }

    @Override
    protected boolean isAdvanced(final String param) {
        if (!Tools.areEqual(getParamDefault(param), getParamSaved(param))) {
            /* it changed, show it */
            return false;
        }
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.isRscDefaultsAdvanced(param);
    }

    @Override
    protected AccessMode.Type getAccessType(final String param) {
        return getBrowser().getCrmXml().getRscDefaultsMetaAttrAccessType(param);
    }

    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    @Override
    protected AccessMode.Mode isEnabledOnlyInAdvancedMode(final String param) {
        return AccessMode.NORMAL;
    }

    @Override
    protected boolean isRequired(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.isRscDefaultsRequired(param);
    }

    @Override
    protected boolean isInteger(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.isRscDefaultsInteger(param);
    }

    @Override
    protected boolean isLabel(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.isRscDefaultsLabel(param);
    }

    @Override
    protected boolean isTimeType(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.isRscDefaultsTimeType(param);
    }

    @Override
    protected boolean isCheckBox(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.isRscDefaultsMetaAttrBoolean(param);
    }

    @Override
    protected String getParamType(final String param) {
        final CrmXml crmXML = getBrowser().getCrmXml();
        return crmXML.getRscDefaultsMetaAttrType(param);
    }

    @Override
    public JPanel getGraphicalView() {
        return getBrowser().getCrmGraph().getGraphPanel();
    }

    @Override
    public Check checkResourceFields(final String param, final String[] params) {
        return checkResourceFields(param, params, false);
    }

    Check checkResourceFields(final String param, final String[] params, final boolean fromServicesInfo) {
        if (fromServicesInfo) {
            return super.checkResourceFields(param, params);
        } else {
            final ServicesInfo ssi = getBrowser().getServicesInfo();
            return ssi.checkResourceFields(param, ssi.getParametersFromXML());
        }
    }
}
