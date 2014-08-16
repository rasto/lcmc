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

package lcmc.view;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import lcmc.gui.widget.WidgetFactory;
import lcmc.model.Cluster;
import lcmc.gui.Browser;
import lcmc.gui.ClusterViewPanel;
import lcmc.gui.EmptyViewPanel;
import lcmc.presenter.ClusterPresenter;
import lcmc.utilities.MyButton;
import lcmc.utilities.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * An implementation of a cluster tab, that contains host views of the hosts,
 * that are in the cluster.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public final class ClusterTab extends JPanel {
    private Cluster cluster;
    private JLabel labelTitle;
    @Autowired
    private EmptyViewPanel emptyViewPanel;
    @Autowired
    private ClusterPresenter clusterPresenter;
    @Autowired
    private ClusterViewPanel clusterViewPanel;

    public static final ImageIcon CLUSTER_ICON = Tools.createImageIcon(Tools.getDefault("ClustersPanel.ClusterIcon"));
    @Autowired
    private WidgetFactory widgetFactory;

    public void initWithCluster(final Cluster cluster0) {
        this.cluster = cluster0;
        if (cluster != null) {
            cluster.setClusterTab(this);
            labelTitle = new JLabel(cluster.getName());
        }

        setLayout(new BorderLayout());
        setBackground(Tools.getDefaultColor("ViewPanel.Status.Background"));
        if (cluster == null) {
            emptyViewPanel.init();
            emptyViewPanel.setDisabledDuringLoad(false);
            add(emptyViewPanel);
        }
    }

    public void addClusterView() {
        if (cluster.hostsCount() > 0) {
            clusterViewPanel.init(cluster);
            add(clusterViewPanel);
        }
        repaint();
    }

    public Cluster getCluster() {
        return cluster;
    }

    @Override
    public String getName() {
        return getClusterName();
    }

    public JLabel getLabelTitle() {
        return labelTitle;
    }

    private String getClusterName() {
        if (cluster == null) {
            return null;
        }
        return cluster.getName();
    }

    public JPanel createTabComponentWithCloseButton() {
        final JPanel tabPanel = new JPanel(new GridBagLayout());
        tabPanel.setOpaque(false);
        final JLabel iconLabel = new JLabel(CLUSTER_ICON);

        final MyButton clusterButton = getCloseButton();

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        tabPanel.add(iconLabel, gbc);

        gbc.gridx++;
        gbc.weightx = 1.0;
        tabPanel.add(labelTitle, gbc);

        gbc.gridx++;
        gbc.weightx = 0.0;
        tabPanel.add(clusterButton, gbc);
        return tabPanel;
    }

    private MyButton getCloseButton() {
        final MyButton closeButton = widgetFactory.createButton("X");
        closeButton.setBackgroundColor(Browser.STATUS_BACKGROUND);
        closeButton.setMargin(new Insets(0, 0, 0, 0));
        closeButton.setIconTextGap(0);

        final ActionListener closeAction =
                new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        clusterPresenter.onCloseCluster(cluster);
                    }
                };
        closeButton.addActionListener(closeAction);
        return closeButton;
    }
}
