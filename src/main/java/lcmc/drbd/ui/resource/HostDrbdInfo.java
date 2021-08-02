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

package lcmc.drbd.ui.resource;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import lcmc.cluster.infrastructure.ssh.ExecCommandConfig;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.Application;
import lcmc.common.domain.ColorText;
import lcmc.common.domain.ExecCallback;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Access;
import lcmc.common.ui.Browser;
import lcmc.common.ui.Info;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.common.ui.utils.UpdatableItem;
import lcmc.host.domain.Host;
import lcmc.host.ui.HostBrowser;
import lcmc.logger.Logger;
import lcmc.logger.LoggerFactory;

/**
 * This class holds info data for a host. It shows host view, just like in the host tab.
 */
@Named
public class HostDrbdInfo extends Info {
    private static final Logger LOG = LoggerFactory.getLogger(HostDrbdInfo.class);
    static final String NO_DRBD_STATUS_TOOLTIP = "drbd status is not available";
    private final HostDrbdMenu hostDrbdMenu;
    private Host host;
    private final Application application;
    private final WidgetFactory widgetFactory;

    public HostDrbdInfo(Application application, SwingUtils swingUtils, Access access, MainData mainData, HostDrbdMenu hostDrbdMenu,
            WidgetFactory widgetFactory) {
        super(application, swingUtils, access, mainData);
        this.hostDrbdMenu = hostDrbdMenu;
        this.application = application;
        this.widgetFactory = widgetFactory;
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

    @Override
    public String getId() {
        return host.getName();
    }

    @Override
    public ImageIcon getCategoryIcon(final Application.RunMode runMode) {
        return HostBrowser.HOST_ICON;
    }

    @Override
    public String getToolTipForGraph(final Application.RunMode runMode) {
        return getBrowser().getHostToolTip(host);
    }

    @Override
    public JComponent getInfoPanel() {
        final Font f = new Font("Monospaced", Font.PLAIN, application.scaled(12));
        final JTextArea textArea = new JTextArea();
        textArea.setFont(f);

        final String stacktrace = Tools.getStackTrace();
        final ExecCallback execCallback =
            new ExecCallback() {
                @Override
                public void done(final String answer) {
                    textArea.setText(answer);
                }

                @Override
                public void doneError(final String answer, final int errorCode) {
                    textArea.setText("error");
                    LOG.sshError(host, "", answer, stacktrace, errorCode);
                }

            };
        // TODO: disable buttons if disconnected?
        final MyButton procDrbdButton = widgetFactory.createButton("/proc/drbd");
        procDrbdButton.addActionListener(e -> host.execCommand(new ExecCommandConfig().commandString("DRBD.getProcDrbd")
                .silentCommand()
                .silentOutput()
                .execCallback(execCallback)));
        host.registerEnableOnConnect(procDrbdButton);
        final MyButton drbdProcsButton = widgetFactory.createButton("DRBD Processes");
        drbdProcsButton.addActionListener(e -> host.execCommand(new ExecCommandConfig().commandString("DRBD.getProcesses")
                                                                                       .silentCommand()
                                                                                       .silentOutput()
                                                                                       .execCallback(execCallback)));
        host.registerEnableOnConnect(drbdProcsButton);

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
        final JPanel panel = new JPanel(new SpringLayout());
        panel.setBackground(HostBrowser.BUTTON_PANEL_BACKGROUND);

        panel.add(procDrbdButton);
        panel.add(drbdProcsButton);
        SpringUtilities.makeCompactGrid(panel, 2, 1,  // rows, cols
                                           1, 1,  // initX, initY
                                           1, 1); // xPad, yPad
        mainPanel.setMinimumSize(new Dimension(application.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                                               application.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        mainPanel.setPreferredSize(new Dimension(application.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                                                 application.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        buttonPanel.add(panel);
        mainPanel.add(new JScrollPane(textArea));
        host.execCommand(new ExecCommandConfig().commandString("DRBD.getProcDrbd")
                                                .silentCommand()
                                                .silentOutput()
                                                .execCallback(execCallback));
        return mainPanel;
    }

    public Host getHost() {
        return host;
    }

    @Override
    public String toString() {
        return host.getName();
    }

    @Override
    public String getName() {
        return host.getName();
    }

    @Override
    public List<UpdatableItem> createPopup() {
        return hostDrbdMenu.getPulldownMenu(host, this);
    }

    @Override
    public JPanel getGraphicalView() {
        final GlobalInfo globalInfo = getBrowser().getClusterBrowser().getGlobalInfo();
        globalInfo.setSelectedNode(null);
        return globalInfo.getGraphicalView();
    }

    /** Returns how much of this is used. */
    public int getUsed() {
        // TODO: maybe the load?
        return -1;
    }

    public ColorText[] getSubtextsForDrbdGraph(final Application.RunMode runMode) {
        final List<ColorText> texts = new ArrayList<>();
        if (getHost().isConnected()) {
            if (!getHost().isDrbdLoaded()) {
               texts.add(new ColorText("DRBD not loaded", null, Color.BLACK));
            } else if (!getHost().isDrbdStatusOk()) {
               texts.add(new ColorText("waiting...", null, Color.BLACK));
            }
        } else {
            texts.add(new ColorText("connecting...", null, Color.BLACK));
        }
        return texts.toArray(new ColorText[0]);
    }

    public String getIconTextForDrbdGraph(final Application.RunMode runMode) {
        if (!getHost().isConnected()) {
            return Tools.getString("HostBrowser.Drbd.NoInfoAvailable");
        }
        return null;
    }

    public ColorText getRightCornerTextForDrbdGraph(final Application.RunMode runMode) {
        return null;
    }

    String getMenuToolTip(final String cmd, final String res) {
        return getHost().getDistString(cmd).replaceAll("@RES-VOL@", res).replaceAll("@.*?@", "");
    }

    @Override
    public String getValueForConfig() {
        return host.getName();
    }
}
