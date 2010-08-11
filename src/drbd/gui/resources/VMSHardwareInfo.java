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
import drbd.data.VMSXML;
import drbd.data.Host;
import drbd.data.resources.Resource;
import drbd.data.ConfigData;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;
import drbd.utilities.Unit;
import drbd.utilities.MyButton;
import drbd.utilities.MyMenuItem;

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
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * This class holds info about Virtual Hardware.
 */
public abstract class VMSHardwareInfo extends EditableInfo {
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** VMS virtual domain info object. */
    private final VMSVirtualDomainInfo vmsVirtualDomainInfo;
    /** Back to overview icon. */
    private static final ImageIcon BACK_ICON = Tools.createImageIcon(
                                                 Tools.getDefault("BackIcon"));
    /** Default units. */
    private static final Map<String, String> DEFAULT_UNIT =
                                                new HashMap<String, String>();
    /** If it has units. */
    private static final Map<String, Boolean> HAS_UNIT =
                                                new HashMap<String, Boolean>();
    /** Creates the VMSHardwareInfo object. */
    public VMSHardwareInfo(final String name, final Browser browser,
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
        final JTable headerTable = getTable(VMSVirtualDomainInfo.HEADER_TABLE);
        if (headerTable != null) {
            mainPanel.add(headerTable.getTableHeader());
            mainPanel.add(headerTable);
        }
        addHardwareTable(mainPanel);

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

    /** Return info object of the whole domain. */
    protected final VMSVirtualDomainInfo getVMSVirtualDomainInfo() {
        return vmsVirtualDomainInfo;
    }

    /** Get first host that has this vm and is connected. */
    protected final Host getFirstConnectedHost() {
        for (final Host h : getBrowser().getClusterHosts()) {
            final VMSXML vmsxml = getBrowser().getVMSXML(h);
            if (vmsxml != null && h.isConnected()) {
                return h;
            }
        }
        return null;
    }

    /** Returns list of menu items. */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final boolean testOnly = false;
        /* remove service */
        final MyMenuItem removeMenuItem = new MyMenuItem(
                    Tools.getString("VMSHardwareInfo.Menu.Remove"),
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

    /** Removes this hardware from the libvirt with confirmation dialog. */
    public final void removeMyself(final boolean testOnly) {
        if (getResource().isNew()) {
            super.removeMyself(testOnly);
            getResource().setNew(false);
            return;
        }
        String desc = Tools.getString("VMSHardwareInfo.confirmRemove.Description");

        desc  = desc.replaceAll("@HW@", toString());
        if (Tools.confirmDialog(
               Tools.getString("VMSHardwareInfo.confirmRemove.Title"),
               desc,
               Tools.getString("VMSHardwareInfo.confirmRemove.Yes"),
               Tools.getString("VMSHardwareInfo.confirmRemove.No"))) {
            removeMyselfNoConfirm(testOnly);
            getResource().setNew(false);
        }
    }

    /** Removes this disk without confirmation dialog. */
    protected abstract void removeMyselfNoConfirm(final boolean testOnly);

    /** Applies the changes. */
    public abstract void apply(final boolean testOnly);

    /** Adds disk table with only this disk to the main panel. */
    protected abstract void addHardwareTable(final JPanel mainPanel);
}
