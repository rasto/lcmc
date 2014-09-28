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

package lcmc.cluster.ui.wizard;

import java.awt.BorderLayout;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import lcmc.common.ui.GUIData;
import lcmc.common.ui.SpringUtilities;
import lcmc.common.ui.WizardDialog;
import lcmc.cluster.ui.widget.Widget;
import lcmc.cluster.ui.widget.WidgetFactory;
import lcmc.common.domain.AccessMode;
import lcmc.common.domain.Application;
import lcmc.cluster.domain.Cluster;
import lcmc.cluster.domain.Clusters;
import lcmc.common.domain.StringValue;
import lcmc.common.domain.util.Tools;
import lcmc.cluster.ui.ClusterTabFactory;

/**
 * An implementation of a dialog where user can enter the name of the cluster.
 */
@Named
public final class Name extends DialogCluster {
    private static final int NAME_FIELD_WIDTH = 120;
    private Widget nameField;

    @Inject
    private ClusterHosts clusterHostsDialog;
    @Inject
    private ClusterTabFactory clusterTabFactory;
    @Inject
    private GUIData guiData;
    @Inject
    private Application application;
    @Inject
    private Clusters allClusters;
    @Inject
    private WidgetFactory widgetFactory;

    @Override
    protected void finishDialog() {
        getCluster().setName(nameField.getStringValue().trim());
    }

    @Override
    public WizardDialog nextDialog() {
        clusterHostsDialog.init(this, getCluster());
        return clusterHostsDialog;
    }

    /** Checks the field if it is correct and renames the tab. */
    @Override
    protected void checkFields(final Widget field) {
        final String name = nameField.getStringValue().trim();
        boolean v = true;
        if (name.isEmpty()) {
            v = false;
        } else {
            for (final Cluster c : allClusters.getClusterSet()) {
                if (c != getCluster() && name.equals(c.getName())) {
                    v = false;
                    break;
                }
            }
        }
        final boolean isValid = v;
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                buttonClass(nextButton()).setEnabled(isValid);
                if (isValid) {
                    nameField.setBackground(new StringValue(name), new StringValue(name), true);
                } else {
                    nameField.wrongValue();
                }
            }
        });
        guiData.renameSelectedClusterTab(name);
    }

    @Override
    protected String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.Name.Title");
    }

    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Cluster.Name.Description");
    }

    @Override
    protected void initDialogBeforeVisible() {
        super.initDialogBeforeVisible();
        final JComponent[] c = {buttonClass(nextButton()) };
        enableComponentsLater(c);
        enableComponents();
        if (!application.existsCluster(getCluster())) {
            application.addClusterToClusters(getCluster());
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    clusterTabFactory.createClusterTab(getCluster());
                }
            });
        }
    }

    @Override
    protected void initDialogAfterVisible() {
        application.invokeLater(new Runnable() {
            @Override
            public void run() {
                nameField.requestFocus();
            }
        });
        if (!application.getAutoClusters().isEmpty()) {
            final String name = application.getAutoClusters().get(0);
            if (!".".equals(name)) {
                application.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        nameField.setValue(new StringValue(name));
                    }
                });
            }
            pressNextButton();
        }
    }

    @Override
    protected JComponent getInputPane() {
        /* Name */
        final JPanel p = new JPanel(new BorderLayout());
        final JPanel pane = new JPanel(new SpringLayout());
        final JLabel nameLabel = new JLabel(Tools.getString("Dialog.Cluster.Name.EnterName"));
        pane.add(nameLabel);
        String name = getCluster().getName();
        if (name == null) {
            name = allClusters.getDefaultClusterName();
        }
        getCluster().setName(name);
        final String regexp = "^[ ,\\w.-]+$";
        nameField = widgetFactory.createInstance(
                                       Widget.GUESS_TYPE,
                                       new StringValue(getCluster().getName()),
                                       Widget.NO_ITEMS,
                                       regexp,
                                       NAME_FIELD_WIDTH,
                                       Widget.NO_ABBRV,
                                       new AccessMode(Application.AccessType.RO, !AccessMode.ADVANCED),
                                       Widget.NO_BUTTON);
        addCheckField(nameField);
        nameLabel.setLabelFor(nameField.getComponent());
        pane.add(nameField.getComponent());

        SpringUtilities.makeCompactGrid(pane, 1, 2,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        p.add(pane, BorderLayout.PAGE_END);
        return p;
    }
}
