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

import lcmc.gui.Browser;
import lcmc.gui.resources.drbd.BlockDevInfo;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.data.VMSXML;
import lcmc.data.VMSXML.DiskData;
import lcmc.data.Host;
import lcmc.data.Application;
import lcmc.data.AccessMode;
import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.ImageIcon;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lcmc.data.StringValue;
import lcmc.data.Value;

import org.w3c.dom.Node;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class holds info about Virtual Disks.
 */
public final class VMSDiskInfo extends VMSHardwareInfo {
    /** Logger. */
    private static final Logger LOG =
                                  LoggerFactory.getLogger(VMSDiskInfo.class);
    /** Source file combo box, so that it can be disabled, depending on type. */
    private final Map<String, Widget> sourceFileWi =
                                            new HashMap<String, Widget>();
    /** Source block combo box, so that it can be disabled, depending on type.*/
    private final Map<String, Widget> sourceDeviceWi =
                                            new HashMap<String, Widget>();

    private final Map<String, Widget> sourceNameWi =
                                                 new HashMap<String, Widget>();
    private final Map<String, Widget> sourceProtocolWi =
                                                 new HashMap<String, Widget>();
    private final Map<String, Widget> sourceHostNameWi =
                                                 new HashMap<String, Widget>();
    private final Map<String, Widget> sourceHostPortWi =
                                                 new HashMap<String, Widget>();
    private final Map<String, Widget> authUsernameWi =
                                                 new HashMap<String, Widget>();
    private final Map<String, Widget> authSecretTypeWi =
                                                 new HashMap<String, Widget>();
    private final Map<String, Widget> authSecretUuidWi =
                                                 new HashMap<String, Widget>();

    /** Target device combo box, that needs to be reloaded if target type has
     * changed. */
    private final Map<String, Widget> targetDeviceWi =
                                            new HashMap<String, Widget>();
    /** Driver name combo box. */
    private final Map<String, Widget> driverNameWi =
                                            new HashMap<String, Widget>();
    /** Driver type combo box. */
    private final Map<String, Widget> driverTypeWi =
                                            new HashMap<String, Widget>();
    /** Driver cache combo box. */
    private final Map<String, Widget> driverCacheWi =
                                            new HashMap<String, Widget>();
    /** Readonly combo box. */
    private final Map<String, Widget> readonlyWi =
                                            new HashMap<String, Widget>();
    /** Previous target device value. */
    private Value prevTargetBusType = null;
    /** Previous target bus type (disk type) value. */
    private Value previousTargetBusType = null;
    /** Parameters. */
    private static final String[] PARAMETERS = {DiskData.TYPE,
                                                DiskData.TARGET_BUS_TYPE,
                                                DiskData.TARGET_DEVICE,
                                                DiskData.SOURCE_FILE,
                                                DiskData.SOURCE_DEVICE,

                                                DiskData.SOURCE_PROTOCOL,
                                                DiskData.SOURCE_NAME,
                                                DiskData.SOURCE_HOST_NAME,
                                                DiskData.SOURCE_HOST_PORT,

                                                DiskData.AUTH_USERNAME,
                                                DiskData.AUTH_SECRET_TYPE,
                                                DiskData.AUTH_SECRET_UUID,

                                                DiskData.DRIVER_NAME,
                                                DiskData.DRIVER_TYPE,
                                                DiskData.DRIVER_CACHE,
                                                DiskData.READONLY,
                                                DiskData.SHAREABLE};
    /** Block parameters. */
    private static final String[] BLOCK_PARAMETERS = {DiskData.TYPE,
                                                      DiskData.TARGET_BUS_TYPE,
                                                      DiskData.TARGET_DEVICE,
                                                      DiskData.SOURCE_DEVICE,
                                                      DiskData.DRIVER_NAME,
                                                      DiskData.DRIVER_TYPE,
                                                      DiskData.DRIVER_CACHE,
                                                      DiskData.READONLY,
                                                      DiskData.SHAREABLE};
    /** File parameters. */
    private static final String[] FILE_PARAMETERS = {DiskData.TYPE,
                                                     DiskData.TARGET_DEVICE,
                                                     DiskData.SOURCE_FILE,
                                                     DiskData.TARGET_BUS_TYPE,
                                                     DiskData.DRIVER_NAME,
                                                     DiskData.DRIVER_TYPE,
                                                     DiskData.DRIVER_CACHE,
                                                     DiskData.READONLY,
                                                     DiskData.SHAREABLE};

