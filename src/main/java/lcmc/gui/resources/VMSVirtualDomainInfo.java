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

import lcmc.data.*;
import lcmc.gui.Browser;
import lcmc.gui.HostBrowser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.gui.SpringUtilities;
import lcmc.data.VMSXML.DiskData;
import lcmc.data.VMSXML.FilesystemData;
import lcmc.data.VMSXML.InterfaceData;
import lcmc.data.VMSXML.InputDevData;
import lcmc.data.VMSXML.GraphicsData;
import lcmc.data.VMSXML.SoundData;
import lcmc.data.VMSXML.SerialData;
import lcmc.data.VMSXML.ParallelData;
import lcmc.data.VMSXML.VideoData;
import lcmc.data.resources.Resource;
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
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.SpringLayout;
import java.util.*;
import java.util.regex.Matcher;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;

import org.w3c.dom.Node;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

import lcmc.gui.widget.Check;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class holds info about VirtualDomain service in the VMs category,
 * but not in the cluster view.
 */
public final class VMSVirtualDomainInfo extends EditableInfo {
    /** Logger. */
    private static final Logger LOG =
                          LoggerFactory.getLogger(VMSVirtualDomainInfo.class);
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
    private volatile Value prevType = null;
    /** Autostart possible values - hosts. */
    private final Value[] autostartPossibleValues;
    /** Timeout of starting, shutting down, etc. actions in seconds. */
    private static final int ACTION_TIMEOUT = 20;
    /** Virsh options. */
    private static final Value VIRSH_OPTION_KVM    = new StringValue();
    private static final Value VIRSH_OPTION_XEN    = new StringValue("-c 'xen:///'");
    private static final Value VIRSH_OPTION_LXC    = new StringValue("-c 'lxc:///'");
    private static final Value VIRSH_OPTION_VBOX   = new StringValue("-c 'vbox:///session'");
    private static final Value VIRSH_OPTION_OPENVZ = new StringValue("-c 'openvz:///system'");
    private static final Value VIRSH_OPTION_UML    = new StringValue("-c 'uml:///system'");
    /** Domain types. */
    static final String DOMAIN_TYPE_KVM = "kvm";
    private static final String DOMAIN_TYPE_XEN    = "xen";
    private static final String DOMAIN_TYPE_LXC    = "lxc";
    private static final String DOMAIN_TYPE_VBOX   = "vbox";
    private static final String DOMAIN_TYPE_OPENVZ = "openvz";
    private static final String DOMAIN_TYPE_UML    = "uml";



    private static final Value[] VIRSH_OPTIONS = new Value[]{
                                                            VIRSH_OPTION_KVM,
                                                            VIRSH_OPTION_XEN,
                                                            VIRSH_OPTION_LXC,
                                                            VIRSH_OPTION_VBOX,
                                                            VIRSH_OPTION_OPENVZ,
                                                            VIRSH_OPTION_UML};

