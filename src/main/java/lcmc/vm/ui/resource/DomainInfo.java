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
package lcmc.vm.ui.resource;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;

import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;

import org.w3c.dom.Node;

import lcmc.Exceptions;
import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.resource.NetInfo;
import lcmc.cluster.ui.widget.Check;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.common.domain.ResourceValue;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.Unit;
import lcmc.common.domain.Value;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Access;
import lcmc.common.ui.Browser;
import lcmc.common.ui.EditableInfo;
import lcmc.common.ui.Info;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.ProgressIndicator;
import lcmc.common.ui.treemenu.ClusterTreeMenu;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.common.ui.utils.WidgetListener;
import lcmc.crm.ui.resource.ServiceInfo;
import lcmc.drbd.ui.resource.BlockDevInfo;
import lcmc.host.domain.Host;
import lcmc.host.ui.HostBrowser;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.vm.domain.VMParams;
import lcmc.vm.domain.VmsXml;
import lcmc.vm.domain.data.DiskData;
import lcmc.vm.domain.data.FilesystemData;
import lcmc.vm.domain.data.GraphicsData;
import lcmc.vm.domain.data.InputDevData;
import lcmc.vm.domain.data.InterfaceData;
import lcmc.vm.domain.data.ParallelData;
import lcmc.vm.domain.data.SerialData;
import lcmc.vm.domain.data.SoundData;
import lcmc.vm.domain.data.VideoData;
import lcmc.vm.service.VIRSH;

/**
 * This class holds info about VirtualDomain service in the VMs category,
 * but not in the cluster view.
 */
@Named
public class DomainInfo extends EditableInfo {
    private static final Logger LOG = LoggerFactory.getLogger(DomainInfo.class);
    /**
     * Timeout of starting, shutting down, etc. actions in seconds.
     */
    private static final int ACTION_TIMEOUT = 20;

    private static final Value VIRSH_OPTION_QEMU = new StringValue();
    private static final Value VIRSH_OPTION_XEN = new StringValue("-c 'xen:///'");
    private static final Value VIRSH_OPTION_LXC = new StringValue("-c 'lxc:///'");
    private static final Value VIRSH_OPTION_VBOX = new StringValue("-c 'vbox:///session'");
    private static final Value VIRSH_OPTION_OPENVZ = new StringValue("-c 'openvz:///system'");
    private static final Value VIRSH_OPTION_UML = new StringValue("-c 'uml:///system'");

    static final String DOMAIN_TYPE_QEMU = "qemu";
    private static final String DOMAIN_TYPE_XEN = "xen";
    private static final String DOMAIN_TYPE_LXC = "lxc";
    private static final String DOMAIN_TYPE_VBOX = "vbox";
    private static final String DOMAIN_TYPE_OPENVZ = "openvz";
    private static final String DOMAIN_TYPE_UML = "uml";
    private static final String DOMAIN_TYPE_KVM = "kvm";


    private static final Value[] VIRSH_OPTIONS =
            new Value[]{VIRSH_OPTION_QEMU, VIRSH_OPTION_XEN, VIRSH_OPTION_LXC, VIRSH_OPTION_VBOX, VIRSH_OPTION_OPENVZ,
                    VIRSH_OPTION_UML};

    /**
     * Whether it needs "display" section.
     */
    private static final Set<String> NEED_DISPLAY = Set.of(DOMAIN_TYPE_QEMU, DOMAIN_TYPE_XEN, DOMAIN_TYPE_VBOX, DOMAIN_TYPE_KVM);
    /**
     * Whether it needs "console" section.
     */
    private static final Set<String> NEED_CONSOLE = Set.of(DOMAIN_TYPE_LXC, DOMAIN_TYPE_OPENVZ, DOMAIN_TYPE_UML);
    /**
     * Whether it needs "filesystem" section.
     */
    private static final Set<String> NEED_FILESYSTEM = Set.of(DOMAIN_TYPE_LXC, DOMAIN_TYPE_OPENVZ, DOMAIN_TYPE_VBOX);
    private static final String[] VM_PARAMETERS =
            new String[]{VMParams.VM_PARAM_DOMAIN_TYPE, VMParams.VM_PARAM_NAME, VMParams.VM_PARAM_VIRSH_OPTIONS,
                    VMParams.VM_PARAM_EMULATOR, VMParams.VM_PARAM_VCPU, VMParams.VM_PARAM_CURRENTMEMORY, VMParams.VM_PARAM_MEMORY,
                    VMParams.VM_PARAM_BOOTLOADER, VMParams.VM_PARAM_BOOT, VMParams.VM_PARAM_BOOT_2, VMParams.VM_PARAM_LOADER,
                    VMParams.VM_PARAM_AUTOSTART, VMParams.VM_PARAM_TYPE, VMParams.VM_PARAM_INIT, VMParams.VM_PARAM_TYPE_ARCH,
                    VMParams.VM_PARAM_TYPE_MACHINE, VMParams.VM_PARAM_ACPI, VMParams.VM_PARAM_APIC, VMParams.VM_PARAM_PAE,
                    VMParams.VM_PARAM_HAP, VMParams.VM_PARAM_CLOCK_OFFSET, VMParams.VM_PARAM_CPU_MATCH,
                    VMParams.VM_PARAM_CPUMATCH_MODEL, VMParams.VM_PARAM_CPUMATCH_VENDOR,
                    VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS, VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_CORES,
                    VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS, VMParams.VM_PARAM_CPUMATCH_FEATURE_POLICY,
                    VMParams.VM_PARAM_CPUMATCH_FEATURES, VMParams.VM_PARAM_ON_POWEROFF, VMParams.VM_PARAM_ON_REBOOT,
                    VMParams.VM_PARAM_ON_CRASH};

    private static final Collection<String> IS_ADVANCED = new HashSet<>(
            Arrays.asList(VMParams.VM_PARAM_ACPI, VMParams.VM_PARAM_APIC, VMParams.VM_PARAM_PAE, VMParams.VM_PARAM_HAP,
                    VMParams.VM_PARAM_CPU_MATCH, VMParams.VM_PARAM_CPUMATCH_MODEL, VMParams.VM_PARAM_CPUMATCH_VENDOR,
                    VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS, VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_CORES,
                    VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS, VMParams.VM_PARAM_CPUMATCH_FEATURE_POLICY,
                    VMParams.VM_PARAM_CPUMATCH_FEATURES, VMParams.VM_PARAM_ON_POWEROFF, VMParams.VM_PARAM_ON_REBOOT,
                    VMParams.VM_PARAM_ON_CRASH));
    /**
     * Map of sections to which every param belongs.
     */
    private static final Map<String, String> SECTION_MAP = new HashMap<>();
    /**
     * Map of short param names with uppercased first character.
     */
    private static final Map<String, String> SHORTNAME_MAP = new HashMap<>();
    /**
     * Map of default values.
     */
    private static final Map<String, Value> DEFAULTS_MAP = new HashMap<>();
    /**
     * Preferred values.
     */
    private static final Map<String, Value> PREFERRED_MAP = new HashMap<>();
    /**
     * Types of some of the field.
     */
    private static final Map<String, Widget.Type> FIELD_TYPES = new HashMap<>();
    /**
     * Possible values for some fields.
     */
    private static final Map<String, Value[]> POSSIBLE_VALUES = new HashMap<>();
    /**
     * Whether parameter is an integer.
     */
    private static final Collection<String> IS_INTEGER = new ArrayList<>();
    /**
     * Required version for a parameter.
     */
    private static final Map<String, String> REQUIRED_VERSION = new HashMap<>();
    /**
     * Returns whether this parameter has a unit prefix.
     */
    private static final Map<String, Boolean> HAS_UNIT_PREFIX = new HashMap<>();
    /**
     * Back to overview icon.
     */
    private static final ImageIcon BACK_ICON = Tools.createImageIcon(Tools.getDefault("BackIcon"));

    static final ImageIcon VNC_ICON = Tools.createImageIcon(Tools.getDefault("VMS.VNC.IconLarge"));
    static final ImageIcon VNC_ICON_SMALL = Tools.createImageIcon(Tools.getDefault("VMS.VNC.IconSmall"));
    static final ImageIcon PAUSE_ICON = Tools.createImageIcon(Tools.getDefault("VMS.Pause.IconLarge"));
    static final String HEADER_TABLE = "header";
    static final String DISK_TABLE = "disks";
    static final String FILESYSTEM_TABLE = "filesystems";
    static final String INTERFACES_TABLE = "interfaces";
    static final String INPUTDEVS_TABLE = "inputdevs";
    static final String GRAPHICS_TABLE = "graphics";
    static final String SOUND_TABLE = "sound";
    static final String SERIAL_TABLE = "serial";
    static final String PARALLEL_TABLE = "parallel";
    static final String VIDEO_TABLE = "video";
    
    private static final Value VM_TRUE = new StringValue("True");
    private static final Value VM_FALSE = new StringValue("False");

    private static final Value DEFINED_ON_HOST_TRUE = VM_TRUE;
    private static final Value DEFINED_ON_HOST_FALSE = VM_FALSE;

    private static final String WIZARD_HOST_PREFIX = Widget.WIZARD_PREFIX + ':';

    private static final String VIRTUAL_SYSTEM_STRING = Tools.getString("DomainInfo.Section.VirtualSystem");
    private static final String VIRTUAL_SYSTEM_FEATURES = Tools.getString("DomainInfo.Section.Features");
    private static final String VIRTUAL_SYSTEM_OPTIONS = Tools.getString("DomainInfo.Section.Options");
    private static final String CPU_MATCH_OPTIONS = Tools.getString("DomainInfo.Section.CPUMatch");
    /**
     * String that is displayed as a tool tip for not applied domain.
     */
    static final String NOT_APPLIED = "not applied yet";
    /**
     * String that is displayed as a tool tip for disabled menu item.
     */
    static final String NO_VM_STATUS_STRING = "VM status is not available";
    /* remove button column */
    private static final int CONTROL_BUTTON_WIDTH = 80;
    private static final Map<Integer, Integer> HEADER_DEFAULT_WIDTHS = Map.of(4, CONTROL_BUTTON_WIDTH);
    private static final Map<Integer, Integer> DISK_DEFAULT_WIDTHS = Map.of(2, CONTROL_BUTTON_WIDTH);
    private static final Map<Integer, Integer> FILESYSTEM_DEFAULT_WIDTHS = Map.of(2, CONTROL_BUTTON_WIDTH);
    private static final Map<Integer, Integer> INTERFACES_DEFAULT_WIDTHS = Map.of(2, CONTROL_BUTTON_WIDTH);
    private static final Map<Integer, Integer> INPUTDEVS_DEFAULT_WIDTHS = Map.of(1, CONTROL_BUTTON_WIDTH);
    private static final Map<Integer, Integer> GRAPHICS_DEFAULT_WIDTHS = Map.of(1, CONTROL_BUTTON_WIDTH);
    private static final Map<Integer, Integer> SOUND_DEFAULT_WIDTHS = Map.of(1, CONTROL_BUTTON_WIDTH);
    private static final Map<Integer, Integer> SERIAL_DEFAULT_WIDTHS = Map.of(1, CONTROL_BUTTON_WIDTH);
    private static final Map<Integer, Integer> PARALLEL_DEFAULT_WIDTHS = Map.of(1, CONTROL_BUTTON_WIDTH);
    private static final Map<Integer, Integer> VIDEO_DEFAULT_WIDTHS = Map.of(1, CONTROL_BUTTON_WIDTH);

    private static final Value TYPE_HVM = new StringValue("hvm");
    private static final Value TYPE_LINUX = new StringValue("linux");
    private static final Value TYPE_EXE = new StringValue("exe");
    private static final Value TYPE_UML = new StringValue("uml");

    private static final Value NO_SELECTION_VALUE = new StringValue();

    static final String IS_USED_BY_CRM_STRING = "it is used by cluster manager";

    public static final Value BOOT_HD = new StringValue("hd", "Hard Disk");
    public static final Value BOOT_NETWORK = new StringValue("network", "Network (PXE)");
    public static final Value BOOT_CDROM = new StringValue("cdrom", "CD-ROM");
    public static final Value BOOT_FD = new StringValue("fd", "Floppy");

