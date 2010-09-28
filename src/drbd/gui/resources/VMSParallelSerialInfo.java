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
import drbd.data.VMSXML.ParallelSerialData;
import drbd.data.Host;
import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.w3c.dom.Node;

/**
 * This class holds info about virtual parallel or serial device.
 */
public abstract class VMSParallelSerialInfo extends VMSHardwareInfo {
    /** Parameters. */
    private static final String[] PARAMETERS = {
                                    ParallelSerialData.TYPE,
                                    ParallelSerialData.SOURCE_PATH,
                                    ParallelSerialData.SOURCE_MODE,
                                    ParallelSerialData.TARGET_PORT,
                                    ParallelSerialData.BIND_SOURCE_HOST,
                                    ParallelSerialData.BIND_SOURCE_SERVICE,
                                    ParallelSerialData.CONNECT_SOURCE_HOST,
                                    ParallelSerialData.CONNECT_SOURCE_SERVICE,
                                    ParallelSerialData.PROTOCOL_TYPE};
    /** Parameters map. */
    private static final Map<String, List<String>> PARAMETERS_MAP =
                                           new HashMap<String, List<String>>();
    static {
        PARAMETERS_MAP.put("dev", Arrays.asList(
                                new String[]{ParallelSerialData.TYPE,
                                             ParallelSerialData.SOURCE_PATH,
                                             ParallelSerialData.TARGET_PORT}));
        PARAMETERS_MAP.put("file", Arrays.asList(
                               new String[]{ParallelSerialData.TYPE,
                                            ParallelSerialData.SOURCE_PATH,
                                            ParallelSerialData.TARGET_PORT}));
        PARAMETERS_MAP.put("null", Arrays.asList(
                                new String[]{ParallelSerialData.TYPE,
                                             ParallelSerialData.TARGET_PORT}));
        PARAMETERS_MAP.put("pipe", Arrays.asList(
                               new String[]{ParallelSerialData.TYPE,
                                            ParallelSerialData.SOURCE_PATH,
                                            ParallelSerialData.TARGET_PORT}));
        PARAMETERS_MAP.put("pty", Arrays.asList(
                               new String[]{ParallelSerialData.TYPE,
                                            ParallelSerialData.TARGET_PORT}));
        PARAMETERS_MAP.put("stdio", Arrays.asList(
                              new String[]{ParallelSerialData.TYPE,
                                           ParallelSerialData.TARGET_PORT}));
        PARAMETERS_MAP.put("tcp", Arrays.asList(
                                new String[]{
                                   ParallelSerialData.TYPE,
                                   ParallelSerialData.SOURCE_MODE, /* one or
                                                                     another */
                                   ParallelSerialData.BIND_SOURCE_HOST,
                                   ParallelSerialData.BIND_SOURCE_SERVICE,
                                   ParallelSerialData.CONNECT_SOURCE_HOST,
                                   ParallelSerialData.CONNECT_SOURCE_SERVICE,
                                   ParallelSerialData.PROTOCOL_TYPE,
                                   ParallelSerialData.TARGET_PORT}));
        PARAMETERS_MAP.put("udp", Arrays.asList(
                                new String[]{
                                   ParallelSerialData.TYPE,
                                   ParallelSerialData.BIND_SOURCE_HOST,
                                   ParallelSerialData.BIND_SOURCE_SERVICE,
                                   ParallelSerialData.CONNECT_SOURCE_HOST,
                                   ParallelSerialData.CONNECT_SOURCE_SERVICE,
                                   ParallelSerialData.TARGET_PORT}));

        PARAMETERS_MAP.put("unix", Arrays.asList(
                               new String[]{ParallelSerialData.TYPE,
                                            ParallelSerialData.SOURCE_MODE,
                                            ParallelSerialData.SOURCE_PATH,
                                            ParallelSerialData.TARGET_PORT}));
        PARAMETERS_MAP.put("vc", Arrays.asList(
                             new String[]{ParallelSerialData.TYPE,
                                          ParallelSerialData.TARGET_PORT}));
    }
    /** Field type. */
    private static final Map<String, GuiComboBox.Type> FIELD_TYPES =
                                       new HashMap<String, GuiComboBox.Type>();
    /** Short name. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();
    static {
        SHORTNAME_MAP.put(ParallelSerialData.TYPE, "Type");
        SHORTNAME_MAP.put(ParallelSerialData.SOURCE_PATH, "Source Path");
        SHORTNAME_MAP.put(ParallelSerialData.SOURCE_MODE, "Mode");
        SHORTNAME_MAP.put(ParallelSerialData.BIND_SOURCE_HOST, "Server");
        SHORTNAME_MAP.put(ParallelSerialData.BIND_SOURCE_SERVICE,
                          "Server Port");
        SHORTNAME_MAP.put(ParallelSerialData.CONNECT_SOURCE_HOST, "Client");
        SHORTNAME_MAP.put(ParallelSerialData.CONNECT_SOURCE_SERVICE,
                          "Client Port");
        SHORTNAME_MAP.put(ParallelSerialData.PROTOCOL_TYPE, "Protocol");
        SHORTNAME_MAP.put(ParallelSerialData.TARGET_PORT, "Target Port");
        FIELD_TYPES.put(ParallelSerialData.PROTOCOL_TYPE,
                        GuiComboBox.Type.RADIOGROUP);
    }

    /** Whether the parameter is enabled only in advanced mode. */
    private static final Set<String> IS_ENABLED_ONLY_IN_ADVANCED =
                                                        new HashSet<String>();

