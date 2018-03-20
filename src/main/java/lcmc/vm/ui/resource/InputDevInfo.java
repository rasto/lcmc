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
package lcmc.vm.ui.resource;

import lcmc.cluster.ui.widget.Widget;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.common.ui.treemenu.ClusterTreeMenu;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.vm.domain.VmsXml;
import lcmc.vm.domain.data.InputDevData;
import org.w3c.dom.Node;

import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.*;
import java.util.*;

/**
 * This class holds info about Virtual input devices.
 */
@Named
final class InputDevInfo extends HardwareInfo {
    /** Parameters. */
    private static final String[] PARAMETERS = {InputDevData.TYPE,
                                                InputDevData.BUS};
    /** Field type. */
    private static final Map<String, Widget.Type> FIELD_TYPES =
                                       new HashMap<String, Widget.Type>();
    /** Short name. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();

    /** Whether the parameter is editable only in advanced mode. */
    private static final Collection<String> IS_ENABLED_ONLY_IN_ADVANCED =
        new HashSet<String>(Arrays.asList(new String[]{InputDevData.BUS}));

    /** Whether the parameter is required. */
    private static final Collection<String> IS_REQUIRED =
        new HashSet<String>(Arrays.asList(new String[]{InputDevData.TYPE}));

    /** Possible values. */
    private static final Map<String, Value[]> POSSIBLE_VALUES =
                                               new HashMap<String, Value[]>();
    static {
        FIELD_TYPES.put(InputDevData.TYPE, Widget.Type.RADIOGROUP);
        SHORTNAME_MAP.put(InputDevData.TYPE, "Type");
        SHORTNAME_MAP.put(InputDevData.BUS, "Bus");
    }
    static {
        POSSIBLE_VALUES.put(InputDevData.TYPE,
                            new Value[]{new StringValue("tablet"),
                                        new StringValue("mouse"),
                                        new StringValue("keyboard")});
        POSSIBLE_VALUES.put(InputDevData.BUS,
                            new Value[]{new StringValue("usb")}); /* no ps2 */
    }

    @Inject
    private SwingUtils swingUtils;

    /** Table panel. */
    private JComponent tablePanel = null;
    @Inject
    private ClusterTreeMenu clusterTreeMenu;

    void init(final String name, final Browser browser, final DomainInfo vmsVirtualDomainInfo) {
        super.init(name, browser, vmsVirtualDomainInfo);
    }

    /** Adds disk table with only this disk to the main panel. */
    @Override
    protected void addHardwareTable(final JPanel mainPanel) {
        tablePanel = getTablePanel("Input Devices",
                                   DomainInfo.INPUTDEVS_TABLE,
                                   getVMSVirtualDomainInfo().getNewInputDevBtn());
        if (getResource().isNew()) {
            swingUtils.invokeLater(new Runnable() {
                @Override
                public void run() {
                    tablePanel.setVisible(false);
                }
            });
        }
        mainPanel.add(tablePanel);
    }

    /** Returns long description of the specified parameter. */
    @Override
    protected String getParamLongDesc(final String param) {
        return getParamShortDesc(param);
    }

    /** Returns short description of the specified parameter. */
    @Override
    protected String getParamShortDesc(final String param) {
        final String name = SHORTNAME_MAP.get(param);
        if (name == null) {
            return param;
        }
        return name;
    }

    /** Returns preferred value for specified parameter. */
    @Override
    protected Value getParamPreferred(final String param) {
        return null;
    }

    /** Returns default value for specified parameter. */
    @Override
    protected Value getParamDefault(final String param) {
        return null;
    }

    /** Returns parameters. */
    @Override
    public String[] getParametersFromXML() {
        return PARAMETERS.clone();
    }

