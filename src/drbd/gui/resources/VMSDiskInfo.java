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
import drbd.gui.ClusterBrowser;
import drbd.gui.GuiComboBox;
import drbd.data.VMSXML;
import drbd.data.VMSXML.DiskData;
import drbd.data.Host;
import drbd.data.ConfigData;
import drbd.data.LinuxFile;
import drbd.data.AccessMode;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import drbd.utilities.SSH;

import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * This class holds info about Virtual Disks.
 */
public class VMSDiskInfo extends VMSHardwareInfo {
    /** Source file combo box, so that it can be disabled, depending on type. */
    private GuiComboBox sourceFileCB = null;
    /** Source block combo box, so that it can be disabled, depending on type.*/
    private GuiComboBox sourceDeviceCB = null;
    /** Target device combo box, that needs to be reloaded if target type has
     * changed. */
    private GuiComboBox targetDeviceCB = null;
    /** Driver name combo box. */
    private GuiComboBox driverNameCB = null;
    /** Driver type combo box. */
    private GuiComboBox driverTypeCB = null;
    /** Readonly combo box. */
    private GuiComboBox readonlyCB = null;
    /** Previous value of target bus and type. So that we know if it changed. */
    private String prevTargetBusType = null;
    /** Previous value of the type (file or block). */
    private String prevType = null;
    /** Parameters. */
    private static final String[] PARAMETERS = {DiskData.TYPE,
                                                DiskData.TARGET_DEVICE,
                                                DiskData.SOURCE_FILE,
                                                DiskData.SOURCE_DEVICE,
                                                DiskData.TARGET_BUS_TYPE,
                                                DiskData.DRIVER_NAME,
                                                DiskData.DRIVER_TYPE,
                                                DiskData.READONLY,
                                                DiskData.SHAREABLE};
    /** Whether the parameter is editable only in advanced mode. */
    private static final Set<String> IS_ENABLED_ONLY_IN_ADVANCED =
        new HashSet<String>(Arrays.asList(new String[]{
                                                DiskData.TARGET_DEVICE,
                                                DiskData.DRIVER_NAME,
                                                DiskData.DRIVER_TYPE}));
    /** Whether the parameter is required. */
    private static final Set<String> IS_REQUIRED =
        new HashSet<String>(Arrays.asList(new String[]{DiskData.TYPE}));
    /** Field type. */
    private static final Map<String, GuiComboBox.Type> FIELD_TYPES =
                                       new HashMap<String, GuiComboBox.Type>();
    /** Target devices depending on the target type. */
    private static final Map<String, String[]> TARGET_DEVICES_MAP =
                                           new HashMap<String, String[]>();
    static {
        FIELD_TYPES.put(DiskData.TYPE,
                        GuiComboBox.Type.RADIOGROUP);
        FIELD_TYPES.put(DiskData.SOURCE_FILE,
                        GuiComboBox.Type.TEXTFIELDWITHBUTTON);
        FIELD_TYPES.put(DiskData.READONLY,
                        GuiComboBox.Type.CHECKBOX);
        FIELD_TYPES.put(DiskData.SHAREABLE,
                        GuiComboBox.Type.CHECKBOX);
        FIELD_TYPES.put(DiskData.TARGET_DEVICE,
                        GuiComboBox.Type.COMBOBOX);
    }
    /** Short name. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();
    static {
        SHORTNAME_MAP.put(DiskData.TYPE, "Type");
        SHORTNAME_MAP.put(DiskData.TARGET_DEVICE, "Target Device");
        SHORTNAME_MAP.put(DiskData.SOURCE_FILE, "Source File");
        SHORTNAME_MAP.put(DiskData.SOURCE_DEVICE, "Source Device");
        SHORTNAME_MAP.put(DiskData.TARGET_BUS_TYPE, "Target Type");
        SHORTNAME_MAP.put(DiskData.DRIVER_NAME, "Driver Name");
        SHORTNAME_MAP.put(DiskData.DRIVER_TYPE, "Driver Type");
        SHORTNAME_MAP.put(DiskData.READONLY, "Readonly");
        SHORTNAME_MAP.put(DiskData.SHAREABLE, "Shareable");
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
    /** Default location for libvirt images. */
    private static final String LIBVIRT_IMAGE_LOCATION =
                                             "/var/lib/libvirt/images/";
    /** Pattern that parses stat output. */
    private static final Pattern STAT_PATTERN = Pattern.compile(
                                                       "(.).{9}\\s+(\\d+)\\s+"
                                                       + "(\\d+)\\s+"
                                                       + "(\\d+) (.*)$");
    /** A map from target bus and type as it is saved to the string
     * representation that appears in the menus. */
    private static final Map<String, String> TARGET_BUS_TYPES =
                                                 new HashMap<String, String>();
    static {
        POSSIBLE_VALUES.put(DiskData.TYPE, new String[]{"file", "block"});
        POSSIBLE_VALUES.put(
                    DiskData.TARGET_BUS_TYPE,
                    new StringInfo[]{
                       new StringInfo("IDE disk",    "ide/disk",    null),
                       new StringInfo("IDE cdrom",   "ide/cdrom",    null),
                       new StringInfo("Floppy disk", "fdc/floppy", null),
                       new StringInfo("SCSI disk",   "scsi/disk",   null),
                       new StringInfo("USB disk",    "usb/disk",    null),
                       new StringInfo("Virtio Disk", "virtio/disk", null)});
        for (final StringInfo tbt : (StringInfo[]) POSSIBLE_VALUES.get(
                                                  DiskData.TARGET_BUS_TYPE)) {
            TARGET_BUS_TYPES.put(tbt.getStringValue(), tbt.toString());
        }
        DEFAULTS_MAP.put(DiskData.READONLY, "False");
        DEFAULTS_MAP.put(DiskData.SHAREABLE, "False");
        TARGET_DEVICES_MAP.put("ide/disk",
                               new String[]{"hda", "hdb", "hdd"});
        TARGET_DEVICES_MAP.put("ide/cdrom",
                               new String[]{"hdc"});
        TARGET_DEVICES_MAP.put("fdc/floppy",
                               new String[]{"fda", "fdb", "fdc", "fdd"});
        TARGET_DEVICES_MAP.put("scsi/disk",
                               new String[]{"sda", "sdb", "sdc", "sdd"});
        TARGET_DEVICES_MAP.put("usb/disk",
                               new String[]{"sda", "sdb", "sdc", "sdd"});
        TARGET_DEVICES_MAP.put("virtio/disk",
                               new String[]{"vda", "vdb", "vdc", "vdd", "vde"});
    }
    /** Cache for files. */
    private final Map<String, LinuxFile> linuxFileCache =
                                            new HashMap<String, LinuxFile>();

