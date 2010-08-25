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
import drbd.gui.GuiComboBox;
import drbd.data.VMSXML;
import drbd.data.VMSXML.SerialData;
import drbd.data.Host;
import drbd.data.ConfigData;
import drbd.utilities.Tools;

import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

/**
 * This class holds info about virtual serial device.
 */
public class VMSSerialInfo extends VMSHardwareInfo {
    /** Parameters. */
    private static final String[] PARAMETERS = {SerialData.TYPE,
                                                SerialData.SOURCE_PATH,
                                                SerialData.SOURCE_MODE,
                                                SerialData.SOURCE_HOST,
                                                SerialData.PROTOCOL_TYPE,
                                                SerialData.TARGET_PORT};

    /** Field type. */
    private static final Map<String, GuiComboBox.Type> FIELD_TYPES =
                                       new HashMap<String, GuiComboBox.Type>();
    /** Short name. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();
    static {
        SHORTNAME_MAP.put(SerialData.TYPE, "Type");
    }

    /** Whether the parameter is enabled only in advanced mode. */
    private static final Set<String> IS_ENABLED_ONLY_IN_ADVANCED =
                                                        new HashSet<String>();

    /** Whether the parameter is required. */
    private static final Set<String> IS_REQUIRED =
        new HashSet<String>(Arrays.asList(new String[]{SerialData.TYPE}));

    /** Default name. */
    private static final Map<String, String> DEFAULTS_MAP =
                                                 new HashMap<String, String>();
    /** Possible values. */
    private static final Map<String, Object[]> POSSIBLE_VALUES =
                                               new HashMap<String, Object[]>();
    static {
        POSSIBLE_VALUES.put(SerialData.TYPE,
                            new String[]{"dev",
                                         "file",
                                         "null",
                                         "pipe",
                                         "pty",
                                         "stdio",
                                         "tcp",
                                         "udp",
                                         "unix",
                                         "vc"});
    }
    /** Creates the VMSSerialInfo object. */
    public VMSSerialInfo(final String name, final Browser browser,
                        final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(name, browser, vmsVirtualDomainInfo);
    }

    /** Adds disk table with only this disk to the main panel. */
    protected final void addHardwareTable(final JPanel mainPanel) {
        mainPanel.add(getTablePanel("Displays",
                                    VMSVirtualDomainInfo.SERIAL_TABLE));
    }

