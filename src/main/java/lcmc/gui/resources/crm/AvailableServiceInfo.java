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
package lcmc.gui.resources.crm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import lcmc.gui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.crm.domain.CrmXml;
import lcmc.crm.domain.ResourceAgent;
import lcmc.gui.Browser;
import lcmc.gui.ClusterBrowser;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

/**
 * This class holds the information about heartbeat service from the ocfs,
 * to show it to the user.
 */
@Named
public class AvailableServiceInfo extends HbCategoryInfo {
    private static final ImageIcon AVAIL_SERVICES_ICON =
                                    Tools.createImageIcon(Tools.getDefault("ServiceInfo.ServiceStartedIconSmall"));
    private static final ImageIcon BACK_TO_OVERVIEW_ICON = Tools.createImageIcon(Tools.getDefault("BackIcon"));
    private ResourceAgent resourceAgent;
    @Inject
    private Application application;
    @Inject
    private AvailableServiceMenu availableServiceInfo;
    @Inject
    private WidgetFactory widgetFactory;

    public void init(final ResourceAgent resourceAgent, final Browser browser) {
        super.init(resourceAgent.getServiceName(), browser);
        this.resourceAgent = resourceAgent;
    }

    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        return AVAIL_SERVICES_ICON;
    }

    /** Returns the info about the service. */
    @Override
    public String getInfo() {
        final StringBuilder s = new StringBuilder(80);
        final CrmXml crmXML = getBrowser().getCrmXml();
        s.append("<h2>");
        s.append(getName());
        s.append(" (");
        s.append(crmXML.getOcfScriptVersion(resourceAgent));
        s.append(")</h2><h3>");
        s.append(crmXML.getShortDesc(resourceAgent));
        s.append("</h3>");
        s.append(crmXML.getLongDesc(resourceAgent));
        s.append("<br><br>");
        final List<String> params = crmXML.getOcfMetaDataParameters(resourceAgent, false);
        for (final String param : params) {
            if (crmXML.isMetaAttr(resourceAgent, param)
                || ServiceInfo.RA_PARAM.equals(param)
                || ServiceInfo.PCMK_ID.equals(param)
                || ServiceInfo.GUI_ID.equals(param)) {
                continue;
            }
            s.append("<b>");
            s.append(param);
            s.append("</b><br>");
            s.append(crmXML.getShortDesc(resourceAgent, param));
            s.append("<br>");
        }
        return s.toString();
    }

    @Override
    protected JComponent getBackButton() {
        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(ClusterBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, 50));
        buttonPanel.setPreferredSize(new Dimension(0, 50));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        final MyButton overviewButton = widgetFactory.createButton(Tools.getString("ClusterBrowser.RAsOverviewButton"),
                                                                   BACK_TO_OVERVIEW_ICON);
        overviewButton.setPreferredSize(new Dimension(application.scaled(180),
                                                      application.scaled(50)));
        overviewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final ResourceAgentClassInfo raci = getBrowser().getClassInfoMap(resourceAgent.getResourceClass());
                if (raci != null) {
                    raci.selectMyself();
                }
            }
        });
        buttonPanel.add(overviewButton, BorderLayout.LINE_START);

        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.LINE_END);
        return buttonPanel;
    }

    @Override
    public List<UpdatableItem> createPopup() {
        return availableServiceInfo.getPulldownMenu(this);
    }

    public ResourceAgent getResourceAgent() {
        return resourceAgent;
    }
}