    /** Creates the VMSDiskInfo object. */
    public VMSDiskInfo(final String name, final Browser browser,
                       final VMSVirtualDomainInfo vmsVirtualDomainInfo) {
        super(name, browser, vmsVirtualDomainInfo);
    }

    /** Adds disk table with only this disk to the main panel. */
    protected final void addHardwareTable(final JPanel mainPanel) {
       mainPanel.add(getTablePanel("Disk", VMSVirtualDomainInfo.DISK_TABLE));
    }

    /** Returns service icon in the menu. */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        return BlockDevInfo.HARDDISK_ICON;
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
        if (DiskData.SOURCE_DEVICE.equals(param)) {
            for (final Host h : getBrowser().getClusterHosts()) {
                final VMSXML vmsxml = getBrowser().getVMSXML(h);
                final Set<String> bds = new TreeSet<String>();
                bds.add("");
                if (vmsxml != null) {
                    for (final BlockDevInfo bdi
                            : h.getBrowser().getBlockDevInfos()) {
                        if (bdi.getBlockDevice().isDrbd()) {
                            bds.add(bdi.getDrbdResourceInfo().getDevice());
                        } else {
                            bds.add(bdi.getName());
                        }
                    }
                    return bds.toArray(new String[bds.size()]);
                }
            }
        }
        return POSSIBLE_VALUES.get(param);
    }

    /** Returns section to which the specified parameter belongs. */
    protected final String getSection(final String param) {
        return "Disk Options";
    }

    /** Returns true if the specified parameter is required. */
    protected final boolean isRequired(final String param) {
        final String type = getComboBoxValue(DiskData.TYPE);
        if ((DiskData.SOURCE_FILE.equals(param) && "file".equals(type))
            || (DiskData.SOURCE_DEVICE.equals(param) && "block".equals(type))) {
            if ("ide/cdrom".equals(prevTargetBusType)
                || "fdc/floppy".equals(prevTargetBusType)) {
                return false;
            } else {
                return true;
            }
        }
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
        String type = null;
        for (final String param : getParametersFromXML()) {
            final String value = getComboBoxValue(param);
            if (DiskData.TYPE.equals(param)) {
                type = value;
            } else if (DiskData.TARGET_BUS_TYPE.equals(param)) {
                final String[] values = value.split("/");
                if (values.length == 2) {
                    parameters.put(DiskData.TARGET_BUS, values[0]);
                    parameters.put(DiskData.TARGET_TYPE, values[1]);
                } else {
                    Tools.appWarning("cannot parse: " + param + " = " + value);
                }
                getResource().setValue(param, value);
                continue;
            }
            if ("file".equals(type)
                && DiskData.SOURCE_DEVICE.equals(param)) {
                getResource().setValue(param, null);
                continue;
            } else if ("block".equals(type)
                && DiskData.SOURCE_FILE.equals(param)) {
                getResource().setValue(param, null);
                continue;
            }
            if (!Tools.areEqual(getParamSaved(param), value)) {
                parameters.put(param, value);
                getResource().setValue(param, value);
            }
        }
        if (!testOnly) {
            for (final Host h : getBrowser().getClusterHosts()) {
                final VMSXML vmsxml = getBrowser().getVMSXML(h);
                if (vmsxml != null) {
                    parameters.put(DiskData.SAVED_TARGET_DEVICE, getName());
                    vmsxml.modifyDiskXML(
                                    getVMSVirtualDomainInfo().getDomainName(),
                                    parameters);
                }
            }
            getResource().setNew(false);
            setName(getParamSaved(DiskData.TARGET_DEVICE));
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
    protected final boolean isAdvanced(final String param) {
        return false;
    }

    /** Returns access type of this parameter. */
    protected final ConfigData.AccessType getAccessType(final String param) {
        return ConfigData.AccessType.ADMIN;
    }

    /** Returns true if the value of the parameter is ok. */
    protected final boolean checkParam(final String param,
                                       final String newValue) {
        if (DiskData.TYPE.equals(param)) {
            if (!newValue.equals(prevType)) {
                if (sourceFileCB != null) {
                    sourceFileCB.setVisible("file".equals(newValue));
                }
                if (sourceDeviceCB != null) {
                    sourceDeviceCB.setVisible("block".equals(newValue));
                }
                //if (driverTypeCB != null) {
                //    if (prevType != null
                //        || getParamSaved(DiskData.DRIVER_TYPE) == null) {
                //        if ("file".equals(newValue)) {
                //            driverTypeCB.setValue("raw");
                //        } else {
                //            driverTypeCB.setValue("");
                //        }
                //    }
                //}
                prevType = newValue;
                checkResourceFields(DiskData.SOURCE_FILE,
                                    getParametersFromXML());
                checkResourceFields(DiskData.SOURCE_DEVICE,
                                    getParametersFromXML());
            }
        } else if (DiskData.TARGET_BUS_TYPE.equals(param)) {
            if (targetDeviceCB != null && !newValue.equals(prevTargetBusType)) {
                final Set<String> devices = new LinkedHashSet<String>();
                devices.add(null);
                for (final String dev : TARGET_DEVICES_MAP.get(newValue)) {
                    if (!getVMSVirtualDomainInfo().isDevice(dev)) {
                        devices.add(dev);
                    }
                }
                final String saved = getParamSaved(DiskData.TARGET_DEVICE);
                String selected = null;
                devices.add(saved);
                if (prevTargetBusType == null && saved != null) {
                    selected = saved;
                } else if (devices.size() > 1) {
                    selected = devices.toArray(new String[devices.size()])[1];
                }
                targetDeviceCB.reloadComboBox(
                                selected,
                                devices.toArray(new String[devices.size()]));
                if (prevTargetBusType != null
                    || getParamSaved(DiskData.DRIVER_NAME) == null) {
                    driverNameCB.setValue("qemu");
                    if ("ide/cdrom".equals(newValue)) {
                        readonlyCB.setValue("True");
                    } else {
                        readonlyCB.setValue("False");
                    }
                }
                prevTargetBusType = newValue;
                checkResourceFields(DiskData.SOURCE_FILE,
                                    getParametersFromXML());
                checkResourceFields(DiskData.SOURCE_DEVICE,
                                    getParametersFromXML());
            }
        }
        if (isRequired(param) && (newValue == null || "".equals(newValue))) {
            return false;
        }
        return true;
    }

    /** Whether the parameter should be enabled. */
    protected final boolean isEnabled(final String param) {
        return true;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    protected final boolean isEnabledOnlyInAdvancedMode(final String param) {
         return IS_ENABLED_ONLY_IN_ADVANCED.contains(param);
    }

    /** Updates parameters. */
    public final void updateParameters() {
        final Map<String, DiskData> disks =
                                        getVMSVirtualDomainInfo().getDisks();
        if (disks != null) {
            final DiskData diskData = disks.get(getName());
            if (diskData != null) {
                for (final String param : getParametersFromXML()) {
                    final String oldValue = getParamSaved(param);
                    String value = getParamSaved(param);
                    final GuiComboBox cb = paramComboBoxGet(param, null);
                    for (final Host h : getBrowser().getClusterHosts()) {
                        final VMSXML vmsxml = getBrowser().getVMSXML(h);
                        if (vmsxml != null) {
                            final String savedValue =
                                               diskData.getValue(param);
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
        updateTable(VMSVirtualDomainInfo.DISK_TABLE);
    }

    /** Returns cached file object. */
    public final LinuxFile getLinuxDir(final String dir, final Host host) {
        LinuxFile ret = linuxFileCache.get(dir);
        if (ret == null) {
            ret = new LinuxFile(this, host, dir, "d", 0, 0);
            linuxFileCache.put(dir, ret);
        }
        return ret;
    }

    /** Returns file system view that allows remote browsing. */
    private FileSystemView getFileSystemView(final Host host) {
        final VMSDiskInfo thisClass = this;
        return new FileSystemView() {
            public final File[] getRoots() {
                return new LinuxFile[]{getLinuxDir("/", host)};
            }

            public final boolean isRoot(final File f) {
                final String path = Tools.getUnixPath(f.toString());
                if ("/".equals(path)) {
                    return true;
                }
                return false;
            }

            public final File createNewFolder(final File containingDir) {
                return null;
            }

            public final File getHomeDirectory() {
                return getLinuxDir(LIBVIRT_IMAGE_LOCATION, host);
            }

            public final Boolean isTraversable(final File f) {
                final LinuxFile lf = linuxFileCache.get(f.toString());
                if (lf != null) {
                    return lf.isDirectory();
                }
                return true;
            }

            public final File getParentDirectory(final File dir) {
                return getLinuxDir(dir.getParent(), host);
            }

            public final File[] getFiles(final File dir,
                                         final boolean useFileHiding) {
                final StringBuffer dirSB = new StringBuffer(dir.toString());
                if ("/".equals(dir.toString())) {
                    dirSB.append('*');
                } else {
                    dirSB.append("/*");
                }
                final SSH.SSHOutput out =
                        Tools.execCommandProgressIndicator(
                                      host,
                                      "stat -c \"%A %a %Y %s %n\" "
                                      + dirSB.toString()
                                      + " 2>/dev/null",
                                      null,
                                      false,
                                      "executing...",
                                      SSH.DEFAULT_COMMAND_TIMEOUT);
                final List<LinuxFile> files = new ArrayList<LinuxFile>();
                if (out.getExitCode() == 0) {
                    for (final String line : out.getOutput().split("\r\n")) {
                        if ("".equals(line.trim())) {
                            continue;
                        }
                        final Matcher m = STAT_PATTERN.matcher(line);
                        if (m.matches()) {
                            final String type = m.group(1);
                            final long lastModified =
                                           Long.parseLong(m.group(3)) * 1000;
                            final long size = Long.parseLong(m.group(4));
                            final String filename = m.group(5);
                            LinuxFile lf = linuxFileCache.get(filename);
                            if (lf == null) {
                                lf = new LinuxFile(thisClass,
                                                   host,
                                                   filename,
                                                   type,
                                                   lastModified,
                                                   size);
                                linuxFileCache.put(filename, lf);
                            } else {
                                lf.update(type, lastModified, size);
                            }
                            files.add(lf);
                        } else {
                            Tools.appWarning("could not match: " + line);
                        }
                    }
                }
                return files.toArray(new LinuxFile[files.size()]);
            }
        };
    }

    /** Starts file chooser. */
    private void startFileChooser(final GuiComboBox paramCB) {
        final Host host = getFirstConnectedHost();
        if (host == null) {
            Tools.appError("Connection to host lost.");
            return;
        }
        final VMSDiskInfo thisClass = this;
        final JFileChooser fc = new JFileChooser(
                                    getLinuxDir(LIBVIRT_IMAGE_LOCATION, host),
                                    getFileSystemView(host)) {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;
                public final void setCurrentDirectory(final File dir) {
                    super.setCurrentDirectory(new LinuxFile(
                                                    thisClass,
                                                    host,
                                                    dir.toString(),
                                                    "d",
                                                    0,
                                                    0));
                }

            };
        fc.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        fc.setDialogType(JFileChooser.CUSTOM_DIALOG);
        fc.setDialogTitle(Tools.getString("VMSDiskInfo.FileChooserTitle")
                          + host.getName());
//        fc.setApproveButtonText(Tools.getString("VMSDiskInfo.Approve"));
        fc.setApproveButtonToolTipText(
                               Tools.getString("VMSDiskInfo.Approve.ToolTip"));
        fc.putClientProperty("FileChooser.useShellFolder", Boolean.FALSE);
        final int ret = fc.showDialog(
                                       Tools.getGUIData().getMainFrame(),
                                       Tools.getString("VMSDiskInfo.Approve"));
        linuxFileCache.clear();
        if (ret == JFileChooser.APPROVE_OPTION
            && fc.getSelectedFile() != null) {
            final String name = fc.getSelectedFile().getAbsolutePath();
            paramCB.setValue(name);
        }
    }

    /** Returns combo box for parameter. */
    protected final GuiComboBox getParamComboBox(final String param,
                                                 final String prefix,
                                                 final int width) {
        if (DiskData.SOURCE_FILE.equals(param)) {
            final String sourceFile = getParamSaved(DiskData.SOURCE_FILE);
            final String regexp = "[^/]$";
            final MyButton fileChooserBtn = new MyButton("Browse...");
            final GuiComboBox paramCB = new GuiComboBox(
                                  sourceFile,
                                  null,
                                  null, /* units */
                                  GuiComboBox.Type.TEXTFIELDWITHBUTTON,
                                  regexp,
                                  width,
                                  null, /* abbrv */
                                  new AccessMode(getAccessType(param),
                                                 false), /* only adv. mode */
                                  fileChooserBtn);
            sourceFileCB = paramCB;
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
                            startFileChooser(paramCB);
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
            if (DiskData.TYPE.equals(param)
                || DiskData.TARGET_BUS_TYPE.equals(param)) {
                paramCB.setAlwaysEditable(false);
            } else if (sourceDeviceCB == null
                       && DiskData.SOURCE_DEVICE.equals(param)) {
                sourceDeviceCB = paramCB;
            } else if (targetDeviceCB == null
                       && DiskData.TARGET_DEVICE.equals(param)) {
                targetDeviceCB = paramCB;
            } else if (driverNameCB == null
                       && DiskData.DRIVER_NAME.equals(param)) {
                driverNameCB = paramCB;
            } else if (driverTypeCB == null
                       && DiskData.DRIVER_TYPE.equals(param)) {
                driverTypeCB = paramCB;
            } else if (readonlyCB == null
                       && DiskData.READONLY.equals(param)) {
                readonlyCB = paramCB;
            }
            return paramCB;
        }
    }

    /** Removes this disk without confirmation dialog. */
    protected final void removeMyselfNoConfirm(final boolean testOnly) {
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                final Map<String, String> parameters =
                                                new HashMap<String, String>();
                parameters.put(DiskData.SAVED_TARGET_DEVICE, getName());
                vmsxml.removeDiskXML(getVMSVirtualDomainInfo().getDomainName(),
                                     parameters);
            }
        }
        for (final Host h : getBrowser().getClusterHosts()) {
            getBrowser().periodicalVMSUpdate(h);
        }
    }

    /** Returns string representation. */
    public final String toString() {
        final StringBuffer s = new StringBuffer(30);
        final String name = getName();
        if (name == null) {
            return "new disk...";
        }
        s.append(name);
        s.append(" (");
        final String saved = getParamSaved(DiskData.TARGET_BUS_TYPE);
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
    protected final String isRemoveable() {
        return null;
    }
}
