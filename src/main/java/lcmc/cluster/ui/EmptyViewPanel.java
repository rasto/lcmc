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
package lcmc.cluster.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.inject.Named;
import javax.inject.Provider;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;

import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.cluster.ui.wizard.AddClusterDialog;
import lcmc.common.domain.AllHostsUpdatable;
import lcmc.common.domain.Application;
import lcmc.common.domain.util.Tools;
import lcmc.common.ui.Browser;
import lcmc.common.ui.ViewPanel;
import lcmc.common.ui.main.MainData;
import lcmc.common.ui.main.MainPresenter;
import lcmc.common.ui.utils.MyButton;
import lcmc.common.ui.utils.SwingUtils;
import lcmc.host.domain.Host;
import lcmc.host.domain.HostFactory;
import lcmc.host.ui.AddHostDialog;

/**
 * An implementation of an empty tab panel with new cluster and host button.
 */
@Named
public final class EmptyViewPanel extends ViewPanel implements AllHostsUpdatable {
    /**
     * Background color of the status panel.
     */
    private static final Color STATUS_BACKGROUND = Tools.getDefaultColor("ViewPanel.Status.Background");
    private static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(Tools.getDefault("ClusterTab.ClusterIcon"));
    private static final ImageIcon HOST_ICON = Tools.createImageIcon(Tools.getDefault("HostTab.HostIcon"));
    private static final Dimension BIG_BUTTON_DIMENSION = new Dimension(300, 100);
    private static final String LOGO_PANEL_STRING = "LOGO-STRING";
    private final EmptyBrowser emptyBrowser;
    private final Provider<AddClusterDialog> addClusterDialogProvider;
    private final Provider<AddHostDialog> addHostDialogProvider;
    private final HostFactory hostFactory;
    private final MainData mainData;
    private final MainPresenter mainPresenter;
    private final Application application;
    private final SwingUtils swingUtils;
    private final WidgetFactory widgetFactory;

    public EmptyViewPanel(EmptyBrowser emptyBrowser, Provider<AddClusterDialog> addClusterDialogProvider,
            Provider<AddHostDialog> addHostDialogProvider, HostFactory hostFactory, MainData mainData, MainPresenter mainPresenter,
            Application application, SwingUtils swingUtils, WidgetFactory widgetFactory) {
        this.emptyBrowser = emptyBrowser;
        this.addClusterDialogProvider = addClusterDialogProvider;
        this.addHostDialogProvider = addHostDialogProvider;
        this.hostFactory = hostFactory;
        this.mainData = mainData;
        this.mainPresenter = mainPresenter;
        this.application = application;
        this.swingUtils = swingUtils;
        this.widgetFactory = widgetFactory;
    }

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
        final MyButton addHostButton = widgetFactory.createButton(Tools.getString("ClusterTab.AddNewHost"), HOST_ICON);
        addHostButton.setBackgroundColor(Browser.STATUS_BACKGROUND);
        addHostButton.setPreferredSize(BIG_BUTTON_DIMENSION);
        addHostButton.addActionListener(e -> {
            final Thread thread = new Thread(() -> {
                final Host host = hostFactory.createInstance();
                addHostDialogProvider.get().showDialogs(host);
            });
            thread.start();
        });
        mainData.registerAddHostButton(addHostButton);
        buttonPanel.add(addHostButton);
        createEmptyView();
        add(logoPanel, BorderLayout.PAGE_END);
        mainData.registerAllHostsUpdate(this);
        mainPresenter.allHostsUpdate();

        /* add new cluster button */
        final MyButton addClusterButton = widgetFactory.createButton(Tools.getString("ClusterTab.AddNewCluster"), CLUSTER_ICON);
        addClusterButton.setBackgroundColor(Browser.STATUS_BACKGROUND);
        addClusterButton.setPreferredSize(BIG_BUTTON_DIMENSION);
        addClusterButton.setMinimumSize(BIG_BUTTON_DIMENSION);
        addClusterButton.addActionListener(e -> {
            final Thread thread = new Thread(() -> addClusterDialogProvider.get().showDialogs());
            thread.start();
        });
        mainData.registerAddClusterButton(addClusterButton);
        mainPresenter.checkAddClusterButtons();
        buttonPanel.add(addClusterButton);
        if (!application.getAutoHosts().isEmpty()) {
            swingUtils.invokeLater(addHostButton::pressButton);
        }
    }

    /** creates cluster view and updates the tree. */
    private void createEmptyView() {
        final JTree tree = emptyBrowser.createTreeMenu((info, disableListeners) -> setRightComponentInView(emptyBrowser, info, disableListeners));
        createPanels(tree);
        emptyBrowser.updateHosts();
    }

    /** Updates the all hosts menu item. */
    @Override
    public void allHostsUpdate() {
        emptyBrowser.updateHosts();
    }
}