    /** Returns service icon in the menu. */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        return NetInfo.NET_I_ICON;
    }

    /** Returns long description of the specified parameter. */
    protected final String getParamLongDesc(final String param) {
        return getParamShortDesc(param);
    }

    /** Returns short description of the specified parameter. */
    protected final String getParamShortDesc(final String param) {
        final String name = SHORTNAME_MAP.get(param);
        if (name == null) {
            return param;
        }
        return name;
    }

    /** Returns preferred value for specified parameter. */
    protected final String getParamPreferred(final String param) {
        return null;
    }

    /** Returns default value for specified parameter. */
    protected final String getParamDefault(final String param) {
        return DEFAULTS_MAP.get(param);
    }

    /** Returns parameters. */
    public final String[] getParametersFromXML() {
        return PARAMETERS;
    }

    /** Returns possible choices for drop down lists. */
    protected final Object[] getParamPossibleChoices(final String param) {
        return POSSIBLE_VALUES.get(param);
    }

    /** Returns section to which the specified parameter belongs. */
    protected final String getSection(final String param) {
        return "Display Options";
    }

    /** Returns true if the specified parameter is required. */
    protected final boolean isRequired(final String param) {
        return IS_REQUIRED.contains(param);
    }

    /** Returns true if the specified parameter is integer. */
    protected final boolean isInteger(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is label. */
    protected final boolean isLabel(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is of time type. */
    protected final boolean isTimeType(final String param) {
        return false;
    }

    /** Returns whether parameter is checkbox. */
    protected final boolean isCheckBox(final String param) {
        return false;
    }

    /** Returns the type of the parameter. */
    protected final String getParamType(final String param) {
        return "undef"; // TODO:
    }

    /** Returns the regexp of the parameter. */
    protected final String getParamRegexp(final String param) {
        return null;
    }

    /** Returns type of the field. */
    protected final GuiComboBox.Type getFieldType(final String param) {
        return FIELD_TYPES.get(param);
    }

    /** Applies the changes. */
    public final void apply(final boolean testOnly) {
        if (testOnly) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                applyButton.setEnabled(false);
            }
        });
        final String[] params = getParametersFromXML();
        final Map<String, String> parameters = new HashMap<String, String>();
        for (final String param : getParametersFromXML()) {
            final String value = getComboBoxValue(param);
            if (!Tools.areEqual(getParamSaved(param), value)) {
                parameters.put(param, value);
                getResource().setValue(param, value);
            }
        }
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                parameters.put(SerialData.SAVED_TYPE,
                               getParamSaved(SerialData.TYPE));
                vmsxml.modifySerialXML(
                                    getVMSVirtualDomainInfo().getDomainName(),
                                    parameters);
            }
            getResource().setNew(false);
            setName(getParamSaved(SerialData.TYPE));
        }
        for (final Host h : getBrowser().getClusterHosts()) {
            getBrowser().periodicalVMSUpdate(h);
        }
        checkResourceFields(null, params);
        getBrowser().reload(getNode());
    }

    /** Returns data for the table. */
    protected final Object[][] getTableData(final String tableName) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return getVMSVirtualDomainInfo().getMainTableData();
        } else if (VMSVirtualDomainInfo.SERIAL_TABLE.equals(tableName)) {
            if (getResource().isNew()) {
                return new Object[][]{};
            }
            return new Object[][]{getVMSVirtualDomainInfo().getSerialDataRow(
                                getName(),
                                null,
                                getVMSVirtualDomainInfo().getSerials(),
                                true)};
        }
        return new Object[][]{};
    }

    /** Returns whether this parameter is advanced. */
    protected final boolean isAdvanced(final String param) {
        return false;
    }

    /** Whether the parameter should be enabled. */
    protected final boolean isEnabled(final String param) {
        return true;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    protected final boolean isEnabledOnlyInAdvancedMode(final String param) {
         return IS_ENABLED_ONLY_IN_ADVANCED.contains(param);
    }

    /** Returns access type of this parameter. */
    protected final ConfigData.AccessType getAccessType(final String param) {
        return ConfigData.AccessType.ADMIN;
    }

    /** Returns true if the value of the parameter is ok. */
    protected final boolean checkParam(final String param,
                                       final String newValue) {
        if (isRequired(param) && (newValue == null || "".equals(newValue))) {
            return false;
        }
        return true;
    }

    /** Updates parameters. */
    public final void updateParameters() {
        final Map<String, SerialData> serials =
                              getVMSVirtualDomainInfo().getSerials();
        if (serials != null) {
            final SerialData serialData = serials.get(getName());
            if (serialData != null) {
                for (final String param : getParametersFromXML()) {
                    final String oldValue = getParamSaved(param);
                    String value = getParamSaved(param);
                    final GuiComboBox cb = paramComboBoxGet(param, null);
                    for (final Host h : getBrowser().getClusterHosts()) {
                        final VMSXML vmsxml = getBrowser().getVMSXML(h);
                        if (vmsxml != null) {
                            final String savedValue =
                                                  serialData.getValue(param);
                            if (savedValue != null) {
                                value = savedValue;
                            }
                        }
                    }
                    if (!Tools.areEqual(value, oldValue)) {
                        getResource().setValue(param, value);
                        if (cb != null) {
                            /* only if it is not changed by user. */
                            cb.setValue(value);
                        }
                    }
                }
            }
        }
        updateTable(VMSVirtualDomainInfo.HEADER_TABLE);
        updateTable(VMSVirtualDomainInfo.SERIAL_TABLE);
    }

    /** Returns string representation. */
    public final String toString() {
        final StringBuffer s = new StringBuffer(30);
        final String type = getParamSaved(SerialData.TYPE);
        if (type == null) {
            s.append("new serial device...");
        } else {
            s.append(type);
        }

        s.append(" /");
        s.append(getName());
        return s.toString();
    }

    /** Removes this serial device without confirmation dialog. */
    protected final void removeMyselfNoConfirm(final boolean testOnly) {
        if (testOnly) {
            return;
        }
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                final Map<String, String> parameters =
                                                new HashMap<String, String>();
                parameters.put(SerialData.SAVED_TYPE,
                               getParamSaved(SerialData.TYPE));
                vmsxml.removeSerialXML(
                                    getVMSVirtualDomainInfo().getDomainName(),
                                    parameters);
            }
        }
        for (final Host h : getBrowser().getClusterHosts()) {
            getBrowser().periodicalVMSUpdate(h);
        }
    }

    /**
     * Returns whether this item is removeable (null), or string why it isn't.
     */
    protected final String isRemoveable() {
        return null;
    }

    /** Returns combo box for parameter. */
    protected final GuiComboBox getParamComboBox(final String param,
                                                 final String prefix,
                                                 final int width) {
        final GuiComboBox paramCB =
                             super.getParamComboBox(param, prefix, width);
        if (SerialData.TYPE.equals(param)) {
            paramCB.setAlwaysEditable(false);
        }
        return paramCB;
    }
}
