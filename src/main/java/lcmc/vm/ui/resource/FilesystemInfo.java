/*
 * This file is part of Linux Cluster Management Console by Rasto Levrinc.
 *
 * Copyright (C) 2012, Rasto Levrinc
 *
 * LCMC is free software; you can redistribute it and/or
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.ui.treemenu.TreeMenuController;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.common.domain.StringValue;
import lcmc.vm.domain.VmsXml;
import lcmc.vm.domain.data.FilesystemData;
import lcmc.common.domain.Value;
import lcmc.common.ui.Browser;
import lcmc.drbd.ui.resource.BlockDevInfo;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.domain.util.Tools;
import org.w3c.dom.Node;

/**
 * This class holds info about Virtual filesystem.
 */
@Named
public final class FilesystemInfo extends HardwareInfo {
    /** Parameters. */
    private static final String[] PARAMETERS = {FilesystemData.TYPE,
                                                FilesystemData.SOURCE_DIR,
                                                FilesystemData.SOURCE_NAME,
                                                FilesystemData.TARGET_DIR};
    /** Whether the parameter is enabled only in advanced mode. */
    private static final Collection<String> IS_ENABLED_ONLY_IN_ADVANCED =
                            new HashSet<String>(Arrays.asList(new String[]{}));
    /** Whether the parameter is required. */
    private static final Collection<String> IS_REQUIRED =
        new HashSet<String>(Arrays.asList(new String[]{
                                                FilesystemData.TYPE,
                                                FilesystemData.TARGET_DIR}));
    /** Field type. */
    private static final Map<String, Widget.Type> FIELD_TYPES =
                                       new HashMap<String, Widget.Type>();
    /** Short name. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();

    /** Preferred values. */
    private static final Map<String, Value> PREFERRED_MAP =
                                                 new HashMap<String, Value>();
    /** Possible values. */
    private static final Map<String, Value[]> POSSIBLE_VALUES =
                                              new HashMap<String, Value[]>();
    /** Filesystem types. */
    public static final Value MOUNT_TYPE = new StringValue("mount", "Mount");
    /** Filesystem types. */
    public static final Value TEMPLATE_TYPE =
                                      new StringValue("template", "Template");

    /** LXC source dir. */
    private static final String LXC_SOURCE_DIR = "/var/lib/lxc";

    static {
        FIELD_TYPES.put(FilesystemData.TYPE, Widget.Type.RADIOGROUP);
        FIELD_TYPES.put(FilesystemData.SOURCE_DIR, Widget.Type.COMBOBOX);
        FIELD_TYPES.put(FilesystemData.SOURCE_NAME, Widget.Type.TEXTFIELD);
        FIELD_TYPES.put(FilesystemData.TARGET_DIR, Widget.Type.COMBOBOX);
    }
    static {
        SHORTNAME_MAP.put(FilesystemData.TYPE, "Type");
        SHORTNAME_MAP.put(FilesystemData.SOURCE_DIR, "Source Dir");
        SHORTNAME_MAP.put(FilesystemData.SOURCE_NAME, "Source Name");
        SHORTNAME_MAP.put(FilesystemData.TARGET_DIR, "Target Dir");
    }
    static {
        POSSIBLE_VALUES.put(FilesystemData.TYPE,
                            new Value[]{MOUNT_TYPE, TEMPLATE_TYPE});
        POSSIBLE_VALUES.put(FilesystemData.TARGET_DIR, new Value[]{new StringValue("/")});
        PREFERRED_MAP.put(FilesystemData.TYPE, new StringValue("mount"));
        PREFERRED_MAP.put(FilesystemData.TARGET_DIR, new StringValue("/"));
    }

    @Inject
    private Application application;
    @Inject
    private SwingUtils swingUtils;
    @Inject
    private WidgetFactory widgetFactory;

    /** Source file combo box, so that it can be disabled, depending on type. */
    private final Map<String, Widget> sourceDirWi =
                                            new HashMap<String, Widget>();
    /** Source block combo box, so that it can be disabled, depending on type.*/
    private final Map<String, Widget> sourceNameWi =
                                            new HashMap<String, Widget>();
    /** Choices for source directories. */
    private Value[] sourceDirs;
    /** Table panel. */
    private JComponent tablePanel = null;
    @Inject
    private TreeMenuController treeMenuController;

    void init(final String name, final Browser browser, final DomainInfo domainInfo) {
        super.init(name, browser, domainInfo);
        final List<Host> hosts = getVMSVirtualDomainInfo().getDefinedOnHosts();
        final Set<Value> sds = new LinkedHashSet<Value>();
        sds.add(new StringValue());
        for (final Host h : hosts) {
            for (final String guiOps : h.getHostParser().getGuiOptions(Host.VM_FILESYSTEM_SOURCE_DIR_LXC)) {
                sds.add(new StringValue(guiOps));
            }
        }
        sourceDirs = sds.toArray(new Value[sds.size()]);
    }

