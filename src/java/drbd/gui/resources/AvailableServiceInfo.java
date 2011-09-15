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
import drbd.data.CRMXML;
import drbd.data.ResourceAgent;
import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.utilities.Tools;
import drbd.utilities.MyButton;
import drbd.utilities.MyMenuItem;
import drbd.utilities.UpdatableItem;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.util.List;
import java.util.ArrayList;

/**
 * This class holds the information about heartbeat service from the ocfs,
 * to show it to the user.
 */
public final class AvailableServiceInfo extends HbCategoryInfo {
    /** Info about the service. */
    private final ResourceAgent resourceAgent;
    /** Available services icon. */
    private static final ImageIcon AVAIL_SERVICES_ICON =
        Tools.createImageIcon(
                Tools.getDefault("ClusterBrowser.ServiceStoppedIcon"));
    /** Back to overview icon. */
    private static final ImageIcon BACK_ICON = Tools.createImageIcon(
                                            Tools.getDefault("BackIcon"));

    /** Prepares a new <code>AvailableServiceInfo</code> object. */
    public AvailableServiceInfo(final ResourceAgent resourceAgent,
                                final Browser browser) {
        super(resourceAgent.getName(), browser);
        this.resourceAgent = resourceAgent;
    }

    /** Returns heartbeat service class. */
    ResourceAgent getResourceAgent() {
        return resourceAgent;
    }

    /** Returns icon for this menu category. */
    @Override public ImageIcon getMenuIcon(final boolean testOnly) {
        return AVAIL_SERVICES_ICON;
    }

    /** Returns type of the info text. text/plain or text/html. */
    @Override protected String getInfoType() {
        return Tools.MIME_TYPE_TEXT_HTML;
    }

    /** Returns the info about the service. */
    @Override public String getInfo() {
        final StringBuilder s = new StringBuilder(80);
        final CRMXML crmXML = getBrowser().getCRMXML();
        s.append("<h2>");
        s.append(getName());
        s.append(" (");
        s.append(crmXML.getVersion(resourceAgent));
        s.append(")</h2><h3>");
        s.append(crmXML.getShortDesc(resourceAgent));
        s.append("</h3>");
        s.append(crmXML.getLongDesc(resourceAgent));
        s.append("<br><br>");
        final String[] params = crmXML.getParameters(resourceAgent, false);
        for (final String param : params) {
            if (crmXML.isMetaAttr(resourceAgent, param)
                || "ra".equals(param)
                || ServiceInfo.PCMK_ID.equals(param)
                || ServiceInfo.GUI_ID.equals(param)) {
                continue;
            }
            s.append("<b>");
            s.append(param);
            s.append("</b><br>");
            s.append(crmXML.getParamShortDesc(resourceAgent, param));
            s.append("<br>");
        }
        return s.toString();
    }

    /** Returns back button. */
    @Override protected JComponent getBackButton() {
        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        final MyButton overviewButton = new MyButton(
                         Tools.getString("ClusterBrowser.RAsOverviewButton"),
                         BACK_ICON);
        overviewButton.setPreferredSize(new Dimension(180, 50));
        overviewButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) {
                final ResourceAgentClassInfo raci =
                        getBrowser().getClassInfoMap(
                                            resourceAgent.getResourceClass());
                if (raci != null) {
                    raci.selectMyself();
                }
            }
        });
        buttonPanel.add(overviewButton, BorderLayout.WEST);

        /* Actions */
        final JMenuBar mb = new JMenuBar();
        mb.setBackground(ClusterBrowser.PANEL_BACKGROUND);
        JMenu serviceCombo;
        serviceCombo = getActionsMenu();
        mb.add(serviceCombo);
        buttonPanel.add(mb, BorderLayout.EAST);
        return buttonPanel;
    }

    /** Returns list of menu items. */
    @Override public List<UpdatableItem> createPopup() {
        final List<UpdatableItem> items = new ArrayList<UpdatableItem>();
        final MyMenuItem addServiceMenu = new MyMenuItem(
                        Tools.getString("ClusterBrowser.AddServiceToCluster"),
                        null,
                        null,
                        new AccessMode(ConfigData.AccessType.ADMIN, false),
                        new AccessMode(ConfigData.AccessType.OP, false)) {

            private static final long serialVersionUID = 1L;

            @Override public String enablePredicate() {
                if (getBrowser().clStatusFailed()) {
                    return ClusterBrowser.UNKNOWN_CLUSTER_STATUS_STRING;
                }
                return null;
            }

            @Override public void action() {
                hidePopup();
                final ServicesInfo si = getBrowser().getServicesInfo();
                final boolean testOnly = false;
                si.addServicePanel(resourceAgent,
                                   null, /* pos */
                                   true,
                                   null,
                                   null,
                                   testOnly);
            }
        };
        items.add(addServiceMenu);
        return items;
    }
}
