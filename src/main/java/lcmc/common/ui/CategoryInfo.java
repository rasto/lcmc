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
package lcmc.common.ui;

import javax.inject.Named;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import lcmc.common.domain.Application;

/**
 * This class holds info data for a category.
 */
@Named
public class CategoryInfo extends Info {
    public static final String MAIN_TABLE = "main";
    private JComponent infoPanel = null;

    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        return Browser.CATEGORY_ICON;
    }

    @Override
    public JComponent getInfoPanel() {
        if (infoPanel != null) {
            return infoPanel;
        }
        final JComponent table = getTable(MAIN_TABLE);
        if (table == null) {
            infoPanel = super.getInfoPanel();
        } else {
            infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS));
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

    protected JComponent getNewButton() {
        return null;
    }

    /** Selects the node in the menu and reloads everything underneath. */
    @Override
    public void selectMyself() {
        super.selectMyself();
        getBrowser().nodeChanged(getNode());
    }
}