    /** Network parameters. */
    private static final String[] NETWORK_PARAMETERS = {
                                                    DiskData.TYPE,
                                                    DiskData.TARGET_BUS_TYPE,
                                                    DiskData.TARGET_DEVICE,

                                                    DiskData.SOURCE_PROTOCOL,
                                                    DiskData.SOURCE_NAME,
                                                    DiskData.SOURCE_HOST_NAME,
                                                    DiskData.SOURCE_HOST_PORT,

                                                    DiskData.AUTH_USERNAME,
                                                    DiskData.AUTH_SECRET_TYPE,
                                                    DiskData.AUTH_SECRET_UUID,

                                                    DiskData.DRIVER_NAME,
                                                    DiskData.DRIVER_TYPE,
                                                    DiskData.DRIVER_CACHE,
                                                    DiskData.READONLY,
                                                    DiskData.SHAREABLE};
    /** Whether the parameter is enabled only in advanced mode. */
    private static final Collection<String> IS_ENABLED_ONLY_IN_ADVANCED =
        new HashSet<String>(Arrays.asList(new String[]{
                                                DiskData.TARGET_DEVICE,
                                                DiskData.DRIVER_NAME,
                                                DiskData.DRIVER_TYPE,
                                                DiskData.DRIVER_CACHE}));
    /** Whether the parameter is required. */
    private static final Collection<String> IS_REQUIRED =
        new HashSet<String>(Arrays.asList(new String[]{DiskData.TYPE}));
    static {
        IS_REQUIRED.add(DiskData.SOURCE_PROTOCOL);
        IS_REQUIRED.add(DiskData.SOURCE_NAME);
        IS_REQUIRED.add(DiskData.SOURCE_HOST_NAME);
        IS_REQUIRED.add(DiskData.SOURCE_HOST_PORT);
        IS_REQUIRED.add(DiskData.AUTH_USERNAME);
    }
    /** Field type. */
    private static final Map<String, Widget.Type> FIELD_TYPES =
                                       new HashMap<String, Widget.Type>();
    /** Target devices depending on the target type. */
    private static final Map<Value, Value[]> TARGET_DEVICES_MAP =
                                                new HashMap<Value, Value[]>();
    static {
        FIELD_TYPES.put(DiskData.TYPE, Widget.Type.RADIOGROUP);
        FIELD_TYPES.put(DiskData.SOURCE_FILE, Widget.Type.COMBOBOX);
        FIELD_TYPES.put(DiskData.READONLY, Widget.Type.CHECKBOX);
        FIELD_TYPES.put(DiskData.SHAREABLE, Widget.Type.CHECKBOX);
        FIELD_TYPES.put(DiskData.TARGET_DEVICE, Widget.Type.COMBOBOX);
    }
    /** Short name. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();
    static {
        SHORTNAME_MAP.put(DiskData.TYPE,
                          Tools.getString("VMSDiskInfo.Param.Type"));
        SHORTNAME_MAP.put(DiskData.TARGET_DEVICE,
                          Tools.getString("VMSDiskInfo.Param.TargetDevice"));
        SHORTNAME_MAP.put(DiskData.SOURCE_FILE,
                          Tools.getString("VMSDiskInfo.Param.SourceFile"));
        SHORTNAME_MAP.put(DiskData.SOURCE_DEVICE,
                          Tools.getString("VMSDiskInfo.Param.SourceDevice"));

        SHORTNAME_MAP.put(DiskData.SOURCE_PROTOCOL,
                          Tools.getString("VMSDiskInfo.Param.SourceProtocol"));
        SHORTNAME_MAP.put(DiskData.SOURCE_NAME,
                          Tools.getString("VMSDiskInfo.Param.SourceName"));
        SHORTNAME_MAP.put(DiskData.SOURCE_HOST_NAME,
                          Tools.getString("VMSDiskInfo.Param.SourceHostName"));
        SHORTNAME_MAP.put(DiskData.SOURCE_HOST_PORT,
                          Tools.getString("VMSDiskInfo.Param.SourceHostPort"));

        SHORTNAME_MAP.put(DiskData.AUTH_USERNAME,
                          Tools.getString("VMSDiskInfo.Param.AuthUsername"));
        SHORTNAME_MAP.put(DiskData.AUTH_SECRET_TYPE,
                          Tools.getString("VMSDiskInfo.Param.AuthSecretType"));
        SHORTNAME_MAP.put(DiskData.AUTH_SECRET_UUID,
                          Tools.getString("VMSDiskInfo.Param.AuthSecretUuid"));

        SHORTNAME_MAP.put(DiskData.TARGET_BUS_TYPE,
                          Tools.getString("VMSDiskInfo.Param.TargetBusType"));
        SHORTNAME_MAP.put(DiskData.DRIVER_NAME,
                          Tools.getString("VMSDiskInfo.Param.DriverName"));
        SHORTNAME_MAP.put(DiskData.DRIVER_TYPE,
                          Tools.getString("VMSDiskInfo.Param.DriverType"));
        SHORTNAME_MAP.put(DiskData.DRIVER_CACHE,
                          Tools.getString("VMSDiskInfo.Param.DriverCache"));
        SHORTNAME_MAP.put(DiskData.READONLY,
                          Tools.getString("VMSDiskInfo.Param.Readonly"));
        SHORTNAME_MAP.put(DiskData.SHAREABLE,
                          Tools.getString("VMSDiskInfo.Param.Shareable"));
    }
    /** Tool tips. */
    private static final Map<String, String> TOOLTIP_MAP =
                                                 new HashMap<String, String>();
    static {
        TOOLTIP_MAP.put(DiskData.SOURCE_HOST_NAME,
                  Tools.getString("VMSDiskInfo.Param.SourceHostName.ToolTip"));
        TOOLTIP_MAP.put(DiskData.SOURCE_HOST_PORT,
                  Tools.getString("VMSDiskInfo.Param.SourceHostPort.ToolTip"));
    }

