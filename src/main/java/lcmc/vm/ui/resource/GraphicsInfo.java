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

import lcmc.cluster.service.NetworkService;
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
import lcmc.vm.domain.data.GraphicsData;
import org.w3c.dom.Node;

import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.*;
import java.util.*;

/**
 * This class holds info about virtual graphics displays.
 */
@Named
public final class GraphicsInfo extends HardwareInfo {

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
    private static final Map<String, Widget.Type> FIELD_TYPES =
                                       new HashMap<String, Widget.Type>();
    /** Short name. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();
    /** Preferred values. */
    private static final Map<String, Value> PREFERRED_VALUES =
                                                 new HashMap<String, Value>();
    /** Whether the parameter is editable only in advanced mode. */
    private static final Collection<String> IS_ENABLED_ONLY_IN_ADVANCED =
        new HashSet<String>(Arrays.asList(new String[]{GraphicsData.KEYMAP}));

    /** Whether the parameter is required. */
    private static final Collection<String> IS_REQUIRED =
        new HashSet<String>(Arrays.asList(new String[]{GraphicsData.TYPE}));

    /** Possible values. */
    private static final Map<String, Value[]> POSSIBLE_VALUES =
                                               new HashMap<String, Value[]>();

    public static final Value TYPE_VNC = new StringValue("vnc");
    public static final Value TYPE_SDL = new StringValue("sdl");

    public static final Value PORT_AUTO = new StringValue("-1", "auto");

    static {
        FIELD_TYPES.put(GraphicsData.TYPE, Widget.Type.RADIOGROUP);
        FIELD_TYPES.put(GraphicsData.PASSWD, Widget.Type.PASSWDFIELD);
        SHORTNAME_MAP.put(GraphicsData.TYPE, "Type");
        SHORTNAME_MAP.put(GraphicsData.PORT, "Port");
        SHORTNAME_MAP.put(GraphicsData.LISTEN, "Listen");
        SHORTNAME_MAP.put(GraphicsData.PASSWD, "Password");
        SHORTNAME_MAP.put(GraphicsData.KEYMAP, "Keymap");
        SHORTNAME_MAP.put(GraphicsData.DISPLAY, "Display");
        SHORTNAME_MAP.put(GraphicsData.XAUTH, "Xauth File");
        PREFERRED_VALUES.put(GraphicsData.PORT, new StringValue("-1"));
        PREFERRED_VALUES.put(GraphicsData.DISPLAY, new StringValue(":0.0"));
        PREFERRED_VALUES.put(GraphicsData.XAUTH,
                             new StringValue(System.getProperty("user.home") + "/.Xauthority"));
        POSSIBLE_VALUES.put(GraphicsData.TYPE, new Value[]{TYPE_VNC, TYPE_SDL});
        POSSIBLE_VALUES.put(
            GraphicsData.XAUTH,
            new Value[]{new StringValue(),
                        new StringValue(System.getProperty("user.home") + "/.Xauthority")});
        POSSIBLE_VALUES.put(GraphicsData.DISPLAY, new Value[]{new StringValue(),
                                                              new StringValue(":0.0")});
        POSSIBLE_VALUES.put(GraphicsData.PORT,
                            new Value[]{PORT_AUTO,
                                        new StringValue("5900", "5900"),
                                        new StringValue("5901", "5901")});
    }

    @Inject
    private SwingUtils swingUtils;

    /** Combo box that can be made invisible. */
    private final Map<String, Widget> portWi = new HashMap<String, Widget>();
    /** Combo box that can be made invisible. */
    private final Map<String, Widget> listenWi = new HashMap<String, Widget>();
    /** Combo box that can be made invisible. */
    private final Map<String, Widget> passwdWi = new HashMap<String, Widget>();
    /** Combo box that can be made invisible. */
    private final Map<String, Widget> keymapWi = new HashMap<String, Widget>();
    /** Combo box that can be made invisible. */
    private final Map<String, Widget> displayWi = new HashMap<String, Widget>();
    /** Combo box that can be made invisible. */
    private final Map<String, Widget> xauthWi = new HashMap<String, Widget>();
    /** Table panel. */
    private JComponent tablePanel = null;
    @Inject
    private ClusterTreeMenu clusterTreeMenu;
    @Inject
    private NetworkService networkService;

    void init(final String name, final Browser browser, final DomainInfo vmsVirtualDomainInfo) {
        super.init(name, browser, vmsVirtualDomainInfo);
    }

