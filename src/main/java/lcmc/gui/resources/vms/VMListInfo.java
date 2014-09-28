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
package lcmc.gui.resources.vms;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;

import lcmc.vm.ui.AddVMConfigDialog;
import lcmc.gui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.host.domain.Host;
import lcmc.vm.domain.VmsXml;
import lcmc.common.domain.Value;
import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.gui.HostBrowser;
import lcmc.gui.resources.CategoryInfo;
import lcmc.gui.resources.Info;
import lcmc.utilities.MenuAction;
import lcmc.utilities.MenuFactory;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

/**
 * This class shows a list of virtual machines.
 */
@Named
public final class VMListInfo extends CategoryInfo {
    /**
     * Default widths for columns.
     */
    private static final Map<Integer, Integer> DEFAULT_WIDTHS =
            Collections.unmodifiableMap(new HashMap<Integer, Integer>() {
                private static final long serialVersionUID = 1L;

                {
                    put(4, 80); /* remove button column */
                }
            });
    /**
     * On what raw is the vms virtual domain info object.
     */
    private volatile Map<String, DomainInfo> domainToInfo =
            new HashMap<String, DomainInfo>();
    /**
     * Colors for some rows.
     */
    private volatile Map<String, Color> domainToColor =
            new HashMap<String, Color>();
    @Inject
    private Provider<AddVMConfigDialog> addVMConfigDialogProvider;
    @Inject
    private Provider<DomainInfo> domainInfoProvider;
    @Inject
    private MenuFactory menuFactory;
    @Inject
    private Application application;
    @Inject
    private WidgetFactory widgetFactory;

    /**
     * Returns browser object of this info.
     */
    @Override
    public ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /**
     * Returns columns for the table.
     */
    @Override
    protected String[] getColumnNames(final String tableName) {
        return new String[]{"Name", "Defined on", "Status", "Memory", ""};
    }

    /**
     * Returns data for the table.
     */
    @Override
    protected Object[][] getTableData(final String tableName) {
        final List<Object[]> rows = new ArrayList<Object[]>();
        final Collection<String> domainNames = new TreeSet<String>();
        for (final Host host : getBrowser().getClusterHosts()) {
            final VmsXml vxml = getBrowser().getVmsXml(host);
            if (vxml != null) {
                domainNames.addAll(vxml.getDomainNames());
            }
        }
        final Map<String, DomainInfo> dti =
                new HashMap<String, DomainInfo>();
        final Map<String, Color> dtc = new HashMap<String, Color>();
        for (final String domainName : domainNames) {
            ImageIcon hostIcon = HostBrowser.HOST_OFF_ICON_LARGE;
            for (final Host host : getBrowser().getClusterHosts()) {
                final VmsXml vxml = getBrowser().getVmsXml(host);
                if (vxml != null && vxml.isRunning(domainName)) {
                    final Color bgColor = host.getPmColors()[0];
                    dtc.put(domainName, bgColor);
                    if (vxml.isSuspended(domainName)) {
                        hostIcon = DomainInfo.PAUSE_ICON;
                    } else {
                        hostIcon = HostBrowser.HOST_ON_ICON_LARGE;
                    }
                    break;
                }
            }
            final DomainInfo vmsvdi =
                    getBrowser().findVMSVirtualDomainInfo(domainName);
            if (vmsvdi != null) {
                dti.put(domainName, vmsvdi);
                final MyButton domainNameLabel = widgetFactory.createButton(domainName,
                        hostIcon);
                final MyButton removeDomain = widgetFactory.createButton(
                        "Remove",
                        ClusterBrowser.REMOVE_ICON_SMALL,
                        "Remove " + domainName
                                + " domain");
                application.makeMiniButton(removeDomain);
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

    /**
     * Returns info object for the key.
     */
    @Override
    protected Info getTableInfo(final String tableName, final String key) {
        return domainToInfo.get(key);
    }

    /**
     * Execute when row in the table was clicked.
     */
    @Override
    protected void rowClicked(final String tableName,
                              final String key,
                              final int column) {
        final DomainInfo vmsvdi = domainToInfo.get(key);
        if (vmsvdi != null) {
            final Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (DEFAULT_WIDTHS.containsKey(column)) {
                        /* remove button */
                        vmsvdi.removeMyself(Application.RunMode.LIVE);
                    } else {
                        vmsvdi.selectMyself();
                    }
                }
            });
            t.start();
        }
    }

