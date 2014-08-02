/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
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
package lcmc.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lcmc.AddClusterDialog;
import lcmc.AddHostDialog;
import lcmc.model.Host;
import lcmc.model.HostFactory;
import lcmc.utilities.AllHostsUpdatable;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An implementation of an empty tab panel with new cluster and host button.
 */
@Component
public final class EmptyViewPanel extends ViewPanel implements AllHostsUpdatable {
    /** Background color of the status panel. */
    private static final Color STATUS_BACKGROUND = Tools.getDefaultColor("ViewPanel.Status.Background");
    private static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(Tools.getDefault("ClusterTab.ClusterIcon"));
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(Tools.getDefault("HostTab.HostIcon"));
    private static final Dimension BIG_BUTTON_DIMENSION = new Dimension(300, 100);
    private static final String LOGO_PANEL_STRING = "LOGO-STRING";
    @Autowired
    private EmptyBrowser emptyBrowser;
    @Autowired
    private AddClusterDialog addClusterDialog;
    @Autowired
    private AddHostDialog addHostDialog;
    @Autowired
    private HostFactory hostFactory;

    public void init() {
        emptyBrowser.init();
        emptyBrowser.initHosts();

        final JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setMinimumSize(new Dimension(0, 110));
        buttonPanel.setPreferredSize(new Dimension(0, 110));
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        buttonPanel.setBackground(STATUS_BACKGROUND);
        add(buttonPanel, BorderLayout.PAGE_START);
        final JPanel logoPanel = new JPanel(new CardLayout());
        logoPanel.setBackground(Color.WHITE);
        final ImageIcon logoImage = Tools.createImageIcon("startpage_head.jpg");

        final JLabel logo = new JLabel(logoImage);
        final JPanel lPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        lPanel.setBackground(Color.WHITE);
        lPanel.add(logo);
        logoPanel.add(lPanel, LOGO_PANEL_STRING);
        final JPanel smallButtonPanel = new JPanel();
        smallButtonPanel.setBackground(STATUS_BACKGROUND);
        smallButtonPanel.setLayout(new BoxLayout(smallButtonPanel, BoxLayout.PAGE_AXIS));
        buttonPanel.add(smallButtonPanel);
        /* add new host button */
        final MyButton addHostButton = new MyButton(Tools.getString("ClusterTab.AddNewHost"), HOST_ICON);
        addHostButton.setBackgroundColor(Browser.STATUS_BACKGROUND);
        addHostButton.setPreferredSize(BIG_BUTTON_DIMENSION);
        addHostButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            final Host host = hostFactory.createInstance();
                            host.init();
                            addHostDialog.showDialogs(host);
                        }
                    });
                thread.start();
            }
        });
        Tools.getGUIData().registerAddHostButton(addHostButton);
        buttonPanel.add(addHostButton);
        createEmptyView();
        add(logoPanel, BorderLayout.PAGE_END);
        Tools.getGUIData().registerAllHostsUpdate(this);
        Tools.getGUIData().allHostsUpdate();

        /* add new cluster button */
        final MyButton addClusterButton = new MyButton(Tools.getString("ClusterTab.AddNewCluster"), CLUSTER_ICON);
        addClusterButton.setBackgroundColor(Browser.STATUS_BACKGROUND);
        addClusterButton.setPreferredSize(BIG_BUTTON_DIMENSION);
        addClusterButton.setMinimumSize(BIG_BUTTON_DIMENSION);
        addClusterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            addClusterDialog.showDialogs();
                        }
                    });
                thread.start();
            }
        });
        Tools.getGUIData().registerAddClusterButton(addClusterButton);
        Tools.getGUIData().checkAddClusterButtons();
        buttonPanel.add(addClusterButton);
        if (!Tools.getApplication().getAutoHosts().isEmpty()) {
            Tools.invokeLater(!Tools.CHECK_SWING_THREAD, new Runnable() {
                @Override
                public void run() {
                    addHostButton.pressButton();
                }
            });
        }
    }

    /** creates cluster view and updates the tree. */
    private void createEmptyView() {
        getTree(emptyBrowser);
        emptyBrowser.updateHosts();
    }

    /** Updates the all hosts menu item. */
    @Override
    public void allHostsUpdate() {
        emptyBrowser.updateHosts();
    }
}