    /** Adds disk table with only this disk to the main panel. */
    @Override
    protected void addHardwareTable(final JPanel mainPanel) {
        tablePanel = getTablePanel("Displays",
                                   DomainInfo.GRAPHICS_TABLE,
                                   getVMSVirtualDomainInfo().getNewGraphicsBtn());
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

    /** Returns service icon in the menu. */
    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        return DomainInfo.VNC_ICON_SMALL;
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
        return PREFERRED_VALUES.get(param);
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
        if (GraphicsData.LISTEN.equals(param)) {

            final List<Host> definedOnHosts =
                                getVMSVirtualDomainInfo().getDefinedOnHosts();
            final Map<String, Integer> networksIntersection = networkService.getNetworksIntersection(definedOnHosts);
            final List<Value> commonNetworks = new ArrayList<Value>();
            commonNetworks.add(new StringValue());
            commonNetworks.add(new StringValue("0.0.0.0", "All Interfaces/0.0.0.0"));
            commonNetworks.add(new StringValue("127.0.0.1", "localhost/127.0.0.1"));
            if (networksIntersection != null) {
                for (final String netIp : networksIntersection.keySet()) {
                    final Value network = new StringValue(netIp);
                    commonNetworks.add(network);
                }
            }
            return commonNetworks.toArray(
                                        new StringValue[commonNetworks.size()]);
        } else if (GraphicsData.KEYMAP.equals(param)) {
            List<Value> keymaps = null;
            final List<Host> definedOnHosts =
                                getVMSVirtualDomainInfo().getDefinedOnHosts();
            for (final Host host : definedOnHosts) {
                if (keymaps == null) {
                    keymaps = new ArrayList<Value>();
                    keymaps.add(new StringValue());
                    keymaps.addAll(host.getHostParser().getAvailableQemuKeymaps());
                } else {
                    final Set<Value> hostKeymaps = host.getHostParser().getAvailableQemuKeymaps();
                    final List<Value> newKeymaps = new ArrayList<Value>();
                    newKeymaps.add(new StringValue());
                    for (final Value km : keymaps) {
                        if (km != null && hostKeymaps.contains(km)) {
                            newKeymaps.add(km);
                        }
                    }
                    keymaps = newKeymaps;
                }
            }
            if (keymaps == null) {
                return new StringValue[]{new StringValue()};
            }
            return keymaps.toArray(new Value[keymaps.size()]);
        }
        return POSSIBLE_VALUES.get(param);
    }

    /** Returns section to which the specified parameter belongs. */
    @Override
    protected String getSection(final String param) {
        return "Display Options";
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
        if (GraphicsData.PORT.equals(param)) {
            return "^(-1|\\d+|aa|auto)$"; //TODO: aa?
        } else if (GraphicsData.LISTEN.equals(param)) {
            return "^(\\d+\\.\\d+\\.\\d+\\.\\d+)?$";
        } else if (GraphicsData.DISPLAY.equals(param)) {
            return "^:\\d+\\.\\d+$";
        }
        return null;
    }

    /** Returns type of the field. */
    @Override
    protected Widget.Type getFieldType(final String param) {
        return FIELD_TYPES.get(param);
    }

