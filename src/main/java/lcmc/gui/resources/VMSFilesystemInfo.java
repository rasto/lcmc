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
package lcmc.gui.resources;

import lcmc.gui.Browser;
import lcmc.gui.Widget;
import lcmc.data.VMSXML;
import lcmc.data.VMSXML.FilesystemData;
import lcmc.data.Host;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.w3c.dom.Node;

/**
 * This class holds info about Virtual filesystem.
 */
public final class VMSFilesystemInfo extends VMSHardwareInfo {
    /** Source file combo box, so that it can be disabled, depending on type. */
    private final Map<String, Widget> sourceDirWi =
                                            new HashMap<String, Widget>();
    /** Source block combo box, so that it can be disabled, depending on type.*/
    private final Map<String, Widget> sourceNameWi =
                                            new HashMap<String, Widget>();
    /** Choices for source directories */
    private final String[] sourceDirs;
    /** Previous target bus value. */
    private String prevTargetBus = null;
    /** Parameters. */
    private static final String[] PARAMETERS = {FilesystemData.TYPE,
                                                FilesystemData.SOURCE_DIR,
                                                FilesystemData.SOURCE_NAME,
                                                FilesystemData.TARGET_DIR};
    /** Mount parameters. */
    private static final String[] MOUNT_PARAMETERS =
                                               {FilesystemData.TYPE,
                                                FilesystemData.SOURCE_DIR,
                                                FilesystemData.TARGET_DIR};
    /** Template parameters. */
    private static final String[] TEMPLATE_PARAMETERS =
                                               {FilesystemData.TYPE,
                                                FilesystemData.SOURCE_NAME,
                                                FilesystemData.TARGET_DIR};
    /** Whether the parameter is enabled only in advanced mode. */
    private static final Set<String> IS_ENABLED_ONLY_IN_ADVANCED =
                            new HashSet<String>(Arrays.asList(new String[]{}));
    /** Whether the parameter is required. */
    private static final Set<String> IS_REQUIRED =
        new HashSet<String>(Arrays.asList(new String[]{
                                                FilesystemData.TYPE,
                                                FilesystemData.TARGET_DIR}));
    /** Field type. */
    private static final Map<String, Widget.Type> FIELD_TYPES =
                                       new HashMap<String, Widget.Type>();
    /** Target devices depending on the target type. */
    private static final Map<String, String[]> TARGET_DEVICES_MAP =
                                           new HashMap<String, String[]>();
    static {
        FIELD_TYPES.put(FilesystemData.TYPE, Widget.Type.RADIOGROUP);
        FIELD_TYPES.put(FilesystemData.SOURCE_DIR, Widget.Type.COMBOBOX);
        FIELD_TYPES.put(FilesystemData.SOURCE_NAME, Widget.Type.TEXTFIELD);
        FIELD_TYPES.put(FilesystemData.TARGET_DIR, Widget.Type.COMBOBOX);
    }
    /** Short name. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();
    static {
        SHORTNAME_MAP.put(FilesystemData.TYPE, "Type");
        SHORTNAME_MAP.put(FilesystemData.SOURCE_DIR, "Source Dir");
        SHORTNAME_MAP.put(FilesystemData.SOURCE_NAME, "Source Name");
        SHORTNAME_MAP.put(FilesystemData.TARGET_DIR, "Target Dir");
    }

    /** Preferred values. */
    private static final Map<String, String> PREFERRED_MAP =
                                                 new HashMap<String, String>();
    /** Defaults. */
    private static final Map<String, String> DEFAULTS_MAP =
                                                 new HashMap<String, String>();
    /** Possible values. */
    private static final Map<String, Object[]> POSSIBLE_VALUES =
                                              new HashMap<String, Object[]>();
    /** Filesystem types. */
    private static final String MOUNT_TYPE = "mount";
    /** Filesystem types. */
    private static final String TEMPLATE_TYPE = "template";

    /** LXC source dir */
    private static final String LXC_SOURCE_DIR = "/var/lib/lxc";

    static {
        POSSIBLE_VALUES.put(FilesystemData.TYPE,
                        new StringInfo[]{
                             new StringInfo("Mount", MOUNT_TYPE, null),
                             new StringInfo("Template", TEMPLATE_TYPE, null)});
        POSSIBLE_VALUES.put(FilesystemData.TARGET_DIR, new String[]{"/"});
        PREFERRED_MAP.put(FilesystemData.TYPE, "mount");
        PREFERRED_MAP.put(FilesystemData.TARGET_DIR, "/");
    }
    /** Table panel. */
    private JComponent tablePanel = null;

