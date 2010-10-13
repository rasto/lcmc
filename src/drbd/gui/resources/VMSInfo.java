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

import drbd.AddVMConfigDialog;
import drbd.gui.Browser;
import drbd.gui.HostBrowser;
import drbd.gui.ClusterBrowser;
import drbd.data.VMSXML;
import drbd.data.Host;
import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import drbd.utilities.MyMenuItem;
import drbd.utilities.UpdatableItem;

import javax.swing.SwingConstants;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.Comparator;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * This class shows a list of virtual machines.
 */
public class VMSInfo extends CategoryInfo {
    /** On what raw is the vms virtual domain info object. */
    private volatile Map<String, VMSVirtualDomainInfo> domainToInfo =
                                   new HashMap<String, VMSVirtualDomainInfo>();
    /** Colors for some rows. */
    private volatile Map<String, Color> domainToColor =
                                                  new HashMap<String, Color>();
    /** Default widths for columns. */
    private static final Map<Integer, Integer> DEFAULT_WIDTHS =
                                                new HashMap<Integer, Integer>();
    static {
        DEFAULT_WIDTHS.put(4, 65); /* remove button column */
    }
    /** Creates the new VMSInfo object with name of the category. */
    public VMSInfo(final String name, final Browser browser) {
        super(name, browser);
    }

    /** Returns browser object of this info. */
    protected final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /** Returns columns for the table. */
    protected final String[] getColumnNames(final String tableName) {
        return new String[]{"Name", "Defined on", "Status", "Memory", ""};
    }

