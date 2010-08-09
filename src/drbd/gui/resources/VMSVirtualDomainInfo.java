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
import drbd.data.VMSXML;
import drbd.data.VMSXML.DiskData;
import drbd.data.VMSXML.InterfaceData;
import drbd.data.Host;
import drbd.data.resources.Resource;
import drbd.data.ConfigData;
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
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;

import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * This class holds info about VirtualDomain service in the VMs category,
 * but not in the cluster view.
 */
public class VMSVirtualDomainInfo extends EditableInfo {
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
    /** Map from target string in the table to vms interface info object.
     */
    private volatile Map<String, VMSInterfaceInfo> interfaceKeyToInfo =
                                       new HashMap<String, VMSInterfaceInfo>();
    /** Timeout of starting, shutting down, etc. actions in seconds. */
    private static final int ACTION_TIMEOUT = 20;
    /** All parameters. */
    private static final String[] VM_PARAMETERS = new String[]{
                                               VMSXML.VM_PARAM_VCPU,
                                               VMSXML.VM_PARAM_CURRENTMEMORY,
                                               VMSXML.VM_PARAM_MEMORY,
                                               VMSXML.VM_PARAM_BOOT,
                                               VMSXML.VM_PARAM_AUTOSTART};
    /** Map of sections to which every param belongs. */
    private static final Map<String, String> SECTION_MAP =
                                                 new HashMap<String, String>();
    /** Map of short param names with uppercased first character. */
    private static final Map<String, String> SHORTNAME_MAP =
                                                 new HashMap<String, String>();
    /** Map of default values. */
    private static final Map<String, String> DEFAULTS_MAP =
                                                 new HashMap<String, String>();
    /** Types of some of the field. */
    private static final Map<String, GuiComboBox.Type> FIELD_TYPES =
                                       new HashMap<String, GuiComboBox.Type>();
    /** Possible values for some fields. */
    private static final Map<String, Object[]> POSSIBLE_VALUES =
                                          new HashMap<String, Object[]>();
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
    private static final ImageIcon VNC_ICON = Tools.createImageIcon(
                                    Tools.getDefault("VMS.VNC.IconLarge"));
    /** Pause / Suspend icon. */
    public static final ImageIcon PAUSE_ICON = Tools.createImageIcon(
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
    protected static final String HEADER_TABLE = "header";
    /** Disk table. */
    protected static final String DISK_TABLE = "disks";
    /** Interface table. */
    protected static final String INTERFACES_TABLE = "interfaces";
    /** Virtual System header. */
    private static final String VIRTUAL_SYSTEM_STRING =
                Tools.getString("VMSVirtualDomainInfo.Section.VirtualSystem");

    static {
        SECTION_MAP.put(VMSXML.VM_PARAM_VCPU,          VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_CURRENTMEMORY, VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_MEMORY,        VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_BOOT,          VIRTUAL_SYSTEM_STRING);
        SECTION_MAP.put(VMSXML.VM_PARAM_AUTOSTART,     VIRTUAL_SYSTEM_STRING);

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
                   VMSXML.VM_PARAM_AUTOSTART,
                   Tools.getString("VMSVirtualDomainInfo.Short.Autostart"));

        FIELD_TYPES.put(VMSXML.VM_PARAM_CURRENTMEMORY,
                        GuiComboBox.Type.TEXTFIELDWITHUNIT);
        FIELD_TYPES.put(VMSXML.VM_PARAM_MEMORY,
                        GuiComboBox.Type.TEXTFIELDWITHUNIT);
        FIELD_TYPES.put(VMSXML.VM_PARAM_AUTOSTART, GuiComboBox.Type.CHECKBOX);
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_AUTOSTART, "False");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_BOOT,   "hd");
        DEFAULTS_MAP.put(VMSXML.VM_PARAM_VCPU,      "1");
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
                                                     null)});
        POSSIBLE_VALUES.put(VMSXML.VM_PARAM_AUTOSTART,
                            new String[]{"True", "False"});
    }
    /**
     * Creates the VMSVirtualDomainInfo object.
     */
    public VMSVirtualDomainInfo(final String name,
                                final Browser browser) {
        super(name, browser);
        setResource(new Resource(name));
    }

    /**
     * Returns browser object of this info.
     */
    protected final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /**
     * Returns a name of the service with virtual domain name.
     */
    public final String toString() {
        return getName();
    }

    /**
     * Returns domain name.
     */
    protected final String getDomainName() {
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
            node.removeFromParent();
        }

        final Enumeration eee = getNode().children();
        int i = 0;
        for (final String disk : diskNames) {
            while (eee.hasMoreElements()) {
                final DefaultMutableTreeNode node =
                                (DefaultMutableTreeNode) eee.nextElement();
                if (!(node.getUserObject() instanceof VMSDiskInfo)) {
                    continue;
                }
                final VMSDiskInfo vmsdi =
                                        (VMSDiskInfo) node.getUserObject();
                if (disk.compareTo(vmsdi.getName()) < 0) {
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
            i++;
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
            node.removeFromParent();
        }

        final Enumeration eee = getNode().children();
        int i = 0;
        for (final String interf : interfaceNames) {
            VMSInterfaceInfo vmsii;
            if (emptySlot == null) {
                while (eee.hasMoreElements()) {
                    final DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) eee.nextElement();
                    if (!(node.getUserObject() instanceof VMSInterfaceInfo)) {
                        i++;
                        continue;
                    }
                    final VMSInterfaceInfo v =
                                       (VMSInterfaceInfo) node.getUserObject();
                    if (interf.compareTo(v.getName()) < 0) {
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
                i++;
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

    /**
     * Sets service parameters with values from resourceNode hash.
     */
    public final void updateParameters() {
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
        running = runningOnHosts.isEmpty();
        try {
            mTransitionLock.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
        }
        for (final String param : getParametersFromXML()) {
            final String oldValue = getParamSaved(param);
            String value = getParamSaved(param);
            final GuiComboBox cb = paramComboBoxGet(param, null);
            if (cb != null) {
                if (VMSXML.VM_PARAM_VCPU.equals(param)) {
                    paramComboBoxGet(param, null).setEnabled(!running);
                } else if (VMSXML.VM_PARAM_CURRENTMEMORY.equals(param)) {
                    paramComboBoxGet(param, null).setEnabled(false);
                }
            }
            for (final Host h : getBrowser().getClusterHosts()) {
                final VMSXML vmsxml = getBrowser().getVMSXML(h);
                if (vmsxml != null) {
                    final String savedValue =
                                       vmsxml.getValue(getDomainName(), param);
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
        updateTable(HEADER_TABLE);
        updateTable(DISK_TABLE);
        updateTable(INTERFACES_TABLE);
        /* disks */
        final boolean interfaceNodeChanged = updateInterfaceNodes();
        final boolean diskNodeChanged = updateDiskNodes();
        if (diskNodeChanged || interfaceNodeChanged) {
            getBrowser().reload(getNode());
        }
        //final VMSInfo vmsi = (VMSInfo) vmsNode.getUserObject();
        //if (vmsi != null) {
        //    vmsi.updateTable(VMSInfo.MAIN_TABLE);
        //}
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                getBrowser().repaintTree();
            }
        });
    }

    /**
     * Returns menu item for VNC different viewers.
     */
    private String getVNCMenuString(final String viewer, final Host host) {
        return Tools.getString("VMSVirtualDomainInfo.StartVNCViewerOn")
                            .replaceAll("@VIEWER@", viewer)
               + host.getName();
    }


    /**
     * Returns info panel.
     */
    public final JComponent getInfoPanel() {
        if (infoPanel != null) {
            return infoPanel;
        }
        final boolean abExisted = applyButton != null;
        /* main, button and options panels */
        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        final JTable table = getTable(HEADER_TABLE);
        if (table != null) {
            mainPanel.add(table.getTableHeader());
            mainPanel.add(table);
        }

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
        final MyButton overviewButton = new MyButton("VMs Overview",
                                                     BACK_ICON);
        overviewButton.setPreferredSize(new Dimension(200, 50));
        overviewButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                getBrowser().getVMSInfo().selectMyself();
            }
        });
        extraButtonPanel.add(overviewButton);
        addParams(optionsPanel,
                  params,
                  ClusterBrowser.SERVICE_LABEL_WIDTH,
                  ClusterBrowser.SERVICE_FIELD_WIDTH * 2,
                  null);
        for (final String param : params) {
            if (VMSXML.VM_PARAM_BOOT.equals(param)) {
                /* no virsh command for os-boot */
                paramComboBoxGet(param, null).setEnabled(false);
            } else if (VMSXML.VM_PARAM_VCPU.equals(param)) {
                paramComboBoxGet(param, null).setEnabled(!running);
            } else if (VMSXML.VM_PARAM_CURRENTMEMORY.equals(param)) {
                paramComboBoxGet(param, null).setEnabled(false);
            }
        }
        /* Actions */
        final JMenuBar mb = new JMenuBar();
        JMenu serviceCombo;
        serviceCombo = getActionsMenu();
        mb.add(serviceCombo);
        buttonPanel.add(mb, BorderLayout.EAST);

        mainPanel.add(optionsPanel);
        mainPanel.add(getTablePanel("Disks", DISK_TABLE));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(getTablePanel("Interfaces", INTERFACES_TABLE));
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(getMoreOptionsPanel(
                              ClusterBrowser.SERVICE_LABEL_WIDTH
                              + ClusterBrowser.SERVICE_FIELD_WIDTH * 2 + 4));
        newPanel.add(new JScrollPane(mainPanel));
        applyButton.setEnabled(checkResourceFields(null, params));
        infoPanel = newPanel;
        return infoPanel;
    }

    /**
     * Starts the domain.
     */
    public final void start(final Host host) {
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

    /**
     * Starts shutting down indicator.
     */
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

    /**
     * Shuts down the domain.
     */
    public final void shutdown(final Host host) {
        final boolean ret = VIRSH.shutdown(host, getDomainName());
        if (ret) {
            startShuttingdownIndicator(host);
        }
    }

    /**
     * Destroys down the domain.
     */
    public final void destroy(final Host host) {
        final boolean ret = VIRSH.destroy(host, getDomainName());
        if (ret) {
            startShuttingdownIndicator(host);
        }
    }

    /**
     * Reboots the domain.
     */
    public final void reboot(final Host host) {
        final boolean ret = VIRSH.reboot(host, getDomainName());
        if (ret) {
            startShuttingdownIndicator(host);
        }
    }

    /**
     * Suspend down the domain.
     */
    public final void suspend(final Host host) {
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

    /**
     * Resume down the domain.
     */
    public final void resume(final Host host) {
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
    private void addDiskPanel(final Point2D pos) {
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
            if (!(node.getUserObject() instanceof VMSDiskInfo)) {
                i++;
                continue;
            }
            break;
        }
        getNode().insert(resource, i);
        getBrowser().reload(getNode());
        vmsdi.selectMyself();
    }

    /** Adds new virtual interface. */
    private void addInterfacePanel(final Point2D pos) {
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
            if (!(node.getUserObject() instanceof VMSInterfaceInfo)) {
                i++;
                continue;
            }
            break;
        }

        getNode().insert(resource, i);
        getBrowser().reload(getNode());
        vmsii.selectMyself();
    }

    /** Add new hardware. */
    private final MyMenu getAddNewHardwareMenu(final String name) {
        return new MyMenu(name,
                          ConfigData.AccessType.ADMIN,
                          ConfigData.AccessType.OP) {
            private static final long serialVersionUID = 1L;

            public boolean enablePredicate() {
                return true;
            }

            public void update() {
                super.update();
                removeAll();
                final Point2D pos = getPos();
                final MyMenuItem newDiskMenuItem = new MyMenuItem(
                   Tools.getString("VMSVirtualDomainInfo.AddNewDisk"),
                   BlockDevInfo.HARDDISK_ICON_LARGE,
                   ConfigData.AccessType.ADMIN,
                   ConfigData.AccessType.OP) {
                    private static final long serialVersionUID = 1L;
                    public void action() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                getPopup().setVisible(false);
                            }
                        });
                        addDiskPanel(getPos());
                    }
                };
                newDiskMenuItem.setPos(pos);
                add(newDiskMenuItem);

                final MyMenuItem newInterfaceMenuItem = new MyMenuItem(
                   Tools.getString("VMSVirtualDomainInfo.AddNewInterface"),
                   NetInfo.NET_I_ICON_LARGE,
                   ConfigData.AccessType.ADMIN,
                   ConfigData.AccessType.OP) {
                    private static final long serialVersionUID = 1L;
                    public void action() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                getPopup().setVisible(false);
                            }
                        });
                        addInterfacePanel(getPos());
                    }
                };
                newInterfaceMenuItem.setPos(pos);
                add(newInterfaceMenuItem);
            }
        };
    }

    /** Adds vm domain start menu item. */
    public final void addStartMenu(final List<UpdatableItem> items,
                                   final Host host) {
        final MyMenuItem startMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.StartOn")
                            + host.getName(),
                            HostBrowser.HOST_ON_ICON_LARGE,
                            null,
                            ConfigData.AccessType.OP,
                            ConfigData.AccessType.OP) {

            private static final long serialVersionUID = 1L;

            public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && !vmsxml.isRunning(getDomainName());
            }

            public boolean enablePredicate() {
                return true;
            }

            public void action() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getPopup().setVisible(false);
                    }
                });
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && host != null) {
                    start(host);
                }
            }
        };
        items.add(startMenuItem);
    }

    /**
     * Adds vm domain shutdown menu item.
     */
    public final void addShutdownMenu(final List<UpdatableItem> items,
                                      final Host host) {
        final MyMenuItem shutdownMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.ShutdownOn")
                            + host.getName(),
                            SHUTDOWN_ICON,
                            null,
                            ConfigData.AccessType.OP,
                            ConfigData.AccessType.OP) {

            private static final long serialVersionUID = 1L;

            public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && vmsxml.isRunning(getDomainName());
            }

            public boolean enablePredicate() {
                return true;
            }

            public void action() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getPopup().setVisible(false);
                    }
                });
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && host != null) {
                    shutdown(host);
                }
            }
        };
        items.add(shutdownMenuItem);
    }

    /**
     * Adds vm domain reboot menu item.
     */
    public final void addRebootMenu(final List<UpdatableItem> items,
                                    final Host host) {
        final MyMenuItem rebootMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.RebootOn")
                            + host.getName(),
                            REBOOT_ICON,
                            null,
                            ConfigData.AccessType.OP,
                            ConfigData.AccessType.OP) {

            private static final long serialVersionUID = 1L;

            public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && vmsxml.isRunning(getDomainName());
            }

            public boolean enablePredicate() {
                return true;
            }

            public void action() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getPopup().setVisible(false);
                    }
                });
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && host != null) {
                    reboot(host);
                }
            }
        };
        items.add(rebootMenuItem);
    }

    /**
     * Adds vm domain resume menu item.
     */
    public final void addResumeMenu(final List<UpdatableItem> items,
                                    final Host host) {
        final MyMenuItem resumeMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.ResumeOn")
                            + host.getName(),
                            RESUME_ICON,
                            null,
                            ConfigData.AccessType.OP,
                            ConfigData.AccessType.OP) {

            private static final long serialVersionUID = 1L;

            public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && vmsxml.isSuspended(getDomainName());
            }

            public boolean enablePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.isSuspended(getDomainName());
            }

            public void action() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getPopup().setVisible(false);
                    }
                });
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && host != null) {
                    resume(host);
                }
            }
        };
        items.add(resumeMenuItem);
    }


    /**
     * Adds vm domain destroy menu item.
     */
    public final void addDestroyMenu(final MyMenu expertSubmenu,
                                     final Host host) {
        final MyMenuItem destroyMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.DestroyOn")
                            + host.getName(),
                            DESTROY_ICON,
                            null,
                            ConfigData.AccessType.OP,
                            ConfigData.AccessType.OP) {

            private static final long serialVersionUID = 1L;

            public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && vmsxml.isRunning(getDomainName());
            }

            public boolean enablePredicate() {
                return true;
            }

            public void action() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getPopup().setVisible(false);
                    }
                });
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && host != null) {
                    destroy(host);
                }
            }
        };
        expertSubmenu.add(destroyMenuItem);
    }

    /**
     * Adds vm domain suspend menu item.
     */
    public final void addSuspendMenu(final MyMenu expertSubmenu,
                                     final Host host) {
        final MyMenuItem suspendMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.SuspendOn")
                            + host.getName(),
                            PAUSE_ICON,
                            null,
                            ConfigData.AccessType.OP,
                            ConfigData.AccessType.OP) {

            private static final long serialVersionUID = 1L;

            public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && vmsxml.isRunning(getDomainName());
            }

            public boolean enablePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && !vmsxml.isSuspended(getDomainName());
            }

            public void action() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getPopup().setVisible(false);
                    }
                });
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && host != null) {
                    suspend(host);
                }
            }
        };
        expertSubmenu.add(suspendMenuItem);
    }

    /**
     * Adds vm domain resume menu item.
     */
    public final void addResumeExpertMenu(final MyMenu expertSubmenu,
                                          final Host host) {
        final MyMenuItem resumeMenuItem = new MyMenuItem(
                            Tools.getString("VMSVirtualDomainInfo.ResumeOn")
                            + host.getName(),
                            RESUME_ICON,
                            null,
                            ConfigData.AccessType.OP,
                            ConfigData.AccessType.OP) {

            private static final long serialVersionUID = 1L;

            public boolean visiblePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.getDomainNames().contains(getDomainName())
                       && vmsxml.isRunning(getDomainName());
            }

            public boolean enablePredicate() {
                final VMSXML vmsxml = getBrowser().getVMSXML(host);
                return vmsxml != null
                       && vmsxml.isSuspended(getDomainName());
            }

            public void action() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getPopup().setVisible(false);
                    }
                });
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && host != null) {
                    resume(host);
                }
            }
        };
        expertSubmenu.add(resumeMenuItem);
    }

    /**
     * Returns list of menu items for VM.
     */
    public final List<UpdatableItem> createPopup() {
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

        /* resume */
        for (final Host h : getBrowser().getClusterHosts()) {
            addResumeMenu(items, h);
        }
        items.add(getAddNewHardwareMenu(
                      Tools.getString("VMSVirtualDomainInfo.AddNewHardware")));

        /* expert options */
        final MyMenu expertSubmenu = new MyMenu(
                        Tools.getString("ClusterBrowser.ExpertSubmenu"),
                        ConfigData.AccessType.OP,
                        ConfigData.AccessType.OP) {
            private static final long serialVersionUID = 1L;
            public boolean enablePredicate() {
                return true;
            }
        };
        items.add(expertSubmenu);
        /* destroy */
        for (final Host h : getBrowser().getClusterHosts()) {
            addDestroyMenu(expertSubmenu, h);
        }

        /* suspend */
        for (final Host h : getBrowser().getClusterHosts()) {
            addSuspendMenu(expertSubmenu, h);
        }

        /* resume */
        for (final Host h : getBrowser().getClusterHosts()) {
            addResumeExpertMenu(expertSubmenu, h);
        }
        return items;
    }

    /**
     * Returns service icon in the menu. It can be started or stopped.
     */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null && vmsxml.isRunning(getDomainName())) {
                return HostBrowser.HOST_ON_ICON;
            }
        }
        return HostBrowser.HOST_OFF_ICON;
    }

    /** Returns category icon. */
    public final ImageIcon getCategoryIcon(final boolean testOnly) {
        return getMenuIcon(testOnly);
    }

    /** Adds vnc viewer menu items. */
    public final void addVncViewersToTheMenu(final List<UpdatableItem> items,
                                             final Host host) {
        final boolean testOnly = false;
        final VMSVirtualDomainInfo thisClass = this;
        if (Tools.getConfigData().isTightvnc()) {
            /* tight vnc test menu */
            final MyMenuItem tightvncViewerMenu = new MyMenuItem(
                                getVNCMenuString("TIGHT", host),
                                VNC_ICON,
                                null,
                                ConfigData.AccessType.RO,
                                ConfigData.AccessType.RO) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return visiblePredicate();
                }

                public boolean visiblePredicate() {
                    final VMSXML vmsxml = getBrowser().getVMSXML(host);
                    return vmsxml != null
                           && vmsxml.isRunning(getDomainName());
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
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
                                null,
                                ConfigData.AccessType.RO,
                                ConfigData.AccessType.RO) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return visiblePredicate();
                }

                public boolean visiblePredicate() {
                    final VMSXML vmsxml = getBrowser().getVMSXML(host);
                    return vmsxml != null
                           && vmsxml.isRunning(getDomainName());
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
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
                                    null,
                                    ConfigData.AccessType.RO,
                                    ConfigData.AccessType.RO) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return visiblePredicate();
                }

                public boolean visiblePredicate() {
                    final VMSXML vmsxml = getBrowser().getVMSXML(host);
                    return vmsxml != null
                           && vmsxml.isRunning(getDomainName());
                }

                public void action() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            getPopup().setVisible(false);
                        }
                    });
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

    /**
     * Returns long description of the specified parameter.
     */
    protected final String getParamLongDesc(final String param) {
        return SHORTNAME_MAP.get(param);
    }

    /**
     * Returns short description of the specified parameter.
     */
    protected final String getParamShortDesc(final String param) {
        return SHORTNAME_MAP.get(param);
    }

    /**
     * Returns preferred value for specified parameter.
     */
    protected final String getParamPreferred(final String param) {
        return null;
    }

    /**
     * Returns default value for specified parameter.
     */
    protected final String getParamDefault(final String param) {
        return DEFAULTS_MAP.get(param);
    }

    /**
     * Returns true if the value of the parameter is ok.
     */
    protected final boolean checkParam(final String param,
                                       final String newValue) {
        if (isRequired(param) && (newValue == null || "".equals(newValue))) {
            return false;
        }
        if (VMSXML.VM_PARAM_MEMORY.equals(param) && running) {
            final int mem = Tools.convertToKilobytes(newValue);
            final int curMem = Tools.convertToKilobytes(
                        getResource().getValue(VMSXML.VM_PARAM_CURRENTMEMORY));
            if (mem < curMem) {
                return false;
            }
        }
        return true;
    }

    /** Returns parameters. */
    public final String[] getParametersFromXML() {
        return VM_PARAMETERS;
    }

    /** Returns possible choices for drop down lists. */
    protected final Object[] getParamPossibleChoices(final String param) {
        if (VMSXML.VM_PARAM_BOOT.equals(param)) {
            return POSSIBLE_VALUES.get(param);
        }
        return null;
    }

    /** Returns section to which the specified parameter belongs. */
    protected final String getSection(final String param) {
        return SECTION_MAP.get(param);
    }

    /** Returns true if the specified parameter is required. */
    protected final boolean isRequired(final String param) {
        return true;
    }

    /** Returns true if the specified parameter is integer. */
    protected final boolean isInteger(final String param) {
        return false;
    }

    /** Returns true if the specified parameter is a label. */
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
        for (final String param : getParametersFromXML()) {
            final String value = getComboBoxValue(param);
            if (!Tools.areEqual(getParamSaved(param), value)) {
                parameters.put(param, value);
                getResource().setValue(param, value);
            }
        }
        VIRSH.setParameters(getBrowser().getClusterHosts(),
                            getDomainName(),
                            parameters);
        checkResourceFields(null, params);
    }

    /** Returns whether this parameter has a unit prefix. */
    protected final boolean hasUnitPrefix(final String param) {
        return HAS_UNIT_PREFIX.containsKey(param) && HAS_UNIT_PREFIX.get(param);
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

    /** Returns HTML string on which hosts the vm is defined. */
    public final String getDefinedOnString() {
        return definedOnString;
    }

    /** Returns HTML string on which hosts the vm is running. */
    public final String getRunningOnString() {
        return runningOnString;
    }

    /** Returns columns for the table. */
    protected final String[] getColumnNames(final String tableName) {
        if (HEADER_TABLE.equals(tableName)) {
            return new String[]{"Name", "Defined on", "Status", "Memory"};
        } else if (DISK_TABLE.equals(tableName)) {
            return new String[]{"Virtual Device", "Source"};
        } else if (INTERFACES_TABLE.equals(tableName)) {
            return new String[]{"Virtual Interface", "Source"};
        }
        return new String[]{};
    }

    /** Returns data for the table. */
    protected final Object[][] getTableData(final String tableName) {
        if (HEADER_TABLE.equals(tableName)) {
            return getMainTableData();
        } else if (DISK_TABLE.equals(tableName)) {
            return getDiskTableData();
        } else if (INTERFACES_TABLE.equals(tableName)) {
            return getInterfaceTableData();
        }
        return new Object[][]{};
    }

    /** Returns data for the main table. */
    protected final Object[][] getMainTableData() {
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
        final MyButton domainNameLabel = new MyButton(domainName, hostIcon);
        domainNameLabel.setOpaque(true);
        rows.add(new Object[]{domainNameLabel,
                              getDefinedOnString(),
                              getRunningOnString(),
                              getResource().getValue("memory")});
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Returns all disks. */
    protected final Map<String, DiskData> getDisks() {
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
    protected final Object[] getDiskDataRow(final String targetDev,
                                            final Map<String, VMSDiskInfo> dkti,
                                            final Map<String, DiskData> disks,
                                            final boolean opaque) {
        final DiskData diskData = disks.get(targetDev);
        if (diskData == null) {
            return new Object[]{targetDev, "unknown"};
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
        return new Object[]{targetDevLabel, source.toString()};
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
    final protected Object[] getInterfaceDataRow(
                                final String mac,
                                final Map<String, VMSInterfaceInfo> iToInfo,
                                final Map<String, InterfaceData> interfaces,
                                boolean opaque) {
        final InterfaceData interfaceData = interfaces.get(mac);
        if (interfaceData == null) {
            return new Object[]{mac, "unknown"};
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
        return new Object[]{iLabel, source.toString()};
    }

    /** Returns all interfaces. */
    protected final Map<String, InterfaceData> getInterfaces() {
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
    protected final void rowClicked(final String tableName, final String key) {
        if (HEADER_TABLE.equals(tableName)) {
            getBrowser().getVMSInfo().selectMyself();
        } else if (DISK_TABLE.equals(tableName)) {
            try {
                mDiskToInfoLock.acquire();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            final VMSDiskInfo vdi = diskKeyToInfo.get(key);
            mDiskToInfoLock.release();
            if (vdi != null) {
                vdi.selectMyself();
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
                vii.selectMyself();
            }
        }
    }

    /** Retrurns color for some rows. */
    protected final Color getTableRowColor(final String tableName,
                                           final String key) {
        if (HEADER_TABLE.equals(tableName)) {
            return rowColor;
        }
        return Browser.PANEL_BACKGROUND;
    }

    /** Alignment for the specified column. */
    protected final int getTableColumnAlignment(final String tableName,
                                                final int column) {

        if (column == 3 && HEADER_TABLE.equals(tableName)) {
            return SwingConstants.RIGHT;
        }
        return SwingConstants.LEFT;
    }

    /** Returns info object for this row. */
    protected final Info getTableInfo(final String tableName,
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
        }
        return null;
    }

    /** Returns whether the devices exists. */
    protected final boolean isDevice(final String dev) {
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
    protected final boolean isAdvanced(final String param) {
        return false;
    }

    /** Whether the parameter should be enabled. */
    protected final boolean isEnabled(final String param) {
        return true;
    }

    /** Returns access type of this parameter. */
    protected final ConfigData.AccessType getAccessType(final String param) {
        return ConfigData.AccessType.ADMIN;
    }
}