    /** Sections. */
    private static final Map<String, String> SECTION_MAP =
                                                 new HashMap<String, String>();
    private static final String SECTION_DISK_OPTIONS =
                         Tools.getString("VMSDiskInfo.Section.DiskOptions");
    private static final String SECTION_SOURCE =
                         Tools.getString("VMSDiskInfo.Section.Source");
    private static final String SECTION_AUTHENTICATION =
                         Tools.getString("VMSDiskInfo.Section.Authentication");

    static {
        SECTION_MAP.put(DiskData.TYPE, SECTION_SOURCE);
        SECTION_MAP.put(DiskData.SOURCE_FILE, SECTION_SOURCE);
        SECTION_MAP.put(DiskData.SOURCE_DEVICE, SECTION_SOURCE);
        SECTION_MAP.put(DiskData.SOURCE_PROTOCOL, SECTION_SOURCE);
        SECTION_MAP.put(DiskData.SOURCE_NAME, SECTION_SOURCE);
        SECTION_MAP.put(DiskData.SOURCE_HOST_NAME, SECTION_SOURCE);
        SECTION_MAP.put(DiskData.SOURCE_HOST_PORT, SECTION_SOURCE);

        SECTION_MAP.put(DiskData.AUTH_USERNAME, SECTION_AUTHENTICATION);
        SECTION_MAP.put(DiskData.AUTH_SECRET_TYPE, SECTION_AUTHENTICATION);
        SECTION_MAP.put(DiskData.AUTH_SECRET_UUID, SECTION_AUTHENTICATION);
    }

    /** Preferred values. */
    private static final Map<String, Value> PREFERRED_MAP =
                                                 new HashMap<String, Value>();
    /** Defaults. */
    private static final Map<String, Value> DEFAULTS_MAP =
                                                 new HashMap<String, Value>();
    /** Possible values. */
    private static final Map<String, Value[]> POSSIBLE_VALUES =
                                              new HashMap<String, Value[]>();
    /** Default location for libvirt images. */
    public static final String LIBVIRT_IMAGE_LOCATION =
                                             "/var/lib/libvirt/images/";
    /** A map from target bus and type as it is saved to the string
     * representation that appears in the menus. */
    private static final Map<String, String> TARGET_BUS_TYPES =
                                                 new HashMap<String, String>();
    /** Disk types. */
    public static final Value FILE_TYPE =
                                 new StringValue("file", "Image file");
    public static final Value BLOCK_TYPE =
                                 new StringValue("block", "Disk/block device");
    public static final Value NETWORK_TYPE =
                                 new StringValue("network", "Network");

    /** Drivers. */
    private static final Value DRIVER_NAME_DEFUALT = new StringValue();
    private static final Value DRIVER_NAME_FILE = new StringValue("file");
    private static final Value DRIVER_NAME_QEMU = new StringValue("qemu");
    private static final Value DRIVER_NAME_PHY = new StringValue("phy");

    /** Bus types. */

    public static final Value BUS_TYPE_IDE =
                                new StringValue("ide/disk", "IDE Disk");
    public static final Value BUS_TYPE_CDROM =
                                new StringValue("ide/cdrom", "IDE CDROM");
    public static final Value BUS_TYPE_FLOPPY =
                                new StringValue("fdc/floppy", "Floppy Disk");
    public static final Value BUS_TYPE_SCSI =
                                new StringValue("scsi/disk", "SCSI Disk");
    public static final Value BUS_TYPE_USB =
                                new StringValue("usb/disk", "USB Disk");
    public static final Value BUS_TYPE_VIRTIO =
                                new StringValue("virtio/disk", "Virtio Disk");

