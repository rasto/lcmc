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
import lcmc.gui.ClusterBrowser;
import lcmc.data.ResourceAgent;
import lcmc.utilities.Tools;
import lcmc.utilities.MyButton;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JComponent;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * This class holds the information about resource agent class and its
 * services.
 */
public final class ResourceAgentClassInfo extends HbCategoryInfo {
    /** Map from ResourceAgent name to its object. It is possible only within
     * a class. */
    private final Map<String, ResourceAgent> raMap =
                                         new HashMap<String, ResourceAgent>();
    /** Back to overview icon. */
    private static final ImageIcon BACK_ICON = Tools.createImageIcon(
                                            Tools.getDefault("BackIcon"));
    /** Prepares a new <code>ResourceAgentClassInfo</code> object. */
    public ResourceAgentClassInfo(final String name, final Browser browser) {
        super(name, browser);
        for (final ResourceAgent ra : getBrowser().getCRMXML().getServices(
                                                                      name)) {
            raMap.put(ra.getName(), ra);
        }
    }

    /** Returns columns for the table. */
    @Override protected String[] getColumnNames(final String tableName) {
        return new String[]{"Name", "Provider"};
    }

    /** Returns data for the table. */
    @Override  protected Object[][] getTableData(final String tableName) {
        final List<Object[]> rows = new ArrayList<Object[]>();
        /** Get classes */
        for (final ResourceAgent ra : getBrowser().getCRMXML().getServices(
                                                                  getName())) {
            final MyButton nameLabel = new MyButton(ra.getName());
            rows.add(new Object[]{nameLabel,
                                  ra.getProvider()});
        }
        return rows.toArray(new Object[rows.size()][]);
    }

    /** Returns name as it appears in the menu. */
    @Override  public String toString() {
        return getName().toUpperCase();
    }

    /** Execute when row in the table was clicked. */
    @Override  protected void rowClicked(final String tableName,
                                         final String key,
                                         final int column) {
        final ResourceAgent ra = raMap.get(key);
        if (ra != null) {
            final AvailableServiceInfo asi =
                                  getBrowser().getAvailableServiceInfoMap(ra);
            if (asi != null) {
                asi.selectMyself();
            }
        }
    }

    /** Returns back button. */
    @Override  protected JComponent getBackButton() {
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,
                                                             0,
                                                             0));
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        final MyButton overviewButton = new MyButton(
                     Tools.getString("ClusterBrowser.ClassesOverviewButton"),
                     BACK_ICON);
        overviewButton.setPreferredSize(new Dimension(180, 50));
        overviewButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                getBrowser().getAvailableServicesInfo().selectMyself();
            }
        });
        buttonPanel.add(overviewButton);
        return buttonPanel;
    }
}
