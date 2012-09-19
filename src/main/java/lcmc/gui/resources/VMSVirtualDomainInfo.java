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
package lcmc.gui.resources;

import lcmc.gui.Browser;
import lcmc.gui.HostBrowser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.GuiComboBox;
import lcmc.gui.SpringUtilities;
import lcmc.data.VMSXML;
import lcmc.data.VMSXML.DiskData;
import lcmc.data.VMSXML.FilesystemData;
import lcmc.data.VMSXML.InterfaceData;
import lcmc.data.VMSXML.InputDevData;
import lcmc.data.VMSXML.GraphicsData;
import lcmc.data.VMSXML.SoundData;
import lcmc.data.VMSXML.SerialData;
import lcmc.data.VMSXML.ParallelData;
import lcmc.data.VMSXML.VideoData;
import lcmc.data.Host;
import lcmc.data.resources.Resource;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.utilities.UpdatableItem;
import lcmc.utilities.Tools;
import lcmc.utilities.MyMenu;
import lcmc.utilities.MyMenuItem;
import lcmc.utilities.VIRSH;
import lcmc.utilities.Unit;
import lcmc.utilities.MyButton;
import lcmc.utilities.WidgetListener;
import lcmc.Exceptions;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JScrollPane;
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
import java.util.Collections;
import java.util.UUID;
import java.util.regex.Matcher;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.geom.Point2D;

import org.w3c.dom.Node;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

/**
 * This class holds info about VirtualDomain service in the VMs category,
 * but not in the cluster view.
 */
public final class VMSVirtualDomainInfo extends EditableInfo {
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** UUID. */
    private String uuid;
    /** HTML string on which hosts the vm is defined. */
    private String definedOnString = "";
    /** HTML string on which hosts the vm is running. */
    private String runningOnString = "";
    /** Row color, that is color of host on which is it running or null. */
    private Color rowColor = Browser.PANEL_BACKGROUND;
    /** Transition between states lock. */
    private final ReadWriteLock mTransitionLock = new ReentrantReadWriteLock();
    /** Transition between states read lock. */
    private final Lock mTransitionReadLock = mTransitionLock.readLock();
    /** Transition between states write lock. */
    private final Lock mTransitionWriteLock = mTransitionLock.writeLock();
    /** Starting. */
    private final Set<String> starting = new HashSet<String>();
    /** Shutting down. */
    private final Set<String> shuttingdown = new HashSet<String>();
    /** Suspending. */
    private final Set<String> suspending = new HashSet<String>();
    /** Resuming. */
    private final Set<String> resuming = new HashSet<String>();
    /** Progress indicator when starting or stopping. */
    private final StringBuilder progress = new StringBuilder("-");
    /** Disk to info hash lock. */
    private final Lock mDiskToInfoLock = new ReentrantLock();
    /** Map from key to vms disk info object. */
    private final Map<String, VMSDiskInfo> diskToInfo =
                                           new HashMap<String, VMSDiskInfo>();
    /** Map from target string in the table to vms disk info object.
     */
    private volatile Map<String, VMSDiskInfo> diskKeyToInfo =
                                           new HashMap<String, VMSDiskInfo>();

    /** FS to info hash lock. */
    private final Lock mFilesystemToInfoLock = new ReentrantLock();
    /** Map from key to vms fs info object. */
    private final Map<String, VMSFilesystemInfo> filesystemToInfo =
                                      new HashMap<String, VMSFilesystemInfo>();
    /** Map from target string in the table to vms fs info object.
     */
    private volatile Map<String, VMSFilesystemInfo> filesystemKeyToInfo =
                                       new HashMap<String, VMSFilesystemInfo>();

    /** Interface to info hash lock. */
    private final Lock mInterfaceToInfoLock = new ReentrantLock();
    /** Map from key to vms interface info object. */
    private final Map<String, VMSInterfaceInfo> interfaceToInfo =
                                       new HashMap<String, VMSInterfaceInfo>();
    /** Map from target string in the table to vms interface info object. */
    private volatile Map<String, VMSInterfaceInfo> interfaceKeyToInfo =
                                       new HashMap<String, VMSInterfaceInfo>();
    /** Input device to info hash lock. */
    private final Lock mInputDevToInfoLock = new ReentrantLock();
    /** Map from key to vms input device info object. */
    private final Map<String, VMSInputDevInfo> inputDevToInfo =
                                       new HashMap<String, VMSInputDevInfo>();
    /** Map from target string in the table to vms interface info object. */
    private volatile Map<String, VMSInputDevInfo> inputDevKeyToInfo =
                                       new HashMap<String, VMSInputDevInfo>();

    /** Graphics device to info hash lock. */
    private final Lock mGraphicsToInfoLock = new ReentrantLock();
    /** Map from key to vms graphics device info object. */
    private final Map<String, VMSGraphicsInfo> graphicsToInfo =
                                       new HashMap<String, VMSGraphicsInfo>();
    /** Map from target string in the table to vms interface info object. */
    private volatile Map<String, VMSGraphicsInfo> graphicsKeyToInfo =
                                       new HashMap<String, VMSGraphicsInfo>();

    /** Sound device to info hash lock. */
    private final Lock mSoundToInfoLock = new ReentrantLock();
    /** Map from key to vms sound device info object. */
    private final Map<String, VMSSoundInfo> soundToInfo =
                                       new HashMap<String, VMSSoundInfo>();
    /** Map from target string in the table to vms interface info object. */
    private volatile Map<String, VMSSoundInfo> soundKeyToInfo =
                                       new HashMap<String, VMSSoundInfo>();

    /** Serial device to info hash lock. */
    private final Lock mSerialToInfoLock = new ReentrantLock();
    /** Map from key to vms serial device info object. */
    private final Map<String, VMSSerialInfo> serialToInfo =
                                       new HashMap<String, VMSSerialInfo>();
    /** Map from target string in the table to vms interface info object. */
    private volatile Map<String, VMSSerialInfo> serialKeyToInfo =
                                       new HashMap<String, VMSSerialInfo>();

    /** Parallel device to info hash lock. */
    private final Lock mParallelToInfoLock = new ReentrantLock();
    /** Map from key to vms parallel device info object. */
    private final Map<String, VMSParallelInfo> parallelToInfo =
                                       new HashMap<String, VMSParallelInfo>();
    /** Preferred emulator. It's distro dependent. */
    private final String preferredEmulator;
    /** Map from target string in the table to vms interface info object. */
    private volatile Map<String, VMSParallelInfo> parallelKeyToInfo =
                                       new HashMap<String, VMSParallelInfo>();

    /** Video device to info hash lock. */
    private final Lock mVideoToInfoLock = new ReentrantLock();
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
    /** Previous type. */
    private volatile String prevType = null;
    /** Autostart possible values - hosts */
    private final String[] autostartPossibleValues;
    /** Timeout of starting, shutting down, etc. actions in seconds. */
    private static final int ACTION_TIMEOUT = 20;
    /** Virsh options. */
    private static final String VIRSH_OPTION_KVM = "";
    private static final String VIRSH_OPTION_XEN = "-c 'xen:///'";
    private static final String VIRSH_OPTION_LXC = "-c 'lxc:///'";
    /** Domain types. */
    static final String DOMAIN_TYPE_KVM = "kvm";
    private static final String DOMAIN_TYPE_XEN = "xen";
    private static final String DOMAIN_TYPE_LXC = "lxc";
    private static final String DOMAIN_TYPE_UML = "uml";
    private static final String DOMAIN_TYPE_OPENVZ = "openvz";



    private static final String[] VIRSH_OPTIONS = new String[]{
                                                            VIRSH_OPTION_KVM,
                                                            VIRSH_OPTION_XEN,
                                                            VIRSH_OPTION_LXC};

