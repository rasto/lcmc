/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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


package drbd.gui;
import drbd.AddClusterDialog;
import drbd.AddHostDialog;
import drbd.utilities.Tools;
import drbd.utilities.AllHostsUpdatable;
import drbd.utilities.MyButton;

import javax.swing.JTree;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Dimension;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;

import javax.swing.ImageIcon;



/**
 * An implementation of an empty tab panel with new cluster and host button.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class EmptyViewPanel extends ViewPanel implements AllHostsUpdatable {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Browser. */
    private EmptyBrowser browser;

    /** Background color of the status panel. */
    private static final Color STATUS_BACKGROUND =
                        Tools.getDefaultColor("ViewPanel.Status.Background");
    /** Add cluster icon. */
    private static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(
                                   Tools.getDefault("ClusterTab.ClusterIcon"));
    /** Add host icon. */
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(
                                        Tools.getDefault("HostTab.HostIcon"));
    /** Dimension of the big buttons. */
    private static final Dimension BIG_BUTTON_DIMENSION =
                                                    new Dimension(300, 100);
    /**
     * Prepares a new <code>ClusterViewPanel</code> object.
     */
    public EmptyViewPanel() {
        super();
        /* add new cluster button */
        browser = new EmptyBrowser();
        browser.initHosts();

        final JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setMinimumSize(new Dimension(0, 110));
        buttonPanel.setPreferredSize(new Dimension(0, 110));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 110));
        buttonPanel.setBackground(STATUS_BACKGROUND);
        add(buttonPanel, BorderLayout.NORTH);

        final MyButton addClusterButton =
                new MyButton(Tools.getString("ClusterTab.AddNewCluster"),
                             CLUSTER_ICON);
        addClusterButton.setBackground(
                        Tools.getDefaultColor("DefaultButton.Background"));
        addClusterButton.setPreferredSize(BIG_BUTTON_DIMENSION);
        addClusterButton.setMinimumSize(BIG_BUTTON_DIMENSION);
        addClusterButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(
                    new Runnable() {
                        public void run() {
                            AddClusterDialog acd = new AddClusterDialog();
                        }
                    });
                thread.start();
            }
        });
        Tools.getGUIData().registerAddClusterButton(addClusterButton);
        Tools.getGUIData().checkAddClusterButtons();
        buttonPanel.add(addClusterButton);

        /* add new host button */
        final MyButton addHostButton = new MyButton(
                                    Tools.getString("ClusterTab.AddNewHost"),
                                    HOST_ICON);
        addHostButton.setBackground(
                        Tools.getDefaultColor("DefaultButton.Background"));
        addHostButton.setPreferredSize(BIG_BUTTON_DIMENSION);
        addHostButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(
                    new Runnable() {
                        public void run() {
                            final AddHostDialog ahd = new AddHostDialog();
                            ahd.showDialogs();
                        }
                    });
                thread.start();
            }
        });
        Tools.getGUIData().registerAddHostButton(addHostButton);
        buttonPanel.add(addHostButton);
        createEmptyView();
        final JLabel logo = new JLabel(Tools.createImageIcon(
                                                    "startpage_head.jpg"));
        add(logo, BorderLayout.SOUTH);
        Tools.getGUIData().registerAllHostsUpdate(this);
    }

    /**
     * creates cluster view and updates the tree.
     */
    private void createEmptyView() {
        final JTree tree = getTree(browser);
        browser.updateHosts(tree);
    }

    /**
     * Updates the all hosts menu item.
     */
    public final void allHostsUpdate() {
        final JTree tree = getTree(browser);
        browser.updateHosts(tree);
    }
}