    /** Whether the parameter is required. */
    private static final Set<String> IS_REQUIRED =
        new HashSet<String>(
                      Arrays.asList(new String[]{ParallelSerialData.TYPE}));

    /** Default name. */
    private static final Map<String, String> DEFAULTS_MAP =
                                                 new HashMap<String, String>();
    /** Preferred values map. */
    private static final Map<String, String> PREFERRED_MAP =
                                                 new HashMap<String, String>();
    /** Possible values. */
    private static final Map<String, StringInfo[]> POSSIBLE_VALUES =
                                           new HashMap<String, StringInfo[]>();
    static {
        POSSIBLE_VALUES.put(
            ParallelSerialData.TYPE,
            new StringInfo[]{new StringInfo("Physical Host Char Device (dev)",
                                            "dev",
                                            null),
                             new StringInfo("Plain File", "file", null),
                             new StringInfo("Null Device", "null", null),
                             new StringInfo("Named Pipe", "pipe", null),
                             new StringInfo("PTTY", "pty", null),
                             new StringInfo("Standard Input/Output",
                                            "stdio",
                                            null),
                             new StringInfo("TCP Console", "tcp", null),
                             new StringInfo("UDP Console", "udp", null),
                             new StringInfo("Unix Socket", "unix", null),
                             new StringInfo("Virtual Console", "vc", null)});
        POSSIBLE_VALUES.put(
            ParallelSerialData.SOURCE_MODE,
            new StringInfo[]{new StringInfo("Server (bind)", "bind", null),
                             new StringInfo("Client (connect)",
                                            "connect",
                                            null)});
        POSSIBLE_VALUES.put(
            ParallelSerialData.PROTOCOL_TYPE,
            new StringInfo[]{new StringInfo("telnet", "telnet", null),
                             new StringInfo("raw", "raw", null)});
        DEFAULTS_MAP.put(ParallelSerialData.TARGET_PORT, "generate");
        PREFERRED_MAP.put(ParallelSerialData.BIND_SOURCE_HOST, "127.0.0.1");
        PREFERRED_MAP.put(ParallelSerialData.BIND_SOURCE_SERVICE, "4555");
        PREFERRED_MAP.put(ParallelSerialData.CONNECT_SOURCE_HOST, "127.0.0.1");
        PREFERRED_MAP.put(ParallelSerialData.CONNECT_SOURCE_SERVICE, "4556");
        PREFERRED_MAP.put(ParallelSerialData.PROTOCOL_TYPE, "telnet");
    }
    /** Table panel. */
    private JComponent tablePanel = null;
    /** Previous value of the type. */
    private String prevType = null;
    /** Creates the VMSParallelSerialInfo object. */
    public VMSParallelSerialInfo(
                             final String name,
                             final Browser browser,
                             final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(name, browser, vmsVirtualDomainInfo);
    }