    /** Adds fs table with only this fs to the main panel. */
    @Override
    protected void addHardwareTable(final JPanel mainPanel) {
        tablePanel = getTablePanel("Filesystem",
                                   DomainInfo.FILESYSTEM_TABLE,
                                   getVMSVirtualDomainInfo().getNewFilesystemBtn());
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
        return BlockDevInfo.HARDDISK_ICON;
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
        return PREFERRED_MAP.get(param);
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

    /** Returns real parameters. */
    @Override
    public String[] getRealParametersFromXML() {
        return PARAMETERS.clone();
    }

    /** Returns possible choices for drop down lists. */
    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        if (FilesystemData.SOURCE_DIR.equals(param)) {
            return sourceDirs;
        }
        return POSSIBLE_VALUES.get(param);
    }

    /** Returns section to which the specified parameter belongs. */
    @Override
    protected String getSection(final String param) {
        return "Filesystem Options";
    }

    /** Returns true if the specified parameter is required. */
    @Override
    protected boolean isRequired(final String param) {
        final Value type = getComboBoxValue(FilesystemData.TYPE);
        if ((FilesystemData.SOURCE_DIR.equals(param)
              && MOUNT_TYPE.equals(type))
             || (FilesystemData.SOURCE_NAME.equals(param)
                 && TEMPLATE_TYPE.equals(type))) {
            return true;
        }
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
        final String[] params = getRealParametersFromXML();
        final Map<String, String> parameters = new HashMap<String, String>();
        for (final String param : params) {
            final Value value = getComboBoxValue(param);
            if (allParams) {
                if (Tools.areEqual(getParamDefault(param), value)) {
                    parameters.put(param, null);
                } else {
                    parameters.put(param, value.getValueForConfig());
                }
            } else if (!Tools.areEqual(getParamSaved(param), value)) {
                if (Tools.areEqual(getParamDefault(param), value)) {
                    parameters.put(param, null);
                } else {
                    parameters.put(param, value.getValueForConfig());
                }
            }
        }
        parameters.put(FilesystemData.SAVED_TARGET_DIR, getName());
        setName(getParamSavedForConfig(FilesystemData.TARGET_DIR));
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
        treeMenuController.reloadNode(getNode(), false);
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

    /** Modify device xml. */
    @Override
    protected void modifyXML(final VmsXml vmsXml,
                             final Node node,
                             final String domainName,
                             final Map<String, String> params) {
        if (vmsXml != null) {
            vmsXml.modifyFilesystemXML(node, domainName, params);
        }
    }

    /** Returns data for the table. */
    @Override
    protected Object[][] getTableData(final String tableName) {
        if (DomainInfo.HEADER_TABLE.equals(tableName)) {
            return getVMSVirtualDomainInfo().getMainTableData();
        } else if (DomainInfo.FILESYSTEM_TABLE.equals(tableName)) {
            if (getResource().isNew()) {
                return new Object[][]{};
            }
            return new Object[][]{
                            getVMSVirtualDomainInfo().getFilesystemDataRow(
                                  getName(),
                                  null,
                                  getVMSVirtualDomainInfo().getFilesystems(),
                                  true)};
        }
        return new Object[][]{};
    }

    /** Returns whether this parameter is advanced. */
    @Override
    protected boolean isAdvanced(final String param) {
        return false;
    }

    /** Returns access type of this parameter. */
    @Override
    protected AccessMode.Type getAccessType(final String param) {
        return AccessMode.ADMIN;
    }
    /** Returns true if the value of the parameter is ok. */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        if (FilesystemData.TYPE.equals(param)) {
            swingUtils.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (final Map.Entry<String, Widget> entry : sourceDirWi.entrySet()) {
                        entry.getValue().setVisible(
                                MOUNT_TYPE.equals(newValue));
                    }
                    for (final Map.Entry<String, Widget> entry : sourceNameWi.entrySet()) {
                        entry.getValue().setVisible(
                                TEMPLATE_TYPE.equals(newValue));
                    }
                }
            });
        }
        return !isRequired(param)
               || (newValue != null && newValue.getValueForConfig() != null && !newValue.getValueForConfig().isEmpty());
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override
    protected AccessMode.Mode isEnabledOnlyInAdvancedMode(final String param) {
         return IS_ENABLED_ONLY_IN_ADVANCED.contains(param) ? AccessMode.ADVANCED : AccessMode.NORMAL;
    }

    /** Updates parameters. */
    @Override
    void updateParameters() {
        final Map<String, FilesystemData> filesystems =
                                    getVMSVirtualDomainInfo().getFilesystems();
        if (filesystems != null) {
            final FilesystemData filesystemData = filesystems.get(getName());
            if (filesystemData != null) {
                for (final String param : getParametersFromXML()) {
                    final Value oldValue = getParamSaved(param);
                    Value value = getParamSaved(param);
                    final Widget wi = getWidget(param, null);
                    for (final Host h
                            : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                        final VmsXml vmsXml = getBrowser().getVmsXml(h);
                        if (vmsXml != null) {
                            final Value savedValue =
                                               filesystemData.getValue(param);
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
        updateTable(DomainInfo.FILESYSTEM_TABLE);
        setApplyButtons(null, getRealParametersFromXML());
    }

    /** Removes this fs without confirmation dialog. */
    @Override
    protected void removeMyselfNoConfirm(final Application.RunMode runMode) {
        final String virshOptions = getVMSVirtualDomainInfo().getVirshOptions();
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            if (vmsXml != null) {
                final Map<String, String> parameters =
                                                new HashMap<String, String>();
                parameters.put(FilesystemData.SAVED_TARGET_DIR, getName());
                vmsXml.removeFilesystemXML(
                                     getVMSVirtualDomainInfo().getDomainName(),
                                     parameters,
                                     virshOptions);
            }
        }
        getBrowser().periodicalVmsUpdate(
                getVMSVirtualDomainInfo().getDefinedOnHosts());
        treeMenuController.removeNode(getNode());
    }

    /** Returns string representation. */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(30);
        final String name = getName();
        if (name == null) {
            return "new FS...";
        }
        final Value saved;
        final Value type = getComboBoxValue(FilesystemData.TYPE);

        if (MOUNT_TYPE.equals(type)) {
            saved = getParamSaved(FilesystemData.SOURCE_DIR);
        } else {
            saved = getParamSaved(FilesystemData.SOURCE_NAME);
        }
        if (saved == null) {
            s.append("new...");
        } else {
            s.append(saved.getValueForConfig());
        }
        s.append(" -> ");
        s.append(name);
        return s.toString();
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
        if (FilesystemData.SOURCE_DIR.equals(param)) {
            final Value sourceDir = getParamSaved(FilesystemData.SOURCE_DIR);
            final MyButton fileChooserBtn = widgetFactory.createButton("Browse...");
            application.makeMiniButton(fileChooserBtn);
            final String regexp = ".*[^/]?$";
            final Widget paramWi = widgetFactory.createInstance(
                                     getFieldType(param),
                                     sourceDir,
                                     getParamPossibleChoices(param),
                                     regexp,
                                     width,
                                     Widget.NO_ABBRV,
                                     new AccessMode(getAccessType(param), AccessMode.NORMAL),
                                     fileChooserBtn);
            paramWi.setAlwaysEditable(true);
            if (prefix == null) {
                sourceDirWi.put("", paramWi);
            } else {
                sourceDirWi.put(prefix, paramWi);
            }
            if (Tools.isWindows()) {
                paramWi.setTFButtonEnabled(false);
            }
            fileChooserBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final String file;
                            final String oldFile = paramWi.getStringValue();
                            if (oldFile == null || oldFile.isEmpty()) {
                                file = LXC_SOURCE_DIR;
                            } else {
                                file = oldFile;
                            }
                            startFileChooser(paramWi,
                                             file,
                                             FILECHOOSER_DIR_ONLY);
                        }
                    });
                    t.start();
                }
            });
            widgetAdd(param, prefix, paramWi);
            return paramWi;
        } else if (FilesystemData.SOURCE_NAME.equals(param)) {
            final Value sourceName = getParamSaved(FilesystemData.SOURCE_NAME);
            final Widget paramWi = widgetFactory.createInstance(
                                    getFieldType(param),
                                    sourceName,
                                    getParamPossibleChoices(param),
                                    Widget.NO_REGEXP,
                                    width,
                                    Widget.NO_ABBRV,
                                    new AccessMode(getAccessType(param), AccessMode.NORMAL),
                                    Widget.NO_BUTTON);
            paramWi.setAlwaysEditable(true);
            if (prefix == null) {
                sourceNameWi.put("", paramWi);
            } else {
                sourceNameWi.put(prefix, paramWi);
            }
            widgetAdd(param, prefix, paramWi);
            return paramWi;
        } else {
            final Widget paramWi = super.createWidget(param, prefix, width);

            if (FilesystemData.SOURCE_DIR.equals(param)
                || FilesystemData.TARGET_DIR.equals(param)) {
                paramWi.setAlwaysEditable(true);
            }
            return paramWi;
        }
    }
}