    /** Whether it needs "display" section. */
    private static final Set<String> NEED_DISPLAY =
         Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                                                            DOMAIN_TYPE_KVM,
                                                            DOMAIN_TYPE_XEN)));
    /** Whether it needs "console" section. */
    private static final Set<String> NEED_CONSOLE =
         Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                                                            DOMAIN_TYPE_LXC,
                                                            DOMAIN_TYPE_OPENVZ,
                                                            DOMAIN_TYPE_UML)));
    /** Whether it needs "console" section. */
    private static final Set<String> NEED_FILESYSTEM =
         Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                                                            DOMAIN_TYPE_LXC,
                                                            DOMAIN_TYPE_OPENVZ,
                                                            DOMAIN_TYPE_UML)));
    /** All parameters. */
    private static final String[] VM_PARAMETERS = new String[]{
                                    VMSXML.VM_PARAM_DOMAIN_TYPE,
                                    VMSXML.VM_PARAM_NAME,
                                    VMSXML.VM_PARAM_VIRSH_OPTIONS,
                                    VMSXML.VM_PARAM_EMULATOR,
                                    VMSXML.VM_PARAM_VCPU,
                                    VMSXML.VM_PARAM_CURRENTMEMORY,
                                    VMSXML.VM_PARAM_MEMORY,
                                    VMSXML.VM_PARAM_BOOTLOADER,
                                    VMSXML.VM_PARAM_BOOT,
                                    VMSXML.VM_PARAM_BOOT_2,
                                    VMSXML.VM_PARAM_LOADER,
                                    VMSXML.VM_PARAM_AUTOSTART,
                                    VMSXML.VM_PARAM_TYPE,
                                    VMSXML.VM_PARAM_INIT,
                                    VMSXML.VM_PARAM_TYPE_ARCH,
                                    VMSXML.VM_PARAM_TYPE_MACHINE,
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
    /** FS table. */
    static final String FILESYSTEM_TABLE = "filesystems";
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
    private static final Map<Integer, Integer> FILESYSTEM_DEFAULT_WIDTHS =
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
    /** Type HVM. */
    private static final String TYPE_HVM = "hvm";
    /** Type Linux. */
    private static final String TYPE_LINUX = "linux";
    /** Type exe. */
    private static final String TYPE_EXE = "exe";

    /** Width of the button field. */
    private static final int CONTROL_BUTTON_WIDTH = 80;
    static {
        /* remove button column */
        HEADER_DEFAULT_WIDTHS.put(4, CONTROL_BUTTON_WIDTH);
        DISK_DEFAULT_WIDTHS.put(2, CONTROL_BUTTON_WIDTH);
        FILESYSTEM_DEFAULT_WIDTHS.put(2, CONTROL_BUTTON_WIDTH);
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
    /** This is a map from host to the check box. */
    private final Map<String, GuiComboBox> definedOnHostComboBoxHash =
                                          new HashMap<String, GuiComboBox>();
    static {
        SECTION_MAP.put(VMSXML.VM_PARAM_NAME,          VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_DOMAIN_TYPE,   VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_EMULATOR,      VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_VCPU,          VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_CURRENTMEMORY, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_MEMORY,        VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_BOOTLOADER,    VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_BOOT,          VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_BOOT_2,        VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_LOADER,        VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_AUTOSTART,     VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_VIRSH_OPTIONS, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_TYPE,          VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_TYPE_ARCH,     VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_TYPE_MACHINE,  VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_INIT,          VIRTUAL_SYSTEM_STRING);

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
                   VMSXML.VM_PARAM_DOMAIN_TYPE,
                   Tools.getString("VMSVirtualDomainInfo.Short.DomainType"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_VCPU,
                   Tools.getString("VMSVirtualDomainInfo.Short.Vcpu"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_BOOTLOADER,
                   Tools.getString("VMSVirtualDomainInfo.Short.Bootloader"));
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
                   VMSXML.VM_PARAM_BOOT_2,
                   Tools.getString("VMSVirtualDomainInfo.Short.Os.Boot.2"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_LOADER,
                   Tools.getString("VMSVirtualDomainInfo.Short.Os.Loader"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_AUTOSTART,
                   Tools.getString("VMSVirtualDomainInfo.Short.Autostart"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_VIRSH_OPTIONS,
                   Tools.getString("VMSVirtualDomainInfo.Short.VirshOptions"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_TYPE,
                   Tools.getString("VMSVirtualDomainInfo.Short.Type"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_INIT,
                   Tools.getString("VMSVirtualDomainInfo.Short.Init"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_TYPE_ARCH,
                   Tools.getString("VMSVirtualDomainInfo.Short.Arch"));
        SHORTNAME_MAP.put(
                   VMSXML.VM_PARAM_TYPE_MACHINE,
                   Tools.getString("VMSVirtualDomainInfo.Short.Machine"));
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
        FIELD_TYPES.put(VMSXML.VM_PARAM_APIC, GuiComboBox.Type.CHECKBOX);
        FIELD_TYPES.put(VMSXML.VM_PARAM_ACPI, GuiComboBox.Type.CHECKBOX);
        FIELD_TYPES.put(VMSXML.VM_PARAM_PAE, GuiComboBox.Type.CHECKBOX);
        FIELD_TYPES.put(VMSXML.VM_PARAM_HAP, GuiComboBox.Type.CHECKBOX);

        PREFERRED_MAP.put(VMSXML.VM_PARAM_CURRENTMEMORY, "512M");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_MEMORY, "512M");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_TYPE, TYPE_HVM);
        PREFERRED_MAP.put(VMSXML.VM_PARAM_TYPE_ARCH, "x86_64");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_TYPE_MACHINE, "pc");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_ACPI, "True");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_APIC, "True");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_PAE, "True");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_ON_POWEROFF, "destroy");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_ON_REBOOT, "restart");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_ON_CRASH, "restart");
        PREFERRED_MAP.put(VMSXML.VM_PARAM_EMULATOR, "/usr/bin/kvm");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_AUTOSTART, null);
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_VIRSH_OPTIONS, "");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_BOOT, "hd");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_BOOT, "");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_DOMAIN_TYPE, DOMAIN_TYPE_KVM);
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
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_BOOT_2,
                           new StringInfo[]{
                                      null,
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
                            new String[]{});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_DOMAIN_TYPE,
                            new String[]{DOMAIN_TYPE_KVM,
                                         DOMAIN_TYPE_XEN,
                                         DOMAIN_TYPE_LXC,
                                         });
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_BOOTLOADER,
                            new String[]{"", "/usr/bin/pygrub"});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_TYPE,
                            new String[]{TYPE_HVM, TYPE_LINUX, TYPE_EXE});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_TYPE_ARCH,
                            new String[]{"", "x86_64", "i686"});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_TYPE_MACHINE,
                            new String[]{"", "pc", "pc-0.12"});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_INIT,
                            new String[]{"", "/bin/sh", "/init"});
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
                            new String[]{"/usr/bin/kvm",
                                         "/usr/bin/qemu"});
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
        final Host firstHost = getBrowser().getClusterHosts()[0];
        preferredEmulator = firstHost.getDistString("KVM.emulator");
        final List<String> hostsList = new ArrayList<String>();
        hostsList.add(null);
        for (final Host h : getBrowser().getClusterHosts()) {
            hostsList.add(h.getName());
        }
        autostartPossibleValues =
                              hostsList.toArray(new String[hostsList.size()]);

        setResource(new Resource(name));
    }

    /** Returns browser object of this info. */
    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** Returns a name of the service with virtual domain name. */
    @Override
    public String toString() {
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
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
        final Map<String, DiskData> disks = getDisks();
        final List<String> diskNames  = new ArrayList<String>();
        if (disks != null) {
            for (final String d : disks.keySet()) {
                diskNames.add(d);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = thisNode.children();
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
                mDiskToInfoLock.lock();
                try {
                    diskToInfo.put(vmsdi.getName(), vmsdi);
                } finally {
                    mDiskToInfoLock.unlock();
                }
                vmsdi.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                mDiskToInfoLock.lock();
                try {
                    diskToInfo.remove(vmsdi.getName());
                } finally {
                    mDiskToInfoLock.unlock();
                }
                vmsdi.setNode(null);
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String disk : diskNames) {
            final Enumeration eee = thisNode.children();
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
            mDiskToInfoLock.lock();
            try {
                diskToInfo.put(disk, vmsdi);
            } finally {
                mDiskToInfoLock.unlock();
            }
            vmsdi.updateParameters();
            final DefaultMutableTreeNode resource =
                                        new DefaultMutableTreeNode(vmsdi);
            getBrowser().setNode(resource);
            thisNode.insert(resource, i);
            nodeChanged = true;
        }
        return nodeChanged;
    }

    /** Updates FS nodes. Returns whether the node changed. */
    private boolean updateFilesystemNodes() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
        final Map<String, FilesystemData> filesystems = getFilesystems();
        final List<String> filesystemNames  = new ArrayList<String>();
        if (filesystems != null) {
            for (final String d : filesystems.keySet()) {
                filesystemNames.add(d);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = thisNode.children();
        boolean nodeChanged = false;
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) e.nextElement();
            if (!(node.getUserObject() instanceof VMSFilesystemInfo)) {
                continue;
            }
            final VMSFilesystemInfo vmsdi =
                              (VMSFilesystemInfo) node.getUserObject();
            if (vmsdi.getResource().isNew()) {
                /* keep */
            } else if (filesystemNames.contains(vmsdi.getName())) {
                /* keeping */
                filesystemNames.remove(vmsdi.getName());
                mFilesystemToInfoLock.lock();
                try {
                    filesystemToInfo.put(vmsdi.getName(), vmsdi);
                } finally {
                    mFilesystemToInfoLock.unlock();
                }
                vmsdi.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                mFilesystemToInfoLock.lock();
                try {
                    filesystemToInfo.remove(vmsdi.getName());
                } finally {
                    mFilesystemToInfoLock.unlock();
                }
                vmsdi.setNode(null);
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String filesystem : filesystemNames) {
            final Enumeration eee = thisNode.children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) eee.nextElement();
                if (!(node.getUserObject() instanceof VMSFilesystemInfo)) {
                    continue;
                }
                final VMSFilesystemInfo v = (VMSFilesystemInfo) node.getUserObject();
                final String n = v.getName();
                if (n != null && filesystem.compareTo(n) < 0) {
                    break;
                }
                i++;
            }
            /* add new vm fs */
            final VMSFilesystemInfo vmsdi =
                        new VMSFilesystemInfo(filesystem, getBrowser(), this);
            mFilesystemToInfoLock.lock();
            try {
                filesystemToInfo.put(filesystem, vmsdi);
            } finally {
                mFilesystemToInfoLock.unlock();
            }
            vmsdi.updateParameters();
            final DefaultMutableTreeNode resource =
                                        new DefaultMutableTreeNode(vmsdi);
            getBrowser().setNode(resource);
            thisNode.insert(resource, i);
            nodeChanged = true;
        }
        return nodeChanged;
    }


    /** Updates interface nodes. Returns whether the node changed. */
    private boolean updateInterfaceNodes() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
        final Map<String, InterfaceData> interfaces = getInterfaces();
        final List<String> interfaceNames  = new ArrayList<String>();
        if (interfaces != null) {
            for (final String i : interfaces.keySet()) {
                interfaceNames.add(i);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = thisNode.children();
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
                mInterfaceToInfoLock.lock();
                try {
                    interfaceToInfo.remove(vmsii.getName());
                } finally {
                    mInterfaceToInfoLock.unlock();
                }
                vmsii.setNode(null);
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String interf : interfaceNames) {
            VMSInterfaceInfo vmsii;
            if (emptySlot == null) {
                final Enumeration eee = thisNode.children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                    if (!(node.getUserObject() instanceof VMSInterfaceInfo)) {
                        if (node.getUserObject() instanceof VMSDiskInfo
                            || node.getUserObject() instanceof VMSFilesystemInfo) {
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
                thisNode.insert(resource, i);
                nodeChanged = true;
            } else {
                vmsii = emptySlot;
                vmsii.setName(interf);
                emptySlot = null;
            }
            mInterfaceToInfoLock.lock();
            try {
                interfaceToInfo.put(interf, vmsii);
            } finally {
                mInterfaceToInfoLock.unlock();
            }
            vmsii.updateParameters();
        }
        return nodeChanged;
    }

    /** Updates input devices nodes. Returns whether the node changed. */
    private boolean updateInputDevNodes() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
        final Map<String, InputDevData> inputDevs = getInputDevs();
        final List<String> inputDevNames  = new ArrayList<String>();
        if (inputDevs != null) {
            for (final String d : inputDevs.keySet()) {
                inputDevNames.add(d);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = thisNode.children();
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
                mInputDevToInfoLock.lock();
                try {
                    inputDevToInfo.remove(vmsid.getName());
                } finally {
                    mInputDevToInfoLock.unlock();
                }
                vmsid.setNode(null);
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String inputDev : inputDevNames) {
            VMSInputDevInfo vmsid;
            final InputDevData data = inputDevs.get(inputDev);
            final Enumeration eee = thisNode.children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) eee.nextElement();
                if (!(node.getUserObject() instanceof VMSInputDevInfo)) {
                    if (node.getUserObject() instanceof VMSDiskInfo
                        || node.getUserObject() instanceof VMSFilesystemInfo
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
            thisNode.insert(resource, i);
            nodeChanged = true;
            mInputDevToInfoLock.lock();
            try {
                inputDevToInfo.put(inputDev, vmsid);
            } finally {
                mInputDevToInfoLock.unlock();
            }
            vmsid.updateParameters();
        }
        /* Sort it. */
        int i = 0;
        for (int j = 0; j < thisNode.getChildCount(); j++) {
            final DefaultMutableTreeNode node =
                         (DefaultMutableTreeNode) thisNode.getChildAt(j);
            final VMSHardwareInfo v = (VMSHardwareInfo) node.getUserObject();
            final String n = v.getName();
            if (i > 0) {
                final DefaultMutableTreeNode prev =
                     (DefaultMutableTreeNode) thisNode.getChildAt(j - 1);
                final VMSHardwareInfo prevI =
                                        (VMSHardwareInfo) prev.getUserObject();
                if (prevI.getClass().getName().equals(v.getClass().getName())) {
                    final String prevN = prevI.getName();
                    if (!prevI.getResource().isNew()
                        && !v.getResource().isNew()
                        && (prevN != null && prevN.compareTo(n) > 0)) {
                        thisNode.remove(j);
                        thisNode.insert(node, j - 1);
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
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
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
        final Enumeration e = thisNode.children();
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
                mGraphicsToInfoLock.lock();
                try {
                    graphicsToInfo.put(vmsgi.getName(), vmsgi);
                } finally {
                    mGraphicsToInfoLock.unlock();
                }
                vmsgi.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                mGraphicsToInfoLock.lock();
                try {
                    graphicsToInfo.remove(vmsgi.getName());
                } finally {
                    mGraphicsToInfoLock.unlock();
                }
                vmsgi.setNode(null);
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String graphicDisplay : graphicsNames) {
            VMSGraphicsInfo vmsgi;
            final GraphicsData data = graphicDisplays.get(graphicDisplay);
            final Enumeration eee = thisNode.children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) eee.nextElement();
                if (!(node.getUserObject() instanceof VMSGraphicsInfo)) {
                    if (node.getUserObject() instanceof VMSDiskInfo
                        || node.getUserObject() instanceof VMSFilesystemInfo
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
            thisNode.insert(resource, i);
            nodeChanged = true;
            mGraphicsToInfoLock.lock();
            try {
                graphicsToInfo.put(graphicDisplay, vmsgi);
            } finally {
                mGraphicsToInfoLock.unlock();
            }
            vmsgi.updateParameters();
        }
        return nodeChanged;
    }

    /** Updates sound devices nodes. Returns whether the node changed. */
    private boolean updateSoundNodes() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
        final Map<String, SoundData> sounds = getSounds();
        final List<String> soundNames  = new ArrayList<String>();
        if (sounds != null) {
            for (final String d : sounds.keySet()) {
                soundNames.add(d);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = thisNode.children();
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
                mSoundToInfoLock.lock();
                try {
                    soundToInfo.put(vmssi.getName(), vmssi);
                } finally {
                    mSoundToInfoLock.unlock();
                }
                vmssi.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                mSoundToInfoLock.lock();
                try {
                    soundToInfo.remove(vmssi.getName());
                } finally {
                    mSoundToInfoLock.unlock();
                }
                vmssi.setNode(null);
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String sound : soundNames) {
            VMSSoundInfo vmssi;
            final SoundData data = sounds.get(sound);
            final Enumeration eee = thisNode.children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) eee.nextElement();
                if (!(node.getUserObject() instanceof VMSSoundInfo)) {
                    if (node.getUserObject() instanceof VMSDiskInfo
                        || node.getUserObject() instanceof VMSFilesystemInfo
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
            thisNode.insert(resource, i);
            nodeChanged = true;
            mSoundToInfoLock.lock();
            try {
                soundToInfo.put(sound, vmssi);
            } finally {
                mSoundToInfoLock.unlock();
            }
            vmssi.updateParameters();
        }
        return nodeChanged;
    }

    /** Updates serial devices nodes. Returns whether the node changed. */
    private boolean updateSerialNodes() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
        final Map<String, SerialData> serials = getSerials();
        final List<String> serialNames  = new ArrayList<String>();
        if (serials != null) {
            for (final String d : serials.keySet()) {
                serialNames.add(d);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = thisNode.children();
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
                mSerialToInfoLock.lock();
                try {
                    serialToInfo.remove(vmssi.getName());
                } finally {
                    mSerialToInfoLock.unlock();
                }
                vmssi.setNode(null);
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String serial : serialNames) {
            VMSSerialInfo vmssi;
            final SerialData data = serials.get(serial);
            if (emptySlot == null) {
                final Enumeration eee = thisNode.children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                    if (!(node.getUserObject() instanceof VMSSerialInfo)) {
                        if (node.getUserObject() instanceof VMSDiskInfo
                            || node.getUserObject() instanceof VMSFilesystemInfo
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
                thisNode.insert(resource, i);
                nodeChanged = true;
            } else {
                vmssi = emptySlot;
                vmssi.setName(serial);
                emptySlot = null;
            }
            mSerialToInfoLock.lock();
            try {
                serialToInfo.put(serial, vmssi);
            } finally {
                mSerialToInfoLock.unlock();
            }
            vmssi.updateParameters();
        }
        return nodeChanged;
    }

    /** Updates parallel devices nodes. Returns whether the node changed. */
    private boolean updateParallelNodes() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
        final Map<String, ParallelData> parallels = getParallels();
        final List<String> parallelNames  = new ArrayList<String>();
        if (parallels != null) {
            for (final String d : parallels.keySet()) {
                parallelNames.add(d);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = thisNode.children();
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
                mParallelToInfoLock.lock();
                try {
                    parallelToInfo.remove(vmspi.getName());
                } finally {
                    mParallelToInfoLock.unlock();
                }
                vmspi.setNode(null);
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String parallel : parallelNames) {
            VMSParallelInfo vmspi;
            final ParallelData data = parallels.get(parallel);
            if (emptySlot == null) {
                final Enumeration eee = thisNode.children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                    if (!(node.getUserObject() instanceof VMSParallelInfo)) {
                        if (node.getUserObject() instanceof VMSDiskInfo
                            || node.getUserObject() instanceof VMSFilesystemInfo
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
                thisNode.insert(resource, i);
                nodeChanged = true;
            } else {
                vmspi = emptySlot;
                vmspi.setName(parallel);
                emptySlot = null;
            }
            mParallelToInfoLock.lock();
            try {
                parallelToInfo.put(parallel, vmspi);
            } finally {
                mParallelToInfoLock.unlock();
            }
            vmspi.updateParameters();
        }
        return nodeChanged;
    }

    /** Updates video devices nodes. Returns whether the node changed. */
    private boolean updateVideoNodes() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
        final Map<String, VideoData> videos = getVideos();
        final List<String> videoNames  = new ArrayList<String>();
        if (videos != null) {
            for (final String d : videos.keySet()) {
                videoNames.add(d);
            }
        }
        final List<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        final Enumeration e = thisNode.children();
        boolean nodeChanged = false;
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) e.nextElement();
            if (!(node.getUserObject() instanceof VMSVideoInfo)) {
                continue;
            }
            final VMSVideoInfo vmsvi = (VMSVideoInfo) node.getUserObject();
            if (vmsvi.getResource().isNew()) {
                /* keep */
            } else if (videoNames.contains(vmsvi.getName())) {
                /* keeping */
                videoNames.remove(vmsvi.getName());
                mVideoToInfoLock.lock();
                try {
                    videoToInfo.put(vmsvi.getName(), vmsvi);
                } finally {
                    mVideoToInfoLock.unlock();
                }
                vmsvi.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                mVideoToInfoLock.lock();
                try {
                    videoToInfo.remove(vmsvi.getName());
                } finally {
                    mVideoToInfoLock.unlock();
                }
                vmsvi.setNode(null);
                nodesToRemove.add(node);
                nodeChanged = true;
            }
        }

        /* remove nodes */
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String video : videoNames) {
            VMSVideoInfo vmspi;
            final VideoData data = videos.get(video);
            final Enumeration eee = thisNode.children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) eee.nextElement();
                if (!(node.getUserObject() instanceof VMSVideoInfo)) {
                    if (node.getUserObject() instanceof VMSDiskInfo
                        || node.getUserObject() instanceof VMSFilesystemInfo
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
            thisNode.insert(resource, i);
            nodeChanged = true;
            mVideoToInfoLock.lock();
            try {
                videoToInfo.put(video, vmspi);
            } finally {
                mVideoToInfoLock.unlock();
            }
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
            @Override
            public void run() {
                hBtn.setBackgroundColor(Browser.PANEL_BACKGROUND);
            }
        });
        hostBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
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
                                    @Override
                                    public void run() {
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
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return;
        }
        final List<String> runningOnHosts = new ArrayList<String>();
        final List<String> suspendedOnHosts = new ArrayList<String>();
        final List<String> definedhosts = new ArrayList<String>();
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            final String hostName = h.getName();
            //final MyButton hostBtn = hostButtons.get(h.getName());
            //final MyButton wizardHostBtn =
            //                      hostButtons.get(WIZARD_PREFIX + h.getName());
            //final GuiComboBox hostCB =
            //                        definedOnHostComboBoxHash.get(h.getName());
            //final GuiComboBox wizardHostCB =
            //           definedOnHostComboBoxHash.get(WIZARD_PREFIX + hostName);

            if (vmsxml != null
                && vmsxml.getDomainNames().contains(getDomainName())) {
                if (vmsxml.isRunning(getDomainName())) {
                    if (vmsxml.isSuspended(getDomainName())) {
                        suspendedOnHosts.add(hostName);
                        mTransitionWriteLock.lock();
                        suspending.remove(hostName);
                        mTransitionWriteLock.unlock();
                    } else {
                        mTransitionWriteLock.lock();
                        resuming.remove(hostName);
                        mTransitionWriteLock.unlock();
                    }
                    runningOnHosts.add(hostName);
                    mTransitionWriteLock.lock();
                    starting.remove(hostName);
                    mTransitionWriteLock.unlock();
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
        final boolean running = !runningOnHosts.isEmpty();
        mTransitionWriteLock.lock();
        /* Set host buttons */
        setHostButtons(running);
        if (runningOnHosts.isEmpty() && starting.isEmpty()) {
            shuttingdown.clear();
            suspending.clear();
            resuming.clear();
            mTransitionWriteLock.unlock();
            runningOnString = "Stopped";
        } else {
            mTransitionWriteLock.unlock();
            if (progress.charAt(0) == '-') {
                progress.setCharAt(0, '\\');
            } else if (progress.charAt(0) == '\\') {
                progress.setCharAt(0, '|');
            } else if (progress.charAt(0) == '|') {
                progress.setCharAt(0, '/');
            } else if (progress.charAt(0) == '/') {
                progress.setCharAt(0, '-');
            }
            mTransitionReadLock.lock();
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
            mTransitionReadLock.unlock();
        }
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            final GuiComboBox hcb =
                                definedOnHostComboBoxHash.get(h.getName());
            if (hcb != null) {
                String value;
                if ((vmsxml != null
                        && vmsxml.getDomainNames().contains(getDomainName()))) {
                    value = DEFINED_ON_HOST_TRUE;
                } else {
                    value = DEFINED_ON_HOST_FALSE;
                }
                hcb.setValue(value);
            }
        }
        for (final String param : getParametersFromXML()) {
            final String oldValue = getParamSaved(param);
            String value = null;
            final GuiComboBox cb = paramComboBoxGet(param, null);
            for (final Host h : getDefinedOnHosts()) {
                final VMSXML vmsxml = getBrowser().getVMSXML(h);
                if (vmsxml != null && value == null) {
                    value = getParamSaved(param);
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
        for (final Host h : getDefinedOnHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                uuid = vmsxml.getValue(getDomainName(), VMSXML.VM_PARAM_UUID);
            }
        }
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                final boolean interfaceNodeChanged = updateInterfaceNodes();
                final boolean diskNodeChanged = updateDiskNodes();
                final boolean filesystemNodeChanged = updateFilesystemNodes();
                final boolean inputDevNodeChanged = updateInputDevNodes();
                final boolean graphicsNodeChanged = updateGraphicsNodes();
                final boolean soundNodeChanged = updateSoundNodes();
                final boolean serialNodeChanged = updateSerialNodes();
                final boolean parallelNodeChanged = updateParallelNodes();
                final boolean videoNodeChanged = updateVideoNodes();
                if (interfaceNodeChanged
                    || diskNodeChanged
                    || filesystemNodeChanged
                    || inputDevNodeChanged
                    || graphicsNodeChanged
                    || soundNodeChanged
                    || serialNodeChanged
                    || parallelNodeChanged
                    || videoNodeChanged) {
                    getBrowser().reload(thisNode, false);
                }
            }
        });
        updateTable(HEADER_TABLE);
        updateTable(DISK_TABLE);
        updateTable(FILESYSTEM_TABLE);
        updateTable(INTERFACES_TABLE);
        updateTable(INPUTDEVS_TABLE);
        updateTable(GRAPHICS_TABLE);
        updateTable(SOUND_TABLE);
        updateTable(SERIAL_TABLE);
        updateTable(PARALLEL_TABLE);
        updateTable(VIDEO_TABLE);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setApplyButtons(null, getParametersFromXML());
                getBrowser().repaintTree();
            }
        });
    }

    /** Returns menu item for VNC different viewers. */
    private String getVNCMenuString(final String viewer, final Host host) {
        return Tools.getString("VMSVirtualDomainInfo.StartVNCViewerOn")
                            .replaceAll("@VIEWER@",
                                        Matcher.quoteReplacement(viewer))
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
                    @Override
                    public void run() {
                        cb.setEnabled(false);
                    }
                });
            }
            cb.addListeners(
                        new WidgetListener() {
                            @Override
                            public void check(final Object value) {
                                checkParameterFields(cb,
                                                     realParamCb,
                                                     ServiceInfo.CACHED_FIELD,
                                                     getParametersFromXML(),
                                                     thisApplyButton);
                            }
                        });
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
    @Override
    public JComponent getInfoPanel() {
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
            final JComponent newVMButton =
                                    getBrowser().getVMSInfo().getNewButton();
            mainPanel.add(newVMButton);
            mainPanel.add(table.getTableHeader());
            mainPanel.add(table);
        }

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
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
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override
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
            getRevertButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
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
        extraButtonPanel.setBackground(Browser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.add(extraButtonPanel);
        addApplyButton(buttonPanel);
        addRevertButton(extraButtonPanel);
        final MyButton overviewButton = new MyButton("VMs Overview",
                                                     BACK_ICON);
        overviewButton.miniButton();
        overviewButton.setPreferredSize(new Dimension(130, 50));
        overviewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
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
        buttonPanel.add(getActionsButton(), BorderLayout.EAST);
        mainPanel.add(optionsPanel);

        final MyButton newDiskBtn = VMSDiskInfo.getNewBtn(this);
        final MyButton newFilesystemBtn = VMSFilesystemInfo.getNewBtn(this);
        final MyButton newInterfaceBtn = VMSInterfaceInfo.getNewBtn(this);
        final MyButton newInputBtn = VMSInputDevInfo.getNewBtn(this);
        final MyButton newGraphicsBtn = VMSGraphicsInfo.getNewBtn(this);
        final MyButton newSoundBtn = VMSSoundInfo.getNewBtn(this);
        final MyButton newSerialBtn = VMSSerialInfo.getNewBtn(this);
        final MyButton newParallelBtn = VMSParallelInfo.getNewBtn(this);
        final MyButton newVideoBtn = VMSVideoInfo.getNewBtn(this);
        /* new video button */
        mainPanel.add(getTablePanel("Disks",
                                    DISK_TABLE,
                                    newDiskBtn));
        mainPanel.add(getTablePanel("Filesystems",
                                    FILESYSTEM_TABLE,
                                    newFilesystemBtn));

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
            @Override
            public void run() {
                setApplyButtons(null, params);
            }
        });
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    /** Starts the domain. */
    void start(final Host host) {
        final boolean ret = VIRSH.start(host,
                                        getDomainName(),
                                        getVirshOptions());
        if (ret) {
            int i = 0;
            mTransitionWriteLock.lock();
            final boolean wasEmpty = starting.isEmpty();
            starting.add(host.getName());
            if (!wasEmpty) {
                mTransitionWriteLock.unlock();
                return;
            }
            mTransitionWriteLock.unlock();
            while (true) {
                getBrowser().periodicalVMSUpdate(host);
                updateParameters();
                getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
                Tools.sleep(1000);
                i++;
                mTransitionReadLock.lock();
                if (starting.isEmpty() || i >= ACTION_TIMEOUT) {
                    mTransitionReadLock.unlock();
                    break;
                }
                mTransitionReadLock.unlock();
            }
            if (i >= ACTION_TIMEOUT) {
                Tools.appWarning("could not start on " + host.getName());
                mTransitionWriteLock.lock();
                try {
                    starting.clear();
                } finally {
                    mTransitionWriteLock.unlock();
                }
            }
            getBrowser().periodicalVMSUpdate(host);
            updateParameters();
            getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
        }
    }

    /** Starts shutting down indicator. */
    private void startShuttingdownIndicator(final Host host) {
        int i = 0;
        mTransitionWriteLock.lock();
        final boolean wasEmpty = starting.isEmpty();
        shuttingdown.add(host.getName());
        if (!wasEmpty) {
            mTransitionWriteLock.unlock();
            return;
        }
        mTransitionWriteLock.unlock();
        while (true) {
            getBrowser().periodicalVMSUpdate(host);
            updateParameters();
            getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
            Tools.sleep(1000);
            i++;
            mTransitionReadLock.lock();
            if (shuttingdown.isEmpty() || i >= ACTION_TIMEOUT) {
                mTransitionReadLock.unlock();
                break;
            }
            mTransitionReadLock.unlock();
        }
        if (i >= ACTION_TIMEOUT) {
            Tools.appWarning("could not shut down on " + host.getName());
            mTransitionWriteLock.lock();
            try {
                shuttingdown.clear();
            } finally {
                mTransitionWriteLock.unlock();
            }
        }
        getBrowser().periodicalVMSUpdate(host);
        updateParameters();
        getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
    }

    /** Shuts down the domain. */
    void shutdown(final Host host) {
        final boolean ret = VIRSH.shutdown(host,
                                           getDomainName(),
                                           getVirshOptions());
        if (ret) {
            startShuttingdownIndicator(host);
        }
    }

    /** Destroys down the domain. */
    void destroy(final Host host) {
        final boolean ret = VIRSH.destroy(host,
                                          getDomainName(),
                                          getVirshOptions());
        if (ret) {
            startShuttingdownIndicator(host);
        }
    }

    /** Reboots the domain. */
    void reboot(final Host host) {
        final boolean ret = VIRSH.reboot(host,
                                         getDomainName(),
                                         getVirshOptions());
        if (ret) {
            startShuttingdownIndicator(host);
        }
    }

    /** Suspend down the domain. */
    void suspend(final Host host) {
        final boolean ret = VIRSH.suspend(host,
                                          getDomainName(),
                                          getVirshOptions());
        if (ret) {
            int i = 0;
            mTransitionWriteLock.lock();
            final boolean wasEmpty = suspending.isEmpty();
            suspending.add(host.getName());
            if (!wasEmpty) {
                mTransitionWriteLock.unlock();
                return;
            }
            mTransitionWriteLock.unlock();
            while (!suspending.isEmpty() && i < ACTION_TIMEOUT) {
                getBrowser().periodicalVMSUpdate(host);
                updateParameters();
                getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
                Tools.sleep(1000);
                i++;
                mTransitionReadLock.lock();
                if (suspending.isEmpty() || i >= ACTION_TIMEOUT) {
                    mTransitionReadLock.unlock();
                    break;
                }
                mTransitionReadLock.unlock();
            }
            if (i >= ACTION_TIMEOUT) {
                Tools.appWarning("could not suspend on " + host.getName());
                mTransitionWriteLock.lock();
                try {
                    suspending.clear();
                } finally {
                    mTransitionWriteLock.unlock();
                }
            }
            getBrowser().periodicalVMSUpdate(host);
            updateParameters();
            getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
        }
    }

    /** Resume down the domain. */
    void resume(final Host host) {
        final boolean ret = VIRSH.resume(host,
                                         getDomainName(),
                                         getVirshOptions());
        if (ret) {
            int i = 0;
            mTransitionWriteLock.lock();
            final boolean wasEmpty = resuming.isEmpty();
            resuming.add(host.getName());
            if (!wasEmpty) {
                mTransitionWriteLock.unlock();
                return;
            }
            mTransitionWriteLock.unlock();
            while (!resuming.isEmpty() && i < ACTION_TIMEOUT) {
                getBrowser().periodicalVMSUpdate(host);
                updateParameters();
                getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
                Tools.sleep(1000);
                i++;
                mTransitionReadLock.lock();
                if (resuming.isEmpty() || i >= ACTION_TIMEOUT) {
                    mTransitionReadLock.unlock();
                    break;
                }
                mTransitionReadLock.unlock();
            }
            if (i >= ACTION_TIMEOUT) {
                Tools.appWarning("could not resume on " + host.getName());
                mTransitionWriteLock.lock();
                try {
                    resuming.clear();
                } finally {
                    mTransitionWriteLock.unlock();
                }
            }
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
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return vmsdi;
        }
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                final Enumeration eee = thisNode.children();
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
                thisNode.insert(resource, i);
            }
        });
        getBrowser().reload(thisNode, true);
        vmsdi.selectMyself();
        return vmsdi;
    }

    /** Adds new virtual fs. */
    public VMSFilesystemInfo addFilesystemPanel() {
        final VMSFilesystemInfo vmsdi =
                             new VMSFilesystemInfo(null, getBrowser(), this);
        vmsdi.getResource().setNew(true);
        final DefaultMutableTreeNode resource =
                                           new DefaultMutableTreeNode(vmsdi);
        getBrowser().setNode(resource);
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return vmsdi;
        }
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                final Enumeration eee = thisNode.children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                    if (node.getUserObject() instanceof VMSFilesystemInfo) {
                        i++;
                        continue;
                    }
                    break;
                }
                thisNode.insert(resource, i);
            }
        });
        getBrowser().reload(thisNode, true);
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
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return vmsii;
        }
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                final Enumeration eee = thisNode.children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                    if (node.getUserObject() instanceof VMSDiskInfo
                        || node.getUserObject() instanceof VMSFilesystemInfo
                        || node.getUserObject() instanceof VMSInterfaceInfo) {
                        i++;
                        continue;
                    }
                    break;
                }

                thisNode.insert(resource, i);
            }
        });
        getBrowser().reload(thisNode, true);
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
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return;
        }
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                final Enumeration eee = thisNode.children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                    if (node.getUserObject() instanceof VMSDiskInfo
                        || node.getUserObject() instanceof VMSFilesystemInfo
                        || node.getUserObject() instanceof VMSInterfaceInfo
                        || node.getUserObject() instanceof VMSInputDevInfo) {
                        i++;
                        continue;
                    }
                    break;
                }

                thisNode.insert(resource, i);
            }
        });
        getBrowser().reload(thisNode, true);
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
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return vmsgi;
        }
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                final Enumeration eee = thisNode.children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                    if (node.getUserObject() instanceof VMSDiskInfo
                        || node.getUserObject() instanceof VMSFilesystemInfo
                        || node.getUserObject() instanceof VMSInterfaceInfo
                        || node.getUserObject() instanceof VMSInputDevInfo
                        || node.getUserObject() instanceof VMSGraphicsInfo) {
                        i++;
                        continue;
                    }
                    break;
                }

                thisNode.insert(resource, i);
            }
        });
        getBrowser().reload(thisNode, true);
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
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return;
        }
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                final Enumeration eee = thisNode.children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                    if (node.getUserObject() instanceof VMSDiskInfo
                        || node.getUserObject() instanceof VMSFilesystemInfo
                        || node.getUserObject() instanceof VMSInterfaceInfo
                        || node.getUserObject() instanceof VMSInputDevInfo
                        || node.getUserObject() instanceof VMSGraphicsInfo
                        || node.getUserObject() instanceof VMSSoundInfo) {
                        i++;
                        continue;
                    }
                    break;
                }

                thisNode.insert(resource, i);
            }
        });
        getBrowser().reload(thisNode, true);
        vmssi.selectMyself();
    }

    /** Adds new serial device. */
    void addSerialsPanel() {
        final VMSSerialInfo vmssi = new VMSSerialInfo(null, getBrowser(), this);
        vmssi.getResource().setNew(true);
        final DefaultMutableTreeNode resource =
                                           new DefaultMutableTreeNode(vmssi);
        getBrowser().setNode(resource);
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return;
        }
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                final Enumeration eee = thisNode.children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                    if (node.getUserObject() instanceof VMSDiskInfo
                        || node.getUserObject() instanceof VMSFilesystemInfo
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

                thisNode.insert(resource, i);
            }
        });
        getBrowser().reload(thisNode, true);
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
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return;
        }
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                final Enumeration eee = thisNode.children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                    if (node.getUserObject() instanceof VMSDiskInfo
                        || node.getUserObject() instanceof VMSFilesystemInfo
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

                thisNode.insert(resource, i);
            }
        });
        getBrowser().reload(thisNode, true);
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
        final DefaultMutableTreeNode thisNode = getNode();
        Tools.invokeAndWait(new Runnable() {
            public void run() {
                thisNode.add(resource);
            }
        });
        getBrowser().reload(thisNode, true);
        vmsvi.selectMyself();
    }

    /** Add new hardware. */
    private MyMenu getAddNewHardwareMenu(final String name) {
        return new MyMenu(name,
                          new AccessMode(ConfigData.AccessType.ADMIN, false),
                          new AccessMode(ConfigData.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                return null;
            }

            @Override
            public void update() {
                Tools.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
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
                    @Override
                    public void action() {
                        hidePopup();
                        addDiskPanel();
                    }
                };
                newDiskMenuItem.setPos(pos);
                add(newDiskMenuItem);

                /* fs */
                final MyMenuItem newFilesystemMenuItem = new MyMenuItem(
                   Tools.getString("VMSVirtualDomainInfo.AddNewFilesystem"),
                   BlockDevInfo.HARDDISK_ICON_LARGE,
                   new AccessMode(ConfigData.AccessType.ADMIN, false),
                   new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void action() {
                        hidePopup();
                        addFilesystemPanel();
                    }
                };
                newFilesystemMenuItem.setPos(pos);
                add(newFilesystemMenuItem);

                /* interface */
                final MyMenuItem newInterfaceMenuItem = new MyMenuItem(
                   Tools.getString("VMSVirtualDomainInfo.AddNewInterface"),
                   NetInfo.NET_I_ICON_LARGE,
                   new AccessMode(ConfigData.AccessType.ADMIN, false),
                   new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void action() {
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
                    @Override
                    public void action() {
                        hidePopup();
                        addGraphicsPanel();
                    }
                };
                newGraphicsMenuItem.setPos(pos); add(newGraphicsMenuItem);


                /* input dev */
                final MyMenuItem newInputDevMenuItem = new MyMenuItem(
                   Tools.getString("VMSVirtualDomainInfo.AddNewInputDev"),
                   null,
                   new AccessMode(ConfigData.AccessType.ADMIN, false),
                   new AccessMode(ConfigData.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void action() {
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
                    @Override
                    public void action() {
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
                    @Override
                    public void action() {
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
                    @Override
                    public void action() {
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
                    @Override
                    public void action() {
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

            @Override
            public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && !vmsxml.isRunning(getDomainName());
            }

            @Override
            public String enablePredicate() {
                if (!Tools.getConfigData().isAdvancedMode() && isUsedByCRM()) {
                    return IS_USED_BY_CRM_STRING;
                }
                return null;
            }

            @Override
            public void action() {
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

            @Override
            public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && vmsxml.isRunning(getDomainName());
            }

            @Override
            public String enablePredicate() {
                if (!Tools.getConfigData().isAdvancedMode() && isUsedByCRM()) {
                    return IS_USED_BY_CRM_STRING;
                }
                return null;
            }

            @Override
            public void action() {
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

            @Override
            public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && vmsxml.isRunning(getDomainName());
            }

            @Override
            public String enablePredicate() {
                if (!Tools.getConfigData().isAdvancedMode() && isUsedByCRM()) {
                    return IS_USED_BY_CRM_STRING;
                }
                return null;
            }

            @Override
            public void action() {
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

            @Override
            public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && vmsxml.isSuspended(getDomainName());
            }

            @Override
            public String enablePredicate() {
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

            @Override
            public void action() {
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


            @Override
            public String enablePredicate() {
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

            @Override
            public void action() {
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

            @Override
            public String enablePredicate() {
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

            @Override
            public void action() {
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

            @Override
            public String enablePredicate() {
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

            @Override
            public void action() {
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
    @Override
    public List<UpdatableItem> createPopup() {
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
            @Override
            public String enablePredicate() {
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
                            @Override
                            public boolean predicate() {
                                return !getResource().isNew();
                            }
                            @Override
                            public String enablePredicate() {
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

                            @Override
                            public void action() {
                                hidePopup();
                                removeMyself(false);
                            }
        };
        items.add(removeMenuItem);
        return items;
    }

    /** Returns service icon in the menu. It can be started or stopped. */
    @Override
    public ImageIcon getMenuIcon(final boolean testOnly) {
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null && vmsxml.isRunning(getDomainName())) {
                return HostBrowser.HOST_ON_ICON;
            }
        }
        return HostBrowser.HOST_OFF_ICON;
    }

    /** Returns category icon. */
    @Override
    public ImageIcon getCategoryIcon(final boolean testOnly) {
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

                @Override
                public String enablePredicate() {
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

                @Override
                public void action() {
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

                @Override
                public String enablePredicate() {
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

                @Override
                public void action() {
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

                @Override
                public String enablePredicate() {
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

                @Override
                public void action() {
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
    @Override
    protected String getParamLongDesc(final String param) {
        return SHORTNAME_MAP.get(param);
    }

    /** Returns short description of the specified parameter. */
    @Override
    protected String getParamShortDesc(final String param) {
        return SHORTNAME_MAP.get(param);
    }

    /** Returns preferred value for specified parameter. */
    @Override
    protected String getParamPreferred(final String param) {
        if (preferredEmulator != null
            && VMSXML.VM_PARAM_EMULATOR.equals(param)) {
            return preferredEmulator;
        }
        return PREFERRED_MAP.get(param);
    }

    /** Returns default value for specified parameter. */
    @Override
    protected String getParamDefault(final String param) {
        return DEFAULTS_MAP.get(param);
    }

    /** Returns true if the value of the parameter is ok. */
    @Override
    protected boolean checkParam(final String param, final String newValue) {
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
        } else if (VMSXML.VM_PARAM_DOMAIN_TYPE.equals(param)) {
            final GuiComboBox cb = paramComboBoxGet(param,
                                                      null);
            if (getResource().isNew()
                && !Tools.areEqual(prevType, newValue)) {
                String xenLibPath = "/usr/lib/xen";
                for (final Host host : getBrowser().getClusterHosts()) {
                    final String xlp = host.getXenLibPath();
                    if (xlp != null) {
                        xenLibPath = xlp;
                        break;
                    }
                }
                String lxcLibPath = "/usr/lib/libvirt";
                for (final Host host : getBrowser().getClusterHosts()) {
                    final String llp = host.getLxcLibPath();
                    if (llp != null) {
                        lxcLibPath = llp;
                        break;
                    }
                }
                final GuiComboBox emCB =
                        paramComboBoxGet(VMSXML.VM_PARAM_EMULATOR, "wizard");
                final GuiComboBox loCB =
                            paramComboBoxGet(VMSXML.VM_PARAM_LOADER, "wizard");
                final GuiComboBox voCB =
                     paramComboBoxGet(VMSXML.VM_PARAM_VIRSH_OPTIONS, "wizard");
                final GuiComboBox typeCB =
                             paramComboBoxGet(VMSXML.VM_PARAM_TYPE, "wizard");
                final GuiComboBox inCB =
                             paramComboBoxGet(VMSXML.VM_PARAM_INIT, "wizard");
                if (Tools.areEqual(DOMAIN_TYPE_XEN, newValue)) {
                    if (emCB != null) {
                        emCB.setValue(xenLibPath + "/bin/qemu-dm");
                    }
                    if (loCB != null) {
                        loCB.setValue(xenLibPath + "/boot/hvmloader");
                    }
                    if (voCB != null) {
                        voCB.setValue(VIRSH_OPTION_XEN);
                    }
                    if (typeCB != null) {
                        typeCB.setValue(TYPE_HVM);
                    }
                    if (inCB != null) {
                        inCB.setValue("");
                    }
                } else if (Tools.areEqual(DOMAIN_TYPE_LXC, newValue)) {
                    if (emCB != null) {
                        emCB.setValue(lxcLibPath + "/libvirt_lxc");
                    }
                    if (loCB != null) {
                        loCB.setValue("");
                    }
                    if (voCB != null) {
                        voCB.setValue(VIRSH_OPTION_LXC);
                    }
                    if (typeCB != null) {
                        typeCB.setValue(TYPE_EXE);
                    }
                    if (inCB != null) {
                        inCB.setValue("/bin/sh");
                    }
                } else {
                    if (emCB != null) {
                        emCB.setValue("/usr/bin/kvm");
                    }
                    if (loCB != null) {
                        loCB.setValue("");
                    }
                    if (voCB != null) {
                        voCB.setValue(VIRSH_OPTION_KVM);
                    }
                    if (typeCB != null) {
                        typeCB.setValue(TYPE_HVM);
                    }
                    if (inCB != null) {
                        inCB.setValue("");
                    }
                }
            }
            prevType = cb.getStringValue();
        } else if (VMSXML.VM_PARAM_CPU_MATCH.equals(param)) {
            final boolean match = !"".equals(newValue);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
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
    @Override
    public String[] getParametersFromXML() {
        return VM_PARAMETERS.clone();
    }

    /** Returns possible choices for drop down lists. */
    @Override
    protected Object[] getParamPossibleChoices(final String param) {
        if (VMSXML.VM_PARAM_AUTOSTART.equals(param)) {
            return autostartPossibleValues;
        } else if (VMSXML.VM_PARAM_VIRSH_OPTIONS.equals(param)) {
            return VIRSH_OPTIONS;
        } else if (VMSXML.VM_PARAM_CPUMATCH_MODEL.equals(param)) {
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
    @Override
    protected String getSection(final String param) {
        return SECTION_MAP.get(param);
    }

    /** Returns true if the specified parameter is required. */
    @Override
    protected boolean isRequired(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is integer. */
    @Override
    protected boolean isInteger(final String param) {
        return IS_INTEGER.contains(param);
    }

    /** Returns true if the specified parameter is a label. */
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
    protected GuiComboBox.Type getFieldType(final String param) {
        return FIELD_TYPES.get(param);
    }

    /** Applies the changes. */
    public void apply(final boolean testOnly) {
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
        final String[] params = getParametersFromXML();
        final Map<String, String> parameters = new HashMap<String, String>();
        setName(getComboBoxValue(VMSXML.VM_PARAM_NAME));
        for (final String param : getParametersFromXML()) {
            final String value = getComboBoxValue(param);
            parameters.put(param, value);
            getResource().setValue(param, value);
        }
        final List<Host> definedOnHosts = new ArrayList<Host>();
        final Map<VMSHardwareInfo, Map<String, String>> allModifiedHWP =
                                                      getAllHWParameters(false);
        final Map<VMSHardwareInfo, Map<String, String>> allHWP =
                                                      getAllHWParameters(true);
        final Map<Node, VMSXML> domainNodesToSave = new HashMap<Node, VMSXML>();
        final String clusterName = getBrowser().getCluster().getName();
        getBrowser().vmStatusLock();
        Tools.startProgressIndicator(clusterName, "VM view update");
        for (final Host host : getBrowser().getClusterHosts()) {
            final GuiComboBox hostCB = definedOnHostComboBoxHash.get(
                                                               host.getName());
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final GuiComboBox wizardHostCB =
                                          definedOnHostComboBoxHash.get(
                                               WIZARD_PREFIX + host.getName());
                    if (wizardHostCB != null) {
                        wizardHostCB.setEnabled(false);
                    }
                }
            });
            final String value =
                definedOnHostComboBoxHash.get(host.getName()).getStringValue();
            final boolean needConsole = needConsole();
            if (DEFINED_ON_HOST_TRUE.equals(value)) {
                Node domainNode = null;
                VMSXML vmsxml = null;
                if (getResource().isNew()) {
                    vmsxml = new VMSXML(host);
                    getBrowser().vmsXMLPut(host, vmsxml);
                    domainNode = vmsxml.createDomainXML(getUUID(),
                                                        getDomainName(),
                                                        parameters,
                                                        needConsole);
                    for (final VMSHardwareInfo hi : allHWP.keySet()) {
                        hi.modifyXML(vmsxml,
                                     domainNode,
                                     getDomainName(),
                                     allHWP.get(hi));
                        hi.getResource().setNew(false);
                    }
                    vmsxml.saveAndDefine(domainNode,
                                         getDomainName(),
                                         getVirshOptions());
                } else {
                    vmsxml = getBrowser().getVMSXML(host);
                    if (vmsxml == null) {
                        vmsxml = new VMSXML(host);
                        getBrowser().vmsXMLPut(host, vmsxml);
                    }
                    if (vmsxml.getDomainNames().contains(getDomainName())) {
                        domainNode = vmsxml.modifyDomainXML(getDomainName(),
                                                            parameters);
                        if (domainNode != null) {
                            for (final VMSHardwareInfo hi
                                                   : allModifiedHWP.keySet()) {
                               if (hi.checkResourceFieldsChanged(
                                            null,
                                            hi.getRealParametersFromXML(),
                                            true)) {
                                    hi.modifyXML(
                                        vmsxml,
                                        domainNode,
                                        getDomainName(),
                                        allModifiedHWP.get(hi));
                                    hi.getResource().setNew(false);
                               }
                            }
                        }
                    } else {
                        /* new on this host */
                        domainNode = vmsxml.createDomainXML(getUUID(),
                                                            getDomainName(),
                                                            parameters,
                                                            needConsole);
                        if (domainNode != null) {
                            for (final VMSHardwareInfo hi : allHWP.keySet()) {
                                hi.modifyXML(
                                    vmsxml,
                                    domainNode,
                                    getDomainName(),
                                    allHWP.get(hi));
                                hi.getResource().setNew(false);
                            }
                        }
                    }
                }
                if (domainNode != null) {
                    domainNodesToSave.put(domainNode, vmsxml);
                }
                definedOnHosts.add(host);
            } else {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                if (vmsxml != null
                    && vmsxml.getDomainNames().contains(getDomainName())) {
                    VIRSH.undefine(host, getDomainName(), getVirshOptions());
                }
            }
        }
        for (final Node dn : domainNodesToSave.keySet()) {
            domainNodesToSave.get(dn).saveAndDefine(dn,
                                                    getDomainName(),
                                                    getVirshOptions());
        }
        for (final VMSHardwareInfo hi : allHWP.keySet()) {
            hi.setApplyButtons(null, hi.getRealParametersFromXML());
        }
        if (getResource().isNew()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final DefaultMutableTreeNode thisNode = getNode();
                    if (thisNode != null) {
                        final Enumeration eee = thisNode.children();
                        while (eee.hasMoreElements()) {
                            final DefaultMutableTreeNode node =
                                        (DefaultMutableTreeNode) eee.nextElement();
                            final VMSHardwareInfo vmshi =
                                            (VMSHardwareInfo) node.getUserObject();
                            if (vmshi != null) {
                                final MyButton mb = vmshi.getApplyButton();
                                if (mb != null) {
                                    mb.setVisible(true);
                                }
                            }
                        }
                    }
                }
            });
        }
        VIRSH.setParameters(definedOnHosts.toArray(
                                      new Host[definedOnHosts.size()]),
                            getDomainName(),
                            parameters,
                            getVirshOptions());
        getResource().setNew(false);
        if (!testOnly) {
            storeComboBoxValues(params);
        }
        getBrowser().periodicalVMSUpdate(getBrowser().getClusterHosts());
        updateParameters();
        Tools.stopProgressIndicator(clusterName, "VM view update");
        getBrowser().vmStatusUnlock();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final Host host : getBrowser().getClusterHosts()) {
                    final GuiComboBox hostCB = definedOnHostComboBoxHash.get(
                                                               host.getName());
                    final GuiComboBox wizardHostCB =
                                            definedOnHostComboBoxHash.get(
                                               WIZARD_PREFIX + host.getName());
                    if (wizardHostCB != null) {
                        wizardHostCB.setEnabled(true);
                    }
                }
            }
        });
    }

    /** Returns parameters of all devices. */
    protected Map<VMSHardwareInfo, Map<String, String>> getAllHWParameters(
                                                    final boolean allParams) {
        final Map<VMSHardwareInfo, Map<String, String>> allParamaters =
                         new TreeMap<VMSHardwareInfo, Map<String, String>>();
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return allParamaters;
        }
        final Enumeration e = thisNode.children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode) e.nextElement();
            final VMSHardwareInfo hi = (VMSHardwareInfo) node.getUserObject();
            allParamaters.put(hi, hi.getHWParameters(allParams));
        }
        return allParamaters;
    }

    /** Returns whether this parameter has a unit prefix. */
    @Override
    protected boolean hasUnitPrefix(final String param) {
        return HAS_UNIT_PREFIX.containsKey(param) && HAS_UNIT_PREFIX.get(param);
    }

    /** Returns units. */
    @Override
    protected Unit[] getUnits() {
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
    @Override
    protected String[] getColumnNames(final String tableName) {
        if (HEADER_TABLE.equals(tableName)) {
            return new String[]{"Name", "Defined on", "Status", "Memory", ""};
        } else if (DISK_TABLE.equals(tableName)) {
            return new String[]{"Virtual Device", "Source", ""};
        } else if (FILESYSTEM_TABLE.equals(tableName)) {
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
    @Override
    protected Object[][] getTableData(final String tableName) {
        if (HEADER_TABLE.equals(tableName)) {
            return getMainTableData();
        } else if (DISK_TABLE.equals(tableName)) {
            return getDiskTableData();
        } else if (FILESYSTEM_TABLE.equals(tableName)) {
            return getFilesystemTableData();
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
        for (final Host host : getDefinedOnHosts()) {
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
        if (disks == null) {
            return new Object[0];
        }
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
        final StringBuilder target = new StringBuilder(10);
        target.append(diskData.getTargetBusType());
        target.append(" : /dev/");
        target.append(targetDev);
        if (dkti != null) {
            mDiskToInfoLock.lock();
            final VMSDiskInfo vdi = diskToInfo.get(targetDev);
            mDiskToInfoLock.unlock();
            dkti.put(target.toString(), vdi);
        }
        final MyButton targetDevLabel = new MyButton(
                                    target.toString(),
                                    BlockDevInfo.HARDDISK_ICON_LARGE);
        targetDevLabel.setOpaque(opaque);
        final StringBuilder source = new StringBuilder(20);
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
        mDiskToInfoLock.lock();
        diskKeyToInfo = dkti;
        mDiskToInfoLock.unlock();
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Returns all filesystems. */
    protected Map<String, FilesystemData> getFilesystems() {
        Map<String, FilesystemData> filesystems = null;
        for (final Host host : getDefinedOnHosts()) {
            final VMSXML vxml = getBrowser().getVMSXML(host);
            if (vxml != null) {
                filesystems = vxml.getFilesystems(getDomainName());
                break;
            }
        }
        return filesystems;
    }

    /** Get one row of the table. */
    protected Object[] getFilesystemDataRow(
                                 final String targetDev,
                                 final Map<String, VMSFilesystemInfo> dkti,
                                 final Map<String, FilesystemData> filesystems,
                                 final boolean opaque) {
        if (filesystems == null) {
            return new Object[0];
        }
        final MyButton removeBtn = new MyButton(
                                           "Remove",
                                           ClusterBrowser.REMOVE_ICON_SMALL,
                                           "Remove " + targetDev);
        removeBtn.miniButton();
        final FilesystemData filesystemData = filesystems.get(targetDev);
        if (filesystemData == null) {
            return new Object[]{targetDev,
                                "unknown",
                                removeBtn};
        }
        final StringBuilder target = new StringBuilder(10);
        target.append(filesystemData.getTargetDir());
        if (dkti != null) {
            mFilesystemToInfoLock.lock();
            final VMSFilesystemInfo vdi = filesystemToInfo.get(targetDev);
            mFilesystemToInfoLock.unlock();
            dkti.put(target.toString(), vdi);
        }
        final MyButton targetDevLabel = new MyButton(
                                    target.toString(),
                                    BlockDevInfo.HARDDISK_ICON_LARGE);
        targetDevLabel.setOpaque(opaque);
        final StringBuilder source = new StringBuilder(20);
        String s = filesystemData.getSourceDir();
        if (s == null) {
            s = filesystemData.getSourceDir();
        }
        if (s != null) {
            source.append(filesystemData.getType());
            source.append(" : ");
            source.append(s);
        }
        return new Object[]{targetDevLabel,
                            source.toString(),
                            removeBtn};
    }

    /** Returns data for the fs table. */
    private Object[][] getFilesystemTableData() {
        final List<Object[]> rows = new ArrayList<Object[]>();
        final Map<String, FilesystemData> filesystems = getFilesystems();
        final Map<String, VMSFilesystemInfo> dkti =
                                      new HashMap<String, VMSFilesystemInfo>();
        if (filesystems != null && !filesystems.isEmpty()) {
            for (final String targetDev : filesystems.keySet()) {
                final Object[] row =
                     getFilesystemDataRow(targetDev, dkti, filesystems, false);
                rows.add(row);
            }
        }
        mFilesystemToInfoLock.lock();
        filesystemKeyToInfo = dkti;
        mFilesystemToInfoLock.unlock();
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Get one row of the table. */
    protected Object[] getInterfaceDataRow(
                                final String mac,
                                final Map<String, VMSInterfaceInfo> iToInfo,
                                final Map<String, InterfaceData> interfaces,
                                final boolean opaque) {
        if (interfaces == null) {
            return new Object[0];
        }
        final MyButton removeBtn = new MyButton(
                                           "Remove",
                                           ClusterBrowser.REMOVE_ICON_SMALL,
                                           "Remove " + mac);
        removeBtn.miniButton();
        final InterfaceData interfaceData = interfaces.get(mac);
        if (interfaceData == null) {
            return new Object[]{mac, "unknown", removeBtn};
        }
        final StringBuilder interf = new StringBuilder(20);
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
        final StringBuilder source = new StringBuilder(20);
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
        if (inputDevs == null) {
            return new Object[0];
        }
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
        if (graphicDisplays == null) {
            return new Object[0];
        }
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
        if (sounds == null) {
            return new Object[0];
        }
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
        if (serials == null) {
            return new Object[0];
        }
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
        if (parallels == null) {
            return new Object[0];
        }
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
        if (videos == null) {
            return new Object[0];
        }
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
        for (final Host host : getDefinedOnHosts()) {
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
        mInputDevToInfoLock.lock();
        inputDevKeyToInfo = iToInfo;
        mInputDevToInfoLock.unlock();
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
        mGraphicsToInfoLock.lock();
        graphicsKeyToInfo = iToInfo;
        mGraphicsToInfoLock.unlock();
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
        mSoundToInfoLock.lock();
        soundKeyToInfo = iToInfo;
        mSoundToInfoLock.unlock();
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
        mSerialToInfoLock.lock();
        serialKeyToInfo = iToInfo;
        mSerialToInfoLock.unlock();
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
        mParallelToInfoLock.lock();
        parallelKeyToInfo = iToInfo;
        mParallelToInfoLock.unlock();
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
        mVideoToInfoLock.lock();
        videoKeyToInfo = iToInfo;
        mVideoToInfoLock.unlock();
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Returns all input devices. */
    protected Map<String, InputDevData> getInputDevs() {
        Map<String, InputDevData> inputDevs = null;
        for (final Host host : getDefinedOnHosts()) {
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
        for (final Host host : getDefinedOnHosts()) {
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
        for (final Host host : getDefinedOnHosts()) {
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
        for (final Host host : getDefinedOnHosts()) {
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
        for (final Host host : getDefinedOnHosts()) {
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
        for (final Host host : getDefinedOnHosts()) {
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
        mInterfaceToInfoLock.lock();
        interfaceKeyToInfo = iToInfo;
        mInterfaceToInfoLock.unlock();
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Execute when row in the table was clicked. */
    @Override
    protected void rowClicked(final String tableName,
                              final String key,
                              final int column) {
        if (HEADER_TABLE.equals(tableName)) {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (HEADER_DEFAULT_WIDTHS.containsKey(column)) {
                        removeMyself(false);
                    } else {
                        getBrowser().getVMSInfo().selectMyself();
                    }
                }
            });
            thread.start();
        } else if (DISK_TABLE.equals(tableName)) {
            mDiskToInfoLock.lock();
            final VMSDiskInfo vdi = diskKeyToInfo.get(key);
            mDiskToInfoLock.unlock();
            if (vdi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (DISK_DEFAULT_WIDTHS.containsKey(column)) {
                            vdi.removeMyself(false);
                        } else {
                            vdi.selectMyself();
                        }
                    }
                });
                thread.start();
            }
        } else if (FILESYSTEM_TABLE.equals(tableName)) {
            mFilesystemToInfoLock.lock();
            final VMSFilesystemInfo vfi = filesystemKeyToInfo.get(key);
            mFilesystemToInfoLock.unlock();
            if (vfi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (FILESYSTEM_DEFAULT_WIDTHS.containsKey(column)) {
                            vfi.removeMyself(false);
                        } else {
                            vfi.selectMyself();
                        }
                    }
                });
                thread.start();
            }
        } else if (INTERFACES_TABLE.equals(tableName)) {
            mInterfaceToInfoLock.lock();
            final VMSInterfaceInfo vii = interfaceKeyToInfo.get(key);
            mInterfaceToInfoLock.unlock();
            if (vii != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
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
            mInputDevToInfoLock.lock();
            final VMSInputDevInfo vidi = inputDevKeyToInfo.get(key);
            mInputDevToInfoLock.unlock();
            if (vidi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
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
            mGraphicsToInfoLock.lock();
            final VMSGraphicsInfo vgi = graphicsKeyToInfo.get(key);
            mGraphicsToInfoLock.unlock();
            if (vgi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
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
            mSoundToInfoLock.lock();
            final VMSSoundInfo vsi = soundKeyToInfo.get(key);
            mSoundToInfoLock.unlock();
            if (vsi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
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
            mSerialToInfoLock.lock();
            final VMSSerialInfo vsi = serialKeyToInfo.get(key);
            mSerialToInfoLock.unlock();
            if (vsi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
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
            mParallelToInfoLock.lock();
            final VMSParallelInfo vpi = parallelKeyToInfo.get(key);
            mParallelToInfoLock.unlock();
            if (vpi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
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
            mVideoToInfoLock.lock();
            final VMSVideoInfo vvi = videoKeyToInfo.get(key);
            mVideoToInfoLock.unlock();
            if (vvi != null) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
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
    @Override
    protected Color getTableRowColor(final String tableName, final String key) {
        if (HEADER_TABLE.equals(tableName)) {
            return rowColor;
        }
        return Browser.PANEL_BACKGROUND;
    }

    /** Alignment for the specified column. */
    @Override
    protected int getTableColumnAlignment(final String tableName,
                                          final int column) {

        if (column == 3 && HEADER_TABLE.equals(tableName)) {
            return SwingConstants.RIGHT;
        }
        return SwingConstants.LEFT;
    }

    /** Returns info object for this row. */
    @Override
    protected Info getTableInfo(final String tableName, final String key) {
        if (HEADER_TABLE.equals(tableName)) {
            return this;
        } else if (DISK_TABLE.equals(tableName)) {
            mDiskToInfoLock.lock();
            final Info info = diskToInfo.get(key);
            mDiskToInfoLock.unlock();
            return info;
        } else if (FILESYSTEM_TABLE.equals(tableName)) {
            mFilesystemToInfoLock.lock();
            final Info info = filesystemToInfo.get(key);
            mFilesystemToInfoLock.unlock();
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
        mDiskToInfoLock.lock();
        final boolean is = diskToInfo.containsKey(dev);
        mDiskToInfoLock.unlock();
        return is;
    }

    /** Returns whether this parameter is advanced. */
    @Override
    protected boolean isAdvanced(final String param) {
        if (!getResource().isNew() && VMSXML.VM_PARAM_NAME.equals(param)) {
            return true;
        }
        return IS_ADVANCED.contains(param);
    }

    /** Whether the parameter should be enabled. */
    @Override
    protected String isEnabled(final String param) {
        final String libvirtVersion =
                            getBrowser().getCluster().getMinLibvirtVersion();
        if (!getResource().isNew() && VMSXML.VM_PARAM_NAME.equals(param)) {
            return "";
        }
        if (REQUIRED_VERSION.containsKey(param)) {
            final String rv = REQUIRED_VERSION.get(param);
            try {
                if (Tools.compareVersions(rv, libvirtVersion) > 0) {
                    return Tools.getString(
                                    "VMSVirtualDomainInfo.AvailableInVersion")
                                        .replace("@VERSION@", rv);
                }
            } catch (Exceptions.IllegalVersionException e) {
                Tools.appWarning(e.getMessage(), e);
                return Tools.getString(
                                "VMSVirtualDomainInfo.AvailableInVersion")
                                    .replace("@VERSION@", rv);
            }
        }
        return null;
    }

    /** Whether the parameter should be enabled only in advanced mode. */
    @Override
    protected boolean isEnabledOnlyInAdvancedMode(final String param) {
         if (VMSXML.VM_PARAM_MEMORY.equals(param)) {
             return true;
         }
         return false;
    }


    /** Returns access type of this parameter. */
    @Override
    protected ConfigData.AccessType getAccessType(final String param) {
        return ConfigData.AccessType.ADMIN;
    }

    /** Returns the regexp of the parameter. */
    @Override
    protected String getParamRegexp(final String param) {
        if (VMSXML.VM_PARAM_NAME.equals(param)) {
            return "^[\\w-]+$";
        } else {
            return super.getParamRegexp(param);
        }
    }

    /**
     * Returns whether the specified parameter or any of the parameters
     * have changed.
     */
    @Override
    public boolean checkResourceFieldsChanged(final String param,
                                              final String[] params) {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
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
        final Enumeration eee = thisNode.children();
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
    @Override
    public boolean checkResourceFieldsCorrect(final String param,
                                              final String[] params) {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
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
        final Enumeration eee = thisNode.children();
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
    @Override
    protected GuiComboBox getParamComboBox(final String param,
                                           final String prefix,
                                           final int width) {
        final GuiComboBox paramCB =
                                 super.getParamComboBox(param, prefix, width);
        if (VMSXML.VM_PARAM_BOOT.equals(param)
            || VMSXML.VM_PARAM_BOOT_2.equals(param)) {
            paramCB.setAlwaysEditable(false);
        }
        return paramCB;
    }

    /** Removes this domain. */
    @Override
    public void removeMyself(final boolean testOnly) {
        if (getResource().isNew()) {
            super.removeMyself(testOnly);
            getResource().setNew(false);
            removeNode();
            return;
        }
        String desc = Tools.getString(
                            "VMSVirtualDomainInfo.confirmRemove.Description");

        String dn = getDomainName();
        if (dn == null) {
            dn = "";
        }
        desc  = desc.replaceAll("@DOMAIN@", Matcher.quoteReplacement(dn));
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
                VIRSH.undefine(h, getDomainName(), getVirshOptions());
            }
        }
        getBrowser().periodicalVMSUpdate(getBrowser().getClusterHosts());
        removeNode();
    }

    /** Returns whether the column is a button, 0 column is always a button. */
    @Override
    protected Map<Integer, Integer> getDefaultWidths(final String tableName) {
        if (HEADER_TABLE.equals(tableName)) {
            return HEADER_DEFAULT_WIDTHS;
        } else if (DISK_TABLE.equals(tableName)) {
            return DISK_DEFAULT_WIDTHS;
        } else if (FILESYSTEM_TABLE.equals(tableName)) {
            return FILESYSTEM_DEFAULT_WIDTHS;
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
    @Override
    protected boolean isControlButton(final String tableName,
                                      final int column) {
        if (HEADER_TABLE.equals(tableName)) {
            return HEADER_DEFAULT_WIDTHS.containsKey(column);
        } else if (DISK_TABLE.equals(tableName)) {
            return DISK_DEFAULT_WIDTHS.containsKey(column);
        } else if (FILESYSTEM_TABLE.equals(tableName)) {
            return FILESYSTEM_DEFAULT_WIDTHS.containsKey(column);
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
    @Override
    protected String getTableToolTip(final String tableName,
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
        } else if (FILESYSTEM_TABLE.equals(tableName)) {
            if (FILESYSTEM_DEFAULT_WIDTHS.containsKey(column)) {
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
                @Override
                public void run() {
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
                @Override
                public void run() {
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
                @Override
                public void run() {
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
    @Override
    public void revert() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return;
        }
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
        final Enumeration eee = thisNode.children();
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
            if (preferredEmulator != null
                && VMSXML.VM_PARAM_EMULATOR.equals(pv)) {
                getResource().setValue(pv, preferredEmulator);
            } else {
                getResource().setValue(pv, PREFERRED_MAP.get(pv));
            }
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

    /** Returns UUID. */
    public String getUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        return uuid;
    }

    /** Return virsh options like -c xen:/// */
    public final String getVirshOptions() {
        return getResource().getValue(VMSXML.VM_PARAM_VIRSH_OPTIONS);
    }

    /** Return whether domain type needs "display" section. */
    public final boolean needDisplay() {
        return NEED_DISPLAY.contains(paramComboBoxGet(
                        VMSXML.VM_PARAM_DOMAIN_TYPE, null).getStringValue());
    }

    /** Return whether domain type needs "console" section. */
    public final boolean needConsole() {
        return NEED_CONSOLE.contains(paramComboBoxGet(
                        VMSXML.VM_PARAM_DOMAIN_TYPE, null).getStringValue());
    }

    /** Return whether domain type needs filesystem instead of disk device. */
    public final boolean needFilesystem() {
        return NEED_FILESYSTEM.contains(paramComboBoxGet(
                        VMSXML.VM_PARAM_DOMAIN_TYPE, null).getStringValue());
    }
}
