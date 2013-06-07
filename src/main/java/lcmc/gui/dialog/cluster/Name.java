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

package lcmc.gui.dialog.cluster;

import lcmc.data.Cluster;
import lcmc.data.ConfigData;
import lcmc.data.AccessMode;
import lcmc.utilities.Tools;
import lcmc.gui.SpringUtilities;
import lcmc.gui.widget.Widget;
import lcmc.gui.widget.WidgetFactory;
import lcmc.gui.dialog.WizardDialog;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JLabel;
import javax.swing.JComponent;

/**
 * An implementation of a dialog where user can enter the name of the cluster.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class Name extends DialogCluster {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;
    /** Name field. */
    private Widget nameField;
    /** Width of the name field. */
    private static final int NAME_FIELD_WIDTH = 120;

    /** Prepares a new <code>Name</code> object. */
    public Name(final WizardDialog previousDialog, final Cluster cluster) {
        super(previousDialog, cluster);
    }

    /** Called before the dialog is finished. It saves the value. */
    @Override
    protected void finishDialog() {
        getCluster().setName(nameField.getStringValue().trim());
    }

    /** Returns the next dialog after this dialog. */
    @Override
    public WizardDialog nextDialog() {
        return new ClusterHosts(this, getCluster());
    }

    /** Checks the field if it is correct and renames the tab. */
    @Override
    protected void checkFields(final Widget field) {
        final boolean isValid =
                            (nameField.getStringValue().trim().length() > 0);
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                buttonClass(nextButton()).setEnabled(isValid);
            }
        });
        Tools.getGUIData().renameSelectedClusterTab(
                                        nameField.getStringValue().trim());
    }

    /** Returns the title of the dialog. */
    @Override
    protected String getClusterDialogTitle() {
        return Tools.getString("Dialog.Cluster.Name.Title");
    }

    /** Returns the description of the dialog. */
    @Override
    protected String getDescription() {
        return Tools.getString("Dialog.Cluster.Name.Description");
    }

    /** Inits the dialog. */
    @Override
    protected void initDialog() {
        super.initDialog();
        final JComponent[] c = {buttonClass(nextButton()) };
        enableComponentsLater(c);
        enableComponents();
        if (!Tools.getConfigData().existsCluster(getCluster())) {
            Tools.getConfigData().addClusterToClusters(getCluster());
            Tools.getGUIData().addClusterTab(getCluster());
        }
    }

    /** Inits the dialog after it becomes visible. */
    @Override
    protected void initDialogAfterVisible() {
        Tools.invokeLater(new Runnable() {
            @Override
            public void run() {
                nameField.requestFocus();
            }
        });
        if (!Tools.getConfigData().getAutoClusters().isEmpty()) {
            final String name = Tools.getConfigData().getAutoClusters().get(0);
            if (!".".equals(name)) {
                Tools.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        nameField.setValue(name);
                    }
                });
            }
            Tools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    pressNextButton();
                }
            });
        }
    }

    /** Returns panel where user can enter a cluster name. */
    @Override
    protected JComponent getInputPane() {
        /* Name */
        final JPanel p = new JPanel(new BorderLayout());
        final JPanel pane = new JPanel(new SpringLayout());
        final JLabel nameLabel = new JLabel(
                            Tools.getString("Dialog.Cluster.Name.EnterName"));
        pane.add(nameLabel);
        String name = getCluster().getName();
        if (name == null) {
            name = Tools.getConfigData().getClusters().getDefaultClusterName();
        }
        getCluster().setName(name);
        final String regexp = "^[ ,\\w.-]+$";
        nameField = WidgetFactory.createInstance(
                                       Widget.GUESS_TYPE,
                                       getCluster().getName(),
                                       Widget.NO_ITEMS,
                                       regexp,
                                       NAME_FIELD_WIDTH,
                                       Widget.NO_ABBRV,
                                       new AccessMode(ConfigData.AccessType.RO,
                                                      !AccessMode.ADVANCED),
                                       Widget.NO_BUTTON);
        addCheckField(nameField);
        nameLabel.setLabelFor(nameField);
        pane.add(nameField);

        SpringUtilities.makeCompactGrid(pane, 1, 2,  // rows, cols
                                              1, 1,  // initX, initY
                                              1, 1); // xPad, yPad
        p.add(pane, BorderLayout.SOUTH);
        return p;
    }
}