    /** Creates the VMSFilesystemInfo object. */
    VMSFilesystemInfo(final String name, final Browser browser,
                      final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(name, browser, vmsVirtualDomainInfo);
        final List<Host> hosts = getVMSVirtualDomainInfo().getDefinedOnHosts();
        final Set<String> sds = new LinkedHashSet<String>();
        sds.add(null);
        for (final Host h : hosts) {
            sds.addAll(h.getGuiOptions(Host.VM_FILESYSTEM_SOURCE_DIR_LXC));
        }
        sourceDirs = sds.toArray(new String[sds.size()]);
    }

    /** Adds fs table with only this fs to the main panel. */
    @Override
    protected void addHardwareTable(final JPanel mainPanel) {
        tablePanel = getTablePanel("Filesystem",
                                   VMSVirtualDomainInfo.FILESYSTEM_TABLE,
                                   getNewBtn(getVMSVirtualDomainInfo()));
        if (getResource().isNew()) {
            SwingUtilities.invokeLater(new Runnable() {
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
    public ImageIcon getMenuIcon(final boolean testOnly) {
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
    protected String getParamPreferred(final String param) {
        return PREFERRED_MAP.get(param);
    }

    /** Returns default value for specified parameter. */
    @Override
    protected String getParamDefault(final String param) {
        return DEFAULTS_MAP.get(param);
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
    protected Object[] getParamPossibleChoices(final String param) {
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
        final String type = getComboBoxValue(FilesystemData.TYPE);
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
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                getInfoPanel();
            }
        });
        final String[] params = getRealParametersFromXML();
        final Map<String, String> parameters = new HashMap<String, String>();
        for (final String param : params) {
            final String value = getComboBoxValue(param);
            if (allParams) {
                if (Tools.areEqual(getParamDefault(param), value)) {
                    parameters.put(param, null);
                } else {
                    parameters.put(param, value);
                }
            } else if (!Tools.areEqual(getParamSaved(param), value)) {
                if (Tools.areEqual(getParamDefault(param), value)) {
                    parameters.put(param, null);
                } else {
                    parameters.put(param, value);
                }
            }
        }
        parameters.put(FilesystemData.SAVED_TARGET_DIR, getName());
        setName(getParamSaved(FilesystemData.TARGET_DIR));
        return parameters;
    }

    /** Applies the changes. */
    @Override
    void apply(final boolean testOnly) {
        if (testOnly) {
            return;
        }
        Tools.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                getApplyButton().setEnabled(false);
                getRevertButton().setEnabled(false);
            }
        });
        getInfoPanel();
        waitForInfoPanel();
        final Map<String, String> parameters =
                                    getHWParameters(getResource().isNew());
        final String[] params = getRealParametersFromXML();
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                final String domainName =
                                getVMSVirtualDomainInfo().getDomainName();
                final Node domainNode = vmsxml.getDomainNode(domainName);
                modifyXML(vmsxml, domainNode, domainName, parameters);
                final String virshOptions =
                                   getVMSVirtualDomainInfo().getVirshOptions();
                vmsxml.saveAndDefine(domainNode, domainName, virshOptions);
            }
        }
        getResource().setNew(false);
        getBrowser().reload(getNode(), false);
        getBrowser().periodicalVMSUpdate(
                                getVMSVirtualDomainInfo().getDefinedOnHosts());
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tablePanel.setVisible(true);
            }
        });
        if (!testOnly) {
            storeComboBoxValues(params);
        }
        checkResourceFieldsChanged(null, params);
    }

    /** Modify device xml. */
    @Override
    protected void modifyXML(final VMSXML vmsxml,
                             final Node node,
                             final String domainName,
                             final Map<String, String> params) {
        if (vmsxml != null) {
            vmsxml.modifyFilesystemXML(node, domainName, params);
        }
    }

    /** Returns data for the table. */
    @Override
    protected Object[][] getTableData(final String tableName) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return getVMSVirtualDomainInfo().getMainTableData();
        } else if (VMSVirtualDomainInfo.FILESYSTEM_TABLE.equals(tableName)) {
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
    protected ConfigData.AccessType getAccessType(final String param) {
        return ConfigData.AccessType.ADMIN;
    }
    /** Returns true if the value of the parameter is ok. */
    @Override
    protected boolean checkParam(final String param, final String newValue) {
        if (FilesystemData.TYPE.equals(param)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (final String p : sourceDirWi.keySet()) {
                        sourceDirWi.get(p).setVisible(
                                                MOUNT_TYPE.equals(newValue));
                    }
                    for (final String p : sourceNameWi.keySet()) {
                        sourceNameWi.get(p).setVisible(
                                               TEMPLATE_TYPE.equals(newValue));
                    }
                }
            });
        }
        if (isRequired(param) && (newValue == null || "".equals(newValue))) {
            return false;
        }
        return true;
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected String isEnabled(final String param) {
        return null;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override
    protected boolean isEnabledOnlyInAdvancedMode(final String param) {
         return IS_ENABLED_ONLY_IN_ADVANCED.contains(param);
    }

    /** Updates parameters. */
    void updateParameters() {
        final Map<String, FilesystemData> filesystems =
                                    getVMSVirtualDomainInfo().getFilesystems();
        if (filesystems != null) {
            final FilesystemData filesystemData = filesystems.get(getName());
            if (filesystemData != null) {
                for (final String param : getParametersFromXML()) {
                    final String oldValue = getParamSaved(param);
                    String value = getParamSaved(param);
                    final Widget wi = getWidget(param, null);
                    for (final Host h
                            : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                        final VMSXML vmsxml = getBrowser().getVMSXML(h);
                        if (vmsxml != null) {
                            final String savedValue =
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
        updateTable(VMSVirtualDomainInfo.HEADER_TABLE);
        updateTable(VMSVirtualDomainInfo.FILESYSTEM_TABLE);
        setApplyButtons(null, getRealParametersFromXML());
    }

    /** Removes this fs without confirmation dialog. */
    @Override
    protected void removeMyselfNoConfirm(final boolean testOnly) {
        final String virshOptions = getVMSVirtualDomainInfo().getVirshOptions();
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                final Map<String, String> parameters =
                                                new HashMap<String, String>();
                parameters.put(FilesystemData.SAVED_TARGET_DIR, getName());
                vmsxml.removeFilesystemXML(
                                     getVMSVirtualDomainInfo().getDomainName(),
                                     parameters,
                                     virshOptions);
            }
        }
        getBrowser().periodicalVMSUpdate(
                                getVMSVirtualDomainInfo().getDefinedOnHosts());
        removeNode();
    }

    /** Returns string representation. */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(30);
        final String name = getName();
        if (name == null) {
            return "new FS...";
        }
        String saved;
        final String type = getComboBoxValue(FilesystemData.TYPE);

        if (MOUNT_TYPE.equals(type)) {
            saved = getParamSaved(FilesystemData.SOURCE_DIR);
        } else {
            saved = getParamSaved(FilesystemData.SOURCE_NAME);
        }
        if (saved == null) {
            s.append("new...");
        } else {
            s.append(saved);
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

    /** Returns "add new" button. */
    static MyButton getNewBtn(final VMSVirtualDomainInfo vdi) {
        final MyButton newBtn = new MyButton("Add Filesystem");
        newBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        vdi.addFilesystemPanel();
                    }
                });
                t.start();
            }
        });
        return newBtn;
    }

    /** Returns combo box for parameter. */
    @Override
    protected Widget createWidget(final String param,
                                  final String prefix,
                                  final int width) {
        if (FilesystemData.SOURCE_DIR.equals(param)) {
            final String sourceDir = getParamSaved(FilesystemData.SOURCE_DIR);
            final String regexp = ".*[^/]?$";
            final MyButton fileChooserBtn = new MyButton("Browse...");
            fileChooserBtn.miniButton();
            final Widget paramWi = new Widget(
                                  sourceDir,
                                  getParamPossibleChoices(param),
                                  null, /* units */
                                  getFieldType(param),
                                  regexp,
                                  width,
                                  null, /* abbrv */
                                  new AccessMode(getAccessType(param),
                                                 false), /* only adv. mode */
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
                            String file;
                            final String oldFile = paramWi.getStringValue();
                            if (oldFile == null || "".equals(oldFile)) {
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
            final String sourceName = getParamSaved(FilesystemData.SOURCE_NAME);
            final Widget paramWi = new Widget(
                                  sourceName,
                                  getParamPossibleChoices(param),
                                  null, /* units */
                                  getFieldType(param),
                                  null, /* regexp */
                                  width,
                                  null, /* abbrv */
                                  new AccessMode(getAccessType(param),
                                                 false)); /* only adv. mode */
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
