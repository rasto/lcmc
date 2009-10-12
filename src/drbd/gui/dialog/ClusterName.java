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

package drbd.gui.dialog;

import drbd.data.Cluster;
import drbd.utilities.Tools;
import drbd.gui.SpringUtilities;
import drbd.gui.GuiComboBox;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * An implementation of a dialog where user can enter the name of the cluster.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class ClusterName extends DialogCluster {
    /** Serial Version UID. */
    private static final long serialVersionUID = 1L;
    /** Name field. */
    private GuiComboBox nameField;
    /** Width of the name field. */
    private static final int NAME_FIELD_WIDTH = 120;

    /**
     * Prepares a new <code>ClusterName</code> object.
     */
    public ClusterName(final WizardDialog previousDialog,
                       final Cluster cluster) {
        super(previousDialog, cluster);
    }

    /**
     * Called before the dialog is finished. It saves the value.
     */
    protected final void finishDialog() {
        getCluster().setName(nameField.getStringValue().trim());
    }

    /**
     * Returns the next dialog after this dialog.
     */
    public final WizardDialog nextDialog() {
        return new ClusterHosts(this, getCluster());
    }

    /**
     * Checks the field if it is correct and renames the tab.
     */
    protected final void checkFields(final GuiComboBox field) {
        final boolean isValid =
                            (nameField.getStringValue().trim().length() > 0);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                buttonClass(nextButton()).setEnabled(isValid);
            }
        });
        Tools.getGUIData().renameSelectedClusterTab(
                                        nameField.getStringValue().trim());
    }

    /**
     * Returns the title of the dialog.
     */
    protected final String getClusterDialogTitle() {
        return Tools.getString("Dialog.ClusterName.Title");
    }

    /**
     * Returns the description of the dialog.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.ClusterName.Description");
    }

    /**
     * Inits the dialog.
     */
    protected final void initDialog() {
        super.initDialog();
        final JComponent[] c = {buttonClass(nextButton()) };
        enableComponentsLater(c);

        enableComponents();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (!Tools.getConfigData().existsCluster(getCluster())) {
                    Tools.getConfigData().addClusterToClusters(getCluster());
                    Tools.getGUIData().addClusterTab(getCluster());
                }
                nameField.requestFocus();
            }
        });
        if (!Tools.getConfigData().getAutoClusters().isEmpty()) {
            final String name = Tools.getConfigData().getAutoClusters().get(0);
            if (!".".equals(name)) {
                Tools.sleep(1000);
                nameField.setValue(name);
            }
            Tools.sleep(1000);
            pressNextButton();
        }
    }

    /**
     * Returns panel where user can enter a cluster name.
     */
    protected final JComponent getInputPane() {
        /* Name */
        final JPanel p = new JPanel(new BorderLayout());
        final JPanel pane = new JPanel(new SpringLayout());
        final JLabel nameLabel = new JLabel(
                            Tools.getString("Dialog.ClusterName.EnterName"));
        pane.add(nameLabel);
        String name = getCluster().getName();
        if (name == null) {
            name = Tools.getConfigData().getClusters().getDefaultClusterName();
        }
        getCluster().setName(name);
        final String regexp = "^[ ,\\w.-]+$";
        nameField = new GuiComboBox(getCluster().getName(),
                                    null, null, regexp, NAME_FIELD_WIDTH, null);
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
