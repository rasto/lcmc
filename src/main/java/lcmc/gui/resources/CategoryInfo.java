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

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;


/**
 * This class holds info data for a category.
 */
public class CategoryInfo extends Info {
    /** Info panel. */
    private JComponent infoPanel = null;
    /** Main table. */
    public static final String MAIN_TABLE = "main";
    /**
     * Prepares a new <code>CategoryInfo</code> object.
     */
    public CategoryInfo(final String name, final Browser browser) {
        super(name, browser);
    }

    /** Info panel for the category. */
    @Override String getInfo() {
        return null;
    }

    /** Returns the icon. */
    @Override public ImageIcon getMenuIcon(final boolean testOnly) {
        return Browser.CATEGORY_ICON;
    }

    /** Returns info panel for this resource. */
    @Override public JComponent getInfoPanel() {
        if (infoPanel != null) {
            return infoPanel;
        }
        final JComponent table = getTable(MAIN_TABLE);
        if (table == null) {
            infoPanel = super.getInfoPanel();
        } else {
            infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setBackground(Browser.PANEL_BACKGROUND);
            final JComponent backButton = getBackButton();
            if (backButton != null) {
                infoPanel.add(backButton);
            }

            final JComponent newButton = getNewButton();
            if (newButton != null) {
                infoPanel.add(newButton);
            }
            final JScrollPane sp = new JScrollPane(table);
            sp.getViewport().setBackground(Browser.PANEL_BACKGROUND);
            sp.setBackground(Browser.PANEL_BACKGROUND);
            infoPanel.add(sp);
        }
        return infoPanel;
    }

    /** Returns new button. */
    protected JComponent getNewButton() {
        return null;
    }

    /** Selects the node in the menu and reloads everything underneath. */
    @Override public void selectMyself() {
        super.selectMyself();
        getBrowser().nodeChanged(getNode());
    }
}
