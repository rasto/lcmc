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
import drbd.gui.SpringUtilities;
import drbd.gui.GuiComboBox;
import drbd.data.VMSXML;
import drbd.data.Host;
import drbd.data.resources.Resource;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;
import drbd.utilities.MyMenuItem;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JScrollPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.BorderLayout;

/**
 * This class holds info about VirtualDomain service in the VMs category,
 * but not in the cluster view.
 */
public class VMSVirtualDomainInfo extends EditableInfo {
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Extra options panel. */
    private final JPanel extraOptionsPanel = new JPanel();
    /** Running VM icon. */
    private static final ImageIcon VM_RUNNING_ICON =
        Tools.createImageIcon(
                Tools.getDefault("VMSVirtualDomainInfo.VMRunningIcon"));
    /** Stopped VM icon. */
    private static final ImageIcon VM_STOPPED_ICON =
        Tools.createImageIcon(
                Tools.getDefault("VMSVirtualDomainInfo.VMStoppedIcon"));
    /** All parameters. */
    private static final String[] parameters = new String[]{"name",
                                                            "status"};
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
    protected ClusterBrowser getBrowser() {
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
    public void updateParameters() {
        for (final String param : getParametersFromXML()) {
            final String oldValue = getParamSaved(param);
            if ("status".equals(param)) {
                final Set<String> runningOnHosts = new TreeSet<String>();
                for (final Host h : getBrowser().getClusterHosts()) {
                    final VMSXML vmsxml = getBrowser().getVMSXML(h);
                    if (vmsxml != null && vmsxml.isRunning(toString())) {
                        runningOnHosts.add(h.getName());
                    }
                }
                if (runningOnHosts.size() == 0) {
                    getResource().setValue("status", "Stopped");
                } else {
                    getResource().setValue(
                        "status",
                        "Running on: "
                        + Tools.join(", ", runningOnHosts.toArray(
                                        new String[runningOnHosts.size()])));
                }
            }
            final String value = getParamSaved(param);
            if (!Tools.areEqual(value, oldValue)) {
                final GuiComboBox cb = paramComboBoxGet(param, null);
                if (cb != null) {
                    if (Tools.areEqual(cb.getStringValue(), oldValue)) {
                        /* only if it is not changed by user. */
                        cb.setValue(value);
                    }
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
        initApplyButton(null);
        addParams(optionsPanel,
                  extraOptionsPanel,
                  getParametersFromXML(),
                  ClusterBrowser.SERVICE_LABEL_WIDTH,
                  ClusterBrowser.SERVICE_FIELD_WIDTH * 2,
                  null); // TODO: same as?
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
        return "undef";
    }

    /**
     * Returns short description of the specified parameter.
     */
    protected final String getParamShortDesc(final String param) {
        return param;
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
        return null;
    }

    /**
     * Returns true if the value of the parameter is ok.
     */
    protected final boolean checkParam(final String param,
                                       final String newValue) {
        return true;
    }

    /**
     * Returns parameters.
     */
    public final String[] getParametersFromXML() {
        return parameters;
    }

    /**
     * Returns possible choices for drop down lists.
     */
    protected final Object[] getParamPossibleChoices(final String param) {
        return null;
    }

    /**
     * Returns section to which the specified parameter belongs.
     */
    protected final String getSection(final String param) {
        return "undef";
    }

    /**
     * Returns true if the specified parameter is required.
     */
    protected final boolean isRequired(final String param) {
        return false;
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
}
