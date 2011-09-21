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

import lcmc.gui.Browser;
import lcmc.gui.GuiComboBox;
import lcmc.data.VMSXML;
import lcmc.data.VMSXML.InterfaceData;
import lcmc.data.Host;
import lcmc.data.ConfigData;
import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.w3c.dom.Node;

/**
 * This class holds info about Virtual Interfaces.
 */
public final class VMSInterfaceInfo extends VMSHardwareInfo {
    /** Source network combo box, so that it can be disabled, depending on
     * type. */
    private final Map<String, GuiComboBox> sourceNetworkCB =
                                            new HashMap<String, GuiComboBox>();
    /** Source bridge combo box, so that it can be disabled, depending on
     * type. */
    private final Map<String, GuiComboBox> sourceBridgeCB =
                                            new HashMap<String, GuiComboBox>();
    /** Parameters. */
    private static final String[] PARAMETERS = {InterfaceData.TYPE,
                                                InterfaceData.MAC_ADDRESS,
                                                InterfaceData.SOURCE_NETWORK,
                                                InterfaceData.SOURCE_BRIDGE,
                                                InterfaceData.TARGET_DEV,
                                                InterfaceData.MODEL_TYPE};
    /** Network parameters. */
    private static final String[] NETWORK_PARAMETERS = {
                                                InterfaceData.TYPE,
                                                InterfaceData.MAC_ADDRESS,
                                                InterfaceData.SOURCE_NETWORK,
                                                InterfaceData.TARGET_DEV,
                                                InterfaceData.MODEL_TYPE};
    /** Bridge parameters. */
    private static final String[] BRIDGE_PARAMETERS = {
                                                InterfaceData.TYPE,
                                                InterfaceData.MAC_ADDRESS,
                                                InterfaceData.SOURCE_BRIDGE,
                                                InterfaceData.TARGET_DEV,
                                                InterfaceData.MODEL_TYPE};
    /** Field type. */
    private static final Map<String, GuiComboBox.Type> FIELD_TYPES =
                                       new HashMap<String, GuiComboBox.Type>();
    /** Short name. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();
    static {
        FIELD_TYPES.put(InterfaceData.TYPE,
                        GuiComboBox.Type.RADIOGROUP);
        SHORTNAME_MAP.put(InterfaceData.TYPE, "Type");
        SHORTNAME_MAP.put(InterfaceData.MAC_ADDRESS, "Mac Address");
        SHORTNAME_MAP.put(InterfaceData.SOURCE_NETWORK, "Source Network");
        SHORTNAME_MAP.put(InterfaceData.SOURCE_BRIDGE, "Source Bridge");
        SHORTNAME_MAP.put(InterfaceData.TARGET_DEV, "Target Device");
        SHORTNAME_MAP.put(InterfaceData.MODEL_TYPE, "Model Type");
    }

    /** Whether the parameter is editable only in advanced mode. */
    private static final Set<String> IS_ENABLED_ONLY_IN_ADVANCED =
        new HashSet<String>(Arrays.asList(new String[]{
                                                InterfaceData.MAC_ADDRESS,
                                                InterfaceData.TARGET_DEV,
                                                InterfaceData.MODEL_TYPE}));

    /** Whether the parameter is required. */
    private static final Set<String> IS_REQUIRED =
        new HashSet<String>(Arrays.asList(new String[]{
                                                InterfaceData.TYPE }));

    /** Preferred values. */
    private static final Map<String, String> PREFERRED_MAP =
                                                 new HashMap<String, String>();
    /** Default values. */
    private static final Map<String, String> DEFAULTS_MAP =
                                                 new HashMap<String, String>();
    /** Possible values. */
    private static final Map<String, Object[]> POSSIBLE_VALUES =
                                               new HashMap<String, Object[]>();
    static {
        DEFAULTS_MAP.put(InterfaceData.MAC_ADDRESS, "generate");
        PREFERRED_MAP.put(InterfaceData.MODEL_TYPE, "virtio");
        PREFERRED_MAP.put(InterfaceData.SOURCE_NETWORK, "default");
        POSSIBLE_VALUES.put(InterfaceData.MODEL_TYPE,
                            new String[]{null,
                                         "default",
                                         "e1000",
                                         "ne2k_pci",
                                         "pcnet",
                                         "rtl8139",
                                         "virtio"});
        POSSIBLE_VALUES.put(InterfaceData.TYPE,
                            new String[]{"network", "bridge"});
    }
    /** Table panel. */
    private JComponent tablePanel = null;
    /** Creates the VMSInterfaceInfo object. */
    VMSInterfaceInfo(final String name, final Browser browser,
                     final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(name, browser, vmsVirtualDomainInfo);
    }

