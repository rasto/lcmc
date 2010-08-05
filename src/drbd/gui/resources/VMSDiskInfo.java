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
import drbd.data.resources.Resource;
import drbd.data.ConfigData;
import drbd.data.LinuxFile;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;
import drbd.utilities.Unit;
import drbd.utilities.MyButton;
import drbd.utilities.SSH;
import drbd.utilities.VIRSH;
import drbd.utilities.MyMenuItem;
import drbd.utilities.MyList;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import java.util.Set;
import java.util.TreeSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * This class holds info about Virtual Disks.
 */
public class VMSDiskInfo extends EditableInfo {
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** VMS virtual domain info object. */
    private final VMSVirtualDomainInfo vmsVirtualDomainInfo;
    /** Source file combo box, so that it can be disabled, depending on type. */
    private GuiComboBox sourceFileCB = null;
    /** Source block combo box, so that it can be disabled, depending on type.*/
    private GuiComboBox sourceDeviceCB = null;
    /** Target device combo box, that needs to be reloaded if target type has
     * changed. */
    private GuiComboBox targetDeviceCB = null;
    /** Back to overview icon. */
    private static final ImageIcon BACK_ICON = Tools.createImageIcon(
                                                 Tools.getDefault("BackIcon"));
    /** Parameters. */
    private static final String[] PARAMETERS = {DiskData.TYPE,
                                                DiskData.TARGET_DEVICE,
                                                DiskData.SOURCE_FILE,
                                                DiskData.SOURCE_DEVICE,
                                                DiskData.TARGET_BUS_TYPE,
                                                DiskData.READONLY};
    /** Section map. */
    private static final Map<String, String> SECTION_MAP =
                                                 new HashMap<String, String>();
    /** Default units. */
    private static final Map<String, String> DEFAULT_UNIT =
                                                new HashMap<String, String>();
    /** If it has units. */
    private static final Map<String, Boolean> HAS_UNIT =
                                                new HashMap<String, Boolean>();
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
        SHORTNAME_MAP.put(DiskData.READONLY, "Readonly");
    }

    /** Default name. */
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
        TARGET_DEVICES_MAP.put("ide/disk",
                               new String[]{"hda", "hdb", "hdd"});
        TARGET_DEVICES_MAP.put("ide/cdrom",
                               new String[]{"hdc"});
        TARGET_DEVICES_MAP.put("fdc/floppy",
                               new String[]{"fda"});
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
        super(name, browser);
        setResource(new Resource(name));
        this.vmsVirtualDomainInfo = vmsVirtualDomainInfo;
    }

    /** Returns browser object of this info. */
    protected final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** Returns info panel. */
    public final JComponent getInfoPanel() {
        if (infoPanel != null) {
            return infoPanel;
        }
        final boolean abExisted = applyButton != null;
        /* main, button and options panels */
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        final JTable headerTable = getTable(vmsVirtualDomainInfo.HEADER_TABLE);
        if (headerTable != null) {
            mainPanel.add(headerTable.getTableHeader());
            mainPanel.add(headerTable);
        }
        mainPanel.add(getTablePanel("Disk", vmsVirtualDomainInfo.DISK_TABLE));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel(
                                        new FlowLayout(FlowLayout.LEFT, 0, 20));
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);

        final String[] params = getParametersFromXML();
        initApplyButton(null);
        /* add item listeners to the apply button. */
        if (!abExisted) {
            applyButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                getBrowser().clStatusLock();
                                apply(false);
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );
        }
        final JPanel extraButtonPanel =
                           new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        extraButtonPanel.setBackground(Browser.STATUS_BACKGROUND);
        buttonPanel.add(extraButtonPanel);
        addApplyButton(buttonPanel);
        final MyButton overviewButton = new MyButton("VM Host Overview",
                                                     BACK_ICON);
        overviewButton.setPreferredSize(new Dimension(200, 50));
        overviewButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                vmsVirtualDomainInfo.selectMyself();
            }
        });
        extraButtonPanel.add(overviewButton);
        addParams(optionsPanel,
                  params,
                  ClusterBrowser.SERVICE_LABEL_WIDTH,
                  ClusterBrowser.SERVICE_FIELD_WIDTH * 2,
                  null);
        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        JMenu serviceCombo;
        serviceCombo = getActionsMenu();
        mb.add(serviceCombo);
        buttonPanel.add(mb, BorderLayout.EAST);

        mainPanel.add(optionsPanel);
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(
                                  ClusterBrowser.SERVICE_LABEL_WIDTH
                                  + ClusterBrowser.SERVICE_FIELD_WIDTH + 4));
        newPanel.add(new JScrollPane(mainPanel));
        applyButton.setEnabled(checkResourceFields(null, params));
        infoPanel = newPanel;
        return infoPanel;
    }

    /** Returns service icon in the menu. */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        return BlockDevInfo.HARDDISK_ICON;
    }

    /** Returns long description of the specified parameter. */
    protected final String getParamLongDesc(final String param) {
        return SHORTNAME_MAP.get(param);
    }

    /** Returns short description of the specified parameter. */
    protected final String getParamShortDesc(final String param) {
        return SHORTNAME_MAP.get(param);
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
        final String sm = SECTION_MAP.get(param);
        if (sm == null) {
            return "Disk Options";
        }
        return sm;
    }

    /** Returns true if the specified parameter is required. */
    protected final boolean isRequired(final String param) {
        return false;
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
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                vmsxml.modifyDiskXML(vmsVirtualDomainInfo.getDomainName(),
                                     getName(),
                                     parameters);
            }
        }
        for (final Host h : getBrowser().getClusterHosts()) {
            getBrowser().periodicalVMSUpdate(h);
        }
    }

    /** Returns whether this parameter has a unit prefix. */
    protected final boolean hasUnitPrefix(final String param) {
        return HAS_UNIT.containsKey(param) && HAS_UNIT.get(param);
    }

    /** Returns units. */
    protected final Unit[] getUnits() {
        return new Unit[]{
                   //new Unit("", "", "KiByte", "KiBytes"), /* default unit */
                   new Unit("K", "K", "KiByte", "KiBytes"),
                   new Unit("M", "M", "MiByte", "MiBytes"),
                   new Unit("G",  "G",  "GiByte",      "GiBytes"),
                   new Unit("T",  "T",  "TiByte",      "TiBytes")
       };
    }

    /** Returns the default unit for the parameter. */
    protected final String getDefaultUnit(final String param) {
        return DEFAULT_UNIT.get(param);
    }

    /** Returns columns for the table. */
    protected final String[] getColumnNames(final String tableName) {
        return vmsVirtualDomainInfo.getColumnNames(tableName);
    }

    /** Returns data for the table. */
    protected final Object[][] getTableData(final String tableName) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return vmsVirtualDomainInfo.getMainTableData();
        } else if (VMSVirtualDomainInfo.DISK_TABLE.equals(tableName)) {
            return new Object[][]{vmsVirtualDomainInfo.getDiskDataRow(
                                            getName(),
                                            null,
                                            vmsVirtualDomainInfo.getDisks(),
                                            true)};
        //} else if (INTERFACES_TABLE.equals(tableName)) {
        }
        return new Object[][]{};
    }

    /** Execute when row in the table was clicked. */
    protected final void rowClicked(final String tableName, final String key) {
        vmsVirtualDomainInfo.selectMyself();
    }

    /** Retrurns color for some rows. */
    protected final Color getTableRowColor(final String tableName,
                                           final String key) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return vmsVirtualDomainInfo.getTableRowColor(tableName, key);
        }
        return Browser.PANEL_BACKGROUND;
    }

    /** Alignment for the specified column. */
    protected final int getTableColumnAlignment(final String tableName,
                                                final int column) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return vmsVirtualDomainInfo.getTableColumnAlignment(tableName,
                                                                column);
        }
        return SwingConstants.LEFT;
    }

    /** Returns info object for this row. */
    protected final Info getTableInfo(final String tableName,
                                      final String key) {
        if (VMSVirtualDomainInfo.HEADER_TABLE.equals(tableName)) {
            return vmsVirtualDomainInfo;
        }
        return null;
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
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (sourceFileCB != null) {
                        sourceFileCB.setVisible("file".equals(newValue));
                    } 
                    if (sourceDeviceCB != null) {
                        sourceDeviceCB.setVisible("block".equals(newValue));
                    }
                }
            });
        } else if (DiskData.TARGET_BUS_TYPE.equals(param)) {
            if (targetDeviceCB != null) {
                final Set<String> devices = new LinkedHashSet<String>();
                devices.add(null);
                for (final String dev : TARGET_DEVICES_MAP.get(newValue)) {
                    // TODO: remove the ones that are in use
                    devices.add(dev);
                }
                final String saved = getParamSaved(DiskData.TARGET_DEVICE);
                String selected = null;
                if (!devices.add(saved)) {
                    /* it was there */
                }
                selected = saved;
                targetDeviceCB.reloadComboBox(
                                selected,
                                devices.toArray(new String[devices.size()]));
            }
        }
        if (isRequired(param) && (newValue == null || "".equals(newValue))) {
            return false;
        }
        return true;
    }

    /** Updates parameters. */
    public final void updateParameters() {
        final Map<String, DiskData> disks = vmsVirtualDomainInfo.getDisks();
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

    /** Get first host that has this vm and is connected. */
    private Host getFirstConnectedHost() {
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null && h.isConnected()) {
                return h;
            }
        }
        return null;
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
        if (ret == JFileChooser.APPROVE_OPTION) {
            if (fc.getSelectedFile() != null) {
                final String name = fc.getSelectedFile().getAbsolutePath();
                paramCB.setValue(name);
            }
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
            final GuiComboBox paramCB = new GuiComboBox(sourceFile,
                                      null,
                                      null, /* units */
                                      GuiComboBox.Type.TEXTFIELDWITHBUTTON,
                                      regexp,
                                      width,
                                      null, /* abbrv */
                                      getAccessType(param),
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
            } else if (DiskData.SOURCE_DEVICE.equals(param)) {
                sourceDeviceCB = paramCB;
            } else if (DiskData.TARGET_DEVICE.equals(param)) {
                targetDeviceCB = paramCB;
            }
            return paramCB;
        }
    }

    /** Returns list of menu items. */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final boolean testOnly = false;
        /* remove service */
        final MyMenuItem removeMenuItem = new MyMenuItem(
                    Tools.getString("VMSDiskInfo.Menu.Remove"),
                    ClusterBrowser.REMOVE_ICON,
                    ClusterBrowser.STARTING_PTEST_TOOLTIP,
                    ConfigData.AccessType.ADMIN,
                    ConfigData.AccessType.OP) {
            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                if (getResource().isNew()) {
                    return true;
                }
                return true;
            }

            public void action() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getPopup().setVisible(false);
                    }
                });
                removeMyself(false);
            }
        };
        addMouseOverListener(removeMenuItem, null);
        items.add((UpdatableItem) removeMenuItem);
        return items;
    }

    /** Removes this disk from the libvirt with confirmation dialog. */
    public void removeMyself(final boolean testOnly) {
        if (getResource().isNew()) {
            removeMyselfNoConfirm(testOnly);
            getResource().setNew(false);
            return;
        }
        String desc = Tools.getString("VMSDiskInfo.confirmRemove.Description");

        desc  = desc.replaceAll("@DISK@", toString());
        if (Tools.confirmDialog(
               Tools.getString("VMSDiskInfo.confirmRemove.Title"),
               desc,
               Tools.getString("VMSDiskInfo.confirmRemove.Yes"),
               Tools.getString("VMSDiskInfo.confirmRemove.No"))) {
            removeMyselfNoConfirm(testOnly);
            getResource().setNew(false);
        }
    }

    /** Removes this disk without confirmation dialog. */
    protected void removeMyselfNoConfirm(final boolean testOnly) {
        if (testOnly) {
            return;
        }
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                vmsxml.removeDiskXML(vmsVirtualDomainInfo.getDomainName(),
                                     getName());
            }
        }
        for (final Host h : getBrowser().getClusterHosts()) {
            getBrowser().periodicalVMSUpdate(h);
        }
    }

    /** Returns string representation. */
    public String toString() {
        final StringBuffer s = new StringBuffer(30);
        s.append(getName());
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
}