    static {
        POSSIBLE_VALUES.put(DiskData.TYPE,
                            new Value[]{FILE_TYPE, BLOCK_TYPE, NETWORK_TYPE});
        POSSIBLE_VALUES.put(DiskData.TARGET_BUS_TYPE,
                            new Value[]{BUS_TYPE_IDE,
                                        BUS_TYPE_CDROM,
                                        BUS_TYPE_FLOPPY,
                                        BUS_TYPE_SCSI,
                                        BUS_TYPE_USB,
                                        BUS_TYPE_VIRTIO});
        POSSIBLE_VALUES.put(DiskData.DRIVER_NAME, new Value[]{
                                                          DRIVER_NAME_DEFUALT,
                                                          DRIVER_NAME_FILE,
                                                          DRIVER_NAME_QEMU,
                                                          DRIVER_NAME_PHY});
        POSSIBLE_VALUES.put(DiskData.DRIVER_TYPE, new Value[]{new StringValue(),
                                                             new StringValue("raw")});
        POSSIBLE_VALUES.put(DiskData.DRIVER_CACHE, new Value[]{new StringValue(),
                                                               new StringValue("default"),
                                                               new StringValue("none"),
                                                               new StringValue("writethrough"),
                                                               new StringValue("writeback"),
                                                               new StringValue("directsync"),
                                                               new StringValue("unsafe")});

        POSSIBLE_VALUES.put(DiskData.SOURCE_PROTOCOL, new Value[]{new StringValue(),
                                                                  new StringValue("rbd"),
                                                                  new StringValue("nbd"),
                                                                  new StringValue("iscsi"),
                                                                  new StringValue("sheepdog"),
                                                                  new StringValue("gluster")});

        POSSIBLE_VALUES.put(DiskData.SOURCE_NAME,
                            new Value[]{new StringValue(),
                                        new StringValue("poolname/imagename"),
                                        new StringValue("poolname/imagename:rbd_cache=1")});
        POSSIBLE_VALUES.put(DiskData.AUTH_SECRET_TYPE,
                            new Value[]{new StringValue(),
                                         new StringValue("ceph"),
                                         new StringValue("iscsi")});
        for (final Value tbt : POSSIBLE_VALUES.get(DiskData.TARGET_BUS_TYPE)) {
            TARGET_BUS_TYPES.put(tbt.getValueForConfig(), tbt.getValueForGui());
        }
        DEFAULTS_MAP.put(DiskData.READONLY, new StringValue("False"));
        DEFAULTS_MAP.put(DiskData.SHAREABLE, new StringValue("False"));
        PREFERRED_MAP.put(DiskData.DRIVER_NAME, new StringValue("file"));
        TARGET_DEVICES_MAP.put(BUS_TYPE_IDE,
                               new Value[]{new StringValue("hda"),
                                           new StringValue("hdb"),
                                           new StringValue("hdd")});
        TARGET_DEVICES_MAP.put(BUS_TYPE_CDROM,
                               new Value[]{new StringValue("hdc")});
        TARGET_DEVICES_MAP.put(BUS_TYPE_FLOPPY,
                               new Value[]{new StringValue("fda"),
                                           new StringValue("fdb"),
                                           new StringValue("fdc"),
                                           new StringValue("fdd")});
        TARGET_DEVICES_MAP.put(BUS_TYPE_SCSI,
                               new Value[]{new StringValue("sda"),
                                           new StringValue("sdb"),
                                           new StringValue("sdc"),
                                           new StringValue("sdd")});
        TARGET_DEVICES_MAP.put(BUS_TYPE_USB,
                               new Value[]{new StringValue("sda"),
                                           new StringValue("sdb"),
                                           new StringValue("sdc"),
                                           new StringValue("sdd")});
        TARGET_DEVICES_MAP.put(BUS_TYPE_VIRTIO,
                               new Value[]{new StringValue("vda"),
                                           new StringValue("vdb"),
                                           new StringValue("vdc"),
                                           new StringValue("vdd"),
                                           new StringValue("vde")});
    }
    /** Default source port if none is specified (and it is needed). */
    public static final String DEFAULT_SOURCE_HOST_PORT = "6789";
    private final Collection<Map<String, Widget>> checkFieldList =
                                          new ArrayList<Map<String, Widget>>();

    /** Table panel. */
    private JComponent tablePanel = null;

    /** Creates the VMSDiskInfo object. */
    VMSDiskInfo(final String name, final Browser browser,
                       final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(name, browser, vmsVirtualDomainInfo);

        checkFieldList.add(sourceNameWi);
        checkFieldList.add(sourceProtocolWi);
        checkFieldList.add(sourceHostNameWi);
        checkFieldList.add(sourceHostPortWi);
        checkFieldList.add(authUsernameWi);
        checkFieldList.add(authSecretTypeWi);
        checkFieldList.add(authSecretUuidWi);
    }

