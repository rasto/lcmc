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
import drbd.data.VMSXML.GraphicsData;
import drbd.data.Host;
import drbd.data.ConfigData;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.w3c.dom.Node;

/**
 * This class holds info about virtual graphics displays.
 */
public final class VMSGraphicsInfo extends VMSHardwareInfo {
    /** Combo box that can be made invisible. */
    private Map<String, GuiComboBox> portCB =
                                            new HashMap<String, GuiComboBox>();
    /** Combo box that can be made invisible. */
    private Map<String, GuiComboBox> listenCB =
                                            new HashMap<String, GuiComboBox>();
    /** Combo box that can be made invisible. */
    private Map<String, GuiComboBox> passwdCB =
                                            new HashMap<String, GuiComboBox>();
    /** Combo box that can be made invisible. */
    private Map<String, GuiComboBox> keymapCB =
                                            new HashMap<String, GuiComboBox>();
    /** Combo box that can be made invisible. */
    private Map<String, GuiComboBox> displayCB =
                                            new HashMap<String, GuiComboBox>();
    /** Combo box that can be made invisible. */
    private Map<String, GuiComboBox> xauthCB =
                                            new HashMap<String, GuiComboBox>();

    /** Parameters. AUTOPORT is generated */
    private static final String[] PARAMETERS = {GraphicsData.TYPE,
                                                GraphicsData.PORT,
                                                GraphicsData.LISTEN,
                                                GraphicsData.PASSWD,
                                                GraphicsData.KEYMAP,
                                                GraphicsData.DISPLAY,
                                                GraphicsData.XAUTH};

    /** VNC parameters. */
    private static final String[] VNC_PARAMETERS = {GraphicsData.TYPE,
                                                    GraphicsData.PORT,
                                                    GraphicsData.LISTEN,
                                                    GraphicsData.PASSWD,
                                                    GraphicsData.KEYMAP};
    /** SDL parameters. */
    private static final String[] SDL_PARAMETERS = {GraphicsData.TYPE,
                                                    GraphicsData.DISPLAY,
                                                    GraphicsData.XAUTH};

    /** Field type. */
    private static final Map<String, GuiComboBox.Type> FIELD_TYPES =
                                       new HashMap<String, GuiComboBox.Type>();
    /** Short name. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();
    /** Preferred values. */
    private static final Map<String, String> PREFERRED_VALUES =
                                                 new HashMap<String, String>();
    /** Whether the parameter is editable only in advanced mode. */
    private static final Set<String> IS_ENABLED_ONLY_IN_ADVANCED =
        new HashSet<String>(Arrays.asList(new String[]{GraphicsData.KEYMAP}));

    /** Whether the parameter is required. */
    private static final Set<String> IS_REQUIRED =
        new HashSet<String>(Arrays.asList(new String[]{GraphicsData.TYPE}));

    /** Default name. */
    private static final Map<String, String> DEFAULTS_MAP =
                                                 new HashMap<String, String>();
    /** Possible values. */
    private static final Map<String, Object[]> POSSIBLE_VALUES =
                                               new HashMap<String, Object[]>();
    static {
        FIELD_TYPES.put(GraphicsData.TYPE, GuiComboBox.Type.RADIOGROUP);
        FIELD_TYPES.put(GraphicsData.PASSWD, GuiComboBox.Type.PASSWDFIELD);
        SHORTNAME_MAP.put(GraphicsData.TYPE, "Type");
        SHORTNAME_MAP.put(GraphicsData.PORT, "Port");
        SHORTNAME_MAP.put(GraphicsData.LISTEN, "Listen");
        SHORTNAME_MAP.put(GraphicsData.PASSWD, "Password");
        SHORTNAME_MAP.put(GraphicsData.KEYMAP, "Keymap");
        SHORTNAME_MAP.put(GraphicsData.DISPLAY, "Display");
        SHORTNAME_MAP.put(GraphicsData.XAUTH, "Xauth File");
        PREFERRED_VALUES.put(GraphicsData.PORT, "-1");
        PREFERRED_VALUES.put(GraphicsData.DISPLAY, ":0.0");
        PREFERRED_VALUES.put(GraphicsData.XAUTH,
                             System.getProperty("user.home") + "/.Xauthority");
        POSSIBLE_VALUES.put(GraphicsData.TYPE,
                            new String[]{"vnc", "sdl"});
        POSSIBLE_VALUES.put(
            GraphicsData.XAUTH,
            new String[]{null,
                         System.getProperty("user.home") + "/.Xauthority"});
        POSSIBLE_VALUES.put(GraphicsData.DISPLAY, new String[]{null, ":0.0"});
        POSSIBLE_VALUES.put(GraphicsData.PORT,
                            new StringInfo[]{
                                        new StringInfo("auto", "-1", null),
                                        new StringInfo("5900", "5900", null),
                                        new StringInfo("5901", "5901", null)});
    }
    /** Table panel. */
    private JComponent tablePanel = null;
    /** Creates the VMSGraphicsInfo object. */
    VMSGraphicsInfo(final String name, final Browser browser,
                           final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(name, browser, vmsVirtualDomainInfo);
    }

