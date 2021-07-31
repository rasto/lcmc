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
package lcmc.crm.ui.resource;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import lcmc.cluster.ui.ClusterBrowser;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.common.ui.utils.MyButton;
import lcmc.crm.domain.ResourceAgent;

/**
 * This class holds the information about resource agent class and its services.
 */
@Named
public final class ResourceAgentClassInfo extends HbCategoryInfo {
    private static final ImageIcon BACK_TO_OVERVIEW_ICON = Tools.createImageIcon(Tools.getDefault("BackIcon"));
    /**
     * Map from ResourceAgent name to its object. It is possible only within a class.
     */
    private final Map<String, ResourceAgent> raMap = new HashMap<>();
    @Inject
    private Application application;
    @Inject
    private WidgetFactory widgetFactory;

    @Override
    public void init(final String name, final Browser browser) {
        super.init(name, browser);
        for (final ResourceAgent ra : getBrowser().getCrmXml().getServices(name)) {
            raMap.put(ra.getServiceName(), ra);
        }
    }

    @Override
    protected String[] getColumnNames(final String tableName) {
        return new String[]{"Name", "Provider"};
    }

    /** Returns data for the table. */
    @Override
    protected Object[][] getTableData(final String tableName) {
        final List<Object[]> rows = new ArrayList<>();
        for (final ResourceAgent ra : getBrowser().getCrmXml().getServices(getName())) {
            final MyButton nameLabel = widgetFactory.createButton(ra.getServiceName());
            rows.add(new Object[]{nameLabel, ra.getProvider()});
        }
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Returns name as it appears in the menu. */
    @Override
    public String toString() {
        return getName().toUpperCase();
    }

    /** Execute when row in the table was clicked. */
    @Override
    protected void rowClicked(final String tableName, final String key, final int column) {
        final ResourceAgent ra = raMap.get(key);
        if (ra != null) {
            final AvailableServiceInfo asi = getBrowser().getAvailableServiceInfoMap(ra);
            if (asi != null) {
                asi.selectMyself();
            }
        }
    }

    @Override
    protected JComponent getBackButton() {
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        final MyButton overviewButton = widgetFactory.createButton(
                                                            Tools.getString("ClusterBrowser.ClassesOverviewButton"),
                                                            BACK_TO_OVERVIEW_ICON);
        overviewButton.setPreferredSize(new Dimension(application.scaled(180), 50));
        overviewButton.addActionListener(e -> getBrowser().getAvailableServicesInfo().selectMyself());
        buttonPanel.add(overviewButton);
        return buttonPanel;
    }
}