    /** Returns device parameters. */
    @Override
    protected Map<String, String> getHWParameters(final boolean allParams) {
        swingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                getInfoPanel();
            }
        });
        final Map<String, String> parameters = new HashMap<String, String>();
        String[] params = {};
        boolean vnc = false;
        if (TYPE_VNC.equals(getComboBoxValue(GraphicsData.TYPE))) {
            vnc = true;
            params = VNC_PARAMETERS;
        } else if (TYPE_SDL.equals(getComboBoxValue(GraphicsData.TYPE))) {
            params = SDL_PARAMETERS;
        }
        for (final String param : params) {
            final Value value = getComboBoxValue(param);
            if (allParams || !Tools.areEqual(getParamSaved(param), value)) {
                if (Tools.areEqual(getParamDefault(param), value)) {
                    parameters.put(param, null);
                } else {
                    parameters.put(param, value.getValueForConfig());
                }
                if (vnc) {
                    if (GraphicsData.PORT.equals(param) && "-1".equals(value.getValueForConfig())) {
                        parameters.put(GraphicsData.AUTOPORT, "yes");
                    } else {
                        parameters.put(GraphicsData.AUTOPORT, "no");
                    }
                }
            }
        }
        setName(VmsXml.graphicsDisplayName(
                getParamSavedForConfig(GraphicsData.TYPE),
                getParamSavedForConfig(GraphicsData.PORT),
                getParamSavedForConfig(GraphicsData.DISPLAY)));
        return parameters;
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
        final Map<String, String> parameters =
                                     getHWParameters(getResource().isNew());
        final String[] params = getRealParametersFromXML();
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            if (vmsXml != null) {
                parameters.put(GraphicsData.SAVED_TYPE,
                               getParamSavedForConfig(GraphicsData.TYPE));
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
        if (Application.isLive(runMode)) {
            storeComboBoxValues(params);
        }
        checkResourceFields(null, params);
    }

    /** Returns data for the table. */
    @Override
    protected Object[][] getTableData(final String tableName) {
        if (DomainInfo.HEADER_TABLE.equals(tableName)) {
            return getVMSVirtualDomainInfo().getMainTableData();
        } else if (DomainInfo.GRAPHICS_TABLE.equals(tableName)) {
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
    @Override
    protected boolean isAdvanced(final String param) {
        return false;
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected String isEnabled(final String param) {
        if (getResource().isNew() || !GraphicsData.TYPE.equals(param)) {
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
        if (GraphicsData.TYPE.equals(param)) {
            final boolean vnc = TYPE_VNC.equals(newValue);
            final boolean sdl = TYPE_SDL.equals(newValue);
            swingUtils.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (final Map.Entry<String, Widget> entry : listenWi.entrySet()) {
                        entry.getValue().setVisible(vnc);
                    }
                    for (final Map.Entry<String, Widget> entry : passwdWi.entrySet()) {
                        entry.getValue().setVisible(vnc);
                    }
                    for (final Map.Entry<String, Widget> entry : keymapWi.entrySet()) {
                        entry.getValue().setVisible(vnc);
                    }
                    for (final Map.Entry<String, Widget> entry : portWi.entrySet()) {
                        entry.getValue().setVisible(vnc);
                    }
                    for (final Map.Entry<String, Widget> entry : displayWi.entrySet()) {
                        entry.getValue().setVisible(sdl);
                    }
                    for (final Map.Entry<String, Widget> entry : xauthWi.entrySet()) {
                        entry.getValue().setVisible(sdl);
                    }
                }
            });
        }
        return !isRequired(param)
               || (newValue != null && !newValue.isNothingSelected());
    }

    /** Updates parameters. */
    @Override
    void updateParameters() {
        final Map<String, GraphicsData> graphicDisplays =
                              getVMSVirtualDomainInfo().getGraphicDisplays();
        if (graphicDisplays != null) {
            final GraphicsData graphicsData = graphicDisplays.get(getName());
            if (graphicsData != null) {
                for (final String param : getParametersFromXML()) {
                    final Value oldValue = getParamSaved(param);
                    Value value = getParamSaved(param);
                    final Widget wi = getWidget(param, null);
                    for (final Host h
                            : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                        final VmsXml vmsXml = getBrowser().getVmsXml(h);
                        if (vmsXml != null) {
                            final Value savedValue =
                                               graphicsData.getValue(param);
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
        setName(VmsXml.graphicsDisplayName(
                getParamSavedForConfig(GraphicsData.TYPE),
                getParamSavedForConfig(GraphicsData.PORT),
                getParamSavedForConfig(GraphicsData.DISPLAY)));
        updateTable(DomainInfo.HEADER_TABLE);
        updateTable(DomainInfo.GRAPHICS_TABLE);
        checkResourceFields(null, getParametersFromXML());
    }

    /** Returns string representation. */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(30);
        if (getName() == null) {
            s.append("new graphics device...");
        } else {
            s.append(getName());
        }

        return s.toString();
    }

    /** Removes this graphics device without confirmation dialog. */
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
                parameters.put(GraphicsData.SAVED_TYPE,
                               getParamSavedForConfig(GraphicsData.TYPE));
                vmsXml.removeGraphicsXML(
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
        return null;
    }

    /** Returns combo box for parameter. */
    @Override
    protected Widget createWidget(final String param, final String prefix, final int width) {
        final Widget paramWi = super.createWidget(param, prefix, width);
        if (GraphicsData.PORT.equals(param)) {
            if (prefix == null) {
                portWi.put("", paramWi);
            } else {
                portWi.put(prefix, paramWi);
            }
        } else if (GraphicsData.LISTEN.equals(param)) {
            if (prefix == null) {
                listenWi.put("", paramWi);
            } else {
                listenWi.put(prefix, paramWi);
            }
        } else if (GraphicsData.PASSWD.equals(param)) {
            if (prefix == null) {
                passwdWi.put("", paramWi);
            } else {
                passwdWi.put(prefix, paramWi);
            }
        } else if (GraphicsData.KEYMAP.equals(param)) {
            if (prefix == null) {
                keymapWi.put("", paramWi);
            } else {
                keymapWi.put(prefix, paramWi);
            }
        } else if (GraphicsData.DISPLAY.equals(param)) {
            if (prefix == null) {
                displayWi.put("", paramWi);
            } else {
                displayWi.put(prefix, paramWi);
            }
        } else if (GraphicsData.XAUTH.equals(param)) {
            if (prefix == null) {
                xauthWi.put("", paramWi);
            } else {
                xauthWi.put(prefix, paramWi);
            }
        }
        return paramWi;
    }

    /** Modify device xml. */
    @Override
    protected void modifyXML(final VmsXml vmsXml, final Node node, final String domainName, final Map<String, String> params) {
        if (vmsXml != null) {
            vmsXml.modifyGraphicsXML(node, domainName, params);
        }
    }

    /** Returns real parameters. */
    @Override
    public String[] getRealParametersFromXML() {
        if (TYPE_VNC.equals(getComboBoxValue(GraphicsData.TYPE))) {
            return VNC_PARAMETERS.clone();
        } else if (TYPE_SDL.equals(getComboBoxValue(GraphicsData.TYPE))) {
            return SDL_PARAMETERS.clone();
        }
        return PARAMETERS.clone();
    }
}