    /** Returns data for the table. */
    protected final Object[][] getTableData(final String tableName) {
        final List<Object[]> rows = new ArrayList<Object[]>();
        final Set<String> domainNames = new TreeSet<String>();
        for (final Host host : getBrowser().getClusterHosts()) {
            final VMSXML vxml = getBrowser().getVMSXML(host);
            if (vxml != null) {
                domainNames.addAll(vxml.getDomainNames());
            }
        }
        final Map<String, VMSVirtualDomainInfo> dti =
                                   new HashMap<String, VMSVirtualDomainInfo>();
        final Map<String, Color> dtc = new HashMap<String, Color>();
        for (final String domainName : domainNames) {
            ImageIcon hostIcon = HostBrowser.HOST_OFF_ICON_LARGE;
            for (final Host host : getBrowser().getClusterHosts()) {
                final VMSXML vxml = getBrowser().getVMSXML(host);
                if (vxml != null && vxml.isRunning(domainName)) {
                    final Color bgColor = host.getPmColors()[0];
                    dtc.put(domainName, bgColor);
                    if (vxml.isSuspended(domainName)) {
                        hostIcon = VMSVirtualDomainInfo.PAUSE_ICON;
                    } else {
                        hostIcon = HostBrowser.HOST_ON_ICON_LARGE;
                    }
                    break;
                }
            }
            final VMSVirtualDomainInfo vmsvdi =
                        getBrowser().findVMSVirtualDomainInfo(domainName);
            if (vmsvdi != null) {
                dti.put(domainName, vmsvdi);
                final MyButton domainNameLabel = new MyButton(domainName,
                                                              hostIcon);
                final MyButton removeDomain = new MyButton(
                                                   null,
                                                   ClusterBrowser.REMOVE_ICON,
                                                   "Remove " + domainName
                                                   + " domain");
                rows.add(new Object[]{domainNameLabel,
                                      vmsvdi.getDefinedOnString(),
                                      vmsvdi.getRunningOnString(),
                                      vmsvdi.getResource().getValue("memory"),
                                      removeDomain});
            }
        }
        domainToInfo = dti;
        domainToColor = dtc;
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Returns info object for the key. */
    protected final Info getTableInfo(final String tableName,
                                      final String key) {
        return domainToInfo.get(key);
    }

    /** Execute when row in the table was clicked. */
    protected final void rowClicked(final String tableName,
                                    final String key,
                                    final int column) {
        final VMSVirtualDomainInfo vmsvdi = domainToInfo.get(key);
        if (vmsvdi != null) {
            final Thread t = new Thread(new Runnable() {
                public void run() {
                    if (DEFAULT_WIDTHS.containsKey(column)) {
                        /* remove button */
                        vmsvdi.removeMyself(false);
                    } else {
                        vmsvdi.selectMyself();
                    }
                }
            });
            t.start();
        }
    }

    /** Alignment for the specified column. */
    protected final int getTableColumnAlignment(final String tableName,
                                                final int column) {
        if (column == 3) {
            return SwingConstants.RIGHT;
        }
        return SwingConstants.LEFT;
    }

    /** Selects the node in the menu. */
    public final void selectMyself() {
        super.selectMyself();
        getBrowser().nodeChanged(getNode());
    }

    /** Returns comparator for column. */
    protected final Comparator<Object> getColComparator(final String tableName,
                                                        final int col) {
        if (col == 0) {
            /* memory */
            final Comparator<Object> c = new Comparator<Object>() {
                public int compare(final Object l1, final Object l2) {
                    return ((MyButton) l1).getText().compareToIgnoreCase(
                                                    ((MyButton) l2).getText());
                }
            };
            return c;
        } else if (col == 3) {
            /* memory */
            final Comparator<Object> c = new Comparator<Object>() {
                public int compare(final Object s1, final Object s2) {
                    final int i1 = Tools.convertToKilobytes((String) s1);
                    final int i2 = Tools.convertToKilobytes((String) s2);
                    if (i1 < i2) {
                        return -1;
                    } else if (i1 > i2) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            };
            return c;
        }
        return null;
    }

    /** Retrurns color for some rows. */
    protected final Color getTableRowColor(final String tableName,
                                           final String key) {
        final Color c = domainToColor.get(key);
        if (c == null) {
            return Browser.PANEL_BACKGROUND;
        } else {
            return c;
        }
    }

    /** Returns new button. */
    protected JComponent getNewButton() {
        final MyButton newButton = new MyButton(
                                     Tools.getString("VMSInfo.AddNewDomain"));
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Thread t = new Thread(new Runnable() {
                    public void run() {
                        addDomainPanel();
                    }
                });
                t.start();
            }
        });
        final JPanel bp = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bp.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        bp.add(newButton);
        final Dimension d = bp.getPreferredSize();
        bp.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) d.getHeight()));
        return (JComponent) bp;
    }

    /** Adds new virtual domain. */
    public final void addDomainPanel() {
        final VMSVirtualDomainInfo vmsdi =
                                new VMSVirtualDomainInfo(null, getBrowser());
        vmsdi.getResource().setNew(true);
        final DefaultMutableTreeNode resource =
                                           new DefaultMutableTreeNode(vmsdi);
        getBrowser().setNode(resource);
        getNode().add(resource);
        getBrowser().reload(getNode(), true);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                vmsdi.getInfoPanel();
                vmsdi.selectMyself();
                final Thread t = new Thread(new Runnable() {
                    public void run() {
                        AddVMConfigDialog avmcd = new AddVMConfigDialog(vmsdi);
                        avmcd.showDialogs();
                    }
                });
                t.start();
            }
        });
    }

    /** Returns list of menu items for VM. */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        /* New domain */
        final MyMenuItem newDomainMenuItem = new MyMenuItem(
                       Tools.getString("VMSInfo.AddNewDomain"),
                       HostBrowser.HOST_OFF_ICON_LARGE,
                       new AccessMode(ConfigData.AccessType.ADMIN, false),
                       new AccessMode(ConfigData.AccessType.OP, false)) {
                        private static final long serialVersionUID = 1L;
                        public void action() {
                            hidePopup();
                            addDomainPanel();
                        }
        };
        items.add(newDomainMenuItem);
        return items;
    }

    /** Returns whether the column is a button, 0 column is always a button. */
    protected final Map<Integer, Integer> getDefaultWidths(
                                                      final String tableName) {
        return DEFAULT_WIDTHS;
    }

    /** Returns if this column contains remove button. */
    protected final boolean isControlButton(final String tableName,
                                           final int column) {
        return DEFAULT_WIDTHS.containsKey(column);
    }

    /** Returns tool tip text in the table. */
    protected String getTableToolTip(final String tableName,
                                     final String key,
                                     final Object object,
                                     final int raw,
                                     final int column) {
        if (DEFAULT_WIDTHS.containsKey(column)) {
            return "Remove domain " + key + ".";
        }
        return super.getTableToolTip(tableName, key, object, raw, column);
    }
}
