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
import drbd.gui.HostBrowser;
import drbd.gui.ClusterBrowser;
import drbd.gui.GuiComboBox;
import drbd.gui.SpringUtilities;
import drbd.data.VMSXML;
import drbd.data.VMSXML.DiskData;
import drbd.data.VMSXML.InterfaceData;
import drbd.data.VMSXML.InputDevData;
import drbd.data.VMSXML.GraphicsData;
import drbd.data.VMSXML.SoundData;
import drbd.data.VMSXML.SerialData;
import drbd.data.VMSXML.ParallelData;
import drbd.data.VMSXML.VideoData;
import drbd.data.Host;
import drbd.data.resources.Resource;
import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;
import drbd.utilities.MyMenu;
import drbd.utilities.MyMenuItem;
import drbd.utilities.VIRSH;
import drbd.utilities.Unit;
import drbd.utilities.MyButton;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JScrollPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.SpringLayout;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Arrays;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.geom.Point2D;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import org.w3c.dom.Node;

/**
 * This class holds info about VirtualDomain service in the VMs category,
 * but not in the cluster view.
 */
public final class VMSVirtualDomainInfo extends EditableInfo {
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Whether the vm is running on at least one host. */
    private boolean running = false;
    /** HTML string on which hosts the vm is defined. */
    private String definedOnString = "";
    /** HTML string on which hosts the vm is running. */
    private String runningOnString = "";
    /** Row color, that is color of host on which is it running or null. */
    private Color rowColor = Browser.PANEL_BACKGROUND;
    /** Transition between states lock. */
    private final Mutex mTransitionLock = new Mutex();
    /** Starting. */
    private final Set<String> starting = new HashSet<String>();
    /** Shutting down. */
    private final Set<String> shuttingdown = new HashSet<String>();
    /** Suspending. */
    private final Set<String> suspending = new HashSet<String>();
    /** Resuming. */
    private final Set<String> resuming = new HashSet<String>();
    /** Progress indicator when starting or stopping. */
    private final StringBuffer progress = new StringBuffer("-");
    /** Disk to info hash lock. */
    private final Mutex mDiskToInfoLock = new Mutex();
    /** Map from key to vms disk info object. */
    private final Map<String, VMSDiskInfo> diskToInfo =
                                           new HashMap<String, VMSDiskInfo>();
    /** Map from target string in the table to vms disk info object.
     */
    private volatile Map<String, VMSDiskInfo> diskKeyToInfo =
                                           new HashMap<String, VMSDiskInfo>();
    /** Interface to info hash lock. */
    private final Mutex mInterfaceToInfoLock = new Mutex();
    /** Map from key to vms interface info object. */
    private final Map<String, VMSInterfaceInfo> interfaceToInfo =
                                       new HashMap<String, VMSInterfaceInfo>();
    /** Map from target string in the table to vms interface info object. */
    private volatile Map<String, VMSInterfaceInfo> interfaceKeyToInfo =
                                       new HashMap<String, VMSInterfaceInfo>();
    /** Input device to info hash lock. */
    private final Mutex mInputDevToInfoLock = new Mutex();
    /** Map from key to vms input device info object. */
    private final Map<String, VMSInputDevInfo> inputDevToInfo =
                                       new HashMap<String, VMSInputDevInfo>();
    /** Map from target string in the table to vms interface info object. */
    private volatile Map<String, VMSInputDevInfo> inputDevKeyToInfo =
                                       new HashMap<String, VMSInputDevInfo>();

    /** Graphics device to info hash lock. */
    private final Mutex mGraphicsToInfoLock = new Mutex();
    /** Map from key to vms graphics device info object. */
    private final Map<String, VMSGraphicsInfo> graphicsToInfo =
                                       new HashMap<String, VMSGraphicsInfo>();
    /** Map from target string in the table to vms interface info object. */
    private volatile Map<String, VMSGraphicsInfo> graphicsKeyToInfo =
                                       new HashMap<String, VMSGraphicsInfo>();

    /** Sound device to info hash lock. */
    private final Mutex mSoundToInfoLock = new Mutex();
    /** Map from key to vms sound device info object. */
    private final Map<String, VMSSoundInfo> soundToInfo =
                                       new HashMap<String, VMSSoundInfo>();
    /** Map from target string in the table to vms interface info object. */
    private volatile Map<String, VMSSoundInfo> soundKeyToInfo =
                                       new HashMap<String, VMSSoundInfo>();

    /** Serial device to info hash lock. */
    private final Mutex mSerialToInfoLock = new Mutex();
    /** Map from key to vms serial device info object. */
    private final Map<String, VMSSerialInfo> serialToInfo =
                                       new HashMap<String, VMSSerialInfo>();
    /** Map from target string in the table to vms interface info object. */
    private volatile Map<String, VMSSerialInfo> serialKeyToInfo =
                                       new HashMap<String, VMSSerialInfo>();

    /** Parallel device to info hash lock. */
    private final Mutex mParallelToInfoLock = new Mutex();
    /** Map from key to vms parallel device info object. */
    private final Map<String, VMSParallelInfo> parallelToInfo =
                                       new HashMap<String, VMSParallelInfo>();
    /** Map from target string in the table to vms interface info object. */
    private volatile Map<String, VMSParallelInfo> parallelKeyToInfo =
                                       new HashMap<String, VMSParallelInfo>();

