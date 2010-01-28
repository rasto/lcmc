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

import drbd.AddHostDialog;
import drbd.gui.Browser;
import drbd.gui.HostBrowser;
import drbd.gui.ClusterBrowser;
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;
import drbd.utilities.MyMenuItem;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import java.util.List;
import java.util.ArrayList;
import java.awt.Dimension;
import java.awt.BorderLayout;

/**
 * This class holds all hosts that are added to the GUI as opposite to all
 * hosts in a cluster.
 */
public class AllHostsInfo extends Info {
    /** infoPanel cache. */
    private JPanel infoPanel = null;

    /**
     * Creates a new AllHostsInfo instance.
     */
    public AllHostsInfo(final Browser browser) {
        super(Tools.getString("ClusterBrowser.AllHosts"), browser);
    }

    /**
     * Returns browser object of this info.
     */
    protected final ClusterBrowser getBrowser() {
        return (ClusterBrowser) super.getBrowser();
    }

    /**
     * Returns info panel of all hosts menu item. If a host is selected,
     * its tab is selected.
     */
    public final JComponent getInfoPanel() {
        if (infoPanel != null) {
            return infoPanel;
        }
        final JPanel newPanel = new JPanel();

        newPanel.setLayout(new BoxLayout(newPanel, BoxLayout.Y_AXIS));
        newPanel.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        final JPanel bPanel = new JPanel(new BorderLayout());
        bPanel.setMaximumSize(new Dimension(10000, 60));
        bPanel.setBackground(ClusterBrowser.STATUS_BACKGROUND);
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        final JMenu actionsMenu = getActionsMenu();
        updateMenus(null);
        mb.add(actionsMenu);
        bPanel.add(mb, BorderLayout.EAST);
        newPanel.add(bPanel);
        infoPanel = newPanel;
        return infoPanel;
    }

    /**
     * Creates the popup for all hosts.
     */
    public final List<UpdatableItem> createPopup() {
        final List<UpdatableItem>items = new ArrayList<UpdatableItem>();

        /* host wizard */
        final MyMenuItem newHostWizardItem =
            new MyMenuItem(Tools.getString("EmptyBrowser.NewHostWizard"),
                           HostBrowser.HOST_ICON,
                           null) {
                private static final long serialVersionUID = 1L;

                public boolean enablePredicate() {
                    return true;
                }

                public void action() {
                    final AddHostDialog dialog = new AddHostDialog();
                    dialog.showDialogs();
                }
            };
        items.add(newHostWizardItem);
        registerMenuItem(newHostWizardItem);
        Tools.getGUIData().registerAddHostButton(newHostWizardItem);
        return items;
    }
}