    /** Returns possible choices for drop down lists. */
    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        return POSSIBLE_VALUES.get(param);
    }

    /** Returns section to which the specified parameter belongs. */
    @Override
    protected String getSection(final String param) {
        return "Input Device Options";
    }

    /** Returns true if the specified parameter is required. */
    @Override
    protected boolean isRequired(final String param) {
        return IS_REQUIRED.contains(param);
    }

    /** Returns true if the specified parameter is integer. */
    @Override
    protected boolean isInteger(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is label. */
    @Override
    protected boolean isLabel(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is of time type. */
    @Override
    protected boolean isTimeType(final String param) {
        return false;
    }

    /** Returns whether parameter is checkbox. */
    @Override
    protected boolean isCheckBox(final String param) {
        return false;
    }

    /** Returns the type of the parameter. */
    @Override
    protected String getParamType(final String param) {
        return "undef"; // TODO:
    }

    /** Returns the regexp of the parameter. */
    @Override
    protected String getParamRegexp(final String param) {
        return null;
    }

    /** Returns type of the field. */
    @Override
    protected Widget.Type getFieldType(final String param) {
        return FIELD_TYPES.get(param);
    }

    /** Applies the changes. */
    @Override
    void apply(final Application.RunMode runMode) {
        if (Application.isTest(runMode)) {
            return;
        }
        swingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                getApplyButton().setEnabled(false);
                getRevertButton().setEnabled(false);
                getInfoPanel();
            }
        });
        waitForInfoPanel();
        final Map<String, String> parameters = getHWParameters(
                                                        getResource().isNew());
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            if (vmsXml != null) {
                final Value type = getParamSaved(InputDevData.TYPE);
                if (type == null) {
                    parameters.put(InputDevData.SAVED_TYPE, null);
                } else {
                    parameters.put(InputDevData.SAVED_TYPE,
                                   type.getValueForConfig());
                }

                final Value bus = getParamSaved(InputDevData.BUS);
                if (bus == null) {
                    parameters.put(InputDevData.SAVED_BUS, null);
                } else {
                    parameters.put(InputDevData.SAVED_BUS,
                                   bus.getValueForConfig());
                }

                final String domainName =
                                getVMSVirtualDomainInfo().getDomainName();
                final Node domainNode = vmsXml.getDomainNode(domainName);
                modifyXML(vmsXml, domainNode, domainName, parameters);
                final String virshOptions =
                                   getVMSVirtualDomainInfo().getVirshOptions();
                vmsXml.saveAndDefine(domainNode, domainName, virshOptions);
            }
        }
        getResource().setNew(false);
        clusterTreeMenu.reloadNodeDontSelect(getNode());
        getBrowser().periodicalVmsUpdate(
                getVMSVirtualDomainInfo().getDefinedOnHosts());
        swingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                tablePanel.setVisible(true);
            }
        });
        final String[] params = getParametersFromXML();
        if (Application.isLive(runMode)) {
            storeComboBoxValues(params);
        }
        checkResourceFields(null, params);
    }

    /** Returns device parameters. */
    @Override
    protected Map<String, String> getHWParameters(final boolean allParams) {
        final Map<String, String> params = super.getHWParameters(allParams);
        setName(getParamSaved(InputDevData.TYPE)
                + ":"
                + getParamSaved(InputDevData.BUS));
        return params;
    }

    /** Returns data for the table. */
    @Override
    protected Object[][] getTableData(final String tableName) {
        if (DomainInfo.HEADER_TABLE.equals(tableName)) {
            return getVMSVirtualDomainInfo().getMainTableData();
        } else if (DomainInfo.INPUTDEVS_TABLE.equals(tableName)) {
            if (getResource().isNew()) {
                return new Object[][]{};
            }
            return new Object[][]{getVMSVirtualDomainInfo().getInputDevDataRow(
                                    getName(),
                                    null,
                                    getVMSVirtualDomainInfo().getInputDevs(),
                                    true)};
        }
        return new Object[][]{};
    }

    /** Returns whether this parameter is advanced. */
    @Override
    protected boolean isAdvanced(final String param) {
        return false;
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected String isEnabled(final String param) {
        if (getResource().isNew() || !InputDevData.TYPE.equals(param)) {
            return null;
        } else {
            return "";
        }
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override
    protected AccessMode.Mode isEnabledOnlyInAdvancedMode(final String param) {
         return IS_ENABLED_ONLY_IN_ADVANCED.contains(param) ? AccessMode.ADVANCED : AccessMode.NORMAL;
    }

    /** Returns access type of this parameter. */
    @Override
    protected AccessMode.Type getAccessType(final String param) {
        return AccessMode.ADMIN;
    }

    /** Returns true if the value of the parameter is ok. */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        return !isRequired(param)
               || (newValue != null && !newValue.isNothingSelected());
    }

    /** Updates parameters. */
    @Override
    void updateParameters() {
        final Map<String, InputDevData> inputDevs =
                                    getVMSVirtualDomainInfo().getInputDevs();
        if (inputDevs != null) {
            final InputDevData inputDevData = inputDevs.get(getName());
            if (inputDevData != null) {
                for (final String param : getParametersFromXML()) {
                    final Value oldValue = getParamSaved(param);
                    Value value = getParamSaved(param);
                    final Widget wi = getWidget(param, null);
                    for (final Host h
                             : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                        final VmsXml vmsXml = getBrowser().getVmsXml(h);
                        if (vmsXml != null) {
                            final Value savedValue =
                                               inputDevData.getValue(param);
                            if (savedValue != null) {
                                value = savedValue;
                            }
                        }
                    }
                    if (!Tools.areEqual(value, oldValue)) {
                        getResource().setValue(param, value);
                        if (wi != null) {
                            /* only if it is not changed by user. */
                            wi.setValue(value);
                        }
                    }
                }
            }
        }
        updateTable(DomainInfo.HEADER_TABLE);
        updateTable(DomainInfo.INPUTDEVS_TABLE);
        checkResourceFields(null, getParametersFromXML());
    }

    /** Returns string representation. */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(30);
        final Value type = getParamSaved(InputDevData.TYPE);
        if (type == null || type.isNothingSelected()) {
            s.append("new input device...");
        } else {
            s.append(type.getValueForGui());
        }

        final Value bus = getParamSaved(InputDevData.BUS);
        if (bus != null && !bus.isNothingSelected()) {
            s.append(" (");
            s.append(bus.getValueForConfig());
            s.append(')');
        }
        return s.toString();
    }

    /** Removes this input device without confirmation dialog. */
    @Override
    protected void removeMyselfNoConfirm(final Application.RunMode runMode) {
        if (Application.isTest(runMode)) {
            return;
        }
        final String virshOptions = getVMSVirtualDomainInfo().getVirshOptions();
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            if (vmsXml != null) {
                final Map<String, String> parameters =
                                                new HashMap<String, String>();
                parameters.put(InputDevData.SAVED_TYPE,
                               getParamSaved(InputDevData.TYPE).getValueForConfig());
                parameters.put(InputDevData.SAVED_BUS,
                               getParamSaved(InputDevData.BUS).getValueForConfig());
                vmsXml.removeInputDevXML(
                                    getVMSVirtualDomainInfo().getDomainName(),
                                    parameters,
                                    virshOptions);
            }
        }
        getBrowser().periodicalVmsUpdate(
                getVMSVirtualDomainInfo().getDefinedOnHosts());
        clusterTreeMenu.removeNode(getNode());
    }

    /**
     * Returns whether this item is removeable (null), or string why it isn't.
     */
    @Override
    protected String isRemoveable() {
        final String type = getParamSaved(InputDevData.TYPE).getValueForConfig();
        if (type != null && "mouse".equals(type)) {
            final String bus = getParamSaved(InputDevData.BUS).getValueForConfig();
            if (bus != null && "ps2".equals(bus)) {
                return "You can never remove this one";
            }
        }
        return null;
    }

    /** Modify device xml. */
    @Override
    protected void modifyXML(final VmsXml vmsXml, final Node node, final String domainName, final Map<String, String> params) {
        if (vmsXml != null) {
            vmsXml.modifyInputDevXML(node, domainName, params);
        }
    }
}
