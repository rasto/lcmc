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
package lcmc.gui.resources.vms;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JComponent;
import javax.swing.JPanel;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.common.domain.StringValue;
import lcmc.vm.domain.VmsXml;
import lcmc.vm.domain.ParallelSerialData;
import lcmc.common.domain.Value;
import lcmc.gui.Browser;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import org.w3c.dom.Node;

/**
 * This class holds info about virtual parallel or serial device.
 */
@Named
public abstract class ParallelSerialInfo extends HardwareInfo {
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
    /** Field type. */
    private static final Map<String, Widget.Type> FIELD_TYPES =
                                       new HashMap<String, Widget.Type>();
    /** Short name. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();

    /** Whether the parameter is required. */
    private static final Collection<String> IS_REQUIRED =
        new HashSet<String>(
                      Arrays.asList(new String[]{ParallelSerialData.TYPE}));

    /** Default name. */
    private static final Map<String, Value> DEFAULTS_MAP =
                                                 new HashMap<String, Value>();
    /** Preferred values map. */
    private static final Map<String, Value> PREFERRED_MAP =
                                                 new HashMap<String, Value>();
    /** Possible values. */
    private static final Map<String, Value[]> POSSIBLE_VALUES =
                                           new HashMap<String, Value[]>();
    static {
        PARAMETERS_MAP.put("dev", Arrays.asList(
            ParallelSerialData.TYPE,
                                              ParallelSerialData.SOURCE_PATH,
                                              ParallelSerialData.TARGET_PORT));
        PARAMETERS_MAP.put("file", Arrays.asList(ParallelSerialData.TYPE,
                                                 ParallelSerialData.SOURCE_PATH,
                                                 ParallelSerialData.TARGET_PORT));
        PARAMETERS_MAP.put("null", Arrays.asList(
            ParallelSerialData.TYPE,
                                              ParallelSerialData.TARGET_PORT));
        PARAMETERS_MAP.put("pipe", Arrays.asList(
            ParallelSerialData.TYPE,
                                              ParallelSerialData.SOURCE_PATH,
                                              ParallelSerialData.TARGET_PORT));
        PARAMETERS_MAP.put("pty", Arrays.asList(
            ParallelSerialData.TYPE,
                                              ParallelSerialData.TARGET_PORT));
        PARAMETERS_MAP.put("stdio", Arrays.asList(
            ParallelSerialData.TYPE,
                                              ParallelSerialData.TARGET_PORT));
        PARAMETERS_MAP.put("tcp", Arrays.asList(
            ParallelSerialData.TYPE,
                                   ParallelSerialData.SOURCE_MODE, /* one or
                                   another */
                                   ParallelSerialData.BIND_SOURCE_HOST,
                                   ParallelSerialData.BIND_SOURCE_SERVICE,
                                   ParallelSerialData.CONNECT_SOURCE_HOST,
                                   ParallelSerialData.CONNECT_SOURCE_SERVICE,
                                   ParallelSerialData.PROTOCOL_TYPE,
                                   ParallelSerialData.TARGET_PORT));
        PARAMETERS_MAP.put("udp", Arrays.asList(
            ParallelSerialData.TYPE,
                                     ParallelSerialData.BIND_SOURCE_HOST,
                                     ParallelSerialData.BIND_SOURCE_SERVICE,
                                     ParallelSerialData.CONNECT_SOURCE_HOST,
                                     ParallelSerialData.CONNECT_SOURCE_SERVICE,
                                     ParallelSerialData.TARGET_PORT));
        PARAMETERS_MAP.put("unix", Arrays.asList(
            ParallelSerialData.TYPE,
                                              ParallelSerialData.SOURCE_MODE,
                                              ParallelSerialData.SOURCE_PATH,
                                              ParallelSerialData.TARGET_PORT));
        PARAMETERS_MAP.put("vc", Arrays.asList(ParallelSerialData.TYPE,
                                               ParallelSerialData.TARGET_PORT));
    }
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
                        Widget.Type.RADIOGROUP);
    }
    static {
        POSSIBLE_VALUES.put(
            ParallelSerialData.TYPE,
            new Value[]{new StringValue("dev", "Physical Host Char Device (dev)"),
                        new StringValue("file", "Plain File"),
                        new StringValue( "null", "Null Device"),
                        new StringValue("pipe", "Named Pipe"),
                        new StringValue("pty", "PTTY"),
                        new StringValue("stdio","Standard Input/Output"),
                        new StringValue("tcp", "TCP Console"),
                        new StringValue("udp", "UDP Console"),
                        new StringValue("unix", "Unix Socket"),
                        new StringValue("vc", "Virtual Console")});
        POSSIBLE_VALUES.put(
            ParallelSerialData.SOURCE_MODE,
            new Value[]{new StringValue("bind", "Server (bind)"),
                        new StringValue("connect", "Client (connect)")});
        POSSIBLE_VALUES.put(
            ParallelSerialData.PROTOCOL_TYPE,
            new Value[]{new StringValue("telnet"),
                        new StringValue("raw")});
        DEFAULTS_MAP.put(ParallelSerialData.TARGET_PORT, new StringValue("generate"));
        PREFERRED_MAP.put(ParallelSerialData.BIND_SOURCE_HOST, new StringValue("127.0.0.1"));
        PREFERRED_MAP.put(ParallelSerialData.BIND_SOURCE_SERVICE, new StringValue("4555"));
        PREFERRED_MAP.put(ParallelSerialData.CONNECT_SOURCE_HOST, new StringValue("127.0.0.1"));
        PREFERRED_MAP.put(ParallelSerialData.CONNECT_SOURCE_SERVICE, new StringValue("4556"));
        PREFERRED_MAP.put(ParallelSerialData.PROTOCOL_TYPE, new StringValue("telnet"));
    }
    /** Table panel. */
    private JComponent tablePanel = null;
    @Inject
    private Application application;
    @Inject
    private WidgetFactory widgetFactory;

    void init(final String name, final Browser browser, final DomainInfo vmsVirtualDomainInfo) {
        super.init(name, browser, vmsVirtualDomainInfo);
    }

    /** Adds disk table with only this disk to the main panel. */
    @Override
    protected final void addHardwareTable(final JPanel mainPanel) {
        tablePanel = getTablePanel(getTableScreenName(),
                                   getTableName(),
                                   getNewBtn0(getVMSVirtualDomainInfo()));
        if (getResource().isNew()) {
            application.invokeLater(new Runnable() {
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
    protected final String getParamLongDesc(final String param) {
        return getParamShortDesc(param);
    }

    /** Returns short description of the specified parameter. */
    @Override
    protected final String getParamShortDesc(final String param) {
        final String name = SHORTNAME_MAP.get(param);
        if (name == null) {
            return param;
        }
        return name;
    }

    /** Returns preferred value for specified parameter. */
    @Override
    protected Value getParamPreferred(final String param) {
        return PREFERRED_MAP.get(param);
    }

    /** Returns default value for specified parameter. */
    @Override
    protected Value getParamDefault(final String param) {
        return DEFAULTS_MAP.get(param);
    }

    /** Returns parameters. */
    @Override
    public final String[] getParametersFromXML() {
        return PARAMETERS.clone();
    }

    /** Returns possible choices for drop down lists. */
    @Override
    protected final Value[] getParamPossibleChoices(final String param) {
        return POSSIBLE_VALUES.get(param);
    }

    /** Returns section to which the specified parameter belongs. */
    @Override
    protected final String getSection(final String param) {
        return "Display Options";
    }

    /** Returns true if the specified parameter is required. */
    @Override
    protected final boolean isRequired(final String param) {
        return IS_REQUIRED.contains(param);
    }

    /** Returns true if the specified parameter is integer. */
    @Override
    protected final boolean isInteger(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is label. */
    @Override
    protected final boolean isLabel(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is of time type. */
    @Override
    protected final boolean isTimeType(final String param) {
        return false;
    }

    /** Returns whether parameter is checkbox. */
    @Override
    protected final boolean isCheckBox(final String param) {
        return false;
    }

    /** Returns the type of the parameter. */
    @Override
    protected final String getParamType(final String param) {
        return "undef"; // TODO:
    }

    /** Returns the regexp of the parameter. */
    @Override
    protected final String getParamRegexp(final String param) {
        return null;
    }

    /** Returns type of the field. */
    @Override
    protected final Widget.Type getFieldType(final String param) {
        return FIELD_TYPES.get(param);
    }

    /** Applies the changes. */
    @Override
    final void apply(final Application.RunMode runMode) {
        if (Application.isTest(runMode)) {
            return;
        }
        application.invokeAndWait(new Runnable() {
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

        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            if (vmsXml != null) {
                final Value type = getParamSaved(ParallelSerialData.TYPE);
                if (type == null) {
                    parameters.put(ParallelSerialData.SAVED_TYPE, null);
                } else {
                    parameters.put(ParallelSerialData.SAVED_TYPE,
                                   type.getValueForConfig());
                }
                final String domainName =
                                getVMSVirtualDomainInfo().getDomainName();
                final Node domainNode = vmsXml.getDomainNode(domainName);
                modifyXML(vmsXml, domainNode, domainName, parameters);
                final String virshOptions =
                                   getVMSVirtualDomainInfo().getVirshOptions();
                vmsXml.saveAndDefine(domainNode, domainName, virshOptions);
            }
            getResource().setNew(false);
        }
        getBrowser().reloadNode(getNode(), false);
        getBrowser().periodicalVmsUpdate(
                getVMSVirtualDomainInfo().getDefinedOnHosts());
        application.invokeLater(new Runnable() {
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
        application.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                getInfoPanel();
            }
        });
        final List<String> params = PARAMETERS_MAP.get(
                                    getComboBoxValue(ParallelSerialData.TYPE).getValueForConfig());
        final Map<String, String> parameters = new HashMap<String, String>();
        if (params == null) {
            return parameters;
        }
        for (final String param : params) {
            final Value value = getComboBoxValue(param);
            if (allParams || !Tools.areEqual(getParamSaved(param), value)) {
                if (Tools.areEqual(getParamDefault(param), value)) {
                    parameters.put(param, null);
                } else {
                    parameters.put(param, value.getValueForConfig());
                }
            }
        }
        return parameters;
    }

    /** Returns whether this parameter is advanced. */
    @Override
    protected final boolean isAdvanced(final String param) {
        return false;
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected final String isEnabled(final String param) {
        if (ParallelSerialData.TARGET_PORT.equals(param)) {
            return "";
        } else if (!getResource().isNew()
                   && ParallelSerialData.TYPE.equals(param)) {
            return "";
        }
        return null;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override
    protected final boolean isEnabledOnlyInAdvancedMode(final String param) {
         return false;
    }

    /** Returns access type of this parameter. */
    @Override
    protected final Application.AccessType getAccessType(final String param) {
        return Application.AccessType.ADMIN;
    }

    /** Returns true if the value of the parameter is ok. */
    @Override
    protected final boolean checkParam(final String param,
                                       final Value newValue) {
        if (ParallelSerialData.TYPE.equals(param)) {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (final String thisParam : PARAMETERS) {
                        getWidget(thisParam, null).setVisible(
                             PARAMETERS_MAP.get(newValue.getValueForConfig()).contains(thisParam));
                    }
                }
            });
        }
        return !isRequired(param)
               || (newValue != null && !newValue.isNothingSelected());
    }

    /**
     * Returns whether this item is removeable (null), or string why it isn't.
     */
    @Override
    protected final String isRemoveable() {
        return null;
    }

    /** Returns combo box for parameter. */
    @Override
    protected final Widget createWidget(final String param,
                                        final String prefix,
                                        final int width) {
        if (ParallelSerialData.SOURCE_PATH.equals(param)) {
            final Value sourceFile =
                                getParamSaved(ParallelSerialData.SOURCE_PATH);
            final String regexp = ".*[^/]$";
            final MyButton fileChooserBtn = widgetFactory.createButton("Browse...");
            final Widget paramWi = widgetFactory.createInstance(
                                  Widget.Type.TEXTFIELD,
                                  sourceFile,
                                  Widget.NO_ITEMS,
                                  regexp,
                                  width,
                                  Widget.NO_ABBRV,
                                  new AccessMode(getAccessType(param),
                                                 false), /* only adv. mode */
                                  fileChooserBtn);
            if (Tools.isWindows()) {
                /* does not work on windows and I tries, ultimatly because
                   FilePane.usesShellFolder(fc) in BasicFileChooserUI returns
                   true and it is not possible to descent into a directory.
                   TODO: It may work in the future.
                */
                paramWi.setTFButtonEnabled(false);
            }
            fileChooserBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final String oldDir = paramWi.getStringValue();
                            final String directory;
                            if (oldDir == null || oldDir.isEmpty()) {
                                final String type = getComboBoxValue(
                                                      ParallelSerialData.TYPE).getValueForConfig();
                                if ("dev".equals(type)) {
                                    directory = "/dev";
                                } else {
                                    directory = "/";
                                }
                            } else {
                                directory = oldDir;
                            }
                            startFileChooser(paramWi,
                                             directory,
                                             FILECHOOSER_FILE_ONLY);
                        }
                    });
                    t.start();
                }
            });
            widgetAdd(param, prefix, paramWi);
            return paramWi;
        } else {
            final Widget paramWi = super.createWidget(param, prefix, width);
            if (ParallelSerialData.TYPE.equals(param)
                || ParallelSerialData.SOURCE_MODE.equals(param)) {
                paramWi.setAlwaysEditable(false);
            }
            return paramWi;
        }
    }

    /** Returns "add new" button. */
    protected abstract MyButton getNewBtn0(final DomainInfo vdi);

    /** Return table name that appears on the screen. */
    protected abstract String getTableScreenName();
    /** Return table name. */
    protected abstract String getTableName();

    /** Return saved value of target port. */
    final String getTargetPort() {
        return getParamSaved(ParallelSerialData.TARGET_PORT).getValueForConfig();
    }

    /** Returns real parameters. */
    @Override
    String[] getRealParametersFromXML() {
       final Value type = getComboBoxValue(ParallelSerialData.TYPE);
       final String typeS;
       if (type == null) {
           typeS = null;
       } else {
           typeS = type.getValueForConfig();
       }

       final List<String> params = PARAMETERS_MAP.get(typeS);
       if (params == null) {
           return PARAMETERS.clone();
       }
       return params.toArray(new String[params.size()]);
    }
}
