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

package lcmc.gui.resources.drbd;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import lcmc.AddDrbdUpgradeDialog;
import lcmc.data.Application;
import lcmc.data.Host;
import lcmc.data.Subtext;
import lcmc.gui.Browser;
import lcmc.gui.DrbdGraph;
import lcmc.gui.HostBrowser;
import lcmc.gui.SpringUtilities;
import lcmc.gui.resources.Info;
import lcmc.utilities.ExecCallback;
import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;
import lcmc.utilities.MyButton;
import lcmc.utilities.SSH;
import lcmc.utilities.Tools;
import lcmc.utilities.UpdatableItem;

/**
 * This class holds info data for a host.
 * It shows host view, just like in the host tab.
 */
public class HostDrbdInfo extends Info {
    /** Logger. */
    private static final Logger LOG =
                                  LoggerFactory.getLogger(HostDrbdInfo.class);
    /** String that is displayed as a tool tip for disabled menu item. */
    static final String NO_DRBD_STATUS_STRING = "drbd status is not available";
    private final HostDrbdMenu hostDrbdMenu;
    /** Host data. */
    private final Host host;
    /** Prepares a new {@code HostDrbdInfo} object. */
    public HostDrbdInfo(final Host host, final Browser browser) {
        super(host.getName(), browser);
        this.host = host;
        hostDrbdMenu = new HostDrbdMenu(host, this);
    }

    /** Returns browser object of this info. */
    @Override
    public HostBrowser getBrowser() {
        return (HostBrowser) super.getBrowser();
    }

    /** Returns a host icon for the menu. */
    @Override
    public ImageIcon getMenuIcon(final Application.RunMode runMode) {
        return HostBrowser.HOST_ICON;
    }

    /** Returns id, which is name of the host. */
    @Override
    public String getId() {
        return host.getName();
    }

    /** Returns a host icon for the category in the menu. */
    @Override
    public ImageIcon getCategoryIcon(final Application.RunMode runMode) {
        return HostBrowser.HOST_ICON;
    }

    /** Start upgrade drbd dialog. */
    void upgradeDrbd() {
        final AddDrbdUpgradeDialog adud = new AddDrbdUpgradeDialog(this);
        adud.showDialogs();
    }

    /** Returns tooltip for the host. */
    @Override
    public String getToolTipForGraph(final Application.RunMode runMode) {
        return getBrowser().getHostToolTip(host);
    }

    /** Returns the info panel. */
    @Override
    public JComponent getInfoPanel() {
        final Font f = new Font("Monospaced",
                                Font.PLAIN,
                                Tools.getApplication().scaled(12));
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
        final MyButton procDrbdButton = new MyButton("/proc/drbd");
        procDrbdButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                host.execCommand("DRBD.getProcDrbd",
                                 execCallback,
                                 null,  /* ConvertCmdCallback */
                                 false,  /* outputVisible */
                                 SSH.DEFAULT_COMMAND_TIMEOUT);
            }
        });
        host.registerEnableOnConnect(procDrbdButton);
        final MyButton drbdProcsButton = new MyButton("DRBD Processes");
        drbdProcsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                host.execCommand("DRBD.getProcesses",
                                 execCallback,
                                 null,  /* ConvertCmdCallback */
                                 false,  /* outputVisible */
                                 SSH.DEFAULT_COMMAND_TIMEOUT);
            }
        });
        host.registerEnableOnConnect(drbdProcsButton);

        final JPanel mainPanel = new JPanel();
        mainPanel.setBackground(HostBrowser.PANEL_BACKGROUND);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(HostBrowser.BUTTON_PANEL_BACKGROUND);
        buttonPanel.setMinimumSize(
                        new Dimension(0, Tools.getApplication().scaled(50)));
        buttonPanel.setPreferredSize(
                        new Dimension(0, Tools.getApplication().scaled(50)));
        buttonPanel.setMaximumSize(
             new Dimension(Short.MAX_VALUE, Tools.getApplication().scaled(50)));
        mainPanel.add(buttonPanel);

        /* Actions */
        buttonPanel.add(getActionsButton(), BorderLayout.LINE_END);
        final JPanel p = new JPanel(new SpringLayout());
        p.setBackground(HostBrowser.BUTTON_PANEL_BACKGROUND);

        p.add(procDrbdButton);
        p.add(drbdProcsButton);
        SpringUtilities.makeCompactGrid(p, 2, 1,  // rows, cols
                                           1, 1,  // initX, initY
                                           1, 1); // xPad, yPad
        mainPanel.setMinimumSize(new Dimension(
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        mainPanel.setPreferredSize(new Dimension(
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Width"),
                Tools.getDefaultSize("HostBrowser.ResourceInfoArea.Height")));
        buttonPanel.add(p);
        mainPanel.add(new JScrollPane(ta));
        host.execCommand("DRBD.getProcDrbd",
                         execCallback,
                         null,  /* ConvertCmdCallback */
                         false,  /* outputVisible */
                         SSH.DEFAULT_COMMAND_TIMEOUT);
        return mainPanel;
    }

    /** Returns host. */
    public Host getHost() {
        return host;
    }

    /** Returns string representation of the host. It's same as name. */
    @Override
    public String toString() {
        return host.getName();
    }

    /** Returns name of the host. */
    @Override
    public String getName() {
        return host.getName();
    }

    /** Creates the popup for the host. */
    @Override
    public List<UpdatableItem> createPopup() {
        return hostDrbdMenu.getPulldownMenu();
    }

    /** Returns grahical view if there is any. */
    @Override
    public JPanel getGraphicalView() {
        final DrbdGraph dg = getBrowser().getDrbdGraph();
        if (dg == null) {
            return null;
        }
        dg.getDrbdInfo().setSelectedNode(null);
        return dg.getDrbdInfo().getGraphicalView();
    }

    /** Returns how much of this is used. */
    public int getUsed() {
        // TODO: maybe the load?
        return -1;
    }

    /** Returns subtexts that appears in the host vertex in the drbd graph. */
    public Subtext[] getSubtextsForDrbdGraph(
                                         final Application.RunMode runMode) {
        final List<Subtext> texts = new ArrayList<Subtext>();
        if (getHost().isConnected()) {
            if (!getHost().isDrbdLoaded()) {
               texts.add(new Subtext("DRBD not loaded", null, Color.BLACK));
            } else if (!getHost().isDrbdStatus()) {
               texts.add(new Subtext("waiting...", null, Color.BLACK));
            }
        } else {
            texts.add(new Subtext("connecting...", null, Color.BLACK));
        }
        return texts.toArray(new Subtext[texts.size()]);
    }

    /** Returns text that appears above the icon in the drbd graph. */
    public String getIconTextForDrbdGraph(final Application.RunMode runMode) {
        if (!getHost().isConnected()) {
            return Tools.getString("HostBrowser.Drbd.NoInfoAvailable");
        }
        return null;
    }

    /** Returns text that appears in the corner of the drbd graph. */
    public Subtext getRightCornerTextForDrbdGraph(
                                         final Application.RunMode runMode) {
        return null;
    }

    /** Tool tip for menu items. */
    String getMenuToolTip(final String cmd, final String res) {
        return getHost().getDistString(cmd).replaceAll("@RES-VOL@", res)
                                           .replaceAll("@.*?@", "");
    }

    @Override
    public String getValueForConfig() {
        return host.getName();
    }
}
