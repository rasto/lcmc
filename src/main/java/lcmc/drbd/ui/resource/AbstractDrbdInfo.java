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
package lcmc.drbd.ui.resource;

import javax.inject.Inject;
import javax.inject.Named;

import lcmc.cluster.domain.Cluster;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Unit;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.EditableInfo;
import lcmc.drbd.domain.DrbdProxy;
import lcmc.drbd.domain.DrbdXml;
import lcmc.host.domain.Host;

/**
 * this class holds info data, menus and configuration
 * for a drbd resource.
 */
@Named
public abstract class AbstractDrbdInfo extends EditableInfo {
    protected static final String DRBD_RES_PARAM_AFTER = "resync-after";
    protected static final String DRBD_RES_PARAM_AFTER_8_3 = "after";
    @Inject
    private WidgetFactory widgetFactory;

    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    public final Cluster getCluster() {
        return getBrowser().getCluster();
    }

    @Override
    protected String getParamRegexp(final String param) {
        return null;
    }

    /**
     * Checks the new value of the parameter if it is conforms to its type
     * and other constraints.
     */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        return getBrowser().getDrbdXml().checkParam(param, newValue);
    }

    @Override
    public Value getParamDefault(final String param) {
        return getBrowser().getDrbdXml().getParamDefault(param);
    }

    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    @Override
    protected final Value getParamPreferred(final String param) {
        return getBrowser().getDrbdXml().getParamPreferred(param);
    }

    @Override
    protected final Value[] getParamPossibleChoices(final String param) {
        return getBrowser().getDrbdXml().getPossibleChoices(param);
    }

    @Override
    protected final String getParamShortDesc(final String param) {
        return getBrowser().getDrbdXml().getParamShortDesc(param);
    }

    @Override
    protected final String getParamLongDesc(final String param) {
        return getBrowser().getDrbdXml().getParamLongDesc(param);
    }

    @Override
    protected final boolean isRequired(final String param) {
        return getBrowser().getDrbdXml().isRequired(param);
    }

    @Override
    protected boolean isAdvanced(final String param) {
        if (!Tools.areEqual(getParamDefault(param), getParamSaved(param))) {
            /* it changed, show it */
            return false;
        }
        return getBrowser().getDrbdXml().isAdvanced(param);
    }

    @Override
    protected final AccessMode.Type getAccessType(final String param) {
        return getBrowser().getDrbdXml().getAccessType(param);
    }

    @Override
    protected final AccessMode.Mode isEnabledOnlyInAdvancedMode(final String param) {
        return AccessMode.NORMAL;
    }

    @Override
    protected final boolean isInteger(final String param) {
        return getBrowser().getDrbdXml().isInteger(param);
    }

    @Override
    protected final boolean isLabel(final String param) {
        return getBrowser().getDrbdXml().isLabel(param);
    }

    @Override
    protected final boolean isTimeType(final String param) {
        /* not required */
        return false;
    }

    @Override
    protected final boolean hasUnitPrefix(final String param) {
        return getBrowser().getDrbdXml().hasUnitPrefix(param);
    }

    protected final String getUnitLong(final String param) {
        return getBrowser().getDrbdXml().getUnitLong(param);
    }

    protected final String getDefaultUnit(final String param) {
        return getBrowser().getDrbdXml().getDefaultUnit(param);
    }

    @Override
    protected final boolean isCheckBox(final String param) {
        final String type = getBrowser().getDrbdXml().getParamType(param);
        return ClusterBrowser.DRBD_RESOURCE_BOOL_TYPE_NAME.equals(type);
    }

    @Override
    protected final String getParamType(final String param) {
        return getBrowser().getDrbdXml().getParamType(param);
    }

    /**
     * Returns the widget that is used to edit this parameter.
     */
    @Override
    protected Widget createWidget(final String param, final String prefix, final int width) {
        final Value[] possibleChoices = getParamPossibleChoices(param);
        getResource().setPossibleChoices(param, possibleChoices);
        final Widget paramWi;
        if (hasUnitPrefix(param)) {
            Value selectedValue = getParamSaved(param);
            if (selectedValue == null) {
                selectedValue = getParamPreferred(param);
                if (selectedValue == null) {
                    selectedValue = getParamDefault(param);
                }
            }
            final Unit[] units = getUnits(param);
            paramWi = widgetFactory.createInstance(
                                 Widget.Type.TEXTFIELDWITHUNIT,
                                 selectedValue,
                                 getPossibleChoices(param),
                                 units,
                                 Widget.NO_REGEXP,
                                 width,
                                 Widget.NO_ABBRV,
                                 new AccessMode(getAccessType(param), isEnabledOnlyInAdvancedMode(param)),
                                 Widget.NO_BUTTON);

            widgetAdd(param, prefix, paramWi);
        } else {
            paramWi = super.createWidget(param, prefix, width);
            if (possibleChoices != null
                && !getBrowser().getDrbdXml().isStringType(param)) {
                paramWi.setEditable(false);
            }
        }
        return paramWi;
    }

    /**
     * Creates drbd config for sections and returns it. Removes 'drbd: ' from the 'after' parameter.
     */
    protected String drbdSectionsConfig(final Host host) {
        final StringBuilder config = new StringBuilder();
        final DrbdXml dxml = getBrowser().getDrbdXml();
        final String[] sections = dxml.getSections();
        final boolean volumesAvailable = host.hasVolumes();
        for (final String sectionString : sections) {
            if (!isSectionEnabled(sectionString)) {
                continue;
            }
            /* remove -options */
            final String section = sectionString.replaceAll("-options$", "");
            if ("resource".equals(section) || DrbdXml.GLOBAL_SECTION.equals(section)) {
                continue;
            }
            final String[] params = dxml.getSectionParams(sectionString);

            if (params.length != 0) {
                final StringBuilder sectionConfig = new StringBuilder();
                boolean inPlugin = false;
                for (final String param : params) {
                    final Value value = getComboBoxValue(param);
                    if (value == null || value.isNothingSelected()) {
                        continue;
                    }
                    if (!value.equals(getParamDefault(param))) {
                        if (param.startsWith(DrbdProxy.PLUGIN_PARAM_PREFIX)) {
                            if (!inPlugin) {
                                sectionConfig.append("\t\tplugin {\n");
                                inPlugin = true;
                            }
                            sectionConfig.append("\t\t\t");
                            sectionConfig.append(param.substring(DrbdProxy.PLUGIN_PARAM_PREFIX.length()));
                            if (value.equals(DrbdXml.CONFIG_YES)) {
                                /* boolean parameter */
                                /* also >= DRBD 8.4 */
                                sectionConfig.append(";\n");
                            } else {
                                sectionConfig.append(' ');
                                sectionConfig.append(Tools.escapeConfig(value.getValueForConfig()));
                                sectionConfig.append(";\n");
                            }
                        } else if (!volumesAvailable
                            && (isCheckBox(param) || "booleanhandler".equals(getParamType(param)))) {
                            if (value.equals(DrbdXml.CONFIG_YES)) {
                                /* boolean parameter */
                                sectionConfig.append("\t\t").append(param).append(";\n");
                            }
                        } else if (DRBD_RES_PARAM_AFTER.equals(param)) {
                            /* resync-after parameter > 8.4 */
                            if (!value.getValueForConfig().equals(Tools.getString("ClusterBrowser.None"))) {
                                sectionConfig.append("\t\t");
                                sectionConfig.append(param);
                                sectionConfig.append('\t');
                                sectionConfig.append(Tools.escapeConfig(value.getValueForConfigWithUnit()));
                                sectionConfig.append(";\n");
                            }
                        } else if (DRBD_RES_PARAM_AFTER_8_3.equals(param)) {
                            /* after parameter < 8.4 */
                            /* we get drbd device here, so it is converted
                             * to the resource. */
                            if (!value.getValueForConfig().equals(Tools.getString("ClusterBrowser.None"))) {
                                final ResourceInfo v0 =
                                     getBrowser().getDrbdResourceNameHash().get(value.getValueForConfig());
                                getBrowser().putDrbdResHash();
                                if (v0 != null) {
                                    final String v = v0.getName();
                                    sectionConfig.append("\t\t");
                                    sectionConfig.append(param);
                                    sectionConfig.append('\t');
                                    sectionConfig.append(Tools.escapeConfig(v));
                                    sectionConfig.append(";\n");
                                }
                            }
                        } else { /* name value parameter */
                            if (inPlugin) {
                                sectionConfig.append("\t\t}\n");
                                inPlugin = false;
                            }
                            sectionConfig.append("\t\t");
                            sectionConfig.append(param);
                            sectionConfig.append('\t');
                            sectionConfig.append(Tools.escapeConfig(value.getValueForConfigWithUnit()));
                            sectionConfig.append(";\n");
                        }
                    }
                }

                if (inPlugin) {
                    sectionConfig.append("\t\t}\n");
                }

                if (sectionConfig.length() > 0) {
                    config.append('\t').append(section).append(" {\n");
                    config.append(sectionConfig);
                    config.append("\t}\n\n");
                }
            }
        }
        return config.toString();
    }

    @Override
    protected final Unit[] getUnits(final String param) {
        final String unitLong = getUnitLong(param);
        final String unitPart = DrbdXml.getUnitPart(unitLong);
        if ("".equals(unitPart)) {
            return getBrowser().getDrbdXml().getUnits(param, unitPart);
        } else {
            return getBrowser().getDrbdXml().getByteUnits(param, unitPart);
        }
    }
}