    /** Adds disk table with only this disk to the main panel. */
    @Override protected void addHardwareTable(final JPanel mainPanel) {
        tablePanel = getTablePanel("Interfaces",
                                   VMSVirtualDomainInfo.INTERFACES_TABLE,
                                   getNewBtn(getVMSVirtualDomainInfo()));
        if (getResource().isNew()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    tablePanel.setVisible(false);
                }
            });
        }
        mainPanel.add(tablePanel);
    }

    /** Returns service icon in the menu. */
    @Override public ImageIcon getMenuIcon(final boolean testOnly) {
        return NetInfo.NET_I_ICON;
    }

    /** Returns long description of the specified parameter. */
    @Override protected String getParamLongDesc(final String param) {
        return getParamShortDesc(param);
    }

    /** Returns short description of the specified parameter. */
    @Override protected String getParamShortDesc(final String param) {
        final String name = SHORTNAME_MAP.get(param);
        if (name == null) {
            return param;
        }
        return name;
    }

    /** Returns preferred value for specified parameter. */
    @Override protected String getParamPreferred(final String param) {
        return PREFERRED_MAP.get(param);
    }

    /** Returns default value for specified parameter. */
    @Override protected String getParamDefault(final String param) {
        return DEFAULTS_MAP.get(param);
    }

    /** Returns parameters. */
    @Override public String[] getParametersFromXML() {
        return PARAMETERS.clone();
    }

    /** Returns possible choices for drop down lists. */
    @Override protected Object[] getParamPossibleChoices(final String param) {
        if (InterfaceData.SOURCE_NETWORK.equals(param)) {
            for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                final VMSXML vmsxml = getBrowser().getVMSXML(h);
                if (vmsxml != null) {
                    final List<String> networks = vmsxml.getNetworks();
                    networks.add(0, null);
                    return networks.toArray(new String[networks.size()]);
                }
            }
        } else if (InterfaceData.SOURCE_BRIDGE.equals(param)) {
            for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                final VMSXML vmsxml = getBrowser().getVMSXML(h);
                if (vmsxml != null) {
                    final List<String> bridges = h.getBridges();
                    bridges.add(0, null);
                    return bridges.toArray(new String[bridges.size()]);
                }
            }
        }
        return POSSIBLE_VALUES.get(param);
    }

    /** Returns section to which the specified parameter belongs. */
    @Override protected String getSection(final String param) {
        return "Interface Options";
    }

    /** Returns true if the specified parameter is required. */
    @Override protected boolean isRequired(final String param) {
        final String type = getComboBoxValue(InterfaceData.TYPE);
        if ((InterfaceData.SOURCE_NETWORK.equals(param)
             && "network".equals(type))
            || (InterfaceData.SOURCE_BRIDGE.equals(param)
                && "bridge".equals(type))) {
            return true;
        }
        return IS_REQUIRED.contains(param);
    }

    /** Returns true if the specified parameter is integer. */
    @Override protected boolean isInteger(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is label. */
    @Override protected boolean isLabel(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is of time type. */
    @Override protected boolean isTimeType(final String param) {
        return false;
    }

    /** Returns whether parameter is checkbox. */
    @Override protected boolean isCheckBox(final String param) {
        return false;
    }

    /** Returns the type of the parameter. */
    @Override protected String getParamType(final String param) {
        return "undef"; // TODO:
    }

    /** Returns the regexp of the parameter. */
    @Override protected String getParamRegexp(final String param) {
        if (VMSXML.InterfaceData.MAC_ADDRESS.equals(param)) {
            return "^((([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2})|generate)?$";
        }
        return null;
    }

    /** Returns type of the field. */
    @Override protected GuiComboBox.Type getFieldType(final String param) {
        return FIELD_TYPES.get(param);
    }

    /** Returns device parameters. */
    @Override protected Map<String, String> getHWParameters(
                                                    final boolean allParams) {
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                getInfoPanel();
            }
        });
        final String[] params = getRealParametersFromXML();

        final Map<String, String> parameters = new HashMap<String, String>();
        for (final String param : params) {
            final String value = getComboBoxValue(param);
            if (allParams
                || InterfaceData.SOURCE_NETWORK.equals(param)
                || InterfaceData.SOURCE_BRIDGE.equals(param)
                || !Tools.areEqual(getParamSaved(param), value)) {
                if (Tools.areEqual(getParamDefault(param), value)) {
                    parameters.put(param, null);
                } else {
                    parameters.put(param, value);
                }
            }
        }
        setName(getParamSaved(InterfaceData.MAC_ADDRESS));
        return parameters;
    }

    /** Modify device xml. */
    @Override protected void modifyXML(final VMSXML vmsxml,
                                       final Node node,
                                       final String domainName,
                                       final Map<String, String> params) {
        if (vmsxml != null) {
            vmsxml.modifyInterfaceXML(node, domainName, params);
        }
    }

    /** Applies the changes. */
    @Override void apply(final boolean testOnly) {
        if (testOnly) {
            return;
        }
        Tools.invokeAndWait(new Runnable() {
            @Override public void run() {
                getApplyButton().setEnabled(false);
                getRevertButton().setEnabled(false);
            }
        });
        getInfoPanel();
        waitForInfoPanel();

        final Map<String, String> parameters = getHWParameters(
                                                        getResource().isNew());
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                parameters.put(InterfaceData.SAVED_MAC_ADDRESS, getName());
                final String domainName =
                                    getVMSVirtualDomainInfo().getDomainName();
                final Node domainNode = vmsxml.getDomainNode(domainName);
                modifyXML(vmsxml, domainNode, domainName, parameters);
                vmsxml.saveAndDefine(domainNode, domainName);
            }
            getResource().setNew(false);
        }
        getBrowser().reload(getNode(), false);
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            getBrowser().periodicalVMSUpdate(h);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                tablePanel.setVisible(true);
            }
        });
        final String[] params = getRealParametersFromXML();
        if (!testOnly) {
            storeComboBoxValues(params);
        }
        checkResourceFieldsChanged(null, params);
    }

    /** Returns data for the table. */
    @Override protected Object[][] getTableData(final String tableName) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return getVMSVirtualDomainInfo().getMainTableData();
        } else if (VMSVirtualDomainInfo.INTERFACES_TABLE.equals(tableName)) {
            if (getResource().isNew()) {
                return new Object[][]{};
            }
            return new Object[][]{getVMSVirtualDomainInfo().getInterfaceDataRow(
                                    getName(),
                                    null,
                                    getVMSVirtualDomainInfo().getInterfaces(),
                                    true)};
        }
        return new Object[][]{};
    }

    /** Returns whether this parameter is advanced. */
    @Override protected boolean isAdvanced(final String param) {
        return false;
    }

    /** Whether the parameter should be enabled. */
    @Override protected String isEnabled(final String param) {
        return null;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override protected boolean isEnabledOnlyInAdvancedMode(
                                                        final String param) {
         return IS_ENABLED_ONLY_IN_ADVANCED.contains(param);
    }

    /** Returns access type of this parameter. */
    @Override protected ConfigData.AccessType getAccessType(
                                                        final String param) {
        return ConfigData.AccessType.ADMIN;
    }

    /** Returns true if the value of the parameter is ok. */
    @Override protected boolean checkParam(final String param,
                                           final String newValue) {
        if (InterfaceData.TYPE.equals(param)) {
            for (final String p : sourceNetworkCB.keySet()) {
                sourceNetworkCB.get(p).setVisible(
                                               "network".equals(newValue));
            }
            for (final String p : sourceBridgeCB.keySet()) {
                sourceBridgeCB.get(p).setVisible(
                                                "bridge".equals(newValue));
            }
            checkOneParam(InterfaceData.SOURCE_NETWORK);
            checkOneParam(InterfaceData.SOURCE_BRIDGE);
        }
        if (isRequired(param) && (newValue == null || "".equals(newValue))) {
            return false;
        }
        return true;
    }
    /** Returns combo box for parameter. */
    @Override protected GuiComboBox getParamComboBox(final String param,
                                                     final String prefix,
                                                     final int width) {
        final GuiComboBox paramCB = super.getParamComboBox(param,
                                                           prefix,
                                                           width);
        if (InterfaceData.TYPE.equals(param)) {
            paramCB.setAlwaysEditable(false);
        } else if (InterfaceData.SOURCE_NETWORK.equals(param)) {
            if (prefix == null) {
                sourceNetworkCB.put("", paramCB);
            } else {
                sourceNetworkCB.put(prefix, paramCB);
            }
            paramCB.setAlwaysEditable(false);
        } else if (InterfaceData.SOURCE_BRIDGE.equals(param)) {
            if (prefix == null) {
                sourceBridgeCB.put("", paramCB);
            } else {
                sourceBridgeCB.put(prefix, paramCB);
            }
            paramCB.setAlwaysEditable(false);
        }
        return paramCB;
    }

    /** Updates parameters. */
    @Override void updateParameters() {
        final Map<String, InterfaceData> interfaces =
                                    getVMSVirtualDomainInfo().getInterfaces();
        if (interfaces != null) {
            final InterfaceData interfaceData = interfaces.get(getName());
            if (interfaceData != null) {
                for (final String param : getParametersFromXML()) {
                    final String oldValue = getParamSaved(param);
                    String value = getParamSaved(param);
                    final GuiComboBox cb = paramComboBoxGet(param, null);
                    for (final Host h
                            : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                        final VMSXML vmsxml = getBrowser().getVMSXML(h);
                        if (vmsxml != null) {
                            final String savedValue =
                                               interfaceData.getValue(param);
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
        updateTable(VMSVirtualDomainInfo.INTERFACES_TABLE);
        checkResourceFieldsChanged(null, getParametersFromXML());
    }

    /** Returns string representation. */
    @Override public String toString() {
        final StringBuilder s = new StringBuilder(30);
        String source;
        if ("network".equals(getParamSaved(InterfaceData.TYPE))) {
            source = getParamSaved(InterfaceData.SOURCE_NETWORK);
        } else {
            source = getParamSaved(InterfaceData.SOURCE_BRIDGE);
        }
        if (source == null) {
            s.append("new interface...");
        } else {
            s.append(source);
        }

        final String saved = getParamSaved(InterfaceData.MAC_ADDRESS);
        if (saved != null) {
            s.append(" (");
            if (saved.length() > 8) {
                s.append(saved.substring(8));
            } else {
                s.append(saved);
            }
            s.append(')');
        }
        return s.toString();
    }

    /** Removes this interface without confirmation dialog. */
    @Override protected void removeMyselfNoConfirm(final boolean testOnly) {
        if (testOnly) {
            return;
        }
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                final Map<String, String> parameters =
                                                new HashMap<String, String>();
                parameters.put(InterfaceData.SAVED_MAC_ADDRESS, getName());
                vmsxml.removeInterfaceXML(
                                    getVMSVirtualDomainInfo().getDomainName(),
                                    parameters);
            }
        }
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            getBrowser().periodicalVMSUpdate(h);
        }
        removeNode();
    }

    /**
     * Returns whether this item is removeable (null), or string why it isn't.
     */
    @Override protected String isRemoveable() {
        return null;
    }

    /** Returns "add new" button. */
    static MyButton getNewBtn(final VMSVirtualDomainInfo vdi) {
        final MyButton newBtn = new MyButton("Add Interface");
        newBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override public void run() {
                        vdi.addInterfacePanel();
                    }
                });
                t.start();
            }
        });
        return newBtn;
    }

    /** Returns real parameters. */
    @Override public String[] getRealParametersFromXML() {
        if ("network".equals(getComboBoxValue(InterfaceData.TYPE))) {
            return NETWORK_PARAMETERS.clone();
        } else {
            return BRIDGE_PARAMETERS.clone();
        }
    }
}
