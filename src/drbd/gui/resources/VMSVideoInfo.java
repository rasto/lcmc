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
import drbd.data.VMSXML.VideoData;
import drbd.data.Host;
import drbd.data.ConfigData;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.w3c.dom.Node;

/**
 * This class holds info about virtual video device.
 */
public class VMSVideoInfo extends VMSHardwareInfo {
    /** Parameters. */
    private static final String[] PARAMETERS = {VideoData.MODEL_TYPE,
                                                VideoData.MODEL_VRAM,
                                                VideoData.MODEL_HEADS};

    /** Whether the parameter is editable only in advanced mode. */
    private static final Set<String> IS_ENABLED_ONLY_IN_ADVANCED =
        new HashSet<String>(Arrays.asList(new String[]{
                                                VideoData.MODEL_VRAM,
                                                VideoData.MODEL_HEADS}));

    /** Field type. */
    private static final Map<String, GuiComboBox.Type> FIELD_TYPES =
                                       new HashMap<String, GuiComboBox.Type>();
    /** Short name. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();
    static {
        SHORTNAME_MAP.put(VideoData.MODEL_TYPE,
                          Tools.getString("VMSVideoInfo.ModelType"));
        SHORTNAME_MAP.put(VideoData.MODEL_VRAM,
                          Tools.getString("VMSVideoInfo.ModelVRAM"));
        SHORTNAME_MAP.put(VideoData.MODEL_HEADS,
                          Tools.getString("VMSVideoInfo.ModelHeads"));
    }

    /** Long name. */
    private static final Map<String, String> LONGNAME_MAP =
                                                 new HashMap<String, String>();
    static {
        LONGNAME_MAP.put(VideoData.MODEL_VRAM,
                         Tools.getString("VMSVideoInfo.ModelVRAM.ToolTip"));
        LONGNAME_MAP.put(VideoData.MODEL_HEADS,
                         Tools.getString("VMSVideoInfo.ModelHeads.ToolTip"));
    }

    /** Whether the parameter is required. */
    private static final Set<String> IS_REQUIRED =
        new HashSet<String>(Arrays.asList(new String[]{VideoData.MODEL_TYPE}));

    /** Default name. */
    private static final Map<String, String> DEFAULTS_MAP =
                                                 new HashMap<String, String>();
    /** Possible values. */
    private static final Map<String, Object[]> POSSIBLE_VALUES =
                                               new HashMap<String, Object[]>();
    static {
        POSSIBLE_VALUES.put(VideoData.MODEL_TYPE,
                            new String[]{"cirrus", "vga", "vmvga", "xen"});
    }
    /** Table panel. */
    private JComponent tablePanel = null;
    /** Creates the VMSVideoInfo object. */
    public VMSVideoInfo(final String name, final Browser browser,
                           final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(name, browser, vmsVirtualDomainInfo);
    }

    /** Adds disk table with only this disk to the main panel. */
    protected final void addHardwareTable(final JPanel mainPanel) {
        tablePanel = getTablePanel("Video Devices",
                                   VMSVirtualDomainInfo.VIDEO_TABLE,
                                   getNewBtn(getVMSVirtualDomainInfo()));
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
        final String name = LONGNAME_MAP.get(param);
        if (name == null) {
            return getParamShortDesc(param);
        }
        return name;
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
        return "Video Device Options";
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
                parameters.put(VideoData.SAVED_MODEL_TYPE,
                               getParamSaved(VideoData.MODEL_TYPE));
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
            public void run() {
                tablePanel.setVisible(true);
            }
        });
        checkResourceFieldsChanged(null, getParametersFromXML());
    }

    /** Returns device parameters. */
    protected Map<String, String> getHWParametersAndSave() {
        final Map<String, String> params = super.getHWParametersAndSave();
        setName(getParamSaved(VideoData.MODEL_TYPE));
        return params;
    }

    /** Returns data for the table. */
    protected final Object[][] getTableData(final String tableName) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return getVMSVirtualDomainInfo().getMainTableData();
        } else if (VMSVirtualDomainInfo.VIDEO_TABLE.equals(tableName)) {
            if (getResource().isNew()) {
                return new Object[][]{};
            }
            return new Object[][]{getVMSVirtualDomainInfo().getVideoDataRow(
                                getName(),
                                null,
                                getVMSVirtualDomainInfo().getVideos(),
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
        final Map<String, VideoData> videos =
                              getVMSVirtualDomainInfo().getVideos();
        if (videos != null) {
            final VideoData videoData = videos.get(getName());
            if (videoData != null) {
                for (final String param : getParametersFromXML()) {
                    final String oldValue = getParamSaved(param);
                    String value = getParamSaved(param);
                    final GuiComboBox cb = paramComboBoxGet(param, null);
                    for (final Host h
                            : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                        final VMSXML vmsxml = getBrowser().getVMSXML(h);
                        if (vmsxml != null) {
                            final String savedValue =
                                                  videoData.getValue(param);
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
        updateTable(VMSVirtualDomainInfo.VIDEO_TABLE);
        checkResourceFieldsChanged(null, getParametersFromXML());
    }

    /** Returns string representation. */
    public final String toString() {
        final StringBuffer s = new StringBuffer(30);
        final String type = getParamSaved(VideoData.MODEL_TYPE);
        if (type == null) {
            s.append("new video device...");
        } else {
            s.append(type);
        }
        return s.toString();
    }

    /** Removes this video device without confirmation dialog. */
    protected final void removeMyselfNoConfirm(final boolean testOnly) {
        if (testOnly) {
            return;
        }
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                final Map<String, String> parameters =
                                                new HashMap<String, String>();
                parameters.put(VideoData.SAVED_MODEL_TYPE,
                               getParamSaved(VideoData.MODEL_TYPE));
                vmsxml.removeVideoXML(getVMSVirtualDomainInfo().getDomainName(),
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
    protected final String isRemoveable() {
        return null;
    }


    /** Returns "add new" button. */
    public static MyButton getNewBtn(final VMSVirtualDomainInfo vdi) {
        final MyButton newBtn = new MyButton("Add Video Device");
        newBtn.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    public void run() {
                        vdi.addVideosPanel();
                    }
                });
                t.start();
            }
        });
        return newBtn;
    }

    /** Modify device xml. */
    protected final void modifyXML(final VMSXML vmsxml,
                                   final Node node,
                                   final String domainName,
                                   final Map<String, String> params) {
        if (vmsxml != null) {
            vmsxml.modifyVideoXML(node, domainName, params);
        }
    }
}
