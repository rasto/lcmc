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

import drbd.data.ConfigData;
import drbd.data.AccessMode;
import drbd.utilities.Tools;
import drbd.gui.SpringUtilities;
import drbd.gui.GuiComboBox;
import drbd.gui.dialog.WizardDialog;

import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;

/**.
 * An implementation of a dialog where user can enter the name and password
 * for the linbit website for the plugins.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class PluginLogin extends WizardDialog {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Field with user name. */
    private GuiComboBox pluginUserField;
    /** Field with password. */
    private GuiComboBox pluginPasswordField;
    /** Checkbox to save the info. */
    private JCheckBox saveCheckBox;
    /** Width of the check boxes. */
    private static final int CHECKBOX_WIDTH = 120;

    /**
     * Prepares a new <code>PluginLogin</code> object.
     */
    public PluginLogin(final WizardDialog previousDialog) {
        super(previousDialog);
    }

    /**
     * Finishes the dialog and sets the information.
     */
    protected final void finishDialog() {
        Tools.getConfigData().setPluginLogin(
                                pluginUserField.getStringValue().trim(),
                                pluginPasswordField.getStringValue().trim(),
                                saveCheckBox.isSelected());
        Tools.loadPlugins();
    }

    /**
     * Returns the next dialog.
     */
    public WizardDialog nextDialog() {
        return null;
    }

    /**
     * Check all fields if they are correct.
     * TODO: two checkfields?
     */
    protected final void checkFields() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean v =
                    (pluginUserField.getStringValue().trim().length() > 0);
                v = v & (pluginPasswordField.getStringValue().trim().length()
                         > 0);
                buttonClass(finishButton()).setEnabled(v);
            }
        });
    }

    /**
     * Check all fields if they are correct.
     */
    protected final void checkFields(final GuiComboBox field) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                boolean v =
                    (pluginUserField.getStringValue().trim().length() > 0);
                v = v & (pluginPasswordField.getStringValue().trim().length()
                         > 0);
                buttonClass(finishButton()).setEnabled(v);
            }
        });
    }

    /**
     * Returns the title of the dialog, defined as
     * Dialog.PluginLogin.Title in TextResources.
     */
    protected final String getDialogTitle() {
        return Tools.getString("Dialog.PluginLogin.Title");
    }

    /**
     * Returns the description of the dialog, defined as
     * Dialog.PluginLogin.Description in TextResources.
     */
    protected final String getDescription() {
        return Tools.getString("Dialog.PluginLogin.Description");
    }

    /**
     * Inits the dialog.
     */
    protected final void initDialog() {
        super.initDialog();
        enableComponentsLater(new JComponent[]{buttonClass(finishButton())});
        enableComponents();
        checkFields();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                pluginUserField.requestFocus();
            }
        });
    }

    /**
     * Returns the input pane, where user can enter the user name, password and
     * can select a check box to save the info for later.
     */
    protected final JComponent getInputPane() {
        final JPanel p = new JPanel(new BorderLayout());
        final JPanel inputPane = new JPanel(new SpringLayout());
        inputPane.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background.Light"));

        /* user */
        final JLabel userLabel = new JLabel(
                      Tools.getString("Dialog.PluginLogin.EnterUser"));
        inputPane.add(userLabel);
        String pluginUser = Tools.getConfigData().getPluginUser();
        if (pluginUser == null) {
            pluginUser = Tools.getConfigData().getDownloadUser();
        }
        pluginUserField = new GuiComboBox(pluginUser,
                                     null, /* items */
                                     null, /* units */
                                     null, /* type */
                                     "^[,\\w.-]+$",
                                     CHECKBOX_WIDTH,
                                     null, /* abbrv */
                                     new AccessMode(ConfigData.AccessType.RO,
                                                    false)); /* only adv. */

        addCheckField(pluginUserField);
        userLabel.setLabelFor(pluginUserField);
        inputPane.add(pluginUserField);

        /* password */
        final JLabel passwordLabel = new JLabel(
                  Tools.getString("Dialog.PluginLogin.EnterPassword"));

        inputPane.add(passwordLabel);
        String pluginPassword = Tools.getConfigData().getPluginPassword();
        if (pluginPassword == null) {
            pluginPassword = Tools.getConfigData().getDownloadPassword();
        }
        pluginPasswordField = new GuiComboBox(
                                  pluginPassword,
                                  null, /* items */
                                  null, /* units */
                                  null, /* type */
                                  null, /* regexp */
                                  CHECKBOX_WIDTH,
                                  null, /* abbrv */
                                  new AccessMode(ConfigData.AccessType.RO,
                                                 false)); /* only adv. mode */

        addCheckField(pluginPasswordField);
        passwordLabel.setLabelFor(pluginPasswordField);
        inputPane.add(pluginPasswordField);

        /* save */
        final JLabel saveLabel = new JLabel("");
        saveLabel.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background.Light"));

        inputPane.add(saveLabel);
        saveCheckBox = new JCheckBox(
                            Tools.getString("Dialog.PluginLogin.Save"),
                            Tools.getConfigData().getPluginLoginSave());
        saveLabel.setLabelFor(saveCheckBox);
        saveCheckBox.setBackground(
                        Tools.getDefaultColor("ConfigDialog.Background.Light"));
        inputPane.add(saveCheckBox);

        SpringUtilities.makeCompactGrid(inputPane, 3, 2,  // rows, cols
                                                   1, 1,  // initX, initY
                                                   1, 1); // xPad, yPad

        p.add(inputPane, BorderLayout.SOUTH);
        return p;
    }

}