    /** Adds disk table with only this disk to the main panel. */
    @Override
    protected void addHardwareTable(final JPanel mainPanel) {
        tablePanel = getTablePanel("Disk",
                                   VMSVirtualDomainInfo.DISK_TABLE,
                                   getNewBtn(getVMSVirtualDomainInfo()));
        if (getResource().isNew()) {
            Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
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
        final String domainType =
                        getVMSVirtualDomainInfo().getWidget(
                            VMSXML.VM_PARAM_DOMAIN_TYPE, null).getStringValue();
        if (DiskData.DRIVER_NAME.equals(param)
            && VMSVirtualDomainInfo.DOMAIN_TYPE_KVM.equals(domainType)) {
            return DRIVER_NAME_QEMU;
        }
        return PREFERRED_MAP.get(param);
    }

    /** Returns default value for specified parameter. */
    @Override
    protected Value getParamDefault(final String param) {
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
        if (BLOCK_TYPE.equals(getComboBoxValue(DiskData.TYPE))) {
            return BLOCK_PARAMETERS.clone();
        } else if (FILE_TYPE.equals(getComboBoxValue(DiskData.TYPE))) {
            return FILE_PARAMETERS.clone();
        } else {
            return NETWORK_PARAMETERS.clone();
        }
    }

    /** Returns possible choices for drop down lists. */
    @Override
    protected Value[] getParamPossibleChoices(final String param) {
        if (DiskData.SOURCE_FILE.equals(param)) {
            final Set<Value> sourceFileDirs = new TreeSet<Value>();
            sourceFileDirs.add(new StringValue(LIBVIRT_IMAGE_LOCATION));
            for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                final VMSXML vmsxml = getBrowser().getVMSXML(h);
                if (vmsxml != null) {
                    for (final String sfd: vmsxml.getSourceFileDirs()) {
                        sourceFileDirs.add(new StringValue(sfd));
                    }
                }
            }
            return sourceFileDirs.toArray(new Value[sourceFileDirs.size()]);
        } else if (DiskData.SOURCE_DEVICE.equals(param)) {
            for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                final VMSXML vmsxml = getBrowser().getVMSXML(h);
                final List<Value> bds = new ArrayList<Value>();
                bds.add(null);
                if (vmsxml != null) {
                    for (final BlockDevInfo bdi
                            : h.getBrowser().getBlockDevInfos()) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            bds.add(new StringValue(bdi.getDrbdVolumeInfo().getDeviceByRes()));
                        } else {
                            bds.add(new StringValue(bdi.getName()));
                        }
                    }
                    return bds.toArray(new Value[bds.size()]);
                }
            }
        }
        return POSSIBLE_VALUES.get(param);
    }

    /** Returns section to which the specified parameter belongs. */
    @Override
    protected String getSection(final String param) {
        final String section = SECTION_MAP.get(param);
        if (section == null) {
            return SECTION_DISK_OPTIONS;
        } else {
            return section;
        }
    }

    /** Returns true if the specified parameter is required. */
    @Override
    protected boolean isRequired(final String param) {
        final Value type = getComboBoxValue(DiskData.TYPE);
        if ((DiskData.SOURCE_FILE.equals(param) && FILE_TYPE.equals(type))
            || (DiskData.SOURCE_DEVICE.equals(param)
                && BLOCK_TYPE.equals(type))) {
            return !BUS_TYPE_CDROM.equals(
                                    getComboBoxValue(DiskData.TARGET_BUS_TYPE))
                    && !BUS_TYPE_FLOPPY.equals(getComboBoxValue(
                                                     DiskData.TARGET_BUS_TYPE));
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
            @Override
            public void run() {
                getInfoPanel();
            }
        });
        final String[] params = getRealParametersFromXML();
        final Map<String, String> parameters =
                                           new LinkedHashMap<String, String>();
        for (final String param : params) {
            final Value value = getComboBoxValue(param);
            if (DiskData.TYPE.equals(param)) {
                parameters.put(param, value.getValueForConfig());
            } else if (DiskData.TARGET_BUS_TYPE.equals(param)) {
                if (value == null) {
                    parameters.put(DiskData.TARGET_BUS, null);
                    parameters.put(DiskData.TARGET_TYPE, null);
                } else {
                    final String[] values = value.getValueForConfig().split("/");
                    if (values.length == 2) {
                        parameters.put(DiskData.TARGET_BUS, values[0]);
                        parameters.put(DiskData.TARGET_TYPE, values[1]);
                    } else {
                        LOG.appWarning("getHWParameters: cannot parse: " + param + " = " + value);
                    }
                }
            } else if (allParams) {
                if (Tools.areEqual(getParamDefault(param), value)) {
                    parameters.put(param, null);
                } else {
                    parameters.put(param, value.getValueForConfig());
                }
            } else if (!Tools.areEqual(getParamSaved(param), value)
                       || DiskData.SOURCE_FILE.equals(param)
                       || DiskData.SOURCE_DEVICE.equals(param)
                       || DiskData.SOURCE_PROTOCOL.equals(param)
                       || DiskData.SOURCE_NAME.equals(param)
                       || DiskData.SOURCE_HOST_NAME.equals(param)
                       || DiskData.SOURCE_HOST_PORT.equals(param)
                       || DiskData.AUTH_USERNAME.equals(param)
                       || DiskData.AUTH_SECRET_TYPE.equals(param)
                       || DiskData.AUTH_SECRET_UUID.equals(param)) {
                if (Tools.areEqual(getParamDefault(param), value)) {
                    parameters.put(param, null);
                } else {
                    parameters.put(param, value.getValueForConfig());
                }
            }
        }
        parameters.put(DiskData.SAVED_TARGET_DEVICE, getName());
        setName(getParamSavedForConfig(DiskData.TARGET_DEVICE));
        return parameters;
    }

    /**
     * Fix ports. Make the number of ports delimited with "," to match the host
     * names.
     */
    private static void fixSourceHostParams(final Map<String, String> params) {
        final String names = params.get(DiskData.SOURCE_HOST_NAME);
        String ports = params.get(DiskData.SOURCE_HOST_PORT);
        if (names == null) {
            params.put(DiskData.SOURCE_HOST_PORT, null);
            return;
        } else if (names.isEmpty()) {
            params.put(DiskData.SOURCE_HOST_PORT, "");
            return;
        }

        if (ports == null || ports.isEmpty()) {
            ports = DEFAULT_SOURCE_HOST_PORT;
        }

        final String[] namesA = names.trim().split("\\s*,\\s*");
        final String[] portsA = ports.trim().split("\\s*,\\s*");

        final String lastPort = portsA[portsA.length - 1];

        final String fixedNames = Tools.join(", ", namesA);
        String fixedPorts;
        if (namesA.length == portsA.length) {
            fixedPorts = Tools.join(", ", portsA);
        } else if (portsA.length < namesA.length) {
            /* add ports */
            fixedPorts = Tools.join(", ", portsA);
            for (int i = 0; i < namesA.length - portsA.length; i++) {
                fixedPorts += ", " + lastPort;
            }
        } else {
            /* remove ports */
            fixedPorts = Tools.join(", ", portsA, namesA.length);
        }
        params.put(DiskData.SOURCE_HOST_NAME, fixedNames);
        params.put(DiskData.SOURCE_HOST_PORT, fixedPorts);
    }


    /** Applies the changes. */
    @Override
    void apply(final Application.RunMode runMode) {
        if (Application.isTest(runMode)) {
            return;
        }
        Tools.invokeAndWait(new Runnable() {
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
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                final String domainName =
                                getVMSVirtualDomainInfo().getDomainName();
                final Node domainNode = vmsxml.getDomainNode(domainName);
                /* fix host ports */

                fixSourceHostParams(parameters);
                final String fixedNames =
                                    parameters.get(DiskData.SOURCE_HOST_NAME);
                final String fixedPorts =
                                    parameters.get(DiskData.SOURCE_HOST_PORT);

                for (final Map.Entry<String, Widget> entry : sourceHostNameWi.entrySet()) {
                    entry.getValue().setValueAndWait(new StringValue(fixedNames));
                }
                for (final Map.Entry<String, Widget> entry : sourceHostPortWi.entrySet()) {
                    entry.getValue().setValueAndWait(new StringValue(fixedPorts));
                }
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
        Tools.invokeLater(new Runnable() {
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
    protected void modifyXML(final VMSXML vmsxml,
                             final Node node,
                             final String domainName,
                             final Map<String, String> params) {
        if (vmsxml != null) {
            vmsxml.modifyDiskXML(node, domainName, params);
        }
    }

    /** Returns data for the table. */
    @Override
    protected Object[][] getTableData(final String tableName) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return getVMSVirtualDomainInfo().getMainTableData();
        } else if (VMSVirtualDomainInfo.DISK_TABLE.equals(tableName)) {
            if (getResource().isNew()) {
                return new Object[][]{};
            }
            return new Object[][]{getVMSVirtualDomainInfo().getDiskDataRow(
                                        getName(),
                                        null,
                                        getVMSVirtualDomainInfo().getDisks(),
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
    protected Application.AccessType getAccessType(final String param) {
        return Application.AccessType.ADMIN;
    }
    /** Returns true if the value of the parameter is ok. */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        if (DiskData.TYPE.equals(param)) {
            Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                @Override
                public void run() {
                    final boolean file = FILE_TYPE.equals(newValue);
                    final boolean block = BLOCK_TYPE.equals(newValue);
                    final boolean network = NETWORK_TYPE.equals(newValue);
                    for (final Map.Entry<String, Widget> entry : sourceFileWi.entrySet()) {
                        entry.getValue().setVisible(file);
                    }
                    for (final Map.Entry<String, Widget> entry : sourceDeviceWi.entrySet()) {
                        entry.getValue().setVisible(block);
                    }
                    for (final Map<String, Widget> w : checkFieldList) {
                        for (final Map.Entry<String, Widget> entry : w.entrySet()) {
                            entry.getValue().setVisible(network);
                        }
                    }
                }
            });
            checkOneParam(DiskData.SOURCE_FILE);
            checkOneParam(DiskData.SOURCE_DEVICE);
        } else if (DiskData.TARGET_BUS_TYPE.equals(param)) {
            final Set<Value> devices = new LinkedHashSet<Value>();
            devices.add(new StringValue());
            if (newValue != null) {
                final Value[] targetDevices = TARGET_DEVICES_MAP.get(newValue);
                if (targetDevices != null) {
                    for (final Value dev : targetDevices) {
                        if (!getVMSVirtualDomainInfo().isDevice(dev.getValueForConfig())) {
                            devices.add(dev);
                        }
                    }
                }
            }
            final Value saved = getParamSaved(DiskData.TARGET_DEVICE);
            devices.add(saved);
            Value selected = null;
            if (saved == null || getResource().isNew()) {
                if (devices.size() > 1) {
                    selected = devices.toArray(new Value[devices.size()])[1];
                }
            } else {
                selected = saved;
            }
            if (prevTargetBusType == null
                || !prevTargetBusType.equals(newValue)) {
                for (final Map.Entry<String, Widget> entry : targetDeviceWi.entrySet()) {
                    entry.getValue().reloadComboBox(
                            selected,
                            devices.toArray(new Value[devices.size()]));
                }
                prevTargetBusType = newValue;
            }
            final Value tbs = getComboBoxValue(DiskData.TARGET_BUS_TYPE);
            if (getParamSaved(DiskData.DRIVER_NAME) == null
                && !tbs.equals(previousTargetBusType)) {
                if (BUS_TYPE_CDROM.equals(newValue)) {
                    for (final Map.Entry<String, Widget> entry : readonlyWi.entrySet()) {
                        entry.getValue().setValue(new StringValue("True"));
                    }
                    for (final Map.Entry<String, Widget> entry : driverTypeWi.entrySet()) {
                        if (getResource().isNew()) {
                            entry.getValue().setValue(new StringValue("raw"));
                        } else {
                            if (entry.getValue().getValue() != null) {
                                entry.getValue().setValue(null);
                            }
                        }
                    }
                } else if (BUS_TYPE_VIRTIO.equals(newValue)) {
                    for (final Map.Entry<String, Widget> entry : driverTypeWi.entrySet()) {
                        entry.getValue().setValue(new StringValue("raw"));
                    }
                    for (final Map.Entry<String, Widget> entry : driverCacheWi.entrySet()) {
                        entry.getValue().setValue(new StringValue("none"));
                    }
                } else {
                    for (final Map.Entry<String, Widget> entry : readonlyWi.entrySet()) {
                        entry.getValue().setValue(new StringValue("False"));
                        driverTypeWi.get(entry.getKey()).setValue(new StringValue("raw"));
                    }
                }
            }
            previousTargetBusType = tbs;
            checkOneParam(DiskData.SOURCE_FILE);
            checkOneParam(DiskData.SOURCE_DEVICE);
        }
        return !isRequired(param)
               || (newValue != null && !newValue.isNothingSelected());
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
    @Override
    void updateParameters() {
        final Map<String, DiskData> disks =
                                        getVMSVirtualDomainInfo().getDisks();
        if (disks != null) {
            final DiskData diskData = disks.get(getName());
            if (diskData != null) {
                for (final String param : getParametersFromXML()) {
                    final Value oldValue = getParamSaved(param);
                    Value value = getParamSaved(param);
                    final Widget wi = getWidget(param, null);
                    for (final Host h
                            : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
                        final VMSXML vmsxml = getBrowser().getVMSXML(h);
                        if (vmsxml != null) {
                            final Value savedValue =
                                               diskData.getValue(param);
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
        updateTable(VMSVirtualDomainInfo.DISK_TABLE);
        setApplyButtons(null, getRealParametersFromXML());
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                if (tablePanel != null) {
                    tablePanel.setVisible(true);
                }
            }
        });
    }

    /** Returns combo box for parameter. */
    @Override
    protected Widget createWidget(final String param,
                                  final String prefix,
                                  final int width) {
        final String prefixS;
        if (prefix == null) {
            prefixS = "";
        } else {
            prefixS = prefix;
        }
        if (DiskData.SOURCE_FILE.equals(param)) {
            final Value sourceFile = getParamSaved(DiskData.SOURCE_FILE);
            final MyButton fileChooserBtn = new MyButton("Browse...");
            fileChooserBtn.miniButton();
            final String regexp = ".*[^/]$";
            final Widget paramWi = WidgetFactory.createInstance(
                                     getFieldType(param),
                                     sourceFile,
                                     getParamPossibleChoices(param),
                                     regexp,
                                     width,
                                     Widget.NO_ABBRV,
                                     new AccessMode(getAccessType(param),
                                                    false), /* only adv. mode */
                                     fileChooserBtn);
            paramWi.setAlwaysEditable(true);
            sourceFileWi.put(prefixS, paramWi);
            if (Tools.isWindows()) {
                /* does not work on windows and I tried, ultimately because
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
                            final String file;
                            final String oldFile = paramWi.getStringValue();
                            if (oldFile == null || oldFile.isEmpty()) {
                                file = LIBVIRT_IMAGE_LOCATION;
                            } else {
                                file = oldFile;
                            }
                            startFileChooser(paramWi,
                                             file,
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
            if (DiskData.TYPE.equals(param)
                || DiskData.TARGET_BUS_TYPE.equals(param)) {
                paramWi.setAlwaysEditable(false);
            } else if (DiskData.SOURCE_DEVICE.equals(param)) {
                paramWi.setAlwaysEditable(true);
                sourceDeviceWi.put(prefixS, paramWi);
            } else if (DiskData.SOURCE_NAME.equals(param)) {
                sourceNameWi.put(prefixS, paramWi);
            } else if (DiskData.SOURCE_PROTOCOL.equals(param)) {
                sourceProtocolWi.put(prefixS, paramWi);
            } else if (DiskData.SOURCE_HOST_NAME.equals(param)) {
                sourceHostNameWi.put(prefixS, paramWi);
            } else if (DiskData.SOURCE_HOST_PORT.equals(param)) {
                sourceHostPortWi.put(prefixS, paramWi);
            } else if (DiskData.AUTH_USERNAME.equals(param)) {
                authUsernameWi.put(prefixS, paramWi);
            } else if (DiskData.AUTH_SECRET_TYPE.equals(param)) {
                authSecretTypeWi.put(prefixS, paramWi);
            } else if (DiskData.AUTH_SECRET_UUID.equals(param)) {
                authSecretUuidWi.put(prefixS, paramWi);
            } else if (DiskData.TARGET_DEVICE.equals(param)) {
                paramWi.setAlwaysEditable(true);
                targetDeviceWi.put(prefixS, paramWi);
            } else if (DiskData.DRIVER_NAME.equals(param)) {
                driverNameWi.put(prefixS, paramWi);
            } else if (DiskData.DRIVER_TYPE.equals(param)) {
                driverTypeWi.put(prefixS, paramWi);
            } else if (DiskData.DRIVER_CACHE.equals(param)) {
                driverCacheWi.put(prefixS, paramWi);
            } else if (DiskData.READONLY.equals(param)) {
                readonlyWi.put(prefixS, paramWi);
            }
            return paramWi;
        }
    }

    /** Removes this disk without confirmation dialog. */
    @Override
    protected void removeMyselfNoConfirm(final Application.RunMode runMode) {
        final String virshOptions = getVMSVirtualDomainInfo().getVirshOptions();
        for (final Host h : getVMSVirtualDomainInfo().getDefinedOnHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                final Map<String, String> parameters =
                                                new HashMap<String, String>();
                parameters.put(DiskData.SAVED_TARGET_DEVICE, getName());
                vmsxml.removeDiskXML(getVMSVirtualDomainInfo().getDomainName(),
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
            return "new disk...";
        }
        s.append(name);
        s.append(" (");
        final String saved = getParamSaved(DiskData.TARGET_BUS_TYPE).getValueForConfig();
        if (saved == null) {
            s.append("new...");
        } else if (TARGET_BUS_TYPES.containsKey(saved)) {
            s.append(TARGET_BUS_TYPES.get(saved));
        } else {
            s.append(saved);
        }
        s.append(')');
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
        final MyButton newBtn = new MyButton("Add Disk");
        newBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        vdi.addDiskPanel();
                    }
                });
                t.start();
            }
        });
        return newBtn;
    }

    /** Returns the regexp of the parameter. */
    @Override
    protected String getParamRegexp(final String param) {
        if (FILE_TYPE.equals(getComboBoxValue(DiskData.TYPE))
            && DiskData.SOURCE_FILE.equals(param)) {
            return ".*[^/]$";
        }
        return super.getParamRegexp(param);
    }

    /** Additional tool tip. */
    @Override
    protected String additionalToolTip(final String param) {
        final String tt = TOOLTIP_MAP.get(param);
        if (tt == null) {
            return "";
        }
        return tt;
    }
}
