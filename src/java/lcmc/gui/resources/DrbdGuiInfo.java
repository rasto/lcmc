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

import lcmc.Exceptions;
import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.GuiComboBox;
import lcmc.data.Cluster;
import lcmc.data.DrbdXML;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.data.Host;
import lcmc.utilities.Tools;
import lcmc.utilities.Unit;

import javax.swing.ImageIcon;

/**
 * this class holds info data, menus and configuration
 * for a drbd resource.
 */
abstract class DrbdGuiInfo extends EditableInfo {
    /** Name of the drbd after parameter. */
    protected static final String DRBD_RES_PARAM_AFTER = "resync-after";
    /** Name of the drbd after parameter. Before 8.4 */
    protected static final String DRBD_RES_PARAM_AFTER_8_3 = "after";
    /** Prepares a new <code>DrbdGuiInfo</code> object. */
    public DrbdGuiInfo(final String name, final Browser browser) {
        super(name, browser);
    }

    /** Returns browser object of this info. */
    @Override public final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** Returns cluster object to resource belongs. */
    public final Cluster getCluster() {
        return getBrowser().getCluster();
    }

    /** Returns the DrbdInfo object (for all drbds). */
    public final DrbdInfo getDrbdInfo() {
        return getBrowser().getDrbdGraph().getDrbdInfo();
    }

    /** Returns the regexp of the parameter. */
    @Override protected String getParamRegexp(final String param) {
        return null;
    }

    /**
     * Checks the new value of the parameter if it is conforms to its type
     * and other constraints.
     */
    @Override protected boolean checkParam(final String param,
                                       final String newValue) {
        return getBrowser().getDrbdXML().checkParam(param, newValue);
    }

    /** Returns default value of the parameter. */
    @Override public String getParamDefault(final String param) {
        return getBrowser().getDrbdXML().getParamDefault(param);
    }

    /** Whether the parameter should be enabled. */
    @Override protected String isEnabled(final String param) {
        return null;
    }


    /** Returns the preferred value for the drbd parameter. */
    @Override protected final String getParamPreferred(final String param) {
        return getBrowser().getDrbdXML().getParamPreferred(param);
    }

    /** Returns the possible values for the pulldown menus, if applicable. */
    @Override protected final Object[] getParamPossibleChoices(
                                                        final String param) {
        return getBrowser().getDrbdXML().getPossibleChoices(param);
    }

    /**
     * Returns the short description of the drbd parameter that is used as
     * a label.
     */
    @Override protected final String getParamShortDesc(final String param) {
        return getBrowser().getDrbdXML().getParamShortDesc(param);
    }

    /**
     * Returns a long description of the parameter that is used for tool tip.
     */
    @Override protected final String getParamLongDesc(final String param) {
        return getBrowser().getDrbdXML().getParamLongDesc(param);
    }

    /** Returns whether this drbd parameter is required parameter. */
    @Override protected final boolean isRequired(final String param) {
        return getBrowser().getDrbdXML().isRequired(param);
    }

    /** Returns whether this parameter is advanced. */
    @Override protected boolean isAdvanced(final String param) {
        if (!Tools.areEqual(getParamDefault(param),
                            getParamSaved(param))) {
            /* it changed, show it */
            return false;
        }
        return getBrowser().getDrbdXML().isAdvanced(param);
    }

