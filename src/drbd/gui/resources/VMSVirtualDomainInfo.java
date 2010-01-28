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
import drbd.utilities.UpdatableItem;
import drbd.utilities.Tools;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JScrollPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
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
class VMSVirtualDomainInfo extends Info {
    /** VirtualDomain object from cluster view. */
    private final VirtualDomainInfo virtualDomainInfo;
    /** Cache for the info panel. */
    private JComponent infoPanel = null;
    /** Extra options panel. */
    private final JPanel extraOptionsPanel = new JPanel();
    /**
     * Creates the VMSVirtualDomainInfo object.
     */
    public VMSVirtualDomainInfo(final VirtualDomainInfo virtualDomainInfo,
                                final Browser browser) {
        super(virtualDomainInfo.getName(), browser);
        this.virtualDomainInfo = virtualDomainInfo;
    }

    /**
     * Returns a name of the service with virtual domain name.
     */
    public String toString() {
        final StringBuffer s = new StringBuffer(30);
        final String string;
        final String id = virtualDomainInfo.getService().getId();
        final String configName = virtualDomainInfo.getParamSaved("config");
        if (configName != null) {
            final Matcher m = VirtualDomainInfo.LIBVIRT_CONF_PATTERN.matcher(
                                                                   configName);
            if (m.matches()) {
                string = m.group(1);
            } else {
                string = id;
            }
        } else {
            string = id;
        }
        if (string == null) {
            s.insert(0, "new ");
        } else {
            if (!"".equals(string)) {
                s.append(string);
            }
        }
        return s.toString();
    }

    /**
     * Returns info panel.
     */
    public JComponent getInfoPanel() {
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
        //infoPanelDone();
        return infoPanel;
    }

    /**
     * Returns list of menu items for VM.
     */
    public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        virtualDomainInfo.addVncViewersToTheMenu(items);
        return items;
    }
}
