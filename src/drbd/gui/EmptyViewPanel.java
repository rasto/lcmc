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
import javax.swing.SwingUtilities;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;

import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.CardLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;

import javax.swing.ImageIcon;

/**
 * An implementation of an empty tab panel with new cluster and host button.
 *
 * @author Rasto Levrinc
 * @version $Id$
 */
public class EmptyViewPanel extends ViewPanel implements AllHostsUpdatable {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Browser. */
    private final EmptyBrowser browser;
    /** Background color of the status panel. */
    private static final Color STATUS_BACKGROUND =
                        Tools.getDefaultColor("ViewPanel.Status.Background");
    /** Normal background color. */
    private static final Color HELP_BACKGROUND =
                        Tools.getDefaultColor("EmptyViewPanel.Help.Background");
    /** Add cluster icon. */
    private static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(
                                   Tools.getDefault("ClusterTab.ClusterIcon"));
    /** Add host icon. */
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(
                                        Tools.getDefault("HostTab.HostIcon"));
    /** Dimension of the big buttons. */
    private static final Dimension BIG_BUTTON_DIMENSION =
                                                    new Dimension(300, 100);
    /** Logo panel for card layout. */
    private static final String LOGO_PANEL_STRING = "LOGO-STRING";
    /** Help panel for card layout. */
    private static final String HELP_PANEL_STRING = "HELP-STRING";
    /** I am new here button text. */
    private static final String HELP_BUTTON_STRING =
                                Tools.getString("EmptyViewPanel.HelpButton");
    /** Button that hides the help text. */
    private static final String HIDE_HELP_BUTTON_STRING =
                               Tools.getString("EmptyViewPanel.HideHelpButton");
    /** I am new here help text. */
    private static final String HELP_TEXT_STRING =
                                Tools.getString("EmptyViewPanel.HelpText");
    /** Menu tree object. */
    private JTree tree;
    /**
     * Prepares a new <code>ClusterViewPanel</code> object.
     */
    public EmptyViewPanel() {
        super();
        browser = new EmptyBrowser();
        Tools.getGUIData().setEmptyBrowser(browser);
        browser.setEmptyViewPanel(this);
        browser.initHosts();

        final JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setMinimumSize(new Dimension(0, 110));
        buttonPanel.setPreferredSize(new Dimension(0, 110));
        buttonPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 110));
        buttonPanel.setBackground(STATUS_BACKGROUND);
        add(buttonPanel, BorderLayout.NORTH);
        final JPanel logoPanel = new JPanel(new CardLayout());
        logoPanel.setBackground(java.awt.Color.WHITE);
        final ImageIcon logoImage = Tools.createImageIcon("startpage_head.jpg");

        final JLabel logo = new JLabel(logoImage);
        final JPanel lPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lPanel.setBackground(java.awt.Color.WHITE);
        lPanel.add(logo);
        logoPanel.add(lPanel, LOGO_PANEL_STRING);
        final JEditorPane hp =
                  new JEditorPane(Tools.MIME_TYPE_TEXT_HTML, HELP_TEXT_STRING);
        hp.setCaretPosition(0);
        Tools.setEditorFont(hp);

        hp.setBackground(HELP_BACKGROUND);
        hp.setEditable(false);
        final JScrollPane sp = new JScrollPane(hp);
        sp.setHorizontalScrollBarPolicy(
                              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        final JPanel helpPanel = new JPanel();
        helpPanel.setBackground(STATUS_BACKGROUND);
        helpPanel.setPreferredSize(
                            new Dimension(600, logoImage.getIconHeight()));
        sp.setPreferredSize(
                        new Dimension(600, logoImage.getIconHeight() - 20));
        helpPanel.add(sp);
        logoPanel.add(helpPanel, HELP_PANEL_STRING);
        /* add "I am new here" button */
        final JPanel smallButtonPanel = new JPanel();
        smallButtonPanel.setBackground(STATUS_BACKGROUND);
        smallButtonPanel.setLayout(new BoxLayout(smallButtonPanel,
                                   BoxLayout.Y_AXIS));
        final MyButton newButton = new MyButton("I am new here");
        smallButtonPanel.add(newButton);
        buttonPanel.add(smallButtonPanel);
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Thread thread = new Thread(
                    new Runnable() {
                        public void run() {
                            final CardLayout cl =
                                        (CardLayout) (logoPanel.getLayout());
                            final String btnString = e.getActionCommand();
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    if (HELP_BUTTON_STRING.equals(btnString)) {
                                        cl.show(logoPanel, HELP_PANEL_STRING);
                                        newButton.setText(
                                                    HIDE_HELP_BUTTON_STRING);
                                    } else {
                                        cl.show(logoPanel, LOGO_PANEL_STRING);
                                        newButton.setText(HELP_BUTTON_STRING);
                                    }
                                }
                            });
                        }
                    });
                thread.start();
            }
        });
        /* check for upgrade field. */
        smallButtonPanel.add(
            Tools.getGUIData().getClustersPanel().registerUpgradeTextField());
        /* add new host button */
        final MyButton addHostButton = new MyButton(
                                    Tools.getString("ClusterTab.AddNewHost"),
                                    HOST_ICON);
        //addHostButton.setBackground(
        //                Tools.getDefaultColor("DefaultButton.Background"));
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
        add(logoPanel, BorderLayout.SOUTH);
        Tools.getGUIData().registerAllHostsUpdate(this);
        Tools.getGUIData().allHostsUpdate();

        /* add new cluster button */
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
                            acd.showDialogs();
                        }
                    });
                thread.start();
            }
        });
        Tools.getGUIData().registerAddClusterButton(addClusterButton);
        Tools.getGUIData().checkAddClusterButtons();
        buttonPanel.add(addClusterButton);
        if (!Tools.getConfigData().getAutoHosts().isEmpty()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    addHostButton.pressButton();
                }
            });
        }
    }

    /**
     * creates cluster view and updates the tree.
     */
    private void createEmptyView() {
        tree = getTree(browser);
        browser.updateHosts(tree);
    }

    /**
     * Updates the all hosts menu item.
     */
    public final void allHostsUpdate() {
        browser.updateHosts(tree);
    }
}