    static {
        SECTION_MAP.put(VMParams.VM_PARAM_NAME, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_DOMAIN_TYPE, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_EMULATOR, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_VCPU, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_CURRENTMEMORY, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_MEMORY, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_BOOTLOADER, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_BOOT, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_BOOT_2, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_LOADER, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_AUTOSTART, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_VIRSH_OPTIONS, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_TYPE, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_TYPE_ARCH, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_TYPE_MACHINE, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMParams.VM_PARAM_INIT, VIRTUAL_SYSTEM_STRING);

        SECTION_MAP.put(VMParams.VM_PARAM_ON_POWEROFF, VIRTUAL_SYSTEM_OPTIONS);
        SECTION_MAP.put(VMParams.VM_PARAM_ON_REBOOT,     VIRTUAL_SYSTEM_OPTIONS);
        SECTION_MAP.put(VMParams.VM_PARAM_ON_CRASH,      VIRTUAL_SYSTEM_OPTIONS);

        SECTION_MAP.put(VMParams.VM_PARAM_ACPI, VIRTUAL_SYSTEM_FEATURES);
        SECTION_MAP.put(VMParams.VM_PARAM_APIC, VIRTUAL_SYSTEM_FEATURES);
        SECTION_MAP.put(VMParams.VM_PARAM_PAE, VIRTUAL_SYSTEM_FEATURES);
        SECTION_MAP.put(VMParams.VM_PARAM_HAP, VIRTUAL_SYSTEM_FEATURES);

        SECTION_MAP.put(VMParams.VM_PARAM_CLOCK_OFFSET,   VIRTUAL_SYSTEM_STRING);

        SECTION_MAP.put(VMParams.VM_PARAM_CPU_MATCH, CPU_MATCH_OPTIONS);
        SECTION_MAP.put(VMParams.VM_PARAM_CPUMATCH_MODEL, CPU_MATCH_OPTIONS);
        SECTION_MAP.put(VMParams.VM_PARAM_CPUMATCH_VENDOR, CPU_MATCH_OPTIONS);
        SECTION_MAP.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS, CPU_MATCH_OPTIONS);
        SECTION_MAP.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_CORES, CPU_MATCH_OPTIONS);
        SECTION_MAP.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS, CPU_MATCH_OPTIONS);
        SECTION_MAP.put(VMParams.VM_PARAM_CPUMATCH_FEATURE_POLICY, CPU_MATCH_OPTIONS);
        SECTION_MAP.put(VMParams.VM_PARAM_CPUMATCH_FEATURES, CPU_MATCH_OPTIONS);

        SHORTNAME_MAP.put(VMParams.VM_PARAM_NAME, Tools.getString("DomainInfo.Short.Name"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_DOMAIN_TYPE, Tools.getString("DomainInfo.Short.DomainType"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_VCPU, Tools.getString("DomainInfo.Short.Vcpu"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_BOOTLOADER, Tools.getString("DomainInfo.Short.Bootloader"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_CURRENTMEMORY, Tools.getString("DomainInfo.Short.CurrentMemory"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_MEMORY, Tools.getString("DomainInfo.Short.Memory"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_BOOT, Tools.getString("DomainInfo.Short.Os.Boot"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_BOOT_2, Tools.getString("DomainInfo.Short.Os.Boot.2"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_LOADER, Tools.getString("DomainInfo.Short.Os.Loader"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_AUTOSTART, Tools.getString("DomainInfo.Short.Autostart"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_VIRSH_OPTIONS, Tools.getString("DomainInfo.Short.VirshOptions"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_TYPE, Tools.getString("DomainInfo.Short.Type"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_INIT, Tools.getString("DomainInfo.Short.Init"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_TYPE_ARCH, Tools.getString("DomainInfo.Short.Arch"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_TYPE_MACHINE, Tools.getString("DomainInfo.Short.Machine"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_ACPI, Tools.getString("DomainInfo.Short.Acpi"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_APIC, Tools.getString("DomainInfo.Short.Apic"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_PAE, Tools.getString("DomainInfo.Short.Pae"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_HAP, Tools.getString("DomainInfo.Short.Hap"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_CLOCK_OFFSET, Tools.getString("DomainInfo.Short.Clock.Offset"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_CPU_MATCH, Tools.getString("DomainInfo.Short.CPU.Match"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_CPUMATCH_MODEL, Tools.getString("DomainInfo.Short.CPUMatch.Model"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_CPUMATCH_VENDOR, Tools.getString("DomainInfo.Short.CPUMatch.Vendor"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS,
                          Tools.getString("DomainInfo.Short.CPUMatch.TopologySockets"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_CORES,
                          Tools.getString("DomainInfo.Short.CPUMatch.TopologyCores"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS,
                          Tools.getString("DomainInfo.Short.CPUMatch.TopologyThreads"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_CPUMATCH_FEATURE_POLICY,
                          Tools.getString("DomainInfo.Short.CPUMatch.Policy"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_CPUMATCH_FEATURES, Tools.getString("DomainInfo.Short.CPUMatch.Features"));

        SHORTNAME_MAP.put(VMParams.VM_PARAM_ON_POWEROFF, Tools.getString("DomainInfo.Short.OnPoweroff"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_ON_REBOOT, Tools.getString("DomainInfo.Short.OnReboot"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_ON_CRASH, Tools.getString("DomainInfo.Short.OnCrash"));
        SHORTNAME_MAP.put(VMParams.VM_PARAM_EMULATOR, Tools.getString("DomainInfo.Short.Emulator"));

        FIELD_TYPES.put(VMParams.VM_PARAM_CURRENTMEMORY, Widget.Type.TEXTFIELDWITHUNIT);
        FIELD_TYPES.put(VMParams.VM_PARAM_MEMORY, Widget.Type.TEXTFIELDWITHUNIT);
        FIELD_TYPES.put(VMParams.VM_PARAM_APIC, Widget.Type.CHECKBOX);
        FIELD_TYPES.put(VMParams.VM_PARAM_ACPI, Widget.Type.CHECKBOX);
        FIELD_TYPES.put(VMParams.VM_PARAM_PAE, Widget.Type.CHECKBOX);
        FIELD_TYPES.put(VMParams.VM_PARAM_HAP, Widget.Type.CHECKBOX);

        PREFERRED_MAP.put(VMParams.VM_PARAM_CURRENTMEMORY, new StringValue("512", VmsXml.getUnitMiBytes()));
        PREFERRED_MAP.put(VMParams.VM_PARAM_MEMORY, new StringValue("512", VmsXml.getUnitMiBytes()));
        PREFERRED_MAP.put(VMParams.VM_PARAM_TYPE, TYPE_HVM);
        PREFERRED_MAP.put(VMParams.VM_PARAM_TYPE_ARCH, new StringValue("x86_64"));
        PREFERRED_MAP.put(VMParams.VM_PARAM_TYPE_MACHINE, new StringValue("pc"));
        PREFERRED_MAP.put(VMParams.VM_PARAM_ACPI, VM_TRUE);
        PREFERRED_MAP.put(VMParams.VM_PARAM_APIC, VM_TRUE);
        PREFERRED_MAP.put(VMParams.VM_PARAM_PAE, VM_TRUE);
        PREFERRED_MAP.put(VMParams.VM_PARAM_CLOCK_OFFSET, new StringValue("utc"));
        PREFERRED_MAP.put(VMParams.VM_PARAM_ON_POWEROFF, new StringValue("destroy"));
        PREFERRED_MAP.put(VMParams.VM_PARAM_ON_REBOOT, new StringValue("restart"));
        PREFERRED_MAP.put(VMParams.VM_PARAM_ON_CRASH, new StringValue("restart"));
        PREFERRED_MAP.put(VMParams.VM_PARAM_EMULATOR, new StringValue("/usr/bin/kvm"));
        DEFAULTS_MAP.put(VMParams.VM_PARAM_AUTOSTART, null);
        DEFAULTS_MAP.put(VMParams.VM_PARAM_VIRSH_OPTIONS, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMParams.VM_PARAM_BOOT, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMParams.VM_PARAM_DOMAIN_TYPE, new StringValue(DOMAIN_TYPE_QEMU));
        DEFAULTS_MAP.put(VMParams.VM_PARAM_VCPU, new StringValue("1"));
        DEFAULTS_MAP.put(VMParams.VM_PARAM_ACPI, VM_FALSE);
        DEFAULTS_MAP.put(VMParams.VM_PARAM_APIC, VM_FALSE);
        DEFAULTS_MAP.put(VMParams.VM_PARAM_PAE, VM_FALSE);
        DEFAULTS_MAP.put(VMParams.VM_PARAM_HAP, VM_FALSE);

        DEFAULTS_MAP.put(VMParams.VM_PARAM_CPU_MATCH, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMParams.VM_PARAM_CPUMATCH_MODEL, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMParams.VM_PARAM_CPUMATCH_VENDOR, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_CORES, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMParams.VM_PARAM_CPUMATCH_FEATURE_POLICY, NO_SELECTION_VALUE);
        DEFAULTS_MAP.put(VMParams.VM_PARAM_CPUMATCH_FEATURES, NO_SELECTION_VALUE);

        HAS_UNIT_PREFIX.put(VMParams.VM_PARAM_MEMORY, true);
        HAS_UNIT_PREFIX.put(VMParams.VM_PARAM_CURRENTMEMORY, true);

        // TODO: no virsh command for os-boot
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_BOOT, new Value[]{BOOT_HD, BOOT_NETWORK, BOOT_CDROM, BOOT_FD});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_BOOT_2, new Value[]{new StringValue(),
                                                                BOOT_HD,
                                                                BOOT_NETWORK,
                                                                BOOT_CDROM,
                                                                BOOT_FD});

        POSSIBLE_VALUES.put(VMParams.VM_PARAM_LOADER, new Value[]{});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_DOMAIN_TYPE,
                            new Value[]{new StringValue(DOMAIN_TYPE_QEMU),
                                        new StringValue(DOMAIN_TYPE_XEN),
                                        new StringValue(DOMAIN_TYPE_LXC),
                                        new StringValue(DOMAIN_TYPE_OPENVZ),
                                        new StringValue(DOMAIN_TYPE_VBOX),
                                        new StringValue(DOMAIN_TYPE_UML),
                                        new StringValue(DOMAIN_TYPE_KVM)});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_BOOTLOADER,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("/usr/bin/pygrub")});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_TYPE,
                            new Value[]{TYPE_HVM, TYPE_LINUX, TYPE_EXE});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_TYPE_ARCH,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("x86_64"),
                                        new StringValue("i686")});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_TYPE_MACHINE,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("pc"),
                                        new StringValue("pc-0.12")});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_INIT,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("/bin/sh"),
                                        new StringValue("/init")});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_CLOCK_OFFSET,
                            new Value[]{new StringValue("utc"),
                                        new StringValue("localtime")});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_ON_POWEROFF,
                            new Value[]{new StringValue("destroy"),
                                        new StringValue("restart"),
                                        new StringValue("preserve"),
                                        new StringValue("rename-restart")});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_ON_REBOOT,
                            new Value[]{new StringValue("restart"),
                                        new StringValue("destroy"),
                                        new StringValue("preserve"),
                                        new StringValue("rename-restart")});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_ON_CRASH,
                            new Value[]{new StringValue("restart"),
                                         new StringValue("destroy"),
                                         new StringValue("preserve"),
                                         new StringValue("rename-restart"),
                                         new StringValue("coredump-destroy"), /* since 0.8.4 */
                                         new StringValue("coredump-restart")}); /* since 0.8.4*/
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_EMULATOR,
                            new Value[]{new StringValue("/usr/bin/kvm"),
                                        new StringValue("/usr/bin/qemu")});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_CPU_MATCH,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("exact"),
                                        new StringValue("minimum"),
                                        new StringValue("strict")});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("1"),
                                        new StringValue("2")});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_CORES,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("1"),
                                        new StringValue("2"),
                                        new StringValue("4"),
                                        new StringValue("8")});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("1"),
                                        new StringValue("2")});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_CPUMATCH_FEATURE_POLICY,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("force"),
                                        new StringValue("require"),
                                        new StringValue("optional"),
                                        new StringValue("disable"),
                                        new StringValue("forbid")});
        POSSIBLE_VALUES.put(VMParams.VM_PARAM_CPUMATCH_FEATURES,
                            new Value[]{NO_SELECTION_VALUE,
                                        new StringValue("aes"),
                                        new StringValue("aes apic")});
        IS_INTEGER.add(VMParams.VM_PARAM_VCPU);
        IS_INTEGER.add(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS);
        IS_INTEGER.add(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_CORES);
        IS_INTEGER.add(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS);
        REQUIRED_VERSION.put(VMParams.VM_PARAM_CPU_MATCH, "0.7.5");
        REQUIRED_VERSION.put(VMParams.VM_PARAM_CPUMATCH_MODEL, "0.7.5");
        REQUIRED_VERSION.put(VMParams.VM_PARAM_CPUMATCH_VENDOR, "0.8.3");
        REQUIRED_VERSION.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_SOCKETS, "0.7.5");
        REQUIRED_VERSION.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_CORES, "0.7.5");
        REQUIRED_VERSION.put(VMParams.VM_PARAM_CPUMATCH_TOPOLOGY_THREADS, "0.7.5");
        REQUIRED_VERSION.put(VMParams.VM_PARAM_CPUMATCH_FEATURE_POLICY, "0.7.5");
        REQUIRED_VERSION.put(VMParams.VM_PARAM_CPUMATCH_FEATURES, "0.7.5");
    }

    private JComponent infoPanel = null;
    private String uuid;
    /**
     * HTML string on which hosts the vm is defined.
     */
    private String definedOnString = "";
    /**
     * HTML string on which hosts the vm is running.
     */
    private String runningOnString = "";
    /**
     * Row color, that is color of host on which is it running or null.
     */
    private Color rowColor = Browser.PANEL_BACKGROUND;
    private final ReadWriteLock mTransitionLock = new ReentrantReadWriteLock();
    private final Lock mTransitionReadLock = mTransitionLock.readLock();
    private final Lock mTransitionWriteLock = mTransitionLock.writeLock();
    private final Set<String> starting = new HashSet<>();
    private final Set<String> shuttingdown = new HashSet<>();
    private final Set<String> suspending = new HashSet<>();
    private final Set<String> resuming = new HashSet<>();
    /**
     * Progress indicator when starting or stopping.
     */
    private final StringBuilder progress = new StringBuilder("-");
    private final Lock mDiskToInfoLock = new ReentrantLock();
    private final Map<String, DiskInfo> diskToInfo = new HashMap<>();
    /**
     * Map from target string in the table to vms disk info object.
     */
    private volatile Map<String, DiskInfo> diskKeyToInfo = new HashMap<>();

    private final Lock mFilesystemToInfoLock = new ReentrantLock();
    private final Map<String, FilesystemInfo> filesystemToInfo = new HashMap<>();
    private volatile Map<String, FilesystemInfo> filesystemKeyToInfo = new HashMap<>();

    private final Lock mInterfaceToInfoLock = new ReentrantLock();
    private final Map<String, InterfaceInfo> interfaceToInfo = new HashMap<>();
    private volatile Map<String, InterfaceInfo> interfaceKeyToInfo = new HashMap<>();
    private final Lock mInputDevToInfoLock = new ReentrantLock();
    private final Map<String, InputDevInfo> inputDevToInfo = new HashMap<>();
    private volatile Map<String, InputDevInfo> inputDevKeyToInfo = new HashMap<>();

    private final Lock mGraphicsToInfoLock = new ReentrantLock();
    private final Map<String, GraphicsInfo> graphicsToInfo = new HashMap<>();
    private volatile Map<String, GraphicsInfo> graphicsKeyToInfo = new HashMap<>();

    private final Lock mSoundToInfoLock = new ReentrantLock();
    private final Map<String, SoundInfo> soundToInfo = new HashMap<>();
    private volatile Map<String, SoundInfo> soundKeyToInfo = new HashMap<>();

    private final Lock mSerialToInfoLock = new ReentrantLock();
    private final Map<String, SerialInfo> serialToInfo = new HashMap<>();
    private volatile Map<String, SerialInfo> serialKeyToInfo = new HashMap<>();

    private final Lock mParallelToInfoLock = new ReentrantLock();
    private final Map<String, ParallelInfo> parallelToInfo = new HashMap<>();
    /**
     * Preferred emulator. It's distro dependent.
     */
    private String preferredEmulator;
    private volatile Map<String, ParallelInfo> parallelKeyToInfo = new HashMap<>();

    private final Lock mVideoToInfoLock = new ReentrantLock();
    private final Map<String, VideoInfo> videoToInfo = new HashMap<>();
    private final Map<String, MyButton> hostButtons = new HashMap<>();
    /**
     * Whether it is used by CRM.
     */
    private boolean usedByCRM = false;
    private volatile Map<String, VideoInfo> videoKeyToInfo = new HashMap<>();
    /**
     * Previous type.
     */
    private volatile Value prevType = null;
    private Value[] autostartPossibleValues;
    /**
     * This is a map from host to the check box.
     */
    private final Map<String, Widget> definedOnHostComboBoxHash = new HashMap<>();
    private final ProgressIndicator progressIndicator;
    private final Application application;
    private final SwingUtils swingUtils;
    private final DomainMenu domainMenu;
    private final Provider<DiskInfo> diskInfoProvider;
    private final Provider<FilesystemInfo> filesystemInfoProvider;
    private final Provider<InterfaceInfo> interfaceInfoProvider;
    private final Provider<InputDevInfo> inputDevInfoProvider;
    private final Provider<GraphicsInfo> graphicsInfoProvider;
    private final Provider<SoundInfo> soundInfoProvider;
    private final Provider<SerialInfo> serialInfoProvider;
    private final Provider<ParallelInfo> parallelInfoProvider;
    private final Provider<VideoInfo> videoInfoProvider;
    private final Provider<VmsXml> vmsXmlProvider;
    private final WidgetFactory widgetFactory;
    private final ClusterTreeMenu clusterTreeMenu;

    public DomainInfo(Application application, SwingUtils swingUtils, Access access, MainData mainData, WidgetFactory widgetFactory,
            ProgressIndicator progressIndicator, DomainMenu domainMenu, Provider<DiskInfo> diskInfoProvider,
            Provider<ParallelInfo> parallelInfoProvider, Provider<FilesystemInfo> filesystemInfoProvider,
            Provider<InterfaceInfo> interfaceInfoProvider, Provider<InputDevInfo> inputDevInfoProvider,
            Provider<GraphicsInfo> graphicsInfoProvider, Provider<VideoInfo> videoInfoProvider, Provider<VmsXml> vmsXmlProvider,
            Provider<SoundInfo> soundInfoProvider, ClusterTreeMenu clusterTreeMenu, Provider<SerialInfo> serialInfoProvider) {
        super(application, swingUtils, access, mainData, widgetFactory);
        this.swingUtils = swingUtils;
        this.progressIndicator = progressIndicator;
        this.application = application;
        this.domainMenu = domainMenu;
        this.diskInfoProvider = diskInfoProvider;
        this.parallelInfoProvider = parallelInfoProvider;
        this.filesystemInfoProvider = filesystemInfoProvider;
        this.widgetFactory = widgetFactory;
        this.interfaceInfoProvider = interfaceInfoProvider;
        this.inputDevInfoProvider = inputDevInfoProvider;
        this.graphicsInfoProvider = graphicsInfoProvider;
        this.videoInfoProvider = videoInfoProvider;
        this.vmsXmlProvider = vmsXmlProvider;
        this.soundInfoProvider = soundInfoProvider;
        this.clusterTreeMenu = clusterTreeMenu;
        this.serialInfoProvider = serialInfoProvider;
    }

    public void einit(final String name, final Browser browser) {
        super.einit(Optional.of(new ResourceValue(name)), name, browser);
        final Host firstHost = getBrowser().getClusterHosts()[0];
        preferredEmulator = firstHost.getHostParser()
                                     .getDistString("KVM.emulator");
        final List<Value> hostsList = new ArrayList<>();
        hostsList.add(null);
        for (final Host h : getBrowser().getClusterHosts()) {
            hostsList.add(new StringValue(h.getName()));
        }
        autostartPossibleValues = hostsList.toArray(new Value[0]);
    }

    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /**
     * Returns a name of the service with virtual domain name.
     */
    @Override
    public String toString() {
        if (getResource().isNew()) {
            return "new domain...";
        } else {
            return getName();
        }
    }

    /**
     * Returns domain name.
     */
    protected String getDomainName() {
        return getName();
    }

    /**
     * Updates disk nodes. Returns whether the node changed.
     */
    private boolean updateDiskNodes() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
        final Map<String, DiskData> disks = getDisks();
        final Collection<String> diskNames = new ArrayList<>();
        if (disks != null) {
            diskNames.addAll(disks.keySet());
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();
        boolean nodeChanged = false;
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (!(info instanceof DiskInfo)) {
                continue;
            }
            final DiskInfo diskInfo = (DiskInfo) info;
            if (diskInfo.getResource().isNew()) {
                /* keep */
            } else if (diskNames.contains(diskInfo.getName())) {
                /* keeping */
                diskNames.remove(diskInfo.getName());
                mDiskToInfoLock.lock();
                try {
                    diskToInfo.put(diskInfo.getName(), diskInfo);
                } finally {
                    mDiskToInfoLock.unlock();
                }
                diskInfo.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                mDiskToInfoLock.lock();
                try {
                    diskToInfo.remove(diskInfo.getName());
                } finally {
                    mDiskToInfoLock.unlock();
                }
                nodesToRemove.add(diskInfo.getNode());
                nodeChanged = true;
            }
        }

        /* remove nodes */
        swingUtils.isSwingThread();
        clusterTreeMenu.removeFromParent(nodesToRemove);

        for (final String disk : diskNames) {
            int i = 0;
            for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
                if (!(info instanceof DiskInfo)) {
                    continue;
                }
                final DiskInfo diskInfo = (DiskInfo) info;
                final String name = diskInfo.getName();
                if (name != null && disk.compareTo(name) < 0) {
                    break;
                }
                i++;
            }
            /* add new vm disk */
            final DiskInfo diskInfo = diskInfoProvider.get();
            diskInfo.init(disk, getBrowser(), this);
            mDiskToInfoLock.lock();
            try {
                diskToInfo.put(disk, diskInfo);
            } finally {
                mDiskToInfoLock.unlock();
            }
            diskInfo.updateParameters();
            clusterTreeMenu.createMenuItem(thisNode, diskInfo, i);
            nodeChanged = true;
        }
        return nodeChanged;
    }

    /**
     * Updates FS nodes. Returns whether the node changed.
     */
    private boolean updateFilesystemNodes() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
        final Map<String, FilesystemData> filesystems = getFilesystems();
        final Collection<String> filesystemNames = new ArrayList<>();
        if (filesystems != null) {
            filesystemNames.addAll(filesystems.keySet());
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();
        boolean nodeChanged = false;
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (!(info instanceof FilesystemInfo)) {
                continue;
            }
            final FilesystemInfo filesystemInfo = (FilesystemInfo) info;
            if (filesystemInfo.getResource().isNew()) {
                /* keep */
            } else if (filesystemNames.contains(filesystemInfo.getName())) {
                /* keeping */
                filesystemNames.remove(filesystemInfo.getName());
                mFilesystemToInfoLock.lock();
                try {
                    filesystemToInfo.put(filesystemInfo.getName(), filesystemInfo);
                } finally {
                    mFilesystemToInfoLock.unlock();
                }
                filesystemInfo.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                mFilesystemToInfoLock.lock();
                try {
                    filesystemToInfo.remove(filesystemInfo.getName());
                } finally {
                    mFilesystemToInfoLock.unlock();
                }
                nodesToRemove.add(filesystemInfo.getNode());
                nodeChanged = true;
            }
        }

        /* remove nodes */
        swingUtils.isSwingThread();
        clusterTreeMenu.removeFromParent(nodesToRemove);

        for (final String filesystem : filesystemNames) {
            int i = 0;
            for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
                if (!(info instanceof FilesystemInfo)) {
                    continue;
                }
                final FilesystemInfo filesystemInfo = (FilesystemInfo) info;
                final String name = filesystemInfo.getName();
                if (name != null && filesystem.compareTo(name) < 0) {
                    break;
                }
                i++;
            }
            /* add new vm fs */
            final FilesystemInfo filesystemInfo = filesystemInfoProvider.get();
            filesystemInfo.init(filesystem, getBrowser(), this);
            mFilesystemToInfoLock.lock();
            try {
                filesystemToInfo.put(filesystem, filesystemInfo);
            } finally {
                mFilesystemToInfoLock.unlock();
            }
            filesystemInfo.updateParameters();
            clusterTreeMenu.createMenuItem(thisNode, filesystemInfo, i);
            nodeChanged = true;
        }
        return nodeChanged;
    }

    /**
     * Updates interface nodes. Returns whether the node changed.
     */
    private boolean updateInterfaceNodes() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
        final Map<String, InterfaceData> interfaces = getInterfaces();
        final Collection<String> interfaceNames = new ArrayList<>();
        if (interfaces != null) {
            interfaceNames.addAll(interfaces.keySet());
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();
        boolean nodeChanged = false;
        InterfaceInfo emptySlot = null; /* for generated mac address. */
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (!(info instanceof InterfaceInfo)) {
                continue;
            }
            final InterfaceInfo interfaceInfo = (InterfaceInfo) info;
            if (interfaceInfo.getResource().isNew()) {
                /* keep */
            } else if ("generate".equals(interfaceInfo.getName())) {
                emptySlot = interfaceInfo;
            } else if (interfaceNames.contains(interfaceInfo.getName())) {
                /* keeping */
                interfaceNames.remove(interfaceInfo.getName());
                interfaceInfo.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                mInterfaceToInfoLock.lock();
                try {
                    interfaceToInfo.remove(interfaceInfo.getName());
                } finally {
                    mInterfaceToInfoLock.unlock();
                }
                nodesToRemove.add(interfaceInfo.getNode());
                nodeChanged = true;
            }
        }

        /* remove nodes */
        swingUtils.isSwingThread();
        clusterTreeMenu.removeFromParent(nodesToRemove);

        for (final String interf : interfaceNames) {
            final InterfaceInfo interfaceInfo;
            if (emptySlot == null) {
                int i = 0;
                for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
                    if (!(info instanceof InterfaceInfo)) {
                        if (info instanceof DiskInfo || info instanceof FilesystemInfo) {
                            i++;
                        }
                        continue;
                    }
                    final InterfaceInfo v = (InterfaceInfo) info;

                    final String name = v.getName();
                    if (name != null && interf.compareTo(name) < 0) {
                        break;
                    }
                    i++;
                }
                /* add new vm interface */
                interfaceInfo = interfaceInfoProvider.get();
                interfaceInfo.init(interf, getBrowser(), this);
                clusterTreeMenu.createMenuItem(thisNode, interfaceInfo, i);
                nodeChanged = true;
            } else {
                interfaceInfo = emptySlot;
                interfaceInfo.setName(interf);
                emptySlot = null;
            }
            mInterfaceToInfoLock.lock();
            try {
                interfaceToInfo.put(interf, interfaceInfo);
            } finally {
                mInterfaceToInfoLock.unlock();
            }
            interfaceInfo.updateParameters();
        }
        return nodeChanged;
    }

    /**
     * Updates input devices nodes. Returns whether the node changed.
     */
    private boolean updateInputDevNodes() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
        final Map<String, InputDevData> inputDevs = getInputDevs();
        final Collection<String> inputDevNames = new ArrayList<>();
        if (inputDevs != null) {
            inputDevNames.addAll(inputDevs.keySet());
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();
        boolean nodeChanged = false;
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (!(info instanceof InputDevInfo)) {
                continue;
            }
            final InputDevInfo inputDevInfo = (InputDevInfo) info;
            if (inputDevInfo.getResource().isNew()) {
                /* keep */
            } else if (inputDevNames.contains(inputDevInfo.getName())) {
                /* keeping */
                inputDevNames.remove(inputDevInfo.getName());
                inputDevInfo.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                mInputDevToInfoLock.lock();
                try {
                    inputDevToInfo.remove(inputDevInfo.getName());
                } finally {
                    mInputDevToInfoLock.unlock();
                }
                nodesToRemove.add(inputDevInfo.getNode());
                nodeChanged = true;
            }
        }

        /* remove nodes */
        swingUtils.isSwingThread();
        clusterTreeMenu.removeFromParent(nodesToRemove);

        for (final String inputDev : inputDevNames) {
            int i = 0;
            for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
                if (!(info instanceof InputDevInfo)) {
                    if (info instanceof DiskInfo || info instanceof FilesystemInfo || info instanceof InterfaceInfo) {
                        i++;
                    }
                    continue;
                }
                final InputDevInfo inputDevInfo = (InputDevInfo) info;
                final String n = inputDevInfo.getName();
                if (n != null && inputDev.compareTo(n) < 0) {
                    break;
                }
                i++;
            }
            /* add new vm input device */
            final InputDevInfo inputDevInfo = inputDevInfoProvider.get();
            inputDevInfo.init(inputDev, getBrowser(), this);
            clusterTreeMenu.createMenuItem(thisNode, inputDevInfo, i);
            nodeChanged = true;
            mInputDevToInfoLock.lock();
            try {
                inputDevToInfo.put(inputDev, inputDevInfo);
            } finally {
                mInputDevToInfoLock.unlock();
            }
            inputDevInfo.updateParameters();
        }
        /* Sort it. */
        clusterTreeMenu.sortChildrenLeavingNewUp(thisNode);
        return nodeChanged;
    }

    /**
     * Updates graphics devices nodes. Returns whether the node changed.
     */
    private boolean updateGraphicsNodes() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return false;
        }
        final Map<String, GraphicsData> graphicDisplays = getGraphicDisplays();
        final Collection<String> graphicsNames = new ArrayList<>();
        if (graphicDisplays != null) {
            graphicsNames.addAll(graphicDisplays.keySet());
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();
        boolean nodeChanged = false;
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (!(info instanceof GraphicsInfo)) {
                continue;
            }
            final GraphicsInfo graphicsInfo = (GraphicsInfo) info;
            if (graphicsInfo.getResource().isNew()) {
                /* keep */
            } else if (graphicsNames.contains(graphicsInfo.getName())) {
                /* keeping */
                graphicsNames.remove(graphicsInfo.getName());
                mGraphicsToInfoLock.lock();
                try {
                    graphicsToInfo.put(graphicsInfo.getName(), graphicsInfo);
                } finally {
                    mGraphicsToInfoLock.unlock();
                }
                graphicsInfo.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                mGraphicsToInfoLock.lock();
                try {
                    graphicsToInfo.remove(graphicsInfo.getName());
                } finally {
                    mGraphicsToInfoLock.unlock();
                }
                nodesToRemove.add(graphicsInfo.getNode());
                nodeChanged = true;
            }
        }

        /* remove nodes */
        swingUtils.isSwingThread();
        clusterTreeMenu.removeFromParent(nodesToRemove);

        for (final String graphicDisplay : graphicsNames) {
            int i = 0;
            for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
                if (!(info instanceof GraphicsInfo)) {
                    if (info instanceof DiskInfo
                        || info instanceof FilesystemInfo
                        || info instanceof InterfaceInfo
                        || info instanceof InputDevInfo) {
                        i++;
                    }
                    continue;
                }
                final GraphicsInfo graphicsInfo = (GraphicsInfo) info;
                final String n = graphicsInfo.getName();
                if (n != null && graphicDisplay.compareTo(n) < 0) {
                    break;
                }
                i++;
            }
            /* add new vm graphics device */
            final GraphicsInfo graphicsInfo = graphicsInfoProvider.get();
            graphicsInfo.init(graphicDisplay, getBrowser(), this);
            clusterTreeMenu.createMenuItem(thisNode, graphicsInfo, i);
            nodeChanged = true;
            mGraphicsToInfoLock.lock();
            try {
                graphicsToInfo.put(graphicDisplay, graphicsInfo);
            } finally {
                mGraphicsToInfoLock.unlock();
            }
            graphicsInfo.updateParameters();
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
        final Collection<String> soundNames = new ArrayList<>();
        if (sounds != null) {
            soundNames.addAll(sounds.keySet());
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();
        boolean nodeChanged = false;
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (!(info instanceof SoundInfo)) {
                continue;
            }
            final SoundInfo soundInfo = (SoundInfo) info;
            if (soundInfo.getResource().isNew()) {
                /* keep */
            } else if (soundNames.contains(soundInfo.getName())) {
                /* keeping */
                soundNames.remove(soundInfo.getName());
                mSoundToInfoLock.lock();
                try {
                    soundToInfo.put(soundInfo.getName(), soundInfo);
                } finally {
                    mSoundToInfoLock.unlock();
                }
                soundInfo.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                mSoundToInfoLock.lock();
                try {
                    soundToInfo.remove(soundInfo.getName());
                } finally {
                    mSoundToInfoLock.unlock();
                }
                nodesToRemove.add(soundInfo.getNode());
                nodeChanged = true;
            }
        }

        /* remove nodes */
        swingUtils.isSwingThread();
        clusterTreeMenu.removeFromParent(nodesToRemove);

        for (final String sound : soundNames) {
            int i = 0;
            for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
                if (!(info instanceof SoundInfo)) {
                    if (info instanceof DiskInfo
                        || info instanceof FilesystemInfo
                        || info instanceof InterfaceInfo
                        || info instanceof InputDevInfo
                        || info instanceof GraphicsInfo) {
                        i++;
                    }
                    continue;
                }
                final SoundInfo soundInfo = (SoundInfo) info;
                final String n = soundInfo.getName();
                if (n != null && sound.compareTo(n) < 0) {
                    break;
                }
                i++;
            }
            /* add new vm sound device */
            final SoundInfo soundInfo = soundInfoProvider.get();
            soundInfo.init(sound, getBrowser(), this);
            clusterTreeMenu.createMenuItem(thisNode, soundInfo, i);
            nodeChanged = true;
            mSoundToInfoLock.lock();
            try {
                soundToInfo.put(sound, soundInfo);
            } finally {
                mSoundToInfoLock.unlock();
            }
            soundInfo.updateParameters();
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
        final Collection<String> serialNames = new ArrayList<>();
        if (serials != null) {
            serialNames.addAll(serials.keySet());
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();
        boolean nodeChanged = false;
        SerialInfo emptySlot = null; /* for generated target port. */
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (!(info instanceof SerialInfo)) {
                continue;
            }
            final SerialInfo serialInfo = (SerialInfo) info;
            if (serialInfo.getResource().isNew()) {
                /* keep */
            } else if ("generate".equals(serialInfo.getTargetPort())) {
                emptySlot = serialInfo;
            } else if (serialNames.contains(serialInfo.getName())) {
                /* keeping */
                serialNames.remove(serialInfo.getName());
                serialInfo.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                mSerialToInfoLock.lock();
                try {
                    serialToInfo.remove(serialInfo.getName());
                } finally {
                    mSerialToInfoLock.unlock();
                }
                nodesToRemove.add(serialInfo.getNode());
                nodeChanged = true;
            }
        }

        /* remove nodes */
        swingUtils.isSwingThread();
        clusterTreeMenu.removeFromParent(nodesToRemove);

        for (final String serial : serialNames) {
            final SerialInfo serialInfo;
            if (emptySlot == null) {
                int i = 0;
                for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
                    if (!(info instanceof SerialInfo)) {
                        if (info instanceof DiskInfo
                            || info instanceof FilesystemInfo
                            || info instanceof InterfaceInfo
                            || info instanceof InputDevInfo
                            || info instanceof GraphicsInfo
                            || info instanceof SoundInfo) {
                            i++;
                        }
                        continue;
                    }
                    final SerialInfo v = (SerialInfo) info;
                    final String name = v.getName();
                    if (name != null && serial.compareTo(name) < 0) {
                        break;
                    }
                    i++;
                }
                /* add new vm serial device */
                serialInfo = serialInfoProvider.get();
                serialInfo.init(serial, getBrowser(), this);
                clusterTreeMenu.createMenuItem(thisNode, serialInfo, i);
                nodeChanged = true;
            } else {
                serialInfo = emptySlot;
                serialInfo.setName(serial);
                emptySlot = null;
            }
            mSerialToInfoLock.lock();
            try {
                serialToInfo.put(serial, serialInfo);
            } finally {
                mSerialToInfoLock.unlock();
            }
            serialInfo.updateParameters();
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
        final Collection<String> parallelNames = new ArrayList<>();
        if (parallels != null) {
            parallelNames.addAll(parallels.keySet());
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();
        boolean nodeChanged = false;
        ParallelInfo emptySlot = null; /* for generated target port. */
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (!(info instanceof ParallelInfo)) {
                continue;
            }
            final ParallelInfo parallelInfo = (ParallelInfo) info;
            if (parallelInfo.getResource().isNew()) {
                /* keep */
            } else if ("generate".equals(parallelInfo.getTargetPort())) {
                emptySlot = parallelInfo;
            } else if (parallelNames.contains(parallelInfo.getName())) {
                /* keeping */
                parallelNames.remove(parallelInfo.getName());
                parallelInfo.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                mParallelToInfoLock.lock();
                try {
                    parallelToInfo.remove(parallelInfo.getName());
                } finally {
                    mParallelToInfoLock.unlock();
                }
                nodesToRemove.add(parallelInfo.getNode());
                nodeChanged = true;
            }
        }

        /* remove nodes */
        swingUtils.isSwingThread();
        clusterTreeMenu.removeFromParent(nodesToRemove);

        for (final String parallel : parallelNames) {
            final ParallelInfo parallelInfo;
            if (emptySlot == null) {
                int i = 0;
                for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
                    if (!(info instanceof ParallelInfo)) {
                        if (info instanceof DiskInfo
                            || info instanceof FilesystemInfo
                            || info instanceof InterfaceInfo
                            || info instanceof InputDevInfo
                            || info instanceof GraphicsInfo
                            || info instanceof SoundInfo
                            || info instanceof SerialInfo) {
                            i++;
                        }
                        continue;
                    }
                    final ParallelInfo v = (ParallelInfo) info;
                    final String n = v.getName();
                    if (n != null && parallel.compareTo(n) < 0) {
                        break;
                    }
                    i++;
                }
                /* add new vm parallel device */
                parallelInfo = parallelInfoProvider.get();
                parallelInfo.init(parallel, getBrowser(), this);
                clusterTreeMenu.createMenuItem(thisNode, parallelInfo, i);
                nodeChanged = true;
            } else {
                parallelInfo = emptySlot;
                parallelInfo.setName(parallel);
                emptySlot = null;
            }
            mParallelToInfoLock.lock();
            try {
                parallelToInfo.put(parallel, parallelInfo);
            } finally {
                mParallelToInfoLock.unlock();
            }
            parallelInfo.updateParameters();
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
        final Collection<String> videoNames = new ArrayList<>();
        if (videos != null) {
            videoNames.addAll(videos.keySet());
        }
        final Collection<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();
        boolean nodeChanged = false;
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (!(info instanceof VideoInfo)) {
                continue;
            }
            final VideoInfo videoInfo = (VideoInfo) info;
            if (videoInfo.getResource().isNew()) {
                /* keep */
            } else if (videoNames.contains(videoInfo.getName())) {
                /* keeping */
                videoNames.remove(videoInfo.getName());
                mVideoToInfoLock.lock();
                try {
                    videoToInfo.put(videoInfo.getName(), videoInfo);
                } finally {
                    mVideoToInfoLock.unlock();
                }
                videoInfo.updateParameters(); /* update old */
            } else {
                /* remove not existing vms */
                mVideoToInfoLock.lock();
                try {
                    videoToInfo.remove(videoInfo.getName());
                } finally {
                    mVideoToInfoLock.unlock();
                }
                nodesToRemove.add(videoInfo.getNode());
                nodeChanged = true;
            }
        }

        /* remove nodes */
        swingUtils.isSwingThread();
        clusterTreeMenu.removeFromParent(nodesToRemove);

        for (final String video : videoNames) {
            int i = 0;
            for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
                if (!(info instanceof VideoInfo)) {
                    if (info instanceof DiskInfo
                        || info instanceof FilesystemInfo
                        || info instanceof InterfaceInfo
                        || info instanceof InputDevInfo
                        || info instanceof GraphicsInfo
                        || info instanceof SoundInfo
                        || info instanceof SerialInfo
                        || info instanceof ParallelInfo) {
                        i++;
                    }
                    continue;
                }
                final VideoInfo v = (VideoInfo) info;
                final String n = v.getName();
                if (n != null && video.compareTo(n) < 0) {
                    break;
                }
                i++;
            }
            /* add new vm video device */
            final VideoInfo videoInfo = videoInfoProvider.get();
            videoInfo.init(video, getBrowser(), this);
            clusterTreeMenu.createMenuItem(thisNode, videoInfo, i);
            nodeChanged = true;
            mVideoToInfoLock.lock();
            try {
                videoToInfo.put(video, videoInfo);
            } finally {
                mVideoToInfoLock.unlock();
            }
            videoInfo.updateParameters();
        }
        return nodeChanged;
    }

    /**
     * Returns button for defined hosts.
     */
    private MyButton getHostButton(final Host host, final String prefix) {
        final MyButton hostBtn = widgetFactory.createButton("Start", null, "not defined on " + host.getName());
        application.makeMiniButton(hostBtn);
        swingUtils.invokeLater(() -> hostBtn.setBackgroundColor(Browser.PANEL_BACKGROUND));
        hostBtn.addActionListener(e -> {
            LOG.debug1("actionPerformed: BUTTON: host: " + host.getName());
            final Thread t = new Thread(() -> {
                final VmsXml vxml = getBrowser().getVmsXml(host);
                if (vxml != null) {
                    if (hostBtn.getIcon() == VNC_ICON) {
                        final int remotePort = vxml.getRemotePort(getDomainName());
                        application.startTightVncViewer(host, remotePort);
                    } else if (hostBtn.getIcon() == HostBrowser.HOST_ON_ICON) {
                        swingUtils.invokeLater(() -> hostBtn.setEnabled(false));
                        start(host);
                    }
                }
            });
            t.start();
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

    /**
     * Sets service parameters with values from resourceNode hash.
     */
    public void updateParameters() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return;
        }
        final List<String> runningOnHosts = new ArrayList<>();
        final List<String> suspendedOnHosts = new ArrayList<>();
        final List<String> definedhosts = new ArrayList<>();
        for (final Host h : getBrowser().getClusterHosts()) {
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            final String hostName = h.getName();
            if (vmsXml != null && vmsXml.getDomainNames().contains(getDomainName())) {
                if (vmsXml.isRunning(getDomainName())) {
                    if (vmsXml.isSuspended(getDomainName())) {
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
                definedhosts.add("<font color=\"#A3A3A3\">" + hostName + "</font>");
            }
        }
        definedOnString = "<html>" + Tools.join(", ", definedhosts.toArray(new String[0])) + "</html>";
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
                            "<html>Starting on: " + Tools.join(", ", starting.toArray(new String[0])) + progress + "</html>";
                } else if (!shuttingdown.isEmpty()) {
                    runningOnString = "<html>Shutting down on: " + Tools.join(", ", shuttingdown.toArray(new String[0])) + progress
                                      + "</html>";
                } else if (!suspending.isEmpty()) {
                    runningOnString =
                            "<html>Suspending on: " + Tools.join(", ", suspending.toArray(new String[0])) + progress + "</html>";
                } else if (!resuming.isEmpty()) {
                    runningOnString =
                            "<html>Resuming on: " + Tools.join(", ", resuming.toArray(new String[0])) + progress + "</html>";
                } else if (!suspendedOnHosts.isEmpty()) {
                    runningOnString = "<html>Paused on: " + Tools.join(", ", suspendedOnHosts.toArray(new String[0])) + "</html>";
                } else {
                    runningOnString = "<html>Running on: " + Tools.join(", ", runningOnHosts.toArray(new String[0])) + "</html>";
                }
            } finally {
                mTransitionReadLock.unlock();
            }
        }
        for (final Host h : getBrowser().getClusterHosts()) {
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            final Widget hwi = definedOnHostComboBoxHash.get(h.getName());
            if (hwi != null) {
                final Value value;
                if (vmsXml != null && vmsXml.getDomainNames().contains(getDomainName())) {
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
                final VmsXml vmsXml = getBrowser().getVmsXml(h);
                if (vmsXml != null && value == null) {
                    final Value savedValue;
                    if (VMParams.VM_PARAM_CURRENTMEMORY.equals(param) || VMParams.VM_PARAM_MEMORY.equals(param)) {
                        savedValue = VmsXml.convertKilobytes(vmsXml.getValue(getDomainName(), param));
                    } else {
                        savedValue = new StringValue(vmsXml.getValue(getDomainName(), param));
                    }
                    if (savedValue.isNothingSelected()) {
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
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            if (vmsXml != null) {
                uuid = vmsXml.getValue(getDomainName(), VMParams.VM_PARAM_UUID);
            }
        }
        swingUtils.invokeInEdt(() -> {
            final boolean interfaceNodeChanged = updateInterfaceNodes();
            final boolean diskNodeChanged = updateDiskNodes();
            final boolean filesystemNodeChanged = updateFilesystemNodes();
            final boolean inputDevNodeChanged = updateInputDevNodes();
            final boolean graphicsNodeChanged = updateGraphicsNodes();
            final boolean soundNodeChanged = updateSoundNodes();
            final boolean serialNodeChanged = updateSerialNodes();
            final boolean parallelNodeChanged = updateParallelNodes();
            final boolean videoNodeChanged = updateVideoNodes();
            if (interfaceNodeChanged || diskNodeChanged || filesystemNodeChanged || inputDevNodeChanged || graphicsNodeChanged
                || soundNodeChanged || serialNodeChanged || parallelNodeChanged || videoNodeChanged) {
                clusterTreeMenu.reloadNodeDontSelect(thisNode);
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
        swingUtils.invokeLater(() -> {
            setApplyButtons(null, getParametersFromXML());
            clusterTreeMenu.repaintMenuTree();
        });
    }

    /** Returns "Defined on" panel. */
    public JPanel getDefinedOnHostsPanel(final String prefix, final MyButton thisApplyButton) {
        final JPanel hostPanel = new JPanel(new SpringLayout());
        int rows = 0;
        boolean running = false;
        for (final Host host : getBrowser().getClusterHosts()) {
            final VmsXml vmsXml = getBrowser().getVmsXml(host);
            if (vmsXml != null && vmsXml.isRunning(getDomainName())) {
                running = true;
            }
            if (vmsXml != null && !vmsXml.getDomainNames().contains(getDomainName())) {
                final boolean notDefined = false;
            }
            final Value defaultValue;
            if (host.isConnected()
                && (getResource().isNew()
                    || (vmsXml != null && vmsXml.getDomainNames().contains(getDomainName())))) {
                defaultValue = DEFINED_ON_HOST_TRUE;
            } else {
                defaultValue = DEFINED_ON_HOST_FALSE;
            }
            final MyButton hostBtn = getHostButton(host, prefix);
            final Widget wi = widgetFactory.createInstance(
                    Widget.Type.CHECKBOX,
                    defaultValue,
                    Widget.NO_ITEMS,
                    Widget.NO_REGEXP,
                    application.getServiceFieldWidth() * 2,
                    Widget.NO_ABBRV,
                    new AccessMode(AccessMode.ADMIN, AccessMode.NORMAL),
                    hostBtn);
            Widget rpwi = null;
            if (prefix == null) {
                definedOnHostComboBoxHash.put(host.getName(), wi);
            } else {
                definedOnHostComboBoxHash.put(prefix + ':' + host.getName(), wi);
                rpwi = definedOnHostComboBoxHash.get(host.getName());
            }
            final Widget realParamWi = rpwi;
            if (!host.isConnected()) {
                swingUtils.invokeLater(() -> wi.setEnabled(false));
            }
            wi.addListeners(new WidgetListener() {
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
                     application.getServiceLabelWidth(),
                     application.getServiceFieldWidth() * 2,
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

    @Override
    public JComponent getInfoPanel() {
        swingUtils.isSwingThread();
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
            final JComponent newVMButton = getBrowser().getVmsInfo().getNewButton();
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
            getApplyButton().addActionListener(e -> {
                LOG.debug1("actionPerformed: BUTTON: apply");
                final Thread thread = new Thread(() -> {
                    getBrowser().clStatusLock();
                    apply(Application.RunMode.LIVE);
                    getBrowser().clStatusUnlock();
                });
                thread.start();
            });
            getRevertButton().addActionListener(e -> {
                LOG.debug1("actionPerformed: BUTTON: revert");
                final Thread thread = new Thread(() -> {
                    getBrowser().clStatusLock();
                    revert();
                    getBrowser().clStatusUnlock();
                });
                thread.start();
            });
        }
        final JPanel extraButtonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        extraButtonPanel.setBackground(Browser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.add(extraButtonPanel);
        addApplyButton(buttonPanel);
        addRevertButton(extraButtonPanel);
        final MyButton overviewButton = widgetFactory.createButton("VMs Overview", BACK_ICON);
        application.makeMiniButton(overviewButton);
        overviewButton.setPreferredSize(new Dimension(130, 50));
        overviewButton.addActionListener(e -> {
            LOG.debug1("actionPerformed: BUTTON: overview");
            getBrowser().getVmsInfo().selectMyself();
        });
        extraButtonPanel.add(overviewButton);
        /* define on hosts */
        optionsPanel.add(getDefinedOnHostsPanel(null, getApplyButton()));
        addParams(optionsPanel, params, application.getServiceLabelWidth(), application.getServiceFieldWidth() * 2, null);
        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.LINE_END);
        mainPanel.add(optionsPanel);

        final MyButton newDiskBtn = getNewDiskBtn();
        final MyButton newFilesystemBtn = getNewFilesystemBtn();
        final MyButton newInterfaceBtn = getNewInterfaceBtn();
        final MyButton newInputBtn = getNewInputDevBtn();
        final MyButton newGraphicsBtn = getNewGraphicsBtn();
        final MyButton newSoundBtn = getNewSoundBtn();
        final MyButton newSerialBtn = getNewSerialBtn();
        final MyButton newParallelBtn = getNewParallelBtn();
        final MyButton newVideoBtn = getNewVideoBtn();
        /* new video button */
        mainPanel.add(getTablePanel("Disks", DISK_TABLE, newDiskBtn));
        mainPanel.add(getTablePanel("Filesystems", FILESYSTEM_TABLE, newFilesystemBtn));

        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Interfaces", INTERFACES_TABLE, newInterfaceBtn));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Input Devices", INPUTDEVS_TABLE, newInputBtn));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Graphics Devices", GRAPHICS_TABLE, newGraphicsBtn));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Sound Devices", SOUND_TABLE, newSoundBtn));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Serial Devices", SERIAL_TABLE, newSerialBtn));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Parallel Devices", PARALLEL_TABLE, newParallelBtn));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Video Devices", VIDEO_TABLE, newVideoBtn));
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.PAGE_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(application.getServiceLabelWidth()
                                         + application.getServiceFieldWidth() * 2 + 4));
        newPanel.add(new JScrollPane(mainPanel));
        swingUtils.invokeLater(() -> setApplyButtons(null, params));
        infoPanel = newPanel;
        infoPanelDone();
        return infoPanel;
    }

    /** Starts the domain. */
    void start(final Host host) {
        final boolean ret = VIRSH.start(host, getDomainName(), getVirshOptions());
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
                getBrowser().periodicalVmsUpdate(host);
                updateParameters();
                getBrowser().getVmsInfo().updateTable(VMListInfo.MAIN_TABLE);
                Tools.sleep(1000);
                i++;
                mTransitionReadLock.lock();
                try {
                    if (starting.isEmpty() || i >= ACTION_TIMEOUT) {
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
            getBrowser().periodicalVmsUpdate(host);
            updateParameters();
            getBrowser().getVmsInfo().updateTable(VMListInfo.MAIN_TABLE);
        }
    }

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
            getBrowser().periodicalVmsUpdate(host);
            updateParameters();
            getBrowser().getVmsInfo().updateTable(VMListInfo.MAIN_TABLE);
            Tools.sleep(1000);
            i++;
            mTransitionReadLock.lock();
            try {
                if (shuttingdown.isEmpty() || i >= ACTION_TIMEOUT) {
                    break;
                }
            } finally {
                mTransitionReadLock.unlock();
            }
        }
        if (i >= ACTION_TIMEOUT) {
            LOG.appWarning("startShuttingdownIndicator: could not shut down on " + host.getName());
            mTransitionWriteLock.lock();
            try {
                shuttingdown.clear();
            } finally {
                mTransitionWriteLock.unlock();
            }
        }
        getBrowser().periodicalVmsUpdate(host);
        updateParameters();
        getBrowser().getVmsInfo().updateTable(VMListInfo.MAIN_TABLE);
    }

    /** Shuts down the domain. */
    void shutdown(final Host host) {
        final boolean ret = VIRSH.shutdown(host, getDomainName(), getVirshOptions());
        if (ret) {
            startShuttingdownIndicator(host);
        }
    }

    /** Destroys down the domain. */
    void destroy(final Host host) {
        final boolean ret = VIRSH.destroy(host, getDomainName(), getVirshOptions());
        if (ret) {
            startShuttingdownIndicator(host);
        }
    }

    /** Reboots the domain. */
    void reboot(final Host host) {
        final boolean ret = VIRSH.reboot(host, getDomainName(), getVirshOptions());
        if (ret) {
            startShuttingdownIndicator(host);
        }
    }

    /** Suspend down the domain. */
    void suspend(final Host host) {
        final boolean ret = VIRSH.suspend(host, getDomainName(), getVirshOptions());
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
                getBrowser().periodicalVmsUpdate(host);
                updateParameters();
                getBrowser().getVmsInfo().updateTable(VMListInfo.MAIN_TABLE);
                Tools.sleep(1000);
                i++;
                mTransitionReadLock.lock();
                try {
                    if (suspending.isEmpty() || i >= ACTION_TIMEOUT) {
                        break;
                    }
                } finally {
                    mTransitionReadLock.unlock();
                }
            }
            if (i >= ACTION_TIMEOUT) {
                LOG.appWarning("suspend: could not suspend on " + host.getName());
                mTransitionWriteLock.lock();
                try {
                    suspending.clear();
                } finally {
                    mTransitionWriteLock.unlock();
                }
            }
            getBrowser().periodicalVmsUpdate(host);
            updateParameters();
            getBrowser().getVmsInfo().updateTable(VMListInfo.MAIN_TABLE);
        }
    }

    /** Resume down the domain. */
    void resume(final Host host) {
        final boolean ret = VIRSH.resume(host, getDomainName(), getVirshOptions());
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
                getBrowser().periodicalVmsUpdate(host);
                updateParameters();
                getBrowser().getVmsInfo().updateTable(VMListInfo.MAIN_TABLE);
                Tools.sleep(1000);
                i++;
                mTransitionReadLock.lock();
                try {
                    if (resuming.isEmpty() || i >= ACTION_TIMEOUT) {
                        break;
                    }
                } finally {
                    mTransitionReadLock.unlock();
                }
            }
            if (i >= ACTION_TIMEOUT) {
                LOG.appWarning("resume: could not resume on " + host.getName());
                mTransitionWriteLock.lock();
                try {
                    resuming.clear();
                } finally {
                    mTransitionWriteLock.unlock();
                }
            }
            getBrowser().periodicalVmsUpdate(host);
            updateParameters();
            getBrowser().getVmsInfo().updateTable(VMListInfo.MAIN_TABLE);
        }
    }

    public DiskInfo addDiskPanel() {
        final DiskInfo diskInfo = diskInfoProvider.get();
        diskInfo.init(null, getBrowser(), this);
        diskInfo.getResource().setNew(true);
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return diskInfo;
        }
        int i = 0;
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (info instanceof DiskInfo) {
                i++;
                continue;
            }
            break;
        }
        clusterTreeMenu.createMenuItem(thisNode, diskInfo, i);
        clusterTreeMenu.reloadNode(thisNode);
        diskInfo.selectMyself();
        return diskInfo;
    }

    public FilesystemInfo addFilesystemPanel() {
        final FilesystemInfo filesystemInfo = filesystemInfoProvider.get();
        filesystemInfo.init(null, getBrowser(), this);
        filesystemInfo.getResource().setNew(true);
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return filesystemInfo;
        }
        int i = 0;
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (info instanceof FilesystemInfo) {
                i++;
                continue;
            }
            break;
        }
        clusterTreeMenu.createMenuItem(thisNode, filesystemInfo, i);
        clusterTreeMenu.reloadNode(thisNode);
        filesystemInfo.selectMyself();
        return filesystemInfo;
    }

    /** Adds new virtual interface. */
    public InterfaceInfo addInterfacePanel() {
        final InterfaceInfo interfaceInfo = interfaceInfoProvider.get();
        interfaceInfo.init(null, getBrowser(), this);
        interfaceInfo.getResource().setNew(true);
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return interfaceInfo;
        }
        int i = 0;
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (info instanceof DiskInfo
                || info instanceof FilesystemInfo
                || info instanceof InterfaceInfo) {
                i++;
                continue;
            }
            break;
        }

        clusterTreeMenu.createMenuItem(thisNode, interfaceInfo, i);
        clusterTreeMenu.reloadNode(thisNode);
        interfaceInfo.selectMyself();
        return interfaceInfo;
    }

    /** Adds new virtual input device. */
    void addInputDevPanel() {
        final InputDevInfo inputDevInfo = inputDevInfoProvider.get();
        inputDevInfo.init(null, getBrowser(), this);
        inputDevInfo.getResource().setNew(true);
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return;
        }
        int i = 0;
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (info instanceof DiskInfo
                || info instanceof FilesystemInfo
                || info instanceof InterfaceInfo
                || info instanceof InputDevInfo) {
                i++;
                continue;
            }
            break;
        }

        clusterTreeMenu.createMenuItem(thisNode, inputDevInfo, i);
        clusterTreeMenu.reloadNode(thisNode);
        inputDevInfo.selectMyself();
    }

    /** Adds new graphics device. */
    public GraphicsInfo addGraphicsPanel() {
        final GraphicsInfo graphicsInfo = graphicsInfoProvider.get();
        graphicsInfo.init(null, getBrowser(), this);
        graphicsInfo.getResource().setNew(true);
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return graphicsInfo;
        }
        int i = 0;
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (info instanceof DiskInfo
                || info instanceof FilesystemInfo
                || info instanceof InterfaceInfo
                || info instanceof InputDevInfo
                || info instanceof GraphicsInfo) {
                i++;
                continue;
            }
            break;
        }
        clusterTreeMenu.createMenuItem(thisNode, graphicsInfo, i);
        clusterTreeMenu.reloadNode(thisNode);
        graphicsInfo.selectMyself();
        return graphicsInfo;
    }

    void addSoundsPanel() {
        final SoundInfo soundInfo = soundInfoProvider.get();
        soundInfo.init(null, getBrowser(), this);
        soundInfo.getResource().setNew(true);
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return;
        }
        int i = 0;
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (info instanceof DiskInfo
                || info instanceof FilesystemInfo
                || info instanceof InterfaceInfo
                || info instanceof InputDevInfo
                || info instanceof GraphicsInfo
                || info instanceof SoundInfo) {
                i++;
                continue;
            }
            break;
        }

        clusterTreeMenu.createMenuItem(thisNode, soundInfo, i);
        clusterTreeMenu.reloadNode(thisNode);
        soundInfo.selectMyself();
    }

    void addSerialsPanel() {
        final SerialInfo serialInfo = serialInfoProvider.get();
        serialInfo.init(null, getBrowser(), this);
        serialInfo.getResource().setNew(true);
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return;
        }
        int i = 0;
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (info instanceof DiskInfo
                || info instanceof FilesystemInfo
                || info instanceof InterfaceInfo
                || info instanceof InputDevInfo
                || info instanceof GraphicsInfo
                || info instanceof SoundInfo
                || info instanceof SerialInfo) {
                i++;
                continue;
            }
            break;
        }

        clusterTreeMenu.createMenuItem(thisNode, serialInfo, i);
        clusterTreeMenu.reloadNode(thisNode);
        serialInfo.selectMyself();
    }

    /** Adds new parallel device. */
    void addParallelsPanel() {
        final ParallelInfo parallelInfo = parallelInfoProvider.get();
        parallelInfo.init(null, getBrowser(), this);
        parallelInfo.getResource().setNew(true);
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return;
        }
        int i = 0;
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            if (info instanceof DiskInfo
                || info instanceof FilesystemInfo
                || info instanceof InterfaceInfo
                || info instanceof InputDevInfo
                || info instanceof GraphicsInfo
                || info instanceof SoundInfo
                || info instanceof SerialInfo
                || info instanceof ParallelInfo) {
                i++;
                continue;
            }
            break;
        }

        clusterTreeMenu.createMenuItem(thisNode, parallelInfo, i);
        clusterTreeMenu.reloadNode(thisNode);
        parallelInfo.selectMyself();
    }

    void addVideosPanel() {
        final VideoInfo videoInfo = videoInfoProvider.get();
        videoInfo.init(null, getBrowser(), this);
        videoInfo.getResource().setNew(true);
        final DefaultMutableTreeNode thisNode = getNode();
        clusterTreeMenu.createMenuItem(thisNode, videoInfo);
        clusterTreeMenu.reloadNode(thisNode);
        videoInfo.selectMyself();
    }

    /** Returns list of menu items for VM. */
    @Override
    public List<UpdatableItem> createPopup() {
        return domainMenu.getPulldownMenu(this);
    }

    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        for (final Host h : getBrowser().getClusterHosts()) {
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            if (vmsXml != null && vmsXml.isRunning(getDomainName())) {
                return HostBrowser.HOST_ON_ICON;
            }
        }
        return HostBrowser.HOST_OFF_ICON;
    }

    @Override
    public ImageIcon getCategoryIcon(final Application.RunMode runMode) {
        return getMenuIcon(runMode);
    }

    @Override
    protected String getParamLongDesc(final String param) {
        return SHORTNAME_MAP.get(param);
    }

    @Override
    protected String getParamShortDesc(final String param) {
        return SHORTNAME_MAP.get(param);
    }

    @Override
    protected Value getParamPreferred(final String param) {
        if (preferredEmulator != null && VMParams.VM_PARAM_EMULATOR.equals(param)) {
            return new StringValue(preferredEmulator);
        }
        return PREFERRED_MAP.get(param);
    }

    @Override
    protected Value getParamDefault(final String param) {
        return DEFAULTS_MAP.get(param);
    }

    /** Returns true if the value of the parameter is ok. */
    @Override
    protected boolean checkParam(final String param, final Value newValue) {
        if (isRequired(param) && (newValue == null || newValue.isNothingSelected())) {
            return false;
        }
        if (VMParams.VM_PARAM_MEMORY.equals(param)) {
            final long mem = VmsXml.convertToKilobytes(newValue);
            if (mem < 4096) {
                return false;
            }
            final long curMem = VmsXml.convertToKilobytes(getComboBoxValue(VMParams.VM_PARAM_CURRENTMEMORY));
            return mem >= curMem;
        } else if (VMParams.VM_PARAM_CURRENTMEMORY.equals(param)) {
            final long curMem = VmsXml.convertToKilobytes(newValue);
            if (curMem < 4096) {
                return false;
            }
            final long mem = VmsXml.convertToKilobytes(getComboBoxValue(VMParams.VM_PARAM_MEMORY));
            if (mem < curMem) {
                getWidget(VMParams.VM_PARAM_MEMORY, null).setValue(newValue);
            }
        } else if (VMParams.VM_PARAM_DOMAIN_TYPE.equals(param)) {
            final Widget wi = getWidget(param, null);
            if (getResource().isNew()
                && !Tools.areEqual(prevType, newValue)) {
                String xenLibPath = "/usr/lib/xen";
                for (final Host host : getBrowser().getClusterHosts()) {
                    final String xlp = host.getHostParser().getXenLibPath();
                    if (xlp != null) {
                        xenLibPath = xlp;
                        break;
                    }
                }
                String lxcLibPath = "/usr/lib/libvirt";
                for (final Host host : getBrowser().getClusterHosts()) {
                    final String llp = host.getHostParser().getLxcLibPath();
                    if (llp != null) {
                        lxcLibPath = llp;
                        break;
                    }
                }
                final Widget emWi = getWidget(VMParams.VM_PARAM_EMULATOR, Widget.WIZARD_PREFIX);
                final Widget loWi = getWidget(VMParams.VM_PARAM_LOADER, Widget.WIZARD_PREFIX);
                final Widget voWi = getWidget(VMParams.VM_PARAM_VIRSH_OPTIONS, Widget.WIZARD_PREFIX);
                final Widget typeWi = getWidget(VMParams.VM_PARAM_TYPE, Widget.WIZARD_PREFIX);
                final Widget inWi = getWidget(VMParams.VM_PARAM_INIT, Widget.WIZARD_PREFIX);
                if (Tools.areEqual(DOMAIN_TYPE_XEN, newValue.getValueForConfig())) {
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
                } else if (Tools.areEqual(DOMAIN_TYPE_LXC, newValue.getValueForConfig())) {
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
                } else if (Tools.areEqual(DOMAIN_TYPE_VBOX, newValue.getValueForConfig())) {
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
                } else if (Tools.areEqual(DOMAIN_TYPE_OPENVZ, newValue.getValueForConfig())) {
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
                } else if (Tools.areEqual(DOMAIN_TYPE_UML, newValue.getValueForConfig())) {
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
                        voWi.setValue(VIRSH_OPTION_QEMU);
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
        if (VMParams.VM_PARAM_AUTOSTART.equals(param)) {
            return autostartPossibleValues;
        } else if (VMParams.VM_PARAM_VIRSH_OPTIONS.equals(param)) {
            return VIRSH_OPTIONS;
        } else if (VMParams.VM_PARAM_CPUMATCH_MODEL.equals(param)) {
            final Set<Value> models = new LinkedHashSet<>();
            models.add(new StringValue());
            for (final Host host : getBrowser().getClusterHosts()) {
                models.addAll(host.getHostParser().getCPUMapModels());
            }
            return models.toArray(new Value[0]);
        } else if (VMParams.VM_PARAM_CPUMATCH_VENDOR.equals(param)) {
            final Set<Value> vendors = new LinkedHashSet<>();
            vendors.add(new StringValue());
            for (final Host host : getBrowser().getClusterHosts()) {
                vendors.addAll(host.getHostParser().getCPUMapVendors());
            }
            return vendors.toArray(new Value[0]);
        }
        return POSSIBLE_VALUES.get(param);
    }

    @Override
    protected String getSection(final String param) {
        return SECTION_MAP.get(param);
    }

    @Override
    protected boolean isRequired(final String param) {
        return VMParams.VM_PARAM_NAME.equals(param);
    }

    @Override
    protected boolean isInteger(final String param) {
        return IS_INTEGER.contains(param);
    }

    @Override
    protected boolean isLabel(final String param) {
        return false;
    }

    @Override
    protected boolean isTimeType(final String param) {
        return false;
    }

    @Override
    protected boolean isCheckBox(final String param) {
        return false;
    }

    @Override
    protected String getParamType(final String param) {
        return "undef"; // TODO:
    }

    @Override
    protected Widget.Type getFieldType(final String param) {
        return FIELD_TYPES.get(param);
    }

    public void apply(final Application.RunMode runMode) {
        if (Application.isTest(runMode)) {
            return;
        }
        swingUtils.invokeAndWait(() -> {
            getApplyButton().setEnabled(false);
            getRevertButton().setEnabled(false);
            getInfoPanel();
        });
        waitForInfoPanel();
        final String[] params = getParametersFromXML();
        final Map<String, String> parameters = new HashMap<>();
        setName(getComboBoxValue(VMParams.VM_PARAM_NAME).getValueForConfig());
        for (final String param : getParametersFromXML()) {
            final Value value = getComboBoxValue(param);
            if (value == null) {
                parameters.put(param, "");
            } else if (value.getUnit() != null) {
                parameters.put(param, Long.toString(VmsXml.convertToKilobytes(value)));
            } else {
                final String valueForConfig = value.getValueForConfig();
                parameters.put(param, Objects.requireNonNullElse(valueForConfig, ""));
            }
            getResource().setValue(param, value);
        }
        final List<Host> definedOnHosts = new ArrayList<>();
        final Map<HardwareInfo, Map<String, String>> allModifiedHWP = getAllHWParameters(false);
        final Map<HardwareInfo, Map<String, String>> allHWP = getAllHWParameters(true);
        final Map<Node, VmsXml> domainNodesToSave = new HashMap<>();
        final String clusterName = getBrowser().getCluster().getName();
        getBrowser().vmStatusLock();
        progressIndicator.startProgressIndicator(clusterName, "VM view update");
        for (final Host host : getBrowser().getClusterHosts()) {
            final Widget hostWi = definedOnHostComboBoxHash.get(host.getName());
            swingUtils.invokeLater(() -> {
                final Widget wizardHostWi = definedOnHostComboBoxHash.get(WIZARD_HOST_PREFIX + host.getName());
                if (wizardHostWi != null) {
                    wizardHostWi.setEnabled(false);
                }
            });
            final Value value = definedOnHostComboBoxHash.get(host.getName()).getValue();
            final boolean needConsole = needConsole();
            if (DEFINED_ON_HOST_TRUE.equals(value)) {
                final Node domainNode;
                VmsXml vmsXml;
                if (getResource().isNew()) {
                    vmsXml = vmsXmlProvider.get();
                    vmsXml.init(host);
                    getBrowser().vmsXmlPut(host, vmsXml);
                    domainNode = vmsXml.createDomainXML(getUUID(), getDomainName(), parameters, needConsole);
                    for (final HardwareInfo hi : allHWP.keySet()) {
                        hi.modifyXML(vmsXml, domainNode, getDomainName(), allHWP.get(hi));
                        hi.getResource().setNew(false);
                    }
                    vmsXml.saveAndDefine(domainNode, getDomainName(), getVirshOptions());
                } else {
                    vmsXml = getBrowser().getVmsXml(host);
                    if (vmsXml == null) {
                        vmsXml = vmsXmlProvider.get();
                        vmsXml.init(host);
                        getBrowser().vmsXmlPut(host, vmsXml);
                    }
                    if (vmsXml.getDomainNames().contains(getDomainName())) {
                        domainNode = vmsXml.modifyDomainXML(getDomainName(), parameters);
                        if (domainNode != null) {
                            for (final HardwareInfo hi : allModifiedHWP.keySet()) {
                               if (hi.checkResourceFields(null, hi.getRealParametersFromXML(), true).isChanged()) {
                                    hi.modifyXML(vmsXml, domainNode, getDomainName(), allModifiedHWP.get(hi));
                                    hi.getResource().setNew(false);
                               }
                            }
                        }
                    } else {
                        /* new on this host */
                        domainNode = vmsXml.createDomainXML(getUUID(), getDomainName(), parameters, needConsole);
                        if (domainNode != null) {
                            for (final HardwareInfo hi : allHWP.keySet()) {
                                hi.modifyXML(vmsXml, domainNode, getDomainName(), allHWP.get(hi));
                                hi.getResource().setNew(false);
                            }
                        }
                    }
                }
                if (domainNode != null) {
                    domainNodesToSave.put(domainNode, vmsXml);
                }
                definedOnHosts.add(host);
            } else {
                final VmsXml vmsXml = getBrowser().getVmsXml(host);
                if (vmsXml != null && vmsXml.getDomainNames().contains(getDomainName())) {
                    VIRSH.undefine(host, getDomainName(), getVirshOptions());
                }
            }
        }
        for (final Node dn : domainNodesToSave.keySet()) {
            domainNodesToSave.get(dn).saveAndDefine(dn, getDomainName(), getVirshOptions());
        }
        for (final HardwareInfo hi : allHWP.keySet()) {
            hi.setApplyButtons(null, hi.getRealParametersFromXML());
        }
        if (getResource().isNew()) {
            final DefaultMutableTreeNode thisNode = getNode();
            if (thisNode != null) {
                for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
                    final HardwareInfo hardwareInfo = (HardwareInfo) info;
                    if (hardwareInfo != null) {
                        final MyButton applyButton = hardwareInfo.getApplyButton();
                        if (applyButton != null) {
                            applyButton.setVisible(true);
                        }
                    }
                }
            }
        }
        VIRSH.setParameters(definedOnHosts.toArray(new Host[0]), getDomainName(), parameters, getVirshOptions());
        getResource().setNew(false);
        if (Application.isLive(runMode)) {
            storeComboBoxValues(params);
        }
        getBrowser().periodicalVmsUpdate(getBrowser().getClusterHosts());
        updateParameters();
        progressIndicator.stopProgressIndicator(clusterName, "VM view update");
        getBrowser().vmStatusUnlock();
        swingUtils.invokeLater(() -> {
            for (final Host host : getBrowser().getClusterHosts()) {
                final Widget hostWi = definedOnHostComboBoxHash.get(host.getName());
                final Widget wizardHostWi = definedOnHostComboBoxHash.get(WIZARD_HOST_PREFIX + host.getName());
                if (wizardHostWi != null) {
                    wizardHostWi.setEnabled(true);
                }
            }
        });
    }

    protected Map<HardwareInfo, Map<String, String>> getAllHWParameters(final boolean allParams) {
        final Map<HardwareInfo, Map<String, String>> allParamaters = new TreeMap<>();
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return allParamaters;
        }
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            final HardwareInfo hardwareInfo = (HardwareInfo) info;
            allParamaters.put(hardwareInfo, hardwareInfo.getHWParameters(allParams));
        }
        return allParamaters;
    }

    @Override
    protected boolean hasUnitPrefix(final String param) {
        return HAS_UNIT_PREFIX.containsKey(param) && HAS_UNIT_PREFIX.get(param);
    }

    @Override
    protected final Unit[] getUnits(final String param) {
        return VmsXml.getUnits();
    }

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
        final List<Object[]> rows = new ArrayList<>();
        final String domainName = getDomainName();
        ImageIcon hostIcon = HostBrowser.HOST_OFF_ICON_LARGE;
        Color newColor = Browser.PANEL_BACKGROUND;
        for (final Host host : getBrowser().getClusterHosts()) {
            final VmsXml vxml = getBrowser().getVmsXml(host);
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
            final MyButton domainNameLabel = widgetFactory.createButton(domainName, hostIcon);
            domainNameLabel.setOpaque(true);
            final MyButton removeDomain = widgetFactory.createButton("Remove",
                                                       ClusterBrowser.REMOVE_ICON_SMALL,
                                                       "Remove " + domainName + " domain");
            application.makeMiniButton(removeDomain);
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
            final VmsXml vxml = getBrowser().getVmsXml(host);
            if (vxml != null) {
                disks = vxml.getDisks(getDomainName());
                break;
            }
        }
        return disks;
    }

    /** Get one row of the table. */
    protected Object[] getDiskDataRow(final String targetDev,
                                      final Map<String, DiskInfo> dkti,
                                      final Map<String, DiskData> disks,
                                      final boolean opaque) {
        if (disks == null) {
            return new Object[0];
        }
        final MyButton removeBtn = widgetFactory.createButton("Remove",
                                                ClusterBrowser.REMOVE_ICON_SMALL,
                                                "Remove " + targetDev);
        application.makeMiniButton(removeBtn);
        final DiskData diskData = disks.get(targetDev);
        if (diskData == null) {
            return new Object[]{targetDev, "unknown", removeBtn};
        }
        final StringBuilder target = new StringBuilder(10);
        target.append(diskData.getTargetBusType());
        target.append(" : /dev/");
        target.append(targetDev);
        if (dkti != null) {
            mDiskToInfoLock.lock();
            final DiskInfo vdi;
            try {
                vdi = diskToInfo.get(targetDev);
            } finally {
                mDiskToInfoLock.unlock();
            }
            dkti.put(target.toString(), vdi);
        }
        final MyButton targetDevLabel = widgetFactory.createButton(target.toString(), BlockDevInfo.HARDDISK_ICON_LARGE);
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
        return new Object[]{targetDevLabel, source.toString(), removeBtn};
    }

    /** Returns all hosts on which this domain is defined. */
    List<Host> getDefinedOnHosts() {
        final List<Host> definedOn = new ArrayList<>();
        for (final Host h : getBrowser().getClusterHosts()) {
            if (getResource().isNew()) {
                final Value value =
                        definedOnHostComboBoxHash.get(h.getName()).getValue();
                if (DEFINED_ON_HOST_TRUE.equals(value)) {
                    definedOn.add(h);
                }
            } else {
                final VmsXml vmsXml = getBrowser().getVmsXml(h);
                if (vmsXml != null && vmsXml.getDomainNames().contains(getDomainName())) {
                    definedOn.add(h);
                }
            }
        }
        return definedOn;
    }

    private Object[][] getDiskTableData() {
        final List<Object[]> rows = new ArrayList<>();
        final Map<String, DiskData> disks = getDisks();
        final Map<String, DiskInfo> dkti = new HashMap<>();
        if (disks != null && !disks.isEmpty()) {
            for (final String targetDev : disks.keySet()) {
                final Object[] row = getDiskDataRow(targetDev, dkti, disks, false);
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

    protected Map<String, FilesystemData> getFilesystems() {
        Map<String, FilesystemData> filesystems = null;
        for (final Host host : getDefinedOnHosts()) {
            final VmsXml vxml = getBrowser().getVmsXml(host);
            if (vxml != null) {
                filesystems = vxml.getFilesystems(getDomainName());
                break;
            }
        }
        return filesystems;
    }

    /** Get one row of the table. */
    protected Object[] getFilesystemDataRow(final String targetDev,
                                            final Map<String, FilesystemInfo> dkti,
                                            final Map<String, FilesystemData> filesystems,
                                            final boolean opaque) {
        if (filesystems == null) {
            return new Object[0];
        }
        final MyButton removeBtn = widgetFactory.createButton("Remove", ClusterBrowser.REMOVE_ICON_SMALL, "Remove " + targetDev);
        application.makeMiniButton(removeBtn);
        final FilesystemData filesystemData = filesystems.get(targetDev);
        if (filesystemData == null) {
            return new Object[]{targetDev, "unknown", removeBtn};
        }
        final StringBuilder target = new StringBuilder(10);
        target.append(filesystemData.getTargetDir());
        if (dkti != null) {
            mFilesystemToInfoLock.lock();
            FilesystemInfo vdi;
            try {
                vdi = filesystemToInfo.get(targetDev);
            } finally {
                mFilesystemToInfoLock.unlock();
            }
            dkti.put(target.toString(), vdi);
        }
        final MyButton targetDevLabel = widgetFactory.createButton(target.toString(), BlockDevInfo.HARDDISK_ICON_LARGE);
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
        return new Object[]{targetDevLabel, source.toString(), removeBtn};
    }

    /** Returns data for the fs table. */
    private Object[][] getFilesystemTableData() {
        final List<Object[]> rows = new ArrayList<>();
        final Map<String, FilesystemData> filesystems = getFilesystems();
        final Map<String, FilesystemInfo> dkti = new HashMap<>();
        if (filesystems != null && !filesystems.isEmpty()) {
            for (final String targetDev : filesystems.keySet()) {
                final Object[] row = getFilesystemDataRow(targetDev, dkti, filesystems, false);
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

    protected Object[] getInterfaceDataRow(final String mac,
                                           final Map<String, InterfaceInfo> iToInfo,
                                           final Map<String, InterfaceData> interfaces,
                                           final boolean opaque) {
        if (interfaces == null) {
            return new Object[0];
        }
        final MyButton removeBtn = widgetFactory.createButton("Remove",
                                                ClusterBrowser.REMOVE_ICON_SMALL,
                                                "Remove " + mac);
        application.makeMiniButton(removeBtn);
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
            final InterfaceInfo vii = interfaceToInfo.get(mac);
            iToInfo.put(interf.toString(), vii);
        }
        final MyButton iLabel = widgetFactory.createButton(interf.toString(), NetInfo.NET_INTERFACE_ICON_LARGE);
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

    protected Object[] getInputDevDataRow(final String index,
                                          final Map<String, InputDevInfo> iToInfo,
                                          final Map<String, InputDevData> inputDevs,
                                          final boolean opaque) {
        if (inputDevs == null) {
            return new Object[0];
        }
        final MyButton removeBtn = widgetFactory.createButton("Remove", ClusterBrowser.REMOVE_ICON_SMALL, "Remove " + index);
        application.makeMiniButton(removeBtn);
        final InputDevData inputDevData = inputDevs.get(index);
        if (inputDevData == null) {
            return new Object[]{index + ": unknown", "", removeBtn};
        }
        if (iToInfo != null) {
            final InputDevInfo vidi = inputDevToInfo.get(index);
            iToInfo.put(index, vidi);
        }
        final MyButton iLabel = widgetFactory.createButton(index, null);
        iLabel.setOpaque(opaque);
        return new Object[]{iLabel, removeBtn};
    }

    protected Object[] getGraphicsDataRow(final String index,
                                          final Map<String, GraphicsInfo> iToInfo,
                                          final Map<String, GraphicsData> graphicDisplays,
                                          final boolean opaque) {
        if (graphicDisplays == null) {
            return new Object[0];
        }
        final MyButton removeBtn = widgetFactory.createButton("Remove", ClusterBrowser.REMOVE_ICON_SMALL, "Remove " + index);
        application.makeMiniButton(removeBtn);
        final GraphicsData graphicsData = graphicDisplays.get(index);
        if (graphicsData == null) {
            return new Object[]{index + ": unknown", "", removeBtn};
        }
        if (iToInfo != null) {
            final GraphicsInfo vidi = graphicsToInfo.get(index);
            iToInfo.put(index, vidi);
        }
        final MyButton iLabel = widgetFactory.createButton(index, VNC_ICON);
        iLabel.setOpaque(opaque);
        return new Object[]{iLabel, removeBtn};
    }

    protected Object[] getSoundDataRow(final String index,
                                       final Map<String, SoundInfo> iToInfo,
                                       final Map<String, SoundData> sounds,
                                       final boolean opaque) {
        if (sounds == null) {
            return new Object[0];
        }
        final MyButton removeBtn = widgetFactory.createButton("Remove", ClusterBrowser.REMOVE_ICON_SMALL, "Remove " + index);
        application.makeMiniButton(removeBtn);
        final SoundData soundData = sounds.get(index);
        if (soundData == null) {
            return new Object[]{index + ": unknown", "", removeBtn};
        }
        final String model = soundData.getModel();
        if (iToInfo != null) {
            final SoundInfo vidi = soundToInfo.get(index);
            iToInfo.put(index, vidi);
        }
        final MyButton iLabel = widgetFactory.createButton(model, null);
        iLabel.setOpaque(opaque);
        return new Object[]{iLabel, removeBtn};
    }

    protected Object[] getSerialDataRow(final String index,
                                        final Map<String, SerialInfo> iToInfo,
                                        final Map<String, SerialData> serials,
                                        final boolean opaque) {
        if (serials == null) {
            return new Object[0];
        }
        final MyButton removeBtn = widgetFactory.createButton("Remove", ClusterBrowser.REMOVE_ICON_SMALL, "Remove " + index);
        application.makeMiniButton(removeBtn);
        final SerialData serialData = serials.get(index);
        if (serialData == null) {
            return new Object[]{index + ": unknown", "", removeBtn};
        }
        if (iToInfo != null) {
            final SerialInfo vidi = serialToInfo.get(index);
            iToInfo.put(index, vidi);
        }
        final MyButton iLabel = widgetFactory.createButton(index, null);
        iLabel.setOpaque(opaque);
        return new Object[]{iLabel, removeBtn};
    }

    protected Object[] getParallelDataRow(final String index,
                                          final Map<String, ParallelInfo> iToInfo,
                                          final Map<String, ParallelData> parallels,
                                          final boolean opaque) {
        if (parallels == null) {
            return new Object[0];
        }
        final MyButton removeBtn = widgetFactory.createButton("Remove", ClusterBrowser.REMOVE_ICON_SMALL, "Remove " + index);
        application.makeMiniButton(removeBtn);
        final ParallelData parallelData = parallels.get(index);
        if (parallelData == null) {
            return new Object[]{index + ": unknown", "", removeBtn};
        }
        if (iToInfo != null) {
            final ParallelInfo vidi = parallelToInfo.get(index);
            iToInfo.put(index, vidi);
        }
        final MyButton iLabel = widgetFactory.createButton(index, null);
        iLabel.setOpaque(opaque);
        return new Object[]{iLabel, removeBtn};
    }

    protected Object[] getVideoDataRow(final String index,
                                       final Map<String, VideoInfo> iToInfo,
                                       final Map<String, VideoData> videos,
                                       final boolean opaque) {
        if (videos == null) {
            return new Object[0];
        }
        final MyButton removeBtn = widgetFactory.createButton("Remove", ClusterBrowser.REMOVE_ICON_SMALL, "Remove " + index);
        application.makeMiniButton(removeBtn);
        final VideoData videoData = videos.get(index);
        if (videoData == null) {
            return new Object[]{index + ": unknown", "", removeBtn};
        }
        final String modelType = videoData.getModelType();
        if (iToInfo != null) {
            final VideoInfo vidi = videoToInfo.get(index);
            iToInfo.put(index, vidi);
        }
        final MyButton iLabel = widgetFactory.createButton(modelType, null);
        iLabel.setOpaque(opaque);
        return new Object[]{iLabel, removeBtn};
    }

    protected Map<String, InterfaceData> getInterfaces() {
        Map<String, InterfaceData> interfaces = null;
        for (final Host host : getDefinedOnHosts()) {
            final VmsXml vxml = getBrowser().getVmsXml(host);
            if (vxml != null) {
                interfaces = vxml.getInterfaces(getDomainName());
                break;
            }
        }
        return interfaces;
    }

    private Object[][] getInputDevTableData() {
        final List<Object[]> rows = new ArrayList<>();
        final Map<String, InputDevData> inputDevs = getInputDevs();
        final Map<String, InputDevInfo> iToInfo = new HashMap<>();
        if (inputDevs != null) {
            for (final String index : inputDevs.keySet()) {
                final Object[] row = getInputDevDataRow(index, iToInfo, inputDevs, false);
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

    private Object[][] getGraphicsTableData() {
        final List<Object[]> rows = new ArrayList<>();
        final Map<String, GraphicsData> graphicDisplays = getGraphicDisplays();
        final Map<String, GraphicsInfo> iToInfo = new HashMap<>();
        if (graphicDisplays != null) {
            for (final String index : graphicDisplays.keySet()) {
                final Object[] row = getGraphicsDataRow(index, iToInfo, graphicDisplays, false);
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
        final List<Object[]> rows = new ArrayList<>();
        final Map<String, SoundData> sounds = getSounds();
        final Map<String, SoundInfo> iToInfo = new HashMap<>();
        if (sounds != null) {
            for (final String index : sounds.keySet()) {
                final Object[] row = getSoundDataRow(index, iToInfo, sounds, false);
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
        final List<Object[]> rows = new ArrayList<>();
        final Map<String, SerialData> serials = getSerials();
        final Map<String, SerialInfo> iToInfo = new HashMap<>();
        if (serials != null) {
            for (final String index : serials.keySet()) {
                final Object[] row = getSerialDataRow(index, iToInfo, serials, false);
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
        final List<Object[]> rows = new ArrayList<>();
        final Map<String, ParallelData> parallels = getParallels();
        final Map<String, ParallelInfo> iToInfo = new HashMap<>();
        if (parallels != null) {
            for (final String index : parallels.keySet()) {
                final Object[] row = getParallelDataRow(index, iToInfo, parallels, false);
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

    private Object[][] getVideoTableData() {
        final List<Object[]> rows = new ArrayList<>();
        final Map<String, VideoData> videos = getVideos();
        final Map<String, VideoInfo> iToInfo = new HashMap<>();
        if (videos != null) {
            for (final String index : videos.keySet()) {
                final Object[] row = getVideoDataRow(index, iToInfo, videos, false);
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

    protected Map<String, InputDevData> getInputDevs() {
        Map<String, InputDevData> inputDevs = null;
        for (final Host host : getDefinedOnHosts()) {
            final VmsXml vxml = getBrowser().getVmsXml(host);
            if (vxml != null) {
                inputDevs = vxml.getInputDevs(getDomainName());
                break;
            }
        }
        return inputDevs;
    }

    protected Map<String, GraphicsData> getGraphicDisplays() {
        Map<String, GraphicsData> graphicDisplays = null;
        for (final Host host : getDefinedOnHosts()) {
            final VmsXml vxml = getBrowser().getVmsXml(host);
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
            final VmsXml vxml = getBrowser().getVmsXml(host);
            if (vxml != null) {
                sounds = vxml.getSounds(getDomainName());
                break;
            }
        }
        return sounds;
    }

    protected Map<String, SerialData> getSerials() {
        Map<String, SerialData> serials = null;
        for (final Host host : getDefinedOnHosts()) {
            final VmsXml vxml = getBrowser().getVmsXml(host);
            if (vxml != null) {
                serials = vxml.getSerials(getDomainName());
                break;
            }
        }
        return serials;
    }

    protected Map<String, ParallelData> getParallels() {
        Map<String, ParallelData> parallels = null;
        for (final Host host : getDefinedOnHosts()) {
            final VmsXml vxml = getBrowser().getVmsXml(host);
            if (vxml != null) {
                parallels = vxml.getParallels(getDomainName());
                break;
            }
        }
        return parallels;
    }

    protected Map<String, VideoData> getVideos() {
        Map<String, VideoData> videos = null;
        for (final Host host : getDefinedOnHosts()) {
            final VmsXml vxml = getBrowser().getVmsXml(host);
            if (vxml != null) {
                videos = vxml.getVideos(getDomainName());
                break;
            }
        }
        return videos;
    }

    private Object[][] getInterfaceTableData() {
        final List<Object[]> rows = new ArrayList<>();
        final Map<String, InterfaceData> interfaces = getInterfaces();
        final Map<String, InterfaceInfo> iToInfo = new HashMap<>();
        if (interfaces != null) {
            for (final String mac : interfaces.keySet()) {
                final Object[] row = getInterfaceDataRow(mac, iToInfo, interfaces, false);
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

    @Override
    protected void rowClicked(final String tableName, final String key, final int column) {
        if (HEADER_TABLE.equals(tableName)) {
            final Thread thread = new Thread(() -> {
                if (HEADER_DEFAULT_WIDTHS.containsKey(column)) {
                    removeMyself(Application.RunMode.LIVE);
                } else {
                    getBrowser().getVmsInfo().selectMyself();
                }
            });
            thread.start();
        } else if (DISK_TABLE.equals(tableName)) {
            mDiskToInfoLock.lock();
            final DiskInfo vdi;
            try {
                vdi = diskKeyToInfo.get(key);
            } finally {
                mDiskToInfoLock.unlock();
            }
            if (vdi != null) {
                final Thread thread = new Thread(() -> {
                    if (DISK_DEFAULT_WIDTHS.containsKey(column)) {
                        vdi.removeMyself(Application.RunMode.LIVE);
                    } else {
                        vdi.selectMyself();
                    }
                });
                thread.start();
            }
        } else if (FILESYSTEM_TABLE.equals(tableName)) {
            mFilesystemToInfoLock.lock();
            final FilesystemInfo vfi;
            try {
                vfi = filesystemKeyToInfo.get(key);
            } finally {
                mFilesystemToInfoLock.unlock();
            }
            if (vfi != null) {
                final Thread thread = new Thread(() -> {
                    if (FILESYSTEM_DEFAULT_WIDTHS.containsKey(column)) {
                        vfi.removeMyself(Application.RunMode.LIVE);
                    } else {
                        vfi.selectMyself();
                    }
                });
                thread.start();
            }
        } else if (INTERFACES_TABLE.equals(tableName)) {
            mInterfaceToInfoLock.lock();
            final InterfaceInfo vii;
            try {
                vii = interfaceKeyToInfo.get(key);
            } finally {
                mInterfaceToInfoLock.unlock();
            }
            if (vii != null) {
                final Thread thread = new Thread(() -> {
                    if (INTERFACES_DEFAULT_WIDTHS.containsKey(column)) {
                        vii.removeMyself(Application.RunMode.LIVE);
                    } else {
                        vii.selectMyself();
                    }
                });
                thread.start();
            }
        } else if (INPUTDEVS_TABLE.equals(tableName)) {
            mInputDevToInfoLock.lock();
            final InputDevInfo vidi;
            try {
                vidi = inputDevKeyToInfo.get(key);
            } finally {
                mInputDevToInfoLock.unlock();
            }
            if (vidi != null) {
                final Thread thread = new Thread(() -> {
                    if (INPUTDEVS_DEFAULT_WIDTHS.containsKey(column)) {
                        vidi.removeMyself(Application.RunMode.LIVE);
                    } else {
                        vidi.selectMyself();
                    }
                });
                thread.start();
            }
        } else if (GRAPHICS_TABLE.equals(tableName)) {
            mGraphicsToInfoLock.lock();
            final GraphicsInfo vgi;
            try {
                vgi = graphicsKeyToInfo.get(key);
            } finally {
                mGraphicsToInfoLock.unlock();
            }
            if (vgi != null) {
                final Thread thread = new Thread(() -> {
                    if (GRAPHICS_DEFAULT_WIDTHS.containsKey(column)) {
                        vgi.removeMyself(Application.RunMode.LIVE);
                    } else {
                        vgi.selectMyself();
                    }
                });
                thread.start();
            }
        } else if (SOUND_TABLE.equals(tableName)) {
            mSoundToInfoLock.lock();
            final SoundInfo vsi;
            try {
                vsi = soundKeyToInfo.get(key);
            } finally {
                mSoundToInfoLock.unlock();
            }
            if (vsi != null) {
                final Thread thread = new Thread(() -> {
                    if (SOUND_DEFAULT_WIDTHS.containsKey(column)) {
                        vsi.removeMyself(Application.RunMode.LIVE);
                    } else {
                        vsi.selectMyself();
                    }
                });
                thread.start();
            }
        } else if (SERIAL_TABLE.equals(tableName)) {
            mSerialToInfoLock.lock();
            final SerialInfo vsi;
            try {
                vsi = serialKeyToInfo.get(key);
            } finally {
                mSerialToInfoLock.unlock();
            }
            if (vsi != null) {
                final Thread thread = new Thread(() -> {
                    if (SERIAL_DEFAULT_WIDTHS.containsKey(column)) {
                        vsi.removeMyself(Application.RunMode.LIVE);
                    } else {
                        vsi.selectMyself();
                    }
                });
                thread.start();
            }
        } else if (PARALLEL_TABLE.equals(tableName)) {
            mParallelToInfoLock.lock();
            final ParallelInfo vpi;
            try {
                vpi = parallelKeyToInfo.get(key);
            } finally {
                mParallelToInfoLock.unlock();
            }
            if (vpi != null) {
                final Thread thread = new Thread(() -> {
                    if (PARALLEL_DEFAULT_WIDTHS.containsKey(column)) {
                        vpi.removeMyself(Application.RunMode.LIVE);
                    } else {
                        vpi.selectMyself();
                    }
                });
                thread.start();
            }
        } else if (VIDEO_TABLE.equals(tableName)) {
            mVideoToInfoLock.lock();
            final VideoInfo vvi;
            try {
                vvi = videoKeyToInfo.get(key);
            } finally {
                mVideoToInfoLock.unlock();
            }
            if (vvi != null) {
                final Thread thread = new Thread(() -> {
                    if (VIDEO_DEFAULT_WIDTHS.containsKey(column)) {
                        vvi.removeMyself(Application.RunMode.LIVE);
                    } else {
                        vvi.selectMyself();
                    }
                });
                thread.start();
            }
        }
    }

    @Override
    protected Color getTableRowColor(final String tableName, final String key) {
        if (HEADER_TABLE.equals(tableName)) {
            return rowColor;
        }
        return Browser.PANEL_BACKGROUND;
    }

    @Override
    protected int getTableColumnAlignment(final String tableName, final int column) {

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

    @Override
    protected boolean isAdvanced(final String param) {
        if (!getResource().isNew() && VMParams.VM_PARAM_NAME.equals(param)) {
            return true;
        }
        return IS_ADVANCED.contains(param);
    }

    @Override
    protected String isEnabled(final String param) {
        final String libvirtVersion = getBrowser().getCluster().getMinLibvirtVersion();
        if (!getResource().isNew() && VMParams.VM_PARAM_NAME.equals(param)) {
            return "";
        }
        if (REQUIRED_VERSION.containsKey(param)) {
            final String rv = REQUIRED_VERSION.get(param);
            try {
                if (Tools.compareVersions(rv, libvirtVersion) > 0) {
                    return Tools.getString("DomainInfo.AvailableInVersion").replace("@VERSION@", rv);
                }
            } catch (final Exceptions.IllegalVersionException e) {
                LOG.appWarning("isEnabled: " + e.getMessage(), e);
                return Tools.getString("DomainInfo.AvailableInVersion").replace("@VERSION@", rv);
            }
        }
        return null;
    }

    @Override
    protected AccessMode.Mode isEnabledOnlyInAdvancedMode(final String param) {
         return VMParams.VM_PARAM_MEMORY.equals(param) ? AccessMode.ADVANCED : AccessMode.NORMAL;
    }


    @Override
    protected AccessMode.Type getAccessType(final String param) {
        return AccessMode.ADMIN;
    }

    @Override
    protected String getParamRegexp(final String param) {
        if (VMParams.VM_PARAM_NAME.equals(param)) {
            return "^[\\w-]+$";
        } else {
            return super.getParamRegexp(param);
        }
    }

    /**
     * Returns whether the specified parameter or any of the parameters have changed.
     */
    @Override
    public Check checkResourceFields(final String param, final String[] params) {
        final DefaultMutableTreeNode thisNode = getNode();
        final List<String> changed = new ArrayList<>();
        final List<String> incorrect = new ArrayList<>();
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
            final Widget wizardHostWi = definedOnHostComboBoxHash.get(WIZARD_HOST_PREFIX + host.getName());
            final Value value = hostWi.getValue();
            final VmsXml vmsXml = getBrowser().getVmsXml(host);
            final Value savedValue;
            if (vmsXml != null && vmsXml.getDomainNames().contains(getDomainName())) {
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

            if ((vmsXml == null
                 || (!getResource().isNew() && !vmsXml.getDomainNames().contains(getDomainName())))
                && DEFINED_ON_HOST_TRUE.equals(value)) {
                changed.add("host");
            } else if (vmsXml != null
                       && vmsXml.getDomainNames().contains(getDomainName())
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
        final Check check = new Check(incorrect, changed);
        check.addCheck(super.checkResourceFields(param, params));
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            final HardwareInfo hardwareInfo = (HardwareInfo) info;
            check.addCheck(hardwareInfo.checkResourceFields(null, hardwareInfo.getRealParametersFromXML(), true));
        }
        return check;
    }

    @Override
    protected Widget createWidget(final String param, final String prefix, final int width) {
        final Widget paramWi = super.createWidget(param, prefix, width);
        if (VMParams.VM_PARAM_BOOT.equals(param) || VMParams.VM_PARAM_BOOT_2.equals(param)) {
            paramWi.setAlwaysEditable(false);
        }
        return paramWi;
    }

    /** Removes this domain. */
    @Override
    public void removeMyself(final Application.RunMode runMode) {
        if (getResource().isNew()) {
            super.removeMyself(runMode);
            getResource().setNew(false);
            clusterTreeMenu.removeNode(getNode());
            return;
        }
        String desc = Tools.getString("DomainInfo.confirmRemove.Description");

        String dn = getDomainName();
        if (dn == null) {
            dn = "";
        }
        desc  = desc.replaceAll("@DOMAIN@", Matcher.quoteReplacement(dn));
        if (application.confirmDialog(Tools.getString("DomainInfo.confirmRemove.Title"),
                                desc,
                                Tools.getString("DomainInfo.confirmRemove.Yes"),
                                Tools.getString("DomainInfo.confirmRemove.No"))) {
            removeMyselfNoConfirm(runMode);
            getResource().setNew(false);
        }
    }

    /** Removes this virtual domain without confirmation dialog. */
    protected void removeMyselfNoConfirm(final Application.RunMode runMode) {
        for (final Host h : getBrowser().getClusterHosts()) {
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            if (vmsXml != null && vmsXml.getDomainNames().contains(getDomainName())) {
                VIRSH.undefine(h, getDomainName(), getVirshOptions());
            }
        }
        getBrowser().periodicalVmsUpdate(getBrowser().getClusterHosts());
        clusterTreeMenu.removeNode(getNode());
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
    protected boolean isControlButton(final String tableName, final int column) {
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

    /**
     * Sets button next to host to the start button.
     */
    private void setButtonToStart(final Host host, final Widget hostWi, final MyButton hostBtn, final boolean stopped) {
        if (hostWi != null) {
            final boolean enable = host.isConnected();
            swingUtils.invokeLater(() -> {
                hostWi.setTFButtonEnabled(enable && stopped);
                hostBtn.setText("Start");
                hostBtn.setIcon(HostBrowser.HOST_ON_ICON);
                hostBtn.setToolTipText("Start on " + host.getName());
            });
        }
    }

    /**
     * Sets button next to host to the view button.
     */
    private void setButtonToView(final Host host, final Widget hostWi, final MyButton hostBtn) {
        if (hostWi != null) {
            final boolean enable = host.isConnected();
            swingUtils.invokeLater(() -> {
                hostWi.setTFButtonEnabled(enable);
                hostBtn.setText("View");
                hostBtn.setIcon(VNC_ICON);
                hostBtn.setToolTipText("Graphical console on " + host.getName());
            });
        }
    }

    /**
     * Sets button next to host to the not defined button.
     */
    private void setButtonToNotDefined(final Host host, final Widget hostWi, final MyButton hostBtn) {
        if (hostWi != null) {
            swingUtils.invokeLater(() -> {
                hostWi.setTFButtonEnabled(false);
                hostBtn.setIcon(null);
                hostBtn.setToolTipText("not defined on " + host.getName());
            });
        }
    }

    /** Sets all host buttons. */
    private void setHostButtons(final boolean running) {
        for (final Host h : getBrowser().getClusterHosts()) {
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            final MyButton hostBtn = hostButtons.get(h.getName());
            final MyButton wizardHostBtn = hostButtons.get(WIZARD_HOST_PREFIX + h.getName());
            final Widget hostWi = definedOnHostComboBoxHash.get(h.getName());
            final Widget wizardHostWi = definedOnHostComboBoxHash.get(WIZARD_HOST_PREFIX + h.getName());
            if (vmsXml != null
                && vmsXml.getDomainNames().contains(getDomainName())) {
                if (vmsXml.isRunning(getDomainName())) {
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

    /** Revert values. */
    @Override
    public void revert() {
        final DefaultMutableTreeNode thisNode = getNode();
        if (thisNode == null) {
            return;
        }
        for (final Host h : getBrowser().getClusterHosts()) {
            final Widget hostWi = definedOnHostComboBoxHash.get(h.getName());
            final Value savedValue;
            final VmsXml vmsXml = getBrowser().getVmsXml(h);
            if (getResource().isNew()
                || (vmsXml != null && vmsXml.getDomainNames().contains(getDomainName()))) {
                savedValue = DEFINED_ON_HOST_TRUE;
            } else {
                savedValue = DEFINED_ON_HOST_FALSE;
            }
            hostWi.setValue(savedValue);
        }
        for (final Object info : clusterTreeMenu.nodesToInfos(thisNode.children())) {
            final HardwareInfo hardwareInfo = (HardwareInfo) info;
            if (hardwareInfo.checkResourceFields(null, hardwareInfo.getRealParametersFromXML(), true).isChanged()) {
                hardwareInfo.revert();
            }
        }
        super.revert();
    }

    /** Saves all preferred values. */
    public void savePreferredValues() {
        for (final String pv : PREFERRED_MAP.keySet()) {
            if (preferredEmulator != null && VMParams.VM_PARAM_EMULATOR.equals(pv)) {
                getResource().setValue(pv, new StringValue(preferredEmulator));
            } else {
                getResource().setValue(pv, PREFERRED_MAP.get(pv));
            }
        }
    }

    boolean isUsedByCRM() {
        return usedByCRM;
    }

    public void setUsedByCRM(final boolean usedByCRM) {
        this.usedByCRM = usedByCRM;
    }

    public String getUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        return uuid;
    }

    /**
     * Return virsh options like -c xen:///.
     */
    public String getVirshOptions() {
        final Value v = getResource().getValue(VMParams.VM_PARAM_VIRSH_OPTIONS);
        if (v == null) {
            return "";
        }
        return v.getValueForConfig();
    }

    /**
     * Return whether domain type needs "display" section.
     */
    public boolean needDisplay() {
        return NEED_DISPLAY.contains(getWidget(VMParams.VM_PARAM_DOMAIN_TYPE, null).getStringValue());
    }

    /**
     * Return whether domain type needs "console" section.
     */
    public boolean needConsole() {
        return NEED_CONSOLE.contains(getWidget(VMParams.VM_PARAM_DOMAIN_TYPE, null).getStringValue());
    }

    /**
     * Return whether domain type needs filesystem instead of disk device.
     */
    public boolean needFilesystem() {
        return NEED_FILESYSTEM.contains(getWidget(VMParams.VM_PARAM_DOMAIN_TYPE, null).getStringValue());
    }

    MyButton getNewDiskBtn() {
        final MyButton newBtn = widgetFactory.createButton("Add Disk");
        newBtn.addActionListener(e -> {
            final Thread t = new Thread(this::addDiskPanel);
            t.start();
        });
        return newBtn;
    }

    MyButton getNewFilesystemBtn() {
        final MyButton newBtn = widgetFactory.createButton("Add Filesystem");
        newBtn.addActionListener(e -> {
            final Thread t = new Thread(this::addFilesystemPanel);
            t.start();
        });
        return newBtn;
    }

    MyButton getNewInterfaceBtn() {
        final MyButton newBtn = widgetFactory.createButton("Add Interface");
        newBtn.addActionListener(e -> {
            final Thread t = new Thread(this::addInterfacePanel);
            t.start();
        });
        return newBtn;
    }

    MyButton getNewGraphicsBtn() {
        final MyButton newBtn = widgetFactory.createButton("Add Graphics Display");
        newBtn.addActionListener(e -> {
            final Thread t = new Thread(this::addGraphicsPanel);
            t.start();
        });
        return newBtn;
    }

    MyButton getNewSoundBtn() {
        final MyButton newBtn = widgetFactory.createButton("Add Sound Device");
        newBtn.addActionListener(e -> {
            final Thread t = new Thread(this::addSoundsPanel);
            t.start();
        });
        return newBtn;
    }

    MyButton getNewSerialBtn() {
        final MyButton newBtn = widgetFactory.createButton("Add Serial Device");
        newBtn.addActionListener(e -> {
            final Thread t = new Thread(this::addSerialsPanel);
            t.start();
        });
        return newBtn;
    }

    MyButton getNewParallelBtn() {
        final MyButton newBtn = widgetFactory.createButton("Add Parallel Device");
        newBtn.addActionListener(e -> {
            final Thread t = new Thread(this::addParallelsPanel);
            t.start();
        });
        return newBtn;
    }

    MyButton getNewVideoBtn() {
        final MyButton newBtn = widgetFactory.createButton("Add Video Device");
        newBtn.addActionListener(e -> {
            final Thread t = new Thread(this::addVideosPanel);
            t.start();
        });
        return newBtn;
    }

    MyButton getNewInputDevBtn() {
        final MyButton newBtn = widgetFactory.createButton("Add Input Device");
        newBtn.addActionListener(e -> {
            final Thread t = new Thread(this::addInputDevPanel);
            t.start();
        });
        return newBtn;
    }
}
