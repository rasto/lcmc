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
import drbd.data.Host;
import drbd.data.resources.Resource;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;
import drbd.utilities.MyMenuItem;
import drbd.utilities.VIRSH;
import drbd.utilities.Unit;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JScrollPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * This class holds info about VirtualDomain service in the VMs category,
 * but not in the cluster view.
 */
public class VMSVirtualDomainInfo extends EditableInfo {
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Extra options panel. */
    private final JPanel extraOptionsPanel = new JPanel();
    /** Whether the vm is running on at least one host. */
    private boolean running = false;
    /** Running VM icon. */
    private static final ImageIcon VM_RUNNING_ICON =
        Tools.createImageIcon(
                Tools.getDefault("VMSVirtualDomainInfo.VMRunningIcon"));
    /** Stopped VM icon. */
    private static final ImageIcon VM_STOPPED_ICON =
        Tools.createImageIcon(
                Tools.getDefault("VMSVirtualDomainInfo.VMStoppedIcon"));
    /** All parameters. */
    private static final String[] VM_PARAMETERS = new String[]{"name",
                                                               "defined",
                                                               "status",
                                                               "vcpu",
                                                               "currentMemory",
                                                               "memory",
                                                               "os-boot",
                                                               "autostart"};
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
    static {
        SECTION_MAP.put("name",          "General Info");
        SECTION_MAP.put("defined",       "General Info");
        SECTION_MAP.put("status",        "General Info");
        SECTION_MAP.put("vcpu",          "Virtual System");
        SECTION_MAP.put("currentMemory", "Virtual System");
        SECTION_MAP.put("memory",        "Virtual System");
        SECTION_MAP.put("os-boot",       "Virtual System");
        SECTION_MAP.put("autostart",     "Virtual System");
        SHORTNAME_MAP.put("name",          "Name");
        SHORTNAME_MAP.put("defined",       "Defined On");
        SHORTNAME_MAP.put("status",        "Status");
        SHORTNAME_MAP.put("vcpu",          "Number of CPUs");
        SHORTNAME_MAP.put("currentMemory", "Current Memory");
        SHORTNAME_MAP.put("memory",        "Max Memory");
        SHORTNAME_MAP.put("os-boot",       "Boot Device");
        SHORTNAME_MAP.put("autostart",     "Autostart");
        FIELD_TYPES.put("name",          GuiComboBox.Type.LABELFIELD);
        FIELD_TYPES.put("defined",       GuiComboBox.Type.LABELFIELD);
        FIELD_TYPES.put("status",        GuiComboBox.Type.LABELFIELD);
        FIELD_TYPES.put("currentMemory", GuiComboBox.Type.TEXTFIELDWITHUNIT);
        FIELD_TYPES.put("memory",        GuiComboBox.Type.TEXTFIELDWITHUNIT);
        FIELD_TYPES.put("autostart",     GuiComboBox.Type.CHECKBOX);
        DEFAULTS_MAP.put("autostart", "False");
        DEFAULTS_MAP.put("os-boot",   "hd");
        DEFAULTS_MAP.put("vcpu",      "1");
        HAS_UNIT_PREFIX.put("memory", true);
        HAS_UNIT_PREFIX.put("currentMemory", true);
        // TODO: no virsh command for os-boot
        POSSIBLE_VALUES.put("os-boot",
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
        POSSIBLE_VALUES.put("autostart", new String[]{"True", "False"});
    }
    /**
     * Creates the VMSVirtualDomainInfo object.
     */
    public VMSVirtualDomainInfo(final String name,
                                final Browser browser) {
        super(name, browser);
        setResource(new Resource(name));
        getResource().setValue("name", name);
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
     * Sets service parameters with values from resourceNode hash.
     */
    public final void updateParameters() {
        for (final String param : getParametersFromXML()) {
            final String oldValue = getParamSaved(param);
            String value = getParamSaved(param);
            final GuiComboBox cb = paramComboBoxGet(param, null);
            if ("status".equals(param)) {
                final List<String> runningOnHosts = new ArrayList<String>();
                final List<String> definedhosts = new ArrayList<String>();
                for (final Host h : getBrowser().getClusterHosts()) {
                    final VMSXML vmsxml = getBrowser().getVMSXML(h);
                    final String hostName = h.getName();
                    if (vmsxml != null
                        && vmsxml.getDomainNames().contains(toString())) {
                        if (vmsxml.isRunning(toString())) {
                            runningOnHosts.add(hostName);
                        }
                        definedhosts.add(hostName);
                    } else {
                        definedhosts.add("<font color=\"#A3A3A3\">"
                                         + hostName + "</font>");
                    }
                }
                getResource().setValue(
                    "defined",
                    "<html>"
                    + Tools.join(" ", definedhosts.toArray(
                                    new String[definedhosts.size()]))
                    + "</html>");
                if (runningOnHosts.isEmpty()) {
                    getResource().setValue("status", "Stopped");
                    running = false;
                } else {
                    running = true;
                    getResource().setValue(
                        "status",
                        "<html>Running on: "
                        + Tools.join(", ", runningOnHosts.toArray(
                                            new String[runningOnHosts.size()]))
                        + "</html>");
                }
            } else if ("name".equals(param)) {
            } else {
                if (cb != null) {
                    if ("vcpu".equals(param)) {
                        paramComboBoxGet(param, null).setEnabled(!running);
                    } else if ("currentMemory".equals(param)) {
                        paramComboBoxGet(param, null).setEnabled(false);
                    }
                }
                for (final Host h : getBrowser().getClusterHosts()) {
                    final VMSXML vmsxml = getBrowser().getVMSXML(h);
                    if (vmsxml != null) {
                        final String savedValue =
                                        vmsxml.getValue(toString(), param);
                        if (savedValue != null) {
                            value = savedValue;
                        }
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

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));

        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        extraOptionsPanel.setBackground(ClusterBrowser.EXTRA_PANEL_BACKGROUND);
        extraOptionsPanel.setLayout(new BoxLayout(extraOptionsPanel,
                                    BoxLayout.Y_AXIS));
        extraOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
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
        addApplyButton(buttonPanel);
        addParams(optionsPanel,
                  extraOptionsPanel,
                  params,
                  ClusterBrowser.SERVICE_LABEL_WIDTH,
                  ClusterBrowser.SERVICE_FIELD_WIDTH * 2,
                  null); // TODO: same as?
        for (final String param : params) {
            if ("os-boot".equals(param)) {
                /* no virsh command for os-boot */
                paramComboBoxGet(param, null).setEnabled(false);
            } else if ("vcpu".equals(param)) {
                paramComboBoxGet(param, null).setEnabled(!running);
            } else if ("currentMemory".equals(param)) {
                paramComboBoxGet(param, null).setEnabled(false);
            }
        }
        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        JMenu serviceCombo;
        serviceCombo = getActionsMenu();
        updateMenus(null);
        mb.add(serviceCombo);
        buttonPanel.add(mb, BorderLayout.EAST);
        Tools.registerExpertPanel(extraOptionsPanel);

        mainPanel.add(optionsPanel);
        mainPanel.add(extraOptionsPanel);
        final JPanel newPanel = new JPanel();
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.add(buttonPanel);
        newPanel.add(new JScrollPane(mainPanel));
        newPanel.add(Box.createVerticalGlue());
        applyButton.setEnabled(checkResourceFields(null, params));
        infoPanel = newPanel;
        return infoPanel;
    }

    /**
     * Returns list of menu items for VM.
     */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        for (final Host h : getBrowser().getClusterHosts()) {
            addVncViewersToTheMenu(items, h);
        }
        return items;
    }

    /**
     * Returns service icon in the menu. It can be started or stopped.
     * TODO: broken icon, not managed icon.
     */
    public final ImageIcon getMenuIcon(final boolean testOnly) {
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null && vmsxml.isRunning(toString())) {
                return VM_RUNNING_ICON;
            }
        }
        return VM_STOPPED_ICON;
    }

    /**
     * Adds vnc viewer menu items.
     */
    public final void addVncViewersToTheMenu(final List<UpdatableItem> items,
                                             final Host host) {
        final boolean testOnly = false;
        final VMSVirtualDomainInfo thisClass = this;
        if (Tools.getConfigData().isTightvnc()) {
            /* tight vnc test menu */
            final MyMenuItem tightvncViewerMenu = new MyMenuItem(
                                "start TIGHT VNC viewer on " + host.getName(),
                                null,
                                null) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    final VMSXML vmsxml = getBrowser().getVMSXML(host);
                    return vmsxml != null
                           && vmsxml.isRunning(thisClass.toString());
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
                                                         thisClass.toString());
                        final Host host = vxml.getHost();
                        if (host != null && remotePort > 0) {
                            Tools.startTightVncViewer(host, remotePort);
                        }
                    }
                }
            };
            registerMenuItem(tightvncViewerMenu);
            items.add(tightvncViewerMenu);
        }

        if (Tools.getConfigData().isUltravnc()) {
            /* ultra vnc test menu */
            final MyMenuItem ultravncViewerMenu = new MyMenuItem(
                                "start ULTRA VNC viewer on " + host.getName(),
                                null,
                                null) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    final VMSXML vmsxml = getBrowser().getVMSXML(host);
                    return vmsxml != null
                           && vmsxml.isRunning(thisClass.toString());
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
                                                         thisClass.toString());
                        final Host host = vxml.getHost();
                        if (host != null && remotePort > 0) {
                            Tools.startUltraVncViewer(host, remotePort);
                        }
                    }
                }
            };
            registerMenuItem(ultravncViewerMenu);
            items.add(ultravncViewerMenu);
        }

        if (Tools.getConfigData().isRealvnc()) {
            /* real vnc test menu */
            final MyMenuItem realvncViewerMenu = new MyMenuItem(
                                    "start REAL VNC test on " + host.getName(),
                                    null,
                                    null) {

                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    final VMSXML vmsxml = getBrowser().getVMSXML(host);
                    return vmsxml != null
                           && vmsxml.isRunning(thisClass.toString());
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
                                                         thisClass.toString());
                        final Host host = vxml.getHost();
                        if (host != null && remotePort > 0) {
                            Tools.startRealVncViewer(host, remotePort);
                        }
                    }
                }
            };
            registerMenuItem(realvncViewerMenu);
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
        return true;
    }

    /**
     * Returns parameters.
     */
    public final String[] getParametersFromXML() {
        return VM_PARAMETERS;
    }

    /**
     * Returns possible choices for drop down lists.
     */
    protected final Object[] getParamPossibleChoices(final String param) {
        if ("os-boot".equals(param)) {
            return POSSIBLE_VALUES.get(param);
        }
        return null;
    }

    /**
     * Returns section to which the specified parameter belongs.
     */
    protected final String getSection(final String param) {
        return SECTION_MAP.get(param);
    }

    /**
     * Returns true if the specified parameter is required.
     */
    protected final boolean isRequired(final String param) {
        return true;
    }

    /**
     * Returns true if the specified parameter is integer.
     */
    protected final boolean isInteger(final String param) {
        return false;
    }

    /**
     * Returns true if the specified parameter is of time type.
     */
    protected final boolean isTimeType(final String param) {
        return false;
    }

    /**
     * Returns whether parameter is checkbox.
     */
    protected final boolean isCheckBox(final String param) {
        return false;
    }

    /**
     * Returns the type of the parameter according to the OCF.
     */
    protected final String getParamType(final String param) {
        return "undef";
    }

    /**
     * Returns type of the field.
     */
    protected final GuiComboBox.Type getFieldType(final String param) {
        return FIELD_TYPES.get(param);
    }

    /**
     * Applies the changes.
     */
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
                            toString(),
                            parameters);
        checkResourceFields(null, params);
    }

    /**
     * Returns whether this parameter has a unit prefix.
     */
    protected final boolean hasUnitPrefix(final String param) {
        return HAS_UNIT_PREFIX.containsKey(param) && HAS_UNIT_PREFIX.get(param);
    }

    /**
     * Returns units.
     */
    protected final Unit[] getUnits() {
        return new Unit[]{
                   //new Unit("", "", "KiByte", "KiBytes"), /* default unit */
                   new Unit("K", "K", "KiByte", "KiBytes"),
                   new Unit("M", "M", "MiByte", "MiBytes"),
                   new Unit("G",  "G",  "GiByte",      "GiBytes"),
                   new Unit("T",  "T",  "TiByte",      "TiBytes")
       };
    }

    /**
     * Returns the default unit for the parameter.
     */
    protected final String getDefaultUnit(final String param) {
        return DEFAULT_UNIT.get(param);
    }
}