    /** Video device to info hash lock. */
    private final Mutex mVideoToInfoLock = new Mutex();
    /** Map from key to vms video device info object. */
    private final Map<String, VMSVideoInfo> videoToInfo =
                                       new HashMap<String, VMSVideoInfo>();
    /** Map to host buttons, to start and view virtual hosts. */
    private final Map<String, MyButton> hostButtons =
                                               new HashMap<String, MyButton>();
    /** Whether it is used by CRM. */
    private boolean usedByCRM = false;
    /** Map from target string in the table to vms interface info object. */
    private volatile Map<String, VMSVideoInfo> videoKeyToInfo =
                                       new HashMap<String, VMSVideoInfo>();
    /** Timeout of starting, shutting down, etc. actions in seconds. */
    private static final int ACTION_TIMEOUT = 20;
    /** All parameters. */
    private static final String[] VM_PARAMETERS = new String[]{
                                    VMSXML.VM_PARAM_NAME,
                                    VMSXML.VM_PARAM_TYPE,
                                    VMSXML.VM_PARAM_EMULATOR,
                                    VMSXML.VM_PARAM_VCPU,
                                    VMSXML.VM_PARAM_CURRENTMEMORY,
                                    VMSXML.VM_PARAM_MEMORY,
                                    VMSXML.VM_PARAM_BOOT,
                                    VMSXML.VM_PARAM_LOADER,
                                    VMSXML.VM_PARAM_AUTOSTART,
                                    VMSXML.VM_PARAM_ARCH,
                                    VMSXML.VM_PARAM_ACPI,
                                    VMSXML.VM_PARAM_APIC,
                                    VMSXML.VM_PARAM_PAE,
                                    VMSXML.VM_PARAM_HAP,
                                    VMSXML.VM_PARAM_CPU_MATCH,
                                    VMSXML.VM_PARAM_CPUMATCH_MODEL,
                                    VMSXML.VM_PARAM_CPUMATCH_VENDOR,
                                    VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS,
                                    VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_CORES,
                                    VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS,
                                    VMSXML.VM_PARAM_CPUMATCH_FEATURE_POLICY,
                                    VMSXML.VM_PARAM_CPUMATCH_FEATURES,
                                    VMSXML.VM_PARAM_ON_POWEROFF,
                                    VMSXML.VM_PARAM_ON_REBOOT,
                                    VMSXML.VM_PARAM_ON_CRASH};
    /** Advanced parameters. */
    private static final Set<String> IS_ADVANCED =
        new HashSet<String>(Arrays.asList(new String[]{
                                    VMSXML.VM_PARAM_ACPI,
                                    VMSXML.VM_PARAM_APIC,
                                    VMSXML.VM_PARAM_PAE,
                                    VMSXML.VM_PARAM_HAP,
                                    VMSXML.VM_PARAM_CPU_MATCH,
                                    VMSXML.VM_PARAM_CPUMATCH_MODEL,
                                    VMSXML.VM_PARAM_CPUMATCH_VENDOR,
                                    VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS,
                                    VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_CORES,
                                    VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS,
                                    VMSXML.VM_PARAM_CPUMATCH_FEATURE_POLICY,
                                    VMSXML.VM_PARAM_CPUMATCH_FEATURES,
                                    VMSXML.VM_PARAM_ON_POWEROFF,
                                    VMSXML.VM_PARAM_ON_REBOOT,
                                    VMSXML.VM_PARAM_ON_CRASH}));
    /** Map of sections to which every param belongs. */
    private static final Map<String, String> SECTION_MAP =
                                                 new HashMap<String, String>();
    /** Map of short param names with uppercased first character. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();
    /** Map of default values. */
    private static final Map<String, String> DEFAULTS_MAP =
                                                 new HashMap<String, String>();
    /** Preferred values. */
    private static final Map<String, String> PREFERRED_MAP =
                                                 new HashMap<String, String>();
    /** Types of some of the field. */
    private static final Map<String, GuiComboBox.Type> FIELD_TYPES =
                                       new HashMap<String, GuiComboBox.Type>();
    /** Possible values for some fields. */
    private static final Map<String, Object[]> POSSIBLE_VALUES =
                                          new HashMap<String, Object[]>();
    /** Whether parameter is an integer. */
    private static final List<String> IS_INTEGER = new ArrayList<String>();
    /** Required version for a parameter. */
    private static final Map<String, String> REQUIRED_VERSION =
                                                new HashMap<String, String>();
    /** Returns whether this parameter has a unit prefix. */
    private static final Map<String, Boolean> HAS_UNIT_PREFIX =
                                               new HashMap<String, Boolean>();
    /** Returns default unit. */
    private static final Map<String, String> DEFAULT_UNIT =
                                               new HashMap<String, String>();
    /** Back to overview icon. */
    private static final ImageIcon BACK_ICON = Tools.createImageIcon(
                                            Tools.getDefault("BackIcon"));
    /** VNC Viewer icon. */
    static final ImageIcon VNC_ICON = Tools.createImageIcon(
                                    Tools.getDefault("VMS.VNC.IconLarge"));
    /** VNC Viewer icon small. */
    static final ImageIcon VNC_ICON_SMALL = Tools.createImageIcon(
                                    Tools.getDefault("VMS.VNC.IconSmall"));
    /** Pause / Suspend icon. */
    static final ImageIcon PAUSE_ICON = Tools.createImageIcon(
                                       Tools.getDefault("VMS.Pause.IconLarge"));
    /** Resume icon. */
    private static final ImageIcon RESUME_ICON = Tools.createImageIcon(
                                      Tools.getDefault("VMS.Resume.IconLarge"));
    /** Shutdown icon. */
    private static final ImageIcon SHUTDOWN_ICON = Tools.createImageIcon(
                                    Tools.getDefault("VMS.Shutdown.IconLarge"));
    /** Reboot icon. */
    private static final ImageIcon REBOOT_ICON = Tools.createImageIcon(
                                      Tools.getDefault("VMS.Reboot.IconLarge"));
    /** Destroy icon. */
    private static final ImageIcon DESTROY_ICON = Tools.createImageIcon(
                                    Tools.getDefault("VMS.Destroy.IconLarge"));
    /** Header table. */
    static final String HEADER_TABLE = "header";
    /** Disk table. */
    static final String DISK_TABLE = "disks";
    /** Interface table. */
    static final String INTERFACES_TABLE = "interfaces";
    /** Input devices table. */
    static final String INPUTDEVS_TABLE = "inputdevs";
    /** Graphics devices table. */
    static final String GRAPHICS_TABLE = "graphics";
    /** Sound devices table. */
    static final String SOUND_TABLE = "sound";
    /** Serial devices table. */
    static final String SERIAL_TABLE = "serial";
    /** Parallel devices table. */
    static final String PARALLEL_TABLE = "parallel";
    /** Video devices table. */
    static final String VIDEO_TABLE = "video";
    /** Defined on host string value. */
    private static final String DEFINED_ON_HOST_TRUE = "True";
    /** Not defined on host string value. */
    private static final String DEFINED_ON_HOST_FALSE = "False";
    /** Wizard prefix string. */
    private static final String WIZARD_PREFIX = "wizard:";
    /** Virtual System header. */
    private static final String VIRTUAL_SYSTEM_STRING =
                Tools.getString("VMSVirtualDomainInfo.Section.VirtualSystem");
    /** System features. */
    private static final String VIRTUAL_SYSTEM_FEATURES =
                Tools.getString("VMSVirtualDomainInfo.Section.Features");
    /** System options. */
    private static final String VIRTUAL_SYSTEM_OPTIONS =
                Tools.getString("VMSVirtualDomainInfo.Section.Options");
    /** CPU match options. */
    private static final String CPU_MATCH_OPTIONS =
                Tools.getString("VMSVirtualDomainInfo.Section.CPUMatch");
    /** String that is displayed as a tool tip for not applied domain. */
    static final String NOT_APPLIED = "not applied yet";
    /** String that is displayed as a tool tip for disabled menu item. */
    static final String NO_VM_STATUS_STRING = "VM status is not available";
    /** Default widths for columns. */
    private static final Map<Integer, Integer> HEADER_DEFAULT_WIDTHS =
                                               new HashMap<Integer, Integer>();
    /** Default widths for columns. */
    private static final Map<Integer, Integer> DISK_DEFAULT_WIDTHS =
                                               new HashMap<Integer, Integer>();
    /** Default widths for columns. */
    private static final Map<Integer, Integer> INTERFACES_DEFAULT_WIDTHS =
                                               new HashMap<Integer, Integer>();
    /** Default widths for columns. */
    private static final Map<Integer, Integer> INPUTDEVS_DEFAULT_WIDTHS =
                                               new HashMap<Integer, Integer>();
    /** Default widths for columns. */
    private static final Map<Integer, Integer> GRAPHICS_DEFAULT_WIDTHS =
                                               new HashMap<Integer, Integer>();
    /** Default widths for columns. */
    private static final Map<Integer, Integer> SOUND_DEFAULT_WIDTHS =
                                               new HashMap<Integer, Integer>();
    /** Default widths for columns. */
    private static final Map<Integer, Integer> SERIAL_DEFAULT_WIDTHS =
                                               new HashMap<Integer, Integer>();
    /** Default widths for columns. */
    private static final Map<Integer, Integer> PARALLEL_DEFAULT_WIDTHS =
                                               new HashMap<Integer, Integer>();
    /** Default widths for columns. */
    private static final Map<Integer, Integer> VIDEO_DEFAULT_WIDTHS =
                                               new HashMap<Integer, Integer>();
    /** Width of the button field. */
    private static final int CONTROL_BUTTON_WIDTH = 80;
    static {
        /* remove button column */
        HEADER_DEFAULT_WIDTHS.put(4, CONTROL_BUTTON_WIDTH);
        DISK_DEFAULT_WIDTHS.put(2, CONTROL_BUTTON_WIDTH);
        INTERFACES_DEFAULT_WIDTHS.put(2, CONTROL_BUTTON_WIDTH);
        INPUTDEVS_DEFAULT_WIDTHS.put(1, CONTROL_BUTTON_WIDTH);
        GRAPHICS_DEFAULT_WIDTHS.put(1, CONTROL_BUTTON_WIDTH);
        SOUND_DEFAULT_WIDTHS.put(1, CONTROL_BUTTON_WIDTH);
        SERIAL_DEFAULT_WIDTHS.put(1, CONTROL_BUTTON_WIDTH);
        PARALLEL_DEFAULT_WIDTHS.put(1, CONTROL_BUTTON_WIDTH);
        VIDEO_DEFAULT_WIDTHS.put(1, CONTROL_BUTTON_WIDTH);
    }
    /** String that is displayed as a tool tip if a menu item is used by CRM. */
    static final String IS_USED_BY_CRM_STRING = "it is used by cluster manager";
    /** New vm domain button. */
    private JComponent newVMButton = null;
    /** New disk button. */
    private MyButton newDiskBtn = null;
    /** New interface button. */
    private MyButton newInterfaceBtn = null;
    /** New input button. */
    private MyButton newInputBtn = null;
    /** New graphics button. */
    private MyButton newGraphicsBtn = null;
    /** New sound button. */
    private MyButton newSoundBtn = null;
    /** New serial button. */
    private MyButton newSerialBtn = null;
    /** New parallel button. */
    private MyButton newParallelBtn = null;
    /** New video button. */
    private MyButton newVideoBtn = null;
    /** This is a map from host to the check box. */
    private final Map<String, GuiComboBox> definedOnHostComboBoxHash =
                                          new HashMap<String, GuiComboBox>();
    static {
        SECTION_MAP.put(VMSXML.VM_PARAM_NAME,          VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_TYPE,          VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_EMULATOR,      VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_VCPU,          VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_CURRENTMEMORY, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_MEMORY,        VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_BOOT,          VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_LOADER,        VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_AUTOSTART,     VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_ARCH,          VIRTUAL_SYSTEM_STRING);

        SECTION_MAP.put(VMSXML.VM_PARAM_ON_POWEROFF,   VIRTUAL_SYSTEM_OPTIONS);
        SECTION_MAP.put(VMSXML.VM_PARAM_ON_REBOOT,     VIRTUAL_SYSTEM_OPTIONS);
        SECTION_MAP.put(VMSXML.VM_PARAM_ON_CRASH,      VIRTUAL_SYSTEM_OPTIONS);

        SECTION_MAP.put(VMSXML.VM_PARAM_ACPI, VIRTUAL_SYSTEM_FEATURES);
        SECTION_MAP.put(VMSXML.VM_PARAM_APIC, VIRTUAL_SYSTEM_FEATURES);
        SECTION_MAP.put(VMSXML.VM_PARAM_PAE, VIRTUAL_SYSTEM_FEATURES);
        SECTION_MAP.put(VMSXML.VM_PARAM_HAP, VIRTUAL_SYSTEM_FEATURES);

        SECTION_MAP.put(VMSXML.VM_PARAM_CPU_MATCH, CPU_MATCH_OPTIONS);
        SECTION_MAP.put(VMSXML.VM_PARAM_CPUMATCH_MODEL, CPU_MATCH_OPTIONS);
        SECTION_MAP.put(VMSXML.VM_PARAM_CPUMATCH_VENDOR, CPU_MATCH_OPTIONS);
        SECTION_MAP.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS,
                        CPU_MATCH_OPTIONS);
        SECTION_MAP.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_CORES,
                        CPU_MATCH_OPTIONS);
        SECTION_MAP.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS,
                        CPU_MATCH_OPTIONS);
        SECTION_MAP.put(VMSXML.VM_PARAM_CPUMATCH_FEATURE_POLICY,
                        CPU_MATCH_OPTIONS);
        SECTION_MAP.put(VMSXML.VM_PARAM_CPUMATCH_FEATURES,
                        CPU_MATCH_OPTIONS);

        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_NAME,
                   Tools.getString("VMSVirtualDomainInfo.Short.Name"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_TYPE,
                   Tools.getString("VMSVirtualDomainInfo.Short.Type"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_VCPU,
                   Tools.getString("VMSVirtualDomainInfo.Short.Vcpu"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_CURRENTMEMORY,
                   Tools.getString("VMSVirtualDomainInfo.Short.CurrentMemory"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_MEMORY,
                   Tools.getString("VMSVirtualDomainInfo.Short.Memory"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_BOOT,
                   Tools.getString("VMSVirtualDomainInfo.Short.Os.Boot"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_LOADER,
                   Tools.getString("VMSVirtualDomainInfo.Short.Os.Loader"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_AUTOSTART,
                   Tools.getString("VMSVirtualDomainInfo.Short.Autostart"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_ARCH,
                   Tools.getString("VMSVirtualDomainInfo.Short.Arch"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_ACPI,
                   Tools.getString("VMSVirtualDomainInfo.Short.Acpi"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_APIC,
                   Tools.getString("VMSVirtualDomainInfo.Short.Apic"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_PAE,
                   Tools.getString("VMSVirtualDomainInfo.Short.Pae"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_HAP,
                   Tools.getString("VMSVirtualDomainInfo.Short.Hap"));

        SHORTNAME_MAP.put(
            VMSXML.VM_PARAM_CPU_MATCH,
            Tools.getString("VMSVirtualDomainInfo.Short.CPU.Match"));
        SHORTNAME_MAP.put(
            VMSXML.VM_PARAM_CPUMATCH_MODEL,
            Tools.getString("VMSVirtualDomainInfo.Short.CPUMatch.Model"));
        SHORTNAME_MAP.put(
            VMSXML.VM_PARAM_CPUMATCH_VENDOR,
            Tools.getString("VMSVirtualDomainInfo.Short.CPUMatch.Vendor"));
        SHORTNAME_MAP.put(
            VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS,
            Tools.getString(
                       "VMSVirtualDomainInfo.Short.CPUMatch.TopologySockets"));
        SHORTNAME_MAP.put(
            VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_CORES,
            Tools.getString(
                        "VMSVirtualDomainInfo.Short.CPUMatch.TopologyCores"));
        SHORTNAME_MAP.put(
            VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS,
            Tools.getString(
                       "VMSVirtualDomainInfo.Short.CPUMatch.TopologyThreads"));
        SHORTNAME_MAP.put(
            VMSXML.VM_PARAM_CPUMATCH_FEATURE_POLICY,
            Tools.getString("VMSVirtualDomainInfo.Short.CPUMatch.Policy"));
        SHORTNAME_MAP.put(
            VMSXML.VM_PARAM_CPUMATCH_FEATURES,
            Tools.getString("VMSVirtualDomainInfo.Short.CPUMatch.Features"));

        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_ON_POWEROFF,
                   Tools.getString("VMSVirtualDomainInfo.Short.OnPoweroff"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_ON_REBOOT,
                   Tools.getString("VMSVirtualDomainInfo.Short.OnReboot"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_ON_CRASH,
                   Tools.getString("VMSVirtualDomainInfo.Short.OnCrash"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_EMULATOR,
                   Tools.getString("VMSVirtualDomainInfo.Short.Emulator"));

        FIELD_TYPES.put(VMSXML.VM_PARAM_CURRENTMEMORY,
                        GuiComboBox.Type.TEXTFIELDWITHUNIT);
        FIELD_TYPES.put(VMSXML.VM_PARAM_MEMORY,
                        GuiComboBox.Type.TEXTFIELDWITHUNIT);
        FIELD_TYPES.put(VMSXML.VM_PARAM_AUTOSTART, GuiComboBox.Type.CHECKBOX);
        FIELD_TYPES.put(VMSXML.VM_PARAM_APIC, GuiComboBox.Type.CHECKBOX);
        FIELD_TYPES.put(VMSXML.VM_PARAM_ACPI, GuiComboBox.Type.CHECKBOX);
        FIELD_TYPES.put(VMSXML.VM_PARAM_PAE, GuiComboBox.Type.CHECKBOX);
        FIELD_TYPES.put(VMSXML.VM_PARAM_HAP, GuiComboBox.Type.CHECKBOX);

        PREFERRED_MAP.put(VMSXML.VM_PARAM_CURRENTMEMORY, "512M");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_MEMORY, "512M");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_ARCH, "x86_64");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_ACPI, "True");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_APIC, "True");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_PAE, "True");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_ON_POWEROFF, "destroy");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_ON_REBOOT, "restart");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_ON_CRASH, "restart");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_EMULATOR, "/usr/bin/kvm");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_AUTOSTART, "False");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_BOOT, "hd");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_TYPE, "kvm");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_VCPU, "1");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_ACPI, "False");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_APIC, "False");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_PAE, "False");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_HAP, "False");

        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPU_MATCH, "");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPUMATCH_MODEL, "");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPUMATCH_VENDOR, "");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS, "");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_CORES, "");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS, "");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPUMATCH_FEATURE_POLICY, "");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPUMATCH_FEATURES, "");

        HAS_UNIT_PREFIX.put(VMSXML.VM_PARAM_MEMORY, true);
        HAS_UNIT_PREFIX.put(VMSXML.VM_PARAM_CURRENTMEMORY, true);
        // TODO: no virsh command for os-boot
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_BOOT,
                           new StringInfo[]{
                                      new StringInfo("Hard Disk",
                                                     "hd",
                                                     null),
                                      new StringInfo("Network (PXE)",
                                                     "network",
                                                     null),
                                      new StringInfo("CD-ROM",
                                                     "cdrom",
                                                     null),
                                      new StringInfo("Floppy",
                                                     "fd",
                                                     null)});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_LOADER,
                            new String[]{"", "/usr/lib/xen/boot/hvmloader"});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_AUTOSTART,
                            new String[]{"True", "False"});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_TYPE,
                            new String[]{"kvm", "xen"});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_ARCH,
                            new String[]{"x86_64", "i686", ""});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_ON_POWEROFF,
                            new String[]{"destroy",
                                         "restart",
                                         "preserve",
                                         "rename-restart"});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_ON_REBOOT,
                            new String[]{"restart",
                                         "destroy",
                                         "preserve",
                                         "rename-restart"});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_ON_CRASH,
                            new String[]{"restart",
                                         "destroy",
                                         "preserve",
                                         "rename-restart",
                                         "coredump-destroy", /* since 0.8.4 */
                                         "coredump-restart"}); /* since 0.8.4*/
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_EMULATOR,
                            new StringInfo[]{
                                   new StringInfo("kvm",
                                                  "/usr/bin/kvm",
                                                  null),
                                   new StringInfo("xen",
                                                  "/usr/lib/xen/bin/qemu-dm",
                                                  null),
                                   new StringInfo("qemu",
                                                  "/usr/bin/qemu",
                                               null)});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_CPU_MATCH,
                            new String[]{"", "exact", "minimum", "strict"});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS,
                            new String[]{"", "1", "2"});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_CORES,
                            new String[]{"", "1", "2"});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS,
                            new String[]{"", "1", "2"});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_CPUMATCH_FEATURE_POLICY,
                            new String[]{"",
                                         "force",
                                         "require",
                                         "optional",
                                         "disable",
                                         "forbid"});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_CPUMATCH_FEATURES,
                            new String[]{"", "aes", "aes apic"});
        IS_INTEGER.add(VMSXML.VM_PARAM_VCPU);
        IS_INTEGER.add(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS);
        IS_INTEGER.add(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_CORES);
        IS_INTEGER.add(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS);
        REQUIRED_VERSION.put(VMSXML.VM_PARAM_CPU_MATCH, "0.7.5");
        REQUIRED_VERSION.put(VMSXML.VM_PARAM_CPUMATCH_MODEL, "0.7.5");
        REQUIRED_VERSION.put(VMSXML.VM_PARAM_CPUMATCH_VENDOR, "0.8.3");
        REQUIRED_VERSION.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS,
                             "0.7.5");
        REQUIRED_VERSION.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_CORES, "0.7.5");
        REQUIRED_VERSION.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS,
                             "0.7.5");
        REQUIRED_VERSION.put(VMSXML.VM_PARAM_CPUMATCH_FEATURE_POLICY,
                             "0.7.5");
        REQUIRED_VERSION.put(VMSXML.VM_PARAM_CPUMATCH_FEATURES,
                             "0.7.5");
    }
    /** Creates the VMSVirtualDomainInfo object. */
    public VMSVirtualDomainInfo(final String name,
                                final Browser browser) {
        super(name, browser);
        setResource(new Resource(name));
    }

    /** Returns browser object of this info. */
    @Override public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** Returns a name of the service with virtual domain name. */
    @Override public String toString() {
        if (getResource().isNew()) {
            return "new domain...";
        } else {
            return getName();
        }
    }

    /** Returns domain name. */
    protected String getDomainName() {
        return getName();
    }

    /** Updates disk nodes. Returns whether the node changed. */
    private boolean updateDiskNodes() {
        final Map<String, DiskData> disks = getDisks();
        final List<String> diskNames  = new ArrayList<String>();
        if (disks != null) {
            for (final String d : disks.keySet()) {
                diskNames.add(d);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = getNode().children();
        boolean nodeChanged = false;
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) e.nextElement();
            if (!(node.getUserObject() instanceof VMSDiskInfo)) {
                continue;
            }
            final VMSDiskInfo vmsdi =
                              (VMSDiskInfo) node.getUserObject();
            if (vmsdi.getResource().isNew()) {
                /* keep */
            } else if (diskNames.contains(vmsdi.getName())) {
                /* keeping */
                diskNames.remove(vmsdi.getName());
                try {
                    mDiskToInfoLock.acquire();
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                diskToInfo.put(vmsdi.getName(), vmsdi);
                mDiskToInfoLock.release();
                vmsdi.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                try {
                    mDiskToInfoLock.acquire();
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                diskToInfo.remove(vmsdi.getName());
                mDiskToInfoLock.release();
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    node.removeFromParent();
                }
            });
        }

        for (final String disk : diskNames) {
            final Enumeration eee = getNode().children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) eee.nextElement();
                if (!(node.getUserObject() instanceof VMSDiskInfo)) {
                    continue;
                }
                final VMSDiskInfo v = (VMSDiskInfo) node.getUserObject();
                final String n = v.getName();
                if (n != null && disk.compareTo(n) < 0) {
                    break;
                }
                i++;
            }
            /* add new vm disk */
            final VMSDiskInfo vmsdi =
                                new VMSDiskInfo(disk, getBrowser(), this);
            try {
                mDiskToInfoLock.acquire();
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            diskToInfo.put(disk, vmsdi);
            mDiskToInfoLock.release();
            vmsdi.updateParameters();
            final DefaultMutableTreeNode resource =
                                        new DefaultMutableTreeNode(vmsdi);
            getBrowser().setNode(resource);
            getNode().insert(resource, i);
            nodeChanged = true;
        }
        return nodeChanged;
    }

    /** Updates interface nodes. Returns whether the node changed. */
    private boolean updateInterfaceNodes() {
        final Map<String, InterfaceData> interfaces = getInterfaces();
        final List<String> interfaceNames  = new ArrayList<String>();
        if (interfaces != null) {
            for (final String i : interfaces.keySet()) {
                interfaceNames.add(i);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = getNode().children();
        boolean nodeChanged = false;
        VMSInterfaceInfo emptySlot = null; /* for generated mac address. */
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) e.nextElement();
            if (!(node.getUserObject() instanceof VMSInterfaceInfo)) {
                continue;
            }
            final VMSInterfaceInfo vmsii =
                                      (VMSInterfaceInfo) node.getUserObject();
            if (vmsii.getResource().isNew()) {
                /* keep */
            } else if ("generate".equals(vmsii.getName())) {
                emptySlot = vmsii;
            } else if (interfaceNames.contains(vmsii.getName())) {
                /* keeping */
                interfaceNames.remove(vmsii.getName());
                vmsii.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                try {
                    mInterfaceToInfoLock.acquire();
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                interfaceToInfo.remove(vmsii.getName());
                mInterfaceToInfoLock.release();
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    node.removeFromParent();
                }
            });
        }

        for (final String interf : interfaceNames) {
            VMSInterfaceInfo vmsii;
            if (emptySlot == null) {
                final Enumeration eee = getNode().children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                    if (!(node.getUserObject() instanceof VMSInterfaceInfo)) {
                        if (node.getUserObject() instanceof VMSDiskInfo) {
                            i++;
                        }
                        continue;
                    }
                    final VMSInterfaceInfo v =
                                       (VMSInterfaceInfo) node.getUserObject();

                    final String n = v.getName();
                    if (n != null && interf.compareTo(n) < 0) {
                        break;
                    }
                    i++;
                }
                /* add new vm interface */
                vmsii = new VMSInterfaceInfo(interf, getBrowser(), this);
                final DefaultMutableTreeNode resource =
                                            new DefaultMutableTreeNode(vmsii);
                getBrowser().setNode(resource);
                getNode().insert(resource, i);
                nodeChanged = true;
            } else {
                vmsii = emptySlot;
                vmsii.setName(interf);
                emptySlot = null;
            }
            try {
                mInterfaceToInfoLock.acquire();
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            interfaceToInfo.put(interf, vmsii);
            mInterfaceToInfoLock.release();
            vmsii.updateParameters();
        }
        return nodeChanged;
    }

    /** Updates input devices nodes. Returns whether the node changed. */
    private boolean updateInputDevNodes() {
        final Map<String, InputDevData> inputDevs = getInputDevs();
        final List<String> inputDevNames  = new ArrayList<String>();
        if (inputDevs != null) {
            for (final String d : inputDevs.keySet()) {
                inputDevNames.add(d);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = getNode().children();
        boolean nodeChanged = false;
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) e.nextElement();
            if (!(node.getUserObject() instanceof VMSInputDevInfo)) {
                continue;
            }
            final VMSInputDevInfo vmsid =
                              (VMSInputDevInfo) node.getUserObject();
            if (vmsid.getResource().isNew()) {
                /* keep */
            } else if (inputDevNames.contains(vmsid.getName())) {
                /* keeping */
                inputDevNames.remove(vmsid.getName());
                vmsid.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                try {
                    mInputDevToInfoLock.acquire();
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                inputDevToInfo.remove(vmsid.getName());
                mInputDevToInfoLock.release();
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    node.removeFromParent();
                }
            });
        }

        for (final String inputDev : inputDevNames) {
            VMSInputDevInfo vmsid;
            final InputDevData data = inputDevs.get(inputDev);
            final Enumeration eee = getNode().children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) eee.nextElement();
                if (!(node.getUserObject() instanceof VMSInputDevInfo)) {
                    if (node.getUserObject() instanceof VMSDiskInfo
                        || node.getUserObject() instanceof VMSInterfaceInfo) {
                        i++;
                    }
                    continue;
                }
                final VMSInputDevInfo v =
                                     (VMSInputDevInfo) node.getUserObject();
                final String n = v.getName();
                if (n != null && inputDev.compareTo(n) < 0) {
                    break;
                }
                i++;
            }
            /* add new vm input device */
            vmsid = new VMSInputDevInfo(inputDev, getBrowser(), this);
            final DefaultMutableTreeNode resource =
                                        new DefaultMutableTreeNode(vmsid);
            getBrowser().setNode(resource);
            getNode().insert(resource, i);
            nodeChanged = true;
            try {
                mInputDevToInfoLock.acquire();
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            inputDevToInfo.put(inputDev, vmsid);
            mInputDevToInfoLock.release();
            vmsid.updateParameters();
        }
        /* Sort it. */
        int i = 0;
        for (int j = 0; j < getNode().getChildCount(); j++) {
            final DefaultMutableTreeNode node =
                         (DefaultMutableTreeNode) getNode().getChildAt(j);
            final VMSHardwareInfo v = (VMSHardwareInfo) node.getUserObject();
            final String n = v.getName();
            if (i > 0) {
                final DefaultMutableTreeNode prev =
                     (DefaultMutableTreeNode) getNode().getChildAt(j - 1);
                final VMSHardwareInfo prevI =
                                        (VMSHardwareInfo) prev.getUserObject();
                if (prevI.getClass().getName().equals(v.getClass().getName())) {
                    final String prevN = prevI.getName();
                    if (!prevI.getResource().isNew()
                        && !v.getResource().isNew()
                        && prevN.compareTo(n) > 0) {
                        getNode().remove(j);
                        getNode().insert(node, j - 1);
                    }
                } else {
                    i = 0;
                }
            }
            i++;
        }
        return nodeChanged;
    }

    /** Updates graphics devices nodes. Returns whether the node changed. */
    private boolean updateGraphicsNodes() {
        final Map<String, GraphicsData> graphicDisplays =
                                                        getGraphicDisplays();
        final List<String> graphicsNames  = new ArrayList<String>();
        if (graphicDisplays != null) {
            for (final String d : graphicDisplays.keySet()) {
                graphicsNames.add(d);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = getNode().children();
        boolean nodeChanged = false;
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) e.nextElement();
            if (!(node.getUserObject() instanceof VMSGraphicsInfo)) {
                continue;
            }
            final VMSGraphicsInfo vmsgi =
                              (VMSGraphicsInfo) node.getUserObject();
            if (vmsgi.getResource().isNew()) {
                /* keep */
            } else if (graphicsNames.contains(vmsgi.getName())) {
                /* keeping */
                graphicsNames.remove(vmsgi.getName());
                vmsgi.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                try {
                    mGraphicsToInfoLock.acquire();
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                graphicsToInfo.remove(vmsgi.getName());
                mGraphicsToInfoLock.release();
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    node.removeFromParent();
                }
            });
        }

        for (final String graphicDisplay : graphicsNames) {
            VMSGraphicsInfo vmsgi;
            final GraphicsData data = graphicDisplays.get(graphicDisplay);
            final Enumeration eee = getNode().children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) eee.nextElement();
                if (!(node.getUserObject() instanceof VMSGraphicsInfo)) {
                    if (node.getUserObject() instanceof VMSDiskInfo
                        || node.getUserObject() instanceof VMSInterfaceInfo
                        || node.getUserObject() instanceof VMSInputDevInfo) {
                        i++;
                    }
                    continue;
                }
                final VMSGraphicsInfo v =
                                     (VMSGraphicsInfo) node.getUserObject();
                final String n = v.getName();
                if (n != null && graphicDisplay.compareTo(n) < 0) {
                    break;
                }
                i++;
            }
            /* add new vm graphics device */
            vmsgi = new VMSGraphicsInfo(graphicDisplay, getBrowser(), this);
            final DefaultMutableTreeNode resource =
                                        new DefaultMutableTreeNode(vmsgi);
            getBrowser().setNode(resource);
            getNode().insert(resource, i);
            nodeChanged = true;
            try {
                mGraphicsToInfoLock.acquire();
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            graphicsToInfo.put(graphicDisplay, vmsgi);
            mGraphicsToInfoLock.release();
            vmsgi.updateParameters();
        }
        return nodeChanged;
    }

    /** Updates sound devices nodes. Returns whether the node changed. */
    private boolean updateSoundNodes() {
        final Map<String, SoundData> sounds = getSounds();
        final List<String> soundNames  = new ArrayList<String>();
        if (sounds != null) {
            for (final String d : sounds.keySet()) {
                soundNames.add(d);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = getNode().children();
        boolean nodeChanged = false;
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) e.nextElement();
            if (!(node.getUserObject() instanceof VMSSoundInfo)) {
                continue;
            }
            final VMSSoundInfo vmssi =
                              (VMSSoundInfo) node.getUserObject();
            if (vmssi.getResource().isNew()) {
                /* keep */
            } else if (soundNames.contains(vmssi.getName())) {
                /* keeping */
                soundNames.remove(vmssi.getName());
                vmssi.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                try {
                    mSoundToInfoLock.acquire();
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                soundToInfo.remove(vmssi.getName());
                mSoundToInfoLock.release();
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    node.removeFromParent();
                }
            });
        }

        for (final String sound : soundNames) {
            VMSSoundInfo vmssi;
            final SoundData data = sounds.get(sound);
            final Enumeration eee = getNode().children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) eee.nextElement();
                if (!(node.getUserObject() instanceof VMSSoundInfo)) {
                    if (node.getUserObject() instanceof VMSDiskInfo
                        || node.getUserObject() instanceof VMSInterfaceInfo
                        || node.getUserObject() instanceof VMSInputDevInfo
                        || node.getUserObject() instanceof VMSGraphicsInfo) {
                        i++;
                    }
                    continue;
                }
                final VMSSoundInfo v = (VMSSoundInfo) node.getUserObject();
                final String n = v.getName();
                if (n != null && sound.compareTo(n) < 0) {
                    break;
                }
                i++;
            }
            /* add new vm sound device */
            vmssi = new VMSSoundInfo(sound, getBrowser(), this);
            final DefaultMutableTreeNode resource =
                                        new DefaultMutableTreeNode(vmssi);
            getBrowser().setNode(resource);
            getNode().insert(resource, i);
            nodeChanged = true;
            try {
                mSoundToInfoLock.acquire();
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            soundToInfo.put(sound, vmssi);
            mSoundToInfoLock.release();
            vmssi.updateParameters();
        }
        return nodeChanged;
    }

    /** Updates serial devices nodes. Returns whether the node changed. */
    private boolean updateSerialNodes() {
        final Map<String, SerialData> serials = getSerials();
        final List<String> serialNames  = new ArrayList<String>();
        if (serials != null) {
            for (final String d : serials.keySet()) {
                serialNames.add(d);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = getNode().children();
        boolean nodeChanged = false;
        VMSSerialInfo emptySlot = null; /* for generated target port. */
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) e.nextElement();
            if (!(node.getUserObject() instanceof VMSSerialInfo)) {
                continue;
            }
            final VMSSerialInfo vmssi = (VMSSerialInfo) node.getUserObject();
            if (vmssi.getResource().isNew()) {
                /* keep */
            } else if ("generate".equals(vmssi.getTargetPort())) {
                emptySlot = vmssi;
            } else if (serialNames.contains(vmssi.getName())) {
                /* keeping */
                serialNames.remove(vmssi.getName());
                vmssi.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                try {
                    mSerialToInfoLock.acquire();
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                serialToInfo.remove(vmssi.getName());
                mSerialToInfoLock.release();
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    node.removeFromParent();
                }
            });
        }

        for (final String serial : serialNames) {
            VMSSerialInfo vmssi;
            final SerialData data = serials.get(serial);
            if (emptySlot == null) {
                final Enumeration eee = getNode().children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                    if (!(node.getUserObject() instanceof VMSSerialInfo)) {
                        if (node.getUserObject() instanceof VMSDiskInfo
                            || node.getUserObject() instanceof VMSInterfaceInfo
                            || node.getUserObject() instanceof VMSInputDevInfo
                            || node.getUserObject() instanceof VMSGraphicsInfo
                            || node.getUserObject() instanceof VMSSoundInfo) {
                            i++;
                        }
                        continue;
                    }
                    final VMSSerialInfo v =
                                          (VMSSerialInfo) node.getUserObject();
                    final String n = v.getName();
                    if (n != null && serial.compareTo(n) < 0) {
                        break;
                    }
                    i++;
                }
                /* add new vm serial device */
                vmssi = new VMSSerialInfo(serial, getBrowser(), this);
                final DefaultMutableTreeNode resource =
                                            new DefaultMutableTreeNode(vmssi);
                getBrowser().setNode(resource);
                getNode().insert(resource, i);
                nodeChanged = true;
            } else {
                vmssi = emptySlot;
                vmssi.setName(serial);
                emptySlot = null;
            }
            try {
                mSerialToInfoLock.acquire();
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            serialToInfo.put(serial, vmssi);
            mSerialToInfoLock.release();
            vmssi.updateParameters();
        }
        return nodeChanged;
    }

    /** Updates parallel devices nodes. Returns whether the node changed. */
    private boolean updateParallelNodes() {
        final Map<String, ParallelData> parallels = getParallels();
        final List<String> parallelNames  = new ArrayList<String>();
        if (parallels != null) {
            for (final String d : parallels.keySet()) {
                parallelNames.add(d);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = getNode().children();
        boolean nodeChanged = false;
        VMSParallelInfo emptySlot = null; /* for generated target port. */
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) e.nextElement();
            if (!(node.getUserObject() instanceof VMSParallelInfo)) {
                continue;
            }
            final VMSParallelInfo vmspi =
                                      (VMSParallelInfo) node.getUserObject();
            if (vmspi.getResource().isNew()) {
                /* keep */
            } else if ("generate".equals(vmspi.getTargetPort())) {
                emptySlot = vmspi;
            } else if (parallelNames.contains(vmspi.getName())) {
                /* keeping */
                parallelNames.remove(vmspi.getName());
                vmspi.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                try {
                    mParallelToInfoLock.acquire();
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                parallelToInfo.remove(vmspi.getName());
                mParallelToInfoLock.release();
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    node.removeFromParent();
                }
            });
        }

        for (final String parallel : parallelNames) {
            VMSParallelInfo vmspi;
            final ParallelData data = parallels.get(parallel);
            if (emptySlot == null) {
                final Enumeration eee = getNode().children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                    if (!(node.getUserObject() instanceof VMSParallelInfo)) {
                        if (node.getUserObject() instanceof VMSDiskInfo
                            || node.getUserObject() instanceof VMSInterfaceInfo
                            || node.getUserObject() instanceof VMSInputDevInfo
                            || node.getUserObject() instanceof VMSGraphicsInfo
                            || node.getUserObject() instanceof VMSSoundInfo
                            || node.getUserObject() instanceof VMSSerialInfo) {
                            i++;
                        }
                        continue;
                    }
                    final VMSParallelInfo v =
                                        (VMSParallelInfo) node.getUserObject();
                    final String n = v.getName();
                    if (n != null && parallel.compareTo(n) < 0) {
                        break;
                    }
                    i++;
                }
                /* add new vm parallel device */
                vmspi = new VMSParallelInfo(parallel, getBrowser(), this);
                final DefaultMutableTreeNode resource =
                                            new DefaultMutableTreeNode(vmspi);
                getBrowser().setNode(resource);
                getNode().insert(resource, i);
                nodeChanged = true;
            } else {
                vmspi = emptySlot;
                vmspi.setName(parallel);
                emptySlot = null;
            }
            try {
                mParallelToInfoLock.acquire();
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            parallelToInfo.put(parallel, vmspi);
            mParallelToInfoLock.release();
            vmspi.updateParameters();
        }
        return nodeChanged;
    }

    /** Updates video devices nodes. Returns whether the node changed. */
    private boolean updateVideoNodes() {
        final Map<String, VideoData> videos = getVideos();
        final List<String> videoNames  = new ArrayList<String>();
        if (videos != null) {
            for (final String d : videos.keySet()) {
                videoNames.add(d);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = getNode().children();
        boolean nodeChanged = false;
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) e.nextElement();
            if (!(node.getUserObject() instanceof VMSVideoInfo)) {
                continue;
            }
            final VMSVideoInfo vmspi = (VMSVideoInfo) node.getUserObject();
            if (vmspi.getResource().isNew()) {
                /* keep */
            } else if (videoNames.contains(vmspi.getName())) {
                /* keeping */
                videoNames.remove(vmspi.getName());
                vmspi.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                try {
                    mVideoToInfoLock.acquire();
                } catch (final InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                videoToInfo.remove(vmspi.getName());
                mVideoToInfoLock.release();
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    node.removeFromParent();
                }
            });
        }

        for (final String video : videoNames) {
            VMSVideoInfo vmspi;
            final VideoData data = videos.get(video);
            final Enumeration eee = getNode().children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) eee.nextElement();
                if (!(node.getUserObject() instanceof VMSVideoInfo)) {
                    if (node.getUserObject() instanceof VMSDiskInfo
                        || node.getUserObject() instanceof VMSInterfaceInfo
                        || node.getUserObject() instanceof VMSInputDevInfo
                        || node.getUserObject() instanceof VMSGraphicsInfo
                        || node.getUserObject() instanceof VMSSoundInfo
                        || node.getUserObject() instanceof VMSSerialInfo
                        || node.getUserObject() instanceof VMSParallelInfo) {
                        i++;
                    }
                    continue;
                }
                final VMSVideoInfo v = (VMSVideoInfo) node.getUserObject();
                final String n = v.getName();
                if (n != null && video.compareTo(n) < 0) {
                    break;
                }
                i++;
            }
            /* add new vm video device */
            vmspi = new VMSVideoInfo(video, getBrowser(), this);
            final DefaultMutableTreeNode resource =
                                        new DefaultMutableTreeNode(vmspi);
            getBrowser().setNode(resource);
            getNode().insert(resource, i);
            nodeChanged = true;
            try {
                mVideoToInfoLock.acquire();
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            videoToInfo.put(video, vmspi);
            mVideoToInfoLock.release();
            vmspi.updateParameters();
        }
        return nodeChanged;
    }

    /** Returns button for defined hosts. */
    private MyButton getHostButton(final Host host, final String prefix) {
        final MyButton hostBtn = new MyButton("Start", null, "not defined on "
                                                             + host.getName());
        hostBtn.miniButton();
        final MyButton hBtn = hostBtn;
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                hBtn.setBackgroundColor(Browser.PANEL_BACKGROUND);
            }
        });
        hostBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override public void run() {
                        final VMSXML vxml = getBrowser().getVMSXML(host);
                        if (vxml != null) {
                            if (hBtn.getIcon() == VNC_ICON) {
                                final int remotePort = vxml.getRemotePort(
                                                          getDomainName());
                                Tools.startTightVncViewer(host,
                                                          remotePort);
                            } else if (hBtn.getIcon()
                                       == HostBrowser.HOST_ON_ICON) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override public void run() {
                                        hBtn.setEnabled(false);
                                    }
                                });
                                start(host);
                            }
                        }
                    }
                });
                t.start();
            }
        });
        hostBtn.setPreferredSize(new Dimension(80, 20));
        hostBtn.setMinimumSize(hostBtn.getPreferredSize());
        if (prefix == null) {
            hostButtons.put(host.getName(), hostBtn);
        } else {
            hostButtons.put(prefix + ":" + host.getName(), hostBtn);
        }
        return hostBtn;
    }

    /** Sets service parameters with values from resourceNode hash. */
    public void updateParameters() {
        final List<String> runningOnHosts = new ArrayList<String>();
        final List<String> suspendedOnHosts = new ArrayList<String>();
        final List<String> definedhosts = new ArrayList<String>();
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            final String hostName = h.getName();
            final MyButton hostBtn = hostButtons.get(h.getName());
            final MyButton wizardHostBtn =
                                  hostButtons.get(WIZARD_PREFIX + h.getName());
            final GuiComboBox hostCB =
                                    definedOnHostComboBoxHash.get(h.getName());
            final GuiComboBox wizardHostCB =
                       definedOnHostComboBoxHash.get(WIZARD_PREFIX + hostName);

            if (vmsxml != null
                && vmsxml.getDomainNames().contains(getDomainName())) {
                if (vmsxml.isRunning(getDomainName())) {
                    if (vmsxml.isSuspended(getDomainName())) {
                        suspendedOnHosts.add(hostName);
                        try {
                            mTransitionLock.acquire();
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        suspending.remove(hostName);
                        mTransitionLock.release();
                    } else {
                        try {
                            mTransitionLock.acquire();
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        resuming.remove(hostName);
                        mTransitionLock.release();
                    }
                    runningOnHosts.add(hostName);
                    try {
                        mTransitionLock.acquire();
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    starting.remove(hostName);
                    mTransitionLock.release();
                }
                definedhosts.add(hostName);
            } else {
                definedhosts.add("<font color=\"#A3A3A3\">"
                                 + hostName + "</font>");
            }
        }
        definedOnString = "<html>"
                          + Tools.join(", ", definedhosts.toArray(
                                     new String[definedhosts.size()]))
                          + "</html>";
        running = !runningOnHosts.isEmpty();
        try {
            mTransitionLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        /* Set host buttons */
        setHostButtons(running);
        if (runningOnHosts.isEmpty() && starting.isEmpty()) {
            shuttingdown.clear();
            suspending.clear();
            resuming.clear();
            mTransitionLock.release();
            runningOnString = "Stopped";
        } else {
            mTransitionLock.release();
            if (progress.charAt(0) == '-') {
                progress.setCharAt(0, '\\');
            } else if (progress.charAt(0) == '\\') {
                progress.setCharAt(0, '|');
            } else if (progress.charAt(0) == '|') {
                progress.setCharAt(0, '/');
            } else if (progress.charAt(0) == '/') {
                progress.setCharAt(0, '-');
            }
            try {
                mTransitionLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (!starting.isEmpty()) {
                runningOnString =
                        "<html>Starting on: "
                        + Tools.join(", ", starting.toArray(
                                            new String[starting.size()]))
                        + progress.toString()
                        + "</html>";
            } else if (!shuttingdown.isEmpty()) {
                runningOnString =
                        "<html>Shutting down on: "
                        + Tools.join(", ", shuttingdown.toArray(
                                            new String[shuttingdown.size()]))
                        + progress.toString()
                        + "</html>";
            } else if (!suspending.isEmpty()) {
                runningOnString =
                        "<html>Suspending on: "
                        + Tools.join(", ", suspending.toArray(
                                                new String[suspending.size()]))
                        + progress.toString()
                        + "</html>";
            } else if (!resuming.isEmpty()) {
                runningOnString =
                        "<html>Resuming on: "
                        + Tools.join(", ", resuming.toArray(
                                                new String[suspending.size()]))
                        + progress.toString()
                        + "</html>";
            } else if (!suspendedOnHosts.isEmpty()) {
                runningOnString =
                        "<html>Paused on: "
                        + Tools.join(", ", suspendedOnHosts.toArray(
                                          new String[suspendedOnHosts.size()]))
                        + "</html>";
            } else {
                runningOnString =
                        "<html>Running on: "
                        + Tools.join(", ", runningOnHosts.toArray(
                                            new String[runningOnHosts.size()]))
                        + "</html>";
            }
            mTransitionLock.release();
            for (final Host h : getBrowser().getClusterHosts()) {
                final VMSXML vmsxml = getBrowser().getVMSXML(h);
                final GuiComboBox hcb =
                                    definedOnHostComboBoxHash.get(h.getName());
            }
        }
        for (final String param : getParametersFromXML()) {
            final String oldValue = getParamSaved(param);
            String value = getParamSaved(param);
            final GuiComboBox cb = paramComboBoxGet(param, null);
            for (final Host h : getBrowser().getClusterHosts()) {
                final VMSXML vmsxml = getBrowser().getVMSXML(h);
                if (vmsxml != null) {
                    final String savedValue =
                                       vmsxml.getValue(getDomainName(), param);
                    if (savedValue == null) {
                        value = getParamDefault(param);
                    } else {
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
        updateTable(HEADER_TABLE);
        updateTable(DISK_TABLE);
        updateTable(INTERFACES_TABLE);
        updateTable(INPUTDEVS_TABLE);
        updateTable(GRAPHICS_TABLE);
        updateTable(SOUND_TABLE);
        updateTable(SERIAL_TABLE);
        updateTable(PARALLEL_TABLE);
        updateTable(VIDEO_TABLE);
        /* disks */
        final boolean interfaceNodeChanged = updateInterfaceNodes();
        final boolean diskNodeChanged = updateDiskNodes();
        final boolean inputDevNodeChanged = updateInputDevNodes();
        final boolean graphicsNodeChanged = updateGraphicsNodes();
        final boolean soundNodeChanged = updateSoundNodes();
        final boolean serialNodeChanged = updateSerialNodes();
        final boolean parallelNodeChanged = updateParallelNodes();
        final boolean vidoNodeChanged = updateVideoNodes();
        if (diskNodeChanged
            || interfaceNodeChanged
            || inputDevNodeChanged
            || graphicsNodeChanged
            || soundNodeChanged
            || serialNodeChanged
            || parallelNodeChanged
            || vidoNodeChanged) {
            getBrowser().reload(getNode(), false);
        }
        //final VMSInfo vmsi = (VMSInfo) vmsNode.getUserObject();
        //if (vmsi != null) {
        //    vmsi.updateTable(VMSInfo.MAIN_TABLE);
        //}
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                getBrowser().repaintTree();
            }
        });
    }

    /** Returns menu item for VNC different viewers. */
    private String getVNCMenuString(final String viewer, final Host host) {
        return Tools.getString("VMSVirtualDomainInfo.StartVNCViewerOn")
                            .replaceAll("@VIEWER@", viewer)
               + host.getName();
    }

    /** Returns "Defined on" panel. */
    public JPanel getDefinedOnHostsPanel(final String prefix,
                                         final MyButton thisApplyButton) {
        final JPanel hostPanel = new JPanel(new SpringLayout());
        int rows = 0;
        boolean running = false;
        for (final Host host : getBrowser().getClusterHosts()) {
            String defaultValue = null;
            final VMSXML vmsxml = getBrowser().getVMSXML(host);
            if (vmsxml != null && vmsxml.isRunning(getDomainName())) {
                running = true;
            }
            boolean notDefined = false;
            if (vmsxml != null && !vmsxml.getDomainNames().contains(
                                                            getDomainName())) {
                notDefined = false;
            }
            if (host.isConnected()
                && (getResource().isNew()
                    || (vmsxml != null
                        && vmsxml.getDomainNames().contains(
                                                        getDomainName())))) {
                defaultValue = DEFINED_ON_HOST_TRUE;
            } else {
                defaultValue = DEFINED_ON_HOST_FALSE;
            }
            final MyButton hostBtn = getHostButton(host, prefix);
            final GuiComboBox cb = new GuiComboBox(
                                        defaultValue,
                                        null, /* items */
                                        null,
                                        GuiComboBox.Type.CHECKBOX,
                                        null,
                                        ClusterBrowser.SERVICE_FIELD_WIDTH * 2,
                                        null, /* abbrv */
                                        new AccessMode(
                                          ConfigData.AccessType.ADMIN,
                                          false),
                                        hostBtn);
            GuiComboBox rpcb = null;
            if (prefix == null) {
                definedOnHostComboBoxHash.put(host.getName(), cb);
            } else {
                definedOnHostComboBoxHash.put(prefix + ":" + host.getName(),
                                              cb);
                rpcb = definedOnHostComboBoxHash.get(host.getName());
            }
            final GuiComboBox realParamCb = rpcb;
            if (!host.isConnected()) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        cb.setEnabled(false);
                    }
                });
            }
            cb.addListeners(
                new ItemListener() {
                    @Override public void itemStateChanged(final ItemEvent e) {
                        if (cb.isCheckBox()
                            || e.getStateChange() == ItemEvent.SELECTED) {
                            checkParameterFields(cb,
                                                 realParamCb,
                                                 ServiceInfo.CACHED_FIELD,
                                                 getParametersFromXML(),
                                                 thisApplyButton);
                        }
                    }
                }, null);
            cb.setBackgroundColor(ClusterBrowser.PANEL_BACKGROUND);
            final JLabel label = new JLabel(host.getName());
            cb.setLabel(label, null);
            addField(hostPanel,
                     label,
                     cb,
                     ClusterBrowser.SERVICE_LABEL_WIDTH,
                     ClusterBrowser.SERVICE_FIELD_WIDTH * 2,
                     0);
            rows++;
        }
        setHostButtons(running);
        SpringUtilities.makeCompactGrid(hostPanel, rows, 2, /* rows, cols */
                                                   1, 1,    /* initX, initY */
                                                   1, 1);   /* xPad, yPad */
        final JPanel doPanel = getParamPanel("Defined on");
        doPanel.add(hostPanel);
        return doPanel;
    }

    /** Returns info panel. */
    @Override public JComponent getInfoPanel() {
        if (infoPanel != null) {
            return infoPanel;
        }
        final boolean abExisted = getApplyButton() != null;
        /* main, button and options panels */
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        final JTable table = getTable(HEADER_TABLE);
        if (table != null) {
            newVMButton = getBrowser().getVMSInfo().getNewButton();
            mainPanel.add(newVMButton);
            mainPanel.add(table.getTableHeader());
            mainPanel.add(table);
        }

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);

        final String[] params = getParametersFromXML();
        initApplyButton(null);
        /* add item listeners to the apply button. */
        if (!abExisted) {
            getApplyButton().addActionListener(
                new ActionListener() {
                    @Override public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override public void run() {
                                getBrowser().clStatusLock();
                                apply(false);
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );
            getRevertButton().addActionListener(
                new ActionListener() {
                    @Override public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override public void run() {
                                getBrowser().clStatusLock();
                                revert();
                                getBrowser().clStatusUnlock();
                            }
                        });
                        thread.start();
                    }
                }
            );
        }
        final JPanel extraButtonPanel =
                           new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        extraButtonPanel.setBackground(Browser.STATUS_BACKGROUND);
        buttonPanel.add(extraButtonPanel);
        addApplyButton(buttonPanel);
        addRevertButton(extraButtonPanel);
        final MyButton overviewButton = new MyButton("VMs Overview",
                                                     BACK_ICON);
        overviewButton.miniButton();
        overviewButton.setPreferredSize(new Dimension(130, 50));
        overviewButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                getBrowser().getVMSInfo().selectMyself();
            }
        });
        extraButtonPanel.add(overviewButton);
        /* define on hosts */
        optionsPanel.add(getDefinedOnHostsPanel(null, getApplyButton()));
        addParams(optionsPanel,
                  params,
                  ClusterBrowser.SERVICE_LABEL_WIDTH,
                  ClusterBrowser.SERVICE_FIELD_WIDTH * 2,
                  null);
        /* Actions */
        final JMenuBar mb = new JMenuBar();
        JMenu serviceCombo;
        serviceCombo = getActionsMenu();
        mb.add(serviceCombo);
        buttonPanel.add(mb, BorderLayout.EAST);
        mainPanel.add(optionsPanel);

        newDiskBtn = VMSDiskInfo.getNewBtn(this);
        newInterfaceBtn = VMSInterfaceInfo.getNewBtn(this);
        newInputBtn = VMSInputDevInfo.getNewBtn(this);
        newGraphicsBtn = VMSGraphicsInfo.getNewBtn(this);
        newSoundBtn = VMSSoundInfo.getNewBtn(this);
        newSerialBtn = VMSSerialInfo.getNewBtn(this);
        newParallelBtn = VMSParallelInfo.getNewBtn(this);
        newVideoBtn = VMSVideoInfo.getNewBtn(this);
        /* new video button */
        mainPanel.add(getTablePanel("Disks",
                                    DISK_TABLE,
                                    newDiskBtn));

        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Interfaces",
                                    INTERFACES_TABLE,
                                    newInterfaceBtn));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Input Devices",
                                    INPUTDEVS_TABLE,
                                    newInputBtn));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Graphics Devices",
                                    GRAPHICS_TABLE,
                                    newGraphicsBtn));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Sound Devices",
                                    SOUND_TABLE,
                                    newSoundBtn));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Serial Devices",
                                    SERIAL_TABLE,
                                    newSerialBtn));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Parallel Devices",
                                    PARALLEL_TABLE,
                                    newParallelBtn));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Video Devices",
                                    VIDEO_TABLE,
                                    newVideoBtn));
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(
                              ClusterBrowser.SERVICE_LABEL_WIDTH
                              + ClusterBrowser.SERVICE_FIELD_WIDTH * 2 + 4));
        newPanel.add(new JScrollPane(mainPanel));
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                setApplyButtons(null, params);
            }
        });
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    /** Starts the domain. */
    void start(final Host host) {
        final boolean ret = VIRSH.start(host, getDomainName());
        if (ret) {
            int i = 0;
            try {
                mTransitionLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final boolean wasEmpty = starting.isEmpty();
            starting.add(host.getName());
            if (!wasEmpty) {
                mTransitionLock.release();
                return;
            }
            while (!starting.isEmpty() && i < ACTION_TIMEOUT) {
                mTransitionLock.release();
                getBrowser().periodicalVMSUpdate(host);
                updateParameters();
                getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
                Tools.sleep(1000);
                i++;
                try {
                    mTransitionLock.acquire();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (i >= ACTION_TIMEOUT) {
                Tools.appWarning("could not start on " + host.getName());
                starting.clear();
            }
            mTransitionLock.release();
            getBrowser().periodicalVMSUpdate(host);
            updateParameters();
            getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
        }
    }

    /** Starts shutting down indicator. */
    private void startShuttingdownIndicator(final Host host) {
        int i = 0;
        try {
            mTransitionLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final boolean wasEmpty = starting.isEmpty();
        shuttingdown.add(host.getName());
        if (!wasEmpty) {
            mTransitionLock.release();
            return;
        }
        while (!shuttingdown.isEmpty() && i < ACTION_TIMEOUT) {
            mTransitionLock.release();
            getBrowser().periodicalVMSUpdate(host);
            updateParameters();
            getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
            Tools.sleep(1000);
            i++;
            try {
                mTransitionLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (i >= ACTION_TIMEOUT) {
            Tools.appWarning("could not shut down on " + host.getName());
            shuttingdown.clear();
        }
        mTransitionLock.release();
        getBrowser().periodicalVMSUpdate(host);
        updateParameters();
        getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
    }

    /** Shuts down the domain. */
    void shutdown(final Host host) {
        final boolean ret = VIRSH.shutdown(host, getDomainName());
        if (ret) {
            startShuttingdownIndicator(host);
        }
    }

    /** Destroys down the domain. */
    void destroy(final Host host) {
        final boolean ret = VIRSH.destroy(host, getDomainName());
        if (ret) {
            startShuttingdownIndicator(host);
        }
    }

    /** Reboots the domain. */
    void reboot(final Host host) {
        final boolean ret = VIRSH.reboot(host, getDomainName());
        if (ret) {
            startShuttingdownIndicator(host);
        }
    }

    /** Suspend down the domain. */
    void suspend(final Host host) {
        final boolean ret = VIRSH.suspend(host, getDomainName());
        if (ret) {
            int i = 0;
            try {
                mTransitionLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final boolean wasEmpty = suspending.isEmpty();
            suspending.add(host.getName());
            if (!wasEmpty) {
                mTransitionLock.release();
                return;
            }
            while (!suspending.isEmpty() && i < ACTION_TIMEOUT) {
                mTransitionLock.release();
                getBrowser().periodicalVMSUpdate(host);
                updateParameters();
                getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
                Tools.sleep(1000);
                i++;
                try {
                    mTransitionLock.acquire();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (i >= ACTION_TIMEOUT) {
                Tools.appWarning("could not suspend on " + host.getName());
                suspending.clear();
            }
            mTransitionLock.release();
            getBrowser().periodicalVMSUpdate(host);
            updateParameters();
            getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
        }
    }

    /** Resume down the domain. */
    void resume(final Host host) {
        final boolean ret = VIRSH.resume(host, getDomainName());
        if (ret) {
            int i = 0;
            try {
                mTransitionLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final boolean wasEmpty = resuming.isEmpty();
            resuming.add(host.getName());
            if (!wasEmpty) {
                mTransitionLock.release();
                return;
            }
            while (!resuming.isEmpty() && i < ACTION_TIMEOUT) {
                mTransitionLock.release();
                getBrowser().periodicalVMSUpdate(host);
                updateParameters();
                getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
                Tools.sleep(1000);
                i++;
                try {
                    mTransitionLock.acquire();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (i >= ACTION_TIMEOUT) {
                Tools.appWarning("could not resume on " + host.getName());
                resuming.clear();
            }
            mTransitionLock.release();
            getBrowser().periodicalVMSUpdate(host);
            updateParameters();
            getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
        }
    }

    /** Adds new virtual disk. */
    public VMSDiskInfo addDiskPanel() {
        final VMSDiskInfo vmsdi = new VMSDiskInfo(null, getBrowser(), this);
        vmsdi.getResource().setNew(true);
        final DefaultMutableTreeNode resource =
                                           new DefaultMutableTreeNode(vmsdi);
        getBrowser().setNode(resource);
        final Enumeration eee = getNode().children();
        int i = 0;
        while (eee.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode) eee.nextElement();
            if (node.getUserObject() instanceof VMSDiskInfo) {
                i++;
                continue;
            }
            break;
        }
        getNode().insert(resource, i);
        getBrowser().reload(getNode(), true);
        vmsdi.selectMyself();
        return vmsdi;
    }

    /** Adds new virtual interface. */
    public VMSInterfaceInfo addInterfacePanel() {
        final VMSInterfaceInfo vmsii =
                            new VMSInterfaceInfo(null, getBrowser(), this);
        vmsii.getResource().setNew(true);
        final DefaultMutableTreeNode resource =
                                           new DefaultMutableTreeNode(vmsii);
        getBrowser().setNode(resource);
        final Enumeration eee = getNode().children();
        int i = 0;
        while (eee.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode) eee.nextElement();
            if (node.getUserObject() instanceof VMSDiskInfo
                || node.getUserObject() instanceof VMSInterfaceInfo) {
                i++;
                continue;
            }
            break;
        }

        getNode().insert(resource, i);
        getBrowser().reload(getNode(), true);
        vmsii.selectMyself();
        return vmsii;
    }

    /** Adds new virtual input device. */
    void addInputDevPanel() {
        final VMSInputDevInfo vmsidi =
                                 new VMSInputDevInfo(null, getBrowser(), this);
        vmsidi.getResource().setNew(true);
        final DefaultMutableTreeNode resource =
                                           new DefaultMutableTreeNode(vmsidi);
        getBrowser().setNode(resource);
        final Enumeration eee = getNode().children();
        int i = 0;
        while (eee.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode) eee.nextElement();
            if (node.getUserObject() instanceof VMSDiskInfo
                || node.getUserObject() instanceof VMSInterfaceInfo
                || node.getUserObject() instanceof VMSInputDevInfo) {
                i++;
                continue;
            }
            break;
        }

        getNode().insert(resource, i);
        getBrowser().reload(getNode(), true);
        vmsidi.selectMyself();
    }

    /** Adds new graphics device. */
    public VMSGraphicsInfo addGraphicsPanel() {
        final VMSGraphicsInfo vmsgi =
                            new VMSGraphicsInfo(null, getBrowser(), this);
        vmsgi.getResource().setNew(true);
        final DefaultMutableTreeNode resource =
                                           new DefaultMutableTreeNode(vmsgi);
        getBrowser().setNode(resource);
        final Enumeration eee = getNode().children();
        int i = 0;
        while (eee.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode) eee.nextElement();
            if (node.getUserObject() instanceof VMSDiskInfo
                || node.getUserObject() instanceof VMSInterfaceInfo
                || node.getUserObject() instanceof VMSInputDevInfo
                || node.getUserObject() instanceof VMSGraphicsInfo) {
                i++;
                continue;
            }
            break;
        }

        getNode().insert(resource, i);
        getBrowser().reload(getNode(), true);
        vmsgi.selectMyself();
        return vmsgi;
    }

    /** Adds new sound device. */
    void addSoundsPanel() {
        final VMSSoundInfo vmssi = new VMSSoundInfo(null, getBrowser(), this);
        vmssi.getResource().setNew(true);
        final DefaultMutableTreeNode resource =
                                           new DefaultMutableTreeNode(vmssi);
        getBrowser().setNode(resource);
        final Enumeration eee = getNode().children();
        int i = 0;
        while (eee.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode) eee.nextElement();
            if (node.getUserObject() instanceof VMSDiskInfo
                || node.getUserObject() instanceof VMSInterfaceInfo
                || node.getUserObject() instanceof VMSInputDevInfo
                || node.getUserObject() instanceof VMSGraphicsInfo
                || node.getUserObject() instanceof VMSSoundInfo) {
                i++;
                continue;
            }
            break;
        }

        getNode().insert(resource, i);
        getBrowser().reload(getNode(), true);
        vmssi.selectMyself();
    }

    /** Adds new serial device. */
    void addSerialsPanel() {
        final VMSSerialInfo vmssi = new VMSSerialInfo(null, getBrowser(), this);
        vmssi.getResource().setNew(true);
        final DefaultMutableTreeNode resource =
                                           new DefaultMutableTreeNode(vmssi);
        getBrowser().setNode(resource);
        final Enumeration eee = getNode().children();
        int i = 0;
        while (eee.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode) eee.nextElement();
            if (node.getUserObject() instanceof VMSDiskInfo
                || node.getUserObject() instanceof VMSInterfaceInfo
                || node.getUserObject() instanceof VMSInputDevInfo
                || node.getUserObject() instanceof VMSGraphicsInfo
                || node.getUserObject() instanceof VMSSoundInfo
                || node.getUserObject() instanceof VMSSerialInfo) {
                i++;
                continue;
            }
            break;
        }

        getNode().insert(resource, i);
        getBrowser().reload(getNode(), true);
        vmssi.selectMyself();
    }

    /** Adds new parallel device. */
    void addParallelsPanel() {
        final VMSParallelInfo vmspi =
                            new VMSParallelInfo(null, getBrowser(), this);
        vmspi.getResource().setNew(true);
        final DefaultMutableTreeNode resource =
                                           new DefaultMutableTreeNode(vmspi);
        getBrowser().setNode(resource);
        final Enumeration eee = getNode().children();
        int i = 0;
        while (eee.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode) eee.nextElement();
            if (node.getUserObject() instanceof VMSDiskInfo
                || node.getUserObject() instanceof VMSInterfaceInfo
                || node.getUserObject() instanceof VMSInputDevInfo
                || node.getUserObject() instanceof VMSGraphicsInfo
                || node.getUserObject() instanceof VMSSoundInfo
                || node.getUserObject() instanceof VMSSerialInfo
                || node.getUserObject() instanceof VMSParallelInfo) {
                i++;
                continue;
            }
            break;
        }

        getNode().insert(resource, i);
        getBrowser().reload(getNode(), true);
        vmspi.selectMyself();
    }

    /** Adds new video device. */
    void addVideosPanel() {
        final VMSVideoInfo vmsvi = new VMSVideoInfo(null, getBrowser(), this);
        vmsvi.getResource().setNew(true);
        final DefaultMutableTreeNode resource =
                                           new DefaultMutableTreeNode(vmsvi);
        getBrowser().setNode(resource);
        /* all the way till the end */
        getNode().add(resource);
        getBrowser().reload(getNode(), true);
        vmsvi.selectMyself();
    }

    /** Add new hardware. */
    private MyMenu getAddNewHardwareMenu(final String name) {
        return new MyMenu(name,
                          new AccessMode(ConfigData.AccessType.ADMIN, false),
                          new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override public String enablePredicate() {
                return null;
            }

            @Override public void update() {
                Tools.invokeAndWait(new Runnable() {
                    @Override public void run() {
                        removeAll();
                    }
                });
                final Point2D pos = getPos();
                /* disk */
                final MyMenuItem newDiskMenuItem = new MyMenuItem(
                   Tools.getString("VMSVirtualDomainInfo.AddNewDisk"),
                   BlockDevInfo.HARDDISK_ICON_LARGE,
                   new AccessMode(ConfigData.AccessType.ADMIN, false),
                   new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override public void action() {
                        hidePopup();
                        addDiskPanel();
                    }
                };
                newDiskMenuItem.setPos(pos);
                add(newDiskMenuItem);

                /* interface */
                final MyMenuItem newInterfaceMenuItem = new MyMenuItem(
                   Tools.getString("VMSVirtualDomainInfo.AddNewInterface"),
                   NetInfo.NET_I_ICON_LARGE,
                   new AccessMode(ConfigData.AccessType.ADMIN, false),
                   new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override public void action() {
                        hidePopup();
                        addInterfacePanel();
                    }
                };
                newInterfaceMenuItem.setPos(pos);
                add(newInterfaceMenuItem);

                /* graphics */
                final MyMenuItem newGraphicsMenuItem = new MyMenuItem(
                   Tools.getString("VMSVirtualDomainInfo.AddNewGraphics"),
                   VNC_ICON,
                   new AccessMode(ConfigData.AccessType.ADMIN, false),
                   new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override public void action() {
                        hidePopup();
                        addGraphicsPanel();
                    }
                };
                newGraphicsMenuItem.setPos(pos);
                add(newGraphicsMenuItem);


                /* input dev */
                final MyMenuItem newInputDevMenuItem = new MyMenuItem(
                   Tools.getString("VMSVirtualDomainInfo.AddNewInputDev"),
                   null,
                   new AccessMode(ConfigData.AccessType.ADMIN, false),
                   new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override public void action() {
                        hidePopup();
                        addInputDevPanel();
                    }
                };
                newInputDevMenuItem.setPos(pos);
                add(newInputDevMenuItem);

                /* sounds */
                final MyMenuItem newSoundsMenuItem = new MyMenuItem(
                   Tools.getString("VMSVirtualDomainInfo.AddNewSound"),
                   null,
                   new AccessMode(ConfigData.AccessType.ADMIN, false),
                   new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override public void action() {
                        hidePopup();
                        addSoundsPanel();
                    }
                };
                newSoundsMenuItem.setPos(pos);
                add(newSoundsMenuItem);

                /* serials */
                final MyMenuItem newSerialsMenuItem = new MyMenuItem(
                   Tools.getString("VMSVirtualDomainInfo.AddNewSerial"),
                   null,
                   new AccessMode(ConfigData.AccessType.ADMIN, false),
                   new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override public void action() {
                        hidePopup();
                        addSerialsPanel();
                    }
                };
                newSerialsMenuItem.setPos(pos);
                add(newSerialsMenuItem);

                /* parallels */
                final MyMenuItem newParallelsMenuItem = new MyMenuItem(
                   Tools.getString("VMSVirtualDomainInfo.AddNewParallel"),
                   null,
                   new AccessMode(ConfigData.AccessType.ADMIN, false),
                   new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override public void action() {
                        hidePopup();
                        addParallelsPanel();
                    }
                };
                newParallelsMenuItem.setPos(pos);
                add(newParallelsMenuItem);

                /* videos */
                final MyMenuItem newVideosMenuItem = new MyMenuItem(
                   Tools.getString("VMSVirtualDomainInfo.AddNewVideo"),
                   null,
                   new AccessMode(ConfigData.AccessType.ADMIN, true),
                   new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override public void action() {
                        hidePopup();
                        addVideosPanel();
                    }
                };
                newVideosMenuItem.setPos(pos);
                add(newVideosMenuItem);
                super.update();
            }
        };
    }

    /** Adds vm domain start menu item. */
    void addStartMenu(final List<UpdatableItem> items, final Host host) {
        final MyMenuItem startMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.StartOn")
                            + host.getName(),
                            HostBrowser.HOST_ON_ICON_LARGE,
                            Tools.getString("VMSVirtualDomainInfo.StartOn")
                            + host.getName(),
                            new AccessMode(ConfigData.AccessType.OP, false),
                            new AccessMode(ConfigData.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && !vmsxml.isRunning(getDomainName());
            }

            @Override public String enablePredicate() {
                if (!Tools.getConfigData().isAdvancedMode() && isUsedByCRM()) {
                    return IS_USED_BY_CRM_STRING;
                }
                return null;
            }

            @Override public void action() {
                hidePopup();
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && host != null) {
                    start(host);
                }
            }
        };
        items.add(startMenuItem);
    }

    /** Adds vm domain shutdown menu item. */
    void addShutdownMenu(final List<UpdatableItem> items, final Host host) {
        final MyMenuItem shutdownMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.ShutdownOn")
                            + host.getName(),
                            SHUTDOWN_ICON,
                            Tools.getString("VMSVirtualDomainInfo.ShutdownOn")
                            + host.getName(),
                            new AccessMode(ConfigData.AccessType.OP, false),
                            new AccessMode(ConfigData.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && vmsxml.isRunning(getDomainName());
            }

            @Override public String enablePredicate() {
                if (!Tools.getConfigData().isAdvancedMode() && isUsedByCRM()) {
                    return IS_USED_BY_CRM_STRING;
                }
                return null;
            }

            @Override public void action() {
                hidePopup();
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && host != null) {
                    shutdown(host);
                }
            }
        };
        items.add(shutdownMenuItem);
    }

    /** Adds vm domain reboot menu item. */
    void addRebootMenu(final List<UpdatableItem> items, final Host host) {
        final MyMenuItem rebootMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.RebootOn")
                            + host.getName(),
                            REBOOT_ICON,
                            Tools.getString("VMSVirtualDomainInfo.RebootOn")
                            + host.getName(),
                            new AccessMode(ConfigData.AccessType.OP, false),
                            new AccessMode(ConfigData.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && vmsxml.isRunning(getDomainName());
            }

            @Override public String enablePredicate() {
                if (!Tools.getConfigData().isAdvancedMode() && isUsedByCRM()) {
                    return IS_USED_BY_CRM_STRING;
                }
                return null;
            }

            @Override public void action() {
                hidePopup();
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && host != null) {
                    reboot(host);
                }
            }
        };
        items.add(rebootMenuItem);
    }

    /** Adds vm domain resume menu item. */
    void addResumeMenu(final List<UpdatableItem> items, final Host host) {
        final MyMenuItem resumeMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.ResumeOn")
                            + host.getName(),
                            RESUME_ICON,
                            Tools.getString("VMSVirtualDomainInfo.ResumeOn")
                            + host.getName(),
                            new AccessMode(ConfigData.AccessType.OP, false),
                            new AccessMode(ConfigData.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && vmsxml.isSuspended(getDomainName());
            }

            @Override public String enablePredicate() {
                if (getResource().isNew()) {
                    return NOT_APPLIED;
                }
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                if (vmsxml == null) {
                    return NO_VM_STATUS_STRING;
                }
                if (!vmsxml.isSuspended(getDomainName())) {
                    return "it is not suspended";
                }
                return null;
                //return vmsxml != null
                //       && vmsxml.isSuspended(getDomainName());
            }

            @Override public void action() {
                hidePopup();
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && host != null) {
                    resume(host);
                }
            }
        };
        items.add(resumeMenuItem);
    }


    /** Adds vm domain destroy menu item. */
    void addDestroyMenu(final List<UpdatableItem> items,
                        final Host host) {
        final MyMenuItem destroyMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.DestroyOn")
                            + host.getName(),
                            DESTROY_ICON,
                            Tools.getString("VMSVirtualDomainInfo.DestroyOn")
                            + host.getName(),
                            new AccessMode(ConfigData.AccessType.OP, false),
                            new AccessMode(ConfigData.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;


            @Override public String enablePredicate() {
                if (!Tools.getConfigData().isAdvancedMode() && isUsedByCRM()) {
                    return IS_USED_BY_CRM_STRING;
                }
                if (getResource().isNew()) {
                    return NOT_APPLIED;
                }
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                if (vmsxml == null
                    || !vmsxml.getDomainNames().contains(getDomainName())) {
                    return NO_VM_STATUS_STRING;
                }
                if (!vmsxml.isRunning(getDomainName())) {
                    return "not running";
                }
                return null;
            }

            @Override public void action() {
                hidePopup();
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && host != null) {
                    destroy(host);
                }
            }
        };
        items.add(destroyMenuItem);
    }

    /** Adds vm domain suspend menu item. */
    void addSuspendMenu(final MyMenu advancedSubmenu, final Host host) {
        final MyMenuItem suspendMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.SuspendOn")
                            + host.getName(),
                            PAUSE_ICON,
                            Tools.getString("VMSVirtualDomainInfo.SuspendOn")
                            + host.getName(),
                            new AccessMode(ConfigData.AccessType.OP, false),
                            new AccessMode(ConfigData.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override public String enablePredicate() {
                if (getResource().isNew()) {
                    return NOT_APPLIED;
                }
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                if (vmsxml == null
                    || !vmsxml.getDomainNames().contains(getDomainName())) {
                    return NO_VM_STATUS_STRING;
                }
                if (!vmsxml.isRunning(getDomainName())) {
                    return "not running";
                }
                if (vmsxml.isSuspended(getDomainName())) {
                    return "it is already suspended";
                }
                return null;
            }

            @Override public void action() {
                hidePopup();
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && host != null) {
                    suspend(host);
                }
            }
        };
        advancedSubmenu.add(suspendMenuItem);
    }

    /** Adds vm domain resume menu item. */
    void addResumeAdvancedMenu(final MyMenu advancedSubmenu, final Host host) {
        final MyMenuItem resumeMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.ResumeOn")
                            + host.getName(),
                            RESUME_ICON,
                            Tools.getString("VMSVirtualDomainInfo.ResumeOn")
                            + host.getName(),
                            new AccessMode(ConfigData.AccessType.OP, false),
                            new AccessMode(ConfigData.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override public String enablePredicate() {
                if (getResource().isNew()) {
                    return NOT_APPLIED;
                }
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                if (vmsxml == null
                    || !vmsxml.getDomainNames().contains(getDomainName())) {
                    return NO_VM_STATUS_STRING;
                }
                if (!vmsxml.isRunning(getDomainName())) {
                    return "not running";
                }
                if (!vmsxml.isSuspended(getDomainName())) {
                    return "it is not suspended";
                }
                return null;
            }

            @Override public void action() {
                hidePopup();
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && host != null) {
                    resume(host);
                }
            }
        };
        advancedSubmenu.add(resumeMenuItem);
    }

    /** Returns list of menu items for VM. */
    @Override public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        /* vnc viewers */
        for (final Host h : getBrowser().getClusterHosts()) {
            addVncViewersToTheMenu(items, h);
        }

        /* start */
        for (final Host h : getBrowser().getClusterHosts()) {
            addStartMenu(items, h);
        }

        /* shutdown */
        for (final Host h : getBrowser().getClusterHosts()) {
            addShutdownMenu(items, h);
        }

        /* reboot */
        for (final Host h : getBrowser().getClusterHosts()) {
            addRebootMenu(items, h);
        }

        /* destroy */
        for (final Host h : getBrowser().getClusterHosts()) {
            addDestroyMenu(items, h);
        }

        /* resume */
        for (final Host h : getBrowser().getClusterHosts()) {
            addResumeMenu(items, h);
        }
        items.add(getAddNewHardwareMenu(
                      Tools.getString("VMSVirtualDomainInfo.AddNewHardware")));

        /* advanced options */
        final MyMenu advancedSubmenu = new MyMenu(
                        Tools.getString("VMSVirtualDomainInfo.MoreOptions"),
                        new AccessMode(ConfigData.AccessType.OP, false),
                        new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;
            @Override public String enablePredicate() {
                return null;
            }
        };
        items.add(advancedSubmenu);

        /* suspend */
        for (final Host h : getBrowser().getClusterHosts()) {
            addSuspendMenu(advancedSubmenu, h);
        }

        /* resume */
        for (final Host h : getBrowser().getClusterHosts()) {
            addResumeAdvancedMenu(advancedSubmenu, h);
        }

        /* remove domain */
        final MyMenuItem removeMenuItem = new MyMenuItem(
                       Tools.getString("VMSVirtualDomainInfo.RemoveDomain"),
                       ClusterBrowser.REMOVE_ICON,
                       Tools.getString("VMSVirtualDomainInfo.RemoveDomain"),
                       Tools.getString("VMSVirtualDomainInfo.CancelDomain"),
                       ClusterBrowser.REMOVE_ICON,
                       Tools.getString("VMSVirtualDomainInfo.CancelDomain"),
                       new AccessMode(ConfigData.AccessType.ADMIN, false),
                       new AccessMode(ConfigData.AccessType.OP, false)) {
                            private static final long serialVersionUID = 1L;
                            @Override public boolean predicate() {
                                return !getResource().isNew();
                            }
                            @Override public String enablePredicate() {
                                if (!Tools.getConfigData().isAdvancedMode()
                                    && isUsedByCRM()) {
                                    return IS_USED_BY_CRM_STRING;
                                }
                                for (final Host host
                                           : getBrowser().getClusterHosts()) {
                                    final VMSXML vmsxml =
                                                getBrowser().getVMSXML(host);
                                    if (vmsxml == null) {
                                        continue;
                                    }
                                    if (vmsxml.isRunning(getDomainName())) {
                                        return "it is running";
                                    }
                                }
                                return null;
                            }

                            @Override public void action() {
                                hidePopup();
                                removeMyself(false);
                            }
        };
        items.add(removeMenuItem);
        return items;
    }

    /** Returns service icon in the menu. It can be started or stopped. */
    @Override public ImageIcon getMenuIcon(final boolean testOnly) {
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null && vmsxml.isRunning(getDomainName())) {
                return HostBrowser.HOST_ON_ICON;
            }
        }
        return HostBrowser.HOST_OFF_ICON;
    }

    /** Returns category icon. */
    @Override public ImageIcon getCategoryIcon(final boolean testOnly) {
        return getMenuIcon(testOnly);
    }

    /** Adds vnc viewer menu items. */
    void addVncViewersToTheMenu(final List<UpdatableItem> items,
                                final Host host) {
        final boolean testOnly = false;
        final VMSVirtualDomainInfo thisClass = this;
        if (Tools.getConfigData().isTightvnc()) {
            /* tight vnc test menu */
            final MyMenuItem tightvncViewerMenu = new MyMenuItem(
                            getVNCMenuString("TIGHT", host),
                            VNC_ICON,
                            getVNCMenuString("TIGHT", host),
                            new AccessMode(ConfigData.AccessType.RO, false),
                            new AccessMode(ConfigData.AccessType.RO, false)) {

                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    if (getResource().isNew()) {
                        return NOT_APPLIED;
                    }
                    final VMSXML vmsxml = getBrowser().getVMSXML(host);
                    if (vmsxml == null) {
                        return NO_VM_STATUS_STRING;
                    }
                    if (!vmsxml.isRunning(getDomainName())) {
                        return "not running";
                    }
                    return null;
                }

                @Override public void action() {
                    hidePopup();
                    final VMSXML vxml = getBrowser().getVMSXML(host);
                    if (vxml != null) {
                        final int remotePort = vxml.getRemotePort(
                                                            getDomainName());
                        final Host host = vxml.getHost();
                        if (host != null && remotePort > 0) {
                            Tools.startTightVncViewer(host, remotePort);
                        }
                    }
                }
            };
            items.add(tightvncViewerMenu);
        }

        if (Tools.getConfigData().isUltravnc()) {
            /* ultra vnc test menu */
            final MyMenuItem ultravncViewerMenu = new MyMenuItem(
                            getVNCMenuString("ULTRA", host),
                            VNC_ICON,
                            getVNCMenuString("ULTRA", host),
                            new AccessMode(ConfigData.AccessType.RO, false),
                            new AccessMode(ConfigData.AccessType.RO, false)) {

                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    if (getResource().isNew()) {
                        return NOT_APPLIED;
                    }
                    final VMSXML vmsxml = getBrowser().getVMSXML(host);
                    if (vmsxml == null) {
                        return NO_VM_STATUS_STRING;
                    }
                    if (!vmsxml.isRunning(getDomainName())) {
                        return "not running";
                    }
                    return null;
                }

                @Override public void action() {
                    hidePopup();
                    final VMSXML vxml = getBrowser().getVMSXML(host);
                    if (vxml != null) {
                        final int remotePort = vxml.getRemotePort(
                                                             getDomainName());
                        final Host host = vxml.getHost();
                        if (host != null && remotePort > 0) {
                            Tools.startUltraVncViewer(host, remotePort);
                        }
                    }
                }
            };
            items.add(ultravncViewerMenu);
        }

        if (Tools.getConfigData().isRealvnc()) {
            /* real vnc test menu */
            final MyMenuItem realvncViewerMenu = new MyMenuItem(
                            getVNCMenuString("REAL", host),
                            VNC_ICON,
                            getVNCMenuString("REAL", host),
                            new AccessMode(ConfigData.AccessType.RO, false),
                            new AccessMode(ConfigData.AccessType.RO, false)) {

                private static final long serialVersionUID = 1L;

                @Override public String enablePredicate() {
                    if (getResource().isNew()) {
                        return NOT_APPLIED;
                    }
                    final VMSXML vmsxml = getBrowser().getVMSXML(host);
                    if (vmsxml == null) {
                        return NO_VM_STATUS_STRING;
                    }
                    if (!vmsxml.isRunning(getDomainName())) {
                        return "not running";
                    }
                    return null;
                }

                @Override public void action() {
                    hidePopup();
                    final VMSXML vxml = getBrowser().getVMSXML(host);
                    if (vxml != null) {
                        final int remotePort = vxml.getRemotePort(
                                                            getDomainName());
                        final Host host = vxml.getHost();
                        if (host != null && remotePort > 0) {
                            Tools.startRealVncViewer(host, remotePort);
                        }
                    }
                }
            };
            items.add(realvncViewerMenu);
        }
    }

    /** Returns long description of the specified parameter. */
    @Override protected String getParamLongDesc(final String param) {
        return SHORTNAME_MAP.get(param);
    }

    /** Returns short description of the specified parameter. */
    @Override protected String getParamShortDesc(final String param) {
        return SHORTNAME_MAP.get(param);
    }

    /** Returns preferred value for specified parameter. */
    @Override protected String getParamPreferred(final String param) {
        return PREFERRED_MAP.get(param);
    }

    /** Returns default value for specified parameter. */
    @Override protected String getParamDefault(final String param) {
        return DEFAULTS_MAP.get(param);
    }

    /** Returns true if the value of the parameter is ok. */
    @Override protected boolean checkParam(final String param,
                                           final String newValue) {
        if (isRequired(param) && (newValue == null || "".equals(newValue))) {
            return false;
        }
        if (VMSXML.VM_PARAM_MEMORY.equals(param)) {
            final long mem = Tools.convertToKilobytes(newValue);
            if (mem < 4096) {
                return false;
            }
            final long curMem = Tools.convertToKilobytes(
                        getComboBoxValue(VMSXML.VM_PARAM_CURRENTMEMORY));
            if (mem < curMem) {
                return false;
            }
        } else if (VMSXML.VM_PARAM_CURRENTMEMORY.equals(param)) {
            final long curMem = Tools.convertToKilobytes(newValue);
            if (curMem < 4096) {
                return false;
            }
            final long mem = Tools.convertToKilobytes(
                             getComboBoxValue(VMSXML.VM_PARAM_MEMORY));
            if (mem < curMem) {
                paramComboBoxGet(VMSXML.VM_PARAM_MEMORY, null).setValue(
                                                                     newValue);
            }
        } else if (VMSXML.VM_PARAM_TYPE.equals(param)) {
            final GuiComboBox emCB = paramComboBoxGet(VMSXML.VM_PARAM_EMULATOR,
                                                      null);
            if (getResource().isNew()
                && !Tools.areEqual(emCB.getStringValue(), newValue)) {
                paramComboBoxGet(VMSXML.VM_PARAM_EMULATOR, null).setValue(
                                                                     newValue);
                if (Tools.areEqual("xen", newValue)) {
                    paramComboBoxGet(VMSXML.VM_PARAM_LOADER, null).setValue(
                                                "/usr/lib/xen/boot/hvmloader");
                } else {
                    paramComboBoxGet(VMSXML.VM_PARAM_LOADER, null).setValue("");
                }
            }
        } else if (VMSXML.VM_PARAM_CPU_MATCH.equals(param)) {
            final boolean match = !"".equals(newValue);
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    for (final String p : new String[]{
                                    VMSXML.VM_PARAM_CPUMATCH_MODEL,
                                    VMSXML.VM_PARAM_CPUMATCH_VENDOR,
                                    VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS,
                                    VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_CORES,
                                    VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS,
                                    VMSXML.VM_PARAM_CPUMATCH_FEATURE_POLICY,
                                    VMSXML.VM_PARAM_CPUMATCH_FEATURES}) {
                        paramComboBoxGet(p, null).setVisible(match);
                    }
                }
            });
        }
        return true;
    }

    /** Returns parameters. */
    @Override public String[] getParametersFromXML() {
        return VM_PARAMETERS;
    }

    /** Returns possible choices for drop down lists. */
    @Override protected Object[] getParamPossibleChoices(final String param) {
        if (VMSXML.VM_PARAM_CPUMATCH_MODEL.equals(param)) {
            final Set<String> models = new LinkedHashSet<String>();
            models.add("");
            for (final Host host : getBrowser().getClusterHosts()) {
                models.addAll(host.getCPUMapModels());
            }
            return models.toArray(new String[models.size()]);
        } else if (VMSXML.VM_PARAM_CPUMATCH_VENDOR.equals(param)) {
            final Set<String> vendors = new LinkedHashSet<String>();
            vendors.add("");
            for (final Host host : getBrowser().getClusterHosts()) {
                vendors.addAll(host.getCPUMapVendors());
            }
            return vendors.toArray(new String[vendors.size()]);
        }
        return POSSIBLE_VALUES.get(param);
    }

    /** Returns section to which the specified parameter belongs. */
    @Override protected String getSection(final String param) {
        return SECTION_MAP.get(param);
    }

    /** Returns true if the specified parameter is required. */
    @Override protected boolean isRequired(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is integer. */
    @Override protected boolean isInteger(final String param) {
        return IS_INTEGER.contains(param);
    }

    /** Returns true if the specified parameter is a label. */
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

    /** Returns type of the field. */
    @Override protected GuiComboBox.Type getFieldType(final String param) {
        return FIELD_TYPES.get(param);
    }

    /** Applies the changes. */
    public void apply(final boolean testOnly) {
        if (testOnly) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                getApplyButton().setEnabled(false);
            }
        });
        final String[] params = getParametersFromXML();
        final Map<String, String> parameters = new HashMap<String, String>();
        setName(getComboBoxValue(VMSXML.VM_PARAM_NAME));
        for (final String param : getParametersFromXML()) {
            final String value = getComboBoxValue(param);
            parameters.put(param, value);
            getResource().setValue(param, value);
        }
        final List<Host> definedOnHosts = new ArrayList<Host>();
        for (final Host host : getBrowser().getClusterHosts()) {
            final String value =
              definedOnHostComboBoxHash.get(host.getName()).getStringValue();
            if (DEFINED_ON_HOST_TRUE.equals(value)) {
                if (getResource().isNew()) {
                    final VMSXML vmsxml = new VMSXML(host);
                    getBrowser().vmsXMLPut(host, vmsxml);
                    final Node domainNode = vmsxml.createDomainXML(
                                                           getDomainName(),
                                                           parameters);
                    final Map<VMSHardwareInfo, Map<String, String>> allHWP =
                                                          getAllHWParameters();
                    for (final VMSHardwareInfo hi : allHWP.keySet()) {
                        hi.modifyXML(vmsxml,
                                     domainNode,
                                     getDomainName(),
                                     allHWP.get(hi));
                        hi.getResource().setNew(false);
                    }
                    vmsxml.saveAndDefine(domainNode, getDomainName());
                } else {
                    VMSXML vmsxml = getBrowser().getVMSXML(host);
                    if (vmsxml == null) {
                        vmsxml = new VMSXML(host);
                        getBrowser().vmsXMLPut(host, vmsxml);
                    }
                    Node domainNode;
                    if (vmsxml.getDomainNames().contains(getDomainName())) {
                        domainNode = vmsxml.modifyDomainXML(getDomainName(),
                                                            parameters);
                    } else {
                        domainNode = vmsxml.createDomainXML(getDomainName(),
                                                            parameters);
                    }
                    if (domainNode != null) {
                        final Enumeration eee = getNode().children();
                        while (eee.hasMoreElements()) {
                            final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                            final VMSHardwareInfo vmshi =
                                       (VMSHardwareInfo) node.getUserObject();
                            if (vmshi.checkResourceFieldsChanged(
                                            null,
                                            vmshi.getRealParametersFromXML(),
                                            true)) {
                                vmshi.modifyXML(vmsxml,
                                                domainNode,
                                                getDomainName(),
                                                vmshi.getHWParametersAndSave());
                                vmshi.getResource().setNew(false);
                                vmshi.setApplyButtons(
                                            null,
                                            vmshi.getRealParametersFromXML());
                            }
                        }
                        vmsxml.saveAndDefine(domainNode, getDomainName());
                    }
                }
                definedOnHosts.add(host);
            } else {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                if (vmsxml != null
                    && vmsxml.getDomainNames().contains(getDomainName())) {
                    VIRSH.undefine(host, getDomainName());
                }
            }
        }
        if (getResource().isNew()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    final Enumeration eee = getNode().children();
                    while (eee.hasMoreElements()) {
                        final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                        final VMSHardwareInfo vmshi =
                                        (VMSHardwareInfo) node.getUserObject();
                        vmshi.getApplyButton().setVisible(true);
                    }
                }
            });
        }
        VIRSH.setParameters(definedOnHosts.toArray(
                                      new Host[definedOnHosts.size()]),
                            getDomainName(),
                            parameters);
        getResource().setNew(false);
        for (final Host host : definedOnHosts) {
            getBrowser().periodicalVMSUpdate(host);
        }
        updateParameters();
        getBrowser().reload(getNode(), false);
        if (!testOnly) {
            setApplyButtons(null, getParametersFromXML());
        }
    }

    /** Returns parameters of all devices. */
    protected Map<VMSHardwareInfo, Map<String, String>> getAllHWParameters() {
        final Enumeration e = getNode().children();
        final Map<VMSHardwareInfo, Map<String, String>> allParamaters =
                         new TreeMap<VMSHardwareInfo, Map<String, String>>();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode) e.nextElement();
            final VMSHardwareInfo hi = (VMSHardwareInfo) node.getUserObject();
            allParamaters.put(hi, hi.getHWParametersAndSave());
        }
        return allParamaters;
    }

    /** Returns whether this parameter has a unit prefix. */
    @Override protected boolean hasUnitPrefix(final String param) {
        return HAS_UNIT_PREFIX.containsKey(param) && HAS_UNIT_PREFIX.get(param);
    }

    /** Returns units. */
    @Override protected Unit[] getUnits() {
        return new Unit[]{
                   //new Unit("", "", "KiByte", "KiBytes"), /* default unit */
                   new Unit("K", "K", "KiByte", "KiBytes"),
                   new Unit("M", "M", "MiByte", "MiBytes"),
                   new Unit("G",  "G",  "GiByte",      "GiBytes"),
                   new Unit("T",  "T",  "TiByte",      "TiBytes")
       };
    }

    /** Returns the default unit for the parameter. */
    protected String getDefaultUnit(final String param) {
        return DEFAULT_UNIT.get(param);
    }

    /** Returns HTML string on which hosts the vm is defined. */
    String getDefinedOnString() {
        return definedOnString;
    }

    /** Returns HTML string on which hosts the vm is running. */
    String getRunningOnString() {
        return runningOnString;
    }

    /** Returns columns for the table. */
    @Override protected String[] getColumnNames(final String tableName) {
        if (HEADER_TABLE.equals(tableName)) {
            return new String[]{"Name", "Defined on", "Status", "Memory", ""};
        } else if (DISK_TABLE.equals(tableName)) {
            return new String[]{"Virtual Device", "Source", ""};
        } else if (INTERFACES_TABLE.equals(tableName)) {
            return new String[]{"Virtual Interface", "Source", ""};
        } else if (INPUTDEVS_TABLE.equals(tableName)) {
            return new String[]{"Input Device", ""};
        } else if (GRAPHICS_TABLE.equals(tableName)) {
            return new String[]{"Graphic Display", ""};
        } else if (SOUND_TABLE.equals(tableName)) {
            return new String[]{"Sound Device", ""};
        } else if (SERIAL_TABLE.equals(tableName)) {
            return new String[]{"Serial Device", ""};
        } else if (PARALLEL_TABLE.equals(tableName)) {
            return new String[]{"Parallel Device", ""};
        } else if (VIDEO_TABLE.equals(tableName)) {
            return new String[]{"Video Device", ""};
        }
        return new String[]{};
    }

    /** Returns data for the table. */
    @Override protected Object[][] getTableData(final String tableName) {
        if (HEADER_TABLE.equals(tableName)) {
            return getMainTableData();
        } else if (DISK_TABLE.equals(tableName)) {
            return getDiskTableData();
        } else if (INTERFACES_TABLE.equals(tableName)) {
            return getInterfaceTableData();
        } else if (INPUTDEVS_TABLE.equals(tableName)) {
            return getInputDevTableData();
        } else if (GRAPHICS_TABLE.equals(tableName)) {
            return getGraphicsTableData();
        } else if (SOUND_TABLE.equals(tableName)) {
            return getSoundTableData();
        } else if (SERIAL_TABLE.equals(tableName)) {
            return getSerialTableData();
        } else if (PARALLEL_TABLE.equals(tableName)) {
            return getParallelTableData();
        } else if (VIDEO_TABLE.equals(tableName)) {
            return getVideoTableData();
        }
        return new Object[][]{};
    }

    /** Returns data for the main table. */
    protected Object[][] getMainTableData() {
        final List<Object[]> rows = new ArrayList<Object[]>();
        final String domainName = getDomainName();
        ImageIcon hostIcon = HostBrowser.HOST_OFF_ICON_LARGE;
        Color newColor = Browser.PANEL_BACKGROUND;
        for (final Host host : getBrowser().getClusterHosts()) {
            final VMSXML vxml = getBrowser().getVMSXML(host);
            if (vxml != null) {
                if (vxml.isRunning(domainName)) {
                    newColor = host.getPmColors()[0];
                    if (vxml.isSuspended(domainName)) {
                        hostIcon = PAUSE_ICON;
                    } else {
                        hostIcon = HostBrowser.HOST_ON_ICON_LARGE;
                    }
                }
                break;
            }
        }
        rowColor = newColor;
        if (domainName != null) {
            final MyButton domainNameLabel = new MyButton(domainName, hostIcon);
            domainNameLabel.setOpaque(true);
            final MyButton removeDomain = new MyButton(
                                               "Remove",
                                               ClusterBrowser.REMOVE_ICON_SMALL,
                                               "Remove " + domainName
                                               + " domain");
            removeDomain.miniButton();
            rows.add(new Object[]{domainNameLabel,
                                  getDefinedOnString(),
                                  getRunningOnString(),
                                  getResource().getValue("memory"),
                                  removeDomain});
        }
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Returns all disks. */
    protected Map<String, DiskData> getDisks() {
        Map<String, DiskData> disks = null;
        for (final Host host : getBrowser().getClusterHosts()) {
            final VMSXML vxml = getBrowser().getVMSXML(host);
            if (vxml != null) {
                disks = vxml.getDisks(getDomainName());
                break;
            }
        }
        return disks;
    }

    /** Get one row of the table. */
    protected Object[] getDiskDataRow(final String targetDev,
                                      final Map<String, VMSDiskInfo> dkti,
                                      final Map<String, DiskData> disks,
                                      final boolean opaque) {
        final MyButton removeBtn = new MyButton(
                                           "Remove",
                                           ClusterBrowser.REMOVE_ICON_SMALL,
                                           "Remove " + targetDev);
        removeBtn.miniButton();
        final DiskData diskData = disks.get(targetDev);
        if (diskData == null) {
            return new Object[]{targetDev,
                                "unknown",
                                removeBtn};
        }
        final StringBuffer target = new StringBuffer(10);
        target.append(diskData.getTargetBusType());
        target.append(" : /dev/");
        target.append(targetDev);
        if (dkti != null) {
            try {
                mDiskToInfoLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final VMSDiskInfo vdi = diskToInfo.get(targetDev);
            mDiskToInfoLock.release();
            dkti.put(target.toString(), vdi);
        }
        final MyButton targetDevLabel = new MyButton(
                                    target.toString(),
                                    BlockDevInfo.HARDDISK_ICON_LARGE);
        targetDevLabel.setOpaque(opaque);
        final StringBuffer source = new StringBuffer(20);
        String s = diskData.getSourceDev();
        if (s == null) {
            s = diskData.getSourceFile();
        }
        if (s != null) {
            source.append(diskData.getType());
            source.append(" : ");
            source.append(s);
        }
        return new Object[]{targetDevLabel,
                            source.toString(),
                            removeBtn};
    }

    /** Returns all hosts on which this domain is defined. */
    List<Host> getDefinedOnHosts() {
        final List<Host> definedOn = new ArrayList<Host>();
        for (final Host h : getBrowser().getClusterHosts()) {
            if (getResource().isNew()) {
                final String value =
                  definedOnHostComboBoxHash.get(h.getName()).getStringValue();
                if (DEFINED_ON_HOST_TRUE.equals(value)) {
                    definedOn.add(h);
                }
            } else {
                final VMSXML vmsxml = getBrowser().getVMSXML(h);
                if (vmsxml != null
                    && vmsxml.getDomainNames().contains(getDomainName())) {
                    definedOn.add(h);
                }
            }
        }
        return definedOn;
    }

    /** Returns data for the disk table. */
    private Object[][] getDiskTableData() {
        final List<Object[]> rows = new ArrayList<Object[]>();
        final Map<String, DiskData> disks = getDisks();
        final Map<String, VMSDiskInfo> dkti =
                                          new HashMap<String, VMSDiskInfo>();
        if (disks != null && !disks.isEmpty()) {
            for (final String targetDev : disks.keySet()) {
                final Object[] row =
                                  getDiskDataRow(targetDev, dkti, disks, false);
                rows.add(row);
            }
        }
        try {
            mDiskToInfoLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        diskKeyToInfo = dkti;
        mDiskToInfoLock.release();
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Get one row of the table. */
    protected Object[] getInterfaceDataRow(
                                final String mac,
                                final Map<String, VMSInterfaceInfo> iToInfo,
                                final Map<String, InterfaceData> interfaces,
                                final boolean opaque) {
        final MyButton removeBtn = new MyButton(
                                           "Remove",
                                           ClusterBrowser.REMOVE_ICON_SMALL,
                                           "Remove " + mac);
        removeBtn.miniButton();
        final InterfaceData interfaceData = interfaces.get(mac);
        if (interfaceData == null) {
            return new Object[]{mac, "unknown", removeBtn};
        }
        final StringBuffer interf = new StringBuffer(20);
        interf.append(mac);
        final String dev = interfaceData.getTargetDev();
        if (dev != null) {
            interf.append(' ');
            interf.append(dev);
        }
        if (iToInfo != null) {
            final VMSInterfaceInfo vii = interfaceToInfo.get(mac);
            iToInfo.put(interf.toString(), vii);
        }
        final MyButton iLabel = new MyButton(interf.toString(),
                                             NetInfo.NET_I_ICON_LARGE);
        iLabel.setOpaque(opaque);
        final StringBuffer source = new StringBuffer(20);
        final String type = interfaceData.getType();
        String s;
        if ("network".equals(type)) {
            s = interfaceData.getSourceNetwork();
        } else {
            s = interfaceData.getSourceBridge();
        }
        if (s != null) {
            source.append(type);
            source.append(" : ");
            source.append(s);
        }
        return new Object[]{iLabel, source.toString(), removeBtn};
    }

    /** Get one row of the table. */
    protected Object[] getInputDevDataRow(
                                final String index,
                                final Map<String, VMSInputDevInfo> iToInfo,
                                final Map<String, InputDevData> inputDevs,
                                final boolean opaque) {
        final MyButton removeBtn = new MyButton(
                                           "Remove",
                                           ClusterBrowser.REMOVE_ICON_SMALL,
                                           "Remove " + index);
        removeBtn.miniButton();
        final InputDevData inputDevData = inputDevs.get(index);
        if (inputDevData == null) {
            return new Object[]{index + ": unknown", "", removeBtn};
        }
        if (iToInfo != null) {
            final VMSInputDevInfo vidi = inputDevToInfo.get(index);
            iToInfo.put(index, vidi);
        }
        final MyButton iLabel = new MyButton(index, null);
        iLabel.setOpaque(opaque);
        return new Object[]{iLabel, removeBtn};
    }

    /** Get one row of the table. */
    protected Object[] getGraphicsDataRow(
                                final String index,
                                final Map<String, VMSGraphicsInfo> iToInfo,
                                final Map<String, GraphicsData> graphicDisplays,
                                final boolean opaque) {
        final MyButton removeBtn = new MyButton(
                                           "Remove",
                                           ClusterBrowser.REMOVE_ICON_SMALL,
                                           "Remove " + index);
        removeBtn.miniButton();
        final GraphicsData graphicsData = graphicDisplays.get(index);
        if (graphicsData == null) {
            return new Object[]{index + ": unknown", "", removeBtn};
        }
        final String type = graphicsData.getType();
        if (iToInfo != null) {
            final VMSGraphicsInfo vidi = graphicsToInfo.get(index);
            iToInfo.put(index, vidi);
        }
        final MyButton iLabel = new MyButton(index, VNC_ICON);
        iLabel.setOpaque(opaque);
        return new Object[]{iLabel, removeBtn};
    }

    /** Get one row of the table. */
    protected Object[] getSoundDataRow(
                                final String index,
                                final Map<String, VMSSoundInfo> iToInfo,
                                final Map<String, SoundData> sounds,
                                final boolean opaque) {
        final MyButton removeBtn = new MyButton(
                                           "Remove",
                                           ClusterBrowser.REMOVE_ICON_SMALL,
                                           "Remove " + index);
        removeBtn.miniButton();
        final SoundData soundData = sounds.get(index);
        if (soundData == null) {
            return new Object[]{index + ": unknown", "", removeBtn};
        }
        final String model = soundData.getModel();
        if (iToInfo != null) {
            final VMSSoundInfo vidi = soundToInfo.get(index);
            iToInfo.put(index, vidi);
        }
        final MyButton iLabel = new MyButton(model, null);
        iLabel.setOpaque(opaque);
        return new Object[]{iLabel, removeBtn};
    }

    /** Get one row of the table. */
    protected Object[] getSerialDataRow(
                                final String index,
                                final Map<String, VMSSerialInfo> iToInfo,
                                final Map<String, SerialData> serials,
                                final boolean opaque) {
        final MyButton removeBtn = new MyButton(
                                           "Remove",
                                           ClusterBrowser.REMOVE_ICON_SMALL,
                                           "Remove " + index);
        removeBtn.miniButton();
        final SerialData serialData = serials.get(index);
        if (serialData == null) {
            return new Object[]{index + ": unknown", "", removeBtn};
        }
        final String type = serialData.getType();
        if (iToInfo != null) {
            final VMSSerialInfo vidi = serialToInfo.get(index);
            iToInfo.put(index, vidi);
        }
        final MyButton iLabel = new MyButton(index, null);
        iLabel.setOpaque(opaque);
        return new Object[]{iLabel, removeBtn};
    }

    /** Get one row of the table. */
    protected Object[] getParallelDataRow(
                                final String index,
                                final Map<String, VMSParallelInfo> iToInfo,
                                final Map<String, ParallelData> parallels,
                                final boolean opaque) {
        final MyButton removeBtn = new MyButton(
                                           "Remove",
                                           ClusterBrowser.REMOVE_ICON_SMALL,
                                           "Remove " + index);
        removeBtn.miniButton();
        final ParallelData parallelData = parallels.get(index);
        if (parallelData == null) {
            return new Object[]{index + ": unknown", "", removeBtn};
        }
        final String type = parallelData.getType();
        if (iToInfo != null) {
            final VMSParallelInfo vidi = parallelToInfo.get(index);
            iToInfo.put(index, vidi);
        }
        final MyButton iLabel = new MyButton(index, null);
        iLabel.setOpaque(opaque);
        return new Object[]{iLabel, removeBtn};
    }

    /** Get one row of the table. */
    protected Object[] getVideoDataRow(
                                final String index,
                                final Map<String, VMSVideoInfo> iToInfo,
                                final Map<String, VideoData> videos,
                                final boolean opaque) {
        final MyButton removeBtn = new MyButton(
                                           "Remove",
                                           ClusterBrowser.REMOVE_ICON_SMALL,
                                           "Remove " + index);
        removeBtn.miniButton();
        final VideoData videoData = videos.get(index);
        if (videoData == null) {
            return new Object[]{index + ": unknown", "", removeBtn};
        }
        final String modelType = videoData.getModelType();
        if (iToInfo != null) {
            final VMSVideoInfo vidi = videoToInfo.get(index);
            iToInfo.put(index, vidi);
        }
        final MyButton iLabel = new MyButton(modelType, null);
        iLabel.setOpaque(opaque);
        return new Object[]{iLabel, removeBtn};
    }


    /** Returns all interfaces. */
    protected Map<String, InterfaceData> getInterfaces() {
        Map<String, InterfaceData> interfaces = null;
        for (final Host host : getBrowser().getClusterHosts()) {
            final VMSXML vxml = getBrowser().getVMSXML(host);
            if (vxml != null) {
                interfaces = vxml.getInterfaces(getDomainName());
                break;
            }
        }
        return interfaces;
    }

    /** Returns data for the input devices table. */
    private Object[][] getInputDevTableData() {
        final List<Object[]> rows = new ArrayList<Object[]>();
        final Map<String, InputDevData> inputDevs = getInputDevs();
        final Map<String, VMSInputDevInfo> iToInfo =
                                      new HashMap<String, VMSInputDevInfo>();
        if (inputDevs != null) {
            for (final String index : inputDevs.keySet()) {
                final Object[] row = getInputDevDataRow(index,
                                                        iToInfo,
                                                        inputDevs,
                                                        false);
                rows.add(row);
            }
        }
        try {
            mInputDevToInfoLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        inputDevKeyToInfo = iToInfo;
        mInputDevToInfoLock.release();
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Returns data for the graphics devices table. */
    private Object[][] getGraphicsTableData() {
        final List<Object[]> rows = new ArrayList<Object[]>();
        final Map<String, GraphicsData> graphicDisplays = getGraphicDisplays();
        final Map<String, VMSGraphicsInfo> iToInfo =
                                      new HashMap<String, VMSGraphicsInfo>();
        if (graphicDisplays != null) {
            for (final String index : graphicDisplays.keySet()) {
                final Object[] row = getGraphicsDataRow(index,
                                                     iToInfo,
                                                     graphicDisplays,
                                                     false);
                rows.add(row);
            }
        }
        try {
            mGraphicsToInfoLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        graphicsKeyToInfo = iToInfo;
        mGraphicsToInfoLock.release();
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Returns data for the sound devices table. */
    private Object[][] getSoundTableData() {
        final List<Object[]> rows = new ArrayList<Object[]>();
        final Map<String, SoundData> sounds = getSounds();
        final Map<String, VMSSoundInfo> iToInfo =
                                      new HashMap<String, VMSSoundInfo>();
        if (sounds != null) {
            for (final String index : sounds.keySet()) {
                final Object[] row = getSoundDataRow(index,
                                                     iToInfo,
                                                     sounds,
                                                     false);
                rows.add(row);
            }
        }
        try {
            mSoundToInfoLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        soundKeyToInfo = iToInfo;
        mSoundToInfoLock.release();
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Returns data for the serial devices table. */
    private Object[][] getSerialTableData() {
        final List<Object[]> rows = new ArrayList<Object[]>();
        final Map<String, SerialData> serials = getSerials();
        final Map<String, VMSSerialInfo> iToInfo =
                                      new HashMap<String, VMSSerialInfo>();
        if (serials != null) {
            for (final String index : serials.keySet()) {
                final Object[] row = getSerialDataRow(index,
                                                      iToInfo,
                                                      serials,
                                                      false);
                rows.add(row);
            }
        }
        try {
            mSerialToInfoLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        serialKeyToInfo = iToInfo;
        mSerialToInfoLock.release();
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Returns data for the parallel devices table. */
    private Object[][] getParallelTableData() {
        final List<Object[]> rows = new ArrayList<Object[]>();
        final Map<String, ParallelData> parallels = getParallels();
        final Map<String, VMSParallelInfo> iToInfo =
                                      new HashMap<String, VMSParallelInfo>();
        if (parallels != null) {
            for (final String index : parallels.keySet()) {
                final Object[] row = getParallelDataRow(index,
                                                        iToInfo,
                                                        parallels,
                                                        false);
                rows.add(row);
            }
        }
        try {
            mParallelToInfoLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        parallelKeyToInfo = iToInfo;
        mParallelToInfoLock.release();
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Returns data for the video devices table. */
    private Object[][] getVideoTableData() {
        final List<Object[]> rows = new ArrayList<Object[]>();
        final Map<String, VideoData> videos = getVideos();
        final Map<String, VMSVideoInfo> iToInfo =
                                      new HashMap<String, VMSVideoInfo>();
        if (videos != null) {
            for (final String index : videos.keySet()) {
                final Object[] row = getVideoDataRow(index,
                                                     iToInfo,
                                                     videos,
                                                     false);
                rows.add(row);
            }
        }
        try {
            mVideoToInfoLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        videoKeyToInfo = iToInfo;
        mVideoToInfoLock.release();
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Returns all input devices. */
    protected Map<String, InputDevData> getInputDevs() {
        Map<String, InputDevData> inputDevs = null;
        for (final Host host : getBrowser().getClusterHosts()) {
            final VMSXML vxml = getBrowser().getVMSXML(host);
            if (vxml != null) {
                inputDevs = vxml.getInputDevs(getDomainName());
                break;
            }
        }
        return inputDevs;
    }

    /** Returns all graphics devices. */
    protected Map<String, GraphicsData> getGraphicDisplays() {
        Map<String, GraphicsData> graphicDisplays = null;
        for (final Host host : getBrowser().getClusterHosts()) {
            final VMSXML vxml = getBrowser().getVMSXML(host);
            if (vxml != null) {
                graphicDisplays = vxml.getGraphicDisplays(getDomainName());
                break;
            }
        }
        return graphicDisplays;
    }

    /** Returns all sound devices. */
    protected Map<String, SoundData> getSounds() {
        Map<String, SoundData> sounds = null;
        for (final Host host : getBrowser().getClusterHosts()) {
            final VMSXML vxml = getBrowser().getVMSXML(host);
            if (vxml != null) {
                sounds = vxml.getSounds(getDomainName());
                break;
            }
        }
        return sounds;
    }

    /** Returns all serial devices. */
    protected Map<String, SerialData> getSerials() {
        Map<String, SerialData> serials = null;
        for (final Host host : getBrowser().getClusterHosts()) {
            final VMSXML vxml = getBrowser().getVMSXML(host);
            if (vxml != null) {
                serials = vxml.getSerials(getDomainName());
                break;
            }
        }
        return serials;
    }

    /** Returns all parallel devices. */
    protected Map<String, ParallelData> getParallels() {
        Map<String, ParallelData> parallels = null;
        for (final Host host : getBrowser().getClusterHosts()) {
            final VMSXML vxml = getBrowser().getVMSXML(host);
            if (vxml != null) {
                parallels = vxml.getParallels(getDomainName());
                break;
            }
        }
        return parallels;
    }

    /** Returns all video devices. */
    protected Map<String, VideoData> getVideos() {
        Map<String, VideoData> videos = null;
        for (final Host host : getBrowser().getClusterHosts()) {
            final VMSXML vxml = getBrowser().getVMSXML(host);
            if (vxml != null) {
                videos = vxml.getVideos(getDomainName());
                break;
            }
        }
        return videos;
    }

    /** Returns data for the interface table. */
    private Object[][] getInterfaceTableData() {
        final List<Object[]> rows = new ArrayList<Object[]>();
        final Map<String, InterfaceData> interfaces = getInterfaces();
        final Map<String, VMSInterfaceInfo> iToInfo =
                                      new HashMap<String, VMSInterfaceInfo>();
        if (interfaces != null) {
            for (final String mac : interfaces.keySet()) {
                final Object[] row = getInterfaceDataRow(mac,
                                                         iToInfo,
                                                         interfaces,
                                                         false);
                rows.add(row);
            }
        }
        try {
            mInterfaceToInfoLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        interfaceKeyToInfo = iToInfo;
        mInterfaceToInfoLock.release();
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Execute when row in the table was clicked. */
    @Override protected void rowClicked(final String tableName,
                                        final String key,
                                        final int column) {
        if (HEADER_TABLE.equals(tableName)) {
            final Thread thread = new Thread(new Runnable() {
                @Override public void run() {
                    if (HEADER_DEFAULT_WIDTHS.containsKey(column)) {
                        removeMyself(false);
                    } else {
                        getBrowser().getVMSInfo().selectMyself();
                    }
                }
            });
            thread.start();
        } else if (DISK_TABLE.equals(tableName)) {
            try {
                mDiskToInfoLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final VMSDiskInfo vdi = diskKeyToInfo.get(key);
            mDiskToInfoLock.release();
            if (vdi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override public void run() {
                        if (DISK_DEFAULT_WIDTHS.containsKey(column)) {
                            vdi.removeMyself(false);
                        } else {
                            vdi.selectMyself();
                        }
                    }
                });
                thread.start();
            }
        } else if (INTERFACES_TABLE.equals(tableName)) {
            try {
                mInterfaceToInfoLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final VMSInterfaceInfo vii = interfaceKeyToInfo.get(key);
            mInterfaceToInfoLock.release();
            if (vii != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override public void run() {
                        if (INTERFACES_DEFAULT_WIDTHS.containsKey(column)) {
                            vii.removeMyself(false);
                        } else {
                            vii.selectMyself();
                        }
                    }
                });
                thread.start();
            }
        } else if (INPUTDEVS_TABLE.equals(tableName)) {
            try {
                mInputDevToInfoLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final VMSInputDevInfo vidi = inputDevKeyToInfo.get(key);
            mInputDevToInfoLock.release();
            if (vidi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override public void run() {
                        if (INPUTDEVS_DEFAULT_WIDTHS.containsKey(column)) {
                            vidi.removeMyself(false);
                        } else {
                            vidi.selectMyself();
                        }
                    }
                });
                thread.start();
            }
        } else if (GRAPHICS_TABLE.equals(tableName)) {
            try {
                mGraphicsToInfoLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final VMSGraphicsInfo vgi = graphicsKeyToInfo.get(key);
            mGraphicsToInfoLock.release();
            if (vgi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override public void run() {
                        if (GRAPHICS_DEFAULT_WIDTHS.containsKey(column)) {
                            vgi.removeMyself(false);
                        } else {
                            vgi.selectMyself();
                        }
                    }
                });
                thread.start();
            }
        } else if (SOUND_TABLE.equals(tableName)) {
            try {
                mSoundToInfoLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final VMSSoundInfo vsi = soundKeyToInfo.get(key);
            mSoundToInfoLock.release();
            if (vsi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override public void run() {
                        if (SOUND_DEFAULT_WIDTHS.containsKey(column)) {
                            vsi.removeMyself(false);
                        } else {
                            vsi.selectMyself();
                        }
                    }
                });
                thread.start();
            }
        } else if (SERIAL_TABLE.equals(tableName)) {
            try {
                mSerialToInfoLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final VMSSerialInfo vsi = serialKeyToInfo.get(key);
            mSerialToInfoLock.release();
            if (vsi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override public void run() {
                        if (SERIAL_DEFAULT_WIDTHS.containsKey(column)) {
                            vsi.removeMyself(false);
                        } else {
                            vsi.selectMyself();
                        }
                    }
                });
                thread.start();
            }
        } else if (PARALLEL_TABLE.equals(tableName)) {
            try {
                mParallelToInfoLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final VMSParallelInfo vpi = parallelKeyToInfo.get(key);
            mParallelToInfoLock.release();
            if (vpi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override public void run() {
                        if (PARALLEL_DEFAULT_WIDTHS.containsKey(column)) {
                            vpi.removeMyself(false);
                        } else {
                            vpi.selectMyself();
                        }
                    }
                });
                thread.start();
            }
        } else if (VIDEO_TABLE.equals(tableName)) {
            try {
                mVideoToInfoLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final VMSVideoInfo vvi = videoKeyToInfo.get(key);
            mVideoToInfoLock.release();
            if (vvi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override public void run() {
                        if (VIDEO_DEFAULT_WIDTHS.containsKey(column)) {
                            vvi.removeMyself(false);
                        } else {
                            vvi.selectMyself();
                        }
                    }
                });
                thread.start();
            }
        }
    }

    /** Retrurns color for some rows. */
    @Override protected Color getTableRowColor(final String tableName,
                                               final String key) {
        if (HEADER_TABLE.equals(tableName)) {
            return rowColor;
        }
        return Browser.PANEL_BACKGROUND;
    }

    /** Alignment for the specified column. */
    @Override protected int getTableColumnAlignment(final String tableName,
                                                    final int column) {

        if (column == 3 && HEADER_TABLE.equals(tableName)) {
            return SwingConstants.RIGHT;
        }
        return SwingConstants.LEFT;
    }

    /** Returns info object for this row. */
    @Override protected Info getTableInfo(final String tableName,
                                          final String key) {
        if (HEADER_TABLE.equals(tableName)) {
            return this;
        } else if (DISK_TABLE.equals(tableName)) {
            try {
                mDiskToInfoLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final Info info = diskToInfo.get(key);
            mDiskToInfoLock.release();
            return info;
        } else if (INTERFACES_TABLE.equals(tableName)) {
            return interfaceToInfo.get(key);
        } else if (INPUTDEVS_TABLE.equals(tableName)) {
            return inputDevToInfo.get(key);
        } else if (GRAPHICS_TABLE.equals(tableName)) {
            return graphicsToInfo.get(key);
        } else if (SOUND_TABLE.equals(tableName)) {
            return soundToInfo.get(key);
        } else if (SERIAL_TABLE.equals(tableName)) {
            return serialToInfo.get(key);
        } else if (PARALLEL_TABLE.equals(tableName)) {
            return parallelToInfo.get(key);
        } else if (VIDEO_TABLE.equals(tableName)) {
            return videoToInfo.get(key);
        }
        return null;
    }

    /** Returns whether the devices exists. */
    protected boolean isDevice(final String dev) {
        try {
            mDiskToInfoLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        final boolean is = diskToInfo.containsKey(dev);
        mDiskToInfoLock.release();
        return is;
    }

    /** Returns whether this parameter is advanced. */
    @Override protected boolean isAdvanced(final String param) {
        if (!getResource().isNew() && VMSXML.VM_PARAM_NAME.equals(param)) {
            return true;
        }
        return IS_ADVANCED.contains(param);
    }

    /** Whether the parameter should be enabled. */
    @Override protected String isEnabled(final String param) {
        final String libvirtVersion =
                            getBrowser().getCluster().getMinLibvirtVersion();
        if (!getResource().isNew() && VMSXML.VM_PARAM_NAME.equals(param)) {
            return "";
        }
        if (REQUIRED_VERSION.containsKey(param)) {
            final String rv = REQUIRED_VERSION.get(param);
            if (Tools.compareVersions(rv, libvirtVersion) > 0) {
                return Tools.getString(
                                "VMSVirtualDomainInfo.AvailableInVersion")
                                    .replace("@VERSION@", rv);
            }
        }
        return null;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override protected boolean isEnabledOnlyInAdvancedMode(
                                                         final String param) {
         if (VMSXML.VM_PARAM_MEMORY.equals(param)) {
             return true;
         }
         return false;
    }


    /** Returns access type of this parameter. */
    @Override protected ConfigData.AccessType getAccessType(
                                                         final String param) {
        return ConfigData.AccessType.ADMIN;
    }

    /** Returns the regexp of the parameter. */
    @Override protected String getParamRegexp(final String param) {
        if (VMSXML.VM_PARAM_NAME.equals(param)) {
            return "^[\\w-]*$";
        } else {
            return super.getParamRegexp(param);
        }
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed.
     */
    @Override public boolean checkResourceFieldsChanged(final String param,
                                                        final String[] params) {
        boolean changed = false;
        for (final Host host : getBrowser().getClusterHosts()) {
            if (!definedOnHostComboBoxHash.containsKey(host.getName())) {
                continue;
            }
            final String value =
                definedOnHostComboBoxHash.get(host.getName()).getStringValue();
            final VMSXML vmsxml = getBrowser().getVMSXML(host);
            if ((vmsxml == null
                 || (!getResource().isNew()
                     && !vmsxml.getDomainNames().contains(getDomainName())))
                && DEFINED_ON_HOST_TRUE.equals(value)) {
                changed = true;
            } else if (vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && DEFINED_ON_HOST_FALSE.equals(value)) {
                changed = true;
            }
        }
        final Enumeration eee = getNode().children();
        while (eee.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode) eee.nextElement();
            final VMSHardwareInfo vmshi =
                            (VMSHardwareInfo) node.getUserObject();
            if (vmshi.checkResourceFieldsChanged(
                                             null,
                                             vmshi.getRealParametersFromXML(),
                                             true)) {
                changed = true;
            }
        }
        final boolean ch = super.checkResourceFieldsChanged(param, params)
                           || changed;
        return ch;
    }

    /** Returns whether all the parameters are correct. */
    @Override public boolean checkResourceFieldsCorrect(final String param,
                                                        final String[] params) {
        boolean cor = false;
        for (final Host host : getBrowser().getClusterHosts()) {
            if (!definedOnHostComboBoxHash.containsKey(host.getName())) {
                continue;
            }
            final GuiComboBox hostCB = definedOnHostComboBoxHash.get(
                                                               host.getName());
            final GuiComboBox wizardHostCB = definedOnHostComboBoxHash.get(
                                               WIZARD_PREFIX + host.getName());
            final String value = hostCB.getStringValue();
            String savedValue;
            final VMSXML vmsxml = getBrowser().getVMSXML(host);
            if (vmsxml != null
                && vmsxml.getDomainNames().contains(getDomainName())) {
                savedValue = DEFINED_ON_HOST_TRUE;
            } else {
                savedValue = DEFINED_ON_HOST_FALSE;
            }
            hostCB.setBackground(value, savedValue, false);
            if (wizardHostCB != null) {
                wizardHostCB.setBackground(value, savedValue, false);
            }
            if (DEFINED_ON_HOST_TRUE.equals(value)) {
                cor = true; /* at least one */
            }
        }
        if (!cor) {
            for (final String key : definedOnHostComboBoxHash.keySet()) {
                definedOnHostComboBoxHash.get(key).wrongValue();
            }
        }
        final Enumeration eee = getNode().children();
        while (eee.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode) eee.nextElement();
            final VMSHardwareInfo vmshi =
                            (VMSHardwareInfo) node.getUserObject();
            if (!vmshi.checkResourceFieldsCorrect(
                                              null,
                                              vmshi.getRealParametersFromXML(),
                                              true)) {
                cor = false;
            }
        }
        return super.checkResourceFieldsCorrect(null, params) && cor;
    }

    /** Returns combo box for parameter. */
    @Override protected GuiComboBox getParamComboBox(final String param,
                                                     final String prefix,
                                                     final int width) {
        final GuiComboBox paramCB =
                                 super.getParamComboBox(param, prefix, width);
        if (VMSXML.VM_PARAM_BOOT.equals(param)) {
            paramCB.setAlwaysEditable(false);
        }
        return paramCB;
    }

    /** Removes this domain. */
    @Override public void removeMyself(final boolean testOnly) {
        if (getResource().isNew()) {
            super.removeMyself(testOnly);
            getResource().setNew(false);
            return;
        }
        String desc = Tools.getString(
                            "VMSVirtualDomainInfo.confirmRemove.Description");

        desc  = desc.replaceAll("@DOMAIN@", getDomainName());
        if (Tools.confirmDialog(
               Tools.getString("VMSVirtualDomainInfo.confirmRemove.Title"),
               desc,
               Tools.getString("VMSVirtualDomainInfo.confirmRemove.Yes"),
               Tools.getString("VMSVirtualDomainInfo.confirmRemove.No"))) {
            removeMyselfNoConfirm(testOnly);
            getResource().setNew(false);
        }
    }

    /** Removes this virtual domain without confirmation dialog. */
    protected void removeMyselfNoConfirm(final boolean testOnly) {
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null
                && vmsxml.getDomainNames().contains(getDomainName())) {
                VIRSH.undefine(h, getDomainName());
            }
        }
        for (final Host h : getBrowser().getClusterHosts()) {
            getBrowser().periodicalVMSUpdate(h);
        }
    }

    /** Returns whether the column is a button, 0 column is always a button. */
    @Override protected Map<Integer, Integer> getDefaultWidths(
                                                    final String tableName) {
        if (HEADER_TABLE.equals(tableName)) {
            return HEADER_DEFAULT_WIDTHS;
        } else if (DISK_TABLE.equals(tableName)) {
            return DISK_DEFAULT_WIDTHS;
        } else if (INTERFACES_TABLE.equals(tableName)) {
            return INTERFACES_DEFAULT_WIDTHS;
        } else if (INPUTDEVS_TABLE.equals(tableName)) {
            return INPUTDEVS_DEFAULT_WIDTHS;
        } else if (GRAPHICS_TABLE.equals(tableName)) {
            return GRAPHICS_DEFAULT_WIDTHS;
        } else if (SOUND_TABLE.equals(tableName)) {
            return SOUND_DEFAULT_WIDTHS;
        } else if (SERIAL_TABLE.equals(tableName)) {
            return SERIAL_DEFAULT_WIDTHS;
        } else if (PARALLEL_TABLE.equals(tableName)) {
            return PARALLEL_DEFAULT_WIDTHS;
        } else if (VIDEO_TABLE.equals(tableName)) {
            return VIDEO_DEFAULT_WIDTHS;
        }
        return null;
    }

    /** Returns default widths for columns. Null for computed width. */
    @Override protected boolean isControlButton(final String tableName,
                                                final int column) {
        if (HEADER_TABLE.equals(tableName)) {
            return HEADER_DEFAULT_WIDTHS.containsKey(column);
        } else if (DISK_TABLE.equals(tableName)) {
            return DISK_DEFAULT_WIDTHS.containsKey(column);
        } else if (INTERFACES_TABLE.equals(tableName)) {
            return INTERFACES_DEFAULT_WIDTHS.containsKey(column);
        } else if (INPUTDEVS_TABLE.equals(tableName)) {
            return INPUTDEVS_DEFAULT_WIDTHS.containsKey(column);
        } else if (GRAPHICS_TABLE.equals(tableName)) {
            return GRAPHICS_DEFAULT_WIDTHS.containsKey(column);
        } else if (SOUND_TABLE.equals(tableName)) {
            return SOUND_DEFAULT_WIDTHS.containsKey(column);
        } else if (SERIAL_TABLE.equals(tableName)) {
            return SERIAL_DEFAULT_WIDTHS.containsKey(column);
        } else if (PARALLEL_TABLE.equals(tableName)) {
            return PARALLEL_DEFAULT_WIDTHS.containsKey(column);
        } else if (VIDEO_TABLE.equals(tableName)) {
            return VIDEO_DEFAULT_WIDTHS.containsKey(column);
        }
        return false;
    }

    /** Returns tool tip text in the table. */
    @Override protected String getTableToolTip(final String tableName,
                                               final String key,
                                               final Object object,
                                               final int raw,
                                               final int column) {
        if (HEADER_TABLE.equals(tableName)) {
            if (HEADER_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove domain " + key + ".";
            }
        } else if (DISK_TABLE.equals(tableName)) {
            if (DISK_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + ".";
            }
        } else if (INTERFACES_TABLE.equals(tableName)) {
            if (INTERFACES_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + ".";
            }
        } else if (INPUTDEVS_TABLE.equals(tableName)) {
            if (INPUTDEVS_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + ".";
            }
        } else if (GRAPHICS_TABLE.equals(tableName)) {
            if (GRAPHICS_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + ".";
            }
        } else if (SOUND_TABLE.equals(tableName)) {
            if (SOUND_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + ".";
            }
        } else if (SERIAL_TABLE.equals(tableName)) {
            if (SERIAL_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + ".";
            }
        } else if (PARALLEL_TABLE.equals(tableName)) {
            if (PARALLEL_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + ".";
            }
        } else if (VIDEO_TABLE.equals(tableName)) {
            if (VIDEO_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + ".";
            }
        }
        return super.getTableToolTip(tableName, key, object, raw, column);
    }

    /** Sets button next to host to the start button. */
    private void setButtonToStart(final Host host,
                                  final GuiComboBox hostCB,
                                  final MyButton hostBtn,
                                  final boolean stopped) {
        if (hostCB != null) {
            final boolean enable = host.isConnected();
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    hostCB.setTFButtonEnabled(enable && stopped);
                    hostBtn.setText("Start");
                    hostBtn.setIcon(HostBrowser.HOST_ON_ICON);
                    hostBtn.setToolTipText("Start on " + host.getName());
                }
            });
        }
    }

    /** Sets button next to host to the view button. */
    private void setButtonToView(final Host host,
                                 final GuiComboBox hostCB,
                                 final MyButton hostBtn) {
        if (hostCB != null) {
            final boolean enable = host.isConnected();
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    hostCB.setTFButtonEnabled(enable);
                    hostBtn.setText("View");
                    hostBtn.setIcon(VNC_ICON);
                    hostBtn.setToolTipText("Graphical console on "
                                           + host.getName());
                }
            });
        }
    }

    /** Sets button next to host to the not defined button. */
    private void setButtonToNotDefined(final Host host,
                                       final GuiComboBox hostCB,
                                       final MyButton hostBtn) {
        if (hostCB != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    hostCB.setTFButtonEnabled(false);
                    hostBtn.setIcon(null);
                    hostBtn.setToolTipText("not defined on " + host.getName());
                }
            });
        }
    }

    /** Sets all host buttons. */
    private void setHostButtons(final boolean running) {
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            final MyButton hostBtn = hostButtons.get(h.getName());
            final MyButton wizardHostBtn =
                                 hostButtons.get(WIZARD_PREFIX + h.getName());
            final GuiComboBox hostCB =
                                    definedOnHostComboBoxHash.get(h.getName());
            final GuiComboBox wizardHostCB =
                    definedOnHostComboBoxHash.get(WIZARD_PREFIX + h.getName());
            if (vmsxml != null
                && vmsxml.getDomainNames().contains(getDomainName())) {
                if (vmsxml.isRunning(getDomainName())) {
                    setButtonToView(h, hostCB, hostBtn);
                    setButtonToView(h, wizardHostCB, wizardHostBtn);
                } else {
                    setButtonToStart(h, hostCB, hostBtn, !running);
                    setButtonToStart(h, wizardHostCB, wizardHostBtn, !running);
                }
            } else {
                setButtonToNotDefined(h, hostCB, hostBtn);
                setButtonToNotDefined(h, wizardHostCB, wizardHostBtn);
            }
        }
    }

    /** Revert valus. */
    @Override public void revert() {
        for (final Host h : getBrowser().getClusterHosts()) {
            final GuiComboBox hostCB =
                                    definedOnHostComboBoxHash.get(h.getName());
            String savedValue;
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (getResource().isNew()
                || (vmsxml != null
                    && vmsxml.getDomainNames().contains(getDomainName()))) {
                savedValue = DEFINED_ON_HOST_TRUE;
            } else {
                savedValue = DEFINED_ON_HOST_FALSE;
            }
            hostCB.setValue(savedValue);
        }
        final Enumeration eee = getNode().children();
        while (eee.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode) eee.nextElement();
            final VMSHardwareInfo vmshi =
                            (VMSHardwareInfo) node.getUserObject();
            if (vmshi.checkResourceFieldsChanged(
                                             null,
                                             vmshi.getRealParametersFromXML(),
                                             true)) {
                vmshi.revert();
            }
        }
        super.revert();
    }

    /** Saves all preferred values. */
    public void savePreferredValues() {
        for (final String pv : PREFERRED_MAP.keySet()) {
            getResource().setValue(pv, PREFERRED_MAP.get(pv));
        }
    }

    /** Returns whether it is used by CRM. */
    boolean isUsedByCRM() {
        return usedByCRM;
    }

    /** Sets whether it is used by CRM. */
    public void setUsedByCRM(final boolean usedByCRM) {
        this.usedByCRM = usedByCRM;
    }
}