    /**
     * Alignment for the specified column.
     */
    @Override
    protected int getTableColumnAlignment(final String tableName,
                                          final int column) {
        if (column == 3) {
            return SwingConstants.RIGHT;
        }
        return SwingConstants.LEFT;
    }

    /**
     * Selects the node in the menu.
     */
    @Override
    public void selectMyself() {
        super.selectMyself();
        getBrowser().nodeChanged(getNode());
    }

    /**
     * Returns comparator for column.
     */
    @Override
    protected Comparator<Object> getColComparator(final String tableName,
                                                  final int col) {
        if (col == 0) {
            /* memory */
            return new Comparator<Object>() {
                @Override
                public int compare(final Object o1, final Object o2) {
                    return ((AbstractButton) o1).getText().compareToIgnoreCase(
                            ((AbstractButton) o2).getText());
                }
            };
        } else if (col == 3) {
            /* memory */
            return new Comparator<Object>() {
                @Override
                public int compare(final Object o1, final Object o2) {
                    final long i1 = VmsXml.convertToKilobytes((Value) o1);
                    final long i2 = VmsXml.convertToKilobytes((Value) o2);
                    if (i1 < i2) {
                        return -1;
                    } else if (i1 > i2) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            };
        }
        return null;
    }

    /**
     * Retrurns color for some rows.
     */
    @Override
    protected Color getTableRowColor(final String tableName, final String key) {
        final Color c = domainToColor.get(key);
        if (c == null) {
            return Browser.PANEL_BACKGROUND;
        } else {
            return c;
        }
    }

    /**
     * Returns new button.
     */
    @Override
    protected JComponent getNewButton() {
        final MyButton newButton = widgetFactory.createButton(Tools.getString("VMListInfo.AddNewDomain"))
                .addAction(new Runnable() {
                    @Override
                    public void run() {
                        final Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                addDomainPanel();
                            }
                        });
                        t.start();
                    }
                });
        final JPanel bp = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        bp.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        bp.add(newButton);
        final Dimension d = bp.getPreferredSize();
        bp.setMaximumSize(new Dimension(Short.MAX_VALUE, (int) d.getHeight()));
        return bp;
    }

    /**
     * Adds new virtual domain.
     */
    public void addDomainPanel() {
        final DomainInfo domainInfo = domainInfoProvider.get();
        domainInfo.init(null, getBrowser());
        domainInfo.getResource().setNew(true);
        final DefaultMutableTreeNode resource =
                new DefaultMutableTreeNode(domainInfo);
        getBrowser().setNode(resource);
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                getNode().add(resource);
                getBrowser().reloadAndWait(getNode(), true);
                domainInfo.getInfoPanel();
                domainInfo.selectMyself();
                final Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final AddVMConfigDialog addVMConfigDialog = addVMConfigDialogProvider.get();
                        addVMConfigDialog.init(domainInfo);
                        addVMConfigDialog.showDialogs();
                    }
                });
                t.start();
            }
        });
    }

    /**
     * Returns list of menu items for VM.
     */
    @Override
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        /* New domain */
        final UpdatableItem newDomainMenuItem = menuFactory.createMenuItem(
                Tools.getString("VMListInfo.AddNewDomain"),
                HostBrowser.HOST_OFF_ICON_LARGE,
                new AccessMode(Application.AccessType.ADMIN, false),
                new AccessMode(Application.AccessType.OP, false))
            .addAction(new MenuAction() {
                @Override
                public void run(final String text) {
                    hidePopup();
                    addDomainPanel();
                }
            });
        items.add(newDomainMenuItem);
        return items;
    }

    /**
     * Returns whether the column is a button, 0 column is always a button.
     */
    @Override
    protected Map<Integer, Integer> getDefaultWidths(final String tableName) {
        return DEFAULT_WIDTHS;
    }

    /**
     * Returns if this column contains remove button.
     */
    @Override
    protected boolean isControlButton(final String tableName,
                                      final int column) {
        return DEFAULT_WIDTHS.containsKey(column);
    }

    /**
     * Returns tool tip text in the table.
     */
    @Override
    protected String getTableToolTip(final String tableName,
                                     final String key,
                                     final Object object,
                                     final int raw,
                                     final int column) {
        if (DEFAULT_WIDTHS.containsKey(column)) {
            return "Remove domain " + key + '.';
        }
        return super.getTableToolTip(tableName, key, object, raw, column);
    }
}