    /** Adds disk table with only this disk to the main panel. */
    protected final void addHardwareTable(final JPanel mainPanel) {
        tablePanel = getTablePanel(getTableScreenName(),
                                   getTableName(),
                                   getNewBtn0(getVMSVirtualDomainInfo()));
        if (getResource().isNew()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    tablePanel.setVisible(false);
                }
            });
        }
        mainPanel.add(tablePanel);
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
        return PREFERRED_MAP.get(param);
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
                getApplyButton().setEnabled(false);
            }
        });
        final Map<String, String> parameters = getHWParametersAndSave();

        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                parameters.put(ParallelSerialData.SAVED_TYPE,
                               getParamSaved(ParallelSerialData.TYPE));
                final String domainName =
                                getVMSVirtualDomainInfo().getDomainName();
                final Node domainNode = vmsxml.getDomainNode(domainName);
                modifyXML(vmsxml, domainNode, domainName, parameters);
                vmsxml.saveAndDefine(domainNode, domainName);
            }
            getResource().setNew(false);
        }
        getBrowser().reload(getNode());
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            getBrowser().periodicalVMSUpdate(h);
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                tablePanel.setVisible(true);
            }
        });
        checkResourceFields(null, getParametersFromXML());
    }

    /** Returns device parameters. */
    protected Map<String, String> getHWParametersAndSave() {
        final List<String> params = PARAMETERS_MAP.get(
                                    getComboBoxValue(ParallelSerialData.TYPE));
        final Map<String, String> parameters = new HashMap<String, String>();
        String type = null;
        for (final String param : params) {
            final String value = getComboBoxValue(param);
            if (ParallelSerialData.TYPE.equals(param)) {
                type = value;
            }
            if (!Tools.areEqual(getParamSaved(param), value)) {
                parameters.put(param, value);
                getResource().setValue(param, value);
            }
        }
        return parameters;
    }

    /** Returns whether this parameter is advanced. */
    protected final boolean isAdvanced(final String param) {
        return false;
    }

    /** Whether the parameter should be enabled. */
    protected final boolean isEnabled(final String param) {
        if (ParallelSerialData.TARGET_PORT.equals(param)) {
            return false;
        } else if (!getResource().isNew()
                   && ParallelSerialData.TYPE.equals(param)) {
            return false;
        }
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
        if (ParallelSerialData.TYPE.equals(param)) {
            if (!newValue.equals(prevType)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (final String param : PARAMETERS) {
                            paramComboBoxGet(param, null).setVisible(
                                 PARAMETERS_MAP.get(newValue).contains(param));
                        }
                    }
                });
            }
        }
        if (isRequired(param) && (newValue == null || "".equals(newValue))) {
            return false;
        }
        return true;
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
        if (ParallelSerialData.SOURCE_PATH.equals(param)) {
            final String sourceFile =
                                getParamSaved(ParallelSerialData.SOURCE_PATH);
            final String regexp = "[^/]$";
            final MyButton fileChooserBtn = new MyButton("Browse...");
            final GuiComboBox paramCB = new GuiComboBox(
                                  sourceFile,
                                  null,
                                  null, /* units */
                                  GuiComboBox.Type.TEXTFIELD,
                                  regexp,
                                  width,
                                  null, /* abbrv */
                                  new AccessMode(getAccessType(param),
                                                 false), /* only adv. mode */
                                  fileChooserBtn);
            if (Tools.isWindows()) {
                /* does not work on windows and I tries, ultimatly because
                   FilePane.usesShellFolder(fc) in BasicFileChooserUI returns
                   true and it is not possible to descent into a directory.
                   TODO: It may work in the future.
                */
                paramCB.setTFButtonEnabled(false);
            }
            fileChooserBtn.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final Thread t = new Thread(new Runnable() {
                        public void run() {
                            final String type = getComboBoxValue(
                                                      ParallelSerialData.TYPE);
                            String directory;
                            if ("dev".equals(type)) {
                                directory = "/dev";
                            } else {
                                directory = "/";
                            }
                            startFileChooser(paramCB, directory);
                        }
                    });
                    t.start();
                }
            });
            paramComboBoxAdd(param, prefix, paramCB);
            return paramCB;
        } else {
            final GuiComboBox paramCB =
                                 super.getParamComboBox(param, prefix, width);
            if (ParallelSerialData.TYPE.equals(param)
                || ParallelSerialData.SOURCE_MODE.equals(param)) {
                paramCB.setAlwaysEditable(false);
            }
            return paramCB;
        }
    }

    /** Returns "add new" button. */
    protected abstract MyButton getNewBtn0(final VMSVirtualDomainInfo vdi);

    /** Modify device xml. */
    protected abstract void modifyXML(final VMSXML vmsxml,
                                      final Node node,
                                      final String domainName,
                                      final Map<String, String> params);
    /** Return table name that appears on the screen. */
    protected abstract String getTableScreenName();
    /** Return table name. */
    protected abstract String getTableName();

    /** Return saved value of target port. */
    public final String getTargetPort() {
        return getParamSaved(ParallelSerialData.TARGET_PORT);
    }
}