    /** Whether it needs "display" section. */
    private static final Set<String> NEED_DISPLAY =
         Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
                                                            DOMAIN_TYPE_KVM,
                                                            DOMAIN_TYPE_XEN,
                                                            DOMAIN_TYPE_VBOX)));
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
                                                            DOMAIN_TYPE_VBOX)));
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
                                    VMSXML.VM_PARAM_CLOCK_OFFSET,
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
    private static final Collection<String> IS_ADVANCED =
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
    private static final Map<String, Value> DEFAULTS_MAP =
                                                 new HashMap<String, Value>();
    /** Preferred values. */
    private static final Map<String, Value> PREFERRED_MAP =
                                                 new HashMap<String, Value>();
    /** Types of some of the field. */
    private static final Map<String, Widget.Type> FIELD_TYPES =
                                       new HashMap<String, Widget.Type>();
    /** Possible values for some fields. */
    private static final Map<String, Value[]> POSSIBLE_VALUES =
                                          new HashMap<String, Value[]>();
    /** Whether parameter is an integer. */
    private static final Collection<String> IS_INTEGER = new ArrayList<String>();
    /** Required version for a parameter. */
    private static final Map<String, String> REQUIRED_VERSION =
                                                new HashMap<String, String>();
    /** Returns whether this parameter has a unit prefix. */
    private static final Map<String, Boolean> HAS_UNIT_PREFIX =
                                               new HashMap<String, Boolean>();
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
    
    private static final Value VM_TRUE = new StringValue("True");
    
    private static final Value VM_FALSE = new StringValue("False");
    
    /** Defined on host string value. */
    private static final Value DEFINED_ON_HOST_TRUE = VM_TRUE;
    /** Not defined on host string value. */
    private static final Value DEFINED_ON_HOST_FALSE = VM_FALSE;
    /** Wizard prefix string. */
    private static final String WIZARD_HOST_PREFIX = Widget.WIZARD_PREFIX + ':';
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
            Collections.unmodifiableMap(new HashMap<Integer, Integer>() {
                    private static final long serialVersionUID = 1L;
                /* remove button column */
            {
                put(4, CONTROL_BUTTON_WIDTH);
            }});
    /** Default widths for columns. */
    private static final Map<Integer, Integer> DISK_DEFAULT_WIDTHS =
            Collections.unmodifiableMap(new HashMap<Integer, Integer>() {
                    private static final long serialVersionUID = 1L;
            {
                put(2, CONTROL_BUTTON_WIDTH);
            }});
    /** Default widths for columns. */
    private static final Map<Integer, Integer> FILESYSTEM_DEFAULT_WIDTHS =
            Collections.unmodifiableMap(new HashMap<Integer, Integer>() {
                    private static final long serialVersionUID = 1L;
            {
                put(2, CONTROL_BUTTON_WIDTH);
            }});
    /** Default widths for columns. */
    private static final Map<Integer, Integer> INTERFACES_DEFAULT_WIDTHS =
            Collections.unmodifiableMap(new HashMap<Integer, Integer>() {
                    private static final long serialVersionUID = 1L;
            {
                put(2, CONTROL_BUTTON_WIDTH);
            }});
    /** Default widths for columns. */
    private static final Map<Integer, Integer> INPUTDEVS_DEFAULT_WIDTHS =
            Collections.unmodifiableMap(new HashMap<Integer, Integer>() {
                    private static final long serialVersionUID = 1L;
            {
                put(1, CONTROL_BUTTON_WIDTH);
            }});
    /** Default widths for columns. */
    private static final Map<Integer, Integer> GRAPHICS_DEFAULT_WIDTHS =
            Collections.unmodifiableMap(new HashMap<Integer, Integer>() {
                    private static final long serialVersionUID = 1L;
            {
                put(1, CONTROL_BUTTON_WIDTH);
            }});
    /** Default widths for columns. */
    private static final Map<Integer, Integer> SOUND_DEFAULT_WIDTHS =
            Collections.unmodifiableMap(new HashMap<Integer, Integer>() {
                    private static final long serialVersionUID = 1L;
            {
                put(1, CONTROL_BUTTON_WIDTH);
            }});
    /** Default widths for columns. */
    private static final Map<Integer, Integer> SERIAL_DEFAULT_WIDTHS =
            Collections.unmodifiableMap(new HashMap<Integer, Integer>() {
                    private static final long serialVersionUID = 1L;
            {
                put(1, CONTROL_BUTTON_WIDTH);
            }});
    /** Default widths for columns. */
    private static final Map<Integer, Integer> PARALLEL_DEFAULT_WIDTHS =
            Collections.unmodifiableMap(new HashMap<Integer, Integer>() {
                    private static final long serialVersionUID = 1L;
            {
                put(1, CONTROL_BUTTON_WIDTH);
            }});
    /** Default widths for columns. */
    private static final Map<Integer, Integer> VIDEO_DEFAULT_WIDTHS =
            Collections.unmodifiableMap(new HashMap<Integer, Integer>() {
                    private static final long serialVersionUID = 1L;
            {
                put(1, CONTROL_BUTTON_WIDTH);
            }});
    /** Type HVM. */
    private static final Value TYPE_HVM = new StringValue("hvm");
    /** Type Linux. */
    private static final Value TYPE_LINUX = new StringValue("linux");
    /** Type exe. */
    private static final Value TYPE_EXE = new StringValue("exe");
    /** Type UML. */
    private static final Value TYPE_UML = new StringValue("uml");

    private static final Value NO_SELECTION_VALUE = new StringValue();
    
    /** Width of the button field. */
    private static final int CONTROL_BUTTON_WIDTH = 80;
    /** String that is displayed as a tool tip if a menu item is used by CRM. */
    static final String IS_USED_BY_CRM_STRING = "it is used by cluster manager";
    /** This is a map from host to the check box. */
    private final Map<String, Widget> definedOnHostComboBoxHash =
                                          new HashMap<String, Widget>();

    public static final Value BOOT_HD      = new StringValue("hd", "Hard Disk");
    public static final Value BOOT_NETWORK =
                                    new StringValue("network", "Network (PXE)");
    public static final Value BOOT_CDROM   = new StringValue("cdrom", "CD-ROM");
    public static final Value BOOT_FD      = new StringValue("fd", "Floppy");

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

        SECTION_MAP.put(VMSXML.VM_PARAM_CLOCK_OFFSET,   VIRTUAL_SYSTEM_STRING);

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
                   VMSXML.VM_PARAM_CLOCK_OFFSET,
                   Tools.getString("VMSVirtualDomainInfo.Short.Clock.Offset"));
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
                        Widget.Type.TEXTFIELDWITHUNIT);
        FIELD_TYPES.put(VMSXML.VM_PARAM_MEMORY, Widget.Type.TEXTFIELDWITHUNIT);
        FIELD_TYPES.put(VMSXML.VM_PARAM_APIC, Widget.Type.CHECKBOX);
        FIELD_TYPES.put(VMSXML.VM_PARAM_ACPI, Widget.Type.CHECKBOX);
        FIELD_TYPES.put(VMSXML.VM_PARAM_PAE, Widget.Type.CHECKBOX);
        FIELD_TYPES.put(VMSXML.VM_PARAM_HAP, Widget.Type.CHECKBOX);

        PREFERRED_MAP.put(VMSXML.VM_PARAM_CURRENTMEMORY,
                          new StringValue("512", VMSXML.getUnitMiBytes()));
        PREFERRED_MAP.put(VMSXML.VM_PARAM_MEMORY,
                          new StringValue("512", VMSXML.getUnitMiBytes()));
        PREFERRED_MAP.put(VMSXML.VM_PARAM_TYPE, TYPE_HVM);
        PREFERRED_MAP.put(VMSXML.VM_PARAM_TYPE_ARCH, new StringValue("x86_64"));
        PREFERRED_MAP.put(VMSXML.VM_PARAM_TYPE_MACHINE, new StringValue("pc"));
        PREFERRED_MAP.put(VMSXML.VM_PARAM_ACPI, VM_TRUE);
        PREFERRED_MAP.put(VMSXML.VM_PARAM_APIC, VM_TRUE);
        PREFERRED_MAP.put(VMSXML.VM_PARAM_PAE, VM_TRUE);
        PREFERRED_MAP.put(VMSXML.VM_PARAM_CLOCK_OFFSET, new StringValue("utc"));
        PREFERRED_MAP.put(VMSXML.VM_PARAM_ON_POWEROFF, new StringValue("destroy"));
        PREFERRED_MAP.put(VMSXML.VM_PARAM_ON_REBOOT, new StringValue("restart"));
        PREFERRED_MAP.put(VMSXML.VM_PARAM_ON_CRASH, new StringValue("restart"));
        PREFERRED_MAP.put(VMSXML.VM_PARAM_EMULATOR, new StringValue("/usr/bin/kvm"));
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_AUTOSTART, null);
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_VIRSH_OPTIONS, NO_SELECTION_VALUE);
        //DEFAULTS_MAP.put(VMSXML.VM_PARAM_BOOT, new StringValue("hd"));
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_BOOT, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_DOMAIN_TYPE, new StringValue(DOMAIN_TYPE_KVM));
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_VCPU, new StringValue("1"));
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_ACPI, VM_FALSE);
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_APIC, VM_FALSE);
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_PAE, VM_FALSE);
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_HAP, VM_FALSE);

        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPU_MATCH, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPUMATCH_MODEL, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPUMATCH_VENDOR, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_CORES, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPUMATCH_FEATURE_POLICY, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_CPUMATCH_FEATURES, NO_SELECTION_VALUE);

        HAS_UNIT_PREFIX.put(VMSXML.VM_PARAM_MEMORY, true);
        HAS_UNIT_PREFIX.put(VMSXML.VM_PARAM_CURRENTMEMORY, true);

        // TODO: no virsh command for os-boot
        POSSIBLE_VALUES.put(
                      VMSXML.VM_PARAM_BOOT,
                      new Value[]{BOOT_HD, BOOT_NETWORK, BOOT_CDROM, BOOT_FD});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_BOOT_2,
                            new Value[]{new StringValue(),
                                        BOOT_HD,
                                        BOOT_NETWORK,
                                        BOOT_CDROM,
                                        BOOT_FD});

        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_LOADER, new Value[]{});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_DOMAIN_TYPE,
                            new Value[]{new StringValue(DOMAIN_TYPE_KVM),
                                        new StringValue(DOMAIN_TYPE_XEN),
                                        new StringValue(DOMAIN_TYPE_LXC),
                                        new StringValue(DOMAIN_TYPE_OPENVZ),
                                        new StringValue(DOMAIN_TYPE_VBOX),
                                        new StringValue(DOMAIN_TYPE_UML),
                                        });
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_BOOTLOADER,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("/usr/bin/pygrub")});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_TYPE,
                            new Value[]{TYPE_HVM, TYPE_LINUX, TYPE_EXE});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_TYPE_ARCH,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("x86_64"),
                                        new StringValue("i686")});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_TYPE_MACHINE,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("pc"),
                                        new StringValue("pc-0.12")});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_INIT,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("/bin/sh"),
                                        new StringValue("/init")});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_CLOCK_OFFSET,
                            new Value[]{new StringValue("utc"),
                                        new StringValue("localtime")});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_ON_POWEROFF,
                            new Value[]{new StringValue("destroy"),
                                        new StringValue("restart"),
                                        new StringValue("preserve"),
                                        new StringValue("rename-restart")});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_ON_REBOOT,
                            new Value[]{new StringValue("restart"),
                                        new StringValue("destroy"),
                                        new StringValue("preserve"),
                                        new StringValue("rename-restart")});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_ON_CRASH,
                            new Value[]{new StringValue("restart"),
                                         new StringValue("destroy"),
                                         new StringValue("preserve"),
                                         new StringValue("rename-restart"),
                                         new StringValue("coredump-destroy"), /* since 0.8.4 */
                                         new StringValue("coredump-restart")}); /* since 0.8.4*/
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_EMULATOR,
                            new Value[]{new StringValue("/usr/bin/kvm"),
                                        new StringValue("/usr/bin/qemu")});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_CPU_MATCH,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("exact"),
                                        new StringValue("minimum"),
                                        new StringValue("strict")});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("1"),
                                        new StringValue("2")});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_CORES,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("1"),
                                        new StringValue("2"),
                                        new StringValue("4"),
                                        new StringValue("8")});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("1"),
                                        new StringValue("2")});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_CPUMATCH_FEATURE_POLICY,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("force"),
                                        new StringValue("require"),
                                        new StringValue("optional"),
                                        new StringValue("disable"),
                                        new StringValue("forbid")});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_CPUMATCH_FEATURES,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("aes"),
                                        new StringValue("aes apic")});
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
        final List<Value> hostsList = new ArrayList<Value>();
        hostsList.add(null);
        for (final Host h : getBrowser().getClusterHosts()) {
            hostsList.add(new StringValue(h.getName()));
        }
        autostartPossibleValues =
                              hostsList.toArray(new Value[hostsList.size()]);

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
        final Collection<String> diskNames  = new ArrayList<String>();
        if (disks != null) {
            for (final String d : disks.keySet()) {
                diskNames.add(d);
            }
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> e = thisNode.children();
        boolean nodeChanged = false;
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node = e.nextElement();
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
        Tools.isSwingThread();
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String disk : diskNames) {
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> eee = thisNode.children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node = eee.nextElement();
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
        final Collection<String> filesystemNames  = new ArrayList<String>();
        if (filesystems != null) {
            for (final String d : filesystems.keySet()) {
                filesystemNames.add(d);
            }
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> e = thisNode.children();
        boolean nodeChanged = false;
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node = e.nextElement();
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
        Tools.isSwingThread();
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String filesystem : filesystemNames) {
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> eee = thisNode.children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node = eee.nextElement();
                if (!(node.getUserObject() instanceof VMSFilesystemInfo)) {
                    continue;
                }
                final VMSFilesystemInfo v =
                                      (VMSFilesystemInfo) node.getUserObject();
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
        final Collection<String> interfaceNames  = new ArrayList<String>();
        if (interfaces != null) {
            for (final String i : interfaces.keySet()) {
                interfaceNames.add(i);
            }
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> e = thisNode.children();
        boolean nodeChanged = false;
        VMSInterfaceInfo emptySlot = null; /* for generated mac address. */
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node = e.nextElement();
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
        Tools.isSwingThread();
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String interf : interfaceNames) {
            final VMSInterfaceInfo vmsii;
            if (emptySlot == null) {
                @SuppressWarnings("unchecked")
                final Enumeration<DefaultMutableTreeNode> eee =
                                                           thisNode.children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node = eee.nextElement();
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
        final Collection<String> inputDevNames  = new ArrayList<String>();
        if (inputDevs != null) {
            for (final String d : inputDevs.keySet()) {
                inputDevNames.add(d);
            }
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> e = thisNode.children();
        boolean nodeChanged = false;
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node = e.nextElement();
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
        Tools.isSwingThread();
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String inputDev : inputDevNames) {
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> eee = thisNode.children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node = eee.nextElement();
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
            final VMSInputDevInfo vmsid = new VMSInputDevInfo(inputDev, getBrowser(), this);
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
        final Collection<String> graphicsNames  = new ArrayList<String>();
        if (graphicDisplays != null) {
            for (final String d : graphicDisplays.keySet()) {
                graphicsNames.add(d);
            }
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> e = thisNode.children();
        boolean nodeChanged = false;
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node = e.nextElement();
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
        Tools.isSwingThread();
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String graphicDisplay : graphicsNames) {
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> eee = thisNode.children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node = eee.nextElement();
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
            final VMSGraphicsInfo vmsgi = new VMSGraphicsInfo(graphicDisplay, getBrowser(), this);
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
        final Collection<String> soundNames  = new ArrayList<String>();
        if (sounds != null) {
            for (final String d : sounds.keySet()) {
                soundNames.add(d);
            }
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> e = thisNode.children();
        boolean nodeChanged = false;
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node = e.nextElement();
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
        Tools.isSwingThread();
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String sound : soundNames) {
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> eee = thisNode.children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node = eee.nextElement();
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
            final VMSSoundInfo vmssi = new VMSSoundInfo(sound, getBrowser(), this);
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
        final Collection<String> serialNames  = new ArrayList<String>();
        if (serials != null) {
            for (final String d : serials.keySet()) {
                serialNames.add(d);
            }
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> e = thisNode.children();
        boolean nodeChanged = false;
        VMSSerialInfo emptySlot = null; /* for generated target port. */
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node = e.nextElement();
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
        Tools.isSwingThread();
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String serial : serialNames) {
            final VMSSerialInfo vmssi;
            if (emptySlot == null) {
                @SuppressWarnings("unchecked")
                final Enumeration<DefaultMutableTreeNode> eee =
                                                           thisNode.children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node = eee.nextElement();
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
        final Collection<String> parallelNames  = new ArrayList<String>();
        if (parallels != null) {
            for (final String d : parallels.keySet()) {
                parallelNames.add(d);
            }
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> e = thisNode.children();
        boolean nodeChanged = false;
        VMSParallelInfo emptySlot = null; /* for generated target port. */
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node = e.nextElement();
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
        Tools.isSwingThread();
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String parallel : parallelNames) {
            final VMSParallelInfo vmspi;
            if (emptySlot == null) {
                @SuppressWarnings("unchecked")
                final Enumeration<DefaultMutableTreeNode> eee =
                                                           thisNode.children();
                int i = 0;
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node = eee.nextElement();
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
        final Collection<String> videoNames  = new ArrayList<String>();
        if (videos != null) {
            for (final String d : videos.keySet()) {
                videoNames.add(d);
            }
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove =
                                    new ArrayList<DefaultMutableTreeNode>();
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> e = thisNode.children();
        boolean nodeChanged = false;
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node = e.nextElement();
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
        Tools.isSwingThread();
        for (final DefaultMutableTreeNode node : nodesToRemove) {
            node.removeFromParent();
        }

        for (final String video : videoNames) {
            @SuppressWarnings("unchecked")
            final Enumeration<DefaultMutableTreeNode> eee = thisNode.children();
            int i = 0;
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node = eee.nextElement();
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
            final VMSVideoInfo vmspi = new VMSVideoInfo(video, getBrowser(), this);
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
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
            @Override
            public void run() {
                hBtn.setBackgroundColor(Browser.PANEL_BACKGROUND);
            }
        });
        hostBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                LOG.debug1("actionPerformed: BUTTON: host: " + host.getName());
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
                                Tools.invokeLater(new Runnable() {
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
            hostButtons.put(prefix + ':' + host.getName(), hostBtn);
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
            if (vmsxml != null
                && vmsxml.getDomainNames().contains(getDomainName())) {
                if (vmsxml.isRunning(getDomainName())) {
                    if (vmsxml.isSuspended(getDomainName())) {
                        suspendedOnHosts.add(hostName);
                        mTransitionWriteLock.lock();
                        try {
                            suspending.remove(hostName);
                        } finally {
                            mTransitionWriteLock.unlock();
                        }
                    } else {
                        mTransitionWriteLock.lock();
                        try {
                            resuming.remove(hostName);
                        } finally {
                            mTransitionWriteLock.unlock();
                        }
                    }
                    runningOnHosts.add(hostName);
                    mTransitionWriteLock.lock();
                    try {
                        starting.remove(hostName);
                    } finally {
                        mTransitionWriteLock.unlock();
                    }
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
            try {
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
            } finally {
                mTransitionReadLock.unlock();
            }
        }
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            final Widget hwi = definedOnHostComboBoxHash.get(h.getName());
            if (hwi != null) {
                final Value value;
                if ((vmsxml != null
                        && vmsxml.getDomainNames().contains(getDomainName()))) {
                    value = DEFINED_ON_HOST_TRUE;
                } else {
                    value = DEFINED_ON_HOST_FALSE;
                }
                hwi.setValue(value);
            }
        }
        for (final String param : getParametersFromXML()) {
            final Value oldValue = getParamSaved(param);
            Value value = null;
            final Widget wi = getWidget(param, null);
            for (final Host h : getDefinedOnHosts()) {
                final VMSXML vmsxml = getBrowser().getVMSXML(h);
                if (vmsxml != null && value == null) {
                    final Value savedValue;
                    if (VMSXML.VM_PARAM_CURRENTMEMORY.equals(param)
                        || VMSXML.VM_PARAM_MEMORY.equals(param)) {
                        savedValue = VMSXML.convertKilobytes(
                                      vmsxml.getValue(getDomainName(), param));
                    } else {
                        savedValue = new StringValue(
                                      vmsxml.getValue(getDomainName(), param));
                    }
                    if (savedValue == null || savedValue.isNothingSelected()) {
                        value = getParamDefault(param);
                    } else {
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
        for (final Host h : getDefinedOnHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null) {
                uuid = vmsxml.getValue(getDomainName(), VMSXML.VM_PARAM_UUID);
            }
        }
        Tools.invokeAndWait(new Runnable() {
            @Override
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
        Tools.invokeLater(new Runnable() {
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
            final VMSXML vmsxml = getBrowser().getVMSXML(host);
            if (vmsxml != null && vmsxml.isRunning(getDomainName())) {
                running = true;
            }
            if (vmsxml != null && !vmsxml.getDomainNames().contains(
                                                            getDomainName())) {
                final boolean notDefined = false;
            }
            final Value defaultValue;
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
            final Widget wi = WidgetFactory.createInstance(
                                        Widget.Type.CHECKBOX,
                                        defaultValue,
                                        Widget.NO_ITEMS,
                                        Widget.NO_REGEXP,
                                        ClusterBrowser.SERVICE_FIELD_WIDTH * 2,
                                        Widget.NO_ABBRV,
                                        new AccessMode(
                                          Application.AccessType.ADMIN,
                                          false),
                                        hostBtn);
            Widget rpwi = null;
            if (prefix == null) {
                definedOnHostComboBoxHash.put(host.getName(), wi);
            } else {
                definedOnHostComboBoxHash.put(prefix + ':' + host.getName(),
                                              wi);
                rpwi = definedOnHostComboBoxHash.get(host.getName());
            }
            final Widget realParamWi = rpwi;
            if (!host.isConnected()) {
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        wi.setEnabled(false);
                    }
                });
            }
            wi.addListeners(
                        new WidgetListener() {
                            @Override
                            public void check(final Value value) {
                                checkParameterFields(wi,
                                                     realParamWi,
                                                     ServiceInfo.CACHED_FIELD,
                                                     getParametersFromXML(),
                                                     thisApplyButton);
                            }
                        });
            wi.setBackgroundColor(ClusterBrowser.PANEL_BACKGROUND);
            final JLabel label = new JLabel(host.getName());
            wi.setLabel(label, null);
            addField(hostPanel,
                     label,
                     wi.getComponent(),
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
        Tools.isSwingThread();
        if (infoPanel != null) {
            return infoPanel;
        }
        final boolean abExisted = getApplyButton() != null;
        /* main, button and options panels */
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
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
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);

        final String[] params = getParametersFromXML();
        initApplyButton(null);
        /* add item listeners to the apply button. */
        if (!abExisted) {
            getApplyButton().addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        LOG.debug1("actionPerformed: BUTTON: apply");
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
                        LOG.debug1("actionPerformed: BUTTON: revert");
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
                           new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
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
                LOG.debug1("actionPerformed: BUTTON: overview");
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
        buttonPanel.add(getActionsButton(), BorderLayout.LINE_END);
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
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.PAGE_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(
                              ClusterBrowser.SERVICE_LABEL_WIDTH
                              + ClusterBrowser.SERVICE_FIELD_WIDTH * 2 + 4));
        newPanel.add(new JScrollPane(mainPanel));
        Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
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
            mTransitionWriteLock.lock();
            try {
                final boolean wasEmpty = starting.isEmpty();
                starting.add(host.getName());
                if (!wasEmpty) {
                    mTransitionWriteLock.unlock();
                    return;
                }
            } finally {
                mTransitionWriteLock.unlock();
            }
            int i = 0;
            while (true) {
                getBrowser().periodicalVMSUpdate(host);
                updateParameters();
                getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
                Tools.sleep(1000);
                i++;
                mTransitionReadLock.lock();
                try {
                    if (starting.isEmpty() || i >= ACTION_TIMEOUT) {
                        mTransitionReadLock.unlock();
                        break;
                    }
                } finally {
                    mTransitionReadLock.unlock();
                }
            }
            if (i >= ACTION_TIMEOUT) {
                LOG.appWarning("start: could not start on " + host.getName());
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
        mTransitionWriteLock.lock();
        try {
            final boolean wasEmpty = starting.isEmpty();
            shuttingdown.add(host.getName());
            if (!wasEmpty) {
                mTransitionWriteLock.unlock();
                return;
            }
        } finally {
            mTransitionWriteLock.unlock();
        }
        int i = 0;
        while (true) {
            getBrowser().periodicalVMSUpdate(host);
            updateParameters();
            getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
            Tools.sleep(1000);
            i++;
            mTransitionReadLock.lock();
            try {
                if (shuttingdown.isEmpty() || i >= ACTION_TIMEOUT) {
                    mTransitionReadLock.unlock();
                    break;
                }
            } finally {
                mTransitionReadLock.unlock();
            }
        }
        if (i >= ACTION_TIMEOUT) {
            LOG.appWarning("startShuttingdownIndicator: could not shut down on "
                           + host.getName());
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
            mTransitionWriteLock.lock();
            try {
                final boolean wasEmpty = suspending.isEmpty();
                suspending.add(host.getName());
                if (!wasEmpty) {
                    mTransitionWriteLock.unlock();
                    return;
                }
            } finally {
                mTransitionWriteLock.unlock();
            }
            int i = 0;
            while (!suspending.isEmpty() && i < ACTION_TIMEOUT) {
                getBrowser().periodicalVMSUpdate(host);
                updateParameters();
                getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
                Tools.sleep(1000);
                i++;
                mTransitionReadLock.lock();
                try {
                    if (suspending.isEmpty() || i >= ACTION_TIMEOUT) {
                        mTransitionReadLock.unlock();
                        break;
                    }
                } finally {
                    mTransitionReadLock.unlock();
                }
            }
            if (i >= ACTION_TIMEOUT) {
                LOG.appWarning("suspend: could not suspend on "
                               + host.getName());
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
            mTransitionWriteLock.lock();
            try {
                final boolean wasEmpty = resuming.isEmpty();
                resuming.add(host.getName());
                if (!wasEmpty) {
                    mTransitionWriteLock.unlock();
                    return;
                }
            } finally {
                mTransitionWriteLock.unlock();
            }
            int i = 0;
            while (!resuming.isEmpty() && i < ACTION_TIMEOUT) {
                getBrowser().periodicalVMSUpdate(host);
                updateParameters();
                getBrowser().getVMSInfo().updateTable(VMSInfo.MAIN_TABLE);
                Tools.sleep(1000);
                i++;
                mTransitionReadLock.lock();
                try {
                    if (resuming.isEmpty() || i >= ACTION_TIMEOUT) {
                        mTransitionReadLock.unlock();
                        break;
                    }
                } finally {
                    mTransitionReadLock.unlock();
                }
            }
            if (i >= ACTION_TIMEOUT) {
                LOG.appWarning("resume: could not resume on "
                               + host.getName());
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
            @Override
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
            @Override
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
            @Override
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
            @Override
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
            @Override
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
            @Override
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
            @Override
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
            @Override
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
            @Override
            public void run() {
                thisNode.add(resource);
            }
        });
        getBrowser().reload(thisNode, true);
        vmsvi.selectMyself();
    }

    /** Add new hardware. */
    private UpdatableItem getAddNewHardwareMenu(final String name) {
        return new MyMenu(name,
                          new AccessMode(Application.AccessType.ADMIN, false),
                          new AccessMode(Application.AccessType.OP, false)) {
            private static final long serialVersionUID = 1L;

            @Override
            public String enablePredicate() {
                return null;
            }

            @Override
            public void updateAndWait() {
                removeAll();
                final Point2D pos = getPos();
                /* disk */
                final MyMenuItem newDiskMenuItem = new MyMenuItem(
                   Tools.getString("VMSVirtualDomainInfo.AddNewDisk"),
                   BlockDevInfo.HARDDISK_ICON_LARGE,
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
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
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
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
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
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
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
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
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
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
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
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
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
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
                   new AccessMode(Application.AccessType.ADMIN, false),
                   new AccessMode(Application.AccessType.OP, false)) {
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
                   new AccessMode(Application.AccessType.ADMIN, true),
                   new AccessMode(Application.AccessType.OP, false)) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void action() {
                        hidePopup();
                        addVideosPanel();
                    }
                };
                newVideosMenuItem.setPos(pos);
                add(newVideosMenuItem);
                super.updateAndWait();
            }
        };
    }

    /** Adds vm domain start menu item. */
    void addStartMenu(final Collection<UpdatableItem> items, final Host host) {
        final UpdatableItem startMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.StartOn")
                            + host.getName(),
                            HostBrowser.HOST_ON_ICON_LARGE,
                            Tools.getString("VMSVirtualDomainInfo.StartOn")
                            + host.getName(),
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {

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
                if (!Tools.getApplication().isAdvancedMode() && isUsedByCRM()) {
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
    void addShutdownMenu(final Collection<UpdatableItem> items, final Host host) {
        final UpdatableItem shutdownMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.ShutdownOn")
                            + host.getName(),
                            SHUTDOWN_ICON,
                            Tools.getString("VMSVirtualDomainInfo.ShutdownOn")
                            + host.getName(),
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {

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
                if (!Tools.getApplication().isAdvancedMode() && isUsedByCRM()) {
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
    void addRebootMenu(final Collection<UpdatableItem> items, final Host host) {
        final UpdatableItem rebootMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.RebootOn")
                            + host.getName(),
                            REBOOT_ICON,
                            Tools.getString("VMSVirtualDomainInfo.RebootOn")
                            + host.getName(),
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {

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
                if (!Tools.getApplication().isAdvancedMode() && isUsedByCRM()) {
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
    void addResumeMenu(final Collection<UpdatableItem> items, final Host host) {
        final UpdatableItem resumeMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.ResumeOn")
                            + host.getName(),
                            RESUME_ICON,
                            Tools.getString("VMSVirtualDomainInfo.ResumeOn")
                            + host.getName(),
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {

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
    void addDestroyMenu(final Collection<UpdatableItem> items,
                        final Host host) {
        final UpdatableItem destroyMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.DestroyOn")
                            + host.getName(),
                            DESTROY_ICON,
                            Tools.getString("VMSVirtualDomainInfo.DestroyOn")
                            + host.getName(),
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;


            @Override
            public String enablePredicate() {
                if (!Tools.getApplication().isAdvancedMode() && isUsedByCRM()) {
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
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {

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
                            new AccessMode(Application.AccessType.OP, false),
                            new AccessMode(Application.AccessType.OP, false)) {

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
                        new AccessMode(Application.AccessType.OP, false),
                        new AccessMode(Application.AccessType.OP, false)) {
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
        final UpdatableItem removeMenuItem = new MyMenuItem(
                       Tools.getString("VMSVirtualDomainInfo.RemoveDomain"),
                       ClusterBrowser.REMOVE_ICON,
                       Tools.getString("VMSVirtualDomainInfo.RemoveDomain"),
                       Tools.getString("VMSVirtualDomainInfo.CancelDomain"),
                       ClusterBrowser.REMOVE_ICON,
                       Tools.getString("VMSVirtualDomainInfo.CancelDomain"),
                       new AccessMode(Application.AccessType.ADMIN, false),
                       new AccessMode(Application.AccessType.OP, false)) {
                            private static final long serialVersionUID = 1L;
                            @Override
                            public boolean predicate() {
                                return !getResource().isNew();
                            }
                            @Override
                            public String enablePredicate() {
                                if (!Tools.getApplication().isAdvancedMode()
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
    void addVncViewersToTheMenu(final Collection<UpdatableItem> items,
                                final Host host) {
        final boolean testOnly = false;
        final VMSVirtualDomainInfo thisClass = this;
        if (Tools.getApplication().isTightvnc()) {
            /* tight vnc test menu */
            final UpdatableItem tightvncViewerMenu = new MyMenuItem(
                            getVNCMenuString("TIGHT", host),
                            VNC_ICON,
                            getVNCMenuString("TIGHT", host),
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false)) {

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

        if (Tools.getApplication().isUltravnc()) {
            /* ultra vnc test menu */
            final UpdatableItem ultravncViewerMenu = new MyMenuItem(
                            getVNCMenuString("ULTRA", host),
                            VNC_ICON,
                            getVNCMenuString("ULTRA", host),
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false)) {

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

        if (Tools.getApplication().isRealvnc()) {
            /* real vnc test menu */
            final UpdatableItem realvncViewerMenu = new MyMenuItem(
                            getVNCMenuString("REAL", host),
                            VNC_ICON,
                            getVNCMenuString("REAL", host),
                            new AccessMode(Application.AccessType.RO, false),
                            new AccessMode(Application.AccessType.RO, false)) {

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
    protected Value getParamPreferred(final String param) {
        if (preferredEmulator != null
            && VMSXML.VM_PARAM_EMULATOR.equals(param)) {
            return new StringValue(preferredEmulator);
        }
        return PREFERRED_MAP.get(param);
    }

    /** Returns default value for specified parameter. */
    @Override
    protected Value getParamDefault(final String param) {
        return DEFAULTS_MAP.get(param);
    }

    /** Returns true if the value of the parameter is ok. */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        if (isRequired(param)
            && (newValue == null || newValue.isNothingSelected())) {
            return false;
        }
        if (VMSXML.VM_PARAM_MEMORY.equals(param)) {
            final long mem = VMSXML.convertToKilobytes(newValue);
            if (mem < 4096) {
                return false;
            }
            final long curMem = VMSXML.convertToKilobytes(
                            getComboBoxValue(VMSXML.VM_PARAM_CURRENTMEMORY));
            if (mem < curMem) {
                return false;
            }
        } else if (VMSXML.VM_PARAM_CURRENTMEMORY.equals(param)) {
            final long curMem = VMSXML.convertToKilobytes(newValue);
            if (curMem < 4096) {
                return false;
            }
            final long mem = VMSXML.convertToKilobytes(
                                 getComboBoxValue(VMSXML.VM_PARAM_MEMORY));
            if (mem < curMem) {
                getWidget(VMSXML.VM_PARAM_MEMORY, null).setValue(newValue);
            }
        } else if (VMSXML.VM_PARAM_DOMAIN_TYPE.equals(param)) {
            final Widget wi = getWidget(param, null);
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
                final Widget emWi = getWidget(VMSXML.VM_PARAM_EMULATOR,
                                              Widget.WIZARD_PREFIX);
                final Widget loWi = getWidget(VMSXML.VM_PARAM_LOADER,
                                              Widget.WIZARD_PREFIX);
                final Widget voWi = getWidget(VMSXML.VM_PARAM_VIRSH_OPTIONS,
                                              Widget.WIZARD_PREFIX);
                final Widget typeWi = getWidget(VMSXML.VM_PARAM_TYPE,
                                                Widget.WIZARD_PREFIX);
                final Widget inWi = getWidget(VMSXML.VM_PARAM_INIT,
                                              Widget.WIZARD_PREFIX);
                if (Tools.areEqual(DOMAIN_TYPE_XEN,
                                   newValue.getValueForConfig())) {
                    if (emWi != null) {
                        emWi.setValue(new StringValue(xenLibPath + "/bin/qemu-dm"));
                    }
                    if (loWi != null) {
                        loWi.setValue(new StringValue(xenLibPath + "/boot/hvmloader"));
                    }
                    if (voWi != null) {
                        voWi.setValue(VIRSH_OPTION_XEN);
                    }
                    if (typeWi != null) {
                        typeWi.setValue(TYPE_HVM);
                    }
                    if (inWi != null) {
                        inWi.setValue(NO_SELECTION_VALUE);
                    }
                } else if (Tools.areEqual(DOMAIN_TYPE_LXC,
                                          newValue.getValueForConfig())) {
                    if (emWi != null) {
                        emWi.setValue(new StringValue(lxcLibPath + "/libvirt_lxc"));
                    }
                    if (loWi != null) {
                        loWi.setValue(NO_SELECTION_VALUE);
                    }
                    if (voWi != null) {
                        voWi.setValue(VIRSH_OPTION_LXC);
                    }
                    if (typeWi != null) {
                        typeWi.setValue(TYPE_EXE);
                    }
                    if (inWi != null) {
                        inWi.setValue(new StringValue("/bin/sh"));
                    }
                } else if (Tools.areEqual(DOMAIN_TYPE_VBOX,
                                          newValue.getValueForConfig())) {
                    if (emWi != null) {
                        emWi.setValue(new StringValue(xenLibPath + ""));
                    }
                    if (loWi != null) {
                        loWi.setValue(new StringValue(xenLibPath + ""));
                    }
                    if (voWi != null) {
                        voWi.setValue(VIRSH_OPTION_VBOX);
                    }
                    if (typeWi != null) {
                        typeWi.setValue(TYPE_HVM);
                    }
                    if (inWi != null) {
                        inWi.setValue(NO_SELECTION_VALUE);
                    }
                } else if (Tools.areEqual(DOMAIN_TYPE_OPENVZ,
                                          newValue.getValueForConfig())) {
                    if (emWi != null) {
                        emWi.setValue(NO_SELECTION_VALUE);
                    }
                    if (loWi != null) {
                        loWi.setValue(NO_SELECTION_VALUE);
                    }
                    if (voWi != null) {
                        voWi.setValue(VIRSH_OPTION_OPENVZ);
                    }
                    if (typeWi != null) {
                        typeWi.setValue(TYPE_EXE);
                    }
                    if (inWi != null) {
                        inWi.setValue(new StringValue("/sbin/init"));
                    }
                } else if (Tools.areEqual(DOMAIN_TYPE_UML,
                                          newValue.getValueForConfig())) {
                    if (emWi != null) {
                        emWi.setValue(NO_SELECTION_VALUE);
                    }
                    if (loWi != null) {
                        loWi.setValue(NO_SELECTION_VALUE);
                    }
                    if (voWi != null) {
                        voWi.setValue(VIRSH_OPTION_UML);
                    }
                    if (typeWi != null) {
                        typeWi.setValue(TYPE_UML);
                    }
                    if (inWi != null) {
                        inWi.setValue(NO_SELECTION_VALUE);
                    }
                } else {
                    if (emWi != null) {
                        emWi.setValue(new StringValue("/usr/bin/kvm"));
                    }
                    if (loWi != null) {
                        loWi.setValue(NO_SELECTION_VALUE);
                    }
                    if (voWi != null) {
                        voWi.setValue(VIRSH_OPTION_KVM);
                    }
                    if (typeWi != null) {
                        typeWi.setValue(TYPE_HVM);
                    }
                    if (inWi != null) {
                        inWi.setValue(NO_SELECTION_VALUE);
                    }
                }
            }
            prevType = wi.getValue();
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
    protected Value[] getParamPossibleChoices(final String param) {
        if (VMSXML.VM_PARAM_AUTOSTART.equals(param)) {
            return autostartPossibleValues;
        } else if (VMSXML.VM_PARAM_VIRSH_OPTIONS.equals(param)) {
            return VIRSH_OPTIONS;
        } else if (VMSXML.VM_PARAM_CPUMATCH_MODEL.equals(param)) {
            final Set<Value> models = new LinkedHashSet<Value>();
            models.add(new StringValue());
            for (final Host host : getBrowser().getClusterHosts()) {
                models.addAll(host.getCPUMapModels());
            }
            return models.toArray(new Value[models.size()]);
        } else if (VMSXML.VM_PARAM_CPUMATCH_VENDOR.equals(param)) {
            final Set<Value> vendors = new LinkedHashSet<Value>();
            vendors.add(new StringValue());
            for (final Host host : getBrowser().getClusterHosts()) {
                vendors.addAll(host.getCPUMapVendors());
            }
            return vendors.toArray(new Value[vendors.size()]);
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
        return VMSXML.VM_PARAM_NAME.equals(param);
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
    protected Widget.Type getFieldType(final String param) {
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
                getInfoPanel();
            }
        });
        waitForInfoPanel();
        final String[] params = getParametersFromXML();
        final Map<String, String> parameters = new HashMap<String, String>();
        setName(getComboBoxValue(VMSXML.VM_PARAM_NAME).getValueForConfig());
        for (final String param : getParametersFromXML()) {
            final Value value = getComboBoxValue(param);
            if (value == null) {
                parameters.put(param, "");
            } else if (value.getUnit() != null) {
                parameters.put(param,
                               Long.toString(VMSXML.convertToKilobytes(value)));
            } else {
                final String valueForConfig = value.getValueForConfig();
                if (valueForConfig == null) {
                    parameters.put(param, "");
                } else {
                    parameters.put(param, valueForConfig);
                }
            }
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
            final Widget hostWi = definedOnHostComboBoxHash.get(host.getName());
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final Widget wizardHostWi =
                                          definedOnHostComboBoxHash.get(
                                           WIZARD_HOST_PREFIX + host.getName());
                    if (wizardHostWi != null) {
                        wizardHostWi.setEnabled(false);
                    }
                }
            });
            final Value value =
                definedOnHostComboBoxHash.get(host.getName()).getValue();
            final boolean needConsole = needConsole();
            if (DEFINED_ON_HOST_TRUE.equals(value)) {
                final Node domainNode;
                VMSXML vmsxml;
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
                               if (hi.checkResourceFields(
                                            null,
                                            hi.getRealParametersFromXML(),
                                            true).isChanged()) {
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
            Tools.invokeLater(new Runnable() {
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
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final Host host : getBrowser().getClusterHosts()) {
                    final Widget hostWi = definedOnHostComboBoxHash.get(
                                                               host.getName());
                    final Widget wizardHostWi = definedOnHostComboBoxHash.get(
                                           WIZARD_HOST_PREFIX + host.getName());
                    if (wizardHostWi != null) {
                        wizardHostWi.setEnabled(true);
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
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> e = thisNode.children();
        while (e.hasMoreElements()) {
            final DefaultMutableTreeNode node = e.nextElement();
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
    protected final Unit[] getUnits(final String param) {
        return VMSXML.getUnits();
    }

    /** Returns the default unit for the parameter. */
    protected String getDefaultUnit(final String param) {
        return null;
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
            final VMSDiskInfo vdi;
            try {
                vdi = diskToInfo.get(targetDev);
            } finally {
                mDiskToInfoLock.unlock();
            }
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
                final Value value =
                        definedOnHostComboBoxHash.get(h.getName()).getValue();
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
        try {
            diskKeyToInfo = dkti;
        } finally {
            mDiskToInfoLock.unlock();
        }
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
            VMSFilesystemInfo vdi;
            try {
                vdi = filesystemToInfo.get(targetDev);
            } finally {
                mFilesystemToInfoLock.unlock();
            }
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
        try {
            filesystemKeyToInfo = dkti;
        } finally {
            mFilesystemToInfoLock.unlock();
        }
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
        final String s;
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
        try {
            inputDevKeyToInfo = iToInfo;
        } finally {
            mInputDevToInfoLock.unlock();
        }
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
        try {
            graphicsKeyToInfo = iToInfo;
        } finally {
            mGraphicsToInfoLock.unlock();
        }
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
        try {
            soundKeyToInfo = iToInfo;
        } finally {
            mSoundToInfoLock.unlock();
        }
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
        try {
            serialKeyToInfo = iToInfo;
        } finally {
            mSerialToInfoLock.unlock();
        }
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
        try {
            parallelKeyToInfo = iToInfo;
        } finally {
            mParallelToInfoLock.unlock();
        }
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
        try {
            videoKeyToInfo = iToInfo;
        } finally {
            mVideoToInfoLock.unlock();
        }
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
        try {
            interfaceKeyToInfo = iToInfo;
        } finally {
            mInterfaceToInfoLock.unlock();
        }
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
            final VMSDiskInfo vdi;
            try {
                vdi = diskKeyToInfo.get(key);
            } finally {
                mDiskToInfoLock.unlock();
            }
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
            final VMSFilesystemInfo vfi;
            try {
                vfi = filesystemKeyToInfo.get(key);
            } finally {
                mFilesystemToInfoLock.unlock();
            }
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
            final VMSInterfaceInfo vii;
            try {
                vii = interfaceKeyToInfo.get(key);
            } finally {
                mInterfaceToInfoLock.unlock();
            }
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
            final VMSInputDevInfo vidi;
            try {
                vidi = inputDevKeyToInfo.get(key);
            } finally {
                mInputDevToInfoLock.unlock();
            }
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
            final VMSGraphicsInfo vgi;
            try {
                vgi = graphicsKeyToInfo.get(key);
            } finally {
                mGraphicsToInfoLock.unlock();
            }
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
            final VMSSoundInfo vsi;
            try {
                vsi = soundKeyToInfo.get(key);
            } finally {
                mSoundToInfoLock.unlock();
            }
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
            final VMSSerialInfo vsi;
            try {
                vsi = serialKeyToInfo.get(key);
            } finally {
                mSerialToInfoLock.unlock();
            }
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
            final VMSParallelInfo vpi;
            try {
                vpi = parallelKeyToInfo.get(key);
            } finally {
                mParallelToInfoLock.unlock();
            }
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
            final VMSVideoInfo vvi;
            try {
                vvi = videoKeyToInfo.get(key);
            } finally {
                mVideoToInfoLock.unlock();
            }
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
            try {
                return diskToInfo.get(key);
            } finally {
                mDiskToInfoLock.unlock();
            }
        } else if (FILESYSTEM_TABLE.equals(tableName)) {
            mFilesystemToInfoLock.lock();
            try {
                return filesystemToInfo.get(key);
            } finally {
                mFilesystemToInfoLock.unlock();
            }
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
        try {
            return diskToInfo.containsKey(dev);
        } finally {
            mDiskToInfoLock.unlock();
        }
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
            } catch (final Exceptions.IllegalVersionException e) {
                LOG.appWarning("isEnabled: " + e.getMessage(), e);
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
         return VMSXML.VM_PARAM_MEMORY.equals(param);
    }


    /** Returns access type of this parameter. */
    @Override
    protected Application.AccessType getAccessType(final String param) {
        return Application.AccessType.ADMIN;
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
    public Check checkResourceFields(final String param,
                                     final String[] params) {
        final DefaultMutableTreeNode thisNode = getNode();
        final List<String> changed = new ArrayList<String>();
        final List<String> incorrect = new ArrayList<String>();
        if (thisNode == null) {
            incorrect.add("missing node");
            return new Check(incorrect, changed);
        }

        boolean cor = false;
        for (final Host host : getBrowser().getClusterHosts()) {
            if (!definedOnHostComboBoxHash.containsKey(host.getName())) {
                continue;
            }
            final Widget hostWi = definedOnHostComboBoxHash.get(host.getName());
            final Widget wizardHostWi = definedOnHostComboBoxHash.get(
                                           WIZARD_HOST_PREFIX + host.getName());
            final Value value = hostWi.getValue();
            final VMSXML vmsxml = getBrowser().getVMSXML(host);
            final Value savedValue;
            if (vmsxml != null
                && vmsxml.getDomainNames().contains(getDomainName())) {
                savedValue = DEFINED_ON_HOST_TRUE;
            } else {
                savedValue = DEFINED_ON_HOST_FALSE;
            }
            hostWi.setBackground(value, savedValue, false);
            if (wizardHostWi != null) {
                wizardHostWi.setBackground(value, savedValue, false);
            }
            if (DEFINED_ON_HOST_TRUE.equals(value)) {
                cor = true; /* at least one */
            }

            if ((vmsxml == null
                 || (!getResource().isNew()
                     && !vmsxml.getDomainNames().contains(getDomainName())))
                && DEFINED_ON_HOST_TRUE.equals(value)) {
                changed.add("host");
            } else if (vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && DEFINED_ON_HOST_FALSE.equals(value)) {
                changed.add("host");
            }
        }
        if (!cor) {
            for (final String key : definedOnHostComboBoxHash.keySet()) {
                definedOnHostComboBoxHash.get(key).wrongValue();
            }
            incorrect.add("no host");
        }
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> eee = thisNode.children();
        final Check check = new Check(incorrect, changed);
        check.addCheck(super.checkResourceFields(param, params));
        while (eee.hasMoreElements()) {
            final DefaultMutableTreeNode node = eee.nextElement();
            final VMSHardwareInfo vmshi =
                            (VMSHardwareInfo) node.getUserObject();
            check.addCheck(vmshi.checkResourceFields(
                                              null,
                                              vmshi.getRealParametersFromXML(),
                                              true)); 
        }
        return check;
    }

    /** Returns combo box for parameter. */
    @Override
    protected Widget createWidget(final String param,
                                  final String prefix,
                                  final int width) {
        final Widget paramWi = super.createWidget(param, prefix, width);
        if (VMSXML.VM_PARAM_BOOT.equals(param)
            || VMSXML.VM_PARAM_BOOT_2.equals(param)) {
            paramWi.setAlwaysEditable(false);
        }
        return paramWi;
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
                return "Remove domain " + key + '.';
            }
        } else if (DISK_TABLE.equals(tableName)) {
            if (DISK_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + '.';
            }
        } else if (FILESYSTEM_TABLE.equals(tableName)) {
            if (FILESYSTEM_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + '.';
            }
        } else if (INTERFACES_TABLE.equals(tableName)) {
            if (INTERFACES_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + '.';
            }
        } else if (INPUTDEVS_TABLE.equals(tableName)) {
            if (INPUTDEVS_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + '.';
            }
        } else if (GRAPHICS_TABLE.equals(tableName)) {
            if (GRAPHICS_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + '.';
            }
        } else if (SOUND_TABLE.equals(tableName)) {
            if (SOUND_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + '.';
            }
        } else if (SERIAL_TABLE.equals(tableName)) {
            if (SERIAL_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + '.';
            }
        } else if (PARALLEL_TABLE.equals(tableName)) {
            if (PARALLEL_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + '.';
            }
        } else if (VIDEO_TABLE.equals(tableName)) {
            if (VIDEO_DEFAULT_WIDTHS.containsKey(column)) {
                return "Remove " + key + '.';
            }
        }
        return super.getTableToolTip(tableName, key, object, raw, column);
    }

    /** Sets button next to host to the start button. */
    private void setButtonToStart(final Host host,
                                  final Widget hostWi,
                                  final MyButton hostBtn,
                                  final boolean stopped) {
        if (hostWi != null) {
            final boolean enable = host.isConnected();
            Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                @Override
                public void run() {
                    hostWi.setTFButtonEnabled(enable && stopped);
                    hostBtn.setText("Start");
                    hostBtn.setIcon(HostBrowser.HOST_ON_ICON);
                    hostBtn.setToolTipText("Start on " + host.getName());
                }
            });
        }
    }

    /** Sets button next to host to the view button. */
    private void setButtonToView(final Host host,
                                 final Widget hostWi,
                                 final MyButton hostBtn) {
        if (hostWi != null) {
            final boolean enable = host.isConnected();
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    hostWi.setTFButtonEnabled(enable);
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
                                       final Widget hostWi,
                                       final MyButton hostBtn) {
        if (hostWi != null) {
            Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                @Override
                public void run() {
                    hostWi.setTFButtonEnabled(false);
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
                             hostButtons.get(WIZARD_HOST_PREFIX + h.getName());
            final Widget hostWi = definedOnHostComboBoxHash.get(h.getName());
            final Widget wizardHostWi =
                definedOnHostComboBoxHash.get(WIZARD_HOST_PREFIX + h.getName());
            if (vmsxml != null
                && vmsxml.getDomainNames().contains(getDomainName())) {
                if (vmsxml.isRunning(getDomainName())) {
                    setButtonToView(h, hostWi, hostBtn);
                    setButtonToView(h, wizardHostWi, wizardHostBtn);
                } else {
                    setButtonToStart(h, hostWi, hostBtn, !running);
                    setButtonToStart(h, wizardHostWi, wizardHostBtn, !running);
                }
            } else {
                setButtonToNotDefined(h, hostWi, hostBtn);
                setButtonToNotDefined(h, wizardHostWi, wizardHostBtn);
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
            final Widget hostWi = definedOnHostComboBoxHash.get(h.getName());
            final Value savedValue;
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (getResource().isNew()
                || (vmsxml != null
                    && vmsxml.getDomainNames().contains(getDomainName()))) {
                savedValue = DEFINED_ON_HOST_TRUE;
            } else {
                savedValue = DEFINED_ON_HOST_FALSE;
            }
            hostWi.setValue(savedValue);
        }
        @SuppressWarnings("unchecked")
        final Enumeration<DefaultMutableTreeNode> eee = thisNode.children();
        while (eee.hasMoreElements()) {
            final DefaultMutableTreeNode node = eee.nextElement();
            final VMSHardwareInfo vmshi =
                            (VMSHardwareInfo) node.getUserObject();
            if (vmshi.checkResourceFields(null,
                                          vmshi.getRealParametersFromXML(),
                                          true).isChanged()) {
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
                getResource().setValue(pv, new StringValue(preferredEmulator));
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

    /** Return virsh options like -c xen:///. */
    public String getVirshOptions() {
        final Value v = getResource().getValue(VMSXML.VM_PARAM_VIRSH_OPTIONS);
        if (v == null) {
            return "";
        }
        return v.getValueForConfig();
    }

    /** Return whether domain type needs "display" section. */
    public boolean needDisplay() {
        return NEED_DISPLAY.contains(getWidget(
                        VMSXML.VM_PARAM_DOMAIN_TYPE, null).getStringValue());
    }

    /** Return whether domain type needs "console" section. */
    public boolean needConsole() {
        return NEED_CONSOLE.contains(getWidget(
                        VMSXML.VM_PARAM_DOMAIN_TYPE, null).getStringValue());
    }

    /** Return whether domain type needs filesystem instead of disk device. */
    public boolean needFilesystem() {
        return NEED_FILESYSTEM.contains(getWidget(
                        VMSXML.VM_PARAM_DOMAIN_TYPE, null).getStringValue());
    }
}