    /** Returns access type of this parameter. */
    @Override protected final ConfigData.AccessType getAccessType(
                                                         final String param) {
        return getBrowser().getDrbdXML().getAccessType(param);
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override protected final boolean isEnabledOnlyInAdvancedMode(
                                                         final String param) {
        return false;
    }

    /** Returns whether this drbd parameter is of integer type. */
    @Override protected final boolean isInteger(final String param) {
        return getBrowser().getDrbdXML().isInteger(param);
    }

    /** Returns whether this drbd parameter is of label type. */
    @Override protected final boolean isLabel(final String param) {
        return getBrowser().getDrbdXML().isLabel(param);
    }

    /** Returns whether this drbd parameter is of time type. */
    @Override protected final boolean isTimeType(final String param) {
        /* not required */
        return false;
    }

    /** Returns whether this parameter has a unit prefix. */
    @Override protected final boolean hasUnitPrefix(final String param) {
        return getBrowser().getDrbdXML().hasUnitPrefix(param);
    }

    /** Returns the long unit name. */
    protected final String getUnitLong(final String param) {
        return getBrowser().getDrbdXML().getUnitLong(param);
    }

    /**
     * Returns the default unit for the parameter.
     */
    protected final String getDefaultUnit(final String param) {
        return getBrowser().getDrbdXML().getDefaultUnit(param);
    }

    /**
     * Returns whether the parameter is of the boolean type and needs the
     * checkbox.
     */
    @Override protected final boolean isCheckBox(final String param) {
        final String type = getBrowser().getDrbdXML().getParamType(param);
        if (type == null) {
            return false;
        }
        if (ClusterBrowser.DRBD_RES_BOOL_TYPE_NAME.equals(type)) {
            return true;
        }
        return false;
    }

    /** Returns the type of the parameter (like boolean). */
    @Override protected final String getParamType(final String param) {
        return getBrowser().getDrbdXML().getParamType(param);
    }

    /**
     * Returns the widget that is used to edit this parameter.
     */
    @Override protected GuiComboBox getParamComboBox(final String param,
                                                     final String prefix,
                                                     final int width) {
        GuiComboBox paramCb;
        final Object[] possibleChoices = getParamPossibleChoices(param);
        getResource().setPossibleChoices(param, possibleChoices);
        if (hasUnitPrefix(param)) {
            String selectedValue = getParamSaved(param);
            if (selectedValue == null) {
                selectedValue = getParamPreferred(param);
                if (selectedValue == null) {
                    selectedValue = getParamDefault(param);
                }
            }
            String unit = getUnitLong(param);
            if (unit == null) {
                unit = "";
            }

            final int index = unit.indexOf('/');
            String unitPart = "";
            if (index > -1) {
                unitPart = unit.substring(index);
            }
            GuiComboBox.Type type = null;
            Unit[] units = null;
            if ("".equals(unit)) {
                units = new Unit[]{
                    new Unit("", "", "", ""),

                    new Unit("k",
                             "K",
                             "k",
                             "k"),

                    new Unit("m",
                             "M",
                             "m",
                             "m"),

                    new Unit("g",
                             "G",
                             "g",
                             "g")
                };
            } else {
                units = new Unit[]{
                    new Unit("", "", "Byte", "Bytes"),

                    new Unit("K",
                             "k",
                             "KiByte" + unitPart,
                             "KiBytes" + unitPart),

                    new Unit("M",
                             "m",
                             "MiByte" + unitPart,
                             "MiBytes" + unitPart),

                    new Unit("G",
                             "g",
                             "GiByte" + unitPart,
                             "GiBytes" + unitPart),

                    new Unit("s",
                             "s",
                             "Sector" + unitPart,
                             "Sectors" + unitPart)
                };
            }

            paramCb = new GuiComboBox(selectedValue,
                                      getPossibleChoices(param),
                                      units,
                                      GuiComboBox.Type.TEXTFIELDWITHUNIT,
                                      null, /* regexp */
                                      width,
                                      null, /* abbrv */
                                      new AccessMode(
                                           getAccessType(param),
                                           isEnabledOnlyInAdvancedMode(param)));

            paramComboBoxAdd(param, prefix, paramCb);
        } else {
            paramCb = super.getParamComboBox(param, prefix, width);
            if (possibleChoices != null
                && !getBrowser().getDrbdXML().isStringType(param)) {
                paramCb.setEditable(false);
            }
        }
        return paramCb;
    }

    /**
     * Creates drbd config for sections and returns it. Removes 'drbd: '
     * from the 'after' parameter.
     */
    protected String drbdSectionsConfig(final Host host)
                     throws Exceptions.DrbdConfigException {
        final StringBuilder config = new StringBuilder("");
        final DrbdXML dxml = getBrowser().getDrbdXML();
        final String[] sections = dxml.getSections();
        final boolean volumesAvailable = host.hasVolumes();
        for (final String sectionString : sections) {
            /* remove -options */
            final String section = sectionString.replaceAll("-options$", "");
            if ("resource".equals(section)
                || DrbdXML.GLOBAL_SECTION.equals(section)) {
                continue;
            }
            final String[] params = dxml.getSectionParams(sectionString);

            if (params.length != 0) {
                final StringBuilder sectionConfig = new StringBuilder("");
                for (String param : params) {
                    final String value = getComboBoxValue(param);
                    if (value == null) {
                        continue;
                    }
                    if (!value.equals(getParamDefault(param))) {
                        if (!volumesAvailable
                            && (isCheckBox(param)
                                || "booleanhandler".equals(
                                                        getParamType(param)))) {
                            if (value.equals(DrbdXML.CONFIG_YES)) {
                                /* boolean parameter */
                                sectionConfig.append("\t\t" + param + ";\n");
                            }
                        } else if (DRBD_RES_PARAM_AFTER.equals(param)) {
                            /* resync-after parameter > 8.4 */
                            if (!value.equals(Tools.getString(
                                                "ClusterBrowser.None"))) {
                                if (value != null) {
                                    sectionConfig.append("\t\t");
                                    sectionConfig.append(param);
                                    sectionConfig.append('\t');
                                    sectionConfig.append(
                                                    Tools.escapeConfig(value));
                                    sectionConfig.append(";\n");
                                }
                            }
                        } else if (DRBD_RES_PARAM_AFTER_8_3.equals(param)) {
                            /* after parameter < 8.4 */
                            /* we get drbd device here, so it is converted
                             * to the resource. */
                            if (!value.equals(Tools.getString(
                                                "ClusterBrowser.None"))) {
                                final DrbdVolumeInfo v0 =
                                     getBrowser().getDrbdDevHash().get(value);
                                getBrowser().putDrbdDevHash();
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
                            sectionConfig.append("\t\t");
                            sectionConfig.append(param);
                            sectionConfig.append('\t');
                            sectionConfig.append(Tools.escapeConfig(value));
                            sectionConfig.append(";\n");
                        }
                    }
                }

                if (sectionConfig.length() > 0) {
                    config.append("\t" + section + " {\n");
                    config.append(sectionConfig);
                    config.append("\t}\n\n");
                }
            }
        }
        return config.toString();
    }
}
