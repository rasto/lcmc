/*
 * This file is part of Linux Cluster Management Console
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2012-2013, Rasto Levrinc
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.drbd.ui.resource;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.ui.Access;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.common.ui.Browser;
import lcmc.drbd.ui.DrbdGraph;
import lcmc.host.ui.HostBrowser;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.Info;
import lcmc.common.domain.ExecCallback;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.cluster.service.ssh.ExecCommandConfig;

/**
 * This class holds info data for a host.
 */
public class ProxyHostInfo extends Info {
    private final Application application;
    private final WidgetFactory widgetFactory;
    private final ProxyHostMenu proxyHostMenu;

    private static final Logger LOG = LoggerFactory.getLogger(ProxyHostInfo.class);
    private static final String NAME_PREFIX = Tools.getString("ProxyHostInfo.NameInfo");
    private Host host;

    public ProxyHostInfo(Application application, SwingUtils swingUtils, Access access, MainData mainData, Application application1, WidgetFactory widgetFactory, ProxyHostMenu proxyHostMenu) {
        super(application, swingUtils, access, mainData);
        this.application = application1;
        this.widgetFactory = widgetFactory;
        this.proxyHostMenu = proxyHostMenu;
    }

    public void init(final Host host, final Browser browser) {
        super.init(host.getName(), browser);
        this.host = host;
    }

    @Override
    public HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        return HostBrowser.HOST_ICON;
    }

    /** Returns id, which is name of the host. */
    @Override
    public String getId() {
        return host.getName();
    }

    @Override
    public ImageIcon getCategoryIcon(final Application.RunMode runMode) {
        return HostBrowser.HOST_ICON;
    }

    @Override
    public JComponent getInfoPanel() {
        final Font f = new Font("Monospaced", Font.PLAIN, application.scaled(12));
        final JTextArea ta = new JTextArea();
        ta.setFont(f);

        final String stacktrace = Tools.getStackTrace();
        final ExecCallback execCallback =
            new ExecCallback() {
                @Override
                public void done(final String answer) {
                    ta.setText(answer);
                }

                @Override
                public void doneError(final String answer, final int errorCode) {
                    ta.setText("error");
                    LOG.sshError(host, "", answer, stacktrace, errorCode);
                }

            };
        // TODO: disable buttons if disconnected?
        final MyButton procDrbdButton = widgetFactory.createButton("Show Proxy Info");
        procDrbdButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                host.execCommand(new ExecCommandConfig().commandString("DRBD.showProxyInfo")
                                                        .execCallback(execCallback));
            }
        });
        host.registerEnableOnConnect(procDrbdButton);

        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(HostBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(new Dimension(0, application.scaled(50)));
        buttonPanel.setPreferredSize(new Dimension(0, application.scaled(50)));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, application.scaled(50)));
        mainPanel.add(buttonPanel);

        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.LINE_END);
        final JPanel p = new JPanel(new SpringLayout());
        p.setBackground(HostBrowser.BUTTON_PANEL_BACKGROUND);

        p.add(procDrbdButton);
        SpringUtilities.makeCompactGrid(p, 1, 1,  // rows, cols
                                           1, 1,  // initX, initY
                                           1, 1); // xPad, yPad
        mainPanel.setMinimumSize(new Dimension(
                application.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                application.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        mainPanel.setPreferredSize(new Dimension(
                application.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                application.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        buttonPanel.add(p);
        mainPanel.add(new JScrollPane(ta));
        host.execCommand(new ExecCommandConfig().commandString("DRBD.showProxyInfo")
                                                .execCallback(execCallback));
        return mainPanel;
    }

    /** Returns host. */
    public Host getHost() {
        return host;
    }

    boolean equals(final ProxyHostInfo otherHI) {
        if (otherHI == null) {
            return false;
        }
        return otherHI.toString().equals(host.getName()); //TODO: ?
    }

    /** Returns string representation of the host. It's same as name. */
    @Override
    public String toString() {
        return NAME_PREFIX + host.getName();
    }

    /** Returns name of the host. */
    @Override
    public String getName() {
        return host.getName();
    }

    @Override
    public List<UpdatableItem> createPopup() {
        return proxyHostMenu.getPulldownMenu(this);
    }

    @Override
    public JPanel getGraphicalView() {
        final DrbdGraph dg = getBrowser().getDrbdGraph();
        if (dg == null) {
            return null;
        }
        final GlobalInfo globalInfo = getBrowser().getClusterBrowser().getGlobalInfo();
        globalInfo.setSelectedNode(null);
        return globalInfo.getGraphicalView();
    }
}