    /** Adds disk table with only this disk to the main panel. */
    @Override protected void addHardwareTable(final JPanel mainPanel) {
        tablePanel = getTablePanel("Displays",
                                   VMSVirtualDomainInfo.GRAPHICS_TABLE,
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
        return VMSVirtualDomainInfo.VNC_ICON_SMALL;
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
        return PREFERRED_VALUES.get(param);
    }

    /** Returns default value for specified parameter. */
    @Override protected String getParamDefault(final String param) {
        return DEFAULTS_MAP.get(param);
    }

    /** Returns parameters. */
    @Override public String[] getParametersFromXML() {
        return PARAMETERS;
    }

    /** Returns possible choices for drop down lists. */
    @Override protected Object[] getParamPossibleChoices(final String param) {
        if (GraphicsData.LISTEN.equals(param)) {
            Map<String, String> networksIntersection = null;

            final List<Host> definedOnHosts =
                                getVMSVirtualDomainInfo().getDefinedOnHosts();
            for (final Host host : definedOnHosts) {
                networksIntersection =
                            host.getNetworksIntersection(networksIntersection);
            }
            final List<StringInfo> commonNetworks = new ArrayList<StringInfo>();
            commonNetworks.add(null);
            commonNetworks.add(new StringInfo("All Interfaces/0.0.0.0",
                                              "0.0.0.0",
                                              null));
            commonNetworks.add(new StringInfo("localhost/127.0.0.1",
                                              "127.0.0.1", null));
            if (networksIntersection != null) {
                for (final String netIp : networksIntersection.keySet()) {
                    final StringInfo network = new StringInfo(netIp,
                                                              netIp,
                                                              null);
                    commonNetworks.add(network);
                }
            }
            return commonNetworks.toArray(
                                        new StringInfo[commonNetworks.size()]);
        } else if (GraphicsData.KEYMAP.equals(param)) {
            List<String> keymaps = null;
            final List<Host> definedOnHosts =
                                getVMSVirtualDomainInfo().getDefinedOnHosts();
            for (final Host host : definedOnHosts) {
                if (keymaps == null) {
                    keymaps = new ArrayList<String>();
                    keymaps.add(null);
                    keymaps.addAll(host.getQemuKeymaps());
                } else {
                    final Set<String> hostKeymaps = host.getQemuKeymaps();
                    final List<String> newKeymaps = new ArrayList<String>();
                    newKeymaps.add(null);
                    for (final String km : keymaps) {
                        if (km != null && hostKeymaps.contains(km)) {
                            newKeymaps.add(km);
                        }
                    }
                    keymaps = newKeymaps;
                }
            }
            if (keymaps == null) {
                return new String[]{null};
            }
            return keymaps.toArray(new String[keymaps.size()]);
        }
        return POSSIBLE_VALUES.get(param);
    }

    /** Returns section to which the specified parameter belongs. */
    @Override protected String getSection(final String param) {
        return "Display Options";
    }

    /** Returns true if the specified parameter is required. */
    @Override protected boolean isRequired(final String param) {
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
        if (GraphicsData.PORT.equals(param)) {
            return "^(-1|\\d+|aa)$";
        } else if (GraphicsData.LISTEN.equals(param)) {
            return "^(\\d+\\.\\d+\\.\\d+\\.\\d+)?$";
        } else if (GraphicsData.DISPLAY.equals(param)) {
            return "^:\\d+\\.\\d+$";
        }
        return null;
    }

    /** Returns type of the field. */
    @Override protected GuiComboBox.Type getFieldType(final String param) {
        return FIELD_TYPES.get(param);
    }

    /** Returns device parameters. */
    @Override protected Map<String, String> getHWParametersAndSave(
                                                   final boolean allParams) {
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                getInfoPanel();
            }
        });
        final Map<String, String> parameters = new HashMap<String, String>();
        String[] params = {};
        boolean vnc = false;
        if ("vnc".equals(getComboBoxValue(GraphicsData.TYPE))) {
            vnc = true;
            params = VNC_PARAMETERS;
        } else if ("sdl".equals(getComboBoxValue(GraphicsData.TYPE))) {
            params = SDL_PARAMETERS;
        }
        for (final String param : params) {
            final String value = getComboBoxValue(param);
            if (allParams || !Tools.areEqual(getParamSaved(param), value)) {
                if (Tools.areEqual(getParamDefault(param), value)) {
                    parameters.put(param, null);
                } else {
                    parameters.put(param, value);
                }
                if (vnc) {
                    if (GraphicsData.PORT.equals(param) && "-1".equals(value)) {
                        parameters.put(GraphicsData.AUTOPORT, "yes");
                    } else {
                        parameters.put(GraphicsData.AUTOPORT, "no");
                    }
                }
                getResource().setValue(param, value);
            }
        }
        setName(VMSXML.graphicsDisplayName(
                                        getParamSaved(GraphicsData.TYPE),
                                        getParamSaved(GraphicsData.PORT),
                                        getParamSaved(GraphicsData.DISPLAY)));
        return parameters;
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
        final Map<String, String> parameters =
                                 getHWParametersAndSave(getResource().isNew());
        final String[] params = getRealParametersFromXML();
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                parameters.put(GraphicsData.SAVED_TYPE,
                               getParamSaved(GraphicsData.TYPE));
                final String domainName =
                                getVMSVirtualDomainInfo().getDomainName();
                final Node domainNode = vmsxml.getDomainNode(domainName);
                modifyXML(vmsxml, domainNode, domainName, parameters);
                vmsxml.saveAndDefine(domainNode, domainName);
            }
        }
        getResource().setNew(false);
        getBrowser().reload(getNode(), false);
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            getBrowser().periodicalVMSUpdate(h);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                tablePanel.setVisible(true);
            }
        });
        checkResourceFieldsChanged(null, params);
    }

    /** Returns data for the table. */
    @Override protected Object[][] getTableData(final String tableName) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return getVMSVirtualDomainInfo().getMainTableData();
        } else if (VMSVirtualDomainInfo.GRAPHICS_TABLE.equals(tableName)) {
            if (getResource().isNew()) {
                return new Object[][]{};
            }
            return new Object[][]{getVMSVirtualDomainInfo().getGraphicsDataRow(
                                getName(),
                                null,
                                getVMSVirtualDomainInfo().getGraphicDisplays(),
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
        if (getResource().isNew() || !GraphicsData.TYPE.equals(param)) {
            return null;
        } else {
            return "";
        }
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
        if (GraphicsData.TYPE.equals(param)) {
            final boolean vnc = "vnc".equals(newValue);
            final boolean sdl = "sdl".equals(newValue);
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    for (final String p : listenCB.keySet()) {
                        listenCB.get(p).setVisible(vnc);
                    }
                    for (final String p : passwdCB.keySet()) {
                        passwdCB.get(p).setVisible(vnc);
                    }
                    for (final String p : keymapCB.keySet()) {
                        keymapCB.get(p).setVisible(vnc);
                    }
                    for (final String p : portCB.keySet()) {
                        portCB.get(p).setVisible(vnc);
                    }
                    for (final String p : displayCB.keySet()) {
                        displayCB.get(p).setVisible(sdl);
                    }
                    for (final String p : xauthCB.keySet()) {
                        xauthCB.get(p).setVisible(sdl);
                    }
                }
            });
        }
        if (isRequired(param) && (newValue == null || "".equals(newValue))) {
            return false;
        }
        return true;
    }

    /** Updates parameters. */
    void updateParameters() {
        final Map<String, GraphicsData> graphicDisplays =
                              getVMSVirtualDomainInfo().getGraphicDisplays();
        if (graphicDisplays != null) {
            final GraphicsData graphicsData = graphicDisplays.get(getName());
            if (graphicsData != null) {
                for (final String param : getParametersFromXML()) {
                    final String oldValue = getParamSaved(param);
                    String value = getParamSaved(param);
                    final GuiComboBox cb = paramComboBoxGet(param, null);
                    for (final Host h
                            : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                        final VMSXML vmsxml = getBrowser().getVMSXML(h);
                        if (vmsxml != null) {
                            final String savedValue =
                                               graphicsData.getValue(param);
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
        setName(VMSXML.graphicsDisplayName(
                                        getParamSaved(GraphicsData.TYPE),
                                        getParamSaved(GraphicsData.PORT),
                                        getParamSaved(GraphicsData.DISPLAY)));
        updateTable(VMSVirtualDomainInfo.HEADER_TABLE);
        updateTable(VMSVirtualDomainInfo.GRAPHICS_TABLE);
        checkResourceFieldsChanged(null, getParametersFromXML());
    }

    /** Returns string representation. */
    @Override public String toString() {
        final StringBuilder s = new StringBuilder(30);
        if (getName() == null) {
            s.append("new graphics device...");
        } else {
            s.append(getName());
        }

        return s.toString();
    }

    /** Removes this graphics device without confirmation dialog. */
    @Override protected void removeMyselfNoConfirm(final boolean testOnly) {
        if (testOnly) {
            return;
        }
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                final Map<String, String> parameters =
                                                new HashMap<String, String>();
                parameters.put(GraphicsData.SAVED_TYPE,
                               getParamSaved(GraphicsData.TYPE));
                vmsxml.removeGraphicsXML(
                                    getVMSVirtualDomainInfo().getDomainName(),
                                    parameters);
            }
        }
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            getBrowser().periodicalVMSUpdate(h);
        }
    }

    /**
     * Returns whether this item is removeable (null), or string why it isn't.
     */
    @Override protected String isRemoveable() {
        return null;
    }

    /** Returns "add new" button. */
    static MyButton getNewBtn(final VMSVirtualDomainInfo vdi) {
        final MyButton newBtn = new MyButton("Add Graphics Display");
        newBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override public void run() {
                        vdi.addGraphicsPanel();
                    }
                });
                t.start();
            }
        });
        return newBtn;
    }

    /** Returns combo box for parameter. */
    @Override protected GuiComboBox getParamComboBox(final String param,
                                                     final String prefix,
                                                     final int width) {
        final GuiComboBox paramCB =
                                 super.getParamComboBox(param, prefix, width);
        if (GraphicsData.PORT.equals(param)) {
            if (prefix == null) {
                portCB.put("", paramCB);
            } else {
                portCB.put(prefix, paramCB);
            }
        } else if (GraphicsData.LISTEN.equals(param)) {
            if (prefix == null) {
                listenCB.put("", paramCB);
            } else {
                listenCB.put(prefix, paramCB);
            }
        } else if (GraphicsData.PASSWD.equals(param)) {
            if (prefix == null) {
                passwdCB.put("", paramCB);
            } else {
                passwdCB.put(prefix, paramCB);
            }
        } else if (GraphicsData.KEYMAP.equals(param)) {
            if (prefix == null) {
                keymapCB.put("", paramCB);
            } else {
                keymapCB.put(prefix, paramCB);
            }
        } else if (GraphicsData.DISPLAY.equals(param)) {
            if (prefix == null) {
                displayCB.put("", paramCB);
            } else {
                displayCB.put(prefix, paramCB);
            }
        } else if (GraphicsData.XAUTH.equals(param)) {
            if (prefix == null) {
                xauthCB.put("", paramCB);
            } else {
                xauthCB.put(prefix, paramCB);
            }
        }
        return paramCB;
    }

    /** Modify device xml. */
    @Override protected void modifyXML(final VMSXML vmsxml,
                                       final Node node,
                                       final String domainName,
                                       final Map<String, String> params) {
        if (vmsxml != null) {
            vmsxml.modifyGraphicsXML(node, domainName, params);
        }
    }

    /** Returns real parameters. */
    @Override String[] getRealParametersFromXML() {
        if ("vnc".equals(getComboBoxValue(GraphicsData.TYPE))) {
            return VNC_PARAMETERS;
        } else if ("sdl".equals(getComboBoxValue(GraphicsData.TYPE))) {
            return SDL_PARAMETERS;
        }
        return PARAMETERS;
    }
}
